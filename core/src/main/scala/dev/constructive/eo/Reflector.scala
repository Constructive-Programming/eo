package dev.constructive.eo

import cats.data.{Const, ZipList}
import cats.{Applicative, Apply, Id, Monoid}

/** Classifying typeclass for the [[data.Kaleidoscope]] optic family.
  *
  * Every `Reflector[F]` is an `Apply[F]` with an additional `reflect` operation: given an `F[A]`
  * and a function `f: F[A] => B` that aggregates the whole F-shape into a single `B`, produce an
  * `F[B]` whose shape is determined by `F`'s Applicative semantics. The exact shape varies per
  * instance — that variation is the optic's *whole point*, since downstream code picks which
  * aggregation semantics it wants at call time.
  *
  * **Why `Apply`, not `Applicative`.** The plan's D2 sketch suggested `extends Applicative[F]`.
  * That would exclude `cats.data.ZipList`, whose cats instance is only `CommutativeApply` — ZipList
  * has no top-level `pure` because it would need to synthesise an infinite list. Narrowing to
  * `Apply[F]` keeps all three shipped instances in the family:
  *   - `Apply[List]` — provided by cats via `Applicative[List]` (cartesian).
  *   - `Apply[ZipList]` — provided by cats via `CommutativeApply[ZipList]` (zipping).
  *   - `Apply[Const[M, *]]` — provided by cats via `Applicative[Const[M, *]]` (requires
  *     `Monoid[M]`, which we keep as the instance's constraint since that matches cats's surface).
  *
  * This mirrors Grate's plan-004 narrowing (`Distributive → Representable`) — the typeclass
  * substrate tightens to match the concrete instances' available capabilities.
  *
  * **What `reflect(fa)(f)` returns per instance** — this variation is load-bearing:
  *   - `forList` — singleton `List(f(fa))`. Matches `List`'s cartesian Applicative `pure`.
  *   - `forZipList` — broadcast: `ZipList(List.fill(fa.value.size)(f(fa)))`. ZipList's own `pure`
  *     would need an infinite list; we pick the length from `fa` so `reflect` stays total.
  *   - `forConst[M]` — phantom retag: `Const(fa.getConst)`. `Const[M, A]`'s `A` is phantom, so
  *     reflecting the same `m: M` into a `Const[M, B]` is the identity up to the type parameter.
  *     The aggregating function `f` has nowhere to land, which is exactly the `Const`-summation
  *     semantics — the monoid value already carries the aggregate.
  *
  * **Reflector laws** (verified as unit tests per instance in
  * `tests/src/test/scala/eo/ReflectorInstancesSpec.scala`, NOT a discipline RuleSet for v1 — see
  * plan Open Question #6):
  *
  *   - **R1 — map-compat**: `reflect(fa)(f).map(g) == reflect(fa)(fa_ => g(f(fa_)))`. Post-
  *     composition with a pure function distributes into the reflector's aggregator. Holds for all
  *     three shipped instances by construction: the output shape's elements are all `f(fa)` (or
  *     phantom for Const), and mapping over them composes pointwise.
  *   - **R2 — const-collapse**: `reflect(fa)(_ => b).map(_ => ())` has the same F-shape as
  *     `fa.map(_ => ())`. Witnesses that the aggregator's `B` doesn't alter the output F's *shape*
  *     — only its element values. Trivially holds for List (singleton-to-unit list), ZipList
  *     (broadcast-to-unit ZipList of same length), and Const (identity on the phantom side).
  *
  * **Honest status**: the Reflector typeclass is not as well-pinned in the literature as Traverse
  * or Distributive. The plan cited Penner's blog and the Clarke et al. categorical paper; neither
  * gives a single compact signature. The one here is the minimum needed by the Kaleidoscope carrier
  * shipped in [[data.Kaleidoscope]]; if a future Reflector user (or a stronger law) forces a
  * revised signature, this is the file to evolve.
  *
  * @see
  *   [[data.Kaleidoscope]] for the carrier that consumes Reflector instances.
  * @see
  *   `docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md` for the full rationale,
  *   including D2 (typeclass shape) and the Unit 1 research spike that narrowed `Applicative` to
  *   `Apply`.
  */
trait Reflector[F[_]] extends Apply[F]:

  /** The reflector operation — aggregate the entire F-shape into a single `B` via `f`, then lift
    * the result back into an `F[B]` whose per-element layout is determined by the instance.
    *
    * See the class-level documentation for what each shipped instance returns.
    */
  def reflect[A, B](fa: F[A])(f: F[A] => B): F[B]

/** Typeclass instances for [[Reflector]]. The v1 surface is deliberately narrow — three instances
  * witnessing the three distinct aggregation shapes cited in the plan (cartesian, zipping,
  * summation). Extending it further (`Option`, `NonEmptyList`, `Validated`, `ZipStream`, …) is
  * called out in the plan's Future Considerations.
  */
object Reflector:

  /** `Reflector[List]` — cartesian-product Applicative. `reflect(fa)(f)` returns the singleton
    * `List(f(fa))`, matching `Applicative[List].pure(f(fa))`. The reflector "collapses" the
    * aggregated result into a single cell, which is the cartesian semantics of composing through
    * `List.Applicative`.
    *
    * @group Instances
    */
  given forList: Reflector[List] with
    private val A: Applicative[List] = Applicative[List]

    def pure[A](x: A): List[A] = A.pure(x)

    def ap[A, B](ff: List[A => B])(fa: List[A]): List[B] = A.ap(ff)(fa)

    override def map[A, B](fa: List[A])(f: A => B): List[B] = A.map(fa)(f)

    override def product[A, B](fa: List[A], fb: List[B]): List[(A, B)] = A.product(fa, fb)

    def reflect[A, B](fa: List[A])(f: List[A] => B): List[B] = List(f(fa))

  /** `Reflector[ZipList]` — zipping Applicative. `reflect(fa)(f)` broadcasts `f(fa)` across the
    * ZipList's length, producing `ZipList(List.fill(fa.value.size)(f(fa)))`. This is the
    * length-aware analogue of `pure` — cats's `CommutativeApply[ZipList]` omits a top-level `pure`
    * because it would need an infinite list; the reflector sidesteps that by picking the length
    * from the incoming `fa`.
    *
    * Underlying Apply comes from cats's `CommutativeApply[ZipList]`
    * (`ZipList.catsDataCommutativeApplyForZipList`). We do NOT inherit `Applicative[ZipList]`
    * because cats does not ship one; see plan D2 / Unit 1 research notes.
    *
    * @group Instances
    */
  given forZipList: Reflector[ZipList] with
    private val A: Apply[ZipList] = ZipList.catsDataCommutativeApplyForZipList

    def ap[A, B](ff: ZipList[A => B])(fa: ZipList[A]): ZipList[B] = A.ap(ff)(fa)

    // No `pure` — ZipList has no top-level pure (would need an infinite list). The Reflector only
    // extends `Apply`, so `pure` is not part of the contract.

    override def map[A, B](fa: ZipList[A])(f: A => B): ZipList[B] = A.map(fa)(f)

    override def product[A, B](fa: ZipList[A], fb: ZipList[B]): ZipList[(A, B)] =
      A.product(fa, fb)

    def reflect[A, B](fa: ZipList[A])(f: ZipList[A] => B): ZipList[B] =
      val b = f(fa)
      ZipList(List.fill(fa.value.size)(b))

  /** `Reflector[Const[M, *]]` — summation-shaped Applicative. The `Const[M, A]`'s `A` is phantom,
    * so `reflect(fa)(f)` returns `fa.retag[B]` — the monoid value carries the aggregate on its own,
    * and the aggregator function has no runtime focus to consume. This is the degenerate shape but
    * preserves the `Reflector` contract: the laws reduce to identity on the phantom side.
    *
    * Requires `Monoid[M]` because cats's `Applicative[Const[M, *]]` does. The pass-through
    * `retag[B]` skips allocation on the JVM — `Const[M, A]` and `Const[M, B]` share the same
    * runtime layout.
    *
    * @group Instances
    */
  given forConst[M](using M: Monoid[M]): Reflector[Const[M, *]] with
    private val A: Applicative[Const[M, *]] = Applicative[Const[M, *]]

    def pure[A](x: A): Const[M, A] = A.pure(x)

    def ap[A, B](ff: Const[M, A => B])(fa: Const[M, A]): Const[M, B] = A.ap(ff)(fa)

    override def map[A, B](fa: Const[M, A])(f: A => B): Const[M, B] = A.map(fa)(f)

    override def product[A, B](fa: Const[M, A], fb: Const[M, B]): Const[M, (A, B)] =
      A.product(fa, fb)

    def reflect[A, B](fa: Const[M, A])(f: Const[M, A] => B): Const[M, B] =
      val _ = f(fa) // evaluated for consistency with the other instances (no-op on Const).
      fa.retag[B]

  /** `Reflector[Id]` — the degenerate identity instance. `cats.Id[A] = A`, so `reflect(a)(f)`
    * simplifies to `f(a)`: the aggregation collapses to the aggregator's return value with no
    * wrapping. This instance is **used only by** [[data.Kaleidoscope.forgetful2kaleidoscope]], the
    * Iso → Kaleidoscope bridge — an Iso's focus is a single `A`, so threading it through a
    * single-slot Reflector leaves the optic's semantics unchanged.
    *
    * Plan Open Question #3 weighed `Reflector[Id]` against `Reflector[List]` with a singleton-list
    * rebuild. `Reflector[Id]` wins because the `AssociativeFunctor[Kaleidoscope, Xo, Xi]` push side
    * reads `kO.focus.asInstanceOf[A]` — with `FCarrier = Id`, `kO.focus: Id[A] = A`, so the cast is
    * an exact type match. Using `List` would produce `kO.focus: List[A]` and the same cast would
    * fail at runtime when the inner's Reflector tried to walk the focus.
    *
    * @group Instances
    */
  given forId: Reflector[Id] with
    private val A: Applicative[Id] = Applicative[Id]

    def pure[A](x: A): Id[A] = x

    def ap[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = A.ap(ff)(fa)

    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

    override def product[A, B](fa: Id[A], fb: Id[B]): Id[(A, B)] = (fa, fb)

    def reflect[A, B](fa: Id[A])(f: Id[A] => B): Id[B] = f(fa)
