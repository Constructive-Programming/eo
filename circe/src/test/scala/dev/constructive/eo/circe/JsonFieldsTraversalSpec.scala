package dev.constructive.eo.circe

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}

/** Behaviour spec for [[JsonFieldsTraversal]] — the multi-field array traversal produced by
  * `JsonTraversal.fields(_.a, _.b, ...)` (Unit 4).
  *
  * '''2026-04-25 consolidation.''' 12 → 5 named blocks. The pre-image had:
  *
  *   - Three "happy path" specs (Unsafe + default Ior + sibling preservation + getAll +
  *     *Unsafe-agreement) that hit the same code path. Collapsed into one happy-path test that
  *     asserts all those invariants.
  *   - Three default-Ior atomicity specs all asserting "Ior.Both with chain" and only differing in
  *     element layout. Collapsed into one one-broken-elem case + one two-failures-accumulate case.
  *   - Two place/placeUnsafe specs paired into one parity test.
  */
class JsonFieldsTraversalSpec extends JsonSpecBase:

  import JsonFieldsTraversalSpec.*
  import JsonFieldsTraversalSpec.given

  /** Build a basket whose `items` array holds `elems`, then run the multi-field traversal
    * `.modify(f)`.
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

  // ---- Happy path --------------------------------------------------

  // covers: modify (*Unsafe) upper-case name + double price on every element,
  // modify (default) returns Ior.Right on happy path, leaves non-focused fields
  // byte-identical (qty), getAll (default) returns every element's NamedTuple,
  // *Unsafe agreement: modify === Ior.Right(modifyUnsafe) on happy inputs
  "JsonFieldsTraversal happy path: modify+getAll, default↔Unsafe parity, qty preserved" >> {
    val basket = Basket(
      "Alice",
      Vector(
        Order("x", 1.0, qty = 7),
        Order("y", 2.0, qty = 2),
        Order("z", 3.0, qty = 3),
      ),
    )
    val json = basket.asJson
    val f: NamePrice => NamePrice =
      nt => (name = nt.name.toUpperCase, price = nt.price * 2)

    val expected = basket
      .copy(items =
        Vector(
          Order("X", 2.0, qty = 7),
          Order("Y", 4.0, qty = 2),
          Order("Z", 6.0, qty = 3),
        )
      )
      .asJson

    val ior = codecPrism[Basket].items.each.fields(_.name, _.price).modify(f)(json)
    val unsafe = codecPrism[Basket].items.each.fields(_.name, _.price).modifyUnsafe(f)(json)
    val parity = ior === Ior.Right(unsafe)
    val correct = unsafe === expected

    val getAll = codecPrism[Basket].items.each.fields(_.name, _.price).getAll(json)
    val getAllOk = getAll.toOption must beSome.which { vec =>
      (vec.length === 3)
        .and(vec(0).name === "x")
        .and(vec(0).price === 1.0)
        .and(vec(2).name === "z")
        .and(vec(2).price === 3.0)
    }

    // Sibling field qty preserved.
    val qtyOk = unsafe.hcursor.downField("items").downN(0).downField("qty").as[Int] === Right(7)

    parity.and(correct).and(getAllOk).and(qtyOk)
  }

  // ---- Per-element atomicity (D4) ---------------------------------

  // covers: element with ONE focused field missing — chain-of-one PathMissing(price); the
  //   broken element's slots are NOT atomically touched (modify never produces a partial NT)
  //   but the good elements still apply the modify (witnessed via downN(0) and downN(2));
  //   element with BOTH focused fields missing contributes TWO PathMissing entries to the
  //   chain (one per missing field) — same accumulation across cross-element splits
  "atomicity: per-element failures (single-field miss + both-missing + cross-element split)" >> {
    val good = Order("x", 1.0, qty = 1).asJson
    val brokenElem = Json.obj(
      "name" -> Json.fromString("y"),
      "qty" -> Json.fromInt(2),
      // "price" missing
    )
    val singleOk = runFieldsModify(
      Seq(good, brokenElem, Order("z", 3.0, qty = 3).asJson),
      nt => (name = nt.name.toUpperCase, price = nt.price * 2),
    ) match
      case Ior.Both(chain, out) =>
        val outArr = out.hcursor.downField("items").focus.flatMap(_.asArray)
        (out.hcursor.downField("items").downN(0).downField("name").as[String] === Right("X"))
          .and(out.hcursor.downField("items").downN(0).downField("price").as[Double] === Right(2.0))
          .and(outArr.map(_(1)) === Some(brokenElem))
          .and(out.hcursor.downField("items").downN(2).downField("name").as[String] === Right("Z"))
          .and(chain.length === 1L)
          .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("price")))
      case other => ko(s"expected Ior.Both, got $other")

    def assertNamePriceMissing(elems: Seq[Json]) =
      runFieldsModify(elems) match
        case Ior.Both(chain, _) =>
          (chain.length === 2L)
            .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("name"))) === true)
            .and(chain.toList.contains(JsonFailure.PathMissing(PathStep.Field("price"))) === true)
        case other => ko(s"expected Ior.Both, got $other")

    val emptyElem = Json.obj("qty" -> Json.fromInt(2))
    val missingName = Json.obj("price" -> Json.fromDoubleOrNull(1.0), "qty" -> Json.fromInt(1))
    val missingPrice = Json.obj("name" -> Json.fromString("y"), "qty" -> Json.fromInt(2))
    val good2 = Order("z", 3.0, qty = 3).asJson
    val bothMissOk = assertNamePriceMissing(Seq(good, emptyElem))
      .and(assertNamePriceMissing(Seq(missingName, missingPrice, good2)))

    singleOk.and(bothMissOk)
  }

  // covers: empty array — modify returns Ior.Right(input),
  //   missing prefix — modify returns Ior.Left (nothing to iterate);
  //   place / placeUnsafe overwrite every element's focused fields with a constant
  //   (default ↔ Unsafe parity)
  "empty / missing prefix + place / placeUnsafe overwrites" >> {
    val emptyB = Basket("Alice", Vector.empty)
    val emptyJson = emptyB.asJson
    val emptyOk = codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(nt => (name = nt.name, price = nt.price))(emptyJson) === Ior.Right(emptyJson)

    val stump = Json.obj("owner" -> Json.fromString("Alice"))
    val missingOk = codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(nt => (name = nt.name, price = nt.price))(stump)
      .isLeft === true

    val basket = Basket("Alice", Vector(Order("a", 0.5, qty = 1), Order("b", 0.6, qty = 2)))
    val nt: NamePrice = (name = "Z", price = 99.0)
    val expectedZ = basket
      .copy(items = Vector(Order("Z", 99.0, qty = 1), Order("Z", 99.0, qty = 2)))
      .asJson
    val place = codecPrism[Basket].items.each.fields(_.name, _.price).place(nt)(basket.asJson)
    val placeUnsafe =
      codecPrism[Basket].items.each.fields(_.name, _.price).placeUnsafe(nt)(basket.asJson)

    val placeChain = (place === Ior.Right(expectedZ)).and(placeUnsafe === expectedZ)
    emptyOk.and(missingOk).and(placeChain)
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
