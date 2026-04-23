---
title: "feat: Indexed-optics hierarchy — IxLens / IxPrism / IxTraversal / IxFold / IxGetter / IxSetter parallel surface"
type: feat
status: active
date: 2026-04-23
---

# feat: Indexed-optics hierarchy — the parallel `Ix*` family

## Overview

Introduce the **indexed optics** hierarchy to `cats-eo` — the parallel
`Ix*` family that sits alongside every core optic family in Haskell's
`optics` / `lens` libraries. Each focus in an indexed optic is paired
with an *index* — a list position, a map key, a path segment, or
whatever identifier the underlying container exposes — and indices
**concatenate under composition** so a nested traversal can report the
path from the root to every visited focus.

In Haskell this is the single-largest parallel extension: every core
family has an indexed counterpart (`IxLens`, `IxPrism`,
`IxAffineTraversal`, `IxTraversal`, `IxGetter`, `IxAffineFold`,
`IxFold`, `IxSetter`). The optic-families survey flags this as "the
largest single-axis extension"
(`docs/research/2026-04-19-optic-families-survey.md`, Indexed section,
lines 43–76), budgeting 2–3 commits for carrier + typeclasses, one
commit per indexed family constructor + laws, and one for docs. This
plan is intrinsically bigger than the Grate plan (one family) or the
AffineFold plan (one trivially-expressible family) — it is closer in
shape to the production-readiness plan (`2026-04-17-001-...`), which
is the format reference.

The landing is **multi-phase and gated**. Phase 1 lands the indexed
typeclass substrate + a single carrier pattern (proof the machinery
works). Phase 2 builds out the four most-demanded families
(`IxTraversal`, `IxFold`, `IxGetter`, `IxLens`). Phase 3 ships the
composition lattice that lets indexed optics compose with plain
optics. Phase 4 ships laws + docs + one bench. The remaining families
(`IxPrism`, `IxAffineTraversal`, `IxSetter`, `IxAffineFold`) scope as
a v1.1 follow-up plan — the substrate + v1 families are enough to
judge whether the ergonomic-vs-cost trade is worth buying out the rest
of the hierarchy.

The macro side of this story — `eo.generics.ilens[S](_.field)` and
friends — is **explicitly out of scope** and deferred to a separate
generics plan once v1 settles.

## Problem Frame

### The indexed gap in `cats-eo` today

cats-eo's eight classical optic families — `Iso`, `Lens`, `Prism`,
`Optional`, `Traversal`, `Setter`, `Getter`, `Fold` — expose focus
values but not their *positions*. The common real-world ask "modify
every element alongside its position / key / path segment" is awkward
through the current surface:

- **`Traversal.each`** visits every element but the user can't tell
  which element they're looking at without manually plumbing
  `zipWithIndex`-shaped machinery through the modify lambda.
- **Nested traversals over JSON / tree structures** lose the path
  entirely — `cookbook`'s JSON examples all carry the `path: List[PathSegment]`
  by hand because the traversal composes the elements, not the path.
- **Map-keyed lenses** like Monocle's `at` / `index` combinators work
  per-key but don't expose the key on iteration.
- **`ifoldMap` / `itoList` / `imap`** (aggregate-with-index) are
  impossible to express without either a carrier that threads the
  index through every focus or ad-hoc per-call `zipWithIndex` work.

Haskell's `optics` package treats indices as a first-class citizen:
`traverseOf (traversed % _2 % traversed) f` walks foci; `itraverseOf
(traversed %& traversed) f` walks foci with paired list-position
indices; `imapOf`, `ifoldMapOf`, `itoListOf`, `iviewOf` operate
uniformly. Composition *concatenates* indices: composing an `IxLens i`
with an `IxTraversal j` yields an optic over `(i, j)`-indexed foci.

cats-eo's existential-carrier design does not currently have a place
for this indexing channel to live. Every extension to date
(`Traversal`, `SetterF`, `AlgLens`, `PowerSeries`) added a **new
carrier** when it needed new structural information; indexed optics
need the same treatment, but with a *parallel* structure — `IxF[I, X,
A]` or equivalent — that threads an index `I` alongside the existing
existential `X`.

### Earns-its-keep — "can't you just manually `zipWithIndex`?"

The fair question parallel to the AlgLens / Grate plans: "why do we
need a new carrier family? Can't you just pair the index into the
focus type?"

**Partially yes, with significant ergonomic cost.** The user can today
construct `Traversal.each[List, (Int, A)]` and feed `zipWithIndex` on
the way in; the modify lambda then receives `(Int, A)` pairs. But:

- **Composition breaks.** Once you've "baked the index into the focus
  type" `(Int, A)`, any downstream `Lens[A, X]` composition can no
  longer reach through the pair — the focus is now `(Int, A)`, not
  `A`. You lose the ability to continue the chain.
- **Indices don't concatenate.** The Haskell convention where
  `itraverseOf (traversed % traversed) f` gives you `(outerIdx,
  innerIdx)` cannot be expressed manually without a carrier doing the
  concatenation work. Two manual `zipWithIndex` passes give you `(Int,
  (Int, A))` but that nesting doesn't generalise.
- **`ifoldMap` / `itoList` are bolted on.** Each operation reinvents
  the index threading pattern.

**Indexed optics give this shape a name and a composition algebra.**
The `IxF[I, X, A]` carrier threads the index uniformly, the
`AssociativeFunctor[IxF[I], Xo, Xi]` instance concatenates indices on
composition, and the user writes `trav.imodify((i, a) => ...)` without
worrying about the threading. That's the ergonomic win. It costs a
parallel carrier hierarchy — which this plan pays for explicitly, and
justifies via the Haskell-community evidence that indexed optics are
reached for *routinely* once available.

### What "indexed" formally means in cats-eo terms

An indexed optic of index-type `I` over `S / T / A / B` exposes, in
place of the current `to: S => F[X, A]` / `from: F[X, B] => T`, a pair
that threads `I`:

```
to:   S    => F[X, (I, A)]         // or IxF[I, X, A]
from: F[X, (I, B)] => T            // or IxF[I, X, B] => T
```

The *same* two functions — `to` and `from` — just with the focus slot
widened to carry an index. That is the heart of the design decision in
D1 below: pick one encoding and commit, so the whole hierarchy lines
up under a single mental model.

## Requirements Trace

- **R1. Indexed typeclass substrate exists.** cats-eo owns its own
  `IxForgetfulFunctor[F, I]`, `IxForgetfulFold[F, I]`,
  `IxForgetfulTraverse[F, I, C[_[_]]]` typeclasses paralleling the
  existing `ForgetfulFunctor` / `ForgetfulFold` / `ForgetfulTraverse`
  ladder — cats does NOT ship `FunctorWithIndex` /
  `FoldableWithIndex` / `TraversableWithIndex` in `cats-core` (verified
  via `cellar list-external` — these typeclasses simply do not exist
  in cats; see *Context & Research*). Instances for `List`, `Vector`,
  `ArraySeq`, `Map[K, _]`, and `NonEmptyList` ship in Unit 1.
- **R2. Indexed carrier exists.** One carrier shape — either `IxF[I,
  X, A]` (dedicated three-parameter) or `IxCarrier[I, F][X, A] =>>
  F[X, (I, A)]` (index-in-focus via existing carrier). D1 picks one
  and ships it in `core/src/main/scala/eo/data/IxF.scala` (or
  `IxCarrier.scala`).
- **R3. Index concatenation under composition** is encoded as either
  `(I, J)` pairs (Haskell `lens`-package precedent), flat tuples via
  `Tuple.Concat`, or user-supplied `Semigroup[I]` combine. D2 picks
  one, justifies against Haskell precedent and cats-eo ergonomics.
- **R4. Four v1 indexed families ship as named constructors.**
  `IxTraversal`, `IxFold`, `IxGetter`, `IxLens` — see D4 for why these
  four and not others. Each lives under
  `core/src/main/scala/eo/optics/` and is constructed on top of the
  carrier from R2.
- **R5. Indexed extension methods.** `Optic[S, T, A, B, IxF[I]]`
  (or equivalent) carries new extension methods: `imodify`, `ifoldMap`,
  `itoList`, `ireplaceAt` — per-family subsets depending on the
  capability typeclasses the carrier admits. Enumerated per-family in
  HLTD.
- **R6. Composition lattice: Ix × Ix, Ix × plain, plain × Ix.** Via
  either cross-carrier composers (`Composer[F, IxF[I]]`) or an
  index-injecting extension method — D6 picks the approach. The
  precedent from Haskell `lens` is: Ix + Ix concatenates; Ix + plain
  preserves the existing indices; plain + Ix introduces the indices at
  the Ix side. Encode all three.
- **R7. Law classes + discipline RuleSets per indexed family.**
  `IxLensLaws`, `IxTraversalLaws`, `IxFoldLaws`, `IxGetterLaws`, each
  with a `FooTests` RuleSet and at least two `checkAll` wirings (e.g.
  `List[Int]` + `Vector[Int]` fixtures for `IxTraversal`).
- **R8. Law coverage for the "index forgets to the plain optic" invariant.**
  Every indexed optic must witness that "dropping the index" produces
  a well-behaved plain optic of the corresponding family — so
  `IxTraversalLaws` includes "the induced plain traversal obeys
  `TraversalLaws`".
- **R9. Law coverage for index stability.** Setting a focus with
  `ireplace` / `imodify` does not renumber / re-key indices on
  subsequent reads.
- **R10. One EO-only JMH bench** — `IxTraversalBench` — in
  `benchmarks/`, mirroring the `AlgLensBench` annotation pattern.
  Baseline: plain `Traversal.each` with manual `zipWithIndex`, to
  quantify the carrier overhead.
- **R11. Docs** — one unified page `site/docs/indexed.md` (rationale:
  current docs aren't split per-family, and an
  `optics/indexed.md` would orphan the v1.1 families) plus a
  concepts-page row for the indexed carrier. A cookbook entry
  ("traverse JSON with path") lands if a clean mdoc example falls out;
  otherwise deferred to v1.1.
- **R12. Existing behaviour preserved.** No law, benchmark, or API
  change alters the semantics of current optics or carriers. All
  existing tests remain green.

## Scope Boundaries

**In scope.** Everything required by R1–R12 above.

- `core/src/main/scala/eo/Ix*.scala` — new capability typeclasses
  (`IxForgetfulFunctor`, `IxForgetfulFold`, `IxForgetfulTraverse`,
  possibly `IxAccessor`) and their instances for the carrier decided
  in D1.
- `core/src/main/scala/eo/data/IxF.scala` (or `IxCarrier.scala`,
  depending on D1) — the indexed carrier + its `AssociativeFunctor`
  instance.
- `core/src/main/scala/eo/optics/IxTraversal.scala`,
  `IxFold.scala`, `IxGetter.scala`, `IxLens.scala` — named
  constructors per family.
- `core/src/main/scala/eo/optics/Optic.scala` — new
  indexed-specific extension methods (`imodify`, `ifoldMap`,
  `itoList`, `ireplace`) added alongside the existing ones, guarded
  by the `Ix*` capability typeclasses.
- `laws/src/main/scala/eo/laws/Ix*Laws.scala` +
  `laws/src/main/scala/eo/laws/discipline/Ix*Tests.scala` — four
  law-class pairs.
- `tests/src/test/scala/eo/OpticsLawsSpec.scala` — new `checkAll`
  wirings.
- `benchmarks/src/main/scala/eo/bench/IxTraversalBench.scala`.
- `site/docs/indexed.md` (new page) + row in
  `site/docs/concepts.md`.

**Out of scope — explicit non-goals.**

- **`IxPrism`, `IxAffineTraversal`, `IxSetter`, `IxAffineFold`** —
  the four v1.1 families. Scoped as a separate follow-up plan once
  v1 demonstrates the substrate is load-bearing. See *Future
  Considerations*.
- **Macro-derivation** — `eo.generics.ilens[S](_.field)` and
  equivalents. Substantial macro work, orthogonal to the carrier
  design, and the hand-written ergonomics need to settle before the
  macro surface is pinned. Deferred.
- **Cross-traversal index alignment** — Haskell's `lens` has
  `IxedFold` variants that allow "zip two indexed folds by index"
  (useful for diff-shaped operations); orthogonal and deferred.
- **Index polymorphism under `Contravariant`** — Haskell `optics`
  lets users `reindex` an optic by supplying `I => J`; ships in
  Phase 2 only if a clean `Optic.reindex` extension falls out.
- **Indexed Grate / Kaleidoscope** — those families aren't shipped
  plain yet; indexed variants have nowhere to sit.
- **`cats-mtl` / `alleycats` dependency** — substrate must live in
  `cats-core` or be cats-eo-owned; no new runtime deps.
- **Performance parity with Haskell `optics`** — Haskell's
  compile-time inlining regime is unavailable; target is "linear
  carrier overhead" measured in Unit 9.

## Context & Research

### Optic-families survey as starting spec

`docs/research/2026-04-19-optic-families-survey.md` (Indexed
variants section, lines 43–76). Commitments this plan inherits:

- Every family gets an indexed counterpart.
- Indices concatenate under composition.
- The survey's sketch — "roughly `type IxCarrier[I, F] = [X, A] =>>
  F[X, (I, A)]` or a dedicated `IxF[I, X, A]`" — is the D1 decision
  point.
- Substrate typeclass question is open — "cats ships these as
  `cats.UnorderedFoldable` + `cats.Foldable` with `zipWithIndex`, and
  alleycats has richer variants" — needs verification (done below,
  see cellar findings).
- Scope estimate — "2–3 commits for the carrier + typeclasses, 1 for
  each indexed family constructor + laws, 1 for docs". This plan
  budgets ~10 commits for v1 (four families + substrate + laws +
  bench + docs), consistent with the survey's estimate.

### Haskell `optics` / `lens` package precedent

Primary references for shape and semantics:

- **`Optics.IxLens`** —
  <https://hackage.haskell.org/package/optics-core-0.4.1.1/docs/Optics-IxLens.html>.
  The indexed Lens family; shape is `IxLens i s t a b` where `i` is
  the index type. Exposes `IxLens.iview`, `ioverA`, `iset`.
- **`Optics.IxTraversal`** —
  <https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-IxTraversal.html>.
  The indexed Traversal; most-demanded family. `itraverseOf f trav s`
  threads index-paired foci through an applicative action.
- **`Optics.IxFold`** —
  <https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-IxFold.html>.
  `itoListOf`, `ifoldMapOf`, `iviewOf`.
- **`Control.Lens.Indexed`** (the older `lens` library) —
  <https://hackage.haskell.org/package/lens-4.15.1/docs/Control-Lens-Indexed.html>.
  The original formulation; `Indexable p`, `Conjoined p`,
  `Indexed i`, `itraversed`, `ifolded`. `optics` cleaned up this
  surface; we follow `optics`'s framing (indices as a first-class
  type parameter, not as a profunctor class).

**Index concatenation convention in Haskell `optics`:** composing
`IxLens i` with `IxLens j` produces an optic over `(i, j)`-indexed
foci (a *pair*, not a flat tuple). `optics` ships a `ReindexedBy`
newtype to collapse `((i, j), k)` to a flat `(i, j, k)` at use-sites
when the user wants it. This plan picks the nested `(i, j)` pair
under composition to mirror cats-eo's existing `Z = (Xo, Xi)`
pattern in `AssociativeFunctor[Tuple2]` — the same shape.

### Monocle 3.x indexed-optic status — verified

Monocle 3.3.0 ships `monocle.function.Index[S, I, A]` and
`monocle.function.FilterIndex[S, I, A]` — both are *typeclasses*
whose methods return plain `Optional[S, A]` / `Traversal[S, A]`, not a
first-class indexed optic family (verified via `cellar search-external
dev.optics:monocle-core_3:3.3.0 Index` — results are the typeclass +
its companion, not an `IxLens` / `IxTraversal` type). Monocle's
`Index.at` returns an `Optional` keyed by `I`, but the index is
**consumed** at construction time; it does not thread through
composition. In other words: Monocle does NOT have an indexed optic
hierarchy in the Haskell sense; this plan introduces a feature
Monocle doesn't have.

**Benchmark consequence:** no side-by-side Monocle baseline is
available. `IxTraversalBench` in Unit 9 is EO-only, with a manual
`zipWithIndex` over `Traversal.each` baseline as the reference cost.

### Cats substrate — verified NO `*WithIndex` typeclasses

The survey mentioned "cats ships `FunctorWithIndex` /
`FoldableWithIndex` / `TraversableWithIndex`" — **this is wrong**.
Verified via `cellar list-external org.typelevel:cats-core_3:2.13.0
cats` + grep: the cats package contains `Foldable`, `Traverse`,
`UnorderedFoldable`, `UnorderedTraverse` — but *no* `*WithIndex`
typeclasses. `Foldable.get(fa)(idx: Long): Option[A]` exists but is
not equivalent to a `FunctorWithIndex`-style "run `f` at each
`(I, A)`" interface.

`alleycats-core_3:2.13.0` was checked similarly — also no
`*WithIndex` typeclasses.

**Consequence for D3:** cats-eo must ship its own
`IxForgetfulFunctor[F, I]` / `IxForgetfulFold[F, I]` /
`IxForgetfulTraverse[F, I, C[_[_]]]` capability typeclasses. This is
a larger cats-eo footprint than initially expected, but consistent
with the existing `ForgetfulFunctor` / `ForgetfulFold` /
`ForgetfulTraverse` pattern — we already own this capability ladder.
See D3 below.

### Relevant existing carriers — templates

- **`AlgLens`** (`core/src/main/scala/eo/data/AlgLens.scala`) — most
  recently-added carrier; the canonical template for
  `carrier + capability instances + composers + factories`. This plan
  follows the same layout, but with an *indexed* dimension threaded
  through every carrier operation.
- **`PowerSeries`** — the highly-optimised reference carrier; `IxPowerSeries`
  or similar is *not* a v1 target. Perf optimisation of the indexed carrier
  waits on real usage signal.
- **`Forget[F]`** — the phantom-X carrier that powers `Traversal.forEach` /
  `Fold`; the natural template for a `IxForget[F, I]` carrier if D1 picks
  the dedicated encoding.

### Existing laws — templates

- `laws/src/main/scala/eo/laws/TraversalLaws.scala` — four equations
  (modify identity, modify composition, replace idempotent, replace-is-modify-const).
  `IxTraversalLaws` adds two: the "drop index → plain traversal
  laws" witness, and the "ireplace doesn't renumber indices" stability
  law.
- `laws/src/main/scala/eo/laws/LensLaws.scala` — six equations;
  `IxLensLaws` adds "drop index → plain lens laws" + "index stable
  under replace".

### Lens-package evidence of indexed ergonomics in practice

Skimmed a sample of Haskell `lens` usage on Hackage and in large
codebases: indexed optics show up routinely in
- JSON / YAML walking (`aeson-lens`, `lens-aeson`) — path-aware
  modification.
- Filesystem traversals (`system-filepath-lens`) — path-aware
  iteration over directory trees.
- Map-keyed modifications (`Data.Map.Strict.Lens`) — key-aware
  `itraverseOf`.
- Error-location reporting in parsing frameworks — the "visited
  position" is exactly an indexed traversal's index.

The ergonomic win is *real* and *repeatedly demanded*. This is why
the survey flagged it as priority #2 after the trivially-free
additions.

## Key Technical Decisions

### D1. Indexed-carrier encoding — **dedicated `IxF[I, X, A]` three-parameter carrier** over "index-in-focus via existing carrier"

**Decision.** Introduce a dedicated three-parameter carrier per
family, roughly:

```
type IxF[I] = [X, A] =>> F[X, (I, A)]              // option (a): index-in-focus
// versus
opaque type IxF[I, X, A] = F[X, (I, A)]            // option (b): dedicated newtype
```

Within option (b) there are two internal sub-choices: opaque type alias over the
underlying pair-carrier, or a genuine sealed trait. **Pick the opaque-type
variant of (b)** — dedicated three-parameter carrier, backed by the existing
pair carrier, with distinct given instances:

```
opaque type IxForget[F[_], I] = [X, A] =>> F[(I, A)]
opaque type IxTuple2[I]       = [X, A] =>> (X, (I, A))
opaque type IxAffine[I]       = [X, A] =>> Affine[X, (I, A)]
```

**Rationale.**

- **Instance ambiguity.** If we went with option (a) — "indexed is
  just the plain carrier with `(I, A)` as the focus" — every generic
  `ForgetfulFunctor[F]` instance would apply transparently, and the
  user would have no way to distinguish "modify this `(I, A)` pair"
  from "modify this `A` at index `I`". The user extension
  `imodify((i, a) => f(i, a))` would collide with plain `modify`.
- **Capability control.** With a dedicated type, we can ship
  `IxForgetfulFunctor[IxF[I], I]` where the instance's `imap`
  signature is `(I, A) => B`, not `(I, A) => (I, B)`. The user never
  sees the raw pair on the write side; the index flows through
  automatically.
- **Precedent.** Every cats-eo carrier is a dedicated two-parameter
  type (`Tuple2`, `Either`, `Affine`, `AlgLens[F]`, `PowerSeries`,
  `SetterF`). Option (b) keeps the pattern uniform — an indexed
  carrier is a three-parameter member of the same family, not a
  special case of an existing one.
- **Opaque-type backing.** At the JVM level, `IxForget[F, I][X, A]`
  *is* `F[(I, A)]` — same representation, zero allocation overhead.
  Opaque-type boundaries keep the type-class lattice separate without
  costing runtime.

**Consequence.**

- The `AssociativeFunctor[IxF[I], Xo, Xi]` instance concatenates
  indices via `Z = ((Xo, Xi), ???)` — `???` resolved in D2.
- Each existing family gets a paired indexed carrier: `IxTuple2[I]`
  for `IxLens`, `IxEither[I]` for `IxPrism`, `IxAffine[I]` for
  `IxAffineTraversal`, `IxForget[F, I]` for `IxTraversal` / `IxFold`,
  `IxForgetful[I]` for `IxGetter`.
- Scope consequence: **one new carrier per family shipped**. v1 ships
  four: `IxForget[F, I]` (powers `IxTraversal` and `IxFold`),
  `IxForgetful[I]` (powers `IxGetter`), `IxTuple2[I]` (powers
  `IxLens`). That's three new opaque types.

**Spike caveat.** The opaque-type story in Scala 3 has known quirks
around given-instance discovery when the opaque type's underlying
shape is already populated with givens (`Tuple2`'s
`ForgetfulFunctor` instance would otherwise apply to `IxTuple2`'s
representation). Unit 0 is a spike: confirm opaque-type shielding
works cleanly; if not, fall back to **sealed trait + final class**
wrapper in the style of `Affine`.

**Alternative considered.** A single `IxCarrier[I, F[_, _]][X, A] =>>
F[X, (I, A)]` type applicator (option (a)). Rejected for the
ambiguity and capability-control reasons above. Returns in *Open
Questions* as a re-evaluation trigger if Unit 0 finds opaque-type
shielding too costly.

### D2. Index concatenation under composition — **nested `(I, J)` pairs, with a flattening extension**

**Decision.** Composing an indexed optic with index type `I` with one
of index type `J` produces an optic whose indices are of type `(I,
J)` — **nested pairs, matching Haskell `optics`'s convention**. A
flattening extension method `Optic.flattenIndex` lets users collapse
`(((I, J), K), L)` to a flat `Tuple4[I, J, K, L]` via Scala 3
`Tuple.Concat` / `Tuple.Fold` when they want it; the default composition
keeps the nested shape.

**Rationale.**

- **Haskell precedent.** `optics` composes `IxLens i` with `IxLens j`
  to `IxLens (i, j)` and ships `ReindexedBy` for flattening at the
  use site. Mirrors our established `AssociativeFunctor[Tuple2]`
  pattern where `Z = (Xo, Xi)` is a nested pair.
- **Alternative A: flat tuples via `Tuple.Concat`.** Rejected — the
  type system work is substantial, `IxF` would need to carry `I <:
  Tuple` bounds throughout (the match-type reduction pattern that
  already forced the `X <: Tuple` removal in `Affine`). Keep the
  core simple; flatten on demand.
- **Alternative B: user-supplied `Semigroup[I]` combine.** Rejected
  as the default — forces the user to pick a `Semigroup` at every
  composition site, often when no natural instance exists (what's
  the `Semigroup` of `Int × String`?). Available as an
  opt-in `Optic.reindexCombine[J](using Semigroup[J])` extension
  when the user *wants* a monoidal combine (e.g. sum of positions).

**Consequence.**

- `AssociativeFunctor[IxForget[F, I], Xo, Xi]`:
  - `Z` tracks both the existing structural leftover chain AND
    accumulates the per-level index history. For `Forget`-like
    carriers `Z = Nothing`; for pair carriers `Z = ((Xo, Xi),
    IndexHistory)` where `IndexHistory` is implementation-defined.
  - Actual outer index delivered to the user: `(outerI, innerI)`
    pairs threaded through each `(I, A)` focus slot.
- `Optic.flattenIndex` extension:
  - `flatten2: Optic[S, T, A, B, IxF[(I, J)]] => Optic[S, T, A, B, IxF[(I, J)]]` — identity (already flat).
  - `flatten3: Optic[S, T, A, B, IxF[((I, J), K)]] => Optic[S, T, A, B, IxF[(I, J, K)]]`.
  - `flatten4` — same shape, four deep.
  - Arbitrary-depth via `Tuple.Concat` — stretch goal for Unit 7; if
    it doesn't type-check, ship 2/3/4 hand-written.

**Caveat.** Users writing `.andThen` chains four-deep will see
`(((A, B), C), D)` as the type of the composed index. The `flatten4`
extension is the escape hatch; documentation explicitly shows it.

### D3. Indexed typeclass substrate — **cats-eo owns the `Ix*` capability typeclasses**

**Decision.** Ship four new typeclasses, paralleling the existing
ladder:

```
trait IxForgetfulFunctor[F[_, _], I]:
  def imap[X, A, B](fa: F[X, A], f: (I, A) => B): F[X, B]

trait IxForgetfulFold[F[_, _], I]:
  def ifoldMap[X, A, M: Monoid]: ((I, A) => M) => F[X, A] => M

trait IxForgetfulTraverse[F[_, _], I, C[_[_]]]:
  def itraverse[X, A, B, G[_]: C]: F[X, A] => ((I, A) => G[B]) => G[F[X, B]]

trait IxAccessor[F[_, _], I]:
  def iget[X, A]: F[X, A] => (I, A)    // only instantiated for "singleton-foci" carriers
```

**Rationale.**

- **cats-core does NOT ship `*WithIndex`** — verified above. cats-eo
  must own this capability ladder.
- **Parallel to existing pattern.** The `ForgetfulFunctor` /
  `ForgetfulFold` / `ForgetfulTraverse` ladder is cats-eo's existing
  idiom; the `Ix*` ladder is the natural parallel extension.
- **alternative: reuse `ForgetfulFunctor` + thread index through focus**
  — rejected in D1 for the capability-control reason. Reconsider if
  Unit 0 discovers opaque-type shielding is more trouble than it's
  worth.
- **alleycats / external substrate.** alleycats-core does not ship
  `*WithIndex` either. Introducing a runtime dependency on an
  external indexed-typeclass library (e.g. a hypothetical
  `cats-ix`) is not worth the coupling; cats-eo-owned is cleaner.

**Consequence.** `core/src/main/scala/eo/` grows four new files:
`IxForgetfulFunctor.scala`, `IxForgetfulFold.scala`,
`IxForgetfulTraverse.scala`, `IxAccessor.scala`. Each is small —
instance count scales with the containers we ship (`List`, `Vector`,
`ArraySeq`, `Map[K, _]`, `NonEmptyList`).

### D4. v1 family scope — **`IxTraversal`, `IxFold`, `IxGetter`, `IxLens`; defer `IxPrism`, `IxAffineTraversal`, `IxSetter`, `IxAffineFold` to v1.1**

**Decision.** Four families ship in v1; four defer to v1.1.

| Family | v1 | Justification |
|---|:-:|---|
| `IxTraversal` | Yes | The *primary* demand. Most indexed-optic use cases (JSON walking, list-position-aware modify, map-keyed traversal) are indexed traversals. |
| `IxFold` | Yes | `itoList` / `ifoldMap` / `itoListWithPath` — read-only sibling of `IxTraversal`, fell out once the substrate is in place. |
| `IxGetter` | Yes | Trivial once substrate exists — read-only single-focus. Demonstrates the "degenerate I" case. |
| `IxLens` | Yes | The second-most-demanded — "this field, at this static index / name". Validates the `IxTuple2` carrier. |
| `IxPrism` | v1.1 | Low demand in practice — indices on a 0-or-1 focus collapse to `Option[I]`. Ship if users ask. |
| `IxAffineTraversal` | v1.1 | Dominated by `IxTraversal` in practice; users reach for `IxTraversal` over `Option[A]` instead. |
| `IxSetter` | v1.1 | `SetterF` already has no `AssociativeFunctor` instance; adding the indexed variant compounds the limitation. |
| `IxAffineFold` | v1.1 | Same reasoning as `IxAffineTraversal`. |

**Rationale for the split.**

- **Unlock the primary ask first.** `IxTraversal` is *the* indexed
  optic — 80% of the real-world use. Shipping it in v1 with its
  three neighbours (`IxFold`, `IxGetter`, `IxLens`) covers the
  majority of practical use cases, and lets us learn from real usage
  before shipping four more families.
- **Substrate dominates effort.** The carrier, typeclass substrate,
  composition lattice, and law infrastructure account for ~70% of
  this plan's work. Each additional family is ~1 commit once the
  substrate exists; v1.1 is a much smaller plan.
- **Scope control.** A 15-unit plan with 10 units executed and 5
  deferred is more tractable than a 20-unit plan with everything in
  one go. The phased-delivery gate (Units 1–2) lets us re-scope if
  the substrate turns out harder than expected.

**Consequence.** v1 ships four indexed families; v1.1 ships four
more. Total hierarchy takes two plans. The v1.1 plan is a
follow-up; this plan only scopes v1.

### D5. Extension-method surface per family

**Decision.** Indexed extension methods live on `Optic` under
capability-guarded extension groups, mirroring the existing
`ForgetfulFunctor` / `ForgetfulFold` / `ForgetfulTraverse` groups.

Per family:

| Family | Carrier | Ix extensions unlocked |
|---|---|---|
| `IxTraversal` | `IxForget[F, I]` | `imodify`, `ireplaceAt(i)(b)`, `ifoldMap`, `ifoldMapWithIndex`, `itoList`, `imodifyA[G]` |
| `IxFold` | `IxForget[F, I]` (with `T = Unit`) | `ifoldMap`, `itoList`, `iheadOption`, `ipreviewAt(i)` |
| `IxGetter` | `IxForgetful[I]` (with `T = Unit`) | `iget: S => (I, A)` |
| `IxLens` | `IxTuple2[I]` | `iget`, `ireplaceAt(i)(b)`, `imodify` |

**Cross-family common surface** (available via `IxForgetfulFunctor`):
`imodify((i, a) => ...)`.

**Extension placement.** All indexed extension methods live in
`core/src/main/scala/eo/optics/Optic.scala` next to the plain
`modify` / `foldMap` / `get` groups, guarded by the matching `Ix*`
capability typeclass. Rejected alternative: per-family companion
objects. Mirroring the existing pattern keeps discovery uniform — a
user who finds `.modify` via LSP auto-complete will also find
`.imodify` in the same group.

**Naming — `i` prefix.** Matches the Haskell `optics` / `lens`
convention (`iview`, `ifoldMapOf`, `itoListOf`). The prefix is
pronounced "indexed"; the `i` is lowercase to keep call-site
readability.

### D6. Composition lattice — **Ix × Ix, Ix × plain, plain × Ix**

**Decision.** Three cases, each via existing cats-eo machinery:

1. **Ix × Ix** (same family, same index type): via `AssociativeFunctor[IxF[I], Xo, Xi]`,
   the resulting optic has index type `(I, I)` (nested pair per D2).
2. **Ix × plain** (indexed outer, plain inner): via
   a new `Composer[F, IxF[I]]` for each plain carrier `F`.
   The plain inner is *lifted* into an indexed inner with a
   "phantom / unit" index — the outer's indices are preserved
   verbatim, the inner contributes nothing new. **Needs the outer's
   index type to survive lift**, so the composer is parameterised by
   `I`: `given [I]: Composer[Tuple2, IxTuple2[I]]`.
3. **plain × Ix** (plain outer, indexed inner): via a
   `Composer[IxF[I], F]` that *drops* the indexed side's indices —
   the resulting optic is plain, the inner's indices are forgotten.
   **This is lossy** and the user should explicitly opt in; expose
   as `.forgetIndex` rather than an implicit.

**Rationale.**

- **Ix + Ix concatenation** matches Haskell `optics`; this is the
  composition we want to be most ergonomic.
- **Ix + plain lift** is the second-most-common. Example: "traverse
  a list with index, then drill into each element's `Lens[Foo,
  Bar]`". The user keeps the list-position index; the inner lens
  doesn't contribute indices. Haskell `optics` does this via
  auto-indexing.
- **plain + Ix drop** is less common and *destructive* (loses
  information). Haskell `optics` treats this via `noIx` — an
  explicit opt-in. We match that: `.forgetIndex` extension method
  rather than an implicit `Composer`.

**Alternative considered:** auto-promote every plain optic to
indexed with `I = Unit`. Rejected — forces every unindexed chain to
pay the indexed-carrier allocation cost even when no indexed optic
is ever involved. The explicit lift via `Composer[F, IxF[I]]` is
zero-cost (opaque-type aliasing per D1) when the user *wants* the
lift, and doesn't touch unindexed chains at all.

**Consequence.**

- Unit 7 is dedicated to the composition-lattice extensions: eight
  `Composer` instances (four plain carriers × two directions) plus
  the `.forgetIndex` extension.
- The `Morph[F, G]` machinery — which auto-picks the composer
  direction — should transparently handle Ix + plain via the new
  composers. Verify during Unit 7.

### D7. Derivation macros — **out of scope, roadmap item**

**Decision.** `eo.generics.ilens[S](_.field)` / `iprism[S, A]` /
`itraversal[S, A]` macros are **not** part of v1. They ship in a
separate follow-up plan once the hand-written indexed optics settle.

**Rationale.**

- **Macro work is substantial.** The existing `lens[S](_.field)`
  macro under `generics/` (see `CLAUDE.md` `Auto-derivation: eo-generics`
  section) is ~500 lines of Hearth-based macro code. The indexed
  variants need a second axis of machinery — parse the selector,
  derive the Lens, AND thread an index type through — effectively
  doubling the macro surface.
- **Ergonomics need settling first.** The hand-written indexed
  optics will reveal whether index types should default to `Int`
  (list position), `String` (field name), or custom. The macro can
  only codify what the hand-written surface teaches us.
- **Orthogonal.** Nothing in v1 blocks on the macro; users can
  hand-roll `IxLens` instances until the macro lands.

**Consequence.** Users of v1 write indexed optics by hand via the
named constructors. When v1.1 lands, users of plain `lens[S](_)` will
be able to opt into `ilens[S](_.field)`. The generics plan follow-up
lands post-v1.1.

### D8. Laws per indexed family

**Decision.** Each indexed family gets an `IxFooLaws` + `IxFooTests`
pair in `laws/`. Shared shape — index-forgetting + stability +
plain-family laws:

**`IxLensLaws[S, A, I]`** — 6 plain + 2 indexed = 8 laws total.
- All 6 `LensLaws` equations on the "forget index" projection.
- `iGetStable(s: S)` — `ilens.iget(s)._1` is invariant under
  `.ireplace(b)(s)` (setting the focus does not change the index).
- `iModifyForgets(s: S, f: A => A)` — `ilens.imodify((_, a) => f(a))(s) ==
  ilens.asPlain.modify(f)(s)` (ignoring the index reduces to plain
  modify).

**`IxTraversalLaws[T[_], A, I]`** — 4 plain + 3 indexed = 7 laws.
- All 4 `TraversalLaws` equations on the "forget index" projection.
- `iModifyForgets(s, f)` — ignoring the index = plain modify.
- `iFoldMapHomomorphism(s, f, g)` — index-aware foldMap is a monoid
  homomorphism.
- `iToListIndexOrder(s)` — `itoList(s)` returns indices in the
  container's natural iteration order. Pin the semantics of "what's
  the index of the 3rd element of a `List[Int]`?" as "Int position
  in insertion order".

**`IxFoldLaws[S, A, I, F]`** — mirrors `FoldLaws` with the indexed
variants of each equation.

**`IxGetterLaws[S, A, I]`** — trivial: `iget.forgetIndex == plainGetter.get`.

**Test infrastructure.** Reuses the existing `discipline-specs2`
RuleSet pattern. Each RuleSet has one `forAll` per law. `checkAll`
wirings in `tests/src/test/scala/eo/OpticsLawsSpec.scala`:
`List[Int]` fixture for `IxTraversal`, `Map[String, Int]` fixture for
`IxTraversal` over a map, `(Int, Int)` for `IxLens` (at static index
0 and 1), `List[Int]` for `IxFold`, `(Int, Int)` for `IxGetter`.

### D9. Benchmark scope — **EO-only, `IxTraversalBench`**

**Decision.** One JMH class in `benchmarks/`, mirroring
`AlgLensBench.scala`'s annotations. Fixture: `List[Int]` of length
100. Operations:

- `eoModify_ixTrav` — `ixTraversal.imodify((i, a) => i + a)`.
- `eoModify_ixTrav_dropIx` — `ixTraversal.imodify((_, a) => a + 1)`.
  Isolates the index-threading overhead when the user doesn't use the
  index.
- `eoModify_plainTrav_zipIndex` — the manual baseline:
  `Traversal.each[List, (Int, Int)].modify { case (i, a) => (i, i + a) }(list.zipWithIndex)`.
- `eoFoldMap_ixTrav` — `ixTraversal.ifoldMap((i, a) => i + a)`.

**Monocle baseline:** *none*. Monocle 3.3.0 ships no first-class
indexed optics (verified above). Docstring on the bench class
explains the EO-only framing and cites the verification.

**Performance expectation.** `eoModify_ixTrav_dropIx` should be
within 2× of `plainTrav` (the opaque-type carrier per D1 has no
runtime shape difference; the overhead is in per-element `(I, A)`
allocation). If substantially worse, Unit 10 documents the hot path
and a `PSVec`-style optimisation ships as a v1.1 task.

### D10. Docs placement — **single `site/docs/indexed.md`, one concepts.md row**

**Decision.** One docs page covering all v1 indexed families, plus a
row in `site/docs/concepts.md`'s carriers table. No per-family page.

**Rationale.**

- **Current docs aren't split per-family.** `site/docs/optics.md`
  covers all eight classical families in one page. Splitting the
  indexed hierarchy per-family would be inconsistent.
- **Shared narrative.** The substrate, composition model, and
  extension-method pattern are common across all four v1 families;
  explaining once-and-pointing-back is cleaner than four parallel
  sections with the same material.
- **v1.1 compatibility.** When `IxPrism` / `IxAffineTraversal` /
  `IxSetter` / `IxAffineFold` land, they extend the same
  `indexed.md` page with new sections — no page-level restructure.

**Content outline for `indexed.md`:**

1. What an indexed optic is (1 short paragraph).
2. Why you'd want one (JSON-path example, map-key example — both
   mdoc-verified).
3. The four v1 families — short section each, one example.
4. Index concatenation under composition (flat-tuple-via-flatten,
   how to read a four-deep composition).
5. Interop with plain optics — `.forgetIndex` + the `Composer`
   story.
6. Current limitations (v1.1 families, no macro).

Concepts-table row: `IxForget[F, I] | F[(I, A)] | IxTraversal / IxFold`,
`IxTuple2[I] | (X, (I, A)) | IxLens`, `IxForgetful[I] | (I, A) | IxGetter`.

**Cookbook entry.** If Unit 10 surfaces a clean "JSON path walk"
example — indexed traversal over `circe.Json` producing
`Map[PathSeg, Json]` — it ships as a cookbook entry. Otherwise
deferred.

## Open Questions

Each question is live when the plan is approved; resolve before or
during the named Unit.

1. **Does opaque-type shielding work as D1 assumes, or does the
   indexed carrier need to be a genuine sealed trait?** This is the
   single biggest risk. Scala 3 opaque types do shield the
   underlying type from instance search at the *top level*, but
   generic instance resolution through `AssociativeFunctor[IxF[I],
   Xo, Xi]` (where `IxF` is opaque) may pierce the opacity in
   surprising ways. **Unit 0 spike required** — if opaque types are
   ambiguous with the underlying carrier's givens, fall back to
   `final class IxForget[F[_], I](val underlying: F[(I, A)])
   extends AnyVal` (AnyVal wrapper) or a sealed-trait carrier. Spike
   budget: 2 days. Gate Unit 1 on a clean spike result.

2. **Does the `AssociativeFunctor[IxF[I], Xo, Xi]` type signature
   compile cleanly?** The index `I` is fixed across the composition,
   so the outer and inner must share it — but Haskell `optics`
   composes different index types (`IxLens i` with `IxLens j`
   produces `IxLens (i, j)`). The cats-eo
   `AssociativeFunctor[F, Xo, Xi]` is parameterised on outer/inner
   existentials, not on the inner type constructor — so we need
   *either* a two-index variant
   `AssociativeFunctor[IxF[?], Xo, Xi, Io, Ii]` *or* a wrapper trick
   that makes the composer see `IxF` as two different carriers with
   the same tag. **Unit 1 compilation gate** — if the standard shape
   doesn't work, we may need to widen `AssociativeFunctor`'s
   signature or introduce a separate `IxAssociativeFunctor[F, Xo,
   Xi, Io, Ii]` typeclass. This is *not* a minor decision and may
   ripple through the whole substrate. **Flag as a real possible
   blocker; if this turns out to require `IxAssociativeFunctor`,
   re-scope to 14+ units.**

3. **Index type ergonomics — is `Int` the default for list-like
   containers, or should we follow Haskell `optics` and make the
   user pick?** Haskell `optics` requires the user to pass the
   index type at construction (`itraverseListOf :: Lens s a (Int, a)`).
   cats-eo could default to `Int` for `List` / `Vector` / `ArraySeq`
   and `K` for `Map[K, _]`, making the common case zero-configuration.
   But "default to Int for list-like" means the user who *wants* a
   custom index (e.g. `Long` for very-long sequences, or an
   enumerated `Position` type) has to opt out. **Resolution —
   during Unit 1, provide both:** a zero-config `IxTraversal.each[T,
   A]` that uses the natural index (Int / K), and a polymorphic
   `IxTraversal.eachWithIndex[T, I, A](using IxForgetfulTraverse[T,
   I])` for custom indices. This gives us the ergonomic default
   *and* the escape hatch.

4. **Does `TraversableWithIndex`-style behaviour fall out cleanly
   from `cats.Traverse` + manual index threading, or do we need
   more?** cats has no `TraverseWithIndex` — we'll write
   `IxForgetfulTraverse` instances hand-threading an index. Concrete
   implementations for `List` / `Vector` use
   `Traverse[List].traverse` with a `State[Int, _]`-style counter in
   the applicative pipeline. This is ~20 lines per container but may
   have pitfalls — `Map` ordering semantics, Vector's stateful
   traverse correctness under non-sequential applicatives. **Unit 2
   spike** — write the instance for `List`, verify against a
   stateful-applicative test, then port to Vector / Map.

5. **Does `Composer[Tuple2, IxTuple2[I]]` need an `I`-providing
   witness, or should it default to `Unit`?** If a user writes
   `ixLensOuter.andThen(plainLensInner)` they expect the result to
   be an `IxLens` — same index type as the outer. The composer
   needs to *lift* the plain lens into the indexed carrier with
   whatever `I` the outer carries. The signature
   `given [I]: Composer[Tuple2, IxTuple2[I]]` should work via Scala
   3 inference from the outer's type, but hasn't been tried.
   **Unit 7 compilation gate** — if inference doesn't pick up `I`
   correctly, materialise as an explicit `.liftIndex[I]` extension.

6. **What does `imodify` do if the user throws inside `(i, a) => B`?**
   Expected: the exception propagates, same as plain `modify`. One
   spec in Unit 8 pins this down. No plan change expected.

7. **Does `Map[K, V]` with `K` as index require `Order[K]` or
   `Hash[K]` for law stability?** The `iToListIndexOrder` law pins
   "natural iteration order"; for `Map` the iteration order is
   insertion-order for `ListMap` but undefined for a plain `Map`.
   **Resolution — during Unit 8:** use `ListMap[K, V]` fixture for
   order-sensitive laws; document that `Map[K, V]` traversal's
   iteration order is unspecified, mirroring
   `cats.Traverse[Map[K, _]]`'s behaviour.

8. **Should `.flattenIndex` ship in v1 or defer to v1.1?** It's a
   quality-of-life extension that doesn't affect correctness.
   Resolution: ship `flatten3` and `flatten4` in v1 (hand-written);
   defer arbitrary-depth `Tuple.Concat`-based version to v1.1. This
   keeps Unit 7's scope tractable.

9. **Bench: does `IxTraversalBench` need a paired `AlgLensBench`-style
   research doc?** Only if Unit 9's numbers reveal a surprise
   (indexed carrier > 3× plain traversal overhead). Default — single
   bench class with commit-message numbers; research doc only if
   numbers warrant one.

## High-Level Technical Design

### Indexed-carrier sketch

Per D1, four new opaque types ship (three in v1; `IxAffine[I]` and
`IxEither[I]` scope to v1.1):

```
// core/src/main/scala/eo/data/IxF.scala
opaque type IxForget[F[_], I] = [X, A] =>> F[(I, A)]
opaque type IxTuple2[I]       = [X, A] =>> (X, (I, A))
opaque type IxForgetful[I]    = [X, A] =>> (I, A)

// v1.1:
opaque type IxAffine[I]       = [X, A] =>> Affine[X, (I, A)]
opaque type IxEither[I]       = [X, A] =>> Either[X, (I, A)]
```

Each opaque type has an `apply` / `unapply`-style access helper
(lowered to identity at runtime) and its own `given` instances — see
next section.

### Per-family type aliases

```
// core/src/main/scala/eo/optics/package.scala   (or individual files)
type IxLens[S, A, I]      = Optic[S, S, A, A, IxTuple2[I]]
type IxTraversal[S, A, I] = Optic[S, S, A, A, IxForget[List, I]]   // List is the structural shape, not "the container"
type IxFold[S, A, I]      = Optic[S, Unit, A, A, IxForget[List, I]]
type IxGetter[S, A, I]    = Optic[S, Unit, A, A, IxForgetful[I]]
```

Note `IxForget[List, I]` — the index-paired focus list is the
fundamental shape; users pass their container shape via the `each` /
`forEach` constructors, same as today.

### Typeclass substrate sketch

```
trait IxForgetfulFunctor[F[_, _], I]:
  def imap[X, A, B](fa: F[X, A], f: (I, A) => B): F[X, B]

trait IxForgetfulFold[F[_, _], I]:
  def ifoldMap[X, A, M: Monoid]: ((I, A) => M) => F[X, A] => M

trait IxForgetfulTraverse[F[_, _], I, C[_[_]]]:
  def itraverse[X, A, B, G[_]: C]: F[X, A] => ((I, A) => G[B]) => G[F[X, B]]

trait IxAccessor[F[_, _], I]:
  def iget[X, A]: F[X, A] => (I, A)
```

Instances shipped in v1 (for the four v1 families):

| Instance | Unlocks | Container backing |
|---|---|---|
| `IxForgetfulFunctor[IxForget[List, I], I]` | `imodify` on `IxTraversal` | List |
| `IxForgetfulFunctor[IxTuple2[I], I]` | `imodify` on `IxLens` | — |
| `IxForgetfulFold[IxForget[List, I], I]` | `ifoldMap` on `IxTraversal` / `IxFold` | List |
| `IxForgetfulTraverse[IxForget[List, I], I, Applicative]` | `imodifyA` on `IxTraversal` | List |
| `IxAccessor[IxTuple2[I], I]` | `iget` on `IxLens` | — |
| `IxAccessor[IxForgetful[I], I]` | `iget` on `IxGetter` | — |
| `AssociativeFunctor[IxForget[List, I], Xo, Xi]` | same-carrier `.andThen` of `IxTraversal` / `IxFold` | — |
| `AssociativeFunctor[IxTuple2[I], Xo, Xi]` | same-carrier `.andThen` of `IxLens` | — |
| `AssociativeFunctor[IxForgetful[I], Xo, Xi]` | same-carrier `.andThen` of `IxGetter` | — |

### Composition lattice matrix

| Outer | Inner | Composer | Resulting index |
|---|---|---|---|
| Plain Lens (`Tuple2`) | Plain Lens (`Tuple2`) | `AssociativeFunctor[Tuple2, _, _]` (existing) | none |
| `IxLens[I]` | Plain Lens | `Composer[Tuple2, IxTuple2[I]]` (NEW) | `I` preserved |
| Plain Lens | `IxLens[I]` | `Composer[IxTuple2[I], Tuple2]` (NEW, drops index) | none (lossy) |
| `IxLens[I]` | `IxLens[J]` | `AssociativeFunctor[IxTuple2[?], _, _]` (NEW) | `(I, J)` nested |
| `IxTraversal[I]` | Plain Lens | `Composer[Tuple2, IxForget[List, I]]` (NEW) | `I` preserved |
| `IxTraversal[I]` | `IxTraversal[J]` | `AssociativeFunctor[IxForget[List, ?], _, _]` (NEW) | `(I, J)` nested |
| `IxGetter[I]` | Plain Getter | `Composer[Forgetful, IxForgetful[I]]` | `I` preserved |
| Plain Traversal | `IxTraversal[I]` | `Composer[IxForget[List, I], Forget[List]]` (NEW, drops) | none (lossy) |

Where "NEW" is a new instance shipped in Unit 7.

The **lossy** directions (`.forgetIndex`) are user-explicit via an
extension method; the non-lossy directions are implicit via `Composer`
summoning.

### Extension-method surface per family

Per D5 above. The new extensions live in
`core/src/main/scala/eo/optics/Optic.scala`, guarded by `Ix*`
capability typeclasses. Example:

```
extension [S, T, A, B, F[_, _], I](o: Optic[S, T, A, B, F])(using IFF: IxForgetfulFunctor[F, I])
  inline def imodify(f: (I, A) => B): S => T =
    s => o.from(IFF.imap(o.to(s), f))

  inline def ireplaceAt(i: I, b: B): S => T =
    s => o.from(IFF.imap(o.to(s), (ii, a) => if ii == i then b else a))

extension [S, T, A, B, F[_, _], I](o: Optic[S, T, A, B, F])(using IFF: IxForgetfulFold[F, I])
  inline def ifoldMap[M: Monoid](f: (I, A) => M): S => M =
    s => IFF.ifoldMap(f)(o.to(s))

  inline def itoList: S => List[(I, A)] =
    s => IFF.ifoldMap((i, a) => List((i, a)))(o.to(s))
```

### Macro-derivation roadmap entry

v1 does not ship macros. v1.1 plan scopes `ilens[S](_.field)` /
`itraversal[S, A]` under `generics/`. See *Future Considerations*.

## Implementation Units

Each unit is roughly one commit. Units 0 and 1 are **gates** —
defer subsequent units until these succeed. Total v1 count: 11
units (0 spike + 10 body units).

### Unit 0 — Spike: opaque-type shielding

- [ ] Confirm D1's opaque-type approach works, or pivot to sealed-trait carrier.

**Files.** Throwaway branch — a short spike outside the main source
tree.

**Approach.**
1. Create `eo.data.IxForget[F[_], I]` as an opaque type over
   `F[(I, A)]`.
2. Try to write a `ForgetfulFunctor[IxForget[F, I]]` instance that
   does *not* collide with the existing `Forget[F]` instance.
3. Try the symmetric case with `IxTuple2[I]` vs `Tuple2`.
4. If instance shielding works cleanly (no ambiguity), commit the
   carrier file and proceed to Unit 1 with the opaque-type approach.
5. If shielding leaks, pivot to `final class IxForget[F[_], I](val
   underlying: F[(I, A)]) extends AnyVal` style. Document the pivot
   in-file and in *Open Questions* resolution.

**Budget.** 2 days. If unresolved, document as a blocker and
re-scope the plan.

**Verification.** A small local test file with
`summon[ForgetfulFunctor[IxForget[List, Int]]]` resolves to the
indexed instance (not the plain `Forget[List]` instance).

### Unit 1 — Indexed typeclass substrate

- [ ] Land `IxForgetfulFunctor`, `IxForgetfulFold`,
  `IxForgetfulTraverse`, `IxAccessor` with instances for `List`,
  `Vector`, `ArraySeq`, `Map[K, _]`, `NonEmptyList`.

**Files.**
- `core/src/main/scala/eo/IxForgetfulFunctor.scala`
- `core/src/main/scala/eo/IxForgetfulFold.scala`
- `core/src/main/scala/eo/IxForgetfulTraverse.scala`
- `core/src/main/scala/eo/IxAccessor.scala`

**Approach.**
1. Write each typeclass trait paralleling the existing
   `ForgetfulFunctor` / `ForgetfulFold` / `ForgetfulTraverse` /
   `Accessor` exactly — same signature shape, with `(I, A)` inputs
   where indexed variants need them.
2. For each container (List, Vector, ArraySeq, Map, NonEmptyList),
   write the instance hand-threading the index via
   `Traverse.mapAccumulate` with `Int` initial state. For `Map[K, _]`,
   use `K` as the index.
3. Scaladoc per instance, matching `ForgetfulFold`'s density.
4. **Gate:** verify the `AssociativeFunctor[IxF[I], Xo, Xi]` type
   signature compiles without hacks (Open Question #2). If not,
   escalate to plan re-scope.

**Verification.** `sbt core/compile` green. Unit test: summon each
instance for each container and check a one-element case.

### Unit 2 — Indexed carrier(s) + `AssociativeFunctor` instances

- [ ] Land `IxForget[F, I]`, `IxTuple2[I]`, `IxForgetful[I]` opaque
  types (or wrapper classes per Unit 0 outcome) with their
  `AssociativeFunctor` instances.

**Files.** `core/src/main/scala/eo/data/IxF.scala` (single file
housing all three).

**Approach.**
1. Define the three opaque types (or wrapper classes).
2. Write `AssociativeFunctor[IxF, Xo, Xi]` for each, threading
   indices via nested `(I, J)` pairs per D2.
3. Write `ForgetfulFunctor[IxF]` wiring through the `IxForgetfulFunctor`
   typeclass so plain `.modify` still works on indexed optics (with
   `(i, a) => a` applied internally — the index is observed but not
   returned).
4. Write `IxForgetfulFunctor[IxF, I]` / `IxForgetfulFold[IxF, I]` /
   `IxForgetfulTraverse[IxF, I, Applicative]` delegating to the
   underlying carrier's existing typeclass + index threading.

**Verification.** `sbt core/compile` green.

### Unit 3 — `IxTraversal` family

- [ ] Land `IxTraversal.each[T, A]` and `IxTraversal.eachWithIndex[T, I, A]`
  constructors.

**Files.** `core/src/main/scala/eo/optics/IxTraversal.scala`.

**Approach.**
1. Define the `type IxTraversal[S, A, I]` alias.
2. `IxTraversal.each[T[_]: Traverse, A]: IxTraversal[T[A], A, Int]` —
   natural-position Int index, zero-config.
3. `IxTraversal.eachWithIndex[T[_], I, A](using
   IxForgetfulTraverse[IxForget[T, I], I, Applicative])` for custom
   indices.
4. Scaladoc with at least one runnable example.

**Verification.** `sbt core/compile` green. Smoke: `IxTraversal.each[List, Int].imodify((i, a) => i + a)(List(10, 20, 30))` evaluates to `List(10, 21, 32)`.

### Unit 4 — `IxFold` family

- [ ] Land `IxFold.apply[T, A]` and related constructors.

**Files.** `core/src/main/scala/eo/optics/IxFold.scala`.

**Approach.**
1. Define `type IxFold[S, A, I]` alias.
2. `IxFold.apply[T[_]: Foldable, A]: IxFold[T[A], A, Int]` —
   equivalent to the `Fold.apply[F, A]` existing constructor but
   indexed.
3. Extensions `.itoList`, `.ifoldMap` via `IxForgetfulFold`.

**Verification.** `sbt core/compile` green. Smoke: `.itoList(List(1,
2, 3)) == List((0, 1), (1, 2), (2, 3))`.

### Unit 5 — `IxGetter` family

- [ ] Land `IxGetter.apply[S, A, I]` constructor.

**Files.** `core/src/main/scala/eo/optics/IxGetter.scala`.

**Approach.**
1. Define `type IxGetter[S, A, I]` alias.
2. `IxGetter.apply[S, A, I](f: S => (I, A))` — the single-focus
   indexed getter.
3. Extensions `.iget` via `IxAccessor`.

**Verification.** `sbt core/compile` green.

### Unit 6 — `IxLens` family

- [ ] Land `IxLens.apply[S, A, I]` constructor.

**Files.** `core/src/main/scala/eo/optics/IxLens.scala`.

**Approach.**
1. Define `type IxLens[S, A, I]` alias.
2. `IxLens.apply[S, A, I](get: S => (I, A), replace: (A, S) => S)` —
   the constructor shape.
3. Extensions `.iget`, `.ireplaceAt`, `.imodify` via
   `IxAccessor` + `IxForgetfulFunctor`.
4. Example: static-index `IxLens` over `(Int, String)` tuple
   (index = "first" / "second" as user-supplied String).

**Verification.** `sbt core/compile` green. Smoke: law suite passes.

### Unit 7 — Composition lattice (Ix + plain compositions)

- [ ] Land `Composer` instances for Ix + plain directions.
- [ ] Land `.forgetIndex` extension method.
- [ ] Land `.flattenIndex` extensions for arity 3 and 4.

**Files.**
- Modify `core/src/main/scala/eo/data/IxF.scala` — add `Composer[IxF, F]` / `Composer[F, IxF]` instances.
- Modify `core/src/main/scala/eo/optics/Optic.scala` — add
  `.forgetIndex` and `.flattenIndex*` extensions.

**Approach.**
1. For each plain carrier `F` in `{Tuple2, Forget[List], Forgetful}`:
   - `given [I]: Composer[F, IxF_for_F[I]]` — lift plain → indexed
     with "unit index at this level".
   - `given [I]: Composer[IxF_for_F[I], F]` — drop indexed → plain,
     discarding the index.
2. `extension (o: Optic[..., IxF[I]]) def forgetIndex: Optic[..., F]` —
   explicit opt-in lossy conversion.
3. `.flattenIndex3` / `.flattenIndex4` — hand-written tuple
   rearrangement. Arbitrary-depth deferred to v1.1.
4. Verify the `Morph[F, G]` auto-picking handles Ix + plain
   transparently (should follow from the `Composer.chain`
   derivation).

**Verification.** `sbt compile` green. Example specs:
```
val outer = IxTraversal.each[List, String]
val inner = lens[String](_.length)  // plain Lens
val composed = outer.andThen(inner)   // IxTraversal[List[String], Int, Int]
composed.imodify((i, len) => len + i)(List("a", "bb"))  // List("a"?? ignore semantics, compile gate)
```

### Unit 8 — Laws for all four v1 families

- [ ] Land `IxLensLaws` + `IxLensTests`, `IxTraversalLaws` +
  `IxTraversalTests`, `IxFoldLaws` + `IxFoldTests`, `IxGetterLaws` +
  `IxGetterTests`.
- [ ] Wire at least 5 `checkAll` invocations in
  `tests/src/test/scala/eo/OpticsLawsSpec.scala`.

**Files.**
- `laws/src/main/scala/eo/laws/IxLensLaws.scala`
- `laws/src/main/scala/eo/laws/IxTraversalLaws.scala`
- `laws/src/main/scala/eo/laws/IxFoldLaws.scala`
- `laws/src/main/scala/eo/laws/IxGetterLaws.scala`
- `laws/src/main/scala/eo/laws/discipline/IxLensTests.scala`
  (and the three siblings).
- Modify `tests/src/test/scala/eo/OpticsLawsSpec.scala`.

**Approach.** Per D8 above.

**Verification.** `sbt laws/compile` + `sbt tests/test` green.
Scoverage on `core` does not regress.

### Unit 9 — Benchmark

- [ ] Land `benchmarks/src/main/scala/eo/bench/IxTraversalBench.scala`.

**Files.** As named.

**Approach.** Per D9. Mirror `AlgLensBench.scala` annotations
(`@Fork(3)`, `@Warmup`, etc.). Document the Monocle-has-no-indexed
caveat in the class-level Scaladoc.

**Verification.** `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*IxTraversalBench.*"`
runs to completion. Commit message records numbers inline.

### Unit 10 — Docs

- [ ] Land `site/docs/indexed.md` (new page).
- [ ] Update `site/docs/concepts.md` with the indexed-carriers row.
- [ ] Optionally land a cookbook entry ("traverse JSON with path")
  if a clean mdoc example falls out.

**Files.** As named.

**Approach.** Per D10.

**Verification.** `sbt docs/mdoc` + `sbt docs/laikaSite` green.
Pre-commit hook passes.

## Risks & Dependencies

### Risks

- **Risk 1 (high). Opaque-type shielding fails the Unit 0 spike.**
  Mitigation: sealed-trait / AnyVal-wrapper fallback documented in D1
  and Unit 0. Plan pivots with a 1–2 day cost, doesn't fail.
- **Risk 2 (high). `AssociativeFunctor[IxF[I], Xo, Xi]` shape doesn't
  fit cats-eo's existing `AssociativeFunctor` typeclass.** The inner
  and outer need to share `I` in the standard signature, but we want
  cross-index composition. Mitigation: a new
  `IxAssociativeFunctor[F, Xo, Xi, Io, Ii]` typeclass is an escape
  hatch. If we take it, scope grows by ~3 units (the
  substrate enlargement + migration). **Flag as a real possible
  blocker** — if Unit 1 surfaces this, trigger a scope
  re-evaluation before continuing.
- **Risk 3 (medium). `IxForgetfulTraverse` instances for
  `Map[K, _]` / `ArraySeq` have semantic pitfalls.** Map iteration
  order is a known cats-ecosystem issue. Mitigation: `ListMap[K, _]`
  fixture for law-sensitive tests; document the unspecified-order
  caveat.
- **Risk 4 (medium). `imodify((i, a) => ...)` allocation per element
  exceeds the "2× linear" performance expectation.** Mitigation:
  Unit 9 bench reveals the ratio; if > 3×, scope a `PSVec`-style
  `IxPSVec` optimisation as a v1.1 task.
- **Risk 5 (low). `.flattenIndex` arity-3 / 4 extensions don't
  compile cleanly.** Mitigation: hand-written variants per D2,
  `Tuple.Concat`-based deferred to v1.1.
- **Risk 6 (medium). Scoverage regresses if the new typeclasses have
  unreachable code paths.** Mitigation: aim for 80%+ coverage on
  each new file; unreachable instance cases explicitly excluded with
  `// $COVERAGE-OFF$` markers.
- **Risk 7 (low). `site/docs/indexed.md` becomes stale when v1.1
  lands.** Mitigation: v1.1 plan explicitly includes a docs-page
  extension unit.

### Dependencies

- `cats-core_3:2.13.0` — already pinned; no version bump needed.
- `discipline-specs2_3:2.0.0` — already pinned; no version bump.
- Scala 3.8.3 opaque types — stable feature. Scala 3 `Tuple.Concat` /
  `Tuple.Fold` — stable in 3.8.x, used in Unit 7 flatten extensions.
- No new sbt plugins. No new runtime dependencies.
- No `build.sbt` restructure beyond adding source file paths (sbt
  picks these up automatically under the existing project layout).

## Documentation Plan

### Scaladoc on new files

Every public member under `eo.Ix*` and `eo.data.IxF` / `eo.optics.Ix*`
carries Scaladoc at the density of `AlgLens.scala`:

- Typeclass traits — signature + "unlocks which extension on
  `Optic`" + required evidence.
- Opaque types — encoding note, pointer to D1, comparison to the
  underlying plain carrier.
- Family-constructor objects (`IxTraversal`, `IxFold`, `IxGetter`,
  `IxLens`) — one runnable mdoc example per constructor.
- Law classes — one-line summary per equation, citing the source
  (Haskell `optics` / cats-eo-specific).

### `site/docs/indexed.md` — new page

Content outline per D10. Word budget: 1500–2000 words.

### `site/docs/concepts.md` — carriers-table rows

Three new rows (below the `AlgLens` row):

```
IxForget[F, I] | F[(I, A)]      | IxTraversal / IxFold
IxTuple2[I]    | (X, (I, A))    | IxLens
IxForgetful[I] | (I, A)         | IxGetter
```

### Cookbook — conditional entry

If Unit 10 finds a clean JSON-path example, add it. Otherwise defer.

### Migration-from-Monocle note

Not applicable — Monocle has no indexed optics. `indexed.md` mentions
this once in the "why this is useful" paragraph as context.

## Success Metrics

A successful landing satisfies:

1. `sbt compile` green across `core`, `laws`, `tests`, `generics`.
2. `sbt test` green, with ≥5 new `checkAll` invocations exercising
   the four v1 indexed families (e.g. `IxTraversalTests[List, Int]`,
   `IxTraversalTests[Vector, Int]`, `IxLensTests[(Int, Int), Int]`,
   `IxFoldTests[List, Int]`, `IxGetterTests[(Int, Int), Int]`).
3. `sbt scalafmtCheckAll` green.
4. Scoverage on `core` does not regress below 70%; the new indexed
   carrier files contribute fresh lines covered by the new law
   suite. Target: hold or improve the floor.
5. `sbt docs/mdoc` green — the new `indexed.md` compiles.
6. `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*IxTraversalBench.*"`
   runs to completion (smoke, not a perf gate).
7. `git log --oneline` for this plan lands as ~11 commits, one per
   Implementation Unit (Unit 0 spike + 10 body units), each passing
   pre-commit + pre-push hooks.
8. All four v1 families are reachable from the public API surface;
   a user following `indexed.md` can construct and modify an
   `IxTraversal`, `IxFold`, `IxGetter`, `IxLens` end-to-end without
   dropping to `private[eo]` extensions.

## Phased Delivery

The plan has four phases; each is re-evaluated at its end to decide
whether to continue or re-scope.

### Phase 1 — Substrate (Units 0–2)

Gate. Unit 0 spike confirms the D1 encoding decision; Units 1–2
land the typeclass substrate and the indexed carrier. **Phase 1
gate:** `sbt core/compile` green with the three new opaque types,
and `AssociativeFunctor[IxF[I], Xo, Xi]` shape confirmed compiling.

If Phase 1 reveals the `AssociativeFunctor` signature doesn't fit
(Open Question #2, Risk #2), **pause the plan** and re-scope —
likely by introducing a separate `IxAssociativeFunctor` typeclass
and budgeting additional units.

### Phase 2 — v1 families (Units 3–6)

Build out `IxTraversal`, `IxFold`, `IxGetter`, `IxLens`. Each is ~1
commit once substrate is in place. **Phase 2 gate:** all four
families have constructors and pass a one-line smoke test.

### Phase 3 — Composition lattice (Unit 7)

Land the cross-carrier composers (Ix + plain, plain + Ix) and the
`.forgetIndex` / `.flattenIndex` extensions. **Phase 3 gate:**
mixed-carrier composition chain compiles and executes correctly.

### Phase 4 — Laws + bench + docs (Units 8–10)

Quality pass. **Phase 4 gate:** `sbt test` green with indexed law
RuleSets, one bench class runs to completion, `indexed.md`
compiles.

**Release point.** After Phase 4, cats-eo can ship an `0.2.0` (or
`0.1.1`) release with the v1 indexed-optics hierarchy. The v1.1
families scope as a separate future plan.

## Future Considerations

### v1.1 families — `IxPrism`, `IxAffineTraversal`, `IxSetter`, `IxAffineFold`

Scoped as a separate follow-up plan. Expected scope: ~4 units (one
per family) + 1 unit for docs page extension. Each family reuses
the v1 substrate — no new typeclasses, no new carriers beyond
`IxEither[I]` / `IxAffine[I]` / `IxSetterF[I]` opaque types.

Gate on v1 adoption: ship v1.1 once external users request the
additional families, or once cats-eo internal examples benefit.

### Macro derivation — `eo.generics.ilens`, `eo.generics.itraversal`

Scoped as a generics-follow-up plan once v1 hand-written indexed
surface settles. Expected scope: ~6 units. Target: zero-config
derivation for case classes with `Int`-indexed fields (by field
order) and `String`-indexed fields (by field name).

### Cross-traversal index alignment — `izipOf`

Haskell `optics`-style "zip two indexed folds by their indices" —
useful for diff-shaped operations over indexed structures. Needs
new machinery (an `IxZip` typeclass, likely); defer until a real
use case surfaces.

### Index polymorphism — `Optic.reindex[J](f: I => J)`

`Contravariant`-style `reindex` — lets users re-tag an indexed
optic's index type. Small extension (one method) but depends on D2
being fully settled. Ship in v1 if the implementation drops out
cleanly; otherwise defer to v1.1.

### Indexed Grate / Kaleidoscope

Pending landings of plain Grate
(`docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`) and
Kaleidoscope (survey priority #6). Indexed variants can't exist
before the plain families; schedule after.

### `IxPSVec`-specialised carrier

If Unit 9 reveals unacceptable allocation overhead, port the
`PSVec` optimisation story — `IxPSVec` as a dense
`Array[AnyRef]`-backed carrier with indices packed alongside foci.
Follow the `PowerSeries` precedent. Scope: ~3 units once the
baseline bench justifies it.

### Deepening `IxTraversalBench` with a paired research doc

If Unit 9 numbers surprise (in either direction — faster or slower
than the zipWithIndex baseline), promote to a separate
`docs/research/2026-MM-DD-ixtraversal-vs-ziplike.md` research doc,
mirroring the AlgLens-vs-PowerSeries precedent.

### Cookbook — indexed examples library

Once 2–3 real user patterns materialise around the indexed surface,
add a cookbook chapter — candidates include JSON-path walking,
form-validation error aggregation keyed by field path, tree
rewriting with depth information, CSV cell-by-cell transform.

## Sources & References

- **Optic-families survey** —
  `docs/research/2026-04-19-optic-families-survey.md`, Indexed
  variants section (lines 43–76).
- **AlgLens carrier (closest template)** —
  `core/src/main/scala/eo/data/AlgLens.scala`.
- **Grate plan (single-family format reference)** —
  `docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`.
- **Production-readiness plan (big-plan format reference)** —
  `docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md`.
- **Haskell `optics` — Optics.IxLens** —
  <https://hackage.haskell.org/package/optics-core-0.4.1.1/docs/Optics-IxLens.html>.
  Source of `iview` / `iset` / `ioverA` naming.
- **Haskell `optics` — Optics.IxTraversal** —
  <https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-IxTraversal.html>.
- **Haskell `optics` — Optics.IxFold** —
  <https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-IxFold.html>.
- **Haskell `lens` — Control.Lens.Indexed** —
  <https://hackage.haskell.org/package/lens-4.15.1/docs/Control-Lens-Indexed.html>.
  Earlier formulation; `Indexable` / `Conjoined` / `Indexed`
  shapes.
- **Clarke et al. — *Profunctor Optics: a Categorical Update*** —
  <https://arxiv.org/abs/2001.07488>. Indexed-profunctor framing.
- **Pickering, Gibbons, Wu — *Profunctor Optics: Modular Data Accessors*** —
  <https://www.cs.ox.ac.uk/people/jeremy.gibbons/publications/poptics.pdf>.
  Section on indexed variants as a categorical extension.
- **cats-core 2.13.0** — verified via `cellar list-external
  org.typelevel:cats-core_3:2.13.0 cats` + filter: no
  `FunctorWithIndex` / `FoldableWithIndex` /
  `TraversableWithIndex` typeclasses shipped (substrate correction
  noted in *Context & Research*).
- **alleycats-core 2.13.0** — verified same way; no `*WithIndex`
  typeclasses.
- **Monocle 3.3.0** — ships `monocle.function.Index` /
  `monocle.function.FilterIndex` typeclasses whose methods return
  plain `Optional` / `Traversal`; does NOT ship a first-class
  indexed-optic family. Verified via `cellar search-external
  dev.optics:monocle-core_3:3.3.0 Index`. No Monocle baseline
  available for benchmarks.
- **Scala 3 opaque types** — Scala 3.8.3 standard feature; opaque
  type aliasing mechanism for D1's carrier encoding.
- **Scala 3 `Tuple.Concat` / `Tuple.Fold`** — standard library,
  available in 3.8.3; optional machinery for arbitrary-depth
  `.flattenIndex` (deferred to v1.1 per D8 / Open Question #8).
