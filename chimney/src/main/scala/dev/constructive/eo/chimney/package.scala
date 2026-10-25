package dev.constructive.eo
package chimney

import _root_.io.scalaland.chimney.{
  partial,
  Codec,
  Iso as ChimneyIso,
  PartialTransformer,
  Patcher,
  Transformer,
}
import cats.Applicative

import forgetful.{ForgetfulFunctor, ForgetfulTraverse}
import optics.{BijectionIso, Getter, Iso, Modify, Optic, PickMendPrism, Prism}

// chimney → eo: each chimney value as the eo optic it already is. `Chimney.*` summons / derives
// the chimney value and calls these.

extension [S, T](t: Transformer[S, T])

  /** This transformer as a read: `get(s) = t.transform(s)`. See [[Chimney.getter]]. */
  def toGetter: Getter[S, T] =
    Getter(t.transform)

extension [A, B](i: ChimneyIso[A, B])

  /** This chimney `Iso` as an eo `Iso`: `get = first`, `reverseGet = second`. See [[Chimney.iso]].
    */
  def toIso: BijectionIso[A, A, B, B] =
    Iso[A, A, B, B](i.first.transform, i.second.transform)

extension [Domain, Dto](c: Codec[Domain, Dto])

  /** This codec as a prism from `Dto` to `Domain`: `getOption` decodes, `reverseGet` encodes. See
    * [[Chimney.prism]].
    */
  def toPrism: PickMendPrism[Dto, Domain, Domain] =
    Prism.optional[Dto, Domain](dto => c.decode.transform(dto).asOption, c.encode.transform)

extension [S, T](pt: PartialTransformer[S, T])

  /** This partial transformer as a [[ResultFold]]. `failFast = true` stops at the first error, as
    * chimney's `transformFailFast` does. See [[Chimney.resultFold]].
    */
  def toResultFold(failFast: Boolean = false): ResultFold[S, T] =
    new ResultFold(pt.transform(_, failFast))

extension [A, P](p: Patcher[A, P])

  /** This patcher as a write-only optic: `replace(patch)` patches the `A`. See
    * [[Chimney.patching]].
    */
  def toModify: Modify[A, A, Unit, P] =
    Modify(f => a => p.patch(a, f(())))

// eo → chimney at a focus: a chimney value run where an eo optic points.

extension [S, T, A, F[_, _]](o: Optic[S, T, A, A, F])(using FF: ForgetfulFunctor[F])

  /** Patch the focus: `o.modify(a => patcher.patch(a, p))`. eo finds the part, chimney merges the
    * patch's fields into it — `lens[Order](_.customer).patch(CustomerPatch(email = "…"))(order)`. A
    * miss (Prism / Optional) passes the `S` through untouched; a Traversal patches every focus.
    * chimney derives the `Patcher` when none is in scope.
    */
  inline def patch[P](p: P)(using pa: Patcher[A, P]): S => T =
    o.modify(a => pa.patch(a, p))

extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using FF: ForgetfulFunctor[F])

  /** Change the focus type in place: run chimney's `Transformer[A, B]` at the focus, turning the
    * `S` into a `T` — `Traversal.pEach[List, RawPrice, Price].transformFocus(order.prices)`. Needs
    * a polymorphic optic (`T` differs from `S`): eo-generics' derived lenses are monomorphic, so
    * reach for the core `p*` constructors (`Lens.pLens`, `Prism.pPrism`, `Traversal.pEach`,
    * `Traversal.first`/`second`, …). chimney derives the `Transformer` when none is in scope.
    */
  inline def transformFocus(using t: Transformer[A, B]): S => T =
    o.modify(t.transform)

extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
    FT: ForgetfulTraverse[F, Applicative]
)

  /** [[transformFocus]] for a `PartialTransformer`: the new `T`, or the errors of every focus that
    * failed (all of them — errors accumulate across a Traversal's foci). The error paths are
    * relative to each focus: chimney does not know where the optic pointed.
    */
  inline def transformFocusPartial(using pt: PartialTransformer[A, B]): S => partial.Result[T] =
    o.modifyA(a => pt.transform(a))(using resultApplicative)

extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])

  /** Change the result type: run chimney's `Transformer[T, U]` on whatever the optic writes back,
    * turning its `S => T` side into `S => U` — the outer counterpart of [[transformFocus]]. Every
    * write lands in `U`: `lens[User](_.name).transformImage[UserView].modify(_.trim)(user)` edits a
    * field and hands back the view. A miss (Prism / Optional) still produces a `U` — the untouched
    * `T`, transformed. The focus side (`S`, `A`, `B`) is unchanged, so reads are the original
    * optic's. chimney derives the `Transformer` when none is in scope.
    *
    * Core's `Optic.outerProfunctor.rmap` with a chimney transformer: the result is a generic
    * `Optic` (the concrete class's fused overloads don't survive the remap).
    */
  def transformImage[U](using t: Transformer[T, U]): Optic[S, U, A, B, F] =
    Optic.outerProfunctor[A, B, F].rmap(o)(t.transform)

  /** [[transformImage]] for a `PartialTransformer[T, U]`: every write hands back the `U` or the
    * errors of turning the written `T` into one — "edit the DTO, then validate it into the domain".
    */
  def transformImagePartial[U](using
      pt: PartialTransformer[T, U]
  ): Optic[S, partial.Result[U], A, B, F] =
    Optic.outerProfunctor[A, B, F].rmap(o)(pt.transform(_))

/** Error-accumulating `Applicative[partial.Result]` for [[transformFocusPartial]]. Private — the
  * public cats instance belongs to chimney-cats; this one only drives `modifyA`.
  */
private val resultApplicative: Applicative[partial.Result] = new Applicative[partial.Result]:
  def pure[A](a: A): partial.Result[A] = partial.Result.Value(a)

  def ap[A, B](ff: partial.Result[A => B])(fa: partial.Result[A]): partial.Result[B] =
    partial.Result.map2(ff, fa, (f: A => B, a: A) => f(a), failFast = false)

  override def map[A, B](fa: partial.Result[A])(f: A => B): partial.Result[B] = fa.map(f)
