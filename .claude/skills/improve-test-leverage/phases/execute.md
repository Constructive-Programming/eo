# Phase 4 — Execute (model: sonnet)

Write the planned artifacts under repo conventions. Read a neighbor spec
first; these are the load-bearing rules:

- **Framework**: specs2 `mutable.Specification`; law registrations via
  `CheckAllHelpers` (mixes in Discipline; helpers return `Fragment` to
  silence `-Wnonunit-statement`). `// covers: <file:line mutant>` comment at
  every artifact — the established audit convention.
- **Generators**: `Arbitrary`/`Cogen` given instances near the spec (see
  `OpticsLawsSpec`); shared fixtures in `Samples.scala` / `examples/` /
  `JsonSpecFixtures`-style objects. Distributions must straddle the
  documented thresholds (builder capacity 16, `transformRecursionLimit` 512,
  `OnStackLimit`).
- **Negative fixtures**: instantiate the law trait directly with the broken
  instance and assert the law method returns `false` on a pinned witness
  input — never register negative fixtures through `checkAll` (Discipline
  expects laws to pass). A *stateful* `to` is a legitimate way to be
  unlawful (purity is an implicit law). Generic-interface corruption can use
  `null.asInstanceOf` / call-counting when the type system forbids honest
  corruption — document why.
- **specs2 implicit pollution**: inside `Prop.forAll`, bind operands to
  typed vals before `==`, and avoid `.flatten`/`.sum` chains whose implicit
  evidence search collides with specs2's `ValueCheck` conversions
  (`xs.map(_.sum).sum` instead of `xs.flatten.sum`).
- **No mocks** anywhere; everything is pure data. Top-level test ADTs when
  macros are involved (`new T(...)` loses outer accessors in nested classes).
- Format before committing: `sbt "scalafixAll; scalafmtAll"` (the sbt form,
  NOT the CLI scalafmt — they diverge; pre-commit gates on the sbt form).
- One candidate per commit, message pattern:
  `test(<module>): <candidate> — kills N mutants in M lines`.

Deletions planned by phase 3's reduction question are executed here too —
deleting subsumed examples is part of the artifact, not a separate favor.
