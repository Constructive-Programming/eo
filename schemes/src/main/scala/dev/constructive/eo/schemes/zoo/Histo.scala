package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import optics.Optic

/** Histomorphism citizen — a course-of-value fold worn as an optic over [[Scheme]] with **`X =
  * Attr[F, A]`**, the cofree comonad `νX. A × F[X]`. This is the thesis at its sharpest: the
  * histomorphism's existential is *literally* the universal index for folds. `Cata` is the same
  * optic at the forgetful resolution `X = Nothing`; `Histo` keeps the whole decorated history, so
  * `Histo : Cata :: Lens : Getter` — the index refined from the trivial comonad up to the cofree one.
  *
  * `alg: F[Attr[F, A]] => A` sees, per child, not just its folded result but its entire decorated
  * subtree ([[Attr.head]] = result, [[Attr.tail]] = the child's own decorated layer), so it can read
  * arbitrarily far down — folds unreachable by a single-pass [[Cata]]. `Getter`-shaped (`Optic[S,
  * Unit, A, Unit, Scheme]`), consumed via `.get`; the read projects the root's head ([[Attr.forget]]).
  *
  * Space honesty: course-of-value recursion retains O(n) `Attr` cells by nature. Stack-safe (the
  * [[Machines.foldLayered]] machine). Heads-only (`alg ∘ map(_.head)`) degenerates to [[Cata]].
  */
final class Histo[F[_], S, A](private[zoo] val alg: F[Attr[F, A]] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Scheme]:
  type X = Attr[F, A]

  private val toAttr: S => Attr[F, A] =
    Machines.foldLayered[F, S, Attr[F, A]](
      P.project,
      (_, layer) => Attr(alg(layer), layer),
    )

  def to(s: S): Scheme[X, A] = Scheme(Attr.forget(toAttr(s)))
  def from(b: Scheme[X, Unit]): Unit = ()
