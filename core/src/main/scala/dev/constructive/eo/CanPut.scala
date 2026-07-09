package dev.constructive.eo

import forgetful.ForgetfulApplicative
import optics.Optic

/** Capability: a `T` can be constructed directly from an `A`, running `f: A => B` at the focus —
  * the carrier-free surface of any optic whose carrier admits [[forgetful.ForgetfulApplicative]]
  * (today: the `Direct`-carrier families, and Folds over an `Applicative`).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanPut]] alias covers `A = B`. See
  * [[CanGet]] for the doctrine and the coherence rule.
  */
trait CanPutP[T, A, B]:
  def put(f: A => B): A => T

/** Monomorphic [[CanPutP]] (`A = B`). */
type CanPut[T, A] = CanPutP[T, A, A]

object CanPutP:

  /** Derive from any optic in scope whose carrier can inject. Optic before gate in the same using
    * clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      fa: ForgetfulApplicative[F],
  ): CanPutP[T, A, B] =
    f => o.put(f)
