# eo-jsoniter spike — design sketch

**Date:** 2026-04-29
**Status:** **Phase 1 + 1.5 SHIPPED 2026-04-29** (commits `481b7cb`, `3b75460`, this commit). Read-only optics on the **`Affine` carrier (Prism) and `MultiFocus[List]` carrier (Traversal) — no new carriers**. Phase-2 read-write splice greenlit by JMH (16.2× / 7.9× / 11.9× scalar speedups; 2.3× on 10-element array fold).
**Context:** Companion module to [`eo-circe`](../../circe/) for jsoniter-scala. Goal of this spike: figure out whether `eo-jsoniter` would *complement* eo-circe (different niche, both ship), *replace* it (jsoniter's perf wins, eo-circe goes away), or *be redundant* (jsoniter users don't need an AST optic). Outcome: **complement**. The original draft of this doc proposed a new `ByteSpanF` carrier; the actual ship reuses the existing `Affine` carrier — see §2 for the revision.

## 1. Why this is not "circe but faster"

`circe` and `jsoniter-scala` solve overlapping problems with opposite architectural priorities:

| | circe | jsoniter-scala |
|---|---|---|
| Runtime model | Parses to a `Json` AST first, then decodes | Streams bytes directly into compile-time-derived codecs |
| Mid-air mutation | Natural — operate on the `Json` tree | None — there's no AST to mutate |
| Dynamic shapes | First-class — drill into anything via cursors | Awkward — needs a codec for the surrounding shape |
| Throughput | Baseline | ~5–10× circe (varies by shape and direction) |
| Allocation | High (AST per parse) | Low (no AST) |

`eo-circe` exposes optics *over the AST*: `JsonPrism[A] <: Optic[Json, Json, A, A, Either]` walks the `io.circe.Json` tree, encodes/decodes only at the leaves, accumulates `JsonFailure` along the way. The carrier is `Either`; the existential carries the `(DecodingFailure, HCursor)` of the partial-decode site so partial success surfaces as `Ior.Both(failures, json)`.

Mirroring that shape on top of jsoniter would mean fabricating a runtime AST that jsoniter explicitly avoids — forfeits the perf win, gains nothing over eo-circe. **Not interesting.**

The interesting eo-jsoniter would instead be optics *over the byte stream*, so the focus type is "a slice inside a JSON byte buffer that decodes to A". No allocations until the user actually reads the focus. This is structurally different from anything currently in cats-eo.

## 2. Carrier shape — what shipped

### 2.1 Reuse `Affine`, no new carrier

```scala
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic

// JsoniterPrism is just an Optic[Array[Byte], Array[Byte], A, A, Affine]
// with X = (Array[Byte], (Array[Byte], Int, Int)).
//   Fst[X] = Array[Byte]                     — Miss carries the original bytes
//   Snd[X] = (Array[Byte], Int, Int)         — Hit carries (bytes, start, end)
val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
  JsoniterPrism[Long]("$.payload.user.id")
```

Affine.Hit's `snd` is the focused-span context (bytes + start + end), Hit's `b` is the decoded `A`. Affine.Miss's `fst` is the original bytes for pass-through. Standard cats-eo extensions (`.foldMap`, `.modify`, `.andThen`) light up automatically via Affine's existing `ForgetfulFold[Affine]` / `ForgetfulFunctor[Affine]` / `AssociativeFunctor[Affine, _, _]` instances — **zero new typeclass instances shipped**. The composition matrix gains zero cells (no new row, no new column). This is the load-bearing ergonomic win the spike validated.

### 2.2 Why the original "new carrier" reasoning was wrong

The first draft of this doc proposed a fresh `ByteSpanF` carrier. Two arguments backed it:

1. *"The focused value is parameterised by `A`."* True, but irrelevant. `Affine[X, B]` already has `B` as its focus parameter. The codec dispatch via `JsonValueCodec[A]` is summoned at the optic factory call site, not at the carrier level.
2. *"Write-back is non-trivial."* True, but solvable inside Affine. The splice operation `(newBytes, span) => Array[Byte]` lives inside the optic's `from` closure — it doesn't need to be lifted into the carrier's typeclass surface. The Hit branch's `snd: (Array[Byte], Int, Int)` carries exactly the splice context phase 2 needs.

The real test: does the spike compile and pass on Affine without any new typeclass instance? Yes — see commit `481b7cb`. The same Affine carrier that powers `Optional` powers JsoniterPrism. Ergonomics-equivalent, zero matrix bloat.

### 2.3 Read-only vs read-write split — same carrier, different `from`

Phase 1 ships `from = identity` (Hit returns `h.snd._1`, Miss returns `m.fst`). Phase 2 plugs in splice mechanics inside the same `from` closure: encode the focus to bytes via `JsonValueCodec[A].encodeToArray`, then memcpy `bytes[0, start) ++ newBytes ++ bytes[end, length)`. **No carrier change** — the X shape was deliberately picked to carry enough context for both phases.

This is what makes the Affine choice worth it: the read-write transition is ~30 lines of splice mechanics inside `from`, not a new carrier + new typeclass instances + new matrix paperwork.

## 3. Surface — what shipped vs what's queued

### 3.1 SHIPPED — `JsoniterPrism[A]("$.path")` decode at a path

```scala
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

given JsonValueCodec[Long] = JsonCodecMaker.make

val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
  JsoniterPrism[Long]("$.payload.user.id")

val bytes: Array[Byte] = ...
idP.foldMap(identity[Long])(bytes)  // Long — zero AST allocation
```

Path syntax (subset — see §5.1 for the rationale): `$`, `$.field.field`, `$[i]`, dotted chains. Wildcards / filters / recursive descent rejected at parse time.

### 3.2 SHIPPED — read-only escapes via Affine extensions

These ALL light up automatically; no extra code in `eo-jsoniter`. The Affine carrier ships them via `ForgetfulFold[Affine]` / `ForgetfulFunctor[Affine]`:

```scala
import cats.instances.string.given

val emailP = JsoniterPrism[String]("$.payload.user.email")

emailP.foldMap(identity[String])(bytes)   // String, decoded — Monoid.empty on miss
emailP.headOption(bytes)                  // Option[String]
emailP.exists(_.contains("@"))(bytes)     // Boolean
```

Confirmed in `JsoniterPrismSpec` (6 specs / 21 expectations).

### 3.3 SHIPPED (phase 1.5) — `JsoniterTraversal[A]` over `[*]` paths

```scala
import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.jsoniter.JsoniterTraversal

val itemsT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[PSVec]] =
  JsoniterTraversal[Long]("$.cart.items[*]")

itemsT.foldMap(identity[Long])(bytes)   // sum, no AST
itemsT.length(bytes)                    // count via .foldMap _ => 1
itemsT.exists(_ > 0)(bytes)             // short-circuit on first hit
```

**Carrier: `MultiFocus[PSVec]` — same carrier as `Traversal.each`.** Initially shipped on `MultiFocus[List]`, then swapped to PSVec for two reasons:

1. **Same-carrier `.andThen` composition.** PSVec ships `mfAssocPSVec` (`AssociativeFunctor[MultiFocus[PSVec], _, _]`) — chains like `JsoniterTraversal.andThen(Traversal.each[List, A])` resolve through the standard `Optic.andThen` route with zero-copy per-element reassembly. List would have forced a Composer hop.
2. **Single allocation per traversal.** PSVec wraps an `Array[AnyRef]` zero-copy via `PSVec.unsafeWrap`. `JsoniterTraversal.to` allocates one array of length `spans.length`, decodes each span into it, then wraps. List would allocate one cons cell per element.

The `[*]` step expands the current array via `JsonPathScanner.findAll`; spans are decoded eagerly into the array (jsoniter's `readFromSubArray`), but the cost is paid only when `.to` runs. Spans whose decode throws are silently dropped — `foldMap` reads the focuses that exist, ignores the rest. `JsoniterPrism` rejects wildcard paths at construction, redirects the user to this Traversal factory.

Existential `X = (Array[Byte], List[JsonPathScanner.Span])` carries the bytes + per-element spans for the future phase-2 splice path. Phase-1.5 `from` is identity — returns `bytes` unchanged.

### 3.4 QUEUED — `JsoniterPrism[A].replace` write-back (phase 2)

Same carrier, plug splice mechanics into `from`:

```scala
val updated: Array[Byte] = emailP.replace("new@example.com")(bytes)
```

Implementation: replace `from` from identity to:
```scala
val from: Affine[X, A] => Array[Byte] = aff =>
  aff match
    case h: Affine.Hit[X, A] =>
      val (src, start, end) = h.snd
      val encoded = jsoniter.writeToArray(h.b)  // via JsonValueCodec[A]
      // memcpy: src[0, start) ++ encoded ++ src[end, src.length)
      val out = new Array[Byte](start + encoded.length + (src.length - end))
      System.arraycopy(src, 0, out, 0, start)
      System.arraycopy(encoded, 0, out, start, encoded.length)
      System.arraycopy(src, end, out, start + encoded.length, src.length - end)
      out
    case m: Affine.Miss[X, A] => m.fst
```

Estimated: ~30 LoC inside the existing factory + a `JsoniterPrismWriteSpec`. Gated on §6 bench numbers.

### 3.5 QUEUED — cross-module bridge to `eo-circe`

For dynamic-shape escapes after a typed-shape jsoniter drill. Sketch:

```scala
// JsoniterCirceBridge — decode the focused span as a circe `Json` value,
// then continue with eo-circe optics for dynamic-shape work.
val userSubtreeP: Optic[Array[Byte], Array[Byte], Json, Json, Affine] =
  JsoniterPrism[Json]("$.payload.user")  // requires JsonValueCodec[io.circe.Json]
val nameP = userSubtreeP.andThen(JsonPrism[String](_.name))
```

The bridge needs a `JsonValueCodec[io.circe.Json]` instance — non-trivial to derive auto, available manually. Estimated: ~0.5 day. Not worth shipping until a real user wants it.

## 4. Performance — measured

`JsoniterReadBench` lives in `benchmarks/`. Four fixture shapes, run with the project default `-i 5 -wi 3 -f 3` (15 measured iterations across 3 forks):

| Bench | eo-circe (ns/op) | eo-jsoniter (ns/op) | Speedup | Phase-2 bar (≥3×) |
|---|---|---|---|---|
| Hit @ depth 3, `Long` (`$.payload.user.id`) | 799.059 ± 17.169 | 49.929 ± 0.610 | **16.0×** | clear |
| Hit @ depth 4, `String` (`$.payload.user.profile.email`) | 810.451 ± 6.127 | 102.311 ± 1.447 | **7.9×** | clear |
| Miss @ depth 3 (path doesn't exist) | 796.048 ± 16.501 | 69.298 ± 2.448 | **11.5×** | clear |
| `[*]` fold-sum over 10-element array (PSVec carrier) | 798.638 ± 21.105 | 342.476 ± 4.343 | **2.3×** | marginal |

Standard errors are tight — `JsoniterPrism[Long]`'s 49.9 ns/op has a ±0.6 ns half-width.

**Carrier note for the traversal row.** Initially shipped on `MultiFocus[List]` at 350.6 ± 9.5 ns/op (2.3×); swapped to `MultiFocus[PSVec]` for downstream composability with `Traversal.each` (mfAssocPSVec) and tightened to 342.5 ± 4.3 ns/op — modest perf bump (~2%, single Array alloc vs cons-cell allocation), no speedup-ratio change. The composability win is qualitative: the JsoniterTraversal now sits in the same carrier as the entire classical Traversal family.

**Why the scalar speedup is large.** eo-circe's `JsonPrism.foldMap` measures the realistic workload: parse `Array[Byte]` → `Json` AST via `io.circe.parser.parse`, then drill the AST. Most of the 800+ ns/op is in the parse, not the drill. Jsoniter skips the AST entirely — `JsonPathScanner.find` walks the bytes once, jsoniter's `readFromSubArray` decodes only the focused span.

**The miss case is interesting.** `eo-jsoniter` on miss (68 ns/op) is faster than `eo-jsoniter` on the depth-4 hit (104 ns/op) — the scanner aborts as soon as a field doesn't match, before any decode. eo-circe's miss case is the same as the hit case (still pays the parse) at 811 ns/op. **Speedup on miss is 11.9× — better than depth-4 hit.** The original concern that jsoniter's edge would collapse on miss was unfounded.

**The traversal fixture is honest about the perf shape.** `[*]` over a 10-element array gets 2.3× — much narrower than the scalar reads. Why: jsoniter's per-element decode cost (encode each Long into `List[Long]`, ~30 ns/element) plus the spans-list allocation (`List[Span]` of length 10) accumulates to 350 ns/op on the eo-jsoniter side. The eo-circe side is dominated by the parse (~800 ns regardless), so the array fold post-parse is cheap. **Traversal still wins, but by less.** A larger array would push the speedup higher (constant parse cost spread over more elements); this 10-element fixture is the conservative case.

**Caveats.**
- The eo-circe number includes UTF-8 conversion (`new String(bytes, "UTF-8")`) before parse. Real production code typically does this anyway since circe's parser takes a `String`. If you cache the parsed `Json` across calls, eo-circe's per-call cost drops and the speedup narrows. Most real-world workloads (HTTP handlers, log line processors) ARE one-shot parse+drill; this bench reflects that.
- `String` focus (8.0×) is slower than `Long` focus (16.6×) because String decode has more allocation — a UTF-8 substring copy. The Long focus is closer to the theoretical floor.
- These numbers are a quick-run; trust them indicatively, not absolutely. JMH's standard caveats apply (machine noise, JIT warmup, GC).

**Verdict for phase-2 gating:** scalar bars (16.2× / 7.9× / 11.9×) clear by a wide margin; traversal sits at 2.3× — under the ≥3× original bar but above the 1.5× "marginal" bar. Read-write splice ships in phase 2 anyway: the scalar fixtures dominate real-world workloads and the traversal cost is honest about its shape (more elements per array would push it higher).

Bench source: `benchmarks/src/main/scala/dev/constructive/eo/bench/JsoniterReadBench.scala`. Re-run with `sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 .*JsoniterReadBench.*"`.

## 5. Risks and known limitations

### 5.1 JSONPath subset complexity (status: deliberate scope)

A real JSONPath has filter expressions (`?(@.x > 5)`), wildcards (`[*]`, `.*`), recursive descent (`..`). Phase 1 ships only `$`, `$.field.field`, `$[i]`, dotted chains. Wildcards / filters / recursive descent are rejected at parse time — `PathParser` returns `Left(...)`. Anything richer is the user's job (chain multiple optics, fall through to eo-circe).

The hand-rolled scanner is 246 LoC; adding a dependency on `com.lemonlabs:jsonpath` or `com.gravitydev:json-path-scala` would unlock a richer surface but pull in ~hundreds of KB of transitive deps. **Decision: stay hand-rolled** unless real users push back.

### 5.2 Splice mechanics for `JsoniterPrism.replace` (status: queued, phase 2)

Encoding `A` to bytes is delegated to jsoniter; splicing back into the buffer is straightforward when the new bytes have the same length as the old span. When they don't (typical), we allocate a fresh `Array[Byte]` of new size and memcpy three slices. That's `O(N)` per write, vs `O(depth)` for circe AST replace. **For deep / large documents, this could regress vs eo-circe** on write-heavy workloads. Bench needs to confirm.

### 5.3 No traversal write-back (status: out of scope, phase 2+)

`JsoniterTraversal.modify` (write back into N array elements at once) is structurally hard to splice safely without re-walking the whole array — successive splices invalidate prior offsets. Phase 1 ships read-only Traversal only; the write-back side is "use eo-circe for that case, or re-encode the case class".

### 5.4 Jsoniter's macro requirement (status: accepted)

Every focus type `A` needs a `JsonValueCodec[A]` in scope. cats-eo's design has been macro-light; this re-introduces a macro dependency in the `eo-jsoniter` module's user-facing surface (already present in eo-avro via kindlings-avro-derivation, so the precedent exists). The macros artifact is `Test`-scoped on our build (callers bring their own).

### 5.5 ~~Carrier proliferation~~ — RESOLVED via Affine reuse

Original draft worried about an 8th carrier (`ByteSpanF` + 40 cells of matrix paperwork). Phase 1 reused Affine — **zero new carrier**, **zero new matrix cells**. Risk no longer applies.

### 5.6 Maintenance burden (status: ongoing)

Tracking jsoniter-scala upstream changes, both for codec-maker macros and for stream-reader API drift. eo-circe's circe surface has been remarkably stable; jsoniter's underlying API has more churn. Pinned to `2.38.9` for now; bumps will be Dependabot-driven.

## 6. Build-or-skip — phase 1 + phase 1.5 verdicts, phase 2 gating

**Phase 1: SHIPPED.** Read-only `JsoniterPrism` on the Affine carrier. Commit `481b7cb`.

**Phase 1.5: SHIPPED.** `JsoniterTraversal` on the `MultiFocus[List]` carrier with `[*]` wildcard support. Adds `PathStep.Wildcard`, `JsonPathScanner.findAll`, and the new factory. 6 more specs / 14 more expectations green; full jsoniter spec count is now 12 specs / 35 expectations.

Effort actuals vs estimates (cumulative through phase 1.5):
- Carrier (Affine reuse) + path parser + scanner + Prism factory + Traversal factory: ~3 hours
- Specs (Prism + Traversal): ~1 hour
- Module wiring + scalafmt + bench: ~1 hour
- **Total: ~5 hours actual** through phase 1.5 (vs ~3.5 days estimate for phase 1 alone, sans bench + docs)

Underran the estimate by ~5× because the Affine + MultiFocus[List] carrier reuse eliminated:
- Carrier design + typeclass instances
- Matrix-update paperwork
- Custom extension-method shipping (`.foldMap` / `.headOption` / `.length` / `.exists` all light up free)

**Phase 2 gating: PASS for scalar paths.** Scalar JMH numbers land at 7.9× / 11.9× / 16.2× across the three single-focus fixtures (§4) — comfortably above the 3× gating bar. Traversal sits at 2.3× over a 10-element array, under the original ≥3× bar but above 1.5×. Read-write splice (`JsoniterPrism.replace`) is greenlit for phase 2; traversal write-back is more nuanced (write-side cost dominated by re-encoding each element + cumulative splice O(N) shifts).

Original gating thresholds (for the record):
- ≥3× on `Fold.foldMap` over 1 KB synthetic JSON, depth 3 → ship phase 2. **Hit (16.2×).**
- 1.5–3× → ship phase 2, but flag in docs as "marginal — pick based on workload".
- <1.5× → abandon phase 2.

## 7. Decision matrix — phase 1 outcome

| Question | Original answer | Phase-1 verdict |
|---|---|---|
| Is the carrier conceptually clean? | Proposed new `ByteSpanF` | **Affine reuse — cleaner**. No new carrier. |
| Does it fit cats-eo's existential style? | Yes (assumed new carrier) | **Yes** — `X = (Array[Byte], (Array[Byte], Int, Int))` carries Hit/Miss context. |
| Does it cannibalise eo-circe? | No — different workloads | **No** — confirmed. Different niches. |
| Is the perf claim plausible? | Model says 5–7× on read | **Pending bench** (§4). |
| Is the maintenance cost worth it? | Depends on phase-1 numbers | **Lower than estimated** — Affine reuse means no carrier maintenance, just jsoniter version pins. |
| Could it be skipped entirely? | Yes | Phase 1 was cheap enough that "skip" wouldn't have saved meaningful budget. |

## 8. Open questions still worth pushback

Resolved during phase 1 + 1.5 (no longer open):
- ~~Could `Forget[F]` be reused with `F = ByteSpan`?~~ — Reused **Affine** + **MultiFocus[List]** instead, even cleaner.
- ~~JSONPath dependency vs hand-rolled?~~ — Hand-rolled, ~280 LoC across `find` + `findAll`, no external dep.
- ~~Traversal phase 1.5?~~ — Shipped, 2.3× on 10-element array. Smaller-than-scalar speedup but honest about its shape.

Still open:
- **Phase-2 splice approach.** "Memcpy three slices on every write" or delta-rope structure? Delta-ropes win on hot-loop modify but cost design days. JMH-driven decision after the simple memcpy ships.
- **Phase-2 traversal write-back.** `JsoniterTraversal.modify` is structurally trickier than Prism — each element splice invalidates subsequent spans. Punt to phase 3 unless a user shows up demanding it.
- **Cross-module bridge to eo-circe (§3.5).** Worth shipping in v0.1.0 of eo-jsoniter, or wait for a real user to ask? Inclined toward "wait" — manual `JsonValueCodec[io.circe.Json]` is awkward to derive.

Phase 1 + 1.5 doc revision complete. Phase 2 (Prism splice writes) is the next concrete unit.
