# Agent guide

## Project

`cats-eo` — an Existential Optics library for Scala 3, built on top of
[cats](https://typelevel.org/cats/). Sources under `src/main/scala/eo/`, tests
under `src/test/scala/`. Scala `3.8.3` via sbt `1.12.9` (`project/build.properties`),
runs on JDK 17 or JDK 21.

Runtime dependency: `org.typelevel:cats-core_3:2.13.0`.
Test-only: `org.typelevel:discipline-specs2_3:2.0.0`.

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
sbt test                              # run the discipline-specs2 laws
sbt "~compile"                        # watch compile
scalafmt                              # format in place per .scalafmt.conf
scalafmt --check                      # CI-style verify
scalafix --rules RemoveUnused         # example rewrite
scala -e 'println(1 + 2)'             # quick REPL eval
```

### Test-suite quality

[`sbt-scoverage`](https://github.com/scoverage/sbt-scoverage) lives in
[`project/plugins.sbt`](./project/plugins.sbt) for statement / branch
coverage:

```sh
sbt "clean; coverage; tests/test; coverageReport"
# HTML + XML under core/target/scala-<ver>/scoverage-report/
```

The report directory sits under `target/` and is `.gitignore`d.

Coverage is the project's primary quality signal. The law and behaviour
suites in `cats-eo-tests` currently reach ~70 %% of core's statements and
branches — the rest is either pure type-level machinery (no runtime
footprint) or code reachable only when someone adds the matching carrier
/ composer instances to core.

**Why not mutation testing?** sbt-stryker4s was evaluated earlier and
dropped. Because EO is mostly type-level, stryker4s' default mutators
find exactly **one** mutable runtime expression across the whole
codebase — not enough signal to justify the plugin, the per-checkout
`target/stryker4s-report/` directories, or the CI time. *Future work:*
teaching stryker about EO-specific mutators (e.g. swapping the `to` /
`from` halves of an `Optic` constructor, or flipping associators in
`AssociativeFunctor` instances) would make mutation testing a much
richer signal than the default AST-level mutations. A good project for
someone who wants to understand stryker's mutator plugin API.

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
fork count of at least 1, the `-prof` profilers when investigating
specific suspicions, and the usual reminder that "the numbers below are
just data".

### Auto-derivation: `eo-generics`

`generics/` is a separate sub-project that synthesises boilerplate
optics at compile time, built on top of Mateusz Kubuszok's
[`com.kubuszok:hearth_3:0.3.0`](https://github.com/MateuszKubuszok/hearth)
macro-commons library. Two entry points so far:

```scala
import eo.generics.{lens, prism}

// Product-type Lens (GenLens-style partial application):
case class Person(name: String, age: Int)
val ageL  = lens[Person](_.age)            // Optic[Person, Person, Int, Int, Tuple2]
val nameL = lens[Person](_.name)

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
