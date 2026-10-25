# jsoniter integration

The `cats-eo-jsoniter` module adds two cursor-backed optics for
editing JSON byte streams without ever materialising an AST. They
sit on top of [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala)
and reuse the existing cats-eo carriers — no new carrier ships in
this module. Paths can be spelled as JSONPath strings or built with
the compile-time-checked [typed cursors](#typed-cursors)
(`JsoniterPrism[S].field(_.x)`), mirroring eo-circe's surface.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-jsoniter" % "@VERSION@"
```

## Why this exists

[`circe`](https://circe.github.io/circe/) parses to a `Json` AST
first, then walks the tree. That makes circe great for dynamic-shape
work (the shapes the tree can take are unbounded; you can drill into
anything via cursors). It's also the source of most of circe's
runtime cost — most of an HTTP-request-handler's JSON time is in the
parse, not the drill.

[`jsoniter-scala`](https://github.com/plokhotnyuk/jsoniter-scala)
takes the opposite trade. Codecs are derived at compile time, JSON
is streamed straight into the codec, no AST materialised. Throughput
is 5–10× circe on parse-heavy workloads. The cost: every focus type
needs a `JsonValueCodec[A]` in scope; dynamic-shape work isn't
natural.

cats-eo-jsoniter is the optic surface for that style. `JsoniterPrism`
focuses a JSONPath inside a JSON byte buffer, decodes only when the
user reads the focus, encodes-and-splices on write. `JsoniterTraversal`
fans out over `[*]` array paths. The realistic comparison vs eo-circe
on the hot path:

| Operation                                | eo-circe | eo-jsoniter | Speedup |
|------------------------------------------|---------:|------------:|--------:|
| Read scalar `Long` at depth 3            |  ~810 ns |     ~50 ns  |  16.2×  |
| Read scalar `String` at depth 4          |  ~830 ns |    ~102 ns  |   8.1×  |
| Read miss (path doesn't resolve)         |  ~810 ns |     ~68 ns  |  11.8×  |
| Fold over 10-element array (`[*]`)       |  ~820 ns |    ~340 ns  |   2.4×  |
| Write `.replace` Long at depth 3         | ~1450 ns |    ~100 ns  |  14.3×  |
| Write `.modify` Long at depth 3          | ~1440 ns |     ~87 ns  |  16.5×  |

Numbers from the
[`JsoniterBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/dev/constructive/eo/bench/JsoniterBench.scala)
suite — see [benchmarks → JsoniterBench](../benchmarks.md) for the full
table with confidence intervals and caveats. The traversal speedup
narrows because per-element decode + array allocation accumulates;
larger arrays push it back up. Writes don't degrade vs reads on the
jsoniter side because the splice is bounded by `O(src.length)`
memcpy — for hot-path 250-byte payloads that's ~50 ns.

eo-jsoniter is **complementary** to eo-circe, not a replacement.
Reach for circe when shapes are dynamic; reach for jsoniter when the
shape is fixed at compile time and you care about throughput.

## JsoniterPrism

A `JsoniterPrism[A]` focuses a single value at a JSONPath inside an
`Array[Byte]`. Construct it with the path string + a
`JsonValueCodec[A]` in scope — and note that the constructor returns
`Either[String, JsoniterPrism[A]]`: a path is DATA (a config value, a
CLI argument, a registry lookup), so an unparseable one is a value you
handle, not an exception you catch. There is deliberately no throwing
twin.

Every path on this page is a literal, so the examples unwrap once
(a `Left` here would be a typo in the page itself) and use the prism:

```scala mdoc:silent
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.Optic.*

given JsonValueCodec[Long]   = JsonCodecMaker.make
given JsonValueCodec[String] = JsonCodecMaker.make

// Page helpers for LITERAL paths. Application code handles the Either
// instead (e.g. `fromPath[Long](raw).fold(handle, use)`).
def prism[A](path: String)(using JsonValueCodec[A]): JsoniterPrism[A] =
  JsoniterPrism.fromPath[A](path).fold(msg => throw new IllegalArgumentException(msg), identity)

def traversal[A](path: String)(using JsonValueCodec[A]): JsoniterTraversal[A] =
  JsoniterTraversal.fromPath[A](path).fold(msg => throw new IllegalArgumentException(msg), identity)

val sample: Array[Byte] =
  """{"payload":{"user":{"id":42,"email":"alice@example.com"}}}""".getBytes("UTF-8")

val idP    = prism[Long]("$.payload.user.id")
val emailP = prism[String]("$.payload.user.email")
```

### Reading

`.foldMap` decodes the focus through the codec only when the focus
hits; on miss it returns the `Monoid` identity:

```scala mdoc
import cats.instances.long.given
import cats.instances.string.given

idP.foldMap(identity[Long])(sample)
emailP.foldMap(identity[String])(sample)

// Miss: path doesn't resolve, returns Monoid.empty
val absentP = prism[Long]("$.payload.user.absent")
absentP.foldMap(identity[Long])(sample)
```

`.headOption` / `.length` / `.exists` light up the same way through
the underlying `Affine` carrier — no separate code path.

### Writing — `.replace` / `.modify`

The Hit branch encodes the new focus via `JsonValueCodec[A]` and
splices into the source bytes at the recorded `[start, end)` span.
Three `arraycopy`s into a fresh `Array[Byte]`. Length-changing
encodings (number widening, string growth/shrinkage) are handled
transparently:

```scala mdoc
import dev.constructive.eo.data.Affine.given

new String(idP.replace(99L)(sample), "UTF-8")
new String(idP.replace(1234567L)(sample), "UTF-8")  // longer
new String(emailP.replace("bob@x.org")(sample), "UTF-8")  // shorter
```

`.modify` runs `f` on the decoded focus, then encodes the result and
splices:

```scala mdoc
new String(idP.modify(_ * 10)(sample), "UTF-8")
```

Miss path is a no-op — the original bytes pass through unchanged:

```scala mdoc
val sameBytes = absentP.replace(99L)(sample)
sameBytes.toSeq == sample.toSeq
```

## Typed cursors

`JsoniterPrism[A]` (no path argument) is the root-level
`Prism[Array[Byte], A]`: `to` decodes the whole document via the
codec, `from` / `reverseGet` encode it back. From the root, `.field(_.x)`
/ `.at(i)` / `.each` drill in with compile-time checks against the
case-class schema, so no JSONPath string is ever hand-written. The
`.address.street` sugar is macro-powered and compiles to
`.field(_.address).field(_.street)`:

```scala mdoc:silent
case class Zip(code: Int)
case class Address(street: String, zip: Zip)
case class Person(name: String, age: Int, address: Address)

given JsonValueCodec[Int]     = JsonCodecMaker.make
given JsonValueCodec[Zip]     = JsonCodecMaker.make
given JsonValueCodec[Address] = JsonCodecMaker.make
given JsonValueCodec[Person]  = JsonCodecMaker.make

val alice       = Person("Alice", 30, Address("Main St", Zip(12345)))
val aliceBytes  = com.github.plokhotnyuk.jsoniter_scala.core.writeToArray(alice)
```

```scala mdoc
val streetP = JsoniterPrism[Person].address.street          // Dynamic sugar
val ageP    = JsoniterPrism[Person].field(_.age)            // explicit form

streetP.foldMap(identity[String])(aliceBytes)
new String(ageP.modify(_ + 1)(aliceBytes), "UTF-8")
JsoniterPrism[Person].headOption(aliceBytes)                // whole-document decode
```

Every drilled cursor is the same `JsoniterPrism` the string-path
factory builds — `JsoniterPrism[Person].address.street` and
`JsoniterPrism.fromPath[String]("$.address.street")` (the `Either`-returning
constructor above) are the identical optic.
Each step needs a `JsonValueCodec` for its focus type in scope; when
you don't want to derive codecs for intermediate types you never
decode, use the string-path factory, which needs only the leaf codec.

`.at(i)` and `.each` work on collection-typed fields; `.each` hands
off to a `JsoniterTraversal`, and further `.field` calls continue
past the wildcard:

```scala mdoc:silent
case class Item(sku: String, price: Double)
case class Basket(items: List[Item])

given JsonValueCodec[Double]     = JsonCodecMaker.make
given JsonValueCodec[Item]       = JsonCodecMaker.make
given JsonValueCodec[List[Item]] = JsonCodecMaker.make
given JsonValueCodec[Basket]     = JsonCodecMaker.make

val basketBytes = com.github.plokhotnyuk.jsoniter_scala.core.writeToArray(
  Basket(List(Item("a", 1.5), Item("b", 2.5)))
)
```

```scala mdoc
import cats.instances.double.given

JsoniterPrism[Basket].field(_.items).at(1).headOption(basketBytes)
JsoniterPrism[Basket].field(_.items).each.price.foldMap(identity[Double])(basketBytes)
```

## Migrating a `JsonCodecMaker` model (optics-as-evidence)

There are two ways to use this module — pick deliberately:

1. **Layer on an existing codec**: keep materialising your case class
   elsewhere; use a prism for the one or two hot-path fields.
2. **Replace the materialised model**: the wire `Array[Byte]` *is* the
   data structure. This is the intended endpoint of "replace the
   jsoniter macros with eo optics" — you do **not** look for a
   codec-derivation API in this module, because the point is to stop
   needing a whole-document codec.

Before — macro-codec'd model, whole document decoded and re-encoded to
touch one field:

```scala
case class User(id: Long, email: String, plan: String)
object User:
  given JsonValueCodec[User] = JsonCodecMaker.make

val user    = readFromArray[User](bytes)   // materialises everything
val updated = writeToArray(user.copy(plan = "pro"))
```

After — byte holder + field prisms with **leaf codecs only**
(`JsonValueCodec[User]` is never derived):

```scala mdoc:silent
// given JsonValueCodec[String] = JsonCodecMaker.make   — leaf codec, already in scope above

val userBytes: Array[Byte] =
  """{"id":7,"email":"a@x.org","plan":"free"}""".getBytes("UTF-8")

val planP = prism[String]("$.plan")
```

```scala mdoc
planP.headOption(userBytes)                          // read one field
new String(planP.replace("pro")(userBytes), "UTF-8") // write it back, siblings untouched
```

The consuming code then follows the standard doctrine —
[consume via capability, construct via optic](../capabilities.md).
Signatures demand the weakest capability that covers what they do;
the prism *is* the evidence, so the byte holder slots into functions
that never name the optic or the wire format:

```scala mdoc:silent
import dev.constructive.eo.*

// These know NOTHING about JSON, jsoniter, or Array[Byte] — only
// that a String plan can be read out of / rewritten inside a T.
// That generic T is what buys you:
//   - testing:    unit-test the logic with a plain case class + lens,
//                 no JSON fixtures or byte buffers involved;
//   - re-use:     the same function serves the byte holder, the
//                 materialised model, an Avro record, a circe Json —
//                 any T with the evidence;
//   - decoupling: the module defining these never depends on the wire
//                 format, the codec choice, or this library's optics.
def currentPlan[T](t: T)(using cg: CanGetOption[T, String]): Option[String] =
  cg.getOption(t)

def upgradePlan[T](t: T)(using cm: CanModify[T, String]): T =
  cm.replace("pro")(t)

// The prism given IS the capability evidence for T = Array[Byte]
// (one optic given per (S, A) pair — the coherence rule applies):
given JsoniterPrism[String] = planP
```

```scala mdoc
currentPlan(userBytes)
new String(upgradePlan(userBytes), "UTF-8")
```

And the same consumers serve the mode-1 materialised model unchanged —
an [eo-generics lens](../generics.md) on the case class is equally
valid evidence, which is also the natural way to unit-test them:

```scala mdoc:silent
import dev.constructive.eo.generics.lens

case class User(id: Long, email: String, plan: String)

given CanModify[User, String] = lens[User](_.plan)
```

```scala mdoc
upgradePlan(User(7, "a@x.org", "free"))
```

The corners that trip a mechanical migration:

- **Optional fields**: `JsonCodecMaker` omits `None` on the wire, so an
  absent field is simply a Miss — `.headOption` returns `None`, writes
  pass through. There is no way to *add* a missing field through a
  prism (splice needs a span); keep a template with the field present,
  or fall through to eo-circe for structural edits.
- **Discriminated unions** (`"type"`-tagged enums): read the
  discriminator with a `String` prism at the tag path, then apply the
  per-variant prisms. A whole-variant focus works too — derive the
  codec for the *case* (not the sealed parent) and point a prism at the
  variant's payload path.
- **Assembling a value from several fields**: compose reads (two
  prisms, two `.headOption` calls) rather than looking for a
  full-record decoder — if you find yourself wanting the whole record,
  that's mode 1, and `JsoniterPrism[A]` (or plain `readFromArray`) is
  the honest spelling.

## JsoniterTraversal

`JsoniterTraversal[A]` accepts wildcard paths (`[*]`) — fans out over
every element of the focused array. Carrier is `MultiFocus[PSVec]`,
the same carrier `Traversal.each` uses, so the surface composes
uniformly with the rest of cats-eo's traversal machinery.

```scala mdoc:silent
import cats.instances.int.given

val cart: Array[Byte] =
  """{"cart":{"items":[1,2,3,4,5,6,7,8,9,10]}}""".getBytes("UTF-8")

val itemsT = traversal[Long]("$.cart.items[*]")
```

```scala mdoc
itemsT.foldMap(identity[Long])(cart)            // sum, via Monoid[Long]
itemsT.foldMap(_ => 1)(cart)                    // count, via Monoid[Int]
```

Spans whose decode throws are silently dropped — `foldMap` reads the
focuses that exist and ignores the rest. `JsoniterPrism` rejects
wildcard paths at construction with an explicit redirect to this
factory.

`.modify` writes back over `[*]` too: every focus is re-encoded and
all spans are spliced in a single pass (segments between spans are
copied verbatim, so later spans can't be invalidated by earlier
splices). Elements whose decode refused are dropped from the focus
set together with their span and stay byte-untouched:

```scala mdoc
new String(itemsT.modify(_ * 10)(cart), "UTF-8")
```

The two failure directions are deliberately asymmetric, because
`Optic.from` is total by type — a write has nowhere to report a
failure. A decode failure DROPS the element from the focus set; an
ENCODE failure keeps that element's ORIGINAL bytes while its siblings
are written, so a partially applied write still reports success. Read
first (`foldMap` / `getAll`) when you need to know which elements were
really there.

## Reading in place from a larger buffer

A JSON document often arrives inside a bigger byte array: one network
read holding several messages, a framed log segment, a memory-mapped
file. `.inPlace` turns a prism or a traversal into the same optic over
a `JsonSlice` — `length` bytes of an array from `offset` on — which
reads the document where it lies, with nothing copied before the scan:

```scala mdoc:silent
import dev.constructive.eo.jsoniter.{JsonSlice, jsonSlice}

// Two documents back to back in one buffer, as a single read might deliver them.
val other  = """{"payload":{"user":{"id":7}}}""".getBytes("UTF-8")
val buffer = other ++ sample
```

```scala mdoc
val second = JsonSlice(buffer, other.length, sample.length)

idP.inPlace.foldMap(identity[Long])(second)
emailP.inPlace.foldMap(identity[String])(second)
```

**No copy is made**, and that is a contract, not a detail:

- **Don't mutate the array** while a slice of it, or anything read
  from one, is in use: the optics read the bytes as they are when they
  run.
- **A slice keeps the whole array reachable**, however few bytes it
  covers — a 300-byte slice of a 256 KiB read pins the 256 KiB. Call
  `.toArray` on anything you keep long-term.
- **Writes never mutate the source.** A write that splices returns a
  slice over a **fresh** array holding only the slice's bytes, with the
  new focus spliced in (offset 0). A write that changes nothing (a
  miss) hands back the input slice itself:

```scala mdoc
val written = idP.inPlace.replace(99L)(second)
(written.offset, written.length == written.array.length)

absentP.inPlace.replace(99L)(second) eq second

new String(buffer, "UTF-8") == new String(other ++ sample, "UTF-8")  // untouched
```

The scan never reads outside the slice, and the focus decodes through
jsoniter-scala's `readFromSubArray`, so the bytes around it can be
anything.

### Drilling in place

`.inPlace` can go anywhere in a chain. `JsoniterPrism.inPlace[A]` is
the root prism over a slice, and the in-place optics drill with the
same `.field(_.x)` / `.at(i)` / `.each` / `.x` surface, checked against
the schema by the same macros. Drill first and take `.inPlace` last,
or take it first and drill: it is the same optic.

```scala mdoc
val people = "...".getBytes("UTF-8") ++ aliceBytes
val alices = people.jsonSlice(3, aliceBytes.length)   // (offset, length)

JsoniterPrism.inPlace[Person].address.street.foldMap(identity[String])(alices)
JsoniterPrism[Person].address.street.inPlace.foldMap(identity[String])(alices)

val carts = JsonSlice("xx".getBytes("UTF-8") ++ cart, 2, cart.length)
itemsT.inPlace.foldMap(identity[Long])(carts)
```

### `.raw` in place: a view of the focused bytes

On the whole-array prism, `.raw` focuses the focused value's own JSON
bytes as a **copy**, an `Array[Byte]`. On an in-place prism it focuses a
`JsonSlice`: a **view** of those bytes where they lie in the source
array, nothing copied. That makes it the source of further in-place
optics, through ordinary `andThen`:

```scala mdoc
val addressJson = JsoniterPrism.inPlace[Person].field(_.address).raw
val zipOfAddress = JsoniterPrism.inPlace[Address].field(_.zip).field(_.code)

val zipCode = addressJson.andThen(zipOfAddress)   // one Optic[JsonSlice, JsonSlice, Int, Int, Affine]

zipCode.foldMap(identity[Int])(alices)
addressJson.headOption(alices).map(_.array eq people)  // the focus shares the source array
new String(zipCode.replace(54321)(alices).toArray, "UTF-8")
```

A raw focus is under the same rules as any slice (don't mutate the
array under it; `.toArray` to keep it). Writing through it splices the
new focus slice's bytes **verbatim** — no re-encoding and no
validation, as the whole-array `.raw` writes — into a fresh array, so
neither the source array nor the new focus's own array changes. A
composed write re-encodes only the leaf: the inner prism splices it
into a fresh copy of the focused span, and the outer one splices that
into a fresh copy of the document. The Optional laws hold for it, and
for the composed chain (pinned by `JsonSliceSpec`).

### Making slices

- `JsonSlice(bytes, offset, length)` — a view of a range;
  `JsonSlice(bytes)` — the whole array; `bytes.jsonSlice(offset,
  length)` — the same as a method on the array (`(offset, length)`, as
  `JsonSlice.apply` takes them). There is deliberately no implicit
  conversion from `Array[Byte]`: a slice shares its array, so taking
  one is spelled out.
- `JsonSlice(buf: java.nio.ByteBuffer)` — the bytes between the
  buffer's position and limit, leaving its position alone. A heap
  buffer with an accessible array is **not copied** (a view of
  `array()` from `arrayOffset() + position()`); a **direct** or
  **read-only** buffer has no array to share, so its bytes are
  **copied once**.

```scala mdoc
val heap = java.nio.ByteBuffer.wrap(buffer, other.length, sample.length)
JsonSlice(heap) == second
JsonSlice(heap).array eq buffer                       // a view
JsonSlice(heap.asReadOnlyBuffer()).array eq buffer    // a copy
```

Some ground rules:

- **Equality is by content**: two slices are equal when they hold the
  same bytes, wherever they sit. A slice reads and writes exactly like
  the same bytes copied out into an array of their own — the
  whole-array `JsoniterPrism` / `JsoniterTraversal` run the same scan
  and splice over `(0, length)` — and the laws (with their
  canonical-re-encoding caveat) hold on slices as they do on arrays.
- **A range outside the array fails fast**: `JsonSlice(bytes, offset,
  length)` (and `jsonSlice`) throws `IndexOutOfBoundsException` at
  construction when `offset` or `length` is negative or
  `offset + length > bytes.length` — the contract of
  `Arrays.copyOfRange`, `ByteBuffer.wrap` and `readFromSubArray`.
  Offsets are your arithmetic over an array you hold, so a wrong one is
  a bug at the call site; no optic ever runs on one.
- **Why a view type, not `(array, offset, length)` arguments**: an optic
  has one source type. An in-place prism is an ordinary
  `Optic[JsonSlice, JsonSlice, A, A, Affine]` (a `MultiFocus[PSVec]`
  traversal for `JsoniterTraversal`), so `CanGetOption[JsonSlice, A]`
  and the rest of the capability surface derive from it as from any
  other optic.

eo-jsoniter has no streaming dependency. From an fs2 `Chunk[Byte]`,
`JsonSlice(chunk.toByteBuffer)` reads it in place when the chunk is one
array slice, as a socket read is (fs2 wraps the slice's own array), and
copies it once otherwise. The
[stream triage demo](../cookbook/grpc-filter.md) judges every message
that way.

## JSONPath subset

The accepted grammar is deliberately small:

```
path  := '$' (step)*
step  := '.' ident | '[' int ']' | '[*]'
ident := [A-Za-z_][A-Za-z_0-9]*
int   := [0-9]+
```

So:

| Path                       | Meaning                                     |
|----------------------------|---------------------------------------------|
| `$`                        | The whole document                          |
| `$.foo.bar`                | Field `bar` of object `foo`                 |
| `$.items[0]`               | First element of `items` array              |
| `$.items[*]`               | Every element of `items` array (Traversal)  |
| `$.users[*].profile.email` | `email` of every user (Traversal)           |

Filter expressions (`?(@.x > 5)`), recursive descent (`..`), and
property wildcards (`.*`) are out of scope. For dynamic-shape paths
fall through to eo-circe.

## Path-walk on the wire

The
[`JsonPathScanner`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/main/scala/dev/constructive/eo/jsoniter/JsonPathScanner.scala)
is a hand-rolled byte walker (~325 LoC) that resolves a path to a
`(start, end)` byte span without parsing the document. It skips JSON
strings (with all backslash escapes), numbers, objects, arrays, and
literals; reports a Miss on the first structural mismatch. The
existential `X` on `JsoniterPrism` carries `(bytes, start, end)` —
just enough context for phase-2 splice writes to memcpy three slices
into a fresh buffer.

The `[*]` wildcard expands the current array to a `List[Span]` via
`JsonPathScanner.findAll`, then `JsoniterTraversal.to` decodes each
span into a single `Array[AnyRef]` and wraps it via
`PSVec.unsafeWrap`. One allocation per traversal, no per-element
cons-cell.

## Carrier reuse

This module ships zero new carriers. `JsoniterPrism` is an
`Optic[Array[Byte], Array[Byte], A, A, Affine]`; `JsoniterTraversal`
is an `Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]]`.
Their [in-place faces](#reading-in-place-from-a-larger-buffer) are the
same carriers over `JsonSlice` instead of `Array[Byte]`.
The standard cats-eo extensions on those carriers light up
automatically:

| On `Optic[..., Affine]`        | Lights up via                             |
|--------------------------------|-------------------------------------------|
| `.foldMap` / `.headOption`     | `ForgetfulFold[Affine]`                   |
| `.modify` / `.replace`         | `ForgetfulFunctor[Affine]`                |
| `.modifyA` / `.all`            | `ForgetfulTraverse[Affine, Applicative]`  |
| `.andThen` (same-carrier)      | `AssociativeFunctor[Affine, _, _]`        |

| On `Optic[..., MultiFocus[PSVec]]` | Lights up via                       |
|------------------------------------|-------------------------------------|
| `.foldMap` / `.length` / `.exists` | `mfFold[PSVec]`                     |
| `.modify` / `.replace`             | `mfFunctor[PSVec]`                  |
| `.andThen` (same-carrier)          | `mfAssocPSVec`                      |

So `JsoniterTraversal[A].andThen(Traversal.each[List, A])` — chaining
a JSON-byte traversal with the classical Scala-collection traversal
— composes through the standard `Optic.andThen` with zero-copy
per-element reassembly, no Composer hop required.

## When to reach for which

| Task                                                  | Use                       |
|-------------------------------------------------------|---------------------------|
| Read one scalar from a JSON byte buffer, no AST       | `JsoniterPrism.fromPath[A]("$.path")` (`Either`)  |
| Same, compile-time checked against the case-class schema | `JsoniterPrism[S].field(_.x)` / `JsoniterPrism[S].x` |
| Decode / encode the whole document (`Prism[Array[Byte], A]`) | `JsoniterPrism[A]`                  |
| Replace a `JsonCodecMaker` model with bytes + optics  | [migration recipe](#migrating-a-jsoncodecmaker-model-optics-as-evidence) |
| Write one scalar back into a JSON byte buffer         | `JsoniterPrism.fromPath[A]("$.path")` → `.replace(b)(bytes)` |
| Sum / count over an array on the wire                 | `JsoniterTraversal.fromPath[A]("$.path[*]")` → `.foldMap(...)` |
| Read a document in place from a larger buffer (no copy) | `JsoniterPrism.inPlace[A]…`, `prism.inPlace`, `traversal.inPlace` over `JsonSlice(…)` |
| Drill into dynamic shapes (no codec for surrounding)  | eo-circe `codecPrism[…]` — different module |
| Edit deeply through `[*]` on the wire                 | Phase-3 (not yet shipped); fall through to eo-circe today |
| Chain a JSON traversal with a Scala collection traversal | `JsoniterTraversal[A].andThen(Traversal.each[F, A])` |

For eo-jsoniter in a streaming service, see the
[allocation-free stream triage demo](../cookbook/grpc-filter.md): an fs2 filter that
classifies and filters 64-field messages straight off a socket, reading three
values per message instead of decoding it.

For the full failure-mode matrix (decode failures silently dropping
to Miss, malformed JSON, length-changing splice mechanics) see the
[`JsoniterPrismSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterPrismSpec.scala),
[`JsoniterPrismWriteSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterPrismWriteSpec.scala),
and
[`JsoniterTraversalSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterTraversalSpec.scala).
