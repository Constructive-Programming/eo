package eo

import data.Forget

import cats.Applicative

trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  def pure[X, A](a: A): F[X, A]

object ForgetfulApplicative:
    given forgetFApplicative[F[_]: Applicative]: ForgetfulApplicative[Forget[F]] with
      def map[X, A, B](fa: F[A], f: A => B): F[B] = Applicative[F].map(fa)(f)
      def pure[X, A](a: A): F[A] = Applicative[F].pure(a)
