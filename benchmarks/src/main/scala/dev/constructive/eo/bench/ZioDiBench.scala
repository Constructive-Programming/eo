package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import _root_.zio.ZEnvironment
import bench.fixture.{DiDb, DiMetrics}
import optics.Lens
import zio.service

/** `cats-eo-zio` overhead: eo's `ZEnvironment` service lens (leaf and drilled through a field lens)
  * vs the hand-written `env.get` / `env.update` equivalent. Both sides pay ZEnvironment's own map
  * machinery; the delta is the optic hop.
  *
  * The effectful surfaces (`Ref` focus ops, `focusLayer`) are deliberately not benched: they are
  * one-line wrappers whose per-op cost is the same `modify`/`get` hop measured here, and an
  * `Unsafe.run` per invocation would drown that hop in ZIO runtime noise.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ZioDiBench extends JmhDefaults:

  private val db = DiDb("jdbc:h2", 4)
  private val env = ZEnvironment(db).add(DiMetrics("eo"))

  private val dbL = service[DiDb & DiMetrics, DiDb]
  private val urlL = Lens[DiDb, String](_.url, (d, u) => d.copy(url = u))
  private val dbUrl = dbL.andThen(urlL)

  // ---- leaf: the service slot itself ---------------------------------

  @Benchmark def eoServiceGet: DiDb = dbL.get(env)
  @Benchmark def handServiceGet: DiDb = env.get[DiDb]

  @Benchmark def eoServiceReplace: ZEnvironment[DiDb & DiMetrics] = dbL.replace(db)(env)
  @Benchmark def handServiceReplace: ZEnvironment[DiDb & DiMetrics] = env.update[DiDb](_ => db)

  // ---- drilled: environment -> service -> field -----------------------

  @Benchmark def eoDrillGet: String = dbUrl.get(env)
  @Benchmark def handDrillGet: String = env.get[DiDb].url

  @Benchmark def eoDrillModify: ZEnvironment[DiDb & DiMetrics] = dbUrl.modify(_ + "!")(env)

  @Benchmark def handDrillModify: ZEnvironment[DiDb & DiMetrics] =
    env.update[DiDb](d => d.copy(url = d.url + "!"))
