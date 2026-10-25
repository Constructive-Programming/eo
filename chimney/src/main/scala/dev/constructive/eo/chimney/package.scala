package dev.constructive.eo
package chimney

import scala.annotation.unused

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

  /** Change the type you write at the focus: the new optic accepts a `C` and runs chimney's
    * `Transformer[C, B]` to turn it into the `B` this optic writes back. Reads, the source and the
    * result are unchanged — `Optic[S, T, A, C]` — so the focus reads as an `A` and is written as a
    * `C`: `lens[User](_.address).transformFocus[AddressView].replace(view)(user)` writes a view
    * where the lens expects an `Address`. Works on monomorphic optics (eo-generics' lenses); on a
    * polymorphic one (`Traversal.pEach[List, RawPrice, Price]`), `transformFocus[RawPrice]` lets an
    * edit of the `RawPrice` it reads be written back as a `Price` (`.modify(r => RawPrice(r.cents *
    * 2))` reprices and retypes in one write). The write-side counterpart of [[transformImage]],
    * which changes `T`; together they give `Optic[S, U, A, C]`. chimney derives the `Transformer`
    * when none is in scope.
    *
    * A generic `Optic`, like `Optic.innerProfunctor.lmap` — but `to` is the original optic's own,
    * so reads don't pay a carrier `map` they never use; only `from` maps the written `C`s.
    */
  def transformFocus[C](using t: Transformer[C, B]): Optic[S, T, A, C, F] =
    new Optic[S, T, A, C, F]:
      type X = o.X
      def to(s: S): F[X, A] = o.to(s)
      def from(fc: F[X, C]): T = o.from(FF.map(fc, t.transform))

extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
    FF: ForgetfulFunctor[F],
    @unused FT: ForgetfulTraverse[F, Applicative],
)

  /** [[transformFocus]] for a `PartialTransformer[C, B]`: write a `C`, and get back the new `T` or
    * the errors of every written `C` that didn't convert (all of them — errors accumulate across a
    * Traversal's foci) — `Optic[S, partial.Result[T], A, C]`. The error paths are relative to each
    * focus: chimney does not know where the optic pointed.
    *
    * One pass, no per-focus `partial.Result` plumbing: the carrier's `map` converts every written
    * `C` to `B | Null` — a failing one records its errors on the side and leaves `null` — and only
    * if none failed does this optic's `from` rebuild the `T`, at which point every slot holds a
    * real `B`. On any failure the mapped carrier is dropped and the errors returned: the underlying
    * optic's write path never sees a `null`. The `ForgetfulTraverse` gate (otherwise unused) keeps
    * this to carriers whose `map` runs eagerly, so every error is in before `from` is decided.
    */
  def transformFocusPartial[C](using
      pt: PartialTransformer[C, B]
  ): Optic[S, partial.Result[T], A, C, F] =
    new Optic[S, partial.Result[T], A, C, F]:
      type X = o.X
      def to(s: S): F[X, A] = o.to(s)

      def from(fc: F[X, C]): partial.Result[T] =
        var errors: partial.Result.Errors | Null = null
        val mapped: F[X, B | Null] = FF.map(
          fc,
          c =>
            pt.transform(c) match
              case partial.Result.Value(b)  => b
              case e: partial.Result.Errors =>
                errors = errors match
                  case null                        => e
                  case prev: partial.Result.Errors => partial.Result.Errors.merge(prev, e)
                null,
        )
        errors match
          // No write failed, so every `B | Null` slot holds a `B`: the one place the union narrows.
          case null => partial.Result.Value(o.from(mapped.asInstanceOf[F[X, B]]))
          case e: partial.Result.Errors => e

extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])

  /** Change the result type: run chimney's `Transformer[T, U]` on whatever the optic writes back,
    * turning its `S => T` side into `S => U` — the result-side counterpart of [[transformFocus]].
    * Every write lands in `U`: `lens[User](_.name).transformImage[UserView].modify(_.trim)(user)`
    * edits a field and hands back the view. A miss (Prism / Optional) still produces a `U` — the
    * untouched `T`, transformed. The focus side (`S`, `A`, `B`) is unchanged, so reads are the
    * original optic's. chimney derives the `Transformer` when none is in scope.
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
