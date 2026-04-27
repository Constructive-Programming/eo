package dev.constructive.eo

import optics.Optic

/** Bridge between two carriers — reshape an `F`-carrier optic into a `G`-carrier optic preserving
  * both halves. Required by `Optic.morph`; the principal mechanism by which optic families cross
  * boundaries (Lens → Optional, Lens → Setter, Iso → Lens, …).
  *
  * @tparam F
  *   source carrier
  * @tparam G
  *   target carrier
  */
trait Composer[F[_, _], G[_, _]]:
  /** Bridge an `F`-carrier optic into a `G`-carrier optic.
    *
    * @tparam S
    *   source type
    * @tparam T
    *   result type
    * @tparam A
    *   focus read
    * @tparam B
    *   focus written back
    */
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]

/** Typeclass instances for [[Composer]]. Additional composers live near the carrier they produce:
  * `Composer[Tuple2, Affine]` under [[data.Affine]], `Composer[Tuple2, SetterF]` under
  * [[data.SetterF]], `Composer[Tuple2, PowerSeries]` under [[data.PowerSeries]], etc.
  *
  * Resolution tiers:
  *   1. Regular priority: atomic direct bridges defined here and in each carrier's companion. Fast
  *      at both compile and runtime.
  *   2. Low priority (inherited from [[LowPriorityComposerInstances]]): a single
  *      [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]] derivation with `Tuple2` as
  *      the fixed intermediate. Covers Forgetful-origin chains to Affine / SetterF / PowerSeries /
  *      MultiFocus[F] uniformly without introducing ambiguity. Carriers that don't have a
  *      `Composer[Tuple2, G]` bridge (e.g. the Function1-shaped MultiFocus absorbed from the v1
  *      Grate carrier) ship their own `Composer[Forgetful, X]` directly at tier 1.
  *
  * An earlier `chain[F, G, H]` derivation over arbitrary intermediate `G` was removed during the
  * 2026-04-24 resolution refactor because it introduced implicit-search ambiguity whenever two
  * intermediate carriers both reached the target (e.g. `Composer[Forgetful, PowerSeries]` via
  * `Tuple2` OR via `Either`). The fixed-intermediate `chainViaTuple2` is unambiguous by
  * construction.
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

/** Low-priority `Composer` instances. The lone inhabitant is
  * [[LowPriorityComposerInstances.chainViaTuple2 chainViaTuple2]] — a transitive derivation with
  * `Tuple2` pinned as the intermediate carrier. Lifts any `Composer[F, Tuple2]` +
  * `Composer[Tuple2, G]` pair into `Composer[F, G]` without introducing the ambiguity the older
  * fully-general `chain[F, G, H]` did.
  *
  * Why fixed-intermediate:
  *
  *   - Runtime: identical to a direct 2-hop chain — one `cf.to` call, one `cg.to` call, two optic
  *     wrapper allocations. Same cost as the previous direct `Composer[Forgetful, X]` givens that
  *     explicitly spelled out `tuple2X.to(forgetful2tuple.to(o))`.
  *   - Compile time: one named given to consider at the low-priority tier, with two direct implicit
  *     parameters. No combinatorial enumeration over intermediate carriers.
  *   - Correctness: unambiguous by construction. Previously `chain` matched with multiple `G`
  *     instantiations whenever two paths existed (e.g. Tuple2 and Either both bridging Forgetful to
  *     PowerSeries), and Scala 3's ambiguity rule killed the entire search. With a fixed
  *     intermediate, there's only one candidate path.
  *
  * Non-Tuple2 intermediate paths — currently none exist in the shipped composer table — can be
  * added as additional fixed-intermediate givens if future carriers need them (e.g. a hypothetical
  * `chainViaEither` at a still-lower priority).
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
