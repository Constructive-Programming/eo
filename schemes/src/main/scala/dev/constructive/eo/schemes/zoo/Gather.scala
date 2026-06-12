package dev.constructive.eo
package schemes
package zoo

// ===========================================================================================
// Gather / Scatter — the decoration optics, over the BiAffine carrier.
//
// A generalized scheme's decoration is an optic whose existential leftover is one F-layer
// (the names echo droste's Gather/Scatter and recursion-schemes' distCata/distHisto/...).
// Sides are pinned in the TYPE, eo-style (read-only/build-only citizens):
//
//   - Fold side (Gather): build-only. `from` is the GATHER — it consumes
//     `Step(layer: F[W], result: A)` and produces the decoration `W` (histo's gather is
//     literally the `Attr` constructor). The read side is vestigial (throws, the
//     `Unfold.algebra` precedent); `Done` never occurs on this side.
//
//   - Unfold side (Scatter): a full citizen. `to` is the SCATTER — an affine match
//     answering each slot with `Step(_, seed)` (call the coalgebra) or `Done(layer: F[W])`
//     (a prebuilt layer; unroll it, no coalgebra call). `from` on the Step arm is the
//     POINTED unit — the seed injection `A => W` (gana's `pure`: ana = identity, futu =
//     `Coattr.Pure`), giving the unit law `to(from(Step((), a))) == Step((), a)`.
//
// X pinning per shape: gather side X = (Unit, F[W]) (Snd = the one-F-layer context); scatter
// side X = (F[W], Unit) (Fst = the prebuilt-layer Done payload). The generic drivers in
// [[Schemes]] are fully typed against these refinements — no casts at the seam.
//
// para and apo have NO generic values here: their generic routes are deliberately inferior
// (para's gather would re-embed each subterm — droste's Gather.para; apo's scatter would
// re-walk grafts through Project — distApo, O(graft)), and the native `Schemes.para` /
// `Schemes.apo` engines subsume them. Their decoration semantics survive as law fixtures in
// the test suite (GatherScatterLawsSpec), pinning the native routes to the definitions.
// ===========================================================================================

/** Fold-side decoration optic: gather-only (build-only member). `from` = gather. */
type Gather[F[_], W, A] =
  optics.Optic[Unit, W, Unit, A, data.BiAffine] { type X = (Unit, F[W]) }

/** Named fold-side decoration values. */
object Gather:
  import data.BiAffine
  import data.BiAffine.{Done, Step}
  import optics.Optic

  private def vestigialRead(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Gather.$name is gather-only (build-only): its read side is vestigial by specification"
    )

  private def foldSideDone(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Gather.$name is a fold-side decoration: Done never occurs on the gather seam"
    )

  private def mkCata[F[_], A]: Gather[F, A, A] =
    new Optic[Unit, A, Unit, A, BiAffine]:
      type X = (Unit, F[A])
      def to(u: Unit): BiAffine[X, Unit] = vestigialRead("cata")
      def from(xb: BiAffine[X, A]): A = xb match
        case s: Step[X, A] => s.b
        case _: Done[X, A] => foldSideDone("cata")

  private val cataAny: AnyRef = mkCata[[x] =>> Any, Any]

  /** The undecorated fold — gather keeps the result, discards the layer. Identity-stable singleton:
    * the generic driver recognises it and takes the direct (decoration-free) route.
    */
  def cata[F[_], A]: Gather[F, A, A] = cataAny.asInstanceOf[Gather[F, A, A]]

  private def mkHisto[F[_], A]: Gather[F, Attr[F, A], A] =
    new Optic[Unit, Attr[F, A], Unit, A, BiAffine]:
      type X = (Unit, F[Attr[F, A]])
      def to(u: Unit): BiAffine[X, Unit] = vestigialRead("histo")
      def from(xb: BiAffine[X, A]): Attr[F, A] = xb match
        case s: Step[X, A] => Attr(s.b, s.snd)
        case _: Done[X, A] => foldSideDone("histo")

  private val histoAny: AnyRef = mkHisto[[x] =>> Any, Any]

  /** Histomorphism decoration — the gather IS the [[Attr]] constructor: each node keeps its result
    * plus its children's full decorated histories.
    */
  def histo[F[_], A]: Gather[F, Attr[F, A], A] =
    histoAny.asInstanceOf[Gather[F, Attr[F, A], A]]
