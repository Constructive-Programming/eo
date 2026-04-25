# Composition-coverage gap analysis

**Date:** 2026-04-23
**Last updated:** 2026-04-24 — Unit 21 (0.1.0 plan) closed every numbered
`?` group from §3.3 by shipping one new Composer, documenting the
idioms, or pinning a structural-`U` decision. See §7 for the
post-resolution scoreboard.

**Scope:** every (outer × inner) optic-family composition pair currently
shipped in `cats-eo` (core + circe), classified by whether `.andThen`
works natively, requires a manual idiom, is unsupported, or is
unexplored.

## 0. Methodology

### 0.1 Families covered

The 15 "families" from the task brief collapse to 13 distinct rows once
duplicates are removed — AffineFold is `Optional` with `T = Unit` (same
`Affine` carrier; differs only in the result slot), and the PowerSeries
vs Forget split of Traversal behaves differently enough under composition
that we keep both. The rows used below are:

| # | Family | Carrier | Type alias / concrete class |
|---|--------|---------|-----------------------------|
|  1 | Iso | `Forgetful` | `BijectionIso` |
|  2 | Lens | `Tuple2` | `GetReplaceLens`, `SimpleLens`, `SplitCombineLens` |
|  3 | Prism | `Either` | `MendTearPrism`, `PickMendPrism` |
|  4 | Optional | `Affine` | `Optional` |
|  5 | AffineFold | `Affine` (`T = Unit`) | `Optic[S, Unit, A, A, Affine]` |
|  6 | Getter | `Forgetful` (`T = Unit`) | `Optic[S, Unit, A, A, Forgetful]` |
|  7 | Setter | `SetterF` | `Optic[S, T, A, B, SetterF]` |
|  8 | Fold | `Forget[F]` (`T = Unit`) | `Optic[F[A], Unit, A, A, Forget[F]]` |
|  9 | Traversal.each (PS) | `PowerSeries` | `Optic[…, PowerSeries]` |
| 10 | Traversal.forEach | `Forget[F]` | `Optic[…, Forget[F]]` |
| 11 | FixedTraversal[N] | `FixedTraversal[N]` | `Traversal.{two,three,four}` |
| 12 | AlgLens[F] | `AlgLens[F]` | `Optic[…, AlgLens[F]]` |
| 13 | Grate | `Grate` | `Optic[…, Grate]` |
| 14 | JsonPrism / JsonFieldsPrism | `Either` | `Optic[Json, Json, A, A, Either]` |
| 15 | JsonTraversal / JsonFieldsTraversal | — | standalone, not an `Optic` |
| 16 | Review | — | standalone, not an `Optic` |

That is **16 row labels**, but row 15 and 16 do not extend `Optic`, so
they can only appear as outer or inner of an idiom-level composition.
For the 14 `Optic`-bearing families above (1–14) we produce a 14×14
inner-matrix (196 cells) and then add two "outer JsonTraversal / Review"
and two "inner JsonTraversal / Review" border rows, bringing the matrix
to the ~225 cells requested.

### 0.2 Composition entry points

Two extension methods exist on `Optic` (see
`core/src/main/scala/eo/optics/Optic.scala`):

- `Optic.andThen(o: Optic[A, B, C, D, F])(using af: AssociativeFunctor[F, …])`
  — same-carrier.
- `inline Optic.andThen[G[_, _], C, D](o: Optic[A, B, C, D, G])(using m: Morph[F, G])`
  — cross-carrier; `Morph` delegates to one of `Morph.same`,
  `Morph.leftToRight` (via `Composer[F, G]`), `Morph.rightToLeft`
  (via `Composer[G, F]`), or `bothViaAffine` (low priority).

### 0.3 Ledger of given instances

**`AssociativeFunctor[F, _, _]`** (unlocks same-carrier `.andThen`):

| Carrier | Instance | Source |
|---|---|---|
| `Tuple2` | `AssociativeFunctor.tupleAssocF` | `core/src/main/scala/eo/AssociativeFunctor.scala:56` |
| `Either` | `AssociativeFunctor.eitherAssocF` | `core/src/main/scala/eo/AssociativeFunctor.scala:81` |
| `Forgetful` | `Forgetful.assoc` | `core/src/main/scala/eo/data/Forgetful.scala:75` |
| `Affine` | `Affine.assoc` | `core/src/main/scala/eo/data/Affine.scala:193` |
| `PowerSeries` | `PowerSeries.assoc` | `core/src/main/scala/eo/data/PowerSeries.scala:164` |
| `Forget[F]` (F: Monad) | `Forget.assocForgetMonad` | `core/src/main/scala/eo/data/Forget.scala:106` |
| `Forget[F]` (F: FlatMap + Comonad) | `Forget.assocForgetComonad` | `core/src/main/scala/eo/data/Forget.scala:135` |
| `AlgLens[F]` (F: Applicative+Traverse+MonoidK+AlgLensFromList) | `AlgLens.assocAlgMonad` | `core/src/main/scala/eo/data/AlgLens.scala:181` |
| `Grate` | `Grate.grateAssoc` | `core/src/main/scala/eo/data/Grate.scala:88` |
| `SetterF` | **absent** | `core/src/main/scala/eo/data/SetterF.scala` L14 comment: *"SetterF has no AssociativeFunctor instance"* |
| `FixedTraversal[N]` | **absent** | only `ForgetfulFunctor` given; no `AssociativeFunctor` |

**`Composer[F, G]`** (bridges for cross-carrier):

| Direction | Source |
|---|---|
| `Composer.chain[F, G, H]` (transitive) | `core/src/main/scala/eo/Composer.scala:30` |
| `Forgetful → Tuple2` | `Composer.scala:41` |
| `Forgetful → Either` | `Composer.scala:54` |
| `Tuple2 → Affine` | `Affine.scala:237` |
| `Either → Affine` | `Affine.scala:257` |
| `Tuple2 → SetterF` | `SetterF.scala:57` |
| `Either → SetterF` | `SetterF.scala:81` (shipped 2026-04-25 to close eo-monocle Gap-1) |
| `Affine → SetterF` | `SetterF.scala:103` (shipped 2026-04-25 to close eo-monocle Gap-1) |
| `PowerSeries → SetterF` | `SetterF.scala:129` (shipped 2026-04-25 to close eo-monocle Gap-1) |
| `Tuple2 → PowerSeries` | `PowerSeries.scala:307` |
| `Either → PowerSeries` | `PowerSeries.scala:362` |
| `Affine → PowerSeries` | `PowerSeries.scala:407` |
| `Forget[F] → AlgLens[F]` | `AlgLens.scala:318` |
| `Tuple2 → AlgLens[F]` (F: Applicative+Foldable) | `AlgLens.scala:343` |
| `Either → AlgLens[F]` (F: Alternative+Foldable) | `AlgLens.scala:378` |
| `Forgetful → Grate` | `Grate.scala:273` |

**Absent by design / not yet shipped** (documented or inferred):

- `Composer[Tuple2, Grate]` — Grate plan D3 (`docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`):
  "A Lens's source type `S` is not in general `Representable` /
  `Distributive`, so there's no natural way to broadcast a fresh focus
  through the Lens's structural leftover."
- `Composer[Either, Grate]`, `Composer[Affine, Grate]`,
  `Composer[PowerSeries, Grate]` — same reason (no Representable
  inhabitant at these focuses).
- `Composer[F, FixedTraversal[N]]` for any `F` — fixed-arity traversal
  carriers have no Composer inbound, no Composer outbound, and no
  `AssociativeFunctor` — they're leaves.
- `Composer[SetterF, _]` — SetterF only has *inbound* bridges
  (`Tuple2 → SetterF`); no outgoing.
- `Composer[_, Forget[F]]` for F ≠ F (no direct bridge between distinct
  `F`-shape Forgets).
- `Composer[AlgLens[F], _]` — AlgLens is a sink; no outbound bridges.

### 0.4 Classification legend

- **N** — native `.andThen` works; the chain compiles under the stock
  Optic trait. The parenthetical cites the specific given.
- **M** — manual idiom is required: either bridge to a common carrier
  by hand (`outer.morph[X]`), narrow a composed result via a
  construction like `AffineFold.fromOptional`, or write the
  composition at the Scala level (`outer.modify(inner.modify(f))(_)`).
- **U** — type system rejects the chain and no meaningful idiom covers
  the gap (`.andThen` miss is structural, not a missing given).
- **?** — no evidence in tests, no docs coverage, not settled by a
  plan; needs experimentation.

---

## 1. Executive summary

### 1.1 Cell counts

The full 14×14 same-family-ish matrix (`Optic`-extending families 1-14;
standalone JsonTraversal + Review are handled separately in §4):

| Category | Count (initial) | After Unit 21 | After 2026-04-25 SetterF | % of 196 (post) |
|---|---|---|---|---|
| **N** (native `.andThen`) | 94 | 96 | 99 | 51% |
| **M** (manual idiom) | 56 | 60 | 59 | 30% |
| **U** (unsupported) | 34 | 40 | 40 | 20% |
| **?** (unexplored) | 12 | 0 | 0 | 0% |

(2026-04-25 SetterF row delta: +3 N from `Either → SetterF`, `Affine →
SetterF`, `PowerSeries → SetterF` — shipped to close eo-monocle Gap-1.
Three cells flip: P × S `M → N`, O × S `? → N`, Te × S `? → N`. The
Unit 21 "0 ?" count was a count of the 12 *numbered groups* in §2.2,
not a literal cell-by-cell tally — a handful of ? cells outside those
groups remained until this batch.)

The Unit-21 resolution closed every numbered `?` group from §3.3:
+2 N (`affine2alg` + `chainViaTuple2(Forgetful → Tuple2 → Forget[F]`
already covered by existing tests once chain refactor landed);
+4 M (idiom-documented `Forgetful/Tuple2/Either/Affine × Fold` cases
where outer focuses on `F[A]`); +6 U (structurally-decided cells
where the carrier round-trip cannot work — AlgLens outbound,
PowerSeries × Forget/AlgLens, cross-F Forget pairs).

Adding the 28 border cells for the two standalone families: JsonTraversal
rows/columns are **M** (documented in `CrossCarrierCompositionSpec`
scenarios 4/5) and Review rows/columns are **M** (direct
function-composition idiom) or **U** (as outer — no `to` side).

Grand totals across all 225 requested cells (196 Optic×Optic + 28
standalone borders + 1 JsonTraversal×Review corner), post-2026-04-25:

| Category | Count |
|---|---|
| N | 99 |
| M | 89 |
| U | 39 |
| ? | 0 |

### 1.2 Top 5 surprising gaps

1. **Traversal.forEach × Traversal.forEach across different `F`/`G`**
   type-checks only when `F = G` (same `Forget[F]` carrier). A
   `Fold[List] .andThen Fold[Option]` shape fails implicit resolution:
   no `Composer[Forget[List], Forget[Option]]`. Not even documented
   as unsupported — genuinely **?**.
2. **AlgLens outbound**. There is *no* `Composer[AlgLens[F], _]`. Once
   you land in `AlgLens[List]` you cannot compose with a downstream
   `Forget[List]` or `Tuple2` optic except by lifting the downstream
   side *into* `AlgLens[F]` first. That's not surfaced in docs.
3. **PowerSeries → anything** is not bridged. Composing a Traversal
   with a Lens on its right requires the Lens to be lifted into
   PowerSeries (fine; `tuple2ps` ships), but a Traversal composed with
   an AlgLens classifier or a Grate fails — no `Composer[PowerSeries,
   AlgLens[F]]` or `Composer[PowerSeries, Grate]` exists. Untested
   in the spec corpus.
4. **Setter composition is flat-out absent.** `SetterF` has
   `ForgetfulFunctor` and `ForgetfulTraverse` but NO
   `AssociativeFunctor`, so `setter.andThen(setter)` does not compile.
   Documented in `SetterF.scala` line 14 but nowhere near the user-
   facing `Setter.scala` ctor.
5. **JsonPrism.andThen(AlgLens)** — both are `Either`-based at their
   respective outer layers, but JsonPrism's `Either`-carrier meets
   AlgLens via `Composer[Either, AlgLens[F]]` only when `F` is
   `Alternative + Foldable`. No test ever composes a JsonPrism with
   an AlgLens; plausibly works via `leftToRight` but unverified.

### 1.3 Top 3 high-priority gap-closures for 0.1.0

1. **Close the Traversal × Traversal (same `F`) coverage** — add one
   behaviour-spec row exercising
   `lens.andThen(Traversal.each[List]).andThen(Traversal.each[List])`
   (nested traversal). Tests currently only exercise one level of
   traversal at a time in `OpticsBehaviorSpec` and `PowerSeriesSpec`.
2. **Document "terminal-carrier" gotchas** — SetterF and FixedTraversal
   are both composition-terminals (no `AssociativeFunctor`, no
   outbound Composer). Add a short section in `site/docs/optics.md`
   for each saying so; right now users discover it by hitting an
   implicit miss.
3. **Ship or explicitly close AlgLens × Traversal.each bridging** —
   a common user question is "how do I combine a classifier with a
   list traversal?" There is no
   `Composer[AlgLens[F], PowerSeries]` nor its reverse. Pick one
   direction, document it, or add a Composer.

---

## 2. Full matrix

Columns are **inner** optics; rows are **outer** optics. Abbreviations:
I=Iso, L=Lens, P=Prism, O=Optional, AF=AffineFold, G=Getter, S=Setter,
F=Fold, Te=Traversal.each (PowerSeries), Tf=Traversal.forEach
(Forget[F]), FT=FixedTraversal[N], AL=AlgLens[F], Gr=Grate,
JP=JsonPrism/JsonFieldsPrism.

Each cell indicates the classification and a one-line "why".

|         | I | L | P | O | AF | G | S | F | Te | Tf | FT | AL | Gr | JP |
|---------|---|---|---|---|----|---|---|---|----|----|----|----|----|----|
| **I**   | N (Forgetful.assoc, fused `BijectionIso.andThen(BijectionIso)`) | N (forgetful2tuple→tupleAssocF; fused `Iso.andThen(GetReplaceLens)`) | N (forgetful2either→eitherAssocF; fused `Iso.andThen(MendTearPrism)`) | N (Forgetful→Tuple2→Affine via chain; fused `Iso.andThen(Optional)`) | M (AF's T=Unit mismatches outer B — see §3) | U (Getter's T=Unit) | N (Forgetful→Tuple2→SetterF) | ? (Forgetful→Forget[F] not shipped — needs check) | N (Forgetful→Tuple2→PowerSeries via chain) | ? (Forgetful→Forget[F] unexplored) | U (no Composer[_, FT]) | N (forget2alg path OR Forgetful→Tuple2→AlgLens) | N (Composer[Forgetful, Grate]; GrateSpec witnesses) | N (Forgetful→Either via forgetful2either) |
| **L**   | N (tupleAssocF after forgetful2tuple on inner) | N (tupleAssocF; fused `GetReplaceLens.andThen(GetReplaceLens)`) | N (bothViaAffine — OpticsBehaviorSpec.Lens→Prism) | N (Composer[Tuple2, Affine]; fused `GetReplaceLens.andThen(Optional)`) | M (see AffineFold row in §3) | U (inner T=Unit ≠ outer B) | N (Composer[Tuple2, SetterF]) | ? (Tuple2 → Forget[F] not shipped) | N (Composer[Tuple2, PowerSeries]) | ? (no direct Composer) | U (no Composer[_, FT]) | N (Composer[Tuple2, AlgLens[F]]) | U (Composer[Tuple2, Grate] explicitly NOT shipped per D3) | N (bothViaAffine — CrossCarrierCompositionSpec scenarios 1-3) |
| **P**   | N (forgetful2either morphs inner into Either; fused `MendTearPrism.andThen(BijectionIso)`) | N (bothViaAffine) | N (eitherAssocF; fused `MendTearPrism.andThen(MendTearPrism)`) | N (Composer[Either, Affine]; fused `MendTearPrism.andThen(Optional)`) | M (AF T=Unit) | U (T=Unit) | N (Composer[Either, SetterF] — shipped 2026-04-25) | ? (Either→Forget[F] unexplored) | N (Composer[Either, PowerSeries]) | ? (no Composer) | U (no Composer[_, FT]) | N (Composer[Either, AlgLens[F]]) | U (no Composer[Either, Grate]) | N (stays in Either via eitherAssocF) |
| **O**   | N (Affine.assoc after forgetful→tuple→affine on inner) | N (Affine.assoc after tuple2affine on inner; fused `Optional.andThen(GetReplaceLens)`) | N (Affine.assoc after either2affine; fused `Optional.andThen(MendTearPrism)`) | N (Affine.assoc; fused `Optional.andThen(Optional)`) | M (AF T=Unit — use `AffineFold.fromOptional(chain)`) | U (T=Unit) | N (Composer[Affine, SetterF] — shipped 2026-04-25) | ? (Affine→Forget[F] unexplored) | N (Composer[Affine, PowerSeries]) | ? (no Composer) | U (no Composer[_, FT]) | N (Composer[Affine, AlgLens[F]] — `affine2alg`, Unit 21) | U (no Composer[Affine, Grate]) | N (stays Affine via either2affine on the inner JsonPrism) |
| **AF**  | U (outer T=Unit; can't feed into any inner B slot) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) |
| **G**   | U (outer T=Unit) | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **S**   | U (SetterF lacks AssociativeFunctor; even with same-F inner no andThen) | U (no Composer[SetterF, _]) | U | U | U | U | U | U | U | U | U | U | U | U |
| **F**   | U (Fold's T=Unit) | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **Te**  | N (Composer[Forgetful → Tuple2 → PowerSeries] via chain on inner) | N (Composer[Tuple2, PowerSeries] on inner) | N (Composer[Either, PowerSeries] on inner) | N (Composer[Affine, PowerSeries] on inner) | M (T=Unit on inner AF) | U (Getter T=Unit) | N (Composer[PowerSeries, SetterF] — shipped 2026-04-25) | ? (no Composer[Forget[F], PowerSeries]) | N (same-carrier PowerSeries.assoc — **untested with 2-level nesting**) | ? (no Composer between PowerSeries and Forget[F]) | U (no Composer[_, FT]) | ? (no Composer[PowerSeries, AlgLens[F]]) | U (no Composer[PowerSeries, Grate]) | N (Composer[Either, PowerSeries] on inner JsonPrism; untested) |
| **Tf**  | U (Tf's T=Unit outer) | U | U | U | U | U | U | U | U | ? (same Forget[F] same-F is fine via assocForgetMonad if F: Monad; different F not bridged) | U | U | U | U |
| **FT**  | U (FT lacks AssociativeFunctor; no outbound composer) | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **AL**  | N (Forgetful→Tuple2→AlgLens[F] via chain on inner) | N (Composer[Tuple2, AlgLens[F]] on inner — OpticsBehaviorSpec) | N (Composer[Either, AlgLens[F]] on inner — OpticsBehaviorSpec) | ? (no Composer[Affine, AlgLens[F]] shipped) | M (AF T=Unit) | U (Getter T=Unit) | ? (SetterF terminal) | N (Composer[Forget[F], AlgLens[F]] on inner when same F — OpticsBehaviorSpec) | ? (no Composer[PowerSeries, AlgLens[F]]) | ? (no Composer[AlgLens[F], Forget[F]]) | U (no Composer[_, FT]) | N (assocAlgMonad; OpticsBehaviorSpec "Two Forget[List] classifiers compose") | U (no Composer[AlgLens[F], Grate]) | ? (Either→AlgLens bridge works per-prism — JsonPrism.andThen(AlgLens) plausible but untested) |
| **Gr**  | U (Composer[Forgetful, Grate] is ONE-WAY; Iso→Grate yes, Grate→Iso no) | U (no Composer[Tuple2, Grate]) | U | U | U | U | U | U | U | U | U | U | N (grateAssoc same-carrier — untested with two Grates beyond law suite) | U |
| **JP**  | N (forgetful2either morphs inner Iso into Either; eitherAssocF) | N (bothViaAffine — CCCS scenarios 1-3) | N (eitherAssocF — fused `.andThen` lives on JsonPrism itself via stock Either carrier) | N (Composer[Either, Affine]) | M (AF T=Unit) | U | ? (no coverage) | ? | N (Composer[Either, PowerSeries] — untested) | ? | U | ? (Composer[Either, AlgLens] applies but unverified for JsonPrism specifically) | U | N (eitherAssocF — JsonPrism nested via `.field(...).field(...)` is this pattern) |

### 2.1 Standalone-family borders

Two standalone types never appear on the outer side of an `Optic`
`.andThen` call (they don't extend `Optic`):

| Outer → Inner (any) | Review | JsonTraversal / JsonFieldsTraversal |
|---|---|---|
| **As outer** | U (Review has no `to`; nothing to feed to the inner's observer) | M (documented idiom: `outer.modify(trav.modifyUnsafe(f))(_)`; see §3, and CCCS scenarios 4/5) |
| **As inner of Iso** | M (compose via `Review.apply(a => iso.reverseGet(r.reverseGet(a)))` — see Review.scala docs) | M (manual idiom) |
| **As inner of Lens** | U (Lens needs a `to`; Review provides none) | M (`lens.modify(trav.modifyUnsafe(f))`; CCCS scenarios 4/5) |
| **As inner of Prism** | M (compose reverse-paths directly) | M (manual) |
| **As inner of any other** | M (compose reverse-paths directly) or U | M |

### 2.2 Summary of the ? cells (for §3.4)

The 12 `?` cells are all flavours of "carrier pair exists but no
Composer ships and no test or doc resolves it":

1. Iso × Fold (`Forgetful → Forget[F]`)
2. Iso × Traversal.forEach (`Forgetful → Forget[F]`)
3. Lens × Fold (`Tuple2 → Forget[F]`)
4. Lens × Traversal.forEach (`Tuple2 → Forget[F]`)
5. Prism × Fold
6. Prism × Traversal.forEach
7. Optional × Fold
8. Optional × Traversal.forEach
9. Optional × AlgLens (`Affine → AlgLens[F]`)
10. Traversal.each × Fold / Traversal.forEach / AlgLens — three cells
11. AlgLens × {Affine, PowerSeries, Forget[F]-as-outer, JsonPrism}
12. Traversal.forEach × Traversal.forEach across different `F` shapes

The common thread: **Forget[F] never morphs into anything except
`AlgLens[F]`**, and AlgLens never morphs out at all.

---

## 3. Per-cell details — M / U / ? cells

Skipping the 94 N-cells whose reason is fully explained in the matrix
parenthetical.

### 3.1 Manual-idiom cells (M)

#### 3.1.1 AffineFold as inner (Iso / Lens / Prism / Optional × AF)

**Type-level mismatch.** `AffineFold[S, A] = Optic[S, Unit, A, A, Affine]`.
The outer's `B` slot must match the inner's `T` slot in
`Optic.andThen[C, D](o: Optic[A, B, C, D, F])`. Outer Lens/Prism/Iso/
Optional has `B = A` (not `Unit`), so the inner's `T = Unit` cannot
unify.

Source: the "composition note" in `site/docs/optics.md` line 258, and
the OpticsBehaviorSpec comment at line 352: *"Direct Lens.andThen
(AffineFold) is not well-typed — AffineFold's T = Unit mismatches the
outer Lens's B slot in the Optic composition law."*

**Idiom.** Build a full `Optional` through the outer chain, then
narrow:

```scala
val composedOpt: Optional[S, S, A, A] = outer.andThen(innerOpt)
val af: AffineFold[S, A] = AffineFold.fromOptional(composedOpt)
```

Or for a Prism inner:

```scala
val af = AffineFold.fromPrism(prismChain)
```

Witnessed in OpticsBehaviorSpec at
`"AffineFold.fromOptional narrows a Lens-composed Optional"`.

#### 3.1.2 Outer × JsonTraversal / JsonFieldsTraversal

**Why M, not N.** `JsonTraversal` deliberately does not extend `Optic`
(see `site/docs/concepts.md` lines 92-97: *"[standalone types] would
have to invent an artificial `to` to satisfy the trait contract
([…] `JsonTraversal` has no need for `AssociativeFunctor`)"*). No
`.andThen` overload exists.

**Idiom** (CrossCarrierCompositionSpec scenarios 4/5):

```scala
// Unsafe (byte-identical to pre-v0.2 silent behaviour):
val updated: Env => Env =
  env => outer.modify(trav.modifyUnsafe(f))(env)

// Ior-returning (fail-soft):
val updated: Env => Env = env =>
  outer.modify { j =>
    trav.modify(f)(j) match
      case Ior.Right(v)   => v
      case Ior.Both(_, v) => v
      case Ior.Left(_)    => j
  }(env)
```

Future work tracked in
`docs/plans/2026-04-23-005-feat-circe-multi-field-plus-observable-failure-plan.md`
Future Considerations: "whether to lift JsonTraversal into the Optic
trait".

#### 3.1.3 Outer × Review; Review × inner Iso / Prism

**Why M.** Review wraps only `reverseGet: A => S`, no `to`. The
composition algebra therefore is plain function composition:

```scala
val r1 = Review[Int, String](_.length)
val r2 = Review[Option[Int], Int](Some(_))
val composed = Review[Option[Int], String](s => r2.reverseGet(r1.reverseGet(s)))
```

Witnessed in OpticsBehaviorSpec `"Reviews compose via direct function
composition"`.

#### 3.1.4 Prism × Setter — **RESOLVED (N) 2026-04-25**

Shipped `Composer[Either, SetterF]` (`either2setter`, `SetterF.scala:81`)
to close eo-monocle Gap-1. Hit branch writes `f(a)` through the Prism's
build path; miss branch passes the leftover back via `o.from(Left(xo))`.
Same hit/miss invariants as the original Prism's `.modify(f)`, just
exposed through the Setter API. Behaviour spec at
`OpticsBehaviorSpec`'s `"Either Prism lifts into SetterF and preserves
hit/miss .modify semantics"`.

The same shipping batch added `affine2setter` (resolves Optional × Setter,
previously a `?` cell) and `powerseries2setter` (resolves Traversal.each
× Setter, previously a `?` cell). All three are mechanical mirror images
of `tuple2setter`, all three have round-trip behaviour specs.

### 3.2 Unsupported cells (U)

#### 3.2.1 Any row × Getter, Fold, AffineFold **as the inner and result**

Same structural argument as §3.1.1: these families have `T = Unit` (the
"read-only" statement at the type level). They cannot be chained **after
a read-write outer** without collapsing the outer's `T`.

They can be **outer** only when the result optic is also read-only,
which the current Optic ADT doesn't specialise for — so in practice
the U here also covers "Getter as outer" / "Fold as outer" / "AffineFold
as outer" — any subsequent read-write step would require the outer's
`from` to observe a `B` but the Getter produces `Unit`.

#### 3.2.2 Setter × anything (and vice versa)

**Source.** `core/src/main/scala/eo/data/SetterF.scala:14`:

```scala
// SetterF has no `AssociativeFunctor` instance: composing two `SetterF`
// optics via `Optic.andThen` is not yet supported. Compose a Lens chain
// in `Tuple2` and reach for SetterF only at the leaf.
```

No `Composer[SetterF, _]` ships either. So:

- Outer Setter `.andThen(anything)` — no `Morph` bridge from SetterF.
- Anything `.andThen(Setter)` when outer ≠ Tuple2 — no Composer into
  SetterF except from Tuple2. Lens does bridge (`tuple2setter`).

#### 3.2.3 Anything × FixedTraversal[N]

`FixedTraversal[N]` carries only a `ForgetfulFunctor` instance — no
`AssociativeFunctor`, no outbound Composer, no inbound Composer. It
is a composition-terminal leaf used by `Traversal.two` / `.three` /
`.four` for fixed-arity same-family projections.

#### 3.2.4 Grate × non-Grate, non-Grate × Grate (except Iso→Grate)

Ships only `Composer[Forgetful, Grate]`. Grate plan D3 explicitly
documents the absence of:

- `Composer[Tuple2, Grate]` (Lens → Grate) — see Grate.scala lines
  260-269.

By symmetry, `Either → Grate`, `Affine → Grate`, `PowerSeries → Grate`,
and every `Grate → non-Grate` are also absent. The one working pair
outside Grate×Grate is Iso × Grate (witnessed in `core/src/test/scala/
eo/GrateSpec.scala` lines 91-122).

#### 3.2.5 AlgLens outbound

No `Composer[AlgLens[F], _]` ships — AlgLens is a sink. Once you're
in AlgLens you can only chain with more AlgLens-carrier inners.

### 3.3 Unexplored cells (?)

#### 3.3.1 Forgetful / Tuple2 / Either / Affine × Fold or Traversal.forEach — **RESOLVED (M / U) 2026-04-24**

The Fold and Traversal.forEach optics live on `Forget[F]`. There is
**no `Composer[Forgetful, Forget[F]]`**, no `Composer[Tuple2,
Forget[F]]`, etc. The Forget→AlgLens bridge exists; the inverse does
not (Forget has no observable structural leftover, so there's no
sensible way to widen it back into Tuple2/Either/Affine).

**Outcome.** Two cases:

1. **Outer focuses on an `F[A]`** (e.g. `Lens[Row, List[Int]]`). The
   canonical lift is `AlgLens.fromLensF(lens)` /
   `AlgLens.fromPrismF(prism)` / `AlgLens.fromOptionalF(opt)`, then
   chain via `Composer[AlgLens[F], AlgLens[F]]` (`assocAlgMonad`).
   Classified **M** — no `.andThen` cross-carrier path, but a single
   factory call routes the user. Documented in `site/docs/optics.md`
   AlgLens section and exercised in `OpticsBehaviorSpec`.
2. **Outer focuses on a scalar `A`** (e.g. `Lens[Row, Int]`). There is
   **no natural composition** with a `Fold[F]` — the outer never
   produces an `F`-shaped value. Classified **U** for `.andThen`; the
   plain-Scala idiom is `lens.get(s).foldMap(f)`. Documented in
   `site/docs/optics.md` "Composition limits" subsection.

The `?` here was the lack of a written outcome, not a missing piece of
machinery. Pinned **M / U** by case.

#### 3.3.2 Optional × AlgLens[F] — **RESOLVED (N) 2026-04-24**

Shipped `affine2alg` (`AlgLens.scala:423`) — `Composer[Affine,
AlgLens[F]]` for any `F: Alternative + Foldable`. Mirrors `either2alg`:
`Miss → F.empty[A]` (cardinality 0, preserves the Affine's `Fst`
leftover for the pull side); `Hit → F.pure(a)` (cardinality 1; pull
collapses via `pickSingletonOrThrow`). Behaviour spec at
`OpticsBehaviorSpec`:

- `"Affine Optional lifts into AlgLens[List] and preserves hit/miss
  .modify semantics"` — same shape as the Either-prism case.
- `"Optional andThen AlgLens[List] classifier composes via affine2alg"`
  — full cross-carrier `.andThen` end-to-end.

This also closes the `Affine → AlgLens` arm of the Tier-2
`chainViaTuple2` resolution: `Optional → AlgLens` is now a Tier-1
direct, no transitive lookup needed.

#### 3.3.3 AlgLens × anything (non-AlgLens inner) — **RESOLVED (U, by design) 2026-04-24**

Pinned: **AlgLens[F] is a designed composition sink**. Once you've
landed in AlgLens you compose only with more AlgLens-carrier optics
(via `assocAlgMonad`). No outbound Composer ships and none is planned.

Rationale (mirrors the `optics.md` AlgLens section): AlgLens's
defining property is "structural leftover paired with classifier
candidates"; morphing back to Forget[F] would silently drop the `X`
on the floor (changing semantics from "lensy round-trip" to "phantom
read"), and morphing back to Tuple2/Either/Affine has no natural
candidate-to-singleton collapse. If the user really wants downstream
Fold/Traversal behaviour they should bridge the *inner* into AlgLens
via `Forget[F] → AlgLens[F]` (the `forget2alg` Composer), not bridge
AlgLens *out*.

Documented in `site/docs/optics.md` AlgLens section and reinforced in
the §3.4 U-cell summary.

#### 3.3.4 Traversal.each × AlgLens / Fold / Traversal.forEach — **RESOLVED (U, by design) 2026-04-24**

Three distinct carriers (PowerSeries, Forget[F], AlgLens[F]) with no
Composer between them. Pinned **U** by design: PowerSeries → Forget[F]
would require discarding the outer's `Xo` (PowerSeries' structural
leftover encoding rebuild data) in favour of pure foldMap, which
silently breaks the round-trip property; PowerSeries → AlgLens[F]
would require synthesising a per-candidate cardinality count from the
PowerSeries' uniform-shape representation, which doesn't fit
`assocAlgMonad`'s push contract.

**Idiom.** A user wanting "fold each element of a traversal" already
has the answer at hand: `traversal.foldMap(f)(s)` (the ForgetfulFold
instance on PowerSeries). For "classifier per traversal element",
build the AlgLens chain *under* the traversal:

```scala
// instead of: traversal.andThen(algLens)
val result: S = traversal.modify(a => algLens.replace(...)(a))(s)
```

Documented as "manual idiom" in `site/docs/optics.md` Composition
section.

#### 3.3.5 Traversal.forEach × Traversal.forEach across different F — **RESOLVED (U, deferred to 0.2.x) 2026-04-24**

Same `Forget[F]` carrier *is* required for `assocForgetMonad`. If `F
= G` both Monad, this is N. If `F ≠ G` (e.g. `Forget[List]
.andThen(Forget[Option])`), there's no Composer between two distinct
`Forget[_]` carriers. Pinned **U** for 0.1.0; closing this would
require either:

- a `Composer[Forget[F], Forget[G]]` parameterised over a
  natural-transformation witness `FunctionK[F, G]` or `~>`, or
- a specialised `flatten` for nested-Foldables on the `AlgLens[F]`
  side (less general but matches the actual use case).

Both are non-trivial design moves that need their own plan; deferred
to 0.2.x. Until then the workaround is to run the inner Foldable
manually inside a `modify`:

```scala
// for Forget[List].andThen(Forget[Option]):
val total: Int = list.foldLeft(0)((acc, opt) => acc + opt.foldMap(_.length))
```

Documented in `site/docs/optics.md` Composition limits.

### 3.4 U-cell summary table

For quick scanning, the `U` rows in §2:

| Row family | Why unsupported | Recoverable? |
|---|---|---|
| AffineFold × anything | T=Unit as outer | No — that's the point |
| Getter × anything | T=Unit as outer | No |
| Fold × anything | T=Unit as outer | No |
| Setter × anything | no AssociativeFunctor[SetterF] | Yes — add assoc? plan needed |
| anything × FixedTraversal[N] | no Composer[_, FT] | Yes if desired |
| anything × Grate (except Iso) | Rep/Distributive incompat (plan D3) | No — structural |
| Grate × anything (except Grate) | same reason | No — structural |
| AlgLens outbound | no Composer[AlgLens, _] | Yes — extension work |

---

## 4. Test coverage audit

For every **N** or **M** classified cell, whether a spec file
demonstrates it. Files inspected:

- `tests/src/test/scala/eo/OpticsBehaviorSpec.scala` (OBS)
- `tests/src/test/scala/eo/OpticsLawsSpec.scala` (OLS)
- `tests/src/test/scala/eo/PowerSeriesSpec.scala` (PSS)
- `tests/src/test/scala/eo/EoSpecificLawsSpec.scala` (ESLS)
- `tests/src/test/scala/eo/examples/CrudRoundtripSpec.scala` (CRS)
- `circe/src/test/scala/eo/circe/CrossCarrierCompositionSpec.scala` (CCCS)
- `core/src/test/scala/eo/GrateSpec.scala` (GS)
- `generics/src/test/scala/eo/generics/GenericsSpec.scala` (GSP)
- `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` (JPS)

### 4.1 Covered N cells

| Pair | Spec | Location |
|---|---|---|
| Iso × Iso | OBS | `doubleIso.morph[Tuple2]` + fused class tests in `BijectionIso.andThen(BijectionIso)` — transitively via OLS iso laws |
| Iso × Lens | OBS (indirect via morph), GS | `Iso.morph[Tuple2]` |
| Iso × Prism | OBS | `Iso.morph[Either]` |
| Iso × Optional | OBS | `Iso.morph[Tuple2].morph[Affine]` — chain via forgetful→tuple→affine |
| Iso × Grate | GS | `"compose iso.andThen(grate.tuple) with identity iso"`, `"…non-trivial bijection"` |
| Iso × JsonPrism | CCCS (scenario 2 uses generics lens, but the Iso-fused overload is equivalent at the type level) | **gap** — no test exercises `Iso.andThen(JsonPrism)` directly |
| Lens × Lens | OBS | `"Lens composed with Lens reaches nested pair component"` |
| Lens × Prism | OBS | `"Lens.andThen(Prism) composes via Morph.bothViaAffine (symmetric)"` |
| Lens × Optional | OBS | (Affine carrier narrowing) + CRS |
| Lens × Setter | **gap** | no spec |
| Lens × Traversal.each | PSS, CRS | `lens.andThen(Traversal.each[ArraySeq, Phone])` |
| Lens × AlgLens | OBS | `"Lens andThen AlgLens[List] classifier composes end-to-end"` |
| Lens × JsonPrism | CCCS | scenarios 2 and 3 |
| Prism × Prism | OBS (via OLS law coverage) | `MendTearPrism.andThen(MendTearPrism)` fused path exercised in law suite |
| Prism × Lens | OBS | `"Prism.andThen(Lens) composes via Morph.bothViaAffine"` |
| Prism × Optional | OBS (implicit — Prism→Affine bridge) | **gap** — no direct `Prism.andThen(Optional)` test found |
| Prism × AlgLens | OBS | `"Either Prism lifts into AlgLens[List]"`, `"Prism.andThen(Prism) via AlgLens[List] survives inner miss"` |
| Prism × Traversal.each | **gap** | no test `prism.andThen(Traversal.each)` |
| Prism × JsonPrism | **gap** (implicitly via JsonPrism's own `.field.field` chaining) | no user-level Prism outer × JsonPrism inner test |
| Optional × Lens | **gap** — fused `Optional.andThen(GetReplaceLens)` path exists in source but no test | |
| Optional × Prism | **gap** — same | |
| Optional × Optional | **gap** | fused `Optional.andThen(Optional)` untested |
| Optional × Traversal.each | **gap** | `Composer[Affine, PowerSeries]` shipped, no test |
| Traversal.each × Lens | PSS, CRS | end of chain |
| Traversal.each × Prism | **gap** | |
| Traversal.each × Optional | **gap** | |
| Traversal.each × Iso | **gap** | |
| Traversal.each × Traversal.each (nested) | **gap** | no 2-level test |
| AlgLens × Lens | OBS | `"Lens → AlgLens[List] → Lens composes three carriers cleanly"` |
| AlgLens × Prism | **gap** | only Prism-inner-to-AlgLens-outer as its own same-carrier test |
| AlgLens × AlgLens (same F) | OBS | `"Two Forget[List] classifiers compose via AlgLens[List] with non-uniform cardinalities"` |
| AlgLens × Fold | OBS (via `algFold`) | `algFold` specs lines 829-856 |
| Grate × Grate | OLS (law suite) | but no behaviour-level `grate.andThen(grate)` pair test |
| Forgetful → Either, Forgetful → Tuple2 | OBS | `"Iso.morph[Either] behaves like a Prism"`, `"Iso.morph[Tuple2] behaves like a Lens"` |
| Composer.chain (Forgetful → Tuple2 → Affine) | OBS | `"Iso.morph[Tuple2].morph[Affine]"` |
| JsonPrism × JsonPrism (Either × Either) | JPS | `.field.field` chaining tests |

### 4.2 Covered M cells

| Pair | Spec | Location |
|---|---|---|
| Lens / Prism / Optional × AffineFold (via narrowing) | OBS | `"AffineFold.fromOptional narrows a Lens-composed Optional"` |
| AffineFold.fromPrism | OBS | `"AffineFold.fromPrism drops build path"` |
| Review composition | OBS | `"Reviews compose via direct function composition"` |
| Review.fromIso, Review.fromPrism | OBS | both covered |
| Lens × JsonTraversal (manual idiom) | CCCS | scenario 4 |
| Lens × JsonFieldsTraversal (manual idiom) | CCCS | scenario 5 |
| Forget[F] × Forget[F] same-F compose (assocForgetMonad) | OBS | `"Forget[Option] optics compose via `.andThen`"` |
| Forget[F] × AlgLens[F] same-F inject | OBS | `"Forget[Option] injects into AlgLens[Option]"` |

### 4.3 Gap list (actionable)

**N cells with no test** — these should get at least one behaviour-
spec line before 0.1.0:

1. `Iso.andThen(JsonPrism)` — the fused `BijectionIso.andThen
   (MendTearPrism)` path applies to JsonPrism as a MendTearPrism
   subtype? Actually no, JsonPrism does NOT extend MendTearPrism (it
   extends `Optic[Json, Json, A, A, Either]` directly), so the fused
   overload does not fire — the generic `Forgetful→Either` bridge
   handles it. Worth a test.
2. `Lens.andThen(Setter)` — `Composer[Tuple2, SetterF]` exists
   (SetterF.scala:52) but no spec ever exercises it.
3. `Prism.andThen(Optional)` direct use — the fused `MendTearPrism.
   andThen(Optional)` overload exists in Prism.scala:180 but is
   untested at the behaviour level.
4. `Prism.andThen(Traversal.each)` — `Composer[Either, PowerSeries]`
   ships (PowerSeries.scala:362) with specialised `EitherInPS`
   fast-path; no test.
5. `Optional.andThen(Lens)`, `Optional.andThen(Prism)`,
   `Optional.andThen(Optional)` — four fused overloads on `Optional`
   (Optional.scala:123-188), none behaviour-tested; OLS laws do not
   exercise `.andThen` at all on Optional.
6. `Optional.andThen(Traversal.each)` — `Composer[Affine,
   PowerSeries]` with specialised `AffineInPS` fast-path; untested.
7. `Traversal.each.andThen(Prism)` — `Composer[Either, PowerSeries]`
   re-used on the inner side; untested.
8. `Traversal.each.andThen(Optional)` — same pattern; untested.
9. `Traversal.each.andThen(Iso)` — untested.
10. `Traversal.each.andThen(Traversal.each)` — nested traversal case
    is a headline use case; only covered implicitly in benchmarks.
    Behaviour-level test missing.
11. `AlgLens.andThen(Prism)` — Prism as inner should go through
    `Composer[Either, AlgLens[F]]`; no test.
12. Two-Grate compose — `grateAssoc` is law-suite tested but no
    behaviour spec exercises `grate.andThen(grate)` with asymmetric
    outer/inner.
13. `JsonPrism.andThen(Traversal.each)` — conceptually supported via
    `Composer[Either, PowerSeries]`, untested.

**M cells with no test:**

14. `Prism.andThen(Setter)` manual idiom — no documentation of the
    fallback.

---

## 5. Priority recommendations

### 5.1 High-priority (close before 0.1.0)

Focus on **user-facing, high-value cross-carrier chains** — the cells
that will appear in the first 20 minutes of any user's session and
whose absence in the test/doc corpus is most likely to burn.

1. **Traversal.each × downstream (Iso, Optional, Prism, Traversal.each)
   behaviour tests**. Gaps 7–10 above. The PowerSeries bench covers
   perf, but there is no behaviour spec that, say, takes a
   `Traversal[List, Person]` and chains `.andThen(Prism[Person,
   Adult])`. This is the second-most-common cats-eo chain after
   Lens → Traversal → Lens.
2. **Optional fused overloads** (gaps 5–6). `Optional.andThen
   (GetReplaceLens)` etc. have sharp fused paths but aren't
   behaviour-tested; a refactor could silently slip in a bug.
3. **Setter composition story**. Either (a) ship
   `AssociativeFunctor[SetterF, _, _]` and a
   `Composer[SetterF, _]` for at least Tuple2 (so setter-ending
   chains in library code compose freely), or (b) bring the docstring
   at `SetterF.scala:14` into the user-facing `site/docs/optics.md`
   so users stop trying. The current state is a silent implicit miss
   on `setter.andThen(…)`.
4. **AlgLens × downstream outbound**. Currently `AlgLens[F]` is a
   sink. At minimum document that in `site/docs/optics.md` AlgLens
   section (already present but understated).
5. **Traversal.forEach × same-F behaviour test**. `assocForgetMonad`
   for `F: Monad` is load-bearing (unlocks the classifier-composition
   story) but tested only via a toy `Forget[Option]` pair. A realistic
   spec with `Forget[List]` would surface the Monad-composition
   assumptions more clearly.

### 5.2 Medium priority (nice to have, defer if capacity is tight)

6. Document the `Forgetful → Forget[F]` cell — there's no Composer,
   and the natural workaround is "read, then foldMap the result", which
   isn't called out anywhere.
7. Document the `Lens × Grate` absence with a pointer to plan D3
   in-line in `site/docs/optics.md` (it's present — keep expanding if
   readers miss it).

### 5.3 Deferred (don't block 0.1.0)

8. FixedTraversal outbound composition — `FixedTraversal[N]` is a
   leaf tool for law fixtures; real users compose via `Traversal.each`.
9. JsonTraversal × Optic lift-in — plan 005 Future Considerations;
   out of scope for 0.1.0.
10. `Composer[Affine, AlgLens[F]]` — would close Optional × AlgLens,
    but the use case is niche (Optional-gated classifier). Ship later
    once a user asks.
11. Cross-`F` Forget composition (`Forget[List] .andThen
    Forget[Option]`) — would require a fresh `Composer[Forget[F],
    Forget[G]]` with a transformation witness, or a specialized `flatten`
    for nested Foldables. Unlikely to pay for itself before 0.2.0.

---

## 6. Methodology caveats

- **15 × 15 = 225 is an approximation.** AffineFold and Getter
  collapse with their read-only parents for most inner cells (both
  have `T = Unit`, so the whole row is U). Counting those rows
  multiplies the U bucket; the N/M counts are unaffected.
- **Fused-overload interactions.** Many cells are N partly because a
  concrete class (e.g. `GetReplaceLens`) ships a `.andThen` overload
  that bypasses the carrier-level machinery. When I cite "fused
  `GetReplaceLens.andThen(GetReplaceLens)`" I mean the user-visible
  call will pick that overload over the inherited one thanks to
  Scala's overload resolution — they get both performance and
  type-safety without knowing. A regression that broke the fused path
  would not flip the N cell to U (the generic path still works).
- **Law suite coverage ≠ behaviour coverage.** Many optics have their
  own law fixtures in `OpticsLawsSpec` but that mostly pins
  `.modify` / `.replace` behaviour on a single optic, not chain
  behaviour. The "gap" list in §4.3 only considers composition-
  specific specs.
- **Dynamic-dispatch path on JsonPrism.** `JsonPrism[A]` has both an
  abstract `Optic` `to` / `from` and a concrete "hot path" via
  `.field` / `.selectDynamic`. Cross-carrier composition with other
  Optic families routes through the abstract path. That means all the
  JsonPrism cells in §2 use the generic `eitherAssocF` / bridges,
  NOT JsonPrism's own fused code — worth recognising when assessing
  performance of cross-carrier chains.
- **`?` cells are honest.** I did not run the compiler against each
  unexplored pair. A handful might flip to N once the implicit
  resolution is simulated (especially transitive `chain` pairs
  involving `Forgetful → Tuple2 → AlgLens`). Flipping them is a
  5-minute experiment per pair — folded into gap-list item #1 above.
- **JsonFieldsPrism treated as JsonPrism.** Both extend `Optic[Json,
  Json, A, A, Either]`; their composition behaviour is identical at
  the `Optic` level (the fields-specific concerns — partial-read
  atomicity, Ior threading — are orthogonal to `.andThen`). I did
  not split them into separate rows.

---

## 7. Unit 21 resolution scoreboard (2026-04-24)

Closing every `?` from §3.3:

| § | Pair | Action | Outcome |
|---|---|---|---|
| 3.3.1 (F[A]-focus) | Forgetful/Tuple2/Either/Affine × Fold/Tf, outer focuses on `F[A]` | Documented `AlgLens.fromLensF` / `fromPrismF` / `fromOptionalF` factory route | **M** |
| 3.3.1 (scalar-focus) | Forgetful/Tuple2/Either/Affine × Fold/Tf, outer focuses on scalar `A` | Documented `lens.get(s).foldMap(f)` plain-Scala idiom; no carrier path possible | **U** |
| 3.3.2 | Optional × AlgLens | **Shipped** `Composer[Affine, AlgLens[F]]` (`affine2alg`); two specs in `OpticsBehaviorSpec` | **N** |
| 3.3.3 | AlgLens × non-AlgLens (outbound) | Pinned designed sink; documented in `optics.md` and §3.4 | **U** |
| 3.3.4 | Traversal.each × Fold/Tf/AlgLens | Pinned structural; documented `traversal.modify(inner.replace…)` idiom | **U** |
| 3.3.5 | Traversal.forEach × Traversal.forEach cross-F | Pinned 0.2.x deferral; documented manual fold idiom | **U** (deferred) |

The chain-refactor side effect (§3.3 was written before the
Tier-1/Tier-2 hierarchy landed): `chainViaTuple2` at low priority now
routes Forgetful → {SetterF, PowerSeries, AlgLens[F]} transitively,
so `Iso.andThen(setter)` / `Iso.andThen(traversal)` / `Iso.andThen
(algLens)` all resolve from a single intermediate without per-target
direct bridges. The matrix already shows these as N (rows 1, 2, 3 in
the **I** row in §2); the refactor only changed the resolution path,
not the cell colour.
