# Phase 5 — Eval (model: haiku)

The gate, not a formality. Every candidate's value is measured here; nothing
is accepted on prediction.

## Procedure (per candidate)

1. `sbt <affected>/test` — must pass, else fix or revert.
2. Re-run stryker for **every module the candidate claims kills in** (exact
   commands incl. core/laws borrow sets: phases/reports.md).
3. **Diff the new `report.json` against state by mutant key**
   `(file, line, column, mutator, replacement)`. Column is load-bearing: two
   mutants on one line can share mutator AND replacement (both halves of a
   bounds check mutating to `>`); a line-only key collides and under-reports.
4. Count `net_test_lines_added`: run `sbt scalafmtAll` on touched test files
   first, then

   ```sh
   git diff -- '*/src/test/*.scala' \
     | grep -E '^[+-][^+-]' \
     | grep -vE '^[+-]\s*(//|\*|/\*\*|\*/|$)' \
     | awk '/^\+/{a++} /^-/{r++} END{print a-r}'
   ```

   scalafmt is the arbiter of what "a line of code" is (raw counting punishes
   documentation and rewards cramming). File scaffolding
   (package/imports/class declaration) of a NEW spec file is excluded — fixed
   infrastructure that would quantize small artifacts. Measure example
   bodies.
5. Update `suite_test_count` delta (examples added minus examples deleted).

## Verdict rules

- **Accept**: `measured_kills ≥ 1` and body-lines/kill ≤ 8. Negative
  fixtures are exempt from the ratio (they are the only possible killer of
  law-weakening mutants and double as documentation).
- **Revert**: `git checkout -- <touched test files>`; record in
  `do_not_retry` WITH the measured outcome ("predicted 9, measured 0 —
  generator never exceeds OnStackLimit"). A failed measurement is
  information; keep it.
- **Timeout flips are not kills.** Timeout-detected mutants oscillate
  run-to-run (measured across four consecutive runs). A Survived→Timeout
  flip is recorded as incidental, never claimed; kill by value or classify.
- On accept: one small commit
  (`test(<module>): <candidate> — kills N mutants in M lines`), append the
  iteration record to state.

## Interpreting misses

A claimed kill that still survives means the generator never reached the
branch, the assertion is too weak, or the line-targeting was wrong (read the
mutant's column/context again — measured misses corrected the targeting in
every run so far). Fix or downgrade the claim now; never move on with an
unexplained miss.
