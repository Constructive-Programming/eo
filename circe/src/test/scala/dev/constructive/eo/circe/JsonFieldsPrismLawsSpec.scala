package dev.constructive.eo.circe

import cats.data.Ior
import dev.constructive.eo.data.Affine
import dev.constructive.eo.laws.discipline.{OptionalTests, SeamTests}
import dev.constructive.eo.laws.{OptionalLaws, SeamLaws}
import dev.constructive.eo.optics.Optic
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Unit 6: witness that [[JsonPrism]] is a lawful Optional (`Affine`-carried), plus ScalaCheck
  * property coverage against both the default Ior surface and the `*Unsafe` escape hatches.
  *
  * The discipline `OptionalTests` RuleSet runs against the *trait binding* — the generic
  * `Optic[Json, Json, A, A, Affine]` supertype — on a full-cover fixture. Beyond it, the root
  * reverseGet round-trips (still honest for the full-cover prism) and the drilled-seam Optional
  * laws (the sibling-preserving generic-extension path the old `Either` carrier got wrong) are
  * witnessed explicitly. forAll properties exercise the Ior-bearing default surface.
  */
class JsonFieldsPrismLawsSpec extends Specification with Discipline with ScalaCheck:

  import JsonSpecFixtures.Person
  import JsonFieldsPrismLawsSpec.*
  import JsonFieldsPrismLawsSpec.given

  // ---- discipline OptionalLaws ------------------------------------
  //
  // JsonPrism is now `Affine`-carried (an Optional, not a Prism): a drilled focus lives inside a
  // document, so rebuilding needs the siblings, which a Prism's reverseGet can't see. The honest
  // discipline ruleset is `OptionalTests`, run against a FULL-COVER fixture where
  // `modify(identity)` reproduces the source Json structurally (circe `Json` `==` is structural,
  // so this holds cleanly).
  //
  // Migrating Prism→Optional drops the two reverseGet round-trip laws — but those were only ever
  // honest for a full-cover / root prism, and we RE-ADD them explicitly below (root reverseGet
  // round-trips) PLUS gain new drilled-seam Optional coverage (the drilled generic extension,
  // which the old Either carrier got wrong: siblings dropped). Net: more laws, not fewer.
  val pairPrism: Optic[Json, Json, PairFocus, PairFocus, Affine] =
    codecPrism[Pair].fields(_.a, _.b)

  checkAll(
    "JsonPrism — codecPrism[Pair].fields(_.a, _.b) (full cover, Optional)",
    new OptionalTests[Json, PairFocus]:
      val laws: OptionalLaws[Json, PairFocus] = new OptionalLaws[Json, PairFocus]:
        val optional = pairPrism
    .optional,
  )

  // Retain the two Prism round-trip laws the discipline migration drops — still honest for the
  // ROOT full-cover prism, whose `reverseGet` re-encodes the whole document.
  "root reverseGet round-trips (getOption∘reverseGet and reverseGet∘getOption)" >> forAll {
    (p: Pair) =>
      val json = p.asJson
      val root = codecPrism[Pair]
      val otherWay = root.get(root.reverseGet(p)) match
        case Ior.Right(read) => read == p
        case _               => false
      val oneWay = root.get(json) match
        case Ior.Right(read) => root.reverseGet(read) == json
        case _               => false
      otherWay && oneWay
  }

  // The regression that would have caught the sibling-drop FIRST — the shared [[SeamLaws]] run on
  // a DRILLED optic through the generic `.modify` / `.replace` seam. The old `Either` carrier's
  // `from(Right) = encoder` fails `seam modify identity` / `seam replace overwrite` here (drops
  // age + address); the full-cover-only laws above could never reach it. `Json` `==` is
  // structural, so plain equality works; a Person-JSON Arbitrary so `.field(_.name)` actually
  // Hits (a Pair JSON would Miss and the law would pass vacuously).
  private val arbPersonJson: Arbitrary[Json] = Arbitrary(arbPerson.arbitrary.map(_.asJson))

  checkAll(
    "JsonPrism drilled seam — codecPrism[Person].field(_.name)",
    new SeamTests[Json, String]:
      val laws: SeamLaws[Json, String] = new SeamLaws[Json, String]:
        val optic = codecPrism[Person].field(_.name)
        val eqv = (a: Json, b: Json) => a == b
    .seam(using arbPersonJson, summon[Arbitrary[String]], summon[Cogen[String]]),
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
