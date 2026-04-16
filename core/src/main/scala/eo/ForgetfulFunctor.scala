package eo

import cats.{Bifunctor, Functor}
import cats.arrow.Profunctor
import data.Forget

/** Functor over the second type parameter of a bifunctor-like `F[_, _]`.
  *
  * The primary method `map` is uncurried for allocation-free hot paths.
  * If you need a curried variant, derive it at the call site:
  * `fa => f => FF.map(fa, f)`.
  */
trait ForgetfulFunctor[F[_, _]]:
  def map[X, A, B](fa: F[X, A], f: A => B): F[X, B]


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
    def map[X, A, B](fa: (X, A), f: A => B): (X, B) =
      (fa._1, f(fa._2))

  given directEither: ForgetfulFunctor[Either] with
    def map[X, A, B](fa: Either[X, A], f: A => B): Either[X, B] =
      fa.map(f)

  /** Direct instance for `Forget[T]` (i.e. `T[A]`).
    *
    * Bypasses the `bifunctorFF → Bifunctor[Forget[T]] → Functor[T]`
    * chain, calling `Functor[T].map` directly. Benefits Traversal.modify.
    */
  given directForget[T[_]](using F: Functor[T]): ForgetfulFunctor[Forget[T]] with
    def map[X, A, B](fa: T[A], f: A => B): T[B] = F.map(fa)(f)

  given bifunctorFF[F[_, _]](using B: Bifunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B](fa: F[X, A], f: A => B): F[X, B] =
      B.bimap[X, A, X, B](fa)(identity, f)

  given profunctorFF[F[_, _]](using B: Profunctor[F]): ForgetfulFunctor[F] with
    def map[X, A, B](fa: F[X, A], f: A => B): F[X, B] = B.rmap(fa)(f)
