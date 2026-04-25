package dev.constructive.eo
package laws

import optics.Optic
import optics.Optic.*

import cats.Monoid

/** Law equations for a `Fold[S, A]` — a read-only optic whose carrier `F` has `ForgetfulFold[F]`
  * and whose write-side is pinned to `Unit`.
  *
  * These laws encode the two universal properties of any monoidal fold — `foldMap(const(mempty))`
  * collapses to `mempty`, and `foldMap` is a monoid homomorphism in the target monoid. The
  * homomorphism property is stated here (additive `Int` monoid, sufficient to witness the law); see
  * also [[dev.constructive.eo.laws.eo.FoldMapHomomorphismLaws]] which states it for any optic with
  * `ForgetfulFold[F]` and is reused by Traversal / Lens. FoldLaws re-states the empty-constant
  * variant here because it is a genuine stand-alone property of Fold that does not need the
  * homomorphism law's two-function phrasing.
  *
  * Note: `FoldLaws` does not include a `foldMap` consistency check against an externally-supplied
  * `Foldable[F]`. For `Fold.apply[F, A]` that consistency is a corollary of how the carrier
  * `Forget[F]` wires its `ForgetfulFold` instance through `Foldable[F].foldMap`. Users who want
  * that check should exercise it as a spec-level property test against the concrete constructor.
  */
trait FoldLaws[S, A, F[_, _]]:
  def fold: Optic[S, Unit, A, A, F]

  /** `foldMap(const(mempty))(s) == mempty` for any source `s`. */
  def foldMapEmpty(s: S)(using ForgetfulFold[F]): Boolean =
    fold.foldMap[Int](_ => 0)(s) == Monoid[Int].empty

  /** Monoid homomorphism on the target monoid (tested at `Int`). */
  def foldMapHomomorphism(s: S, f: A => Int, g: A => Int)(using
      ForgetfulFold[F]
  ): Boolean =
    fold.foldMap(a => f(a) + g(a))(s) ==
      fold.foldMap(f)(s) + fold.foldMap(g)(s)
