---
date: 2026-06-12
topic: elgot-seam-sketch
spike: gate artifact (plan 2026-06-11-001, stage 5 pre-commit gate)
---

# Does elgot fit the v1 Decor/driver seam? (the decision-11 check)

One page, per the plan's stage-5 gate: sketch the elgot decoration against the v1
signatures BEFORE the M-driver lands. Fail action was: a public-signature change lands
in stage 5, or decision 11 re-opens.

## The shapes (arbo's `elgot/package.scala`)

```scala
elgot:  alg: F[B] => B,  coalg: A => Either[B, F[A]]          // answer-level short-circuit
elgotM: alg: F[B] => B,  coalgM: A => M[Either[B, F[A]]]      // the Calculator.selection shape
```

The `Either` sits **outside** the layer and carries an **answer** `B` — not a finished
structure (apo's `Done(S)`) and not a prebuilt layer (futu's `Done(F[W])`).

## Findings

1. **Elgot is a refold, not an unfold** — its driver seam is hylo-family (`Seed => B`,
   no `S` ever built), NOT `anaF`. So elgot never needed to fit `DecorScatter`'s
   `Done = F[W]` pinning: the follow-up adds a third sub-shape
   (`DecorElgot[F, W, A, B] = Optic[W, W, A, A, BiAffine] { type X = (B, Unit) }` —
   `Done` carries the ANSWER) plus one driver (`elgotF`/`elgotFM`). Additive; no v1
   alias or signature changes.

2. **The v1 M-machine adopts the Or-shape NOW** — `foldLayeredM`'s expand is
   `N => M[Either[R, F[N]]]` internally (the `foldLayeredOr` shape lifted into M).
   `hyloFM`/`anaFM`/`cataFM` always pass `Right`; the elgot follow-up (and an `apoFM`)
   merely supply `Left` answers. This is the one concrete "land ready for it" choice,
   and it costs v1 nothing (one constant `Right` wrapper per node event, folded into
   the `Either` the tailRecM step allocates anyway).

3. **`Forget[M]` citizenship is unaffected** — `elgotFM` returns the same
   `Seed => M[B]` fold shape (`FoldFM`) as `hyloFM`.

## Verdict

**PASS — no v1 public-signature change required.** Decision 11 stands: the follow-up
adds values (`DecorElgot`, `Decor.elgot`, `Decor.coelgot`) + one driver seam
(`elgotF`/`elgotFM`), with the full arbo `Calculator.selection` port as its acceptance
test. The only v1 accommodation is internal: `foldLayeredM`'s Or-shaped expand.
