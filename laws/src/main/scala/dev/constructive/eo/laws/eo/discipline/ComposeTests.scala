package dev.constructive.eo
package laws
package eo
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[LensComposeLaws]]. */
abstract class LensComposeTests[S, A, B] extends Laws:
  def laws: LensComposeLaws[S, A, B]

  def lensCompose(using Arbitrary[S], Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "Lens ∘ Lens",
      "composed get" -> forAll((s: S) => laws.composedGet(s)),
      "composed replace" ->
        forAll((s: S, b: B) => laws.composedReplace(s, b)),
    )

/** Discipline `RuleSet` for [[IsoComposeLaws]]. */
abstract class IsoComposeTests[S, A, B] extends Laws:
  def laws: IsoComposeLaws[S, A, B]

  def isoCompose(using Arbitrary[S], Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "Iso ∘ Iso",
      "composed get" -> forAll((s: S) => laws.composedGet(s)),
      "composed reverseGet" ->
        forAll((c: B) => laws.composedReverseGet(c)),
    )

/** Discipline `RuleSet` for [[PrismComposeLaws]]. */
abstract class PrismComposeTests[S, A, B] extends Laws:
  def laws: PrismComposeLaws[S, A, B]

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
  def laws: OptionalComposeLaws[S, A, B]

  def optionalCompose(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "Optional ∘ Optional",
      "composed getOption" ->
        forAll((s: S) => laws.composedGetOption(s)),
      "composed modify(identity) is identity" ->
        forAll((s: S) => laws.composedModifyIdentity(s)),
    )
