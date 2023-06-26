package eo

trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  def pure[X, A]: A => F[X, A]
