package dev.constructive.eo.laws.data

import dev.constructive.eo.data.PowerSeries
import dev.constructive.eo.{ForgetfulFunctor, ForgetfulTraverse}

import cats.{Applicative, Id}

/** Carrier-level laws for `PowerSeries[X, A]`.
  *
  * `PowerSeries[X, A]` wraps `(Snd[X], ArraySeq[A])` — a flat contiguous vector of focused values
  * accompanied by a phantom `Snd[X]` that threads back through the AssociativeFunctor composition
  * chain.
  *
  * The laws pin the two cats-style instances that drive runtime behaviour:
  *
  *   - `ForgetfulFunctor[PowerSeries]` — identity and composition.
  *   - `ForgetfulTraverse[PowerSeries, Applicative]` at `Id` — identity.
  *
  * `AssociativeFunctor` for `PowerSeries` is exercised at the optic level through the
  * `Composer[Tuple2 → PowerSeries]` / `[Either → PowerSeries]` / `[Affine → PowerSeries]` bridges;
  * the end-to-end `modify` equivalence of those chains lives in
  * `dev.constructive.eo.laws.eo.ChainPathIndependenceLaws`-style specs.
  */
trait PowerSeriesLaws[X, A]:

  def functorIdentity(ps: PowerSeries[X, A])(using
      FF: ForgetfulFunctor[PowerSeries]
  ): Boolean =
    FF.map(ps, identity[A]) == ps

  def functorComposition(
      ps: PowerSeries[X, A],
      f: A => A,
      g: A => A,
  )(using FF: ForgetfulFunctor[PowerSeries]): Boolean =
    FF.map(FF.map(ps, f), g) == FF.map(ps, f.andThen(g))

  def traverseIdentity(ps: PowerSeries[X, A])(using
      FT: ForgetfulTraverse[PowerSeries, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](using Applicative[Id])(ps)(a => a: Id[A]) ==
      ps
