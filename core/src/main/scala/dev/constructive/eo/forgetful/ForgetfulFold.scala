package dev.constructive.eo
package forgetful

import cats.Monoid

/** `foldMap` over the focus of `F[_, _]`. Miss / absent contributes `Monoid.empty`; hit runs `f`
  * and folds.
  *
  * @tparam F
  *   the carrier
  */
trait ForgetfulFold[F[_, _]]:
  def foldMap[X, A, M: Monoid](f: A => M, fa: F[X, A]): M

/** Typeclass instances for [[ForgetfulFold]]. */
object ForgetfulFold:

  /** `Tuple2` — runs `f` on the focus, ignores the leftover. @group Instances */
  given tupleFFold: ForgetfulFold[Tuple2] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: (X, A)): M =
      f(fa._2)

  /** `Either` — `Left` is `Monoid.empty`, `Right` runs `f`. @group Instances */
  given eitherFFold: ForgetfulFold[Either] with

    def foldMap[X, A, M: Monoid](f: A => M, ea: Either[X, A]): M =
      ea.fold(_ => Monoid[M].empty, f)

  // `ForgetfulFold[Affine]` is NOT here — it is carrier-owned (`Affine.fold`), matching
  // Direct / ModifyF / MultiFocus / Forget. Only the stdlib carriers (Tuple2, Either) live in this
  // companion, since their own companions can't be extended.
