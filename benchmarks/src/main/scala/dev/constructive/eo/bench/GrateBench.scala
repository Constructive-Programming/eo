package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.data.Grate
import dev.constructive.eo.data.Grate.given
import dev.constructive.eo.optics.Optic.*

/** JMH bench for the Grate carrier — EO-only (Monocle 3.3.0 ships no Grate, verified via
  * `cellar search-external dev.optics:monocle-core_3:3.3.0 Grate` returning no results). The v1
  * fixture is a homogeneous `Tuple3[Double, Double, Double]` rewritten via `.modify(_ * 2)` —
  * classic "apply `A => B` uniformly to every slot" shape, the canonical use case.
  *
  * Baseline: `naive_tupleRewrite` applies the same function with plain tuple construction — the
  * zero-overhead floor. The gap tells us how much the paired-encoding carrier + `Tuple.fromArray`
  * detour cost relative to a hand-rolled copy.
  *
  * Documents the cost envelope rather than a "Grate is faster" story, mirroring the plan's D6.
  *
  * Annotations mirror `AlgLensBench.scala`: `@Fork(3)` for three forks, 3 warmup + 5 measurement
  * iterations of 1 second each. Run with:
  * {{{
  * sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 1 .*GrateBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class GrateBench:

  private val tripleGrate = Grate.tuple[(Double, Double, Double), Double]

  var data: (Double, Double, Double) = (1.0, 2.0, 3.0)

  @Benchmark def eoModify_grateTuple3: (Double, Double, Double) =
    tripleGrate.modify(_ * 2.0)(data)

  @Benchmark def naive_tupleRewrite: (Double, Double, Double) =
    (data._1 * 2.0, data._2 * 2.0, data._3 * 2.0)
