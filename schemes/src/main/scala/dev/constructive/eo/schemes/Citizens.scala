package dev.constructive.eo
package schemes

import cats.Traverse

import data.Direct
import optics.{Getter, Optic}

/** Concrete scheme citizens — `cataF`/`anaF` return these instead of bare `Getter`/`Review` so
  * composition can **fuse**: the classes carry their (co)algebra and instances as data, and the
  * fused `cross` overload below resolves on the concrete types.
  *
  * They extend the open `Optic` trait directly (`Getter`/`Review` are `final` in core — the
  * perf-pinned encoding stays untouched): full generic composition via the trait members, plus
  * `.get` / `.reverseGet` as stored fields, the use-site-friendly shape.
  *
  * Widening hazard, documented: binding an `AnaF` to a wider type (`Review`-shaped `Optic`) loses
  * the fused `cross` overload — the generic trait `cross` still typechecks and is extensionally
  * equal, but materializes the full intermediate structure. `Schemes.hyloF(coalg, alg)` stays the
  * always-fused spelling.
  */

/** Fold-scheme citizen: Getter-shaped, carrying the node-supplied algebra for fusion. */
final class CataF[F[_], S, A] private[schemes] (
    val get: S => A,
    private[schemes] val alg: (S, F[A]) => A,
) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Nothing

  def to(s: S): Direct[X, A] = Direct(get(s))
  def from(d: Direct[X, Unit]): Unit = ()

  /** View as a plain [[Getter]] — re-enters Getter's fused composition fast paths (and resolves the
    * read-compose overload tie an unascribed `getter.andThen(cataF(...))` can hit).
    */
  def asGetter: Getter[S, A] = Getter(get)

/** Unfold-scheme citizen: Review-shaped, carrying the coalgebra + instances for fusion. */
final class AnaF[F[_], Seed, S] private[schemes] (
    val reverseGet: Seed => S,
    private[schemes] val coalg: Seed => F[Seed],
)(using
    private[schemes] val F: Traverse[F],
    private[schemes] val E: Embed[F, S],
) extends Optic[Unit, S, Unit, Seed, Direct]:
  type X = Nothing

  def to(u: Unit): Direct[X, Unit] = Direct(u)
  def from(d: Direct[X, Seed]): S = reverseGet(d.value)

  /** View as a plain [[optics.Review]] — re-enters Review's fused composition fast paths. NOTE: the
    * widened value loses the fused `cross` below (the widening hazard).
    */
  def asReview: optics.Review[S, Seed] = optics.Review(reverseGet)

  /** THE fusion seam — deforestation as composition, on the seam core names for it (`Optic.cross`'s
    * motivating case is `ana.cross(cata)`). One single-pass machine: each node is built once,
    * folded immediately, and released as the fold ascends — **no full-tree retention, no second
    * traversal** (the materializing spelling builds all of `S`, then folds it). The algebra is
    * node-supplied, so per-node construction is semantically required; a node-*blind* computation
    * should use `Schemes.hyloF(coalg, alg)`, the zero-`S` spelling.
    *
    * Resolution: strictly more specific than the generic trait `cross`, so concrete-typed
    * `anaF(c).cross(cataF(a))` lands here (pinned by an ascription test); widened operands fall
    * back to the generic, materializing route — extensionally equal, allocation-different.
    */
  def cross[A](inner: CataF[F, S, A]): Getter[Seed, A] =
    val machine: Seed => (S, A) = Schemes.fusedPairedFold(coalg, inner.alg)(using F, E)
    Getter[Seed, A](seed => machine(seed)._2)
