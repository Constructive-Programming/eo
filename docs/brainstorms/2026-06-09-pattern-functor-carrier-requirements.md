---
date: 2026-06-09
topic: pattern-functor-carrier
spike: resolved (G1-G4 settled by the 2026-06-09 carrier-fit spike; see Spike Findings)
---

# Pattern-functor as an optic carrier for recursion schemes (opt-in, typed)

## Problem Frame

The recursion schemes shipped in PR #23 (`Schemes.cata/ana/hylo`, Plated-driven, `PSVec[AnyRef]`
heap-stack engine) are stack-safe and compose as optics, but their children are **erased**
(`PSVec[AnyRef]`, positional indexing → `IndexOutOfBounds` / silent arity drops). For users who want
**named-constructor type-safety** on a recursion scheme, eo offers nothing typed today. droste is
typed but its *basic* schemes are **stack-unsafe** (`kernel.hylo` is naive recursion) and don't
compose as optics.

The opportunity: an **opt-in typed path** where the user supplies a pattern functor `F` and eo wraps
it as an optic carrier (`Forget[F]`), driving a stack-safe scheme engine over it. droste's
`Scatter`/`Gather` are exactly eo's Optic `to`/`from`, so the typed path is *"droste, but stack-safe
and optic-composable."* This **complements** #23 (which stays the zero-boilerplate default); it does
not replace or subsume it.

## The unifying insight (spike-confirmed)

Every droste scheme is `kernel.hylo(gather, scatter)`:

| droste | shape | eo optic |
|---|---|---|
| `Scatter[F,A,S]` | `S => Either[A, F[S]]` — expand a seed into one layer | `Optic.to` (project) |
| `Gather[F,S,A]` | `(A, F[S]) => S` — collapse a folded layer | `Optic.from` (embed) |

A pattern functor `F[_]` wears eo's **existing** `Forget[F] = [X, A] =>> F[A]` carrier (the phantom-X
"1-arg-functor-as-2-arg-carrier" encoding), so `project: S => F[S]` and `embed: F[S] => S` form an
`Optic[S, S, S, S, Forget[F]]` **with no change to the `Optic` trait** (spike-proven). The carrier
optic is one *layer*; the recursive *scheme* is a separate stack-safe driver over `to`/`from`
(spike-proven: a typed, `Eval`-trampolined cata folds a 10⁵-deep spine).

| | type-safe children | composes as optic | stack-safe | derive? |
|---|---|---|---|---|
| droste basic | ✓ | ✗ | ✗ | `Fix[F]` free |
| eo #23 (PSVec) | ✗ (erased) | ✓ | ✓ | `plate[S]` derived |
| **typed F path (this)** | **✓ (named ctors)** | **✓** | **✓** | user writes `F` |

## Requirements

**Carrier & engine**
- R1. The typed path uses the **existing `Forget[F]` carrier** for a user-supplied pattern functor
  `F`; `project`/`embed` form an `Optic[S, S, S, S, Forget[F]]`. No change to the `Optic` trait.
  *(spike-proven)*
- R2. A stack-safe scheme driver over the carrier's `to`/`from` provides `cataF`/`anaF`/`hyloF`.
  Stack-safe to 10⁶ (`Eval`-trampolined or an explicit heap walk — planning to choose; spike used
  `Eval`).
- R3. The driver is **type-safe**: it pattern-matches `F`'s typed constructors (no `PSVec[AnyRef]`,
  no positional indexing) — an arity mismatch is a compile error / structurally impossible.

**API (complement, two independent paths)**
- R4. #23's Plated-driven `Schemes.cata/ana/hylo` (positional `(S, PSVec[A]) => A`, zero-boilerplate)
  stays **unchanged** — the default path. *Not* subsumed.
- R5. New opt-in typed path: `Schemes.cataF(gather: (S, F[A]) => A)(using Project[F, S])`,
  `Schemes.anaF(coalg: N => F[N])(using Embed[F, S])`, `Schemes.hyloF(...)`. Gather is the
  para-flavored `(S, F[A]) => A` (the dual of droste's `(A, F[S]) => S`); pure `F[A] => A` is the
  degenerate case.

**User obligations & optional derivation**
- R6. The user **writes the pattern functor `F`** and its `Functor[F]`. (Macros cannot generate the
  `F` *type* — see G3.) eo *may* derive `Project[F, S]` / `Embed[F, S]` from the ADT↔`F` constructor
  correspondence; whether that derivation is feasible/worthwhile is a planning question (it is a
  harder macro than `plate`). Absent derivation, the user hand-writes `Project`/`Embed` (droste's
  model).

**Composability & scope**
- R7. Typed-path schemes are core optic types over `Forget[F]` and compose via `andThen` (and `cross`
  where the carrier instances exist) — eo's "schemes are optics" value preserved.
- R8. v1 = `cataF`/`anaF`/`hyloF` over a user `F`. The zoo (para/apo/histo/futu) is deferred but the
  driver shape (Gather/Scatter) supports it later.

## Success Criteria
- A user-supplied `F` yields `cataF`/`anaF`/`hyloF` that are typed (pattern-match `F`'s ctors) and
  stack-safe at 10⁶.
- The typed schemes compose with the rest of the optic algebra via `andThen`.
- #23's API compiles and behaves unchanged.
- Allocation (extend `SchemesBench`, `-prof gc` B/op): typed-F path **at parity** with droste's
  *basic* schemes — net-better because eo also delivers stack-safety droste's basic path lacks.
  (Beating droste's allocation needs a specialized `F`; out of scope.)

## Scope Boundaries
- **Complement, not subsume/replace**: #23 stays primary and untouched; the typed path is opt-in.
- The pattern functor `F` is **user-written** (eo does not derive the `F` type).
- v1 schemes: `cataF`/`anaF`/`hyloF`. Zoo deferred.
- Applies to recursive ADTs with a user-supplied `F`; non-recursive optics untouched.
- Not chasing a boxing win (a derived generic `F` boxes like everything else).

## Key Decisions
- **Complement, not subsume.** The spike refuted the subsume mechanism: `F` can't be derived, so
  `Plated` can't auto-become a typed-`F` source. The typed path is a second, opt-in door.
- **Reuse `Forget[F]` as the carrier** (no new carrier, no `Optic` generalization) — spike-proven.
- **Aligns with the prior verdict.** `docs/research/2026-06-08-corecursion-encoding-spike.md` said
  "scope B (typed pattern functor ≈ droste) as a thin convenience, not a second engine." This lands
  exactly there: #23 (encoding A) primary; the typed `F` path a thin opt-in. The new, verdict-vetted
  differentiators are stack-safety (droste's basic schemes lack it) and optic-composability.

## Spike Findings (2026-06-09 — resolved the gates)
- **G2 carrier-fit: ✓** A typed `BinF[A]` is an `Optic[Bin,Bin,Bin,Bin,Forget[BinF]]` (`to`=project,
  `from`=embed) — compiles, no `Optic` change. A typed, `Eval`-trampolined cata folds a 10⁵-deep
  spine without overflow. The carrier optic is one layer; the recursion is a separate driver (as
  #23's already is).
- **G3 derivation: ✗ (for the type)** Scala-3 quoted macros emit terms, not type definitions — the
  named `enum F[A]` cannot be macro-generated. The user writes `F`; at most `Project`/`Embed`
  instances are derivable (a harder macro than `plate`, deferred to planning).
- **G1 verdict: reconciled** The typed path is droste-shaped (user writes `F`) — but as a *thin
  opt-in*, which is what the verdict endorsed; the stack-safety + composability adds are real.
- **G4 cheap alternative:** Considered and set aside in favor of the typed path's full
  (named-constructor) type-safety. A typed-`PSVec[A]` tweak to #23 remains available later as a
  cheaper, partial (still-positional) improvement if desired.

## Dependencies / Assumptions
- Reuses the existing `Forget[F]` carrier and the #23 scheme drivers' shape.
- `cats.Eval` (already a dependency) for the stack-safe trampoline, unless planning picks a heap walk.
- droste remains the benchmark baseline, not a runtime dependency.

## Outstanding Questions

### Deferred to Planning
- [Affects R2][Technical] Driver mechanism: `Eval`-trampoline (spike used it; simple, allocates Eval
  nodes per node) vs. an explicit typed heap machine (lower alloc, more code, must avoid re-erasing
  to `AnyRef`). Pick per a B/op measurement.
- [Affects R6][Needs research] Is deriving `Project[F, S]` / `Embed[F, S]` from the ADT↔`F`
  constructor correspondence feasible/worthwhile, or do users hand-write them (droste-style)?
- [Affects R7][Technical] `cross`/`andThen` across a per-`F` `Forget[F]` carrier — which carrier
  instances (`Accessor`/`AssociativeFunctor`/`Composer`) are needed, and do they compose with the
  fixed carriers, or only self-compose / via `Direct` as #23's already do?

### Resolve Before Planning
- (none — the spike resolved the gating premises; the above are planning-level technical choices.)

## Next Steps
→ `/ce:plan` — design the opt-in typed-`F` path (R1–R8) as a complement to #23.
