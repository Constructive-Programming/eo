package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*

/** `Getter.get` at the leaf plus deep composition, paired EO vs Monocle.
  *
  * EO `Getter`s are `Optic[S, Unit, A, Unit, Direct]` — both the leftover `T` and the back-focus
  * `B` are `Unit`, so they compose through the ordinary `andThen` (the fused
  * `Getter.andThen`), just like `Iso` / `Lens`. The depth-3 / depth-6 rows therefore build a
  * *composed* `Getter` on both sides (EO's `g3.andThen(g2)…` vs Monocle's `mG3.andThen(mG2)…`) and
  * dispatch through it once — an apples-to-apples comparison, not EO's hand-nested `get` chain
  * against Monocle's composed reader.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class GetterBench extends JmhDefaults:

  import NestedOptics.{
    d3,
    d6,
    eoG1,
    eoG2,
    eoG3,
    eoG4,
    eoG5,
    eoG6,
    eoGetValue,
    leaf,
    mGet3,
    mGet6,
    mGetValue,
  }
  import DomainOptics.{eoGetId, mGetId, order}

  // ---- canonical scalar focus: order.id ($.id) ----------------------

  @Benchmark def eoGet_orderId: Long = eoGetId.get(order)
  @Benchmark def mGet_orderId: Long = mGetId.get(order)

  // ---- Nested depth sweep (composition, which Order can't express) --
  // Both sides build a composed Getter and dispatch through it once.

  private val eoGet3 = eoG3.andThen(eoG2).andThen(eoG1).andThen(eoGetValue)

  private val eoGet6 =
    eoG6.andThen(eoG5).andThen(eoG4).andThen(eoG3).andThen(eoG2).andThen(eoG1).andThen(eoGetValue)

  @Benchmark def eoGet_0: Int = eoGetValue.get(leaf)
  @Benchmark def mGet_0: Int = mGetValue.get(leaf)

  @Benchmark def eoGet_3: Int = eoGet3.get(d3)
  @Benchmark def mGet_3: Int = mGet3.get(d3)

  @Benchmark def eoGet_6: Int = eoGet6.get(d6)
  @Benchmark def mGet_6: Int = mGet6.get(d6)
