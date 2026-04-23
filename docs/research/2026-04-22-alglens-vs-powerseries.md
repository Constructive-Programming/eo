---
title: "AlgLens[F] vs PowerSeries — overlap, costs, and where each earns its keep"
type: research
status: active
date: 2026-04-22
---

# AlgLens[F] vs PowerSeries

## TL;DR

- `AlgLens[F]` and `PowerSeries` carry the same shape at the type level:
  `(X, F[A])`-ish where `F` is the classifier structure.
- For the `Lens → traversal → Lens` case (same as `Traversal.each`),
  `AlgLens[F]` is **1.5-2.6× slower** than `PowerSeries` on a matched JMH
  bench (`AlgLensBench`) after successive optimisation passes: singleton
  fast-path, single-pass `mapAccumulate` on push, array-cursor pull, and
  finally the `AlgLensFromList[F]` typeclass dispatch that replaces
  generic `combineK` prepend loops with per-F O(n) rebuilds. The first
  implementation ran 10-24× slower.
- `AlgLens[F]` earns its keep on cases PowerSeries can't model:
  non-uniform classifier cardinality, `F` ≠ traversable container (`Option`
  classifiers, etc.), and `Prism/Optional-with-F[A]-focus` via
  `fromPrismF` / `fromOptionalF`.
- Do NOT recommend `AlgLens.fromLensF` as a general replacement for
  `Traversal.each`. Keep it scoped to genuine algebraic-lens uses.

## Shape overlap

| Carrier | Value shape | `X` semantic | Flexibility in `F` |
|---|---|---|---|
| `PowerSeries` | `PowerSeries(xo: Snd[A], vs: PSVec[B])` | tuple-decomposed outer leftover | fixed — `PSVec` is array-backed internal |
| `AlgLens[F]` | `(X, F[A])` | user-chosen leftover | parametric in `F` with `Monad + Traverse + Alternative` |

Semantically identical: both hold outer structural leftover + an
`F`-wrapped focus vector. The difference is specialisation:

- `PowerSeries` commits to one concrete `F` (`PSVec`) and bakes in
  array-backed storage, tight `while` loops, and fused cross-carrier
  composers (`Composer[Tuple2, PowerSeries]`, etc.).
- `AlgLens[F]` is generic — one implementation handles every `F` that
  satisfies the constraints, routing through typeclass dispatch and
  `State`-monadic chunking.

## Cost sources (AlgLens[F])

The generic implementation pays:

1. **`State[List[D], _]` traversal** for the cardinality-preserving chunking
   on `composeFrom`. State monad allocates per step.
2. **`listToF` via `combineK` fold** to rebuild `F[D]` chunks —
   `foldLeft(empty)((acc, d) => acc <+> pure(d))`. For `List`, each step is
   O(n) concatenation, giving O(n²) reassembly.
3. **`Foldable[F].toList` + `.size` + `.splitAt`** allocate intermediate
   `List`s for each chunk.
4. **Typeclass dispatch** — `Monad[F].map`, `Traverse[F].traverse`,
   `Alternative[F].pure` / `combineK` are all virtual calls through given
   instances; the JIT has a harder time devirtualising vs. PowerSeries's
   concrete `ForgetfulFunctor[PowerSeries]` instance.
5. **Boxing** — `Int` focus elements are boxed into `F[Int]`, while
   PowerSeries's `PSVec` backs by `Array[AnyRef]` and in many cases hands
   back the exact array (ArraySeq specialisation).

PowerSeries's fast paths (shipped in the recent optimisation stack) —
`PSSingletonAlwaysHit`, array-sharing for `ArraySeq`, fused
`GetReplaceLens.andThen(GetReplaceLens)`, the fused `Optic.*` extensions
that bypass the carrier entirely — are all specific to `PowerSeries`'s
concrete shape. None of them apply to `AlgLens[F]` because F is abstract.

## Where AlgLens[F] genuinely wins

PowerSeries cannot model these; `AlgLens[F]` can:

1. **Non-uniform classifier cardinality** — a classifier that returns
   different numbers of candidates per input (e.g., k-nearest-neighbours
   with adaptive k, matches against a variable-length list of rules). The
   chunking algebra in `assocAlgMonad` stores per-xi sizes in `Z` so the
   pull side routes the flattened `F[D]` back to the right inner call.
   PowerSeries's `PSVec`-based algebra assumes the flat vector is a plain
   1-to-1 mapping with the outer `fa`.

2. **`F` that isn't a traversable container** — `F = Option` for
   Maybe-classifiers (`at-most-one candidate`), `F = Ior[E, *]` for
   classifier-with-context, user-defined classifier monads. PowerSeries is
   tied to `Traverse[T]` on the outer container shape; `AlgLens[F]`
   parametrises over `F` in the classifier slot instead.

3. **Prism/Optional with `F[A]`-focus** — via `fromPrismF` /
   `fromOptionalF`. PowerSeries has `Composer[Either, PowerSeries]` and
   `Composer[Affine, PowerSeries]`, but those bridge a Prism/Optional
   whose focus is `A` (singleton) into the traversal carrier, not a
   Prism/Optional whose focus is already `F[A]` (the `Option[List[Int]]`
   case). The `MonoidK`-only constraint makes these factories lightweight
   compared to the full traversal bridge.

## Recommendation

- **Traversal over a `List`/`Vector`/`ArraySeq` field + inner optic** —
  keep using `Traversal.each[T, _]` (PowerSeries). Do not reach for
  `AlgLens.fromLensF`.
- **Genuine algebraic lens** (classifier of varying cardinality, multi-rep
  candidates, ML-style "nearest neighbour" lens) — `AlgLens[F]` is the
  tool; the overhead buys real expressiveness.
- **Prism/Optional over an `F[A]` field** (e.g., `Prism` on
  `Option[List[X]]` hit branch) — `AlgLens.fromPrismF` /
  `fromOptionalF` — no direct PowerSeries analogue.

## Measured numbers (JMH, `AlgLensBench`)

Chain: `Lens[Person, List[Phone]] → <traversal> → Lens[Phone, Boolean]`.
`.modify(!_)` flipping every `isMobile`.

Post-`AlgLensFromList` typeclass dispatch (per-F O(n) rebuild, no
`combineK` round-trip):

| size | AlgLens[List] ns/op | PowerSeries ns/op | naive `List.map` | AlgLens / PS |
|------|--------------------:|------------------:|-----------------:|-------------:|
| 4    | 128                 | 84                | 12               | 1.52× |
| 32   | 960                 | 410               | 114              | 2.34× |
| 256  | 6,972               | 2,648             | 974              | 2.63× |
| 1024 | 26,723              | 11,052            | 4,754            | 2.42× |

Post-singleton-fast-path (pre-`AlgLensFromList`), for reference:

| size | AlgLens[List] ns/op | AlgLens / PS |
|------|--------------------:|-------------:|
| 4    | 163                 | 1.88× |
| 32   | 1,325               | 3.25× |
| 256  | 10,481              | 3.91× |
| 1024 | 38,164              | 3.36× |

Pre-review (first implementation — State-based traversal, List
`splitAt` + `listToF` with left-fold `combineK`):

| size | AlgLens[List] ns/op | AlgLens / PS |
|------|--------------------:|-------------:|
| 4    | 870                 | 10.1× |
| 32   | 4,860               | 11.8× |
| 256  | 64,990              | 23.9× |
| 1024 | 187,130             | 16.8× |

Fixture: `Person(name: String, phones: List[Phone])`,
`Phone(isMobile: Boolean, number: String)`. 3 iterations, 2 warmups, 1
fork, 1 thread. Quick-and-dirty numbers, not trustworthy beyond ratios.

## Consequences for the roadmap

- The *Iris classifier* example (deferred to
  `docs/plans/2026-04-22-002-feat-iris-classifier-example.md`) should
  frame `AlgLens[F]` in terms of (1) and (2) above — i.e. choose a
  fixture where the classifier genuinely has varying cardinality (KNN with
  adaptive k, or one-vs-rest over a variable set of reference points) so
  `AlgLens[F]` does something PowerSeries can't. A vanilla "traversal over
  a list of samples" framing would undersell the carrier.
- Consider adding `AlgLens[F]` to the carriers table in
  `site/docs/concepts.md` alongside PowerSeries, with a clear "use A when
  X, use B when Y" side-by-side.
- The `assocAlgMonad` chunking algorithm is a candidate for targeted
  optimisation if `AlgLens[F]` becomes a hot path: the `State` /
  `listToF` / `splitAt` triangle could be replaced with an index-based
  in-place iteration specialised per `F`. This is future work, not
  blocking.

## Sources

- `benchmarks/src/main/scala/eo/bench/AlgLensBench.scala` — the JMH
  bench.
- `benchmarks/src/main/scala/eo/bench/PowerSeriesBench.scala` — the
  PowerSeries-only bench (for context on PowerSeries's performance
  envelope).
- `core/src/main/scala/eo/data/PowerSeries.scala` — carrier definition.
- `core/src/main/scala/eo/data/AlgLens.scala` — carrier definition.
