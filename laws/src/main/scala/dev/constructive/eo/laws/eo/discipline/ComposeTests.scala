package dev.constructive.eo
package laws
package eo
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[LensComposeLaws]]. */
abstract class LensComposeTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: LensComposeLaws[S, A, B]

  /** The "Lens ∘ Lens" rule set. */
  def lensCompose(using Arbitrary[S], Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "Lens ∘ Lens",
      "composed get" -> forAll((s: S) => laws.composedGet(s)),
      "composed replace" ->
        forAll((s: S, b: B) => laws.composedReplace(s, b)),
    )

/** Discipline `RuleSet` for [[IsoComposeLaws]]. */
abstract class IsoComposeTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: IsoComposeLaws[S, A, B]

  /** The "Iso ∘ Iso" rule set. */
  def isoCompose(using Arbitrary[S], Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "Iso ∘ Iso",
      "composed get" -> forAll((s: S) => laws.composedGet(s)),
      "composed reverseGet" ->
        forAll((c: B) => laws.composedReverseGet(c)),
    )

/** Discipline `RuleSet` for [[PrismComposeLaws]]. */
abstract class PrismComposeTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: PrismComposeLaws[S, A, B]

  /** The "Prism ∘ Prism" rule set. */
  def prismCompose(using Arbitrary[S], Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "Prism ∘ Prism",
      "composed getOption" ->
        forAll((s: S) => laws.composedGetOption(s)),
      "composed reverseGet" ->
        forAll((b: B) => laws.composedReverseGet(b)),
    )

/** Discipline `RuleSet` for [[OptionalComposeLaws]]. */
abstract class OptionalComposeTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: OptionalComposeLaws[S, A, B]

  /** The "Optional ∘ Optional" rule set. */
  def optionalCompose(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Optional ∘ Optional",
      "composed getOption" ->
        forAll((s: S) => laws.composedGetOption(s)),
      "composed modify(identity) is identity" ->
        forAll((s: S) => laws.composedModifyIdentity(s)),
    )
