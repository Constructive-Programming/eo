package eo
package laws
package typeclass
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ComposerPathIndependenceLaws]]. */
abstract class ComposerPathIndependenceTests[S, A] extends Laws:
  def laws: ComposerPathIndependenceLaws[S, A]

  def composerPathIndependence(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
  ): RuleSet =
    new SimpleRuleSet(
      "Composer.chain path independence",
      "Forgetful→Tuple2→Affine ≡ Forgetful→Either→Affine on modify" ->
        forAll((s: S, f: A => A) => laws.pathIndependence(s, f)),
    )

/** Discipline `RuleSet` for [[ComposerPreservesGetLaws]]. */
abstract class ComposerPreservesGetTests[S, A, F[_, _], G[_, _], H[_, _]] extends Laws:
  def laws: ComposerPreservesGetLaws[S, A, F, G, H]

  def composerPreservesGet(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Composer.chain preserves get",
      "chain(F→G→H).get(s) == optic.get(s)" ->
        forAll((s: S) => laws.preservesGet(s)),
    )
