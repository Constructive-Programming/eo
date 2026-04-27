package dev.constructive.eo
package laws
package eo
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[FoldMapHomomorphismLaws]]. */
abstract class FoldMapHomomorphismTests[S, A, F[_, _]] extends Laws:
  def laws: FoldMapHomomorphismLaws[S, A, F]

  def foldMapHomomorphism(using
      Arbitrary[S],
      Cogen[A],
      ForgetfulFold[F],
  ): RuleSet =
    new SimpleRuleSet(
      "foldMap is a Monoid homomorphism",
      "foldMap(f ⊕ g) == foldMap(f) ⊕ foldMap(g)" ->
        forAll((s: S, f: A => Int, g: A => Int) => laws.foldMapHomomorphism(s, f, g)),
      "foldMap(_ => mempty) == mempty" ->
        forAll((s: S) => laws.foldMapEmpty(s)),
    )
