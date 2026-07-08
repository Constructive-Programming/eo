package dev.constructive.eo

import cats.Applicative

import forgetful.ForgetfulTraverse
import optics.Optic

/** Capability: the foci `A` inside an `S` can be rewritten effectfully under any `Applicative[G]` —
  * the carrier-free surface of any optic whose carrier admits
  * [[forgetful.ForgetfulTraverse]]`[F, Applicative]` (Lens, Prism, Optional, Traversal, Fold).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanModifyA]] alias covers the
  * common case. For collecting the foci without effects, [[CanFold.foci]] is the carrier-free
  * counterpart of the raw-optic `all` extension. See [[CanGet]] for the doctrine and the coherence
  * rule.
  */
trait CanModifyAP[S, T, A, B]:
  def modifyA[G[_]](f: A => G[B])(using Applicative[G]): S => G[T]

/** Monomorphic [[CanModifyAP]] (`S = T`, `A = B`). */
type CanModifyA[S, A] = CanModifyAP[S, S, A, A]

object CanModifyAP:

  /** Derive from any optic in scope whose carrier traverses under `Applicative`. Optic before gate
    * in the same using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ft: ForgetfulTraverse[F, Applicative],
  ): CanModifyAP[S, T, A, B] with
    def modifyA[G[_]](f: A => G[B])(using Applicative[G]): S => G[T] = o.modifyA(f)
