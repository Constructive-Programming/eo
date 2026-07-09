# cats-eo

An **existential optics** library for Scala 3, built on
[cats](https://typelevel.org/cats/).

One `Optic[S, T, A, B, F]` trait, parameterised over a carrier
`F[_, _]`, unifies every optic family. Composition crosses
families through `Composer[F, G]` bridges rather than N² hand-
written `.andThen` overloads.

## Stacking Benefits

What an optic buys you depends on the scale you're working at — and
it compounds.

*At the value level*: a pinpointed operation, cleanly specified.
Name the part you care about and act on it in one line — no `.copy`
chains, no cursor plumbing, no depth tax.

*At the format level*: the same one line, regardless of encoding.
Lens / Prism / Traversal read identically against in-memory trees,
circe `Json` ([eo-circe](integrations/circe.md)), Avro on the wire
([eo-avro](integrations/avro.md)), and raw JSON bytes
([eo-jsoniter](integrations/jsoniter.md)) — where the byte-level
edit is *flat* in document size and decode-modify-encode is
on-demand.

*At the architecture level*: the optic becomes a contract. A module
demands `CanModify[T, Instant]` — proof that doesn't anchor `T` —
and the caller supplies the optic relevant to their particular
context. Truly modular, de-coupled code: optics passed along as
proof of capability contracts across modules
([Capabilities](capabilities.md)).

The whole ladder runs at hand-written speed — fused hot paths match
or beat what's currently done on large systems, and capability calls
dispatch into the same fused methods ([Benchmarks](benchmarks.md)).

Current version: @VERSION@.

## Ergonomics

```scala mdoc:silent
import dev.constructive.eo.CanModify
import dev.constructive.eo.generics.lens
import dev.constructive.eo.docs.{Address, Person, Zip}

val personName    = lens[Person](_.name)
val personAddress = lens[Person](_.address)
val addressStreet = lens[Address](_.street)

val street = personAddress.andThen(addressStreet)
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
def shout[T](cm: CanModify[T, String]): T => T =
  cm.modify(_.toUpperCase)

shout(personName)(alice)
shout(street)(alice)
```

One generic function, two different paths — a plain lens or a
composed one, handed over as the proof. The optic passed at the call
site is the entire coupling surface; that contract style is the
library's core habit: [Capabilities](capabilities.md).

## Further Reading

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
