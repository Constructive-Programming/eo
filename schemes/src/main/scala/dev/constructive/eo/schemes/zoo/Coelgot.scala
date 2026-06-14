package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Co-Elgot citizen — a [[Hylo]] whose **fold may read the seed** ([[ReadScheme]]):
  * `coalg: A => F[A]` unfolds, `alg: (A, F[B]) => B` folds with the originating seed in hand (the
  * build-side analogue of [[Para]]'s subterm retention, on the fused refold). Built by
  * [[Coelgot.apply]]. Nominally distinct in the fused-refold family (see [[Hylo]]): honest
  * `X = Nothing`.
  */
final class Coelgot[A, B] private[zoo] (private val refold: A => B) extends ReadScheme[A, B]:
  type X = Nothing
  protected def read(a: A): B = refold(a)

object Coelgot:

  /** The seed-reading refold `A => B`, `Traverse[F]` only. Ignoring the seed argument degenerates
    * to [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => F[A], alg: (A, F[B]) => B)(using
      Traverse[F]
  ): Coelgot[A, B] =
    new Coelgot[A, B](Machines.foldLayered[F, A, B](coalg, (a, fr) => alg(a, fr)))
