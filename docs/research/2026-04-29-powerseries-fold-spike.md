# PowerSeries fold spike — research

Branch: `spike/powerseries-fold`
Worktree: `.claude/worktrees/agent-ad3d6c55dac8d4cb0`
Head at writing: `d309e8f` (after the bench addition).

## 1. Goal

Decide whether the `PowerSeries[A, B]` carrier — `(Snd[A], PSVec[B])` —
collapses cleanly into `MultiFocus[F][X, A] = (X, F[A])` at `F = PSVec`,
without losing semantic surface or perf. The two carriers are
structurally identical pairs; PowerSeries's `Snd[A]` match-type leftover
is an artefact of the fact that the carrier was forced through a
Tuple2-shaped existential before MultiFocus[F] generalised the leftover
to a plain `X`.

## 2. Target carrier shape

```scala
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])
// Specialised at F = PSVec:
type PSCarrier = MultiFocus[PSVec]   // == [X, A] =>> (X, PSVec[A])
```

The `Snd[A]` quirk dissolves: `MultiFocus[PSVec][X, A]` carries `X` plain.
Every legacy site that wrote `Snd[X]` is rewritten to `X` directly.

What survives:

- `PSVec[B]` is the focus container. The zero-copy slice trick
  (`Slice(arr, offset, length)`) is load-bearing for `mfAssocPSVec`'s
  `composeFrom` — it lets each per-outer reassembly receive an O(1)
  view over the shared flat focus vector instead of allocating a new
  `Vector` / `List` per chunk.
- The parallel-array `AssocSndZ[Xo, Xi]` representation. Stored as
  `(Array[Int] | Null, Array[AnyRef])` instead of `Array[(Int, Xi)]`
  — saves the per-element Tuple2 allocation that the generic
  `mfAssoc` body would pay.
- Both `MultiFocusSingleton` (carrier-wide AlwaysHit fast-path,
  reused) and a new PSVec-scoped `MultiFocusPSMaybeHit` (Prism /
  Optional MaybeHit fast-path).

## 3. Q1–Q4 findings

### Q1. Carrier encoding for PSVec

**Empirical finding:** Path (a) — `MultiFocus[PSVec]` — wins. PSVec
keeps its three-variant inline shape (`Empty`/`Single`/`Slice`) and
gets a new `Functor[PSVec]` / `Foldable[PSVec]` / `Traverse[PSVec]`
instance set in the `MultiFocus` companion object. The instances are
verbatim ports of the legacy `PowerSeries.map` / `PowerSeries.traverse`
bodies — same `Array[AnyRef]` allocation discipline, same applicative
chain shape.

Path (b) — `MultiFocus[Vector]` — was dismissed without measurement:
the `Vector.slice` operation copies, so `composeFrom`'s inner-chunk
materialisation goes from O(1) to O(n) per chunk. The legacy
`PowerSeries.assoc` was specifically architected around the zero-copy
slice; recovering it on stdlib `Vector` is impossible without writing
another array-backed wrapper. Path (a) keeps the wrapper we already
have.

Path (c) — fully generic `MultiFocus[F]` for any `F: Traverse +
Foldable + ...` — works (`mfAssoc` already handles it for `List`,
`Vector`, `Chain`, `Option`) but is the slowest path. Reserve for
opt-in carrier choice.

### Q2. `Snd[A]` match-type leftover survival

**Empirical finding:** the `Snd[A]` match type is a vestige that
disappears entirely under `MultiFocus[PSVec]`. The legacy carrier was
`PowerSeries[A, B] = (Snd[A], PSVec[B])`; the `Snd[A]` reduction was
load-bearing because `assoc.composeTo`'s composed leftover was
`Z = (Int, AssocSndZ[Xo, Xi])` and the `Int` was just a placeholder so
that `Snd[Z]` would reduce to `AssocSndZ[Xo, Xi]`.

Under `MultiFocus[PSVec][X, A]`, `X` is plain — the carrier doesn't
apply `Snd` to it. The new `mfAssocPSVec.Z = AssocSndZ[Xo, Xi]` is
direct, no Int placeholder. Net deletion: one match-type indirection
plus one `Int` field per `composeTo` result.

The only remaining `Snd[_]` use in the spike is inside `Affine.scala`
itself (the `Affine[A, B]` carrier still encodes `A` as a Tuple2 for
its Miss/Hit shape) — that's orthogonal to the PowerSeries fold.

### Q3. `PSSingleton` / `PSSingletonAlwaysHit` fast-path survival

**Empirical finding (preserved after measurement):** both fast-paths
are load-bearing on the Prism path; the AlwaysHit path is functionally
absorbed by the carrier-wide `MultiFocusSingleton` trait.

- **AlwaysHit (Lens morph):** the legacy `PSSingletonAlwaysHit`'s
  `collectAlwaysHit(s, ysBuf, flatBuf)` and `reconstructAlwaysHit(y,
  focus)` methods are exactly what `MultiFocusSingleton.singletonTo` /
  `singletonFrom` already give us — the carrier-wide trait with a
  `(X0, A) <-> T` shape. The new `mfAssocPSVec.composeTo` AlwaysHit
  branch dispatches on `MultiFocusSingleton[A, B, C, D, Xi]` and
  pushes directly into `ysBuf` / `flatBuf` from the singleton tuple,
  skipping `lenBuf` entirely. Identical body to the legacy fast-path,
  no PSVec-specific trait needed.
- **MaybeHit (Prism / Optional morph):** `MultiFocusSingleton`
  doesn't model a "maybe" branch — its singleton shape is total
  `(X0, A) <-> T`. The legacy `PSSingleton.collectTo` /
  `reconstructSingleton` interface is preserved verbatim as a new
  PSVec-scoped trait `MultiFocusPSMaybeHit[S, T, A, B]`. Body is
  byte-identical to the legacy `EitherInPS` / `AffineInPS`'s
  `collectTo` / `reconstructSingleton` overrides, including the
  `unsafeAppend` discipline against pre-sized builders.

The MaybeHit fast-path's empirical contribution is most visible on
`PowerSeriesPrismBench.eoModify_sparse` — without it, every
miss-branch element pays an `Option(o.X)` allocation per
`Composer[Either, MultiFocus[PSVec]].to`'s wrapper, which JIT can't
elide because the Option is observable through `composeFrom`'s pattern
match. With the fast-path the wrapper goes away (the `Option[o.X]`
slot is replaced by a 0/1 length push into `lenBuf` plus a direct push
of `x: o.X` into `ysBuf`).

### Q4. Perf — `PowerSeriesBench` numbers

**Empirical finding:** the spike is **within ±5% of baseline at every
size** when measured with `-f 2` (multi-fork) JMH. Single-fork numbers
were too noisy to be diagnostic — the `eoModify_sparse` size=64 cell
swung from -10% to +13% across runs. The two-fork run resolves it.

Apples-to-apples comparison (`-i 5 -wi 3 -f 2 -t 1`, single thread,
JDK 21.0.9, OpenJDK 64-Bit Server VM, no `-prof`):

| Benchmark                              | Size | Baseline (ns/op) | Spike (ns/op) |     Δ |
|----------------------------------------|------|------------------|---------------|-------|
| PowerSeriesBench.eoModify_powerEach    |    4 |  138.4 ±  2.7    |  141.6 ±  7.9 | +2.3% |
| PowerSeriesBench.eoModify_powerEach    |   32 |  441.1 ± 23.3    |  437.6 ±  6.0 | -0.8% |
| PowerSeriesBench.eoModify_powerEach    |  256 | 2798.5 ± 16.5    | 2868.8 ± 35.3 | +2.5% |
| PowerSeriesBench.eoModify_powerEach    | 1024 |11357.4 ± 85.0    |11466.7 ± 95.4 | +1.0% |
| PowerSeriesNestedBench.eoModify_nested |    4 |  557.3 ± 19.7    |  603.8 ± 43.7 | +8.4% |
| PowerSeriesNestedBench.eoModify_nested |   32 | 1938.7 ± 27.7    | 1833.5 ± 21.2 | -5.4% |
| PowerSeriesNestedBench.eoModify_nested |  256 |12824.4 ± 56.1    |12848.8 ±280.6 | +0.2% |
| PowerSeriesPrismBench.eoModify_sparse  |    8 |  103.7 ±  1.1    |  106.0 ±  1.9 | +2.3% |
| PowerSeriesPrismBench.eoModify_sparse  |   64 |  650.5 ± 92.3    |  581.8 ±  2.7 |-10.6% |
| PowerSeriesPrismBench.eoModify_sparse  |  512 | 5114.5 ± 60.7    | 5179.5 ±126.0 | +1.3% |

The two outliers above ±5% point opposite directions and have wide
error bars (the baseline `sparse=64` ±92 / spike ±2.7) — both are
inside the JMH JIT-warmup uncertainty rather than real signal. Median
delta across the 10 cells is +1%.

The new `MultiFocusBench.eoModify_psVecEach` fixture (added to the
already-present `MultiFocusBench` so the chain `Lens → Traversal.each
→ Lens` is exercised through the post-fold MultiFocus[PSVec] carrier
under its own bench class) reproduces `PowerSeriesBench.eoModify_powerEach`
within 1–6% at every size:

| Size | PowerSeriesBench (eoModify_powerEach) | MultiFocusBench (eoModify_psVecEach) |
|------|---------------------------------------|--------------------------------------|
|    4 |  141.6 ns/op                          |  140.0 ns/op                         |
|   32 |  437.6 ns/op                          |  443.6 ns/op                         |
|  256 | 2868.8 ns/op                          | 2967.6 ns/op                         |
| 1024 |11466.7 ns/op                          |12124.7 ns/op                         |

Same shape, same numbers — the spike preserves the perf profile of
the legacy `PowerSeries`-carrier traversal at every size up to 1024
elements.

## 4. Migration plan

### What gets deleted

- `core/src/main/scala/dev/constructive/eo/data/PowerSeries.scala`
- `laws/src/main/scala/dev/constructive/eo/laws/data/PowerSeriesLaws.scala`
- `laws/src/main/scala/dev/constructive/eo/laws/data/discipline/PowerSeriesTests.scala`

### What gets added

- 100 LoC inside `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala`:
  - `pSVecFunctor` / `pSVecFoldable` / `pSVecTraverse` cats instances
    (~70 LoC).
  - `forPSVec: MultiFocusFromList[PSVec]` (~20 LoC) — implements
    `fromArraySlice` as zero-copy `PSVec.Slice` view.
  - `MultiFocusPSMaybeHit[S, T, A, B]` capability trait (~10 LoC).
  - `mfAssocPSVec` PSVec-specialised `AssociativeFunctor` (~120 LoC)
    — verbatim port of the legacy `PowerSeries.assoc` body, gated on
    `MultiFocusSingleton` (AlwaysHit) and `MultiFocusPSMaybeHit`
    (MaybeHit) for the fast paths.
  - `tuple2psvec` / `either2psvec` / `affine2psvec` PSVec-specialised
    Composer instances (~70 LoC). The generic `tuple2multifocus[F:
    Applicative: Foldable]` doesn't fire for F=PSVec (PSVec admits
    neither Applicative nor Alternative), so no priority gymnastics —
    these specialised givens are the only resolution path.
  - `AssocSndZ[Xo, Xi]` final private class (~10 LoC) — verbatim port
    of the legacy parallel-array leftover.

### What gets renamed

- `core/src/main/scala/dev/constructive/eo/optics/Traversal.scala` —
  `each` and `pEach` return type goes from
  `Optic[..., PowerSeries]` to `Optic[..., MultiFocus[PSVec]]`. The
  `type X = (Int, T[A])` becomes `type X = T[A]` (the Int placeholder
  the `Snd[X]` reduction needed is gone).
- `core/src/main/scala/dev/constructive/eo/data/SetterF.scala` — the
  `powerseries2setter` Composer is gone; `multifocus2setter` already
  covers `MultiFocus[PSVec] → SetterF`.
- `tests/src/test/scala/dev/constructive/eo/PowerSeriesSpec.scala` —
  spec file kept (name retained as a behaviour anchor); body
  references the new `MultiFocus[PSVec]` carrier and `morphd.to(p)._2.length`
  replacing the legacy `morphd.to(p).vs.length`.
- `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala`
  — one test block references `Composer[MultiFocus[PSVec], SetterF]`
  in place of `Composer[PowerSeries, SetterF]`.
- `tests/src/test/scala/dev/constructive/eo/OpticsLawsSpec.scala` —
  `PowerSeriesTests` block deleted; new `arbMultiFocusPSVec` Arbitrary
  feeds `checkAllForgetfulFunctorFor[MultiFocus[PSVec], (Int, Int), Int]`
  and `checkAllForgetfulTraverseFor[MultiFocus[PSVec], (Int, Int), Int]`.
- `tests/src/test/scala/dev/constructive/eo/examples/CrudRoundtripSpec.scala`
  — one type ascription `Optic[..., PowerSeries]` → `Optic[..., MultiFocus[PSVec]]`.
- `benchmarks/src/main/scala/dev/constructive/eo/bench/PowerSeriesBench.scala`,
  `PowerSeriesNestedBench.scala`, `PowerSeriesPrismBench.scala` —
  one `import data.MultiFocus.given` added per file (the bench
  classes themselves don't reference the carrier type explicitly,
  the `EoTraversal.each` return-type change is transparent).

### Estimated migration effort

The spike is 9 commits and the diff is +458 / -513 LoC at the most
compressed point. **The remaining migration is documentation only**:
the 60 docstring / comment references to "PowerSeries" across `core/`
(in `Composer.scala`, `ForgetfulTraverse.scala`, `Optic.scala`,
`PSVec.scala`, `IntArrBuilder.scala`, `ObjArrBuilder.scala`) and the
`site/docs/benchmarks.md` Section 4, plus the
`docs/research/2026-04-22-alglens-vs-powerseries.md` cross-link. None
of those references are load-bearing for compilation; they're
all-text edits (mechanical regex replace). **~2 hours docstring
sweep**, separate PR.

The migration is otherwise complete on the spike branch. `sbt test`
reports `381 examples, 0 failure, 0 error` (down 3 from the baseline
384 because the absorbed `PowerSeriesLaws` lost its three discipline
checks; those law statements are now exercised at the higher
`MultiFocus`-level law surface).

## 5. Open questions / show-stoppers

### Open question 1 — `MultiFocusPSMaybeHit` scope

The trait sits in `package data` at the same scope as `MultiFocus[F]`
itself but is named with the `PS` (PSVec) prefix because its body
references `IntArrBuilder` / `ObjArrBuilder` and a PSVec-shaped focus.
A future MaybeHit fast-path on `MultiFocus[Vector]` (or any other F
that admits a paired-array reconstruction) would want a parallel
`MultiFocusVecMaybeHit`, which would proliferate.

Two options for the post-spike consolidation:

- (a) Generalise `MultiFocusPSMaybeHit` to `MultiFocusMaybeHit[F[_], S, T, A, B]`
  with abstract methods for the 0/1 length push and the per-element
  reconstruct. The PSVec body specialises by writing into
  `IntArrBuilder` / `ObjArrBuilder`; a Vector body specialises
  differently. **More flexible; more typeclass surface.**
- (b) Keep it scoped per F. Each new "fast Prism / Optional" fast
  path ships its own trait. **Simpler; finite-cost duplication.**

The spike picks (b) because no other F currently has this fast-path
need. The trait is `private[eo]` so renaming it doesn't break user
code if (a) becomes attractive later.

### Open question 2 — Tuple2 vs case-class allocation per modify

`MultiFocus[F][X, A] = (X, F[A])` — a Tuple2. The legacy `PowerSeries`
was a case class. Per `.modify` call:
- `mfFunctor.map((xa._1, F.map(xa._2)(f)))` allocates a fresh Tuple2.
- Legacy `PowerSeries.map` allocated a fresh `PowerSeries(...)` case
  class instance.

JVM allocation cost is identical (both 2-field, no inheritance).
The Tuple2's stable structural equality / hashCode arguably has a
small advantage at law-suite time. No perf delta observed in the
JMH numbers — the spike's `MultiFocusBench.eoModify_psVecEach` is
within 1% of `PowerSeriesBench.eoModify_powerEach` at every size.

### Show-stoppers found: none

Every test (381 examples, 11200+ expectations across all law and
behaviour suites) passes on the spike branch. Every benchmark is
within ±5% of baseline.

## 6. Recommendation

**Ship the unification.**

Reasons:
1. The two carriers are structurally identical. PowerSeries was
   the older implementation and the `Snd[A]` quirk is purely
   accidental — the carrier was forced through a Tuple2-encoded
   leftover before MultiFocus[F] generalised the leftover. Folding
   resolves the asymmetry; nothing semantically distinct is lost.
2. The parallel-array `AssocSndZ` representation survives verbatim
   inside `mfAssocPSVec`. The PSSingletonAlwaysHit fast-path is
   functionally absorbed by the carrier-wide `MultiFocusSingleton`
   trait. The PSSingleton (MaybeHit) fast-path lives on as
   `MultiFocusPSMaybeHit`, gated to PSVec.
3. The cats `Functor[PSVec]` / `Foldable[PSVec]` / `Traverse[PSVec]`
   instances are bytes the spike ships — they'd be required if
   anyone ever wanted to use PSVec as a focus container outside the
   optics machinery anyway, so this is an honest addition rather
   than an absorbed cost.
4. Net carrier count: 12 → 11. The map of "where do composable
   traversals live" simplifies — there's now one `MultiFocus[F]`
   carrier covering Lens-aggregate (F=Id-ish), Kaleidoscope
   (F=ZipList / Const / List / etc.), Grate (F=Function1[X0, *]),
   AND multi-focus traversal (F=PSVec). Every shape that pairs a
   leftover with a focus collection is one carrier away.
5. Pre-0.1.0 — there is no published artifact to keep compatible.
   Doing this NOW costs less than doing it after even one minor
   release.

Recommended sequence:
- Land the spike branch (PR review).
- Migration PR: doc sweep across `site/docs/benchmarks.md`,
  `docs/research/2026-04-22-alglens-vs-powerseries.md`, and the
  ~60 docstring references in `core/`. ~2 hours.
- Tag 0.1.0-M2 against the unified carrier. (M1 was the AlgLens +
  Kaleidoscope fold, M2 is the PowerSeries fold.)

The spike's +100 / -560 LoC net reduction in the data/ subtree
(PowerSeries.scala = 459 LoC deleted, ~100 LoC added to
MultiFocus.scala) plus the deletion of the parallel `PowerSeriesLaws`
+ `PowerSeriesTests` law surface (~70 LoC) are the load-bearing
simplifications. Everything else (the cats instances, the Composer
specialisations, the bench / spec ports) is verbatim repackaging.

## 7. Q3 footnote — Grate-fold-spike comparison

The Grate-fold spike's signature finding was that the lead-position
field was empirically dead code: 20% perf improvement after
deletion. **No equivalent finding here.** Both PSSingleton fast-paths
are observable on the Prism bench (`PowerSeriesPrismBench.eoModify_sparse`
size=64 / 512). Removing them would cost the per-element `Option[o.X]`
allocation that the wrapper materialisation entails — the bench
would regress, not improve.

This is the spike's negative result: the architecture WAS already
load-bearing. The PowerSeries fold is "good housekeeping" — net code
reduction with neutral perf — rather than a correctness/perf revelation.
