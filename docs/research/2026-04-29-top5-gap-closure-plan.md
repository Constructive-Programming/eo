# Top-5 gap-closure plan — post-consolidation

**Date:** 2026-04-29
**Status:** Draft — to be reviewed before any implementation
**Context:** Written after the four MultiFocus folds (AlgLens + Kaleidoscope, Grate, PowerSeries, FixedTraversal[N]) settled. Matrix snapshot: 13×13, 95 N / 57 M / 17 U / 0 ?. The pre-consolidation top-5 list (from `composition-gap-analysis.md` §1.2) reshaped — gaps #2, #3, #5 referenced carriers that are now absorbed into MultiFocus, so the gap text below restates each in post-consolidation terms.

## 1. Cross-F Forget composition — `Forget[F].andThen(Forget[G])` for `F ≠ G`

### Restatement

`Fold[List].andThen(Fold[Option])` fails implicit resolution silently. No `Composer[Forget[List], Forget[Option]]` ships, and the natural-transformation witness `F ~> G` (or `G ~> F`) that a generic Composer would need isn't expressible in cats-eo's `Composer[F, G]` shape (which is over carrier kinds, not value-typed witnesses).

### Decision

**Document as U + ship a workaround pattern.** No code change; one prose update to `site/docs/optics.md` "Composition limits" subsection.

### Rationale

The cross-F bridge fundamentally needs `F ~> G` or a `Foldable[F] + Monad[G]` combination. `Composer[F, G]` is over `F[_, _]` / `G[_, _]` carrier kinds; the witness it consumes can't carry a per-call natural transformation without widening the typeclass surface. That widening is broader than 0.1.0 scope — it changes how every Composer in the codebase resolves.

### Workaround pattern (the U-cell idiom)

```scala
val outerFold: Fold[Source, A] = ...     // Forget[List]-carrier
val innerFold: Fold[A, B] = ...          // Forget[Option]-carrier
def composed(s: Source): Vector[B] =
  outerFold.toList(s).flatMap(a => innerFold.toList(a)).toVector
```

The user falls back to plain Scala `flatMap` to bridge the two F's. Ergonomics regression vs `.andThen`, but no information loss.

### Files touched

- `site/docs/optics.md` — add subsection "Cross-F Fold composition" under "Composition limits" with the workaround pattern, citing the structural reason.
- `docs/research/2026-04-23-composition-gap-analysis.md` §1.2 / §3 — replace the gap's `?`-ish flavour with explicit U classification + cross-link.

### Effort

~1 hour (docs only).

---

## 2. MultiFocus outbound to `Forget[F]` — close the read-only escape

### Restatement

After the four folds, MultiFocus has many inbound Composers (Iso → MF, Lens → MF for some F, etc.) but only ONE outbound: `multifocus2setter`. A user holding `Optic[S, T, A, A, MultiFocus[List]]` who wants to read out the focused list as a Fold has no carrier route — they must call `.modify` with an extractor closure.

### Decision

**Ship `Composer[MultiFocus[F], Forget[F]]` when `Foldable[F]` is in scope.**

### Rationale

`MultiFocus[F][X, A] = (X, F[A])`. Lifting to `Forget[F][_, A] = F[A]` is exact — discard the leftover, return the F[A]. The constraint is `Foldable[F]` (Forget[F]'s carrier needs it for `foldMap` / `traverse_`). Every shipped `F` for MultiFocus already has `Foldable` (List, Option, Vector, Chain, PSVec, Tuple_N, Function1[X, *] under specific X — only the last lacks Foldable, and that's exactly the case that stays U).

This closes 4 cells in the matrix (MultiFocus × Fold, MultiFocus × Traversal.forEach, MultiFocus row-side as outer for Forget-carrier inners) and aligns MultiFocus's outbound profile with the principle "if you have the typeclass evidence, the bridge ships".

### Files touched

- `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` — add `multifocus2forget[F: Foldable]: Composer[MultiFocus[F], Forget[F]]`. ~15 LoC.
- `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala` — add cross-carrier scenario exercising `multifocus.andThen(fold)`.
- `tests/src/test/scala/dev/constructive/eo/OpticsLawsSpec.scala` — add `MorphLaws.A1` block for the Composer.
- `docs/research/2026-04-23-composition-gap-analysis.md` §1.2, §2, §3 — flip 4 cells U → N.
- `site/docs/optics.md` — note the new outbound path under MultiFocus's "Composability" subsection.

### Effort

~3 hours (one Composer + 2 tests + matrix update + docs).

---

## 3. PowerSeries → MultiFocus / Grate (now reframed: cross-F MultiFocus composition)

### Restatement

Pre-folds: "PowerSeries → MultiFocus / Grate is uniformly absent." Post-folds: PowerSeries IS `MultiFocus[PSVec]` and Grate IS `MultiFocus[Function1[X, *]]`, so the gap reframes as **`MultiFocus[F] × MultiFocus[G]` for `F ≠ G`** — same problem as gap #1's cross-F Forget, structurally.

### Decision

**Document as U + ship a workaround pattern.** Same shape as gap #1.

### Rationale

The shipped `mfAssoc*` instances (`mfAssocFunction1` for the absorbed-Grate sub-shape, `mfAssocPSVec` for the absorbed-PowerSeries sub-shape, etc.) are all SAME-F — they require `F = G` at the type level. Cross-F MultiFocus composition needs a per-F transformation witness, same broader-than-Composer change as gap #1.

The folds did NOT close this gap; they reframed it. The reframing matters: a user who pre-fold thought "PowerSeries and Grate are different carriers, no surprise they don't compose" now sees "MultiFocus and MultiFocus, why don't they compose?" The U-cell text in §3 of the gap analysis should call this out explicitly.

### Workaround pattern

```scala
// Outer: MultiFocus[List] (e.g. .each over List)
// Inner: MultiFocus[Function1[Int, *]] (e.g. .tuple)
val composed = outer.modify(a => innerOptic.modify(f)(a))(s)
```

Same nested-modify pattern users had pre-folds, just on a single carrier name now.

### Files touched

- `site/docs/optics.md` — "Composition limits" subsection: "Cross-F MultiFocus composition (e.g. `each.andThen(tuple)`)".
- `docs/research/2026-04-23-composition-gap-analysis.md` — clarify that the U cells in MF row × MF column for cross-F are the same structural shape as cross-F Forget (gap #1).

### Effort

~30 min (docs only). The structural rationale already lives in §1; this is just cross-linking.

---

## 4. SetterF composition — `setter.andThen(setter)` doesn't compile

### Restatement

`SetterF` has `ForgetfulFunctor` and `ForgetfulTraverse` but no `AssociativeFunctor`. `setter.andThen(setter)` fails with a silent implicit miss. Documented in `SetterF.scala` line 14 but nowhere user-facing.

### Decision

**Investigate whether `AssociativeFunctor[SetterF]` is structurally possible. Two possible outcomes:**
- **(a) Yes**: ship the instance + a behaviour spec.
- **(b) No**: surface the absence prominently in `site/docs/optics.md` Setter section AND in the SetterF carrier doc, with a worked example showing the correct idiom.

### Rationale

A `Setter[S, T, A, B]` has shape `(A => B) => (S => T)`. Composing `Setter[S, T, A, B]` with `Setter[A, B, C, D]` should give `Setter[S, T, C, D]` via plain function composition — the inner consumes `(C => D)` and produces `(A => B)`, which the outer consumes. So Setter × Setter SHOULD be expressible.

The question: does cats-eo's specific `SetterF` carrier encoding admit an `AssociativeFunctor[SetterF]` instance, or is the absence a structural feature of the encoding (e.g., the existential `X` type on `Optic[S, T, A, B, SetterF]` doesn't thread through composition)? **The investigation is the load-bearing step**: the gap's text says "by design" but doesn't cite the design. Confirm or refute.

### Files touched

- `core/src/main/scala/dev/constructive/eo/data/SetterF.scala` — either ship `AssociativeFunctor[SetterF]` or expand the comment block on why not.
- `site/docs/optics.md` Setter section — surface the outcome (either "and Setter composes via the AssociativeFunctor instance" or "Setter is composition-terminal; here's why and the workaround").
- If shipping: `tests/src/test/scala/.../OpticsBehaviorSpec.scala` adds a `setter.andThen(setter)` behaviour test.

### Effort

- (a) ~4 hours: investigation + shipping + tests
- (b) ~1 hour: investigation + docs

Effort is bounded by the investigation. Worst case is a clear "structurally impossible" verdict + half a page of optics.md.

---

## 5. JsonPrism × MultiFocus — confirm the path that already exists

### Restatement

JsonPrism's `Either` outer + `Composer[Either, MultiFocus[F]]` (for `F: Alternative + Foldable`) inner is plausibly an N cell — both Composers exist, neither has been combined in a test or doc.

### Decision

**Write the behaviour spec; flip the cell N if it works (likely), document it U if it doesn't.**

### Rationale

This is the simplest gap — no new Composer to ship, no structural question. Just verify that the existing pieces compose under realistic typeclass evidence (`F = List` for `Alternative + Foldable` is the canonical case).

A passing test confirms the matrix's `?`-ish cell as N. A failing test surfaces a real Composer-resolution issue worth a follow-up.

### Files touched

- `tests/src/test/scala/dev/constructive/eo/circe/CrossCarrierCompositionSpec.scala` — add scenario "(6) JsonPrism → MultiFocus[List] cross-carrier modify". Mirror the existing scenarios 4/5 (which exercise JsonPrism → traversal-shape inners).
- `docs/research/2026-04-23-composition-gap-analysis.md` — flip the JP × MF cell with a citation to the new spec.

### Effort

~1 hour (one spec + matrix update).

---

## Summary

| # | Gap | Action | Effort | Outcome |
|---|---|---|---|---|
| 1 | Cross-F `Forget[F].andThen(Forget[G])` | Document U + workaround | 1 h | structural; no carrier change |
| 2 | MultiFocus outbound (read-only) | Ship `Composer[MultiFocus[F], Forget[F]]` for `Foldable[F]` | 3 h | 4 cells flip U → N |
| 3 | Cross-F MultiFocus composition | Document U + workaround (cross-link to #1) | 0.5 h | structural; same shape as #1 |
| 4 | SetterF composition | Investigate; ship XOR document loudly | 1-4 h | TBD (depends on investigation) |
| 5 | JsonPrism × MultiFocus | Write behaviour spec | 1 h | confirms existing N cell |

**Total effort**: ~6.5–9.5 hours, depending on gap #4's investigation outcome.

**Order of operations**: #5 first (smallest, lowest-risk, builds confidence the post-fold Composers actually compose). Then #2 (the only new Composer; closes 4 cells). Then #4's investigation. Then #1 + #3 docs.

Three of the five gaps (#1, #3, #5) are documentation work or single-test work — the matrix understated how much of the gap closure is "ship the test that proves the existing path works". One (#2) is a real Composer ship. One (#4) needs investigation before the action is decided.

**Pre-0.1.0 readiness implications**: with these five closures, the matrix lands at approximately 99 N / 57 M / 13 U / 0 ? — fully documented, no surprises. The remaining 13 U cells are all genuinely structural (T=Unit propagation, Distributive[F] vs Apply[F] mismatch, write-only Setter terminus). At that point the matrix becomes a stable reference rather than an ongoing audit.
