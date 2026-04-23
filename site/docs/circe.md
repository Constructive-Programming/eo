# circe integration

The `cats-eo-circe` module adds two cursor-backed optics for
editing [circe](https://circe.github.io/circe/) JSON without
paying the cost of a full decode / re-encode round-trip.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-circe" % "@VERSION@"
```

## Why this exists

The classical Scala JSON-edit pattern is:

```scala
json.as[Person]                       // decode
    .map(p => p.copy(name = p.name.toUpperCase))
    .map(_.asJson)                    // re-encode
```

That decodes every field of `Person`, allocates a fresh
instance, and re-encodes every field — even if only one
leaf is changing. For wide records the work is mostly wasted.

`JsonPrism` / `JsonTraversal` walk a flat path directly through
circe's `JsonObject` / array representation, modifying only
the focused leaf and rebuilding the parents on the way up.
The
[`JsonPrismBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala)
and
[`JsonTraversalBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsonTraversalBench.scala)
suites document a roughly 2× speedup at every depth and
every array size.

## JsonPrism

```scala mdoc:silent
import eo.circe.codecPrism
import io.circe.Codec
import io.circe.syntax.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject

case class Address(street: String, zip: Int)
object Address:
  given Codec.AsObject[Address] = KindlingsCodecAsObject.derive

case class Person(name: String, age: Int, address: Address)
object Person:
  given Codec.AsObject[Person] = KindlingsCodecAsObject.derive
```

Construct a Prism to the root type, then drill into fields.
The `.address.street` sugar is macro-powered — it compiles to
`.field(_.address).field(_.street)`:

```scala mdoc
val alice   = Person("Alice", 30, Address("Main St", 12345))
val json    = alice.asJson
val streetP = codecPrism[Person].address.street

streetP.modifyUnsafe(_.toUpperCase)(json).noSpacesSortKeys
```

The default `modify` returns `Ior[Chain[JsonFailure], Json]` —
failures are surfaced rather than silently swallowed. The
`*Unsafe` variants preserve the pre-v0.2 silent behaviour.
Full coverage of both surfaces lives in the "Observable-by-default
failures" section of the v0.2 release notes.

Other operations (all the silent escape hatches):

```scala mdoc
streetP.getOptionUnsafe(json)
streetP.placeUnsafe("Broadway")(json).noSpacesSortKeys
streetP.transformUnsafe(_.mapString(_.reverse))(json).noSpacesSortKeys
```

Forgiving semantics on the `*Unsafe` surface — missing paths leave
the Json unchanged:

```scala mdoc
import io.circe.Json
val stump = Json.obj("name" -> Json.fromString("Alice"))
streetP.modifyUnsafe(_.toUpperCase)(stump).noSpacesSortKeys
```

## Array indexing

`.at(i)` drills into the `i`-th element of a JSON array:

```scala mdoc:silent
case class Order(name: String)
object Order:
  given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

case class Basket(owner: String, items: Vector[Order])
object Basket:
  given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
```

```scala mdoc
val basket     = Basket("Alice", Vector(Order("X"), Order("Y"), Order("Z")))
val basketJson = basket.asJson
val secondName = codecPrism[Basket].items.at(1).name

secondName.modifyUnsafe(_.toUpperCase)(basketJson).noSpacesSortKeys
```

Out-of-range / negative / non-array positions pass through
unchanged.

## JsonTraversal (`.each`)

`.each` splits the path at the current array focus and returns
a `JsonTraversal` that walks every element. Further `.field` /
`.at` / selectable-sugar calls on the traversal extend the
per-element suffix:

```scala mdoc
val everyName = codecPrism[Basket].items.each.name

everyName.modifyUnsafe(_.toUpperCase)(basketJson).noSpacesSortKeys
everyName.getAllUnsafe(basketJson)
```

Empty arrays and missing paths leave the Json unchanged.

## Multi-field focus — `.fields(_.a, _.b)`

`.fields(selector1, selector2, ...)` focuses a bundle of named
case-class fields as a Scala 3 `NamedTuple`. Selectors arrive in
selector-order; the NamedTuple type reflects that. Arity must be
≥ 2 — use `.field(_.x)` for a single-field focus.

```scala mdoc:silent
type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derive
```

```scala mdoc
val nameAge = codecPrism[Person].fields(_.name, _.age)

nameAge
  .modifyUnsafe(nt => (name = nt.name.toUpperCase, age = nt.age + 1))(json)
  .noSpacesSortKeys
```

Full-cover selection — spanning every field of `Person` — still
returns a `JsonFieldsPrism`, **not** an `Iso`. JSON decode can
always fail (the input may not even be a JSON object) so totality
isn't witnessable, and an Iso would misleadingly advertise a
guarantee we cannot provide.

```scala mdoc:silent
type Full = NamedTuple.NamedTuple[("name", "age", "address"), (String, Int, Address)]
given Codec.AsObject[Full] = KindlingsCodecAsObject.derive
```

```scala mdoc
val fullL = codecPrism[Person].fields(_.name, _.age, _.address)
// `fullL` is a JsonFieldsPrism, not a JsonIso — the return type is
// unchanged when coverage is complete.
```

Per-element multi-field focus via `.each.fields` focuses a
NamedTuple on every element of an array:

```scala mdoc:silent
case class Item(name: String, price: Double, qty: Int)
object Item:
  given Codec.AsObject[Item] = KindlingsCodecAsObject.derive

case class MultiBasket(owner: String, items: Vector[Item])
object MultiBasket:
  given Codec.AsObject[MultiBasket] = KindlingsCodecAsObject.derive

type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive
```

```scala mdoc
val mbJson = MultiBasket(
  "Alice",
  Vector(Item("x", 1.0, 1), Item("y", 2.0, 2)),
).asJson

codecPrism[MultiBasket]
  .items
  .each
  .fields(_.name, _.price)
  .modifyUnsafe(nt => (name = nt.name.toUpperCase, price = nt.price * 2))(mbJson)
  .noSpacesSortKeys
```

## Reading diagnostics from the default Ior surface

The default `modify` / `transform` / `place` / `transfer` / `get`
methods on `JsonPrism`, `JsonFieldsPrism`, `JsonTraversal`, and
`JsonFieldsTraversal` all return `Ior[Chain[JsonFailure], Json]`
(or `, A]` / `, Vector[A]]` for the reads). Three observable
shapes:

- `Ior.Right(json)` — clean success.
- `Ior.Both(chain, json)` — partial success. The `json` reflects
  every update that did succeed; the `chain` lists every skip.
- `Ior.Left(chain)` — no result producible. Typical for `get`
  misses and for traversal-prefix misses where there's nothing
  to iterate.

```scala mdoc
import cats.data.Ior
import eo.circe.{JsonFailure, PathStep}

val stumpJson = Json.obj("name" -> Json.fromString("Alice"))

// default modify returns Ior.Both on a failure — the Json is
// unchanged, the chain documents the miss
streetP.modify(_.toUpperCase)(stumpJson)
```

Each `JsonFailure` case carries the `PathStep` at which the walk
refused, plus (for `DecodeFailed`) the underlying circe
`DecodingFailure`:

```scala mdoc
val miss: JsonFailure = JsonFailure.PathMissing(PathStep.Field("street"))
miss.message
```

Traversal-side accumulation collects one `JsonFailure` per
skipped element — on a mixed-failure array, the `Ior.Both` Json
reflects the successful updates and the chain lists every element
that was left unchanged:

```scala mdoc
val brokenArr = Json.arr(
  Order("x").asJson,
  Json.fromString("oops"), // not an Order
  Order("z").asJson,
)
val brokenBasket =
  Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenArr)

codecPrism[Basket]
  .items
  .each
  .name
  .modify(_.toUpperCase)(brokenBasket)
```

## Ignoring failures (the `*Unsafe` escape hatch)

For callers who have measured and know they don't want the Ior
allocation, every default method has a sibling `*Unsafe` variant
that preserves the pre-v0.2 silent-forgiving behaviour
byte-for-byte:

```scala mdoc
// Pre-v0.2 shape: modifyUnsafe, silent pass-through on miss.
streetP.modifyUnsafe(_.toUpperCase)(stumpJson).noSpacesSortKeys

// Equivalent via the default surface:
streetP.modify(_.toUpperCase)(stumpJson).getOrElse(stumpJson).noSpacesSortKeys
```

Both spellings produce the same Json. The first pays nothing for
diagnostics; the second gives an observable `Ior` at the price of
one allocation.

## Migration notes (v0.2 rename)

v0.2 renames the silent methods to `*Unsafe` and repurposes the
clean name for the Ior-bearing surface. The mechanical replacement
is: swap the method name for its `*Unsafe` sibling if you want
the pre-v0.2 behaviour preserved exactly.

| Class                     | v0.1 (silent)  | v0.2 default (Ior-bearing)              | v0.2 `*Unsafe` (silent) |
|---------------------------|-----------------|------------------------------------------|-------------------------|
| `JsonPrism[A]`            | `modify(f)`     | `modify(f): Json => Ior[Chain[JsonFailure], Json]` | `modifyUnsafe(f)`       |
| `JsonPrism[A]`            | `transform(f)`  | `transform(f): Json => Ior[...]`          | `transformUnsafe(f)`    |
| `JsonPrism[A]`            | `place(a)`      | `place(a): Json => Ior[...]`              | `placeUnsafe(a)`        |
| `JsonPrism[A]`            | `transfer(f)`   | `transfer(f): Json => C => Ior[...]`      | `transferUnsafe(f)`     |
| `JsonPrism[A]`            | `getOption`     | `get(j): Ior[..., A]`                     | `getOptionUnsafe`       |
| `JsonFieldsPrism[A]`      | (new)           | same five                                 | same five               |
| `JsonTraversal[A]`        | `modify(f)`     | `modify(f): Json => Ior[...]`             | `modifyUnsafe(f)`       |
| `JsonTraversal[A]`        | `transform(f)`  | `transform(f): Json => Ior[...]`          | `transformUnsafe(f)`    |
| `JsonTraversal[A]`        | `getAll`        | `getAll(j): Ior[..., Vector[A]]`          | `getAllUnsafe`          |
| `JsonTraversal[A]`        | (new in v0.2)   | `place(a)` / `transfer(f)` (Ior-bearing)  | `placeUnsafe` / `transferUnsafe` |
| `JsonFieldsTraversal[A]`  | (new)           | same five                                 | same five               |

The `*Unsafe` bodies are byte-identical to the pre-v0.2 silent
shape — no behaviour change, just the rename. The default Ior
surface is a new option: reach for it when you want to see the
path-level diagnostic.

## When to reach for which

| Task                                                  | Use                       |
|-------------------------------------------------------|---------------------------|
| Edit one leaf deep in a JSON tree                     | `JsonPrism` via `.address.street` sugar |
| Edit element `i` of a JSON array                      | `codecPrism[…].items.at(i).…` |
| Edit every element of a JSON array                    | `codecPrism[…].items.each.…` + `modify` |
| Read every element's focus                            | `codecPrism[…].items.each.…` + `getAll` |
| Edit multiple fields atomically                       | `codecPrism[…].fields(_.a, _.b).modify(...)` |
| Observe why a modify was a silent no-op               | default Ior-bearing `.modify(...)` — inspect the `Ior.Both` / `Ior.Left` chain |
| Edit the whole root record (and you have a Codec)     | `codecPrism[Person].modifyUnsafe(f)` |

For the full failure-mode matrix (missing paths, non-array
focuses, empty collections, out-of-range indices), see the
behaviour
[spec](https://github.com/Constructive-Programming/eo/blob/main/circe/src/test/scala/eo/circe/JsonPrismSpec.scala).
