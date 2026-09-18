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
  optic. [`TRef` / `TMap` get the same ops](#stm-focus-ops) returning
  `USTM`, so focused updates compose into one atomic transaction.

Three **optional-dependency sub-packages** extend the same treatment
across the ZIO ecosystem (add the artifact yourself — the
avro/circe/kyo-schema pattern):

- [`eo.zio.schema`](#the-zio-schema-bridge) — zio-schema's own
  `AccessorBuilder` extension point filled in with eo optics, the
  untyped `DynamicValue` navigation kit, and the `BinaryCodec` byte
  face (JSON, protobuf, Avro, msgpack, thrift).
- [`eo.zio.json`](#zio-json-optics) — the circe playbook on
  `zio.json.ast.Json`, plus a `JsonCursor → Optional` bridge.
- [`eo.zio.prelude`](#zvalidation-optics) — `ZValidation` success /
  error-accumulation optics.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-zio" % "@VERSION@"
```

## Which direction are you integrating?

The module is a two-way adapter; pick the row that matches what you
are holding and what the other side expects:

| You're holding | The other side expects | Reach for |
|---|---|---|
| an optic (`lens[AppConfig](_.db)`) | a `ZLayer` in the [dependency graph](https://zio.dev/reference/di/building-dependency-graph) | [`focusLayer`](#layer-projection) — the optic becomes the layer's wiring function |
| an optic | an environment transformation ([overriding a dependency](https://zio.dev/reference/di/providing-different-implementation-of-a-service)) | [`service` + `.andThen`](#the-service-lens) handed to `provideSomeEnvironment` |
| a `ZEnvironment`, `Exit`, or `Ref` of either | eo capability evidence (`CanGet[T, A]`, `CanModify[T, A]`, …) | [`import dev.constructive.eo.zio.given`](#automatic-capability-givens) — no hand-written given |
| a `TRef` / `TMap` | focused updates that compose atomically | [STM focus ops](#stm-focus-ops) — each op is a `USTM` |
| a `Schema[A]` | a Lens per field, a Prism per case, a Traversal per collection | [`makeAccessors(EoAccessorBuilder)`](#the-zio-schema-bridge) |
| encoded bytes or a `DynamicValue` tree | a targeted edit with no full decode | [`codec.prism` / `schema.dynamicPrism` + `DynamicValues`](#the-zio-schema-bridge) |
| a `zio.json.ast.Json` or a `JsonCursor` path | eo navigation / composition | [`JsonValues` + `cursor.optional`](#zio-json-optics) |
| a `ZValidation` | success / accumulated-error optics | [`Validations`](#zvalidation-optics) |

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

That composed optic is what you hand to `provideSomeEnvironment` —
ZIO's own idiom for
[providing a different implementation of a service](https://zio.dev/reference/di/providing-different-implementation-of-a-service):
tweak one field deep inside one service, leave the rest of the
environment alone. Where ZIO's docs override a whole service, the
drilled optic overrides one *field* of one service:

```scala mdoc
val poolOf: ZIO[Db & Metrics, Nothing, Int] = ZIO.serviceWith[Db](_.pool)

val throttled = poolOf.provideSomeEnvironment[Db & Metrics](dbPool.replace(1))

Unsafe.unsafe(implicit u =>
  Runtime.default.unsafe.run(throttled.provideEnvironment(env)).getOrThrowFiberFailure()
)
```

## Layer projection

ZIO's idiom for deriving one service from another is a `ZLayer` whose
construction function projects the dependency — see
[building the dependency graph](https://zio.dev/reference/di/building-dependency-graph)
and [dependency propagation](https://zio.dev/reference/di/dependency-propagation)
in the ZIO docs. That projection *is* a getter, so `focusLayer[S, A]`
builds the layer from `CanGet[S, A]` evidence and slots into the same
`>>>` / `provide` graphs as any hand-written layer — the optic is the
wiring:

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

## STM focus ops

The same four ops exist on `TRef[S]` (and keyed `getFocusAt` /
`updateFocusAt` / `setFocusAt` on `TMap[K, V]`), returning `USTM`
instead of `UIO`. That is
the point, not a spelling difference: focused updates across
*several* transactional references compose into ONE atomic
transaction — something the `Ref` ops structurally cannot express:

```scala mdoc
import _root_.zio.stm.*

val syncPools =
  for
    primary <- TRef.make(Db("a", 1)).commit
    replica <- TRef.make(Db("b", 1)).commit
    _       <- STM.atomically(
                 primary.updateFocus[Int](_ + 9)(using poolL) *>
                   replica.updateFocus[Int](_ + 9)(using poolL)
               )
    out     <- primary.get.commit
  yield out

Unsafe.unsafe(implicit u =>
  Runtime.default.unsafe.run(syncPools).getOrThrowFiberFailure()
)
```

On `TMap`, absent keys pass writes through untouched — no entry is
invented, exactly the miss semantics of every partial optic in eo.

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

## Chunk element optics

`Chunks.each` / `Chunks.at(i)` / `Chunks.eachNonEmpty` are the
collection legs everything below stands on (`DynamicValue.Sequence`,
zio-json arrays, and `BinaryCodec` payloads all speak `Chunk`). zio
ships no cats instances — the orphan `Traverse[Chunk]` given belongs
to zio-interop-cats — so these are **constructors** over private
adapters, per the
[constructor-not-given doctrine](../capabilities.md#cats-containers-as-capability-evidence):

```scala mdoc
import dev.constructive.eo.zio.Chunks

Chunks.each[Int, Int].modify(_ + 1)(Chunk(1, 2, 3))

Chunks.at[Int](1).replace(9)(Chunk(1, 2, 3))
```

## The zio-schema bridge

`eo.zio.schema` (add `dev.zio %% "zio-schema"` yourself). zio-schema
ships [`AccessorBuilder`](https://zio.dev/zio-schema/) — a first-class
extension point where an optics library plugs itself in.
`EoAccessorBuilder` fills it with eo optics: one call returns a fused
Lens per record field, a Prism per enum case, a Traversal per
collection, for any `Schema[A]`, no macros involved:

```scala mdoc:silent
import _root_.zio.schema.*
import dev.constructive.eo.zio.schema.*

case class Person(name: String, age: Int)
object Person:
  given schema: Schema[Person] = DeriveSchema.gen[Person]
```

```scala mdoc
val personCC = Person.schema.asInstanceOf[Schema.CaseClass2[String, Int, Person]]
val (personName, personAge) = personCC.makeAccessors(EoAccessorBuilder)

personAge.modify(_ + 1)(Person("ada", 41))
```

`DynamicValues` is the navigation kit for the **untyped
`DynamicValue` tree** — what any `Schema[A]` encodes to before a
codec turns it into bytes, so one kit covers every wire format
zio-schema speaks. Constructor prisms (`str`, `int`, `record`, …, and
the generic `primitive(standardType)`), sibling-preserving `field` /
`at` / `key` / `variant` navigation, an `each` traversal, and a
`Plated` instance for whole-tree rewrites. `schema.dynamicPrism`
crosses between the two worlds:

```scala mdoc
import dev.constructive.eo.zio.schema.DynamicValues

val dyn = Person.schema.toDynamic(Person("ada", 41))

DynamicValues.field("age").andThen(DynamicValues.int).modify(_ + 1)(dyn)
  .toTypedValue(using Person.schema)
```

And the byte face: any `BinaryCodec[A]` — zio-schema-json,
-protobuf, -avro, -msgpack, and -thrift all produce one — is a Prism
between encoded bytes and `A`, with the usual byte-face laws
(roundtrip identity one way, re-encode normalisation the other,
misses pass through writes):

```scala mdoc
import _root_.zio.schema.codec.JsonCodec as ZJsonCodec

val personCodec = ZJsonCodec.schemaBasedBinaryCodec[Person](using Person.schema)

new String(
  personCodec.prism.modify(p => p.copy(age = p.age + 1))(
    personCodec.encode(Person("ada", 41))
  ).toArray
)
```

## zio-json optics

`eo.zio.json` (add `dev.zio %% "zio-json"` yourself) is the circe
playbook on `zio.json.ast.Json`: constructor prisms, `field` / `at` /
`each` navigation, `Plated`, and `JsonValues.text` as the
`String ↔ Json` on-ramp. Plus the seam circe has no analog for:
zio-json's own `JsonCursor` is already a typed path, and
`cursor.optional` turns any existing one into a sibling-preserving eo
Optional — cursor-based codebases get eo composition without
rewriting a path:

```scala mdoc
import _root_.zio.json.ast.{Json, JsonCursor}
import dev.constructive.eo.zio.json.*
import dev.constructive.eo.zio.json.JsonValues.text

val doc = Json.Obj(
  "name" -> Json.Str("ada"),
  "tags" -> Json.Arr(Json.Str("a"), Json.Str("b")),
)

JsonValues.field("name").andThen(JsonValues.str).getOption(doc)

JsonCursor.field("tags").isArray.element(1).optional.replace(Json.Str("B"))(doc)

text.andThen(JsonValues.field("name")).andThen(JsonValues.str)
  .modify(_.toUpperCase)("""{"name":"ada"}""")
```

`JsonCodec[A].stringPrism` is the typed wire face — the same shape as
[`AvroJson`](avro.md) and the kyo byte faces.

Both kits also ship `atField(name)`, the [`At`](../optics.md)-shaped
sibling of `field`: its focus is the `Option` at that name, so a write
can **create or delete** the field, which `field` structurally cannot
(its focus is the value, so an absent field is a miss and misses pass
writes through). Within an object it is total — presence lives in the
focus — so `getOption` yields `Some(None)` for an object lacking the
field:

```scala mdoc
JsonValues.atField("nickname").replace(Some(Json.Str("ada")))(doc)

JsonValues.atField("name").replace(None)(doc)
```

## ZValidation optics

`eo.zio.prelude` (add `dev.zio %% "zio-prelude"` yourself — zio-schema
already carries it transitively). `Validations.success` is an
Optional, not a Prism: a prism-shaped write would have to invent a
log and would drop the one already there. `Validations.eachFailure`
traverses every accumulated error and is polymorphic in the error
type, so error translation is ordinary `modify`:

```scala mdoc
import _root_.zio.prelude.Validation
import dev.constructive.eo.zio.prelude.Validations

Validations.success[Nothing, String, Int].modify(_ * 2)(Validation.succeed(21))

Validations.eachFailure[Nothing, String, String, Unit].modify(_.toUpperCase)(
  Validation.validate(Validation.fail("first"), Validation.fail("second")).map(_ => ())
)
```

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

What the module *does* own is the plumbing interop-cats can't give
you: `Ref.Synchronized[S].updateFocusZIO(f)` runs an effectful rewrite
**at the focus** while the ref is held, so the read-modify-write stays
atomic — the thing `Ref` cannot express at all. It takes your
`Applicative[ZIO[R, E, *]]` (hence no new dependency here) and
`CanModifyA`, so it works through a lens, a prism (a miss means the
effect never runs), or a traversal (one effect per focus, sequenced):

```scala
val ref: Ref.Synchronized[Db] = ???

ref.updateFocusZIO(refresh)(using urlL, summon)
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
