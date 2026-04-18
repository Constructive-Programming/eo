package eo

import data.Forget

import cats.Applicative

/** Adds `pure` to [[ForgetfulFunctor]] — construct an `F[X, A]`
  * from an `A` alone. Needed by `Optic.put`, which ignores the
  * source `S` and builds the carrier from scratch.
  *
  * @tparam F the carrier
  */
trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  /** Lift an `A` into the carrier with whatever leftover the
    * `Applicative` shape defines. */
  def pure[X, A](a: A): F[X, A]

/** Typeclass instances for [[ForgetfulApplicative]]. */
object ForgetfulApplicative:

  /** `ForgetfulApplicative[Forget[F]]` via any `Applicative[F]`.
    *
    * @group Instances */
  given forgetFApplicative[F[_]: Applicative]: ForgetfulApplicative[Forget[F]] with
    def map[X, A, B](fa: F[A], f: A => B): F[B] = Applicative[F].map(fa)(f)
    def pure[X, A](a: A): F[A] = Applicative[F].pure(a)
