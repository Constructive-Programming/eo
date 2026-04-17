package eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[LensLaws]]. */
abstract class LensTests[S, A] extends Laws:
  def laws: LensLaws[S, A]

  def lens(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Lens",
      "get-replace" -> forAll((s: S) => laws.getReplace(s)),
      "replace-get" -> forAll((s: S, a: A) => laws.replaceGet(s, a)),
      "replace idempotent" ->
        forAll((s: S, a: A) => laws.replaceIdempotent(s, a)),
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "consistent replace-modify" ->
        forAll((s: S, a: A) => laws.consistentReplaceModify(s, a)),
    )
