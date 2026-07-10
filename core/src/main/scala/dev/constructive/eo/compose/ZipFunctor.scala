package dev.constructive.eo
package compose

import data.{Affine, Snd}
import data.Affine.{Hit, Miss}
import optics.Optic

/** Fanout algebra for two optics on the **same source** `S` and **same carrier** `F` — the `zip`
  * (arrow `&&&`) counterpart of [[AssociativeFunctor]] (which handles `andThen`). A concrete
  * instance per carrier hosts the hot `to` / `from`, so the JIT sees monomorphic carrier ops and
  * escape analysis scalar-replaces the intermediate the write materialises — the generic
  * (carrier-erased) body cannot, being forced through `ForgetfulFunctor.map` and a closure. `X1` /
  * `X2` are the two legs' existentials, named distinct from `Optic#X` so `zip` can refinement-type
  * on them (same device as `AssociativeFunctor`'s `Xo` / `Xi`); `Out` is the result carrier the
  * instance chooses (`Tuple2` for total legs, `Affine` for partial).
  *
  * '''Monomorphic (`S = T`).''' The write reconciles the two legs *sequentially*: rebuild through
  * leg-1, then re-read leg-2 on the intermediate `S` so leg-2's write lands on top. This needs the
  * intermediate to be an `S`, so source and result coincide — and it means no caller-supplied merge
  * is required (the re-read IS the reconciliation). The composite is a lawful optic iff the two
  * foci are disjoint; the [[laws.ZipLaws]] set certifies it.
  *
  * @tparam F
  *   shared leg carrier
  * @tparam X1
  *   leg-1 existential
  * @tparam X2
  *   leg-2 existential
  */
trait ZipFunctor[F[_, _], X1, X2]:
  type Out[_, _]

  def zip[S, A, C](
      o1: Optic[S, S, A, A, F] { type X = X1 },
      o2: Optic[S, S, C, C, F] { type X = X2 },
  ): Optic[S, S, (A, C), (A, C), Out]

/** Typeclass instances for [[ZipFunctor]]. */
object ZipFunctor:

  /** `Tuple2` — the fanout of two lenses; total, so `Out = Tuple2` (the result always hits).
    * Threads leg-1's leftover (`X = X1`) and re-reads leg-2 on the intermediate so leg-2's write
    * lands on top of leg-1's. Direct tuple ops (no `ForgetfulFunctor.map`, no closure); the
    * intermediate `S` scalar-replaces under monomorphic leg classes.
    *
    * @group Instances
    */
  given tuple2Zip[X1, X2]: ZipFunctor[Tuple2, X1, X2] with
    type Out[Xz, P] = (Xz, P)

    def zip[S, A, C](
        o1: Optic[S, S, A, A, Tuple2] { type X = X1 },
        o2: Optic[S, S, C, C, Tuple2] { type X = X2 },
    ): Optic[S, S, (A, C), (A, C), Tuple2] =
      new Optic[S, S, (A, C), (A, C), Tuple2]:
        type X = X1
        def to(s: S): (X1, (A, C)) =
          val (x1, a) = o1.to(s)
          val (_, c) = o2.to(s)
          (x1, (a, c))
        def from(xac: (X1, (A, C))): S =
          val (x1, (a, c)) = xac
          val t1 = o1.from((x1, a))
          val (x2, _) = o2.to(t1)
          o2.from((x2, c))

  /** `Affine` — the fanout of two Optionals; partial, so `Out = Affine` (the pair is present iff
    * BOTH legs hit). A hit stores leg-1's Hit context (`Snd[X] = Snd[X1]`) and rebuilds
    * sequentially, re-reading leg-2 on the intermediate; a miss stores the source `s` (`Fst[X] =
    * S`) and returns it unchanged. The `getOption` seam `Some((a, c))` iff both hit follows from
    * the Hit/Miss discrimination in `to`.
    *
    * @group Instances
    */
  given affine2Zip[X1, X2]: ZipFunctor[Affine, X1, X2] with
    type Out[Xz, P] = Affine[Xz, P]

    def zip[S, A, C](
        o1: Optic[S, S, A, A, Affine] { type X = X1 },
        o2: Optic[S, S, C, C, Affine] { type X = X2 },
    ): Optic[S, S, (A, C), (A, C), Affine] =
      new Optic[S, S, (A, C), (A, C), Affine]:
        type X = (S, Snd[X1])
        def to(s: S): Affine[X, (A, C)] =
          o1.to(s) match
            case h1: Hit[X1, A] =>
              o2.to(s) match
                case h2: Hit[X2, C] => new Hit[X, (A, C)](h1.snd, (h1.b, h2.b))
                case _: Miss[X2, C] => new Miss[X, (A, C)](s)
            case _: Miss[X1, A] => new Miss[X, (A, C)](s)
        def from(aff: Affine[X, (A, C)]): S =
          aff match
            case h: Hit[X, (A, C)] =>
              val (a, c) = h.b
              val t1 = o1.from(new Hit[X1, A](h.snd, a))
              o2.to(t1) match
                case h2: Hit[X2, C] => o2.from(new Hit[X2, C](h2.snd, c))
                case _: Miss[X2, C] => t1 // leg-2 absent on the intermediate: leg-1 write only
            case m: Miss[X, (A, C)] => m.fst
