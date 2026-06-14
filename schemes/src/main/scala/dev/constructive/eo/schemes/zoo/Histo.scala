package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Histomorphism citizen — a course-of-value fold worn as an optic over
  * [[dev.constructive.eo.data.Direct]] with **`X = Attr[F, A]`**, the cofree comonad
  * `νX. A × F[X]`. This is the thesis at its sharpest: the histomorphism's existential is
  * *literally* the universal index for folds. `Cata` is the same optic at the forgetful resolution
  * `X = Nothing`; `Histo` keeps the whole decorated history, so `Histo : Cata :: Lens : Getter` —
  * the index refined from the trivial comonad up to the cofree one.
  *
  * `alg: F[Attr[F, A]] => A` sees, per child, not just its folded result but its entire decorated
  * subtree ([[Attr.head]] = result, [[Attr.tail]] = the child's own decorated layer), so it can
  * read arbitrarily far down — folds unreachable by a single-pass [[Cata]]. `Getter`-shaped
  * (`Optic[S, Unit, A, Unit, Direct]`), consumed via `.get`; the read projects the root's head
  * ([[Attr.forget]]).
  *
  * Space honesty: course-of-value recursion retains O(n) `Attr` cells by nature. Stack-safe (the
  * [[Machines.foldLayered]] machine). Heads-only (`alg ∘ map(_.head)`) degenerates to [[Cata]].
  */
final class Histo[F[_], S, A](private[zoo] val alg: F[Attr[F, A]] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Attr[F, A]

  private val toAttr: S => Attr[F, A] =
    Machines.foldLayered[F, S, Attr[F, A]](
      P.project,
      (_, layer) => Attr(alg(layer), layer),
    )

  private[zoo] val run: S => A = s => Attr.forget(toAttr(s))

  def to(s: S): Direct[X, A] = Direct(run(s))
  def from(b: Direct[X, Unit]): Unit = ()

  /** Metamorphism at the universal indices — the **fold→unfold dual of [[Futu.cross]]**'s chrono
    * (and the universal-index lift of [[Cata.meta]]). Fold `this` course-of-value to the neck `A`,
    * then multi-layer-unfold it with `futu` into a fresh `G`-recursive `T`. Reads `S => T`
    * (`.get`).
    *
    * Like [[Cata.meta]] this **does not fuse** — `F` and `G` differ, so the neck `A` is
    * materialised (the [[Meta.X]] is `A`). The cofree history and free multi-layering live on
    * either side of that neck, never cancelling across it.
    */
  def meta[G[_], T](futu: Futu[G, A, T]): MetaChrono[S, A, T] =
    new MetaChrono[S, A, T](run.andThen(futu.build))
