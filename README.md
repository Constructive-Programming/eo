# cats-eo

[![Build](https://github.com/Constructive-Programming/eo/actions/workflows/ci.yml/badge.svg)](https://github.com/Constructive-Programming/eo/actions)
[![Maven Central](https://img.shields.io/maven-central/v/dev.constructive/cats-eo_3.svg)](https://central.sonatype.com/artifact/dev.constructive/cats-eo_3)
[![Scaladoc](https://javadoc.io/badge2/dev.constructive/cats-eo_3/scaladoc.svg)](https://javadoc.io/doc/dev.constructive/cats-eo_3)

An **existential optics** library for Scala 3, built on
[cats](https://typelevel.org/cats/).

One `Optic[S, T, A, B, F]` trait, parameterised over a carrier `F[_, _]`,
unifies every optic family. Composition crosses families through
`Composer[F, G]` bridges rather than N² hand-written `.andThen` overloads.

## Install

> `dev.constructive` namespace registration on Sonatype Central
> Portal is in flight; the artifacts land there on the 0.1.0 tag
> push. Until then, `sbt +publishLocal` produces a local-ivy
> snapshot you can depend on.

```scala
// build.sbt
libraryDependencies ++= Seq(
  "dev.constructive" %% "cats-eo"          % "0.1.0",
  "dev.constructive" %% "cats-eo-generics" % "0.1.0",             // optional: macro-derived optics
  "dev.constructive" %% "cats-eo-circe"    % "0.1.0",             // optional: JSON optics
  "dev.constructive" %% "cats-eo-laws"     % "0.1.0" % Test,      // discipline law sets
)
```

Requires Scala 3.8.x on JDK 17 or JDK 21.

## 60-second example

```scala
import eo.optics.Optic.*
import eo.generics.lens

case class Address(street: String, zip: Int)
case class Person(name: String, address: Address)

val street = lens[Person](_.address).andThen(lens[Address](_.street))
// Optic[Person, Person, String, String, Tuple2]

val alice = Person("Alice", Address("Main St", 12345))
street.get(alice)                       // "Main St"
street.replace("Broadway")(alice)       // Person("Alice", Address("Broadway", 12345))
street.modify(_.toUpperCase)(alice)     // Person("Alice", Address("MAIN ST", 12345))
```

No `.copy` chains, no setter lambdas, no `GenLens` boilerplate. The `lens`
macro works on plain case classes, Scala 3 enums, and union types alike.

## Optics at a glance

| Type        | Carrier                    | Shape                             | Typical use                                   |
|-------------|----------------------------|-----------------------------------|-----------------------------------------------|
| `Lens`      | `Tuple2`                   | one focus, always present         | product-type field access                     |
| `Prism`     | `Either`                   | one focus, may be absent          | sum-type branch, refinement                   |
| `Iso`       | `Forgetful`                | one focus, bijective              | data-shape conversion                         |
| `Optional`  | `Affine`                   | one focus, conditionally present  | partial field, predicate-gated access         |
| `Setter`    | `SetterF`                  | modify only                       | write-through without reading                 |
| `Getter`    | `Forgetful`                | read only                         | projection, derived view                      |
| `Fold`      | `Forget[F]`                | N foci, summarised                | `foldMap` across a container                  |
| `Traversal` | `Forget` / `PowerSeries`   | N foci, each modified             | map over every element, composed downstream   |

Every optic carries a discipline-checked law set in `cats-eo-laws`, so
downstream projects can `checkAll` custom instances the same way they do
for cats typeclasses.

## Modules

| Module               | Purpose                                                                                |
|----------------------|----------------------------------------------------------------------------------------|
| `cats-eo`            | Core optics, carriers, typeclasses                                                     |
| `cats-eo-laws`       | Discipline `FooTests` rule-sets, reusable by downstream projects                       |
| `cats-eo-generics`   | `lens[S](_.field)` / `prism[S, A]` macros built on [hearth](https://github.com/MateuszKubuszok/hearth) |
| `cats-eo-circe`      | `JsonPrism` / `JsonTraversal` — cursor-backed JSON optics, ~2× faster than decode → modify → re-encode |

## A few more examples

### Prism on an enum — no `.copy` required

```scala
import eo.optics.Optic.*
import eo.generics.prism

enum Shape:
  case Circle(r: Double)
  case Square(s: Double)

val circleP = prism[Shape, Shape.Circle]

circleP.to(Shape.Circle(1.0))                       // Right(Circle(1.0))
circleP.to(Shape.Square(2.0))                       // Left(Square(2.0))

// modify acts only on the Circle branch; Squares pass through unchanged.
circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Circle(1.0))  // Circle(2.0)
circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Square(2.0))  // Square(2.0)
```

### `Lens ∘ Optional` — partial focus of a field

```scala
import eo.data.Affine
import eo.optics.Optional
import eo.optics.Optic.*
import eo.generics.lens

// Focus the street only when it starts with "M":
val mainOnly = Optional[Address, Address, String, String, Affine](
  getOrModify = a => Either.cond(a.street.startsWith("M"), a.street, a),
  reverseGet  = { case (a, s) => a.copy(street = s) },
)

val streetIfMain =
  lens[Person](_.address).andThen(mainOnly)

streetIfMain.modify(_.toUpperCase)(alice)
// Uppercases only when address.street starts with "M".
```

### JSON path editing without full decode

```scala
import eo.circe.codecPrism

val street = codecPrism[Person].address.street       // JsonPrism[String]
val names  = codecPrism[Basket].items.each.name      // JsonTraversal[String]

street.modify(_.toUpperCase)(personJson)             // rewrites only address.street
names.modify(_.toUpperCase)(basketJson)              // rewrites every items[*].name
```

Benchmark: [`JsonTraversalBench`](./benchmarks/src/main/scala/eo/bench/JsonTraversalBench.scala)
shows the cursor-backed traversal running ~2× faster than the
`decode → map → re-encode` baseline across array sizes 8 / 64 / 512.

## Benchmarks

JMH harness in [`benchmarks/`](./benchmarks/), paired side-by-side with
[Monocle](https://www.optics.dev/Monocle/) where a direct equivalent exists
(Lens, Prism, Iso, Traversal, Optional, Getter, Setter, Fold). EO-only for
`PowerSeriesBench`, `JsonPrismBench`, `JsonPrismWideBench`, and
`JsonTraversalBench`.

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1"
```

Full run instructions, composition notes, and the JMH caveats live in
[`benchmarks/README.md`](./benchmarks/README.md).

## Documentation

Full mdoc-compiled site: <https://cats-eo-docs.pages.dev>
(hosted on Cloudflare Pages, deployed on every push to `main`
via [`deploy-site.yml`](./.github/workflows/deploy-site.yml);
setup notes in [`docs/cloudflare-pages-setup.md`](./docs/cloudflare-pages-setup.md)).
Markdown sources live under [`site/docs/`](./site/docs/) — every code
fence is verified against the library by `sbt docs/mdoc`.

Scaladoc is published to javadoc.io once an artifact lands on
Maven Central; until then run `sbt doc` locally. Architecture
context lives in [`CLAUDE.md`](./CLAUDE.md); the open release plan is
in [`docs/plans/`](./docs/plans/).

## Development

```sh
sbt compile                                               # full build
sbt test                                                  # core + tests + generics + circe
sbt "~compile"                                            # watch compile
scalafmt && scalafmt --check                              # format + verify
sbt "clean; coverage; tests/test; coverageReport"         # run with statement coverage
```

Benchmarks are **outside** the root aggregator — `sbt compile` and
`sbt test` skip them. Run them explicitly per
[`benchmarks/README.md`](./benchmarks/README.md).

## Contributing

Contributions welcome. A `CONTRIBUTING.md` lands with the 0.1.0 release; in
the meantime, the sbt / scalafmt / scalafix commands above are the minimum
CI-clean PR bar. Open an issue first for substantial design changes.

## License

Apache License 2.0. See [`LICENSE`](./LICENSE).
