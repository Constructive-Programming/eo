package dev.constructive.eo
package schemes
package zoo

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
