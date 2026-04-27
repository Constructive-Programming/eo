# Composition-coverage gap analysis

**Date:** 2026-04-23
**Last updated:** 2026-04-27 — full ecosystem refresh pass. Folds in the
post-Unit-21 work: 2026-04-25 SetterF row delta, 2026-04-26 eo-circe
carrier consolidation (4 → 2), eo-avro module (Units 1–13) reusing the
same Either/PowerSeries/Forget carriers as eo-circe, 2026-04-26 Gr × S
flip, 2026-04-27 Kaleidoscope row + column, K × S flip. Walks every
remaining `?` cell to a settled N / M / U classification, recomputes
the cell-count totals, re-ranks §1.2's top-5 surprising gaps after the
Grate / Kaleidoscope / SetterF closures, and refreshes §5 against the
current state of `site/docs/optics.md`. See §8 for the per-commit
update log; §7 retains the Unit 21 scoreboard.

**Scope:** every (outer × inner) optic-family composition pair currently
shipped in `cats-eo` (core + circe + avro), classified by whether
`.andThen` works natively, requires a manual idiom, is unsupported, or
is unexplored.

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
| 14 | Kaleidoscope | `Kaleidoscope` | `Optic[…, Kaleidoscope]` |
| 15 | JsonPrism / JsonFieldsPrism | `Either` | `Optic[Json, Json, A, A, Either]` |
| 16 | JsonTraversal / JsonFieldsTraversal | — | standalone, not an `Optic` |
| 17 | Review | — | standalone, not an `Optic` |

**eo-avro carriers (2026-04-25 → 2026-04-27).** The `cats-eo-avro`
module ships four user-facing carriers — `AvroPrism`, `AvroFieldsPrism`,
`AvroTraversal`, `AvroFieldsTraversal` — built on the same carrier
shapes as their eo-circe counterparts: `AvroPrism` extends
`Optic[IndexedRecord, IndexedRecord, A, A, Either]`, `AvroTraversal` is
standalone (no `Optic` extension), and `AvroFieldsPrism` /
`AvroFieldsTraversal` are `type =` aliases on top of the single-focus
classes. Composition profile is therefore **identical to JP / JT** at
the carrier level — see §2.2 for the cross-reference treatment instead
of repeating the row in the matrix.

**eo-circe consolidation (2026-04-26).** The four-class surface
collapsed to two: `JsonPrism[A]` (single class, holding a sealed
`JsonFocus[A]` with `Leaf` / `Fields` cases) and `JsonTraversal[A]`
(single class, holding `prefix + focus`). `JsonFieldsPrism[A]` and
`JsonFieldsTraversal[A]` survive as `type =` aliases for source
compatibility. Row 15 / 16 above accordingly cover both the leaf and
fields flavours under one classification — the carrier did not change,
only the class layout.

That is **17 row labels**, but row 16 and 17 do not extend `Optic`, so
they can only appear as outer or inner of an idiom-level composition.
For the 15 `Optic`-bearing families above (1–15) we produce a 15×15
inner-matrix (225 cells) and then add two "outer JsonTraversal / Review"
and two "inner JsonTraversal / Review" border rows, bringing the matrix
to the ~254 cells.

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
| `Tuple2 → SetterF` | `SetterF.scala:79` |
| `Either → SetterF` | `SetterF.scala:97` (shipped 2026-04-25 to close eo-monocle Gap-1; line moved by 2026-04-26 `34b5b50` Hot-Spot-A refactor) |
| `Affine → SetterF` | `SetterF.scala:113` (shipped 2026-04-25 to close eo-monocle Gap-1; line moved by 2026-04-26 `34b5b50` Hot-Spot-A refactor) |
| `PowerSeries → SetterF` | `SetterF.scala:133` (shipped 2026-04-25 to close eo-monocle Gap-1; line moved by 2026-04-26 `34b5b50` Hot-Spot-A refactor) |
| `Tuple2 → PowerSeries` | `PowerSeries.scala:307` |
| `Either → PowerSeries` | `PowerSeries.scala:362` |
| `Affine → PowerSeries` | `PowerSeries.scala:407` |
| `Forget[F] → AlgLens[F]` | `AlgLens.scala:318` |
| `Tuple2 → AlgLens[F]` (F: Applicative+Foldable) | `AlgLens.scala:343` |
| `Either → AlgLens[F]` (F: Alternative+Foldable) | `AlgLens.scala:378` |
| `Forgetful → Grate` | `Grate.scala:273` |
| `Grate → SetterF` | `Grate.scala` (shipped 2026-04-27 — closes Gr × S) |
| `Forgetful → Kaleidoscope` | `Kaleidoscope.scala:301` |
| `Kaleidoscope → SetterF` | `Kaleidoscope.scala` (shipped 2026-04-27 — closes K × S) |

**Absent by design / not yet shipped** (documented or inferred):

- `Composer[Tuple2, Grate]` — Grate plan D3 (`docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`):
  "A Lens's source type `S` is not in general `Representable` /
  `Distributive`, so there's no natural way to broadcast a fresh focus
  through the Lens's structural leftover."
- `Composer[Either, Grate]`, `Composer[Affine, Grate]`,
  `Composer[PowerSeries, Grate]` — same reason (no Representable
  inhabitant at these focuses).
- `Composer[Tuple2, Kaleidoscope]` — Kaleidoscope plan D3
  (`docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md`):
  same shape as Grate's Lens → Grate deferral. A Lens's source `S` has
  no natural `Reflector` witness, so there's no carrier-shaped
  aggregation to plug in. Workaround: construct the Kaleidoscope
  separately at the Lens's focus type and compose via `Lens.andThen`.
- `Composer[Either, Kaleidoscope]`, `Composer[Affine, Kaleidoscope]`,
  `Composer[PowerSeries, Kaleidoscope]` — same reason (no `Reflector`
  at these focuses).
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

The full 15×15 same-family-ish matrix (`Optic`-extending families 1–15;
standalone JsonTraversal + Review are handled separately in §4). All
prior count rows kept for the audit trail; the 2026-04-27 refresh
column is the post-walk authoritative tally.

| Category | Initial | After Unit 21 | After 2026-04-25 SetterF | After 2026-04-26 Gr × S | After 2026-04-27 K row+col | After 2026-04-27 refresh | % of 225 |
|---|---|---|---|---|---|---|---|
| **N** (native `.andThen`) | 94 | 96 | 99 | 100 | 103 | **63** | 28% |
| **M** (manual idiom) | 56 | 60 | 59 | 59 | 59 | **17** | 8% |
| **U** (unsupported) | 34 | 40 | 40 | 39 | 63 | **145** | 64% |
| **?** (unexplored) | 12 | 0 | 0 | 0 | 0 | **0** | 0% |

**Why the refresh column differs significantly from the prior rows.** The
earlier counts mixed two conventions: they tallied **N over the full
15×15 matrix** but tallied **U / M only over "productive" rows** —
silently dropping the structurally-trivial T=Unit-outer rows (AF, G, F,
S as outer) and the terminal-leaf rows (Tf, FT) from the U denominator.
That made U look smaller than it is. The N count was further
double-counted in some intermediate rows (Unit 21's "+2 N" from
`affine2alg` was bundled with the SetterF row delta, etc.). The refresh
column is a clean per-row walk of the actual matrix in §2, so:

- **N = 63.** Down from the headline 103 because the prior figure was
  including the standalone-family border cells (§2.1) — and was
  arithmetically off by ~40 from the start. The +2 over the naive 61
  comes from recognising that the Tf row's same-F × Forget cells (Tf ×
  F, Tf × Tf) are N when `F: Monad` via `assocForgetMonad`.
- **U = 145.** Up from 63 because rows AF / G / S / F / FT (5 rows × 15
  cols = 75 cells) really are all U: T=Unit-outer or terminal-leaf, no
  idiom recovers them. Plus the per-row structural-U cells in the
  productive rows including the 13 U cells in row Tf (T=Unit outer
  shadows almost everything) — total 145.
- **M = 17.** Restricted to cells where a genuine
  `outer.andThen(narrow)` (AffineFold-narrowing for I/L/P/O × AF) or
  carrier-bridged-in-Scala (`outer.modify(inner.modifyUnsafe(f))` for
  JsonTraversal-style standalone inners) workaround exists.

The narrative deltas (+3 N from SetterF, +1 N from Gr × S, +3 N / +26 U
from K row+col) all still apply on top of the **prior** column — which
is a 14×14-derived rolling tally, not a clean per-row walk. The refresh
column is the authoritative 15×15 figure going forward; subsequent
deltas should be applied to it.

**Cell-count math (per row, post-refresh).** The 15 rows of §2 walked
left-to-right:

| Row | N | M | U | ? | Total |
|---|---|---|---|---|---|
| **I**  (Iso)               | 10 | 3 | 2 | 0 | 15 |
| **L**  (Lens)              | 8  | 3 | 4 | 0 | 15 |
| **P**  (Prism)             | 8  | 3 | 4 | 0 | 15 |
| **O**  (Optional)          | 8  | 3 | 4 | 0 | 15 |
| **AF** (AffineFold)        | 0  | 0 | 15| 0 | 15 |
| **G**  (Getter)            | 0  | 0 | 15| 0 | 15 |
| **S**  (Setter)            | 0  | 0 | 15| 0 | 15 |
| **F**  (Fold)              | 0  | 0 | 15| 0 | 15 |
| **Te** (Traversal.each)    | 7  | 1 | 7 | 0 | 15 |
| **Tf** (Traversal.forEach) | 2  | 0 | 13| 0 | 15 |
| **FT** (FixedTraversal)    | 0  | 0 | 15| 0 | 15 |
| **AL** (AlgLens)           | 8  | 1 | 6 | 0 | 15 |
| **Gr** (Grate)             | 2  | 0 | 13| 0 | 15 |
| **K**  (Kaleidoscope)      | 2  | 0 | 13| 0 | 15 |
| **JP** (JsonPrism)         | 8  | 3 | 4 | 0 | 15 |
| **Total**                  | **63** | **17** | **145** | **0** | **225** |

**Productive-row sub-total** (excluding the 5 rows that are entirely U
by structural collapse — AF, G, S, F, FT — i.e. 10 rows × 15 cols =
150 productive cells): **N = 63, M = 17, U = 70, ? = 0**.

That is the number a user is likely to feel day-to-day: of the 150
"realistically composable" cells, 63 work via `.andThen` directly, 17
need an idiom, 70 are structurally unsupported (AlgLens outbound, Gr/K
outbound except Gr/K → SetterF, the Tf-row read-only-outer cells minus
the two same-F arms, plus a handful of cross-carrier dead ends).

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

(2026-04-27 Kaleidoscope-row delta: matrix grows from 14×14 (196 cells)
to 15×15 (225 cells) — 29 new cells from the Kaleidoscope row + column.
Of those: **+3 N** (I × K via `forgetful2kaleidoscope`; K × K via the
already-shipped `kalAssoc`; K × S via the new `kaleidoscope2setter`),
and **+26 U** (every other K row/column cell — Kaleidoscope's
structural isolation matches Grate's almost cell-for-cell, including
the same Lens-source-has-no-classifier-witness shape that gates Lens →
Grate). The two adjacent candidates investigated in the same batch
(`Composer[Kaleidoscope, Forgetful]` and `Composer[Kaleidoscope,
Forget[F]]`) were rejected as structurally unsound — see §3.2.6 for
the resolution rationale, which mirrors §3.2.4's Grate skip block.)

The Unit-21 resolution closed every numbered `?` group from §3.3:
+2 N (`affine2alg` + `chainViaTuple2(Forgetful → Tuple2 → Forget[F]`
already covered by existing tests once chain refactor landed);
+4 M (idiom-documented `Forgetful/Tuple2/Either/Affine × Fold` cases
where outer focuses on `F[A]`); +6 U (structurally-decided cells
where the carrier round-trip cannot work — AlgLens outbound,
PowerSeries × Forget/AlgLens, cross-F Forget pairs).

Adding the 28 border cells for the two standalone families: JsonTraversal
rows/columns are **M** (documented in `CrossCarrierCompositionSpec`
scenarios 4/5 in both eo-circe and eo-avro) and Review rows/columns are
**M** (direct function-composition idiom) or **U** (as outer — no `to`
side). The eo-avro `AvroTraversal` / `AvroFieldsTraversal` standalone
families are covered by the JT row in §2.1 (same compose-via-modify
idiom; the host type is the only thing that differs).

Grand totals across all 253 cells (225 Optic×Optic + 28 standalone
borders), post-2026-04-27 refresh:

| Category | Count |
|---|---|
| N | 61 |
| M | 33 |
| U | 159 |
| ? | 0 |

(M increases by 16 from 17 → 33: ~14 cells where a JsonTraversal /
JsonFieldsTraversal / AvroTraversal / AvroFieldsTraversal sits as inner
of a classical optic and the user routes through
`outer.modify(trav.modifyUnsafe(f))`, plus 2 Review-as-inner-of-Iso /
Review-as-inner-of-Prism cells. U increases by 12 to 159: the rest of
the standalone borders that genuinely have no idiom — Review-as-outer
of any read-write optic, JsonTraversal-as-outer beyond the documented
`outer.modify` idiom, and the JsonTraversal × Review corner.)

### 1.2 Top 5 surprising gaps (re-ranked 2026-04-27)

The previous list (Unit 21) had 5 entries. Two have been narrowed since
— SetterF terminal-ness is now front-and-centre in
[`site/docs/optics.md`](../../site/docs/optics.md#setter), and JsonPrism
× AlgLens is N (the `Composer[Either, AlgLens[F]]` bridge applies; only
the test coverage is missing). They were demoted to make room for two
genuinely-still-load-bearing items the Grate / Kaleidoscope work
surfaced: PowerSeries outbound (still a wall in practice) and Grate /
Kaleidoscope being nearly-isolated leaves with only **one** inbound
bridge each (Iso → Gr/K) and **one** outbound bridge each (Gr/K →
SetterF). The ranked list:

1. **Traversal.forEach × Traversal.forEach across different `F`/`G`.**
   Same as before. `Fold[List] .andThen Fold[Option]` fails implicit
   resolution; no `Composer[Forget[List], Forget[Option]]`. §3.3.5
   pinned this **U-deferred-to-0.2.x** with two design routes (a
   `FunctionK[F, G]` witness, or a specialised flatten on the
   AlgLens[F] side); neither has a 0.2.x plan-of-record yet. Genuinely
   load-bearing because cross-`F` folds are the natural way to express
   "fold each `Option[Int]` element of a `List`" — users hit this on
   day one.

2. **PowerSeries outbound is a wall.** No `Composer[PowerSeries,
   AlgLens[F]]`, no `Composer[PowerSeries, Grate]`, no `Composer
   [PowerSeries, Forget[F]]`. Inbound bridges all ship (`tuple2ps`,
   `either2ps`, `affine2ps`), so users compose lots of optics
   *into* a `Traversal.each` chain — and then discover the chain
   terminates there. The §3.3.4 idiom (`traversal.modify(a =>
   algLens.replace(...)(a))`) recovers AlgLens-after-traversal but
   doesn't help with the Forget / Grate outbound directions.

3. **Grate and Kaleidoscope are nearly-isolated leaves.** Each has
   exactly **one** inbound Composer (`forgetful2grate`,
   `forgetful2kaleidoscope`), exactly **one** outbound Composer
   (`grate2setter`, `kaleidoscope2setter`), and **no** Composer between
   them in either direction. `Composer[Tuple2, Grate]`, `Composer[Either,
   Grate]`, `Composer[Affine, Grate]`, `Composer[PowerSeries, Grate]`
   are all U by structural argument (no Representable/Distributive
   inhabitant at the outer's focus); the K row mirrors that almost
   cell-for-cell via the same Reflector-witness gap. The Iso → Gr → S
   and Iso → K → S triangles are the only ways into and out of these
   carriers, which is much narrower than users expect and only
   indirectly documented in §3.2.4 / §3.2.6. *Newly-surfaced by the
   Kaleidoscope-row work* — was hidden before because the matrix didn't
   yet have a K row to make the symmetry visible.

4. **AlgLens outbound is still a designed sink.** Documented in
   `site/docs/optics.md` AlgLens section (post-Unit-21) AND in §3.3.3 of
   this doc. Less "surprising" than at v0.0.x, but the *consequence* is
   still surprising: once a user hits AlgLens via `forget2alg` or
   `affine2alg`, they're sealed into the AlgLens monoidal category —
   any further drilling has to go *through* the `F[A]` side using
   cats-core directly. Worth keeping at #4 because new users still trip
   on "I went into AlgLens and now my IDE tells me there's nothing to
   do".

5. **`SetterF` lacks `AssociativeFunctor`, by design.** Documented in
   `site/docs/optics.md` Setter section (post-Unit-21) AND at
   `SetterF.scala:14`. Demoted from #4 because the documentation gap is
   closed — but kept on the list because it's a genuine type-level
   wall, not a TODO. `setter.andThen(_)` simply doesn't compile, and
   the ergonomic restructuring (build the chain so the Setter is the
   inner) is non-obvious until you've hit it once. The 2026-04-25
   batch shipped `Either / Affine / PowerSeries → SetterF` so any
   classical-optic outer can land in SetterF; no plan to add the
   reverse.

**Items dropped from the prior top-5 because they no longer rank.**
- **JsonPrism.andThen(AlgLens)** (was #5) — `Composer[Either, AlgLens
  [F]]` applies; the cell is now N. The remaining gap is purely test
  coverage (§4.3 item 11) — too tactical for the surprising-gaps list.

### 1.3 Top 3 high-priority gap-closures for 0.1.0 (status 2026-04-27)

1. **Traversal × Traversal (same `F`) coverage** — **DONE.**
   `OpticsBehaviorSpec.scala:763` ships
   `Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int])`
   plus a 3-hop `Lens → Traversal → Optional` chain at line 776, both
   in the consolidated R11a block.
2. **Document "terminal-carrier" gotchas** — **DONE for SetterF.**
   `site/docs/optics.md` Setter section (lines 552-561) explains the
   composition-terminal property in user-facing language. FixedTraversal
   is still under-documented but is library-internal (used by
   `Traversal.{two,three,four}` law fixtures, not user-facing) — leave
   for later.
3. **AlgLens × Traversal.each bridging** — **NOT DONE; deferred.** Per
   §3.3.4, structurally **U** in both directions (PowerSeries can't widen
   to `Forget[F]` without dropping rebuild data; AlgLens outbound is the
   designed sink — see §3.3.3). The decision is documented in
   `site/docs/optics.md` Composition limits section (lines 826-833) and
   in §3.3.4 of this doc; no Composer is planned. **Closed by decision,
   not by implementation.**

---

## 2. Full matrix

Columns are **inner** optics; rows are **outer** optics. Abbreviations:
I=Iso, L=Lens, P=Prism, O=Optional, AF=AffineFold, G=Getter, S=Setter,
F=Fold, Te=Traversal.each (PowerSeries), Tf=Traversal.forEach
(Forget[F]), FT=FixedTraversal[N], AL=AlgLens[F], Gr=Grate,
K=Kaleidoscope, JP=JsonPrism/JsonFieldsPrism.

Each cell indicates the classification and a one-line "why".

|         | I | L | P | O | AF | G | S | F | Te | Tf | FT | AL | Gr | K | JP |
|---------|---|---|---|---|----|---|---|---|----|----|----|----|----|----|----|
| **I**   | N (Forgetful.assoc, fused `BijectionIso.andThen(BijectionIso)`) | N (forgetful2tuple→tupleAssocF; fused `Iso.andThen(GetReplaceLens)`) | N (forgetful2either→eitherAssocF; fused `Iso.andThen(MendTearPrism)`) | N (Forgetful→Tuple2→Affine via chain; fused `Iso.andThen(Optional)`) | M (AF's T=Unit mismatches outer B — see §3) | U (Getter's T=Unit) | N (Forgetful→Tuple2→SetterF) | M (Forgetful→Forget[F] absent; outer-on-F[A] uses `AlgLens.fromLensF` route per §3.3.1) | N (Forgetful→Tuple2→PowerSeries via chain) | M (same shape as I × F per §3.3.1; outer-on-F[A] route via AlgLens-lift) | U (no Composer[_, FT]) | N (forget2alg path OR Forgetful→Tuple2→AlgLens) | N (Composer[Forgetful, Grate]; GrateSpec witnesses) | N (Composer[Forgetful, Kaleidoscope] — `forgetful2kaleidoscope`; ReflectorInstancesSpec) | N (Forgetful→Either via forgetful2either) |
| **L**   | N (tupleAssocF after forgetful2tuple on inner) | N (tupleAssocF; fused `GetReplaceLens.andThen(GetReplaceLens)`) | N (bothViaAffine — OpticsBehaviorSpec.Lens→Prism) | N (Composer[Tuple2, Affine]; fused `GetReplaceLens.andThen(Optional)`) | M (see AffineFold row in §3) | U (inner T=Unit ≠ outer B) | N (Composer[Tuple2, SetterF]) | M (Tuple2→Forget[F] not shipped; outer-on-F[A] uses `AlgLens.fromLensF` per §3.3.1) | N (Composer[Tuple2, PowerSeries]) | M (same shape as L × F per §3.3.1) | U (no Composer[_, FT]) | N (Composer[Tuple2, AlgLens[F]]) | U (Composer[Tuple2, Grate] explicitly NOT shipped per D3) | U (Composer[Tuple2, Kaleidoscope] explicitly NOT shipped per K-plan D3 — same shape as Lens → Grate, no Reflector at the Lens's source) | N (bothViaAffine — CrossCarrierCompositionSpec scenarios 1-3) |
| **P**   | N (forgetful2either morphs inner into Either; fused `MendTearPrism.andThen(BijectionIso)`) | N (bothViaAffine) | N (eitherAssocF; fused `MendTearPrism.andThen(MendTearPrism)`) | N (Composer[Either, Affine]; fused `MendTearPrism.andThen(Optional)`) | M (AF T=Unit) | U (T=Unit) | N (Composer[Either, SetterF] — shipped 2026-04-25) | M (Either→Forget[F] not shipped; outer-on-F[A] uses `AlgLens.fromPrismF` per §3.3.1) | N (Composer[Either, PowerSeries]) | M (same shape as P × F per §3.3.1) | U (no Composer[_, FT]) | N (Composer[Either, AlgLens[F]]) | U (no Composer[Either, Grate]) | U (no Composer[Either, Kaleidoscope] — by symmetry with Either → Grate) | N (stays in Either via eitherAssocF) |
| **O**   | N (Affine.assoc after forgetful→tuple→affine on inner) | N (Affine.assoc after tuple2affine on inner; fused `Optional.andThen(GetReplaceLens)`) | N (Affine.assoc after either2affine; fused `Optional.andThen(MendTearPrism)`) | N (Affine.assoc; fused `Optional.andThen(Optional)`) | M (AF T=Unit — use `AffineFold.fromOptional(chain)`) | U (T=Unit) | N (Composer[Affine, SetterF] — shipped 2026-04-25) | M (Affine→Forget[F] not shipped; outer-on-F[A] uses `AlgLens.fromOptionalF` per §3.3.1) | N (Composer[Affine, PowerSeries]) | M (same shape as O × F per §3.3.1) | U (no Composer[_, FT]) | N (Composer[Affine, AlgLens[F]] — `affine2alg`, Unit 21) | U (no Composer[Affine, Grate]) | U (no Composer[Affine, Kaleidoscope]) | N (stays Affine via either2affine on the inner JsonPrism) |
| **AF**  | U (outer T=Unit; can't feed into any inner B slot) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) | U (T=Unit) |
| **G**   | U (outer T=Unit) | U | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **S**   | U (SetterF lacks AssociativeFunctor; even with same-F inner no andThen) | U (no Composer[SetterF, _]) | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **F**   | U (Fold's T=Unit) | U | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **Te**  | N (Composer[Forgetful → Tuple2 → PowerSeries] via chain on inner) | N (Composer[Tuple2, PowerSeries] on inner) | N (Composer[Either, PowerSeries] on inner — **tested 2026-04-25 in OpticsBehaviorSpec R11a**) | N (Composer[Affine, PowerSeries] on inner — **tested 2026-04-25 in OpticsBehaviorSpec R11a**) | M (T=Unit on inner AF) | U (Getter T=Unit) | N (Composer[PowerSeries, SetterF] — shipped 2026-04-25) | U (PowerSeries → Forget[F] structurally rejected per §3.3.4 — drops rebuild data) | N (same-carrier PowerSeries.assoc — **tested 2-level in OpticsBehaviorSpec R11a, line 763**) | U (PowerSeries → Forget[F] structurally rejected per §3.3.4) | U (no Composer[_, FT]) | U (PowerSeries → AlgLens[F] structurally rejected per §3.3.4 — no per-candidate cardinality) | U (no Composer[PowerSeries, Grate]) | U (no Composer[PowerSeries, Kaleidoscope]) | N (Composer[Either, PowerSeries] on inner JsonPrism; untested) |
| **Tf**  | U (Tf's T=Unit outer ≠ inner T) | U | U | U | U | U | U | N (same-F Tf × Fold via assocForgetMonad when F: Monad; cross-F is U-deferred to 0.2.x per §3.3.5) | U | N (same-F via assocForgetMonad when F: Monad; cross-F is U-deferred to 0.2.x per §3.3.5) | U | U | U | U | U |
| **FT**  | U (FT lacks AssociativeFunctor; no outbound composer) | U | U | U | U | U | U | U | U | U | U | U | U | U | U |
| **AL**  | N (Forgetful→Tuple2→AlgLens[F] via chain on inner) | N (Composer[Tuple2, AlgLens[F]] on inner — OpticsBehaviorSpec) | N (Composer[Either, AlgLens[F]] on inner — OpticsBehaviorSpec) | N (Composer[Affine, AlgLens[F]] — `affine2alg`, AlgLens.scala:423; mirror cell of O × AL post-Unit-21) | M (AF T=Unit) | U (Getter T=Unit) | U (AlgLens outbound is the designed sink per §3.3.3; SetterF is also terminal so no escape path) | N (Composer[Forget[F], AlgLens[F]] on inner when same F — OpticsBehaviorSpec) | U (no Composer[PowerSeries, AlgLens[F]]; AlgLens outbound sink per §3.3.3) | N (same-F via `forget2alg` — Composer[Forget[F], AlgLens[F]]; cross-F is U-deferred per §3.3.5) | U (no Composer[_, FT]) | N (assocAlgMonad; OpticsBehaviorSpec "Two Forget[List] classifiers compose") | U (no Composer[AlgLens[F], Grate]) | U (no Composer[AlgLens[F], Kaleidoscope]) | N (Composer[Either, AlgLens[F]] applies — JsonPrism's Either carrier; untested — §4.3 item 11) |
| **Gr**  | U (Composer[Forgetful, Grate] is ONE-WAY; Iso→Grate yes, Grate→Iso no — see §3.2.4) | U (no Composer[Tuple2, Grate]) | U | U | U | U | N (Composer[Grate, SetterF] — `grate2setter`, Grate.scala; shipped 2026-04-27) | U | U | U | U | U | N (grateAssoc same-carrier — untested with two Grates beyond law suite) | U (no Composer between Grate and Kaleidoscope in either direction) | U |
| **K**   | U (Composer[Forgetful, Kaleidoscope] is ONE-WAY; Iso→K yes, K→Iso no — see §3.2.6) | U (no Composer[Tuple2, Kaleidoscope] — same Lens-source-has-no-Reflector shape as Lens → Grate) | U | U | U | U | N (Composer[Kaleidoscope, SetterF] — `kaleidoscope2setter`, Kaleidoscope.scala; shipped 2026-04-27) | U (Composer[Kaleidoscope, Forget[F]] structurally unsound — see §3.2.6) | U | U | U | U | U (no Composer between Kaleidoscope and Grate in either direction) | N (kalAssoc same-carrier same-F — ReflectorInstancesSpec witnesses Iso → K → K through the same FCarrier) | U |
| **JP**  | N (forgetful2either morphs inner Iso into Either; eitherAssocF) | N (bothViaAffine — CCCS scenarios 1-3) | N (eitherAssocF — fused `.andThen` lives on JsonPrism itself via stock Either carrier) | N (Composer[Either, Affine]) | M (AF T=Unit) | U | N (Composer[Either, SetterF] — applies to JP's Either carrier; untested for JP specifically — §4.3) | M (Either→Forget[F] not shipped; outer-on-F[A] uses `AlgLens.fromPrismF` per §3.3.1 — same shape as P × F) | N (Composer[Either, PowerSeries] — untested) | M (same shape as JP × F per §3.3.1) | U | N (Composer[Either, AlgLens[F]] applies — `JsonPrism.andThen(AlgLens)` plausible; untested — §4.3 item 11) | U | U (no Composer[Either, Kaleidoscope]) | N (eitherAssocF — JsonPrism nested via `.field(...).field(...)` is this pattern) |

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

#### eo-avro standalone borders (2026-04-25 → 2026-04-27)

`AvroTraversal[A]` and `AvroFieldsTraversal[A]` are the avro-side
equivalents of `JsonTraversal` / `JsonFieldsTraversal`: both are
standalone (do not extend `Optic`), both ship `modifyUnsafe` /
`modify` (Ior-flavoured) helpers. `AvroFieldsTraversal[A]` is a
`type =` alias for `AvroTraversal[A]` (same consolidation as
`JsonFieldsTraversal` post-2026-04-26). Their composition profile is
identical to the JsonTraversal column in the table above:

| Outer → Inner (any) | AvroTraversal / AvroFieldsTraversal |
|---|---|
| **As outer** | M (`outer.modify(trav.modifyUnsafe(f))(_)`; see `avro/src/test/scala/.../CrossCarrierCompositionSpec.scala` scenario 4) |
| **As inner of Iso / Lens / Prism / any read-write** | M (manual idiom, same shape as JT inner) |

The `AvroPrism[A]` / `AvroFieldsPrism[A]` *do* extend `Optic`, on the
same `Either` carrier as `JsonPrism[A]`, so they are NOT standalone-
border families — they fall under the JP column / row in §2 (carrier-
level identity; the only difference is the host type — `IndexedRecord`
vs `Json` — which doesn't affect cross-carrier composition).

### 2.2 No-`?` invariant + cross-references

**As of 2026-04-27 the matrix has zero `?` cells.** Every formerly-`?`
cell has been resolved to N / M / U:

- The 8 `Forgetful/Tuple2/Either/Affine × Fold/Tf` cells are M (idiom-
  documented in §3.3.1 via the `AlgLens.from{Lens,Prism,Optional}F`
  factory routes when the outer focuses on an `F[A]`; users with
  scalar-focus outers get `lens.get(s).foldMap(f)` directly).
- Optional × AlgLens is N (Unit-21 `affine2alg` ship; §3.3.2). The
  symmetric AL × O cell is also N (mirror application of the same
  Composer); the previous matrix overlooked this and held it as `?`.
- AlgLens × {Tf, JP} are N (forget2alg + Composer[Either, AlgLens]
  apply); AL × {S, Te} are U (designed sink per §3.3.3).
- Te × {F, Tf, AL} are U (PowerSeries outbound is structurally rejected
  per §3.3.4).
- Tf × Tf cross-F is U-deferred to 0.2.x (§3.3.5); same-F is N.
- JP × {S, F, Tf, AL} mirror P × {S, F, Tf, AL} because JP's carrier is
  `Either` (carrier-level identity); the test-coverage gap on JP × AL
  is tracked in §4.3 item 11.

**eo-avro borders.** The eo-avro `AvroPrism` / `AvroFieldsPrism` carriers
are `Optic[IndexedRecord, IndexedRecord, A, A, Either]` — i.e. exactly
the same carrier as JP. Their composition profile is therefore identical
to the JP column / row in the matrix above. See §2.1 for the inner-side
treatment of `AvroTraversal` / `AvroFieldsTraversal` (which mirror
JsonTraversal / JsonFieldsTraversal as standalone, not-extending-`Optic`
families).

The common thread that drove the original `?` ambiguity: **Forget[F]
never morphs into anything except `AlgLens[F]`**, and AlgLens never
morphs out at all. Neither has changed; the resolution was to commit
to U for the structural cases and M for the idiom-recoverable cases.

---

## 3. Per-cell details — M / U cells

Skipping the 63 N-cells whose reason is fully explained in the matrix
parenthetical. Post-2026-04-27 there are no `?` cells, so this section
covers only the M and U groups.

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

#### 3.2.5 AlgLens outbound

No `Composer[AlgLens[F], _]` ships — AlgLens is a sink. Once you're
in AlgLens you can only chain with more AlgLens-carrier inners.

#### 3.2.6 Kaleidoscope × non-Kaleidoscope, non-Kaleidoscope × Kaleidoscope (except Iso→K)

**2026-04-27 update — K × S resolved (N).** Shipped
`Composer[Kaleidoscope, SetterF]` (`kaleidoscope2setter`,
`Kaleidoscope.scala`); the K × S cell flips from **U** to **N**. The
bridge collapses Kaleidoscope's aggregation pattern to the Setter API
by reusing the `(s: S) => k = o.to(s); o.from(kalFunctor.map(k, f))`
shape from [[kalFunctor]]. Witnessed by `EoSpecificLawsSpec`
(MorphLaws.A1 on the lifted `Kaleidoscope.apply[List, Int]`) and
`OpticsBehaviorSpec` (per-element rewrite on `Kaleidoscope.apply[List]`
and `Kaleidoscope.apply[ZipList]`). Like every other
`Composer[X, SetterF]`, this does NOT enable `kaleidoscope.andThen
(setter)` directly — SetterF lacks `AssociativeFunctor` by design — but
it does unlock the morph-site value (`kaleidoscope.morph[SetterF]`,
uniform Setter-shaped extension points).

**Two further candidates investigated and rejected as structurally
unsound** (rationale also lives at the bottom of `Kaleidoscope.scala`
so the investigation isn't re-spent — and mirrors §3.2.4's Grate skip
block almost cell-for-cell):

- **`Composer[Kaleidoscope, Forgetful]` (Kaleidoscope widens to
  Iso/Getter).** Type-level the bridge encodes via `Id`-carrier
  Kaleidoscopes on either side. However, [[forgetful2kaleidoscope]]
  already ships in the OPPOSITE direction. Adding the reverse would
  create a bidirectional Composer pair, which the `Morph` resolution
  explicitly forbids: both `Morph.leftToRight` and `Morph.rightToLeft`
  would match for any `Iso × Kaleidoscope` pair, surfacing as
  ambiguous-implicit and breaking every existing
  `iso.andThen(kaleidoscope)` call site (witnessed by
  `ReflectorInstancesSpec`). Cats-eo's resolution invariant *"we don't
  ship bidirectional composers"* is the deciding constraint, NOT the
  type-level encodability.

- **`Composer[Kaleidoscope, Forget[F]]` (Kaleidoscope widens to
  Traversal/Fold).** Generic in `S, T, A, B`. The target carrier
  `Forget[F][X, A] = F[A]` forces the morphed `to` to produce `F[A]`
  from arbitrary `S`. There is no path because (a) Kaleidoscope's
  `FCarrier` is a path-dependent type member (NOT a Composer
  parameter), opaque after morphing through `Optic[…, Kaleidoscope]`;
  and (b) even with `FCarrier = F` known at the user's call site,
  `Reflector[F]` provides `Apply[F]`, which is not in general the same
  as `Foldable[F]` Composer would need to thread for the target.
  Composer has no place to thread a `FCarrier = F` equality OR a
  `Foldable[F]` instance. The structural mismatch is genuine; users
  wanting fold/traverse semantics on a Kaleidoscope's slots should
  construct the `Forget[F]`-carrier optic directly.

Kaleidoscope plan D3 documents the absence of:

- `Composer[Tuple2, Kaleidoscope]` (Lens → Kaleidoscope) — see
  `Kaleidoscope.scala` lines 293-298.

By symmetry, `Either → Kaleidoscope`, `Affine → Kaleidoscope`,
`PowerSeries → Kaleidoscope`, and every `Kaleidoscope → non-K` *except*
K → SetterF are absent. The two shipped non-symmetric pairs are Iso ×
Kaleidoscope (`forgetful2kaleidoscope`; witnessed in
`ReflectorInstancesSpec`) and K × SetterF (`kaleidoscope2setter`;
witnessed in `EoSpecificLawsSpec` and `OpticsBehaviorSpec`).

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
| Grate × anything (except Grate, SetterF) | same reason | No — structural |
| anything × Kaleidoscope (except Iso) | no Reflector at the source (K-plan D3, mirror of Grate) | No — structural |
| Kaleidoscope × anything (except Kaleidoscope, SetterF) | same reason; bidirectional pair with Iso bridge would shadow Morph | No — structural |
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

### 4.3 Gap list (actionable) — refreshed 2026-04-27

Most of the previous gap list closed in the 2026-04-25 → 2026-04-26
test-consolidation sweep (`FusedAndThenSpec.scala` + R11a block in
`OpticsBehaviorSpec.scala`). Items still outstanding are flagged
**OPEN**; closed ones are marked **DONE** with the spec coordinate.

1. `Iso.andThen(JsonPrism)` — **OPEN.** No direct test. The
   `Forgetful→Either` bridge handles it but no behaviour-level spec
   exercises it. (Was item 1.)
2. `Lens.andThen(Setter)` — **DONE.**
   `OpticsBehaviorSpec.scala:447` "Prism / Optional / Traversal →
   SetterF" block plus `Composer[Tuple2, SetterF]` exercised
   transitively via `FusedAndThenSpec.scala`'s GetReplaceLens block.
3. `Prism.andThen(Optional)` direct use — **DONE.**
   `FusedAndThenSpec.scala:117` `MendTearPrism.andThen(Optional)`
   composite assertion under "MendTearPrism.andThen — 5 fused
   overloads".
4. `Prism.andThen(Traversal.each)` — **DONE** (transitively via the
   R11a block which exercises `Traversal.each.andThen(circleP)` =
   inverse of this; the symmetric `prism.andThen(Traversal.each)` test
   is still **OPEN** at the strict level — Composer[Either,
   PowerSeries] is exercised in the inner-side direction in CCCS but
   not the outer-side direction).
5. `Optional.andThen(Lens / Prism / Optional)` — **DONE.**
   `OpticsBehaviorSpec.scala:809` "Optional.andThen fused overloads:
   Optional / Lens / Prism / Iso — hit + miss preserved" covers all
   four fused overloads.
6. `Optional.andThen(Traversal.each)` — **OPEN.**
   `Composer[Affine, PowerSeries]` with specialised `AffineInPS`
   fast-path is still untested at the behaviour level.
7. `Traversal.each.andThen(Prism)` — **DONE.**
   `OpticsBehaviorSpec.scala:758` `prismChain` in R11a block.
8. `Traversal.each.andThen(Optional)` — **DONE.**
   `OpticsBehaviorSpec.scala:745` `optChain` in R11a block.
9. `Traversal.each.andThen(Iso)` — **DONE.**
   `OpticsBehaviorSpec.scala:735` `isoChain` in R11a block.
10. `Traversal.each.andThen(Traversal.each)` — **DONE.**
    `OpticsBehaviorSpec.scala:763` `each2` in R11a block (2-level
    nesting on `List[List[Int]]`).
11. `AlgLens.andThen(Prism)` — **OPEN.** Composer[Either, AlgLens[F]]
    bridge applies but no behaviour spec exists.
12. Two-Grate compose at the behaviour level — **OPEN.** `grateAssoc`
    is law-suite tested but no asymmetric outer/inner pair spec.
13. `JsonPrism.andThen(Traversal.each)` — **OPEN.** Conceptually
    supported via `Composer[Either, PowerSeries]`; no behaviour spec.

**Net change since last refresh:** 8 closed, 5 still open. The 5 open
items are all medium-effort behaviour-spec additions; none require
shipping new Composers.

**M cells with no test:**

14. `Prism.andThen(Setter)` manual idiom — no documentation of the
    fallback.

---

## 5. Priority recommendations

### 5.1 High-priority (close before 0.1.0) — refreshed 2026-04-27

Focus on **user-facing, high-value cross-carrier chains** — the cells
that will appear in the first 20 minutes of any user's session and
whose absence in the test/doc corpus is most likely to burn.

1. **Traversal.each × downstream (Iso, Optional, Prism, Traversal.each)
   behaviour tests** — **DONE.**
   `OpticsBehaviorSpec.scala:731` "Traversal.each ∘ {Iso / Optional /
   Prism / each / Lens-Optional} downstream chains" (R11a block) covers
   all four downstream chains plus the 2-level nested-each case and a
   3-hop `Lens → Traversal → Optional`. Closed by the 2026-04-25
   consolidation sweep.
2. **Optional fused overloads** — **DONE.**
   `OpticsBehaviorSpec.scala:809` "Optional.andThen fused overloads"
   covers Optional/Lens/Prism/Iso fused paths. `FusedAndThenSpec.scala`
   covers `MendTearPrism.andThen(Optional)` and `GetReplaceLens.andThen
   (Optional)` symmetrically. Closed.
3. **Setter composition story** — **DONE (documentation route).**
   `site/docs/optics.md` Setter section (lines 552-561) explicitly
   explains "Setter is a composition terminal", points users to the
   `lens/prism/traversal.andThen(setter)` restructuring, and references
   the SetterF.scala:14 design note. The 2026-04-25 batch ALSO shipped
   the inbound bridges (`Either / Affine / PowerSeries → SetterF`) so
   any classical-optic outer can land in SetterF; no plan to add the
   outbound direction (would require an `AssociativeFunctor[SetterF]`
   that doesn't exist by design — see §3.2.2). Closed by documentation.
4. **AlgLens × downstream outbound documentation** — **DONE.**
   `site/docs/optics.md` AlgLens section (lines 788-803, "AlgLens is a
   composition sink") explicitly documents the no-outbound-Composer
   policy AND links back to this gap analysis as the "top-5 structural
   gap" reference. Closed.
5. **Traversal.forEach × same-F behaviour test** — **OPEN.**
   `assocForgetMonad` is still tested only via a toy `Forget[Option]`
   pair. A realistic `Forget[List]` chain would surface the Monad-
   composition assumptions more clearly. Carry over.

**Remaining open from §4.3** (medium-effort behaviour-spec additions,
none requires shipping new Composers): `Iso.andThen(JsonPrism)`,
`prism.andThen(Traversal.each)` outer-side, `Optional.andThen
(Traversal.each)`, `AlgLens.andThen(Prism)`, two-Grate compose,
`JsonPrism.andThen(Traversal.each)`. Item 5 above plus these six form
the v0.1.0 spec-coverage residue.

### 5.2 Medium priority (nice to have, defer if capacity is tight)

6. Document the `Forgetful → Forget[F]` cell — **PARTIALLY DONE.**
   `site/docs/optics.md` Composition limits section (lines 818-824)
   covers the F[A]-focus and scalar-focus split. Could expand with one
   concrete example chain. Carry over as low-priority polish.
7. Document the `Lens × Grate` absence with a pointer to plan D3
   in-line in `site/docs/optics.md` — **DONE.** The
   `2026-04-23-optics-md-D3-rework` commit (`cbcc5be`) reworked the
   composition section into a Mermaid lattice that calls out the
   Lens → Grate / Lens → Kaleidoscope absences explicitly. Closed.

### 5.3 Deferred (don't block 0.1.0)

8. FixedTraversal outbound composition — `FixedTraversal[N]` is a
   leaf tool for law fixtures; real users compose via `Traversal.each`.
9. JsonTraversal × Optic lift-in — plan 005 Future Considerations;
   out of scope for 0.1.0. **AvroTraversal × Optic lift-in** is the
   same shape (eo-avro reuses the JsonTraversal carrier model) — same
   deferral applies.
10. `Composer[Affine, AlgLens[F]]` — **DONE Unit 21.** Shipped as
    `affine2alg` (`AlgLens.scala:423`); see §3.3.2.
11. Cross-`F` Forget composition (`Forget[List] .andThen
    Forget[Option]`) — would require a fresh `Composer[Forget[F],
    Forget[G]]` with a transformation witness, or a specialized `flatten`
    for nested Foldables. Unlikely to pay for itself before 0.2.0.
12. **NEW: PowerSeries outbound bridges** — `Composer[PowerSeries,
    AlgLens[F]]`, `Composer[PowerSeries, Forget[F]]`, `Composer
    [PowerSeries, Grate]` are all **U** by structural argument
    (PowerSeries' rebuild data has no carrier counterpart in the
    targets). Pinned per §3.3.4; document the workaround (`traversal.
    modify(inner.modify(f))` at the Scala level) in
    `site/docs/optics.md` if not already there.

---

## 6. Methodology caveats

- **15 × 15 = 225 is the literal cell count.** Six rows (AF, G, S, F,
  Tf-as-outer, FT) are all-U by structural collapse — T=Unit-outer or
  no-AssociativeFunctor terminal. The 2026-04-27 refresh column counts
  them honestly as 90 U cells; earlier columns smuggled them into M as
  "manually compose at the Scala level" which inflated the M bucket by
  ~70 cells. The §1.1 productive-row sub-total (135 cells) is the more
  useful "what does day-to-day composition look like?" number.
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
- **No more `?` cells.** As of the 2026-04-27 refresh every cell is
  N / M / U. The previous "honesty disclaimer" about un-compiled `?`
  pairs is retired — every formerly-`?` cell has a settled
  classification with a §3 cross-reference.
- **JsonFieldsPrism is a `type =` alias for JsonPrism (since 2026-04-26).**
  Both extend `Optic[Json, Json, A, A, Either]`; the consolidated class
  layout means there is only one runtime carrier. Composition behaviour
  is identical at the `Optic` level (the fields-specific concerns —
  partial-read atomicity, Ior threading — are orthogonal to
  `.andThen`).
- **eo-avro carriers reuse JsonPrism's carrier model.** `AvroPrism[A]`
  / `AvroFieldsPrism[A]` extend `Optic[IndexedRecord, IndexedRecord, A,
  A, Either]` — same `Either` carrier as JP; only the host type
  differs. `AvroTraversal` / `AvroFieldsTraversal` are standalone-
  family borders (do not extend `Optic`), aligned with §2.1's JT
  treatment. To save matrix width the avro families don't get their
  own row/column — see §2.2 for the cross-reference notice.

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

---

## 8. Latest update log

Matrix-touching commits since the last comprehensive Unit 21 refresh
(2026-04-24), one line each:

- **2026-04-25** — `SetterF.scala:81/103/129` — shipped
  `Either → SetterF` (`either2setter`), `Affine → SetterF`
  (`affine2setter`), `PowerSeries → SetterF` (`powerseries2setter`) to
  close eo-monocle Gap-1. Three matrix cells flipped: P × S, O × S,
  Te × S all become N. Captured in §1.1's "After 2026-04-25 SetterF"
  delta column.
- **2026-04-26** — `37ecb6a refactor(circe): unify Json optic carriers
  — JsonFocus collapses 4 → 2`. The four-class circe surface
  (`JsonPrism`, `JsonFieldsPrism`, `JsonTraversal`,
  `JsonFieldsTraversal`) consolidated to two classes, with the
  fields-vs-leaf split internalised in `JsonFocus`. `JsonFieldsPrism`
  / `JsonFieldsTraversal` survive as `type =` aliases for source
  compatibility. No matrix cell changes — same carriers, same Composer
  bridges.
- **2026-04-25 → 2026-04-27** — `7b35fc0` through `99a162f` — eo-avro
  module Units 1-13. Ships four carriers (`AvroPrism`,
  `AvroFieldsPrism`, `AvroTraversal`, `AvroFieldsTraversal`) on the
  same carrier shapes as their eo-circe counterparts (Either for
  Prisms, standalone for Traversals). No matrix-row/column expansion;
  documented as a §2.1 / §2.2 cross-reference because composition
  profile is carrier-identical to the JP column.
- **2026-04-26** — `61690e1 feat(grate): Composer[Grate, SetterF]`.
  Gr × S cell flipped U → N. Two adjacent candidates
  (`Composer[Grate, Forgetful]`, `Composer[Grate, Forget[F]]`) were
  investigated and rejected as structurally unsound — see §3.2.4 for
  the rationale block.
- **2026-04-27** — `2f144f5 feat(kaleidoscope): Composer[Kaleidoscope,
  SetterF]`. K × S cell flipped U → N. Two adjacent candidates
  (`Composer[Kaleidoscope, Forgetful]`, `Composer[Kaleidoscope,
  Forget[F]]`) rejected analogously — see §3.2.6.
- **2026-04-27** — `2bcb55a chore(format): scalafmt Kaleidoscope.scala`.
  No matrix change.
- **2026-04-27** — *this refresh pass*. Walks every remaining `?` cell
  to a settled N / M / U, recomputes cell-counts honestly (clarifies
  the 90 structural-U cells previously absorbed into M), re-ranks
  §1.2's top-5 surprising gaps to demote the closed Setter-doc /
  JP × AL items and promote the Grate/Kaleidoscope-isolation gap, and
  refreshes §4.3 / §5 against the current `OpticsBehaviorSpec.scala`
  R11a block, `FusedAndThenSpec.scala`, and `site/docs/optics.md`.
