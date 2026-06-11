---
date: 2026-06-10
topic: unfold-build-many-citizen
spike: resolved (2026-06-11, branch spike/unfold-build-many-citizen — see Findings at bottom)
---

# `Unfold` — inhabiting the build-only / many cell

## Problem Frame

The optic family is a two-axis lattice: **focus shape** (carrier) × **capability** (which of
`to` / `from` is vestigial). Filling in every cell:

| focus shape | read-write | read-only (`T=B=Unit`) | build-only (`S=A=Unit`) |
|---|---|---|---|
| total, 1 (`Direct`/`Tuple2`) | Iso / Lens | Getter — `S => A` | Review — `A => S` |
| partial, 0-or-1 (`Either`/`Affine`) | Prism / Optional | AffineFold — `S => Option[A]` | *≡ Review* (a Prism's `mend` is total) |
| many (`Forget`/`MultiFocus`) | Traversal | Fold — `S => F[A]` | **∅ ← this spike** |

Plus `Setter` (`(A=>B) => S=>T`) as a capability-only, carrier-agnostic fourth column (the lattice
bottom). The recursion schemes land on the corners: **`cata` → Getter** (read-only/total — tear a
structure to a value), **`ana` → Review** (build-only/total — grow a structure from a seed),
`hylo` = the diagonal that never materializes the middle.

The bottom-right cell is empty. This spike is about what lives there.

## The shape question (resolve first)

The originating suggestion named the cell `Unfold` with shape `T => F[B]`. But `T => F[B]` is
*one → many*, which is **`Fold`'s** arrow direction (`Fold[S,A]` has `to: S => F[A]`). An optic
whose real map is `to: T => F[B]` on `Forget[F]` is **structurally `Fold[T, B]`** — same carrier
slot, same vestigial `from`. So `T => F[B]` does **not** inhabit the empty cell; it re-derives a
filled one.

The genuinely-empty cell is the **dual**: build-only/many means the real map is `from`, and on the
`Forget[F]`/`MultiFocus[F]` carrier that is

```
embed / algebra :  F[B] => T        (many → one)
```

This is the across-both-axes dual of `Fold` (`S => F[A]` reversed). It is the **algebra** of a
recursion scheme, and it is the F-shape `embed` of `Recursive`/`Corecursive`:

```
project :  S => F[S]   -- Recursive    (Fold-slot; one → many)
embed   :  F[S] => S   -- Corecursive  (the empty cell; many → one)
```

`Plated.plate : Optic[S,S,S,S,MultiFocus[PSVec]]` already **bundles both** (`to: S => (S,PSVec[S])`
is project-ish, `from: (S,PSVec[S]) => S` is embed-ish) — the recursion-schemes brainstorm
(2026-06-08) noted this latent structure. So the family already *contains* embed inside a
read-write traversal; what's missing is the standalone **build-only** citizen that exposes embed
alone.

**Naming proposal.** Keep `Unfold` for the corecursive citizen, but pin it to the empty cell's
honest map `embed: F[B] => T`. (An "unfold" *produces* a structure; `embed` produces one `T` from a
layer of already-built parts — the one-step of corecursion. The aggregation reading — "build an
`Order` from its line-items" — is the same map.) If we'd rather the name track the arrow literally,
the alternatives are `Algebra` / `Embed` / `Fuse`. **Open: confirm `Unfold = F[B] => T`** before any
code.

## Why it's worth a citizen (not just a function)

1. **It makes `direct2forget`'s `???` real.** `Composer.direct2forget` (Composer.scala) lifts a
   Direct optic into `Forget[F]` and its `from` is the one remaining `???` in core — documented as
   unreachable because "`Forget[F]` admits no `ReverseAccessor`… the build side is never invoked."
   That `???` *is* this empty cell. A carrier that genuinely has a build side turns the hole into a
   sound branch (or sharpens why it stays unreachable).

2. **`Schemes.cata` consumes an algebra as a plain function today.** `Schemes.cata(eoSum)` takes
   `F[A] => A` and returns the composed `DirectGetter`. Promoting the algebra to an `Unfold` citizen
   makes `cata = plateFold.cross(unfold)` a genuine *composition* — the same move that already made
   `ana` return a `Review` rather than a bespoke function. `hylo = unfold ∘ fold` becomes an optic
   `.andThen`/`.cross` instead of a hand-written driver.

3. **Aggregation as an optic.** `F[B] => T` is "assemble one whole from many parts" — a `Monoid`-ish
   reduce with a real result type. There is no optic for this today; users drop to `foldMap` + a
   manual rebuild.

## Sketch (to validate in the spike)

```scala
//  carrier: Forget[F]  (Forget[F][X,A] = F[A]) — same as Fold, opposite capability
final class Unfold[T, B, F[_]](val embed: F[B] => T)(using ???)
    extends Optic[Unit, T, Unit, B, Forget[F]]:
  type X = Nothing
  def to(u: Unit): F[Unit] = ???     // vestigial — but F[Unit] needs an F-value; see Q2
  def from(fb: F[B]): T = embed(fb)
```

## Open questions

- **Q1 (shape/name).** Confirm `Unfold = embed = F[B] => T` (recommended) vs. the literal
  `T => F[B]` (which is `Fold` and needs no new cell). This decides everything below.
- **Q2 (vestigial `to`).** `Getter`/`Fold` zero out `from` trivially (`Unit`). But `Unfold`'s
  vestigial side is `to: Unit => F[Unit]` — there's no canonical `F[Unit]` without `Applicative[F]`
  (`pure(())`). Does `Unfold` require `Applicative[F]`, or do we admit a carrier where `to` is
  genuinely unreachable (mirror of `direct2forget`'s `from`)? This is the dual of the read-only
  optics' "`from` ignores its input" and may want the same `readOnly`-style honesty.
- **Q3 (constraint ladder).** Read-only `Fold` needs `Foldable[F]`. What does `Unfold` need —
  `Applicative[F]` (assemble) or something weaker? Is there a `Functor`-only useful subset?
- **Q4 (carrier: `Forget[F]` vs `MultiFocus[F]`).** `Forget` drops the leftover `X`; `embed` of a
  recursion layer may need the structural skeleton (cf. `Plated` using `MultiFocus[PSVec]` precisely
  so the skeleton survives). Which carrier does the standalone citizen want?
- **Q5 (fused, final-class from day one).** Per the encoding findings (final class + stored fn +
  fused `andThen`, never a shared anon wrapper). What composes with `Unfold`? `unfold.andThen(unfold)`
  (algebra composition?), `review.cross(unfold)` (the hylo seam)?
- **Q6 (scope).** Does this live in `core` (as the suggestion proposed) or in `schemes` (its only
  current consumer)? Argument for core: it completes the lattice and removes a core `???`. Argument
  for schemes: no non-scheme use case is known yet.

## Smallest viable spike

Define `Unfold` on `Forget[F]` with `embed: F[B] => T`, `Applicative[F]` for the vestigial `to`,
fused final-class. Rewrite `Schemes.cata` to build `plateFold.cross(unfold)` and confirm it matches
the current `DirectGetter` result (behaviour + the typed-schemes B/op machine numbers). Success
criterion: `cata`/`hylo` expressed as composition, and `direct2forget`'s `from` either becomes sound
or its unreachability is provable from `Unfold`'s laws.

## Findings (spike executed 2026-06-11, branch `spike/unfold-build-many-citizen`)

Implemented: `core/optics/Unfold.scala`, fused member on `Review`, sound `direct2forget`,
`Schemes.cata(Unfold)` overload, `UnfoldSpec` + `SchemesSpec` addition. Follow-up pass (same
branch): `UnfoldLaws` + `UnfoldTests` discipline rulesets in `cats-eo-laws` (registered in
`OpticsLawsSpec` for both an Applicative carrier and a pattern functor), and the composition
matrix extended to the 11-family grid (121 cells) — the `Unfold` row/column exactly mirrors
`Review`'s reversibility pattern (`iso`/`prism`/`review`/`unfold` compose; everything else is
void by design). Two generalizing compositions were added for that symmetry:
`Unfold.andThen(any reversible inner)` (`ReverseAccessor[G]` + `Functor[F]`) and the
`reversible outer ∘ Unfold` extension in `Optic` (the many-rung mirror of `andThen(Review)`).
Full root aggregate green.

- **Q1 — CONFIRMED.** `Unfold[T, B, F] = Optic[Unit, T, Unit, B, Forget[F]]` with the real map
  `embed: F[B] => T`, `X = Nothing`. Final class storing `embed`, per the encoding findings.

- **Q2 — sharper than anticipated, and decisive.** The prime consumers (pattern functors: `BinF`,
  `RoseF`, …) admit `Functor`/`Traverse` but **no `Applicative`** — `pure` cannot pick a
  constructor. Requiring `Applicative[F]` for the vestigial `to` would have excluded the
  recursion-scheme motivation entirely. Resolution: **two factories**. `Unfold.apply`
  (`F: Applicative`, honest `to = pure(())`; read-side ops degrade to the singleton layer) and
  `Unfold.algebra` (constraint-free; `to` throws `UnsupportedOperationException` — the honest
  mirror of `direct2forget`'s formerly-unreachable `from`, and it fails *loudly*, tested).

- **Q3 (ladder).** `embed` itself: no constraint. `unfold.andThen(review)` (pre-process each
  part): `Functor[F]` — pattern functors compose freely. `review.andThen(unfold)` (post-process
  the whole): **no constraint** (the seam threads a single `B`, never an `F`-layer).
  `unfold.andThen(unfold)`: `Applicative[F]` (the same algebraic-lens `pure` re-lift as
  `assocForgetMonad`). `Foldable` is never needed by `Unfold` itself.

- **Q4 (carrier).** `Forget[F]` suffices: `embed` needs no structural leftover. No seam exercised
  in the spike wanted `MultiFocus[F]`; revisit only if a future consumer needs the skeleton to
  survive *alongside* the build (Plated keeps both halves for exactly that reason).

- **Q5 (composition).** Fused final-class members shipped: `andThen(Review)` /
  `andThen(Unfold)` / the generalized `andThenBuildAny` on `Unfold`, plus `Review.andThen(Unfold)`
  and the reversible-outer extension in `Optic`. The non-fused generic path also works: a
  morph-routed `Forget[F]` chain composes via `assocForgetMonad` for `Monad[F]` — and its build
  side **executes** `direct2forget.from`.

- **Q6 — core.** It closes a core hole and its fused member lives on `Review` (core). Schemes
  consumes it.

- **`direct2forget`'s `???` is now a sound branch** (motivation #1 — confirmed). `Unfold` made it
  reachable (`UnfoldSpec` proves execution via `BijectionIso[Unit,·].andThen(unfold)`). The pick
  is total on every reachable path because the only `F[B]` ever fed to a lifted Direct optic's
  `from` is `ForgetPull.monadicPull`'s `pure(b)`; hand-routed cardinality ≠ 1 throws like the
  other `pickSingletonOrThrow` bridges. Cost: the Composer gained a `Foldable[F]` constraint —
  the composition matrix (100 cells) is unaffected.

- **`cata = plateFold.cross(unfold)` — REFUTED as literally stated** (motivation #2 — corrected).
  It is a type error: `cross` needs a shared carrier or `Accessor`+`ReverseAccessor` on
  `MultiFocus[PSVec]`, which rightly don't exist; conceptually `cata` is a *fixpoint* of the
  layer optic, not a 2-optic composition. What **is** true and shipped: the algebra becomes a
  citizen the engine consumes (`Schemes.cata(sizeAlg: Unfold[A, A, PSVec])`), and algebras can be
  *assembled by optic composition* before being consumed (`Review(_*2).andThen(Unfold.algebra(…))`
  — tested). Honesty limit: an untyped `PSVec` layer is node-blind, so pure `PSVec`-algebras only
  express constructor-independent folds; the para overload stays primary. The typed path is where
  pure algebras are fully expressive: **PR #24's `Embed[F, S]` IS `Unfold[S, S, F]`** — unifying
  them (anaF taking an `Unfold`, cataF's pure overload being one) is the natural follow-up once
  #24 lands.

- **Deferred.** Laws (candidates: `(rev ∘ u).embed = rev.reverseGet ∘ u.embed`;
  `(u ∘ rev).embed = u.embed ∘ map(rev.reverseGet)`; singleton degradation
  `modify(f)(()) = embed(pure(f(())))` for Applicative `F`). CompositionMatrixSpec extension to
  the now-11-family matrix (DONE — see above). The `Embed ≅ Unfold` unification (blocked on #24).
