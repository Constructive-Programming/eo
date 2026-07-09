# cats-eo

An **existential optics** library for Scala 3, built on
[cats](https://typelevel.org/cats/).

One `Optic[S, T, A, B, F]` trait, parameterised over a carrier
`F[_, _]`, unifies every optic family. Composition crosses
families through `Composer[F, G]` bridges rather than N² hand-
written `.andThen` overloads.

## What optics buy you

An optic is a tool for cleanly specifying a complex, pinpointed
operation against a value — at a much higher level of interpretation
than the operation itself. You name *which* part of a value you care
about; the carrier names *how* that focus is reached and rebuilt;
your action against the focus stays a one-liner regardless of how
deep the structure goes or how heterogeneously the layers are
encoded.

The Lens / Prism / Traversal vocabulary you'd normally use for
in-memory case-class trees is the same vocabulary that lights up
when one side of the structure is a JSON byte stream
([eo-jsoniter](integrations/jsoniter.md)) or an Apache Avro record on the wire
([eo-avro](integrations/avro.md)) or a circe `Json` AST ([eo-circe](integrations/circe.md)).
You get to specify one side of the mirror — the focus, the
operation, the path — and let the carrier implement the other.
On one side: the bytestream, the wire, the buffered representation.
On the other: your domain classes. Same `.modify` / `.replace` /
`.andThen` reads against both.

Current version: @VERSION@.

## 60-second example

```scala mdoc:silent
import dev.constructive.eo.CanModify
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens
import dev.constructive.eo.docs.{Address, Person, Zip}

val street =
  lens[Person](_.address)
    .andThen(lens[Address](_.street))
```

```scala mdoc
val alice = Person("Alice", Address("Main St", Zip(12345, "6789")))

street.get(alice)
street.replace("Broadway")(alice)
```

No `.copy` chains, no setter lambdas, no `GenLens` boilerplate. The
`lens` macro works on plain case classes, Scala 3 enums, and union
types alike.

And the part that scales beyond one call site: a function can demand
the *capability* instead of the type. `shout` below knows nothing
about `Person` or `Address` — it asks for proof that a `String` can
be rewritten inside `T`, and any optic with that power is the proof:

```scala mdoc
def shout[T](using cm: CanModify[T, String]): T => T =
  cm.modify(_.toUpperCase)

shout(using lens[Person](_.name))(alice)
shout(using lens[Address](_.street))(alice.address)
```

One generic function, two unrelated types — the optic passed at each
call site is the entire coupling surface. That contract style is the
library's core habit: [Capabilities](capabilities.md).

## Keep reading

- [Getting started](getting-started.md) — install + the 60-second
  tour expanded.
- [Capabilities](capabilities.md) — `CanGet` / `CanModify` /
  `CanFold` & co: consume optics as `using` evidence and keep
  modules decoupled; the availability matrix and coherence rules.
- [Concepts](concepts.md) — what an Optic *is*, what a carrier is,
  and how `Composer` bridges family boundaries.
- [Optics reference](optics.md) — one section per family, with
  runnable examples and the compiler-pinned 11-family
  [composition matrix](optics.md#composition-matrix).
- [MultiFocus](multifocus.md) — the unified successor of five v1
  carriers (`AlgLens[F]`, `Kaleidoscope`, `Grate`, `PowerSeries`,
  `FixedTraversal[N]`); typeclass-gated capability matrix and
  composability profile.
- [Generics](generics.md) — the `lens[S](_.field)` and
  `prism[S, A]` macros, backed by Hearth.
- [Circe integration](integrations/circe.md) — `JsonPrism` / `JsonTraversal`,
  cursor-backed navigation into circe `Json` with no full decode.
- [Avro integration](integrations/avro.md) — `AvroPrism` / `AvroTraversal`,
  cursor-backed navigation into Apache Avro `IndexedRecord` with no
  full decode; binary + JSON wire-format input dual.
- [Jsoniter integration](integrations/jsoniter.md) — `JsoniterPrism` /
  `JsoniterTraversal`, byte-cursor navigation directly into
  `Array[Byte]` JSON via jsoniter-scala codecs. Read at
  ~50 ns/op (16× eo-circe), write via splice at ~100 ns/op (14×
  eo-circe).
- [Extensibility](extensibility.md) — how to ship a custom optic
  tuned for your domain's hot path without losing the rest of the
  cats-eo universe.
- [Cookbook](cookbook.md) — common patterns: option fields,
  composed Lens/Optional, multi-focus modify, JSON path edits, and
  `Plated` + `everywhere` — one optic applied at every depth of a
  tree, AST, or JSON document.
- [Migrating from Monocle](migration-from-monocle.md) — a
  translation table + where EO diverges.
