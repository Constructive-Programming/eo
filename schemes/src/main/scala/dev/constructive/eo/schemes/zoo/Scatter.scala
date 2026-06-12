package dev.constructive.eo
package schemes
package zoo

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic

// The Gather/Scatter family is documented in Gather.scala — see that file for
// the full banner comment covering the BiAffine carrier, X-pinning, and the
// design rationale for para/apo having no shipped Scatter/Gather values.

/** Unfold-side decoration optic: a full citizen. Extend and implement [[scatter]] (the affine
  * match: `Right(seed)` keeps unfolding, `Left(layer)` is prebuilt — no coalgebra call) and
  * [[unit]] (the pointed seed injection — gana's `pure`); the `BiAffine` optic surface (`to` =
  * scatter, `from` on `Step` = unit) is provided here.
  */
abstract class Scatter[F[_], W, A] extends Optic[W, W, A, A, BiAffine]:
  type X = (F[W], Unit)

  /** The scatter: `Right(seed)` → call the coalgebra; `Left(layer)` → unroll as-is. */
  def scatter(w: W): Either[F[W], A]

  /** The pointed unit: inject a seed into the decoration (`Scatter.ana` = identity, `Scatter.futu` =
    * `Coattr.Pure`).
    */
  def unit(a: A): W

  final def to(w: W): BiAffine[X, A] = scatter(w) match
    case Right(a)    => new Step[X, A]((), a)
    case Left(layer) => new Done[X, A](layer)

  final def from(xb: BiAffine[X, A]): W = xb match
    case s: Step[X, A] => unit(s.b)
    case _: Done[X, A] =>
      throw new UnsupportedOperationException(
        "Scatter: the pointed unit (from) is inhabited on the Step arm only"
      )

/** Named unfold-side decoration values. */
object Scatter:

  /** The undecorated unfold — every slot is a seed; the unit is the identity (`W = A`). With it,
    * the generic `Schemes.ana(scatter)(gcoalg)` agrees with the direct `Schemes.ana(gcoalg)`
    * (law-pinned); the direct overload IS the fast path.
    */
  final class Id[F[_], A] extends Scatter[F, A, A]:
    def scatter(w: A): Either[F[A], A] = Right(w)
    def unit(a: A): A = a

  /** @see [[Id]] */
  def ana[F[_], A]: Scatter[F, A, A] = new Id[F, A]

  /** Futumorphism decoration — `Pure(seed)` calls the coalgebra, `Roll(layer)` unrolls the prebuilt
    * layer without a call; the unit is `Coattr.Pure`.
    */
  final class Futu[F[_], A] extends Scatter[F, Coattr[F, A], A]:

    def scatter(w: Coattr[F, A]): Either[F[Coattr[F, A]], A] = w match
      case Coattr.Pure(a)     => Right(a)
      case Coattr.Roll(layer) => Left(layer)

    def unit(a: A): Coattr[F, A] = Coattr.Pure(a)

  /** @see [[Futu]] */
  def futu[F[_], A]: Scatter[F, Coattr[F, A], A] = new Futu[F, A]
