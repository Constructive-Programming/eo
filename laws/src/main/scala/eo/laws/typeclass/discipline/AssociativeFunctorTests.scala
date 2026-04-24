package eo
package laws
package typeclass
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[AssociativeFunctorLaws]]. */
abstract class AssociativeFunctorTests[S, A, C, F[_, _]] extends Laws:

  def laws: AssociativeFunctorLaws[S, A, C, F]

  def associativeFunctor(using
      Arbitrary[S],
      Cogen[C],
      Arbitrary[C => C],
  ): RuleSet =
    new SimpleRuleSet(
      "AssociativeFunctor",
      "compose-modify distributes (A1)" ->
        forAll((s: S, f: C => C) => laws.composeModifyDistributes(s, f)),
      "compose-modify identity (A2)" ->
        forAll((s: S) => laws.composeModifyIdentity(s)),
    )
