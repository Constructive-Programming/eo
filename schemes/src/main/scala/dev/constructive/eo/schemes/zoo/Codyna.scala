package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** The fused multi-layer-unfold → node-blind-fold refold — the mirror of [[Dyna]], opposite diagonal
  * of the refold quadrant: a free-monad `futu` unfold ([[Coattr]]) whose fold is a plain `cata`, built
  * with no intermediate `S`. `Getter`-shaped over [[dev.constructive.eo.data.Direct]], `.get`. Built by
  * [[Futu.cross]] or [[Codyna.apply]]. Nominally distinct in the fused-refold family (see [[Hylo]]):
  * honest `X = Nothing`. (`Codyna` is a descriptive name — the free-unfold/plain-fold refold has no
  * standard one in the literature.)
  */
final class Codyna[A, B] private[zoo] (private[zoo] val refold: A => B)
    extends Optic[A, Unit, B, Unit, Direct]:
  type X = Nothing
  def to(a: A): Direct[X, B] = Direct(refold(a))
  def from(b: Direct[X, Unit]): Unit = ()

object Codyna:

  /** The fused free→plain refold `A => B`, `Traverse[F]` only. All-`Pure` `coalg` degenerates to
    * [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => F[Coattr[F, A]], alg: F[B] => B)(using
      F: Traverse[F]
  ): Codyna[A, B] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val run = Machines.foldLayered[F, Coattr[F, A], B](expand, (_, fr) => alg(fr))
    new Codyna[A, B](a => run(Coattr.Pure(a)))
