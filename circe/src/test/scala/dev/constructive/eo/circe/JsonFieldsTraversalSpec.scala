package dev.constructive.eo.circe

import io.circe.syntax.*
import io.circe.{Codec, Json}
import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** Behaviour spec for [[JsonFieldsTraversal]] — the multi-field array traversal produced by
  * `JsonTraversal.fields(_.a, _.b, ...)` (Unit 4).
  *
  * Exercises per-element atomicity (NamedTuple cannot be partial — D4), per-element failure
  * accumulation, and the new `place` / `transfer` surface.
  */
class JsonFieldsTraversalSpec extends JsonSpecBase:

  import JsonFieldsTraversalSpec.*
  import JsonFieldsTraversalSpec.given

  // ---- Happy path --------------------------------------------------

  "codecPrism[Basket].items.each.fields(_.name, _.price)" should {

    "modify (*Unsafe) upper-case name + double price on every element" >> {
      val basket = Basket(
        "Alice",
        Vector(
          Order("x", 1.0, qty = 1),
          Order("y", 2.0, qty = 2),
          Order("z", 3.0, qty = 3),
        ),
      )
      val json = basket.asJson
      val out =
        codecPrism[Basket]
          .items
          .each
          .fields(_.name, _.price)
          .modifyUnsafe(nt => (name = nt.name.toUpperCase, price = nt.price * 2))(json)
      out ===
        basket
          .copy(items =
            Vector(
              Order("X", 2.0, qty = 1),
              Order("Y", 4.0, qty = 2),
              Order("Z", 6.0, qty = 3),
            )
          )
          .asJson
    }

    "modify (default) returns Ior.Right on happy path" >> {
      val basket = Basket("Alice", Vector(Order("x", 1.0, qty = 1)))
      val json = basket.asJson
      val out = codecPrism[Basket]
        .items
        .each
        .fields(_.name, _.price)
        .modify(nt => (name = nt.name.toUpperCase, price = nt.price * 2))(json)
      out === Ior.Right(basket.copy(items = Vector(Order("X", 2.0, qty = 1))).asJson)
    }

    "leaves non-focused fields byte-identical (qty)" >> {
      val basket = Basket("Alice", Vector(Order("x", 1.0, qty = 7)))
      val json = basket.asJson
      val out =
        codecPrism[Basket]
          .items
          .each
          .fields(_.name, _.price)
          .modifyUnsafe(nt => (name = nt.name + "-x", price = nt.price))(json)
      out.hcursor.downField("items").downN(0).downField("qty").as[Int] === Right(7)
    }

    "getAll (default) returns every element's NamedTuple on happy path" >> {
      val basket = Basket("Alice", Vector(Order("x", 1.0, qty = 1), Order("y", 2.0, qty = 2)))
      val result = codecPrism[Basket].items.each.fields(_.name, _.price).getAll(basket.asJson)
      result.toOption must beSome.which { vec =>
        (vec.length === 2)
          .and(vec(0).name === "x")
          .and(vec(0).price === 1.0)
          .and(vec(1).name === "y")
          .and(vec(1).price === 2.0)
      }
    }
  }

  // ---- Per-element atomicity (D4) ---------------------------------
  //
  // The three scenarios below all share the shape "build a basket with a fixed
  // element list, run `.items.each.fields(_.name, _.price).modify(f)`, assert the
  // chain's content". The previous hand-written copies of that build-and-run
  // pattern were the source of the 7-line and 17-line jscpd clones; factoring
  // them as one helper plus per-test assertions removes the duplication while
  // keeping each scenario's intent visible at the call site.

  /** Build a basket whose `items` array holds `elems`, then run the multi-field traversal
    * `.modify(f)`. The basket-wrapper build lives on `JsonSpecFixtures`; this helper just pipes
    * through the traversal under test.
    */
  private def runFieldsModify(
      elems: Seq[Json],
      f: NamePrice => NamePrice = identity,
  ): Ior[Chain[JsonFailure], Json] =
    codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(f)(JsonSpecFixtures.basketRoot(elems))

  "JsonFieldsTraversal default-Ior per-element atomicity" should {

    "leave element unchanged when one of its focused fields misses, recording failure" >> {
      val good = Order("x", 1.0, qty = 1).asJson
      val brokenElem = Json.obj(
        "name" -> Json.fromString("y"),
        "qty" -> Json.fromInt(2),
        // "price" missing
      )
      val result =
        runFieldsModify(
          Seq(good, brokenElem, Order("z", 3.0, qty = 3).asJson),
          nt => (name = nt.name.toUpperCase, price = nt.price * 2),
        )
      result match
        case Ior.Both(chain, out) =>
          val outArr = out.hcursor.downField("items").focus.flatMap(_.asArray)
          // First element was uppercased and doubled.
          (out.hcursor.downField("items").downN(0).downField("name").as[String] ===
            Right("X"))
            .and(
              out.hcursor.downField("items").downN(0).downField("price").as[Double] ===
                Right(2.0)
            )
            // Middle element is byte-identical to input (the broken one).
            .and(outArr.map(_(1)) === Some(brokenElem))
            // Third element was uppercased and doubled.
            .and(
              out.hcursor.downField("items").downN(2).downField("name").as[String] ===
                Right("Z")
            )
            // Chain has one failure: PathMissing(price) for element 1.
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("price")))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    // Both scenarios below assert the same Ior.Both shape (chain has length 2,
    // contains PathMissing(name) and PathMissing(price)) — they only differ in
    // how the missing-name / missing-price elements are arranged in the input
    // array. Factor the assertion once to remove the 10-line clone.
    def assertNamePriceMissing(elems: Seq[Json]) =
      runFieldsModify(elems) match
        case Ior.Both(chain, _) =>
          (chain.length === 2L)
            .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) === true)
            .and(
              chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("price"))) === true
            )
        case other => ko(s"expected Ior.Both, got $other")

    "element with BOTH fields missing contributes TWO entries to the chain" >> {
      val good = Order("x", 1.0, qty = 1).asJson
      val emptyElem = Json.obj("qty" -> Json.fromInt(2))
      assertNamePriceMissing(Seq(good, emptyElem))
    }

    "accumulate across elements: two different broken elements contribute two entries" >> {
      val missingName = Json.obj("price" -> Json.fromDoubleOrNull(1.0), "qty" -> Json.fromInt(1))
      val missingPrice = Json.obj("name" -> Json.fromString("y"), "qty" -> Json.fromInt(2))
      val good = Order("z", 3.0, qty = 3).asJson
      assertNamePriceMissing(Seq(missingName, missingPrice, good))
    }
  }

  // ---- Empty array / missing prefix --------------------------------

  "forgiving on empty array / missing prefix" should {

    "empty array: modify returns Ior.Right(input)" >> {
      val basket = Basket("Alice", Vector.empty)
      val json = basket.asJson
      codecPrism[Basket]
        .items
        .each
        .fields(_.name, _.price)
        .modify(nt => (name = nt.name, price = nt.price))(json) === Ior.Right(json)
    }

    "missing prefix: modify returns Ior.Left (nothing to iterate)" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      codecPrism[Basket]
        .items
        .each
        .fields(_.name, _.price)
        .modify(nt => (name = nt.name, price = nt.price))(stump)
        .isLeft === true
    }
  }

  // ---- place / transfer on multi-field traversal -------------------

  "place / transfer on multi-field traversal" should {

    "place overwrites every element's focused fields with a constant" >> {
      val basket = Basket("Alice", Vector(Order("a", 0.5, qty = 1), Order("b", 0.6, qty = 2)))
      val nt: NamePrice = (name = "Z", price = 99.0)
      val out =
        codecPrism[Basket].items.each.fields(_.name, _.price).place(nt)(basket.asJson)
      out ===
        Ior.Right(
          basket
            .copy(items =
              Vector(
                Order("Z", 99.0, qty = 1),
                Order("Z", 99.0, qty = 2),
              )
            )
            .asJson
        )
    }

    "placeUnsafe overwrites every element's focused fields with a constant" >> {
      val basket = Basket("Alice", Vector(Order("a", 0.5, qty = 1)))
      val nt: NamePrice = (name = "Z", price = 99.0)
      val out =
        codecPrism[Basket].items.each.fields(_.name, _.price).placeUnsafe(nt)(basket.asJson)
      out === basket.copy(items = Vector(Order("Z", 99.0, qty = 1))).asJson
    }
  }

  // ---- *Unsafe agreement -------------------------------------------

  "*Unsafe agreement on happy paths" should {

    "modify === Ior.Right(modifyUnsafe) on happy inputs" >> {
      val basket = Basket(
        "Alice",
        Vector(Order("x", 1.0, qty = 1), Order("y", 2.0, qty = 2)),
      )
      val json = basket.asJson
      val f: NamePrice => NamePrice = nt => (name = nt.name.toUpperCase, price = nt.price * 2)
      val iorOut = codecPrism[Basket].items.each.fields(_.name, _.price).modify(f)(json)
      val unsafeOut =
        codecPrism[Basket].items.each.fields(_.name, _.price).modifyUnsafe(f)(json)
      iorOut === Ior.Right(unsafeOut)
    }
  }

object JsonFieldsTraversalSpec:

  case class Order(name: String, price: Double, qty: Int)

  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

  case class Basket(owner: String, items: Vector[Order])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
  given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive
