# MultiFocus re-design: consolidate and rebuild the Grate side

Trigger: the #129 review rounds produced a fourth concept (`CoordinateEq`) for the same
underlying fault — the carrier's types do not express the behaviour the kernel implements.
The decision (2026-10-05) was to stop patching and research a from-scratch formulation of
the Grate side. This note consolidates the two research passes (behaviour inventory +
comparative design) into that decision record.

## 1. What one carrier is actually carrying

The legacy `MultiFocus[F]` carried two different contracts:

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

The old shared-index kernel lost independent nested coordinates.
These probes do not establish that every evidence- or continuation-based
encoding inside Optic is impossible. The approved redesign instead
retains independent axes explicitly, removing the need for the old
diagonal restoration machinery.

## 2. The full-grid replacement shape

Give nested composition the **product index**, using the existing `Optic.to` / `from` algebra:

```scala
type GlassF[I] = [X, A] =>> (context: X, values: I => A)

trait Indexed[S, T, A, B, I] extends Optic[S, T, A, B, GlassF[I]]:
  outer =>

  def andThen[C, D, J](inner: Indexed[A, B, C, D, J]):
      Indexed[S, T, C, D, (I, J)] {
        type X = (outer.X, I => inner.X)
      }
```

```text
to:   focus (i, j) = inner.to(outerRead(i)).values(j)   -- the FULL grid, not a diagonal
from: outer.from((context = x, values = i =>
        inner.from((context = contexts(i), values = j => written((i, j))))))
```

- The product index represents every nested coordinate; no off-diagonal
  restoration is needed.
- Polymorphic writes consume the whole grid without `C =:= D` or
  index equality. Groupings agree after index and context reassociation
  for lawful components.
- Hostile-index and non-inline-helper tests exercise the original probes.
  They are runtime evidence, not universal proof for arbitrary user constructors.

Honesty items:

- In general this is an **indexed Glass** with residual `X`, not a
  context-free Grate. `Indexed.Grate` is the `X = Unit` alias.
  Context-sensitive zip/collect operations are not installed; selecting
  a residual context when combining sources needs a separate design.
- **Semantics change, deliberately**: `tuple ∘ tuple` `replace(9)` goes from diagonal
  `((9,2),(3,9))` to full-grid `((9,9),(9,9))`. Identity modification
  follows from lawful component round trips. A migration must name this change.
- Composition cannot ride today's `AssociativeFunctor` (it fixes one carrier); it needs a
  dedicated method first.
- Proof obligations: `to` after `from` on **arbitrary** bundles (not only ones
  `to` produced); associativity up to coordinate and context reassociation; no cache
  of derived context may break the round trip.

## 3. Options considered

| Option | Verdict |
|--------|---------|
| **A. Product-index `Indexed` + Unit-context Grate specialization** | **Approved.** Keeps independent axes explicit without new user-facing witnesses; `Representable` is passed explicitly. |
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

## 5. Approved implementation and migration

The approved scope removes the legacy Function1-based Grate branch,
not List/PSVec container traversal or aggregation. There is no retained
diagonal compatibility combinator. Historical witness-index and broadcast
proposals are superseded.

`optics/Indexed.scala` defines the `Indexed` trait and companion;
`data/Glass.scala` defines its carrier. Import the optic from
`dev.constructive.eo.optics.Indexed`; no compatibility alias is provided.
`Indexed` extends `Optic`, with carrier
`GlassF[I] = [X, A] =>> (context: X, values: I => A)` and existential
`X`. All construction and composition use only `to` / `from`.
There is no separate `Context`, `GlassK`, or split/rebuild alias.

Constructors are `representable(r)`, `iso`, `unit`, and `apply`.
Operations are `at`, indexed `modify`, `replace`, and dedicated
product-index `andThen`. A tuple macro and top-level Grate companion
are not provided. Generic classical-family bridges and a same-index
`AssociativeFunctor[GlassF[I]]` are not installed.
The writable-outer `Optic` extension supports a write-only Modify
inner; that positive seam is not a Glass Composer bridge.

`GlassSpec` exercises arbitrary tabulation round trips, three-level
reassociation, type-changing composition through a non-inline helper,
and permuted / empty / Unit / NaN / signed-zero indexes. The equational
composition argument assumes lawful source and target algebras; the
trait does not enforce those constructor obligations.

The container-side `collectList` correction remains: reconstruct with
the observed context and a singleton focus vector. This is not a promise
of a singleton source, and count-coupled composites can reject the
cardinality change.

## 6. Remaining design questions

- Unit-context normalization: generic `andThen` retains
  `(outer.X, I => inner.X)`, including `(Unit, I => Unit)` when both
  inputs are Grates. The `X = Unit` alias is not yet composition-closed.
- Unit axes remain in product indexes; index/context reassociation is
  not literal type equality.
- Lazy composition derives inner contexts per outer index without
  enumeration or memoization. Benchmark the resulting full-grid work
  separately from historical diagonal workloads.
- Law-source attribution remains unresolved; do not attribute this API
  or law names to an unverified Monocle implementation.

### Related

- `2026-09-30-grate-broadcast-constancy.md` (the #129 spike; its superseded banner records the
  route/grouping/equality findings that killed the witness design — consolidated in §1 A–D here)
- `2026-09-30-grate-witness-index.md` (`RepresentativeIndex`, #127)
- `2026-04-28-multifocus-unification.md`, `2026-04-29-powerseries-fold-spike.md`,
  `2026-04-22-alglens-vs-powerseries.md` (variable-shape side, which survives)
