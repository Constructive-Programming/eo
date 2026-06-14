package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Dynamorphism citizen — the **fused** plain-unfold → course-of-value-fold refold, the
  * refold-quadrant diagonal between [[Hylo]] (plain→plain) and [[Chrono]] (free→cofree). A plain
  * `ana` unfold whose fold sees each node's full decorated history ([[Attr]], the cofree memo), built
  * with no intermediate `S`. `Getter`-shaped over [[dev.constructive.eo.data.Direct]], `.get`. Built
  * by [[Ana.cross]] or [[Dyna.apply]]. Nominally distinct in the fused-refold family (see [[Hylo]]):
  * honest `X = Nothing`.
  */
final class Dyna[A, B] private[zoo] (private[zoo] val refold: A => B)
    extends Optic[A, Unit, B, Unit, Direct]:
  type X = Nothing
  def to(a: A): Direct[X, B] = Direct(refold(a))
  def from(b: Direct[X, Unit]): Unit = ()

object Dyna:

  /** The fused plain→cofree refold `A => B`, `Traverse[F]` only. Heads-only `alg` degenerates to
    * [[Hylo]]. Stack-safe; retains O(n) `Attr` cells.
    */
  def apply[F[_], A, B](coalg: A => F[A], alg: F[Attr[F, B]] => B)(using
      F: Traverse[F]
  ): Dyna[A, B] =
    val build: A => Attr[F, B] =
      Machines.foldLayered[F, A, Attr[F, B]](coalg, (_, layer) => Attr(alg(layer), layer))
    new Dyna[A, B](a => Attr.forget(build(a)))
