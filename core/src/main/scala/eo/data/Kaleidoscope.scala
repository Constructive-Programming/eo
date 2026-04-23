package eo
package data

import optics.Optic

import eo.Reflector

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
  *   [[eo.Reflector]] for the classifying typeclass.
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
      // Derive the canonical A to hand to `inner.to`. The shipped generic factory sets
      // `X = FCarrier[A]` with `rebuild = identity`, so `rebuildO(focusO) = focusO: FCarrier[A]`
      // — the A-aggregate itself. In the continuation optic-calculus interpretation, the whole
      // `focus: FCarrier[A]` stands in for "the A the outer focuses on"; the inner then sees
      // that aggregate as its A.
      //
      // The cast through `A` is the scoped-same-F assumption: we're treating the outer's
      // FCarrier[A] (which at the factory site IS the whole A) as the abstract `A` the Optic
      // trait presents. This is safe by construction for every v1 constructor — the Iso bridge
      // (Unit 4) and the generic factory (Unit 3) both set FCarrier appropriately.
      val a: A = kO.focus.asInstanceOf[A]
      val kI = inner.to(a)
      // Convert the inner's FCarrier into the outer's FCarrier type for the result. Both are
      // path-types reducing to the same underlying F — safe by construction at v1 sites.
      val RO: Reflector[kO.FCarrier] = kO.reflector
      val focusC: kO.FCarrier[C] = kI.focus.asInstanceOf[kO.FCarrier[C]]
      val rebuildC: kO.FCarrier[C] => Z = (fc: kO.FCarrier[C]) =>
        // `kO.rebuild(kO.focus) : Xo` is the outer's "current X" — captured lazily since
        // v1 factories never consult `Z._1` on the pull side.
        val xo: Xo = kO.rebuild(kO.focus)
        val xi: Xi = kI.rebuild(fc.asInstanceOf[kI.FCarrier[C]])
        (xo, xi)
      make[Z, C, kO.FCarrier](RO, focusC, rebuildC)

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
      // Reconstruct outer's Kaleidoscope — broadcast `b` across the FCarrier via the Reflector's
      // map (there's no `pure` on the Apply superclass in general — ZipList doesn't have one —
      // so we use the current focus as a length witness and replace each element with b). The
      // outer's rebuild is reconstructed to thread `_._1` (Xo projection), though at shipped
      // sites it's never consulted.
      val outerFocus: xd.FCarrier[B] = R.map(xd.focus)(_ => b)
      val outerK: Kaleidoscope[Xo, B] { type FCarrier[T] = xd.FCarrier[T] } =
        make[Xo, B, xd.FCarrier](
          R,
          outerFocus,
          (_: xd.FCarrier[B]) => xd.rebuild(xd.focus)._1,
        )
      outer.from(outerK)
