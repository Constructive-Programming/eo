package dev.constructive.eo.avro

import cats.data.Chain
import cats.syntax.all.*
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{
  GenericData,
  GenericDatumWriter,
  GenericRecord,
  GenericRecordBuilder,
}
import org.apache.avro.io.EncoderFactory
import vulcan.Codec

/** Shared ADT fixtures + vulcan codecs for the avro-test specs.
  *
  * Mirrors the role of `dev.constructive.eo.circe.JsonSpecFixtures`: a single source of truth for
  * the toy-record schemas and their hand-rolled vulcan codecs that every behaviour spec re-uses.
  *
  * NamedTuple codec auto-derivation lands in Unit 5 (kindlings-avro-derivation, per OQ-avro-6).
  * Until then, the vulcan codecs here are constructed via `Codec.record` directly — vulcan's
  * builder takes a `FreeApplicative[Field[A, *], A]` description; `mapN` over a 2-tuple of
  * `FreeApplicative[Field[A, *], _]`s comes from cats' Apply syntax.
  */
object AvroSpecFixtures:

  // ---- ADTs ----------------------------------------------------------

  /** Two-field case class — minimal product type for the prism specs. */
  case class Person(name: String, age: Int)

  object Person:

    /** Hand-rolled vulcan `Codec[Person]`. Uses `Codec.record` to build the
      * `FreeApplicative[Field[Person, *], Person]` description that vulcan turns into a
      * record-shaped `Codec`. The two fields' codecs (`Codec.string`, `Codec.int`) are summoned
      * from vulcan's primitives.
      */
    given Codec[Person] = Codec.record("Person", "eo.avro.test") { fb =>
      (
        fb("name", _.name),
        fb("age", _.age),
      ).mapN(Person.apply)
    }

  /** The reader schema used by the parse-helper tests. Pulled off the codec at fixture-creation
    * time so spec bodies don't repeat the `.schema.toOption.get` dance.
    */
  lazy val personSchema: Schema =
    summon[Codec[Person]].schema match
      case Right(s) => s
      case Left(e)  => sys.error(s"Person codec schema is broken: $e")

  /** Build a vulcan-encoded `IndexedRecord` for a `Person`. The encoder produces an
    * `org.apache.avro.generic.GenericData.Record` whose positional slots line up with the schema's
    * field order.
    */
  def personRecord(p: Person): GenericRecord =
    summon[Codec[Person]].encode(p) match
      case Right(any) => any.asInstanceOf[GenericRecord]
      case Left(e)    => sys.error(s"Person encode failed unexpectedly: $e")

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
