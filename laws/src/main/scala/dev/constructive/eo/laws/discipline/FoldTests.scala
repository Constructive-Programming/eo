package dev.constructive.eo
package laws
package discipline

import dev.constructive.eo.forgetful.*
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[FoldLaws]]. */
abstract class FoldTests[S, A, F[_, _]] extends Laws:
  /** Laws under test. */
  def laws: FoldLaws[S, A, F]

  /** The "Fold" rule set. */
  def fold(using
      Arbitrary[S],
      Cogen[A],
      ForgetfulFold[F],
  ): RuleSet =
    new SimpleRuleSet(
      "Fold",
      "foldMap(const mempty) == mempty" ->
        forAll((s: S) => laws.foldMapEmpty(s)),
      "foldMap homomorphism" ->
        forAll((s: S, f: A => Int, g: A => Int) => laws.foldMapHomomorphism(s, f, g)),
    )
