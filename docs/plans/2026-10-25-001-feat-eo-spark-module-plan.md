---
title: "feat: cats-eo-spark — Spark operations as eo optic constructions"
type: feat
status: active
date: 2026-10-25
revised: 2026-10-25
phase0: docs/research/2026-10-25-eo-spark-phase0-spike.md
---

# cats-eo-spark — Spark operations as eo optic constructions

## Overview

A new published module, `cats-eo-spark` (`spark/`), that turns the Spark operations that *are*
optics into eo optic **constructions**, so they compose with `andThen` and run through the
same `.get` / `.modify` / `.replace` / `.getOption` / `.foldMap` surface as the rest of the
library. The operations that are not optics (joins, shuffles, I/O, actions) become **consumers**
that take a capability.

The main idea: **a Spark `Column` is a plain Scala value, an expression.** A lens whose `S` and
`A` are *typed columns* (`Col[S]`, `Col[A]`) is an ordinary eo `Lens`. Its `get` builds
`GetStructField` and its `replace` builds `withField`. Catalyst optimizes and code-generates
the composed expression, and no row is ever deserialized into JVM objects.

**The module adds no carriers.** Every optic uses a core carrier (`Tuple2`, `Affine`,
`ModifyF`, `Forget`). The Spark-specific information lives in the **complement `X`**, the way
`JsoniterPrism` keeps `(bytes, span)` in `X` over core `Affine`. Core's `andThen`, `Morph` /
`Composer` bridges and capabilities (`CanGet`, `CanGetOption`, `CanModify`, …) work unchanged
on `Col` values.

The same selector also yields an **eager twin**: an ordinary eo optic on the Scala value. The
test strategy is differential, in the `demoGrpcFilter` style: the symbolic interpretation
evaluated by Spark must equal the eager interpretation evaluated in Scala, on every generated
row. The eager twin is the one checked against `cats-eo-laws`.

## Problem Frame

Spark's nested-data story is stringly typed and asymmetric:

- Reads are easy (`col("a.b.c")`). Writes are not: rewriting a deeply nested field needs nested
  `withField` calls, or a full `struct(...)` rebuild per level, or a `ds.map` that drops out of
  Catalyst. That last option costs deserialization and whole-stage codegen, and under Spark
  Connect it also means shipping closures to the server.
- Arrays and maps need higher-order functions (`transform`, `transform_values`, `filter`,
  `aggregate`), which do not compose with struct access in any first-class way.
- Spark 4's `VARIANT` type (`parse_json`, `variant_get`, `try_variant_get`) is a JSON-like tree,
  the same problem eo already solves for circe, jsoniter, zio-json and kyo-schema.

## Phase 0 outcome (2026-10-25)

GO. Spike write-up: [`docs/research/2026-10-25-eo-spark-phase0-spike.md`](../research/2026-10-25-eo-spark-phase0-spike.md).
What changed in this plan as a result:

- **Runtime is Spark Connect only.** Since Scala 3.8 the stdlib is compiled by Scala 3, and
  Spark's Scala 2 runtime reflection (`ScalaReflection`, the session-companion lookup) cannot
  initialise on it. Spark Classic therefore cannot run in a Scala 3.9 JVM. The Connect client can,
  and the engine runs server-side on the 2.13 stdlib. The `spark-sql-api` target stands; "Classic
  in-process" is gone (upstream-blocked).
- **Encoders:** case classes encode as `TransformingEncoder[T, Row]` over a `RowEncoder`, through
  a codec the Hearth macro `SparkEncoders.derived` generates (field reads, a constructor call).
  Spark's `ProductEncoder` decode path initialises `ScalaReflection`.
- **Tests:** client-only test classpath, and a Connect server child JVM from the classpath-only
  Scala 2.13 project `sparkConnectServer`.
- Confirmed: no new carrier. The guarded `Optional` and plan-time misses are core `Optional`, and
  Catalyst removes the row-struct wrapper and folds the per-row guard into `withField`'s own NULL
  check.

## Requirements Trace

- R1. Typed column `Col[A]`: an opaque type over `org.apache.spark.sql.Column`, with a phantom
  type for the Scala-side shape.
- R2. Symbolic optics over `Col` for every Spark operation that is lawfully an optic (see the
  catalogue below), all on **core carriers**.
- R3. Selector-macro derivation `col[Person](_.address.street)`, which yields the symbolic optic
  **and** its eager twin from one selector.
- R4. Pure-value optics on Spark's own data structures: `Plated[DataType]`, `StructType` field
  lenses / `At`, `Row` optics, `VariantVal` prisms.
- R5. `Dataset` / `DataFrame` lifting: `ds.focus(o).get / modify / replace / foldMap`, with the
  non-optic operations as capability-taking consumers.
- R6. Laws: the eager twins run through `cats-eo-laws` rule sets. Symbolic optics are checked
  **differentially** against their twins. Plan-shape tests check that composition optimizes
  away.
- R7. Own `AgnosticEncoder` derivation (Scala 3 macro). Spark's `TypeTag`-based `Encoders` are
  unavailable on Scala 3.
- R8. Benchmarks: symbolic optic vs `ds.map` + eager optic vs hand-written `withField` chains.
- R9. mdoc docs page, CLAUDE.md module row, and one cookbook recipe.

## Partiality in the complement (no new carrier)

A symbolic `to(c: Col[S])` runs at **plan-build time**, before any row exists. A partial optic
therefore has two kinds of miss, and core `Affine` already has room for both:

- **Static miss, at plan time** → `Affine.Miss(c)`. Examples: the schema has no such field, a
  path does not type-check against the `StructType`, or a variant/JSON path fails to parse.
  `getOption` returns `None` and `modify` passes the column through unchanged.
- **Per-row miss, at run time** → **in the complement.** `to` returns
  `Affine.Hit(x, focus)` with
  `X = (source: Col[S], present: Col[Boolean], rebuild: Col[B] => Col[T])`. `focus` is the
  nullable focus expression. `from` builds
  `when(present, rebuild(b)).otherwise(source)`, so rows where the focus is absent come out as
  they went in. That is exactly the Optional laws, read per row.

Composition is core's `AssociativeFunctor[Affine]`. The composed complement nests the two
guards, so `outer.andThen(inner).modify(f)` builds
`when(p₁, rebuild₁(when(p₂, rebuild₂(f(a)), a₁)))`. **`getOption` returns `Option[Col[A]]`.**
`None` is a static miss; `Some(col)` is a column that is NULL on the rows that miss. Turning
that into a Scala `Option[A]` per row happens only at collect time. The eager twin's `None`
corresponds to the symbolic column's NULL, which is what the differential tests check.

The same trick covers the **NULL-struct problem**. `withField` on a NULL struct returns NULL, so
a plain `Lens` breaks put-get there. The macro emits a `Lens` (`Tuple2` carrier) only for
non-nullable schema positions. For nullable positions (`Option[_]` / nullable encoder fields) it
emits the `Affine` optic above with `present = source.isNotNull`. Users never get an unlawful
lens by accident.

**Arrays and maps.** A symbolic container cannot hand out its elements at plan time, so
`MultiFocus[PSVec]` (which needs the actual foci) does not apply. Element-wise **writes** use
core `Modify` (`ModifyF`): `modify(f)` = `transform(arr, f)` / `transform_values(m, f)`.
Composition with element-level lenses goes through the existing `Modify.andThen` / Lens→Modify
bridges. Element-wise **reads** are `Getter`s into an aggregate. A constructor takes the
combining function as Column expressions and compiles to
`aggregate(arr, zero, (acc, x) => combine(acc, f(x)))`. Several lawful monoids exist per type
(`sum`, `max`, `array_union`, `concat`, …), so they are constructor arguments and never givens,
following the core rule.

## Catalogue: Spark operation → eo construction

| Spark operation | eo construction | Carrier / complement | Lawful? |
|---|---|---|---|
| `getField` / `withField` (struct, non-null) | `Lens[Col[S], Col[A]]` | `Tuple2`, `X = Col[S]` | Yes |
| nullable struct / column | `Optional` | `Affine`, guard in `X` | Yes, per row |
| `dropFields` + `withField` (create/delete) | `At`-shaped optic | `Affine` | Up to field order, like kyo `atField` |
| `element_at` / `map_concat` / `map_filter` (map key) | `At` / `Index` | `Affine`, `present = map_contains_key` | Yes |
| array index | `Index` | `Affine`, `present = size > i` | Yes |
| `transform` (array), `transform_values` (map) | `Modify` | `ModifyF` | Yes |
| `transform_keys` | `Modify` | `ModifyF` | Only for injective `f` (duplicate keys fail), so documented as unlawful in general |
| `aggregate` / `reduce` / `exists` / `forall` (array) | `Getter` into an aggregate | `Tuple2`/`Forget` | Getter laws |
| `filter` (array) | `filtered`-style `Modify` | `ModifyF` | Usual `filtered` caveat |
| `explode` / `posexplode` | `Fold` on `Dataset` (read side only) | — | Read-only: rebuilding needs a shuffle |
| `try_cast` / `cast` | `Prism[Col[String], Col[Int]]` etc. | `Affine`, `present = try_cast(..).isNotNull` | Only on canonical forms (`"007"` does not round-trip), stated as a precondition, as for `JsoniterPrism` |
| `from_json` / `to_json` | `Prism[Col[String], Col[S]]` | `Affine` | Same canonical-form caveat |
| `try_variant_get` / `variant_get` / `parse_json` | `AffineFold` / `Getter` into `VARIANT` | `Affine` | Read-only. No variant update function as of 4.0 (re-check per 4.x minor) |
| `select(exprs*)` / `withColumn` / `drop` | `Lens` on the row-struct (see Lifting) | `Tuple2` | Yes |
| `groupBy(...).agg(...)` / window functions | `Getter` / `Fold` → consumer | — | Read-only |
| `join`, `union`, `repartition`, `cache`, read / write, actions | **not optics**: capability-taking consumers / plain API | — | n/a |
| `ds.map(f)` with an eager optic | eager bridge `ds.mapThrough(o)(f)` (needs `Encoder`) | eager twin | Yes. Escape hatch: leaves Catalyst. Works since Phase 5 once the session has the Scala 3 runtime and the app's classes (`ScalaRuntime.addTo`); optics are `Serializable` |
| `DataType` / `StructType` trees | `Plated[DataType]`, field `Lens` / `At` | core | Yes (plain values) |
| `Row` (untyped) | `field(name)` `Optional`, `Plated[Row]` (the DynamicValues-kit pattern) | `Affine` | Yes |
| `VariantVal` (eager) | constructor prisms + `field` / `at` / `each` / `Plated` (circe playbook) | core | Yes |

`Traversal.modifyA` (effectful traversal) is **impossible symbolically**: an `Applicative`
effect cannot run inside a Catalyst expression. Effectful work goes through the eager bridge.

## Spark Connect and the API target

**What Spark Connect is.** Since 3.4, and fully featured in 4.0, Spark has a client/server
split. A *thin client* builds unresolved logical plans (`Column` / `DataFrame` expressions),
serializes them as protobuf, and sends them over gRPC to a Spark Connect server, which
analyzes, optimizes and runs them. The client has no Catalyst, no executors and no Hadoop on its
classpath, and it can be upgraded independently of the cluster. In Spark 4.0 the Scala
`Dataset` / `Column` / `functions` / `Encoder` / `Row` / `types` API was extracted into a shared
**`spark-sql-api`** module, with two implementations: *Classic* (in-process `spark-sql`) and
*Connect* (`spark-connect-client-jvm`).

**Does focusing on it help? Yes, but target the shared API rather than the Connect client.**

- Symbolic column optics are *exactly* what Connect can express: pure expressions, no closures.
  They are the main product here, and they run unchanged on Classic and Connect.
- **The main dependency is just `spark-sql-api` (`% Provided`).** It is small, with no
  spark-core, Hadoop or Janino. That is what makes it cheap to keep the module in the root
  aggregate (answer 4), and the user picks Classic or Connect at runtime. Depending on
  `spark-connect-client-jvm` itself would *exclude* Classic users and gain nothing.
- `AgnosticEncoder`s live in the shared layer too: they are what Connect uses to encode on the
  client. So our own encoder derivation (answer 3) serves both.
- The eager bridge (`ds.map` + eager optic) is the one part that is worse under Connect, because
  closures must be uploaded as artifacts. It stays a documented escape hatch.
- **Tests** (revised by Phase 0) run the Connect client in the test JVM against a Connect server
  child JVM (Classic engine, `local[1]`) launched from the classpath-only Scala 2.13 project
  `sparkConnectServer`. An in-process Classic session is impossible on Scala 3.8+'s stdlib (see
  Phase 0 outcome). The module split is confirmed for 4.2.0: `Column`, `functions`, `Dataset`,
  `Encoder` and `AgnosticEncoders` are all in `spark-sql-api`.

## Key Technical Decisions

1. **Spark 4.x only, consumed via `CrossVersion.for3Use2_13`.** Spark publishes only `_2.13`
   artifacts. Exclude `_2.13` copies of anything we also pull as `_3` (`scala-xml`,
   `scala-parser-combinators`, `scala-collection-compat`, json4s' transitive deps) and fail on
   mixed suffixes. *Phase 0:* the Connect client runs on Scala 3.9's stdlib on JDK 17/21/25.
   Spark Classic does not (Scala 2 runtime reflection), so the runtime is Connect.
2. **Own `AgnosticEncoder` derivation** from `Mirror`s (Hearth where macros help): primitives,
   `OptionEncoder`, collections, enums/sealed traits. Case classes are
   `TransformingEncoder[T, Row]` over a `RowEncoder`, never `ProductEncoder` (Phase 0).
   Needed for `Dataset[T]`, the eager bridge, and for the macro to know which schema positions
   are nullable (decision 3 above).
3. **Partiality in the complement over core `Affine`**, as described above. No new carriers.
   Arrays and maps use `Modify` + aggregate `Getter`s.
4. **Lifting a `Dataset` = optics on the row-struct.** Treat a row as
   `Col[S] = struct(ds.columns.map(col)*)`, apply any symbolic optic, then `select("r.*")`.
   Catalyst's `SimplifyExtractValueOps` / `CollapseProject` cancel
   `GetStructField(CreateNamedStruct(...))`. **Plan-shape tests** assert that no residual
   `named_struct` survives for top-level edits.
5. **Twin optics from one selector.** `col[S](_.a.b)` returns
   `SparkOptic { symbolic: …Col…; eager: …S… }`. The eager half reuses the `generics` module's
   `lens` / `prism` derivation, and field names come from the same encoder naming, so both
   halves address the same field by construction.
6. **Consume via capability.** A pipeline stage takes
   `(using CanModify[Col[S], Col[String]])`. Non-optic Dataset ops are consumers taking the
   capability for the key they read
   (`joinOn(using CanGet[Col[L], Col[K]], CanGet[Col[R], Col[K]])`).
7. **Packaging: in the root aggregate** (done in Phase 0). Main dependency `spark-sql-api`
   `% Provided`. Tests fork a JVM with Spark's `--add-opens` list (plus
   `--sun-misc-unsafe-memory-access=allow` on JDK ≥ 23), share one Connect session, and start
   one server child JVM per run (about 25 s). All three CI JDK lanes run the suite. Usual
   `-opt-inline:dev.constructive.eo.**`. Explicit nulls stay on.

## Implementation Units

**Phase 0 — Spike. DONE (GO), see the Phase 0 outcome above.**
- `spark/` in the aggregate: `spark-sql-api` `% Provided`, `spark-connect-client-jvm` `% Test`,
  a Connect server child JVM from `sparkConnectServer` (Scala 2.13).
- Shown: struct lens (composed), guarded `Optional` (also composed twice), plan-time miss,
  `Mirror`-derived encoders, the collapsed optimized plan, and the JDK matrix.

**Phase 1 — Pure-value optics.** *1a DONE:* `Schemas` (`DataType` tree — `struct`/`array`/`map`
prisms, `field`/`nullable`/`element`/`mapKey`/`mapValue` navigation, `Plated[DataType]`; not named
`DataTypes`, which is Spark's own Java factory) and `Rows` (`field[A]`/`at[A]` by `ClassTag`,
`Plated[Row]`), checked with Optional / Seam / Navigation / Prism / Plated rule sets. *1b DONE:*
`Variants` over `VariantVal` — scalar prisms, `field` / `at` / `each`, `Plated`, `obj` / `arr` /
`nullValue` constructors. Every value handed out or written is re-encoded through `VariantBuilder`
in a canonical form (keys added in sorted order, depth-first), so `VariantVal`'s byte equality is
structural equality. A Connect spec round-trips a `parse_json` value: we edit it and Spark's
`variant_get` reads the edit back. Original scope: `Plated[DataType]`, `StructType` field `Lens` / `At`, `Row`
kit, `VariantVal` kit. Laws via the existing `cats-eo-laws` rule sets. No session needed.

**Phase 2 — `Col[A]` + total optics.** *DONE:* `ColumnOptics.each` / `values` (`Modify` via
`transform` / `transform_values`), `aggregate` / `size` / `exists` / `forall` (`Getter`s), and
`DatasetOptics` (`rowCol`, `editRows`, plus `modifyAll` / `replaceAll` / `getAll` / `foldAll`
consumed through `CanModify` / `CanGet`). Encoders gained `List` / `Vector` / `Seq` / `Map`. Spark
decodes Scala collections by resolving companions through `currentMirror` (the same dead
reflection), so these are `TransformingEncoder`s over the reflection-free `immutable.ArraySeq` /
`java.util.Map` paths. Differential and plan-shape specs: a composed element-wise edit compiles
to one projection with one `transform`. Original scope: The opaque `Col`; struct `Lens`; array/map `Modify`;
aggregate `Getter`s; `Dataset` row-struct lifting (`focus(o).get/modify/replace`).

**Phase 3 — Partial optics on `Affine`.** *DONE:* `ColumnOptics.key` / `atKey` (map entries: in-place `transform_values`, At-shaped
create / update / delete), `index` (array position), `cast` (`try_cast`), `json` (`from_json` /
`to_json`), `parseJson` (`try_parse_json`) and the read-only `variantGet` (`try_variant_get`; Spark
has no variant update). All are guarded core `Optional`s / `Getter`s, with no new carrier.

The differential spec caught one real hazard. In its default PERMISSIVE mode `from_json` decodes
malformed or non-object input to a struct of NULL fields rather than NULL, so a naive presence
test would rewrite `"not json"` as `{}`. `json` therefore decodes with a reserved corrupt-record
column, counts a row as present only when that column is NULL, and takes a `StructType` only.
Original scope: Guarded-complement `Optional` / `Prism` / `At` /
`Index` (nullable fields, map keys, array indices, `try_cast`, `from_json`, variant reads); static
misses from schema checks.

**Phase 4 — Encoders + selector macro + twins.** *DONE:* `ColumnOptics.path[S](_.a.b)` (macro, `ColumnPath`) gives the symbolic optic and its eager
twin from one selector, with `.get` on an `Option` marking a nullable step. With no `.get` both halves
are Lenses (`Total`); otherwise both are Optionals (`Partial`), and the symbolic presence test ANDs
`isNotNull` over every `Option` prefix. Symbolic writes are one dotted `withField`. Eager writes rebuild
each level through its `Mirror` (no reflection), so the eager half needs no generics dependency.
Encoders gained `Byte` / `Short` / `Float` / `Array[Byte]` / `BigInt` / `BigDecimal` / `LocalDate` /
`Instant` / `LocalDateTime`. Sum types have no Spark struct encoding and are not derived. Original
scope: The `AgnosticEncoder` derivation;
`col[S](_.a.b.c)` emitting a Lens or a guarded `Affine` Optional per nullability; pairing with the
eager generics optics.

**Phase 5 — Consumers + eager bridge.** *DONE:* `DatasetOptics.filterHits` / `joinOn` (inner equi-join, keys read through `CanGet`) /
`groupAggregate` / `explodeAll`, and the eager bridge `mapThrough` (typed `Dataset.map` through an eager
optic).

**This revises Phase 0's "deferred":** Scala 3 closures DO run server-side once the session has the Scala 3
runtime, cats, cats-eo and the application's classes as artifacts (`ScalaRuntime.addTo`; class directories
via the Connect client's `REPLClassDirMonitor` in development). Two requirements fell out:
- **Optics must be serializable.** The one change to `core`: `trait Optic … extends Serializable`.
- **Bridge lambdas must be defined where the server can load their implementing class**, e.g. a top-level
  object, not a test class.

Tuples now encode like case classes, because Spark's tuple `ProductEncoder` needs the dead reflection.
`rowCol` binds columns through `ds.col`, so join conditions stay unambiguous. Original scope: Capability-taking `joinOn` and grouping helpers;
`ds.mapThrough(eagerOptic)(f)` and the effectful `modifyA` path via `mapPartitions`, marked as
leaving Catalyst.

**Phase 6 — Verification + benchmarks.** *DONE:* the verification pieces landed with each phase (laws on eager twins, differential specs,
plan-shape tests). Phase 6 adds `sparkBench` (`spark-bench/`, JMH, outside the aggregate; CI compiles
it). One nested edit (upper-case `address.street`) over a cached 200 000-row server-side Dataset,
forced by a checksum aggregate, with a local Connect server on JDK 25 (`-wi 3 -i 5 -f 3`):

| Benchmark | ms/op |
|---|---:|
| `handWritten` (`withField` by hand) | 107.0 ± 5.4 |
| `symbolic` (`ColumnOptics.path`) | 106.4 ± 12.2 |
| `eagerMap` (`mapThrough`, eager twin) | 187.6 ± 10.9 |
| `handMap` (`Dataset.map` with a hand-written `copy`) | 188.7 ± 11.3 |
| `identityMap` (`Dataset.map(identity)`) | 174.2 ± 11.7 |

The symbolic optic is within noise of the hand-written projection, as expected: the plan-shape specs
already show the same optimized plan. The eager bridge is about 1.75× slower, and all of that is the
typed `Dataset.map` itself. It matches a hand-written `map`, and `map(identity)` already costs 1.6×.
Every row is decoded to JVM objects (`UTF8String` → `String`, a generic row per struct level),
handed to the closure, and re-encoded, outside whole-stage codegen.

The first run measured the bridge at 5× (289 ms/op). A server-side JFR profile found that half of
that was our own encoder. The generated codec built each row with `Row.fromSeq(List(...))`, which
copies the list through an `ArrayBuilder` with a `ClassTag` lookup on every row. Building
`new GenericRow(Array[Any](…))` instead halved the bridge. A smaller cost was the eager path's
per-level `Array.tabulate` + `Tuple.fromArray` before the `Mirror`; it now passes a one-field
`Replaced` view. To profile the server, pass
`-Deo.spark.connect.jvmOptions="<sparkJvmOptions> -XX:StartFlightRecording=filename=…"` to
`sparkBench / Jmh / run / javaOptions`. Mutation testing (`mutationAll`) still skips the module: every mutant run would
start a Connect server. That is a follow-up. Original scope:
- Laws on every eager twin (`LensTests`, `OptionalTests`, `PrismTests`, `FoldTests`).
- The differential property, per optic and per composed chain, batched as one Dataset per
  property:
  `spark.createDataset(xs).select(o.symbolic.modify(f)(row)).collect == xs.map(o.eager.modify(f))`.
  Generators are derived, with NULLs injected at nullable positions. The same properties also
  run through the Connect smoke suite.
- Plan-shape tests (`queryExecution.optimizedPlan` matchers).
- JMH (`spark-bench/`, outside `benchmarks/` like the demo benches): symbolic vs `ds.map` vs
  hand-written `withField`.

**Phase 7 — Docs.** Site page (catalogue + lawfulness caveats + "Classic or Connect"), a
cookbook recipe ("rewrite a field 5 levels deep in a Parquet table"), CLAUDE.md module row,
MiMa baseline.

## Risks

- **Upstream:** Spark Classic on Scala 3.8+ is blocked by eager `scala-reflect` initialisation.
  If users need Classic, the fix is upstream (a lazy `ScalaReflection.universe`) or a Scala 3
  Spark build. Typed UDFs / the eager bridge likely hit the same wall server-side.
- **Fat-client classpath:** the Connect client shades the whole `spark-sql-api`. Users must not
  also put the unshaded `spark-sql-api` + Arrow next to it. Document this on the docs page.
- **Encoder internals:** `AgnosticEncoders` is semi-internal, so pin the Spark minor and run CI
  over the supported 4.x minors.
- **Guard nesting:** deep `when(...)` chains from composed partial optics may defeat codegen on
  wide/deep schemas (the 64 KB method limit). Benchmark that, and simplify adjacent guards in the
  complement (`p₁ && p₂` when the rebuilds commute) if needed.
- **Test time:** one shared session, batched properties, and the Connect suite kept small.

## Resolved

1. Spark 4.x only.
2. Target the shared `spark-sql-api`. After Phase 0, Connect is the only runtime that works on
   Scala 3.9 and the whole suite runs on it.
3. Own `AgnosticEncoder` derivation.
4. In the root aggregate.
