package eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[OptionalLaws]]. */
abstract class OptionalTests[S, A] extends Laws:
  def laws: OptionalLaws[S, A]

  def optional(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Optional",
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "consistent getOption / modify-id" ->
        forAll((s: S) => laws.consistentGetOptionModifyId(s)),
    )
