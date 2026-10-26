---
title: "eo-spark Phase 0 spike — classpath, runtime, encoders, plan shape"
date: 2026-10-25
plan: docs/plans/2026-10-25-001-feat-eo-spark-module-plan.md
verdict: GO, with Spark Connect as the only supported runtime
---

# eo-spark Phase 0 spike

**Verdict: go.** Column optics built from core constructors compile against `spark-sql-api`
4.2.0 alone, run unchanged over Spark Connect, agree with their eager twins row for row, and
Catalyst reduces them to the projection one would write by hand. One plan assumption did not
survive: **Spark Classic cannot run in a JVM on Scala 3.8+'s stdlib**, so the module's runtime is
Spark Connect (client in the user's Scala 3 JVM, engine in a server JVM). That is exactly the
`spark-sql-api` focus the plan had already chosen, now forced rather than preferred.

Everything below is pinned by `spark/src/test/.../ColumnOpticsSpec.scala` (9 examples).

## 1. Versions and module split

- Spark **4.2.0** (latest release; 4.0.x / 4.1.x also published). Only `_2.13` artifacts, consumed
  with `CrossVersion.for3Use2_13`.
- `spark-sql-api_2.13:4.2.0` contains everything the plan needs at compile time: `Column`,
  `functions`, the abstract `Dataset` / `SparkSession`, `Encoder`, `Row`, `types`, and
  `catalyst.encoders.AgnosticEncoders`. The main dependency is just that jar, `% Provided`.
- `spark-connect-client-jvm_2.13:4.2.0` is a 25 MB **shaded fat jar that contains all 1 280
  `spark-sql-api` classes**, wired to its own relocated Arrow / netty / gRPC / Guava.

## 2. Scala 3.9 stdlib vs Spark's Scala 2 reflection (the finding)

Since Scala 3.8 the `scala-library` jar is compiled by Scala 3. It no longer carries the Scala 2
pickles that `scala-reflect` 2.13 reads, and the runtime universe dies during initialisation:

```
scala.reflect.internal.FatalError: class Array does not have a member apply
  at scala.reflect.runtime.JavaUniverse.<init>
  at org.apache.spark.sql.catalyst.ScalaReflection$.<clinit>
```

Every path that touches `scala.reflect.runtime.universe` fails, and Spark touches it in three
places we hit:

| Path | Where | Consequence |
|---|---|---|
| `SparkSession.builder()` (sql-api) | picks the Classic / Connect companion via `currentMirror.reflectModule` inside a `Try` | "Cannot find a SparkSession implementation" — **use the implementation companion directly** (`org.apache.spark.sql.connect.SparkSession.builder()`) |
| Classic codegen | `Invoke.encodedFunctionName` → `ScalaReflection.<clinit>` on the first projection | **Classic is unusable in-process** — not fixable from outside Spark |
| Connect client, decoding a `ProductEncoder` | `ArrowDeserializer` → `ScalaReflection.findConstructor` (plain Java reflection, but the object's initialiser builds the universe) | fixed by our encoder derivation (§4) |

Classic stays fine as the *engine*: the Connect server runs it in its own JVM on the genuine
2.13 stdlib. Spark Classic users on Scala 3.9 have no way in until Spark either ships a Scala 3
build or stops initialising `scala-reflect` eagerly; worth an upstream issue (`ScalaReflection`'s
`universe` is a strict `val`).

Also found in passing: in 4.2.0 `SparkSession.Builder.classic()` selects the **Connect**
companion (`def classic(): this.type = mode(CONNECT_COMPANION)`), an upstream bug. Irrelevant
to us now, but `spark.api.mode=classic` is the working spelling.

## 3. Test harness

- **Server:** `sparkConnectServer`, a classpath-only Scala **2.13.18** sbt project depending on
  `spark-connect`. Not published, not in the root aggregate. The spark specs launch
  `org.apache.spark.sql.connect.service.SparkConnectServer` from its classpath as a child JVM
  (`Sessions.connect`: free port, `spark.master=local[1]`, wait for the port, shutdown hook).
  The server log goes to `spark/target/spark-connect-server.log`. Start-up costs about 25 s, once
  per test run.
- **Client:** the test JVM has the Connect client only. Classpath fixes, all in `build.sbt`:
  - drop `spark-sql-api` and the unshaded `arrow-*` jars from `Test / dependencyClasspath`.
    Next to the fat client they duplicate its classes but wire them to unshaded Arrow, whose
    netty the sql-api POM excludes (`NoClassDefFoundError: io/netty/buffer/AbstractByteBuf`).
  - no spark-core on the client, which also avoids `spark-connect-shims`. Those are stub
    `SparkConf` / `SparkContext` classes that shadow the real spark-core and broke Classic with
    `NoSuchMethodError: SparkConf.set` during the first attempt.
- **JVM flags** (client and server): Spark's `--add-opens` list, plus, on JDK ≥ 23,
  `--sun-misc-unsafe-memory-access=allow`. JDK 24+ denies `sun.misc.Unsafe` memory access by
  default and Arrow's netty allocator then cannot address direct buffers
  (`EmptyByteBuf.memoryAddress` → `UnsupportedOperationException`). JDK 17 rejects the flag, so
  it is version-gated.
- **JDK matrix:** 17 / 21 / 25 all green (§7).

## 4. Encoders: own derivation, and it has to avoid `ProductEncoder`

`SparkEncoders.derived[T]` (a Hearth macro, like eo-generics' derivations) produces
`AgnosticEncoder`s, which ARE `Encoder`s and work for `createDataset` / `.as[T]`:

- primitives, `String`, `Option` map to the stock `AgnosticEncoders` leaves;
- a case class is **not** a `ProductEncoder`. Decoding one makes the Connect client call
  `ScalaReflection.findConstructor` (§2). It is instead a `TransformingEncoder[T, Row]` over a
  `RowEncoder` of its fields. The macro emits the codec: it reads the fields, and it rebuilds `T`
  with a direct constructor call (Hearth's `CaseClass.construct`). The schema is the same and no
  reflection runs. NULL structs pass through the codec as `null` both ways.
  The spike first used `Mirror.fromProduct`; review moved it to Hearth.

Phase 4 extends this (collections, maps, enums / sealed traits, decimals, dates) on the same
`TransformingEncoder` pattern.

## 5. Optics: no new carrier, confirmed

All optics in the spike are core constructors:

- `ColumnOptics.field[S, A](name)` = `Lens[Col[S], Col[A]](getField, withField)`. It is a
  `GetReplaceLens`, so the fused `andThen` applies.
- `ColumnOptics.guarded(present, focus, rebuild)` = core `Optional` whose `getOrModify` is always
  `Right(focus)` (a plan-time hit). `reverseGet` gets the source back from the complement
  `X = (T, S)` and builds `when(present, rebuild).otherwise(source)`. `nullableField` is
  `guarded(_.isNotNull, …)`.
- `ColumnOptics.fieldOf(schema, name)` = core `Optional` with `Left(source)` when the schema lacks
  the field: a plan-time miss (`getOption` is `None`, `modify` hands the column back).

Composition is plain core: Lens ∘ Lens, Optional ∘ Lens, and Optional ∘ Optional ∘ Lens (two
guards: `previous.office.zip`) all go through the existing fused `andThen` overloads. Nothing
Spark-specific was needed in `core`.

## 6. Plan shape

Lifting a `Dataset` by wrapping the row as `struct(columns*)`, applying the optic, and selecting
`r.*` costs nothing. The server's optimized plan for `address.street ← upper(address.street)`:

```
Project [name#16, if (isnull(address#17)) null else named_struct(street, upper(address#17.street), zip, address#17.zip) AS address#22, work#18, ...]
+- LocalRelation [name#16, address#17, work#18, ...]
```

The row struct is gone, untouched columns stay bare attributes, and only the edited struct is
rebuilt. For a guarded edit (`work.zip + 1`, `work` nullable), Catalyst folds our
`CASE WHEN isnotnull(work)` into the `if (isnull(work))` that `withField` emits anyway, so the
per-row guard is free. Both facts are asserted in the spec.

## 7. JDK matrix

| JDK | `sparkIntegration/test` |
|---|---|
| 25 (Temurin 25.0.5) | 9/9 pass |
| 21 (Ubuntu OpenJDK 21.0.12) | 9/9 pass |
| 17 (Temurin 17.0.21) | 9/9 pass |

Spark 4.2 documents Java 17/21/25 support (25 ≥ 25.0.3).

## 8. Consequences for the plan

1. **Runtime = Spark Connect only.** The plan's "Classic + Connect from one artifact" becomes
   "compiles against the shared API, runs on Connect". Classic users on Scala 3.9 are blocked
   upstream (§2). The eager `ds.map` bridge is also off the table for now. Typed UDFs need the
   client to upload closures and the server to deserialize Scala 3 classes on the 2.13 stdlib,
   which is untested and likely to hit the same wall. Column optics, the actual product, are
   unaffected.
2. **Encoders:** `TransformingEncoder` + `RowEncoder` + a Hearth-generated codec is the derivation
   strategy, not `ProductEncoder`.
3. **Test harness** as in §3. Any Spark-touching module in this build needs the same client-only
   classpath and server child process.
4. **Mutation testing / coverage:** `mutationAll` does not include `sparkIntegration` yet. Add it
   once Phase 2 has real code.

## Files

- `build.sbt`: `sparkSqlApi` / `sparkConnectClient` deps, `sparkIntegration` (in the root
  aggregate), `sparkJvmOptions`, `sparkConnectServer`.
- `spark/src/main/.../{Col, ColumnOptics, SparkEncoders}.scala`
- `spark/src/test/.../{Fixtures, ColumnOpticsSpec}.scala`
