# Benchmarks

JMH numbers from [`benchmarks/`](https://github.com/Constructive-Programming/eo/tree/main/benchmarks),
in three groups:

1. **[Optic micro-benchmarks](#optic-micro-benchmarks-vs-monocle)** — per-call
   overhead vs [Monocle](https://www.optics.dev/Monocle/) on the operations both
   libraries implement.
2. **[Integration: edit without decoding](#integration-edit-without-decoding)** —
   one realistic `Order` document edited through circe / avro / jsoniter,
   against decode-modify-encode and other baselines.
3. **[EO-only: PowerSeries composition](#powerseries-traversal-with-downstream-composition)** —
   multi-focus traversal chains with no Monocle equivalent.

## How these were measured

All figures are **average time per operation in nanoseconds (ns/op); lower is
better.** Absolute numbers vary by hardware — the *ratios* are the durable
signal.

| Group | Source | Config |
|---|---|---|
| Integration | Reproducible CI workflow (`benchmarks.yml`, ubuntu-22.04 / temurin@21), error half-widths ≤ ~3 % | `-f 3 -wi 3 -i 5` |
| Micro + PowerSeries | Hand-captured local snapshot (Linux x86-64, JDK 25), indicative | `-f 1 -wi 3 -i 5` |

> **JMH caveat.** Trustworthy numbers need a quiet machine, fork count ≥ 3, and
> locked CPU frequency. The local-snapshot tables are indicative, not
> publication-grade; the integration tables come from CI so anyone can re-run
> them. See [Reproducing](#reproducing) and
> [`benchmarks/README.md`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md).

## Optic micro-benchmarks (vs Monocle)

`eo` is the cats-eo method, `Monocle` the equivalent; `ratio` is `naive`-free —
just the two optic libraries head to head.

**Lens** (`Tuple2` carrier) — `Person.age`:

| Operation | eo | Monocle | ratio |
|---|--:|--:|--:|
| `get`     | 0.45 | 0.52 | 1.16× |
| `replace` | 1.18 | 1.29 | 1.09× |
| `modify`  | 1.37 | 1.52 | 1.11× |

`GetReplaceLens` stores `get` / `enplace` as plain fields and specialises its
fused `modify`, so the hot path is a two-function composition — no `(X, A)`
tuple allocation.

**Prism** (`Either` carrier) — `Option[Int]` plus an `Either[String, Int]`
Right-prism:

| Operation | eo | Monocle |
|---|--:|--:|
| `getOption` Some | 0.42 | 0.46 |
| `getOption` None | 0.42 | 0.47 |
| `reverseGet`     | 1.06 | 1.10 |
| Right `getOption` (Right) | 1.17 | 1.29 |
| Right `getOption` (Left)  | 0.46 | 0.80 |
| Right `reverseGet`        | 1.06 | 1.11 |

**Iso** (`Forgetful` carrier) — `(Int, String) ↔ Person`:

| Operation | eo | Monocle |
|---|--:|--:|
| `get`        | 1.63 | 1.67 |
| `reverseGet` | 1.22 | 1.25 |

`BijectionIso` stores both directions as plain fields — same shape and
direct-call hot path as Monocle's `case class Iso`.

**Optional** (`Affine` carrier) — leaf `Option[String]`, composed through a
`Nested0..6` Lens chain via cross-carrier `.andThen` (the `Morph[Tuple2, Affine]`
auto-lifts each hop):

| Operation | eo | Monocle |
|---|--:|--:|
| `modify_0` (Some)  |  15.11 | 12.52 |
| `modify_0` (None)  |   0.72 |  0.65 |
| `replace_0`        |   7.96 |  1.74 |
| `modify_3`         |  79.02 | 33.45 |
| `modify_6`         | 121.97 | 52.49 |

Within ~2× across depths — EO pays `Affine`'s branching where Monocle has
`Option`-specialised internals.

**Getter / Setter** (`Forgetful` / `SetterF`) — depth-0/3/6 over `Nested`.
Neither family composes through `Optic.andThen` in EO today (see
[Optics → Getter](optics.md#getter)), so the `_3` / `_6` EO numbers nest `.get`
/ `.modify` calls by hand; Monocle composes natively.

| Depth | Getter eo | Getter Monocle | Setter eo | Setter Monocle |
|---|--:|--:|--:|--:|
| `_0` | 0.54 |  0.60 |  1.45 |  1.27 |
| `_3` | 1.50 |  7.88 | 25.37 | 13.18 |
| `_6` | 2.68 | 16.12 | 50.27 | 27.32 |

EO resolves `.get` against each carrier's `Accessor` statically, so its composed
read inlines to a direct call — hence the widening Getter win at depth. On write,
the nested-`modify` shape costs EO ~2× Monocle's native `Setter.andThen`.

**Fold / Traversal** — `foldMap(identity)` and `each.modify(_ + 1)` over
`List[Int]`, sweeping size:

| Size | Fold eo | Fold Monocle | Traversal eo | Traversal Monocle | Traversal speedup |
|---|--:|--:|--:|--:|--:|
| 8   |    50.8 |    11.4 |    17.8 |    119.4 | 6.7× |
| 64  |   458.4 |   165.1 |   145.7 |  1 352.5 | 9.3× |
| 512 | 3 868.7 | 2 179.5 | 1 939.5 | 16 214.0 | 8.4× |

Monocle's `Fold` reduces to a direct `Foldable.foldMap`; EO's `Forget[F]` adds a
per-element dispatch layer, so Monocle wins Fold. Traversal flips it: EO's
`each` (carrier `MultiFocus[PSVec]`) collects element references into a flat
focus vector and rebuilds via `Functor[PSVec].map`, beating Monocle's
per-element `Applicative[Id]` wrapping ~7–9×.

## Integration: edit without decoding

The three integration benches share **one realistic `Order` document** — deep
(`customer.address.street`, depth 3), wide (≥5 fields per level), and arrayed
(`lines`) — so the JSON/Avro backends are directly comparable. Baselines per
metric: `eo` (the cats-eo optic, plus the default `Ior`-bearing surface where it
differs), `naive` (`decode → copy → re-encode`), `monocle` (decode → Monocle
optic → encode), and backend-specific honest comparators (circe's `hcursor` /
`direct` AST edits; jsoniter's hand-rolled partial-scan `native`).

**The thesis, in one line:** a pinpoint read/edit through cats-eo is *flat in
document size*, while decode-modify-encode is *linear* — so the advantage is
small on tiny payloads and enormous on large ones. The exception is whole-array
work, where every approach must visit every element.

### circe — `JsonPrism` / `JsonTraversal` over `Json`

Scalar deep edit, `customer.address.street`:

| size | eo (Unsafe) | eo (Ior) | hcursor | direct | naive | monocle | eo vs naive |
|--:|--:|--:|--:|--:|--:|--:|--:|
| 8   | 1 277 | 1 318 | 1 163 | 1 071 |   4 124 |   4 127 |   3.2× |
| 64  | 1 283 | 1 334 | 1 170 | 1 067 |  25 695 |  24 506 |    20× |
| 512 | 1 274 | 1 387 | 1 166 | 1 075 | 208 089 | 210 073 |   163× |

The edit is **flat** (~1.3 µs at any size); `naive` / `monocle` scale with the
whole payload. `direct` `JsonObject` surgery is the fastest hand form (what
`JsonPrism` mirrors); `hcursor` is competitive. The `Ior` surface costs ~40–110 ns
(one allocation) over `*Unsafe`.

Array write-traversal, `lines[*].name`:

| size | eo (Unsafe) | eo (Ior) | hcursor | direct | naive | monocle |
|--:|--:|--:|--:|--:|--:|--:|
| 8   |   5 263 |   5 406 |   4 293 |   4 206 |   4 428 |   4 861 |
| 64  |  37 618 |  40 899 |  30 336 |  31 371 |  27 353 |  29 413 |
| 512 | 302 606 | 331 334 | 241 304 | 243 950 | 223 535 | 261 365 |

Honest result: a whole-array rewrite is O(elements) for everyone, so the cursor
walk has no edge — EO is ~1.2–1.35× *slower* than naive here. Reach for JSON
traversals for composition and diagnostics, not raw array throughput. (Avro
below is the opposite, because its per-element decode is so costly.)

### avro — `AvroPrism` / `AvroTraversal` over `IndexedRecord`

Scalar `customer.address.street` (`loyaltyId` is omitted — kindlings encodes
`Option` as a union, navigated via `.union[Branch]`):

| size | eo read | naive read | eo modify | naive modify | read speedup | modify speedup |
|--:|--:|--:|--:|--:|--:|--:|
| 8   | 35.5 |   4 995 | 139 |   6 361 |   141× |    46× |
| 64  | 35.2 |  32 469 | 134 |  39 429 |   922× |   294× |
| 512 | 36.1 | 244 717 | 137 | 315 918 | 6 780× | 2 310× |

The flat-vs-linear story at its extreme: a field read is **~35 ns regardless of
record size**, a ~6 800× gap by 512 line items. Array write `lines[*].name`:

| size | eo | naive | monocle | eo speedup |
|--:|--:|--:|--:|--:|
| 8   |    663 |   6 552 |   6 895 | 9.9× |
| 64  |  4 758 |  40 987 |  42 613 | 8.6× |
| 512 | 37 549 | 334 374 | 372 100 | 8.9× |

EO wins ~9× *even on a full-array write* — Avro's per-element decode is costly
enough that walking to the focused leaf and rebuilding one parent beats decoding
every line item.

### jsoniter — `JsoniterPrism` / `JsoniterTraversal` over `Array[Byte]`

`native` is a hand-rolled `JsonReader` that walks to the focus and `skip()`s
every sibling — the optimum a jsoniter expert writes, which eo automates.

| metric | size | eo | native | naive | monocle | eo vs naive |
|---|--:|--:|--:|--:|--:|--:|
| `street` read   |   8 | 172 |    732 |   1 972 |   1 984 |  11.5× |
| `street` read   |  64 | 172 |  3 662 |  11 959 |  11 758 |    70× |
| `street` read   | 512 | 173 | 27 714 |  97 917 |  94 772 |   565× |
| `street` modify | 512 | 5 801 | — | 175 728 | 177 531 | 30× |
| `lines[*].price` fold | 512 | 79 243 | 62 597 | 101 733 | 410 561 | 1.3× |

The byte-walk read is **flat at ~172 ns**; even the hand-rolled `native` scan
scales (it tokenises everything it passes), so eo beats it ~4×. The `modify`
splice is O(bytes) so it grows mildly but stays ~30× under a re-encode. The
**fold** must touch every element, so eo lands near `native` and only ~1.3×
under `naive`.

**eo-jsoniter vs eo-circe** (the `JsoniterBench` cross-EO comparison, ~250-byte
fixture, CI): on one-shot reads — parse + drill — jsoniter beats circe **~8–16×**
because circe pays `circeParse` every call; the gap narrows to ~2.4× on array
folds where per-element work dominates. The
[design spike](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-eo-jsoniter-spike.md)
covers the carrier choice and splice mechanics.

## PowerSeries traversal with downstream composition

EO-only — no Monocle equivalent. `Traversal.each` (carrier `MultiFocus[PSVec]`)
is the composition vehicle for all three chain shapes; `naive` is the
hand-written `copy` + `map` equivalent.

| Chain (bench) | Size | eo | naive | ratio |
|---|--:|--:|--:|--:|
| `Lens → each → Lens` (`PowerSeriesBench`) | 4 | 66 | 13 | 5.1× |
| | 32 | 275 | 80 | 3.4× |
| | 256 | 1 890 | 726 | 2.6× |
| | 1024 | 6 927 | 3 633 | 1.9× |
| 5-hop tree (`PowerSeriesNestedBench`) | 4 | 348 | 65 | 5.4× |
| | 32 | 1 175 | 497 | 2.4× |
| | 256 | 8 085 | 3 376 | 2.4× |
| `each → Prism`, 50/50 hit (`PowerSeriesPrismBench`) | 8 | 59 | 12 | 5.0× |
| | 64 | 408 | 73 | 5.6× |
| | 512 | 3 610 | 660 | 5.5× |

All three scale **linearly**; the ratio to naive tightens as size grows (fixed
per-op setup amortises) — down to 1.9× on the dense Lens chain at 1024 elements.
The sparse-Prism shape holds ~5.5× because the miss branch carries inherent
per-element plumbing.

Under the hood the carrier pairs an existential leftover with a flat `PSVec`
focus vector (`Array[AnyRef]` + an `(offset, length)` window), and two internal
singleton markers (`MultiFocusSingleton` for always-hit Lens morphs,
`MultiFocusPSMaybeHit` for maybe-miss Prism/Optional morphs) let the hot path
write directly into pre-sized builders instead of allocating a per-element
wrapper. The full mechanics and the −59 %…−67 % optimisation history are in the
[composition notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#composition-notes).

## Reproducing

The integration tables are produced by the **Benchmarks** CI workflow
(`.github/workflows/benchmarks.yml`, manual `workflow_dispatch`) — it uploads a
`jmh-results.json` artifact and renders a summary table to the run page.

Locally, the `bench` / `benchQuick` sbt aliases bake in the standard config
(append a JMH filter to scope):

```sh
sbt bench                          # -f 3 -wi 3 -i 5, whole suite
sbt "bench .*OrderAvroBench.*"     # one class
sbt benchQuick                     # -f 1 -wi 2 -i 3, fast + noisy
```

GC and stack profilers help when a number is surprising:

```sh
sbt "bench -prof gc .*LensBench.*"
sbt "bench -prof stack .*PowerSeries.*"
```
