package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Co-Elgot citizen — a [[Hylo]] whose **fold may read the seed**: `coalg: A => F[A]` unfolds, `alg:
  * (A, F[B]) => B` folds with the originating seed in hand (the build-side analogue of [[Para]]'s
  * subterm retention, on the fused refold). Fused `A => B`, `Traverse[F]` only. `Getter`-shaped over
  * [[dev.constructive.eo.data.Direct]], `.get`. Built by [[Coelgot.apply]]. Nominally distinct in the
  * fused-refold family (see [[Hylo]]): honest `X = Nothing`.
  */
final class Coelgot[A, B] private[zoo] (private[zoo] val refold: A => B)
    extends Optic[A, Unit, B, Unit, Direct]:
  type X = Nothing
  def to(a: A): Direct[X, B] = Direct(refold(a))
  def from(b: Direct[X, Unit]): Unit = ()

object Coelgot:

  /** The seed-reading refold `A => B`, `Traverse[F]` only. Ignoring the seed argument degenerates to
    * [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => F[A], alg: (A, F[B]) => B)(using
      Traverse[F]
  ): Coelgot[A, B] =
    new Coelgot[A, B](Machines.foldLayered[F, A, B](coalg, (a, fr) => alg(a, fr)))
