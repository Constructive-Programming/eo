# Phase 2 — Triage (model: sonnet)

Sort every Survived / NoCoverage mutant into the leverage tiers and decide
placement. For each mutant the question is not "what test kills this?" but
"what is the most algebraic artifact that kills this *and its neighbors*?" —
i.e. the highest reachable rung of the promotion ladder.

For each mutant collect: mutator type, file:line:column, original vs
replacement, and enough surrounding source to judge what behaviour changed.
Read the source — measured misses in past runs came from guessing which
conditional a line number referred to.

| Tier | Artifact | Ladder stage | Kill profile | When |
|---|---|---|---|---|
| 0 | **Law promotion** (user gate — phases/law-scan.md) | 3 | one law × EVERY checkAll site repo-wide + clients | candidate property is universal for an abstraction |
| 1 | **Ruleset registration** — `checkAll` via `CheckAllHelpers` against an uncovered instance/carrier/arity | 2 | one line ≈ 5–15 properties | the mutant sits on a path existing laws exercise, just not for this instance shape |
| 2 | **Composed-optic law registration** — laws on `outer.andThen(inner)` for an uncovered family pair | 2 | kills in BOTH families + compose machinery | mutant in compose/carrier dispatch, reachable only through composition |
| 3 | **Negative fixture** — minimally unlawful instance asserted to FAIL a specific law | — | the ONLY killer of law-WEAKENING mutants (guard→false, &&→ǀǀ) in `laws/` | every lawful instance satisfies the weakened law too |
| 4 | **Property** (`forAll`) | 1 | one property ≈ N examples | machinery laws don't reach; generators must STRADDLE thresholds (capacity 16, OnStackLimit, transformRecursionLimit 512) or the mutated branch never executes |
| 5 | **Example** | 0 | one kill each — the floor | fixed constants, single semantically-distinct inputs, dead contract corners |

Every tier-5 plan entry must carry a one-line answer to "why is no higher
rung reachable?" — tier 5 is permitted, defaulting to it is not.

## Also mark

- **Equivalent mutants** (with the reason a reviewer can check): early/extra
  grow = alloc-only; copy-vs-share fast paths; `i >= n` → `i == n` when the
  index increments by 1; routing conditionals between semantically identical
  paths; hashCode mutants applied symmetrically to both comparands.
- **Wrong-module reach**: a core mutant whose only natural exercise is a
  schemes/circe path may be left to aggregate scoverage — note it rather
  than forcing an artificial test.
- **Replaced-soon code**: survivors in code an open PR rewrites (check the
  branch list) — park, don't invest.

## Placement rule

- Mutant in `core` or `laws` → artifact lives in `tests/` (borrowed into
  both mutation runs; one artifact scores twice).
- Mutant in `schemes` / `circe` / `jsoniter` → that module's own `src/test`
  (its stryker run sees nothing else).
- Never duplicate an exercise across placements; choose the larger blast
  radius.
