# Recursion schemes

> **Status: exploratory.** This module is an early exploration of *what* recursion schemes as
> optics should look like and *how* they should be shaped — the API is not yet stable. In
> particular the engine threads children through `PSVec` (a `Array[AnyRef]`-backed vector): this is
> very performant but **type-unsafe** — the per-node child results are erased to `AnyRef` and the
> combiner indexes them positionally, so a coalgebra/algebra arity mismatch is a runtime error, not
> a compile error. Ideas for recovering type safety without losing the stack-safe machine (a typed
> pattern-functor layer? indexed vectors?) are very welcome.
>
> The surface is deliberately small: `cata` / `ana` / `hylo` plus the `Coalg` alias, and nothing
> else — do not search this artifact for `para`, `apo`, `histo`, `futu`, or a monadic `cataM`
> family. You rarely miss `para`: the `cata` algebra `(S, PSVec[A]) => A` is already
> paramorphism-flavored (it sees the original node alongside its folded children). The wider zoo
> is *planned*, not shipped; until it lands, express recursive computations through these three
> rather than hand-rolling your own recursion.

`cats-eo-schemes` expresses the recursion schemes **as optics**, so they compose with the rest
of the optic algebra rather than living in a separate world:

| Scheme | Optic | Direction |
|--------|-------|-----------|
| `cata` | `Getter[S, A]` (driven by `Plated[S]`) | fold an existing `S` to an `A` |
| `ana`  | `Review[S, Seed]` | build an `S` from a seed |
| `hylo` | `Getter[Seed, A]` (**fused** — no intermediate `S`) | unfold-and-fold in one pass |

Task routing: fold an existing tree → `cata`; build one from a seed → `ana`; unfold-then-fold
without keeping the tree → `hylo`; rewrite a tree *in place* (`S => S`) → **not this module** —
that is `Plated.transform` / `Plated.rewrite` in core.

> **Coming from droste / Matryoshka?** There is no fixpoint wrapper and no pattern functor
> here — do not look for `Fix` / `Mu` / `Nu`, a `Functor` instance for a base functor, or a
> `Basis`. The recursive type `S` is used directly, and `Plated[S]` is the one instance the
> fold side needs:

| droste / Matryoshka | `cats-eo-schemes` |
|---------------------|-------------------|
| `Fix[F]` / `Mu[F]` / `Nu[F]` | the recursive type `S` itself — no wrapper |
| pattern functor `F[A]` | `PSVec[A]` — the (untyped) children vector |
| `Basis[F, S]` / `Recursive` + `Corecursive` | `Plated[S]` |
| `Algebra[F, A]` — `F[A] => A` | `(S, PSVec[A]) => A` — node plus its folded children |
| `Coalgebra[F, A]` | `Coalg[Seed, S]` — `Seed => (PSVec[Seed], PSVec[S] => S)` |
| `scheme.cata(alg)` | `Schemes.cata(alg)` — **is** a `Getter[S, A]` |
| `scheme.ana(coalg)` | `Schemes.ana(coalg)` — **is** a `Review[S, Seed]` |
| `scheme.hylo(alg, coalg)` | `Schemes.hylo(expand, alg)` — a **fused** `Getter[Seed, A]` |

All three run on one stack-safe engine, so do **not** wrap your algebras in a trampoline or
`cats.Eval` layer of your own — that only adds allocation on top of a machine that is already
safe. The engine recurses directly on the JVM stack while shallower than 512 frames (balanced
trees never leave the fast path), then hands each deeper subtree to a heap `ArrayDeque`
machine; depths a hand-written recursion would overflow are fine, and a *non-terminating*
coalgebra fails by exhausting the heap (`OutOfMemoryError`), not by `StackOverflowError`.

The examples below use the circe `Plated[Json]` from `cats-eo-circe` as a concrete recursive
`S` — but that is only an example convenience. `Plated[S]` is the **entire** requirement of the
fold side, and any recursive type gets one: derive it with
[`generics.plate[S]`](generics.md#plate-s-recursive-self-traversal-plated), hand-write it via
`Plated.fromChildrenVec`, or call `.asPlated` on an already-built self-traversal optic. There
is no `Basis`-style auto-derivation to look for — that one instance is all the machinery.

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

## Checking the hylo law — `cats-eo-schemes-laws`

The hylo law above is not just prose: it ships as a Discipline law in its own published
artifact, so you can pin your coalgebra / algebra pairs against it in your test suite:

```scala
libraryDependencies += "dev.constructive" %% "cats-eo-schemes-laws" % "@VERSION@" % Test
```

`dev.constructive.eo.schemes.laws.HyloLaws[Seed, S, A]` states the fusion contract:
`hylo(expand, fusedAlg).get(seed) == ana(coalg).cross(cata(alg)).get(seed)`, where the seed
expansion is *derived* from the coalgebra (`coalg(_)._1`) — so the only coherence an instance
asserts is that its `fusedAlg` corresponds to its `alg` over the nodes the coalgebra builds;
an incoherent pair fails the law, which is the point. The Discipline wrapper,
`dev.constructive.eo.schemes.laws.discipline.HyloTests`, is wired like the core `cats-eo-laws`
rule-sets: an abstract class whose `laws` member you override with a `HyloLaws` instance
supplying your `coalg`, `alg`, and `fusedAlg`, then `checkAll` its `hylo` rule-set. You bring
the `Arbitrary[Seed]` — the artifact ships no generators — and the seed generator should
straddle the engine's 512-frame on-stack depth limit so both the recursive fast path and the
heap machine are exercised under the equality. The comparison uses `equals`, so pick a result
type `A` with structural equality (any case class, enum, or primitive).

The artifact is separate from `cats-eo-laws` because its laws quantify over `schemes` types.
Hylo fusion is its only rule-set today; more scheme laws are expected to land there as the zoo
grows.
