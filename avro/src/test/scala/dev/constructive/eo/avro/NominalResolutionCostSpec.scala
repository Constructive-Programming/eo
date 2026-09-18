package dev.constructive.eo.avro

import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** The COST gate for the nominal rung (issue #103).
  *
  * PR #98's rung is correct and stays correct; what it was not is affordable. It asked a per-case-
  * field helper for each name, and that helper linear-scanned EVERY schema field on an exact-name
  * miss, deliberately without an early exit (it had to see a second match to call the name
  * ambiguous). So the cost was `arity x fields.size x nameLength` — quadratic in the field count —
  * and it was paid by exactly the codecs the rung exists for, since a miss is what a name transform
  * guarantees. An identity-named codec answers from Avro's hash and never scanned. That rung is
  * still reachable as [[NominalResolutionOracle]], which this spec times side by side with the live
  * one.
  *
  * '''What is asserted, and what merely prints.''' This box is noisy — project policy is that ns/op
  * here is +/-15-50% and only within-run ratios are load-bearing — so nothing here asserts a
  * nanosecond count. The assertion is on the SHAPE: double the field count and a quadratic
  * implementation costs ~4x while a linear one costs ~2x. The absolute table is printed for the
  * record, never gated.
  *
  * '''Where the assertions run.''' A ratio of two same-run timings is the least noisy thing a clock
  * can give, but it is still a clock, and the default `sbt test` lane is three JDK jobs on shared
  * GitHub runners — the very place the ns/op policy says not to trust one. So the two ratio
  * examples are ARMED ONLY under `-Deo.costGate=true`, which `quality.yml` passes on release tags
  * and on demand, alongside the coverage and mutation reports. They skip, visibly and with a
  * reason, everywhere else. See `NominalCostFixtures.costGateArmed`.
  *
  * The table above them keeps printing on every lane. It costs seconds, gates nothing, and is how a
  * human notices drift between release-tag runs of the gate.
  *
  * A non-timing gate was looked for first and does not exist. The deterministic halves of the claim
  * are already pinned elsewhere — `NominalNameIndexCacheSpec` pins one index instance per schema
  * and no rebuild across repeated resolutions, `NominalResolutionParitySpec` pins every verdict —
  * and the part those cannot see is precisely "how much work per case field", which is not
  * observable from outside: `Schema` cannot be subclassed to count field accesses (its constructors
  * are package-private), and counting probes would mean a mutable counter on the hot path of the
  * method this issue exists to make cheap. Allocated bytes, the repo's other load-bearing metric,
  * is deterministic here but cannot see the regression that matters: the scan this replaced
  * allocated LESS than the index does, so a return to it would read as an improvement.
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
    if !costGateArmed then skipped(costGateDisarmed)
    else
      // 33 / 66 / 132: the middle row is the field count issue #103 measured in the wild. The three
      // sizes are measured INTERLEAVED — one pass of each, `Reps` rounds, per-size minimum — so a
      // burst of load on this (noisy) box lands on all three rather than inflating one ratio.
      val sizes = List(33, 66, 132)
      val fixed = sizes.map(n => (schema(n, snake = true), caseNames(n)))
      // Warm every size before the first timed round. The table example above happens to have run
      // these paths already (the spec is `sequential`), but that is a coincidence of ordering, not
      // a contract — this example must stand on its own if the table is ever moved or removed.
      fixed.foreach((s, ns) => sink.addAndGet(costNs(1, Iters)(_ => live(s, ns)).toLong))
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
      // Linear gives 2 and 4; quadratic gives 4 and 16. The thresholds sit between. The 4n/n bound
      // is the looser of the two because a linear rung still allocates one normalised key per case
      // field per resolution, so its 4x carries a GC tail (measured ~4.9-6.5); the pre-index rung
      // measured 15.4 there, so 10 still separates the two shapes with room to spare.
      (double must beLessThan(3.0)).and(quadruple must beLessThan(10.0))
  }

  "the snake_cased shape is not a different ORDER of cost from the identity-named one" >> {
    if !costGateArmed then skipped(costGateDisarmed)
    else
      // The identity-named codec answers from Avro's name hash: O(arity), no scan, no index. Under
      // the scan the transformed codec paid `fields.size` times that; a linear rung brings it back
      // within a small constant factor.
      val ident = warmNs(66, snake = false)(live)
      val snake = warmNs(66, snake = true)(live)
      println(
        f"  n=66: identity ${ident}%.1f ns, snake ${snake}%.1f ns, ratio ${snake / ident}%.2f"
      )
      // Measured 242x under the scan and 3.6-5.1x under the index, across repeated runs on this
      // box. The bound is loose on purpose: the DENOMINATOR is the identity-named path, which this
      // work does not touch, so every bit of its noise lands in the ratio.
      snake / ident must beLessThan(15.0)
  }

end NominalResolutionCostSpec
