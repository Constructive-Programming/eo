package dev.constructive.eo
package schemes
package zoo

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

  /** The cofree-decorating combine shared by `histo` / `dyna` / `chrono` / `metaChrono`: tag each
    * rebuilt layer `F[Attr[F, A]]` with its algebra result, yielding the node's `Attr` (head =
    * result, tail = the decorated layer). The node argument is unused — the algebra is node-blind.
    */
  def decorate[F[_], N, A](alg: F[Attr[F, A]] => A): (N, F[Attr[F, A]]) => Attr[F, A] =
    (_, layer) => Attr(alg(layer), layer)
