package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[OptionalLaws]].
  *
  * '''Hierarchy:''' inherits the three partial-read props from `internal.PartialReadLawsTests`
  * (`modify identity`, `compose modify`, `consistent getOption / modify-id`). Optional adds nothing
  * unique on top — the partial-read tier captures its full law surface.
  */
abstract class OptionalTests[S, A] extends internal.PartialReadLawsTests[S, A]:
  /** Laws under test. */
  def laws: OptionalLaws[S, A]

  protected def modifyIdentityFn: S => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (S, A => A, A => A) => Boolean = laws.composeModify
  protected def consistentGetOptionModifyIdFn: S => Boolean = laws.consistentGetOptionModifyId

  /** The "Optional" rule set. */
  def optional(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Optional",
      parent = Some(partialReadTier),
    )
