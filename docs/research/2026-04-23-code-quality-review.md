---
title: "Code-quality review for the 0.1.0 surface"
date: 2026-04-23
type: review
status: substitution-for-codescene
scope: core, laws, circe, generics
---

# Pre-0.1.0 code-quality review (simplicity / YAGNI lens)

## Substitution note — Codescene MCP was NOT available

This review substitutes the simplicity-reviewer lens for the Codescene
MCP that the user would normally consult. **Codescene-shaped gaps that
this review cannot measure:**

- **Change-coupling metrics** — which files co-change across git history
  (e.g. does every `PowerSeries.scala` edit also touch
  `optics/Traversal.scala`?). A strong signal for hidden coupling.
- **Code-health hotspots over time** — Codescene combines complexity,
  churn, and authorship into a ranked "where is the rot?" list. Here
  we only have a point-in-time complexity snapshot.
- **Refactoring targets ranked by business value** — Codescene scores
  each hotspot by how much velocity it is costing; we can only flag
  *that* a section is complex, not *what* the fix is worth.
- **Team-knowledge risk** — files with a single author, or files the
  reviewer hasn't touched recently. Not computable from the working
  copy alone.
- **Cohesion / god-class detection** — Codescene's class-cohesion
  metric would likely flag `core/src/main/scala/eo/optics/Optic.scala`
  (14 extension groups on one companion) and the four `Json*.scala`
  files in circe, but this review can't put a number on it.

Where appropriate, the section below calls out individual findings
that would normally be part of the Codescene output.

## Executive summary — top 5 findings

1. **Orphaned family scaffolding: `core/src/main/scala/eo/Reflector.scala`
   has no carrier, no spec, and no references** outside its own
   Scaladoc. It was committed ahead of the Kaleidoscope plan (plan 006,
   queued after 0.1.0). Shipping 151 LoC + three typeclass instances
   whose sole purpose is supporting a family that does not yet exist
   is a pre-0.1.0 YAGNI violation. Fix: delete the file (can be
   re-added verbatim when the Kaleidoscope carrier lands) **or** move
   it out of `core/src/main` into a branch / scratch location before
   cutting 0.1.0.

2. **Core coverage regressed to 58.83% statement / 52.84% branch**
   from the Unit-8 baseline of 68.30% / 70.77%. The regression was
   caused by landing `AlgLens.scala`, `Forget.scala`, and the fused
   `.andThen` overloads on `Iso` / `Prism` / `Lens` / `Optional`
   without matching fixtures in `tests/`. See the "Coverage snapshot"
   section for per-file numbers — the below-80% list is 14 files long.

3. **`circe/` ships four ~400–690-LoC classes with high internal
   duplication.** Every one of `JsonPrism`, `JsonTraversal`,
   `JsonFieldsPrism`, `JsonFieldsTraversal` carries both a
   pre-v0.2 `*Unsafe` implementation and a parallel `*Ior` implementation
   of the same walk (navigate → update → rebuild), plus a separate
   "generic Optic contract" `to` / `from` pair that's only reachable
   through the abstract `Optic` surface. Each pair is a near-identical
   loop with the only difference being success/failure accumulation.
   This is the biggest single source of line count and the most likely
   Codescene hotspot in the module. See the per-module section for
   specifics.

4. **Unjustified / invariant-preserving casts concentrated in
   `PowerSeries.scala` and `AlgLens.scala`** are mostly documented
   by adjacent comments, but **two casts are explicit sentinels that
   deserve a test rather than a comment**: `null.asInstanceOf[Xo]` in
   `Grate.scala:108` (sentinel-broadcast fallback) and the `null`
   sentinel placeholder in `Grate.apply` at `Grate.scala:178`. If any
   future Grate carrier reads the synthesized focus via
   `.to(fa)._1`, those nulls become observable. Either write an
   explicit test pinning the invariant, or document in code via an
   `assert(false, "...")` path.

5. **Layer convention is consistent but the laws module imports
   from `optics.Optic` everywhere** (every law file does `import
   optics.Optic` and `import optics.Optic.*`). This is the accepted
   architecture (optic-family laws depend on the optic surface),
   NOT a violation — but it does mean downstream users cannot
   depend on `cats-eo-laws` without also pulling in the concrete
   optics package. Flagging for the 0.1.0 release notes: `laws`
   and `core` are co-versioned; there is no "pure law equations"
   module.

The remaining findings are mostly individual items rather than
patterns: a couple of unused type-parameter declarations
(`SetterF.scala:26, 38`), one dead `given tupleInterchangeable`
that was kept as a general `(A, B) => (B, A)` implicit, some
consistent-naming wins, and a handful of Scaladoc-missing items on
trait bodies that belong to the 0.1.0 surface.

---

## Coverage snapshot

Run: `sbt "clean; coverage; tests/test; coverageReport"` (2026-04-23,
after landing AlgLens + Forget + Grate + fused andThen overloads).

| Module | Statement | Branch |
|--------|----------:|-------:|
| `core`     | **58.83 %** | **52.84 %** |
| `laws`     | 100.00 %    | 100.00 % |
| `generics` | 28.88 %     | 31.34 % |
| `circe`    | — (tests not run by `tests/test`) | — |

**Regression vs 2026-04-17 baseline** (documented in
`docs/solutions/2026-04-17-coverage-baseline.md`): core statement
dropped from 68.30 % → 58.83 % and branch from 70.77 % → 52.84 %.
The drop is explained by new material (AlgLens, Forget, Grate, and
fused andThen overloads on every concrete optic subclass) landing
with partial fixtures.

### Core files below 80% statement coverage (sorted ascending)

| File | Stmt | Notes on the gap |
|------|------|------------------|
| `eo/optics/Iso.scala` | **5.26 %** (2/38) | Five fused `andThen` overloads on `BijectionIso` + per-overload `to` / `from` stubs — only the base `Iso.apply` path is witnessed. |
| `eo/optics/Prism.scala` | **12.28 %** (14/114) | Same story: `MendTearPrism` has 5 fused overloads + inherited members; `PickMendPrism` has another 3. Only a handful are exercised in `OpticsBehaviorSpec`. |
| `eo/optics/Optional.scala` | **21.92 %** (16/73) | 4 fused `andThen` overloads on `Optional` not exercised beyond the `OpticsLawsSpec` default paths. |
| `eo/optics/Lens.scala` | **25.00 %** (19/76) | 5 fused `andThen` overloads on `GetReplaceLens` + `SimpleLens` + `SplitCombineLens` partially witnessed. |
| `eo/data/IntArrBuilder.scala` | **40.62 %** (13/32) | `unsafeAppend` not exercised; `grow` partially covered. Hot path hits `append` + `freeze` only. |
| `eo/data/PSVec.scala` | **55.56 %** (70/126) | `Slice.unsafeShareableArray` non-dense branch, `equals`/`hashCode`/`toString` overrides under-exercised. |
| `eo/data/Affine.scala` | **55.56 %** (35/63) | `ofLeft` / `ofRight` / Either-shaped `apply` seldom called; `widenB` fast path witnessed only through AlgLens. |
| `eo/data/PowerSeries.scala` | **61.60 %** (146/237) | The slow "general" path in `assoc.composeTo` / `composeFrom` is mostly skipped because every real bridge has a `PSSingleton` fast path; that code is inductively dead at 0.1.0. |
| `eo/data/Forgetful.scala` | **63.89 %** (23/36) | `accessor` / `reverseAccessor` / `applicative` givens not all exercised in specs. |
| `eo/Composer.scala` | **66.67 %** (12/18) | `chain` transitive-Composer not witnessed; `forgetful2either.from`'s Left branch is `???`-unreachable. |
| `eo/data/Forget.scala` | **66.67 %** (24/36) | `forgetFFunctor` / `forgetFApplicative` / `forgetFFold` instance bodies only covered when a spec actually threads a Forget through. |
| `eo/optics/Optic.scala` | **67.05 %** (59/88) | `innerProfunctor`, `transform` / `transfer`, `put`, `reverse` fused bodies partially covered. |
| `eo/ForgetfulTraverse.scala` | **68.42 %** (13/19) | `tupleFTraverse` (Functor bound) not reached — laws pin only the `Applicative`-bound version. |
| `eo/data/ObjArrBuilder.scala` | **69.39 %** (34/49) | `unsafeAppend` and `appendAllFromPSVec` `Slice` branch under-exercised. |
| `eo/data/Grate.scala` | **74.34 %** (113/152) | `Grate.at` not in specs; `grateAssoc.composeFrom` sentinel path under-exercised. |
| `eo/data/SetterF.scala` | 75.00 % (3/4) | Only two statements in the whole file; thin file. |
| `eo/AssociativeFunctor.scala` | 79.49 % (31/39) | `eitherAssocF` Left-of-Left branch partially covered. |

### Files at target

`AlgLens.scala` (91 %), `Morph.scala` (81 %), `Traversal.scala` (83 %),
`FixedTraversal.scala` (100 %), `Accessors.scala` (100 %), every
`optics/*Fold|Getter|Setter|AffineFold|Review|Fold.scala`, and the
four rank-1 typeclass files (`ForgetfulFunctor.scala`,
`ForgetfulFold.scala`).

### `generics/` 28.88 %

This is expected — `generics/src/main/scala/eo/generics/LensMacro.scala`
and `PrismMacro.scala` are Scala 3 macros whose statements only
count at compile time of downstream client code. scoverage treats
macro expansions as uncovered unless an exact compile of an inlined
client triggers instrumentation. Not a real quality concern; mention
in release notes.

### Circe coverage

`circe/` tests are defined in `circe/src/test` but `tests/test` does
not include the `circeIntegration` sub-project. As a result the
coverage run produced `[warn] No coverage data, skipping reports`
for circe. **This is a process gap**: if the release pipeline uses
`coverageReport` as a quality gate, circe is silently excluded.

---

## Per-module findings

### core/

#### YAGNI / premature-abstraction

- **`core/src/main/scala/eo/Reflector.scala`** — *entire file*.
  Defines `trait Reflector[F] extends Apply[F]` plus three instances
  (`forList`, `forZipList`, `forConst`). The Scaladoc at line 5
  says "Classifying typeclass for the [[data.Kaleidoscope]] optic
  family". `data.Kaleidoscope` does **not** exist in this codebase
  (`Glob("**/Kaleidoscope*.scala")` → empty), nor does
  `ReflectorInstancesSpec.scala` referenced at `Reflector.scala:36`.
  Every piece of infrastructure this typeclass was designed to
  support is in the future-work queue (`docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md`).
  **151 LoC of speculative extension.** Delete, ship in the
  Kaleidoscope PR when the carrier exists.

- **`core/src/main/scala/eo/optics/Lens.scala:22-24`** —
  `given tupleInterchangeable[A, B]: (((A, B)) => (B, A))`. Declared
  in `Lens`'s companion; never referenced by core, laws, or any spec
  (grep for `tupleInterchangeable` returns this site only, and
  `.swap` is used inline at `Lens.scala:81` for `Lens.first`, not via
  this implicit). The companion Scaladoc says "used by `Composer`
  bridges to swap `Tuple2`'s sides" but no such Composer exists —
  `Composer[Tuple2, ?]` instances in the codebase do not summon this
  evidence. Either un-declare it or pin it with a test that actually
  depends on it.

- **`core/src/main/scala/eo/data/Forget.scala:127-149`** —
  `LowPriorityForgetInstances.assocForgetComonad` is kept "at lower
  priority so that whenever an `F` is a full `Monad` the
  algebraic-lens composition wins". Inspection: no test summons
  `assocForgetComonad`, and no shipping `Forget[F]` instance uses an
  `F` that is `FlatMap + Comonad` but not `Monad`. The docstring
  at line 122 is candid: "Users who want the Comonad-based
  parallel-fold semantics explicitly can still summon this instance
  by hand." That's fine as a design escape hatch, but if no user ever
  does so, the trait + given + low-priority gymnastics cost more to
  understand than to delete. **Candidate for post-0.1.0 removal** —
  document it as deprecated in 0.1.0 and delete in 0.2.0 if nobody
  depends on it.

- **`core/src/main/scala/eo/ForgetfulApplicative.scala:19`** —
  `object ForgetfulApplicative`. The body is empty ("kept as a
  companion stub in case downstream carriers grow into it"). The
  Forgetful instance already lives in `data/Forgetful.scala`; the
  Forget[F] one in `data/Forget.scala`. Keeping an empty companion
  for a future extension point is a YAGNI violation. No cost to
  having it for now, but when cleaning up for 0.1.0 it can go.

- **`core/src/main/scala/eo/data/AlgLens.scala:282-300`** —
  `pickSingletonOrThrow` throws `IllegalStateException` on an
  "unreachable by construction" case. The doc at line 286 explains
  the reachability argument, and the only call sites are the
  bridges' own `from` methods. Fine as a defensive strategy, but
  the dead throw branch inflates the statement count and shows up
  in coverage as never-exercised. Consider collapsing to a direct
  `Foldable[F].reduceLeftToOption(fb)(identity).get` — any caller
  violating the invariant would NPE at `.get`, same end result,
  fewer statements.

#### Dead / unreachable code

- **`core/src/main/scala/eo/Reflector.scala`** — entire file, see
  YAGNI.
- **`core/src/main/scala/eo/optics/Lens.scala:22-24`** — unused
  given, see YAGNI.
- **`core/src/main/scala/eo/ForgetfulApplicative.scala:19`** — empty
  companion stub.
- **`core/src/main/scala/eo/Composer.scala:62-63`** —
  `forgetful2either.from`'s `Left` branch:
  ```
  val from: Either[X, B] => T = e =>
    e match
      case Right(b) => o.from(b)
      case Left(_)  => ???
  ```
  `???` is documented as unreachable because `X = Nothing` for this
  composer — but there's no test that demonstrates it. The code
  isn't dead at compile time (Scala requires total matches), but it
  is dynamically unreachable and shows up as uncovered in scoverage.
  Fine as-is; mentioning for completeness.
- **`core/src/main/scala/eo/data/SetterF.scala:26, 38`** — both
  `given map[S, A]` and `given traverse[S, A]` declare `S, A` type
  parameters that are never used in the instance body. Remove the
  type parameters — they declare a family of instances where one
  would do, which (in Scala 3) can make implicit resolution
  marginally more expensive and certainly adds conceptual clutter.

#### Coupling / layer violations

None. `core/` imports only `cats.*` and self-refers via
`eo.optics.*` / `eo.data.*`.

#### Scaladoc gaps (0.1.0 surface)

Items below are public, on the released surface, and lack an
immediately-preceding `/** ... */`:

- `core/src/main/scala/eo/Accessors.scala:17` — `object Accessor` (the
  companion for the `Accessor` trait). Trait has full Scaladoc; its
  companion doesn't.
- `core/src/main/scala/eo/Accessors.scala:38` — `object ReverseAccessor`.
  Same pattern.
- `core/src/main/scala/eo/Morph.scala:31-33` — `Morph` trait's three
  abstract members (`type Out`, `def morphSelf`, `def morphO`). The
  trait-level Scaladoc is rich but the members themselves have no
  per-member doc.
- `core/src/main/scala/eo/Morph.scala:54` — `object Morph`. Re-extends
  `LowPriorityMorphInstances` and hosts the three high-priority given
  instances; no doc.
- `core/src/main/scala/eo/optics/Optic.scala:93` — `object Optic`. The
  trait has a full header; the companion with its extensions /
  constructors does not.
- `core/src/main/scala/eo/optics/Lens.scala:237` — `object SimpleLens`
  (holds the `transformEvidence` given). No doc.
- `core/src/main/scala/eo/optics/Review.scala:27` — `object Review`.
  No doc.
- `core/src/main/scala/eo/data/Affine.scala` — `type Fst[T]` and
  `type Snd[T]` have *class-level* docs (line 10-16). No gap there.
  But the `type X = (T, S)` member inside `Optional` at
  `Optional.scala:106` has no doc.
- `core/src/main/scala/eo/optics/Prism.scala:80, 209` — `type X = T`
  (inside `MendTearPrism`) and `type X = S` (inside `PickMendPrism`).
  No per-member doc.
- `core/src/main/scala/eo/optics/Iso.scala:48` — `type X = Nothing`
  inside `BijectionIso`. No doc.
- `core/src/main/scala/eo/optics/Lens.scala:106, 198` — `type X = S`
  and `type X = XA`. No doc.
- `core/src/main/scala/eo/data/AlgLens.scala:77, 83, 97, 103` — the
  four `AlgLensFromList` instances (`forList`, `forOption`,
  `forVector`, `forChain`). No per-instance doc. Trait has rich docs.
  Since `AlgLensFromList` is `private[eo]` these are effectively
  internal; the rule may not apply.
- `core/src/main/scala/eo/data/AlgLens.scala:106` — `object AlgLens`.
  Type alias has rich docs; companion does not.
- `core/src/main/scala/eo/data/FixedTraversal.scala:35, 41, 47` — the
  three given `ForgetfulFunctor` instances. No per-instance doc.
  Companion-level doc at line 24 covers them all, but the pattern
  elsewhere in the codebase is per-given docs.
- `core/src/main/scala/eo/data/PSVec.scala:89` — `object PSVec`. No
  doc; the sealed trait has full docs.
- `core/src/main/scala/eo/data/PowerSeries.scala:24, 36, 164, 307,
  362, 407` — six `given` instances without immediately-preceding
  Scaladoc. `given map` at line 24 and `given traverse` at line 36
  are the most surface-facing.

#### Naming drift

- **`MendTearPrism.tear` / `.mend`** vs **`PickMendPrism.pick` /
  `.mend`**. The two classes share `mend`, but the other half is
  `tear` (return `Either[T, A]`) in one and `pick` (return
  `Option[A]`) in the other. The name difference is deliberate and
  documented at `Prism.scala:205` — but a reader skimming the Prism
  API encounters `tear` and `pick` side-by-side with no explicit
  "here's when to use which". Consider adding a cross-ref Scaladoc
  on each method pointing at its sibling.
- **`Accessor.get` / `ReverseAccessor.reverseGet`** — single-function
  typeclasses with different method names for symmetry with the
  `Optic.get` / `Optic.reverseGet` extensions. Fine, consistent.
- **`Optional.getOrModify`** vs **`AffineFold.fromOptional` uses
  `o.getOrModify(s).toOption`**. The naming is consistent with
  Monocle, so no action needed.
- **`MendTearPrism.reverseGet(b)`** (inline) vs **`PickMendPrism.reverseGet(b)`**
  (inline) — same name / shape; consistent.

#### Complex / magic passages

- **`core/src/main/scala/eo/data/AlgLens.scala:181-266`** —
  `assocAlgMonad` is ~85 lines with a `match { case is:
  AlgLensSingleton ... case _ ... }` at composeTo and composeFrom,
  each arm another 15–25 lines. The complexity is justified by the
  O(n) performance contract and the fast-path vs general-path
  trade-off. Add a one-line comment at the top of the method
  indicating the dispatch structure.

- **`core/src/main/scala/eo/data/PowerSeries.scala:164-260`** —
  `assoc` with three arms (`AlwaysHit` / maybe-hit / generic),
  each ~20 lines, plus the private classes `GetReplaceLensInPS`,
  `GenericTuple2InPS`, `EitherInPS`, `AffineInPS` that conform
  to the `PSSingleton` protocol. Complexity is load-bearing (this
  is the hot path) and documented well by adjacent comments.
  Codescene would flag this as a complexity hotspot but the work
  has been done to justify it. Keep.

- **`core/src/main/scala/eo/data/Grate.scala:88-113`** —
  `grateAssoc.composeTo` / `composeFrom` rely on the sentinel
  `null.asInstanceOf[Xo]` cast (line 108) documented as "safe by
  construction". The argument is valid for every v1 carrier; a
  third-party Grate outer that reads per-slot rebuilds would
  under-approximate. Consider guarding with an
  `require(false)`-style path or replacing with a sealed marker
  trait that witnesses "my outer-rebuild ignores the Xo slot". For
  now, the doc is sufficient.

#### Unjustified `asInstanceOf`

Every cast in core is adjacent to a justifying comment. Notable ones:

- `core/src/main/scala/eo/optics/Optic.scala:86-87` — the two
  `outerRef` / `innerRef` casts refine `type X = self.X` / `type X
  = o.X` on the optic, which Scala cannot prove through the
  existential. Standard pattern, documented at the `andThen`
  Scaladoc.
- `core/src/main/scala/eo/data/Affine.scala:218, 224` — cast the
  `Fst[Z]` / `Snd[Z]` match-type leftovers. The Scaladoc at
  line 166–190 justifies exhaustively.
- `core/src/main/scala/eo/data/Grate.scala:108, 178` — the two
  `null.asInstanceOf[Xo]` / `null.asInstanceOf[A]` sentinels. **These
  are the least-justified casts in core**: the "safe by construction"
  argument is at the level of "all v1 consumers" rather than
  "provable invariant". If someone adds a Grate carrier whose
  `.to(fa)._1` is observed, these nulls become bugs. See Finding 4
  in the executive summary.
- `core/src/main/scala/eo/data/PowerSeries.scala:255, 305` —
  `ys(i).asInstanceOf[Snd[Xi]]` and `y.asInstanceOf[o.X]`. The match
  types are invariant under the existential, standard pattern.
- `core/src/main/scala/eo/data/PSVec.scala:137, 138, 145, 170` —
  `arr(...)`.asInstanceOf[B]` on the `Array[AnyRef]` backing store.
  Well-documented at line 13 ("No ClassTag required").

#### TODO / FIXME markers

None in core.

---

### laws/

#### YAGNI / premature-abstraction

- **`laws/src/main/scala/eo/laws/eo/ChainLaws.scala:50-63`** —
  `ChainAccessorLaws[S, A, F, G, H]`. The Scaladoc says "Currently
  core only ships `Accessor` instances for `Forgetful` and `Tuple2`,
  so the only non-degenerate witness is `Forgetful → Tuple2 →
  Tuple2` with an identity composer at the second hop — testable
  with a locally-declared identity `Composer` in the spec. The trait
  itself is fully generic and will gain more witnesses as new
  `Accessor` instances are added." Right now there is **zero**
  non-degenerate witness at 0.1.0. The trait + discipline ruleset
  exist but nothing concrete exercises them. Consider deferring the
  trait until there's a real `Accessor[F]` beyond Forgetful / Tuple2
  (which the Kaleidoscope plan might bring).

- **`laws/src/main/scala/eo/laws/eo/FoldAndTraverseLaws.scala:62-67`** —
  `ForgetAllModifyLaws[T, A]` is phrased in terms of `Forget[T]`
  specifically. Scaladoc at line 37 is candid about why: `Optic.all`
  uses `traverse(List(_))` and for `Forget[T]` carriers that
  collapses to a single-element list wrapping the whole container.
  The law pins that observation, but the pin is extremely narrow:
  it only holds for `Forget[T]` carriers. That's fine as a
  regression witness, but it would be stronger as a unit test
  against a concrete carrier, not as a law-family trait. Worth
  revisiting post-0.1.0 if / when a second carrier with
  `ForgetfulTraverse[_, Applicative]` gets added.

#### Dead / unreachable code

No dead code; every law trait is referenced by a matching discipline
`*Tests.scala` class, and both sides are referenced by the
`tests/src/test/scala/eo/*Spec.scala` suite (not in scope for this
review but verified via `sbt test` running 102 examples).

#### Coupling / layer violations

The laws module imports `eo.optics.Optic` and `eo.optics.Optic.*`
in every single law file (verified via `Grep`):

```
laws/src/main/scala/eo/laws/IsoLaws.scala
laws/src/main/scala/eo/laws/OptionalLaws.scala
laws/src/main/scala/eo/laws/GrateLaws.scala
laws/src/main/scala/eo/laws/GetterLaws.scala
laws/src/main/scala/eo/laws/PrismLaws.scala
laws/src/main/scala/eo/laws/AffineFoldLaws.scala
laws/src/main/scala/eo/laws/LensLaws.scala
laws/src/main/scala/eo/laws/FoldLaws.scala
laws/src/main/scala/eo/laws/SetterLaws.scala
laws/src/main/scala/eo/laws/TraversalLaws.scala
laws/src/main/scala/eo/laws/eo/MorphLaws.scala
laws/src/main/scala/eo/laws/eo/FoldAndTraverseLaws.scala
laws/src/main/scala/eo/laws/eo/ComposeLaws.scala
laws/src/main/scala/eo/laws/eo/ReverseAndTransformLaws.scala
laws/src/main/scala/eo/laws/eo/ChainLaws.scala
laws/src/main/scala/eo/laws/eo/ModifyALaws.scala
```

This is the accepted architecture — law classes need the `Optic`
trait and its extension methods to express their equations — but it
means `cats-eo-laws` is not a standalone "law equations" module;
consumers pull in the concrete optics package transitively.

The `laws/src/main/scala/eo/laws/typeclass/` subtree imports only
`eo.ForgetfulFunctor` / `eo.ForgetfulTraverse` (typeclass surface),
not `eo.optics.*`. Clean layer.

The `laws/src/main/scala/eo/laws/data/` subtree imports carrier
types directly (`eo.data.Affine`, `eo.data.PowerSeries`, etc.) plus
the matching typeclasses. Clean; no `optics.*` dependency.

No real violation — flagging for 0.1.0 release-notes clarity.

#### Scaladoc gaps (0.1.0 surface)

- **`laws/src/main/scala/eo/laws/eo/ChainLaws.scala:59-60`** — the
  two abstract givens `accessorF` / `accessorH` are declared with no
  per-given doc. They're part of the `ChainAccessorLaws` trait's
  surface.

Every other law file has excellent Scaladoc — the trait header +
per-equation header is consistent across the module.

#### Naming drift

Consistent. The shape `modifyIdentity` / `composeModify` /
`replaceIdempotent` / `consistentReplaceModify` shows up in every
law file that has a modify path (Lens, Prism, Optional, Setter,
Traversal, Grate, FixedTraversal, Affine via AffineLaws). Good.

#### Complex / magic passages

None. Every trait is ≤ 50 LoC; no match expressions with > 6 arms;
no `while` loops; no bit-twiddling.

#### Unjustified `asInstanceOf`

Zero in laws — verified via `Grep -path laws/src/main`.

#### TODO / FIXME markers

None.

---

### circe/

#### YAGNI / premature-abstraction

- **`circe/src/main/scala/eo/circe/JsonPrism.scala:85-94`** —
  the abstract `to: Json => Either[(DecodingFailure, HCursor), A]`
  and `from: ...` members satisfy the `Optic[Json, Json, A, A, Either]`
  contract but are themselves a *third* implementation of the
  navigate-decode-encode loop (alongside the `*Ior` and `*Unsafe`
  siblings). They exist so `JsonPrism` can compose with other
  cats-eo optics via `.andThen`. Question for pre-0.1.0: how often
  does that compose actually fire in practice, relative to callers
  who stay inside `JsonPrism`-native `.field(_.x)` chains? The
  `CrossCarrierCompositionSpec` in `circe/src/test` confirms it
  works, but `site/docs/optics.md` doesn't show any example of
  composing `JsonPrism` with a non-`JsonPrism` optic. If users never
  reach for this, the abstract-member surface can be slimmed post
  0.1.0. **Recommendation**: measure usage by the `cellar
  search-external` route — if fewer than 5 callers in real code use
  `.to` / `.from` on a JsonPrism directly, consider moving them
  behind a `.asOptic` adapter in 0.2.0.

- **`circe/src/main/scala/eo/circe/JsonFieldsPrism.scala:65-89`** —
  same pattern: `to` / `from` for the generic `Optic` contract. The
  `JsonFieldsPrism.to` body is even more work than `JsonPrism.to`
  (it assembles a sub-object on every call). Confirm it's needed
  before the 0.1.0 release notes promise API stability.

- **`circe/src/main/scala/eo/circe/JsonPrism.scala:95-105,
  121-127`** — the `*Unsafe` escape-hatch family. Every method gets
  both a default (`Ior`-bearing) form and an `*Unsafe` form. The
  Scaladoc argues that callers who have measured and don't want the
  `Ior.Right(json)` allocation on the happy path can reach for the
  unsafe form. This doubles the API surface. Before 0.1.0 consider:
  is the allocation actually measured? The JMH benches in
  `benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala` don't
  differentiate between safe / unsafe. If the difference is < 5 ns
  / op, drop the `*Unsafe` variants for 0.1.0.

#### Dead / unreachable code

- **`circe/src/main/scala/eo/circe/JsonPrism.scala:85-94`** — the
  `to: Json => Either[...]` / `from: Either[...] => Json` methods
  are reachable only through the generic Optic contract. If no
  caller uses it outside the test suite (see YAGNI above), it's
  effectively dead in client-code paths.

- **`circe/src/main/scala/eo/circe/JsonFieldsTraversal.scala:313-318`** —
  `rebuildElemWithPrefix`. The helper takes an `@annotation.unused
  original: Json` parameter but simply calls `rebuildElem(newParent,
  parents)` — `original` is never used. Either drop the parameter
  (the call sites pass `elemJson` which is local to the caller) or
  use it and document the purpose.

#### Coupling / layer violations

- **`circe/` depends on `core/`** (normal, intended).
- **`circe/` does NOT depend on `laws/`** (`Grep` for `eo.laws` in
  `circe/src/main` returns nothing). Clean.
- No layer violations inside circe itself; all four classes live
  in the same package and share the same `PathStep` / `JsonFailure`
  primitives.

#### Scaladoc gaps (0.1.0 surface)

- `circe/src/main/scala/eo/circe/JsonFailure.scala:56` — `object
  JsonFailure`. The enum has full docs; the companion (which hosts
  the `Eq` instance and the two `parseInput*` helpers) does not.
- `circe/src/main/scala/eo/circe/JsonPrism.scala:60` — `type X =
  (DecodingFailure, HCursor)` inside the class body. No per-member
  doc.
- `circe/src/main/scala/eo/circe/JsonPrism.scala:486` — `object
  JsonPrism`. No doc.
- `circe/src/main/scala/eo/circe/JsonFieldsPrism.scala:57` — `type
  X = (DecodingFailure, HCursor)`. No per-member doc.
- `circe/src/main/scala/eo/circe/JsonTraversal.scala:652` —
  `object JsonTraversal`. No doc.

Every public method on these four classes has full Scaladoc; the
gaps are the companion objects and the `type X` members.

#### Naming drift

Consistent within circe: every class has
`(modify|transform|place|transfer)(Ior|Unsafe)?` + their
`(modify|transform|place|transfer)Impl` / `Ior` private helpers. If
you've seen `JsonPrism`, the other three feel natural.

One small drift: `JsonPrism.place(a)` vs `JsonTraversal.place(a)` —
the former places the value at one focus; the latter broadcasts the
value across every element of the iterated array. Scaladoc on
`JsonTraversal.place` at line 28 does clarify this, but a reader
looking at method signatures alone might expect per-element "apply
at this one element's place" semantics. Consider renaming
`JsonTraversal.place` → `JsonTraversal.replaceAll` for API-clarity
before 0.1.0 stabilises. **Higher-cost change**; flag as
post-0.1.0 polish.

#### Complex / magic passages

**Every update-loop method in circe is 20–40 lines of
pattern-match + imperative walk**. There's no way to write this
more idiomatically without sacrificing the zero-allocation hot path
that the docstrings repeatedly emphasise. The duplication between
`*Ior` and `*Unsafe` bodies (see the top-5 finding) is the main
target for a refactor pass. Concrete proposal:

> Extract a common `walkAndUpdate[F[_]](
>   path: Array[PathStep],
>   json: Json,
>   onMiss: PathStep => F[Nothing],
>   onLeaf: Json => F[Json]
> ): F[Json]` generalised over the "failure treatment"
> (Ior.Both vs input-unchanged). Every method in `JsonPrism`,
> `JsonTraversal`, `JsonFieldsPrism`, `JsonFieldsTraversal`
> becomes a four- or five-line body that picks an `F` and a
> `onMiss` handler. **Savings estimate: ~1200 LoC removed across
> the four files.** Cost: one new private helper with a tasteful
> type abstraction; a careful re-bench to confirm the inlined-
> allocation story still holds.

This is the single highest-ROI refactor in the whole review.
Recommended as a post-0.1.0 cleanup because it's structural and
wants a benchmark run.

#### Unjustified `asInstanceOf`

All eight circe casts are the same pattern:
```
Json.fromJsonObject(parent.asInstanceOf[JsonObject].add(name, child))
Json.fromValues(parent.asInstanceOf[Vector[Json]].updated(idx, child))
```
in the `rebuildStep` private method. The `parent` comes from an
`Array[AnyRef]` that was populated a few lines earlier with
`parents(i) = obj` (where `obj: JsonObject`) or `parents(i) = arr`
(where `arr: Vector[Json]`). The cast is load-bearing because
the array is `Array[AnyRef]` for storage uniformity. Each of the
four files has the same two casts, justified by the same pattern.
Fine.

#### TODO / FIXME markers

None.

---

### generics/

#### YAGNI / premature-abstraction

None — both macros are focused, minimal, and go through Hearth for
the load-bearing parts. `deriveMulti` is a single varargs entry
point dispatching three arms (partial single / partial multi /
full-cover-iso); no speculative arms.

#### Dead / unreachable code

- **`generics/src/main/scala/eo/generics/LensMacro.scala:88-99`** —
  `Varargs.unapply` fallback arm. The docstring at line 91 says
  "we haven't seen this, but the fallback path preserves correctness
  and points the user at the offending expression." That's an
  explicit admission that it's defensive. The cost is low (7 lines),
  the benefit is error-message quality for a case that the author
  hasn't observed. Accept as-is.

#### Coupling / layer violations

- **`generics/` depends on `core/`** (intended).
- **`generics/` does NOT depend on `laws/`** (verified via `Grep`).
- The macro implementations live in `final private class
  HearthLensMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3`
  — load-bearing dependency on `com.kubuszok:hearth`.

#### Scaladoc gaps (0.1.0 surface)

- **`generics/src/main/scala/eo/generics/package.scala:43`** — `final
  class PartiallyAppliedLens[S]`. The body has a single
  `transparent inline def apply` method; the class itself has no
  header doc. Users see this class in the `lens[S](_.field)`
  signature's middle.

#### Naming drift

- `LensMacro.deriveMulti` / `PrismMacro.derive`. The Prism macro is
  arity-one (single target variant), so `derive` is fine; the Lens
  macro is varargs, so `deriveMulti` makes sense. Consistent with
  the dispatch logic in each.

- Within `LensMacro`: `deriveSingle` / `buildLens` / `buildMultiLens`
  / `buildMultiIso`. Naming scheme is consistent:
  `derive*` is the entry, `build*` is the codegen. Clean.

#### Complex / magic passages

- **`generics/src/main/scala/eo/generics/LensMacro.scala:209-310`** —
  `buildLens` (single-selector partial-cover codegen). 100 lines of
  quoted-reflect code that synthesises the NamedTuple complement
  and the `cc.construct[Id]` primary-constructor thread. Each step
  has an adjacent explanatory comment; the complexity is inherent
  to what this method does (synthesise both halves of a Lens from
  a single-field selector). Accept.

- **`generics/src/main/scala/eo/generics/LensMacro.scala:329-462`** —
  `buildMultiLens` (multi-selector partial-cover codegen). Larger
  (~130 lines) because it has to build two NamedTuple types (focus
  and complement) and thread each declaration-order parameter
  through the constructor. The duplication of the "build a
  NamedTuple type from (names, types)" step (inline at both
  `buildLens:221-239` and `buildMultiLens:354-363` — the latter has a
  local `namedTupleTypeOf` helper, the former inlines it) can be
  factored out. **Savings: ~30 LoC, no behaviour change.** S-size.

#### Unjustified `asInstanceOf`

All `asInstanceOf` in `LensMacro` are inside quoted code (they
will be emitted into the user's compiled output). Each is justified
by the "NamedTuple's runtime is its Values tuple via opaque
subtyping" invariant, documented at lines 281–289 and repeated at
each use. Safe by the Scala 3 spec.

#### TODO / FIXME markers

None.

---

## Cross-module findings

### F1. Three parallel `navigate-path / rebuild-path` implementations in circe

The walk-and-rebuild pattern in circe is duplicated nine times
across the four main files (modify / transform / place / getAll,
each in `Ior` and `Unsafe` forms, plus the abstract `to` / `from`
for the Optic contract). Each instance is 20–40 lines. The simplest
measurable fix is the `walkAndUpdate[F]` extraction proposed above.
This is the single largest source of LoC in the module.

### F2. Fused `andThen` overloads contribute heavily to the core-coverage regression

Every concrete `Optic` subclass (`BijectionIso`, `GetReplaceLens`,
`SplitCombineLens`, `SimpleLens`, `MendTearPrism`, `PickMendPrism`,
`Optional`) ships multiple fused `andThen` overloads whose only
purpose is skipping a `Composer` bridge at runtime. At 0.1.0 the
fixtures witness:

- base `.andThen` via `AssociativeFunctor` — yes
- some fused overloads — yes
- **most fused overloads — no** (see the coverage table)

Each fused overload is ~15 lines + 3 lines of Scaladoc. There are
~15 such overloads across the concrete subclasses. That's ~220 LoC
of performance-only code that coverage reports as uncovered.
**Recommendation**: either add fixtures in `tests/` that exercise
each fused overload (post-0.1.0; S-size per overload, ~2 hours for
all 15), or accept the coverage dip as "performance-only fast paths,
exercised by the benchmark suite not the law suite."

### F3. `private[eo]` vs `private[data]` vs `private` drift

- `PowerSeries.scala` uses `private[eo] class AssocSndZ`,
  `private[eo] trait PSSingleton`, etc.
- `PSVec.scala` uses `private[data] val arr` inside `Slice`.
- `AlgLens.scala` uses `private[eo] trait AlgLensFromList` /
  `private[eo] object AlgLensFromList`.

Three different scoping levels (`private`, `private[data]`,
`private[eo]`) are used for "internal to the package / module"
markers. Since the module is just `core`, and the package structure
is shallow (`eo.*`, `eo.data.*`, `eo.optics.*`), the levels are
effectively interchangeable *within this module*. The drift matters
only for downstream consumers (who shouldn't import any of these
anyway). Fine as-is; documenting for Codescene parity.

### F4. Coverage instrumentation covers circe only if `tests/test` is extended

The root `sbt test` runs `tests/test` which DOES NOT include the
`circeIntegration` sub-project. `coverageReport` therefore reports
"No coverage data" for circe. Fix the release pipeline: either
change `tests/test` into `sbt test` (runs all aggregated tests,
including `circeIntegration/test`), or add `circeIntegration/test`
explicitly to the coverage-report command.

### F5. The `0.1.0` released artifact list includes `cats-eo-laws` which pulls in `cats-eo` transitively

`laws/src/main/scala/eo/laws/*` imports `eo.optics.Optic.*` in every
file. Users of `cats-eo-laws` get the optics package by default.
If this is intended, document. If not, consider a `cats-eo-core`
publish artifact (bare trait + data) and a `cats-eo-optics` publish
artifact (the existing optics surface), so `cats-eo-laws` can depend
on just the bits it needs. **Post-0.1.0; structurally disruptive.**

---

## Prioritized cleanup list

Ordered by (impact × ease-of-fix), with recommendation on
pre-0.1.0 vs post-0.1.0 timing.

### Must-fix for 0.1.0

1. **Remove `core/src/main/scala/eo/Reflector.scala`** — 151 LoC,
   speculative future-work scaffolding. S-size (delete + re-run
   tests). Pre-0.1.0.
2. **Remove `given tupleInterchangeable` at
   `core/src/main/scala/eo/optics/Lens.scala:22-24`** — 3 LoC.
   S-size. Pre-0.1.0.
3. **Document circe coverage is NOT run by `tests/test`** — either
   extend the coverage-report `sbt` incantation to include
   `circeIntegration/test`, or add a note to
   `docs/solutions/2026-04-17-coverage-baseline.md`. S-size.
   Pre-0.1.0.
4. **Fix the unused `@annotation.unused original: Json` parameter at
   `circe/src/main/scala/eo/circe/JsonFieldsTraversal.scala:313-318`** —
   drop the parameter and inline the call. S-size. Pre-0.1.0.
5. **Drop unused type parameters in SetterF instances** at
   `core/src/main/scala/eo/data/SetterF.scala:26, 38`. S-size.
   Pre-0.1.0.
6. **Fill the Scaladoc gaps** enumerated per-module above. Most are
   one-line "companion for [[X]]" comments. M-size total (~30
   one-liners). Pre-0.1.0.
7. **Add a single fixture that exercises the `Grate.apply` null
   sentinel's implicit "consumer never reads the focus" invariant**
   at `core/src/main/scala/eo/data/Grate.scala:178`, so a future
   regression trips a test rather than silently materialising a
   null. S-size. Pre-0.1.0.

### Nice-to-have for 0.1.0

8. **Add fixtures for the top-3 fused-andThen overloads** — `Iso`
   (5.26 % coverage!), `Prism` (12 %), `Optional` (22 %). Each
   fused overload is a 3-line spec that calls
   `.andThen(...).modify(...)` and checks equivalence with the
   unfused path. Pick the 5 most-user-facing overloads per class.
   M-size total, ~4 hours. Pre-0.1.0.
9. **Remove the empty `object ForgetfulApplicative` stub at
   `core/src/main/scala/eo/ForgetfulApplicative.scala:19`** —
   2 LoC. S-size. Pre-0.1.0.

### Post-0.1.0 / 0.1.1 / 0.2.0 candidates

10. **Extract `walkAndUpdate[F]` in circe** to collapse the nine
    parallel path-walkers across `JsonPrism` / `JsonTraversal` /
    `JsonFieldsPrism` / `JsonFieldsTraversal`. Est. **-1200 LoC**.
    L-size (requires a benchmark re-run to confirm zero perf
    regression). Post-0.1.0.
11. **Fold `pickSingletonOrThrow` at
    `core/src/main/scala/eo/data/AlgLens.scala:282-300` into direct
    `Foldable.reduceLeftToOption(_)(identity).get` calls.** S-size,
    -15 LoC. Post-0.1.0.
12. **Deprecate + remove `LowPriorityForgetInstances.assocForgetComonad`
    at `core/src/main/scala/eo/data/Forget.scala:127-149`** if no
    downstream consumer surfaces. ~25 LoC. S-size. Deprecate in
    0.1.0, remove in 0.2.0.
13. **Factor out the duplicated `namedTupleTypeOf` at
    `generics/src/main/scala/eo/generics/LensMacro.scala:221-239 /
    354-363`**. ~30 LoC. S-size. Post-0.1.0.
14. **Consider `replaceAll` rename for `JsonTraversal.place` /
    `transfer`** to match broadcast semantics. API-breaking, needs
    0.2.0.
15. **`cats-eo-laws` → `cats-eo-core` + `cats-eo-optics` split** to
    let law-equation-only users skip the optics package. Structural.
    0.2.0.
16. **`ChainAccessorLaws` → move to a per-spec test or delete** if
    no non-degenerate witness appears. Post-0.1.0.

---

## Nits (ignorable)

Low-signal items that add no blocker to 0.1.0:

- `core/src/main/scala/eo/optics/Traversal.scala:49` and
  `core/src/main/scala/eo/optics/Fold.scala:32` — both carry
  `@scala.annotation.unused ev: Traverse[T] / Foldable[F]`
  documented as "kept as API documentation". Fine; the annotation
  acknowledges it.
- Naming: `MendTearPrism.mend` vs `PickMendPrism.mend` — same
  method name, different classes, different return types for the
  sibling methods (`tear` vs `pick`). Reader has to look up which
  class is in hand.
- `core/src/main/scala/eo/data/Affine.scala:113` — `hashCode` uses
  `snd.hashCode * 31 + (if b == null then 0 else b.hashCode)`.
  The `null` guard is defensive; Scala generic `A` can be
  `AnyRef | Null` under `-Yexplicit-nulls` but we're not using
  that flag. Drop the guard? S-size, 1 LoC. Probably not worth it.
- `core/src/main/scala/eo/data/PSVec.scala:59-78` — `equals` /
  `hashCode` / `toString` overrides at the sealed-trait level.
  Sound, but at ~20 LoC they represent most of the file's
  statement count. Standard value-class boilerplate.
- `circe/src/main/scala/eo/circe/JsonFailure.scala:49-54` — the
  `message` method is an enum-shaped pattern match over six cases
  (at the ~6-arm threshold in the complex-passage rules). Each
  arm is a one-liner; fine.
- `circe/src/main/scala/eo/circe/JsonPrism.scala:441` — the
  `rootStep: PathStep = PathStep.Field("")` sentinel is a magic
  empty-string field name. Documented at line 439; the usage is
  consistent.
- `core/src/main/scala/eo/data/Grate.scala:155` — the Scaladoc
  at `Grate.apply` cites "plan 004 R4 / Risk 3" in prose. If the
  release tooling extracts Scaladoc into Maven Central, those
  plan references will look odd without a backlink. Not a
  blocker; could rewrite as prose.

---

## Endnote — Codescene comparison shopping list

For the next 0.1.x cycle, if Codescene MCP becomes available, ask
specifically for:

1. Change-coupling between `core/data/*` files — does editing
   `PowerSeries.scala` force an edit to `PSVec.scala` every time?
2. Complexity-over-time for `optics/Optic.scala` — is the extension
   companion growing linearly with new extension methods?
3. Hotspot ranking across core / circe / generics combined.
4. Per-author concentration (knowledge-risk signal).
5. Cohesion metric on the four circe classes + the Optic.scala
   companion.

The simplicity-reviewer output here covers most of the
"what is the fix" half but can't rank by "what is it worth" —
that's where Codescene's change-coupling × churn multiplier
would carry the argument.
