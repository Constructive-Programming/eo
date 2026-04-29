# eo-jsoniter spike — design sketch

**Date:** 2026-04-29
**Status:** Draft — design only, no code. Build-or-skip decision pending review.
**Context:** Companion module to [`eo-circe`](../../circe/) for jsoniter-scala. Goal of this spike: figure out whether `eo-jsoniter` would *complement* eo-circe (different niche, both ship), *replace* it (jsoniter's perf wins, eo-circe goes away), or *be redundant* (jsoniter users don't need an AST optic). Spoiler: complement. The interesting question is the carrier shape.

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

## 2. Carrier shape sketch

### 2.1 The byte-cursor carrier

```scala
package dev.constructive.eo.jsoniter

/** Existential carries the source bytes + a span [start, end) of the
  * focused JSON value within them. `start` and `end` are byte offsets
  * into the same backing `Array[Byte]`; the focused span is a single
  * top-level JSON value (object/array/scalar). The cursor is positioned
  * such that `bytes(start)` is the first byte of the value (skipping
  * leading whitespace) and `bytes(end - 1)` is its last byte
  * (a closing `}`, `]`, `"`, digit, or letter).
  */
final class ByteSpan[A](
    val bytes: Array[Byte],
    val start: Int,
    val end: Int,
)

/** Carrier for `JsoniterPrism` / `JsoniterTraversal`. Mirrors `Forget[F]`
  * shape — phantom outer existential, focus carries the byte slice. */
type ByteSpanF = [X, A] =>> Forgetful[X, ByteSpan[A]]
```

The carrier is logically `Forget[ByteSpan]`-shaped: one-way map from `Array[Byte]` source to a span at a JSON path, plus a codec witness `JsonValueCodec[A]` to decode the span on demand.

### 2.2 Why a new carrier (not Forget[F])

Two reasons cats-eo can't reuse `Forget[F]` directly:

1. **The focused value is parameterised by `A`** but the F-shape `Forget[F]` requires `F` to be a unary type constructor independent of `A`. `ByteSpan[A]` is structurally `(Array[Byte], Int, Int)` *plus* a phantom `A` for the codec dispatch — closer to `Const[ByteSpan, A]` than to `Forget[List]`.
2. **Write-back is non-trivial.** A circe optic's `from: Json => Json` is just tree replacement. A byte-span optic's `from: ByteSpan[A] => Array[Byte]` has to *splice* the new bytes into the surrounding buffer (encode `A` to bytes, then memcpy the prefix + new bytes + suffix). That splice is an operation `from: (newBytes: Array[Byte], oldSpan: ByteSpan[A]) => Array[Byte]` — which doesn't fit any existing carrier signature in cats-eo cleanly.

So eo-jsoniter introduces one new carrier: `ByteSpanF` (or a richer pair-carrier, see §2.3). Peer to `PSVec` and `Affine` in the carrier landscape.

### 2.3 Read-only vs read-write split

Two scopes worth surveying:

- **`JsoniterFold[A]`** = `Optic[Array[Byte], Unit, A, A, ByteSpanF]` — read-only path. The byte-span optic decodes A from the slice, T = Unit. **Cheap and clean.** This subsumes 80 % of jsoniter's typical workload (decode a field, project a value, never write back).

- **`JsoniterPrism[A]`** = a real read-write optic. Needs a richer carrier `ByteSpanRW` that holds the source bytes, the span, *and* enough surrounding context to splice a re-encoded `A` back. Splice-aware Prism behaviour is more complex; `from` becomes "encode A → bytes, splice into source". 

Recommend the spike start with the read-only Fold first: 70 % of the unique value at 30 % of the carrier-design cost. Read-write follows once the read-side cursor mechanics are validated.

## 3. Four representative APIs

### 3.1 `JsoniterFold[A].apply` — decode at a path

```scala
import com.github.plokhotnyuk.jsoniter_scala.macros.{JsonCodecMaker, CodecMakerConfig}
import dev.constructive.eo.jsoniter.JsoniterFold

case class User(id: Long, email: String)
given JsonValueCodec[User] = JsonCodecMaker.make

val userF: JsoniterFold[User] = JsoniterFold[User]("$.payload.user")

val bytes: Array[Byte] = ...  // raw HTTP body
userF.foldMap(_.email)(bytes)  // String — zero AST allocation
```

Path syntax: a JSONPath subset (`$.field.field`, `$.array[0]`, `$.field[*]` — the last is the Traversal case). Parsed at construction; runtime path-walk is a state machine over the byte buffer.

### 3.2 `JsoniterTraversal[A].each` — fold over array elements

```scala
val itemsT = JsoniterTraversal[Item]("$.cart.items[*]")

itemsT.foldMap(_.priceCents)(bytes)        // Long — sum without AST
itemsT.length(bytes)                       // Int — count without parse
itemsT.exists(_.outOfStock)(bytes)         // Boolean — short-circuit on first hit
```

Traversal carrier is `MultiFocus[ByteSpanList]` where `ByteSpanList` holds the array's element spans (lazy). Per-element decode happens inside the user's `foldMap` callback, only for elements actually inspected.

### 3.3 `JsoniterPrism[A]` — read-write splice (deferred to phase 2)

```scala
val emailP: JsoniterPrism[String] = JsoniterPrism[String]("$.payload.user.email")

val updated: Array[Byte] = emailP.replace("new@example.com")(bytes)
// New byte buffer, original untouched.
```

`replace` re-encodes the focus to bytes via `JsonValueCodec[String]` and splices into the source buffer at the cursor. Cost: `O(prefix + newSpan + suffix)` memcpy, no AST. Worth a benchmark vs eo-circe's `JsonPrism[A].replace` to confirm the perf claim survives splice overhead.

### 3.4 Cross-module composition with `eo-circe`

```scala
import dev.constructive.eo.circe.JsonPrism
import dev.constructive.eo.jsoniter.JsoniterFold

// Read-only escape: jsoniter byte-span → circe Json (one parse)
val userJsonP: Optic[Array[Byte], Unit, Json, Json, Forget[Option]] =
  JsoniterFold.parsedAsJson("$.payload.user")  // decode just that subtree
val nameP = userJsonP.andThen(JsonPrism[String](_.name))
```

Bridges so users can drill jsoniter for the hot path and fall through to circe-optics for the dynamic-shape parts. Requires one Composer or one carrier-bridge bridge (`ByteSpanF → Forget[Option]` via "decode subtree to Json").

## 4. Performance model and quick perf-target table

Indicative numbers, not measured. Targets to hit/justify in the spike:

| Operation | eo-circe (circe parse + optic) | eo-jsoniter (target) | Why |
|---|---|---|---|
| `Fold.foldMap` over 1 KB JSON, depth 3 | baseline | 0.15× (≈7× faster) | Skip AST, walk bytes |
| `Traversal.foldMap` over 100-element array, sum field | baseline | 0.20× | Skip AST, decode only the field at each element |
| `Prism.replace` on a top-level field | baseline | 0.40× | Splice + re-encode; some overhead |
| `Prism.modify` requiring full re-parse | baseline | ~1.0× (no win) | When the modify changes structure beyond the focus, the splice trick degrades |
| Cold codec compile (`JsonCodecMaker.make`) | n/a | ~100 ms+ per type | Compile-time cost — irrelevant to runtime, relevant to developer feedback loop |

**Conclusion of the model:** eo-jsoniter wins on the hot path (read-mostly, scalar-focus, byte-volume-bound) and matches eo-circe on the workloads where the bottleneck is the decode itself. It loses *availability* on dynamic-shape cases that don't have a codec.

## 5. Risks and open questions

1. **JSONPath subset complexity.** A real JSONPath has filter expressions (`?(@.x > 5)`), wildcards on objects, recursive descent (`..`). Spike ships only the trivial subset (`$.field`, `$[i]`, `$[*]`, dotted chains). Anything richer punts to the user composing multiple optics. Likely fine, but worth declaring up front.

2. **Splice mechanics for `Prism.replace`.** Encoding A to bytes is delegated to jsoniter; splicing back into the buffer is straightforward when the new bytes have the same length as the old span. When they don't (typical), we allocate a fresh `Array[Byte]` of new size and memcpy three slices. That's O(N) per write, vs O(depth) for circe AST replace. **For deep / large documents, this could regress vs eo-circe.** Need a bench.

3. **No traversal write-back.** `JsoniterTraversal.modify` (write back into N array elements at once) is structurally hard to splice safely without re-walking the whole array. Phase-1 ships the read-only Traversal only; the write-back side is "use eo-circe for that case, or re-encode the case class".

4. **Jsoniter's macro requirement.** Every focus type `A` needs a `JsonValueCodec[A]` in scope. cats-eo's design has been macro-light; this re-introduces a macro dependency in the eo-jsoniter module (already present in eo-avro via kindlings-avro-derivation, so the precedent exists).

5. **Carrier proliferation.** Adding `ByteSpanF` brings the carrier count to 8 (Forgetful, Tuple2, Either, Affine, SetterF, Forget[F], MultiFocus[F], + ByteSpanF). The composition-gap matrix grows by one row and column. Each new carrier costs ~40 cells of paperwork even when most are U.

6. **Maintenance burden.** Tracking jsoniter-scala upstream changes, both for codec-maker macros and for stream-reader API drift. eo-circe's circe surface has been remarkably stable; jsoniter's underlying API has more churn.

## 6. Honest build-or-skip recommendation

**Recommend: build the read-only spike (§3.1, §3.2) first, defer §3.3 / §3.4, decide on §2.3 read-write phase after the bench numbers come in.**

Phase-1 effort estimate (read-only `JsoniterFold` + `JsoniterTraversal`, no write):
- Carrier + path parser: ~1 day
- Optic factories + foldMap/headOption/length/exists extensions: ~1 day
- JMH bench vs eo-circe on three representative shapes: ~1 day
- Tests + spec consolidation: ~1 day
- Docs (cookbook recipe + page in `site/docs/jsoniter.md`): ~0.5 day

Total: ~4.5 days for a phase-1 ship with read-only optics, JMH numbers, and a clear "yes, this is the right complement to eo-circe" or "no, it's not worth the carrier" verdict.

Phase 2 (read-write `JsoniterPrism`) is gated on phase-1's perf numbers being convincing. If the read path doesn't win convincingly (target: ≥3× on `Fold.foldMap`), abandon — jsoniter without the perf advantage is just inconvenience.

## 7. Decision matrix

| Question | Answer |
|---|---|
| Is the carrier conceptually clean? | Yes — `ByteSpan[A]` is well-defined, peer to existing carriers. |
| Does it fit cats-eo's existential style? | Yes — phantom `X`, `A` carries the codec, mirrors `Forget[F]` shape. |
| Does it cannibalise eo-circe? | No — different workloads. eo-circe stays for dynamic-shape work; eo-jsoniter takes the typed hot path. |
| Is the perf claim plausible? | Yes — model says 5–7× on read paths. Bench will confirm. |
| Is the maintenance cost worth it? | Probably — depends on phase-1 numbers. |
| Could it be skipped entirely? | Yes — users with codecs already have `lens[CaseClass]`. The optic over-bytes story is a "nice to have", not a "we're missing this". |

Calling it: **fund the phase-1 spike (4.5 days), gate phase-2 on read-side bench numbers, ship eo-circe and eo-jsoniter as complementary modules if both clear the bar**.

## 8. Open invitation for review

This document deliberately commits to no implementation. Things worth pushback on before any code is written:

- **The carrier shape (§2)** — would a non-trivial reuse of `Forget[F]` actually work? E.g., `F = ByteSpan` as a unary constructor on `A`, with the codec witness threaded via implicit context? If so, the new-carrier story collapses and the spike gets cheaper.
- **The path-parser scope (§5.1)** — should we ship a dependency on `com.lemonlabs:jsonpath` or write a 100-LoC walker for the trivial subset? The latter keeps eo-jsoniter dep-light; the former unlocks a richer surface immediately.
- **The §3.4 cross-bridge** — necessary for v0.1.0 of eo-jsoniter, or punt to a follow-up?
- **The phase-2 splice approach** — is "memcpy three slices on every write" acceptable, or do we want a delta-rope structure? Delta-ropes win on hot-loop modify but cost design days.

Pending review.
