package dev.constructive.eo.circe

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.syntax.*
import io.circe.{Codec, Json}
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

  // 2026-04-29 consolidation: 4 spike-block tests → 1 composite.

  // covers: KindlingsCodecAsObject.derive for NamedTuple round-trips a hand-declared codec
  //   (decode of asJson(nt) returns Right(nt)),
  //   round-trip preserves field presence + value in the resulting JSON object,
  //   decode failure on a missing field surfaces as Left,
  //   macro-side Expr.summon resolves Encoder[NT] and Decoder[NT] (the spike's exit-condition
  //   for OQ1 fallback)
  "KindlingsCodecAsObject.derive for NamedTuple: round-trip + field-projection + decode fail + Expr.summon" >> {
    val nt: NamedTupleCodecSpike.Pair = (name = "Alice", age = 30)
    val j: Json = nt.asJson
    val rt = j.as[NamedTupleCodecSpike.Pair] === Right(nt)

    val nt2: NamedTupleCodecSpike.Pair = (name = "Bob", age = 42)
    val j2: Json = nt2.asJson
    val fieldsOk = (j2.hcursor.downField("name").as[String] === Right("Bob"))
      .and(j2.hcursor.downField("age").as[Int] === Right(42))

    val bad: Json = Json.obj("name" -> Json.fromString("Carol"))
    val decFailOk = bad.as[NamedTupleCodecSpike.Pair].isLeft === true

    val (encOk, decOk) = NamedTupleCodecSpikeMacro.summonEncDec
    val summonOk = (encOk === true).and(decOk === true)

    rt.and(fieldsOk).and(decFailOk).and(summonOk)
  }

object NamedTupleCodecSpike:

  type Pair = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

  given Codec.AsObject[Pair] = KindlingsCodecAsObject.derive

  // Singleton NamedTuple shape matching the one the spike macro synthesises.
  // Use `Tuple1[...]` form because the `(x,)` surface syntax is not
  // available inside a type position in 3.8.3.
  type Singleton = NamedTuple.NamedTuple[Tuple1["a"], Tuple1[Int]]

  given Codec.AsObject[Singleton] = KindlingsCodecAsObject.derive
