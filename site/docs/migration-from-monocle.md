# Migrating from Monocle

[Monocle](https://www.optics.dev/Monocle/) is a mature, elegant
optics library and the reference point for optics on Scala. If it is
serving you well, there is no urgency to move. This page is for the
cases where you want what cats-eo optimises for — and a map of what
changes (and what doesn't) when you do.

## Why migrate?

cats-eo and Monocle care about different things. Monocle's design
prizes a clean, minimal core: eight classic optics related by an
inheritance hierarchy, so the library reads as beautifully as the
code you write with it. cats-eo is built for **performance and
industrial application**, and it accepts internal complexity to get
there. The library's own source is not the most elegant code you
will ever read — existential carriers, fused composition overloads,
opaque types, hand-tuned array machines — but every one of those
choices buys something at *your* call site:

- **Efficiency.** Optic operations compile down to plain function
  composition wherever possible. The fused `inline` composers mean a
  deeply composed optic dispatches like hand-written code instead of
  a chain of allocated closures — on the [benchmarks](benchmarks.md),
  composed getters and modifies run ~2× faster than the equivalent
  Monocle composition at depth 6, and `each`-style traversals reach
  ~3× on realistic collection sizes (Monocle wins some single-hop
  cells; the gap is a composition-depth story). When an optic sits on
  a hot path, this is the difference that pays for the migration.
- **Stack safety.** Deep or degenerate structures are a fact of
  industrial data — a 100k-deep JSON spine, a recursive ADT from a
  parser. cats-eo's recursive machinery (`Plated`, the recursion
  schemes) runs on heap-backed machines and is safe at depths where
  simple recursion overflows.
- **Interoperability.** The same Lens / Prism / Traversal vocabulary
  you use on case classes extends to wire formats: circe `Json`
  ASTs, Apache Avro records, and raw jsoniter byte buffers — read
  and edit encoded data without a decode/re-encode round trip. The
  Monocle ecosystem prefers to leave format-specific tooling to the
  formats themselves; cats-eo treats it as part of the optics story.
- **Compositional reach.** Every family composes with every other
  family through one `.andThen`, and the result is compiler-pinned:
  an 11-family [composition matrix](optics.md#composition-matrix)
  asserts every pair either composes (without type ascriptions or
  imports) or fails to compile by design. The family space itself is
  larger: standalone read-only, build-only, and write-only citizens
  (AffineFold, Review, Unfold, Modify), multi-focus shapes (Grate,
  Kaleidoscope, algebraic lenses), and recursion schemes
  (`cata` / `ana` / `hylo`) that *are* optics and drop into the same
  pipelines.

The trade is real in both directions: Monocle gives you a smaller,
smoother surface; cats-eo gives you throughput, depth, and reach.

## What stays the same

Your day-to-day vocabulary ports almost verbatim — `get`, `replace`,
`modify`, `foldMap`, `getOption`, `reverseGet`, and `.andThen`
composition all read the same. `GenLens[S](_.field)` becomes
`lens[S](_.field)` (from `dev.constructive.eo.generics`), and
Monocle's polymorphic `PLens` / `PPrism` / `PIso` shapes map onto
eo's `pLens` / polymorphic constructors one-for-one.

Law testing ports directly too. Monocle ships
`monocle-law` with discipline rule-sets; `cats-eo-laws` follows the
exact same `checkAll` pattern, so your test setup is an import swap:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-laws" % "@VERSION@" % Test
```

```scala
// before: import monocle.law.discipline.LensTests
import dev.constructive.eo.laws.discipline.LensTests

checkAll("Lens[Person, Int]", LensTests[Person, Int](ageL).lens)
```

Every public optic family has a matching `FooLaws` / `FooTests`
pair, including the families Monocle prefers not to ship (AffineFold,
Review, Unfold, Modify, MultiFocus).

## Where eo differs at the call site

### Bring your own optic

Both libraries give you one `.andThen` across families, so that is
not the difference. Monocle composes through its inheritance
hierarchy: every optic *is* a weaker optic, so composition meets at
the common ancestor — an elegant design, and it works because the
hierarchy is closed and known up front. cats-eo chose not to have an
extension hierarchy at all, and *that* is what changes in practice:
composition is driven by typeclass instances over the **carrier**
(`AssociativeFunctor[F, X, Y]` for a shared carrier, a summoned
`Morph[F, G]` / `Composer[F, G]` across carriers), not by where a
family sits in a subtype tree.

The payoff is for optics that come from *outside* the shipped
families. A custom optic needs no blessed place in any hierarchy:
implement the `Optic` trait, put the carrier instances in scope, and
it composes with everything reachable through the bridges — bring
your own optic. [Extensibility](extensibility.md) walks through
shipping a domain-tuned optic end to end. The same machinery is what
makes the stock families compose:

```scala mdoc:silent
import dev.constructive.eo.optics.Lens
import dev.constructive.eo.optics.Optional

case class MigConfig(timeout: Option[Int])
case class MigApp(config: MigConfig)

val timeoutOpt = Optional[MigConfig, MigConfig, Int, Int](
  getOrModify = c => c.timeout.toRight(c),
  reverseGet  = { case (c, t) => c.copy(timeout = Some(t)) },
)

val appConfig =
  Lens[MigApp, MigConfig](_.config, (a, c) => a.copy(config = c))

// Cross-carrier `.andThen` lifts the Lens into the Optional's
// carrier automatically via `Composer[Tuple2, Affine]`.
val appTimeout = appConfig.andThen(timeoutOpt)
```

Composition is carrier-level (one `Composer` per pair of carriers),
not family-level — so a new family, yours or ours, supplies carrier
instances and inherits the cross-family bridges for free.

### The one-way side, built out in both directions

Read-only collapse is common ground, not a difference: compose a
chain that touches a Getter and both libraries land you on the
read-only join — Monocle through its hierarchy, cats-eo through
`ReadCompose` (`lens.andThen(getter)` → Getter,
`prism.andThen(getter)` → AffineFold, `traversal.andThen(getter)` →
Fold). Where cats-eo invests further is the **write side and
beyond**:

- The build/write-only direction is fully populated — `Modify`
  (write-only), `Review` and `Unfold` (build-only, one focus and
  many) are standalone, composable citizens, with a *fallible* tier
  (writes and builds that can reject, with accumulating failures)
  planned next. The [composition matrix](optics.md#composition-matrix)
  pins how every pair behaves, including the cells that are void by
  design (`getter.andThen(modify)` — nothing to write through).
- A preliminary integration with **recursion schemes**: cats-eo is
  attempting to present recursion schemes as optics *themselves* —
  `cata` is a Getter, `ana` a Review, `hylo` a fused Getter — so that
  genuinely complex algorithms over trees and recursive data
  structures sit in the same pipeline as ordinary getters and
  modifiers, and the whole thing remains **one optic from start to
  finish**. The direction this is heading: an HTTP server that
  receives a JSON request, writes to a PostgreSQL database, and logs
  to an Avro-encoded Kafka topic — expressed as a single composed
  optic.

That ambition is why the family roster below is as large as it is.

### A larger family space

Beyond the classic eight, cats-eo ships families Monocle prefers to
express through other means or leave to dedicated libraries:

- **AffineFold** — a standalone read-only 0-or-1 focus (where
  Monocle reaches for `Optional.getOption`), with `T = B = Unit` so
  the type itself forbids writing through it.
- **Review and Unfold** — standalone build-only optics (Monocle
  expresses building through `Iso.reverseGet` / `Prism.reverseGet`).
  `Unfold[T, B, F]` (`embed: F[B] => T`) carries recursion-scheme
  algebras and aggregations as composable citizens.
- **Recursion schemes as optics** — `Schemes.cata` is a `Getter`,
  `ana` a `Review`, `hylo` a fused `Getter`, all stack-safe; they
  compose into ordinary optic pipelines. Monocle prefers to leave
  recursion schemes to dedicated libraries (droste); see
  [Recursion schemes](schemes.md).
- **MultiFocus shapes** — Grate-style fixed-shape rewrites,
  Kaleidoscope aggregation, algebraic lenses; see
  [MultiFocus](multifocus.md).
- **Full-cover macro upgrade** — `lens[S](_.a, _.b, ...)` in one
  varargs call; when the selectors cover every field the result
  upgrades to a `BijectionIso` automatically (a shape Monocle
  prefers to leave to N hand-composed lenses).

### Traversal carrier

cats-eo's `Traversal.each[F, A]` / `pEach[F, A, B]` ride the
`MultiFocus[PSVec]` carrier — `.modify`, `.foldMap`, `.modifyA`, and
downstream `.andThen` all supported. It pays a small constant factor
over a hand-written `map` (see the
[PowerSeries benchmark notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#interpreting-powerseries-numbers))
and wins it back at composition depth.

### JSON, Avro, and raw bytes

[circe-optics](https://github.com/circe/circe-optics) — the
circe-maintained Monocle companion — gives you `JsonPath` optics
over the `Json` AST, and it does that job well. cats-eo's
[circe integration](integrations/circe.md) covers the same AST
territory and adds multi-field foci (`fields(_.a, _.b)` as a
NamedTuple focus) and an **observable failure channel**: misses and
decode failures surface as `Ior` values you can inspect, where
`JsonPath` prefers the simplicity of `Option` (a miss and a
type-mismatch read the same). From there, the same optic vocabulary
extends where the Monocle ecosystem prefers not to go: Apache Avro
`IndexedRecord`s ([eo-avro](integrations/avro.md)) and raw
`Array[Byte]` JSON via jsoniter-scala
([eo-jsoniter](integrations/jsoniter.md)) — pinpoint reads and
splice-writes on encoded data with no full decode/re-encode cycle.

### Plated is stack-safe and composes as an optic

Monocle's `monocle.function.Plated` keeps its recursion simple and
direct — which reads beautifully, but overflows the stack on
degenerate spines (`transform` / `universe` on a deep chain; see the
[benchmarks](benchmarks.md)). cats-eo's `Plated` trades that
simplicity for stack safety: it clears a 100k-deep spine —
`transform` / `everywhere` on a call-stack/heap-machine hybrid, the
reads on a worklist, `rewrite` trampolined through `cats.Eval`.
cats-eo also exposes `everywhere[S]` as a composable `Modify` —
`everywhere.andThen(prism).modify(f)` applies an ordinary optic at
every depth, a combinator Monocle's Plated chooses not to expose.
See the [cookbook recipe](cookbook.md).

## Cheat sheet

| Monocle                                            | cats-eo                                             |
|----------------------------------------------------|-----------------------------------------------------|
| `Lens[S, A](get)(a => s => …)`                     | `Lens[S, A](get, (s, a) => …)`                      |
| `PLens[S, T, A, B](get)(set)`                      | `Lens.pLens[S, T, A, B](get, enplace)` — every family has a polymorphic `pType` constructor |
| `GenLens[S](_.field)`                              | `lens[S](_.field)` (from `dev.constructive.eo.generics`)             |
| `GenLens[S](_.a).andThen(GenLens[S](_.b)).andThen(...)` — N hand-composed GenLenses | `lens[S](_.a, _.b, ...)` — one varargs call; full-cover upgrades to `BijectionIso` automatically |
| `Prism[S, A](_.some)(identity)`                    | `Prism.optional[S, A](_.some, identity)`            |
| `GenPrism[S, A]`                                   | `prism[S, A]` (from `dev.constructive.eo.generics`)                  |
| `Iso[S, A](f)(g)`                                  | `Iso[S, S, A, A](f, g)`                             |
| `Optional[S, A](_.some)(a => s => …)`              | `Optional[S, S, A, A](getOrModify, rg)`             |
| `optional.getOption` used as a read-only view      | `AffineFold(p => ...)` / `AffineFold.select(p)` / `AffineFold(optic.getOption)` — a standalone read-only 0-or-1 family, `T = Unit` forbids `.modify` |
| *(Monocle prefers to keep its surface to the classic optics)* | `MultiFocus.fromLensF` / `fromPrismF` / `fromOptionalF` — classifier-shaped optic over an `F[A]` focus, plus `.collectMap` / `.collectList` aggregation universals; see [Optics → MultiFocus](optics.md#multifocus) |
| `Setter[S, A](f => s => …)`                        | `Modify[S, S, A, A](f => s => …)`                   |
| `Iso.reverseGet` / `Prism.reverseGet` as the build path | `Review[S, A](build)` (build-only, one focus) and `Unfold[T, B, F]` (build-only, many: `embed: F[B] => T`); see [Optics → Review](optics.md#review) / [Unfold](optics.md#unfold) |
| *(recursion schemes via droste)*                   | `Schemes.cata` / `ana` / `hylo` — stack-safe, returned as composable optics; see [Recursion schemes](schemes.md) |
| `Fold.fromFoldable[List, Int]`                     | `Fold[List, Int]` (with `cats.instances.list.given`)|
| `Traversal.fromTraverse[List, Int]`                | `Traversal.each[List, Int]` (`Traversal.pEach[List, Int, Int]` for the polymorphic-write variant) |
| `monocle.function.Plated[A]` + `transform` / `rewrite` / `universe` / `children` | `Plated[S]` — derive with `plate[S]` (from `dev.constructive.eo.generics`) or hand-write with `Plated.fromChildren`; same combinator names, plus `Plated.everywhere[S]` as a composable Modify |
| `monocle.law.discipline.LensTests`                 | `dev.constructive.eo.laws.discipline.LensTests` — same checkAll pattern, every family covered |
| `lens.andThen(otherLens)`                          | `lens.andThen(otherLens)` — same                    |
| `lens.andThen(optional)`                           | `lens.andThen(optional)` — cross-carrier `.andThen` lifts via `Composer[Tuple2, Affine]` |
| `traversal.andThen(lens)`                          | `traversal.andThen(lens)` — auto-morph via the carrier bridges |
| `lens.get(s)`                                      | `lens.get(s)` — same                                |
| `lens.replace(a)(s)` / `lens.set(a)(s)`            | `lens.replace(a)(s)` — same                         |
| `lens.modify(f)(s)`                                | `lens.modify(f)(s)` — same                          |
| `prism.getOption(s)`                               | `prism.getOption(s)` — on the concrete returned class; `prism.to(s).toOption` through the generic trait |
| `prism.reverseGet(a)`                              | `prism.reverseGet(a)` — same                        |
| `optional.getOption(s)`                            | `optional.getOption(s)` — generic `.getOption` extension on any `Optic[_, _, _, _, Affine]` (Optional and AffineFold both ship it) |
| `traversal.modify(f)(xs)`                          | `traversal.modify(f)(xs)` — same                    |
| `fold.foldMap(f)(xs)`                              | `fold.foldMap(f)(xs)` — same                        |
