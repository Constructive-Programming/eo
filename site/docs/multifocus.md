# MultiFocus

`MultiFocus[F][X, A] = (X, F[A])` is the container traversal and
aggregation carrier: a structural leftover `X` paired with an
`F`-shaped focus vector. `List`, `PSVec`, `Option`, and other
containers supply mapping, folding, and traversal operations.

Fixed-index tabulations have a separate home:
[IndexedGlass](optics.md#indexedglass), the full-grid successor to the
legacy Function1-based Grate. They are not a MultiFocus sub-shape.

## Sub-shapes

| Sub-shape | Container | What it's for |
|-----------|-----------|---------------|
| Algebraic lens | `F: Functor` | Batch-relative rewrites and broadcasts, or a single-focus optic lifted over a container. |
| Aggregation (historically Kaleidoscope) | `F: Functor` | `.collectWith` / `.collectMap`; List additionally supports `.collectList`. |
| PowerSeries | `PSVec` | `Traversal.each`, downstream composition, and specialized flattening/reconstruction fast paths. |
| Fixed-arity traversal | `PSVec` | `Traversal.two`, `three`, and `four`; these remain ordinary container traversals. |

The available operations depend on the container's typeclasses and
the reconstruction algebra, not merely the source and target types.

## The capability set

```scala mdoc:silent
import cats.data.ZipList
import cats.instances.list.given
import cats.instances.option.given
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap, collectWith}

val listMF = MultiFocus.apply[List, Int]
```

### `.modify` — `Functor[F]`

```scala mdoc
listMF.modify(_ + 1)(List(1, 2, 3))
```

`mfFunctor` supplies `ForgetfulFunctor`. Mapping keeps the focus
vector's shape; reconstruction receives this source's observed context.

### `.foldMap` — `Foldable[F]`

```scala mdoc
listMF.foldMap(identity[Int])(List(1, 2, 3, 4))
```

`mfFold` supplies `ForgetfulFold`; this is a read-only aggregation.

### `.modifyA` — `Traverse[F]`

```scala mdoc:silent
def safeRecip(d: Double): Option[Double] =
  if d == 0.0 then None else Some(1.0 / d)
val doubleMF = MultiFocus.apply[List, Double]
```

```scala mdoc
doubleMF.modifyA[Option](safeRecip)(List(1.0, 2.0, 4.0))
doubleMF.modifyA[Option](safeRecip)(List(1.0, 0.0, 4.0))
```

`mfTraverse` supplies `ForgetfulTraverse[MultiFocus[F], Applicative]`.
The chosen effect determines failure and sequencing.

### `.collectMap` — Functor-broadcast aggregation

```scala mdoc:silent
val zipMF = MultiFocus.apply[ZipList, Double]
```

```scala mdoc
zipMF.collectMap[Double](zl => zl.value.sum / zl.value.size.toDouble)(
  ZipList(List(1.0, 2.0, 3.0, 4.0))
)
```

`.collectMap[B](agg: F[A] => B)` computes a summary and maps that
value into every focus position, preserving the container shape.

### `.collectWith` — the algebraic-lens universal

`.collectWith(agg: F[A] => A => B)` computes a per-position function
from the whole batch once, then maps it over the focus vector.
It requires `Functor[F]`. `collectMap(agg)` is the constant-function
case; `modify(f)` is the batch-independent case (laws MF4 / MF5).

```scala mdoc
zipMF.collectWith { zl =>
  val mean = zl.value.sum / zl.value.size.toDouble
  v => v - mean
}(ZipList(List(1.0, 2.0, 3.0, 4.0)))

MultiFocus.pApply[List, Double, (Double, Double)].collectWith { xs =>
  val mean = xs.sum / xs.size
  v => (v, v - mean)
}(List(1.0, 2.0, 3.0, 4.0))
```

### `.collectList` — singleton focus vector

```scala mdoc
listMF.collectList(_.sum)(List(1, 2, 3, 4))
```

This List-only operation passes `List(agg(foci))` to `from`, with
the context returned by this source's `to`. **The singleton guarantee
is about the focus vector, not the reconstructed source.**

`MultiFocus.apply[List, A]` and `pApply[List, A, B]` reconstruct by
identity, so they return a singleton List for any input length.
Other optics can retain surrounding structure or a miss branch.
Shape/count-coupled composites can reject the cardinality mismatch:
their observed context may require more or fewer than one written
focus. Neither `S = List[A]` nor `T = List[B]` promises that arbitrary
`collectList` calls work. Use shape-preserving `collectMap` /
`collectWith` when reconstruction requires the original focus count.

### Why two collect variants

Mapping a summary preserves shape; supplying a singleton vector changes
the focus count. These are different operations. The historical
`Reflector[List]` chose a singleton, while `Reflector[ZipList]`
broadcast into existing positions. The explicit split preserves
that distinction without suggesting that every reconstruction supports
cardinality changes.

## Composability profile

### Inbound bridges

| Bridge | Composer | Container constraints |
|--------|----------|-----------------------|
| `Iso → MF[F]` | `forgetful2multifocus` | `Applicative + Foldable` |
| `Lens → MF[F]` | `tuple2multifocus` | `Applicative + Foldable` |
| `Prism → MF[F]` | `either2multifocus` | `Alternative + Foldable` |
| `Optional → MF[F]` | `affine2multifocus` | `Alternative + Foldable` |
| `Forget[F] → MF[F]` | `forget2multifocus` | none |

The `PSVec`-specialized Lens / Prism / Optional bridges construct
singleton or empty vectors directly. Their private
`MultiFocusSingleton` / `MultiFocusPSMaybeHit` markers support the
flattening fast paths.

### Same-carrier `.andThen`

The generic `mfAssoc` requires `Traverse[F] + MultiFocusFromList`.
It records inner contexts and counts, flattens the focus vectors, and
re-slices on reconstruction. `mfAssocPSVec` implements the same
container semantics with specialized builders and parallel-array
context storage. These are not fixed-index diagonal kernels.

### Outbound — ModifyF and read-only Forget

```scala mdoc:silent
import dev.constructive.eo.compose.Composer
import dev.constructive.eo.data.ModifyF
val modify = summon[Composer[MultiFocus[List], ModifyF]].to(listMF)
```

```scala mdoc
modify.modify(_ * 2)(List(1, 2, 3))
```

`multifocus2modify[F: Functor]` provides a write-oriented projection.
`multifocus2forget[F]` drops the context for read-only `Forget[F]`
optics, restricted to `T = Unit`: dropping context cannot reconstruct
an arbitrary target.

### Composition limits

Different containers need an explicit relationship to convert focus
vectors; no generic `F[A] => G[A]` exists. The same-container
`Forget[F]` escape does not supply that relationship.
No generic classical-family bridges to `Glass[I]` are installed, and
there is no `AssociativeFunctor[Glass[I]]`:
[IndexedGlass](optics.md#indexedglass) uses product-index `andThen`.
Its writable-outer `Optic` extension also accepts a write-only `Modify`
inner, without introducing a generic Glass Composer bridge.

## Worked examples

The [Cookbook](cookbook.md) shows container aggregation, batch-relative
rewrites, and an IndexedGlass Boolean-reader example. For chains that
continue through collections, use `Traversal.each` / `pEach`, including
`Lens → each → Lens` and recursive `Plated` traversals.

## Historical landmarks

The original consolidation brought AlgLens, Kaleidoscope, PowerSeries,
fixed traversals, and Function1-based Grate under MultiFocus. The
Function1 branch was subsequently removed: a shared index represented
only a diagonal of nested tabulations. IndexedGlass now represents
the full grid with a product index and retains an existential context.
Historical research describes the earlier encoding, not the current API.

## Constructors at a glance

- `MultiFocus.apply[F, A]`: container identity reconstruction.
- `MultiFocus.pApply[F, A, B]`: type-changing container reconstruction.
- `MultiFocus.fromLensF`, `fromPrismF`, `fromOptionalF`: lift a
  single-focus optic over a container-valued focus.
- `Traversal.each` / `pEach`, `two` / `three` / `four`: PSVec-backed
  container traversals.

Fixed-index constructors instead live on `IndexedGlass`:
`representable(r)`, `iso`, `unit`, and `apply`. There is no top-level
`Grate` constructor companion.
