# Plan 011 — Shaking weight off `PSVec` (the `MultiFocus[PSVec]` hot path)

- **Status:** P1 + P2 implemented and verified (2026-06-07); P3 spiked and **reverted** (no measurable win — see Results). Awaiting a 3-fork CI bench on a quiet box to confirm ns/op.
- **Date:** 2026-06-07
- **Owner:** —
- **Scope:** `core/` data layer (`PSVec`, `ObjArrBuilder`, `MultiFocus.mfAssocPSVec`,
  `Traversal.pEach`, `Plated`). No public API changes intended.
- **Prime directive (per project norm):** every change is gated on (a) `sbt test`
  staying green — laws *and* behaviour — and (b) a measured before/after on `sbt bench`
  (3-fork). No perf claim in this doc is to be trusted on assertion; the numbers below
  are single-fork, directional data gathered to *locate* weight, not to prove a win. Each
  item re-measures before any win is claimed.

---

## TL;DR

`PSVec` itself is already lean. The weight on the `MultiFocus[PSVec]` hot path lives in
**three concrete, independently-shippable places**, ranked by confidence×payoff:

| # | Target | Evidence | Expected win | Risk |
|---|--------|----------|--------------|------|
| **P1** | `mfAssocPSVec` generic fallback uses a **default-capacity-16 `ObjArrBuilder`** that doubles up to N, and re-copies a flat vector that already exists | `ObjArrBuilder.grow` = **9 %** of all PowerSeries allocation, attributed via stack to `composeTo → composeTo(generic) → appendAllFromPSVec → grow` | Remove the grow garbage + an O(N) arraycopy on every `lens.andThen(each)…` chain | **Low** — local to one method, presize + an `n==1` zero-copy short-circuit |
| **P2** | `composeFrom` / `Plated.transform` call `ObjArrBuilder.append` (bounds-checked) on a **pre-sized** buffer N times | CPU: `ObjArrBuilder.append` = **8 %** self-time on PowerSeries; the buffer is provably pre-sized | Drop N redundant capacity checks per op via `unsafeAppend` | **Low** — pre-size invariant already holds |
| **P3** | `PSVec` is a **3-implementor** sealed trait → `apply`/`length`/`offset` are megamorphic and **not inlined** | CPU: `PSVec$Slice.apply`/`.offset`/`.length` show up as *distinct* self-time frames (trivial getters that should vanish into the caller) | Merge `Empty` into a 0-length `Slice` → **2 implementors → bimorphic → inlinable**; also removes the per-node `Slice` wrapper question | **Medium** — touches the shared data type; fully covered by law suite, but measure both ways |

Everything else profiled is **inherent work** (immutable `case class` copies, `String`
upper-casing, `List` cons cells in `universe`) — not `PSVec` overhead and out of scope.

---

## Results (2026-06-07, implemented)

**Shipped: P1 + P2** (both in `MultiFocus.scala`). Full suite green
(`core`/`tests`/`generics`/`laws`: 166 + 42 + 33 examples, 0 failures), `scalafmtCheckAll`
clean. Allocation measured `gc.alloc.rate.norm`, baseline-vs-final, identical config
(`-i 4 -wi 3 -f 2`, JDK 25, single box). **B/op is the trusted signal — ns/op at `f 2` on
this shared box swings ±40 % even on byte-identical paths, so no timing claim is made here;
that's what the deferred 3-fork CI run is for.**

| Benchmark | size | B/op before | B/op after | Δ |
|-----------|-----:|------------:|-----------:|---:|
| `PowerSeriesBench.eoModify_powerEach` | 4 | 752 | 632 | **−16.0 %** |
| | 32 | 2 040 | 1 812 | **−11.2 %** |
| | 256 | 12 000 | 10 808 | **−9.9 %** |
| | 1024 | 45 857 | 41 602 | **−9.3 %** |
| `MultiFocusBench.eoModify_powerEach` | 1024 | 78 738 | 74 426 | **−5.5 %** |
| `PowerSeriesNestedBench.eoModify_nested` | 4 | 2 412 | 2 444 | +1.3 % (noise) |
| `TraversalBench.eoModify` (control, untouched path) | 512 | 24 704 | 24 704 | 0.0 % |
| `MultiFocusBench.eoModify_multiFocus` (control, `List` carrier) | 1024 | 286 070 | 286 000 | 0.0 % |

Deterministic confirmation via async-profiler `event=alloc`: on
`PowerSeriesBench.eoModify_powerEach` N=1024 the `ObjArrBuilder.grow` allocation weight went
**2 889 → 0**, and total `Object[]` allocation **14 236 → 12 129**. The two control
benchmarks moving 0.0 % proves the change is targeted, not global noise.

A small-N regression surfaced mid-implementation: pre-sizing the `n > 1` fallback's `flatBuf`
to `n` made a small-`n`/high-fanout chain (`each.andThen(each)`, N=4) **+9 %**, because
`n < 16 < total` produced *more* doublings than the old default-16. Fixed by flooring the
capacity at `max(n, 16)` → back to +1.3 % (noise).

**P3 reverted — but the real reason is subtler than "no win in the profile" (a follow-up
question forced a proper investigation).** Two findings, both deterministic:

1. **The megamorphic premise is false on the benchmarks.** `-XX:+PrintInlining` on the
   shipped code shows every hot `PSVec` accessor site devirtualizes to a single concrete
   type and inlines: PowerSeries `composeTo`/`composeFrom` `apply` is `TypeProfile
   (41690/41690) = PSVec$Slice` — **100 % monomorphic, inline (hot)**; Plated `rec$1`
   `length`/`apply` likewise resolve `N/N = Slice`. No hot site sees 3 receiver types. Each
   *individual* benchmark's `childrenVec` site sees at most **two** types (`eoTransformExpr`:
   `Empty` leaves + `Slice` App-nodes; `eoTransformDeep`: `Empty` + `Single` spine), which is
   **bimorphic — already inlined by C2 under a type guard.** So P3 is a bimorphic→monomorphic
   change (removes a guard branch), *not* the megamorphic→bimorphic cliff-crossing it was
   designed to be. Expected upside: marginal. (My earlier read that `Slice.*` frames *appearing*
   in the async profile meant "not inlined" was wrong — async-profiler shows inlined frames by
   default. And the "21→45 frames went up" I cited as evidence-against was itself noise.)

2. **The hardware can't measure it.** A high-power A/B (5 forks × 6 warmup × 10 iters) on the
   three Plated benches gave JMH `scoreError` of **±15–50 %** and Δ% that flips sign across
   sizes of the *same* benchmark (`eoTransformExpr` −19.9 % at n=512 but +17.5 % at n=4096).
   8 of 9 rows had overlapping confidence intervals. This box (shared, 32-core, under variable
   load, JDK 25) cannot resolve a sub-10 % timing effect — so allocation (B/op) is the only
   trustworthy signal here, and P3 has **zero** allocation effect by construction.

Net: P3's mechanism doesn't even fire on the current benchmarks, its best-case effect is a
guard-branch removal, and it's unmeasurable on this hardware — while costing a shared-core-type
change and a relaxed UB assertion. Reverted. **Where P3 *would* matter:** an ADT whose
children-traversal site sees `Empty` + `Single` + `Slice` *simultaneously* (leaves + unary +
multi-child nodes through one `childrenVec`) is genuinely megamorphic; none of the current
benchmarks build that shape. Revisiting P3 should start by *adding such a benchmark* and
measuring on a quiet box / CI — otherwise there's nothing to optimize against.

---

## Why `PSVec` is worth the attention

`PSVec` is the focus vector of the `MultiFocus[PSVec]` carrier, which backs:

- `Traversal.each` / `pEach` (`core/.../optics/Traversal.scala:34`) — every multi-focus traversal,
- `Plated` (`core/.../optics/Plated.scala`) — `transform` / `rewrite` / `universe` / `everywhere`,
- the same-carrier `.andThen` associator `mfAssocPSVec` (`core/.../data/MultiFocus.scala:327`) —
  the join taken by **every longer optic chain** that stays in the PSVec carrier.

So a byte or a non-inlined call shaved here is multiplied across traversals, recursion
schemes, and deep `andThen` chains — exactly the surfaces the request flagged.

---

## Methodology

- **Box:** this machine, OpenJDK **25.0.3**, 32 cores, `perf_event_paranoid=2`.
- **Harness:** JMH 1.37 via `sbt-jmh`, run **directly through `java -cp <Jmh/fullClasspath>
  org.openjdk.jmh.Main`** rather than `sbt "Jmh/run …"`. *Why:* the `-prof async:…` option
  is `;`-delimited and **sbt splits the command on `;`**, silently dropping the benchmark
  filter and the profiler sub-options (observed: it ran the whole suite with a bare
  `libPath`). The `java` path gives full argument control.
- **Allocation attribution:** async-profiler `event=alloc;output=collapsed` (works under
  `paranoid=2` — it hooks JVM TLAB callbacks, not `perf_events`).
- **CPU attribution:** async-profiler `event=itimer` (also `perf`-free). Sample counts are
  low (~500/bench, single fork) so CPU figures are **directional**; the acceptance gate is
  a 3-fork `sbt bench`, not these.
- **Caveats:** single fork, JMH compiler-blackhole mode, JDK 25 (project targets 17/21 —
  re-confirm wins on a target JDK before landing). Reproduction commands in the Appendix.

---

## Baseline measurements (`-prof gc`, single fork, `gc.alloc.rate.norm`)

| Benchmark | size | ns/op | B/op | B per element |
|-----------|-----:|------:|-----:|--------------:|
| `PowerSeriesBench.eoModify_powerEach` (ArraySeq) | 4 | 400 | 760 | — |
| | 32 | 1 426 | 2 040 | ~44 |
| | 256 | 2 440 | 11 958 | ~44 |
| | 1024 | 10 647 | 45 878 | **~44** |
| `TraversalBench.eoModify` (`each` over `List[Int]`) | 512 | 11 835 | 24 704 | ~48 |
| `MultiFocusBench.eoModify_powerEach` (PSVec, List fixture) | 1024 | 15 830 | 78 738 | ~77 |
| `MultiFocusBench.eoModify_multiFocus` (`MultiFocus[List]`) | 1024 | 104 587 | 285 972 | ~280 |
| `MultiFocusBench.naive_listMap` (hand-written) | 1024 | 7 878 | 49 177 | ~48 |
| `PowerSeriesNestedBench.eoModify_nested` (5-hop) | 256 | 12 021 | 54 249 | — |
| `PowerSeriesPrismBench.eoModify_sparse` (50 % miss) | 512 | 5 182 | 24 896 | — |
| `PlatedBench.eoTransformExpr` | 4096 | 79 369 | 655 322 | ~160/node |
| `PlatedBench.eoUniverseExpr` | 4096 | 272 126 | 786 510 | ~192/node |

Reading it: the PSVec path (`eoModify_powerEach`, ~44–77 B/elem) is already **3.6× lighter
and 6.6× faster** than the generic `MultiFocus[List]` path (~280 B/elem) and within
~1.6× of a hand-written `naive` loop on bytes. So we are optimizing an already-good path —
the wins are surgical, not structural rewrites.

---

## Allocation attribution

### `PowerSeriesBench.eoModify_powerEach`, N=1024 (`lensPhones.andThen(each).andThen(lensActive)`)

Total alloc samples 31 698. Leaf object → share:

| Allocated object | share | nature |
|------------------|------:|--------|
| `PowerSeriesBench$Phone` | **53.9 %** | **inherent** — immutable `.copy` per toggled element |
| `java.lang.Object[]` | **44.9 %** | **PSVec machinery** — the accumulator/result arrays |
| `scala.Tuple2` | 0.37 % | the per-element `singletonTo` pair — **escape-analysed away** (not the suspect it looks like) |
| `PSVec$Slice` | 0.27 % | view wrappers |
| `ObjArrBuilder` / `AssocSndZ` / `IntArrBuilder` / `PSVec$Single` | <0.2 % ea. | negligible |

The `Object[]` 44.9 % decomposes (frame below the leaf) to:

| Allocating method | weight | meaning |
|-------------------|-------:|---------|
| `ObjArrBuilder.<init>` | 8 504 | the necessary accumulators (`ysBuf`, `flatBuf`, `resultBuf`) |
| **`ObjArrBuilder.grow`** | **2 889** | **avoidable** — an under-sized builder doubling to N |
| `pSVecFunctor.map` | 2 843 | the `.modify`'s `to → map → from` middle array |

The `grow` stack is unambiguous:

```
mfAssocPSVec.composeTo            (outer = lensPhones∘each)
  └─ mfAssocPSVec.composeTo       (inner = lensPhones∘each, GENERIC FALLBACK branch)
       └─ ObjArrBuilder.appendAllFromPSVec
            └─ ObjArrBuilder.grow → new Object[]
```

**Mechanism.** `.andThen` left-associates, so `a.andThen(b).andThen(c)` is
`(a.andThen(b)).andThen(c)`. For the *outer* operand `lensPhones.andThen(each)`, the inner
is **`each`**, which carries **no fast-path marker** (`MultiFocusSingleton` is only mixed
into the Lens bridge, `MultiFocusPSMaybeHit` only into Prism/Optional bridges —
`MultiFocus.scala:48,120,616`). So that composition takes the **generic fallback**
(`MultiFocus.scala:361-374`), and that branch — unlike the two fast paths — builds its
flat buffer with **`new ObjArrBuilder()` (default capacity 16)** (`:365`) and fills it via
`appendAllFromPSVec`. With one outer focus (the single `ArraySeq` field) producing N
children, the 16-slot buffer doubles ~6× up to N and the N children are arraycopied into
it — even though `vy` (the inner result) **already is** that exact flat vector.

This is not a rare escape hatch; `lens.andThen(each)` is the single most common traversal
shape in the library.

### `PlatedBench.eoTransformExpr`, n=4096

Total 56 223. `Object[]` 29.7 %, **`PSVec$Slice` 15.1 %**, then `Expr$App` 15 % +
`String` 15 % + `byte[]` 15 % + `Expr$Var` 10 % (all inherent tree/string rebuild).
The `Slice` 15 % is a **per-internal-node wrapper**: `transform` does
`PSVec.unsafeWrap(out)` per node (`Plated.scala:120`) and the derived `childrenVec`
wraps a fresh array per node — each a `new Slice`.

### `PlatedBench.eoUniverseExpr`, n=4096

Total 30 833. **`::` cons cells 75 %** (inherent — `universe` returns a `List` and uses a
`List` worklist), `Object[]` 12.5 %, `PSVec$Slice` 12.3 %. The PSVec cost here is again the
per-node `Slice`; the dominant cost is the `List` it is asked to produce — out of scope.

---

## CPU attribution (`itimer`, directional)

`PowerSeriesBench.eoModify_powerEach` N=1024 (509 samples), EO self-time leaders:

| frame | samples | note |
|-------|--------:|------|
| `mfAssocPSVec.composeFrom` | 117 | the reassembly loop |
| `Phone.copy` | 51 | inherent |
| **`ObjArrBuilder.append`** | **42** | bounds-checked append on a **pre-sized** buffer → **P2** |
| `ObjArrBuilder.<init>` | 21 | |
| `tuple2multifocusPSVec…singletonFrom` | 21 | the Lens bridge |
| `PSVec$Slice.apply` | 19 | indexed read — **not inlined** |
| `mfAssocPSVec.composeTo` | 17 | |
| `pSVecFunctor.map` | 14 | |
| `ObjArrBuilder.grow` | 7 | the P1 garbage, also CPU |

`PlatedBench.eoTransformExpr` n=4096 (523 samples): `Plated.rec$1` 184, `toUpperCase` 121
(inherent), `Expr$App.<init>` 26, `childrenVec` 19, `PSVec.unsafeWrap` 19,
**`PSVec$Slice.apply` 15, `PSVec$Slice.offset` 4, `PSVec$Slice.length` 2**.

> **Monomorphization signal.** Trivial accessors `Slice.offset` and `Slice.length` appear
> as their *own* CPU frames. A getter that returns a field should be inlined into the
> caller and never show up. Its presence is consistent with `PSVec`'s **3 concrete
> implementors** (`Empty`, `Single`, `Slice`) making `apply`/`length`/`offset` calls
> **megamorphic** (>2-way) at the shared `PSVec` static type, which defeats the JVM's
> bimorphic inline cache. This is the hypothesis P3 tests.

---

## The plan

### P1 — Kill the generic-fallback grow + redundant flat copy *(do first; highest confidence)*

**File:** `core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala`, `mfAssocPSVec.composeTo`
generic fallback (`:361-374`).

Two sub-changes:

1. **Pre-size `flatBuf`.** Replace `new ObjArrBuilder()` (`:365`) with a sized builder.
   The honest floor is `va.length` (`n`); when each inner produces ~1 focus this removes
   the doublings entirely.
2. **`n == 1` zero-copy short-circuit.** When the outer produces a single focus (the
   `lens.andThen(each)` shape — a field that *is* a collection, then traverse it), the
   loop runs once: `inner.to(va(0))` returns `(xi, vy)` and **`vy` is already the complete
   flat focus vector**. Emit `(AssocSndZ(xo, Array(vy.length), Array(xi)), vy)` directly —
   no `flatBuf`, no `grow`, no O(N) `appendAllFromPSVec` arraycopy. `composeFrom`'s generic
   branch already reconstructs from `vys.slice(offset, len)` and a single-element `lens`
   array, so this is symmetric and needs no `composeFrom` change.

**Evidence:** `grow` = 2 889 alloc samples (~9 % of total / ~20 % of `Object[]`) + 7 CPU
samples, plus an un-billed O(N) arraycopy in `appendAllFromPSVec`.
**Expected:** PowerSeries `eoModify_powerEach` B/op down ~9 %+ at N=1024; larger relative
win at small N (the 16-floor dominates there); a full N-copy of CPU removed per op.
**Test-green:** behaviour is identical (same elements, same order, same `AssocSndZ`
shape). Covered by `tests/` law suites for traversal composition + the
`MultiFocus`/`PowerSeries` behaviour specs.
**Risk:** Low. One method, no signature/type change.

### P2 — `unsafeAppend` on provably pre-sized buffers

**Files:** `mfAssocPSVec.composeFrom` always-hit + maybe-hit branches (`:391,:402`) and
`Plated.transform`/`transformMachine` where they fill `out`.

`resultBuf` is constructed `new ObjArrBuilder(ys.length)` and appended **exactly**
`ys.length` times — it can never grow, yet `append` runs the `if len == arr.length`
check on every call (`ObjArrBuilder.append`, 42 CPU samples). Switch those call sites to
`unsafeAppend` (already exists, `ObjArrBuilder.scala:22`). `Plated.transform` already
writes into a raw `new Array[AnyRef](n)` by index, so audit it only for parity.

**Expected:** removes N bounds-checks per op; small steady CPU win, ~0 allocation change.
**Test-green:** the pre-size invariant is local and obvious; assert it with a comment and,
if cheap, a debug `assert`. Law + behaviour suites unchanged.
**Risk:** Low, *provided* the pre-size invariant is documented at each `unsafeAppend` site
(an under-sized `unsafeAppend` is an OOB write — keep them adjacent to their sizing).

### P3 — Merge `Empty` into a zero-length `Slice`: 3 implementors → 2 (bimorphic, inlinable)

**File:** `core/src/main/scala/dev/constructive/eo/data/PSVec.scala`.

Represent the empty vector as a **shared 0-length `Slice` over a shared empty array**
(`val Empty = new Slice(Array.empty, 0, 0)`), dropping the `Empty` case object. `Single`
stays (its inline-element win is real and `Single` is negligible in the profiles, so
there's no reason to disturb it). That leaves **two** concrete implementors of
`apply`/`length`/`head`/`slice` → the JVM's bimorphic inline cache can devirtualize and
inline them, which should make `Slice.offset`/`.length`/`.apply` disappear from the CPU
profile and let the hot loops in `composeTo/From` and `Plated.rec` inline indexed reads.

**Evidence (hypothesis to confirm):** `Slice.offset`/`.length`/`.apply` present as distinct
CPU frames; megamorphic dispatch is the textbook cause.
**Validation before committing:** this one is *measured speculation* — gate it hard.
1. Capture `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` on a focused run before/after
   and confirm the accessors move from "not inlined / megamorphic" to inlined.
2. 3-fork `sbt bench` on `PlatedBench.*`, `PowerSeries*`, `TraversalBench.*`, `MultiFocusBench.*`.
3. If no measurable win, **revert** — the 3-class form is otherwise fine and the doc-comment
   savings narrative (`Empty` allocates nothing) is nice to keep.

**Test-green:** `Empty` semantics must stay identical — `length==0`, `apply` throws
`IndexOutOfBoundsException`, `head` throws `NoSuchElementException`, `slice` returns empty,
`toAnyRefArray` returns a (shared, must-stay-immutable) empty array, value-equality across
variants. The law suite (`tests/`) plus PSVec's own equality/behaviour checks cover this;
add explicit unit assertions for the four `Empty` behaviours if not already present.
**Risk:** Medium. It changes the shared type's shape. Two subtleties: (a) `Empty` is
`PSVec[Nothing]` — the shared `Slice` instance must be cast-safe at every `empty[B]` use
(it is: erased `Array[AnyRef]`, length 0, nothing read); (b) keep the shared empty array
truly never-mutated (it already is — all writers allocate their own).

---

## What this plan explicitly does **not** do

- **No `opaque type` / `AnyVal` rewrite of `PSVec`.** It's a 3-shape sum; a value class
  can't model `Empty`/`Single`/`Slice` without re-introducing allocation or boxing. P3's
  3→2 merge captures the inlining win at a fraction of the risk.
- **No new fast-path marker for `each`/`selfChildren`** (the "structural P2" some would
  reach for). P1's `n==1` short-circuit already captures the dominant `lens.andThen(each)`
  case. Only add a `MultiFocusPSAlwaysVec`-style marker if, *after P1*, the profile still
  shows generic-fallback weight on `each.andThen(each)` (nested traversal) — re-measure
  before adding trait surface.
- **No touching the inherent costs:** `Phone.copy`, `String.toUpperCase`, `universe`'s
  `List` cons. Those are the *work*, not the *overhead*. (A separate idea — a
  `universeVec`/iterator that doesn't build a `List` — is a feature, not weight-shaving,
  and belongs in its own plan.)
- **No `Functor[PSVec].map` fusion into `modify`.** The middle array (`pSVecFunctor.map`,
  2 843 samples) is genuinely needed by the carrier-generic `to → map → from` modify
  contract; fusing it requires a PSVec-specialised `modify` override and risks the generic
  law. Park it; revisit only if P1+P3 land and it's still top-3.

---

## Verification protocol (applies to every item)

1. **Correctness gate (blocking):** `sbt test` green — `core`, `laws`-driven `tests`, and
   `generics`. The traversal/Plated/MultiFocus law suites are the real proof that a
   weight-shave didn't change behaviour.
2. **Format/lint gate:** `sbt scalafmtCheckAll` (pre-commit mirrors this).
3. **Perf gate (blocking for the claim, not the merge):** 3-fork
   `sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 -prof gc .*PowerSeries.* .*Plated.* .*Traversal.* .*MultiFocus.*"`
   before and after, comparing `gc.alloc.rate.norm` (B/op) and ns/op. A change that
   doesn't move B/op or ns/op outside error gets reverted, not rationalized.
4. **Attribution re-check:** re-run the `event=alloc` collapsed profile on
   `PowerSeriesBench.eoModify_powerEach` (N=1024) and confirm the targeted leaf
   (`ObjArrBuilder.grow` for P1; `Slice.*` frames for P3) actually shrank.
5. **Stack-safety regression (P3 especially):** keep `PlatedBench.eoUniverseDeep` /
   `eoTransformDeep` (the degenerate-spine benches) passing — `PSVec` shape changes must
   not perturb the heap-stack machine.

Suggested landing order: **P1 → P2 → (measure) → P3 as a gated spike.** P1 and P2 are
near-zero-risk and should land together; P3 is a measured experiment with a clean revert.

---

## Appendix — reproduce the profiling

```sh
# 1. Export the JMH classpath (sbt's command parser mangles `;` in -prof, so run java directly):
sbt -batch 'export benchmarks/Jmh/fullClasspath' | grep -E '^/' | tail -1 > /tmp/cp.txt

# 2. Stage async-profiler (already present under /tmp/…pyroscope…/ in this env, or download
#    libasyncProfiler.so from https://github.com/async-profiler/async-profiler):
#    cp <libasyncProfiler.so> /tmp/libasyncProfiler.so

# 3. Allocation attribution (works under perf_event_paranoid>=2):
CP=$(cat /tmp/cp.txt)
java -cp "$CP" org.openjdk.jmh.Main -i 4 -wi 2 -f 1 -p size=1024 \
  -prof "async:libPath=/tmp/libasyncProfiler.so;event=alloc;output=collapsed;dir=/tmp/ap" \
  PowerSeriesBench.eoModify_powerEach

# 4. CPU attribution (itimer is perf-free):
java -cp "$CP" org.openjdk.jmh.Main -i 5 -wi 3 -f 1 -p n=4096 \
  -prof "async:libPath=/tmp/libasyncProfiler.so;event=itimer;output=collapsed;dir=/tmp/cpu" \
  PlatedBench.eoTransformExpr

# 5. Aggregate a collapsed file by allocating leaf:
awk '{w=$NF; sub(/ [0-9]+$/,""); m=split($0,s,";"); print w"\t"s[m]}' <file>.csv \
  | awk -F'\t' '{a[$2]+=$1} END{for(k in a) print a[k]"\t"k}' | sort -rn | head

# 6. GC norm baseline table:
java -cp "$CP" org.openjdk.jmh.Main -i 3 -wi 2 -f 1 -prof gc -rf json -rff /tmp/gc.json \
  TraversalBench MultiFocusBench PowerSeries.* PlatedBench
```
