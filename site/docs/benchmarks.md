# Benchmarks

JMH numbers from
[`benchmarks/`](https://github.com/Constructive-Programming/eo/tree/main/benchmarks),
run side-by-side against [Monocle](https://www.optics.dev/Monocle/)
where a direct equivalent exists and against a hand-rolled
`decode → modify → re-encode` baseline for the EO-only JSON and
PowerSeries optics.

## Reading the numbers

All tables show **average time per operation** (lower is better)
with the 99.9 % confidence interval. `eo*` / `m*` are the EO and
Monocle methods, `naive*` is a hand-written baseline without any
optic machinery.

Sample output, run on a Linux 6.19 x86-64 box, JDK 25, JMH 1.37,
with `-f 1 -i 5 -wi 3 -t 1` (one fork, five measurement
iterations, three warmups, single thread). Total wall time:
~10 min. The absolute numbers will vary by hardware; the
*ratios* reproduce across machines.

> **JMH caveats.** Trustworthy numbers need a quiet machine,
> fork count ≥ 3, CPU-frequency scaling locked, and no
> background builds. The tables below are indicative, not
> publication-grade.  See
> [`benchmarks/README.md`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md)
> for repeatable invocations.

## Lens (`Tuple2` carrier)

`Person.age` focused via a Lens. Same fixture on both sides.

| Operation       |        eo | Monocle | ratio  |
|-----------------|----------:|--------:|-------:|
| `get`           | 0.45 ns   | 0.52 ns | 1.16×  |
| `replace`       | 1.18 ns   | 1.29 ns | 1.09×  |
| `modify`        | 1.37 ns   | 1.52 ns | 1.11×  |

The EO `GetReplaceLens` stores `get` / `enplace` as plain
fields and specialises its fused `modify` on the class, so the
hot path is a straight two-function composition — no
`Tuple2` allocation for the `(X, A)` intermediate that the
generic extension would materialise.

## Prism (`Either` carrier)

`Option[Int]` prism plus an `Either[String, Int]` Right-prism:

| Operation                |     eo | Monocle |
|--------------------------|-------:|--------:|
| `getOption` (Some)       | 0.42 ns | 0.46 ns |
| `getOption` (None)       | 0.42 ns | 0.47 ns |
| `reverseGet`             | 1.06 ns | 1.10 ns |
| Right-`getOption` (Right) | 1.17 ns | 1.29 ns |
| Right-`getOption` (Left)  | 0.46 ns | 0.80 ns |
| Right-`reverseGet`       | 1.06 ns | 1.11 ns |

## Iso (`Forgetful` carrier)

`(Int, String) ↔ Person(age, name)` bijection.

| Operation    |      eo | Monocle |
|--------------|--------:|--------:|
| `get`        | 1.63 ns | 1.67 ns |
| `reverseGet` | 1.22 ns | 1.25 ns |

`BijectionIso` stores `get` / `reverseGet` as plain fields —
same storage shape as Monocle's `case class Iso`, same
direct-call hot path.

## Optional (`Affine` carrier)

Composed through a `Nested0..6` chain. The depth-3 / depth-6
EO variants compose the Lens chain via `.morph[Affine]` and
`.andThen` directly onto the leaf Optional — made possible by
dropping the `<: Tuple` bound on `Affine.assoc` (see
[Concepts → Cross-family composition](concepts.md)).

| Operation                   |        eo |   Monocle |
|-----------------------------|----------:|----------:|
| `modify_0`     (Some leaf)  |  15.11 ns |  12.52 ns |
| `modify_0_empty` (None)     |   0.72 ns |   0.65 ns |
| `replace_0`                 |   7.96 ns |   1.74 ns |
| `modify_3`                  |  79.02 ns |  33.45 ns |
| `modify_6`                  | 121.97 ns |  52.49 ns |

Both sides are within ~2× of each other across depths — the EO
path pays Affine's branching overhead relative to Monocle's
`Option`-specialised internals.

## Getter (`Forgetful` carrier, no write)

| Depth      |      eo |  Monocle |
|------------|--------:|---------:|
| `get_0`    | 0.54 ns |  0.60 ns |
| `get_3`    | 1.50 ns |  7.88 ns |
| `get_6`    | 2.68 ns | 16.12 ns |

Monocle's composed `Getter.andThen` chain pays per-hop typeclass
dispatch Monocle's side doesn't optimise away at call-time. EO
resolves the `.get` extension against each carrier's `Accessor`
statically, so the composed chain inlines to a direct function
call.

Getter composition isn't expressible through `Optic.andThen`
in EO today (see
[Optics → Getter](optics.md#getter)); the `_3` / `_6` EO
numbers are from nested `.get` calls. Monocle's first-class
`Getter.andThen` is the surface for its side.

## Setter (`SetterF` carrier, write-only)

| Depth         |       eo | Monocle |
|---------------|---------:|--------:|
| `modify_0`    |  1.45 ns |  1.27 ns |
| `modify_3`    | 25.37 ns | 13.18 ns |
| `modify_6`    | 50.27 ns | 27.32 ns |

Same composition caveat as Getter — EO's deep-modify benches
nest `modify` calls where Monocle composes natively.

## Fold (`Forget[F]` carrier)

`foldMap(identity)` over `List[Int]`, sweeping size.

| Size |         eo |    Monocle |
|------|-----------:|-----------:|
| 8    |    50.8 ns |    11.4 ns |
| 64   |   458.4 ns |   165.1 ns |
| 512  | 3 868.7 ns | 2 179.5 ns |

Monocle wins here because its `Fold.foldMap` reduces to a
direct `Foldable[F].foldMap` call; EO's `Forget[F]` carrier
adds a small per-element dispatch layer through
`ForgetfulFold`.

## Traversal

`each` on `List[Int]`, plus a `modify(_ + 1)` sweep:

| Size | eo (`each`) | Monocle (`fromTraverse`) | speedup |
|------|------------:|-------------------------:|--------:|
| 8    |    17.8 ns  |               119.4 ns   |  6.71×  |
| 64   |   145.7 ns  |             1 352.5 ns   |  9.28×  |
| 512  | 1 939.5 ns  |            16 214.0 ns   |  8.36×  |

A surprisingly large win — EO's `Traversal.each` keeps the
`Forget[T]` carrier linear by delegating straight to
`Functor[T].map`, while Monocle's `Traversal` wraps each
element in an `Applicative[Id]` traversal and pays the
per-element wrapping cost.

## JsonPrism — cursor-backed JSON edit

No Monocle equivalent at this layer. Compared against the
classical `decode → modify → re-encode` baseline.

| Depth | eo        | naive     | speedup |
|-------|----------:|----------:|--------:|
| 1     |  69.1 ns  | 155.5 ns  | 2.25×   |
| 2     | 116.1 ns  | 158.8 ns  | 1.37×   |
| 3     | 135.8 ns  | 253.3 ns  | 1.87×   |
| wide  | 943.5 ns  | 983.6 ns  | 1.04×   |

The "wide" variant uses 28-total-field records; at that width
the naive decoder has to touch every field. EO's
`codecPrism[…].field(_.x).field(_.y)` walks only the focused
path.

## JsonTraversal — `items.each.name` edits

Uppercasing every `items[*].name` inside a `Basket` record, at
three array sizes:

| Items |          eo |      naive | speedup |
|-------|------------:|-----------:|--------:|
| 8     |    796.1 ns |  1 802.2 ns | 2.26×   |
| 64    |  5 977.8 ns | 12 404.9 ns | 2.07×   |
| 512   | 47 498.6 ns | 95 503.5 ns | 2.01×   |

The ratio is roughly constant — the naive path pays a full
decode / re-encode for every element, so both scale linearly
with array size and EO wins by a constant factor from avoiding
the per-element codec round-trip.

## PowerSeries — traversal with downstream composition

EO-only — no Monocle equivalent. Toggles `isMobile` on every
`Phone` inside a `Person.phones: ArraySeq[Phone]`; the chain
is `Lens → Traversal.powerEach → Lens`.

| Size |  eo (`powerEach` chain) | naive `copy` / `map` | ratio |
|------|------------------------:|---------------------:|------:|
| 4    |                 456 ns  |               13 ns  |  34×  |
| 32   |               2 447 ns  |               81 ns  |  30×  |
| 256  |              22 730 ns  |              780 ns  |  29×  |

The carrier is now flat `Vector[A]` with an internal
`Vector.newBuilder` on the `assoc` hot path (swapped from a
homegrown `Vect[N, A]` that paid O(n²) for persistent concat +
slice). The result: linear scaling across all sizes, with the
residual ~29× overhead being the Composer chain's per-element
`.modify` dispatch — not the storage structure.

For single-pass modify of a collection, `Traversal.each[F, A, B]`
(linear, no downstream composition) is still the correct choice.
Reach for `powerEach` when the chain needs to continue past the
traversal.

See the
[composition notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#composition-notes)
for the full tradeoff matrix.

## Reproducing

From the repo root:

```sh
# Trustworthy numbers — three forks, five iterations, three warmups.
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1"

# Smoke check — one fork, faster but noisier.
sbt "benchmarks/Jmh/run -i 3 -wi 2 -f 1 -t 1"

# Filter by class (JMH regex):
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 .*JsonTraversalBench.*"
```

JMH's GC and stack profilers are useful when a number is
surprising:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof gc .*LensBench.*"
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof stack .*PowerSeries.*"
```
