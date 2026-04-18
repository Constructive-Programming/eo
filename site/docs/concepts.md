# Concepts

cats-eo unifies every optic family behind one trait:

```scala
trait Optic[S, T, A, B, F[_, _]]:
  type X
  def to:   S      => F[X, A]
  def from: F[X, B] => T
```

Every family — Lens, Prism, Iso, Optional, Setter, Getter, Fold,
Traversal — is a specialisation of this shape differing only in
the **carrier** `F[_, _]`. Composition crosses families by
morphing from one carrier to another rather than hand-rolling
`.andThen` overloads for every pair.

## Existential vs. profunctor encoding

The classical profunctor presentation quantifies *universally*
over a profunctor:

```scala
type Optic[S, T, A, B] = [P[_, _]] => Profunctor[P] ?=> P[A, B] => P[S, T]
```

Each optic is a polymorphic method. Every call site re-runs the
profunctor argument through the universal quantifier.

The **existential** presentation flips the quantifier: a carrier
`F[_, _]` and an existential witness `X` are *exposed* rather
than quantified over. The optic is then a plain pair of
functions:

```scala
(S => F[X, A], F[X, B] => T)
```

Written as a value — not a method. That one shift has three
consequences:

1. **Every optic is a plain `trait` instance.** No polymorphic
   method invocation at the call site, no inlining-visible
   typeclass dispatch through a forall.
2. **The carrier exposes capability.** Whether you can `get`
   depends on whether `F` has an `Accessor[F]` — not on an
   abstract ProfunctorThing. One capability typeclass per
   operation, one instance per carrier.
3. **Cross-family composition is a bridge problem, not a
   polymorphism problem.** Lens → Optional composition comes
   from a `Composer[Tuple2, Affine]` value, not from cleverness
   in the Optic trait itself.

## Carriers

A carrier `F[_, _]` answers: "what shape does the *middle* of
this optic have?"

| Carrier         | Shape                                          | Family                 |
|-----------------|------------------------------------------------|------------------------|
| `Tuple2`        | `(X, A)` — both halves always present          | `Lens`                 |
| `Either`        | `Either[X, A]` — branch present or absent      | `Prism`                |
| `Forgetful`     | `A` — identity; no leftover                    | `Iso`, `Getter`        |
| `Affine`        | `Either[Fst[X], (Snd[X], A)]`                  | `Optional`             |
| `SetterF`       | `(Fst[X], Snd[X] => A)`                        | `Setter`               |
| `Forget[F]`     | `F[A]` — a `Foldable`/`Traverse` container     | `Fold`, `Traversal`    |
| `PowerSeries`   | `(Snd[X], Vect[Int, A])`                       | Composable `Traversal` |
| `FixedTraversal[N]` | Fixed-length tuple of `A`s                  | `Traversal.{two,three,four}` |

What a carrier supports is *exactly* what its typeclass
instances provide:

| Typeclass                            | Unlocks on `Optic[…, F]`                 |
|--------------------------------------|------------------------------------------|
| `Accessor[F]`                        | `.get(s)`                                 |
| `ReverseAccessor[F]`                 | `.reverseGet(b)`                          |
| `ForgetfulFunctor[F]`                | `.modify(f)`, `.replace(b)`               |
| `ForgetfulApplicative[F]`            | `.put(f)`                                 |
| `ForgetfulTraverse[F, Applicative]`  | `.modifyA[G]`, `.all(s)`                  |
| `ForgetfulFold[F]`                   | `.foldMap[M](f)`                          |
| `AssociativeFunctor[F, X, Y]`        | `.andThen(other)` under the same `F`      |
| `Composer[F, G]`                     | `.morph[G]`                               |

One optic trait, one instance per operation per carrier. Adding
a new carrier means supplying the typeclass instances the
operations it wants to support need — not rewriting `Optic` or
the existing families.

## Composition

### Same-carrier: `Optic.andThen`

When two optics share `F`, `Optic.andThen` composes them under
that carrier:

```scala mdoc:silent
import eo.optics.{Lens, Optic}
import eo.optics.Optic.*

case class Address(street: String)
case class Person(address: Address)

val personAddress =
  Lens[Person, Address](_.address, (p, a) => p.copy(address = a))
val addressStreet =
  Lens[Address, String](_.street, (a, s) => a.copy(street = s))

val streetL = personAddress.andThen(addressStreet)
```

Both pieces live in `Tuple2`; `.andThen` requires
`AssociativeFunctor[Tuple2, X, Y]`, which is defined globally
for any `X, Y`.

### Cross-family: `.morph[G]`

When the downstream optic uses a different carrier, morph the
upstream optic first via `Composer[F, G]`:

```scala mdoc:silent
import eo.data.Affine
import eo.optics.Optional

case class Maybe(flag: Option[String])
case class Wrapped(maybe: Maybe)

val mainOnly = Optional[Maybe, Maybe, String, String, Affine](
  getOrModify = m => m.flag.filter(_.startsWith("M")).toRight(m),
  reverseGet  = { case (m, s) => m.copy(flag = Some(s)) },
)

val wrappedMaybe =
  Lens[Wrapped, Maybe](_.maybe, (w, m) => w.copy(maybe = m))

// `.morph[Affine]` lifts the Lens into the Optional's carrier so
// `.andThen(mainOnly)` type-checks under
// `AssociativeFunctor[Affine, X, Y]`.
val mainStreet = wrappedMaybe.morph[Affine].andThen(mainOnly)
```

`Composer[Tuple2, Affine]` is one of the stdlib instances;
[`eo.data.Affine`](https://javadoc.io/doc/dev.constructive/cats-eo_3/latest/api/eo/data/Affine$.html)
ships it. Other bridges: `Tuple2 → SetterF`, `Tuple2 →
PowerSeries`, `Either → Affine`, `Either → PowerSeries`,
`Affine → PowerSeries`, `Forgetful → Tuple2`, `Forgetful →
Either`.

The transitive `Composer.chain` given lets you hop across two
bridges without naming the intermediate.

## Why the existential machinery is worth it

Unifying every optic behind a single trait collapses the usual
per-family surface ten-fold: no `Lens.modify`,
`Prism.getOption`, `Iso.reverseGet` each written separately.
Extensions on `Optic` are written once against a capability
typeclass (`ForgetfulFunctor[F]` for `.modify`) and any family
whose carrier supplies the instance gets the method for free.

The runtime cost is also neutral. Each carrier's
concrete-class specialisation
([`GetReplaceLens`](https://javadoc.io/doc/dev.constructive/cats-eo_3/latest/api/eo/optics/GetReplaceLens.html),
[`MendTearPrism`](https://javadoc.io/doc/dev.constructive/cats-eo_3/latest/api/eo/optics/MendTearPrism.html),
[`BijectionIso`](https://javadoc.io/doc/dev.constructive/cats-eo_3/latest/api/eo/optics/BijectionIso.html))
stores `get`/`replace`/`reverseGet` as plain fields so the hot
path bypasses typeclass dispatch entirely. See the
[benchmark notes](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/README.md)
for side-by-side numbers against Monocle.
