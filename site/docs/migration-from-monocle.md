# Migrating from Monocle

A side-by-side translation table for the common Monocle idioms,
plus a note on where EO diverges.

## Cheat sheet

| Monocle                                            | cats-eo                                             |
|----------------------------------------------------|-----------------------------------------------------|
| `Lens[S, A](get)(a => s => …)`                     | `Lens[S, A](get, (s, a) => …)`                      |
| `GenLens[S](_.field)`                              | `lens[S](_.field)` (from `eo.generics`)             |
| `Prism[S, A](_.some)(identity)`                    | `Prism.optional[S, A](_.some, identity)`            |
| `GenPrism[S, A]`                                   | `prism[S, A]` (from `eo.generics`)                  |
| `Iso[S, A](f)(g)`                                  | `Iso[S, S, A, A](f, g)`                             |
| `Optional[S, A](_.some)(a => s => …)`              | `Optional[S, S, A, A, Affine](getOrModify, rg)`     |
| `Setter[S, A](f => s => …)`                        | `Setter[S, S, A, A](f => s => …)`                   |
| `Fold.fromFoldable[List, Int]`                     | `Fold[List, Int]` (with `cats.instances.list.given`)|
| `Traversal.fromTraverse[List, Int]`                | `Traversal.each[List, Int, Int]`                    |
| `lens.andThen(otherLens)`                          | `lens.andThen(otherLens)` — same                    |
| `lens.andThen(optional)`                           | `lens.morph[Affine].andThen(optional)` — see below  |
| `traversal.andThen(lens)`                          | `traversal = Traversal.powerEach[…]; traversal.andThen(lens.morph[PowerSeries])` |
| `lens.get(s)`                                      | `lens.get(s)` — same                                |
| `lens.replace(a)(s)` / `lens.set(a)(s)`            | `lens.replace(a)(s)` — same                         |
| `lens.modify(f)(s)`                                | `lens.modify(f)(s)` — same                          |
| `prism.getOption(s)`                               | `prism.getOption(s)` — on the concrete returned class; `prism.to(s).toOption` through the generic trait |
| `prism.reverseGet(a)`                              | `prism.reverseGet(a)` — same                        |
| `optional.getOption(s)`                            | `opt.to(s).affine.toOption` — see below             |
| `traversal.modify(f)(xs)`                          | `traversal.modify(f)(xs)` — same                    |
| `fold.foldMap(f)(xs)`                              | `fold.foldMap(f)(xs)` — same                        |

## Where EO diverges

### Polymorphic constructors

Every EO family ships a monomorphic `Type[S, A]` constructor
and a polymorphic `pType[S, T, A, B]` counterpart. Monocle only
has the monomorphic forms on the top-level `Lens` / `Prism` /
`Iso` objects and exposes the polymorphic shapes through
`PLens` / `PPrism` / `PIso`.

### Cross-family composition requires `.morph`

Monocle's `andThen` has implicit overloads for every optic
pair. cats-eo keeps `Optic.andThen` same-carrier only and
requires an explicit `.morph[G]` to cross families:

```scala mdoc:silent
import eo.data.Affine
import eo.optics.{Lens, Optic}
import eo.optics.Optic.*
import eo.optics.Optional

case class MigConfig(timeout: Option[Int])
case class MigApp(config: MigConfig)

val timeoutOpt = Optional[MigConfig, MigConfig, Int, Int, Affine](
  getOrModify = c => c.timeout.toRight(c),
  reverseGet  = { case (c, t) => c.copy(timeout = Some(t)) },
)

val appConfig =
  Lens[MigApp, MigConfig](_.config, (a, c) => a.copy(config = c))

// Monocle: lens.andThen(optional) just works
// cats-eo: lift the lens into the Optional's carrier first
val appTimeout = appConfig.morph[Affine].andThen(timeoutOpt)
```

The payoff: composition is carrier-level (one `Composer` per
pair of carriers), not family-level (one `andThen` per pair of
optics). Adding a new optic family means supplying carrier
instances; the cross-family bridges come for free.

### Getter / Setter don't compose via `.andThen`

Getter's `T = Unit` and Setter's `SetterF` carrier have no
`AssociativeFunctor` instance. Compose a Lens chain (Tuple2
carrier) and reach for `.get` / `.modify` at the leaf
instead. See [Optics → Getter](optics.md#getter) for the
workaround.

### Traversal has two carriers

Monocle's `Traversal` is a single type with a single
`andThen` behaviour. cats-eo splits:

- `Traversal.each[F, A, B]` — carrier `Forget[F]`, linear
  time, no downstream composition.
- `Traversal.powerEach[F, A]` — carrier `PowerSeries`, composable
  with downstream optics, super-linear runtime.

Pick `each` unless the chain needs to continue past the
traversal. See the
[PowerSeries benchmark notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#interpreting-powerseries-numbers)
for the cost breakdown.

### JsonPrism has no Monocle equivalent

Monocle's `monocle-circe` module only provides a
`Prism[Json, A]` and deep optics through that Prism — it still
forces a full decode of the focused `A`. cats-eo's `JsonPrism`
/ `JsonTraversal` walk circe's `JsonObject` representation
directly, avoiding the intermediate Codec round-trip at every
level of the path. See [Circe integration](circe.md).

## Discipline law instances

Downstream projects can reuse the same `checkAll` pattern they
know from cats. `cats-eo-laws` ships the rule-sets:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-laws" % "@VERSION@" % Test
```

```scala
import eo.laws.discipline.LensTests

checkAll("Lens[Person, Int]", LensTests[Person, Int](ageL).lens)
```

Every public optic family has a matching `FooLaws` /
`FooTests` pair. See the `laws/src/main/scala/eo/laws/` tree
for the full list.
