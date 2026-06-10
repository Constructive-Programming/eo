package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*

/** `Review.reverseGet` at the leaf plus deep composition — the BUILD-direction mirror of
  * [[GetterBench]], and the coverage the fused [[dev.constructive.eo.optics.Review.andThen]] needs.
  *
  * EO `Review`s are `Optic[Unit, S, Unit, A, Direct]` — vestigial `to`, a real `from`/`reverseGet`
  * that builds `S` from the focus `A`. They compose through the fused `Review.andThen(Review)` just
  * as `Getter`s do, so the depth-3 / depth-6 rows build a *composed* `Review` **once, outside the
  * hot path** (`private val`, so construction happens at fork start, not per-invocation — this is
  * the construction/usage split [[CompositionBench]] measures the other half of), then dispatch
  * `reverseGet` through it. Monocle has no `Review`, so the peer is a hand-written builder (the
  * floor: plain nested `new`s, no optic).
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ReviewBench extends JmhDefaults:

  import NestedOptics.{eoR1, eoR2, eoR3, eoR4, eoR5, eoR6, eoRValue}

  // Composed Reviews built once at fork start (out of the measured method), so
  // each @Benchmark isolates `reverseGet` dispatch from the per-`andThen`
  // construction cost CompositionBench measures separately.
  private val eoReview3 = eoR3.andThen(eoR2).andThen(eoR1).andThen(eoRValue)

  private val eoReview6 =
    eoR6.andThen(eoR5).andThen(eoR4).andThen(eoR3).andThen(eoR2).andThen(eoR1).andThen(eoRValue)

  private val seed: Int = 42

  @Benchmark def eoReverseGet_0: Nested0 = eoRValue.reverseGet(seed)
  @Benchmark def eoReverseGet_3: Nested3 = eoReview3.reverseGet(seed)
  @Benchmark def eoReverseGet_6: Nested6 = eoReview6.reverseGet(seed)

  // Floor: the same nodes built by hand, no optic.
  @Benchmark def naiveBuild_0: Nested0 = Nested0(seed, None, Nil)

  @Benchmark def naiveBuild_3: Nested3 =
    Nested3(Nested2(Nested1(Nested0(seed, None, Nil))))

  @Benchmark def naiveBuild_6: Nested6 =
    Nested6(Nested5(Nested4(Nested3(Nested2(Nested1(Nested0(seed, None, Nil)))))))
