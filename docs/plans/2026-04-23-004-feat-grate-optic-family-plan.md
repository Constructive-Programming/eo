---
title: "feat: Grate optic family — dual of Lens for Distributive / Naperian containers"
type: feat
status: active
date: 2026-04-23
---

# feat: Grate optic family — dual of Lens for `Distributive` containers

## Overview

Add the **Grate** optic family to `cats-eo`. Grate is the dual of
Lens: where Lens decomposes a product `S` into a focus `A` alongside
a leftover, Grate lifts a function `S => A` through a *zippable /
distributive* shape to produce a `T`. Classical shape
`((S → A) → B) → T`, profunctor constraint `Closed`, typeclass
substrate `cats.Distributive[F]`.

Targets shapes where every position of a container holds a value of
the same type and the container shape is fixed at compile time —
homogeneous tuples, function-shaped records, finite-key maps,
fixed-length numeric vectors. Canonical use: apply `A => B` uniformly
to every slot and get a `T`. Traversal handles multi-focus rewrites;
Grate is the read-through-structure variant.

This plan scopes a single landing: (1) carrier
`core/src/main/scala/eo/data/Grate.scala`; (2) capability typeclass
instances (`ForgetfulFunctor`, `AssociativeFunctor`) unlocking
`.modify` and same-carrier `.andThen`; (3) factory constructors —
generic `apply[F: Distributive]` plus one HList-polymorphic tuple
constructor (fallback: arities 2–4); (4) `Composer[Forgetful, Grate]`
bridge; (5) `GrateLaws` + `GrateTests` wired under two fixtures;
(6) EO-only JMH bench (Monocle ships no Grate); (7) docs updates in
`optics.md` + `concepts.md`.

Kaleidoscope (the follow-up family grouped with Grate in the
optic-families survey) is **out of scope** — see *Future
Considerations*.

## Problem Frame

### Where Grate fits in the existing library

cats-eo's current carriers all *decompose* the source and rebuild
from the pieces — product (Lens, Iso), sum (Prism, Optional), walk
(Traversal, Fold, Setter), classifier (AlgLens). Grate does something
structurally different: it **lifts** a source-consuming function
through a representable / distributive shape. That shape is witnessed
by `cats.Distributive[F]` and not reachable from any of today's
carriers.

The mainstream ask: "I have a 3-wide homogeneous tuple of `Double`s,
apply `f: Double => Double` uniformly to every slot." Today a user
reaches for `Traversal.each[List, Double]` after materialising a
list, hand-rolls three Lens calls, or drops out of the optic algebra.
Grate gives this shape a name and, because the structure is *known
finite* at the type level, delivers it via one distribute call.

### Earns-its-keep subsection — "can't you just use Traversal?"

The fair question, paralleling the AlgLens/PowerSeries analysis, is
"why not just use `Traversal.each`?"

Traversal and Grate look similar on `.modify(f)` — both apply `f`
uniformly — but they differ in the **universal** each offers beyond
`.modify`:

- **Traversal's universal** is `traverse[G: Applicative]` — an
  effectful element-by-element walk. Gives `.modifyA`, `.foldMap`,
  `.all`. Does **not** give `((S → A) → B) → T`: threading the whole
  `S` through a function that reads just the focus and reassembles
  `T` via a single distribute.
- **Grate's universal** is `((S → A) → B) → T` — the cotraversable
  shape, requiring `Distributive[F]` (strictly stronger than
  `Traverse`; one fixed shape, not a walk). Operations Grate unlocks
  that Traversal does not:
  1. **`zipWithF`** — pointwise zip of two `F[A]`s through a two-arg
     function in one distribute call (Haskell `zipWithOf`). Traversal
     walks one source linearly; two sources do not zip through it.
  2. **`collect`** — lift a K-indexed family of rebuilds through a
     Naperian `F` to produce `T`. Traversal has no K-indexed lookup.
  3. **Pointed `const b` rebuild** via Grate's universal; awkward
     through Traversal.

The 3-tuple case is expressible via `FixedTraversal[3]`, but that
loses the direct `Distributive` framing and loses `zipWithF`. Grate
bakes Distributive in from the start — a separate family rather than
an overload of Traversal.

v1 recommendation (documented in `optics.md` + `concepts.md`):

- **Container + downstream optic composition** — Traversal.
- **Fixed-shape homogeneous tuple / function-shaped record / any
  representable container, especially with `zipWith` / `collect`** —
  Grate.

Grate's v1 does not chase perf parity with Traversal. The ergonomic
win is access to the `Distributive` universal.

## Requirements Trace

- **R1. Carrier exists** in `core/src/main/scala/eo/data/Grate.scala`,
  slotted into `Optic[S, T, A, B, Grate]`.
- **R2. Grate-carrier optics support the standard capability
  extensions** — `.modify` / `.replace` via `ForgetfulFunctor[Grate]`,
  same-carrier `.andThen` via `AssociativeFunctor[Grate, Xo, Xi]`,
  plus Grate-specific `zipWithF` and (optionally) `collect` as
  extensions.
- **R3. Carrier encoding decided** — "paired"
  `GrateF[X, A] = (A, X => A)` wins over "continuation"
  `(X => A) => A`; see *Key Technical Decisions*.
- **R4. Generic `Grate.apply[F[_]: Distributive, A]` constructor**
  ships — one entry point for any Distributive `F` (Function1,
  Tuple2K, user-defined).
- **R5. Concrete N-ary homogeneous-tuple constructor
  `Grate.tuple[T <: Tuple, A]`** — single declaration via
  `Tuple.IsMappedBy` / `Tuple.Size`, fallback to hand-written
  arities 2–4 if HList doesn't compile.
- **R6. At least one Composer bridge** — `Composer[Forgetful, Grate]`
  (Iso → Grate). `Composer[Tuple2, Grate]` does NOT ship (see D3).
- **R7. `GrateLaws` + `GrateTests` pair** in `laws/`, mirroring
  `OptionalLaws` / `OptionalTests`. Fixtures: `Tuple2[Int, Int]` +
  `Tuple3[Int, Int, Int]` wired in `OpticsLawsSpec`.
- **R8. `GrateBench` JMH class** in `benchmarks/`, EO-only (Monocle
  3.3.0 ships no Grate — verified via `cellar search-external
  dev.optics:monocle-core_3:3.3.0 Grate` returning no results).
  Fixture: `Tuple3[Double, Double, Double]`, `.modify(_ * 2)`.
- **R9. Docs** — new section in `site/docs/optics.md` between Lens
  and Prism, new carriers-table row in `site/docs/concepts.md`,
  both mdoc-verified.
- **R10. Existing behaviour preserved** — no regression in any
  existing optic family's laws or benchmarks.

## Scope Boundaries

**In scope.** `core/src/main/scala/eo/data/Grate.scala`;
`laws/src/main/scala/eo/laws/GrateLaws.scala` +
`laws/src/main/scala/eo/laws/discipline/GrateTests.scala`;
`benchmarks/src/main/scala/eo/bench/GrateBench.scala`;
`site/docs/optics.md` + `site/docs/concepts.md`; `checkAll`
wirings in `tests/src/test/scala/eo/OpticsLawsSpec.scala`.

**Out of scope — explicit non-goals:**

- **Kaleidoscope** — follow-up family. Its `Reflector` typeclass is
  not in cats; cats-eo will own it at that landing. Not here.
- **No new typeclass.** `cats.Distributive[F]` is the only substrate.
- **Function-typed Grate** (`Grate[K => V, V]` over finite `K`) —
  needs `Finite[K]` / `Cardinality[K]` we don't ship. Deferred.
- **`Grate[Map[K, V], V]` over fixed key set** — needs `Keyed[F, K]`.
  Deferred.
- **`Composer[Tuple2, Grate]`** (Lens → Grate) — mathematically
  restricted; would ship as a scoped witness. Deferred.
- **Cross-carrier bridges to AlgLens / PowerSeries / Affine / SetterF**
  — structurally disjoint. Deferred.
- **Auto-derivation macro** `dev.constructive.eo.generics.grate[...]` — separate landing.
- **Indexed Grate** (`IxGrate`) — part of the parallel indexed hierarchy.

## Context & Research

### Optic-families survey as starting spec

`docs/research/2026-04-19-optic-families-survey.md` (Grate section,
lines 78–102). Commitments this plan inherits: shape
`((S → A) → B) → T`; substrate `cats.Distributive[F]`; carrier
encoding "roughly `(A, X => A)` or the classic `(X => A) => A`"
(this plan picks paired); bridge picture "Iso → Grate exists; Lens →
Grate doesn't in general"; demand "theoretically elegant,
practically niche" — priority #5 in the survey.

### AlgLens / PowerSeries precedent

`docs/research/2026-04-22-alglens-vs-powerseries.md` set the
"earns its keep" precedent every new carrier must answer. For Grate
the answer is the `zipWithF` / `collect` universal that Traversal
doesn't provide (see *Problem Frame*). Unlike AlgLens, Grate is
**not** on a direct perf collision with an existing carrier — the
Unit 6 bench documents cost envelope, not a side-by-side story.

### Cited sources for law statements

- **Haskell `optics` — `Optics.Grate`**
  (<https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-Grate.html>).
- **Clarke, Elkins, Gibbons, Loregian, Milewski, Pillmore, Román —
  *Profunctor Optics: a Categorical Update*** (arXiv:2001.07488).
- **Pickering, Gibbons, Wu — *Profunctor Optics: Modular Data Accessors***.
- **Bartosz Milewski — *Profunctor Optics: The Categorical View***.

### Relevant existing carriers — what to model Grate on

- **`Affine`** (`core/src/main/scala/eo/data/Affine.scala`) — sealed
  trait + final-class variants, match-type existential slots.
  Closest analogue for a hand-coded non-trivial carrier; Grate v1
  stops short of this (plain pair is enough).
- **`AlgLens`** — the most recently added carrier
  (`type AlgLens[F] = [X, A] =>> (X, F[A])`, given instances for
  `ForgetfulFunctor`/`ForgetfulFold`/`ForgetfulTraverse`/
  `AssociativeFunctor`, bridges `forget2alg`/`tuple2alg`/`either2alg`,
  factories `fromLensF`/`fromPrismF`/`fromOptionalF`). **Grate's
  structure in this plan follows the same template:** type alias +
  given instances + factory constructors + Composer bridges, all in
  `dev.constructive.eo.data.Grate`.
- **`PowerSeries`** — reference for a high-optimisation carrier with
  fused composition paths. Not a v1 Grate target; Grate is niche and
  the optimisation budget is better spent elsewhere.

### Existing typeclasses Grate plugs into

`ForgetfulFunctor[F]` (unlocks `.modify`/`.replace`) — **yes**.
`AssociativeFunctor[F, Xo, Xi]` (same-carrier `.andThen`) — **yes**.
`ForgetfulFold[F]` (multi-focus `.foldMap`) — **no** for v1 (Grate
is not a multi-focus fold). `ForgetfulTraverse[F, Applicative]`
(`.modifyA` / `.all`) — audit during Unit 1, likely no.
`Composer[F, G]` (bridges) — only `Composer[Forgetful, Grate]` in v1.

### The `Optic` trait and Grate's `X`

Each carrier presents as `F[_, _]` with `to: S => F[X, A]` and
`from: F[X, B] => T`. For Grate, `X = S` at every constructor site
— the rebuild `X => A` is `s => getFocus(s)`. When exposed through
`Optic[…, Grate]` with an abstract `X`, the rebuild slot becomes
opaque (same pattern as `Affine`'s `X`).

## Key Technical Decisions

### D1. Carrier encoding — **paired** over continuation

**Decision.** Use the **paired** encoding
`type Grate[X, A] = (A, X => A)` — the focus `a: A` alongside a
rebuild function `k: X => A` that re-reads the focus from a
reassembled `X` (which is always `S` at the constructor site).

**Rationale.**

- **Pair shapes fit existing machinery.** Every leftover-carrying
  carrier cats-eo ships (`Tuple2`, `Affine`, `AlgLens[F]`, `SetterF`,
  `PowerSeries`) is pair-shaped at the top level. A paired Grate
  slots in trivially:
  - `ForgetfulFunctor[Grate].map((a, k), f) = (f(a), k andThen f)`.
  - `AssociativeFunctor[Grate, Xo, Xi]` composes rebuild slots with
    `Z = (Xo, Xi)` (sketch in HLTD below).
  - Dual framing: Lens stores `(X, A)`, Grate stores `(A, X => A)` —
    neighbouring in the Strong/Closed profunctor lattice.

- **Continuation encoding `(X => A) => A` is closer to the textbook
  `((S → A) → B) → T` shape but awkward here.** `ForgetfulFunctor`
  wants plain `F[X, A] => (A => B) => F[X, B]`, which for the
  continuation encoding means threading through a nested HOF, and
  `AssociativeFunctor` would need a `Closed`-profunctor analogue we
  don't ship. Existential-carrier cats-eo is dual / carrier-first,
  not textbook profunctor; paired is the aligned choice.

- **Allocation cost.** One `X => A` closure allocation per `.to`
  call — same order as `Tuple2`'s `(s, get(s))` in `GetReplaceLens`.
  Acceptable for v1; a fused `DistributiveGrate[F, A]` subclass can
  ship later if needed.

**Consequence.** `X = S` at every constructor site; `X` is abstract
through the `Optic` trait — same pattern as `Affine`.

**Caveat.** The paired `from: (B, X => B) => T` ignores the rebuild
slot and uses only `B` — once the modify happened, the source is
gone. The rebuild is load-bearing for `zipWithF` / `collect` and for
same-carrier composition, not for `.modify`.

### D2. Factory constructor scope — generic Distributive + typeclass-polymorphic tuple

**Decision.** Ship **two** factories in v1:

1. **Generic** — `Grate.apply[F[_]: Distributive, A]: Grate[F[A], A]`.
   Any distributive container. This is the constructor a user reaches
   for when they have a `cats.Distributive` instance in scope.

2. **Concrete N-ary homogeneous tuple** — attempted as
   `Grate.tuple[T <: Tuple, A]` using Scala 3's new `Tuple.Map` /
   `Tuple.IsMappedBy` / `Tuple.Size` machinery. Goal: **one**
   declaration that constructs a `Grate[T, A]` whenever `T` is a
   tuple of `A`s, regardless of arity.

   Sketch of the shape the macro-free constructor wants to have
   (pseudocode; real implementation lives in Unit 3):

   ```
   def tuple[T <: Tuple, A](using
     im: Tuple.IsMappedBy[[a] =>> A][T],
     ts: ValueOf[Tuple.Size[T]]
   ): Grate[T, A] = …
   ```

   The `to` function is straightforward — read slot `0` (any slot
   suffices; all hold `A`) — and the rebuild `T => A` can look up
   any-slot via `.productElement` with an index cast. The `from` is
   the piece of real work: given `B`, produce a `T` of the same shape
   filled with the single `B` value. Implementation via
   `Tuple.fromArray` over an `Array[AnyRef]` filled with the `B`.

   **Feasibility note and explicit fallback.** The HList machinery
   *should* be enough — `Tuple.IsMappedBy[F][T]` guarantees `T <:<
   Tuple.Map[F, Inverse]` and `Tuple.Size` exposes the arity as a
   value — but the construction `T => (A, T => A)` + `B => T` sits in
   a corner of Scala 3's type system (the "unmap" direction of
   `Tuple.Map` is inverse-constrained) that is not well-exercised.
   If Unit 3 discovers the single-declaration shape does not
   type-check cleanly, **fall back** to hand-written arities 2, 3, 4
   (`Grate.tuple2`, `Grate.tuple3`, `Grate.tuple4`) and flag the
   fallback in *Open Questions*. A separate future plan could revisit
   with a macro-based constructor.

**Why no other factories in v1.** `Function1` Grates
(`Grate[K => V, V]` for finite `K`) are the other common concrete
shape, but they need a `Finite[K]` witness we don't ship. Deferred.
`Map[K, V]` over a fixed key set needs a `Keyed[F, K]` witness,
also deferred.

### D3. Composer bridge scope — Iso → Grate only

**Decision.** Ship **`Composer[Forgetful, Grate]`** (Iso → Grate).
Defer every other bridge.

- **`Composer[Forgetful, Grate]` — trivial, ships.** Same pattern as
  `Composer[Forgetful, Tuple2]` / `Composer[Forgetful, Either]`.
- **`Composer[Tuple2, Grate]` (Lens → Grate) — does NOT ship.**
  `Distributive[F]` is strictly stronger than `Strong`; a Lens's
  outer `S` need not be distributive (most aren't). The composition
  only makes sense when `S` is already distributive — at which point
  the user should reach for `Grate.apply[F]` directly. **Workaround
  documented in Scaladoc**: "construct the Grate separately at the
  Lens's focus type." A scoped witness requiring `Distributive[S]`
  evidence is possible but the separate-construction path is cleaner.
- **`Composer[Grate, Forgetful]` (Grate → Iso) — No.** Iso needs a
  full bijection; a Grate over a distributive container doesn't
  bijectively map to a single focus.
- **AlgLens / PowerSeries / Affine / SetterF bridges — None in v1.**
  `AlgLens[F]`'s F is a classifier, not a shape; structurally
  disjoint from Grate's Naperian F. Grate → Traversal (when F is
  both Distributive and Traverse) is sound but niche — flagged as
  Future Considerations.

### D4. Given instances — what the carrier ships

| Typeclass | Ships? | Justification |
|---|---|---|
| `ForgetfulFunctor[Grate]` | **Yes** | One-liner; unlocks `.modify` / `.replace`. Required for R2. |
| `AssociativeFunctor[Grate, Xo, Xi]` | **Yes** | Same-carrier `.andThen`. `Z = (Xo, Xi)`. Required for R2. |
| `ForgetfulFold[Grate]` | **No** | Grate is not a multi-focus fold. Paired-encoding focus is one `A`, not the N positions of the container; `.foldMap` would be misleading. `Traversal` is the right tool for multi-focus fold. |
| `ForgetfulTraverse[Grate, Applicative]` | **Audit, likely No** | Same reasoning; duplicates `Traversal.each`. Audit during Unit 1, ship only if a clean shape falls out. |
| `Accessor[Grate]` | **No** | No plain `.get` — classical Grate interface doesn't include it. |
| `ReverseAccessor[Grate]` | **No** | Would need a `Distributive.pure`-like op. |
| `ForgetfulApplicative[Grate]` | **No** | Grate's F is `Distributive`, not `Applicative`. |

Plus **Grate-specific operations** as extensions on
`Optic[S, T, A, B, Grate]`:

- `zipWithF(other: S)(f: (A, A) => B): T` — pointwise zip via
  `Distributive`.
- `collect(k: S => B): T` — lift `S => B` through the distribute
  structure.

(Exact signatures decided in Unit 1 alongside the carrier; may live
in a `GrateOps` extension class to keep `Optic`'s companion scoped.)

### D5. Laws — the Grate round-trip equations

**Decision.** Port from Haskell `optics` (`Optics.Grate`) and the
Clarke et al. categorical formulation:

- **G1 `modifyIdentity`** — `grate.modify(identity)(s) == s` (shared
  shape with every other family).
- **G2 `composeModify`** — `grate.modify(g)(grate.modify(f)(s)) ==
  grate.modify(f andThen g)(s)` (shared shape).
- **G3 `pureRebuild` (Grate-specific)** — given `(a, k) = grate.to(s)`,
  `k(s) == a`. Consistency check between the focus and the rebuild.

Wire into `GrateLaws[S, A]` following `OptionalLaws`; discipline
RuleSet in `GrateTests` with one `forAll` per law.

**Fixtures.** `Tuple2[Int, Int]` and `Tuple3[Int, Int, Int]` at
minimum; a `Function1` fixture (e.g. `Grate[Boolean => Int, Int]`)
is a stretch goal contingent on ScalaCheck `Cogen`/`Arbitrary`
availability.

### D6. Bench scope — EO-only, Tuple3[Double]

- **Fixture**: `Tuple3[Double, Double, Double]`.
- **Grate**: `Grate.tuple[Tuple3[Double, Double, Double], Double]`
  (or arity-3 fallback).
- **Operation**: `.modify(_ * 2)`.
- **EO-only** — Monocle 3.3.0 ships no Grate (verified via
  `cellar search-external dev.optics:monocle-core_3:3.3.0 Grate`).
- **Baselines**: plain `t => (t._1 * 2, t._2 * 2, t._3 * 2)` (the
  zero-overhead floor); optional `FixedTraversal[3]` reference.

Documents the cost envelope, not a "Grate is faster than" story.
JMH annotations mirror `AlgLensBench.scala` (`@Fork(3)`,
`@Warmup(iterations = 3)`, etc.).

### D7. Docs — placement and cookbook scope

- **Placement in `optics.md`**: between Lens and Prism. Grate is the
  direct dual of Lens, so adjacency helps the product/co-product
  mental model. Rejected placement after Traversal — would
  misleadingly cluster Grate with multi-focus optics.
- **`concepts.md` carriers table**: one row below `FixedTraversal[N]`:
  `Grate | (A, X => A) | Grate`.
- **Cookbook entry — NO for v1.** Grate is niche enough that a
  cookbook entry would be speculative; deferred to Future Considerations.

## Open Questions

Each of these is live when the plan is approved; resolve before or
during the named Unit.

1. **Is the paired encoding decision robust, or should Unit 1 spike
   both encodings?** The decision above is strongly argued but not
   battle-tested in cats-eo. If Unit 1 discovers that the paired
   encoding's `ForgetfulFunctor` / `AssociativeFunctor` instances run
   into match-type / existential-reduction walls (the kind that hit
   `Affine`'s `assoc` given and forced the `X <: Tuple` removal), a
   one-day spike of the continuation encoding is warranted before
   committing. Spike budget: 1 day; if inconclusive, pick paired and
   document the pivot.

2. **Does the typeclass-polymorphic `Grate.tuple[T]` constructor
   type-check cleanly?** The HList machinery is new in Scala 3.8.x
   and not heavily exercised for the "unmap" direction of
   `Tuple.Map`. Resolution — run a short compile-check spike in Unit 3
   before writing the real constructor; if it doesn't work, **fall
   back** to arities 2–4 and re-scope R5.

3. **Should `ForgetfulTraverse[Grate, Applicative]` ship?** The D4
   table says "audit during Unit 1, likely no". If a clean shape
   falls out — `(a, k).aTraverse(f) = f(a).map(b => (b, _ => b))` —
   ship it and unlock `.modifyA` / `.all`. Otherwise defer.

4. **Discipline-generated `Arbitrary[Tuple3[Int, Int, Int]]`** — does
   ScalaCheck ship a default? (It does; `Arbitrary.arbTuple3` is
   available via `org.scalacheck.Arbitrary`.) Confirm during Unit 5 to
   avoid fixture plumbing.

5. **What does `.modify(_ => throw)(s)` do on a `Grate`?** Short
   answer: the lambda is called once inside the paired rebuild, so
   the exception propagates. Confirm that this matches the
   documented behaviour for other carriers (Lens throws through the
   same shape). One-line test in Unit 5; no plan change expected.

6. **Does `Composer.chain` transitive derivation work with
   `Composer[Forgetful, Grate]` alone?** If a user writes
   `iso.andThen(lens).andThen(grate)`, Scala needs to find
   `Composer[Tuple2, Grate]` to get Lens → Grate. Per D3 we do not
   ship that. The `.andThen` chain will fail to compile; the
   resulting error will be an implicit-resolution miss for
   `Morph[Tuple2, Grate]`. Document this explicitly in the Grate
   section of `optics.md` with the workaround (construct the Grate at
   the outer focus type directly, then use `iso.andThen(lens)` to
   reach that focus and apply the Grate separately).

## High-Level Technical Design

### Carrier + companion shape

`core/src/main/scala/eo/data/Grate.scala` ships the type alias plus
companion:

```
type Grate[X, A] = (A, X => A)

object Grate:
  given ForgetfulFunctor[Grate]                     = …
  given [Xo, Xi]: AssociativeFunctor[Grate, Xo, Xi] = …
  given Composer[Forgetful, Grate]                  = …

  def apply[F[_]: Distributive, A]: Optic[F[A], F[A], A, A, Grate] = …
  def tuple[T <: Tuple, A](using Tuple.IsMappedBy[[a] =>> A][T]): Optic[T, T, A, A, Grate] = …

  extension [S, T, A, B](o: Optic[S, T, A, B, Grate])
    def zipWithF(other: S)(f: (A, A) => B): T = …
    def collect(k: S => B): T = …
```

Units 1–4 fill in each body.

### Given instance sketches

**`ForgetfulFunctor[Grate]`** — one-liner:
`map((a, k), f) = (f(a), k andThen f)`.

**`AssociativeFunctor[Grate, Xo, Xi]`** — `Z = (Xo, Xi)`.
`composeTo(s, outer, inner)` reads `(a, kO) = outer.to(s)` and
`(c, kI) = inner.to(a)`, returning `(c, (z: Z) => kI(z._2))`.
`composeFrom((d, _), inner, outer)` — for the `.modify` path the
rebuild slot is ignored; threads `b = inner.from((d, _ => d))` then
`outer.from((b, _ => b))`. The rebuild slots are load-bearing only
for the `zipWithF` / `collect` extensions, flushed out in Unit 1.

**`Composer[Forgetful, Grate]`** (Iso → Grate): `type X = S`;
`to = s => (iso.to(s), s0 => iso.to(s0))`;
`from = { case (b, _) => iso.from(b) }`.

**`Grate.apply[F: Distributive, A]`** (generic factory): `type X = F[A]`;
`to` reads an element via `Distributive.map` + pick-any-index (fallback
to `Representable` if needed); `from` broadcasts via
`Distributive.distribute`. Unit 2 finalises the exact spelling.

**`Grate.tuple[T <: Tuple, A]`** (HList-polymorphic): `to` uses
`.productElement(0).asInstanceOf[A]`; `from` builds via
`Tuple.fromArray(Array.fill(sz.value)(b.asInstanceOf[AnyRef]))
.asInstanceOf[T]`. Fallback to hand-written `tuple2`/`tuple3`/`tuple4`
per Unit 3.

### Law class sketch

```
trait GrateLaws[S, A]:
  def grate: Optic[S, S, A, A, Grate]

  def modifyIdentity(s: S): Boolean =
    grate.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    grate.modify(g)(grate.modify(f)(s)) ==
      grate.modify(f.andThen(g))(s)

  def pureRebuild(s: S): Boolean =
    val (a, k) = grate.to(s)
    k(s) == a
```

## Implementation Units

Each unit is ~1 commit. Checkboxes track status.

### Unit 1 — Carrier + `ForgetfulFunctor` + `AssociativeFunctor`

- [x] Land carrier + core typeclass instances.

**Files.** Create `core/src/main/scala/eo/data/Grate.scala`.

**Approach.**
1. Define `type Grate[X, A] = (A, X => A)` in `dev.constructive.eo.data`.
2. `given grateFunctor: ForgetfulFunctor[Grate]` — one-liner.
3. `given grateAssoc[Xo, Xi]: AssociativeFunctor[Grate, Xo, Xi]`
   with `Z = (Xo, Xi)`. `composeFrom` uses the focus half only;
   rebuild slot is load-bearing for `zipWithF`/`collect`, not for
   `.modify`.
4. If the D1 encoding decision hasn't been spiked by this point,
   spike now (budget: 1 day — see *Open Questions*).
5. Scaladoc density matching `AlgLens.scala`.

**Verification.** `sbt core/compile` green; `sbt core/test` green
if existing core-module tests hit the new given.

### Unit 2 — Generic `Grate.apply[F[_]: Distributive, A]`

- [x] Land the generic constructor.

**Files.** Modify `core/src/main/scala/eo/data/Grate.scala`.

**Approach.**
1. Use `Distributive.distribute` to broadcast the rebuild through
   the `F` shape; use `Distributive.map` + pick-any-index helper for
   `to`. If a `Representable[F, R]` witness is available, prefer it
   for the index pick; fall back to `distribute` if not.
2. Smoke test: `Grate.apply[Function[Unit, *], Int]`,
   `grate.modify(_ * 2)(u => 3)(())` evaluates to `6` (Function1
   ships a Distributive instance — verified in *Context & Research*).

**Verification.** `sbt core/compile` green; round-trip spec passes.

### Unit 3 — `Grate.tuple[T <: Tuple, A]` (with fallback)

- [x] Land the concrete N-ary homogeneous-tuple constructor (one
  declaration) or arity 2–4 fallbacks.

**Files.** Modify `core/src/main/scala/eo/data/Grate.scala`.

**Approach.**
1. Spike the single-declaration form with `Tuple.IsMappedBy` +
   `Tuple.Size`. Body: `to` reads `.productElement(0).asInstanceOf[A]`;
   `from` builds the tuple via
   `Tuple.fromArray(Array.fill(sz.value)(b.asInstanceOf[AnyRef]))
   .asInstanceOf[T]`.
2. If it compiles, ship. If not (expected failure mode:
   `Tuple.Size[T]` / `IsMappedBy` not reducing cleanly for abstract
   T), fall back to hand-written `tuple2`/`tuple3`/`tuple4` and
   document in a comment block + in this plan's *Open Questions*.
3. Tuple1 is too trivial (use `Iso[A, Tuple1[A]]`); arities ≥ 5 get
   diminishing returns.

**Verification.** `sbt core/compile` green; spec:
`Grate.tuple3[Int].modify(_ + 1)((1, 2, 3)) == (2, 3, 4)`.

### Unit 4 — Composer bridge (`Forgetful → Grate`)

- [x] Land `given forgetful2grate: Composer[Forgetful, Grate]`.

**Files.** Modify `core/src/main/scala/eo/data/Grate.scala`.

**Approach.** Implementation per D3 sketch. Add a Scaladoc note on
the companion object about the missing `Tuple2 → Grate` direction
and the documented workaround.

**Verification.** `sbt core/compile` green; cross-carrier compose
spec: Iso `.andThen` Grate produces a Grate-carrier optic whose
`.modify` behaves as the composed function.

### Unit 5 — Laws (`GrateLaws` + `GrateTests` + fixtures)

- [x] Land the law class, discipline RuleSet, and at least two
  `checkAll` wirings.

**Files.**
- Create `laws/src/main/scala/eo/laws/GrateLaws.scala`.
- Create `laws/src/main/scala/eo/laws/discipline/GrateTests.scala`.
- Modify `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add
  `checkAll(..., GrateTests[(Int, Int), Int].grate)` and
  `checkAll(..., GrateTests[(Int, Int, Int), Int].grate)`.

**Approach.** `GrateLaws` per D5 sketch; `GrateTests` per the
`OptionalTests` template (three `forAll` entries). Stretch: add a
`Grate[Unit => Int, Int]` fixture if `Cogen`/`Arbitrary` plumbing
falls out cleanly.

**Verification.** `sbt laws/compile` + `sbt tests/test` green;
scoverage shows `Grate.scala` exercised on `ForgetfulFunctor` +
`AssociativeFunctor` paths.

### Unit 6 — Benchmark (`GrateBench` — EO-only)

- [x] Land JMH bench.

**Files.** Create `benchmarks/src/main/scala/eo/bench/GrateBench.scala`.

**Approach.** Bench `eoModify_grate` (`grate.modify(_ * 2)`) and
`naive_tupleRewrite` (`t => (t._1 * 2, …)`); optional
`eoModify_traversal` if `FixedTraversal[3]` is plausible. JMH
annotations mirror `AlgLensBench.scala` exactly.

**Verification.** `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1
.*GrateBench.*"` smoke runs to completion. Commit message records
observed numbers inline; no separate results doc (AlgLens-vs-PowerSeries's
separate-file research doc is overkill for Grate's first bench).

### Unit 7 — Docs (`optics.md` section + `concepts.md` carriers row)

- [x] Land user-facing docs.

**Files.** Modify `site/docs/optics.md` (new Grate section between
Lens and Prism); modify `site/docs/concepts.md` (carriers table row).

**Approach.** Content per the Documentation Plan below.

**Verification.** `sbt docs/mdoc` + `sbt docs/laikaSite` green;
pre-commit hook passes.

## Risks & Dependencies

### Risks

- **Risk 1. Paired encoding hits a match-type wall in
  `AssociativeFunctor[Grate, Xo, Xi]`.** Threading `(Xo, Xi)`
  through nested rebuild projections is the same shape that forced
  `Affine`'s `assoc` given to drop its `X <: Tuple` bound. Mitigation:
  the Unit 1 spike is a gate; if paired can't reduce cleanly,
  re-evaluate continuation encoding (pays its own `Closed`-profunctor
  cost).
- **Risk 2. `Tuple.IsMappedBy` single-declaration form does not
  compile.** HList machinery is new; `Tuple.Map`'s "unmap" direction
  is brittle. Mitigation: arity-2/3/4 fallback documented in D2 and
  Unit 3.
- **Risk 3. `Grate.apply[F: Distributive]` round-trip needs a
  `Representable` witness in practice.** `Distributive` guarantees
  `distribute` but not a direct index — "read any element" may need
  more. Mitigation: if infeasible without `Representable`, scope down
  to `Grate.apply[F: Representable]` in Unit 2 and document.
- **Risk 4. Bench's "EO-only" framing looks weak.** Mitigation: v1
  docs don't lean on bench numbers; bench establishes a cost floor
  for future optimisation.
- **Risk 5. Hand-written arity-3 Grate's rebuild returns `(a, a, a)`
  instead of `(a0, a1, a2)`.** Classic paired-encoding pitfall;
  mitigation — **`pureRebuild`** law catches it in Unit 5.

### Dependencies

- `cats.Distributive[F]` from `cats-core_3:2.13.0` — already pinned.
- `scala.Tuple.{Map, IsMappedBy, Size}` — Scala 3.8.3 stdlib.
- `discipline-specs2_3:2.0.0` — already the test dep.
- No build.sbt / plugin changes.

## Documentation Plan

### Scaladoc on new files

Every public member under `dev.constructive.eo.data.Grate` carries Scaladoc at the
density of `AlgLens.scala`:
- Type alias — encoding note, link to this plan.
- Each `given` — what it unlocks + required evidence.
- `apply[F: Distributive, A]` — Distributive requirement + one-line
  Function1 example.
- `tuple[T <: Tuple, A]` — IsMappedBy constraint; if fallbacks ship,
  one block per arity.
- `zipWithF` / `collect` extensions — two-line examples each;
  pointer to `optics.md` for the high-level story.

### `optics.md` — new Grate section

Placement between Lens and Prism. Content:
- Shape note — `Grate[S, A]` over a `Distributive[F]`.
- Mdoc-verified `Tuple3[Double]` example:
  `Grate.tuple3[Double].modify(_ * 2)((1.0, 2.0, 3.0))` produces
  `(2.0, 4.0, 6.0)`.
- One-line `Grate.apply[F: Distributive]` Function1 example.
- "When to reach for Grate vs Traversal" sub-paragraph (2–3 sentences).
- Cross-carrier composition note: Iso → Grate works via `.andThen`;
  Lens → Grate does not in general — document the workaround.

### `concepts.md` — carriers-table row

One row: `Grate | (A, X => A) | Grate`.

### Docs scope boundaries

- No cookbook entry in v1 (deferred per D7).
- No migration-from-Monocle entry (Monocle has no Grate).
- No `extensibility.md` recipe (that page is about *adding*
  carriers, not *using* them).

## Success Metrics

A successful landing satisfies:

1. `sbt compile` green across `core`, `laws`, `tests`, `generics`.
2. `sbt test` green with at least two new `checkAll` invocations
   exercising `GrateTests` (Tuple2 + Tuple3 fixtures).
3. `sbt scalafmtCheckAll` green.
4. Scoverage on `core` does not regress — `Grate.scala` contributes
   fresh lines covered by the new law suite, so the floor should
   hold or improve.
5. `sbt docs/mdoc` green — the new Grate section in `optics.md`
   compiles.
6. `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*GrateBench.*"` runs to
   completion (smoke — not a perf gate).
7. The `git log --oneline` for this plan lands as ~7 commits, one per
   Implementation Unit, each passing the pre-commit + pre-push hooks
   in `.githooks/`.

## Phased Delivery

1. **Phase 1 — Carrier + core instances (Units 1–2).** Grate carrier
   exists and can be constructed generically; `.modify` works.
2. **Phase 2 — Concrete tuple constructor + Composer bridge
   (Units 3–4).** Tuple-based construction lands; Iso → Grate
   composition works.
3. **Phase 3 — Laws + bench (Units 5–6).** Law coverage lands; bench
   smoke-tested.
4. **Phase 4 — Docs (Unit 7).** `optics.md` + `concepts.md` updated;
   release-ready.

Phases 1–2 can ship independently behind the 0.1.x release gate (the
carrier is usable once Phase 2 lands). Phases 3 and 4 are
release-blocking gates.

## Future Considerations

### Kaleidoscope — the expected next family

Natural follow-up to Grate; see the optic-families survey's
"Kaleidoscope" section. Classifying profunctor constraint is
**`Reflector`**, which cats does **not** ship — cats-eo will own
its own `Reflector` typeclass at the Kaleidoscope landing (do
**not** introduce it here). Reflector is roughly the dual of
`Traversable`, capturing the "zipping" structure of Applicative;
alleycats and cats-mtl have similar-shaped machinery but no clean
drop-in. Use cases: column-wise aggregation over rows
(`ZipList`), cartesian products (`List`), monoid-shaped aggregation
(`Const[M, _]`). Kaleidoscope lands as its own carrier + law suite
+ bench.

### Function-typed Grate — `Grate[K => V, V]` for finite `K`

Needs a `Finite[K]` / `Cardinality[K]` witness we don't ship.
Natural follow-up once v1 demand reveals real users.

### `Grate[Map[K, V], V]` over a fixed key set

Needs a `Keyed[F, K]` witness. Deferred with the same reasoning.

### Cross-carrier bridges

- `Composer[Tuple2, Grate]` (Lens → Grate) as a scoped witness
  requiring `Distributive[S]` evidence — ergonomic win is small
  vs. separate construction; deferred.
- `Composer[Grate, Forget[F]]` for `F: Distributive + Traverse` —
  sound but speculative; deferred.
- AlgLens / PowerSeries / Affine / SetterF bridges — none are
  natural; deferred with low priority.

### Generics macro — `dev.constructive.eo.generics.grate[T]`

Hearth-based macro synthesising a Grate for homogeneous case classes
(all fields same type). Follow-up after Grate settles; reuses the
`Tuple.Map` recognition the Unit 3 spike exercises.

### Bench deepening vs `FixedTraversal[N]`

If Grate sees real adoption for numeric fixed-tuple work, deepen
`GrateBench` to a side-by-side comparison with `FixedTraversal[N]`
and capture it in a separate research doc (mirrors the
AlgLens-vs-PowerSeries precedent).

### Cookbook entry

Add once 2–3 real user patterns materialise — candidates include
uniform numeric updates over sensor-reading tuples, point-wise
delta computation, finite-key lookup-table updates.

## Sources & References

- **Optic-families survey** —
  `docs/research/2026-04-19-optic-families-survey.md`, Grate section
  (lines 78–102).
- **AlgLens vs PowerSeries research (earns-its-keep precedent)** —
  `docs/research/2026-04-22-alglens-vs-powerseries.md`.
- **AlgLens carrier (closest template)** —
  `core/src/main/scala/eo/data/AlgLens.scala`.
- **Production-readiness plan (format reference)** —
  `docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md`.
- **Iris classifier example plan (size reference)** —
  `docs/plans/2026-04-22-002-feat-iris-classifier-example.md`.
- **`cats.Distributive` typeclass** —
  <https://typelevel.org/cats/api/cats/Distributive.html>; signature
  verified via `cellar get-external
  org.typelevel:cats-core_3:2.13.0 cats.Distributive`.
- **Haskell `optics` — Optics.Grate** —
  <https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-Grate.html>;
  source of the two Grate round-trip laws ported in D5.
- **Clarke, Elkins, Gibbons, Loregian, Milewski, Pillmore, Román
  — *Profunctor Optics: a Categorical Update*** —
  <https://arxiv.org/abs/2001.07488>. Section on Closed profunctors
  and grates.
- **Pickering, Gibbons, Wu — *Profunctor Optics: Modular Data
  Accessors*** —
  <https://www.cs.ox.ac.uk/people/jeremy.gibbons/publications/poptics.pdf>.
- **Bartosz Milewski — *Profunctor Optics: The Categorical View*** —
  <https://bartoszmilewski.com/2017/07/07/profunctor-optics-the-categorical-view/>.
- **Chris Penner — *Grate: Distributive optics*** (blog post;
  accessible exposition) — linked from the survey sources.
- **Monocle 3.3.0 does NOT ship Grate** — verified via
  `cellar search-external dev.optics:monocle-core_3:3.3.0 Grate`
  returning no results; Monocle's optic surface is
  Iso/Lens/Prism/Optional/Traversal/Setter/Fold/Getter, no Grate
  family.
- **Scala 3 `Tuple.Map` / `Tuple.IsMappedBy` / `Tuple.Size`** —
  standard library, available in Scala 3.8.3.
