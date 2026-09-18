# Quality Assurance

`cats-eo` is mostly a *type-level* library: a large part of its correctness is
discharged by the compiler before any test runs. The quality signals it tracks
reflect that, in roughly increasing cost-to-fool order:

1. **Types as tests** — the [composition matrix](#composition-matrix) pins, at
   compile time, exactly which optic families compose with which (and at what
   strength), and which combinations are deliberately rejected. A regression
   that loosened or broke the lattice fails to compile.
2. **Discipline law suites** — `cats-eo-laws` defines the optic and typeclass
   laws; `cats-eo-tests` and the integration modules run them against concrete
   instances.
3. **Statement / branch [coverage](#coverage)** (scoverage) — the project's
   primary runtime-quality signal. See the
   [`CLAUDE.md` coverage note](https://github.com/Constructive-Programming/eo/blob/main/CLAUDE.md)
   for why ~70–80 % is the expected ceiling: the remainder is pure type-level
   machinery with no runtime footprint, or code reachable only once a
   downstream carrier instance is added.
4. **[Mutation testing](#mutation-testing)** (stryker4s) — the strongest and
   most expensive signal, and the one that historically did *not* pay its way
   here. It was reintroduced once the `schemes` module grew real runtime
   machinery (the `ArrayDeque` fold machine, the effectful M-drivers, `PSVec`)
   and the opaque-carrier compose work added runtime dispatch to `core`.

The numbers below are regenerated from the live reports by
[`site/tools/gen-qa-report.py`](https://github.com/Constructive-Programming/eo/blob/main/site/tools/gen-qa-report.py);
see [Regenerating these numbers](#regenerating-these-numbers).

## Composition matrix

Every `(outer family ∘ inner family)` pair either composes — *import-free and
without a type ascription*, landing at the strength described in the
[optic taxonomy](optics.md) — or it does **not** compile, because the cell is
void by design (building through a non-invertible optic, writing through a
read-only one, reading through a write-only one). The grid is the pass/fail
projection of
[`CompositionMatrixSpec`](https://github.com/Constructive-Programming/eo/blob/main/tests/src/test/scala/dev/constructive/eo/CompositionMatrixSpec.scala),
which is the single source of truth — if a cell starts needing an import or an
ascription, that spec goes red.

<!-- BEGIN GENERATED: matrix -->

| outer ∘ inner | iso | lens | prism | optional | trav | getter | affold | fold | modify | review | unfold |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **iso** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **lens** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **prism** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **optional** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **trav** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| **getter** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **affold** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **fold** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| **modify** | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ |
| **review** | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| **unfold** | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |

*✓ composes import-free at the strength shown in the [optic taxonomy](optics.md); ✗ does not compile (void by design — building through a read-only optic, reading through a write-only one, etc.). 87 composing / 34 void cells, pinned by `CompositionMatrixSpec`.*

<!-- END GENERATED: matrix -->

## Coverage

Statement and branch coverage per package, from the cross-module scoverage
**aggregate** (`sbt coverageAll`). `BC/SC` is the branch-coverage ÷
statement-coverage ratio — a value well below 1 flags a package whose
conditionals are under-exercised relative to its straight-line code, even when
its statement coverage looks healthy. Packages with no branch statements show
`—`.

<!-- BEGIN GENERATED: coverage -->

| Package | Statements | Stmt&nbsp;% | Branches | Branch&nbsp;% | BC/SC |
|---|--:|--:|--:|--:|--:|
| `dev.constructive.eo` | 33/35 | 94.3% | — | — | — |
| `dev.constructive.eo.accessor` | 8/8 | 100.0% | — | — | — |
| `dev.constructive.eo.avro` | 2020/2392 | 84.4% | 431/556 | 77.5% | 0.92 |
| `dev.constructive.eo.avro.circe` | 205/213 | 96.2% | 32/34 | 94.1% | 0.98 |
| `dev.constructive.eo.avro.vulcan` | 12/14 | 85.7% | — | — | — |
| `dev.constructive.eo.circe` | 673/842 | 79.9% | 135/176 | 76.7% | 0.96 |
| `dev.constructive.eo.compose` | 89/117 | 76.1% | 7/8 | 87.5% | 1.15 |
| `dev.constructive.eo.data` | 1051/1182 | 88.9% | 185/205 | 90.2% | 1.01 |
| `dev.constructive.eo.forgetful` | 28/28 | 100.0% | — | — | — |
| `dev.constructive.eo.generics` | 410/494 | 83.0% | 70/87 | 80.5% | 0.97 |
| `dev.constructive.eo.jsoniter` | 662/677 | 97.8% | 192/199 | 96.5% | 0.99 |
| `dev.constructive.eo.laws` | 175/175 | 100.0% | 6/6 | 100.0% | 1.00 |
| `dev.constructive.eo.laws.data` | 29/29 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.data.discipline` | 21/21 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.discipline` | 208/208 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.discipline.internal` | 21/21 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.eo` | 86/86 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.eo.discipline` | 129/135 | 95.6% | — | — | — |
| `dev.constructive.eo.laws.typeclass` | 29/29 | 100.0% | — | — | — |
| `dev.constructive.eo.laws.typeclass.discipline` | 36/36 | 100.0% | — | — | — |
| `dev.constructive.eo.optics` | 503/567 | 88.7% | 55/59 | 93.2% | 1.05 |
| `dev.constructive.eo.schemes` | 211/211 | 100.0% | 47/47 | 100.0% | 1.00 |
| `dev.constructive.eo.schemes.laws` | 4/4 | 100.0% | — | — | — |
| `dev.constructive.eo.schemes.laws.discipline` | 6/6 | 100.0% | — | — | — |

<!-- END GENERATED: coverage -->

## Mutation testing

stryker4s mutates each module's `main` sources and re-runs that module's own
test suite per mutant. *Score (total)* counts no-coverage mutants against the
score; *Score (covered)* is restricted to mutants on exercised lines. `Timeout`
mutants count as detected (the mutant visibly broke the run). `Compile err`
mutants — mutations that don't type-check, common in this codebase's
match-type / opaque-carrier code — are excluded from both scores.

**StringLiteral mutants are excluded build-wide**
(`ThisBuild / strykerExcludedMutations`): in this codebase string literals are
error messages, vestigial-arm labels, and discipline rule-set names — nothing
any suite asserts on, so they survive as pure noise and drown the genuine
survivors (in `laws`, 99 of 101 unfiltered survivors were rule-set name
labels). They appear as *Ignored* in the HTML reports and are counted in no
score.

Caveats that keep the table honest:

- **`core`** — its behavioural suite lives in the separate `tests/` module
  (which depends on `laws` and `generics`, so core can't `dependsOn` it back —
  that's a project cycle). `mutationAll` instead *task-borrows* the compiled
  suite: it appends `tests/Test/definedTests` and `tests/Test/fullClasspath`
  to core's Test scope for the mutation run only. This is sound because
  stryker compiles every mutant into core's classes behind runtime switches
  (binary-compatible), so specs compiled against unmutated core still
  exercise the mutated bytecode. Core's row below is scored against the full
  cross-module suite.
- **`generics`** is macro code: it expands at *compile* time, so its mutants
  leave no runtime footprint for a test *run* to cover. Mutation testing
  structurally can't score it — the derived-optic laws in `generics/test`
  guard it instead.
- **`laws`** has no in-module tests, so it borrows the `tests/` suite the same
  way core does. Mutating the law *definitions* is the "who tests the tests"
  probe: a killed mutant means the discipline suites notice a corrupted law; a
  survivor pinpoints a law whose discriminating power nothing exercises. The
  surviving mutants are all *law-weakening* mutations (`missIsEmpty`'s guard →
  `false` in `AffineFoldLaws`, `&&` → `||` in `ModifyFLaws.functorIdentity` /
  `functorComposition`): every instance under test satisfies the weakened law
  too, so nothing fails. Killing those requires **negative fixtures** —
  deliberately unlawful instances pinned to fail the suite — which is the
  concrete follow-up this table surfaces.

Two "can't be scored" caveats that used to sit here are **retired** — both were
re-tested on 2026-09-18 and neither reproduces:

- **`jsoniter`** was recorded as un-mutatable because instrumenting
  `PathParser.parseField` supposedly overflowed the JVM's 64 KB per-method
  bytecode limit. It does not: the module mutates and runs clean end to end
  (420 mutants, **0 compile errors**), and `PathParser.scala` itself yields 24
  mutants. Nothing needs splitting or excluding.
- **`avro`** was recorded as unscoreable because stryker's forked test-runner
  failed to initialise in the sandbox. It now completes in about two minutes
  and produces a full report. The row's numbers are real.

What *is* structural and permanent — do not spend a test-writing budget on it:

- **`generics`** — 86 `NoCoverage` mutants, every one inside a quoted macro
  (`LensMacro`, `MacroSelectors`, `PlateMacro`, `PrismMacro`). Macro bodies run
  inside the *compiler*, so stryker's runtime coverage probe never observes
  them. The only tests that can reach them are `must not compile` negative
  specs, which register no coverage. The module's score is 0 % by construction.
- **`avro`'s `AvroPrismMacro`** — 17 `NoCoverage` mutants, identical cause.
- **`kyo`'s `RecordIsoMacro`** — 28 `NoCoverage` mutants, identical cause; it
  is the entire reason kyo's *total* score reads far below its *covered* score.

The high-signal rows are `core` and `laws` (via the borrowed suite), `schemes`,
`circe`, `jsoniter` and `avro` — modules whose mutated code is genuinely
exercised at run time by the suite stryker runs.

<!-- BEGIN GENERATED: mutation -->

| Module | Killed | Timeout | Survived | No&nbsp;cov | Compile&nbsp;err | Score (total) | Score (covered) | Notes |
|---|--:|--:|--:|--:|--:|--:|--:|---|
| `core` | 185 | 5 | 21 | 0 | 3 | 90.0% | 90.0% | Scored against the cross-module suite in `tests/`, task-borrowed into core's Test scope by `mutationAll`. |
| `laws` | 85 | 0 | 0 | 0 | 0 | 100.0% | 100.0% | Borrowed `tests/` suite; the negative fixtures in `UnlawfulFixturesSpec` keep the law-weakening mutants dead — see prose. |
| `generics` | 0 | 0 | 0 | 58 | 0 | 0.0% | — | Macro code: it expands at compile time, so mutants leave no runtime footprint for the test run to cover. |
| `schemes` | 70 | 0 | 12 | 0 | 5 | 85.4% | 85.4% |  |
| `schemes-laws` | 1 | 0 | 0 | 0 | 0 | 100.0% | 100.0% | Recursion-scheme laws (hylo fusion so far; more expected). Like `laws`, mutating it probes whether the law spec notices a corrupted law. |
| `circe` | 35 | 0 | 3 | 15 | 0 | 66.0% | 92.1% |  |
| `avro` | 283 | 0 | 57 | 34 | 6 | 75.7% | 83.2% | Scores fine (~2 min): the old "forked test-runner fails to initialise" caveat no longer reproduces. Its no-coverage mutants are `AvroPrismMacro` quoted-macro bodies — compile-time only, like `generics`. |
| `jsoniter` | 321 | 4 | 77 | 0 | 0 | 80.8% | 80.8% | Mutates clean end to end (0 compile errors): the old `PathParser.parseField` 64 KB method-size caveat no longer reproduces. |

<!-- END GENERATED: mutation -->

The table is regenerated only on release tags, so between releases it lags the
tree. The most recent full sweep (**2026-09-18**, JDK 25, `project <m>; stryker`
per module), after the survivor-killing pass of the same day, measured:
`core` 193 K / 4 T / 19 S, `laws` 85 K / 0 S, `schemes` 48 K / 8 S,
`circe` 44 K / 6 S, `jsoniter` 328 K / 4 T / 58 S, `avro` 391 K / 78 S,
`generics` 0 K / 86 NC.

Two modules in the `mutationAll` alias have never had a row here:

- **`zio`** — **0 mutants exist**. The module is pure optic construction: no
  conditional, no arithmetic, no boolean literal for stryker to mutate. Its
  score is `n/a`, not 0 % — there is no pool.
- **`kyo`** — scores 83.3 % *covered* (20/24 on `schema/StructureOptics.scala`;
  everything else is `RecordIsoMacro`, compile-time only), but only once the
  single-method `extension` block in that file is **braced**: re-printed by
  stryker4s, a significant-indentation `extension` clause loses its method to
  column 0 and the whole file stops compiling, aborting the module.


### Known equivalent mutants

A mutation score is not a coverage target: some mutants are **equivalent** —
the mutated program computes the same observable value as the original, so *no*
test can kill them. Chasing them is how a suite grows LOC without gaining
kill capacity. The 40 survivors below were each read against the source and
carry a checkable reason. **Re-triaging them is wasted work; if one of them is
ever killed, the equivalence claim was wrong and this table is the bug report.**

Keyed `(file, line:column, mutator → replacement)` so a key survives
renumbering only as far as the next edit to the file — re-verify a row whose
line has moved rather than trusting it.

#### `core` — 19 (all of core's survivors)

Every one is a perf fast path whose general path computes the same value.

| file | line:col | mutation | why it cannot be killed |
|---|---|---|---|
| `data/IntArrBuilder.scala` | 17:12 | `len == arr.length` → `!=` (`append`) | With `!=` the builder grows on every *non-full* append, so `len` never reaches `arr.length` and the would-be overflow is unreachable; `freeze` trims to `len`, so the returned array is identical — only extra allocation. |
| `data/IntArrBuilder.scala` | 28:10 | `cap < minCap` → `false` (`doubleTo`) | Returns `arr.length * 2` at once. `grow` is only ever called as `grow(len + 1)` with `len == arr.length ≥ 1`, and `2L ≥ L + 1` for every `L ≥ 1`, so capacity still suffices. |
| `data/IntArrBuilder.scala` | 28:14 | `<` → `<=` | Doubles one step further than needed; capacity still `≥ minCap`. |
| `data/IntArrBuilder.scala` | 28:14 | `<` → `==` | The loop body runs only while `cap == minCap` (reachable only at `L = 1`), and every exit value is still `≥ minCap`. |
| `data/IntArrBuilder.scala` | 37:8 | `len == arr.length` → `false` (`freeze`) | Always trim-copies instead of aliasing; same elements, one extra array. |
| `data/ObjArrBuilder.scala` | 17:8 | guard → `true` (`append`) | Grows before every append; `grow(len + 1)` guarantees the slot. |
| `data/ObjArrBuilder.scala` | 34:12 | guard → `true` (`appendAllFromPSVec`) | Grows unconditionally; capacity is `≥ len + n` either way. |
| `data/ObjArrBuilder.scala` | 34:20 | `>` → `>=` | Grows one element early. |
| `data/ObjArrBuilder.scala` | 40:14 | `<` → `<=` (`doubleTo`) | As `IntArrBuilder` 28:14. |
| `data/ObjArrBuilder.scala` | 56:8 | `len == arr.length` → `false` (`freezeArr`) | Always trim-copies. |
| `data/PSVec.scala` | 75:16 | `i >= length` → `==` (`equals` loop) | `i` starts at 0 and advances by exactly 1, so `>=` first holds precisely where `==` does. |
| `data/PSVec.scala` | 114:10 | `n == 0` → `false` (`Functor.map`) | The general path allocates a 0-length array and `PSVec.unsafeWrap` normalises length 0 back to `Empty`. |
| `data/PSVec.scala` | 143:14 | `i >= n` → `==` (`foldRight` loop) | `i` advances by one. |
| `data/PSVec.scala` | 160:10 | `n == 0` → `false` (`Traverse.traverse`) | `loop` exits immediately and `G.pure(new Array(0))` wraps back to `Empty`. |
| `optics/Plated.scala` | 127:15 | `depth >= transformRecursionLimit` → `true` | Routes every internal node to `transformMachine`, which computes the same value — only the on-stack → heap switch point moves. |
| `optics/Plated.scala` | 127:21 | `>=` → `<` | Same: everything goes to the heap machine. |
| `optics/Plated.scala` | 127:21 | `>=` → `>` | Switches one level deeper. |
| `optics/Plated.scala` | 127:21 | `>=` → `==` | `depth` advances by one, so `==` fires at the same node `>=` would. |
| `data/MultiFocus.scala` | 430:39 | `math.max(n, 16)` → `math.min` | A capacity *floor*: `ObjArrBuilder` grows on demand and floors its own capacity at 1. Deliberate perf tuning, invisible to any value assertion. |

A deep-`transform` test is still worth having as a **stack-safety** test — but
it will not kill the four `Plated:127` mutants, so it must be justified on its
own terms.

#### `circe` — 6 (all of circe's survivors)

| file | line:col | mutation | why it cannot be killed |
|---|---|---|---|
| `JsonFocus.scala` | 77:10 | `path.length == 0` → `false` (`navigateForWrite`) | Falls through to `readPath(json, [])`, which returns `Right(json)` at `i = 0`; the deferred writer then evaluates to `encoder(b)`, exactly the shortcut's value. |
| `JsonFocus.scala` | 100:10 | `path.length == 0` → `false` (`modifyImpl`) | `modifyPath(json, [])(f)` applies `f` at `i = 0`; a decode failure still aborts via `miss` and `getOrElse(json)` returns the input, as the shortcut does. |
| `JsonFocus.scala` | 114:10 | `path.length == 0` → `false` (`placeImpl`) | `modifyPath(json, [])(_ => encoder(a))` is `encoder(a)`. |
| `JsonFocus.scala` | 135:10 | `path.length == 0` → `false` (`placeIor`) | Same, wrapped as `Ior.Right`. |
| `JsonWalk.scala` | 42:12 | `i >= path.length` → `==` (`readPath` loop) | `i` starts at 0 and the only recursive call is `i + 1` from inside the `i < path.length` arm. |
| `JsonWalk.scala` | 67:12 | `i >= path.length` → `==` (`modifyPath` loop) | Same. |

The *other* copy of the array-bounds check in this file (`JsonWalk.scala:56`,
`readPath`) is **not** equivalent — it survived for years because
`JsonIndexBoundsSpec` drove only the write twin, and a differential oracle
killed all six of its variants in 2026-09.

#### `schemes` — 8 (all of schemes' survivors)

| file | line:col | mutation | why it cannot be killed |
|---|---|---|---|
| `Schemes.scala` | 70:14 | `depth >= OnStackLimit` → `==` (`unfoldCoalgRec`) | `depth` advances by one, so the switch happens at the same node. |
| `Schemes.scala` | 70:14 | `depth >= OnStackLimit` → `>` | Switch-point shift by one frame; `unfoldCoalgHeap` computes the same value (the `<` variant *is* killed, which is the evidence that the two engines agree). |
| `Schemes.scala` | 74:10 | `k == 0` → `false` | Leaf fast path: the general path builds a 0-length array and `combine(PSVec.unsafeWrap(empty)) == combine(PSVec.empty)`. |
| `Schemes.scala` | 99:10 | `kids.isEmpty` → `false` (`unfoldCoalgHeap.enter`) | Pushes a frame with a 0-length `out`; the driver immediately takes the `else` arm and calls the same `combine(Empty)`. |
| `Schemes.scala` | 137:14 | `depth >= OnStackLimit` → `==` (`foldInPlaceRec`) | As 70:14. |
| `Schemes.scala` | 137:14 | `depth >= OnStackLimit` → `>` | As 70:14. |
| `Schemes.scala` | 141:10 | `k == 0` → `false` | As 74:10, with `alg` in place of `combine`. |
| `Schemes.scala` | 166:10 | `arr.length == 0` → `false` (`foldInPlaceHeap.enter`) | As 99:10. |

#### `jsoniter` — 7 of 58

| file | line:col | mutation | why it cannot be killed |
|---|---|---|---|
| `JsoniterTraversal.scala` | 56:8 | `spans.isEmpty` → `false` (`to`) | `decodeSpans` on an empty span list returns `(Nil, PSVec.unsafeWrap(new Array(0)))` = `(Nil, Empty)` — the same `MultiFocus`. |
| `JsoniterTraversal.scala` | 170:10 | `written == n` → `false` | Falls to the tight-copy branch, which copies `written == n` elements: same contents. |
| `JsoniterTraversal.scala` | 171:15 | `written == 0` → `false` | Tight-copies a 0-length array; `unsafeWrap` normalises it to `Empty`. |
| `PathParser.scala` | 37:12 | `pos >= s.length` → `==` (`parseSteps`) | Every call site passes `pos ≤ s.length` (`1` on a non-empty input, `end` from a scanner that stops at `s.length`, `pos + 2` under `pos + 1 < s.length`, `end + 1` under `s.charAt(end) == ']'`), so `>=` and `==` coincide. |
| `PathParser.scala` | 49:12 | `pos >= s.length` → `==` (`parseField`) | Called only as `parseField(s, pos + 1, …)` under `pos < s.length`, so `pos ≤ s.length`; the `charAt` in the right disjunct is still guarded at the only dangerous value. |
| `PathParser.scala` | 65:18 | `pos + 1 >= s.length` → `==` (`parseIndex`) | Guarded by `pos < s.length`, so `pos + 1 ≤ s.length`. |
| `PathParser.scala` | 74:19 | `end >= s.length` → `==` (`parseIndex`) | `scanEnd` stops at `s.length`, so `end ≤ s.length`. |

**Not certified here:** the remaining 51 `JsonPathScanner.scala` survivors and
`JsoniterPrism.scala:130:8`. They were triaged into classes (end-of-input
`>=` → `==` on cursors that advance by one; empty-container fast paths), but
one class was measured *wrong* in 2026-09: `254:13 >=` → `>` was called
equivalent and is in fact killable, because with `>` the left disjunct of
`kpos >= bytes.length || bytes(kpos) != '"'` is dead-false and the right
disjunct then indexes at `kpos == bytes.length`. **Any guard whose right-hand
side indexes the array needs the reachability of `pos == length` checked
individually** before it may be called equivalent — which is why those rows are
absent from the table above.

#### Timeout oscillation

A handful of the builder mutants (`IntArrBuilder:17:8`, `ObjArrBuilder:17:12`,
`IntArrBuilder:28:10 → true`, `ObjArrBuilder:40:10 → true`) alternate between
`Timeout` and `Survived` between runs: forcing a grow on every append makes the
builder quadratic, which sometimes trips the per-mutant timeout and sometimes
does not. Both statuses are equivalent-mutant outcomes, but only `Timeout`
counts as *detected*, so a module's survivor count can move by one or two with
no change to the suite. Do not read such a delta as a regression.


## Regenerating these numbers

```sh
# Statement / branch coverage (cross-module aggregate):
SBT_OPTS="-Xmx6g" sbt coverageAll
#   → target/scala-3.8.3/scoverage-report/  (HTML + scoverage.xml)

# Mutation testing across the runtime-logic modules:
SBT_OPTS="-Xmx6g" sbt mutationAll
#   → <module>/target/stryker4s-report/<ts>/  (index.html + report.json)

# Rewrite the tables above from those reports + CompositionMatrixSpec:
python3 site/tools/gen-qa-report.py            # in place
python3 site/tools/gen-qa-report.py --check     # CI: non-zero if stale
```

Both aliases relax the always-on `-Werror` (`tlFatalWarnings`) first, since
instrumented sources can surface `-Wunused` warnings; the larger heap is
because the `set` reapply re-evaluates the Laika docs settings. Mutation runs
with `project <m>; stryker` (not `<m>/stryker`) so specs2 is visible to the
test runner — see
[`project/plugins.sbt`](https://github.com/Constructive-Programming/eo/blob/main/project/plugins.sbt).

The [`quality.yml`](https://github.com/Constructive-Programming/eo/blob/main/.github/workflows/quality.yml)
workflow runs all three on every release tag (and on demand via
`workflow_dispatch`), uploading the HTML reports and the regenerated tables as
build artifacts.
