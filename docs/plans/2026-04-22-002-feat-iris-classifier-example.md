---
title: "feat: Iris classifier example — AlgebraicLens.{listLens, alternativeLens} + docs"
type: feat
status: deferred
date: 2026-04-22
---

# feat: Iris classifier example — AlgebraicLens smart constructors + Iris tutorial

## Overview

Build on the `AlgLens[F]` carrier (shipped 2026-04-22) by adding the
user-facing algebraic-lens surface and a full worked example: a Fisher Iris
classifier written from cats-eo's own tools. Three tracks, each small:

1. **`AlgebraicLens` companion with smart constructors** — `listLens` and
   `alternativeLens` pinned variants, built on top of
   `AlgLens.fromLensF` / `fromPrismF` / `fromOptionalF`. No new carrier; this
   is purely the ergonomic surface users reach for.
2. **Iris classifier example** — `tests/src/test/scala/eo/examples/IrisClassifierSpec.scala`
   encoding a multi-class classifier over the Fisher Iris dataset (sepal/petal
   length+width → species) using composed `AlgLens[List]` optics. The
   classifier IS the optic.
3. **Narrative docs** — `site/docs/examples/iris-classifier.md` walking the
   reader from "plain Scala classifier" to "optic-shaped classifier" with
   compile-time-verified snippets via mdoc. Added to `site/docs/examples/directory.conf`
   so Laika picks it up in the left nav.

## Why this is deferred

This plan was extracted from the inline `#105` / `#106` tasks on
2026-04-22 after the `AlgLens[F]` carrier and bridges landed. Execution was
deferred to first validate that `AlgLens[F]` genuinely earns its keep vs.
`PowerSeries` — see the "Open Questions" section. If that analysis concludes
`AlgLens[F]` is redundant or strictly slower than `PowerSeries` for the
traversal-shape common case, this plan should be reconsidered (either
narrowed to just genuine algebraic-lens use cases, or shelved entirely).

## Requirements Trace

- **R1.** `dev.constructive.eo.optics.AlgebraicLens` object exists with `listLens[S, A]` and
  `alternativeLens[F: Alternative, S, A]` constructors. Both return
  `Optic[S, S, A, A, AlgLens[F]]` (or polymorphic variant).
- **R2.** At least one nontrivial end-to-end example spec under
  `tests/src/test/scala/eo/examples/` that uses these constructors and
  composes them with plain Lens / Prism / Optional.
- **R3.** Mdoc-verified prose doc under `site/docs/examples/` with left-nav entry.

## Scope Boundaries

- Not adding new carriers or law classes. `AlgLens[F]` already carries its
  own Bifunctor / ForgetfulFunctor / ForgetfulFold / ForgetfulTraverse /
  AssociativeFunctor instances and three Composer bridges — no additional
  typeclass wiring.
- Not rewriting the production-readiness laws plan. If `AlgebraicLens` earns
  its own law class (e.g. `AlgebraicLensLaws`) that goes in a follow-up.
- Not sweeping the broader docs site; this is a single new example page.

## Implementation Units

- [ ] **Unit 1: `AlgebraicLens` object with smart constructors**

**Goal:** Expose a user-facing surface for algebraic lenses so users reach for
`AlgebraicLens.listLens(...)` instead of manually calling `AlgLens.fromLensF`.

**Files:**
- Create: `core/src/main/scala/eo/optics/AlgebraicLens.scala` — companion object
  with `listLens`, `alternativeLens` constructors.

**Approach:**
- `listLens[S, A](get: S => List[A], enplace: (S, List[A]) => S)` — construct
  a plain `Lens[S, List[A]]` internally, then lift via `AlgLens.fromLensF`.
  Return type: `Optic[S, S, A, A, AlgLens[List]]`.
- `alternativeLens[F[_]: Alternative, S, A](get: S => F[A], enplace: (S, F[A]) => S)`
  — same pattern but generic in `F`.
- Both should be monomorphic (`S = T`, `A = B`) in the first cut. Polymorphic
  variants (`pListLens`, etc.) can come later if there's demand.

**Execution note:** test-first. Write a spec that exercises both constructors,
then fill them in.

**Verification:**
- `sbt core/compile` green.
- Spec with constructors + composition with plain Lens passes.

- [ ] **Unit 2: Iris classifier example spec**

**Goal:** Encode the Fisher Iris classifier as a composed algebraic lens, end
to end.

**Files:**
- Create: `tests/src/test/scala/eo/examples/IrisClassifierSpec.scala`.
- Create: `tests/src/test/scala/eo/examples/iris.csv` (or hard-code a small
  fixture dataset; the real 150-row Iris set is public domain but pulling it
  in as a test resource is a bigger lift).

**Approach:**
- Encode feature types as case classes (`Measurement`, `IrisSpecies`).
- The classifier: `measurementLens.andThen(classifierAlg).andThen(speciesLens)`
  where `classifierAlg: AlgLens[List][Measurement, IrisSpecies]` produces
  candidate species by proximity to centroids.
- Spec-level assertions: given a set of measurements, the classified species
  match known expected values. Also: composition round-trip laws on the
  classifier itself.

**Verification:**
- `sbt tests/test` green with new spec.

- [ ] **Unit 3: Narrative docs**

**Goal:** Walk the reader from a plain classifier to an optic-shaped
classifier, highlighting where algebraic lens semantics shine.

**Files:**
- Create: `site/docs/examples/iris-classifier.md` — mdoc-verified.
- Modify: `site/docs/examples/directory.conf` — add the new page to left nav.

**Approach:**
- Start with naive classifier code (no optics).
- Introduce `AlgLens[List]` as "a lens whose focus is many values".
- Show composition with plain Lens on either end.
- End with the full Iris example as a short snippet + link to the full spec.

**Verification:**
- `sbt site/mdoc` green (snippets compile).
- Local Laika preview renders the new page in the left nav.

## Dependencies

- `AlgLens[F]` carrier + `fromLensF` / `fromPrismF` / `fromOptionalF`
  factories (shipped 2026-04-22). This plan depends on those being in place.
- Cats `Alternative` for the `alternativeLens` signature.

## Open Questions

- **[Resolved 2026-04-22]** *Is `AlgLens[F]` sufficiently distinct from
  `PowerSeries` to warrant the `AlgebraicLens` surface?* Answered in
  `docs/research/2026-04-22-alglens-vs-powerseries.md`: on the
  traversal-shape common case (same as `Traversal.each`) `AlgLens[F]` is
  1.5–2.6× slower than `PowerSeries`, so `AlgLens.fromLensF` must NOT
  be recommended as a general `Traversal.each` replacement. `AlgLens[F]`
  earns its keep only on cases `PowerSeries` cannot model: non-uniform
  classifier cardinality, `F` that isn't a traversable container, and
  `Prism`/`Optional` with `F[A]` focus via `fromPrismF` /
  `fromOptionalF`. **Implication:** before Unit 1 proceeds, the Iris
  example must be reframed around a genuine varying-cardinality
  classifier (adaptive-k KNN, one-vs-rest over a variable reference
  set) — a vanilla "traversal over a list of samples" framing would
  undersell the carrier. The `AlgebraicLens.listLens` smart constructor
  (R1) should ship with a docstring that explicitly steers users to
  `Traversal.each` for the non-varying case.
- **Should this example live in `tests/` or `benchmarks/`?** A companion
  benchmark comparing `AlgLens[List]` vs `Traversal.each[List, _]` on the
  same fixture already exists (`benchmarks/src/main/scala/eo/bench/AlgLensBench.scala`)
  and covers the perf question. The example itself stays in `tests/`.

## Phased Delivery

1. Resolve the open-question comparison (see the separate "AlgLens vs
   PowerSeries" analysis — 2026-04-22).
2. Unit 1 if the comparison justifies it.
3. Unit 2 + Unit 3 as a single landing.

## Sources & References

- Román, Clarke, Elkins, Gibbons, Milewski, Loregian, Pillmore — *Profunctor
  Optics, a Categorical Update* (NWPT2019, arXiv:2001.07488).
- Chris Penner — *Algebraic lenses* (exposition).
- Fisher, R.A. — *The use of multiple measurements in taxonomic problems*
  (1936).
