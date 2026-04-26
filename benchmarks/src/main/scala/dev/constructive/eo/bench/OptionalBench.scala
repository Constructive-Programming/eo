package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*

/** `Optional.modify` / `Optional.replace` on a `Nested0.flag: Option[String]` focus, at varying
  * composition depth, paired against Monocle's `Optional`.
  *
  * Three depths:
  *   - `_0`: leaf-only Optional, a bare Affine round-trip.
  *   - `_3`, `_6`: the leaf Optional preceded by a `Lens` chain through `Nested`.n.n…n. The Lens
  *     chain is `.morph`-ed into the `Affine` carrier and then `.andThen`-composed with the leaf
  *     `Optional`, mirroring exactly the shape of a Monocle composed `Optional`.
  *
  * Both None and Some branches are represented at depth 0 (via `eoModify_0_empty` /
  * `mModify_0_empty`). The deeper benches exercise the Some branch only — the fixture's depth-3 /
  * -6 records preserve the populated leaf.
  */
class OptionalBench extends JmhDefaults:

  import NestedOptics.{eoFlag, eoN1, eoN2, eoN3, eoN4, eoN5, eoN6, mFlag, mOpt3, mOpt6, leaf,
    leafEmpty, d3, d6}

  // ---- Composed EO optionals — Lens chain composed directly with the
  //      leaf Optional via cross-carrier `.andThen`. The Monocle peer
  //      `mOpt3` / `mOpt6` lives on the shared NestedOptics fixture.

  private val eoOpt3 =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlag)

  private val eoOpt6 =
    eoN6
      .andThen(eoN5)
      .andThen(eoN4)
      .andThen(eoN3)
      .andThen(eoN2)
      .andThen(eoN1)
      .andThen(eoFlag)

  // ---- Depth 0 (leaf) -----------------------------------------------

  @Benchmark def eoModify_0: Nested0 = eoFlag.modify(_.toUpperCase)(leaf)
  @Benchmark def mModify_0: Nested0 = mFlag.modify(_.toUpperCase)(leaf)

  @Benchmark def eoReplace_0: Nested0 = eoFlag.replace("world")(leaf)
  @Benchmark def mReplace_0: Nested0 = mFlag.replace("world")(leaf)

  @Benchmark def eoModify_0_empty: Nested0 = eoFlag.modify(_.toUpperCase)(leafEmpty)
  @Benchmark def mModify_0_empty: Nested0 = mFlag.modify(_.toUpperCase)(leafEmpty)

  // ---- Depth 3 ------------------------------------------------------

  @Benchmark def eoModify_3: Nested3 =
    eoOpt3.modify(_.toUpperCase)(d3)

  @Benchmark def mModify_3: Nested3 =
    mOpt3.modify(_.toUpperCase)(d3)

  // ---- Depth 6 ------------------------------------------------------

  @Benchmark def eoModify_6: Nested6 =
    eoOpt6.modify(_.toUpperCase)(d6)

  @Benchmark def mModify_6: Nested6 =
    mOpt6.modify(_.toUpperCase)(d6)
