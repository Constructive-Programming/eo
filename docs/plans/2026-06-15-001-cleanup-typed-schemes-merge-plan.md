---
title: "cleanup: typed recursion schemes merge-readiness (bibliography + rough edges)"
type: cleanup
status: in-progress (C1, C3, C5 done 2026-06-15; C8 found during C1)
date: 2026-06-15
origin: thread request (kryptt): read the anchor paper, build the bibliography,
  plan the cleanup so PR #24 can merge
---

# cleanup: typed recursion schemes merge-readiness

## State of the branch (2026-06-15)

`feat/typed-recursion-schemes` (49 commits over `origin/main`, PR #24) ships the
typed zoo as existential-indexed optics: the `BiAffine` carrier in core,
`Attr`/`Coattr` decorations, the `Schemes` citizens (cata/para/histo/zygo/mutu,
ana/apo/futu/cozygo/comutu, fused hylo/dyna/codyna/chrono/elgot/coelgot, meta/
metaChrono, prepro/postpro, the M-family), `paraLens`, the `Plated`↔`Basis`
bridge, and the `BiAffine.assoc` matrix row. Tests pass on both JDKs; the plan's
stages 1–7 are implemented; the two open brainstorm spikes
(`elgot-seam-sketch` — PASS, `existential-x-is-the-decoration` — substrate
landed, follow-ups listed) are recorded.

**CI status: Test ✅ / Generate Site ❌ / Cloudflare Pages preview ❌.** The merge
blocker is the docs build, plus a short list of understood rough edges below.

## The anchor paper (what the branch is anchored to)

The requested paper, `arXiv:1103.2841`, is **O'Connor, "Functor is to Lens as
Applicative is to Biplate: Introducing Multiplate" (WGP 2011)** — a lens/plate
paper, not a recursion-schemes paper. That is not a mismatch to fix but the
second half of the branch's thesis: it categorically certifies the *optics half*
of "recursion schemes are optics" —

- lens = coalgebra of the store comonad (§2.2): the formal ground for
  `paraLens` (a para's retained subterms are the store's complement);
- biplate = coalgebra of the Cartesian store comonad (§3): the formal ground for
  `Plated.plate`/`Schemes.fLayer` (the one-layer typed self-traversal);
- the van-Laarhoven isomorphism (§4, via Wadler's free theorems): the polymorphic
  and coalgebraic presentations of the same optic coincide — the branch's
  "two readings of the same fact at the X seam" is this theorem in eo's encoding.

The full bibliography (anchor, its relevant references, its relevant citations,
the recursion-schemes canon, the Scala-ecosystem implementations) is in
[`docs/research/2026-06-15-typed-schemes-bibliography.md`](../research/2026-06-15-typed-schemes-bibliography.md).
The paper the zoo's *schemes* come from is Hinze–Wu–Gibbons' *Unifying
structured recursion schemes* (ICFP 2013) and Uustalu–Vene–Pardo's *Recursion
schemes from comonads* (2001) — both already cited in the plan docs; the
bibliography consolidates them.

## Cleanup items (merge-blocking first)

### C1. ✅ DONE (2026-06-15). Fix `site/docs/schemes.md` — it taught a retired API (blocked `docs/mdoc`, CI red)

`mdoc` reports 10 errors, all API drift between the doc and the shipped surface:

| lines | doc teaches | shipped reality |
|---|---|---|
| 71–102, 248–251, 278–282, 308–311 | node-supplied algebras `(node, folded) => …` (2-arg lambda) | **1-arg node-blind algebras** `F[A] => A` (the late refactor, `Schemes.scala` scaladocs already correct) |
| 258–282 | user-written zygo via a `zoo.Gather` type, `cata[BinF, Bin, (Int, Int), Int](zygo(...))` (4 type args) | **no `Gather`/`Scatter` public type**; `zygo` is a named constructor: `Schemes.zygo(aux)(alg)` |
| 251 | `Schemes.cata(zooSum)` where `zooSum: (Bin, BinF[Int]) => Int` | same 1-arg fix; `ana.cross(cata)` then typechecks against `DirectGetter` |

Action: rewrite the four affected sections against the current API (keep every
claim already scoped in D7); re-run `sbt docs/mdoc` until clean; that also turns
the two site workflows green. The `migration-from-monocle.md`/`optics.md`
"Unknown link 'schemes.md'" warnings should disappear with the same fix (the
link target exists; the warnings pre-date and are informational).

### C2. Point the plan docs' reference section at the bibliography

`docs/plans/2026-06-11-001`'s References section (and the 2026-06-09-002 plan, if
touched) gets one line pointing at the new bibliography file, so the branch's
citations live in exactly one place. No claims change.

### C3. ✅ DONE (kryptt's call: delete; recreate when D5 law specs get a module). `schemes-laws/` was an empty untracked directory tree

`git ls-files schemes-laws` is empty; the directory exists on disk with no
sources. Either (a) delete it, or (b) if the plan's D5 law specs
(`SchemesFLawsSpec`-style discipline suites) were meant to live there, move the
law-heavy specs out of `schemes/src/test` into it as the module skeleton.
Recommendation: (b)-lite — leave `laws/` as the discipline home (it already
hosts the BiAffine/graft laws per commit 07a461fe) and delete `schemes-laws/`;
two law modules is one too many. Needs kryptt's call since the directory is
referenced nowhere.

### C4. CHANGELOG section for the schemes work

`CHANGELOG.md` has no mention of the schemes module (the branch changes the
public surface: new `schemes` artifact, new core `BiAffine`/`Graft`/`Basis`).
Add the 0.1.x section entries per the repo's changelog conventions before merge,
so the release notes don't get written from memory later.

### C5. ✅ DONE with C1. Doc/code contradiction: "referenced nowhere" claims in `schemes.md`

`schemes.md` says "the named values dispatch to native engine routes" and
describes `Gather/Scatter` as public — both stale vs. the concrete-citizen
design (`zoo/*.scala` classes + `Schemes` constructors). After C1, re-read the
page top-to-bottom as a reviewer would: every sentence must match
`Schemes.scala`/`Machines.scala` scaladocs (the scaladocs are already
consistent — they were updated in the refactor commits; the page was not).

### C6. PR description refresh

PR #24's body still describes the U6 Eval-era decisions and the old
`cataF`/`anaF`/`hyloF` names; the branch has since rebased onto main's renamed
surface (`cata`/`ana`/`hylo` typed path) and grown the zoo, `paraLens`, the
M-family re-carrier, and the BiAffine bridges. Rewrite the description as:
thesis (schemes as optics indexed by their existential X), what ships, the
fused-vs-materializing law, benchmark deltas vs droste, and the follow-ups
(elgot port per the PASSed seam sketch; the X-existential spike items;
BiAffine matrix row). Link the bibliography for reviewers who want the papers.

### C7. (non-blocking) `benchmarks` numbers in docs

`site/docs/benchmarks.md` carries the CI-swept numbers; re-run the JMH sweep
once after C1 so the "before/after pin" rows reflect the final merged state
(the plan's merge-gate pins: `cataF`/`hyloF` before/after, ana-gap-not-worsened,
fusion no-intermediate-S). The pins passed in CI at 56685dfa; re-confirm at the
merge candidate commit.

### C8. (new, found during C1) `Getter.andThen` is 3-way ambiguous for Direct-carried scheme citizens

`site/docs/schemes.md`'s lens-composition example — `Getter[Doc, Bin](deepTree.get).andThen(cata)`
— fails to compile: for a Direct-carried citizen (`Optic[A, Unit, C, Unit, Direct]`), **three**
`Getter` overloads all apply — `andThenReadAny` (any inner carrier), the re-homed read-only-inner
override (`inner.T = Unit`), and the trait's same-carrier inline (`outer.F = inner.F = Direct`) —
and dotty calls it a tie. The doc now teaches the unambiguous function-composition spelling, but
core should decide: re-home or drop one of the three (the repo's "overload-set discipline" per the
`Getter` precedent), and pin resolution with a spec. Until then, `getter.andThen(schemeCitizen)`
is a compile-error trap for users.

## Sequencing

1. C1 (unblocks CI, the only red gate) → 2. C5 (same file, one review) →
3. C2, C4 (mechanical) → 4. C6 (after code review settles) → 5. C3 (one-line
decision, needs kryptt) → 6. C7 (last, at the merge candidate).

## Explicitly out of scope (already-triaged follow-ups, not merge blockers)

- elgot/coelgot `Decor` values + `Calculator.selection` port (seam sketch PASSed;
  additive follow-up per decision 11).
- The existential-X spike items (para-as-Lens beyond `paraLens`, memoized
  refolds, honest hylo X-parameter, BiAffine matrix row 12→13).
- Persistent-state M-engine for non-linear Ms; Accessor-into-M capability;
  cats-free interop.
