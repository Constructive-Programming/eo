---
date: 2026-06-10
topic: unfold-build-many-citizen
spike: open
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
