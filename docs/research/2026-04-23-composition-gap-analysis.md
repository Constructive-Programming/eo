# Composition-coverage gap analysis

**Date:** 2026-04-23
**Last updated:** 2026-04-30 — `Traversal.forEach` (`Forget[F]`-carrier
"terminal traversal") is removed pre-0.1.0. Matrix shrinks from 13×13
to 12×12; the Tf row + Tf column dissolve entirely (same shape as the
2026-04-29 FixedTraversal[N] fold). The single Traversal carrier is now
`Traversal.each` (`MultiFocus[PSVec]`) — `.modify`, `.foldMap`,
`.modifyA`, and downstream composition all flow through one optic.
`Forget[F]` survives as the `Fold` carrier; only the `Traversal.forEach`
constructor (and its `Forget[T]`-carrier traversal shape) is gone.

**2026-04-29** — `FixedTraversal[N]` folds into
`MultiFocus[Function1[Int, *]]`. Matrix shrinks from 14×14 to 13×13; the
FT row + FT column dissolve into the existing MF column (the FT-shaped
optic is now a `MultiFocus[Function1[Int, *]]` carrier — same row +
column as the absorbed-Grate factories). FT[N]'s former "all U"
row+column becomes a Function1-shaped subrow of MF (Iso → MF inbound,
MF → SetterF outbound, same-carrier `.andThen` via `mfAssocFunction1`).

**2026-04-28** — `AlgLens[F]` + `Kaleidoscope` collapse into the unified
`MultiFocus[F]` carrier. Matrix shrinks from 15×15 to 14×14; the AL and
K columns merge into a single MF column with the combined cell counts.
See §1.1 for the recount and §3.2.4 / §3.2.6 for the post-merge
`MultiFocus[F]` × non-MF skip rationale.

**2026-04-24** — Unit 21 (0.1.0 plan) closed every numbered `?` group
from §3.3 by shipping one new Composer, documenting the idioms, or
pinning a structural-`U` decision. See §7 for the post-resolution
scoreboard.

**Scope:** every (outer × inner) optic-family composition pair currently
shipped in `cats-eo` (core + circe), classified by whether `.andThen`
works natively, requires a manual idiom, is unsupported, or is
unexplored.

## 0. Methodology

### 0.1 Families covered

The post-MultiFocus-unification rows are:

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
|  9 | Traversal.each | `MultiFocus[PSVec]` | `Optic[…, MultiFocus[PSVec]]` |
| 10 | MultiFocus[F] | `MultiFocus[F]` | `Optic[…, MultiFocus[F]]` (unified successor of `AlgLens[F]` + `Kaleidoscope`; absorbs `FixedTraversal[N]` at `F = Function1[Int, *]` via `Traversal.{two,three,four}`; `Traversal.each` is the `F = PSVec` sub-shape) |
| 11 | Grate | `Grate` | `Optic[…, Grate]` |
| 12 | JsonPrism / JsonFieldsPrism | `Either` | `Optic[Json, Json, A, A, Either]` |
| 13 | JsonTraversal / JsonFieldsTraversal | — | standalone, not an `Optic` |
| 14 | Review | — | standalone, not an `Optic` |

That is **14 row labels**, but rows 13–14 do not extend `Optic`, so
they can only appear as outer or inner of an idiom-level composition.
For the 12 `Optic`-bearing families above (1–12) we produce a 12×12
inner-matrix (144 cells) and then add the standalone-family border
rows, bringing the matrix to ~170 cells.

The pre-2026-04-30 row count was 13 (with a separate `Traversal.forEach`
row on the `Forget[F]` carrier). The drop dissolves the Tf row + Tf
column entirely (same shape as the FixedTraversal[N] fold one day
earlier): `Traversal.each` (`MultiFocus[PSVec]`) covers every Tf use
case, `.foldMap` is the read-only escape, and same-carrier `.andThen`
flows through `mfAssocPSVec`. The pre-2026-04-29 row count was 14 (with
a separate `FixedTraversal[N]` row carrying "all U" — leaf carrier with
no Composer or AssociativeFunctor). The fold collapses FT into
`MultiFocus[Function1[Int, *]]` (the absorbed-Grate sub-shape of MF):
`Traversal.{two,three,four}` now route through `MF[Function1[Int, *]]`
exactly as `MultiFocus.tuple` does, gaining `Iso → MF` inbound,
`MF → SetterF` outbound, and same-carrier `.andThen` via
`mfAssocFunction1`. The pre-2026-04-28 row count was 15 (with separate
`AlgLens[F]` and `Kaleidoscope` rows); see §1.1.

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
| `MultiFocus[F]` (F: Traverse+MultiFocusFromList) | `MultiFocus.mfAssoc` | `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` |
| `MultiFocus[Function1[X0, *]]` | `MultiFocus.mfAssocFunction1` | `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` (covers absorbed-Grate `tuple` factory and absorbed `FixedTraversal[N]` `Traversal.{two,three,four}`) |
| `MultiFocus[PSVec]` | `MultiFocus.mfAssocPSVec` | `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` |
| `Grate` | `Grate.grateAssoc` | `core/src/main/scala/eo/data/Grate.scala:88` |
| `SetterF` | **absent** | `core/src/main/scala/eo/data/SetterF.scala` L14 comment: *"SetterF has no AssociativeFunctor instance"* |

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
| `Forget[F] → MultiFocus[F]` | `MultiFocus.scala` |
| `Tuple2 → MultiFocus[F]` (F: Applicative+Foldable) | `MultiFocus.scala` |
| `Either → MultiFocus[F]` (F: Alternative+Foldable) | `MultiFocus.scala` |
| `Affine → MultiFocus[F]` (F: Alternative+Foldable) | `MultiFocus.scala` |
| `Forgetful → MultiFocus[F]` (F: Applicative+Foldable) | `MultiFocus.scala` |
| `MultiFocus[F] → SetterF` | `MultiFocus.scala` (collapses both `kaleidoscope2setter` and the prior latent `alg2setter`) |
| `Forgetful → Grate` | `Grate.scala:273` |
| `Grate → SetterF` | `Grate.scala` (shipped 2026-04-27 — closes Gr × S) |

**Absent by design / not yet shipped** (documented or inferred):

- `Composer[Tuple2, Grate]` — Grate plan D3 (`docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`):
  "A Lens's source type `S` is not in general `Representable` /
  `Distributive`, so there's no natural way to broadcast a fresh focus
  through the Lens's structural leftover."
- `Composer[Either, Grate]`, `Composer[Affine, Grate]`,
  `Composer[PowerSeries, Grate]` — same reason (no Representable
  inhabitant at these focuses).
- `Composer[SetterF, _]` — SetterF only has *inbound* bridges
  (`Tuple2 → SetterF`); no outgoing.
- `Composer[_, Forget[F]]` for F ≠ F (no direct bridge between distinct
  `F`-shape Forgets).
- `Composer[MultiFocus[F], _]` — MultiFocus is a sink (with the sole
  exception of `MultiFocus[F] → SetterF`).

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

The post-Tf-drop 12×12 matrix (`Optic`-extending families 1-12;
standalone JsonTraversal + Review are handled separately in §4):

| Category | Pre-MF-merge (15×15 = 225 cells) | Post-MF-merge (14×14 = 196 cells) | Post-FT-fold (13×13 = 169 cells) | Post-Tf-drop (12×12 = 144 cells) | % of 144 (current) |
|---|---|---|---|---|---|
| **N** (native `.andThen`) | 103 | 95 | 95 | 95 | 66% |
| **M** (manual idiom) | 59 | 57 | 57 | 49 | 34% |
| **U** (unsupported) | 63 | 44 | 17 | 0 | 0% |
| **?** (unexplored) | 0 | 0 | 0 | 0 | 0% |

**Tf-drop delta (2026-04-30).** The pre-drop Tf row was 13 U cells
(Tf's `T = Unit` makes it inhabit-only-as-inner; same shape as the
Fold row); the Tf column carried the bridge gaps "Forget[T] never
morphs into anything except `MultiFocus[F]`" — 7 U + 5 cells that §3.3
classified M (outer × Fold/Tf with `F[A]` focus, route via
`MultiFocus.fromLensF`/etc) or U (scalar focus). Net footprint:
13 (row) + 12 (column, Tf×Tf corner already counted) = 25 cells. All
17 pre-drop U cells lived in the Tf row (13) + the 4 hard-U Tf-column
cells (AF, G, S, F outers × Tf inner — read-only outers can't feed Tf's
focus); their disappearance brings the U bucket to 0. The Tf-column M
cells (Iso/L/P/O × Tf when focus is `F[A]`, Te × Tf, JP × Tf) collapse
into the matching Iso/L/P/O × Fold and Te × Fold and JP × Fold cells
already documented as M in §3.3.1, with no new manual-idiom shape.
Net change: −25 cells, −17 U, −8 M, 0 N delta.

**FT-fold delta (2026-04-29).** The pre-fold FT row carried 14 cells
all U (FT lacks `AssociativeFunctor` and any outbound Composer); the
FT column carried 14 cells all U (no Composer inbound for any outer).
Total FT footprint: 14 + 14 − 1 (the FT×FT corner counted once) = 27
cells, all U. The fold dissolves both into the absorbed-Grate sub-
shape of MF (`F = Function1[Int, *]`):

- The FT row dissolves entirely. The MF row already covers FT's row
  obligations (the Function1 sub-shape inherits MF row's N/M cells
  when the F-constraint is met, U otherwise — and the per-cell text
  in §2 calls those constraints out inline).
- The FT column dissolves entirely. Where MF has an inbound Composer
  for a given outer (e.g. Iso → MF via `forgetful2multifocus*`), the
  FT-shaped optic now picks up that composition via the same bridge
  (`forgetful2multifocusFunction1`). Where MF lacks one (Lens / Prism
  / Optional inbound, Function1 case — `Function1[X0, *]` admits no
  Foldable / Alternative), the cell stays U with the constraint
  explicit.

Net: **−27 cells, all U**. Pre-fold U=44 → post-fold U=17. N and M
counts are preserved exactly. **Composability win on the user side**:
the user-facing FT-shaped optics (`Traversal.{two,three,four}`) now
inherit `Iso → MF` inbound, `MF → SetterF` outbound, and same-carrier
`.andThen` via `mfAssocFunction1` — three cells that were "U (no
Composer[_, FT])" pre-fold now light up.

**The merge math.** The pre-merge matrix had two separate
classifier-shape rows / columns: AL (`AlgLens[F]`) and K
(`Kaleidoscope`). Each had its own 15-cell row and 15-cell column for
a total of 30 row-cells + 30 column-cells − 4 corner cells (AL × AL,
AL × K, K × AL, K × K) = 56 cells distributed across the two rows and
two columns. Collapsing AL ∪ K → MF (single row, single column)
produces 14 row-cells + 14 column-cells − 1 corner (MF × MF) = 27
cells. Net change: 56 − 27 = **29 cells removed**, matching the
matrix-shape delta 225 − 196 = 29.

Cell-by-cell merge rules (every (outer, AL) cell is the same `(X,
F[A])` value-shape composition as the corresponding (outer, K) cell at
the same F, so the inherited classification is identical):

- **(outer × MF)** — every row gets the AL classification, which was
  N for the inbound carriers (Iso, Lens, Prism, Optional, Forget[F])
  and U for the rest. The K column was U everywhere except Iso × K
  (N via the same `forgetful2multifocus` bridge) and same-carrier
  K × K. No information is lost on the merge.
- **(MF × inner)** — sink shape. Carries the AL row's N for `MF × MF`
  (same-carrier `mfAssoc`), N for `MF × SetterF` (the new
  `multifocus2setter` collapses the prior `kaleidoscope2setter` and
  the latent `alg2setter`), and U for everything else. No `MF →
  Forgetful` (would create a bidirectional pair with
  `forgetful2multifocus`); no `MF → Forget[F]` (the carrier-shaped
  bridge can't thread a `Foldable[F]` witness).

(2026-04-28 MultiFocus-merge delta from the immediately prior matrix:
**29 cells removed**; **−8 N** (the 4 K-row cells that were N: I × K,
K × K, K × S, plus the 4 AL-row cells that were N at I/L/P/O × AL
collapse into the merged row's I/L/P/O × MF); **−2 M** (no AL × M
cells survive separately — they collapse into MF × M); **−19 U** (the
remaining 26 K-cells that were U + the same-shape AL-column cells
collapse into the single MF row/column's 22 U cells; see "merge math"
above).

Earlier deltas (kept for change-log continuity):

(2026-04-25 SetterF row delta: +3 N from `Either → SetterF`, `Affine →
SetterF`, `PowerSeries → SetterF` — shipped to close eo-monocle Gap-1.
Three cells flip: P × S `M → N`, O × S `? → N`, Te × S `? → N`. The
Unit 21 "0 ?" count was a count of the 12 *numbered groups* in §2.2,
not a literal cell-by-cell tally — a handful of ? cells outside those
groups remained until this batch.)

(2026-04-27 Grate-row delta: +1 N from `Composer[Grate, SetterF]`
(`grate2setter`, `Grate.scala`). One cell flips: Gr × S `U → N`. The
two adjacent candidates investigated in the same batch
(`Composer[Grate, Forgetful]` and `Composer[Grate, Forget[F]]`) were
rejected as structurally unsound — see §3.2.4 for the resolution
rationale.)

The Unit-21 resolution closed every numbered `?` group from §3.3:
+2 N (`affine2alg` + `chainViaTuple2(Forgetful → Tuple2 → Forget[F]`
already covered by existing tests once chain refactor landed);
+4 M (idiom-documented `Forgetful/Tuple2/Either/Affine × Fold` cases
where outer focuses on `F[A]`); +6 U (structurally-decided cells
where the carrier round-trip cannot work — MultiFocus outbound,
PowerSeries × Forget/MultiFocus, cross-F Forget pairs).

Adding the border cells for the two standalone families: JsonTraversal
rows/columns are **M** (documented in `CrossCarrierCompositionSpec`
scenarios 4/5) and Review rows/columns are **M** (direct
function-composition idiom) or **U** (as outer — no `to` side).

Grand totals across all post-Tf-drop cells (144 Optic×Optic + standalone
borders), post-2026-04-30:

| Category | Count |
|---|---|
| N | 95 |
| M | 77 |
| U | 0 |
| ? | 0 |

### 1.2 Top surprising gaps

1. **MultiFocus outbound** (other than to SetterF). There is no
   `Composer[MultiFocus[F], Forgetful]` / `Composer[MultiFocus[F],
   Forget[G]]` / `Composer[MultiFocus[F], Tuple2]`. Once you land in
   `MultiFocus[List]` you can only widen to `SetterF` or stay in
   MultiFocus. The carrier is otherwise a sink — same shape as Grate's
   isolation profile, one notch less surprising than the pre-merge
   `AlgLens[F]` outbound + `Kaleidoscope` outbound double-headed gap.
2. **MultiFocus[F] × MultiFocus[G] across different `F` / `G`**.
   `mfAssoc` requires the same `F` on both sides. Composing
   `MultiFocus[List]` with `MultiFocus[Option]` fails implicit
   resolution: no `Composer[MultiFocus[List], MultiFocus[Option]]`.
   Closing this would need a `FunctionK`-witness Composer or a
   specialised nested-Foldable flatten; both are non-trivial design
   moves deferred to 0.2.x.
3. **MultiFocus[PSVec] (= `Traversal.each`) → Grate** is not bridged.
   Composing a Traversal with a Lens on its right works (lifts via
   `tuple2multifocus`), but a Traversal composed with a Grate fails —
   no `Composer[MultiFocus[PSVec], Grate]` exists. Same structural
   reason as Lens × Grate: a `MultiFocus[PSVec]` source isn't in
   general `Representable`.
4. **Setter composition is flat-out absent.** `SetterF` has
   `ForgetfulFunctor` and `ForgetfulTraverse` but NO
   `AssociativeFunctor`, so `setter.andThen(setter)` does not compile.
   Documented in `SetterF.scala` line 14 but nowhere near the user-
   facing `Setter.scala` ctor.
5. **JsonPrism.andThen(MultiFocus)** — both are `Either`-based at
   their respective outer layers, but JsonPrism's `Either`-carrier
   meets MultiFocus via `Composer[Either, MultiFocus[F]]` only when
   `F` is `Alternative + Foldable`. No test ever composes a JsonPrism
   with a MultiFocus; plausibly works via `leftToRight` but unverified.

### 1.3 Top 3 high-priority gap-closures for 0.1.0

1. **Close the Traversal × Traversal (same `F`) coverage** — add one
   behaviour-spec row exercising
   `lens.andThen(Traversal.each[List]).andThen(Traversal.each[List])`
   (nested traversal). Tests currently only exercise one level of
   traversal at a time in `OpticsBehaviorSpec` and `PowerSeriesSpec`.
2. **Document "terminal-carrier" gotchas** — SetterF is a
   composition-terminal (no `AssociativeFunctor`, no outbound
   Composer). Add a short section in `site/docs/optics.md` saying so;
   right now users discover it by hitting an implicit miss.
   (`FixedTraversal[N]` was previously a second terminal — folded into
   `MultiFocus[Function1[Int, *]]` 2026-04-29, which inherits the
   absorbed-Grate composability profile.)
3. **Ship or explicitly close MultiFocus[F] × MultiFocus[G] cross-`F`
   bridging** — a common user question is "how do I combine a
   classifier with a list traversal?" Post-Tf-drop, `Traversal.each`
   IS a `MultiFocus[PSVec]`, so this collapses into the wider
   `MultiFocus[F] × MultiFocus[G]` cross-`F` gap (`F ≠ G`). No
   `Composer[MultiFocus[F], MultiFocus[G]]` exists; closing this would
   need a `FunctionK`-witness Composer or a specialised nested-Foldable
   flatten. Pick one direction, document it, or add a Composer.

---

## 2. Full matrix

Columns are **inner** optics; rows are **outer** optics. Abbreviations:
I=Iso, L=Lens, P=Prism, O=Optional, AF=AffineFold, G=Getter, S=Setter,
F=Fold, Te=Traversal.each (`MultiFocus[PSVec]`), MF=MultiFocus[F] (also
carries the absorbed FixedTraversal[N] sub-shape at
`F = Function1[Int, *]`; the Tf-shaped optic merged into Te at the
`PSVec` sub-shape on 2026-04-30), Gr=Grate,
JP=JsonPrism/JsonFieldsPrism.

Each cell indicates the classification and a one-line "why".

|         | I | L | P | O | AF | G | S | F | Te | MF | Gr | JP |
|---------|---|---|---|---|----|---|---|---|----|----|----|----|
| **I**   | N (Forgetful.assoc, fused `BijectionIso.andThen(BijectionIso)`) | N (forgetful2tuple→tupleAssocF; fused `Iso.andThen(GetReplaceLens)`) | N (forgetful2either→eitherAssocF; fused `Iso.andThen(MendTearPrism)`) | N (Forgetful→Tuple2→Affine via chain; fused `Iso.andThen(Optional)`) | M (AF's T=Unit mismatches outer B — see §3) | U (Getter's T=Unit) | N (Forgetful→Tuple2→SetterF) | ? (Forgetful→Forget[F] not shipped — needs check) | N (Forgetful→MultiFocus[PSVec] via `forgetful2multifocus`) | N (forgetful2multifocus direct; `forgetful2multifocusFunction1` covers the absorbed-FT sub-shape) | N (Composer[Forgetful, Grate]; GrateSpec witnesses) | N (Forgetful→Either via forgetful2either) |
| **L**   | N (tupleAssocF after forgetful2tuple on inner) | N (tupleAssocF; fused `GetReplaceLens.andThen(GetReplaceLens)`) | N (bothViaAffine — OpticsBehaviorSpec.Lens→Prism) | N (Composer[Tuple2, Affine]; fused `GetReplaceLens.andThen(Optional)`) | M (see AffineFold row in §3) | U (inner T=Unit ≠ outer B) | N (Composer[Tuple2, SetterF]) | ? (Tuple2 → Forget[F] not shipped) | N (Composer[Tuple2, MultiFocus[F]] at `F = PSVec`) | N (Composer[Tuple2, MultiFocus[F]] — `tuple2multifocus`; the absorbed-FT sub-shape `MF[Function1[Int, *]]` lacks the inbound Lens bridge — Function1 has no Foldable, same constraint as v1 Grate plan D3) | U (Composer[Tuple2, Grate] explicitly NOT shipped per D3) | N (bothViaAffine — CrossCarrierCompositionSpec scenarios 1-3) |
| **P**   | N (forgetful2either morphs inner into Either; fused `MendTearPrism.andThen(BijectionIso)`) | N (bothViaAffine) | N (eitherAssocF; fused `MendTearPrism.andThen(MendTearPrism)`) | N (Composer[Either, Affine]; fused `MendTearPrism.andThen(Optional)`) | M (AF T=Unit) | U (T=Unit) | N (Composer[Either, SetterF] — shipped 2026-04-25) | ? (Either→Forget[F] unexplored) | N (Composer[Either, MultiFocus[F]] at `F = PSVec`) | N (Composer[Either, MultiFocus[F]] — `either2multifocus`; sub-shape `MF[Function1[Int, *]]` lacks the bridge — Function1 has no Alternative) | U (no Composer[Either, Grate]) | N (stays in Either via eitherAssocF) |
| **O**   | N (Affine.assoc after forgetful→tuple→affine on inner) | N (Affine.assoc after tuple2affine on inner; fused `Optional.andThen(GetReplaceLens)`) | N (Affine.assoc after either2affine; fused `Optional.andThen(MendTearPrism)`) | N (Affine.assoc; fused `Optional.andThen(Optional)`) | M (AF T=Unit — use `AffineFold.fromOptional(chain)`) | U (T=Unit) | N (Composer[Affine, SetterF] — shipped 2026-04-25) | ? (Affine→Forget[F] unexplored) | N (Composer[Affine, MultiFocus[F]] at `F = PSVec`) | N (Composer[Affine, MultiFocus[F]] — `affine2multifocus`; sub-shape `MF[Function1[Int, *]]` lacks the bridge — Function1 has no Alternative) | U (no Composer[Affine, Grate]) | N (stays Affine via either2affine on the inner JsonPrism) |
| **AF**  | U (outer T=Unit; can't feed into any inner B slot) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) |
| **G**   | U (outer T=Unit) | U | U | U | U | U | U | U | U | U | U | U |
| **S**   | U (SetterF lacks AssociativeFunctor; even with same-F inner no andThen) | U (no Composer[SetterF, _]) | U | U | U | U | U | U | U | U | U | U |
| **F**   | U (Fold's T=Unit) | U | U | U | U | U | U | U | U | U | U | U |
| **Te**  | N (Forgetful→MultiFocus[PSVec] via `forgetful2multifocus` on inner) | N (Composer[Tuple2, MultiFocus[PSVec]] on inner) | N (Composer[Either, MultiFocus[PSVec]] on inner) | N (Composer[Affine, MultiFocus[PSVec]] on inner) | M (T=Unit on inner AF) | U (Getter T=Unit) | N (Composer[MultiFocus[F], SetterF] — `multifocus2setter` — at `F = PSVec`) | N (Composer[Forget[F], MultiFocus[F]] on inner when same F; covers Te × Fold[PSVec] — Te × Fold[F] for `F ≠ PSVec` falls through to the same-F bridge gap) | N (same-carrier `mfAssocPSVec` — same as MF × MF at `F = PSVec`) | U (no Composer between distinct MultiFocus[F] / MultiFocus[G] carriers) | U (no Composer between MultiFocus[PSVec] and Grate) | ? (Either→MultiFocus bridge works per-prism — JsonPrism.andThen(Te) plausible but untested) |
| **MF**  | U (no Composer[MultiFocus[F], Forgetful] — would shadow `forgetful2multifocus`; see §3.2.6) | N (Composer[Tuple2, MultiFocus[F]] on inner — OpticsBehaviorSpec) | N (Composer[Either, MultiFocus[F]] on inner — OpticsBehaviorSpec) | N (Composer[Affine, MultiFocus[F]] on inner — `affine2multifocus`) | M (AF T=Unit) | U (Getter T=Unit) | N (Composer[MultiFocus[F], SetterF] — `multifocus2setter` — covers all F including Function1[Int, *]) | N (Composer[Forget[F], MultiFocus[F]] on inner when same F — OpticsBehaviorSpec) | U (no Composer[MultiFocus[F], MultiFocus[PSVec]] when `F ≠ PSVec`) | N (mfAssoc / mfAssocPSVec / `mfAssocFunction1` same-carrier; the FT-absorbed `MF[Function1[Int, *]]` self-compose lights up `mfAssocFunction1`'s Z=(Xo, Xi) closure-rebuild path) | U (no Composer between MultiFocus and Grate in either direction) | ? (Either→MultiFocus bridge works per-prism — JsonPrism.andThen(MultiFocus) plausible but untested) |
| **Gr**  | U (Composer[Forgetful, Grate] is ONE-WAY; Iso→Grate yes, Grate→Iso no — see §3.2.4) | U (no Composer[Tuple2, Grate]) | U | U | U | U | N (Composer[Grate, SetterF] — `grate2setter`, Grate.scala; shipped 2026-04-27) | U | U | U (no Composer between Grate and MultiFocus in either direction) | N (grateAssoc same-carrier — untested with two Grates beyond law suite) | U |
| **JP**  | N (forgetful2either morphs inner Iso into Either; eitherAssocF) | N (bothViaAffine — CCCS scenarios 1-3) | N (eitherAssocF — fused `.andThen` lives on JsonPrism itself via stock Either carrier) | N (Composer[Either, Affine]) | M (AF T=Unit) | U | ? (no coverage) | ? | N (Composer[Either, MultiFocus[PSVec]] on the Te inner — untested) | M (Composer[Either, MultiFocus[F]] resolves type-level, see CCCS scenario 6; value-level multi-element drilling routes via `Traversal.each` / `mfAssocPSVec` — `pickSingletonOrThrow` in `either2multifocus.from` doesn't handle non-singleton fb) | U | N (eitherAssocF — JsonPrism nested via `.field(...).field(...)` is this pattern) |

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

The remaining `?` cells are all flavours of "carrier pair exists but
no Composer ships and no test or doc resolves it":

1. Iso × Fold (`Forgetful → Forget[F]`)
2. Lens × Fold (`Tuple2 → Forget[F]`)
3. Prism × Fold
4. Optional × Fold
5. JsonPrism × Setter / Fold / MultiFocus — three cells covered by
   the inherited Either-row, but unverified for JsonPrism specifically.

The common thread: **Forget[F] never morphs into anything except
`MultiFocus[F]`**, and MultiFocus only morphs out to SetterF.

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

#### 3.2.3 Anything × FixedTraversal[N] — RESOLVED (folded into MultiFocus)

**2026-04-29 update.** `FixedTraversal[N]` is dissolved into
`MultiFocus[Function1[Int, *]]` (the absorbed-Grate sub-shape of
`MultiFocus[F]`). `Traversal.{two,three,four}` now construct
`MF[Function1[Int, *]]`-carrier optics; the FT row + column collapse
into the existing MF row + column. The FT-shaped composability profile
inherits the absorbed-Grate sub-shape's bridges:

- `Iso → MF[Function1[Int, *]]` via `forgetful2multifocusFunction1`. ✓
- `MF[Function1[Int, *]] → SetterF` via `multifocus2setter` (Functor[F]
  generic). ✓
- Same-carrier `.andThen` via `mfAssocFunction1`. ✓
- `Lens / Prism / Optional → MF[Function1[Int, *]]` — NOT shipped:
  `Function1[X0, *]` admits neither `Foldable` nor `Alternative`, so
  the generic `tuple2multifocus` / `either2multifocus` /
  `affine2multifocus` instance constraints are not met. Same
  structural restriction as v1 Grate plan D3 — the leftover would
  need a `MonoidK[F]` / `Alternative[F]` to fail-soft.

Net effect: cells where FT was the inner (e.g. `L × FT = U`) become the
same as MF cells with the F=Function1[Int, *] caveat noted inline.

#### 3.2.4 Grate × non-Grate, non-Grate × Grate

**2026-04-27 update — Gr × S resolved (N).** Shipped `Composer[Grate,
SetterF]` (`grate2setter`, `Grate.scala`); the Gr × S cell flips from
**U** to **N**. The bridge collapses Grate's broadcast pattern to the
Setter API by reusing the existing `(a, k) = o.to(s)` /
`o.from((f(a), k.andThen(f)))` shape from [[grateFunctor]]. Witnessed by
`EoSpecificLawsSpec` (MorphLaws.A1 on the lifted Grate.tuple) and
`OpticsBehaviorSpec` (per-slot broadcast behaviour on tuple Grates and
on `Grate.apply[Function1[Boolean, *]]`). Like every other
`Composer[X, SetterF]`, this does NOT enable `grate.andThen(setter)`
directly — SetterF lacks `AssociativeFunctor` by design — but it does
unlock the morph-site value (`grate.morph[SetterF]`, `eo-monocle`-style
interop, uniform Setter-shaped extension points).

**Two further candidates investigated and rejected as structurally
unsound** (rationale also lives at the bottom of `Grate.scala` so the
investigation isn't re-spent):

- **`Composer[Grate, Forgetful]` (Grate widens to Iso/Getter).**
  Type-level the bridge encodes (`to: S => A` reads the focus,
  `from: B => T` calls `o.from((b, _ => b))`). However,
  [[forgetful2grate]] already ships in the OPPOSITE direction. Adding
  the reverse would create a bidirectional Composer pair, which the
  `Morph` resolution explicitly forbids: both `Morph.leftToRight` and
  `Morph.rightToLeft` would match for any `Iso × Grate` pair, surfacing
  as ambiguous-implicit and breaking every existing `iso.andThen(grate)`
  call site (witnessed in [[GrateSpec]]). Cats-eo's resolution invariant
  *"we don't ship bidirectional composers"* is the deciding constraint,
  NOT the type-level encodability. Workaround: `grate.to(s)._1` for the
  read, or `Getter(s => grate.to(s)._1)` when interop demands a Getter
  shape.

- **`Composer[Grate, Forget[F]]` (Grate widens to Traversal/Fold).**
  Generic in `S, T, A, B`. The target carrier `Forget[F][X, A] = F[A]`
  forces the morphed `to` to produce `F[A]` from arbitrary `S`, and the
  morphed `from` to consume `F[B]`. There is no path from a generic
  Grate's `(A, X => A)` to an `F[A]` because (a) the existential `X` is
  unrelated to `F` (the `Grate.apply[F: Representable]` case
  *coincidentally* has `X = F.Representation`, but Composer can't
  thread a `Representable[F]` instance and `S` is opaque), and (b) for
  the tuple-shaped Grate the natural target `F` would be `Vector` or
  similar, but again the bridge is domain-specific, not Composer-shaped.
  The structural mismatch is genuine; users wanting "fold/traverse over
  the slots a Grate exposes" should construct the `Forget[F]`-carrier
  optic directly.

Grate plan D3 documents the absence of:

- `Composer[Tuple2, Grate]` (Lens → Grate) — see Grate.scala lines
  270-279.

By symmetry, `Either → Grate`, `Affine → Grate`, `PowerSeries → Grate`,
and every `Grate → non-Grate` *except* Gr → SetterF are absent. The two
shipped non-symmetric pairs are Iso × Grate (`forgetful2grate`;
witnessed in `core/src/test/scala/eo/GrateSpec.scala` lines 91-122) and
Gr × SetterF (`grate2setter`; witnessed in `EoSpecificLawsSpec` and
`OpticsBehaviorSpec`).

#### 3.2.5 (deleted)

Folded into §3.2.6 — `MultiFocus[F]` outbound is now the unified
sink-rationale section.

#### 3.2.6 MultiFocus[F] × non-MultiFocus, non-MultiFocus × MultiFocus (except Iso→MF)

**2026-04-28 update — `MultiFocus[F]` carrier merge.** The pre-merge
`AlgLens[F]` × * and `Kaleidoscope` × * rows collapse into a single
`MultiFocus[F]` × * row. The MF × MF cell stays N (same-carrier
`mfAssoc`); MF × SetterF stays N (`multifocus2setter` collapses both
the prior `kaleidoscope2setter` and the latent `alg2setter`); every
other MF × inner cell is U with the same structural rationale that
governed Kaleidoscope × inner pre-merge.

**Inbound bridges** (every cell is N):

- `Composer[Forgetful, MultiFocus[F]]` (Iso → MF) — `forgetful2multifocus`
  with `F: Applicative + Foldable`.
- `Composer[Tuple2, MultiFocus[F]]` (Lens → MF) — `tuple2multifocus`
  with `F: Applicative + Foldable`; mixes in `MultiFocusSingleton` so
  `mfAssoc`'s singleton fast-path fires.
- `Composer[Either, MultiFocus[F]]` (Prism → MF) — `either2multifocus`
  with `F: Alternative + Foldable`.
- `Composer[Affine, MultiFocus[F]]` (Optional → MF) — `affine2multifocus`
  with `F: Alternative + Foldable`.
- `Composer[Forget[F], MultiFocus[F]]` (Fold → MF) — `forget2multifocus`,
  unconstrained.

**Outbound — only MF → SetterF.** `MultiFocus[F] → SetterF` ships as
`multifocus2setter`; the bridge collapses MultiFocus's broadcast
pattern via `(s: S) => (x, fa) = o.to(s); o.from((x, F.map(fa, f)))`.
Witnessed by `EoSpecificLawsSpec` (MorphLaws.A1 on the lifted
`MultiFocus.apply[List, Int]`) and `OpticsBehaviorSpec` (per-element
rewrite on `MultiFocus.apply[List]` and `MultiFocus.apply[ZipList]`).
Like every other `Composer[X, SetterF]`, this does NOT enable
`multiFocus.andThen(setter)` directly — SetterF lacks
`AssociativeFunctor` by design — but it does unlock the morph-site
value (`multiFocus.morph[SetterF]`).

**Two further candidates investigated and rejected as structurally
unsound** (rationale also lives at the bottom of `MultiFocus.scala`):

- **`Composer[MultiFocus[F], Forgetful]` (MultiFocus widens to
  Iso/Getter).** Type-level encodable. However,
  `forgetful2multifocus` already ships in the OPPOSITE direction.
  Adding the reverse would create a bidirectional Composer pair, which
  the `Morph` resolution explicitly forbids: both `Morph.leftToRight`
  and `Morph.rightToLeft` would match for any `Iso × MultiFocus` pair,
  surfacing as ambiguous-implicit and breaking every existing
  `iso.andThen(multifocus)` call site. Cats-eo's resolution invariant
  *"we don't ship bidirectional composers"* is the deciding
  constraint, NOT the type-level encodability. Workaround:
  `multiFocus.to(s)._2` for the read side.

- **`Composer[MultiFocus[F], Forget[G]]` (MultiFocus widens to
  Traversal/Fold).** Generic in `S, T, A, B`. The target carrier
  `Forget[G][X, A] = G[A]` forces the morphed `to` to produce `G[A]`
  from arbitrary `S`. The structural mismatch: even with `F = G`
  matching, the Composer has no place to thread the `Foldable[G]`
  witness — the carrier-shaped bridge can't carry an inner constraint
  scoped to the call site. Users wanting fold/traverse semantics on a
  MultiFocus's slots should construct the `Forget[F]`-carrier optic
  directly.

By symmetry of these two skip-rationales, `Tuple2 → MultiFocus → X`
chains where X ∉ {SetterF, MultiFocus[F]} all sit in the U bucket.
The shipped `MultiFocus` outbound surface is: same-carrier `andThen`
(via `mfAssoc`), `morph[SetterF]` (via `multifocus2setter`), and
nothing else.

The pre-merge `AlgLens.scala` and `Kaleidoscope.scala` files are
deleted; the unified file is `MultiFocus.scala`.

### 3.3 Unexplored cells (?)

#### 3.3.1 Forgetful / Tuple2 / Either / Affine × Fold — **RESOLVED (M / U) 2026-04-24**

The Fold optic lives on `Forget[F]`. There is
**no `Composer[Forgetful, Forget[F]]`**, no `Composer[Tuple2,
Forget[F]]`, etc. The `Forget → MultiFocus` bridge exists; the inverse
does not (Forget has no observable structural leftover, so there's no
sensible way to widen it back into Tuple2/Either/Affine).

**Outcome.** Two cases:

1. **Outer focuses on an `F[A]`** (e.g. `Lens[Row, List[Int]]`). The
   canonical lift is `MultiFocus.fromLensF(lens)` /
   `MultiFocus.fromPrismF(prism)` / `MultiFocus.fromOptionalF(opt)`,
   then chain via `Composer[MultiFocus[F], MultiFocus[F]]` (`mfAssoc`).
   Classified **M** — no `.andThen` cross-carrier path, but a single
   factory call routes the user. Documented in `site/docs/optics.md`
   MultiFocus section and exercised in `OpticsBehaviorSpec`.
2. **Outer focuses on a scalar `A`** (e.g. `Lens[Row, Int]`). There is
   **no natural composition** with a `Fold[F]` — the outer never
   produces an `F`-shaped value. Classified **U** for `.andThen`; the
   plain-Scala idiom is `lens.get(s).foldMap(f)`. Documented in
   `site/docs/optics.md` "Composition limits" subsection.

The `?` here was the lack of a written outcome, not a missing piece of
machinery. Pinned **M / U** by case.

#### 3.3.2 Optional × MultiFocus[F] — **RESOLVED (N) 2026-04-24**

Shipped `affine2multifocus` (`MultiFocus.scala`; pre-merge: `affine2alg`
in `AlgLens.scala`) — `Composer[Affine, MultiFocus[F]]` for any `F:
Alternative + Foldable`. Mirrors `either2multifocus`: `Miss → F.empty[A]`
(cardinality 0, preserves the Affine's `Fst` leftover for the pull
side); `Hit → F.pure(a)` (cardinality 1; pull collapses via
`pickSingletonOrThrow`). Behaviour spec at `OpticsBehaviorSpec`:

- `"Affine Optional lifts into MultiFocus[List] and preserves hit/miss
  .modify semantics"` — same shape as the Either-prism case.
- `"Optional andThen MultiFocus[List] classifier composes via
  affine2multifocus"` — full cross-carrier `.andThen` end-to-end.

This also closes the `Affine → MultiFocus` arm of the Tier-2
`chainViaTuple2` resolution: `Optional → MultiFocus` is now a Tier-1
direct, no transitive lookup needed.

#### 3.3.3 MultiFocus × anything (non-MultiFocus inner) — **RESOLVED (U, by design) 2026-04-24**

Pinned: **MultiFocus[F] is a designed composition sink** (sole
exception: `multifocus2setter`). Once you've landed in MultiFocus you
compose only with more MultiFocus-carrier optics (via `mfAssoc`) or
widen to `SetterF`. No other outbound Composer ships and none is
planned.

Rationale (mirrors the `optics.md` MultiFocus section): MultiFocus's
defining property is "structural leftover paired with classifier
candidates"; morphing back to Forget[F] would silently drop the `X`
on the floor (changing semantics from "lensy round-trip" to "phantom
read"), and morphing back to Tuple2/Either/Affine has no natural
candidate-to-singleton collapse. If the user really wants downstream
Fold/Traversal behaviour they should bridge the *inner* into MultiFocus
via `Forget[F] → MultiFocus[F]` (the `forget2multifocus` Composer),
not bridge MultiFocus *out*.

Documented in `site/docs/optics.md` MultiFocus section and reinforced
in the §3.4 U-cell summary.

#### 3.3.4 Traversal.each × MultiFocus / Fold (cross-`F`) — **RESOLVED (U, by design) 2026-04-24**

Two distinct carriers (`MultiFocus[PSVec]` and `MultiFocus[F]` for
`F ≠ PSVec`) with no Composer between them. Pinned **U** by design:
`MultiFocus[PSVec] → MultiFocus[F]` would require synthesising a
per-candidate cardinality count from PSVec's uniform-shape
representation, which doesn't fit `mfAssoc`'s push contract.

**Idiom.** A user wanting "fold each element of a traversal" reaches
for `traversal.foldMap(f)(s)` — the read-only escape that ships as
an extension method on every `MultiFocus[F]`-carrier optic where `F`
admits `Foldable` (closes top-5 gap #2 from
`docs/research/2026-04-29-top5-gap-closure-plan.md`). For "classifier
per traversal element", build the MultiFocus chain *under* the
traversal:

```scala
// instead of: traversal.andThen(multiFocus)
val result: S = traversal.modify(a => multiFocus.replace(...)(a))(s)
```

Documented as "manual idiom" in `site/docs/optics.md` Composition
section.

### 3.4 U-cell summary table

For quick scanning, the `U` rows in §2:

| Row family | Why unsupported | Recoverable? |
|---|---|---|
| AffineFold × anything | T=Unit as outer | No — that's the point |
| Getter × anything | T=Unit as outer | No |
| Fold × anything | T=Unit as outer | No |
| Setter × anything | no AssociativeFunctor[SetterF] | Yes — add assoc? plan needed |
| anything × Grate (except Iso) | Rep/Distributive incompat (plan D3) | No — structural |
| Grate × anything (except Grate, SetterF) | same reason | No — structural |
| MultiFocus[F] × anything (except inbound carriers, MultiFocus, SetterF) | bidirectional pair with Iso bridge would shadow Morph; Foldable witness can't ride a carrier-shaped bridge | No — structural for some, extension work for others |

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
| Lens × MultiFocus | OBS | `"Lens andThen MultiFocus[List] classifier composes end-to-end"` |
| Lens × JsonPrism | CCCS | scenarios 2 and 3 |
| Prism × Prism | OBS (via OLS law coverage) | `MendTearPrism.andThen(MendTearPrism)` fused path exercised in law suite |
| Prism × Lens | OBS | `"Prism.andThen(Lens) composes via Morph.bothViaAffine"` |
| Prism × Optional | OBS (implicit — Prism→Affine bridge) | **gap** — no direct `Prism.andThen(Optional)` test found |
| Prism × MultiFocus | OBS | `"Either Prism lifts into MultiFocus[List]"`, `"Prism.andThen(Prism) via MultiFocus[List] survives inner miss"` |
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
| MultiFocus × Lens | OBS | `"Lens → MultiFocus[List] → Lens composes three carriers cleanly"` |
| MultiFocus × Prism | **gap** | only Prism-inner-to-MultiFocus-outer as its own same-carrier test |
| MultiFocus × MultiFocus (same F) | OBS | `"Two Forget[List] classifiers compose via MultiFocus[List] with non-uniform cardinalities"` |
| MultiFocus × Fold | OBS (via `mfFold`) | `mfFold` cardinality+miss spec |
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
| Forget[F] × MultiFocus[F] same-F inject | OBS | `"Forget[Option] injects into MultiFocus[Option]"` |

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
11. `MultiFocus.andThen(Prism)` — Prism as inner should go through
    `Composer[Either, MultiFocus[F]]`; no test.
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
4. **MultiFocus × downstream outbound**. Currently `MultiFocus[F]` is
   a sink (sole exception: `multifocus2setter`). At minimum document
   that in `site/docs/optics.md` MultiFocus section (already present
   but understated).
5. ~~Traversal.forEach × same-F behaviour test~~ — N/A 2026-04-30:
   `Traversal.forEach` is removed. `Forget[F]` survives as the `Fold`
   carrier; same-F Fold composition via `assocForgetMonad` for `F:
   Monad` is still load-bearing (Fold-shaped chain) but is now best
   exercised via the `Forget[Option]` injection-into-MultiFocus
   composition spec already in `OpticsBehaviorSpec`.

### 5.2 Medium priority (nice to have, defer if capacity is tight)

6. Document the `Forgetful → Forget[F]` cell — there's no Composer,
   and the natural workaround is "read, then foldMap the result", which
   isn't called out anywhere.
7. Document the `Lens × Grate` absence with a pointer to plan D3
   in-line in `site/docs/optics.md` (it's present — keep expanding if
   readers miss it).

### 5.3 Deferred (don't block 0.1.0)

8. ~~FixedTraversal outbound composition~~ — RESOLVED 2026-04-29:
   `FixedTraversal[N]` folded into `MultiFocus[Function1[Int, *]]`, the
   absorbed-Grate sub-shape of `MultiFocus[F]`. Inherits `Iso → MF`
   inbound, `MF → SetterF` outbound, and same-carrier `.andThen` via
   `mfAssocFunction1`. Lens / Prism / Optional inbound bridges remain
   absent — `Function1[X0, *]` lacks Foldable / Alternative, same
   constraint as v1 Grate plan D3.
9. JsonTraversal × Optic lift-in — plan 005 Future Considerations;
   out of scope for 0.1.0.
10. `Composer[Affine, MultiFocus[F]]` — already shipped via
    `affine2multifocus`. Pre-merge there were two separate Composers
    (`Composer[Affine, AlgLens[F]]` shipped in Unit 21 + a parallel
    Kaleidoscope-side gap that was never explored). Both collapse into
    the single carrier-merged Composer.
11. Cross-`F` MultiFocus composition (`MultiFocus[List] .andThen
    MultiFocus[Option]`, which post-Tf-drop subsumes the prior cross-`F`
    Forget composition) — would require a fresh
    `Composer[MultiFocus[F], MultiFocus[G]]` with a transformation
    witness, or a specialized `flatten` for nested Foldables. Unlikely
    to pay for itself before 0.2.0.

---

## 6. Methodology caveats

- **14 × 14 = 196 is an approximation** (was 15 × 15 = 225 pre-merge).
  AffineFold and Getter collapse with their read-only parents for most
  inner cells (both have `T = Unit`, so the whole row is U). Counting
  those rows multiplies the U bucket; the N/M counts are unaffected.
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
  involving `Forgetful → Tuple2 → MultiFocus`). Flipping them is a
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
| 3.3.1 (F[A]-focus) | Forgetful/Tuple2/Either/Affine × Fold, outer focuses on `F[A]` | Documented `MultiFocus.fromLensF` / `fromPrismF` / `fromOptionalF` factory route | **M** |
| 3.3.1 (scalar-focus) | Forgetful/Tuple2/Either/Affine × Fold, outer focuses on scalar `A` | Documented `lens.get(s).foldMap(f)` plain-Scala idiom; no carrier path possible | **U** |
| 3.3.2 | Optional × MultiFocus | **Shipped** `Composer[Affine, MultiFocus[F]]` (`affine2multifocus`); two specs in `OpticsBehaviorSpec` | **N** |
| 3.3.3 | MultiFocus × non-MultiFocus (outbound) | Pinned designed sink (only outbound is `multifocus2setter`); documented in `optics.md` and §3.4 | **U** |
| 3.3.4 | Traversal.each × MultiFocus[F] (cross-F) | Pinned structural; documented `traversal.modify(inner.replace…)` idiom and `.foldMap` for read-only escape | **U** |
| ~~3.3.5~~ | ~~Traversal.forEach × Traversal.forEach cross-F~~ | N/A 2026-04-30 — `Traversal.forEach` removed pre-0.1.0; cross-F MultiFocus composition gap subsumes it (see Top-Surprising #2) | — |

The chain-refactor side effect (§3.3 was written before the
Tier-1/Tier-2 hierarchy landed): `chainViaTuple2` at low priority now
routes Forgetful → {SetterF, PowerSeries, MultiFocus[F]} transitively,
so `Iso.andThen(setter)` / `Iso.andThen(traversal)` / `Iso.andThen
(multifocus)` all resolve from a single intermediate without per-target
direct bridges. The matrix already shows these as N (rows 1, 2, 3 in
the **I** row in §2); the refactor only changed the resolution path,
not the cell colour.
