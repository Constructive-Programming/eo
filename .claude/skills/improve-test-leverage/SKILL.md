---
name: improve-test-leverage
description: Orchestrates a coverage + mutation improvement cycle tuned to cats-eo's algebraic structure. Reads scoverage + stryker4s reports, triages surviving mutants, and drives every test artifact UP the promotion ladder (examples → property → law) so the suite kills more mutants with FEWER test lines. Phases live in phases/*.md, each with its own model assignment. Mandatory consolidation and law-scan phases close every run; law promotion is user-gated. Supports a measured eval-optimization loop ("loop" argument).
model: sonnet
---

# Improve Test Leverage

The algebraic test-quality cycle for cats-eo. Instead of asking "which example
test kills this mutant?", it asks **"which law, registered against which
instance, kills the largest family of mutants?"** — and then asks the harder
question every run MUST answer: **"which existing tests does that make
redundant?"**

## The promotion ladder (the spine of everything)

Every test artifact in this repo sits on a ladder, and the skill's job is to
push artifacts UP it — never to let the suite accumulate at the bottom:

```
stage 0   example test            (one input, one assertion)
stage 1   property (forAll)       (one invariant over a generated domain)
stage 2   ruleset registration    (checkAll — an existing law family applied
                                   to a new instance via CheckAllHelpers)
stage 3   LAW in cats-eo-laws     (a new universal property, promoted through
                                   the user gate; ships to clients)
```

Two structural facts make the ladder pay:

1. **Each rung multiplies.** A property replaces N examples; a ruleset is
   ~5–15 properties in one line; a promoted law is inherited by EVERY
   `checkAll` site in the repo plus every downstream client, with zero
   test-file changes.
2. **Placement is attribution.** The `tests/` module is task-borrowed into
   core's AND laws' mutation runs (`mutationAll` in build.sbt), and scoverage
   aggregates cross-module — so a high-rung artifact in `tests/` scores
   everywhere at once, while a `schemes` mutant can only be killed from
   `schemes/src/test`.

**Announce at start:** "I'm using the improve-test-leverage skill (phase:
<phase>)."

## Objective (all three terms, tracked in state)

```
iteration_leverage = (new_kills + new_branches_covered) / max(1, net_test_lines_added)
suite_kill_density = total_detected_mutants / suite_test_code_lines   (must rise)
suite_test_count   = number of examples + properties                  (net delta ≤ 0 per run)
```

- `net_test_lines_added`: scalafmt-normalized **code** lines (comments/blanks
  excluded, file scaffolding of new specs excluded). Measurement recipe in
  [phases/eval.md](phases/eval.md).
- `suite_test_count` is the metric the first two runs neglected: a run that
  only ADDs leaves the suite bigger. The mandatory
  [consolidation phase](phases/consolidate.md) must offset additions —
  **net example-count growth over a run requires explicit justification in
  the final report** (legitimate: NoCoverage-dominated pools, negative
  fixtures, which are documentation).
- Mutants are keyed `(file, line, column, mutator, replacement)` — column is
  load-bearing (same-line same-replacement pairs exist).

## Phases

Each phase is a file under `phases/`, with the model it should run on. Run
gates (user confirmation) and accept/revert decisions inline; dispatch
self-contained phases as subagents with the listed model when the work
doesn't need the conversation's full context.

| # | Phase | File | Model | Why this model |
|---|-------|------|-------|----------------|
| 1 | Reports | [phases/reports.md](phases/reports.md) | haiku | mechanical: run aliases, locate reports, parse |
| 2 | Triage | [phases/triage.md](phases/triage.md) | sonnet | mutant→tier pattern matching, placement |
| 3 | Plan + self-review | [phases/plan.md](phases/plan.md) | sonnet | candidate design, generator distributions |
| 4 | Execute | [phases/execute.md](phases/execute.md) | sonnet | spec writing under repo conventions |
| 5 | Eval | [phases/eval.md](phases/eval.md) | haiku | mechanical: run, diff by key, count lines |
| 6 | **Consolidate (mandatory)** | [phases/consolidate.md](phases/consolidate.md) | **opus** | hardest reasoning: proving a property subsumes examples without kill regression |
| 7 | **Law-scan (mandatory)** | [phases/law-scan.md](phases/law-scan.md) | **opus** | holistic cross-suite generalization; candidate laws need real proofs-of-universality |
| 8 | Report | [phases/report.md](phases/report.md) | haiku | tables, QA page regen (ORDER: coverage before mutation re-runs — see file) |

Consolidate and law-scan get the strongest model because they are where the
user's leverage actually lives — shrinking the suite and minting laws — and
where a weaker model produces the failure mode the first two runs measured:
plenty of ADDs, zero reductions, zero law candidates surfaced.

## Modes

- **Guided (default)**: phases 1→8 in order, one planned batch, user-confirmed
  at phase 3→4.
- **Loop** (`loop [N]`): phases 1–3 build the pool; then N accepted
  iterations of pick→implement→eval (phases 4–5 per candidate); then
  **phases 6 and 7 run unconditionally** before phase 8. The loop is not
  done when the budget is spent — it is done when the suite is SMALLER or
  the report justifies why not.
  - Iteration moves: ADD (tiers from triage), CLASSIFY-OUT (justified
    equivalents). CONSOLIDATE is NOT an iteration move — it cannot win a
    kills-per-line ranking (its kills are zero by definition), which is
    exactly why the first two runs never picked it. It is phase 6, always.
  - Accept: `measured_kills ≥ 1` and body-lines/kill ≤ 8. Revert into
    `do_not_retry` WITH the measured outcome.
  - Stop: budget; or 2 consecutive reverts; or pool exhausted. Then 6→7→8.
- **Aliases**: `/test-upgrade` (phases 1–5, ADD-only bias),
  `/test-fix` (phases 1–5, gap-closing bias), `/test-prune` (phases 1, 6, 8
  only — pure reduction).

## State

`target/test-leverage/state.json`: baseline (per-module status counts, suite
test count, suite test LOC), iteration records (candidate, predicted vs
measured, verdict), `do_not_retry` (with measured outcomes — failed
predictions are information), `classified_out` (with reviewable
justifications), `pending_promotions` (law candidates awaiting the user when
running unattended).

## Guardrails

- Never weaken an assertion or delete a test to shrink the denominator —
  phase 6's eval re-runs mutation and requires ZERO kill regression,
  measured, never assumed.
- Never mark a mutant equivalent to dodge a hard test; justifications must be
  reviewable.
- Only `*/src/test/` files change, except a user-approved law promotion
  (touches `laws/src/main` + its `.discipline` package — see phases/law-scan.md).
- The law gate ALWAYS stops for the user, in every mode. Unattended runs
  queue candidates in `pending_promotions` instead of blocking.
- Timeout-detected kills are run-unstable (measured: one mutant flip-flopped
  across four consecutive runs) — never count a Timeout flip as a claimed
  kill; classify the mutant or kill it by value.
