package dev.constructive.eo
package optics

import kyo.{Maybe, Result}

import kernel.Monoid
import data.Affine

object Optional:

  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Result[T, A],
      reverseGet: ((S, B)) => T,
  ): Optional[S, T, A, B] =
    new Optional[S, T, A, B](getOrModify, (s, b) => reverseGet((s, b)))

  def readOnly[S, A](matches: S => Maybe[A]): AffineFold[S, A] =
    AffineFold(matches)

  def selectReadOnly[A](p: A => Boolean): AffineFold[A, A] =
    AffineFold.select(p)

final class Optional[S, T, A, B](
    val getOrModify: S => Result[T, A],
    val reverseGet: (S, B) => T,
) extends Optic[S, T, A, B, Affine],
      CanGetOption[S, A],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = (T, S)

  def to(s: S): Affine[X, A] =
    getOrModify(s).foldOrThrow(
      a => new Affine.Hit[X, A](s, a),
      t => new Affine.Miss[X, A](t),
    )

  def from(a: Affine[X, B]): T =
    a match
      case h: Affine.Hit[X, B]  => reverseGet(h.snd, h.b)
      case m: Affine.Miss[X, B] => m.fst

  def getOption(s: S): Maybe[A] = getOrModify(s).toMaybe

  def modify(f: A => B): S => T =
    s => getOrModify(s).foldOrThrow(a => reverseGet(s, f(a)), t => t)

  def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    getOrModify(s).foldOrThrow(f, _ => M.empty)

  private inline def fuseToOptional[C, D](
      innerHit: (S, A) => Result[T, C],
      innerWrite: (A, D) => B,
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s => getOrModify(s).foldError(a => innerHit(s, a), err => err),
      reverseGet = (s, d) =>
        getOrModify(s).foldOrThrow(a => reverseGet(s, innerWrite(a, d)), t => t),
    )

  inline def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) => inner.getOrModify(a).mapFailure(b => reverseGet(s, b)),
      innerWrite = (a, d) => inner.reverseGet(a, d),
    )

  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Result.succeed(inner.get(a)),
      innerWrite = (a, d) => inner.enplace(a, d),
    )

  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) => inner.tear(a).mapFailure(b => reverseGet(s, b)),
      innerWrite = (_, d) => inner.mend(d),
    )

  def andThen[C, D](inner: BijectionIso[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Result.succeed(inner.get(a)),
      innerWrite = (_, d) => inner.reverseGet(d),
    )
