package dev.constructive.eo
package schemes

import cats.{Monad, Traverse}

import data.{Forget, ForgetK}
import optics.Optic

/** Effectful scheme citizens — the M-generic drivers' return types. Computational steps evolve in a
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
  * below requires the concrete types.
  */

/** Generic effectful-fold citizen: `run: S => M[A]`. What `hyloM` and the fused
  * `AnaM.andThen(CataM)` return.
  */
sealed class FoldM[M[_], S, A] private[schemes] (val run: S => M[A])
    extends Optic[S, Unit, A, Unit, Forget[M]]:
  type X = Nothing

  def to(s: S): Forget[M][X, A] = ForgetK(run(s))
  def from(d: Forget[M][X, Unit]): Unit = ()

/** Effectful fold-scheme citizen: carries its algebra for fusion. */
final class CataM[M[_], F[_], S, A] private[schemes] (
    run: S => M[A],
    private[schemes] val algM: (S, F[A]) => M[A],
) extends FoldM[M, S, A](run)

/** Effectful unfold-scheme citizen: `run: Seed => M[S]`, carrying the coalgebra + instances for
  * fusion.
  */
final class AnaM[M[_], F[_], Seed, S] private[schemes] (
    run: Seed => M[S],
    private[schemes] val coalgM: Seed => M[F[Seed]],
)(using
    private[schemes] val M: Monad[M],
    private[schemes] val F: Traverse[F],
    private[schemes] val E: Embed[F, S],
) extends FoldM[M, Seed, S](run):

  /** The fused M seam — here `andThen` genuinely is the focus seam (`Forget[M]` Kleisli). One
    * single-pass machine in `M` (the paired fold lifted through `tailRecM`): each node built once,
    * folded immediately — no `M[S]` materialization of the whole structure. Requires the concrete
    * types; widened operands fall back to the generic materializing `andThen`.
    */
  def andThen[A](inner: CataM[M, F, S, A]): FoldM[M, Seed, A] =
    val machine: Seed => M[(S, A)] =
      Schemes.fusedPairedFoldM(coalgM, inner.algM)(using M, F, E)
    new FoldM[M, Seed, A](seed => M.map(machine(seed))(_._2))
