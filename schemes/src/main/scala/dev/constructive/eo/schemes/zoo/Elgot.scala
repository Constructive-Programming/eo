package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Elgot citizen — a [[Hylo]] whose **unfold may short-circuit** ([[ReadScheme]]): `coalg: A =>
  * Either[B, F[A]]` answers `Left(b)` (the seed resolves directly, stop) or `Right(layer)` (keep
  * unfolding); `alg: F[B] => B` folds the rest. Driven by the short-circuit-aware
  * [[Machines.foldLayeredOr]]. Built by [[Elgot.apply]]. Nominally distinct in the fused-refold
  * family (see [[Hylo]]): honest `X = Nothing`.
  */
final class Elgot[A, B] private[zoo] (private val refold: A => B) extends ReadScheme[A, B]:
  type X = Nothing
  protected def read(a: A): B = refold(a)

object Elgot:

  /** The short-circuit refold `A => B`, `Traverse[F]` only. An all-`Right` `coalg` degenerates to
    * [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => Either[B, F[A]], alg: F[B] => B)(using
      Traverse[F]
  ): Elgot[A, B] =
    new Elgot[A, B](Machines.foldLayeredOr[F, A, B](coalg, fr => alg(fr)))
