package dev.constructive.eo

import optics.Optic

/** Bridge between carriers — reshape an `F`-carrier optic into a `G`-carrier optic. Used by
  * `Optic.morph`; the mechanism by which optic families cross boundaries (Lens → Optional, Lens →
  * Setter, Iso → Lens, …).
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

  import data.Direct

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

  /** Express a read-only Direct optic (a Getter) as a Fold — lift the single focus into `F` via
    * `pure`. This is the `Composer[Direct, Forget[F]]` bridge
    * [[dev.constructive.eo.laws.GetterLaws]] anticipated; it became sound once `Fold` was made
    * honestly one-way (`B = Unit`), because the `from` is now genuinely unreachable: `Forget[F]`
    * admits no `ReverseAccessor`, so the resulting fold's build side is never invoked (mirrors
    * [[direct2either]]'s unreachable `Left`). Powers `Getter.andThen(Fold)` and `Optic.cross`
    * against a `Fold`. Requires `Applicative[F]` for `pure`.
    *
    * @group Instances
    */
  given direct2forget[F[_]](using F: cats.Applicative[F]): Composer[Direct, data.Forget[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, data.Forget[F]] =
      new Optic[S, T, A, B, data.Forget[F]]:
        type X = Nothing
        def to(s: S): data.Forget[F][X, A] = data.ForgetK(F.pure(o.to(s).value))
        def from(u: data.Forget[F][X, B]): T = ???

/** Low-priority `Composer` instances —
  * [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]], a transitive derivation pinned
  * to `Tuple2` as the intermediate. Same runtime cost as the explicit 2-hop call; compile-time
  * single given, no combinatorial enumeration; unambiguous by construction. Add a `chainViaEither`
  * at a still-lower priority if a future carrier needs it.
  */
trait LowPriorityComposerInstances:

  /** Transitive derivation via `Tuple2` as the intermediate carrier: given `F → Tuple2` and
    * `Tuple2 → G`, derive `F → G`. Fires cleanly for Direct-origin chains to any target with a
    * `Composer[Tuple2, _]` direct (Affine / SetterF / MultiFocus[F] / MultiFocus[PSVec]).
    *
    * @group Instances
    */
  given chainViaTuple2[F[_, _], G[_, _]](using
      f2tuple: Composer[F, Tuple2],
      tuple2g: Composer[Tuple2, G],
  ): Composer[F, G] with

    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] =
      tuple2g.to(f2tuple.to(o))
