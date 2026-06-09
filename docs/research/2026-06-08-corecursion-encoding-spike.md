---
date: 2026-06-08
topic: corecursion-encoding-spike
status: complete
plan: docs/plans/2026-06-08-001-feat-recursion-schemes-generation-spike-plan.md
verdict: BUILD (if pursued) — encoding-A (closure) primary; B is a thin opt-in typed layer over A
real-world-reference: ~/workspace/crypto/arbo (production recursion-scheme usage)
---

# Corecursion (generate) encoding spike — Stage-0 verdict (corrected)

**A prior draft of this verdict said "do not build" on the grounds that eo's closure
encoding is "pure-only" and can't reach the real (effectful) use case. That was wrong —
asserted from the plan's scope, not tested. Corrected here with a passing demonstrator.**

## What was built (Stage 0)

A new un-aggregated `spike` sbt module (deps: core, circeIntegration, generics, droste
`0.9.0-M3`; specs2/scalacheck in Test). **Droste co-compiles cleanly** against cats 2.13.0
/ Scala 3.8.3 (pins cats 2.6.1 / scala3-lib 3.0.0 transitively, evicted up — no issue).

1. A pure seed→`Json` unfold, three ways (`DomainUnfold.scala`, `Stage0GateSpec` green).
2. An **arbo-shaped effectful** unfold→fold — fused, fail-able — three ways
   (`EffectfulDemo.scala`, `EffectfulDemoSpec` green): droste `hyloM`, eo closure `hyloM`,
   hand-written. Computes a Fibonacci-style sum over a tree whose expansion is an
   effectful, fail-able "options" lookup (`M = Either[String, *]`; seed 13 is blocked).

## Stage 1: stack-safety — the win over a hand-written one-off

`Schemes.hylo` (a synchronous `ArrayDeque` post-order machine, mirroring
`Plated.transformMachine` — every node pushed as a frame, never recursed on the JVM stack)
**completes a depth-10⁶ unfold**, while the naive hand-written recursion `hyloNaive`
`StackOverflow`s at that depth (`StackSafetySpec`, 3 green). This is the thing a hand-written
one-off cannot give you for free, and the concrete justification for a reusable engine.
(The synchronous machine suffices because no generation scheme has a fixpoint axis — see
the plan's corrected engine decision.)

### Stack-safe AND effectful together (the full arbo shape) — proven

`Schemes.hyloEither` threads a **fail-able effect** (`Either[E, *]`, arbo's `M` shape) through
the same heap machine via a `Left`-abort (`EffectfulStackSafetySpec`, 4 green): a depth-10⁶
effectful unfold completes where the naive effectful recursion overflows, **and** a `Left`
injected 500k levels deep is threaded correctly — which only works if the machine reaches
that depth stack-safely. So the two properties hold *together*, on the exact shape arbo runs
in production (effectful, fused, fail-able, deep).

### General monad — last gap vs droste closed (and exceeded)

`Schemes.hyloM` threads an **arbitrary lawful `Monad[M]`** stack-safely by driving the
heap-machine descent through `Monad`'s law-required stack-safe `tailRecM` — the JVM call
stack is never used for descent. `GeneralEffectSpec` (4 green) runs the *same* generic
`hyloM` over two unrelated monads, `Eval` and `Either`, both at depth 10⁶ (success + deep
failure). This is a **stronger** guarantee than droste's `hyloM`, which is stack-safe only
when `M` itself is — eo's is stack-safe for *any* lawful monad. The earlier "pure-only" /
"reinventing droste" framing is fully retracted: eo's encoding reaches the full effectful,
stack-safe generality and then some, with no pattern functor.

## The correction: eo does effectful generation

`EffectfulDemoSpec` proves eo's closure encoding agrees with both droste's `hyloM` and the
hand-written recursion on **both** the success path (fib sums) and the failure path (the
blocked seed short-circuits to `Left` identically). eo threads the effect exactly as the
existing `Plated.rewrite` does (`plate.modifyA[Eval](go)`) — a monadic `traverse` over the
child seeds. **The effectful case is no obstacle to eo's encoding.** The earlier claim that
eo was "pure-only" confused *the spike plan's scope* (it only listed pure `ana`/`apo`/`hylo`)
with *eo's capability*.

## Ceremony on the real (effectful) shape

| Implementation | Setup ceremony | Effect? | Node payload in fold? |
|---|---|---|---|
| eo closure `hyloM` | ~6-line helper + ~6-line call site; **no pattern functor, no `Functor`/`Traverse`** | yes | via closure capture |
| droste `hyloM` | `GameF` + `Functor` + **`Traverse`** (mandatory for monadic) + `AlgebraM` + `CoalgebraM` ≈ 26 lines, 3 typeclasses | yes | typed `Algebra` |
| hand-written | ~5 lines (cats `traverse` threads `M`) | yes | direct |

On the effectful shape — the one arbo actually uses — **eo's closure encoding has the
lowest library ceremony of the two abstractions**: it needs no pattern functor and no
`Traverse` instance (which droste *requires* for the monadic scheme), while still threading
the effect. Node payloads a typed fold needs (arbo's `case SellNode(order, …)`) are
available by closure capture, and eo's `cata` additionally passes the original node
(`(S, PSVec[A]) => A`, paramorphism-flavored).

## Stage 2: A vs B — settled

Encoding B (typed descriptor) was built on the same effectful example (`EncodingBSpec`,
3 green) and **desugars to encoding A** through a `Traverse[F]` adapter (`Schemes.hyloM_B`):
children are `F.toList`, and the typed algebra's `F[A]` input is the original `F[Seed]`
shape positionally re-filled with folded results. Consequences, all code-backed:

- **B inherits A's stack-safety + monad-generality for free** (it *is* A underneath) —
  verified stack-safe at depth 10⁶.
- **B's only delta over A:** define the pattern functor `F` + its `Functor`/`Traverse`
  (~12 lines for `GameF`), buying a *typed, named* algebra `F[A] => A` (arbo's
  `case SellNode(order, …)` style). **A's delta:** positional `PSVec[A]` children with node
  payload via closure capture, zero new types.
- So it is **not** an either/or: **A is the primitive; B is a thin opt-in typed layer on
  top of A.** You implement A once (the stack-safe `tailRecM` machine) and offer B as a
  `Traverse`-based convenience for callers who want typed folds.

Crucially, **B is essentially droste**: a typed pattern functor + `Traverse` + typed
`Algebra`. Building B in eo is largely reimplementing droste; its only edge is riding A's
stronger stack-safety. **A is the *only* encoding that justifies eo generation existing
separately from droste** — it is the pattern-functor-free, least-ceremony, eo-native primitive.

## Schemes as eo optics — the connection that makes them *eo's*

An interim version of the engine (`Schemes.hylo`/`ana`/`cata` as free functions) was a fair
criticism: *less* general than droste **and** disconnected from eo — touching no `Optic`,
`Plated`, or carrier. That is the worst of both worlds and abandons the only reason to put
schemes in an optics library. Fixed by expressing the schemes **as optics** (`Optics.scala`,
`OpticsSpec` 4 green):

- **`cata` is a `Getter`** — a real `Optic[S, Unit, A, Unit, Direct]` — **driven by a
  generics-derived `Plated[S]`** (`plate[Expr]`). It reads children through `Plated.childrenVec`
  (so it generalises `Plated.transform` from `S=>S` to `S=>A`) and returns an eo optic.
- **`ana` is a `Review` that is itself an `Optic`** — `ReviewOptic[S, A] extends
  Optic[Unit, S, Unit, A, Direct]`, the exact **mirror of `Getter`** (`Optic[S, Unit, A,
  Unit, Direct]`): `Getter` has a real `to` (read `S=>A`) + vestigial `from`; `Review` has a
  vestigial `to` (reads `Unit`) + a real `from` that *builds* `S` from focus `A`. Confirmed
  empirically (`OpticsSpec`): it ascribes to `Optic[…]`, its `to`/`from` run, and it composes
  via `andThen`. **Note:** eo's *core* `Review` is a bare case class that deliberately doesn't
  extend `Optic` ("pure Review has no `to`") — but with source `Unit` its `to` is exactly as
  vestigial as `Getter`'s `from`, so that is an inconsistency with `Getter`, not a necessity.
  If generation graduates, core `Review` should be reworked to extend `Optic` (the mirror of
  `DirectGetter`), unifying the read/build optic pair.
- **They compose.** `outerGetter.andThen(cata(alg))` reads through an optic to a recursive
  structure and folds it in one composed `Getter` (verified: `Wrapped --Getter--> Expr
  --cata--> Double`); and `ana(...).andThen(ReviewOptic(...))` composes builds the dual way.
  **droste's free-standing schemes cannot do this.**

This reframes the droste comparison honestly: eo's generality is **compositional** (schemes
join the whole optic algebra — `andThen` with lenses/prisms/folds), which is a *different
axis* from droste's **pattern-functor** generality (the full zoo over any `Functor`). eo
trades the latter for the former — and the former is the entire point of eo. So "lacks
droste's generality" is true on the zoo axis and beside the point on the axis eo exists for.

## Verdict

**If eo adds a generation surface, encoding A (closure) is primary; B is an optional
`Traverse`-adapter layer.** All capability concerns are resolved (effectful, fused,
short-circuit, stack-safe for any lawful monad, lower ceremony than droste), so there is no
capability reason not to build it. The remaining decision is purely product appetite: eo's
distinctive value is **A** (no pattern functor + stack-safety + composes with the optic
surface + no droste dep). For callers who specifically want typed/named folds, B (≈ droste)
or droste-interop are equivalent — so scope B as a thin convenience, not a second engine.

The earlier "do not build" was wrong and is fully retracted: it rested on a capability
claim ("eo is pure-only") that was asserted, not tested, and is false.

## Lesson

Stage-0's *value* was real (cheap, isolated, caught the droste co-compile question), but the
verdict it produced was wrong because one axis — capability — was *asserted* ("eo is
pure-only") rather than *tested*. The fix was ~40 lines of demonstrator. Test the claim.

## Reproduction / references

- `sbt spike/testOnly dev.constructive.eo.spike.EffectfulDemoSpec` — eo == droste == hand,
  success and failure paths.
- `sbt spike/testOnly dev.constructive.eo.spike.Stage0GateSpec` — pure three-way agreement.
- `spike/.../EffectfulDemo.scala`, `spike/.../DomainUnfold.scala` — the implementations.
- Real-world reference: `~/workspace/crypto/arbo` — `Calculator.selection` (`elgotM`),
  `data/SellTree.scala`, `elgot/package.scala`.
