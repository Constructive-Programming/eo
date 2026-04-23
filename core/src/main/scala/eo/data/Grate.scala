package eo
package data

import optics.Optic

import cats.Representable

/** Paired carrier for the `Grate` optic family — `Grate[X, A] = (A, X => A)`.
  *
  * Grate is the dual of `Lens`: where a Lens decomposes a product `S` into a focus `A` alongside a
  * leftover, a Grate **lifts** a source-consuming function `X => A` through a *distributive /
  * Naperian* shape. Its classical profunctor shape is `((S => A) => B) => T`; in `cats-eo` we
  * encode it as a pair of
  *   - `a: A` — a representative focus (read by `.modify`, `.replace`), and
  *   - `k: X => A` — a rebuild function that re-reads the focus from a reassembled source.
  *
  * This paired encoding fits every existing pair-shaped carrier (`Tuple2`, `Affine`, `AlgLens[F]`,
  * `SetterF`, `PowerSeries`). The "continuation" encoding `(X => A) => A` is closer to the textbook
  * shape but would force a `Closed` profunctor analogue that `cats-eo` does not ship; see the plan
  * at `docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md` (Key Technical Decisions D1) for
  * the full rationale.
  *
  * At every constructor site in this file `X = S` — the rebuild `X => A` is `s0 => read(s0)` where
  * `read` is the Grate's own observation. When the Grate is carried through the existential
  * position of `Optic[…, Grate]`, `X` becomes abstract, same pattern as `Affine`'s `X`.
  *
  * The rebuild slot is load-bearing for Grate-specific operations (`zipWithF`, `collect` — see
  * extension methods in the companion); it is deliberately ignored by `.modify` / `.replace`
  * because once the user's `f: A => B` has fired, the rebuild becomes a function of the already-
  * produced `B`.
  */
type Grate[X, A] = (A, X => A)

/** Constructors and typeclass instances for [[Grate]].
  *
  * The shipped instances are the minimum required by the plan's Requirements Trace R2:
  *   - [[grateFunctor]] — unlocks `.modify` / `.replace` on any `Grate`-carrier optic.
  *   - [[grateAssoc]] — unlocks same-carrier `.andThen` between two `Grate`-carrier optics.
  *
  * Grate deliberately does **not** ship `ForgetfulFold` / `ForgetfulTraverse` / `Accessor`; see the
  * plan's D4 table. `ForgetfulTraverse` duplicates `Traversal.each`; `Accessor` is mismatched
  * because classical Grate has no plain `.get`.
  *
  * Factory constructors (`Grate.apply[F: Distributive]`, `Grate.tuple[T]` / arity fallbacks) and
  * the `Composer[Forgetful, Grate]` bridge land in subsequent Implementation Units.
  */
object Grate:

  /** `ForgetfulFunctor[Grate]` — unlocks `.modify` / `.replace`. Maps the focus through `f` and
    * post-composes the rebuild with `f` so the invariant `k(x) = a` survives the map: after
    * mapping, the new focus is `f(a)` and the new rebuild is `k andThen f`.
    *
    * One allocation per map (a fresh pair + a fresh `andThen` closure). No walk over the
    * distributive structure — that happens on the original `to` / `from` call, not during
    * `.modify`'s focus rewrite.
    *
    * @group Instances
    */
  given grateFunctor: ForgetfulFunctor[Grate] with

    def map[X, A, B](ga: (A, X => A), f: A => B): (B, X => B) =
      (f(ga._1), ga._2.andThen(f))

  /** `AssociativeFunctor[Grate, Xo, Xi]` — same-carrier `.andThen`.
    *
    * `Z = (Xo, Xi)` — the combined existential is the product of outer and inner rebuilds. Push
    * side reads `(a, kO)` from the outer and `(c, kI)` from the inner on that `a`, producing the
    * fused rebuild `(z: Z) => kI(z._2)`. Pull side (the `.modify` path) ignores the rebuild slots
    * on both sides — once the user's `f: C => D` has fired through [[grateFunctor]], the carried
    * focus is already the post-update value and the rebuild slots collapse to "return the focus"
    * (placeholder closures `_ => d` and `_ => b`).
    *
    * Caveat (mirrored in the class-level note on Grate): the rebuild fields in the composite are
    * placeholders. This matches the `.modify` contract because the focus half is already
    * load-bearing; operations that read the rebuild slot (`zipWithF`, `collect` — future work) need
    * a different composition path, which would ship as a specialised bridge alongside those
    * extensions.
    *
    * @group Instances
    */
  given grateAssoc[Xo, Xi]: AssociativeFunctor[Grate, Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Grate] { type X = Xo },
        inner: Optic[A, B, C, D, Grate] { type X = Xi },
    ): (C, Z => C) =
      val (a, _) = outer.to(s)
      val (c, kI) = inner.to(a)
      (c, (z: Z) => kI(z._2))

    def composeFrom[S, T, A, B, C, D](
        xd: (D, Z => D),
        inner: Optic[A, B, C, D, Grate] { type X = Xi },
        outer: Optic[S, T, A, B, Grate] { type X = Xo },
    ): T =
      val (d, _) = xd
      // The rebuild closures below are placeholders — the paired-encoding caveat. `.modify` only
      // consults the focus half, which `grateFunctor` has already updated to `d`.
      val b: B = inner.from((d, (_: Xi) => d))
      outer.from((b, (_: Xo) => b))

  // ------------------------------------------------------------------
  // Factories (Units 2 & 3)
  // ------------------------------------------------------------------

  /** Generic Grate factory — any `Representable[F]` container `F[_]` with element type `A` yields a
    * `Grate` over `F[A]` with focus `A`. `Representable[F]` gives a bijection
    * `F[A] ↔ (Representation => A)`, which is exactly the structure a classical
    * `Distributive`-Grate `\h -> tabulate (\i -> h (flip index i))` relies on.
    *
    * **Why `Representable`, not `Distributive`.** The plan's R4 speaks of `Distributive[F]`; its
    * Risk 3 anticipates narrowing to `Representable` if position-aware `.modify` turns out to need
    * `index` / `tabulate`. That risk fired: pure `cats.Distributive` gives `distribute` and
    * `cosequence` but no way to read a single slot (`F[A] => A`) or materialise per-position values
    * (`(R => A) => F[A]`), both of which the paired encoding's per-slot `.modify` needs. Every
    * `Representable` is `Distributive` (cats ships `Representable.distributive` as a derivation),
    * so every `Distributive` instance a user cares about in practice (Function1, Pair, Tuple2K, …)
    * is reachable via its `Representable` witness. The plan's `Grate.apply[F: Distributive]`
    * signature narrows to `Grate.apply[F: Representable]` here — documented as the settled Risk 3
    * resolution.
    *
    * Encoding: `X = F.Representation`. The rebuild slot is `R => A` — a per-index read of the
    * current container. On `to(fa)`, we snapshot the index function and return `(index(fa)(repr0),
    * index(fa))` where `repr0` is threaded through the focus for law-compat (see G3 in the law
    * spec). On `from((b, k))`, we materialise the new container as `tabulate(k)` — the rebuild
    * closure already embodies the per-slot modification produced by [[grateFunctor]]'s `.map`.
    *
    * After `.modify(f)`, this composes cleanly:
    *   1. `to(fa) = (index(fa)(repr0), index(fa))`
    *   2. `FF.map(_, f) = (f(a), index(fa) andThen f)`
    *   3. `from((_, k andThen f)) = tabulate(r => f(index(fa)(r))) = F.map(fa)(f)`
    * which is exactly the Functor-map over the representable container. G1 `modifyIdentity` and G2
    * `composeModify` reduce to the standard Functor laws.
    *
    * `repr0` — the representative index threaded through the focus — is obtained by running `index`
    * on a "fresh" container built by `tabulate(identity)`; any fixed `R` satisfies the consistency
    * invariant `k(repr0) == a`.
    *
    * @group Factories
    * @example
    *   {{{
    * import cats.instances.function.given   // Representable[Function1[Boolean, *]]
    * import eo.data.Grate
    * import eo.data.Grate.given
    * import eo.optics.Optic.*
    *
    * val g = Grate[[a] =>> Boolean => a, Int]
    * val f: Boolean => Int = b => if b then 1 else 2
    * val doubled = g.modify(_ * 2)(f)
    * doubled(true)  // 2
    * doubled(false) // 4
    *   }}}
    */
  def apply[F[_], A](using F: Representable[F]): Optic[F[A], F[A], A, A, Grate] =
    new Optic[F[A], F[A], A, A, Grate]:
      type X = F.Representation
      val to: F[A] => (A, X => A) = fa =>
        val read: X => A = F.index(fa)
        // `a` is the canonical focus value; we have no way to synthesise a concrete Representation
        // in-house (Representable doesn't expose a "canonical" index, and creating one from
        // `tabulate(identity).index` is circular). We stash a null sentinel — `.modify` /
        // `.replace` / same-carrier `.andThen` all ignore this field (only the rebuild drives
        // `from`). Users who need a well-defined focus should prefer [[Grate.at]] with an
        // explicit representative index.
        (null.asInstanceOf[A], read)
      val from: ((A, X => A)) => F[A] = { case (_, k) => F.tabulate(k) }

  /** Representable-indexed Grate factory, parameterised by a representative index `repr0`. Use this
    * when downstream code (laws, extensions) needs the focus `a` returned by `.to` to have a
    * well-defined value — e.g. a `pureRebuild`-style consistency check `k(repr0) == a`.
    *
    * @group Factories
    */
  def at[F[_], A](F: Representable[F])(repr0: F.Representation): Optic[F[A], F[A], A, A, Grate] =
    new Optic[F[A], F[A], A, A, Grate]:
      type X = F.Representation
      val to: F[A] => (A, X => A) = fa =>
        val read: X => A = F.index(fa)
        (read(repr0), read)
      val from: ((A, X => A)) => F[A] = { case (_, k) => F.tabulate(k) }
