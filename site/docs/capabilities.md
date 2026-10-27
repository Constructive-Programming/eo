# Capabilities

The capability traits — `CanGet`, `CanGetOption`, `CanReverseGet`, `CanModify`, `CanFold`,
`CanPut`, `CanModifyF`, `CanModifyA`, `CanPlace`, `CanTransform` — are the **consuming
surface** of cats-eo, living at the top of the package hierarchy (`dev.constructive.eo`)
because they are how most code should *reference* optics:

> **Consume via capability, construct via optic.** A method that *uses* an optic takes the
> weakest capability trait that covers what it does. Concrete optic types (`Lens`, `Prism`,
> `Getter`, …) appear where optics are built and composed.

## Late binding

A capability erases everything about the optic except what the consumer needs — the carrier
`F[_, _]` and the existential leftover `X` are gone, leaving a plain two-to-four-parameter
trait. That makes it a legal, ergonomic `using` parameter, which is what unlocks the pattern
this library was written for: leave the subject type generic, and demand only the evidence.

```scala mdoc:silent
import dev.constructive.eo.*
import dev.constructive.eo.generics.lens

import java.time.{Duration, Instant}

// This function knows NOTHING about the shape of T — only that an
// Instant can be rewritten inside it.
def adjustTimes[T](delta: Duration)(using CanModify[T, Instant]): T => T =
  _.modify[Instant](_.plus(delta))

// Each domain type supplies its own evidence — a derived lens
// IS the capability, so the given can be declared at the
// capability type directly.
case class Meeting(title: String, start: Instant)
given CanModify[Meeting, Instant] = lens[Meeting](_.start)
```

```scala mdoc
val shift = adjustTimes[Meeting](Duration.ofHours(1))
val standup = Meeting("standup", Instant.EPOCH)
shift(standup)
```

The module that defines `adjustTimes` never depends on `Meeting` — new types opt in by
bringing an optic given (or passing one explicitly at the call site with `(using myLens)`).

## Subject-first syntax

Every capability trait ships `inline` extension methods directly on the subject `S` (or on
the focus `B` / `A` for `CanReverseGet` / `CanPut`), parameterized by the target type `A` —
including derived operations like `replace`, `headOption`, `length`, `exists`, `foci`, and
`transfer`:

```scala mdoc
val delay = Duration.ofHours(1)
standup.modify[Instant](_.plus(delay))
standup.replace[Instant](Instant.EPOCH.plusSeconds(60))
```

### Why it matters: simplicity and multi-focus utility

1. **No throwaway `using` names.** Without subject-first extensions, every consuming method has
   to invent a local identifier (`using cm: CanModify[T, Instant]`) solely to flip the receiver
   and write `cm.modify(f)(t)`. With the extension syntax, the `using` clause stays anonymous
   evidence — `(using CanModify[T, Instant])` — and the body reads naturally off the value:
   `t.modify[Instant](f)` or `t.replace[Instant](x)`.
2. **Direct value operations when evidence is in scope.** At call sites where capability givens
   are already in scope, you act on the value directly (`s.modify[A](_ + x)`, `s.replace[A](x)`,
   `s.get[A]`) without naming an optic or calling `summon`.
3. **Unambiguous multi-focus operations on the same `S`.** Because capabilities are keyed by
   `(S, A)`, a single subject `S` routinely has several capability givens in scope at once — one
   per focus type `A`. The receiver `s` pins `S` and the type argument `[A]` pins `A`, so a
   function that coordinates several foci of the same generic `T` selects each capability inline
   by its focus type:

```scala mdoc:silent
given CanGet[Meeting, String] = lens[Meeting](_.title)
given CanGet[Meeting, Instant] = lens[Meeting](_.start)

def reschedule[T](newTitle: String, delta: Duration)(t: T)(using
    CanGet[T, String],
    CanModify[T, Instant],
): (String, T) =
  (t.get[String], t.modify[Instant](_.plus(delta)))
```

```scala mdoc
reschedule("retro", Duration.ofMinutes(30))(standup)
```

4. **Zero overhead.** Every extension is `inline` (the two-type-parameter operations `foldMap`,
   `modifyF`, `modifyA`, and `transfer` route through `AnyVal` partially-applied wrappers with
   `inline def apply`, so the second type parameter `M` / `G[_]` / `C` is inferred from the
   lambda without boxing), expanding straight to the underlying capability call at compile time.

| Capability | Subject-first extensions | Polymorphic variant |
|---|---|---|
| `CanGet[S, A]` | `s.get[A]` | — |
| `CanGetOption[S, A]` | `s.getOption[A]` | — |
| `CanReverseGet[T, B]` | `b.reverseGet[T]` | — |
| `CanModify[S, A]` | `s.modify[A](f)`, `s.replace[A](a)` | `s.modifyP[T, A, B](f)`, `s.replaceP[T, A, B](b)` |
| `CanFold[S, A]` | `s.foldMap[A](f)`, `s.headOption[A]`, `s.length[A]`, `s.exists[A](p)`, `s.foci[A]` | — |
| `CanPut[T, A]` | `a.put[T](f)` | `a.putP[T, B](f)` |
| `CanModifyF[S, A]` | `s.modifyF[A](f)` | `s.modifyFP[T, A, B](f)` |
| `CanModifyA[S, A]` | `s.modifyA[A](f)` | `s.modifyAP[T, A, B](f)` |
| `CanPlace[T, B]` | `t.place[B](b)`, `t.transfer[B](f)` | — |
| `CanTransform[T, D, B]` | `t.transform[B](f)` | `t.transformP[D, B](f)` |

## The capability × family matrix

Every concrete optic class **implements** its capabilities directly, so a capability call on
a Lens, Prism, Getter, … dispatches straight into the same fused method a direct call uses —
no wrapper, no allocation (see [Benchmarks](benchmarks.md#capability-dispatch)). Optics that
implement no capability directly — results of generic composition, and the concrete
`Traversal` class, which declares no capability mixins yet — are served by a **derived
given** in each capability's companion instead — a thin wrapper delegating to the same
extension methods.

| Capability | Operations | Lens | Iso | Prism | Optional | Traversal | Getter | AffineFold | Fold | Review | Modify |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `CanGet[S, A]` | `get` | ● | ● | | | | ● | | | | |
| `CanGetOption[S, A]` | `getOption` | | | ● | ● | | | ● | | | |
| `CanReverseGet[T, B]` | `reverseGet` | | ● | ● | | | | | | ● | |
| `CanModify[S, A]`¹ | `modify`, `replace` | ● | ● | ● | ● | ○ | | | | | ● |
| `CanFold[S, A]` | `foldMap`, `headOption`, `length`, `exists`, `foci` | ● | ● | ● | ● | ○ | ● | ● | ● | | |
| `CanPut[T, A]`¹ | `put` | | ○ | | | | | | | ○ | |
| `CanModifyF[S, A]`¹ | `modifyF` | ○ | | | | | | | | | |
| `CanModifyA[S, A]`¹ | `modifyA` | ○ | | ○ | ○ | ○ | | | | | |
| `CanPlace[T, B]` | `place`, `transfer` | ²| | | | | | | | | |
| `CanTransform[T, D, B]` | `transform` | ² | | | | | | | | | |

● implemented by the concrete class (hot path) · ○ derived given (wrapper) ·
¹ monomorphic alias; the polymorphic trait carries a `P` suffix (`CanModifyP[S, T, A, B]`, …) ·
² no derived given — these two need `T`-side evidence over the optic's existential `X`, so
they are constructed explicitly: `CanPlace.from(myLens)`.

`CanFold.foci` returns the plain `List[A]` of visited foci, in traversal order. On a raw
`Optic` the same operation is spelled `all` — an alias of `foci`, available on any carrier
with a `ForgetfulFold` instance.

## Coherence: one optic given per `(S, A)`

A capability is keyed by its type parameters only — *which* optic backs it is erased. Treat
capability evidence like any other typeclass:

- **Keep one optic given per `(S, A)` pair in scope.** Two same-typed foci
  (`Person(name: String, alias: String)`) cannot be told apart by `CanModify[Person, String]`;
  wrap such foci in distinct types, or pass the optic explicitly at the call site.
- **Don't split what must agree.** `(using g: CanGet[T, Int], m: CanModify[T, Int])` gives no
  guarantee both came from the same optic. A read-then-write method should demand one
  `CanModify` — its `modify` observes and rewrites in a single pass.

## Writing your own capability-gated methods

If you take an `Optic[…, F]` and a typeclass on `F` in the same signature, put the **optic
first, in the same `using` clause**:

```scala
def render[T, T2, B, F[_, _]](t: T)(using
    o: Optic[T, T2, Json, B, F],  // pins F …
    acc: Accessor[F],             // … before the gate is searched
): Doc = ???
```

Same-clause parameters resolve left to right, so the optic pins the carrier before
`Accessor[F]` is looked up. A context bound (`F[_, _]: Accessor`) desugars — since Scala 3.6
— to a clause placed *before* the explicit one, searching `Accessor[F]` while `F` is still
free: with more than one instance in scope it fails with a misleading error. (Or skip the
issue entirely: take `CanGet[T, Json]` and let the library do this for you.)

## Cats containers as capability evidence

The optic companions derive lawful optics straight from cats typeclasses — as
**constructors, not givens**. A cats container admits several lawful optics at once
(the whole-container traversal, a positional lens, a slot traversal, a grate), so no
single one can be THE canonical given for its `(F[A], A)` pair. You pick the optic you
mean and declare the given in your own scope — exactly the
[coherence rule](#coherence-one-optic-given-per-s-a), with the choice where it belongs.

| cats class | constructor | optic | capabilities |
|---|---|---|---|
| `Traverse[F]` | `Traversal.each` | `Traversal` over the elements | `CanModify` + `CanFold` |
| `Functor[F]` | `Modify.functor` | write-only `Modify` (`= F.map`) | `CanModify` |
| `Foldable[F]` | `Fold[F, A]` | read-only `Fold` | `CanFold` |
| `Bitraverse[F]` | `Traversal.first` / `second` / `both` | `Traversal` over one slot (or every `A` of an `F[A, A]`) | `CanModify` + `CanFold` |
| `Representable[F]` | `Lens.representable(r)` | lawful `Lens` at ONE representation point | `CanGet` + `CanModify` + `CanFold` |
| `Representable[F]` | `Indexed.representable(r)` / `representableP(r)` | fixed-index tabulation with `X = Unit`, monomorphic / polymorphic | full-grid rebuilds; dedicated indexed operations, not generic capability seams |

`Indexed` has dedicated indexed operations; its inclusion above
does not imply generic `Can*` derivations or classical-family composition
bridges. Unit-context composition normalization remains open.

For the capability-backed constructors, two ways to turn one into evidence — bind it as an optic given (the
capability derivations do the rest), or skip optics entirely with a direct capability
instance (the `Can*` traits are SAM-convertible):

```scala mdoc
import dev.constructive.eo.*
import dev.constructive.eo.optics.{Lens, Traversal}

def bump[S](s: S)(using CanModify[S, Int]): S = s.modify[Int](_ + 1)

locally {
  given Traversal[List[Int], List[Int], Int, Int] = Traversal.each
  bump(List(1, 2, 3))
  List(1, 2, 3).modify[Int](_ + 10)
  List(1, 2, 3).replace[Int](0)
}

locally {
  given CanModify[Vector[Int], Int] = f => _.map(f) // direct instance, no optic
  bump(Vector(1, 2))
}

// The positional Lens no whole-container bridge can produce — one point of a function:
val atK = Lens.representable[[x] =>> String => x, Int](using summon)("k")
atK.replace(99)((_: String).length)("k")

atK.replace(99)((_: String).length)("other") // siblings survive
```

Two bridges are left for the reader:
`Applicative` ⇒ `CanReverseGet` — `pure` is `reverseGet`, but ship it as a *direct*
capability given, not a `Review` optic given, or it ambiguates against any element
optic on the same container — and the more interesting `Comonad`/Review dual:
`extract` is a lawful direct `CanGet`, though `extract` + `map` is NOT a lawful Lens
on any multi-position container.

## When not to use capabilities

In a hot inner loop where the optic is statically known, call the concrete optic directly —
the fused members and `inline` composition are the fastest path, and a capability bound
through the *derived given* pays a per-summon wrapper. On concrete optics the capability
methods **are** the fused members, so ordinary API seams lose nothing. Numbers:
[capability dispatch benchmarks](benchmarks.md#capability-dispatch).
