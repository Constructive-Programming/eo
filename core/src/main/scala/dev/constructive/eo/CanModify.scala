package dev.constructive.eo

import forgetful.ForgetfulFunctor
import optics.Optic

/** Capability: the focused `A` inside an `S` can be rewritten to a `B`, producing a `T` — the
  * carrier-free surface of any optic whose carrier admits [[forgetful.ForgetfulFunctor]] (every
  * writable family: Lens, Iso, Prism, Optional, Traversal, Modify).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanModify]] alias covers the common
  * `S = T`, `A = B` case. A read-then-write method should demand ONE `CanModify` (whose `modify`
  * observes and rewrites in a single pass) rather than split `CanGet` + `CanModify` evidence —
  * nothing ties two separately-summoned capabilities to the same optic. See [[CanGet]] for the
  * doctrine and the coherence rule.
  */
trait CanModifyP[S, T, A, B]:
  def modify(f: A => B): S => T
  def replace(b: B): S => T = modify(_ => b)

/** Monomorphic [[CanModifyP]] (`S = T`, `A = B`) — the shape most consuming signatures want:
  * `def adjustTimes[T](using cm: CanModify[T, DateTime]): T => T = cm.modify(adjustTime)`.
  */
type CanModify[S, A] = CanModifyP[S, S, A, A]

object CanModifyP:

  /** Derive from any optic in scope whose carrier can map its focus. Optic before gate in the same
    * using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ff: ForgetfulFunctor[F],
  ): CanModifyP[S, T, A, B] =
    f => o.modify(f)
