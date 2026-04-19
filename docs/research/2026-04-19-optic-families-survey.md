# Optic families survey

Research artifact. Catalogue of every named optic family that shows
up in the Scala / Haskell / profunctor-optics ecosystem, annotated
with how it maps onto cats-eo's existential-carrier design and what
it would take to add each one.

Compiled 2026-04-19. Intent: serve as the starting point for a
series of future `docs/plans/*` files — one per family we decide to
adopt.

## The classical eight — what cats-eo already ships

Built-in today. Match Monocle's public surface.

| Family | Shape | R / W | Foci | Carrier in cats-eo |
|---|---|:-:|---|---|
| Iso (a.k.a. Adapter) | `S ↔ A` bijection | R + W | 1 | `Forgetful` |
| Lens | Focus inside a product | R + W | 1 | `Tuple2` |
| Prism | Focus one branch of a sum | R* + W | 0–1 | `Either` |
| Optional / AffineTraversal | Lens + Prism hybrid | R* + W | 0–1 | `Affine` |
| Traversal | Walk a container | R + W | 0–N | `Forget[F]` or `PowerSeries` |
| Setter | Write-only | — + W | 0–N | `SetterF` |
| Getter | Read-only single focus | R + — | 1 | `Forgetful` with `T = Unit` |
| Fold | Read-only multi focus | R + — | 0–N | `Forget[F]` with `T = Unit` |

`*` = "read-or-miss" — returns `Option[A]` / `Either[miss, A]`.

## Trivially expressible without a new carrier

cats-eo's carriers already admit these shapes — it's mostly a
matter of shipping a named constructor and docstring.

| Family | Why it's free | Effort |
|---|---|---|
| **AffineFold** | Read-only 0-or-1 focus = `Optic[…, Affine]` with `T = Unit`. All the Affine ForgetfulFold machinery is already wired | S |
| **Review** | Reverse-only (build `S` from `A`) = the `ReverseAccessor[Forgetful]` direction of a Prism. Already reachable via `Optic.reverse` | S |
| **ReversedLens / ReversedPrism** | Haskell `optics` ships these as first-class types for ergonomic reasons; cats-eo handles reversal as an operation (`Optic.reverse`) rather than a distinct family | skip unless demand |

These belong in a "round out the surface" plan — one commit per
constructor plus laws + docs.

## Indexed variants — the big parallel hierarchy

In Haskell's `optics` / `lens`, every core family has a **`Ix…`**
counterpart where each focus carries an index (list position, map
key, path segment). Indices concatenate under composition.

| Family | Notes |
|---|---|
| IxLens | single-focus + index |
| IxPrism | rarely named in libraries, usually just IxAffineTraversal |
| IxAffineTraversal | 0-or-1 focus + index |
| IxTraversal | 0–N foci + index |
| IxGetter / IxAffineFold / IxFold | read-only variants |
| IxSetter | write-only with index |

**Why it's a real want**: the common ask "modify every element
alongside its position" is awkward to express via raw `modify`
lambdas. Indexed traversals compose cleanly and index-aware
operations like `imap` / `ifoldMap` / `itoList` come for free once
the machinery is in place.

**What it takes in cats-eo**:

- A new carrier shape, roughly `type IxCarrier[I, F[_, _]] = [X, A] =>> F[X, (I, A)]` or a dedicated two-parameter `IxF[I, X, A]`.
- Indexed counterparts of `ForgetfulFunctor` / `ForgetfulTraverse` / `ForgetfulFold` that thread an index through the traversal.
- `FunctorWithIndex[T]` / `FoldableWithIndex[T]` / `TraversableWithIndex[T]` typeclass substrate (cats ships these as `cats.UnorderedFoldable` + `cats.Foldable` with `zipWithIndex`, and alleycats has richer variants).
- A macro or convenience constructor that lifts a `Traverse[T]` + `FunctorWithIndex[T]` into an `IxTraversal`.
- Indexed extension methods (`imodify`, `ifoldMap`, `itoList`, `ireplace`).
- Composition lattice: composing an `IxX` with an unindexed `Y` preserves the index list; composing two indexed optics concatenates.

**Estimated scope**: the largest single-axis extension. Probably
2–3 commits for the carrier + typeclasses, 1 for each indexed
family constructor + laws, 1 for docs.

## Grate — the dual of Lens

Represents `((S → A) → B) → T`. Targets **Naperian** (representable)
containers — fixed-shape structures indexable by a finite type
(`Vector[N]`, `Map[K, _]` with fixed key set, function-shaped
containers).

**What it enables**: lifting operations through zip-like / distributive
shapes. If you have `f: Double => Double` and a `Grate` into a
3-tuple of Doubles, you can apply `f` to every slot in one hop
without enumerating them.

**Typeclass substrate**: `cats.Distributive[F]` (the corepresentable
counterpart to `Traverse`).

**In cats-eo terms**: a new carrier shape roughly
`type GrateF[X, A] = (A, X => A)` or the classic `(X => A) => A`
encoding. `AssociativeFunctor[GrateF, ...]` and
`ForgetfulFunctor[GrateF]` instances. Composition with existing
optics via one or two new `Composer` bridges (Iso → Grate exists;
Lens → Grate doesn't in general).

**Demand**: theoretically elegant, practically niche. Mostly
useful for fixed-arity numeric work or typed records where every
slot has the same type.

## Algebraic Lens — bulk-aware update

Shape `(S → A) × (F[S] × B → T)` where `F` is `Foldable`. Lets the
update function see the **whole collection of S's**, not just the
single S being updated — so you can compute aggregations (mean,
min, classifier output) and fold them into the update.

**Use case**: "update every row in a table with a column that
depends on the whole column's mean" in a single composed optic.
Or: "classify this record against the distribution of all records
in the set".

**In cats-eo terms**: a genuinely new carrier. Current carriers
don't expose `F[S]` to the update side. Carrier might be
`AlgLensF[X, A]` holding `(F[X], A)` or similar. `AssociativeFunctor`
instance and cross-carrier composers to Lens and Traversal.

**Demand**: small but real for data-pipeline domains (ETL,
analytics). Not commonly reached for in everyday product code.

## Achromatic Lens — algebraic Maybe-Lens with `create`

Special case of Algebraic Lens specialised to `Maybe` (i.e.
`Option`). Ships a `create: B => S` operation alongside the usual
`view` + `update`, and extra laws that pin down what `create` must
do at an absent focus.

**Use case**: "update this field if present, otherwise materialise
a new S from the focus value". cats-eo's `Optional` handles the
present case; Achromatic adds the create-from-thin-air case.

**In cats-eo terms**: probably an extension of `Optional` — add a
`create` member to the carrier or to a specialised subclass.
Smaller surface than Algebraic Lens.

## Kaleidoscope — Applicative-parameterised aggregation

Proposed by Chris Penner. An optic parameterised by an
`Applicative[F]` that uses `F`'s structure to define **grouping
logic**:

- `ZipList` → column-wise aggregation across rows of a table
- plain `List` → cartesian products
- `Const[M, _]` with `Monoid[M]` → summation-shaped aggregation

**Classifying profunctor constraint**: `Reflector` (unlike
`Traversable` for Traversal, `Distributive` for Grate).

**Use case**: operations like "compute per-column mean of this
DataFrame" or "zip-align two JSON arrays and combine keyed
entries" expressed as a single optic.

**In cats-eo terms**: a new carrier plus a `Reflector`-equivalent
typeclass. Structurally unlike any existing cats-eo carrier. The
typeclass substrate is non-trivial — cats doesn't ship `Reflector`
directly; there's some similar machinery in `alleycats` and
`cats-mtl`.

**Demand**: niche but growing; tabular data / analytics
frameworks (Scala's `frameless`, `spark-optics`) could benefit.

## Co-Prism / Loop / etc.

Fragmentary references in categorical-optics papers. No
mainstream library exposes them. Not worth tracking until a real
use case surfaces.

## Profunctor-constraint classification

Orthogonal to families. Each family corresponds to a profunctor
shape in the classical presentation:

| Profunctor constraint | Family |
|---|---|
| `Strong` (cartesian) | Lens |
| `Choice` (cocartesian) | Prism |
| `Strong + Choice` | AffineTraversal |
| `Traversing` | Traversal |
| `Mapping` | Setter |
| `Closed` | Grate |
| `Reflector` | Kaleidoscope |
| none (any profunctor) | Iso |

cats-eo's existential encoding turns these constraints into
**capability typeclasses per carrier** (`Accessor`,
`ForgetfulFunctor`, `ForgetfulTraverse`, `AssociativeFunctor`,
…). Same lattice, different angle — but when we add a new family,
the work still boils down to "pick a carrier and ship its
capability instances".

## Prioritisation for cats-eo

Ordered by (demand × ergonomic win) / (implementation effort):

1. **AffineFold + Review** — trivial, rounds out the classical
   surface. One combined plan, one commit.
2. **Indexed variants** — highest real-world demand, biggest
   single block of work. Split across a carrier plan, a
   typeclass-substrate plan, and per-family constructor plans.
3. **Algebraic Lens** — small but real demand in data-pipeline
   code. New carrier + a handful of composers.
4. **Achromatic Lens** — specialisation of Algebraic for the
   Maybe case; natural follow-up.
5. **Grate** — theoretically elegant, niche in practice. Ship
   once #1–#4 settle the library's feel for "add a new family".
6. **Kaleidoscope** — revisit after Grate; similar shape, bigger
   typeclass-substrate lift (cats doesn't ship `Reflector`).
7. **Co-Prism / Loop / etc.** — watch list. Don't build until a
   user asks.

Each line above should eventually produce a
`docs/plans/2026-MM-DD-NNN-feat-<family>-plan.md` when we pick it
up.

## Sources

- [optics (Haskell) — Optics.html](https://hackage.haskell.org/package/optics/docs/Optics.html)
- [optics (Haskell) — Optics.IxLens](https://hackage.haskell.org/package/optics-core-0.4.1.1/docs/Optics-IxLens.html)
- [optics (Haskell) — Optics.Re (reversed)](https://hackage.haskell.org/package/optics-core-0.4.1/docs/Optics-Re.html)
- [Control.Lens.Indexed (Haskell lens)](https://hackage.haskell.org/package/lens-4.15.1/docs/Control-Lens-Indexed.html)
- [Understanding Profunctor Optics (Clarke et al., 2020)](https://arxiv.org/pdf/2001.11816)
- [Profunctor Optics: a Categorical Update (Román 2020)](https://arxiv.org/abs/2001.07488)
- [Profunctor Optics: Modular Data Accessors (Pickering, Gibbons, Wu)](https://www.cs.ox.ac.uk/people/jeremy.gibbons/publications/poptics.pdf)
- [Kaleidoscopes — Chris Penner](https://chrispenner.ca/posts/kaleidoscopes)
- [Algebraic lenses — Chris Penner](https://chrispenner.ca/posts/algebraic)
- [Profunctor Optics — beuke.org](https://beuke.org/profunctor-optics/)
- [Profunctor Optics: The Categorical View — Bartosz Milewski](https://bartoszmilewski.com/2017/07/07/profunctor-optics-the-categorical-view/)
- [Monocle (Scala) library overview — Baeldung](https://www.baeldung.com/scala/monocle-optics)
- [Lens Resources — bgavran/Lens_Resources](https://github.com/bgavran/Lens_Resources)
