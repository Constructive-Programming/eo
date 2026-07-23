# ZIO integration

The `cats-eo-zio` module integrates eo's capability layer with
[ZIO 2's dependency-injection model](https://zio.dev/reference/di/).
No new carrier ships in this module — both ZIO's DI substrate and its
wiring functions already have optic shapes:

- `ZEnvironment[R]` is a type-indexed map, so every tagged service
  slot is a **lawful Lens** ([`service`](#the-service-lens)).
- A `ZLayer` wiring function `S => A` is exactly `CanGet[S, A]`, so
  sub-service layers project out of an aggregate service through the
  optic that names the field ([`focusLayer`](#layer-projection)).
- `zio.Ref` is sealed, so runtime state gets **capability-driven
  extension methods** ([`getFocus` / `updateFocus` /
  `setFocus`](#ref-focus-ops)) rather than a wrapped `Ref[A]` view —
  which is the doctrine anyway: consume via capability, construct via
  optic.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-zio" % "@VERSION@"
```

## The service lens

`service[R, A]` focuses the `A` service slot inside a
`ZEnvironment[R]`. It is a lawful lens because `ZEnvironment` is a
type-indexed map: writing a slot replaces exactly that entry and the
sibling services are the untouched leftover.

```scala mdoc:silent
import zio.*
import dev.constructive.eo.*
import dev.constructive.eo.zio.*
import dev.constructive.eo.generics.lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

val env = ZEnvironment(Db("jdbc:h2", 4)).add(Metrics("eo"))

val dbL = service[Db & Metrics, Db]
```

```scala mdoc
dbL.get(env)
```

Because `service` returns the concrete fused lens class, it composes
with ordinary field optics through the same `.andThen` as everywhere
else — drill from the environment into a field of a service:

```scala mdoc
val poolL = lens[Db](_.pool)

val dbPool = dbL.andThen(poolL)

dbPool.modify(_ * 2)(env).get[Db]
```

That composed optic is what you hand to
`provideSomeEnvironment` / `ZIO.updateService`-style test overrides:
tweak one field deep inside one service, leave the rest of the
environment alone.

## Layer projection

ZIO's idiom for deriving one service from another is a `ZLayer` whose
construction function projects the dependency. That projection *is* a
getter, so `focusLayer[S, A]` builds the layer from `CanGet[S, A]`
evidence — the optic is the wiring:

```scala mdoc:silent
val urlL = lens[Db](_.url)

// One aggregate config service, one sub-service layer per field optic:
val urlLayer: ZLayer[Db, Nothing, String] = {
  given CanGet[Db, String] = urlL
  focusLayer[Db, String]
}
```

```scala mdoc
Unsafe.unsafe(implicit u =>
  Runtime.default.unsafe
    .run(ZIO.service[String].provideLayer(ZLayer.succeed(Db("jdbc:h2", 4)) >>> urlLayer))
    .getOrThrowFiberFailure()
)
```

`serviceFocus[S, A]` is the one-shot read of the same shape —
`ZIO.serviceWith[S]` routed through `CanGet` instead of an ad-hoc
projection lambda.

## Ref focus ops

Runtime state held in a `Ref[S]` gets the same capability treatment.
Each op demands the *weakest* capability it needs, and a
read-then-write is ONE `CanModify` in a single atomic `Ref.update`
pass — never split get + set evidence:

```scala mdoc
val program =
  for
    ref <- Ref.make(Db("jdbc:h2", 4))
    _   <- ref.updateFocus[String](_.toUpperCase)(using urlL)
    _   <- ref.setFocus(8)(using poolL)
    out <- ref.get
  yield out

Unsafe.unsafe(implicit u =>
  Runtime.default.unsafe.run(program).getOrThrowFiberFailure()
)
```

`getFocus` (via `CanGet`) and `getFocusOption` (via `CanGetOption`,
for Prism / Optional / AffineFold evidence) complete the read side.

## Automatic capability givens

ZIO's types can also provide eo capabilities *by themselves*. A
`given` import (Scala 3's `*` wildcard deliberately does not pull
givens) puts one optic given per pair in scope:

- `(ZEnvironment[R], A)` for every tagged service `A` of `R` — the
  [`service`](#the-service-lens) lens, so `CanGet` / `CanModify` /
  `CanFold` and the derived capabilities;
- `(Exit[E, A], A)` — a success prism, so `CanGetOption` /
  `CanModify` / `CanReverseGet`; failed exits pass through writes
  untouched.

Generic capability-consuming code then accepts ZIO subjects with no
hand-written given:

```scala mdoc
import dev.constructive.eo.zio.given

def report[T](t: T)(using g: CanGet[T, Db]): String = g.get(t).url
def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)

report(env)

bump(Exit.succeed(41))

bump(Exit.fail("boom"): Exit[String, Int])
```

Coherence rule as everywhere in eo: these are THE optic givens for
their `(S, A)` pairs — don't declare competing ones.

## Effectful modify

There is deliberately **no** `modifyZIO` in this module.
`CanModifyF.modifyF` is polymorphic in its functor, and
[zio-interop-cats](https://github.com/zio/interop-cats) owns the
`cats.Functor[ZIO[R, E, *]]` instance — so the integration is one
import on your side of the classpath, with nothing for this module to
duplicate:

```scala
// libraryDependencies += "dev.zio" %% "zio-interop-cats" % "23.1.0.5"
import zio.interop.catz.*

def refresh(url: String): Task[String] = ???

val refreshed: Db => Task[Db] = urlL.modifyF(refresh)
```

## Overhead

`ZioDiBench` pairs every op above (leaf service get/replace, drilled
get/modify) against the hand-written `env.get` / `env.update`
equivalent. Both sides pay `ZEnvironment`'s own map machinery — and
the optic side measures *cheaper*: reads are allocation-free (0 B/op
vs ~120 B/op hand-written) and writes allocate less, because the
lens captures its `zio.Tag` once at construction while every
hand-written `env.get[A]` / `env.update[A]` call site re-materializes
it. See the [benchmarks page](../benchmarks.md) for the current
sweep.
