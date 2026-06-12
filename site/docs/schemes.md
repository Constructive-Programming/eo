# Recursion schemes

`cats-eo-schemes` expresses the recursion schemes **as optics** over a user-supplied
**pattern functor**, so they compose with the rest of the optic algebra rather than
living in a separate world — and the algebras pattern-match your functor's **named
constructors** (compile-time arity safety, no positional indexing):

| Scheme | Optic | Direction |
|--------|-------|-----------|
| `cata` | `Cata` (Getter-shaped) | fold an existing `S` to an `A` |
| `ana`  | `Ana` (Review-shaped) | build an `S` from a seed |
| `hylo` | `Getter[Seed, A]` (**fused** — no intermediate `S`) | unfold-and-fold in one pass |
| `para` / `apo` / `histo` / `futu` | the zoo (below) | decorated folds / unfolds |
| `cataM` / `anaM` / `hyloM` | `Forget[M]`-carried | effectful steps in a `Monad[M]` |

Everything runs on one stack-safe, post-order machine family (heap-stacked past depth
512, not JVM-call-stacked) — safe to depths a hand-written recursion would overflow,
tested at 10⁶.

```scala mdoc:silent
import dev.constructive.eo.schemes.Schemes
import dev.constructive.eo.optics.Getter
```

## The pattern-functor setup — `cata` / `ana` / `hylo`

You supply a *pattern functor* `F[_]` — your recursive type with its recursive positions
replaced by a type parameter — and the algebra pattern-matches `F`'s **named
constructors**.

You write three things: the functor `F`, its `cats.Traverse`, and a `Basis` (`Project[F, S]` =
`project: S => F[S]`, plus `Embed[F, S]` = `embed: F[S] => S`). Everything else is derived from those.

```scala mdoc:silent
import cats.{Applicative, Eval, Traverse}
import dev.constructive.eo.schemes.Basis
import dev.constructive.eo.schemes.zoo.Cata

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

`cata` folds an `S` to an `A`. The algebra sees the node plus its already-folded children **as a
typed `BinF[A]`** — `l` and `r` are `A`, by name, no positional indexing:

```scala mdoc:silent
val sumLeavesF: Cata[BinF, Bin, Int] =
  Schemes.cata[BinF, Bin, Int] { (_, folded) =>
    folded match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r
  }
```

```scala mdoc
sumLeavesF.get(binTree)
```

`ana` builds an `S` from a seed via a single fused coalgebra `Seed => F[Seed]`; `Embed` glues each
layer. `hylo` is the **fused** refold (`Seed => A`, no intermediate `Bin`) and needs only
`Traverse[F]`:

```scala mdoc:silent
// build a right spine of (n+1) unit leaves
val buildBin = Schemes.ana[BinF, Int, Bin] { n =>
  if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
}

// fused: count the leaves directly, building no Bin
val countLeavesF: Getter[Int, Int] =
  Schemes.hylo[BinF, Int, Int](
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

`cata`/`hylo` are Getter-shaped and `ana` is Review-shaped, so
they compose with the rest of the optic algebra via `andThen` and `cross` (the materializing
`ana(…).cross(cata(…))` equals the fused `hylo` for a pure algebra — the hylo law). They run on
a **`< 512`-on-stack / heap-`ArrayDeque` machine** (no `cats.Eval`
trampoline) — your `Traverse[F]` is used only per *layer* (any lawful instance works), so they are
stack-safe to depths a hand-written recursion would overflow and allocate close to droste (see the
[benchmarks](benchmarks.md)). **Choosing a path:** reach for `cata`/`ana`/`hylo` (default) when you
want zero
boilerplate; reach for `cata`/`ana`/`hylo` when you want the algebra to be type-checked against
named constructors. Deriving `Project`/`Embed` from the `S`↔`F` correspondence is future work; today
they are hand-written (as above).

### Composing with lenses

Because the schemes are `Getter`s, they slot into a lens pipeline. Compose a **lens chain** to
focus a recursive field buried in a record, then fold it with the scheme. Read-only optics compose
`Getter`-to-`Getter`, so wrap the lens's read in a `Getter` (or just read at the leaf,
`cata(alg).get(lens.get(record))`) — the same composed lens still *writes* the field back:

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
| cata / ana | none | — | `Gather.cata` / `Scatter.ana` |
| **para** | child slots carry the original subterms | product | (native only) |
| **apo** | child slots may graft a finished subtree | sum | (native only) |
| **histo** | full decorated history per child (`Attr`) | iterated product | `Gather.histo` |
| **futu** | multiple layers per step (`Coattr`) | iterated sum | `Scatter.futu` |
| zygo / dyna / … | user-written `Gather`/`Scatter` values | — | (yours — example below) |

`para` pairs each child slot with its **original subterm** — taken from the nodes the machine
already walks, with no per-node re-`embed`:

```scala mdoc:silent
// count branches whose left child is a leaf — needs the subterm, not just the result
val leftLeafBranches = Schemes.para[BinF, Bin, Int] { (_, layer) =>
  layer match
    case BinF.LeafF(_) => 0
    case BinF.BranchF((ls, l), (_, r)) =>
      l + r + (ls match { case Bin.Leaf(_) => 1; case _ => 0 })
}
```

```scala mdoc
leftLeafBranches.get(binTree)
```

`apo` lets the coalgebra answer any slot with an **already-finished subtree** — grafted into the
result **by reference**, never recursed, never projected (the law suite pins this with an `eq`
check, so the O(1) claim survives any benchmark noise):

```scala mdoc:silent
val cached: Bin = binTree // an expensive subtree you already have

val patched = Schemes.apo[BinF, Int, Bin] { n =>
  if n <= 1 then BinF.LeafF(9)
  else BinF.BranchF(Left(cached), Right(n - 1)) // graft left, keep unfolding right
}
```

```scala mdoc
patched.reverseGet(2)
```

`histo` gives the algebra each child's **entire decorated history** (`Attr[F, A]`: the result
plus that child's own decorated layer — course-of-value recursion; note it inherently retains
O(n) `Attr` cells):

```scala mdoc:silent
import dev.constructive.eo.schemes.zoo.{Attr, Coattr}

// add each branch's grandchildren-through-history to its result
val withGrand = Schemes.histo[BinF, Bin, Int] { (_, layer) =>
  layer match
    case BinF.LeafF(n) => n
    case BinF.BranchF(l, r) =>
      def grand(a: Attr[BinF, Int]): Int = a.tail match
        case BinF.LeafF(_)        => 0
        case BinF.BranchF(gl, gr) => gl.head + gr.head
      l.head + r.head + grand(l) + grand(r)
}
```

`futu` lets the coalgebra emit **several layers per step** (`Coattr.Roll` layers are unrolled
with no further coalgebra calls):

```scala mdoc:silent
val twoAtATime = Schemes.futu[BinF, Int, Bin] { n =>
  if n <= 1 then BinF.LeafF(1)
  else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
}
```

### Fusion is composition: `cross`

`ana`/`cata` return concrete citizens (`Ana`/`Cata`) carrying their (co)algebras, so the
build-output→read-input composition — the seam core's `Optic.cross` names — **fuses**: one
single-pass machine, each node built once and folded immediately, no full-tree retention and no
second traversal. (`hylo` remains the zero-`S` spelling for seed-typed algebras; binding an
`Ana` to a wider type falls back to the generic, materializing `cross` — extensionally equal.)

```scala mdoc:silent
val zooExpand: Int => BinF[Int] = n =>
  if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
val zooSum: (Bin, BinF[Int]) => Int = (_, fa) =>
  fa match { case BinF.LeafF(n) => n; case BinF.BranchF(l, r) => l + r }

val fusedLeafSum = Schemes.ana[BinF, Int, Bin](zooExpand).cross(Schemes.cata(zooSum))
```

```scala mdoc
fusedLeafSum.get(6)
```

### Write your own decoration: zygo as a `Decor` value

The generality that droste exposes as `gcata`/`gana` lives here as the **public `Gather`/`Scatter` decoration optics**:
a decoration is an optic over the `BiAffine` carrier (fold side: `from` = *gather*; unfold side:
`to` = *scatter*, `from` on `Step` = the seed-injecting unit). A zygomorphism — the algebra
consults a helper fold alongside each child's result — is a user-written gather value, consumed
by the same `cata(decor)(galg)` driver as the named members:

```scala mdoc:silent
import dev.constructive.eo.schemes.zoo.Gather

def zygo[B](helper: BinF[B] => B): Gather[BinF, (B, Int), Int] =
  new Gather[BinF, (B, Int), Int]:
    def gather(layer: BinF[(B, Int)], a: Int): (B, Int) =
      (helper(summon[Traverse[BinF]].map(layer)(_._1)), a)

val leafCount: BinF[Int] => Int =
  { case BinF.LeafF(_) => 1; case BinF.BranchF(l, r) => l + r }

// sum, with the helper count available at every branch
val sumWithCount = Schemes.cata[BinF, Bin, (Int, Int), Int](zygo(leafCount)) { (_, layer) =>
  layer match
    case BinF.LeafF(n)                  => n
    case BinF.BranchF((_, l), (cr, r)) => l + r + 0 * cr // helper in scope per child
}
```

User-written values run the generic decoration route (one decoration dispatch + `Step` per
node); the named values dispatch to native engine routes.

### Effectful steps: `cataM` / `anaM` / `hyloM`

When producing a layer is itself effectful — fetching a node's children from a service, the
`arbo` Calculator shape — the M-generic drivers run the same machine **lifted through
`Monad[M].tailRecM`** (one `M`-action per node event; stack-safety rides on M's `tailRecM`;
supported Ms are single-pass and *linear* — a branching/replaying `M` like `List` is documented
unsupported). Results are `Forget[M]`-carried citizens consumed via `.run`, and
`AnaM.andThen(CataM)` fuses (there `andThen` genuinely is the focus seam):

```scala mdoc:silent
import cats.data.State

type Counted[T] = State[Int, T] // counts service calls, arbo's GetSellOptions shape

def fetchLayer(n: Int): Counted[BinF[Int]] =
  State(calls => (calls + 1, zooExpand(n)))

val countedLeafSum =
  Schemes
    .anaM[Counted, BinF, Int, Bin](fetchLayer)
    .andThen(Schemes.cataM[Counted, BinF, Bin, Int]((s, fa) => State.pure(zooSum(s, fa))))
```

```scala mdoc
countedLeafSum.run(6).run(0).value // (service calls, leaf sum) — one fused pass
```

### The BiAffine carrier

The decoration optics' carrier is new in core: **`BiAffine`** — `Affine`'s data shape worn on the *build*
seam. `Step(context, focus)` keeps going; `Done(payload)` means "this slot is already finished —
do not call the coalgebra" (apo grafts a finished subtree, futu unrolls a prebuilt layer). Its
laws are the graft-finality and round-trip equations in `cats-eo-laws`. Composition here is
scoped to the shipped seams — the fused `cross`, the M-path `andThen`, and the generic drivers;
BiAffine's full composition-matrix row is follow-up work, as are the elgot/coelgot decorations
(the answer-level short-circuit, which the M machine's internals are already shaped for).

---

> An earlier `PSVec`-based untyped path (`cata`/`ana`/`hylo` driven by `Plated`) was
> removed once the typed path subsumed it: the erased positional indexing it required
> made algebra arity slips a runtime error, which is exactly what the typed path fixes.
