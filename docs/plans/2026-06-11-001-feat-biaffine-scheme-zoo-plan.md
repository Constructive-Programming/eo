---
title: "feat: BiAffine carrier + the typed recursion-scheme zoo as optics (para/apo/histo/futu, M-generic driver)"
type: feat
status: implemented (CI bench numbers + code review pending)
date: 2026-06-11
origin: docs/brainstorms/2026-06-08-recursion-schemes-in-eo-requirements.md
grows: PR #24 (feat/typed-recursion-schemes)
revised: 2026-06-11 (v2 — schemes are optic values, decorations are BiAffine optics, M-generic driver; free-range gcataF/ganaF dropped)
---

# feat: BiAffine carrier + the typed recursion-scheme zoo as optics

## Overview

Grow PR #24 (`cataF`/`anaF`/`hyloF`) into the **wider recursion-scheme zoo** — para,
apo, histo, futu — built on three structural commitments (v2):

1. **Decorations are optics.** A generalized scheme's gather/scatter pair *is* an
   optic in eo's own encoding: `scatter: W => Either[A, F[W]]` is an affine match
   whose leftover is one F-layer; `gather: (A, F[W]) => W` is the product build that
   consumes it. Together: `Optic[W, W, A, A, BiAffine]` with existential `X` a
   `Tuple2` (so `Fst`/`Snd` reduce, as with `Affine`): `Snd[X] = F[W]` — the
   one-F-layer leftover — **uniformly**; `Fst[X]` — the `Done` payload — **pinned
   per value** (apo: a finished `S`; futu: a prebuilt layer `F[W]`).
   Members inhabit the family the way `Fold`/`Review` inhabit the optic lattice:
   fold-side decorations (para/histo) are **build-only** members (gather = `from`,
   read side Unit-pinned), unfold-side (apo/futu) are **read-only** members
   (scatter = `to`) — laws bind the inhabited side. The zoo is a **vocabulary of
   named decoration values** of this one family — not free-range `gcataF`/`ganaF`
   methods (dropped).
2. **Schemes are optic values with fusion semantics.** `cataF`/`anaF` return concrete
   optic classes that *carry their (co)algebra and decoration as data*, so
   `anaF(coalg).cross(cataF(alg))` **fuses to hylo** — deforestation as composition,
   on the seam core already names for it (`Optic.cross`'s scaladoc: "the motivating
   case is `ana.cross(cata)`"), implemented as a fused overload in the `Unfold`
   (#26)/`DirectGetter` style. On the M path fusion IS a fused `andThen` — there the
   seam genuinely is focus→source (`Forget[M]` Kleisli). `hyloF` remains as the name
   for the fused result (and the always-fused spelling), not a third independent
   driver.

   > **Superseded (2026-06-13, directional flip).** The `.cross` framing above was
   > a directional inconsistency: pure `ana` shipped as a *backward* `Review[S, Seed]`
   > while its effectful twin `anaM` was a *forward* `FoldM[Seed, S]`. `.cross` was
   > only needed to undo that backwardness. Resolution: pure `ana`/`apo`/`futu` are now
   > **forward `Getter[Seed, S]`** (mirroring `anaM`), so the materialising refold is
   > plain `ana.andThen(cata) : Getter[Seed, A]` via the core fused `Getter.andThen` —
   > no `cross`, no `Cata`/`Ana`/`CataM`/`AnaM` clone classes (deleted). `Schemes.hylo`
   > stays the fused (no-intermediate-`S`) spelling. Note the flip makes `ana.andThen(cata)`
   > genuinely *materialising* (885k B/op, == manual `cata.get(ana.get())`); the old
   > fused-pairs `.cross` member (820k) is gone, but the real fusion win lives in `hylo`
   > (361k, unchanged). The M path mirrors this: `anaM.andThen(cataM)` is a concrete
   > `FoldM.andThen` (Kleisli `flatMap`, materialising); `hyloM` is the fused spelling.
3. **The driver is M-generic.** Computational steps evolve in a `Monad[M]` (the arbo
   `Calculator` shape: fetching children is effectful, `GetSellOptions[M, O]`).
   Effectful schemes return **`Forget[M]`-carried citizens** (`Seed => M[B]` is a
   Fold over `Forget[M]` — an existing carrier with existing composition via
   `assocForgetMonad`/`ReadCompose`). The pure citizens are a separate **fused fast
   path** (Direct-carried), pinned **extensionally equal** to the `M = Id` driver by
   law — agreement, not architectural identity.
   `Project`/`Embed` stop being the API surface: the driver takes **layer optics as
   arguments** (a `Basis` is one *constructor* of a layer optic, an effectful
   `S => M[F[S]]` is another, a circe/Plated layer a third).

The motivating symmetry stands from v1: every fold-side enrichment is comonadic =
product-shaped, every unfold-side enrichment is monadic = sum-shaped, and eo already
owns both shapes as carriers:

| scheme | decoration (as a `Decor` value) | shape | W |
|---|---|---|---|
| cata / ana | `Decor.id` | — | `A` |
| **para** | `Decor.para` — child slots carry original subterms | product | `(S, A)` |
| **apo** | `Decor.apo` — child slots may graft a finished subtree | sum | `Either[S, A]` |
| **histo** | `Decor.histo` — full decorated history per child | iterated product | `Attr[F, A]` |
| **futu** | `Decor.futu` — multiple layers per step | iterated sum | `Coattr[F, A]` |
| zygo / dyna / chrono | user-written `Decor` values | — | user's `W` |
| elgot / coelgot / micro | **follow-up** (answer-level sum, `Either[B, F[A]]` outside the layer) | sum | `Either[B, _]` outside `F` |

"BiAffine" is the carrier this family wears: `Affine`'s data shape where the
miss-branch is a *successful* outcome on the build seam (`Done` = "finished, graft
as-is"), with its own laws (`graft(Done(t)) == t`). Literature check (2026-06-11):
the adjacent cells are named — coalgebraic prism (Clarke et al. 2024, Rem. 3.19),
achromatic lens (Riley §4.10), partial isos (Rendel–Ostermann), fold/unfold lenses
(Pacheco–Cunha) — but **the decoration-as-build-affine-optic cell is unpublished**.
droste has the shape without the name (`Gather`/`Scatter`); eo names it and pins it
with laws.

## Decisions (settled 2026-06-11, interactive; v2 revisions marked)

1. **Scope** — recursion schemes only. The failure-typed-build spike
   (`docs/brainstorms/2026-06-10-failure-typed-build-biaffine.md`) stays separate.
2. **Encoding** — ~~citizens first, carrier later~~ **(v2)** the `BiAffine` carrier
   and the `Decor` optic family are the *foundation*, since the zoo's surface is
   built from them.
3. **Zoo scope v1** — para + apo + histo + futu, as named `Decor` values.
4. **Engine** — typed pattern-functor path only. ~~Untyped `Plated`/`PSVec` unchanged~~ **(superseded 2026-06-12, user decision post-review: the untyped `cata`/`ana`/`hylo` were REMOVED — the typed path subsumes them; core `Plated` itself is untouched).**
5. **Decorations** — hand-rolled `Attr`/`Coattr` (droste's model, no cats-free dep).
6. **paraF is explicit** — a named member, not a documentation note.
7. ~~Public free-range `gcataF`/`ganaF`~~ **(v2) dropped.** The generality lives in
   the public `Decor` family instead: zygo/dyna/chrono are user-*written* `Decor`
   values, not user-called generic methods. Squint test: the gather/scatter pair was
   always an optic; now it is one.
8. **Sequencing** — grow PR #24. Rationale (recorded post-review): stage 3
   re-derives #24's own unmerged `cataF`/`anaF`, so one PR avoids publishing a
   transient API and re-reviewing the same lines twice; the staging seams below
   remain the split points if review load demands it. Split trigger: if review
   needs a second full round-trip (or the diff crosses ~3k added lines), stages
   1–4 split out as the foundation PR — author's call at the end of stage 4.
9. **(v2, refined in review) Schemes are optic values with fusion composition** —
   pure path: `anaF.cross(cataF)` fuses to hylo (`cross` is core's name for the
   build-output→read-input seam); M path: `AnaFM.andThen(CataFM)` fuses (there it
   is the focus seam). Collapsing to bare `DirectGetter`/`Review` at construction
   is a no-go.
10. **(v2) M-generic driver in v1** — effectful results are `Forget[M]`-carried
    citizens; the pure citizens are the Direct-carried fast path, law-pinned
    extensionally equal to `M = Id`.
11. **(v2) elgot/coelgot/micro deferred** to a follow-up that also completes the arbo
    `Calculator.selection` port. v1's acceptance example is arbo-*shaped* (effectful
    children in `M`) but uses cata/ana/hylo decorations only.

## Problem Frame

PR #24's typed path has exactly three schemes, all undecorated, all pure, all
collapsing to `DirectGetter`/`Review` at construction — which erases precisely the
structure a real consumer needs. The reference real-world case
(`~/workspace/crypto/arbo/src/main/scala/arbo/Calculator.scala` +
`arbo/elgot/package.scala`) had to hand-roll its scheme family on droste's kernel:
an *effectful* coalgebra (`A => M[Either[B, F[A]]]`, children fetched via
`GetSellOptions[M, O]`), answer-level short-circuit, fused execution, result
`A => M[B]`. Nothing in eo (or droste's public zoo) offers: decorated schemes that
**compose as optics**, an **M-generic** driver, or **fusion by composition**
(`ana.cross(cata) == hylo`). droste's basic schemes are also stack-unsafe and its
`gana`-apo re-walks grafts in O(graft).

The gap: the zoo as *composable optic values* — decorations, algebras (`Unfold`,
#26), layers (`fLayer`, #24), and assembled schemes all citizens of one algebra —
stack-safe, M-generic, with O(1) graft and hylo-fusion as measurable, law-pinned
differentiators.

## Design

### D1. The `BiAffine` carrier + the `Decor` optic family (foundation)

```scala
/** Affine's data shape worn on the BUILD seam: Done = "the engine does not call the
  * coalgebra for this slot". Its payload's meaning is pinned PER VALUE via X:
  * apo's Done carries a finished subtree S (prefill the slot, O(1) graft);
  * futu's Done carries a prebuilt layer F[W] (unroll it, still no coalgebra call). */
enum BiAffine[X, +A]:
  case Step(snd: Snd[X], a: A)   // keep going from a (one-F-layer context alongside)
  case Done(fst: Fst[X])         // no coalgebra call — payload interpreted per value
```

A **decoration** is an optic of this carrier whose existential leftover is one
F-layer. The family has **typed sub-shapes** — sides are pinned in the *type*,
eo-style, per the read-only-optics convention (pass-2 resolution):

```scala
/** scatter = to:   W => Step(layerCtx, focus) | Done(payload)  — affine match
  * gather  = from: Step(layerCtx, result) => W                 — product build  */
type DecorGather [F[_], W, A] = Optic[Unit, W, Unit, A, BiAffine] // fold side: gather-only
type DecorScatter[F[_], W, A] = Optic[W, Unit, A, Unit, BiAffine] // unfold side: scatter-only
type Decor       [F[_], W, A] = Optic[W, W, A, A, BiAffine]       // full citizen (both halves)
// X a Tuple2 per value: Snd[X] = F[W] uniform; Fst[X] = the Done payload
// (apo: S, futu: F[W])
```

para/histo are `DecorGather` values; apo/futu are `DecorScatter` values; a member
carrying both halves is a full `Decor` (the apo+para composite, when someone needs
it). The fold-family constructors accept `DecorGather`, the unfold-family
`DecorScatter` — each driver takes exactly the half it consumes. This is the honest
version of droste's separate `Gather`/`Scatter` types, inside one family.

Named values: `Decor.id` (W = A), `Decor.para[F, S]` (W = (S, A)),
`Decor.apo[F, S]` (W = Either[S, A]), `Decor.histo[F, A]` (W = Attr[F, A]),
`Decor.futu[F, A]` (W = Coattr[F, A]). zygo/dyna/chrono: user-written values, one
shown in the docs.

- **No `Optic` trait change** (the `fLayer`/#26 standard). Capabilities: a
  `Graft`-style reverse accessor consuming the `Done` channel, instantiated per
  value (graft-finished for apo, unroll-layer for futu).
- **Laws (bind the inhabited side only):** `graft(Done(t)) == t` (Done is final —
  scatter-side members); gather/scatter round-trip where both sides exist; the
  fast-path agreement laws live in D5. The `Done`/`Step`-coherence-across-`andThen`
  law **moves to the matrix-row follow-up** — it needs the
  `AssociativeFunctor[BiAffine]` instance this PR does not ship.
- **Composition-matrix row (11 → 12): follow-up PR**, not this one.

### D2. Decoration data: `Attr` / `Coattr` (`schemes/Decor.scala`)

```scala
final case class Attr[F[_], A](head: A, tail: F[Attr[F, A]])   // cofree, no laziness
enum Coattr[F[_], A]:
  case Pure(a: A)                                              // free, no suspension
  case Roll(layer: F[Coattr[F, A]])
```

Minimal API; no `Eval` fields. Space honesty: histo is inherently O(n) decorations —
documented, not hidden.

### D3. Scheme citizens: concrete optic classes, fusion by `andThen`, M-generic

Constructors keep their names; what they *return* changes — concrete classes that
carry their parts so composition can fuse:

```scala
// Pure path, still named cataF/anaF/hyloF. DirectGetter/Review are FINAL in core
// (perf-pinned encoding), so the citizens extend the open Optic TRAIT directly —
// zero core changes, full generic composition via the trait members:
final class CataF[F[_], S, A](layer: …, decor: …, alg: …)
    extends Optic[S, Unit, A, Unit, Direct]      // Getter-shaped, carries its parts
final class AnaF[F[_], Seed, S](layer: …, decor: …, coalg: …)
    extends Optic[Unit, S, Unit, Seed, Direct]   // Review-shaped

// THE fusion seam — cross, core's own name for build-output→read-input composition
// (Optic.cross scaladoc: "the motivating case is ana.cross(cata)"):
//   anaF(coalg).cross(cataF(alg)) : DirectGetter[Seed, A]   — fused, no S built
// as a fused overload on the concrete classes (the valdef-encoding memory says
// exactly this seam regresses 3x if left generic). Widening hazard, documented:
// binding anaF(…) to a wider type loses the fused overload — hyloF(coalg, alg)
// stays as the always-fused spelling.

// Effectful path: concrete carrying classes here too — the fused overload cannot
// live on the erased trait type (overloads resolve on concrete classes):
final class CataFM[M[_], F[_], S, A](…)    // upcasts to Optic[S, Unit, A, Unit, Forget[M]]
final class AnaFM [M[_], F[_], Seed, S](…) // upcasts to Optic[Seed, Unit, S, Unit, Forget[M]]
def cataFM[M[_]: Monad, F[_], S, A](layerM: S => M[F[S]], …): CataFM[M, F, S, A]
def anaFM [M[_]: Monad, F[_], Seed, S](coalgM: Seed => M[F[Seed]], …): AnaFM[M, F, Seed, S]
// AnaFM.andThen(CataFM) fuses via the concrete classes — here andThen is the
// genuine focus seam (Forget[M] Kleisli) — the arbo execution shape (Seed => M[B]).
//
// Consumption: effect Ms (IO, …) have no Foldable, so the Foldable-gated Fold ops
// (.foldMap/.headOption) and ReadCompose cells do NOT apply. The concrete classes
// expose the run surface directly as the public consumption op:
//   CataFM.run: S => M[A]        AnaFM.run: Seed => M[S]      (not raw .to)
// v1 composition scope for Forget[M] citizens: same-carrier andThen
// (assocForgetMonad) + run. An Accessor-into-M capability is follow-up material.
//
// hyloFM(coalgM, algM) is the always-fused M spelling (what D6's eoHyloM row runs).
// Same widening hazard as the pure path: a widened AnaFM still typechecks through
// the generic trait andThen (assocForgetMonad) — extensionally equal but
// MATERIALIZING (M[S] built, then folded). The M fusion law pins the
// concrete-typed spelling only.
```

- **Layers are arguments, not implicits.** `Basis`/`Project`/`Embed` become
  *constructors* of layer optics (`fLayer` et al.), with overloads defaulting to the
  `Basis`-derived layer so the common case stays terse. An effectful layer
  (`S => M[F[S]]`) and an integration layer (circe/Plated) plug into the same slot —
  this is the "richer usage surface": compose into the layer seam before recursing.
- **Engines:** the pure citizens (`cataF`/`anaF`/`hyloF`) keep the `< 512`-on-stack
  / `ArrayDeque` hybrid untouched. `CataFM`/`AnaFM`/`hyloFM` **always run the
  foldLayered state machine lifted into M — no `M = Id` special-case** (that is
  what makes D5's agreement law a real cross-architecture pin): state = the
  explicit frame deque, threaded through `Monad[M].tailRecM`, one iteration per
  node event — each paying tailRecM's per-step `Either`, the structural B/op floor
  vs the pure machine (acknowledged; benched). NOT droste's `hyloM`
  (flatMap-recursive — O(depth) call stack on a strict `M`, the shape this plan
  elsewhere criticizes). Stack-safety thus reduces to the lawfulness of M's
  `tailRecM` — **per-M and tested, not asserted**: `Id`/`Eval` to 10⁶
  (`CataFM[Eval]`/`AnaFM[Eval]`). **Supported Ms are single-pass and linear** —
  the lifted machine threads mutable state (the frame deque, in-place child
  arrays), so a branching/replaying `M` (`List`, retrying or streaming effects)
  would share that state across branches and corrupt the fold. The contract is
  stated in scaladoc + docs; a persistent-state variant is deferred until a real
  consumer needs it.
- **Inlining discipline:** concrete classes host no shared per-instance `Function1`
  dispatch (use-site-friendly encoding rules); PrintInlining check on the fused hot
  path AND the `Decor.id`-routed cata path before merging.

### D4. The named zoo (decoration values + native engine routes)

- **para** (`Decor.para`): the machine walks real `S` nodes and keeps each frame's
  projected layer, so child slots pair subterms positionally **without droste's
  per-node re-`embed`**. Bench-pin.
- **apo** (`Decor.apo`): **native O(1) graft.** `Done(s)` slots are already-finished
  results — prefill, never recurse, never project. A `foldLayered` sibling
  (`foldLayeredOr`) consumes the BiAffine slot decision directly. droste's
  scatter-apo re-walks grafts through `project`; this is the measurable claim.
- **histo / futu** (`Decor.histo` / `Decor.futu`): definitional — the proof the
  `Decor` family is correctly shaped. Engine stores `Attr`/`Coattr` in the result
  slots.
- Fold-side algebras stay node-supplied (`(S, …) => A`), matching `cataF`.
- **User-written `Decor` values** (zygo/dyna/chrono): the same public constructors
  accept any `Decor` value. Named values dispatch to their native engine routes
  (identity match); user values run the **generic decoration route**, which pays the
  per-node decoration dispatch the native routes avoid — documented honestly, with
  a generic-route bench row (D6) as the honesty number.

### D5. Laws & tests (`SchemesFLawsSpec` / `SchemesFSpec` extensions)

- **Fusion law (new, central):** `anaF(c).cross(cataF(a)) == hyloF(c, a)`, stated
  with the side condition the branch's own hyloF scaladoc records: unconditional
  for *pure* algebras (node argument ignored); for node-reading algebras it holds
  under the seed↔`embed(coalg(seed))` correspondence — both forms pinned. And
  **builds no intermediate `S`** (allocation-pinned via the gc profiler in CI,
  B/op). M path: concrete-typed `anaFM(c).andThen(cataFM(a)) == hyloFM(c, a)`,
  allocation-pinned — the widened trait-`andThen` spelling is extensionally equal
  but materializing, explicitly outside the pin.
- **Degeneration laws:** para ignoring subterms == cata; never-grafting apo == ana;
  heads-only histo == cata; single-layer futu == ana.
- **Fast-path agreement laws:** `cataFM[Id](…).run == cataF(…).get` (and ana/hylo
  likewise) — extensional equality between the Direct-carried fast path and the
  `M = Id` driver, per decision 10.
- *(Spec roles: `SchemesFLawsSpec` hosts the law properties above;
  `SchemesFSpec` the engine/behaviour tests — stack-safety, graft identity, the
  acceptance example.)*
- **Decoration optic laws:** scatter/gather round-trip per named `Decor` value;
  `graft(Done(t)) == t`.
- **O(1) graft observable:** grafted subtree present **by reference** (`eq`) in the
  result — the law-shaped perf claim.
- **Stack-safety to 10⁶ tested per driver** (pure machine; the lifted machine via
  `CataFM[Eval]`/`AnaFM[Eval]`), deep `Coattr` chains included; histo's O(n) space
  documented.
- **Generic-route correctness:** the D7 zygo (a user-written `Decor`) run through
  the generic decoration route, pinned against a hand-rolled zygo — the route's
  correctness criterion, not just its D6 dispatch-cost number.
- **Linear-M contract:** one test documents a non-linear `M` (`List`) as
  unsupported — the mutable-state machine's stated boundary, exercised rather
  than implied.
- **Acceptance example (arbo-shaped):** an effectful hylo over a sell-tree-like
  fixture whose children arrive in `M` (`GetSellOptions` analogue), result
  `Seed => M[B]`, composed via `anaFM.andThen(cataFM)`. The full
  `Calculator.selection` port (needs elgot) is the follow-up's acceptance test.

### D6. Benchmarks (`SchemesBench` additions)

Paired vs droste on the existing fixtures, B/op primary: `eoParaF`/`dPara`,
`eoApoF`/`dApo`, `eoHistoF`/`dHisto`, `eoFutuF`/`dFutu`, plus `eoHyloM`/`dHyloM`
(effectful driver overhead; pin: `eoHyloM` B/op ≤ `dHyloM` — the per-node-event
`Either` floor is the expected cost, not an excuse) and a fused-vs-materialized
pair pinning the fusion law's allocation claim. Headline pins: para ≤ droste B/op (no re-embed); **apo B/op
independent of graft size** (droste linear); histo/futu parity-or-better; ana's known
2.4× B/op gap (CI 2026-06-11) not worsened by the new surface; **`cataF`/`hyloF`
before/after pin for the `Decor.id` re-derivation** (B/op equal and CI ns within noise
vs the pre-stage-3 baseline — the regression this refactor is most likely to cause);
a **generic-route row** for one user-written `Decor` value (D4's dispatch-cost honesty
number). Pin classes: **merge gates** — the `cataF`/`hyloF` before/after pin, the
ana-gap-not-worsened pin, the fusion no-intermediate-`S` pin; **docs-claim gates** —
para ≤ droste and apo graft-independence (each held to its verification item);
**recorded, not gated** — histo/futu (definitional members, not differentiators).
Two verification items before headlines go in docs: **confirm droste's apo
actually re-walks grafts on the fixtures** (if not, the O(1)-graft claim adjusts to
absolute numbers), and record histo's **peak retained decorations** (analytic count
from the fixture confirmed by one heap-histogram run outside JMH; the number lands
in the docs' space-honesty note).

### D7. Docs (`site/docs/schemes.md`)

- Zoo section anchored on the symmetry table; schemes-as-values and fusion-as-`cross`
  shown first (the story IS the surface now). Claims scoped to the shipped seams
  (cross fusion, M-path `andThen`, layer arguments) — general matrix citizenship
  waits for the follow-up row.
- BiAffine narrative: *the decoration optic* — the unpublished cell, adjacent named
  cells cited honestly.
- apo example with teeth: patch-one-subtree-keep-the-rest, O(1) graft visible.
- **zygo written by hand as a `Decor` value** — the proof the family replaces the
  dropped free-range generics.
- Effectful example: the arbo-shaped fetch-children-in-M hylo.

## Staging (commits on `feat/typed-recursion-schemes`)

0. **Rebase `feat/typed-recursion-schemes` onto `main`** — the plan leans on
   main-only artifacts the branch predates (Unfold #26's fused-overload precedent,
   `ReadCompose`, the accessor/forgetful/compose package split, the def-based
   `to`/`from` encoding). Re-run the typed-schemes bench after rebasing to confirm
   the `foldLayered` numbers survive; reconcile class names used below against
   post-rebase core.
1. `BiAffine` carrier + `Graft` capability + carrier laws (core + laws).
2. `Attr`/`Coattr` + unit tests.
3. `Decor` family + named values (id/para/apo/histo/futu) + decoration laws;
   `cataF`/`anaF` re-derived through `Decor.id` — rewritten in place, same public
   API (behaviour-identical, tests prove it).
4. Scheme citizens (`CataF`/`AnaF` classes) + **fused `cross` overload** (pure path;
   the M-path fused `andThen` lands in stage 5) + fusion laws; `foldLayeredOr`
   (O(1) graft) + zoo degeneration laws + stack-safety sweep. Overload-set
   discipline per `Getter`'s precedent: re-home trait overloads into the class
   where dotty would tie, and pin resolution with a matrix-spec-style ascription
   test (`anaF(c).cross(cataF(a))` resolves to the fused overload).
5. M-generic driver (`CataFM`/`AnaFM`, the tailRecM-lifted machine) + `run` surface +
   arbo-shaped acceptance example. **Pre-commit gate:** sketch the elgot
   `Decor`/driver seam against the v1 signatures (one page, brainstorm note) —
   turns decision 11's "no re-architecture" from assertion into check. Fail
   action: a public-signature change lands in this stage before the M-driver
   commits; if the sketch demands a new driver shape, decision 11 re-opens.
6. `SchemesBench` zoo + hyloM + fusion + generic-route rows; CI numbers (benchmarks
   workflow, `-prof gc`); PrintInlining runs per D3 (fused path + `Decor.id` cata path).
7. `site/docs/schemes.md` rewrite per D7; PR #24 description widened.

## Alternatives considered (rejected)

- **Free-range `gcataF`/`ganaF` methods** (v1 of this plan): rejected — the
  gather/scatter pair is an optic and the surface should say so; generality moves to
  the public `Decor` family.
- **`andThen`-spelled pure fusion** (v2 as first written): rejected in review — it
  forks `andThen` into two opposite seam semantics on one class; core already names
  the build-output→read-input seam `cross` (its scaladoc cites `ana.cross(cata)` as
  the motivating case).
- **Collapse to `DirectGetter`/`Review` at construction** (v1): rejected — erases the
  structure fusion and effectful composition need; concrete carrying classes instead.
- **`distApo` encoding** (apo via scatter over `Either[S, A]`): O(graft) re-walk;
  native `Done`-slot engine instead.
- **cats-free `Cofree`/`Free`**: dependency + `Eval` fields the machine never needs.
- **Failure-channel unification**: out of scope (decision 1).
- **Separate/stacked PR**: rejected (decision 8); #24 grows.

## Out of scope

- **elgot / coelgot / micro** (answer-level sum, `Either[B, F[A]]` *outside* the
  layer — arbo's exact decoration): explicit **follow-up**, whose acceptance test is
  the full arbo `Calculator.selection` port. The `Decor` family and M-driver land
  ready for it (the follow-up adds values + one driver seam, no re-architecture).
- Failure-typed build (2026-06-10 brainstorm) — separate spike.
- Untyped `Plated`/`PSVec` path — unchanged.
- Named zygo/dyna/chrono — user-written `Decor` values (one documented).
- BiAffine composition-matrix row (12-family matrix) — follow-up PR.
- Accessor-into-M capability for `Forget[M]` citizens — v1 ships same-carrier
  `andThen` + `run` only (D3).
- Persistent-state M-engine for non-linear Ms (`List`, replaying/streaming
  effects) — v1's lifted machine is single-pass linear by contract (D3).
- cats-free interop conversions.

## Open questions (to resolve during implementation)

- Exact `Decor` existential plumbing: the per-value X pinning (`Snd[X] = F[W]`
  uniform, `Fst[X]` per member) as construction-site refinement vs a
  type-member-refined family — surface at the spike, as `fLayer` did in #24.
- `foldLayeredOr` vs parameterizing `foldLayered` — whichever keeps the hot cata
  path's inlining intact (PrintInlining check).
- How terse the common case stays once layers are arguments (default-overload
  ergonomics: `cataF(alg)` with a `Basis` in scope must remain one call).
- Whether `Attr` wants a `Plated` instance — defer unless a test wants it.

## References

- arbo (`~/workspace/crypto/arbo`): `Calculator.scala`, `elgot/package.scala` — the
  real-world consumer this design must serve (`ElgotCoalgebraM`, `elgotM`, `micro`).
- droste `algebras.scala` / `kernel.scala` (Gather/Scatter, `hyloM`).
- Uustalu–Vene–Pardo, *Recursion schemes from comonads* (2001); Hinze–Wu–Gibbons,
  *Unifying structured recursion schemes* (ICFP 2013).
- Clarke et al., *Profunctor Optics: a Categorical Update* (2024) — coalgebraic prism.
- Riley, *Categories of Optics*; §4.10 achromatic variant.
- Yang–Wu, *Fantastic Morphisms and Where to Find Them* (arXiv 2202.13633).
- Kmett, recursion-schemes (`distPara`/`distApo`); *Elgot (Co)Algebras* (2008) —
  answer-level vs subtree-level affine, now the explicit v1/follow-up boundary.
