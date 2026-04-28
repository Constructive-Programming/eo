package dev.constructive.eo.circe

import cats.data.Ior
import dev.constructive.eo.laws.PrismLaws
import dev.constructive.eo.laws.discipline.PrismTests
import dev.constructive.eo.optics.Optic
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Unit 6: witness that [[JsonFieldsPrism]] satisfies the three Prism laws, plus ScalaCheck
  * property coverage against both the default Ior surface and the `*Unsafe` escape hatches.
  *
  * The discipline `PrismTests` RuleSet is computed against the *trait binding* — the generic
  * `Optic[Json, Json, A, A, Either]` supertype — which routes through
  * `ForgetfulFunctor[Either].map` rather than the concrete-class Ior surface. That gives the laws
  * direct access to the `to` / `reverseGet` contract, which is what PrismLaws expects.
  *
  * forAll properties separately exercise the Ior-bearing default surface.
  */
class JsonFieldsPrismLawsSpec extends Specification with Discipline with ScalaCheck:

  import JsonSpecFixtures.Person
  import JsonFieldsPrismLawsSpec.*
  import JsonFieldsPrismLawsSpec.given

  // ---- discipline PrismLaws ---------------------------------------
  //
  // The generic Prism laws (ported from Monocle) require `reverseGet`
  // to round-trip through the source `S` — `getOption(s) match { case
  // Some(a) => reverseGet(a) == s }`. For a JsonFieldsPrism whose
  // focus covers every field of the source case class, `reverseGet(nt)`
  // reproduces the full Json; for partial-cover focuses the outer
  // context (non-focused fields) is lost, and the Prism laws genuinely
  // don't hold — that's the expected consequence of reusing the Prism
  // carrier for a structurally-Optional-like shape (see D1 of the
  // multi-field plan).
  //
  // We therefore witness the Prism laws on a FULL-COVER fixture where
  // selecting every field reproduces the source. Partial-cover
  // behaviour is still covered by JsonPrismSpec's behavioural specs
  // and the forAll properties below.
  val pairPrism: Optic[Json, Json, PairFocus, PairFocus, Either] =
    codecPrism[Pair].fields(_.a, _.b)

  checkAll(
    "JsonFieldsPrism — codecPrism[Pair].fields(_.a, _.b) (full cover)",
    new PrismTests[Json, PairFocus]:
      val laws: PrismLaws[Json, PairFocus] = new PrismLaws[Json, PairFocus]:
        val prism = pairPrism
    .prism,
  )

  // ---- forAll properties on the default Ior surface ---------------
  //
  // 2026-04-29 consolidation: 5 forAll-property blocks → 1 composite forAll. The discipline
  // PrismTests checkAll above stays 1:1 (each named law preserved).

  // covers: modify(identity) on valid Person JSON === Ior.Right(inputJson),
  //   modify(f) on valid Person JSON === Ior.Right(modifyUnsafe(f)) on the happy path,
  //   get(valid json) decodes to a NameAge whose name/age match the Person,
  //   placeUnsafe-then-getOptionUnsafe round-trips the focus,
  //   two-step modify on disjoint fields == composed single .fields modify
  "JsonFieldsPrism default-Ior surface forAll: modify-id / parity / get / round-trip / compose" >> forAll {
    (p: Person, suffix: String, newName: String, newAge: Int) =>
      val json = p.asJson
      val L = codecPrism[Person].fields(_.name, _.age)
      val f: NameAge => NameAge = nt => (name = nt.name + suffix, age = nt.age)

      val modIdOk = L.modify(identity[NameAge])(json) == Ior.Right(json)
      val parityOk = L.modify(f)(json) == Ior.Right(L.modifyUnsafe(f)(json))

      val getOk = L.get(json) match
        case Ior.Right(nt) => nt.name == p.name && nt.age == p.age
        case _             => false

      val nt: NameAge = (name = newName, age = newAge)
      val roundTripOk = L.getOptionUnsafe(L.placeUnsafe(nt)(json)) match
        case Some(read) => read.name == newName && read.age == newAge
        case None       => false

      val nameL = codecPrism[Person].field(_.name)
      val ageL = codecPrism[Person].field(_.age)
      val stepByStep =
        ageL.modifyUnsafe((i: Int) => i + 1)(nameL.modifyUnsafe((s: String) => s.toUpperCase)(json))
      val both =
        L.modifyUnsafe((nt: NameAge) => (name = nt.name.toUpperCase, age = nt.age + 1): NameAge)(
          json
        )
      val composeOk = stepByStep == both

      modIdOk && parityOk && getOk && roundTripOk && composeOk
  }

object JsonFieldsPrismLawsSpec:

  // `Address` / `Person` come from `JsonSpecFixtures`; `Pair` and the
  // NamedTuple aliases stay here.
  import JsonSpecFixtures.{Address, Person}

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive

  // ---- Full-cover fixture for the discipline Prism laws ----------
  //
  // A 2-field case class where selecting (a, b) covers everything, so
  // `reverseGet(nt)` reproduces a full-context Json.

  case class Pair(a: Int, b: String)

  object Pair:
    given Codec.AsObject[Pair] = KindlingsCodecAsObject.derive

  type PairFocus = NamedTuple.NamedTuple[("a", "b"), (Int, String)]
  given Codec.AsObject[PairFocus] = KindlingsCodecAsObject.derive

  // ---- Arbitrary + Cogen instances for the laws ------------------

  given arbAddress: Arbitrary[Address] = Arbitrary {
    for
      street <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      zip <- Arbitrary.arbitrary[Int]
    yield Address(street, zip)
  }

  given arbPerson: Arbitrary[Person] = Arbitrary {
    for
      name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      age <- Gen.choose(0, 150)
      addr <- arbAddress.arbitrary
    yield Person(name, age, addr)
  }

  /** Pair arbitrary, lifted to Json — the PrismTests source side for the full-cover fixture. */
  given arbPair: Arbitrary[Pair] = Arbitrary {
    for
      a <- Arbitrary.arbitrary[Int]
      b <- Gen.alphaNumStr
    yield Pair(a, b)
  }

  /** Arbitrary[Json] for the discipline PrismTests — sourced from Pair so the full-cover focus can
    * round-trip cleanly. The forAll property block below uses `Arbitrary[Person]` directly so
    * there's no Json-vs-Person ambiguity.
    */
  given arbJson: Arbitrary[Json] = Arbitrary(arbPair.arbitrary.map(_.asJson))

  /** PairFocus arbitrary — the A side of the full-cover laws. */
  given arbPairFocus: Arbitrary[PairFocus] = Arbitrary {
    arbPair.arbitrary.map(p => (a = p.a, b = p.b): PairFocus)
  }

  given cogenPairFocus: Cogen[PairFocus] =
    Cogen[(Int, String)].contramap(nt => (nt.a, nt.b))

  /** NameAge arbitrary for the forAll properties below. */
  given arbNameAge: Arbitrary[NameAge] = Arbitrary {
    for
      name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      age <- Gen.choose(0, 150)
    yield (name = name, age = age): NameAge
  }

  given cogenNameAge: Cogen[NameAge] =
    Cogen[(String, Int)].contramap(nt => (nt.name, nt.age))
