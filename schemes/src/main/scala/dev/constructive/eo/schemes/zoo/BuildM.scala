package dev.constructive.eo
package schemes
package zoo

/** Monadic-build citizen — the **effectful** unfold schemes ([[BuildScheme]] at carrier `M[S]`): an
  * unfold whose coalgebra returns its layer in `M` and whose effects are sequenced through the
  * construction. Builds `B => M[S]` (`.reverseGet` yields `M[S]`).
  *
  * The build-side mirror of [[FoldM]]: one class carries the whole `M`-unfold zoo —
  * [[Schemes.anaM]] / [[Schemes.apoM]] / [[Schemes.futuM]] — differing only by the expand handed to
  * the shared engine. The residual index rides the phantom `XI` (`anaM` is `BuildM[…, S]`, `apoM`
  * is `BuildM[…, Either[S, A]]`, `futuM` is `BuildM[…, Coattr[F, A]]`), preserving the monad-tower
  * index the same way [[FoldM]] preserves the comonad-tower one.
  *
  * Runs on [[Machines.foldLayeredM]] under the same single-pass / linear / sequential `M` contract
  * as [[FoldM]].
  *
  * @tparam XI
  *   the residual index this unfold threads — the optic existential `X`, carried as a type
  *   parameter so one class spans the whole build-side `M`-zoo.
  */
final class BuildM[S, B, M[_], XI] private[zoo] (run: B => M[S]) extends BuildScheme[M[S], B]:
  type X = XI
  protected def write(b: B): M[S] = run(b)

object BuildM:

  /** Wrap an already-wired effectful unfold `B => M[S]`. The engine plumbing lives in the
    * [[Schemes]] `*M` factories (each picks the expand and pins `XI`).
    */
  private[schemes] def apply[S, B, M[_], XI](run: B => M[S]): BuildM[S, B, M, XI] =
    new BuildM[S, B, M, XI](run)
