package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import cats.instances.double.given
import cats.instances.list.given
import dev.constructive.eo.bench.fixture.{Domain, LineItem}
import dev.constructive.eo.optics.Fold as EoFold
import java.util.concurrent.TimeUnit
import monocle.Fold as MFold
import org.openjdk.jmh.annotations.*

/** `Fold.foldMap` over a `List[Int]`, paired EO vs Monocle.
  *
  * Both drop through a `Foldable[List]`; the EO side stores the fold via `Forget[List]` carrier,
  * the Monocle side via its own `Fold.fromFoldable`. Summing the list's elements is the worked
  * example — cheap per element, so the per-element `Monoid[Int].combine` and iteration cost
  * dominate and any typeclass-dispatch overhead becomes visible.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class FoldBench extends JmhDefaults:

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var xs: List[Int] = uninitialized

  // Real-world fold: sum a price field across a list of records (canonical
  // `LineItem`s), the in-memory analogue of the JSON `lines[*].price` fold.
  var lines: List[LineItem] = uninitialized

  val eoFold = EoFold[List, Int]
  val mFold = MFold.fromFoldable[List, Int]

  val eoLineFold = EoFold[List, LineItem]
  val mLineFold = MFold.fromFoldable[List, LineItem]

  @Setup(Level.Iteration)
  def init(): Unit =
    xs = List.tabulate(size)(identity)
    lines = Domain.mkOrder(size).lines

  @Benchmark def eoFoldMap: Int = eoFold.foldMap(identity[Int])(xs)
  @Benchmark def mFoldMap: Int = mFold.foldMap(identity[Int])(xs)

  @Benchmark def eoFoldPrices: Double = eoLineFold.foldMap[Double](_.price)(lines)
  @Benchmark def mFoldPrices: Double = mLineFold.foldMap[Double](_.price)(lines)
