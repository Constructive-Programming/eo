package eo
package laws
package eo
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet`s for [[MorphLaws]]. Two entry points —
  * `morphPreservesModify` and `morphPreservesGet` — because the two
  * laws require disjoint capabilities on `F` / `G` (ForgetfulFunctor
  * vs. Accessor) and not every carrier pair satisfies both.
  */
abstract class MorphTests[S, A, F[_, _], G[_, _]] extends Laws:
  def laws: MorphLaws[S, A, F, G]

  def morphPreservesModify(using
      Arbitrary[S], Arbitrary[A], Cogen[A],
      ForgetfulFunctor[F], ForgetfulFunctor[G],
  ): RuleSet =
    new SimpleRuleSet(
      "Morph preserves modify",
      "modify equivalence" ->
        forAll((s: S, f: A => A) => laws.morphPreservesModify(s, f)),
    )

  def morphPreservesGet(using
      Arbitrary[S], Arbitrary[A],
      data.Accessor[F], data.Accessor[G],
  ): RuleSet =
    new SimpleRuleSet(
      "Morph preserves get",
      "get equivalence" ->
        forAll((s: S) => laws.morphPreservesGet(s)),
    )
