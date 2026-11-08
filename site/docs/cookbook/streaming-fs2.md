# Streaming with fs2

There is no `cats-eo-fs2` module, on purpose: once an optic is a value, most
of the pipes such a module would ship are one-liners over
[fs2](https://fs2.io)'s own combinators. This page teaches those one-liners
instead. The optic says *which part* of each element; fs2 decides
everything about the stream: concurrency, back-pressure, ordering,
resource safety.

```scala mdoc:silent
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.{Pipe, Pure, Stream}

import dev.constructive.eo.*
import dev.constructive.eo.optics.Prism
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens
```

The running example is a stream of sensor readings:

```scala mdoc:silent
enum Reading:
  case Temp(sensor: String, celsius: Double)
  case Humidity(sensor: String, percent: Double)
  case Fault(sensor: String, code: Int)

import Reading.*

val tempP = Prism[Reading, Temp](
  { case t: Temp => Right(t); case r => Left(r) },
  identity,
)
val faultP = Prism[Reading, Fault](
  { case f: Fault => Right(f); case r => Left(r) },
  identity,
)

val readings: Stream[Pure, Reading] = Stream(
  Temp("a", 21.5),
  Humidity("a", 40.0),
  Fault("b", 503),
  Temp("b", 85.0),
  Fault("c", 404),
)
```

## Keep matches with a prism

**Why:** a stream carries several kinds of element and the next stage wants
one kind. A prism already knows how to recognise it, so filtering is
`getOption` handed to fs2. Two shapes, depending on what goes downstream:

```scala mdoc:silent
import cats.syntax.functorFilter.*   // mapFilter, from fs2's FunctorFilter[Stream[F, *]]

// Keep the FOCUS: the stream's element type narrows to A.
def only[F[_], S, A](using CanGetOption[S, A]): Pipe[F, S, A] =
  _.mapFilter(_.getOption[A])

// Keep the WHOLE ELEMENT when its focus passes a test: the element type,
// and every byte of each kept element, stays as it came.
def keepWhere[F[_], S, A](q: A => Boolean)(using CanGetOption[S, A]): Pipe[F, S, S] =
  _.filter(_.getOption[A].exists(q))
```

```scala mdoc
readings.through(only(using tempP)).toList

readings.through(keepWhere[Pure, Reading, Fault](_.code >= 500)(using faultP)).toList
```

The helpers ask for `CanGetOption[S, A]`, not for a `Prism`: anything that
can partially read an `A` out of an `S` fits, an `Optional`, an
`AffineFold`, a composed path, or a byte-level prism that never decodes `S`.
That is exactly the [stream triage demo](grpc-filter.md)'s filter: its
policy is written once against `CanGetOption` and runs on raw message bytes,
forwarding the kept messages byte for byte, the `keepWhere` shape.

## Effectful modify in a stream

**Why:** every element needs a field rewritten by an effect: a lookup, a
call, a clock read. [Effect threading](effect-threading.md)'s `.modifyA`
already turns an `A => G[A]` into an `S => G[S]`; fs2's `evalMap` runs that
for each element, in order:

```scala mdoc:silent
case class Order(id: Int, currency: String, total: Double)

val totalL = lens[Order](_.total)

// Stand-in for a rates service.
def toEur(amount: Double): IO[Double] = IO(amount * 0.9)

val orders = Stream(Order(1, "USD", 100.0), Order(2, "USD", 20.0)).covary[IO]
```

```scala mdoc
orders.evalMap(totalL.modifyA(toEur)).compile.toList.unsafeRunSync()
```

Any optic with `modifyA` fits the slot, a traversal included: the effect then
runs for each focus of each element.

**Do not use `Stream` itself as the effect.** It is tempting to read
"`.modifyA` takes any `Applicative`" and hand it `Stream`, hoping to check
or expand each focus with a little stream. fs2's `Applicative` for `Stream`
comes from its `Monad` (`flatMap`), so combining the foci's streams gives
**every combination** of their outputs, a cartesian product. This is the
`Applicative` a traversal's `.modifyA` would combine its foci with, shown
here on cats' own `traverse`:

```scala mdoc
import cats.syntax.traverse.*

type PureStream[A] = Stream[Pure, A]

// Two choices per focus, three foci: 2 × 2 × 2 = 8 lists, not 2.
List(1, 2, 3).traverse[PureStream, Int](a => Stream(a, -a)).toList
```

With `n` outputs per focus and `k` foci that is `nᵏ` elements. Run the
per-focus stream inside an ordinary effect instead: with `G = IO`,
`a => check(a).compile.lastOrError` gives one result per focus.

## Route by prism

**Why:** different kinds of element go to different places: faults to an
alert channel, everything else to storage. A prism built with `Prism(...)`
tears each element into an `Either` (`faultP.tear`: `Right` for a match,
`Left` for the rest), and fs2 splits on it:

```scala mdoc:silent
def alert(f: Fault): IO[String]    = IO(s"ALERT ${f.sensor} ${f.code}")
def store(r: Reading): IO[String]  = IO(s"stored $r")

val routed: Stream[IO, String] =
  readings.covary[IO]
    .map(faultP.tear)                                  // Stream[IO, Either[Reading, Fault]]
    .broadcastThrough(
      _.collect { case Right(f) => f }.evalMap(alert),
      _.collect { case Left(r) => r }.evalMap(store),
    )
```

```scala mdoc
// Sorted: the order across the two branches is not fixed.
routed.compile.toList.unsafeRunSync().sorted
```

`broadcastThrough` hands every element to each pipe and merges their
outputs, so how the branches interleave, and how far one may run ahead of
the other before back-pressure holds it, is fs2's decision, not the
prism's. Within each branch the order is kept. To keep the stream itself
and only send each side to a sink, call `observeEither(left, right)` on the
`Either` stream instead.

## Run a pipe on one field

**Why:** you already have a `Pipe[F, A, A]` (a rate limiter, a deduplicator,
a stage from another library) and you want to run it on one field of each
element while the rest of the element comes through. Hold each element
back, feed its field to the pipe, and put each output back into the next
held element:

```scala mdoc:silent
import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.syntax.functor.*

// The field is read now and written later, so ask for ONE optic that can do
// both, rather than a CanGet and a CanModify that might be different optics.
def zoomPipe[F[_]: Concurrent, S, A](field: CanGet[S, A] & CanModify[S, A])(
    pipe: Pipe[F, A, A]
): Pipe[F, S, S] =
  in =>
    Stream.eval(Queue.unbounded[F, S]).flatMap { held =>
      in.evalTap(held.offer)
        .map(field.get)
        .through(pipe)
        .evalMap(a => held.take.map(field.replace(a)))
    }

case class Employee(name: String, bonus: Int)

val bonusL = lens[Employee](_.bonus)

val staff = Stream(Employee("Ann", -5), Employee("Bo", 10), Employee("Cy", 7)).covary[IO]
```

```scala mdoc
// One output per input, in order: correct.
staff.through(zoomPipe(bonusL)(_.map(_ * 2))).compile.toList.unsafeRunSync()
```

**This is only correct when the pipe emits exactly one output for each
input, in the same order.** Nothing ties an output to the element its input
came from; the pairing is by position. A pipe that drops an element shifts
every later output onto the wrong element, with no error:

```scala mdoc
// Drops Ann's -5: Bo's 10 lands on Ann, Cy's 7 on Bo, and Cy is gone.
staff.through(zoomPipe(bonusL)(_.filter(_ > 0))).compile.toList.unsafeRunSync()
```

A pipe that emits more than it reads hangs: its second output waits for an
element that has not been read yet, and fs2 reads the next one only when the
pipe asks for it. A pipe that reorders (`parEvalMapUnordered`, a buffer by
key) puts each output on whichever element happens to be next.

So reach for `zoomPipe` only for pipes that are one-to-one by construction.
When the per-field work is really an `A => F[A]`, skip the pipe and modify
in place: `_.evalMap(bonusL.modifyA(f))`, as in
[Effectful modify](#effectful-modify-in-a-stream), keeps each result with its
element by construction. When the pipe filters, filter the whole element
with the field instead (`keepWhere`).

## Further

- **End to end.** The [stream triage demo](grpc-filter.md)
  ([`demos/grpc-filter`](https://github.com/Constructive-Programming/eo/tree/main/demos/grpc-filter))
  is a whole fs2 service built from the first recipe: gRPC frames off a
  socket, each message classified and filtered by eo-jsoniter prisms
  without decoding it, kept frames forwarded byte for byte.
- **Shared state.** cats-effect's `Ref.lens` and fs2's `SignallingRef.lens`
  zoom a reference into one part of its state; they take a getter and a
  setter, which a lens supplies:

```scala mdoc:silent
import cats.effect.Ref
import fs2.concurrent.SignallingRef

case class Stats(seen: Long, lastFault: Option[String])

val lastFaultL = lens[Stats](_.lastFault)

def zoomed(ref: Ref[IO, Stats]): Ref[IO, Option[String]] =
  Ref.lens(ref)(lastFaultL.get, s => a => lastFaultL.replace(a)(s))

def zoomedSignal(ref: SignallingRef[IO, Stats]): SignallingRef[IO, Option[String]] =
  SignallingRef.lens(ref)(lastFaultL.get, s => a => lastFaultL.replace(a)(s))
```

```scala mdoc
(for
  ref <- Ref[IO].of(Stats(0L, None))
  _   <- zoomed(ref).set(Some("b"))
  s   <- ref.get
yield s).unsafeRunSync()
```

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> (the streaming
`modifyF` pattern); fs2's
[guide](https://fs2.io/#/guide) for `evalMap`, `broadcastThrough` and
`observeEither`.
