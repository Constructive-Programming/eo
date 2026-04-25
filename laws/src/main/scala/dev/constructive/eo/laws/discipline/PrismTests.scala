package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[PrismLaws]]. */
abstract class PrismTests[S, A] extends Laws:
  def laws: PrismLaws[S, A]

  def prism(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Prism",
      "partial round trip one way" ->
        forAll((s: S) => laws.partialRoundTripOneWay(s)),
      "round trip other way" ->
        forAll((a: A) => laws.roundTripOtherWay(a)),
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "consistent getOption / modify-id" ->
        forAll((s: S) => laws.consistentGetOptionModifyId(s)),
    )
