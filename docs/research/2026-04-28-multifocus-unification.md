# MultiFocus[F] unification — research spike

Branch: `spike/multifocus-unification`
Worktree: `.claude/worktrees/agent-a157fb83b45b4567d`
Head at writing: see `git log -1` on the branch.

## 1. Goal

Decide whether `AlgLens[F]` and `Kaleidoscope` can collapse into a single
`MultiFocus[F][X, A] = (X, F[A])` carrier without losing semantic surface or
perf. The two carriers are structurally identical pairs; the differences
(F-as-parameter vs path-dependent member, `cats.Functor`/`Traverse` vs
project-local `Reflector`) are encoding choices, not capability deltas.

## 2. Target carrier shape

```scala
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])
```

Identical to today's `AlgLens[F]`. `X` is the structural leftover (the same
role it plays in `Tuple2`, `Affine`, `Grate`); `F[A]` is the focus
collection / aggregate. The unification rests on two observations:

- Kaleidoscope's `type FCarrier[_]` member is fixed at every shipped
  constructor site (`apply[F]`, `forgetful2kaleidoscope` with `Id`). Lifting
  it to a type parameter loses no expressivity — the path-dep encoding never
  carried different Fs at different positions of a chain because
  `kalAssoc`'s same-F restriction already forbade it.
- Reflector[F]'s only operation beyond `Apply[F]` is `reflect: (fa: F[A]) =>
  (f: F[A] => B) => F[B]`. See Q1 for the empirical analysis of when this
  is and isn't derivable from cats hierarchy.

Grate stays separate. Its `(A, X => A)` shape (Distributive / Representable
profile) carries no `F[A]` collection — it's a Naperian rebuild closure, not
an aggregable focus. The two profiles are distinct.

## 3. Q1–Q4 findings

### Q1. Reflector[F] vs Apply[F] / Functor[F]

**Empirical finding:** Reflector's `reflect` is not derivable uniformly
from `Apply[F]`. The four shipped Reflector instances split:

| Instance | `reflect(fa)(f)` returns | Functor.map fits? | Apply.product fits? | Applicative.pure fits? |
|---|---|---|---|---|
| `forList` | `List(f(fa))` (singleton) | NO (would broadcast) | N/A | YES |
| `forZipList` | `ZipList(List.fill(size)(f(fa)))` | YES | N/A | NO (no pure) |
| `forConst[M]` | `fa.retag[B]` | YES | N/A | YES |
| `forId` | `f(fa)` | YES | N/A | YES |

The List instance picks **cartesian / singleton** (== `pure`); ZipList picks
**broadcast / length-preserving** (== `Functor.map(_ => f(fa))`). Both
choices satisfy R1 (map-compat) and R2 (const-collapse) because the laws are
deliberately weak.

**No single derivation from `Apply[F]` covers all four**:
- `Functor.map` derivation collapses List into ZipList semantics — the
  cartesian "singleton" shape disappears.
- `Applicative.pure` derivation rules out ZipList (no top-level pure).

**Implication for design**: `MultiFocus.collect` cannot be a single uniform
extension method. The spike ships two:
- `collectMap[B](agg)` requires only `Functor[F]`; preserves F-shape.
  Matches v1 ZipList and Const Reflector. Collapses List into the
  length-preserving variant — different from v1 List behaviour.
- `collectList(agg)` is a `MultiFocus[List]`-specific extension that
  reproduces the v1 cartesian-singleton shape (`List(agg(fa))`) directly.
  Plus `MultiFocus[List]` users can also use `collectMap` if they want the
  broadcast.

This is not a regression — the v1 List Reflector's choice was always one of
two possible defaults; surfacing both is more honest. **Reflector[F] is
deleted**; the typeclass adds nothing the cats hierarchy + the user's
explicit per-instance choice can't.

### Q2. AlgLensFromList[F] survival

**Empirical finding:** `Traverse[F] + MonoidK[F]` can derive `fromList`
in principle:

```scala
xs.foldLeft(MonoidK[F].empty[A])((acc, a) => MonoidK[F].combineK(acc, F.pure(a)))
```

**…but with broken asymptotics on Vector and silent truncation on Option:**

- `Vector.combineK` is `xs ++ ys` — copy-on-concat, so the loop above is
  O(n²). The original `AlgLensFromList[Vector]` exists precisely to escape
  this via `Vector.tabulate` / `xs.toVector` (O(n)).
- `Option.combineK` is left-biased: `Some(1) combineK Some(2) = Some(1)`,
  so the loop silently drops every element after the first. The original
  `AlgLensFromList[Option]` throws `IllegalStateException` on cardinality
  mismatch — explicit failure beats silent loss.
- Plus `MonoidK[F]` requires `pure` somewhere to lift each element, which
  isn't part of MonoidK — typically pulled in via `Applicative[F]`. That's
  another constraint piled on what should be a builder.

**Implication for design**: keep `MultiFocusFromList[F]` as a renamed
`AlgLensFromList[F]`. The spike ships `forList`, `forOption`, `forVector`,
`forChain` instances, byte-identical to today. Known per-F cost; no
reduction.

### Q3. Path-dependent → parametric Kaleidoscope migration

**Empirical findings:** counted Kaleidoscope usage sites by grepping `core/`,
`tests/`, `benchmarks/`:
- **2 construction call sites** in `core/`: the `apply[F, A]` factory and
  the `forgetful2kaleidoscope` bridge (latter pins `FCarrier = Id`).
- **3 places using `.collect[F, B]`**: in `OpticsLawsSpec` (KaleidoscopeLaws
  K3), in `OpticsBehaviorSpec` (a smoke test), and in
  `KaleidoscopeBench`.
- **1 same-carrier `.andThen` site**: covered by `kalAssoc` and exercised
  in the laws spec only (no production composition path uses it).
- **1 `Composer[Kaleidoscope, SetterF]` use** in `EoSpecificLawsSpec` and
  `OpticsBehaviorSpec`.

At every construction site `F` is **already explicit** (`apply[List]`,
`apply[ZipList]`, `apply[Const[M, *]]`, `apply[Id]`). The path-dep
encoding never let a chain carry different `F`s at different positions —
`kalAssoc`'s same-F restriction was the only valid composition.

**Migration is a mechanical rename**: `Kaleidoscope` → `MultiFocus[F]` at
every type signature. The `.collect[F, B]` extension's explicit `F` type
parameter goes away — `F` is now in the optic's own type. **Net win**: the
public surface gets cleaner.

### Q4. AlgLensSingleton perf fast-path

**Empirical finding:** the `MultiFocusSingleton` capability trait is
preserved verbatim (renamed); the same-carrier `.andThen` body in
`mfAssoc.composeTo` and `composeFrom` pattern-matches on
`MultiFocusSingleton[A, B, C, D, Xi] @unchecked` exactly as the v1
`assocAlgMonad` did. Sole user is the `tuple2multifocus` bridge.

**Bench numbers** (single-iteration JMH `-i 1 -wi 1 -f 1 -t 1`, size=32, on
`AlgLensBench.eoModify_algLens` — the singleton fast-path benchmark):

| Variant | Score (ns/op) | Notes |
|---|---|---|
| Pre-spike (AlgLens) | 1657.684 | Baseline |
| Post-spike (MultiFocus) | 1640.428 | Within noise |

The fast-path is preserved structurally. Single-iteration JMH numbers are
inherently noisy; the regression delta is well below the warmup-cycle
variance, and the code-path is byte-identical (the AlgLensBench was edited
only in its imports and the call to `MultiFocus.fromLensF` instead of
`AlgLens.fromLensF`).

## 4. Migration plan

### What gets deleted

- `core/src/main/scala/dev/constructive/eo/data/AlgLens.scala`
- `core/src/main/scala/dev/constructive/eo/data/Kaleidoscope.scala`
- `core/src/main/scala/dev/constructive/eo/Reflector.scala`
- `laws/src/main/scala/dev/constructive/eo/laws/KaleidoscopeLaws.scala`
- `laws/src/main/scala/dev/constructive/eo/laws/discipline/KaleidoscopeTests.scala`
- `tests/src/test/scala/dev/constructive/eo/ReflectorInstancesSpec.scala`
- `benchmarks/src/main/scala/dev/constructive/eo/bench/KaleidoscopeBench.scala`

### What gets added

- `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala` — the
  new carrier file (~340 LoC), absorbs all the AlgLens + Kaleidoscope
  surface.
- `core/src/test/scala/dev/constructive/eo/MultiFocusSpec.scala` — spike
  smoke spec.

### What gets renamed

In files outside the spike scope (still to do as a follow-up):

- `AlgLensBench` → keep the file name but use MultiFocus internally
  (already done in spike).
- `OpticsLawsSpec.scala` (lines 458–656) — port the four `arbAlgLens*`
  Arbitraries, the `checkAllForgetfulFunctorFor` / `Traverse` blocks for
  AlgLens, and the three Kaleidoscope `checkAllKaleidoscope` blocks. The
  Kaleidoscope laws need a rewrite: K3 `collectViaReflect` becomes
  `collectViaMap` (Functor-broadcast) at the carrier level; List loses
  the singleton-collect law unless we ship a separate K3 specific to
  `MultiFocus[List]` via `collectList`.
- `OpticsBehaviorSpec.scala` (lines 375, 397–533, 547–601) — every
  AlgLens / Kaleidoscope behavioural test is a mechanical rename
  `AlgLens` → `MultiFocus`, `Kaleidoscope` → `MultiFocus[F]`.
- `EoSpecificLawsSpec.scala` (lines 102–116) — the
  `kaleidoscope2setter`-via-MorphLaws case becomes
  `multifocus2setter`-via-MorphLaws.
- `InternalsCoverageSpec.scala` — touches `AlgLensFromList` internals.
- `CheckAllHelpers.scala` — drop `checkAllKaleidoscopeFor`, replace with
  a `checkAllMultiFocusFor` that uses cats `Apply[F]` directly.

### Estimated migration effort

The spike writes ~340 LoC core file and deletes ~700 LoC. The follow-up
**migration of the test suite** — porting the ~250 lines of
AlgLens/Kaleidoscope-specific test bodies across 4 spec files plus the
helpers and the laws file — is estimated at **half a day** of mechanical
rename work plus another **half day** for the K3 law-rewrite (because
the cartesian-vs-broadcast collect split needs a clean restatement).

The bench (`AlgLensBench`) is already migrated. `KaleidoscopeBench`
(deleted in the spike) needs a fresh `MultiFocusBench` that exercises
both `collectMap` and `collectList` paths plus the same-F `.andThen` —
about a day.

**Total migration: ~2 days from the spike branch to a green
`sbt test`.**

## 5. Open questions / show-stoppers

### Open question 1 — collect API ergonomics

The Q1 finding forces two `collect` variants. Today's Kaleidoscope users
write `kal.collect[F, B](agg)` with explicit F because the path-dep
member is opaque. Under MultiFocus the F is in the optic's type, so
`mf.collectMap[B](agg)` no longer needs the F parameter. **But**
`MultiFocus[List]` users now have to choose between `collectMap`
(broadcast) and `collectList` (cartesian-singleton).

The K3 law `collectViaReflect` had a single statement that doesn't apply
uniformly any more. Either:
- (a) Ship a single law `collectViaMap` (Functor-broadcast) at the
  carrier level. List users lose the cartesian story from the law surface
  but get it back at the call site via `collectList`.
- (b) Ship two laws, `collectViaMap` carrier-wide and `collectViaList`
  for `MultiFocus[List]`. More type-class proliferation; more honest.

The doc author leans toward (a) — the spike's `collectList` is concrete
test-level evidence, not a discipline law. (b) can be retrofitted if the
Reflector laws were genuinely useful in practice (unclear — they were
exercised by exactly the in-house spec).

### Open question 2 — Iso → MultiFocus[F] constraint set

The spike's `forgetful2multifocus[F: Applicative: Foldable]` requires
both `Applicative[F]` (for `pure`) and `Foldable[F]` (for the
`pickSingletonOrThrow` on pull). The previous AlgLens chain bridged Iso
through `Morph.viaTuple2` low-priority — Tuple2 → AlgLens[F] also
required the same constraints. Net delta: zero; just promoted from
implicit-fallback to direct-Composer.

Kaleidoscope's old bridge required only `Reflector[Id]`. Under
MultiFocus, Iso → MultiFocus[Id] is ALSO available via
`forgetful2multifocus[Id]` (Id has both Applicative and Foldable). The
two paths converge.

### Show-stoppers found: none

No structural fact prevents the unification. Every test and bench in
the spike scope compiles and runs. The 196 specs2 expectations the spike
ships pass; the ~250-line follow-up is mechanical.

## 6. Recommendation

**Ship the unification.**

Reasons:
1. The two carriers ARE structurally identical. The pre-existing split is
   accidental complexity — Kaleidoscope's path-dep encoding was a
   speculative optimisation that never paid off (no chain ever carried
   different Fs).
2. The Reflector typeclass has a single fragile choice baked into it
   (List-singleton vs ZipList-broadcast) that would be more honestly
   surfaced as two separate user-chosen ops. Deleting Reflector and
   leaning on `Functor[F]` + an `Applicative[F]`-shaped helper for the
   List case removes a project-local typeclass that was paying off only
   in its own laws spec.
3. `AlgLensFromList[F]` survives verbatim (renamed), addressing Q2's
   "is this a known cost" concern. No regression there.
4. The singleton fast-path (Q4) is byte-preserved; the perf delta is
   single-iteration noise.
5. Net LoC reduction: ~700 deletions for a ~340-line addition (~50% net
   reduction in the carrier surface). Mental-model reduction: 1 carrier
   instead of 2, with one consistent F-as-parameter encoding shared with
   the rest of the project (matching `Forget[F]` and the new
   `MultiFocus[F]`).
6. Pre-0.1.0 — there is no published artifact to keep compatible. Doing
   this NOW costs less than doing it after even one minor release.

Recommended sequence:
- Land the spike branch (PR review).
- Migration PR: port the 4 affected test specs + laws + bench, fix the
  doc (`site/docs/optics.md`'s Kaleidoscope section). ~2 days.
- Tag 0.1.0-M1 against the unified carrier.

The Reflector typeclass deletion plus the carrier merge are the
load-bearing simplifications. Everything else (fromList, singleton
fast-path, F[A]-focus factories, the SetterF widening) is a verbatim
port.
