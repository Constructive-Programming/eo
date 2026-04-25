package dev.constructive.eo
package laws
package eo
package discipline

import cats.Applicative
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ModifyAIdLaws]]. */
abstract class ModifyAIdTests[S, A, F[_, _]] extends Laws:
  def laws: ModifyAIdLaws[S, A, F]

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

/** Discipline `RuleSet` for [[ModifyAConstLaws]]. */
abstract class ModifyAConstTests[S, A, F[_, _]] extends Laws:
  def laws: ModifyAConstLaws[S, A, F]

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
