# Decoupling and modularity

Because an optic is a concrete, portable value relating two types,
you can replace a direct dependence on a *data type* with a
dependence on the *relationship*: a function that needs "every
`Instant` in whatever you give me" asks for the optic, not the
type. You can also say precisely how much you need — the guarantees
of an Iso, or just the freedom of a fold — and modules that share
no types at all can still interoperate at the call site, or even at
the wire-format level.

```scala mdoc:silent
import dev.constructive.eo.optics.{Lens, Optic, Prism, Traversal}
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus.given   // Functor / Foldable / Traverse for MultiFocus[PSVec] (post-fold)
```

## Require the optic, not the type

**Why:** the article's `adjustSheetTimes` shouldn't have to know
about `BalanceSheet`. Any function whose real job is "apply `f` at
every `Instant`" can take the optic as its parameter and let the
caller decide what structure it runs against — the dependence on
the concrete type evaporates:

```scala mdoc:silent
import java.time.Instant
import dev.constructive.eo.data.{MultiFocus, PSVec}

def adjustTimes[S](t: Optic[S, S, Instant, Instant, MultiFocus[PSVec]])(
    f: Instant => Instant
): S => S = t.modify(f)
```

Any structure that can point at its `Instant`s gets the behaviour —
all it takes is that structure's own optic. A record whose
timestamps sit in two fields, and a bare collection, are served by
the same function:

```scala mdoc:silent
case class Meeting(subject: String, start: Instant, end: Instant)

val meetingTimes =
  Lens[Meeting, (Instant, Instant)](
    m => (m.start, m.end),
    (m, se) => m.copy(start = se._1, end = se._2),
  ).andThen(Traversal.two[(Instant, Instant), (Instant, Instant), Instant, Instant](
    _._1,
    _._2,
    (_, _),
  ))

val bumpMeeting = adjustTimes(meetingTimes)(_.plusSeconds(3600))
val bumpAll     = adjustTimes(Traversal.each[List, Instant])(_.plusSeconds(3600))
```

```scala mdoc
bumpMeeting(Meeting("standup", Instant.EPOCH, Instant.EPOCH.plusSeconds(900)))
bumpAll(List(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
```

`Meeting` and `List[Instant]` share no interface, no parent trait,
no typeclass — the optic *is* the interface. This is the article's
central move: interoperability between otherwise wholly independent
modules, done at the call site, with the structure-owning module
publishing one optic value instead of exposing its shape.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468>
("Modular Application Design").

## Re-use an optic across representations

**Why:** two subsystems rarely agree on representation — one thinks
in UTC `Instant`s, another in wall-clock `LocalDateTime`s; one holds
the domain record, another the wire tuple it serialises to. An optic
is a value relating two types, and `Optic` carries two lawful
`cats.arrow.Profunctor` instances that let you *re-aim* a published
optic instead of re-deriving it: `Optic.innerProfunctor` maps the
focus pair (what the optic yields, and what it accepts back), and
`Optic.outerProfunctor` maps the source pair (what it reads from,
and what it rebuilds).

Re-aim the focus — `meetingTimes` above yields `Instant`s, but the
calendar subsystem edits wall-clock times:

```scala mdoc:silent
import java.time.{LocalDateTime, ZoneOffset}

val meetingWallClock =
  Optic
    .innerProfunctor[Meeting, Meeting, MultiFocus[PSVec]]
    .dimap(meetingTimes)((wall: LocalDateTime) => wall.toInstant(ZoneOffset.UTC))(
      LocalDateTime.ofInstant(_, ZoneOffset.UTC)
    )
```

```scala mdoc
meetingWallClock.modify(_.plusHours(2))(
  Meeting("standup", Instant.EPOCH, Instant.EPOCH.plusSeconds(900))
)
```

Re-aim the source — the scheduling gateway never sees `Meeting`, only
the `(startEpochSecond, endEpochSecond)` pair it puts on the wire.
The same optic serves it too:

```scala mdoc:silent
val wireTimes =
  Optic
    .outerProfunctor[Instant, Instant, MultiFocus[PSVec]]
    .dimap(meetingTimes)((p: (Long, Long)) =>
      Meeting("wire", Instant.ofEpochSecond(p._1), Instant.ofEpochSecond(p._2))
    )(m => (m.start.getEpochSecond, m.end.getEpochSecond))
```

```scala mdoc
wireTimes.modify(_.plusSeconds(3600))((0L, 900L))
```

The same moves cover any representation split: UTF-8 vs UTF-16 text
(dimap through the re-encoding pair), big- vs little-endian words
(dimap through `java.lang.Long.reverseBytes`), or a legacy envelope
around the record you actually care about.

One caveat: `dimap` returns an anonymous `Optic`, so the generic
capability surface (`.modify`, `.foldMap`, …) applies but
concrete-class fused overloads are erased. When the concrete optic
ships its own input-side mapping — `Prism.tearFrom` / `mendFrom` —
prefer that form on hot paths.

## Depend only on what's needed

**Why:** the recipe above still pins the carrier — it accepts
traversals only, and the optic travels as an explicit argument.
Most functions need even less: "something I can fold `Double`s out
of", "something I can rewrite `Double`s through". The
[capability traits](../capabilities.md) — `CanGet`, `CanGetOption`,
`CanModify`, `CanFold`, … — are exactly those contracts as proper
types, so the optic arrives as **`using` evidence** and the subject
type stays fully generic. This is the article's
`implicit T: Traversal[T, DateTime]` move, with the carrier erased:

```scala mdoc:silent
import dev.constructive.eo.{CanFold, CanModify}

// Read-side contract: anything that can fold Doubles out of S —
// Lens, Prism, Optional, Traversal, Fold, Iso all qualify.
def total[S](s: S)(using o: CanFold[S, Double]): Double =
  o.foldMap(identity)(s)

// Write-side contract: anything that can rewrite Doubles in place.
def scale[S](k: Double)(using cm: CanModify[S, Double]): S => S =
  cm.modify(_ * k)
```

```scala mdoc:silent
import dev.constructive.eo.generics.lens

case class Line(desc: String, amount: Double)
case class Invoice(fee: Double, lines: List[Line])

val feeL = lens[Invoice](_.fee)

val lineAmounts =
  lens[Invoice](_.lines)
    .andThen(Traversal.each[List, Line])
    .andThen(lens[Line](_.amount))
```

Concrete optic classes *implement* the capabilities, so a lens can
be handed over as the evidence itself; an optic known only at the
generic `Optic[…, F]` type — like the composed traversal — is bound
as a `given` and the capability is derived on the spot:

```scala mdoc
val inv = Invoice(5.0, List(Line("widgets", 10.0), Line("gadgets", 20.0)))

// A Lens IS a CanFold — pass it as the evidence directly.
total(inv)(using feeL)

// The composed traversal is an anonymous Optic; bind it as a given
// and both contracts derive from it.
given Optic[Invoice, Invoice, Double, Double, MultiFocus[PSVec]] = lineAmounts
total(inv)
scale(1.1)(inv)
```

The trait ladder, weakest first: `CanFold` (`foldMap` /
`headOption` / `exists` / `length` / `foci`), `CanGetOption`,
`CanModify` (`modify` / `replace`), `CanGet`, `CanReverseGet`
(build). A signature that demands only `CanFold` accepts
everything; one that demands both `CanGet` and `CanReverseGet`
insists on an Iso. Under the hood each trait is gated by the
matching typeclass on the carrier (`ForgetfulFold[F]`,
`PartialAccessor[F]`, `ForgetfulFunctor[F]`, `Accessor[F]`,
`ReverseAccessor[F]`) — a signature can still take `Optic[…, F]`
plus the gate directly when it genuinely needs the carrier; see
[Capabilities](../capabilities.md) for the full matrix, the coherence
rules, and the using-clause ordering footgun that page documents
for hand-written gate signatures.

**Source:** Hansen — *We Need More Optics* ("We can eliminate the
dependence on the BalanceSheet type by requiring a Traversal
instead"; "you are able to specify if you need the guarantees of an
Iso, or the freedom of a Getter").

## Serdes free pipes

**Why:** a Kafka consumer gets an `Array[Byte]` payload, needs to
change one field, and re-emits binary on the producer side. The
conventional answer couples the consumer to the *whole* schema — a
decode into the full case-class tree per message, on a hot path
where that is pure overhead. `AvroPrism` decouples it: the module
depends on one field's optic, not on `OrderEvent`. It IS a
byte-carried optic (`Optic[Array[Byte], Array[Byte], A, A,
Affine]`): it locates the focused field's byte span under a cached
reader schema, decodes only that slice, and writes by splicing the
re-encoded focus back into the payload — no `IndexedRecord`
materialised on either side:

```scala mdoc:silent
import dev.constructive.eo.avro as eoavro
import dev.constructive.eo.avro.AvroCodec
import java.io.ByteArrayOutputStream
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory

// Hosted in dev.constructive.eo.docs with kindlings-derived
// AvroEncoder / AvroDecoder / AvroSchemaFor givens:
//
//   case class OrderEvent(orderId: String, customer: String, total: Double)
import dev.constructive.eo.docs.OrderEvent

// Stand-in for an inbound Kafka record: serialise an OrderEvent to
// binary under the same schema the prism caches.
val outSchema = summon[AvroCodec[OrderEvent]].schema
val sample = OrderEvent("ord-42", "alice", 99.99)
val sampleBytes: Array[Byte] =
  val rec = summon[AvroCodec[OrderEvent]].encode(sample).asInstanceOf[GenericRecord]
  val out = new ByteArrayOutputStream()
  val encoder = EncoderFactory.get().binaryEncoder(out, null)
  val writer = new GenericDatumWriter[GenericRecord](outSchema)
  writer.write(rec, encoder)
  encoder.flush()
  out.toByteArray

// The optic: walk into `customer` once at construction time,
// reuse on every inbound record. Qualified through the `eoavro`
// alias: eo-circe has a `codecPrism` of its own.
val upperCustomer = eoavro.codecPrism[OrderEvent].customer
```

```scala mdoc
// Kafka hot path: bytes in → splice in place → bytes out. No
// re-serialisation step — the prism's write side IS the emit path.
val outBytes: Array[Byte] =
  upperCustomer.modify(_.toUpperCase)(sampleBytes)

// Round-trip witness — decode the output to confirm the customer
// field changed and the rest is preserved.
(upperCustomer.getOption(outBytes),
 eoavro.codecPrism[OrderEvent].getOption(outBytes))
```

`.modify` on the byte optic is silent-pass-through — bad bytes,
missing fields, or decode mismatches leave the input payload
untouched (an `Affine` Miss) rather than allocating an `Ior`
chain. That matches the Kafka consumer budget: at-least-once
delivery already implies the consumer must be tolerant of
malformed payloads at the offset commit boundary, and the
per-record allocation cost of `Ior` is a tax on the happy path.
When you DO want the diagnostic — for a dead-letter queue, say —
flip to the record-carried face:
`upperCustomer.record.modify(...)` accepts the same bytes and
returns `Ior[Chain[AvroFailure], IndexedRecord]`; route on the
`Ior.Both` / `Ior.Left` shape from there.

The cached reader schema is the load-bearing piece: a single
`codecPrism[OrderEvent]` value pins the schema once, and the
parser reuses it across millions of inbound records. For the
schema-registry case where the reader schema arrives at
runtime, use the explicit-schema overload —
`AvroPrism.codecPrism[OrderEvent](runtimeSchema)` — to bypass
the kindlings-derived schema entirely.

**Source:** cats-eo internal (`AvroPrism`'s byte-carried
default surface). Background framing on the streaming /
Kafka use case lives in the
[Avro integration intro](../integrations/avro.md#why-this-exists).

## Inspectable cross-format bridges

**Why:** the consumer reads Avro off a topic and must emit JSON
to a partner API — or receives JSON and must re-emit Avro. Two
modules, two wire formats, and the only thing they genuinely share
is one field. The conventional bridge couples them anyway: decode
the whole record into a case class, build a whole output value,
encode it all. With a byte optic on each side of the seam, only the
moved branch is ever decoded and re-encoded: the full object is
**never constructed** on either format. (`AvroJsonBridgeSpec`
proves this with a counting root codec — both bridge directions run
with zero root decodes and zero root encodes; `AvroJsonBridgeBench`
puts B/op numbers on it.)

```scala mdoc:silent
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.jsoniter.JsoniterPrism

given JsonValueCodec[String] = JsonCodecMaker.make

// One byte optic per format, same focus type. Drilled once,
// reused for every message. (`JsoniterPrism.fromPath` returns an
// `Either` — a path is data; the literal below is unwrapped here.)
val customerAvro = eoavro.codecPrism[OrderEvent].customer
val customerJson =
  JsoniterPrism
    .fromPath[String]("$.customer")
    .fold(msg => throw new IllegalArgumentException(msg), identity)

// The JSON side's output skeleton. Placeholders must be VALID
// encodings of the branch type — the splice write decodes the
// current focus before replacing it.
val receiptTemplate: Array[Byte] =
  """{"kind":"receipt","customer":"","status":"ok"}""".getBytes("UTF-8")
```

```scala mdoc
// Avro → JSON: slice the branch off the Avro wire form, splice
// it into the JSON template. No OrderEvent, no receipt object.
val receipt = customerAvro.getOption(sampleBytes) match
  case Some(c) => customerJson.replace(c)(receiptTemplate)
  case None    => receiptTemplate
new String(receipt, "UTF-8")
```

```scala mdoc
// JSON → Avro: read the branch from JSON bytes, splice it into
// existing Avro wire bytes. Same guarantee, reversed.
val inbound: Array[Byte] =
  """{"kind":"receipt","customer":"carol","status":"ok"}""".getBytes("UTF-8")
val updatedAvro = customerJson.getOption(inbound) match
  case Some(c) => customerAvro.replace(c)(sampleBytes)
  case None    => sampleBytes

// Witness (this read DOES construct the object — the bridge
// above never did): the branch moved, the siblings survived.
eoavro.codecPrism[OrderEvent].getOption(updatedAvro)
```

Cost model worth knowing: each `.replace` locates the span,
decodes the *current* focus (the `Affine` write carries it),
re-encodes the new one, and allocates one output buffer. Moving
one branch this way is far cheaper than materialising a wide
object — but for many branches per message, or fragment moves
between two *Avro* payloads, prefer `sliceBytes` / `graftBytes`
(no decode at all).

**Source:** `AvroJsonBridgeSpec` (avro module, `eo.avro.jsoniter`) and
`AvroJsonBridgeBench` (benchmarks) in cats-eo.

## Re-usable laws for testing

A decoupled boundary needs a tested contract. Composing optics
means the combinators are already tested — what's left to check is
*your* instances, the optic values your module publishes. The
discipline suites that gate cats-eo's own carriers ship in
`cats-eo-laws`, so a composed optic like `meetingTimes` gets the
full Traversal rule-set in one `checkAll` instead of hand-written
round-trip tests:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-laws" % "@VERSION@" % Test
```

```scala
import cats.Functor
import dev.constructive.eo.laws.TraversalLaws
import dev.constructive.eo.laws.discipline.TraversalTests

// meetingTimes walks a plain Meeting, so T is the constant type
// lambda — its Functor is one line. Arbitrary[Meeting] and
// Cogen[Instant] come from your ScalaCheck generators.
given Functor[[X] =>> Meeting] with
  def map[A, B](fa: Meeting)(f: A => B): Meeting = fa

checkAll(
  "meetingTimes",
  new TraversalTests[[X] =>> Meeting, Instant]:
    val laws = new TraversalLaws[[X] =>> Meeting, Instant]:
      val traversal = meetingTimes
  .traversal,
)
```

Every optic family has a matching `FooLaws` / `FooTests` pair — the
[migration guide](../migration-from-monocle.md#what-stays-the-same) has
the import-swap table.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> ("Re-usable
tests").
