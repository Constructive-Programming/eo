package dev.constructive.eo
package schemes
package zoo

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic

// ===========================================================================================
// Gather / Scatter — the decoration optics, over the BiAffine carrier.
//
// A generalized scheme's decoration is an optic whose existential leftover is one F-layer
// (the names echo droste's Gather/Scatter and recursion-schemes' distCata/distHisto/...).
// Both are ABSTRACT CLASSES, not type aliases: a decoration is written by extending the
// class and implementing ONE named method (`gather` / `scatter` + `unit`) — the Optic
// `to`/`from` plumbing over BiAffine is implemented once, here, and the generic drivers in
// [[Schemes]] call the named methods directly (no per-node carrier wrappers on the generic
// route). Sides are pinned in the TYPE, eo-style (read-only/build-only citizens):
//
//   - Fold side (Gather): build-only. [[Gather.gather]] consumes one decorated layer
//     `F[W]` plus the node's result `A` and produces the decoration `W` (histo's gather is
//     literally the `Attr` constructor). The optic read side is vestigial (throws, the
//     `Unfold.algebra` precedent); `Done` never occurs on this side.
//
//   - Unfold side (Scatter): a full citizen. [[Scatter.scatter]] answers each slot with
//     `Right(seed)` (call the coalgebra) or `Left(layer)` (a prebuilt `F[W]`; unroll it, no
//     coalgebra call — the `Done` arm). [[Scatter.unit]] is the POINTED unit — the seed
//     injection `A => W` (gana's `pure`: ana = identity, futu = `Coattr.Pure`), giving the
//     unit law `to(from(Step((), a))) == Step((), a)`.
//
// X pinning per shape: gather side X = (Unit, F[W]) (Snd = the one-F-layer context); scatter
// side X = (F[W], Unit) (Fst = the prebuilt-layer Done payload).
//
// para and apo have NO shipped values here: their generic routes are deliberately inferior
// (para's gather would re-embed each subterm — droste's Gather.para; apo's scatter would
// re-walk grafts through Project — distApo, O(graft)), and the native `Schemes.para` /
// `Schemes.apo` engines subsume them. Their decoration semantics survive as law fixtures in
// the test suite (GatherScatterLawsSpec), pinning the native routes to the definitions.
// ===========================================================================================

/** Fold-side decoration optic: gather-only (build-only member). Extend and implement [[gather]];
  * the `BiAffine` optic surface (`from` = gather on the `Step` arm, vestigial read) is provided
  * here.
  */
abstract class Gather[F[_], W, A] extends Optic[Unit, W, Unit, A, BiAffine]:
  type X = (Unit, F[W])

  /** The gather: one decorated layer plus the node's result, to the node's decoration. */
  def gather(layer: F[W], a: A): W

  final def to(u: Unit): BiAffine[X, Unit] =
    throw new UnsupportedOperationException(
      "Gather is gather-only (build-only): its read side is vestigial by specification"
    )

  final def from(xb: BiAffine[X, A]): W = xb match
    case s: Step[X, A] => gather(s.snd, s.b)
    case _: Done[X, A] =>
      throw new UnsupportedOperationException(
        "Gather is a fold-side decoration: Done never occurs on the gather seam"
      )

/** Named fold-side decoration values. */
object Gather:

  /** The undecorated fold — gather keeps the result, discards the layer (`W = A`). With it, the
    * generic `Schemes.cata(gather)(galg)` agrees with the direct `Schemes.cata(galg)` (law-pinned);
    * the direct overload IS the fast path, no dispatch involved.
    */
  final class Id[F[_], A] extends Gather[F, A, A]:
    def gather(layer: F[A], a: A): A = a

  /** @see [[Id]] */
  def cata[F[_], A]: Gather[F, A, A] = new Id[F, A]

  /** Histomorphism decoration — the gather IS the [[Attr]] constructor: each node keeps its result
    * plus its children's full decorated histories.
    */
  final class Histo[F[_], A] extends Gather[F, Attr[F, A], A]:
    def gather(layer: F[Attr[F, A]], a: A): Attr[F, A] = Attr(a, layer)

  /** @see [[Histo]] */
  def histo[F[_], A]: Gather[F, Attr[F, A], A] = new Histo[F, A]
