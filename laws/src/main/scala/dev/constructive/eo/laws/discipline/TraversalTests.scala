package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}

/** Discipline `RuleSet` for [[TraversalLaws]].
  *
  * '''Hierarchy:''' Traversal is also pure modify-tier — the four props are inherited verbatim from
  * [[internal.ReplaceLawsTests]], with no Traversal-specific extras at this layer (the `Optic.all`
  * / `foldMap` laws live in `eo.discipline.TraverseAllTests` and friends instead).
  */
abstract class TraversalTests[T[_], A] extends internal.ReplaceLawsTests[T[A], A]:
  def laws: TraversalLaws[T, A]

  protected def modifyIdentityFn: T[A] => Boolean = laws.modifyIdentity
  protected def composeModifyFn: (T[A], A => A, A => A) => Boolean = laws.composeModify
  protected def replaceIdempotentFn: (T[A], A) => Boolean = laws.replaceIdempotent
  protected def consistentReplaceModifyFn: (T[A], A) => Boolean = laws.consistentReplaceModify

  def traversal(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): RuleSet =
    new DefaultRuleSet(
      name = "Traversal",
      parent = Some(replaceTier),
    )
