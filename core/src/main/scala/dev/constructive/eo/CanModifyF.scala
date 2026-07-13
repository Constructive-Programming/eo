package dev.constructive.eo

import kernel.Functor
import forgetful.ForgetfulTraverse
import optics.Optic

/** Capability: the focused `A` inside an `S` can be rewritten effectfully under any `Functor[G]` —
  * the carrier-free surface of any optic whose carrier admits
  * [[forgetful.ForgetfulTraverse]]`[F, Functor]` (today: Lens).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanModifyF]] alias covers the
  * common case. See [[CanGet]] for the doctrine and the coherence rule.
  */
trait CanModifyFP[S, T, A, B]:
  def modifyF[G[_]](f: A => G[B])(using Functor[G]): S => G[T]

/** Monomorphic [[CanModifyFP]] (`S = T`, `A = B`). */
type CanModifyF[S, A] = CanModifyFP[S, S, A, A]

object CanModifyFP:

  /** Derive from any optic in scope whose carrier traverses under `Functor`. Optic before gate in
    * the same using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ft: ForgetfulTraverse[F, Functor],
  ): CanModifyFP[S, T, A, B] with
    def modifyF[G[_]](f: A => G[B])(using Functor[G]): S => G[T] = o.modifyF(f)
