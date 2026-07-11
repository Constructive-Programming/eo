package dev.constructive.eo.laws.data.discipline

import dev.constructive.eo.data.{Fst, ModifyF, Snd}
import dev.constructive.eo.forgetful.ForgetfulFunctor
import dev.constructive.eo.laws.data.ModifyFLaws
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ModifyFLaws]]. */
abstract class ModifyFTests[X, A] extends Laws:
  /** Laws under test. */
  def laws: ModifyFLaws[X, A]

  /** The "ModifyF" rule set. */
  def modifyF(using
      Arbitrary[Fst[X]],
      Arbitrary[Snd[X]],
      Cogen[Snd[X]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[ModifyF],
  ): RuleSet =
    new SimpleRuleSet(
      "ModifyF",
      "functor identity (extensional)" ->
        forAll((fst: Fst[X], fn: Snd[X] => A, x: Snd[X]) => laws.functorIdentity(fst, fn, x)),
      "functor composition (extensional)" ->
        forAll((fst: Fst[X], fn: Snd[X] => A, f: A => A, g: A => A, x: Snd[X]) =>
          laws.functorComposition(fst, fn, f, g, x)
        ),
    )
