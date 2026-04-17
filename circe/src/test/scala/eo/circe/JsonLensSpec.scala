package eo.circe

import io.circe.{Codec, Json, JsonObject}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[JsonLens]] — the cross-representation optic
  * that bridges a native Scala `S` and its `JsonObject` encoding.
  *
  *   1. `to` extracts the native focus and the complement is the
  *      encoded rest.
  *   2. `modify[A => Json]` produces the same JsonObject as the
  *      naïve `decode ∘ copy ∘ encode` round-trip.
  *   3. `transform[Json => Json]` mutates a JsonObject in place
  *      without any decode / encode on the S-side.
  *   4. `place` / `transfer` land new values via the stored
  *      fieldName, with the forgiving insert-if-missing semantics.
  */
class JsonLensSpec extends Specification with ScalaCheck:

  import JsonLensSpec.*

  // ---- to / modify --------------------------------------------------

  "JsonLens.to" should {
    "extract the native focus and leave a complement JsonObject" >> {
      val p                   = Person("Alice", 30)
      val (complement, focus) = nameL.to(p)
      focus      === "Alice"
      complement === JsonObject("age" -> Json.fromInt(30))
    }
  }

  "JsonLens.modify" should {
    "produce the same JsonObject as the naïve round-trip" >> {
      val p            = Person("Alice", 30)
      val upcase       = (s: String) => Json.fromString(s.toUpperCase)
      val viaOptic     = nameL.modify(upcase)(p)
      val viaRoundTrip = p.copy(name = p.name.toUpperCase).asJsonObject
      viaOptic === viaRoundTrip
    }
  }

  // ---- transform — the flagship hot path ----------------------------

  "JsonLens.transform" should {
    "modify a JsonObject field without touching the rest" >> {
      val p       = Person("Alice", 30)
      val encoded = p.asJsonObject
      val out     = nameL.transform(_.mapString(_.toUpperCase))(encoded)

      out("name")        === Some(Json.fromString("ALICE"))
      out("age")         === encoded("age")
      out.remove("name") === encoded.remove("name")
    }

    "insert the field when the target JsonObject does not have it" >> {
      val orphan = JsonObject("age" -> Json.fromInt(30))
      val out    = nameL.transform {
        case Json.Null => Json.fromString("default")
        case other     => other
      }(orphan)
      out("name") === Some(Json.fromString("default"))
      out("age")  === Some(Json.fromInt(30))
    }

    "preserve structural equality on a no-op transform" >> {
      val encoded = Person("Bob", 41).asJsonObject
      nameL.transform(identity[Json])(encoded) === encoded
    }
  }

  // ---- place / transfer ---------------------------------------------

  "JsonLens.place" should {
    "overwrite the field value, inserting if absent" >> {
      val present = JsonObject("name" -> Json.fromString("Alice"), "age" -> Json.fromInt(30))
      val absent  = JsonObject("age" -> Json.fromInt(30))

      nameL.place(Json.fromString("Bob"))(present) ===
        JsonObject("name" -> Json.fromString("Bob"), "age" -> Json.fromInt(30))

      nameL.place(Json.fromString("Bob"))(absent) ===
        JsonObject("age" -> Json.fromInt(30), "name" -> Json.fromString("Bob"))
    }
  }

  "JsonLens.transfer" should {
    "write the result of a C => Json encoder into the field" >> {
      val encoded = Person("Alice", 30).asJsonObject
      val upcase  = (s: String) => Json.fromString(s.toUpperCase)
      nameL.transfer(upcase)(encoded)("bob") ===
        encoded.add("name", Json.fromString("BOB"))
    }
  }

object JsonLensSpec:

  case class Person(name: String, age: Int)
  object Person:
    given Codec.AsObject[Person] = KindlingsCodecAsObject.derive

  val nameL: JsonLens[Person, String] =
    JsonLens[Person, String](fieldName = "name", selector = _.name)
