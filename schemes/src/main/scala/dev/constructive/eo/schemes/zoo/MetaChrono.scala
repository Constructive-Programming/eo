package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Metamorphism at the universal indices ([[ReadScheme]]) — the **fold→unfold dual of [[Chrono]]**,
  * reading `S => T`. Fold the `F`-recursive `S` course-of-value to a neck `A` (the cofree history,
  * [[Attr]]), then multi-layer-unfold `A` into a `G`-recursive `T` (the free coalgebra,
  * [[Coattr]]). Built by [[Histo.meta]] or [[MetaChrono.apply]].
  *
  * The universal-index twin of [[Meta]]: same `X = A` neck, same no-fusion (`F ≠ G`). The cofree
  * comonad on the fold side and the free monad on the unfold side never cancel across the neck —
  * `chrono` is exactly this combination *with `F = G`*, where they do.
  *
  * @tparam A
  *   the neck — the retained intermediate value type (the optic's existential `X`)
  */
final class MetaChrono[S, A, T] private[zoo] (private val run: S => T) extends ReadScheme[S, T]:
  type X = A
  protected def read(s: S): T = run(s)

object MetaChrono:

  /** The course-of-value fold → multi-layer unfold read `S => T`. **Does not fuse** — keeps both
    * `Basis`es (`Project[F, S]`, `Embed[G, T]`). Stack-safe (two passes).
    */
  def apply[F[_], S, A, G[_], T](
      algebra: F[Attr[F, A]] => A,
      coalg: A => G[Coattr[G, A]],
  )(using F: Traverse[F], P: Project[F, S], G: Traverse[G], E: Embed[G, T]): MetaChrono[S, A, T] =
    val fold: S => A =
      val toAttr = Machines.foldLayered[F, S, Attr[F, A]](P.project, Attr.decorate(algebra))
      s => Attr.forget(toAttr(s))
    val unfold: A => T =
      val run = Machines.buildLayered[G, Coattr[G, A], T](Coattr.expand(coalg))
      a => run(Coattr.Pure(a))
    new MetaChrono[S, A, T](fold.andThen(unfold))
