# jsoniter integration

The `cats-eo-jsoniter` module adds two cursor-backed optics for
editing JSON byte streams without ever materialising an AST. They
sit on top of [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala)
and reuse the existing cats-eo carriers — no new carrier ships in
this module.

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
[`JsoniterBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsoniterBench.scala)
suite — see [benchmarks → JsoniterBench](benchmarks.md) for the full
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
`JsonValueCodec[A]` in scope:

```scala mdoc:silent
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.Optic.*

given JsonValueCodec[Long]   = JsonCodecMaker.make
given JsonValueCodec[String] = JsonCodecMaker.make

val sample: Array[Byte] =
  """{"payload":{"user":{"id":42,"email":"alice@example.com"}}}""".getBytes("UTF-8")

val idP    = JsoniterPrism[Long]("$.payload.user.id")
val emailP = JsoniterPrism[String]("$.payload.user.email")
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
val absentP = JsoniterPrism[Long]("$.payload.user.absent")
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

## JsoniterTraversal

`JsoniterTraversal[A]` accepts wildcard paths (`[*]`) — fans out over
every element of the focused array. Carrier is `MultiFocus[PSVec]`,
the same carrier `Traversal.each` uses, so the surface composes
uniformly with the rest of cats-eo's traversal machinery.

```scala mdoc:silent
import cats.instances.int.given

import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.jsoniter.JsoniterTraversal

val cart: Array[Byte] =
  """{"cart":{"items":[1,2,3,4,5,6,7,8,9,10]}}""".getBytes("UTF-8")

val itemsT = JsoniterTraversal[Long]("$.cart.items[*]")
```

```scala mdoc
itemsT.foldMap(identity[Long])(cart)            // sum, via Monoid[Long]
itemsT.foldMap(_ => 1)(cart)                    // count, via Monoid[Int]
```

Spans whose decode throws are silently dropped — `foldMap` reads the
focuses that exist and ignores the rest. `JsoniterPrism` rejects
wildcard paths at construction with an explicit redirect to this
factory.

Phase-1.5 ships read-only Traversal. Write-back over `[*]` is a phase-3
question (each splice would invalidate subsequent spans, so a naive
loop is wrong) — punt unless a real workload demands it.

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
is a hand-rolled byte walker (~280 LoC) that resolves a path to a
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
| Read one scalar from a JSON byte buffer, no AST       | `JsoniterPrism[A]("$.path")`            |
| Write one scalar back into a JSON byte buffer         | `JsoniterPrism[A].replace(b)(bytes)`    |
| Sum / count over an array on the wire                 | `JsoniterTraversal[A]("$.path[*]").foldMap(...)` |
| Drill into dynamic shapes (no codec for surrounding)  | eo-circe `codecPrism[…]` — different module |
| Edit deeply through `[*]` on the wire                 | Phase-3 (not yet shipped); fall through to eo-circe today |
| Chain a JSON traversal with a Scala collection traversal | `JsoniterTraversal[A].andThen(Traversal.each[F, A])` |

For the full failure-mode matrix (decode failures silently dropping
to Miss, malformed JSON, length-changing splice mechanics) see the
[`JsoniterPrismSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterPrismSpec.scala),
[`JsoniterPrismWriteSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterPrismWriteSpec.scala),
and
[`JsoniterTraversalSpec`](https://github.com/Constructive-Programming/eo/blob/main/jsoniter/src/test/scala/dev/constructive/eo/jsoniter/JsoniterTraversalSpec.scala).
