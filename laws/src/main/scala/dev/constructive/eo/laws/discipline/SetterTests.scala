package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[SetterLaws]].
  *
  * '''Hierarchy:''' `Setter` is the pure modify-tier law family — its props are exactly the four
  * shared with `Lens` / `Traversal`. The body adds nothing on top of [[internal.ReplaceLawsTests]];
  * it just declares the parent and renames the resulting ruleSet to "Setter".
  */
abstract class SetterTests[S, A] extends internal.ReplaceLawsTests[S, A]:
  def laws: SetterLaws[S, A]

  protected def modifyIdentityFn: S => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (S, A => A, A => A) => Boolean = laws.composeModify
  protected def replaceIdempotentFn: (S, A) => Boolean = laws.replaceIdempotent
  protected def consistentReplaceModifyFn: (S, A) => Boolean = laws.consistentReplaceModify

  def setter(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Setter",
      parent = Some(replaceTier),
    )
