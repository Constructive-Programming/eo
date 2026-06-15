package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*

/** `Modify.modify` (eo's write-only family, named `Setter` in Monocle) at the leaf plus deep
  * composition, paired EO vs Monocle.
  *
  * EO's `Modify` carrier `ModifyF` has both a `ForgetfulFunctor[ModifyF]` (powers `.modify`) and an
  * `AssociativeFunctor[ModifyF]` (`assocModifyF`), so two Modify optics compose through the
  * ordinary `andThen` — `s1.andThen(s2).modify(f) == s1.modify(s2.modify(f))`. The depth-3 /
  * depth-6 rows build a *composed* write-only optic on both sides (EO's `s3.andThen(s2)…` vs
  * Monocle's `mS3.andThen(mS2)…`) and dispatch through it once, rather than hand-nesting `modify`
  * on the EO side.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class SetterBench extends JmhDefaults:

  import NestedOptics.{
    d3,
    d6,
    eoS1,
    eoS2,
    eoS3,
    eoS4,
    eoS5,
    eoS6,
    eoSetValue,
    leaf,
    mSet3,
    mSet6,
    mSetValue,
  }
  import DomainOptics.{eoSetId, mSetId, order}

  // ---- canonical scalar focus: order.id ($.id) ----------------------

  @Benchmark def eoModify_orderId: Order = eoSetId.modify(_ + 1)(order)
  @Benchmark def mModify_orderId: Order = mSetId.modify(_ + 1)(order)

  // ---- Nested depth sweep (composition, which Order can't express) --

  // Both sides build a composed write-only optic and dispatch through it once.
  private val eoSet3 = eoS3.andThen(eoS2).andThen(eoS1).andThen(eoSetValue)

  private val eoSet6 =
    eoS6.andThen(eoS5).andThen(eoS4).andThen(eoS3).andThen(eoS2).andThen(eoS1).andThen(eoSetValue)

  @Benchmark def eoModify_0: Nested0 = eoSetValue.modify(_ + 1)(leaf)
  @Benchmark def mModify_0: Nested0 = mSetValue.modify(_ + 1)(leaf)

  @Benchmark def eoModify_3: Nested3 = eoSet3.modify(_ + 1)(d3)
  @Benchmark def mModify_3: Nested3 = mSet3.modify(_ + 1)(d3)

  @Benchmark def eoModify_6: Nested6 = eoSet6.modify(_ + 1)(d6)
  @Benchmark def mModify_6: Nested6 = mSet6.modify(_ + 1)(d6)
