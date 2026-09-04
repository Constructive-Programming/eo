---
date: 2026-06-12
topic: existential-x-is-the-decoration
spike: open (raised in PR #24 review — FoldM.scala thread)
---

# X = Nothing is a choice: the existential IS the decoration

## The observation (kryptt, PR #24)

> I see `X = Nothing` here and in AnaM, CataM while we have Attr and Coattr that pretty
> much are exactly the extra info needed on every to/from step. […] are we actually
> missing a very beautiful connection between optics and recursion schemes?

Yes — and naming it reorganizes the whole module's story.

## The connection

In eo's encoding, an optic is `(to: S => F[X, A], from: F[X, B] => T)` and **X is the
leftover** — whatever `to` must retain for `from` to rebuild. The scheme citizens pin
`X = Nothing` because `Direct`/`Forget` carriers are **forgetful**: a `Cata` worn as a
Getter throws away everything except the answer. That is a *choice of existential
resolution*, not a fact about folds.

What would a non-forgetful fold retain? Exactly the decoration:

| scheme | whole-scheme X | which optic it makes the fold |
|---|---|---|
| cata | `Nothing` | Getter — the forgetful projection |
| **para** | `F[(S, A)]` — subterms retained | **a lawful Lens**: `from` re-embeds the retained subterms, so get-put holds *definitionally* |
| **histo** | `Attr[F, A]` — the full memo | the iterated Lens: cofree = νX. A × F[X] |
| **apo** | `Either`-residual on the build | the Prism's match, worn build-side |
| **futu** | `Coattr[F, A]` | the iterated Prism residual: free = μX. A + F[X] |

Two readings of the same fact:

1. **Per layer:** a `Gather` optic's leftover is one F-layer of W — `X = (Unit, F[W])`.
   **Whole scheme:** the fixpoint of that per-layer leftover. `Attr[F, A] = νX. A × F[X]`
   is *literally* the fixpoint of the gather-side leftover; `Coattr[F, A] = μX. A + F[X]`
   of the scatter-side. **Attr/Coattr are not auxiliary data types — they are the
   universal existentials of decorated schemes.** The Gather/Scatter optics manufacture
   X layer-by-layer; the engine's out-array of W's is the X being threaded.

2. **Comonadically:** a lawful lens is a coalgebra of the store comonad (Riley; the
   lens complement is the store's "position"). histo is gcata over the **cofree**
   comonad — the iterated store. So histo : cata :: Lens : Getter, with `Attr` playing
   the complement. The plan's sum/product symmetry table (para = Tuple2/Lens carrier,
   apo = Either/Prism carrier) is the same statement made per-layer; this is it made
   whole-scheme, at the X seam.

The sharpest corollary: **deforestation is choosing the forgetful existential.**
`ana.cross(cata)` fused (no S built) vs materializing (S built) are the *same optic at
two X-resolutions* — `X = Nothing` vs `X = S` (or `Attr` for the memoized middle). The
fused/materializing pair we law-pinned is an instance of a general principle: refining
X from `Nothing` upward trades allocation for capability.

## What it could buy (follow-up candidates, in rough order of value)

1. **para-as-Lens** — `Optic[S, S, A, A, Tuple2] { type X = F[(S, A)] }`: get = fold,
   put = re-embed retained subterms with the new focus. get-put is definitional;
   put-get is the algebra-coherence law. The first *lawful writable* recursion scheme.
2. **Memoized refolds** — `cata.withHistory: X = Attr[F, A]`: hold the memo, modify,
   re-fold incrementally (only the spine above a change recomputes). Lens laws become
   memo-coherence laws. This is the incremental-computation story (Adapton-flavored)
   falling out of optic laws.
3. **The honest hylo optic** — expose the fused/materializing choice as an X
   parameter instead of two spellings.
4. **BiAffine's matrix row** — composing decorated schemes = composing their Xs;
   the `(W, F[W])` tuples compose exactly like Affine's existentials, which is what
   the deferred AssociativeFunctor[BiAffine] instance will thread.

## Recommendation

Not in PR #24 — it lands the forgetful citizens + the per-layer decoration optics,
which are the substrate. This spike is the natural *third* act after the elgot
follow-up: elgot completes the decoration vocabulary; this completes the existential
story (and would be the paper-worthy claim: "recursion schemes are optics indexed by
their existential; the (co)free (co)monads are the universal indices").
