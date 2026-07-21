# Cookbook

Optics are probably the most powerful unused tool in our arsenal —
this page is the working proof. Runnable patterns for the questions
that come up most often, organised by the three jobs optics do best:

1. **[Navigating structures](#navigating-structures)** — reach one
   branch of a sum, every node of a recursive tree, several fields
   at once, or a field inside bytes you never decode. One `.andThen`
   surface for all of it.
2. **[Decoupling and modularity](#decoupling-and-modularity)** —
   replace a direct dependence on a data type with an optic: depend
   on the *relationship*, ask for the *weakest capability* you need,
   and let independent modules interoperate at the call site.
3. **[Effect threading](#effect-threading)** — turn control flow
   into type definitions: the optic pins *where*, the effect type
   decides *what happens* — abort, accumulate, audit, defer.

The framing follows Hansen — *We Need More Optics*
(<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468>) and
Penner — *Optics By Example*
(<https://leanpub.com/optics-by-example/>). These are worked
examples, not a tutorial — for the optic families themselves see the
[Optics reference](optics.md). Every fence is compiled by mdoc
against the current library version, and every recipe cites its
source.

If you arrive with a task rather than an optic in mind, start here:

| I want to…                                                  | Recipe |
|-------------------------------------------------------------|--------|
| edit one branch of a sum type, misses passing through       | [Visit contingent fields](#visit-contingent-fields) |
| apply one edit at every node of a recursive tree            | [Visit across whole trees](#visit-across-whole-trees) |
| update only the matching elements of a collection           | [Visit through arbitrary structure](#visit-through-arbitrary-structure) |
| fold several fields into one number                         | [Isolate what you need](#isolate-what-you-need) |
| rewrite every slot of a fixed shape, or summarise a batch   | [Compute aggregations](#compute-aggregations) |
| change a field deep in JSON without decoding the payload    | [Edit JSON without decoding](#edit-json-without-decoding) |
| depend on a relationship instead of a data type             | [Require the optic, not the type](#require-the-optic-not-the-type) |
| ask for the weakest capability a function needs             | [Depend only on what's needed](#depend-only-on-what-s-needed) |
| edit Avro bytes on a hot path, no decode                    | [Serdes free pipes](#serdes-free-pipes) |
| move one field between wire formats (Avro ↔ JSON)           | [Inspectable cross-format bridges](#inspectable-cross-format-bridges) |
| law-test the optics my module publishes                     | [Re-usable laws for testing](#re-usable-laws-for-testing) |
| make an in-place update fallible                            | [Validate-in-place with `modifyF`](#validate-in-place-with-modifyf) |
| swap abort / accumulate / audit without touching the optic  | [Structure orthogonal to effects](#structure-orthogonal-to-effects) |
| batch N+1 lookups through one traversal                     | [Batch-load nested IDs](#batch-load-nested-ids) |
| stamp an effect's result back into the entity               | [Persist-and-stamp decode free](#persist-and-stamp-decode-free) |

```scala mdoc:silent
import dev.constructive.eo.optics.{Lens, Optic, Prism, Traversal}
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus.given   // Functor / Foldable / Traverse for MultiFocus[PSVec] (post-fold)
```

## Navigating structures

One branch of a sum; every node of a recursive tree; a sparse walk
over a collection; several fields at once; and the same moves over
JSON you never decode. The recipes ramp by *shape*, and the point
throughout is that no step dead-ends: whatever you just focused,
the next `.andThen` keeps going.

### Visit contingent fields

**Why:** you want to rewrite one variant of a sum type — the bound
variable of a `Var` node in an AST, say — and leave every other
case untouched, without a hand-written `match` that re-builds the
misses. Both hops derive from the type itself: `prism[S, A]`
matches the branch, `lens[S](_.field)` reaches inside it, and one
`.andThen` chains them — no pattern match, no `copy` plumbing, no
optic written by hand.

```scala mdoc:silent
// The AST under edit — hosted at package level in
// dev.constructive.eo.docs (macro-derived optics need top-level
// targets; mdoc wraps every fence in an object):
//
//   enum Expr:
//     case Var(name: String)
//     case App(f: Expr, x: Expr)
//     case Lam(bind: String, body: Expr)
import dev.constructive.eo.docs.Expr
import dev.constructive.eo.generics.{lens, prism}

val varName = prism[Expr, Expr.Var].andThen(lens[Expr.Var](_.name))

// The focus arrives as the case's `(name: String)` NamedTuple —
// the selector covers Var's only field, so the derived optic is a
// full BijectionIso rather than a partial Lens.
val upperVar = varName.modify(v => (name = v.name.toUpperCase))
```

```scala mdoc
// Hit branch: the Var's name is uppercased.
upperVar(Expr.Var("x"))

// Miss branch: passes through, the Lam is untouched.
upperVar(Expr.Lam("y", Expr.Var("y")))
```

The derived Prism rides the `Either` carrier and the derived field
optic composes across the seam through the same `.andThen` as
everything else on this page. When you need a Prism the macro can't
express — a validating match, a normalising re-build — the
hand-rolled `Prism[S, A](sel, emb)` constructor takes the match
explicitly; see [Generics → `prism[S, A]`](generics.md) for what
the derivation covers (enums, sealed traits, union types) and the
full story on the NamedTuple focus.

**Source:** Baeldung — *Monocle Optics*,
<https://www.baeldung.com/scala/monocle-optics>; framing from
Wlaschin — *Domain Modeling Made Functional*, ch. 4,
<https://pragprog.com/titles/swdddf/domain-modeling-made-functional/>.

### Visit across whole trees

`varName` edits exactly the node you hand it: `Var("x")` hits,
`Lam(...)` misses — the nested `Var("y")` inside stays untouched,
because a Prism looks at the *top* of the value, not into it. One
more derivation closes the gap: `plate[Expr]` derives the `Plated`
instance (which fields recurse, which are leaves), and
`everywhere` turns it into a recursive optic that reaches every
sub-term. The **same `varName` optic** then composes into it:

```scala mdoc:silent
import dev.constructive.eo.optics.Plated
import dev.constructive.eo.generics.plate

given Plated[Expr] = plate[Expr]

// `everywhere` is `transform` in optic form: everywhere.andThen(d).modify(g)
// applies `d` at every node, bottom-up. Reuse `varName` from above.
val everyVarName = Plated.everywhere[Expr].andThen(varName)

val upperEveryVar = everyVarName.modify(v => (name = v.name.toUpperCase))
```

```scala mdoc
val term = Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))

// varName alone: the top of `term` is an App, not a Var — a miss,
// nothing changes.
upperVar(term)

// everywhere ∘ varName: every variable at every depth — f -> F and
// the nested y -> Y. The Lam binder "y" (a String, not a Var node)
// is left alone.
upperEveryVar(term)
```

That pair of calls is the whole comparison: `varName` is the
surgical single-node edit, `everyVarName` the same edit quantified
over the tree — and the step between them is one derivation plus
one `.andThen`, not a rewrite. The `.modify` runs at every node,
bottom-up and stack-safe to any depth (a million-node tree, or a
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

### Visit through arbitrary structure

**Why:** you have a list of results, some `Ok` and some `Err`, and
want to bump only the successes — leaving the failures exactly
where they are. Walking the container *and* matching the branch in
one pass is the "sparse traversal" that's genuinely annoying to
hand-roll; here it's a one-liner.

```scala mdoc:silent
import scala.collection.immutable.ArraySeq

// Result (Ok | Err) is hosted in dev.constructive.eo.docs like Expr
// above — enum macro targets need a package-level home under mdoc.
import dev.constructive.eo.docs.Result

val okP = prism[Result, Result.Ok]

val bumpOks =
  Traversal.each[ArraySeq, Result]
    .andThen(okP)
    // Result.Ok is single-field, where the full-cover lens macro returns
    // an Iso with a NamedTuple focus — hand-write the Int lens instead.
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

### Isolate what you need

**Why:** you have a basket of line items and want one number —
total value — without writing a fold by hand or threading two
fields through a loop. The `lens[S](_.a, _.b)` macro focuses both
`quantity` and `price` as a single Scala 3 `NamedTuple`, so the
multiply-and-sum drops straight into `foldMap`.

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

### Compute aggregations

A `Lens` sees one value; a `Traversal` sees many but only lets you
map over them. `MultiFocus[F]` is the optic for the jobs in between:
rewrite *every* slot of a fixed-shape record with one function, or
fold the focused values into a summary and scatter it back across
them. The two recipes below are the prototypical jobs; the
[MultiFocus reference](multifocus.md) carries the design story (it
unifies five separate optics from v1 into this one carrier).

#### Recipe A — Adjust every channel of a colour at once

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

#### Recipe B — Summarise a batch, then broadcast or collapse

**Why:** a column of readings from a batch needs to become a
*report*: each row pairing the original value with its distance
from the batch average, plus the batch-level statistics — mean,
variance, standard deviation — computed once. The same
`MultiFocus[F]` optic covers every shape the report needs:
broadcast a summary into every position, collapse the batch to a
single footer value, rewrite every row *relative to the batch* in
one expression, or fold out the statistical moments in one pass —
you pick the flavour at the call site:

```scala mdoc:silent
case class ReportRow(value: Double, distanceFromMean: Double)

val readings = List(12.0, 14.0, 11.0, 15.0)

val readingsMF = MultiFocus.apply[List, Double]
```

```scala mdoc
// (1) Baseline column — .collectMap broadcasts the batch mean back
//     into every position, keeping the shape.
readingsMF.collectMap[Double](xs => xs.sum / xs.size)(readings)

// (2) Report footer — .collectList collapses the batch to a single
//     summary value, whatever the input length.
readingsMF.collectList(xs => xs.sum / xs.size)(readings)

// (3) The report rows, in one expression — .collectWith is the
//     algebraic-lens universal: the aggregate sees the whole batch
//     ONCE, computes the baseline, and returns the per-row rewrite.
//     The pApply factory makes the walk type-changing, so each
//     Double becomes a ReportRow pairing the original with its
//     distance from the average.
MultiFocus.pApply[List, Double, ReportRow].collectWith { xs =>
  val mean = xs.sum / xs.size
  v => ReportRow(v, v - mean)
}(readings)

// (4) The batch-level statistics for the report footer: count, sum
//     and sum of squares in ONE foldMap pass via the tuple monoid,
//     then the moments fall out.
val (n, sum, sumSq) = readingsMF.foldMap(v => (1, v, v * v))(readings)
val mean     = sum / n
val variance = sumSq / n - mean * mean   // population variance
val stdDev   = math.sqrt(variance)
```

**Which flavour?**

- `.collectMap[B](agg: F[A] => B)` keeps the shape — the summary
  lands in every position. Reach for it when a downstream step needs
  the aggregate *alongside* the originals (share-of-total, a
  normalisation baseline).
- `.collectList(agg: List[A] => B)` produces a single-element result
  regardless of input length. Reach for it when you just want the
  summary.
- `.collectWith(agg: F[A] => A => B)` is the general map-shaped
  collect — the aggregate sees the batch once and the returned
  function runs per position, so batch-relative rewrites
  (share-of-total, distance-from-mean) are one expression.
  `collectMap` is its constant-function special case, and the
  type-changing `MultiFocus.pApply[F, A, B]` factory un-pins the
  element type so the rewrite can emit a different row type
  (step 3). Only `collectList` stays separate — its singleton
  collapse changes the focus count, which no map-shaped combinator
  can express.

Why distinct flavours, rather than one derived automatically? The
[MultiFocus reference](multifocus.md#why-two-collect-variants) has
the answer.

**Sources:** Penner — *Kaleidoscopes: lenses that never die*,
<https://chrispenner.ca/posts/kaleidoscopes>; Penner —
*Algebraic lenses*, <https://chrispenner.ca/posts/algebraic>.

### Edit JSON without decoding

**Why:** you need to change one field deep inside a JSON payload
and pass the rest through untouched — no full decode into a
case-class tree, no re-encode, no decoder for the siblings you
never read. Navigation doesn't stop at the case-class boundary:
`codecPrism[S]` walks circe's `Json` directly, and only the focused
leaf is materialised as `A`:

```scala mdoc:silent
import dev.constructive.eo.circe.codecPrism
import io.circe.Codec
import io.circe.syntax.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject

case class UserAddress(street: String, zip: Int)
object UserAddress:
  given Codec.AsObject[UserAddress] = KindlingsCodecAsObject.derived

case class SiteUser(name: String, address: UserAddress)
object SiteUser:
  given Codec.AsObject[SiteUser] = KindlingsCodecAsObject.derived

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

The `*Unsafe` suffix opts out of the failure channel; the default
`modify` returns `Ior[Chain[JsonFailure], Json]`, so a silent
no-op edit tells you which step refused — the
[circe integration's failure flow](integrations/circe.md#failure-flow)
covers routing it.

**Source:** cats-eo internal (`JsonPrism`); related to circe-optics'
`root.*` idiom <https://circe.github.io/circe/optics.html>.

#### …and every element of a JSON array

Walk an array without materialising it as a Scala collection;
only the focused leaf of each element is decoded. The `.each`
step splits the Prism into a `JsonTraversal`:

```scala mdoc:silent
case class Item(name: String, price: Double)
object Item:
  given Codec.AsObject[Item] = KindlingsCodecAsObject.derived

case class Basket(owner: String, items: Vector[Item])
object Basket:
  given Codec.AsObject[Basket] = KindlingsCodecAsObject.derived
```

```scala mdoc
val basket = Basket("Alice", Vector(Item("apple", 1.0), Item("pear", 2.0)))
val everyItemName = codecPrism[Basket].items.each.name

everyItemName.modifyUnsafe(_.toUpperCase)(basket.asJson).noSpacesSortKeys
```

Per-element failures to decode accumulate into
`Ior.Both(chain, partialJson)` on the default surface — the
[circe integration's failure flow](integrations/circe.md#failure-flow)
shows how to route the chain.

**Source:** cats-eo internal (`JsonTraversal`).

## Decoupling and modularity

Because an optic is a concrete, portable value relating two types,
you can replace a direct dependence on a *data type* with a
dependence on the *relationship*: a function that needs "every
`Instant` in whatever you give me" asks for the optic, not the
type. You can also say precisely how much you need — the guarantees
of an Iso, or just the freedom of a fold — and modules that share
no types at all can still interoperate at the call site, or even at
the wire-format level.

### Require the optic, not the type

**Why:** the article's `adjustSheetTimes` shouldn't have to know
about `BalanceSheet`. Any function whose real job is "apply `f` at
every `Instant`" can take the optic as its parameter and let the
caller decide what structure it runs against — the dependence on
the concrete type evaporates:

```scala mdoc:silent
import java.time.Instant
import dev.constructive.eo.data.{MultiFocus, PSVec}

def adjustTimes[S](t: Optic[S, S, Instant, Instant, MultiFocus[PSVec]])(
    f: Instant => Instant
): S => S = t.modify(f)
```

Any structure that can point at its `Instant`s gets the behaviour —
all it takes is that structure's own optic. A record whose
timestamps sit in two fields, and a bare collection, are served by
the same function:

```scala mdoc:silent
case class Meeting(subject: String, start: Instant, end: Instant)

val meetingTimes =
  Lens[Meeting, (Instant, Instant)](
    m => (m.start, m.end),
    (m, se) => m.copy(start = se._1, end = se._2),
  ).andThen(Traversal.two[(Instant, Instant), (Instant, Instant), Instant, Instant](
    _._1,
    _._2,
    (_, _),
  ))

val bumpMeeting = adjustTimes(meetingTimes)(_.plusSeconds(3600))
val bumpAll     = adjustTimes(Traversal.each[List, Instant])(_.plusSeconds(3600))
```

```scala mdoc
bumpMeeting(Meeting("standup", Instant.EPOCH, Instant.EPOCH.plusSeconds(900)))
bumpAll(List(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
```

`Meeting` and `List[Instant]` share no interface, no parent trait,
no typeclass — the optic *is* the interface. This is the article's
central move: interoperability between otherwise wholly independent
modules, done at the call site, with the structure-owning module
publishing one optic value instead of exposing its shape.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468>
("Modular Application Design").

### Re-use an optic across representations

**Why:** two subsystems rarely agree on representation — one thinks
in UTC `Instant`s, another in wall-clock `LocalDateTime`s; one holds
the domain record, another the wire tuple it serialises to. An optic
is a value relating two types, and `Optic` carries two lawful
`cats.arrow.Profunctor` instances that let you *re-aim* a published
optic instead of re-deriving it: `Optic.innerProfunctor` maps the
focus pair (what the optic yields, and what it accepts back), and
`Optic.outerProfunctor` maps the source pair (what it reads from,
and what it rebuilds).

Re-aim the focus — `meetingTimes` above yields `Instant`s, but the
calendar subsystem edits wall-clock times:

```scala mdoc:silent
import java.time.{LocalDateTime, ZoneOffset}

val meetingWallClock =
  Optic
    .innerProfunctor[Meeting, Meeting, MultiFocus[PSVec]]
    .dimap(meetingTimes)((wall: LocalDateTime) => wall.toInstant(ZoneOffset.UTC))(
      LocalDateTime.ofInstant(_, ZoneOffset.UTC)
    )
```

```scala mdoc
meetingWallClock.modify(_.plusHours(2))(
  Meeting("standup", Instant.EPOCH, Instant.EPOCH.plusSeconds(900))
)
```

Re-aim the source — the scheduling gateway never sees `Meeting`, only
the `(startEpochSecond, endEpochSecond)` pair it puts on the wire.
The same optic serves it too:

```scala mdoc:silent
val wireTimes =
  Optic
    .outerProfunctor[Instant, Instant, MultiFocus[PSVec]]
    .dimap(meetingTimes)((p: (Long, Long)) =>
      Meeting("wire", Instant.ofEpochSecond(p._1), Instant.ofEpochSecond(p._2))
    )(m => (m.start.getEpochSecond, m.end.getEpochSecond))
```

```scala mdoc
wireTimes.modify(_.plusSeconds(3600))((0L, 900L))
```

The same moves cover any representation split: UTF-8 vs UTF-16 text
(dimap through the re-encoding pair), big- vs little-endian words
(dimap through `java.lang.Long.reverseBytes`), or a legacy envelope
around the record you actually care about.

One caveat: `dimap` returns an anonymous `Optic`, so the generic
capability surface (`.modify`, `.foldMap`, …) applies but
concrete-class fused overloads are erased. When the concrete optic
ships its own input-side mapping — `Prism.tearFrom` / `mendFrom` —
prefer that form on hot paths.

### Depend only on what's needed

**Why:** the recipe above still pins the carrier — it accepts
traversals only, and the optic travels as an explicit argument.
Most functions need even less: "something I can fold `Double`s out
of", "something I can rewrite `Double`s through". The
[capability traits](capabilities.md) — `CanGet`, `CanGetOption`,
`CanModify`, `CanFold`, … — are exactly those contracts as proper
types, so the optic arrives as **`using` evidence** and the subject
type stays fully generic. This is the article's
`implicit T: Traversal[T, DateTime]` move, with the carrier erased:

```scala mdoc:silent
import dev.constructive.eo.{CanFold, CanModify}

// Read-side contract: anything that can fold Doubles out of S —
// Lens, Prism, Optional, Traversal, Fold, Iso all qualify.
def total[S](s: S)(using o: CanFold[S, Double]): Double =
  o.foldMap(identity)(s)

// Write-side contract: anything that can rewrite Doubles in place.
def scale[S](k: Double)(using cm: CanModify[S, Double]): S => S =
  cm.modify(_ * k)
```

```scala mdoc:silent
case class Line(desc: String, amount: Double)
case class Invoice(fee: Double, lines: List[Line])

val feeL = lens[Invoice](_.fee)

val lineAmounts =
  lens[Invoice](_.lines)
    .andThen(Traversal.each[List, Line])
    .andThen(lens[Line](_.amount))
```

Concrete optic classes *implement* the capabilities, so a lens can
be handed over as the evidence itself; an optic known only at the
generic `Optic[…, F]` type — like the composed traversal — is bound
as a `given` and the capability is derived on the spot:

```scala mdoc
val inv = Invoice(5.0, List(Line("widgets", 10.0), Line("gadgets", 20.0)))

// A Lens IS a CanFold — pass it as the evidence directly.
total(inv)(using feeL)

// The composed traversal is an anonymous Optic; bind it as a given
// and both contracts derive from it.
given Optic[Invoice, Invoice, Double, Double, MultiFocus[PSVec]] = lineAmounts
total(inv)
scale(1.1)(inv)
```

The trait ladder, weakest first: `CanFold` (`foldMap` /
`headOption` / `exists` / `length` / `foci`), `CanGetOption`,
`CanModify` (`modify` / `replace`), `CanGet`, `CanReverseGet`
(build). A signature that demands only `CanFold` accepts
everything; one that demands both `CanGet` and `CanReverseGet`
insists on an Iso. Under the hood each trait is gated by the
matching typeclass on the carrier (`ForgetfulFold[F]`,
`PartialAccessor[F]`, `ForgetfulFunctor[F]`, `Accessor[F]`,
`ReverseAccessor[F]`) — a signature can still take `Optic[…, F]`
plus the gate directly when it genuinely needs the carrier; see
[Capabilities](capabilities.md) for the full matrix, the coherence
rules, and the using-clause ordering footgun that page documents
for hand-written gate signatures.

**Source:** Hansen — *We Need More Optics* ("We can eliminate the
dependence on the BalanceSheet type by requiring a Traversal
instead"; "you are able to specify if you need the guarantees of an
Iso, or the freedom of a Getter").

### Serdes free pipes

**Why:** a Kafka consumer gets an `Array[Byte]` payload, needs to
change one field, and re-emits binary on the producer side. The
conventional answer couples the consumer to the *whole* schema — a
decode into the full case-class tree per message, on a hot path
where that is pure overhead. `AvroPrism` decouples it: the module
depends on one field's optic, not on `OrderEvent`. It IS a
byte-carried optic (`Optic[Array[Byte], Array[Byte], A, A,
Affine]`): it locates the focused field's byte span under a cached
reader schema, decodes only that slice, and writes by splicing the
re-encoded focus back into the payload — no `IndexedRecord`
materialised on either side:

```scala mdoc:silent
import dev.constructive.eo.avro as eoavro
import dev.constructive.eo.avro.AvroCodec
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
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
```

```scala mdoc
// Kafka hot path: bytes in → splice in place → bytes out. No
// re-serialisation step — the prism's write side IS the emit path.
val outBytes: Array[Byte] =
  upperCustomer.modify(_.toUpperCase)(sampleBytes)

// Round-trip witness — decode the output to confirm the customer
// field changed and the rest is preserved.
(upperCustomer.getOption(outBytes),
 eoavro.codecPrism[OrderEvent].getOption(outBytes))
```

`.modify` on the byte optic is silent-pass-through — bad bytes,
missing fields, or decode mismatches leave the input payload
untouched (an `Affine` Miss) rather than allocating an `Ior`
chain. That matches the Kafka consumer budget: at-least-once
delivery already implies the consumer must be tolerant of
malformed payloads at the offset commit boundary, and the
per-record allocation cost of `Ior` is a tax on the happy path.
When you DO want the diagnostic — for a dead-letter queue, say —
flip to the record-carried face:
`upperCustomer.record.modify(...)` accepts the same bytes and
returns `Ior[Chain[AvroFailure], IndexedRecord]`; route on the
`Ior.Both` / `Ior.Left` shape from there.

The cached reader schema is the load-bearing piece: a single
`codecPrism[OrderEvent]` value pins the schema once, and the
parser reuses it across millions of inbound records. For the
schema-registry case where the reader schema arrives at
runtime, use the explicit-schema overload —
`AvroPrism.codecPrism[OrderEvent](runtimeSchema)` — to bypass
the kindlings-derived schema entirely.

**Source:** cats-eo internal (`AvroPrism`'s byte-carried
default surface). Background framing on the streaming /
Kafka use case lives in the
[Avro integration intro](integrations/avro.md#why-this-exists).

### Inspectable cross-format bridges

**Why:** the consumer reads Avro off a topic and must emit JSON
to a partner API — or receives JSON and must re-emit Avro. Two
modules, two wire formats, and the only thing they genuinely share
is one field. The conventional bridge couples them anyway: decode
the whole record into a case class, build a whole output value,
encode it all. With a byte optic on each side of the seam, only the
moved branch is ever decoded and re-encoded: the full object is
**never constructed** on either format. (`AvroJsonBridgeSpec`
proves this with a counting root codec — both bridge directions run
with zero root decodes and zero root encodes; `AvroJsonBridgeBench`
puts B/op numbers on it.)

```scala mdoc:silent
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.jsoniter.JsoniterPrism

given JsonValueCodec[String] = JsonCodecMaker.make

// One byte optic per format, same focus type. Drilled once,
// reused for every message.
val customerAvro = eoavro.codecPrism[OrderEvent].customer
val customerJson = JsoniterPrism.fromPath[String]("$.customer")

// The JSON side's output skeleton. Placeholders must be VALID
// encodings of the branch type — the splice write decodes the
// current focus before replacing it.
val receiptTemplate: Array[Byte] =
  """{"kind":"receipt","customer":"","status":"ok"}""".getBytes("UTF-8")
```

```scala mdoc
// Avro → JSON: slice the branch off the Avro wire form, splice
// it into the JSON template. No OrderEvent, no receipt object.
val receipt = customerAvro.getOption(sampleBytes) match
  case Some(c) => customerJson.replace(c)(receiptTemplate)
  case None    => receiptTemplate
new String(receipt, "UTF-8")
```

```scala mdoc
// JSON → Avro: read the branch from JSON bytes, splice it into
// existing Avro wire bytes. Same guarantee, reversed.
val inbound: Array[Byte] =
  """{"kind":"receipt","customer":"carol","status":"ok"}""".getBytes("UTF-8")
val updatedAvro = customerJson.getOption(inbound) match
  case Some(c) => customerAvro.replace(c)(sampleBytes)
  case None    => sampleBytes

// Witness (this read DOES construct the object — the bridge
// above never did): the branch moved, the siblings survived.
eoavro.codecPrism[OrderEvent].getOption(updatedAvro)
```

Cost model worth knowing: each `.replace` locates the span,
decodes the *current* focus (the `Affine` write carries it),
re-encodes the new one, and allocates one output buffer. Moving
one branch this way is far cheaper than materialising a wide
object — but for many branches per message, or fragment moves
between two *Avro* payloads, prefer `sliceBytes` / `graftBytes`
(no decode at all).

**Source:** `AvroJsonBridgeSpec` (jsoniter module) and
`AvroJsonBridgeBench` (benchmarks) in cats-eo.

### Re-usable laws for testing

A decoupled boundary needs a tested contract. Composing optics
means the combinators are already tested — what's left to check is
*your* instances, the optic values your module publishes. The
discipline suites that gate cats-eo's own carriers ship in
`cats-eo-laws`, so a composed optic like `meetingTimes` gets the
full Traversal rule-set in one `checkAll` instead of hand-written
round-trip tests:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-laws" % "@VERSION@" % Test
```

```scala
import cats.Functor
import dev.constructive.eo.laws.TraversalLaws
import dev.constructive.eo.laws.discipline.TraversalTests

// meetingTimes walks a plain Meeting, so T is the constant type
// lambda — its Functor is one line. Arbitrary[Meeting] and
// Cogen[Instant] come from your ScalaCheck generators.
given Functor[[X] =>> Meeting] with
  def map[A, B](fa: Meeting)(f: A => B): Meeting = fa

checkAll(
  "meetingTimes",
  new TraversalTests[[X] =>> Meeting, Instant]:
    val laws = new TraversalLaws[[X] =>> Meeting, Instant]:
      val traversal = meetingTimes
  .traversal,
)
```

Every optic family has a matching `FooLaws` / `FooTests` pair — the
[migration guide](migration-from-monocle.md#what-stays-the-same) has
the import-swap table.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> ("Re-usable
tests").

## Effect threading

Optics are great at turning a host language's control flow into
just type definitions. The optic fixes *where* — which foci, at
what depth; the effect type `G` you thread through `.modifyF` /
`.modifyA` decides *what the control flow is*: abort on first
failure, accumulate every failure, log alongside the rewrite, defer
into `IO`. Swapping behaviours means swapping `G`, never touching
the optic.

### Validate-in-place with `modifyF`

**Why:** the update can fail. Bump `age` by 1, but reject a
negative input with `None` — and keep the validation and the write
as one expression instead of a get / check / set dance. `.modifyF[G]`
lifts an `A => G[B]` through any carrier that admits `Functor[G]`:

```scala mdoc:silent
case class Visitor(name: String, age: Int)

val visitorAgeL = lens[Visitor](_.age)
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
validation, for instance); the next recipe is exactly that.
`Witherable`-style filter-and-drop traversal is cross-referenced from
[Penner — *Composable filters using Witherable optics*](https://chrispenner.ca/posts/witherable-optics);
the carrier is deferred to a follow-up release.

**Source:** Monocle / Haskell `lens` classic `traverseOf`,
generalised.

### Structure orthogonal to effects

**Why:** the article's `systemVerifier` insight — "what to do is
decided by the effect type" — in its smallest honest form. Take the
`lineAmounts` traversal from the "Depend only on what's needed"
recipe and thread three different `G`s through the *same*
`.modifyA`. The
optic never changes; the control flow does:

```scala mdoc:silent
import cats.data.{ValidatedNel, Writer}
import cats.syntax.validated.*

type Checked[A] = ValidatedNel[String, A]
type Audit[A]   = Writer[List[String], A]

val badInv = Invoice(5.0, List(Line("widgets", -10.0), Line("gadgets", -20.0)))
```

```scala mdoc
// G = Option: abort on the first bad focus — short-circuit control flow.
lineAmounts.modifyA[Option](a =>
  if a > 0 then Some(a * 1.1) else None
)(badInv)

// G = ValidatedNel: visit EVERY focus, accumulate every failure —
// same optic, same lambda shape, completely different control flow.
lineAmounts.modifyA[Checked](a =>
  if a > 0 then (a * 1.1).validNel else s"non-positive amount: $a".invalidNel
)(badInv)

// G = Writer: rewrite AND emit an audit line per focus — the effect
// threads a log through the walk without touching the domain types.
lineAmounts.modifyA[Audit](a =>
  Writer(List(s"scaled $a"), a * 1.1)
)(inv).run
```

The same slot takes `IO` (each focus becomes an effect to
sequence), `Eval` (defer the whole rewrite), or an fs2 `Stream` —
the article's original `systemVerifier` pipes every focus of a
traversal through a verification stream with `.modifyF`, and its
shape is exactly the three calls above with a fancier `G`. The
domain types never learn any of this is happening.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> ("What
to do is decided by the effect type").

### Batch-load nested IDs

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

### Persist-and-stamp decode free

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

// cats.Eval stands in for your effect type (cats-effect IO, ZIO,
// Future…) — deferred, has map/flatMap, already on the classpath.
import cats.Eval

final case class BalanceSheet(id: Long, owner: String, total: Double)
object BalanceSheet:
  given Codec.AsObject[BalanceSheet] = KindlingsCodecAsObject.derived

// One derived lens onto the id — no hand-written getter/setter, and
// it works even though `id` shares the case class with two other fields.
val sheetId = lens[BalanceSheet](_.id)

// The effectful store: inserts the row, hands back the generated id.
def save(sheet: BalanceSheet): Eval[Long] = Eval.always {
  val _ = sheet // pretend: INSERT … RETURNING id
  42L
}

// Persist, then stamp the returned id back onto the object with the
// lens — `save(...).map(sheetId.replace(_)(sheet))`.
def store(sheet: BalanceSheet): Eval[BalanceSheet] =
  save(sheet).map(sheetId.replace(_)(sheet))

// The whole PUT handler: request bytes in, response bytes out.
def handlePut(body: String): Eval[String] =
  decode[BalanceSheet](body) match
    case Right(draft) => store(draft).map(_.asJson.noSpaces)
    case Left(err)    => Eval.now(Json.obj("error" -> err.getMessage.asJson).noSpaces)
```

```scala mdoc
// Incoming PUT body — `id` is a placeholder the store overwrites.
val request = """{"id":0,"owner":"Acme Corp","total":1234.5}"""

handlePut(request).value
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
- [Circe integration](integrations/circe.md) — cursor-backed JSON optics;
  [failure flow](integrations/circe.md#failure-flow) for the Ior decision
  tree.
- [Avro integration](integrations/avro.md) — cursor-backed Avro optics;
  [failure flow](integrations/avro.md#failure-flow) for the schema-driven
  Ior decision tree.
- [Migrating from Monocle](migration-from-monocle.md) —
  side-by-side translation guide.
- Hansen — *We Need More Optics*, The Startup,
  <https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> — the
  article this page's three-part structure follows.
