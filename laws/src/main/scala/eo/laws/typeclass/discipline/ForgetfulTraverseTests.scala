package eo.laws.typeclass.discipline

import eo.ForgetfulTraverse
import eo.laws.typeclass.ForgetfulTraverseLaws

import cats.Applicative
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ForgetfulTraverseLaws]]. */
abstract class ForgetfulTraverseTests[F[_, _], X, A] extends Laws:
  def laws: ForgetfulTraverseLaws[F, X, A]

  def forgetfulTraverse(using
      Arbitrary[F[X, A]],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "ForgetfulTraverse",
      "traverse[Id] identity" ->
        forAll((fa: F[X, A]) => laws.traverseIdentity(fa)),
    )
