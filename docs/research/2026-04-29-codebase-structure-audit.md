# Codebase structure audit (2026-04-29)

Branch: `worktree-agent-a65d9b8cbd48097f3` from `1661006`. Inputs: full `core/`,
`laws/`, `circe/`, `avro/`, `generics/` source trees post-MultiFocus
unification, post-test consolidation.

This is research output. Each finding lists the recommended change, what
gets better, and a counter-argument from a future maintainer's
perspective. The user reviews and lands selectively.

## Scope of the current layout

```
dev.constructive.eo                         (3 typeclass files + Composer + Morph + Accessors)
  .data        (carriers + carrier-only typeclass instances)
  .optics      (Optic + 9 optic-family files)
dev.constructive.eo.laws                    (laws + discipline RuleSets)
  .data        (data-carrier laws — Affine, SetterF)
  .discipline  (RuleSets per family)
  .eo          (laws specific to EO's encoding — Compose, FoldAndTraverse, ModifyA, Morph, …)
  .typeclass   (laws for the project's typeclasses)
dev.constructive.eo.circe                   (Json carrier optics — flat package)
dev.constructive.eo.avro                    (Avro carrier optics — flat package)
dev.constructive.eo.generics                (lens/prism macros — flat package)
```

Repository totals: **10,779 LoC** of main Scala across the five modules,
**4,237** of which are comment lines (~39 %). 75 source files.

---

## Findings

### F1 — `data` is a mixed-purpose drawer

**Observation.** `core/src/main/scala/dev/constructive/eo/data/` currently
holds *three* unrelated concerns under one package:

1. Carrier types (`Forgetful`, `Forget`, `Affine`, `MultiFocus`, `SetterF`,
   `PSVec`).
2. Capability typeclasses for those carriers (`Accessor`,
   `ReverseAccessor`).
3. Internal builder utilities (`IntArrBuilder`, `ObjArrBuilder`).

Capability typeclasses in `Accessors.scala` are public; the array builders
are `private[eo]` and only one carrier uses them (PSVec via MultiFocus).
Meanwhile, the *Forgetful* family of typeclasses (`ForgetfulFunctor`,
`ForgetfulApplicative`, `ForgetfulFold`, `ForgetfulTraverse`) lives in
`dev.constructive.eo.*` directly — sibling to `data`, not inside it.

**Recommendation.** Three files:

- Move `IntArrBuilder` / `ObjArrBuilder` into a new
  `dev.constructive.eo.data.internal` sub-package (or simply keep them
  next to `PSVec` and label `private[data]` instead of `private[eo]` —
  cleaner ownership).
- Move `Accessors.scala` (`Accessor` + `ReverseAccessor`) up to
  `dev.constructive.eo` next to the other `Forgetful*` typeclasses. Their
  package position should match the other capability typeclasses' package
  position.
- Adopt the convention that `eo.data.*` is *only* carrier types + their
  carrier-specific typeclass instances (which already live here today).

**Justification.** Today a user importing `eo.data.*` picks up
`Accessor` (a typeclass) and `IntArrBuilder` (an internal helper) on the
same import as `Affine` (a public carrier). The package ceiling reads as
"data" but it isn't — `Accessor` is the same kind of object as
`ForgetfulFunctor`, which lives elsewhere. The split fixes a directly
observable inconsistency.

**Counter-argument.** `Accessor[F]` is *only* meaningful for two-parameter
carriers, and every one of those lives under `eo.data`. Moving `Accessor`
to `eo` puts it next to `Composer` and `Morph` — fine — but the typeclass
itself has no semantic content outside the carrier ecosystem. A
maintainer might reasonably say "leave it where the only conceivable
implementations live."

---

### F2 — `MultiFocus.scala` is a ~1000-line catch-all

**Observation.** `data/MultiFocus.scala` is **978 LoC** in a single file.
It contains:

- The carrier type alias (`type MultiFocus[F]`).
- Three private internal traits (`MultiFocusSingleton`,
  `MultiFocusFromList`, `MultiFocusPSMaybeHit`) plus the
  `MultiFocusFromList` companion with five F-instances.
- The `MultiFocus` companion containing:
  - `Functor[PSVec]`, `Foldable[PSVec]`, `Traverse[PSVec]` cats-instance
    givens.
  - `mfFunctor`, `mfFold`, `mfTraverse` capability instances.
  - `mfAssoc`, `mfAssocFunction1`, `mfAssocPSVec` (three same-carrier
    composers, the third with a 100-line specialised body).
  - `AssocSndZ` data class.
  - The `.collectMap` / `.collectList` / `.at(i)` extension methods.
  - Six `Composer[…, MultiFocus[F]]` and three
    `Composer[…, MultiFocus[PSVec]]` bridges.
  - The `apply` / `representable` / `representableAt` / `tuple` /
    `fromLensF` / `fromPrismF` / `fromOptionalF` factories.

This file is the single biggest source of comment noise the cleanup
pass is targeting, *and* it sits at the heart of the encoding. Splitting
it would reduce the cognitive load of every read.

**Recommendation.** Three files:

- `data/MultiFocus.scala` — type alias, capability instances
  (`mfFunctor`, `mfFold`, `mfTraverse`), generic factories (`apply`,
  `fromLensF`, `fromPrismF`, `fromOptionalF`), the
  `MultiFocusSingleton` / `MultiFocusFromList` private traits *for the
  generic path only*. ~400 LoC.
- `data/MultiFocusPSVec.scala` — `Functor`/`Foldable`/`Traverse[PSVec]`
  cats instances, `mfAssocPSVec`, `MultiFocusPSMaybeHit`, `AssocSndZ`,
  `tuple2multifocusPSVec` / `either2multifocusPSVec` /
  `affine2multifocusPSVec`. ~350 LoC. Single hot-path specialisation
  surface.
- `data/MultiFocusFunction1.scala` — `mfAssocFunction1`,
  `representable`, `representableAt`, `tuple`,
  `forgetful2multifocusFunction1`, `.at(i)` extension. ~200 LoC. The
  Grate-absorbed surface.

The companion-object `given` instances would need to be re-exposed via
`export` clauses or `@inline given` aliases in `MultiFocus.scala` so
`import data.MultiFocus.given` still picks them up — a known-good
pattern.

**Justification.** The split mirrors the natural conceptual seams:
generic-`F`, PSVec-specialised, Function1-specialised. After the split,
`data/MultiFocus.scala` is the file you read to understand the carrier;
the other two are the optimisation surfaces. A bug in PSVec-fast-paths
no longer makes you scroll past the generic factories.

**Counter-argument.** The companion-object `given` discoverability
trick — putting all instances inside the `MultiFocus` companion so a
`MultiFocus[F]` summon finds them — is real. Distributing givens across
peer-files works only if every consumer remembers to import the right
sub-package. `export` clauses in the parent companion mitigate this but
add their own indirection.

---

### F3 — Lens / Prism / Iso / Optional ship multiple types per file under one optic family

**Observation.** Each of `optics/Lens.scala`, `optics/Prism.scala`,
`optics/Iso.scala`, `optics/Optional.scala` already ships exactly the
shape we want — companion-object factory + 1–3 concrete subclasses. The
fused-`andThen` overloads on those subclasses (4–6 per class) are the
single biggest source of file-size in the optics package. **No change
recommended** for these files.

**Observation 2.** `optics/Review.scala` ships *three* objects:
`Review`, `ReversedLens`, `ReversedPrism`. The latter two are
read-aloud aliases for `Review.fromIso` / `Review.fromPrism`. **No
change recommended** — the alias is two lines each, splitting would
add three files for three lines of code.

**Recommendation.** None for the optics package — the file-per-family
convention holds up well after consolidation.

---

### F4 — `Forget.scala` and `Forgetful.scala` carry confusingly-similar names

**Observation.** Two carriers, two files:

- `Forgetful[X, A] = A` — the identity carrier (Iso, Getter).
- `Forget[F][X, A] = F[A]` — the `F`-shape carrier (Fold, MultiFocus
  bridges).

The names are deliberately echoes of the Haskell `forall p. Profunctor p
=> p[A, B] -> Forget(A -> M)` lineage, but in cats-eo they encode two
*different* carriers. Every PR I looked at has a "do you mean Forget or
Forgetful" comment somewhere.

**Recommendation.** Rename `Forgetful` → `Identity` (the actual
mathematical identity carrier) and `Forget[F]` → `Forgetful[F]` (it's
the carrier that "forgets" outer-structural context except for `F[A]`).
Or rename `Forget[F]` to something more descriptive of its actual
shape, `FCarrier[F]` or `Wrap[F]`.

This is a breaking-API rename, scoped to the 0.1.0 cycle.

**Justification.** Names exist to disambiguate. `Forgetful` /
`Forget[F]` differ by an `ful` and an `[F]` — the latter being a
parameter, not a name component. Every typeclass that targets one is
ambiguously labelled (`given accessor: Accessor[Forgetful]` vs `given
forgetFFunctor: ForgetfulFunctor[Forget[F]]` — note the `F` doubling on
the right).

**Counter-argument.** `Forgetful` is already entrenched in the
`Forgetful*` typeclass family (`ForgetfulFunctor`, `ForgetfulFold`,
`ForgetfulApplicative`, `ForgetfulTraverse`) — *those* don't target
`Forgetful` the carrier specifically; they're "two-parameter
forgetful functor / fold / traverse". Renaming the carrier without
also renaming the typeclasses produces a worse confusion. Renaming
both is a sweeping cosmetic change — high churn, low semantic value.
A future maintainer might rightfully say "lock the names, fix the
docstrings, and document the distinction prominently in the package
object instead."

---

### F5 — `Composer` / `AssociativeFunctor` / `Morph` cluster sits at the package top level

**Observation.** Three intimately-related typeclasses
(`Composer[F, G]`, `AssociativeFunctor[F, Xo, Xi]`, `Morph[F, G]`)
live as flat siblings in `dev.constructive.eo`. They form one
conceptual unit — the *composition surface* that makes `.andThen`
work across carriers. A reader trying to understand cats-eo's
composition story has to navigate three top-level files to do so.

**Recommendation.** Two options, in increasing scope:

- **Soft.** Add a package-level scaladoc to `package object eo` that
  cross-links the three and describes the layered priority. Today
  there's *no* `eo` package object — only `eo.circe` / `eo.avro` /
  `eo.generics` have one. Adding one would let users land on a single
  import target with the composition narrative front-and-centre.
- **Hard.** Move the three into a `dev.constructive.eo.compose`
  sub-package. This makes the conceptual unit textually obvious.

**Justification (soft).** Discovery. Right now the encoding's most
load-bearing typeclass cluster has no single landing page; you either
read `Optic.scala` or you stumble across each file. A `package
object` doc with a 5-line "how `.andThen` resolves" diagram fixes
this cheaply.

**Justification (hard).** Group them by responsibility. The
sub-package buys all the same scaladoc-anchor benefits *and* makes
the cluster locatable in the directory tree.

**Counter-argument.** Both options renege on the existing flat-package
convention. The flat layout has the virtue that `import
dev.constructive.eo.*` brings the entire surface into scope at once;
a sub-package fragmentation forces users into more imports for less
benefit. Library APIs typically don't gain from being moved into
namespaces just because the implementer thinks they're related. The
soft option is unambiguously good; the hard option is a wash.

---

### F6 — `private[eo]` boundaries leak more than necessary

**Observation.** Several types are visible to all of `eo.*` when they
need only `eo.data` or `eo.optics`:

- `IntArrBuilder` / `ObjArrBuilder` — `private[eo]`. Only used inside
  `eo.data`. Could tighten to `private[data]`.
- `MultiFocusSingleton` / `MultiFocusFromList` /
  `MultiFocusPSMaybeHit` — `private[eo]`. Only used inside
  `eo.data.MultiFocus`. Could tighten to `private[data]`.
- `AssocSndZ` (inside `MultiFocus.scala`) — `private[eo]`. Only
  reachable from `mfAssocPSVec`. Could be a private nested class.
- `Forget.assocFor` — `private[data]`. Correct as-is — referenced
  cross-file by `LowPriorityForgetInstances`.
- `optics.GetReplaceLens` — public. Used externally for the
  fused-`andThen` overloads, which is the explicit hot-path
  contract. Correct.

**Recommendation.** Tighten the four `MultiFocus*` private traits and
the two array builders from `private[eo]` to `private[data]`.
`AssocSndZ` becomes `private[MultiFocus]` (a nested-class private
inside the companion).

**Justification.** Visibility scoping is documentation: a tighter
scope says "you don't need to think about this from outside the
package". The wider `private[eo]` scope today suggests these are
project-wide internals; they aren't.

**Counter-argument.** The `private[data]` scope is actually slightly
*riskier* if someone later wants to test the internals from
`dev.constructive.eo.tests` (no current test reaches in, but the
laws module already has `eo.laws.data.*` for `Affine` and `SetterF`
laws). Keeping `private[eo]` leaves the door open for future law /
behaviour suites that probe these internals directly. If you don't
foresee needing that, tighten; if you might, keep.

---

### F7 — `Optic.scala` mixes nine extension blocks under one trait companion

**Observation.** `core/src/main/scala/dev/constructive/eo/optics/Optic.scala`
is **339 LoC**. The companion object hosts:

- Two `Profunctor` givens.
- The `id` constructor.
- A `morph` private extension.
- Nine carrier-capability-gated extension blocks
  (`get`, `reverseGet`, `reverse`, `modify`/`replace`, `place`/`transfer`,
  `transform`, `put`, `modifyF`, `modifyA`/`all`, `foldMap`, `getOption`).

Each block is small (3–10 lines), but they share a common indentation
hierarchy: every block is `extension [S, T, A, B, F[_, _]](o: Optic[…])(using …)
…`. That's nine repetitions of an 80-character preamble.

**Recommendation.** Keep the file intact. The repetition is the price
of having capability-gating per typeclass — splitting into
sub-traits would require extension-method visibility games that hurt
discoverability more than they help.

But: add a `// @group` ScalaDoc tag to each block (already done for
some) and rely on `docs/laikaSite` to render them as collapsible
sections. The structural seam is already there — it just needs to be
named.

**Counter-argument.** None significant. The "split into sub-traits"
alternative was considered and rejected pre-0.1.0 for the
discoverability reason; the file is what it is.

---

### F8 — Scala 3 idiom opportunities

The dossier called out specific power-features. A scan for unused
opportunities:

#### F8a — `enum Affine` in place of the `sealed trait Affine` + `Miss` / `Hit` final classes

`data/Affine.scala` defines:

```scala
sealed trait Affine[A, B]:
  def fold[C](onMiss: Fst[A] => C, onHit: (Snd[A], B) => C): C = …

object Affine:
  final class Miss[A, B](val fst: Fst[A]) extends Affine[A, B] …
  final class Hit[A, B](val snd: Snd[A], val b: B) extends Affine[A, B] …
```

Scala 3 idiomatic version:

```scala
enum Affine[A, B]:
  case Miss(fst: Fst[A])
  case Hit(snd: Snd[A], b: B)
```

**Recommendation.** Defer — perf-load-bearing. The `Miss[A, B]` /
`Hit[A, B]` constructors carry a `widenB` `inline def` that exploits
the phantom-`B` shape (every `Miss` stores only `Fst[A]`); rewriting
as an enum would force a per-case `apply` that doesn't trivially
support an inline `widenB` cast. The custom `equals` / `hashCode` /
`toString` overrides also have to be re-stated on the enum cases.
Until benchmarks demonstrate parity, leave as-is.

**Counter-argument.** The current shape is borderline-cargo-cult; an
enum + carefully-shaped `widenB` extension method on the companion
would give equivalent perf. Worth a measured spike post-0.1.0.

#### F8b — `opaque type` for `PSVec`

**Observation.** `PSVec` is a `sealed trait` with three case
implementations (`Empty`, `Single`, `Slice`). The CLAUDE.md
explicitly notes "cats-eo doesn't use `opaque type` today — but they
could replace the `PSVec`-style hand-rolled wrapper if perf were less
load-bearing."

`Empty` / `Single` exist *because* perf is load-bearing — they elide
the backing-array allocation that the array-backed shape pays. So
`opaque type PSVec[B] = Array[AnyRef]` would *lose* the
zero-element / single-element specialisation. **No change** —
the current shape correctly trades one virtual-method dispatch for
three eliminated allocations. Document this tradeoff in the file's
header.

#### F8c — `transparent inline def` on `Optic.modify` / `replace`

**Observation.** The capability-gated extensions on `Optic.scala` use
plain `inline def`, not `transparent inline def`. The trait
declaration of `modify` is `inline def modify(f: A => B): S => T`;
the per-class overrides in `GetReplaceLens` / `MendTearPrism` /
`PickMendPrism` / `BijectionIso` are also `inline def`. The
`transparent` modifier would let the call-site refine the return
type to a more specific function shape — but `S => T` is already
the most specific shape available. **No change.**

#### F8d — `derives` clauses on case classes used in tests

**Observation.** `tests/Samples.scala` and `JsonSpecFixtures` define
the standard `Person` / `Address` etc. with hand-rolled `equals` /
`hashCode` (case classes already do that, but the encoding is
boilerplate in many specs). A future round could leverage `derives`
for `Eq` / `Show` / property-test instances. This is purely cosmetic.

#### F8e — `NamedTuple` already used; no leverage gap

`generics/LensMacro.scala`'s multi-selector path returns
`NamedTuple[Names, Types]` — Scala 3.7+ feature, already in use.

#### F8f — Polymorphic function types `[T] => T => T` for natural transformations

**Observation.** None of the public surface uses
`[A] => F[A] => G[A]` polymorphic-function syntax. The closest case
is `ForgetfulTraverse.traverse`, which *could* in principle take a
poly-fn argument:

```scala
def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]
```

is already a curried, type-erased path; lifting it to a poly-fn would
require a per-instance refactor with no externally observable gain.
**No change.**

#### F8g — `enum Morph.Direction` for the `same / leftToRight / rightToLeft / bothViaAffine` priorities

**Observation.** `Morph.scala`'s four `given` instances form a
priority tower. The pattern is "value-level given resolution",
which Scala 3 supports first-class. *Could* be expressed as an enum
priority annotation if cats-eo grew a generic priority-encoding
DSL — but it doesn't, and four givens is too few to justify one.
**No change.**

---

### F9 — `circe.PathStep` and `avro.PathStep` are duplicated for a documented reason

**Observation.** `circe/PathStep.scala` and `avro/PathStep.scala`
are near-identical. The `package object avro` doc explicitly calls
out:

> Gap-1 (per the eo-avro plan). PathStep is duplicated, not shared
> with eo-circe — the UnionBranch case is Avro-only and forcing
> it into eo-circe would pollute that module. The duplication is
> intentional; see PathStep's class doc.

**Recommendation.** Document this once at the top of *both* files
with a `@see` cross-reference. The decision is sound; the
discoverability is not.

**Counter-argument.** `@see` cross-references between integration
modules require the reader to know there's something to look for.
Today's per-package docs already explain the decision. A formal
cross-reference is gold-plating.

---

### F10 — `tests/` test-fixture types are scattered across spec files

**Observation.** Each major spec file (`OpticsBehaviorSpec.scala`,
`OpticsLawsSpec.scala`, `MultiFocusSpec.scala`, …) defines its own
top-level `private case class Person(name, age)` / `private case
class Phone(...)` ADTs at the file head. Slight variations between
files (some have `phones: List[Phone]`, others don't).

`tests/Samples.scala` exists but only holds Arbitrary instances and
generators — not ADTs.

**Recommendation.** Promote the most-used ADTs (`Person`, `Phone`,
`Address`, `Tree`) into `tests/Samples.scala` (or a new
`tests/Fixtures.scala` if collision is a concern). Adopt a
"single sample-shape per ADT name" rule.

**Justification.** Slight variations cost reader time. Today
`Person(name, age)` in one spec, `Person(name, phones: List[Phone])`
in another, and `Person(name, age, phones)` in a third all coexist —
the difference is meaningful in some specs but invisible in their
file headers.

**Counter-argument.** Spec-local ADTs are idiomatic for ScalaCheck
test suites, which often want to stand alone. Pulling them out
forces coordination on shape changes ("if I add a field to Person
to test something, I might break someone else's spec"). The
status quo is conservative; centralising is more aggressive.

---

## Summary of recommended actions, ranked

| Rank | Action | Effort | Value |
|---|---|---|---|
| 1 | F2 — split `MultiFocus.scala` into `MultiFocus`/`MultiFocusPSVec`/`MultiFocusFunction1` | M | High |
| 2 | F1 — relocate `Accessors.scala` up + clarify `data/internal` for builders | S | Medium |
| 3 | F5 (soft) — add `package object eo` doc with the `Composer`/`AssociativeFunctor`/`Morph` story | XS | Medium |
| 4 | F6 — tighten `private[eo]` to `private[data]` where it applies | S | Low (cosmetic) |
| 5 | F4 — rename `Forgetful` → `Identity`, `Forget[F]` → `Forgetful[F]` (or document loudly) | L (breaking) | Medium |
| 6 | F8a — measure `enum Affine` post-0.1.0 | M | Speculative |

F7, F8b, F8c, F8d, F8e, F8f, F8g, F9 are no-changes with rationale documented above.
F10 is a test-suite refactor whose value depends on how often shape changes happen.

---

## What this audit doesn't cover

- The `benchmarks/` subproject — explicitly out of scope per CLAUDE.md
  (not part of the root aggregate).
- The `site/` Laika setup.
- Cross-module dependency graph clean-ups (e.g. should `laws` depend
  on `core` directly or via an interface module).
- Renaming the `cats-eo-laws` artifact's `eo.laws.eo` package, which is
  visibly redundant. Worth a separate proposal — it's a public-API
  concern.
