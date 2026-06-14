package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Apomorphism citizen — an unfold that may **short-circuit with a finished subtree**, worn as an
  * optic over [[dev.constructive.eo.data.Direct]] with **`X = Either[S, A]`** (the residual): each
  * child slot is either `Left(s)` — an already-built `S`, grafted in directly — or `Right(a)` — a
  * seed to keep unfolding. `Review`-shaped (`Optic[Unit, S, Unit, A, Direct]`), consumed via
  * `.reverseGet`.
  *
  * `coalg: A => F[Either[S, A]]` is the build-side dual of [[Para]]'s read-side subterm retention:
  * where para *reads* original subterms, apo *writes* finished ones. The `Either` residual is the
  * Prism's match worn build-side. An all-`Right` coalgebra degenerates to [[Ana]].
  *
  * '''O(1) graft.''' A `Left(s)` subtree is placed into its result slot **by reference** —
  * [[Machines.foldLayeredOr]]'s `Left` arm returns it without recursing or re-`project`ing — so
  * grafting a finished subtree is constant-time, not O(subtree). Stack-safe.
  */
final class Apo[F[_], A, S](private[zoo] val coalg: A => F[Either[S, A]])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends Optic[Unit, S, Unit, A, Direct]:
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

  def to(u: Unit): Direct[X, Unit] = Direct(())
  def from(b: Direct[X, A]): S = build(Direct.value(b))
