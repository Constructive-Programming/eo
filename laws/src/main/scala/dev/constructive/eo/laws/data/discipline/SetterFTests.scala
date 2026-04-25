package dev.constructive.eo.laws.data.discipline

import dev.constructive.eo.data.{Fst, SetterF, Snd}
import dev.constructive.eo.ForgetfulFunctor
import dev.constructive.eo.laws.data.SetterFLaws

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[SetterFLaws]]. */
abstract class SetterFTests[X, A] extends Laws:
  def laws: SetterFLaws[X, A]

  def setterF(using
      Arbitrary[Fst[X]],
      Arbitrary[Snd[X]],
      Cogen[Snd[X]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[SetterF],
  ): RuleSet =
    new SimpleRuleSet(
      "SetterF",
      "functor identity (extensional)" ->
        forAll((fst: Fst[X], fn: Snd[X] => A, x: Snd[X]) => laws.functorIdentity(fst, fn, x)),
      "functor composition (extensional)" ->
        forAll((fst: Fst[X], fn: Snd[X] => A, f: A => A, g: A => A, x: Snd[X]) =>
          laws.functorComposition(fst, fn, f, g, x)
        ),
    )
