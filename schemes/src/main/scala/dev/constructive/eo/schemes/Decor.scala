package dev.constructive.eo
package schemes

/** Decoration data for the typed recursion-scheme zoo (histo / futu) — hand-rolled, droste-style,
  * rather than cats-free: no `Eval` suspension fields (the `foldLayered` machine never suspends),
  * no dependency, shapes tuned to the engine.
  *
  * Space honesty: a histomorphism decorates **every** node with its full sub-result history, so a
  * fold over n nodes retains O(n) [[Attr]] cells until the algebra releases them. That is inherent
  * to course-of-value recursion, not an engine artifact.
  */

/** Cofree-without-laziness: a fold result (`head`) decorating one layer of already-decorated
  * children (`tail`). The histomorphism's algebra sees `F[Attr[F, A]]` — each child's result *plus*
  * that child's entire decorated history.
  *
  * @tparam F
  *   the pattern functor
  * @tparam A
  *   the fold result decorating each node
  */
final case class Attr[F[_], A](head: A, tail: F[Attr[F, A]])

object Attr:

  /** Discard the history, keep the top result — `histo`'s final projection. */
  def forget[F[_], A](attr: Attr[F, A]): A = attr.head

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
