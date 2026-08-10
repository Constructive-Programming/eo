package dev.constructive.eo
package zio
package prelude

import _root_.zio.prelude.ZValidation
import cats.{Applicative, Eval, Foldable, Traverse}

import optics.{Optional, Traversal}

/** zio-prelude integration (optional dependency — add `zio-prelude` yourself to use this
  * sub-package; zio-schema already carries it transitively).
  *
  * `ZValidation[W, E, A]` is Either-with-accumulation: a success value, or a `NonEmptyChunk[E]` of
  * every error, both alongside a `W` log. Two optics cover it:
  *
  *   - [[Validations.success]] — an Optional (not a Prism: a lawful `reverseGet` would have to
  *     invent a log, and a prism-shaped write would drop the one already there).
  *   - [[Validations.eachFailure]] — Traversal over every accumulated error, polymorphic in `E`, so
  *     error translation is the same `modify` as everywhere else.
  */
object Validations:

  /** Optional onto the success value — the log rides along, failures pass writes through. */
  def success[W, E, A]: Optional[ZValidation[W, E, A], ZValidation[W, E, A], A, A] =
    Optional[ZValidation[W, E, A], ZValidation[W, E, A], A, A](
      {
        case ZValidation.Success(_, a) => Right(a)
        case f                         => Left(f)
      },
      (s, a) =>
        s match
          case ZValidation.Success(w, _) => ZValidation.Success(w, a)
          case f                         => f,
    )

  /** Traversal over every accumulated error — zero foci on a success. Polymorphic in the error:
    * `eachFailure.modify(toDomainError)` is `mapError` with optic composition.
    */
  def eachFailure[W, E, E2, A]: Traversal[ZValidation[W, E, A], ZValidation[W, E2, A], E, E2] =
    Traversal.pEach[[e] =>> ZValidation[W, e, A], E, E2](using failureTraverse)

  private def failureTraverse[W, A]: Traverse[[e] =>> ZValidation[W, e, A]] =
    new Traverse[[e] =>> ZValidation[W, e, A]]:
      def traverse[G[_]: Applicative, E, E2](
          fa: ZValidation[W, E, A]
      )(f: E => G[E2]): G[ZValidation[W, E2, A]] =
        fa match
          case ZValidation.Success(w, a)  => Applicative[G].pure(ZValidation.Success(w, a))
          case ZValidation.Failure(w, es) =>
            // One source of truth for the NonEmptyChunk fold — see Chunks.nonEmptyChunkTraverse.
            Applicative[G].map(Chunks.nonEmptyChunkTraverse.traverse(es)(f))(
              ZValidation.Failure(w, _)
            )
      override def map[E, E2](fa: ZValidation[W, E, A])(f: E => E2): ZValidation[W, E2, A] =
        fa match
          case ZValidation.Success(w, a)  => ZValidation.Success(w, a)
          case ZValidation.Failure(w, es) => ZValidation.Failure(w, es.map(f))
      def foldLeft[E, B](fa: ZValidation[W, E, A], b: B)(f: (B, E) => B): B =
        fa match
          case ZValidation.Success(_, _)  => b
          case ZValidation.Failure(_, es) => es.foldLeft(b)(f)
      def foldRight[E, B](fa: ZValidation[W, E, A], lb: Eval[B])(
          f: (E, Eval[B]) => Eval[B]
      ): Eval[B] =
        fa match
          case ZValidation.Success(_, _)  => lb
          case ZValidation.Failure(_, es) => Foldable.iterateRight(es.toChunk, lb)(f)
