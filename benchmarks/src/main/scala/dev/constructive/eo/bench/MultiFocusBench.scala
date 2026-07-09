package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import cats.instances.list.*
import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.MultiFocus
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** `MultiFocus[List]` (via `fromLensF`) vs `MultiFocus[PSVec]` (via `Traversal.each[List, _]`) on
  * the same `Lens[Order, List[LineItem]] → inner → Lens[LineItem, Int]` chain over the canonical
  * `Order.lines`. This isolates the per-optic-machinery cost of the two carriers on the common
  * "Lens over a List field" traversal shape; `naive_*` is the hand-rolled `copy` + `List.map`
  * baseline.
  *
  * The aggregator (`collect*`) and `MultiFocus.tuple` benches that used to share this file now live
  * in [[MultiFocusCollectBench]]; the duplicate ArraySeq PSVec path (identical to
  * [[PowerSeriesBench]]) was dropped — see plan 009, Phase 3.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class MultiFocusBench extends JmhDefaults:

  @Param(Array("4", "32", "256", "1024"))
  var size: Int = uninitialized

  var order: Order = uninitialized

  private val linesLens =
    EoLens[Order, List[LineItem]](_.lines, (o, ls) => o.copy(lines = ls))

  private val qtyLens =
    EoLens[LineItem, Int](_.qty, (li, q) => li.copy(qty = q))

  // Path A: MultiFocus[List] via fromLensF — cardinality-preserving chunking; the
  // tuple2multifocus singleton fast-path fires on the inner Lens.
  private val mfPath =
    MultiFocus.fromLensF(linesLens).andThen(qtyLens)

  // Path B: PowerSeries via Traversal.each[List, LineItem] — specialized
  // PSVec-backed traversal carrier. The chain needs the outer lens
  // explicitly because Traversal.each[List, LineItem] takes `List[LineItem]`,
  // not `Order`.
  private val psPath =
    linesLens
      .andThen(EoTraversal.each[List, LineItem])
      .andThen(qtyLens)

  @Setup(Level.Iteration)
  def init(): Unit =
    order = Domain.mkOrder(size)

  @Benchmark def eoModify_multiFocus: Order =
    mfPath.modify(_ + 1)(order)

  @Benchmark def eoModify_powerEach: Order =
    psPath.modify(_ + 1)(order)

  @Benchmark def naive_listMap: Order =
    order.copy(
      lines = order.lines.map(li => li.copy(qty = li.qty + 1))
    )
