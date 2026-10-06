# Grate constancy: index-free vs tabulating bundles — spike

> [!IMPORTANT]
> **SUPERSEDED** (2026-10-05). The sum-typed-bundle + `Optic.SameFocus` design described here
> does not land: a second review round showed the witness makes behaviour route-dependent (a
> non-inline generic helper or `dimap` wrapper loses the `C =:= D` evidence) and that the
> diagonal restore addresses positions with `==`, which is not a coordinate identity for `Double`
> (a write at `NaN` is silently dropped; signed zeros collide). All four root problems —
> diagonal read, erased evidence, observable grouping, index equality — trace to the carrier
> sharing one index type in the old kernel; the probes do not rule out every possible
> evidence/continuation encoding. The decision and
> the replacement design (product-indexed `Indexed`, full-grid composition) are in
> [`2026-10-05-multifocus-redesign.md`](./2026-10-05-multifocus-redesign.md). What survives from
> this spike is the variable-shape side: the `collectList` real-context fix and the
> cross-family runtime battery, which shipped without any of the grate machinery.

The surviving `collectList` correction preserves observed context and
supplies a singleton focus vector, not a guaranteed singleton source.
Shape/count-coupled composites can reject the cardinality mismatch.

Branch: `spike/grate-broadcast-sum`, a single commit on `origin/main` @
52671355 (the commit that landed the *optic-class* answer: `Z = Xo`,
`Function1BroadcastOptic`, `RepresentativeIndex`, and the positional
write). This branch adds the *sum-typed bundle* answer on top and keeps
main's machinery where it is better: the broadcast class stays as the
kernel's per-index write hook and the bridge keeps its
`RepresentativeIndex` witness, so **`unobserved` is gone entirely**: the
collapsing `List` aggregate hands the write the leftover its own read
produced, and nothing in the Grate surface forges an index or anything
else. Prototype + tests + measurements for the constancy question on
`MultiFocus[Function1[X0, *]]`.

Gates on the rebased tree (sbt 1.13.0, JDK 25 Temurin 25.0.4.1): root
`test` 237 examples / 0 failures; `core/test` 32 specs, including
**main's own F1 spec unchanged** (14 examples / 349 expectations — its
positional `grate ∘ iso`, diagonal-read and witness blocks all pass
against this branch's kernel) plus this branch's 12 blocks / 903
expectations; `scalafmtCheckAll`, `scalafixAll --check`,
`scalafmtSbtCheck`, `benchmarks/scalafmtCheck`, `mimaReportBinaryIssues`,
`githubWorkflowCheck`, `docs/mdoc` (0 errors), `docs/laikaSite`,
`core/doc`.

## 1. The question

Two shapes of optic meet on one carrier type, and the type does not say
which is which:

- a **broadcast** optic holds one focus and reads it at whatever index
  you like — every optic built by `forgetful2multifocusFunction1` (the
  Iso shim), and any composite whose outer is one;
- a **tabulating** optic reads a real position — `MultiFocus.tuple`,
  `representable` (`representableAt` was retired upstream while this
  branch was rebased; nothing here depended on it).

`mfAssocFunction1` therefore has to choose a read and a write shape per
side. Upstream (`52671355`) answered that with an optic-level class plus a
witness index for the one read that genuinely needs an index, which fixes
`grate ∘ iso` and the tabulating read. This branch answers the *remaining*
gap with data: a bundle says whether the value it holds needs an index at
all. (Before that upstream commit, the kernel sampled the shape, which is
how the older numbers in §6 were produced: main sampled
the outer's read at `null.asInstanceOf[X0]` in `composeTo`
(`kO(null)`) and collapsed every write through `inner.from((null, kD))`
+ `outer.from((null, _ => b))`. Two consequences were observable at a
composite's boundary (both reproduced as failing tests on main, see
§4): `tuple.andThen(<iso shim>).modify` wrote slot 0's value to every
slot, and any composite whose *own* outer read positionally sampled
index 0 instead of the position it was reading.

Two ways to fix it:

| | constancy lives in | visible to | survives a composite |
|---|---|---|---|
| optic-class witness (the `fix/grate-positional-composition` baseline) | the optic object, via a `Function1BroadcastOptic` class + a `broadcastFrom` write hook | `composeFrom`'s pattern match | yes — the *function value* carries the shape upward; the class is re-tested per composition level |
| sum-typed bundle (this branch) | the bundle, as `Broadcast(a, lifted) \| F[A]` | any consumer of the bundle | yes — a composite's read/write shape is recomputed from data, not from a class |

## 2. Design as shipped on this branch

```scala
private[eo] type Focus[F[_], A] = F[A] | Broadcast[F, A]   // the sum
private[eo] final class Broadcast[F[_], A](val value: A, val lifted: F[A])
opaque type MultiFocusK[F[_], X, A] = (X, Focus[F, A])
```

- **The sum hides inside the opaque.** The public shape is unchanged:
  `MultiFocus(x, fa)` builds a plain half, `to: S => (X, …)`-style
  impls still build pairs, `.context` is still identity. The two sum
  cases are `private[eo]`, so user code can neither name them nor hold
  a `Broadcast` as data — which matters, because the carrier classifies
  a half by testing for that class (a user focus whose type was
  `Broadcast`, or `Any` holding one, would otherwise be mis-read).
- **`.foci: F[A]` stays total** by carrying the lifted `F[A]` in the
  broadcast case (for `Function1`, the constant `_ => value`). No
  `Pointed`-shaped constraint, no signature change — that is the whole
  reason `lifted` exists. Cost: one small object per broadcast bundle.
  `value` and `lifted` are built from one value by every producer, and
  `map` keeps them together up to the purity of the mapping function.
- **`.broadcast: Option[A]` is new** — the observable half of the
  design. `Some(a)` marks an index-free bundle, `None` a tabulation.
- **`MultiFocus.broadcast[X0, X, A](x, a)` is new** — the constructor
  for an index-free bundle, for optics that broadcast without an Iso.
  Together with `.broadcast` it is the *entire* public surface of the
  sum.
- **main's class and witness are kept.** `Function1BroadcastOptic` stays as the kernel's
  per-index write hook for the broadcast inner (one value per position, no
  bundle read it did not build), and `forgetful2multifocusFunction1` still
  takes a `RepresentativeIndex[X0]`. The sum and the class answer different
  questions — the class says *this optic writes one value*, the bundle says
  *this value is index-free* — and where they overlap the kernel prefers the
  bundle (so the index-free shape is what travels upward), falling back to
  the class hook inside the tabulating branch, where it needs no per-position
  read.
- **`mfAssocFunction1`** branches on the bundle:
  - read: index-free outer → one `inner.to(value)` call (the composite
    inherits the inner's half); tabulating outer → `i =>
    inner.to(readAt(outer, i))` read at `i`.
  - write: `Z = AssocF1Z[X0, Xo, Xi](xo, innerPlan)` where `innerPlan`
    is `Left(xi)` for an index-free read and `Right(innerReads)` for a
    tabulating one — *the inner reads the read actually observed* (per
    index: leftover, read bundle, shape). `composeFrom` replays them:
    one collapsed `inner.from((xi, bundle))` handed back as a
    `Broadcast` half when the read was index-free, or one
    `inner.from((xiAt(i), Broadcast(writeAt(i))))` per position when it
    was tabulating.
    Recording the observed leftovers is load-bearing, not bookkeeping:
    composition is associative, so a composite is itself an inner whose
    `from` reads its own `AssocF1Z` back — supplying anything else (the
    pre-sum kernel supplied `unobserved[Xi]`) is an NPE on entirely
    shipped factories, `iso.andThen(iso.andThen(iso)).modify(_ + 1)`.
    `mfAssoc` and `mfAssocPSVec` record per-element inner leftovers for
    the same reason.
  - **tabulating ∘ tabulating, stable focus** — the composite reads one
    position per index (the shared index set makes the two sides advance
    together), so its read is not a bijection: each inner structure's
    off-diagonal is not in the composite's focus vector, and the plain
    bundle-only write cannot put it back (there is no `C => D`).
    the stability fact is a property of the *optic*, so it rides on the
    optic: `Optic.SameFocus` (`sameFocus: Option[C =:= D]`) is mixed into
    every `andThen` composite from that call site's `C =:= D`
    (`summonFrom`) and into the monomorphic factories
    (`representable` / `tuple` / `apply`) with `Some(…)` directly. The
    kernel reads it off the `inner` it is already handed and writes the
    *exact inverse* of its diagonal read — every position keeps what it
    was read at, and only the addressed position takes the new value. For
    a type-changing write (`C =/= D`) it is `None`, the kernel keeps the
    documented per-position value write, and that boundary is pinned by a
    test. An earlier cut put this on the composition algebra
    (`AssociativeFunctor.composeFromSameFocus`, with the hint threaded
    through `Optic.andThen`); review pointed out that one carrier's fact
    does not belong in every carrier's contract, and the optic-level
    witness is also the idiom upstream established with
    `Function1BroadcastOptic`.
- **No stand-in values at all.** Every write is handed what its own read produced: the kernel
  records the inner leftovers it observed (`AssocF1Z.innerPlan`), the bridge takes a
  `RepresentativeIndex`, and the collapsing `List` aggregate passes the read's own leftover
  (`o.from((bundle.context, List(agg(bundle.foci))))`). The `unobserved[A] = null.asInstanceOf[A]`
  helper is deleted — its three historical sites (the inner's leftover, the bridge's index, the
  aggregate's leftover) are all fixed at the source rather than patched at the call site.

## 3. What is left over `52671355` (the residual)

Upstream already fixed the positional read (`kO(i)` per index), the
per-index write for the broadcast class, the outer-leftover threading and
the forged index. What this branch still adds, each with a probe or a
green test above:

1. **Tabulating ∘ tabulating is lawful.** `tuple ∘ tuple` reads one
   position per index, so its read is a diagonal; upstream writes one
   collapsed row for it (`modify(identity)` → `((1,4),(1,4))`),
   `replace(9)` → `((9,9),(9,9))`). Here the write is the exact inverse of
   the read whenever the inner optic carries `Optic.SameFocus` (every
   monomorphic factory and every `andThen` composite whose focus types
   coincide do), so `modify(identity)` is the identity,
   `modify(h) ∘ modify(f) == modify(f andThen h)`, and `replace(v)` touches
   only the positions it was given — including when the inner is itself a
   composite (`tabulating ∘ (tabulating ∘ shim)`).
2. **Composite inners are written per position.** Upstream's witness is the
   *class*, and a composite is not one, so `tuple ∘ (shim ∘ shim)` collapses
   (`modify(_ + 1)` on `(1,2)` → `(2,2)`); here it is `(2,3)`. The bundle
   says what the class cannot.
3. **The inner's leftover is real, not forged.** Upstream threads only the
   outer's (`Z = Xo`) and still hands the inner `unobserved[Xi]`, so an inner
   whose write reads its leftover — `fromLensF` with a shipped `Lens` — dies
   with an NPE. Here the read records every leftover it produced and the
   write replays it.
4. **Constancy is inspectable.** `.broadcast` is a public, typed answer to
   "does this bundle need an index?" — the API main does not have — and a
   broadcast optic's bundle is index-free by construction, so the index-free
   shape travels upward through `map` / `collectWith` / `andThen`.
5. **Upstream machinery is kept, not replaced.** The class stays as the
   kernel's per-index hook (the cheapest path for the shim inner), the bridge
   keeps its witness, and `unobserved` drops from three sites (upstream's
   kernel leftover, upstream's aggregate leftover, the bridge's forged
   index) to **none**.

What it does **not** add: anything main already covers. The positional
read/write, `Z` threading, the witness and its refusal semantics, the QA
table, `GrateShapeSpec` and upstream's own test blocks are inherited
unchanged — upstream's F1 spec passes verbatim against this kernel.

## 4. Evidence

`core/src/test/scala/dev/constructive/eo/MultiFocusFunction1CompositionSpec.scala`
(12 blocks, 903 expectations) plus
`core/src/test/scala/external/EoOpaqueSurfaceSpec.scala` (the public
surface seen from *outside* the `eo` package tree — it pins that the
internal sum neither leaks nor breaks inlining at a downstream call
site). Two of the composition blocks fail on `main`, pass here — runs on
main: `2 failures / 4 examples`; here: `12 examples, 0 failure`.

| block | `52671355` (fresh probe) | here |
|---|---|---|
| `tuple.andThen(<iso shim>)`: per-position write survives | ✗ (`modify(identity)` → `(l, l)`) | ✓ |
| `<iso shim>.andThen(MultiFocus.tuple)`: tabulated read + single-value write | ✓ | ✓ |
| `(<iso shim> ∘ representable).andThen(<iso shim>)`: stays positional | ✗ (samples one index) | ✓ |
| bundle shape: `.broadcast` / `.foci` / chain | n/a (no surface) | ✓ |
| `<iso shim> ∘ (<iso shim> ∘ <iso shim>)`: right-associated chain | ✗ (NPE, see §6) | ✓ |
| `<iso shim> ∘ (<positional probe> ∘ <iso shim>)`: leftovers survive | ✗ | ✓ |
| `fromLensF` inner: the lens's leftover survives the write | ✗ (null component / NPE) | ✓ |
| `tuple ∘ (<iso shim> ∘ <iso shim>)`: per-position write through a composite | ✗ | ✓ |
| `tuple ∘ tuple`: diagonal read, in-place write (MF1 / MF2 / replace / `at`) | ✗ (`modify(identity)` → `((1,4),(1,4))`) | ✓ |
| associativity across shim / tabulating mixes | ✓ (all-shim chain) | ✓ |
| tabulating ∘ composite-inner (`tuple ∘ (shim ∘ shim)`) | ✗ (`modify(_+1)` on `(1,2)` → `(2,2)`) | ✓ (`(2,3)`) |
| `fromLensF` inner (its write reads its leftover) | ✗ **NPE** (`GetReplaceLens.from` dereferences the forged `null`) | ✓ |
| type-changing write through a tabulating composite (documented fallback) | n/a | ✓ |
| `collectList` on a Lens / Prism / Optional-provenanced optic | ✗ NPE / `MatchError` | ✓ |
| positional factories: bare modify / replace / at / collect | ✓ | ✓ |

The `collectList` row is a real bug, not a smell: probing a lawful Lens
whose write reads its leftover (`s.isEmpty`) under the shipped `fromLensF`
threw `NullPointerException` in `GetReplaceLens.from`; the Prism and
Optional provenances threw `MatchError: null` (their writes pattern-match
the leftover). Those three now pass, pinned by the new runtime sweep
`tests/.../MultiFocusCrossFamilySpec.scala` — nine property blocks covering
every inbound provenance through `modify` / `replace` / `collectMap` /
`collectWith` / `collectList` / `foldMap` / `headOption` / `length`, plus
both composition directions across Iso / Lens / Prism / Optional with
independently computed expectations.

The three ✗ rows were re-probed on `52671355` with a throwaway spec that
prints instead of asserting (removed before the rebase): `tuple ∘ tuple`
still collapses to one row (`.replace(9)` → `((9,9),(9,9))`), the
composite-inner case still takes the collapse branch because a composite is
not the broadcast *class*, and the `fromLensF` inner still receives a
forged leftover and dies. That is the honest boundary of the upstream
answer: its class witness cannot describe a composite inner, and its
`Z = Xo` threads only the outer's leftover.


Whole-suite gates on this branch: `core/test` 14 specs green,
root `test` 237 examples / 0 failures, `docs/mdoc` 0 errors (the new
snippets compile from outside `core`, which also pins that `.broadcast`
/ `MultiFocus.broadcast` are usable without the opaque's internals).

### Measured cost — no defensible number on this host

The `.foci` shape test, the `Broadcast` wrapper and the `andThen` witness
argument all cost something, and this note will not pretend to have
measured it. JMH runs from this environment put the *untouched Monocle
control* — `PowerSeriesNestedBench.monocle_nested`, a different library
that neither change touches — between **-30% and +19%** across
back-to-back runs on the same commit, and the F1 microbenches swing ±12%
run to run. With that noise floor nothing about a few-percent delta is
attributable, and the repo's own benchmark doctrine (quiet machine,
`@Fork(3)`+, `B/op` gates) is not satisfiable here. Two structural
statements stand instead, both reviewable by reading the code:

- the shape test only ever runs on a *tabulating* bundle whose value is not
  a `Broadcast` (the index-free case returns the stored lift), so it is a
  predictable never-taken branch on the container carriers;
- the extra inner read is confined to the tabulating branch and skipped
  entirely for the broadcast class (the hook path).

Numbers for the record, same session, `-f 3`: `eoModify_multiFocusTuple3`
≈ 20–22 ns/op and `Tuple6` ≈ 32–40 ns/op on either tree; the shared
harness's variance band is wider than any difference between them.

## 5. Cost and risk

- **Binary compatibility: the descriptors do *not* change; the inlined
  bodies do.** `MultiFocusK` still erases to `Tuple2` — the union is the
  second type argument, whose erased type was already `Object`
  (`F[A]` on an abstract `F`). What breaks is (a) code compiled against
  0.18 that inlined `.foci` / `.context` / `apply`: those bodies extract
  `Tuple2._2` and cast it to `F[A]`, and a newly produced broadcast
  bundle puts a `Broadcast` there instead, so the cast fails; (b) any
  client that named the F1 kernel's context refinement
  (`mfAssocFunction1.Z = Unit` on main, now `AssocF1Z[…]`). Source
  compatibility holds for normal callers: the module call sites in
  `avro` / `circe` / `jsoniter` / `zio` / `kyo` needed no respelling —
  only `MultiFocus.scala` itself did (see the next point). MiMa is
  disabled on the 0.x line (`mima.sbt`) and every minor so far has been a
  deliberate breaking release, so this is a CHANGELOG line, not a
  blocker.
- **It edits fresh upstream code.** The kernel, the broadcast class's `to`
  and the bridge are all from `52671355`; landing this means rewriting the
  kernel's write path (three cases instead of two) and re-reviewing the
  class against the sum. That review cost is the price of the four items in
  §3, and it is the strongest argument for harvesting 1–3 as a small kernel
  delta instead (see §7).
- **The extra inner read is now conditional.** The diagonal write re-reads the
  inner per written position *unless* the inner is the broadcast class (the hook
  path) — so the shim pays nothing, and only a tabulating or composite inner
  pays it.
- **Threading the read's leftover has one boundary: a *composite*.** An
  optic whose context records per-focus structure (`MultiFocus.apply[List, Int].andThen(lens)`
  has `Z = (Xo, F[(Xi, Int)])`) cannot honour a write whose vector has a
  different cardinality than the read's, and `collectList` is exactly that
  write: on the empty input it rebuilds `Nil` rather than `List(0)`, and on
  a longer read it would index past the collapsed vector. The old forged
  `null` failed loudly (NPE) where this now fails quietly-or-loudly
  depending on shape, so `collectList` is asserted only for
  count-agnostic provenances (generic factory, Iso/Lens/Prism/Optional
  bridges) and the composite boundary is *named* in
  `MultiFocusCrossFamilySpec` rather than pinned. A witness on the optic
  (`Optic.SameFocus`-style) is the way to turn it into a compile error.
- **Churn, not just additions.** The compiler forces every
  `new Optic[…, MultiFocus[F]]:` impl in `MultiFocus.scala` to spell its
  `to` / `from` in the carrier type (a narrower *parameter* type is
  illegal on an override), so ~20 signatures were respelled and their
  bodies now consume `.context` / `.foci`. That is most of the diff size
  and it is mechanical — but it is the kind of diff that hides a
  transcription error, so the root suite (not only the F1 spec) is the
  gate that matters here.
- **A tabulating outer re-reads the inner per written position.**
  `composeFrom`'s per-position branch calls `inner.to(outerRead(i))`
  again to recover that position's leftover, because `AssocF1Z` cannot
  hold the method-typed read bundle. Pure optics make that equivalent,
  but it is one extra inner read per written position that main did not
  pay (main paid one read in total — and got the wrong answer).
- **One new object per broadcast read**, and one `instanceof` per
  `.foci` read on every carrier. The `.foci` branch is what makes the
  sum invisible to existing consumers; it cannot be removed without
  giving `.foci` a `Pointed`-shaped constraint.
- **One new public name pair**: `MultiFocus.broadcast` / `.broadcast`.
  The sum's cases (`Focus`, `Broadcast`) are `private[eo]` on purpose:
  the carrier classifies a half by testing for that class, so user code
  must not be able to hold one as data, and the public story is the
  accessor rather than the representation. (An earlier cut had them
  public; a `ClassCastException` / silent mis-read was reachable from
  plain `MultiFocus[cats.Id, Unit, Broadcast[List, Int]]((), value)`.)
  inline bodies and downstream optic authors both need them.
- **`.broadcast` allocates an `Option`**; the kernels use the raw half
  instead, so only explicit user calls pay it.
- **A type-changing write through a tabulating ∘ tabulating composite is
  still under-determined** — the one lossy boundary left. The composite
  reads one position per index (the shared index set cannot address
  `(outer position, inner position)` pairs), so with `C =:= D` the
  kernel writes the exact inverse of that read, but with `C =/= D` there
  is no `C => D` to put the unread positions back and it falls back to
  the per-position value write (pinned by the "documented lossy
  fallback" block). `modify` / `replace` / `set` are all on the exact
  side, because their focus type is stable; a hand-written polymorphic
  inner is what takes the fallback path. The stable-focus write re-reads
  the inner once per written position (it must recompute the read it is
  inverting — the plan holds the outer's read function, not the read
  bundles), so a `.modify` through a tabulating composite costs one extra
  inner read per position versus the lossy path.

## 6. What review found (and what it changed)

An independent pass over the first cut of this branch found four things
worth recording, all addressed here:

1. **A blocking NPE on entirely shipped factories.** The first cut kept
   `Z`'s shape witness but still handed the inner an invented leftover,
   so `iso.andThen(iso.andThen(iso)).modify(_ + 1)` — an inner that is
   itself a composite — read a null context. Fixed by recording the
   *observed* inner leftovers (§2); both new right-associated blocks pin
   it.
2. **`fromLensF` as an inner is reachable, and was wrong.** Its write
   reads its leftover back, so the invented one dropped the carried
   component (or NPE'd). Reachable with shipped `Lens` + `fromLensF`
   under a shipped Iso shim, on main as well as on the first cut. Fixed
   by the same leftover threading; pinned by its own block.
3. **`tuple ∘ tuple` was unlawful, and its test composed nothing.** The
   read is a diagonal, so the write could not invert it from the bundle
   alone. Fixed here by the same-focus entry point (§2), which is exact
   whenever the focus type is stable; the mislabelled test block now
   names what it does and the real composition is pinned by the new
   spec.
4. **The untagged union could misclassify ordinary payloads.** With the
   sum's cases public, `MultiFocus[cats.Id, Unit, Broadcast[List, Int]]((), value)`
   made the carrier test a user value for its own internal class (CCE
   with a `Broadcast`-typed focus, silent wrong answer with `A = Any`).
   Fixed by making the cases `private[eo]`; the public surface is
   `.broadcast` + `MultiFocus.broadcast`.
5. **Two claims were wrong** and are corrected above: the erasure
   explanation (descriptors are `Tuple2` either way) and the vacuous
   `MultiFocus.tuple.andThen(MultiFocus.tuple)` test block, which never
   composed anything — it now names what it does, and the real
   composition is pinned in the new spec.

Left as documented behaviour rather than fixed: the shim's `from`
stand-in for a hand-rebuilt tabulating bundle (§2), and the type-changing
write through a tabulating ∘ tabulating composite (§5).

## 7. Cheaper variants of the same idea

- **Subtype sum, no representation change**: keep
  `(X, F[A])` and make the broadcast case a `Function1` subclass
  (`class Broadcast[X0, A](val value: A) extends (X0 => A)`, detected by
  a marker trait). Zero cost for other carriers, no erasure change, no
  churn — but the sum is a runtime class tag rather than a type, and the
  marker has to be re-established by every transform (`map` / `collect`
  helpers) or constancy silently dies on the way through.
- **Optic-class witness**: an `indexFree` class plus a `broadcastFrom`
  write hook on the optic — **shipped upstream as `52671355`**. No carrier
  change, no `.foci` branch, no extra allocation; the shape is a property of
  the optic, so it is re-tested per composition level and the kernel needs no
  context bit. Its gap is exactly §3's items 1–3: a class cannot describe a
  composite, and one threaded leftover is not all of them.
- **Harvest items 1–3 into that kernel** (the recommended cheap path, not
  prototyped here): keep `Z = Xo`, add a per-index leftover record the read
  fills in, and read the `Optic.SameFocus` witness off the inner (both facts
  are already on the optic, and `andThen`'s one-line mixin is carrier-agnostic).
  That is a kernel-local change — no carrier change, no respelling, no
  `.foci` branch, no erasure break, roughly 50–60 lines — and it buys the
  three ✗ rows of §4 without the sum's costs.

## 8. Recommendation

**Harvest §3's first three items; take the sum only if `.broadcast` is
wanted as public API.**

The runtime rules this branch lands are not really a property of the
sum — any path that knows the shape at the kernel needs them:

1. a composition's write shape follows the shape its *read* took,
   recorded in the composite's context;
2. `composeFrom` must be handed the inner leftovers the read observed —
   composition is associative, so an inner that is itself a composite
   reads its own context back. This branch had to be fixed for exactly
   that reason (§6.1), and the same rule applies to any optic-class
   witness that threads only one side's leftover.

The sum's *specific* value is §3's item 4: constancy is inspectable
(`.broadcast`) and a composite's shape is derived from data, so it is
correct by construction rather than re-tested. Choose it if you want that
surface, and accept in exchange:

- one `instanceof` per `.foci` read on every carrier and one extra object
  per broadcast read (measured: flat on the PSVec hot path, ~3%
  noise-adjacent on a 25 ns/op Grate microbench, iso-shim path
  unbenchmarked);
- a mechanical ~20-signature respelling inside `MultiFocus.scala`, which
  is most of the diff;
- the inlined-body binary hazard and the `AssocF1Z` refinement change;
- an extra inner read per written position on the tabulating write path.

If instead you want the smallest, least-risky change with the same
runtime behaviour, the subtype sum (§7) or the optic-class witness keeps
`(X, F[A])` exactly as it is: no branch on every carrier, no extra
allocation, a fraction of the diff — at the price of constancy staying an
internal fact (no `.broadcast`) and, for the subtype sum, of every
identity-preserving transform having to re-establish the marker.

Either way, the two positional bugs of §4, the leftover-threading rule of
§6 and the stable-focus write of §2 should land. The one boundary that
cannot be made lawful — a type-changing write through a tabulating ∘
tabulating composite — is pinned by a test rather than left implicit.
