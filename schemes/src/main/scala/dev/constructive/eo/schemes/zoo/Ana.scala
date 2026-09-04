package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Anamorphism citizen — an unfold ([[BuildScheme]]) with `X = S` (the structure it threads).
  * Carries `coalg` so [[cross]] can fuse with a node-blind [[Cata]] (→ [[Hylo]]) or a
  * course-of-value [[Histo]] (→ [[Dyna]]). Refining `X` upward to [[Coattr]] = `μX. Seed + F[X]`
  * (the free monad) gives the multi-layer unfold (futumorphism — [[Futu]]).
  */
final class Ana[F[_], Seed, S](private[zoo] val coalg: Seed => F[Seed])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends BuildScheme[S, Seed]:
  type X = S

  private[zoo] val build: Seed => S =
    Machines.buildLayered[F, Seed, S](coalg)

  protected def write(seed: Seed): S = build(seed)

  /** The fused **hylo** seam: ana ∘ a node-blind [[Cata]]. Because the fold retains nothing (`X =
    * Nothing`), deforestation is sound — the one-pass machine is rebuilt from `coalg` + `cata.alg`,
    * building **no intermediate `S`**. A member (not the generic `Optic.cross`, which would
    * `reverse.andThen` into a materialising read) so the fusion wins overload resolution.
    */
  def cross[B](cata: Cata[F, S, B]): Hylo[Seed, B] = Hylo[F, Seed, B](coalg, cata.alg)

  /** The fused **dynamorphism** seam: plain unfold ∘ course-of-value fold ([[Histo]]) — the
    * refold-quadrant diagonal between [[cross]]'s `hylo` and [[Futu.cross]]'s `chrono`. Fuses; the
    * [[Attr]] cofree memo is threaded internally, no intermediate `S`.
    */
  def cross[B](histo: Histo[F, S, B]): Dyna[Seed, B] = Dyna[F, Seed, B](coalg, histo.alg)
