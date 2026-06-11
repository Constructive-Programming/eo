package dev.constructive.eo
package laws
package discipline

import cats.{Applicative, Functor}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet`s for [[UnfoldLaws]]. [[unfold]] is the core set every unfold satisfies
  * (`Functor[F]` suffices — pattern-functor algebras included); [[unfoldApplicative]] extends it
  * with the vestigial-read degradation law that only `Unfold.apply`-built (Applicative-carrier)
  * unfolds can state.
  */
abstract class UnfoldTests[T, B, F[_]] extends Laws:
  def laws: UnfoldLaws[T, B, F]

  def unfold(using
      Arbitrary[F[B]],
      Arbitrary[F[Int]],
      Arbitrary[B],
      Cogen[T],
      Cogen[Int],
      Functor[F],
  ): RuleSet =
    new SimpleRuleSet(
      "Unfold",
      "embed consistent with reference" ->
        forAll((fb: F[B]) => laws.embedConsistent(fb)),
      "Review post-compose coherent (f ∘ embed)" ->
        forAll((f: T => Int, fb: F[B]) => laws.postComposeCoherent(f, fb)),
      "Review pre-compose coherent (embed ∘ map(g))" ->
        forAll((g: Int => B, fi: F[Int]) => laws.preComposeCoherent(g, fi)),
    )

  def unfoldApplicative(using
      Arbitrary[F[B]],
      Arbitrary[F[Int]],
      Arbitrary[B],
      Cogen[T],
      Cogen[Int],
      Applicative[F],
  ): RuleSet =
    new DefaultRuleSet(
      "Unfold (Applicative)",
      Some(unfold),
      "vestigial read degrades to the singleton layer" ->
        forAll((b: B) => laws.vestigialSingleton(b)),
    )
