package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Dynamorphism citizen — the **fused** plain-unfold → course-of-value-fold refold
  * ([[ReadScheme]]), the refold-quadrant diagonal between [[Hylo]] (plain→plain) and [[Chrono]]
  * (free→cofree): a plain `ana` unfold whose fold sees each node's decorated history ([[Attr]]), no
  * intermediate `S`. Built by [[Ana.cross]] or [[Dyna.apply]]. Nominally distinct in the
  * fused-refold family (see [[Hylo]]): honest `X = Nothing`.
  */
final class Dyna[A, B] private[zoo] (private val refold: A => B) extends ReadScheme[A, B]:
  type X = Nothing
  protected def read(a: A): B = refold(a)

object Dyna:

  /** The fused plain→cofree refold `A => B`, `Traverse[F]` only. Heads-only `alg` degenerates to
    * [[Hylo]]. Stack-safe; retains O(n) `Attr` cells.
    */
  def apply[F[_], A, B](coalg: A => F[A], alg: F[Attr[F, B]] => B)(using
      F: Traverse[F]
  ): Dyna[A, B] =
    val build: A => Attr[F, B] = Machines.foldLayered[F, A, Attr[F, B]](coalg, Attr.decorate(alg))
    new Dyna[A, B](a => Attr.forget(build(a)))
