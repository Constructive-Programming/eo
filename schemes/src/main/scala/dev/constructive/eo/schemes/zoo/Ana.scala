package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import optics.Optic

/** Anamorphism citizen — an unfold worn as an optic over [[Scheme]] with `X = S` (the structure it
  * threads). `Review`-shaped (`Optic[Unit, S, Unit, Seed, Scheme]`): the build `from` runs the
  * unfold; the read side is vestigial. Carries `coalg` so [[cross]] can fuse with a node-blind
  * [[Cata]]. Refining `X` upward to [[Coattr]] = `μX. Seed + F[X]` (the free monad) gives the
  * multi-layer unfold (futumorphism — see [[Futu]]).
  */
final class Ana[F[_], Seed, S](private[zoo] val coalg: Seed => F[Seed])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends Optic[Unit, S, Unit, Seed, Scheme]:
  type X = S

  private[zoo] val build: Seed => S =
    Machines.foldLayered[F, Seed, S](coalg, (_, fr) => E.embed(fr))

  def to(u: Unit): Scheme[X, Unit] = Scheme(())
  def from(b: Scheme[X, Seed]): S = build(Scheme.value(b))

  /** The fused hylo seam: ana ∘ a **node-blind** [[Cata]]. Because the fold retains nothing of the
    * tree (`X = Nothing`), deforestation is sound — rebuild the one-pass [[Machines.foldLayered]]
    * machine from `this.coalg` + `cata.alg`, building **no intermediate `S`**. This is what makes
    * `ana.cross(cata)` *be* [[Schemes.hylo]] rather than a materialise-then-fold.
    *
    * A member (not the generic `Optic.cross` extension, which would `reverse.andThen` into a
    * materialising read) so it wins overload resolution and the fusion is the default.
    */
  def cross[B](cata: Cata[F, S, B]): Hylo[Seed, B] =
    new Hylo[Seed, B](Machines.foldLayered[F, Seed, B](coalg, (_, fr) => cata.alg(fr))(using F))

  /** The fused **dynamorphism** seam: plain unfold ∘ course-of-value fold ([[Histo]]). The
    * refold-quadrant diagonal between [[cross]]'s `hylo` (plain→plain) and [[Futu.cross]]'s
    * `chrono` (free→cofree): a plain `ana` unfold whose fold sees each node's full decorated
    * history. Fuses — the [[Attr]] cofree memo is threaded internally, no intermediate `S`.
    * Delegates to [[Schemes.dyna]] (`Traverse[F]` only; the `Histo`'s `Project` goes unused).
    */
  def cross[B](histo: Histo[F, S, B]): Hylo[Seed, B] =
    Schemes.dyna[F, Seed, B](coalg, histo.alg)(using F)
