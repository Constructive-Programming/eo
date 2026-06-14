package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Apomorphism citizen — an unfold that may **short-circuit with a finished subtree**
  * ([[BuildScheme]]) with **`X = Either[S, A]`** (the residual): each child slot is either
  * `Left(s)` — an already-built `S`, grafted in directly — or `Right(a)` — a seed to keep
  * unfolding.
  *
  * `coalg: A => F[Either[S, A]]` is the build-side dual of [[Para]]'s read-side subterm retention:
  * where para *reads* original subterms, apo *writes* finished ones. The `Either` residual is the
  * Prism's match worn build-side. An all-`Right` coalgebra degenerates to [[Ana]].
  *
  * '''O(1) graft.''' A `Left(s)` subtree is placed into its result slot **by reference** —
  * [[Machines.foldLayeredOr]]'s `Left` arm returns it without recursing or re-`project`ing.
  * Stack-safe.
  */
final class Apo[F[_], A, S](private[zoo] val coalg: A => F[Either[S, A]])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends BuildScheme[S, A]:
  type X = Either[S, A]

  private val build: A => S =
    val run = Machines.foldLayeredOr[F, Either[S, A], S](
      {
        case Left(s)  => Left(s) // finished subtree — grafted by reference, O(1)
        case Right(a) => Right(coalg(a)) // seed — keep unfolding
      },
      fr => E.embed(fr),
    )
    a => run(Right(a))

  protected def write(a: A): S = build(a)
