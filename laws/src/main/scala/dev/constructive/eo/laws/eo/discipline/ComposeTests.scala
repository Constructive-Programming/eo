package dev.constructive.eo
package laws
package eo
package discipline

import dev.constructive.eo.accessor.Accessor
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
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

// ===== Capability-keyed compose-coherence rule sets =====

/** Discipline `RuleSet` for [[ComposedGetLaws]] — capability-keyed `get` distribution, usable at
  * any cross-family cell whose three legs all read totally.
  */
abstract class ComposedGetTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: ComposedGetLaws[S, A, B]

  /** The capability "composed get" rule set. */
  def composedGet(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "composed get (capability)",
      "composed get" -> forAll((s: S) => laws.composedGet(s)),
    )

/** Discipline `RuleSet` for [[ComposedGetOptionLaws]] — capability-keyed partial-read distribution.
  */
abstract class ComposedGetOptionTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: ComposedGetOptionLaws[S, A, B]

  /** The capability "composed getOption" rule set. */
  def composedGetOption(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "composed getOption (capability)",
      "composed getOption" -> forAll((s: S) => laws.composedGetOption(s)),
    )

/** Discipline `RuleSet` for [[ComposedFoldMapLaws]] — capability-keyed fold distribution, stated at
  * the additive `Int` monoid (sufficient to witness the homomorphism).
  */
abstract class ComposedFoldMapTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: ComposedFoldMapLaws[S, A, B]

  /** The capability "composed foldMap" rule set. */
  def composedFoldMap(using Arbitrary[S], Cogen[B]): RuleSet =
    new SimpleRuleSet(
      "composed foldMap (capability)",
      "composed foldMap" ->
        forAll((s: S, f: B => Int) => laws.composedFoldMap(f)(s)),
    )

/** Discipline `RuleSet` for [[ComposedReverseGetLaws]] — capability-keyed build distribution. */
abstract class ComposedReverseGetTests[S, A, B] extends Laws:
  /** Laws under test. */
  def laws: ComposedReverseGetLaws[S, A, B]

  /** The capability "composed reverseGet" rule set. */
  def composedReverseGet(using Arbitrary[B]): RuleSet =
    new SimpleRuleSet(
      "composed reverseGet (capability)",
      "composed reverseGet" -> forAll((c: B) => laws.composedReverseGet(c)),
    )

/** Discipline `RuleSet` for [[ComposeAssociativityLaws]]. */
abstract class ComposeAssociativityTests[S, A, F[_, _]] extends Laws:
  /** Laws under test. */
  def laws: ComposeAssociativityLaws[S, A, F]

  /** Associativity of `modify` alone — for carriers with only `ForgetfulFunctor[F]`. */
  def associativeCompose(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "compose associativity",
      "associative modify" ->
        forAll((s: S, f: A => A) => laws.associativeModify(s, f)),
    )

  /** Associativity of `modify` and `get` — for total-read carriers (`Accessor[F]`). */
  def associativeComposeWithGet(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      Accessor[F],
  ): RuleSet =
    new SimpleRuleSet(
      "compose associativity (readable)",
      "associative modify" ->
        forAll((s: S, f: A => A) => laws.associativeModify(s, f)),
      "associative get" ->
        forAll((s: S) => laws.associativeGet(s)),
    )
