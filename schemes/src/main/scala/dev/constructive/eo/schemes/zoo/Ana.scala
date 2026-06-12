package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.{Getter, Optic}

/** Unfold-scheme citizen: Review-shaped, carrying the coalgebra + instances for fusion.
  *
  * See [[Cata]] for the general citizen contract (widening hazard, encoding rationale).
  */
final class Ana[F[_], Seed, S] private[schemes] (
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
    * should use `Schemes.hylo(coalg, alg)`, the zero-`S` spelling.
    *
    * Resolution: strictly more specific than the generic trait `cross`, so concrete-typed
    * `ana(c).cross(cata(a))` lands here (pinned by an ascription test); widened operands fall back
    * to the generic, materializing route — extensionally equal, allocation-different.
    */
  def cross[A](inner: Cata[F, S, A]): Getter[Seed, A] =
    val machine: Seed => (S, A) = Machines.fusedPairedFold(coalg, inner.alg)(using F, E)
    Getter[Seed, A](seed => machine(seed)._2)
