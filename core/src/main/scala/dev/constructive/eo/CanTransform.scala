package dev.constructive.eo

import forgetful.ForgetfulFunctor
import optics.Optic

/** Capability: a `D` at the focus of an already-built `T` can be transformed via `D => B` — the
  * carrier-free surface of the `transform` extension (the generalised [[CanPlace]]).
  *
  * Like [[CanPlace]] this capability has NO derived given — the extension requires `T => F[X, D]`
  * evidence over the optic's existential `X`. Construct it explicitly: `CanTransform.from(myLens)`.
  * See [[CanGet]] for the doctrine and the coherence rule.
  */
trait CanTransform[T, D, B]:
  def transform(f: D => B): T => T

object CanTransform:

  /** Lift an optic whose `T`-side evidence is available. */
  def from[S, T, A, B, D, F[_, _]](o: Optic[S, T, A, B, F])(using
      ff: ForgetfulFunctor[F],
      ev: T => F[o.X, D],
  ): CanTransform[T, D, B] =
    f => o.transform(f)

export optics.Optic.transform

extension [T](t: T)

  /** Transform the `B` focus inside `t` via the [[CanTransform]] instance in scope. */
  inline def transform[B](f: B => B)(using ct: CanTransform[T, B, B]): T =
    ct.transform(f)(t)

  /** Polymorphic counterpart to [[transform]] — transforms a `D` at the focus into a `B`. */
  inline def transformP[D, B](f: D => B)(using ct: CanTransform[T, D, B]): T =
    ct.transform(f)(t)
