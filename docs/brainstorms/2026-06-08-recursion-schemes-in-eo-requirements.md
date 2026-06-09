---
date: 2026-06-08
topic: recursion-schemes-in-eo
revised: 2026-06-08 (post document-review)
---

# Recursion Schemes in eo

## Problem Frame

eo describes pinpointed read/write on nested structures. `Plated` is where
recursion first appears — `transform`/`rewrite`/`children`/`universe` over a
homogeneous recursive `S`. The recursion-schemes zoo (cata/ana/hylo/para/apo/…)
is a much richer vocabulary for *how* to navigate, generate, and destroy
recursive structure. The question this brainstorm answers: where do those
schemes fit in eo, and what do users gain?

The structural finding reframes the space. `Plated.plate :
Optic[S,S,S,S,MultiFocus[PSVec]]` — with `to: S => (S, PSVec[S])` and
`from: (S, PSVec[S]) => S` — **can be read as `project` + `embed`.** (No
`Recursive`/`Corecursive` types exist in the code — the structure is *latent* in
`Plated`, not named.) `transform` is a bottom-up rewrite hardcoded to `S => S` —
cata-*shaped*, but **not** a true catamorphism, which folds to an arbitrary `A`.

So the navigate/generate/destroy triangle is less complete than it first looks:

| Verb | eo today | Gap |
|---|---|---|
| **navigate** (children / descendants) | `children`, `universe` | — complete |
| **destroy** (fold / rewrite) | `transform` (`S=>S`), `rewrite` (fixpoint) | **no fold-to-arbitrary-`A`** (true cata) |
| **generate** (unfold / build) | — nothing — | **the whole leg** |

`Plated.rebuild` only swaps children into an *existing* node; there is no
`embed`-from-seed. So eo has **navigate, a restricted destroy (`S=>S` only), and
no generate** — closer to *1.5 of 3 legs* than 2. Adding generation is the
chosen center of gravity; the missing fold-to-`A` is a smaller adjacent gap the
generate work forces into the open (see R7 / the hylo-fold gap).

## Why now / who asked  (premise — validate before productionizing)

**No user has logged a request for generation.** The current driver is
conceptual completeness (close the N/G/D triangle) plus the author's interest in
the scheme zoo. That is an honest *bet*, not evidenced demand — so this document
treats "is generation worth shipping?" as an **open go/no-go question the spike
must be allowed to answer "no" to**, not a settled premise.

Candidate use cases to validate (hypotheses, not confirmed needs):
- Synthesize a deep circe `Json` from a seed/spec (fixtures, config trees, templates).
- Build an Avro-shaped nested record from a generator.
- Parse flat input → AST (the `Expr` demo below).

Counter-baseline that must be beaten: **droste/matryoshka already provide
ana/apo/hylo.** For the rare unfold, an "eo + droste interop" docs page (feed
`Plated`'s project/embed into an existing scheme lib) may deliver most of the
value at a fraction of the build/maintenance cost. The spike grades against this
baseline (R8/R9), and "recommend interop, don't build" is a permitted verdict.

## The core tension

Why generation is genuinely harder than destruction — stated precisely (the
earlier "hidden pattern functor in `X`" framing was wrong: verified `X = S`, so
`to` keeps the *whole node*, not a stripped functor):

- **Destruction has a source.** An `S` already exists; eo deconstructs it via
  `to` and reconstructs via `from`. The user never names a node shape — the
  source supplies it.
- **Generation has no source.** Starting from a seed, *someone* must say what
  node to build, and "a node with holes where children go" **is** the pattern
  functor. There is no source to crib it from.

This forces a two-encoding fork the prototype settles:

```
A — closure-carrying (no pattern functor, eo-native)
    ana[Seed,S](coalg: Seed => (PSVec[Seed], PSVec[S] => S)): Seed => S
    user inlines the constructor per node; "skeleton" lives in the closure.
    Note: this is its OWN ceremony — the constructor is re-spelled per call site.

B — derived descriptor ("Coplated")
    ana[Seed,S](coalg: Seed => Descriptor[Seed]): Seed => S
    eo-generics derives the node descriptor; canonical, reusable, fuses hylo
    — but REINTRODUCES the pattern functor eo worked to hide.
```

**Honest consequence for positioning:** "recursion schemes *without* the pattern
functor" survives **only in encoding A**. If B wins, the pitch vs
droste/matryoshka weakens from "no pattern functor" to "derivation/convenience."
The verdict (R9) must state which promise actually survives on the recommended
encoding.

Naming intuition (not a structural law): a recursive *builder* is roughly to
`Review` what `Plated` is to `Fold`/`Setter`. But **`Review` is not an `Optic`**
(it is a bare `case class Review(reverseGet: A => S)` with no `to`), so this
analogy does **not** transfer `andThen`/carrier composition — whether a
`Coplated` should be an `Optic` at all is an open design question, not implied by
the symmetry.

## Requirements

**Structural framing (durable, encoding-independent)**
- R1. Build on what exists — `Plated` (readable as project+embed) and
  `transform` (`S=>S` rewrite) — rather than introducing a parallel
  `Fix`/pattern-functor world. Do **not** assume `Recursive`/`Corecursive` or a
  fold-to-`A` cata exist; they don't.
- R2. Generation is the missing **generate** leg. Treat the `Coplated`/`Review`
  pairing as a naming intuition only; decide independently whether the builder
  is an `Optic`.

**Prototype experiment — staged, so cost tracks the decision (the deliverable)**
- R3. **Stage the spike** so effort follows the verdict rather than front-running it:
  - **Stage 0 (premise gate):** write one *domain* generation example (circe
    `Json` unfold, or Avro record) and sketch the droste-interop baseline. If no
    convincing domain example exists and interop suffices, the verdict may be
    "don't build" — stop here.
  - **Stage 1 (encoding A, no macro):** implement `apo` + `hylo` (the
    discriminating schemes) under closure-encoding A. `ana` is the base case A
    builds on; include `ana` under A but **only carry `ana` into the comparison
    if it changes the verdict**.
  - **Stage 2 (encoding B, hand-written):** hand-write a `Coplated` descriptor
    for the samples (no `eo-generics` macro yet) and compare `apo`/`hylo`
    ergonomics + fusion against A.
  - **Stage 3 (B derivation, gated):** only if hand-written B is competitive on
    the R8 axes, probe the `eo-generics` derivation (R5).
- R4. Showcase types: (a) arithmetic `Expr` enum
  (`Lit(Double) | Neg(Expr) | Add(Expr,Expr) | Mul(Expr,Expr)`) — minimal type
  where the pattern-functor tension bites; **(b) a domain type from eo's actual
  audience** (small circe-`Json`-shaped ADT with an object/array case, or Avro
  record) so the verdict generalizes beyond a toy. R9 must report ergonomics on
  the domain type, not only `Expr`.
- R5. Derivation probe (Stage 3 only): target the `Tree[N]` sample in
  `generics/src/test/scala/dev/constructive/eo/generics/samples/`. **Note two
  blockers to resolve first:** the sample is *test-only* (a `generics/src/main`
  macro can't see the test classpath — move it or add a main-side sample), and
  all its cases carry fields (won't exercise the parameterless-variant path).
  Prefer reusing `Expr` as the probe if it can double for `Tree[N]`.
- R6. `apo` demonstrates structure-sharing generation (graft pre-built
  subtrees). ("generative dual of `rewrite`" is a loose analogy, not a claim.)
- R7. `hylo` demonstrates fusion — `ana`∘`cata` with no materialized
  intermediate tree. **Blocker:** the "evaluate to `Double`" fold needs a
  fold-to-`A` cata, which does **not** exist (`transform` is `S=>S`). Pull a
  *minimal* `cata[S,A]` (generalize `transform`'s existing stack-safe machine to
  an arbitrary result) into prototype scope — it is small and R7 depends on it.
  ("no materialized tree" is verified by allocation profiling, not asserted.)

**Verdict criteria (how "then decide" is graded)**
- R8. Grade on: ergonomics (LOC / per-call-site ceremony — including A's
  per-node closures vs B's descriptor vs droste's pattern functor); which
  no-pattern-functor promise actually survives; derivability via `eo-generics`;
  **stack-safety of *fused* `hylo` at depth 10⁶** (see below); hylo-fusion cost;
  allocation (B/op — trustworthy here; ns is not; EA may elide B's descriptor so
  the verdict may need CI ns). **Plus a go/no-go axis:** head-to-head vs
  droste-interop on the domain task — would a circe/Avro user switch?
- R9. Written verdict answering **both** questions: (1) should this ship at all
  (vs droste interop), and (2) if so, which encoding is primary and whether the
  other earns a place (e.g. closure base + derived opt-in) — and which
  positioning promise that choice preserves.

## Success Criteria
- A table comparing A vs B (and the interop baseline) across the R8 axes, ending
  in a clear go/no-go + primary-encoding recommendation (R9).
- *Fused* `hylo` proven stack-safe at depth 10⁶ (empirically). For
  *materializing* `ana`, the limit is O(depth) output heap regardless of stack
  discipline — verify only that the unfold loop doesn't recurse on the JVM stack
  (a smaller depth suffices); 10⁶ is the *fusion* bar, not the materialize bar.
- A working `hylo` "evaluate an expression" demo (with the minimal fold-to-`A`
  from R7) that never materializes the tree.

## Scope Boundaries
- **In scope, newly:** a minimal fold-to-`A` `cata` (R7 needs it) — promoted
  from the read-side follow-on list because the headline demo depends on it.
- **Not** building the rest of the exotic tail (`para`, `histo`, `futu`, `zygo`,
  `chrono`) now — deferred until the encoding is settled and demand is shown.
- **Not** building the `eo-generics` Coplated *derivation macro* until Stage 3 is
  reached (verdict-gated). Hand-written descriptors carry Stages 1–2.
- **Not** competing with droste/matryoshka as a general framework; the interop
  baseline is a partner, not just a rival.
- Remaining Tier-1 read-side wins (`para`, lift `universe`→ a composable `Fold`
  optic / `cosmos`) stay follow-on.
- Spike to settle go/no-go + encoding; productionizing (module placement, laws,
  docs, benchmarks) is downstream.

## Key Decisions
- **Lead with generate, but gate it.** Generation is the missing leg, but with no
  logged demand the spike must be allowed to conclude "don't build, recommend
  interop." Premise honesty over symmetry.
- **Decide by prototype, not theory** — consistent with the project's "measure
  stack-safety/alloc, don't assert" bar.
- **Stage cost to track the decision.** Encoding A first (no macro); hand-written
  B next; B's derivation macro only if B is already competitive. Avoid building
  the expensive option to its hardest point before learning whether it wins.
- **Discriminate with `apo`+`hylo`.** They pull the encodings apart; `ana` rides
  along only if it changes the verdict.
- **Premise is an acknowledged conceptual/learning bet** (confirmed 2026-06-08):
  no logged downstream need or user ask. The Stage 0 go/no-go gate therefore
  stands at full strength — the spike may conclude "recommend droste interop,
  don't build" and that is a success, not a failure.

## Outstanding Questions

### Deferred to Planning
- [Affects R7][Technical] Scope of the minimal `cata[S,A]`: reuse `transform`'s
  ArrayDeque machine generalized to a result type, or a fresh fold?
- [Affects R3][Technical] Will the new unfold trampoline live on `cats.Eval`
  (like `rewrite`) or a hand-rolled heap machine (like `transform`)? Generation
  reuses **none** of the existing destroy-side machinery — it is net-new.
- [Affects R3][Technical] Where do the schemes live — `core` alongside `Plated`,
  or a new slice? (If they need `eo-generics`, a `core`-resident API inverts the
  current `generics → core` dependency direction.)
- [Affects R5][Technical] How far can `eo-generics` derive encoding B for
  multi-constructor enums vs only homogeneous-shape types?
- [Affects R9][Needs research] If both encodings survive, cleanest layering so
  closure-`ana` and derived-`Coplated` share machinery.

### Resolved by the prototype itself
- Go/no-go (ship vs interop) and which encoding (A vs B) is primary — R8/R9 are
  the instrument; no separate decision needed before building.

## Next Steps
With the premise question above resolved, → `/ce:plan` to turn the staged
experiment (R3–R9) into a concrete spike plan.
