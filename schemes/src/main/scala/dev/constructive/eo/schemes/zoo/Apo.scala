package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Affine
import optics.Optic

/** Apomorphism citizen — an unfold that may **short-circuit with a finished subtree**
  * ([[BuildScheme]]) with **`X = Either[S, A]`** (the residual): each child slot is either
  * `Left(s)` — an already-built `S`, grafted in directly — or `Right(a)` — a seed to keep
  * unfolding.
  *
  * `coalg: A => F[Either[S, A]]` is the build-side dual of [[Para]]'s read-side subterm retention:
  * where para *reads* original subterms, apo *writes* finished ones. The `Either` residual is the
  * Prism's match worn build-side. An all-`Right` coalgebra degenerates to [[Ana]].
  *
  * '''The residual is a [[data.Affine]] optic.''' apo's per-slot decision is exactly `Affine`'s
  * build seam — `Left(s)` is `Miss(s)` (a finished slot, the O(1) graft), `Right(a)` is `Hit((),
  * a)` (keep unfolding). [[Apo.scatter]] exposes that decision as a composable `Affine`-carried
  * optic (a *scatter*), and this engine constructs and consumes it through that optic: every slot
  * goes `residual → scatter.to → Miss/Hit → engine`, so apo genuinely speaks the carrier the
  * carrier was written for. The pure [[Machines.foldLayeredOr]] engine still recurses over an
  * `Either` at its boundary (it is shared with elgot/cozygo); the `Miss`/`Hit` decision is
  * collapsed onto that boundary at the last step.
  *
  * '''O(1) graft.''' A `Miss(s)` subtree is placed into its result slot **by reference** — the
  * engine's `Left` arm returns it without recursing or re-`project`ing. Stack-safe.
  */
final class Apo[F[_], A, S](private[zoo] val coalg: A => F[Either[S, A]])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends BuildScheme[S, A]:
  type X = Either[S, A]

  private val build: A => S =
    val sc = Apo.scatter[S, A]
    val run = Machines.foldLayeredOr[F, Either[S, A], S](
      residual =>
        sc.to(residual)
          .fold[Either[S, F[Either[S, A]]]](
            s => Left(s), // Miss — finished subtree, grafted by reference (O(1))
            (_, a) => Right(coalg(a)), // Hit — seed, keep unfolding
          ),
      fr => E.embed(fr),
    )
    a => run(Right(a))

  protected def write(a: A): S = build(a)

object Apo:

  /** apo's per-slot residual worn on the [[data.Affine]] build seam — a *scatter* decoration.
    * `Left(s) → Miss(s)` (the O(1) graft); `Right(a) → Hit((), a)` (keep unfolding). The
    * existential is pinned `X = (S, Unit)`: `Fst[X] = S` is the grafted subtree, `Snd[X] = Unit` (a
    * single slot decision carries no extra one-layer leftover). As a genuine `Optic[…, Affine]`
    * value it composes via [[data.Affine.assoc]] and the [[data.Affine.either2affine]] bridge (so a
    * Prism whose focus is the residual composes straight into it). The `X` is exposed (refined) so
    * `Fst[X]` reduces at use sites.
    */
  def scatter[S, A]: Optic[Either[S, A], Unit, A, Unit, Affine] { type X = (S, Unit) } =
    new Optic[Either[S, A], Unit, A, Unit, Affine]:
      type X = (S, Unit)
      def to(e: Either[S, A]): Affine[X, A] = e match
        case Left(s)  => new Affine.Miss[X, A](s)
        case Right(a) => new Affine.Hit[X, A]((), a)
      def from(b: Affine[X, Unit]): Unit = ()
