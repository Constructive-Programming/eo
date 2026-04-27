# Avro integration

The `cats-eo-avro` module adds two cursor-backed optics for
editing [Apache Avro](https://avro.apache.org/) records without
paying the cost of a full decode / re-encode round-trip.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-avro" % "@VERSION@"
```

## Why this exists

Avro lives in streaming pipelines: Kafka topics, S3 sinks, Schema
Registry-backed services. Payloads arrive on the wire as
`Array[Byte]` (binary) or `String` (Avro JSON), each one carrying
its own schema. The classical Scala edit pattern is:

```scala
codec.decode(bytes).map { p =>
  p.copy(name = p.name.toUpperCase)
}.map(codec.encode)
```

That decodes every field of `Person`, allocates a fresh
instance, and re-encodes every field — even when only one
leaf is changing. For wide records the work is mostly wasted,
and the whole pipeline budget is spent on serialisation rather
than on whatever business logic the consumer actually wants to
run.

`AvroPrism` / `AvroTraversal` walk a flat path directly through
the [`IndexedRecord`](https://avro.apache.org/docs/current/api/java/org/apache/avro/generic/IndexedRecord.html)
representation, modifying only the focused leaf and rebuilding
the parents on the way up. The
[`AvroOpticsBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/AvroOpticsBench.scala)
suite documents the speedup against the kindlings-avro-derivation
codec round-trip — see the
[Avro benchmarks section](benchmarks.md#avroprism--direct-walk-over-indexedrecord)
for the full table.

The codec backend is
[kindlings-avro-derivation](https://github.com/MateuszKubuszok/kindlings-avro-derivation)
0.1.2, which pins apache-avro 1.12.1. cats-eo-avro wraps the
kindlings `AvroEncoder[A]` / `AvroDecoder[A]` / `AvroSchemaFor[A]`
triplet in a single [`AvroCodec[A]`](https://github.com/Constructive-Programming/eo/blob/main/avro/src/main/scala/dev/constructive/eo/avro/AvroCodec.scala)
typeclass so user code summons one thing per type.

## AvroPrism

```scala mdoc:silent
import dev.constructive.eo.avro.{AvroCodec, codecPrism}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}

case class Address(street: String, zip: Int)
object Address:
  given AvroEncoder[Address] = AvroEncoder.derived
  given AvroDecoder[Address] = AvroDecoder.derived
  given AvroSchemaFor[Address] = AvroSchemaFor.derived

case class Person(name: String, age: Int, address: Address)
object Person:
  given AvroEncoder[Person] = AvroEncoder.derived
  given AvroDecoder[Person] = AvroDecoder.derived
  given AvroSchemaFor[Person] = AvroSchemaFor.derived
```

Construct a Prism to the root type, then drill into fields.
The `.address.street` sugar is macro-powered — it compiles to
`.field(_.address).field(_.street)`:

```scala mdoc
import org.apache.avro.generic.GenericRecord

val alice   = Person("Alice", 30, Address("Main St", 12345))
val record  = summon[AvroCodec[Person]].encode(alice).asInstanceOf[GenericRecord]
val streetP = codecPrism[Person].address.street

streetP.modifyUnsafe(_.toUpperCase)(record)
  .asInstanceOf[GenericRecord].get("address")
```

The default `modify` returns `Ior[Chain[AvroFailure], IndexedRecord]` —
failures are surfaced rather than silently swallowed, mirroring
`JsonPrism`'s default surface. The `*Unsafe` variants ship the
silent-pass-through hot path used by Kafka consumers that have
measured and don't want the diagnostic allocation.

Other operations (all the silent escape hatches):

```scala mdoc
streetP.getOptionUnsafe(record)
streetP.placeUnsafe("Broadway")(record)
  .asInstanceOf[GenericRecord].get("address")
streetP.transformUnsafe(any => any)(record).getSchema.getName
```

Forgiving semantics on the `*Unsafe` surface — missing paths leave
the record unchanged:

```scala mdoc
import org.apache.avro.generic.GenericData

// A stump record carrying only `name` and `age` — the `address`
// slot is null at the schema-allocated position.
val stumpSchema = summon[AvroCodec[Person]].schema
val stump = new GenericData.Record(stumpSchema)
stump.put(stumpSchema.getField("name").pos, "Alice")
stump.put(stumpSchema.getField("age").pos, 30)
// `address` left null — the walker hits `NotARecord` at that step.

streetP.modifyUnsafe(_.toUpperCase)(stump).asInstanceOf[GenericRecord].get("address")
```

## Array indexing

`.at(i)` drills into the `i`-th element of an Avro `array<T>`:

```scala mdoc:silent
case class Order(name: String)
object Order:
  given AvroEncoder[Order] = AvroEncoder.derived
  given AvroDecoder[Order] = AvroDecoder.derived
  given AvroSchemaFor[Order] = AvroSchemaFor.derived

case class Basket(owner: String, items: List[Order])
object Basket:
  given AvroEncoder[Basket] = AvroEncoder.derived
  given AvroDecoder[Basket] = AvroDecoder.derived
  given AvroSchemaFor[Basket] = AvroSchemaFor.derived
```

```scala mdoc
val basket = Basket("Alice", List(Order("X"), Order("Y"), Order("Z")))
val basketRec =
  summon[AvroCodec[Basket]].encode(basket).asInstanceOf[GenericRecord]
val secondName = codecPrism[Basket].items.at(1).name

secondName.getOptionUnsafe(basketRec)
secondName.modifyUnsafe(_.toUpperCase)(basketRec)
  .asInstanceOf[GenericRecord].get("items").toString
```

Out-of-range / negative / non-array positions surface as
`Ior.Both(chain, inputRecord)` on the default surface — the
walker accumulates `IndexOutOfRange` / `NotAnArray` /
`PathMissing` and threads the original record back through.

## AvroTraversal (`.each`)

`.each` splits the path at the current array focus and returns
an `AvroTraversal` that walks every element. Further `.field` /
`.at` / selectable-sugar calls on the traversal extend the
per-element suffix:

```scala mdoc
val everyName = codecPrism[Basket].items.each.name

everyName.modifyUnsafe(_.toUpperCase)(basketRec)
  .asInstanceOf[GenericRecord].get("items").toString
everyName.getAllUnsafe(basketRec)
```

Empty arrays and missing prefixes leave the record unchanged on
the silent surface; on the default Ior surface, prefix-walk
failures land in `Ior.Left` (nothing to iterate) and per-element
skips accumulate one `AvroFailure` per refused element into
`Ior.Both(chain, partialRecord)`.

## Multi-field focus — `.fields(_.a, _.b)`

`.fields(selector1, selector2, ...)` focuses a bundle of named
case-class fields as a Scala 3 `NamedTuple`. Selectors arrive in
selector-order; the NamedTuple type reflects that. Arity must be
≥ 2 — use `.field(_.x)` for a single-field focus.

```scala mdoc:silent
type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
given AvroEncoder[NameAge] = AvroEncoder.derived
given AvroDecoder[NameAge] = AvroDecoder.derived
given AvroSchemaFor[NameAge] = AvroSchemaFor.derived
```

```scala mdoc
val nameAge = codecPrism[Person].fields(_.name, _.age)

nameAge
  .modifyUnsafe(nt => (name = nt.name.toUpperCase, age = nt.age + 1))(record)
  .asInstanceOf[GenericRecord].get("name")
```

Full-cover selection — spanning every field of `Person` — still
returns an `AvroFieldsPrism`, **not** an `Iso`. Avro decode can
always fail (the input may not even be record-shaped, the codec
may refuse a field, the union branch may not match) so totality
isn't witnessable, and an Iso would misleadingly advertise a
guarantee we cannot provide.

Per-element multi-field focus via `.each.fields` focuses a
NamedTuple on every element of an array — same shape as
`JsonTraversal`'s `.each.fields`, just over the Avro carrier.

## Union branches — `.union[Branch]`

Avro's schema-driven `union<...>` types — including the standard
`Option[A]` encoding as `union<null, A>` and sealed-trait sums —
demand a per-branch resolution step. cats-eo-avro spells this as
`.union[Branch]`:

```scala mdoc:silent
case class Transaction(id: String, amount: Option[Long])
object Transaction:
  given AvroEncoder[Transaction] = AvroEncoder.derived
  given AvroDecoder[Transaction] = AvroDecoder.derived
  given AvroSchemaFor[Transaction] = AvroSchemaFor.derived
```

```scala mdoc
val txRec = summon[AvroCodec[Transaction]]
  .encode(Transaction("t-1", Some(42L))).asInstanceOf[GenericRecord]

val amountP = codecPrism[Transaction].field(_.amount).union[Long]

amountP.modifyUnsafe(_ + 1L)(txRec)
  .asInstanceOf[GenericRecord].get("amount")
```

`.union[Branch]` works against `Option[A]` (`union<null, A>`),
sealed-trait sums (`union<RecordA, RecordB, ...>`), and Scala 3
enums. It does **not** accept Scala 3 `A | B` union types — those
have no schema-side mapping in kindlings. When the runtime value
sits on a different branch than the requested one, the walker
surfaces `AvroFailure.UnionResolutionFailed` carrying the
schema-declared alternatives so the caller can route on the
mismatch.

## Reading diagnostics from the default Ior surface

The default `modify` / `transform` / `place` / `transfer` / `get`
methods on `AvroPrism`, `AvroFieldsPrism`, `AvroTraversal`, and
`AvroFieldsTraversal` all return
`Ior[Chain[AvroFailure], IndexedRecord]` (or `, A]` /
`, Vector[A]]` for the reads). Three observable shapes:

- `Ior.Right(record)` — clean success.
- `Ior.Both(chain, record)` — partial success. The `record`
  reflects every update that did succeed; the `chain` lists
  every skip.
- `Ior.Left(chain)` — no result producible. Typical for `get`
  misses, parse failures, and traversal-prefix misses where
  there's nothing to iterate.

### Failure flow

The diagram below traces the path an
`IndexedRecord | Array[Byte] | String` input takes through a
default-surface read/modify, showing which `AvroFailure` case
lands in the chain at each refusal point and which `Ior` shape
the caller observes.

```mermaid
flowchart TD
  input["IndexedRecord | Array[Byte] | String"] --> parsed{parses?}
  parsed -- "no (Array[Byte])" --> binFail["BinaryParseFailed"]
  parsed -- "no (String)" --> jsonFail["JsonParseFailed"]
  binFail --> iorLeft(["Ior.Left(chain)"])
  jsonFail --> iorLeft
  parsed -- yes --> walk{prefix walk}
  walk -- "field on non-record" --> notRec["NotARecord"]
  walk -- "field absent" --> pathMissing["PathMissing"]
  walk -- "at on non-array" --> notArr["NotAnArray"]
  walk -- "index out of range" --> idxOOR["IndexOutOfRange"]
  walk -- "branch mismatch" --> unionFail["UnionResolutionFailed"]
  walk -- "bad enum symbol" --> enumFail["BadEnumSymbol"]
  walk -- "decoder refused" --> decodeFail["DecodeFailed"]
  walk -- "clean path" --> travStep{traversal?}
  notRec --> opShape1{operation}
  pathMissing --> opShape1
  notArr --> opShape1
  idxOOR --> opShape1
  unionFail --> opShape1
  enumFail --> opShape1
  decodeFail --> opShape1
  opShape1 -- "get / prefix-miss traversal" --> iorLeft
  opShape1 -- "modify / place / transform" --> iorBoth(["Ior.Both(chain, inputRecord)"])
  travStep -- "no (single focus)" --> iorRight(["Ior.Right(record)"])
  travStep -- "yes" --> perElt{per-element walk}
  perElt -- "every element OK" --> iorRight
  perElt -- "some elements skip" --> iorBothPartial(["Ior.Both(chain, partialRecord)"])
  perElt -- "empty array" --> iorRight
```

- Parse-step failures (`BinaryParseFailed`, `JsonParseFailed`)
  surface as `Ior.Left` regardless of operation — there's no
  record to thread back.
- Prefix-walk failures (the `NotARecord` / `PathMissing` /
  `NotAnArray` / `IndexOutOfRange` / `UnionResolutionFailed` /
  `BadEnumSymbol` / `DecodeFailed` branches) surface as
  `Ior.Left` on read operations and `Ior.Both(chain, inputRecord)`
  on modify operations — the unchanged input record rides along
  so the caller can keep walking.
- Per-element traversal skips (the `.each` path) accumulate one
  `AvroFailure` per refused element and land in `Ior.Both`
  together with the partially-updated record.

## Failure model

Nine `AvroFailure` cases cover every refusal point on a walk.
Each carries the `PathStep` at which the walker refused, plus
case-specific context. The first five mirror `JsonFailure`
case-for-case; the four schema-driven cases are Avro-only.

```scala mdoc:silent
import dev.constructive.eo.avro.{AvroFailure, PathStep}
```

**`PathMissing`** — named field absent from its parent record.
Mirrors `JsonFailure.PathMissing`:

```scala mdoc
val pm: AvroFailure = AvroFailure.PathMissing(PathStep.Field("street"))
pm.message
```

**`NotARecord`** — parent wasn't a record at the walk position
(so the walker couldn't look up a field by name). Avro's
schema-driven analogue of `JsonFailure.NotAnObject`:

```scala mdoc
val nr: AvroFailure = AvroFailure.NotARecord(PathStep.Field("address"))
nr.message
```

**`NotAnArray`** — parent wasn't an array at the walk position
(so the walker couldn't index). Mirrors `JsonFailure.NotAnArray`:

```scala mdoc
val na: AvroFailure = AvroFailure.NotAnArray(PathStep.Index(2))
na.message
```

**`IndexOutOfRange`** — index outside `[0, size)`. The actual
array length rides along on the failure case so callers can
route on it:

```scala mdoc
val oor: AvroFailure = AvroFailure.IndexOutOfRange(PathStep.Index(7), 3)
oor.message
```

**`DecodeFailed`** — the kindlings codec refused at the focused
leaf. The wrapped `Throwable` is whatever the underlying decoder
threw — typically an `AvroRuntimeException`, `AvroTypeException`,
or `ClassCastException` when the runtime payload doesn't line up
with the schema:

```scala mdoc
val cause = new RuntimeException("missing record field 'name'")
val df: AvroFailure = AvroFailure.DecodeFailed(PathStep.Field("name"), cause)
df.message
```

**`BinaryParseFailed`** — input `Array[Byte]` didn't parse as
Avro binary under the supplied reader schema. Surfaced only by
the dual-input overloads; when the caller passes a parsed
`IndexedRecord` directly this case cannot fire:

```scala mdoc
val binCause = new RuntimeException("EOF before record complete")
val bpf: AvroFailure = AvroFailure.BinaryParseFailed(binCause)
bpf.message
```

**`JsonParseFailed`** — input `String` didn't parse as Avro JSON
wire format under the supplied reader schema. Surfaced only by
the triple-input overloads; record / bytes input cannot trigger
this case. Distinct from circe's general-purpose JSON parser —
this is apache-avro's `JsonDecoder`, which expects the
schema-aware Avro JSON encoding (with branch-tagged unions
etc.):

```scala mdoc
val jsonCause = new RuntimeException("Expected int. Got VALUE_STRING")
val jpf: AvroFailure = AvroFailure.JsonParseFailed(jsonCause)
jpf.message
```

**`UnionResolutionFailed`** — walker reached a `union<...>` value
but none of the candidate branches matched the runtime type. The
schema-declared alternatives ride along so the caller can route
on the mismatch. Schema-driven; no JSON analogue:

```scala mdoc
val urf: AvroFailure = AvroFailure.UnionResolutionFailed(
  branches = List("null", "long"),
  step    = PathStep.UnionBranch("long"),
)
urf.message
```

**`BadEnumSymbol`** — walker reached an enum value whose runtime
symbol isn't a member of the schema's declared symbol set.
Schema-driven; no JSON analogue. Reserved for the enum-aware
walker hooks:

```scala mdoc
val bes: AvroFailure = AvroFailure.BadEnumSymbol(
  symbol = "MAGENTA",
  valid  = List("RED", "GREEN", "BLUE"),
  step   = PathStep.Field("color"),
)
bes.message
```

Pattern-match on the case to route to per-cause log / metric
streams; the structured `PathStep` is the same shape across
every case so a single `step` accessor in user code is enough
for "which cursor position refused":

```scala mdoc
def route(chain: cats.data.Chain[AvroFailure]): List[String] =
  chain.toList.map {
    case AvroFailure.PathMissing(step)            => s"miss:    $step"
    case AvroFailure.NotARecord(step)             => s"shape:   $step (not record)"
    case AvroFailure.NotAnArray(step)             => s"shape:   $step (not array)"
    case AvroFailure.IndexOutOfRange(step, n)     => s"bounds:  $step (size=$n)"
    case AvroFailure.DecodeFailed(step, c)        => s"decode:  $step: ${c.getMessage}"
    case AvroFailure.BinaryParseFailed(c)         => s"bytes:   ${c.getMessage}"
    case AvroFailure.JsonParseFailed(c)           => s"json:    ${c.getMessage}"
    case AvroFailure.UnionResolutionFailed(bs, s) => s"union:   $s (branches: ${bs.mkString(",")})"
    case AvroFailure.BadEnumSymbol(sym, valid, s) => s"enum:    $s '$sym' valid=${valid.mkString(",")}"
  }
```

## Bytes / String / record input — parse on the fly

Every edit and read method on `AvroPrism` / `AvroFieldsPrism` /
`AvroTraversal` / `AvroFieldsTraversal` accepts
`IndexedRecord | Array[Byte] | String` as the source. When the
input is bytes, the library parses it through apache-avro's
`BinaryDecoder` under the reader schema cached on the prism;
when the input is a string, parsing goes through
apache-avro's `JsonDecoder` (Avro JSON wire format, not the
generic JSON encoding circe ships). Parse failures surface
through the same `AvroFailure` accumulator as every other
failure mode.

The triple-input shape is the streaming-pipeline default:
Kafka consumers receive `Array[Byte]`, REST handlers receive
`String`, and intermediate stages pass parsed `IndexedRecord`
forward — one optic surface, three call sites.

```scala mdoc:silent
import java.io.ByteArrayOutputStream
import org.apache.avro.io.EncoderFactory
import org.apache.avro.generic.GenericDatumWriter

def toBinary(rec: GenericRecord): Array[Byte] =
  val out = new ByteArrayOutputStream()
  val encoder = EncoderFactory.get().binaryEncoder(out, null)
  val writer = new GenericDatumWriter[GenericRecord](rec.getSchema)
  writer.write(rec, encoder)
  encoder.flush()
  out.toByteArray

val incomingBytes: Array[Byte] = toBinary(record)
val upperName = codecPrism[Person].field(_.name).modify(_.toUpperCase)
```

```scala mdoc
// Happy path: parsed, modified, Ior.Right.
upperName(incomingBytes).map(
  _.asInstanceOf[GenericRecord].get("name").toString
)

// Bad bytes: Ior.Left(Chain(AvroFailure.BinaryParseFailed(_))).
upperName(Array(0.toByte))
```

```scala mdoc
// Same prism, Avro JSON string input.
val incomingJson: String = """{"name":"Alice","age":30,"address":{"street":"Main St","zip":12345}}"""
upperName(incomingJson).map(
  _.asInstanceOf[GenericRecord].get("name").toString
)
```

Handing in an `IndexedRecord` directly still works unchanged —
the widened `(IndexedRecord | Array[Byte] | String) => _`
signature is a supertype of the parsed-input shape, and the
parse cost is zero when the input is already a record.

On the `*Unsafe` surface, unparseable bytes / strings fall back
to a synthetic empty record built from the reader schema —
there's no meaningful "input unchanged" semantic for bytes that
aren't Avro, and the whole point of `*Unsafe` is to drop failure
detail. Callers who need parse diagnostics stay on the default
Ior-bearing surface.

## Schema sourcing — runtime vs. derived

By default `codecPrism[S]` reads the schema off the in-scope
`AvroCodec[S]` (which kindlings derives from the case class
shape). For the streaming-pipeline case where the reader schema
arrives at runtime — from an `.avsc` file or a Schema
Registry lookup — the explicit-schema overload accepts it
directly:

```scala
import org.apache.avro.Schema
import dev.constructive.eo.avro.AvroPrism

val readerSchema: Schema = /* loaded from registry */ ???
val explicit = AvroPrism.codecPrism[Person](readerSchema)
```

The user-supplied schema overrides whatever `codec.schema`
would produce; the codec is still summoned (kindlings needs
both sides for encode + decode) but the wire-format boundary
defers to the explicit reader schema. This is the standard
pattern when the producer schema and the consumer schema can
drift independently.

## Ignoring failures (the `*Unsafe` escape hatch)

For Kafka consumers and other hot-path call sites where the
caller has measured and knows they don't want the Ior
allocation, every default method has a sibling `*Unsafe` variant
that ships the silent-pass-through hot path:

```scala mdoc
val nameP = codecPrism[Person].field(_.name)

// Hot-path: silent pass-through on miss.
nameP.modifyUnsafe(_.toUpperCase)(stump).asInstanceOf[GenericRecord].get("name")

// Equivalent via the default surface:
nameP
  .modify(_.toUpperCase)(stump)
  .getOrElse(stump)
  .asInstanceOf[GenericRecord].get("name")
```

Both spellings produce the same record. The first pays nothing
for diagnostics; the second gives an observable `Ior` at the
price of one allocation. Pick the surface that matches the
call-site budget — every prism / traversal class ships both.

## Surface summary

| Class                     | Default (Ior-bearing)                                                          | `*Unsafe` (silent)            |
|---------------------------|--------------------------------------------------------------------------------|-------------------------------|
| `AvroPrism[A]`            | `modify(f): In => Ior[Chain[AvroFailure], IndexedRecord]`                      | `modifyUnsafe(f)`             |
| `AvroPrism[A]`            | `transform(f): In => Ior[...]`                                                 | `transformUnsafe(f)`          |
| `AvroPrism[A]`            | `place(a): In => Ior[...]`                                                     | `placeUnsafe(a)`              |
| `AvroPrism[A]`            | `transfer(f): In => C => Ior[...]`                                             | `transferUnsafe(f)`           |
| `AvroPrism[A]`            | `get(in): Ior[..., A]`                                                         | `getOptionUnsafe`             |
| `AvroFieldsPrism[A]`      | same five                                                                      | same five                     |
| `AvroTraversal[A]`        | `modify(f) / transform(f) / place(a) / transfer(f) / getAll(in): Ior[...]`     | `modifyUnsafe / ... / getAllUnsafe` |
| `AvroFieldsTraversal[A]`  | same five                                                                      | same five                     |

`In` = `IndexedRecord | Array[Byte] | String` everywhere.

## When to reach for which

| Task                                                       | Use                                                |
|------------------------------------------------------------|----------------------------------------------------|
| Edit one leaf deep in an Avro record                       | `AvroPrism` via `.address.street` sugar            |
| Edit element `i` of an Avro array                          | `codecPrism[…].items.at(i).…`                      |
| Edit every element of an Avro array                        | `codecPrism[…].items.each.…` + `modify`            |
| Read every element's focus                                 | `codecPrism[…].items.each.…` + `getAll`            |
| Edit multiple fields atomically                            | `codecPrism[…].fields(_.a, _.b).modify(...)`       |
| Resolve an `Option[A]` / sealed-trait branch               | `codecPrism[…].field(_.amount).union[A]`           |
| Observe why a modify was a silent no-op                    | default Ior `.modify(...)` — inspect the chain     |
| Edit Kafka payloads in place (binary in, binary out)       | `AvroPrism.modifyUnsafe(...)` on `Array[Byte]`     |
| Parse + edit Avro JSON wire payloads                       | `AvroPrism.modify(...)` on `String`                |

For the Kafka end-to-end recipe (read bytes, modify, re-emit),
see the [Cookbook → Kafka payload edit](cookbook.md#kafka-payload-edit--binary-in-binary-out).
For the full failure-mode matrix and the per-case behaviour
specs, see
[`AvroPrismSpec`](https://github.com/Constructive-Programming/eo/blob/main/avro/src/test/scala/dev/constructive/eo/avro/AvroPrismSpec.scala)
and siblings in the `avro/src/test/scala/` tree.
