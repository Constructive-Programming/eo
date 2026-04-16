package eo

import data.Forget

import cats.Applicative

trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  def pure[X, A]: A => F[X, A]

object ForgetfulApplicative:
    given forgetFApplicative[F[_]: Applicative]: ForgetfulApplicative[Forget[F]] with
      def map[X, A, B]: F[A] => (A => B) => F[B] = Applicative[F].map
      def pure[X, A]: A => F[A] = Applicative[F].pure
