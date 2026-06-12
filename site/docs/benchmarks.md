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
signal. Where `-prof gc` data is available, **B/op (`·gc.alloc.rate.norm`) is the
trustworthy metric** on a shared runner; ns/op is directional.

All tables come from a single reproducible **`benchmarks.yml` CI run** (run
27340593833, 2026-06-11): ubuntu-22.04 / temurin@21, full `.*` sweep,
`-f 3 -wi 3 -i 5 -prof gc` — except the recursion-scheme tables, which come
from a dedicated `.*SchemesBench.*` run (27398242244, 2026-06-12) with the same
config. Every table is internally consistent — all numbers within a table are
from the same run instance, so cross-table absolute comparisons are valid.
Anyone with Actions access can re-run it.

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

| Operation | eo (ns) | eo (B/op) | Monocle (ns) | Monocle (B/op) | ratio |
|---|--:|--:|--:|--:|--:|
| `get` (`id`)             |  1.09 |   0 |  1.27 |   0 | 1.17× |
| `replace` (`id`)         |  3.17 |  40 |  3.03 |  40 | 0.96× |
| `modify` (`id`)          |  3.78 |  40 |  3.99 |  40 | 1.05× |
| `modify` (deep `street`) | 34.37 | 152 | 31.34 | 176 | 0.91× |

The cost over hand-written field access is essentially nil. `GetReplaceLens`
stores `get` / `enplace` as plain fields and specialises its fused `modify`, so
the hot path is a two-function composition — no `(X, A)` tuple allocation — and
`get` lands within a few tenths of a ns of a bare `order.id`. Monocle sits in the
same place. **Monocle is faster on the depth-3 `street` modify** (~1.1×) in
ns/op, but **EO allocates less** (152 vs 176 B/op): both rebuild the same three
records through a fused composed optic (EO's `inline` `GetReplaceLens.andThen`,
Monocle's composed `Lens`), and EO's per-hop `get`/`enplace` closure pair carries
a touch more indirection than Monocle's purpose-built `case class Lens` — a
small constant that first shows at depth 3.

**Prism** (`Either` carrier) — `Option[Int]` plus an `Either[String, Int]`
Right-prism (alloc-identical at 0 B/op absent, 16 B/op present):

| Operation | eo (ns) | Monocle (ns) |
|---|--:|--:|
| `getOption` Some    | 0.95 | 1.11 |
| `getOption` None    | 0.95 | 1.11 |
| `reverseGet`        | 2.59 | 2.59 |
| Right `getOption` (Right) | 2.35 | 2.46 |
| Right `getOption` (Left)  | 1.12 | 1.24 |
| Right `reverseGet`        | 2.58 | 2.59 |

**Iso** (`Direct` carrier) — `Address ↔ (String, String, String, String)`
(alloc-identical at 32 B/op both ways):

| Operation | eo (ns) | Monocle (ns) |
|---|--:|--:|
| `get`        | 3.64 | 3.69 |
| `reverseGet` | 3.00 | 3.07 |

`BijectionIso` stores both directions as plain fields — same shape and
direct-call hot path as Monocle's `case class Iso`. (`Direct` is now an `opaque
type`; the `wrap` / `unwrap` at the carrier boundary are `transparent inline`
identities, so the hot path is unchanged.)

**Optional** (`Affine` carrier) — leaf `Option[String]`, composed through a
`Nested0..6` Lens chain via cross-carrier `.andThen` (the `Morph[Tuple2, Affine]`
auto-lifts each hop):

| Operation | eo (ns) | eo (B/op) | Monocle (ns) | Monocle (B/op) |
|---|--:|--:|--:|--:|
| `modify_0` (Some)  | 26.21 | 136 | 22.19 | 112 |
| `modify_0` (None)  |  0.94 |   0 |  0.94 |   0 |
| `replace_0`        |  4.31 |  40 |  3.21 |  40 |
| `modify_3`         | 45.32 | 184 | 73.11 | 304 |
| `modify_6`         | 87.97 | 232 | 118.60 | 496 |
| `loyaltyId` (Some) | 26.53 | 136 | 20.57 | 112 |
| `loyaltyId` (None) |  1.05 |   0 |  1.10 |   0 |

At composition depth the per-hop cost stays low: `modify_3` and `modify_6` track
the work of a hand-written nested `copy` with an `Option` match at the leaf, since
the fused `andThen` composers are `inline` — each compose site splices distinct
lambdas, so a deep chain doesn't reuse one shared `andThen$$anonfun$` bytecode and
trip C2's recursive-inline cap (before that, `modify_6` was ~1.6× slower). Monocle
trails at depth (`modify_3` ~1.6× ns and ~1.7× B/op; `modify_6` ~1.3× ns and
~2.1× B/op) — EO's opaque `Affine` carrier allocates less per hop as depth grows.
**Monocle is faster at the single-hit leaf** (`modify_0` / `loyaltyId` Some — ~1.2×
ns) because its `Option`-specialised internals shave the per-hit overhead EO's
generic `Affine` leaf carries to stay uniform across families; allocation at the leaf
also slightly favours Monocle (112 vs 136 B/op). The `loyaltyId` rows are the
canonical `customer.loyaltyId: Option[String]` focus (in memory — Avro omits it as
a union), Some and None branches.

**Getter / Modify** (`Direct` / `ModifyF`) — depth-0/3/6 over `Nested`. Both
families compose through the fused **`inline` `andThen`** on their concrete
subclasses (`Getter` / `Modify`), so every row builds a *composed*
optic on both sides and dispatches through it once — apples-to-apples with
Monocle's composed `Getter`/`Setter`.

| Depth | Getter eo (ns) | Getter M (ns) | Modify eo (ns) | Modify eo (B/op) | Modify M (ns) | Modify M (B/op) |
|---|--:|--:|--:|--:|--:|--:|
| `_0` |  0.94 |  0.50 |  2.21 |  24 |  2.22 |  24 |
| `_3` |  5.38 |  8.12 | 11.48 |  72 | 25.79 | 168 |
| `_6` | 11.82 | 25.16 | 26.16 | 120 | 59.22 | 288 |

At composition depth both families stay close to the hand-written baseline — a
depth-N chain of `.get` calls, or a nested `copy` for the modifier. The lever is
`inline` on the same-carrier `andThen`: each compose site splices a *distinct*
lambda, so a depth-N chain becomes distinct synthetic methods per level. A plain
`def` reuses one shared `andThen$$anonfun$` bytecode across the chain, which C2
reads as recursion and caps (`MaxRecursiveInlineLevel`), leaving the deep tail as
virtual `Function1.apply`; splicing distinct lambdas sidesteps that cap with no
JVM flag. Modify (benchmarked as `SetterBench`; Monocle's family is still `Setter`)
additionally sheds its per-hop `ModifyF` allocation via the fused `Modify` path,
saving 96 B/op at depth 3 and 168 B/op at depth 6 vs Monocle. Monocle trails in
both ns and B/op here (~2.2× ns and ~2.3× B/op at `_3`/`_6`). **Monocle is faster
at the scalar leaf** (`_0`, `order.id`) by a few tenths of a ns — the
sub-nanosecond floor where its specialised case classes shave the last field load
and EO's generic carrier does not. Getter is alloc-free at all depths (both
libraries, 0 B/op).

**Fold / Traversal** — Fold `foldMap(identity)` over `List[Int]`; Traversal
`each.modify` over the canonical `Order.lines` (bump each line's `qty`), sweeping
size:

| Size | Fold eo (ns) | Fold M (ns) | Traversal eo (ns) | Traversal eo (B/op) | Traversal M (ns) | Traversal M (B/op) | Traversal speedup |
|---|--:|--:|--:|--:|--:|--:|--:|
| 8   |    21.2 |    21.3 |   111.3 |    728 |    287.4 |  1 936 | 2.6× |
| 64  |   325.8 |   307.1 |   953.0 |  4 928 |  1 766.5 | 14 448 | 1.9× |
| 512 | 4 663.2 | 4 482.3 | 6 506.4 | 39 025 | 37 551.7 | 176 925 | 5.8× |

The hand-written reference is a bare `foldLeft` for the fold and an `xs.map` +
`copy` for the traversal. **Fold is on par with Monocle** (1.00–1.04× across sizes)
— both collapse to the *same* `cats.Foldable[List].foldMap`, one `Monoid.combine`
per element, exactly what a hand-written `foldMap` does. EO's `Fold.apply` returns a
concrete `ForgetFold` whose eager `foldMap` member folds straight through the
captured `Foldable[F]` — the same stored-method shape as Monocle's
`Fold.fromFoldable`. Before that specialisation EO routed every fold through the
generic `Optic.foldMap` extension, and paid for it: ~5× at size 8 (`109 → 20` ns),
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
Monocle trails **both in ns and in B/op**, reaching ~5.8× at 512 line items
(176 925 vs 39 025 B/op — ~4.5× allocation gap — and 37 552 vs 6 506 ns).

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

Scalar deep edit, `customer.address.street` (ns/op):

| size | eo (Unsafe) | eo (Ior) | hcursor | direct | naive | monocle | eo vs naive |
|--:|--:|--:|--:|--:|--:|--:|--:|
| 8   | 1 170 | 1 181 | 1 179 | 1 113 |   3 939 |   3 958 |   3.4× |
| 64  | 1 191 | 1 167 | 1 174 | 1 102 |  25 199 |  25 058 |    21× |
| 512 | 1 179 | 1 199 | 1 172 | 1 112 | 213 293 | 218 807 |   181× |

The edit is **flat** (~1.17 µs at any size); `naive` / `monocle` scale with the
whole payload. `direct` `JsonObject` surgery is the fastest hand form (what
`JsonPrism` mirrors); `hcursor` is competitive. On this scalar path the `Ior`
surface is within noise of `*Unsafe` (≤~20 ns); the per-element `Ior` cost shows
up only on the array traversal below.

Array write-traversal, `lines[*].name` (ns/op):

| size | eo (Unsafe) | eo (Ior) | hcursor | direct | naive | monocle |
|--:|--:|--:|--:|--:|--:|--:|
| 8   |   4 762 |   5 089 |   4 402 |   4 437 |   4 131 |   4 475 |
| 64  |  34 615 |  37 926 |  32 918 |  32 822 |  27 333 |  27 960 |
| 512 | 281 686 | 300 692 | 261 895 | 265 665 | 230 017 | 264 975 |

Honest result: a whole-array rewrite is O(elements) for everyone, so the cursor
walk has no *structural* edge — but EO's `JsonTraversal` lands close to the
hand-written cursor / AST forms (`eo` ≈ `hcursor` ≈ `direct`) and is comparable
with decode-modify-encode through Monocle, after the per-element path stopped
re-allocating its walk state (`JsonWalk` uses flat index loops, not per-element
`Array→Vector`/`zip`/`foldRight`). It still trails `naive` by ~1.2× at 512 —
a bulk decode-map-encode is the most cache-friendly way to rewrite *every*
element — so reach for JSON traversals for composition, diagnostics, and
pinpoint edits, not raw whole-array throughput. (Avro below is the opposite,
because its per-element decode is so costly.)

### avro — `AvroPrism` / `AvroTraversal` over `IndexedRecord`

Scalar `customer.address.street` (`loyaltyId` is omitted — kindlings encodes
`Option` as a union, navigated via `.union[Branch]`):

| size | eo read (ns) | naive read (ns) | eo modify (ns) | naive modify (ns) | read speedup | modify speedup |
|--:|--:|--:|--:|--:|--:|--:|
| 8   |  35 |   3 343 | 134 |   4 624 |    96× |    34× |
| 64  |  35 |  19 534 | 133 |  27 848 |   558× |   209× |
| 512 |  35 | 151 831 | 134 | 219 597 | 4 338× | 1 639× |

The flat-vs-linear story at its extreme: a field read is **~35 ns regardless of
record size**, a ~4 300× gap by 512 line items. Array write `lines[*].name` (ns/op):

| size | eo | naive | monocle | eo speedup |
|--:|--:|--:|--:|--:|
| 8   |    660 |   4 861 |   5 200 | 7.4× |
| 64  |  4 719 |  29 142 |  30 875 | 6.2× |
| 512 | 37 812 | 234 232 | 265 549 | 6.2× |

EO is ~6–7× faster than the hand-written decode-modify-encode *even on a full-array
write* — Avro's per-element decode is costly enough that walking to the focused
leaf and rebuilding one parent beats decoding every line item. (`monocle` here is
that same round-trip, since Monocle has no `IndexedRecord` carrier.)

### jsoniter — `JsoniterPrism` / `JsoniterTraversal` over `Array[Byte]`

`native` is a hand-rolled `JsonReader` that walks to the focus and `skip()`s
every sibling — the optimum a jsoniter expert writes, which eo automates.

| metric | size | eo | native | naive | monocle | eo vs naive |
|---|--:|--:|--:|--:|--:|--:|
| `street` read   |   8 |   195 |    761 |   2 003 |   1 995 |  10.3× |
| `street` read   |  64 |   194 |  3 750 |  12 208 |  12 124 |    63× |
| `street` read   | 512 |   188 | 28 689 | 100 297 |  98 821 |   533× |
| `street` modify | 512 | 3 580 |     — | 170 846 | 169 437 |    48× |
| `lines[*].price` fold | 512 | 79 970 | 66 655 | 103 433 | 450 222 | 1.3× |

The byte-walk read is **flat at ~190–195 ns**; even the hand-rolled `native` scan
scales (it tokenises everything it passes), so eo beats it ~4× at size 8 and
pulls further ahead with size. The `modify` splice is O(bytes) so it grows mildly
but stays ~48× under a re-encode. The **fold** must touch every element, so eo
lands near `native` and only ~1.3× under `naive`.

**eo-jsoniter vs eo-circe** (the `JsoniterBench` cross-EO comparison, on the
canonical `Order` swept over size 8/64/512, CI): eo-circe parses the whole
document each call (`circeParse` then drill), so it is O(size); the eo-jsoniter
byte-walk is flat. One-shot scalar read (`$.id`): jsoniter ~37–39 ns at *every*
size vs circe 4 422 → 215 684 ns, a gap that grows from **~120× (size 8) to
~5 500× (512)** as the parse cost climbs. `.replace`/`.modify $.id`: jsoniter
100 → 3 171 ns (O(bytes) splice) vs circe ~8 580 → 408 803 ns, **~85–130×**.
The array `lines[*].price` fold narrows to **~4.6×** — both must visit every
element. (A deliberately-absent `$.customer.absent` miss costs eo-jsoniter a flat
~174 ns; there is no honest circe peer, since a typed codec can't drill an absent
field.) The
[design spike](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-eo-jsoniter-spike.md)
covers the carrier choice and splice mechanics.

## PowerSeries traversal with downstream composition

Three composed-traversal chains. `naive` is the hand-written `copy` + `map`
equivalent — the baseline that matters. `monocle` is the *same* composition built
with Monocle (`Lens.andThen(Traversal.fromTraverse).andThen(Lens)`,
`Traversal.andThen(Prism)`, nested traversals) — these compositions are first-class
there too, so it's a fair peer. `Traversal.each` (carrier `MultiFocus[PSVec]`) is
EO's vehicle for all three; a `@Setup` guard asserts the three paths agree.

| Chain (bench) | N | eo (ns) | naive (ns) | monocle (ns) | eo ÷ naive |
|---|--:|--:|--:|--:|--:|
| `Lens → each → Lens` (`PowerSeriesBench`) | 4 | 139 | 26 | 219 | 5.3× |
| | 16 | 306 | 101 | 585 | 3.0× |
| | 64 | 891 | 394 | 2 039 | 2.3× |
| | 256 | 3 256 | 1 611 | 21 652 | 2.0× |
| | 1024 | 14 320 | 5 504 | 59 063 | 2.6× |
| | 4096 | 59 039 | 22 325 | 183 499 | 2.6× |
| 5-hop tree (`PowerSeriesNestedBench`) | 4 | 776 | 134 | 1 286 | 5.8× |
| | 16 | 1 484 | 388 | 2 787 | 3.8× |
| | 64 | 4 084 | 1 421 | 8 727 | 2.9× |
| | 256 | 16 374 | 4 811 | 96 167 | 3.4× |
| | 1024 | 64 687 | 21 508 | 254 200 | 3.0× |
| `each → Prism`, 50/50 hit (`PowerSeriesPrismBench`) | 8 | 137 | 25 | 312 | 5.5× |
| | 32 | 431 | 84 | 989 | 5.1× |
| | 128 | 1 425 | 325 | 3 680 | 4.4× |
| | 512 | 6 135 | 1 289 | 32 619 | 4.8× |
| | 2048 | 28 200 | 5 403 | 100 077 | 5.2× |

Both `eo` and the hand-written `naive` track **O(N)**. The test is per-element
cost (`ns ÷ element`, traversing `N` leaves — `4N` for the nested tree): once the
fixed per-op setup amortises past the smallest `N`, it flattens to a constant.

| Chain | eo `ns ÷ element` (N small → large) | naive `ns ÷ element` | log-log slope (eo / naive) |
|---|---|---|--:|
| `Lens → each → Lens` | 35 → 19 → 14 → 13 → 14 → 14 | 6.5 → 6.3 → 6.2 → 6.3 → 5.4 → 5.4 | 0.91 / 0.96 |
| 5-hop tree | 49 → 23 → 16 → 16 → 16 | 8.4 → 6.1 → 5.5 → 4.7 → 5.3 | 0.83 / 0.91 |
| `each → Prism` | 17 → 13 → 11 → 12 → 14 | 3.1 → 2.6 → 2.5 → 2.5 → 2.6 | 0.98 / 0.97 |

A least-squares fit on `log(time)` vs `log(N)` gives slopes of **0.9–1.0** for
every eo and naive series (a slope of 1 is exact linearity; the dip below 1 is the
small-`N` fixed cost, which makes the curve concave — i.e. it rules out
*super*-linear growth, not linearity). The `eo ÷ naive` overhead settles at
~2–3× on the dense Lens and nested chains and ~5× on the sparse Prism (whose
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

| Op | Subject | n | eo (ns) | monocle (ns) | visitor (ns) | eo ÷ visitor |
|---|---|--:|--:|--:|--:|--:|
| `universe` | `Expr` balanced | 512 | 14 942 | 176 285 | 6 807 | 2.2× |
| | `Expr` balanced | 4096 | 103 813 | 1 718 746 | 55 300 | 1.9× |
| | `Json` balanced | 512 | 20 178 | 214 869 | 14 444 | 1.4× |
| | `Json` balanced | 4096 | 181 762 | 1 934 153 | 151 342 | 1.2× |
| | `Bin` deep spine | 512 | 15 475 | **SO** | 6 833 | 2.3× |
| | `Bin` deep spine | 4096 | 114 131 | **SO** | 56 748 | 2.0× |
| `transform` | `Expr` balanced | 512 | 18 175 | 15 974 | 8 298 | 2.2× |
| | `Expr` balanced | 4096 | 147 385 | 175 679 | 66 432 | 2.2× |
| | `Bin` deep spine | 512 | 12 906 | **SO** | 4 103 | 3.1× |
| | `Bin` deep spine | 4096 | 132 044 | **SO** | 34 856 | 3.8× |

(ns/op from the reproducible CI benchmarks workflow — 3-fork on the shared runner,
so absolute numbers run higher than a quiet desktop; the ratios are the signal and
sub-~1.5× differences aren't meaningful. **SO** = `StackOverflowError`.)

Three results:

- **`universe` rivals the hand-written visitor.** Reading children straight off
  the PSVec carrier (no `List` round-trip, explicit worklist) puts the JSON
  subject ~1.2–1.4× the bare visitor and `Expr` within ~1.9–2.2× — close to
  the recursion you'd write by hand, while staying composable through `.andThen`.
  (Monocle is ~12–17× slower here at n=4096, its lazy-`#:::` append going
  quadratic, and is *not stack-safe*: both `universe` and `transform`
  `StackOverflowError` on the degenerate spine at depth ≳2048. EO clears the
  spine at every size here and at 100k in the stack-safety test, so the deep rows
  compare EO against the visitor only.)
- **`transform` sits within ~2–3.8× of the hand-written visitor and is
  stack-safe.** `Expr` balanced stays near ~2.2× vs the visitor across sizes;
  Monocle is competitive here (~1.1–1.2× EO ns at balanced depth, as both are
  roughly the same post-optimisation shape) but `StackOverflowError`s on the
  degenerate spine. The spine widens to ~3.1–3.8× vs the visitor (the hybrid
  fallback to the heap-stack machine adds overhead the visitor never pays on a pure
  call stack). `transform` went from an `Eval` trampoline (was ~10× slower) to a
  **hybrid**: a direct call-stack recursion (≈ a hand-written rebuild — no
  per-node heap `Frame`) while shallow, handing any subtree past a depth bound to
  the heap-stack machine so a degenerate spine still can't overflow. (`rewrite`
  keeps its own `cats.Eval` trampoline — it stays stack-safe on *both* the descent
  and a long re-fire chain, which a synchronous machine would put back on the call
  stack.) With `childrenVec` / `rebuild` (no `to`/`from` tuple per node) and
  leaves applied in place, allocation is ~80 B/node on `Expr` (visitor is 44)
  and ~56 B/node on the deep spine. Both paths sit within **~2–4×** of the bare
  visitor.

The residual gap to the visitor is the **carrier materialisation**: even on the
fast recursive path EO allocates a `PSVec` of children + an `out` array per
internal node, where a hand visitor fuses extract-and-rebuild into one
`new Node(go(l), go(r))` and allocates neither — plus the heap stack the deep
fallback uses (so it never overflows; the visitor and Monocle both do on the
spine). Closing the last ~2–4× would mean fusing the recursion into the `plate[S]`
macro — but that emits a *function*, not an `Optic`, which would break the
`.andThen` composition `everywhere` relies on, so it's deliberately not done.

## Recursion schemes — the typed path vs droste and hand-written

`SchemesBench` measures the typed recursion schemes (`cata` / `ana` / `hylo` and the
zoo — the `foldLayered` `ArrayDeque` machine, stack-safe to 10⁶ nodes) against
[droste](https://github.com/higherkindness/droste) and hand-written recursion over a
perfect binary `Bin` tree (8 191 nodes). An earlier untyped `PSVec` path was **removed**
once the typed path subsumed it (its erased positional indexing made algebra arity slips
a runtime error — the exact thing the typed path fixes).

Core rows (run 27398242244, 2026-06-12; B/op is the trustworthy metric on the shared
runner):

| Method | B/op | vs droste (B/op) |
|---|--:|--:|
| `handCata` / `handHylo` |       0 | — |
| `handAna`  | 163 816 | — |
| `drosteCata` | 164 824 | 1× |
| `drosteHylo` | 328 641 | 1× |
| `drosteAna`  | 327 632 | 1× |
| `eoCata` | 361 385 | 2.2× |
| `eoHyloF` | 361 385 | 1.1× |
| `eoAna`  | 524 193 | 1.6× |

The residual constant vs droste is the stack-safety machinery (per-node child array +
frames past depth 512) — droste's basic schemes are stack-*unsafe* naive recursion, and
the hand baselines are the irreducible floor. The zoo, grafting, fusion, and M-path
numbers follow.

### The zoo — para / apo / histo / futu, grafting, fusion, and the M path

The same `SchemesBench` workload (depth-12 perfect binary tree, 8 191 nodes) through the
decorated schemes — eo's typed zoo (`para` / `apo` / `histo` / `futu`) against
`droste.scheme.zoo` — plus the routes that pin the driver's design decisions: the generic
decoration route, the monadic machine at `cats.Id`, and fused-vs-materialised `cross`.
As above, B/op is the trustworthy column; ns/op is directional.

| Method | ns/op | B/op | B/op vs droste |
|---|--:|--:|--:|
| `eoPara`      | 183 747 |   557 945 | 0.50× |
| `drostePara`  | 283 752 | 1 114 890 | 1× |
| `eoApo`       | 195 677 |   655 249 | 0.57× |
| `drosteApo`   | 293 721 | 1 146 674 | 1× |
| `eoApoGraft`     |  35 |   224 | 0.88× |
| `drosteApoGraft` |  46 |   256 | 1× |
| `eoHisto`     | 191 617 |   557 969 | 1.24× |
| `drosteHisto` | 103 420 |   448 705 | 1× |
| `eoFutu`      | 188 749 |   655 249 | 1.43× |
| `drosteFutu`  |  93 458 |   458 689 | 1× |
| `eoCata`             | 172 428 | 361 385 | 2.19× |
| `eoCataGenericRoute`  | 162 279 | 362 313 | 2.20× |
| `drosteCata`          |  56 542 | 164 824 | 1× |
| `eoHyloF` | 180 767 | 361 385 | — |
| `eoHyloM` | 343 202 | 929 472 | — |
| `eoCrossFused`        | 239 393 | 820 066 | — |
| `eoCrossMaterialized` | 375 824 | 885 579 | — |

Six results:

- **`para` / `apo` halve droste's allocation.** eo decorates on the same array machine as
  `cata`/`ana`, pairing subterms off the already-walked nodes; droste's zoo re-embeds each
  subterm (para) and re-allocates the `Either` spine (apo), landing at ~2× eo's B/op
  (1 114 890 vs 557 945; 1 146 674 vs 655 249). The ns column agrees directionally
  (~1.5× in eo's favour on both).
- **Grafting is O(1) on both — parity, with a guarantee.** The graft bench embeds a prebuilt
  8 191-node subtree in one `apo` step: both land flat at a couple hundred B/op (224 vs 256),
  because droste's `zoo.apo` `R` *is* the fixed point, so its `Left(fix)` also embeds by
  reference. eo's differentiator here is not speed but the **law-shaped `eq` guarantee** that
  the grafted subtree is embedded untouched; the O(graft) re-walk contrast applies to generic
  `distApo`-style decoration routes, not to droste's native `zoo.apo`.
- **The generic decoration route costs nothing.** A user-written identity gather — which skips
  the driver's identity fast path — lands at 362 313 B/op vs the fast path's 361 385: escape
  analysis elides the per-node decoration wrapper, so writing your own `Decor` route is
  alloc-free over `cata`.
- **`histo` / `futu` trail droste by ~1.2–1.4× B/op — the price of stack-safety.** The remaining
  gap is the stack-safe machine's per-node child array; droste's zoo recursion is naive
  call-stack recursion (stack-*unsafe*), so it pays no machine bookkeeping — and overflows on
  the deep inputs eo's machine clears.
- **`eoHyloM` is the tailRecM per-event floor.** The monadic machine at `cats.Id` costs
  929 472 B/op vs 361 385 for `hylo` (~2.6×) — that delta is the `tailRecM` step-event
  wrapping, the price of arbitrary-monad algebras. This run includes the M-machine
  optimisation: the previous run (27384569800) had `eoHyloM` at 1 606 586 B/op, a **−42%**
  improvement.
- **Fused `cross` beats materialising on both axes.** Composing `ana` into `cata` via
  `cross` fuses into one pass — 239 393 ns / 820 066 B/op vs 375 824 ns / 885 579 B/op for
  build-the-tree-then-fold (~1.6× faster, no intermediate tree). The fused path also dropped
  **−22%** from the previous run's 1 049 417 B/op with the same optimisation commit.

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
