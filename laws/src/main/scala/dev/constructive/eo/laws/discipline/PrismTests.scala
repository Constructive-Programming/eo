package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[PrismLaws]].
  *
  * '''Hierarchy:''' inherits the three partial-read props from [[internal.PartialReadLawsTests]]
  * and adds the two reverseGet round-trip props (`partial round trip one way` and
  * `round trip other way`) that are specific to Prism.
  */
abstract class PrismTests[S, A] extends internal.PartialReadLawsTests[S, A]:
  def laws: PrismLaws[S, A]

  protected def modifyIdentityFn: S => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (S, A => A, A => A) => Boolean = laws.composeModify
  protected def consistentGetOptionModifyIdFn: S => Boolean = laws.consistentGetOptionModifyId

  def prism(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Prism",
      parent = Some(partialReadTier),
      "partial round trip one way" ->
        forAll((s: S) => laws.partialRoundTripOneWay(s)),
      "round trip other way" ->
        forAll((a: A) => laws.roundTripOtherWay(a)),
    )
