# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
