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

  /** Read-modify-write fanout. Modifying a coupled pair `(A, C) => (A, C)` must first *observe*
    * both foci, so — unlike the read-only [[CanGet.zip]] — the writeable zip needs both read and
    * write. The receiver is therefore the intersection
    * `CanGet[S, A] & CanModify[S, A]`, which every concrete optic (Lens, Optional, …) already
    * satisfies, so the read and write are tied to the SAME optic (honouring the doctrine's "one
    * CanModify observes and rewrites"). The two writes reconcile sequentially (leg-2 on leg-1's
    * result), so no merge is needed. Lawful iff the foci are disjoint.
    */
  extension [S, A](self: CanGet[S, A] & CanModify[S, A])

    def zip[C](
        that: CanGet[S, C] & CanModify[S, C]
    ): CanGet[S, (A, C)] & CanModify[S, (A, C)] =
      new CanGet[S, (A, C)] with CanModifyP[S, S, (A, C), (A, C)]:
        def get(s: S): (A, C) = (self.get(s), that.get(s))
        def modify(g: ((A, C)) => (A, C)): S => S = s =>
          val (a, c) = g(get(s))
          that.replace(c)(self.replace(a)(s))
