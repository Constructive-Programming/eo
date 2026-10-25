# Iron integration

The `cats-eo-iron` module turns [Iron](https://github.com/Iltotore/iron)
refined types into optics. Iron checks a refinement either at compile
time (a literal is proven to satisfy the constraint) or at runtime
(a value that arrives as data is tested). The runtime kind is a
partial conversion `A ⇀ A :| C` with a total way back — and that is
exactly a Prism.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-iron" % "@VERSION@"
```

No carrier and no givens ship in this module: `IronPrism` builds the
stock core `MendTearPrism`, so every fused composition (Lens → Optional,
Traversal → filtering Traversal) and every capability applies unchanged.

## IronPrism

`IronPrism[A, C]` needs Iron's `RuntimeConstraint[A, C]`, which Iron
derives from any `Constraint` wherever the types are concrete — so
every built-in constraint and every one you define qualifies.

```scala mdoc:silent
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive

import dev.constructive.eo.iron.IronPrism
import dev.constructive.eo.optics.{Lens, Traversal}
import dev.constructive.eo.optics.Optic.*

val positive = IronPrism[Int, Positive]
```

`getOption` tests the constraint, `reverseGet` forgets it:

```scala mdoc
positive.getOption(42)
positive.getOption(-1)
positive.reverseGet(7.refineUnsafe[Positive])
```

Both Prism laws hold for any pure constraint: a value that passed the
test passes it again, and forgetting a refinement gives back the very
value that was refined. The module's spec runs `cats-eo-laws`'
`PrismTests` on every constructor.

### From a constraint instance

When you hold the constraint itself — a `RuntimeConstraint[A, C]`, or
one of Iron's `Constraint` givens — `.prism` gives the same Prism:

```scala mdoc:silent
import io.github.iltotore.iron.constraint.string.Alphanumeric
import dev.constructive.eo.iron.prism

val positiveInt: RuntimeConstraint[Int, Positive] = summon
val alphanumeric = summon[Constraint[String, Alphanumeric]].prism
```

```scala mdoc
positiveInt.prism.getOption(-1)
alphanumeric.getOption("abc123")
alphanumeric.getOption("not ok!")
```

The `Constraint` form is inline, like Iron's own
`RuntimeConstraint.derived`, so its receiver must be a statically known
instance such as a `summon`.

## Composing

A refinement is usually a property of one field. Compose it after the
Lens that reaches the field and you get an Optional that writes only
when the field is valid:

```scala mdoc:silent
final case class Person(name: String, age: Int)

val ageL = Lens[Person, Int](_.age, (p, a) => p.copy(age = a))
val validAge = ageL.andThen(positive)
```

```scala mdoc
validAge.getOption(Person("Ada", 36))
validAge.modify(a => (a + 1).refineUnsafe[Positive])(Person("Ada", 36))
validAge.modify(a => (a + 1).refineUnsafe[Positive])(Person("Bob", -1))
```

After a Traversal the prism is a filter: only the elements that
satisfy the constraint are visited, the rest pass through untouched.

```scala mdoc
Traversal.each[List, Int].andThen(positive).foldMap(List(_))(List(3, -1, 4, 0))
```

Like every Prism, a miss is a silent pass-through on write. When the
reason for a miss matters, Iron's own `refineEither` carries the
constraint's message.

## Refined newtypes

Iron's `RefinedType` / `RefinedSubtype` companions define opaque
newtypes over a refinement. `IronPrism.refined[T]` builds the prism
from the base type to the newtype:

```scala mdoc:silent
type Age = Age.T
object Age extends RefinedType[Int, Positive]

val ageP = IronPrism.refined[Age]
```

```scala mdoc
ageP.getOption(30)
ageP.getOption(0)
```

With `import dev.constructive.eo.iron.prism` (above), the companion
itself gives the same Prism:

```scala mdoc
Age.prism.getOption(30)
```

## Whole containers

`IronPrism.all[F, A, C]` is all-or-nothing over any `Foldable`: it
hits only when every element satisfies the constraint, and then
refines the container as it stands — no copy, no per-element
allocation, since `A :| C` is an opaque alias of `A`.

```scala mdoc
IronPrism.all[List, Int, Positive].getOption(List(1, 2, 3))
IronPrism.all[List, Int, Positive].getOption(List(1, -2, 3))
```

To keep only the valid elements of a mixed container instead,
compose `Traversal.each` with `IronPrism` as above.
