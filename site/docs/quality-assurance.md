# Quality Assurance

`cats-eo` is mostly a *type-level* library: a large part of its correctness is
discharged by the compiler before any test runs. The quality signals it tracks
reflect that, in roughly increasing cost-to-fool order:

1. **Types as tests** — the [composition matrix](#composition-matrix) pins, at
   compile time, exactly which optic families compose with which (and at what
   strength), and which combinations are deliberately rejected. A regression
   that loosened or broke the lattice fails to compile.
2. **Discipline law suites** — `cats-eo-laws` defines the optic and typeclass
   laws; `cats-eo-tests` and the integration modules run them against concrete
   instances.
3. **Statement / branch [coverage](#coverage)** (scoverage) — the project's
   primary runtime-quality signal. See the
   [`CLAUDE.md` coverage note](https://github.com/Constructive-Programming/eo/blob/main/CLAUDE.md)
   for why ~70–80 % is the expected ceiling: the remainder is pure type-level
   machinery with no runtime footprint, or code reachable only once a
   downstream carrier instance is added.
4. **[Mutation testing](#mutation-testing)** (stryker4s) — the strongest and
   most expensive signal, and the one that historically did *not* pay its way
   here. It was reintroduced once the `schemes` module grew real runtime
   machinery (the `ArrayDeque` fold machine, the effectful M-drivers, `PSVec`)
   and the opaque-carrier compose work added runtime dispatch to `core`.

The numbers below are regenerated from the live reports by
[`site/tools/gen-qa-report.py`](https://github.com/Constructive-Programming/eo/blob/main/site/tools/gen-qa-report.py);
see [Regenerating these numbers](#regenerating-these-numbers).

## Composition matrix

Every `(outer family ∘ inner family)` pair either composes — *import-free and
without a type ascription*, landing at the strength described in the
[optic taxonomy](optics.md) — or it does **not** compile, because the cell is
void by design (building through a non-invertible optic, writing through a
read-only one, reading through a write-only one). The grid is the pass/fail
projection of
[`CompositionMatrixSpec`](https://github.com/Constructive-Programming/eo/blob/main/tests/src/test/scala/dev/constructive/eo/CompositionMatrixSpec.scala),
which is the single source of truth — if a cell starts needing an import or an
ascription, that spec goes red.

<!-- BEGIN GENERATED: matrix -->
| outer ∘ inner | iso | lens | prism | optional | trav | getter | affold | fold | modify | review | unfold |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **iso** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **lens** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **prism** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **optional** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **trav** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **getter** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **affold** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **fold** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **modify** | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ |
| **review** | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| **unfold** | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |

*✓ composes import-free at the strength shown in the [optic taxonomy](optics.md); ✗ does not compile (void by design — building through a read-only optic, reading through a write-only one, etc.). 87 composing / 34 void cells, pinned by `CompositionMatrixSpec`.*
<!-- END GENERATED: matrix -->

## Coverage

Statement and branch coverage per package, from the cross-module scoverage
**aggregate** (`sbt coverageAll`). `BC/SC` is the branch-coverage ÷
statement-coverage ratio — a value well below 1 flags a package whose
conditionals are under-exercised relative to its straight-line code, even when
its statement coverage looks healthy. Packages with no branch statements show
`—`.

<!-- BEGIN GENERATED: coverage -->
| Package | Statements | Stmt&nbsp;% | Branches | Branch&nbsp;% | BC/SC |
|---|--:|--:|--:|--:|--:|
| `dev.constructive.eo.accessor` | 8/8 | 100.0% | — | — | — |
| `dev.constructive.eo.avro` | 979/1430 | 68.5% | 214/355 | 60.3% | 0.88 |
| `dev.constructive.eo.circe` | 665/846 | 78.6% | 128/174 | 73.6% | 0.94 |
| `dev.constructive.eo.compose` | 90/118 | 76.3% | 7/8 | 87.5% | 1.15 |
| `dev.constructive.eo.data` | 998/1139 | 87.6% | 153/172 | 89.0% | 1.02 |
| `dev.constructive.eo.forgetful` | 24/28 | 85.7% | — | — | — |
| `dev.constructive.eo.generics` | 397/472 | 84.1% | 66/79 | 83.5% | 0.99 |
| `dev.constructive.eo.jsoniter` | 436/583 | 74.8% | 109/170 | 64.1% | 0.86 |
| `dev.constructive.eo.laws` | 151/151 | 100.0% | 6/6 | 100.0% | 1.00 |
| `dev.constructive.eo.laws.data` | 29/29 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.data.discipline` | 21/21 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.discipline` | 190/190 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.discipline.internal` | 21/21 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.eo` | 65/65 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.eo.discipline` | 96/96 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.typeclass` | 29/29 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.typeclass.discipline` | 36/36 | 100.0% | — | — | — |
| `dev.constructive.eo.optics` | 404/483 | 83.6% | 46/52 | 88.5% | 1.06 |
| `dev.constructive.eo.schemes` | 187/187 | 100.0% | 35/35 | 100.0% | 1.00 |
<!-- END GENERATED: coverage -->

## Mutation testing

stryker4s mutates each module's `main` sources and re-runs that module's own
test suite per mutant. *Score (total)* counts no-coverage mutants against the
score; *Score (covered)* is restricted to mutants on exercised lines. `Timeout`
mutants count as detected (the mutant visibly broke the run). `Compile err`
mutants — mutations that don't type-check, common in this codebase's
match-type / opaque-carrier code — are excluded from both scores.

**StringLiteral mutants are excluded build-wide**
(`ThisBuild / strykerExcludedMutations`): in this codebase string literals are
error messages, vestigial-arm labels, and discipline rule-set names — nothing
any suite asserts on, so they survive as pure noise and drown the genuine
survivors (in `laws`, 99 of 101 unfiltered survivors were rule-set name
labels). They appear as *Ignored* in the HTML reports and are counted in no
score.

Caveats that keep the table honest:

- **`core`** — its behavioural suite lives in the separate `tests/` module
  (which depends on `laws` and `generics`, so core can't `dependsOn` it back —
  that's a project cycle). `mutationAll` instead *task-borrows* the compiled
  suite: it appends `tests/Test/definedTests` and `tests/Test/fullClasspath`
  to core's Test scope for the mutation run only. This is sound because
  stryker compiles every mutant into core's classes behind runtime switches
  (binary-compatible), so specs compiled against unmutated core still
  exercise the mutated bytecode. Core's row below is scored against the full
  cross-module suite.
- **`generics`** is macro code: it expands at *compile* time, so its mutants
  leave no runtime footprint for a test *run* to cover. Mutation testing
  structurally can't score it — the derived-optic laws in `generics/test`
  guard it instead.
- **`laws`** has no in-module tests, so it borrows the `tests/` suite the same
  way core does. Mutating the law *definitions* is the "who tests the tests"
  probe: a killed mutant means the discipline suites notice a corrupted law; a
  survivor pinpoints a law whose discriminating power nothing exercises. The
  surviving mutants are all *law-weakening* mutations (`missIsEmpty`'s guard →
  `false` in `AffineFoldLaws`, `&&` → `||` in `ModifyFLaws.functorIdentity` /
  `functorComposition`): every instance under test satisfies the weakened law
  too, so nothing fails. Killing those requires **negative fixtures** —
  deliberately unlawful instances pinned to fail the suite — which is the
  concrete follow-up this table surfaces.

Two modules can't currently be scored, for reasons worth recording:

- **`jsoniter`** — instrumenting `PathParser.parseField` (one large byte-cursor
  method) with its mutants overflows the JVM's 64 KB per-method bytecode limit,
  so the sandbox won't compile. It's un-mutatable in place without splitting the
  method or excluding it.
- **`avro`** — stryker's forked test-runner fails to initialise in the sandbox
  (the initial test run dies in well under a second, emitting no test output),
  even though the same specs pass under plain `sbt test`. Reproduced under both
  JDK 21 and JDK 25 with the default and legacy runners.

The high-signal rows are therefore `core` and `laws` (via the borrowed suite),
`schemes`, and `circe` — modules whose mutated code is genuinely exercised at
run time by the suite stryker runs.

<!-- BEGIN GENERATED: mutation -->
> _No stryker4s reports found. Run `sbt mutationAll`, then re-run `site/tools/gen-qa-report.py`._
<!-- END GENERATED: mutation -->

## Regenerating these numbers

```sh
# Statement / branch coverage (cross-module aggregate):
SBT_OPTS="-Xmx6g" sbt coverageAll
#   → target/scala-3.8.3/scoverage-report/  (HTML + scoverage.xml)

# Mutation testing across the runtime-logic modules:
SBT_OPTS="-Xmx6g" sbt mutationAll
#   → <module>/target/stryker4s-report/<ts>/  (index.html + report.json)

# Rewrite the tables above from those reports + CompositionMatrixSpec:
python3 site/tools/gen-qa-report.py            # in place
python3 site/tools/gen-qa-report.py --check     # CI: non-zero if stale
```

Both aliases relax the always-on `-Werror` (`tlFatalWarnings`) first, since
instrumented sources can surface `-Wunused` warnings; the larger heap is
because the `set` reapply re-evaluates the Laika docs settings. Mutation runs
with `project <m>; stryker` (not `<m>/stryker`) so specs2 is visible to the
test runner — see
[`project/plugins.sbt`](https://github.com/Constructive-Programming/eo/blob/main/project/plugins.sbt).

The [`quality.yml`](https://github.com/Constructive-Programming/eo/blob/main/.github/workflows/quality.yml)
workflow runs all three on every release tag (and on demand via
`workflow_dispatch`), uploading the HTML reports and the regenerated tables as
build artifacts.
