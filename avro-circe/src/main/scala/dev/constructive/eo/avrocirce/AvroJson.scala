package dev.constructive.eo.avrocirce

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.optics.Getter
import io.circe.Json
import java.nio.ByteBuffer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericEnumSymbol, GenericFixed, IndexedRecord}
import org.apache.avro.io.DecoderFactory

/** Structural Avro → circe bridge: turn an Avro generic runtime value into a circe [[io.circe.Json]]
  * document '''without''' first decoding into a typed case class. The Avro mirror of
  * `dev.constructive.eo.circe`, on the read side only — it walks the generic value model
  * (`IndexedRecord` / `java.util.Map` / `java.util.List` / boxed primitives / `Utf8` / …) and emits
  * the `Json` a hand-written circe encoder would produce for the '''structural''' cases.
  *
  * ==Rendering conventions==
  *
  * [[avroToJson]] is a purely structural walk of the Avro value model:
  *   - `IndexedRecord` → object (field name from `getSchema.getFields`, recurse on value, schema
  *     declaration order);
  *   - `java.util.Map` → object (stringify keys, recurse on values) — matches
  *     `Encoder[Map[String, ?]]`;
  *   - `java.util.List` → array (recurse);
  *   - `CharSequence` (incl. `org.apache.avro.util.Utf8`) → `Json.fromString`;
  *   - `Integer` / `Long` → `Json.fromLong` (`Json.fromInt(i) == Json.fromLong(i.toLong)`);
  *   - `Double` → `Json.fromDoubleOrNull` (matches circe's `Encoder[Double]`);
  *   - `Float` → `Json.fromFloatOrNull` (matches circe's `Encoder[Float]`). NB widening float→double
  *     first would change the value (`0.1f` → `0.10000000149…`) and break `JsonNumber` equality, so
  *     the float branch must not;
  *   - `Boolean` → `Json.fromBoolean`; a resolved `null` union branch → `Json.Null`;
  *   - `GenericEnumSymbol` → `Json.fromString`;
  *   - `ByteBuffer` / `GenericFixed` → array of signed byte ints (circe's `Encoder[Array[Byte]]`
  *     convention).
  *
  * Unions are resolved at the value level (the runtime value IS the branch), so dispatch on the
  * runtime type needs no union special-casing.
  *
  * ==Non-goals (deliberate)==
  *
  * The bridge sees only the '''runtime''' Avro value, never the logical type or a case-class field
  * type — so a source whose circe encoder does a '''non-structural''' transform is not reproducible
  * structurally and the bridge is not a drop-in there. Two representative cases:
  *   - '''logical types''': an `Instant` stored as timestamp-millis is a `long` at runtime; the
  *     bridge renders `Json.fromLong`, not the ISO-8601 string a `Encoder[Instant]` would emit;
  *   - '''stringified numerics''': an encoder that renders a `long` as a decimal '''string''' has no
  *     structural counterpart — the bridge renders `Json.fromLong`.
  *
  * These are the caller's concern (post-process the `Json`, or decode the typed value): the walk is
  * defined by the wire shape, not the intended semantic type.
  *
  * @groupname base Structural walk
  * @groupprio base 0
  * @groupname optic Read optic (bytes → Json)
  * @groupprio optic 1
  */
object AvroJson:

  /** The whole substance: a recursive structural walk of an Avro generic record into a circe
    * [[io.circe.Json]] object. Allocates no typed case class; field order is the schema's field
    * declaration order.
    * @group base
    */
  def avroToJson(record: IndexedRecord): Json =
    val fields = record.getSchema.getFields
    Json.fromFields((0 until fields.size).map(i => fields.get(i).name -> valueToJson(record.get(i))))

  /** Dispatch on the Avro runtime type. Order matters: structured / enum / fixed cases precede
    * `CharSequence` (`Utf8` is also a `CharSequence`).
    */
  private def valueToJson(value: Any): Json = value match
    case null                    => Json.Null
    case r: IndexedRecord        => avroToJson(r)
    case m: java.util.Map[?, ?]  =>
      Json.fromFields(m.asScala.map((k, v) => k.toString -> valueToJson(v)))
    case l: java.util.List[?]    => Json.fromValues(l.asScala.map(valueToJson))
    case e: GenericEnumSymbol[?] => Json.fromString(e.toString)
    case f: GenericFixed         => bytesFieldToJson(f.bytes)
    case b: ByteBuffer           => bytesFieldToJson(byteBufferBytes(b))
    case s: CharSequence         => Json.fromString(s.toString)
    case b: java.lang.Boolean    => Json.fromBoolean(b)
    case i: java.lang.Integer    => Json.fromLong(i.toLong)
    case l: java.lang.Long       => Json.fromLong(l)
    case f: java.lang.Float      => Json.fromFloatOrNull(f)
    case d: java.lang.Double     => Json.fromDoubleOrNull(d)
    case other                   => Json.fromString(other.toString)

  /** circe's `Encoder[Array[Byte]]` convention: a JSON array of signed byte values. */
  private def bytesFieldToJson(bytes: Array[Byte]): Json =
    Json.fromValues(bytes.map(b => Json.fromInt(b.toInt)))

  /** Read a `ByteBuffer`'s remaining bytes without disturbing its position. */
  private def byteBufferBytes(bb: ByteBuffer): Array[Byte] =
    val dup   = bb.duplicate()
    val bytes = new Array[Byte](dup.remaining())
    dup.get(bytes)
    bytes

  /** The read optic: the base [[avroToJson]] composed onto a bytes → record read [[Getter]].
    * `parseRecord` parses payload bytes to a generic `IndexedRecord` (no typed decode), and eo's
    * fused `Getter.andThen(Getter)` maps the structural walk over it, yielding a total
    * `Getter[Array[Byte], Json]`.
    *
    * The `schema` must be the exact writer schema the bytes were encoded under: the parse is
    * position-based and does no writer/reader resolution, so a mismatched schema silently misreads.
    * For a mixed-schema stream, resolve writer→reader first (e.g. `ConfluentWire.recordReader` /
    * `ConfluentWire.resolvingBytes` in `cats-eo-avro`) and walk the resolved record / bytes.
    * @group optic
    */
  def bytesToJson(schema: Schema): Getter[Array[Byte], Json] =
    new Getter(parseRecord(schema)).andThen(new Getter(avroToJson))

  /** Parse Avro binary payload bytes to a generic `IndexedRecord` under `schema` — the
    * parse-to-generic-record step behind [[bytesToJson]].
    */
  private def parseRecord(schema: Schema): Array[Byte] => IndexedRecord =
    val reader = new GenericDatumReader[IndexedRecord](schema)
    bytes => reader.read(null, DecoderFactory.get.binaryDecoder(bytes, null))

end AvroJson
