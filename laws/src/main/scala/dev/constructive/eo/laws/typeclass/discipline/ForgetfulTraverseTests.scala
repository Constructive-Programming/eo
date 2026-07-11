package dev.constructive.eo.laws.typeclass.discipline

import cats.Applicative
import dev.constructive.eo.forgetful.ForgetfulTraverse
import dev.constructive.eo.laws.typeclass.ForgetfulTraverseLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ForgetfulTraverseLaws]]. */
abstract class ForgetfulTraverseTests[F[_, _], X, A] extends Laws:
  /** Laws under test. */
  def laws: ForgetfulTraverseLaws[F, X, A]

  /** The "ForgetfulTraverse" rule set. */
  def forgetfulTraverse(using
      Arbitrary[F[X, A]],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "ForgetfulTraverse",
      "traverse[Id] identity" ->
        forAll((fa: F[X, A]) => laws.traverseIdentity(fa)),
    )
