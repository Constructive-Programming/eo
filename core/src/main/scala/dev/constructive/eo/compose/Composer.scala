package dev.constructive.eo
package compose

import cats.{Applicative, Foldable}

import optics.Optic

/** Bridge between carriers — reshape an `F`-carrier optic into a `G`-carrier optic. Used by
  * `Optic.morph`; the mechanism by which optic families cross boundaries (Lens → Optional, Lens →
  * Modify, Iso → Lens, …).
  *
  * @tparam F
  *   source carrier
  * @tparam G
  *   target carrier
  */
trait Composer[F[_, _], G[_, _]]:
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]

/** Typeclass instances for [[Composer]]. Additional composers live near the carrier they produce
  * (e.g. `Composer[Tuple2, Affine]` in [[data.Affine]]).
  *
  * Resolution tiers:
  *   1. Regular: direct bridges here and in each carrier's companion.
  *   2. Low (from [[LowPriorityComposerInstances]]):
  *      [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]] with `Tuple2` as a fixed
  *      intermediate. Covers Direct-origin chains uniformly without introducing the implicit
  *      ambiguity the earlier fully-general `chain[F, G, H]` had.
  */
object Composer extends LowPriorityComposerInstances:

  import data.{Direct, Forget, MultiFocusK}

  /** Express an Iso (or Getter) as a Lens — the Lens's leftover is `Unit` because the bijection
    * doesn't need any.
    *
    * @group Instances
    */
  given direct2tuple: Composer[Direct, Tuple2] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Tuple2] =
      new Optic[S, T, A, B, Tuple2]:
        type X = Unit
        def to(s: S): (X, A) = ((), o.to(s).value)
        def from(pair: (X, B)): T = o.from(Direct(pair._2))

  /** Express an Iso (or Getter) as a Prism — always takes the `Right` branch; `Nothing` in the
    * `Left` slot so the miss branch is uninhabited.
    *
    * @group Instances
    */
  given direct2either: Composer[Direct, Either] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Either] =
      new Optic[S, T, A, B, Either]:
        type X = Nothing
        def to(s: S): Either[X, A] = Right(o.to(s).value)
        def from(e: Either[X, B]): T =
          e match
            case Right(b) => o.from(Direct(b))
            // `X = Nothing` makes `Left` uninhabited; returning the `Nothing` value (which
            // conforms to `T`) keeps the match total and compiler-verified — no `???` needed.
            case Left(x) => x

  /** Express a Direct optic (a Getter, Review, or Iso) as a `Forget[F]`-carrier optic — lift the
    * single focus into `F` via `pure` on the read side, and pick the single `B` back out of the
    * `F[B]` on the build side. This is the `Composer[Direct, Forget[F]]` bridge
    * [[dev.constructive.eo.laws.GetterLaws]] anticipated. Powers `Getter.andThen(Fold)`,
    * `Optic.cross` against a `Fold`, and — since [[optics.Unfold]] gave `Forget[F]` a genuine
    * build-only citizen — the build side of `review.andThen(unfold)` chains.
    *
    * The `from` was a documented-unreachable `???` while `Forget[F]` had no build-capable
    * inhabitant; `Unfold` made it reachable (via `assocForgetMonad.composeFrom` on a `Monad[F]`
    * chain). The singleton pick is sound on every reachable path: the only `F[B]` ever fed to a
    * lifted Direct optic's `from` is `ForgetPull.monadicPull`'s `pure(b)`. A hand-routed call with
    * cardinality ≠ 1 throws, mirroring the other `pickSingletonOrThrow` bridges. Requires
    * `Applicative[F]` for `pure` and `Foldable[F]` for the pick.
    *
    * @group Instances
    */
  given direct2forget[F[_]](using
      F: Applicative[F],
      FF: Foldable[F],
  ): Composer[Direct, Forget[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Forget[F]] =
      new Optic[S, T, A, B, Forget[F]]:
        type X = Nothing
        def to(s: S): Forget[F][X, A] = Forget(F.pure(o.to(s).value))
        def from(fb: Forget[F][X, B]): T =
          o.from(Direct(MultiFocusK.pickSingletonOrThrow[F, B](fb.value, "Direct")))

/** Low-priority `Composer` instances —
  * [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]], a transitive derivation pinned
  * to `Tuple2` as the intermediate. Same runtime cost as the explicit 2-hop call; compile-time
  * single given, no combinatorial enumeration; unambiguous by construction. Add a `chainViaEither`
  * at a still-lower priority if a future carrier needs it.
  */
trait LowPriorityComposerInstances:

  /** Transitive derivation via `Tuple2` as the intermediate carrier: given `F → Tuple2` and
    * `Tuple2 → G`, derive `F → G`. Fires cleanly for Direct-origin chains to any target with a
    * `Composer[Tuple2, _]` direct (Affine / ModifyF / MultiFocus[F] / MultiFocus[PSVec]).
    *
    * @group Instances
    */
  given chainViaTuple2[F[_, _], G[_, _]](using
      f2tuple: Composer[F, Tuple2],
      tuple2g: Composer[Tuple2, G],
  ): Composer[F, G] with

    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] =
      tuple2g.to(f2tuple.to(o))
