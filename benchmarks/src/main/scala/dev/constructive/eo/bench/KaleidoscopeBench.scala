package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*

import cats.data.{Const, ZipList}

import dev.constructive.eo.data.Kaleidoscope

/** JMH bench for the Kaleidoscope carrier — EO-only (Monocle 3.3.0 ships no Kaleidoscope, verified
  * the same way as Grate: no matches under `cellar search-external`). Two fixtures per plan D7, one
  * per Reflector instance, because the whole point of Kaleidoscope is that `F` varies:
  *
  *   - **Fixture 1 — ZipList column-wise aggregation.** `List[Double]` walked as a ZipList and
  *     reduced to its mean via `.collect`. The Reflector's broadcast gives the same aggregate back
  *     at every position; the bench measures the overhead of the carrier + path-type cast chain vs.
  *     a hand-rolled `.sum / .size` loop.
  *   - **Fixture 2 — Const[Int, *] summation.** `Const[Int, Int]` projected through the phantom
  *     side; `.collect(identity)` reads the monoid value already carried by `Const`. Measures the
  *     overhead of routing through the Kaleidoscope carrier on a degenerate Reflector where the
  *     aggregation is a pass-through.
  *
  * Baselines are hand-rolled variants (no carrier allocation, no Reflector dispatch) — the
  * zero-overhead floor. Documents the cost envelope rather than a "Kaleidoscope is faster" story,
  * mirroring `GrateBench.scala` (plan 004 D6).
  *
  * Annotations mirror `GrateBench` / `AlgLensBench`. Run with:
  * {{{
  * sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 1 .*KaleidoscopeBench.*"
  * }}}
  */
class KaleidoscopeBench extends JmhDefaults:

  // Fixture 1 — ZipList column-wise mean. The data is a 16-element ZipList of Doubles; the
  // aggregator folds the whole ZipList into a single mean. Kaleidoscope's `.collect` threads that
  // mean back through the Reflector's broadcast, producing a ZipList where every position is the
  // same mean value.
  private val zipListK: dev.constructive.eo.optics.Optic[ZipList[Double], ZipList[
    Double
  ], Double, Double, Kaleidoscope] =
    Kaleidoscope.apply[ZipList, Double]

  private val zipData: ZipList[Double] =
    ZipList((1 to 16).toList.map(_.toDouble))

  private val meanAgg: ZipList[Double] => Double =
    zl => zl.value.sum / zl.value.size.toDouble

  @Benchmark def eoCollect_zipMean: ZipList[Double] =
    zipListK.collect[ZipList, Double](meanAgg)(zipData)

  @Benchmark def naive_zipMeanBroadcast: ZipList[Double] =
    val mean = zipData.value.sum / zipData.value.size.toDouble
    ZipList(List.fill(zipData.value.size)(mean))

  // Fixture 2 — Const[Int, *] summation. `Const[Int, Int]`'s `A` slot is phantom, so `.collect`
  // reduces to a retag — the aggregator is called once (exercising the Reflector's reflect path)
  // but the underlying Int value is pass-through. Measures carrier + dispatch overhead on the
  // smallest possible aggregation shape.
  private val constK
      : dev.constructive.eo.optics.Optic[Const[Int, Int], Const[Int, Int], Int, Int, Kaleidoscope] =
    Kaleidoscope.apply[Const[Int, *], Int]

  private val constData: Const[Int, Int] = Const(42)

  private val identityAgg: Const[Int, Int] => Int =
    _.getConst

  @Benchmark def eoCollect_constSum: Const[Int, Int] =
    constK.collect[Const[Int, *], Int](identityAgg)(constData)

  @Benchmark def naive_constSum: Const[Int, Int] =
    Const(constData.getConst)
