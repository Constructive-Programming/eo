package dev.constructive.eo.avro

import scala.annotation.tailrec

import java.util.ArrayList
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import org.apache.avro.Schema

/** Schema generators and a min-of-reps timer for the issue-#103 cost specs.
  *
  * Two codec shapes, because the nominal rung's two rungs have wildly different costs and only one
  * of them was ever exercised by the #98 benchmark:
  *
  *   - IDENTITY-named — the schema field is spelled exactly like the case field, so Avro's own
  *     per-record name hash answers in O(1) and nothing else runs. This is the control: it must not
  *     move.
  *   - SNAKE_CASED — every exact lookup misses, which is the population the nominal rung exists for
  *     (an identity-named codec was already right positionally). This is where the cost lives.
  */
object NominalCostFixtures:

  /** Fixed-width names so the per-comparison character count does not drift with the field count —
    * otherwise a cost ratio would conflate `fields.size` with `nameLength`.
    */
  def fieldName(i: Int, snake: Boolean): String =
    val n = f"$i%03d"
    if snake then s"alpha_beta_gamma_$n" else s"alphaBetaGamma$n"

  /** The Scala case-field names: always camelCase, whatever the schema spells. */
  def caseNames(n: Int): List[String] = List.tabulate(n)(i => fieldName(i, snake = false))

  private val serial = new AtomicInteger(0)

  /** A fresh record schema of `n` string fields. Distinct record names, so no two generated schemas
    * are structurally equal by accident.
    */
  def schema(n: Int, snake: Boolean): Schema =
    named(s"Rec${serial.incrementAndGet()}", n, snake)

  /** As [[schema]], with the record name pinned — two calls with one name give STRUCTURALLY EQUAL
    * but non-identical schemas, which is how the cache's identity keying is pinned.
    */
  def named(recordName: String, n: Int, snake: Boolean): Schema =
    val fields = new ArrayList[Schema.Field](n)
    (0 until n).foreach: i =>
      fields.add(
        new Schema.Field(fieldName(i, snake), Schema.create(Schema.Type.STRING), null, null)
      )
    Schema.createRecord(recordName, null, "eo.perf", false, fields)

  /** Keeps the measured results reachable so nothing under test folds away. */
  val sink: AtomicLong = new AtomicLong(0L)

  /** Nanoseconds per resolution, MIN over `reps` passes of `iters` resolutions.
    *
    * Min and not mean: this box is noisy (project policy — ns/op here is +/-15-50%, only within-run
    * ratios are load-bearing), and the minimum is the sample least contaminated by whatever else
    * the machine was doing. Every committed assertion is on a RATIO of two such numbers taken in
    * the same run, never on an absolute.
    */
  def costNs(reps: Int, iters: Int)(op: Int => Int): Double =
    @tailrec def pass(r: Int, acc: Double): Double =
      if r <= 0 then acc
      else
        @tailrec def run(i: Int, total: Int): Int =
          if i >= iters then total else run(i + 1, total + op(i))
        val t0 = System.nanoTime()
        val out = run(0, 0)
        val t1 = System.nanoTime()
        sink.addAndGet(out.toLong): Unit
        val ns = (t1 - t0).toDouble / iters.toDouble
        pass(r - 1, if ns < acc then ns else acc)
    pass(reps, Double.MaxValue)

  /** Cost of the FIRST resolution against each of `k` never-before-seen schemas — one single pass,
    * because a second pass would find them all cached and would be measuring something else. Schema
    * construction happens outside the clock.
    */
  def coldCostNs(n: Int, snake: Boolean, k: Int)(resolve: (Schema, List[String]) => Int): Double =
    val names = caseNames(n)
    val warmup = Array.fill(k)(schema(n, snake))
    warmup.foreach(s => sink.addAndGet(resolve(s, names).toLong))
    val fresh = Array.fill(k)(schema(n, snake))
    val t0 = System.nanoTime()
    @tailrec def run(i: Int, total: Int): Int =
      if i >= k then total else run(i + 1, total + resolve(fresh(i), names))
    val out = run(0, 0)
    val t1 = System.nanoTime()
    sink.addAndGet(out.toLong): Unit
    (t1 - t0).toDouble / k.toDouble

end NominalCostFixtures
