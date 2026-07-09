package dev.constructive.eo

import accessor.PartialAccessor
import optics.Optic

/** Capability: an `A` may be read out of an `S` — the carrier-free surface of any optic whose
  * carrier admits [[accessor.PartialAccessor]] (Prism, Optional, AffineFold).
  *
  * Prefer this trait in consuming signatures; see [[CanGet]] for the doctrine and the one-optic-
  * given-per-`(S, A)` coherence rule.
  */
trait CanGetOption[S, A]:
  def getOption(s: S): Option[A]

object CanGetOption:

  /** Partial-read fanout — the pair is present iff BOTH legs hit. Trivially lawful (no write path).
    * The partial-read counterpart of [[CanGet.zip]]; pairs Prism / Optional / AffineFold reads.
    */
  extension [S, A](self: CanGetOption[S, A])

    def zip[C](that: CanGetOption[S, C]): CanGetOption[S, (A, C)] =
      s => self.getOption(s).flatMap(a => that.getOption(s).map(c => (a, c)))

  /** Derive from any optic in scope whose carrier can partially read. Optic before gate in the same
    * using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      acc: PartialAccessor[F],
  ): CanGetOption[S, A] =
    s => o.getOption(s)
