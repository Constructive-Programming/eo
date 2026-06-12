---
name: test-prune
description: Alias for improve-test-leverage biased to CONSOLIDATE moves — shrink test LOC while holding every kill. Replaces example clusters with laws/properties, deletes tests that restate laws, merges redundant properties.
model: sonnet
---

# test-prune — alias

This is an alias for the `improve-test-leverage` skill. Read and follow
`.claude/skills/improve-test-leverage/SKILL.md` in full, with this mode
bias pre-set:

- **Candidate moves**: CONSOLIDATE only (step 9's rules — no test may
  restate what a law already says; >1 example on one code path becomes
  one property; merge same-operation properties). The accept criterion
  is the consolidation one: `net_test_lines_added < 0` with **zero kill
  regression**, measured by re-running the affected mutation targets —
  never assumed.
- No new artifacts; no CLASSIFY-OUT.
- Arguments after `/test-prune` are passed through unchanged.

Announce as: "Using improve-test-leverage (test-prune alias) — shrinking
test LOC at held kill capacity."
