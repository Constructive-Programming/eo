package dev.constructive.eo

import forgetful.ForgetfulFunctor
import optics.Optic

/** Capability: a `B` can be written into an already-built `T` — the carrier-free surface of the
  * `place` / `transfer` extensions.
  *
  * Unlike its siblings this capability has NO derived given: the extensions additionally require
  * `T => F[X, B]` evidence over the optic's *existential* `X`, which a generic optic given does not
  * refine. Construct it explicitly at the seam instead — `CanPlace.from(myLens)` — and pass it
  * along. See [[CanGet]] for the doctrine and the coherence rule.
  */
trait CanPlace[T, B]:
  def place(b: B): T => T
  def transfer[C](f: C => B): T => C => T = t => c => place(f(c))(t)

object CanPlace:

  /** Lift an optic whose `T`-side evidence is available (e.g. a `SimpleLens`, whose companion
    * provides the `S => (X, A)` splitter as that evidence).
    */
  def from[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
      ff: ForgetfulFunctor[F],
      ev: T => F[o.X, B],
  ): CanPlace[T, B] =
    b => o.place(b)
