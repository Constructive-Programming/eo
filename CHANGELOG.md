# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **`Plated` — recursive self-traversal + recursion combinators.** A new
  `optics.Plated[S]` (the cats-eo analogue of Haskell `lens`'s `Plated`) whose
  `plate` is a `Traversal[S, S]` over the immediate same-typed children of a
  recursive ADT, plus stack-safe combinators `transform` (bottom-up rewrite),
  `rewrite` (Option-rule fixpoint), `children`, and `universe`. All trampoline
  through `cats.Eval` (or an explicit worklist), so deep trees don't overflow.
  Build one with `Plated.fromChildren`, derive it with `generics.plate[S]`
  (focuses every exact-`S`-typed field across all cases; enums, sealed
  hierarchies, recursive case classes), or call the combinators directly on any
  self-traversal optic via the `.transformAll` / `.universeOf` extensions. New
  `Traversal.selfChildren` constructor builds the underlying `MultiFocus[PSVec]`
  self-traversal from an explicit children view. The read path (`children` /
  `universe`) goes through `Plated.childrenList`, which `fromChildren` (and every
  derived plate) overrides to read children directly — skipping the optic's
  `List → Array → PSVec → List` round-trip, which puts `universe` within ~parity
  of a hand-written recursive visitor on deep and JSON trees (see the
  [benchmarks](https://github.com/Constructive-Programming/eo/blob/main/site/docs/benchmarks.md)).
- **Universal `Plated` instances for the JSON and Avro carriers.**
  `dev.constructive.eo.circe.platedJson` makes `io.circe.Json` a recursive
  self-traversal (children = an array's elements / an object's field values), so
  `transform` / `rewrite` / `universe` walk a whole document — redact every
  field at any depth, rewrite every string, rename keys throughout — no decode.
  `dev.constructive.eo.avro.platedAvro` is the Avro mirror over `IndexedRecord`
  (children = directly-record-valued fields; records nested inside array / map /
  union fields are leftover skeleton in this version).

### Changed

- **Identity carrier renamed `Forgetful` → `Direct`.** The carrier behind `Iso`
  and `Getter` (`type Direct[X, A] = A`) now reads as what it does — direct
  function application, no leftover — rather than naming the category-theory
  mechanism. The `Forgetful*` typeclasses (`ForgetfulFunctor`, `ForgetfulFold`,
  `ForgetfulTraverse`, `ForgetfulApplicative`) keep their names, and the
  scaladoc still notes `Direct` *is* the forgetful functor. Mechanical rename
  for any external code: `Optic[…, Forgetful]` → `Optic[…, Direct]`,
  `import …data.Forgetful.given` → `…data.Direct.given`. Pre-1.0 / no published
  baseline (`tlMimaPreviousVersions` is empty), so no MiMa break.

### Added

- **`getOption` for `Either`-carrier optics.** Previously `getOption` was
  defined only for the `Affine` carrier (`Optional` / `AffineFold`). A `Prism`
  surfaced as the bare `Optic[S, T, A, B, Either]` — e.g. a derived
  `generics.prism`, whose static type is the base `Optic`, not the concrete
  `Prism` subclass — had no `getOption`. An `Either`-carrier overload now fills
  that gap (`@targetName`-disambiguated; the concrete `Prism`'s own member still
  wins at its static type), so a read on a derived prism reads through the same
  `getOption` call as everything else.

### Changed

- **Internal carrier unification — `AlgLens[F]` + `Kaleidoscope` collapse
  into `MultiFocus[F]`.** The two pre-0.1.0 carriers had identical
  `(X, F[A])` value shapes and only differed in their encoding (parameter
  vs path-dependent F; cats-`Functor` vs project-local `Reflector`).
  `MultiFocus[F][X, A] = (X, F[A])` is now the single home for
  classifier-shaped optics + aggregation universals. The
  `Reflector[F]` typeclass is deleted. The K3 `collectViaReflect` law is
  replaced by a carrier-wide `collectViaMap` (Functor-broadcast); the
  `Reflector[List]` cartesian-singleton story survives at the call site as
  the `MultiFocus[List].collectList` extension. Pre-0.1.0 — no published
  artifact to break, but the rename is mechanical for any external code
  on the spike branch (`AlgLens[F]` → `MultiFocus[F]`, `Kaleidoscope` →
  `MultiFocus[F]`, `kal.collect[F, B](agg)` → `mf.collectMap[B](agg)` or
  `mf.collectList(agg)`).

## [0.1.0] - 2026-04-25

### Added

- First public release.
- Full discipline-checked law coverage for every public optic (Iso,
  Lens, Prism, Optional, AffineFold, Getter, Setter, Fold, Traversal,
  Grate, AlgLens[F], Kaleidoscope) and every shipped carrier (Affine,
  SetterF, Vect / PSVec, PowerSeries, FixedTraversal[N], AlgLens[F],
  Grate, Kaleidoscope).
- mdoc-verified docs site with optic-family taxonomy, composition
  lattice, and Ior failure-flow Mermaid diagrams.
- Benchmarks vs. Monocle for Lens / Prism / Iso / Traversal / Fold /
  Getter / Optional / Setter; EO-only benches for PowerSeries / Grate /
  AlgLens / Kaleidoscope.
- circe integration — `JsonPrism` / `JsonTraversal` / `JsonFieldsPrism`
  / `JsonFieldsTraversal` with **observable-by-default** failure
  surface (`Ior` — see plan 005 in `docs/plans/`).
- Avro integration (`cats-eo-avro`) — `AvroPrism` / `AvroTraversal` /
  `AvroFieldsPrism` / `AvroFieldsTraversal` with the same Ior surface,
  plus `.union[Branch]` for schema unions (Option, sealed traits,
  Scala 3 enums) and a triple-input shape
  (`IndexedRecord | Array[Byte] | String`) covering parsed records,
  binary wire payloads, and Avro JSON wire format. Codec backend is
  [kindlings-avro-derivation](https://github.com/MateuszKubuszok/kindlings)
  (auto-derived `AvroEncoder` / `AvroDecoder` / `AvroSchemaFor`,
  combined into one project-internal `AvroCodec[A]`). Hot-path walks
  beat the kindlings-codec round-trip baseline by 3-7&times; on the
  shipped JMH benches.
- `lens` / `prism` Scala 3 macros via [Hearth](https://github.com/MateuszKubuszok/hearth):
  multi-field NamedTuple focus and full-cover Iso upgrade.
- Sonatype Central Portal publishing via `sbt-typelevel-ci-release`.

### Changed

- Pre-`1.0`, the public surface is refined as we learn from real
  users. Each non-bugfix release will list breaking changes here.
  `0.1.0` is the first publish, so there is no prior surface to break.

### Fixed

- N/A for the first release.

### Known issues

- circe walk-and-rebuild duplication (~1200 LoC) — flagged in
  [`docs/research/2026-04-23-code-quality-review.md`](docs/research/2026-04-23-code-quality-review.md)
  §F1 as a `0.1.1` refactor target. The user-facing surface should not
  change; the cleanup is internal.

### Composition coverage

The full composition matrix scoreboard is **96 N / 90 M / 39 U / 0 ?**
across 225 cells (196 Optic&times;Optic + 28 standalone borders + 1
JsonTraversal&times;Review corner). See:

- The "Composition limits" subsection in
  [`site/docs/optics.md`](site/docs/optics.md) — manual-idiom (M) and
  unsupported-by-design (U) pairs are documented inline next to each
  optic family.
- The resolution scoreboard in
  [`docs/research/2026-04-23-composition-gap-analysis.md`](docs/research/2026-04-23-composition-gap-analysis.md)
  &sect;7 (and the per-cell ledger in &sect;1.1 / &sect;3 / &sect;4).

[Unreleased]: https://github.com/Constructive-Programming/eo/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Constructive-Programming/eo/releases/tag/v0.1.0
