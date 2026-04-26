package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.optics.AffineFold

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
class AffineFoldBench extends JmhDefaults:

  private val eoFlagAF: AffineFold[Nested0, String] =
    AffineFold[Nested0, String](_.flag)

  import NestedOptics.{eoFlag => eoFlagOpt, eoN1, eoN2, eoN3, eoN4, eoN5, eoN6, mFlag, mOpt3,
    mOpt6, leaf, leafEmpty, d3, d6}

  // ---- Composed EO Optionals narrowed to AffineFold via .getOption.
  //      The Monocle `mOpt3` / `mOpt6` peers live on the shared NestedOptics
  //      fixture.

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
