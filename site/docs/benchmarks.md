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
EO variants compose the Lens chain directly onto the leaf
Optional via cross-carrier `.andThen` — the `Morph[Tuple2,
Affine]` instance auto-lifts each Lens hop into the `Affine`
carrier, no explicit morph step required. Made possible by
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

A surprisingly large win — EO's `Traversal.each` (carrier
`MultiFocus[PSVec]`) collects element references into a flat
focus vector and rebuilds via `Functor[PSVec].map`, while
Monocle's `Traversal` wraps each element in an `Applicative[Id]`
traversal and pays the per-element wrapping cost.

## JsonPrism — cursor-backed JSON edit

No Monocle equivalent at this layer. Two EO surfaces side by
side: `.modifyUnsafe` (silent, pre-v0.2 shape) and `.modify`
(default, returns `Ior[Chain[JsonFailure], Json]`). Baseline is
the classical `decode → modify → re-encode`.

| Depth | `.modifyUnsafe` |    `.modify` (Ior) |     naive | Unsafe vs naive | Ior vs naive | Ior tax |
|:-----:|----------------:|-------------------:|----------:|----------------:|-------------:|--------:|
| 1     |    68.8 ± 2.7   |    67.4 ± 1.4      |    151 ns |         2.20×   |       2.24×  | ~0 ns   |
| 2     |   114.6 ± 0.7   |   117.3 ± 2.8      |    151 ns |         1.32×   |       1.29×  | +2.7 ns |
| 3     |   129.3 ± 5.3   |   131.9 ± 4.9      |    234 ns |         1.81×   |       1.77×  | +2.5 ns |

The "Ior tax" column isolates the cost of opting into the
default diagnostic-bearing surface: a single `Ior.Right(json)`
case-class allocation per call, flat per path depth (not per
step). At d1 the number is inside measurement noise (d1 Ior
actually comes out slightly faster on this run — JMH standard
error territory).

A fourth "wide" fixture (28-field record, measured separately)
showed the ratio narrow to ~1.04× because the naive decoder
touches every field regardless of arity, while EO still walks
only the focused path. That wide-record bench stayed on the
pre-rename harness; the numbers in the table above are for the
standard Person / Deep3 fixtures.

**When to reach for `.modifyUnsafe`.** If you've measured and
want pre-v0.2 throughput exactly, or you have a hot path where
the `Ior.Right` allocation matters (heap-sensitive loops, very
short overall work units). Otherwise the default is the
recommended entry — you get structured `JsonFailure` diagnostics
for the same order-of-magnitude speedup over naive.

## JsoniterBench — byte-cursor JSON read+write vs eo-circe

EO-only — different niche from eo-circe: jsoniter-scala codecs are
compile-time-derived, so the optic walks bytes directly with no AST
materialised. Six fixtures cover the read and write surface; the
`c*` rows are eo-circe `JsonPrism` after `circeParse(new String(bytes,
"UTF-8"))` (the realistic per-call workflow), the `j*` rows are
[`JsoniterPrism`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/main/scala/dev/constructive/eo/jsoniter/JsoniterPrism.scala)
/
[`JsoniterTraversal`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/main/scala/dev/constructive/eo/jsoniter/JsoniterTraversal.scala)
on the same `Array[Byte]`.

Run, 3 forks / 5 measured iterations / 15 samples per cell on a
~250-byte synthetic JSON fixture:

| Bench                                    | eo-circe (ns/op) | eo-jsoniter (ns/op) | Speedup |
|------------------------------------------|-----------------:|--------------------:|--------:|
| Read scalar `Long` at depth 3            |   801.8 ± 24.8   |       49.6 ± 0.4    | **16.2×** |
| Read scalar `String` at depth 4          |   830.9 ± 16.5   |      102.1 ± 1.9    |  **8.1×** |
| Read miss (path doesn't resolve)         |   807.2 ± 28.7   |       68.2 ± 0.5    | **11.8×** |
| `[*]` fold-sum over 10-element array     |   822.4 ± 37.7   |      340.0 ± 5.0    |  **2.4×** |
| `.replace` Long at depth 3               |  1447.9 ± 44.6   |      101.4 ± 32.5   | **14.3×** |
| `.modify` Long at depth 3                |  1444.4 ± 27.1   |       87.4 ± 1.3    | **16.5×** |

eo-circe's number always includes the `circeParse` step — that's
what dominates the per-call cost, not the drill. If a caller caches
the parsed `Json` across calls, the speedup narrows; most real-world
JSON workloads (HTTP request handlers, log processors) are
one-shot, which is what the bench measures.

The miss case is faster on the eo-jsoniter side than the depth-4
hit (~68 vs ~100 ns) — the byte scanner aborts on the first
non-matching field before any decode runs. eo-circe's miss case
costs the same as hit because the parse always runs.

The traversal speedup (2.4×) is more honest than the scalar reads:
per-element decode + `Array[AnyRef]` allocation accumulates over the
10 elements. Larger arrays push the ratio higher (constant parse
cost spread over more elements); smaller arrays would push it lower.

Writes don't degrade vs reads on the eo-jsoniter side — the splice
is bounded by `O(src.length)` memcpy, which is ~50 ns for a 250-byte
fixture. eo-circe writes pay parse + AST modify + emit, roughly
double the read cost. **`.modify` slightly beats `.replace`** because
`replace = modify(_ => b)` adds a closure layer; difference is
within noise.

The `jReplaceLong` row's wide ±32 ns half-width is a JIT-warmup
outlier from one fork; re-runs cluster in 80–120 ns. Bumping
`-f 5 -wi 5` tightens it.

Run the suite with:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 .*JsoniterBench.*"
```

The
[design spike](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-eo-jsoniter-spike.md)
documents the carrier choice (no new carrier — Affine for the Prism,
`MultiFocus[PSVec]` for the Traversal), the splice mechanics, and
the phased-build outcome.

## AvroPrism — direct-walk over `IndexedRecord`

EO-only — no Monocle equivalent at this layer. `AvroPrism.modify` walks
the `IndexedRecord` tree along its precomputed `PathStep` array, decodes
only at the focused leaf, and stitches the parents back together — the
classical alternative is to decode the whole record into its
case-class tree, mutate the leaf, and re-encode end-to-end.

Two depths benched in `AvroOpticsBench`: depth 1 (`Person.name`,
shallow) and depth 3 (`Deep3.d2.d1.atom.value`, deep). Each depth has
paired `eo*` / `native*` rows so JMH reports them side-by-side. The
`eo*` side is the silent `*Unsafe` hot path; the `native*` side is the
raw [kindlings-avro-derivation][kindlings-avro] codec round-trip
(`AvroCodec.decodeEither` → case-class `.copy` chain →
`AvroCodec.encode`). Same shape as `JsonPrismBench` for eo-circe; the
codec library swap is the only difference.

Run them with the standard JMH invocation, filtering by class:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 .*AvroOpticsBench.*"
```

A third pair of rows (`eoModifyIor*`) reports the default
Ior-bearing surface for the same fixtures — the additional cost is a
single `Ior.Right(record)` allocation per call, mirroring the "Ior
tax" datapoint reported for `JsonPrism`.

The plan target is "≤2× the unwrapped baseline at deep paths" — the
direct-walk speedup absorbs Avro's per-step `IndexedRecord.get(i)` /
`put(i, v)` cost without ever materialising the case-class tree at the
intermediate depths.

[kindlings-avro]: https://github.com/MateuszKubuszok/kindlings-avro-derivation

## JsonTraversal — `items.each.name` edits

Uppercasing every `items[*].name` inside a `Basket` record, at
three array sizes. Same two-surface story as JsonPrism above:

| Items | `.modifyUnsafe` | `.modify` (Ior) |      naive | Unsafe vs naive | Ior vs naive | Ior tax   |
|:-----:|----------------:|----------------:|-----------:|----------------:|-------------:|----------:|
| 8     |    797 ±  18    |    819 ±  22    |  1 659 ns  |         2.08×   |       2.03×  |   +22 ns / +2.9 % |
| 64    |  5 991 ± 188    |  6 128 ±  83    | 12 196 ns  |         2.04×   |       1.99×  |  +137 ns / +2.3 % |
| 512   | 47 046 ± 601    | 48 928 ±1 428   | 92 710 ns  |         1.97×   |       1.89×  | +1 882 ns / +4.0 % |

Traversal's Ior tax scales with element count (~3.7 ns per
element at size 512), consistent with per-element `Chain`
bookkeeping on the success path. The gap to naive is ~2× at
every size and stays ~2× when switching to the default
Ior-bearing surface — the cursor-walk speedup absorbs the
accumulator cost comfortably.

The ratio is roughly constant across sizes — the naive path
pays a full decode / re-encode for every element, so both sides
scale linearly and EO wins by a constant factor from avoiding
the per-element codec round-trip.

## PowerSeries traversal with downstream composition

EO-only — no Monocle equivalent. Three variants in the harness,
one per common chain shape, all sharing the `PowerSeries`-backed
`Traversal.each` as the composition vehicle.

### `PowerSeriesBench.eoModify_powerEach` — `Lens → Traversal.each → Lens`

Toggles `isMobile` on every `Phone` inside a `Person.phones:
ArraySeq[Phone]`. The two-hop chain exercises the flat "dense
singleton outer + multi-focus inner + dense singleton inner" pattern.

| Size | eo        |  naive  | ratio |
|------|----------:|--------:|------:|
| 4    |    66 ns  |   13 ns |  5.1× |
| 32   |   275 ns  |   80 ns |  3.4× |
| 256  | 1 890 ns  |  726 ns |  2.6× |
| 1024 | 6 927 ns  | 3 633 ns |  1.9× |

### `PowerSeriesNestedBench.eoModify_nested` — 5-hop tree of traversals

`Company → List[Department] → ArraySeq[Employee] → Boolean`. Two
traversal fan-outs with Lens hops between them — the worst shape
for flat-carrier composition.

| Size | eo       |  naive  | ratio |
|------|---------:|--------:|------:|
| 4    |  348 ns  |   65 ns | 5.4×  |
| 32   | 1 175 ns |  497 ns | 2.4×  |
| 256  | 8 085 ns | 3 376 ns | 2.4×  |

### `PowerSeriesPrismBench.eoModify_sparse` — `Traversal.each → Prism`

Increments every `Ok.value: Int` inside an `ArraySeq[Result]`
where `Result = Ok(Int) | Err(String)` is a 50/50 split. The
Prism miss branch is the slow part of the composition machinery —
this bench is the regression oracle for it.

| Size | eo       |  naive | ratio |
|------|---------:|-------:|------:|
| 8    |    59 ns |  12 ns | 5.0×  |
| 64   |   408 ns |  73 ns | 5.6×  |
| 512  | 3 610 ns | 660 ns | 5.5×  |

### What makes these numbers possible

The `PowerSeries` carrier pairs an existential leftover
`xo: Snd[A]` with a `PSVec[B]` focus vector — an `Array[AnyRef]`
plus an `(offset, length)` window, so per-element slices during
reassembly are pointer updates rather than arraycopies.

Most of the machinery is hidden behind the `PSSingleton`
protocol — an internal trait implemented by the morphed
`Lens` / `Prism` / `Optional` optics (their `.to` would otherwise
build a throwaway `PowerSeries` + `PSVec.Single` per element).
`assoc.composeTo` / `composeFrom` detect the protocol and call
`collectTo` / `reconstructSingleton` directly, skipping the
per-element wrapper allocations. The `PSSingletonAlwaysHit`
refinement covers the "every call hits" case (Lens morphs) —
it also skips the per-element length `Array[Int]` since every
slot is implicitly 1.

`Traversal.pEach`'s `from` rebuilds the container via `Functor.map`
with a captured `var` counter — the `State[Int, _]` chain that
`Traverse.mapAccumulate` would build shows up as 25 % CPU on
`State`-thunk bookkeeping when used literally. For `ArraySeq`
specifically both the `.to` (zero-copy from `ArraySeq.ofRef.unsafeArray`)
and `.from` (direct `ArraySeq.unsafeWrapArray` of the shared
`PSVec.Slice` backing array) go through an `Array[AnyRef]` end-to-end
with a single `System.arraycopy` at most — `Traverse[ArraySeq].map`'s
builder-sizing path through `SeqOps.size$` was 18 % CPU before
this bypass.

`IntArrBuilder.unsafeAppend` / `ObjArrBuilder.unsafeAppend` skip
the per-call grow-check on hot paths where the total is known
upfront (`composeTo` pre-sizes all three builders to `n` in
PSSingleton paths). `PSVec.Slice.unsafeShareableArray` completes
the end-to-end zero-copy story for freshly-built result arrays.

The cumulative effect vs the pre-optimisation baseline on this
same harness is −59 % to −67 % ns and −58 % to −67 % alloc
across the three benches. Ratios to naive now sit at 1.9× on
dense Lens-chain (`powerEach @ 1024`), 2.4× on nested, 5× on
sparse-Prism — the remaining gap on sparse is inherent Prism
miss-branch plumbing and is substantially smaller than the
5-10× ratios other optic libraries publish for the same shape.

`Traversal.each` / `pEach` (carrier `MultiFocus[PSVec]`) covers
both single-pass modifies and chains that continue past the
traversal — same optic, same `.modify`, with `.foldMap` for
read-only aggregation and `.andThen` for downstream composition.

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
