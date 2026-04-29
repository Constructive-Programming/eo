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
([eo-jsoniter](jsoniter.md)) or an Apache Avro record on the wire
([eo-avro](avro.md)) or a circe `Json` AST ([eo-circe](circe.md)).
You get to specify one side of the mirror — the focus, the
operation, the path — and let the carrier implement the other.
On one side: the bytestream, the wire, the buffered representation.
On the other: your domain classes. Same `.modify` / `.replace` /
`.andThen` reads against both.

Current version: @VERSION@.

## 60-second example

```scala mdoc:silent
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
street.modify(_.toUpperCase)(alice)
```

No `.copy` chains, no setter lambdas, no `GenLens` boilerplate. The
`lens` macro works on plain case classes, Scala 3 enums, and union
types alike.

## Keep reading

- [Getting started](getting-started.md) — install + the 60-second
  tour expanded.
- [Concepts](concepts.md) — what an Optic *is*, what a carrier is,
  and how `Composer` bridges family boundaries.
- [Optics reference](optics.md) — one section per family, with
  runnable examples.
- [MultiFocus](multifocus.md) — the unified successor of five v1
  carriers (`AlgLens[F]`, `Kaleidoscope`, `Grate`, `PowerSeries`,
  `FixedTraversal[N]`); typeclass-gated capability matrix and
  composability profile.
- [Generics](generics.md) — the `lens[S](_.field)` and
  `prism[S, A]` macros, backed by Hearth.
- [Circe integration](circe.md) — `JsonPrism` / `JsonTraversal`,
  cursor-backed navigation into circe `Json` with no full decode.
- [Avro integration](avro.md) — `AvroPrism` / `AvroTraversal`,
  cursor-backed navigation into Apache Avro `IndexedRecord` with no
  full decode; binary + JSON wire-format input dual.
- [Jsoniter integration](jsoniter.md) — `JsoniterPrism` /
  `JsoniterTraversal`, byte-cursor navigation directly into
  `Array[Byte]` JSON via jsoniter-scala codecs. Read at
  ~50 ns/op (16× eo-circe), write via splice at ~100 ns/op (14×
  eo-circe).
- [Extensibility](extensibility.md) — how to ship a custom optic
  tuned for your domain's hot path without losing the rest of the
  cats-eo universe.
- [Cookbook](cookbook.md) — common patterns: option fields,
  composed Lens/Optional, multi-focus modify, JSON path edits.
- [Migrating from Monocle](migration-from-monocle.md) — a
  translation table + where EO diverges.
