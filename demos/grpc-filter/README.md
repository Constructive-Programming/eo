# gRPC stream triage, with eo-jsoniter and fs2

A filter on a gRPC stream of JSON messages, read as raw bytes off a TCP socket.
It classifies each message and forwards only the ones a policy keeps, without
ever decoding a message: it reads the three or so values the policy needs
straight from the bytes, with eo-jsoniter's prisms, and forwards what it keeps
byte for byte.

```
upstream service ──TCP──▶ filter ──TCP──▶ downstream service
                           │
   bytes ─ GrpcFrames.splitFrames ─▶ message ─ Wire.detail ⇀ DetailJson ─ Policy.keep ─▶ keep?
                                                                                           │
   bytes ◀─ GrpcFrames.joinFrames ◀─ the kept messages, byte for byte ◀────────────────────┘
```

Each message is an `Issue` with 64 fields. The 61st, `detail`, is one of three
kinds, each with its own fields, nested up to three objects deep:

```scala
enum Detail:
  case Bug(component: String, build: String, reproducible: Boolean, trace: Trace)
  case Incident(severity: Severity, startedAt: Long, mitigated: Boolean, impact: Impact)
  case Request(product: String, votes: Votes, quarter: String, requester: Customer)
```

The policy keeps severe incidents on critical services, bugs whose top stack
frame is in a watched module, and popular requests from customers on the plans
that matter. It is written once, against capabilities:

```scala
def keep[D](detail: D)(using
    kind: CanGetOption[D, Kind],
    severity: CanGetOption[D, Severity],
    tier: CanGetOption[D, Tier],
    // … module, plan, votes
): Boolean
```

With `Wire`'s givens it reads a detail's bytes where they lie: the message is a
`JsonSlice` of the socket's read, and the detail a `JsonSlice` of the message,
neither copied:

```scala
val detail: Focus[JsonSlice, Wire.DetailJson] = JsoniterPrism.inPlace[Issue].field(_.detail).raw
given tier: Focus[DetailJson, Tier] =
  JsoniterPrism.inPlace[Detail.Incident].field(_.impact).field(_.service).field(_.tier)
```

With `Model`'s, a decoded `Detail`, through eo-generics' prisms and lenses:

```scala
given tier: Focus[Detail, Tier] =
  prism[Detail, Detail.Incident]
    .andThen(lens[Detail.Incident](_.impact))
    .andThen(lens[Impact](_.service))
    .andThen(lens[Service](_.tier))
```

The baseline decodes every message and uses `Model`. The tests check that
both judge every message alike.

| file | what it is |
|---|---|
| `Issue.scala` | the message model and its jsoniter-scala codecs |
| `Issues.scala` | ScalaCheck generators for issues, derived from the model by scalacheck-derived |
| `Wire.scala` | eo-jsoniter optics over a message's bytes |
| `Model.scala` | the same foci on a decoded `Detail` (eo-generics) |
| `Policy.scala` | which issues to keep, written against `CanGetOption` |
| `GrpcFrames.scala` | gRPC's Length-Prefixed-Message framing as fs2 pipes: `splitFrames`, `joinFrames` |
| `Triage.scala` | a message's verdict, both ways; the filter pipe; its tally |
| `Net.scala` | the upstream service, the filter and the downstream service on TCP sockets |
| `Demo.scala` | runs the three on loopback, with each filter |

## Numbers

JMH (`demos/grpc-filter-bench`) on a 4-vCPU 2.1 GHz Xeon container, JDK 21:

| per message | full decode | eo-jsoniter optics |
|---|--:|--:|
| judging one message | 13.8 µs, 8 557 B | 5.9 µs, 705 B |
| … with `detail` first in the message | 13.4 µs, 8 548 B | 1.2 µs, 664 B |
| the whole filter pipe, in memory | 17.7 µs, 13 917 B | 8.9 µs, 6 001 B |

Over loopback sockets, `sbt "demoGrpcFilter/run 200000"` filters 91 k
messages/s with the optics and 48 k with full decoding.

The scan through a message costs most of the optics' time. It reads the bytes
in front of the focused field, so it depends on where the producer puts that
field.

## Limits

The frames travel on plain TCP, not inside HTTP/2 streams. So there are no
headers, trailers or status codes, and no compression: a compressed frame
fails the stream. See [the docs page](../../site/docs/cookbook/grpc-filter.md) for
the details.

## Running it

```sh
sbt demoGrpcFilter/test                       # the suite
sbt "demoGrpcFilter/run 200000"               # the three services on loopback
sbt "demoGrpcFilterBench/Jmh/run -prof gc"    # the benchmarks
```
