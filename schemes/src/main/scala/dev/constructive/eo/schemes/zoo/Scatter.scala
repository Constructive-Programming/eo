package dev.constructive.eo
package schemes
package zoo

// The Gather/Scatter family is documented in Gather.scala — see that file for
// the full banner comment covering the BiAffine carrier, X-pinning, and the
// design rationale for para/apo having no shipped Scatter/Gather values.

/** Unfold-side decoration optic: scatter (`to`, an affine match) + pointed unit (`from` on Step).
  */
type Scatter[F[_], W, A] =
  optics.Optic[W, W, A, A, data.BiAffine] { type X = (F[W], Unit) }

/** Named unfold-side decoration values. */
object Scatter:
  import data.BiAffine
  import data.BiAffine.{Done, Step}
  import optics.Optic

  private def scatterSideDone(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Scatter.$name: the pointed unit (from) is inhabited on the Step arm only"
    )

  private def mkAna[F[_], A]: Scatter[F, A, A] =
    new Optic[A, A, A, A, BiAffine]:
      type X = (F[A], Unit)
      def to(w: A): BiAffine[X, A] = new Step[X, A]((), w)
      def from(xb: BiAffine[X, A]): A = xb match
        case s: Step[X, A] => s.b
        case _: Done[X, A] => scatterSideDone("ana")

  private val anaAny: AnyRef = mkAna[[x] =>> Any, Any]

  /** The undecorated unfold — every slot is a seed; the unit is the identity. Identity-stable
    * singleton (the generic driver takes the direct route on it).
    */
  def ana[F[_], A]: Scatter[F, A, A] = anaAny.asInstanceOf[Scatter[F, A, A]]

  private def mkFutu[F[_], A]: Scatter[F, Coattr[F, A], A] =
    new Optic[Coattr[F, A], Coattr[F, A], A, A, BiAffine]:
      type X = (F[Coattr[F, A]], Unit)
      def to(w: Coattr[F, A]): BiAffine[X, A] = w match
        case Coattr.Pure(a)     => new Step[X, A]((), a)
        case Coattr.Roll(layer) => new Done[X, A](layer)
      def from(xb: BiAffine[X, A]): Coattr[F, A] = xb match
        case s: Step[X, A] => Coattr.Pure(s.b)
        case _: Done[X, A] => scatterSideDone("futu")

  private val futuAny: AnyRef = mkFutu[[x] =>> Any, Any]

  /** Futumorphism decoration — `Pure(seed)` calls the coalgebra, `Roll(layer)` unrolls the prebuilt
    * layer without a call; the unit is `Coattr.Pure`.
    */
  def futu[F[_], A]: Scatter[F, Coattr[F, A], A] =
    futuAny.asInstanceOf[Scatter[F, Coattr[F, A], A]]
