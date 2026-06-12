package dev.constructive.eo

import org.specs2.mutable.Specification

import optics.AffineFold
import data.ModifyF
import forgetful.ForgetfulFunctor
import laws.AffineFoldLaws
import laws.data.ModifyFLaws

/** Negative fixtures: deliberately UNLAWFUL instances asserted to FAIL specific laws.
  *
  * Discipline registrations can only witness that lawful instances pass; they can never witness
  * that a law *discriminates*. A law-weakening mutation (a guard short-circuited to `false`, an
  * `&&` flipped to `||`) leaves every lawful instance passing, so the whole borrowed suite is
  * blind to it — mutation testing surfaced exactly three such survivors in `cats-eo-laws`. Each
  * fixture below is minimally unlawful: it violates precisely the clause the mutant weakens, so
  * the law method must return `false` here, and any weakening of that clause flips the result to
  * `true` and fails this spec.
  *
  * See site/docs/quality-assurance.md ("Mutation testing" caveats) for the full story.
  */
class UnlawfulFixturesSpec extends Specification:

  "AffineFoldLaws.missIsEmpty rejects a Miss/foldMap-inconsistent instance" >> {
    // covers: laws/AffineFoldLaws.scala missIsEmpty guard (ConditionalExpression → false).
    // Stateful `to`: the law's getOption sees a Miss, its foldMap then sees a Hit — an
    // impure, unlawful optic (purity is an implicit law). Unmutated law: Miss branch
    // compares foldMap(identity) = 7 with Monoid[Int].empty = 0 → false. Weakened guard
    // skips the Miss branch entirely → vacuously true → this expectation fails.
    val brokenAf: AffineFold[Int, Int] =
      var calls = 0
      AffineFold[Int, Int] { _ =>
        calls += 1
        if calls == 1 then None else Some(7)
      }
    val laws = new AffineFoldLaws[Int, Int]:
      def af = brokenAf
    laws.missIsEmpty(0, identity) must beFalse
  }

  "ModifyFLaws.functorIdentity rejects a continuation-corrupting functor" >> {
    // covers: laws/data/ModifyFLaws.scala functorIdentity (&& → ||).
    // This functor preserves the source (first conjunct TRUE) but discards the mapped
    // continuation (second conjunct FALSE): && → false, || → true. `null.asInstanceOf[C]`
    // unboxes to 0 for C = Int, and the fixture's fn returns 7 ≠ 0.
    given broken: ForgetfulFunctor[ModifyF] with
      def map[X, B, C](fa: ModifyF[X, B], f: B => C): ModifyF[X, C] =
        ModifyF((fa.modifier._1, _ => null.asInstanceOf[C]))
    val laws = new ModifyFLaws[(Int, String), Int] {}
    laws.functorIdentity(1, _ => 7, "x") must beFalse
  }

  "ModifyFLaws.functorComposition rejects a first-call-corrupting functor" >> {
    // covers: laws/data/ModifyFLaws.scala functorComposition (&& → ||).
    // Corrupts only the FIRST map call — the inner map of the law's two-step lhs — so
    // lhs's continuation diverges from rhs's while both sources stay equal: first
    // conjunct TRUE, second FALSE. lhs._2(x) = g(null→0 + 1) = 2; rhs._2(x) =
    // g(f(fn("abc"))) = 3 + 1 + 1 = 5.
    given broken: ForgetfulFunctor[ModifyF] with
      var calls = 0
      def map[X, B, C](fa: ModifyF[X, B], f: B => C): ModifyF[X, C] =
        calls += 1
        if calls == 1 then ModifyF((fa.modifier._1, _ => null.asInstanceOf[C]))
        else ModifyF((fa.modifier._1, x => f(fa.modifier._2(x))))
    val laws = new ModifyFLaws[(Int, String), Int] {}
    laws.functorComposition(1, _.length, _ + 1, _ + 1, "abc") must beFalse
  }
