package dev.constructive.eo.circe

import scala.language.implicitConversions

import cats.data.Ior
import io.circe.Json
import io.circe.syntax.*
import org.specs2.mutable.Specification

/** Observable-identity behaviour specs for [[JsonFailure]].
  *
  * '''2026-04-25 consolidation.''' 9 → 5 named blocks. Each `JsonFailure` case previously had two
  * specs (fire-scenario + message-projection). Collapsed into one composite per case.
  */
class JsonFailureSpec extends Specification:

  import JsonSpecFixtures.*

  // covers: PathMissing fires when a named field is absent, message includes step identifier
  "JsonFailure.PathMissing fires + message includes step identifier" >> {
    val missing = Json.obj("age" -> Json.fromInt(30))
    val result = codecPrism[Person].field(_.name).modify(identity)(missing)
    val fireOk = result match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
      case _ => ko(s"expected Ior.Both, got $result")

    val failure: JsonFailure = JsonFailure.PathMissing(PathStep.Field("x"))
    val messageOk = (failure.message must contain("path missing"))
      .and(failure.message must contain("Field(x)"))

    fireOk.and(messageOk)
  }

  // covers: NotAnObject fires when the parent is not a JsonObject, message is distinctive
  "JsonFailure.NotAnObject fires + distinctive message" >> {
    val wrongShape = Json.fromValues(List(Json.fromString("Alice")))
    val result = codecPrism[Person].field(_.name).modify(identity)(wrongShape)
    val fireOk = result match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name"))
      case _ => ko(s"expected Ior.Both, got $result")

    val failure: JsonFailure = JsonFailure.NotAnObject(PathStep.Field("y"))
    val messageOk = failure.message must contain("expected JSON object")

    fireOk.and(messageOk)
  }

  // covers: NotAnArray fires when parent is not a JSON array for an index step,
  // distinctive message
  "JsonFailure.NotAnArray fires + distinctive message" >> {
    val broken =
      Json.obj("owner" -> Json.fromString("Alice"), "items" -> Json.fromString("whoops"))
    val result = codecPrism[Basket].items.at(0).modify(identity)(broken)
    val fireOk = result match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.NotAnArray(PathStep.Index(0))
      case _ => ko(s"expected Ior.Both, got $result")

    val failure: JsonFailure = JsonFailure.NotAnArray(PathStep.Index(3))
    val messageOk = failure.message must contain("expected JSON array")

    fireOk.and(messageOk)
  }

  // covers: IndexOutOfRange fires when index past end of array, message includes size
  "JsonFailure.IndexOutOfRange fires + message includes size" >> {
    val basket = Basket("Alice", Vector(Order("X")))
    val result = codecPrism[Basket].items.at(5).modify(identity)(basket.asJson)
    val fireOk = result match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1)
      case _ => ko(s"expected Ior.Both, got $result")

    val failure: JsonFailure = JsonFailure.IndexOutOfRange(PathStep.Index(7), 3)
    val messageOk = failure.message must contain("size=3")

    fireOk.and(messageOk)
  }

  // covers: DecodeFailed fires when leaf Json doesn't decode
  "JsonFailure.DecodeFailed fires when leaf Json doesn't decode" >> {
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
