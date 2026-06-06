package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*

/** Cross-carrier `.andThen` composition cost — EO's signature feature (plan 009, Phase 2).
  *
  * Two axes, on the shared [[fixture.Nested]] chain:
  *   - **build** — constructing a depth-N composed optic from its hops (`a.andThen(b).andThen…`).
  *     The result is returned so JMH consumes it; this isolates the per-`andThen` allocation +
  *     `Composer` dispatch cost.
  *   - **reuse** — `.modify` through a pre-built depth-N optic (the steady-state cost once the optic
  *     is a constant).
  *
  * Two carrier shapes:
  *   - **same-carrier** `Lens.andThen(Lens)` (`Tuple2`), depths 3 and 6.
  *   - **cross-carrier** `Lens…andThen(Optional)` — the `Tuple2` Lens chain is lifted into the
  *     `Affine` carrier by an automatic `Morph[Tuple2, Affine]`, no explicit `.morph` at the call
  *     site. This is the composition EO does that a fixed-carrier optic library can't.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class CompositionBench extends JmhDefaults:

  import NestedOptics.{d3, d6, eoFlag, eoN1, eoN2, eoN3, eoN4, eoN5, eoN6, leaf}

  // Leaf Lens on Nested0.value (NestedOptics ships a Getter / Setter for it,
  // but composition needs a full Lens).
  private val leafValue =
    EoLens[Nested0, Int](_.value, (n, v) => n.copy(value = v))

  // Pre-built composed optics for the reuse axis.
  private val lens3 = eoN3.andThen(eoN2).andThen(eoN1).andThen(leafValue)
  private val lens6 =
    eoN6.andThen(eoN5).andThen(eoN4).andThen(eoN3).andThen(eoN2).andThen(eoN1).andThen(leafValue)
  private val opt3 = eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlag)

  // ---- same-carrier Lens.andThen(Lens) (Tuple2) ---------------------

  @Benchmark def buildLens3: Any =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(leafValue)

  @Benchmark def buildLens6: Any =
    eoN6.andThen(eoN5).andThen(eoN4).andThen(eoN3).andThen(eoN2).andThen(eoN1).andThen(leafValue)

  @Benchmark def reuseLens3: Nested3 = lens3.modify(_ + 1)(d3)
  @Benchmark def reuseLens6: Nested6 = lens6.modify(_ + 1)(d6)

  // ---- cross-carrier Lens…andThen(Optional) (Tuple2 → Affine) -------

  @Benchmark def buildLensOptional3: Any =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlag)

  @Benchmark def reuseLensOptional3: Nested3 = opt3.modify(_.toUpperCase)(d3)

  // ---- depth-1 reference + uncomposed leaf floor --------------------

  private val lens1 = eoN1.andThen(leafValue)

  @Benchmark def buildLens1: Any = eoN1.andThen(leafValue)
  @Benchmark def reuseLens1: Nested1 = lens1.modify(_ + 1)(Nested.Default1)

  // Uncomposed leaf Lens — the floor (no `.andThen` at all).
  @Benchmark def reuseLeaf: Nested0 = leafValue.modify(_ + 1)(leaf)
