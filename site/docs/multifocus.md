# MultiFocus

`MultiFocus[F][X, A] = (X, F[A])` is cats-eo's multi-focus carrier: a
structural leftover `X` paired with an `F`-shaped bundle of foci. A
single carrier, specialised through the type parameter `F`, backs
every optic that focuses more than one value at once — traversals,
grates, algebraic lenses, and aggregating (Kaleidoscope) reads.

What you get: the optic's surface is exactly the intersection of cats's
typeclass hierarchy on `F` with what the generic carrier body supports.
Pick an `F` and the methods light up automatically — `.modify`
(`Functor`), `.foldMap` (`Foldable`), `.modifyA` (`Traverse`), `.at(i)`
(`Representable`), `.collectWith` / `.collectMap` / `.collectList`
aggregation, and same-carrier `.andThen` — with no new carrier, law
surface, or `AssociativeFunctor` instance to write.

For the mechanical intro see [Optics → MultiFocus](optics.md#multifocus);
for runnable patterns the [Cookbook](cookbook.md) ships three
end-to-end recipes that exercise the prototypical Grate / Kaleidoscope /
PowerSeries-downstream shapes.

## Sub-shapes

The choice of `F` selects a sub-shape, each suited to a different
multi-focus job:

| Sub-shape | `F` | What it's for |
|-----------|-----|---------------|
| **`AlgLens[F]`** | `F: Functor / Foldable / Traverse` | Algebraic ("classifier") lenses — a focus computed as a fold / classification over the structure, broadcast back on write. |
| **Kaleidoscope** | `F: Apply` | Aggregating reads and batch-relative rewrites — `.collectWith` / `.collectMap` / `.collectList`. |
| **Grate** | `Function1[X0, *]` | Uniform rewrite across a fixed shape — homogeneous tuples and Naperian / representable containers (`MultiFocus.tuple` / `representable` / `representableAt`). |
| **PowerSeries** | `PSVec` | Element-wise traversal of a collection with downstream `.andThen` composition — the `Traversal.each` carrier; carries the hand-tuned `mfAssocPSVec` fast paths (`MultiFocusSingleton` for Lens morphs, `MultiFocusPSMaybeHit` for Prism / Optional). |
| **`FixedTraversal[N]`** | `PSVec` | Fixed-arity traversal — the `Traversal.{two,three,four}` factories tabulate their known arity into the PowerSeries carrier, so they compose like `each`. |

All five share one runtime shape — a leftover paired with a focus
container — so they live as a single carrier rather than five. See
[Historical landmarks](#historical-landmarks) for that consolidation
and its measured payoff.

## The general flexibility win

`MultiFocus[F][X, A] = (X, F[A])` is **just a pair**. The carrier
ships no typeclass machinery of its own; it inherits whatever `F`
brings.

That distinguishes cats-eo's encoding from monolithic-carrier
alternatives — Monocle's per-family classes (`Lens`, `Prism`,
`Traversal`, `IndexedTraversal`, …) bake the typeclass requirements
into the carrier definition itself. Adding a new optic family means
introducing a new carrier with a new typeclass set. cats-eo's
existential encoding lets the user add a new `F` and the existing
`MultiFocus` surface lights up automatically: `.modify` if `F` has
`Functor`, `.foldMap` if `F` has `Foldable`, `.modifyA` if `F` has
`Traverse`, `.at(i)` if `F` has `Representable`, same-carrier
`.andThen` if `F` has `Traverse + MultiFocusFromList`. No new carrier
file, no new law surface, no new `AssociativeFunctor` instance — the
generic body in `MultiFocus.scala` covers it.

The sub-shapes are the demonstration: each is just a different `F`
plugged into the same shape. The `PSVec` case adds a hand-tuned
same-carrier specialisation (`mfAssocPSVec`) for perf, but its
*capability* set is the generic one, lit up by `cats.Functor[PSVec]`
etc. shipped in the companion.

## The capability set

Every method below is gated on a typeclass that `F` either has or
doesn't have. Bring an `F` to the table and the optic's surface is
exactly the intersection of cats's hierarchy on `F` with what the
generic body supports.

```scala mdoc:silent
import cats.data.ZipList
import cats.instances.list.given
import cats.instances.option.given
import cats.instances.function.given
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.data.MultiFocus.{at, collectList, collectMap, collectWith}
```

### `.modify` — `Functor[F]`

```scala mdoc:silent
val listMF = MultiFocus.apply[List, Int]
```

```scala mdoc
listMF.modify(_ + 1)(List(1, 2, 3))
```

`mfFunctor[F: Functor]` provides `ForgetfulFunctor[MultiFocus[F]]`,
which `Optic.modify` consumes. `Functor[List]` arrives via
`cats.instances.list.given`; the same body lights up for
`Vector`, `Option`, `ZipList`, `PSVec`, `Function1[X, *]`, and any
user-defined `F: Functor`.

### `.foldMap` — `Foldable[F]`

```scala mdoc
listMF.foldMap(identity[Int])(List(1, 2, 3, 4))
```

`mfFold[F: Foldable]` provides `ForgetfulFold[MultiFocus[F]]`. The
carrier-wide `Optic.foldMap` extension picks it up — no
MultiFocus-specific extension method ships, the read-only escape
flows through the carrier-generic body.

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

`mfTraverse[F: Traverse]` provides
`ForgetfulTraverse[MultiFocus[F], Applicative]`. Failures short-circuit
on whatever `Applicative[G]` the user supplies.

### `.collectMap` — Functor-broadcast aggregation

```scala mdoc:silent
val zipMF = MultiFocus.apply[ZipList, Double]
```

```scala mdoc
// Column-wise mean: aggregator sees the whole ZipList, returns
// the mean, the broadcast fills back through Functor[ZipList].map.
zipMF.collectMap[Double](zl => zl.value.sum / zl.value.size.toDouble)(
  ZipList(List(1.0, 2.0, 3.0, 4.0))
)
```

`.collectMap[B](agg: F[A] => B)` requires only `Functor[F]`. The
aggregator collapses the entire `F[A]` focus to a single `B`; the
broadcast `F.map(_ => b)` puts the aggregate back into every position,
preserving the `F`-shape exactly.

### `.collectWith` — the algebraic-lens universal

`collectMap`'s aggregate never sees the individual focus.
`.collectWith(agg: F[A] => A => B)` is the general map-shaped
collect: the curried aggregate sees the whole batch ONCE, and the
`A => B` it returns runs per position — so batch-relative rewrites
(distance-from-mean, share-of-total) are one expression. It
subsumes both map-shaped siblings — `collectMap(agg) =
collectWith(fa => _ => agg(fa))` and `modify(f) = collectWith(_ => f)`,
pinned as discipline laws MF4 / MF5 — and requires only
`Functor[F]`, like `collectMap`.

```scala mdoc
// Batch-relative rewrite: subtract the column mean from every slot.
zipMF.collectWith { zl =>
  val mean = zl.value.sum / zl.value.size.toDouble
  v => v - mean
}(ZipList(List(1.0, 2.0, 3.0, 4.0)))

// Type-changing via the pApply factory: each reading becomes a
// (value, distance) pair — the report-row shape.
MultiFocus.pApply[List, Double, (Double, Double)].collectWith { xs =>
  val mean = xs.sum / xs.size
  v => (v, v - mean)
}(List(1.0, 2.0, 3.0, 4.0))
```

The second call runs through `MultiFocus.pApply[F, A, B]` — the
polymorphic counterpart to the generic `MultiFocus.apply[F, A]`
factory (`apply` is now `pApply[F, A, A]`), sound because the
factory's rebuild is identity on the written-back `F[B]`.

### `.collectList` — List-only cartesian collapse

```scala mdoc
listMF.collectList(_.sum)(List(1, 2, 3, 4))
```

`MultiFocus[List]`-only, produces `List(agg(fa))` — a one-element
output regardless of input length. Reproduces the v1
`Reflector[List]`'s cartesian-singleton choice at the call site
without a typeclass.

### `.at(i)` — `Representable[F]`

```scala mdoc:silent
val grateF = MultiFocus.representable[[a] =>> Boolean => a, Int]
```

```scala mdoc
val payment: Boolean => Int = b => if b then 100 else 0
grateF.at(true)(payment)
grateF.at(false)(payment)
```

`.at(i: F.Representation)` reads the focus at a representative
index — typed against the cats `Representable[F]` instance. For
`Function1[X, *]` this is the natural `apply(i)` lookup; for
custom Naperian containers the user's `Representable` witness
defines the index space. Surface gated on `Representable[F]`,
which most `F`s with `Functor + Foldable + Distributive` already
admit.

### Why two `collect` variants

The v1 `Reflector[F]` typeclass collapsed differently per `F`:

| Instance | `reflect(fa)(f)` returns | Functor.map fits? | Applicative.pure fits? |
|----------|--------------------------|-------------------|------------------------|
| `forList` | `List(f(fa))` (singleton / cartesian) | NO (would broadcast) | YES |
| `forZipList` | `ZipList(List.fill(size)(f(fa)))` (length-preserving) | YES | NO (no top-level pure) |
| `forConst[M]` | `fa.retag[B]` (phantom retag) | YES | YES |
| `forId` | `f(fa)` | YES | YES |

No single derivation from `Apply[F]` covers all four behaviours
uniformly — picking one would have silently changed the v1 List
semantics. The chosen split (Functor-broadcast as the carrier-wide
default, List-cartesian as the call-site extension) is honest about
the choice without cluttering the discipline surface.

`.collectWith` later generalised the *map-shaped* side: it is the
universal that `collectMap` and `modify` specialise (laws MF4 /
MF5), so the surviving split is map-shaped (`collectWith` and its
special cases, `Functor`-derivable) versus shape-collapsing
(`collectList`, the one behaviour no map-shaped combinator can
express).

## Composability profile

`MultiFocus[F]` has shipped inbound bridges from every classical
read-write family (conditional on `F`'s typeclass set) and two
outbound bridges: `→ ModifyF` (write) and a restricted `→ Forget[F]`
read-only escape (`multifocus2forget`, available only when
`T = Unit`). The remaining outbound directions are **structurally
rejected** rather than absent — see
[Composition limits](#composition-limits) below.

### Inbound bridges

| Bridge | Composer | `F` constraints | Notes |
|--------|----------|-----------------|-------|
| `Iso → MF[F]` | `forgetful2multifocus` | `Applicative + Foldable` | Broadcasts the Iso's `S => A` to a singleton `F[A]`. |
| `Iso → MF[Function1[X0, *]]` | `forgetful2multifocusFunction1` | (none — Function1 carrier) | Direct broadcast; lights up `Iso → Traversal.{two,three,four}` and `Iso → MultiFocus.representable / tuple`. |
| `Lens → MF[F]` | `tuple2multifocus` | `Applicative + Foldable` | Mixes in `MultiFocusSingleton` so the same-carrier `mfAssoc` fast-path fires. Alongside `tuple2multifocusPSVec` for the `F = PSVec` specialisation. |
| `Prism → MF[F]` | `either2multifocus` | `Alternative + Foldable` | Miss branch produces `MonoidK[F].empty`. PSVec specialisation: `either2multifocusPSVec`. |
| `Optional → MF[F]` | `affine2multifocus` | `Alternative + Foldable` | Same shape as Prism. PSVec specialisation: `affine2multifocusPSVec`. |
| `Forget[F] → MF[F]` | `forget2multifocus` | (none) | Lifts a Fold into a MultiFocus on the same `F`. |

Each inbound bridge produces a `MultiFocus[F]`-carrier optic that
inherits the full capability set above without per-bridge surface
work. The PSVec-specialised bridges (`tuple2multifocusPSVec`, `either2multifocusPSVec`,
`affine2multifocusPSVec`) sidestep the generic `Applicative[F]` /
`Alternative[F]` constraint because PSVec admits neither — instead
they directly call `PSVec.singleton` / `PSVec.empty` and mix in
`MultiFocusPSMaybeHit` for the Prism / Optional fast-paths inside
`mfAssocPSVec`'s body.

### Same-carrier `.andThen`

Three `AssociativeFunctor[MultiFocus[F], _, _]` instances ship,
specialised by `F`:

- **`mfAssoc`** — the generic body for `F: Traverse +
  MultiFocusFromList`. Covers `List`, `Vector`, `Option`,
  `cats.data.Chain`. Singleton fast-path via `MultiFocusSingleton`
  (so morphed Lenses skip the per-element `F.pure` round-trip).
- **`mfAssocFunction1`** — the absorbed-Grate sub-shape's body for
  `F = Function1[X0, *]`. `Z = (Xo, Xi)`; the rebuild is a
  closure-on-closure, no per-element accumulator. Lights up for
  `MultiFocus.representable`, `MultiFocus.tuple`, and
  `Traversal.{two,three,four}`.
- **`mfAssocPSVec`** — the absorbed-PowerSeries body for `F = PSVec`.
  Parallel-array `AssocSndZ` leftover (saves the per-element Tuple2
  the generic body would build). AlwaysHit fast-path via
  `MultiFocusSingleton`, MaybeHit fast-path via
  `MultiFocusPSMaybeHit` (Prism / Optional inners skip the
  per-element wrapper allocation).

### Outbound — `ModifyF` and a read-only `Forget[F]` escape

```scala mdoc:silent
import dev.constructive.eo.compose.Composer
import dev.constructive.eo.data.ModifyF

val modify = summon[Composer[MultiFocus[List], ModifyF]].to(listMF)
```

```scala mdoc
modify.modify(_ * 2)(List(1, 2, 3))
```

`multifocus2modify[F: Functor]` — closes the U → N gap for both
the prior v1 `kaleidoscope2setter` and the latent never-shipped
`alg2setter`. Like every other `Composer[X, ModifyF]`, this does NOT
enable `multiFocus.andThen(modify)` directly: cross-carrier `.andThen`
goes through `AssociativeFunctor[F]` on a single carrier, and ModifyF
deliberately doesn't ship one (the deferred-modify semantic doesn't
fit `composeTo` / `composeFrom`). The morph value lives at the morph
site, not at the chain site. Same-carrier `modify.andThen(modify)`
*does* work — see the [Modify section](optics.md#modify) for the
`AssociativeFunctor[ModifyF]` instance shipped in `ModifyF.scala`.

The second outbound bridge, `multifocus2forget[F]`, expresses a
`MultiFocus[F]`-carrier optic as a read-only `Forget[F]` — discard the
structural leftover, keep the focused `F[A]`. It is the structural
inverse of the `forget2multifocus` inbound bridge above and ships
*only* for `T = Unit` optics: once `Forget` drops the leftover it can't
reconstruct a `T ≠ Unit` target, and that same `T = Unit` restriction is
what keeps the bidirectional `Forget[F] ⇄ MultiFocus[F]` pair from making
`Morph` resolution ambiguous (see [Composition
limits](#composition-limits) below). It's the explicit-`Composer`
companion to the carrier-wide `.foldMap` / `.headOption` / `.length`
read methods.

### Composition limits

One further outbound direction is **structurally rejected** outright,
and the `→ Forget` direction is rejected only for a *different* effect
(`G ≠ F`) — the same-`F` case ships as the `multifocus2forget` escape
above. The rationale lives at the bottom of `MultiFocus.scala` and in
[`docs/research/2026-04-23-composition-gap-analysis.md` §3.2.6](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-23-composition-gap-analysis.md):

- **`Composer[MultiFocus[F], Direct]`** (MultiFocus widens to
  Iso/Getter). Type-level encodable, but `forgetful2multifocus`
  already ships in the OPPOSITE direction. Adding the reverse would
  create a bidirectional Composer pair, which the
  [`Morph`](https://github.com/Constructive-Programming/eo/blob/main/core/src/main/scala/dev/constructive/eo/Morph.scala)
  resolution explicitly forbids — both `Morph.leftToRight` and
  `Morph.rightToLeft` would match for any `Iso × MultiFocus` pair,
  surfacing as ambiguous-implicit and breaking every
  `iso.andThen(multifocus)` call site. Workaround:
  `multiFocus.to(s)._2` for the read side.
- **`Composer[MultiFocus[F], Forget[G]]` for `G ≠ F`** (MultiFocus
  widens to a *different* effect's Traversal/Fold). Generic in
  `S, T, A, B`. The morphed `to` has the MultiFocus's `F[A]` in hand
  but must yield `G[A]`, and with no relationship between `F` and `G`
  there's no way to convert one to the other. The same-`F` case
  (`Forget[F]`) is exactly the `multifocus2forget` read-only escape
  documented above (restricted to `T = Unit`), so it ships rather than
  being rejected. Users wanting fold/traverse semantics on a
  MultiFocus's slots reach for that escape, construct the
  `Forget[F]`-carrier optic directly, or stay on MultiFocus and use
  `.foldMap` / `.modifyA`.

Lens / Prism / Optional → `MultiFocus[Function1[X0, *]]` is also
structurally absent: `Function1[X0, *]` lacks `Foldable` /
`Alternative`, so the constraint set on `tuple2multifocus[F:
Applicative: Foldable]` (and the Prism / Optional variants) doesn't
fire for the Naperian carrier. The Iso bridge
`forgetful2multifocusFunction1` carries no constraint — it's the
only inbound for the absorbed-Grate sub-shape — so chains of the
form `iso.andThen(MultiFocus.tuple[...])` work, but
`lens.andThen(grate)` does not. (`Traversal.two/three/four` are
unaffected: they ride `MultiFocus[PSVec]` and compose freely.)

## Worked examples

Three end-to-end recipes in the [Cookbook](cookbook.md) cover the
prototypical post-fold shapes:

- **[Recipe A — Prototypical Grate-shape via `MultiFocus.tuple`](cookbook.md)** —
  the "broadcast a uniform `A => B` over a homogeneous tuple"
  idiom. Exercises the absorbed-Grate sub-shape
  `MultiFocus[Function1[Int, *]]`.
- **[Recipe B — Prototypical Kaleidoscope-shape via `.collectWith` / `.collectMap` / `.collectList`](cookbook.md)** —
  the "applicative-aware aggregation" idiom, told as report-row
  preparation: broadcast baseline, cartesian footer, and the
  type-changing `pApply` + `collectWith` batch-relative rewrite.
- **[Recipe C — PowerSeries downstream composition (`Lens → each → Lens`)](cookbook.md)** —
  the post-consolidation crown jewel: the absorbed-PowerSeries
  sub-shape `MultiFocus[PSVec]` lets `.andThen` continue past
  `Traversal.each` into a downstream `Lens`. The deleted
  `Traversal.forEach` shape (`Forget[T]`-based, terminal) couldn't.

## Historical landmarks

This page absorbed the previous "Grate" and "MultiFocus" sections
of `optics.md` in the post-fold doc sweep. The empirical
justification for each absorbed carrier lives in a research spike
on the worktree branch that landed it:

- **AlgLens + Kaleidoscope merge** — the foundational fold;
  `Reflector[F]` deleted, two `.collect` flavours (`.collectMap`
  Functor-broadcast and `.collectList` cartesian) replace the v1
  typeclass.
  See [`docs/research/2026-04-28-multifocus-unification.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-28-multifocus-unification.md).
- **Grate fold** — the lead-position field's empirical dead-code
  deletion; +20% perf on `Grate.modify`; absorbed factories ship
  as `MultiFocus.representable` / `MultiFocus.representableAt` /
  `MultiFocus.tuple`. The spike's research doc was lost in
  consolidation; the surviving evidence is the carrier-doc
  comment in `MultiFocus.scala` and the absorbed factory code.
- **PowerSeries fold** — `Snd[A]` match-type vestige eliminated;
  `mfAssocPSVec` preserves the parallel-array `AssocSndZ`
  representation and both `MultiFocusSingleton` /
  `MultiFocusPSMaybeHit` fast-paths verbatim. JMH within ±5%
  of baseline at every size up to 1024.
  See [`docs/research/2026-04-29-powerseries-fold-spike.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-powerseries-fold-spike.md).
- **FixedTraversal `[N]` fold** — `Traversal.{two,three,four}` rerouted
  through `MultiFocus[Function1[Int, *]]`; the FT-shape gains the
  inbound `Iso ↪`, outbound `↪ ModifyF`, and same-carrier
  `.andThen` from the unified MF carrier — three new compositions
  the user can write today that pre-fold were all U.
  See [`docs/research/2026-04-29-fixedtraversal-fold-spike.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-fixedtraversal-fold-spike.md).

The current, compiler-pinned composition matrix (11 families, 121
cells) lives in
[Optics → Composition matrix](optics.md#composition-matrix); the
historical gap analysis that tracked the fold cell by cell is
[`docs/research/2026-04-23-composition-gap-analysis.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-23-composition-gap-analysis.md).
The pre-spike analysis of `MultiFocus[List]` vs PowerSeries on the
traversal-shape common case (1.5–2.6× slower, hence both carriers
shipped pre-fold) lives in
[`docs/research/2026-04-22-alglens-vs-powerseries.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-22-alglens-vs-powerseries.md);
post-fold the gap is closed by `mfAssocPSVec`'s parallel-array
specialisation.

## Constructors at a glance

```scala
// Generic factory — F[A] source, identity rebuild
def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]]

// Polymorphic counterpart — focus type change, the pEach analogue
def pApply[F[_], A, B]: Optic[F[A], F[B], A, B, MultiFocus[F]]

// Cross-carrier lifts — focus is already F[A], inner gets A
def fromLensF[F, S, T, A, B](
  lens: Optic[S, T, F[A], F[B], Tuple2]
): Optic[S, T, A, B, MultiFocus[F]]

def fromPrismF[F: MonoidK, S, T, A, B](
  prism: Optic[S, T, F[A], F[B], Either]
): Optic[S, T, A, B, MultiFocus[F]]

def fromOptionalF[F: MonoidK, S, T, A, B](
  opt: Optic[S, T, F[A], F[B], Affine]
): Optic[S, T, A, B, MultiFocus[F]]

// Absorbed-Grate factories — F = Function1[F.Representation, *]
def representable[F: Representable, A]
  : Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]

def representableAt[F, A](F: Representable[F])(repr0: F.Representation)
  : Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]

// Absorbed-Grate.tuple — F = Function1[Int, *]
def tuple[T <: Tuple, A](using ValueOf[Tuple.Size[T]], Tuple.Union[T] <:< A)
  : Optic[T, T, A, A, MultiFocus[Function1[Int, *]]]
```

`Traversal.each[T, A]` and `Traversal.{two,three,four}` are shipped
in `dev.constructive.eo.optics.Traversal` and produce
`MultiFocus[PSVec]` / `MultiFocus[Function1[Int, *]]` carriers
respectively.

## Further reading

- [Cookbook → Many focuses at once](cookbook.md) —
  the three end-to-end recipes that ground each absorbed
  sub-shape.
- [Concepts → Composition](concepts.md#composition) — the carrier
  graph and the bridge / lattice diagrams.
- [`MultiFocus.scala`](https://github.com/Constructive-Programming/eo/blob/main/core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala) —
  the canonical source for the carrier definition, capability
  traits, and Composer instances; bottom-of-file comment carries
  the structural-rejection rationale for the directions
  `MultiFocus → Direct` and `MultiFocus → Forget[G]`.

The sub-shapes `MultiFocus[F]` unifies have a deeper literature:

- Chris Penner's articles on grates, Kaleidoscopes, and algebraic
  lenses ([chrispenner.ca](https://chrispenner.ca/)) — the original
  treatments of the families absorbed here.
- [*Profunctor optics, a categorical update*](https://arxiv.org/abs/2001.07488)
  (Clarke, Elkins, Gibbons, Loregian, Milewski, Pillmore, Román) —
  the categorical account that places grates, traversals, and
  algebraic optics in one framework.
- [*Co-Presheaf Optics*](https://bartoszmilewski.com/2021/12/28/co-presheaf-optics/)
  (Bartosz Milewski).
