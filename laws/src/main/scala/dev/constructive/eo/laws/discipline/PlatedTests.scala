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
  /** Laws under test. */
  def laws: PlatedLaws[S]

  /** Whether `a` may be substituted into a child slot currently holding `x`. Defaults to `true`
    * (every single-sorted `Plated[S]`); override for a multi-sorted union `S = S1 | S2 | …` where
    * each child slot is typed to its member sort `Si` (O'Connor 2011 §5).
    */
  def sameSort(x: S, a: S): Boolean =
    val _ = (x, a)
    true

  /** The "Plated" rule set. */
  def plated(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Plated",
      "plate.modify(identity) == identity" ->
        forAll((s: S) => laws.plateModifyIdentity(s)),
      "plate.modify(g) ∘ plate.modify(f) == plate.modify(f andThen g)" ->
        forAll { (s: S, a1: S, a2: S, k1: Int, k2: Int) =>
          val pick1: S => S = x =>
            if sameSort(x, a1) then a1
            else if sameSort(x, a2) then a2
            else if sameSort(x, s) then s
            else x
          val pick2: S => S = x =>
            if sameSort(x, a2) then a2
            else if sameSort(x, a1) then a1
            else if sameSort(x, s) then s
            else x
          val f: S => S = x => if ((x.## ^ k1) & 1) == 0 then pick1(x) else x
          val g: S => S = x => if ((x.## ^ k2) & 1) == 0 then pick2(x) else x
          laws.plateComposeModify(s, f, g) &&
          laws.plateComposeModify(s, pick1, pick2)
        },
      "transform(identity) == identity" ->
        forAll((s: S) => laws.transformIdentity(s)),
      "transformM[Id](f) == transform(f)" ->
        forAll { (s: S, a1: S, k1: Int) =>
          val f: S => S = x => if ((x.## ^ k1) & 1) == 0 && sameSort(x, a1) then a1 else x
          laws.transformMIdConsistent(s, f)
        },
      "universe == self :: children.flatMap(universe)" ->
        forAll((s: S) => laws.universeDecomposition(s)),
      "children.length == plate.length" ->
        forAll((s: S) => laws.childrenLengthMatchesPlateLength(s)),
    )
