package eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import eo.circe.{JsonPrism, codecPrism}

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** Wide-fixture companion to [[JsonPrismBench]].
  *
  * Three levels of nesting with realistic field widths: 8 / 14 / 6 total fields across the levels.
  * The narrow companion spec's fixtures have 2–3 fields per level, which is the degenerate case
  * where "decode all fields" ≈ "navigate path depth" and the structural O(depth) vs O(fields)
  * advantage doesn't materialise.
  *
  * At 8+14+6 = 28 total fields with a depth-3 path, naive has to pay decoder + encoder for every
  * field, whereas the cursor-based modify touches only the four cursor stops on the path and the
  * focused leaf. That's the gap this bench measures.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class JsonPrismWideBench:

  import JsonPrismWideBench.*

  private val fixtureJson: Json = DefaultL1.asJson

  private val leafL: JsonPrism[String] =
    codecPrism[L1].field(_.next).field(_.next).field(_.value)

  @Benchmark def eoModify_wide: Json =
    leafL.modify(_.toUpperCase)(fixtureJson)

  @Benchmark def naiveModify_wide: Json =
    fixtureJson
      .as[L1]
      .map { l1 =>
        l1.copy(next =
          l1.next.copy(next = l1.next.next.copy(value = l1.next.next.value.toUpperCase))
        )
      }
      .toOption
      .get
      .asJson

object JsonPrismWideBench:

  // ---- L3: 6 fields, focused leaf -----------------------------------

  final case class L3(
      a: Int,
      b: String,
      c: Double,
      d: Boolean,
      e: Long,
      value: String,
  )

  object L3:
    given Codec.AsObject[L3] = KindlingsCodecAsObject.derive

  // ---- L2: 14 fields, one is L3 -------------------------------------

  final case class L2(
      f01: Int,
      f02: String,
      f03: Double,
      f04: Boolean,
      f05: Long,
      f06: Int,
      f07: String,
      f08: Double,
      f09: Boolean,
      f10: Long,
      f11: Int,
      f12: String,
      f13: Double,
      next: L3,
  )

  object L2:
    given Codec.AsObject[L2] = KindlingsCodecAsObject.derive

  // ---- L1: 8 fields, one is L2 --------------------------------------

  final case class L1(
      f1: Int,
      f2: String,
      f3: Double,
      f4: Boolean,
      f5: Long,
      f6: Int,
      f7: String,
      next: L2,
  )

  object L1:
    given Codec.AsObject[L1] = KindlingsCodecAsObject.derive

  // ---- Default fully-populated fixture ------------------------------

  val DefaultL3: L3 =
    L3(1, "b", 3.14, true, 5L, "Alice")

  val DefaultL2: L2 =
    L2(
      1,
      "b",
      3.14,
      true,
      5L,
      6,
      "g",
      8.0,
      false,
      10L,
      11,
      "l",
      13.0,
      DefaultL3,
    )

  val DefaultL1: L1 =
    L1(1, "b", 3.14, true, 5L, 6, "g", DefaultL2)
