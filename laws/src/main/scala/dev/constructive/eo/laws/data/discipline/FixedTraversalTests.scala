package dev.constructive.eo.laws.data.discipline

import dev.constructive.eo.data.FixedTraversal
import dev.constructive.eo.ForgetfulFunctor
import dev.constructive.eo.laws.data.FixedTraversalLaws

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[FixedTraversalLaws]]. */
abstract class FixedTraversalTests[N <: Int, X, A] extends Laws:
  def laws: FixedTraversalLaws[N, X, A]

  def fixedTraversal(using
      Arbitrary[FixedTraversal[N][X, A]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[FixedTraversal[N]],
  ): RuleSet =
    new SimpleRuleSet(
      "FixedTraversal",
      "functor identity" ->
        forAll((fa: FixedTraversal[N][X, A]) => laws.functorIdentity(fa)),
      "functor composition" ->
        forAll((fa: FixedTraversal[N][X, A], f: A => A, g: A => A) =>
          laws.functorComposition(fa, f, g)
        ),
    )
