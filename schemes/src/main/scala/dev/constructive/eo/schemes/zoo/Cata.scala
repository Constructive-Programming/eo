package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import optics.Optic

/** Catamorphism citizen — a **node-blind** fold worn as an optic over [[Scheme]] with `X = Nothing`,
  * the forgetful (trivial) resolution of the recursion index. `Getter`-shaped (`Optic[S, Unit, A,
  * Unit, Scheme]`): the read `to` runs the fold; the build side is vestigial. Carries `alg` so
  * [[Ana.cross]] can rebuild the fused [[Hylo]] machine.
  *
  * `alg: F[A] => A` sees only the already-folded children (named constructors), never the source
  * node — that blindness (`X = Nothing`) is the soundness condition that licenses fusion. Refining
  * `X` upward gives the richer folds: `F[(S, A)]` (paramorphism, a lawful Lens) and [[Attr]] =
  * `νX. A × F[X]` (histomorphism, the cofree comonad — see [[Histo]]).
  */
final class Cata[F[_], S, A](private[zoo] val alg: F[A] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Scheme]:
  type X = Nothing

  private val run: S => A =
    Machines.foldLayered[F, S, A](P.project, (_, fr) => alg(fr))

  def to(s: S): Scheme[X, A] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()
