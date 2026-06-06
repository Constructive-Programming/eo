# `cats-eo` benchmarks

A JMH harness with two layers:

1. **Optic-overhead micro-benches** comparing `cats-eo` against
   [Monocle](https://www.optics.dev/Monocle/) on the operations both
   libraries implement (Lens / Prism / Iso / Traversal / Getter / Setter /
   Fold / Optional / AffineFold), plus EO-only carrier benches
   (`MultiFocus[PSVec]` / `MultiFocus[List]` traversals and aggregators).
2. **Canonical-schema integration benches** — one realistic `Order` document
   navigated identically across all three byte/AST backends (circe / jsoniter /
   avro), each measured against the same baselines so the real-world
   "edit-without-decoding" story is directly comparable. See
   [*Canonical-schema integration*](#canonical-schema-integration-real-world).

**The `benchmarks/` sub-project is deliberately outside the root
aggregator** — `sbt compile` and `sbt test` skip it. Run explicitly.

## Running

Trustworthy numbers (five iterations, three warm-ups, three forks):

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1"
```

Quick smoke check (one iteration, one warm-up, one fork) — useful
while iterating on a bench, not for comparisons:

```sh
sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 -t 1"
```

Filter by class (`.*` is JMH's regex syntax):

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 .*OptionalBench.*"
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 .*Order.*"
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 dev.constructive.eo.bench.OrderCirceBench.eoStreet"
```

Enable a profiler (see `-prof list` for available):

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof gc .*LensBench.*"
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof stack .*PowerSeries.*"
```

## Environment caveats

JMH's own disclaimer applies in full:

> The numbers below are just data. To gain reusable insights, you
> need to follow up on why the numbers are the way they are.

Specifically — the numbers are only meaningful when:

1. The machine is quiet (close Chrome, no background builds).
2. CPU frequency scaling is disabled or locked to a steady state.
3. `-f` ≥ 3 (the default `@Fork(3)` annotations honour this when
   invoking without `-f`).
4. The JVM is recent enough that JMH's `@Fork`-injected runtime
   detection works; the build targets JDK 17 and JDK 21.

## What's in the harness

### Monocle-paired (Lens, Prism, Iso, Traversal, Optional, Getter, Setter, Fold)

| Bench class        | Subject                    | Notes |
|-------------------|---------------------------|-------|
| `LensBench`       | `Person.age` Lens         | Existing; `Person` single-level fixture |
| `PrismBench`      | `Option[Int]` + `Either[String, Int]` | Existing; both sides of the `Some`/`None` split |
| `IsoBench`        | `(Int, String) ↔ Person`  | Existing; `BijectionIso` specialisation |
| `TraversalBench`  | `each[List, Int, Int]`    | Existing; `-P size=N` sweeps `N ∈ {8, 64, 512}` |
| `OptionalBench`   | `Nested0.flag: Option[String]` | Depths 0 / 3 / 6 over shared `Nested` fixture |
| `GetterBench`     | `Nested0.value: Int`      | Depths 0 / 3 / 6; see *Composition notes* |
| `SetterBench`     | `Nested0.value: Int`      | Depths 0 / 3 / 6; see *Composition notes* |
| `FoldBench`       | `Foldable[List]` + `Monoid[Int]` | `-P size=N` sweep |

### EO-only (no direct Monocle equivalent)

| Bench class              | Subject | Monocle equivalent |
|--------------------------|---------|--------------------|
| `PowerSeriesBench`       | `Lens → Traversal.each[ArraySeq, Phone] → Lens` over `Person.phones` (sweeps 4/32/256/1024) | None. The `MultiFocus[PSVec]` carrier is the mechanism behind EO's cross-optic composition. |
| `PowerSeriesNestedBench` | 5-hop `Company → List[Dept] → ArraySeq[Emp] → Boolean` (sweeps 4/32/256 × 4-dept fanout) | None. Stress-tests tree-of-traversals composition. |
| `PowerSeriesPrismBench`  | `Traversal.each[ArraySeq, Result] → Prism[Result, Int]` on a 50/50 Ok/Err mix (sweeps 8/64/512) | None. Regression oracle for Prism miss-branch plumbing. |
| `MultiFocusBench`        | `MultiFocus[List]` (`fromLensF`) vs `MultiFocus[PSVec]` (`Traversal.each`) on the same `Lens → List/each → Lens` chain (sweeps 4/32/256/1024) | None. The unified carrier that absorbed the v1 AlgLens/Kaleidoscope/Grate/PowerSeries families. |
| `MultiFocusCollectBench` | `collectMap` (ZipList mean / `Const` sum), `collectList` (cartesian-singleton), and `MultiFocus.tuple` 3-/6-slot modifies | None. Carrier-level reduction / broadcast machinery. |
| `JsoniterBench`         | Cross-EO: `eo-jsoniter` (byte-walk) vs `eo-circe` (AST) read/write at depths 3/4, miss path, `[*]` fold-sum, and `replace`/`modify` writes | None. The two EO JSON backends head-to-head on the same bytes. |

All micro-benches share the same shape: a paired `eoXxx` / `mXxx` method per metric, so the generated JMH report shows them side-by-side in one table.

`AffineFoldBench` (`AffineFold.getOption` at depths 0/3/6 against Monocle's `Optional.getOption`) rounds out the Monocle-paired set; Monocle 3.x ships no standalone `AffineFold`, so it sits between the paired and EO-only tables.

### Canonical-schema integration (real-world)

One shared [`Order`](src/main/scala/dev/constructive/eo/bench/fixture/Domain.scala)
event — deliberately **deep** (`customer.address.street`, depth 3), **wide**
(≥5 fields per level), **arrayed** (`lines: List[LineItem]`), and **optional**
(`customer.loyaltyId`) — drives every integration bench on the *same logical
schema*, so the three byte/AST backends are directly comparable.

Each metric ships every relevant baseline side-by-side in one report row.
The vocabulary is consistent across backends:

- `eo*` / `eoIor*` — the EO optic (silent `*Unsafe` hot path and, where it
  differs, the default `Ior`-bearing surface).
- `naive*` — full decode → modify → re-encode; materialises the whole
  record. The "what you'd write without optics" baseline, present in every
  backend.
- `native*` — a hand-optimized, backend-specific path that **avoids the full
  read**. Only jsoniter ships one here: a `JsonReader` codec that walks to the
  focus and `skip()`s every sibling. It's exactly what eo automates generically
  — and a correctness guard in the bench's `@Setup` asserts it agrees with the
  full decode. circe and avro have no separate `native*`; eo *is* their partial
  walk.
- `hcursor*` / `direct*` (circe only) — two ways to edit the circe AST without
  decoding: `HCursor` (whose `.top` replays the zipper to rebuild — sometimes
  *slower* than a full decode) vs direct `JsonObject` surgery (rebuild only the
  touched parents).
- `monocle*` — decode to the case class, run a Monocle optic, re-encode. Monocle
  has no byte/AST carrier, so it must pay the full round-trip — this is where
  EO's structural-edit advantage shows.

| Bench class          | Foci | Baselines | Notes |
|----------------------|------|-----------|-------|
| `OrderCirceBench`    | `customer.address.street` modify; `lines[*].name` traversal | `eo`/`eoIor`/`naive`/`hcursor`/`direct`/`monocle` | circe `Json` AST |
| `OrderJsoniterBench` | `customer.address.street` read+write; `lines[*].price` fold | `eo`/`native` (partial scan, read only)/`naive`/`monocle` | Raw `Array[Byte]`. `[*]` is read-only in jsoniter phase-1.5, so its array story is a fold, not a write traversal. |
| `OrderAvroBench`     | `customer.address.street` read+write; `lines[*].name` write traversal | `eo`/`naive`/`monocle` | `IndexedRecord`. `loyaltyId` omitted — kindlings encodes `Option` as a union navigated via `.union[Branch]`, not a transparent field. |

`@Param size ∈ {8, 64, 512}` grows the surrounding document so the
edit-without-decoding cost (depth/size-insensitive) shows against the
decode-modify-encode baselines (O(all fields)). The one case where `eo ≈ native`
is the fold — scanning a whole array can't skip an AST.

### Shared fixtures

Two shared fixtures under `dev.constructive.eo.bench.fixture`:

**`Domain`** — the canonical [`Order`](src/main/scala/dev/constructive/eo/bench/fixture/Domain.scala)
event used by every integration bench (`Order*`). `Domain.mkOrder(n)` builds a
deterministic order with `n` line items. `DomainMonocle` holds the shared Monocle
peers (`street` / `names` / `prices`) every `monocle*` baseline runs against.
Per-backend codecs (circe / jsoniter / avro) live in each bench's companion so a
backend that fails to derive can't block the others.

**`Nested`** (`Nested0` … `Nested6`) — the depth-parameterised case-class chain
used by the Monocle-paired micro-benches `OptionalBench`, `GetterBench`,
`SetterBench`. Each leaf carries:

- `value: Int` — Lens / Getter / Setter focus
- `flag: Option[String]` — Optional focus (with Some-default and empty-default leaf variants)
- `items: List[Int]` — Fold focus (FoldBench builds its own list parameterised by `-P size`)

## Composition notes

Not every optic family composes in EO via the universal `Optic.andThen`:

- **`Lens.andThen(Lens)`** — `Tuple2` carrier, composes cleanly.
- **`Prism.andThen(Prism)`** — `Either` carrier, composes cleanly.
- **`Iso.andThen(Iso)`** — `Forgetful` carrier, composes cleanly.
- **`Traversal.andThen` via `MultiFocus[PSVec]`** — works, but pays the
  `MultiFocus[PSVec]` machinery cost (see `PowerSeriesBench`).
- **`Lens.andThen(Optional)`** — works via cross-carrier `.andThen`. `Affine.assoc[X, Y]` carries no Tuple bound, so a `Morph[Tuple2, Affine]` lifts the Lens chain into the Affine carrier automatically; no explicit `.morph` step on the call site.
- **`Getter.andThen(Getter)`** — blocked: Getter's `T = Unit` vs the outer `B = A` in the `Optic.andThen` slot. The bench nests `get` calls manually: `leaf.get(n1.get(n2.get(...)))`.
- **`Setter.andThen(Setter)`** — blocked: `SetterF` has no `AssociativeFunctor` instance. The bench nests `modify` calls: `n1.modify(leafSetter.modify(f))`.

For Monocle side, every corresponding `andThen` is first-class on the optic trait.

The remaining Getter/Setter workarounds reflect what a user would actually write if composing these families in EO today. The cleaner Monocle side isn't a bench advantage — the work done on both sides is the same Scala code with the same allocations.

## Interpreting PowerSeries numbers

All three PowerSeries benches scale **linearly** with traversed-collection size; the eo-to-naive ratios tighten as size grows (fixed per-op setup amortises).

Current ratios (see [benchmarks.md](../site/docs/benchmarks.md#powerseries--traversal-with-downstream-composition) for full tables):

- **`PowerSeriesBench` (dense `Lens → Traversal → Lens`):** 5.1× at N=4 → 1.9× at N=1024.
- **`PowerSeriesNestedBench` (5-hop tree):** 5.4× at N=4 → 2.4× at N=256.
- **`PowerSeriesPrismBench` (Prism miss-branch):** ~5.5× across sizes — Prism miss plumbing (per-element Either-tag write + miss-branch `ys` slot) is the inherent cost.

Under the hood the hot paths use two private fast-path markers on the `MultiFocus[PSVec]` carrier. Prism/Optional morphs mix in `MultiFocusPSMaybeHit` (maybe-hit), whose `collectTo` writes directly into pre-sized flat `Array[Int]` + `Array[AnyRef]` builders (`IntArrBuilder` / `ObjArrBuilder`) without per-element `Either`/`Option` wrappers. Lens morphs mix in the carrier-wide `MultiFocusSingleton` (always-hit), which additionally skips the length-tracking `Array[Int]`. `pEach.to` zero-copies `ArraySeq.ofRef` inputs into a `PSVec.Slice`, and `pEach.from` hands the result `Slice`'s backing array straight to `ArraySeq.unsafeWrapArray` via `unsafeShareableArray` when the Slice densely covers it.

Use `Traversal.each` / `pEach` (carrier `MultiFocus[PSVec]`) for both single-pass element-wise modifies and chains that continue past the traversal — same optic, same `.modify`, with `.foldMap` for read-only aggregation and `.andThen` for downstream composition.
