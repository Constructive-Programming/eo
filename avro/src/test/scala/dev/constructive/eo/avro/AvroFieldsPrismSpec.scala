package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.util.ArrayList
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for the multi-field NamedTuple Prism ([[AvroPrism]] with a Fields focus) on the
  * Avro carrier.
  *
  * Mirrors `dev.constructive.eo.circe.JsonFieldsPrismLawsSpec`'s forAll-property block. Discipline
  * `PrismTests` is omitted: the Avro Prism laws would require `reverseGet(focus) == input`, which
  * for the multi-field case demands that the NamedTuple's derived schema be record-equivalent to
  * the parent's schema — and `IndexedRecord.equals` is schema-name sensitive. The behavioural
  * properties below cover the same ground (modify-identity, place-then-get round-trip, and the
  * focused-modify ↔ chained-single-modify equivalence) without requiring that schema-identity
  * coincidence.
  */
class AvroFieldsPrismSpec extends Specification with ScalaCheck:

  import AvroSpecFixtures.*
  import AvroFieldsPrismSpec.*
  import AvroFieldsPrismSpec.given

  private given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
    yield Person(n, a)
  )

  // ---- forAll properties on the default Ior surface ---------------
  //
  // 2026-04-29 consolidation: 5 forAll-property blocks → 1 composite forAll. All five
  // semantic invariants live in one property, joined by &&.

  // covers: modify(identity) on valid Person record === Ior.Right(record),
  //   modify(f) on valid record === Ior.Right(modifyUnsafe(f)) on the happy path,
  //   get(valid record) decodes to a NameAge whose name/age match the Person,
  //   placeUnsafe-then-getOptionUnsafe round-trips the focus,
  //   two-step modify on disjoint fields == single .fields modify (composability bonus)
  "AvroFieldsPrism default-Ior surface forAll: modify-id / parity / get / round-trip / compose" >> forAll {
    (p: Person, suffix: String, newName: String, newAge: Int) =>
      val record = personRecord(p)
      val L = codecPrism[Person].fields(_.name, _.age).record
      val f: NameAge => NameAge = nt => (name = nt.name + suffix, age = nt.age)

      val modIdOk = L.modify(identity[NameAge])(record) match
        case Ior.Right(out) => recordsEqual(out, record)
        case _              => false

      val parityOk = L.modify(f)(record) match
        case Ior.Right(out) => recordsEqual(out, L.modifyUnsafe(f)(record))
        case _              => false

      val getOk = L.get(record) match
        case Ior.Right(nt) => nt.name == p.name && nt.age == p.age
        case _             => false

      val nt: NameAge = (name = newName, age = newAge)
      val roundTripOk = L.getOptionUnsafe(L.placeUnsafe(nt)(record)) match
        case Some(read) => read.name == newName && read.age == newAge
        case None       => false

      val nameL = codecPrism[Person].field(_.name).record
      val ageL = codecPrism[Person].field(_.age).record
      val stepByStep =
        ageL.modifyUnsafe((i: Int) => i + 1)(
          nameL.modifyUnsafe((s: String) => s.toUpperCase)(record)
        )
      val both = L.modifyUnsafe((nt: NameAge) =>
        (name = nt.name.toUpperCase, age = nt.age + 1): NameAge
      )(record)
      val composeOk = recordsEqual(stepByStep, both)

      modIdOk && parityOk && getOk && roundTripOk && composeOk
  }

  // ---- Atomic-read miss surface (D4) ------------------------------
  //
  // 2026-04-29 consolidation: 4 atomic-miss tests → 1 composite block.

  // covers: get on a record missing one of the selected fields surfaces Ior.Left(chain) with
  //   one PathMissing per missing field — never assembles a partial NamedTuple,
  //   modify on a partial-miss record returns Ior.Both(chain, ORIGINAL record) — never
  //   Ior.Both(chain, partialNT-shaped),
  //   modifyUnsafe on a partial-miss record returns the input record unchanged,
  //   getOptionUnsafe on a partial-miss record returns None
  "AvroFieldsPrism partial-miss surfaces (D4): get/modify/modifyUnsafe/getOptionUnsafe" >> {
    val ageOnly = ageOnlyRecord(30)
    val L = codecPrism[Person].fields(_.name, _.age).record

    val getOk = L.get(ageOnly) match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    val modifyOk = L.modify(identity[NameAge])(ageOnly) match
      case Ior.Both(chain, out) =>
        (chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
          .and((out eq ageOnly) === true)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result

    val unsafeMod = L.modifyUnsafe(identity[NameAge])(ageOnly)
    val unsafeModOk = (unsafeMod eq ageOnly) === true
    val unsafeGetOk = L.getOptionUnsafe(ageOnly) === None

    getOk.and(modifyOk).and(unsafeModOk).and(unsafeGetOk)
  }

  // ---- .record.transform / transformUnsafe (AvroFocus.Fields.buildSubRecord) -----------
  //
  // Unlike modify/get (which route through the ATOMIC readFields — any missing selected field
  // fails the whole read), transform/transformUnsafe project the sub-record via buildSubRecord,
  // which is non-atomic: a missing selected field projects as `null` instead of failing.

  // covers: buildSubRecord's per-index projection loop — rewrite the projected NamedTuple
  //   sub-record directly (no codec decode/encode) and splice back; the untouched selected
  //   field (`age`) passes through the projection unchanged.
  //   NOTE: uses the Person shape because a second differently-shaped .fields expansion in one
  //   file trips an AvroPrismMacro hoisting bug ("reference to decode_String$macro$N outside
  //   the scope where it was defined"); surfaced by this run, tracked separately.
  "AvroFieldsPrism .record.transform / transformUnsafe: sub-record rewrite splices back, sibling survives" >> {
    val record = personRecord(Person("alice", 30))
    val L = codecPrism[Person].fields(_.name, _.age).record

    def upperName(sub: IndexedRecord): IndexedRecord =
      val fresh = new GenericData.Record(sub.getSchema)
      val namePos = sub.getSchema.getField("name").pos
      fresh.put(namePos, sub.get(namePos).toString.toUpperCase)
      val agePos = sub.getSchema.getField("age").pos
      fresh.put(agePos, sub.get(agePos))
      fresh

    val viaIor = L.transform(upperName)(record)
    val viaUnsafe = L.transformUnsafe(upperName)(record)

    val iorOk = viaIor match
      case Ior.Right(out) =>
        (out.get(personSchema.getField("name").pos).toString === "ALICE")
          .and(out.get(personSchema.getField("age").pos).asInstanceOf[Int] === 30)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    val unsafeOk =
      (viaUnsafe.get(personSchema.getField("name").pos).toString === "ALICE")
        .and(viaUnsafe.get(personSchema.getField("age").pos).asInstanceOf[Int] === 30)

    iorOk.and(unsafeOk)
  }

  // covers: buildSubRecord's missing-field branch (`if parentField == null then null`) — unlike
  //   readFields (which Ior.Left/Ior.Both on ANY missing selected field), transformUnsafe's
  //   non-atomic projection succeeds even when a selected field is absent from the schema, and
  //   overlayFields silently drops the update for a field the parent schema doesn't have
  "AvroFieldsPrism .record.transformUnsafe on a schema-drifted parent: missing field projects as null, present field still rewrites" >> {
    val ageOnly = ageOnlyRecord(30)
    val L = codecPrism[Person].fields(_.name, _.age).record

    val bumpAge: IndexedRecord => IndexedRecord = sub =>
      val fresh = new GenericData.Record(sub.getSchema)
      val namePos = sub.getSchema.getField("name").pos
      val agePos = sub.getSchema.getField("age").pos
      fresh.put(namePos, sub.get(namePos)) // null — "name" is absent from ageOnly's schema
      fresh.put(agePos, sub.get(agePos).asInstanceOf[Integer].intValue + 1)
      fresh

    val out = L.transformUnsafe(bumpAge)(ageOnly)
    out.get(0).asInstanceOf[Integer].intValue === 31
  }

  // ---- Helpers -----------------------------------------------------

  /** Build an `age`-only record (no `name` field) under a one-field schema. The walker reaches this
    * record as the parent of `.fields(_.name, _.age)` and surfaces `PathMissing(name)` for the
    * missing slot.
    */
  private def ageOnlyRecord(age: Int): GenericRecord =
    val ageOnlySchema =
      val fields = new ArrayList[Schema.Field]()
      fields.add(
        new Schema.Field("age", Schema.create(Schema.Type.INT), null, null)
      )
      Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val rec = new GenericData.Record(ageOnlySchema)
    rec.put(0, Integer.valueOf(age))
    rec

  /** Compare two records via apache-avro's structural compare. Uses the lhs's schema as the
    * comparison schema so two records under different schema instances but identical declared
    * shapes compare equal positionally.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

end AvroFieldsPrismSpec

object AvroFieldsPrismSpec:

  /** The NamedTuple type the macro synthesises for `Person.fields(_.name, _.age)`. Declared at the
    * spec object level so the spec body can ascribe values to it without re-typing the NamedTuple
    * shape.
    */
  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

  // Kindlings-derived AvroCodec for the synthesised NamedTuple. The macro summons this at the
  // call site of `.fields(_.name, _.age)`; without the explicit derivation here it wouldn't be
  // resolvable from inside this spec.
  given AvroEncoder[NameAge] = AvroEncoder.derived
  given AvroDecoder[NameAge] = AvroDecoder.derived
  given AvroSchemaFor[NameAge] = AvroSchemaFor.derived

end AvroFieldsPrismSpec
