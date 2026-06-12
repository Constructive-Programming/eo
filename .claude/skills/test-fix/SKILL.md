---
name: test-fix
description: Alias for improve-test-leverage biased to killing currently-surviving mutants and covering uncovered branches — the gap-closing mode. Use when the QA page shows survivors or NoCoverage clusters that should be dead.
model: sonnet
---

# test-fix — alias

Alias for `improve-test-leverage`. Run phases 1-5 of
`.claude/skills/improve-test-leverage/` with this bias: candidates ranked by
**measured gaps** (Survived / NoCoverage mutants, uncovered branches from the
current reports), any ladder stage; every candidate must name the existing
surviving mutants it kills. CLASSIFY-OUT is in scope — documenting an
equivalent mutant IS a fix.

Arguments pass through.

Announce as: "Using improve-test-leverage (test-fix alias)."
