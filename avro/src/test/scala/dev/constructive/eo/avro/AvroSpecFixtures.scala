package dev.constructive.eo.avro

import cats.data.Chain
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{
  GenericData,
  GenericDatumWriter,
  GenericRecord,
  GenericRecordBuilder,
}
import org.apache.avro.io.EncoderFactory

/** Shared ADT fixtures + kindlings-derived codecs for the avro-test specs.
  *
  * Mirrors the role of `dev.constructive.eo.circe.JsonSpecFixtures`: a single source of truth for
  * the toy-record schemas and their derived codecs that every behaviour spec re-uses.
  *
  * Kindlings' `AvroEncoder` / `AvroDecoder` / `AvroSchemaFor` instances are summoned via
  * `*.derived` per-typeclass; cats-eo-avro's [[AvroCodec]] wrapper picks them up through
  * [[AvroCodec.derived]] and exposes one combined typeclass to the prism layer.
  */
object AvroSpecFixtures:

  // ---- ADTs ----------------------------------------------------------

  /** Two-field case class — minimal product type for the prism specs. */
  case class Person(name: String, age: Int)

  object Person:

    given AvroEncoder[Person] = AvroEncoder.derived
    given AvroDecoder[Person] = AvroDecoder.derived
    given AvroSchemaFor[Person] = AvroSchemaFor.derived

  /** The reader schema used by the parse-helper tests. Pulled off the codec at fixture-creation
    * time so spec bodies don't repeat the `.schema` dance.
    */
  lazy val personSchema: Schema = summon[AvroCodec[Person]].schema

  /** Build a kindlings-encoded `IndexedRecord` for a `Person`. The encoder produces an
    * `org.apache.avro.generic.GenericData.Record` whose positional slots line up with the schema's
    * field order.
    */
  def personRecord(p: Person): GenericRecord =
    summon[AvroCodec[Person]].encode(p).asInstanceOf[GenericRecord]

  /** Encode a record to its binary wire representation under the supplied schema. Used by the
    * parse-helper round-trip tests.
    */
  def toBinary(record: GenericRecord, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    val writer = new GenericDatumWriter[GenericRecord](schema)
    writer.write(record, encoder)
    encoder.flush()
    out.toByteArray

  /** Build a record by name + value pairs against the supplied schema, useful when the spec wants a
    * "broken" record for failure-path tests.
    */
  def buildRecord(schema: Schema)(fields: (String, AnyRef)*): GenericRecord =
    val b = new GenericRecordBuilder(schema)
    fields.foreach { case (n, v) => b.set(n, v) }
    b.build()

  /** Build an `org.apache.avro.generic.GenericData.Array` from a Scala iterable. The walker's
    * `NotAnArray` / `IndexOutOfRange` paths trigger on this runtime type.
    */
  def buildArray[A](elemSchema: Schema, elems: Iterable[A]): GenericData.Array[A] =
    val arr = new GenericData.Array[A](elems.size, Schema.createArray(elemSchema))
    elems.foreach(arr.add)
    arr

  /** Convenience: an empty `Chain[AvroFailure]` for assertions that compare against
    * `Ior.Right(_)`-shaped happy-path expectations.
    */
  val emptyChain: Chain[AvroFailure] = Chain.empty[AvroFailure]

end AvroSpecFixtures
