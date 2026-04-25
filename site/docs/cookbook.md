# Cookbook

Runnable patterns for the questions that come up most often.
Every fence is compiled by mdoc against the current library
version, and every recipe cites the source — Penner's *Optics By
Example*, Monocle docs, Haskell `lens` tutorial, circe-optics, or
cats-eo itself where the surface is novel.

Recipes are grouped by theme. The order inside each theme moves
from the most familiar framing to the most cats-eo-unique
capability; skim the headings for a jumping-off point.

```scala mdoc:silent
import eo.optics.{Iso, Lens, Optic, Optional, Prism, Traversal}
import eo.optics.Optic.*
import eo.data.Forgetful.given    // Accessor[Forgetful] — powers .get on Iso / Getter
import eo.data.Forget.given       // ForgetfulFunctor / Fold / Traverse for Forget[F] carriers
```

## Theme A — Product editing

### Edit a deeply-nested coordinate

The canonical Atom/Molecule walk every Haskell tutorial opens
with — increment a deeply-buried leaf through four composition
hops:

```scala mdoc:silent
case class Point(x: Int, y: Int)
case class Atom(point: Point, mass: Double)
case class Molecule(atoms: List[Atom])

val everyX =
  Lens[Molecule, List[Atom]](_.atoms, (m, as) => m.copy(atoms = as))
    .andThen(Traversal.each[List, Atom])
    .andThen(Lens[Atom, Point](_.point, (a, p) => a.copy(point = p)))
    .andThen(Lens[Point, Int](_.x, (p, x) => p.copy(x = x)))
```

```scala mdoc
val water = Molecule(List(
  Atom(Point(0, 0), 1.0),
  Atom(Point(1, 0), 16.0),
  Atom(Point(2, 0), 1.0),
))
everyX.modify(_ + 1)(water).atoms.map(_.point.x)
```

Same-carrier Lens hops fuse through `Tuple2`'s
`AssociativeFunctor`; the cross-carrier hop into `PowerSeries` at
`.each` happens without a manual `.morph` call. For macro-derived
versions of these Lenses see [Generics](generics.md).

**Source:** Gonzalez — *Control.Lens.Tutorial*,
<https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>.

### Virtual-field Iso — Celsius ↔ Fahrenheit facade

Expose a `fahrenheit` handle on a `Temperature` whose underlying
representation is Celsius. Callers `get` and `reverseGet` without
knowing which unit the type stores:

```scala mdoc:silent
import eo.optics.BijectionIso

case class Temperature(toC: Double)

val celsius = BijectionIso[Temperature, Temperature, Double, Double](
  _.toC,
  Temperature(_),
)
val c2f = BijectionIso[Double, Double, Double, Double](
  c => c * 9.0 / 5.0 + 32.0,
  f => (f - 32.0) * 5.0 / 9.0,
)

val fahrenheit = celsius.andThen(c2f)
```

```scala mdoc
fahrenheit.get(Temperature(100.0))
fahrenheit.reverseGet(32.0)
fahrenheit.modify(_ + 9.0)(Temperature(0.0))
```

`BijectionIso.andThen(BijectionIso)` is fused through the
concrete-subclass override — no per-hop typeclass dispatch —
so the facade-as-public-API refactor story doesn't cost
anything at runtime. See [Iso](optics.md#iso) for the carrier
details.

**Source:** Penner — *Virtual Record Fields Using Lenses*,
<https://chrispenner.ca/posts/virtual-fields>.

### Multi-field NamedTuple focus (cats-eo-unique)

Focus several case-class fields at once and update them
atomically. The `lens[S](_.a, _.b, ...)` macro returns a Lens
whose focus is a Scala 3 `NamedTuple` in selector order — no
Monocle analogue:

```scala mdoc:silent
import eo.generics.lens

case class OrderItem(sku: String, quantity: Int, price: Double)

val qtyAndPrice = lens[OrderItem](_.quantity, _.price)
```

```scala mdoc
val order = OrderItem("abc-123", 3, 9.99)

// Atomic multi-field update: see both fields as a NamedTuple,
// return the NamedTuple with new values.
qtyAndPrice.modify { nt =>
  (quantity = nt.quantity * 2, price = nt.price * 0.9)
}(order)
```

The selector order determines the NamedTuple shape, so
`lens[OrderItem](_.price, _.quantity)` would produce a focus
whose `.price` field comes first. Partial cover → `SimpleLens`;
full cover flips to a `BijectionIso` (next recipe). See
[Generics → Multi-field Lens](generics.md)
for the full treatment.

**Source:** cats-eo internal.

### Full-cover Iso upgrade (cats-eo-unique)

When the selector set spans *every* case field of `S`, the macro
silently upgrades from `SimpleLens` to `BijectionIso`. The user
writes a Lens and the macro delivers a round-trip bijection —
handy at serialization boundaries:

```scala mdoc:silent
case class ProductCode(value: String)

// arity-1 wrapper: single-selector full-cover returns BijectionIso,
// not SimpleLens. The focus type is the Scala 3 NamedTuple
// NamedTuple[("value",), (String,)] — selector name preserved.
val codeIso = lens[ProductCode](_.value)
```

```scala mdoc
val pc = ProductCode("abc-42")
val nt = codeIso.get(pc)
nt.value
codeIso.reverseGet((value = "zzz-99"))
codeIso.modify(n => (value = n.value.toUpperCase))(pc)
```

The return-type switch is deliberate: on full cover there's no
structural complement left, so the lens degenerates into a
bijection. Same holds for multi-field full cover — see
[Generics → Full-cover Iso](generics.md).

**Source:** cats-eo internal.

## Theme B — Sum-branch access

### Prism + Lens — branch into a case, then edit inside

Branch into a specific enum case and modify a field inside the
hit branch; the miss branch passes through unchanged. The pattern
covers both the "update one variant" discriminated-union use case
and the F#-style expression-tree AST case-dispatch:

```scala mdoc:silent
enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

val varP = Prism[Expr, Expr.Var](
  {
    case v: Expr.Var => Right(v)
    case other       => Left(other)
  },
  identity,
)

val varName =
  varP.andThen(Lens[Expr.Var, String](_.name, (v, n) => v.copy(name = n)))
```

```scala mdoc
// Hit branch: the Var's name is uppercased.
varName.modify(_.toUpperCase)(Expr.Var("x"))

// Miss branch: passes through, the Lam is untouched.
varName.modify(_.toUpperCase)(Expr.Lam("y", Expr.Var("y")))
```

The Prism (`Either`) and Lens (`Tuple2`) compose directly;
`.andThen` summons `Composer[Either, Tuple2]` via
`Morph.bothViaAffine` under the hood (both sides lift into
`Affine`, then bridge). Same story for Scala 3 enums derived
through the `prism[S, A]` macro — see
[Generics → `prism[S, A]`](generics.md).

**Source:** Baeldung — *Monocle Optics*,
<https://www.baeldung.com/scala/monocle-optics>; framing from
Wlaschin — *Domain Modeling Made Functional*, ch. 4,
<https://pragprog.com/titles/swdddf/domain-modeling-made-functional/>.

## Theme C — Collection walks

### `each` vs. `forEach` — the composable traversal and its tight twin

cats-eo ships two Traversal carriers for two different situations.
Reach for `Traversal.forEach` when the chain terminates at the
traversal — map once and done, no downstream optic:

```scala mdoc:silent
import cats.instances.list.given

val bumpList = Traversal.forEach[List, Int, Int]
```

```scala mdoc
bumpList.modify(_ + 1)(List(1, 2, 3))
bumpList.foldMap(identity[Int])(List(1, 2, 3))   // sum
```

Reach for `Traversal.each` when the chain continues past the
traversal — the same map, then drill further into each element:

```scala mdoc:silent
case class Dial(isMobile: Boolean, number: String)
case class Subscriber(phones: List[Dial])

val everyMobile =
  Lens[Subscriber, List[Dial]](_.phones, (s, ps) => s.copy(phones = ps))
    .andThen(Traversal.each[List, Dial])
    .andThen(Lens[Dial, Boolean](_.isMobile, (d, m) => d.copy(isMobile = m)))
```

```scala mdoc
everyMobile.modify(!_)(Subscriber(List(
  Dial(isMobile = false, "555-0001"),
  Dial(isMobile = true,  "555-0002"),
)))
```

The cost tradeoff is documented in
[Optics → Traversal](optics.md#traversal) and the
[PowerSeries benchmarks](benchmarks.md#powerseries-traversal-with-downstream-composition):
`each` runs 2-3× over a naive `copy`/`map` for dense
Lens-Traversal-Lens chains, amortising toward 1.9× as the
container size grows; `forEach` matches the naive path.

**Source:** Penner — *Optics By Example* ch. 7 (Simple
Traversals), <https://leanpub.com/optics-by-example/>; Gonzalez
— *Control.Lens.Tutorial*,
<https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>.

### Sparse traversal over a Prism (cats-eo-unique)

For every element of a container, drill through a Prism and
modify only the hit branch — `Err` elements pass through
unchanged, `Ok` elements get their `value` bumped. This is the
"sparse traversal" pattern that's genuinely annoying to
hand-roll, and the optic spelling is a one-liner:

```scala mdoc:silent
import scala.collection.immutable.ArraySeq

enum Result:
  case Ok(value: Int)
  case Err(reason: String)

val okP = Prism[Result, Result.Ok](
  {
    case o: Result.Ok => Right(o)
    case other        => Left(other)
  },
  identity,
)

val bumpOks =
  Traversal.each[ArraySeq, Result]
    .andThen(okP)
    .andThen(Lens[Result.Ok, Int](_.value, (o, v) => o.copy(value = v)))
```

```scala mdoc
bumpOks.modify(_ + 1)(
  ArraySeq(Result.Ok(10), Result.Err("nope"), Result.Ok(20))
)
```

Performance: the `PowerSeriesPrismBench` suite measures this
shape at ~5× over a hand-rolled naive loop — and that's the
published worst case for `each`, not the typical one. The
per-benchmark curve lives in
[benchmarks → PowerSeries with Prism inner](benchmarks.md#powerseries-traversal-with-downstream-composition).

**Source:** Penner — *Optics By Example* ch. 8 (Traversal
Actions) + ch. 10 (Missing Values),
<https://leanpub.com/optics-by-example/>.

## Theme D — JSON editing and tree walks

### The JSON arc — edit leaf, edit array, diagnose

One vignette in three acts: walk a deep JSON path and modify a
leaf without decoding the siblings; walk every element of a
nested array; and observe *why* an edit was a silent no-op.
The [Ior failure-flow diagram](circe.md#failure-flow) covers
the full decision tree.

#### Act 1 — edit one leaf deep in a JSON tree (no decode)

`codecPrism[S]` walks circe's `Json` directly; only the focused
leaf is materialised as `A`:

```scala mdoc:silent
import eo.circe.codecPrism
import io.circe.Codec
import io.circe.syntax.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject

case class UserAddress(street: String, zip: Int)
object UserAddress:
  given Codec.AsObject[UserAddress] = KindlingsCodecAsObject.derive

case class SiteUser(name: String, address: UserAddress)
object SiteUser:
  given Codec.AsObject[SiteUser] = KindlingsCodecAsObject.derive

val userStreet = codecPrism[SiteUser].address.street
```

```scala mdoc
val userJson = SiteUser("Alice", UserAddress("Main St", 12345)).asJson
userStreet.modifyUnsafe(_.toUpperCase)(userJson).noSpacesSortKeys
```

The
[`JsonPrismBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala)
suite documents a 2× speedup at every depth over the
decode / `.copy` / re-encode path. circe-optics' analogous
`root.user.address.street` surface forces a full decode per
level; cats-eo's cursor walk does not.

#### Act 2 — edit every element of a JSON array

Walk an array without materialising it as a Scala collection;
only the focused leaf of each element is decoded. The `.each`
step splits the Prism into a `JsonTraversal`:

```scala mdoc:silent
case class Item(name: String, price: Double)
object Item:
  given Codec.AsObject[Item] = KindlingsCodecAsObject.derive

case class Basket(owner: String, items: Vector[Item])
object Basket:
  given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
```

```scala mdoc
val basket = Basket("Alice", Vector(Item("apple", 1.0), Item("pear", 2.0)))
val everyItemName = codecPrism[Basket].items.each.name

everyItemName.modifyUnsafe(_.toUpperCase)(basket.asJson).noSpacesSortKeys
```

Per-element failures to decode accumulate into
`Ior.Both(chain, partialJson)` on the default surface — next act.

#### Act 3 — diagnose a silent edit no-op

A deep `.modify` appears to do nothing — which path step
refused? The `*Unsafe` methods preserve the pre-v0.2 silent
behaviour, but the default `modify` returns
`Ior[Chain[JsonFailure], Json]` so the diagnostic is
observable:

```scala mdoc:silent
import cats.data.Ior
import io.circe.Json

// A stump Json missing the `.address` field altogether.
val stump = Json.obj("name" -> Json.fromString("Alice"))
```

```scala mdoc
userStreet.modify(_.toUpperCase)(stump)
```

The `Ior.Both(chain, json)` carries both the pre-v0.2 behaviour
(the unchanged Json) and the diagnostic (the chain). Fold the
chain into a readable message, route each case to its own log
stream, or collapse on the `getOrElse` escape hatch — the full
cascade is Theme I below.

**Source:** cats-eo internal (`JsonPrism`, `JsonTraversal`);
related to circe-optics' `root.*` idiom
<https://circe.github.io/circe/optics.html>. Structured-failure
inspiration from cats' `Ior` typeclass and
`DecodingFailure.history`.

### jq-style path + predicate

"For every `item` whose `price > 100`, uppercase its `name`" —
expressible in jq as
`.items[] | select(.price > 100) | .name |= ascii_upcase`. The
cats-eo rendering reaches through the multi-field focus (Scala 3
NamedTuple) and branches inside the modify lambda:

```scala mdoc:silent
type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
given Codec.AsObject[NamePrice] = KindlingsCodecAsObject.derive

val premiumNames =
  codecPrism[Basket]
    .items
    .each
    .fields(_.name, _.price)
```

```scala mdoc
val mixedBasket = Basket(
  "Alice",
  Vector(
    Item("apple",  1.0),
    Item("lobster", 150.0),
    Item("truffle", 300.0),
  ),
).asJson

premiumNames
  .modifyUnsafe { nt =>
    if nt.price > 100.0 then
      (name = nt.name.toUpperCase, price = nt.price)
    else nt
  }(mixedBasket)
  .noSpacesSortKeys
```

A tighter spelling via `filtered` / `selected` is not in
cats-eo 0.1.0 — track that as AffineFold-adjacent work for the
0.2 cycle. Today the predicate inlines inside the modify lambda,
which is honest about the shape of the work.

**Source:** Penner — *Generalizing 'jq' and Traversal Systems*,
<https://chrispenner.ca/posts/traversal-systems>.

### Recursive rename over a user-defined Tree

Walk a user-supplied tree type — the library doesn't need to
ship a tree carrier when the user's type already has
`cats.Traverse`. Bring your own `Traverse` and `each` picks it
up:

```scala mdoc:silent
import cats.Traverse
import cats.Applicative
import cats.syntax.functor.*

enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

// Hand-rolled Traverse[Tree] — in real code reach for a derivation
// or cats.Reducible instance on your own ADT.
given Traverse[Tree] with
  def foldLeft[A, B](fa: Tree[A], b: B)(f: (B, A) => B): B = fa match
    case Tree.Leaf(a)       => f(b, a)
    case Tree.Branch(l, r)  => foldLeft(r, foldLeft(l, b)(f))(f)
  def foldRight[A, B](fa: Tree[A], lb: cats.Eval[B])(f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] =
    fa match
      case Tree.Leaf(a)       => f(a, lb)
      case Tree.Branch(l, r)  => foldRight(l, foldRight(r, lb)(f))(f)
  def traverse[G[_]: Applicative, A, B](fa: Tree[A])(f: A => G[B]): G[Tree[B]] =
    fa match
      case Tree.Leaf(a)       => f(a).map(Tree.Leaf(_))
      case Tree.Branch(l, r)  =>
        Applicative[G].map2(traverse(l)(f), traverse(r)(f))(Tree.Branch(_, _))

val renameLeaves = Traversal.each[Tree, String]
```

```scala mdoc
val tree: Tree[String] =
  Tree.Branch(
    Tree.Leaf("a"),
    Tree.Branch(Tree.Leaf("b"), Tree.Leaf("c")),
  )
renameLeaves.modify(_.toUpperCase)(tree)
```

Works identically for any other `Traverse[F]` — trees, rose
trees, non-empty lists, user ADTs. The *Iris classifier*
follow-on (algebraic lens over a labelled dataset) is tracked
separately in
[`docs/plans/2026-04-22-002-feat-iris-classifier-example.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/plans/2026-04-22-002-feat-iris-classifier-example.md).

**Source:** Stanislav Glebik — *RefTree talk*,
<https://stanch.github.io/reftree/docs/talks/Visualize/>.

## Theme E — Algebraic / classifier

### AlgLens vignette — partition + z-shift

Two patterns that need the whole `F[A]` visible, not just a
single `A`: (a) split a list by its own mean, and (b) shift
each row's score by the dataset-wide mean. Both are algebraic —
the update function inspects the full classifier to decide what
to do with each element:

```scala mdoc:silent
import eo.data.AlgLens
import eo.data.AlgLens.given

case class Row(id: Int, score: Double)

// Lens whose focus is already the list — we're updating the whole
// List[Row] through the dataset-wide mean.
val rowsL =
  Lens[List[Row], List[Row]](identity, (_, rs) => rs)

// Lift the Lens into AlgLens[List] so the inner focus is a single
// Row but the modify function sees the whole list.
val rowsAlg = AlgLens.fromLensF[List, List[Row], List[Row], Row, Row](rowsL)
```

```scala mdoc
val rows = List(Row(1, 10.0), Row(2, 20.0), Row(3, 30.0))

// z-shift: each row's score minus the column-wide mean.
rowsAlg.modify { r =>
  // The closure fires per-element; to read the whole list we reach
  // back through the original data. In an AlgLens-aware helper
  // method (modifyWithAggregate — future work) the aggregator
  // would be passed in explicitly.
  val mean = rows.map(_.score).sum / rows.length
  r.copy(score = r.score - mean)
}(rows)

// Partition: return only the rows at or above the mean. AlgLens
// accepts rebuild with a different cardinality than the source
// when F is MonoidK-friendly (List is).
rowsAlg.modifyA[List] { r =>
  val mean = rows.map(_.score).sum / rows.length
  if r.score >= mean then List(r) else Nil
}(rows)
```

The shipping surface is `.modify` + a closure that reaches back
to the full list; the `modifyWithAggregate` / `.classify`
conveniences sketched in Penner's blog post are tracked for a
follow-up release. The family rationale — and why `AlgLens[F]`
is a composition *sink* — lives in
[Optics → AlgLens](optics.md#alglens).

**Source:** Penner — *Algebraic lenses*,
<https://chrispenner.ca/posts/algebraic>.

### Kaleidoscope column-wise aggregation

A `Kaleidoscope[F, A]` reduces the entire `F[A]` focus to a
single `B` via an aggregator, broadcasts it back through the
`Reflector[F]`, and returns the rebuilt structure. The
behaviour depends on *which* Reflector you plug in — List gives
cartesian (singleton) broadcast, ZipList gives column-zip,
Const gives monoidal summation:

```scala mdoc:silent
import cats.data.ZipList
import eo.data.Kaleidoscope

val zipK = Kaleidoscope.apply[ZipList, Double]
val listK = Kaleidoscope.apply[List, Int]
```

```scala mdoc
// Column-wise mean: the aggregator sees the whole ZipList, returns
// the mean, Reflector[ZipList] broadcasts it back.
zipK.collect[ZipList, Double](zl => zl.value.sum / zl.value.size.toDouble)(
  ZipList(List(1.0, 2.0, 3.0, 4.0))
)

// Cartesian: Reflector[List] wraps the result in a singleton.
listK.collect[List, Int](_.sum)(List(1, 2, 3, 4))
```

The same optic value at every call site; the semantics track
whichever `Reflector` you plug in. See
[Optics → Kaleidoscope](optics.md#kaleidoscope) for the full
table of Reflector instances and the "when to reach for
Kaleidoscope vs. Grate vs. Traversal" decision note.

**Source:** Penner — *Intro to Kaleidoscopes*,
<https://chrispenner.ca/posts/kaleidoscopes>.

## Theme F — Write-only / read-only escape

### Review — reverse-only build path

Build a `UserId` from a `UUID` through a `Review` so tests and
prod share one construction path — reverse-only, no observable
read side:

```scala mdoc:silent
import java.util.UUID
import eo.optics.Review

case class UserId(value: UUID)

val userIdR: Review[UserId, UUID] = Review(UserId(_))
```

```scala mdoc
userIdR.reverseGet(UUID.fromString("00000000-0000-0000-0000-000000000001"))
```

Name the "build-only" intent as first-class and team members
stop reaching for bare smart constructors. Contrast with
`Getter` (next recipe) for read-only. `Review` deliberately
sits *outside* the `Optic` trait — its lack of a read side
would force an artificial `to` member. See
[Optics → Review](optics.md#review) for the rationale.

**Source:** Penner — *Optics By Example* ch. 13,
<https://leanpub.com/optics-by-example/>.

### Getter — derive a read-only view

Expose a derived projection so callers can't try to write
through it. `Getter[S, A]` wraps a pure `S => A` with
`T = Unit` on the full optic trait — the mismatch with `B`
statically rules out `.modify`:

```scala mdoc:silent
import eo.optics.Getter

case class Shopper(name: String, age: Int)

val nameInitial = Getter[Shopper, Char](_.name.head)
```

```scala mdoc
nameInitial.get(Shopper("Alice", 30))
```

See the composition note at
[Optics → Getter](optics.md#getter) — `Getter.andThen(Getter)`
is handled by composing the underlying `S => A` functions
directly rather than through `Optic.andThen`, because the
`T = Unit` slot doesn't align with the outer `B`.

**Source:** Monocle — Focus docs,
<https://www.optics.dev/Monocle/docs/focus>.

### Setter — opaque bulk write

Encrypt every password in a `UserDb` without exposing the
plaintext to the call site. `Setter[S, A]` writes but doesn't
read — the carrier `SetterF` carries no observable read side:

```scala mdoc:silent
import eo.optics.Setter

case class Users(credentials: Map[String, String])

val encryptAll = Setter[Users, Users, String, String] { f => users =>
  users.copy(credentials = users.credentials.view.mapValues(f).toMap)
}
```

```scala mdoc
encryptAll.modify(pw => s"bcrypt($pw)")(
  Users(Map("alice" -> "s3cret", "bob" -> "hunter2"))
)
```

`Setter` is a composition terminal: `lens.andThen(setter)`
works, `setter.andThen(...)` does not. See the [Setter
section](optics.md#setter) for the intentional asymmetry.

**Source:** Monocle — Optics docs,
<https://www.optics.dev/Monocle/docs/optics>.

### AffineFold — predicate-narrowed read

Return `Some(age)` only when the subject is an adult; no
write-back. `AffineFold` is the 0-or-1 read-only shape with
`T = Unit`:

```scala mdoc:silent
import eo.optics.AffineFold

case class Person(age: Int)

val adultAge: AffineFold[Person, Int] =
  AffineFold(p => Option.when(p.age >= 18)(p.age))
```

```scala mdoc
adultAge.getOption(Person(20))
adultAge.getOption(Person(15))
```

Narrow an existing `Optional` or `Prism` to its read-only
projection via `AffineFold.fromOptional` /
`AffineFold.fromPrism` — both discard the write / build path
but keep the matcher. See
[Optics → AffineFold](optics.md#affinefold) for the
composition-corner note (direct `.andThen` off a Lens into an
AffineFold doesn't type-check; narrow after composing).

**Source:** Penner — *Optics By Example* ch. 10 (Missing
Values), <https://leanpub.com/optics-by-example/>.

## Theme G — Composition

### Three-family ladder: Iso → Lens → Traversal → Prism → Lens

One vignette exercises every `Composer` bridge cats-eo ships,
with no explicit `.morph` calls anywhere. From a `UserTuple`
(structurally the same as `UserRecord`), pull out the
`orders: List[Order]`, filter to the `Paid` variant, and
increment their `amount`:

```scala mdoc:silent
final case class UserTuple(name: String, orders: List[Order])
final case class UserRecord(name: String, orders: List[Order])

enum Order:
  case Paid(amount: Double)
  case Pending(amount: Double)
  case Cancelled

val userIso =
  Iso[UserTuple, UserTuple, UserRecord, UserRecord](
    ut => UserRecord(ut.name, ut.orders),
    ur => UserTuple(ur.name, ur.orders),
  )

val userOrders =
  Lens[UserRecord, List[Order]](_.orders, (u, os) => u.copy(orders = os))

val paidP = Prism[Order, Order.Paid](
  {
    case p: Order.Paid => Right(p)
    case other         => Left(other)
  },
  identity,
)

val paidAmount =
  Lens[Order.Paid, Double](_.amount, (p, a) => p.copy(amount = a))

val bumpPaid =
  userIso
    .andThen(userOrders)
    .andThen(Traversal.each[List, Order])
    .andThen(paidP)
    .andThen(paidAmount)
```

```scala mdoc
val input = UserTuple("Alice", List(
  Order.Paid(10.0),
  Order.Pending(20.0),
  Order.Cancelled,
  Order.Paid(30.0),
))
bumpPaid.modify(_ + 5.0)(input)
```

Five hops, carriers `Forgetful → Tuple2 → PowerSeries → Either
→ Tuple2`. The cross-carrier `.andThen` does the morph lifting
at each seam — the per-pair Monocle overload table becomes one
`Composer[F, G]` lookup per hop. The full carrier graph is the
[composition lattice](concepts.md#composition-lattice) in the
concepts page.

**Source:** composition demo drawn from Chapuis' hands-on intro
<https://jonaschapuis.com/2018/07/optics-a-hands-on-introduction-in-scala/>
and Borjas' lunar-phase example
<https://tech.lfborjas.com/optics/>.

## Theme H — Effectful modification

### Validate-in-place with `modifyF`

Bump `age` by 1, but fail with `None` if the input is negative.
`.modifyF[G]` lifts an `A => G[B]` through any carrier that
admits `Functor[G]`:

```scala mdoc:silent
case class Visitor(name: String, age: Int)

val visitorAgeL =
  Lens[Visitor, Int](_.age, (v, a) => v.copy(age = a))
```

```scala mdoc
import cats.syntax.functor.*
import cats.instances.option.*

visitorAgeL.modifyF[Option](age =>
  if age >= 0 then Some(age + 1) else None
)(Visitor("Alice", 30))

visitorAgeL.modifyF[Option](age =>
  if age >= 0 then Some(age + 1) else None
)(Visitor("Alice", -1))
```

`.modifyA[G]` is the `Applicative[G]` variant — reach for it
when the chain has branching effects to combine (traversal +
validation, for instance). `Witherable`-style filter-and-drop
traversal is cross-referenced from
[Penner — *Composable filters using Witherable optics*](https://chrispenner.ca/posts/witherable-optics);
the carrier is deferred to a follow-up release.

**Source:** Monocle / Haskell `lens` classic `traverseOf`,
generalised.

### Batch-load nested IDs — O(N) → O(1) queries (cats-eo-unique)

Each node in a tree carries an ID that needs to resolve to a
DB-backed record. Naive `.modifyA` fires one query per node;
the optic spelling collects all IDs through a `foldMap` pass,
issues one batched query, then `.modify` again to distribute
results. 100×–300× without restructuring the domain types:

```scala mdoc:silent
case class Node(id: Int, label: String)
case class Payload(id: Int, body: String)

// Stand-in for a DB-backed batch fetch.
def fetchAll(ids: List[Int]): Map[Int, Payload] =
  ids.map(i => i -> Payload(i, s"body-$i")).toMap

val leaves = Traversal.forEach[List, Node, Node]
val eachLeaf = Traversal.each[List, Node]
```

```scala mdoc
val nodes = List(Node(1, "a"), Node(2, "b"), Node(3, "c"))

// Pass 1: collect every ID into a single list via foldMap.
val allIds = leaves.foldMap((n: Node) => List(n.id))(nodes)

// Pass 2: issue ONE query for the whole set.
val byId = fetchAll(allIds)

// Pass 3: broadcast the resolved payloads back through the same
// traversal. The structure is preserved; only the labels shift.
eachLeaf.modify(n => n.copy(label = byId(n.id).body))(nodes)
```

The pattern generalises to trees, graphs, nested containers,
anything with a `Traverse`. The two-pass idiom is what cats-eo's
`foldMap` + `modify` pair already enables out of the box — no
cats-eo-specific API to learn.

**Source:** Penner — *Using traversals to batch database
queries*, <https://chrispenner.ca/posts/traversals-for-batching>.

## Theme I — Observable failure (cats-eo-unique)

The next three recipes cover the observability story for the
JSON cursor optics: partial-success walks, parse errors surfaced
through the same chain, and how to classify the failures by
case. The [Ior failure-flow diagram](circe.md#failure-flow) has
the full decision tree.

### Partial-success array walk — `Ior.Both`

Across a mixed-failure array, produce the updated JSON *and*
the per-element miss list. `Ior.Both(chain, partialJson)` is
the observable shape — every element that *did* succeed is
reflected in the payload, every refusal accumulates into the
chain:

```scala mdoc:silent
case class SimpleItem(name: String)
object SimpleItem:
  given Codec.AsObject[SimpleItem] = KindlingsCodecAsObject.derive

case class SimpleBasket(owner: String, items: Vector[SimpleItem])
object SimpleBasket:
  given Codec.AsObject[SimpleBasket] = KindlingsCodecAsObject.derive
```

```scala mdoc
val brokenArr = Json.arr(
  SimpleItem("x").asJson,
  Json.fromString("oops"),          // not a SimpleItem
  SimpleItem("z").asJson,
)
val brokenBasket =
  Json.obj("owner" -> Json.fromString("Alice"), "items" -> brokenArr)

codecPrism[SimpleBasket]
  .items
  .each
  .name
  .modify(_.toUpperCase)(brokenBasket)
```

circe-optics silently drops per-element failures; cats-eo's
`JsonTraversal` collects them so the caller can decide what to
do. The `getOrElse(input).noSpacesSortKeys` escape hatch
reproduces the pre-v0.2 silent shape byte-for-byte when you
know you don't want the diagnostic.

**Source:** cats-eo internal.

### Parse-error surface — `Json | String` input

When the input is a `String`, unparseable input surfaces as
`Ior.Left(Chain(JsonFailure.ParseFailed(_)))` through the same
chain as every other failure mode:

```scala mdoc:silent
val upperName = codecPrism[SimpleBasket].items.each.name.modify(_.toUpperCase)
```

```scala mdoc
// Happy path: parsed, modified, Ior.Right.
upperName(
  """{"owner":"Alice","items":[{"name":"apple"}]}"""
).map(_.noSpacesSortKeys)

// Parse failure: Ior.Left(Chain(JsonFailure.ParseFailed(_))).
upperName("not json at all")
```

The widened `(Json | String) => _` signature is a supertype of
`Json => _`, so every pre-existing call site compiles
unchanged. Parse cost is zero when the input is already a
`Json`; one `io.circe.parser.parse` when it's a `String`.

**Source:** cats-eo internal.

### Classify failures — *why* did the edit no-op?

Pattern-match on each `JsonFailure` case and route to distinct
log / metric streams. The chain collects one entry per refusal
point on the walk; the cases cover every way a cursor walk can
skip:

```scala mdoc:silent
import eo.circe.JsonFailure

def route(chain: cats.data.Chain[JsonFailure]): List[String] =
  chain.toList.map {
    case JsonFailure.PathMissing(step)       => s"miss:    $step"
    case JsonFailure.NotAnObject(step)       => s"shape:   $step (not object)"
    case JsonFailure.NotAnArray(step)        => s"shape:   $step (not array)"
    case JsonFailure.IndexOutOfRange(step,n) => s"bounds:  $step (size=$n)"
    case JsonFailure.DecodeFailed(step, df)  => s"decode:  $step: ${df.message}"
    case JsonFailure.ParseFailed(pf)         => s"parse:   ${pf.message}"
  }
```

```scala mdoc
val stump2 = Json.obj("name" -> Json.fromString("Alice"))
userStreet.modify(_.toUpperCase)(stump2) match
  case Ior.Right(_)        => List("ok")
  case Ior.Both(chain, _)  => route(chain)
  case Ior.Left(chain)     => route(chain)
```

The Ior surface isn't a curiosity — it's production-grade
observability. One vignette demonstrates how; per-case routing
generalises to metrics, structured logs, or error surfaces at
service boundaries.

**Source:** cats-eo internal.

## Further reading

- [Concepts](concepts.md) — the theory behind the unified Optic
  trait and carriers; the
  [composition lattice](concepts.md#composition-lattice)
  diagram maps out every `Composer[F, G]` bridge.
- [Optics reference](optics.md) — the full per-family tour,
  introduced by the
  [family taxonomy](optics.md#family-taxonomy) diagram.
- [Generics](generics.md) — macro-derived `lens[S](...)` and
  `prism[S, A]`.
- [Circe integration](circe.md) — cursor-backed JSON optics;
  [failure flow](circe.md#failure-flow) for the Ior decision
  tree.
- [Migrating from Monocle](migration-from-monocle.md) —
  side-by-side translation guide.
