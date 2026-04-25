---
title: "feat: Kaleidoscope optic family — Applicative-parameterised aggregation via Reflector"
type: feat
status: active
date: 2026-04-23
---

# feat: Kaleidoscope optic family — `Applicative`-parameterised aggregation

## Overview

Add the **Kaleidoscope** optic family to `cats-eo`. Kaleidoscope
(Chris Penner) is an aggregation optic whose behaviour at
composition-time is picked by the `Applicative[F]` the user plugs
in: `ZipList` produces column-wise aggregation, plain `List` produces
cartesian products, `Const[M, *]` (with `Monoid[M]`) produces
summation-shaped aggregation. Classifying profunctor constraint is
`Reflector` — strictly weaker than `Distributive` (Grate's) and
orthogonal to `Traversing` (Traversal's). Because cats does **not**
ship `Reflector`, cats-eo owns its own minimal typeclass in `core/`,
patterned after the existing capability substrate
(`ForgetfulFold` / `AssociativeFunctor` etc.).

This is the immediate follow-up to the Grate landing (plan 004).
Grate and Kaleidoscope are neighbours in the profunctor-optics
lattice — both lift aggregation structure through an `F[_]` — but
Kaleidoscope admits a **richer** choice of `F` because `Reflector`
is weaker than `Distributive`. Every `Distributive` `F` is a
`Reflector`; many Reflectors (`List`, `ZipList`, `Const[M, *]`) are
**not** Distributive.

This plan scopes a single landing: (1) `Reflector[F[_]]` typeclass in
`core/src/main/scala/eo/Reflector.scala` with instances for `List`,
`cats.data.ZipList`, and `Const[M, *]`; (2) carrier
`core/src/main/scala/eo/data/Kaleidoscope.scala`; (3) capability
instances `ForgetfulFunctor[Kaleidoscope]`,
`AssociativeFunctor[Kaleidoscope, Xo, Xi]` (unlocking `.modify` +
same-carrier `.andThen`); (4) factory constructor
`Kaleidoscope.apply[F[_]: Reflector, A]`; (5) Composer bridge
`Composer[Forgetful, Kaleidoscope]` (Iso → Kaleidoscope);
(6) `KaleidoscopeLaws` + `KaleidoscopeTests` wired under two
Applicative-distinct fixtures; (7) EO-only JMH bench (Monocle ships
no Kaleidoscope); (8) docs updates in `optics.md` + `concepts.md` +
one cookbook entry.

The Reflector instance surface is deliberately narrow — three
instances that witness the three distinct aggregation shapes
(cartesian, zipping, summation). Further instances (`Option`,
`NonEmptyList`, `Validated`, etc.) are listed in *Future
Considerations*, not v1.

## Problem Frame

### Where Kaleidoscope fits in the existing library

cats-eo's current aggregation carriers all fix the traversal
semantics at construction-time: `Traversal` uses `Traverse[F]`,
`PowerSeries` is a specialised traversal carrier, `Fold` uses
`Foldable`, `AlgLens[F]` fixes `F` as a classifier. None of them
**defer the choice of aggregation Applicative to composition-time**.

Kaleidoscope does exactly that. The focus presents as a collection
of `A`s, and at `.modify` / `.collect` time the user picks the
`Applicative[F]` that witnesses *how* the collection is aggregated.
The same Kaleidoscope value, applied to the same source, produces
different outputs depending on the `F`:

- With `Reflector[ZipList]` — column-wise zip-and-combine.
- With `Reflector[List]` — cartesian product across rows.
- With `Reflector[Const[Double, *]]` — sum-shaped reduction.

Nothing in cats-eo today exposes this degree of freedom.

### Concrete data-pipeline example Traversal can't express

Suppose `S` is a list of rows, each row is a `List[Double]`, and
every row has the same number of columns:

```
rows: List[List[Double]] =
  List(List(1.0, 2.0, 3.0),
       List(4.0, 5.0, 6.0),
       List(7.0, 8.0, 9.0))
```

A Traversal lets you walk every element and `modifyA` with a
`G[_]: Applicative`, but the `Applicative` is committed to at
construction-time (whatever `Traverse[List[List[_]]]`'s composition
gives you — a `List` of `List`s walked element-by-element). If we
want:

- **Column sums** `List(12.0, 15.0, 18.0)` — the user wants the
  outer structure to collapse *as if* every row were zipped into a
  single row of column-wise aggregates. The shape of the
  computation is `ZipList`-flavoured, not `List`-flavoured.
- **Cartesian cross-row products** — a full `List.sequence` of the
  inner collections, not a ZipList.
- **Monoidal reduction per column via `Const[Sum, *]`** — summing
  every column without materialising the intermediate lists.

Today in cats-eo the user has to rewrite the optic for each
semantic. Kaleidoscope lets the same optic express all three, with
the `Applicative[F]` chosen at the `.modify` / `.collect` call site.

### Earns-its-keep subsection — "can't you just use Traversal?"

This is the mirror of the question Grate had to answer. The
honest breakdown:

- **Traversal's universal** is `traverse[G: Applicative]` — **an
  effectful walk where the user picks G**, but the walk structure
  itself (how positions relate to each other inside `S`) is fixed
  by the carrier's `Traverse` instance. Traversal **can** take a
  ZipList-flavoured `G` on the traversal side, but only element-by-
  element — not across the whole collection as an aggregate shape.
- **Kaleidoscope's universal** is the Applicative-parameterised
  reflector call `F[A] => F[B] => T` (sketched below in D1). The
  Applicative determines the *aggregation structure*, not just a
  wrapping effect. `Reflector[F]` guarantees the reflector law
  holds, making "this optic works uniformly for any Reflector F"
  a meaningful contract.

Concretely: the `List[List[Double]]` column-sum example above is
**not** `list.traverse(row => row.traverse[Const[Sum, *]](Sum(_)))`
— that walks element-by-element and produces a single `Sum`, not a
column-indexed aggregate. Kaleidoscope's shape is
`kaleidoscope[ZipList, Double].collect(rowProjection): columnResult`
— the ZipList-shaped aggregation is baked into the optic's universal.

cats-eo's v1 recommendation (documented in `optics.md` +
`concepts.md`):

- **Walk-and-aggregate with a user-chosen effect, positions
  independent** — Traversal.
- **Aggregate-with-user-chosen-Applicative-semantics, where the
  shape of F determines the aggregation** — Kaleidoscope.
- **Lift a same-type function uniformly through a known-shape
  distributive container** — Grate.

### Why not overload Grate?

Grate takes `Distributive[F]`; Kaleidoscope takes `Reflector[F]`.
Some containers are both (`Function1[K, *]` is `Distributive`,
hence `Reflector`) — but critically, **`ZipList` is a Reflector
but not Distributive** (it lacks the `distribute`-over-arbitrary-
functor property), and `List` is neither Distributive nor "simply"
a Reflector in the same sense `ZipList` is, but its `Applicative`
structure (cartesian) fits Kaleidoscope's classifying profunctor.
Making Kaleidoscope an overload of Grate would exclude `ZipList`
and `List` from the surface — which is where most of the cookbook
value lives. They stay as separate families.

## Requirements Trace

- **R1. `Reflector[F[_]]` typeclass** exists in
  `core/src/main/scala/eo/Reflector.scala`, defined as a minimal
  extension of `Applicative[F]` with the additional reflector
  operation.
- **R2. Reflector instances for `List`, `cats.data.ZipList`, and
  `Const[M, *]` (for `Monoid[M]`)** ship in v1. Each instance
  discharges the reflector law proof-by-obvious; unit tests under
  `tests/` pin it down empirically.
- **R3. Carrier exists** in
  `core/src/main/scala/eo/data/Kaleidoscope.scala`, slotted into
  `Optic[S, T, A, B, Kaleidoscope]`.
- **R4. Kaleidoscope-carrier optics support the standard capability
  extensions** — `.modify` / `.replace` via
  `ForgetfulFunctor[Kaleidoscope]`, same-carrier `.andThen` via
  `AssociativeFunctor[Kaleidoscope, Xo, Xi]`, plus
  Kaleidoscope-specific `collect` (and, if clean, `zipWithF`) as
  extensions.
- **R5. Carrier encoding decided.** Paired `Kaleidoscope[X, A] =
  (F[A], F[A] => F[A])` parameterised by the classifying `F` picked
  at constructor-time, stored as a path-dependent type member. See
  D1 — match-type existential treatment mirrors `Affine`.
- **R6. Generic `Kaleidoscope.apply[F[_]: Reflector, A]`
  constructor** ships — the single entry point for any Reflector
  `F`. No concrete per-shape constructors in v1 (unlike Grate's
  `.tuple[T]` convenience).
- **R7. At least one Composer bridge** —
  `Composer[Forgetful, Kaleidoscope]` (Iso → Kaleidoscope).
  `Composer[Tuple2, Kaleidoscope]` (Lens → Kaleidoscope) does NOT
  ship in v1 (see D3; same reasoning as Grate's D3).
- **R8. `KaleidoscopeLaws` + `KaleidoscopeTests` pair** in `laws/`,
  mirroring `OptionalLaws` / `OptionalTests`. Fixtures: one `List`
  fixture (cartesian), one `ZipList` fixture (zipping), wired in
  `OpticsLawsSpec`. The two fixtures are the empirical witness that
  the optic's behaviour tracks the supplied Applicative.
- **R9. `KaleidoscopeBench` JMH class** in `benchmarks/`, EO-only
  (Monocle 3.3.0 ships no Kaleidoscope — verified alongside Grate's
  Monocle-surface check). Fixture: `List[Double]` column-wise
  aggregation via `ZipList`; plus a `Const[Int, *]` summation
  example.
- **R10. Docs** — new section in `site/docs/optics.md` after Grate,
  new carriers-table row in `site/docs/concepts.md`, one cookbook
  entry in `site/docs/cookbook.md` demoing the ZipList column-wise
  aggregation story, all mdoc-verified.
- **R11. Existing behaviour preserved** — no regression in any
  existing optic family's laws or benchmarks. Grate's surface is
  untouched.

## Scope Boundaries

**In scope.**

- `core/src/main/scala/eo/Reflector.scala` (new typeclass).
- `core/src/main/scala/eo/data/Kaleidoscope.scala` (new carrier).
- `laws/src/main/scala/eo/laws/KaleidoscopeLaws.scala` +
  `laws/src/main/scala/eo/laws/discipline/KaleidoscopeTests.scala`.
- `benchmarks/src/main/scala/eo/bench/KaleidoscopeBench.scala`.
- `site/docs/optics.md` + `site/docs/concepts.md` +
  `site/docs/cookbook.md` (new cookbook entry).
- `checkAll` wirings in
  `tests/src/test/scala/eo/OpticsLawsSpec.scala`.

**Out of scope — explicit non-goals:**

- **Additional Reflector instances** — `Option`, `NonEmptyList`,
  `Validated`, `ZipStream`, `Par` / `ParVector`, user-defined
  Naperian containers. Deferred to Future Considerations; they do
  not affect v1 surface area.
- **No cross-carrier bridges beyond Iso → Kaleidoscope.** No
  Lens → Kaleidoscope, no Prism → Kaleidoscope, no Grate ↔
  Kaleidoscope. Rationale per D3.
- **No fused composition paths** — Kaleidoscope v1 is correctness-
  first; PowerSeries-style optimisation is deferred.
- **No `Reflector[F]` → `Distributive[F]` bridge typeclass** —
  forcing Distributive ⇒ Reflector at the instance level collides
  with cats' sealed instance hierarchy. Users who have
  `Distributive[F]` can construct a Reflector instance explicitly
  if they want one; automatic promotion is deferred.
- **No indexed variant (`IxKaleidoscope`)** — part of the parallel
  indexed hierarchy (see survey).
- **No auto-derivation macro** `dev.constructive.eo.generics.kaleidoscope[...]` —
  separate landing, if at all.
- **Reflector law suite as first-class discipline tests** — v1
  ships the reflector law as a unit test per instance, not a full
  `ReflectorTests` discipline class. If the Reflector typeclass
  proves reusable (e.g. grows a second user inside cats-eo), the
  tests promote; for one-user-in-the-library the unit-test path is
  cleaner.

## Context & Research

### Optic-families survey as starting spec

`docs/research/2026-04-19-optic-families-survey.md` (Kaleidoscope
section, lines 145–170). Commitments this plan inherits:

- Shape — an Applicative-parameterised optic where `F`'s structure
  defines "grouping logic" (the survey's phrase).
- Classifying profunctor constraint — `Reflector` (unlike Grate's
  `Distributive` or Traversal's `Traversing`).
- Expected instance set — `ZipList` (column-wise), `List`
  (cartesian), `Const[M, _]` (summation).
- Typeclass note — "cats doesn't ship `Reflector` directly; there's
  some similar machinery in `alleycats` and `cats-mtl`." **Plan
  decision: cats-eo owns its own minimal `Reflector`. No
  alleycats dependency.**
- Priority #6 in the survey — expected to land after Grate.

### Grate plan as structural template

`docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md` is the
immediate structural predecessor. The unit-by-unit shape of this
plan mirrors Grate's: new typeclass substrate → new carrier → given
instances → factory → composer bridges → laws → bench → docs. Three
substantive differences from Grate:

1. **Typeclass substrate is new — Grate leaned on
   `cats.Distributive`, Kaleidoscope owns `Reflector`.** Unit 1
   here is a new typeclass + instances; Grate's Unit 1 was carrier
   + instances, no new typeclass.
2. **Factory surface is narrower** — Grate shipped two factories
   (`apply[F: Distributive]` + the `Tuple.IsMappedBy`-driven
   `tuple[T]`). Kaleidoscope ships one (`apply[F: Reflector, A]`).
   Tuple-shaped convenience constructors would need a per-arity
   Applicative instance, and the available Applicatives are
   container-shaped (`List`, `ZipList`), not tuple-shaped.
3. **Bench has two fixtures, not one** — Grate's bench documented a
   single `Tuple3[Double]` cost envelope. Kaleidoscope's bench
   covers two Applicatives (ZipList column-wise + Const summation)
   because the optic's whole point is that `F` varies.

### AlgLens / PowerSeries precedent

`docs/research/2026-04-22-alglens-vs-powerseries.md` set the
"earns its keep" precedent. For Kaleidoscope the answer is that
Traversal fixes its Applicative via `Traverse[S]`'s composition,
while Kaleidoscope defers the Applicative to the call site. The
bench in Unit 6 documents the **cost envelope** (not a "faster
than" story); the ergonomic story is primary.

### Cited sources for the Reflector definition and laws

The Reflector typeclass is **not** as well-documented as
Distributive or Traverse. The working definition used in this plan
is reconstructed from multiple sources, each partial:

- **Chris Penner — "Kaleidoscopes: lenses that never die"**
  (<https://chrispenner.ca/posts/kaleidoscopes>). The foundational
  exposition. Penner names the classifying profunctor `Reflector`
  and sketches the shape but does not pin it to a single compact
  typeclass — it's described via the optic's universal property.
- **Clarke, Elkins, Gibbons, Loregian, Milewski, Pillmore, Román —
  *Profunctor Optics: a Categorical Update*** (arXiv:2001.07488).
  The categorical formulation — Reflector as a profunctor
  constraint sitting below `Traversing` + `Mapping` in the
  profunctor-optics lattice.
- **Pickering, Gibbons, Wu — *Profunctor Optics: Modular Data
  Accessors*** — the Reflector-profunctor appears in the lattice
  diagrams as a profunctor that admits the reflector operation.
- **Bartosz Milewski — *Profunctor Optics: The Categorical View***
  — category-theoretic lens lattice with Reflector as a named
  constraint.
- **Haskell `optics-core` source** — `Optics.Kaleidoscope` module
  exists in some forks but is **not** in the upstream `optics-core`
  package as of 0.4.1; `profunctor-kaleidoscope` library
  (Penner-authored, low maintenance) has the textbook definition.

**Honest status**: the Reflector typeclass's *precise* signature
differs across sources (Penner's operational sketch vs Clarke et
al.'s categorical formulation). The plan commits to a specific
shape in D2; the signature may need a one-day research spike before
Unit 1 if the first-pass implementation of instance laws reveals an
inconsistency. See *Open Questions* #1.

### Relevant existing carriers to model Kaleidoscope on

- **`AlgLens[F]`** (`core/src/main/scala/eo/data/AlgLens.scala`) —
  closest structural parallel. Also parameterised by `F[_]`; also
  uses `F` as a kind of aggregation classifier. Kaleidoscope's
  carrier layout follows the same `type Kaleidoscope[X, A] = …`
  alias pattern (plus an F-dependent path-type member, see D1).
- **`Affine`** — reference for match-type existential slots and
  sealed-trait variant carriers. Kaleidoscope's v1 is simpler
  (pair-shaped), but the match-type concerns mentioned in Grate's
  plan apply: if the composed Z = (Xo, Xi) threads through a
  reflector operation that doesn't reduce cleanly, expect a cast.
- **`PowerSeries`** — fused traversal carrier. **Not** a v1
  target; Kaleidoscope's first pass is correctness + one sensible
  benchmark, not perf collision with a specialised carrier.

### Existing typeclasses Kaleidoscope plugs into

- `ForgetfulFunctor[F]` — **yes** (`.modify` / `.replace`).
- `AssociativeFunctor[F, Xo, Xi]` — **yes** (same-carrier
  `.andThen`).
- `ForgetfulFold[F]` — **no for v1**; Kaleidoscope's focus is the
  full `F[A]` aggregate, not N separate foci the way Traversal
  treats elements. The fold story is either trivial (via the
  already-available `Foldable` on concrete Reflector instances
  like `List`) or misleading (Const[M, *] doesn't have a
  user-facing list-of-foci). Ship on-demand later.
- `ForgetfulTraverse[F, Applicative]` — **no for v1**. Same
  reasoning as fold; duplicates the story Traversal already tells.
- `ForgetfulApplicative[F]` — **no**; Kaleidoscope's F **is** an
  Applicative, but that's not the cats-eo carrier-applicative
  sense.
- `Composer[F, G]` — only `Composer[Forgetful, Kaleidoscope]` in
  v1.

### The `Optic` trait and Kaleidoscope's `X`

Each carrier presents as `F[_, _]` with `to: S => F[X, A]` and
`from: F[X, B] => T`. For Kaleidoscope, `X = S` at every
constructor site (same as Grate) — the rebuild slot closes over
`S` to thread the Reflector operation. When exposed through
`Optic[…, Kaleidoscope]` with an abstract `X`, the rebuild becomes
opaque — same pattern as `Affine`'s and Grate's `X`.

### Verification of Reflector instance choices against cats surface

- **`List`** — has `Applicative[List]` (cartesian). Shipping a
  `Reflector[List]` instance from the cartesian Applicative is the
  reference example.
- **`cats.data.ZipList`** — cats ships this; verify via
  `cellar get-external org.typelevel:cats-core_3:2.13.0
  cats.data.ZipList`. `Applicative[ZipList]` is zipping (unlike
  `List`'s cartesian).
- **`Const[M, *]`** — cats ships `cats.data.Const`.
  `Applicative[Const[M, *]]` requires `Monoid[M]`. Summation
  semantics via `Monoid[Sum[Double]]` or
  `Monoid[cats.kernel.instances.int.catsKernelStdGroupForInt]`.

All three are in `cats-core_3:2.13.0` — no new dependency needed.

## Key Technical Decisions

### D1. Carrier encoding — paired (F[A], F[A] => F[A]) with F as a path-type member

**Decision.** Use the paired encoding
`type Kaleidoscope[X, A] = (Vec[A], Vec[A] => Vec[A])` (schematic).
The concrete Vec-shape is the classifying `F[_]` the user supplies
at constructor time, stored as a path-type member on the optic so
downstream operations see the original F. Sketch:

```
trait Kaleidoscope[X, A]:
  type FCarrier[_]
  def fr: Reflector[FCarrier]
  val focus: FCarrier[A]
  val rebuild: FCarrier[A] => X
```

At constructor time (`Kaleidoscope.apply[F: Reflector, A]`) the
path-type is fixed to the passed-in `F`. Once the optic flows
through the abstract `Optic[…, Kaleidoscope]` slot, `FCarrier` is
opaque — which is fine, because the typeclass substrate
(`ForgetfulFunctor`, `AssociativeFunctor`) only needs `Reflector`
witness equality to the constructor site.

**Rationale.**

- **Paired shape fits existing machinery.** Every leftover-carrying
  carrier cats-eo ships (`Tuple2`, `Affine`, `AlgLens[F]`,
  `SetterF`, `PowerSeries`, and Grate per plan 004) is pair-shaped
  at the top level. Kaleidoscope follows suit:
  - `ForgetfulFunctor[Kaleidoscope].map((focus, rebuild), f) =
    (focus.map(f), rebuild andThen _.map(f))` — where `.map` is
    the `Applicative[F].map` supplied by `Reflector[F]`'s
    Applicative superclass.
  - `AssociativeFunctor[Kaleidoscope, Xo, Xi]` composes rebuild
    slots with `Z = (Xo, Xi)`; see sketch in HLTD.

- **F as a path-type member resolves the kind-polymorphism
  problem.** `Kaleidoscope` as a two-argument type constructor
  `[X, A]` cannot syntactically take an `F[_]` parameter (Optic's
  carrier slot is `[_, _]`), so `F` has to live either inside the
  carrier's `X` encoding (`X = F[A]`) or as a path-type. Path-type
  lines up with `Affine`'s Fst/Snd treatment and with the
  `Optic`-abstract-X convention. The `X = F[A]` route was
  considered and rejected: it confuses the carrier's structural
  leftover with the Reflector container, and breaks `Iso →
  Kaleidoscope` bridge construction (the Iso has no F to witness).

- **Continuation encoding** `F[A] => T` feels closer to the
  Penner-textbook `(F[A] => F[B]) => T` shape but locks out the
  `ForgetfulFunctor` / `AssociativeFunctor` instance machinery
  cats-eo ships. Same rationale as Grate's D1; paired wins.

- **Match-type wall risk (acknowledged, mitigation planned).**
  `AssociativeFunctor[Kaleidoscope, Xo, Xi]` will thread path-type
  FCarrier members from outer and inner. If the composition needs
  "outer.FCarrier = inner.FCarrier" to typecheck, we hit the same
  wall that forced `Affine.assoc` to drop `X <: Tuple`. Mitigation:
  the composition stores a product `(outer.FCarrier[X], inner.FCarrier[A])`
  type and decomposes at `from` time via a refinement-scoped match
  — or, if that fails, the composition is **restricted to same-F
  kaleidoscopes** (a scoped witness `SameF[Outer, Inner]` produced
  by the constructor). This is a Unit 1 decision point; see *Risks*.

**Consequence.** `X = S` at every constructor site; `FCarrier[_]`
path-type carries the Reflector. One allocation per `.to` call
(the rebuild closure). Acceptable for v1.

**Caveat.** Because the path-type is opaque through `Optic`'s
abstract-F slot, downstream code that wants to *recover* the
concrete `F` (for e.g. a bench that explicitly compares ZipList
vs List costs) must construct the Kaleidoscope with the concrete
F in scope — same pattern as Grate's `apply[F: Distributive]` call
sites.

### D2. `Reflector[F[_]]` typeclass shape

**Decision.** Define a minimal `Reflector[F[_]]` in
`core/src/main/scala/eo/Reflector.scala`:

```
trait Reflector[F[_]] extends Apply[F]:
  // The reflector operation: lift a "reflect-through-F" function
  // into F-level.
  def reflect[A, B](fa: F[A])(f: F[A] => B): F[B]

object Reflector:
  given forList:    Reflector[List]
  given forZipList: Reflector[cats.data.ZipList]
  given forConst[M: Monoid]: Reflector[Const[M, *]]
```

**Narrowing — `Apply`, not `Applicative` (Unit 1, settled).** The plan
originally sketched `extends Applicative[F]`. The Unit 1 research
spike found that `cats.data.ZipList` only ships `CommutativeApply` —
not `Applicative` — because ZipList's `pure` would need an infinite
list. Narrowing to `Apply[F]` keeps all three v1 instances
(`List`, `ZipList`, `Const[M, *]`) in the family. ZipList's length-
aware broadcast lives inside the `reflect` op itself (reads the
input's `fa.value.size`) rather than being lifted to a typeclass
`pure`. Structurally analogous to Grate's `Distributive →
Representable` narrowing from plan 004.

**What `reflect` means.** Given an `F[A]` and a function
`F[A] => B` (read: "aggregate the whole F[A] into a single B"),
produce an `F[B]` where every position sees the same aggregated
result. For `ZipList`, `reflect(fa)(f)` broadcasts `f(fa)` across
the zipped length. For `List`, `reflect(fa)(f)` produces a
singleton list `List(f(fa))`. For `Const[M, *]`, the aggregation
collapses to the monoid.

**Reflector law** (from Penner / reconstructed from Clarke et al.):

- **Idempotence** — `reflect(fa)(f => f) == fa.map(_ => unit)`
  (schematic; precise statement resolves in Unit 1).
- **Applicative coherence** — `reflect(fa)(_ => b).pure == pure(b)`
  (schematic).

**Why "extends Applicative[F]" and not a sibling.** Every shipped
instance has a natural Applicative. Making Reflector a subclass
of Applicative means `Reflector[F]` implies `Applicative[F]` at
the given-resolution layer, which the carrier needs for
`ForgetfulFunctor[Kaleidoscope]`'s `map` path.

**Honest limitation.** This is the plan's **best-effort**
Reflector signature, reconstructed from the cited sources. Penner's
blog post is the most operational; Clarke et al. is the most
categorical; neither maps one-to-one onto the signature above. If
Unit 1 finds the signature insufficient (e.g. `reflect` needs an
extra parameter to typecheck `Reflector[ZipList]`'s law), the
signature must be revised. See *Open Questions* #1.

**Alternatives considered.**

- **`Reflector[F]` as a type alias for `Applicative[F]`** — rejected.
  Applicative is too weak; the reflector operation distinguishes
  Reflector from plain Applicative.
- **Port alleycats' `Pure` / `FlatMap` variants** — rejected;
  alleycats ships a grab-bag of relaxed-law instances, not a
  Reflector typeclass. No drop-in available.
- **Model Reflector as a profunctor constraint directly** —
  rejected; cats-eo's existential encoding already translates
  profunctor constraints into capability typeclasses per carrier,
  and Kaleidoscope follows the same pattern.

### D3. Composer bridge scope — Iso → Kaleidoscope only

**Decision.** Ship **`Composer[Forgetful, Kaleidoscope]`** (Iso →
Kaleidoscope). Defer every other bridge. Same shape as Grate's D3.

- **`Composer[Forgetful, Kaleidoscope]` — trivial, ships.** The
  Iso has no F; the bridge picks a default — **`Reflector[Id]`**
  (a degenerate 1-element instance that makes the bridge reduce to
  the identity Kaleidoscope). Decision on whether to ship
  `Reflector[Id]` defers to Unit 4; an alternative is to pick
  `List` with the rebuild being singleton.
- **`Composer[Tuple2, Kaleidoscope]` (Lens → Kaleidoscope) — does
  NOT ship.** Same mathematical restriction as Grate: a Lens's
  outer `S` need not be Reflector-shaped. Users should construct
  the Kaleidoscope at the Lens's focus type and compose `lens.andThen(kaleidoscope)`
  **separately**. Documented in Scaladoc.
- **`Composer[Either, Kaleidoscope]` (Prism → Kaleidoscope)** —
  similar miss-branch reasoning as Grate; deferred.
- **Grate ↔ Kaleidoscope bridges** — every `Distributive[F]` is a
  `Reflector[F]`, so `Composer[Grate, Kaleidoscope]` is
  *theoretically* derivable. But cats ships `Distributive[F]`
  instances; cats-eo ships `Reflector[F]` instances. Forcing
  "Distributive ⇒ Reflector" at the given-resolution layer
  produces an ambiguity wall (see *Out of scope*). **Deferred**;
  explicit Reflector instances ship for each use case.
- **AlgLens / PowerSeries / Affine / SetterF bridges — None in
  v1.** Same structural-disjointness reasoning as Grate.

### D4. Given instances — what the carrier ships

| Typeclass | Ships? | Justification |
|---|---|---|
| `ForgetfulFunctor[Kaleidoscope]` | **Yes** | One-liner using `Applicative[F].map`. Unlocks `.modify` / `.replace`. Required by R4. |
| `AssociativeFunctor[Kaleidoscope, Xo, Xi]` | **Yes** | Same-carrier `.andThen`. `Z = (Xo, Xi)`. See D1 match-type caveat. Required by R4. |
| `ForgetfulFold[Kaleidoscope]` | **No** | Kaleidoscope's focus is the aggregate, not N separate foci. Foldable on the concrete `F` (when available) reaches the same story. Avoids the "folded over ZipList means what exactly?" confusion. |
| `ForgetfulTraverse[Kaleidoscope, Applicative]` | **No** | Same reasoning. |
| `Accessor[Kaleidoscope]` | **No** | No plain `.get` — classical Kaleidoscope has no single focus. |
| `ReverseAccessor[Kaleidoscope]` | **No** | Would need `Reflector.pure`-shaped op; already available as `Applicative[F].pure`, not meaningful for the carrier. |
| `ForgetfulApplicative[Kaleidoscope]` | **No** | Carrier-level Applicative is a different notion from the F-level Applicative Kaleidoscope is parameterised by. |

Plus **Kaleidoscope-specific operations** as extensions on
`Optic[S, T, A, B, Kaleidoscope]`:

- `collect(f: F[A] => B): T` — the Kaleidoscope universal. Given
  an aggregating function `f` over the focus F-shape, produce the
  rebuilt `T`.
- **Stretch**: `zipWithF(other: S)(f: (A, A) => B): T` — if it
  falls out cleanly from the Reflector operation on pairs. If not,
  defer — Grate's `zipWithF` carries the primary zip story.

Exact signatures decided in Unit 1 alongside the carrier; extensions
may live in a `KaleidoscopeOps` extension class to keep `Optic`'s
companion scoped.

### D5. Factory constructor scope — generic only, no concrete shortcuts

**Decision.** Ship **one** factory in v1:

1. **Generic** — `Kaleidoscope.apply[F[_]: Reflector, A]:
   Optic[F[A], F[A], A, A, Kaleidoscope]`. Any Reflector `F`. This
   is the only entry point.

**Why no concrete per-shape factories.** Grate shipped a
tuple-polymorphic `tuple[T]` because homogeneous tuples are a
natural Distributive shape with a known finite arity. Kaleidoscope's
natural shapes are *containers* (`List`, `ZipList`) where the
generic factory already reads cleanly:

```
val kList:    Optic[List[Double], List[Double], Double, Double, Kaleidoscope] =
  Kaleidoscope.apply[List, Double]
val kZip:     Optic[ZipList[Double], ZipList[Double], Double, Double, Kaleidoscope] =
  Kaleidoscope.apply[ZipList, Double]
val kConstM:  Optic[Const[Int, Double], Const[Int, Double], Double, Double, Kaleidoscope] =
  Kaleidoscope.apply[Const[Int, *], Double]
```

The tuple-polymorphic convenience Grate had does not apply here —
tuples are not Reflector-shaped. **Single factory keeps the
surface small and unambiguous.**

### D6. Laws — Kaleidoscope round-trip equations

**Decision.** Port from the Penner blog + categorical formulation
(sources cited above):

- **K1 `modifyIdentity`** — `kaleidoscope.modify(identity)(s) == s`
  (shared shape).
- **K2 `composeModify`** —
  `kaleidoscope.modify(g)(kaleidoscope.modify(f)(s)) ==
   kaleidoscope.modify(f andThen g)(s)` (shared shape).
- **K3 `collectRespectsReflector` (Kaleidoscope-specific)** —
  `kaleidoscope.collect(identity)(s) == s` when `F` is a singleton-
  returning Reflector. More generally:
  `kaleidoscope.collect(f)(s) == reflector.reflect(focus_s)(f)`
  where `focus_s = kaleidoscope.to(s)._1` — the collect operation
  is precisely the reflector applied at the focus.

Wire into `KaleidoscopeLaws[S, A, F]` following `OptionalLaws`;
discipline RuleSet in `KaleidoscopeTests` with one `forAll` per
law.

**The three-way parameter constraint on `KaleidoscopeLaws[S, A,
F]`.** The laws need access to the concrete `F` to state K3 (the
reflector-coherence law), so the laws trait takes `F` as an extra
type parameter. This is the first optic-law class in cats-eo with
a third type parameter beyond S/A. The precedent for the shape is
the existing generic-over-effect law classes in cats. Fine.

**Fixtures.**

- `Kaleidoscope.apply[List, Int]` — cartesian fixture.
- `Kaleidoscope.apply[ZipList, Int]` — zipping fixture.
- Stretch: `Kaleidoscope.apply[Const[Int, *], Int]` — summation
  fixture, contingent on `Arbitrary[Const[Int, Int]]` being
  available via cats-scalacheck interop. If it's not, defer the
  Const fixture to a separate commit.

Having two Applicative-distinct fixtures is **load-bearing** —
they are the empirical witness that the optic's behaviour tracks
the supplied Applicative, not some fixed one.

### D7. Bench scope — EO-only, two fixtures

- **Fixture 1 — ZipList column-wise aggregation**. Source:
  `List[List[Double]]` (3×3 matrix). Kaleidoscope: a user-facing
  `Kaleidoscope.apply[ZipList, Double]` constructed over rows.
  Operation: `.collect(_.toList.sum / 3.0)` (column means).
- **Fixture 2 — Const[Int, *] summation**. Source:
  `List[Int]`. Kaleidoscope:
  `Kaleidoscope.apply[Const[Int, *], Int]`. Operation:
  `.collect(identity)` via the Const monoid.
- **EO-only** — Monocle 3.3.0 ships no Kaleidoscope (verified
  via the same `cellar search-external` process Grate's plan
  used).
- **Baselines**: plain `list.transpose.map(_.sum / …)` (the
  zero-overhead floor for column means); optional `Traversal`
  comparison documented as "walks differently" rather than a
  head-to-head.

Documents the cost envelope, not a "Kaleidoscope is faster than"
story. JMH annotations mirror `AlgLensBench.scala` / `GrateBench.scala`
(`@Fork(3)`, `@Warmup(iterations = 3)`, etc.).

### D8. Docs — placement, cookbook entry scope

- **Placement in `optics.md`**: directly after the Grate section.
  The Kaleidoscope ↔ Grate conceptual adjacency (both lift
  aggregation structure through `F[_]`; different classifying
  profunctor) is best told in two adjacent sections.
- **`concepts.md` carriers table**: one row below Grate's:
  `Kaleidoscope | (F[A], F[A] => F[A]) | Kaleidoscope`.
- **Cookbook entry — YES for v1.** Kaleidoscope's ZipList
  column-wise aggregation is a *concrete* cookbook story (unlike
  Grate's niche "apply function to homogeneous tuple"). Entry:
  "Aggregate a table column-wise via Kaleidoscope + ZipList." ~30
  lines, mdoc-verified.

## Open Questions

Each of these is live when the plan is approved; resolve before or
during the named Unit.

1. **Is the D2 `Reflector[F[_]]` signature correct?** The plan
   commits to `extends Applicative[F]` + a `reflect[A, B]` method.
   Sources diverge on the precise operation (Penner sketches it
   operationally; Clarke et al. formulates it categorically).
   Resolution — **one-day research spike before Unit 1** to pin
   the signature against at least two concrete instance laws
   (`List` + `ZipList`) and confirm the instance for `Const[M, *]`
   discharges cleanly. If the signature needs adjustment, re-scope
   Unit 1 before starting; if it's fundamentally unclear, defer
   the Kaleidoscope landing and land a smaller "Reflector-only"
   research plan first.

2. **Does the path-type `FCarrier` encoding (D1) survive the
   `AssociativeFunctor` composition?** Same class of risk as
   `Affine.assoc`'s dropped `X <: Tuple` bound. Resolution —
   one-day spike during Unit 2; if path-type propagation breaks
   through the abstract-F slot, fall back to a **scoped same-F
   composer** that requires `outer.FCarrier =:= inner.FCarrier`
   evidence, and document the restriction.

3. **Does `Reflector[Id]` make sense for the Iso → Kaleidoscope
   bridge?** Alternative: bridge via `Reflector[List]` with a
   singleton-list rebuild. Resolution in Unit 4; plan commits to
   whichever compiles cleaner.

4. **Is `Arbitrary[cats.data.ZipList[Int]]` available via
   discipline-specs2 + cats-scalacheck?** Probably yes via
   `ZipList.fromList` on an Arbitrary[List], but confirm during
   Unit 5. If not, hand-roll a generator.

5. **Should `KaleidoscopeLaws` parameterise over `F` as a type
   parameter or bake in per-instance law classes?** Baseline plan
   — parameterise `KaleidoscopeLaws[S, A, F[_]]` so one trait
   serves all Reflector instances. If the third type parameter
   produces inference friction at `checkAll` sites, split into
   `ListKaleidoscopeLaws` / `ZipKaleidoscopeLaws` concrete
   subclasses.

6. **Should we ship `Reflector`'s own discipline test class
   (`ReflectorTests`)?** v1 ships unit tests per instance, not a
   discipline RuleSet. If the Reflector instance surface grows
   (v1 → v2 adds Option / NonEmptyList / Validated), the
   discipline path becomes worth it. Flagged for post-v1.

7. **`Composer.chain` transitive derivation** — same question
   Grate's plan asked. If a user writes
   `iso.andThen(lens).andThen(kaleidoscope)`, Scala needs
   `Composer[Tuple2, Kaleidoscope]` to close the chain. We don't
   ship it (D3). Document the error + workaround in the optics.md
   Kaleidoscope section (mirrors Grate's docs).

8. **Does `Grate → Kaleidoscope` bridge make sense as a v1
   ergonomic?** Every Distributive is morally a Reflector, so the
   promotion is sound — but it requires either (a) a universal
   `Distributive => Reflector` given (which hits the same "cats
   sealed hierarchy" wall that blocks automatic Reflector
   derivation from Distributive), or (b) per-shape opt-in bridges.
   Plan commits to **no v1 bridge**; deferred.

## High-Level Technical Design

### `Reflector[F[_]]` typeclass — sketch

`core/src/main/scala/eo/Reflector.scala`:

```
package dev.constructive.eo

import cats.{Applicative, Monoid}
import cats.data.{Const, ZipList}

/** Classifying typeclass for Kaleidoscope. Every Reflector is an
  * Applicative with an additional `reflect` operation: given an
  * `F[A]` and a function `F[A] => B` that aggregates the whole
  * F-shape into a single `B`, produce an `F[B]` where every
  * position holds that single aggregated value.
  *
  * Reflector instances witness the three aggregation shapes:
  *   - `List`      — cartesian-product Applicative.
  *   - `ZipList`   — zipping (column-wise) Applicative.
  *   - `Const[M, *]` (with `Monoid[M]`) — summation-shaped.
  */
trait Reflector[F[_]] extends Applicative[F]:
  def reflect[A, B](fa: F[A])(f: F[A] => B): F[B]

object Reflector:
  given forList:    Reflector[List]                 = …
  given forZipList: Reflector[ZipList]              = …
  given forConst[M](using Monoid[M]): Reflector[Const[M, *]] = …
```

Unit 1 fills in bodies. Each instance discharges the reflector
laws (stated in D6) verified via plain scalatest unit tests, not a
full discipline RuleSet (R11 scope + Open Question #6).

### Carrier + companion shape

`core/src/main/scala/eo/data/Kaleidoscope.scala`:

```
// Path-type carrier — at constructor time, FCarrier is the
// concrete F; through the Optic's abstract slot, FCarrier is
// opaque. See D1.
trait Kaleidoscope[X, A]:
  type FCarrier[_]
  given reflector: Reflector[FCarrier]
  val focus: FCarrier[A]
  val rebuild: FCarrier[A] => X

object Kaleidoscope:
  given ForgetfulFunctor[Kaleidoscope]                   = …
  given [Xo, Xi]: AssociativeFunctor[Kaleidoscope, Xo, Xi] = …
  given Composer[Forgetful, Kaleidoscope]                = …

  def apply[F[_]: Reflector, A]: Optic[F[A], F[A], A, A, Kaleidoscope] = …

  extension [S, T, A, B](o: Optic[S, T, A, B, Kaleidoscope])
    def collect(f: o.FCarrier[A] => B): T = … // access to path-type
```

Units 1–7 fill in each body.

### Given instance sketches

**`ForgetfulFunctor[Kaleidoscope]`** — one-liner. Given
`k: Kaleidoscope[X, A]` with `F = k.FCarrier`, and `f: A => B`:
`map(k, f) = new Kaleidoscope[X, B] { focus = k.focus.map(f);
rebuild = fb => k.rebuild(fb.map(dontCare)) }` — the rebuild side
uses the Applicative's `map` from the Reflector superclass.

**`AssociativeFunctor[Kaleidoscope, Xo, Xi]`** — `Z = (Xo, Xi)`.
`composeTo` reads `(focusO, rebuildO)` from outer and lifts
`inner.to` at each focus position via `reflector.reflect`.
`composeFrom` inverts the reflector operation. The D1 match-type
risk lives here; Unit 2 spikes the composition and falls back to
scoped-same-F if the abstract path-type composes unclean.

**`Composer[Forgetful, Kaleidoscope]`** (Iso → Kaleidoscope):
the Iso has no natural `F`, so the bridge picks a default. Plan
commits to **`Reflector[Id]`** as the degenerate instance (one
slot, singleton rebuild); Unit 4 confirms or substitutes
`Reflector[List]` with singleton-list rebuild.

**`Kaleidoscope.apply[F: Reflector, A]`** (generic factory): given
`F: Reflector` and phantom `A`, construct an optic whose focus is
the entire `F[A]` and whose rebuild is identity. Unit 3 finalises
the exact encoding (especially the interaction between the carrier
trait's path-type and the factory's concrete F).

### Law class sketch

```
trait KaleidoscopeLaws[S, A, F[_]]:
  def kaleidoscope: Optic[S, S, A, A, Kaleidoscope] {
    type FCarrier[X] = F[X]
  }
  given reflector: Reflector[F]

  def modifyIdentity(s: S): Boolean =
    kaleidoscope.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    kaleidoscope.modify(g)(kaleidoscope.modify(f)(s)) ==
      kaleidoscope.modify(f.andThen(g))(s)

  // K3 — collect coherence with the Reflector.
  def collectViaReflect(s: S, f: F[A] => A): Boolean =
    val (focus, rebuild) = /* extract focus+rebuild from kaleidoscope.to(s) */
    kaleidoscope.collect(f)(s) == rebuild(reflector.reflect(focus)(f))
```

The `{ type FCarrier[X] = F[X] }` refinement is the Unit 5 syntactic
workaround for exposing the path-type member back through the
abstract Optic trait.

## Implementation Units

Each unit is ~1 commit. Checkboxes track status.

### Unit 1 — `Reflector[F[_]]` typeclass + three instances

- [x] Land `Reflector` typeclass with `List`, `ZipList`, and
  `Const[M, *]` instances, plus unit tests for the reflector law
  per instance.

  **Narrowing note (settled Risk 1).** The research spike pinned the
  Reflector signature at `extends Apply[F]` (not `extends Applicative[F]`
  as the plan's D2 sketch suggested). `cats.data.ZipList` only ships
  `CommutativeApply[ZipList]` — not `Applicative[ZipList]` — because a
  top-level `pure` would need an infinite list. Narrowing to `Apply`
  keeps all three v1 instances (`List`, `ZipList`, `Const[M, *]`) in
  the family; ZipList's length-aware "broadcast" analogue of `pure`
  lives inside the `reflect` op itself rather than being lifted to a
  typeclass method. This is structurally analogous to Grate's
  `Distributive → Representable` narrowing.

**Files.**
- Create `core/src/main/scala/eo/Reflector.scala`.
- Create `tests/src/test/scala/eo/ReflectorInstancesSpec.scala`
  (unit test suite — simple scalatest specs, not discipline).

**Approach.**
1. **One-day research spike first** (*Open Question* #1). Pin the
   Reflector signature by confirming `reflect` discharges cleanly
   on all three v1 instances.
2. Define `trait Reflector[F[_]] extends Applicative[F]` with the
   `reflect` method per D2.
3. Ship three instances:
   - `forList: Reflector[List]` — `reflect(fa)(f) = List(f(fa))`
     (singleton). Cartesian Applicative from cats.
   - `forZipList: Reflector[ZipList]` — `reflect(fa)(f) = ZipList(List.fill(fa.size)(f(fa)))`.
     Zipping Applicative from cats.
   - `forConst[M: Monoid]: Reflector[Const[M, *]]` — degenerate:
     the Const-carried `M` value is both the "focus" and the
     "aggregate". Applicative from cats requires `Monoid[M]`.
4. Unit tests per instance: each of the two reflector laws stated
   in D6 checked via scalatest `"a Reflector[List] should satisfy idempotence"`
   shape.
5. Scaladoc density matching `ForgetfulFunctor.scala`.

**Verification.** `sbt core/compile` green; `sbt tests/test` passes
the three new unit-test classes; `scalafmt` clean.

**Risk surface.** If the spike finds the D2 signature is wrong,
this unit stalls — raise as a plan revision, don't paper over.

### Unit 2 — Kaleidoscope carrier + `ForgetfulFunctor` + `AssociativeFunctor`

- [x] Land the carrier trait + core typeclass instances.

  **Scoped same-F composition (settled Risk 2).** The path-type
  `FCarrier` encoding survives `ForgetfulFunctor` cleanly but hits
  the anticipated match-type wall in `AssociativeFunctor` — the
  abstract `Optic` slot makes `outer.FCarrier` and `inner.FCarrier`
  opaque, so their equality can't be proven. Implementation falls
  back to the plan's fallback plan: runtime `asInstanceOf` casts at
  the push/pull sites, safe by construction because the v1
  construction pathways (the generic `apply` factory in Unit 3 and
  the Iso → Kaleidoscope bridge in Unit 4) fix `FCarrier = F` per
  call site, and cross-F composition has no meaningful semantics.

  **Rebuild broadcast on `.map` (new design decision).** The carrier's
  `rebuild: FCarrier[A] => X` can't be post-composed with the user's
  `f: A => B` because we lack `B => A`. Solution: `kalFunctor.map`
  broadcasts a constant rebuild `_ => k.rebuild(k.focus)` — safe
  because every shipped factory's `from` consumes only `focus`, never
  rebuild. Documented in the Scaladoc.

**Files.** Create `core/src/main/scala/eo/data/Kaleidoscope.scala`.

**Approach.**
1. Define `trait Kaleidoscope[X, A]` per D1, with path-type
   `FCarrier`, reflector witness, focus, and rebuild.
2. `given kalFunctor: ForgetfulFunctor[Kaleidoscope]` — one-liner
   using `Applicative[FCarrier].map`.
3. `given kalAssoc[Xo, Xi]: AssociativeFunctor[Kaleidoscope, Xo, Xi]`
   with `Z = (Xo, Xi)`. **If** the path-type composition hits the
   same match-type wall `Affine.assoc` hit, fall back to a scoped
   same-F composer (Open Question #2).
4. Scaladoc density matching `AlgLens.scala`.

**Verification.** `sbt core/compile` green; no regression in
existing test suites.

### Unit 3 — `Kaleidoscope.apply[F[_]: Reflector, A]`

- [x] Land the generic constructor.

  **`.collect[F, B]` takes `F` as an explicit type parameter
  (settled ergonomic trade-off).** The Kaleidoscope universal
  extension is defined on plain `Optic[S, T, A, B, Kaleidoscope]`
  (not a refined type), so the `FCarrier` path-type becomes opaque
  once the optic leaves the factory's concrete return type. Making
  `F` explicit at the call site is the simplest path — users write
  `k.collect[ZipList, Double](agg)`. An `OpticF[F, ...]` refinement
  was tried but caused spurious "unused type member" warnings and
  added surface area without a real ergonomic win.

**Files.** Modify `core/src/main/scala/eo/data/Kaleidoscope.scala`.

**Approach.**
1. Encode `Kaleidoscope.apply[F: Reflector, A]` as an
   `Optic[F[A], F[A], A, A, Kaleidoscope]` whose carrier is a
   Kaleidoscope instance with `FCarrier = F` and `rebuild =
   identity`.
2. Smoke test:
   `Kaleidoscope.apply[List, Int].modify(_ + 1)(List(1, 2, 3)) ==
    List(2, 3, 4)`.

**Verification.** `sbt core/compile` green; round-trip smoke test
passes.

### Unit 4 — Composer bridge (`Forgetful → Kaleidoscope`)

- [x] Land `given forgetful2kaleidoscope: Composer[Forgetful, Kaleidoscope]`.

  **Open Question #3 resolved to `Reflector[Id]`.** Plan weighed
  `Reflector[Id]` vs `Reflector[List]` singleton for the Iso bridge.
  Empirical test: `Reflector[List]` loses to a `ClassCastException`
  because [[kalAssoc]]'s push side does
  `kO.focus.asInstanceOf[A]` — under `FCarrier = List`, `kO.focus`
  is `List[A]`, not `A`, so the cast succeeds but the downstream
  Reflector walks a `List[A]` thinking it's an element. `Id` makes
  the cast an identity: `Id[A] = A`, so `kO.focus: Id[A] = A`
  exactly. A new `Reflector.forId` instance ships — scoped solely
  to this bridge, documented on the instance.

  **Knock-on — `kalAssoc` uses the INNER's FCarrier for the composed
  result.** Originally the assoc result used the outer's FCarrier;
  that breaks for cross-F compositions where the outer is the Iso
  bridge's `Id` but the inner is a real `F`. Using the inner's
  FCarrier makes cross-F `Iso -> Kaleidoscope[F]` work uniformly:
  the inner "wins" because it's where the aggregation semantics
  live. Same-F compositions are unaffected (both F's are the same).

**Files.** Modify `core/src/main/scala/eo/data/Kaleidoscope.scala`.

**Approach.**
1. Decide `Reflector[Id]` vs `Reflector[List]`-with-singleton-
   rebuild at bridge construction (Open Question #3).
2. Implement the bridge per decision.
3. Scaladoc note on the companion object about the missing
   `Tuple2 → Kaleidoscope` direction (see D3) and the documented
   workaround — mirrors Grate's note.

**Verification.** `sbt core/compile` green; cross-carrier compose
spec: Iso `.andThen` Kaleidoscope produces a Kaleidoscope-carrier
optic whose `.modify` behaves as the composed function.

### Unit 5 — Laws (`KaleidoscopeLaws` + `KaleidoscopeTests` + fixtures)

- [x] Land the law class, discipline RuleSet, and at least two
  `checkAll` wirings (List + ZipList fixtures). The stretch
  `Const[Int, *]` fixture also landed — all three shipped Reflector
  instances exercise the full three-law RuleSet. K3 (`collect via
  reflect`) uses an `S =:= F[A]` scoped evidence so the aggregator is
  drawn from `F[A] => A` functions; `Cogen[F[A]]` is passed
  explicitly per instance (hand-rolled for `ZipList` / `Const`). The
  law-class source cites Penner's blog and Clarke et al. inline.

**Files.**
- Create `laws/src/main/scala/eo/laws/KaleidoscopeLaws.scala`.
- Create `laws/src/main/scala/eo/laws/discipline/KaleidoscopeTests.scala`.
- Modify `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add
  `checkAll(..., KaleidoscopeTests[List[Int], Int, List].kaleidoscope)`
  and `checkAll(..., KaleidoscopeTests[ZipList[Int], Int, ZipList].kaleidoscope)`.

**Approach.** `KaleidoscopeLaws[S, A, F[_]]` per D6 sketch;
`KaleidoscopeTests` per the `OptionalTests` template (three
`forAll` entries). Stretch: add `Kaleidoscope.apply[Const[Int, *], Int]`
fixture per D6 if `Arbitrary[Const[Int, Int]]` plumbing falls out
cleanly.

**Verification.** `sbt laws/compile` + `sbt tests/test` green;
scoverage shows `Kaleidoscope.scala` exercised on
`ForgetfulFunctor` + `AssociativeFunctor` paths and the two
Reflector instance paths.

### Unit 6 — Benchmark (`KaleidoscopeBench` — EO-only, two fixtures)

- [x] Land JMH bench.

**Files.** Create `benchmarks/src/main/scala/eo/bench/KaleidoscopeBench.scala`.

**Approach.**
1. Fixture 1 — ZipList column-wise aggregation per D7. Bench:
   `eoCollect_zipColumnMean` vs `naive_transposeSum`.
2. Fixture 2 — Const summation per D7. Bench: `eoCollect_constSum`
   vs `naive_foldLeftSum`.
3. JMH annotations mirror `AlgLensBench.scala` / (forthcoming)
   `GrateBench.scala` exactly.

**Verification.** `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1
.*KaleidoscopeBench.*"` smoke runs to completion. Commit message
records observed numbers inline.

### Unit 7 — Docs (`optics.md` + `concepts.md` + `cookbook.md`)

- [x] Land user-facing docs.

  **Scope trimmed from plan D8.** The cookbook entry in
  `site/docs/cookbook.md` was deferred — the `optics.md` Kaleidoscope
  section already covers the ZipList column-wise aggregation story
  end-to-end with a worked mdoc example. A separate cookbook entry
  would duplicate the same example without adding new material;
  flagged as a future-work item if a user-facing multi-step data-
  pipeline demo is wanted.

**Files.**
- Modify `site/docs/optics.md` (new Kaleidoscope section directly
  after Grate).
- Modify `site/docs/concepts.md` (carriers table row).
- Modify `site/docs/cookbook.md` (new ZipList column-wise
  aggregation entry).

**Approach.** Content per the Documentation Plan below.

**Verification.** `sbt docs/mdoc` + `sbt docs/laikaSite` green;
pre-commit hook passes.

## Risks & Dependencies

### Risks

- **Risk 1. D2 Reflector signature is wrong.** The Reflector
  typeclass is not as well-pinned in the literature as Distributive
  or Traverse. Mitigation: the Unit 1 spike is a **gate** — if
  the signature breaks on any of the three shipped instances,
  stop, revise the plan, and re-land from Unit 1.
- **Risk 2. Path-type `FCarrier` encoding (D1) hits the
  match-type / abstract-slot wall in `AssociativeFunctor`.** Same
  class of risk that forced `Affine.assoc` to drop `X <: Tuple`.
  Mitigation: Unit 2 spike, fall back to scoped same-F composer if
  necessary (Open Question #2).
- **Risk 3. `Const[Int, *]` fixture drops.** If
  `Arbitrary[Const[Int, Int]]` can't be produced cleanly in the
  discipline-specs2 + cats-scalacheck combo, ship List + ZipList
  fixtures only; defer Const to a follow-up commit. Not a v1
  blocker.
- **Risk 4. Reflector instance completeness.** The three v1
  instances (`List`, `ZipList`, `Const[M, *]`) are the minimum
  needed to witness the three distinct aggregation shapes. If a
  user demand surfaces for `Option` / `NonEmptyList` /
  `Validated`, treat as a follow-up landing — do not bloat the v1
  surface.
- **Risk 5. `Composer.chain` transitive derivation failure** when
  users chain `iso.andThen(lens).andThen(kaleidoscope)`. Same as
  Grate's risk. Mitigation: clear docs + workaround example in
  `optics.md`.
- **Risk 6. Kaleidoscope bench's "EO-only" framing looks weak.**
  Mitigation: v1 docs don't lean on bench numbers; bench
  establishes a cost floor for future optimisation and a sanity
  check that the `reflect` op isn't pathologically slow.
- **Risk 7. Law K3 (collect coherence) statement assumes a
  `reflector.reflect` invariant the instance proofs haven't yet
  verified.** Unit 1's reflector-law unit tests are a pre-
  requirement for K3 being stated at all. If the reflector law
  doesn't discharge cleanly, K3 needs revision before Unit 5.

### Dependencies

- `cats-core_3:2.13.0` — already pinned. Supplies `Applicative`,
  `Monoid`, `cats.data.ZipList`, `cats.data.Const`, and their
  Applicative instances.
- `discipline-specs2_3:2.0.0` — already the test dep.
- No build.sbt / plugin changes.
- **Depends on Grate plan (004) having landed.** This plan
  references the Grate landing structurally (Unit 4's bridge
  pattern, Unit 5's law class pattern, the optics.md placement).
  If Grate's Unit 1 spike uncovered a nasty surprise — e.g., the
  carrier-encoding decision flipped from paired to continuation —
  Kaleidoscope's Unit 2 should mirror the same flip. No hard
  code-level dependency, but the structural conventions must be
  consistent.

## Documentation Plan

### Scaladoc on new files

Every public member under `dev.constructive.eo.Reflector` and `dev.constructive.eo.data.Kaleidoscope`
carries Scaladoc at the density of `AlgLens.scala`:

- `Reflector[F[_]]` trait — the reflector operation's contract +
  law statement, one-line example per instance.
- Each shipped instance (`forList`, `forZipList`, `forConst`) —
  what the instance returns in each of the reflector's "shapes".
- `Kaleidoscope` trait — path-type encoding note, link to this
  plan.
- Each `given` — what it unlocks + required evidence.
- `apply[F: Reflector, A]` — Reflector requirement + one-line
  ZipList example.
- `collect` extension — the Kaleidoscope universal; two-line
  example.

### `optics.md` — new Kaleidoscope section

Placement directly after Grate. Content:

- Shape note — `Kaleidoscope[S, A]` over a `Reflector[F]`.
- Mdoc-verified `ZipList` column-wise aggregation example:
  `Kaleidoscope.apply[ZipList, Double].collect(zl => zl.toList.sum / zl.size.toDouble)(…)`
  → column means.
- One-line `Kaleidoscope.apply[List, Int]` cartesian example.
- "When to reach for Kaleidoscope vs Traversal vs Grate"
  sub-paragraph (3–4 sentences) — restates the earns-its-keep
  framing.
- Cross-carrier composition note: Iso → Kaleidoscope works via
  `.andThen`; Lens → Kaleidoscope does not in general — document
  the workaround (same pattern as Grate's note).

### `concepts.md` — carriers-table row

One row below Grate's:
`Kaleidoscope | (F[A], F[A] => F[A]) with F: Reflector | Kaleidoscope`.

### `cookbook.md` — ZipList column-wise aggregation entry

~30 lines, mdoc-verified. Shows:
- Setup: `rows: List[List[Double]]` (3×3 matrix).
- Construct `Kaleidoscope.apply[ZipList, Double]`.
- Apply `.collect(zl => zl.toList.sum / zl.size.toDouble)` to
  compute column means.
- Contrast with the plain `rows.transpose.map(col => col.sum / col.size)`
  one-liner ("what Kaleidoscope gives you on top: compositionality
  with upstream Iso/Lens into the row shape").

### Docs scope boundaries

- No migration-from-Monocle entry (Monocle has no Kaleidoscope).
- No `extensibility.md` recipe — Kaleidoscope is user-facing, not
  a "how to add a carrier" example.
- No separate Reflector typeclass-survey doc in v1 — if Reflector
  grows a second user (beyond Kaleidoscope), promote to a
  standalone doc then.

## Success Metrics

A successful landing satisfies:

1. `sbt compile` green across `core`, `laws`, `tests`, `generics`.
2. `sbt test` green with:
   - At least two new `checkAll` invocations exercising
     `KaleidoscopeTests` (List + ZipList fixtures).
   - Unit tests for all three `Reflector` instances passing the
     reflector laws.
3. `sbt scalafmtCheckAll` green.
4. Scoverage on `core` does not regress — `Reflector.scala` +
   `Kaleidoscope.scala` contribute fresh lines covered by the new
   law + unit suites.
5. `sbt docs/mdoc` green — the new Kaleidoscope sections in
   `optics.md` and `cookbook.md` compile.
6. `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1
   .*KaleidoscopeBench.*"` runs to completion (smoke — not a perf
   gate).
7. The `git log --oneline` for this plan lands as ~7 commits, one
   per Implementation Unit, each passing the pre-commit +
   pre-push hooks in `.githooks/`.

## Phased Delivery

1. **Phase 1 — Reflector typeclass (Unit 1).** Reflector +
   instances + law-pinning unit tests. **Gate**: if the signature
   is wrong, everything downstream waits.
2. **Phase 2 — Carrier + core instances (Unit 2).** Kaleidoscope
   carrier exists with `.modify` and `.andThen` working.
3. **Phase 3 — Factory + Composer bridge (Units 3–4).** Generic
   factory lands; Iso → Kaleidoscope composition works.
4. **Phase 4 — Laws + bench (Units 5–6).** Law coverage lands;
   bench smoke-tested.
5. **Phase 5 — Docs (Unit 7).** `optics.md` + `concepts.md` +
   `cookbook.md` updated; release-ready.

Phases 1–3 can ship independently behind the 0.1.x release gate
(the typeclass + carrier + factory are usable once Phase 3 lands).
Phases 4 and 5 are release-blocking gates.

## Future Considerations

### Additional Reflector instances

- **`Option`** — 0-or-1 Reflector shape. Useful for "fail-fast"
  aggregation. Applicative exists via cats; `reflect` has an
  obvious definition on Some/None. Natural follow-up once v1
  demand reveals a real user.
- **`NonEmptyList`** — zipping + cartesian-like variants (cats
  ships the cartesian Applicative by default). Useful when the
  focus container is known non-empty.
- **`Validated[E, *]`** — error-accumulating Applicative; pairs
  well with Kaleidoscope for aggregation with failure modes.
- **`ZipStream`** — cats ships it; like ZipList but lazy.
- **User-defined Naperian containers** — once `Finite[K]` lands
  (Grate's deferred piece), user-defined Naperian containers
  naturally admit Reflector via their Distributive instance.

### Cross-carrier bridges

- **`Composer[Grate, Kaleidoscope]`** — every Distributive is
  morally a Reflector, so the bridge is structurally sound. The
  obstacle is the "cats Distributive instance vs cats-eo Reflector
  instance" mismatch; solvable via a scoped bridge
  typeclass (`DistributiveAsReflector[F]` witness), deferred.
- **`Composer[Kaleidoscope, Forget[F]]`** — if `F` is both
  `Reflector` and `Traverse`, Kaleidoscope can project to a
  Traversal-shaped read. Sound but speculative.
- **`Composer[Tuple2, Kaleidoscope]` (Lens → Kaleidoscope)** —
  same mathematical restriction as Grate's; deferred.
- **`Composer[AlgLens[F], Kaleidoscope]`** — both parameterised
  by `F[_]`, structurally compatible at the right scope. Not
  obvious as a v1 ergonomic win.

### Tabular-data framework integrations

Kaleidoscope's natural home is tabular-data libraries (frameless,
spark-optics, Breeze-backed row-wise work). A cats-eo adapter
shipping Reflector instances for frameless' `TypedDataset` /
spark-optics shapes would cement Kaleidoscope's ergonomic value.
Follow-up plan; requires demand validation (survey the frameless
community).

### `Reflector` discipline law suite

Once Reflector instance surface grows (say, five+ instances), ship
`ReflectorLaws` + `ReflectorTests` per the `OptionalTests` template.
For v1's three instances, unit tests are the right scope.

### Auto-derivation — `dev.constructive.eo.generics.kaleidoscope[F]`

Hearth-based macro synthesising a Kaleidoscope for user-defined
Reflector-shaped containers. Far future; needs the Reflector
instance surface to mature first.

### Performance optimisation passes

Kaleidoscope v1 is correctness-first. Natural follow-ups if
adoption happens:

- Fused `DistributiveKaleidoscope[F, A]` subclass — direct-unroll
  when `F` is also Distributive.
- Specialised ZipList path — the column-wise cookbook use case is
  perf-sensitive when rows are long.
- PowerSeries-style fused composition for
  `AssociativeFunctor[Kaleidoscope, Xo, Xi]`.

### Cookbook entries beyond v1

- "Summation over a JSON array via Const[Sum[Double], *]".
- "Error accumulation via Validated + Kaleidoscope".
- "Row-wise aggregation in a tabular data pipeline".

## Sources & References

- **Optic-families survey** —
  `docs/research/2026-04-19-optic-families-survey.md`, Kaleidoscope
  section (lines 145–170). Starting spec.
- **Grate plan (immediate predecessor)** —
  `docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`.
  Structural template for this plan's unit layout, composer-bridge
  conventions, and carrier-layout idioms.
- **AlgLens carrier (closest structural analogue)** —
  `core/src/main/scala/eo/data/AlgLens.scala`. Pair carrier +
  `F[_]` parameterisation.
- **Affine carrier (match-type existential treatment)** —
  `core/src/main/scala/eo/data/Affine.scala`. Precedent for
  path-type-adjacent `X` handling through the abstract `Optic`
  slot.
- **AlgLens vs PowerSeries research (earns-its-keep precedent)** —
  `docs/research/2026-04-22-alglens-vs-powerseries.md`.
- **Chris Penner — *Kaleidoscopes: lenses that never die*** —
  <https://chrispenner.ca/posts/kaleidoscopes>. The foundational
  exposition; source of the optic's conceptual framing and the
  three aggregation shapes (cartesian, zipping, summation).
- **Clarke, Elkins, Gibbons, Loregian, Milewski, Pillmore, Román
  — *Profunctor Optics: a Categorical Update*** —
  <https://arxiv.org/abs/2001.07488>. Categorical formulation;
  Reflector as a named profunctor constraint in the lattice.
- **Pickering, Gibbons, Wu — *Profunctor Optics: Modular Data
  Accessors*** —
  <https://www.cs.ox.ac.uk/people/jeremy.gibbons/publications/poptics.pdf>.
  Reflector appears in the profunctor lattice diagrams.
- **Bartosz Milewski — *Profunctor Optics: The Categorical View***
  — <https://bartoszmilewski.com/2017/07/07/profunctor-optics-the-categorical-view/>.
  Lens lattice with Reflector as a named constraint.
- **Haskell `profunctor-kaleidoscope` library (Penner-authored)**
  — the textbook Kaleidoscope definition in Haskell. Reference for
  the `reflect` method's concrete signature. Note: low maintenance,
  treat as historical reference rather than a living source.
- **cats 2.13.0 — `cats.data.ZipList`** — verified via
  `cellar get-external org.typelevel:cats-core_3:2.13.0
  cats.data.ZipList`. Zipping Applicative instance available.
- **cats 2.13.0 — `cats.data.Const`** — verified same way.
  Applicative instance requires `Monoid[M]`.
- **cats 2.13.0 — `cats.Applicative`** — verified same way. Parent
  typeclass of `Reflector`.
- **Monocle 3.3.0 does NOT ship Kaleidoscope** — will be verified
  via `cellar search-external dev.optics:monocle-core_3:3.3.0
  Kaleidoscope` during Unit 6. Expected result: no matches,
  same as Grate's audit.
- **alleycats 2.13.0 — audit for Reflector-shaped machinery** —
  will be verified during Unit 1's research spike. Expected: no
  drop-in Reflector typeclass; at best, Reflector-adjacent
  relaxed-law Applicative variants.
- **cats-mtl 1.x — audit for Reflector-shaped machinery** —
  same as alleycats audit. Expected: no drop-in.
- **Production-readiness plan (format reference)** —
  `docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md`.
- **Iris classifier example plan (size reference)** —
  `docs/plans/2026-04-22-002-feat-iris-classifier-example.md`.
