package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import _root_.kyo.{Env, TypeMap, Var}
import bench.fixture.{DiDb, DiMetrics}
import kyo.*
import optics.Lens

/** `cats-eo-kyo` overhead, two tiers:
  *
  *   - '''TypeMap tier''' — eo's `service` lens (leaf and drilled) vs hand-written `tm.get` /
  *     `tm.add`. Pure map ops; the delta is the optic hop, same shape as ZioDiBench.
  *   - '''Effect tier''' — `Env.focus` / `Var.updateFocus` vs the hand-written `Env.use` /
  *     `Var.updateDiscard` lambda, both run to completion with `.eval`. Both sides pay kyo's full
  *     suspend/handle machinery, so the pair isolates the capability hop inside a realistic
  *     effectful op (kyo's pure eval is cheap enough to keep the delta visible, unlike a per-op ZIO
  *     `Unsafe.run`).
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class KyoDiBench extends JmhDefaults:

  private val db = DiDb("jdbc:h2", 4)
  private val tm = TypeMap(db, DiMetrics("eo"))

  private val dbL = service[DiDb & DiMetrics, DiDb]
  private val urlL = Lens[DiDb, String](_.url, (d, u) => d.copy(url = u))
  private val dbUrl = dbL.andThen(urlL)

  // ---- TypeMap tier: pure map ops -------------------------------------

  @Benchmark def eoMapGet: DiDb = dbL.get(tm)
  @Benchmark def handMapGet: DiDb = tm.get[DiDb]

  @Benchmark def eoMapDrillModify: TypeMap[DiDb & DiMetrics] = dbUrl.modify(_ + "!")(tm)

  @Benchmark def handMapDrillModify: TypeMap[DiDb & DiMetrics] =
    val d = tm.get[DiDb]
    tm.add(d.copy(url = d.url + "!"))

  // ---- Effect tier: full suspend/handle/eval on both sides ------------

  @Benchmark def eoEnvFocus: String =
    Env.run(db)(Env.focus[DiDb, String](using urlL)).eval

  @Benchmark def handEnvUse: String =
    Env.run(db)(Env.use[DiDb](_.url)).eval

  @Benchmark def eoVarUpdateFocus: (DiDb, Unit) =
    Var.runTuple(db)(Var.updateFocus[DiDb, String](_ + "!")(using urlL)).eval

  @Benchmark def handVarUpdate: (DiDb, Unit) =
    Var.runTuple(db)(Var.updateDiscard[DiDb](d => d.copy(url = d.url + "!"))).eval
