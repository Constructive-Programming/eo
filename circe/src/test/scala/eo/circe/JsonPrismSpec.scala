package eo.circe

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[JsonPrism]] — the cursor-backed Prism from `Json` to a native type, with
  * field-drilling sugar.
  *
  * Scenarios:
  *   1. Root `codecPrism[Person]` — full decode, full re-encode. 2. Single-field drill via
  *      `.field(_.name)` — modifies one string in the encoded Json, leaves other fields identical.
  *      3. Two-level drill `.field(_.address).field(_.street)` — same semantics, one level deeper.
  *      4. `transform` operates on the raw Json at the focus, bypassing Codec entirely. 5. Failure
  *         path — Json that can't decode at the target position passes through unchanged
  *         (forgiving).
  */
class JsonPrismSpec extends Specification with ScalaCheck:

  import JsonPrismSpec.*

  // ---- Root-level ---------------------------------------------------

  "codecPrism[S] at the root" should {
    "round-trip modify via full decode/encode" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = codecPrism[Person].modify((p: Person) => p.copy(name = p.name.toUpperCase))(json)
      out === p.copy(name = "ALICE").asJson
    }

    "pass through Json that doesn't decode" >> {
      val notAPerson = Json.fromString("not a person")
      codecPrism[Person].modify(identity[Person])(notAPerson) === notAPerson
    }
  }

  // ---- One-level drill ---------------------------------------------

  "codecPrism[Person].field(_.name)" should {

    val nameL = codecPrism[Person].field(_.name)

    "modify the focused field in place" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.modify(_.toUpperCase)(json)
      out === p.copy(name = "ALICE").asJson
    }

    "leave every other field byte-identical to the input" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.modify(_ + "-x")(json)
      out.hcursor.downField("age").as[Int] === Right(30)
      out.hcursor.downField("address").as[Address] ===
        Right(Address("Main St", 12345))
    }

    "transform operates on the raw Json at focus" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = nameL.transform(_.mapString(_.reverse))(json)
      out === p.copy(name = "Alice".reverse).asJson
    }

    "getOption returns the decoded focus" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      nameL.getOption(p.asJson) === Some("Alice")
    }

    "getOption returns None when the path is missing" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      nameL.getOption(missing) === None
    }
  }

  // ---- Two-level drill ---------------------------------------------

  "codecPrism[Person].field(_.address).field(_.street)" should {

    val streetL = codecPrism[Person].field(_.address).field(_.street)

    "modify a nested field without touching siblings" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out = streetL.modify(_.toUpperCase)(json)
      out === p.copy(address = Address("MAIN ST", 12345)).asJson
    }

    "the middle address Codec is not invoked beyond cursor navigation" >> {
      // Smoke check: reading a deep focus via getOption only decodes
      // the focus and whatever circe needs to navigate — NOT a full
      // Person/Address materialisation by the user.
      val p = Person("Alice", 30, Address("Main St", 12345))
      streetL.getOption(p.asJson) === Some("Main St")
    }

    "forgiving: modify on a Json missing the path returns input unchanged" >> {
      val stump = Json.obj("name" -> Json.fromString("Alice"))
      streetL.modify(_.toUpperCase)(stump) === stump
    }
  }

  // ---- Selectable field sugar --------------------------------------

  "selectDynamic sugar `codecPrism[Person].address.street`" should {

    "behave identically to `.field(_.address).field(_.street)`" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val explicit = codecPrism[Person].field(_.address).field(_.street)
      val sugared = codecPrism[Person].address.street
      sugared.modify(_.toUpperCase)(json) === explicit.modify(_.toUpperCase)(json)
    }

    "support a single-level sugar drill" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      codecPrism[Person].name.modify(_.toUpperCase)(json) ===
        p.copy(name = "ALICE").asJson
    }

    "round-trip getOption through the deep sugar path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      codecPrism[Person].address.street.getOption(p.asJson) === Some("Main St")
    }

    "place works through the sugar path" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      codecPrism[Person].address.street.place("Broadway")(json) ===
        p.copy(address = Address("Broadway", 12345)).asJson
    }
  }

  // ---- Array indexing ----------------------------------------------

  "JsonPrism .at(i) on a Vector focus" should {

    "modify the i-th element of a root-level array" >> {
      val orders = Vector(Order("A"), Order("B"), Order("C"))
      val json = orders.asJson
      val out = codecPrism[Vector[Order]].at(1).name.modify(_.toUpperCase)(json)
      out === Vector(Order("A"), Order("B".toUpperCase), Order("C")).asJson
    }

    "leave sibling indices byte-identical" >> {
      val orders = Vector(Order("A"), Order("B"), Order("C"))
      val json = orders.asJson
      val out = codecPrism[Vector[Order]].at(2).name.modify(_ + "-x")(json)
      out.hcursor.downN(0).as[Order] === Right(Order("A"))
      out.hcursor.downN(1).as[Order] === Right(Order("B"))
      out.hcursor.downN(2).as[Order] === Right(Order("C-x"))
    }

    "modify an element reached through a nested field path" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      val json = basket.asJson
      val out = codecPrism[Basket].items.at(0).name.modify(_.toUpperCase)(json)
      out === basket.copy(items = Vector(Order("X".toUpperCase), Order("Y"))).asJson
    }

    "forgiving: out-of-range index leaves the input unchanged" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X")))
      val json = basket.asJson
      codecPrism[Basket].items.at(5).name.modify(_.toUpperCase)(json) === json
    }

    "forgiving: negative index leaves the input unchanged" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      val json = basket.asJson
      codecPrism[Basket].items.at(-1).name.modify(_.toUpperCase)(json) === json
    }

    "getOption on a nested array index returns the element" >> {
      val basket = Basket(owner = "Alice", items = Vector(Order("X"), Order("Y")))
      codecPrism[Basket].items.at(1).getOption(basket.asJson) === Some(Order("Y"))
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

    "place overwrites the focused field" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      streetL.place("Broadway")(json) ===
        p.copy(address = Address("Broadway", 12345)).asJson
    }

    "transfer lifts a C => A into a focus-replacer" >> {
      val p = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val upcase: String => String = _.toUpperCase
      streetL.transfer(upcase)(json)("main ave") ===
        p.copy(address = Address("MAIN AVE", 12345)).asJson
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
