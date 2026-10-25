package dev.constructive.eo
package laws
package eo
package discipline

import cats.Applicative
import dev.constructive.eo.forgetful.*
import dev.constructive.eo.laws.discipline.internal.MultiShot
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ModifyAIdLaws]]. */
abstract class ModifyAIdTests[S, A, F[_, _]] extends Laws:
  /** Laws under test. */
  def laws: ModifyAIdLaws[S, A, F]

  /** The "modifyA at Id ≡ modify" rule set. */
  def modifyAId(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[F],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "modifyA at Id ≡ modify",
      "modifyA[Id] == modify" ->
        forAll((s: S, f: A => A) => laws.modifyAIdIsModify(s, f)),
    )

/** Discipline `RuleSet` for [[ModifyAListLaws]]. A multi-shot result grows as `2^n` in the number
  * of foci, so `S` is drawn at ScalaCheck size 8; a generator that ignores the size parameter
  * should stay that small.
  */
abstract class ModifyAListTests[S, A, F[_, _]] extends Laws:
  /** Laws under test. */
  def laws: ModifyAListLaws[S, A, F]

  /** The "modifyA at List ≡ cartesian traverse" rule set. */
  def modifyAList(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    val ss = MultiShot.capped[S]
    new SimpleRuleSet(
      "modifyA at List ≡ cartesian traverse",
      "modifyA[List] == Traverse[List] over the foci" ->
        forAll(ss, MultiShot.fn[A, List](identity))(laws.modifyAListIsCartesian(_, _)),
      "all(s).map(from) == List(s)" ->
        forAll(ss)(laws.allIsToRoundTrip(_)),
    )

/** Discipline `RuleSet` for [[ModifyAConstLaws]]. */
abstract class ModifyAConstTests[S, A, F[_, _]] extends Laws:
  /** Laws under test. */
  def laws: ModifyAConstLaws[S, A, F]

  /** The "modifyA at Const[M,*] ≡ foldMap" rule set. */
  def modifyAConst(using
      Arbitrary[S],
      Cogen[A],
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): RuleSet =
    new SimpleRuleSet(
      "modifyA at Const[M,*] ≡ foldMap",
      "modifyA[Const[Int,*]].getConst == foldMap" ->
        forAll((s: S, f: A => Int) => laws.modifyAConstIsFoldMap(s, f)),
    )
