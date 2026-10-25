# Agent guide

## Project

`cats-eo` — an Existential Optics library for Scala 3, built on top of
[cats](https://typelevel.org/cats/). Scala `3.9.0` (the `scala3Version` pin in
`build.sbt`) via sbt `1.13.0` (`project/build.properties`), runs on JDK 17, 21, or 25 — but the `kyo` module (and therefore the docs site) needs JDK 25; on older JVMs it drops out of the root aggregate.

Human contributors: see [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the
day-one bootstrap. This file is the parallel guide for AI agents.

Runtime dependency: `org.typelevel:cats-core_3:2.13.0`.
Test-only: `org.typelevel:discipline-specs2_3:2.0.0`.

### Module structure

| Module | Directory | Artifact | Purpose |
|--------|-----------|----------|---------|
| `core` | `core/` | `cats-eo` | Hand-written optics and data structures; cats-typeclass bridges live as CONSTRUCTORS on optic companions, not givens (`Traversal.each`/`first`/`second`/`both`, `Modify.functor`, `Fold[F, A]`, `Lens.representable(r)` positional Lens, `Indexed.representable(r)` fixed-index tabulation). Clients declare their own givens, since multiple lawful optics per `(F[A], A)` pair rule out a canonical one. `Optional` is a sealed abstract class with an existential intermediate (`type M`, `X = (T, M)`): every fused composition supplies a one-tear read, `modify`, and `to` / `rebuild`, so no write re-runs its outer `tear` (only `Optional.apply` and the private `Optional.fused` construct one; never re-add a `reverseGet` that re-reads `S`). `Optional.modify` / `replace` are `inline` and forward to the two-argument `modifyAt(s, f)`, so `optional.modify(f)(s)` allocates no `S => T` closure — implementations override `modifyAt`, never `modify`. `ReadCompose` reads through an optic's own `get` / `getOption` (`CanGet` / `CanGetOption`) instead of its carrier `to(s)`, and a `ForgetFold[S, F, A]` composed with a single-focus inner stays in `F` (`forgetTotal`: cats `Functor[F]`; `forgetPartial`, middle priority: cats `FunctorFilter[F]`, an inner miss is `F`'s empty) — only multi-focus inners or containers without those instances fall back to `ForgetFold[S, List, C]`. `MultiFocus` backs container traversal/aggregation; `optics.Indexed` (in `optics/Indexed.scala`) extends `Optic` with carrier `data.GlassF[I] = [X, A] =>> (context: X, values: I => A)` (in `data/Glass.scala`) and existential `X`, using only `to` / `from`. Its dedicated `andThen` takes product indexes; `Indexed.Grate` is the `X = Unit` alias, with Unit-context composition normalization still open. No generic classical-family Glass bridges or top-level Grate companion are installed. |
| `laws` | `laws/` | `cats-eo-laws` | Discipline-style law definitions (reusable by downstream projects) |
| `tests` | `tests/` | — (not published) | Law-based and behavioural test suites |
| `generics` | `generics/` | `cats-eo-generics` | Auto-derivation of Lens/Prism via Scala 3 quoted macros |
| `schemes` | `schemes/` | `cats-eo-schemes` | Recursion schemes (cata/ana/hylo) as composable optics |
| `schemesLaws` | `schemes-laws/` | `cats-eo-schemes-laws` | Laws for the recursion schemes (hylo fusion so far; more expected) — separate from `laws` because they quantify over `schemes` types |
| `circe` | `circe/` | `cats-eo-circe` | `Plated[Json]` and circe optic integration |
| `avro` | `avro/` | `cats-eo-avro` | Apache Avro optic integration; the `eo.avro.circe` sub-package is the structural Avro ↔ circe bridge (`AvroJson` + the `.json` / `.avro` cursor faces on `AvroPrism`/`JsonPrism`), `eo.avro.jsoniter` the AST-free Avro ↔ JSON-bytes twin (`AvroJsoniter` + the same faces on `JsoniterPrism`), and `eo.avro.vulcan` bridges `vulcan.Codec` → `AvroCodec` (`AvroVulcan`) — circe, cats-eo-circe, jsoniter-scala-core, cats-eo-jsoniter, and vulcan are `Optional` deps, callers add them themselves. NB avro depends on the circe/jsoniter MODULES (Optional); the reverse would be a project cycle, so the cross-format bridge specs live in `avro/src/test` |
| `jsoniter` | `jsoniter/` | `cats-eo-jsoniter` | jsoniter-scala optic integration |
| `zio` | `zio/` | `cats-eo-zio` | ZIO 2 integration: `ZEnvironment` service lens, capability-driven `Ref` focus ops, `ZLayer` projection through `CanGet`; STM focus ops on `TRef`/`TMap` (USTM-returning — multi-focus updates compose into one transaction); `Chunks.each/at/eachNonEmpty` constructors (private Traverse adapters — the orphan given belongs to zio-interop-cats). Three `% Optional` sub-packages (callers add the artifact, avro/circe/kyo pattern): `eo.zio.schema` — `EoAccessorBuilder` fills zio-schema's own AccessorBuilder extension point (Lens per field / Prism per case / Traversal per collection, no macros), `DynamicValues` untyped-tree kit (constructor prisms + generic `primitive(st)`, `field`/`at`/`key`/`variant`/`each`, `Plated`), `schema.dynamicPrism` typed↔untyped, `BinaryCodec.prism` byte face (json/protobuf/avro/msgpack/thrift); `eo.zio.json` — circe playbook on `zio.json.ast.Json` (`JsonValues` prisms + navigation + `Plated` + `text` String↔Json face, `JsonCodec.stringPrism`) plus `JsonCursor.optional` bridging zio-json's own typed paths; `eo.zio.prelude` — `Validations.success` (Optional; prism would drop the log) + `eachFailure` (error-polymorphic traversal). Cross-format spec fuel: zio-schema-derivation + zio-schema-json are Test-scope |
| `kyo` | `kyo/` | `cats-eo-kyo` | Kyo integration (kyo-prelude 1.0.0-RC6 — kyo RC5+ ships Java-25-only bytecode, so this module REQUIRES JDK 25 and leaves the root aggregate on older JVMs; CI's primary lane is 25): `TypeMap` service lens, `Env.focus` / `Layer.focus`, `Var` focus ops; `Record.iso[T]` (quoted macro, NamedTuple OR case class ↔ `Record`, no arity ceiling) + `Record.lens[F]("name")` field lenses via `Fields.Have`; `StructureValues.field`/`key` target the FIRST match (Chunk-backed ⇒ duplicates representable; write-all breaks `modify(identity)`), and `atField` is the At-shaped create/delete optic — lawful up to field ORDER only (delete destroys the position a later insert would restore, so put-put fails; `NavigationTests` yes, `SeamTests` no — the zio kits hide the same behaviour behind order-insensitive equality); the `eo.kyo.schema` sub-package bridges kyo-schema (`% Optional` — callers add it themselves, avro/circe pattern): `Focus` → Lens/Optional, `Schema.prism[Codec]` / `stringPrism` byte-face prisms, and `StructureValues` — optics over the untyped `Structure.Value` tree (constructor prisms, `field`/`at`/`key`/`each`/`variant` navigation, `Plated[Value]`, `Schema.valuePrism` typed↔untyped; `variant` accepts both the declared `VariantCase` spelling AND the single-field wrapper record RC6's encoder actually emits). Module opts out of `-Yexplicit-nulls` (kyo's inline kernel isn't nulls-clean) and `-Wconf`-silences kyo-schema-origin warnings |
| `chimney` | `chimney/` | `cats-eo-chimney` | chimney 2.1.0 integration (chimney 2.x is hearth-based, like `generics`). chimney → eo: `Chimney.getter` (`Transformer` → `Getter`, read-only — no way back), `Chimney.iso` (chimney `Iso` → `BijectionIso`), `Chimney.prism[Domain, Dto]` (`Codec` → `PickMendPrism[Dto, Domain, Domain]`: decode = getOption, encode = reverseGet), `Chimney.resultFold` / `.toResultFold()` (`PartialTransformer` → core `ForgetFold[S, PartialResult, T]`; `PartialResult` is an opaque alias of chimney's `partial.Result` whose companion carries `Monad` / `Foldable` / `FunctorFilter`, so core's `ReadCompose` keeps the errors through `.andThen` with no import — lens/iso/`Getter` map, prism/optional/`AffineFold` `mapFilter` (inner miss → `fromEmpty`), another result fold `flatMap`s; `.result` / `.either` / `.getOption` / `.affineFold` are extensions on `ForgetFold[S, PartialResult, A]`; multi-focus inners fall back to core's `List` fold. There is no chimney-specific fold class: never re-add one — extend core's `ForgetFold` instead), `Chimney.patching` (`Patcher[A, P]` → `Modify[A, A, Unit, P]`, `replace(p)` patches); each also a `.toX` extension on the chimney value. `Chimney.iso` / `prism` are `inline` (chimney's `Iso.derive` / `Codec.derive` are macros, no given); the rest summon — chimney 2's `Transformer` / `PartialTransformer` / `Patcher` companions carry an `inline given derive`. eo → chimney at a focus (`Optic` extensions, like core's ops): `.patch(p)`; `.transformFocus[C]` (`Transformer[C, B]` pre-composed onto the write side — returns `Optic[S, T, A, C, F]` written with a `C`, reads untouched: `to` is the original's, so no identity `map` on reads (core's `innerProfunctor.lmap` would pay one); works on generics' monomorphic lenses; on a polymorphic optic, `.transformFocus[A].modify(f)` edits in the read type and writes the converted `B` — never document a `modify(identity)` idiom for it), `.transformFocusPartial[C]` (`PartialTransformer[C, B]` → `Optic[S, partial.Result[T], A, C, F]`; its `from` is one pass: the carrier's `map` turns every written `C` into `B | Null` with errors collected on the side, and the original `from` rebuilds only if nothing failed — the optic's write never sees a null; the `ForgetfulTraverse` gate is kept, unused, to restrict it to carriers whose `map` is eager); on the S → T side, `.transformImage[U]` / `.transformImagePartial[U]` (`Transformer[T, U]` / `PartialTransformer[T, U]` post-composed onto the write side via core's `Optic.outerProfunctor.rmap` — a generic `Optic[S, U, A, B, F]`, reads unchanged, a miss's `T` is transformed too). Laws hold only as far as chimney's directions invert: `ChimneyLawsSpec` runs `IsoTests` / `PrismTests` / `OptionalTests` / `GetterTests` on derived fixtures. generics + laws are Test-only deps. `ChimneyBench` (benchmarks/) runs each op three ways — `eo*` (this module) / `chimney*` (chimney alone) / `hand*`; results table on `site/docs/integrations/chimney.md` (BENCHMARKS.md only pairs eo vs Monocle). Known gaps: a getter∘lens read still allocates chimney's converted value (JIT-elided when chimney is called alone), and on a decode miss the JIT removes less of chimney's own error chain through the optic than in a direct call — eo itself allocates nothing there (JFR-verified) |
| `benchmarks` | `benchmarks/` | — (not published) | JMH benchmarks vs Monocle (not part of root aggregate) |
| `demoUplcJvm` | `demos/uplc-jvm/` | — (not published) | Demo: UPLC (Cardano Plutus Core) → JVM bytecode as a chain of LAWFUL monomorphic eo optics, `CborScript ⇀ TypedProgram ≅ JvmClass ⇀ JvmClassScript` (stages `toTyped`, `lowering`, `compiling` — the last is `UplcJvm.compile` / `decompile`; callers use `UplcJvm.toTyped` / `toJvmClass` / `toBytecode`, all from the script, all `MendTearPrism`s). Two PRISMS around one iso: Scalus decodes ANY declared Plutus Core version, but the ledger takes only those Plutus V3 has at PV11 (`Semantics.versions` = 1.0.0, 1.1.0), so `toTyped` misses on any other; a class can exceed the JVM's 64 KB method / 65 535-constant limits, so `compiling` misses (`UplcJvm.compile` gives the limit as a `Left` — the one catch of ASM's `MethodTooLargeException` / `ClassTooLargeException`). `lowering`'s way back types the embedded script WITHOUT the version check (only a typed program is lowered). Lawful because every stage keeps its script — `TypedProgram` its source, the class the script as Base64 constants — so `reverseGet` walks back exactly; `OpticSpec` runs cats-eo-laws' `PrismTests` on `toTyped` and `toBytecode` (JvmClass has no equality, so no `IsoTests` for `lowering`); tests reach stages through `Oracle.typed` / `jvmClass` / `classFile`, and `Oracle.compare` checks the round trips on every program. The four types hand out COPIES only (`bytes`, `tree`), so nothing changes a value after the compiler made or checked it. Compile time is STACK SAFE (`OpticSpec` compiles 100 000-deep programs on a 1 MB stack): decode, `Typer` and `Lower` are walks of `Step`s (`Step.scala`) — plain calls while shallow, `cats.Eval` on the heap from `Walk.Deep` (128) calls deep — so EVERY recursive call must go through `Walk.nested`. Trampolining every node (plain `Eval`) made translation ~2× slower. Steps run once each, in order, so `Lower` writes instructions as a step is made. Never sequence effectful `Eval`s with cats' `traverse_` / `void`: cats treats `Eval` as pure and drops a step whose result is unused. Scalus' flat decoder/encoder still recurse (overflow at 2 000–20 000 deep on 1 MB). `CborScript` / `JvmClassScript` are opaque over Scalus' `ByteString` (value equality, so the laws can use `==`), `JvmClass` opaque over ASM's `ClassNode` (made only by `Lower` and `UplcJvm.decompile`; `.tree` to inspect); `CborScript.parse` is the checked entry for untrusted bytes. Compiled with `-opt -opt-inline:**,!scala.**`: inlining from the Scala library made the short validations 12–27% slower (it grows `Rt.force` past what the JIT inlines), everything else measured neutral to ~5% faster. Keep generated code free of unreachable instructions (the typer drops what follows a failure): dead code leaves constant-pool entries that break the class file's read/write fixpoint. TypedProgram = typed Scala-like expression over Scalus' own `Term`s (hybrid typing: types are Scalus `DefaultUni`s, builtin signatures its `TypeScheme`s, instantiated at argument types; unboxed `BigInt`/`boolean`/`ByteString`/`String` where provable, boxed Scalus `Constant`s elsewhere), JvmClass written by asm-commons' `InstructionAdapter` (no own IR — and not a library above ASM: those cost translation speed), class file by ASM `ClassWriter` (computes frames). Saturated builtins are direct JVM calls of `scalus.uplc.builtin.Builtins` (or lazy-`Data` natives in `Rt`); the rest, and builtins used as values, run Scalus' CEK meanings. Reuse Scalus code before adding demo-local code: failures are `MachineError`, `Exec.run` returns `eval.Result`. Correctness is DIFFERENTIAL against the Scalus 1.3.0 CEK machine (Plutus V3 @ PV11, variant E — `Semantics.scala`): vendored Plutus conformance suite (`src/test/resources/plutus-conformance`, regenerate with `scripts/refresh-conformance.sh`, never hand-edit) + validators. Result/error only — no budget metering. Not in the root aggregate (Scalus pulls JNI crypto); CI runs `demoUplcJvm/test` as its own step. See its README |
| `demoUplcJvmValidators` | `demos/uplc-jvm-validators/` | — (not published) | On-chain validators compiled to UPLC by `scalus-plugin` (isolated so the plugin never sees the compiler's sources), plus the shared off-chain fixtures (`Contexts`, `Scenarios`); test-scope dep of `demoUplcJvm` |
| `demoUplcJvmBench` | `demos/uplc-jvm-bench/` | — (not published) | JMH for the demo: CEK machine vs generated class per validator, Fibonacci scaling, translation cost. NOT part of `benchmarks/`, the nightly sweep or `BENCHMARKS.md`; its numbers live on `site/docs/cookbook/uplc-jvm.md` (the cookbook's "Blockchain smart contract compilation demo" page). CI only compiles it (`demoUplcJvmBench/Jmh/compile`) |
| `demoGrpcFilter` | `demos/grpc-filter/` | — (not published) | Demo: an fs2 filter on a gRPC stream read as raw bytes off a TCP socket — Length-Prefixed-Message frames (`GrpcFrames.splitFrames` / `joinFrames` pipes) of JSON `Issue`s (64 fields; the 61st, `detail`, a 3-case enum nested 3 objects deep). It classifies and filters every message with eo-jsoniter prisms WITHOUT decoding it (`Wire.detail` = `JsoniterPrism[Issue].field(_.detail).raw` slices the detail's bytes into the opaque `DetailJson`; typed per-case leaf prisms, `JsoniterPrism[Detail.Incident].field(_.impact)…`, read it) and forwards kept frames byte for byte. `Policy.keep` is written ONCE against `CanGetOption` and runs on bytes (`Wire` givens) and on a decoded `Detail` (`Model` givens: eo-generics `prism`/`lens` chains, checked with `OptionalTests`); the differential tests compare the two on every message. Leaf prisms don't check the kind — the policy classifies first. `Wire.kind` is the one string path (`"type"` is the codec's tag, not a model field) and the one construction-time throw; `Issue.decode` the one catch of jsoniter-scala's throwing decode. `GrpcFrames.Malformed` fails the stream (compressed / oversized / truncated frames): no HTTP/2, no compression. Sockets are read 256 KiB at a time (`Net.reads`): fs2's 8 KiB `reads` was the loopback bottleneck. `Issues` = ScalaCheck generators DERIVED from the model by scalacheck-derived (only leaves hand-written: text, JSON-safe doubles, the opaque `Module` / `Votes`); `Issues(seed)` samples at `Issues.Size` (7, ~4 KB/message) with `Seed.slide` — consecutive `Seed.next` seeds draw overlapping, correlated values; derived sums shrink the size by one per level and fail at 0, so properties start at `minSize = 2`. Not in the root aggregate; CI runs `demoGrpcFilter/test` |
| `demoGrpcFilterBench` | `demos/grpc-filter-bench/` | — (not published) | JMH for that demo: one message judged (full decode vs optics, with `detail` late as written or moved first) and the in-memory filter pipe. Numbers live on `site/docs/cookbook/grpc-filter.md` (the cookbook's "Allocation-free stream triage demo" page). CI only compiles it (`demoGrpcFilterBench/Jmh/compile`) |

The root project aggregates `core`, `laws`, `tests`, `generics`, `schemes`,
`schemesLaws`, `circe`, `avro`, `jsoniter`, `zio`, `chimney`, and `kyo`. `sbt compile` and `sbt test`
cover those; benchmarks and the demos must be invoked explicitly
(see below; `sbt demoUplcJvm/test`, `sbt demoGrpcFilter/test`).

Published modules compile with Scala 3's bytecode optimizer (`scala3LibrarySettings` in
`build.sbt`): `-opt`, plus `-opt-inline:dev.constructive.eo.**` — the inliner may copy code
only from eo's own packages, never from cats, the JDK or the Scala library, whose next
releases would leave stale copies in our jars.

## Toolchain

Every tool below is installed via [Coursier](https://get-coursier.io). One-shot
bootstrap:

```sh
# Coursier launcher (native)
curl -fLo /usr/local/bin/cs \
  https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz \
  && gunzip -f /usr/local/bin/cs && chmod +x /usr/local/bin/cs

# A recent JVM (the project runs on JDK 17, 21, or 25; `kyo` needs 25)
cs java --jvm temurin:21 --setup        # writes JAVA_HOME into ~/.profile

# Scala dev tools
cs install --install-dir /usr/local/bin sbt scala scalafmt scalafix metals metals-mcp
```

After `cs java --setup`, open a fresh shell (or `source ~/.profile`) so
`JAVA_HOME` is on your PATH.

Installed versions in this environment: `sbt 1.12.9` (the launcher script — it
boots the sbt version pinned in `project/build.properties`), `scala-runner
1.12.4`, `scalafmt 3.11.0` (honours the `version` pin in `.scalafmt.conf`),
`scalafix 0.14.6`, `metals 1.6.7`, `metals-mcp 1.6.7`.

### Day-to-day commands

```sh
sbt compile                           # full project compile
sbt test                              # run all test suites (core, tests, generics)
sbt "~compile"                        # watch compile
scalafmt                              # format in place per .scalafmt.conf
scalafmt --check                      # CI-style verify
                                      # (`project.git = true`: untracked files are skipped —
                                      #  `git add` new files before formatting them)
scalafix --rules RemoveUnused         # example rewrite
scala -e 'println(1 + 2)'             # quick REPL eval
```

### Git hooks

A `.githooks/` directory ships two scripts that mirror the CI gates so you
don't land a broken commit on main:

```sh
.githooks/install.sh                  # one-shot: points git at .githooks/
git commit --no-verify                # bypass pre-commit if you need to
git push   --no-verify                # bypass pre-push if you need to
```

- `pre-commit` — `sbt scalafmtCheckAll; docs/mdoc; docs/laikaSite`. Only
  runs when staged files touch `*.scala` / `*.sbt` / `*.md` / `.scalafmt.conf`
  / `site/` / `build.sbt`, so unrelated commits (tag moves, merges) pay
  nothing.
- `pre-push` — `sbt test`. Full root-aggregate test sweep before push.
  Does not run benchmarks / circe-integration / docs builds — those are
  covered by pre-commit and CI.

### Test-suite quality

[`sbt-scoverage`](https://github.com/scoverage/sbt-scoverage) lives in
[`project/plugins.sbt`](./project/plugins.sbt) for statement / branch
coverage:

```sh
SBT_OPTS="-Xmx6g" sbt coverageAll
# alias for: set ThisBuild/tlFatalWarnings := false; clean; coverage;
#            test; coverageReport; coverageAggregate
# HTML + XML under <module>/target/scala-<ver>/scoverage-report/
# Aggregate report at target/scala-<ver>/scoverage-report/
```

`coverageAll` runs the whole root-aggregate `test`, so every module's
suite is exercised and `coverageAggregate` rolls them into one
cross-module per-package view at the repo root. It relaxes the always-on
`-Werror` (`tlFatalWarnings`) first because scoverage-instrumented
sources can surface `-Wunused` warnings, and wants a larger heap because
the `set` reapply re-evaluates the Laika docs settings.

The report directory sits under `target/` and is `.gitignore`d.

Coverage is the project's primary quality signal — see the
[Quality Assurance docs page](./site/docs/quality-assurance.md) for the
live per-package numbers (statement %, branch %, BC/SC ratio). The rest
is either pure type-level machinery (no runtime footprint) or code
reachable only when someone adds the matching carrier / composer
instances.

### Mutation testing (stryker4s)

[`sbt-stryker4s`](https://stryker-mutator.io/docs/stryker4s/) lives in
[`project/plugins.sbt`](./project/plugins.sbt). It was dropped in an
earlier pass (a whole-project run found a single mutable runtime
expression — EO was almost entirely type-level) and reintroduced once
`schemes` grew real runtime machinery and core gained opaque-carrier
dispatch.

```sh
SBT_OPTS="-Xmx6g" sbt mutationAll
# per-module reports under <module>/target/stryker4s-report/<ts>/
```

Key facts, all the hard-won kind:

- **Invoke as `project <m>; stryker`** — the `mutationAll` alias uses the
  project-switch form across core, laws, generics, schemes, schemesLaws,
  circe, avro, jsoniter, zio, kyo, chimney. The module-scoped `<m>/stryker` form works
  again since 0.20.4; before that it resolved `loadedTestFrameworks` from the
  aggregating root project (no test deps), so specs2 was invisible and
  *every* mutant came back `NoCoverage`.
- **It's a report, not a gate** (`strykerThresholdsBreak := 0`): a low
  score never fails the build.
- **`core` and `laws` are scored against the `tests/` suite via
  task-level borrowing.** `core.dependsOn(tests % Test)` would be a
  project cycle (tests → laws/generics → core), but cross-project *task*
  references are legal: `mutationAll` appends `tests/Test/definedTests` +
  `tests/Test/fullClasspath` to each module's Test scope for the run.
  Sound because stryker compiles mutants behind runtime switches
  (binary-compatible), so tests' specs — compiled against the unmutated
  modules — still exercise the mutated bytecode (verified: core 3% →
  ~63%/78% covered; laws unscoreable → ~97%). Works even though laws has
  NO test framework of its own — specs2 is detected from the borrowed
  classpath. Deliberately scoped to the alias: a permanent setting would
  make root `sbt test` run the suite twice.
- **StringLiteral mutants are excluded build-wide**
  (`ThisBuild / strykerExcludedMutations`): string literals here are
  error messages, vestigial-arm labels, and discipline rule-set names —
  nothing any suite asserts on, so they survive as pure noise (laws: 99
  of 101 unfiltered survivors were name labels).
- **Mutating `laws` is the "who tests the tests" probe.** Killed mutant =
  the discipline suites notice a corrupted law definition. The surviving
  mutants are law-WEAKENING mutations (guard → `false` in
  `AffineFoldLaws.missIsEmpty`, `&&` → `||` in
  `ModifyFLaws.functorIdentity`/`functorComposition`): all instances
  under test satisfy the weakened law too, so only **negative fixtures**
  (deliberately unlawful instances pinned to fail) would kill them — a
  known follow-up.
  **`generics`** is macro code (expands at compile time → no runtime
  mutants to cover), so it structurally can't be scored. Caveats are
  documented in the QA page. The high-signal modules are `core`, `laws`,
  `schemes`, `circe`.

The [`quality.yml`](./.github/workflows/quality.yml) workflow runs
`coverageAll` + `mutationAll` + `site/tools/gen-qa-report.py` on every
release tag, refreshing the QA page tables and uploading the HTML reports
as artifacts. The `improve-test-leverage` skill
(`.claude/skills/improve-test-leverage/SKILL.md`) consumes both reports
to plan high-leverage test additions. *Future work:* EO-specific mutators
(swapping the `to` / `from` halves of an `Optic`, flipping
`AssociativeFunctor` associators) would beat the default AST-level
mutations — a good entry into stryker's mutator plugin API.

### Benchmarks (JMH vs Monocle)

A separate `benchmarks/` sub-project, built on
[`sbt-jmh`](https://github.com/sbt/sbt-jmh), benchmarks EO's optics
against the equivalent operations on
[`dev.optics:monocle-core`](https://www.optics.dev/Monocle/). It is
**not** part of the root aggregate, so `sbt compile` and `sbt test` skip
it; invoke it explicitly:

```sh
# Full Lens / Prism / Iso / Traversal sweep:
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 1 -t 1"

# Single benchmark, quick smoke check:
sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 LensBench.eoGet"

# Filter by class or method via regex (sbt-jmh syntax):
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 1 .*PrismBench.*"
```

`benchmarks/src/main/scala/eo/bench/OpticsBench.scala` defines four
benchmark classes — `LensBench`, `PrismBench`, `IsoBench`,
`TraversalBench` — each with paired `eo*` / `m*` methods so a single
report row gives the side-by-side comparison.

JMH's own caveats apply: trustworthy numbers need a quiet machine, a
fork count of at least 3 (the annotation defaults are `@Fork(3)`), the
`-prof` profilers when investigating specific suspicions, and the usual
reminder that "the numbers below are just data".

#### Benchmark CI pipeline

Three hand-written workflows (NOT in the generated ci.yml) automate this:
`bench-pr.yml` posts a same-VM A/B delta comment on PRs touching
benchmark-mapped paths (reduced profile, `perf:full` label escalates,
same-repo PRs only). It runs on the default branch after successful PR CI,
not as a check on the PR head; read-only measurement and trusted comment
publication use separate jobs. Manual dispatch from `main` with `pr=<number>`
reruns A/B, or `mode=aa` calibrates the default branch. `bench-sweep.yml`
runs the nightly-if-changed /
release-tag full sweep, appends `bench/series.jsonl` on gh-pages, renders
the chart, bot-commits `BENCHMARKS.md` (generated — never hand-edit), and
attaches results to releases. Logic lives in
`.github/bench/bench_tools.py` and `.github/bench/pr_bench.py`
(stdlib-only, unit-tested — run
`python3 -m unittest discover .github/bench`); the path→bench mapping is
its `MODULE_BENCHES` dict (import-derived; `core/` or harness changes ⇒
full suite). Doctrine: **B/op warns against calibrated thresholds (once
`.github/bench/thresholds.json` exists, set from A/A calibration), ns/op
advises; PR benchmark comments never gate merges**.
Operator runbook: `.github/bench/README.md`.

### Auto-derivation: `eo-generics`

`generics/` is a separate sub-project that synthesises boilerplate
optics at compile time, built on top of Mateusz Kubuszok's
[`com.kubuszok:hearth_3:0.4.2`](https://github.com/MateuszKubuszok/hearth)
macro-commons library. Two entry points so far:

```scala
import dev.constructive.eo.generics.{lens, prism}

// Product-type Lens (GenLens-style partial application):
case class Person(name: String, age: Int)
val ageL  = lens[Person](_.age)            // SimpleLens[Person, Int, <NamedTuple complement>]
val nameL = lens[Person](_.name)

// Varargs: multiple selectors in one call. Focus is a Scala 3
// NamedTuple in SELECTOR order, complement in DECLARATION order
// among non-focused fields. Partial cover → SimpleLens.
case class OrderItem(sku: String, quantity: Int, price: Double)
val qtyAndPrice = lens[OrderItem](_.quantity, _.price)
// → SimpleLens[OrderItem, NamedTuple[("quantity", "price"), (Int, Double)],
//                          NamedTuple[("sku",), (String,)]]

// Full cover (selectors span every case field) → BijectionIso.
// This is the sole change in return-type at arity 1: `lens[Wrapper](_.value)`
// on a 1-field case class now returns a BijectionIso, not a SimpleLens.
val nameAgeIso = lens[Person](_.name, _.age)
// → BijectionIso[Person, Person, NamedTuple[("name", "age"), (String, Int)], ...]

// Sum-type Prism from a Scala 3 enum, sealed trait, OR union type:
enum Shape:
  case Circle(r: Double), Square(s: Double), Triangle(b: Double, h: Double)

val circleP = prism[Shape, Shape.Circle]   // Optic[Shape, Shape, Shape.Circle, Shape.Circle, Either]
val intP    = prism[Int | String, Int]     // Scala 3 union type works too

// Recursive parameterised ADTs are also fine:
enum Tree[+N]:
  case Leaf(value: N)
  case Branch(left: Tree[N], right: Tree[N])

val branchLeftL = lens[Tree.Branch[Int]](_.left)
val leafP       = prism[Tree[Int], Tree.Leaf[Int]]
```

How the macros use Hearth:

- `lens[S](_.field)` parses the selector lambda with vanilla
  `quotes.reflect`, then routes setter construction through Hearth's
  `CaseClass.parse[S]` + `caseFieldValuesAt(s)` + `construct[Id]`.
  The emitted setter calls `new S(...)` (NOT `s.copy(...)`), which
  means it works uniformly for case classes AND for Scala 3 enum
  cases — the latter don't expose `.copy` at all.

- `prism[S, A]` recognises sealed traits, enums, and union types
  through Hearth's `Enum.parse[S]`. The deconstruct is generated
  by `Enum.matchOn`, which automatically picks `MatchCase.eqValue`
  for parameterless enum cases (whose erased type would otherwise
  collide with each other) and `MatchCase.typeMatch` for the rest.
  The emitted reconstruct is a plain upcast `(a: A) => a: S`.

Macro-generated `new T(...)` calls don't carry an outer accessor,
so test ADTs live at top level in `generics/src/test/scala/eo/generics/samples/`
rather than nested inside the spec class — otherwise the back-end
trips on "missing outer accessor in class GenericsSpec".

Behaviour specs in `generics/src/test/scala/eo/generics/GenericsSpec.scala`
check the three Lens laws and three Prism laws on derived instances
across all the supported shapes (case class, enum, union, recursive
parameterised ADT). Independent of `cats-eo-laws` at runtime.

## Code conventions

**Key principle: avoid unnecessary work.** An optic should cost no more than
the hand-written code it replaces. Every operation does the least work its
result needs — before writing or reviewing a code path, ask what it computes
that the caller never uses:

- **A read never pays for a write.** Read-only paths use an optic's direct
  `get` / `getOption`, not its carrier `to(s)`, whose leftover exists only so
  `from` can rebuild (a split-combine lens's `(complement, focus)` tuple is
  built and thrown away on every read otherwise). `ReadCompose` does this.
- **Run each step once.** A composite must not repeat its outer step's work:
  no `reverseGet` that re-runs a `tear` / decode the read already did, no
  second traversal to finish what the first pass could have collected.
- **Stop early.** A miss passes through without building anything; a failed
  validation returns its errors without rebuilding the structure it would
  discard (`transformFocusPartial` maps the foci, then calls `from` only if
  nothing failed).
- **Measure it.** A new fused path or bridge gets a benchmark row next to the
  hand-written equivalent, and B/op (allocation per call) is the figure that
  must not regress. Generic `Morph` / `AssociativeFunctor` routes are the
  fallback, not the destination: when a common composition lands there, give
  the concrete class a fused overload.

Beyond the scalafix `DisableSyntax` bans in `.scalafix.conf` (no `return`,
no XML, no `finalize`) — and, for main code only, `.scalafix-main.conf`'s ban on
`.modify(identity)` (it writes back what it read: dead work, or an idiom hiding the real
operation). `commonSettings` points every project's `Compile / scalafixConfig` at
`.scalafix-main.conf`, which `include`s `.scalafix.conf`; `Test` scopes and the `laws` /
`schemesLaws` modules (whose `src/main` is law code — `modify(identity)` IS the identity law)
stay on `.scalafix.conf`:

- **No `while` loops.** Write a tail-recursive `@tailrec` function instead —
  it compiles to the same loop bytecode (no perf or stack cost) while keeping
  the code expression-oriented. Thread loop counters / accumulators / cursor
  positions as immutable recursion params and return the final value where an
  imperative loop would read a `var` afterward; every recursive call must be in
  tail position (the annotation enforces it). `var` is still allowed for the
  genuine mutable *fields* of the perf-critical builders (`IntArrBuilder`,
  `ObjArrBuilder`, `PSVec` cursors) — it's the `while` *control flow* that's
  banned, in new code as well. (No scalafix rule enforces this: `DisableSyntax`
  has no `noWhile`, and a regex ban false-positives on the word "while" in
  prose — so it's a review convention.)

- **Consume via capability, construct via optic.** Method parameters that
  *use* an optic take the weakest capability trait (`CanGet`, `CanGetOption`,
  `CanModify`, `CanFold`, … — top-level in `dev.constructive.eo`), never a
  concrete optic type; concrete types (`Lens`, `Prism`, `Getter`, …) are for
  construction and composition. Keep ONE optic given per `(S, A)` pair in
  scope — capabilities are keyed by their type params only, so two same-typed
  foci need newtypes or explicit `(using myLens)` passing. A read-then-write
  method demands one `CanModify`, not split `CanGet` + `CanModify` evidence
  (nothing ties two summons to the same optic).

- **Optic before gate typeclass in `using` clauses.** When a signature takes
  an `Optic[…, F]` together with a typeclass on `F`, both go in the SAME
  using clause with the optic first (left-to-right resolution pins `F`).
  Never write the gate as a context bound (`F[_, _]: Accessor`): SIP-64
  desugars it to a clause searched BEFORE the optic, with `F` still free —
  it fails with a misleading "given was not considered" error whenever the
  gate has more than one instance. `CapsMatrixSpec` pins this for the
  capability companions.

- **No exceptions as control flow.** A failure is a value: return
  `Either`/`Ior`/`Option`, or miss through the carrier. `throw` is legitimate
  in exactly three places — (1) converting a third-party throwing API into a
  structured failure at ONE boundary (`AvroCodec`, `AvroBinaryCursor`'s
  `*Uncaught` split — the catch *is* the design there), (2) a fail-fast
  argument check at optic-construction time, (3) an unreachable invariant
  guard. Never `throw` to signal an ordinary miss, and never catch a
  throwable just to decide control flow. The full inventory, verdict per
  site, and the open write-side decision live in
  [`docs/research/2026-09-22-exception-audit.md`](./docs/research/2026-09-22-exception-audit.md)
  — read it before adding a `throw` or a `catch NonFatal` to `src/main`.

## Metals MCP (stdio)

`metals-mcp` (new in v1.6.7) is registered as a project-local MCP server in
[`.mcp.json`](./.mcp.json):

```json
{
  "mcpServers": {
    "metals": {
      "type": "stdio",
      "command": "metals-mcp",
      "args": ["--workspace", ".", "--transport", "stdio"]
    }
  }
}
```

When Claude Code (or any MCP client) starts in this repo, it spawns
`metals-mcp` over stdio and exposes these tools to the agent:

| Tool | Use for |
|------|---------|
| `compile-file`, `compile-module`, `compile-full` | Fast feedback on a change |
| `test` | Run a specific test class / method (`testClass` required) |
| `glob-search`, `typed-glob-search` | Find a symbol by name across workspace + deps |
| `inspect` | Dump full signature, members, docstrings of a symbol |
| plus `find-dep`, `list-modules`, `import-build`, `file-decode`, … | see `tools/list` |

On first use Metals runs `sbt bloopInstall`, creates `.bloop/` and `.metals/`
(both `.gitignored`), and indexes the project. Subsequent calls are cached.

If you prefer the HTTP transport instead, regenerate the config with:

```sh
metals-mcp --workspace . --client claude-code   # writes HTTP .mcp.json, starts server
```

Metals knows about the current project graph, so prefer `glob-search` /
`inspect` for symbols **inside this project** (optics, accessors, instances),
and use `cellar` (below) for symbols **published to Maven** when you want to
answer without spinning up a build.

## JVM API lookups: `cellar`

[`cellar`](https://github.com/VirtusLab/cellar) answers signature, package,
and source queries straight from the `.tasty`/classfile a dependency ships to
Maven. Use it as the first line of investigation for cats, discipline, or any
other library — it is more reliable than guessing at API shapes and faster
than booting metals for external deps.

Install: native binary from
<https://github.com/VirtusLab/cellar/releases/latest>, or
`nix profile install github:VirtusLab/cellar`.

### External queries (no build tool needed)

```sh
cellar get-external     org.typelevel:cats-core_3:2.12.0 cats.Monad
cellar list-external    org.typelevel:cats-core_3:2.12.0 cats.data
cellar search-external  org.typelevel:cats-core_3:2.12.0 Traverse
cellar get-source       org.typelevel:cats-core_3:2.12.0 cats.Monad
cellar deps             org.typelevel:cats-core_3:2.12.0
cellar meta             org.typelevel:cats-core_3:2.12.0
```

### Project-aware queries (uses sbt)

From the repo root, auto-detect `build.sbt` and resolve against the project's
classpath. The module is named `root` in `build.sbt`.

```sh
cellar get    --module root cats.Monad
cellar list   --module root cats.data
cellar search --module root Traverse
```

### Workflow

When uncertain about a symbol:
1. `search` / `search-external` — locate the right name
2. `list`   / `list-external`  — see neighbours in the package
3. `get`    / `get-external`   — read the signature
4. `get-source`                — only when implementation details matter

Prefer `metals`-mcp for intra-project symbols (it knows your sources and local
overrides), `cellar` for dependency symbols (no build required, works offline
against the coursier cache).
