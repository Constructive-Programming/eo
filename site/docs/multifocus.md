# MultiFocus

`MultiFocus[F][X, A] = (X, F[A])` — a structural leftover paired with
an `F`-shaped focus. One carrier, classifier-shaped through the type
parameter `F`, that absorbed five separate carriers in the lead-up
to 0.1.0. This page is the consolidated reference: the unification
narrative, the typeclass-gated capability matrix, the composability
profile, and the historical landmarks.

For the mechanical intro see [Optics → MultiFocus](optics.md#multifocus);
for runnable patterns the [Cookbook](cookbook.md) ships three
end-to-end recipes that exercise the prototypical Grate / Kaleidoscope /
PowerSeries-downstream shapes.

## The unification narrative

Five carriers — `AlgLens[F]`, `Kaleidoscope`, `Grate`, `PowerSeries`,
`FixedTraversal[N]` — each pairing a structural leftover with some
flavour of focus container, all collapse pre-0.1.0 into the single
`MultiFocus[F]` carrier. Each former carrier is now a sub-shape
selected by the choice of `F`:

| Former carrier | Post-fold shape | Notes |
|----------------|-----------------|-------|
| `AlgLens[F]` | `MultiFocus[F]` for `F: Functor / Foldable / Traverse` | Direct rename. `F`-as-parameter encoding survived verbatim. |
| `Kaleidoscope` | `MultiFocus[F]` for `F: Apply` aggregates | The path-dependent `FCarrier` member became a plain type parameter; `Reflector[F]` was deleted in favour of two extension methods (`.collectMap`, `.collectList`) — see [Q1 below](#why-two-collect-variants). |
| `Grate` | `MultiFocus[Function1[X0, *]]` | The `(A, X => A)` pair collapsed into `(Unit, F.Representation => A)`; the lead-position field was empirically dead (see [Q1 of the Grate fold](#historical-landmarks) — 20% perf win on `.modify` from dropping it). |
| `PowerSeries` | `MultiFocus[PSVec]` | The `(Snd[A], PSVec[B])` shape lost its `Snd` match-type vestige and gained the same-carrier `mfAssocPSVec` `AssociativeFunctor` instance. The parallel-array `AssocSndZ` representation survived inside the PSVec specialisation; both `MultiFocusSingleton` (AlwaysHit) and `MultiFocusPSMaybeHit` (Prism / Optional) fast-paths are preserved. |
| `FixedTraversal[N]` | `MultiFocus[Function1[Int, *]]` | `Traversal.{two,three,four}` factories now produce the absorbed-Grate sub-shape over an `Int => A` lookup; same carrier as `MultiFocus.tuple`. |

The empirical justification lives in four research spike documents
referenced under [Historical landmarks](#historical-landmarks)
below. Two headline numbers from those spikes:

- **20% perf gain on Grate's `.modify`** from the lead-position
  field deletion — the field was carried unconditionally by
  every shipped Grate constructor but discarded by every shipped
  `.from`. The post-fold `MultiFocus[Function1[X0, *]]` body
  carries no equivalent.
- **Composition matrix collapse from 145 U cells to 17 U cells**
  in the gap analysis. Five separate "all U" rows (and matching
  columns) became one `MultiFocus[F]` row + column with shipped
  inbound bridges from every classical family. Carrier count:
  pre-spike 14, post-fold 9.

The pre-fold split was accidental complexity. Each carrier had been
introduced for a real semantic distinction at construction time, but
the runtime shape — pair of leftover and focus container — was
identical in every case. Lifting `F` to a plain type parameter and
letting the cats hierarchy supply `Functor` / `Foldable` / `Traverse`
on demand collapsed the surface without losing any capability.

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

The five-carrier collapse is the demonstration: each former carrier
was just a different `F` plugged into the same shape. The `PSVec`
case adds a hand-tuned same-carrier specialisation
(`mfAssocPSVec`) for perf, but the *capability* set is the
generic one, lit up by `cats.Functor[PSVec]` etc. shipped in the
companion.

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
import dev.constructive.eo.data.MultiFocus.{at, collectList, collectMap}
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

## Composability profile

`MultiFocus[F]` has shipped inbound bridges from every classical
read-write family (conditional on `F`'s typeclass set) and a single
outbound bridge to `SetterF`. Every other outbound direction is
**structurally rejected** rather than absent — see
[Composition limits](#composition-limits) below.

### Inbound bridges

| Bridge | Composer | `F` constraints | Notes |
|--------|----------|-----------------|-------|
| `Iso → MF[F]` | `forgetful2multifocus` | `Applicative + Foldable` | Broadcasts the Iso's `S => A` to a singleton `F[A]`. |
| `Iso → MF[Function1[X0, *]]` | `forgetful2multifocusFunction1` | (none — Function1 carrier) | Direct broadcast; lights up `Iso → Traversal.{two,three,four}` and `Iso → MultiFocus.representable / tuple`. |
| `Lens → MF[F]` | `tuple2multifocus` | `Applicative + Foldable` | Mixes in `MultiFocusSingleton` so the same-carrier `mfAssoc` fast-path fires. Alongside `tuple2psvec` for the `F = PSVec` specialisation. |
| `Prism → MF[F]` | `either2multifocus` | `Alternative + Foldable` | Miss branch produces `MonoidK[F].empty`. PSVec specialisation: `either2psvec`. |
| `Optional → MF[F]` | `affine2multifocus` | `Alternative + Foldable` | Same shape as Prism. PSVec specialisation: `affine2psvec`. |
| `Forget[F] → MF[F]` | `forget2multifocus` | (none) | Lifts a Fold into a MultiFocus on the same `F`. |

Each inbound bridge produces a `MultiFocus[F]`-carrier optic that
inherits the full capability set above without per-bridge surface
work. The PSVec-specialised bridges (`tuple2psvec`, `either2psvec`,
`affine2psvec`) sidestep the generic `Applicative[F]` /
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

### Outbound — only to `SetterF`

```scala mdoc:silent
import dev.constructive.eo.data.SetterF

val setter = listMF.morph[SetterF]
```

```scala mdoc
setter.modify(_ * 2)(List(1, 2, 3))
```

`multifocus2setter[F: Functor]` — closes the U → N gap for both
the prior `kaleidoscope2setter` and the latent never-shipped
`alg2setter`. Like every other `Composer[X, SetterF]`, this does NOT
enable `multiFocus.andThen(setter)` directly: `SetterF` lacks
`AssociativeFunctor` by design. The morph value lives at the morph
site, not at the chain site.

### Composition limits

Two further outbound directions are **structurally rejected** rather
than absent. The rationale lives at the bottom of `MultiFocus.scala`
and in
[`docs/research/2026-04-23-composition-gap-analysis.md` §3.2.6](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-23-composition-gap-analysis.md):

- **`Composer[MultiFocus[F], Forgetful]`** (MultiFocus widens to
  Iso/Getter). Type-level encodable, but `forgetful2multifocus`
  already ships in the OPPOSITE direction. Adding the reverse would
  create a bidirectional Composer pair, which the
  [`Morph`](https://github.com/Constructive-Programming/eo/blob/main/core/src/main/scala/dev/constructive/eo/Morph.scala)
  resolution explicitly forbids — both `Morph.leftToRight` and
  `Morph.rightToLeft` would match for any `Iso × MultiFocus` pair,
  surfacing as ambiguous-implicit and breaking every
  `iso.andThen(multifocus)` call site. Workaround:
  `multiFocus.to(s)._2` for the read side.
- **`Composer[MultiFocus[F], Forget[G]]`** (MultiFocus widens to
  Traversal/Fold). Generic in `S, T, A, B`. The target carrier
  forces the morphed `to` to produce `G[A]` from arbitrary `S`. Even
  with `F = G` matching, the Composer has no place to thread the
  `Foldable[G]` instance through the Composer surface. Users wanting
  fold/traverse semantics on a MultiFocus's slots construct the
  `Forget[F]`-carrier optic directly, or stay on MultiFocus and use
  `.foldMap` / `.modifyA`.

Lens / Prism / Optional → `MultiFocus[Function1[X0, *]]` is also
structurally absent: `Function1[X0, *]` lacks `Foldable` /
`Alternative`, so the constraint set on `tuple2multifocus[F:
Applicative: Foldable]` (and the Prism / Optional variants) doesn't
fire for the Naperian carrier. The Iso bridge
`forgetful2multifocusFunction1` carries no constraint — it's the
only inbound for the absorbed-Grate sub-shape — so chains of the
form `iso.andThen(MultiFocus.tuple[...])` and
`iso.andThen(Traversal.two(...))` work, but `lens.andThen(grate)`
does not.

## Worked examples

Three end-to-end recipes in the [Cookbook](cookbook.md) cover the
prototypical post-fold shapes:

- **[Recipe A — Prototypical Grate-shape via `MultiFocus.tuple`](cookbook.md#recipe-a--prototypical-grate-shape-via-multifocustuple)** —
  the "broadcast a uniform `A => B` over a homogeneous tuple"
  idiom. Exercises the absorbed-Grate sub-shape
  `MultiFocus[Function1[Int, *]]`.
- **[Recipe B — Prototypical Kaleidoscope-shape via `.collectMap` / `.collectList`](cookbook.md#recipe-b--prototypical-kaleidoscope-shape-via-collectmap--collectlist)** —
  the "applicative-aware aggregation" idiom. Exercises the
  absorbed-Kaleidoscope reasoning across `MultiFocus[ZipList]`
  (length-preserving broadcast) and `MultiFocus[List]` (cartesian
  collapse).
- **[Recipe C — PowerSeries downstream composition (`Lens → each → Lens`)](cookbook.md#recipe-c--powerseries-downstream-composition-lens--each--lens)** —
  the post-consolidation crown jewel: the absorbed-PowerSeries
  sub-shape `MultiFocus[PSVec]` lets `.andThen` continue past
  `Traversal.each` into a downstream `Lens`. The deleted
  `Traversal.forEach` shape (Forget[T]-based, terminal) couldn't.

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
- **FixedTraversal[N] fold** — `Traversal.{two,three,four}` rerouted
  through `MultiFocus[Function1[Int, *]]`; the FT-shape gains the
  inbound `Iso ↪`, outbound `↪ SetterF`, and same-carrier
  `.andThen` from the unified MF carrier — three new compositions
  the user can write today that pre-fold were all U.
  See [`docs/research/2026-04-29-fixedtraversal-fold-spike.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-29-fixedtraversal-fold-spike.md).

The composition gap analysis tracks the matrix collapse cell by
cell:
[`docs/research/2026-04-23-composition-gap-analysis.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-23-composition-gap-analysis.md).
The pre-spike analysis of MultiFocus[List] vs PowerSeries on the
traversal-shape common case (1.5–2.6× slower, hence both carriers
shipped pre-fold) lives in
[`docs/research/2026-04-22-alglens-vs-powerseries.md`](https://github.com/Constructive-Programming/eo/blob/main/docs/research/2026-04-22-alglens-vs-powerseries.md);
post-fold the gap is closed by `mfAssocPSVec`'s parallel-array
specialisation.

## Constructors at a glance

```scala
// Generic factory — F[A] source, identity rebuild
def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]]

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

- [Optics → MultiFocus](optics.md#multifocus) — the introductory
  treatment in the family taxonomy.
- [Cookbook → Theme E (Algebraic / classifier)](cookbook.md#theme-e--algebraic--classifier) —
  the three end-to-end recipes that ground each absorbed
  sub-shape.
- [Concepts → Composition](concepts.md#composition) — the carrier
  graph and the bridge / lattice diagrams.
- [`MultiFocus.scala`](https://github.com/Constructive-Programming/eo/blob/main/core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala) —
  the canonical source for the carrier definition, capability
  traits, and Composer instances; bottom-of-file comment carries
  the structural-rejection rationale for the directions
  `MultiFocus → Forgetful` and `MultiFocus → Forget[G]`.
