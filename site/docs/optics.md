# Optics reference

One section per family, each with the shape, carrier, primary
use case, and a minimal runnable example. For the per-method
reference see the Scaladoc.

```scala mdoc:silent
import eo.optics.{Lens, Optic}
import eo.optics.Optic.*
import eo.data.Forgetful.given    // Accessor[Forgetful] — powers .get on Iso / Getter
```

Every page here shows optics constructed by hand. For the
macro-derived `lens[S](_.field)` / `prism[S, A]` flavour, see
[Generics](generics.md).

## Lens

A `Lens[S, A]` focuses a single, always-present field of a
product type. Carrier: `Tuple2`.

```scala mdoc:silent
case class Person(name: String, age: Int)
val ageL = Lens[Person, Int](_.age, (p, a) => p.copy(age = a))
```

```scala mdoc
val alice = Person("Alice", 30)
ageL.get(alice)
ageL.replace(31)(alice)
ageL.modify(_ + 1)(alice)
```

Composes via `.andThen` with other Lenses and — transparently,
with no extra syntax — with `Optional` / `Setter` / `Traversal`
optics too. The cross-carrier variant of `.andThen` summons a
`Composer[F, G]` or `Composer[G, F]` to bring both sides under
a common carrier.

## Prism

A `Prism[S, A]` focuses one branch of a sum type — `Some` over
`None`, or a specific case of an enum. Carrier: `Either`.

```scala mdoc:silent
import eo.optics.Prism

enum Shape:
  case Circle(r: Double)
  case Square(s: Double)

val circleP = Prism[Shape, Shape.Circle](
  {
    case c: Shape.Circle => Right(c)
    case other           => Left(other)
  },
  identity,
)
```

```scala mdoc
circleP.to(Shape.Circle(1.0))
circleP.to(Shape.Square(2.0))

// modify acts only on the Circle branch; Squares pass through
// unchanged.
circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Circle(1.0))
circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Square(2.0))
```

For auto-derivation on enums / sealed traits / union types see
`prism[S, A]` in [Generics](generics.md).

## Iso

An `Iso[S, A]` is a bijection — every `S` round-trips to exactly
one `A` and back. Carrier: `Forgetful` (the identity carrier).

```scala mdoc:silent
import eo.optics.Iso

case class PersonPair(age: Int, name: String)
val pairIso = Iso[(Int, String), (Int, String), PersonPair, PersonPair](
  t => PersonPair(t._1, t._2),
  p => (p.age, p.name),
)
```

```scala mdoc
pairIso.get((30, "Alice"))
pairIso.reverseGet(PersonPair(30, "Alice"))
```

## Optional

An `Optional[S, A]` focuses a conditionally-present field —
an `Option[A]` field, a predicate-gated access, a
refinement-style narrowing. Carrier: `Affine`.

```scala mdoc:silent
import eo.data.Affine
import eo.optics.Optional

case class Contact(flag: Option[String])

val presentFlag = Optional[Contact, Contact, String, String, Affine](
  getOrModify = c => c.flag.toRight(c),
  reverseGet  = { case (c, s) => c.copy(flag = Some(s)) },
)
```

```scala mdoc
presentFlag.modify(_.toUpperCase)(Contact(Some("hello")))
presentFlag.modify(_.toUpperCase)(Contact(None))
```

Composition with a Lens is automatic: `lens.andThen(optional)`
summons `Composer[Tuple2, Affine]` under the hood and morphs
the Lens into the Affine carrier. No explicit `.morph` required
on your end.

### Read-only construction

`Optional.readOnly(matches: S => Option[A])` builds an
`Optic[S, Unit, A, A, Affine]` from just a partial projection —
no write-back required. `T = Unit` statically rules out
`.modify` / `.replace`, so the resulting optic exposes only the
read-side operations (`.foldMap`, `.modifyA` for collecting
effects).

Use this when the source shape has no natural write-back
(`headOption` on a List, predicate-gated filters like
`Option.when(p(s))(s)`), or as an API-boundary declaration that
callers cannot write through the returned optic.

```scala mdoc:silent
case class Adult(age: Int)
val adultAge =
  Optional.readOnly[Adult, Int](p => Option.when(p.age >= 18)(p.age))
```

```scala mdoc
import cats.instances.int.given
adultAge.foldMap(identity[Int])(Adult(20))
adultAge.foldMap(identity[Int])(Adult(15))
```

`Optional.selectReadOnly(p)` is the corresponding filter — keep
only inputs matching `p`:

```scala mdoc:silent
val evenOpt = Optional.selectReadOnly[Int](_ % 2 == 0)
```

```scala mdoc
evenOpt.foldMap(identity[Int])(4)
evenOpt.foldMap(identity[Int])(3)
```

## Setter

A `Setter[S, A]` can modify but not read — a write-only focus
for cases where the focus value isn't observable to the caller.
Carrier: `SetterF`.

```scala mdoc:silent
import eo.optics.Setter

case class SetterConfig(values: Map[String, Int])
val bumpAll = Setter[SetterConfig, SetterConfig, Int, Int] { f => cfg =>
  cfg.copy(values = cfg.values.view.mapValues(f).toMap)
}
```

```scala mdoc
bumpAll.modify(_ + 1)(SetterConfig(Map("a" -> 1, "b" -> 2)))
```

## Getter

A `Getter[S, A]` is the read-only counterpart to `Setter` — a
pure projection. Carrier: `Forgetful` with `T = Unit`.

```scala mdoc:silent
import eo.optics.Getter

val nameLen = Getter[Person, Int](_.name.length)
```

```scala mdoc
nameLen.get(Person("Alice", 30))
```

Getter → Getter doesn't compose via `Optic.andThen` today
(Getter's `T = Unit` mismatches the outer `B` slot). For a
deeper read, compose a Lens chain and call `.get` on the
composed lens.

## Fold

A `Fold[F, A]` summarises every element of a `Foldable[F]` via
`Monoid[M]`. Carrier: `Forget[F]`.

```scala mdoc:silent
import cats.instances.list.given
import eo.optics.Fold

val listFold = Fold[List, Int]
```

```scala mdoc
listFold.foldMap(identity[Int])(List(1, 2, 3))
listFold.foldMap((i: Int) => i * i)(List(1, 2, 3))
```

`Fold.select(p)` narrows to elements matching a predicate:

```scala mdoc:silent
val positive = Fold.select[Int](_ > 0)
```

```scala mdoc
positive.foldMap(identity[Int])(3)
positive.foldMap(identity[Int])(-3)
```

## Review

A `Review[S, A]` is the reverse-only counterpart to `Getter` —
it wraps an `A => S` build function. Unlike the other families,
`Review` does **not** extend `Optic` (the Optic trait requires
an observing `to` that a pure review has none of); it's a
standalone type with its own composition.

```scala mdoc:silent
import eo.optics.Review

val someIntR = Review[Option[Int], Int](Some(_))
```

```scala mdoc
someIntR.reverseGet(42)
```

Compose by composing the underlying `A => S` functions directly:

```scala mdoc:silent
val lengthR = Review[Int, String](_.length)
val someLen = Review[Option[Int], String](
  s => someIntR.reverseGet(lengthR.reverseGet(s))
)
```

```scala mdoc
someLen.reverseGet("hello")
```

Two factory methods pull the natural build direction out of an
Iso or a Prism — aliased as `ReversedLens` and `ReversedPrism`
for users who expect to find those names next to the rest of
the optics reference:

```scala mdoc:silent
import eo.optics.{BijectionIso, MendTearPrism, ReversedLens, ReversedPrism}

val doubleIso =
  BijectionIso[Int, Int, Int, Int](_ * 2, _ / 2)
val revIso = ReversedLens(doubleIso)

val somePrism = new MendTearPrism[Option[Int], Option[Int], Int, Int](
  tear = {
    case Some(n) => Right(n)
    case other   => Left(other)
  },
  mend = Some(_),
)
val revPrism = ReversedPrism(somePrism)
```

```scala mdoc
revIso.reverseGet(5)
revPrism.reverseGet(7)
```

**`ReversedLens` only accepts a bijective Lens** (an
`BijectionIso`). A general Lens doesn't carry enough
information to reconstruct its source from the focus alone —
for that, construct a `Review` directly with your own
`A => S`.

## Traversal

A `Traversal` is the multi-focus modify optic — map over every
element of a container. Two carriers coexist:

* `Traversal.each[F, A]` / `Traversal.pEach[F, A, B]` — carrier
  `PowerSeries`, the default. Supports `.andThen` with downstream
  optics. Linear scaling with a small constant-factor overhead
  (~15–20× over a naive `copy`/`map`) from the Composer chain's
  per-element dispatch.
* `Traversal.forEach[F, A, B]` — carrier `Forget[F]`, map-only
  fast path. Identity-shaped carrier, linear time, no downstream
  optic composition.

Use `each` for the composable default — it's what Scala users
reach for intuitively:

```scala mdoc:silent
import eo.optics.Traversal
import eo.data.PowerSeries

val listEach = Traversal.pEach[List, Int, Int]
```

```scala mdoc
listEach.modify(_ + 1)(List(1, 2, 3))
```

Stick with `forEach` when the chain terminates — no downstream
optics — and you want the tight map-only path:

```scala mdoc:silent
val listForEach = Traversal.forEach[List, Int, Int]
```

```scala mdoc
listForEach.modify(_ + 1)(List(1, 2, 3))
```

`each` shines when the chain continues past the traversal — e.g.
"for every phone, toggle `isMobile`":

```scala mdoc:silent
case class Phone(isMobile: Boolean, number: String)
case class Owner(phones: List[Phone])

val ownerAllPhonesMobile =
  Lens[Owner, List[Phone]](_.phones, (o, ps) => o.copy(phones = ps))
    .andThen(Traversal.each[List, Phone])
    .andThen(Lens[Phone, Boolean](_.isMobile, (p, m) => p.copy(isMobile = m)))
```

```scala mdoc
ownerAllPhonesMobile.modify(!_)(Owner(List(
  Phone(isMobile = false, "555-0001"),
  Phone(isMobile = true,  "555-0002"),
)))
```

See [the PowerSeries benchmark
notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md#interpreting-powerseries-numbers)
for the cost tradeoff.
