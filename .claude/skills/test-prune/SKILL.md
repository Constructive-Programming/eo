---
name: test-prune
description: Alias for improve-test-leverage running ONLY the consolidation pipeline — shrink test count and LOC while holding every kill. Replaces example clusters with properties, deletes tests that restate laws, merges redundant properties.
model: opus
---

# test-prune — alias

Alias for `improve-test-leverage`. Run phases 1, 6 and 8 of
`.claude/skills/improve-test-leverage/` (reports → **consolidate** → report):
pure reduction. The whole point is phase 6
(`phases/consolidate.md`) — N examples to one property, properties to
ruleset registrations, deletion of law-restating tests — with its
zero-kill-regression eval. Acceptance: `net_test_lines_added < 0` and
`suite_test_count` strictly reduced, kills held (measured, not assumed).
Law candidates that surface during folding are queued for phase 7's user
gate.

Arguments pass through (module focus narrows the inventory).

Announce as: "Using improve-test-leverage (test-prune alias) — pure
reduction."
