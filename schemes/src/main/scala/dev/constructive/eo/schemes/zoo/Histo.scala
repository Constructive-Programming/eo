package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Histomorphism citizen — a course-of-value fold ([[ReadScheme]]) with **`X = Attr[F, A]`**, the
  * cofree comonad `νX. A × F[X]`. The thesis at its sharpest: the histomorphism's existential is
  * *literally* the universal index for folds. `Cata` is the same shape at `X = Nothing`; `Histo`
  * keeps the whole decorated history, so `Histo : Cata :: Lens : Getter`.
  *
  * `alg: F[Attr[F, A]] => A` sees, per child, not just its folded result but its entire decorated
  * subtree ([[Attr.head]] = result, [[Attr.tail]] = the child's own decorated layer) — folds
  * unreachable by a single-pass [[Cata]]. Heads-only (`alg ∘ map(_.head)`) degenerates to [[Cata]].
  * Space honesty: course-of-value recursion retains O(n) `Attr` cells by nature. Stack-safe.
  */
final class Histo[F[_], S, A](private[zoo] val alg: F[Attr[F, A]] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends ReadScheme[S, A]:
  type X = Attr[F, A]

  private val toAttr: S => Attr[F, A] =
    Machines.foldLayered[F, S, Attr[F, A]](P.project, Attr.decorate(alg))

  private val run: S => A = s => Attr.forget(toAttr(s))
  protected def read(s: S): A = run(s)

  /** Metamorphism at the universal indices — the **fold→unfold dual of [[Futu.cross]]**'s chrono.
    * Fold `this` course-of-value to the neck `A`, then multi-layer-unfold it with `futu` into a
    * fresh `G`-recursive `T`. **Does not fuse** — `F`/`G` differ, so the neck is materialised.
    */
  def meta[G[_], T](futu: Futu[G, A, T]): MetaChrono[S, A, T] =
    new MetaChrono[S, A, T](run.andThen(futu.build))
