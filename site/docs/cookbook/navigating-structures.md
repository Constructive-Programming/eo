# Navigating structures

One branch of a sum; every node of a recursive tree; a sparse walk
over a collection; several fields at once; and the same moves over
JSON you never decode. The recipes ramp by *shape*, and the point
throughout is that no step dead-ends: whatever you just focused,
the next `.andThen` keeps going.

```scala mdoc:silent
import dev.constructive.eo.optics.{Lens, Optic, Prism, Traversal}
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus.given   // Functor / Foldable / Traverse for MultiFocus[PSVec] (post-fold)
```

## Visit contingent fields

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
explicitly; see [Generics → `prism[S, A]`](../generics.md) for what
the derivation covers (enums, sealed traits, union types) and the
full story on the NamedTuple focus.

**Source:** Baeldung — *Monocle Optics*,
<https://www.baeldung.com/scala/monocle-optics>; framing from
Wlaschin — *Domain Modeling Made Functional*, ch. 4,
<https://pragprog.com/titles/swdddf/domain-modeling-made-functional/>.

## Visit across whole trees

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
`bind: String` stays a leaf. See [Generics → `plate[S]`](../generics.md).

**Source:** Mitchell & Runciman — *Uniform Boilerplate and List
Processing* (Uniplate),
<https://ndmitchell.com/downloads/paper-uniform_boilerplate_and_list_processing-30_sep_2007.pdf>;
Haskell `lens` `Control.Lens.Plated`,
<https://hackage.haskell.org/package/lens/docs/Control-Lens-Plated.html>.

## Visit through arbitrary structure

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
[benchmarks → PowerSeries with Prism inner](../benchmarks.md#powerseries-traversal-with-downstream-composition).

**Source:** Penner — *Optics By Example* ch. 8 (Traversal
Actions) + ch. 10 (Missing Values),
<https://leanpub.com/optics-by-example/>.

## Isolate what you need

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
[Generics → Multi-field Lens](../generics.md) for the full treatment
of the NamedTuple focus.

**Source:** cats-eo internal; fold framing from Penner — *Optics
By Example* ch. 7, <https://leanpub.com/optics-by-example/>.

## Compute aggregations

A `Lens` sees one value; a `Traversal` visits a container's elements.
`Indexed` addresses a fixed index space, while `MultiFocus[F]`
supports container traversal and aggregation. The two recipes below
show that distinction; see the [MultiFocus reference](../multifocus.md).

### Recipe A — Adjust a fixed-index configuration

**Why:** an environment-indexed configuration has a path for each
coordinate. Read one, rewrite each with its index visible, or fill
the whole space without enumerating or comparing indexes.

```scala mdoc:silent
import cats.Representable
import cats.instances.function.given
import java.nio.file.Path
import dev.constructive.eo.optics.Indexed
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap}

enum Env:
  case Dev, Prod

val readerRepr = summon[Representable.Aux[[a] =>> Env => a, Env]]
val configGlass = Indexed.representable[[a] =>> Env => a, Path](readerRepr)
val config: Env => Path =
  case Env.Prod => Path.of("/opt/app/")
  case Env.Dev  => Path.of("/dev/app/0292103")
```

```scala mdoc
configGlass.at(Env.Dev)(config)
val adjustedConfig = configGlass.modify((env, path) =>
  if env == Env.Dev then path.resolve("logs") else path
)(config)
(adjustedConfig(Env.Dev), adjustedConfig(Env.Prod))
val replacePath = Path.of("/")
val replacedConfig = configGlass.replace(replacePath)(config)
(replacedConfig(Env.Dev), replacedConfig(Env.Prod))
```

```scala mdoc:silent
require(configGlass.at(Env.Dev)(config) == Path.of("/dev/app/0292103"))
require(configGlass.at(Env.Prod)(config) == Path.of("/opt/app/"))
require(adjustedConfig(Env.Dev) == config(Env.Dev).resolve("logs"))
require(adjustedConfig(Env.Prod) == config(Env.Prod))
require(replacedConfig(Env.Dev) == replacePath)
require(replacedConfig(Env.Prod) == replacePath)
```

This representable constructor returns `Indexed.Grate`, the
`X = Unit` specialization. Other glasses retain residual context.
Nested `andThen` uses product indexes to address the full grid;
Unit-context normalization and generic classical-family seams remain
open. For effectful per-element rewrites, use container traversal.

For nested environment/channel settings, type-changing writes, and
labelled sensor grids that retain per-row metadata, continue with
[Indexed: Grates and Glasses](../indexed.md).

**Source:** Penner — *Grate: yet another optic*,
<https://chrispenner.ca/posts/grate>.

### Recipe B — Summarise a batch, then broadcast or collapse

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
- `.collectList(agg: List[A] => B)` supplies a singleton focus vector
  to reconstruction with the observed context. This identity-backed
  `readingsMF` returns a singleton List, but other optics can preserve
  surrounding structure or reject a shape/count mismatch. It is not
  a general promise of a single-element source.
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
[MultiFocus reference](../multifocus.md#why-two-collect-variants) has
the answer.

**Sources:** Penner — *Kaleidoscopes: lenses that never die*,
<https://chrispenner.ca/posts/kaleidoscopes>; Penner —
*Algebraic lenses*, <https://chrispenner.ca/posts/algebraic>.

## Edit JSON without decoding

**Why:** you need to change one field deep inside a JSON payload
and pass the rest through untouched — no full decode into a
case-class tree, no re-encode, no decoder for the siblings you
never read. Navigation doesn't stop at the case-class boundary:
`codecPrism[S]` walks circe's `Json` directly, and only the focused
leaf is materialised as `A`:

```scala mdoc:silent
import dev.constructive.eo.circe.codecPrism
import io.circe.syntax.*

// The payload's case classes — hosted at package level in
// dev.constructive.eo.docs with kindlings-derived codecs (heavy
// derivation macros run in compiled sources, not mdoc fences):
//
//   case class UserAddress(street: String, zip: Int)
//   object UserAddress:
//     given Codec.AsObject[UserAddress] = KindlingsCodecAsObject.derived
//
//   case class SiteUser(name: String, address: UserAddress)   // ditto
import dev.constructive.eo.docs.{SiteUser, UserAddress}

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
[circe integration's failure flow](../integrations/circe.md#failure-flow)
covers routing it.

**Source:** cats-eo internal (`JsonPrism`); related to circe-optics'
`root.*` idiom <https://circe.github.io/circe/optics.html>.

### …and every element of a JSON array

Walk an array without materialising it as a Scala collection;
only the focused leaf of each element is decoded. The `.each`
step splits the Prism into a `JsonTraversal`:

```scala mdoc:silent
// Same hosting pattern — kindlings-derived Codec.AsObject on both:
//
//   case class Item(name: String, price: Double)
//   case class Basket(owner: String, items: Vector[Item])
import dev.constructive.eo.docs.{Basket, Item}
```

```scala mdoc
val basket = Basket("Alice", Vector(Item("apple", 1.0), Item("pear", 2.0)))
val everyItemName = codecPrism[Basket].items.each.name

everyItemName.modifyUnsafe(_.toUpperCase)(basket.asJson).noSpacesSortKeys
```

Per-element failures to decode accumulate into
`Ior.Both(chain, partialJson)` on the default surface — the
[circe integration's failure flow](../integrations/circe.md#failure-flow)
shows how to route the chain.

**Source:** cats-eo internal (`JsonTraversal`).
