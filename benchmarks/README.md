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
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 eo.bench.JsonPrismBench.eoModify_d3"
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

| Bench class             | Subject | Monocle equivalent |
|-------------------------|---------|--------------------|
| `PowerSeriesBench`      | `Traversal.each[ArraySeq, A]` (PowerSeries-backed) chain | None. PowerSeries is the carrier behind EO's cross-optic composition. |
| `JsonPrismBench`        | `JsonPrism.modify` at depths 1/2/3 | None. Cursor-backed JSON navigation is cats-eo specific. |
| `JsonPrismWideBench`    | Same on wide (8/14/6 field) records   | None |
| `JsonTraversalBench`    | `codecPrism[Basket].items.each.name.modify` over N elements | None |

All benches share the same shape: a paired `eoXxx` / `mXxx` (or `naiveXxx`) method per metric, so the generated JMH report shows them side-by-side in one table.

### Shared fixture

`eo.bench.fixture.Nested` (`Nested0` … `Nested6`) is the depth-parameterised case-class chain used by `OptionalBench`, `GetterBench`, `SetterBench`. Each leaf carries:

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

`PowerSeriesBench` shows `Traversal.each` (PowerSeries-backed) scaling **linearly** with traversed-collection size — roughly ~15–20× off the naive `copy`/`map` baseline at every size after the PSVec slice-view refactor. The remaining overhead is the Composer chain's per-element `.modify` dispatch plus the singleton PSVec wrap per Lens-morph hop, not the storage structure.

Use `Traversal.forEach[F, A, B]` (carrier `Forget[F]`) for single-pass element-wise modifies where no downstream optic composition is needed — it's the linear-and-tight fast path. Reach for `Traversal.each` / `pEach` (carrier `PowerSeries`) when the chain needs to continue past the traversal.
