# chimney integration

The `cats-eo-chimney` module connects eo to
[chimney](https://chimney.readthedocs.io/), the Scala library that
derives conversions between similar data types. The two tools do
different jobs. chimney turns a whole `S` into a `T`; eo points at one
part of a structure and reads or writes it. This module turns chimney's
derived values into eo optics, so a chain can convert first and then
keep going into fields of the target. It also lets eo run chimney
conversions and patches at a focus, or on what an optic writes back.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-chimney" % "@VERSION@"
```

The module brings in chimney (2.1.0) itself. eo-generics is not a
dependency; the examples below use it for field lenses, so add
`cats-eo-generics` if you want the same.

| chimney value                | eo optic                       | Writes? |
|------------------------------|--------------------------------|---------|
| `Transformer[S, T]`          | `Getter[S, T]`                 | no      |
| `Iso[A, B]`                  | `Iso[A, B]`                    | yes     |
| `Codec[Domain, Dto]`         | prism from `Dto` to `Domain`   | yes     |
| `PartialTransformer[S, T]`   | `ResultFold[S, T]`             | no      |
| `Patcher[A, P]`              | `Modify[A, A, Unit, P]`        | yes     |

`Chimney.getter` / `iso` / `prism` / `resultFold` / `patching`
summon or derive the chimney value. Every chimney value also has a
`.toGetter` / `.toIso` / `.toPrism` / `.toResultFold()` / `.toModify`
extension, for values you configured by hand (`Transformer.define[…]
.withFieldRenamed(…).buildTransformer`). They are plain constructors,
not givens: you declare the optic you want, as everywhere else in eo.

```scala mdoc:silent
import dev.constructive.eo.chimney.*
import dev.constructive.eo.docs.chimney.*
import dev.constructive.eo.generics.lens
import dev.constructive.eo.optics.Optic.*
```

## Convert, then read a field

A `Transformer[S, T]` becomes a `Getter[S, T]`. Compose it with lenses
on `T` to read a field of the converted value:

```scala mdoc:silent
val viewCity =
  Chimney
    .getter[User, UserView]
    .andThen(lens[UserView](_.address))
    .andThen(lens[AddressView](_.city))

val ada = User("Ada", "hunter2", Address("1 Analytical St", "London"))
```

```scala mdoc
viewCity.get(ada)
```

chimney derives the `Transformer` here; with one in scope, that one is
used. The chain can only read: a `Transformer` has no way back from
`T` to `S`. For that, use one of the two-way chimney types below.

## Write through a conversion

chimney's `Iso[A, B]` (two total transformers) becomes an eo `Iso`, so
the chain can `modify` and land back in `A`:

```scala mdoc
val rowAge = Chimney.iso[Person, PersonRow].andThen(lens[PersonRow](_.age))

rowAge.modify(_ + 1)(Person("Ada", 36))
```

chimney's `Codec[Domain, Dto]` encodes totally but may fail to decode.
It becomes a prism from the DTO to the domain value: `getOption`
decodes and `reverseGet` encodes. Composed with a lens, it edits a
domain field directly on the DTO. A DTO that does not decode is passed
through unchanged, which is how eo prisms always behave on a miss:

```scala mdoc
val owner = Chimney.prism[Account, AccountDto].andThen(lens[Account](_.owner))

owner.modify(_.toUpperCase)(AccountDto("ada", 120L))
owner.modify(_.toUpperCase)(AccountDto("bob", -5L))
```

These optics are lawful only if chimney's two directions really invert
each other: decode after encode must return the domain value, and
encode after decode must return the DTO. Check your own instances with
cats-eo-laws' `IsoTests` and `PrismTests`, as this module's own specs
do.

## Read with errors: `ResultFold`

A `PartialTransformer[S, T]` becomes a `ResultFold[S, T]`. This is a
read-only optic with at most one focus, like `AffineFold`. The
difference is what happens on a miss: it keeps chimney's errors,
including the path of each failing field. `.result(s)` returns the
`partial.Result`, `.either(s)` an `Either`, and `.getOption(s)` the
focus without the errors:

```scala mdoc
val decode = Chimney.resultFold[AccountDto, Account]

decode.result(AccountDto("bob", -5L)).asErrorPathMessageStrings.toList
```

The errors survive composition with a single-focus optic. `andThen`
with a lens, iso, `Getter` or another `ResultFold` keeps the outer
errors. `andThen` with a prism, optional or `AffineFold` turns an
inner miss into chimney's empty-value error:

```scala mdoc
val decodedOwner = decode.andThen(lens[Account](_.owner))

decodedOwner.either(AccountDto("ada", 120L))
decodedOwner.either(AccountDto("bob", -5L)).left.map(_.asErrorPathMessageStrings.toList)
```

A `Traversal` or `Fold` has many foci, so there is no single result
whose errors could be kept. To compose with one, start from
`.affineFold`, the version without errors. `.toResultFold(failFast =
true)` stops at the first error, as chimney's `transformFailFast` does.

## Patch a nested part

A `Patcher[A, P]` merges the fields of a patch value `P` into an `A`.
`optic.patch(p)` applies it wherever the optic points. eo finds the
part, and chimney merges the patch into it:

```scala mdoc
val order = Order(1L, Customer("Ada", "ada@old", "000"))

lens[Order](_.customer).patch(ContactPatch("ada@new", "111"))(order)
```

`Chimney.patching[A, P]` returns the same operation as a stored,
write-only optic (`Modify[A, A, Unit, P]`, where `replace(p)` applies
the patch), so it can be composed after any writable optic:

```scala mdoc
val contact = lens[Order](_.customer).andThen(Chimney.patching[Customer, ContactPatch])

contact.replace(ContactPatch("e", "p"))(order)
```

If the optic misses (a prism or optional), the value passes through
unchanged. A `Traversal` patches every focus.

## Change a focus's type

`optic.transformFocus` runs chimney's `Transformer[A, B]` at the
focus and turns an `S` into a `T`. The optic must be able to change
the type of its focus (`T` different from `S`). eo-generics' derived
lenses can't do that, so use the core polymorphic constructors:
`Traversal.pEach`, `Lens.pLens`, `Prism.pPrism`, `Traversal.first` /
`second`, and so on.

```scala mdoc:silent
import dev.constructive.eo.optics.Traversal

val prices = Traversal.pEach[List, RawPrice, Price]
```

```scala mdoc
prices.transformFocus(List(RawPrice(100), RawPrice(250)))
```

`transformFocusPartial` does the same with a `PartialTransformer`. It
returns the new `T`, or the errors from every focus that failed;
across a `Traversal`'s foci the errors accumulate rather than stopping
at the first one:

```scala mdoc
prices.transformFocusPartial(List(RawPrice(-1), RawPrice(5), RawPrice(-2)))
  .asErrorPathMessageStrings
  .toList
```

Each error's path is relative to its focus, because chimney does not
know where the optic was pointing.

## Change the result type

`optic.transformImage[U]` works on the other side of the optic. It
runs chimney's `Transformer[T, U]` on whatever the optic writes back,
so every write returns a `U` instead of a `T`. Reads are unchanged.
Here a lens edits a `User` and returns the public view:

```scala mdoc
lens[User](_.name).transformImage[UserView].modify(_.toUpperCase)(ada)
```

A prism or optional that misses still returns a `U`: the unchanged
value, transformed. `transformImagePartial[U]` does the same with a
`PartialTransformer[T, U]`, so a write returns the `U` or the errors
from converting into it — edit the DTO, then validate it into the
domain:

```scala mdoc
val balance = lens[AccountDto](_.balance).transformImagePartial[Account]

balance.modify(_ + 1)(AccountDto("ada", 120L)).asOption
balance.modify(_ - 200)(AccountDto("ada", 120L)).asErrorPathMessageStrings.toList
```

Both return a generic `Optic`, so `.modify`, `.replace` and
`.reverseGet` keep working, but the concrete optic's fused fast paths
are not carried over.

## Performance

`ChimneyBench` (in `benchmarks/`) runs every scenario three ways:
through this module (**eo-chimney**), with **chimney** alone (the same
transformers, built once; the `into…withFieldComputed` DSL where a
field override is needed), and **hand**-written with neither library.
All three compute the same value. Optics and transformers are built
once, outside the measured call, and only the success paths are
measured.

| Scenario | eo-chimney B/op | chimney B/op | hand B/op | eo ns/op | chimney ns/op | hand ns/op |
|---|---:|---:|---:|---:|---:|---:|
| `transformFocus` — retype 64 list elements | 3,616 | 3,072 | 3,072 | 1043 | 847 | 725 |
| `transformFocusPartial` — same, partially | 11,432 | 3,184 | 4,624 | 3019 | 962 | 928 |
| `transformImage` — edit a field, return the view | 96 | 152 | 96 | 29 | 37 | 25 |
| `transformImagePartial` — edit, then validate | 120 | 96 | 64 | 29 | 22 | 12 |
| `Chimney.getter` ∘ two lenses — read a field | 80 | 0 | 0 | 19 | 1 | 1 |
| `Chimney.prism` ∘ lens — edit a DTO field | 240 | 128 | 72 | 58 | 47 | 19 |
| `ResultFold` ∘ lens — decode, read, keep errors | 96 | 72 | 16 | 24 | 19 | 3 |
| `lens.patch` — patch a nested part | 48 | 48 | 48 | 10 | 23 | 10 |

One local run (JDK 25, `-i 5 -wi 3 -f 1 -prof gc`) on a shared 4-vCPU
machine. As on the [benchmarks page](../benchmarks.md), allocation
(B/op) is the figure to compare; nanoseconds only show the direction,
and several of these error bars are as wide as the number itself.

What the numbers say:

- **`transformImage` allocates no more than the hand-written code**,
  and less than chimney's DSL. `transformFocus` and
  `transformImagePartial` cost a little more than chimney alone.
- **`transformFocusPartial` costs about 3.5× chimney's allocation**
  (and is about 3× slower). It runs through the optic's generic
  `modifyA` with an error-accumulating `partial.Result` applicative,
  which allocates per element. For a plain collection, chimney's own
  `transformIntoPartial` is the faster route; the eo version earns its
  cost where chimney cannot reach, such as foci picked out by a prism
  or a nested traversal.
- **Convert-then-read allocates where chimney alone does not.** The JIT
  removes chimney's whole conversion when only one field is read, but
  the eo chain's lenses build intermediate tuples (80 B). Reading a
  field this way is a convenience, not a fast path.

Reproduce with:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof gc .*ChimneyBench.*"
```

## Notes

- **Field-type changes inside a case class.** chimney already handles
  these on its own. Put a `Transformer[A, B]` in scope and
  `s.transformInto[T]` uses it for that field. `transformFocus` is for
  the cases chimney cannot reach by itself: containers, sum-type
  branches, and anywhere else an eo optic points.
- **Dependencies.** chimney 2.x is built on the same macro library as
  eo-generics ([hearth](https://github.com/MateuszKubuszok/hearth)), so
  both use it on one classpath.
