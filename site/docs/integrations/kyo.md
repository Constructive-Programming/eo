# Kyo integration

The `cats-eo-kyo` module integrates eo with
[Kyo's dependency management](https://getkyo.io/latest/kyo-prelude/)
at the same three seams as [the ZIO module](zio.md) — the two systems'
DI substrates are both type-indexed maps, and the integration surface
mirrors it deliberately:

- `TypeMap[R]` (what `Env[R]` carries, what `Env.runAll` accepts) is a
  type-indexed map, so every tagged slot is a **lawful Lens**
  ([`service`](#the-service-lens)).
- `Layer.from` takes a wiring function `S => A < *` — exactly
  `CanGet[S, A]` — so aggregate services project to sub-service layers
  ([`Layer.focus`](#env-and-layer-focus)).
- `Var[S]` state gets the same capability-driven focus ops as
  `zio.Ref` ([`Var.getFocus` / `updateFocus` /
  `setFocus`](#var-focus-ops)).

Kyo's surface is companion-static (`Env.get`, `Var.update`), so the
focus ops here extend the companion objects and read like native Kyo.

The module depends on **kyo-prelude only** — `Env`, `Var`, `Layer` and
`TypeMap` all live in Kyo's dependency-light pure layer (kyo-data and
kyo-kernel come transitively). No kyo-core IO runtime is pulled in, so
the module is usable from pure kyo-prelude programs and full kyo-core
applications alike.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-kyo" % "@VERSION@"
```

## The service lens

`service[R, A]` focuses the `A` slot inside a `TypeMap[R]` — the
direct analogue of the ZIO module's `ZEnvironment` service lens, and
lawful for the same reason: `add` on a present tag replaces exactly
that entry, sibling slots are the untouched leftover.

```scala mdoc:silent
import kyo.*
import dev.constructive.eo.*
import dev.constructive.eo.kyo.*
import dev.constructive.eo.generics.lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

val tm = TypeMap(Db("jdbc:h2", 4), Metrics("eo"))

val dbL = service[Db & Metrics, Db]
```

```scala mdoc
dbL.get(tm)
```

It composes with field optics through the ordinary `.andThen`, which
is what you want for `Env.runAll` test overrides — tweak one field of
one service in an environment map, leave the rest alone:

```scala mdoc
val poolL = lens[Db](_.pool)

dbL.andThen(poolL).modify(_ * 2)(tm).get[Db]
```

## Env and Layer focus

`Env.focus[R, A]` reads a focus out of the `R` service in the
environment — `Env.use` routed through `CanGet` instead of an ad-hoc
projection lambda:

```scala mdoc:silent
val urlL = lens[Db](_.url)

given CanGet[Db, String] = urlL
```

```scala mdoc
Env.run(Db("jdbc:h2", 4))(Env.focus[Db, String]).eval
```

`Layer.focus[S, A]` derives an `A` service layer from an `S` service
by focusing — `Layer.from` is `CanGet`-shaped, so the optic is the
wiring. It slots straight into `Env.runLayer` graphs:

```scala mdoc
val prog = Env.runLayer(Layer(Db("jdbc:h2", 4)), Layer.focus[Db, String])(Env.get[String])

Memo.run(prog).eval
```

## Var focus ops

`Var[S]` is Kyo's stateful effect; the focus ops mirror the ZIO
module's `Ref` extensions with the same names and the same capability
demands. A read-then-write is ONE `CanModify` in a single
`Var.updateDiscard` pass:

```scala mdoc
val update =
  for
    _   <- Var.updateFocus[Db, String](_.toUpperCase)(using urlL)
    _   <- Var.setFocus[Db, Int](8)(using poolL)
    url <- Var.getFocus[Db, String]
  yield url

Var.runTuple(Db("jdbc:h2", 4))(update).eval
```

`getFocusOption` (via `CanGetOption`) covers partial foci — Prism,
Optional and AffineFold evidence.

## Explicit nulls

Kyo's inline kernel is not `-Yexplicit-nulls`-clean: its machinery
expands inside *your* compilation units wherever a kyo inline def is
used (`.eval`, `Env.run`, …) and passes a `null` `Safepoint`
interceptor, which fails to typecheck under that flag. This is a
property of Kyo, not of this module — but it means projects enabling
`-Yexplicit-nulls` cannot currently compile kyo call sites, with or
without eo. (`cats-eo-kyo` itself opts out of the flag that the rest
of the eo build carries.)

## Overhead

`KyoDiBench` pairs the ops above against their hand-written
equivalents in two tiers: pure `TypeMap` ops (`tm.get` / `tm.add`),
and full effectful round-trips (`Env.focus` vs `Env.use`,
`Var.updateFocus` vs `Var.updateDiscard`) where both sides pay Kyo's
suspend/handle/eval machinery and the pair isolates the capability
hop. Measured: allocation parity on the map tier (reads 0 B/op both
sides, drilled writes identical), and +8–16 B/op on the effect tier —
the per-call capability closure. See the
[benchmarks page](../benchmarks.md) for the current sweep.
