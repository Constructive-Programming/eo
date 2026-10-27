# Generics

Writing a `Lens` or `Prism` by hand is mechanical, repetitive work — a getter, a
setter that rebuilds the whole case class around one changed field, a pattern
match per sum-type branch. `cats-eo-generics` writes all of it for you. Three
macros — `lens`, `prism`, and `plate` — inspect your case classes and enums at
**compile time** and emit the optic directly: no runtime reflection, no derivation
typeclass to summon, no per-field wiring to keep in sync as the type evolves.
What they generate is the same `new S(...)` construction and pattern match you'd
have typed yourself, so a derived optic runs as fast as a hand-written one (it
matches Monocle's specialised classes — see the [benchmarks](benchmarks.md))
while collapsing to a single readable line.

And because each macro returns an ordinary `Optic` value, the payoff goes beyond
saved keystrokes: derived optics **compose** — with each other, with your own
domain functions, and with the serialization layer — so a data transformation,
or an entire request handler, becomes one clean expression instead of a nest of
`copy` calls. [Composing derived optics into
pipelines](#composing-derived-optics-into-pipelines) builds that up.

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-generics" % "@VERSION@"
```

```scala mdoc:silent
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.{lens, prism}
import dev.constructive.eo.docs.{Address, Customer, NameAgePair, Person, Shape, Shape2, Coords, Zip}
```

> Macro-derived optics need their target case classes and enum
> cases to live at a package-level location. Deriving for a type
> nested inside a class fails at compile time with
> "missing outer accessor in class …" — the emitted `new S(...)`
> call carries no outer pointer. The fix is to move the ADT to the
> top level. The page hosts its
> samples in `dev.constructive.eo.docs.*` for that reason — the same `lens` /
> `prism` calls work identically on your own top-level ADTs.

## `lens[S](_.field)`

Point at a field and you get a `Lens` to it. It's a two-step call — `lens[Customer]`
pins the source type, the second call picks the field — which keeps the field
selector fully type-checked against `Customer`.

Coming from Monocle, this is EO's `GenLens[S](_.field)` / `Focus[S](_.field)`,
with three differences: nested paths (`_.a.b`) are not supported in a single
call (chain `.andThen` instead), a selector set covering every field promotes
the result to a `BijectionIso` (the `GenIso` role — see below), and enum cases
work. Likewise, `prism[S, A]` below is `GenPrism` extended to union types.

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

> One surprise at arity 1: on a **1-field case class** the single
> selector already covers every field, so `lens[Wrapper](_.value)`
> returns a `BijectionIso` whose focus is a 1-field NamedTuple —
> `.get` yields `(value = 42)`, not the bare `42`. See the
> full-cover Iso section below.

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

## Composing derived optics into pipelines

A derived optic is just a value of type `Optic[…]`, and that's what makes the
generics module more than a typing shortcut. Three things compose with it for
free:

**Other optics.** `lens` and `prism` results `.andThen` each other — and every
hand-written optic — across carriers, so you build a deep accessor out of small
derived pieces rather than spelling the traversal by hand:
`prism[Event, Event.Purchase].andThen(lens[Purchase](_.amount))`. The
cross-carrier bridging is automatic; see
[Concepts → Cross-family](concepts.md#cross-family-andthen-across-carriers).

**Your own functions.** The focus is an ordinary value, so `.modify`,
`.foldMap`, and friends take *your* domain functions directly — a pricing rule,
a normaliser, a validation. You write the business logic; the optic does the
"reach in, rebuild around it" plumbing. Chaining a few reads as a clean
left-to-right pipeline instead of nested `copy` calls.

**The wire.** Pair a derived optic with a serialization codec and the same
`.get` / `.replace` / `.modify` vocabulary spans the gap between your domain
types and their on-the-wire form: decode once, transform through optics,
re-encode — or, with [eo-circe](integrations/circe.md) / [eo-avro](integrations/avro.md), edit the encoded
form in place and never fully decode at all.

Put together, that turns a request handler into a short pipeline. The
**persist-and-stamp** recipe ([Cookbook → Effect threading](cookbook/effect-threading.md#persist-and-stamp-decode-free)) is the
canonical shape — decode a PUT body into a domain object, store it effectfully,
stamp the database-assigned id back on with a derived id-lens, and re-encode the
result: the whole handler reads as `decode → store → stamp → encode`.

## What `generics` deliberately does not derive

Knowing the module's edges saves a search through the API for things that
aren't there:

- **No Focus-style path DSL.** A nested selector path like
  `lens[Person](_.address.street)` is a compile error, not a deep lens. Chain
  two derivations with `.andThen` instead — composition is the module's whole
  design, and the composed optic fuses just as well.
- **No Traversal derivation.** There is no `each`-style macro; collection
  traversal lives in core (`Traversal.each` and friends) and composes with
  derived lenses and prisms through the same `.andThen`.
- **No per-ADT optic bundles.** Each call derives exactly one optic. Nothing
  generates "all the lenses of `Person`" as a companion-object bundle — you
  ask for the fields you use.
- **No runtime reflection.** Everything happens at compile time; the emitted
  code is the same construction and pattern match you'd write by hand.

## Compile-time diagnostics

All macro failures surface at compile time with explicit messages
prefixed `lens[S]:` for grep-ability:

- **Empty varargs** — `lens[Customer]()` → "requires at least one
  field selector".
- **Non-case-class source** — `lens[SomeInterface](...)` → Hearth's
  `CaseClass.parse` diagnostic.
- **Non-field selector** — `lens[Customer](_.name.toUpperCase)` →
  "selector at position 0 must be a single-field accessor like
  `_.fieldName`. Nested paths (e.g. `_.a.b`) are not yet supported —
  chain two derivations instead:
  `lens[S](_.a).andThen(lens[A](_.b))`."
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

`prism ∘ lens` works naturally through `Composer` bridges — but note what the
composite *is*: a lens drilled through a prism can miss (the shape might be a
`Square`), so the result auto-upgrades to the `Affine` carrier. That's why the
`import dev.constructive.eo.data.Affine` below is load-bearing — the composed
optic's type names `Affine`, so it must be in scope wherever that type is
written down:

```scala mdoc:silent
import dev.constructive.eo.data.Affine

val circleCoordsX =
  prism[Shape2, Shape2.Circle]
    .andThen(lens[Shape2.Circle](_.c))
    .andThen(lens[Coords](_.x))
```

An `Affine` optic has no total `.get` — read through `.getOption`, and writes
(`.modify` / `.replace`) simply no-op on the miss branch:

```scala mdoc
circleCoordsX.getOption(Shape2.Circle(Coords(3, 4), 1.0))
circleCoordsX.getOption(Shape2.Square(Coords(3, 4), 2.0))
circleCoordsX.modify(_ + 10)(Shape2.Circle(Coords(3, 4), 1.0))
circleCoordsX.modify(_ + 10)(Shape2.Square(Coords(3, 4), 2.0))
```

## `plate[S]` — recursive self-traversal (`Plated`)

`plate[S]` derives a `Plated[S]` — the immediate-children traversal behind
`transform` / `transformM` / `rewrite` / `children` / `universe` / `everywhere`
/ `everywhereIn` over a recursive ADT or mutually recursive type family:

- **Single-sorted ADTs** (enums, sealed hierarchies, recursive case classes):
  focuses every field of type `S`, plus children inside `Option[S]`, `List[S]`,
  `Vector[S]`, `ArraySeq[S]`, `Seq[S]`, `Array[S]`, or any `F[S]` with a
  `cats.Traverse[F]` in scope. Fields of other types stay as leftover skeleton.
- **Multiplate families** (`type Lang = Stm | Expr | Var | Typ`, `plate[Lang]`):
  O'Connor's *Multiplate* (see [*Functor is to Lens as Applicative is to Biplate:
  Introducing Multiplate*](https://arxiv.org/pdf/1103.2841), §5) — a multi-sorted
  plate over a Scala 3 union of the family's types, descending into any direct or
  container field whose element type is a member of `S`. See
  [Multiplate families](#multiplate-families) below.

A single-sorted `Plated[S]` is what the paper calls a self-`Biplate`: the
children/rebuild coalgebra the paper proves isomorphic to the applicative
multi-focus traversal (EO's `Traversal` / `.modifyA` side of the same value).

The derived instance backs `Plated.everywhere[S]` (and `Plated.everywhereIn[S, T]`
to keep a specific root sort `T <: S` on a union plate) — a composable `Modify`
that lifts any downstream optic to *every* depth, so
`everywhere[S].andThen(prism).modify(f)` rewrites that focus across the whole
tree. See [Optics → Modify](optics.md#modify) and the
[Cookbook recipe](cookbook/navigating-structures.md#visit-across-whole-trees).

```scala
import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.Plated

enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

// App.f, App.x and Lam.body are the recursion points; Lam.bind is a leaf.
given Plated[Expr] = plate[Expr]

// Uppercase every variable occurrence, anywhere in the tree (stack-safe).
Plated.transform { case Expr.Var(n) => Expr.Var(n.toUpperCase); case e => e }(
  Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))
)
```

### Multiplate families

A Multiplate answers what no single-sorted plate can: in a mutually recursive
family (`Stm` holds `Expr`s through `Var`s and back), `plate[Stm]` sees no child
inside an `Expr`-typed field, and the paper's `Biplate`-per-pair alternative
cannot rewrite two different child types in one traversal. In Scala 3 the whole
family *is* a type — `type Lang = Stm | Expr | Var | Typ` — and `plate[Lang]`
derives one `Plated[Lang]` whose children are every member-typed field across
the union's branches, direct or container-wrapped. None of Haskell's supporting
machinery transfers or is needed: the rank-3 `mkPlate` / `Projector` record
exists to fake a product of coalgebras in a language without untagged sums, and
the union derivation replaces it. `Plated.everywhereIn[Lang, Stm]` keeps the
root sort (`Stm` in, `Stm` out) while walking the whole `Lang` tree; writing one
member sort is a `prism[Lang, Member]` composed after it, whose miss branch is
exactly the paper's `purePlate` pass-through.

One honesty note: writes rebuild each slot at the member type it was declared
with, so a `Lang => Lang` rule that swaps sorts (a `Var` into an `Expr` slot)
k*eeps the original* rather than crashing — sort-preserving rewrites, which the
law suite's `sameSort` hook pins for every union plate.

For a mutually recursive family (`Stm` ⇄ `StmExpr` with `SBlock(ss: List[Stm])`
and leaf `StmVar`), derive `Plated` once on the union and compose `everywhereIn`
with a union `prism` — the same move the paper's headline `rename` example
makes, in one optic chain instead of a `Plate` record:

```scala mdoc:silent
import dev.constructive.eo.docs.{Stm, StmExpr, StmLang, StmTyp, StmVar}
import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.Plated

given Plated[StmLang] = plate[StmLang]

val renameVarsInStm =
  Plated.everywhereIn[StmLang, Stm]
    .andThen(prism[StmLang, StmVar])
    .andThen(lens[StmVar](_.name))
    .modify(v => (name = "_" + v.name))
```

```scala mdoc
val block = Stm.SBlock(List(
  Stm.SDecl(StmTyp.TInt, StmVar("x")),
  Stm.SAss(StmVar("x"), StmExpr.EStm(Stm.SReturn(StmExpr.EVar(StmVar("y"))))),
))
renameVarsInStm(block)
```

Works on enums, sealed hierarchies, recursive case classes, and union types. See
the [cookbook Plated recipe](cookbook/navigating-structures.md#visit-across-whole-trees) for a worked, runnable
example, and the [Spark integration](integrations/spark.md)
for a Multiplate over an existing carrier (`DataType | StructField`).

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
