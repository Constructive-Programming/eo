package dev.constructive.eo.circe

import io.circe.syntax.*

import scala.language.implicitConversions

/** Behaviour spec for [[JsonTraversal]] — the array-walking traversal introduced alongside the
  * prism in v0.1 and retargeted to the default-Ior / `*Unsafe` pair in v0.2.
  *
  * '''2026-04-25 consolidation.''' 19 → 6 named blocks. Pre-image had:
  *
  *   - Three "happy path" specs (one per modify/getAll/transform) that all hit the same
  *     happy-path traversal logic — collapsed into one parity test.
  *   - Two empty/missing-prefix specs (one per modify+getAll) — collapsed into one.
  *   - Three Ior.Both accumulation specs at three array layouts — collapsed into two
  *     (single-failure + multi-failure).
  *   - Four place/transfer specs (default + Unsafe pairs) — collapsed via parity.
  *   - Three *Unsafe-agreement specs — already covered by the parity assertions above.
  *   - Two Chain accumulation specs — already covered by the multi-failure test.
  */
class JsonTraversalSpec extends JsonSpecBase:

  import JsonSpecFixtures.{Basket, Order}

  /** Build a basket whose items array holds `elems`, then run the per-element name traversal. */
  private def runItemsModify(elems: Json*): Ior[Chain[JsonFailure], Json] =
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)(JsonSpecFixtures.basketRoot(elems))

  // covers: return Ior.Right(updatedJson) when every element succeeds, modify happy
  // === Ior.Right(modifyUnsafe happy), apply f to each raw Json leaf on the happy
  // path (transform), return Ior.Right(Vector) when every element decodes (getAll),
  // getAll happy === Ior.Right(getAllUnsafe happy)
  "JsonTraversal happy path: modify+transform+getAll, default↔Unsafe parity" >> {
    val basket = Basket("Alice", Vector(Order("x"), Order("y"), Order("z")))
    val json = basket.asJson

    val expected = basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson
    val ior = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json)
    val unsafe = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(json)
    val parityM = ior === Ior.Right(unsafe)
    val correctM = unsafe === expected

    val basketAB = Basket("Alice", Vector(Order("ab"), Order("cd")))
    val transformed =
      codecPrism[Basket].items.each.name.transform(_.mapString(_.reverse))(basketAB.asJson)
    val correctT = transformed ===
      Ior.Right(basketAB.copy(items = Vector(Order("ba"), Order("dc"))).asJson)

    val getAllIor = codecPrism[Basket].items.each.name.getAll(json)
    val getAllUnsafe = codecPrism[Basket].items.each.name.getAllUnsafe(json)
    val parityGA = getAllIor === Ior.Right(getAllUnsafe)
    val correctGA = getAllUnsafe === Vector("x", "y", "z")

    parityM.and(correctM).and(correctT).and(parityGA).and(correctGA)
  }

  // covers: return Ior.Right(input) on an empty array (no elements to iterate),
  // yield Chain.empty on happy path even at size zero
  "JsonTraversal empty array: modify returns Ior.Right(input)" >> {
    val basket = Basket("Alice", Vector.empty)
    val json = basket.asJson
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json) === Ior.Right(json)
  }

  // covers: return Ior.Left on a missing prefix field (modify), return Ior.Left
  // on a missing prefix (getAll), modifyUnsafe on broken prefix returns input
  "JsonTraversal missing prefix: default → Ior.Left, Unsafe → input unchanged" >> {
    val stump = Json.obj("owner" -> Json.fromString("Alice"))
    val mResult = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(stump)
    val mOk = mResult match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("items")))
      case _ => ko(s"expected Ior.Left, got $mResult")

    val gaOk = codecPrism[Basket].items.each.name.getAll(stump).isLeft === true
    val unsafeOk = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(stump) === stump

    mOk.and(gaOk).and(unsafeOk)
  }

  // covers: return Ior.Both when one element's suffix misses (element left
  // unchanged, failure recorded), yield a single-element Chain for one per-element
  // failure, return Ior.Both(chain, partial) for getAll
  "JsonTraversal single per-element failure: Ior.Both(chain-of-one, partial output)" >> {
    val good = Order("x").asJson
    val malformed = Json.fromString("oops")
    val result = runItemsModify(good, malformed, Order("z").asJson)
    val mOk = result match
      case Ior.Both(chain, out) =>
        val expected = JsonSpecFixtures.basketRoot(
          Vector(Order("X").asJson, malformed, Order("Z").asJson)
        )
        (out === expected)
          .and(chain === Chain.one(JsonFailure.NotAnObject(PathStep.Field("name"))))
      case _ => ko(s"expected Ior.Both, got $result")

    val arr = Json.arr(Order("x").asJson, Json.fromString("oops"), Order("z").asJson)
    val root = Json.obj("owner" -> Json.fromString("Alice"), "items" -> arr)
    val gaOk = codecPrism[Basket].items.each.name.getAll(root) match
      case Ior.Both(chain, vs) =>
        (vs === Vector("x", "z"))
          .and(chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name")))
      case other => ko(s"expected Ior.Both, got $other")

    mOk.and(gaOk)
  }

  // covers: accumulate multiple per-element failures across the array
  "JsonTraversal accumulates multiple per-element failures across the array" >> {
    val result =
      runItemsModify(
        Json.fromString("oops"), // not an object
        Json.obj(), // object missing "name"
        Order("z").asJson, // succeeds
      )
    result match
      case Ior.Both(chain, out) =>
        (chain.length === 2L)
          .and(chain.toList.contains(JsonFailure.NotAnObject(PathStep.Field("name"))) === true)
          .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) === true)
          .and(
            out.hcursor.downField("items").downN(2).downField("name").as[String] === Right("Z")
          )
      case _ => ko(s"expected Ior.Both, got $result")
  }

  // covers: place overwrites every element's focused field with a constant,
  // placeUnsafe overwrites every element, transfer lifts C => A into element
  // broadcaster, transferUnsafe lifts C => A, place on a missing prefix returns
  // Ior.Left
  "place / placeUnsafe / transfer / transferUnsafe — default↔Unsafe parity + missing prefix" >> {
    val basket = Basket("Alice", Vector(Order("x"), Order("y")))
    val json = basket.asJson
    val expectedZ = basket.copy(items = Vector(Order("Z"), Order("Z"))).asJson

    val place = codecPrism[Basket].items.each.name.place("Z")(json)
    val placeUnsafe = codecPrism[Basket].items.each.name.placeUnsafe("Z")(json)
    val placeOk = (place === Ior.Right(expectedZ)).and(placeUnsafe === expectedZ)

    val upcase: String => String = _.toUpperCase
    val expectedZZZ = basket.copy(items = Vector(Order("ZZZ"), Order("ZZZ"))).asJson
    val transfer = codecPrism[Basket].items.each.name.transfer(upcase)(json)("zzz")
    val transferUnsafe = codecPrism[Basket].items.each.name.transferUnsafe(upcase)(json)("zzz")
    val transferOk = (transfer === Ior.Right(expectedZZZ)).and(transferUnsafe === expectedZZZ)

    val stump = Json.obj("owner" -> Json.fromString("Alice"))
    val placeMissing = codecPrism[Basket].items.each.name.place("Z")(stump).isLeft === true

    placeOk.and(transferOk).and(placeMissing)
  }

// Order / Basket come from JsonSpecFixtures.
