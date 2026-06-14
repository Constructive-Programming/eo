package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import optics.Optic

/** Paramorphism citizen — a fold that **retains the original subterms**, worn as an optic over
  * [[Scheme]] with **`X = F[(S, A)]`**: each child slot pairs the original subterm `S` with its
  * folded result `A`. `Getter`-shaped (`Optic[S, Unit, A, Unit, Scheme]`), consumed via `.get`.
  *
  * `alg: F[(S, A)] => A` is strictly more informed than [[Cata]]'s `F[A] => A` — it can read the
  * subterm itself, not just its summary (e.g. "keep the larger of each child's *original* subtree").
  * Ignoring the `S` half degenerates to [[Cata]].
  *
  * '''On the existential, honestly.''' `X = F[(S, A)]` is the store-comonad complement, which is why
  * the brainstorm flags `para` as the candidate *writable* scheme (`para : Cata :: Lens : Getter`).
  * The get-put direction holds definitionally (re-embedding the retained subterms rebuilds the
  * node), but put-get holds only under an algebra-coherence condition — so the lawful writable
  * `Lens` is conditional, not free. This citizen ships the unconditionally-sound read; the writable
  * put is a scoped follow-up rather than an asserted capability.
  *
  * The subterms are recovered by re-`project`ing each node (one extra peel per node) and zipping with
  * the children's results in `Foldable` order — sound for any lawful `Traverse`. Stack-safe (the
  * [[Machines.foldLayered]] machine).
  */
final class Para[F[_], S, A](private[zoo] val alg: F[(S, A)] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Scheme]:
  type X = F[(S, A)]

  private val run: S => A =
    Machines.foldLayered[F, S, A](
      P.project,
      (s, fa) =>
        // pair each child's original subterm (re-projected) with its folded result, in order.
        val it = F.toList(fa).iterator
        alg(F.map(P.project(s))(sub => (sub, it.next()))),
    )

  def to(s: S): Scheme[X, A] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()
