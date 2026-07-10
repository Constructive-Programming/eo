# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **`ConfluentWire.resolvingBytes` — the framed → framed drift-translating diagonal.**
  The Confluent surface had every corner except one: a `Array[Byte] => Either[AvroFailure,
  Array[Byte]]` that does per-message writer-schema resolution (like `recordReader` / `confluent`)
  AND *translates* writer→reader drift (like `resolvingRecord` — Avro's `ResolvingDecoder`, never
  the `resolve` fingerprint gate), handing back reader-layout framed bytes rather than a typed `A`
  or an `F[A]`. Per payload: strip the header, look the writer schema up by id, resolve-decode the
  body writer→reader, re-encode under `readerSchema` and re-frame under `frameId`. Because the
  output is reader-layout, it is stable across writer-schema evolution within a reader generation —
  the property the gating `resolve` (which refuses drift with `SchemaMismatch`) cannot give. A
  factory in the `confluent` mould (compute once at construction, cheap per call): the returned
  function closes over a `ConcurrentHashMap` keyed by writer id, so `schemaById` is consulted once
  per distinct writer id and every later payload under a seen id reuses the cached bridge. Failures
  are `Left` per the existing taxonomy — `NotConfluentFramed` (bad/`null` frame),
  `SchemaResolutionFailed` (the hook threw), `ResolveFailed` / `EncodeFailed`.
- **`eo.avro.circe.AvroJson` — structural Avro ↔ circe bridge, a lawful `Prism`.** A new
  sub-package of `cats-eo-avro` bridging Avro's generic value model and `io.circe.Json` with no
  typed case class in the middle. `AvroJson.record(schema): Prism[Json, IndexedRecord]` is the
  bidirectional entry point — and `AvroJson.codecPrism[A]: Prism[Json, A]` its typed counterpart
  (schema off the `AvroCodec`, no `IndexedRecord` at the call site; decode failure is the same
  prism miss): `reverseGet` is the total structural walk (record → object in
  schema-declaration order, map → object, list → array, `Utf8`/`CharSequence` → string, int/long →
  `fromLong`, double/float → `fromDoubleOrNull`/`fromFloatOrNull`, enum → string,
  `ByteBuffer`/`GenericFixed` → array of signed byte ints, resolved `null` branch → `Json.Null`);
  `getOption` is the strict schema-guided inverse (exact field cover — no extras, no defaults;
  `toInt`/`toLong` integrality and range; enum symbols and fixed lengths checked; unions
  first-branch-that-parses, `Json.Null` only ever matching a `null` branch), so the two prism
  round-trip laws hold (property-pinned). Also `avroToJson(record): Json` directly and the
  `bytesToJson(schema): Getter[Array[Byte], Json]` read optic (parse-to-record fused with the walk
  via `Getter.andThen`). Logical types and encoder-specific string-transforms are explicit
  non-goals (the bridge sees only the runtime value). circe rides on `cats-eo-avro` as an
  `Optional` dependency: it never reaches downstream classpaths transitively, and any caller of
  this sub-package already depends on circe directly — its API surface *names* `io.circe.Json`.

### Changed

- **`ConfluentWire.reader` / `recordReader` are strict on the frame.** A payload that does not
  parse as a Confluent frame now raises `AvroFailureException(NotConfluentFramed)` in `F` instead
  of silently falling back to a direct decode under the codec's schema — the fallback could
  accidentally succeed on corrupt bytes and yield garbage. Consumers of topics with mixed framed /
  unframed producers opt into their own fallback by catching that failure and decoding directly
  (`AvroCodec.decodeValue[A]` / `AvroCodec.decodeRecord`). Behavioural break for anyone relying on
  the auto-detect; correctness over convenience.

### Fixed

- **`ConfluentWire.strip(null)` is a defined failure, not an NPE.** A `null` payload (a Kafka
  tombstone or a mis-produced record) previously dereferenced `bytes.length` and threw
  `NullPointerException` out of the header strip; it now returns
  `Left(AvroFailure.NotConfluentFramed(...))` like any other malformed frame, so a downstream
  consumer rejects it diagnosably. `strip`'s parameter widens to `Array[Byte] | Null` (source- and
  binary-compatible under the module's `-Yexplicit-nulls`).

## [0.7.0] - 2026-07-09

### Added

- **Capability traits at `dev.constructive.eo`** — `CanGet`, `CanGetOption`,
  `CanReverseGet`, `CanModifyP`/`CanModify`, `CanFold`, `CanPutP`/`CanPut`,
  `CanModifyFP`/`CanModifyF`, `CanModifyAP`/`CanModifyA`, `CanPlace`,
  `CanTransform`: the carrier-free consuming surface of an optic, usable as
  `using` evidence (`def adjustTimes[T](using cm: CanModify[T, Instant])`).
  Concrete optic classes implement their capabilities directly (hot path);
  a derived given per companion serves optics bound at the generic
  `Optic[…, F]` type. `CanFold.foci` is the carrier-free counterpart of the
  `all` extension. See the new [Capabilities](https://eo.constructive.dev/capabilities.html)
  page and `CapsBench` (capability call ≈ direct call on CI).
- **Fused `SplitCombineLens.andThen(SplitCombineLens)`** — macro-derived lens
  chains (`lens[Person](_.address).andThen(lens[Address](_.street))`) now
  compose into a concrete class, keeping the fused read/write paths and the
  capability mixins instead of falling back to an anonymous `Optic`.

### Changed

- **Binary-breaking (recompile required):** stored-function `val`s whose names
  collide with capability kernels became private constructor params + methods:
  `Getter.get`, `GetReplaceLens.get`, `SplitCombineLens.get`,
  `BijectionIso.get`/`reverseGet`, `Review.reverseGet`;
  `GetReplaceLens.replace` went niladic → unary. Source-compatible via
  auto-eta (`xs.map(g.get)` still compiles), but jars compiled against
  ≤ 0.6.x fail at runtime against 0.7.0 core — the `inline` fused `andThen`s
  spliced the old accessors into caller bytecode. Recompile downstream.

### Added

- **`Getter`s now compose with `Getter`s via `andThen`.** `g1.andThen(g2)` reads
  `s => g2.get(g1.get(s))` and yields a `Getter`, matching how `Iso` / `Lens`
  compose through their fused subclasses. `Getter.apply` now returns a concrete
  `DirectGetter` carrying the fused `andThen` (previously a bare anonymous `Optic`
  with no `andThen`, forcing callers to hand-nest `get` calls). As part of this,
  a `Getter`'s back-focus slot is now `Unit` (`Optic[S, Unit, A, Unit, Direct]`,
  was `Optic[S, Unit, A, A, Direct]`) — making the read-only-ness explicit in the
  type and lining the inner `T` up with the outer `B` for composition. Pre-1.0,
  no published baseline, so no MiMa break; external code that ascribed the full
  `Optic[S, Unit, A, A, Direct]` type updates the final type argument to `Unit`.

- **`Plated` — recursive self-traversal + recursion combinators.** A new
  `optics.Plated[S]` (the cats-eo analogue of Haskell `lens`'s `Plated`) whose
  `plate` is a `Traversal[S, S]` over the immediate same-typed children of a
  recursive ADT, plus stack-safe combinators `transform` (bottom-up rewrite),
  `rewrite` (Option-rule fixpoint), `children`, and `universe`. The headline is
  `Plated.everywhere` — `transform` in *composable optic* form: a `Setter` whose
  `modify` is the recursive `transform`, so `everywhere.andThen(prism).andThen(lens).modify(f)`
  applies `f` at the focus *at every depth* (e.g. uppercase every variable in an
  expression tree). It reuses the existing `SetterF` carrier and `Morph`/`Composer`
  bridges — no new carrier — so it composes with any inner optic (Lens / Prism /
  Optional / …) as the outer of `.andThen`. All are stack-safe on deep trees —
  `transform` recurses on the call stack while shallow and falls back to a
  heap-stack machine past a depth bound, `universe` / `children` use a worklist,
  `rewrite` a `cats.Eval` trampoline.
  Build one with `Plated.fromChildren`, derive it with `generics.plate[S]`
  (focuses every exact-`S`-typed field across all cases; enums, sealed
  hierarchies, recursive case classes), or call the combinators directly on any
  self-traversal optic via the `.transformAll` / `.universeOf` extensions. New
  `Traversal.selfChildren` constructor builds the underlying `MultiFocus[PSVec]`
  self-traversal from an explicit children view. The carrier is PSVec-native end
  to end — `fromChildrenVec` / `generics.plate[S]` speak the `MultiFocus[PSVec]`
  focus vector directly, with a `List`-shaped `fromChildren` kept as a
  hand-writing convenience — so neither path pays a `List ↔ PSVec` round-trip.
  `universe` reads children straight off the carrier (no `List` per node) and
  `transform` is hybrid — a direct call-stack recursion while shallow, a
  heap-stack machine past a depth bound (via `childrenVec` / `rebuild`, no
  `to`/`from` tuple per node; leaves applied in place) instead of an `Eval`
  trampoline; both stay stack-safe (deep + 100k trees) while running
  within ~2–3× of a hand-written recursive visitor and ahead of Monocle's
  `Plated` (which is not stack-safe). See the
  [benchmarks](https://github.com/Constructive-Programming/eo/blob/main/site/docs/benchmarks.md).
- **Universal `Plated` instances for the JSON and Avro carriers.**
  `dev.constructive.eo.circe.platedJson` makes `io.circe.Json` a recursive
  self-traversal (children = an array's elements / an object's field values), so
  `transform` / `rewrite` / `universe` walk a whole document — redact every
  field at any depth, rewrite every string, rename keys throughout — no decode.
  `dev.constructive.eo.avro.platedAvro` is the Avro mirror over `IndexedRecord`
  (children = directly-record-valued fields; records nested inside array / map /
  union fields are leftover skeleton in this version).

### Changed

- **Optic composition is `inline` at the fused composers — deep chains no longer
  hit the JIT's recursive-inline cap.** The same-carrier fused `andThen` on
  `DirectGetter` / `GetReplaceLens` / `BijectionIso` / `SetterOptic` /
  `MendTearPrism` / `PickMendPrism` / `Optional` is now `inline`. A plain `def`
  compiled the composer's `s => inner.get(get(s))` lambda once, so a depth-N
  runtime chain reused that single bytecode and HotSpot's C2 treated the cascade
  as recursion — capping inlining at `MaxRecursiveInlineLevel` and leaving the deep
  tail as virtual `Function1.apply`. `inline` splices a *distinct* lambda per
  compose site (distinct synthetic methods per level), so the chain inlines fully
  with no JVM flag — the effect Monocle gets from a fresh anonymous class per
  compose. CI (`-f 3 -wi 3 -i 5`) at depth 6: Getter 27.2→11.8 ns, Setter 44→26 ns,
  Optional `modify` 139→94 ns — EO now **1.3–2.3× faster** than the equivalent
  Monocle composition (was 1.3–2.8× slower). Terminal mixed-carrier overloads
  (`Lens→Optional` etc.) fire once per chain and stay plain `def`. Behaviour
  identical; pre-1.0, no MiMa baseline.

- **circe JSON array-write traversal now matches hand-written AST surgery.**
  `JsonWalk.walkPath` / `rebuildPath` (the per-element backbone of `JsonTraversal`
  writes) converted the immutable `path: Array[PathStep]` to a `Vector` /
  `IndexedSeq` and ran an `Either`-monad `foldLeftM` / `zip` + `foldRight` on every
  call — i.e. once per array element. Rewritten as manual index loops
  (behaviour-identical). `OrderCirceBench` `lines[*].name` write (CI, `-f 3 -wi 3
  -i 5`) at 512 elements: 868→655 KB/op allocation (−25%, now 1.07× the hand-rolled
  `direct` `JsonObject` surgery, was 1.42×) and 277→249 µs (now on par with the
  `direct` / `hcursor` hand forms and faster than decode→Monocle→encode). Helps
  every JSON optic op, not just traversals.

- **`Setter` composition is now allocation-free per hop (fused `andThen`).**
  `Setter.apply` returns a concrete `SetterOptic` that stores its writer directly
  and overloads `andThen(SetterOptic)` to compose writers outright
  (`s1.andThen(s2).modify(f) == s1.modify(s2.modify(f))`), building the chain once
  at compose time — the same fused-subclass shape as `Lens` / `Iso` / `Getter`.
  Previously two `Setter`s composed only through the generic
  `AssociativeFunctor[SetterF]`, which allocated a fresh `SetterF` per hop at
  *modify* time. `SetterBench` (CI, `-f 3 -wi 3 -i 5`) allocation goes from ~2.8×
  Monocle at depth to parity: depth-3 368→168 B/op, depth-6 800→288 B/op (==
  Monocle). The fused composer is also `inline` (see the composition entry above),
  which removes the dispatch overhead too — net, Setter composition is now *faster*
  than Monocle at depth (depth-6 26 vs 60 ns). Cross-carrier composition
  (`lens.andThen(setter)` via
  `Morph[Tuple2, SetterF]`) still routes through `assocSetterF`. Pre-1.0, no MiMa
  baseline; `Setter.apply`'s return type widened from `Optic[…, SetterF]` to the
  subtype `SetterOptic[…]` (source-compatible).

- **Carrier typeclass methods take named parameters (were point-free / curried).**
  `Accessor.get`, `ReverseAccessor.reverseGet`, `ForgetfulFold.foldMap`, and
  `ForgetfulTraverse.traverse` were declared with zero value parameters, returning
  a curried `Function1` / `PolyFunction` chain (e.g. `foldMap[X,A,M]: (A => M) =>
  F[X,A] => M`). They now take their arguments directly (`foldMap[X,A,M](f, fa)`),
  matching the already-flat `ForgetfulFunctor.map` and simplifying the dispatch
  the JIT sees. Behaviour-identical and allocation-neutral (the intermediate
  closures were already eliminated by `inline` + escape analysis); source-breaking
  only for code that *implements* a custom carrier or calls these SPI methods
  directly — update the overrides/calls to the named-parameter arity. Pre-1.0, no
  MiMa baseline.

- **`Direct` is now an `opaque type`, not a transparent alias.** `Direct[X, A]`
  was `type Direct[X, A] = A`, which the compiler dealiased to `A` during implicit
  search — so `object Direct`'s givens (`Accessor[Direct]`, `AssociativeFunctor
  [Direct, …]`, …) were *not* in the implicit scope of a bare `A`, and same-carrier
  composition (`iso.andThen(iso)` via the generic path, `getter.andThen(getter)`)
  couldn't summon them. `opaque type Direct[X, A] = A` makes `Direct` a distinct
  type whose companion *is* in scope, so those givens resolve with no import and
  the generic `andThen` works for `Direct`-carrier optics. It still erases to `A`
  (zero runtime cost); `Direct.apply` (wrap) / `.value` (unwrap) are
  `transparent inline` identities that compile away entirely, and exist only so
  construction sites outside `object Direct` satisfy the type.
  `Forget[F]` is decoupled from `Direct` (now `[X, A] =>> F[A]` directly, the same
  type it always was) so the Fold/Forget machinery is unaffected. No behaviour
  change; pre-1.0, no MiMa baseline.

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
