# Benchmarks

JMH numbers from [`benchmarks/`](https://github.com/Constructive-Programming/eo/tree/main/benchmarks),
in three groups:

1. **[Optic micro-benchmarks](#optic-micro-benchmarks)** — per-call overhead over
   hand-written field access, with [Monocle](https://www.optics.dev/Monocle/)
   alongside as the other optics library on the operations both implement.
2. **[Integration: edit without decoding](#integration-edit-without-decoding)** —
   one realistic `Order` document edited through circe / avro / jsoniter,
   measured against the decode-modify-encode you'd write by hand (and Monocle,
   which pays the same round-trip).
3. **[PowerSeries composition](#powerseries-traversal-with-downstream-composition)** —
   composed multi-focus traversal chains, measured against the hand-written
   `copy` + `map` and the equivalent Monocle composition.

## How these were measured

All figures are **average time per operation in nanoseconds (ns/op); lower is
better.** Absolute numbers vary by hardware — the *ratios* are the durable
signal.

Every table comes from the reproducible **`benchmarks.yml` CI workflow** (always
the same environment — ubuntu-22.04 / temurin@21, `-f 3 -wi 3 -i 5`, error
half-widths ≤ ~5 %, mostly ≤ 2 %), and anyone with Actions access can re-run it.
The figures are gathered across **several runs** of that workflow, though — each
table is refreshed when its bench changes, so a table is internally consistent
(one run) but two tables may come from different run instances. Since each run is
a fresh shared VM, treat *cross-table* absolute comparisons loosely; within a
table, and for the ratios everywhere, the signal holds.

> **JMH caveat.** A shared CI runner still isn't a tuned, quiet desktop, so read
> these as reproducible *directional* data — the shape and the ratios — not
> sub-nanosecond truth. See [Reproducing](#reproducing) and
> [`benchmarks/README.md`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md).

## Optic micro-benchmarks

For a single operation the hand-written baseline is direct field access
(`order.id`) or a `copy` — sub-nanosecond, the floor any optic adds overhead
over. The honest question these tables answer is *how little* an optic costs over
that floor. `eo` is the cats-eo method; `Monocle` is the same operation in the
other optics library, shown alongside as a reference point — the `ratio` column,
where present, is the two libraries head to head.

**Lens** (`Tuple2` carrier) — shallow `order.id` and a depth-3 `customer.address.street`:

| Operation | eo | Monocle | ratio |
|---|--:|--:|--:|
| `get` (`id`)          |  1.15 |  1.30 | 1.13× |
| `replace` (`id`)      |  4.71 |  4.75 | 1.01× |
| `modify` (`id`)       |  5.17 |  5.24 | 1.01× |
| `modify` (deep `street`) | 35.36 | 30.93 | 0.87× |

The cost over hand-written field access is essentially nil. `GetReplaceLens`
stores `get` / `enplace` as plain fields and specialises its fused `modify`, so
the hot path is a two-function composition — no `(X, A)` tuple allocation — and
`get` lands within a few tenths of a ns of a bare `order.id`. Monocle sits in the
same place. **Monocle is faster on the depth-3 `street` modify** (~1.15×): both
rebuild the same three records through a fused composed optic (EO's `inline`
`GetReplaceLens.andThen`, Monocle's composed `Lens`), and EO's per-hop
`get`/`enplace` closure pair carries a touch more indirection than Monocle's
purpose-built `case class Lens` — a small constant that first shows at depth 3.

**Prism** (`Either` carrier) — `Option[Int]` plus an `Either[String, Int]`
Right-prism:

| Operation | eo | Monocle |
|---|--:|--:|
| `getOption` Some | 1.01 | 1.15 |
| `getOption` None | 1.01 | 1.15 |
| `reverseGet`     | 2.77 | 2.94 |
| Right `getOption` (Right) | 2.67 | 2.82 |
| Right `getOption` (Left)  | 1.15 | 1.30 |
| Right `reverseGet`        | 2.64 | 2.93 |

**Iso** (`Direct` carrier) — `Address ↔ (String, String, String, String)`:

| Operation | eo | Monocle |
|---|--:|--:|
| `get`        | 4.58 | 4.68 |
| `reverseGet` | 3.98 | 4.11 |

`BijectionIso` stores both directions as plain fields — same shape and
direct-call hot path as Monocle's `case class Iso`. (`Direct` is now an `opaque
type`; the `wrap` / `unwrap` at the carrier boundary are `transparent inline`
identities, so the hot path is unchanged.)

**Optional** (`Affine` carrier) — leaf `Option[String]`, composed through a
`Nested0..6` Lens chain via cross-carrier `.andThen` (the `Morph[Tuple2, Affine]`
auto-lifts each hop):

| Operation | eo | Monocle |
|---|--:|--:|
| `modify_0` (Some)  | 29.07 | 23.62 |
| `modify_0` (None)  |  1.15 |  1.01 |
| `replace_0`        |  6.00 |  5.92 |
| `modify_3`         | 45.12 | 68.56 |
| `modify_6`         | 79.49 | 114.49 |
| `loyaltyId` (Some) | 21.26 | 18.28 |
| `loyaltyId` (None) |  1.15 |  1.16 |

At composition depth the per-hop cost stays low: `modify_3` and `modify_6` track
the work of a hand-written nested `copy` with an `Option` match at the leaf, since
the fused `andThen` composers are `inline` — each compose site splices distinct
lambdas, so a deep chain doesn't reuse one shared `andThen$$anonfun$` bytecode and
trip C2's recursive-inline cap (before that, `modify_6` was ~1.6× slower). Monocle
lands a little behind here (`modify_3` ~1.5×, `modify_6` ~1.4×). **Monocle is
faster at the single-hit leaf** (`modify_0` / `loyaltyId` Some) — its
`Option`-specialised internals shave the per-hit cost EO's generic `Affine` leaf
carries to stay uniform across families. The `loyaltyId` rows are the canonical
`customer.loyaltyId: Option[String]` focus (in memory — Avro omits it as a union),
Some and None branches.

**Getter / Modify** (`Direct` / `ModifyF`) — depth-0/3/6 over `Nested`. Both
families compose through the fused **`inline` `andThen`** on their concrete
subclasses (`Getter` / `Modify`), so every row builds a *composed*
optic on both sides and dispatches through it once — apples-to-apples with
Monocle's composed `Getter`/`Setter`.

| Depth | Getter eo | Getter Monocle | Setter eo | Setter Monocle |
|---|--:|--:|--:|--:|
| `_0` |  1.01 |  0.58 |  3.18 |  3.20 |
| `_3` |  5.01 |  8.39 | 12.61 | 28.92 |
| `_6` | 10.76 | 22.30 | 25.94 | 54.76 |

At composition depth both families stay close to the hand-written baseline — a
depth-N chain of `.get` calls, or a nested `copy` for the modifier. The lever is
`inline` on the same-carrier `andThen`: each compose site splices a *distinct*
lambda, so a depth-N chain becomes distinct synthetic methods per level. A plain
`def` reuses one shared `andThen$$anonfun$` bytecode across the chain, which C2
reads as recursion and caps (`MaxRecursiveInlineLevel`), leaving the deep tail as
virtual `Function1.apply`; splicing distinct lambdas sidesteps that cap with no
JVM flag. Modify (benchmarked as `SetterBench`; Monocle's family is still `Setter`) additionally sheds its per-hop `ModifyF` allocation (the fused
`Modify` writes through `modifyFn` directly: depth-6 800→120 B/op). Monocle
gets the same un-capped inlining from a fresh anonymous class per compose, and
trails here (~1.7–2.3× at `_3`/`_6`). **Monocle is faster at the scalar leaf**
(`_0`, `order.id`) by a few tenths of a ns — the sub-nanosecond floor where its
specialised case classes shave the last field load and EO's generic carrier does
not.

**Fold / Traversal** — Fold `foldMap(identity)` over `List[Int]`; Traversal
`each.modify` over the canonical `Order.lines` (bump each line's `qty`), sweeping
size:

| Size | Fold eo | Fold Monocle | Traversal eo | Traversal Monocle | Traversal speedup |
|---|--:|--:|--:|--:|--:|
| 8   |    20.7 |    21.3 |   119.8 |    341.1 | 2.8× |
| 64  |   293.8 |   281.6 |   917.8 |  2 289.7 | 2.5× |
| 512 | 3 770.1 | 3 634.0 | 7 874.8 | 37 929.5 | 4.8× |

The hand-written reference is a bare `foldLeft` for the fold and an `xs.map` +
`copy` for the traversal. **Fold is on par with Monocle** (0.97–1.04× across sizes)
— both collapse to the *same* `cats.Foldable[List].foldMap`, one `Monoid.combine`
per element, exactly what a hand-written `foldMap` does. EO's `Fold.apply` returns a
concrete `ForgetFold` whose eager `foldMap` member folds straight through the
captured `Foldable[F]` — the same stored-method shape as Monocle's
`Fold.fromFoldable`. Before that specialisation EO routed every fold through the
generic `Optic.foldMap` extension, and paid for it: ~5× at size 8 (`109 → 21` ns),
~2.9× at 64, ~1.8× at 512. That overhead was a per-*call* constant (the
`ForgetfulFold[Forget[F]]` summon, an intermediate `S => M` closure, and a
box/unbox), **not** per-element — which is why it dominated small folds and faded
into the asymptote on large ones. It was also invisible to `-prof gc` (the two were
always allocation-identical at 14 080 B/op @512; escape analysis elides the
closure, so the cost was cycles, not bytes — only CI's low-noise timing resolves
it). The traversal is where the carriers genuinely diverge: EO's `each` (carrier
`MultiFocus[PSVec]`) collects element references into a flat focus vector and
rebuilds via `Functor[PSVec].map`, where Monocle wraps each element in
`Applicative[Id]` — so EO tracks the hand-written `map` more closely and
Monocle trails, widening to ~4.8× by 512 line items (each element pays a `LineItem`
copy that dwarfs the carrier difference).

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
| 8   | 1 156 | 1 146 | 1 180 | 1 078 |   4 121 |   4 132 |   3.6× |
| 64  | 1 173 | 1 166 | 1 168 | 1 090 |  26 141 |  26 104 |    22× |
| 512 | 1 154 | 1 153 | 1 207 | 1 075 | 205 532 | 205 971 |   178× |

The edit is **flat** (~1.15 µs at any size); `naive` / `monocle` scale with the
whole payload. `direct` `JsonObject` surgery is the fastest hand form (what
`JsonPrism` mirrors); `hcursor` is competitive. On this scalar path the `Ior`
surface is within noise of `*Unsafe` (≤~10 ns); the per-element `Ior` cost shows
up only on the array traversal below.

Array write-traversal, `lines[*].name`:

| size | eo (Unsafe) | eo (Ior) | hcursor | direct | naive | monocle |
|--:|--:|--:|--:|--:|--:|--:|
| 8   |   4 561 |   4 791 |   4 258 |   4 298 |   4 405 |   4 861 |
| 64  |  32 340 |  35 890 |  31 749 |  31 467 |  27 823 |  29 390 |
| 512 | 259 776 | 282 207 | 244 897 | 241 236 | 222 137 | 259 769 |

Honest result: a whole-array rewrite is O(elements) for everyone, so the cursor
walk has no *structural* edge — but EO's `JsonTraversal` now lands right on the
hand-written cursor / AST forms (`eo` ≈ `hcursor` ≈ `direct`) and is on par with
decode-modify-encode through Monocle, after the per-element path stopped
re-allocating its walk state (`JsonWalk` uses flat index loops, not per-element
`Array→Vector`/`zip`/`foldRight`). It still trails `naive` by ~1.2× at 512 —
a bulk decode-map-encode is the most cache-friendly way to rewrite *every*
element — so reach for JSON traversals for composition, diagnostics, and
pinpoint edits, not raw whole-array throughput. (Avro below is the opposite,
because its per-element decode is so costly.)

### avro — `AvroPrism` / `AvroTraversal` over `IndexedRecord`

Scalar `customer.address.street` (`loyaltyId` is omitted — kindlings encodes
`Option` as a union, navigated via `.union[Branch]`):

| size | eo read | naive read | eo modify | naive modify | read speedup | modify speedup |
|--:|--:|--:|--:|--:|--:|--:|
| 8   | 39.0 |   5 016 | 133 |   6 339 |   129× |    48× |
| 64  | 39.7 |  32 405 | 135 |  40 284 |   816× |   298× |
| 512 | 40.1 | 246 338 | 134 | 310 277 | 6 143× | 2 309× |

The flat-vs-linear story at its extreme: a field read is **~40 ns regardless of
record size**, a ~6 100× gap by 512 line items. Array write `lines[*].name`:

| size | eo | naive | monocle | eo speedup |
|--:|--:|--:|--:|--:|
| 8   |    662 |   6 790 |   6 875 | 10.3× |
| 64  |  4 602 |  41 776 |  43 707 | 9.1× |
| 512 | 36 370 | 330 871 | 369 396 | 9.1× |

EO is ~9–10× faster than the hand-written decode-modify-encode *even on a full-array
write* — Avro's per-element decode is costly enough that walking to the focused
leaf and rebuilding one parent beats decoding every line item. (`monocle` here is
that same round-trip, since Monocle has no `IndexedRecord` carrier.)

### jsoniter — `JsoniterPrism` / `JsoniterTraversal` over `Array[Byte]`

`native` is a hand-rolled `JsonReader` that walks to the focus and `skip()`s
every sibling — the optimum a jsoniter expert writes, which eo automates.

| metric | size | eo | native | naive | monocle | eo vs naive |
|---|--:|--:|--:|--:|--:|--:|
| `street` read   |   8 |   174 |    722 |   1 987 |   2 001 |  11.4× |
| `street` read   |  64 |   175 |  3 648 |  12 069 |  12 000 |   69× |
| `street` read   | 512 |   173 | 27 849 | 98 683 | 97 786 |  569× |
| `street` modify | 512 | 5 743 |     — | 179 257 | 180 254 |   31× |
| `lines[*].price` fold | 512 | 83 149 | 62 743 | 104 683 | 415 919 | 1.3× |

The byte-walk read is **flat at ~174 ns**; even the hand-rolled `native` scan
scales (it tokenises everything it passes), so eo beats it ~4× at size 8 and
pulls further ahead with size. The `modify` splice is O(bytes) so it grows mildly
but stays ~31× under a re-encode. The **fold** must touch every element, so eo
lands near `native` and only ~1.3× under `naive`.

**eo-jsoniter vs eo-circe** (the `JsoniterBench` cross-EO comparison, on the
canonical `Order` swept over size 8/64/512, CI): eo-circe parses the whole
document each call (`circeParse` then drill), so it is O(size); the eo-jsoniter
byte-walk is flat. One-shot scalar read (`$.id`): jsoniter ~36 ns at *every* size
vs circe 4 154 → 212 204 ns, a gap that grows from **~110× (size 8) to ~6 000×
(512)** as the parse cost climbs. `.replace`/`.modify $.id`: jsoniter 133 → 5 696
ns (O(bytes) splice) vs circe ~8 400 → ~396 000 ns, **~59–78×**. The array
`lines[*].price` fold narrows to **~4.5×** — both must visit every element. (A
deliberately-absent `$.customer.absent` miss costs eo-jsoniter a flat ~158 ns;
there is no honest circe peer, since a typed codec can't drill an absent field.)
The
[design spike](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-eo-jsoniter-spike.md)
covers the carrier choice and splice mechanics.

## PowerSeries traversal with downstream composition

Three composed-traversal chains. `naive` is the hand-written `copy` + `map`
equivalent — the baseline that matters. `monocle` is the *same* composition built
with Monocle (`Lens.andThen(Traversal.fromTraverse).andThen(Lens)`,
`Traversal.andThen(Prism)`, nested traversals) — these compositions are first-class
there too, so it's a fair peer. `Traversal.each` (carrier `MultiFocus[PSVec]`) is
EO's vehicle for all three; a `@Setup` guard asserts the three paths agree.

| Chain (bench) | N | eo | naive | monocle | eo ÷ naive |
|---|--:|--:|--:|--:|--:|
| `Lens → each → Lens` (`PowerSeriesBench`) | 4 | 136 | 28 | 218 | 4.8× |
| | 16 | 295 | 94 | 694 | 3.1× |
| | 64 | 934 | 340 | 2 620 | 2.8× |
| | 256 | 3 637 | 1 338 | 22 745 | 2.7× |
| | 1024 | 15 622 | 5 327 | 63 100 | 2.9× |
| | 4096 | 61 250 | 21 918 | 215 527 | 2.8× |
| 5-hop tree (`PowerSeriesNestedBench`) | 4 | 809 | 145 | 1 357 | 5.6× |
| | 16 | 1 528 | 400 | 2 981 | 3.8× |
| | 64 | 4 449 | 1 418 | 10 657 | 3.1× |
| | 256 | 17 370 | 5 560 | 97 615 | 3.1× |
| | 1024 | 68 147 | 22 809 | 260 893 | 3.0× |
| `each → Prism`, 50/50 hit (`PowerSeriesPrismBench`) | 8 | 150 | 27 | 333 | 5.5× |
| | 32 | 428 | 94 | 1 164 | 4.5× |
| | 128 | 1 722 | 385 | 4 472 | 4.5× |
| | 512 | 7 918 | 1 629 | 33 642 | 4.9× |
| | 2048 | 32 097 | 6 560 | 110 498 | 4.9× |

Both `eo` and the hand-written `naive` track **O(N)**. The test is per-element
cost (`ns ÷ element`, traversing `N` leaves — `4N` for the nested tree): once the
fixed per-op setup amortises past the smallest `N`, it flattens to a constant.

| Chain | eo `ns ÷ element` (N small → large) | naive `ns ÷ element` | log-log slope (eo / naive) |
|---|---|---|--:|
| `Lens → each → Lens` | 34 → 18 → 15 → 14 → 15 → 15 | 7.1 → 5.9 → 5.3 → 5.2 → 5.2 → 5.4 | 0.90 / 0.96 |
| 5-hop tree | 51 → 24 → 17 → 17 → 17 | 9.1 → 6.2 → 5.5 → 5.4 → 5.6 | 0.81 / 0.92 |
| `each → Prism` | 19 → 13 → 13 → 15 → 16 | 3.4 → 3.0 → 3.0 → 3.2 → 3.2 | 0.98 / 1.00 |

A least-squares fit on `log(time)` vs `log(N)` gives slopes of **0.8–1.0** for
every eo and naive series (a slope of 1 is exact linearity; the dip below 1 is the
small-`N` fixed cost, which makes the curve concave — i.e. it rules out
*super*-linear growth, not linearity). Error bars are ≤3.1% on all eo/naive
points, so the flattening is signal, not noise. The `eo ÷ naive` overhead settles
at ~2–3× on the dense Lens and nested chains and ~4.5–5× on the sparse Prism (whose
miss branch carries inherent per-element plumbing). `monocle` is shown for
reference; its scaling on the shared runner is noisier and not characterised here.

Under the hood the carrier pairs an existential leftover with a flat `PSVec`
focus vector (`Array[AnyRef]` + an `(offset, length)` window), and two internal
singleton markers (`MultiFocusSingleton` for always-hit Lens morphs,
`MultiFocusPSMaybeHit` for maybe-miss Prism/Optional morphs) let the hot path
write directly into pre-sized builders instead of allocating a per-element
wrapper. The full mechanics and the −59 %…−67 % optimisation history are in the
[composition notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#composition-notes).

## Plated recursion — read (`universe`) + write (`transform`) vs a hand visitor (and Monocle)

`PlatedBench` measures cats-eo's `Plated` against the hand-written recursion
("visitor") you'd write without optics — the baseline that matters — with
Monocle's `monocle.function.Plated` alongside for reference, over three
subjects: a normal-depth `Expr` tree, a degenerate deep `Bin` spine, and a
normal-depth circe `Json` tree (via the universal `Plated[Json]`). The `Plated`
carrier is PSVec-native, so neither path converts to a `List` and back.

| Op | Subject | eo | monocle | visitor | eo ÷ visitor |
|---|---|--:|--:|--:|--:|
| `universe` | `Expr` balanced | 149 769 | 3 282 289 | 58 253 | 2.6× |
| | `Json` balanced | 208 270 | 3 452 301 | 145 936 | 1.4× |
| | `Bin` deep spine | 148 040 | **SO** | 74 868 | 2.0× |
| `transform` | `Expr` balanced | 150 029 | 180 014 | 73 318 | 2.0× |
| | `Bin` deep spine | 153 204 | **SO** | 49 453 | 3.1× |

(ns/op at n=4096, from the reproducible CI benchmarks workflow — 3-fork on the
shared runner, so absolute numbers run higher than a quiet desktop; the ratios are
the signal and sub-~1.5× differences aren't meaningful. **SO** = `StackOverflowError`.)

Three results:

- **`universe` rivals the hand-written visitor.** Reading children straight off
  the PSVec carrier (no `List` round-trip, explicit worklist) puts the JSON
  subject ~1.4× the bare visitor and `Expr` ~2.6× — close to the recursion
  you'd write by hand, while staying composable through `.andThen`.
  (Monocle is ~16–22× slower here, its lazy-`#:::` append going quadratic, and is
  *not stack-safe*: both `universe` and `transform` `StackOverflowError` on the
  degenerate spine at depth ≳2048. EO clears the spine at every size here and at
  100k in the stack-safety test, so the deep rows compare EO against the visitor
  only.)
- **`transform` sits within ~2–3× of the hand-written visitor and is stack-safe.**
  It went from an
  `Eval` trampoline (was ~10× slower) to a **hybrid**: a direct call-stack
  recursion (≈ a hand-written rebuild — no per-node heap `Frame`) while shallow,
  handing any subtree past a depth bound to the heap-stack machine so a
  degenerate spine still can't overflow. (`rewrite` keeps its own `cats.Eval`
  trampoline — it stays stack-safe on *both* the descent and a long re-fire
  chain, which a synchronous machine would put back on the call stack.) With
  `childrenVec` / `rebuild` (no `to`/`from` tuple per node) and
  leaves applied in place, allocation is ~80 B/node on `Expr` (visitor is 44)
  and ~76 B/node on the deep spine (the heap-stack fallback the degenerate spine
  takes at this size). Both paths sit within **~2–3×** of the bare visitor.

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
