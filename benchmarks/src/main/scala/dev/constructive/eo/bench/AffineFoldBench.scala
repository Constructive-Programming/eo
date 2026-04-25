package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{AffineFold, Lens => EoLens, Optional => EoOptional}
import dev.constructive.eo.optics.Optic.*

import monocle.{Lens => MLens, Optional => MOptional}

/** `AffineFold.getOption` on a `Nested0.flag: Option[String]` focus at varying composition depth,
  * paired against Monocle's `Optional.getOption` (Monocle 3.x does not ship a standalone
  * `AffineFold`; the closest Monocle equivalent of a read-only `AffineFold` is `Optional.getOption`
  * itself).
  *
  * Three depths:
  *   - `_0`: leaf-only AffineFold; bare Affine read round-trip.
  *   - `_3`, `_6`: leaf narrowed via `AffineFold.fromOptional(composed)` — builds a full composed
  *     Optional through the Lens chain, then drops its write path. This is the recommended
  *     composition route (see `OpticsBehaviorSpec.AffineFold`).
  *
  * Both `None` and `Some` branches are represented at depth 0 (`_0_empty` vs the populated leaf) to
  * show the hit/miss cost split.
  *
  * A secondary EO-vs-EO comparison shows the specialisation win: the same read against a full
  * `Optional` (X = (S, S) shape) vs the `AffineFold` (X = (Unit, Unit) shape) at the leaf.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AffineFoldBench:

  // ---- Leaf ---------------------------------------------------------

  private val eoFlagAF: AffineFold[Nested0, String] =
    AffineFold[Nested0, String](_.flag)

  private val eoFlagOpt =
    EoOptional[Nested0, Nested0, String, String, Affine](
      getOrModify = n0 => n0.flag.toRight(n0),
      reverseGet = (pair: (Nested0, String)) => pair._1.copy(flag = Some(pair._2)),
    )

  private val mFlag: MOptional[Nested0, String] =
    MOptional[Nested0, String](_.flag)(s => n0 => n0.copy(flag = Some(s)))

  // ---- Per-level lenses ---------------------------------------------

  private val eoN1 = EoLens[Nested1, Nested0](_.n, (x, v) => x.copy(n = v))
  private val eoN2 = EoLens[Nested2, Nested1](_.n, (x, v) => x.copy(n = v))
  private val eoN3 = EoLens[Nested3, Nested2](_.n, (x, v) => x.copy(n = v))
  private val eoN4 = EoLens[Nested4, Nested3](_.n, (x, v) => x.copy(n = v))
  private val eoN5 = EoLens[Nested5, Nested4](_.n, (x, v) => x.copy(n = v))
  private val eoN6 = EoLens[Nested6, Nested5](_.n, (x, v) => x.copy(n = v))

  private val mN1: MLens[Nested1, Nested0] = MLens[Nested1, Nested0](_.n)(v => x => x.copy(n = v))
  private val mN2: MLens[Nested2, Nested1] = MLens[Nested2, Nested1](_.n)(v => x => x.copy(n = v))
  private val mN3: MLens[Nested3, Nested2] = MLens[Nested3, Nested2](_.n)(v => x => x.copy(n = v))
  private val mN4: MLens[Nested4, Nested3] = MLens[Nested4, Nested3](_.n)(v => x => x.copy(n = v))
  private val mN5: MLens[Nested5, Nested4] = MLens[Nested5, Nested4](_.n)(v => x => x.copy(n = v))
  private val mN6: MLens[Nested6, Nested5] = MLens[Nested6, Nested5](_.n)(v => x => x.copy(n = v))

  // ---- Composed Optionals narrowed to AffineFold --------------------

  private val eoOpt3 =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlagOpt)

  private val eoOpt6 =
    eoN6
      .andThen(eoN5)
      .andThen(eoN4)
      .andThen(eoN3)
      .andThen(eoN2)
      .andThen(eoN1)
      .andThen(eoFlagOpt)

  // `fromOptional` here takes Optional[S, S, A, A]; the composed values
  // above are Optic values that are structurally Optional but typed as
  // `Optic`. We hit them through the generic `.getOption` extension on
  // `Optic[_, _, _, _, Affine]` instead of narrowing via `fromOptional`,
  // which would require the concrete Optional type. Same observable
  // `.getOption` path.

  private val mOpt3 = mN3.andThen(mN2).andThen(mN1).andThen(mFlag)

  private val mOpt6 =
    mN6.andThen(mN5).andThen(mN4).andThen(mN3).andThen(mN2).andThen(mN1).andThen(mFlag)

  // ---- Inputs -------------------------------------------------------

  private val leaf: Nested0 = Nested.DefaultLeaf
  private val leafEmpty: Nested0 = Nested.EmptyFlagLeaf
  private val d3: Nested3 = Nested.Default3
  private val d6: Nested6 = Nested.Default6

  // ---- Depth 0 ------------------------------------------------------

  @Benchmark def eoGetOption_0: Option[String] = eoFlagAF.getOption(leaf)
  @Benchmark def mGetOption_0: Option[String] = mFlag.getOption(leaf)

  @Benchmark def eoGetOption_0_empty: Option[String] = eoFlagAF.getOption(leafEmpty)
  @Benchmark def mGetOption_0_empty: Option[String] = mFlag.getOption(leafEmpty)

  // ---- Depth 0 — EO specialisation comparison -----------------------
  // AffineFold (X = (Unit, Unit)) vs full Optional (X = (Nested0, Nested0)).
  // Observable contract is identical; this row quantifies the per-Hit
  // allocation saving from dropping the unused S slot.

  @Benchmark def eoGetOption_0_asAffineFold: Option[String] = eoFlagAF.getOption(leaf)
  @Benchmark def eoGetOption_0_asOptional: Option[String] = eoFlagOpt.getOption(leaf)

  // ---- Depth 3 ------------------------------------------------------

  @Benchmark def eoGetOption_3: Option[String] = eoOpt3.getOption(d3)
  @Benchmark def mGetOption_3: Option[String] = mOpt3.getOption(d3)

  // ---- Depth 6 ------------------------------------------------------

  @Benchmark def eoGetOption_6: Option[String] = eoOpt6.getOption(d6)
  @Benchmark def mGetOption_6: Option[String] = mOpt6.getOption(d6)
