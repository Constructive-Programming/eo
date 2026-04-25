# Getting started

## Install

Add the module(s) you need to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "dev.constructive" %% "cats-eo"          % "@VERSION@",
  "dev.constructive" %% "cats-eo-generics" % "@VERSION@",  // optional: macro-derived optics
  "dev.constructive" %% "cats-eo-circe"    % "@VERSION@",  // optional: JSON optics
  "dev.constructive" %% "cats-eo-laws"     % "@VERSION@" % Test,
)
```

Requires Scala 3.8.x on JDK 17 or JDK 21.

## First example

```scala mdoc:silent
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens
import dev.constructive.eo.docs.{Address, Person, Zip}
```

A Lens focuses a single field of a product:

```scala mdoc
val nameL = lens[Person](_.name)

val alice = Person("Alice", Address("Main St", Zip(12345, "6789")))

nameL.get(alice)
nameL.modify(_.toUpperCase)(alice)
```

Compose two lenses with `.andThen` — the result is another Lens
into the composite path:

```scala mdoc
val streetL =
  lens[Person](_.address)
    .andThen(lens[Address](_.street))

streetL.get(alice)
streetL.replace("Broadway")(alice)
streetL.modify(_.toUpperCase)(alice)
```

No `.copy` chains; the `lens` macro generates the setter for you.

## What next?

- If you want to understand what a *carrier* is and why one trait
  backs every optic family, read [Concepts](concepts.md).
- If you want the per-family tour with the kind of optic you'd
  reach for in each situation, read [Optics reference](optics.md).
- If you're coming from Monocle, jump to
  [Migrating from Monocle](migration-from-monocle.md).

## Development commands

For contributors working on the library itself:

```sh
sbt compile                                          # full build
sbt test                                             # core + tests + generics + circe
sbt "~compile"                                       # watch compile
sbt docs/mdoc                                        # compile site's code fences
sbt docs/tlSitePreview                               # preview the site locally
scalafmt && scalafmt --check                         # format + verify
sbt "clean; coverage; tests/test; coverageReport"    # coverage report
```

Benchmarks live outside the root aggregator — see
[`benchmarks/README.md`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md)
for run instructions.
