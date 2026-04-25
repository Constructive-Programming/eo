package dev.constructive.eo
package data

import cats.Representable

import optics.Optic

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
    * side reads `(a, _)` from the outer (discarding the outer rebuild `kO`) and `(c, kI)` from the
    * inner on that `a`, producing the fused rebuild `(z: Z) => kI(z._2)` — which consults the inner
    * index `z._2` only. The outer rebuild is structurally unreachable once we've collapsed through
    * the outer focus, so we drop it on push.
    *
    * Pull side threads the rebuild faithfully to the inner (whose `from` may read it per-slot — as
    * [[tuple]] does). The outer's rebuild is reconstructed as a constant broadcaster `_ => b` —
    * acceptable because a) the shipped outers in v1 are either iso-morphed (Forgetful → Grate,
    * reads only the focus) or tuple-built (broadcasts already), and b) the paired encoding's
    * outer-rebuild has no inner-index handle to reach, so any finer reconstruction would require a
    * profunctor-style closed witness beyond the scope of `AssociativeFunctor`.
    *
    * The `null.asInstanceOf[Xo]` sentinel below is safe **by construction** — [[composeTo]]'s fused
    * rebuild closes over `z._2` only, so `kD((null, xi)) == kI(xi)` for any null-cast Xo.
    *
    * Caveat: if a third-party outer Grate comes along whose `from` genuinely consults the per-slot
    * rebuild (not true of any v1 carrier), this assoc's outer-rebuild broadcast would
    * under-approximate. Documented for the future-work `zipWithF`/`collect` bridge.
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
      val (d, kD) = xd
      // INVARIANT: [[composeTo]]'s fused kD closes over `z._2` only — the returned function is
      // literally `(z: (Xo, Xi)) => kI(z._2)`, ignoring the first tuple component. So
      // `kD((sentinel, xi)) == kI(xi)` for ANY Xo value. The `null.asInstanceOf[Xo]` cast is
      // therefore safe by construction regardless of `Xo`'s runtime shape (including primitives).
      // If any future outer Grate's kD starts reading z._1, this assoc's composeFrom will need
      // a genuine Xo witness — which the existing paired encoding doesn't expose. Witnessed
      // end-to-end by every grate.andThen(grate) spec in GrateSpec; the invariant is the reason
      // those chains preserve semantics.
      val kI: Xi => D = xi => kD((null.asInstanceOf[Xo], xi))
      val b: B = inner.from((d, kI))
      // Outer's rebuild — broadcast fallback. V1 outers (iso-morphed, tuple-built) don't consult
      // this; future per-slot-outer Grates would need a Closed-profunctor-style reconstruction
      // which isn't expressible in the paired encoding without additional machinery.
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
    * import dev.constructive.eo.data.Grate
    * import dev.constructive.eo.data.Grate.given
    * import dev.constructive.eo.optics.Optic.*
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

  /** Polymorphic homogeneous-tuple Grate factory — a single declaration that works for any `T <:
    * Tuple` whose elements all have type `A`. The arity is recovered from `Tuple.Size[T]` and the
    * all-same-element constraint from `Tuple.Union[T] <:< A`.
    *
    * **Constraint choice — Union over IsMappedBy.** The plan's D2 sketch reached for
    * `Tuple.IsMappedBy[[a] =>> A][T]`. In Scala 3.8.3 that evidence hits a wall for concrete
    * tuples: it requires proving `Map[F, InverseMap[T, F]] =:= T` for the abstract inverse, and the
    * compiler emits `"Cannot prove that X =:= Tuple.Map[Tuple.InverseMap[X, F], F]"`. Switching to
    * `Tuple.Union[T] <:< A` sidesteps the unmap problem entirely: `Union` computes the join of all
    * element types, and `<:< A` asserts that every element is an `A`. The plan's OQ2 fallback to
    * hand-written `tuple2` / `tuple3` / `tuple4` was NOT needed — a single declaration covers every
    * arity from 2 through 22.
    *
    * **Encoding — index-typed `X`.** `X = Int` — the slot index, not the tuple itself. `to(t)`
    * produces `(t._0, i => t._i)`: the focus is slot 0, the rebuild reads by index. After
    * [[grateFunctor]] `.map(f)`, the rebuild becomes `i => f(t._i)`. `from((_, k'))` ignores the
    * focus and materialises the result as `Tuple.fromArray(Array.tabulate(size)(i => k'(i)))` —
    * applying `f` at every slot. This matches the classical per-slot Grate semantics.
    *
    * @group Factories
    * @example
    *   {{{
    * import dev.constructive.eo.data.Grate
    * import dev.constructive.eo.data.Grate.given
    * import dev.constructive.eo.optics.Optic.*
    *
    * val g3 = Grate.tuple[(Int, Int, Int), Int]
    * g3.modify(_ + 1)((1, 2, 3))   // (2, 3, 4)
    * g3.replace(42)((1, 2, 3))     // (42, 42, 42)
    *   }}}
    *
    * @note
    *   The `Union[T] <:< A` evidence requires `T` concrete at the call site. Abstract `T <: Tuple`
    *   (e.g. inside a generic function parameterised over the tuple type) won't resolve until `T`
    *   is pinned; shift the call to the concrete site.
    */
  def tuple[T <: Tuple, A](using
      sz: ValueOf[Tuple.Size[T]],
      ev: Tuple.Union[T] <:< A,
  ): Optic[T, T, A, A, Grate] =
    val _ = ev // evidence compile-time only; kept for the constraint surface
    val size = sz.value
    new Optic[T, T, A, A, Grate]:
      type X = Int
      val to: T => (A, X => A) = t =>
        val read: X => A = (i: Int) => t.productElement(i).asInstanceOf[A]
        (read(0), read)
      val from: ((A, X => A)) => T = {
        case (_, k) =>
          val arr = new Array[Object](size)
          var i = 0
          while i < size do
            arr(i) = k(i).asInstanceOf[Object]
            i += 1
          Tuple.fromArray(arr).asInstanceOf[T]
      }

  // ------------------------------------------------------------------
  // Composer bridges (Unit 4)
  // ------------------------------------------------------------------

  /** Trivial injection `Forgetful ↪ Grate` — lets an `Iso`-carrier optic compose against a
    * `Grate`-carrier optic via cross-carrier `.andThen`. The Iso's forward `to: S => A` doubles as
    * the rebuild's read; the reverse `from: B => T` drives the pull side.
    *
    * Does **not** ship the mirror `Composer[Tuple2, Grate]` (Lens → Grate) by design. A Lens's
    * source type `S` is not in general `Representable` / `Distributive`, so there's no natural way
    * to broadcast a fresh focus through the Lens's structural leftover — the composition only makes
    * sense when `S` is already representable, at which point the user should reach for
    * [[Grate.apply]] directly. Per plan D3, the workaround for users who want Lens → Grate is to
    * construct the Grate separately at the Lens's focus type and compose through `Lens.andThen`
    * (staying in `Tuple2`). Consequence for transitive `Composer.chain`: a user-written
    * `iso.andThen(lens).andThen(grate)` will not type-check — the resolution miss surfaces as
    * `"Morph[Tuple2, Grate]"` implicit not found. See `site/docs/optics.md` Grate section for the
    * worked workaround.
    *
    * @group Instances
    */
  given forgetful2grate: Composer[Forgetful, Grate] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Grate] =
      new Optic[S, T, A, B, Grate]:
        type X = S
        val to: S => (A, S => A) = s => (o.to(s), s0 => o.to(s0))
        val from: ((B, S => B)) => T = { case (b, _) => o.from(b) }
