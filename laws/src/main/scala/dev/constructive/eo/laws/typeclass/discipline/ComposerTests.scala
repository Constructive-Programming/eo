package dev.constructive.eo
package laws
package typeclass
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ComposerPathIndependenceLaws]]. */
abstract class ComposerPathIndependenceTests[S, A] extends Laws:
  /** Laws under test. */
  def laws: ComposerPathIndependenceLaws[S, A]

  /** The "Composer.chain path independence" rule set. */
  def composerPathIndependence(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
  ): RuleSet =
    new SimpleRuleSet(
      "Composer.chain path independence",
      "Direct→Tuple2→Affine ≡ Direct→Either→Affine on modify" ->
        forAll((s: S, f: A => A) => laws.pathIndependence(s, f)),
    )

/** Discipline `RuleSet` for [[ComposerPreservesGetLaws]]. */
abstract class ComposerPreservesGetTests[S, A, F[_, _], G[_, _], H[_, _]] extends Laws:
  /** Laws under test. */
  def laws: ComposerPreservesGetLaws[S, A, F, G, H]

  /** The "Composer.chain preserves get" rule set. */
  def composerPreservesGet(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Composer.chain preserves get",
      "chain(F→G→H).get(s) == optic.get(s)" ->
        forAll((s: S) => laws.preservesGet(s)),
    )
