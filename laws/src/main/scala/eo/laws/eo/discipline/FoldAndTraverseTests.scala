package eo
package laws
package eo
package discipline

import cats.Traverse
import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[FoldMapHomomorphismLaws]]. */
abstract class FoldMapHomomorphismTests[S, A, F[_, _]] extends Laws:
  def laws: FoldMapHomomorphismLaws[S, A, F]

  def foldMapHomomorphism(using
      Arbitrary[S], Arbitrary[A], Cogen[A],
      ForgetfulFold[F],
  ): RuleSet =
    new SimpleRuleSet(
      "foldMap is a Monoid homomorphism",
      "foldMap(f ⊕ g) == foldMap(f) ⊕ foldMap(g)" ->
        forAll((s: S, f: A => Int, g: A => Int) =>
          laws.foldMapHomomorphism(s, f, g)
        ),
      "foldMap(_ => mempty) == mempty" ->
        forAll((s: S) => laws.foldMapEmpty(s)),
    )

/** Discipline `RuleSet` for [[TraverseAllLaws]]. */
abstract class TraverseAllTests[T[_]: Traverse, A] extends Laws:
  def laws: TraverseAllLaws[T, A]

  def traverseAll(using Arbitrary[T[A]]): RuleSet =
    new SimpleRuleSet(
      "Optic.all on Forget[T]",
      "all(s).length == 1" ->
        forAll((s: T[A]) => laws.allHasLengthOne(s)),
      "all(s).head == s" ->
        forAll((s: T[A]) => laws.allHeadIsInput(s)),
    )

/** Discipline `RuleSet` for [[ForgetAllModifyLaws]]. */
abstract class ForgetAllModifyTests[T[_]: Traverse, A] extends Laws:
  def laws: ForgetAllModifyLaws[T, A]

  def allMap(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Forget[T]: all-then-map ≡ modify",
      "T.map(all(s).head)(f) == modify(f)(s)" ->
        forAll((s: T[A], f: A => A) => laws.allMapEqualsModify(s, f)),
    )
