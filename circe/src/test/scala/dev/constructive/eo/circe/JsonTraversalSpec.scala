package dev.constructive.eo.circe

import io.circe.syntax.*

/** Behaviour spec for [[JsonTraversal]] — the array-walking traversal introduced alongside the
  * prism in v0.1 and retargeted to the default-Ior / `*Unsafe` pair in v0.2.
  *
  * Per the plan's Unit 2: every surface gets paired coverage, accumulation scenarios mix failure
  * reasons across elements, and prefix-walk failures return `Ior.Left` (no Json fallback — there's
  * nothing to iterate).
  */
class JsonTraversalSpec extends JsonSpecBase:

  import JsonSpecFixtures.{Basket, Order}

  // ---- Default Ior surface — modify --------------------------------

  "JsonTraversal.modify (default Ior surface)" should {

    "return Ior.Right(updatedJson) when every element succeeds" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y"), Order("z")))
      val result = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(basket.asJson)
      result === Ior.Right(
        basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson
      )
    }

    "return Ior.Right(input) on an empty array (no elements to iterate)" >> {
      val basket = Basket("Alice", Vector.empty)
      val json = basket.asJson
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json) === Ior.Right(json)
    }

    "return Ior.Left on a missing prefix field (nothing to iterate)" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      val result = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(stump)
      result match
        case Ior.Left(chain) =>
          (chain.length === 1L).and(
            chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("items"))
          )
        case _ => ko(s"expected Ior.Left, got $result")
    }

    // Both Ior.Both scenarios below share the basket-build pattern; the
    // `JsonSpecFixtures.basketRoot` helper builds the wrapper JSON, this
    // local helper just pipes it through the traversal under test.
    def runItemsModify(elems: Json*): Ior[Chain[JsonFailure], Json] =
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)(JsonSpecFixtures.basketRoot(elems))

    "return Ior.Both when one element's suffix misses (element left unchanged, failure recorded)" >> {
      // A basket whose items array has one malformed element (a string
      // instead of an Order).
      val good = Order("x").asJson
      val malformed = Json.fromString("oops")
      val result = runItemsModify(good, malformed, Order("z").asJson)
      result match
        case Ior.Both(chain, out) =>
          val expectedArr = Json.arr(
            Order("X").asJson,
            malformed,
            Order("Z").asJson,
          )
          val expected =
            Json.obj("owner" -> Json.fromString("Alice"), "items" -> expectedArr)
          (out === expected)
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name")))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "accumulate multiple per-element failures across the array" >> {
      val result =
        runItemsModify(
          Json.fromString("oops"), // not an object
          Json.obj(), // object missing "name"
          Order("z").asJson, // succeeds
        )
      result match
        case Ior.Both(chain, out) =>
          // chain length 2: NotAnObject + PathMissing
          (chain.length === 2L)
            .and(chain.toList.contains(JsonFailure.NotAnObject(PathStep.Field("name"))) === true)
            .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) === true)
            .and(
              // The third element (the one that succeeded) is uppercased
              // in the result.
              out.hcursor.downField("items").downN(2).downField("name").as[String] ===
                Right("Z")
            )
        case _ => ko(s"expected Ior.Both, got $result")
    }
  }

  // ---- Default Ior surface — getAll --------------------------------

  "JsonTraversal.getAll (default Ior surface)" should {

    "return Ior.Right(Vector) when every element decodes" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      codecPrism[Basket].items.each.name.getAll(basket.asJson) ===
        Ior.Right(Vector("x", "y"))
    }

    "return Ior.Both(chain, partial) when some elements miss" >> {
      val arr = Json.arr(Order("x").asJson, Json.fromString("oops"), Order("z").asJson)
      val root = Json.obj("owner" -> Json.fromString("Alice"), "items" -> arr)
      val result = codecPrism[Basket].items.each.name.getAll(root)
      result match
        case Ior.Both(chain, vs) =>
          (vs === Vector("x", "z"))
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name")))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "return Ior.Left on a missing prefix" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      codecPrism[Basket].items.each.name.getAll(stump).isLeft === true
    }
  }

  // ---- Default Ior surface — transform -----------------------------

  "JsonTraversal.transform (default Ior surface)" should {

    "apply f to each raw Json leaf on the happy path" >> {
      val basket = Basket("Alice", Vector(Order("ab"), Order("cd")))
      val result =
        codecPrism[Basket].items.each.name.transform(_.mapString(_.reverse))(basket.asJson)
      result === Ior.Right(basket.copy(items = Vector(Order("ba"), Order("dc"))).asJson)
    }
  }

  // ---- Default Ior surface — place / transfer ---------------------

  "JsonTraversal.place / transfer (new in v0.2)" should {

    "place overwrites every element's focused field with a constant" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val result = codecPrism[Basket].items.each.name.place("Z")(basket.asJson)
      result ===
        Ior.Right(basket.copy(items = Vector(Order("Z"), Order("Z"))).asJson)
    }

    "placeUnsafe overwrites every element's focused field with a constant" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val out = codecPrism[Basket].items.each.name.placeUnsafe("Z")(basket.asJson)
      out === basket.copy(items = Vector(Order("Z"), Order("Z"))).asJson
    }

    "transfer lifts a C => A into an element broadcaster" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val upcase: String => String = _.toUpperCase
      val out = codecPrism[Basket].items.each.name.transfer(upcase)(basket.asJson)("zzz")
      out === Ior.Right(basket.copy(items = Vector(Order("ZZZ"), Order("ZZZ"))).asJson)
    }

    "transferUnsafe lifts a C => A into an element broadcaster" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val upcase: String => String = _.toUpperCase
      val out = codecPrism[Basket].items.each.name.transferUnsafe(upcase)(basket.asJson)("zzz")
      out === basket.copy(items = Vector(Order("ZZZ"), Order("ZZZ"))).asJson
    }

    "place on a missing prefix returns Ior.Left" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      codecPrism[Basket].items.each.name.place("Z")(stump).isLeft === true
    }
  }

  // ---- *Unsafe agreement -------------------------------------------

  "*Unsafe agreement on happy paths" should {

    "modify happy === Ior.Right(modifyUnsafe happy)" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val json = basket.asJson
      val iorOut = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json)
      val unsafeOut = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(json)
      iorOut === Ior.Right(unsafeOut)
    }

    "getAll happy === Ior.Right(getAllUnsafe happy)" >> {
      val basket = Basket("Alice", Vector(Order("x"), Order("y")))
      val json = basket.asJson
      val iorOut = codecPrism[Basket].items.each.name.getAll(json)
      val unsafeOut = codecPrism[Basket].items.each.name.getAllUnsafe(json)
      iorOut === Ior.Right(unsafeOut)
    }

    "modifyUnsafe on broken prefix returns input (silent escape hatch)" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(stump) === stump
    }
  }

  // ---- Chain shape --------------------------------------------------

  "Chain accumulation shape" should {

    "yield Chain.empty on happy path even at size zero" >> {
      val basket = Basket("Alice", Vector.empty)
      val result = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(basket.asJson)
      result.isRight === true
    }

    "yield a single-element Chain for one per-element failure" >> {
      val arr = Json.arr(Json.fromString("oops"), Order("y").asJson)
      val root = Json.obj("owner" -> Json.fromString("Alice"), "items" -> arr)
      val result = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(root)
      val chainOpt = result.left
      chainOpt === Some(Chain.one(JsonFailure.NotAnObject(PathStep.Field("name"))))
    }
  }

// Order / Basket come from JsonSpecFixtures — see the import above.
