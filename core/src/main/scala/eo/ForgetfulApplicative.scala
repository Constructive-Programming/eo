package eo

/** Adds `pure` to [[ForgetfulFunctor]] — construct an `F[X, A]` from an `A` alone. Needed by
  * `Optic.put`, which ignores the source `S` and builds the carrier from scratch.
  *
  * Instance for `Forget[F]` lives in [[data.Forget]] with the rest of that carrier's ladder.
  *
  * @tparam F
  *   the carrier
  */
trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  /** Lift an `A` into the carrier with whatever leftover the `Applicative` shape defines.
    */
  def pure[X, A](a: A): F[X, A]

/** Typeclass instances for [[ForgetfulApplicative]] — none currently outside the `Forget[F]` /
  * `Forgetful` carriers; kept as a companion stub in case downstream carriers grow into it.
  */
object ForgetfulApplicative
