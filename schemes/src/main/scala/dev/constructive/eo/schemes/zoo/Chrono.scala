package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Chronomorphism citizen — the **fused** futu-then-histo refold, [[Hylo]] lifted to the universal
  * indices: unfold through the free monad ([[Coattr]]), fold through the cofree comonad ([[Attr]]),
  * building no intermediate `S`. `Getter`-shaped over [[dev.constructive.eo.data.Direct]], consumed
  * via `.get`. Built by [[Futu.cross]] or [[Chrono.apply]].
  *
  * A nominally-distinct member of the fused-refold family (see [[Hylo]]): honest `X = Nothing`, since
  * fusion discards the `Coattr`/`Attr` it threads internally.
  */
final class Chrono[A, B] private[zoo] (private[zoo] val refold: A => B)
    extends Optic[A, Unit, B, Unit, Direct]:
  type X = Nothing
  def to(a: A): Direct[X, B] = Direct(refold(a))
  def from(b: Direct[X, Unit]): Unit = ()

object Chrono:

  /** The fused free→cofree refold `A => B`, needing only `Traverse[F]`. Heads-only `algebra` +
    * all-`Pure` `coalg` degenerate to [[Hylo]]. Stack-safe; retains O(n) `Attr` cells by nature.
    */
  def apply[F[_], A, B](
      coalg: A => F[Coattr[F, A]],
      algebra: F[Attr[F, B]] => B,
  )(using F: Traverse[F]): Chrono[A, B] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val build: Coattr[F, A] => Attr[F, B] =
      Machines.foldLayered[F, Coattr[F, A], Attr[F, B]](
        expand,
        (_, layer) => Attr(algebra(layer), layer),
      )
    new Chrono[A, B](a => Attr.forget(build(Coattr.Pure(a))))
