package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[LensLaws]]. */
abstract class LensTests[S, A] extends Laws:
  def laws: LensLaws[S, A]

  def lens(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Lens",
      "get-replace" -> forAll((s: S) => laws.getReplace(s)),
      "replace-get" -> forAll((s: S, a: A) => laws.replaceGet(s, a)),
      OpticLawProps.replaceIdempotent[S, A](laws.replaceIdempotent),
      OpticLawProps.modifyIdentity[S](laws.modifyIdentity),
      OpticLawProps.composeModify[S, A](laws.composeModify),
      OpticLawProps.consistentReplaceModify[S, A](laws.consistentReplaceModify),
    )
