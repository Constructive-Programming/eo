package dev.constructive.eo
package compose

import optics.Optic
import data.Affine

/** Picks the direction to morph a pair of optics into a shared carrier so `.andThen` works across
  * carriers. Four givens, decreasing priority:
  *   - [[Morph.same]] — same carrier, no morph (`Out = F`).
  *   - [[Morph.leftToRight]] — morph left into `G` via `Composer[F, G]` (`Out = G`).
  *   - [[Morph.rightToLeft]] — morph right into `F` via `Composer[G, F]` (`Out = F`).
  *   - [[LowPriorityMorphInstances.bothViaAffine]] — both into `Affine` when neither bridges
  *     directly (covers e.g. Prism ∘ Lens).
  *
  * cats-eo ships essentially one bidirectional Composer pair — `forget2multifocus` /
  * `multifocus2forget` (the latter restricted to `T = Unit`). For every other carrier pair at most
  * one of the first three fires; for that one pair a `Forget[F]` ⇄ `MultiFocus[F]` chain can be
  * ambiguous, resolved by routing through the explicit `Composer[..].to(o)` form (see
  * `multifocus2forget`'s docstring).
  *
  * @tparam F
  *   outer carrier
  * @tparam G
  *   inner carrier
  */
trait Morph[F[_, _], G[_, _]]:
  /** Common target carrier both `F` and `G` morph into. */
  type Out[_, _]

  /** Lift the outer (`F`-carrier) optic into the shared [[Out]] carrier. */
  def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, Out]

  /** Lift the inner (`G`-carrier) optic into the shared [[Out]] carrier. */
  def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, Out]

/** Low-priority `Morph` instances — consulted only when none of the higher-priority instances in
  * [[Morph$]] apply.
  */
trait LowPriorityMorphInstances:

  /** Both → Affine fallback. Enables `prism.andThen(lens)` (and the symmetric direction) when
    * neither side has a direct composer to the other.
    *
    * @group Instances
    */
  given bothViaAffine[F[_, _], G[_, _]](using
      cf: Composer[F, Affine],
      cg: Composer[G, Affine],
  ): Morph[F, G] with
    type Out[X, A] = Affine[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, Affine] = cf.to(o)
    def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, Affine] = cg.to(o)

/** Typeclass instances for [[Morph]]. */
object Morph extends LowPriorityMorphInstances:

  /** Identity morph; wins resolution when `F = G`. @group Instances */
  given same[F[_, _]]: Morph[F, F] with
    type Out[X, A] = F[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, F] = o
    def morphO[A, B, C, D](o: Optic[A, B, C, D, F]): Optic[A, B, C, D, F] = o

  /** Morph outer → inner's carrier via `Composer[F, G]`. @group Instances */
  given leftToRight[F[_, _], G[_, _]](using cf: Composer[F, G]): Morph[F, G] with
    type Out[X, A] = G[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] = cf.to(o)
    def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, G] = o

  /** Morph inner → outer's carrier via `Composer[G, F]`. @group Instances */
  given rightToLeft[F[_, _], G[_, _]](using cg: Composer[G, F]): Morph[F, G] with
    type Out[X, A] = F[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, F] = o
    def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, F] = cg.to(o)
