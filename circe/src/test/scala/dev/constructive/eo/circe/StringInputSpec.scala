package dev.constructive.eo.circe

import scala.language.implicitConversions

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}

/** Behaviour spec for the `Json | String` union-input overloads on every public JSON optic.
  *
  * '''2026-04-25 consolidation.''' 12 → 4 named blocks. One block per class (JsonPrism /
  * JsonFieldsPrism / JsonTraversal) collapses 6 / 2 / 3 sub-specs into composite tests covering
  * happy-path, Ior.Left(ParseFailed), and Json.Null/empty fallbacks. The Json-input pass-through
  * test stays separate.
  */
class StringInputSpec extends JsonSpecBase:

  import JsonSpecFixtures.{Address, Person}
  import StringInputSpec.*
  import StringInputSpec.given

  // covers: parse the String and modify via default Ior, return Ior.Left(ParseFailed)
  // on an unparseable String, return Json.Null on unparseable Unsafe, round-trip get
  // on well-formed String, return None from getOptionUnsafe on unparseable, place via
  // Ior from String
  "JsonPrism String-input: parse+modify+place + Ior.Left/Json.Null on bad input" >> {
    val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
    val expectedUpper = Person("ALICE", 30, Address("Main St", 12345)).asJson

    val modOk =
      codecPrism[Person].field(_.name).modify(_.toUpperCase)(str) === Ior.Right(expectedUpper)
    val getOk = codecPrism[Person].field(_.name).get(str) === Ior.Right("Alice")
    val placeOk = codecPrism[Person].field(_.name).place("Bob")(str) ===
      Ior.Right(Person("Bob", 30, Address("Main St", 12345)).asJson)

    val badIor = codecPrism[Person].field(_.name).modify(_.toUpperCase)("not json at all")
    val badIorOk = (badIor.left.map(_.size) === Some(1L))
      .and(
        badIor.left.flatMap(_.headOption).collect { case _: JsonFailure.ParseFailed => "yes" } ===
          Some("yes")
      )
      .and(badIor.right === None)

    val unsafeNullOk =
      codecPrism[Person].field(_.name).modifyUnsafe(_.toUpperCase)("still not json") === Json.Null
    val unsafeNoneOk = codecPrism[Person].field(_.name).getOptionUnsafe("not json") === None

    modOk.and(getOk).and(placeOk).and(badIorOk).and(unsafeNullOk).and(unsafeNoneOk)
  }

  // covers: parse the String and modify via default Ior (multi-field), return
  // Ior.Left(ParseFailed) on unparseable
  "JsonFieldsPrism String-input: parse+modify + Ior.Left(ParseFailed) on bad input" >> {
    val str = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
    val p = codecPrism[Person].fields(_.name, _.age)
    val modOk = p.modify(nt => (name = nt.name.toUpperCase, age = nt.age + 1))(str) ===
      Ior.Right(Person("ALICE", 31, Address("Main St", 12345)).asJson)

    val badOk = p
      .modify(identity)("{ malformed")
      .left
      .flatMap(_.headOption)
      .collect { case _: JsonFailure.ParseFailed => "yes" } === Some("yes")

    modOk.and(badOk)
  }

  // covers: parse String and uppercase every name, return Ior.Left(ParseFailed) on
  // unparseable, return Vector.empty from getAllUnsafe on unparseable
  "JsonTraversal String-input: parse+modify + Ior.Left(ParseFailed) + getAllUnsafe-empty on bad input" >> {
    val str = """{"items":[{"name":"a","qty":1},{"name":"b","qty":2}]}"""
    val expected = Basket(List(Item("A", 1), Item("B", 2))).asJson
    val modOk =
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)(str) === Ior.Right(expected)

    val badIor =
      codecPrism[Basket].items.each.name.modify(_.toUpperCase)("totally not json")
    val badIorOk = badIor
      .left
      .flatMap(_.headOption)
      .collect { case _: JsonFailure.ParseFailed => "yes" } === Some("yes")

    val unsafeEmptyOk =
      codecPrism[Basket].items.each.name.getAllUnsafe("not json") === Vector.empty

    modOk.and(badIorOk).and(unsafeEmptyOk)
  }

  // covers: A Json input still works through the widened signatures
  "Json input pass-through still works through the String-widened signatures" >> {
    val json = Person("Alice", 30, Address("Main St", 12345)).asJson
    val out = codecPrism[Person].field(_.name).modify(_.toUpperCase)(json)
    val expected = Person("ALICE", 30, Address("Main St", 12345)).asJson
    out === Ior.Right(expected)
  }

object StringInputSpec:

  given Codec.AsObject[NamedTuple.NamedTuple[("name", "age"), (String, Int)]] =
    KindlingsCodecAsObject.derive

  final case class Item(name: String, qty: Int)

  object Item:
    given Codec.AsObject[Item] = KindlingsCodecAsObject.derive

  final case class Basket(items: List[Item])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
