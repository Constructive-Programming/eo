package dev.constructive.eo.circe

import cats.data.Ior
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
import org.specs2.mutable.Specification

/** Observable-identity behaviour specs for [[JsonFailure]]. Each case of the enum must be reachable
  * from at least one call against a real [[JsonPrism]], and the `message` projection must emit a
  * human-readable string that names the `PathStep`.
  */
class JsonFailureSpec extends Specification:

  import JsonFailureSpec.*

  "JsonFailure.PathMissing" should {

    "fire when a named field is absent from its parent JsonObject" >> {
      val missing = Json.obj("age" -> Json.fromInt(30))
      val result = codecPrism[Person].field(_.name).modify(identity)(missing)
      result match
        case Ior.Both(chain, _) =>
          chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "message includes the step identifier" >> {
      val failure: JsonFailure = JsonFailure.PathMissing(PathStep.Field("x"))
      failure.message must contain("path missing")
      failure.message must contain("Field(x)")
    }
  }

  "JsonFailure.NotAnObject" should {

    "fire when the parent is not a JsonObject" >> {
      // An array at the "name" position can't host a `.name` lookup.
      val wrongShape = Json.fromValues(List(Json.fromString("Alice")))
      val result = codecPrism[Person].field(_.name).modify(identity)(wrongShape)
      result match
        case Ior.Both(chain, _) =>
          chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name"))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "message is distinctive" >> {
      val failure: JsonFailure = JsonFailure.NotAnObject(PathStep.Field("y"))
      failure.message must contain("expected JSON object")
    }
  }

  "JsonFailure.NotAnArray" should {

    "fire when the parent is not a JSON array for an index step" >> {
      // `.items.at(0)` on a non-array items value.
      val broken =
        Json.obj("owner" -> Json.fromString("Alice"), "items" -> Json.fromString("whoops"))
      val result = codecPrism[Basket].items.at(0).modify(identity)(broken)
      result match
        case Ior.Both(chain, _) =>
          chain.headOption.get === JsonFailure.NotAnArray(PathStep.Index(0))
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "message is distinctive" >> {
      val failure: JsonFailure = JsonFailure.NotAnArray(PathStep.Index(3))
      failure.message must contain("expected JSON array")
    }
  }

  "JsonFailure.IndexOutOfRange" should {

    "fire when the index is past the end of the array" >> {
      val basket = Basket("Alice", Vector(Order("X")))
      val result = codecPrism[Basket].items.at(5).modify(identity)(basket.asJson)
      result match
        case Ior.Both(chain, _) =>
          chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1)
        case _ => ko(s"expected Ior.Both, got $result")
    }

    "message includes the size" >> {
      val failure: JsonFailure = JsonFailure.IndexOutOfRange(PathStep.Index(7), 3)
      failure.message must contain("size=3")
    }
  }

  "JsonFailure.DecodeFailed" should {

    "fire when the leaf Json doesn't decode" >> {
      // A Person with `age` as a string instead of an int.
      val broken = Json
        .obj(
          "name" -> Json.fromString("Alice"),
          "age" -> Json.fromString("thirty"),
          "address" -> Address("Main St", 12345).asJson,
        )
      val result = codecPrism[Person].get(broken)
      result match
        case Ior.Left(chain) =>
          chain.headOption.get match
            case JsonFailure.DecodeFailed(_, _) => ok
            case other                          => ko(s"expected DecodeFailed, got $other")
        case _ => ko(s"expected Ior.Left, got $result")
    }
  }

object JsonFailureSpec:

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
