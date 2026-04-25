package dev.constructive.eo
package data

import optics.Optic

import dev.constructive.eo.Reflector

/** Carrier for the `Kaleidoscope` optic family — an `Applicative`-parameterised aggregation optic
  * whose behaviour at composition time is picked by the `Reflector[F]` the user plugs in.
  *
  * Conceptually Kaleidoscope is "a focus `F[A]` together with a way to project back to the world
  * `X`". The `Reflector[F]` supplied at construction time determines the aggregation shape —
  * cartesian (List), zipping (ZipList), summation (Const[M, *]), etc. — that the `.collect`
  * universal uses to reduce the focus down to a single value.
  *
  * **Path-type encoding (plan D1).** `FCarrier[_]` is a type member, not a type parameter — so
  * `Kaleidoscope` can still be used as a two-argument carrier `[X, A]` at the `Optic`'s `F[_, _]`
  * slot. At constructor sites the `FCarrier` is fixed (e.g. to `List`, `ZipList`, or
  * `Const[M, *]`); through the abstract `Optic[…, Kaleidoscope]` slot, the member becomes opaque.
  * This mirrors the `Optic.X` existential convention.
  *
  * **Invariants.**
  *   - `focus: FCarrier[A]` — the aggregated input, typically the entire `F[A]` at construction
  *     time.
  *   - `rebuild: FCarrier[A] => X` — how to recover the `X` world from an `FCarrier[A]`-shaped
  *     aggregate. At every shipped constructor site `X = FCarrier[A]` and `rebuild = identity`; the
  *     slot becomes load-bearing for `.collect` (which passes a *reflected* `F[B]` through it) and
  *     for `AssociativeFunctor` composition.
  *   - `reflector: Reflector[FCarrier]` — the classifying typeclass witness; mirrors how
  *     `AlgLens[F]` carries around its classifier constraint.
  *
  * **Why paired, not continuation-shaped.** The textbook Kaleidoscope signature
  * `((F[A] => F[B]) => T)` is a continuation — closer to Penner's operational sketch. The paired
  * encoding fits cats-eo's existing carriers (`Tuple2`, `Affine`, `AlgLens[F]`, `PowerSeries`,
  * `Grate`) and cleanly supports `ForgetfulFunctor` / `AssociativeFunctor` instance machinery. The
  * cost: the rebuild post-map has to broadcast a constant `X` (see [[Kaleidoscope.kalFunctor]]) —
  * acceptable because `.modify` / `.replace` consume only the focus, never the rebuild.
  *
  * @tparam X
  *   existential leftover — at shipped constructor sites equals `FCarrier[A]`; abstract through the
  *   `Optic` trait's `type X`.
  * @tparam A
  *   focus type (the element type of `FCarrier[A]`, not the whole focus).
  *
  * @see
  *   [[dev.constructive.eo.Reflector]] for the classifying typeclass.
  * @see
  *   `docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md` for the full rationale — D1
  *   (carrier encoding), D2 (Reflector narrowing), D4 (given instance table).
  */
trait Kaleidoscope[X, A]:
  type FCarrier[_]
  given reflector: Reflector[FCarrier]
  val focus: FCarrier[A]
  val rebuild: FCarrier[A] => X

/** Constructors and typeclass instances for [[Kaleidoscope]].
  *
  * The shipped v1 instances are the minimum required by the plan's Requirements Trace (R4):
  *   - [[kalFunctor]] — unlocks `.modify` / `.replace` on any `Kaleidoscope`-carrier optic.
  *   - [[kalAssoc]] — unlocks same-carrier `.andThen` between two `Kaleidoscope`-carrier optics.
  *
  * Kaleidoscope deliberately does NOT ship `ForgetfulFold` / `ForgetfulTraverse` / `Accessor`; see
  * the plan's D4 table. The focus is the *aggregate*, not N individual foci, so a per-element fold
  * / traversal story is misleading. Foldable on the concrete `F` is available at the call site when
  * `F` supports it (e.g. `List`, `ZipList`).
  *
  * Factory constructor [[apply]] and the `Composer[Forgetful, Kaleidoscope]` bridge land in
  * subsequent Implementation Units (3 and 4).
  */
object Kaleidoscope:

  /** Helper — construct a Kaleidoscope value from its four required pieces (path-type F, reflector
    * witness, focus, rebuild). Keeps the anonymous-class boilerplate tidy at call sites.
    */
  private[eo] def make[X, A, F[_]](
      R: Reflector[F],
      focus0: F[A],
      rebuild0: F[A] => X,
  ): Kaleidoscope[X, A] { type FCarrier[T] = F[T] } =
    new Kaleidoscope[X, A]:
      type FCarrier[T] = F[T]
      given reflector: Reflector[FCarrier] = R
      val focus: FCarrier[A] = focus0
      val rebuild: FCarrier[A] => X = rebuild0

  /** `ForgetfulFunctor[Kaleidoscope]` — unlocks `.modify` / `.replace`. Maps the focus through `f`
    * using `Reflector[FCarrier]`'s Apply-superclass `map`, and broadcasts the rebuild to a constant
    * returning the original `X`.
    *
    * **Why the rebuild broadcasts a constant.** The carrier's rebuild is typed `FCarrier[A] => X`;
    * after mapping A → B we'd need `FCarrier[B] => X`, but the only handle we have is the forward
    * `f: A => B`. Without an inverse `B => A` we cannot post-compose into `FCarrier[B]` space.
    * Since `from` in every shipped factory ignores the rebuild (only `focus` drives `.modify` /
    * `.replace`), the mapped rebuild's only job is to be well-typed — a constant `_ => rebuild0`
    * suffices. The original rebuild is captured in a `lazy val` so the broadcast value is computed
    * at most once (and skipped entirely if `from` never consults it).
    *
    * If a future extension adds a Kaleidoscope-carrier `from` that does inspect its input's
    * rebuild, this strategy will need a redesign — tracked as a future concern in the plan.
    *
    * @group Instances
    */
  given kalFunctor: ForgetfulFunctor[Kaleidoscope] with

    def map[X, A, B](k: Kaleidoscope[X, A], f: A => B): Kaleidoscope[X, B] =
      val R: Reflector[k.FCarrier] = k.reflector
      val fb: k.FCarrier[B] = R.map(k.focus)(f)
      lazy val rebuilt: X = k.rebuild(k.focus)
      make[X, B, k.FCarrier](R, fb, (_: k.FCarrier[B]) => rebuilt)

  /** `AssociativeFunctor[Kaleidoscope, Xo, Xi]` — same-carrier `.andThen`.
    *
    * `Z = (Xo, Xi)` — outer and inner existentials carried as a pair, mirroring every other pair-
    * shaped carrier's `AssociativeFunctor` instance (`Tuple2`, `Grate`, `AlgLens[F]`).
    *
    * **Scoped same-F restriction (plan D1 / Risk 2 resolved).** The composition requires outer and
    * inner to share the same `FCarrier`. The plan acknowledged this as a possible fallback if
    * path-type propagation through the abstract Optic slot failed; in practice it was the only
    * tractable option. Composing two Kaleidoscopes with different Reflectors would require a
    * coherent "merge" of the two aggregation shapes (e.g. zip + cartesian) that isn't obviously
    * well-defined and isn't part of Penner's textbook definition. The casts below align the inner
    * and outer `FCarrier`s — safe in practice because every v1 construction pathway (the generic
    * [[apply]] factory; the Iso → Kaleidoscope bridge) fixes `FCarrier = F` per call site, and
    * `same-F` composition is the only composition users can meaningfully write.
    *
    * **Push side** — read outer to obtain `kO = (focusO, rebuildO)`. At the shipped sites the
    * outer's focus IS the `A`-shaped aggregate (X = F[A], so `focus: F[A]` and `A` corresponds to
    * the focus's element type). Read inner by applying inner.to to the outer's "canonical A" —
    * derived via the Reflector contract: the outer's rebuild closes over the world `X`, so the
    * outer's focus *is* the canonical A-aggregate the inner should read from. The cast aligns types
    * through the abstract Optic slot.
    *
    * **Pull side** — receive `Kaleidoscope[Z, D]`, reconstruct inner's rebuild via projection onto
    * `z._2` (drop outer's index), run `inner.from` to get a `B`, then `outer.from` on a
    * constant-rebuild Kaleidoscope whose focus is broadcast from `b`. The outer's rebuild
    * reconstruction is a sentinel — structurally unreachable under the v1 shipped factories,
    * documented for future-work cross-F bridges.
    *
    * @group Instances
    */
  given kalAssoc[Xo, Xi]: AssociativeFunctor[Kaleidoscope, Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Kaleidoscope] { type X = Xo },
        inner: Optic[A, B, C, D, Kaleidoscope] { type X = Xi },
    ): Kaleidoscope[Z, C] =
      val kO = outer.to(s)
      // Derive the canonical A to hand to `inner.to`. At shipped sites:
      //   - Iso bridge (FCarrier = Id): `focus: Id[A] = A` — cast is an identity.
      //   - Generic factory (FCarrier = F, X = F[A]): `focus: F[A]`. Here casting to `A`
      //     is semantically "the aggregate IS the thing the inner reads" — coherent only
      //     when F[A] = A structurally, which is NOT generally true.
      //
      // The latter case is the "cross-F composition doesn't make sense" story the plan's
      // D1/D3 call out. In v1 the meaningful compositions are:
      //   (a) Iso → Kaleidoscope — outer is Id, cast is free. Works.
      //   (b) Kaleidoscope → Kaleidoscope with same F — focus shape matches.
      // Pure (b) is the main `.andThen` path once both sides are user-constructed Kaleidoscopes.
      val a: A = kO.focus.asInstanceOf[A]
      val kI = inner.to(a)
      // Use the INNER's FCarrier/Reflector for the composed result — the aggregation semantics
      // live in the inner (the outer, for the Iso bridge, is a trivial Id). This matches the
      // cross-F composition pattern where the inner's carrier "wins" because it's the one that
      // actually reflects/aggregates.
      val RI: Reflector[kI.FCarrier] = kI.reflector
      val focusC: kI.FCarrier[C] = kI.focus
      val rebuildC: kI.FCarrier[C] => Z = (fc: kI.FCarrier[C]) =>
        val xo: Xo = kO.rebuild(kO.focus)
        val xi: Xi = kI.rebuild(fc)
        (xo, xi)
      make[Z, C, kI.FCarrier](RI, focusC, rebuildC)

    def composeFrom[S, T, A, B, C, D](
        xd: Kaleidoscope[Z, D],
        inner: Optic[A, B, C, D, Kaleidoscope] { type X = Xi },
        outer: Optic[S, T, A, B, Kaleidoscope] { type X = Xo },
    ): T =
      val R: Reflector[xd.FCarrier] = xd.reflector
      // Reconstruct inner's Kaleidoscope — xd.rebuild(fd) yields (Xo, Xi); project the Xi half.
      val innerK: Kaleidoscope[Xi, D] { type FCarrier[T] = xd.FCarrier[T] } =
        make[Xi, D, xd.FCarrier](
          R,
          xd.focus,
          (fd: xd.FCarrier[D]) => xd.rebuild(fd)._2,
        )
      val b: B = inner.from(innerK)
      // Reconstruct outer's Kaleidoscope — the outer's focus is `b` (singleton from the Iso
      // bridge's Id-shape; unused for other shipped sites since `from` ignores it). We
      // construct a minimal Id-carrier Kaleidoscope here; if the outer's FCarrier is something
      // else, its `from` should still work because v1 factories only consult `focus` on the
      // pull side.
      val outerK =
        make[Xo, B, cats.Id](
          summon[Reflector[cats.Id]],
          b,
          (_: cats.Id[B]) => xd.rebuild(xd.focus)._1,
        )
      outer.from(outerK.asInstanceOf[Kaleidoscope[Xo, B]])

  // ------------------------------------------------------------------
  // Factory (Unit 3)
  // ------------------------------------------------------------------

  /** Generic Kaleidoscope factory — given any `Reflector[F]`, construct an optic whose focus is the
    * entire `F[A]` and whose rebuild is identity. This is the single entry point per plan D5; no
    * tuple-shaped convenience overloads (tuples are not Reflector-shaped).
    *
    * Encoding: `X = F[A]`, `focus = fa`, `rebuild = identity`. After composition through the
    * abstract `Optic[…, Kaleidoscope]` slot the path-type `FCarrier` becomes opaque, so
    * `.collect[F, ...]` takes `F` as an explicit type argument.
    *
    * @group Factories
    * @example
    *   {{{
    * import cats.data.ZipList
    * import dev.constructive.eo.data.Kaleidoscope
    * import dev.constructive.eo.data.Kaleidoscope.given
    * import dev.constructive.eo.optics.Optic.*
    *
    * val k = Kaleidoscope.apply[ZipList, Double]
    * // Column-wise aggregation — `.collect` receives the whole ZipList
    * // focus and folds it down with the user's aggregator.
    * val mean = k.collect[ZipList, Double](zl => zl.value.sum / zl.value.size.toDouble)
    * mean(ZipList(List(1.0, 2.0, 3.0)))   // 2.0
    *   }}}
    */
  def apply[F[_], A](using F: Reflector[F]): Optic[F[A], F[A], A, A, Kaleidoscope] =
    new Optic[F[A], F[A], A, A, Kaleidoscope]:
      type X = F[A]
      val to: F[A] => Kaleidoscope[F[A], A] =
        (fa: F[A]) => make[F[A], A, F](F, fa, identity[F[A]])
      val from: Kaleidoscope[F[A], A] => F[A] = k =>
        // The shipped rebuild for this factory is `identity: F[A] => F[A]`. After `.modify(f)`
        // lands via [[kalFunctor]], the rebuild becomes the constant broadcast — but the focus
        // has already been mapped by `f`, so returning the focus gives the correct result.
        k.focus.asInstanceOf[F[A]]

  // ------------------------------------------------------------------
  // Kaleidoscope-specific extension — `.collect` (the Kaleidoscope universal)
  // ------------------------------------------------------------------

  /** `.collect[F, B](agg)` — the Kaleidoscope universal. Given an aggregator `agg: F[A] => B`,
    * reduce the focus down to a single `B` via `Reflector.reflect`, broadcast it back across `F`,
    * then feed the resulting `F[B]` through the Kaleidoscope's rebuild to recover a `T`-shaped
    * result.
    *
    * For the generic [[apply]] factory (`X = F[A]`, `rebuild = identity`, `T = F[A]`) this reduces
    * to:
    *   1. Project the source via `o.to(s)` — yields `Kaleidoscope[F[A], A]` with focus `fa`.
    *   2. `reflect(fa)(agg): F[B]` via the Reflector.
    *   3. Since `T = F[A]` and `A = B` at the monomorphic factory site, the F[B] IS the result.
    *
    * **Why `F` is an explicit type parameter.** `Kaleidoscope`'s `FCarrier` path-type becomes
    * opaque once the optic flows through `Optic[…, Kaleidoscope]`, so the extension can't recover
    * it structurally. Requiring the user to write `collect[F, B](agg)` at the call site is the
    * ergonomic trade-off the plan's D5 accepts for the single-factory surface.
    *
    * @group Operations
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, Kaleidoscope])

    def collect[F[_], C](agg: F[A] => C)(using F: Reflector[F], ev: C =:= B): S => T =
      val _ = ev // evidence compile-time only; pins C to B so the Reflector's B aligns with the
      // optic's output focus.
      (s: S) =>
        val k = o.to(s)
        // Cast the path-type FCarrier to `F` — safe at every v1 construction site.
        val reflected: F[B] = F.reflect(k.focus.asInstanceOf[F[A]])(agg).asInstanceOf[F[B]]
        val rebuildOrig: F[A] => o.X = k.rebuild.asInstanceOf[F[A] => o.X]
        val kReflected =
          make[o.X, B, F](F, reflected, (fb: F[B]) => rebuildOrig(fb.asInstanceOf[F[A]]))
        o.from(kReflected.asInstanceOf[Kaleidoscope[o.X, B]])

  // ------------------------------------------------------------------
  // Composer bridge (Unit 4)
  // ------------------------------------------------------------------

  /** Trivial injection `Forgetful ↪ Kaleidoscope` — lets an `Iso`-carrier optic compose against a
    * `Kaleidoscope`-carrier optic via cross-carrier `.andThen`. The Iso's forward `to: S => A`
    * becomes the focus of an `Id`-carrier Kaleidoscope (`FCarrier = Id`, so `Id[A] = A`), with the
    * rebuild reading back through the Iso's reverse `from: B => T`.
    *
    * **Why `Reflector[Id]`.** Plan Open Question #3 weighed `Id` against `List` with a singleton-
    * list rebuild. `Id` wins because the [[kalAssoc]] push side does `kO.focus.asInstanceOf[A]`,
    * which is an exact type match under `FCarrier = Id` (since `Id[A] = A`). Using `List` would
    * wrap the Iso's focus in a singleton list and the same cast would fail at runtime when the
    * inner Kaleidoscope's Reflector walked the focus expecting `A` and found `List[A]`. The bridge
    * is the sole user of [[Reflector.forId]] — documented there.
    *
    * Does **not** ship the mirror `Composer[Tuple2, Kaleidoscope]` (Lens → Kaleidoscope) by design
    * — see plan D3, same shape as Grate's Lens → Grate deferral. A Lens's source type has no
    * natural `Reflector` witness; users who want Lens → Kaleidoscope should construct the
    * Kaleidoscope separately at the Lens's focus type and compose via `Lens.andThen`. The
    * resolution miss surfaces as `"Morph[Tuple2, Kaleidoscope]"` implicit not found.
    *
    * @group Instances
    */
  given forgetful2kaleidoscope: Composer[Forgetful, Kaleidoscope] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Kaleidoscope] =
      new Optic[S, T, A, B, Kaleidoscope]:
        type X = S
        val to: S => Kaleidoscope[S, A] = (s: S) =>
          val R = summon[Reflector[cats.Id]]
          // `Id[A] = A` — the Iso's focus IS the Id-wrapped focus, no constructor allocation.
          make[S, A, cats.Id](R, o.to(s), (_: cats.Id[A]) => s)
        val from: Kaleidoscope[S, B] => T = k =>
          // The `from` consumes only the mapped focus. For the Iso bridge `FCarrier = Id`, so
          // `focus: Id[B] = B` — hand it straight to the Iso's reverse side.
          val b = k.focus.asInstanceOf[B]
          o.from(b)
