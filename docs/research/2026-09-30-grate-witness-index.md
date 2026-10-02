# Grate index witness — supplying the index instead of forging it

**Question.** `MultiFocus[Function1[X0, *]]` (the Grate carrier) has exactly one read that no rule of
the type system can serve: `Function1BroadcastOptic.from` must turn a written bundle `X0 => B` back
into a `T`, and its own carrier stores only `Unit`. Baseline `81a53d3d`
(`fix/grate-positional-composition`) left that read at a `null` sentinel plus a documented
constant-bundle contract. This note measures what it costs to make that index a *real* value, and
what the change can and cannot buy.

**Verdict, up front.** The witness is worth shipping as *hygiene* — the read stops being a forged
value and the constant-bundle contract stops being the only thing standing between the library and
an unspecified read — but it must not be sold as a fix. Nothing a user can write today changes
behaviour: every shipped path hands that `from` a constant bundle. The honest price is a new public
typeclass in implicit position on a shipped `Composer`, plus the loss of that `Composer` for index
types that have no inhabitant. See §7 for the recommendation and §6 for the comparison table.

## 1. The stand-in sites before this change

`core/src/main/scala/dev/constructive/eo/data/MultiFocus.scala`, baseline `81a53d3d`:

```scala
private[eo] inline def unobserved[A]: A = null.asInstanceOf[A]   // the only null.asInstanceOf site
```

| # | site | stands in for | producer missing because |
|---|------|---------------|--------------------------|
| 1 | `Function1BroadcastOptic.from` — `foci(fb)(unobserved[X0])` | an **index** into a bundle the optic did not build | the carrier is `(Unit, X0 => A)` and the written focus lives inside the function |
| 2 | `mfAssocFunction1.composeFrom` bundle branch — `inner.from((unobserved[Xi], kD))` | the **inner optic's leftover** | `Z = Xo` threads the outer's leftover; the inner's is produced by `composeTo` and discarded |
| 3 | `collectList` — `o.from((unobserved[o.X], List(b)))` | the **optic's own leftover** | the aggregation rebuilds a singleton `List(b)` and never sees the leftover it discards |

Site 1 is the only *index*. Sites 2 and 3 are existential leftovers: per-position data whose only
producer is the read side.

## 2. What the witness removes

`RepresentativeIndex[X0]` (new, `core/src/main/scala/dev/constructive/eo/data/RepresentativeIndex.scala`)
carries one real `X0`. The bridge takes it at construction and the optic stores it:

```scala
final private[eo] class Function1BroadcastOptic[S, T, A, B, X0](o: Optic[S, T, A, B, Direct], at: X0)
    extends Optic[S, T, A, B, MultiFocus[Function1[X0, *]]]:
  def from(fb: MultiFocus[Function1[X0, *]][X, B]): T = broadcastFrom(MultiFocusK.foci(fb)(at))

given forgetful2multifocusFunction1[X0](using ri: RepresentativeIndex[X0])
    : Composer[Direct, MultiFocus[Function1[X0, *]]] with
  def to[S, T, A, B](o: Optic[S, T, A, B, Direct]) =
    new Function1BroadcastOptic[S, T, A, B, X0](o, ri.index)
```

Site 1 is gone: no `null` reaches a bundle read, and the read now has a stated answer even for a
bundle that varies. `unobserved` keeps exactly the two leftover sites, and its docstring gets
stronger — the remaining uses are *never* indices, so the class-wide invariant is now "an optic's
own leftover on a write path that discards it" rather than "an unobserved value, one of which might
be an index".

## 3. What the witness provably cannot remove

- **Site 2 (inner leftover).** A witness is an `X0`. What `inner.from` needs is an `Xi` — its own
  leftover, produced per position by the inner's `to`. No index value can stand for it, and no
  *single* value can either: nothing constrains the inner's leftover to be constant across the
  index space. Recovering it means threading `X0 => Xi` through the composition's `Z` (a function of
  the index, built in `composeTo`, consumed in `composeFrom`), which changes the public `Z` of every
  `MF[F1] ∘ MF[F1]` composite from `Xo` to a pair. That is "thread the existential", not "make the
  index real", and it is out of scope here. This is the sharp version of the user-visible summary:
  **a per-position write needs one value per index, not one index.**
- **Site 3 (own leftover).** `collectList` collapses a focus list to one element; the leftover that
  would rebuild through it was never read. Same shape: an index witness cannot reach it.
- **An empty index space.** `Function1[Nothing, *]`-indexed (a phantom slot) has no inhabitant to
  witness, so the variant *refuses* the bridge rather than reading a bundle at an impossible index.
  That is the mechanical cost of the whole path: the sentinel existed because a read with no index
  still had to return something.

## 4. Index availability per shipped Grate factory

The factories never need an index themselves — they walk their own space (`tuple` counts
`0..size-1`, `representable` tabulates). The index is needed by the *bridge*, and its type is fixed
by whichever carrier a Direct optic is being composed into:

| carrier source | `X0` | usable index value? |
|---|---|---|
| `MultiFocus.tuple[T, A]` | `Int` | yes — `0`, in range whenever `T` is non-empty (the shipped `GrateShapeSpec` / `MultiFocusFunction1Spec` cells are all this shape) |
| `MultiFocus.representable[F, A]` | `F.Representation` | **no** — `cats.Representable` exposes `index` / `tabulate` / the abstract `Representation` type and nothing else (verified with `cellar get-external org.typelevel:cats-core_3:2.13.0 cats.Representable`), so the caller must supply one (`MultiFocusFunction1Spec` does it with one `given` line on a permuted three-point instance) |
| `MultiFocus.apply[F, A]` over `F = Function1[X0, *]` | `X0` | no — whatever the function's domain type is; `Int` and `Boolean` are covered by shipped instances, everything else is caller-supplied |
| the bridge itself (`Direct → MF[F1[X0, *]]`) | free `X0` | no — this is the constraint under discussion |

### The adjacent ruling on main (#123), and how this differs

`origin/main` `6e53b857` retired `MultiFocus.representableAt(F)(repr0)` on the grounds that **position
is a read-time argument, never a property of the optic** — the factory tabulated pointwise, so `repr0`
never reached the built optic, and `.at(i)` subsumes it per call. That is the same *class* of
question this note asks (should an index live on the optic?), and the maintainer answered it with
"no" — twice over: the ruling also records that giving `repr0` runtime meaning would take "a field
on the optic class plus a runtime witness match (the `Function1BroadcastOptic` shape)", i.e. exactly
this prototype's mechanism. Two things separate the cases, and they are the load-bearing part of the
recommendation:

1. **`.at(i)` subsumes `repr0`; nothing subsumes the bridge's index.** A built Grate optic's reads
   are all caller-driven, so a construction-time index has no reader. The bridge's `from` is the
   opposite: the kernel hands it a whole bundle and there is no caller to ask, so its options are a
   real value at construction or a forged one at the read.
2. **A construction-time index is a claim about the optic; a bridge witness is not.** `repr0` said
   "this optic is *at* this index" — false, since two calls built the same optic. `RepresentativeIndex`
   says "if you ever have to read a bundle this optic did not build, read it here" — a fallback
   position, unobservable on every shipped path (§8).

The honest residual: the shipped `Boolean` → `false` instance *does* privilege a value, in the very
way #123's ruling declines to for `Function1[Boolean, *]`. It is defensible only as a convenience for
implicitness, and the alternative (no shipped instances) is priced in §7.

## 5. The candidate supplies, measured

| route | verdict |
|---|---|
| **`Representable`-derived** | **Impossible.** No `Representation` value exists in the typeclass; `representableAt` (retired on main, see §4) had to take one from the caller for the same reason. |
| **`ValueOf` / singleton-typed** | **Does not cover the shipped surface.** `ValueOf[Int]`, `[Boolean]`, `[Unit]`, `[Long]` do not resolve (measured: compile error on `summon[Option[ValueOf[Int]]]` in a scratch spec); synthesis exists only for literal / stable singleton types, and only in a `using` position. Gating the bridge on `ValueOf` would turn the `iso ∘ grate` cell red for every shipped Grate (`tuple`'s index is `Int`). Kept as a *secondary* instance (`given singleton[X0 <: Singleton](using ValueOf[X0])`) for the exotic case — `def narrow[R <: Singleton](r: R)` does infer `R = true` for `narrow(true)` (measured), so literal index types work. |
| **Witness typeclass with canonical instances + explicit `at`** | **This prototype.** `RepresentativeIndex[Int] = 0`, `[Boolean] = false`, `[Unit] = ()`, singletons via `ValueOf`, everything else via `RepresentativeIndex.at(i)` (local `given` or explicit). Keeps all 6 composing Grate cells green (the shipped index types all have instances) and keeps `.andThen` import-free. |
| **Explicit-index bridge only** (no typeclass, no implicit `Composer`) | Not prototyped: it deletes the only inbound bridge for the absorbed-Grate sub-shape, so `iso ∘ grate` (and `Iso → Traversal.two/three/four`, which ride the same morph) would have to be written as `.morph(using …)` everywhere. The cell count in `GrateShapeSpec` would drop by one and `CompositionMatrixSpec`/docs would follow. This is the pure form of the trade; the prototype's typeclass is strictly weaker (it keeps implicitness wherever an index exists) at the cost of a public typeclass. |

## 6. Comparison table

| axis | sentinel (baseline `81a53d3d`) | witness (`RepresentativeIndex`) |
|---|---|---|
| **implicitness** | `iso.andThen(grate)` resolves with no imports, for every `X0` | still resolves with no imports for `Int` / `Boolean` / `Unit` / singleton indices (companion instances — nothing to import); **refused** for an index type with no instance unless one is put in scope |
| **API churn** | zero | new public `data.RepresentativeIndex[X0]` (+ `at`, 4 instances); `forgetful2multifocusFunction1` gains a `using`; `Function1BroadcastOptic` gains a field (both `private[eo]`); docs: `multifocus.md` bridge row + the "carries no constraint" sentence, QA grate legend, CHANGELOG (MiMa is off on 0.x) |
| **index availability** | irrelevant — nothing is read | `tuple` ✓ (`Int`→0), `representable` ✗ needs a caller witness per index type (`F.Representation` has no canonical inhabitant — #123's ruling, §4), `apply[F1[X0,*]]` ✗ for non-`Int`/`Boolean` `X0`, bridge over an arbitrary `X0` ✗ |
| **ergonomics** | nothing to write | a local `given RepresentativeIndex[X] = RepresentativeIndex.at(v)` (one line) where no canonical instance exists; on a `Representable`-indexed grate that is the same value the read side passes to `.at(i)` per call |
| **behaviour for an uninhabited `X0`** | compiles; the read can only be reached through the constant-bundle contract (a lambda that ignores its argument never dereferences the sentinel) | **does not compile** — no instance can be produced for a type with no inhabitant, so the read that has no answer is refused at the bridge |
| **observable behaviour, shipped paths** | identical | identical (measured: witness 0 vs witness 1 give the same `modify` / round-trip results) |

## 7. Recommendation, with the honest cost

**Ship the witness as hygiene, not as a fix** — the change is small, keeps every shipped composition
cell green, and retires the one place where core forged an *index*. Do not expect it to change any
result a user can currently observe; §8 is the evidence for why. Read §4's #123 subsection first:
the maintainers have already ruled against construction-time indices once, and this path has to be
the exception it is (nothing subsumes the bridge's index) rather than a quiet contradiction of it.

What the developer pays:

1. **A public typeclass in implicit position on a shipped `Composer`.** New API surface, a new
   failure mode (`iso.andThen(grate)` stops resolving for a non-canonical index type, with an
   implicit-not-found message naming `RepresentativeIndex` — mitigated by an `@implicitNotFound`
   if that matters), and a subtle footgun: a *local* witness silently changes the position read on
   a varying bundle. It cannot change a constant-bundle path.
2. **A capacity regression for empty index spaces.** The `iso ∘ grate` chain no longer compiles
   when the grate's index type has no inhabitant. Nothing in the tree exercises it, and the read it
   would have taken is unanswerable, but it *was* reachable before.
3. **A field that shipped paths do not read.** This is the same *shape* as the deleted
   `MultiFocusLeadPosition` (dropped in `12b827b9` as dead code, +20% on `Grate.modify`), and #123
   declined the same shape for `repr0`. The difference is that this field is read where a value is
   genuinely required, and §8 pins that read with a test. It is still not benchmarked:
   `MultiFocusCollectBench` drives `MultiFocus.tuple` directly and never builds a bridge, so the
   change cannot show up in the Grate bench (neither as a win nor as a regression). Reasoning from
   the code, not from a run: the bridge instance is built once per morph, outside any measured loop.
4. **A shipped instance that privileges a value** (`Boolean` → `false`). Drop the `Boolean` /
   `Int` / `Unit` instances and this cost disappears at the price of three red cells and a `given`
   line at every call site that has no witness of its own — the sharper trade if the team reads
   #123's ruling strictly.

Alternatives, and why they lose here:

- *Explicit-index bridge only* — maximal honesty, deletes a documented capability. Only worth it if
  the team wants the Grate inbound to be a deliberate, spelled-out act.
- *Keep the sentinel* — the baseline is defensible: sites 2 and 3 stay either way, so the "no
  `null` in core" argument is already half-compromised, and the constant-bundle contract is
  enforced by the kernel's `Function1BroadcastOptic` dispatch rather than by a value. If the team
  also wants to hold #123's line consistently (no index on an optic, ever), this is the coherent
  answer rather than this prototype.
- *Remove the read* — if `from` could avoid reading a bundle (a carrier whose write does not travel
  as a function), no index would be needed at all. That is a carrier change, not a bridge change,
  and it is the other design path's territory.

## 8. Prototype evidence

Branch `feat/grate-witness-index`, rebased onto `origin/main` `6e53b857` (the #123 retirement) on top
of `81a53d3d`. The rebase also dropped every stale `representableAt` mention the branch carried, added
the break entry to `mima.sbt` per the line's convention, and wired the new witness block onto #123's
own permuted `Tri` / `Slot` fixture.

| test (`core/src/test/scala/dev/constructive/eo/MultiFocusFunction1Spec.scala`) | pins |
|---|---|
| `RepresentativeIndex: the bridged from reads at the supplied index; constant bundles are witness-invariant` | the read really is at `at` (`-1000` vs `-990` for a varying bundle read at index 0 vs 1 — white-box, see below); the shipped path is invariant (`from(to(x)) == x`, `modify(_+1)(3) == 4` under both witnesses) |
| `grate ∘ iso stays positional under a non-canonical witness` | the baseline's positional guarantee survives a deliberately "wrong" witness (`at(2)`): `modify(_+1)((10,20,30)) == (11,21,31)`, reads at 0/2 unchanged — the witness is not a write index |
| `explicit witness on an algebraic index` | a `sealed trait Side` index (no canonical inhabitant, one `given` line) bridges, composes and stays positional |
| `witnessed permuted Representable` | on #123's own `Tri` / `Slot` fixture (index order is a permutation of field order), the witness `S1` is what resolves the bridge over a `representable` grate, and the write still equals the instance's own `map` — position stays a property of the *call*, not of the optic |
| `RepresentativeIndex: witnessed index bridges, unwitnessed index does not resolve` | `typeChecks` cells: same carrier shape with a witness → true, without → false |
| `RepresentativeIndex: the shipped instances name a real index` | `0` / `false` / `()` / the singleton — so the instances are values, not a renamed sentinel |
| `RepresentativeIndex: a Boolean-indexed grate bridges off the companion instance` | the *second* shipped index type reaches the bridge with no local `given` (the doc claim that the grid stays import-free) |

All seven, plus the full root aggregate (`sbt test`: all modules, 0 failures), `GrateShapeSpec` /
`CompositionMatrixSpec`, and the line's CI gate set — `scalafmtCheckAll`, `scalafmtSbtCheck`,
`benchmarks/scalafmtCheck`, `scalafixAll --check`, `githubWorkflowCheck`, `mimaReportBinaryIssues` —
plus `docs/mdoc` and `docs/laikaSite`, pass. Two measurements worth keeping:

- **The witness's observable surface is empty on the public API.** The carrier's `X` is abstract at
  every call site that goes through `Composer.to`, so a *data-dependent* bundle cannot even be
  named by a caller: `val b: MultiFocus[F1[Int, *]][bridged.X, Int] = MultiFocus(...)((), i => i * 10)`
  fails with `Found: Unit, Required: bridged.X`, and the only bundle anyone can build for that
  receiver is the optic's own constant `to`. The varying-bundle test above is therefore white-box
  (`Function1BroadcastOptic` is `private[eo]`, and the spec lives in that package). This is the
  strongest argument *for* the witness (the read is unobservable, so its correctness should be
  structural, not a contract) and against overselling it.
- **A price-free variant does not exist.** Removing the *category* rather than the instance would
  need an index-free carrier for the bridge's write; every read of an `X0 => B` needs an `X0`.

## 9. What would change this answer

- If a user-visible API ever hands a **varying** bundle to a broadcast `from` (a `put`/`place`-style
  entry point, an existential-bundle composition), the witness stops being hygiene and becomes the
  only thing keeping that read defined — ship it now, cheaper than later.
- If the Grate inbound is meant to be spelled out at every call site (a deliberate engineer-facing
  gate), drop the implicit `Composer` and keep only `RepresentativeIndex.at` — a one-line change on
  top of this prototype.
- If the team prefers zero new public surface, keep the sentinel and re-word the contract; sites 2
  and 3 mean the "no forged value" story is already partial.
