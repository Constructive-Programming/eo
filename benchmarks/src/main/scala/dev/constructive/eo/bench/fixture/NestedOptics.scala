package dev.constructive.eo
package bench
package fixture

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{
  Getter => EoGetter,
  Lens => EoLens,
  Optional => EoOptional,
  Setter => EoSetter,
}

import monocle.{Getter => MGetter, Lens => MLens, Optional => MOptional, Setter => MSetter}

/** Pre-built EO and Monocle lenses / optionals over the [[Nested]] case-class chain. The
  * `OptionalBench`, `AffineFoldBench`, and any future bench that wants the same depth-0/3/6 shape
  * pulls these from here instead of redefining the same six EO + six Monocle lenses (and the leaf
  * `Optional` over `Nested0.flag`) in the per-class scope.
  *
  * Constants only. Each `@Benchmark` method still lives on its owning class — JMH requires
  * per-class state — but the JMH state ''values'' it pulls in via `private val` aliases come from
  * here. A bench class declares e.g. `private val eoFlag = NestedOptics.eoFlag`; the JIT sees the
  * same constant (the object's static field) at every call site.
  */
object NestedOptics:

  // ---- Leaf optionals on Nested0.flag ------------------------------

  /** EO `Optional[Nested0, String]` over `flag: Option[String]`. Type is left to inference so the
    * concrete `Optional` subclass surfaces (callers that need the fused-andThen path see the same
    * shape they would have if they declared the val locally).
    */
  val eoFlag =
    EoOptional[Nested0, Nested0, String, String, Affine](
      getOrModify = n0 => n0.flag.toRight(n0),
      reverseGet = (pair: (Nested0, String)) => pair._1.copy(flag = Some(pair._2)),
    )

  /** Monocle `Optional[Nested0, String]` over the same `flag` field. */
  val mFlag: MOptional[Nested0, String] =
    MOptional[Nested0, String](_.flag)(s => n0 => n0.copy(flag = Some(s)))

  // ---- Per-level EO lenses (Tuple2 carrier) -------------------------
  //
  // Inferred return type is `GetReplaceLens` — the same fused-friendly
  // shape the per-class bench code used to declare locally.

  val eoN1 = EoLens[Nested1, Nested0](_.n, (x, v) => x.copy(n = v))
  val eoN2 = EoLens[Nested2, Nested1](_.n, (x, v) => x.copy(n = v))
  val eoN3 = EoLens[Nested3, Nested2](_.n, (x, v) => x.copy(n = v))
  val eoN4 = EoLens[Nested4, Nested3](_.n, (x, v) => x.copy(n = v))
  val eoN5 = EoLens[Nested5, Nested4](_.n, (x, v) => x.copy(n = v))
  val eoN6 = EoLens[Nested6, Nested5](_.n, (x, v) => x.copy(n = v))

  // ---- Per-level Monocle lenses ------------------------------------

  val mN1: MLens[Nested1, Nested0] = MLens[Nested1, Nested0](_.n)(v => x => x.copy(n = v))
  val mN2: MLens[Nested2, Nested1] = MLens[Nested2, Nested1](_.n)(v => x => x.copy(n = v))
  val mN3: MLens[Nested3, Nested2] = MLens[Nested3, Nested2](_.n)(v => x => x.copy(n = v))
  val mN4: MLens[Nested4, Nested3] = MLens[Nested4, Nested3](_.n)(v => x => x.copy(n = v))
  val mN5: MLens[Nested5, Nested4] = MLens[Nested5, Nested4](_.n)(v => x => x.copy(n = v))
  val mN6: MLens[Nested6, Nested5] = MLens[Nested6, Nested5](_.n)(v => x => x.copy(n = v))

  // ---- Pre-composed Monocle Optional chains -----------------------
  //
  // Both `OptionalBench` and `AffineFoldBench` build the depth-3 / depth-6
  // Monocle Optional chain by walking `mN3.andThen(mN2).andThen(mN1).andThen(mFlag)`
  // (and the longer 6-deep variant). The chains were identical 12-line
  // private-val blocks in each bench; centralising them here removes the
  // duplicate without changing call-site behaviour (each bench still pulls
  // them in via `import NestedOptics.*` so the JIT sees the same constant).

  val mOpt3: MOptional[Nested3, String] =
    mN3.andThen(mN2).andThen(mN1).andThen(mFlag)

  val mOpt6: MOptional[Nested6, String] =
    mN6.andThen(mN5).andThen(mN4).andThen(mN3).andThen(mN2).andThen(mN1).andThen(mFlag)

  // ---- Inputs (also shared across benches) -----------------------

  val leaf: Nested0 = Nested.DefaultLeaf
  val leafEmpty: Nested0 = Nested.EmptyFlagLeaf
  val d3: Nested3 = Nested.Default3
  val d6: Nested6 = Nested.Default6

  // ---- Per-level EO Getters (Forgetful carrier) -----------------

  val eoGetValue = EoGetter[Nested0, Int](_.value)

  val eoG1 = EoGetter[Nested1, Nested0](_.n)
  val eoG2 = EoGetter[Nested2, Nested1](_.n)
  val eoG3 = EoGetter[Nested3, Nested2](_.n)
  val eoG4 = EoGetter[Nested4, Nested3](_.n)
  val eoG5 = EoGetter[Nested5, Nested4](_.n)
  val eoG6 = EoGetter[Nested6, Nested5](_.n)

  // ---- Per-level Monocle Getters + composed get-3/get-6 -----------

  val mGetValue = MGetter[Nested0, Int](_.value)

  val mG1 = MGetter[Nested1, Nested0](_.n)
  val mG2 = MGetter[Nested2, Nested1](_.n)
  val mG3 = MGetter[Nested3, Nested2](_.n)
  val mG4 = MGetter[Nested4, Nested3](_.n)
  val mG5 = MGetter[Nested5, Nested4](_.n)
  val mG6 = MGetter[Nested6, Nested5](_.n)

  val mGet3 = mG3.andThen(mG2).andThen(mG1).andThen(mGetValue)

  val mGet6 =
    mG6.andThen(mG5).andThen(mG4).andThen(mG3).andThen(mG2).andThen(mG1).andThen(mGetValue)

  // ---- Per-level EO Setters (SetterF carrier) ---------------------

  val eoSetValue =
    EoSetter[Nested0, Nested0, Int, Int](f => n0 => n0.copy(value = f(n0.value)))

  val eoS1 = EoSetter[Nested1, Nested1, Nested0, Nested0](f => x => x.copy(n = f(x.n)))
  val eoS2 = EoSetter[Nested2, Nested2, Nested1, Nested1](f => x => x.copy(n = f(x.n)))
  val eoS3 = EoSetter[Nested3, Nested3, Nested2, Nested2](f => x => x.copy(n = f(x.n)))
  val eoS4 = EoSetter[Nested4, Nested4, Nested3, Nested3](f => x => x.copy(n = f(x.n)))
  val eoS5 = EoSetter[Nested5, Nested5, Nested4, Nested4](f => x => x.copy(n = f(x.n)))
  val eoS6 = EoSetter[Nested6, Nested6, Nested5, Nested5](f => x => x.copy(n = f(x.n)))

  // ---- Per-level Monocle Setters + composed set-3/set-6 -----------

  val mSetValue: MSetter[Nested0, Int] =
    MSetter[Nested0, Int](f => n0 => n0.copy(value = f(n0.value)))

  val mS1: MSetter[Nested1, Nested0] =
    MSetter[Nested1, Nested0](f => x => x.copy(n = f(x.n)))

  val mS2: MSetter[Nested2, Nested1] =
    MSetter[Nested2, Nested1](f => x => x.copy(n = f(x.n)))

  val mS3: MSetter[Nested3, Nested2] =
    MSetter[Nested3, Nested2](f => x => x.copy(n = f(x.n)))

  val mS4: MSetter[Nested4, Nested3] =
    MSetter[Nested4, Nested3](f => x => x.copy(n = f(x.n)))

  val mS5: MSetter[Nested5, Nested4] =
    MSetter[Nested5, Nested4](f => x => x.copy(n = f(x.n)))

  val mS6: MSetter[Nested6, Nested5] =
    MSetter[Nested6, Nested5](f => x => x.copy(n = f(x.n)))

  val mSet3: MSetter[Nested3, Int] =
    mS3.andThen(mS2).andThen(mS1).andThen(mSetValue)

  val mSet6: MSetter[Nested6, Int] =
    mS6.andThen(mS5).andThen(mS4).andThen(mS3).andThen(mS2).andThen(mS1).andThen(mSetValue)
