package dev.constructive.eo.laws.data.discipline

import cats.Applicative
import dev.constructive.eo.accessor.{Graft, PartialAccessor}
import dev.constructive.eo.data.{BiAffine, Fst, Snd}
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulFunctor, ForgetfulTraverse}
import dev.constructive.eo.laws.data.BiAffineLaws
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[BiAffineLaws]]. */
abstract class BiAffineTests[X, A] extends Laws:
  def laws: BiAffineLaws[X, A]

  def biAffine(using
      Arbitrary[BiAffine[X, A]],
      Arbitrary[A],
      Arbitrary[Fst[X]],
      Arbitrary[Snd[X]],
      Cogen[A],
      ForgetfulFunctor[BiAffine],
      ForgetfulFold[BiAffine],
      ForgetfulTraverse[BiAffine, Applicative],
      Graft[BiAffine],
      PartialAccessor[BiAffine],
  ): RuleSet =
    new SimpleRuleSet(
      "BiAffine",
      "functor identity" ->
        forAll((fa: BiAffine[X, A]) => laws.functorIdentity(fa)),
      "functor composition" ->
        forAll((fa: BiAffine[X, A], f: A => A, g: A => A) => laws.functorComposition(fa, f, g)),
      "traverse[Id] identity" ->
        forAll((fa: BiAffine[X, A]) => laws.traverseIdentity(fa)),
      "Done has no focus" ->
        forAll((fst: Fst[X]) => laws.doneHasNoFocus(fst)),
      "Step has its focus" ->
        forAll((snd: Snd[X], a: A) => laws.stepHasFocus(snd, a)),
      "Done is map-inert" ->
        forAll((fst: Fst[X], f: A => A) => laws.doneMapInert(fst, f)),
      "Done folds empty" ->
        forAll((fst: Fst[X]) => laws.doneFoldEmpty(fst)),
    )
