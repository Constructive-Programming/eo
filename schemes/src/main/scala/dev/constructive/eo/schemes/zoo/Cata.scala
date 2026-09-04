package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Catamorphism citizen — a **node-blind** fold ([[ReadScheme]]) with `X = Nothing`, the forgetful
  * (trivial) resolution of the recursion index. Carries `alg` so [[Ana.cross]] can rebuild the
  * fused [[Hylo]] machine.
  *
  * `alg: F[A] => A` sees only the already-folded children (named constructors), never the source
  * node — that blindness (`X = Nothing`) is the soundness condition that licenses fusion. Refining
  * `X` upward gives the richer folds: `F[(S, A)]` (paramorphism, [[Para]]) and [[Attr]] =
  * `νX. A × F[X]` (histomorphism, the cofree comonad — [[Histo]]).
  */
final class Cata[F[_], S, A](private[zoo] val alg: F[A] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends ReadScheme[S, A]:
  type X = Nothing

  private val run: S => A = Machines.foldLayered[F, S, A](P.project, (_, fr) => alg(fr))
  protected def read(s: S): A = run(s)

  /** Metamorphism — the fold→unfold seam, **dual to [[Ana.cross]]**'s unfold→fold. Fold `this` to
    * the neck `A`, then unfold it with `ana` into a fresh `G`-recursive `T`. **Does not fuse**:
    * fold over `F`, unfold over a possibly-different `G`, so the neck is materialised (the [[Meta]]
    * `X = A`).
    */
  def meta[G[_], T](ana: Ana[G, A, T]): Meta[S, A, T] =
    new Meta[S, A, T](run.andThen(ana.build))
