package eo.laws.typeclass.discipline

import eo.ForgetfulFunctor
import eo.laws.typeclass.ForgetfulFunctorLaws

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ForgetfulFunctorLaws]]. */
abstract class ForgetfulFunctorTests[F[_, _], X, A] extends Laws:
  def laws: ForgetfulFunctorLaws[F, X, A]

  def forgetfulFunctor(using
      Arbitrary[F[X, A]], Arbitrary[A], Cogen[A],
      ForgetfulFunctor[F],
  ): RuleSet =
    new SimpleRuleSet(
      "ForgetfulFunctor",
      "functor identity" ->
        forAll((fa: F[X, A]) => laws.functorIdentity(fa)),
      "functor composition" ->
        forAll((fa: F[X, A], f: A => A, g: A => A) =>
          laws.functorComposition(fa, f, g)
        ),
    )
