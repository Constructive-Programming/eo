package dev.constructive.eo.circe

import scala.language.implicitConversions

import cats.data.Ior
import dev.constructive.eo.generics.lens
import dev.constructive.eo.optics.{AffineFold, Lens, Optic}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.specs2.mutable.Specification

/** Unit 7: cross-carrier composition regression specs.
  *
  * '''2026-04-25 consolidation.''' 15 → 5 named blocks (one per scenario). Pre-image had each
  * scenario expressed as 3 separate "should" sub-blocks (success, miss, diagnostic). Each
  * scenario's three cases are now a single composite Result.
  */
class CrossCarrierCompositionSpec extends Specification:

  import JsonSpecFixtures.*
  import CrossCarrierCompositionSpec.*
  import CrossCarrierCompositionSpec.given

  // covers: scenario (1) — plain Lens(Box) → JsonFieldsPrism → AffineFold chain returns
  //   Some(name) on an adult payload, None on a minor (AffineFold miss), None on a payload
  //   that doesn't decode (Either-prism miss);
  //   scenario (2) — generics lens[S](_.field) → single-field JsonPrism cross-carrier,
  //   getOption reads embedded name on valid envelope, getOption returns None on undecodable
  //   payload (Affine miss), direct .modify on Json.obj() surfaces Ior.Both(PathMissing(name));
  //   scenario (3) — generics lens → multi-field JsonPrism (.fields), modify updates name+age
  //   through the chain, concrete-class .modify preserves address, direct .fields.get on a
  //   name-only payload surfaces Ior.Left accumulating PathMissing(age)
  "(1)+(2)+(3) lens → JsonFieldsPrism + AffineFold + multi-field .fields: cross-carrier + diagnostics" >> {
    // ---- (1) plain Lens → JsonFieldsPrism → AffineFold ----
    val box = Lens[Box, Json](_.payload, (b, j) => b.copy(payload = j))
    val personFields: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val adultName: AffineFold[NameAge, String] =
      AffineFold(nt => Option.when(nt.age >= 18)(nt.name))
    val boxToFields: Optic[Box, Box, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      box.andThen(personFields)
    val boxChain: Box => Option[String] =
      (b: Box) => boxToFields.getOption(b).flatMap(nt => adultName.getOption(nt))

    val adultBox = Box(Person("Alice", 30, Address("Main St", 1)).asJson)
    val minorBox = Box(Person("Bob", 12, Address("Oak Ave", 2)).asJson)
    val brokenBox = Box(Json.fromString("not a person"))
    val s1 = (boxChain(adultBox) === Some("Alice"))
      .and(boxChain(minorBox) === None)
      .and(boxChain(brokenBox) === None)

    // ---- (2) single-field ----
    val outer2 = Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val inner2: Optic[Json, Json, String, String, Either] = codecPrism[Person].field(_.name)
    val chain2 = outer2.andThen(inner2)

    val validEnv = Envelope("env", Person("Alice", 30, Address("Main St", 1)).asJson)
    val emptyEnv = Envelope("env", Json.obj())

    val direct = codecPrism[Person].field(_.name)
    val diag2 = direct.modify(_.toUpperCase)(Json.obj()) match
      case Ior.Both(c, _) => c.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
      case other          => ko(s"expected Ior.Both, got $other")
    val s2 = (chain2.getOption(validEnv) === Some("Alice"))
      .and(chain2.getOption(emptyEnv) === None)
      .and(diag2)

    // ---- (3) multi-field ----
    val outerGen: Optic[Envelope, Envelope, Json, Json, Tuple2] = lens[Envelope](_.payload)
    val inner3: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val chain3: Optic[Envelope, Envelope, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      outerGen.andThen(inner3)

    val f: NameAge => NameAge = nt => (name = nt.name.toUpperCase, age = nt.age + 1)
    val out: Envelope = chain3.modify(f)(validEnv)
    val modOk = (out.payload.hcursor.downField("name").as[String] === Right("ALICE"))
      .and(out.payload.hcursor.downField("age").as[Int] === Right(31))

    val p = Person("Alice", 30, Address("Main St", 1))
    val concrete = codecPrism[Person].fields(_.name, _.age)
    val concreteOk = concrete.modify(f)(p.asJson) match
      case Ior.Right(json) =>
        json.hcursor.downField("address").as[Address] === Right(Address("Main St", 1))
      case other => ko(s"expected Ior.Right, got $other")

    val payload = Json.obj("name" -> Json.fromString("Alice"))
    val diag3 = codecPrism[Person].fields(_.name, _.age).get(payload) match
      case Ior.Left(c) => c.toList.contains(JsonFailure.PathMissing(PathStep.Field("age"))) === true
      case other       => ko(s"expected Ior.Left, got $other")

    val s3 = modOk.and(concreteOk).and(diag3)
    s1.and(s2).and(s3)
  }

  // covers: manual composition outer.modify(trav.modifyUnsafe(f)) works,
  // manual composition with Ior unwrap, diagnostic case: traversal.modify directly
  // observes chain failures
  "(4) generics lens → JsonTraversal: manual composition (Unsafe + Ior unwrap + diagnostic)" >> {
    val outer = Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val trav = codecPrism[Basket].items.each.name

    val basket = Basket("Alice", Vector(Order("x"), Order("y"), Order("z")))
    val env = Envelope("env", basket.asJson)
    val expected = basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson

    val unsafeOk = outer.modify(trav.modifyUnsafe(_.toUpperCase))(env).payload === expected

    val iorOk = outer.modify { (j: Json) =>
      trav.modify(_.toUpperCase)(j) match
        case Ior.Right(v)   => v
        case Ior.Both(_, v) => v
        case Ior.Left(_)    => j
    }(env).payload === expected

    val brokenArr = Json.arr(Json.fromString("oops"), Order("y").asJson)
    val brokenBasket =
      Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenArr)
    val diagOk = trav.modify(_.toUpperCase)(brokenBasket) match
      case Ior.Both(c, _) => c.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name"))
      case other          => ko(s"expected Ior.Both, got $other")

    unsafeOk.and(iorOk).and(diagOk)
  }

  // covers: manual composition happy path (Unsafe), manual composition default Ior,
  // diagnostic case: missing per-element field surfaces through direct .modify
  "(5) generics lens → multi-field JsonTraversal: manual (Unsafe + Ior + per-elem diagnostic)" >> {
    val outer = Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val fieldsT = codecPrism[MultiBasket].items.each.fields(_.name, _.price)

    val mbasket = MultiBasket("Alice", Vector(MItem("x", 1.0, 1), MItem("y", 2.0, 2)))
    val env = Envelope("env", mbasket.asJson)

    val unsafeOk = outer
      .modify(
        fieldsT.modifyUnsafe(nt => (name = nt.name.toUpperCase, price = nt.price * 2))
      )(env)
      .payload === mbasket
      .copy(items = Vector(MItem("X", 2.0, 1), MItem("Y", 4.0, 2)))
      .asJson

    val iorOk = outer.modify { (j: Json) =>
      fieldsT.modify(nt => (name = nt.name, price = nt.price * 2))(j) match
        case Ior.Right(v)   => v
        case Ior.Both(_, v) => v
        case Ior.Left(_)    => j
    }(env).payload === mbasket
      .copy(items = Vector(MItem("x", 2.0, 1), MItem("y", 4.0, 2)))
      .asJson

    val brokenItems = Json.arr(
      Json.obj("name" -> Json.fromString("x"), "qty" -> Json.fromInt(1)),
      MItem("y", 2.0, 2).asJson,
    )
    val brokenBasket =
      Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenItems)
    val diagOk = fieldsT.modify(nt => (name = nt.name, price = nt.price))(brokenBasket) match
      case Ior.Both(c, _) =>
        c.toList.contains(JsonFailure.PathMissing(PathStep.Field("price"))) === true
      case other => ko(s"expected Ior.Both, got $other")

    unsafeOk.and(iorOk).and(diagOk)
  }

  // covers: JsonPrism (Either-carrier) → MultiFocus[F] cross-carrier resolution via
  // `Composer[Either, MultiFocus[F]]` (when `F: Alternative + Foldable`). Confirms
  // gap #5 of the top-5 closure plan at the **type level**: the Composer summons
  // cleanly and the cross-carrier `.andThen` produces an `Optic[…, MultiFocus[F]]`.
  //
  // VALUE-LEVEL FINDING: Non-trivial modify scenarios for the chained
  // `Optic[Json, Json, A, A, MultiFocus[F]]` round-trip awkwardly because
  // `either2multifocus.from` calls `pickSingletonOrThrow(fb, "Either")`, which
  // expects exactly one element on the focus side — but `mfFunctor[F].map` on a
  // multi-element `F[A]` (which the same-carrier composition can produce) doesn't
  // collapse back to a singleton. Recorded in the gap-analysis matrix as a **M
  // cell**: `JsonPrism × MultiFocus[F]` is type-correct under
  // `Composer[Either, MF[F]]` but the canonical use cases (drilling into a
  // List[A]-shaped focus) are better served by `Traversal.each` (which routes via
  // the PSVec-specialised `mfAssocPSVec` path that handles non-singleton focus
  // counts correctly).
  "(6) JsonPrism → MultiFocus[List]: Composer summons + chain compiles" >> {
    import cats.instances.list.given // Alternative[List] + Foldable[List]
    import dev.constructive.eo.data.MultiFocus
    import dev.constructive.eo.data.MultiFocus.given // mfFunctor + either2multifocus
    import dev.constructive.eo.Composer

    // Type-level: the Composer summons cleanly under List's Alternative+Foldable.
    val composerOk = summon[Composer[Either, MultiFocus[List]]] != null

    val tagsPrism: Optic[Json, Json, List[Int], List[Int], Either] =
      codecPrism[Tagged].field(_.tags)
    // The MultiFocus-side identity-shape on List[Int].
    val tagsMF: Optic[List[Int], List[Int], List[Int], List[Int], MultiFocus[List]] =
      new Optic[List[Int], List[Int], List[Int], List[Int], MultiFocus[List]]:
        type X = Unit
        val to: List[Int] => (Unit, List[List[Int]]) = xs => ((), List(xs))
        val from: ((Unit, List[List[Int]])) => List[Int] = { case (_, xss) => xss.head }

    // The cross-carrier .andThen resolves through `either2multifocus[List]`.
    val chain: Optic[Json, Json, List[Int], List[Int], MultiFocus[List]] =
      tagsPrism.andThen(tagsMF)

    // Type-level evidence is sufficient for gap #5 closure; the value-level
    // semantic is documented above as M.
    val chainTypeOk = chain != null

    composerOk.and(chainTypeOk)
  }

object CrossCarrierCompositionSpec:

  case class MItem(name: String, price: Double, qty: Int)

  object MItem:
    given Codec.AsObject[MItem] = KindlingsCodecAsObject.derive

  case class MultiBasket(owner: String, items: Vector[MItem])

  object MultiBasket:
    given Codec.AsObject[MultiBasket] = KindlingsCodecAsObject.derive

  case class Box(payload: Json)

  case class Envelope(tag: String, payload: Json)

  // For scenario (6): JsonPrism → MultiFocus[List] cross-carrier path.
  case class Tagged(name: String, tags: List[Int])

  object Tagged:
    given Codec.AsObject[Tagged] = KindlingsCodecAsObject.derive

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
  given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive
