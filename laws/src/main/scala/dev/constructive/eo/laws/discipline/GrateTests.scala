package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[GrateLaws]]. */
abstract class GrateTests[S, A] extends Laws:
  def laws: GrateLaws[S, A]

  def grate(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Grate",
      OpticLawProps.modifyIdentity[S](laws.modifyIdentity),
      OpticLawProps.composeModify[S, A](laws.composeModify),
      OpticLawProps.replaceIdempotent[S, A](laws.replaceIdempotent),
    )
