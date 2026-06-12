# Agent guide

## Project

`cats-eo` — an Existential Optics library for Scala 3, built on top of
[cats](https://typelevel.org/cats/). Scala `3.8.3` via sbt `1.12.9`
(`project/build.properties`), runs on JDK 17 or JDK 21.

Human contributors: see [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the
day-one bootstrap. This file is the parallel guide for AI agents.

Runtime dependency: `org.typelevel:cats-core_3:2.13.0`.
Test-only: `org.typelevel:discipline-specs2_3:2.0.0`.

### Module structure

| Module | Directory | Artifact | Purpose |
|--------|-----------|----------|---------|
| `core` | `core/` | `cats-eo` | Hand-written optics and data structures |
| `laws` | `laws/` | `cats-eo-laws` | Discipline-style law definitions (reusable by downstream projects) |
| `tests` | `tests/` | — (not published) | Law-based and behavioural test suites |
| `generics` | `generics/` | `cats-eo-generics` | Auto-derivation of Lens/Prism via Scala 3 quoted macros |
| `schemes` | `schemes/` | `cats-eo-schemes` | Recursion schemes (cata/ana/hylo) as composable optics |
| `circe` | `circe/` | `cats-eo-circe` | `Plated[Json]` and circe optic integration |
| `avro` | `avro/` | `cats-eo-avro` | Apache Avro optic integration; the `eo.avro.circe` sub-package is the structural Avro ↔ circe bridge (`AvroJson`) and `eo.avro.vulcan` bridges `vulcan.Codec` → `AvroCodec` (`AvroVulcan`) — circe and vulcan are `Optional` deps, callers add them themselves |
| `jsoniter` | `jsoniter/` | `cats-eo-jsoniter` | jsoniter-scala optic integration |
| `benchmarks` | `benchmarks/` | — (not published) | JMH benchmarks vs Monocle (not part of root aggregate) |

The root project aggregates `core`, `laws`, `tests`, `generics`, `schemes`,
`circe`, `avro`, and `jsoniter`. `sbt compile` and `sbt test` cover those;
benchmarks must be invoked explicitly (see below).

## Toolchain

Every tool below is installed via [Coursier](https://get-coursier.io). One-shot
bootstrap:

```sh
# Coursier launcher (native)
curl -fLo /usr/local/bin/cs \
  https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz \
  && gunzip -f /usr/local/bin/cs && chmod +x /usr/local/bin/cs

# A recent JVM (sbt 1.12 supports JDK 17 and JDK 21)
cs java --jvm temurin:21 --setup        # writes JAVA_HOME into ~/.profile

# Scala dev tools
cs install --install-dir /usr/local/bin sbt scala scalafmt scalafix metals metals-mcp
```

After `cs java --setup`, open a fresh shell (or `source ~/.profile`) so
`JAVA_HOME` is on your PATH.

Installed versions in this environment: `sbt 1.12.9`, `scala-runner 1.12.4`
(Scala `3.8.3` by default, matching the project), `scalafmt 3.11.0` (honours
the `version` pin in `.scalafmt.conf`), `scalafix 0.14.6`, `metals 1.6.7`,
`metals-mcp 1.6.7`.

### Day-to-day commands

```sh
sbt compile                           # full project compile
sbt test                              # run all test suites (core, tests, generics)
sbt "~compile"                        # watch compile
scalafmt                              # format in place per .scalafmt.conf
scalafmt --check                      # CI-style verify
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

- **Invoke as `project <m>; stryker`, NOT `<m>/stryker`.** The
  module-scoped task form reads `loadedTestFrameworks` from the
  aggregating root project (no test deps), so specs2 is invisible and
  *every* mutant comes back `NoCoverage`. The `mutationAll` alias uses the
  project-switch form across core, laws, generics, schemes, circe, avro,
  jsoniter.
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
same-repo PRs only); `bench-sweep.yml` runs the nightly-if-changed /
release-tag full sweep, appends `bench/series.jsonl` on gh-pages, renders
the chart, bot-commits `BENCHMARKS.md` (generated — never hand-edit), and
attaches results to releases. All logic lives in
`.github/bench/bench_tools.py` (stdlib-only, unit-tested — run
`python3 -m unittest discover .github/bench`); the path→bench mapping is
its `MODULE_BENCHES` dict (import-derived; `core/` or harness changes ⇒
full suite). Doctrine: **B/op gates (only once
`.github/bench/thresholds.json` exists, set from A/A calibration), ns/op
advises**. Operator runbook: `.github/bench/README.md`.

### Auto-derivation: `eo-generics`

`generics/` is a separate sub-project that synthesises boilerplate
optics at compile time, built on top of Mateusz Kubuszok's
[`com.kubuszok:hearth_3:0.3.0`](https://github.com/MateuszKubuszok/hearth)
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

Beyond the scalafix `DisableSyntax` bans in `.scalafix.conf` (no `return`,
no XML, no `finalize`):

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
