package dev.constructive.eo

import cats.Monoid

import data.Affine

/** `foldMap` over the focus of `F[_, _]`. Miss / absent contributes `Monoid.empty`; hit runs `f`
  * and folds.
  *
  * @tparam F
  *   the carrier
  */
trait ForgetfulFold[F[_, _]]:
  def foldMap[X, A, M: Monoid]: (A => M) => F[X, A] => M

/** Typeclass instances for [[ForgetfulFold]]. */
object ForgetfulFold:

  /** `Tuple2` — runs `f` on the focus, ignores the leftover. @group Instances */
  given tupleFFold: ForgetfulFold[Tuple2] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, A)) => M =
      f => fa => f(fa._2)

  /** `Either` — `Left` is `Monoid.empty`, `Right` runs `f`. @group Instances */
  given eitherFFold: ForgetfulFold[Either] with

    def foldMap[X, A, M: Monoid]: (A => M) => Either[X, A] => M =
      f => ea => ea.fold(_ => Monoid[M].empty, f)

  /** `Affine` — miss empty, hit runs `f` on the focus. @group Instances */
  given affineFFold: ForgetfulFold[Affine] with

    def foldMap[X, A, M: Monoid]: (A => M) => Affine[X, A] => M =
      f =>
        fa =>
          fa match
            case _: Affine.Miss[X, A] => Monoid[M].empty
            case h: Affine.Hit[X, A]  => f(h.b)
