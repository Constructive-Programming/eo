package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[TraversalLaws]]. */
abstract class TraversalTests[T[_], A] extends Laws:
  def laws: TraversalLaws[T, A]

  def traversal(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Traversal",
      OpticLawProps.modifyIdentity[T[A]](laws.modifyIdentity),
      OpticLawProps.composeModify[T[A], A](laws.composeModify),
      OpticLawProps.replaceIdempotent[T[A], A](laws.replaceIdempotent),
      OpticLawProps.consistentReplaceModify[T[A], A](laws.consistentReplaceModify),
    )
