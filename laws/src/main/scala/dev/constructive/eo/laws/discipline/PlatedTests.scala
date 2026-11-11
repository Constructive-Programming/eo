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

  /** A sort-respecting rewrite: each node becomes the first of `as` of its own sort (or stays). */
  private def choose(as: S*): S => S = x => as.find(sameSort(x, _)).getOrElse(x)

  /** `h` on a pseudo-random subset of nodes (picked by hash, salted by `k`), identity elsewhere. */
  private def gated(k: Int, h: S => S): S => S = x => if ((x.## ^ k) & 1) == 0 then h(x) else x

  /** The "Plated" rule set. */
  def plated(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Plated",
      "plate.modify(identity) == identity" ->
        forAll((s: S) => laws.plateModifyIdentity(s)),
      "plate.modify(g) ∘ plate.modify(f) == plate.modify(f andThen g)" ->
        forAll { (s: S, a1: S, a2: S, k1: Int, k2: Int) =>
          val pick1 = choose(a1, a2, s)
          val pick2 = choose(a2, a1, s)
          laws.plateComposeModify(s, gated(k1, pick1), gated(k2, pick2)) &&
          laws.plateComposeModify(s, pick1, pick2)
        },
      "mapChildren(g) == plate.modify(g)" ->
        forAll((s: S, a1: S, a2: S, k: Int) =>
          laws.mapChildrenConsistent(s, gated(k, choose(a1, a2)))
        ),
      "transform(identity) == identity" ->
        forAll((s: S) => laws.transformIdentity(s)),
      "transformM[Id](f) == transform(f)" ->
        forAll((s: S, a1: S, k: Int) => laws.transformMIdConsistent(s, gated(k, choose(a1)))),
      "universe == self :: children.flatMap(universe)" ->
        forAll((s: S) => laws.universeDecomposition(s)),
      "children.length == plate.length" ->
        forAll((s: S) => laws.childrenLengthMatchesPlateLength(s)),
    )
