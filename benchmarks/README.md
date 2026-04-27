# `cats-eo` benchmarks

A JMH harness comparing `cats-eo` optics against
[Monocle](https://www.optics.dev/Monocle/) on the operations both
libraries implement, plus EO-only benches for the features Monocle
has no equivalent for (`PowerSeries`, `JsonPrism`, `JsonTraversal`).

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
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 dev.constructive.eo.bench.JsonPrismBench.eoModify_d3"
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
| `PowerSeriesBench`       | `Lens → Traversal.each[ArraySeq, Phone] → Lens` over `Person.phones` (sweeps 4/32/256/1024) | None. PowerSeries is the carrier behind EO's cross-optic composition. |
| `PowerSeriesNestedBench` | 5-hop `Company → List[Dept] → ArraySeq[Emp] → Boolean` (sweeps 4/32/256 × 4-dept fanout) | None. Stress-tests tree-of-traversals composition. |
| `PowerSeriesPrismBench`  | `Traversal.each[ArraySeq, Result] → Prism[Result, Int]` on a 50/50 Ok/Err mix (sweeps 8/64/512) | None. Regression oracle for Prism miss-branch plumbing. |
| `JsonPrismBench`         | `JsonPrism.modify` at depths 1/2/3 | None. Cursor-backed JSON navigation is cats-eo specific. |
| `JsonPrismWideBench`     | Same on wide (8/14/6 field) records   | None |
| `JsonTraversalBench`     | `codecPrism[Basket].items.each.name.modify` over N elements | None |

All benches share the same shape: a paired `eoXxx` / `mXxx` (or `naiveXxx`) method per metric, so the generated JMH report shows them side-by-side in one table.

### Shared fixture

`dev.constructive.eo.bench.fixture.Nested` (`Nested0` … `Nested6`) is the depth-parameterised case-class chain used by `OptionalBench`, `GetterBench`, `SetterBench`. Each leaf carries:

- `value: Int` — Lens / Getter / Setter focus
- `flag: Option[String]` — Optional focus (with Some-default and empty-default leaf variants)
- `items: List[Int]` — Fold focus (FoldBench builds its own list parameterised by `-P size`)

## Composition notes

Not every optic family composes in EO via the universal `Optic.andThen`:

- **`Lens.andThen(Lens)`** — `Tuple2` carrier, composes cleanly.
- **`Prism.andThen(Prism)`** — `Either` carrier, composes cleanly.
- **`Iso.andThen(Iso)`** — `Forgetful` carrier, composes cleanly.
- **`Traversal.andThen` via `PowerSeries`** — works, but pays the
  `PowerSeries` machinery cost (see `PowerSeriesBench`).
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

Under the hood the hot paths use a private `PSSingleton` protocol — morphed Lens/Prism/Optional inners `collectTo` into pre-sized flat `Array[Int]` + `Array[AnyRef]` builders without per-element `PowerSeries` or `PSVec.Single` wrappers. The `PSSingletonAlwaysHit` refinement (for Lens morphs, which always hit) additionally skips the length-tracking `Array[Int]`. `pEach.to` zero-copies `ArraySeq.ofRef` inputs into a `PSVec.Slice`, and `pEach.from` hands the result `Slice`'s backing array straight to `ArraySeq.unsafeWrapArray` via `unsafeShareableArray` when the Slice densely covers it.

Use `Traversal.each` / `pEach` (carrier `MultiFocus[PSVec]`) for both single-pass element-wise modifies and chains that continue past the traversal — same optic, same `.modify`, with `.foldMapF` for read-only aggregation and `.andThen` for downstream composition.
