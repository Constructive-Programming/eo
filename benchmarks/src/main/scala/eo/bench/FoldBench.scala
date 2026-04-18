package eo
package bench

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import eo.optics.{Fold => EoFold}

import cats.instances.list.given

import monocle.{Fold => MFold}

/** `Fold.foldMap` over a `List[Int]`, paired EO vs Monocle.
  *
  * Both drop through a `Foldable[List]`; the EO side stores the
  * fold via `Forget[List]` carrier, the Monocle side via its
  * own `Fold.fromFoldable`. Summing the list's elements is the
  * worked example — cheap per element, so the per-element
  * `Monoid[Int].combine` and iteration cost dominate and any
  * typeclass-dispatch overhead becomes visible.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class FoldBench:

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var xs: List[Int] = uninitialized

  val eoFold = EoFold[List, Int]
  val mFold  = MFold.fromFoldable[List, Int]

  @Setup(Level.Iteration)
  def init(): Unit =
    xs = List.tabulate(size)(identity)

  @Benchmark def eoFoldMap: Int = eoFold.foldMap(identity[Int])(xs)
  @Benchmark def mFoldMap:  Int = mFold.foldMap(identity[Int])(xs)
