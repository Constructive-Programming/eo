---
date: 2026-06-10
topic: failure-typed-build-biaffine
spike: open (collaborative — design space, not a decision)
---

# Failure-typed build: generalizing `Affine` to miss both ways

## Problem Frame

Every eo optic's `from` is **total**: `from: F[X, B] => T` always produces a `T`. That encodes a
real assumption — *writing back never fails*. It holds for Lens (copy), Iso (bijection), Prism
(`mend` is total — partiality is read-only), Optional, Setter.

It stops holding the moment a focus has an **invariant the write must respect**:

- Smart constructors: `Age.from(Int): Either[Err, Age]`. Today eo models this as a `Prism`
  (`getOption` is the fallible direction, `mend: Age => Int` the total one) — fine when the failure
  is naturally a *read*. But "I have an `Int`, write it into the `Age` slot, and that can fail" is a
  **fallible build**, and eo has no optic whose `from` can say no.
- The integrations already gesture at this. `circe`/`avro`/`jsoniter` decode through an
  `Ior[Chain[Failure], A]` channel (`JsonFailure`, `AvroFailure`, `parseInputIor`,
  `AvroTraversal`'s per-element `Ior.Both` accumulation). Decode = read; but **encode/splice can
  also fail or partially fail**, and the current `from: … => Array[Byte] | Json | IndexedRecord` has
  nowhere to put that — it throws or silently passes through.

`Affine` is the natural place to generalize, because it's already the carrier that admits a *miss*:

```
Affine[X, A] = Miss(fst: Fst[X])            -- read missed; carry leftover for pass-through
             | Hit (snd: Snd[X], b: A)       -- read hit; carry focus + leftover
to   : S => Affine[X, A]      -- the read may Miss
from : Affine[X, B] => T      -- TOTAL: Miss passes the leftover through, Hit rebuilds
```

The miss lives **only on the read** (in `to`). The ask: a carrier where the **build can miss too**.

## What "miss both ways" could mean (the design space)

Three distinct things hide under "fallible build" — worth separating before encoding:

1. **Build-rejects** — `from` returns "this `B` can't be written here" (invariant violated). Short-
   circuit, `Either[E, T]`-shaped.
2. **Build-accumulates** — a multi-focus write where *some* elements fail; keep the successes,
   collect the failures. `Ior[E, T]`-shaped. This is **exactly what `AvroTraversal` already does on
   read** — the spike would make it symmetric on write.
3. **Build-from-nothing** — the `ana`/corecursion case: build can choose to *stop* (produce a leaf
   rather than recurse). That's a different "miss" (a coalgebra returning the base case), arguably
   the [[2026-06-10-unfold-build-many-citizen]] spike's territory, not this one.

This spike targets **(1) and (2)**: a focus type whose write-back is partial, with an error channel.

## Candidate encodings (react to these)

**Candidate A — error channel in the focus.** Leave `Affine` alone; make the focus `Either[E, A]`
or `Ior[E, A]`. The optic is an ordinary `Optional[S, T, Either[E,A], Either[E,B]]`. *Pro:* zero new
carrier; reuses everything. *Con:* the failure is data, not structure — `getOption`/`modify` don't
*know* it's a failure, so laws and short-circuiting aren't enforced; every consumer re-implements
"is this a real focus or an error." Probably too weak.

**Candidate B — `BiAffine`: a four-constructor carrier.** Make miss a first-class outcome on both
sides:

```
BiAffine[X, A] = ReadMiss (fst: Fst[X])           -- to    produced no focus
               | Hit      (snd: Snd[X], b: A)      -- focus present
               | WriteMiss(err: E,  fst: Fst[X])   -- from   rejected the B   (NEW)
to   : S => BiAffine[X, A]               -- ReadMiss | Hit
from : BiAffine[X, B] => Either[E, T]    -- Hit may become WriteMiss → Left(E)
```

*Pro:* the failure is structural; composition can thread it; laws can be stated (`getOption` ⊥ on
ReadMiss, `replace` may ⊥ on WriteMiss). *Con:* `from`'s return type changes shape (`Either[E,T]`,
not `T`) — touches the `Optic` trait's `from` signature or needs a carrier-local escape. The
`E`/error type has to live *somewhere* (carrier param? focus-adjacent? a fixed `Chain[Failure]`?).

**Candidate C — `Validated`/`Ior` carrier (accumulating).** Generalize the carrier's *summary* type
from `Affine` (0-or-1) to `Ior[E, ·]` so both `to` and `from` carry an accumulating error channel —
the per-element-failure shape `AvroTraversal` already runs on read, made into a reusable carrier and
extended to write. *Pro:* directly matches the integration story (the `Ior` channel is already
there); subsumes the traversal-with-partial-failures case. *Con:* the most invasive; interacts with
`MultiFocus` (many foci, each can fail each way).

## The hard questions (where I want your steer)

- **Q1 — does `from`'s type change, or do we keep `from: F[X,B] => T` and put failure in `T`/`F`?**
  Changing `Optic.from` to allow a fallible result is the deepest fork in the road: it either
  becomes `from: F[X, B] => T` with failure *encoded in the carrier `F`* (so `T` is morally
  `Either[E, T']` and the carrier knows it — Candidate B/C), or we keep `from` total and the error
  is in the focus (Candidate A). I lean toward "failure in the carrier" — it's the only option where
  the optic *laws* can mention it — but it's the expensive one.

- **Q2 — short-circuit (`Either`) or accumulate (`Ior`)?** The integrations want accumulate (collect
  every bad field). Pure smart-constructor semantics want short-circuit. Is the carrier parametric
  in the error semiring (`E: Semigroup` ⇒ `Ior`), with `Either` the degenerate
  no-accumulation case?

- **Q3 — does this *subsume* `Affine`/`Prism`?** Affine = "WriteMiss uninhabited (`E = Nothing`)".
  Prism = "read-miss only, build total". A clean generalization would let both fall out as the
  `E = Nothing` / write-total specializations — the same way `AffineFold` is `Optional` with
  `T=B=Unit`. If the encoding *doesn't* recover them cleanly, that's a smell.

- **Q4 — where does it sit in the family lattice?** It's a *new axis*: not read-capability
  (read/build/both) but **fallibility** (which directions can fail). Does the two-axis table become
  three-axis (focus-shape × capability × fallibility), and if so does every existing cell get a
  fallible variant, or only the partial row?

- **Q5 — laws.** What replaces the Optional round-trips when build can fail? Candidate:
  `getOption(s) = Some(a)` ⟹ `replace(b)(s)` either succeeds-and-round-trips or fails-with-`E`
  (never silently no-ops). The "fail loud" property is the whole point.

## Concrete first step (proposed — your call)

Stand up **Candidate B (`BiAffine`)** as a throwaway carrier in a branch, with `E` fixed to
`Chain[String]` (defer the parametric-error question), and try to express **one real case**:
`circe`'s street-write where the codec encode can fail. See what breaks at the `Optic.from`
signature seam — that single friction point (does `from` stay total?) is the decision the whole
spike turns on, and it's cheap to surface. Then bring it back here and decide A vs B vs C together.

This is the one you wanted to drive — so I've left A/B/C and Q1–Q5 open rather than picking. Tell me
which candidate to prototype first (my vote: B, fixed-error, one circe case) and whether `from`
changing shape is on or off the table.

## Scope addendum (2026-06-11, from the 3-axis taxonomy work)

The taxonomy figure on the docsite (read cardinality × write cardinality × capability —
`site/docs/optics.md`, generated by `site/tools/gen-taxonomy-svg.py`) places this spike's
deliverables precisely, and two items are hereby pulled INTO this plan's scope:

1. **`WriteCompose`** — the write-side join typeclass mirroring `ReadCompose`: composing
   write/build sides should land at the join of their write cardinalities (1 / 0-or-1 / N),
   the way `ReadCompose` lands read-only chains at the join of read strengths. The fallible
   (0-or-1) rung of that lattice is exactly this spike's error channel, so the typeclass and
   the carrier should be designed together.
2. **Missing-cell carriers** — the taxonomy's empty cells are write-cardinality gaps:
   - middle-layer (read-write) write-0-or-1 column: *fallible writes* (smart-constructor
     replace, per-element encode failure) — Candidates B/C;
   - bottom-layer (build-only) 0-or-1 cell: *fallible build* — the `Either[E, T]`-shaped
     `from`;
   - the off-diagonal ∅ cells (read-N × write-1, read-1 × write-N, read-0/1 × write-N) need
     carriers whose write cardinality differs from their read cardinality — candidates should
     fall out of the same carrier generalization rather than be designed ad hoc.
