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

Every table on this page comes from the same reproducible **`benchmarks.yml` CI
workflow** (ubuntu-22.04 / temurin@21, `-f 3 -wi 3 -i 5`, error half-widths
≤ ~5 %, mostly ≤ 2 %), so the numbers are comparable across sections and anyone
with Actions access can re-run them.

> **JMH caveat.** A shared CI runner still isn't a tuned, quiet desktop, so read
> these as reproducible *directional* data — the shape and the ratios — not
> sub-nanosecond truth. See [Reproducing](#reproducing) and
> [`benchmarks/README.md`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md).

## Optic micro-benchmarks (vs Monocle)

`eo` is the cats-eo method, `Monocle` the equivalent; `ratio` is `naive`-free —
just the two optic libraries head to head.

**Lens** (`Tuple2` carrier) — `Person.age`:

| Operation | eo | Monocle | ratio |
|---|--:|--:|--:|
| `get`     | 1.03 | 1.21 | 1.17× |
| `replace` | 2.56 | 2.68 | 1.05× |
| `modify`  | 2.97 | 2.94 | 0.99× |

Effectively parity. `GetReplaceLens` stores `get` / `enplace` as plain fields
and specialises its fused `modify`, so the hot path is a two-function
composition — no `(X, A)` tuple allocation — matching Monocle's hand-written
`case class Lens`.

**Prism** (`Either` carrier) — `Option[Int]` plus an `Either[String, Int]`
Right-prism:

| Operation | eo | Monocle |
|---|--:|--:|
| `getOption` Some | 0.93 | 1.06 |
| `getOption` None | 0.93 | 1.06 |
| `reverseGet`     | 2.33 | 2.40 |
| Right `getOption` (Right) | 2.50 | 2.65 |
| Right `getOption` (Left)  | 1.08 | 1.17 |
| Right `reverseGet`        | 2.34 | 2.41 |

**Iso** (`Direct` carrier) — `(Int, String) ↔ Person`:

| Operation | eo | Monocle |
|---|--:|--:|
| `get`        | 3.23 | 3.19 |
| `reverseGet` | 2.54 | 2.71 |

`BijectionIso` stores both directions as plain fields — same shape and
direct-call hot path as Monocle's `case class Iso`.

**Optional** (`Affine` carrier) — leaf `Option[String]`, composed through a
`Nested0..6` Lens chain via cross-carrier `.andThen` (the `Morph[Tuple2, Affine]`
auto-lifts each hop):

| Operation | eo | Monocle |
|---|--:|--:|
| `modify_0` (Some)  |  26.92 | 23.05 |
| `modify_0` (None)  |   1.51 |  0.99 |
| `replace_0`        |   5.25 |  3.72 |
| `modify_3`         |  55.68 | 66.72 |
| `modify_6`         | 180.55 | 108.86 |

Competitive across depths: the cross-carrier Lens→Optional chain actually
*beats* Monocle's native composition at depth 3 (the `Morph[Tuple2, Affine]`
lift fuses well), and trails ~1.7× at depth 6 where `Affine`'s per-hop branching
adds up against Monocle's `Option`-specialised internals.

**Getter / Setter** (`Direct` / `SetterF`) — depth-0/3/6 over `Nested`.
Neither family composes through `Optic.andThen` in EO today (see
[Optics → Getter](optics.md#getter)), so the `_3` / `_6` EO numbers nest `.get`
/ `.modify` calls by hand; Monocle composes natively.

| Depth | Getter eo | Getter Monocle | Setter eo | Setter Monocle |
|---|--:|--:|--:|--:|
| `_0` | 0.95 |  0.54 |  2.96 |  2.38 |
| `_3` | 2.74 |  8.82 | 51.42 | 26.46 |
| `_6` | 4.55 | 27.65 | 88.23 | 52.35 |

EO resolves `.get` against each carrier's `Accessor` statically, so its composed
read inlines to a direct call — hence the widening Getter win at depth (~6× by
depth 6). On write, the nested-`modify` shape costs EO ~1.7× Monocle's native
`Setter.andThen`.

**Fold / Traversal** — `foldMap(identity)` and `each.modify(_ + 1)` over
`List[Int]`, sweeping size:

| Size | Fold eo | Fold Monocle | Traversal eo | Traversal Monocle | Traversal speedup |
|---|--:|--:|--:|--:|--:|
| 8   |  109.4 |    21.9 |    87.5 |    216.6 | 2.5× |
| 64  |  932.4 |   313.5 |   620.7 |  1 528.3 | 2.5× |
| 512 | 8 007.9 | 4 728.1 | 5 218.6 | 34 875.0 | 6.7× |

Monocle's `Fold` reduces to a direct `Foldable.foldMap`; EO's `Forget[F]` adds a
per-element dispatch layer, so Monocle wins Fold (~1.7–5×). Traversal flips it:
EO's `each` (carrier `MultiFocus[PSVec]`) collects element references into a flat
focus vector and rebuilds via `Functor[PSVec].map`, beating Monocle's
per-element `Applicative[Id]` wrapping — a gap that widens to ~6.7× by 512
elements.

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
| `Lens → each → Lens` (`PowerSeriesBench`) | 4 | 152 | 26 | 5.8× |
| | 32 | 504 | 203 | 2.5× |
| | 256 | 3 409 | 1 616 | 2.1× |
| | 1024 | 14 364 | 5 530 | 2.6× |
| 5-hop tree (`PowerSeriesNestedBench`) | 4 | 754 | 130 | 5.8× |
| | 32 | 2 368 | 698 | 3.4× |
| | 256 | 15 210 | 4 804 | 3.2× |
| `each → Prism`, 50/50 hit (`PowerSeriesPrismBench`) | 8 | 146 | 25 | 5.8× |
| | 64 | 742 | 169 | 4.4× |
| | 512 | 6 755 | 1 292 | 5.2× |

All three scale **linearly**; the ratio to naive is largest on tiny inputs
(fixed per-op setup dominates) and settles around 2–3× on the dense Lens and
nested-tree chains as the element work amortises. The sparse-Prism shape holds
~5× because the miss branch carries inherent per-element plumbing.

Under the hood the carrier pairs an existential leftover with a flat `PSVec`
focus vector (`Array[AnyRef]` + an `(offset, length)` window), and two internal
singleton markers (`MultiFocusSingleton` for always-hit Lens morphs,
`MultiFocusPSMaybeHit` for maybe-miss Prism/Optional morphs) let the hot path
write directly into pre-sized builders instead of allocating a per-element
wrapper. The full mechanics and the −59 %…−67 % optimisation history are in the
[composition notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#composition-notes).

## Plated recursion — read (`universe`) + write (`transform`) vs Monocle vs a hand visitor

`PlatedBench` pits cats-eo's `Plated` against Monocle's `monocle.function.Plated`
and the hand-written recursion ("visitor") you'd write without optics, over three
subjects: a normal-depth `Expr` tree, a degenerate deep `Bin` spine, and a
normal-depth circe `Json` tree (via the universal `Plated[Json]`). The `Plated`
carrier is PSVec-native, so neither path converts to a `List` and back.

| Op | Subject | eo | monocle | visitor |
|---|---|--:|--:|--:|
| `universe` | `Expr` balanced | 15 000 | 172 100 | 6 800 |
| | `Json` balanced | 20 300 | 207 900 | 16 100 |
| | `Bin` deep spine | 15 600 | **SO** | 6 900 |
| `transform` | `Expr` balanced | 18 200 | 14 700 | 8 400 |
| | `Bin` deep spine | 13 000 | **SO** | 4 100 |

(ns/op at n=512, 3-fork; **indicative only** — this machine is noisy enough that
sub-2× differences aren't significant. The CI workflow produces the canonical
figures.)

Three results:

- **Monocle's `Plated` is not stack-safe.** On the degenerate spine both
  `universe` and `transform` `StackOverflowError` at depth ≳2048 (and `universe`,
  where it survives at 1024, runs ~700× slower than EO — its lazy-`#:::` append
  going quadratic). EO clears the spine at every size here and at 100k in the
  stack-safety test, so the deep rows compare EO against the visitor only.
- **`universe` beats Monocle by ~10–12× and rivals the visitor.** Reading
  children straight off the PSVec carrier (no `List` round-trip, explicit
  worklist) puts the JSON subject ~on par with the bare visitor (~1.3×) and
  `Expr` within ~2×.
- **`transform` is on par with Monocle and stack-safe.** It went from an
  `Eval` trampoline (was ~10× slower) to a **hybrid**: a direct call-stack
  recursion (≈ a hand-written rebuild — no per-node heap `Frame`) while shallow,
  handing any subtree past a depth bound to the heap-stack machine so a
  degenerate spine still can't overflow. (`rewrite` keeps its own `cats.Eval`
  trampoline — it stays stack-safe on *both* the descent and a long re-fire
  chain, which a synchronous machine would put back on the call stack.) With
  `childrenVec` / `rebuild` (no `to`/`from` tuple per node) and
  leaves applied in place, allocation is ~80 B/node on `Expr` (visitor is 44)
  and ~56 B/node on the deep spine. Both paths sit within **~2–3×** of the bare
  visitor.

The residual gap to the visitor is the **carrier materialisation**: even on the
fast recursive path EO allocates a `PSVec` of children + an `out` array per
internal node, where a hand visitor fuses extract-and-rebuild into one
`new Node(go(l), go(r))` and allocates neither — plus the heap stack the deep
fallback uses (so it never overflows; the visitor and Monocle both do on the
spine). Closing the last ~2–3× would mean fusing the recursion into the `plate[S]`
macro — but that emits a *function*, not an `Optic`, which would break the
`.andThen` composition `everywhere` relies on, so it's deliberately not done.

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
