package eo.laws.data

import eo.data.PowerSeries
import eo.data.PowerSeries.given
import eo.{ForgetfulFunctor, ForgetfulTraverse}

import cats.{Applicative, Id}

/** Carrier-level laws for `PowerSeries[X, A]`.
  *
  * `PowerSeries[X, A]` wraps `(Snd[X], Vect[Int, A])` — a dynamically-
  * sized vector of focused values accompanied by a phantom `Snd[X]`
  * that threads back through the [[eo.data.PowerSeries.AssociativeFunctor]]
  * composition chain.
  *
  * The laws pin the two cats-style instances that drive runtime
  * behaviour:
  *
  *   * `ForgetfulFunctor[PowerSeries]` — identity and composition.
  *   * `ForgetfulTraverse[PowerSeries, Applicative]` at `Id` —
  *     identity.
  *
  * `AssociativeFunctor[PowerSeries, X, Y]` is exercised at the optic
  * level through `Composer[Tuple2 → PowerSeries]` / `[Either →
  * PowerSeries]` / `[Affine → PowerSeries]` bridges; the
  * end-to-end `modify` equivalence of those chains lives in
  * [[eo.laws.eo.ChainPathIndependenceLaws]]-style specs (Unit 7).
  */
trait PowerSeriesLaws[X, A]:
  def functorIdentity(ps: PowerSeries[X, A])(using
      FF: ForgetfulFunctor[PowerSeries]
  ): Boolean =
    FF.map(ps, identity[A]) == ps

  def functorComposition(
      ps: PowerSeries[X, A], f: A => A, g: A => A,
  )(using FF: ForgetfulFunctor[PowerSeries]): Boolean =
    FF.map(FF.map(ps, f), g) == FF.map(ps, f.andThen(g))

  def traverseIdentity(ps: PowerSeries[X, A])(using
      FT: ForgetfulTraverse[PowerSeries, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](using Applicative[Id])(ps)(a => (a: Id[A])) ==
      ps
