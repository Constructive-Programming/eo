package dev.constructive.eo
package schemes
package zoo

/** Effectful fold-scheme citizen: carries its algebra for fusion. */
final class CataM[M[_], F[_], S, A] private[schemes] (
    run: S => M[A],
    private[schemes] val algM: (S, F[A]) => M[A],
) extends FoldM[M, S, A](run)
