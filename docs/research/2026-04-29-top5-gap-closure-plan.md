# Top-5 gap-closure plan — post-consolidation

**Date:** 2026-04-29
**Status:** Draft — to be reviewed before any implementation
**Context:** Written after the four MultiFocus folds (AlgLens + Kaleidoscope, Grate, PowerSeries, FixedTraversal[N]) settled. Matrix snapshot: 13×13, 95 N / 57 M / 17 U / 0 ?. The pre-consolidation top-5 list (from `composition-gap-analysis.md` §1.2) reshaped — gaps #2, #3, #5 referenced carriers that are now absorbed into MultiFocus, so the gap text below restates each in post-consolidation terms.

## 1. Cross-F Forget composition — `Forget[F].andThen(Forget[G])` for `F ≠ G`

**Status: SHIPPED 2026-04-29.** A Forget-specific `.andThen` extension lives in `Forget.scala` (inside `object Forget`). The user supplies a natural transformation `F ~> G` and `FlatMap[G]`; the result carrier is `Forget[G]`. The original "U + workaround" plan was upgraded to a real ship after the user observed that supplying nat transformations is a perfectly principled side-channel — the meaning shift (result carrier ≠ outer's F) is documented, not hidden.

### Restatement

`Fold[List].andThen(Fold[Option])` fails implicit resolution silently via the Composer/AssociativeFunctor route — `Composer[F, G]` is over carrier kinds, not value-typed witnesses, so a per-call `F ~> G` doesn't fit. The `.andThen` extension on Optic requires same-`F` (via `AssociativeFunctor[F]`).

### Decision (revised)

**Ship a dedicated `.andThen` extension method on `Optic[S, Unit, A, A, Forget[F]]`.** The user opts into the meaning shift by passing a nat. Same shape as gap #4's SetterF extension: more-specific extension method, sidesteps the typeclass-protocol ergonomics question.

### Rationale

The cross-F bridge needs `F ~> G` plus `FlatMap[G]`. Both are typeclass evidence the user can supply at the call site. The shipped form:

```scala
extension [S, A, F[_]](outer: Optic[S, Unit, A, A, Forget[F]])
  def andThen[G[_], B](inner: Optic[A, Unit, B, B, Forget[G]])(using
      nat: cats.~>[F, G],
      flatG: FlatMap[G],
  ): Optic[S, Unit, B, B, Forget[G]] = ...
```

The implementation is direct: `composed.to(s) = flatG.flatMap(nat(outer.to(s)))(inner.to)`; `composed.from = _ => ()` (T = Unit).

Restricted to `T = Unit` (the Fold case): cross-F composition has no natural way to thread `from` for general `T`, since the F → G nat doesn't tell us how to bridge `T` types. T = Unit is the only shape where the discard semantic on both sides agrees trivially.

### Worked examples

```scala
import cats.~>
import cats.arrow.FunctionK

// List ~> Option (head): pick the head, run inner on it
given listHead: List ~> Option = new (List ~> Option):
  def apply[T](fa: List[T]): Option[T] = fa.headOption

val composedHeadEven = triplet.andThen(evenOpt)
// composed.to(5) = headOption([4,5,6]).flatMap(...) = Some(4) (4 is even → Some(4))

// Option ~> List (None → Nil, Some(a) → [a]): expand the optional through the inner
given liftK: Option ~> List = new (Option ~> List):
  def apply[T](fa: Option[T]): List[T] = fa.toList
val composedExpand = maybe.andThen(expand)

// Identity nat (F = G) works too — equivalent to same-F flatMap
given idListK: List ~> List = FunctionK.id[List]
```

### Files touched

- `core/src/main/scala/dev/constructive/eo/data/Forget.scala` — added `andThen` extension inside `object Forget` (~30 LoC including the docstring + meaning-shift documentation).
- `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala` — new block "Cross-F Forget composition: forget.andThen(forget)..." exercising List ~> Option, Option ~> List, and identity-nat parity.
- `docs/research/2026-04-23-composition-gap-analysis.md` §1.2 / §3 — flip the U-cell mention to "shipped via `.andThen` extension; user supplies nat".
- `site/docs/optics.md` — Composition-limits section: replace "cross-F is ruled out" with "cross-F ships under nat-supplied opt-in".

### Effort

~2 hours actual (vs ~1 h budgeted for docs-only path). The expanded scope was the user's call: the nat-supplied side-channel is too useful to leave unshipped.

---

## 2. MultiFocus outbound to `Forget[F]` — close the read-only escape

**Status: SHIPPED 2026-04-29.** `multifocus2forget[F]: Composer[MultiFocus[F], Forget[F]]` lives in `MultiFocus.scala`. The morph discards the structural leftover and exposes the focused `F[A]` directly. `from` returns `().asInstanceOf[T]` — sound only when `T = Unit` (Forget-carrier optics have T=Unit by construction).

### Empirical finding on the bidirectional ban

The original plan flagged `multifocus2forget` as structurally rejected because `forget2multifocus` already ships and Scala 3's Morph resolution forbids bidirectional Composer pairs (both `Morph.leftToRight` and `Morph.rightToLeft` would fire, producing implicit ambiguity). The 2026-04-29 implementation tested this empirically by shipping the Composer and running the full test suite; **no chain in the corpus triggered the ambiguity**. Why: every `Forget[F].andThen(MultiFocus[F])` use site routes through the explicit `summon[Composer[..]].to(o)` form (or its `.morph[…]` wrapper), which never invokes Morph; the ambiguity only manifests for `.andThen` chains that don't exist in the codebase. The Composer ships unguarded; if a real-world chain hits the ambiguity later, the workaround is one extra `.morph`-shaped call.

### Files touched

- `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` — added `multifocus2forget[F]` (~30 LoC including the bidirectional-pair documentation).
- `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala` — extended the existing MultiFocus read-only escape spec to exercise the new Composer (`asFold = summon[Composer[MultiFocus[List], Forget[List]]].to(listMF)`).
- `docs/research/2026-04-23-composition-gap-analysis.md` §1.2, §3.2.6 — updated to reflect the ship.

### Capability surface today

The user's "aggregate the focused F[A] monoidally" capability lights up via three distinct paths, all working:

1. **Carrier-wide `Optic.foldMap`** extension (gated on `ForgetfulFold[F]`, which `mfFold[F: Foldable]` provides). Works on any `MultiFocus[F]`-carrier optic without imports beyond `MultiFocus.given`.
2. **`.headOption` / `.length` / `.exists`** extensions (shipped 2026-04-29 via the optic-extensions research) — same gate, same auto-pickup.
3. **`multifocus2forget` Composer** (this gap) — for users that want the explicit Optic[…, Forget[F]] for downstream interop / further composition.

The remaining 7 of the 20 surveyed extension methods in `docs/research/2026-04-29-optic-extension-methods.md` are candidates if user demand surfaces.

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

**Status: SHIPPED 2026-04-29.** `AssociativeFunctor[SetterF, Xo, Xi]` lives in `SetterF.scala` as `assocSetterF` with `Z = (Fst[Xo], Snd[Xi])`. Standard `Optic.andThen[SetterF]` resolution picks it up — no extension method, no naming gymnastics. Outcome **(a) Yes** in the original plan terms.

### Outcome of the investigation

The investigation initially landed on (b) No (extension method) when the AssociativeFunctor protocol seemed to require recovering the user's `c2d` from a post-`mfFunctor.map` continuation. That reading was wrong. The fix is: `composeTo` doesn't need to call `inner.to` at all. Seeding the result SetterF with `(outer-source, identity[C])` is sufficient because:

- SetterF's continuation is structurally `identity[A]` at every canonical construction site (`coerceToSetter` line 60, `Setter.apply` line 29).
- After `mfFunctor.map` applies `c2d`, the continuation becomes `c2d` directly (since `f ∘ identity = f`).
- `composeFrom` then extracts `c2d` from `xd.setter._2` and applies it through `inner.from` then `outer.from` — that's the deferred-modify semantic verbatim.

The trick is `Z = (Fst[Xo], Snd[Xi])`, which makes `Fst[Z] = Fst[Xo]` and `Snd[Z] = Snd[Xi]` after match-type reduction. asInstanceOf casts coerce abstract `Fst[Xo] / Snd[Xo] / Fst[Xi] / Snd[Xi]` to the canonical `(S, A)` / `(A, C)` decomposition — sound under the universal SetterF convention enforced at every construction site, unsafe only for hand-built optics that violate it (and there's no public API path to build such an optic).

### What shipped

```scala
given assocSetterF[Xo, Xi]: AssociativeFunctor[SetterF, Xo, Xi] with
  type Z = (Fst[Xo], Snd[Xi])

  def composeTo[S, T, A, B, C, D](
      s: S,
      outer: optics.Optic[S, T, A, B, SetterF] { type X = Xo },
      inner: optics.Optic[A, B, C, D, SetterF] { type X = Xi },
  ): SetterF[Z, C] =
    val xo: Fst[Xo] = outer.to(s).setter._1
    SetterF((xo, identity[C].asInstanceOf[Snd[Xi] => C]))

  def composeFrom[S, T, A, B, C, D](
      xd: SetterF[Z, D],
      inner: optics.Optic[A, B, C, D, SetterF] { type X = Xi },
      outer: optics.Optic[S, T, A, B, SetterF] { type X = Xo },
  ): T =
    val xo: Fst[Xo] = xd.setter._1.asInstanceOf[Fst[Xo]]
    val c2d: Snd[Xi] => D = xd.setter._2.asInstanceOf[Snd[Xi] => D]
    val innerModify: A => B = a => inner.from(SetterF((a.asInstanceOf[Fst[Xi]], c2d)))
    outer.from(SetterF((xo, innerModify.asInstanceOf[Snd[Xo] => B])))
```

### Why not the extension-method route

A first attempt at this gap shipped a SetterF-specific `.andThen` extension method, on the assumption that the AssociativeFunctor protocol couldn't fit. That attempt failed at compile time: trait member methods in Scala 3 always shadow imported extension methods with the same name, so `outerSf.andThen(innerSf)` resolved to `Optic.andThen` (the trait method, requiring `AssociativeFunctor[SetterF]`) before the extension was ever consulted. The fix isn't to refactor the trait method — it's to ship the AssociativeFunctor instance the trait method already wants.

### Files touched

- `core/src/main/scala/dev/constructive/eo/data/SetterF.scala` — class docstring updated to describe the assocSetterF encoding; `assocSetterF` given lives at the bottom of `object SetterF`.
- `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala` — new block "SetterF same-carrier composition: setter.andThen(setter)" exercising Lens-into-SetterF × Lens-into-SetterF, then × Prism-into-SetterF (hit/miss), then × Optional-into-SetterF (hit/miss).
- `site/docs/optics.md` — Setter section rewritten to show `setter.andThen(setter)` as a working chain with the AssocFunctor; "Composition limits" SetterF entry rescoped to outbound Composers only.

### Effort

~3 hours actual: failed extension-method attempt + course-correct + AssocFunctor implementation + behaviour spec + docs. The protocol-fit re-examination is the core of the work.

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
| 1 | Cross-F `Forget[F].andThen(Forget[G])` | SHIPPED — `.andThen` extension under `F ~> G` + `FlatMap[G]` | 2 h | nat-supplied side-channel |
| 2 | MultiFocus outbound (read-only) | Ship `Composer[MultiFocus[F], Forget[F]]` for `Foldable[F]` | 3 h | 4 cells flip U → N |
| 3 | Cross-F MultiFocus composition | Document U + workaround (cross-link to #1) | 0.5 h | structural; same shape as #1 |
| 4 | SetterF composition | SHIPPED — `AssociativeFunctor[SetterF]` (`assocSetterF`) | 3 h | `setter.andThen(setter)` compiles |
| 5 | JsonPrism × MultiFocus | Write behaviour spec | 1 h | confirms existing N cell |

**Total effort**: ~6.5–9.5 hours, depending on gap #4's investigation outcome.

**Order of operations**: #5 first (smallest, lowest-risk, builds confidence the post-fold Composers actually compose). Then #2 (the only new Composer; closes 4 cells). Then #4's investigation. Then #1 + #3 docs.

Three of the five gaps (#1, #3, #5) are documentation work or single-test work — the matrix understated how much of the gap closure is "ship the test that proves the existing path works". One (#2) is a real Composer ship. One (#4) needs investigation before the action is decided.

**Pre-0.1.0 readiness implications**: with these five closures, the matrix lands at approximately 99 N / 57 M / 13 U / 0 ? — fully documented, no surprises. The remaining 13 U cells are all genuinely structural (T=Unit propagation, Distributive[F] vs Apply[F] mismatch, write-only Setter terminus). At that point the matrix becomes a stable reference rather than an ongoing audit.
