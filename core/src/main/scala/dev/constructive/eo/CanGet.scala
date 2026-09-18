package dev.constructive.eo

import accessor.Accessor
import optics.Optic

/** Capability: an `A` can be read out of an `S` — the carrier-free surface of any optic whose
  * carrier admits [[accessor.Accessor]] (Lens, Iso, Getter).
  *
  * This is the type to name in a *consuming* signature: leave the subject generic and demand only
  * the evidence the method needs — `def render[T](t: T)(using id: CanGet[T, OrderId])`. Concrete
  * optic types belong where optics are constructed and composed. One optic given per `(S, A)` pair
  * should be in scope at a call site — capabilities follow ordinary typeclass coherence; newtype
  * same-typed foci apart rather than relying on implicit priority to pick between them.
  */
trait CanGet[S, A]:
  def get(s: S): A

object CanGet:

  /** Read fanout — pair two reads on the same `S`. Trivially lawful (no write path, hence no
    * disjointness obligation, unlike the writeable [[optics.Optic.zip]] / [[CanModifyP.zip]]). This
    * is `&&&` on the two `get` functions. An extension (not a trait member) so that on a value that
    * is also `CanModifyP`, the more-specific read-modify-write [[CanModifyP.zip]] is chosen
    * instead.
    */
  extension [S, A](self: CanGet[S, A])

    def zip[C](that: CanGet[S, C]): CanGet[S, (A, C)] =
      s => (self.get(s), that.get(s))

  /** Derive from any optic in scope whose carrier can always read. The optic parameter precedes the
    * gate typeclass in the same using clause deliberately: same-clause resolution runs left to
    * right, so the optic pins the carrier `F` before `Accessor[F]` is searched. (A context bound
    * `F[_, _]: Accessor` would desugar — SIP-64, Scala 3.6+ — to a clause searched FIRST, with `F`
    * still free, and fail.) Concrete optic classes implement [[CanGet]] directly, and a concrete
    * given wins over this one by specificity, so this instance only serves optics known at the
    * generic `Optic[…, F]` type.
    */
  given [S, T, A, B, F[_, _]](using o: Optic[S, T, A, B, F], acc: Accessor[F]): CanGet[S, A] =
    s => o.get(s)
