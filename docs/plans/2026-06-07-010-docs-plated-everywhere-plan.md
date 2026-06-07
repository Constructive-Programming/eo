# Docs plan — surface `Plated` + `everywhere` across the site

**Date:** 2026-06-07
**Scope:** documentation site (`site/docs/*.md`) **plus one core change** —
`core/.../optics/Plated.scala` `rewrite` reworked to share `transform`'s
stack-safe machinery.
**Goal:** make the recursive-optics story (the `Plated` typeclass and the
`everywhere` composable Setter) discoverable from the places a reader actually
looks, and lean on it as an adoption motivator without bloating the 60-second
pitch. Along the way: give `rewrite` the same stack-safety `transform` has at a
fraction of the per-node cost, and refresh the benchmark figures from the latest
CI run.

## Background — current state

`Plated` is already documented in five files:

| File | What it has today | Lines |
|------|-------------------|-------|
| `cookbook.md` | Full worked recipe: `Plated.fromChildren` + `everywhere.andThen(varName)` over `Expr` | 207–260 |
| `generics.md` | `plate[S]` macro derivation | 277–307 |
| `circe.md` | `Plated[Json]` + standalone `transform`/`universe` | 382–415 |
| `avro.md` | `Plated[IndexedRecord]` + prose on `transform`/`rewrite`/`children`/`universe` | 562–573 |
| `benchmarks.md` | `PlatedBench` vs Monocle vs hand visitor; stack-safety claims | 245–291 |

**The gap:** `everywhere` — the composable `Optic[S,S,S,S,SetterF]` that is the
actual "this is why optics" moment — appears **only** in the cookbook. The
circe / avro / generics sections show the standalone combinators
(`transform`, `universe`) but never the `everywhere[S].andThen(inner).modify(…)`
composition that reuses an ordinary optic and lifts it to every depth.

`optics.md`, `migration-from-monocle.md`, `index.md`, and `multifocus.md` don't
mention `Plated`/`everywhere` at all.

## Decisions (confirmed with the user)

1. **index.md:** *light nudge* — one sentence + one "Keep reading" link to the
   cookbook recipe. Do **not** expand the main "What optics buy you" pitch.
2. **optics.md:** *Setter note + taxonomy footnote* — `everywhere` is a worked
   example inside the existing Setter section (it literally is a Setter), plus a
   one-line footnote near PowerSeries pointing to generics + cookbook. **No new
   node in the family-taxonomy mermaid** — `Plated` is a typeclass over the
   PowerSeries (`MultiFocus[PSVec]`) carrier, not a new carrier.
3. **circe / avro:** *add runnable `everywhere`-composition demos* showing the
   same optic composing into recursion (the "redact/uppercase at any depth"
   example already teased in prose).

## Core change — `rewrite` stack-safety (investigated sharing; KEPT `Eval`)

`core/src/main/scala/dev/constructive/eo/optics/Plated.scala`.

The brief was to give `rewrite` `transform`'s stack-safety with less penalty by
sharing the machine, replicating only if sharing carried a penalty. We tried the
share, measured the penalty, and — per the user's call — **kept the `Eval`
trampoline**.

**The share we tried** (express `rewrite` via the stack-safe `transform`, the
Haskell `rewriteOf l f = transformOf l (\x -> maybe x go (f x))` shape, zero
replicated walk, no per-node `Eval`):

```scala
def rewrite[S](f)(s)(using P) = { def go(x) = transform(node => f(node).fold(node)(go))(x); go(s) }
```

**Why it was rejected — verified empirically, two independent axes:**

- **Tree descent** — fully stack-safe under the share (100k-deep spine fine, even
  firing at every node, since each fire re-processes a shallow result). ✓
- **Fixpoint re-firing — NOT stack-safe under the share.** When the rule fires,
  `go` re-runs on the JVM call stack (exactly as Haskell's `rewriteOf`, which is
  also unsafe here). A rule re-firing in a long chain at one position
  (`Counter(n) => Counter(n-1)` ×200k) **overflows** — even though it terminates.
  ✗ verified `StackOverflowError`. The old `Eval` version trampolines re-fires
  too, so it survives this.

**Decision (user):** revert to / keep the `Eval` implementation — fully stack-safe
on **both** axes (descent *and* re-fire). The per-node `Eval`/`flatMap` cost is
accepted in exchange for that safety. `rewrite` is **not** in `PlatedBench`, so
this costs nothing in the published numbers.

```scala
def rewrite[S](f: S => Option[S])(s: S)(using P: Plated[S]): S =
  def step(x: S): Eval[S] = f(x).fold(Eval.now(x))(go)
  def go(x: S): Eval[S] = Eval.defer(P.plate.modifyA[Eval](go)(x)).flatMap(step)
  go(s).value
```

The `rewrite` Scaladoc now states it is fully stack-safe on both axes and *why*
it keeps `Eval` (sharing `transform`'s synchronous machine would put the re-fire
chain back on the call stack). `PlatedSpec` gained a regression test exercising
**both** a 100k-deep descent and a 200k-long re-fire chain.

## Documentation correctness fix to fold in (applies wherever touched)

The hybrid-transform commit (`d7b0b2c`) made the per-combinator stack-safety
story uneven. Correct, final picture:

- `transform` (and therefore `everywhere.modify`) — call-stack recursion under
  depth 512, then an explicit **heap-stack machine**; *not* `Eval`.
- `universe` / `children` — explicit **worklist**; *not* `Eval`.
- `rewrite` — **trampolines through `cats.Eval`** (stack-safe on descent *and*
  long re-fire chains).

Fix the stale "everything trampolines through `cats.Eval`" wording at
**cookbook.md** and **circe.md** to this per-combinator picture (attribute `Eval`
only to `rewrite`).

## Per-file changes

### 1. `optics.md` — Setter section (≈311–358) + PowerSeries footnote (≈212–222)

- **Setter section:** after the `bumpAll` example, add a short paragraph +
  runnable snippet: `Plated.everywhere[Expr]` is a Setter whose `.modify` is the
  recursive `transform`, so `everywhere.andThen(prism).modify(f)` writes at every
  depth. Reuse the `Expr` shape and a hand `Plated.fromChildren` (matching the
  cookbook so it's familiar), or `import` the cookbook's; keep it to one
  `andThen` + one `.modify`. Cross-link to cookbook + generics.
  - This also strengthens the Setter narrative: Setter isn't only
    "write-at-one-path", it's the carrier a whole-tree rewrite lands on.
- **PowerSeries footnote (≈222):** one line — "`Plated` (recursive
  self-traversal) rides this same `MultiFocus[PSVec]` carrier via
  `Traversal.selfChildren`; see [Generics → `plate[S]`](generics.md) and the
  [Cookbook recipe](cookbook.md)."
- **Taxonomy prose (≈60–65):** optional half-sentence noting the recursion
  schemes are built on PowerSeries, not a new node. No mermaid edit.

### 2. `migration-from-monocle.md` — cheat-sheet row (≈8–32) + divergence note

- **Cheat-sheet row:**
  `monocle.function.Plated[A]` / `Plated.transform` / `.rewrite` / `.universe` /
  `.children` → cats-eo `plate[S]` (derive a `Plated[S]`) or `Plated.fromChildren`,
  same combinator names.
- **`Where EO diverges` subsection** — new short entry "Plated is stack-safe and
  composes as an optic": Monocle's `Plated` overflows on deep trees (cite the
  benchmark observation at `benchmarks.md:267`), and Monocle has no
  `everywhere`-as-an-optic — cats-eo's `everywhere[S]` composes with `.andThen`
  like any other Setter. Link to cookbook + benchmarks.

### 3. `circe.md` — `Recursive edits` section (382–415)

- Keep the existing `Plated[Json]` framing and the standalone `transform`
  snippet (it's a good first contact).
- **Add** an `everywhere` composition act: build a small `Prism[Json, String]`
  (string-leaf focus via `json.asString` / `Json.fromString`) and show
  `Plated.everywhere[Json].andThen(jsonString).modify(_.toUpperCase)(doc)` —
  same composition vocabulary as the rest of the page, now reaching every depth.
  - **Feasibility note:** there is no built-in "field named X at any depth"
    optic; the inner optic must be hand-built on raw `Json`. The string-leaf
    Prism is the cleanest guaranteed-to-compile shape. The "redact `ssn`"
    framing can stay prose, or use `Plated.transform` (object-aware) rather than
    a composed optic. **Will prototype-compile the inner Prism during
    implementation** before committing the snippet.
- **Fix** the `cats.Eval` wording (412–413) per the correctness section.

### 4. `avro.md` — `Recursive edits` section (562–573)

- Add a parallel `everywhere` mention. Avro's inner optic is harder (field
  access by name on `IndexedRecord`), so:
  - **Preferred:** a runnable `everywhere[IndexedRecord].andThen(<hand
    Lens/Prism>)` if a clean one-liner inner optic exists.
  - **Fallback (decide at implementation):** prose pointer — "`everywhere`
    composes the same way as for `Json`; see [circe](circe.md) and the
    [cookbook](cookbook.md)" — if the compiled inner optic is too heavy for the
    page. Note the existing "records nested in array/map/union are leftover
    skeleton" caveat still applies.

### 5. `generics.md` — `plate[S]` section (277–307)

- One sentence after the combinator list: the derived `Plated[S]` also backs
  `Plated.everywhere[S]`, the composable Setter — link to the cookbook recipe
  and to optics.md's Setter section. (Currently lists transform/rewrite/
  children/universe but omits `everywhere`.)

### 6. `index.md` — light nudge (per decision 1)

- Add one "Keep reading" bullet:
  "[Cookbook → Plated](cookbook.md) — one optic, every depth: recursive rewrites
  (`everywhere`) over ASTs, trees, and JSON."
- Optionally one clause in the existing JSON/wire paragraph (≈25) — "…the same
  vocabulary that recurses through an entire tree with `everywhere`." Keep it to
  a clause; do not expand the pitch.

### 7. `cookbook.md` — correctness fix only

- Fix the `cats.Eval` wording at 247–248 (the recipe content itself stays — it's
  the canonical worked example). Describe `everywhere`/`transform`/`rewrite` as
  stack-safe via the explicit machine/worklist, with **no** `Eval` attribution.

### 8. `benchmarks.md` — refresh Plated numbers (245–291)

Refresh the table and analytical claims from the latest canonical CI run
(`27098806867`, n=512, 3-fork). New `score(ns)` → rounded table:

| Op | Subject | eo | monocle | visitor |
|---|---|--:|--:|--:|
| `universe` | `Expr` balanced | 15 000 | 176 000 | 7 000 |
| | `Json` balanced | 20 000 | 210 000 | 16 000 |
| | `Bin` deep spine | 15 500 | **SO** | 7 000 |
| `transform` | `Expr` balanced | 18 000 | 16 000 | 8 000 |
| | `Bin` deep spine | 13 000 | **SO** | 4 000 |

Re-derived multipliers (update the three bullet claims accordingly):

- **`universe` beats Monocle by ~10–12×** (Expr 15k vs 176k ≈ 11.8×; Json 20k vs
  210k ≈ 10.4×) and **rivals the visitor on Json** (~1.3×), within ~2× on `Expr`.
  Claim holds.
- **`transform` is on par with Monocle** (Expr 18k vs 16k ≈ 1.1×). Claim holds.
- **The "within ~2× of the visitor" line must soften to ~2–3×**: transform
  `Expr` 18k vs 8k ≈ 2.2×, deep spine 13k vs 4k ≈ 3.2×; universe `Expr` 15k vs
  7k ≈ 2.2×. Reword the "Both paths sit within ~2×" sentence (line 282) and the
  closing "Closing the last ~2×" sentence (line 289) to "~2–3×".

**Allocation figures** (~80 B/node, ~56 B/node at lines 281–282) come from a
separate `-prof gc` run **not** in this score-only output — leave them unless a
fresh `-prof gc` run is taken. Note in the commit message that only the
score table was refreshed.

The `rewrite` decision does **not** affect these numbers (`rewrite` isn't in
`PlatedBench`, and it kept its existing `Eval` impl); the table is valid as-is.

## Out of scope / explicitly not doing

- No new mermaid node for Plated in optics.md (decision 2).
- No expansion of index.md's "What optics buy you" core pitch (decision 1).
- No jsoniter.md change — the byte/token model has no natural recursive `Plated`
  instance; leave as-is.
- No `-prof gc` re-run for allocation figures unless explicitly requested (the
  CI run refreshed scores only).

## Verification

- `sbt core/test` (and the full `sbt test`) — all `Plated` law + behaviour specs
  green, including the new `rewrite` regression test that exercises **both** a
  100k-deep descent and a 200k-long re-fire chain (the `Eval` impl clears both).
- `sbt docs/mdoc` must pass — every new ```scala mdoc``` block compiles. This is
  the load-bearing gate (the pre-commit hook runs it). Prototype the circe/avro
  inner optics first.
- `scalafmtCheckAll` (pre-commit) — now relevant: `Plated.scala` changes.
- Eyeball the rendered `everywhere` snippets, the migration table, and the
  refreshed benchmark table.

## Suggested commit slicing

1. `test(plated): guard rewrite stack-safety on descent + re-fire axes` — the new
   `PlatedSpec` regression test (Eval impl unchanged), plus the `rewrite`
   Scaladoc clarifying why it keeps `Eval`. (No behaviour change to ship; the
   share was investigated and rejected — see this plan.)
2. `docs: correct per-combinator cats.Eval / stack-safety wording` (cookbook +
   circe — attribute `Eval` only to `rewrite`).
3. `docs(benchmarks): refresh Plated figures from CI run 27098806867`.
4. `docs(optics,generics): surface everywhere as a composable Setter` (optics.md
   Setter note + footnote, generics one-liner).
5. `docs(circe,avro): add everywhere composition demos`.
6. `docs(migration,index): add Plated row + cookbook nudge`.
