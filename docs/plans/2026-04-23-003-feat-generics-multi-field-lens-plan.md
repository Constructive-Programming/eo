---
title: "feat: Generics multi-field Lens — varargs selectors with automatic Iso upgrade"
type: feat
status: active
date: 2026-04-23
---

# feat: Generics multi-field Lens — varargs selectors with automatic Iso upgrade

## Overview

Extend the `eo-generics` sub-project's `lens[S](...)` macro so it accepts a
**varargs list of single-field accessors** and returns an optic focusing the
entire selected set at once. The existing 1-selector form
(`lens[Person](_.age)`) keeps its current shape — a
`SimpleLens[S, A, XA]` with `XA` being the NamedTuple complement — but a
2-or-more-selector call switches to a new code path that:

- focuses a Scala 3 `NamedTuple` assembled from the selected fields in
  **selector order**, not declaration order;
- carries the non-focused fields as a `NamedTuple` complement in
  declaration order (the existing rule, just extended);
- automatically **upgrades to `Iso`** (concretely, `BijectionIso[S, S, T, T]`)
  when the selector set covers every field of the case class — the focus is
  still a NamedTuple in selector order, so both `lens[Person](_.name, _.age)`
  and `lens[Person](_.age, _.name)` are valid Isos with different focus shapes.

`transparent inline` at the partial-application call site absorbs the
return-type switch between Lens and Iso, so users keep the same entry point
regardless of arity.

Nested paths (`_.address.street`) remain out of scope — they need a distinct
macro pass (see Future Considerations).

## Problem Frame

`cats-eo`'s current `lens[S](_.field)` buys one Lens per call. To atomically
edit several fields today the user writes a `Lens[Person, (String, Int)]` by
hand — precisely the boilerplate the generics module was designed to erase.
There is no derivation path to `Iso` either: a user who wants the bijection
between `S` and its product-of-fields NamedTuple must reach for
`Iso.apply` and re-write `get` / `reverseGet`. And the surface is
asymmetric with `prism[S, A]`, which already recognises multiple shapes
and picks the right family per call.

The upstream brainstorm (inline 2026-04-23): extend `lens` to varargs, land
a NamedTuple focus, and automatically emit `BijectionIso` when the selector
set is total. Those are the choices this plan honours.

## Requirements Trace

- **R1. Varargs entry point.** `lens[S](f1, f2, ..., fn)` compiles for any
  `n ≥ 1`, each `f_i` being a single-field accessor `_.fieldX` on `S`.
- **R2. Arity-1 semantic parity on partial cover.** `lens[Person](_.age)`
  on a case class with more than one field still emits a
  `SimpleLens[Person, Int, XA]` with `XA` encoding the complement as a
  `NamedTuple` — the existing behaviour for the non-full-cover case.
  Byte-for-byte identity with today's output is NOT required (project is
  pre-release at `0.1.0-SNAPSHOT`; no downstream MiMa contract to
  preserve). The arity-1 full-cover case is an Iso per D2.
- **R3. Multi-field focus is a Scala 3 `NamedTuple` in selector order.**
  `lens[Person](_.age, _.name)` focuses `NamedTuple[("age", "name"),
  (Int, String)]`. Selector order is load-bearing — see D1.
- **R4. Multi-field complement is a `NamedTuple` in declaration order.**
  For `Person(name, age, email)` and `lens[Person](_.email, _.name)` the
  complement is `NamedTuple[("age",), (Int,)]` (declaration order among
  the non-focused fields).
- **R5. All-fields-selected case emits a `BijectionIso[S, S, T, T]`** where
  `T` is the selector-ordered NamedTuple. One- and multi-field full-cover
  both take this path — see D2 for the 1-field carveout.
- **R6. `transparent inline` propagates the concrete result type.** The
  entry-point signature declares a lower-bound-of-useful-intersection
  return type (precise shape resolved in D7); both arms pick up `.modify`,
  `.replace`, `.get` through existing typeclass instances.
- **R7. Compile-time diagnostics for every failure mode** — empty
  varargs, duplicate selectors, non-field selectors, unknown field names,
  non-case-class `S`. See D6 for exact message catalogue.
- **R8. Tests cover every supported shape** — 1/2/3/N-field case classes,
  enum case records (currently supported by the 1-field path), recursive
  parameterised ADTs (as for 1-field), full-cover Iso round-trip, and
  every failure-mode diagnostic.
- **R9. Downstream `site/docs/generics.md` reflects the new syntax**, with
  mdoc-verified examples for the 2-field, full-cover-Iso, and
  duplicate-rejection cases.
- **R10. No regression in 1-field perf.** The 1-field path is physically
  unchanged; bench numbers on `benchmarks/src/main/scala/eo/bench/OpticsBench.scala`
  must match main within noise.

## Scope Boundaries

In scope:

- `generics/src/main/scala/eo/generics/LensMacro.scala` — extend to
  varargs, add multi-field codegen, add full-cover Iso codegen.
- `generics/src/main/scala/eo/generics/package.scala` — adjust
  `PartiallyAppliedLens[S]` (or add a companion partial-application
  class) so the varargs entry point type-checks.
- `generics/src/test/scala/eo/generics/GenericsSpec.scala` — add
  property tests for the new shapes. Sample ADTs land under
  `generics/src/test/scala/eo/generics/samples/` to keep outer-accessor
  issues at bay.
- `site/docs/generics.md` — add a "Multi-field Lens / Iso" section.
- `CLAUDE.md` — one-line mention in the auto-derivation section.

Out of scope (explicit non-goals):

- **Nested paths.** `lens[Person](_.address.street, _.name)` is a separate
  macro pass (needs recursive selector parsing + field-name disambiguation
  across record levels). Tracked in Future Considerations.
- **Polymorphic varargs.** `lens[Person](_.name)` today is monomorphic
  (`S = T`, `A = B`); the multi-field path stays monomorphic. A
  type-changing `plens[S, T](…)` variant is a separate, lower-priority
  request.
- **`iso[S, T]` as a standalone entry.** The Iso emerges **automatically**
  from total coverage; we do not add a top-level `iso` macro in this plan.
  If demand materialises later, a `iso[S](_.f1, _.f2, …)` shim that
  errors on partial cover is trivial — but it would duplicate the check
  already performed here, so we defer until evidence shows a standalone
  entry is wanted.
- **Generalising to tuples (plain `Tuple`, not `NamedTuple`)** as the
  focus. Scala 3's NamedTuple is opaque-aliased over its `Values` tuple,
  so users who want the plain-tuple shape can already erase names with
  `.toTuple`; we do not add a second "no-names" code path.
- **Law-class additions.** The new shape is still a Lens (or Iso); the
  existing `LensLaws` / `IsoLaws` cover it. No new law file in this plan.

## Context & Research

### Relevant Code and Patterns

- `generics/src/main/scala/eo/generics/LensMacro.scala` — 1-field
  macro. `LensMacro.derive[S, A]` is the `transparent inline` entry;
  `HearthLensMacro` uses `CaseClass.parse[S]` + `caseFieldValuesAt` +
  `construct[Id]`; `extractFieldName(t: Term)` parses the selector
  lambda; `buildLens` assembles `namesTpe` + `valuesTpe` via
  `TypeRepr.of[*:].appliedTo(...)`, folded into
  `NamedTuple.NamedTuple`, then emits `split` via
  `Expr.ofTupleFromSeq` + `asExprOf[xa]` and `combine` via
  `cc.construct[Id]` + `.asInstanceOf[Tuple].apply(i).asInstanceOf[t]`
  index reads. Output is `SimpleLens[S, A, xa]`.
- `generics/src/main/scala/eo/generics/package.scala` —
  `PartiallyAppliedLens[S]` with one `transparent inline apply[A]`
  method delegating to `LensMacro.derive`.
- `core/src/main/scala/eo/optics/Lens.scala` — `SimpleLens[S, A, XA]`
  + `transformEvidence` given. Fused `GetReplaceLens.andThen` and
  friends rely on the concrete subclass surviving inference, not
  just `Optic[…]`.
- `core/src/main/scala/eo/optics/Iso.scala` — `BijectionIso[S, T, A,
  B]` with `get` / `reverseGet`. Ships fused `andThen` overloads, so
  the emitted Iso picks up composition speedups automatically.
- `generics/src/test/scala/eo/generics/GenericsSpec.scala` — specs2 +
  ScalaCheck property tests using `forAll`; sample ADTs live under
  `eo.generics.samples.*` because macro-emitted `new T(...)` calls
  trip on inner-class outer accessors. No `cats-eo-laws` dep.

### External References

- Scala 3 NamedTuple reference:
  <https://docs.scala-lang.org/scala3/reference/experimental/named-tuples.html>.
  Opaque alias `type NamedTuple[N <: Tuple, V <: Tuple] = V`;
  structural-refinement named access; singleton-String names.
- Scala SIP — Named Tuples:
  <https://github.com/scala/improvement-proposals/blob/main/content/named-tuples.md>.
- Scala 3 Macros guide (Varargs + transparent inline + quotes.reflect):
  <https://docs.scala-lang.org/scala3/guides/macros/macros.html>.
- Hearth macro-commons: <https://github.com/MateuszKubuszok/hearth>.
- Monocle multi-focus discussion: <https://github.com/optics-dev/Monocle/issues/1133>.

### Institutional Learnings

- `docs/solutions/` does not yet exist; anything surprising about
  NamedTuple-under-`transparent inline` lands there once observed.
- `LensMacro` comments flag `scala.quoted.Expr.ofTupleFromSeq` as
  intentionally fully qualified to avoid shadowing by Hearth's own
  `Expr`. Multi-field codegen keeps the same spelling.

## Key Technical Decisions

### D1. Selector order = focus NamedTuple order; declaration order = complement order

`lens[Person](_.age, _.name)` on `Person(name, age)` focuses
`NamedTuple[("age", "name"), (Int, String)]`. The user's selector order
is observable at both the type level (`Names` tuple) and runtime
(storage order). The complement NamedTuple uses declaration order
among non-selected fields — the only defensible choice given there is
no user input for that axis, and it matches today's 1-field convention.

### D2. Full-cover is always an Iso, including the 1-field case

Total coverage (selector set ≡ field set) emits `BijectionIso[S, S, T,
T]` with `T` the selector-ordered NamedTuple, uniformly for N = 1, 2,
… all fields.

**Behaviour change for `Wrapper(value: Int)`.** `lens[Wrapper](_.value)`
today returns `SimpleLens[Wrapper, Int, NamedTuple[EmptyTuple,
EmptyTuple]]`; under this plan it returns
`BijectionIso[Wrapper, Wrapper, NamedTuple[("value",), (Int,)], …]`.
That is a return-type change. Justification: consistency > backward-compat
at `0.1.0-SNAPSHOT`. The new shape is strictly more useful (name-access
on focus, Forgetful-carrier fast path, round-trip via `reverseGet`).
Called out in CHANGELOG (see Documentation Plan); no deprecation because
there is no prior public release to deprecate from. Rejected alternative:
"1-field stays Lens, Iso triggers only at N ≥ 2" — the "total ⇒ Iso"
rule is easier to teach.

### D3. Duplicate selectors are a compile error

`lens[Person](_.name, _.name)` aborts macro expansion with a message
naming the duplicate. Silent dedupe hides copy-paste bugs; warn-then-dedupe
still mutates emitted code. Fail-fast is zero-cost and gives a sharp
signal. Users who want a deliberately duplicated projection can reach
for `Iso.apply` with an explicit NamedTuple literal.

### D4. `reverseGet` reshuffles via name-indexed NamedTuple lookup

When the Iso focus is not in declaration order, the generated
`reverseGet`, for each declaration-order parameter, (1) computes that
field's selector-order position as a compile-time `Int`, (2) emits
`focus.asInstanceOf[Tuple].apply(i).asInstanceOf[FieldType]`, (3) threads
through `cc.construct[Id]` at the matching parameter position. Runtime
cost is one erased `asInstanceOf[Tuple]` cast plus N boxed reads —
identical shape to today's complement read pattern. We reuse the
NamedTuple-opaque-over-Tuple relation the 1-field macro already relies
on; a hypothetical tuple-level `Runtime.namedTupleLookup` is not
available in Scala 3.8.x and positional reads are correct by
construction.

### D5. Hearth reuse: identical infrastructure, new selector-parsing code

Reused verbatim from the 1-field macro: `CaseClass.parse[S]` (case-class
validation), `cc.caseFields` / `sTpe.typeSymbol.caseFields` (declaration
order + per-field types), `cc.construct[Id]` (primary-constructor
invocation, now deciding per-parameter whether to read from focus or
complement), `scala.quoted.Expr.ofTupleFromSeq` (tuple packing).

New macro-local code: (a) varargs-list parser that walks
`Expr[Seq[S => Any]]` / `Varargs(...)` and runs the existing
`extractFieldName` per element; (b) validation pass (non-empty, no
duplicates, all names known); (c) focus-side NamedTuple type
construction mirroring the existing complement path; (d) Iso-arm
codegen emitting `BijectionIso.apply` with macro-generated `get` /
`reverseGet`.

### D6. Error-message catalogue

All messages prefixed `lens[${Type.prettyPrint[S]}]:` for grep-ability,
matching the existing 1-field pattern.

| Condition | Message |
|-----------|---------|
| Empty varargs | "requires at least one field selector." |
| Non-case-class `S` | Reuses Hearth's `CaseClass.parse` `Left(reason)`. Same wording as today. |
| Selector not a `_.field` | "selector at position $i must be a single-field accessor like `_.fieldName`. Nested paths (e.g. `_.a.b`) are not yet supported. Got: ${selectorTerm.show}" |
| Unknown field | "'$fieldName' is not a field of $S. Known fields: ${knownFields.mkString(\", \")}" (same as today) |
| Duplicate selectors | "duplicate field selector '$fieldName' at positions $i, $j. Each field may appear at most once." |

The empty-varargs case is rejected on principle — even though a
0-field NamedTuple-focus Iso is technically derivable as the constant
Iso `S ≅ EmptyTuple`, it is not a useful artifact and the accidental
`lens[S]()` is far more likely than the deliberate one.

### D7. Single varargs `apply` on `PartiallyAppliedLens[S]`; no back-compat overload

One `transparent inline` entry point handles every arity, including
N = 1. Shape:

```text
transparent inline def apply(
    inline selectors: (S => Any)*
): Optic[S, S, ?, ?, ?]
```

The macro body reads the inline `selectors` list (via
`scala.quoted.Varargs.unapply` or a direct term walk), dispatches on
arity and cover status:

  - N = 0 → compile error (R7, D6).
  - N = 1, partial cover → emit `SimpleLens[S, A, XA]` — the existing
    single-field codegen path, unchanged structurally.
  - N = 1, full cover (1-field case class) → emit
    `BijectionIso[S, S, T, T]` per D2.
  - N ≥ 2, partial cover → emit multi-field `SimpleLens` with
    NamedTuple focus + NamedTuple complement.
  - N ≥ 2, full cover → emit `BijectionIso[S, S, T, T]` with
    NamedTuple focus.

`transparent inline` refines the return type to the concrete subclass
the splice emits, so chained `.andThen` picks up the fused
concrete-subclass overloads (`SimpleLens.andThen(*)`,
`BijectionIso.andThen(*)`).

**Why not two overloads.** An earlier draft proposed a separate
1-arg `apply[A](inline selector: S => A)` beside the varargs arm to
preserve byte-for-byte parity with today's single-field signature.
That parity is not required — the project is pre-release at
`0.1.0-SNAPSHOT`, no MiMa contract binds us — and the unified
entry removes a whole class of overload-resolution risk
(previously flagged as OQ1). The 1-selector semantic contract is
preserved by the N=1 arm inside the macro body; the surface just
has one `apply` symbol instead of two.

### D8. Emit stock `BijectionIso`, not a new subclass

The full-cover Iso emits `BijectionIso[S, S, T, T]` directly — no new
`NamedTupleIso` subclass. Rationale: `BijectionIso` already ships the
fused `andThen` overloads we need; a derivation-origin subclass
carries no call-site behaviour change and would create a MiMa story
later.

## Open Questions

Honest uncertainties; flagged for resolution during Unit 1 or earlier.

- **OQ1. `transparent inline` + varargs return-type refinement.** The
  single varargs entry point (D7) declares a broad return type
  (`Optic[S, S, ?, ?, ?]`) but relies on `transparent inline` to refine
  to `SimpleLens[...]` or `BijectionIso[...]` at each call site so the
  downstream fused-`andThen` overloads pick the right arm. This
  combination — `transparent inline` on a varargs method whose splice
  emits arity-dependent concrete types — is not a pattern exercised
  elsewhere in cats-eo. Verify with a smoke test in Unit 1 before
  building out the codegen. Fallback: split into two named entry
  points (`lens[S]` kept as-is, new `lensN[S]` for multi-field).
- **OQ2. Focus-side NamedTuple synthesis via `quotes.reflect`.** The
  complement path proves the opaque-subtype
  `Values <: NamedTuple[Names, Values]` relation and
  `asExprOf[xa]` work for narrowing a plain tuple. We need the
  pattern to hold for reading a NamedTuple value in the
  Iso-arm `reverseGet` — the 1-field macro already does this for
  complement reads, so the shape is proven, but I have not run the
  Iso-lambda variant. Fallback: compose
  `focus.toTuple.apply(i).asInstanceOf[Ti]` chains (opaque-subtype
  accepts this by construction).
- **OQ3. `Varargs(...)` extractor on `inline` varargs.** Unverified
  whether `scala.quoted.Varargs.unapply` de-sugars an inline
  `Expr[Seq[S => Any]]` to a `List[Expr[S => Any]]` in 3.8.3.
  Fallback: manual `asTerm` walk through
  `Inlined(_, _, Typed(Seq(...), _))` — robust but uglier.
- **OQ4. Downstream pinning of 1-field Wrapper return type (D2).** No
  telemetry; presumed zero external callers at 0.1.0-SNAPSHOT. No code
  mitigation beyond the CHANGELOG entry.
- **OQ5. scalafmt on varargs-of-lambdas.** Trailing commas are desirable
  for diffability; unverified that 3.11.0 with our config accepts all
  spellings. Resolve during Unit 5/6 if it surfaces.

## High-Level Technical Design

> *Directional guidance for reviewers. The implementing agent should
> treat this as context, not code to reproduce.*

### Control flow at macro expansion

```text
lens[S](f1, …, fn)
   │
   ▼  PartiallyAppliedLens[S].apply   [transparent inline]
   ▼  LensMacro.deriveMultiImpl
   ▼  parse varargs → names (selector order)
   ▼  validate (non-empty / no duplicates / known fields)
   │
   ▼  names == caseFields(S) ?
        ├─ yes → buildMultiIso   (→ BijectionIso[S, S, Focus, Focus])
        └─ no  → buildMultiLens  (→ SimpleLens[S, Focus, Complement])
```

### Type + codegen layout

- `focusNamesTpe`, `focusValuesTpe` — right-folds over selector-ordered
  names / types using `TypeRepr.of[*:].appliedTo(...)`.
- `focusTpe` = `NamedTuple.NamedTuple[focusNamesTpe, focusValuesTpe]`.
- `complementNamesTpe` / `complementValuesTpe` — same construction,
  restricted to declaration-order non-selected fields (this already
  exists in 1-field `buildLens`).
- **Lens arm (`split` / `combine`):** both tuples packed via
  `Expr.ofTupleFromSeq` and narrowed with `asExprOf`; `combine`
  threads per-parameter focus/complement reads through
  `cc.construct[Id]` using the name-indexed lookup.
- **Iso arm (`get` / `reverseGet`):** `get` packs the focus from
  `Select.unique(sTerm, name)` reads in selector order; `reverseGet`
  reads each declaration-order parameter at its selector-order index
  via `focus.asInstanceOf[Tuple].apply(i).asInstanceOf[Ti]` and
  threads through `cc.construct[Id]`.

Both arms share `cc.construct[Id]`, which already works for recursive
/ parameterised / enum-case records via the 1-field macro — no new
tests beyond the multi-selector generalisations in Unit 5.

### `PartiallyAppliedLens[S]` shape

```text
final class PartiallyAppliedLens[S]:
  transparent inline def apply(
      inline selectors: (S => Any)*
  ): Optic[S, S, ?, ?, ?] = …
```

One entry point. The macro body dispatches on arity + cover status
per D7. `Optic[S, S, ?, ?, ?]` is the narrowest cats-eo supertype
spanning both concrete arms (`SimpleLens[...]` and
`BijectionIso[S, S, ..., ...]`); `transparent inline` refines at
each call site so downstream `.andThen` picks the right fused
overload. If OQ1 reveals the `transparent inline` + varargs
refinement doesn't propagate, the fallback is two named entry
points (`lens[S]` + `lensN[S]`).

## Implementation Units

- [x] **Unit 1: Varargs entry point + arity detection**

  **Goal:** Multi-selector calls compile end-to-end, dispatch to a
  new macro entry, and reject invalid varargs. No codegen yet — the
  splice emits a deliberate `"multi-field codegen not yet wired"`
  error so the plumbing can be reviewed in isolation.

  **Files:**
  - Modify: `generics/src/main/scala/eo/generics/package.scala` (replace
    `PartiallyAppliedLens[S].apply` with the single varargs entry per D7;
    keep the existing N=1 codegen path reachable from the unified macro
    body).
  - Modify: `generics/src/main/scala/eo/generics/LensMacro.scala` (new
    `deriveN[S](selectors: List[Expr[S => Any]])` entry that dispatches
    on arity + cover; keep existing `deriveImpl` machinery reusable for
    the N=1 partial-cover arm).

  **Approach:** `LensMacro.deriveMulti[S](selectorsExpr: Expr[Seq[S =>
  Any]]): Expr[Optic[S, S, ?, ?, ?]]`. Use `Varargs.unapply` (OQ3
  fallback if needed) to recover per-selector expressions. Run
  `extractFieldName` per element, accumulate `(index, name)` pairs,
  validate against `CaseClass.parse[S].caseFields`. Emit D6
  diagnostics for each failure. For this unit, the success arm aborts
  with the Unit 2 stub.

  **Execution note:** test-first. Write the MacroErrorSpec skeleton
  first; each D6 message gets a failing typecheck assertion; fill in
  the macro until all pass.

  **Verification:** `sbt generics/compile` green; scratch
  `lens[Person](_.name, _.age)` surfaces the stub error; each D6
  diagnostic lands in a
  `scala.compiletime.testing.typeCheckErrors`-based
  `MacroErrorSpec.scala`.

  **Test scenarios:** empty varargs; duplicate selector; unknown field
  name; non-field selector (`_.name.toUpperCase`); non-case-class `S`.

- [x] **Unit 2: N-field Lens codegen (partial cover)**

  **Goal:** `lens[S](_.f1, _.f2, …)` with `k < fieldCount(S)` compiles
  to `SimpleLens[S, Focus, Complement]` with Focus in selector order,
  Complement in declaration order.

  **Files:** Modify `generics/src/main/scala/eo/generics/LensMacro.scala`
  (add `buildMultiLens`).

  **Approach:** construct `focusTpe` and `complementTpe` via the same
  `TypeRepr.of[*:].appliedTo` folds the 1-field macro uses. Emit
  `split: S => (complementTpe, focusTpe)` via two
  `Expr.ofTupleFromSeq` calls + `asExprOf` narrows. Emit `combine:
  (complementTpe, focusTpe) => S` through `cc.construct[Id]`, per
  parameter looking up focus vs complement name indexes and emitting
  the matching `asInstanceOf[Tuple].apply(i).asInstanceOf[t]` read.
  Wrap as `SimpleLens[S, focusTpe, complementTpe](…)`. Replace Unit
  1's stub with `if selectedCount < caseFieldCount then buildMultiLens
  else <Unit 3>`.

  **Execution note:** ship Unit 5's Lens-arm tests in the same commit
  as self-verification.

  **Verification:** 2-/3-field partial-cover tests pass; 1-field tests
  unchanged.

  **Test scenarios:** 2-of-3 cover; 2-of-4 selector-order ≠
  declaration-order; recursive ADT `Tree.Branch` partial cover;
  multi-field enum case.

- [x] **Unit 3: Iso detection + `BijectionIso` codegen (full cover)**

  **Goal:** total-coverage calls emit `BijectionIso[S, S, T, T]`; N =
  1 is included (D2).

  **Files:** Modify `generics/src/main/scala/eo/generics/LensMacro.scala`
  (add `buildMultiIso`).

  **Approach:** detect coverage as `selectedNames.toSet ==
  declarationNames.toSet && sizes match` (cheap; folds into the
  duplicate check). Emit `get: S => focusTpe` via `Expr.ofTupleFromSeq`
  in selector order, narrow with `asExprOf[focusTpe]`. Emit
  `reverseGet: focusTpe => S` via `cc.construct[Id]`: per
  declaration-order parameter, compute selector-order index, emit
  `focus.asInstanceOf[Tuple].apply(i).asInstanceOf[Ti]`. Wrap as
  `BijectionIso[S, S, focusTpe, focusTpe](get, reverseGet)`. Branch
  `deriveMulti`: full cover ⇒ `buildMultiIso`, partial ⇒
  `buildMultiLens`.

  **Verification:** Iso round-trip tests pass including selector-order
  variant (`lens[Pair](_.snd, _.fst)`).

  **Test scenarios:** 2-of-2 selector-order Iso; 3-of-3 in declaration
  order; 3-of-3 reversed; 1-field Iso (D2 witness); `reverseGet ∘ get
  = identity` on each.

- [ ] **Unit 4: Error diagnostics hardening**

  **Goal:** every D6 row has an exact-message test.

  **Files:** `generics/src/test/scala/eo/generics/MacroErrorSpec.scala`
  (created in Unit 1; expand here).

  **Approach:** one spec per D6 row using
  `scala.compiletime.testing.typeCheckErrors`. Assert the
  `lens[${Type.prettyPrint[S]}]:` prefix plus the diagnostic-specific
  substring. Preserve the existing 1-field diagnostic wording for
  known-field / unknown-field / non-field-accessor messages (R2
  semantic parity).

  **Verification:** `sbt generics/test` green.

  **Test scenarios:** every D6 row.

- [ ] **Unit 5: Behaviour property tests**

  **Goal:** three Lens laws on derived multi-field Lenses; three Iso
  laws (`get ∘ reverseGet = id`, `reverseGet ∘ get = id`, modify
  commutativity) on derived full-cover Isos.

  **Files:** Modify `generics/src/test/scala/eo/generics/GenericsSpec.scala`
  (new sections "Multi-field Lens derivation" and "Full-cover Iso
  derivation"); add samples under
  `generics/src/test/scala/eo/generics/samples/` if the existing
  `Person` / `Employee` don't cover selector-order scrambling.

  **Approach:** ScalaCheck `forAll` properties covering (a) focus
  NamedTuple field-name access (singleton-String names land), (b)
  Lens laws across 2-of-3 and 2-of-4 with selector-order ≠
  declaration-order, (c) Iso laws including the 1-field witness
  (D2), (d) composition smoke on one Iso → Lens chain to verify the
  fused `BijectionIso.andThen(GetReplaceLens)` overload fires.

  **Verification:** `sbt generics/test` green.

  **Test scenarios:** 2-field Lens vs Iso split; 3-field scrambled;
  4-field scrambled; recursive ADT partial cover; 1-field Iso.

- [ ] **Unit 6: Documentation**

  **Goal:** `site/docs/generics.md` and `CLAUDE.md` reflect the new
  shape; every new doc fence is mdoc-verified.

  **Files:**
  - Modify `site/docs/generics.md` — add "Multi-field Lens" and
    "Full-cover Iso" subsections after the existing `lens[S](_.field)`
    section.
  - Modify `CLAUDE.md` — refresh the "Product-type Lens (GenLens-style
    partial application)" snippet to demonstrate both single- and
    multi-field forms.
  - No CHANGELOG yet (`CHANGELOG.md` lands as part of the 0.1.0
    release plan; until then, plan-doc links in `docs/plans/` are the
    record of record).

  **Approach:** mdoc fences for `val nameAndAge = lens[Person](_.name,
  _.age)` (show `.name` / `.age` field-name access), `val pairIso =
  lens[Pair](_.fst, _.snd)` (exercise `.get`, `.reverseGet`,
  `reverse`), and a `scala mdoc:fail` fence for duplicate-selector
  rejection.

  **Verification:** `sbt docs/mdoc` green; `scalafmtCheckAll` green.

## Risks & Dependencies

- **R1 (medium).** `transparent inline` + varargs return-type
  refinement (OQ1) may not propagate the concrete result type
  through to the call site. Mitigation: detect during Unit 1's
  scratch test; if broken, fall back to two named entry points
  (`lens[S]` retained one-selector + `lensN[S]` varargs) so each
  entry carries a clean `transparent inline` signature.
- **R2 (low).** NamedTuple focus-side type synthesis (OQ2)
  unexpectedly fails in Scala 3.8.3. Mitigation: the complement
  path already proves the roundtrip works; the focus-side reuses
  the identical `TypeRepr.of[scala.NamedTuple.NamedTuple].appliedTo`
  call. Observed deviations land as a note in `docs/solutions/`.
- **R3 (low).** `Varargs` extractor on `inline` varargs (OQ3)
  returns `None`. Mitigation: switch to manual `asTerm` parsing
  per OQ3's fallback.
- **R4 (low).** Behaviour change on 1-field Wrapper (D2) surprises
  downstream. Mitigation: loud CHANGELOG entry; no internal callers
  to migrate (verified in Unit 1 by grepping the codebase — the
  1-field-Wrapper pattern isn't used anywhere currently).
- **R5 (medium).** Fused `andThen` overloads on `BijectionIso` /
  `GetReplaceLens` don't fire on the new multi-field `SimpleLens`
  outputs because Scala overload resolution picks the generic
  `Optic.andThen` when the concrete type isn't visible. Mitigation:
  `transparent inline` on the partial-application entry should
  preserve the concrete return type; Unit 5's composition
  spot-check verifies it does.

**Dependencies:** none outside the repo. No new sbt plugins, no
new Maven dependencies, no `cats-kernel` additions.

## Documentation Plan

- **`site/docs/generics.md`** — new subsection "Multi-field Lens
  and full-cover Iso" immediately after the existing single-field
  section. Three mdoc blocks: 2-field Lens, full-cover Iso,
  duplicate-selector compile error (mdoc:fail). Also a one-line
  pointer to the "Nested paths" Future Considerations from this
  plan so readers don't expect `_.address.street` to work.
- **`site/docs/optics.md`** (only if it has a GenLens-style
  example) — add a cross-link to generics.md's new subsection.
  Low priority; confirm during Unit 6 whether optics.md already
  points at generics.md.
- **`CLAUDE.md`** — refresh the "Product-type Lens" snippet so
  the agent-facing guide teaches the new syntax.
- **Scaladoc on `LensMacro.derive` and `deriveMulti`** — cover
  the NamedTuple focus shape, selector-order semantics, and
  full-cover-⇒-Iso rule. The existing `LensMacro` object doc is
  the reference for density.
- **`CHANGELOG.md`** — one "Changed" entry if/when the file
  exists; otherwise coordinate with the production-readiness plan
  that introduces it.

## Success Metrics

- **SM1.** `sbt compile` green on all four root-aggregate sub-projects.
- **SM2.** `sbt test` green; `generics/test` gains ≥ 10 new property
  tests (one per test scenario in Units 4 and 5) and all pass.
- **SM3.** `sbt scalafmtCheckAll` green.
- **SM4.** `sbt docs/mdoc` (or site sub-project equivalent) green —
  every new doc fence compiles.
- **SM5.** Multi-field Lens benchmark — even a single-fork smoke
  (`sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 .*LensBench.*"`) produces
  numbers that don't obviously regress on the 1-field path. A full
  bench sweep isn't required for this plan; spot-check only.
- **SM6.** A grep of `cats-eo`'s own codebase for
  `lens[\w+\]\(_.` followed by multiple selectors yields at least
  one genuine use of the new form (likely in `site/docs/`
  examples) — evidence the new surface is reachable, not just
  reachable by tests.

## Phased Delivery

Unit 1 → Unit 2 → Unit 3 → Unit 4 → Unit 5 → Unit 6. Each unit
leaves the tree green; no dependency jumps a unit.

1. **Unit 1 (plumbing).** Entry point, arity dispatch, stub
   splice. One commit.
2. **Unit 2 (Lens arm).** Partial-cover codegen. One commit.
   Ship Unit 5's Lens-arm tests together so the unit is
   self-verifying.
3. **Unit 3 (Iso arm).** Full-cover codegen. One commit. Ship
   Unit 5's Iso-arm tests together.
4. **Unit 4 (diagnostics).** Error-message tests. One commit. Can
   ship earlier if convenient — Unit 1 already emits all D6
   messages.
5. **Unit 5 (remaining tests).** Any laws / composition spot-checks
   not already shipped alongside Units 2 / 3. One commit.
6. **Unit 6 (docs).** `site/docs/generics.md` + `CLAUDE.md`. One
   commit.

Rough total: 5–6 commits, one branch, landable inside a single
PR. No CI matrix implications; no release implications at 0.1.0.

## Future Considerations

- **Nested paths.** `lens[Person](_.address.street, _.name)` needs a
  separate macro pass that walks each selector chain and recursively
  invokes Hearth construct at every record level; focus and
  complement become structured (nested NamedTuples), which changes the
  type-synthesis story materially. Worth its own plan post-0.1.0 once
  the flat-multi-field macro has ridden a release. Until then, users
  chain manually: `lens[Person](_.address).andThen(lens[Address](_.street))`.
  That's why the nested-path plan is a genuine feature, not just an
  ergonomic shortcut — the manual chain has no NamedTuple focus.
- **Polymorphic multi-field Lens.** A type-changing `plens[S, T](…)`
  threading an `S => T` refactor through combine — needs Hearth's
  polymorphic construct path and is substantially more work than the
  monomorphic case handled here.
- **Dedicated `iso[S](...)` entry.** A shim that errors on partial
  cover; zero expressive gain, modest readability benefit. Add if
  users reach for it.
- **NamedTuple destructuring.** `val (a, b) = namedTuple` via
  `.toTuple` — document in cookbook once shipped.
- **Law classes for derived shapes.** Existing `LensLaws` / `IsoLaws`
  cover the derivations transparently; if macro-specific invariants
  emerge (e.g. "selector-order roundtrip"), those would land as a
  `GenericsLensLaws` follow-up — not this plan.

## Sources & References

- Scala 3 reference — *Named Tuples*.
  <https://docs.scala-lang.org/scala3/reference/experimental/named-tuples.html>.
- SIP — *Named Tuples*.
  <https://github.com/scala/improvement-proposals/blob/main/content/named-tuples.md>.
- Scala 3 Macros guide — *Varargs, transparent inline, quotes.reflect*.
  <https://docs.scala-lang.org/scala3/guides/macros/macros.html>.
- Hearth macro-commons — `CaseClass.parse[S]`,
  `caseFieldValuesAt`, `construct[Id]`.
  <https://github.com/MateuszKubuszok/hearth>.
- Monocle multi-focus discussion thread.
  <https://github.com/optics-dev/Monocle/issues/1133>.
- `cats-eo` internal — `generics/src/main/scala/eo/generics/LensMacro.scala`
  (existing 1-field macro, reference implementation).
- `cats-eo` internal — `generics/src/main/scala/eo/generics/PrismMacro.scala`
  (reference for Hearth-backed sum-type macro).
- `cats-eo` internal — `core/src/main/scala/eo/optics/Lens.scala`,
  `core/src/main/scala/eo/optics/Iso.scala` (target concrete
  subclasses the macro emits).
- `cats-eo` internal — the 2026-04-17 production-readiness plan
  (downstream `LensLaws` / `IsoLaws` that cover the derived
  instances at law level).
