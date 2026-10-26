# Effect threading

Optics are great at turning a host language's control flow into
just type definitions. The optic fixes *where* — which foci, at
what depth; the effect type `G` you thread through `.modifyF` /
`.modifyA` decides *what the control flow is*: abort on first
failure, accumulate every failure, log alongside the rewrite, defer
into `IO`. Swapping behaviours means swapping `G`, never touching
the optic.

```scala mdoc:silent
import dev.constructive.eo.optics.{Lens, Optic, Prism, Traversal}
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus.given   // Functor / Foldable / Traverse for MultiFocus[PSVec] (post-fold)
```

## Validate-in-place with `modifyF`

**Why:** the update can fail. Bump `age` by 1, but reject a
negative input with `None` — and keep the validation and the write
as one expression instead of a get / check / set dance. `.modifyF[G]`
lifts an `A => G[B]` through any carrier that admits `Functor[G]`:

```scala mdoc:silent
import dev.constructive.eo.generics.lens

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

## Structure orthogonal to effects

**Why:** the article's `systemVerifier` insight — "what to do is
decided by the effect type" — in its smallest honest form. Take the
`lineAmounts` traversal from
[Depend only on what's needed](decoupling.md#depend-only-on-what-s-needed)
and thread three different `G`s through the *same* `.modifyA`. The
optic never changes; the control flow does:

```scala mdoc:silent
// The invoice and its traversal, as in Depend only on what's needed.
case class Line(desc: String, amount: Double)
case class Invoice(fee: Double, lines: List[Line])

val lineAmounts =
  lens[Invoice](_.lines)
    .andThen(Traversal.each[List, Line])
    .andThen(lens[Line](_.amount))

val inv = Invoice(5.0, List(Line("widgets", 10.0), Line("gadgets", 20.0)))
```

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
sequence) or `Eval` (defer the whole rewrite). The article's
original `systemVerifier` pipes the foci through a verification
fs2 `Stream`; mind that `Stream` itself as `G` combines a
traversal's foci as a cartesian product —
[Streaming with fs2](streaming-fs2.md#effectful-modify-in-a-stream)
shows the warning and the shapes that work. The domain types never
learn any of this is happening.

**Source:** Hansen — *We Need More Optics*, The Startup,
<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> ("What
to do is decided by the effect type").

## Batch-load nested IDs

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

## Persist-and-stamp decode free

**Why:** a `PUT` (or `POST`) handler receives a draft entity on the
wire, persists it, and must return the *same* entity enriched with
the database-assigned id. The id only exists after the effectful
store runs, so this is the classic "set one field to the result of
an effect" shape — and a derived id-lens makes the stamp a one-liner.
With circe carrying the value across the wire on both ends, the
whole handler is a short pipeline: `decode → store → stamp →
encode`.

```scala mdoc:silent
import io.circe.Json
import io.circe.syntax.*
import io.circe.parser.decode
import dev.constructive.eo.generics.lens

// cats.Eval stands in for your effect type (cats-effect IO, ZIO,
// Future…) — deferred, has map/flatMap, already on the classpath.
import cats.Eval

// Hosted in dev.constructive.eo.docs with a kindlings-derived
// Codec.AsObject given:
//
//   final case class BalanceSheet(id: Long, owner: String, total: Double)
import dev.constructive.eo.docs.BalanceSheet

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
see also [Generics → Composing into pipelines](../generics.md#composing-derived-optics-into-pipelines).
