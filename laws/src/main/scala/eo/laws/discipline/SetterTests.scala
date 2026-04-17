package eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[SetterLaws]]. */
abstract class SetterTests[S, A] extends Laws:
  def laws: SetterLaws[S, A]

  def setter(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Setter",
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "replace idempotent" ->
        forAll((s: S, a: A) => laws.replaceIdempotent(s, a)),
      "consistent replace-modify" ->
        forAll((s: S, a: A) => laws.consistentReplaceModify(s, a)),
    )
