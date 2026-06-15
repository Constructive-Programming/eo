package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Comutumorphism citizen — the **build-side dual of [[Mutu]]** ([[BuildScheme]]) with **`X =
  * Either[A, B]`**: two coalgebras unfold by mutual co-recursion, each slot tagged with the
  * coalgebra that produced it.
  *
  * `coalgA: A => F[Either[A, B]]` and `coalgB: B => F[Either[A, B]]` are the two mutually
  * co-recursive unfolds; the entry seed is an `A` (`Left`). It generalises [[Cozygo]] — that scheme
  * is `comutu` where the secondary coalgebra never re-enters the primary type — and so, like its
  * fold-side mirror, degenerates to [[Ana]] when only one coalgebra is ever reached. Stack-safe
  * (the [[Machines.foldLayered]] machine).
  */
final class Comutu[F[_], A, B, S](
    private[zoo] val coalgA: A => F[Either[A, B]],
    private[zoo] val coalgB: B => F[Either[A, B]],
)(using F: Traverse[F], E: Embed[F, S])
    extends BuildScheme[S, A]:
  type X = Either[A, B]

  private val build: A => S =
    val expand: Either[A, B] => F[Either[A, B]] =
      case Left(a)  => coalgA(a)
      case Right(b) => coalgB(b)
    val run = Machines.buildLayered[F, Either[A, B], S](expand)
    a => run(Left(a))

  protected def write(a: A): S = build(a)
