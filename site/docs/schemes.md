# Recursion schemes

`cats-eo-schemes` expresses the recursion schemes **as optics** over a user-supplied
**pattern functor**, so they compose with the rest of the optic algebra rather than
living in a separate world — and the algebras pattern-match your functor's **named
constructors** (compile-time arity safety, no positional indexing):

| Scheme | Optic | Direction |
|--------|-------|-----------|
| `cata` | `Getter[S, A]` | fold an existing `S` to an `A` |
| `ana`  | `Review[S, Seed]` | build an `S` from a seed |
| `hylo` | `Getter[Seed, A]` (**fused** — no intermediate `S`) | unfold-and-fold in one pass |
| `para` / `apo` / `histo` / `futu` | the zoo (below) | decorated folds / unfolds |
| `cataM` / `anaM` / `hyloM` | `.get`/`.reverseGet` yield `M[…]` | effectful steps in a `Monad[M]` |

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
val sumLeavesF = Schemes.cata[BinF, Bin, Int] {
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
val countLeavesF = Schemes.hylo[BinF, Int, Int](
    coalg = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1),
    alg = {
      case BinF.LeafF(_)      => 1
      case BinF.BranchF(l, r) => l + r
    },
  )
```

```scala mdoc
sumLeavesF.get(buildBin.reverseGet(3)) // 4 unit leaves
countLeavesF.get(3)                    // same count, fused — no Bin materialised
countLeavesF.get(1000000)              // stack-safe: the heap machine, O(depth) heap
```

`cata` and `hylo` are **Getter-shaped** (forward reads over the `Direct` carrier) and `ana` is
**Review-shaped** (its build-only dual), so they compose with the rest of the optic algebra: `cata`/`hylo` via `andThen`, and the
build⇄read refold via `ana.cross(cata)` (the materializing `ana(…).cross(cata(…))` equals the fused
`hylo` for a pure algebra — the hylo law). They run on
a **`< 512`-on-stack / heap-`ArrayDeque` machine** (no `cats.Eval`
trampoline) — your `Traverse[F]` is used only per *layer* (any lawful instance works), so they are
stack-safe to depths a hand-written recursion would overflow and allocate close to droste (see the
[benchmarks](benchmarks.md)). The typed path is the only path — the earlier untyped
`Plated`-driven spelling was removed once this one subsumed it (see the note at the bottom of this
page). Deriving `Project`/`Embed` from the `S`↔`F` correspondence is future work; today they are
hand-written (as above).

### Composing with lenses

Because the schemes read through `.get`, they slot into a lens pipeline. Compose a **lens chain**
to focus a recursive field buried in a record, then fold it with the scheme — wrap the composite
read in a `Getter` so it stays a reusable optic (the same composed lens still *writes* the field
back):

```scala mdoc:silent
import dev.constructive.eo.optics.Lens

case class Inner(label: String, tree: Bin)
case class Doc(id: Int, inner: Inner)

val innerL = Lens[Doc, Inner](_.inner, (d, i) => d.copy(inner = i))
val treeL  = Lens[Inner, Bin](_.tree, (i, t) => i.copy(tree = t))
val deepTree = innerL.andThen(treeL) // Lens[Doc, Bin] — lens composition

// read through the composed lens, fold with the scheme, wrap as a Getter → reusable optic
val docLeafSum = Getter[Doc, Int](doc => sumLeavesF.get(deepTree.get(doc)))

val record = Doc(1, Inner("x", binTree))
```

```scala mdoc
docLeafSum.get(record)                // focus Doc -> its tree, then fold to the leaf sum
deepTree.replace(Bin.Leaf(0))(record) // the SAME composed lens writes the field back
```

The single peel/glue layer is also available on its own as `Schemes.fLayer[F, S]`, an
`Optic[S, S, S, S, MultiFocus[F]]` (`to = project`, `from = embed`) — the typed analogue of
`Plated`'s `plate` for one layer. It composes with the rest of core on the shared carrier: read a
node's immediate foci via `.foldMap` (`Foldable[F]`), rewrite them via `.modify`/`.replace`
(`Functor[F]`), or effect over them via `.modifyA`/`.all` (`Traverse[F]`). It is one layer, not the
recursion; the `Plated.fromBasis` derivation is its recursive face, and the recursive schemes drive
`project`/`embed` themselves rather than composing `fLayer`.

## The zoo: para / apo / histo / futu

The decorated schemes are **one sum/product symmetry**, shipped as named optic citizens —
`final class`es in `zoo` carrying their parts, so composition and fusion (`ana.cross(cata)`)
resolve against the concrete types. Their decorations are consumed natively by the engine on
`Affine`'s arms worn on the build seam (see below):

| scheme | decoration | shape |
|---|---|---|
| cata / ana | none (`X = Nothing` / `S`) | the forgetful base |
| **para** | child slots carry the original subterms | product |
| **apo** | child slots may graft a finished subtree | sum |
| **histo** | full decorated history per child (`Attr`) | iterated product |
| **futu** | multiple layers per step (`Coattr`) | iterated sum |
| zygo / mutu / cozygo / comutu / dyna / chrono / elgot / … | auxiliary carriers between the towers | see the scaladocs |

`para` pairs each child slot with its **original subterm** — taken from the nodes the machine
already walks, with no per-node re-`embed`:

```scala mdoc:silent
// count branches whose left child is a leaf — needs the subterm, not just the result
val leftLeafBranches = Schemes.para[BinF, Bin, Int] {
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
val withGrand = Schemes.histo[BinF, Bin, Int] {
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

### Composition and fusion: `cross` vs `hylo`

`ana` is a build-only `Review` and `cata` a read-only `Getter` — duals over `Direct`. The
unfold-then-fold refold is their `cross` at the build-output⇄read-input seam (exactly what
`Optic.cross` documents: "the motivating case is `ana.cross(cata)`"), yielding a forward read.
`ana(…).cross(cata(…))` is the **materializing** hylo: it builds the whole `S`, then folds it.
`Schemes.hylo` is the **fused** spelling — one single-pass machine, each node built once and folded
immediately, no intermediate `S` and no second traversal. The two agree for a pure algebra (the
hylo law).

```scala mdoc:silent
val zooExpand: Int => BinF[Int] = n =>
  if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
val zooSum: BinF[Int] => Int =
  { case BinF.LeafF(n) => n; case BinF.BranchF(l, r) => l + r }

val fusedLeafSum = Schemes.ana[BinF, Int, Bin](zooExpand).cross(Schemes.cata(zooSum))
```

```scala mdoc
fusedLeafSum.get(6)
```

### A decorated fold with a helper: `zygo`

The generality droste exposes as `gcata`/`gana` lives here as **named citizens between the
towers**. A zygomorphism — the main algebra consults an auxiliary algebra alongside each child's
result — is one constructor: the helper algebra, then the main algebra reading `(helper, main)`
per child. (`para` is exactly `zygo` at `B = S` with the helper `embed`; `mutu` generalises to
two mutually-recursive algebras.)

```scala mdoc:silent
val leafCount: BinF[Int] => Int =
  { case BinF.LeafF(_) => 1; case BinF.BranchF(l, r) => l + r }

// leaf sum, where every branch also sees its children's helper results
val sumWithCount = Schemes.zygo[BinF, Bin, Int, Int](leafCount) {
  case BinF.LeafF(n)                  => n
  case BinF.BranchF((cl, l), (cr, r)) => l + r + cl * cr
}
```

```scala mdoc
sumWithCount.get(binTree)
```

The named citizens dispatch to native engine routes — the decoration is consumed inside the
machine, not as a per-node optic dispatch.

### Effectful steps: `cataM` / `anaM` / `hyloM`

When producing a layer is itself effectful — fetching a node's children from a service, the
`arbo` Calculator shape — the M-generic drivers run the same machine **lifted through
`Monad[M].tailRecM`** (one `M`-action per node event; stack-safety rides on M's `tailRecM`;
supported Ms are single-pass and *linear* — a branching/replaying `M` like `List` is documented
unsupported). `cataM` reads via `.get: S => M[A]` and `anaM` builds via
`.reverseGet: Seed => M[S]`; `hyloM` is the **fused** effectful refold — one single-pass machine,
no intermediate `S` built (the materialising pair `cataM(alg).get(anaM(coalg).reverseGet(seed))`
agrees with it — the `M = Id` cross-architecture pin in the spec).

```scala mdoc:silent
import cats.data.State

type Counted[T] = State[Int, T] // counts service calls, arbo's GetSellOptions shape

def fetchLayer(n: Int): Counted[BinF[Int]] =
  State(calls => (calls + 1, zooExpand(n)))

// fused: each node's layer is fetched in M and folded immediately — one pass, no Bin built
val countedLeafSum = Schemes.hyloM[Counted, BinF, Int, Int](
  fetchLayer,
  fa => State.pure(zooSum(fa)),
)
```

```scala mdoc
countedLeafSum.get(6).run(0) // (service calls, leaf sum) — one fused pass
```

### The build-seam carrier is `Affine`

The decoration machinery needs no new carrier: it rides **`Affine`** with its arms read on the
*build* seam — `Hit(context, focus)` keeps going; `Miss(payload)` means "this slot is already
finished — do not call the coalgebra" (apo grafts a finished subtree, futu unrolls a prebuilt
layer). The build-channel injection vocabulary is the `Graft[Affine]` instance (`done = Miss`,
`step = Hit`), and its laws are the graft-finality and round-trip equations in `cats-eo-laws`.
Affine's own composition row covers decoration composition: `Affine.assoc` (same-carrier
`andThen`) plus the cross-carrier bridges from `Tuple2` (Lens) and `Either` (Prism), so
`lens.andThen(apoScatter)`-style compositions resolve; `Schemes.apoScatter` exposes the
`Left(s) → Miss(s)` graft channel as a composable scatter optic. On the scheme side,
`elgot`/`coelgot` (the answer-level short-circuit and seed-reading refolds) are shipped citizens,
and `meta`/`metaChrono` complete the non-fusing fold→unfold quadrant.

---

> An earlier `PSVec`-based untyped path (`cata`/`ana`/`hylo` driven by `Plated`) was
> removed once the typed path subsumed it: the erased positional indexing it required
> made algebra arity slips a runtime error, which is exactly what the typed path fixes.
