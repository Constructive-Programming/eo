package dev.constructive.eo

import optics.Optic
import data.Affine

/** Picks the direction in which to morph a pair of optics into a shared carrier, so that `.andThen`
  * can be called on optics whose carriers differ.
  *
  * Four given instances cover the common cases, in decreasing priority:
  *   - [[Morph.same]] — both carriers are the same `F`, no morphing; `Out = F`.
  *   - [[Morph.leftToRight]] — when a `Composer[F, G]` exists, morph the left (outer) side into
  *     `G`; `Out = G`.
  *   - [[Morph.rightToLeft]] — when a `Composer[G, F]` exists, morph the right (inner) side into
  *     `F`; `Out = F`.
  *   - [[LowPriorityMorphInstances.bothViaAffine]] — fallback when neither side bridges to the
  *     other: if both `F` and `G` reach `Affine` via a `Composer`, lift both into the `Affine`
  *     carrier; `Out = Affine`. This covers pairs like `(Either, Tuple2)` (Prism ∘ Lens) where
  *     `cats-eo` ships no direct bridge in either direction.
  *
  * `cats-eo` doesn't ship bidirectional composers (no pair has both `Composer[F, G]` *and*
  * `Composer[G, F]`), so at most one of the first three fires for any concrete pair. The
  * `bothViaAffine` instance lives in a lower-priority parent trait so it only kicks in when the
  * higher-priority ones are all inapplicable.
  *
  * @tparam F
  *   outer optic's carrier
  * @tparam G
  *   inner optic's carrier
  */
trait Morph[F[_, _], G[_, _]]:
  /** Common target carrier both `F` and `G` morph into. */
  type Out[_, _]

  /** Lift the outer (`F`-carrier) optic into the shared [[Out]] carrier. */
  def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, Out]

  /** Lift the inner (`G`-carrier) optic into the shared [[Out]] carrier. */
  def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, Out]

/** Low-priority `Morph` instances — consulted only when the higher-priority instances in [[Morph$]]
  * don't apply. Kept in a separate trait purely for given-priority purposes.
  */
trait LowPriorityMorphInstances:

  /** Fallback morph: when `F` and `G` have no direct `Composer` between them but both reach
    * `Affine`, lift both sides into the `Affine` carrier. Enables `prism.andThen(lens)` and the
    * symmetric `lens.andThen(prism)` to work via a common `Affine`-shaped result.
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

/** Typeclass instances for [[Morph]]. Four built-ins: `same` (identity morph), `leftToRight` (lift
  * `F` into `G` via `Composer[F, G]`), `rightToLeft` (dual via `Composer[G, F]`), and the
  * low-priority `bothViaAffine` fallback that sends both sides through `Affine`.
  */
object Morph extends LowPriorityMorphInstances:

  /** Same carrier — no morphing. Most-specific given, wins overload resolution whenever `F = G`.
    *
    * @group Instances
    */
  given same[F[_, _]]: Morph[F, F] with
    type Out[X, A] = F[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, F] = o
    def morphO[A, B, C, D](o: Optic[A, B, C, D, F]): Optic[A, B, C, D, F] = o

  /** Morph the outer side to the inner side's carrier via `Composer[F, G]`.
    *
    * @group Instances
    */
  given leftToRight[F[_, _], G[_, _]](using cf: Composer[F, G]): Morph[F, G] with
    type Out[X, A] = G[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] = cf.to(o)
    def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, G] = o

  /** Morph the inner side to the outer side's carrier via `Composer[G, F]`.
    *
    * @group Instances
    */
  given rightToLeft[F[_, _], G[_, _]](using cg: Composer[G, F]): Morph[F, G] with
    type Out[X, A] = F[X, A]
    def morphSelf[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, F] = o
    def morphO[A, B, C, D](o: Optic[A, B, C, D, G]): Optic[A, B, C, D, F] = cg.to(o)
