# cats-eo

[![CI](https://github.com/Constructive-Programming/eo/actions/workflows/ci.yml/badge.svg)](https://github.com/Constructive-Programming/eo/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.constructive/cats-eo_3.svg)](https://central.sonatype.com/artifact/dev.constructive/cats-eo_3)
[![Scaladoc](https://javadoc.io/badge2/dev.constructive/cats-eo_3/javadoc.svg)](https://javadoc.io/doc/dev.constructive/cats-eo_3)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

`cats-eo` is an existential optics library for Scala 3, layered on
[cats](https://typelevel.org/cats/). One `Optic[S, T, A, B, F]` trait
parameterised over a carrier `F[_, _]` unifies every optic family;
cross-family composition goes through `Composer[F, G]` bridges instead
of N&sup2; hand-written `.andThen` overloads.

## Install

```scala
libraryDependencies += "dev.constructive" %% "cats-eo" % "0.1.0"
// Optional submodules:
libraryDependencies += "dev.constructive" %% "cats-eo-laws"     % "0.1.0" % Test
libraryDependencies += "dev.constructive" %% "cats-eo-generics" % "0.1.0"
```

Requires Scala 3.8.x on JDK 17 or JDK 21.

## 60-second tour

```scala
import dev.constructive.eo.optics.Lens
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens

case class Address(street: String, zip: Int)
case class Person(name: String, address: Address)

// Hand-written Lens:
val streetL = Lens[Address, String](_.street, (a, s) => a.copy(street = s))

// Macro-derived Lens, composed across two case classes:
val personStreet = lens[Person](_.address).andThen(streetL)

val alice = Person("Alice", Address("Main St", 12345))
personStreet.get(alice)                       // "Main St"
personStreet.replace("Broadway")(alice)       // address.street := "Broadway"
personStreet.modify(_.toUpperCase)(alice)     // address.street := "MAIN ST"
```

## What's in cats-eo

- [`Iso`](https://cats-eo.constructive.dev/optics.html#iso) — a bijective
  one-focus optic; the carrier is `Forgetful`.
- [`Lens`](https://cats-eo.constructive.dev/optics.html#lens) — a
  one-focus optic into an always-present field of a product.
- [`Prism`](https://cats-eo.constructive.dev/optics.html#prism) — a
  one-focus optic into a sum-type branch.
- [`Optional`](https://cats-eo.constructive.dev/optics.html#optional) — a
  one-focus optic where the focus may be absent.
- [`AffineFold`](https://cats-eo.constructive.dev/optics.html#affinefold) —
  a read-only `Optional`; partial projection without a write side.
- [`Getter`](https://cats-eo.constructive.dev/optics.html#getter) — a
  read-only one-focus projection.
- [`Setter`](https://cats-eo.constructive.dev/optics.html#setter) — a
  write-only optic; modify without observing.
- [`Fold`](https://cats-eo.constructive.dev/optics.html#fold) — N foci
  summarised through a `Monoid`.
- [`Traversal`](https://cats-eo.constructive.dev/optics.html#traversal) —
  N foci, each individually modifiable; `.each` and `.forEach` flavours.
- [`AlgLens`](https://cats-eo.constructive.dev/optics.html#alglens) —
  classifier-shaped update where the whole `F[A]` is visible (adaptive
  KNN, one-vs-rest).
- [`Grate`](https://cats-eo.constructive.dev/optics.html#grate) — the
  dual of `Lens`; lifts a function on the focus to a function on the
  whole structure.
- [`Kaleidoscope`](https://cats-eo.constructive.dev/optics.html#kaleidoscope) —
  applicative-aware aggregation; folds a structure under any
  `Apply` carrier.
- [`Review`](https://cats-eo.constructive.dev/optics.html#review) — the
  reverse-only half of a `Prism`; build, never observe.
- [`JsonPrism`](https://cats-eo.constructive.dev/circe.html#jsonprism) —
  cursor-backed JSON optic with observable-by-default `Ior` failures.
- [`JsonFieldsPrism`](https://cats-eo.constructive.dev/circe.html#multi-field-focus----fields-_a-_b) —
  multi-field flavour of `JsonPrism`.
- [`JsonTraversal`](https://cats-eo.constructive.dev/circe.html#jsontraversal-each) —
  `.each` traversal across JSON arrays.
- [`JsonFieldsTraversal`](https://cats-eo.constructive.dev/circe.html#multi-field-focus----fields-_a-_b) —
  multi-field flavour of `JsonTraversal`.

Every optic ships a discipline-checked law set in `cats-eo-laws`, so
downstream projects can `checkAll` custom instances the same way they do
for cats typeclasses.

## Where to go next

- Getting started: <https://cats-eo.constructive.dev/getting-started.html>
- Macro-derived optics (`generics`): <https://cats-eo.constructive.dev/generics.html>
- Cookbook (recipes): <https://cats-eo.constructive.dev/cookbook.html>
- Composition gap analysis (research):
  [`docs/research/2026-04-23-composition-gap-analysis.md`](./docs/research/2026-04-23-composition-gap-analysis.md)

## Project status

`0.1.0` is the first public release. Binary compatibility is guaranteed
within the `0.1.x` series via MiMa starting at `0.1.1` (the `0.1.0`
publish has no prior version to compare against). Pre-`1.0`, expect
surface refinements as we learn from real users; each non-bugfix release
will list breaking changes in [`CHANGELOG.md`](./CHANGELOG.md).

## License

Apache License 2.0. See [`LICENSE`](./LICENSE).

## Acknowledgements

- [cats](https://typelevel.org/cats/) — the typeclass foundation every
  carrier instance plugs into.
- [Monocle](https://www.optics.dev/Monocle/) — the prior art the
  optic-family vocabulary descends from, and the benchmark baseline.
- [Hearth](https://github.com/MateuszKubuszok/hearth) — the macro-commons
  library that powers `cats-eo-generics`.
- [discipline](https://github.com/typelevel/discipline) — the rule-set
  harness behind `cats-eo-laws`.
