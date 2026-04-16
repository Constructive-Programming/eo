package eo

import cats.Bifunctor
import cats.arrow.Profunctor

trait ForgetfulFunctor[F[_, _]]:
  def map[X, A, B]: F[X, A] => (A => B) => F[X, B]


object ForgetfulFunctor:
  /** Hand-written Tuple2 instance.
    *
    * Bypasses cats `Bifunctor[Tuple2]` so the hot Lens path
    * (`Optic.modify` / `Optic.replace`) avoids the extra closure
    * `bimap(_)(identity, f)` allocates. Being more specific than
    * `bifunctorFF[F]` (no `using Bifunctor[F]` constraint), this
    * given out-ranks the generic derivation in Scala 3 implicit
    * search (concrete type beats constrained type parameter).
    *
    * Do not define competing `ForgetfulFunctor[Tuple2]` instances
    * downstream — doing so could cause ambiguity and silently
    * regress Lens performance back to the Bifunctor path.
    */
  given directTuple: ForgetfulFunctor[Tuple2] with
    def map[X, A, B]: ((X, A)) => (A => B) => (X, B) =
      xa => f => (xa._1, f(xa._2))

  given bifunctorFF[F[_, _]](using B: Bifunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B]: F[X, A] => (A => B) => F[X, B] =
      fa => f => B.bimap[X, A, X, B](fa)(identity, f)

  given profunctorFF[F[_, _]](using B: Profunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B]: F[X, A] => (A => B) => F[X, B] = B.rmap[X, A, B]
