package eo.laws.data.discipline

import eo.data.Vect
import eo.laws.data.VectLaws

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[VectLaws]]. */
abstract class VectTests[N <: Int, A] extends Laws:
  def laws: VectLaws[N, A]

  def vect(using Arbitrary[Vect[N, A]], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Vect",
      "functor identity" ->
        forAll((xs: Vect[N, A]) => laws.functorIdentity(xs)),
      "functor composition" ->
        forAll((xs: Vect[N, A], f: A => A, g: A => A) => laws.functorComposition(xs, f, g)),
      "traverse[Id] identity" ->
        forAll((xs: Vect[N, A]) => laws.traverseIdentity(xs)),
      "map preserves size" ->
        forAll((xs: Vect[N, A], f: A => A) => laws.mapPreservesSize(xs, f)),
    )
