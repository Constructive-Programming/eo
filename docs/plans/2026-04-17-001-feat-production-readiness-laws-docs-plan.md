---
title: "feat: Production-readiness — Laws, Tests, Docs, Benchmarks, and 0.1.0 Release"
type: feat
status: active
date: 2026-04-17
revised: 2026-04-24
---

# feat: Production-readiness — Laws, Tests, Docs, Benchmarks, and 0.1.0 Release

## Overview

Bring `cats-eo` from an internal-feeling snapshot (`0.1.0-SNAPSHOT`) to a first
public Maven Central release (`0.1.0`) suitable for external adoption. The work
is structured as five complementary tracks, executed in sequence so each track
lands on a firm base:

1. **Laws & discipline restructure** — split `OpticLaws.scala` / `EoSpecificLaws.scala` into
   idiomatic `FooLaws` (equations) + `FooTests` (discipline RuleSet) pairs, one
   file per abstraction, mirroring cats / Monocle.
2. **Coverage closure** — add the missing law classes (`Getter`, `Fold`,
   `Affine`, `SetterF`, `Vect`, `PowerSeries`, and the `Forgetful*` carrier
   type-classes) and the matching property-test wiring so every public type
   carries a discipline-checked law set. Lift scoverage on `core` from ~70% to
   ≥85% statement coverage.
3. **Documentation track** — introduce an sbt-typelevel-site sub-project
   (Laika + mdoc), populate it with a guided tour, concept pages, API
   reference, and mdoc-verified runnable examples. Rewrite the README. Raise
   Scaladoc to full-release quality (`-groups`, `@tparam S/T/A/B`, examples)
   across the public surface.
4. **Benchmarks expansion** — add `Fold`, `Getter`, `Optional`, `Setter`, and
   `PowerSeries` benches, following Monocle's trait-per-optic +
   `Nested0…Nested6` fixture pattern. Keep JMH in its own sub-project, not on
   CI critical path.
5. **Release infrastructure** — adopt `sbt-typelevel-ci-release` (Sonatype
   Central Portal flow post-June-2025 OSSRH sunset), wire a JDK 17 / 21 CI
   matrix with scalafmt / scalafix / scoverage / mdoc / MiMa gates, write a
   `CHANGELOG.md`, and cut the 0.1.0 tag.

The entire plan is scoped for a pre-1.0 release — MiMa is introduced but not
enforced on the 0.1.0 tag itself; it becomes a gate on 0.1.1 and forward
within the `0.1.x` series.

## Problem Frame

> **2026-04-24 revision note.** The library has grown substantially since this
> plan's initial baseline (2026-04-17). New optic families have landed on `main`:
> `AlgLens[F]` (classifier carrier + `algFold` downstream), `AffineFold` (folded
> into `Optional`'s constructor surface), `Grate` (plan 004 complete: carrier +
> laws + bench + docs), circe multi-field optics and observable-failure Ior
> surface (plan 005 complete), `lens` / `prism` generics macros with multi-field
> NamedTuple focus + full-cover Iso (plan 003 complete), and an in-flight
> Kaleidoscope family (plan 006, 4 of 7 units landed — see §"Kaleidoscope
> in-flight coordination" below). The 0.1.0 target now includes laws, docs,
> and composition coverage for *these* new families, not just the pre-plan
> baseline. Three review inputs — a 15×15 composition-gap matrix
> ([`2026-04-23-composition-gap-analysis.md`](../research/2026-04-23-composition-gap-analysis.md)),
> a simplicity-lens code-quality review
> ([`2026-04-23-code-quality-review.md`](../research/2026-04-23-code-quality-review.md)),
> and a cookbook + diagrams research sweep
> ([`2026-04-23-external-sources-cookbook-ideas.md`](../research/2026-04-23-external-sources-cookbook-ideas.md))
> — define the additional units needed before tag; they are integrated into
> the unit list below.

The library has strong bones (rigorous existential-optics core, working Scala 3
macros, discipline-checked core optics, JMH suite vs Monocle for Lens / Prism /
Iso / Traversal) but is invisible as a public artifact:

- **No public release path.** `build.sbt` targets `0.1.0-SNAPSHOT`, no
  publishing config, no CI, no MiMa, no release automation. Any external user
  would need to clone + `sbt publishLocal`.
- **Documentation is absent.** `README.md` is the stock sbt 3-line stub. There
  is no `docs/`, no examples folder, no tutorial. The rich design notes live
  only in `CLAUDE.md` (agent-facing) and in Scaladoc on a few files.
  Newcomers cannot learn the library from the repository alone.
- **Law and test coverage has grown uneven** as the code grew. Post-PowerSeries
  additions (`Vect`, `PowerSeries`, `FixedTraversal`, richer `Affine` /
  `SetterF` / `Composer`) landed with behavior examples (`Unthreaded.scala`) but
  without law classes. The existing `OpticsLawsSpec` does not instantiate laws
  for `Getter`, `Fold`, `Affine`, `SetterF`, `Vect`, `PowerSeries`, or the
  `Forgetful*` type-classes.
- **Law organization does not match conventions.** `OpticLaws.scala` and
  `EoSpecificLaws.scala` bundle many `XxxLaws` traits + `XxxTests` RuleSets in
  a single file per module — fine while iterating internally, awkward for
  downstream discoverability and for downstream projects that want to depend on
  `cats-eo-laws`.
- **No changelog, no release notes, no version policy.** A public release needs
  "what's in it" and "what's guaranteed" artifacts.

The upstream brainstorm-equivalent was the in-conversation request: "let's go
over coverage again, add laws for PowerSeries, seriously improve documentation,
get ready for production — use Monocle and Cats as inspiration." The user has
chosen the maximal bundle (laws + tests + docs + release infra + benchmarks
expansion), docs-via-mdoc+Laika, `0.1.0` on Maven Central with pre-1.0
semantics, and full laws for PowerSeries / Vect. This plan honors those
choices directly.

## Requirements Trace

Requirements the plan must satisfy end-to-end:

- **R1. Every public type in `core/` carries a discipline-checked law set.**
  Explicitly:
  - **Optics** — `Iso`, `Lens`, `Prism`, `Optional`, `Setter`, `Traversal`,
    `Getter`, `Fold`.
  - **Data carriers** — `Affine`, `SetterF`, `Vect`, `PowerSeries`,
    `FixedTraversal`. (`Forgetful` is a type alias over `Id`; its behavior is
    exercised through the type-classes that use it, not a standalone law class.)
  - **Type-classes** — `ForgetfulFunctor`, `ForgetfulTraverse`,
    `AssociativeFunctor`, `Composer`. (The smaller type-classes
    `ForgetfulFold`, `ForgetfulApplicative`, `Accessor`, `ReverseAccessor`
    are covered by the optic-level laws that consume them — see Unit 7
    for the concrete scope; dedicated law classes are deferred to 0.1.1
    unless a second carrier lands.)

  "Discipline-checked law set" means a `FooLaws` trait (equations) plus a
  `FooTests` abstract class (discipline `RuleSet`) plus at least one
  `checkAll(...)` instantiation in `tests/`.
- **R2. Laws module is idiomatically structured** — `FooLaws` (equations)
  under `eo.laws.*`, `FooTests` (discipline RuleSet) under
  `eo.laws.discipline.*`. Cohesively grouped per abstraction, not
  one-equation-per-file. Matches cats / Monocle conventions. The published
  `cats-eo-laws` artifact is usable by downstream projects.
- **R3. `core` statement coverage ≥85%** (up from ~70% today) as measured
  by `sbt "clean; coverage; tests/test; coverageReport"`. Branch coverage
  is reported but not floored — scoverage 2.4.x under-reports branches on
  match types and inline givens, which we use heavily.
- **R4. A user can learn the library from the repository alone** via a
  `README.md` with navigation, a mdoc-verified `docs/` microsite built by
  `sbt-typelevel-site` (Laika), and an `examples/` folder. The guided tour
  covers: getting started, core concepts (existential optics vs classical
  profunctor encoding), each optic with runnable examples, the generics
  module, and a migration-from-Monocle appendix.
- **R5. All public surface has Scaladoc that compiles with `-groups`** and
  includes `@tparam` for every type parameter on `Optic`, its companion, and
  on every optic / data-carrier / type-class public member. `@example` blocks
  are welcome where they clarify usage (and can point at the microsite for
  deeper examples).
- **R6. The project publishes `cats-eo`, `cats-eo-laws`, and
  `cats-eo-generics` to Maven Central (via the Central Portal)** at version
  `0.1.0` from a git tag, signed, with source and Scaladoc jars, via
  `sbt-typelevel-ci-release`.
- **R7. CI enforces on every PR**: compile (JDK 17 + JDK 21), `sbt test`,
  `scalafmtCheckAll` + `scalafmtSbtCheck`, `scalafix --check` (ruleset
  documented), `coverage + coverageReport` with codecov upload, `docs/mdoc`,
  `mimaReportBinaryIssues` (reports only on 0.1.0; enforced on subsequent
  0.1.x releases).
- **R8. Benchmarks are expanded** to cover `Fold`, `Getter`, `Optional`,
  `Setter`, and `PowerSeries` in addition to existing `Lens / Prism / Iso /
  Traversal`, each with an EO implementation and a Monocle baseline where
  Monocle has an equivalent. Each bench uses Monocle's `Nested0…Nested6`
  fixture approach so numbers are comparable side-by-side.
- **R9. A `CHANGELOG.md`** captures the 0.1.0 release contents, using
  Keep-a-Changelog conventions, and is wired for subsequent releases.
- **R10. Existing behavior is preserved.** No law, benchmark, or API change
  alters the semantics of current optics or carriers; the existing ~70% of
  `core/` remains green against its laws.
- **R11. Composition-gap top-3 closures land at the behaviour-spec level**
  before tag, per
  [`2026-04-23-composition-gap-analysis.md`](../research/2026-04-23-composition-gap-analysis.md)
  §1.3. Concretely:
  - **R11a.** Behaviour tests for `Traversal.each × {Iso, Optional, Prism,
    Traversal.each}` — the second-most-common chain after `Lens → Traversal →
    Lens`, currently zero-covered.
  - **R11b.** Behaviour tests for Optional's four fused `.andThen` overloads
    (`Optional.andThen(GetReplaceLens | MendTearPrism | Optional |
    Traversal.each)`) — each has a fast path in `Optional.scala:123-188` that
    no spec currently exercises; overlaps with the Unit 8 coverage lift
    on `core/optics/Optional.scala`.
  - **R11c.** Setter composition story — either ship
    `AssociativeFunctor[SetterF, _, _]` plus `Composer[SetterF, _]` for at
    least Tuple2, *or* promote the terminal-carrier gotcha from
    `SetterF.scala:14` into user-facing `site/docs/optics.md`. Same for
    `FixedTraversal[N]` terminal-carrier note.
- **R12. Structural composition gaps get an explicit disposition.**
  `AlgLens[F]` outbound (no `Composer[AlgLens, _]`) and the
  `PowerSeries → {AlgLens, Grate}` absence are documented in
  `site/docs/optics.md` with a "why and what to do" paragraph each; and
  `Composer[Affine, AlgLens[F]]` + `JsonPrism × AlgLens` move from the
  composition-gap `?` cells into either an experimental-resolution
  micro-unit (§R12a) or an explicit deferral entry.
- **R13. Core coverage regression is repaired before tag.** The baseline
  68.30% → 58.83% statement (70.77% → 52.84% branch) regression flagged in
  [`2026-04-23-code-quality-review.md`](../research/2026-04-23-code-quality-review.md)
  §Coverage snapshot is unwound: `Iso.scala`, `Prism.scala`, `Optional.scala`,
  `Lens.scala` fused-`.andThen` overloads get behaviour fixtures (same work
  as R11b for Optional); the ≥85 % statement target from R3 becomes
  reachable once that coverage work lands. Branch target from R3 is relaxed
  to ≥75 % (match-type heavy code still under-reports).
- **R14. Code-quality review "must-fix before 0.1.0" items are closed.**
  Concretely (from review §Prioritized cleanup list → Must-fix for 0.1.0):
  - Finding #1 from the review (orphan-Reflector) is **dismissed as a
    timing artifact** — Kaleidoscope plan 006 Units 1-4 landed after the
    review ran, so `Reflector.scala` is now the live substrate for an
    in-flight family, not orphan scaffolding.
  - Findings 2-5 (coverage regression, circe walk-and-rebuild duplication,
    Grate null sentinels, laws-pulls-core architecture) are absorbed into
    the unit list below.
  - Remove the unused `given tupleInterchangeable` (`Lens.scala:22-24`),
    the empty `object ForgetfulApplicative` stub, the unused type
    parameters on `SetterF.scala:26, 38`, and the unused
    `@annotation.unused original: Json` parameter on
    `JsonFieldsTraversal.scala:313-318`.
  - Fill the enumerated Scaladoc companion-object gaps (overlaps with R5).
  - Add one fixture that pins the `Grate.apply` / `grateAssoc` null-sentinel
    invariants (`Grate.scala:108, 178`) — a future regression that starts
    reading the sentinel materialises as a test failure, not a silent NPE.
  - Ensure `circe/` is included in the `coverageReport` run, either by
    promoting `circeIntegration/test` into the coverage incantation or by
    documenting the exclusion in the baseline doc.
- **R15. Documentation deliverables from the cookbook + diagrams review
  ship with 0.1.0**, per
  [`2026-04-23-external-sources-cookbook-ideas.md`](../research/2026-04-23-external-sources-cookbook-ideas.md):
  - **R15a.** Ship three priority Mermaid diagrams — D1 (composition
    lattice, `site/docs/concepts.md`), D5 (JsonPrism Ior failure-flow,
    `site/docs/circe.md`), D3 (optic family taxonomy tree, top of
    `site/docs/optics.md`). Runners-up (D4, D7 simplified, D9) are
    stretch-goals.
  - **R15b.** Converge `site/docs/cookbook.md` on ~18-22 recipes (target
    footprint per the review's §"Recipe count" estimate) drawn from the
    30-recipe catalogue, merging adjacent recipes (e.g. 8+9 into one
    traversal-choice section; 13+14+15 into one JSON arc; 11+12+18 into
    one AlgLens vignette). Preserve the cats-eo-unique recipes first
    (recipes 3, 4, 10, 13-15, 23, 26, 28-30).
  - **R15c.** Cross-link recipes with Penner / Monocle-docs attribution
    so readers can chase the original framings (Penner's *Optics By
    Example* is the single heaviest source; cite it by chapter where
    the recipe maps directly).

## Scope Boundaries

In scope:

- All code under `core/`, `laws/`, `tests/`, `generics/`, `benchmarks/`.
- New `docs/` sub-project and a new top-level `docs/` markdown tree.
- Build plumbing (`build.sbt`, `project/plugins.sbt`, `project/build.properties`).
- CI configuration (`.github/workflows/ci.yml`, `release.yml`).
- `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE` header review.

Out of scope (explicit non-goals):

- **No brand-new optic abstractions added *during* this plan's execution.**
  The plan documents and hardens what exists on `main` at the moment of
  the 0.1.0 cut. `Grate` (plan 004), `AlgLens`, and `AffineFold` have
  already landed and are therefore in scope for laws / docs / composition
  coverage. The in-flight **Kaleidoscope** family (plan 006; Units 1-4
  landed, Units 5-7 pending) is treated as conditional scope — it ships
  in 0.1.0 *if* Units 5-7 of plan 006 land before tag, otherwise it
  defers cleanly to 0.1.1.
- **Indexed optics (plan 007) deferred to 0.2.x.** `IxLens` / `IxTraversal` /
  `IxFold` / `IxSetter` + a parallel law / bench / docs tree are
  structurally disruptive (every family's surface grows an index
  parameter). Track in
  [`docs/plans/2026-04-23-007-feat-indexed-optics-hierarchy-plan.md`](./2026-04-23-007-feat-indexed-optics-hierarchy-plan.md).
- **No Scala.js / Scala Native cross-build.** The codebase is JVM-only today.
  Adding cross-build is tracked as a 1.0 follow-up (see Future Considerations
  below).
- **No 1.0.0 stability review.** API is allowed to shift inside the `0.1.x`
  and `0.2.x` lines; MiMa becomes a gate on `0.1.1+` only.
- **No new macro features in `generics/`.** Existing `lens` / `prism` macros
  are documented and tested; no new derivations (e.g., no `iso`/`optional`
  macro) in this plan.
- **No performance-regression CI job.** JMH benchmarks are added and
  documented but run offline — CI runtime and noise floor do not justify it
  pre-1.0. A later plan can add a nightly benchmarks workflow.
- **No changes to `CLAUDE.md`'s agent-facing content.** It is updated only
  where new commands (sbt tasks) or layout conventions materially changed;
  the user-facing documentation lives in the new `docs/` tree.

## Context & Research

### Relevant Code and Patterns

- **Laws convention already in use** — `laws/src/main/scala/eo/laws/OpticLaws.scala`
  uses `trait XxxLaws { def lens: Optic[...]; def roundTrip(s): Boolean }` plus
  `abstract class XxxTests[...] extends Laws { def laws: XxxLaws[...]; def
  iso(using Arbitrary[S], ...): RuleSet = new SimpleRuleSet("name", ...) }`.
  The pattern is correct — the restructure is purely moving each abstraction
  into its own file under `eo.laws` / `eo.laws.discipline`.
- **Carrier naming + paths** to follow when adding new law files:
  - Optics → `laws/src/main/scala/eo/laws/<Name>Laws.scala`,
    `laws/src/main/scala/eo/laws/discipline/<Name>Tests.scala`.
  - Data carriers → `laws/src/main/scala/eo/laws/data/<Name>Laws.scala`,
    `laws/src/main/scala/eo/laws/data/discipline/<Name>Tests.scala`.
- **Test wiring** — `tests/src/test/scala/eo/OpticsLawsSpec.scala` is the
  existing `AnyFunSuite with Discipline`-style entry point. Additional law
  classes plug in as `checkAll("Getter[Person, String] lawful", GetterTests[...].getter)`.
- **JMH fixture** — `benchmarks/src/main/scala/eo/bench/OpticsBench.scala`
  already defines a `Nested` case-class tree; reuse + extend it. Adopt
  Monocle's `MonocleLensBench` / `StdLensBench` trait-pair pattern.
- **Scaladoc density** — `Optic.scala`, `Lens.scala`, and `ForgetfulFunctor.scala`
  already carry heavy docstrings (the existing model). Extend that treatment
  uniformly to the `optics/` thin files (`Fold.scala`, `Getter.scala`,
  `Iso.scala`, `Optional.scala`, `Prism.scala`, `Setter.scala`,
  `Traversal.scala`) and to `data/` carriers.

### External References

- **sbt-typelevel-ci-release** as the publishing plugin — drops
  `sbt-sonatype` + `sbt-pgp` direct usage, matches Monocle's current setup,
  bakes in the Sonatype Central Portal flow introduced after OSSRH sunset
  (2025-06-30).
  <https://typelevel.org/sbt-typelevel/> ·
  <https://github.com/sbt/sbt-ci-release>.
- **sbt-typelevel-site + Laika + mdoc** as the docs toolchain — matches
  typelevel/cats's current `main` docs setup (`file("site")` project + Laika
  `directory.conf`). sbt-mdoc 2.9.0 current. (Monocle still uses Docusaurus;
  we choose the more current Typelevel pattern since cats-eo is named after
  cats and the audiences overlap.)
  <https://github.com/typelevel/cats/blob/main/build.sbt>.
- **Laws split conventions** — cats publishes `cats-laws` with
  `MonadLaws` (equations) + `MonadTests` (discipline RuleSet). Monocle
  publishes `monocle-law` with `LensTests` / `PrismTests` / etc. We mirror
  the cats layout because the law equations and RuleSet for the same
  abstraction are discovered side-by-side.
- **JMH / Monocle bench pattern** — Monocle's `bench/` has
  `abstract class LensBench { def lensGet0/3/6 }` then `MonocleLensBench extends
  LensBench` and `StdLensBench extends LensBench`. Same method names, one
  fixture, side-by-side JMH rows.
  <https://github.com/optics-dev/Monocle/tree/master/bench/src/main/scala/monocle/bench>.
- **Scaladoc groups** — `scalacOptions += "-groups"` is how Scala 3 enables
  `@group` tags. Use `@group Constructors`, `@group Operations`,
  `@group Instances` uniformly per optic companion.
  <https://docs.scala-lang.org/scala3/guides/scaladoc/docstrings.html>.

### Institutional Learnings

None in `docs/solutions/` yet (no such folder exists). As this plan executes,
any surprising incidents (e.g., "MiMa false positive on existential type X",
"Laika config gotcha Y") should land in `docs/solutions/` for future-us.

## Key Technical Decisions

- **D1. Adopt `sbt-typelevel-ci-release` 0.8.x (not hand-rolled sonatype /
  pgp).** Rationale: one plugin covers release automation, PGP secrets from
  CI, Central Portal API, and consistent `tlBaseVersion` / MiMa conventions
  from the same plugin family we use for site + mima. Hand-rolled publishing
  was the 2022 pattern; post-OSSRH-sunset it is strictly more work for no
  benefit.
- **D2. `tlBaseVersion := "0.1"`. Do not enforce MiMa on 0.1.0 itself.**
  Rationale: MiMa is useless when no prior artifact is published. Set
  `tlMimaPreviousVersions := Set.empty` in 0.1.0's build; on 0.1.1 the plugin
  starts comparing against `0.1.0` automatically. Pre-1.0 minor bumps are free
  to break compatibility; the `0.1.x` patch series is binary-compat-guarded.
- **D3. Docs stack is `sbt-typelevel-site` (Laika + mdoc).** Rationale: it
  matches cats's current docs, is Markdown-native (low friction for PR
  contributions), and mdoc verifies every code fence compiles against the
  library — so docs cannot silently rot. Rejected: Docusaurus (requires Node
  in the dev loop), microsites (abandoned), vanilla Scaladoc alone (no
  narrative layer).
- **D4. Split laws into `eo.laws` + `eo.laws.discipline`, one abstraction
  per file.** Rationale: matches cats and makes `cats-eo-laws` a clean
  downstream dependency. The current bundled files (`OpticLaws.scala`,
  `EoSpecificLaws.scala`) become entry-point "pallets" that re-export for
  backward source-compatibility in this same release — then we decide per
  abstraction whether the re-exports stay long-term.
- **D5. Keep `tests/` as the single place where discipline RuleSets are
  instantiated.** Rationale: it is the only `publish / skip := true`
  sub-project with access to both `core` and `laws`. All new property tests
  land here. `core/src/test/scala/eo/FoldSpec.scala` (the lone smoke test)
  is migrated into `tests/` for consistency.
- **D6. Use `cats-kernel` `Eq` / `Hash` for law equality everywhere.**
  Rationale: structural `==` is the current pattern but is fragile for
  function-typed law equations (e.g., `modify` equality). Every `FooLaws`
  definition should accept `Eq[S]` / `Eq[A]` / `Eq[B]` implicit evidence so
  property equality can be overridden per test fixture.
- **D7. CI matrix: `scala: [3.8.3]`, `java: [temurin@17, temurin@21]`,
  `os: [ubuntu-latest]`.** Rationale: no Scala 2 support (3.8.3 is the only
  supported compiler), no cross-platform (JVM-only today), JDK 17 is the
  current LTS floor matching `cats-core 2.13.0`'s support matrix, JDK 21 is
  the next LTS. macOS/Windows runners are unnecessary pre-1.0.
- **D8. `cats-kernel-laws` gets pulled transitively by depending on
  `discipline-core` + our own scalacheck arbitraries.** Rationale: we do not
  need `cats-laws` as a dep — we write optics / carrier laws, not monad laws.
  Keep the laws module slim.
- **D9. Bump scalafmt from `3.0.7` (current pin) to `3.11.0`** (already
  installed via coursier locally). Rationale: 3.0.7 is from 2021; the modern
  version is stable, has better Scala-3 support, and matches the `CLAUDE.md`
  "installed versions" claim. Bump pin in `.scalafmt.conf`, run
  `scalafmtAll`, commit the formatting churn as its own commit (it will touch
  many files; isolated from behavior changes).
- **D10. `coverage` enforcement is a report + codecov upload, not a CI
  failure gate.** Rationale: scoverage false positives on type-level-only
  code (`AssociativeFunctor` instances with existential `Z`) make a hard
  floor noisy. Publish the report, watch trend, discuss lifts in PR.
  Enforcement is a 1.0 concern.

## Open Questions

### Resolved During Planning

- **Which docs toolchain?** → `sbt-typelevel-site` (Laika + mdoc). User
  chose this option explicitly.
- **PowerSeries / Vect law depth?** → Full law classes. User chose this.
- **Publishing target?** → Maven Central (Central Portal) at 0.1.0, pre-1.0
  semantics. User chose this.
- **Include benchmarks expansion?** → Yes. User chose this.
- **Do we need a separate `cats-eo-bench` published artifact?** → No, keep
  `benchmarks/` with `publish / skip := true`. It exists only to produce
  JMH numbers.
- **Do we need a separate `cats-eo-docs` published artifact?** → No, the
  docs sub-project is `publish / skip := true` and exists to produce a
  deployable site bundle.

### Deferred to Implementation

- **Exact scoverage % achievable per file.** Some files (`AssociativeFunctor`)
  are almost entirely type-level; their runtime statement coverage may stay
  low regardless of test effort. Target is ≥85% on `core` overall; per-file
  floors are set during Unit 2 when we see real numbers.
- **Whether to publish snapshot builds to Sonatype snapshots.** Left for the
  release cut in Unit 15 — may not be worth the extra secret surface for a
  single 0.1.0 line.

### New since 2026-04-24 revision — surfaced by review inputs

- **OQ-R1. The 12 `?` cells from the composition-gap analysis §2.2.** Each is
  a "carrier pair exists but no Composer ships and no test resolves it"
  question. Before tag, pick one of three dispositions for each:
  (a) experimental 5-minute compile-run to flip ? → N,
  (b) ship a new Composer / AssociativeFunctor given,
  (c) explicit deferral entry in `site/docs/optics.md`.
  The 12 cells cluster into three batches (Lens/Prism/Optional × Fold/
  Traversal.forEach; Traversal.each × {Fold, Traversal.forEach, AlgLens};
  cross-F Forget × Forget). Cross-reference
  [composition-gap §2.2](../research/2026-04-23-composition-gap-analysis.md#22-summary-of-the--cells-for-34)
  on resolution.
- **OQ-R2. Setter composition direction (R11c).** Ship an
  `AssociativeFunctor[SetterF, _, _]` plus a `Composer[SetterF, _]` for
  Tuple2? Or keep SetterF as a composition-terminal and make that a
  documented boundary? Performance implication is negligible either way;
  it's purely a UX / API-surface call.
- **OQ-R3. How much of the circe walk-and-rebuild extraction ships in
  0.1.0?** The review's single highest-ROI refactor (∼1200 LoC removed via
  a shared `walkAndUpdate[F]` helper) is structurally disruptive and wants
  a benchmark re-run. Default disposition: defer to 0.1.1 per the review's
  "Post-0.1.0" ranking, but flag the current duplication in release notes
  so downstream readers know the shape is provisional.
- **OQ-R4. Should `Codescene MCP` wiring be attempted before tag?** The
  code-quality review substituted the simplicity-reviewer lens for a
  Codescene run because no Codescene MCP was available. If the tooling
  surfaces before tag, a one-shot Codescene pass adds change-coupling,
  churn × hotspot, and god-class metrics we currently lack. Low priority;
  mark as optional pre-tag nice-to-have.

### Resolved since 2026-04-17

- **Which docs toolchain?** → `sbt-typelevel-site` (Laika + mdoc). User
  chose this option explicitly. *Resolved; site is live under `site/`.*
- **PowerSeries / Vect law depth?** → Full law classes. User chose this.
  *Resolved; laws + fixtures landed in Units 5-6.*
- **Publishing target?** → Maven Central (Central Portal) at 0.1.0, pre-1.0
  semantics. User chose this. *Resolved; Unit 13 wired.*
- **Include benchmarks expansion?** → Yes. User chose this. *Resolved;
  Unit 12 complete + Grate / PowerSeries benches added.*
- **Do we need a separate `cats-eo-bench` published artifact?** → No, keep
  `benchmarks/` with `publish / skip := true`. *Resolved.*
- **Do we need a separate `cats-eo-docs` published artifact?** → No.
  *Resolved; `site/` is `publish / skip := true`.*
- **`OpticLaws.scala` / `EoSpecificLaws.scala` umbrella fate.** Decided
  during Unit 1 execution — the split dropped the umbrellas entirely;
  no `@deprecated` shim was needed because no downstream existed at
  0.1.0-SNAPSHOT. *Resolved.*
- **Laika navigation taxonomy.** *Resolved in Unit 11 execution:
  `site/docs/directory.conf` ships the final ordering (getting-started /
  concepts / optics / generics / cookbook / circe / benchmarks /
  extensibility / migration).*
- **Which specific `@example` blocks migrate from Scaladoc into the
  microsite.** *Resolved in Unit 11 — short examples stayed in Scaladoc;
  anything over ~5 lines now lives in `site/docs/`.*

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance for
> review, not implementation specification. The implementing agent should
> treat it as context, not code to reproduce.*

### Target repository layout

```
cats-eo/
├── build.sbt                              # +docs project, +ThisBuild release settings
├── project/
│   ├── build.properties                   # unchanged (sbt 1.12.9)
│   └── plugins.sbt                        # +sbt-typelevel-ci-release, +sbt-typelevel-site
├── .github/
│   └── workflows/
│       ├── ci.yml                         # new — compile+test+fmt+fix+cov+mdoc+mima
│       └── release.yml                    # new — tag-triggered publish to Central
├── core/                                  # (unchanged shape)
├── laws/
│   └── src/main/scala/eo/
│       ├── laws/
│       │   ├── IsoLaws.scala              # split from OpticLaws.scala
│       │   ├── LensLaws.scala
│       │   ├── PrismLaws.scala
│       │   ├── OptionalLaws.scala
│       │   ├── SetterLaws.scala
│       │   ├── TraversalLaws.scala
│       │   ├── GetterLaws.scala           # new
│       │   ├── FoldLaws.scala             # new
│       │   ├── eo/                        # EO-specific laws, grouped
│       │   │   ├── MorphLaws.scala
│       │   │   ├── ComposeLaws.scala
│       │   │   ├── ReverseAndTransformLaws.scala
│       │   │   ├── ModifyALaws.scala
│       │   │   ├── FoldAndTraverseLaws.scala
│       │   │   └── ChainLaws.scala
│       │   ├── data/                      # carrier laws
│       │   │   ├── AffineLaws.scala
│       │   │   ├── SetterFLaws.scala
│       │   │   ├── VectLaws.scala
│       │   │   ├── PowerSeriesLaws.scala
│       │   │   └── FixedTraversalLaws.scala
│       │   └── typeclass/                 # type-class laws
│       │       ├── ForgetfulFunctorLaws.scala
│       │       ├── ForgetfulTraverseLaws.scala
│       │       ├── AssociativeFunctorLaws.scala
│       │       └── ComposerLaws.scala
│       └── laws/discipline/               # RuleSets, mirrors structure above
│           ├── IsoTests.scala
│           ├── …
│           ├── data/
│           └── typeclass/
├── tests/                                 # checkAll wiring; +PowerSeriesSpec, +VectSpec
├── generics/                              # (unchanged)
├── benchmarks/                            # + OptionalBench, PowerSeriesBench,
│                                          #   FoldBench, GetterBench, SetterBench
│                                          #   (priority order — see Unit 12);
│                                          #   reuse Nested0..Nested6 fixture
├── site/                                  # new sbt-typelevel-site project
│   ├── build.sbt                          # or configured inline in root build.sbt
│   └── src/main/mdoc/                     # (if using custom mdoc dir)
├── docs/                                  # markdown source, read by Laika
│   ├── directory.conf                     # Laika nav order
│   ├── index.md
│   ├── getting-started.md
│   ├── concepts/
│   │   ├── existential-optics.md
│   │   ├── carriers.md
│   │   └── laws.md
│   ├── optics/                            # one page per optic
│   │   ├── iso.md
│   │   ├── lens.md
│   │   ├── prism.md
│   │   ├── optional.md
│   │   ├── traversal.md
│   │   ├── setter.md
│   │   ├── getter.md
│   │   └── fold.md
│   ├── generics.md                        # lens/prism macros
│   ├── cookbook/
│   │   ├── nested-records.md
│   │   ├── adts-and-sum-types.md
│   │   ├── json-paths.md
│   │   └── powerseries-heterogeneous.md
│   ├── migration-from-monocle.md
│   ├── plans/                             # this plan lives here
│   │   └── 2026-04-17-001-feat-production-readiness-laws-docs-plan.md
│   └── solutions/                         # learnings, as they accrue
├── examples/                              # runnable sbt-run examples
│   └── src/main/scala/eo/examples/…
├── README.md                              # rewritten
├── CHANGELOG.md                           # new
├── CONTRIBUTING.md                        # new
└── LICENSE                                # unchanged
```

### Dependency graph across implementation units

```mermaid
flowchart TB
  U1[U1 Laws split + reorg] --> U2[U2 Coverage baseline measurement]
  U1 --> U3[U3 Getter+Fold laws]
  U1 --> U4[U4 Affine+SetterF carrier laws]
  U1 --> U5[U5 Vect laws + tests]
  U5 --> U6[U6 PowerSeries laws + tests]
  U1 --> U7[U7 Type-class laws Forgetful* / Associative / Composer]
  U2 --> U8[U8 Coverage gap-fill to ≥85%]
  U3 --> U8
  U4 --> U8
  U5 --> U8
  U6 --> U8
  U7 --> U8
  U8 --> U9[U9 Scaladoc uplift + -groups]
  U9 --> U10[U10 Site sub-project + Laika scaffold]
  U10 --> U11[U11 Docs content guided tour + cookbook]
  U11 --> U12[U12 Benchmarks expansion]
  U1 --> U13[U13 Release infra plugins + build settings]
  U13 --> U14[U14 CI workflows + publish secrets]

  %% 2026-04-24 revision — composition / quality / cookbook / diagrams
  U1 --> U16[U16 Composition-gap top-3 closures R11]
  U8 --> U16
  U1 --> U17[U17 Code-quality must-fix + circe coverage R13/R14]
  U11 --> U18[U18 Mermaid diagrams D1/D3/D5 R15a]
  U11 --> U19[U19 Cookbook consolidation to 18-22 recipes R15b/c]
  U18 --> U19
  U16 --> U21[U21 Resolve 12 ? composition cells OQ-R1]
  U7 --> U21

  %% Kaleidoscope 006 conditional
  K006[plan 006 Units 5-7 Kaleidoscope] -.->|conditional| U20[U20 Kaleidoscope cross-scope coordination]
  U20 -.->|if shipped| U18
  U20 -.->|if shipped| U19

  U14 --> U15[U15 README + CHANGELOG + CONTRIBUTING + 0.1.0 cut]
  U12 --> U15
  U11 --> U15
  U16 --> U15
  U17 --> U15
  U18 --> U15
  U19 --> U15
  U20 --> U15
  U21 --> U15
```

### Law-file skeleton shape (directional)

For every new law class, the same skeleton (not implementation — just shape):

```scala
// laws/src/main/scala/eo/laws/GetterLaws.scala
package eo.laws

trait GetterLaws[S, A]:
  def getter: Optic[S, S, A, A, Forgetful]
  def getConsistent(s: S): Boolean =
    getter.get(s) == getter.to(s)
  // + any Getter-specific invariants we identify
```

```scala
// laws/src/main/scala/eo/laws/discipline/GetterTests.scala
package eo.laws.discipline

abstract class GetterTests[S, A] extends Laws:
  def laws: GetterLaws[S, A]
  def getter(using Arbitrary[S], Eq[A]): RuleSet =
    new SimpleRuleSet("Getter", "getConsistent" -> forAll(laws.getConsistent _))
```

### Release flow

```mermaid
sequenceDiagram
  participant Dev
  participant Git
  participant CI as GitHub Actions
  participant Central as Sonatype Central Portal
  Dev->>Git: git tag v0.1.0 && git push --tags
  Git->>CI: tag push triggers release.yml
  CI->>CI: sbt ci-release (signs, bundles)
  CI->>Central: upload + close + release
  Central-->>Dev: artifact visible in ~15 min
  Dev->>Git: edit CHANGELOG [0.1.0] release notes
```

## Implementation Units

Unit granularity: each unit below represents roughly one atomic commit's worth
of work. Units marked *Execution note: test-first* should start by adding a
failing discipline `checkAll` block, then write the law equations until it
passes.

- [x] **Unit 1: Split `laws/` into idiomatic per-abstraction files**

**Goal:** Restructure `OpticLaws.scala` and `EoSpecificLaws.scala` into one
file per abstraction under `eo.laws.*` (equations) and
`eo.laws.discipline.*` (RuleSets), matching cats conventions. Delete or
`@deprecated`-shim the old bundled objects. No behavior changes.

**Requirements:** R2, R10.

**Dependencies:** none — foundational restructure.

**Files:**
- Modify: `laws/src/main/scala/eo/laws/OpticLaws.scala` → becomes a thin
  `@deprecated` re-export umbrella (or is deleted if we decide against
  back-compat).
- Modify: `laws/src/main/scala/eo/laws/EoSpecificLaws.scala` → same treatment.
- Create: `laws/src/main/scala/eo/laws/IsoLaws.scala`, `LensLaws.scala`,
  `PrismLaws.scala`, `OptionalLaws.scala`, `SetterLaws.scala`,
  `TraversalLaws.scala` (one per optic).
- Create: `laws/src/main/scala/eo/laws/discipline/IsoTests.scala`, `LensTests.scala`,
  `PrismTests.scala`, `OptionalTests.scala`, `SetterTests.scala`,
  `TraversalTests.scala`.
- Create: cohesively-grouped EO law files under `laws/src/main/scala/eo/laws/eo/` —
  group closely-related equations together rather than one file per equation.
  Target grouping (refine during the split):
  - `MorphLaws.scala` (A1/A2 — morph preserves modify & get).
  - `ComposeLaws.scala` (Lens/Iso/Prism/Optional compositions together; if
    this file exceeds ~250 LoC, split per-optic-pair).
  - `ReverseAndTransformLaws.scala` (Iso reverse involution + place/transfer/
    transform + put ≡ reverseGet).
  - `ModifyALaws.scala` (D1 Identity + D3 Const).
  - `FoldAndTraverseLaws.scala` (foldMap homomorphism + traverse-all
    length/content + forget-all-modify).
  - `ChainLaws.scala` (Composer chain path-independence + accessor).
- Create: the matching `laws/src/main/scala/eo/laws/eo/discipline/*Tests.scala`.
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala`,
  `tests/src/test/scala/eo/EoSpecificLawsSpec.scala` — adjust imports to
  new locations.

**Approach:**
- Do this as a semantic move: each law equation and each RuleSet lands in its
  new home with its equations intact. Cosmetic renames of private helpers are
  allowed when necessary to resolve visibility (see next bullet). After the
  move, `sbt test` must pass with the same count of test cases.
- **Package-private visibility risk.** The current bundled files share
  `private[laws]` helpers (e.g., shared arbitrary fixtures inside
  `OpticLaws`). Splitting to sibling files may require widening those helpers
  to package-private at a coarser level or extracting them into a shared
  `LawsHelpers.scala`. Watch for `not accessible from` errors during the
  split; fix by promoting visibility rather than duplicating code.
- Decide the umbrella-object fate once the split is visible — likely keep the
  old `OpticLaws` object with `@deprecated` type aliases pointing at the new
  location, so any downstream copy-paste still compiles for 0.1.x.
- Scalafmt the touched files; do not reformat unrelated files.

**Execution note:** Run `sbt "clean; test"` before and after, compare totals to
confirm the move is mechanical.

**Patterns to follow:**
- cats' `typelevel/cats/laws/src/main/scala/cats/laws/` + `…/discipline/` split.
- Monocle's `law/src/main/scala/monocle/law/` + `…/discipline/` split.

**Test scenarios:**
- Happy path: every existing `checkAll` block in the two spec files still
  passes after imports are updated.
- Integration: `laws/target/scala-*/cats-eo-laws_*.jar` still produces a
  loadable module (`sbt lawsProject/publishLocal` + `sbt tests/test` against
  the published local jar).

**Verification:**
- `sbt "clean; compile; test"` green.
- `git grep "object OpticLaws" laws/` returns either nothing or only the
  `@deprecated` shim.
- Each new file is cohesively grouped (one abstraction or one closely-related
  law family). Size is a secondary signal — files should not be split further
  just to hit a LoC number.

- [x] **Unit 2: Establish coverage baseline and file-level targets**

**Goal:** Run scoverage on the freshly-split `tests/` to capture the current
statement/branch coverage per file, write the baseline into
`docs/solutions/2026-04-17-coverage-baseline.md`, and set explicit per-file
coverage targets used by later units.

**Requirements:** R3.

**Dependencies:** Unit 1 (so the coverage map reflects the final laws
organization).

**Files:**
- Create: `docs/solutions/2026-04-17-coverage-baseline.md` with the full
  scoverage table (file, statement coverage %, branch coverage %, note on
  whether the file is type-level-only).

**Approach:**
- Run `sbt "clean; coverage; tests/test; coverageReport"`.
- Transcribe the per-file numbers.
- For each file, annotate: *"feature code (aim ≥85%)"*, *"type-level only
  (no target)"*, or *"mixed (aim per-file)"*.
- Output is informational for Unit 8 — no code change.

**Execution note:** None.

**Patterns to follow:** existing scoverage invocation from CLAUDE.md.

**Test scenarios:**
- Test expectation: none — this unit produces a measurement artifact, no
  behavioral change.

**Verification:**
- The baseline doc exists and lists every `.scala` under `core/src/main/`.
- The plan's later coverage-lift unit cites this baseline.

- [x] **Unit 3: Add `GetterLaws` + `FoldLaws`**

**Goal:** Fill the two obvious optic-level law gaps. Getter should have at
least one consistency law (`get` = `to .accessor.get`); Fold should have
monoid-homomorphism laws analogous to cats-kernel's `FoldableLaws`, adapted
to the `Forget[F]` carrier.

**Requirements:** R1, R3.

**Dependencies:** Unit 1.

**Files:**
- Create: `laws/src/main/scala/eo/laws/GetterLaws.scala`,
  `laws/src/main/scala/eo/laws/FoldLaws.scala`.
- Create: `laws/src/main/scala/eo/laws/discipline/GetterTests.scala`,
  `laws/src/main/scala/eo/laws/discipline/FoldTests.scala`.
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add `checkAll`
  for `GetterTests` and `FoldTests` with at least two fixtures each (e.g.,
  `Getter[Person, String]`, `Getter[(Int,String), Int]`, `Fold[List,Int]`,
  `Fold[Option,Int]`).

**Approach:**
- Getter laws: `getConsistent` (get matches `Accessor.get(to(s))`),
  `pureId` (for `Forgetful` carriers, `reverseGet` is `identity` — this
  becomes a type-level check only if `reverseGet` exists for Getter; if
  Getter exposes no `reverseGet`, drop and document).
- Fold laws:
  - `foldMapEmpty` (empty structure folds to `Monoid.empty`),
  - `foldMapCombine` (`foldMap(xs ++ ys) == foldMap(xs) |+| foldMap(ys)`
    once we can express `++`; for generic `F[_]: Foldable`, use cats'
    equivalence via `Foldable.combineK` or a simpler reformulation),
  - `foldMapIdentity` (`foldMap(identity)(xs) == xs.combineAll`),
  - `selectFilter` (for `Fold.select(p)`, the produced fold only emits
    elements satisfying `p`).

**Execution note:** test-first — write the `checkAll` block in
`OpticsLawsSpec.scala` first, then the laws + tests classes, then
satisfy the signatures until the suite compiles + passes.

**Patterns to follow:** cats' `FoldableLaws` / `FoldableTests` for the
shape; this project's `OpticLaws.IsoLaws` / `IsoTests` for the RuleSet
wiring.

**Test scenarios:**
- Happy path — `checkAll("Getter[Person, String] lawful", GetterTests[...].getter)`
  passes.
- Happy path — `checkAll("Fold[List, Int] lawful", FoldTests[...].fold)` passes.
- Edge case — empty-structure Fold (empty `List[Int]`) yields `Monoid.empty`.
- Edge case — `Fold.select(const(false))` produces a fold that always yields
  `Monoid.empty`.
- Error path — n/a (pure functions, no failure modes).

**Verification:**
- `sbt tests/test` shows both new `checkAll` blocks green.
- Coverage report for `core/src/main/scala/eo/optics/Getter.scala` and
  `…/Fold.scala` rises above 85%.

- [x] **Unit 4: Add `AffineLaws` + `SetterFLaws` (carrier laws)**

**Goal:** Give `Affine` and `SetterF` standalone law classes independent of
the `Optional` / `Setter` optics that use them. Laws should cover the
carrier-level invariants — functor/traverse identity + composition, and
`Affine`'s `Fst/Snd` projections' round-trip.

**Requirements:** R1, R3.

**Dependencies:** Unit 1.

**Files:**
- Create: `laws/src/main/scala/eo/laws/data/AffineLaws.scala`,
  `laws/src/main/scala/eo/laws/data/SetterFLaws.scala`.
- Create: `laws/src/main/scala/eo/laws/data/discipline/AffineTests.scala`,
  `laws/src/main/scala/eo/laws/data/discipline/SetterFTests.scala`.
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add `checkAll`
  blocks for both.

**Approach:**
- Affine laws: functor identity (`map id == id`), functor composition
  (`map (f ∘ g) == map f ∘ map g`), traverse naturality (standard
  traverse law), associate-left / associate-right idempotence (`Affine`
  has both `AssociativeFunctor` instances).
- SetterF laws: distributive-traverse law, `modify id == id`, modify
  composition.

**Execution note:** test-first.

**Patterns to follow:** cats-kernel's `FunctorLaws`, `TraverseLaws`,
adapted to the 2-parameter shape.

**Test scenarios:**
- Happy path — `AffineTests[...].affine` green on fixtures
  `Affine[(Int,String), Boolean]`, `Affine[(String,Person), Age]`.
- Happy path — `SetterFTests[...].setterF` green on fixtures.
- Edge case — `Affine` constructed in the `Fst` branch: functor / traverse
  still identity.
- Integration — `Optional[S, A]` built on top continues to pass its existing
  laws (no regression in `OptionalTests`).

**Verification:**
- `sbt tests/test` green.
- Coverage report for `core/src/main/scala/eo/data/Affine.scala` and
  `…/SetterF.scala` rises above 85%.

- [x] **Unit 5: `VectLaws` + `VectSpec`**

**Goal:** Vect is heterogeneous and phantom-typed but still has a runtime
structure (the four constructors: `NilVect`, `ConsVect`, `TConsVect`,
`AdjacentVect`). We add a dedicated law class that pins down its
functor/traverse identity, concat associativity (`++` is associative up to
structural equality, with `NilVect` as identity on both sides), and
`slice` preservation, plus a behavior spec that exercises the
constructor-level invariants property-based (e.g., `(xs :+ x) ++ ys == xs ++ (x +: ys)`).

**Requirements:** R1, R3.

**Dependencies:** Unit 1.

**Files:**
- Create: `laws/src/main/scala/eo/laws/data/VectLaws.scala`.
- Create: `laws/src/main/scala/eo/laws/data/discipline/VectTests.scala`.
- Create: `tests/src/test/scala/eo/VectSpec.scala` — property tests that
  do not fit a discipline RuleSet (e.g., arity invariants, structural
  equality).
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add `checkAll`.

**Approach:**
- Laws: `Functor[Vect[N, *]]` identity + composition, `Traverse` identity
  + sequential composition, concat associativity, `slice` length
  (`xs.slice(0, n).length == n`), cons/snoc symmetry.
- Spec: arity invariants (`(xs ++ ys).length == xs.length + ys.length`),
  structural equivalence between `TConsVect` and `ConsVect` where they
  should coincide, `AdjacentVect` flattening round-trip.
- Use `cats.laws.discipline.FunctorTests` and `TraverseTests` shapes as
  models for the per-N instances — each fixture pins a specific `N`
  (e.g., `Vect[3, Int]`).

**Execution note:** test-first — especially valuable here because `Vect`
has had no dedicated tests. Write three failing assertions first, then
make them pass.

**Patterns to follow:** cats-kernel `SemigroupLaws` for concat
associativity; Monocle has no direct analog.

**Test scenarios:**
- Happy path — functor identity on `Vect[3, Int]`, `Vect[5, String]`.
- Happy path — traverse identity with `Applicative[Option]`.
- Edge case — `NilVect ++ xs == xs` and `xs ++ NilVect == xs`.
- Edge case — `slice(0, 0)` on any `Vect` produces `NilVect`.
- Edge case — `slice(k, k)` produces `NilVect` for any valid `k`.
- Integration — `PowerSeries` operations that consume a `Vect` behave
  identically for `xs ++ ys` vs `xs` followed by `ys` piecewise.
- Error path — n/a (structural operations, no failure modes; illegal
  indices should be phantom-type ruled out at compile time — verify that
  with a `compileErrors` assertion).

**Verification:**
- `sbt tests/test` green.
- `core/src/main/scala/eo/data/Vect.scala` coverage reaches ≥85% (this
  file is currently ~0% covered).

- [x] **Unit 6: `PowerSeriesLaws` + `PowerSeriesSpec`**

**Goal:** Full law coverage for the `PowerSeries` carrier + its
`Traversal.powerEach` surface. This is the largest new law family in the
plan because PowerSeries interacts with both `Affine` and `Tuple2`
through `Composer` chains.

**Requirements:** R1, R3.

**Dependencies:** Unit 1, Unit 5 (Vect laws must exist; PowerSeries laws
rely on Vect behaving lawfully).

**Files:**
- Create: `laws/src/main/scala/eo/laws/data/PowerSeriesLaws.scala`.
- Create: `laws/src/main/scala/eo/laws/data/discipline/PowerSeriesTests.scala`.
- Create: `laws/src/main/scala/eo/laws/data/FixedTraversalLaws.scala`,
  `laws/src/main/scala/eo/laws/data/discipline/FixedTraversalTests.scala`.
- Create: `tests/src/test/scala/eo/PowerSeriesSpec.scala` — property-based
  behavior tests for `Traversal.powerEach` / `pPowerEach`.
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala` — add `checkAll`
  blocks.
- Migrate: existing `tests/src/test/scala/eo/Unthreaded.scala` examples
  become mdoc-verified snippets in Unit 11; keep the file for now as a
  non-docs reference but drop it once Unit 11 lands.

**Approach:**
- PowerSeries laws: `ForgetfulFunctor` identity + composition,
  `ForgetfulTraverse` with `Applicative` identity + sequential composition,
  `AssociativeFunctor` left/right associativity (the `associateLeft ∘
  associateRight == identity` round-trip), Composer round-trips for the
  four chain bridges (`Tuple2 → PowerSeries`, `Either → PowerSeries`,
  `Affine → PowerSeries`, `PowerSeries → Affine`).
- FixedTraversal laws: arity preservation — a `FixedTraversal[N]` modify
  over `N` elements produces an output with `N` elements; functor
  identity; functor composition.
- Spec: `Traversal.powerEach` applied to a `(head, Vect[N, A])` fixture
  visits every element; modifies are in index order; `pPowerEach` with
  type-changing modifies preserves arity and head.

**Execution note:** test-first; this is high-value new coverage.

**Patterns to follow:** `Affine` laws in Unit 4 as structural model;
cats' `TraverseLaws` shape.

**Test scenarios:**
- Happy path — functor identity on `PowerSeries[(Int,Unit), String]`.
- Happy path — traverse identity with `Applicative[Option]`.
- Happy path — `Traversal.powerEach` maps every element of a
  `Vect`-backed fixture.
- Edge case — empty Vect (`Vect[0, A]`) → powerEach is a no-op.
- Edge case — single-element Vect (`Vect[1, A]`) → powerEach is a
  single-function map.
- Integration — `Composer` chain `Tuple2 → Affine → PowerSeries` round-trip
  preserves `get` and `modify` semantics on every fixture.
- Integration — migrated `Unthreaded.scala` example becomes a property test
  (its current "print" demonstrations become real assertions against
  expected output).
- Error path — n/a (pure type-safe operations; index errors are
  compile-time).

**Verification:**
- `sbt tests/test` green.
- `core/src/main/scala/eo/data/PowerSeries.scala` and
  `…/FixedTraversal.scala` coverage reaches ≥85%.
- The migrated `Unthreaded.scala` scenarios exist in `PowerSeriesSpec.scala`
  as property tests.

- [x] **Unit 7: Type-class laws (`ForgetfulFunctor`, `ForgetfulTraverse`,
  `AssociativeFunctor`, `Composer`)** *(completed 2026-04-24 —
  `AssociativeFunctorLaws` + `ComposerPathIndependenceLaws` +
  `ComposerPreservesGetLaws` landed under
  `laws/src/main/scala/eo/laws/typeclass/`, the earlier chain laws were
  promoted from `eo/ChainLaws.scala`)*

**Goal:** Dedicate law classes to the four carrier type-classes that drive
runtime behavior across every optic and carrier in use today. Any
downstream projects adding a new carrier (`F[X, A]`) can verify the
instance with `ForgetfulFunctorTests[F].forgetfulFunctor` and see exactly
which law fails. Some of these laws exist inline in `EoSpecificLaws`
today (e.g., `ChainPathIndependenceLaws`); this unit promotes them to
carrier-level laws.

**Requirements:** R1.

**Dependencies:** Unit 1.

**Files:**
- Create: `laws/src/main/scala/eo/laws/typeclass/ForgetfulFunctorLaws.scala`,
  `ForgetfulTraverseLaws.scala`, `AssociativeFunctorLaws.scala`,
  `ComposerLaws.scala`.
- Create: matching `laws/src/main/scala/eo/laws/typeclass/discipline/*Tests.scala`.
- Modify: `tests/src/test/scala/eo/OpticsLawsSpec.scala` — instantiate
  one fixture per (carrier, type-class) pair.

**Approach:**
- `ForgetfulFunctor`: identity + composition.
- `ForgetfulTraverse`: the three standard traverse laws (identity,
  naturality, sequential composition), parameterized by the constraint
  `C[_[_]]`.
- `AssociativeFunctor`: `associateLeft ∘ associateRight == identity` on
  both sides, associativity of chained associations.
- `Composer`: chain preserves `get`, chain preserves `modify`, chain is
  path-independent (the existing `ChainPathIndependenceLaws` and
  `ChainAccessorLaws` move into this file as first-class Composer laws).

**Deferred to 0.1.1** (called out here so future-us doesn't re-plan from
scratch): `ForgetfulFoldLaws` (one-equation monoid-homomorphism law —
already covered transitively by `ForgetfulTraverse` + existing
`FoldMapHomomorphismLaws` at optic level); `ForgetfulApplicativeLaws`
(similarly thin); `AccessorLaws` / `ReverseAccessorLaws` (one conditional
round-trip equation — will be valuable when a second carrier with these
instances lands, but adds noise today). Each of the four can be added in
~30 minutes when justified; no work is thrown away by waiting.

**Execution note:** test-first at the fixture level. Start with one
`ForgetfulFunctorTests[Tuple2]` call, watch it fail because the laws
class is empty, write the law, iterate.

**Patterns to follow:** existing `EoSpecificLaws.ChainPathIndependenceLaws`,
`MorphLaws`.

**Test scenarios:**
- Happy path — one `checkAll` per (carrier, type-class) pair that exists
  today: (Tuple2, ForgetfulFunctor), (Tuple2, AssociativeFunctor),
  (Either, ForgetfulFunctor), (Either, AssociativeFunctor),
  (Affine, ForgetfulTraverse[Applicative]),
  (SetterF, ForgetfulTraverse[Distributive]),
  (PowerSeries, ForgetfulTraverse[Applicative]).
- Happy path — `ComposerTests` green for each bridge direction
  (Tuple2 → PowerSeries, Either → PowerSeries, Affine → PowerSeries,
  PowerSeries → Affine).
- Integration — existing `ChainPathIndependence` behavior is preserved via
  the Composer laws.
- Edge case — `Forgetful` existential `X = Nothing` threads through
  composition without materializing a value (verified by successful
  compile of the law instance).

**Verification:**
- `sbt tests/test` green; at least ~10 new `checkAll` blocks.
- `EoSpecificLaws.scala` after this unit only contains the laws that are
  genuinely optic-level (MorphLaws, Compose of optics, Transform, Put,
  TraverseAll, ForgetAllModify) — carrier-level laws have moved.

- [x] **Unit 8: Coverage gap-fill to ≥85% on `core`** *(completed 2026-04-24 — core
  statement coverage raised from 58.83% to 86.07%, branch coverage from 52.84% to 85.28%.
  Three new spec files: `FusedAndThenSpec` covers 17 concrete-subclass fused `.andThen`
  overloads on Lens/Prism/Iso; `InternalsCoverageSpec` covers PSVec variants + the
  `IntArrBuilder` / `ObjArrBuilder` grow-on-demand paths + each `Reflector` instance's
  cats `Apply` delegation; `GrateCoverageSpec` covers the `Grate.at` factory and the
  `grateFunctor.map` direct-instance path. Test count 230 → 288.)*

**Goal:** Use the baseline from Unit 2 to identify the remaining
coverage gaps after Units 3–7 and write targeted tests to reach ≥85%.
Emphasize branches that Scalacheck would not have randomly triggered
(e.g., all four `Vect` constructors, `Affine.Fst` vs `Snd` branches,
`AssociativeFunctor` path selections).

**Requirements:** R3, R10.

**Dependencies:** Units 2–7.

**Files:**
- Modify: `tests/src/test/scala/eo/OpticsBehaviorSpec.scala` and the
  per-file specs created in Units 3–7.
- Create (if needed): `tests/src/test/scala/eo/coverage/*.scala` —
  targeted tests for branches that property-based fixtures missed.
- Update: `docs/solutions/2026-04-17-coverage-baseline.md` with the
  post-work numbers.

**Approach:**
- Run coverage, diff against baseline, list every file below 85%.
- For each, decide: (a) add a targeted test, (b) annotate as type-level
  only, (c) accept the gap with justification.
- Avoid writing trivial tests for coverage's sake — if a method is never
  called, delete it instead of testing it.

**Execution note:** None specific; this is remediation.

**Patterns to follow:** existing `OpticsBehaviorSpec.scala` style (`forAll`
properties, not one-off example tests).

**Test scenarios:**
- Happy path — every branch of `AssociativeFunctor` instances for
  `Tuple2`, `Either`, `Affine`, `Forgetful` is exercised at least once.
- Edge case — each `Vect` constructor path is hit (including
  `AdjacentVect` which a naive gen may skip).
- Error path — any genuinely unreachable branches are marked with
  `@unreachable` or rewritten to not exist.

**Verification:**
- `sbt "clean; coverage; tests/test; coverageReport"` shows `core`
  overall statement coverage ≥85% and branch coverage ≥80%.
- Every non-type-level file is ≥85% or has an explicit justification in
  the baseline doc.

- [ ] **Unit 9: Scaladoc uplift across public surface**

**Goal:** Bring every public trait / class / def in `core/` and `generics/`
to release-quality Scaladoc. Enable `-groups`, add `@tparam` for every
type parameter, add `@example` where short (≤5 lines) examples clarify
usage, use `@group Constructors` / `@group Operations` / `@group Instances`
uniformly on optic companions.

**Requirements:** R5.

**Dependencies:** Unit 8 (so the API is stable before we document it).

**Files:**
- Modify: `build.sbt` — add `Compile / doc / scalacOptions ++= Seq("-groups")`
  to `core`, `generics`, and `laws`.
- Modify: every file under `core/src/main/scala/eo/optics/` that is
  currently *light* or *medium* per the gap map — `Iso`, `Prism`,
  `Optional`, `Fold`, `Getter`, `Setter`, `Traversal`.
- Modify: every file under `core/src/main/scala/eo/data/` similarly.
- Modify: every file under `core/src/main/scala/eo/` (non-data) — Accessors,
  AssociativeFunctor, the Forgetful* type-class files.
- Modify: `generics/src/main/scala/eo/generics/LensMacro.scala`,
  `PrismMacro.scala`, `package.scala`.

**Approach:**
- On every optic companion: add `@group Constructors` on each `apply` /
  variant, `@group Operations` on combinators (`andThen`, `modify`,
  `replace`, `morph`), `@group Instances` on `given` blocks.
- On `Optic[S, T, A, B, F]`: document S/T/A/B/F with `@tparam` — this is
  the single biggest readability win.
- Keep examples short. Long examples go into the microsite (Unit 11).
- Run `sbt doc` and fix warnings: broken `@link`, unknown `@group`.

**Execution note:** None; pure documentation edit. Keep per-file commits.

**Patterns to follow:** existing heavy Scaladoc in `Optic.scala`,
`Lens.scala`, `ForgetfulFunctor.scala`.

**Test scenarios:**
- Test expectation: none — documentation change with no runtime behavior.
- Verify: `sbt doc` produces zero `[warn]`-level Scaladoc messages on
  `core` and `generics`.

**Verification:**
- `sbt doc` output under `core/target/scala-*/api/` and
  `generics/target/scala-*/api/` has `@group`-grouped sections visible on
  every optic companion.
- Every `type`, `trait`, `class`, `def`, `val` that is `public` in
  `core/src/main/scala/` has a `/** */` docstring.

- [x] **Unit 10: `site/` sub-project scaffold (sbt-typelevel-site + Laika + mdoc)**

**Goal:** Introduce the docs sub-project without writing content yet —
wire plugins, Laika config, mdoc variable substitutions, a smoke-test
`index.md`, and verify `sbt docs/tlSite` (or equivalent) produces a
local static site under `docs/target/`.

**Requirements:** R4.

**Dependencies:** Units 1–9 (the library needs to compile cleanly and
have its public surface documented before we point docs at it).

**Files:**
- Modify: `project/plugins.sbt` — add
  `addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.5")`.
- Modify: `build.sbt` — add a `docs` project rooted at `file("site")`,
  `enablePlugins(TypelevelSitePlugin)`, `tlSiteHelium := …` (match
  cats' setup), `mdocVariables += ("VERSION" -> version.value)`, depend
  on `core`, `laws`, `generics`.
- Modify: `build.sbt` — **explicitly pin** the transitive mdoc / Laika /
  scalafmt versions rather than relying on sbt-typelevel-site 0.8.5's
  year-old defaults. Start with: `mdocVersion := "2.8.0"` (or newer
  stable compatible with Scala 3.8.3), and leave a comment documenting
  why (sbt-typelevel 0.8.5 pins predate Scala 3.8). Verify with a single
  `sbt docs/mdoc` run that the pins resolve.
- Create: `docs/directory.conf` — Laika navigation order (initially
  stub: getting-started, concepts, optics, generics, cookbook,
  migration-from-monocle).
- Create: `docs/index.md` — one page smoke content that imports from
  `eo.*` in an mdoc fence and demonstrates a minimal Lens.
- Modify: `.gitignore` — add `docs/target/`, `site/target/`.

**Approach:**
- Follow cats's `build.sbt` shape verbatim for the `docs` project block,
  adjusting depends-on and project root.
- First smoke-test: `sbt docs/mdoc && sbt docs/tlSite` — the mdoc fence
  in `index.md` must compile against the current `core` and produce
  static HTML. If mdoc rejects Scala 3.8.3 (possible — sbt-typelevel
  0.8.5 predates it), bump `mdocVersion` explicitly and retry before
  deciding the toolchain is broken.
- Do not wire to GitHub Pages yet (Unit 14).

**Execution note:** None.

**Patterns to follow:** cats's [build.sbt docs section](https://github.com/typelevel/cats/blob/main/build.sbt).

**Test scenarios:**
- Happy path — `sbt docs/mdoc` succeeds and rewrites
  `site/target/mdoc/index.md` with the compiled code output.
- Happy path — `sbt docs/tlSite` produces `docs/target/docs/site/index.html`.
- Error path — breaking a code fence (e.g., typo `Lens.appply`) makes
  `sbt docs/mdoc` fail the build (this is the core value of mdoc).

**Verification:**
- `sbt docs/mdoc` passes.
- `docs/target/` contains a generated `site/` with working `index.html`.
- `docs/directory.conf` lists the intended top-level pages even if they
  are empty stubs.

- [x] **Unit 11: Docs content — guided tour, concepts, optics pages, cookbook, migration guide**

**Goal:** Populate the `docs/` tree with real content so a newcomer can
learn the library without reading source. Every code fence is
mdoc-compiled.

**Requirements:** R4.

**Dependencies:** Unit 10.

**Files:**
- Create: `docs/getting-started.md` — install, first Lens, first Prism,
  first Traversal.
- Create: `docs/concepts/existential-optics.md` — what the `F[X, A]`
  carrier means, why `X` is existential, how this differs from the
  profunctor encoding used by Monocle 3.
- Create: `docs/concepts/carriers.md` — Tuple2 / Either / Affine /
  SetterF / Forgetful / PowerSeries / FixedTraversal, one short
  section each with a canonical example.
- Create: `docs/concepts/laws.md` — what each law means, how to use
  `cats-eo-laws` in a downstream project.
- Create: one markdown per optic under `docs/optics/`: `iso.md`,
  `lens.md`, `prism.md`, `optional.md`, `traversal.md`, `setter.md`,
  `getter.md`, `fold.md`. Each includes: motivation, constructor
  survey, operations survey (`andThen`, `modify`, etc.), link to the
  discipline law set.
- Create: `docs/generics.md` — how `lens[S](_.field)` and
  `prism[S, A]` work (Scala 3 macros via Hearth), what's derived, what
  isn't, gotchas (outer accessors).
- Create: `docs/cookbook/` — `nested-records.md`,
  `adts-and-sum-types.md`, `json-paths.md` (migrated from
  `JsonOptic.scala`), `powerseries-heterogeneous.md` (the content from
  `Unthreaded.scala`).
- Create: `docs/migration-from-monocle.md` — side-by-side Lens /
  Prism / Traversal / Iso API mapping.
- Move: `Unthreaded.scala` content into `docs/cookbook/powerseries-heterogeneous.md`
  as mdoc fences; the test-side of `Unthreaded.scala` was already
  absorbed in Unit 6. Delete the `tests/` file once covered.
- Move: interesting bits of `JsonOptic.scala` into
  `docs/cookbook/json-paths.md`.
- Move: relevant parts of `Samples.scala` into the appropriate
  cookbook pages; the file can be deleted afterward or kept as a
  compile-time smoke test inside `tests/`.
- Modify: `docs/directory.conf` — final navigation order.

**Approach:**
- Every code fence is `scala mdoc`. Long imports go in an `mdoc:invisible`
  preamble.
- Side-by-side Monocle / cats-eo comparison in `migration-from-monocle.md`
  is the single most-important page for adoption; spend the time.
- Don't duplicate Scaladoc content — link to it from each page
  (`API reference: @:api(eo.optics.Lens)`).

**Execution note:** Write one cookbook page first, run `sbt docs/mdoc`,
make sure the round-trip works, then expand.

**Patterns to follow:** cats's `docs/` tree for tone and structure;
Monocle's tutorials for cookbook layout.

**Test scenarios:**
- Happy path — `sbt docs/mdoc` succeeds for every file.
- Integration — each "cookbook" fence compiles end-to-end against the
  current `core` (i.e., examples would not compile if someone renames
  `Lens.apply` without updating the page).
- Edge case — a fence that should *fail* (e.g., demonstrating a
  compile-time error in `docs/concepts/existential-optics.md`) uses
  `mdoc:fail` so mdoc verifies it fails for the right reason.

**Verification:**
- `sbt docs/mdoc` green on every page.
- `docs/target/site/index.html` navigates to all expected pages.
- Deleted `tests/src/test/scala/eo/Unthreaded.scala` and, if appropriate,
  `JsonOptic.scala` / `Samples.scala`.

- [x] **Unit 12: Benchmarks expansion — Fold, Getter, Optional, Setter, PowerSeries**

**Goal:** Mirror Monocle's trait-per-optic + paired
`EoXxxBench` / `MonocleXxxBench` pattern across the optics that do not
have benches yet. Shared fixture is a `Nested0..Nested6` case-class tree.

**Requirements:** R8.

**Dependencies:** Unit 1 (stable law structure); can proceed in parallel
with Units 10–11.

**Files:**
- Modify: `benchmarks/src/main/scala/eo/bench/OpticsBench.scala` or
  split into per-optic files under `benchmarks/src/main/scala/eo/bench/`:
  `LensBench.scala` (existing, refactor), `PrismBench.scala`,
  `IsoBench.scala`, `TraversalBench.scala`, plus new `FoldBench.scala`,
  `GetterBench.scala`, `OptionalBench.scala`, `SetterBench.scala`,
  `PowerSeriesBench.scala`.
- Create: `benchmarks/src/main/scala/eo/bench/fixture/Nested.scala` —
  shared `Nested0..Nested6` fixture (extract from whatever is currently
  embedded in `OpticsBench.scala`).
- Modify: `README.md` (benchmarks section) or new `benchmarks/README.md`
  — document the run commands and what each number means.

**Approach:**
- Priority order (land in this sequence, stop if time runs out — each
  standalone bench is a separate commit):
  1. **Fixture extraction** → `Nested.scala` (enables everything else).
  2. **`OptionalBench`** — Monocle has a direct equivalent; real
     side-by-side signal. Highest-value new bench.
  3. **`PowerSeriesBench`** — user-requested, EO-only (no Monocle
     equivalent), documents the new capability's cost.
  4. **`FoldBench`** — Monocle has an equivalent via `monocle.Fold`.
  5. **`GetterBench`** — Monocle has an equivalent.
  6. **`SetterBench`** — Monocle has an equivalent.
  Each of (4)–(6) is thinner: the underlying `Forget` / `Forgetful`
  carriers are already benched transitively through Lens / Traversal.
  Keep them in scope for completeness (user chose expansion), but do
  not sink a day each into them.
- Each bench: one `abstract class XxxBench` with method names
  (`foldFoldMap0`, `foldFoldMap3`, `foldFoldMap6`), implemented by
  `EoXxxBench extends XxxBench` (using cats-eo) and `MonocleXxxBench
  extends XxxBench` (using Monocle) where Monocle has the equivalent.
  For `PowerSeriesBench`, there is no Monocle equivalent — bench only
  the EO variant, document that clearly.
- Use `@BenchmarkMode(Array(Mode.AverageTime))` and
  `@OutputTimeUnit(TimeUnit.NANOSECONDS)` for the shallow cases,
  `MICROSECONDS` for the deep (`_6`) ones.
- Do not add a CI job. Document in `benchmarks/README.md` how to run
  locally with `-i 5 -wi 3 -f 3 -t 1` for trustworthy numbers.

**Execution note:** None.

**Patterns to follow:** Monocle's
[bench/src/main/scala/monocle/bench/](https://github.com/optics-dev/Monocle/tree/master/bench/src/main/scala/monocle/bench).

**Test scenarios:**
- Happy path — `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*Bench"`
  compiles and runs every bench to completion with plausible numbers
  (not zero, not NaN).
- Edge case — shallow benches (`_0`) produce non-zero nanosecond numbers;
  deep benches (`_6`) produce non-zero microsecond numbers.
- Integration — side-by-side report row shows both `EoLensBench.lensGet3`
  and `MonocleLensBench.lensGet3` in the same JMH output.

**Verification:**
- `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*" ` produces a complete
  table (no missing benches, no exceptions).
- Documented run commands in `benchmarks/README.md` reproduce the
  historical Lens numbers within the same order of magnitude.

- [x] **Unit 13: Release infrastructure — plugins, build settings, MiMa, Central Portal wiring**

**Goal:** Turn `build.sbt` into a publishable configuration. Wire
`sbt-typelevel-ci-release` and its friends, set
`tlBaseVersion := "0.1"`, configure MiMa (disabled on 0.1.0), declare
developers / homepage / licenses / scmInfo.

**Requirements:** R6, R7.

**Dependencies:** Unit 1 for stable module naming; can proceed in parallel
with Units 2–12.

**Files:**
- Modify: `project/plugins.sbt` — add
  `addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "0.8.5")`.
  Drop any plugin that conflicts.
- Modify: `build.sbt`:
  - Add `ThisBuild / tlBaseVersion := "0.1"`.
  - Add `ThisBuild / organization := "<TBD — confirm with user>"`.
    Current project is hosted under the user's personal account and
    uses `rodolfo.hansen@adtechnacity.com` git email. The Sonatype
    Central Portal namespace must be either a reverse-DNS domain the
    user controls (`com.adtechnacity`, `io.github.<username>`, …) or a
    GitHub-verified `io.github.<username>` coordinate. Pick **before**
    starting DNS / GitHub verification.
  - Add `ThisBuild / organizationName := "…"`.
  - Add `ThisBuild / homepage := Some(url("https://github.com/…"))`.
  - Add `ThisBuild / licenses := Seq(License.Apache2)` (or the license
    `LICENSE` file actually specifies — verify).
  - Add `ThisBuild / developers := List(…)`.
  - Add `ThisBuild / tlMimaPreviousVersions := Set.empty` on 0.1.0.
  - Set `name` of `root` project correctly; ensure `publish / skip` on
    `root`, `tests`, `benchmarks`, `docs`.
  - Remove `version := "0.1.0-SNAPSHOT"` from `commonSettings` — the
    plugin derives version from tags.
- Create: `mima.sbt` (or inline) — empty on 0.1.0 for future filters.
- Modify: `CLAUDE.md` release-related sections — point at the new
  `sbt ci-release` flow.

**Approach:**
- Follow cats's `build.sbt` structure for the `ThisBuild` block.
- Confirm the license file matches what `LICENSE` actually says (it
  appears 11.1K — likely Apache 2.0, verify).
- **Central Portal namespace steps** (required before Unit 14 can issue a
  real release). These are external to the build but must happen in this
  unit so that CI secrets are ready:
  1. Register the chosen namespace at
     <https://central.sonatype.com/publishing/namespaces>.
  2. If `com.<domain>`: add the DNS TXT record Central asks for on the
     domain's DNS. Verify resolves (`dig TXT <domain>`).
  3. If `io.github.<username>`: create the verification repository on
     GitHub as instructed.
  4. Wait for Central to confirm (documentation says up to 48h; in
     practice often minutes).
  5. Only then generate GPG keys and upload the public key to
     `keys.openpgp.org` for the signing step.
  6. Document every credential needed by CI (Unit 14) — but do **not**
     commit them anywhere.
- **Namespace-reuse gotcha:** if the chosen `com.<domain>` or
  `io.github.<username>` coordinate was ever used under legacy OSSRH,
  registration in the Central Portal will bounce. Confirm the coordinate
  is unused first; if in doubt, choose a fresh one.
- Verify the organization / GitHub URL with the user before shipping.

**Execution note:** None.

**Patterns to follow:**
- Cats `build.sbt`.
- Monocle `build.sbt`.
- [sbt-typelevel FAQ](https://typelevel.org/sbt-typelevel/faq.html).

**Test scenarios:**
- Happy path — `sbt compile` still works.
- Happy path — `sbt +publishLocal` produces expected artifacts
  (`cats-eo_3-0.1.0-SNAPSHOT.jar` etc.).
- Integration — `sbt "mimaReportBinaryIssues"` runs (no-op on 0.1.0 but
  must not error).

**Verification:**
- `sbt publishLocal` succeeds; artifacts include source jar and
  Scaladoc jar.
- `sbt mimaReportBinaryIssues` succeeds (empty comparison set).
- `build.sbt` and `project/plugins.sbt` reviewed for hand-rolled
  publishing remnants.

- [x] **Unit 14: CI workflows + publish secrets**

**Goal:** Write `.github/workflows/ci.yml` and `release.yml` so every PR
gets validated and every git tag triggers a release. Scaffold the
required GitHub Secrets for the Central Portal.

**Requirements:** R7.

**Dependencies:** Unit 13.

**Files:**
- Create: `.github/workflows/ci.yml` — matrix `scala: [3.8.3]`,
  `java: [temurin@17, temurin@21]`, steps: checkout,
  `sbt scalafmtCheckAll scalafmtSbtCheck`, `sbt "compile; tests/test;
  generics/test"`, `sbt coverageReport` (JDK 21 only),
  `codecov/codecov-action@v5`, `sbt doc`, `sbt docs/mdoc`,
  `sbt mimaReportBinaryIssues`.
- Create: `.github/workflows/release.yml` — on `v*` tag push, run
  `sbt ci-release`, then `sbt docs/tlSitePreview` +
  `actions/deploy-pages@v4` (or equivalent GH-Pages push).
- Create: `.github/dependabot.yml` — weekly sbt-plugin updates.
- Modify: `README.md` — add build-status / maven-central / codecov
  badges (stubs resolved in Unit 15 once secrets exist).
- Document (in this plan + CONTRIBUTING.md): the four GitHub Secrets
  required: `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `PGP_SECRET`,
  `PGP_PASSPHRASE`.

**Approach:**
- Start with cats' `.github/workflows/ci.yml` as a template; delete
  Scala.js / Native / Scala-2 rows that do not apply; keep the
  scalafmt + mdoc + MiMa + test steps.
- The `scalafix --check` gate is listed in R7 but we do not have a
  `.scalafix.conf` yet. Add one containing at minimum `RemoveUnused`
  and `NoAutoTupling`, wire the gate, verify clean.
- Coverage upload runs on JDK 21 only to avoid duplicate reports.
- Secrets config is documentation only — the user provisions them in
  GitHub Settings; this plan does not insert real credentials.

**Execution note:** Test each workflow via `act` locally if available, or
via a throwaway PR branch that deliberately fails scalafmt to confirm
the gate triggers.

**Patterns to follow:**
- Cats `ci.yml` and `release.yml`.
- [sbt/sbt-ci-release](https://github.com/sbt/sbt-ci-release).

**Test scenarios:**
- Happy path — a PR with a clean branch has every check green.
- Error path — a PR with a scalafmt violation fails the `Format` job
  clearly.
- Error path — a PR that bumps a public method's signature (forcing a
  MiMa issue on 0.1.1+, not 0.1.0) is caught by `mimaReportBinaryIssues`.
- Integration — tagging `v0.1.0` triggers `release.yml`, which pushes
  signed artifacts to Central Portal and the built site to GH Pages.

**Verification:**
- A clean PR passes CI with all jobs green.
- A deliberately broken PR fails the right job.
- `.github/workflows/release.yml` dry-run via `act` (if available) or
  documented as "first real run happens on the 0.1.0 tag."

- [x] **Unit 16: Composition-gap top-3 closures (R11) + structural disposition (R12)**

**Goal:** Close the three highest-priority gaps from the composition-gap
analysis at the behaviour-spec level, and ship the two structural-boundary
docs paragraphs so users stop hitting silent implicit misses.

**Requirements:** R11, R12.

**Dependencies:** Unit 1 (stable laws layout). Can run in parallel with
Units 9-12. Work overlaps materially with Unit 8 (Optional fused-overload
coverage) and with Unit 11 (docs content for the terminal-carrier
paragraphs) — land Unit 16 first or in the same PR as the overlap to avoid
double-editing.

**Files:**
- Modify: `tests/src/test/scala/eo/OpticsBehaviorSpec.scala` — add
  property-based behaviour rows for:
  - **R11a block** — `Traversal.each × Iso`, `Traversal.each × Optional`,
    `Traversal.each × Prism`, and 2-level-nested
    `Traversal.each.andThen(Traversal.each)`. Reference fixtures: a
    `List[Option[Int]]` or `List[Result]` shape for the Prism variant; a
    `List[(A, A)]` or similar for nested-each. Parallel to the bench-only
    coverage these already get in `benchmarks/`.
  - **R11b block** — behaviour spec per Optional fused overload
    (`Optional.andThen(GetReplaceLens | MendTearPrism | Optional |
    Traversal.each)` at `Optional.scala:123-188`). Each spec calls the
    fused path and the unfused-via-carrier path on the same fixture and
    asserts `.modify` / `.get` equivalence. These fixtures double as the
    coverage fix for `core/optics/Optional.scala` (21.92 % → ≥85 %).
  - **R11c** — if the decision on Setter composition is "ship the
    carrier instances" (see OQ-R2), add `setter.andThen(setter)` +
    `lens.andThen(setter)` property specs. If the decision is "document
    as terminal", no spec changes here; the docs work falls under Unit 11
    (or Unit 11.5 below).
- Modify: `site/docs/optics.md` — append short "terminal-carrier" paragraphs
  for `SetterF` and `FixedTraversal[N]`, naming the implicit miss the user
  will hit and the workaround. Append an "AlgLens[F] is a sink" paragraph
  explicitly (the section exists but understates it per composition-gap §5.1).
- Modify: `site/docs/optics.md` — add a "Grate composes only with Iso"
  paragraph cross-referencing plan 004 D3 for the Rep/Distributive
  argument; the composition-gap §3.2.4 lists every absent Composer pair
  that should be named there.

**Approach:**
- Land the Traversal-each-outer family first (R11a) — it's the
  user-facing chain the tests undercover most visibly. The `Composer[Either,
  PowerSeries]` (`PowerSeries.scala:362`) + `Composer[Affine,
  PowerSeries]` (`PowerSeries.scala:407`) fast paths ALL need at least
  one behaviour spec.
- For R11b, pick the 5 most-user-facing fused paths per
  `Iso` / `Prism` / `Optional` / `Lens` concrete subclass (code-quality
  review recommendation §"Nice-to-have #8"). Each fused overload is a 3-line
  spec calling `.andThen(…).modify(…)` and asserting equivalence with the
  unfused path.
- R11c: the Setter decision is OQ-R2; resolve at the top of the unit, then
  either ship the carrier instances or land the docs prose. Do not leave
  it ambiguous.

**Execution note:** Test-first for R11a and R11b. Write failing forAll
properties that exercise the chain, then verify they pass on current
code (because the fast paths do work — they're just untested).

**Patterns to follow:** existing `OpticsBehaviorSpec.scala` style; the
cross-carrier composition tests already in `CrossCarrierCompositionSpec`.

**Test scenarios:**
- Happy path — every R11a chain `.modify(f).get == f.applied.get` for a
  `forAll` on List/Option fixtures.
- Happy path — every R11b Optional fused path is byte-for-byte equivalent
  to the carrier-derived path on the same fixture.
- Edge case — Prism miss inside a Traversal.each propagates as
  "element unchanged" per PowerSeries semantics.
- Error path — n/a (these are pure total optics; error paths live in
  circe / Ior land, not here).

**Verification:**
- `sbt tests/test` green with ≥10 new `forAll` blocks.
- Coverage re-run shows `core/optics/Optional.scala`, `Iso.scala`,
  `Prism.scala`, `Lens.scala` above the 80 % line (R13 target).
- `site/docs/optics.md` renders the new terminal-carrier paragraphs under
  the Setter / FixedTraversal / AlgLens sections; mdoc green.

- [x] **Unit 17: Code-quality review must-fix + circe-coverage wiring (R13, R14)**

  *(completed 2026-04-24 — dead-code removals landed; `ForgetfulApplicative`
  stub + `assocForgetComonad` both pinned leave-alone per user decision;
  Grate sentinel invariant statement strengthened inline; `CLAUDE.md`
  coverage incantation extended to include `circeIntegration/test` +
  `coverageAggregate`. High-impact companion Scaladocs added
  (`Accessor`, `ReverseAccessor`, `Morph`, `PSVec`, `Optic`, `SimpleLens`,
  `AlgLens`, `Review`); finer-grained per-given Scaladoc gap-fill deferred
  to Unit 9's broader Scaladoc uplift to avoid duplicative work.)*

**Goal:** Close the code-quality review's "Must-fix for 0.1.0" items and
repair the scoverage pipeline so circe is no longer silently skipped.

**Requirements:** R13, R14.

**Dependencies:** None structural — can run anywhere after Unit 1. Should
land before Unit 15 (release cut) so the tag ships the cleaned surface.

**Files:**
- Modify: `core/src/main/scala/eo/optics/Lens.scala` — remove the unused
  `given tupleInterchangeable[A, B]: (((A, B)) => (B, A))` at lines 22-24
  (code-quality review §core/YAGNI; 3 LoC).
- **Leave alone**: `core/src/main/scala/eo/ForgetfulApplicative.scala` —
  the empty `object ForgetfulApplicative` stub at line 19. Review
  §core/Dead-or-unreachable flags it as 2 LoC of dead code, but the
  stub's Scaladoc documents it as an intentional extension point for
  user-supplied instances (matching the pattern on the sibling
  capability typeclasses). **Pinned 2026-04-24** against the review's
  recommendation; keeping for consistency with the rest of the capability
  typeclass surface. Revisit only if a concrete simplification proves
  that user extension isn't wanted there.
- Modify: `core/src/main/scala/eo/data/SetterF.scala` — remove the unused
  `[S, A]` type parameters on `given map` (line 26) and `given traverse`
  (line 38) (review §core/Dead; ~4 LoC).
- Modify: `circe/src/main/scala/eo/circe/JsonFieldsTraversal.scala` —
  drop or wire up the unused `@annotation.unused original: Json`
  parameter at lines 313-318 (review §circe/Dead).
- Modify: `core/src/main/scala/eo/data/Grate.scala` — keep the
  `null.asInstanceOf[Xo]` sentinels at lines 108, 178 but add an
  `assert(false, "...")`-style guard or a code comment naming the exact
  invariant each sentinel relies on. **Do NOT invoke the sentinel at
  runtime** — the cast stays sound for every v1 carrier; the change is
  documentary + defensive.
- Create: `core/src/test/scala/eo/GrateSentinelInvariantSpec.scala` (or
  add to existing `GrateSpec.scala`) — one fixture that pins the
  invariant "no current Grate carrier observes `.to(fa)._1` before
  `composeFrom` replaces it". If the test is inherently a compile-time
  property (i.e. no runtime check possible without adding a hostile
  carrier), convert to a `// coverage-sentinel` comment + a
  `compileErrors` negative test. Review recommendation §7.
- Modify: `build.sbt` or `CLAUDE.md`'s coverage command — extend the
  coverage incantation from `sbt "clean; coverage; tests/test;
  coverageReport"` to include `circeIntegration/test` so circe appears
  in the scoverage report. Update
  `docs/solutions/2026-04-17-coverage-baseline.md` to reflect the new
  coverage of `circe/` (was "No coverage data"; now real numbers).
- Modify: Scaladoc gaps — add the ~30 one-line "companion for [[X]]"
  docstrings enumerated in review §core/Scaladoc-gaps +
  §circe/Scaladoc-gaps + §generics/Scaladoc-gaps. Covers:
  - `Accessor` / `ReverseAccessor` companions, `Morph` trait members +
    companion, `Optic` companion, `SimpleLens` / `Review` companions,
    `AlgLens` / `PSVec` companions, the four `AlgLensFromList` instances
    (if public), the three `FixedTraversal` given instances, six
    `PowerSeries` givens.
  - `JsonFailure` / `JsonPrism` / `JsonFieldsPrism` / `JsonTraversal`
    companions + `type X` members.
  - `PartiallyAppliedLens` class header in `generics/package.scala`.
- Modify: `laws/src/main/scala/eo/laws/eo/ChainLaws.scala:59-60` —
  add per-given docstrings for `accessorF` / `accessorH` abstract
  members.

**Approach:**
- Do the dead-code removals in one PR (very small diff, mechanical).
- Scaladoc gap-fill in a second PR — ~30 one-line edits; bundle them
  so reviewers can skim a single diff.
- Grate sentinel work in a third, isolated PR since it touches
  correctness reasoning.
- Coverage-incantation fix lives in whatever PR the release-pipeline
  changes land in; the `docs/solutions/` update becomes a small 3rd
  commit in that PR.
- Finding #1 from the review (orphan `Reflector.scala`) is **NOT**
  acted on — Kaleidoscope Units 1-4 landed after the review ran;
  `Reflector` is the live substrate for that family, not orphan.
  Call this out in the PR description.

**Execution note:** None special. Each sub-task is S-sized.

**Patterns to follow:**
- Existing Scaladoc shape in `Optic.scala`, `Lens.scala`,
  `ForgetfulFunctor.scala` is the per-member model for the docstring
  additions.
- The `docs/solutions/2026-04-17-coverage-baseline.md` format is the
  model for the coverage-incantation note.

**Test scenarios:**
- Happy path — `sbt test` still green after dead-code removal.
- Integration — `sbt doc` runs without Scaladoc warnings on the
  surface; Unit 9's zero-warning goal survives.
- Regression — `sbt "clean; coverage; tests/test; circeIntegration/test;
  coverageReport"` produces a coverage row for `circe/` (was empty
  before).

**Verification:**
- `git grep tupleInterchangeable core/` returns empty.
- `sbt doc` zero-warning on `core`, `laws`, `generics`, `circe`.
- Coverage report's circe module is populated.
- Review §"Must-fix for 0.1.0" checklist items 1-7 all complete.

- [ ] **Unit 18: Mermaid diagrams — composition lattice, Ior failure-flow, family taxonomy (R15a)**

**Goal:** Ship the three priority diagrams from the
cookbook-sources research (§Track 2 shortlist) so users can orient on the
cross-carrier composition story, the Ior surface, and the family tree at a
glance. Laika's Helium theme ships Mermaid support natively — no new build
plumbing required.

**Requirements:** R15a.

**Dependencies:** Unit 11 (site content exists to embed into).

**Files:**
- Modify: `site/docs/concepts.md` — embed **D1 (composition lattice)**
  immediately after the "Cross-family composition" section. ~40 lines of
  Mermaid `flowchart LR`: nodes = carriers (`Tuple2`, `Either`,
  `Forgetful`, `Affine`, `SetterF`, `Forget[F]`, `PowerSeries`, `Grate`,
  `AlgLens[F]`, plus `Kaleidoscope` if plan 006 lands); edges = shipped
  `Composer[F, G]` givens labelled by direction + `Composer` /
  `Composer.chain` / `Morph.bothViaAffine`. Effort: **S**.
  Edge set is enumerated in
  [composition-gap §0.3](../research/2026-04-23-composition-gap-analysis.md#03-ledger-of-given-instances).
- Modify: `site/docs/circe.md` — embed **D5 (JsonPrism Ior failure-flow
  decision graph)** next to the "Reading diagnostics" section. Mermaid
  `flowchart TD`: input = `Json | String`; terminals = `Ior.Right`,
  `Ior.Both`, `Ior.Left`; decision branches per `JsonFailure` case
  (`ParseFailed`, `PathMissing`, `TypeMismatch`, `DecodeFailed`) +
  collect-on-traversal-skips. Effort: **M**. Verify cases against
  `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` so each branch
  matches shipping code.
- Modify: `site/docs/optics.md` — embed **D3 (optic family taxonomy
  tree)** at the top of the page as an at-a-glance index. Mermaid
  `flowchart TD`: root `Optic[S, T, A, B, F]`, branching by carrier,
  leaves at concrete families (Iso, Lens, Prism, Optional, AffineFold,
  Getter, Setter, Fold, Traversal.each / .forEach, FixedTraversal[N],
  AlgLens[F], Grate; add Kaleidoscope if plan 006 ships in 0.1.0). Link
  leaves to in-page anchors via Mermaid node links. Effort: **S**.
- **Optional stretch (if time):**
  - **D4** — `site/docs/circe.md` or `extensibility.md` — sequence
    diagram of the `modifyImpl` walk (root → field step → leaf →
    rebuild-up). Effort: M.
  - **D9** — `site/docs/concepts.md` — one concrete cross-carrier
    ladder (`Iso → Lens → Traversal.each → Prism → Lens`) rendered
    as linear `flowchart LR` with per-edge Morph / Composer labels.
    Effort: M.
  - **D7 simplified** — `site/docs/benchmarks.md` — two-box "naive
    PowerSeries vs. flat-array" comparison. Effort: S–M.

**Approach:**
- Start with D1 (S-effort, highest payoff) → verify it renders in local
  `sbt site/tlSitePreview` → repeat for D3 and D5.
- Cross-reference existing `concepts.md` text and the composition-gap
  §0.3 ledger when drafting D1; every edge must be a shipping given,
  not a hypothetical.
- Keep labels short; two-word `Composer` / `chain` / `morph-via-Affine`
  style. Helium's theme colours the nodes automatically; do not
  override.
- D5 is the only diagram with real decision-branching semantics —
  draft, preview, refine until every `JsonFailure` terminal makes
  sense to a reader who hasn't read `JsonFailure.scala`.

**Execution note:** mdoc doesn't parse Mermaid fences (they're opaque
to the Scala compiler), so no mdoc changes are needed. A site rebuild
is enough.

**Patterns to follow:**
- Laika's Helium theme Mermaid support docs in the research file
  (§Rendering tech survey).
- cats / Cats-Effect sites don't use Mermaid yet — we're breaking new
  ground in the Typelevel-adjacent space. Keep diagram density high
  per page (two or fewer per page).

**Test scenarios:**
- Happy path — `sbt site/tlSitePreview` renders all three diagrams
  inline without console warnings.
- Edge case — a malformed Mermaid fence causes a console warning
  only (does NOT fail the build) — so manually review each diagram
  after local preview before commit.

**Verification:**
- Local preview shows all three diagrams.
- Each diagram's nodes match shipping code (one pass to reconcile
  with the composition-gap ledger and `JsonFailure` ADT).
- Links / anchors in D3 leaves resolve to their in-page sections.

- [ ] **Unit 19: Cookbook recipe consolidation (R15b, R15c)**

**Goal:** Converge `site/docs/cookbook.md` on ~18-22 recipes drawn from
the 30-recipe research catalogue, merging adjacent recipes where one
page can cover two motivating problems, and attribute each recipe to
its literature source (Penner's *Optics By Example*, Monocle docs,
Haskell `lens` tutorial, circe-optics, etc.).

**Requirements:** R15b, R15c.

**Dependencies:** Unit 11 (base cookbook content exists), ideally Unit 18
(diagrams are available to cross-link from recipe prose).

**Files:**
- Modify: `site/docs/cookbook.md` — target structure (working draft,
  refine during authoring):
  - **Theme A (Product editing)** — keep recipes 1-4 (deep-nested
    coordinate; Celsius↔Fahrenheit virtual field; multi-field
    NamedTuple focus; full-cover Iso upgrade). Recipes 3 and 4 are
    cats-eo-unique — lead with them.
  - **Theme B (Sum-branch)** — merge recipes 5-6 into one "Prism +
    Lens chain" section; keep recipe 7 (Review → ID construction) as
    its own section under "Write-only" (theme F).
  - **Theme C (Collection walks)** — merge recipes 8-9 into one
    "each vs. forEach" section; keep recipes 10-11 distinct (sparse
    traversal, AlgLens teaser); recipe 12 (Iris classifier) stays as
    a cross-link to plan 002 (not inline).
  - **Theme D (JSON / tree)** — merge recipes 13-15 into one three-act
    JSON arc (edit leaf / edit array / diagnose); keep recipes 16-17
    separate (jq-style filter; recursive rename).
  - **Theme E (Algebraic)** — merge recipes 11, 18 into one AlgLens
    vignette (partition + z-shift). Recipe 19 (Kaleidoscope) becomes
    a cross-reference to plan 006 *or* if plan 006 Units 5-7 land in
    time, a first-class recipe.
  - **Theme F (Write / read escape)** — keep recipes 7 (Review),
    20 (Getter), 21 (Setter), 22 (AffineFold).
  - **Theme G (Composition)** — recipe 23 (three-family ladder) is
    load-bearing; place it prominently with a cross-link to D1 (the
    composition-lattice diagram from Unit 18).
  - **Theme H (Effectful)** — keep recipes 25, 26 (batch-load 100×);
    27 (Witherable) remains deferred cross-ref.
  - **Theme I (Observable failure)** — keep 28, 29, 30; cross-link
    from D5 (Ior failure-flow).
- Modify: each recipe gets an "attribution" line naming source
  (Penner chapter reference for recipes mapped from *Optics By
  Example*; Gonzalez's Hackage tutorial for the Atom/Molecule
  example; Monocle docs for the Getter / Setter baseline; circe-optics
  for the `root.*` JSON-path context).
- Expected final count: **~20 recipes** (in the research review's
  18-22 target band).

**Approach:**
- Re-use recipe prose from the research doc's §Theme sections where
  the framing is already tight; don't invent new examples
  gratuitously.
- Every code fence is mdoc — leverage existing Unit 11 infrastructure.
- Attribution style: "Source: Penner — *Optics By Example* ch. 7,
  <https://leanpub.com/optics-by-example/>" inline at the end of each
  recipe (parallel to existing solution-doc citation style).
- Where a recipe flags a gap (e.g. recipe 11's `.classify` API
  doesn't exist), either implement the gap in-scope (only if
  trivially small; usually not), or rephrase the recipe around the
  shipping surface + a "future work" cross-reference.
- If plan 006 Kaleidoscope lands in time, promote recipe 19 from
  deferred to real; otherwise keep as cross-reference.

**Execution note:** Write one theme at a time, run `sbt site/mdoc`,
verify, move on. Avoid large-batch authoring.

**Patterns to follow:**
- cats docs `cookbook/` framing (problem → code → why).
- The research catalogue's recipe shape (Problem / Optics / Outline /
  Source / Why / cats-eo angle) is the direct template.

**Test scenarios:**
- Happy path — `sbt site/mdoc` green on every page after recipe
  consolidation.
- Edge case — a recipe citing a shipping gap (e.g. 11 without
  `.classify`) compiles via the documented workaround, not the
  unshipped surface.
- Integration — every cross-reference (D1/D3/D5 diagrams, plan 002
  iris example, plan 006 Kaleidoscope, plan 007 indexed deferral)
  resolves.

**Verification:**
- `sbt site/mdoc` green.
- Final recipe count ∈ [18, 22].
- Every cats-eo-unique recipe (3, 4, 10, 13-15, 23, 26, 28-30) is
  retained.
- Penner attribution appears on every Penner-sourced recipe.

- [ ] **Unit 20: Kaleidoscope cross-scope coordination (conditional — plan 006 Units 5-7)**

**Goal:** Track plan 006's remaining units (3 of 7) as in-flight
conditional scope for 0.1.0. If plan 006 Units 5-7 land in time (~1-2
weeks real-time), the Kaleidoscope family ships in 0.1.0 with laws,
bench, docs, and cookbook mention. If they do not, Kaleidoscope cleanly
defers to 0.1.1 — Units 1-4 already on `main` are forward-compatible,
no rollback needed.

**Requirements:** R15b (if Kaleidoscope is in, cookbook recipe 19 is
promoted to first-class per Unit 19's structure).

**Dependencies:** plan 006 Units 5-7. No strict ordering with Units 16-19.

**Files (pass-through to plan 006):**
- See
  [`docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md`](./2026-04-23-006-feat-kaleidoscope-optic-family-plan.md)
  for the remaining units:
  - Unit 4 — `Composer[Forgetful, Kaleidoscope]` + law-level fixture.
    *Landed `c79dd7a`.*
  - Unit 5 — `KaleidoscopeLaws` + `KaleidoscopeTests` + `checkAll`
    fixtures. *Pending.*
  - Unit 6 — `KaleidoscopeBench` (EO-only, two fixtures). *Pending.*
  - Unit 7 — `site/docs/optics.md` Kaleidoscope section + `concepts.md`
    carrier-row update + `cookbook.md` Kaleidoscope vignette. *Pending.*

**Approach:**
- No work *in this unit* — this is a tracking / coordination entry so
  the 0.1.0 plan's dependency graph reflects the open scope.
- **Go / no-go decision point** — when Unit 14 (CI) and Unit 17
  (code-quality cleanup) are both ready to merge, inspect plan 006's
  state: if Units 4-5 are landed and 6-7 are realistic within a week,
  Kaleidoscope ships in 0.1.0; otherwise, tag without it and retarget
  Kaleidoscope for 0.1.1.
- If Kaleidoscope ships: update Unit 18's D1 (composition lattice)
  and D3 (family taxonomy) to include Kaleidoscope. Update Unit 19's
  recipe 19. Update CHANGELOG entry (Unit 15).
- If Kaleidoscope defers: add a 0.1.1 teaser entry to CHANGELOG.

**Execution note:** None — meta-unit.

**Patterns to follow:** plan 006's own unit structure.

**Test scenarios:** n/a — see plan 006.

**Verification:**
- Plan 006 Units 5-7 are either all marked `- [x]` (Kaleidoscope in)
  or the go/no-go decision is recorded in this plan with a date.
- CHANGELOG reflects the final disposition.

- [x] **Unit 21: Resolve the `?` composition-gap cells (OQ-R1)** — completed 2026-04-24. Shipped `Composer[Affine, AlgLens[F]]` (`affine2alg`), pinned AlgLens-outbound and Traversal.each × {Fold, Tf, AlgLens} as designed-`U`, deferred cross-F Forget composition to 0.2.x, and documented every idiom in `site/docs/optics.md` "Composition limits" subsection. Behaviour specs in `OpticsBehaviorSpec` for the new Composer; gap-analysis updated with §7 resolution scoreboard (12 → 0 `?` cells).

**Goal:** Before tag, each of the 12 `?` cells from the composition-gap
analysis §2.2 gets one of three dispositions:
(a) experimental compile-run to flip ? → N (5-minute prompt each),
(b) ship a new Composer / AssociativeFunctor given,
(c) explicit deferral entry in `site/docs/optics.md`.

**Requirements:** R12, OQ-R1.

**Dependencies:** Unit 1 (laws layout); ideally Unit 7 (type-class
laws — new Composers trigger them).

**Files:**
- Create or modify: one-off Scala REPL / scratch file that walks every
  `?` pair and pins its disposition. Not checked in.
- Modify: `site/docs/optics.md` — add a "composition coverage" section
  (or extend the existing one) listing the final (N / M / U / deferred)
  disposition per cell. This becomes part of the release-notes
  transparency for 0.1.0.
- Modify (conditional): if any `?` cell gets a new Composer / assoc
  given, the relevant `core/src/main/scala/eo/data/*.scala` file plus
  a matching law fixture under `laws/` + a behaviour spec under
  `tests/`.

**Approach:**
- Batch the 12 cells as three groups per composition-gap §2.2:
  - Group 1: Iso / Lens / Prism / Optional × Fold / Traversal.forEach
    (8 cells) — most likely disposition is "document as M" (compose
    via `foldMap` or `AlgLens.fromLensF`).
  - Group 2: Traversal.each × {Fold, Traversal.forEach, AlgLens}
    (3 cells) — likely disposition is "defer, track in 0.2.x".
  - Group 3: Cross-F Traversal.forEach × Traversal.forEach (1 cell)
    — disposition is "defer" (needs a `Composer[Forget[F], Forget[G]]`
    with a transformation witness).
- For any cell flipped to N via a new given, the work cascades into
  Unit 16 (add a behaviour spec) and Unit 7 (add a law-level fixture
  for the new given's `AssociativeFunctor` / `Composer` witness).

**Execution note:** 5 minutes per cell for the experimental check;
hours per cell if the disposition is "ship new given". Budget 1 day
total assuming most dispositions are (a) or (c).

**Patterns to follow:** composition-gap analysis §2 matrix is the
authoritative starting point; §3.3 enumerates the 12 cells with a
best-guess disposition per cell.

**Test scenarios:**
- For each cell flipped to N via a new given: a behaviour-spec row
  under `OpticsBehaviorSpec` that exercises the chain.
- For each cell kept as M or U: a docs entry naming the workaround.

**Verification:**
- `site/docs/optics.md` has a table of 12 cells with dispositions +
  1-line justifications.
- If Unit 21 adds Composers, all three of {law, behaviour spec,
  coverage} are populated.

- [ ] **Unit 15: README + CHANGELOG + CONTRIBUTING, and cut the 0.1.0 release**

**Goal:** Write the three top-level markdown files a public project needs,
then tag `v0.1.0` and trigger the release workflow.

**Requirements:** R4, R6, R9.

**Dependencies:** Units 11, 12, 14, 16, 17, 18, 19, 20 (Kaleidoscope
go/no-go must be decided), 21.

**Files:**
- Create: `README.md` — overwrite the sbt stub. Sections: summary (1
  sentence + logo if any), install (sbt snippet), 60-second Lens example,
  table of optics with one-line descriptions, links to site sections,
  badges (build, maven-central, codecov, scaladoc), development +
  release pointers, license line.
- Create: `CHANGELOG.md` — Keep-a-Changelog format. Initial entry:
  `## [0.1.0] — 2026-MM-DD` listing the landed scope:
  - "First public release."
  - "Full discipline-checked law coverage for every public optic (Iso,
    Lens, Prism, Optional, AffineFold, Getter, Setter, Fold, Traversal,
    Grate, AlgLens[F]) and every shipped carrier (Affine, SetterF,
    Vect, PowerSeries, FixedTraversal[N], AlgLens, Grate)."
  - "mdoc-verified docs site with optic-family taxonomy, composition
    lattice, and Ior failure-flow diagrams."
  - "Benchmarks vs. Monocle for Lens / Prism / Iso / Traversal / Fold /
    Getter / Optional / Setter; EO-only benches for PowerSeries /
    Grate / (Kaleidoscope if shipped)."
  - "circe integration — JsonPrism / JsonTraversal / JsonFieldsPrism /
    JsonFieldsTraversal with observable-by-default failure (Ior
    surface)."
  - "`lens` / `prism` generics macros with multi-field NamedTuple
    focus and full-cover Iso upgrade."
  - "Sonatype Central Portal publishing via sbt-typelevel-ci-release."
  - **Breaking changes relative to pre-v0.2 surface** — promote to a
    dedicated "Changed" subsection: the circe observable-failure
    migration (plan 005; `JsonPrism!` commits, `Json | String` input
    widening, `*Unsafe` escape-hatches). Users of pre-0.1.0 circe
    consumers need the migration notes at
    `site/docs/circe.md` + `migration-from-monocle.md`.
  - **Known issues** — list the circe walk-and-rebuild duplication
    (review §F1; ∼1200 LoC removable) as a 0.1.1 refactor target so
    downstream readers know the shape may move.
  - **Composition coverage disposition** — link to the `site/docs/optics.md`
    "composition coverage" table (Unit 21 output) so downstream users
    can see the explicit N / M / U / deferred map.
- Create: `CONTRIBUTING.md` — how to run tests, run benchmarks, preview
  the site locally, submit a PR, what the CI gates check. Name the
  manual recovery path for a partial Central Portal publish (per
  Risk table).
- Modify: `CLAUDE.md` — cross-reference `CONTRIBUTING.md` for human
  contributors.
- Tag: `v0.1.0` (manual step by maintainer).

**Approach:**
- README: match the opener pattern of cats / monocle READMEs (concise,
  lots of links). Do not duplicate the site content — the README is a
  launchpad.
- CHANGELOG: the Keep-a-Changelog template is well-known; use `Added`,
  `Changed`, `Fixed`, `Removed` sections.
- CONTRIBUTING: short but complete — a new contributor should finish a
  PR without asking anyone how to run the tests.

**Execution note:** This is the last unit; do it calmly and triple-check
the artifacts on Maven Central after the tag push.

**Patterns to follow:** cats / monocle READMEs and CHANGELOGs.

**Test scenarios:**
- Happy path — local preview (`sbt docs/tlSitePreview`) renders the new
  README + the docs site.
- Integration — `git tag v0.1.0 && git push --tags` triggers
  `release.yml`; artifacts appear on
  `https://central.sonatype.com/artifact/<org>/cats-eo_3`.
- Edge case — CHANGELOG links (compare-URLs to previous releases) are
  absent on 0.1.0 (first release); placeholder added for 0.1.1.

**Verification:**
- `cats-eo_3`, `cats-eo-laws_3`, `cats-eo-generics_3` all resolve on
  `https://repo1.maven.org/maven2/…/0.1.0/`.
- README renders correctly on the GitHub repo home page with working
  badges.
- `CHANGELOG.md` has a populated `[0.1.0]` section.
- GitHub Pages site is live at the configured URL and shows the docs
  tree.

## System-Wide Impact

- **Interaction graph:**
  - `laws/` module structure changes → every downstream test spec that
    `import eo.laws._` is affected. Within this repo only
    `tests/src/test/scala/eo/OpticsLawsSpec.scala` and
    `EoSpecificLawsSpec.scala` are affected. Downstream projects (none
    yet — pre-0.1.0) would see new stable import paths after the split.
  - `build.sbt` module names change → anyone running
    `sbt coreProject/test` etc. must adjust. `CLAUDE.md` references the
    current names; update if renamed.
  - `docs` sub-project adds a new command namespace (`sbt docs/mdoc`,
    `sbt docs/tlSite`). CLAUDE.md day-to-day section should list it.
  - `sbt-typelevel-ci-release` adds `tlRelease*`, `tlBaseVersion`,
    `tlMimaPreviousVersions` keys — no interaction with the code, but
    re-running `cs setup` / metals index may blink until `.bloop/` is
    regenerated.
- **Error propagation:**
  - mdoc failures gate the `docs` task — silent doc rot becomes an
    explicit build failure. This is a win; confirm it does not cascade
    into `compile` of unrelated modules (it won't — `docs` is separate).
  - A law equation that fails on a fixture produces a discipline failure
    with property-based shrink output; this matches the current behavior
    and is fine.
  - MiMa reports remain non-fatal on 0.1.0 (`reportOnly`); on 0.1.1+,
    failures block the release.
- **State lifecycle risks:**
  - `target/` and `docs/target/` directories balloon. Update
    `.gitignore` accordingly.
  - GitHub secrets (`PGP_*`, `SONATYPE_*`) are sensitive; only the
    `release.yml` workflow accesses them, and we gate that workflow on
    `v*` tag pushes only.
  - The Central Portal staging flow (close + release) is not
    transactional — if the workflow errors mid-way, the release may be
    partially published. Document the manual recovery path in
    `CONTRIBUTING.md`.
- **API surface parity:**
  - Scaladoc `@group` tags are additive — no API change, only doc
    organization. No runtime effect.
  - Law class splits change `eo.laws.OpticLaws.IsoLaws[...]` to
    `eo.laws.IsoLaws[...]`. We will ship a type alias / re-export in
    the old location for one release to avoid breaking any hypothetical
    downstream consumer, or `@deprecated` it.
- **Integration coverage:**
  - New `checkAll` blocks in `tests/` cover the full law matrix at
    integration level. The Unit 6 integration scenario (migrated
    `Unthreaded.scala`) covers the PowerSeries-through-Composer chain
    end-to-end.
  - Site + mdoc add a new kind of integration: docs compile against
    the library on every CI run, ensuring documentation parity.
- **Unchanged invariants:**
  - Existing optic semantics (`get`, `modify`, `replace`, `andThen`,
    `morph`, `transfer`, etc.) do not change. The laws reorganization
    and documentation uplift must not touch runtime behavior.
  - Existing JMH results for `Lens / Prism / Iso / Traversal` should
    remain at their current levels; Unit 12 extracts the fixture but
    should not regress.
  - Cats dependency stays at `cats-core 2.13.0`. No bumps in this plan.

## Risks & Dependencies

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Sonatype Central Portal onboarding is slow (namespace verification can take days). | Medium | High (delays 0.1.0 tag) | Start Unit 13 registration early; do not gate other units on it. The CI release workflow is ready before the namespace is live. |
| `sbt-typelevel-ci-release` version pinning conflicts with current sbt 1.12.9 or Scala 3.8.3. | Low | Medium (build breaks) | Pin to the exact latest compatible release (0.8.5 target). If incompatibility is found, fall back to `sbt-ci-release` (core) + hand-rolled mima — documented as a deferred fallback. |
| Laika / mdoc stop compiling against Scala 3.8.3 during the plan window. | Low | Medium (docs blocked) | Pin mdoc 2.9.x explicitly; if pin breaks, bump to current and accept whatever churn the bump requires — document in CHANGELOG. |
| Coverage below 85% is unreachable for `AssociativeFunctor` / existential-heavy files. | Medium | Low (target adjusts) | D10 decision: coverage is reported, not enforced at 0.1.0. Document per-file exceptions in the baseline doc. |
| Vect / PowerSeries laws reveal a genuine bug in existing behavior. | Low-Medium | Medium (scope grows) | Treat the bug as first-class: fix in a dedicated commit under this plan, note in CHANGELOG as a pre-0.1.0 fix. If the fix is large, defer to a 0.1.1. |
| Scalafmt bump (3.0.7 → 3.11.0) produces very large formatting diff. | High (certain) | Low (review cost) | Do the reformat in its own commit, before any other scalafmt-touching work. Keep that commit purely mechanical. |
| MiMa false positives on existential types / phantom-typed Vect on 0.1.1+. | Medium | Medium (release friction) | Document `mimaBinaryIssueFilters` usage in CONTRIBUTING.md; each intentional break gets a filter entry with a comment explaining why. |
| GitHub Pages deploy fails after first site publish. | Medium | Low (site down, not library broken) | Release workflow publishes library first, site second. Site failures do not block library publish. Document manual re-run. |
| Downstream (future external users) depending on `OpticLaws.IsoLaws` via the old object find the name gone after Unit 1. | Low (pre-0.1.0, no known users) | Low | Keep `@deprecated` re-exports for one release (0.1.0 → 0.2.0); remove in 0.2.0. |
| Package-private helpers in current bundled laws files do not survive the file split (`not accessible from sibling package` errors). | Medium | Low (mechanical fix) | Widen visibility to package-level (`private[laws]`) or extract shared helpers into `laws/src/main/scala/eo/laws/LawsHelpers.scala`. Flagged explicitly in Unit 1's Approach. |
| Central Portal namespace choice collides with legacy OSSRH registration or fails DNS / GitHub verification. | Medium | High (blocks 0.1.0 tag) | Unit 13's Approach walks the namespace-selection + verification steps in order. Pick a coordinate known to be unused; verify via `dig TXT` / GitHub UI before generating GPG keys. Budget 1–2 days of real-time wait for Central to confirm. |
| Versions transitively pinned by `sbt-typelevel-site 0.8.5` (mdoc, Laika, scalafmt) are too old for Scala 3.8.3. | Medium | Medium (docs or fmt break) | Unit 10 explicitly overrides `mdocVersion`; Unit 1/D9 bumps scalafmt independently. If Laika fails, document the mismatch in `docs/solutions/` and fall back to the older Scala 3.x for docs compilation as a last resort. |

## Documentation / Operational Notes

- **Release cadence:** Plan for 0.1.x (patch releases) monthly-or-on-demand
  through 2026, `0.2.0` only when an intentional binary break is required,
  `1.0.0` only when the API is judged stable by a separate 1.0-readiness
  review (out of scope here).
- **Contributor onboarding:** CONTRIBUTING.md is the single source. Keep
  CLAUDE.md in sync for agent contributors.
- **Issue templates:** GitHub issue templates (bug / feature / question)
  are a nice-to-have but not in this plan; add in the 0.1.x line.
- **Security policy:** `SECURITY.md` with disclosure email — add if the
  user intends this as a foundational library; otherwise skip for 0.1.0.
  (Raise at Unit 14 for user decision.)
- **Code of Conduct:** Not required pre-1.0 but standard for Typelevel
  projects. Skip for 0.1.0; add if/when we ship a 1.0.
- **Badges:** README badges need to wait for live artifacts (Central,
  GH Actions, Codecov) — add in Unit 15 after first green CI run.

## Alternative Approaches Considered

- **Docusaurus (Monocle's current docs toolchain) instead of Laika.**
  Rejected because it requires a Node.js toolchain in the dev loop, which
  adds friction for contributors and is incongruent with the
  Typelevel-adjacent positioning cats-eo aims for.
- **sbt-microsites instead of Laika.** Rejected — abandoned; no longer
  maintained.
- **Hand-rolled `sbt-sonatype` + `sbt-pgp` publishing.** Rejected because
  OSSRH sunset (2025-06-30) means the old path no longer works anyway;
  all roads lead to the Central Portal, and `sbt-typelevel-ci-release`
  covers it with less code.
- **Run the laws restructure last, not first.** Rejected because every
  subsequent unit adds new law files; doing the split at the end would
  require touching those files twice.
- **Add Scala.js / Scala Native cross-build in 0.1.0.** Rejected because
  (a) it's a multi-week effort on its own (cross-cutting source
  directories, Jsdom test runner, etc.), (b) the first goal is
  JVM-usable publishing, (c) adding cross-build is itself binary-compat
  neutral and can ship as a later 0.x minor.
- **Skip benchmarks expansion.** Rejected; user explicitly chose the
  full bundle. Benchmarks also provide hard evidence for marketing
  claims ("as fast as or faster than Monocle for …") in the README.
- **Defer PowerSeries laws to 0.2.0.** Rejected; user chose full laws
  now. Also: PowerSeries without law coverage at 0.1.0 would leave the
  most visually-new piece of code the most fragile, which is the
  opposite of what "ready for production" means.

## Success Metrics

- **Adoption-readiness:** a newcomer who has never touched the repo can
  follow `README → Getting Started → first Lens` end-to-end in under
  30 minutes without asking for help.
- **Quality floor:** `core` statement coverage ≥85%, every public type
  in `core/` is discipline-law-checked, no public method in `core/`
  lacks a Scaladoc docstring.
- **Benchmarks parity:** for every optic where Monocle has an equivalent
  operation, the cats-eo number is at most 2× Monocle's at shallow
  depth and at most 1.2× Monocle's at depth 6 (fused hot paths should
  pay off at depth). If we're faster, note that in the README.
- **Release hygiene:** 0.1.0 is tagged, artifacts on Maven Central, site
  on GH Pages, CHANGELOG populated, CI green on `main`, within one
  calendar month of Unit 1 merging (plan velocity signal).

## Phased Delivery

> **2026-04-24 update:** Phases 1 and 3 and most of Phases 2 and 4 have
> landed. The remaining work reorganises into three pre-tag waves (G, H, I)
> that absorb the new Units 16-21 plus the still-open Units 7-partial, 8,
> 9, and 15. Historical phases and their completion status are kept below
> for audit traceability.

**Phase 1 — Laws structure + coverage (Units 1–8).** Lands the
restructure, fills the law gaps, lifts coverage. No user-visible
behavior change. *Status: Units 1-6 landed; Unit 7 partial (ForgetfulFunctor
and ForgetfulTraverse laws on main; AssociativeFunctor + Composer laws
pending); Unit 8 pending.*

**Phase 2 — Documentation (Units 9–11).** Scaladoc uplift, site
scaffold, site content. *Status: Units 10-11 landed; Unit 9 (Scaladoc
uplift with `-groups`) pending.*

**Phase 3 — Benchmarks (Unit 12).** *Status: landed; Grate + PowerSeries
+ PowerSeriesPrismBench extensions are on main.*

**Phase 4 — Release (Units 13–15).** *Status: Units 13-14 landed;
Unit 15 pending.*

**Phase 5 — Composition + quality + diagrams + cookbook (Units 16-21;
new in 2026-04-24 revision).** Six units responding to the three review
inputs:
- Unit 16 — behaviour closures for R11 top-3 composition gaps +
  structural-disposition docs (R12).
- Unit 17 — code-quality must-fix items (dead code, Scaladoc gap-fill,
  Grate sentinels, circe coverage wiring).
- Unit 18 — three Mermaid diagrams (D1 / D3 / D5) on the site.
- Unit 19 — cookbook consolidation to ~18-22 recipes with attribution.
- Unit 20 — Kaleidoscope (plan 006) conditional cross-scope tracking.
- Unit 21 — resolve the 12 ? composition cells (OQ-R1).

Suggested merge order for the remaining work (optimizing for parallelism
and reviewable PR size — dependencies permitting):

- **Wave G (parallel-safe, unblocked now):**
  - Unit 7-remainder (AssociativeFunctor + Composer laws; depends on
    Unit 1 which is landed).
  - Unit 9 (Scaladoc uplift; depends on stable surface — 0.1.0-SNAPSHOT
    API is de facto stable at this point).
  - Unit 17 (code-quality must-fix; landable independently in small
    per-concern PRs).
  - Unit 16 (composition-gap closures; parallel with Unit 17).
- **Wave H (after G; mostly parallel):**
  - Unit 8 (coverage gap-fill to ≥85%; depends on Unit 16's fixtures
    absorbing much of the core/optics regression).
  - Unit 18 (Mermaid diagrams; depends on Unit 11 which is landed).
  - Unit 19 (cookbook consolidation; depends on Unit 18 so diagrams
    can be cross-referenced from recipe prose).
  - Unit 21 (? cell resolution; depends on Unit 7-remainder for any
    cells that flip to N via new givens).
- **Wave I (Kaleidoscope go/no-go):**
  - Unit 20 decision — inspect plan 006's state; ship Kaleidoscope in
    0.1.0 if Units 5-7 are realistic within a week, otherwise defer
    cleanly to 0.1.1. Go/no-go documented in this plan and in
    CHANGELOG.
- **Wave J (release cut):**
  - Unit 15 (README + CHANGELOG + CONTRIBUTING + tag v0.1.0).

If executed with maximum parallelism the remaining work is 4 waves
(G → H → I → J); sequential is ~8 merges. Historical wave breakdown
(pre-revision) for audit:

- **Wave A (foundation, parallel-safe):** Unit 1, Unit 13.
- **Wave B (after A, mostly parallel):** Unit 2, Units 3 / 4 / 7, Unit 5.
- **Wave C:** Unit 6, Unit 12.
- **Wave D:** Unit 8.
- **Wave E (sequential):** Unit 9 → Unit 10 → Unit 11.
- **Wave F:** Unit 14, Unit 15.

## Documentation Plan

Beyond the docs content Unit 11 originally produced, the revision adds
three streams of documentation work (Units 18, 19, 16-docs-parts) that
together form the "site-wide refresh for 0.1.0 tag" deliverable.

### Priority Mermaid diagrams (R15a, Unit 18)

| Diagram | Slot | Effort | Data source |
|---------|------|-------:|-------------|
| **D1** — Composition lattice (carriers = nodes, `Composer` givens = edges) | `site/docs/concepts.md`, after "Cross-family composition" | S | `given Composer[_, _]` set + composition-gap §0.3 ledger |
| **D3** — Optic family taxonomy tree (root = `Optic[S, T, A, B, F]`, leaves = families grouped by carrier) | `site/docs/optics.md`, top of page as at-a-glance index | S | Carrier column of `concepts.md` table + `optic-families-survey.md` |
| **D5** — JsonPrism Ior failure-flow decision graph (input → `Ior.Right` / `Both` / `Left` with per-`JsonFailure` branches) | `site/docs/circe.md`, adjacent to "Reading diagnostics" | M | `JsonPrism.scala` walker + `JsonFailure` ADT; verify against `JsonPrismSpec` |

Stretch diagrams (D4 sequence walk, D9 cross-carrier ladder, D7
simplified PowerSeries chunking) ship if capacity allows — none are
blocking for tag.

### Cookbook recipe footprint (R15b, R15c, Unit 19)

Target: ~18-22 recipes in `site/docs/cookbook.md`, drawn from the
30-recipe research catalogue in
[`2026-04-23-external-sources-cookbook-ideas.md`](../research/2026-04-23-external-sources-cookbook-ideas.md).

| Theme | Recipes retained | Source attribution |
|-------|-----------------:|--------------------|
| **A. Product editing (Lens chains)** | 4 (recipes 1-4) | Gonzalez *Control.Lens.Tutorial*; Penner *Virtual fields*; cats-eo novel (3, 4) |
| **B. Sum-branch (Prism)** | 2-3 (5 merged with 6, separate 7) | Baeldung / RockTheJVM; Wlaschin DDD; Penner ch. 13 |
| **C. Collection walks (Traversal)** | 3-4 (8+9 merged, 10, 11; 12 cross-link) | Penner *Optics By Example* ch. 7-8, 10; Gonzalez |
| **D. JSON / tree editing** | 3-4 (13+14+15 merged; 16, 17 separate) | cats-eo novel (13-15); Penner *jq post*; RefTree |
| **E. Algebraic / classifier** | 1-2 (11+18 merged; 19 conditional on plan 006) | Penner *Algebraic lenses*; Román et al. NWPT 2019 |
| **F. Write / read escape** | 3 (20, 21, 22) | Monocle Focus docs; Penner ch. 10, 13 |
| **G. Composition laddering** | 1-2 (23 load-bearing; 24 stretch) | Chapuis hands-on; Borjas lunar phase; Penner ch. 14 |
| **H. Effectful read (`modifyA`)** | 2 (25, 26; 27 deferred cross-ref) | Penner *batch-load* 100× story |
| **I. Observable failure (Ior)** | 3 (28, 29, 30) | cats-eo novel; cats `Ior` + circe `DecodingFailure.history` |
| **Total** | **~20** | Penner's *Optics By Example* is the single heaviest source |

Each recipe gets an inline `Source:` line with author + title +
chapter (for books) + URL. Penner's recipes cite the chapter directly
so readers can chase the original framing. The cats-eo-unique recipes
(3, 4, 10, 13-15, 23, 26, 28-30) lead the cookbook's first scroll.

### Site-wide refresh incorporating review inputs

The following site pages get updates beyond the new diagrams + cookbook
recipes. None are "greenfield" — they extend already-published pages:

- **`site/docs/concepts.md`** — embed D1 (composition lattice);
  cross-link D9 (if shipped) as a worked example.
- **`site/docs/optics.md`** — embed D3 (family taxonomy); append
  terminal-carrier paragraphs for SetterF, FixedTraversal[N],
  AlgLens[F] outbound, and Grate structural incompatibility (Unit 16
  + Unit 21 output). Add a "composition coverage" section from
  Unit 21's final N/M/U/deferred table.
- **`site/docs/circe.md`** — embed D5 (Ior failure-flow); cross-link
  recipes 13-15 and 28-30 from the cookbook.
- **`site/docs/cookbook.md`** — absorb the Unit 19 consolidation.
- **`site/docs/extensibility.md`** — cross-link to D4 / D7 if shipped.

### Housekeeping docs (unchanged targets)

- `CLAUDE.md` — day-to-day sbt commands get a new row for `docs/mdoc`
  and `docs/tlSite`; release commands point at the ci-release flow.
  *Already current as of plan-006 landing.*
- `CONTRIBUTING.md` — new, covers everything a first-time contributor
  needs. Adds the manual recovery path for a partial Central Portal
  publish. *Created in Unit 15.*
- `CHANGELOG.md` — new, seeded at 0.1.0 with the explicit "Changed"
  subsection for the circe v0.2 observable-failure migration. *Created
  in Unit 15.*
- `docs/solutions/2026-04-17-coverage-baseline.md` — the measurement
  artifact from Unit 2; updated in Unit 8 and again in Unit 17 (to
  include circe's newly-wired numbers).
- `docs/solutions/` — populated as incidents occur; not pre-written
  here.

### Release-notes architecture callout

Per R14 finding #5 and code-quality review §F5, the 0.1.0 CHANGELOG
states explicitly that `cats-eo-laws` depends transitively on `cats-eo`
(every law file imports `eo.optics.Optic.*`). This is intentional
architecture, not a bug — users of `cats-eo-laws` in downstream
discipline-checked test suites get the optics package automatically.
A possible 0.2.0 split into `cats-eo-core` + `cats-eo-optics` so
law-equation-only consumers can skip optics is tracked as future
work — flagged in Future Considerations, not in-scope here.

## Operational / Rollout Notes

- **Monitoring:** no runtime monitoring relevant (pure library). CI
  monitoring: GitHub Actions email on failed `main` or tag build.
- **Rollout:** tag-based. `v0.1.0` triggers publish; no staged /
  percentage rollout possible for a published artifact — if something is
  wrong, yank via `v0.1.1` with the fix. Cannot delete from Central.
- **Versioning policy:** 0.1.x = binary-compat within the line,
  enforced by MiMa from 0.1.1 onward. 0.2.0 and up allow breaking
  changes.
- **Communication:** announce 0.1.0 on the repo's release page,
  optionally on `r/scala` and the Typelevel Discord.

## Future Considerations

- **Scala.js / Scala Native cross-build** — 0.2.0 timeframe.
- **Optic families already tracked in their own plans** (no longer
  listed here as bare bullets — see the plan directory):
  - [plan 004](./2026-04-23-004-feat-grate-optic-family-plan.md) —
    `Grate` *(complete; merged, ships in 0.1.0)*.
  - [plan 006](./2026-04-23-006-feat-kaleidoscope-optic-family-plan.md)
    — Kaleidoscope *(Units 1-4 landed on main; Units 5-7 pending;
    conditional 0.1.0 scope per Unit 20)*.
  - [plan 007](./2026-04-23-007-feat-indexed-optics-hierarchy-plan.md)
    — Indexed optics (IxLens / IxTraversal / IxFold / IxSetter).
    **Deferred to 0.2.x** per 2026-04-24 scope call — the parallel
    hierarchy is structurally disruptive and too large to ship inside
    0.1.0's pre-tag window.
- **Other optic families surveyed but not yet planned** — see
  [`docs/research/2026-04-19-optic-families-survey.md`](../research/2026-04-19-optic-families-survey.md).
  Candidates after the plan-006/007 line ships: Achromatic Lens,
  Witherable / filter-during-parse traversal (recipe 27 in the
  cookbook research), `filtered` / `selected` predicate-gated
  traversal (recipe 16 in the cookbook research).
- **Auto-derivation for Iso / Optional / Traversal** in `generics/` — a
  natural extension of the current `lens` / `prism` macros.
- **Mutation testing with EO-aware mutators for sbt-stryker4s** — the
  CLAUDE.md note already documents this; a good community contribution
  project for post-1.0.
- **Nightly benchmarks workflow** with numbers posted to a GitHub
  discussion — better as a 1.0 concern when the API is stable.
- **`ValidCarrier[F[_, _], X]` witness.** `Affine.assoc` currently
  admits any `X` / `Y` — a deliberate relaxation taken at the
  Unit 12 bench work to unblock `Lens.andThen(Optional)` (see the
  docstring on `Affine.assoc`). The principled fix is to thread
  a `ValidCarrier[Affine, X]` (requires `X <: Tuple`) through
  every optic op, with identity witnesses for carriers that admit
  any existential. Deferred: touches the whole `Optic` extension
  surface; best landed alongside the Getter/Setter composition
  gaps so the witness threading pays for all three at once.
- **Circe `walkAndUpdate[F]` extraction.** Review §F1 identifies the
  single highest-ROI structural refactor in the codebase: ~1200 LoC
  removable across the four `Json*.scala` files via a shared
  `walkAndUpdate[F]` helper that generalises the path-walk /
  rebuild loop over a failure-treatment `F`. Structurally disruptive,
  wants a benchmark re-run. **Target: 0.1.1 or 0.2.0** — documented
  as known shape in the 0.1.0 CHANGELOG so downstream readers don't
  build long-term on the current layout.
- **`cats-eo-laws` → `cats-eo-core` + `cats-eo-optics` split.** Review
  §F5 observes that `cats-eo-laws` transitively pulls `cats-eo`
  because every law file imports `eo.optics.Optic.*`. A three-way
  split would let law-equation-only users skip the optics package.
  Structurally disruptive; 0.2.0 candidate.
- **`LowPriorityForgetInstances.assocForgetComonad` — leave alone.**
  Review §core/YAGNI flagged this ~25-LoC path as having no shipping
  consumer, suggesting deprecation. **Pinned 2026-04-24 against the
  review** — the path's Scaladoc documents it as a deliberate design
  escape hatch for downstream projects wiring an `AssociativeFunctor`
  through a `Comonad`-flavoured Forget. Keeping intact. Revisit only
  if user feedback shows nobody reaches for it across 0.1.x.
- **`pickSingletonOrThrow` collapse** in `AlgLens.scala:282-300` into
  direct `Foldable.reduceLeftToOption(_)(identity).get` — saves 15
  LoC. Post-0.1.0.
- **`JsonTraversal.place` → `replaceAll` rename** per review
  §circe/Naming to match broadcast semantics. API-breaking; 0.2.0.
- **Codescene MCP pass** — the code-quality review substituted the
  simplicity-reviewer lens because no Codescene MCP was available.
  When the tooling lands, run a one-shot Codescene pass and absorb
  change-coupling / churn × hotspot / god-class metrics into a
  follow-up review doc. See OQ-R4 + the review's
  §"Codescene comparison shopping list" for the specific metrics to
  request.

## Sources & References

- **2026-04-24 revision inputs** (three research docs absorbed into the
  Unit 16-21 additions + Requirements R11-R15 + Open Questions OQ-R1..4):
  - [`docs/research/2026-04-23-composition-gap-analysis.md`](../research/2026-04-23-composition-gap-analysis.md)
    — 15×15 composition-family matrix (696 lines, 94 N / 86 M / 35 U /
    12 ? cells). Source for R11 (top-3 closures), R12 (structural
    dispositions), OQ-R1 (12 ? cells).
  - [`docs/research/2026-04-23-code-quality-review.md`](../research/2026-04-23-code-quality-review.md)
    — simplicity-lens / YAGNI review substituting for an absent
    Codescene MCP (923 lines). Source for R13 (coverage regression
    repair), R14 (dead-code + Scaladoc + Grate-sentinel + circe-
    coverage must-fix), OQ-R3 (circe `walkAndUpdate` extraction
    deferral), OQ-R4 (future Codescene pass). **Finding #1 of the
    review (orphan `Reflector.scala`) is explicitly dismissed as a
    timing artifact** — Kaleidoscope plan 006 Units 1-4 landed after
    the review ran; Reflector is the live substrate for that family.
  - [`docs/research/2026-04-23-external-sources-cookbook-ideas.md`](../research/2026-04-23-external-sources-cookbook-ideas.md)
    — 30-recipe cookbook catalogue + 9-diagram candidate list
    (1141 lines). Source for R15a (three priority Mermaid diagrams),
    R15b (18-22 recipe target), R15c (attribution discipline,
    Penner et al.).
- **Cross-referenced plans:**
  - [plan 006 — Kaleidoscope optic family](./2026-04-23-006-feat-kaleidoscope-optic-family-plan.md)
    — 7 units, 1-4 landed on main (`9a09f65`, `7dd1bb7`, `dd5a685`,
    `c79dd7a`). Units 5-7 remaining are tracked by Unit 20 of this plan as
    conditional 0.1.0 scope.
  - [plan 007 — Indexed optics hierarchy](./2026-04-23-007-feat-indexed-optics-hierarchy-plan.md)
    — **deferred to 0.2.x per 2026-04-24 scope call**; out-of-scope
    for 0.1.0.
  - [plan 004 — Grate optic family](./2026-04-23-004-feat-grate-optic-family-plan.md)
    — complete; ships in 0.1.0.
  - [plan 005 — circe multi-field + observable failure](./2026-04-23-005-feat-circe-multi-field-plus-observable-failure-plan.md)
    — complete; v0.2 breaking changes to be called out in Unit 15
    CHANGELOG.
  - [plan 003 — generics multi-field Lens](./2026-04-23-003-feat-generics-multi-field-lens-plan.md)
    — complete.
  - [plan 002 — Iris classifier example](./2026-04-22-002-feat-iris-classifier-example.md)
    — cross-referenced from cookbook recipe 12.
- Research agents invoked during planning:
  - `compound-engineering:research:best-practices-researcher` — current
    Typelevel / Monocle conventions for docs, laws, publishing, CI,
    benchmarks, Scaladoc (2026).
- Local code references:
  - `laws/src/main/scala/eo/laws/OpticLaws.scala` — current laws shape
    to split.
  - `laws/src/main/scala/eo/laws/EoSpecificLaws.scala` — EO-specific
    laws to reorganize.
  - `tests/src/test/scala/eo/OpticsLawsSpec.scala`,
    `EoSpecificLawsSpec.scala`, `OpticsBehaviorSpec.scala` — wiring
    pattern for new `checkAll` blocks.
  - `tests/src/test/scala/eo/Unthreaded.scala`,
    `JsonOptic.scala`, `Samples.scala` — content to migrate into
    `docs/cookbook/`.
  - `benchmarks/src/main/scala/eo/bench/OpticsBench.scala` — fixture
    pattern to extend for Unit 12.
  - `project/plugins.sbt` — current plugin set (sbt-scoverage 2.4.4,
    sbt-jmh 0.4.7).
  - `.scalafmt.conf` — version pin to bump.
  - `CLAUDE.md` — agent-facing conventions that must stay coherent.
- External references:
  - [typelevel/cats `build.sbt`](https://github.com/typelevel/cats/blob/main/build.sbt).
  - [typelevel/cats `docs/directory.conf`](https://github.com/typelevel/cats/blob/main/docs/directory.conf).
  - [typelevel/cats `mima.sbt`](https://github.com/typelevel/cats/blob/main/mima.sbt).
  - [typelevel/cats `.github/workflows/ci.yml`](https://github.com/typelevel/cats/blob/main/.github/workflows/ci.yml).
  - [typelevel/cats `MonadTests.scala`](https://github.com/typelevel/cats/blob/main/laws/src/main/scala/cats/laws/discipline/MonadTests.scala).
  - [optics-dev/Monocle `build.sbt`](https://github.com/optics-dev/Monocle/blob/master/build.sbt).
  - [optics-dev/Monocle `LensTests.scala`](https://github.com/optics-dev/Monocle/blob/master/law/src/main/scala/monocle/law/discipline/LensTests.scala).
  - [optics-dev/Monocle `bench/`](https://github.com/optics-dev/Monocle/tree/master/bench/src/main/scala/monocle/bench).
  - [sbt-typelevel docs](https://typelevel.org/sbt-typelevel/) ·
    [FAQ](https://typelevel.org/sbt-typelevel/faq.html).
  - [sbt/sbt-ci-release](https://github.com/sbt/sbt-ci-release).
  - [scala/scala3 #23108 — OSSRH EOL](https://github.com/scala/scala3/issues/23108).
  - [Binary Compatibility for Library Authors](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html).
  - [Scala 3 scaladoc docstrings](https://docs.scala-lang.org/scala3/guides/scaladoc/docstrings.html).
  - [Scaladoc for Library Authors](https://docs.scala-lang.org/overviews/scaladoc/for-library-authors.html).
