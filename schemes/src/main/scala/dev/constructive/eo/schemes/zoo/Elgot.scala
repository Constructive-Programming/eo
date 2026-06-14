package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Elgot citizen — a [[Hylo]] whose **unfold may short-circuit**: `coalg: A => Either[B, F[A]]`
  * answers `Left(b)` (the seed resolves directly, stop) or `Right(layer)` (keep unfolding); `alg:
  * F[B] => B` folds the rest. Fused refold `A => B` (no intermediate `S`), driven by the
  * short-circuit-aware [[Machines.foldLayeredOr]]. `Getter`-shaped over
  * [[dev.constructive.eo.data.Direct]], `.get`. Built by [[Elgot.apply]]. Nominally distinct in the
  * fused-refold family (see [[Hylo]]): honest `X = Nothing`.
  */
final class Elgot[A, B] private[zoo] (private[zoo] val refold: A => B)
    extends Optic[A, Unit, B, Unit, Direct]:
  type X = Nothing
  def to(a: A): Direct[X, B] = Direct(refold(a))
  def from(b: Direct[X, Unit]): Unit = ()

object Elgot:

  /** The short-circuit refold `A => B`, `Traverse[F]` only. An all-`Right` `coalg` degenerates to
    * [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => Either[B, F[A]], alg: F[B] => B)(using
      Traverse[F]
  ): Elgot[A, B] =
    new Elgot[A, B](Machines.foldLayeredOr[F, A, B](coalg, fr => alg(fr)))
