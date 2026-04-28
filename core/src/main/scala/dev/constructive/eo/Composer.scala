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
  *      intermediate. Covers Forgetful-origin chains uniformly without introducing the implicit
  *      ambiguity the earlier fully-general `chain[F, G, H]` had.
  */
object Composer extends LowPriorityComposerInstances:

  import data.Forgetful

  /** Express an Iso (or Getter) as a Lens — the Lens's leftover is `Unit` because the bijection
    * doesn't need any.
    *
    * @group Instances
    */
  given forgetful2tuple: Composer[Forgetful, Tuple2] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Tuple2] =
      new Optic[S, T, A, B, Tuple2]:
        type X = Unit
        val to: S => (X, A) = s => ((), o.to(s))
        val from: ((X, B)) => T = pair => o.from(pair._2)

  /** Express an Iso (or Getter) as a Prism — always takes the `Right` branch; `Nothing` in the
    * `Left` slot so the miss branch is uninhabited.
    *
    * @group Instances
    */
  given forgetful2either: Composer[Forgetful, Either] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Either] =
      new Optic[S, T, A, B, Either]:
        type X = Nothing
        val to: S => Either[X, A] = s => Right(o.to(s))
        val from: Either[X, B] => T = e =>
          e match
            case Right(b) => o.from(b)
            case Left(_)  => ???

/** Low-priority `Composer` instances —
  * [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]], a transitive derivation pinned
  * to `Tuple2` as the intermediate. Same runtime cost as the explicit 2-hop call; compile-time
  * single given, no combinatorial enumeration; unambiguous by construction. Add a `chainViaEither`
  * at a still-lower priority if a future carrier needs it.
  */
trait LowPriorityComposerInstances:

  /** Transitive derivation via `Tuple2` as the intermediate carrier: given `F → Tuple2` and
    * `Tuple2 → G`, derive `F → G`. Fires cleanly for Forgetful-origin chains to any target with a
    * `Composer[Tuple2, _]` direct (Affine / SetterF / PowerSeries / MultiFocus[F]).
    *
    * @group Instances
    */
  given chainViaTuple2[F[_, _], G[_, _]](using
      f2tuple: Composer[F, Tuple2],
      tuple2g: Composer[Tuple2, G],
  ): Composer[F, G] with

    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] =
      tuple2g.to(f2tuple.to(o))
