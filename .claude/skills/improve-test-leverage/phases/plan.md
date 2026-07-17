# Phase 3 — Plan + adversarial self-review (model: sonnet)

Design the minimal artifact set from the triage pool, then attack the plan
before anyone else can.

## The plan table — one row per artifact, not per assertion

```
| # | Artifact | Tier/stage | File | Mutants killed (file:line:col) | Why no higher rung |
```

For each row include: generators with their **distribution argument** ("sizes
biased to {0, 1, 15, 16, 17, 48}" — default `Gen.listOf` almost never crosses
capacity thresholds), the invariant or law statement, and the causal chain
per claimed kill (mutant flips X → assertion Y fails on input class Z).

## Self-review checklist (each row must survive all five)

1. Any tier-4/5 row that restates an existing law → replace with a tier-1
   registration of that law.
2. Any row whose generator can't actually reach the mutated branch — the
   most common measured failure (thresholds, capacity boundaries).
3. Negative fixtures must be *minimally* unlawful — break exactly the clause
   the mutant weakens, keep all else lawful, or the fixture can't
   distinguish weakened from original.
4. Each claimed kill names the assertion that fails under the mutant.
5. **The reduction question (new — the one early runs skipped): does this
   artifact make any EXISTING test redundant?** If a new property subsumes
   examples, the plan row lists the examples to delete and phase 6 deletes
   them. Adding without subsuming is the exception, not the rule.

In guided mode, present the plan to the user after self-review (this is the
phase 3→4 gate). In loop mode, the pool + budget were confirmed once
up-front; proceed.
