package dev.constructive.eo

import cats.Eq
import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.Plated
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}

/** Cross-representation optics bridging native Scala types and their Apache Avro on-the-wire form.
  *
  * The entry point is [[AvroPrism.codecPrism]] (read-aloud API parallel to
  * [[dev.constructive.eo.circe.codecPrism]]):
  *
  * {{{
  *   import dev.constructive.eo.avro.{AvroCodec, codecPrism}
  *   import hearth.kindlings.avroderivation.AvroEncoder.derived
  *   import hearth.kindlings.avroderivation.AvroDecoder.derived
  *   import hearth.kindlings.avroderivation.AvroSchemaFor.derived
  *
  *   case class Person(name: String, age: Int)
  *   // `given AvroCodec[Person]` falls out automatically from kindlings'
  *   // derived `AvroEncoder[Person]`, `AvroDecoder[Person]`, `AvroSchemaFor[Person]`.
  *
  *   val personPrism: AvroPrism[Person]   = codecPrism[Person]
  *   val namePrism:   AvroPrism[String]   = personPrism.field(_.name)
  *   namePrism.modify(_.toUpperCase)(payloadBytes)
  *   // → the same Array[Byte] payload, with `name` upper-cased in
  *   //   place — no Person (nor any root record) ever materialised.
  *   namePrism.record.modify(_.toUpperCase)(record)
  *   // → the IndexedRecord-carried face, with Ior diagnostics.
  * }}}
  *
  * The default carrier is the binary wire form itself (`Array[Byte]`, mirroring
  * `dev.constructive.eo.jsoniter.JsoniterPrism`); the record-carried face behind `.record` follows
  * `dev.constructive.eo.circe`'s architecture decisions on `Json`: `IndexedRecord` plays the role
  * of `Json`, [[AvroCodec]] plays the role of `(io.circe.Encoder[A], io.circe.Decoder[A])`, and
  * `AvroFailure` plays the role of `JsonFailure`.
  *
  * '''Carrier deviation from eo-circe (deliberate).''' `AvroRecordPrism` is `Affine`-carried (a
  * lawful Optional whose composed / upcast writes preserve siblings). `eo-circe`'s `JsonPrism` is
  * still `Either`-carried and has the same latent reconstruct-standalone footgun on a drilled
  * composed write; aligning circe is tracked separately. Do not assume the two record faces share a
  * carrier.
  *
  * '''Gap-1 (per the eo-avro plan).''' [[PathStep]] is duplicated, not shared with eo-circe — the
  * `UnionBranch` case is Avro-only and forcing it into eo-circe would pollute that module. The
  * duplication is intentional; see [[PathStep]]'s class doc.
  */
package object avro:

  /** Type alias for the Avro carrier — the underlying `org.apache.avro.generic.IndexedRecord`
    * interface, exposing positional `get(i)` / `put(i, v)` accessors that map cleanly to a
    * path-step navigation. The optic surface threads `Avro` everywhere `JsonPrism` would have
    * threaded `Json`.
    */
  type Avro = IndexedRecord

  /** Root-level Prism from Avro to a native type `S`. Reads `S`'s schema off the in-scope
    * [[AvroCodec]]`[S]` (per OQ-avro-5). Alias for [[AvroPrism.codecPrism]] that reads more
    * naturally when composed with `.field`.
    */
  def codecPrism[S](using codec: AvroCodec[S]): AvroPrism[S] = AvroPrism.codecPrism[S]

  /** Root-level Prism from Avro to a native type `S`, with an explicit reader schema. Use when the
    * schema is loaded at runtime from an `.avsc` file or a Schema Registry rather than derived from
    * the codec.
    */
  def codecPrism[S](schema: Schema)(using codec: AvroCodec[S]): AvroPrism[S] =
    AvroPrism.codecPrism[S](schema)

  /** Structural equality for [[IndexedRecord]] — schema + positional field values, recursing
    * through nested records / arrays / maps.
    *
    * Per Gap-5 / OQ-avro-9 this is a public `given`. Downstream property tests and round-trip specs
    * that compare records by value (rather than reference) pick this up via
    * `import dev.constructive.eo.avro.given`.
    *
    * Implementation note: defers to `org.apache.avro.generic.GenericData.compare` which already
    * walks the schema-driven runtime shape recursively. Equal iff `compare == 0`.
    */
  given Eq[IndexedRecord] with

    def eqv(x: IndexedRecord, y: IndexedRecord): Boolean =
      x.getSchema == y.getSchema &&
        GenericData.get().compare(x, y, x.getSchema) == 0

  /** [[Eq]] specialised to [[GenericRecord]] — the more common runtime type. Reuses the same
    * `compare`-based implementation.
    */
  given Eq[GenericRecord] = Eq.by(identity[IndexedRecord])

  /** A [[dev.constructive.eo.optics.Plated]] over the Avro record tree — the Avro analogue of
    * `dev.constructive.eo.circe.platedJson`. The immediate children of a record are its
    * directly-record-valued fields, so `Plated.transform` / `rewrite` / `universe` walk a whole
    * nested-record document recursively (redact a field in every nested record, rewrite a value at
    * any depth, …). Rebuild copies into a fresh `GenericData.Record` so the walk stays pure;
    * stack-safe via the combinators' `cats.Eval` trampoline.
    *
    * '''Scope (v1):''' only fields whose value is *itself* an `IndexedRecord` are recursion points.
    * Records nested inside array / map / union-of-record fields are leftover skeleton — descending
    * those is a future extension (mirrors the core `plate`'s exact-self-type rule).
    */
  given platedAvro: Plated[IndexedRecord] =
    Plated.fromChildrenVec(avroRecordChildren, avroRecordRebuild)

  private def avroRecordChildren(rec: IndexedRecord): PSVec[IndexedRecord] =
    val n = rec.getSchema.getFields.size
    val buf = collection.mutable.ArrayBuffer.empty[IndexedRecord]
    var i = 0
    while i < n do
      rec.get(i) match
        case child: IndexedRecord => buf += child
        case _                    => ()
      i += 1
    PSVec.fromIterable(buf)

  private def avroRecordRebuild(rec: IndexedRecord, vec: PSVec[IndexedRecord]): IndexedRecord =
    val schema = rec.getSchema
    val n = schema.getFields.size
    val copy = new GenericData.Record(schema)
    var i = 0
    var k = 0
    while i < n do
      rec.get(i) match
        case _: IndexedRecord =>
          copy.put(i, vec(k))
          k += 1
        case other =>
          copy.put(i, other)
      i += 1
    copy

end avro
