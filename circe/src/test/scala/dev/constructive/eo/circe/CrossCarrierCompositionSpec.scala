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

  // covers: chain on adult payload returns Some(name), chain on minor returns
  // None (AffineFold miss), chain on a payload that doesn't decode returns None
  "(1) plain Lens → JsonFieldsPrism → AffineFold: success / minor-miss / undecodable miss" >> {
    val box = Lens[Box, Json](_.payload, (b, j) => b.copy(payload = j))
    val personFields: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val adultName: AffineFold[NameAge, String] =
      AffineFold(nt => Option.when(nt.age >= 18)(nt.name))
    val boxToFields: Optic[Box, Box, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      box.andThen(personFields)
    val chain: Box => Option[String] =
      (b: Box) => boxToFields.getOption(b).flatMap(nt => adultName.getOption(nt))

    val adultBox = Box(Person("Alice", 30, Address("Main St", 1)).asJson)
    val minorBox = Box(Person("Bob", 12, Address("Oak Ave", 2)).asJson)
    val brokenBox = Box(Json.fromString("not a person"))

    (chain(adultBox) === Some("Alice"))
      .and(chain(minorBox) === None)
      .and(chain(brokenBox) === None)
  }

  // covers: getOption reads the embedded name on a valid envelope, getOption returns
  // None when the payload doesn't decode, diagnostic case via direct JsonPrism
  "(2) generics lens[S](_.field) → single-field JsonPrism: success / Affine miss / Ior diagnostic" >> {
    val outer = Lens[Envelope, Json](_.payload, (e, j) => e.copy(payload = j))
    val inner: Optic[Json, Json, String, String, Either] = codecPrism[Person].field(_.name)
    val chain = outer.andThen(inner)

    val validEnv = Envelope("env", Person("Alice", 30, Address("Main St", 1)).asJson)
    val emptyEnv = Envelope("env", Json.obj())

    val direct = codecPrism[Person].field(_.name)
    val diagOk = direct.modify(_.toUpperCase)(Json.obj()) match
      case Ior.Both(c, _) => c.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
      case other          => ko(s"expected Ior.Both, got $other")

    (chain.getOption(validEnv) === Some("Alice"))
      .and(chain.getOption(emptyEnv) === None)
      .and(diagOk)
  }

  // covers: modify updates name and age through the cross-carrier chain,
  // concrete-class Ior surface preserves address (direct call), diagnostic case:
  // missing age surfaces through direct Ior surface
  "(3) generics lens → multi-field JsonPrism (.fields): cross-carrier modify + Ior diagnostic" >> {
    val outerGen: Optic[Envelope, Envelope, Json, Json, Tuple2] = lens[Envelope](_.payload)
    val inner: Optic[Json, Json, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val chain: Optic[Envelope, Envelope, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      outerGen.andThen(inner)
    val validEnv = Envelope("env", Person("Alice", 30, Address("Main St", 1)).asJson)

    val f: NameAge => NameAge = nt => (name = nt.name.toUpperCase, age = nt.age + 1)
    val out: Envelope = chain.modify(f)(validEnv)
    val modOk = (out.payload.hcursor.downField("name").as[String] === Right("ALICE"))
      .and(out.payload.hcursor.downField("age").as[Int] === Right(31))

    val p = Person("Alice", 30, Address("Main St", 1))
    val concrete = codecPrism[Person].fields(_.name, _.age)
    val concreteOk = concrete.modify(f)(p.asJson) match
      case Ior.Right(json) =>
        json.hcursor.downField("address").as[Address] === Right(Address("Main St", 1))
      case other => ko(s"expected Ior.Right, got $other")

    val payload = Json.obj("name" -> Json.fromString("Alice"))
    val diagOk = codecPrism[Person].fields(_.name, _.age).get(payload) match
      case Ior.Left(c) => c.toList.contains(JsonFailure.PathMissing(PathStep.Field("age"))) === true
      case other       => ko(s"expected Ior.Left, got $other")

    modOk.and(concreteOk).and(diagOk)
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

object CrossCarrierCompositionSpec:

  case class MItem(name: String, price: Double, qty: Int)

  object MItem:
    given Codec.AsObject[MItem] = KindlingsCodecAsObject.derive

  case class MultiBasket(owner: String, items: Vector[MItem])

  object MultiBasket:
    given Codec.AsObject[MultiBasket] = KindlingsCodecAsObject.derive

  case class Box(payload: Json)

  case class Envelope(tag: String, payload: Json)

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
  given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive
