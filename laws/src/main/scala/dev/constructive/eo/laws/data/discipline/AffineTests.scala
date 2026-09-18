package dev.constructive.eo.laws.data.discipline

import cats.Applicative
import dev.constructive.eo.accessor.{Graft, PartialAccessor}
import dev.constructive.eo.data.{Affine, Fst, Snd}
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulFunctor, ForgetfulTraverse}
import dev.constructive.eo.laws.data.AffineLaws
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[AffineLaws]]. */
abstract class AffineTests[X, A] extends Laws:
  /** Laws under test. */
  def laws: AffineLaws[X, A]

  /** The "Affine" rule set. */
  def affine(using
      Arbitrary[Affine[X, A]],
      Arbitrary[A],
      Arbitrary[Fst[X]],
      Arbitrary[Snd[X]],
      Cogen[A],
      ForgetfulFunctor[Affine],
      ForgetfulFold[Affine],
      ForgetfulTraverse[Affine, Applicative],
      Graft[Affine],
      PartialAccessor[Affine],
  ): RuleSet =
    new SimpleRuleSet(
      "Affine",
      "functor identity" ->
        forAll((fa: Affine[X, A]) => laws.functorIdentity(fa)),
      "functor composition" ->
        forAll((fa: Affine[X, A], f: A => A, g: A => A) => laws.functorComposition(fa, f, g)),
      "traverse[Id] identity" ->
        forAll((fa: Affine[X, A]) => laws.traverseIdentity(fa)),
      "done has no focus" ->
        forAll((fst: Fst[X]) => laws.doneHasNoFocus(fst)),
      "step has its focus" ->
        forAll((snd: Snd[X], a: A) => laws.stepHasFocus(snd, a)),
      "done is map-inert" ->
        forAll((fst: Fst[X], f: A => A) => laws.doneMapInert(fst, f)),
      "done folds empty" ->
        forAll((fst: Fst[X]) => laws.doneFoldEmpty(fst)),
    )
