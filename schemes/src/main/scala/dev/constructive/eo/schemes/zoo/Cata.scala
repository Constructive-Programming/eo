package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Catamorphism citizen — a **node-blind** fold worn as an optic over
  * [[dev.constructive.eo.data.Direct]] with `X = Nothing`, the forgetful (trivial) resolution of
  * the recursion index. `Getter`-shaped (`Optic[S, Unit, A, Unit, Direct]`): the read `to` runs the
  * fold; the build side is vestigial. Carries `alg` so [[Ana.cross]] can rebuild the fused [[Hylo]]
  * machine.
  *
  * `alg: F[A] => A` sees only the already-folded children (named constructors), never the source
  * node — that blindness (`X = Nothing`) is the soundness condition that licenses fusion. Refining
  * `X` upward gives the richer folds: `F[(S, A)]` (paramorphism, a lawful Lens) and [[Attr]] =
  * `νX. A × F[X]` (histomorphism, the cofree comonad — see [[Histo]]).
  */
final class Cata[F[_], S, A](private[zoo] val alg: F[A] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Nothing

  private[zoo] val run: S => A =
    Machines.foldLayered[F, S, A](P.project, (_, fr) => alg(fr))

  def to(s: S): Direct[X, A] = Direct(run(s))
  def from(b: Direct[X, Unit]): Unit = ()

  /** Metamorphism — the fold→unfold seam, **dual to [[Ana.cross]]**'s unfold→fold. Fold `this` to
    * the neck value `A`, then unfold it with `ana` into a fresh `G`-recursive `T`. The result
    * [[Meta]] reads `S => T` (`.get`).
    *
    * Unlike [[Ana.cross]] this **does not fuse**: the fold is over `F` and the unfold over a
    * possibly-different `G`, so there is no `project ∘ embed` cancellation — the neck `A` is
    * genuinely materialised (the [[Meta.X]] is `A`, not `Nothing`). That heterogeneity is exactly
    * why `meta` keeps both `Basis`es while `hylo` drops them.
    */
  def meta[G[_], T](ana: Ana[G, A, T]): Meta[S, A, T] =
    new Meta[S, A, T](run.andThen(ana.build))
