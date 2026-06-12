# Phase 8 — Report (model: haiku)

Close the run: refresh the QA page and present the trajectory with all three
objective terms.

## Refresh sequence (ORDER IS LOAD-BEARING)

1. `SBT_OPTS="-Xmx6g" sbt coverageAll` — FIRST: its `clean` deletes every
   stryker4s-report directory.
2. Re-run mutation for the modules the run touched (commands in
   phases/reports.md).
3. `python3 site/tools/gen-qa-report.py` — regenerates the QA page tables;
   `--check` must then pass (idempotency).
4. Coverage truth = the aggregate XML's `<statement>` elements only. Neither
   per-module log lines nor the sbt summary line are the aggregate.

## The report

Present the trajectory, not just the endpoint:

```
| Iter | Candidate | Pred | Meas | Body lines | Lines/kill | Verdict |
|------|-----------|-----:|-----:|-----------:|-----------:|---------|
| ...one row per iteration, accepted AND reverted...

| Suite metric            | Before | After |
|-------------------------|-------:|------:|
| detected mutants        |        |       |
| suite_test_count        |        |       |   <- net delta ≤ 0 or justified
| suite test code lines   |        |       |
| suite_kill_density      |        |       |   <- must have risen
| branch coverage         |        |       |

Promotions this run: <N examples folded into M properties; K properties
registered as rulesets; law candidates surfaced: L (gated: promoted/kept/
rejected/pending)>
```

Plus: classify-outs with justifications, `do_not_retry` additions with
measured outcomes, and salvage notes for reverted artifacts a human might
still want.

If `suite_test_count` grew, the report MUST carry the justification (e.g.
"pool was NoCoverage-dominated: new reach requires new artifacts; phase 6
folded 9 pre-existing examples to compensate"). "We didn't get to it" is not
a justification — phase 6 is mandatory.

The QA page diff ships with the PR so the docs stay truthful.
