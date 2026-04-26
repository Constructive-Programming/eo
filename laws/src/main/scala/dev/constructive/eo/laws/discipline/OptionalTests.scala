package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[OptionalLaws]]. */
abstract class OptionalTests[S, A] extends Laws:
  def laws: OptionalLaws[S, A]

  def optional(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Optional",
      OpticLawProps.modifyIdentity[S](laws.modifyIdentity),
      OpticLawProps.composeModify[S, A](laws.composeModify),
      "consistent getOption / modify-id" ->
        forAll((s: S) => laws.consistentGetOptionModifyId(s)),
    )
