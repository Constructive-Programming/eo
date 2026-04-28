package dev.constructive.eo

/** Adds `pure` to [[ForgetfulFunctor]]. Needed by `Optic.put`, which builds the carrier from
  * scratch ignoring the source. The `Forget[F]` instance lives in [[data.Forget]].
  *
  * @tparam F
  *   the carrier
  */
trait ForgetfulApplicative[F[_, _]] extends ForgetfulFunctor[F]:
  def pure[X, A](a: A): F[X, A]

/** Companion stub — no instances live here. */
object ForgetfulApplicative
