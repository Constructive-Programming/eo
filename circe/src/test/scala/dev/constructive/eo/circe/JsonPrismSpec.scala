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

  // covers: codecPrism[S] modify round-trips on happy path (default ↔ Unsafe parity),
  //   decode failure surfaces Ior.Both(chain-of-one DecodeFailed, inputJson) on modify,
  //   decode failure surfaces Ior.Left on get,
  //   *Unsafe modify on undecodable Json passes the input through unchanged
  "codecPrism[S]: modify happy round-trip (parity) + decode-failure Ior.Both / Ior.Left / pass-through" >> forAll {
    (p: Person) =>
      val json = p.asJson
      val expected = p.copy(name = p.name.toUpperCase).asJson
      val unsafe =
        codecPrism[Person].modifyUnsafe((q: Person) => q.copy(name = q.name.toUpperCase))(json)
      val default =
        codecPrism[Person].modify((q: Person) => q.copy(name = q.name.toUpperCase))(json)
      val happyOk = (unsafe == expected) && (default == Ior.Right(expected))

      val notAPerson = Json.fromString("not a person")
      val iorBad = codecPrism[Person].modify(identity[Person])(notAPerson)
      val getBad = codecPrism[Person].get(notAPerson)
      val unsafeBad = codecPrism[Person].modifyUnsafe(identity[Person])(notAPerson)
      val iorOk = iorBad match
        case Ior.Both(chain, j) =>
          j == notAPerson && chain.length == 1L &&
          chain.headOption.exists(_.isInstanceOf[JsonFailure.DecodeFailed])
        case _ => false
      val badOk = iorOk && getBad.isLeft && (unsafeBad == notAPerson)

      happyOk && badOk
  }

  // ---- One-level drill (.field) -------------------------------------

  // covers: field(_.name) modify on happy input — default ↔ Unsafe parity,
  //   modify in place leaves every other field byte-identical (age + address),
  //   transform default ↔ Unsafe parity (raw Json leaf rewrite),
  //   get returns Ior.Right(decoded focus), getOptionUnsafe returns Some(decoded focus);
  //   field(_.name) on a missing-path Json: get returns Ior.Left(chain-of-one PathMissing),
  //   getOptionUnsafe returns None, modify returns Ior.Both(PathMissing, inputJson),
  //   modifyUnsafe returns the input unchanged
  "field(_.name): happy parity (modify/transform/get) + sibling preservation + missing-path PathMissing" >> forAll {
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

      val out = nameL.modifyUnsafe(mFn)(json)
      val ageOk = out.hcursor.downField("age").as[Int] == Right(p.age)
      val addrOk = out.hcursor.downField("address").as[Address] == Right(p.address)

      // ---- Missing-path branch ----
      val missing = Json.obj("age" -> Json.fromInt(30))
      val getMiss = nameL.get(missing)
      val modMiss = nameL.modify(_.toUpperCase)(missing)
      val unsafeModMiss = nameL.modifyUnsafe(_.toUpperCase)(missing)
      val unsafeGetMiss = nameL.getOptionUnsafe(missing)

      val missGetOk = getMiss match
        case Ior.Left(chain) =>
          chain.length == 1L &&
          chain.headOption.contains(JsonFailure.PathMissing(PathStep.Field("name")))
        case _ => false

      val missModOk = modMiss ==
        Ior.Both(Chain.one(JsonFailure.PathMissing(PathStep.Field("name"))), unsafeModMiss)

      parityModify && correctModify && parityTransform && correctTransform &&
      getOk && unsafeGetOk && ageOk && addrOk && missGetOk && missModOk &&
      unsafeModMiss == missing && unsafeGetMiss == None
  }

  // ---- Two-level drill ---------------------------------------------

  // covers: nested drill (.field(_.address).field(_.street)) modify without touching siblings,
  //   getOptionUnsafe through nested drill returns Some(decoded leaf),
  //   *Unsafe missing-path drill returns input unchanged,
  //   default-Ior missing-path drill returns Ior.Both(PathMissing(address), inputJson)
  "field(_.address).field(_.street): nested drill happy + missing-path PathMissing" >> forAll {
    (p: Person) =>
      val streetL = codecPrism[Person].field(_.address).field(_.street)
      val json = p.asJson
      val expected =
        p.copy(address = p.address.copy(street = p.address.street.toUpperCase)).asJson
      val unsafeOk = streetL.modifyUnsafe(_.toUpperCase)(json) == expected
      val getOk = streetL.getOptionUnsafe(json) == Some(p.address.street)

      val stump = Json.obj("name" -> Json.fromString("Alice"))
      val unsafeMissOk = streetL.modifyUnsafe(_.toUpperCase)(stump) == stump
      val defaultMissOk = streetL.modify(_.toUpperCase)(stump) match
        case Ior.Both(chain, j) =>
          j == stump &&
          chain.headOption.contains(JsonFailure.PathMissing(PathStep.Field("address")))
        case _ => false

      unsafeOk && getOk && unsafeMissOk && defaultMissOk
  }


  // ---- Array indexing (.at) -----------------------------------------

  // covers: at(i) on root-level Vector modify the i-th element + leave siblings byte-identical,
  //   at(i) on nested Basket.items modifies the right element + leaves others alone,
  //   nested at(i) getOptionUnsafe returns the element,
  //   *Unsafe out-of-range index leaves input unchanged,
  //   default-Ior out-of-range surfaces Ior.Both(IndexOutOfRange, inputJson),
  //   *Unsafe negative index leaves input unchanged
  "at(i) on Vector focus: index modify + sibling preservation + OOR / negative index handling" >> {
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

    // ---- OOR / negative index branches ----
    val basket1 = Basket(owner = "Alice", items = Vector(Order("X")))
    val json1 = basket1.asJson
    val unsafeOOR =
      codecPrism[Basket].items.at(5).name.modifyUnsafe(_.toUpperCase)(json1) === json1
    val defaultOOR = codecPrism[Basket].items.at(5).name.modify(_.toUpperCase)(json1) match
      case Ior.Both(chain, out) =>
        (out === json1)
          .and(chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1))
      case _ => org.specs2.execute.Failure("expected Ior.Both"): org.specs2.execute.Result
    val negIndex =
      codecPrism[Basket].items.at(-1).name.modifyUnsafe(_.toUpperCase)(basket.asJson) ===
        basket.asJson

    r1.and(r2).and(r3).and(r4).and(unsafeOOR).and(defaultOOR).and(negIndex)
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

  // covers: placeUnsafe overwrites the focused field, place (default) returns Ior.Right,
  //   transferUnsafe lifts a C => A into a focus-replacer, transfer (default) returns Ior.Right;
  //   selectDynamic sugar `codecPrism[Person].address.street` behaves identically to
  //   .field(_.address).field(_.street) (parity on modifyUnsafe / getOptionUnsafe),
  //   single-level selectDynamic sugar `codecPrism[Person].name` matches .field(_.name),
  //   sugared placeUnsafe / place (default) on a deep selectDynamic chain
  "place / transfer drilled JsonPrism + selectDynamic sugar: default↔Unsafe parity + sugar parity" >> forAll {
    (p: Person) =>
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

      // selectDynamic sugar parity (the deleted standalone test).
      val sugared = codecPrism[Person].address.street
      val mFn: String => String = _.toUpperCase
      val parityModify = sugared.modifyUnsafe(mFn)(json) == streetL.modifyUnsafe(mFn)(json)
      val singleSugar = codecPrism[Person]
        .name
        .modifyUnsafe(_.toUpperCase)(json) == p.copy(name = p.name.toUpperCase).asJson
      val sugarGet = sugared.getOptionUnsafe(json) == Some(p.address.street)
      val sugarPlaceUnsafe = sugared.placeUnsafe("Broadway")(json) ==
        p.copy(address = p.address.copy(street = "Broadway")).asJson
      val sugarPlaceDefault = sugared.place("Broadway")(json) ==
        Ior.Right(p.copy(address = p.address.copy(street = "Broadway")).asJson)

      placeUnsafe && placeDefault && transferUnsafe && transferDefault &&
      parityModify && singleSugar && sugarGet && sugarPlaceUnsafe && sugarPlaceDefault
  }

  // ---- Multi-field focus (.fields) ---------------------------------

  // covers: .fields(_.name, _.age) modify round-trips two focused fields atomically (Unsafe),
  //   default modify returns Ior.Right on happy path,
  //   non-focused fields (address) preserved byte-identical,
  //   get returns NamedTuple in selector order, getOptionUnsafe returns Some(NT),
  //   place (default) overwrites all selected fields atomically;
  //   missing-one-field get returns Ior.Left(chain-of-one PathMissing),
  //   missing-both-fields get returns Ior.Left(chain-of-two PathMissings),
  //   missing-one-field modify returns Ior.Both(chain, inputJson) — atomicity preserved;
  //   selector-order != declaration-order — NT slots in selector order;
  //   full-cover stays a JsonFieldsPrism, never collapses to a JsonIso (D1 invariant)
  ".fields(_.name, _.age): happy modify+place + missing/both PathMissing + selector-order + full-cover D1" >> forAll {
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

      // ---- Failure paths (constants — not parametric) ----
      val missingAge = Json.obj("name" -> Json.fromString("Alice"))
      val missingOneOk = nameAgeL.get(missingAge) match
        case Ior.Left(chain) =>
          chain.length == 1L &&
          chain.headOption.contains(JsonFailure.PathMissing(PathStep.Field("age")))
        case _ => false

      val missingBoth = Json.obj("address" -> Address("Main St", 12345).asJson)
      val missingBothOk = nameAgeL.get(missingBoth) match
        case Ior.Left(chain) =>
          chain.length == 2L &&
          chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) &&
          chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("age")))
        case _ => false

      val atomicityOk = nameAgeL.modify(nt => nt)(missingAge) match
        case Ior.Both(chain, out) =>
          out == missingAge &&
          chain.headOption.contains(JsonFailure.PathMissing(PathStep.Field("age")))
        case _ => false

      // ---- selector order != declaration order ----
      val ageNameL = codecPrism[Person].fields(_.age, _.name)
      val selOrderGetOk =
        ageNameL.get(json).toOption.exists(nt => nt.age == p.age && nt.name == p.name)
      val selOrderModOk =
        ageNameL.modifyUnsafe(nt => (age = nt.age + 100, name = nt.name))(json) ==
          p.copy(age = p.age + 100).asJson

      // ---- full-cover D1 invariant ----
      val fullL = codecPrism[Person].fields(_.name, _.age, _.address)
      val fullOut = fullL.modifyUnsafe(nt =>
        (name = nt.name.toUpperCase, age = nt.age, address = nt.address)
      )(json)
      val fullCoverOk = fullOut == p.copy(name = p.name.toUpperCase).asJson

      unsafeOk && defaultOk && addrPreserved && getOk && unsafeGetOk && placeOk &&
      missingOneOk && missingBothOk && atomicityOk &&
      selOrderGetOk && selOrderModOk && fullCoverOk
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
