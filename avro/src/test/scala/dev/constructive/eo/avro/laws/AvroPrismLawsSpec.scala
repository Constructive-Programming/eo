package dev.constructive.eo.avro
package laws

import cats.data.Ior
import dev.constructive.eo.laws.PrismLaws
import dev.constructive.eo.laws.discipline.PrismTests
import dev.constructive.eo.optics.Optic
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.{GenericData, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Unit 9: witness that [[AvroPrism]] satisfies the three Prism laws on a full-cover Avro fixture,
  * plus ScalaCheck property coverage against both the default Ior surface and the `*Unsafe` escape
  * hatches.
  *
  * Mirrors `dev.constructive.eo.circe.JsonFieldsPrismLawsSpec` block-for-block — same shape, same
  * scenario count — with two Avro-forced deviations:
  *
  *   1. '''Discipline `PrismTests` runs against the IDENTITY full-cover prism `codecPrism[Pair]`
  *      (focus = `Pair`), not against `codecPrism[Pair].fields(_.a, _.b)` (focus = NamedTuple).'''
  *      `PrismLaws.partialRoundTripOneWay` is `prism.reverseGet(a) == s` — universal `==`. For
  *      `IndexedRecord`, `GenericData.Record.equals` is schema-instance-sensitive: two records are
  *      equal only when their schemas are the same instance AND their fields compare structurally.
  *      A `.fields(...)` cover encodes `a` through the NamedTuple's codec — the resulting record
  *      carries kindlings' synthesised `NamedTuple` schema, which is NOT the same instance as the
  *      `Pair` schema even when the field set is identical. Concrete numbers from the smoke test we
  *      ran before writing this file (Pair{a:Int, b:String} vs PairFocus NamedTuple, both with
  *      kindlings-default config):
  *
  * {{{
  *      Pair record schema fullName:    "Pair"
  *      NamedTuple schema fullName:     "NamedTuple"
  *      Rebuilt schema fullName:        "NamedTuple"
  *      rebuilt == record:              false      // Schema mismatch
  *      GenericData.compare:            0          // Values match positionally
  * }}}
  *
  * The `==`-based law fails on the schema check, even though the values are structurally equal. The
  * shared structural [[Eq]] given (`given Eq[IndexedRecord]` from the package object) routes
  * through `GenericData.compare` and would pass; but `PrismLaws` doesn't take a cats `Eq[S]` — it
  * uses `==` directly (mirroring Monocle). So we shift the discipline ruleset to a fixture where
  * the focus IS the source: `codecPrism[Pair]` with focus = `Pair`. `reverseGet` re-encodes `Pair`
  * through the same codec under the same schema, and the law holds. Partial-cover behaviour is
  * still witnessed by [[AvroFieldsPrismSpec]]'s forAll properties + the `forAll` block here (which
  * uses `Eq[IndexedRecord]` for comparison).
  *
  * The smoke test that produced those numbers lives in the conversation history; the fact that it
  * produced `rebuilt == record: false` was the criterion to move the discipline ruleset off
  * `.fields(_.a, _.b)`.
  *
  *   2. '''The `.union[Long]` Prism is structurally NOT a Prism for the discipline-laws purpose.'''
  *      `partialRoundTripOneWay` requires `reverseGet(getOption(s).get) == s` — but a union prism's
  *      `reverseGet(longValue)` produces an empty / null-filled parent record (since the union
  *      branch lives several path-steps deep and the prism layer's reverseGet has no parent
  *      context). Concrete: `unionPrism.reverseGet(99L)` produces a `Transaction` record with all
  *      slots null, which throws `NullPointerException` on apache-avro's `Record.equals`. We
  *      witness the union prism's partial round-trip on the `Some` branch via forAll instead.
  *
  * forAll properties separately exercise the Ior-bearing default surface and the `*Unsafe` escape
  * hatches.
  */
class AvroPrismLawsSpec extends Specification with Discipline with ScalaCheck:

  import AvroSpecFixtures.{Person, Transaction, personRecord, transactionRecord}
  import AvroPrismLawsSpec.*
  import AvroPrismLawsSpec.given

  // ---- discipline PrismLaws ---------------------------------------
  //
  // The generic Prism laws (ported from Monocle) require `reverseGet`
  // to round-trip through the source `S` via universal `==`. For an
  // Avro carrier that means the rebuilt `IndexedRecord` must satisfy
  // apache-avro's schema-instance-sensitive `equals`. The full-cover
  // identity prism `codecPrism[Pair]` (focus = Pair, NOT the .fields
  // NamedTuple sub-cover) re-encodes through the same codec under the
  // same schema, so `==` holds. See the class doc above for the
  // smoke-test numbers that drove this fixture choice.

  val pairPrism: Optic[IndexedRecord, IndexedRecord, Pair, Pair, Either] =
    codecPrism[Pair]

  checkAll(
    "AvroPrism — codecPrism[Pair] (full-cover identity)",
    new PrismTests[IndexedRecord, Pair]:
      val laws: PrismLaws[IndexedRecord, Pair] = new PrismLaws[IndexedRecord, Pair]:
        val prism = pairPrism
    .prism,
  )

  // ---- forAll properties on the default Ior surface ---------------
  //
  // 2026-04-29 consolidation: 11 forAll blocks across 3 should{}'s → 3 composite forAll
  // properties (one per surface). The discipline `checkAll` PrismTests block above stays 1:1.

  // covers: AvroFieldsPrism modify(identity) on valid Person === Ior.Right(record),
  //   modify(f) === Ior.Right(modifyUnsafe(f)) on the happy path,
  //   get(valid record) decodes to a NameAge whose name/age match the Person,
  //   placeUnsafe-then-getOptionUnsafe round-trips the focus,
  //   two-step modify on disjoint fields == composed single .fields modify
  "AvroFieldsPrism .fields default-Ior forAll: modify-id / parity / get / round-trip / compose" >> forAll {
    (p: Person, suffix: String, newName: String, newAge: Int) =>
      val record = personRecord(p)
      val L = codecPrism[Person].fields(_.name, _.age)
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

      val nt: NameAge = (name = newName, age = newAge): NameAge
      val roundTripOk = L.getOptionUnsafe(L.placeUnsafe(nt)(record)) match
        case Some(read) => read.name == newName && read.age == newAge
        case None       => false

      val nameL = codecPrism[Person].field(_.name)
      val ageL = codecPrism[Person].field(_.age)
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

  // covers: AvroPrism .union[Long] get-then-place on the Some(long) branch round-trips the long,
  //   .union[Long] modify(identity) on the Some(long) branch is record-equivalent to the input,
  //   .union[Long] compose-modify on the Some(long) branch: (g ∘ f) ≡ g.after(f) on the hit branch
  //
  // Discipline `PrismTests` is unsound on `.union[Long]` for the reverseGet-loses-parent reason
  // documented in the class doc — these three laws are the meaningful ones on the Some branch.
  "AvroPrism .union[Long] on Option[Long] forAll: get / modify-id / compose-modify on Some" >> forAll {
    (id: String, n: Long) =>
      val record = transactionRecord(Transaction(id, Some(n)))
      val U = codecPrism[Transaction].field(_.amount).union[Long]

      val getOk = U.get(record) match
        case Ior.Right(read) => read == n
        case _               => false

      val modIdOk = U.modify(identity[Long])(record) match
        case Ior.Right(out) => recordsEqual(out, record)
        case _              => false

      val f: Long => Long = _ + 1L
      val g: Long => Long = _ * 2L
      val twoStep = U.modify(g)(U.modify(f)(record) match
        case Ior.Right(r)   => r
        case Ior.Both(_, r) => r
        case Ior.Left(_)    => record)
      val composed = U.modify(f.andThen(g))(record)
      val composeOk = (twoStep, composed) match
        case (Ior.Right(a), Ior.Right(b)) => recordsEqual(a, b)
        case _                            => false

      getOk && modIdOk && composeOk
  }

  // covers: AvroTraversal modify(identity) on a valid Basket record === Ior.Right(record),
  //   compose-modify: T.modify(f).andThen(T.modify(g)) == T.modify(g.compose(f)),
  //   replaceIdempotent: T.replace(a).andThen(T.replace(a)) == T.replace(a)
  //
  // EO's `TraversalTests` parameterises on `T[_]` + `Forget[T]` carrier — AvroTraversal doesn't
  // fit (source is a flat IndexedRecord; the multi-focus shape lives entirely inside the carrier).
  // The two universal Traversal laws + the replace-idempotence behaviour are witnessed by hand.
  "AvroTraversal default-Ior forAll: modify-id / compose-modify / replace-idempotent" >> forAll {
    (b: AvroSpecFixtures.Basket, suffix: String, name: String) =>
      val record = AvroSpecFixtures.basketRecord(b)
      val T = codecPrism[AvroSpecFixtures.Basket].items.each.name

      val modIdOk = T.modify(identity[String])(record) match
        case Ior.Right(out) => recordsEqual(out, record)
        case _              => false

      val f: String => String = _.toUpperCase
      val g: String => String = _ + suffix
      val twoStep = (T.modify(f)(record), T) match
        case (Ior.Right(r), tr) => tr.modify(g)(r)
        case _                  => Ior.left(cats.data.Chain.empty[AvroFailure])
      val composed = T.modify(f.andThen(g))(record)
      val composeOk = (twoStep, composed) match
        case (Ior.Right(a), Ior.Right(c)) => recordsEqual(a, c)
        case _                            => false

      val once = T.modify(_ => name)(record)
      val twice = once match
        case Ior.Right(r)   => T.modify(_ => name)(r)
        case Ior.Both(_, r) => T.modify(_ => name)(r)
        case Ior.Left(_)    => once
      val idemOk = (once, twice) match
        case (Ior.Right(a), Ior.Right(c)) => recordsEqual(a, c)
        case _                            => false

      modIdOk && composeOk && idemOk
  }

  // ---- Helpers -----------------------------------------------------

  /** Compare two records via apache-avro's structural compare. Uses the lhs's schema as the
    * comparison schema so two records under different schema instances but identical declared
    * shapes compare equal positionally.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

end AvroPrismLawsSpec

object AvroPrismLawsSpec:

  // ---- Full-cover identity fixture for the discipline Prism laws ----
  //
  // A 2-field case class. PrismTests runs against `codecPrism[Pair]`
  // — the IDENTITY prism whose focus IS Pair — so `reverseGet`
  // re-encodes through the same codec under the same Pair schema and
  // universal `==` works (see class doc for the smoke-test numbers
  // showing why `.fields(_.a, _.b)` does NOT survive `==`).

  case class Pair(a: Int, b: String)

  object Pair:

    given AvroEncoder[Pair] = AvroEncoder.derived
    given AvroDecoder[Pair] = AvroDecoder.derived
    given AvroSchemaFor[Pair] = AvroSchemaFor.derived

  // The NamedTuple type the `.fields(_.name, _.age)` macro synthesises — kept here so the forAll
  // block's `NameAge`-shaped lambdas can ascribe without re-typing.

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

  // Kindlings codecs for the synthesised NamedTuple.
  given AvroEncoder[NameAge] = AvroEncoder.derived
  given AvroDecoder[NameAge] = AvroDecoder.derived
  given AvroSchemaFor[NameAge] = AvroSchemaFor.derived

  // ---- Arbitrary instances for the laws --------------------------

  given arbPair: Arbitrary[Pair] = Arbitrary {
    for
      a <- Arbitrary.arbitrary[Int]
      b <- Gen.alphaNumStr
    yield Pair(a, b)
  }

  /** Arbitrary[IndexedRecord] for the discipline PrismTests source side. Sourced from `Pair` so the
    * full-cover identity prism can round-trip cleanly.
    */
  given arbIndexedRecord: Arbitrary[IndexedRecord] = Arbitrary(
    arbPair.arbitrary.map(p => summon[AvroCodec[Pair]].encode(p).asInstanceOf[IndexedRecord])
  )

  given cogenPair: Cogen[Pair] = Cogen[(Int, String)].contramap(p => (p.a, p.b))

  // ---- Arbitrary instances for the forAll properties --------------
  //
  // Person / Basket / Order arbitraries are local to this spec (mirror the equivalent block in
  // JsonFieldsPrismLawsSpec / AvroFieldsPrismSpec).

  given arbPerson: Arbitrary[AvroSpecFixtures.Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
    yield AvroSpecFixtures.Person(n, a)
  )

  given arbOrder: Arbitrary[AvroSpecFixtures.Order] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      p <- Gen.choose(0.0, 1000.0)
      q <- Gen.choose(0, 100)
    yield AvroSpecFixtures.Order(n, p, q)
  )

  given arbBasket: Arbitrary[AvroSpecFixtures.Basket] = Arbitrary(
    for
      owner <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      items <- Gen.listOfN(3, arbOrder.arbitrary)
    yield AvroSpecFixtures.Basket(owner, items)
  )

end AvroPrismLawsSpec
