# Optic extension method survey (2026-04-29)

Branch: `worktree-agent-a65d9b8cbd48097f3` from `1661006`. Companion to
the typeclass-hierarchy walk in
`docs/research/2026-04-29-codebase-structure-audit.md`.

Walks the cats hierarchy and identifies extension methods cats-eo could
ship on `Optic[…, F]` but doesn't yet, prioritised on user reach and
implementation cost. The first three are shipped in the same branch
this doc lands on; the rest are surfaced for the user to triage.

## Reading the table

`Carrier` — what the carrier needs to admit. `Forgetful + Forget[F] +
MultiFocus[F] + Affine + Either + Tuple2` is "every carrier that ships
today"; rows that name a specific carrier require it.

`Typeclass` — the cats / cats-eo typeclass instance that gates the
extension. The minimal one is named; subtypes (e.g. `Traverse[F]`
implies `Functor[F]` + `Foldable[F]`) are not duplicated.

`Status` — ✅ shipped in this branch, 🚧 candidate, 🔵 deferred (kept
for the user to consider).

## The full table — 20 candidates

| # | Method signature on `Optic[S, T, A, B, F]` | Carrier capability gate | Reasoning | Example | Status |
|---|---|---|---|---|---|
| 1 | `headOption(s: S): Option[A]` | `ForgetfulFold[F]` (or `Foldable[F]` for `Forget[F]`) — works on any read carrier | First focus — the most-asked operation on a multi-focus optic. Currently users reach for `.foldMap(List(_))(s).headOption` (allocates a List). Direct path cuts the allocation. | `lens.andThen(Traversal.each).headOption(person)` returns the first phone. | ✅ shipped |
| 2 | `length(s: S): Int` | `ForgetfulFold[F]` | Count visible foci. Today users write `.foldMap(_ => 1)(s)` — allocates a closure, sums via `Monoid[Int].combine` per element. Direct via `Foldable.size` / `Foldable.foldLeft(_ + 1)`. | `personPrism.field(_.phones).each.length(p)` | ✅ shipped |
| 3 | `exists(p: A => Boolean)(s: S): Boolean` | `ForgetfulFold[F]` | Predicate over visible foci — short-circuits on first hit. Today via `.foldMap(_ => true)(s)` under `Boolean` monoid (no short-circuit) — wastes work on long traversals. | `phonesEach.exists(_.isMobile)(person)` | ✅ shipped |
| 4 | `forall(p: A => Boolean)(s: S): Boolean` | `ForgetfulFold[F]` | Dual of `exists` — short-circuits on first miss. | `ageLens.forall(_ >= 18)(p)` (pointwise: returns `true` iff age ≥ 18). | 🔵 deferred |
| 5 | `find(p: A => Boolean)(s: S): Option[A]` | `ForgetfulFold[F]` | First focus matching the predicate. Composes `headOption ∘ filter`-style. | `phonesEach.find(_.isMobile)(person)` | 🔵 deferred |
| 6 | `count(p: A => Boolean)(s: S): Int` | `ForgetfulFold[F]` | Specialisation of `length` to a predicate. Useful for "how many of these match" without materialising the filtered list. | `phonesEach.count(_.isMobile)(person)` | 🔵 deferred |
| 7 | `toList(s: S): List[A]` | `ForgetfulFold[F]` | Materialise foci as a List. Today users build it via `.foldMap(List(_))(s)` — that is the implementation, but `.toList` is the readable surface and matches Monocle's `Fold.getAll`. | `phonesEach.toList(person)` | 🔵 deferred |
| 8 | `getMaximum[Ord >: A](s: S)(using Order[Ord]): Option[A]` | `ForgetfulFold[F]` | Maximum focus under an order. Cats's `Foldable.maximumOption` does this in one pass; we can route through it. Useful for "highest-rated review", "most-recent timestamp". | `reviewRating.getMaximum(reviews)` | 🔵 deferred |
| 9 | `sum(s: S)(using Numeric[A]): A` | `ForgetfulFold[F]` | Sum visible foci — common enough it deserves an alias for `.foldMap(identity[A])(s)` under `Monoid[A]`. | `lineItemPriceTraversal.sum(invoice)` | 🔵 deferred |
| 10 | `productAll(s: S)(using Numeric[A]): A` | `ForgetfulFold[F]` | Multiply visible foci. Less universally useful than `.sum` but symmetric. | `factorTraversal.productAll(s)` | 🔵 deferred |
| 11 | `replaceA[G[_]: Applicative](gb: G[B])(s: S): G[T]` | `ForgetfulTraverse[F, Applicative]` | Apply an effect to *replace* the focus, not just modify. Equivalent to `modifyA(_ => gb)`. Reach for it when the new focus value already lives in `G` (e.g. async DB read). | `phoneNumber.replaceA(IO.fetchNumber(id))(person)` | 🔵 deferred |
| 12 | `traverseVoid[G[_]: Applicative](f: A => G[Unit])(s: S): G[Unit]` | `ForgetfulTraverse[F, Applicative]` | Run an effect at every focus, drop the resulting `T`. Cats has this on `Foldable` and `Traverse` for the same allocation reason — building the `G[T]` is wasted work when the caller drops it. | `phonesEach.traverseVoid(p => log("seen", p))(person)` | 🔵 deferred |
| 13 | `partition(f: A => Either[B, B])(s: S): (List[A], List[A])` | `MultiFocus[F]` + `Alternative[F] + Foldable[F]` (uses `Foldable.partitionEither`) | Split a multi-focus into two halves by classification. Builds on cats's `Foldable.partitionEither`. Useful for "active vs archived" / "passing vs failing tests". | `phonesEach.partition(p => Either.cond(p.isMobile, p, p))(person)` | 🔵 deferred |
| 14 | `mapAccumulate[Acc, B](init: Acc)(f: (Acc, A) => (Acc, B))(s: S): (Acc, T)` | `Traverse[F]` (carriers backed by `Traverse[F]`) | Stateful traversal — combine `foldLeft` and `modify` in one pass. Common idiom in v1 monocle's `traverseS`. Implementation: `o.modifyA[State[Acc, *]]` over the State monad's `Applicative`. | `phonesEach.mapAccumulate(0)((idx, p) => (idx+1, p.copy(label = idx)))(person)` | 🔵 deferred |
| 15 | `firstOption(s: S): Option[A]` (alias of `headOption`) | `ForgetfulFold[F]` | Naming sugar — Monocle calls this `headOption` on Fold and `firstOption` on Traversal. `headOption` is more descriptive; aliasing both keeps users from typing the wrong one. | `firstPhone.firstOption(person)` | 🔵 deferred — ship under one name |
| 16 | `getAll(s: S): List[A]` | `ForgetfulFold[F]` | Monocle's name for `.toList`. Drop the alias; only ship `.toList` for clarity. | n/a | 🔵 deferred — alias only if user demand |
| 17 | `at(i: Int)(s: S): Option[A]` (Foldable) | `ForgetfulFold[F]` | Pull the i-th visible focus; `Foldable.get` does it in O(i). Distinct from the existing `MultiFocus.at` which requires `Representable[F]` (O(1) for Naperian shapes). | `phonesEach.at(2)(person)` returns the third phone | 🔵 deferred |
| 18 | `intercalate(sep: A)(s: S)(using Monoid[A]): A` | `ForgetfulFold[F]` | Concatenate visible foci with a separator — useful when the focus type is `String`, `Chain`, or anything monoidal. Cats's `Foldable.intercalate` already does this. | `tagsEach.intercalate(", ")(post)` | 🔵 deferred |
| 19 | `nonEmpty(s: S): Boolean` | `ForgetfulFold[F]` | Inverse of `isEmpty` — a O(1) check (because `Foldable.nonEmpty` short-circuits on the first focus). Same shape as `exists(_ => true)` but with a cleaner name. | `phonesEach.nonEmpty(person)` | 🔵 deferred |
| 20 | `foldRight[B](z: Eval[B])(f: (A, Eval[B]) => Eval[B])(s: S): Eval[B]` | `ForgetfulFold[F]` (with stronger `Foldable[F]` for the carrier) | Right-fold path through the foci. cats's `Foldable.foldRight` does this; cats-eo could surface it for users who want explicit lazy evaluation. | `phonesEach.foldRight(Eval.now(0))((p, acc) => Eval.defer(acc.map(_ + 1)))` | 🔵 deferred |

## Top-3 picks shipped in this branch

The first three in the table above are shipped because they form the
load-bearing read surface that turns `Forget[F]` and `MultiFocus[F]`
optics into something a user can read with. Today the project has no
ergonomic way to "look at the foci of a Traversal"; users have to
`foldMap(List(_))(s)` and post-process. The three new methods cover the
three most-typed operations in any optics-using codebase.

### #1 — `.headOption(s)` on any `ForgetfulFold[F]` carrier

```scala
extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using FF: ForgetfulFold[F])
  inline def headOption(s: S): Option[A] =
    given Monoid[Option[A]] =
      Monoid.instance(None, (a, b) => a.orElse(b))
    FF.foldMap(using summon[Monoid[Option[A]]])((a: A) => Some(a): Option[A])(o.to(s))
```

(The implementation in the actual ship uses a custom first-Some monoid —
see `core/src/main/scala/dev/constructive/eo/optics/Optic.scala`.)

### #2 — `.length(s)` on any `ForgetfulFold[F]` carrier

```scala
extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using FF: ForgetfulFold[F])
  inline def length(s: S): Int =
    FF.foldMap(using cats.Monoid[Int])((_: A) => 1)(o.to(s))
```

### #3 — `.exists(p)(s)` on any `ForgetfulFold[F]` carrier

```scala
extension [S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using FF: ForgetfulFold[F])
  inline def exists(p: A => Boolean)(s: S): Boolean =
    FF.foldMap(using cats.Monoid.instance(false, (a: Boolean, b: Boolean) => a || b))(p)(o.to(s))
```

Note: `Boolean`'s `Monoid` under disjunction (`||`) doesn't ship in
cats by default. We materialise it inline in the extension method,
which is `inline def` so the per-call cost is the same as a hand-rolled
`foldLeft(false)(_ || _)`.

## Highest-value candidate NOT shipped: `.toList(s)` (#7)

`.toList` is more frequently used in everyday code than `.length` or
`.exists`. The reason it didn't make the top-3 ship cut:

- `.toList` already has a near-equivalent today: users write
  `.foldMap(List(_))(s)` (it's two characters longer and one allocation
  expensive — `List(_)` boxes + a per-call `Monoid[List].combine`).
- `.headOption` and `.exists` benefit *substantially* from
  short-circuiting; `.toList` does not.
- The implementation of `.toList` for `Forget[F]` carriers needs to
  thread through `Foldable[F].toList` rather than the
  `ForgetfulFold[F]`-typeclass-only path, because the latter only
  exposes a `foldMap`. We can either (a) re-implement via foldMap with a
  Chain accumulator, or (b) tighten the carrier requirement. Either is
  a separate decision the user should approve.

Ship `.toList` next; it's a one-liner once the carrier requirement is
chosen.

## Method ordering rationale (full table → ship list)

The 20-row table is ordered from highest user-frequency / lowest
implementation risk → lowest. The ship cut is the first three rows
because:

- They're each ≤ 5 lines of implementation.
- They're each gated on the lowest-priority typeclass we ship
  (`ForgetfulFold[F]`), which means every read-side optic in cats-eo
  picks them up at once — Fold, Traversal, AffineFold, Optional,
  Prism, MultiFocus[F].
- They're orthogonal: shipping #1 doesn't constrain #2 or #3 in any way.

Rows 4–10 (predicate / monoid extensions) are all the same shape and
could land as a follow-up batch once the user reviews this doc.
Rows 11–14 require `ForgetfulTraverse` (more powerful) or carrier
specialisation; lower priority because the existing `.modifyA` and
`.foldMap` cover most needs and the marginal value is smaller.

Rows 15–20 are either aliases (15, 16) or specialised (17–20) and
should ship only if specific user demand surfaces.

## Out-of-table observation: `Optic[…, MultiFocus[F]]`-specific extensions

The dossier asked about MultiFocus-specific methods. The current
companion already has `.collectMap` / `.collectList` / `.at(i)`. A
candidate the survey turned up but didn't include in the ranked table:

- `.partitionEither(f: A => Either[B, C])(s: S): (F[B], F[C])` — reuses
  cats's `Foldable.partitionEither`. Specialises to MultiFocus[F]
  because we need an `Alternative[F]` to combine the two halves into
  the same carrier shape. Today the surface is empty — the user would
  reach for cats's `Foldable.partitionEither` over the read result of
  `.toList(s)`. Modest user reach (the use case is more uncommon than
  `.headOption`); see row 13.

These are all candidates for a future batch.

## Scala-3 leverage call-out

The shipped extensions use `inline def` with `(using FF: ForgetfulFold[F])`
context bounds — exactly the dossier's item 4 ("Extension methods with
typeclass-gated `using` clauses"). The capability-gating means `.length`
on a Lens compiles only when the user has a `ForgetfulFold[Tuple2]` in
scope (cats-eo ships one). On a Setter, where no `ForgetfulFold[SetterF]`
exists, the call simply fails to compile — the right behaviour, since
SetterF is a write-only carrier with no read path.

`transparent inline def` — dossier item 5 — was considered for the
return-type refinement story, but `Boolean`, `Int`, `Option[A]` are
already maximally-precise return types, so transparency would gain
nothing.

`scala.compiletime.error` — dossier item 12 — was considered for a
"can't call `.headOption` on a Setter" friendly error message. The
existing typeclass-gating already produces a "no `ForgetfulFold[SetterF]`
in scope" error at the call site, which is informative enough. No
additional compile-time machinery needed.
