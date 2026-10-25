---
title: "feat: cats-eo-spark — Spark operations as eo optic constructions"
type: feat
status: proposed
date: 2026-10-25
revised: 2026-10-25
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
| `ds.map(f)` with an eager optic | eager bridge `ds.mapThrough(o)(f)` (needs `Encoder`) | eager twin | Yes. Escape hatch: leaves Catalyst, and needs closure upload under Connect |
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
- **Tests** use an in-process Classic `SparkSession` (`spark-sql` `% Test`), which is fast and
  needs no server. There is also one **Connect smoke suite** (an in-process
  `SparkConnectService` from `spark-connect` `% Test`, client from `spark-connect-client-jvm`
  `% Test`) that re-runs the differential properties over gRPC. It proves that the same optics
  serialize. Phase 0 checks the exact module split (`Column`, `functions` and `AgnosticEncoders`
  in `sql/api`) against the pinned 4.x minor.

## Key Technical Decisions

1. **Spark 4.x only, consumed via `CrossVersion.for3Use2_13`.** Spark publishes only `_2.13`
   artifacts. Exclude `_2.13` copies of anything we also pull as `_3` (`scala-xml`,
   `scala-parser-combinators`, `scala-collection-compat`, json4s' transitive deps) and fail on
   mixed suffixes. Phase 0 must prove that Scala 3.9's Scala-3-compiled stdlib runs Spark's
   2.13 bytecode, and on which JDKs (Spark 4.x lists 17/21; check 25, the primary lane).
2. **Own `AgnosticEncoder` derivation** from `Mirror`s via Hearth (already the `generics`
   dependency): `ProductEncoder`, primitives, `OptionEncoder`, collections, enums/sealed traits.
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
7. **Packaging: in the root aggregate.** Main dependency `spark-sql-api` `% Provided`, so it
   compiles with everyone else at little cost. Spark tests need a forked JVM with Spark's
   `--add-opens` list (`Test / fork := true`) and one shared `SparkSession` per JVM. If Spark
   cannot run on JDK 25 at the pinned minor, the module still **compiles** in every lane
   (bytecode targets 17). The 25 lane then excludes `spark/test` the way the kyo module is gated
   by JVM (`kyoBuildActive`), rather than dropping the module from the aggregate. Usual
   `-opt-inline:dev.constructive.eo.**`. Explicit nulls stay on, with `.nn` at Java-API
   boundaries.

## Implementation Units

**Phase 0 — Spike (go/no-go, time-boxed).**
- `spark/` sbt project in the aggregate: `spark-sql-api` `% Provided`, plus
  `spark-sql` / `spark-connect` / `spark-connect-client-jvm` `% Test`, all `for3Use2_13`; a forked
  test JVM; a shared Classic session fixture.
- Prove: Scala 3.9 + Spark 2.13 runtime on 17/21/25; one `AgnosticEncoder` derived for a nested
  case class; one `withField` lens composed twice with `explain(true)` showing the collapsed plan;
  the same expression sent through an in-process Connect server; an `Affine` optic with a guard
  in the complement, composed twice.
- Output: `docs/research/2026-10-xx-eo-spark-spike.md` (classpath, JDK matrix, `sql/api` module
  split).

**Phase 1 — Pure-value optics.** `Plated[DataType]`, `StructType` field `Lens` / `At`, `Row`
kit, `VariantVal` kit. Laws via the existing `cats-eo-laws` rule sets. No session needed.

**Phase 2 — `Col[A]` + total optics.** The opaque `Col`; struct `Lens`; array/map `Modify`;
aggregate `Getter`s; `Dataset` row-struct lifting (`focus(o).get/modify/replace`).

**Phase 3 — Partial optics on `Affine`.** Guarded-complement `Optional` / `Prism` / `At` /
`Index` (nullable fields, map keys, array indices, `try_cast`, `from_json`, variant reads); static
misses from schema checks.

**Phase 4 — Encoders + selector macro + twins.** The `AgnosticEncoder` derivation;
`col[S](_.a.b.c)` emitting a Lens or a guarded `Affine` Optional per nullability; pairing with the
eager generics optics.

**Phase 5 — Consumers + eager bridge.** Capability-taking `joinOn` and grouping helpers;
`ds.mapThrough(eagerOptic)(f)` and the effectful `modifyA` path via `mapPartitions`, marked as
leaving Catalyst.

**Phase 6 — Verification + benchmarks.**
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

- **Classpath:** Scala 3 stdlib vs Spark's 2.13 expectations, suffix conflicts, JDK 25. Phase 0
  is the gate.
- **Encoder internals:** `AgnosticEncoders` is semi-internal, so pin the Spark minor and run CI
  over the supported 4.x minors.
- **Guard nesting:** deep `when(...)` chains from composed partial optics may defeat codegen on
  wide/deep schemas (the 64 KB method limit). Benchmark that, and simplify adjacent guards in the
  complement (`p₁ && p₂` when the rebuilds commute) if needed.
- **Test time:** one shared session, batched properties, and the Connect suite kept small.

## Resolved

1. Spark 4.x only.
2. Target the shared `spark-sql-api` (Classic + Connect from one artifact). Connect is covered
   by a smoke suite.
3. Own `AgnosticEncoder` derivation.
4. In the root aggregate.
