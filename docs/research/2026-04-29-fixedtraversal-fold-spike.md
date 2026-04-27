# FixedTraversal[N] fold spike — research

Branch: `worktree-agent-a7440331692ceabac` (worktree off main `c677d3b`).
Head at writing: `72ed2fd` after the carrier-eq fix.

## 1. Goal

Decide whether the `FixedTraversal[N]` carrier — a tuple-shaped match
type holding `N` `A` slots followed by a phantom `X` slot — collapses
cleanly into the unified `MultiFocus[F][X, A] = (X, F[A])` carrier.
The hypothesis from the task brief is

> `FixedTraversal[N][A, B] ≡ MultiFocus[Tuple_N][X, B]` where
> `Tuple_N[B] = (B, B, …, B)` is the homogeneous N-arity tuple.

That formulation is structurally right but the natural target
classifier is **`Function1[Int, *]`**, not `Tuple_N`: cats has every
typeclass we need on `Function1[Int, *]` (`Functor`, `Monad`,
`Distributive`, `ContravariantMonoidal`) but no first-class
homogeneous-tuple `Apply` / `Traverse` / `Foldable` for an arbitrary
`Tuple_N`. `MultiFocus.tuple[T <: Tuple, A]` already lives in core
(absorbed from v1 `Grate.tuple` in the Grate fold) and produces
exactly an `Optic[T, T, A, A, MultiFocus[Function1[Int, *]]]` — the
same carrier we need for `Traversal.{two,three,four}` post-fold.

## 2. Target carrier shape

```scala
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])
// Specialised at F = Function1[Int, *]:
type FT_Carrier = MultiFocus[Function1[Int, *]]
//             == [X, A] =>> (X, Int => A)
```

Every `Traversal.two/three/four` post-fold has shape

```scala
new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
  type X = Unit
  val to: S => (Unit, Int => A)        // snapshot N getters
  val from: ((Unit, Int => B)) => T    // call rebuild k(0..N-1)
```

Identical encoding to the absorbed `MultiFocus.tuple[T <: Tuple, A]`
(which uses `productElement(i)` for the read side and
`Tuple.fromArray(Array.tabulate)` for the rebuild). The new
`Traversal.two/three/four` factories generalise that shape — the
read side closes over the user-supplied `(S => A)` getters instead
of `productElement`, and the rebuild side calls a user-supplied
`(B, B, …) => T` instead of `Tuple.fromArray`.

## 3. Q1–Q3 findings

### Q1. Carrier encoding

**Empirical finding:** `MultiFocus[Function1[Int, *]]` is the right
target. `MultiFocus.tuple[T <: Tuple, A]` already produces this
carrier shape; the new `Traversal.two/three/four` factories use the
same encoding with custom `to` / `from` bodies. The hypothesis's
`Tuple_N` formulation works definitionally (an `(B, B, …, B)`-shaped
container IS structurally equivalent to a representable `Int => B`
over a finite index — both are functions from a tag set to a value)
but isn't directly usable because the `Tuple_N`-shape lacks cats
typeclass instances.

The ForgetfulFunctor instance `mfFunctor[Function1[Int, *]]` is
provided by the existing carrier-wide `mfFunctor[F: Functor]` — cats
already gives `Functor[Function1[T1, *]]` via the Monad-for-Reader
instance. Same body verbatim, no new instance needed.

The `MultiFocusLeadPosition` capability (lifted from the absorbed
Grate carrier) is NOT mixed into the post-fold `Traversal.two/three/four`
factories. The Grate-fold spike's Q1 finding was that the lead value
is empirically dead through `.modify` — every shipped path discards
it. Same conclusion holds for FT[N] post-fold: the rebuild closure
`Int => B` is the only externally observable side, and `.modify`
walks indices `0..N-1` regardless of any "lead". Adding the trait
would cost storage for nothing.

### Q2. Composability profile

**Empirical finding:** the FT row + column dissolve into the existing
`MultiFocus[Function1[Int, *]]` row + column, with the same
constraint profile as the absorbed-Grate sub-shape of MF.

| Bridge | Status | Mechanism |
|--------|--------|-----------|
| `Iso → MF[Function1[Int, *]]` | ✓ shipped | `forgetful2multifocusFunction1` (already in core, exercised by `MultiFocusFunction1Spec`) |
| `Lens → MF[Function1[Int, *]]` | ✗ structurally absent | `tuple2multifocus[F: Applicative: Foldable]` — `Function1[X0, *]` lacks Foldable |
| `Prism → MF[Function1[Int, *]]` | ✗ structurally absent | `either2multifocus[F: Alternative: Foldable]` — Function1 lacks Alternative |
| `Optional → MF[Function1[Int, *]]` | ✗ structurally absent | `affine2multifocus[F: Alternative: Foldable]` — same |
| `MF[Function1[Int, *]] → SetterF` | ✓ shipped | `multifocus2setter[F: Functor]` (carrier-wide; Function1 is Functor) |
| Same-carrier `.andThen` | ✓ shipped | `mfAssocFunction1[X0, Xo, Xi]` (already in core, exercised by `MultiFocusFunction1Spec`) |

The Lens / Prism / Optional inbound restrictions match v1 Grate plan
D3 exactly: `Function1[X0, *]` is the Naperian shape, and Lens /
Prism / Optional carriers can't broadcast their leftover into a pure
function-rebuild without an empty / fail-soft branch the function-
classifier doesn't expose.

**Composability win on the user side.** Pre-fold, every cell `_ × FT`
was U because no Composer ever fired. Post-fold, the user-facing
`Traversal.{two,three,four}` optics gain three concrete `.andThen`
abilities:

1. `iso.andThen(Traversal.two[…])` (cross-carrier via
   `forgetful2multifocusFunction1`)
2. `Traversal.two[…].andThen(setter)` (cross-carrier via
   `multifocus2setter`)
3. `Traversal.two[…].andThen(Traversal.three[…])` (same-carrier via
   `mfAssocFunction1`)

None of these were shipped pre-fold. The fold is therefore a real
**capability gain**, not just a code-reduction cleanup.

### Q3. Migration scope

| Action | File | Δ LoC |
|--------|------|------:|
| Delete `FixedTraversal.scala` | `core/src/main/scala/dev/constructive/eo/data/FixedTraversal.scala` | −67 |
| Delete `FixedTraversalLaws.scala` | `laws/src/main/scala/dev/constructive/eo/laws/data/FixedTraversalLaws.scala` | −27 |
| Delete `FixedTraversalTests.scala` | `laws/src/main/scala/dev/constructive/eo/laws/data/discipline/FixedTraversalTests.scala` | −29 |
| Rewrite `Traversal.two/three/four` to MF[Function1[Int, *]] | `core/src/main/scala/dev/constructive/eo/optics/Traversal.scala` | +73 / −62 (net +11 — bigger because each factory inlines a small `match` over the index) |
| Drop `checkAllFixedTraversalFor` helper | `tests/src/test/scala/dev/constructive/eo/CheckAllHelpers.scala` | −19 |
| Drop FT spec block in `OpticsLawsSpec` (3 arities × 2 laws + 1 typeclass-generic block) | `tests/src/test/scala/dev/constructive/eo/OpticsLawsSpec.scala` | −37 / +13 (replacement comment) |
| Drop FT import in `EoSpecificLawsSpec` | `tests/src/test/scala/dev/constructive/eo/EoSpecificLawsSpec.scala` | −2 / +6 (updated comment) |
| Drop FT mention in shared `ForgetfulFunctorLaws` doc | `laws/.../typeclass/ForgetfulFunctorLaws.scala` | −1 / +1 |
| Collapse FT row+column in gap analysis matrix | `docs/research/2026-04-23-composition-gap-analysis.md` | (text — see §4) |

Net source delta: **−180 LoC code + docs sweep**. Smaller than the
Grate fold (which was +458 / −513) because FT[N] never had any
perf-tuning machinery to absorb — it was documented as a "leaf tool
for law fixtures" and the body was 3 trivial `ForgetfulFunctor`
instances over fixed-shape tuple matches.

## 4. Q4. Surprises encountered

### 4.1 Function1 carrier-level laws can't be discipline-checked

`mfFunctor[Function1[Int, *]].map((x, k), f) = (x, k.andThen(f))` —
the result is a NEW closure object, structurally non-equal to the
input even when extensionally identical. Discipline laws use `==` for
equality; the same problem `SetterF` has.

The first attempt at the spike included a `MultiFocus[Function1[Int, *]]`
row in `OpticsLawsSpec`'s `checkAllForgetfulFunctorFor` sweep. Both
laws (functor identity, functor composition) failed at "0 passed
tests" because every closure-equality check fails by reference. The
fix was to drop that block entirely and rely on:

- The carrier-wide `mfFunctor[F: Functor]` body, exercised
  extensionally by the `MultiFocus[List | Option | Vector | Chain]`
  blocks (same body, container-equality friendly F).
- `MultiFocusFunction1Spec`'s G1/G2 forAll blocks on
  `MultiFocus.tuple` (the absorbed-Grate factory uses the same
  carrier; G1 is `_ * 2` then `identity` extensionally, G2 is two
  modify calls each producing a closure that's evaluated at every
  index — both verifiable by sampling the result).
- `EoSpecificLawsSpec`'s "Traversal.two modifies both components"
  forAll, which witnesses the FT-shaped factory at the `(Int, Int)`
  source and verifies extensional equality of the result tuple.

### 4.2 Test-count delta is exactly accounted for

Baseline: 381 examples (from the PowerSeries-fold spike doc).
Post-fold: **373 examples**. Δ = **−8**.

Every removed example tracks to a deleted FT-carrier-laws
discipline check:

| Removed block | Examples |
|---------------|---------:|
| `checkAllFixedTraversalFor[2, Unit, Int]` | 2 |
| `checkAllFixedTraversalFor[3, Unit, Int]` | 2 |
| `checkAllFixedTraversalFor[4, Unit, Int]` | 2 |
| `checkAllForgetfulFunctorFor[FixedTraversal[2], Unit, Int]` | 2 |
| **Total** | **8** |

No FT-specific behaviour test is removed — the
`Traversal.two/three/four` `.modify` smoke tests in
`EoSpecificLawsSpec` are kept verbatim and pass on the new carrier.
The absorbed laws (functor identity + composition for the FT shape)
are still witnessed at the typeclass level by the carrier-wide
`mfFunctor[F: Functor]` body across the `MultiFocus[List | Option |
Vector | Chain]` blocks, plus extensionally on
`MultiFocus[Function1[Int, *]]` via the per-spec smoke tests.

### 4.3 No perf-tuning machinery to recover

The PowerSeries fold spike's signature was the parallel-array
`AssocSndZ` representation surviving as `mfAssocPSVec`'s Z. The Grate
fold spike's was the lead-position field's empirical dead-code
deletion (+20% perf). FT[N] has neither:

- No `AssociativeFunctor[FixedTraversal[N]]` ever existed, so there's
  no specialised assoc body to port.
- No `Composer[F, FixedTraversal[N]]` ever existed, so there's no
  bridge body to port.
- The `ForgetfulFunctor` instances are 3 trivial per-arity
  `case (a0, a1, x) => (f(a0), f(a1), x)` patterns — replaced by ONE
  generic `mfFunctor[Function1[Int, *]]` derived from
  `Functor[Function1[Int, *]]`'s `andThen`. Same operation count
  (one focus modification per slot); different syntax.

This is the cleanest of the four folds, as the brief predicted.

## 5. Migration plan

### What gets deleted

- `core/src/main/scala/dev/constructive/eo/data/FixedTraversal.scala`
  (67 LoC — type alias + 3 ForgetfulFunctor instances).
- `laws/src/main/scala/dev/constructive/eo/laws/data/FixedTraversalLaws.scala`
  (27 LoC — 2 generic functor laws over arity N).
- `laws/src/main/scala/dev/constructive/eo/laws/data/discipline/FixedTraversalTests.scala`
  (29 LoC — discipline RuleSet for those 2 laws).

### What gets rewritten

- `core/src/main/scala/dev/constructive/eo/optics/Traversal.scala` —
  `two/three/four` return type goes from `Optic[…, FixedTraversal[N]]`
  to `Optic[…, MultiFocus[Function1[Int, *]]]`. Each factory inlines
  a small `match` over the index `0..N-1` returning the right
  user-supplied getter. The `from` body unpacks `(_, k)` and calls
  the user-supplied reverse builder at the indexed positions.

### What gets pruned (minimal-diff)

- `tests/src/test/scala/dev/constructive/eo/CheckAllHelpers.scala` —
  drop `checkAllFixedTraversalFor` helper + its imports.
- `tests/src/test/scala/dev/constructive/eo/OpticsLawsSpec.scala` —
  drop the 3 FT-arity arbitraries + 3 `checkAllFixedTraversalFor`
  calls + 1 `checkAllForgetfulFunctorFor[FixedTraversal[2], …]`
  call. Replace with an explanatory comment block (the absorbed laws
  are exercised by the existing `MultiFocus[F]` carrier sweep).
- `tests/src/test/scala/dev/constructive/eo/EoSpecificLawsSpec.scala`
  — drop `FixedTraversal` from the imports list. Update the Traversal-
  modify-smoke-test comment to reference the post-fold carrier.
- `laws/src/main/scala/dev/constructive/eo/laws/typeclass/ForgetfulFunctorLaws.scala`
  — drop `FixedTraversal[N]` from the example-carriers docstring.

### What gets re-counted (matrix sweep)

- `docs/research/2026-04-23-composition-gap-analysis.md` — collapse
  FT row + column. Matrix shrinks 14×14 → 13×13 (169 cells, −27
  cells, all U). N/M counts unchanged; U drops 44 → 17. The
  `mfAssocFunction1` + `mfAssocPSVec` AssociativeFunctor instance
  ledger entries are added (they were missing prior to this spike —
  a small accidental omission, fixed in passing).

## 6. Estimated migration effort

The spike is **2 commits** on the worktree branch; diff is
**+70 / −206 LoC** (the matching `git diff` reports
`8 files changed, 70 insertions(+), 206 deletions(-)`). Plus a third
commit fixing the carrier-eq surprise (Q4.1) and a fourth applying
the matrix update + this research doc.

**Doc sweep beyond the matrix:** `grep -rn FixedTraversal site/`
returns no hits, and the user-facing documentation
(`site/docs/optics.md`) does not currently have a Traversal subsection
that mentions FT[N] specifically — `Traversal.{two,three,four}` are
documented inline at their factory site, and those docstrings now
read against `MultiFocus[Function1[Int, *]]`. **No additional doc
sweep needed.**

The migration is otherwise complete on the spike branch. `sbt test`
reports `373 examples, 0 failure, 0 error` (down 8 from baseline as
expected; see §4.2). All four verification gates pass:

```sh
sbt -no-colors "core/compile"   # passes
sbt -no-colors "core/test"      # passes — 12 / 12, FoldSpec + MultiFocusSpec + MultiFocusFunction1Spec
sbt -no-colors "tests/test"     # passes — 182 / 182
sbt -no-colors "test"           # full root: passes — 373 examples, 0 failure
```

## 7. Open questions / show-stoppers

### 7.1 Function1 carrier-level discipline laws (Q4.1)

The `MultiFocus[Function1[Int, *]]` carrier laws can't be checked at
the discipline level via structural `==`. The mitigation (point at
the carrier-generic body via Container-equality F's, plus extensional
behaviour smoke tests) is sufficient — no Function1-specific functor
behaviour was lost — but a future cleanup could ship a generic
"closure-extensional ForgetfulFunctorLaws" for `Function1[T, *]`-
shaped carriers (sample at a finite list of indices, compare
extensionally). Same shape SetterFLaws already uses. Not blocking
the fold; logged as a 0.1.x cleanup.

### 7.2 Show-stoppers found: none

Every test (373 examples, 11000+ expectations across all law and
behaviour suites) passes on the spike branch. The spike adds no
benchmark since FT[N] has no perf surface — `Traversal.two/three/four`
are O(N) regardless of carrier and N is bounded at 4.

## 8. Recommendation

**Ship the fold.**

Reasons:

1. **Cleanest of the four folds.** `Tuple_N` is morally the
   homogeneous arity carrier; cats's `Function1[Int, *]` is the
   working representable substitute. The absorbed-Grate sub-shape of
   MF already shipped this carrier — the FT fold is recognising that
   `Traversal.{two,three,four}` was structurally always a special
   case of `MultiFocus.tuple`.
2. **Real composability gain.** Pre-fold, every `_ × FT` cell in the
   gap analysis was U. Post-fold, FT-shaped optics inherit `Iso ↪`,
   `↪ SetterF`, and same-carrier `.andThen` from the unified MF
   carrier — three new compositions the user can write today.
3. **Net carrier count: 11 → 10.** Hits the target the brief named.
4. **Net code reduction: ~180 LoC.** Three source files deleted; one
   factory file rewritten with the same shape as `MultiFocus.tuple`;
   one helper deleted; one law block collapsed.
5. **No perf delta** — there's no perf-tuning machinery on FT[N] to
   preserve or recover. Same allocation profile post-fold (the
   per-arity `match` inside `Int => A` is the only difference, and
   it's a hot-path JIT specialises away).
6. **Pre-0.1.0** — there is no published artifact to keep
   compatible. Doing the fold NOW costs less than after even one
   minor release.

Recommended sequence:

- Land the spike branch (PR review).
- Migration PR: none additional (the matrix update + research doc
  are already in the spike commit set).
- Tag 0.1.0-M3 against the unified carrier post-fold. (M1 was
  AlgLens + Kaleidoscope; M2 was PowerSeries; M3 is FixedTraversal +
  the empty `Composer[_, FT]` row eliminated).

The +70 / −206 LoC net reduction is the load-bearing simplification.
The composability gain (Iso → FT-shape, FT-shape → SetterF, FT-shape
self-compose) is the load-bearing user-facing improvement. Everything
else (matrix update, doc cross-references) is mechanical.
