package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[PlatedLaws]]. Reusable by downstream projects to re-check the Plated
  * laws on their own recursive types (hand-written or macro-derived `plate`).
  */
abstract class PlatedTests[S] extends Laws:
  def laws: PlatedLaws[S]

  def plated(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Plated",
      "plate.modify(identity) == identity" ->
        forAll((s: S) => laws.plateModifyIdentity(s)),
      "transform(identity) == identity" ->
        forAll((s: S) => laws.transformIdentity(s)),
      "universe == self :: children.flatMap(universe)" ->
        forAll((s: S) => laws.universeDecomposition(s)),
      "children.length == plate.length" ->
        forAll((s: S) => laws.childrenLengthMatchesPlateLength(s)),
    )
