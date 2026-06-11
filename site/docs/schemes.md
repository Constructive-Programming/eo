# Recursion schemes

> **Status: exploratory.** This module is an early exploration of *what* recursion schemes as
> optics should look like and *how* they should be shaped — the API is not yet stable. In
> particular the engine threads children through `PSVec` (a `Array[AnyRef]`-backed vector): this is
> very performant but **type-unsafe** — the per-node child results are erased to `AnyRef` and the
> combiner indexes them positionally, so a coalgebra/algebra arity mismatch is a runtime error, not
> a compile error. If you want **named-constructor type safety**, the opt-in typed pattern-functor
> path at the bottom of this page (`cataF`/`anaF`/`hyloF`) gives it — at the cost of writing a
> pattern functor `F` and its `Traverse`/`Project`/`Embed`. The two paths complement each other;
> this `PSVec` path stays the zero-boilerplate default.

`cats-eo-schemes` expresses the recursion schemes **as optics**, so they compose with the rest
of the optic algebra rather than living in a separate world:

| Scheme | Optic | Direction |
|--------|-------|-----------|
| `cata` | `Getter[S, A]` (driven by `Plated[S]`) | fold an existing `S` to an `A` |
| `ana`  | `Review[S, Seed]` | build an `S` from a seed |
| `hylo` | `Getter[Seed, A]` (**fused** — no intermediate `S`) | unfold-and-fold in one pass |

All three run on a single stack-safe, post-order machine (heap-stacked, not JVM-call-stacked),
so they are safe to depths a hand-written recursion would overflow. The examples below use the
circe `Plated[Json]` from `cats-eo-circe` as a concrete recursive `S`.

```scala mdoc:silent
import dev.constructive.eo.schemes.Schemes
import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.Getter
import dev.constructive.eo.optics.Optic.*   // get, andThen, cross
import dev.constructive.eo.circe.platedJson  // given Plated[Json]
import io.circe.Json
```

## `cata` — a fold that is a `Getter`

The algebra sees each node plus its already-folded children. Here, sum every number anywhere
in a JSON document:

```scala mdoc:silent
val sumNumbers: Getter[Json, Int] =
  Schemes.cata[Json, Int]((node, folded) =>
    node.asNumber.flatMap(_.toInt).getOrElse(0) + folded.toList.sum
  )

val doc = Json.obj(
  "a" -> Json.fromInt(1),
  "b" -> Json.arr(Json.fromInt(2), Json.fromInt(3)),
  "c" -> Json.fromString("ignored"),
)
```

```scala mdoc
sumNumbers.get(doc)
```

Because `cata` is a `Getter`, it composes onto any optic that ends in the recursive type:

```scala mdoc:silent
final case class Payload(label: String, body: Json)
val bodySum: Getter[Payload, Int] =
  Getter[Payload, Json](_.body).andThen(sumNumbers)
```

```scala mdoc
bodySum.get(Payload("p", doc))
```

### The algebra as an optic — `cata(Unfold)`

The *input* of a fold is itself a citizen of the optic algebra: a pure algebra
`PSVec[A] => A` is an [`Unfold[A, A, PSVec]`](optics.md#unfold) — the build-only/many
optic — and `cata` accepts it directly. The payoff is that algebras can be **assembled
by optic composition** (`Review ∘ Unfold` post-processes each layer's result,
`Unfold ∘ Review` pre-processes each part) before the engine consumes them:

```scala mdoc:silent
import dev.constructive.eo.optics.{Review, Unfold}

// node-blind algebra (counts nodes) carried as a build-only optic …
val sizeAlg = Unfold.algebra[Int, Int, PSVec](kids => 1 + kids.toList.sum)

// … and a per-layer post-processing step composed in front of it
val weighted = Review[Int, Int](_ * 2).andThen(sizeAlg)

val docSize: Getter[Json, Int] = Schemes.cata[Json, Int](sizeAlg)
```

```scala mdoc
docSize.get(doc)
```

One honesty note: a `PSVec` layer is **node-blind** — the children arrive positionally
with the constructor erased — so a pure `PSVec`-algebra can only express
constructor-independent folds (sizes, counts, child aggregations). Folds that dispatch
on the node (like `sumNumbers` above) use the para-flavored `(S, PSVec[A]) => A`
overload, which stays the primary form on this untyped path.

## `ana` — an unfold that is a `Review`

A coalgebra maps a seed to its child seeds plus a builder for the node (the canonical anamorphism
shape — children and assembly decided together). It returns a `Review`, so `.reverseGet` runs the
unfold:

```scala mdoc:silent
// seed n builds a right-nested pair tree of depth n, leaves = 1
val buildTree =
  Schemes.ana[Int, Json] { n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Json]) => Json.fromInt(1))
    else (PSVec.of(0, n - 1), (ks: PSVec[Json]) => Json.arr(ks(0), ks(1)))
  }
```

```scala mdoc
buildTree.reverseGet(2)
```

## `hylo` — the fused refold

`hylo` unfolds and folds in one pass, **never building the intermediate structure**. The same
`expand` drives the unfold; `alg` folds the children's results directly:

```scala mdoc:silent
// fused: count the leaves of that same tree, with no Json ever constructed
val countLeaves: Getter[Int, Int] =
  Schemes.hylo[Int, Int](
    expand = n => if n <= 0 then PSVec.empty[Int] else PSVec.of(0, n - 1),
    alg = (n, rs) => if n <= 0 then 1 else rs.toList.sum,
  )
```

```scala mdoc
countLeaves.get(2)
countLeaves.get(20)  // stack-safe; no 2^20-node tree is materialised
```

## `cross` — build, then read (structure-preserving)

The core `cross` combinator joins a *build* optic to a *read* optic at their shared middle type.
It is `self.reverse.andThen(that)`: it flips the reversible builder so it reads what it would have
built, then composes. The result is a **full `Optic`**, not a collapsed getter — its read
capability follows the composed carrier (`.get` through a Getter, `.getOption` through a Prism,
`.foldMap` through a Fold), and it works **cross-carrier** via `Morph`. With `ana` and `cata` it
is exactly the **materializing** hylo (`cata ∘ ana` — it *does* build the `S`):

```scala mdoc:silent
val refoldSum = buildTree.cross(sumNumbers) // Optic[Int, Unit, Int, Unit, Direct]
```

```scala mdoc
refoldSum.get(2)
```

`refoldSum` and the fused `countLeaves` compute the same value (the **hylo law**,
`hylo == cata ∘ ana`); the difference is that the fused `hylo` never allocates the intermediate
`Json`, while `ana.cross(cata)` builds it and then folds. Reach for the fused `hylo` when the
intermediate structure is large; reach for `ana` / `cata` / `cross` when you want the
intermediate `S`, or want to drop the schemes into a larger optic pipeline.

`cross` is overloaded (like `andThen`): a trait-member overload composes under a single carrier,
and a `Morph`-bridged overload (same name) composes *across* carriers — overload resolution picks
the right one. So crossing a builder with a **`Fold`** (not just a single-focus getter) bridges
`Direct → Forget` via `Morph` and reads *many* foci from what was built — read-many falls out of
`cross`:

```scala mdoc:silent
import dev.constructive.eo.optics.{Fold, Review}
import dev.constructive.eo.data.Forget.given
import cats.instances.list.given

// build a List[Int] from a seed, then fold every element
val buildList = Review[List[Int], Int](n => (1 to n).toList)
val sumBuilt = buildList.cross(Fold[List, Int])
```

```scala mdoc
sumBuilt.foldMap[Int](identity)(4) // 1+2+3+4
```

## Typed pattern-functor schemes — `cataF` / `anaF` / `hyloF`

The schemes above thread children through `PSVec[AnyRef]`: fast, but the algebra indexes them
positionally (`kids(0)`, `kids(1)`), so an arity slip is a runtime error. The **typed** path trades
a little boilerplate for compile-time safety: you supply a *pattern functor* `F[_]` — your recursive
type with its recursive positions replaced by a type parameter — and the algebra pattern-matches
`F`'s **named constructors** instead.

You write three things: the functor `F`, its `cats.Traverse`, and a `Basis` (`Project[F, S]` =
`project: S => F[S]`, plus `Embed[F, S]` = `embed: F[S] => S`). Everything else is derived from those.

```scala mdoc:silent
import cats.{Applicative, Eval, Traverse}
import dev.constructive.eo.schemes.{Basis, CataF} // `Schemes`, `Getter`, `get` already imported above

// A binary tree…
enum Bin:
  case Leaf(n: Int)
  case Branch(l: Bin, r: Bin)

// …and its pattern functor: recursion (`Bin`) becomes the parameter `A`.
enum BinF[+A]:
  case LeafF(n: Int)
  case BranchF(l: A, r: A)

given Traverse[BinF] with
  def traverse[G[_]: Applicative, A, B](fa: BinF[A])(f: A => G[B]): G[BinF[B]] =
    fa match
      case BinF.LeafF(n)      => Applicative[G].pure(BinF.LeafF(n))
      case BinF.BranchF(l, r) => Applicative[G].map2(f(l), f(r))(BinF.BranchF(_, _))
  def foldLeft[A, B](fa: BinF[A], b: B)(f: (B, A) => B): B = fa match
    case BinF.LeafF(_)      => b
    case BinF.BranchF(l, r) => f(f(b, l), r)
  def foldRight[A, B](fa: BinF[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match
    case BinF.LeafF(_)      => lb
    case BinF.BranchF(l, r) => f(l, Eval.defer(f(r, lb)))

given Basis[BinF, Bin] = Basis(
  { case Bin.Leaf(n) => BinF.LeafF(n); case Bin.Branch(l, r) => BinF.BranchF(l, r) },
  { case BinF.LeafF(n) => Bin.Leaf(n); case BinF.BranchF(l, r) => Bin.Branch(l, r) },
)

val binTree: Bin = Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))
```

`cataF` folds an `S` to an `A`. The algebra sees the node plus its already-folded children **as a
typed `BinF[A]`** — `l` and `r` are `A`, by name, no positional indexing:

```scala mdoc:silent
val sumLeavesF: CataF[BinF, Bin, Int] =
  Schemes.cataF[BinF, Bin, Int] { (_, folded) =>
    folded match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r
  }
```

```scala mdoc
sumLeavesF.get(binTree)
```

`anaF` builds an `S` from a seed via a single fused coalgebra `Seed => F[Seed]`; `Embed` glues each
layer. `hyloF` is the **fused** refold (`Seed => A`, no intermediate `Bin`) and needs only
`Traverse[F]`:

```scala mdoc:silent
// build a right spine of (n+1) unit leaves
val buildBin = Schemes.anaF[BinF, Int, Bin] { n =>
  if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
}

// fused: count the leaves directly, building no Bin
val countLeavesF: Getter[Int, Int] =
  Schemes.hyloF[BinF, Int, Int](
    coalg = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1),
    alg = (_, folded) =>
      folded match
        case BinF.LeafF(_)      => 1
        case BinF.BranchF(l, r) => l + r,
  )
```

```scala mdoc
sumLeavesF.get(buildBin.reverseGet(3)) // 4 unit leaves
countLeavesF.get(3)                    // same count, fused — no Bin materialised
countLeavesF.get(1000000)              // stack-safe: the heap machine, O(depth) heap
```

Like their `PSVec` counterparts, `cataF`/`hyloF` are `Getter`s and `anaF` is a `Review`, so
they compose with the rest of the optic algebra via `andThen` and `cross` (the materializing
`anaF(…).cross(cataF(…))` equals the fused `hyloF` for a pure algebra — the hylo law). They run on
the **same `< 512`-on-stack / heap-`ArrayDeque` machine** as the `PSVec` schemes (no `cats.Eval`
trampoline) — your `Traverse[F]` is used only per *layer* (any lawful instance works), so they are
stack-safe to depths a hand-written recursion would overflow and allocate close to droste (see the
[benchmarks](benchmarks.md)). **Choosing a path:** reach for `cata`/`ana`/`hylo` (default) when you
want zero
boilerplate; reach for `cataF`/`anaF`/`hyloF` when you want the algebra to be type-checked against
named constructors. Deriving `Project`/`Embed` from the `S`↔`F` correspondence is future work; today
they are hand-written (as above).

### Composing with lenses

Because the schemes are `Getter`s, they slot into a lens pipeline. Compose a **lens chain** to
focus a recursive field buried in a record, then fold it with the scheme. Read-only optics compose
`Getter`-to-`Getter`, so wrap the lens's read in a `Getter` (or just read at the leaf,
`cataF(alg).get(lens.get(record))`) — the same composed lens still *writes* the field back:

```scala mdoc:silent
import dev.constructive.eo.optics.Lens

case class Inner(label: String, tree: Bin)
case class Doc(id: Int, inner: Inner)

val innerL = Lens[Doc, Inner](_.inner, (d, i) => d.copy(inner = i))
val treeL  = Lens[Inner, Bin](_.tree, (i, t) => i.copy(tree = t))
val deepTree = innerL.andThen(treeL) // Lens[Doc, Bin] — lens composition

// wrap the composed lens's read in a Getter, then andThen the scheme → reusable Getter[Doc, Int]
val docLeafSum = Getter[Doc, Bin](deepTree.get).andThen(sumLeavesF.asGetter)

val record = Doc(1, Inner("x", binTree))
```

```scala mdoc
docLeafSum.get(record)                // focus Doc -> its tree, then fold to the leaf sum
deepTree.replace(Bin.Leaf(0))(record) // the SAME composed lens writes the field back
```

The single peel/glue layer is also available on its own as `Schemes.fLayer[F, S]`, an
`Optic[S, S, S, S, Forget[F]]` (`to = project`, `from = embed`) — the typed analogue of `Plated`'s
`plate` for one layer. Given a `Foldable[F]` it reads a node's immediate foci via `.foldMap`. It is
primarily the proof that a typed `F` is an optic carrier; the recursive schemes drive `project`/
`embed` themselves rather than composing `fLayer`.

## The zoo: para / apo / histo / futu

The decorated schemes are **one sum/product symmetry**, and eo ships it as a vocabulary of
*decoration optics* (the `Decor` family, worn on the new `BiAffine` carrier — see below):

| scheme | decoration | shape | `Decor` value |
|---|---|---|---|
| cata / ana | none | — | `Decor.cata` / `Decor.ana` |
| **para** | child slots carry the original subterms | product | `Decor.para` |
| **apo** | child slots may graft a finished subtree | sum | `Decor.apo` |
| **histo** | full decorated history per child (`Attr`) | iterated product | `Decor.histo` |
| **futu** | multiple layers per step (`Coattr`) | iterated sum | `Decor.futu` |
| zygo / dyna / … | user-written `Decor` values | — | (yours — example below) |

`paraF` pairs each child slot with its **original subterm** — taken from the nodes the machine
already walks, with no per-node re-`embed`:

```scala mdoc:silent
// count branches whose left child is a leaf — needs the subterm, not just the result
val leftLeafBranches = Schemes.paraF[BinF, Bin, Int] { (_, layer) =>
  layer match
    case BinF.LeafF(_) => 0
    case BinF.BranchF((ls, l), (_, r)) =>
      l + r + (ls match { case Bin.Leaf(_) => 1; case _ => 0 })
}
```

```scala mdoc
leftLeafBranches.get(binTree)
```

`apoF` lets the coalgebra answer any slot with an **already-finished subtree** — grafted into the
result **by reference**, never recursed, never projected (the law suite pins this with an `eq`
check, so the O(1) claim survives any benchmark noise):

```scala mdoc:silent
val cached: Bin = binTree // an expensive subtree you already have

val patched = Schemes.apoF[BinF, Int, Bin] { n =>
  if n <= 1 then BinF.LeafF(9)
  else BinF.BranchF(Left(cached), Right(n - 1)) // graft left, keep unfolding right
}
```

```scala mdoc
patched.reverseGet(2)
```

`histoF` gives the algebra each child's **entire decorated history** (`Attr[F, A]`: the result
plus that child's own decorated layer — course-of-value recursion; note it inherently retains
O(n) `Attr` cells):

```scala mdoc:silent
import dev.constructive.eo.schemes.{Attr, Coattr}

// add each branch's grandchildren-through-history to its result
val withGrand = Schemes.histoF[BinF, Bin, Int] { (_, layer) =>
  layer match
    case BinF.LeafF(n) => n
    case BinF.BranchF(l, r) =>
      def grand(a: Attr[BinF, Int]): Int = a.tail match
        case BinF.LeafF(_)        => 0
        case BinF.BranchF(gl, gr) => gl.head + gr.head
      l.head + r.head + grand(l) + grand(r)
}
```

`futuF` lets the coalgebra emit **several layers per step** (`Coattr.Roll` layers are unrolled
with no further coalgebra calls):

```scala mdoc:silent
val twoAtATime = Schemes.futuF[BinF, Int, Bin] { n =>
  if n <= 1 then BinF.LeafF(1)
  else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
}
```

### Fusion is composition: `cross`

`anaF`/`cataF` return concrete citizens (`AnaF`/`CataF`) carrying their (co)algebras, so the
build-output→read-input composition — the seam core's `Optic.cross` names — **fuses**: one
single-pass machine, each node built once and folded immediately, no full-tree retention and no
second traversal. (`hyloF` remains the zero-`S` spelling for seed-typed algebras; binding an
`AnaF` to a wider type falls back to the generic, materializing `cross` — extensionally equal.)

```scala mdoc:silent
val zooExpand: Int => BinF[Int] = n =>
  if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
val zooSum: (Bin, BinF[Int]) => Int = (_, fa) =>
  fa match { case BinF.LeafF(n) => n; case BinF.BranchF(l, r) => l + r }

val fusedLeafSum = Schemes.anaF[BinF, Int, Bin](zooExpand).cross(Schemes.cataF(zooSum))
```

```scala mdoc
fusedLeafSum.get(6)
```

### Write your own decoration: zygo as a `Decor` value

The generality that droste exposes as `gcata`/`gana` lives here as the **public `Decor` family**:
a decoration is an optic over the `BiAffine` carrier (fold side: `from` = *gather*; unfold side:
`to` = *scatter*, `from` on `Step` = the seed-injecting unit). A zygomorphism — the algebra
consults a helper fold alongside each child's result — is a user-written gather value, consumed
by the same `cataF(decor)(galg)` driver as the named members:

```scala mdoc:silent
import dev.constructive.eo.schemes.DecorGather
import dev.constructive.eo.data.BiAffine
import dev.constructive.eo.optics.Optic

def zygo[B](helper: BinF[B] => B): DecorGather[BinF, (B, Int), Int] =
  new Optic[Unit, (B, Int), Unit, Int, BiAffine]:
    type X = (Unit, BinF[(B, Int)])
    def to(u: Unit): BiAffine[X, Unit] =
      throw new UnsupportedOperationException("gather-only")
    def from(xb: BiAffine[X, Int]): (B, Int) = xb match
      case s: BiAffine.Step[X, Int] =>
        (helper(summon[Traverse[BinF]].map(s.snd)(_._1)), s.b)
      case _: BiAffine.Done[X, Int] =>
        throw new UnsupportedOperationException("fold-side")

val leafCount: BinF[Int] => Int =
  { case BinF.LeafF(_) => 1; case BinF.BranchF(l, r) => l + r }

// sum, with the helper count available at every branch
val sumWithCount = Schemes.cataF[BinF, Bin, (Int, Int), Int](zygo(leafCount)) { (_, layer) =>
  layer match
    case BinF.LeafF(n)                  => n
    case BinF.BranchF((_, l), (cr, r)) => l + r + 0 * cr // helper in scope per child
}
```

User-written values run the generic decoration route (one decoration dispatch + `Step` per
node); the named values dispatch to native engine routes.

### Effectful steps: `cataFM` / `anaFM` / `hyloFM`

When producing a layer is itself effectful — fetching a node's children from a service, the
`arbo` Calculator shape — the M-generic drivers run the same machine **lifted through
`Monad[M].tailRecM`** (one `M`-action per node event; stack-safety rides on M's `tailRecM`;
supported Ms are single-pass and *linear* — a branching/replaying `M` like `List` is documented
unsupported). Results are `Forget[M]`-carried citizens consumed via `.run`, and
`AnaFM.andThen(CataFM)` fuses (there `andThen` genuinely is the focus seam):

```scala mdoc:silent
import cats.data.State

type Counted[T] = State[Int, T] // counts service calls, arbo's GetSellOptions shape

def fetchLayer(n: Int): Counted[BinF[Int]] =
  State(calls => (calls + 1, zooExpand(n)))

val countedLeafSum =
  Schemes
    .anaFM[Counted, BinF, Int, Bin](fetchLayer)
    .andThen(Schemes.cataFM[Counted, BinF, Bin, Int]((s, fa) => State.pure(zooSum(s, fa))))
```

```scala mdoc
countedLeafSum.run(6).run(0).value // (service calls, leaf sum) — one fused pass
```

### The BiAffine carrier

`Decor`'s carrier is new in core: **`BiAffine`** — `Affine`'s data shape worn on the *build*
seam. `Step(context, focus)` keeps going; `Done(payload)` means "this slot is already finished —
do not call the coalgebra" (apo grafts a finished subtree, futu unrolls a prebuilt layer). Its
laws are the graft-finality and round-trip equations in `cats-eo-laws`. Composition here is
scoped to the shipped seams — the fused `cross`, the M-path `andThen`, and the generic drivers;
BiAffine's full composition-matrix row is follow-up work, as are the elgot/coelgot decorations
(the answer-level short-circuit, which the M machine's internals are already shaped for).
