package dev.constructive.eo
package schemes
package zoo

/** Monadic-fold citizen — the **effectful** read schemes ([[ReadScheme]] at focus `M[A]`): a fold
  * whose algebra returns `M[A]` and whose effects are sequenced through the structure in
  * `Foldable` order. Reads `S => M[A]` (`.get` yields `M[A]`).
  *
  * One class carries the whole read-side `M`-zoo — [[Schemes.cataM]] / [[Schemes.paraM]] /
  * [[Schemes.histoM]] and the fused [[Schemes.hyloM]] / [[Schemes.chronoM]] — exactly as the pure
  * [[Cata]] / [[Para]] / … differ only by the combine they hand the shared engine. The recursion
  * index is **not** erased by the consolidation: it rides the phantom type parameter `XI`, so
  * `cataM` is `FoldM[…, Nothing]`, `paraM` is `FoldM[…, F[(S, A)]]`, etc. — the same `X`-as-index
  * thesis the pure zoo pins per class, here pinned per factory.
  *
  * All variants run on the single [[Machines.foldLayeredM]] engine (the `Monad[M]`-lifted walk,
  * `tailRecM`-driven and stack-safe). '''Contract:''' `M` must be a **single-pass, linear,
  * sequentially-evaluated** monad (`Id`, `Eval`, `State`, `IO`, …). A branching / replaying `M`
  * (`List`, retrying effects) shares the engine's mutable walk state across branches and corrupts
  * the fold — see [[Machines.foldLayeredM]]'s contract.
  *
  * @tparam XI
  *   the recursion index this fold retains — the optic existential `X`, carried as a type parameter
  *   so one class spans the whole read-side `M`-zoo without losing the index.
  */
final class FoldM[S, A, M[_], XI] private[zoo] (run: S => M[A]) extends ReadScheme[S, M[A]]:
  type X = XI
  protected def read(s: S): M[A] = run(s)

object FoldM:

  /** Wrap an already-wired effectful fold `S => M[A]`. The engine plumbing lives in the
    * [[Schemes]] `*M` factories (each picks the combine and pins `XI`); this just dresses the
    * resulting function as the optic citizen.
    */
  private[schemes] def apply[S, A, M[_], XI](run: S => M[A]): FoldM[S, A, M, XI] =
    new FoldM[S, A, M, XI](run)
