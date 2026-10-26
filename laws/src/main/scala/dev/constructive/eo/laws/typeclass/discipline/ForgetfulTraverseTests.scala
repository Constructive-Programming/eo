package dev.constructive.eo.laws.typeclass.discipline

import cats.Applicative
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulTraverse}
import dev.constructive.eo.laws.discipline.internal.MultiShot
import dev.constructive.eo.laws.typeclass.ForgetfulTraverseLaws
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet`s for [[ForgetfulTraverseLaws]]. */
abstract class ForgetfulTraverseTests[F[_, _], X, A] extends Laws:
  /** Laws under test. */
  def laws: ForgetfulTraverseLaws[F, X, A]

  /** The "ForgetfulTraverse" rule set — identity at `Id` only. */
  def forgetfulTraverse(using
      Arbitrary[F[X, A]],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "ForgetfulTraverse",
      "traverse[Id] identity" ->
        forAll((fa: F[X, A]) => laws.traverseIdentity(fa)),
    )

  /** The "ForgetfulTraverse (multi-shot)" rule set — [[forgetfulTraverse]] plus sequential and
    * parallel composition and agreement with cats' `Traverse[List]` under the multi-shot `List` /
    * `Vector` effects. A multi-shot result grows as `2^n` in the number of foci, so the structure
    * is drawn at ScalaCheck size 8; a generator that ignores the size parameter should stay that
    * small.
    */
  def forgetfulTraverseMultiShot(using
      Arbitrary[F[X, A]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulTraverse[F, Applicative],
      ForgetfulFold[F],
  ): RuleSet =
    val fas: Gen[F[X, A]] = MultiShot.capped[F[X, A]]
    val listF: Gen[A => List[A]] = MultiShot.fn[A, List](identity)
    val vectorF: Gen[A => Vector[A]] = MultiShot.fn[A, Vector](_.toVector)
    val optionF: Gen[A => Option[A]] = Gen.function1(Gen.option(Arbitrary.arbitrary[A]))
    new DefaultRuleSet(
      "ForgetfulTraverse (multi-shot)",
      Some(forgetfulTraverse),
      "traverse sequential composition (Nested[List, Option])" ->
        forAll(fas, listF, optionF)(laws.traverseSequentialComposition(_, _, _)),
      "traverse parallel composition (Tuple2K[List, Vector])" ->
        forAll(fas, listF, vectorF)(laws.traverseParallelComposition(_, _, _)),
      "traverse[List] == Traverse[List] over the foci" ->
        forAll(fas, listF)(laws.traverseMultiShotAgreesWithFoci(_, _)),
    )
