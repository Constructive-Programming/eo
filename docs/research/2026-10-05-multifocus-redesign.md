# MultiFocus re-design: consolidate and rebuild the Grate side

Trigger: the #129 review rounds produced a fourth concept (`CoordinateEq`) for the same
underlying fault — the carrier's types do not express the behaviour the kernel implements.
The decision (2026-10-05) was to stop patching and research a from-scratch formulation of
the Grate side. This note consolidates the two research passes (behaviour inventory +
comparative design) into that decision record.

## 1. What one carrier is actually carrying

`MultiFocus[F]` is two contracts wearing one name:

- **Variable-shape containers** (`List`, `PSVec`, `Option`, …): a flattened focus vector +
  per-focus counts; composition records leftovers and re-slices. This half is coherent, and
  #129's observed-context / `collectList` corrections belong to it.
- **Fixed-index functions** (`Function1[X0, *]`, the Grate side): a `X0 => A` tabulation.
  Composition can only advance the *shared* index — a **diagonal read**. Every mechanism born
  on this branch exists to make partial reads support stable writes: `unobserved` (gone),
  `RepresentativeIndex` (#127), the `Focus` sum + `Function1BroadcastOptic` (#129),
  `Optic.SameFocus` (#129), `CoordinateEq` (draft). Four witnesses, one fault.

The precise holes (each pinned by a failing probe, each reproduced against #127's
`52671355`):

| # | Hole | Root cause |
|---|------|------------|
| A | `tuple ∘ tuple` reads only the diagonal of a nested grid | one index type; `(outer, inner)` pairs unrepresentable |
| B | off-diagonal restore needs `C =:= D`, which erasure makes **route-dependent** (non-inline generic helpers, wrappers) | the property is in the data path, not the optic's type |
| C | grouping observable across a type-changing intermediate | partial restoration at different levels |
| D | `==` on an arbitrary `X0` is not a coordinate identity (NaN, ±0.0, finer observations) | equality-based slot addressing is not well-defined on `X0 => A` |

None of A–D is fixable inside `Optic[S, T, A, B, MultiFocus[Function1[X0, *]]]`: the type
`X0 => A` composes only by sharing `X0` or by comparing against it. That is the consolidation
finding: a rewrite, not another witness.

## 2. The shape that dissolves all four

Give nested composition the **product index**, and give the optic a split/rebuild algebra:

```scala
trait IndexedGlass[S, T, A, B, I]:
  type Context
  def split(s: S): (Context, I => A)
  def rebuild(context: Context, values: I => B): T

  def andThen[C, D, J](inner: IndexedGlass[A, B, C, D, J]):
      IndexedGlass[S, T, C, D, (I, J)] {
        type Context = (this.Context, I => inner.Context)
      }
```

```text
split:   focus (i, j) = inner.split(outerRead(i)).focus(j)   -- the FULL grid, not a diagonal
rebuild: outer.rebuild(x, i => inner.rebuild(contexts(i), j => written(i, j)))
```

- **A gone**: every nested coordinate is read and addressed; nothing off-diagonal needs
  restoring.
- **B/C gone**: no equality evidence exists at all, so no route or grouping can change
  behaviour; polymorphic writes replace the whole grid without `C =:= D`.
- **D gone**: nothing compares an index to anything; the written grid is consumed at every
  coordinate.

Honesty items:

- In general this is an **indexed Glass** (context-carrying), not a classical Grate; the
  Grate `((F[A] => A) => B) => F[B]` is recovered when `Context = Unit`. `zipWith` degrades
  when the residual context is not Unit — an advisor-confirmed limit.
- **Semantics change, deliberately**: `tuple ∘ tuple` `replace(9)` goes from diagonal
  `((9,2),(3,9))` to full-grid `((9,9),(9,9))`, and `modify(identity)` is the identity by
  construction. A migration must name this, not discover it.
- Composition cannot ride today's `AssociativeFunctor` (it fixes one carrier); it needs a
  dedicated method first.
- Proof obligations: split-after-rebuild on **arbitrary** tabulations (not only ones a
  `split` produced); associativity up to the coordinate reassociation `(I, J, K)`; no cache
  of derived context may break the round trip.

## 3. Options considered

| Option | Verdict |
|--------|---------|
| **A. Product-index `IndexedGlass` + Unit-context Grate specialization** | **Recommended.** Only candidate that makes behaviour type-expressible with zero new user-facing givens (`Representable` passed explicitly or off the companion as today). |
| B. Graded path types (`Then[Axis[I], Axis[J]]`) as the optic parameter | Only worth it as an optional static façade over A if index ergonomics demand it; grades alone fix nothing and can be claimed falsely. |
| C. Split lawful-diagonal vs explicitly-lossy constructors in the current carrier | Compatibility band-aid; diagonal restore still needs coordinate identity (D survives). Not the architecture. |
| D. A `Tabulate`/bundle value type | Packaging for A, not an alternative; a bare reader comonad needs monoidal indexes we do not have. |

Prior-art notes verified along the way: `Rep(Compose F G) = (Rep F, Rep G)` while
`Rep(Product F G) = Either` — nested means tuple, side-by-side means sum; a reader has a
canonical monad, and the comonad needs `Monoid (Rep f)`. Monocle attribution is **unresolved**:
no shipped `Grate.scala` was found in `optics-dev/Monocle` (path 404s; web search was
unavailable in this environment), so do not cite it for the L1/L2 law names in docs.

## 4. Compatibility budget

Searched every module for `MultiFocus` / grate usage:

- `.broadcast` and `RepresentativeIndex`: **zero** downstream consumers — the #127/#129
  surface was never load-bearing outside core/tests.
- Production grate consumption lives in core, `tests/`, `benchmarks/`.
- `jsoniter`/`avro` use `MultiFocus[PSVec]` — the **variable-shape** side, untouched by this
  rewrite.
- Retire list on landing A: `Function1Carrier`, `Function1BroadcastOptic`, `mfAssocFunction1`,
  `restoredBundle`, `AssocF1Z`, `AssocF1InnerRead`, `CoordinateEq`, `Optic.SameFocus`,
  `RepresentativeIndex`, and the `Focus`-sum `Broadcast` tag (its only purpose was the F1
  carrier's index-free bundles) — then re-verify whether `MultiFocus[F]` still needs the sum
  at all for container Fs.
- Keep list: MF1–MF5 law suite, cross-family battery, `collectList` context corrections, PSVec
  / generic kernels.

## 5. Phased path

1. Prototype A standalone (new file, e.g. `data/Grate.scala` or a spike package), not in
   #129. Port `tuple` / `representable` / `apply`-equivalents to `IndexedGlass` constructors.
2. Prove the split/rebuild and full-grid composition laws against arbitrary tabulations,
   three-level associativity, and the hostile index cases (empty, singleton, permuted,
   NaN, ±0.0).
3. Land full-grid composition as the grate; keep the diagonal behaviour only behind an
   explicitly named legacy combinator if users need it.
4. Delete the F1 runtime-dispatch machinery once nothing references it.
5. #129 disposition: split the variable-shape corrections (`collectList` real context,
   observed leftovers, cross-family suite) into their own PR; the broadcast-sum + witness
   half is superseded by this re-design.

## 6. Open questions for the developer

1. Is nested `andThen` intended to mean **all nested coordinates** (full-grid) or to retain
   diagonal selection? (Everything else follows.)
2. Name: is `Glass`/`IndexedGlass` acceptable in the public vocabulary, with `Grate` reserved
   for the `Context = Unit` specialization?
3. Accept `(I, Unit)` index normalization deferred (result indices carry unit axes initially)?
4. Must hand-written `Optic[_, _, _, _, MultiFocus[Function1[I, *]]]` instances remain
   source-compatible, or is deleting the carrier in 0.x acceptable (MiMa is off anyway)?
5. Law citations: which published Grate law source should the docs align to (Monocle
   attribution currently unverified)?

### Related

- `2026-09-30-grate-broadcast-constancy.md` (the #129 spike; its superseded banner records the
  route/grouping/equality findings that killed the witness design — consolidated in §1 A–D here)
- `2026-09-30-grate-witness-index.md` (`RepresentativeIndex`, #127)
- `2026-04-28-multifocus-unification.md`, `2026-04-29-powerseries-fold-spike.md`,
  `2026-04-22-alglens-vs-powerseries.md` (variable-shape side, which survives)
