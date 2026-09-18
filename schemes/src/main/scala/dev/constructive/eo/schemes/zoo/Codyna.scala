package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** The **fused** multi-layer-unfold → node-blind-fold refold ([[ReadScheme]]) — the mirror of
  * [[Dyna]], opposite diagonal of the refold quadrant: a free-monad `futu` unfold ([[Coattr]])
  * whose fold is a plain `cata`, no intermediate `S`. Built by [[Futu.cross]] or [[Codyna.apply]].
  * Nominally distinct in the fused-refold family (see [[Hylo]]): honest `X = Nothing`. (`Codyna` is
  * a descriptive name — the free-unfold/plain-fold refold has no standard one in the literature.)
  */
final class Codyna[A, B] private[zoo] (private val refold: A => B) extends ReadScheme[A, B]:
  type X = Nothing
  protected def read(a: A): B = refold(a)

object Codyna:

  /** The fused free→plain refold `A => B`, `Traverse[F]` only. All-`Pure` `coalg` degenerates to
    * [[Hylo]]. Stack-safe.
    */
  def apply[F[_], A, B](coalg: A => F[Coattr[F, A]], alg: F[B] => B)(using
      F: Traverse[F]
  ): Codyna[A, B] =
    val run = Machines.foldLayered[F, Coattr[F, A], B](Coattr.expand(coalg), (_, fr) => alg(fr))
    new Codyna[A, B](a => run(Coattr.Pure(a)))
