package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[ModifyLaws]].
  *
  * '''Hierarchy:''' `Modify` is the pure modify-tier law family — its props are exactly the four
  * shared with `Lens` / `Traversal`. The body adds nothing on top of `internal.ReplaceLawsTests`;
  * it just declares the parent and renames the resulting ruleSet to "Modify".
  */
abstract class ModifyTests[S, A] extends internal.ReplaceLawsTests[S, A]:
  /** Laws under test. */
  def laws: ModifyLaws[S, A]

  protected def modifyIdentityFn: S => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (S, A => A, A => A) => Boolean = laws.composeModify
  protected def replaceIdempotentFn: (S, A) => Boolean = laws.replaceIdempotent
  protected def consistentReplaceModifyFn: (S, A) => Boolean = laws.consistentReplaceModify

  /** The "Modify" rule set. */
  def modify(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Modify",
      parent = Some(replaceTier),
    )
