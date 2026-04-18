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

streetP.modify(_.toUpperCase)(json).noSpacesSortKeys
```

Other operations:

```scala mdoc
streetP.getOption(json)
streetP.place("Broadway")(json).noSpacesSortKeys
streetP.transform(_.mapString(_.reverse))(json).noSpacesSortKeys
```

Forgiving semantics — missing paths leave the Json unchanged:

```scala mdoc
import io.circe.Json
val stump = Json.obj("name" -> Json.fromString("Alice"))
streetP.modify(_.toUpperCase)(stump).noSpacesSortKeys
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

secondName.modify(_.toUpperCase)(basketJson).noSpacesSortKeys
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

everyName.modify(_.toUpperCase)(basketJson).noSpacesSortKeys
everyName.getAll(basketJson)
```

Empty arrays and missing paths leave the Json unchanged.

## When to reach for which

| Task                                                  | Use                       |
|-------------------------------------------------------|---------------------------|
| Edit one leaf deep in a JSON tree                     | `JsonPrism` via `.address.street` sugar |
| Edit element `i` of a JSON array                      | `codecPrism[…].items.at(i).…` |
| Edit every element of a JSON array                    | `codecPrism[…].items.each.…` + `modify` |
| Read every element's focus                            | `codecPrism[…].items.each.…` + `getAll` |
| Edit the whole root record (and you have a Codec)     | `codecPrism[Person].modify(f)` |

For the full failure-mode matrix (missing paths, non-array
focuses, empty collections, out-of-range indices), see the
behaviour
[spec](https://github.com/Constructive-Programming/eo/blob/main/circe/src/test/scala/eo/circe/JsonPrismSpec.scala).
