package dev.constructive.eo.laws.data.discipline

import dev.constructive.eo.data.Affine
import dev.constructive.eo.{ForgetfulFunctor, ForgetfulTraverse}
import dev.constructive.eo.laws.data.AffineLaws

import cats.Applicative
import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[AffineLaws]]. */
abstract class AffineTests[X, A] extends Laws:
  def laws: AffineLaws[X, A]

  def affine(using
      Arbitrary[Affine[X, A]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[Affine],
      ForgetfulTraverse[Affine, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "Affine",
      "functor identity" ->
        forAll((fa: Affine[X, A]) => laws.functorIdentity(fa)),
      "functor composition" ->
        forAll((fa: Affine[X, A], f: A => A, g: A => A) => laws.functorComposition(fa, f, g)),
      "traverse[Id] identity" ->
        forAll((fa: Affine[X, A]) => laws.traverseIdentity(fa)),
    )
