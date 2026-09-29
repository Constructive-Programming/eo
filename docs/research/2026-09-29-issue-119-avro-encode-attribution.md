---
date: 2026-09-29
topic: issue-119-avro-encode-attribution
status: fix landed (per-thread write plumbing in `writeDatum`, route bench, safety pins)
scope: avro binary write path; plus a claim check on `generics` `lens[S]` focus ordering
---

# Issue #119 — decomposing the avro encode gap

Trigger: [issue #119](https://github.com/Constructive-Programming/eo/issues/119)
reported that a kindlings/eo produce path (`AvroCodec.derived` → `AvroEncoder.derived` →
`GenericDatumWriter`) measures ~1.4–1.7× slower and 9–18% more allocated than a hand-written
direct-`BinaryEncoder` writer, and hypothesised the intermediate `GenericData.Record` tree.
The same report arrived in-thread bundled with a claim that `lens[S]` reorders its focus —
checked at the end; it does not.

## The write path, decomposed

A standalone attribution harness (published `cats-eo-avro` **0.17.0**, Scala 3.8.4, JDK 25,
`ThreadMXBean.getThreadAllocatedBytes`; fixture: 15-field record + 12-field all-`Option`
nested + 18-branch union, **245 B** output — the issue's shape at reduced scale; the
hand-written route was byte-identical to the eo route, so the comparison is honest):

| Route                                                       | ns/op | B/op |
|-------------------------------------------------------------|------:|-----:|
| kindlings `encode(a)` → `Any` (tree only)                    |   532 |  664 |
| write datum with **fresh** BAOS + writer + encoder (pre-#119 `writeDatum`) | 1,599 | **2,928** |
| write datum with **reused** BAOS + encoder + cached writer   | 1,311 |  392 |
| full eo route (`AvroCodec.encodeValue`)                      | 1,670 | 3,456 |
| hand-written fields straight to a reused `BinaryEncoder`     |   761 |  397 |

Two findings, and the reporter's hypothesis was only half right:

1. **The time gap is tree + dispatch.** Tree build ≈ 1/3 of it; `GenericDatumWriter`'s
   per-field walk (casts, position reads, `resolveUnion` per union field) ≈ the rest. That is
   the *irreducible* cost of going through avro's datum model and lives in kindlings'
   derivation, not in this repo. Reusing plumbing barely moves ns/op.
2. **The allocation gap was mostly ours.** `writeDatum` allocated a fresh
   `ByteArrayOutputStream` (32-byte start — doubling and copying a dozen times toward a
   multi-KB payload), a fresh `GenericDatumWriter`, and a fresh `BufferedBinaryEncoder`
   (2 KB internal buffer) on **every call** — 2,928 B/op against a 392 B/op reused floor on a
   245 B record, scaling to ~2× the payload once the BAOS growth chain dominates. That is
   the bulk of the reported 9–18% over a hand-written writer that already reuses its buffers.

## The fix

`AvroBinaryCursor.writeDatum` now delegates to a per-thread `DatumWriters` — the exact mirror
of what the READ side has done for a while (`DatumReaders` per-thread reader cache +
reusable `binaryDecoderCache`):

- one `ByteArrayOutputStream` + one `BufferedBinaryEncoder` per writing thread, re-bound via
  `EncoderFactory.binaryEncoder(out, reuse)` (avro's supported reconfigure path: position
  reset, buffer kept);
- `GenericDatumWriter` cached per `Schema` in a `ThreadLocal` `HashMap` — the same shape and
  the same thread-safety reasoning as the reader cache;
- `out.reset()` at the START of each encode and `toByteArray` at the end: every result is a
  fresh detached array (retention-safe), and a mid-write throw leaves no observable residue.

All five call sites (root `encodeRecord`/`encodeValue`, leaf-span splice, `.fields` overlay,
the circe + jsoniter bridge writes) funnel through this one choke point.

Measured after the change (same harness): full eo route **3,456 → 904 B/op** (−74%); the
write side now costs exactly the returned array (~245 B + tree 664 B). Byte-identical output
against the old plumbing, asserted in-harness and covered by the module's exact-bytes specs.

Pins (`AvroWriteCorrectnessSpec`): retention under 200 same-thread writes, clean recovery
after an aborted write, 8-thread concurrent encode equals single-writer golden bytes.

## Re-measured permanently (`AvroEncodeRouteBench`)

`sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 -prof gc .*AvroEncodeRouteBench.*"` — four
routes (tree-only, full `encodeValue`, the pre-fix fresh-plumbing shape kept as `naive_*`
baseline, hand-written stream) on a reduced version of the issue's record. Local quick profile
(one fork, `-prof gc`, gc.alloc.rate.norm — the repo's B/op-is-the-gate doctrine):

| Route                    | ns/op | B/op |
|--------------------------|------:|-----:|
| `eo_encodeToAny`         |    58 |  312 |
| `naive_freshPlumbing`    |   744 | 2,624 |
| `eo_encodeValue` (fixed) |   758 |  568 |
| `handwritten_stream`     |   174 |  240 |

What's left between `eo_encodeValue` and `handwritten_stream` is the tree (≈ 312 B) and
`GenericDatumWriter` dispatch (time). Closing THAT gap is the encode-side twin of the
whole-record-builder work (#95): a derived `A => Encoder => Unit` streaming encoder — which
lives in kindlings-avro-derivation (its `AvroEncoder` typeclass is `A => Any`, so no
macro-side shim in eo can skip the datum). Worth an upstream ask; until then,
byte-carrying optics (`graftBytes`, offset-walk writes) remain the zero-tree escape hatch,
and the reported producer-vs-handwritten ALLOCATION delta is effectively gone on this side.

## The bundled lens claim — not reproducible

Claim: "`lens[Person](_.age, _.name)` should give focus `("age" -> Int, "name" -> String)`
but instead produces `("name" -> String, "age" -> Int)`." Checked against published
**cats-eo-generics 0.14.0** (the release current when the report was drafted) and **0.17.0**,
and against `HEAD`:

- a `BijectionIso[Person, Person, NamedTuple[("age","name"), (Int,String)], …]` ascription
  on `lens[Person](_.age, _.name)` **compiles** on both versions (all `Optic` parameters are
  invariant, so a declaration-order focus could not typecheck);
- the compiler's own revealed type for that call:
  `BijectionIso[Person, Person, (age : Int, name : String), …]` — selector order;
- a partial-cover pin, `SimpleLens[Employee, NamedTuple[("salary","id"), …], …]` for
  `lens[Employee](_.salary, _.id)` (declaration order would be `("id","salary")`), also
  compiles; named field access + laws run correctly at runtime;
- source-level: `buildMultiLens` / `buildMultiIso` have fed `namedTupleTypeOf(selectedNames,
  …)` in selector order since the macro exists (v0.14.0 included), and `GenericsSpec`
  exercises both the reversed and declaration-order selector spellings.

The `("name" -> String, "age" -> Int)` display shape is a *dealiased* NamedTuple printing
(`Map[Values, Labels]` pairs); the current contract prints as `(age : Int, name : String)`.
If a future reporter can attach the exact version and the code that produced that display,
re-open — as filed, the claim does not reproduce.
