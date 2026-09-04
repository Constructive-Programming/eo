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

## Which direction are you integrating?

The module is a two-way adapter; pick the row that matches what you
are holding and what the other side expects:

| You're holding | The other side expects | Reach for |
|---|---|---|
| an optic (`lens[Config](_.maxOrders)`) | an `Env` read ([Dependencies](https://getkyo.io/latest/kyo-prelude/#dependencies)) | [`Env.focus`](#kyo-docs-examples-through-optics) instead of an `Env.use` projection lambda |
| an optic | a `Layer` in the wiring graph ([Wiring with layers](https://getkyo.io/latest/kyo-prelude/#wiring-with-layers)) | [`Layer.focus`](#kyo-docs-examples-through-optics) — the optic becomes the layer's wiring function |
| an optic | a `TypeMap` for `Env.runAll` overrides | [`service` + `.andThen`](#the-service-lens) |
| a `TypeMap`, `Maybe`, `Result`, or `Var` state | eo capability evidence (`CanGet[T, A]`, `CanModify[T, A]`, …) | [`import dev.constructive.eo.kyo.given`](#automatic-capability-givens) — no hand-written given |
| a NamedTuple or case class | a kyo `Record` (or back) | [`Record.iso[T]`](#records-record-iso-and-record-lens) — a staged bijection |
| a `Record[F]` | one field, optic-shaped | [`Record.lens[F]("name")`](#records-record-iso-and-record-lens) |
| a kyo-schema `Schema[A]` / `Focus` | eo optics over values or encoded payloads | [the `eo.kyo.schema` bridge](#the-kyo-schema-bridge-eo-kyo-schema) (optional dependency) |
| an untyped `Structure.Value` tree | navigation, rewrites, or a typed leaf — without decoding the spine | [`StructureValues` + `Schema.valuePrism`](#the-untyped-tree-structure-value) |

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

## Kyo docs examples through optics

The [Dependencies section of the kyo-prelude docs](https://getkyo.io/latest/kyo-prelude/#dependencies)
reads a limit out of a config service with a projection lambda:

```scala
// from the kyo docs:
case class Config(maxOrders: Int, currency: String)

val limit: Int < Env[Config] =
    Env.use[Config](_.maxOrders)
```

With an optic naming the field, the same value is `Env.focus` — and
the optic is reusable everywhere else the field is touched (writes,
`Var` updates, layer wiring), not just in this one lambda:

```scala mdoc:silent
case class Config(maxOrders: Int, currency: String)

val maxOrdersL = lens[Config](_.maxOrders)
val currencyL = lens[Config](_.currency)

given CanGet[Config, Int] = maxOrdersL

val limit: Int < Env[Config] = Env.focus[Config, Int]
```

```scala mdoc
Env.run(Config(5, "EUR"))(limit).eval
```

The [Wiring with layers section](https://getkyo.io/latest/kyo-prelude/#wiring-with-layers)
builds a `configLayer` and derives further layers from it with
`Layer.from`. When the derived service is a *focus* of the aggregate,
`Layer.focus` replaces the hand-written projection — same graph, same
`Env.runLayer` resolution:

```scala mdoc:silent
val configLayer: Layer[Config, Any] =
  Layer(Config(maxOrders = 10, currency = "USD"))

val currencyLayer: Layer[String, Env[Config]] = {
  given CanGet[Config, String] = currencyL
  Layer.focus[Config, String]
}
```

```scala mdoc
Memo.run(Env.runLayer(configLayer, currencyLayer)(Env.get[String])).eval
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

## Records: `Record.iso` and `Record.lens`

Kyo's [`Record`](https://getkyo.io/latest/kyo-data/) is a string-keyed
typed record — `Record["name" ~ String & "age" ~ Int]` — and a
NamedTuple or case class of the same shape is the same data.
`Record.iso[T]` names that bijection, and it is a **staged macro**: the
expansion is exactly the code you would write by hand for the concrete
shape (`("name" ~ t._1) & ("age" ~ t._2)` one way, `getField` reads
under kyo's own `Fields.Have` evidence the other), so there is no
runtime `Fields` machinery, no per-call iteration, and no arity
ceiling — beyond 22 fields the named-tuple expansion rides the same
`scala.runtime.Tuples` calls the stdlib's `Tuple#apply` compiles to.

```scala mdoc:silent
type PersonR = "name" ~ String & "age" ~ Int

val personI = Record.iso[(name: String, age: Int)]
```

```scala mdoc
personI.get((name = "Alice", age = 30)).name

personI.reverseGet("name" ~ "Ada" & "age" ~ 36)
```

Case classes take the same call — the rebuild goes through the primary
constructor (`new`, not `copy`), so enum cases work too, and generic
case classes are fine at concrete instantiations. (`Customer` is a
plain `case class Customer(name: String, age: Int)` hosted at package
level — the macro's `new T(...)` needs top-level targets, the same
rule as [the generics macros](../generics.md).)

```scala mdoc:silent
import dev.constructive.eo.docs.Customer

val customerI = Record.iso[Customer]
```

```scala mdoc
customerI.get(Customer("Grace", 45)).age
```

`Record.lens[F]("name")` focuses one field, GenLens-style — the focus
type is inferred from the same `Fields.Have` evidence that types
`record.name` itself, and the replace is kyo's right-biased `&` merge.
It composes with the iso like any other eo optic, and serves
capability-consuming code:

```scala mdoc:silent
val ageL = Record.lens[PersonR]("age")
```

```scala mdoc
val ageInCustomer = customerI.andThen(ageL)

ageInCustomer.modify(_ + 1)(Customer("Grace", 45))
```

`reverseGet ∘ get` is the identity; `get ∘ reverseGet` is kyo's
`compact` — extra entries a widened record carries are dropped, not
preserved. Field lenses are construction-only (no automatic givens):
the field *name* is not part of the `(S, A)` capability key, so two
same-typed fields would collide.

## The kyo-schema bridge: `eo.kyo.schema`

[kyo-schema](https://getkyo.io/latest/kyo-schema/) derives a `Schema[A]`
carrying structure, validation, wire configuration, and codecs. The
`dev.constructive.eo.kyo.schema` sub-package turns that machinery into
eo optics. It is an **optional dependency** — `cats-eo-kyo` does not
pull kyo-schema transitively; add it (and a codec artifact, here json)
yourself:

```scala
libraryDependencies += "io.getkyo" %% "kyo-schema" % "1.0.0-RC6"
libraryDependencies += "io.getkyo" %% "kyo-schema-json" % "1.0.0-RC6"
```

### Focus bridge

kyo-schema's `Focus[Root, Value, Mode]` is its own schema-checked
optic, behind a mode lattice — and each mode maps onto the matching eo
carrier: `Focus.Id` (product paths) → **Lens**, `Maybe` (sum-variant
paths) → **Optional**, `Chunk` (collection paths) → **Traversal**
(kyo's `Chunk` IS a `Seq`, so the mode is a lens onto the collection
slot composed with `Traversal.each`). Bridged optics compose with
everything else on this page. (`KyoItem` / `KyoCart` / `KyoShape` are
package-level ADTs with `derives Schema`.)

```scala mdoc:silent
import dev.constructive.eo.kyo.schema.*
import dev.constructive.eo.docs.{KyoCart, KyoItem, KyoShape}

val priceL = Schema[KyoItem].focus(_.price).lens
val radiusO = Schema[KyoShape].focus(_.Circle.radius).toOptional
val itemsT = Schema[KyoCart].foreach(_.items).traversal

val cart = KyoCart("c-1", Vector(KyoItem("apple", 1.0), KyoItem("pear", 2.0)))
```

```scala mdoc
priceL.modify(_ * 2)(KyoItem("apple", 1.0))

radiusO.getOption(KyoShape.Circle(2.5))

radiusO.replace(9.9)(KyoShape.Square(4.0)) // miss: passes through untouched

itemsT.foldMap(_.price)(cart)

itemsT.andThen(priceL).modify(_ + 0.5)(cart).items
```

### Codec byte faces

`Schema[A].encode` / `decode` under any kyo codec (json, msgpack,
protobuf, …) are exactly a Prism's two halves: `prism[C]` over the
encoded `Span[Byte]`, `stringPrism[C]` over the encoded `String`.
`decode`'s `Result` folds straight into the prism's `Either` tear —
failures and panics are the miss arm, carrying the original input back
losslessly. Compose with a bridged Focus lens to read/modify a field
*inside an encoded payload* in one expression:

```scala mdoc:silent
val itemJsonP = Schema[KyoItem].stringPrism[Json]
```

```scala mdoc
itemJsonP.reverseGet(KyoItem("apple", 1.0))

itemJsonP.andThen(priceL).modify(_ * 10)("""{"name":"apple","price":1.0}""")

itemJsonP.getOption("not json") // miss

itemJsonP.modify(identity)("not json") // misses pass writes through untouched
```

Lawfulness caveats, same as the [avro](avro.md)/[circe](circe.md)
bridges: `get ∘ reverseGet` is the identity (the codec roundtrip law);
`reverseGet ∘ get` re-encodes, so byte-level layout normalizes.
Decoding consumes ONE value — trailing input is accepted on reads and
dropped by rewrites.

### The untyped tree: `Structure.Value`

Every `Schema[A]` encodes to kyo's untyped `Structure.Value` tree
before a codec turns it into bytes — kyo's answer to circe's `Json`,
except one tree round-trips through **every** codec. `StructureValues`
ports the [circe module's](circe.md) playbook to that tree: one
constructor prism per `Value` case (`str`, `integer`, `decimal`,
`record`, `sequence`, …), sibling-preserving `field`/`at`/`key`
navigation, an `each` traversal, a `Plated[Value]` for whole-document
rewrites — plus `variant(name)`, sum navigation circe has no analog
for. And `Schema[A].valuePrism` is the typed ↔ untyped face beside
`prism`/`stringPrism`: decode only the leaf you touch, leave the spine
untyped.

```scala mdoc:silent
import dev.constructive.eo.kyo.schema.StructureValues.{*, given}

val cartV = Structure.encode(cart)
```

```scala mdoc
field("id").getOption(cartV)

// Untyped spine, typed leaves — only KyoItem is ever decoded:
Structure.decode[KyoCart](
  field("items").andThen(each).andThen(Schema[KyoItem].valuePrism)
    .modify(i => i.copy(price = i.price + 0.5))(cartV)
)

// Sum navigation on the untyped side:
val shapeV = Structure.encode[KyoShape](KyoShape.Circle(2.5))
variant("Circle").andThen(field("radius")).andThen(decimal).modify(_ * 2)(shapeV)

// `atField` focuses the Option at a name, so a write can CREATE or DELETE
// the field — `field` can do neither (its miss passes writes through):
atField("note").replace(Some(Structure.Value.Str("gift")))(cartV)

atField("id").replace(None)(cartV)
```

`field` and `key` target the '''first''' matching name — `Record` and
`MapEntries` are `Chunk`-backed, so duplicates are representable, and
read-first/write-all would break `modify(identity)` on such a tree.
`atField` is lawful up to field **order** and no further: deleting
destroys the position, so a later insert appends rather than restoring
it in place. That is inherent to At-style access over an ordered record
— Monocle's `At` sidesteps it only because `Map` has no order.

`Plated.transform` / `rewrite` / `universe` walk the whole tree, any
schema, any depth:

```scala mdoc
import dev.constructive.eo.optics.Plated

Plated.transform[Structure.Value] {
  case Structure.Value.Str(s) => Structure.Value.Str(s.toUpperCase)
  case other                  => other
}(cartV)
```

And because kyo ships a public `Schema[Structure.Value]`, the
[byte faces above](#codec-byte-faces) apply to the untyped tree
itself — edit one field inside an encoded payload with **no typed
value materialised**, under any codec:

```scala mdoc
val valueJsonP = summon[Schema[Structure.Value]].stringPrism[Json]

valueJsonP.andThen(field("id")).andThen(str).modify(_.toUpperCase)(
  Schema[KyoCart].stringPrism[Json].reverseGet(cart)
)
```

One wire-format wart: RC6's `Structure.encode` spells a sum as a
single-field wrapper record (`Record(Chunk(("Circle", payload)))`) and
never emits the `Value.VariantCase` case declared for sums — kyo's own
`Path.Variant` navigation matches only `VariantCase`, so it can't see
the encoder's output
([getkyo/kyo#1860](https://github.com/getkyo/kyo/issues/1860)).
`variant(name)` accepts both spellings and rebuilds whichever it read,
so it keeps working whichever way upstream resolves it.

## JDK requirement

kyo `1.0.0-RC5+` ships Java-25-only bytecode, and macros execute
inside the compiler's JVM — so **compiling or running against
`cats-eo-kyo` requires JDK 25**, unlike the rest of eo (JDK 17 floor).
In this repo the kyo module drops out of the root aggregate on older
JVMs and CI runs a dedicated 25 lane.

## Automatic capability givens

Kyo's types can also provide eo capabilities *by themselves*, mirroring
[the ZIO module's givens](zio.md#automatic-capability-givens). A
`given` import (the `*` wildcard deliberately does not pull givens)
puts one optic given per pair in scope:

- `(TypeMap[R], A)` for every tagged slot `A` of `R` — the
  [`service`](#the-service-lens) lens;
- `(Maybe[A], A)` — a `Present` prism;
- `(Result[E, A], A)` — a success prism; failures and panics pass
  through writes untouched.

```scala mdoc
import dev.constructive.eo.kyo.given

def report[T](t: T)(using g: CanGet[T, Db]): String = g.get(t).url
def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)

report(tm)

bump(Maybe(41))

bump(Result.fail("e"): Result[String, Int])
```

Coherence rule as everywhere in eo: these are THE optic givens for
their `(S, A)` pairs — don't declare competing ones.

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
