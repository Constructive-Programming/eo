package dev.constructive.eo.avro.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{
  writeToArray,
  JsonReader,
  JsonValueCodec,
  JsonWriter
}
import dev.constructive.eo.avro.AvroBinaryCursor
import dev.constructive.eo.optics.Getter
import java.nio.ByteBuffer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericEnumSymbol, GenericFixed, IndexedRecord}

/** Structural Avro → JSON-'''bytes''' bridge: render an Avro generic runtime value straight to a
  * UTF-8 JSON byte array through jsoniter-scala's `JsonWriter`, '''without''' a typed case class —
  * and without a JSON AST — in the middle. The AST-free sibling of
  * `dev.constructive.eo.avro.circe.AvroJson`: same structural walk, same rendering conventions, but
  * the output is `Array[Byte]` instead of `io.circe.Json`, so circe never touches the classpath.
  * Lives inside `cats-eo-avro` with jsoniter-scala-core as an `Optional` dependency (the rendering
  * runs through its `JsonWriter`) — add `com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core`
  * to use this package.
  *
  * ==Rendering conventions (record → JSON bytes)==
  *
  * [[avroToJson]] mirrors `AvroJson.avroToJson` case for case, so the two bridges render
  * parse-equivalent documents for the same record:
  *   - `IndexedRecord` → object (field name from `getSchema.getFields`, recurse on value, schema
  *     declaration order);
  *   - `java.util.Map` → object (stringify keys, recurse on values, entry iteration order);
  *   - `java.util.List` → array (recurse);
  *   - `CharSequence` (incl. `org.apache.avro.util.Utf8`) → JSON string;
  *   - `Integer` / `Long` → JSON number; `Float` / `Double` → JSON number via jsoniter's
  *     shortest-round-trip rendering, non-finite values → `null` (matching circe's
  *     `fromFloatOrNull` / `fromDoubleOrNull`);
  *   - `Boolean` → JSON boolean; a resolved `null` union branch → `null`;
  *   - `GenericEnumSymbol` → JSON string;
  *   - `ByteBuffer` / `GenericFixed` → array of signed byte ints (circe's `Encoder[Array[Byte]]`
  *     convention, same as `AvroJson`);
  *   - any other runtime type → JSON string of `value.toString` — a lenient last resort (the walk
  *     is total), not a convention to rely on.
  *
  * Unions are resolved at the value level (the runtime value IS the branch), so dispatch on the
  * runtime type needs no union special-casing.
  *
  * ==Non-goals (deliberate)==
  *
  * Same as `AvroJson`: the bridge sees only the '''runtime''' Avro value, never the logical type —
  * an `Instant` stored as timestamp-millis renders as a JSON number, not an ISO-8601 string. And
  * this bridge is '''render-only''': the strict JSON → record parse (the writable prism family)
  * stays on the circe side, where a JSON AST exists to parse into. A JSON-bytes → Avro direction
  * here would be a schema-directed `JsonReader` walk — add it when a client needs the write path
  * without circe.
  *
  * @groupname base Structural walk
  * @groupprio base 0
  * @groupname optic Read optic (Avro bytes → JSON bytes)
  * @groupprio optic 1
  */
object AvroJsoniter:

  /** The whole substance of the bridge: the recursive structural walk of an Avro generic record,
    * rendered to UTF-8 JSON bytes via jsoniter's `JsonWriter`. Allocates no typed case class and no
    * AST; field order is the schema's field declaration order.
    * @group base
    */
  def avroToJson(record: IndexedRecord): Array[Byte] =
    writeToArray(record)(using recordCodec)

  /** The read optic: [[avroToJson]] composed onto a bytes → record read
    * [[dev.constructive.eo.optics.Getter]] — a total `Getter[Array[Byte], Array[Byte]]` from Avro
    * payload bytes to JSON document bytes. The AST-free counterpart of `AvroJson.bytesToJson`.
    *
    * The `schema` must be the exact writer schema the bytes were encoded under: the parse is
    * position-based and does no writer/reader resolution, so a mismatched schema silently misreads.
    * For a mixed-schema stream, resolve writer → reader first (e.g.
    * `dev.constructive.eo.avro.ConfluentWire.recordReader`) and walk the resolved record with
    * [[avroToJson]].
    * @group optic
    */
  def bytesToJson(schema: Schema): Getter[Array[Byte], Array[Byte]] =
    new Getter(parseRecord(schema)).andThen(new Getter(avroToJson))

  /** Parse Avro binary payload bytes to a generic `IndexedRecord` under `schema` — routed through
    * `AvroBinaryCursor` like `AvroJson.parseRecord`, so the reader comes from the shared per-thread
    * cache instead of a closure-held instance that every thread using the optic would share.
    */
  private def parseRecord(schema: Schema): Array[Byte] => IndexedRecord =
    bytes =>
      AvroBinaryCursor
        .records
        .read(bytes, 0, bytes.length, schema, schema, threadLocalStorage = true)

  /** Write-only `JsonValueCodec` hosting the walk — the adapter jsoniter's `writeToArray` needs.
    * The decode side is unreachable by construction (nothing in this object reads).
    */
  private val recordCodec: JsonValueCodec[IndexedRecord] = new JsonValueCodec[IndexedRecord]:
    def encodeValue(record: IndexedRecord, out: JsonWriter): Unit = writeRecord(record, out)
    def decodeValue(in: JsonReader, default: IndexedRecord): IndexedRecord =
      in.decodeError("AvroJsoniter is render-only; no JSON → record decode")
    def nullValue: IndexedRecord = null.asInstanceOf[IndexedRecord]

  private def writeRecord(record: IndexedRecord, out: JsonWriter): Unit =
    val fields = record.getSchema.getFields
    out.writeObjectStart()
    (0 until fields.size).foreach { i =>
      out.writeKey(fields.get(i).name)
      writeValue(record.get(i), out)
    }
    out.writeObjectEnd()

  /** Dispatch on the Avro runtime type — mirrors `AvroJson.valueToJson`. Order matters: structured
    * / enum / fixed cases precede `CharSequence` (`Utf8` is also a `CharSequence`).
    */
  private def writeValue(value: Any, out: JsonWriter): Unit = value match
    case null                   => out.writeNull()
    case r: IndexedRecord       => writeRecord(r, out)
    case m: java.util.Map[?, ?] =>
      out.writeObjectStart()
      m.forEach { (k, v) =>
        out.writeKey(k.toString)
        writeValue(v, out)
      }
      out.writeObjectEnd()
    case l: java.util.List[?] =>
      out.writeArrayStart()
      l.forEach(v => writeValue(v, out))
      out.writeArrayEnd()
    case e: GenericEnumSymbol[?] => out.writeVal(e.toString)
    case f: GenericFixed         => writeBytesField(f.bytes, out)
    case b: ByteBuffer           => writeBytesField(byteBufferBytes(b), out)
    case s: CharSequence         => out.writeVal(s.toString)
    case b: java.lang.Boolean    => out.writeVal(b.booleanValue)
    case i: java.lang.Integer    => out.writeVal(i.intValue)
    case l: java.lang.Long       => out.writeVal(l.longValue)
    case f: java.lang.Float      =>
      // jsoniter's writeVal(Float) throws on non-finite; circe's fromFloatOrNull yields Null
      if f.isNaN || f.isInfinite then out.writeNull() else out.writeVal(f.floatValue)
    case d: java.lang.Double =>
      if d.isNaN || d.isInfinite then out.writeNull() else out.writeVal(d.doubleValue)
    case other => out.writeVal(other.toString)

  /** circe's `Encoder[Array[Byte]]` convention: a JSON array of signed byte values. */
  private def writeBytesField(bytes: Array[Byte], out: JsonWriter): Unit =
    out.writeArrayStart()
    bytes.foreach(b => out.writeVal(b.toInt))
    out.writeArrayEnd()

  /** Read a `ByteBuffer`'s remaining bytes without disturbing its position. */
  private def byteBufferBytes(bb: ByteBuffer): Array[Byte] =
    val dup = bb.duplicate()
    val bytes = new Array[Byte](dup.remaining())
    dup.get(bytes)
    bytes

end AvroJsoniter
