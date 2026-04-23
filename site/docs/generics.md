# Generics

The `cats-eo-generics` module supplies two macros that eliminate
the boilerplate usually written by hand for Lens / Prism
derivation.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-generics" % "@VERSION@"
```

```scala mdoc:silent
import eo.optics.Optic.*
import eo.generics.{lens, prism}
import eo.docs.{Address, Customer, NameAgePair, Person, Shape, Shape2, Coords, Zip}
```

> Macro-derived optics need their target case classes and enum
> cases to live at a package-level location. The page hosts its
> samples in `eo.docs.*` for that reason — the same `lens` /
> `prism` calls work identically on your own top-level ADTs.

## `lens[S](_.field)`

Two-step partial application: `lens[Customer]` pins the source
type, the second call picks the field:

```scala mdoc:silent
val nameL = lens[Customer](_.name)
val ageL  = lens[Customer](_.age)
```

```scala mdoc
val alice = Customer("Alice", 30)
nameL.get(alice)
ageL.replace(31)(alice)
nameL.modify(_.toUpperCase)(alice)
```

Works on any N-field case class. The macro also handles Scala 3
enum cases, which would normally break under Monocle's
`GenLens` because enum cases don't expose `.copy`. EO emits a
direct `new S(…)` call through
[hearth](https://github.com/MateuszKubuszok/hearth)'s
`CaseClass.construct`, which works uniformly for both.

### Composition

Use `.andThen` to drill deeper:

```scala mdoc:silent
val streetL =
  lens[Person](_.address).andThen(lens[Address](_.street))
```

```scala mdoc
val bob = Person("Bob", Address("Elm St", Zip(54321, "0000")))
streetL.get(bob)
streetL.modify(_.toUpperCase)(bob)
```

### Type-level complement

The derived lens exposes the **structural complement** of the
focused field as its existential `X`. For an N-field case class
focused on one field, `X` is a `NamedTuple` over the remaining
fields, preserving both names and types. That's the evidence
`Optic.transform` / `.place` / `.transfer` need — no
`given` at the call site required:

```scala mdoc
val renamed = nameL.place("Carol")(alice)
```

## Multi-field Lens — `lens[S](_.a, _.b, …)`

The same entry point accepts multiple selectors. When the selector
set is a **strict subset** of the case class's fields, the macro
emits a `SimpleLens[S, Focus, Complement]` where `Focus` is a
Scala 3 `NamedTuple` in SELECTOR order and `Complement` is a
`NamedTuple` in DECLARATION order among the non-focused fields.

```scala mdoc:silent
// `Customer(name, age)` with only 2 fields is full-cover territory —
// see the Iso section below. For a proper partial-cover example we
// need a wider case class; use `Person(name, address)` (2 fields) by
// focusing *one* field to stay on the Lens path, then reach for
// multi-field on wider data like the 3-field ADT below.

final case class OrderItem(sku: String, quantity: Int, price: Double)
val qtyAndPrice = lens[OrderItem](_.quantity, _.price)
```

```scala mdoc
val item = OrderItem("abc-123", 3, 9.99)
val focus = qtyAndPrice.get(item)
focus.quantity
focus.price

val (complement, _) = qtyAndPrice.to(item)
complement.sku
```

The focus NamedTuple preserves selector order, so
`lens[OrderItem](_.price, _.quantity)` would produce a focus whose
`.price` field comes before `.quantity`. That choice is deliberate
(D1 in the implementation plan) — downstream code usually cares
about the order the fields appear in the call, not the original
declaration order.

## Full-cover Iso — `lens[S](_.a, _.b, …)` covering every field

When the selector set covers **every** case field of `S` (in any
order, at any arity including N = 1 on a 1-field wrapper), the
macro emits a `BijectionIso[S, S, Focus, Focus]` instead of a
`SimpleLens`. Downstream `.get` / `.reverseGet` / `.modify` all
work without extra evidence; `.andThen` picks up the fused
`BijectionIso` overloads for free.

```scala mdoc:silent
val nameAgeIso = lens[NameAgePair](_.name, _.age)
```

```scala mdoc
val pair = NameAgePair("Dana", 42)
val tuple = nameAgeIso.get(pair)
tuple.name
tuple.age
nameAgeIso.reverseGet(tuple)
```

Selector-order inversion flips the NamedTuple shape:

```scala mdoc:silent
val ageNameIso = lens[NameAgePair](_.age, _.name)
```

```scala mdoc
val rev = ageNameIso.get(pair)
rev.age
rev.name
ageNameIso.reverseGet(rev)
```

## Compile-time diagnostics

All macro failures surface at compile time with explicit messages
prefixed `lens[S]:` for grep-ability:

- **Empty varargs** — `lens[Customer]()` → "requires at least one
  field selector".
- **Non-case-class source** — `lens[SomeInterface](...)` → Hearth's
  `CaseClass.parse` diagnostic.
- **Non-field selector** — `lens[Customer](_.name.toUpperCase)` →
  "selector at position 0 must be a single-field accessor like
  `_.fieldName`. Nested paths (e.g. `_.a.b`) are not yet supported."
- **Unknown field** — `lens[Widget](_.bogus)` → "'bogus' is not a
  field of Widget. Known fields: name, size".
- **Duplicate selectors** — `lens[OrderItem](_.sku, _.sku)` →
  "duplicate field selector 'sku' at positions 0, 1. Each field
  may appear at most once."

Duplicate rejection fires at compile time:

```scala mdoc:fail
val dup = lens[NameAgePair](_.name, _.name)
```

Nested paths are rejected too — chain manually if you need them:

```scala mdoc:fail
val nested = lens[Person](_.address.street)
```

```scala mdoc:silent
// Fine — two independent Lens derivations composed:
val ok = lens[Person](_.address).andThen(lens[Address](_.street))
```

## `prism[S, A]`

A `prism[S, A]` derives a Prism from the parent sum type `S` to
a specific child `A <: S`. Recognises:

- Scala 3 enums,
- Sealed traits with direct child types,
- Scala 3 union types.

```scala mdoc:silent
val circleP   = prism[Shape, Shape.Circle]
val squareP   = prism[Shape, Shape.Square]
val triangleP = prism[Shape, Shape.Triangle]
```

```scala mdoc
circleP.to(Shape.Circle(1.0))
circleP.to(Shape.Square(2.0))

circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Circle(1.0))
circleP.modify(c => Shape.Circle(c.r * 2))(Shape.Square(2.0))
```

Union types work the same way:

```scala mdoc:silent
val intP = prism[Int | String, Int]
```

```scala mdoc
intP.to(42: Int | String)
intP.to("hi": Int | String)
```

### Composition with Lens chains

`prism ∘ lens` works naturally through `Composer` bridges:

```scala mdoc:silent
import eo.data.Affine

val circleCoordsX =
  prism[Shape2, Shape2.Circle]
    .andThen(lens[Shape2.Circle](_.c))
    .andThen(lens[Coords](_.x))
```

```scala mdoc
circleCoordsX.modify(_ + 10)(Shape2.Circle(Coords(3, 4), 1.0))
circleCoordsX.modify(_ + 10)(Shape2.Square(Coords(3, 4), 2.0))
```

## Macro errors

Both macros fail at compile time with explicit errors when
their input doesn't fit. Examples:

- `lens[Person](_.address.street)` — nested paths not yet
  supported in a single macro call; chain instead:
  `lens[Person](_.address).andThen(lens[Address](_.street))`.
- `prism[Shape, OtherEnum.Foo]` — `A` must be a direct child of
  `S`; otherwise the macro aborts with "not a direct child of".
- `lens[NonCaseClass](_.field)` — only works on case classes.

See the [Scaladoc for
`LensMacro`](https://javadoc.io/doc/dev.constructive/cats-eo-generics_3/latest/api/eo/generics/LensMacro$.html)
and
[`PrismMacro`](https://javadoc.io/doc/dev.constructive/cats-eo-generics_3/latest/api/eo/generics/PrismMacro$.html)
for the implementation details.
