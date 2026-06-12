package dev.constructive.eo
package schemes
package zoo

import data.{Forget, ForgetK}
import optics.Optic

/** Generic effectful-fold citizen: `run: S => M[A]`. What `hyloM` and the fused
  * `AnaM.andThen(CataM)` return.
  *
  * Effectful scheme citizens — the M-generic drivers' return types. Computational steps evolve in a
  * `Monad[M]` (the arbo `Calculator` shape: fetching a node's children is effectful), and the
  * results are **`Forget[M]`-carried** optics: `S => M[A]` worn as `Optic[S, Unit, A, Unit,
  * Forget[M]]`, composing same-carrier through `assocForgetMonad`.
  *
  * Consumption: effect Ms (IO, State, …) have no `Foldable`, so the Foldable-gated Fold operations
  * (`.foldMap`/`.headOption`) and ReadCompose cells do NOT apply — the public consumption surface
  * is the stored [[FoldM.run]] (not raw `.to`). An Accessor-into-M capability is follow-up
  * material.
  *
  * Supported Ms are **single-pass and linear** — the lifted machine threads mutable state (the
  * frame deque, in-place child arrays), so a branching/replaying `M` (`List`, retrying or streaming
  * effects) would share that state across branches and corrupt the fold. See the linear-M boundary
  * test in `SchemesMSpec`. A persistent-state variant is deferred until a real consumer needs one.
  *
  * Re-forcing the same `M[A]` value returned by [[FoldM.run]] is safe — each force allocates its
  * own fresh mutable state (the frame deque is allocated inside the `M`, not before it). Concurrent
  * forcing of a single `M[A]` value remains unsupported; each `run(s)` call is independent.
  *
  * Widening hazard (the M-path mirror of `Ana.cross`'s): a widened `AnaM` still typechecks through
  * the generic trait `andThen` via `assocForgetMonad` — extensionally equal but MATERIALIZING
  * (`M[S]` built, then folded). `Schemes.hyloM` stays the always-fused M spelling; the fused member
  * on [[AnaM]] requires the concrete types.
  *
  * `FoldM` is `open` (not `sealed`) so that [[CataM]] and [[AnaM]] — which live in the `zoo`
  * subpackage — can extend it. Users may also wrap their own `S => M[A]` as a `FoldM` citizen; the
  * constructor is public.
  */
class FoldM[M[_], S, A](val run: S => M[A]) extends Optic[S, Unit, A, Unit, Forget[M]]:
  type X = Nothing

  def to(s: S): Forget[M][X, A] = ForgetK(run(s))
  def from(d: Forget[M][X, Unit]): Unit = ()
