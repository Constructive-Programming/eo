---
title: "feat: Benchmark-suite unification — one canonical schema, real-world coverage"
type: feat
status: active
date: 2026-06-06
---

# feat: Benchmark-suite unification — one canonical schema, real-world coverage

## Overview

The JMH harness has grown organically to 16 files / ~2,000 lines across three
generations of the library (AlgLens → Grate/Kaleidoscope → MultiFocus). It works,
but it has three structural problems and several coverage gaps:

1. **Fixture sprawl.** At least three distinct `Person`, two byte-identical
   `Deep3 → Deep2 → Deep1 → Atom("Alice")` trees (circe and avro each define
   their own), plus `L1/L2/L3`, `Basket/Item`, `Payload/User/Profile`, and
   `Company/Department/Employee`. No shared schema, so numbers across backends
   are *not* apples-to-apples.
2. **A junk-drawer bench.** `MultiFocusBench` (238 lines) absorbed the deleted
   Grate/Kaleidoscope/PowerSeries benches; `eoModify_psVecEach` literally
   re-benches `PowerSeriesBench.eoModify_powerEach`, and it carries two
   `@Setup(Level.Iteration)` methods on one class.
3. **Boilerplate.** The 6-annotation JMH preamble is copy-pasted onto all 15
   bench classes (the `JmhDefaults` walk-back note explains why a `trait` can't
   carry it; see *Open question 1* for a real fix).

And the gaps — things the library is *about* that nothing measures:

- **Cross-carrier `.andThen`** (Tuple2 ↔ Affine ↔ Either ↔ MultiFocus). This is
  EO's signature feature; today it's only exercised incidentally inside other
  benches, never isolated.
- **One realistic document benched identically across all three byte/AST
  backends** (circe / jsoniter / avro) plus the naive, native, and
  Monocle-on-decoded baselines. The integration benches each tell a partial
  story (deep-narrow, wide-flat, array) on a *different* fixture.
- **Optic construction / amortization cost.** Every bench builds optics outside
  the loop (correct for steady state) — but nothing measures what
  `codecPrism[X].field(_.a).field(_.b)` costs to build, which matters for
  one-shot use.
- **`generics` auto-derivation runtime parity.** The macro emits `new S(...)`
  setters; nothing confirms they match a hand-written `copy` at runtime.

This plan unifies the suite onto **one canonical realistic domain model**,
adds the missing real-world and composition benches, and trims the dead
duplication — while keeping JMH out of the root aggregate exactly as today.

## Design decisions (settled)

- **One canonical schema.** A single `Order` event (deep + wide + array +
  optional) drives every integration bench and the core micro-benches that can
  use it. (See *Canonical schema* below.)
- **Three baselines for real-world benches**, side-by-side in one report row
  per metric:
  - `naive*` — hand-written decode → copy-chain → encode (what you'd write
    without optics).
  - `native*` — the backend's own idiomatic in-place path (circe `ACursor`,
    jsoniter codec, Avro `IndexedRecord`) without EO.
  - `monocle*` — decode to the case class, run a Monocle optic, re-encode
    (a general optics library that has *no* byte/AST carrier — this is where
    EO's structural-edit advantage shows).
  - `eo*` — the EO optic (both `*Unsafe` and `Ior`-bearing where they differ).
- **Keep the Monocle-paired micro-benches.** `LensBench`/`PrismBench`/`IsoBench`/
  `TraversalBench`/`Getter`/`Setter`/`Fold`/`Optional`/`AffineFold` stay — they
  are the per-call-overhead story and remain valid. They migrate onto the
  canonical fixture where it fits, but their *shape* is unchanged.

## Canonical schema

One realistic e-commerce `Order` event. Deep (`customer.address.street`,
depth 3), wide (`Order` ~5 fields, `LineItem` ~5 fields), arrayed
(`lines: Vector[LineItem]`), and optional (`customer.loyaltyId`).

```scala
final case class Address(street: String, city: String, zip: String, country: String)
final case class Customer(
    name: String,
    email: String,
    tier: String,
    loyaltyId: Option[String],   // Optional / AffineFold focus
    address: Address,            // depth-2 product
)
final case class LineItem(
    sku: String,
    name: String,                // Traversal focus (lines[*].name)
    qty: Int,
    price: Double,               // Fold focus (sum lines[*].price)
    tags: Vector[String],
)
final case class Order(
    id: Long,                    // depth-1 scalar (Lens/Getter/Setter, jsoniter $.id)
    currency: String,
    region: String,
    customer: Customer,          // path to depth-3 street
    lines: Vector[LineItem],     // array traversal / fold
)
```

Foci this single schema unlocks:

| Optic family | Focus | JSONPath (jsoniter) |
|---|---|---|
| Lens / Getter / Setter | `order.id: Long` | `$.id` |
| Lens (deep) | `customer.address.street: String` | `$.customer.address.street` |
| Optional / AffineFold | `customer.loyaltyId: Option[String]` | `$.customer.loyaltyId` |
| Traversal | `lines[*].name: String` | `$.lines[*].name` |
| Fold | `lines[*].price: Double` | `$.lines[*].price` |

**Prisms stay on a dedicated ADT.** Sum-type derivation across circe + avro +
jsoniter-JSONPath is awkward and not representative of a clean cross-backend
comparison, so the `Result = Ok | Err` ADT (already in `PowerSeriesPrismBench`)
remains the Prism vehicle, joined by the `generics`-derived prism bench.

**Avro `Option` caveat.** kindlings-avro derivation of `Option[String]`
(nullable union) must be confirmed before the avro bench uses `loyaltyId`. If it
doesn't derive cleanly, the avro variant focuses `customer.address.street` and
`lines[*].name` only (no Optional row) — documented in the bench, not worked
around silently.

## Work breakdown

### Phase 0 — Shared fixture (low risk, additive)

- `fixture/Domain.scala` — the case classes above + `mkOrder(nLines): Order`
  builder (deterministic, no `Math.random`). **Pure data, core-only deps.**
- `fixture/DomainCirce.scala` — `Codec.AsObject` givens (Kindlings) + the EO
  `codecPrism` optics + Monocle optics over the decoded `Order`.
- `fixture/DomainJsoniter.scala` — `JsonValueCodec` givens + JSONPath constants.
- `fixture/DomainAvro.scala` — `AvroEncoder/Decoder/SchemaFor` givens + EO
  `codecPrism` optics (subject to the Option caveat).
- `fixture/DomainMonocle.scala` — Monocle `Lens`/`Optional`/`Traversal` over the
  case classes (shared by every "monocle-on-decoded" baseline).

Each integration's codecs live in their own file so a backend that fails to
derive doesn't block the others.

### Phase 1 — Unified real-world benches (the headline)

One bench class per backend, all on the canonical `Order`, each shipping the
four baselines per metric:

- `OrderCirceBench` — `customer.address.street` modify (depth 3) and
  `lines[*].name` traversal (sweep N ∈ {8, 64, 512}). Rows: `eo*`/`eoIor*`,
  `naive*`, `native*` (ACursor), `monocle*`.
- `OrderJsoniterBench` — same foci via `$.customer.address.street` and
  `$.lines[*].name`; read (`foldMap`), `replace`, `modify`. Baselines:
  `eo*` (jsoniter), `native*` (raw jsoniter codec), `monocle*` (decode → modify
  → encode). The existing `JsoniterBench`'s eo-jsoniter-vs-eo-circe row is
  preserved as a cross-EO datapoint.
- `OrderAvroBench` — `customer.address.street` and `lines[*].name`; baselines
  `eo*`, `native*` (kindlings round-trip), `monocle*`.

**Retire after migration:** `JsonPrismBench`, `JsonPrismWideBench`,
`JsonTraversalBench`, `AvroOpticsBench`, and the per-fixture parts of
`JsoniterBench` — their depth/width/array stories all collapse into the three
canonical benches above, parameterized by depth and array size. The "wide
fixture matters" insight from `JsonPrismWideBench` is preserved because the
canonical `Order` is itself wide (≥5 fields per level), which is exactly the
non-degenerate case that bench was built to demonstrate.

### Phase 2 — New coverage (fills the gaps)

- `CompositionBench` — isolate cross-carrier `.andThen`. Two axes: (a) *build*
  cost — constructing a depth-N composed optic; (b) *reuse* cost — `.modify`
  through a pre-built depth-N optic. Carriers: Lens∘Lens (Tuple2),
  Lens∘Optional (Tuple2→Affine morph), Traversal∘Lens (MultiFocus[PSVec]),
  Traversal∘Prism (MultiFocus + Either). Depth sweep 1/3/6. This is the bench
  that measures EO's signature feature head-on.
- `GenericsBench` — `generics.lens`/`generics.prism` derived optics vs a
  hand-written Lens/Prism vs raw `copy`, on the canonical `Order`. Confirms the
  macro-emitted `new S(...)` setter is runtime-free relative to `copy`.
- `OpticBuildBench` — amortization: build `codecPrism[Order].field(...).field(...)`
  inside the loop vs reuse a pre-built one. Answers "is one-shot optic use
  cheap?" for each backend.

### Phase 3 — Cleanup & dedup

- Split `MultiFocusBench`:
  - drop `eoModify_psVecEach` / `naive_psVecEach` / `psPersonAllMobiles` /
    `initPS` / `PsPhone` / `PsPerson` — exact duplicate of `PowerSeriesBench`.
  - move `collectMap`/`collectList`/`tuple3`/`tuple6` aggregators into a new
    `MultiFocusCollectBench`, parameterized by size (currently fixed at 16).
  - what remains in `MultiFocusBench`: the genuine `MultiFocus[List]`
    (`fromLensF`) vs `MultiFocus[PSVec]` (`Traversal.each`) comparison.
- Fold over `lines[*].price` (real aggregation) added to `FoldBench` alongside
  the existing `List[Int]` sum, so Fold has a real-world row too.
- `benchmarks/README.md` rewritten to describe the canonical schema, the
  four-baseline convention, and the new bench inventory.

### Phase 4 (optional, after Phase 1–3 land)

- Resolve *Open question 1* (JMH preamble). If meta-annotations resolve in
  JMH's processor, collapse the 6-line preamble to a single `@BenchDefaults`.
  Otherwise document the CLI-default approach and strip the overridable five
  annotations (keep only `@State`), relying on the documented run command.

## Open questions

1. **Can the JMH preamble be DRY'd?** The `JmhDefaults` walk-back established
   that JMH's `BenchmarkProcessor` reads annotations off the concrete class only
   (no `@Inherited`, no trait propagation). Two untested escapes: (a) a JMH
   meta-annotation (`@BenchDefaults` annotated with the six) — needs a spike to
   confirm the processor resolves it; (b) strip the five overridable annotations
   and rely on the documented `-f 3 -wi 3 -i 5` run line, keeping only `@State`.
   Resolve in Phase 4; don't block the restructure on it.
2. **Avro `Option` derivation** — confirm kindlings-avro handles
   `Option[String]` before `OrderAvroBench` uses `loyaltyId` (see caveat above).
3. **Bench retirement vs. continuity** — retiring `JsonPrismBench` et al. breaks
   historical-number continuity. Acceptable because the canonical benches
   measure the same operations on a strictly more realistic fixture, but the
   PR should call out the swap explicitly so anyone tracking trend data knows.

## Non-goals

- No change to JMH being outside the root aggregate.
- No new library code — fixtures and benches only.
- No mutation/coverage tooling changes.

## Execution status

- [x] Phase 0: `fixture/Domain.scala` (data + builder) and `fixture/DomainMonocle.scala`
      (shared Monocle peers: `street` / `names` / `prices`).
- [x] Phase 0 codecs: circe (`OrderCirceBench` companion), jsoniter
      (`OrderJsoniterBench` companion), avro (`OrderAvroBench` companion). Per-bench
      so a backend that fails to derive can't block the others.
- [x] Phase 1: all three integration benches on the canonical schema, each with the
      relevant baselines side-by-side, compiling + smoke-tested:
  - `OrderCirceBench` — `customer.address.street` modify + `lines[*].name` traversal;
    `eo`/`eoIor`/`naive`/`native` (ACursor)/`monocle`.
  - `OrderJsoniterBench` — street read/write + `lines[*].price` fold; `eo`/`native`
    (codec round-trip)/`monocle`. (`[*]` is read-only in jsoniter phase-1.5, so its
    array story is a fold, not a write traversal.)
  - `OrderAvroBench` — street read/write + `lines[*].name` write traversal;
    `eo`/`native` (kindlings round-trip)/`monocle`. `loyaltyId` omitted (union, not a
    transparent field — see Avro caveat).
- [x] Phase 3 (partial): MultiFocus split — duplicate ArraySeq PSVec path dropped,
      `collect*`/`tuple` moved to `MultiFocusCollectBench`.
- [x] Phase 1 cleanup: retired `JsonPrismBench`, `JsonPrismWideBench`,
      `JsonTraversalBench`, `AvroOpticsBench` (fully superseded by the `Order*`
      canonical benches). `JsoniterBench` **kept** — it's the only cross-EO
      comparison (eo-jsoniter byte-walk vs eo-circe AST on the same bytes), which
      the `Order*` benches don't cover. `benchmarks/README.md` updated.
- [ ] **Blocking before merge — site/docs accuracy.** Retiring the four benches
      left dangling references *and* stale numeric claims in the published docs:
      - Source links (404 after merge): `site/docs/avro.md` (→ `OrderAvroBench`),
        `site/docs/circe.md`, `site/docs/cookbook.md`, `site/docs/extensibility.md`
        (→ `OrderCirceBench`).
      - Stale ratios tied to the *old narrow* fixtures: "~2× speedup", "~1× at the
        wide (28-field) shape" in circe.md / cookbook.md / extensibility.md, and the
        `AvroOpticsBench` depth-1/3 prose + the `JsonTraversalBench` run command in
        `site/docs/benchmarks.md`. The canonical (wide) schema shows much larger
        gaps (circe ~4×+, avro up to ~1000× on the expensive-decode path), so these
        need a **measured re-run** of the `Order*` suite (`-f 3 -wi 3 -i 5`, quiet
        machine) and a prose rewrite — not a find-replace. Deliberately left
        untouched here to avoid publishing unverified numbers.
- [ ] Phase 2: `CompositionBench`, `GenericsBench`, `OpticBuildBench`
- [ ] Phase 3 remainder: FoldBench real-world (`lines[*].price`) row
- [ ] Phase 4: JMH preamble decision

### Early signal (size 64, `-i 1 -wi 1 -f 1`, smoke only — not publishable)

| metric | eo | native | monocle |
|---|---:|---:|---:|
| circe `street` modify | 655 ns¹ | 2,604 ns (naive) | 2,596 ns |
| circe `street` modify | — | 3,277 ns (native ACursor) | — |
| jsoniter `street` read | 104 ns | 6,207 ns | 32,162 ns |
| jsoniter `street` modify | 456 ns | 10,733 ns | 58,094 ns |
| jsoniter `lines[*].price` fold | 6,446 ns | 7,303 ns | 27,618 ns |
| avro `street` read | 21 ns | 16,373 ns | 62,310 ns |
| avro `street` modify | 72 ns | 73,557 ns | 80,467 ns |
| avro `lines[*].name` modify | 2,598 ns | 89,755 ns | 30,638 ns |

¹ circe street numbers measured at size 8. The pattern holds: EO's structural
edit is depth/document-size-insensitive; decode-modify-encode pays O(all fields).
Fold (`lines[*].price`) is the one case where eo ≈ native — a fold must scan the
whole array either way, so there's no AST to skip.
