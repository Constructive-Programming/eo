package eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import eo.bench.fixture.*
import eo.data.Affine
import eo.optics.{Lens => EoLens, Optic, Optional => EoOptional}

import monocle.{Lens => MLens, Optional => MOptional}

/** `Optional.modify` / `Optional.replace` on a `Nested0.flag:
  * Option[String]` focus, at varying composition depth, paired
  * against Monocle's `Optional`.
  *
  * Three depths:
  *   - `_0`: leaf-only Optional, so a bare Affine round-trip.
  *   - `_3`, `_6`: the leaf Optional preceded by a `Lens` chain
  *     through `Nested`.n.n…n. Both the Some and None branches are
  *     represented at depth 0.
  *
  * **Why the depth-3 / depth-6 EO benches use `Lens.modify ∘
  * Optional.modify`** rather than a single `.andThen`-composed
  * Optic: `Affine.assoc[X <: Tuple, Y <: Tuple]` requires each
  * input's existential `X` to be provably `<: Tuple`, but that
  * proof is erased through `.morph[Affine]`, so direct Lens →
  * Optional `.andThen` composition does not type-check in EO at
  * present. The by-hand `chain.modify(optional.modify(f))` form
  * measures the same runtime work that a composed optic would
  * perform — the lens chain runs top-down, the optional's
  * modify runs at the leaf — while sidestepping the typeclass
  * lookup.  Monocle has no such restriction, so its side uses
  * the composed form directly; the bench pair stays honest
  * about what each API supports. Filed as EO tech debt — fixing
  * would require adding a Tuple witness through `morph` or a
  * dedicated `AssociativeFunctor[Affine, X, Y]` with unbounded
  * X/Y.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class OptionalBench:

  // ---- Leaf optionals -----------------------------------------------

  private val eoFlag: Optic[Nested0, Nested0, String, String, Affine] =
    EoOptional[Nested0, Nested0, String, String, Affine](
      getOrModify = n0 => n0.flag.toRight(n0),
      reverseGet  = (pair: (Nested0, String)) => pair._1.copy(flag = Some(pair._2)),
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

  // ---- EO: Lens-chain composition (Tuple2 stays in Tuple2) ---------

  private val eoChain3 = eoN3.andThen(eoN2).andThen(eoN1)
  private val eoChain6 =
    eoN6.andThen(eoN5).andThen(eoN4).andThen(eoN3).andThen(eoN2).andThen(eoN1)

  // ---- Monocle: fully composed Optional ----------------------------

  private val mOpt3 = mN3.andThen(mN2).andThen(mN1).andThen(mFlag)
  private val mOpt6 =
    mN6.andThen(mN5).andThen(mN4).andThen(mN3).andThen(mN2).andThen(mN1).andThen(mFlag)

  // ---- Inputs -------------------------------------------------------

  private val leaf:      Nested0 = Nested.DefaultLeaf
  private val leafEmpty: Nested0 = Nested.EmptyFlagLeaf
  private val d3:        Nested3 = Nested.Default3
  private val d6:        Nested6 = Nested.Default6

  // ---- Depth 0 (leaf) -----------------------------------------------

  @Benchmark def eoModify_0:       Nested0 = eoFlag.modify(_.toUpperCase)(leaf)
  @Benchmark def mModify_0:        Nested0 = mFlag.modify(_.toUpperCase)(leaf)

  @Benchmark def eoReplace_0:      Nested0 = eoFlag.replace("world")(leaf)
  @Benchmark def mReplace_0:       Nested0 = mFlag.replace("world")(leaf)

  @Benchmark def eoModify_0_empty: Nested0 = eoFlag.modify(_.toUpperCase)(leafEmpty)
  @Benchmark def mModify_0_empty:  Nested0 = mFlag.modify(_.toUpperCase)(leafEmpty)

  // ---- Depth 3 ------------------------------------------------------

  @Benchmark def eoModify_3: Nested3 =
    eoChain3.modify(eoFlag.modify(_.toUpperCase))(d3)
  @Benchmark def mModify_3:  Nested3 =
    mOpt3.modify(_.toUpperCase)(d3)

  // ---- Depth 6 ------------------------------------------------------

  @Benchmark def eoModify_6: Nested6 =
    eoChain6.modify(eoFlag.modify(_.toUpperCase))(d6)
  @Benchmark def mModify_6:  Nested6 =
    mOpt6.modify(_.toUpperCase)(d6)
