package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.specs2.mutable.Specification

/** Behaviour spec for the `Json | String` union-input overloads on every public JSON optic. Covers
  * three shapes per class: (1) happy-path parse on a String that's well-formed JSON, (2)
  * `Ior.Left(Chain(ParseFailed(_)))` on the Ior surface when the String doesn't parse, (3)
  * `Json.Null` fallback on the `*Unsafe` surface when the String doesn't parse.
  *
  * Not repeating the full behavioural coverage — the Json-input surface is exercised by
  * `JsonPrismSpec` / `JsonFieldsPrismSpec` / `JsonTraversalSpec` / `JsonFieldsTraversalSpec`, and
  * the widening to `Json | String` is structurally uniform (one shared `parseInputIor` /
  * `parseInputUnsafe` helper in `JsonFailure`).
  */
class StringInputSpec extends Specification:

  import JsonSpecFixtures.{Address, Person}
  import StringInputSpec.*
  import StringInputSpec.given

  // ---- JsonPrism ----------------------------------------------------

  "JsonPrism accepting String input" should {

    "parse the String and modify via the default Ior surface" >> {
      val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
      val out = codecPrism[Person].field(_.name).modify(_.toUpperCase)(str)
      val expected = Person("ALICE", 30, Address("Main St", 12345)).asJson
      out === Ior.Right(expected)
    }

    "return Ior.Left(ParseFailed) on an unparseable String (Ior surface)" >> {
      val out = codecPrism[Person].field(_.name).modify(_.toUpperCase)("not json at all")
      out.left.map(_.size) === Some(1L)
      out.left.flatMap(_.headOption).collect { case _: JsonFailure.ParseFailed => "yes" } ===
        Some("yes")
      out.right === None // Left, not Both — nothing to return
    }

    "return Json.Null on an unparseable String (Unsafe surface)" >> {
      val out = codecPrism[Person].field(_.name).modifyUnsafe(_.toUpperCase)("still not json")
      out === Json.Null
    }

    "round-trip get on a well-formed String (Ior surface)" >> {
      val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
      codecPrism[Person].field(_.name).get(str) === Ior.Right("Alice")
    }

    "return None from getOptionUnsafe on an unparseable String" >> {
      codecPrism[Person].field(_.name).getOptionUnsafe("not json") === None
    }

    "place via Ior from a String input" >> {
      val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
      val out = codecPrism[Person].field(_.name).place("Bob")(str)
      out === Ior.Right(Person("Bob", 30, Address("Main St", 12345)).asJson)
    }
  }

  // ---- JsonFieldsPrism ---------------------------------------------

  "JsonFieldsPrism accepting String input" should {

    "parse the String and modify via the default Ior surface" >> {
      val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
      val p = codecPrism[Person].fields(_.name, _.age)
      val out = p.modify(nt => (name = nt.name.toUpperCase, age = nt.age + 1))(str)
      out === Ior.Right(Person("ALICE", 31, Address("Main St", 12345)).asJson)
    }

    "return Ior.Left(ParseFailed) on an unparseable String" >> {
      val p = codecPrism[Person].fields(_.name, _.age)
      val out = p.modify(identity)("{ malformed")
      out.left.flatMap(_.headOption).collect { case _: JsonFailure.ParseFailed => "yes" } ===
        Some("yes")
    }
  }

  // ---- JsonTraversal -----------------------------------------------

  "JsonTraversal accepting String input" should {

    "parse the String and uppercase every name" >> {
      val str = """{"items":[{"name":"a","qty":1},{"name":"b","qty":2}]}"""
      val out = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(str)
      val expected = Basket(List(Item("A", 1), Item("B", 2))).asJson
      out === Ior.Right(expected)
    }

    "return Ior.Left(ParseFailed) on an unparseable String" >> {
      val out = codecPrism[Basket].items.each.name.modify(_.toUpperCase)("totally not json")
      out.left.flatMap(_.headOption).collect { case _: JsonFailure.ParseFailed => "yes" } ===
        Some("yes")
    }

    "return Vector.empty from getAllUnsafe on an unparseable String" >> {
      val out = codecPrism[Basket].items.each.name.getAllUnsafe("not json")
      out === Vector.empty
    }
  }

  // ---- Parse-happy on a String that already matches a Json =========

  "A Json input still works through the widened signatures" >> {
    val json = Person("Alice", 30, Address("Main St", 12345)).asJson
    val out = codecPrism[Person].field(_.name).modify(_.toUpperCase)(json)
    val expected = Person("ALICE", 30, Address("Main St", 12345)).asJson
    out === Ior.Right(expected)
  }

object StringInputSpec:

  // `Address` / `Person` come from `JsonSpecFixtures`; the
  // spec-specific `Item` + `Basket` (different from the fixture's
  // `Basket`!) stay here.

  // NamedTuple focus for the multi-field `.fields(_.name, _.age)` spec.
  given Codec.AsObject[NamedTuple.NamedTuple[("name", "age"), (String, Int)]] =
    KindlingsCodecAsObject.derive

  final case class Item(name: String, qty: Int)

  object Item:
    given Codec.AsObject[Item] = KindlingsCodecAsObject.derive

  final case class Basket(items: List[Item])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
