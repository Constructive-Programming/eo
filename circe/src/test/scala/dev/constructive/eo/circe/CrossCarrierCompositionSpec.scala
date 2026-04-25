package dev.constructive.eo.circe

import cats.data.Ior
import dev.constructive.eo.generics.lens
import dev.constructive.eo.optics.{AffineFold, Lens, Optic}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.specs2.mutable.Specification

/** Unit 7: cross-carrier composition regression specs.
  *
  * The five named chains below witness that JsonPrism / JsonFieldsPrism compose cleanly with the
  * rest of the cats-eo optics family — plain Scala-level Lenses, macro-derived Lenses from
  * `dev.constructive.eo.generics`, and `AffineFold`. Chains (4) and (5) use the MANUAL composition
  * idiom because `JsonTraversal` is deliberately outside the Optic trait (see
  * `site/docs/concepts.md` — "would have to invent an artificial `to`"). Future Considerations in
  * `docs/plans/2026-04-23-005-feat-circe-multi-field-plus-observable-failure-plan.md` tracks
  * whether to lift that restriction.
  *
  * Every scenario asserts at least one success case AND one diagnostic case (Ior.Both / Ior.Left
  * path) — cross-carrier composition regressions would surface in either the typecheck or the
  * diagnostic output.
  */
class CrossCarrierCompositionSpec extends Specification:

  import JsonSpecFixtures.*
  import CrossCarrierCompositionSpec.*
  import CrossCarrierCompositionSpec.given

  // ================================================================
  // Scenario 1: Plain Lens → multi-field JsonPrism → AffineFold
  // ================================================================

  "(1) plain Lens → JsonFieldsPrism → AffineFold, composed read-only chain" should {

    // Notes on the chain shape (important escalation point for the
    // Unit 7 spec ledger):
    //
    //  - `box: Lens[Box, Box, Json, Json]` has carrier `Tuple2`.
    //  - `personFields: JsonFieldsPrism[NameAge]` has carrier `Either`.
    //  - `adultName: AffineFold[NameAge, String]` has carrier `Affine`.
    //
    // Morph.bothViaAffine bridges the first step (Tuple2 ⊔ Either →
    // Affine) cleanly. But AffineFold's `T = Unit` type signature
    // means the standard `.andThen` chain shape
    // `Optic[A, B, C, D, F]` requires `A == B` at the inner step.
    // This mismatch is NOT specific to this plan — it's pre-existing
    // behaviour documented in tests/src/test/scala/eo/OpticsBehaviorSpec.scala
    // ("Direct Lens.andThen(AffineFold) is not well-typed — AffineFold's
    // T = Unit mismatches the outer Lens's B slot"). The idiomatic
    // route is to build an Optional/Prism chain first and narrow via
    // `AffineFold.fromOptional(...)` or `AffineFold.fromPrism(...)`.
    //
    // We therefore express scenario (1) as a two-step composition:
    // first compose the Lens + JsonFieldsPrism to an Affine-carrier
    // chain, then wrap via `AffineFold.fromPrism` on the
    // `Optic[..., Either]` intermediate before it collapses to Affine.

    val box =
      Lens[Box, Json](_.payload, (b, j) => b.copy(payload = j))

    val personFields: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)

    // Inner AffineFold narrowing a NameAge → Option[name]. Lives on the
    // Affine carrier.
    val adultName: AffineFold[NameAge, String] =
      AffineFold(nt => Option.when(nt.age >= 18)(nt.name))

    // Step 1: compose the outer Lens with the JsonFieldsPrism through
    // the cross-carrier Morph. Result carrier is Affine.
    val boxToFields: Optic[Box, Box, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      box.andThen(personFields)

    // Step 2: the full Affine-carrier chain. AffineFold's getOption
    // extension sits on any Optic with Affine carrier, so we express
    // scenario (1)'s read as `boxToFields.getOption(_).filter(adult-rule)`
    // which is the same observable behaviour as "Lens → Prism →
    // AffineFold" for the getOption surface.
    val chain: Box => Option[String] =
      (b: Box) => boxToFields.getOption(b).flatMap(nt => adultName.getOption(nt))

    val adultBox = Box(Person("Alice", 30, Address("Main St", 1)).asJson)
    val minorBox = Box(Person("Bob", 12, Address("Oak Ave", 2)).asJson)

    "chain on an adult payload returns Some(name)" >> {
      chain(adultBox) === Some("Alice")
    }

    "chain on a minor payload returns None (AffineFold miss)" >> {
      chain(minorBox) === None
    }

    "chain on a payload that doesn't decode returns None (diagnostic case)" >> {
      val broken = Box(Json.fromString("not a person"))
      chain(broken) === None
    }
  }

  // ================================================================
  // Scenario 2: generics lens → single-field JsonPrism, native .andThen
  // ================================================================

  "(2) generics lens[S](_.field) → single-field JsonPrism, native .andThen" should {

    // Bind to a hand-written Lens of Envelope → Json here to keep type
    // inference simple at the .andThen call site; scenario 3 exercises
    // the generics-lens binding explicitly.
    val outer =
      Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val inner: Optic[Json, Json, String, String, Either] =
      codecPrism[Person].field(_.name)

    val chain = outer.andThen(inner)

    val validEnv = Envelope("env", Person("Alice", 30, Address("Main St", 1)).asJson)
    val emptyEnv = Envelope("env", Json.obj())

    "getOption reads the embedded name on a valid envelope (Affine via bothViaAffine)" >> {
      // Reading through the composed trait-bound chain works cleanly:
      // Lens.to grabs the Json payload, JsonPrism.to decodes the name
      // to String, Affine.getOption witnesses the `Some("Alice")` hit.
      chain.getOption(validEnv) === Some("Alice")
    }

    "getOption returns None when the payload doesn't decode (Affine miss path)" >> {
      chain.getOption(emptyEnv) === None
    }

    "diagnostic case: direct JsonPrism surface surfaces PathMissing" >> {
      // Important design note: `.modify` on the trait-bound cross-
      // carrier chain goes through the generic Optic.modify extension,
      // which on the Either carrier calls
      //   `from(Right(a'))` where `from(Right(a')) = encoder(a')`.
      // That drops the outer Json context — the focused field's value
      // is rebuilt to a bare JSON (e.g. `Json.fromString("ALICE")`) and
      // the surrounding Person fields are lost. This is the same
      // reason JsonPrism ships a concrete `.modifyUnsafe` override
      // (and the Ior-bearing `.modify`) that walk the path natively —
      // those methods know about the parent context.
      //
      // For diagnostics on the direct JsonPrism surface the Ior result
      // is clean:
      val direct = codecPrism[Person].field(_.name)
      val result = direct.modify(_.toUpperCase)(Json.obj())
      result match
        case Ior.Both(c, _) =>
          c.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
        case _ => ko(s"expected Ior.Both, got $result")
    }
  }

  // ================================================================
  // Scenario 3: generics lens → multi-field JsonPrism (.fields),
  //              native .andThen
  // ================================================================

  "(3) generics lens[S](_.field) → multi-field JsonPrism (.fields), native .andThen" should {

    // Here we pull the outer lens in through dev.constructive.eo.generics to witness
    // that the macro-derived Lens composes identically to a hand-rolled
    // one against a multi-field JsonFieldsPrism. We widen the
    // generics-macro output to a plain Optic[Envelope, Envelope, Json,
    // Json, Tuple2] so type inference on the subsequent .andThen has a
    // clean upper bound — the macro's transparent-inline-refined
    // output type otherwise carries a `(payload : Json)` field
    // refinement that interacts awkwardly with the cross-carrier
    // Composer search.
    val outerGen: Optic[Envelope, Envelope, Json, Json, Tuple2] =
      lens[Envelope](_.payload)
    val inner: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)

    // Cross-carrier Tuple2 + Either bridges through Morph.bothViaAffine
    // → Affine. Narrow the chain's static type so `.modify` returns a
    // concrete `Envelope => Envelope` (the generic Optic.modify
    // extension on Affine).
    val chain: Optic[Envelope, Envelope, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      outerGen.andThen(inner)

    val validEnv = Envelope("env", Person("Alice", 30, Address("Main St", 1)).asJson)

    "modify updates name and age through the cross-carrier chain" >> {
      // Same caveat as scenario (2): the trait-bound cross-carrier
      // chain uses `JsonFieldsPrism.from(Right(nt))` = `encoder(nt)`,
      // which yields a JsonObject with ONLY the focused fields. Outer
      // context (e.g. Person.address) is lost through this specific
      // `.modify` surface. Users wanting in-place outer-context-
      // preserving update should reach for the concrete class's
      // `.modifyUnsafe` or Ior-bearing `.modify` directly.
      val f: NameAge => NameAge =
        nt => (name = nt.name.toUpperCase, age = nt.age + 1)
      val out: Envelope = chain.modify(f)(validEnv)
      (out.payload.hcursor.downField("name").as[String] === Right("ALICE"))
        .and(out.payload.hcursor.downField("age").as[Int] === Right(31))
    }

    "concrete-class Ior surface preserves address (direct call)" >> {
      // Demonstrating the concrete-call path: address IS preserved
      // when using JsonFieldsPrism's concrete `.modify` directly.
      val p = Person("Alice", 30, Address("Main St", 1))
      val concrete = codecPrism[Person].fields(_.name, _.age)
      val f: NameAge => NameAge =
        nt => (name = nt.name.toUpperCase, age = nt.age + 1)
      val result = concrete.modify(f)(p.asJson)
      result match
        case Ior.Right(json) =>
          json.hcursor.downField("address").as[Address] === Right(Address("Main St", 1))
        case _ => ko(s"expected Ior.Right, got $result")
    }

    "diagnostic case: missing `age` surfaces through the direct Ior surface" >> {
      val payload = Json.obj("name" -> Json.fromString("Alice"))
      val directResult = codecPrism[Person].fields(_.name, _.age).get(payload)
      directResult match
        case Ior.Left(c) =>
          c.toList.contains(JsonFailure.PathMissing(PathStep.Field("age"))) === true
        case _ => ko(s"expected Ior.Left, got $directResult")
    }
  }

  // ================================================================
  // Scenario 4: generics lens → JsonTraversal, MANUAL composition
  // ================================================================

  "(4) generics lens[S](_.field) → JsonTraversal, manual composition idiom" should {

    // JsonTraversal deliberately does not extend the Optic trait (see
    // site/docs/concepts.md — "would have to invent an artificial
    // `to`"). Native .andThen on a JsonTraversal is therefore NOT
    // available. The idiomatic chain composes at the Scala level via
    // `outerLens.modify(trav.modifyUnsafe(f))(env)` or the Ior
    // equivalent.
    //
    // Future Considerations in
    // docs/plans/2026-04-23-005-feat-circe-multi-field-plus-observable-failure-plan.md
    // tracks whether to lift JsonTraversal into the Optic trait.

    val outer =
      Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val trav = codecPrism[Basket].items.each.name

    val basket = Basket("Alice", Vector(Order("x"), Order("y"), Order("z")))
    val env = Envelope("env", basket.asJson)

    "manual composition: outer.modify(trav.modifyUnsafe(f))(env) works" >> {
      val updated = outer.modify(trav.modifyUnsafe(_.toUpperCase))(env)
      updated.payload ===
        basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson
    }

    "manual composition with Ior: unwrap via getOrElse-style" >> {
      val updated = outer.modify { (j: Json) =>
        trav.modify(_.toUpperCase)(j) match
          case Ior.Right(v)   => v
          case Ior.Both(_, v) => v
          case Ior.Left(_)    => j
      }(env)
      updated.payload ===
        basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson
    }

    "diagnostic case: traversal.modify directly observes chain failures" >> {
      val brokenArr = Json.arr(Json.fromString("oops"), Order("y").asJson)
      val brokenBasket =
        Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenArr)
      val result = trav.modify(_.toUpperCase)(brokenBasket)
      result match
        case Ior.Both(c, _) =>
          c.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name"))
        case _ => ko(s"expected Ior.Both, got $result")
    }
  }

  // ================================================================
  // Scenario 5: generics lens → multi-field JsonTraversal
  //              (.each.fields), MANUAL composition
  // ================================================================

  "(5) generics lens[S](_.field) → multi-field JsonTraversal (.each.fields), manual composition" should {

    // Same manual-composition idiom as (4). JsonFieldsTraversal (new
    // in Unit 4) is not an Optic either — matches JsonTraversal's
    // design choice. Documented in the same Future Considerations
    // entry referenced in scenario (4).

    val outer =
      Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val fieldsT = codecPrism[MultiBasket].items.each.fields(_.name, _.price)

    val mbasket = MultiBasket(
      "Alice",
      Vector(MItem("x", 1.0, 1), MItem("y", 2.0, 2)),
    )
    val env = Envelope("env", mbasket.asJson)

    "manual composition happy path (Unsafe)" >> {
      val updated = outer.modify(
        fieldsT.modifyUnsafe(nt => (name = nt.name.toUpperCase, price = nt.price * 2))
      )(env)
      updated.payload ===
        mbasket
          .copy(items =
            Vector(
              MItem("X", 2.0, 1),
              MItem("Y", 4.0, 2),
            )
          )
          .asJson
    }

    "manual composition with default Ior surface" >> {
      val updated = outer.modify { (j: Json) =>
        fieldsT.modify(nt => (name = nt.name, price = nt.price * 2))(j) match
          case Ior.Right(v)   => v
          case Ior.Both(_, v) => v
          case Ior.Left(_)    => j
      }(env)
      updated.payload ===
        mbasket
          .copy(items =
            Vector(
              MItem("x", 2.0, 1),
              MItem("y", 4.0, 2),
            )
          )
          .asJson
    }

    "diagnostic case: missing per-element field surfaces through direct .modify" >> {
      val brokenItems = Json.arr(
        Json.obj("name" -> Json.fromString("x"), "qty" -> Json.fromInt(1)),
        // missing price
        MItem("y", 2.0, 2).asJson,
      )
      val brokenBasket =
        Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenItems)
      val result =
        fieldsT.modify(nt => (name = nt.name, price = nt.price))(brokenBasket)
      result match
        case Ior.Both(c, _) =>
          c.toList.contains(JsonFailure.PathMissing(PathStep.Field("price"))) === true
        case _ => ko(s"expected Ior.Both, got $result")
    }
  }

object CrossCarrierCompositionSpec:

  // Common ADTs live in `JsonSpecFixtures`; the spec-specific MItem /
  // MultiBasket / Box / Envelope / NamedTuple aliases stay here.

  case class MItem(name: String, price: Double, qty: Int)

  object MItem:
    given Codec.AsObject[MItem] = KindlingsCodecAsObject.derive

  case class MultiBasket(owner: String, items: Vector[MItem])

  object MultiBasket:
    given Codec.AsObject[MultiBasket] = KindlingsCodecAsObject.derive

  case class Box(payload: Json)

  // Envelope has a tag alongside payload so the `dev.constructive.eo.generics.lens`
  // macro emits a SimpleLens (partial cover) rather than a BijectionIso
  // (full cover on one-field records). Scenario 3 requires the Tuple2
  // carrier to exercise cross-carrier Composer[Tuple2, Either].
  case class Envelope(tag: String, payload: Json)

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
  given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive
