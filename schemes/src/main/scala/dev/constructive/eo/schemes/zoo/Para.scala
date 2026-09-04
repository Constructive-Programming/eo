package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Paramorphism citizen — a fold that **retains the original subterms** ([[ReadScheme]]) with **`X =
  * F[(S, A)]`**: each child slot pairs the original subterm `S` with its folded result `A`.
  *
  * `alg: F[(S, A)] => A` is strictly more informed than [[Cata]]'s `F[A] => A` — it can read the
  * subterm itself, not just its summary. Ignoring the `S` half degenerates to [[Cata]].
  *
  * '''On the existential, honestly.''' `X = F[(S, A)]` is the store-comonad complement, which is
  * why the brainstorm flags `para` as the candidate *writable* scheme (`para : Cata :: Lens :
  * Getter`). get-put holds definitionally (re-embedding the retained subterms rebuilds the node),
  * but put-get holds only under an algebra-coherence condition — so the lawful writable `Lens` is
  * conditional, not free. This citizen ships the unconditionally-sound read; the writable put is a
  * scoped follow-up rather than an asserted capability.
  *
  * Subterms are recovered by re-`project`ing each node (one extra peel per node) and zipping with
  * the children's results in `Foldable` order — sound for any lawful `Traverse`. Stack-safe (the
  * [[Machines.foldLayered]] machine).
  */
final class Para[F[_], S, A](private[zoo] val alg: F[(S, A)] => A)(using
    F: Traverse[F],
    P: Project[F, S],
) extends ReadScheme[S, A]:
  type X = F[(S, A)]

  private val run: S => A =
    Machines.foldLayeredSlot[F, S, A](
      P.project,
      (_, layer, slots) =>
        // pair each child's original subterm with its folded result — positionally, off the
        // layer the machine already expanded and its own slot buffer. No re-project, no
        // per-node `List` (the C7 re-pin: the toList+re-project route cost ~+262k B/op on the
        // 8 191-node fixture, pushing para past droste).
        alg(Machines.rebuildLayerPaired(layer, slots)),
    )

  protected def read(s: S): A = run(s)
