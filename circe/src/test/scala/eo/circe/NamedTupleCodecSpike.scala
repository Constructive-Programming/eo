package eo.circe

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.mutable.Specification

/** Unit 0 spike: confirm that `KindlingsCodecAsObject.derive[NT]` produces a usable
  * `Codec.AsObject[NT]` for a Scala 3 NamedTuple, and that `Expr.summon[Encoder[NT]]` resolves it
  * when NT is synthesised inside a macro.
  *
  * If either test fails, the plan halts and OQ1 fallback is adopted per the plan's Unit 0 exit
  * condition.
  */
class NamedTupleCodecSpike extends Specification:

  import NamedTupleCodecSpike.given

  "KindlingsCodecAsObject.derive for NamedTuple" should {

    "round-trip a hand-declared codec" >> {
      val nt: NamedTupleCodecSpike.Pair = (name = "Alice", age = 30)
      val j: Json = nt.asJson
      val decoded = j.as[NamedTupleCodecSpike.Pair]
      decoded === Right(nt)
    }

    "round-trip preserves field presence/value in JSON" >> {
      val nt: NamedTupleCodecSpike.Pair = (name = "Bob", age = 42)
      val j: Json = nt.asJson
      (j.hcursor.downField("name").as[String] === Right("Bob"))
        .and(j.hcursor.downField("age").as[Int] === Right(42))
    }

    "decode failure surfaces as Left" >> {
      val bad: Json = Json.obj("name" -> Json.fromString("Carol"))
      val result = bad.as[NamedTupleCodecSpike.Pair]
      result.isLeft === true
    }

    "macro-side Expr.summon resolves Encoder[NT] and Decoder[NT]" >> {
      val (encOk, decOk) = NamedTupleCodecSpikeMacro.summonEncDec
      (encOk === true).and(decOk === true)
    }
  }

object NamedTupleCodecSpike:

  type Pair = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

  given Codec.AsObject[Pair] = KindlingsCodecAsObject.derive

  // Singleton NamedTuple shape matching the one the spike macro synthesises.
  // Use `Tuple1[...]` form because the `(x,)` surface syntax is not
  // available inside a type position in 3.8.3.
  type Singleton = NamedTuple.NamedTuple[Tuple1["a"], Tuple1[Int]]

  given Codec.AsObject[Singleton] = KindlingsCodecAsObject.derive
