package dev.constructive.eo
package schemes
package zoo

import cats.Monad

/** Free-without-suspension: a futumorphism's coalgebra answers each slot with either a seed still
  * to expand ([[Coattr.Pure]]) or an already-known layer to unroll without consulting the coalgebra
  * again ([[Coattr.Roll]]) — the multi-layer-per-step channel.
  *
  * @tparam F
  *   the pattern functor
  * @tparam A
  *   the seed type
  */
enum Coattr[F[_], A]:

  /** A seed — the engine calls the coalgebra on it. */
  case Pure(a: A)

  /** A prebuilt layer — unrolled directly, no coalgebra call for this layer. */
  case Roll(layer: F[Coattr[F, A]])

object Coattr:

  /** The futumorphic expand step shared by `futu` / `chrono` / `codyna` / `metaChrono`: turn a
    * coalgebra `A => F[Coattr[F, A]]` into the engine's layer producer over `Coattr` — `Pure` calls
    * the coalgebra, `Roll` unrolls a prebuilt layer with no coalgebra call.
    */
  def expand[F[_], A](coalg: A => F[Coattr[F, A]]): Coattr[F, A] => F[Coattr[F, A]] =
    case Pure(a)     => coalg(a)
    case Roll(layer) => layer

  /** The effectful expand step shared by `futuM` / `chronoM` — the M-lifted [[expand]] worn in the
    * `Or`-shape [[Machines.foldLayeredM]] consumes (always `Right`; futu never grafts). `Pure` runs
    * the effectful coalgebra, `Roll` unrolls a prebuilt layer purely (`M.pure`).
    */
  def expandM[M[_], F[_], A, R](coalg: A => M[F[Coattr[F, A]]])(using
      M: Monad[M]
  ): Coattr[F, A] => M[Either[R, F[Coattr[F, A]]]] =
    case Pure(a)     => M.map(coalg(a))(Right(_))
    case Roll(layer) => M.pure(Right(layer))
