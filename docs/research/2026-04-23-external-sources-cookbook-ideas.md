# External sources — cookbook ideas and site diagrams for 0.1.0

Research artifact. Two tracks:

1. **Cookbook / example mining** — recipes harvested from the optics
   literature (books, blog series, official docs) that translate to
   cats-eo's surface, each scored for whether cats-eo's unique angles
   (existential carriers, cross-carrier `.andThen`, JsonPrism
   Ior-surface, PowerSeries, AlgLens) make the example more
   instructive than its Haskell / Monocle-on-Scala original.
2. **Site diagrams** — candidate visuals for the Laika-rendered
   [`site/docs/`](../../site/docs) tree, with keep/drop
   recommendations and a ranked shortlist.

Compiled 2026-04-23. Indexed-optic recipes are explicitly **out of
scope** for 0.1.0 (see [`optic-families-survey.md`](2026-04-19-optic-families-survey.md#indexed-variants-the-big-parallel-hierarchy));
any that appear are cross-referenced to the "deferred" column only.

Source-attribution convention per recipe: `(Author — Title, year,
URL)`. Paraphrased throughout — no verbatim quotes.

---

## Track 1 — Cookbook / example mining

### Source survey

The recipe list below is drawn from these sources. "Mined" means I
read a summary or the page itself and extracted at least one
vignette; "seen" means a reference surfaced in search and was
cross-checked but didn't yield new material beyond what other
sources already covered.

**Books**

- Chris Penner — *Optics By Example* (Leanpub, 2020). Mined: lens
  records / composition, folds / filtering / queries, custom
  traversals + laws, prisms and adapters, polymorphic optics,
  classy optics, `partsOf`. Chapters on indexed structures noted
  but cross-referenced only.
  <https://leanpub.com/optics-by-example/>
- Scott Wlaschin — *Domain Modeling Made Functional* (Pragmatic
  Bookshelf, 2018). Seen — Wlaschin's style is record update /
  variant case decomposition rather than lens-library use, so the
  translations here are structural rather than direct.
  <https://pragprog.com/titles/swdddf/domain-modeling-made-functional/>
- Sandy Maguire — *Thinking with Types* (self-published, 2018).
  Seen — type-level focus, limited optics material.
  <https://thinkingwithtypes.com/>

**Blog posts / long-form**

- Chris Penner's blog — every optics post; especially mined:
  *Algebraic lenses* <https://chrispenner.ca/posts/algebraic>,
  *Intro to Kaleidoscopes* <https://chrispenner.ca/posts/kaleidoscopes>,
  *Virtual Record Fields Using Lenses* <https://chrispenner.ca/posts/virtual-fields>,
  *Composable filters using Witherable optics* <https://chrispenner.ca/posts/witherable-optics>,
  *Generalizing 'jq' and Traversal Systems* <https://chrispenner.ca/posts/traversal-systems>,
  *Optics + Regex* <https://chrispenner.ca/posts/lens-regex-pcre>,
  *Using traversals to batch database queries* <https://chrispenner.ca/posts/traversals-for-batching>,
  *Advent of Optics* days 1–4
  <https://chrispenner.ca/posts/advent-of-optics-01> … <https://chrispenner.ca/posts/advent-of-optics-04>.
- Bartosz Milewski — *Profunctor Optics: The Categorical View*
  <https://bartoszmilewski.com/2017/07/07/profunctor-optics-the-categorical-view/>.
  Mined for motivation only; the post is theory-first with few
  concrete vignettes.
- Oleg Grenrus — *Glassery* (2017)
  <https://oleg.fi/gists/posts/2017-04-18-glassery.html>. Seen —
  van-Laarhoven-era reference.
- Luis Borjas Reyes — *Taking a close look at Optics* (2018)
  <https://tech.lfborjas.com/optics/>. Mined for the lunar-phase
  example (composed Lens + Prism + Traversal on real domain data).
- Jonas Chapuis — *Optics: a hands-on introduction in Scala* (2018)
  <https://jonaschapuis.com/2018/07/optics-a-hands-on-introduction-in-scala/>.
  Referenced via search summary (site blocked direct fetch); uses
  Monocle-flavoured examples that carry over straight.

**Libraries / official docs**

- Monocle — <https://www.optics.dev/Monocle/docs/focus>. Mined the
  three Focus examples: required field, optional field, list index.
- circe-optics — <https://circe.github.io/circe/optics.html>. Mined
  for `each` / `JsonPath` idioms; direct fetch failed (403 /
  redirect) but the GitHub tree at
  <https://github.com/circe/circe-optics> and the GitHub-hosted
  issue/test files are the load-bearing reference.
- Haskell `lens` — Gabriella Gonzalez's
  `Control.Lens.Tutorial`
  <https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>
  (Atom / Molecule coordinate-shifting examples; lens composition).
- Typelevel blog — *Error handling in Http4s with classy optics*
  <https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html>
  (direct fetch timed out, but the title plus the surrounding
  search summaries pin the pattern).
- ZIO Optics — Jorge Vasquez's Scalac eBook
  <https://scalac.io/ebook/improve-your-focus-with-zio-optics/>.
  Referenced (direct fetch blocked); the ZIO Optics surface is a
  close cousin to Monocle's so its recipes duplicate ones I
  extracted elsewhere.

**Talks / recorded**

- Julien Truffaut — *Monocle 3: A Peek into the Future*, Scala in
  the City, 2020
  <https://www.youtube.com/watch?v=oAkiny6BzL0>. Seen; informed
  the Monocle-vs-EO framing but yielded no new vignettes beyond
  the Focus docs.
- Bartosz Milewski — *Introduction to Profunctor Optics* (YouTube)
  <https://www.youtube.com/watch?v=3HIJCibf8Qc>. Seen; theory.

### Recipes, grouped by theme

Every recipe follows the same shape:

- **Problem** — one-line "I want to…".
- **Optics used** — referenced to cats-eo's surface.
- **Outline** — five-to-ten-line pseudo-Scala sketch, not
  compile-ready.
- **Source** — author + title + URL + date if available.
- **Why it's instructive** — the mental model it unlocks.
- **cats-eo angle** — what a cats-eo-specific rendering would add
  over the original.
- **Gap** (optional) — marked only when the recipe can't be
  expressed today.

The slot where each recipe lands in the site tree is called out
before it; `site/docs/cookbook.md` is the default, with
`circe.md` / `optics.md` / `extensibility.md` as alternatives.

---

#### Theme A — Product editing (Lens chains)

##### 1. Edit a deeply-nested coordinate (the Atom/Molecule classic)

Slot: **cookbook.md** — duplicates the existing "deeply-nested field"
entry at a slightly more *classic* framing; adjacent recipe, not
replacement.

- **Problem**: increment the `x` coordinate of every `Atom` inside a
  `Molecule`.
- **Optics**: `lens[Molecule](_.atoms)` → `Traversal.each` →
  `lens[Atom](_.point)` → `lens[Point](_.x)`.
- **Outline**:

      val xs =
        lens[Molecule](_.atoms)
          .andThen(Traversal.each[List, Atom])
          .andThen(lens[Atom](_.point))
          .andThen(lens[Point](_.x))
      xs.modify(_ + 1)(mol)

- **Source**: Gonzalez — *Control.Lens.Tutorial*, Hackage,
  <https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>.
- **Why**: this is the reference example every Haskell tutorial
  opens with. Translating it cleanly through four composition hops
  is the user's first *proof* that the library isn't clumsier than
  Haskell's.
- **cats-eo angle**: same-carrier Lens hops fuse (the `Tuple2`
  `AssociativeFunctor`), and the cross-carrier hop into
  `PowerSeries` at `.each` happens without a manual morph. A
  side-by-side vs. Monocle is a gentle way to introduce
  `Composer[Tuple2, PowerSeries]` before the theory page.

##### 2. Virtual-field lens — Celsius ↔ Fahrenheit facade

Slot: **cookbook.md**.

- **Problem**: expose a `fahrenheit` handle on a `Temperature`
  value whose underlying representation is Celsius, so callers
  can `view` / `set` without knowing.
- **Optics**: `Iso[Temperature, Double](_.toC, Temperature(_))`
  composed with a `BijectionIso[Double, Double](c2f, f2c)`.
- **Outline**:

      val celsius: BijectionIso[Temperature, Temperature, Double, Double] =
        BijectionIso(_.toC, Temperature(_))
      val c2f: BijectionIso[Double, Double, Double, Double] =
        BijectionIso(c => c * 9.0/5 + 32, f => (f - 32) * 5.0/9)
      val fahrenheit = celsius.andThen(c2f)

- **Source**: Penner — *Virtual Record Fields Using Lenses*,
  <https://chrispenner.ca/posts/virtual-fields>.
- **Why**: the "lens as public API boundary" mental model. A
  refactor can redefine the underlying representation and the
  facade compiles unchanged — a stronger selling point than
  "avoid `.copy`".
- **cats-eo angle**: cats-eo's `BijectionIso.andThen(BijectionIso)`
  is fused through the concrete-subclass override, so the refactor
  story doesn't cost per-hop typeclass dispatch.

##### 3. Drilling into a product via a macro-derived multi-field lens

Slot: **generics.md** — extends the existing multi-field section
with a cookbook framing.

- **Problem**: focus `(quantity, price)` on an `OrderItem`, modify
  both atomically, then persist.
- **Optics**: `lens[OrderItem](_.quantity, _.price)` — partial
  cover → `SimpleLens[OrderItem, NamedTuple, NamedTuple]`.
- **Outline**:

      val qp = lens[OrderItem](_.quantity, _.price)
      qp.modify { nt =>
        (quantity = nt.quantity * 2, price = nt.price * 0.9)
      }(order)

- **Source**: cats-eo itself (novel — no Monocle or Haskell
  `lens` equivalent); motivated by Julien Truffaut's *Monocle 3*
  talk describing the pain of N hand-composed `GenLens`es
  <https://www.youtube.com/watch?v=oAkiny6BzL0>.
- **Why**: demonstrates a cats-eo-unique capability (varargs
  `lens` with NamedTuple focus) in a use case (atomic multi-field
  update) that's genuinely awkward in Monocle.
- **cats-eo angle**: load-bearing — this is one of the strongest
  "why EO" recipes.

##### 4. Full-cover Iso upgrade — wrapper newtype ↔ NamedTuple

Slot: **generics.md** or **migration-from-monocle.md**.

- **Problem**: a single-field `case class ProductCode(value: String)`
  needs a bijective view as `NamedTuple[("value",), (String,)]` for
  serialization boundaries.
- **Optics**: `lens[ProductCode](_.value)` returns a
  `BijectionIso`, not a `SimpleLens`, because coverage is total.
- **Outline**:

      val iso = lens[ProductCode](_.value)   // BijectionIso
      iso.reverseGet((value = "abc-42"))     // → ProductCode("abc-42")
      iso.modify(_.toUpperCase)(pc)

- **Source**: cats-eo itself (novel).
- **Why**: surprising ergonomics — the user writes a lens and the
  macro upgrades it automatically. Anchors the "look for the
  full-cover tell" mental habit.
- **cats-eo angle**: unique to cats-eo, no Monocle counterpart.

---

#### Theme B — Sum-branch access (Prism)

##### 5. Branch into an enum case and modify inside the branch

Slot: **cookbook.md** — already present in a thin form, strengthen
with the fused chain.

- **Problem**: if the input is a `Click`, bump `x`; otherwise pass
  through.
- **Optics**: `prism[Input, Input.Click].andThen(lens[Input.Click](_.x))`.
- **Outline**:

      val clickX =
        prism[Input, Input.Click].andThen(lens[Input.Click](_.x))
      clickX.modify(_ + 1)(Input.Click(10, 20))   // hits
      clickX.modify(_ + 1)(Input.Scroll(5))       // passes through

- **Source**: Baeldung — *Monocle Optics*,
  <https://www.baeldung.com/scala/monocle-optics>. RockTheJVM —
  *Lenses, Prisms, and Optics in Scala*,
  <https://rockthejvm.com/articles/lenses-prisms-and-optics-in-scala>.
- **Why**: makes explicit that Prism miss-branch doesn't
  short-circuit the whole pipeline — the downstream `.modify`
  fires only on the hit branch.
- **cats-eo angle**: `Composer[Either, Tuple2]` goes through Affine;
  the worked chain shows the bridge in action and motivates
  `Morph.bothViaAffine`.

##### 6. The F# expression-tree case-dispatch use case

Slot: **cookbook.md**.

- **Problem**: for a family of `Expr` AST cases, update just the
  `Var` case's name without touching `App` / `Lam` subtrees.
- **Optics**: `prism[Expr, Expr.Var].andThen(lens[Expr.Var](_.name))`.
- **Outline**:

      enum Expr:
        case Var(name: String)
        case App(f: Expr, x: Expr)
        case Lam(bind: String, body: Expr)

      val varName =
        prism[Expr, Expr.Var].andThen(lens[Expr.Var](_.name))
      varName.modify(_.toUpperCase)(Expr.Var("x"))

- **Source**: Wlaschin — *Domain Modeling Made Functional*, ch. 4
  (discriminated unions);
  <https://pragprog.com/titles/swdddf/domain-modeling-made-functional/>.
  Translation is structural, not line-by-line.
- **Why**: DDD-friendly framing — Prisms are the optic for
  discriminated-union case dispatch.

##### 7. Reverse-only Review — ID construction

Slot: **optics.md** — augments the existing Review section with a
realistic use case.

- **Problem**: build a `UserId` from a `UUID` safely through a
  Review so tests and prod share one construction path.
- **Optics**: `Review[UserId, UUID](UserId.apply)`.
- **Outline**:

      val userIdR: Review[UserId, UUID] = Review(UserId(_))
      val u = userIdR.reverseGet(UUID.randomUUID())

- **Source**: Penner — *Optics By Example*, ch. 13 (polymorphic +
  reverse), <https://leanpub.com/optics-by-example/>.
- **Why**: names the "build-only" optic as first-class so team
  members stop reaching for bare smart constructors. Contrast with
  Getter for read-only.

---

#### Theme C — Collection walks (Traversal)

##### 8. Map over every element, terminate at the traversal — `forEach`

Slot: already present (`cookbook.md`), keep.

- **Problem**: increment every `Int` in a `List`.
- **Optics**: `Traversal.forEach[List, Int, Int]`.
- **Outline**:

      val every = Traversal.forEach[List, Int, Int]
      every.modify(_ + 1)(List(1, 2, 3))

- **Source**: Penner — *Optics By Example*, ch. 7 (Simple
  Traversals), <https://leanpub.com/optics-by-example/>.
- **Why**: names the "tight map-only path" and sets up the
  `each` / `forEach` contrast.

##### 9. Walk a nested structure *through* a traversal — `each`

Slot: already present (`cookbook.md`), keep.

- **Problem**: for every `Phone` inside `Subscriber`, toggle
  `isMobile`.
- **Optics**: `lens[Subscriber](_.phones).andThen(Traversal.each).andThen(lens[Phone](_.isMobile))`.
- **Outline**: see existing cookbook page.
- **Source**: Gonzalez — *Control.Lens.Tutorial*,
  <https://hackage.haskell.org/package/lens-tutorial-1.0.5/docs/Control-Lens-Tutorial.html>.
- **cats-eo angle**: PowerSeries's `PSSingleton` protocol is
  what keeps this under 2× of hand-written code at size 4 and
  amortises down toward 1.9× at size 1024; link to the benchmarks
  page for the curve.

##### 10. Traversal over a Prism — skip the misses cleanly

Slot: **cookbook.md**.

- **Problem**: for each `Result` in an `ArraySeq[Result]` where
  `Result = Ok(Int) | Err(String)`, bump `Ok.value` by one; leave
  `Err` untouched.
- **Optics**: `Traversal.each[ArraySeq, Result].andThen(prism[Result, Result.Ok]).andThen(lens[Result.Ok](_.value))`.
- **Outline**:

      val bumpOks =
        Traversal.each[ArraySeq, Result]
          .andThen(prism[Result, Result.Ok])
          .andThen(lens[Result.Ok](_.value))
      bumpOks.modify(_ + 1)(arr)

- **Source**: Penner — *Optics By Example*, ch. 8 (Traversal
  Actions) + ch. 10 (Missing Values),
  <https://leanpub.com/optics-by-example/>. Measured in cats-eo
  as `PowerSeriesPrismBench`
  ([bench](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/PowerSeriesPrismBench.scala)).
- **Why**: the "sparse traversal" is the single most annoying
  case to hand-roll. The one-liner via optics + the published
  ratio-to-naive number is the hook.
- **cats-eo angle**: direct bench-backed story. 5× naive on
  `PowerSeriesPrismBench` is documented in
  [benchmarks.md](../../site/docs/benchmarks.md#powerseries-traversal-with-downstream-composition);
  the cookbook recipe is the prose side.

##### 11. Partition-by-threshold (algebraic lens / AlgLens teaser)

Slot: **cookbook.md** — novel, strong lead-in to an AlgLens
section.

- **Problem**: split a `List[Int]` into two buckets — below and
  above the list's own mean.
- **Optics**: `AlgLens.fromLensF[List, List[Int], Int]` + an
  aggregator applied through `>-` (cats-eo equivalent: the
  AlgLens's `modify` with a closure that inspects the `F[A]`
  focus).
- **Outline**:

      // pseudo-code for the shape; exact API is AlgLens-specific
      val mean = xs => xs.sum.toDouble / xs.length
      val partition: List[Int] => (List[Int], List[Int]) =
        AlgLens.fromLensF[List, ...].classify { xs =>
          val m = mean(xs)
          xs.partition(_ < m)
        }

- **Source**: Penner — *Algebraic lenses*,
  <https://chrispenner.ca/posts/algebraic>.
- **Why**: textbook demonstration of "the update function needs
  the whole `F[A]`, not just a single `A`" — that's the signature
  of an algebraic lens vs. a plain traversal.
- **cats-eo angle**: AlgLens is a novel carrier in cats-eo.
  Having a recipe that shows an aggregation flowing through the
  classifier is the entry point.
- **Gap**: the pseudo-code's `.classify` surface isn't public in
  v0.1.0. Either expose it or frame the recipe around
  `.modify` + a closure that carries `F[A]` explicitly.

##### 12. Iris classifier — algebraic lens on a flower dataset

Slot: **cookbook.md** (follow-on to recipe 11) or a dedicated
`docs/plans/2026-04-22-002-feat-iris-classifier-example.md`
already tracked in the plan directory.

- **Problem**: given an input flower's measurements, classify it
  against a reference dataset of labelled measurements.
- **Optics**: `AlgLens.fromLensF[List, Flower, Measurements]` +
  a k-NN-style aggregator.
- **Outline**: see the plan file; a handful of lines plus the
  dataset.
- **Source**: Román/Clarke/Elkins/Gibbons/Milewski/Loregian/Pillmore
  — *Profunctor Optics: A Categorical Update*, NWPT 2019 abstract,
  <https://cs.ttu.ee/events/nwpt2019/abstracts/paper14.pdf>;
  Penner — *Algebraic lenses*, <https://chrispenner.ca/posts/algebraic>.
- **Why**: the canonical motivating example for algebraic lenses.
  Pairs well with the partition recipe to ground the pattern.

---

#### Theme D — JSON / tree editing

##### 13. Edit one leaf deep in a JSON tree (no decode)

Slot: already present (`circe.md`), keep and reinforce.

- **Problem**: change `user.address.street` in a 40-field JSON
  record without decoding the full record.
- **Optics**: `codecPrism[User].address.street`.
- **Outline**: see existing circe.md page.
- **Source**: cats-eo (`JsonPrism`) — no Haskell or Monocle
  direct equivalent. Related: circe-optics' `root.user.address.street`
  <https://circe.github.io/circe/optics.html>, but that forces a
  full decode.
- **cats-eo angle**: this is the headline JsonPrism story. The
  2× speedup at every depth is documented in
  [benchmarks.md](../../site/docs/benchmarks.md#jsonprism-cursor-backed-json-edit).

##### 14. Edit every element of a JSON array — `.each.name`

Slot: already present (`circe.md`), keep.

- **Problem**: uppercase `items[*].name` inside `Basket`.
- **Optics**: `codecPrism[Basket].items.each.name`.
- **Outline**: see existing page.
- **Source**: cats-eo (`JsonTraversal`). Related: circe-optics'
  `root.order.items.each.quantity.int.modify(_ * 2)`,
  <https://circe.github.io/circe/optics.html>.
- **cats-eo angle**: the Ior-bearing default surface lets you
  discriminate "one element failed to decode" from "the whole
  walk failed" — circe-optics silently drops per-element failures.

##### 15. Diagnose a silent JSON edit no-op (Ior surface)

Slot: already present (`circe.md`), keep.

- **Problem**: a deep `.modify` appears to do nothing; which
  path step refused?
- **Optics**: `codecPrism[User].address.street.modify(f)(badJson)` →
  inspect `Ior.Both` / `Ior.Left`.
- **Source**: cats-eo's Ior surface is novel (the 2026-Q1
  v0.2 design move); no Haskell counterpart ships structured
  per-step diagnostics for JSON cursor walks.
- **cats-eo angle**: load-bearing — one of the single strongest
  "why cats-eo" cases for Scala practitioners coming from
  io.circe.optics.

##### 16. jq-style path + predicate against JSON

Slot: **cookbook.md** (new) or **circe.md** (appendix).

- **Problem**: "for every `item` whose `price > 100`, uppercase
  its `name`" — expressible in jq as
  `.items[] | select(.price > 100) | .name |= ascii_upcase`.
- **Optics**: `codecPrism[Basket].items.each` + an in-lambda
  branch: if `price > 100`, uppercase `name`, else pass through.
  A closer Haskell-lens spelling via `filtered` isn't in cats-eo
  0.1.0.
- **Outline**:

      codecPrism[Basket].items.each
        .fields(_.name, _.price)
        .modifyUnsafe { nt =>
          if nt.price > 100 then (name = nt.name.toUpperCase, price = nt.price)
          else nt
        }(json)

- **Source**: Penner — *Generalizing 'jq' and Traversal Systems*,
  <https://chrispenner.ca/posts/traversal-systems>.
- **Why**: every Scala-on-Kafka / Scala-on-config user has
  reached for jq; showing an optic spelling makes the
  transition concrete.
- **cats-eo angle**: uses the NamedTuple multi-field focus —
  another cats-eo-unique capability.
- **Gap**: `filtered` / `withered` predicate-gated traversal
  would make this cleaner; that's AffineFold-adjacent work that's
  worth tracking as a 0.2 item, not a blocker.

##### 17. Tree-shaped recursive rename

Slot: **cookbook.md**.

- **Problem**: in a `Tree[Node]`, rename every node whose
  `label` matches a predicate.
- **Optics**: a traversal over the tree's children (reachable via
  `cats.Traverse` on the tree ADT) + a Lens into `label`.
- **Outline**:

      given Traverse[Tree] = ??? // user or derived
      val every =
        Traversal.each[Tree, Node].andThen(lens[Node](_.label))
      every.modify(oldToNew)(tree)

- **Source**: Stanislav Glebik — *RefTree talk*,
  <https://stanch.github.io/reftree/docs/talks/Visualize/>
  (practical tree-editing with optics, seen via search).
- **Why**: users always have a tree in their domain. Showing
  that `each` + `Traverse[Tree]` is enough is reassuring.
- **cats-eo angle**: highlights "bring your own `Traverse`" —
  the library doesn't need a tree carrier to walk one.

---

#### Theme E — Algebraic / classifier

##### 18. Mean-aggregate classifier over a column

Slot: **cookbook.md** (alongside recipes 11–12).

- **Problem**: for every row in a `List[Row]`, replace the row's
  score with the `(row.score - column_mean)` z-score-style diff.
- **Optics**: `AlgLens.fromLensF[List, Row, Double]` with an
  aggregator that computes the mean and applies the shift.
- **Outline**:

      val zshift: List[Row] => List[Row] =
        AlgLens[List, Row, Double].modifyWithAggregate { rows =>
          val m = rows.map(_.score).sum / rows.length
          rows.map(r => r.copy(score = r.score - m))
        }

- **Source**: Penner — *Algebraic lenses*,
  <https://chrispenner.ca/posts/algebraic> (mean-measurements
  example).
- **Why**: the "column-aware update" framing — every row's
  update depends on all rows — is the one that sells the family
  to data engineers.
- **Gap**: same surface caveat as recipe 11 — a
  `modifyWithAggregate`-shaped extension is the missing piece;
  today users write the aggregator + `AlgLens.fromLensF` by hand.

##### 19. Kaleidoscope column-wise aggregation (deferred)

Slot: **NOT the cookbook**. Record as future work in
`optic-families-survey.md`.

- **Problem**: across a `List[Row]` compute per-column mean and
  return a single "average row".
- **Optics**: Kaleidoscope — not in cats-eo 0.1.0.
- **Source**: Penner — *Intro to Kaleidoscopes*,
  <https://chrispenner.ca/posts/kaleidoscopes>.
- **Gap**: documented deferral, cross-reference from cookbook
  "see also" with one-line summary only.

---

#### Theme F — Write-only / read-only escape

##### 20. Getter for a derived projection (no write-back)

Slot: already present (`cookbook.md` — "derive a read-only view"),
keep.

- **Problem**: expose `name.head` as a `Getter` so callers
  can't try to set it.
- **Optics**: `Getter[Shopper, Char](_.name.head)`.
- **Source**: Monocle — Focus docs,
  <https://www.optics.dev/Monocle/docs/focus>; classical.
- **cats-eo angle**: Getter here is `Optic[…, Forgetful]` with
  `T = Unit`; the mismatch with the full optic trait is the
  anchor for the "Getter doesn't compose via `.andThen`" note
  that already lives in `optics.md#getter`.

##### 21. Setter for opaque bulk write (e.g. password-encrypt all users)

Slot: **optics.md** (strengthen Setter), **cookbook.md** (short
vignette).

- **Problem**: encrypt every password in a `UserDb` without
  exposing the plaintext to the call site.
- **Optics**: `Setter[UserDb, UserDb, String, String]`.
- **Outline**:

      val encryptAll: Setter[UserDb, UserDb, String, String] =
        Setter { f => db =>
          db.copy(users = db.users.view.mapValues(
            u => u.copy(password = f(u.password))
          ).toMap)
        }

- **Source**: Monocle — Optics doc,
  <https://www.optics.dev/Monocle/docs/optics>. Classical.
- **Why**: names "write-only" as a deliberate API boundary
  choice, not a limitation.

##### 22. AffineFold — predicate-narrowed read

Slot: already present (`optics.md`), augment.

- **Problem**: return `Some(age)` only when the subject is an
  adult; no write-back.
- **Optics**: `AffineFold(p => Option.when(p.age >= 18)(p.age))`.
- **Source**: Penner — *Optics By Example*, ch. 10 (Missing
  Values). Cats-eo's `AffineFold` is close to Haskell's
  `AffineFold`.
- **Why**: names a pattern that everyone writes by hand —
  "return `Option` iff predicate holds" — as a first-class
  optic.

---

#### Theme G — Composition laddering

##### 23. Three-family ladder: Iso → Lens → Prism → Traversal

Slot: **concepts.md** (practical demo) or **optics.md**.

- **Problem**: from a `UserTuple` (structurally the same as
  `UserRecord`), pull out the `orders: List[Order]`, filter to
  `PaidOrder`s, and increment their `amount`.
- **Optics**:

      userIso.andThen(userRecordOrders)
             .andThen(Traversal.each[List, Order])
             .andThen(paidOrderP)
             .andThen(amountL)

- **Outline**: five hops, carriers `Forgetful → Tuple2 →
  PowerSeries → Either → Tuple2`. The Composer chain does the
  lifting.
- **Source**: composition demo drawn from Chapuis' hands-on
  intro
  <https://jonaschapuis.com/2018/07/optics-a-hands-on-introduction-in-scala/>
  and Borjas' lunar-phase example
  <https://tech.lfborjas.com/optics/>.
- **Why**: one vignette that exercises every `Composer` bridge
  cats-eo ships, with no explicit `.morph` calls. The "look ma,
  no hands" payoff for the cross-carrier design.
- **cats-eo angle**: this is the single best showcase of the
  existential-carrier advantage — the per-pair Monocle overload
  table becomes one `Morph[F, G]` lookup per hop.

##### 24. Classy-optics-lite — a `HasPerson` trait exposing a Lens

Slot: **extensibility.md** (or a new `patterns.md`).

- **Problem**: a service trait wants to abstract over "has a
  `Person` somewhere" without caring where; implementors provide
  the `Lens`.
- **Optics**: a trait `trait HasPerson[S] { def person: Lens[S, Person] }`;
  instances picked up as `given`.
- **Outline**:

      trait HasPerson[S]:
        def person: Lens[S, Person]
      object HasPerson:
        given HasPerson[Order] = new:
          def person = lens[Order](_.buyer)

      def ageOf[S](s: S)(using h: HasPerson[S]): Int =
        h.person.andThen(lens[Person](_.age)).get(s)

- **Source**: Penner — *Optics By Example*, ch. 14 (Classy
  Optics). Typelevel blog — *Error handling in Http4s with
  classy optics*,
  <https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html>.
- **Why**: removes hard-coded dependencies on specific parent
  records — makes functions reusable across related shapes.
  Direct translation of Haskell's `makeClassy` style into
  Scala 3 givens.

---

#### Theme H — Effectful read (`modifyA`)

##### 25. Validate-in-place — age increment that fails on negative

Slot: already present (`cookbook.md` — "apply a function that
needs an effect"), keep.

- **Problem**: bump `age` by 1, but fail with `None` if input is
  negative.
- **Optics**: `shopperAgeL.modifyF[Option](…)`.
- **Source**: Monocle / Haskell-lens classic `traverseOf`,
  generalised.

##### 26. Batch-load nested IDs — O(N) → O(1) queries

Slot: **cookbook.md** (new recipe) or **extensibility.md**
(worked example of the effectful pathway).

- **Problem**: each node in a tree carries an ID that needs to
  resolve to a DB-backed record. Naive `.modifyA` fires one query
  per node. Collect all IDs through a traversal, issue one
  batched query, then `.modifyA` again to distribute results.
- **Optics**: `Traversal.pEach` + two passes through `modifyA`
  (one collect, one inject).
- **Outline**:

      val collect = treeEach.foldMap[List[Id]](id => List(id))(tree)
      val rows    = db.fetchAll(collect)  // one query
      val byId    = rows.groupMapReduce(_.id)(identity)((a, _) => a)
      treeEach.modify(id => byId(id))(tree)

- **Source**: Penner — *Using traversals to batch database
  queries*, <https://chrispenner.ca/posts/traversals-for-batching>.
- **Why**: the "100x-300x without restructuring" story. Load-
  bearing for the effectful-traversal pitch.

##### 27. Witherable-style filter-during-parse (deferred)

Slot: **NOT the cookbook**. Cross-reference only.

- **Problem**: parse every element of a container; drop the ones
  that fail to parse.
- **Optics**: `Witherable[F]`-backed traversal — not in cats-eo
  0.1.0.
- **Source**: Penner — *Composable filters using Witherable
  optics*, <https://chrispenner.ca/posts/witherable-optics>.
- **Gap**: needs `cats.Alternative`-style filter semantics on
  the Traversal carrier; tracked under "follow-up families".

---

#### Theme I — Observable failure

##### 28. Partial-success Json walk — `Ior.Both`

Slot: already present (`circe.md`), keep.

- **Problem**: across a mixed-failure array, produce the updated
  JSON *and* the per-element miss list.
- **Optics**: `codecPrism[Basket].items.each.name.modify(f)` →
  `Ior[Chain[JsonFailure], Json]`.
- **cats-eo angle**: only cats-eo ships this today. Pair with
  the diagnostic-accumulation diagram (see Track 2).

##### 29. String parse as input — parse errors surface in the
same chain

Slot: already present (`circe.md`), keep.

- **Problem**: the input is a `String` not a `Json`; unparseable
  input should surface as `Ior.Left(Chain(ParseFailed(_)))`.
- **Optics**: the same `JsonPrism.modify`; the widened input
  type is `Json | String`.
- **cats-eo angle**: novel in cats-eo.

##### 30. Classify failures — *why* did the edit no-op?

Slot: **cookbook.md** — complementary to recipe 15.

- **Problem**: given the `Chain[JsonFailure]` returned by
  `.modify`, pattern-match on each `JsonFailure` case
  (`PathMissing` / `TypeMismatch` / `DecodeFailed` /
  `ParseFailed`) and route to distinct log streams.
- **Outline**:

      result.left.toList.foreach {
        case JsonFailure.PathMissing(step)    => metrics.miss(step)
        case JsonFailure.TypeMismatch(step)   => metrics.shape(step)
        case JsonFailure.DecodeFailed(_, df)  => log.warn(df.message)
        case JsonFailure.ParseFailed(_)       => log.warn("bad json input")
      }

- **Source**: cats-eo — novel. Structured-failure inspiration
  from cats' `Ior` typeclass and `DecodingFailure.history`.
- **Why**: the Ior surface isn't a curiosity — it's
  production-grade observability. One vignette to show how.

---

### Recipe count, by theme (0.1.0 cookbook footprint)

| Theme                              | Count | In-cookbook v1 |
|------------------------------------|------:|---------------:|
| A. Product editing                 |   4   |   4 (1 new to cookbook; 3 strengthen existing pages) |
| B. Sum-branch                      |   3   |   3 |
| C. Collection walks                |   5   |   4 (skip 12 — defer to standalone example per the existing plan) |
| D. JSON / tree                     |   5   |   5 |
| E. Algebraic / classifier          |   2+1 |   2 (19 noted as deferred) |
| F. Write / read escape             |   3   |   3 |
| G. Composition laddering           |   2   |   2 |
| H. Effectful read                  |   3   |   2 (27 deferred) |
| I. Observable failure              |   3   |   3 |
| **Total**                          | **30** | **28 in-cookbook + 2 deferred cross-refs** |

Target was 15–25. The catalogue above is 30 — on the basis that
this is a research artifact, the ship list can prune by
merging adjacent recipes (e.g. 8 + 9 into one "traversal
choice" section; 13 + 14 + 15 into one JSON arc; 11 + 12 + 18
into a single AlgLens vignette). A realistic landing
target is **~18–22 recipes** for the 0.1.0 cookbook page.

### Recipes by cats-eo-unique angle

These are the recipes where a straight Haskell / Monocle
translation would be less instructive than the cats-eo rendering.
Prioritise in the cookbook's first scroll:

1. **3** — multi-field NamedTuple focus (no Monocle equivalent)
2. **4** — full-cover Iso upgrade (varargs tell, no equivalent)
3. **10** — sparse traversal + Prism, benched against naive
4. **13** / **14** — JsonPrism / JsonTraversal over decode/re-encode
5. **15** / **28** / **29** / **30** — Ior-surface diagnostics
6. **23** — three-family composition ladder (the cross-carrier demo)
7. **11** / **18** — AlgLens classifier / column-aware update
8. **26** — batch-load via traversal (Penner's 100×)

### Gaps flagged for the 0.1.0 backlog

- **Recipe 11 / 18** — `AlgLens.modifyWithAggregate`-style
  convenience surface. Today users reach into `.modify` with a
  hand-rolled closure.
- **Recipe 16** — predicate-gated traversal (`filtered` /
  `selected` on `Traversal`). Today users inline the predicate
  inside the `modify` lambda.
- **Recipe 27** — `Witherable` / filter-and-drop traversal
  semantics. Deferred to follow-up families.
- **Indexed variants** — any recipe that wants `iover` /
  `imodify` etc. These are explicitly out of scope for 0.1.0
  per the families survey; the cookbook should not introduce
  them at all.

---

## Track 2 — Site diagrams and graphs

### Rendering tech survey

Laika's Helium theme (the default site theme for cats-eo, as used
in `site/`) **ships Mermaid support natively** in HTML output:
fenced code blocks with the language tag `mermaid` are rendered
by a bundled JS runtime. Constraints worth noting:

- **HTML only.** Mermaid does not render in EPUB or PDF output
  (`typelevel/Laika/docs/src/03-preparing-content/03-theme-settings.md`,
  <https://github.com/typelevel/Laika/blob/main/docs/src/03-preparing-content/03-theme-settings.md>).
  cats-eo's site is HTML-only, so this is fine.
- **Theme-coloured.** Mermaid diagrams inherit Helium's colour
  palette; no custom styling beyond that.
- **No PlantUML / Graphviz / D2 support out of the box.** For
  non-Mermaid diagrams the options are: raw SVG (author by hand
  or export from a tool like Excalidraw / Mermaid Live / TLDraw
  and commit the SVG), or a rendered PNG/SVG asset. Mermaid is
  strongly preferred when the shape fits its DSL.

### Diagram candidates, with keep/drop calls

#### D1. Composition lattice — carriers as nodes, Composers as edges

- **What**: directed graph where each node is a carrier
  (`Tuple2`, `Either`, `Forgetful`, `Affine`, `SetterF`,
  `Forget[F]`, `PowerSeries`, `Grate`, `AlgLens[F]`); each edge
  is a `Composer[F, G]` given; edge labels indicate direction
  and the typeclass (`Composer` or transitive `Composer.chain`).
- **Data source**: the `given Composer[_, _]` set shipped in
  `core/src/main/scala/eo/data/*`. The `concepts.md` page
  already lists them in prose.
- **Rendering**: Mermaid `graph LR` or `flowchart LR`.
- **Placement**: **`concepts.md`**, immediately after "Cross-
  family composition".
- **Effort**: **S** — 10 nodes, maybe 12 edges, one
  Mermaid block.
- **Recommend**: **keep**. This is the one diagram that collapses
  two pages of prose into one look.

#### D2. Carrier × capability matrix

- **What**: rows = carriers, columns = capability typeclasses
  (`Accessor`, `ReverseAccessor`, `ForgetfulFunctor`,
  `ForgetfulApplicative`, `ForgetfulTraverse`, `ForgetfulFold`,
  `AssociativeFunctor`), cells = ✓ / ✗.
- **Data source**: already exists as a Markdown table in
  `concepts.md`.
- **Rendering**: the existing Markdown table is the right
  rendering. A graphical version would have strictly less
  information density than the table.
- **Placement**: already in place.
- **Effort**: **S** — no new work, keep as-is.
- **Recommend**: **drop** the graphical version. The existing
  Markdown table is already optimal for this data shape.

#### D3. Optic family taxonomy tree

- **What**: tree rooted at `Optic[S, T, A, B, F]`, branching by
  carrier, leaves at concrete families (Lens, Prism, Iso,
  Optional, AffineFold, Setter, Getter, Fold, Traversal, Grate,
  AlgLens).
- **Data source**: the carrier column of the existing `concepts.md`
  table + the `optic-families-survey.md` catalogue.
- **Rendering**: Mermaid `graph TD` or `flowchart TD`.
- **Placement**: **`optics.md`** top-of-page, as an
  at-a-glance index; clicking nodes via Laika's fragment links
  jumps to the family section.
- **Effort**: **S** — single Mermaid block.
- **Recommend**: **keep**. Orients first-time readers in 5
  seconds. The intra-page link affordance is the bonus.

#### D4. JsonPrism path-walk sequence diagram

- **What**: a sequence diagram showing the `modifyImpl` walk:
  root `Json` → `JsonObject.apply(field)` → recurse into child
  → decode leaf `A` → apply `f` → encode → `JsonObject.add` on
  way up → root `Json`.
- **Data source**: the inline walker in
  `circe/src/main/scala/eo/circe/JsonPrism.scala` (per
  `extensibility.md`).
- **Rendering**: Mermaid `sequenceDiagram`.
- **Placement**: **`circe.md`** or **`extensibility.md`**
  (the latter already describes the walker conceptually).
- **Effort**: **M** — sequence diagrams are easy but getting
  the abstraction level right (not too much implementation
  detail, not too little) takes a draft-and-refine pass.
- **Recommend**: **keep**. The mental model "path steps go down,
  rebuild goes up" is load-bearing for the perf story, and the
  diagram beats prose.

#### D5. JsonPrism Ior failure-flow decision graph

- **What**: flowchart from input (`Json | String`) to output
  (`Ior.Right` / `Ior.Both` / `Ior.Left`), with decision
  branches for each `JsonFailure` case (`ParseFailed`,
  `PathMissing`, `TypeMismatch`, `DecodeFailed`) + the
  collect-on-traversal-skips branch.
- **Data source**: the observable-by-default surface code in
  `circe/src/main/scala/eo/circe/JsonPrism.scala` and the
  `JsonFailure` ADT.
- **Rendering**: Mermaid `flowchart TD`.
- **Placement**: **`circe.md`**, adjacent to the "Reading
  diagnostics" section.
- **Effort**: **M** — the decision graph is straightforward
  but needs to stay legible at three or four decision points.
- **Recommend**: **keep**. This is the one diagram that would
  materially help users who hit their first `Ior.Both` and
  don't know what the `Chain` content means.

#### D6. Existential vs. profunctor encoding, side-by-side

- **What**: two columns — left shows the profunctor presentation
  `[P[_,_]] => Profunctor[P] ?=> P[A, B] => P[S, T]`; right
  shows the existential presentation
  `(S => F[X, A], F[X, B] => T)`. Arrows from each side to the
  corresponding concepts (universally-quantified polymorphism
  vs. carrier + existential witness).
- **Data source**: `concepts.md` prose.
- **Rendering**: could be done as inline Markdown in a two-
  column table; a proper diagram would want side-by-side boxes
  with labelled arrows, which Mermaid `flowchart LR` can do but
  not beautifully.
- **Placement**: **`concepts.md`**, at the "Existential vs.
  profunctor" section.
- **Effort**: **M** (Mermaid) / **L** (hand-authored SVG for
  quality).
- **Recommend**: **drop for 0.1.0**. Pedagogical value is real
  but the audience who cares is small (readers who already
  know the profunctor encoding are a minority of cats-eo
  users), and the prose version is already workable. Revisit
  for a "deep-dive" page in a later release.

#### D7. PowerSeries chunking diagram

- **What**: visual of how `PSSingleton` / `PSSingletonAlwaysHit`
  skip per-element `PowerSeries` wrapper allocations. Boxes
  showing: (a) the naive path — one `PowerSeries` +
  `PSVec.Single` per element; (b) the fast path — one flat
  `Array[AnyRef]` with `IntArrBuilder` tracking lengths; (c)
  AlwaysHit — no length array at all.
- **Data source**: `core/src/main/scala/eo/data/PSVec.scala`
  and the `PSSingleton` protocol code; `benchmarks.md` prose.
- **Rendering**: Mermaid `flowchart LR` with grouped subgraphs
  per variant.
- **Placement**: **`benchmarks.md`** near the PowerSeries
  section, or **`extensibility.md`**.
- **Effort**: **M–L**. The diagram has to carry real
  information (array shapes, element counts) or it becomes
  decorative.
- **Recommend**: **consider shipping a simplified version**.
  Users staring at the 1.9×–5× ratios benefit from one glance
  of "why is this fast". A two-box "naive vs. flat-array"
  comparison gets 80% of the value for 30% of the effort.

#### D8. Concrete-subclass fused `.andThen` diagram

- **What**: arrow from `GetReplaceLens.andThen(GetReplaceLens)`
  through two paths: (a) generic path — carrier +
  `AssociativeFunctor[Tuple2]`; (b) fused path — direct
  `get`/`replace` composition skipping the carrier.
- **Data source**: the `GetReplaceLens.andThen` override.
- **Rendering**: Mermaid `flowchart LR`, or an inline
  before/after pseudo-code block.
- **Placement**: **`extensibility.md`** (axis 2 — "Shadow
  generic extensions") or **`benchmarks.md`**.
- **Effort**: **S–M**.
- **Recommend**: **keep as inline pseudo-code rather than
  a diagram**. The two paths are two lines of code each;
  Mermaid doesn't improve over a side-by-side Scala block.

#### D9. Cross-carrier composition walkthrough

- **What**: one concrete chain (recipe 23:
  `Iso → Lens → Traversal.each → Prism → Lens`) rendered as a
  linear flowchart, with each edge annotated with the `Morph`
  direction and the `Composer[F, G]` the morph picks up.
- **Data source**: the `given` set in `core/src/main/scala/eo/data/`
  (same as D1), plus the `Morph` instances (`same`,
  `leftToRight`, `rightToLeft`, `bothViaAffine`).
- **Rendering**: Mermaid `flowchart LR`.
- **Placement**: **`concepts.md`** (after D1) or
  **`optics.md`** at the top of the Traversal section.
- **Effort**: **M**. This is the diagram that forces
  clarity about the `Morph` semantics; drafting may reveal
  sharp edges in the `Morph` API itself.
- **Recommend**: **keep**. Complements D1 (D1 = whole graph;
  D9 = one traversal through it). Together they satisfy
  both "show me the big picture" and "show me one concrete
  example".

### Shortlist — what to ship for 0.1.0 if forced to pick three

Ranked by (pedagogical payoff) / (author effort):

1. **D1 — Composition lattice** (effort S, payoff high).
   Collapses two pages of prose. Single Mermaid block. The
   single clearest "what does cross-carrier composition mean"
   artifact.
2. **D5 — JsonPrism Ior failure-flow** (effort M, payoff
   high). The Ior surface is cats-eo's most novel shipping
   feature; the diagram removes the "what does `Ior.Both`
   even mean" stumble for every new user.
3. **D3 — Optic family taxonomy tree** (effort S, payoff
   medium-high). Orients readers in five seconds and doubles
   as an intra-page nav affordance in `optics.md`.

Runners-up if time permits:

- **D4 — JsonPrism path-walk sequence diagram**. Complements
  the perf story in `benchmarks.md`.
- **D9 — Cross-carrier composition walkthrough**. One concrete
  ladder through the D1 graph.
- **D7 (simplified) — PowerSeries chunking two-box compare**.
  Rounds out `benchmarks.md`.

Explicitly dropped for 0.1.0: **D2** (existing table is
already optimal), **D6** (audience too narrow for the effort),
**D8** (better as inline code).

### Implementation notes for the three-shortlist

- **D1** — estimate ~40 lines of Mermaid; keep edge labels
  short (`Composer`, `chain`, `morph-via-Affine`); colour the
  "identity carrier" (`Forgetful`) distinctly from the
  composition carriers.
- **D5** — start from the `JsonFailure` ADT, one terminal
  `Ior.*` per case. Verify the diagram against the behaviour
  spec
  <https://github.com/Constructive-Programming/eo/blob/main/circe/src/test/scala/eo/circe/JsonPrismSpec.scala>
  so the cases match the code.
- **D3** — group leaves by carrier (`Tuple2` → Lens, `Either`
  → Prism, `Forgetful` → Iso / Getter, `Affine` → Optional /
  AffineFold, `Forget[F]` → Fold / Traversal.forEach, `SetterF`
  → Setter, `PowerSeries` → Traversal.each, `Grate` → Grate,
  `AlgLens[F]` → AlgLens). Leaf labels link to the in-page
  anchors already in `optics.md`.

---

## Appendix — dead / flaky links encountered

These fetches failed during research; none were load-bearing
enough to block this report.

- `https://www.baeldung.com/scala/monocle-optics` — 403 on
  direct fetch. Content summarised from the search-result
  snippet and RockTheJVM's equivalent article.
- `https://scalac.io/blog/scala-optics-lenses-with-monocle/` —
  403 on direct fetch. Content corroborated via search
  summary only.
- `https://scalac.io/wp-content/uploads/2021/11/ZIO_OPTICS-_Ebook_Scalac.pdf`
  — 403 on direct fetch. Cited by title.
- `https://www.scala-exercises.org/monocle/lens`,
  `https://www.scala-exercises.org/circe/Optics` — redirect to
  a broken home page. Content present in search summaries only.
- `https://typelevel.org/Laika/latest/01-about-laika/01-features.html`
  — 403 on direct fetch. Cross-checked via the GitHub source.
- `https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html`
  — 403 on direct fetch. Load-bearing for the "classy optics
  in Scala" recipe; title pinned from search; prose
  reconstructed from surrounding references (classy-optics
  library docs at <https://index.scala-lang.org/pismute/classy-optics>).

No dead link was load-bearing enough that the recipe or
diagram it supports had to be dropped. Every recipe's source
citation includes at least one link that resolved during
research.

## Appendix — sources that surprised with a cats-eo-unique angle

- **Penner — *Using traversals to batch database queries***.
  The 100×–300× result on Unison Share is a textbook
  demonstration of what cats-eo's `Traversal.modifyA` +
  `foldMap` double pass already enables. Stronger Scala-
  practitioner pitch than any Haskell `partsOf` demo.
- **Penner — *Algebraic lenses***. The classifier framing
  ("optic that holds a learning algorithm inside") is
  unusually good at motivating AlgLens. Most optics posts
  motivate by "edit deeply-nested field"; this one motivates
  by "encode a decision procedure as an optic" — which
  lands harder for Scala engineers who already have the
  record-update pattern from Monocle.
- **Penner — *Generalizing 'jq' and Traversal Systems***.
  The jq→optics mapping is a strong on-ramp for Scala users
  who have reached for jq on the command line; much better
  framing than "here's a traversal with a filter".
- **circe-optics' `root.*`**. The decode-per-level tax it
  pays is invisible in the tutorial prose — you only notice
  it once you profile. cats-eo's `codecPrism[S].field.sub.leaf`
  sugar + the cursor walk is a drop-in ergonomic + perf win,
  but the pitch only lands if the cookbook shows the
  measured delta side-by-side (recipes 13–15 + the benchmark
  table).
