package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[GrateLaws]]. */
abstract class GrateTests[S, A] extends Laws:
  def laws: GrateLaws[S, A]

  def grate(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Grate",
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "replace idempotent" ->
        forAll((s: S, a: A) => laws.replaceIdempotent(s, a)),
    )
