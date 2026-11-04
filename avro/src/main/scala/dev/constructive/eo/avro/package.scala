package dev.constructive.eo

import scala.annotation.tailrec

import cats.Eq
import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.Plated
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}

/** Cross-representation optics bridging native Scala types and their Apache Avro on-the-wire form.
  *
  * The entry point is [[AvroPrism.codecPrism]] (read-aloud API parallel to
  * `dev.constructive.eo.circe.codecPrism`):
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
  * '''Carrier note.''' Both `AvroRecordPrism` and `eo-circe`'s `JsonPrism` are `Affine`-carried —
  * lawful Optionals whose composed / upcast writes preserve siblings. A drilled focus is an
  * Optional, so the carrier widens to `Affine` (via the Composer on composition) rather than
  * pretending to be a `Prism`; the two record faces stay in step.
  *
  * '''Deliberate duplication.''' [[PathStep]] is duplicated, not shared with eo-circe — the
  * `UnionBranch` case is Avro-only and forcing it into eo-circe would pollute that module. See
  * [[PathStep]]'s class doc.
  */
package object avro:

  /** Avro '''binary payload''' bytes — the wire encoding under a writer schema. Same runtime type
    * as any other `Array[Byte]`; the alias keeps the role visible in bridge signatures where a
    * second byte-array or document type sits alongside (e.g.
    * `bytesPrism[A]: MendTearPrism[AvroBytes, JsoniterBytes, A, A]` in `avro.jsoniter`, or the
    * `.json` faces' `Optic[AvroBytes, Json, A, A, …]` in `avro.circe`).
    */
  type AvroBytes = Array[Byte]

  /** Root-level Prism from Avro to a native type `S`. Reads `S`'s schema off the in-scope
    * [[AvroCodec]]`[S]`. Alias for [[AvroPrism.codecPrism]] that reads more naturally when composed
    * with `.field`.
    */
  def codecPrism[S](using codec: AvroCodec[S]): AvroPrism[S] = AvroPrism.codecPrism[S]

  /** Structural equality for `IndexedRecord` — schema + positional field values, recursing through
    * nested records / arrays / maps.
    *
    * A public `given`: downstream property tests and round-trip specs that compare records by value
    * (rather than reference) pick this up via `import dev.constructive.eo.avro.given`.
    *
    * Implementation note: defers to `org.apache.avro.generic.GenericData.compare` which already
    * walks the schema-driven runtime shape recursively. Equal iff `compare == 0`.
    */
  given Eq[IndexedRecord] with

    def eqv(x: IndexedRecord, y: IndexedRecord): Boolean =
      x.getSchema == y.getSchema &&
        GenericData.get().compare(x, y, x.getSchema) == 0

  /** `Eq` specialised to `GenericRecord` — the more common runtime type. Reuses the same
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
    @tailrec def loop(i: Int): Unit =
      if i < n then
        rec.get(i) match
          case child: IndexedRecord => buf += child
          case _                    => ()
        loop(i + 1)
    loop(0)
    PSVec.from(buf)

  private def avroRecordRebuild(rec: IndexedRecord, vec: PSVec[IndexedRecord]): IndexedRecord =
    val schema = rec.getSchema
    val n = schema.getFields.size
    val copy = new GenericData.Record(schema)
    @tailrec def loop(i: Int, k: Int): Unit =
      if i < n then
        val nextK =
          rec.get(i) match
            case _: IndexedRecord =>
              copy.put(i, vec(k))
              k + 1
            case other =>
              copy.put(i, other)
              k
        loop(i + 1, nextK)
    loop(0, 0)
    copy

  /** How the JSON → record bridges treat a field the body OMITS (shared by `AvroJson` and
    * `AvroJsoniter`): `Refuse` is the strict prism's miss, `Default` takes the schema's declared
    * default, `DefaultOrNull` also resolves a nullable field with no declared default to `null`.
    */
  private[avro] enum AbsentField:
    case Refuse, Default, DefaultOrNull

    /** The fill for an absent `field` under this mode, as the generic runtime value the walks
      * produce; `None` is the miss.
      *
      * A declared default is apache-avro's own resolution (`GenericData.getDefaultValue` encodes
      * the default's JSON node to binary and decodes it back under the field schema, so logical
      * types, unions and nested record defaults come back exactly as the binary path would produce
      * them). It caches per `Field` and hands back the SAME instance on every call, hence the
      * `deepCopy`: two parsed records must not alias one mutable array / map / record default. This
      * is precisely what `GenericRecordBuilder` does, so the value is Avro's answer, not ours.
      * `hasDefaultValue` is checked FIRST: `getDefaultValue` throws `AvroMissingFieldException` for
      * a field without one, and a miss here must stay a value. The other throw (an IOException from
      * the round trip) is unreachable for a schema that parsed: `Schema` validates every default at
      * parse time.
      *
      * `DefaultOrNull` adds one rule Avro itself lacks: a nullable field (`Schema.isNullable` — a
      * `null` type, or a union with a `null` branch in any position, `Option[A]`'s `["null", A]` as
      * well as wider unions like `["null", "string", "int"]`) with no declared default reads as
      * `null`.
      */
    def fill(field: Schema.Field): Option[Any] =
      this match
        case Refuse                     => None
        case _ if field.hasDefaultValue =>
          Some(genericData.deepCopy(field.schema, genericData.getDefaultValue(field)))
        case DefaultOrNull if field.schema.isNullable => Some(null)
        case _                                        => None

  /** apache-avro's singleton `GenericData` — the instance `GenericRecordBuilder` uses; holds the
    * default conversions, so a logical-type default resolves to the value the binary decode does.
    */
  private val genericData: GenericData = GenericData.get()

end avro
