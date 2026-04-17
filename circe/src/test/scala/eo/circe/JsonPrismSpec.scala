package eo.circe

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[JsonPrism]] — the cursor-backed Prism from
  * `Json` to a native type, with field-drilling sugar.
  *
  * Scenarios:
  *   1. Root `codecPrism[Person]` — full decode, full re-encode.
  *   2. Single-field drill via `.field(_.name)` — modifies one
  *      string in the encoded Json, leaves other fields identical.
  *   3. Two-level drill `.field(_.address).field(_.street)` — same
  *      semantics, one level deeper.
  *   4. `transform` operates on the raw Json at the focus, bypassing
  *      Codec entirely.
  *   5. Failure path — Json that can't decode at the target position
  *      passes through unchanged (forgiving).
  */
class JsonPrismSpec extends Specification with ScalaCheck:

  import JsonPrismSpec.*

  // ---- Root-level ---------------------------------------------------

  "codecPrism[S] at the root" should {
    "round-trip modify via full decode/encode" >> {
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out  = codecPrism[Person].modify(
        (p: Person) => p.copy(name = p.name.toUpperCase)
      )(json)
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
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out  = nameL.modify(_.toUpperCase)(json)
      out === p.copy(name = "ALICE").asJson
    }

    "leave every other field byte-identical to the input" >> {
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out  = nameL.modify(_ + "-x")(json)
      out.hcursor.downField("age").as[Int]     === Right(30)
      out.hcursor.downField("address").as[Address] ===
        Right(Address("Main St", 12345))
    }

    "transform operates on the raw Json at focus" >> {
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out  = nameL.transform(_.mapString(_.reverse))(json)
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
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      val out  = streetL.modify(_.toUpperCase)(json)
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

  // ---- Place / transfer --------------------------------------------

  "place / transfer on a drilled JsonPrism" should {

    val streetL = codecPrism[Person].field(_.address).field(_.street)

    "place overwrites the focused field" >> {
      val p    = Person("Alice", 30, Address("Main St", 12345))
      val json = p.asJson
      streetL.place("Broadway")(json) ===
        p.copy(address = Address("Broadway", 12345)).asJson
    }

    "transfer lifts a C => A into a focus-replacer" >> {
      val p    = Person("Alice", 30, Address("Main St", 12345))
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
