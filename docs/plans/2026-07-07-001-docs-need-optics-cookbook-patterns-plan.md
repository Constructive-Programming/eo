# Plan — fold the "We Need More Optics" patterns into the cookbook

Source: Hansen — *We Need More Optics*, The Startup, 2020-08-14
(`~/Documents/need-optics.pdf`). Six patterns; mapping against
`site/docs/cookbook.md` as of `eaf2da1`. Ponytail-reviewed: recipe 2
folded into recipe 1, concepts sidebar and fs2 cross-ref cut.

## Pattern inventory → coverage

| # | Article pattern | Cookbook status | Action |
|---|-----------------|-----------------|--------|
| 1 | Effectful lens — `save(sheet).map(BalanceSheetId.set(_)(sheet))` | **Covered** — Theme G "Persist-and-stamp" already cites the article's gist | none |
| 2 | Cross-family composition as re-use pitch | **Covered** — Theme F ladder | none |
| 3 | Sibling-field traversal — `sheetTimes: BalanceSheet ⇒ every DateTime` via `Traversal.applyN` | **Missing** | Recipe 1 |
| 4 | Modular application design — depend on the optic, not on `BalanceSheet` | **Missing** | folded into Recipe 1 (closing fence) |
| 5 | Re-usable law tests — `checkAll("balanceSheetTimes", TraversalTests(sheetTimes))` | **Missing** from cookbook; laws module ships exactly this | Recipe 3 |
| 6 | Optics as a learning tool — Iso = biconditional, Lens = weakened implication | Not cookbook material | cut (pedagogy; revisit for concepts.md separately if ever) |

The article's streaming `modifyF`-into-fs2 pattern is deferred: fs2
is not a dependency, and Theme G's validate-in-place + batch-load
recipes already carry the "the effect type decides" story.

## Recipe 1 — Every timestamp on the sheet (sibling fields into one traversal)

Slot: Theme C, after "`each` past the traversal".

**Composition finding → core fix (TDD, executed):**
`Traversal.two`'s original carrier was `MultiFocus[Function1[Int, *]]`,
whose arity is erased inside the lookup closure — no generic
`Composer[MultiFocus[Function1[Int, *]], MultiFocus[F]]` can exist, so
`Traversal.two(...).andThen(each)` did not compile. Since every
constructor in `Traversal.scala` is expected to compose, the fix went
into core: `two/three/four` know their arity at construction time, so
they now tabulate straight into `MultiFocus[PSVec]` and compose like
`each` in both directions. Red/green pinned by
`tests/.../FixedArityTraversalSpec.scala` (two∘each, each∘two,
two∘each∘two, Lens-drilled with siblings preserved, two∘Lens,
two∘Prism, three/four∘Lens, foldMap read, composed-carrier witness).
The Grate-shaped `Function1[Int, *]` encoding remains available as
`MultiFocus.tuple`. Doc fallout updated: optics.md fixed-arity
paragraph, multifocus.md sub-shapes row + composition-limits note.

Recipe shape (the article's original spelling, now valid):

```scala
val sheetTransactions =               // both sibling lists as one traversal
  Traversal.two[Sheet, Sheet, List[Transaction], List[Transaction]](
    _.froms, _.tos, Sheet(_, _))
val transactionTimes =
  Traversal.two[Transaction, Transaction, Instant, Instant](
    _.issue, _.acknowledge, Transaction(_, _))

val sheetTimes = sheetTransactions
  .andThen(Traversal.each[List, Transaction])
  .andThen(transactionTimes)          // carrier: MultiFocus[PSVec]

def increaseTimesBy(secs: Long) = sheetTimes.modify(_.plusSeconds(secs))
```

Caveat that stays: `reverse` sees only the foci (full-cover), so
sibling fields need a Lens drill first — the composed seam preserves
them.

Close with the article's modular-design payoff as one short fence +
two sentences (folded former recipe 2 — the whole point is one line):

```scala
def adjustTimes[S](t: Optic[S, S, Instant, Instant, MultiFocus[PSVec]])(
    f: Instant => Instant): S => S = t.modify(f)
```

One caveat sentence: `Traversal.two` handles the fixed-arity shape
only as a terminal / post-Iso hop — link the multifocus.md seam note.

## Recipe 3 — Law-check your own optic

Slot: short subsection at the tail of Further reading (no new Theme
header for one recipe). Plain `scala` fences (not mdoc), matching how
migration-from-monocle.md renders its `checkAll` — no docs-build
dependency change. Spelling (compile-verified): constant-functor
type lambda `TraversalTests[[X] =>> Sheet, Instant]` +
`TraversalLaws` with `val traversal = sheetTimes`; note the
`Arbitrary[Sheet]` / `Cogen[Instant]` obligations in prose.

## While-in-the-file cleanups (from the ponytail review of the page)

- Theme G persist-and-stamp: replace the 5-line hand-rolled `IO`
  class with `cats.Eval` (already on the classpath); `.unsafeRun()`
  becomes `.value`.
- ~~Theme C `okP` / Theme F `paidP` via `prism[S, A]`~~ — **dropped**:
  the prism macro crashes the compiler on enums defined inside
  mdoc's nested wrappers (silent "no classfiles" mdoc failure;
  same top-level-ADT constraint the generics test samples obey).
  Hand-rolled prisms stay in the cookbook.

## Mechanics

- Every new compiled fence goes through `sbt docs/mdoc`; pre-commit
  runs `scalafmtCheckAll; docs/mdoc; docs/laikaSite`.
- House format per recipe: **Why** → silent setup fence → visible
  result fence → caveat → **Source** (cite
  <https://medium.com/swlh/we-need-more-optics-3ee2010b1d1c> — verify
  slug before committing).
- One PR: Recipe 1 (+ folded modular-design fence) → Recipe 3 →
  cleanups.
