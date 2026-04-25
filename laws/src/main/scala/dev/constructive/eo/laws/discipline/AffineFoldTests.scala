package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[AffineFoldLaws]]. */
abstract class AffineFoldTests[S, A] extends Laws:
  def laws: AffineFoldLaws[S, A]

  def affineFold(using Arbitrary[S], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "AffineFold",
      "getOption consistent with foldMap list" ->
        forAll((s: S) => laws.getOptionConsistent(s)),
      "miss implies empty foldMap" ->
        forAll((s: S, f: A => Int) => laws.missIsEmpty(s, f)),
      "hit implies singleton foldMap" ->
        forAll((s: S, f: A => Int) => laws.hitIsSingleton(s, f)),
    )
