package eo
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[IsoLaws]]. */
abstract class IsoTests[S, A] extends Laws:
  def laws: IsoLaws[S, A]

  def iso(using Arbitrary[S], Arbitrary[A]): RuleSet =
    new SimpleRuleSet(
      "Iso",
      "round trip one way" -> forAll((s: S) => laws.roundTripOneWay(s)),
      "round trip other way" -> forAll((a: A) => laws.roundTripOtherWay(a)),
    )
