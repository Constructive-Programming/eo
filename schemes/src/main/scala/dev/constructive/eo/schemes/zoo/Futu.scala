package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Futumorphism citizen — a multi-layer unfold worn as an optic over
  * [[dev.constructive.eo.data.Direct]] with **`X = Coattr[F, A]`**, the free monad `μX. A + F[X]`.
  * The build-side mirror of [[Histo]]. `coalg: A => F[Coattr[F, A]]` answers each slot with
  * [[Coattr.Pure]] (keep unfolding) or [[Coattr.Roll]] (a prebuilt layer, no coalgebra call), so
  * one step may emit several layers. `Review`-shaped (`Optic[Unit, S, Unit, A, Direct]`), consumed
  * via `.reverseGet`; the root seed enters as `Coattr.Pure`. An all-`Pure` coalgebra degenerates to
  * [[Ana]]. Carries `coalg` so [[cross]] can fuse with [[Histo]] (→ [[Chrono]]) or [[Cata]] (→
  * [[Codyna]]). Stack-safe.
  */
final class Futu[F[_], A, S](private[zoo] val coalg: A => F[Coattr[F, A]])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends Optic[Unit, S, Unit, A, Direct]:
  type X = Coattr[F, A]

  private[zoo] val build: A => S =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val run = Machines.foldLayered[F, Coattr[F, A], S](expand, (_, fr) => E.embed(fr))
    a => run(Coattr.Pure(a))

  def to(u: Unit): Direct[X, Unit] = Direct(())
  def from(b: Direct[X, A]): S = build(Direct.value(b))

  /** The fused **chrono** seam: futu ∘ [[Histo]] — [[Hylo]] at the universal indices. The build
    * threads the free monad ([[Coattr]]), the fold the cofree comonad ([[Attr]]); fused, no
    * intermediate `S`.
    */
  def cross[B](histo: Histo[F, S, B]): Chrono[A, B] = Chrono[F, A, B](coalg, histo.alg)

  /** The fused mirror-of-dyna seam: futu ∘ node-blind [[Cata]] (→ [[Codyna]]) — opposite diagonal
    * of the refold quadrant. Fuses; the [[Coattr]] free layers are threaded internally, no
    * intermediate `S`.
    */
  def cross[B](cata: Cata[F, S, B]): Codyna[A, B] = Codyna[F, A, B](coalg, cata.alg)
