package dev.constructive.eo
package forgetful

import kyo.Result

import kernel.Monoid

trait ForgetfulFold[F[_, _]]:
  def foldMap[X, A, M: Monoid](f: A => M, fa: F[X, A]): M

object ForgetfulFold:

  given tupleFFold: ForgetfulFold[Tuple2] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: (X, A)): M =
      f(fa._2)

  given resultFFold: ForgetfulFold[Result] with

    def foldMap[X, A, M: Monoid](f: A => M, ea: Result[X, A]): M =
      ea.fold(f, _ => Monoid[M].empty, _ => Monoid[M].empty)

  // `ForgetfulFold[Affine]` is NOT here — it is carrier-owned (`Affine.fold`), matching
  // Direct / ModifyF / MultiFocus / Forget. Only the non-eo carriers (Tuple2, Result) live in this
  // companion, since their own companions can't be extended.
