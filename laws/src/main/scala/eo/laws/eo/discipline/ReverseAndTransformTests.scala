package eo
package laws
package eo
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ReverseInvolutionLaws]]. */
abstract class ReverseInvolutionTests[S, A] extends Laws:
  def laws: ReverseInvolutionLaws[S, A]

  def reverseInvolution(using Arbitrary[S], Arbitrary[A]): RuleSet =
    new SimpleRuleSet(
      "Iso reverse is involutive",
      "reverse.reverse.get == get" ->
        forAll((s: S) => laws.reverseInvolutionGet(s)),
      "reverse.reverse.reverseGet == reverseGet" ->
        forAll((a: A) => laws.reverseInvolutionReverseGet(a)),
    )

/** Discipline `RuleSet` for [[TransformLaws]]. */
abstract class TransformTests[S, A, X0] extends Laws:
  def laws: TransformLaws[S, A, X0]

  def transform(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Lens transform / place / transfer",
      "transform(identity) == identity" ->
        forAll((t: S) => laws.transformIdentity(t)),
      "transfer(f)(t)(c) == place(f(c))(t)" ->
        forAll((t: S, c: A, f: A => A) => laws.transferIsCurriedPlace(t, c, f)),
      "place(a) == transform(_ => a)" ->
        forAll((t: S, a: A) => laws.placeIsTransformConst(t, a)),
    )

/** Discipline `RuleSet` for [[PutIsReverseGetLaws]]. */
abstract class PutIsReverseGetTests[S, A] extends Laws:
  def laws: PutIsReverseGetLaws[S, A]

  def putIsReverseGet(using Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Optic.put ≡ reverseGet ∘ pure",
      "put f a == reverseGet (f a)" ->
        forAll((a: A, f: A => A) => laws.putIsReverseGetCompose(a, f)),
    )
