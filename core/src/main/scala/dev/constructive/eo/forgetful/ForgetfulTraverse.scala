package dev.constructive.eo
package forgetful

import kyo.Result

import kernel.{Applicative, Functor}

trait ForgetfulTraverse[F[_, _], C[_[_]]]:
  def traverse[X, A, B, G[_]: C](fa: F[X, A], f: A => G[B]): G[F[X, B]]

object ForgetfulTraverse:

  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with

    def traverse[X, A, B, G[_]: Functor](fa: (X, A), f: A => G[B]): G[(X, B)] =
      Functor[G].map(f(fa._2))(fa._1 -> _)

  given tupleFTraverseApplicative: ForgetfulTraverse[Tuple2, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: (X, A), f: A => G[B]): G[(X, B)] =
      Applicative[G].map(f(fa._2))(fa._1 -> _)

  given resultFTraverse: ForgetfulTraverse[Result, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: Result[X, A], f: A => G[B]): G[Result[X, B]] =
      fa.foldError(
        a => Applicative[G].map(f(a))(b => Result.succeed(b)),
        err => Applicative[G].pure(err),
      )

  // `ForgetfulTraverse[Affine]` is NOT here — it is carrier-owned (`Affine.traverse`), matching
  // Direct / ModifyF / MultiFocus / Forget. Only the non-eo carriers (Tuple2, Result) live in this
  // companion, since their own companions can't be extended.
