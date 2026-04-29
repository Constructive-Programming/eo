# eo-jsoniter spike — design sketch

**Date:** 2026-04-29
**Status:** **Phase 1 SHIPPED 2026-04-29** (commit `481b7cb`). Read-only optics on the **`Affine` carrier — no new carrier**. Phase 2 (read-write splice) gated on JMH numbers.
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

### 3.3 QUEUED — `JsoniterTraversal[A]` over arrays

Not in phase 1. Shape would be `Optic[Array[Byte], Array[Byte], A, A, Affine]` over a `[*]` path step that produces multiple spans, walked via the existing MultiFocus[List] machinery once we add a `JsonPathScanner.findAll(bytes, path): List[Span]` companion to the existing `find`. Estimated: ~1 day.

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

`JsoniterReadBench` lives in `benchmarks/`. Three fixture shapes, run with the project default `-i 5 -wi 3 -f 3` (15 measured iterations across 3 forks):

| Bench | eo-circe (ns/op) | eo-jsoniter (ns/op) | Speedup | Phase-2 bar (≥3×) |
|---|---|---|---|---|
| Hit @ depth 3, `Long` (`$.payload.user.id`) | 835.054 ± 10.587 | 50.336 ± 0.532 | **16.6×** | clear |
| Hit @ depth 4, `String` (`$.payload.user.profile.email`) | 807.099 ± 9.252 | 100.731 ± 1.727 | **8.0×** | clear |
| Miss @ depth 3 (path doesn't exist) | 817.634 ± 4.469 | 69.491 ± 2.404 | **11.8×** | clear |

All three exceed the ≥3× gating bar set in §6 by a wide margin. Standard errors are tight — `JsoniterPrism[Long]`'s 50.3 ns/op has a ±0.5 ns half-width.

**Why the speedup.** eo-circe's `JsonPrism.foldMap` measures the realistic workload: parse `Array[Byte]` → `Json` AST via `io.circe.parser.parse`, then drill the AST. Most of the 800+ ns/op is in the parse, not the drill. Jsoniter skips the AST entirely — `JsonPathScanner.find` walks the bytes once, jsoniter's `readFromSubArray` decodes only the focused span.

**The miss case is interesting.** `eo-jsoniter` on miss (69 ns/op) is faster than `eo-jsoniter` on the depth-4 hit (100 ns/op) — the scanner aborts as soon as a field doesn't match, before any decode. eo-circe's miss case is the same as the hit case (still pays the parse) at 818 ns/op. **Speedup on miss is 11.8× — better than depth-4 hit.** The original concern that jsoniter's edge would collapse on miss was unfounded.

**Caveats.**
- The eo-circe number includes UTF-8 conversion (`new String(bytes, "UTF-8")`) before parse. Real production code typically does this anyway since circe's parser takes a `String`. If you cache the parsed `Json` across calls, eo-circe's per-call cost drops and the speedup narrows. Most real-world workloads (HTTP handlers, log line processors) ARE one-shot parse+drill; this bench reflects that.
- `String` focus (8.0×) is slower than `Long` focus (16.6×) because String decode has more allocation — a UTF-8 substring copy. The Long focus is closer to the theoretical floor.
- These numbers are a quick-run; trust them indicatively, not absolutely. JMH's standard caveats apply (machine noise, JIT warmup, GC).

**Verdict for phase-2 gating:** all three bars cleared. Read-write splice should ship in phase 2.

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

## 6. Build-or-skip — phase 1 verdict and phase 2 gating

**Phase 1: SHIPPED.** Read-only `JsoniterPrism` on the Affine carrier. 553 LoC across 5 files (4 main, 1 test). 6 specs / 21 expectations green. scalafmtCheckAll clean. Commit `481b7cb`.

Effort actuals vs estimates:
- Carrier + path parser: ~30 min (vs 1 day estimate — the Affine reuse cut the carrier-design half to zero)
- Optic factory: ~20 min (single class, ~85 LoC)
- Spec: ~30 min
- Module wiring + scalafmt + iterating on warnings: ~40 min
- **Total: ~2 hours actual** (vs ~3.5 days estimate, sans bench + docs which are still pending)

Underran the estimate by ~10× because the Affine reuse eliminated:
- Carrier design + typeclass instances
- Matrix-update paperwork
- Custom extension-method shipping (`.foldMap` etc. light up free)

**Phase 2 gating: PASS.** Phase-1 JMH numbers landed at 8.0× / 11.8× / 16.6× across the three fixtures (§4) — comfortably above the 3× gating bar. Read-write splice (`JsoniterPrism.replace`) is greenlit for phase 2.

Original gating thresholds (for the record):
- ≥3× on `Fold.foldMap` over 1 KB synthetic JSON, depth 3 → ship phase 2. **Hit (16.6×).**
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

Resolved during phase 1 (no longer open):
- ~~Could `Forget[F]` be reused with `F = ByteSpan`?~~ — Reused **Affine** instead, even cleaner.
- ~~JSONPath dependency vs hand-rolled?~~ — Hand-rolled, 246 LoC, no external dep.

Still open:
- **Phase-2 splice approach.** "Memcpy three slices on every write" or delta-rope structure? Delta-ropes win on hot-loop modify but cost design days. Decision after phase-1 perf bench.
- **Cross-module bridge to eo-circe (§3.5).** Worth shipping in v0.1.0 of eo-jsoniter, or wait for a real user to ask? Inclined toward "wait" — manual `JsonValueCodec[io.circe.Json]` is awkward to derive.
- **Traversal phase 1.5.** Worth ~1 day to add `JsonPathScanner.findAll` + `JsoniterTraversal[A]` over `[*]` paths before phase 2? Probably yes — unlocks the array-fold story which is half the read use case. Queue ahead of phase 2.

Phase 1 doc revision complete. Bench is the immediate next step.
