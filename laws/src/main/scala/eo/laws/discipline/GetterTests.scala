package eo
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[GetterLaws]]. */
abstract class GetterTests[S, A] extends Laws:
  def laws: GetterLaws[S, A]

  def getter(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Getter",
      "get consistent with reference" ->
        forAll((s: S) => laws.getConsistent(s)),
    )
