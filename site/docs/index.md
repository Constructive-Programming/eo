# cats-eo

An **existential optics** library for Scala 3, built on
[cats](https://typelevel.org/cats/).

One `Optic[S, T, A, B, F]` trait, parameterised over a carrier
`F[_, _]`, unifies every optic family. Composition crosses
families through `Composer[F, G]` bridges rather than N² hand-
written `.andThen` overloads.

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
- [Generics](generics.md) — the `lens[S](_.field)` and
  `prism[S, A]` macros, backed by Hearth.
- [Circe integration](circe.md) — `JsonPrism` / `JsonTraversal`,
  cursor-backed navigation into circe `Json` with no full decode.
- [Extensibility](extensibility.md) — how to ship a custom optic
  tuned for your domain's hot path without losing the rest of the
  cats-eo universe.
- [Cookbook](cookbook.md) — common patterns: option fields,
  composed Lens/Optional, multi-focus modify, JSON path edits.
- [Migrating from Monocle](migration-from-monocle.md) — a
  translation table + where EO diverges.
