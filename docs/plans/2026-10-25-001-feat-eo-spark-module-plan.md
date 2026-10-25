---
title: "feat: cats-eo-spark — Spark operations as eo optic constructions"
type: feat
status: proposed
date: 2026-10-25
---

# cats-eo-spark — Spark operations as eo optic constructions

## Overview

A new published module, `cats-eo-spark` (`spark/`), that turns the Spark operations that *are*
optics into eo optic **constructions**, so they compose with `andThen` and run through the
same `.get` / `.modify` / `.replace` / `.foldMap` surface as the rest of the library. The
operations that are not optics (joins, shuffles, I/O, actions) become **consumers** that take a
capability.

The main idea: **a Spark `Column` is a plain Scala value, an expression.** A lens whose `S` and
`A` are *typed columns* (`Col[S]`, `Col[A]`) is an ordinary eo `Lens`. Its `get` builds
`GetStructField` and its `replace` builds `withField`. Catalyst optimizes and code-generates
the composed expression, and no row is ever deserialized into JVM objects. Core's
`GetReplaceLens`, `Modify`, `Getter` and the capability traits (`CanGet`, `CanModify`, …)
therefore work **unchanged** on `Col` values. The module adds the typed-column wrapper, the
constructors, two symbolic carriers for the cases where hit/miss is only known per row, and
lifting onto `Dataset`.

The same selector also yields an **eager twin**: an ordinary eo optic on the Scala value. The
test strategy is differential, in the `demoGrpcFilter` style: the symbolic interpretation
evaluated by Spark must equal the eager interpretation evaluated in Scala, on every generated
row. The eager twin is the one checked against `cats-eo-laws`.

## Problem Frame

Spark's nested-data story is stringly typed and asymmetric:

- Reads are easy (`col("a.b.c")`). Writes are not: rewriting a deeply nested field needs nested
  `withField` calls, or a full `struct(...)` rebuild per level, or a `ds.map` that drops out of
  Catalyst. That last option costs deserialization and whole-stage codegen.
- Arrays and maps need higher-order functions (`transform`, `transform_values`, `filter`,
  `aggregate`), which do not compose with struct access in any first-class way.
- Spark 4's `VARIANT` type (`parse_json`, `variant_get`, `try_variant_get`) is a JSON-like tree,
  the same problem eo already solves for circe, jsoniter, zio-json and kyo-schema.

eo's whole point is "pinpointed read/write on nested data, composed". Spark is the largest
nested-data runtime on the JVM.

## Requirements Trace

- R1. Typed column `Col[A]`: an opaque type over `org.apache.spark.sql.Column`, with a phantom
  type for the Scala-side shape.
- R2. Symbolic optics over `Col` for every Spark operation that is lawfully an optic (see the
  catalogue below), built from core constructors wherever a core carrier fits.
- R3. Selector-macro derivation `col[Person](_.address.street)`, which yields the symbolic optic
  **and** its eager twin from one selector.
- R4. Pure-value optics on Spark's own data structures, which need no `SparkSession`:
  `Plated[DataType]`, `StructType` field lenses / `At`, `Row` optics, `VariantVal` prisms.
- R5. `Dataset` / `DataFrame` lifting: `ds.focus(o).get / modify / replace / foldMap`, with the
  non-optic operations as capability-taking consumers.
- R6. Laws: the eager twins run through `cats-eo-laws` rule sets. Symbolic optics are checked
  **differentially** against their twins on a local `SparkSession`. Plan-shape tests check
  that composition optimizes away.
- R7. Benchmarks: symbolic optic vs `ds.map` + eager optic vs hand-written `withField` chains.
- R8. mdoc docs page, CLAUDE.md module row, and one cookbook recipe.

## Catalogue: Spark operation → eo construction

The plan does **not** promise that every Spark operation becomes an optic. Several are not
optics, and the module states that openly.

| Spark operation | eo construction | Carrier | Lawful? |
|---|---|---|---|
| `getField` / `withField` (struct) | `Lens[Col[S], Col[A]]` | core `Tuple2` (GetReplaceLens) | Yes, for non-null structs. Null structs: see the `ColMiss` decision |
| `dropFields` + `withField` (create/delete) | `At`-shaped optic | core `Affine` | Up to field order, like kyo `atField` |
| nullable struct / column | symbolic `Optional` | **new `ColMiss`** | Yes, with miss = SQL NULL |
| `element_at` / `map_concat` / `map_filter` (map key) | `At` / `Index` | `ColMiss` | Yes |
| `transform` (array), `transform_values` (map) | `Modify` (Setter) | core `ModifyF` | Yes |
| `transform_keys` | `Modify` | `ModifyF` | Only for injective `f` (duplicate keys fail), so documented as unlawful in general |
| `aggregate` / `reduce` (array) | `Fold` via a **Column monoid** | **new `ColFold`** | Fold laws |
| `filter` (array) | `Fold` / `Traversal.filtered`-style `Modify` | `ModifyF` | Usual `filtered` caveat |
| `explode` / `posexplode` | `Fold` on `Dataset` (read side only) | — | Read-only: rebuilding needs a shuffle (`groupBy` + `collect_list`) |
| `try_cast` / `cast` | `Prism[Col[String], Col[Int]]` etc. | `ColMiss` | Only on canonical forms (`"007"` does not round-trip), stated as a law precondition |
| `from_json` / `to_json` | `Prism[Col[String], Col[S]]` | `ColMiss` | Same canonical-form caveat as circe `text` |
| `parse_json` / `try_variant_get` / `variant_get` | `AffineFold` / `Getter` into `VARIANT` | `ColMiss` | Read-only. Spark 4.0 has no variant *update* function (verify against the targeted 4.x) |
| `select(exprs*)` / `withColumn` / `drop` | `Lens` on the row-struct (see Lifting) | `Tuple2` | Yes |
| `groupBy(...).agg(...)` / window functions | `Getter` / `Fold` → consumer | — | Read-only |
| `join`, `union`, `repartition`, `cache`, read / write, actions | **not optics**: capability-taking consumers / plain API | — | n/a |
| `ds.map(f)` with an eager optic | eager bridge `ds.mapThrough(o)(f)` (needs `Encoder`) | eager twin | Yes, but leaves Catalyst, so it is an escape hatch |
| `DataType` / `StructType` trees | `Plated[DataType]`, field `Lens` / `At` on `StructType` | core | Yes (plain Scala values) |
| `Row` (untyped) | `field(name)` `Optional`, `Plated[Row]`, the DynamicValues-kit pattern | core `Affine` | Yes |
| `VariantVal` (eager) | constructor prisms + `field` / `at` / `each` / `Plated` (circe playbook) | core | Yes |

Partial optics (`Optional`, `Prism`, `AffineFold`) **cannot use core's `Either` / `Affine`
carriers symbolically.** Those carriers decide hit/miss from the Scala value inside `to`, but a
symbolic `to(c: Col[S])` runs at plan-build time, before any row exists. Hit/miss is a per-row
fact. This is why the module needs its own carrier (see Key Technical Decisions §3).

`Traversal.modifyA` (effectful traversal) is **impossible symbolically**: an `Applicative`
effect cannot run inside a Catalyst expression. Symbolic array/map optics stop at
`Modify` + `Fold`. Effectful work goes through the eager bridge.

## Scope Boundaries

**In scope (v1):** Spark 4.x classic plus Spark Connect (they share the `Column` API from 4.0
on), R1–R8, Scala 3 only.

**Out of scope:** Spark 3.x; Structured Streaming specifics (the column optics apply as they
are, but there are no streaming-only constructs); `Plated[LogicalPlan]` / Catalyst `TreeNode`
optics (internal, unstable API, a possible later research spike); write-side variant updates;
MLlib.

## Key Technical Decisions

1. **Spark is Scala 2.13 only, so consume it via `CrossVersion.for3Use2_13`.** Spark 4.x
   publishes only `_2.13` artifacts, and there is no announced Scala 3 build. All Spark deps are
   `% Provided` (clusters supply Spark) and
   `.cross(CrossVersion.for3Use2_13)`. Exclude `_2.13` copies of anything we also pull as `_3`
   (`scala-xml`, `scala-parser-combinators`, `scala-collection-compat`, json4s'
   transitive deps), and fail the build on mixed suffixes (sbt `conflictWarning`). Phase 0 must
   prove that Scala 3.9's Scala-3-compiled stdlib runs Spark's 2.13 bytecode, and on which JDK
   (Spark 4.x officially supports 17/21; check 25, since the aggregate's primary lane is 25).

2. **Encoders: derive Spark 4 `AgnosticEncoder`s with a Scala 3 macro, not TypeTags.**
   `Encoders.product[T]` needs a Scala 2 `TypeTag`, which Scala 3 does not have. Spark 4's
   encoder layer is the `AgnosticEncoders` ADT (`ProductEncoder`, `PrimitiveIntEncoder`,
   `OptionEncoder`, …), shared by classic and Connect. Deriving it with Hearth (already the
   `generics` dependency) from `Mirror`s is about one file of code. The fallback is depending on
   [`vincenzobaz/spark-scala3`](https://github.com/vincenzobaz/spark-scala3) if it is current
   for 4.x. Phase 0 decides. Encoders are needed only for `Dataset[T]` and the eager bridge.
   Pure-`Col` work needs none.

3. **New carrier `ColMiss`: partiality as SQL NULL.** A symbolic partial optic is
   `Optic[Col[S], Col[T], Col[A], Col[B], ColMiss]`. `to` yields the leftover `Col[S]` plus a
   *nullable* focus `Col[A]` (NULL = miss). `from` rebuilds
   `when(focus.isNotNull, rebuild(s, b)).otherwise(s)`. Wiring it up takes `ForgetfulFunctor`
   (so `.modify`), `AssociativeFunctor[ColMiss]` (same-carrier `andThen`), and `Composer`s
   `Tuple2 → ColMiss` and `ModifyF`-side, which give Lens-then-Optional composition. All of
   this follows the extension points the circe / jsoniter carriers already use, with **no
   changes to `core`**. `getOption` returns `Col[A]` (nullable). Turning it into Scala
   `Option[A]` happens only at collect time.
   - *NULL-struct lens decision:* `withField` on a NULL struct returns NULL, so
     `replace(a)(null).get == null ≠ a` and put-get fails. Choice: plain `Lens` is documented as
     lawful on **non-nullable** schema positions. The macro emits a `ColMiss` Optional wherever
     the Spark schema field is nullable (`Option[_]` in Scala, or a nullable encoder field).
     Users never get an unlawful lens by accident.

4. **New carrier `ColFold`: folds via Column monoids.** `foldMap` needs `Monoid[M]`, and
   symbolically `M` is `Col[M]`. The module ships `ColMonoid[M]` instances (`sum`, `max`,
   `min`, `count`, `array_union`, `concat`, …) as **constructors, not givens**, following the
   core rule against a canonical instance when several lawful ones exist. Array folds compile to
   `aggregate(arr, zero, (acc, x) => combine(acc, f(x)))`. Dataset folds compile to `agg(...)`.

5. **Lifting a `Dataset` = optics on the row-struct.** Treat a row as
   `Col[S] = struct(ds.columns.map(col)*)`, apply any symbolic optic, then
   `select(result.getField("*"))` / `select("r.*")`. Every `focus(o).modify` therefore becomes
   one projection. Catalyst's `SimplifyExtractValueOps` / `CollapseProject` cancel
   `GetStructField(CreateNamedStruct(...))`, so the round trip should cost nothing. **Plan-shape
   tests** assert that the optimized plan has no residual `named_struct` for top-level edits.

6. **Twin optics from one selector.** `col[S](_.a.b)` returns
   `SparkOptic[S, A] { symbolic: …Col…; eager: …S… }`. The eager half reuses the generics
   module's `lens` / `prism` derivation. Field names come from the encoder's naming (Mirror
   labels), so the two halves address the same field by construction. Differential tests then
   check that they *behave* the same.

7. **Consume via capability.** A pipeline stage takes
   `(using CanModify[Col[S], Col[String]])`, exactly as core recommends. `Col` values are plain
   values, so core capabilities apply, and the module adds no capability traits. Non-optic
   Dataset ops (`join`, …) are offered as consumers that take a capability for the key/field
   they read (`joinOn(using CanGet[Col[L], Col[K]], CanGet[Col[R], Col[K]])`).

8. **Packaging.** Published, `% Provided` Spark. It is **not** in the root aggregate (Spark's
   classpath roughly doubles cold-resolve time and its tests need forked JVMs with `--add-opens`).
   CI runs `spark/test` as its own step, like the demos. If it proves cheap it can be promoted
   into the aggregate later. Also: `-opt-inline:dev.constructive.eo.**` as usual, and expect to
   relax `-Yexplicit-nulls` at Spark boundaries only (Java APIs everywhere). Prefer
   `.nn` / local `@nowarn` over a module-wide opt-out like kyo's.

## Implementation Units

**Phase 0 — Spike (go/no-go, time-boxed).**
- `spark/` sbt project, `% Provided` Spark 4.x `for3Use2_13`, a forked test JVM with Spark's
  `--add-opens` list, and a local `SparkSession` fixture.
- Prove: Scala 3.9 + Spark 2.13 runtime on JDK 17/21/25; one `AgnosticEncoder` derived for a
  nested case class; one `withField` lens composed twice, with `explain(true)` showing the
  collapsed plan. Output: `docs/research/2026-10-xx-eo-spark-spike.md` with the encoder and JDK
  decisions.

**Phase 1 — Pure-value optics (no SparkSession).** `Plated[DataType]`, `StructType` field
`Lens` / `At` (schema evolution), `Row` kit (`field`, `at`, `each`, `Plated[Row]`), `VariantVal`
kit (constructor prisms + navigation, the circe playbook). Laws via the existing
`cats-eo-laws` rule sets: fast and fully lawful, and it lands value early.

**Phase 2 — `Col[A]` + core-carrier symbolic optics.** The opaque `Col`, the struct `Lens`
(`getField` / `withField`), array/map `Modify` (`transform`, `transform_values`), `Getter`s for
pure expressions, and the `Dataset` row-struct lifting (`focus(o).get/modify/replace`).

**Phase 3 — `ColMiss` + `ColFold` carriers.** The carrier instances and composers; symbolic
`Optional` / `Prism` / `At` / `Index` (nullable fields, map keys, `try_cast`, `from_json`,
variant reads); `ColMonoid` constructors; array and Dataset folds.

**Phase 4 — Selector macro + twins.** `col[S](_.a.b.c)`, which emits a Lens or a `ColMiss`
Optional per nullability; twin pairing with the eager generics optics; the `AgnosticEncoder`
derivation (if Phase 0 chose it).

**Phase 5 — Consumers + eager bridge.** Capability-taking `joinOn`, `groupByKey`-style helpers;
`ds.mapThrough(eagerOptic)(f)` and the effectful `modifyA` path via `mapPartitions`, explicitly
marked as leaving Catalyst.

**Phase 6 — Verification + benchmarks.**
- Laws on every eager twin (`LensTests`, `OptionalTests`, `PrismTests`, `FoldTests`).
- The differential property, per optic and per composed chain: for a generated `xs`,
  `spark.createDataset(xs).select(o.symbolic.modify(f)(row)).collect == xs.map(o.eager.modify(f))`.
  This is batched: one Dataset per property, not one Spark job per sample, to keep the suite
  fast. ScalaCheck generators are derived (as in `demoGrpcFilter`), with NULLs injected at
  nullable positions.
- Plan-shape tests (`queryExecution.optimizedPlan` matchers).
- JMH (`spark-bench/`, outside `benchmarks/` like the demo benches): symbolic vs `ds.map` vs
  hand-written `withField`. Expect symbolic ≈ hand-written ≫ `ds.map`.

**Phase 7 — Docs.** Site page (catalogue table + lawfulness caveats), a cookbook recipe
("rewrite a field 5 levels deep in a Parquet table"), CLAUDE.md module row, MiMa baseline.

## Risks

- **Classpath:** Scala 3 stdlib vs Spark's 2.13 expectations, `_2.13`/`_3` suffix conflicts,
  JDK 25 support. Phase 0 is a hard gate, and if it fails the module does not proceed.
- **Encoder internals:** `AgnosticEncoders` is `catalyst` (semi-internal) API, so pin a Spark
  minor version and run a CI matrix over the supported 4.x minors.
- **Nullability laws:** handled by the Lens-vs-`ColMiss` split. Getting the macro's nullability
  detection wrong would hand users an unlawful lens. The differential tests with NULL injection
  are the guard.
- **Expression bloat:** deep `when(...)` nesting from composed `ColMiss` optics may defeat
  codegen (the 64 KB method limit, as in the UPLC demo) on wide schemas. Benchmark wide/deep
  schemas and fuse adjacent `ColMiss` steps in `AssociativeFunctor` if needed.
- **Test time:** a SparkSession costs seconds to start, so use one shared session per JVM and
  batched properties.

## Open Questions

1. Spark target: 4.x only (recommended), or also 3.5?
2. Classic + Connect from day one (recommended, since the `Column` API is shared), or classic
   only?
3. Encoders: own `AgnosticEncoder` derivation (recommended) or depend on `spark-scala3`?
4. Root aggregate: start outside it (recommended), or inside like zio/kyo?
