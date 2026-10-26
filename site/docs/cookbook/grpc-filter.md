# Allocation-free stream triage demo

> **A demo, not a module.** The code lives in
> [`demos/grpc-filter`](https://github.com/Constructive-Programming/eo/tree/main/demos/grpc-filter)
> and is not published. It shows what [eo-jsoniter](../integrations/jsoniter.md)'s
> byte-level optics buy a streaming service: a filter on a gRPC stream that
> classifies each message and forwards only the ones a policy keeps, without
> ever decoding a message, written with [fs2](https://fs2.io).

## The idea: read three values, not 64 fields

An upstream service streams issues to a downstream one. They travel as gRPC
messages with JSON payloads, raw bytes off a TCP socket. Each issue has 64
fields. One of them, `detail`, says what kind of issue it is: a bug, an
incident or a feature request, each with its own fields, nested up to three
objects deep. A filter between the two services keeps:

- incidents of severity 1 or 2 on a critical service,
- bugs whose top stack frame is in a watched module,
- requests with enough votes from customers on the plans that matter,

and drops everything else.

To decide, the filter needs the kind and one or two values of the
kind's own. The usual way decodes the whole issue to read them: 64 fields,
lists, nested objects, about 8.5 KB of objects per 4 KB message. This filter
reads them where they are, in the bytes, with eo-jsoniter's prisms. It never
builds an `Issue`, and it forwards the messages it keeps exactly as they came.

```
upstream service ──TCP──▶ filter ──TCP──▶ downstream service
                           │
   bytes ─ GrpcFrames.splitFrames ─▶ message ─ Wire.detail ⇀ DetailJson ─ Policy.keep ─▶ keep?
                                                                                           │
   bytes ◀─ GrpcFrames.joinFrames ◀─ the kept messages, byte for byte ◀────────────────────┘
```

## The stream

gRPC frames every message the same way, whatever its encoding: one byte saying
whether the message is compressed, its length as a 4-byte big-endian unsigned
integer, then the message (the *Length-Prefixed-Message* of the
[gRPC over HTTP/2 spec](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md)).
gRPC is not tied to protobuf: a service can carry JSON (`application/grpc+json`),
and this one does.

`GrpcFrames` holds the framing as two fs2 pipes over raw bytes. `splitFrames` cuts
messages out of the bytes in whatever pieces the socket reads them, and fails
the stream on a compressed frame (no message encoding is negotiated here), on a
message over gRPC's default 4 MiB limit, or on bytes that end inside a frame.
Each message it hands out is a view of the socket read it arrived in (an fs2
`Chunk.ArraySlice`), not a copy; only a message that straddles two reads is
copied into an array of its own. `joinFrames` frames messages again, without
copying them either.

```scala
socketBytes                           // Stream[F, Byte], as the socket reads it
  .through(GrpcFrames.splitFrames())  // Stream[F, Chunk.ArraySlice[Byte]]: one view per message
  .through(GrpcFrames.joinFrames)     // Stream[F, Byte]: the same frames again
```

The demo puts these frames straight on TCP rather than inside HTTP/2 streams:
the subject is what happens to the message bytes, and over HTTP/2 the same
frames arrive as the body of a call. So the demo does not speak HTTP/2, and has
no headers, trailers or status codes.

## The message

```scala
final case class Issue(
    id: Long,
    key: String,
    title: String,
    description: String,
    reporter: Person,          // Person → Team → Org: three objects deep
    // … 55 more …
    detail: Detail,            // the 61st field
    tags: Map[String, String],
    checklist: List[CheckItem],
    revision: Long,
)

enum Detail:
  case Bug(component: String, build: String, reproducible: Boolean, trace: Trace)
  case Incident(severity: Severity, startedAt: Long, mitigated: Boolean, impact: Impact)
  case Request(product: String, votes: Votes, quarter: String, requester: Customer)

final case class Impact(users: Long, errorRate: Double, service: Service)
final case class Service(name: String, tier: Tier, region: String)   // detail → impact → service
```

jsoniter-scala writes a `Detail` as an object whose first field, `"type"`, names
its case: `{"type":"Incident","severity":"Sev1",…}`.

The issues come from ScalaCheck generators that
[scalacheck-derived](https://github.com/martinhh/scalacheck-derived) derives
from the model, one per case class and enum, from its fields and cases:

```scala
object Issues:
  given Arbitrary[String] = /* words; one in twenty is something JSON must escape, or non-ASCII */
  given Arbitrary[Double] = Arbitrary(Gen.choose(0.0, 1e6))      // JSON has no NaN or infinities
  given Arbitrary[Module] = Arbitrary(Gen.oneOf(modules))
  given Arbitrary[Votes] = Arbitrary(Gen.choose(0, 200).map(Votes(_)))
  given Arbitrary[Detail] = deriveArbitrary
  given Arbitrary[Issue] = deriveArbitrary
```

Only the leaves are written by hand, so a field added to the model is generated
without touching the generators. A third of the issues are of each kind. The
stream draws them at size 7: up to seven words in a text and seven elements in
a list, which makes a message 2.6 to 5.9 KB, 4.1 KB on average. The property
tests draw them at every size from 2 to 40.

## Reading without decoding

`Wire` holds the optics over a message's bytes. Each scans the bytes for its
path and decodes only what it focuses:

```scala
object Wire:
  opaque type DetailJson = JsonSlice       // a detail's own JSON, where it lies in the message

  // A message where it lies in the socket's read: a view, nothing copied.
  def json(message: Chunk.ArraySlice[Byte]): JsonSlice = JsonSlice(message.toByteBuffer)

  // Message ⇀ its detail's bytes: one scan, in place, past the 60 fields in front; nothing decoded.
  val detail: Focus[JsonSlice, DetailJson] = JsoniterPrism.inPlace[Issue].field(_.detail).raw

  given kind: Focus[DetailJson, Kind] = /* JsoniterPrism.fromPath[Kind]("$.type"), .inPlace */
  given severity: Focus[DetailJson, Severity] =
    JsoniterPrism.inPlace[Detail.Incident].field(_.severity)
  given tier: Focus[DetailJson, Tier] =
    JsoniterPrism.inPlace[Detail.Incident].field(_.impact).field(_.service).field(_.tier)
  given module: Focus[DetailJson, Module] =
    JsoniterPrism.inPlace[Detail.Bug].field(_.trace).field(_.frames).at(0).field(_.module)
  given plan: Focus[DetailJson, Plan] =
    JsoniterPrism.inPlace[Detail.Request].field(_.requester).field(_.contract).field(_.plan)
  given votes: Focus[DetailJson, Votes] = JsoniterPrism.inPlace[Detail.Request].field(_.votes)
```

Reading takes two steps. `detail` scans the message once and hands out the
detail's own bytes, a few hundred of them (`.raw` focuses the encoded value
rather than a decoded one). Every optic here reads in place:
`JsoniterPrism.inPlace` points a prism at a `JsonSlice`, a range of an array,
so the message is never copied out of the socket's read to be judged
(`JsonSlice(chunk.toByteBuffer)` wraps the read's own array: fs2 hands an
array slice's array to the buffer as it is). On an in-place prism `.raw`
focuses a `JsonSlice` too, a view of the detail's bytes where they lie in the
message, so the detail isn't copied out either. The other optics read the
policy's leaves from that view: a kind, an enum, a number. `DetailJson` is an
opaque type, so the detail's optics can't be pointed at a whole message by
mistake.

Nothing copied means the views share the read's array: the filter judges a
message and forwards it before the next read, and keeps nothing (`bytes`
copies a detail out, for the tests). The two steps also compose with plain
`andThen`, into one optic from a message to a leaf, still copying nothing:

```scala
val severityOf: Focus[JsonSlice, Severity] = Wire.detail.andThen(Wire.severity)
```

The paths are typed: `.field(_.impact)` is checked against the model when
`Wire` compiles, so renaming a field breaks the build, not the filter. Each
case's paths start from that case's own root, `JsoniterPrism.inPlace[Detail.Incident]`,
because the detail's JSON is that case's JSON plus its `"type"`. Only the
kind's path is a string: `"type"` is the codec's tag, not a field of the model.

Leaf optics of different cases don't check the kind: `tier` on a bug's detail
misses only because a bug has no `impact`. The policy classifies before it
reads a leaf, and the tests check that the bytes and the decoded issue are
always judged alike.

## One policy, two readers

The policy is written once, against capabilities, and doesn't know what it
reads:

```scala
final case class Policy(urgent: Set[Severity], tiers: Set[Tier], modules: Set[Module],
                        plans: Set[Plan], minVotes: Votes):

  def keep[D](detail: D)(using
      kind: CanGetOption[D, Kind],
      severity: CanGetOption[D, Severity],
      tier: CanGetOption[D, Tier],
      module: CanGetOption[D, Module],
      plan: CanGetOption[D, Plan],
      votes: CanGetOption[D, Votes],
  ): Boolean =
    kind.getOption(detail) match
      case Some(Kind.Incident) =>
        severity.getOption(detail).exists(urgent) && tier.getOption(detail).exists(tiers)
      case Some(Kind.Bug)     => module.getOption(detail).exists(modules)
      case Some(Kind.Request) =>
        plan.getOption(detail).exists(plans) && votes.getOption(detail).exists(_.count >= minVotes.count)
      case None => false
```

With `Wire`'s givens in scope, `D` is `DetailJson` and the policy reads bytes.
With `Model`'s, `D` is a decoded `Detail`, read through eo-generics' derived
prisms and lenses:

```scala
object Model:
  given tier: Focus[Detail, Tier] =
    prism[Detail, Detail.Incident]
      .andThen(lens[Detail.Incident](_.impact))
      .andThen(lens[Impact](_.service))
      .andThen(lens[Service](_.tier))
  // … one per leaf, and the kind
```

The baseline uses `Model`, after decoding the whole issue. So do the tests,
which check that both readers judge every message alike. That is what
*consume via capability, construct via optic* buys: the code that decides
never names the optic or the format it reads.

```scala
def optics(policy: Policy)(message: JsonSlice): Verdict =
  import Wire.given
  Wire.detail.getOption(message) match
    case Some(detail) => Verdict(Wire.kind.getOption(detail), policy.keep(detail))
    case None         => Verdict(None, keep = false)

def decoding(policy: Policy)(message: JsonSlice): Verdict =
  import Model.given
  Issue.decode(message).map(_.detail) match
    case Some(detail) => Verdict(Some(Kind.of(detail)), policy.keep(detail))
    case None         => Verdict(None, keep = false)
```

Both read the message in place: `Issue.decode` decodes it with jsoniter-scala's
`readFromSubArray`, so the baseline doesn't copy it out either.

A message of a kind the demo doesn't know, from a newer producer say, has no
kind: the policy drops it, and the tally counts it as unclassified. The optics
read it like any other message. A decoder of the full model rejects the whole
message, and the baseline drops it for that reason.

## The filter

```scala
def filter[F[_]: Async](verdict: JsonSlice => Verdict, tally: Ref[F, Tally]): Pipe[F, Byte, Byte] =
  _.through(GrpcFrames.splitFrames())
    .chunks
    .evalMap { messages =>
      Async[F].delay(messages.map(message => (message, verdict(Wire.json(message))))).timed.flatMap {
        (took, judged) =>
          tally.update(_.add(judged, took)).as(judged.collect { case (m, Verdict(_, true)) => m })
      }
    }
    .unchunks
    .through(GrpcFrames.joinFrames)
```

`Net` puts it between two sockets. The upstream service streams frames to
whoever connects. The filter reads up to 256 KiB per read (fs2's `reads` asks
for 8 KiB, and on loopback the reads were the bottleneck), triages, and writes
what it keeps to the downstream service. Reading, judging and writing run on
separate fibers, with a few chunks buffered between them (`prefetchN`). The
downstream service decodes everything it receives and checks it against the
policy.

`sbt "demoGrpcFilter/run 200000"` runs the three of them on the loopback
interface, once with each filter (after an untimed run of each to warm the JIT
compiler up):

```
eo-jsoniter optics: 200000 messages, 813.3 MB in 2.21 s:
  91 k messages/s, 368 MB/s
  judging them took 7.1 µs per message; the rest is sockets and framing
  kind            seen      kept
  Bug            66199      8205
  Incident       63289     10745
  Request        70512     12500
  none               0         0
  downstream received 31450 messages (31450 kept); 31450 decode as issues, and the policy keeps 31450 of them
full decode (baseline): 200000 messages, 813.3 MB in 4.16 s:
  48 k messages/s, 196 MB/s
  judging them took 16.8 µs per message; the rest is sockets and framing
  …
```

## Correctness

`sbt demoGrpcFilter/test`:

| suite | checks |
|---|---|
| `GrpcFramesSpec` | any messages, joined into frames and cut into arbitrary pieces, split back whole; the frame layout; the size limit (unsigned lengths included); compressed frames and truncated bytes fail the stream; a message inside one chunk is a view of it, nothing copied, also after one that straddles chunks |
| `TriageSpec` | the optics judge every message exactly as decoding does: 3 000 issues the size the stream's are, under the default policy and under 100 random ones, and 500 random issues of every size under random policies, every kind both kept and dropped; a message read in place, between its neighbours' bytes, is judged as the message alone; the slice `Wire.detail` hands out is the detail's own JSON, and a view of the message's array, not a copy; `Wire.detail.andThen(Wire.severity)` reads and writes a message's severity; pretty-printed JSON is read the same; an unknown kind, a missing `detail` and bytes that aren't JSON are dropped as unclassified |
| `TriageSpec` (laws) | `Model`'s optics, and the wire's `Wire.detail.andThen(Wire.severity)` on messages read in place, pass cats-eo-laws' `OptionalTests` |
| `PipelineSpec` | the whole filter, in memory with the bytes cut at random, and over real sockets: it forwards exactly the messages the decoded issues say to keep, byte for byte and in order, and its tally adds up; a malformed frame fails it |

## Benchmarks

JMH, in
[`demos/grpc-filter-bench`](https://github.com/Constructive-Programming/eo/tree/main/demos/grpc-filter-bench).
Average time per message in **ns (lower is better)**, error = 99.9 %
confidence half-width; allocation per message from `-prof gc`
(`gc.alloc.rate.norm`). Measured on a 4-vCPU Intel Xeon (2.1 GHz) container,
OpenJDK 21.0.12, fs2 3.14.0, jsoniter-scala 2.41.2; three forks, 5
measurements after 3 warm-ups. A shared container is not a quiet benchmark
machine: read the ratios, not the absolute values.

### One message

`TriageBench` judges a pool of 256 messages, small enough to stay in cache, as
a message just read from a socket is. `decode` decodes the whole issue and runs
the policy on its `Detail`; `optics` slices out the detail and runs the policy
on its bytes; `detail` is the slicing alone.

`layout` is where `detail` sits in the message: `late` is where the codec
writes it (the 61st of 64 fields, about 90 % of the way in), `first` is the
same message with `detail` moved to the front.

| layout | `decode` | `optics` | `detail` | speed-up | allocation |
|---|--:|--:|--:|--:|--:|
| late | 13 833 ± 620 ns, 8 557 B | 5 929 ± 450 ns, 705 B | 5 371 ± 219 ns, 367 B | 2.3× | 12× less |
| first | 13 403 ± 707 ns, 8 548 B | 1 207 ± 78 ns, 664 B | 711 ± 63 ns, 431 B | 11× | 13× less |

Decoding reads every byte, wherever `detail` is. The scan reads the bytes in
front of `detail`, so its cost depends on where the producer puts the field:
in the codec's own layout, nine tenths of the optics' time is the scan past
the 60 fields in front of `detail`. It skips them at about the speed of
jsoniter-scala's own `skip`. What it saves either way is the building: 700
bytes allocated per message instead of 8.6 KB.

Since `.raw` became a view of the message (`JsoniterPrism.inPlace`), the
detail isn't copied out of it either, and the optics allocate less still.
Allocation per message in short runs of the code before and after that
change, in one session (see the notes under the next table):

| layout | `decode` | `optics` before → after | `detail` before → after |
|---|--:|--:|--:|
| late | 8 363 B | 600 → 417 B | 351 → 40 B |
| first | 8 363 B | 664 → 353 B | 375 → 104 B |

Slicing out the detail now allocates next to nothing; what `optics` still
allocates is reading the leaves.

### The whole filter

`StreamBench` runs the filter's pipe in memory: 1 024 messages' frames,
arriving in 256 KiB pieces, are cut into messages, judged, tallied and the kept
ones framed again.

| judge | per message | allocation | read in place¹ | detail in place too² |
|---|--:|--:|--:|--:|
| `decode` | 17 695 ± 845 ns | 13 917 B | 9 228 B | 9 254 B |
| `optics` | 8 911 ± 449 ns | 6 001 B | 1 505 B | 1 375 B |

2.0× faster, with 2.3× less allocation, when each message was copied out of
the socket's chunks into an array of its own (4 KB): the pipe added 3 to 4 µs
and 5.3 KB per message either way, most of it that copy. Now `splitFrames`
hands out views of the chunks and both judges read a message where it lies
(`.inPlace` for the optics, `readFromSubArray` for the decoder), and the
third column shows the copy gone: 4.5 to 4.7 KB less per message. The last
column is with the detail read where it lies in the message too (`.raw` on an
in-place prism is a view): 130 B less again for the optics, so they now
allocate 6.7× less than decoding. (The view of each message is now built
through `JsonSlice(chunk.toByteBuffer)`; the buffer it wraps costs nothing
measurable, 1 375 B against 1 370 B building the slice from the chunk's
array, offset and length by hand.)

¹ ² Short runs (`-i 3 -wi 2 -f 1 -prof gc`: one fork, three measurements, on
a busy shared container, JDK 25), of the code before (¹) and after (²) `.raw`
became a view, in one session. Allocation per message is deterministic and
was steady to ±30 B; the runs' times were noise (errors as large as the
scores) and are not quoted. The first two columns are the full run described
above, from before either change.

## Limits

- **No HTTP/2.** The frames travel on plain TCP: no headers, trailers or
  status codes, and no compression (`grpc-encoding` is never negotiated, so a
  compressed frame fails the stream).
- **The scan's cost follows the layout.** A field late in a large message
  costs most of a read through the message. An optic only saves the reading
  past the field and the building of what it doesn't focus.
- **Copies.** A message is read where it lies in the socket's read, except one
  that straddles two reads (about one in 64 at 256 KiB a read), which is copied
  into an array of its own. The detail is read where it lies in the message.
- **Leaf optics trust the classification.** They don't check the kind
  themselves; the policy classifies first.

## Running it

```sh
# the suite
sbt demoGrpcFilter/test
# the three services on loopback, with each filter
sbt "demoGrpcFilter/run 200000"
# the benchmarks
sbt "demoGrpcFilterBench/Jmh/run -prof gc"
```

The demo and its benchmarks sit outside the root aggregate; CI runs the suite
and compiles the benchmarks as their own step.
