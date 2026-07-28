package dev.constructive.eo.avro.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{
  writeToArray,
  JsonReader,
  JsonValueCodec,
  JsonWriter
}
import dev.constructive.eo.avro.{AvroBinaryCursor, AvroCodec, AvroPrism, AvroTraversal}
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.optics.{Getter, MendTearPrism, Optic, Prism}
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
  * ==Codec diagonals==
  *
  * The typed prism family mirrors `AvroJson`'s, with [[JsoniterBytes]] in the `Json` slot: the
  * fundamental diagonal [[valuePrism]] tears a generic runtime value into a typed `A` and mends any
  * generic value out as JSON bytes; [[bytesPrism]] and [[recordPrism]] pre-compose its input slots
  * via `tearFrom` / `mendFrom`. The [[AvroBytes]] / [[JsoniterBytes]] aliases (see the package
  * object) keep the two `Array[Byte]` roles apart in the signatures.
  *
  * ==Drilled cursor (`.json` face)==
  *
  * The full `AvroPrism` cursor sugar — `.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` /
  * `.each` / Dynamic selection — reaches this bridge through the [[json]] extensions on `AvroPrism`
  * / `AvroTraversal` (drill first, flip last, like `.record`): reads yield the typed focus, writes
  * render the whole modified document as JSON bytes. [[render]] is the focus-as-standalone-JSON
  * terminal for the read side.
  *
  * ==Non-goals (deliberate)==
  *
  * Same as `AvroJson`: the bridge sees only the '''runtime''' Avro value, never the logical type —
  * an `Instant` stored as timestamp-millis renders as a JSON number, not an ISO-8601 string. And
  * the JSON side stays '''output-only''': `AvroJson.record`'s strict JSON → record parse has no
  * counterpart here (it needs an AST to parse lawfully) — a schema-directed `JsonReader` walk could
  * add it; until then the typed diagonals cover the write-back direction.
  *
  * @groupname diagonal Codec diagonals (tearFrom / mendFrom of valuePrism)
  * @groupprio diagonal 0
  * @groupname base Structural walk
  * @groupprio base 1
  * @groupname optic Read optic (Avro bytes → JSON bytes)
  * @groupprio optic 2
  */
object AvroJsoniter:

  /** The fundamental codec diagonal — the prisms below are this one with their '''input''' slots
    * pre-composed via `MendTearPrism.tearFrom` / `mendFrom`. Tears an Avro '''generic runtime
    * value''' into a typed `A` via the codec's decode; a miss surrenders the '''structural
    * JSON-bytes view''' of the value (the [[avroToJson]] walk generalised to any value) instead of
    * the raw input, so a payload that is valid Avro but not a valid `A` still lands somewhere
    * inspectable. The mend renders any generic value back as JSON bytes — the same structural walk.
    * `AvroJson.valuePrism` with [[JsoniterBytes]] in the `Json` slot.
    * @group diagonal
    */
  def valuePrism[A](using codec: AvroCodec[A]): MendTearPrism[Any, JsoniterBytes, A, Any] =
    Prism.pPrism[Any, JsoniterBytes, A, Any](
      value => codec.decodeEither(value).left.map(_ => valueToJson(value)),
      valueToJson,
    )

  /** Typed-both-ways byte diagonal — tear Avro '''payload bytes''' ([[AvroBytes]]) into a typed
    * `A`, mend `A` out as JSON bytes through the codec's encode, so
    * `modify(f: A => A): AvroBytes => JsoniterBytes` works in one hop. The counterpart of
    * `AvroJson.bytesPrism`; same parse contract as [[bytesToJson]] (position-based under
    * `codec.schema`, no writer/reader resolution).
    * @group diagonal
    */
  def bytesPrism[A](using codec: AvroCodec[A]): MendTearPrism[AvroBytes, JsoniterBytes, A, A] =
    valuePrism[A].tearFrom(parse(codec.schema)).mendFrom(codec.encode)

  /** Record-sourced diagonal — for streams already resolved to generic records (e.g. the output of
    * `ConfluentWire.recordReader`): tear an `IndexedRecord` into a typed `A`, mend an
    * `IndexedRecord` out as JSON bytes.
    * @group diagonal
    */
  def recordPrism[A](using
      AvroCodec[A]
  ): MendTearPrism[IndexedRecord, JsoniterBytes, A, IndexedRecord] =
    valuePrism[A].tearFrom((r: IndexedRecord) => r).mendFrom((r: IndexedRecord) => r)

  /** The whole substance of the bridge: the recursive structural walk of an Avro generic record,
    * rendered to UTF-8 JSON bytes via jsoniter's `JsonWriter`. Allocates no typed case class and no
    * AST; field order is the schema's field declaration order.
    * @group base
    */
  def avroToJson(record: IndexedRecord): JsoniterBytes =
    valueToJson(record)

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
  def bytesToJson(schema: Schema): Getter[AvroBytes, JsoniterBytes] =
    new Getter(parseRecord(schema)).andThen(new Getter(avroToJson))

  /** Focus-as-JSON terminal: render a typed focus as a '''standalone JSON document''' through the
    * codec's encode + the structural walk. Compose it after any drilled optic to read just the
    * focused field as JSON bytes:
    *
    * {{{
    *   codecPrism[Person].field(_.address).andThen(AvroJsoniter.render[Address])
    *     .getOption(avroBytes)                       // Option[JsoniterBytes] of the address alone
    * }}}
    *
    * For the whole-document face (drilled writes included) use the [[json]] extension instead.
    * @group optic
    */
  def render[A](using codec: AvroCodec[A]): Getter[A, JsoniterBytes] =
    new Getter(a => valueToJson(codec.encode(a)))

  /** [[avroToJson]] generalised to any Avro generic runtime value — [[valuePrism]]'s mend and its
    * tear's miss fallback.
    */
  private def valueToJson(value: Any): JsoniterBytes =
    writeToArray[Any](value)(using anyCodec)

  /** Position-based binary parse to a generic value under a single schema — [[bytesPrism]]'s
    * `tearFrom`. Routed through `AvroBinaryCursor` like `AvroJson.parse`, so the reader comes from
    * the shared per-thread cache instead of a closure-held instance that every thread using the
    * optic would share.
    */
  private def parse(schema: Schema): AvroBytes => Any =
    bytes =>
      AvroBinaryCursor
        .leaves
        .read(bytes, 0, bytes.length, schema, schema, threadLocalStorage = true)

  /** Parse Avro binary payload bytes to a generic `IndexedRecord` under `schema` — the
    * parse-to-generic-record step behind [[bytesToJson]] and the `.json` faces. Same shared
    * per-thread reader cache as [[parse]].
    */
  private[jsoniter] def parseRecord(schema: Schema): AvroBytes => IndexedRecord =
    bytes =>
      AvroBinaryCursor
        .records
        .read(bytes, 0, bytes.length, schema, schema, threadLocalStorage = true)

  /** Write-only `JsonValueCodec` hosting the walk — the adapter jsoniter's `writeToArray` needs.
    * The decode side is unreachable by construction (nothing in this object reads).
    */
  private val anyCodec: JsonValueCodec[Any] = new JsonValueCodec[Any]:
    def encodeValue(value: Any, out: JsonWriter): Unit = writeValue(value, out)
    def decodeValue(in: JsonReader, default: Any): Any =
      in.decodeError("AvroJsoniter renders only; no JSON → value decode")
    def nullValue: Any = null.asInstanceOf[Any]

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

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroPrism]] — the prism's `.record`
  * face with Avro bytes in and '''JSON document bytes out'''. Drill with the full cursor sugar
  * (`.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` / Dynamic selection) and flip last:
  *
  * {{{
  *   import dev.constructive.eo.avro.codecPrism
  *   import dev.constructive.eo.avro.jsoniter.*
  *
  *   codecPrism[Person].name.json.modify(_.toUpperCase)(avroBytes)  // JsoniterBytes of the WHOLE
  *                                                                  // doc, name uppercased
  * }}}
  *
  * Reads (`.getOption`) still yield the typed focus `A`; every write (`.modify` / `.replace`)
  * rebuilds the record through the record face's single-walk writer and renders it via
  * [[AvroJsoniter.avroToJson]] — no byte splice, no re-parse. Routing through `.record` (rather
  * than the byte-span face) is what makes `.at(i)` navigable here: index steps are unsupported by
  * the byte-span locate but fine on the record walk. A path / branch / decode '''Miss''' renders
  * the document unchanged; genuinely '''malformed bytes throw''' at the eager parse (the output
  * format changes, so there is no byte-face-style silent pass-through). Implemented as
  * `Optic.outerProfunctor.dimap`, so the generic capability surface applies at this terminal hop.
  */
extension [A](p: AvroPrism[A])

  def json: Optic[AvroBytes, JsoniterBytes, A, A, Affine] =
    Optic
      .outerProfunctor[A, A, Affine]
      .dimap(p.record)(AvroJsoniter.parseRecord(p.rootSchemaCached))(AvroJsoniter.avroToJson)

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroTraversal]] — `.each`'s
  * multi-focus counterpart of the prism extension above: `.foldMap` / `.all` read the typed
  * elements, `.modify` splices every element (this one stays on the byte face — the traversal's
  * record face is the Ior diagnostic surface, not an `Optic`) and renders the whole spliced
  * document as JSON bytes. Reads on malformed bytes fold zero foci (byte-walk semantics); a write
  * on them throws at the render.
  */
extension [A](t: AvroTraversal[A])

  def json: Optic[AvroBytes, JsoniterBytes, A, A, MultiFocus[PSVec]] =
    Optic
      .outerProfunctor[A, A, MultiFocus[PSVec]]
      .dimap(t)(identity[AvroBytes])(AvroJsoniter.bytesToJson(t.rootSchemaCached).get)
