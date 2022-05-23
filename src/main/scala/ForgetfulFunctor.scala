package eo

import cats.Bifunctor
import cats.arrow.Profunctor

trait ForgetfulFunctor[F[_, _]] {
  def map[X, A, B]: F[X, A] => (A => B) => F[X, B]
}

object ForgetfulFunctor {
  given bifunctorFF[F[_, _]](using B: Bifunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B]: F[X, A] => (A => B) => F[X, B] =
      fa => f => B.bimap[X, A, X, B](fa)(identity, f)

  given profunctorFF[F[_, _]](using B: Profunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B]: F[X, A] => (A => B) => F[X, B] = B.rmap[X, A, B]
}
