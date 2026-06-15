package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Metamorphism citizen — a **fold-then-unfold** read `S => T` ([[ReadScheme]]). The fold-direction
  * dual of [[Hylo]]: where `hylo` is the fused unfold-then-fold (`X = Nothing`, deforests), `meta`
  * is the fold-then-unfold whose existential **`X = A` is the neck** — the intermediate value the
  * fold produces and the unfold consumes.
  *
  * That non-trivial `X` is the honest statement that `meta` **cannot fuse**: it folds a functor `F`
  * down to `A`, then unfolds a *different* `G` back up; with `F ≠ G` there is no `project ∘ embed`
  * cancellation, so `A` is genuinely materialised. (Even `F = G` does not fuse it — the barrier is
  * the scalar neck, not the functor mismatch.) Built by [[Cata.meta]] or [[Meta.apply]].
  *
  * @tparam A
  *   the neck — the retained intermediate value type (the optic's existential `X`)
  */
final class Meta[S, A, T] private[zoo] (private val run: S => T) extends ReadScheme[S, T]:
  type X = A
  protected def read(s: S): T = run(s)

object Meta:

  /** The fold-then-unfold read `S => T`: fold the `F`-recursive `S` to a neck `A` (node-blind
    * `alg`), then unfold `A` into a fresh `G`-recursive `T` (`coalg`). **Does not fuse** — it keeps
    * *both* `Basis`es (`Project[F, S]` to fold, `Embed[G, T]` to build); where `hylo` needs only
    * `Traverse`, `meta` cannot drop either. Stack-safe (two [[Machines.foldLayered]] passes).
    */
  def apply[F[_], S, A, G[_], T](alg: F[A] => A, coalg: A => G[A])(using
      F: Traverse[F],
      P: Project[F, S],
      G: Traverse[G],
      E: Embed[G, T],
  ): Meta[S, A, T] =
    val fold: S => A = Machines.foldLayered[F, S, A](P.project, (_, fr) => alg(fr))
    val unfold: A => T = Machines.buildLayered[G, A, T](coalg)
    new Meta[S, A, T](fold.andThen(unfold))
