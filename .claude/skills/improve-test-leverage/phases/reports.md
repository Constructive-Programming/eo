# Phase 1 — Reports (model: haiku)

Mechanical phase: produce or locate fresh scoverage + stryker4s reports and
extract the baseline numbers into state.

## Reuse before regenerating

Reports are expensive (~15–25 min full sweep). Reuse if same branch and no
main-source changes since they were written:

- Coverage aggregate: `target/scala-*/scoverage-report/scoverage.xml`
- Mutation per module: `<module>/target/stryker4s-report/<timestamp>/report.json`
  (schema v2: `files{}.mutants[].status` ∈ Killed / Timeout / Survived /
  NoCoverage / CompileError / Ignored)

## Regenerating

```sh
SBT_OPTS="-Xmx6g -Xss8m" sbt coverageAll     # FIRST — its `clean` deletes stryker reports
SBT_OPTS="-Xmx6g -Xss8m" sbt mutationAll     # SECOND — stops at avro's known failure (expected)
```

Order is load-bearing: `coverageAll` begins with `clean`, which deletes every
`stryker4s-report` directory. Coverage first, always.

**Single-module re-run** (used by phase 5 per candidate):

```sh
# schemes / circe — module-local suites:
SBT_OPTS="-Xmx6g -Xss8m" sbt -batch 'set ThisBuild/tlFatalWarnings := false' \
  'project schemes' stryker

# core or laws — MUST include the borrow sets or every mutant is NoCoverage:
SBT_OPTS="-Xmx6g -Xss8m" sbt -batch 'set ThisBuild/tlFatalWarnings := false' \
  'set LocalProject("core")/Test/definedTests ++= (LocalProject("tests")/Test/definedTests).value' \
  'set LocalProject("core")/Test/fullClasspath ++= (LocalProject("tests")/Test/fullClasspath).value' \
  'project core' stryker
```

Never `<module>/stryker` (module-scoped task form) — it resolves test
frameworks from the root project and reports 100% NoCoverage. Always
`project <m>; stryker`.

## Baseline into state

Write to `target/test-leverage/state.json`:

- per-module mutant status counts (keyed by module);
- coverage: statement and branch counts **from the aggregate XML's
  `<statement>` elements** — per-module log lines and even the sbt summary
  line can show a single module, not the aggregate;
- `suite_test_count`: number of specs2 examples + properties across all
  `*/src/test/**/*.scala` (count `>>` example registrations);
- `suite_test_loc`: scalafmt-normalized code lines of those files (the
  comment/blank-excluding count from phases/eval.md).

## Module caveats (do not fight these)

| Module | Status | Why |
|---|---|---|
| `core`, `laws` | scored via borrowed `tests/` suite | task-level borrowing; see build.sbt `mutationAll` comment |
| `generics` | structurally unscoreable (0%) | macro code expands at compile time — no runtime mutants; guarded by `generics/test` law specs |
| `avro` | not scored | stryker's forked test-runner fails to initialise in its sandbox (specs pass under plain `sbt test`) |
| `jsoniter` | not scored | instrumenting `PathParser.parseField` overflows the JVM 64 KB method limit (StringLiteral exclusion does NOT rescue it — tested) |
| all | StringLiteral mutants excluded build-wide | error messages / labels / rule-set names — unkillable noise (`ThisBuild / strykerExcludedMutations`) |
| all | CompileError mutants are normal | `-Yexplicit-nulls` flow-typing rejects some null-check mutations; excluded from the score — do NOT strip the flag |
