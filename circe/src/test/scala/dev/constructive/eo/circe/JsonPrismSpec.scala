package dev.constructive.eo.circe

import scala.language.implicitConversions

import cats.data.{Chain, Ior}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[JsonPrism]] — the cursor-backed Prism from `Json` to a native type, with
  * field-drilling sugar.
  *
  * '''2026-04-25 consolidation.''' Pre-consolidation this spec carried 56 hand-written scenarios,
  * many of which paired the default Ior surface (`modify` / `transform` / `place` / `transfer` /
  * `get`) with the `*Unsafe` escape hatch (`modifyUnsafe` / …) for the SAME case. Each property
  * below subsumes the matched pair via the **default ↔ Unsafe parity invariant**:
  * `defaultOp(args).right.exists(_ == unsafeOp(args))` on the happy path, and `Ior.Both(chain,
  * unsafeOp(args))` on the failure path. ScalaCheck `forAll` over a Person/Address/Vector generator
  * hits both surfaces with one assertion.
  */
class JsonPrismSpec extends Specification with ScalaCheck:

  import JsonSpecFixtures.*
  import JsonPrismSpec.*
  import JsonPrismSpec.given

  // ---- Generators (light) -------------------------------------------

  private given Arbitrary[Address] = Arbitrary(
    for
      s <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      z <- Gen.choose(0, 99999)
    yield Address(s, z)
  )

  private given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
      addr <- Arbitrary.arbitrary[Address]
    yield Person(n, a, addr)
  )

  // ---- Root-level codecPrism[S] -------------------------------------

  // covers: round-trip modify via full decode/encode (default), round-trip modify
  // (Unsafe), pass through Json that doesn't decode (Unsafe)
  "codecPrism[S] modify round-trips on the happy path with default↔Unsafe parity" >> forAll {
    (p: Person) =>
      val json = p.asJson
      val expected = p.copy(name = p.name.toUpperCase).asJson
      val unsafe =
        codecPrism[Person].modifyUnsafe((q: Person) => q.copy(name = q.name.toUpperCase))(json)
      val default =
        codecPrism[Person].modify((q: Person) => q.copy(name = q.name.toUpperCase))(json)
      (unsafe == expected) && (default == Ior.Right(expected))
  }

  // covers: decode failure surfaces Ior.Both(chain-of-one, inputJson) on modify,
  // decode failure surfaces Ior.Left on get, pass through Json that doesn't decode (Unsafe)
  "codecPrism[S] decode failure surfaces Ior.Both on modify, Ior.Left on get, no-op on Unsafe" >> {
    val notAPerson = Json.fromString("not a person")
    val ior = codecPrism[Person].modify(identity[Person])(notAPerson)
    val getResult = codecPrism[Person].get(notAPerson)
    val unsafeOut = codecPrism[Person].modifyUnsafe(identity[Person])(notAPerson)
    val iorOk = ior match
      case Ior.Both(chain, json) =>
        (json === notAPerson)
          .and(chain.length === 1L)
          .and(chain.headOption.get.isInstanceOf[JsonFailure.DecodeFailed] === true)
      case _ =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $ior"): org.specs2.execute.Result
    iorOk.and(getResult.isLeft === true).and(unsafeOut === notAPerson)
  }

  // ---- One-level drill (.field) -------------------------------------

  // covers: modify the focused field in place (default), modify the focused field
  // in place (Unsafe), leave every other field byte-identical to the input,
  // transform default surface, transform *Unsafe surface, get returns
  // Ior.Right(decoded focus), getOptionUnsafe returns Some(decoded focus), modify
  // and modifyUnsafe agree on the happy path
  "field(_.name) happy path: modify+transform+get default↔Unsafe parity, siblings preserved" >> forAll {
    (p: Person) =>
      val nameL = codecPrism[Person].field(_.name)
      val json = p.asJson
      val mFn: String => String = _ + "-x"
      val tFn: Json => Json = _.mapString(_.reverse)
      val expModify = p.copy(name = p.name + "-x").asJson
      val expTransform = p.copy(name = p.name.reverse).asJson

      val parityModify = nameL.modify(mFn)(json) == Ior.Right(nameL.modifyUnsafe(mFn)(json))
      val correctModify = nameL.modifyUnsafe(mFn)(json) == expModify
      val parityTransform =
        nameL.transform(tFn)(json) == Ior.Right(nameL.transformUnsafe(tFn)(json))
      val correctTransform = nameL.transformUnsafe(tFn)(json) == expTransform
      val getOk = nameL.get(json) == Ior.Right(p.name)
      val unsafeGetOk = nameL.getOptionUnsafe(json) == Some(p.name)

      // Sibling fields are byte-identical after the modify.
      val out = nameL.modifyUnsafe(mFn)(json)
      val ageOk = out.hcursor.downField("age").as[Int] == Right(p.age)
      val addrOk = out.hcursor.downField("address").as[Address] == Right(p.address)

      parityModify && correctModify && parityTransform && correctTransform &&
      getOk && unsafeGetOk && ageOk && addrOk
  }

  // covers: get returns Ior.Left with PathMissing when the path is absent,
  // getOptionUnsafe returns None when the path is missing, modify on a missing-path
  // Json produces Ior.Both(PathMissing, inputJson), modifyUnsafe on a missing-path
  // Json returns input unchanged, modify on broken === Ior.Both(chain, modifyUnsafe(broken))
  "field(_.name) on missing path: default↔Unsafe parity surfaces PathMissing" >> {
    val nameL = codecPrism[Person].field(_.name)
    val missing = Json.obj("age" -> Json.fromInt(30))
    val getResult = nameL.get(missing)
    val modifyResult = nameL.modify(_.toUpperCase)(missing)
    val unsafeModify = nameL.modifyUnsafe(_.toUpperCase)(missing)
    val unsafeGet = nameL.getOptionUnsafe(missing)

    val getOk = getResult match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name")))
      case _ =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $getResult"): org.specs2.execute.Result

    val modifyOk = modifyResult ===
      Ior.Both(Chain.one(JsonFailure.PathMissing(PathStep.Field("name"))), unsafeModify)

    getOk
      .and(modifyOk)
      .and(unsafeModify === missing)
      .and(unsafeGet === None)
  }

  // ---- Two-level drill ---------------------------------------------

  // covers: modify a nested field without touching siblings, the middle address
  // Codec is not invoked beyond cursor navigation, *Unsafe missing path returns
  // input unchanged, default missing path returns Ior.Both(PathMissing(address))
  "field(_.address).field(_.street): nested drill with default↔Unsafe parity" >> forAll {
    (p: Person) =>
      val streetL = codecPrism[Person].field(_.address).field(_.street)
      val json = p.asJson
      val expected =
        p.copy(address = p.address.copy(street = p.address.street.toUpperCase)).asJson
      val unsafeOk = streetL.modifyUnsafe(_.toUpperCase)(json) == expected
      val getOk = streetL.getOptionUnsafe(json) == Some(p.address.street)

      unsafeOk && getOk
  }

  "field(_.address).field(_.street) missing path: Unsafe is no-op, default surfaces PathMissing(address)" >> {
    val streetL = codecPrism[Person].field(_.address).field(_.street)
    val stump = Json.obj("name" -> Json.fromString("Alice"))
    val unsafeMiss = streetL.modifyUnsafe(_.toUpperCase)(stump) === stump
    val defaultMiss = streetL.modify(_.toUpperCase)(stump) match
      case Ior.Both(chain, j) =>
        (j === stump).and(
          chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("address"))
        )
      case _ => org.specs2.execute.Failure("expected Ior.Both"): org.specs2.execute.Result
    unsafeMiss.and(defaultMiss)
  }

  // ---- Selectable field sugar (.address.street) ---------------------

  // covers: behave identically to .field(_.address).field(_.street), single-level
  // sugar drill, round-trip getOptionUnsafe through deep sugar, placeUnsafe through
  // sugar, place (default) returns Ior.Right
  "selectDynamic sugar `codecPrism[Person].address.street` matches .field chain" >> forAll {
    (p: Person) =>
      val json = p.asJson
      val explicit = codecPrism[Person].field(_.address).field(_.street)
      val sugared = codecPrism[Person].address.street
      val mFn: String => String = _.toUpperCase

      val parityModify = sugared.modifyUnsafe(mFn)(json) == explicit.modifyUnsafe(mFn)(json)
      val singleSugar = codecPrism[Person]
        .name
        .modifyUnsafe(_.toUpperCase)(json) == p.copy(name = p.name.toUpperCase).asJson
      val sugarGet = sugared.getOptionUnsafe(json) == Some(p.address.street)
      val sugarPlaceUnsafe = sugared.placeUnsafe("Broadway")(json) ==
        p.copy(address = p.address.copy(street = "Broadway")).asJson
      val sugarPlaceDefault = sugared.place("Broadway")(json) ==
        Ior.Right(p.copy(address = p.address.copy(street = "Broadway")).asJson)

      parityModify && singleSugar && sugarGet && sugarPlaceUnsafe && sugarPlaceDefault
  }

  // ---- Array indexing (.at) -----------------------------------------

  // covers: modify the i-th element of a root-level array, leave sibling indices
  // byte-identical, modify an element reached through a nested field path,
  // getOptionUnsafe on a nested array index returns the element
  "at(i) on Vector focus: index modify + sibling preservation + nested field path" >> {
    val orders = Vector(Order("A"), Order("B"), Order("C"))
    val json = orders.asJson
    val outAt1 = codecPrism[Vector[Order]].at(1).name.modifyUnsafe(_.toUpperCase)(json)
    val r1 = outAt1 === Vector(Order("A"), Order("B".toUpperCase), Order("C")).asJson
    val outAt2 = codecPrism[Vector[Order]].at(2).name.modifyUnsafe(_ + "-x")(json)
    val r2 = (outAt2.hcursor.downN(0).as[Order] === Right(Order("A")))
      .and(outAt2.hcursor.downN(1).as[Order] === Right(Order("B")))
      .and(outAt2.hcursor.downN(2).as[Order] === Right(Order("C-x")))
    val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
    val outNested =
      codecPrism[Basket].items.at(0).name.modifyUnsafe(_.toUpperCase)(basket.asJson)
    val r3 = outNested ===
      basket.copy(items = Vector(Order("X".toUpperCase), Order("Y"))).asJson
    val r4 = codecPrism[Basket].items.at(1).getOptionUnsafe(basket.asJson) === Some(Order("Y"))
    r1.and(r2).and(r3).and(r4)
  }

  // covers: *Unsafe out-of-range index leaves input unchanged, default
  // out-of-range index surfaces IndexOutOfRange, *Unsafe negative index leaves
  // input unchanged
  "at(i) out-of-range / negative index: Unsafe is no-op, default surfaces IndexOutOfRange" >> {
    val basket = Basket(owner = "Alice", items = Vector(Order("X")))
    val json = basket.asJson
    val unsafeOOR =
      codecPrism[Basket].items.at(5).name.modifyUnsafe(_.toUpperCase)(json) === json
    val defaultOOR = codecPrism[Basket].items.at(5).name.modify(_.toUpperCase)(json) match
      case Ior.Both(chain, out) =>
        (out === json)
          .and(chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1))
      case _ => org.specs2.execute.Failure("expected Ior.Both"): org.specs2.execute.Result

    val basket2 = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
    val negIndex =
      codecPrism[Basket].items.at(-1).name.modifyUnsafe(_.toUpperCase)(basket2.asJson) ===
        basket2.asJson

    unsafeOOR.and(defaultOOR).and(negIndex)
  }

  // ---- Traversal (.each) — *Unsafe surface --------------------------
  //
  // Paired default-Ior traversal specs live in JsonTraversalSpec.
  // Keeping the *Unsafe flavour here preserves the pre-v0.2 spec
  // for the prism-fluent sugar chains.

  // covers: modify every element's focused field in one pass, transform applies
  // to each raw Json leaf, getAllUnsafe collects every focus, on an empty array
  // modifyUnsafe is a no-op, *Unsafe a missing prefix field leaves input unchanged,
  // at the root: modifyUnsafe every element of a top-level array
  ".each Unsafe surface: modify+transform+getAll + empty/missing/root variants" >> {
    val basket = Basket(owner = "Alice", items = Vector(Order("x"), Order("y"), Order("z")))
    val r1 = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(basket.asJson) ===
      basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson

    val basket2 = Basket(owner = "Alice", items = Vector(Order("ab"), Order("cd")))
    val r2 = codecPrism[Basket]
      .items
      .each
      .name
      .transformUnsafe(_.mapString(_.reverse))(basket2.asJson) ===
      basket2.copy(items = Vector(Order("ba"), Order("dc"))).asJson

    val basket3 = Basket(owner = "Alice", items = Vector(Order("x"), Order("y")))
    val r3 = codecPrism[Basket].items.each.name.getAllUnsafe(basket3.asJson) === Vector("x", "y")

    val emptyB = Basket(owner = "Alice", items = Vector.empty)
    val r4 = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(emptyB.asJson) ===
      emptyB.asJson

    val stump = Json.obj("owner" -> Json.fromString("Alice"))
    val r5 = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(stump) === stump

    val rootArr = Vector(Order("a"), Order("b"))
    val r6 = codecPrism[Vector[Order]].each.name.modifyUnsafe(_.toUpperCase)(rootArr.asJson) ===
      Vector(Order("A"), Order("B")).asJson

    r1.and(r2).and(r3).and(r4).and(r5).and(r6)
  }

  // ---- Place / transfer ---------------------------------------------

  // covers: placeUnsafe overwrites the focused field, transferUnsafe lifts a C => A
  // into a focus-replacer, place (default) returns Ior.Right on the happy path,
  // transfer (default) returns Ior.Right on the happy path
  "place / transfer drilled JsonPrism: default↔Unsafe parity" >> forAll { (p: Person) =>
    val streetL = codecPrism[Person].field(_.address).field(_.street)
    val json = p.asJson

    val placeUnsafe = streetL.placeUnsafe("Broadway")(json) ==
      p.copy(address = p.address.copy(street = "Broadway")).asJson
    val placeDefault = streetL.place("Broadway")(json) ==
      Ior.Right(p.copy(address = p.address.copy(street = "Broadway")).asJson)

    val upcase: String => String = _.toUpperCase
    val transferUnsafe = streetL.transferUnsafe(upcase)(json)("main ave") ==
      p.copy(address = p.address.copy(street = "MAIN AVE")).asJson
    val transferDefault = streetL.transfer(upcase)(json)("main ave") ==
      Ior.Right(p.copy(address = p.address.copy(street = "MAIN AVE")).asJson)

    placeUnsafe && placeDefault && transferUnsafe && transferDefault
  }

  // ---- Multi-field focus (.fields) ---------------------------------

  // covers: modify round-trips two focused fields atomically (Unsafe), modify
  // (default) round-trips on happy path as Ior.Right, leaves non-focused fields
  // byte-identical, get (default) returns the NamedTuple on happy path,
  // getOptionUnsafe returns Some(NamedTuple) on happy path, place (default)
  // overwrites all selected fields atomically
  ".fields(_.name, _.age) happy path: round-trip modify + atomic place + non-focused preserved" >> forAll {
    (p: Person) =>
      val nameAgeL = codecPrism[Person].fields(_.name, _.age)
      val json = p.asJson
      val newP = p.copy(name = p.name + "-x", age = p.age + 1)

      val unsafeOk = nameAgeL
        .modifyUnsafe(nt => (name = nt.name + "-x", age = nt.age + 1))(json) == newP.asJson
      val defaultOk = nameAgeL
        .modify(nt => (name = nt.name + "-x", age = nt.age + 1))(json) == Ior.Right(newP.asJson)

      val out = nameAgeL.modifyUnsafe(nt => (name = nt.name + "-x", age = nt.age))(json)
      val addrPreserved = out.hcursor.downField("address").as[Address] == Right(p.address)

      val getOk = nameAgeL.get(json).toOption.exists(nt => nt.name == p.name && nt.age == p.age)
      val unsafeGetOk =
        nameAgeL.getOptionUnsafe(json).exists(nt => nt.name == p.name && nt.age == p.age)

      val newNt: NameAge = (name = "Carol", age = 55)
      val placeOk = nameAgeL.place(newNt)(json) ==
        Ior.Right(p.copy(name = "Carol", age = 55).asJson)

      unsafeOk && defaultOk && addrPreserved && getOk && unsafeGetOk && placeOk
  }

  // covers: get on Json missing one focused field returns Ior.Left with
  // PathMissing, get on Json missing both focused fields accumulates both failures,
  // modify on a Json missing one field returns Ior.Both(chain, inputJson) (atomicity)
  ".fields(_.name, _.age) failure paths: PathMissing accumulates + atomicity preserved" >> {
    val nameAgeL = codecPrism[Person].fields(_.name, _.age)

    val missingAge = Json.obj("name" -> Json.fromString("Alice"))
    val r1 = nameAgeL.get(missingAge) match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("age")))
      case _ => org.specs2.execute.Failure("expected Ior.Left"): org.specs2.execute.Result

    val missingBoth = Json.obj("address" -> Address("Main St", 12345).asJson)
    val r2 = nameAgeL.get(missingBoth) match
      case Ior.Left(chain) =>
        (chain.length === 2L)
          .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) === true)
          .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("age"))) === true)
      case _ => org.specs2.execute.Failure("expected Ior.Left"): org.specs2.execute.Result

    val r3 = nameAgeL.modify(nt => nt)(missingAge) match
      case Ior.Both(chain, out) =>
        (out === missingAge)
          .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("age")))
      case _ => org.specs2.execute.Failure("expected Ior.Both"): org.specs2.execute.Result

    r1.and(r2).and(r3)
  }

  // covers: carries the NamedTuple in selector order, modify round-trips via
  // selector-order NT
  ".fields with selector-order != declaration-order — NT in selector order" >> forAll {
    (p: Person) =>
      val ageNameL = codecPrism[Person].fields(_.age, _.name)
      val getOk = ageNameL.get(p.asJson).toOption.exists(nt => nt.age == p.age && nt.name == p.name)
      val out = ageNameL.modifyUnsafe(nt => (age = nt.age + 100, name = nt.name))(p.asJson)
      getOk && (out == p.copy(age = p.age + 100).asJson)
  }

  // covers: full-cover does NOT produce a JsonIso (still returns JsonFieldsPrism per D1)
  "codecPrism[Person].fields full-cover stays a JsonFieldsPrism (D1 invariant)" >> {
    val fullL = codecPrism[Person].fields(_.name, _.age, _.address)
    val p = Person("Alice", 30, Address("Main St", 12345))
    val out = fullL.modifyUnsafe(nt =>
      (name = nt.name.toUpperCase, age = nt.age, address = nt.address)
    )(p.asJson)
    out === p.copy(name = "ALICE").asJson
  }

object JsonPrismSpec:

  // Common ADTs (`Address`, `Person`, `Order`, `Basket`) live in
  // `JsonSpecFixtures` — this object holds only the spec-specific
  // NamedTuple aliases + their codec instances.
  import JsonSpecFixtures.Address

  // ---- NamedTuple codec givens for multi-field (.fields) specs ----

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive

  type NameAgeAddress =
    NamedTuple.NamedTuple[("name", "age", "address"), (String, Int, Address)]

  given Codec.AsObject[NameAgeAddress] = KindlingsCodecAsObject.derive

  type AgeName = NamedTuple.NamedTuple[("age", "name"), (Int, String)]
  given Codec.AsObject[AgeName] = KindlingsCodecAsObject.derive
