package eo
package laws
package discipline

import cats.Functor
import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[TraversalLaws]]. */
abstract class TraversalTests[T[_]: Functor, A] extends Laws:
  def laws: TraversalLaws[T, A]

  def traversal(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Traversal",
      "modify identity" -> forAll((s: T[A]) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: T[A], f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "replace idempotent" ->
        forAll((s: T[A], a: A) => laws.replaceIdempotent(s, a)),
      "consistent replace-modify" ->
        forAll((s: T[A], a: A) => laws.consistentReplaceModify(s, a)),
    )
