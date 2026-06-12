package dev.constructive.eo
package schemes
package zoo

import cats.{Monad, Traverse}

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
      Machines.fusedPairedFoldM(coalgM, inner.algM)(using M, F, E)
    new FoldM[M, Seed, A](seed => M.map(machine(seed))(_._2))
