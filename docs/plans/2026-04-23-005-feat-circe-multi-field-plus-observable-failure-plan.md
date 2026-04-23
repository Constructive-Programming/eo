---
title: "feat: circe — multi-field focus (.fields) + observable-by-default failure path (Ior API)"
type: feat
status: active
date: 2026-04-23
---

# feat: circe — multi-field focus (`.fields`) + observable-by-default failure path (Ior API)

## Overview

Extend the `cats-eo-circe` module (`circe/`) with **two complementary
capabilities that ship together**:

1. **Multi-field focus on `JsonPrism` and `JsonTraversal`.** A new
   varargs entry `.fields(_.a, _.b, …)` focuses a Scala 3 `NamedTuple`
   assembled from the selected case-class fields in **selector order**.
   Single-field `.field(_.x)` keeps its current surface untouched. On
   `JsonTraversal` the same `.fields` produces a NamedTuple focus per
   array element.
2. **Observable-by-default failures.** The existing `modify` /
   `transform` / `place` / `transfer` / `getOption` surface on
   `JsonPrism` and `JsonTraversal` is **renamed to `*Unsafe`** and the
   default names now return `Ior[Chain[JsonFailure], Json]` (or
   `Ior[Chain[JsonFailure], A]` for the new `get`). The user's design
   principle: *"failure is never strict, it is documented by default
   and can be wholly ignored if the end user so chooses."* Callers who
   want the old silent `Json => Json` / `Option[A]` behaviour reach for
   the explicit `*Unsafe` variant.

The two stories pair naturally. Multi-field reads are the scenario in
which silent "on any miss, return the input unchanged" most obviously
asks *which* selected field missed. And a single multi-field modify
can produce multiple per-field failures on the same Prism, so the
accumulation-shaped `Chain[JsonFailure]` carrier appears on both the
Prism and the Traversal surface from day one. Designing the
`JsonFailure` ADT once with full awareness of the multi-field read
story keeps the two stories aligned.

This is a **breaking change** to the existing `eo.circe` API. At
`0.1.0-SNAPSHOT` that cost is acceptable — see Risks & Dependencies
and the Migration Notes section under the Documentation Plan.

Scala-level nested paths, per-field independent update semantics, and
auto-upgrades to a new optic family (e.g. `JsonIso`) remain **out of
scope** — see D1, D2, and Scope Boundaries.

## Problem Frame

`cats-eo-circe` today offers exactly one leaf focus at a time. Editing
two fields of the same JSON object atomically requires chaining two
`.modify` calls or decoding the parent and working at the Scala level
— precisely the cost `cats-eo-circe` was designed to avoid. Downstream
users have asked for the ergonomic parity with the generics-side
multi-field lens that landed in plan 003
(`docs/plans/2026-04-23-003-feat-generics-multi-field-lens-plan.md`).

And the forgiving `Json => Json` signature is **misleading on
failure**. `.modify(f)(stumpJson)` returning the input unchanged is
the right hot path for a *knowing* caller — diagnostics cost every
caller an `Ior` wrap — but the current default makes it invisible
*that a miss occurred at all*. The raw `to: Json => Either[(DecodingFailure,
HCursor), A]` that the generic Optic extensions expose is (a) not
cursor-structured in a way the multi-field story can use and (b) not
discoverable as a diagnostic tool.

The honest principle: **users should see failures unless they
explicitly opt out.** That inverts the previous "forgive silently,
offer an opt-in `*E` alternative" approach and fixes the discoverability
bug at the API level rather than at documentation level. The
`*Unsafe` suffix names the silence and forces the choice to be a
deliberate one.

**The two problems share surface.** Any multi-field modify that can
return "none of the fields changed because one field failed" urgently
needs a parallel way to say "*this* field, at *that* PathStep, failed
for *this* reason". And the traversal case can have *multiple* such
failures from a single call — one per broken element, or one per
broken field inside a single element. `Ior` is the honest carrier:
`Right` = clean success, `Both` = result plus warnings, `Left` = no
result producible at all. Designing the `JsonFailure` ADT together
with the multi-field code keeps all four flows aligned.

## Requirements Trace

- **R1. Varargs `.fields` on `JsonPrism`.** For `codecPrism[Person]`,
  `codecPrism[Person].fields(_.name, _.age)` compiles and produces a
  `JsonPrism`-shaped optic whose focus is
  `NamedTuple[("name", "age"), (String, Int)]`. At least two selectors
  are required.
- **R2. Varargs `.fields` on `JsonTraversal`.** The same macro entry
  works on a traversal — `codecPrism[Basket].items.each.fields(_.name,
  _.price)` focuses a NamedTuple per array element.
- **R3. Single-field `.field` unchanged.** `.field(_.x)`'s current
  signature, semantics, and hot-path loop are untouched. The
  `Array[PathStep]` storage for the single-field case is preserved
  byte-for-byte.
- **R4. Default API is Ior-bearing.** The default `modify` /
  `transform` / `place` / `transfer` methods on `JsonPrism`,
  `JsonFieldsPrism`, `JsonTraversal`, and `JsonFieldsTraversal`
  return `Ior[Chain[JsonFailure], Json]`. The default `get` method
  returns `Ior[Chain[JsonFailure], A]`. The semantics follow D4: **apply
  what's possible**, bounded by the focus-type's ability to represent a
  partial result. See D4 for the per-surface breakdown (single-field
  prism, multi-field prism, traversal, multi-field traversal).
- **R5. `*Unsafe` escape hatch.** Every default method has a
  sibling `*Unsafe` method that returns the pre-plan silent shape:
  `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` / `transferUnsafe`
  return `Json` (or the curried `Json => C => Json` for
  `transferUnsafe`); `getOptionUnsafe` returns `Option[A]`. The
  `*Unsafe` hot path is byte-identical to today's implementation —
  no Ior construction, no Chain.Builder allocation.
- **R6. `JsonFailure` ADT.** A structured failure type carrying the
  `PathStep` at which the walk aborted plus the reason. Lives in the
  `eo.circe` package. Appears inside the `Chain[JsonFailure]` carried
  by every Ior result.
- **R7. Symmetric Prism + Traversal accumulation.** Both
  `JsonPrism.*` and `JsonTraversal.*` default methods carry
  `Chain[JsonFailure]`. A single multi-field prism call can produce
  multiple failures (one per skipped field); a single traversal call
  can produce many more (one per skipped element, each potentially
  emitting multiple per-field failures). See D3 for the `Chain`
  vs. `NonEmptyChain` choice.
- **R8. Failure-mode coverage.** Every documented failure of the
  pre-plan silent methods has a corresponding `JsonFailure` case:
  missing path step, non-object where a field was expected, non-array
  where an index was expected, index out of range, decode failure at
  the leaf. See the ADT sketch below.
- **R9. `.getOption` on multi-field.** Multi-field `JsonPrism` and
  `JsonTraversal` expose `get` (Ior-valued, `Chain[JsonFailure]`
  accumulating) plus `getOptionUnsafe`. Traversal `getAll` keeps its
  current contract under `getAllUnsafe`; the default `getAll` returns
  `Ior[Chain[JsonFailure], Vector[A]]` with per-element failures
  accumulated.
- **R10. Macro-time diagnostics.** Empty varargs, duplicate selectors,
  unknown field names, non-case-class `A`, non-single-field selectors
  (`_.a.b`) all abort macro expansion with the clear message
  catalogue from D9.
- **R11. Tests.** Law-based coverage (Prism laws on the multi-field
  `JsonPrism`), behavioural specs for every failure mode under both
  the default and `*Unsafe` surface, and a round-trip spec for the
  `JsonTraversal` accumulation case.
- **R12. Docs.** `site/docs/circe.md` gains a "multi-field focus"
  section, a "reading diagnostics from the default Ior surface" section,
  and a migration-notes subsection for the rename. `site/docs/cookbook.md`
  gains a "diagnose JSON path failures" entry.
- **R13. No regression on the `*Unsafe` hot path.** The existing
  `JsonPrismBench` / `JsonTraversalBench` numbers — retargeted at the
  `*Unsafe` methods post-rename — must match main within JMH noise.
  The default Ior-bearing methods are reported as a new surface with
  no regression gate of their own. See Risks R2 and OQ6.

## Scope Boundaries

In scope:

- `circe/src/main/scala/eo/circe/JsonPrism.scala` — rename existing
  `modify` / `transform` / `place` / `transfer` / `getOption` to the
  `*Unsafe` suffix, add the new Ior-returning defaults under the old
  names, add the macro hook for `.fields`.
- `circe/src/main/scala/eo/circe/JsonTraversal.scala` — same rename +
  add, plus the forgiving `placeUnsafe` / `transferUnsafe` methods
  that don't yet exist (Unit 2 introduces them symmetrically).
- `circe/src/main/scala/eo/circe/JsonPrismMacro.scala` — new
  `fieldsImpl[A](…)` / `fieldsTraversalImpl[A](…)` macros extending
  the existing `extractFieldName` pattern.
- `circe/src/main/scala/eo/circe/JsonFailure.scala` — new file, the
  failure ADT.
- `circe/src/main/scala/eo/circe/JsonFieldsPrism.scala` — new file, the
  multi-field prism sibling class (see D2).
- `circe/src/main/scala/eo/circe/JsonFieldsTraversal.scala` — new
  file, the multi-field traversal sibling class.
- `circe/src/main/scala/eo/circe/PathStep.scala` — visibility
  promoted from `private[circe]` to public (see D6); `Field` / `Index`
  cases already sufficient for every failure mode.
- `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` — reflect the
  rename across every existing spec; new test sections: multi-field
  modify / failure / default Ior surface.
- `circe/src/test/scala/eo/circe/JsonTraversalSpec.scala` — same.
- `circe/src/test/scala/eo/circe/JsonFailureSpec.scala` — the failure
  ADT's observable-identity tests.
- `benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala` and
  `JsonPrismWideBench.scala` and `JsonTraversalBench.scala` — methods
  retargeted at `*Unsafe` names so the pre-existing perf reference
  survives intact.
- `site/docs/circe.md`, `site/docs/cookbook.md` — new doc sections
  plus migration notes.

Out of scope:

- **`JsonIso` auto-upgrade on full cover.** The generics-side multi-field
  lens auto-upgrades to `BijectionIso` when selectors span every case
  field. The circe side **does not** — see D1 for rationale.
  `codecPrism[Person].fields(_.name, _.age, _.address)` on a
  three-field `Person` returns `JsonFieldsPrism[NamedTuple[…]]`, not a
  `JsonIso`.
- **Nested-path selectors.** `codecPrism[Person].fields(_.address.street,
  _.name)` is out of scope; selectors must be single-field. Users
  chain via `.address.fields(_.street, _.zip)` for nested composition.
  Revisit post-first-release.
- **`Selectable`-driven multi-field** (`codecPrism[Person].(name, age)`).
  Scala 3's `Selectable` only supports single-name access; the sugar
  doesn't reach this surface. Out permanently unless the language
  changes.
- **Writer-accumulator / side-channel observer** failure observation
  alternatives. Rejected in D7 in favour of the default-Ior approach.
- **New law classes.** Multi-field `JsonFieldsPrism` is still a Prism
  (focus shape changed, laws unchanged). The existing `PrismLaws`
  instance suffices. No new law file.
- **cross-module `eo.circe` → `eo.laws` dependency at Compile scope.**
  The circe module does not depend on `cats-eo-laws` today; this plan
  does not change that. The Prism laws are instantiated inside the
  circe test tree using a `laws % Test` LocalProject dep — see Unit 6.

Breaking change surface (called out explicitly):

- Every caller of `JsonPrism.modify` / `.transform` / `.place` /
  `.transfer` / `.getOption` gets a compile error under the new API
  because the return type changed. The fix is either rename the call
  to the `*Unsafe` variant (preserving today's semantics exactly) or
  adapt to the new `Ior` shape. This is the primary migration cost
  and the primary justification for the pre-1.0 window.

## Context & Research

### Relevant Code and Patterns

- **`circe/src/main/scala/eo/circe/JsonPrism.scala`** — the current
  `JsonPrism[A]` class. Notable shape:
  - Storage: `private[circe] val path: Array[PathStep]`, plus
    `encoder: Encoder[A]` / `decoder: Decoder[A]`.
  - Extends `Optic[Json, Json, A, A, Either]` with abstract `to` /
    `from` returning `Either[(DecodingFailure, HCursor), A]`.
  - Imperative hot-path loops: `modifyImpl`, `transformImpl`,
    `placeImpl` share an `Array[AnyRef]` of per-step parents, walk
    down via `asObject` / `asArray`, then rebuild via
    `JsonObject.add` / `Vector.updated` on the way up.
  - `widenPath[B]` / `widenPathIndex[B]` / `toTraversal[B]` extend
    the path by one step.
- **`circe/src/main/scala/eo/circe/JsonPrismMacro.scala`** — the
  `fieldImpl` / `selectFieldImpl` / `atImpl` / `eachImpl` /
  `fieldTraversalImpl` / `selectFieldTraversalImpl` / private
  `extractFieldName` / private `iterableElementType` helpers. The
  multi-field macro extends this object.
- **`circe/src/main/scala/eo/circe/JsonTraversal.scala`** — same
  imperative loop pattern with `prefix: Array[PathStep]` + `suffix:
  Array[PathStep]`. `modifyImpl` walks the prefix once, maps
  `arr.map(elemUpdate)` across the array, unwinds the prefix.
  `updateElementDecoded` / `updateElementRaw` are the per-element
  walks. Current forgiving methods: `modify`, `transform`, `getAll`.
  No `place` / `transfer` yet — Unit 2 adds their `*Unsafe`
  counterparts together with the default `place` / `transfer`.
- **`circe/src/main/scala/eo/circe/PathStep.scala`** — `private[circe]
  enum PathStep { case Field(name: String); case Index(i: Int) }`.
  Package-private today; the `JsonFailure` ADT requires making it
  public (see D6).
- **`core/src/main/scala/eo/optics/Optic.scala`** — the generic
  `.getOption` extension on `Optic[_, _, _, _, Affine]` (recent).
  That extension lives on the Affine-carrier generic surface and is
  **untouched** by this plan: it is a different code path from the
  renamed-to-`getOptionUnsafe` method on `JsonPrism`.
- **`docs/plans/2026-04-23-003-feat-generics-multi-field-lens-plan.md`**
  — closest structural analogue. Inherits D5 (selector order =
  NamedTuple order; declaration order = complement order) and the
  error-message prefix convention from D9 of plan 003 (we swap
  `lens[…]:` for `JsonPrism.fields[…]:`). Full-cover auto-Iso
  **rejected** here for the circe-side asymmetry reason in D1.
- **`docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md`**
  — format / length reference.

### External References

- **Scala 3 NamedTuple reference** —
  <https://docs.scala-lang.org/scala3/reference/experimental/named-tuples.html>.
  `type NamedTuple[N <: Tuple, V <: Tuple] = V` as an opaque alias;
  singleton-String name tuple.
- **circe Decoder / Encoder** —
  <https://circe.github.io/circe/codecs/auto-derivation.html>.
  circe-core `0.14.10` does **not** ship a built-in NamedTuple
  `Codec.AsObject`. Derivation is delegated to libraries.
- **kindlings-circe-derivation** `0.1.0` —
  <https://github.com/MateuszKubuszok/hearth>. The companion
  `KindlingsCodecAsObject` object ships
  `EncoderHandleAsNamedTupleRuleImpl` /
  `DecoderHandleAsNamedTupleRuleImpl` rules, so
  `KindlingsCodecAsObject.derive[NT]` does produce a
  `Codec.AsObject[NT]` for a NamedTuple `NT`. Confirmed via `cellar
  search-external com.kubuszok:kindlings-circe-derivation_3:0.1.0
  NamedTuple`. Still requires a spike — see OQ1.
- **cats `Ior`** —
  <https://typelevel.org/cats/api/cats/data/Ior.html>.
  Three-case ADT: `Ior.Left(a)`, `Ior.Right(b)`, `Ior.Both(a, b)`.
  `Semigroup[A]`-powered accumulation via `Ior.combine`, `putRight`,
  `putLeft`, `flatMap`. `cats-core` is already on the module's
  classpath.
- **cats `Chain`** —
  <https://typelevel.org/cats/api/cats/data/Chain.html>. Empty-allowed
  constant-time-append list. `Chain.Builder` used for hot-path
  accumulation without intermediate allocation per element.

### Institutional Learnings

- The `JsonPrism` hot-path performance work (Jan 2026, commits `1e2e86b`
  and `8483e9a`) specifically avoids `HCursor` transitions because
  `HCursor.set.top` re-materialises the underlying map
  representation. The default Ior-bearing methods must preserve that
  property — their failure cases produce `Ior.Left(chain)` or
  `Ior.Both(chain, json)` on the *same walk that would have returned
  input unchanged*, without constructing an HCursor. The `*Unsafe`
  methods retain the exact pre-plan loops byte-for-byte.
- Plan 003's D7 "single varargs entry point" approach doesn't
  trivially port here: `transparent inline` across the
  `JsonPrism`/`JsonFieldsPrism` split is doable, but cleaner is to
  just publish `.field` and `.fields` as two distinct surface entries
  (D10). Single-field users pay nothing; multi-field users reach for
  the new name.
- The `Chain.Builder` allocation cost is a pure happy-path concern
  for the Ior-bearing default: we materialise a builder only when a
  failure actually surfaces. A single `Chain.empty` reference is
  reused for the clean-success path so `Ior.Right(json)` is a single
  allocation at the end of a successful walk.

## Key Technical Decisions

### D1. Full cover does NOT auto-upgrade to a `JsonIso` (Q1 = skip Iso)

`codecPrism[Person].fields(_.name, _.age, _.address)` on a full
three-field `Person` returns `JsonFieldsPrism[NamedTuple[…]]`, **not**
a `JsonIso`. Rationale: a circe-side Iso would suggest a strict
bijection `Json ↔ Person`, and JSON decode always admits failure
(the input may not even be a JSON object). There is no safety
guarantee to uphold. Introducing `JsonIso` would misleadingly advertise
totality we cannot provide.

Deliberate asymmetry with plan 003's Scala-level generics: the
generics side *does* auto-upgrade on full cover because Scala-level
case-class construction is total (every NamedTuple assembled from a
case class round-trips back to an instance). The circe side's "decode
may fail" story rules out the analogous guarantee.

**No special-case at the macro level.** The multi-field macro emits
`JsonFieldsPrism` regardless of whether the selector set covers every
case field or not. Full cover is a latent no-op from the macro's
perspective — the optic is structurally the same. Flagged here so
nobody re-adds a coverage check later "as a reader affordance".

### D2. `.fields(_.a, _.b)` is additive — new varargs entry, not a replacement (Q3)

`.field(_.x)` keeps its current single-selector signature exactly:

```text
extension [A](o: JsonPrism[A])
  transparent inline def field[B](inline selector: A => B)
      (using Encoder[B], Decoder[B]): JsonPrism[B]
```

`.fields(_.a, _.b, …)` is a **new varargs entry** at the package
extension level:

```text
extension [A](o: JsonPrism[A])
  transparent inline def fields(inline selectors: (A => Any)*)
      (using Encoder[NT], Decoder[NT]): JsonFieldsPrism[NT]
```

where `NT` is the macro-synthesised NamedTuple. Minimum arity is
**two** — one-selector `.fields(_.name)` would be strictly worse than
`.field(_.name)` (unnecessary NamedTuple wrap). Macro emits the empty
/ single error per D9.

Rationale: additive API has zero risk of regression, and the
two-name split (`.field` singular, `.fields` varargs) reads well at
call sites.

### D3. Default failure carrier is `Ior[Chain[JsonFailure], _]`

The two shape choices made here:

**`Ior`, not `Either`.** JSON editing can partially succeed. A
traversal updates 97 elements and fails on 3; a multi-field prism
modifies one field successfully while another raises a decode
failure — these are "here is your result AND here is what went wrong"
situations. `Either` forces fail-fast; `Ior` honestly models the
three cases:

- `Ior.Right(json)` — clean success. No failures observed.
- `Ior.Both(failures, json)` — partial success. The result `json`
  reflects every update that did succeed; the `failures` chain lists
  every skip.
- `Ior.Left(failures)` — total failure. No result producible. Examples:
  root input isn't a JSON object, path prefix can't resolve, the
  root-level decode in `get` fails outright.

The `Both` constructor is the load-bearing one: it's the shape that
was missing from the previous Either-based draft. In the prism case
it arises when the prism's write operation (re-encoding the focus or
overlaying fields) succeeds for some fields but not others. In the
traversal case it arises per-element: some elements succeed, some
fail, the overall Json reflects the successful subset with failures
accumulated.

**`Chain[JsonFailure]`, not `NonEmptyChain`.** `Ior.Both` and
`Ior.Left` already require the left side to be present by
construction — there is no "Both with empty failures" state
reachable via the constructors. So the wrapping empties-guarantee is
handled by `Ior` itself; the inner collection just needs fast append
and cheap "empty success" pass-through. `Chain` gives us
constant-time append and a free `Chain.empty` for the happy path.
`NonEmptyChain` would force every builder to emit via
`NonEmptyChain.fromChainUnsafe` at the boundary, which is friction
without additional safety. If a public method needs to advertise "you
will see at least one failure", the `Ior.Left` / `Ior.Both`
constructors already carry that.

Rejected alternative: `Validated[NonEmptyChain[JsonFailure], _]`.
Validated is two-case (Valid / Invalid) and loses the "partial
success" third case that Ior gives. The whole point is that JSON
edits can produce a usable-but-imperfect result; Validated's
fail-fast discipline doesn't match.

### D4. "Apply what's possible" — per-surface semantics

The user's rule is *"failure is never strict, it is documented by
default and can be wholly ignored if the end user so chooses."* The
corollary is: *the modify always applies every update that is
representable*. But "what's possible" is bounded by the focus type's
ability to express a partial result. Per surface:

**Single-field `JsonPrism`.** The focus is a single value `A`. If the
walk succeeds and decode succeeds, `modify(f)` re-encodes `f(a)` and
overlays it onto the path: `Ior.Right(newJson)`. If the path walk
fails (missing field, non-object parent, index out of range) or the
leaf decode fails, the result is `Ior.Both(chain-of-one, inputJson)`
— the rest of the JSON is returned unchanged and the one failure is
surfaced. **`Ior.Left` never appears on a single-field prism's
`modify`** because the input `json` is always producible as a
fallback. It *does* appear on `get`, where "apply what's possible"
gives nothing back when the read fails.

**Multi-field `JsonFieldsPrism`.** The focus is a NamedTuple. Here's
the subtle design point: **a NamedTuple cannot be partial**. You
can't hand `f: (name, age) => (name, age)` a `(name, age)` that is
missing one component. So if any selected field fails to read, the
user's `f` cannot be invoked at all — the modify step itself is
atomic. The Ior result captures this honestly:

- All selected fields read → `f` is invoked → write succeeds
  → `Ior.Right(newJson)`.
- Some subset of selected fields fail to read → `f` cannot be
  invoked → the input Json is returned unchanged and every failing
  field contributes one `JsonFailure` to the chain
  → `Ior.Both(chain, inputJson)`. Multiple failures are naturally
  possible here (one per broken field).
- The *parent* path doesn't resolve to an object at all → there's
  nothing to read from → `Ior.Left(chain-of-one)`.

The atomicity is driven by the type system's inability to represent a
partial NamedTuple, not by a stylistic choice. Calling this out
explicitly because readers will expect "apply what's possible" to
mean "only update the fields that decoded cleanly" — which would
require running `f` on a partial NamedTuple, which isn't representable
at all.

For users who want per-field-independent updates, plan ahead for a
second-generation feature: an API where the caller supplies
per-field endomorphisms (`nt.a => nt.a`, `nt.b => nt.b`) individually
and the system applies each one independently. Listed in Future
Considerations; out of v1.

**`JsonTraversal` (single-field).** The traversal iterates array
elements. Per-element isolation already exists: the current forgiving
`.modify` returns each broken element unchanged. Under R4 that becomes:

- Every element succeeds → `Ior.Right(newJson)`.
- Some elements fail (per-element reasons) → the Json contains the
  successful updates and unchanged broken elements, and every
  per-element failure contributes one `JsonFailure`
  → `Ior.Both(chain, newJson)`.
- The *prefix* walk fails (no array at the iterated position)
  → `Ior.Left(chain-of-one)` — nothing to iterate.

**`JsonFieldsTraversal` (multi-field on traversal).** Combines the
two. Per-element, the NamedTuple atomicity from `JsonFieldsPrism`
applies: if one of the selected fields of element 5 fails to read,
element 5 is left unchanged and every broken field of element 5
contributes one `JsonFailure`. Across elements, accumulation
continues as in single-field traversal. A single call can produce a
`Chain[JsonFailure]` of length many: one per broken field per
broken element.

This is the subtle design point readers will trip over. Document it
carefully in the "multi-field focus" section of `site/docs/circe.md`
with a small table: *what partial-result shape is representable?* Only
where it is representable does per-element forgiveness apply;
otherwise atomicity is forced by the focus type.

### D5. Naming — `*Unsafe` for the silent variants

The default surface gets the clean name; the silent variant gets the
suffix:

| Default (new) | `*Unsafe` (renamed) | Return type |
|---------------|---------------------|-------------|
| `modify(f)` | `modifyUnsafe(f)` | default: `Ior[Chain[JsonFailure], Json]`; unsafe: `Json` |
| `transform(f)` | `transformUnsafe(f)` | default: `Ior[Chain[JsonFailure], Json]`; unsafe: `Json` |
| `place(a)` | `placeUnsafe(a)` | default: `Ior[Chain[JsonFailure], Json]`; unsafe: `Json` |
| `transfer(f)` | `transferUnsafe(f)` | default: `Json => Ior[…]` curried on `C`; unsafe: `Json => C => Json` |
| `get` | `getOptionUnsafe` | default: `Ior[Chain[JsonFailure], A]`; unsafe: `Option[A]` |

Naming notes:

- `get` on the default surface replaces the old `getOption` name —
  "get the focus; if it didn't work, read the diagnostic". The `Option`
  return type moves into the `*Unsafe` variant along with the
  `Option` in its name: `getOptionUnsafe`. This keeps both names
  self-describing: default "get returns Ior", unsafe "get-as-Option
  is the forgiving escape hatch".
- The generic `getOption` extension on the Affine-carrier `Optic`
  (`core/src/main/scala/eo/optics/Optic.scala`) is **untouched**. It
  lives on a different surface — Lens-style and Prism-style optics
  that flow through the Affine carrier — and is semantically distinct
  from the `JsonPrism` direct read path. If symmetry with the
  Ior-bearing surface is desired on that extension too, a
  separate plan can address it; out of scope here.
- The abstract `to` / `from` methods from the `Optic` trait are the
  existential-optics contract. They stay exactly as they are —
  `to: Json => Either[(DecodingFailure, HCursor), A]`. The rename
  does not touch the Optic supertype.

### D6. `PathStep` becomes package-public; `JsonFailure` lives alongside

`PathStep` is currently `private[circe]`. The `JsonFailure` ADT
carries `PathStep` values through its public API, so `PathStep` must
be published. Change the visibility to package-default (public); add
a scaladoc header explaining that users will see it inside
`JsonFailure` and in `Optic` `toString` outputs.

`JsonFailure` lives in `circe/src/main/scala/eo/circe/JsonFailure.scala`
as a Scala 3 `enum` (matches the `PathStep` style):

```text
enum JsonFailure:
  case PathMissing(step: PathStep)
  case NotAnObject(step: PathStep)
  case NotAnArray(step: PathStep)
  case IndexOutOfRange(step: PathStep, size: Int)
  case DecodeFailed(step: PathStep, cause: DecodingFailure)

  // Human-readable diagnostic; keeps default toString for structural inspection.
  def message: String = …
```

**Rationale for `enum`** — consistent with `PathStep`, cheaper exhaustivity
story at pattern-match sites, ships a `derives` hook if we later want
`Show` / `Eq`. **Rejected:** a `sealed trait` hierarchy with
dedicated subclasses per case; marginal readability gain, no
mechanical benefit.

**Why `step` as `PathStep`, not a raw `String` or `Int`.** The walk
that emits the failure already has the `PathStep` value in hand
(it's what drove the asObject / asArray branch). Carrying it
unchanged preserves the full context at zero cost beyond one
reference.

**Why no `pathContext: Array[PathStep]` case-argument.** Debatable;
deferred — the current design gives you the *step* but not the
*prefix leading to it*, which for deeply nested failures is arguably
thin. Listed in Open Questions (OQ3). Leaving it out of v1 keeps the
ADT small and the `*Unsafe` variants' implementation trivial (they
drop the chain on the floor); a later refinement that adds
`pathContext: Array[PathStep]` on each case is non-breaking.

### D7. Observable-by-default = Alt A-reversed (default surface returns Ior)

Decision recorded here; **not** re-litigated.

> The default API is the diagnostic-bearing one. The `*Unsafe` suffix
> names the silence. Users opting out of diagnostics make that choice
> deliberately — the discoverability bug at the API level is fixed
> directly rather than through documentation hints. The hot-path
> shape of the `*Unsafe` variants is byte-identical to today's
> forgiving methods, so callers who have measured and know they don't
> want the Ior allocation pay nothing for the change. The default
> Ior-bearing methods share the underlying walk logic with the
> `*Unsafe` variants (D11's codegen structure) — they only differ in
> whether failures short-circuit to `return inputJson` or contribute
> to a `Chain.Builder`.

**Rejected Alt B (Writer-accumulator).** Threading a
`Writer[Chain[JsonFailure], Json]` through every call would shift the
ergonomic default from "returns new Json" to "returns new Json with
diagnostic log". Ior already captures the same three states (Right /
Both / Left) with a cleaner case analysis and less typeclass
machinery.

**Rejected Alt C (side-channel observer / callback).** Passing an
`onFailure: JsonFailure => Unit` callback is conceptually
ergonomic — callers opt in by supplying a callback — but it's deeply
unidiomatic Scala, bakes in a specific I/O surface (the callback
is effectful), and loses compositionality. Ior gives the same
diagnostic opt-in with referential transparency.

**Rejected Alt D (previous draft: new `*E` methods returning `Either`).**
The critical problem is discoverability: a new parallel surface
means users *must* already know the `*E` names exist to benefit. The
default-flip makes the surface visible without effort.

### D8. Multi-field storage is a new sibling class `JsonFieldsPrism[A]`

The existing `JsonPrism[A]` stays exactly as-is in terms of storage,
storing `Array[PathStep]` = path to a single leaf. The multi-field
variant lives in a new `JsonFieldsPrism[A]` class with different
storage:

```text
final class JsonFieldsPrism[A] private[circe] (
    private[circe] val parentPath: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either], Dynamic: …
```

where `A` is the NamedTuple focus type, `parentPath` walks from root
to the parent JsonObject, and `fieldNames` names the selected fields
(in selector order) under that parent. `encoder` / `decoder` are the
codec pair for the NamedTuple (typically derived via kindlings).

Rejected alternative: **extend `JsonPrism[A]` with an optional "siblings"
suffix.** That would balloon the hot-path match on every step of the
single-field walk to branch on "is this the last step, and do we have
siblings?". Perf regression on the current single-field fast path is
unacceptable (R13). A separate class keeps both paths monomorphic and
their hot loops straight-line.

Open question on terminology: `JsonFieldsPrism` vs `MultiFieldPrism` vs
`JsonNamedTuplePrism`. Settled: `JsonFieldsPrism` — it mirrors the
`.fields` entry and says what it does (focus is a bundle of named
fields) without type-system jargon in the class name.

### D9. `JsonFieldsTraversal[A]` is a parallel sibling to `JsonTraversal[A]`

For the traversal-side multi-field focus, introduce
`JsonFieldsTraversal[A]` with shape:

```text
final class JsonFieldsTraversal[A] private[circe] (
    private[circe] val prefix: Array[PathStep],
    private[circe] val elemParent: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Dynamic: …
```

`prefix` walks from root to the iterated array. `elemParent` walks from
each array element to the object that contains the selected fields
(empty when the elements themselves are the object carrying the
fields, which is the common case). `fieldNames` names the selected
fields (selector order).

### D10. Macro error-message catalogue

All messages prefixed `JsonPrism.fields:` (or
`JsonTraversal.fields:` respectively) for grep-ability. Extends the
existing `JsonPrism.field:` / `JsonPrism selectDynamic:` /
`JsonPrism.at:` / `JsonPrism.each:` conventions in
`JsonPrismMacro.scala`.

| Condition | Message |
|-----------|---------|
| Empty varargs | `"requires at least two field selectors (for one selector, use .field(_.x) instead)."` |
| Single-selector varargs | Same as empty varargs — require arity ≥ 2 to keep the `.field` / `.fields` contract crisp. |
| Non-case-class `A` | `"parent focus ${Type.show[A]} has no case fields; .fields requires a case class."` |
| Selector not a `_.field` | `"selector at position $i must be a single-field accessor like \`_.fieldName\`. Nested paths (e.g. \`_.a.b\`) are not yet supported. Got: ${selectorTerm.show}"` |
| Unknown field | `"'$fieldName' is not a field of ${Type.show[A]}. Known fields: ${knownFields.mkString(\", \")}."` |
| Duplicate selectors | `"duplicate field selector '$fieldName' at positions $i, $j. Each field may appear at most once."` |
| NamedTuple codec unreachable | `"no given Encoder[$NT] / Decoder[$NT] in scope. Derive one via \`given Codec.AsObject[$NT] = KindlingsCodecAsObject.derive\`, or provide one manually."` (surfaces as `Expr.summon[Encoder[nt]].getOrElse(report.errorAndAbort(…))`) |

### D11. `.fields` is published via `extension` on `JsonPrism` / `JsonTraversal` — not as a method on the class

Keep `JsonPrism[A]` itself free of multi-field machinery. The
`.fields` entry lives in the `JsonPrism` companion as an `extension`,
just like the existing `.field` / `.at` / `.each` extensions:

```text
object JsonPrism:
  extension [A](o: JsonPrism[A])
    transparent inline def fields(inline selectors: (A => Any)*)
        (using Encoder[Any], Decoder[Any]): Any =
      ${ JsonPrismMacro.fieldsImpl[A]('o, 'selectors) }
```

The splice resolves the `using` clause at the call site — the macro
body synthesises `Encoder[NT]` / `Decoder[NT]` summons internally
rather than relying on the parameter list. (Using
`Expr.summon[Encoder[nt]]` inside the macro after the NamedTuple type
is known.)

Rejected alternative: method on the class. That path requires the
class to know its own multi-field API, which defeats D8's "keep both
classes monomorphic and their hot loops straight-line" rule.

### D12. Multi-field codegen strategy: walk-to-parent + per-field reads

At macro expansion time, the `.fields(_.a, _.b)` macro on a
`JsonPrism[A]`:

1. Parse selector list via `Varargs.unapply` (fallback to manual
   `asTerm` walk per OQ from plan 003).
2. Run `extractFieldName` per selector; validate against
   `A.typeSymbol.caseFields` (existence, no duplicates).
3. Build the NamedTuple type: `namesTpe` = selector-ordered
   `TypeRepr.of[*:].appliedTo(nameLits)`, `valuesTpe` = selector-ordered
   `TypeRepr.of[*:].appliedTo(fieldTypes)`, then `NamedTuple.NamedTuple
   [namesTpe, valuesTpe]`. Same pattern as plan 003's
   `LensMacro.buildLens`.
4. Summon `Encoder[NT]` / `Decoder[NT]` with
   `Expr.summon[…].getOrElse(report.errorAndAbort(D10))`.
5. Emit
   `parent.toFieldsPrism[NT](Array("a", "b"))(using encNT, decNT)`,
   where `toFieldsPrism` is a new `private[circe]` method on
   `JsonPrism` analogous to `toTraversal`. It hands the parent's
   path over as `parentPath` to the new `JsonFieldsPrism[NT]`.

The read path at runtime: walk `parentPath` to the parent JsonObject,
then iterate `fieldNames` and accumulate either per-field
`JsonFailure` (for missing or non-decoding fields) into a
`Chain.Builder`, or the read values into a staging buffer. If the
chain is empty at the end, assemble a sub-`JsonObject` of
`(fieldName -> fieldJson)` pairs, feed to `decoder: Decoder[NT]`,
return `Ior.Right(nt)`. If the chain is non-empty, return
`Ior.Both(chain, inputJson)` — per D4, multi-field read failure
leaves the input Json unchanged.

The write path at runtime: user yields a new `NT`, encode via
`encoder: Encoder[NT]` to a `JsonObject`, overlay each selected field
onto the parent via successive `JsonObject.add` calls, rebuild the
outer path on the way up. Parent-of-parent walks use the same
`JsonObject.add` fast path the single-field version exploits. The
write itself cannot fail post-encode; failures only appear in the
read phase.

The `*Unsafe` variants share the walk but replace the Ior wiring with
"return input on first failure" — straight-line code, no Chain.Builder.
A small `doWalk(…, mode: Mode)` helper parameterised over
`Mode.Collect` (builds chain) vs `Mode.ShortCircuit` (returns input)
keeps the two surfaces on a single code path without performance
coupling.

### D13. `transfer` / `place` semantics on the default Ior surface

`place(a: A): Json => Ior[Chain[JsonFailure], Json]` overwrites the
focus with `a`. Because the write doesn't *read* the focus, the only
failure modes are parent-walk misses (`PathMissing`, `NotAnObject`,
`NotAnArray`, `IndexOutOfRange`) — never `DecodeFailed`. The default
variant documents this in scaladoc. `placeUnsafe` keeps today's
silent shape: `Json => Json`.

`transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json]`
mirrors `place` with the pre-composition. Same failure modes.
`transferUnsafe` keeps today's `Json => C => Json`.

## Open Questions

Honest uncertainties. Resolve during Unit 0 (the NamedTuple-codec
spike) or Unit 1 (plumbing). Note OQ4 from the prior draft
(prism/traversal asymmetry) is **removed** — the symmetric-accumulation
decision in D3 settles it. OQ6 remains and is reframed below.

- **OQ1. NamedTuple codec derivation via kindlings at macro
  expansion time.** `kindlings-circe-derivation 0.1.0` ships
  `EncoderHandleAsNamedTupleRuleImpl` and
  `DecoderHandleAsNamedTupleRuleImpl` rules (verified via `cellar
  search-external com.kubuszok:kindlings-circe-derivation_3:0.1.0
  NamedTuple`), so `KindlingsCodecAsObject.derive[NT]` **should**
  produce a `Codec.AsObject[NT]` for a NamedTuple `NT`. But the
  rules may interact oddly with macro-synthesised NamedTuple type
  representations (e.g. opaque alias vs. raw Tuple). **Unit 0 spike
  required**: a scratch `given` in the test tree that asserts
  `summon[Encoder[NamedTuple[("a", "b"), (Int, String)]]]` compiles
  against the circeIntegration test classpath. If it fails, the
  fallback is (a) require users to provide their own NamedTuple
  codec explicitly, (b) ship a minimal hand-written NamedTuple
  codec in `eo.circe.instances` that covers at least the
  small-tuple cases we test, or (c) block the plan on upstream
  kindlings fix. Picking (a) is a strict-but-honest fallback; (b)
  has scope-creep risk. Recommend (a) if the spike fails.
- **OQ2. `Varargs.unapply` on `inline` varargs in Scala 3.8.3.**
  Same uncertainty plan 003's OQ3 flagged. Fallback is a manual
  `asTerm` walk through `Inlined(_, _, Typed(Seq(...), _))`. Verify
  in Unit 1.
- **OQ3. `JsonFailure` case shape: just `step: PathStep`, or
  `pathContext: Array[PathStep]` as well?** Debatable. The walk has
  the full prefix in hand when a failure fires. Carrying it costs
  one `Array.copyOf` per failure — negligible on the error path,
  zero on the happy path. But it bloats every `JsonFailure` instance
  to a small boxed array. **Decision: ship v1 without `pathContext`;
  revisit once behaviour specs demand it.** An adapter method
  `JsonFailure.withPath(prefix: Array[PathStep]): JsonFailure` can
  decorate after the fact if needed.
- **OQ4. `transparent inline` return-type refinement through the
  `.fields` extension.** The extension signature has to declare some
  concrete return type; we write `Any` and rely on `transparent
  inline` to refine to `JsonFieldsPrism[NT]`. Same question plan 003
  raised for the generics varargs entry. Proven on `.at` and `.each`
  which already do this in `JsonPrismMacro.scala`. Low risk;
  verification is a scratch compile in Unit 1.
- **OQ5. Do the new default methods need cats-effect / IO-flavoured
  variants?** The default Ior surface covers the stated need. A future
  `modifyF[G]`-integrated variant that combines monadic traversal
  with observable failure is possible but a clear later-plan item.
  Neither the default nor the `*Unsafe` variant blocks on this.
- **OQ6 (new). `*Unsafe` vs default-Ior hot-path perf delta.**
  Hypothesis: `modifyUnsafe` is byte-identical to today's
  `modify`, so its microbench numbers match main exactly. Default
  `modify` adds a happy-path cost of one `Ior.Right(json)` allocation
  per successful call and a `Chain.Builder` materialisation only when
  failures surface. Expected overhead: low single-digit nanoseconds
  on the happy path. Flagged for Unit-N (Unit 1) verification via a
  targeted bench run. Not a go / no-go blocker — if the delta is
  larger than expected, either (a) inline the Ior.Right wrapping via
  an inline helper, (b) introduce a `modifyMaybe` variant that
  returns `Json | Ior[…]` via a union type if the delta matters to
  users. Document the number in `site/docs/benchmarks.md` after
  Unit 1.

## High-Level Technical Design

> *Directional guidance for reviewers. The implementing agent should
> treat this as context, not code to reproduce.*

### `JsonFailure` ADT sketch

```text
// circe/src/main/scala/eo/circe/JsonFailure.scala
package eo.circe

import io.circe.DecodingFailure

enum JsonFailure:
  /** Named field absent from its parent JsonObject at `step`. */
  case PathMissing(step: PathStep)

  /** Parent wasn't a JsonObject at `step` (so we couldn't look up a field). */
  case NotAnObject(step: PathStep)

  /** Parent wasn't a JSON array at `step` (so we couldn't index). */
  case NotAnArray(step: PathStep)

  /** Index was outside `[0, size)` at `step`. */
  case IndexOutOfRange(step: PathStep, size: Int)

  /** Decoder refused at `step`. `step` is `PathStep.Field("")` (or a
    * sentinel) at root-level decode failures. */
  case DecodeFailed(step: PathStep, cause: DecodingFailure)

  /** Human-readable diagnostic. Kept separate from toString so the
    * default enum.toString still exposes the structural form. */
  def message: String = this match
    case PathMissing(s)    => s"path missing at $s"
    case NotAnObject(s)    => s"expected JSON object at $s"
    case NotAnArray(s)     => s"expected JSON array at $s"
    case IndexOutOfRange(s, n) => s"index out of range at $s (size=$n)"
    case DecodeFailed(s, c)    => s"decode failed at $s: ${c.message}"
```

(Separately: `PathStep` visibility drops from `private[circe]` to
public in the same commit.)

### Failure accumulation through the walk loops

Both the single-field and multi-field walks share a parameterised
helper:

```text
private[circe] enum WalkMode:
  case Collect(builder: Chain.Builder[JsonFailure])
  case ShortCircuit  // used by the *Unsafe variants
```

The walk loop records failures into the `Collect(builder)` case and
continues where the semantics allow (traversal per-element;
multi-field read per-field), or records one and aborts when it
can't (prefix walk). The `ShortCircuit` case returns the input Json
on the first failure, straight-line, with no allocation. This is the
byte-identical replication of today's behaviour for `*Unsafe`.

### Default `modify` / `transform` / etc. on `JsonPrism[A]` (single-field)

```text
def modify(f: A => A): Json => Ior[Chain[JsonFailure], Json]
def transform(f: Json => Json): Json => Ior[Chain[JsonFailure], Json]
def place(a: A): Json => Ior[Chain[JsonFailure], Json]
def transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json]
def get(json: Json): Ior[Chain[JsonFailure], A]

// *Unsafe escape hatches — today's semantics preserved.
inline def modifyUnsafe(f: A => A): Json => Json
inline def transformUnsafe(f: Json => Json): Json => Json
inline def placeUnsafe(a: A): Json => Json
inline def transferUnsafe[C](f: C => A): Json => C => Json
inline def getOptionUnsafe(json: Json): Option[A]
```

### Multi-field macro codegen flow

```text
codecPrism[Person].fields(_.name, _.age)
    │
    ▼  JsonPrism.fields  [transparent inline extension]
    ▼  JsonPrismMacro.fieldsImpl[A]
    ▼  parse varargs → fieldNames (selector order)
    ▼  validate (arity ≥ 2 / no duplicates / known fields / case class)
    ▼  build NamedTuple type: NT = NamedTuple[(n1, …, nk), (t1, …, tk)]
    ▼  Expr.summon[Encoder[NT]]; Expr.summon[Decoder[NT]]  (D10 error if missing)
    │
    ▼  emit: parent.toFieldsPrism[NT](Array(n1, …, nk))(using encNT, decNT)
    │
    ▼  runtime: JsonFieldsPrism[NT](parent.path, fieldNames, encNT, decNT)
```

### `JsonFieldsPrism[A]` class shape

```text
final class JsonFieldsPrism[A] private[circe] (
    private[circe] val parentPath: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either],
      Dynamic:

  type X = (DecodingFailure, HCursor)

  // Abstract Optic members (for generic Optic contract).
  def to: Json => Either[(DecodingFailure, HCursor), A] = …
  def from: Either[(DecodingFailure, HCursor), A] => Json = …

  // Default Ior-bearing surface.
  def modify(f: A => A): Json => Ior[Chain[JsonFailure], Json] = …
  def transform(f: Json => Json): Json => Ior[Chain[JsonFailure], Json] = …
  def place(a: A): Json => Ior[Chain[JsonFailure], Json] = …
  def transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json] = …
  def get(json: Json): Ior[Chain[JsonFailure], A] = …

  // *Unsafe escape hatches.
  inline def modifyUnsafe(f: A => A): Json => Json = …
  inline def transformUnsafe(f: Json => Json): Json => Json = …
  inline def placeUnsafe(a: A): Json => Json = …
  inline def transferUnsafe[C](f: C => A): Json => C => Json = …
  inline def getOptionUnsafe(json: Json): Option[A] = …
```

The `Dynamic` mix-in is retained for symmetry — a follow-up could
let `.fields(_.a, _.b).c` drill further, but that's out of scope.
Relationship to `JsonPrism`: sibling. No inheritance. Shared
algebraic role (Prism over Json) captured via the common `Optic`
supertype.

### `JsonFieldsTraversal[A]` class shape

```text
final class JsonFieldsTraversal[A] private[circe] (
    private[circe] val prefix: Array[PathStep],
    private[circe] val elemParent: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Dynamic:

  // Default Ior-bearing surface (Chain accumulates per-element × per-field).
  def modify(f: A => A): Json => Ior[Chain[JsonFailure], Json] = …
  def transform(f: Json => Json): Json => Ior[Chain[JsonFailure], Json] = …
  def place(a: A): Json => Ior[Chain[JsonFailure], Json] = …
  def transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json] = …
  def getAll(json: Json): Ior[Chain[JsonFailure], Vector[A]] = …

  // *Unsafe escape hatches.
  inline def modifyUnsafe(f: A => A): Json => Json = …
  inline def transformUnsafe(f: Json => Json): Json => Json = …
  inline def placeUnsafe(a: A): Json => Json = …
  inline def transferUnsafe[C](f: C => A): Json => C => Json = …
  inline def getAllUnsafe(json: Json): Vector[A] = …
```

### Method signatures — summary table

| Class | Default (new) | `*Unsafe` (preserves today's shape) |
|-------|---------------|--------------------------------------|
| `JsonPrism[A]` | `modify(f): Json => Ior[Chain[JsonFailure], Json]` | `modifyUnsafe(f): Json => Json` |
| `JsonPrism[A]` | `transform(f): Json => Ior[Chain[JsonFailure], Json]` | `transformUnsafe(f): Json => Json` |
| `JsonPrism[A]` | `place(a): Json => Ior[Chain[JsonFailure], Json]` | `placeUnsafe(a): Json => Json` |
| `JsonPrism[A]` | `transfer(f): Json => C => Ior[Chain[JsonFailure], Json]` | `transferUnsafe(f): Json => C => Json` |
| `JsonPrism[A]` | `get(json): Ior[Chain[JsonFailure], A]` | `getOptionUnsafe(json): Option[A]` |
| `JsonFieldsPrism[A]` | same five | same five |
| `JsonTraversal[A]` | `modify(f): Json => Ior[Chain[JsonFailure], Json]` | `modifyUnsafe(f): Json => Json` |
| `JsonTraversal[A]` | `transform(f): Json => Ior[Chain[JsonFailure], Json]` | `transformUnsafe(f): Json => Json` |
| `JsonTraversal[A]` | `place(a): Json => Ior[Chain[JsonFailure], Json]` (new surface) | `placeUnsafe(a): Json => Json` (new) |
| `JsonTraversal[A]` | `transfer(f): Json => C => Ior[Chain[JsonFailure], Json]` (new) | `transferUnsafe(f): Json => C => Json` (new) |
| `JsonTraversal[A]` | `getAll(json): Ior[Chain[JsonFailure], Vector[A]]` | `getAllUnsafe(json): Vector[A]` |
| `JsonFieldsTraversal[A]` | same five | same five |

**Note on `place` / `transfer` on `JsonTraversal`:** the current
`JsonTraversal.scala` exposes only `modify`, `transform`, `getAll`
as forgiving methods. Under the rename those become `modifyUnsafe`,
`transformUnsafe`, `getAllUnsafe`, and the new default `modify`,
`transform`, `getAll` replace them with Ior-bearing signatures. The
`place` and `transfer` forgiving methods don't exist yet for
traversals — Unit 2 adds both the new default `place` / `transfer`
and their `*Unsafe` counterparts in the same commit, because
otherwise the naming symmetry breaks.

## Implementation Units

- [x] **Unit 0: NamedTuple codec derivation spike**

  **Goal:** confirm that `KindlingsCodecAsObject.derive[NT]` produces
  a usable `Codec.AsObject[NT]` for a macro-synthesised NamedTuple,
  and that `Expr.summon[Encoder[NT]]` inside a Scala 3 quote resolves
  it.

  **Files:** temp scratch file
  `circe/src/test/scala/eo/circe/NamedTupleCodecSpike.scala` (deleted
  before merging Unit 3 if the spike succeeds; retained as a
  regression spec if it surfaces caveats).

  **Approach:** two tests. (1) Hand-declared:
  `given c: Codec.AsObject[NamedTuple[("name", "age"), (String, Int)]]
  = KindlingsCodecAsObject.derive; val j: Json =
  NamedTuple[…]("Alice", 30).asJson; j.as[NamedTuple[…]] === Right(…)`.
  (2) Via `Expr.summon`: a tiny inline macro in the test scope that
  runs `Expr.summon[Encoder[NamedTuple[("n",), (Int,)]]]` and
  reports success/failure.

  **Verification:** both tests green on `sbt circeIntegration/test`.

  **Exit condition:** if the hand-declared case fails, halt the
  plan and re-prioritise OQ1 fallback (a), (b), or (c). If only the
  macro-side summon fails, OQ4 becomes real; adopt an explicit-given
  fallback where users must provide the NamedTuple codec themselves
  and `.fields` does NOT auto-summon.

- [x] **Unit 1: `JsonFailure` ADT + `PathStep` visibility bump + rename to `*Unsafe` + default Ior surface on single-field `JsonPrism`**

  **Goal:** validate the default-Ior surface and the `*Unsafe` escape
  hatches on the current single-field optic before adding multi-field
  complexity. This is the breaking-change commit; the rest of the
  plan rides on its naming.

  **Files:**
  - New: `circe/src/main/scala/eo/circe/JsonFailure.scala` (ADT per D6).
  - Modify: `circe/src/main/scala/eo/circe/PathStep.scala` (drop
    `private[circe]` on the `enum PathStep`; add scaladoc header).
  - Modify: `circe/src/main/scala/eo/circe/JsonPrism.scala` —
    (a) rename `modify` → `modifyUnsafe`, `transform` →
    `transformUnsafe`, `place` → `placeUnsafe`, `transfer` →
    `transferUnsafe`, `getOption` → `getOptionUnsafe`; keep their
    bodies byte-identical;
    (b) add new default `modify`, `transform`, `place`, `transfer`
    (Ior-bearing) and a new `get` (Ior-bearing) built on top of the
    `WalkMode.Collect` helper.
  - Modify: `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` —
    every spec currently asserting against the forgiving API gets
    split into paired assertions: one against `*Unsafe` (identity to
    today), one against the default Ior surface (`Ior.Right` on
    happy path, `Ior.Both(chain, inputJson)` on failure).
  - New: `circe/src/test/scala/eo/circe/JsonFailureSpec.scala` (ADT
    observable-identity tests: every case reachable from at least one
    scenario).
  - Modify: `benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala`
    and `JsonPrismWideBench.scala` — every method currently calling
    `modify` / `transform` / etc. retargeted at the `*Unsafe`
    variants so the pre-existing perf reference survives.

  **Approach:** introduce a shared `doWalk[T](json, mode: WalkMode):
  Either[JsonFailure, T]`-style private helper that drives the
  loop and feeds failures into the `Collect(builder)` case or
  short-circuits under `ShortCircuit`. Both the default and `*Unsafe`
  surfaces call it with the appropriate mode. For the default, the
  final result coerces `Chain.Builder.result()` → `Ior.Right(json)`
  if empty, `Ior.Both(chain, inputJson)` otherwise (for modify /
  transform / place / transfer) or `Ior.Left(chain)` for `get` when
  nothing can be returned.

  **Verification:** `sbt circeIntegration/test` green. Every
  `JsonFailure` case exercised. A one-shot
  `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*JsonPrismBench.*"` run
  produces numbers within JMH noise of main for the `*Unsafe` path
  (R13). The default path is logged as a new datapoint and its
  overhead vs. `*Unsafe` documented for OQ6.

  **Test scenarios:** path-missing; non-object parent; non-array
  parent; index-out-of-range; decode-failed; every default method at
  least once; every `*Unsafe` method at least once;
  `modify(f)(happy)` === `Ior.Right(modifyUnsafe(f)(happy))`;
  `modify(f)(broken)` === `Ior.Both(chain-of-one, broken)` matching
  `modifyUnsafe(f)(broken) === broken`.

- [x] **Unit 2: Default + `*Unsafe` surface on `JsonTraversal` + new forgiving `place` / `transfer` + accumulation**

  **Goal:** land the rename + Ior-bearing defaults + accumulation
  story on `JsonTraversal`. Plug the missing `place` / `transfer`
  symmetric pair.

  **Files:**
  - Modify: `circe/src/main/scala/eo/circe/JsonTraversal.scala` —
    rename `modify` / `transform` / `getAll` to `*Unsafe`, add
    defaults, add new `place` / `transfer` pairs (default + unsafe).
  - Modify: `circe/src/test/scala/eo/circe/JsonTraversalSpec.scala`
    (create if absent) — behaviour specs for each surface.
  - Modify: `benchmarks/src/main/scala/eo/bench/JsonTraversalBench.scala`
    — retarget at `*Unsafe` for perf reference.

  **Approach:** the per-element walk reuses the `Collect(builder)`
  helper. After the element map, `Ior.Right(newJson)` if empty chain,
  `Ior.Both(chain, newJson)` if non-empty (the successful elements
  updated in place). Prefix failures `Ior.Left(chain-of-one)`
  because nothing to iterate. Per D4: prefix failures do not return
  the input as a fallback — `Ior.Left` with the input Json excluded
  is the honest signal. Users who want the fallback reach for
  `.leftMap(_ => input)` or `_.getOrElse(json)` explicitly.

  **Execution note:** `Ior`, `Chain` both arrive via `cats.data`;
  `cats-core` is already on `circeIntegration`. No new Maven
  coordinates.

  **Verification:** `sbt circeIntegration/test` green. Accumulation
  scenarios (three out of five elements fail for different reasons)
  exercised; result Json reflects the two successful updates.

  **Test scenarios:** empty array (chain-empty, `Ior.Right(input)`);
  every-element-succeeds (chain-empty, `Ior.Right(updated)`); one
  element decode-fails (chain-length-1 with `DecodeFailed`,
  `Ior.Both`); mixed failure reasons across elements
  (`Ior.Both(chain-length-many, partially-updated)`); prefix-missing
  (chain-length-1 with `PathMissing`, `Ior.Left`).

- [x] **Unit 3: Multi-field `JsonFieldsPrism[A]` + `.fields` macro + read/write codegen**

  **Goal:** `.fields(_.a, _.b)` compiles on `JsonPrism[A]` and emits
  a `JsonFieldsPrism[NT]` whose default `.modify` round-trips all
  selected fields atomically with Ior-shaped results.

  **Files:**
  - New: `circe/src/main/scala/eo/circe/JsonFieldsPrism.scala` — class
    per D8. Full default + `*Unsafe` surface from day one.
  - Modify: `circe/src/main/scala/eo/circe/JsonPrism.scala` — add
    `private[circe] def toFieldsPrism[B](fieldNames: Array[String])
    (using Encoder[B], Decoder[B]): JsonFieldsPrism[B]`.
  - Modify: `circe/src/main/scala/eo/circe/JsonPrismMacro.scala` —
    add `fieldsImpl[A](parent, selectorsE)`. Parse, validate, synthesise
    NamedTuple type, summon codecs, emit `toFieldsPrism` call.
  - Modify: `circe/src/main/scala/eo/circe/JsonPrism.scala` (companion
    `extension` section) — add `.fields` entry per D2 / D11.
  - Modify: `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` — new
    section `"JsonPrism .fields multi-field focus"`.

  **Approach:** walk `parentPath` to get the parent object. Per D12,
  iterate `fieldNames` into a `Chain.Builder` (default) or
  short-circuit (unsafe). If all fields read cleanly, assemble a
  sub-`JsonObject`, decode to NT, invoke `f`, encode back, overlay
  each field onto the parent via `JsonObject.add`, rebuild the outer
  path, return `Ior.Right(newJson)`. If any fields failed, return
  `Ior.Both(chain, inputJson)` — the atomicity rule from D4 makes
  the input Json the correct "best we can do" carrier.

  **Verification:** `sbt circeIntegration/test` green.
  `.fields(_.name, _.age)` modify-round-trip passes on realistic
  cases (Person); leaves every non-focused field byte-identical.

  **Test scenarios:** 2-of-3 partial cover on Person; 3-of-3 full
  cover (confirming D1's no-auto-Iso rule — the output is still
  `JsonFieldsPrism`); selector order ≠ declaration order;
  `place` on a multi-field; default behaviour on each failure mode;
  multi-failure scenario (`name` and `age` both missing → chain of
  two).

- [x] **Unit 4: Multi-field `JsonFieldsTraversal[A]` + `.fields` on traversal**

  **Goal:** `.fields(_.name, _.price)` on a `JsonTraversal` compiles
  and produces a traversal whose `.modify` updates every element's
  selected fields atomically-per-element, accumulating failures
  across both fields and elements.

  **Files:**
  - New: `circe/src/main/scala/eo/circe/JsonFieldsTraversal.scala`
    — class per D9.
  - Modify: `circe/src/main/scala/eo/circe/JsonTraversal.scala` —
    add `private[circe] def toFieldsTraversal[B](fieldNames:
    Array[String])(using Encoder[B], Decoder[B]):
    JsonFieldsTraversal[B]`; add `.fields` extension in the
    companion.
  - Modify: `circe/src/main/scala/eo/circe/JsonPrismMacro.scala` —
    add `fieldsTraversalImpl[A](parent, selectorsE)`, mirror of
    `fieldsImpl`.
  - Modify: `circe/src/test/scala/eo/circe/JsonPrismSpec.scala` (or
    a dedicated `JsonFieldsTraversalSpec.scala`) — behaviour specs.

  **Approach:** per-element walk = `elemParent` walk from the element
  root to the object carrying the fields, then the same "read each
  named field, assemble, decode" pattern as Unit 3. Per D4,
  per-element atomicity: if element 5 fails on two fields, element 5
  is left unchanged in the output and two failures are recorded for
  it; successful elements update in place. Across elements the chain
  accumulates naturally.

  **Verification:** `sbt circeIntegration/test` green.

  **Test scenarios:** `Basket(items: Vector[Order])` with each Order
  having `(name, price, qty)`; `.each.fields(_.name, _.price)`
  modify uppercase name + double price; default observe failures on
  per-element mixed scenarios (three of five elements have one
  broken field each → chain of three, two of five updated);
  default observes multi-failure per element (element 2 has both
  `name` and `price` missing → two entries in chain for element 2,
  element unchanged); forgiving on empty array / missing prefix.

- [ ] **Unit 5: Macro-error diagnostics hardening**

  **Goal:** every D10 row has an exact-message compile-error test.

  **Files:**
  - New: `circe/src/test/scala/eo/circe/FieldsMacroErrorSpec.scala`
    using `scala.compiletime.testing.typeCheckErrors`.
  - Possible: minor polish pass on `JsonPrismMacro.scala` wording.

  **Approach:** one spec per D10 row. Assert the
  `JsonPrism.fields:` / `JsonTraversal.fields:` prefix plus the
  diagnostic-specific substring.

  **Verification:** `sbt circeIntegration/test` green.

  **Test scenarios:** empty varargs; single-selector varargs
  (should suggest `.field`); duplicate selector; unknown field;
  non-field selector (`_.name.toUpperCase`); non-case-class `A`;
  NamedTuple-codec-missing (construct a NamedTuple shape whose
  field types have no codec, e.g. a user-defined class without a
  Codec).

- [ ] **Unit 6: Property tests + discipline Prism laws on `JsonFieldsPrism`**

  **Goal:** witness `JsonFieldsPrism` satisfies the three Prism laws
  under `cats-eo-laws`'s `PrismLaws`. Property coverage matches
  what the single-field `JsonPrismSpec` already delivers, now against
  both the default Ior surface and the `*Unsafe` surface.

  **Files:**
  - Modify: `build.sbt` — add `libraryDependencies += laws % Test`
    on `circeIntegration`.
  - New: `circe/src/test/scala/eo/circe/JsonFieldsPrismLawsSpec.scala`
    — wire `PrismLaws[…]` via discipline-specs2.
  - Modify: `circe/src/test/scala/eo/circe/JsonFieldsPrismSpec.scala`
    (or the multi-field section in `JsonPrismSpec.scala`) — add
    ScalaCheck `forAll` properties targeting both surfaces.

  **Approach:** generate `Person` arbitraries via cats-kernel
  `Arbitrary` composites; wire through the `PrismLaws` discipline
  RuleSet targeted against `*Unsafe` (the shape `PrismLaws` already
  expects — `to: Json => Either[_, A]`, `from: A => Json`). The
  default Ior surface is witnessed via `forAll` properties, not the
  law class — law classes for Ior-shaped optics are a separate
  future plan.

  **Verification:** `sbt circeIntegration/test` green; ≥ 10 new
  property specs.

  **Test scenarios:** Prism law witnesses on
  `codecPrism[Person].fields(_.name, _.age)` via the `*Unsafe`
  surface; `forAll` property: `modify(id)(json) === Ior.Right(json)`
  on happy inputs; commutativity of two disjoint-field modifies (a
  bonus property, not a Prism law).

- [ ] **Unit 7: Cross-carrier composition regression specs**

  **Goal:** prove the new multi-field JsonPrism / JsonTraversal compose
  cleanly with the rest of the optics family — specifically with
  plain Scala-level Lenses, macro-derived Lenses from
  `eo.generics`, and `AffineFold` — so the Composer-bridge
  machinery (`Composer[Tuple2, Either]`, `Composer[Either, Affine]`,
  etc.) doesn't silently break when JsonFieldsPrism lands as a new
  Optic subclass.

  This unit is regression insurance, not new feature work — every
  scenario here was specifically requested to remain load-bearing in
  future. If a Composer-bridge or Morph instance regresses, these
  specs fail at compile time or yield wrong Ior diagnostics.

  **Files:**
  - New: `circe/src/test/scala/eo/circe/CrossCarrierCompositionSpec.scala`
    — the circe test module already depends on both `core` (for
    Lens / AffineFold) and `generics` (for the `lens` macro), so it
    is the natural home for tests that span all three modules.

  **Test scenarios (every one a named spec block):**

  1. **Plain Lens → multi-field JsonPrism → AffineFold, native
     `.andThen` chain.** Fixture: `case class Box(payload: Json)`,
     `val box = Lens[Box, Json](_.payload, (b, j) => b.copy(payload = j))`;
     `codecPrism[Person].fields(_.name, _.age)` focuses a
     `(name: String, age: Int)` NamedTuple; `AffineFold` reads the
     name iff `age >= 18`. Full chain:
     `box.andThen(codecPrism[Person].fields(_.name, _.age))
         .andThen(AffineFold(nt => Option.when(nt.age >= 18)(nt.name)))`.
     Validate: `.getOption(Box(adultJson)) == Some("Alice")` and
     `.getOption(Box(minorJson)) == None`. This exercises
     `Composer[Tuple2, Either]` → `Composer[Either, Affine]`
     transitive chaining across three distinct carriers. Chain's
     final carrier is `Affine`.

  2. **Generics `lens[S](_.field)` → single-field JsonPrism,
     native `.andThen`.** Fixture: `case class Envelope(payload: Json)`;
     `val outer = lens[Envelope](_.payload)` (from `eo.generics`);
     `val inner = codecPrism[Person].field(_.name)`. Chain:
     `outer.andThen(inner)`. Validate both surfaces:
     - `.modify(_.toUpperCase)(env).map(_.payload)` returns
       `Ior.Right` with the uppercase Person JSON when the payload
       decodes.
     - `.modifyUnsafe(_.toUpperCase)(env)` returns an Envelope with
       the modified JSON payload.
     - Diagnostic: when `env.payload` is the empty JsonObject `{}`,
       the default Ior surface returns
       `Ior.Both(Chain(PathMissing(...)), env)`, not a silent no-op.

  3. **Generics `lens[S](_.field)` → multi-field JsonPrism
     (`.fields`), native `.andThen`.** Same outer Envelope. Inner
     is `codecPrism[Person].fields(_.name, _.age)`. Chain validates
     that `JsonFieldsPrism` — the new class introduced in Unit 3 —
     participates in cross-carrier composition identically to the
     existing single-field `JsonPrism`. Explicit test that
     per-NamedTuple-field failure (e.g. `age` is absent) surfaces
     through the chain's Ior output as a `JsonFailure` referencing
     the correct `PathStep.Field`.

  4. **Generics `lens[S](_.field)` → JsonTraversal, manual
     composition idiom.** JsonTraversal does NOT extend `Optic`
     (see `site/docs/concepts.md` — a deliberate design choice),
     so native `.andThen` is unavailable. The idiomatic chain is
     `outerLens.modify(trav.modify(f)(_))(s)` at the Scala-lens
     level plus `trav.modifyUnsafe(f)` at the JSON level. The test
     documents this manual idiom and verifies it compiles and
     produces the expected output. Include a TODO comment in the
     spec pointing at the future-work plan to make JsonTraversal
     an Optic (see Future Considerations below).

  5. **Generics `lens[S](_.field)` → multi-field JsonTraversal
     (`.each.fields`), manual composition idiom.** Same manual
     idiom as (4). Verifies the new `.fields`-bearing traversal
     from Unit 4 slots into the manual composition path without
     extra friction.

  **Approach:** use real `Encoder` / `Decoder` instances from
  `kindlings-circe-derivation` (already on the test classpath).
  Each spec block constructs its fixture inline, composes, and
  asserts at least one success case, one failure case, and one
  diagnostic case (via the default Ior surface). No property
  tests here — this is compile-time + fixed-input regression
  insurance.

  **Execution note:** this unit EXISTS to catch silent
  cross-module regressions. A passing `sbt circeIntegration/test`
  is the primary signal; secondary signal is that none of the
  five chains use `asInstanceOf` or `.asInstanceOf[Optic[...]]`
  casts to typecheck.

  **Verification:**
  - `sbt circeIntegration/test` green with five new spec blocks.
  - No `asInstanceOf` in the new spec file.
  - `scalafmtCheckAll` green.

  **Test scenarios summary:** five named composition chains as
  above. Every chain asserts at least one success case AND at
  least one diagnostic case (the Ior.Both / Ior.Left path).

- [ ] **Unit 8: Documentation**

  **Goal:** `site/docs/circe.md` gains the full surface story;
  `site/docs/cookbook.md` gains a diagnostics recipe; a migration
  notes subsection covers the rename. Every new fence is
  mdoc-verified.

  **Files:**
  - Modify: `site/docs/circe.md` — after the "JsonTraversal (`.each`)"
    section, add:
    - **"Multi-field focus — `.fields(_.a, _.b)`"**: 2-field Person
      example; full-cover example with commentary on "this is still
      a JsonFieldsPrism, NOT a JsonIso"; per-element `.each.fields`
      example.
    - **"Reading diagnostics from the default Ior surface"**:
      side-by-side `.modify` (returns `Ior[Chain[JsonFailure], Json]`)
      vs `.modifyUnsafe` (returns `Json`) on a stump Json; listing
      each `JsonFailure` case; traversal accumulation example
      showing `Ior.Both(chain, partially-updated)`.
    - **"Ignoring failures"** (a shorter sub-section): the
      idiomatic two lines for callers who want today's behaviour —
      `prism.modifyUnsafe(f)(json)` OR
      `prism.modify(f)(json).getOrElse(json)`.
    - **"Migration notes (rename in v0.2)"**: a compact subsection
      listing the five renamed methods on each of the four classes
      (`JsonPrism`, `JsonFieldsPrism`, `JsonTraversal`,
      `JsonFieldsTraversal`), the mechanical replacement, and the
      justification. Pointer to the Ior section for callers who want
      to adopt the new surface rather than just rename.
  - Modify: `site/docs/cookbook.md` — new recipe "Diagnose a silent
    JSON edit no-op" demonstrating
    `prism.modify(f)(stump) === Ior.Both(Chain(JsonFailure.PathMissing(PathStep.Field("address"))), stump)`.
  - Consider (low priority): one-line "see circe.md" cross-link in
    any existing cookbook recipe that uses `codecPrism`.

  **Approach:** mdoc fences as plain compiled scenarios. The
  documentation doubles as example specs.

  **Verification:** `sbt docs/mdoc` green; `sbt scalafmtCheckAll`
  green.

## Risks & Dependencies

- **R1 (high). Breaking change to the existing `eo.circe` public
  API.** Every external caller of `modify` / `transform` / `place` /
  `transfer` / `getOption` on `JsonPrism` (and `modify` / `transform`
  / `getAll` on `JsonTraversal`) gets a compile error post-Unit 1.
  Mitigation: (a) the mechanical replacement is exact — swap the
  method name for its `*Unsafe` sibling, behaviour preserved
  byte-for-byte; (b) the migration notes subsection in
  `site/docs/circe.md` documents the fix; (c) the plan is landed in
  a single PR so the rename is atomic across the module and its
  benchmarks. The `0.1.0-SNAPSHOT` semver window explicitly tolerates
  this class of break. A CHANGELOG entry is planned once the
  CHANGELOG file exists — until then, the plan document itself
  serves as the migration record.
- **R2 (low). `*Unsafe` hot-path perf regression.** The rename is
  byte-identical to today's bodies; no logic change. Mitigation:
  post-Unit 1 single-shot bench spot-check
  (`sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*JsonPrismBench.*"`),
  compared against main HEAD before the rename. Expected delta:
  zero within noise.
- **R3 (medium — new). Default Ior-surface overhead vs `*Unsafe`.**
  OQ6 flags this explicitly. Expected: low single-digit ns per call
  on the happy path (one `Ior.Right(json)` allocation). If the
  microbench shows more, the mitigation ladder is (a) `@inline`
  helpers, (b) specialised fast-path branches, (c) as last resort, a
  `Json | Ior[…]` union-type variant. Not a go/no-go.
- **R4 (medium). NamedTuple codec derivation via kindlings
  `0.1.0`.** OQ1 captures this. Mitigation: Unit 0 is gated — if
  the spike fails, the plan halts and the OQ1 fallback is adopted.
  Kindlings does ship the `…AsNamedTupleRuleImpl` rules (verified),
  so the spike is expected to succeed; the risk is residual
  interaction bugs with `Expr.summon` inside a macro.
- **R5 (low). mdoc compile surface grows.** Unit 8 adds six-to-eight
  new mdoc fences. If NamedTuple codec derivation is flaky in the
  docs sub-project (which has a distinct classpath from
  `circeIntegration`), the kindlings dep addition in `docs/` may
  need a tweak. Currently `kindlingsCirce` is declared Compile-scope
  on `docs` (see `build.sbt` line 343). No change expected.
- **R6 (low). `JsonFailure` case coverage evolves.** If a future
  spec surfaces a failure mode the current ADT doesn't capture
  (e.g. "encoder threw"), we'll add a case non-breakingly.
- **R7 (medium). Discipline-PrismLaws wiring in circeIntegration/test.**
  Adding `laws % Test` is a new edge. The `cats-eo-laws` artifact
  is built into the repo aggregate, so it resolves via sbt's own
  LocalProject. The risk is a mismatch between the `PrismLaws`
  expectations and the `JsonPrism`-shaped Optic: `PrismLaws` in
  cats-eo-laws expects an abstract `Optic[_, _, _, _, Either]` with
  a specific `to` / `from` behaviour. Since `JsonPrism` already
  extends that supertype and all its specs pass (confirmed by the
  current test suite), the multi-field variant should pass without
  change. If the law wiring surfaces a real semantic mismatch in
  the `JsonFieldsPrism.to` / `.from` implementation, the fix belongs
  in `JsonFieldsPrism`, not in the laws, and Unit 6 will surface it.
- **R8 (low). Trailing-comma scalafmt on varargs of selector lambdas.**
  Same risk plan 003 flagged. Low-impact; fix in Unit 3 if it appears.

**Dependencies:** one new Maven-level test dep — `laws % Test` on
`circeIntegration` (technically a LocalProject dep, not a new
external Maven artifact). No new external deps. No sbt plugin
additions. `cats.data.Ior` and `cats.data.Chain` both ship with
`cats-core`, already on `circeIntegration`.

**Shared-internals perf note:** the default and `*Unsafe` surfaces
share their walk loop via the `WalkMode.Collect` / `WalkMode.ShortCircuit`
parameterisation described in D12. This means (a) `*Unsafe` callers
pay no Ior or Chain overhead and (b) the two surfaces cannot diverge
behaviourally without reviewer notice.

## Documentation Plan

- **`site/docs/circe.md`** — four new sections immediately after
  the existing "JsonTraversal (`.each`)" section:
  1. "Multi-field focus — `.fields(_.a, _.b)`" with three mdoc
     fences: two-field partial-cover Person; full-cover example
     (with the explanatory paragraph on D1's no-auto-Iso rule);
     `.each.fields` on a Basket.
  2. "Reading diagnostics from the default Ior surface" with two mdoc
     fences: side-by-side `.modify` / `.modifyUnsafe` on a stump
     Json; traversal `.modify` accumulating per-element failures into
     `Ior.Both(chain, partially-updated)`.
  3. "Ignoring failures (the `*Unsafe` escape hatch)" — a short
     sub-section showing the two idiomatic forms.
  4. **"Migration notes (v0.2 rename)"** — compact table listing every
     renamed method across the four classes, with a one-line
     before/after example. Stresses that `*Unsafe` is behaviour-preserving;
     the default Ior surface is a new option, not a replacement
     behaviour.
- **`site/docs/cookbook.md`** — one new recipe "Diagnose a silent
  JSON edit no-op", three-to-five mdoc fences showing the
  `JsonFailure` cases users hit most often, plus the "just ignore
  them" recipe for the `*Unsafe` variant.
- **`site/docs/optics.md`** — no changes. Cross-representation optics
  live in `circe.md`; optics.md is about the core families.
- **`CLAUDE.md`** — no changes required (the agent-facing guide does
  not currently document the circe-integration API surface at the
  level `.fields` would slot into; if Unit 8 surfaces a natural fit,
  add a one-liner then).
- **Scaladoc on `JsonFailure`, `JsonFieldsPrism`,
  `JsonFieldsTraversal`** — full scaladoc header on each, mirroring
  the density of today's `JsonPrism.scala` header (the "Shape on
  the Optic type", "Failure diagnostics" paragraphs). Every case in
  `JsonFailure` gets a one-sentence doc. Scaladoc on every renamed
  `*Unsafe` method calls out "Escape hatch: preserves pre-0.2
  silent-forgiving behaviour; prefer the Ior-returning sibling
  unless performance measurement shows the difference."
- **`CHANGELOG.md`** — does not yet exist in this repo. When it lands
  (coordinate with the production-readiness plan), the first release
  introducing this change adds a clearly-marked **Breaking** section
  listing the rename and the new Ior-default surface. Until then, the
  migration notes subsection in `site/docs/circe.md` and this plan
  document are the migration record.
- **`site/docs/benchmarks.md`** — add a short note documenting the
  measured `*Unsafe` vs default-Ior delta after Unit 1, tying back
  to OQ6.

## Success Metrics

- **SM1.** `sbt compile` green on all five root-aggregate sub-projects
  (core / laws / tests / generics / circeIntegration).
- **SM2.** `sbt test` green. `circeIntegration/test` gains ≥ 30 new
  tests across Units 1–6 (five more than the previous draft because
  every existing spec now has paired default/`*Unsafe` assertions),
  every `JsonFailure` case exercised in at least one spec, every D10
  error-message row exercised in `FieldsMacroErrorSpec`.
- **SM3.** `sbt scalafmtCheckAll` green.
- **SM4.** `sbt docs/mdoc` green — every new `circe.md` /
  `cookbook.md` fence compiles.
- **SM5.** `JsonPrism.*Unsafe` bench numbers (a one-shot
  `sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*JsonPrismBench.*"`)
  do not regress outside JMH noise (roughly ±5 %) vs main HEAD
  pre-rename. Multi-field bench numbers and default Ior-surface
  numbers reported for reference (no regression gate — new surfaces).
- **SM6.** `.fields` reachable from a doc fence — the mdoc on
  `circe.md` is a witness that the macro binds cleanly in a
  documentation compilation unit, which has a distinct classpath
  from the tests.
- **SM7 (new).** The default-surface → `*Unsafe` rename is
  grep-complete: no call site in `core/`, `laws/`, `tests/`,
  `generics/`, `circe/`, `benchmarks/`, or `site/` references the
  old forgiving-shape `modify` / `transform` / `place` / `transfer`
  / `getOption` / `getAll` names in a context that now expects the
  Ior-bearing defaults. Automatable via `Grep` for the pre-rename
  names scoped to non-test contexts; test contexts must exercise
  both surfaces by design.

## Phased Delivery

Unit 0 → Unit 1 → Unit 2 → Unit 3 → Unit 4 → Unit 5 → Unit 6 → Unit 7 → Unit 8.
Each unit leaves the tree green.

1. **Unit 0 (spike).** NamedTuple codec derivation confirmation. One
   spike commit (or a throwaway branch whose result is discarded
   before the first real commit). Gate: if the spike succeeds,
   proceed. If it fails, re-scope to OQ1 fallback (a) before any
   further work.
2. **Unit 1 (rename + default Ior on single-field prism).** Breaking
   commit. `JsonFailure` ADT + `PathStep` visibility + rename-to-unsafe
   + new default Ior methods on `JsonPrism`. One commit. Bench retarget
   included in the same commit so the project remains buildable.
3. **Unit 2 (traversal rename + default Ior + new place/transfer).**
   One commit.
4. **Unit 3 (multi-field prism).** `JsonFieldsPrism[A]` + `.fields`
   macro + read/write codegen + default and `*Unsafe` surfaces on the
   new class. One commit.
5. **Unit 4 (multi-field traversal).** Sibling to Unit 3. One commit.
6. **Unit 5 (diagnostics).** Error-message specs. One commit. Can
   ship earlier if convenient — Units 3 / 4 already emit all D10
   messages.
7. **Unit 6 (laws).** PrismLaws wiring + forAll default-surface
   properties. One commit.
8. **Unit 7 (cross-carrier composition regression specs).** Five
   named chains across Lens / generics-lens / JsonPrism /
   JsonFieldsPrism / AffineFold / JsonTraversal. One commit. Gates
   the docs commit — docs reference these chains as canonical
   examples.
9. **Unit 8 (docs).** Migration notes + new sections + benchmark
   note. One commit.

Rough total: 8–9 commits (Unit 0 spike excluded if discarded), one
branch, landable as one PR. No CI matrix implications. Release
implications: bumps appropriate for a pre-1.0 breaking rename; handle
via the production-readiness plan's release checklist when it lands.

## Future Considerations

- **Nested-path selectors in `.fields`.** `codecPrism[Person].fields(
  _.address.street, _.name)` — a future macro pass that walks each
  selector chain recursively. Focus shape becomes a structured
  NamedTuple (possibly with nested NamedTuple values). Out of v1 — the
  flat multi-field macro already buys the primary use case.
- **`Selectable`-driven multi-field** (`codecPrism[Person].(name,
  age)`). Blocked on Scala-language change. Listed for completeness.
- **Per-field-independent update semantics.** A variant where the user
  provides `f: NT => NT` that updates a subset of focus fields, and
  only the updated fields are written back. Requires change-tracking
  at the NamedTuple level; deep-equality comparison per field; more
  complex failure model. This is also the feature that would let the
  "apply what's possible" rule (D4) cover the multi-field prism's
  partial-read case — by letting `f` receive a partial
  representation. Clear second-generation feature.
- **`JsonFailure.pathContext: Array[PathStep]` refinement.** OQ3's
  deferred question. Ship if user demand appears.
- **Traversal × `.fields` fusion.**
  `codecPrism[Basket].items.each.fields(_.name, _.price).andThen(…)`
  — today's `andThen` chains across the shared `Either` carrier; any
  fusion optimisation (merge the element walk with the field
  reassembly pass) lives on the `JsonFieldsTraversal` side. Measure
  first.
- **Per-step-typed `JsonFailure` refinements.** A type-level split
  that guarantees `DecodeFailed` doesn't appear in
  `place` returns. Marginal value; defer.
- **`modifyF[G]` + observable failure.** OQ5. Combine the effectful
  `.modifyF[G]` with Ior-bearing failure. Needs a `G`-aware
  failure accumulator (IorT style). Separate plan.
- **A dedicated `codecIso[S]` entry for hand-proven bijections.**
  An escape hatch for the "I know decode can't fail for this input
  domain" case — where the user owns both the Encoder and the input
  distribution. Low priority and orthogonal to `.fields`.
- **PrismLaws-for-Ior.** A new law class in `cats-eo-laws` that
  expresses the Prism laws against an Ior-bearing `modify` /
  `get` pair, so Unit 6's default-surface properties become law
  witnesses rather than ad-hoc ScalaCheck. Separate plan.
- **Make `JsonTraversal` extend `Optic`.** Today `JsonTraversal` is
  deliberately outside the Optic trait (see
  `site/docs/concepts.md` — "would have to invent an artificial
  `to`"). That's the reason Unit 7 scenarios (4) and (5) use
  manual composition (`outer.modify(jt.modify(f)(_))`) rather than
  native `.andThen`. If user demand surfaces for a native
  `.andThen` path, the design work is: (a) pick a carrier that
  fits the traversal's `(prefix, suffix)` storage — probably
  `Forget[Vector]` or a bespoke `JsonTraversalF` carrier; (b)
  supply `AssociativeFunctor` + `Composer[Tuple2, F]` + similar
  bridges; (c) decide whether the Ior-bearing `modify` fits the
  generic `ForgetfulFunctor[F]` / `ForgetfulTraverse[F, _]`
  machinery or requires a new capability typeclass. Non-trivial;
  earn it only when the manual-composition idiom documented in
  Unit 7 starts to feel like a limitation users actually hit.

## Sources & References

- `docs/plans/2026-04-23-003-feat-generics-multi-field-lens-plan.md`
  — structural analogue; mirrors sections, swaps the
  semantic analysis. D1 / D2 carveouts explain the asymmetry.
- `docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md`
  — format / length reference; motivates the `PrismLaws` wiring in
  Unit 6 and the CHANGELOG coordination.
- `circe/src/main/scala/eo/circe/JsonPrism.scala` — current
  single-field implementation, reference for the rename and the
  multi-field sibling class's hot-path idioms.
- `circe/src/main/scala/eo/circe/JsonTraversal.scala` — current
  traversal implementation, reference for Unit 2 / Unit 4.
- `circe/src/main/scala/eo/circe/JsonPrismMacro.scala` — existing
  macro patterns (`fieldImpl`, `eachImpl`, `extractFieldName`,
  `iterableElementType`), directly extended by the `.fields` macro.
- `circe/src/main/scala/eo/circe/PathStep.scala` — ADT whose visibility
  shifts in Unit 1.
- `core/src/main/scala/eo/optics/Optic.scala` — `Optic[S, T, A, B, F]`
  trait; `getOption` extension on `Affine`-carrier optics (untouched
  by this plan).
- `build.sbt` — `circeIntegration` module; `kindlingsCirce` Test-scope
  dep; `laws` LocalProject (new Test dep in Unit 6).
- Scala 3 reference — *Named Tuples*.
  <https://docs.scala-lang.org/scala3/reference/experimental/named-tuples.html>.
- SIP — *Named Tuples*.
  <https://github.com/scala/improvement-proposals/blob/main/content/named-tuples.md>.
- circe `Decoder` / `Encoder` API —
  <https://circe.github.io/circe/codecs/auto-derivation.html>.
- kindlings-circe-derivation — GitHub source
  <https://github.com/MateuszKubuszok/hearth> and cellar-verified
  symbols (`EncoderHandleAsNamedTupleRuleImpl`,
  `DecoderHandleAsNamedTupleRuleImpl`).
- cats `Ior` —
  <https://typelevel.org/cats/api/cats/data/Ior.html>.
- cats `Chain` —
  <https://typelevel.org/cats/api/cats/data/Chain.html>.
- Monocle multi-focus discussion —
  <https://www.optics.dev/Monocle/docs/optics>.
