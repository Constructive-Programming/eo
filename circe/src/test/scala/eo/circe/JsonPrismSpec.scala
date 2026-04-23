package eo.circe

import cats.data.{Chain, Ior}

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[JsonPrism]] — the cursor-backed Prism from `Json` to a native type, with
  * field-drilling sugar.
  *
  * Every scenario is exercised twice: once against the default Ior-bearing surface (`modify` /
  * `transform` / `place` / `transfer` / `get`) and once against the `*Unsafe` escape hatches
  * (`modifyUnsafe` / …) to witness that the escape hatches preserve pre-v0.2 silent behaviour
  * byte-for-byte.
  */
class JsonPrismSpec extends Specification with ScalaCheck:

  import JsonPrismSpec.*

  // ---- Root-level ---------------------------------------------------

  "codecPrism[S] at the root" should {

    "round-trip modify via full decode/encode (default Ior surface)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out =
        codecPrism[Person].modify((p: Person) => p.copy(name = p.name.toUpperCase))(json)
      out === Ior.Right(p.copy(name = "ALICE").asJson)
    }

    "round-trip modify via *Unsafe surface" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out =
        codecPrism[Person]
          .modifyUnsafe((p: Person) => p.copy(name = p.name.toUpperCase))(json)
      out === p.copy(name = "ALICE").asJson
    }

    "decode failure surfaces Ior.Both(chain-of-one, inputJson) on modify" >> {
      val notAPerson = Json.fromString("not a person")
      val result = codecPrism[Person].modify(identity[Person])(notAPerson)
      result match
        case Ior.Both(chain, json) =>
          (json === notAPerson)
            .and(chain.length === 1L)
            .and(chain.headOption.get.isInstanceOf[JsonFailure.DecodeFailed] === true)
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "decode failure surfaces Ior.Left on get" >> {
      val notAPerson = Json.fromString("not a person")
      val result = codecPrism[Person].get(notAPerson)
      result.isLeft === true
    }

    "pass through Json that doesn't decode (*Unsafe surface)" >> {
      val notAPerson = Json.fromString("not a person")
      codecPrism[Person].modifyUnsafe(identity[Person])(notAPerson) === notAPerson
    }
  }

  // ---- One-level drill ---------------------------------------------

  "codecPrism[Person].field(_.name)" should {

    val nameL = codecPrism[Person].field(_.name)

    "modify the focused field in place (default surface)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.modify(_.toUpperCase)(json)
      out === Ior.Right(p.copy(name = "ALICE").asJson)
    }

    "modify the focused field in place (*Unsafe surface)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.modifyUnsafe(_.toUpperCase)(json)
      out === p.copy(name = "ALICE").asJson
    }

    "leave every other field byte-identical to the input" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.modifyUnsafe(_ + "-x")(json)
      (out.hcursor.downField("age").as[Int] === Right(30)).and(
        out.hcursor.downField("address").as[Address] ===
          Right(Address("Main St", 12345))
      )
    }

    "transform operates on the raw Json at focus (default surface)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.transform(_.mapString(_.reverse))(json)
      out === Ior.Right(p.copy(name = "Alice".reverse).asJson)
    }

    "transform operates on the raw Json at focus (*Unsafe surface)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.transformUnsafe(_.mapString(_.reverse))(json)
      out === p.copy(name = "Alice".reverse).asJson
    }

    "get returns Ior.Right(decoded focus) on a complete Json" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      nameL.get(p.asJson) === Ior.Right("Alice")
    }

    "getOptionUnsafe returns Some(decoded focus)" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      nameL.getOptionUnsafe(p.asJson) === Some("Alice")
    }

    "get returns Ior.Left with PathMissing when the path is absent" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      val result = nameL.get(missing)
      result match
        case Ior.Left(chain) =>
          (chain.length === 1L).and(
            chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
          )
        case _ => ko(s"expected Ior.Left, got $result")
    }

    "getOptionUnsafe returns None when the path is missing" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      nameL.getOptionUnsafe(missing) === None
    }

    "modify on a missing-path Json produces Ior.Both(PathMissing, inputJson)" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      val result = nameL.modify(_.toUpperCase)(missing)
      result match
        case Ior.Both(chain, json) =>
          (json === missing)
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name")))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "modifyUnsafe on a missing-path Json returns input unchanged" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      nameL.modifyUnsafe(_.toUpperCase)(missing) === missing
    }

    "modify and modifyUnsafe agree on the happy path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      nameL.modify(_ + "-x")(json) === Ior.Right(nameL.modifyUnsafe(_ + "-x")(json))
    }

    "modify on broken === Ior.Both(chain, modifyUnsafe(broken))" >> {
      val broken = Json.obj("age" -> Json.fromInt(30))
      val iorOut = nameL.modify(_.toUpperCase)(broken)
      val unsafeOut = nameL.modifyUnsafe(_.toUpperCase)(broken)
      iorOut === Ior.Both(Chain.one(JsonFailure.PathMissing(PathStep.Field("name"))), unsafeOut)
    }
  }

  // ---- Two-level drill ---------------------------------------------

  "codecPrism[Person].field(_.address).field(_.street)" should {

    val streetL = codecPrism[Person].field(_.address).field(_.street)

    "modify a nested field without touching siblings" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      streetL.modifyUnsafe(_.toUpperCase)(json) ===
        p.copy(address = Address("MAIN ST", 12345)).asJson
    }

    "the middle address Codec is not invoked beyond cursor navigation" >> {
      // Smoke check: reading a deep focus via getOptionUnsafe only decodes
      // the focus and whatever circe needs to navigate — NOT a full
      // Person/Address materialisation by the user.
      val p = Person("Alice", 30, Address("Main St", 12345))
      streetL.getOptionUnsafe(p.asJson) === Some("Main St")
    }

    "*Unsafe: modify on a Json missing the path returns input unchanged" >> {
      val stump = Json.obj("name" -> Json.fromString("Alice"))
      streetL.modifyUnsafe(_.toUpperCase)(stump) === stump
    }

    "default: modify on a Json missing the path returns Ior.Both with PathMissing(address)" >> {
      val stump = Json.obj("name" -> Json.fromString("Alice"))
      val result = streetL.modify(_.toUpperCase)(stump)
      result match
        case Ior.Both(chain, json) =>
          (json === stump)
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("address")))
        case _ => ko(s"expected Ior.Both, got $result")
    }
  }

  // ---- Selectable field sugar --------------------------------------

  "selectDynamic sugar `codecPrism[Person].address.street`" should {

    "behave identically to `.field(_.address).field(_.street)`" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val explicit = codecPrism[Person].field(_.address).field(_.street)
      val sugared = codecPrism[Person].address.street
      sugared.modifyUnsafe(_.toUpperCase)(json) === explicit.modifyUnsafe(_.toUpperCase)(json)
    }

    "support a single-level sugar drill" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      codecPrism[Person].name.modifyUnsafe(_.toUpperCase)(json) ===
        p.copy(name = "ALICE").asJson
    }

    "round-trip getOptionUnsafe through the deep sugar path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      codecPrism[Person].address.street.getOptionUnsafe(p.asJson) === Some("Main St")
    }

    "placeUnsafe works through the sugar path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      codecPrism[Person].address.street.placeUnsafe("Broadway")(json) ===
        p.copy(address = Address("Broadway", 12345)).asJson
    }

    "place (default) returns Ior.Right on the happy path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      codecPrism[Person].address.street.place("Broadway")(json) ===
        Ior.Right(p.copy(address = Address("Broadway", 12345)).asJson)
    }
  }

  // ---- Array indexing ----------------------------------------------

  "JsonPrism .at(i) on a Vector focus" should {

    "modify the i-th element of a root-level array" >> {
      val orders = Vector(Order("A"), Order("B"), Order("C"))
      val json = orders.asJson
      val out = codecPrism[Vector[Order]].at(1).name.modifyUnsafe(_.toUpperCase)(json)
      out === Vector(Order("A"), Order("B".toUpperCase), Order("C")).asJson
    }

    "leave sibling indices byte-identical" >> {
      val orders = Vector(Order("A"), Order("B"), Order("C"))
      val json = orders.asJson
      val out = codecPrism[Vector[Order]].at(2).name.modifyUnsafe(_ + "-x")(json)
      (out.hcursor.downN(0).as[Order] === Right(Order("A")))
        .and(out.hcursor.downN(1).as[Order] === Right(Order("B")))
        .and(out.hcursor.downN(2).as[Order] === Right(Order("C-x")))
    }

    "modify an element reached through a nested field path" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      val json = basket.asJson
      val out = codecPrism[Basket].items.at(0).name.modifyUnsafe(_.toUpperCase)(json)
      out === basket.copy(items = Vector(Order("X".toUpperCase), Order("Y"))).asJson
    }

    "*Unsafe: out-of-range index leaves the input unchanged" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X")))
      val json = basket.asJson
      codecPrism[Basket].items.at(5).name.modifyUnsafe(_.toUpperCase)(json) === json
    }

    "default: out-of-range index surfaces IndexOutOfRange" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X")))
      val json = basket.asJson
      val result = codecPrism[Basket].items.at(5).name.modify(_.toUpperCase)(json)
      result match
        case Ior.Both(chain, out) =>
          (out === json)
            .and(chain.length === 1L)
            .and(chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "*Unsafe: negative index leaves the input unchanged" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      val json = basket.asJson
      codecPrism[Basket].items.at(-1).name.modifyUnsafe(_.toUpperCase)(json) === json
    }

    "getOptionUnsafe on a nested array index returns the element" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      codecPrism[Basket].items.at(1).getOptionUnsafe(basket.asJson) === Some(Order("Y"))
    }
  }

  // ---- Traversal ----------------------------------------------------

  "JsonPrism .each traversal" should {

    "modify every element's focused field in one pass" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("x"), Order("y"), Order("z")))
      val json = basket.asJson
      val out = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json)
      out === basket.copy(items = Vector(Order("X"), Order("Y"), Order("Z"))).asJson
    }

    "transform applies to each raw Json leaf" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("ab"), Order("cd")))
      val json = basket.asJson
      val out = codecPrism[Basket]
        .items
        .each
        .name
        .transform(_.mapString(_.reverse))(json)
      out === basket.copy(items = Vector(Order("ba"), Order("dc"))).asJson
    }

    "getAll collects every focus" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("x"), Order("y")))
      codecPrism[Basket].items.each.name.getAll(basket.asJson) ===
        Vector("x", "y")
    }

    "on an empty array, modify is a no-op" >> {
      val basket = Basket(owner = "Alice", items = Vector.empty)
      val json = basket.asJson
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json) === json
    }

    "forgiving: a missing prefix field leaves input unchanged" >> {
      val stump = Json.obj("owner" -> Json.fromString("Alice"))
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)(stump) === stump
    }

    "at the root: modify every element of a top-level array" >> {
      val orders = Vector(Order("a"), Order("b"))
      val json = orders.asJson
      codecPrism[Vector[Order]].each.name.modify(_.toUpperCase)(json) ===
        Vector(Order("A"), Order("B")).asJson
    }
  }

  // ---- Place / transfer --------------------------------------------

  "place / transfer on a drilled JsonPrism" should {

    val streetL = codecPrism[Person].field(_.address).field(_.street)

    "placeUnsafe overwrites the focused field" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      streetL.placeUnsafe("Broadway")(json) ===
        p.copy(address = Address("Broadway", 12345)).asJson
    }

    "transferUnsafe lifts a C => A into a focus-replacer" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val upcase: String => String = _.toUpperCase
      streetL.transferUnsafe(upcase)(json)("main ave") ===
        p.copy(address = Address("MAIN AVE", 12345)).asJson
    }

    "place (default) returns Ior.Right on the happy path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      streetL.place("Broadway")(json) ===
        Ior.Right(p.copy(address = Address("Broadway", 12345)).asJson)
    }

    "transfer (default) returns Ior.Right on the happy path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val upcase: String => String = _.toUpperCase
      streetL.transfer(upcase)(json)("main ave") ===
        Ior.Right(p.copy(address = Address("MAIN AVE", 12345)).asJson)
    }
  }

object JsonPrismSpec:

  case class Address(street: String, zip: Int)

  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derive

  case class Person(name: String, age: Int, address: Address)

  object Person:
    given Codec.AsObject[Person] = KindlingsCodecAsObject.derive

  case class Order(name: String)

  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

  case class Basket(owner: String, items: Vector[Order])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
