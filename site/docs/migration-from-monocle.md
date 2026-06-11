# Migrating from Monocle

A side-by-side translation table for the common Monocle idioms,
plus a note on where EO diverges.

## Cheat sheet

| Monocle                                            | cats-eo                                             |
|----------------------------------------------------|-----------------------------------------------------|
| `Lens[S, A](get)(a => s => …)`                     | `Lens[S, A](get, (s, a) => …)`                      |
| `GenLens[S](_.field)`                              | `lens[S](_.field)` (from `dev.constructive.eo.generics`)             |
| `GenLens[S](_.a).andThen(GenLens[S](_.b)).andThen(...)` — N hand-composed GenLenses | `lens[S](_.a, _.b, ...)` — one varargs call; full-cover upgrades to `BijectionIso` automatically (no Monocle equivalent) |
| `Prism[S, A](_.some)(identity)`                    | `Prism.optional[S, A](_.some, identity)`            |
| `GenPrism[S, A]`                                   | `prism[S, A]` (from `dev.constructive.eo.generics`)                  |
| `Iso[S, A](f)(g)`                                  | `Iso[S, S, A, A](f, g)`                             |
| `Optional[S, A](_.some)(a => s => …)`              | `Optional[S, S, A, A, Affine](getOrModify, rg)`     |
| *(no standalone equivalent — Monocle reaches for `Optional.getOption`)* | `AffineFold(p => ...)` / `AffineFold.select(p)` / `AffineFold(optic.getOption)` for a read-only view of an Optional/Prism — read-only 0-or-1 focus, `T = Unit` forbids `.modify` |
| *(no direct equivalent — algebraic lenses + Kaleidoscopes are not in Monocle)* | `MultiFocus.fromLensF` / `fromPrismF` / `fromOptionalF` — classifier-shaped optic over `F[A]` focus, plus `.collectMap` / `.collectList` aggregation universals; see [Optics → MultiFocus](optics.md#multifocus) |
| `Setter[S, A](f => s => …)`                        | `Setter[S, S, A, A](f => s => …)`                   |
| *(no equivalent — build-only optics are not standalone citizens in Monocle)* | `Review[S, A](build)` (build-only, one focus) and `Unfold[T, B, F]` (build-only, many: `embed: F[B] => T` — recursion-scheme algebras, aggregation); see [Optics → Review](optics.md#review) / [Unfold](optics.md#unfold) |
| `Fold.fromFoldable[List, Int]`                     | `Fold[List, Int]` (with `cats.instances.list.given`)|
| `Traversal.fromTraverse[List, Int]`                | `Traversal.each[List, Int]` (`Traversal.pEach[List, Int, Int]` for the polymorphic-write variant) |
| `monocle.function.Plated[A]` + `transform` / `rewrite` / `universe` / `children` | `Plated[S]` — derive with `plate[S]` (from `dev.constructive.eo.generics`) or hand-write with `Plated.fromChildren`; same combinator names, plus `Plated.everywhere[S]` as a composable Setter (no Monocle equivalent) |
| `lens.andThen(otherLens)`                          | `lens.andThen(otherLens)` — same                    |
| `lens.andThen(optional)`                           | `lens.andThen(optional)` — cross-carrier `.andThen` lifts via `Composer[Tuple2, Affine]` |
| `traversal.andThen(lens)`                          | `traversal = Traversal.each[…]; traversal.andThen(lens)` — auto-morph via `Composer[Tuple2, PowerSeries]` |
| `lens.get(s)`                                      | `lens.get(s)` — same                                |
| `lens.replace(a)(s)` / `lens.set(a)(s)`            | `lens.replace(a)(s)` — same                         |
| `lens.modify(f)(s)`                                | `lens.modify(f)(s)` — same                          |
| `prism.getOption(s)`                               | `prism.getOption(s)` — on the concrete returned class; `prism.to(s).toOption` through the generic trait |
| `prism.reverseGet(a)`                              | `prism.reverseGet(a)` — same                        |
| `optional.getOption(s)`                            | `optional.getOption(s)` — generic `.getOption` extension on any `Optic[_, _, _, _, Affine]` (Optional and AffineFold both ship it) |
| `traversal.modify(f)(xs)`                          | `traversal.modify(f)(xs)` — same                    |
| `fold.foldMap(f)(xs)`                              | `fold.foldMap(f)(xs)` — same                        |

## Where EO diverges

### Polymorphic constructors

Every EO family ships a monomorphic `Type[S, A]` constructor
and a polymorphic `pType[S, T, A, B]` counterpart. Monocle only
has the monomorphic forms on the top-level `Lens` / `Prism` /
`Iso` objects and exposes the polymorphic shapes through
`PLens` / `PPrism` / `PIso`.

### Cross-family composition: `.andThen` auto-morphs

Monocle's `andThen` has implicit overloads for every optic
pair. cats-eo keeps `Optic.andThen` carrier-aware: same-carrier
composition goes through `AssociativeFunctor[F, X, Y]`, and
cross-carrier composition routes through a summoned
`Morph[F, G]` (which picks up a `Composer[F, G]` or
`Composer[G, F]`) to lift both sides under a shared carrier.
The upshot at the call site: the same `.andThen` works whether
the two optics share `F` or not:

```scala mdoc:silent
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Lens
import dev.constructive.eo.optics.Optional

case class MigConfig(timeout: Option[Int])
case class MigApp(config: MigConfig)

val timeoutOpt = Optional[MigConfig, MigConfig, Int, Int, Affine](
  getOrModify = c => c.timeout.toRight(c),
  reverseGet  = { case (c, t) => c.copy(timeout = Some(t)) },
)

val appConfig =
  Lens[MigApp, MigConfig](_.config, (a, c) => a.copy(config = c))

// Cross-carrier `.andThen` lifts the Lens into the Optional's
// carrier automatically via `Composer[Tuple2, Affine]`.
val appTimeout = appConfig.andThen(timeoutOpt)
```

The payoff: composition is carrier-level (one `Composer` per
pair of carriers), not family-level (one `andThen` per pair of
optics). Adding a new optic family means supplying carrier
instances; the cross-family bridges come for free.

### Getter / Setter compose by collapse, not by carrier

Getter's `T = Unit` and Setter's `SetterF` carrier share no
`AssociativeFunctor` instance — instead, a chain that touches a
read-only optic anywhere collapses to the read-only join
(`lens.andThen(getter)` → Getter, `prism.andThen(getter)` →
AffineFold, `traversal.andThen(getter)` → Fold), and a chain into
a Setter collapses the read side (`lens.andThen(setter)` →
Setter). `getter.andThen(setter)` itself is void by design — there
is nothing to write through. See the
[composition matrix](optics.md#composition-matrix) for every pair.

### Traversal carrier

cats-eo's `Traversal` is a single carrier:

- `Traversal.each[F, A]` / `pEach[F, A, B]` — carrier
  `MultiFocus[PSVec]`. Supports `.modify`, `.foldMap`, `.modifyA`,
  and `.andThen` with downstream optics. Pays a small constant-
  factor overhead over the naive map path; see the
  [PowerSeries benchmark notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#interpreting-powerseries-numbers)
  for the cost breakdown.

### JsonPrism has no Monocle equivalent

Monocle's `monocle-circe` module only provides a
`Prism[Json, A]` and deep optics through that Prism — it still
forces a full decode of the focused `A`. cats-eo's `JsonPrism`
/ `JsonTraversal` walk circe's `JsonObject` representation
directly, avoiding the intermediate Codec round-trip at every
level of the path. See [Circe integration](circe.md).

### Plated is stack-safe and composes as an optic

Monocle's `monocle.function.Plated` overflows the stack on deep
trees — `transform` and `universe` `StackOverflowError` on a
degenerate spine (see the [benchmarks](benchmarks.md)). cats-eo's
`Plated` clears a 100k-deep spine: `transform` / `everywhere` run on a
call-stack/heap-machine hybrid, the reads on a worklist, and `rewrite`
trampolines through `cats.Eval` (so even a long re-fire chain is safe).
cats-eo also adds `everywhere[S]`, a recursive rewrite
exposed as a composable `Setter` — `everywhere.andThen(prism).modify(f)`
applies an ordinary optic at every depth — which Monocle has no
equivalent for. See the [cookbook recipe](cookbook.md).

## Discipline law instances

Downstream projects can reuse the same `checkAll` pattern they
know from cats. `cats-eo-laws` ships the rule-sets:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-laws" % "@VERSION@" % Test
```

```scala
import dev.constructive.eo.laws.discipline.LensTests

checkAll("Lens[Person, Int]", LensTests[Person, Int](ageL).lens)
```

Every public optic family has a matching `FooLaws` /
`FooTests` pair. See the `laws/src/main/scala/eo/laws/` tree
for the full list.
