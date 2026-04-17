---
title: "Coverage baseline for cats-eo core — post-Phase-1"
date: 2026-04-17
type: measurement
status: post-units-1-through-8
---

# Coverage baseline and lift report — `core/`

Measurement artefact from the production-readiness plan. Captures the
baseline right after Unit 1 (laws reorganisation) and the final
numbers after Units 3–8 added discipline-checked law classes, carrier
laws, type-class laws, and targeted fixtures.

## How it was measured

```sh
sbt "clean; coverage; tests/test; coverageReport"
```

Tests exercised: `tests/src/test/scala/eo/*Spec.scala` plus
`core/src/test/scala/eo/FoldSpec.scala` (runs transitively during
`tests/test`). `generics/` tests were **not** included because they
do not touch `core/` internals at the level of the laws module.

## Headline numbers

| Module | Baseline (Unit 2) | After Phase 1 (Units 3–8) | Lift |
|--------|------------------:|---------------------------:|-----:|
| `core` statement | 38.58 % | **68.30 %** | +29.7 pts |
| `core` branch    | 23.44 % | **70.77 %** | +47.3 pts |
| `laws` statement | 100.00 % | 100.00 % | — |
| `laws` branch    | 100.00 % | 100.00 % | — |

The plan's ≥85 % statement target is not met at 0.1.0. See the "Still
open at 0.1.0" section below for the honest picture of which files
need more fixtures and why they were scope-deferred.

## Per-file breakdown (post-Unit-8)

| File | Stmt | Branch | Class | Target | Status |
|------|------|--------|-------|--------|--------|
| `eo/Accessors.scala` | 100 % | n/a | feature | ≥85 % | ✅ at target |
| `eo/ForgetfulFold.scala` | 100 % | n/a | feature | ≥85 % | ✅ lifted from 57 % |
| `eo/optics/Fold.scala` | 100 % | n/a | feature | ≥85 % | ✅ |
| `eo/optics/Getter.scala` | 100 % | n/a | feature | ≥85 % | ✅ |
| `eo/optics/Iso.scala` | 100 % | n/a | feature | ≥85 % | ✅ |
| `eo/optics/Optional.scala` | 100 % | n/a | feature | ≥85 % | ✅ |
| `eo/optics/Setter.scala` | 100 % | n/a | feature | ≥85 % | ✅ |
| `eo/data/FixedTraversal.scala` | 100 % | 100 % | feature | ≥85 % | ✅ lifted from 61 % |
| `eo/optics/Traversal.scala` | 83 % | 100 % | feature | ≥85 % | close (fused hot path) |
| `eo/data/PowerSeries.scala` | 81 % | 100 % | feature | ≥85 % | close; lifted from 16 % |
| `eo/AssociativeFunctor.scala` | 80 % | 75 % | mixed | ≥85 % | close; match-type paths |
| `eo/Composer.scala` | 78 % | 100 % | feature | ≥85 % | close; chain variant unused at 0.1.0 |
| `eo/optics/Prism.scala` | 75 % | 100 % | feature | ≥85 % | close |
| `eo/data/SetterF.scala` | 75 % | n/a | feature (thin) | ≥85 % | close |
| `eo/data/Vect.scala` | 70 % | 67 % | feature | ≥85 % | close; lifted from 0 % |
| `eo/data/Affine.scala` | 67 % | 44 % | feature | ≥85 % | lifted from 49 % |
| `eo/optics/Lens.scala` | 62 % | n/a | feature | ≥85 % | `SimpleLens` / `SplitCombineLens` under-exercised |
| `eo/optics/Optic.scala` | 55 % | 66 % | feature | ≥80 % | many-extension file; biggest absolute gap |
| `eo/ForgetfulFunctor.scala` | 53 % | n/a | feature | ≥85 % | SetterF / FixedTraversal mappings under-exercised |
| `eo/ForgetfulTraverse.scala` | 44 % | n/a | feature | ≥80 % | `tupleFTraverse` (Functor not Applicative) isn't reachable through current ForgetfulTraverseLaws (which pins `Applicative`) |
| `eo/data/Forgetful.scala` | 25 % | n/a | feature | ≥80 % | `bifunctor` / left/right-only `AssociativeFunctor` givens not witnessed |
| `eo/ForgetfulApplicative.scala` | 0 % | n/a | feature (thin) | ≥70 % | only one given (`Forget[F]`); covered by the Fold-level `pure` test but that test still doesn't cross into this file (scoverage counts the `object ForgetfulApplicative` wrapper as uncovered) |

## Still open at 0.1.0 — deferred to 0.1.1 or later

The files below are **feature code** (not pure type-level machinery)
and should reach ≥85 % at 0.1.1. Each is tagged with the specific gap
that the next pass should close:

| File | Baseline | After Phase 1 | Gap to close next |
|------|---------:|---------------:|-------------------|
| `Forgetful.scala` | 25 % | 25 % | Add `Bifunctor[Forget[F]]` spec; add `leftAssocForget` / `rightAssocForget` witnesses through a Composer chain that hits FlatMap-only vs Comonad-only paths. |
| `ForgetfulTraverse.scala` | 13 % | 44 % | Add `ForgetfulTraverseLaws[F, Functor]` (separate trait from the `[Applicative]` variant); wire a Tuple2 fixture through it. |
| `ForgetfulFunctor.scala` | 53 % | 53 % | SetterF and FixedTraversal functor fixtures that exercise `map` instead of only `identity`. |
| `Optic.scala` | 48 % | 55 % | Extensions `modifyF`, `all`, `transfer`, `transform` are exercised but not on every carrier; widen to Affine / PowerSeries. |
| `Lens.scala` | 62 % | 62 % | `Lens.curried` / `Lens.pCurried` / `SimpleLens` / `SplitCombineLens` spec. |
| `Affine.scala` | 49 % | 67 % | `affine.fold` + `aFold` with exhaustive Left / Right / Nested right fixtures. |

**Why the ≥85 % target is not met at 0.1.0**: the plan's aspirational
≥85 % floor assumed a starting point of ~70 % statement coverage. The
real baseline was 38.58 %. Units 3–8 doubled the coverage, picked up
the biggest concrete wins (Vect, PowerSeries, FixedTraversal,
ForgetfulFold), but the remaining gaps are mostly about type-class
instance breadth — which is easy incremental work for 0.1.1 and does
not need to gate the first release.

## Re-measurement command

```sh
sbt "clean; coverage; tests/test; coverageReport"
```

After each 0.1.x PR that adds a fixture, re-run and update this table.
