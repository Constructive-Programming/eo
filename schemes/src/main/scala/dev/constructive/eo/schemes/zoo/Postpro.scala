package dev.constructive.eo
package schemes
package zoo

import cats.{~>, Traverse}

/** Postpromorphism citizen — an unfold that applies a **natural transformation `η : F ~> F` after
  * each step** ([[BuildScheme]]). The build-side mirror of [[Prepro]]: the
  * coalgebra `coalg: A => F[A]` is exactly [[Ana]]'s (so `X = S`, the structure it threads), but
  * each emitted subtree is hoisted through `η` once per level it sits below the root.
  *
  * Like [[Prepro]], this is the layer-transforming axis, not an index refinement: `apo`/`futu`
  * refine the residual `X`; `postpro` keeps `ana`'s index and decorates the *layer optic* on the
  * `embed` glue side. With `η = id` it is exactly [[Ana]].
  *
  * '''Cost, honestly.''' Each built child subtree is hoisted (`embed ∘ η` at every layer) before
  * its parent embeds it, so a node at depth `k` is re-transformed `k` times: `O(n · depth)`, the
  * inherent cost of the postpromorphism. Both the outer build and each hoist run on the stack-safe
  * [[Machines.foldLayered]] machine.
  */
final class Postpro[F[_], A, S](
    private[zoo] val eta: F ~> F,
    private[zoo] val coalg: A => F[A],
)(using F: Traverse[F], P: Project[F, S], E: Embed[F, S])
    extends BuildScheme[S, A]:
  type X = S

  private val build: A => S =
    // The one-shot hoist: apply η at *every* layer of a built S, rebuilding it.
    val hoist: S => S = Machines.foldLayered[F, S, S](P.project, (_, fr) => E.embed(eta(fr)))
    // Embed each step, hoisting every child subtree first, so depth-k nodes accumulate η k times.
    Machines.foldLayered[F, A, S](coalg, (_, fr) => E.embed(F.map(fr)(hoist)))

  protected def write(a: A): S = build(a)
