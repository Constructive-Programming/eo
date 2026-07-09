package dev.constructive.eo

import accessor.ReverseAccessor
import optics.Optic

/** Capability: a `T` can be built from a `B` — the carrier-free surface of any optic whose carrier
  * admits [[accessor.ReverseAccessor]] (Iso, Prism, Review).
  *
  * Prefer this trait in consuming signatures; see [[CanGet]] for the doctrine and the coherence
  * rule.
  */
trait CanReverseGet[T, B]:
  def reverseGet(b: B): T

object CanReverseGet:

  /** Derive from any optic in scope whose carrier can build. Optic before gate in the same using
    * clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ra: ReverseAccessor[F],
  ): CanReverseGet[T, B] =
    b => o.reverseGet(b)
