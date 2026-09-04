package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Chronomorphism citizen — the **fused** futu-then-histo refold ([[ReadScheme]]), [[Hylo]] lifted
  * to the universal indices: unfold through the free monad ([[Coattr]]), fold through the cofree
  * comonad ([[Attr]]), no intermediate `S`. Built by [[Futu.cross]] or [[Chrono.apply]]. A
  * nominally-distinct member of the fused-refold family (see [[Hylo]]): honest `X = Nothing`.
  */
final class Chrono[A, B] private[zoo] (private val refold: A => B) extends ReadScheme[A, B]:
  type X = Nothing
  protected def read(a: A): B = refold(a)

object Chrono:

  /** The fused free→cofree refold `A => B`, `Traverse[F]` only. Heads-only `algebra` + all-`Pure`
    * `coalg` degenerate to [[Hylo]]. Stack-safe; retains O(n) `Attr` cells by nature.
    */
  def apply[F[_], A, B](
      coalg: A => F[Coattr[F, A]],
      algebra: F[Attr[F, B]] => B,
  )(using F: Traverse[F]): Chrono[A, B] =
    val build: Coattr[F, A] => Attr[F, B] =
      Machines.foldLayered[F, Coattr[F, A], Attr[F, B]](
        Coattr.expand(coalg),
        Attr.decorate(algebra),
      )
    new Chrono[A, B](a => Attr.forget(build(Coattr.Pure(a))))
