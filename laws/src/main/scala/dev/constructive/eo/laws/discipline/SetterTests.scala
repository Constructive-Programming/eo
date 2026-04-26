package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[SetterLaws]]. */
abstract class SetterTests[S, A] extends Laws:
  def laws: SetterLaws[S, A]

  def setter(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Setter",
      OpticLawProps.modifyIdentity[S](laws.modifyIdentity),
      OpticLawProps.composeModify[S, A](laws.composeModify),
      OpticLawProps.replaceIdempotent[S, A](laws.replaceIdempotent),
      OpticLawProps.consistentReplaceModify[S, A](laws.consistentReplaceModify),
    )
