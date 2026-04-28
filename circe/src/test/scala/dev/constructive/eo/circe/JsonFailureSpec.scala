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
  *
  * '''2026-04-29 consolidation.''' 5 → 1 named block. Each case's fire + message-projection is
  * still witnessed; the spec frame collapses.
  */
class JsonFailureSpec extends Specification:

  import JsonSpecFixtures.*

  // covers: PathMissing fires when a named field is absent (Ior.Both with the failure in chain),
  //   PathMissing.message includes "path missing" + "Field(x)" step identifier;
  //   NotAnObject fires when the parent is not a JsonObject for a Field step,
  //   NotAnObject.message contains "expected JSON object";
  //   NotAnArray fires when parent is not a JSON array for an Index step,
  //   NotAnArray.message contains "expected JSON array";
  //   IndexOutOfRange fires when index past end of array (Ior.Both with size in chain),
  //   IndexOutOfRange.message contains "size=N";
  //   DecodeFailed fires when leaf Json doesn't decode (Ior.Left with DecodeFailed in chain)
  "JsonFailure: every case fires from a triggering input + message-projection holds" >> {
    // ---- PathMissing ----
    val missing = Json.obj("age" -> Json.fromInt(30))
    val pmResult = codecPrism[Person].field(_.name).modify(identity)(missing)
    val pmFireOk = pmResult match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.PathMissing(PathStep.Field("name"))
      case _ => ko(s"expected Ior.Both, got $pmResult")
    val pmFailure: JsonFailure = JsonFailure.PathMissing(PathStep.Field("x"))
    val pmMessageOk = (pmFailure.message must contain("path missing"))
      .and(pmFailure.message must contain("Field(x)"))

    // ---- NotAnObject ----
    val wrongShape = Json.fromValues(List(Json.fromString("Alice")))
    val noResult = codecPrism[Person].field(_.name).modify(identity)(wrongShape)
    val noFireOk = noResult match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.NotAnObject(PathStep.Field("name"))
      case _ => ko(s"expected Ior.Both, got $noResult")
    val noFailure: JsonFailure = JsonFailure.NotAnObject(PathStep.Field("y"))
    val noMessageOk = noFailure.message must contain("expected JSON object")

    // ---- NotAnArray ----
    val broken =
      Json.obj("owner" -> Json.fromString("Alice"), "items" -> Json.fromString("whoops"))
    val naResult = codecPrism[Basket].items.at(0).modify(identity)(broken)
    val naFireOk = naResult match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.NotAnArray(PathStep.Index(0))
      case _ => ko(s"expected Ior.Both, got $naResult")
    val naFailure: JsonFailure = JsonFailure.NotAnArray(PathStep.Index(3))
    val naMessageOk = naFailure.message must contain("expected JSON array")

    // ---- IndexOutOfRange ----
    val basket = Basket("Alice", Vector(Order("X")))
    val iorResult = codecPrism[Basket].items.at(5).modify(identity)(basket.asJson)
    val iorFireOk = iorResult match
      case Ior.Both(chain, _) =>
        chain.headOption.get === JsonFailure.IndexOutOfRange(PathStep.Index(5), 1)
      case _ => ko(s"expected Ior.Both, got $iorResult")
    val iorFailure: JsonFailure = JsonFailure.IndexOutOfRange(PathStep.Index(7), 3)
    val iorMessageOk = iorFailure.message must contain("size=3")

    // ---- DecodeFailed ----
    val brokenLeaf = Json
      .obj(
        "name" -> Json.fromString("Alice"),
        "age" -> Json.fromString("thirty"),
        "address" -> Address("Main St", 12345).asJson,
      )
    val dfFireOk = codecPrism[Person].get(brokenLeaf) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case JsonFailure.DecodeFailed(_, _) => ok
          case other                          => ko(s"expected DecodeFailed, got $other")
      case other => ko(s"expected Ior.Left, got $other")

    pmFireOk.and(pmMessageOk).and(noFireOk).and(noMessageOk)
      .and(naFireOk).and(naMessageOk).and(iorFireOk).and(iorMessageOk).and(dfFireOk)
  }
