package dev.constructive.eo
package bench
package fixture

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{Lens => EoLens, Optional => EoOptional}

import monocle.{Lens => MLens, Optional => MOptional}

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
