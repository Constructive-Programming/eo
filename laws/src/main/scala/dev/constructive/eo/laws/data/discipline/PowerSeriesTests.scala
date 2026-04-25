package dev.constructive.eo.laws.data.discipline

import dev.constructive.eo.data.PowerSeries
import dev.constructive.eo.{ForgetfulFunctor, ForgetfulTraverse}
import dev.constructive.eo.laws.data.PowerSeriesLaws

import cats.Applicative
import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[PowerSeriesLaws]]. */
abstract class PowerSeriesTests[X, A] extends Laws:
  def laws: PowerSeriesLaws[X, A]

  def powerSeries(using
      Arbitrary[PowerSeries[X, A]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[PowerSeries],
      ForgetfulTraverse[PowerSeries, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "PowerSeries",
      "functor identity" ->
        forAll((ps: PowerSeries[X, A]) => laws.functorIdentity(ps)),
      "functor composition" ->
        forAll((ps: PowerSeries[X, A], f: A => A, g: A => A) => laws.functorComposition(ps, f, g)),
      "traverse[Id] identity" ->
        forAll((ps: PowerSeries[X, A]) => laws.traverseIdentity(ps)),
    )
