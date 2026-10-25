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
| `PartialTransformer[S, T]`   | `ForgetFold[S, PartialResult, T]` | no   |
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

## Read with errors: a fold into the result

A `PartialTransformer[S, T]` becomes a core `ForgetFold` whose
container is chimney's result: a `ForgetFold[S, PartialResult, T]`.
(`PartialResult` is chimney's own `partial.Result` under another name,
so it can carry the cats instances the fold uses; at runtime it is the
same value.) It reads at most one focus, like `AffineFold`, but on a
miss it keeps chimney's errors, including the path of each failing
field. `.result(s)` returns the `partial.Result`, `.either(s)` an
`Either`, and `.getOption(s)` the focus without the errors:

```scala mdoc
val decode = Chimney.resultFold[AccountDto, Account]

decode.result(AccountDto("bob", -5L)).asErrorPathMessageStrings.toList
```

The errors survive composition with a single-focus optic, because a
core fold stays in its container when the container allows it. `andThen`
with a lens, iso or `Getter` maps the result. `andThen` with a prism,
optional or `AffineFold` maps it too, an inner miss becoming chimney's
empty-value error. `andThen` with another result fold combines both
sides' errors:

```scala mdoc
val decodedOwner = decode.andThen(lens[Account](_.owner))

decodedOwner.either(AccountDto("ada", 120L))
decodedOwner.either(AccountDto("bob", -5L)).left.map(_.asErrorPathMessageStrings.toList)
```

The same holds for any core fold whose container has the matching cats
instance: a fold into `Option` or `Either[E, *]` stays one through a
lens.

A `Traversal` or a fold over a collection has many foci, so there is no
single result whose errors could be kept; composing with one gives a
`List` fold. To compose without the errors, start from `.affineFold`.
`.toResultFold(failFast = true)` stops at the first error, as chimney's
`transformFailFast` does.

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

## Change the focus's write type

`optic.transformFocus[C]` returns a new optic that is written with a
`C` instead of the `B` the original optic writes. chimney's
`Transformer[C, B]` turns each written `C` into a `B`. Reads are
unchanged: the new optic is an `Optic[S, T, A, C]`, so the focus still
reads as an `A`. This works on eo-generics' derived lenses, which keep
one type for reading and writing; here a lens to an `Address` is
written with an `AddressView`:

```scala mdoc
val addressAsView = lens[User](_.address).transformFocus[AddressView]

addressAsView.replace(AddressView("Paris", "2 Rue"))(ada)
addressAsView.get(ada)
```

On an optic that can change its focus type (`Traversal.pEach`,
`Lens.pLens`, `Prism.pPrism`, `Traversal.first` / `second`, …),
`transformFocus` lets an edit be made in the type the optic reads.
The `Traversal` below reads `RawPrice`s and writes `Price`s;
`transformFocus[RawPrice]` lets it be written with `RawPrice`s, so one
write reprices every element and converts it:

```scala mdoc:silent
import dev.constructive.eo.optics.Traversal

val prices = Traversal.pEach[List, RawPrice, Price]
```

```scala mdoc
prices.transformFocus[RawPrice].modify(r => RawPrice(r.cents * 2))(List(RawPrice(100), RawPrice(250)))
```

`transformFocusPartial[C]` does the same with a
`PartialTransformer[C, B]`. A write returns the new `T`, or the errors
from every written value that didn't convert; across a `Traversal`'s
foci the errors accumulate rather than stopping at the first one. The
original optic's own write runs only when every value converted, so it
never sees a failed one:

```scala mdoc
prices
  .transformFocusPartial[RawPrice]
  .modify(r => RawPrice(r.cents - 150))(List(RawPrice(100), RawPrice(250), RawPrice(120)))
  .asErrorPathMessageStrings
  .toList
```

Each error's path is relative to its focus, because chimney does not
know where the optic was pointing.

## Change the result type

`optic.transformImage[U]` changes the other write-side type: the
result. It runs chimney's `Transformer[T, U]` on whatever the optic
writes back, so every write returns a `U` instead of a `T`. Reads are
unchanged. Combined with `transformFocus`, both write-side types
change: `lens[User](_.address).transformFocus[AddressView]
.transformImage[UserView]` is written with an `AddressView` and
returns a `UserView`.
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
transformers, built once; `map`, `copy` or the
`into…withFieldComputed` DSL where the job needs more than one
transformation), and **hand**-written with neither library. All three
compute the same value, including the intermediate values: the
hand-written code builds the same view, domain value or DTO the other
two do. Optics and transformers are built once, outside the measured
call. The rows marked "…" run the same operation
on inputs that fail.

| Scenario | eo-chimney B/op | chimney B/op | hand B/op | eo ns/op | chimney ns/op | hand ns/op |
|---|---:|---:|---:|---:|---:|---:|
| `transformFocus` — reprice ×2 and retype 64 list elements | 5,464 | 6,144 | 3,072 | 1527 | 1442 | 3719 |
| `transformFocusPartial` — same, validating | 5,592 | 6,256 | 4,624 | 1607 | 1685 | 972 |
| … half the elements invalid | 7,317 | 10,616 | 3,088 | 1724 | 2421 | 644 |
| `transformFocus` — write a view through a derived lens | 48 | 48 | 48 | 12 | 10 | 10 |
| `transformImage` — edit a field, return the view | 96 | 152 | 96 | 28 | 38 | 24 |
| `transformImagePartial` — edit, then validate | 120 | 96 | 64 | 30 | 24 | 12 |
| … the edit fails validation | 184 | 144 | 0 | 48 | 34 | 1 |
| `Chimney.getter` ∘ two lenses — read a field | 48 | 0 | 0 | 22 | 1 | 1 |
| `Chimney.prism` ∘ lens — edit a DTO field | 152 | 128 | 72 | 47 | 49 | 21 |
| … the DTO doesn't decode | 160 | 72 | 0 | 39 | 20 | 1 |
| result fold ∘ lens — decode, read, keep errors | 96 | 72 | 16 | 24 | 21 | 3 |
| … the DTO doesn't decode | 160 | 160 | 16 | 36 | 35 | 3 |
| `lens.patch` — patch a nested part | 48 | 48 | 48 | 11 | 10 | 9 |

One run per row (JDK 25, `-i 5 -wi 3 -f 3 -prof gc`) on a shared 4-vCPU
machine. As on the [benchmarks page](../benchmarks.md), allocation
(B/op) is the figure to compare; nanoseconds only show the direction,
and several error bars are as wide as the number itself.

What the numbers say:

- **`transformFocus` / `transformFocusPartial` allocate less than
  chimney alone.** They reprice and retype in one pass where chimney
  alone needs a `map` and then a transform; on failing input the
  partial version allocates about 30% less than chimney's own error
  collection.
- **`transformImage` matches hand-written code; `transformImagePartial`
  costs 24–40 B more than chimney alone** (the generic optic's carrier
  value around the edit, against chimney's plain `copy` then
  transform).
- **Editing a DTO through a `Codec` decodes once.** The prism ∘ lens
  composition keeps the decoded value for the write instead of
  decoding again (240 → 152 B per edit against the module's first
  version).
- **On a miss the gap to chimney is chimney's own errors.** Every
  object allocated on the "DTO doesn't decode" rows is a chimney error
  value. Called directly, the JIT can remove part of that error chain;
  one call deeper, through the optic, it doesn't.
- **Convert-then-read still allocates the converted value.** The lenses
  now read directly (80 → 48 B), and the remaining 48 B is chimney's
  `UserView` + `AddressView`. The JIT removes those when chimney is
  called alone and only one field is read, but not through the optic
  chain. Reading a field this way is a convenience, not a fast path.

Reproduce with:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -prof gc .*ChimneyBench.*"
```

## Notes

- **Converting a whole value.** When the goal is a new type for the
  whole structure, chimney does that on its own: put a
  `Transformer[A, B]` in scope and `s.transformInto[T]` uses it for
  that field. `transformFocus` and `transformImage` are for keeping an
  optic and changing what it is written with or what it hands back,
  wherever the optic points: containers, sum-type branches, nested
  fields.
- **Dependencies.** chimney 2.x is built on the same macro library as
  eo-generics ([hearth](https://github.com/MateuszKubuszok/hearth)), so
  both use it on one classpath.
