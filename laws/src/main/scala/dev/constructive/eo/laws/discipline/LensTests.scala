package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[LensLaws]].
  *
  * '''Hierarchy:''' inherits the four modify-tier props from [[internal.ReplaceLawsTests]] and adds
  * the two get/replace round-trip props (`getReplace`, `replaceGet`) that distinguish a Lens from a
  * Setter / Traversal.
  */
abstract class LensTests[S, A] extends internal.ReplaceLawsTests[S, A]:
  def laws: LensLaws[S, A]

  protected def modifyIdentityFn: S => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (S, A => A, A => A) => Boolean = laws.composeModify
  protected def replaceIdempotentFn: (S, A) => Boolean = laws.replaceIdempotent
  protected def consistentReplaceModifyFn: (S, A) => Boolean = laws.consistentReplaceModify

  def lens(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Lens",
      parent = Some(replaceTier),
      "get-replace" -> forAll((s: S) => laws.getReplace(s)),
      "replace-get" -> forAll((s: S, a: A) => laws.replaceGet(s, a)),
    )
