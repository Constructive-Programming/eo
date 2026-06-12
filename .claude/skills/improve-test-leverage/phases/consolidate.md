# Phase 6 — Consolidate (model: opus — MANDATORY, every run)

The phase the first two runs skipped, and the reason the suite grew when the
user expected it to shrink. CONSOLIDATE cannot win a kills-per-line ranking
(its kills are zero by definition), so it must never compete with ADDs — it
runs unconditionally at the end of every run, in every mode, with the
strongest available model: proving that a property subsumes a set of examples
*without kill regression* is the hardest reasoning in this skill.

**The success criterion is dramatic, not cosmetic.** This repo has a
consolidation culture (`InternalsCoverageSpec` records 25→9 and 9→5 named
blocks; `JsonFailureSpec` 9→5→1) — match it. A run that added K examples and
deletes fewer than K has failed this phase unless the report justifies it.

## Procedure

1. **Inventory** every example/property in the affected test files (in a full
   run: the whole suite, largest files first — `OpticsBehaviorSpec` at ~50K is
   the standing target). For each: the method/path under test, ladder stage,
   and the mutants its assertions plausibly kill (from the latest reports).
2. **Find the folds**, in order of payoff:
   - **N examples on one code path → 1 property.** The generated scenario can
     carry both input and expected outcome (`case class Scenario(input, expected)`
     via `Gen.oneOf` over the categories) when categories are finite; when
     the assertion is a universal invariant, the invariant IS the property —
     don't force scenario encoding.
   - **A property that restates an existing law → delete it, register the
     ruleset** (stage 1 → stage 2; one `checkAll` line).
   - **Examples that are specific cases of an existing property → delete.**
   - **Two properties asserting facets of one operation → merge.**
   - **Same-shape assertions repeated across instance types → one
     parameterized helper, or escalate to phase 7 as a law candidate.**
3. **Keep out of scope**: negative fixtures (one per weakened clause — they
   are documentation); `// covers:` audit comments (they are why the
   denominator excludes comments).
4. **Eval — zero kill regression, measured**: after rewriting, re-run
   mutation for every affected module (borrow sets for core/laws). ANY
   mutant that flips Killed→Survived means a deleted test was the sole
   killer — restore that exercise before proceeding. Then `sbt test` green,
   `sbt "scalafixAll; scalafmtAll"`.
5. **Record**: per fold — examples deleted, property created, net lines
   (negative), kill regression check result. Update `suite_test_count` /
   `suite_test_loc` in state. Promotions feed the phase 8 report's
   promotion summary.

## Verdict

Consolidation accepts on `net_test_lines_added < 0` with zero measured kill
regression. There is no lines/kill ratio here — the leverage is the
denominator shrinking while kills hold.
