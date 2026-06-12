---
name: test-fix
description: Alias for improve-test-leverage biased to killing the currently-surviving mutants and covering uncovered branches — the gap-closing mode. Use when the QA page shows survivors or NoCoverage clusters that should be dead.
model: sonnet
---

# test-fix — alias

This is an alias for the `improve-test-leverage` skill. Read and follow
`.claude/skills/improve-test-leverage/SKILL.md` in full, with this mode
bias pre-set:

- **Target selection**: rank candidates by *measured gaps* — Survived
  and NoCoverage mutants and uncovered branches from the current
  reports — rather than by abstract tier ambition. Any tier is fair
  game, but every candidate must name the existing surviving mutants it
  kills; do not add artifacts that only deepen already-killed paths.
- CLASSIFY-OUT is in scope: a gap that turns out to be an equivalent
  mutant or wrong-module reach is *fixed* by documenting it.
- Arguments after `/test-fix` are passed through unchanged.

Announce as: "Using improve-test-leverage (test-fix alias) — closing
measured survivor/coverage gaps."
