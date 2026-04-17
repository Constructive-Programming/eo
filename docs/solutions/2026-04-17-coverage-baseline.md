---
title: "Coverage baseline for cats-eo core (pre-Unit 3)"
date: 2026-04-17
type: measurement
status: baseline
---

# Coverage baseline — `core/` statements + branches

Measurement artefact captured right after Unit 1 (laws reorganisation)
and before Unit 3 (new law classes). Feeds Unit 8's targeted gap-fill.

## How it was measured

```sh
sbt "clean; coverage; tests/test; coverageReport"
```

Tests exercised: `tests/src/test/scala/eo/*Spec.scala` plus
`core/src/test/scala/eo/FoldSpec.scala` (runs transitively during
`tests/test`). `generics/` tests were **not** included in the baseline
since they do not touch `core/` internals at the level of the laws
module.

Only `eo/` production sources are counted; `core/src/test/` is excluded
from the `<class>` entries by scoverage automatically.

## Headline numbers

| Module | Statement | Branch |
|--------|-----------|--------|
| `core` | **38.58 %** | **23.44 %** |
| `laws` | 100.00 % | 100.00 % |

The plan's prose ("~70 %% of core") was aspirational — the law suite
exercises most public *optic* methods (which reads as "high coverage"
by eye) but large swathes of the *carrier* machinery (Vect, PowerSeries,
FixedTraversal, ForgetfulTraverse) have no dedicated tests and show
red in the report. Units 3–7 are what close that gap.

## Per-file breakdown

Classification legend:

- **feature** — runtime code with non-trivial behaviour; aim ≥85 %
  statement coverage after Unit 8.
- **type-level-only** — file is almost entirely `given` instances or
  phantom-typed declarations; runtime statement count is noisy and the
  ≥85 % floor is not meaningful.
- **mixed** — file has both feature code and type-level scaffolding;
  per-file target is set case by case.

| File | Stmt | Branch | Stmts (hit/total) | Branches (hit/total) | Class | Target |
|------|------|--------|-------------------|----------------------|-------|--------|
| `eo/Accessors.scala` | 100 % | n/a | 3/3 | 0/0 | feature | keep ≥85 % |
| `eo/AssociativeFunctor.scala` | 80 % | 75 % | 33/41 | 6/8 | mixed | lift to ≥85 % via Unit 7 (typeclass laws) |
| `eo/Composer.scala` | 78 % | n/a | 15/19 | 0/0 | feature | lift to ≥85 % via Unit 7 |
| `eo/ForgetfulApplicative.scala` | 0 % | n/a | 0/5 | 0/0 | feature (thin) | lift via Unit 7 fixture exercising Applicative path |
| `eo/ForgetfulFold.scala` | 57 % | n/a | 8/14 | 0/0 | feature | lift via Unit 3 (FoldLaws), Unit 7 |
| `eo/ForgetfulFunctor.scala` | 53 % | n/a | 8/15 | 0/0 | feature | lift via Unit 7 |
| `eo/ForgetfulTraverse.scala` | 13 % | n/a | 4/29 | 0/0 | feature | lift via Unit 7 — biggest Unit 7 pay-off file |
| `eo/data/Affine.scala` | 49 % | 33 % | 54/109 | 3/9 | feature | lift to ≥85 % via Unit 4 (AffineLaws) |
| `eo/data/FixedTraversal.scala` | 61 % | 66 % | 11/18 | 2/3 | feature | lift to ≥85 % via Unit 6 |
| `eo/data/Forgetful.scala` | 25 % | n/a | 7/27 | 0/0 | feature | lift via Unit 7 (carrier typeclass laws) |
| `eo/data/PowerSeries.scala` | 16 % | 25 % | 12/72 | 2/8 | feature | lift to ≥85 % via Unit 6 |
| `eo/data/SetterF.scala` | 75 % | n/a | 3/4 | 0/0 | feature (thin) | lift to ≥85 % via Unit 4 (SetterFLaws) |
| `eo/data/Vect.scala` | **0 %** | **0 %** | 0/131 | 0/33 | feature | lift to ≥85 % via Unit 5 — largest lift in the plan |
| `eo/optics/Fold.scala` | 100 % | n/a | 4/4 | 0/0 | feature | keep ≥85 % |
| `eo/optics/Getter.scala` | 100 % | n/a | 2/2 | 0/0 | feature | keep ≥85 % |
| `eo/optics/Iso.scala` | 100 % | n/a | 2/2 | 0/0 | feature | keep ≥85 % |
| `eo/optics/Lens.scala` | 62 % | n/a | 15/24 | 0/0 | feature | lift via Units 3/4/7 (incidentally exercises more constructors) |
| `eo/optics/Optic.scala` | 48 % | 66 % | 63/131 | 2/3 | feature | lift via all of Units 3–7 |
| `eo/optics/Optional.scala` | 100 % | n/a | 2/2 | 0/0 | feature | keep ≥85 % |
| `eo/optics/Prism.scala` | 75 % | n/a | 6/8 | 0/0 | feature | lift via Unit 7 |
| `eo/optics/Setter.scala` | 100 % | n/a | 2/2 | 0/0 | feature | keep ≥85 % |
| `eo/optics/Traversal.scala` | 50 % | n/a | 6/12 | 0/0 | feature | lift via Unit 6 (PowerSeriesSpec exercises `powerEach`) |
| `eo/PowerSeries.scala` | n/a | n/a | — | — | stub | empty file; pre-existing scalafmt warning |

## Biggest-lift files

Ordered by missing-statements contribution to the overall 38.58 %
number:

1. **`data/Vect.scala`** — 131 missing statements, 33 missing branches. 0 % baseline.
   Owned by Unit 5.
2. **`data/PowerSeries.scala`** — 60 missing statements, 6 missing branches. 16 % baseline.
   Owned by Unit 6.
3. **`optics/Optic.scala`** — 68 missing statements. 48 % baseline.
   Shared across Units 3, 6, 7 (every new law class exercises more of
   `Optic`'s extension-method surface).
4. **`data/Affine.scala`** — 55 missing statements, 6 missing branches. 49 % baseline.
   Owned by Unit 4.
5. **`data/Forgetful.scala`** — 20 missing statements. 25 % baseline.
   Owned by Unit 7.
6. **`ForgetfulTraverse.scala`** — 25 missing statements. 13 % baseline.
   Owned by Unit 7.

## Files already at or above the ≥85 % target

- `eo/Accessors.scala` (100 %)
- `eo/optics/Fold.scala` (100 %)
- `eo/optics/Getter.scala` (100 %)
- `eo/optics/Iso.scala` (100 %)
- `eo/optics/Optional.scala` (100 %)
- `eo/optics/Setter.scala` (100 %)

These thin optic wrapper files already enjoy high coverage because
their lawful behaviour is exercised through the per-optic discipline
suites in `tests/src/test/scala/eo/OpticsLawsSpec.scala`. Unit 8
should not regress them.

## Target after Units 3–7

Based on the individual unit verifications in the plan, expected
per-file coverage after those units land:

| File | Baseline | Expected after | Owner |
|------|----------|----------------|-------|
| `data/Vect.scala` | 0 % | ≥85 % | Unit 5 |
| `data/PowerSeries.scala` | 16 % | ≥85 % | Unit 6 |
| `data/FixedTraversal.scala` | 61 % | ≥85 % | Unit 6 |
| `data/Affine.scala` | 49 % | ≥85 % | Unit 4 |
| `data/SetterF.scala` | 75 % | ≥85 % | Unit 4 |
| `ForgetfulTraverse.scala` | 13 % | ≥80 % | Unit 7 |
| `ForgetfulFunctor.scala` | 53 % | ≥85 % | Unit 7 |
| `ForgetfulFold.scala` | 57 % | ≥85 % | Units 3 + 7 |
| `ForgetfulApplicative.scala` | 0 % | ≥70 % | Unit 7 (thin file; one fixture suffices) |
| `data/Forgetful.scala` | 25 % | ≥80 % | Unit 7 |
| `AssociativeFunctor.scala` | 80 % | ≥85 % | Unit 7 |
| `Composer.scala` | 78 % | ≥85 % | Unit 7 |
| `optics/Traversal.scala` | 50 % | ≥85 % | Unit 6 |
| `optics/Prism.scala` | 75 % | ≥85 % | Unit 7 |
| `optics/Lens.scala` | 62 % | ≥85 % | Units 3/4 |
| `optics/Optic.scala` | 48 % | ≥80 % | transitively via Units 3–7 |

**Overall core target after Unit 8:** ≥85 % statement, ≥60 % branch.

The branch-coverage target is intentionally below the statement target
because scoverage 2.4 under-reports branches on Scala 3 match types
and inline givens — this is a documented noise source (see plan
decision D10). Units 3–7 should drive branch coverage well above the
current 23 %, but enforcing an 80 % branch floor would force noisy
per-file exemptions.

## Re-measurement command

Unit 8 should update this file with the post-work numbers by re-running:

```sh
sbt "clean; coverage; tests/test; coverageReport"
```

then re-sorting the per-file table.
