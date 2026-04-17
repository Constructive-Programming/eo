package eo
package laws
package eo
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ChainPathIndependenceLaws]]. */
abstract class ChainPathIndependenceTests[S, A] extends Laws:
  def laws: ChainPathIndependenceLaws[S, A]

  def chainPathIndependence(using
      Arbitrary[S], Arbitrary[A], Cogen[A]
  ): RuleSet =
    new SimpleRuleSet(
      "Composer.chain path independence",
      "Forgetful→Tuple2→Affine ≡ Forgetful→Either→Affine on modify" ->
        forAll((s: S, f: A => A) => laws.chainPathIndependence(s, f)),
    )

/** Discipline `RuleSet` for [[ChainAccessorLaws]]. */
abstract class ChainAccessorTests[S, A, F[_, _], G[_, _], H[_, _]]
    extends Laws:
  def laws: ChainAccessorLaws[S, A, F, G, H]

  def chainAccessor(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Composer.chain preserves get",
      "chain(F→G→H).get(s) == optic.get(s)" ->
        forAll((s: S) => laws.chainPreservesGet(s)),
    )
