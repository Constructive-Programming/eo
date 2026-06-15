package dev.constructive.eo
package schemes
package zoo

import cats.{~>, Traverse}

/** Prepromorphism citizen — a fold that applies a **natural transformation `η : F ~> F` before
  * recursing** ([[ReadScheme]]). The algebra `alg: F[A] => A` is exactly [[Cata]]'s — node-blind,
  * so `X = Nothing` — but the recursion is reshaped: the layer reaching a node at depth `k` has had
  * `η` applied `k` times.
  *
  * This is the orthogonal axis to the comonad/monad index towers. `para`/`histo` refine *what the
  * algebra sees* (the existential `X`); `prepro` keeps the trivial index and instead decorates the
  * *layer optic* — the `project` peel is pre-composed with the accumulating `η`. With `η = id` it
  * is exactly [[Cata]].
  *
  * '''Cost, honestly.''' Each descent applies `η` to a whole subtree before folding it (the
  * one-shot hoist `embed ∘ η` at every layer), so a node at depth `k` is re-transformed `k` times:
  * `O(n · depth)` total, the inherent cost of the prepromorphism (Uustalu & Vene). Both the outer
  * fold and each hoist run on the stack-safe [[Machines.foldLayered]] machine.
  */
final class Prepro[F[_], S, A](
    private[zoo] val eta: F ~> F,
    private[zoo] val alg: F[A] => A,
)(using F: Traverse[F], P: Project[F, S], E: Embed[F, S])
    extends ReadScheme[S, A]:
  type X = Nothing

  private val run: S => A =
    // The one-shot hoist: apply η at *every* layer of an S, rebuilding it.
    val hoist: S => S = Machines.foldLayered[F, S, S](P.project, (_, fr) => E.embed(eta(fr)))
    // Descend pre-hoisting each child, so depth-k nodes accumulate η k times.
    val expand: S => F[S] = s => F.map(P.project(s))(hoist)
    Machines.foldLayered[F, S, A](expand, (_, fr) => alg(fr))

  protected def read(s: S): A = run(s)
