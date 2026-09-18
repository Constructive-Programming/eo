package dev.constructive.eo.avro

import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** The COST gate for the nominal rung (issue #103).
  *
  * PR #98's rung is correct and stays correct; what it was not is affordable. `totalNominalIndex`
  * asks `nominalIndex` once per case field, and `nominalIndex` linear-scans EVERY schema field on
  * an exact-name miss, deliberately without an early exit (it has to see a second match to call the
  * name ambiguous). So the cost is `arity x fields.size x nameLength` — quadratic in the field
  * count — and it is paid by exactly the codecs the rung exists for, since a miss is what a name
  * transform guarantees. An identity-named codec answers from Avro's hash and never scans.
  *
  * '''What is asserted, and what merely prints.''' This box is noisy — project policy is that ns/op
  * here is +/-15-50% and only within-run ratios are load-bearing — so nothing here asserts a
  * nanosecond count. The assertion is on the SHAPE: double the field count and a quadratic
  * implementation costs ~4x while a linear one costs ~2x. The absolute table is printed for the
  * record, never gated.
  */
class NominalResolutionCostSpec extends Specification:

  import NominalCostFixtures.*

  sequential

  private val Reps = 7
  private val Iters = 400

  /** Repeated resolution against ONE schema — a `def`-shaped or parameterised-`given` optic
    * re-resolves per operation, so this is the shape a call site actually pays.
    */
  private def warmNs(n: Int, snake: Boolean)(resolve: (Schema, List[String]) => Int): Double =
    val sch = schema(n, snake)
    val names = caseNames(n)
    costNs(Reps, Iters)(_ => resolve(sch, names))

  private val live: (Schema, List[String]) => Int =
    (s, ns) => AvroWalk.totalNominalIndex(s, ns, 0)

  private val frozen: (Schema, List[String]) => Int =
    (s, ns) => NominalResolutionOracle.totalNominalIndex(s, ns, 0)

  // ---- the table (printed, never gated) -------------------------------------

  "the resolution cost table is printed for the record" >> {
    val rows =
      for
        snake <- List(false, true)
        n <- List(2, 12, 66)
      yield
        val shape = if snake then "snake_cased" else "identity   "
        val w = warmNs(n, snake)(live)
        val c = coldCostNs(n, snake, 200)(live)
        val wo = warmNs(n, snake)(frozen)
        val co = coldCostNs(n, snake, 200)(frozen)
        f"  $shape%s  n=$n%3d   warm ${wo}%10.1f -> ${w}%10.1f ns   cold ${co}%10.1f -> ${c}%10.1f ns"
    println("=== nominal resolution, ns per resolution (frozen pre-index -> live) ===")
    rows.foreach(println)
    ok
  }

  // ---- the gate ------------------------------------------------------------

  "resolution cost is sub-quadratic in the schema's field count" >> {
    // 33 / 66 / 132: the middle row is the field count issue #103 measured in the wild. The three
    // sizes are measured INTERLEAVED — one pass of each, `Reps` rounds, per-size minimum — so a
    // burst of load on this (noisy) box lands on all three rather than inflating one ratio.
    val sizes = List(33, 66, 132)
    val fixed = sizes.map(n => (schema(n, snake = true), caseNames(n)))
    val best =
      (1 to Reps).foldLeft(sizes.map(_ => Double.MaxValue)): (acc, _) =>
        acc
          .zip(fixed)
          .map: (prev, sc) =>
            val ns = costNs(1, Iters)(_ => live(sc._1, sc._2))
            if ns < prev then ns else prev
    val List(t33, t66, t132) = best: @unchecked
    val double = t66 / t33
    val quadruple = t132 / t33
    println(
      f"  shape: n=33 ${t33}%.1f ns, n=66 ${t66}%.1f ns, n=132 ${t132}%.1f ns"
        + f"  |  2n/n = ${double}%.2f (linear ~2, quadratic ~4)"
        + f"  4n/n = ${quadruple}%.2f (linear ~4, quadratic ~16)"
    )
    // Linear gives 2 and 4; quadratic gives 4 and 16. The thresholds sit between. The 4n/n bound is
    // the looser of the two because a linear rung still allocates one normalised key per case field
    // per resolution, so its 4x carries a GC tail (measured ~6.5); the pre-index rung measured 15.4
    // there, so 10 still separates the two shapes with room to spare.
    (double must beLessThan(3.0)).and(quadruple must beLessThan(10.0))
  }

  "the snake_cased shape is not a different ORDER of cost from the identity-named one" >> {
    // The identity-named codec answers from Avro's name hash: O(arity), no scan, no index. Under
    // the scan the transformed codec paid `fields.size` times that; a linear rung brings it back
    // within a small constant factor.
    val ident = warmNs(66, snake = false)(live)
    val snake = warmNs(66, snake = true)(live)
    println(f"  n=66: identity ${ident}%.1f ns, snake ${snake}%.1f ns, ratio ${snake / ident}%.2f")
    // Measured 242x under the scan and 3.6-5.1x under the index, across repeated runs on this box.
    snake / ident must beLessThan(15.0)
  }

end NominalResolutionCostSpec
