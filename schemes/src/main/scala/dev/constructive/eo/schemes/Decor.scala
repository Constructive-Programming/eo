package dev.constructive.eo
package schemes

/** Decoration data for the typed recursion-scheme zoo (histo / futu) — hand-rolled, droste-style,
  * rather than cats-free: no `Eval` suspension fields (the `foldLayered` machine never suspends),
  * no dependency, shapes tuned to the engine.
  *
  * Space honesty: a histomorphism decorates **every** node with its full sub-result history, so a
  * fold over n nodes retains O(n) [[Attr]] cells until the algebra releases them. That is inherent
  * to course-of-value recursion, not an engine artifact.
  */

/** Cofree-without-laziness: a fold result (`head`) decorating one layer of already-decorated
  * children (`tail`). The histomorphism's algebra sees `F[Attr[F, A]]` — each child's result *plus*
  * that child's entire decorated history.
  *
  * @tparam F
  *   the pattern functor
  * @tparam A
  *   the fold result decorating each node
  */
final case class Attr[F[_], A](head: A, tail: F[Attr[F, A]])

object Attr:

  /** Discard the history, keep the top result — `histo`'s final projection. */
  def forget[F[_], A](attr: Attr[F, A]): A = attr.head

/** Free-without-suspension: a futumorphism's coalgebra answers each slot with either a seed still
  * to expand ([[Coattr.Pure]]) or an already-known layer to unroll without consulting the coalgebra
  * again ([[Coattr.Roll]]) — the multi-layer-per-step channel.
  *
  * @tparam F
  *   the pattern functor
  * @tparam A
  *   the seed type
  */
enum Coattr[F[_], A]:

  /** A seed — the engine calls the coalgebra on it. */
  case Pure(a: A)

  /** A prebuilt layer — unrolled directly, no coalgebra call for this layer. */
  case Roll(layer: F[Coattr[F, A]])

// ===========================================================================================
// The Decor family — decorations as optics over the BiAffine carrier.
//
// A generalized scheme's decoration is an optic whose existential leftover is one F-layer.
// Sides are pinned in the TYPE, eo-style (read-only/build-only citizens):
//
//   - Fold side (DecorGather): build-only. `from` is the GATHER — it consumes
//     `Step(layer: F[W], result: A)` and produces the decoration `W` (histo's gather is
//     literally the `Attr` constructor). The read side is vestigial (throws, the
//     `Unfold.algebra` precedent); `Done` never occurs on this side.
//
//   - Unfold side (DecorScatter): a full citizen. `to` is the SCATTER — an affine match
//     answering each slot with `Step(_, seed)` (call the coalgebra) or `Done(layer: F[W])`
//     (a prebuilt layer; unroll it, no coalgebra call). `from` on the Step arm is the
//     POINTED unit — the seed injection `A => W` (gana's `pure`: ana = identity, apo =
//     `Right`, futu = `Coattr.Pure`), giving the unit law `to(from(Step((), a))) ==
//     Step((), a)`.
//
// X pinning per shape: gather side X = (Unit, F[W]) (Snd = the one-F-layer context); scatter
// side X = (F[W], Unit) (Fst = the prebuilt-layer Done payload). The generic drivers in
// [[Schemes]] are fully typed against these refinements — no casts at the seam.
//
// NOTE (generic vs native routes): `Decor.apo`'s generic scatter unrolls a grafted subtree
// through `Project` (droste's distApo — O(graft)). The O(1) graft is the privilege of the
// NATIVE `apoF` engine, which prefills result slots from apo's `Done` directly and never
// consults this value. Likewise `Decor.para`'s generic gather re-embeds the subterm it pairs
// (droste's Gather.para); the native `paraF` pairs subterms from the walked nodes instead.
// ===========================================================================================

/** Fold-side decoration: gather-only (build-only member). `from` = gather. */
type DecorGather[F[_], W, A] =
  optics.Optic[Unit, W, Unit, A, data.BiAffine] { type X = (Unit, F[W]) }

/** Unfold-side decoration: scatter (`to`, an affine match) + pointed unit (`from` on Step). */
type DecorScatter[F[_], W, A] =
  optics.Optic[W, W, A, A, data.BiAffine] { type X = (F[W], Unit) }

/** Named decoration values — the recursion-scheme zoo's vocabulary. */
object Decor:
  import cats.Functor

  import data.BiAffine
  import data.BiAffine.{Done, Step}
  import optics.Optic

  private def vestigialRead(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Decor.$name is gather-only (build-only): its read side is vestigial by specification"
    )

  private def foldSideDone(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Decor.$name is a fold-side decoration: Done never occurs on the gather seam"
    )

  private def scatterSideDone(name: String): Nothing =
    throw new UnsupportedOperationException(
      s"Decor.$name: the pointed unit (from) is inhabited on the Step arm only"
    )

  // ----- fold side (gather) ------------------------------------------------

  private def mkCata[F[_], A]: DecorGather[F, A, A] =
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
  def cata[F[_], A]: DecorGather[F, A, A] = cataAny.asInstanceOf[DecorGather[F, A, A]]

  /** Paramorphism decoration: each child slot pairs the original subterm with its result.
    *
    * Generic-route honesty: this gather *re-embeds* the subterm from the layer (droste's
    * `Gather.para`) — the native `paraF` avoids that by pairing subterms from the nodes the machine
    * already walks.
    */
  def para[F[_]: Functor, S, A](using E: Embed[F, S]): DecorGather[F, (S, A), A] =
    new Optic[Unit, (S, A), Unit, A, BiAffine]:
      type X = (Unit, F[(S, A)])
      def to(u: Unit): BiAffine[X, Unit] = vestigialRead("para")
      def from(xb: BiAffine[X, A]): (S, A) = xb match
        case s: Step[X, A] => (E.embed(Functor[F].map(s.snd)(_._1)), s.b)
        case _: Done[X, A] => foldSideDone("para")

  private def mkHisto[F[_], A]: DecorGather[F, Attr[F, A], A] =
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
  def histo[F[_], A]: DecorGather[F, Attr[F, A], A] =
    histoAny.asInstanceOf[DecorGather[F, Attr[F, A], A]]

  // ----- unfold side (scatter + pointed unit) -------------------------------

  private def mkAna[F[_], A]: DecorScatter[F, A, A] =
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
  def ana[F[_], A]: DecorScatter[F, A, A] = anaAny.asInstanceOf[DecorScatter[F, A, A]]

  /** Apomorphism decoration, generic route: `Right(seed)` keeps unfolding, `Left(s)` answers with
    * the grafted subtree's projected layer — distApo, O(graft) through `Project`. The O(1) graft is
    * the native `apoF` engine's privilege; it never consults this value.
    */
  def apo[F[_]: Functor, S, A](using P: Project[F, S]): DecorScatter[F, Either[S, A], A] =
    new Optic[Either[S, A], Either[S, A], A, A, BiAffine]:
      type X = (F[Either[S, A]], Unit)
      def to(w: Either[S, A]): BiAffine[X, A] = w match
        case Right(a) => new Step[X, A]((), a)
        case Left(s)  => new Done[X, A](Functor[F].map(P.project(s))(Left(_)))
      def from(xb: BiAffine[X, A]): Either[S, A] = xb match
        case s: Step[X, A] => Right(s.b)
        case _: Done[X, A] => scatterSideDone("apo")

  private def mkFutu[F[_], A]: DecorScatter[F, Coattr[F, A], A] =
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
  def futu[F[_], A]: DecorScatter[F, Coattr[F, A], A] =
    futuAny.asInstanceOf[DecorScatter[F, Coattr[F, A], A]]
