package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroFieldsPrism]] — the multi-field NamedTuple Prism on the Avro carrier.
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

  "AvroFieldsPrism default-Ior surface (forAll properties)" should {

    "modify(identity) on a valid Person record === Ior.Right(record)" >> {
      forAll { (p: Person) =>
        val record = personRecord(p)
        val L = codecPrism[Person].fields(_.name, _.age)
        L.modify(identity[NameAge])(record) match
          case Ior.Right(out) => recordsEqual(out, record)
          case _              => false
      }
    }

    "modify === Ior.Right(modifyUnsafe) on a valid Person record" >> {
      forAll { (p: Person, suffix: String) =>
        val record = personRecord(p)
        val L = codecPrism[Person].fields(_.name, _.age)
        val f: NameAge => NameAge = nt => (name = nt.name + suffix, age = nt.age)
        L.modify(f)(record) match
          case Ior.Right(out) => recordsEqual(out, L.modifyUnsafe(f)(record))
          case _              => false
      }
    }

    "get(valid record) decodes to a NameAge whose name/age match the Person" >> {
      forAll { (p: Person) =>
        val record = personRecord(p)
        val L = codecPrism[Person].fields(_.name, _.age)
        L.get(record) match
          case Ior.Right(nt) => nt.name == p.name && nt.age == p.age
          case _             => false
      }
    }

    "placeUnsafe-then-getOptionUnsafe round-trips the focus" >> {
      forAll { (p: Person, newName: String, newAge: Int) =>
        val record = personRecord(p)
        val L = codecPrism[Person].fields(_.name, _.age)
        val nt: NameAge = (name = newName, age = newAge)
        val modified = L.placeUnsafe(nt)(record)
        L.getOptionUnsafe(modified) match
          case Some(read) => read.name == newName && read.age == newAge
          case None       => false
      }
    }

    "two-step modify on disjoint fields == compose on Unsafe surface (bonus property)" >> {
      // Witnesses that two disjoint single-field modifies commute through a single .fields modify.
      forAll { (p: Person) =>
        val record = personRecord(p)
        val nameL = codecPrism[Person].field(_.name)
        val ageL = codecPrism[Person].field(_.age)
        val stepByStep =
          ageL.modifyUnsafe((i: Int) => i + 1)(
            nameL.modifyUnsafe((s: String) => s.toUpperCase)(record)
          )
        val both = codecPrism[Person]
          .fields(_.name, _.age)
          .modifyUnsafe(nt => (name = nt.name.toUpperCase, age = nt.age + 1): NameAge)(record)
        recordsEqual(stepByStep, both)
      }
    }
  }

  // ---- Atomic-read miss surface (D4) ------------------------------

  "AvroFieldsPrism atomic-read on partial miss" should {

    // covers: get on a record missing one of the selected fields surfaces Ior.Left with one
    // PathMissing per missing field, never assembles a partial NamedTuple
    "get → Ior.Left(chain) with PathMissing(name) when 'name' is absent" >> {
      val ageOnly = ageOnlyRecord(30)
      val L = codecPrism[Person](personSchema).fields(_.name, _.age)
      L.get(ageOnly) match
        case Ior.Left(chain) =>
          (chain.length === 1L)
            .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
        case other =>
          org
            .specs2
            .execute
            .Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
    }

    // covers: modify on a record missing one of the selected fields returns
    // Ior.Both(chain-of-PathMissing, ORIGINAL record) — never Ior.Both(chain, partialNT-shaped value)
    "modify → Ior.Both(chain, ORIGINAL record) when one selected field is absent" >> {
      val ageOnly = ageOnlyRecord(30)
      val L = codecPrism[Person](personSchema).fields(_.name, _.age)
      L.modify(identity[NameAge])(ageOnly) match
        case Ior.Both(chain, out) =>
          (chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
            .and((out eq ageOnly) === true)
        case other =>
          org
            .specs2
            .execute
            .Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result
    }

    // covers: modifyUnsafe on a partial-miss record returns the input record unchanged
    "modifyUnsafe → input record unchanged on partial miss" >> {
      val ageOnly = ageOnlyRecord(30)
      val L = codecPrism[Person](personSchema).fields(_.name, _.age)
      val out = L.modifyUnsafe(identity[NameAge])(ageOnly)
      (out eq ageOnly) === true
    }

    // covers: getOptionUnsafe on a partial-miss record returns None
    "getOptionUnsafe → None on partial miss" >> {
      val ageOnly = ageOnlyRecord(30)
      val L = codecPrism[Person](personSchema).fields(_.name, _.age)
      L.getOptionUnsafe(ageOnly) === None
    }
  }

  // ---- Helpers -----------------------------------------------------

  /** Build an `age`-only record (no `name` field) under a one-field schema. The walker reaches this
    * record as the parent of `.fields(_.name, _.age)` and surfaces `PathMissing(name)` for the
    * missing slot.
    */
  private def ageOnlyRecord(age: Int): GenericRecord =
    val ageOnlySchema =
      val fields = new java.util.ArrayList[Schema.Field]()
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
