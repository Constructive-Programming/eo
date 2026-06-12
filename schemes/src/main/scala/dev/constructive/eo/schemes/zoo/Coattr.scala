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
