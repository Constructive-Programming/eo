# Cookbook

Runnable patterns for the questions that come up most often. Each
recipe opens with the problem it solves, every fence is compiled by
mdoc against the current library version, and every recipe cites
the source — Penner's *Optics By Example*, Monocle docs, Haskell
`lens` tutorial, circe-optics, or cats-eo itself where the surface
is novel.

These are worked examples, not a tutorial — for the optic families
themselves see the [Optics reference](optics.md). Recipes are
grouped by theme, moving from the everyday to the most
cats-eo-unique capability; skim the headings for a jumping-off
point.

```scala mdoc:silent
import dev.constructive.eo.optics.{Iso, Lens, Optic, Optional, Prism, Traversal}
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus.given   // Functor / Foldable / Traverse for MultiFocus[PSVec] (post-fold)
```

## Theme A — Product editing

### Virtual-field Iso — Celsius ↔ Fahrenheit facade

**Why:** your type stores Celsius, but half your callers think in
Fahrenheit. Expose a `fahrenheit` handle that reads and writes as
if the field were really there — one representation, no drift, and
the unit choice stays an implementation detail callers never see.

```scala mdoc:silent
import dev.constructive.eo.optics.BijectionIso

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

### Total inventory value — multi-field focus into a fold (cats-eo-unique)

**Why:** you have a basket of line items and want one number —
total value — without writing a fold by hand or threading two
fields through a loop. The `lens[S](_.a, _.b)` macro focuses both
`quantity` and `price` as a single Scala 3 `NamedTuple`, so the
multiply-and-sum drops straight into `foldMap`. No Monocle
analogue for the multi-field focus.

```scala mdoc:silent
import cats.instances.list.given     // Traverse[List] for .each
import cats.instances.double.given   // Monoid[Double] for .foldMap
import dev.constructive.eo.generics.lens

case class OrderItem(sku: String, quantity: Int, price: Double)

// Focus both pricing fields at once, then walk every item.
val lineValue =
  Traversal.each[List, OrderItem]
    .andThen(lens[OrderItem](_.quantity, _.price))
```

```scala mdoc
val inventory = List(
  OrderItem("apple",   3, 9.99),
  OrderItem("pear",   10, 2.50),
  OrderItem("lobster", 2, 45.00),
)

// One pass: see each item's (quantity, price) NamedTuple, multiply,
// and let the Double monoid sum the line values into a grand total.
lineValue.foldMap(nt => nt.quantity * nt.price)(inventory)
```

The focus arrives in selector order, so `nt.quantity` and
`nt.price` are named — the lambda reads like the business rule it
encodes. The same `lineValue` optic also writes (`.modify` to
re-price every line); the fold is just the read-side escape. See
[Generics → Multi-field Lens](generics.md) for the full treatment
of the NamedTuple focus.

**Source:** cats-eo internal; fold framing from Penner — *Optics
By Example* ch. 7, <https://leanpub.com/optics-by-example/>.

## Theme B — Sum-branch access

### Prism + Lens — branch into a case, then edit inside

**Why:** you want to rewrite one variant of a sum type — rename
the bound variable of every `Var` node in an AST, say — and leave
every other case untouched, without a hand-written `match` that
re-builds the misses. The Prism selects the branch, the Lens edits
inside it, and misses pass through for free.

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

### …and across the whole tree — `Plated` + `everywhere` (cats-eo-unique)

The Prism above edits *one* `Var`. Give `Expr` a `Plated` and the
**same `varName` optic composes into `everywhere`** — a recursive
optic that reaches every sub-term — so one `.andThen` chain plus
`.modify` uppercases every variable at every depth:

```scala mdoc:silent
import dev.constructive.eo.optics.Plated

// `plate[Expr]` from eo-generics derives this; spelled out by hand so
// the page stays macro-free. It names each case's immediate Expr children.
given Plated[Expr] = Plated.fromChildren(
  {
    case Expr.App(f, x)    => List(f, x)
    case Expr.Lam(_, body) => List(body)
    case Expr.Var(_)       => Nil
  },
  {
    case (Expr.App(_, _), f :: x :: Nil) => Expr.App(f, x)
    case (Expr.Lam(b, _), body :: Nil)   => Expr.Lam(b, body)
    case (leaf, _)                       => leaf
  },
)

// `everywhere` is `transform` in optic form: everywhere.andThen(d).modify(g)
// applies `d` at every node, bottom-up. Reuse `varName` (Prism → Lens) from above.
val everyVarName = Plated.everywhere[Expr].andThen(varName)
```

```scala mdoc
val term = Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))

// Every variable, at every depth: f -> F and the nested y -> Y. The Lam
// binder "y" (a String, not a Var node) is left alone.
everyVarName.modify(_.toUpperCase)(term)
```

`everywhere` composes like any other optic — `.andThen` a Prism to
pick a case, a Lens to reach a field — and the `.modify` runs at every
node, bottom-up and stack-safe to any depth (a million-node tree, or a
100k-deep degenerate spine, won't overflow). When the rewrite is easier
as a plain per-node function, `Plated.transform(f)` is the same engine;
`Plated.universe` / `children` are the read side (every sub-term /
immediate children); `rewrite` repeats an `Expr => Option[Expr]` rule
to a fixpoint. The derivation follows the *exact self-type* rule —
only `Expr`-typed fields are recursion points, so `Lam`'s
`bind: String` stays a leaf. See [Generics → `plate[S]`](generics.md).

**Source:** Mitchell & Runciman — *Uniform Boilerplate and List
Processing* (Uniplate),
<https://ndmitchell.com/downloads/paper-uniform_boilerplate_and_list_processing-30_sep_2007.pdf>;
Haskell `lens` `Control.Lens.Plated`,
<https://hackage.haskell.org/package/lens/docs/Control-Lens-Plated.html>.

## Theme C — Collection walks

### `each` past the traversal — drill, then keep drilling

**Why:** flip the `isMobile` flag on every phone a subscriber owns.
The point isn't the map — it's that `each` keeps composing: walk
into the list, traverse it, *and* drill into a field of each
element, all in one optic. A plain `.map` would make you re-assemble
the surrounding `Subscriber` by hand on the way back out.

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
[Optics → PowerSeries](optics.md#powerseries) and the
[PowerSeries benchmarks](benchmarks.md#powerseries-traversal-with-downstream-composition):
`each` runs 2-3× over a naive `copy`/`map` for dense
Lens-Traversal-Lens chains, amortising toward 1.9× as the
container size grows.

**Source:** Penner — *Optics By Example* ch. 7 (Simple
Traversals), <https://leanpub.com/optics-by-example/>; Gonzalez
— *Control.Lens.Tutorial*,
<https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>.

### Sparse traversal over a Prism (cats-eo-unique)

**Why:** you have a list of results, some `Ok` and some `Err`, and
want to bump only the successes — leaving the failures exactly
where they are. Walking the container *and* matching the branch in
one pass is the "sparse traversal" that's genuinely annoying to
hand-roll; here it's a one-liner.

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

**Why:** you need to change one field deep inside a JSON payload
and pass the rest through untouched — no full decode into a
case-class tree, no re-encode, no decoder for the siblings you
never read. One vignette in three acts: edit a deep leaf, edit
every element of a nested array, then see *why* an edit was a
silent no-op. The [Ior failure-flow
diagram](circe.md#failure-flow) covers the full decision tree.

#### Act 1 — edit one leaf deep in a JSON tree (no decode)

`codecPrism[S]` walks circe's `Json` directly; only the focused
leaf is materialised as `A`:

```scala mdoc:silent
import dev.constructive.eo.circe.codecPrism
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
[`OrderCirceBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/dev/constructive/eo/bench/OrderCirceBench.scala)
suite shows this edit is **flat in document size** — ~1.3 µs whether the record
is small or large — while the decode / `.copy` / re-encode path scales with the
whole payload, so the gap grows from ~3× on a tiny record to ~160× on a large
one. circe-optics' analogous `root.user.address.street` surface forces a full
decode per level; cats-eo's cursor walk does not.

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
cascade is Theme H below.

**Source:** cats-eo internal (`JsonPrism`, `JsonTraversal`);
related to circe-optics' `root.*` idiom
<https://circe.github.io/circe/optics.html>. Structured-failure
inspiration from cats' `Ior` typeclass and
`DecodingFailure.history`.

### jq-style path + predicate

**Why:** the jq one-liner you'd reach for at the shell —
`.items[] | select(.price > 100) | .name |= ascii_upcase`,
"uppercase the name of every item over $100" — but in typed Scala,
over a `Json` you never fully decode. The optic reaches through the
multi-field focus (a Scala 3 NamedTuple) and branches inside the
modify lambda:

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

**Why:** rename every leaf of *your own* recursive tree type —
not a container the library knows about. cats-eo ships no tree
carrier and doesn't need one: if your type has a `cats.Traverse`,
`each` picks it up and walks it. Bring your own instance:

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

## Theme E — Many focuses at once: rewrite, aggregate, broadcast

A `Lens` sees one value; a `Traversal` sees many but only lets you
map over them. `MultiFocus[F]` is the optic for the jobs in between:
rewrite *every* slot of a fixed-shape record with one function,
fold the focused values into a summary and scatter it back across
them, or walk a nested collection and keep composing past it. The
three recipes below are the prototypical jobs; the [MultiFocus
reference](multifocus.md) carries the design story (it unifies five
separate optics from v1 into this one carrier).

### Recipe A — Adjust every channel of a colour at once

**Why:** you have a fixed-shape record of same-typed fields — the
R, G, B of a colour, the x/y/z of a vector, the left/right of a
stereo frame — and you want to hit *all* of them with one function.
There's no collection to traverse and no reason to write three
`.copy` calls: focus the whole tuple and rewrite every slot
uniformly, or fill them all with a constant.

```scala mdoc:silent
import cats.instances.function.given  // Functor[Function1[Int, *]] for .modify
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap}

// Three colour channels — each a Double in [0.0, 1.0].
val rgbMF = MultiFocus.tuple[(Double, Double, Double), Double]
```

```scala mdoc
val violet = (0.5, 0.0, 0.5)

// Brighten all three channels uniformly: the same function runs at
// every slot.
rgbMF.modify(c => (c * 1.4).min(1.0))(violet)

// Replace every slot with the same constant — the broadcast pattern.
rgbMF.replace(0.0)(violet)
```

`.modify` runs the same function at every slot; `.replace(b)` fills
every slot with `b`. This is the *Grate* shape of `MultiFocus`
([details](multifocus.md#sub-shapes)), and it works over any
fixed-arity homogeneous structure, not just tuples. One caveat: the
tuple/Grate shape is map-only — when you need an *effectful*
per-slot rewrite (`.modifyA[G]`), reach for a collection-backed
`MultiFocus.apply[List, A]` instead.

**Source:** Penner — *Grate: yet another optic*,
<https://chrispenner.ca/posts/grate>.

### Recipe B — Summarise a batch, then broadcast or collapse

**Why:** you have a column of numbers from a batch — sensor
readings, line-item amounts, weights — and you need a summary in a
particular shape. Sometimes you want it written back into every
position (so each row carries the batch total as the denominator
for a "share of total", or the mean as a baseline); sometimes you
want the batch collapsed to a single value. The same `MultiFocus[F]`
optic does both — you pick the flavour at the call site:

```scala mdoc:silent
import cats.data.ZipList

val zipMF = MultiFocus.apply[ZipList, Double]
val intListMF = MultiFocus.apply[List, Int]
```

```scala mdoc
// (1) .collectMap broadcasts one summary back to every position —
//     here the batch mean, so every slot ends up holding the average.
//     (.value just unwraps the ZipList so the result prints legibly.)
zipMF.collectMap[Double](zl => zl.value.sum / zl.value.size.toDouble)(
  ZipList(List(1.0, 2.0, 3.0, 4.0))
).value

// (2) .collectList collapses the whole batch to a single value —
//     the total as a one-element result, whatever the input length.
intListMF.collectList(_.sum)(List(1, 2, 3, 4))

// (3) Same optic, .collectMap flavour: the grand total stamped onto
//     every position — the denominator for a later "share of total".
intListMF.collectMap[Int](_.sum)(List(1, 2, 3, 4))
```

**Which flavour?**

- `.collectMap[B](agg: F[A] => B)` keeps the shape — the summary
  lands in every position. Reach for it when a downstream step needs
  the aggregate *alongside* the originals (share-of-total, a
  normalisation baseline).
- `.collectList(agg: List[A] => B)` produces a single-element result
  regardless of input length. Reach for it when you just want the
  summary.

Why two flavours, rather than one derived automatically? The
[MultiFocus reference](multifocus.md#why-two-collect-variants) has
the answer.

**Sources:** Penner — *Kaleidoscopes: lenses that never die*,
<https://chrispenner.ca/posts/kaleidoscopes>; Penner —
*Algebraic lenses*, <https://chrispenner.ca/posts/algebraic>.

### Recipe C — Bulk-edit a nested collection, then read it back

**Why:** the everyday "update all the line items" edit — rename
every order in a cart, bump every price, flag every overdue
invoice. The focus lives two hops deep: into the record, then
across the list it holds. The payoff is that the traversal doesn't
dead-end — the *same* optic keeps composing past `.each` to reach
the exact field you want, and folds back out, so one value gives
you both the write (rename every order) and the read (pull every
name for a report).

```scala mdoc:silent
case class Order(id: Int, name: String, total: Double)
case class Cart(owner: String, orders: List[Order])

val cartOrdersL =
  Lens[Cart, List[Order]](_.orders, (c, os) => c.copy(orders = os))

val orderNameL =
  Lens[Order, String](_.name, (o, n) => o.copy(name = n))

// Three hops, one .andThen each, no manual .morph:
//   Cart → its orders (a List) → each order → that order's name
val everyOrderName =
  cartOrdersL
    .andThen(Traversal.each[List, Order])
    .andThen(orderNameL)   // the downstream hop past .each that pays off
```

```scala mdoc
val cart = Cart("Alice", List(
  Order(1, "apple",  1.0),
  Order(2, "pear",   2.0),
  Order(3, "lobster", 150.0),
))

// Modify every order's name through the chain.
everyOrderName.modify(_.toUpperCase)(cart)

// Read-only escape via .foldMap — Foldable[PSVec] is shipped
// alongside the carrier instances.
everyOrderName.foldMap((s: String) => List(s))(cart)
```

The `.andThen(orderNameL)` *after* the traversal is the part that
earns its keep: because `.each` doesn't dead-end, you compose
toward the field you actually want instead of stopping at the list
and re-drilling by hand — and the same value still `.foldMap`s for
the read side. At scale it stays cheap, within a few percent of a
hand-tuned traversal up to ~1k elements; the
[PowerSeries benchmarks](benchmarks.md#powerseries-traversal-with-downstream-composition)
have the curve.

**Source:** cats-eo internal; performance discussion in
[Optics → PowerSeries](optics.md#powerseries) and the
[benchmarks](benchmarks.md#powerseries-traversal-with-downstream-composition).

## Theme F — Composition

### Three-family ladder: Iso → Lens → Traversal → Prism → Lens

**Why:** real edits cross optic families — an Iso here, a Lens, a
Traversal over a list, a Prism into one variant, another Lens
inside it. In Monocle each adjacent pair needs the right `compose*`
overload; in cats-eo one `.andThen` bridges every seam with no
manual `.morph`. Here: from a `UserTuple`, pull out the
`orders: List[Payment]`, narrow to the `Paid` variant, and bump
their `amount`:

```scala mdoc:nest:silent
final case class UserTuple(name: String, orders: List[Payment])
final case class UserRecord(name: String, orders: List[Payment])

enum Payment:
  case Paid(amount: Double)
  case Pending(amount: Double)
  case Cancelled

val userIso =
  Iso[UserTuple, UserTuple, UserRecord, UserRecord](
    ut => UserRecord(ut.name, ut.orders),
    ur => UserTuple(ur.name, ur.orders),
  )

val userOrders =
  Lens[UserRecord, List[Payment]](_.orders, (u, os) => u.copy(orders = os))

val paidP = Prism[Payment, Payment.Paid](
  {
    case p: Payment.Paid => Right(p)
    case other         => Left(other)
  },
  identity,
)

val paidAmount =
  Lens[Payment.Paid, Double](_.amount, (p, a) => p.copy(amount = a))

val bumpPaid =
  userIso
    .andThen(userOrders)
    .andThen(Traversal.each[List, Payment])
    .andThen(paidP)
    .andThen(paidAmount)
```

```scala mdoc
val input = UserTuple("Alice", List(
  Payment.Paid(10.0),
  Payment.Pending(20.0),
  Payment.Cancelled,
  Payment.Paid(30.0),
))
bumpPaid.modify(_ + 5.0)(input)
```

Five hops, carriers `Direct → Tuple2 → PowerSeries → Either
→ Tuple2`. The cross-carrier `.andThen` does the morph lifting
at each seam — the per-pair Monocle overload table becomes one
`Composer[F, G]` lookup per hop. The full carrier graph is the
[composition lattice](concepts.md#composition-lattice) in the
concepts page.

**Source:** composition demo drawn from Chapuis' hands-on intro
<https://jonaschapuis.com/2018/07/optics-a-hands-on-introduction-in-scala/>
and Borjas' lunar-phase example
<https://tech.lfborjas.com/optics/>.

## Theme G — Effectful modification

### Validate-in-place with `modifyF`

**Why:** the update can fail. Bump `age` by 1, but reject a
negative input with `None` — and keep the validation and the write
as one expression instead of a get / check / set dance. `.modifyF[G]`
lifts an `A => G[B]` through any carrier that admits `Functor[G]`:

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

**Why:** every node in a structure carries an ID you need to
resolve against a database, and the naive `.modifyA` fires one
query per node — the classic N+1. Use the same traversal twice:
`foldMap` collects every ID in one pass, you issue a single batched
query, then `.modify` distributes the results back. 100×–300×
fewer queries with no change to the domain types:

```scala mdoc:silent
case class Node(id: Int, label: String)
case class Payload(id: Int, body: String)

// Stand-in for a DB-backed batch fetch.
def fetchAll(ids: List[Int]): Map[Int, Payload] =
  ids.map(i => i -> Payload(i, s"body-$i")).toMap

val eachLeaf = Traversal.each[List, Node]
```

```scala mdoc
val nodes = List(Node(1, "a"), Node(2, "b"), Node(3, "c"))

// Pass 1: collect every ID into a single list via foldMap.
val allIds = eachLeaf.foldMap((n: Node) => List(n.id))(nodes)

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

### Persist-and-stamp — return the stored object with its DB id

**Why:** a `PUT` (or `POST`) handler receives a draft entity on the
wire, persists it, and must return the *same* entity enriched with
the database-assigned id. The id only exists after the effectful
store runs, so this is the classic "set one field to the result of
an effect" shape — and a derived id-lens makes the stamp a one-liner.
With circe carrying the value across the wire on both ends, the
whole handler is a short pipeline: `decode → store → stamp →
encode`.

```scala mdoc:silent
import io.circe.{Codec, Json}
import io.circe.syntax.*
import io.circe.parser.decode
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import dev.constructive.eo.generics.lens

// Stand-in for your effect type (cats-effect IO, ZIO, Future…).
final class IO[A](val unsafeRun: () => A):
  def map[B](f: A => B): IO[B] = IO(f(unsafeRun()))
  def flatMap[B](f: A => IO[B]): IO[B] = IO(f(unsafeRun()).unsafeRun())
object IO:
  def apply[A](a: => A): IO[A] = new IO(() => a)

final case class BalanceSheet(id: Long, owner: String, total: Double)
object BalanceSheet:
  given Codec.AsObject[BalanceSheet] = KindlingsCodecAsObject.derive

// One derived lens onto the id — no hand-written getter/setter, and
// it works even though `id` shares the case class with two other fields.
val sheetId = lens[BalanceSheet](_.id)

// The effectful store: inserts the row, hands back the generated id.
def save(sheet: BalanceSheet): IO[Long] = IO {
  val _ = sheet // pretend: INSERT … RETURNING id
  42L
}

// Persist, then stamp the returned id back onto the object with the
// lens — `save(...).map(sheetId.replace(_)(sheet))`.
def store(sheet: BalanceSheet): IO[BalanceSheet] =
  save(sheet).map(sheetId.replace(_)(sheet))

// The whole PUT handler: request bytes in, response bytes out.
def handlePut(body: String): IO[String] =
  decode[BalanceSheet](body) match
    case Right(draft) => store(draft).map(_.asJson.noSpaces)
    case Left(err)    => IO(Json.obj("error" -> err.getMessage.asJson).noSpaces)
```

```scala mdoc
// Incoming PUT body — `id` is a placeholder the store overwrites.
val request = """{"id":0,"owner":"Acme Corp","total":1234.5}"""

handlePut(request).unsafeRun()
```

Every step earns its place: `decode` is the wire → domain hop
(circe), `store` is your effect, the stamp is the derived lens
standing in for a hand-written `sheet.copy(id = newId)`, and
`asJson` is the domain → wire hop back out. The lens is the only
optic, and it stays a reusable value — compose it further
(`orderLens.andThen(sheetId)`) the moment the entity nests inside a
larger request. In production you'd reach for an opaque `DbId`
rather than a bare `Long`, and likely a separate `Draft` type
without the id field; the shape of the pipeline doesn't change.

**Source:** the lens-stamps-the-effect-result pattern,
<https://gist.github.com/kryptt/af0a626849f0e3f5b16fcd161a5545e4>;
see also [Generics → Composing into pipelines](generics.md#composing-derived-optics-into-pipelines).

## Theme H — Observable failure (cats-eo-unique)

The next three recipes cover the observability story for the
JSON cursor optics: partial-success walks, parse errors surfaced
through the same chain, and how to classify the failures by
case. The [Ior failure-flow diagram](circe.md#failure-flow) has
the full decision tree.

### Partial-success array walk — `Ior.Both`

**Why:** some elements of the array decode, some don't, and you
want *both* outcomes — the JSON updated where it could be, and a
list of which elements were skipped — not a silent drop.
`Ior.Both(chain, partialJson)` is exactly that shape: every
success lands in the payload, every refusal accumulates into the
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

**Why:** the bytes on the wire might not be JSON at all. Feed the
optic a raw `String` and a parse failure comes back as
`Ior.Left(Chain(JsonFailure.ParseFailed(_)))` — through the *same*
channel as every other failure, so you handle malformed input and
a missing field with one match:

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

**Why:** the edit did nothing and you need to know which step
refused — a missing path? the wrong shape? a decode failure? — so
each cause can go to its own log or metric. Pattern-match the
`JsonFailure` cases; the chain holds one entry per refusal point on
the walk, covering every way a cursor walk can skip:

```scala mdoc:silent
import dev.constructive.eo.circe.JsonFailure

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

## Theme I — Streaming / Kafka

### Kafka payload edit

**Why:** a Kafka consumer gets an `Array[Byte]` payload, needs to
change one field, and re-emits binary on the producer side — on a
hot path where decoding the whole record into a case-class tree per
message is pure overhead. The `AvroPrism` triple-input surface
takes the `Array[Byte]` directly, parses it through apache-avro's
`BinaryDecoder` under a cached reader schema, and threads the modify
back through without ever materialising the full tree:

```scala mdoc:silent
import dev.constructive.eo.avro as eoavro
import dev.constructive.eo.avro.AvroCodec
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord, IndexedRecord}
import org.apache.avro.io.EncoderFactory

case class OrderEvent(orderId: String, customer: String, total: Double)
object OrderEvent:
  given AvroEncoder[OrderEvent] = AvroEncoder.derived
  given AvroDecoder[OrderEvent] = AvroDecoder.derived
  given AvroSchemaFor[OrderEvent] = AvroSchemaFor.derived

// Stand-in for an inbound Kafka record: serialise an OrderEvent to
// binary under the same schema the prism caches.
val outSchema = summon[AvroCodec[OrderEvent]].schema
val sample = OrderEvent("ord-42", "alice", 99.99)
val sampleBytes: Array[Byte] =
  val rec = summon[AvroCodec[OrderEvent]].encode(sample).asInstanceOf[GenericRecord]
  val out = new ByteArrayOutputStream()
  val encoder = EncoderFactory.get().binaryEncoder(out, null)
  val writer = new GenericDatumWriter[GenericRecord](outSchema)
  writer.write(rec, encoder)
  encoder.flush()
  out.toByteArray

// The optic: walk into `customer` once at construction time,
// reuse on every inbound record. Disambiguate the Avro
// `codecPrism` from the circe one imported earlier in this page
// by qualifying through the `eoavro` alias.
val upperCustomer = eoavro.codecPrism[OrderEvent].customer

// Re-serialise the modified IndexedRecord back to binary for the
// producer side. In a real consumer this lives in your sink.
def toBinary(rec: IndexedRecord): Array[Byte] =
  val out = new ByteArrayOutputStream()
  val encoder = EncoderFactory.get().binaryEncoder(out, null)
  val writer = new GenericDatumWriter[GenericRecord](rec.getSchema)
  writer.write(rec.asInstanceOf[GenericRecord], encoder)
  encoder.flush()
  out.toByteArray
```

```scala mdoc
// Kafka hot path: bytes in → modify in place → bytes out.
val outBytes: Array[Byte] =
  toBinary(upperCustomer.modifyUnsafe(_.toUpperCase)(sampleBytes))

// Round-trip witness — decode the output to confirm the customer
// field changed and the rest is preserved.
val outRec = upperCustomer
  .modifyUnsafe(_.toUpperCase)(sampleBytes)
  .asInstanceOf[GenericRecord]
(outRec.get("customer").toString,
 outRec.get("orderId").toString,
 outRec.get("total"))
```

`modifyUnsafe` is the silent-pass-through variant — bad bytes,
missing fields, or decode mismatches leave the input bytes
unmodified rather than allocating an `Ior` chain. That matches
the Kafka consumer budget: at-least-once delivery already
implies the consumer must be tolerant of malformed payloads at
the offset commit boundary, and the per-record allocation cost
of `Ior` is a tax on the happy path. When you DO want the
diagnostic — for a dead-letter queue, say — the default
`.modify(...)` call returns
`Ior[Chain[AvroFailure], IndexedRecord]` for the exact same
fixture; route on the `Ior.Both` / `Ior.Left` shape from there.

The cached reader schema is the load-bearing piece: a single
`codecPrism[OrderEvent]` value pins the schema once, and the
parser reuses it across millions of inbound records. For the
schema-registry case where the reader schema arrives at
runtime, use the explicit-schema overload —
`AvroPrism.codecPrism[OrderEvent](runtimeSchema)` — to bypass
the kindlings-derived schema entirely.

**Source:** cats-eo internal (`AvroPrism`'s triple-input
surface, Unit 10). Background framing on the streaming /
Kafka use case lives in the
[Avro integration intro](avro.md#why-this-exists).

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
- [Avro integration](avro.md) — cursor-backed Avro optics;
  [failure flow](avro.md#failure-flow) for the schema-driven
  Ior decision tree.
- [Migrating from Monocle](migration-from-monocle.md) —
  side-by-side translation guide.
