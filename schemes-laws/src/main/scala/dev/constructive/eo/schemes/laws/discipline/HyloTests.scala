package dev.constructive.eo
package schemes
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[HyloLaws]]. Reusable by downstream projects to check the hylo fusion
  * contract on their own coalgebra / algebra pairs. The seed generator should straddle the engines'
  * on-stack depth limit (512) so both the recursive fast path and the heap machine are exercised
  * under the equality.
  */
abstract class HyloTests[Seed, S, A] extends Laws:
  def laws: HyloLaws[Seed, S, A]

  def hylo(using Arbitrary[Seed]): RuleSet =
    new SimpleRuleSet(
      "Hylo",
      "hylo(expand, fused) == ana(coalg) cross cata(alg)" ->
        forAll((seed: Seed) => laws.hyloFusion(seed)),
    )
