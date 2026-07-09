# Getting started

Don't worry about an abstraction tax: cats-eo turns deep, awkward read/modify code into a readable one-liner *and* runs at hand-written speed — matching or beating Monocle on the single-optic hot paths ([benchmarks](benchmarks.md)).

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

## Define

```scala mdoc:silent
import dev.constructive.eo.CanModify
import dev.constructive.eo.optics.*
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens
import dev.constructive.eo.docs.{Address, Person, Zip}
```

A Lens focuses a single field of a product:

```scala mdoc
val nameL = lens[Person](_.name)

val alice = Person("ms. alice liddell, esq.", Address("main st.", Zip(12345, "6789")))

nameL.get(alice)
```

A rewrite goes through the same lens. The rule is a plain
`String => String` — a text-utilities function that has never heard
of `Person`: split into words, strip symbols other than `'.'`,
shout the short words, title-case the rest:

```scala mdoc
def emphasize(s: String): String =
  s.split(" ")
    .map(_.filter(c => c.isLetterOrDigit || c == '.'))
    .map(w => if w.length < 4 then w.toUpperCase else w.capitalize)
    .mkString(" ")

nameL.modify(emphasize)(alice)
```

Compose two lenses with `.andThen` — the result is another Lens
into the composite path:

```scala mdoc
val streetL =
  lens[Person](_.address)
    .andThen(lens[Address](_.street))

streetL.get(alice)
streetL.replace("Broadway")(alice)
streetL.modify(emphasize)(alice)
```

No `.copy` chains; the `lens` macro generates the setter for you.

## Call

`nameL.modify(emphasize)(alice)` works, but the line couples three
decisions: the rule (`emphasize`), the shape (`Person`), and the
field (`name`). Move the rule behind a **capability** and the shape
and field become the caller's business:

```scala mdoc
def emphasized[T](using cm: CanModify[T, String]): T => T =
  cm.modify(emphasize)
```

The signature is the contract: "give me proof a String can be
rewritten inside `T`, and I'll return the rewrite." Any optic that
can modify — a lens, a composed path, a prism into one branch — is
that proof. A concrete lens is passed straight in; a *composed*
optic like `streetL` is bound as a `given` instead, and the
capability derives from it on the spot:

```scala mdoc
emphasized(using nameL)(alice)

given Optic[Person, Person, String, String, Tuple2] = streetL
emphasized[Person](alice)
```

This is the library's core habit: use capabilities to write truly
modular, de-coupled code by passing optics along as proof of
capability contracts across modules. Deep dive:
[Capabilities](capabilities.md).

## Learn More

- If you want to understand what a *carrier* is and why one trait
  backs every optic family, read [Concepts](concepts.md).
- If the capability contract above is the part that hooked you,
  [Capabilities](capabilities.md) has the full matrix and the
  coherence rules.
- If you want the per-family tour with the kind of optic you'd
  reach for in each situation, read [Optics reference](optics.md).
- If you're coming from Monocle, jump to
  [Migrating from Monocle](migration-from-monocle.md).

## Contribute

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
