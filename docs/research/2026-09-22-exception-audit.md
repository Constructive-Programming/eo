---
date: 2026-09-22
topic: exception-audit
status: pass 1 landed (circe walk + jsoniter path construction); P1 decided — (a) documented + pinned, carrier tracked as #117
scope: src/main of core, laws, circe, avro, jsoniter, zio, kyo, schemes, generics
---

# Exception audit — `throw` as control flow, and its sibling `catch NonFatal`

Trigger: "find anywhere we might be abusing exceptions, e.g. using `throw` as control flow."
This is the inventory, the verdict per site, and what changed.

## Headline

- **One real control-flow exception existed** — circe's `JsonWalk.MissSignal` (a private
  `ControlThrowable` used as a non-local exit). Removed in this pass; the walk threads `Either`.
- **One public constructor family threw on parseable input** — `JsoniterPrism.fromPath` /
  `JsoniterTraversal(path)`. Both are now a SINGLE `fromPath` that returns `Either[String, …]`; the
  throwing forms are gone (breaking change, recorded in `mima.sbt`).
- **33 `catch NonFatal` sites, 20 `throw`/`sys.error` statements** in `src/main`. Most catches are
  the *correct* boundary pattern (a third-party throwing API converted into `Either`/`Ior` once).
  The ones that matter are the **silent write fallbacks** in `avro`/`jsoniter`, because they report
  success while writing nothing — a product decision, not a mechanical fix (P1 below).
- **`core`, `laws`, `zio`, `kyo`, `schemes`, `generics` contain no exception-based control flow at
  all.** Every `throw` there is a JDK collection contract, an unreachable invariant guard, or the
  documented poisoned read of `Unfold.algebra`.

## How to re-run the inventory

```sh
# throw statements + sys.error (statement position; ignores prose mentions)
grep -rn --include=*.scala -E '^\s*(else )?throw new|sys\.error|fold\([a-z] => throw' */src/main

# every NonFatal catch (both spellings: bare and scala.util.control-qualified)
grep -rn --include=*.scala -c 'case NonFatal' */src/main
grep -rn --include=*.scala -E 'case scala\.util\.control\.NonFatal' */src/main

# throwable-as-control-flow markers
grep -rn --include=*.scala -E 'ControlThrowable|NonLocalReturn|ControlFlow' */src
```

### What is out of scope, and why

`src/test` and `benchmarks/src/main` were read but not counted: the throws there are
`throwAn[…]` assertions, fixture helpers (`fold(f => throw …, identity)`), and `ConfluentWire.SchemaById`
stubs — which exist *because* of G, not as separate findings. `src/main` is where a throw can reach a
library user.

## Taxonomy and verdicts

### A. Throwable as a control-flow signal — the abuse class

| Site | Pattern | Verdict |
|------|---------|---------|
| `circe/.../JsonWalk.scala` (was `MissSignal`, `miss()`, `try`/`catch` in `modifyPath`) | private `ControlThrowable` thrown by the walk and by the terminal callback, caught in the same method | **Removed (2026-09-22).** `modifyPath` now takes `f: Json => WalkResult` (`JsonFailure \| Json`) and short-circuits on the failure arm; the module has no `try`/`catch` left. Zero cost — see below. |

The design was *disciplined* — `ControlThrowable` carries no stack trace, is invisible to
`NonFatal`, and could not escape `modifyPath`. But it was the one place in the codebase where a
failure was signalled by unwinding the stack instead of by returning a value, and it made every
consumer (`JsonFocus`: 8 `modifyPath` call sites raising `miss` 7 times; `JsonTraversal`: 1) spell a
failure as `JsonWalk.miss(...): Nothing` — a signature that lies about what the call does. Nothing
else in the tree does this.

### B. `throw` on input that has a representable failure

| Site | Pattern | Verdict |
|------|---------|---------|
| `jsoniter/.../JsoniterPrism.scala` (`fromPath`), `JsoniterTraversal.scala` (`apply`) | `PathParser.parse` returns `Either[String, ...]`; the caller discarded it and re-threw as `IllegalArgumentException` | **Fixed (2026-09-22), the honest way round:** both companions now expose ONE string-path constructor, `fromPath`, returning `Either[String, …]`. The throwing `fromPath` and `JsoniterTraversal.apply(path)` were REMOVED rather than kept as edges (breaking; `mima.sbt` records it) — a path is data, so the type says so and there is no throwing twin to reach for. |

### C. Silent fallbacks driven by `catch NonFatal` — the consequential class (P1, open)

These are the sites that can lose data quietly. All of them sit at `Optic.from`, which is **total by
type** (`from: F[X, B] => T`): there is nowhere in the signature to put a failure, so the code catches
the throwable and returns the input unchanged.

| Site | Behaviour on failure |
|------|----------------------|
| `avro/.../AvroFocus.scala` — 7 catches returning the input record: `:113`, `:126`, `:153`, `:162`, `:263`, `:371`, `:378` | `replace`/`modify` on a record focus returns the ORIGINAL record; the caller sees success. (5 further catches at `:42`, `:199`, `:212`, `:405`, `:414` return the input WITH an `Ior.Both` failure chain — same write result, but diagnosed.) |
| `avro/.../circe/AvroJson.scala:442`, `avro/.../jsoniter/AvroJsoniter.scala:499` | unparseable Avro bytes re-apply the retained subtree: document bytes unchanged, success reported |
| `jsoniter/.../JsoniterTraversal.scala:71` | a focus whose encode throws **keeps its original bytes** while the other foci are written — a partially applied write reported as success |
| `jsoniter/.../JsoniterTraversal.scala:175` (`decodeSpans`), `JsoniterPrism.scala:135` (root-prism `scan`) | a jsoniter decode throw downgrades to "no focus" |
| `avro/.../jsoniter/AvroJsoniter.scala:273` (`parseSlice`) | same, as `Option[Any]` |

Two different things are mixed here and deserve different treatment:

1. **Write-side silence is a doctrine violation.** `AvroWalk.requireFieldNamed`'s own scaladoc
   (`avro/.../AvroWalk.scala:485-489`) names this exact failure class as the thing to avoid: *"reads
   return `None` and writes pass the payload through unchanged while reporting success"*. The
   fallbacks above are that shape, one layer below where the schema can check.
2. **Read-side downgrade is defensible** (a miss is a miss), but the exception does double duty:
   jsoniter hands back ONE exception type for "these bytes are malformed" *and* "these bytes are
   fine but not this codec's shape", so a corrupt document is indistinguishable from an ordinary
   miss. `avro`'s `AvroCodec` does not have this problem — it wraps into typed `AvroFailure` cases at
   the boundary.

The real fix is the **failure-typed write** already scoped in
`docs/brainstorms/2026-06-10-failure-typed-build-biaffine.md` — a carrier whose `from` can miss
(`BiAffine` / `WriteCompose`), which is exactly "every remaining hole in the family space". Interim
options, cheapest first:

- (a) **Document + pin**: keep the pass-through, state it on each member's scaladoc, and add tests
  pinning it as intended (today only the `Ior` surface's failures are pinned). — **DECIDED AND
  LANDED 2026-09-22**; see "What landed" items 4-5.
- (b) **Fail loud**: delete the `catch` and let the encoder's throwable propagate out of
  `replace`/`modify`. This is a **behaviour change** on a public API — writes that used to no-op now
  raise — so it needs an explicit decision. Rejected for now.
- (c) **Carrier**: change nothing locally; land it in the biaffine spike. — the destination, tracked
  as **issue #117**, which carries the site-by-site case list.

A diagnostic wart in the same family, worth fixing when (c) lands: on the avro record face an
**encode** failure surfaces as `AvroFailure.DecodeFailed` (`AvroFocus.decodeOrFail`'s `onHit` arm,
`AvroFocus.scala:41-46`), so the failure names the wrong seam. The pinned test asserts the case, not
the name.

### D. Third-party throwing API → structured failure at one boundary — the correct pattern

| Site | Verdict |
|------|---------|
| `avro/.../AvroCodec.scala` (5 catches), `AvroBinaryCursor.scala` (6; an `*Uncaught` internal body plus a single public wrapper), `ConfluentWire.scala` (4) | **Keep.** apache-avro, Jackson and kindlings all throw; converting once at the edge into `AvroFailure.BinaryParseFailed` / `DecodeFailed` / `EncodeFailed` / `ResolveFailed` / `SchemaResolutionFailed` is what that type is for. The `walkElementsUncaught` ↔ `locateElements` naming split (`AvroBinaryCursor.scala:94`, `:129`) makes the boundary explicit. |
| `circe/.../JsonFailure.scala` (`parseInputIor` / `parseInputUnsafe`), every `Ior` member on `JsonPrism` / `JsonTraversal` | **Keep / exemplar.** No throwable anywhere in a public failure path. |
| `jsoniter` decode + scan (3 catches), `avro/.../AvroJsoniter.scala:273` | **Keep as mechanism** — jsoniter ships no `Either`-returning read API, so the catch *is* the boundary. See C.2 for the two-meaning caveat. |

### E. `throw` at optic-construction time (loud by design)

| Site | Verdict |
|------|---------|
| `avro/.../AvroWalk.scala:414`, `:466`, `:475`, `:510`; `avro/.../AvroTraversal.scala:192`, `:194`, `:201` | **Keep (for now).** These fire from macro-expanded `.field` / `.fieldNamed` calls when schema and case class disagree — i.e. at the call site that is wrong, with the candidate names in the message. Construction-time is eager, not deferred. An `Either`-returning constructor would push a schema-vs-case-class match into every user's happy path for a condition they cannot act on; the same shape as B is available if that changes. |
| `avro/.../AvroWalk.scala:272`, `:287` (`sys.error`) | **Keep.** Defensive arms of `rebuildStep`, unreachable while the walk's schema descent holds. Note they would surface as a silent no-op write through C's catch — one more argument for (a)/(b) above. |

### F. Invariant guards (unreachable by construction)

| Site | Verdict |
|------|---------|
| `core/.../MultiFocus.scala:289` (`pickSingletonOrThrow`) | **Keep.** Called only from `Composer` bridges whose source optic is single-focus by type; the 2026-04-23 code-quality review reached the same conclusion. Pinned by `InternalsCoverageSpec`. |
| `core/.../MultiFocus.scala:94` (`MultiFocusFromList[Option]`) | **Keep.** `Option`'s carrier cardinality is 0/1 by construction; reachable only if a user hand-writes an optic that lies about its focus count. |
| `core/.../PSVec.scala:191`, `:194`, `:204` | **Keep.** JDK collection contract (`apply` out of bounds, `head` on empty). Anything else makes `PSVec` the odd collection out. |
| `core/.../optics/Unfold.scala:127` | **Keep.** `Unfold.algebra` has no `Applicative[F]`, so the vestigial `to: () => F[Unit]` has no lawful inhabitant; the throw marks the read side unavailable and no build-only operation touches it. Pinned by `UnfoldSpec`. |

### G. Throwing callback contracts (user code must throw)

| Site | Verdict |
|------|---------|
| `avro/.../ConfluentWire.scala:64` — `type SchemaById = Int => Schema` | **Follow-up (P2).** A total function type whose documented contract is "may throw". Containment is sound (each call site catches into `AvroFailure.SchemaResolutionFailed`), but every user and every test fixture has to write `id => throw ...`. A `SchemaById.fromEither(Int => Either[Throwable, Schema])` / `fromTry` companion lets a registry client that already returns `Either` plug in without throwing. |
| `avro/.../vulcan/AvroVulcan.scala:42`, `:43` | **Keep, documented.** `AvroCodec.encode` is total by interface and `schema` is resolved once at construction; both map vulcan's `Either` onto a throw, with the error-mapping rule spelled out in the object scaladoc. Same family as C — the fix is a fallible build signature, not a local change. |

## What landed in this pass (2026-09-22)

1. **`circe/.../JsonWalk.scala`** — `MissSignal` / `miss` deleted; `modifyPath` takes
   `f: Json => WalkResult` and the union failure arm short-circuits every splice frame
   (`case failure: JsonFailure => failure`). Call sites updated in `JsonFocus.scala` (8) and
   `JsonTraversal.scala` (1). Public behaviour is unchanged: the whole root-aggregate suite passes
   (`sbt test`, every module green, 0 failures), including every law-based spec and
   `JsonIndexBoundsSpec`'s mutant-hunting bounds property.

   **The channel is a union, not an `Either`** (`type WalkResult = JsonFailure | Json`). Framing the
   failure as a value is what matters; `Either` would additionally box `Right`/`Left` once per
   splice frame for no information, since both arms are already heap objects. `[[readPath]]` keeps
   `Either` — it is tail-recursive, so it boxes once per *call* rather than once per hop, and its
   callers want the `Either` combinators.

   **Cost, measured** on the `OrderCirceBench.eoStreet` shape
   (`codecPrism[Order].field(_.customer).field(_.address).field(_.street).modifyUnsafe(_.toUpperCase)`,
   depth 3; B/op via `ThreadMXBean.getCurrentThreadAllocatedBytes`, 200k iterations after 4x200k
   warmup — the same quantity JMH's `-prof gc` reports, and the one the `JsonWalk` scaladoc quotes;
   three runs, identical to the byte):

   | | before (`ControlThrowable`) | after (union) | delta |
   |---|---|---|---|
   | hit | 1080 B/op | 1080 B/op | **0** — no box per frame |
   | miss | 80 B/op | 40 B/op | **-40 B/op** (the old design allocated a `MissSignal` Throwable on top of the failure) |

   (An intermediate `Either[JsonFailure, Json]` revision measured +64 B/op on the hit path, which is
   what motivated the union.) CI's `bench-pr.yml` should still confirm ns/op on a quiet machine: this
   pass could not run JMH locally, because the `benchmarks` project depends on `kyoIntegration`,
   which needs JDK 25.

2. **`jsoniter/.../JsoniterPrism.scala` + `JsoniterTraversal.scala`** — one string-path
   constructor each, `fromPath`, returning `Either[String, …]`; the throwing forms are deleted
   (breaking; `mima.sbt` + CHANGELOG carry the migration). Messages are unchanged, so
   `PathParserBoundarySpec`'s contract holds; the pins in `JsoniterPrismSpec` /
   `JsoniterTraversalSpec` assert the `Left` payloads (malformed path, `[*]` redirect). Call sites
   migrated across the jsoniter/avro specs (a test-scope `JsoniterPathFixtures` unwraps literal
   paths), the benchmark harnesses (fail-fast on literal paths), the docs site and the agent
   skill.

3. **`circe/src/test/.../JsonWalkSpec.scala`** — new contract pins: a terminal failure comes back
   as-is (the same object, never a partially rebuilt document), a miss below the root short-circuits
   every splice frame, `f` does not run on a miss, and a throwable from `f` propagates instead of
   being swallowed as a miss.

4. **P1 (a) — the write-side pass-through is now documented wherever a user meets it**: a normative
   bullet in `AvroPrism`'s and `JsoniterPrism`'s *Laws & preconditions*, a paragraph in
   `JsoniterTraversal`'s class doc, the `AvroFocus.navigateForWrite` writer contract, and the two
   user-facing pages (`site/docs/integrations/avro.md`, `.../jsoniter.md`). Each states the rule
   (silent tier: input unchanged, success reported; `*Ior` tier: the diagnostic), not just the
   behaviour.

5. **P1 (a) — pinned as INTENDED, so it cannot drift before the carrier lands**:
   `AvroWriteCorrectnessSpec` ("an encoder that throws on the NEW value passes the record through")
   proves the decode works and only the encode refuses, then asserts the silent tier returns the
   *same* record instance while the Ior twin carries `DecodeFailed`;
   `JsoniterTraversalWriteSpec` pins the PARTIAL case (`[1,2,3]` + `_+1` through an odd-refusing
   codec → `[2,2,4]`: one element keeps its ORIGINAL bytes while its siblings are written) and the
   all-refused case (payload byte-identical). Both cite issue #117.

6. **Issue [#117](https://github.com/Constructive-Programming/eo/issues/117)** opened — "the
   fallible-write carrier (BiAffine) — where a total `from` forces a throw or a silent pass-through":
   the site-by-site case list, the acceptance sketch (laws with negative fixtures, `WriteCompose`,
   migration, and the 0 B/op allocation bar this pass established) and the non-goals.

## What to do next (priority order)

1. **P1 — decided** (C): (a) landed, carrier tracked as [#117](https://github.com/Constructive-Programming/eo/issues/117).
   This was the only finding here that can silently lose data.
2. **P2 — `SchemaById` `Either` combinator** (G), so callers are never *required* to write a throw.
3. **Hygiene — ratchet** (below), so new `throw` / `catch NonFatal` in `src/main` is a conscious
   choice rather than an accident.
4. **Optional** — `AvroWalk`'s construction-time throws into `Either`-returning constructors with the
   throwing ones kept as edges (the shape used in B). Only worth it if a caller wants to react to a
   schema/optic mismatch instead of crashing.
5. **Destinations** — `docs/brainstorms/2026-06-10-failure-typed-build-biaffine.md` (a fallible
   `from`) and `Unfold.algebra`'s read-side hole are two faces of the same missing thing: a
   build-side failure channel.

### Ratchet (proposal — not landed)

A `tests/` spec that walks `*/src/main/**/*.scala`, counts `throw` in statement position and
`catch NonFatal`, and fails unless every site appears in a checked-in allowlist (this file's
inventory). That turns "move away from this practice" into a reviewable invariant instead of a
convention that decays. It would read the source tree from the test JVM — a new shape for this repo —
so it is worth doing only if the allowlist is expected to shrink.
