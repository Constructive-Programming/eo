package dev.constructive.eo

import cats.{Functor, Representable}
import cats.instances.function.given
import org.specs2.mutable.Specification

import optics.{AffineFold, Optic, PickFold}
import data.{ModifyF, MultiFocus}
import forgetful.ForgetfulFunctor
import laws.{AffineFoldLaws, MultiFocusLaws}
import laws.data.ModifyFLaws

/** Negative fixtures: deliberately UNLAWFUL instances asserted to FAIL specific laws.
  *
  * Discipline registrations can only witness that lawful instances pass; they can never witness
  * that a law *discriminates*. A law-weakening mutation (a guard short-circuited to `false`, an
  * `&&` flipped to `||`) leaves every lawful instance passing, so the whole borrowed suite is blind
  * to it — mutation testing surfaced exactly three such survivors in `cats-eo-laws`. Each fixture
  * below is minimally unlawful: it violates precisely the clause the mutant weakens, so the law
  * method must return `false` here, and any weakening of that clause flips the result to `true` and
  * fails this spec.
  *
  * See site/docs/quality-assurance.md ("Mutation testing" caveats) for the full story.
  *
  * The last fixture is a *semantic* one rather than a mutation-killing one: it pins the rebuild
  * shape a retired API parameter could only have expressed (sample ONE written focus at a lead
  * index, rebuild every position from it), so the surviving pointwise-tabulation property is known
  * to discriminate that shape rather than merely to pass.
  */
class UnlawfulFixturesSpec extends Specification:

  "AffineFoldLaws.missIsEmpty rejects a Miss/foldMap-inconsistent instance" >> {
    // covers: laws/AffineFoldLaws.scala missIsEmpty guard (ConditionalExpression → false).
    // Stateful `to`: the law's getOption sees a Miss, its foldMap then sees a Hit — an
    // impure, unlawful optic (purity is an implicit law). Unmutated law: Miss branch
    // compares foldMap(identity) = 7 with Monoid[Int].empty = 0 → false. Weakened guard
    // skips the Miss branch entirely → vacuously true → this expectation fails.
    val brokenAf: PickFold[Int, Int] =
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

  "MultiFocusLaws.modifyIdentity rejects a lead-sampling rebuild" >> {
    // covers: laws/MultiFocusLaws.scala modifyIdentity against a `from` that samples ONE written
    // focus at a chosen "lead" index and rebuilds every position from it — the only semantics the
    // retired `MultiFocus.representableAt(F)(repr0)` parameter could have had. A lead rebuild
    // collapses the container to a constant, so the law must return false for it and true for
    // the shipped pointwise `MultiFocus.representable` on the same carrier, same input.
    //
    // The carrier's `S` is a STRUCTURAL container here, not `Function1[X0, *]`: the law
    // compares with `==`, which is reference equality on a function, so a function-typed `S`
    // would fail this law whatever the rebuild shape (that is also why the MF discipline
    // fixtures register List / ZipList / Const rather than the Grate carrier).
    case class Dup[A](a: A, b: A)
    given dupFunctor: Functor[Dup] with
      def map[A, B](fa: Dup[A])(f: A => B): Dup[B] = Dup(f(fa.a), f(fa.b))
    given dupRepresentable: Representable.Aux[Dup, Boolean] = new Representable[Dup]:
      type Representation = Boolean
      def F: Functor[Dup] = dupFunctor
      def index[A](fa: Dup[A]): Boolean => A = if _ then fa.a else fa.b
      def tabulate[A](f: Boolean => A): Dup[A] = Dup(f(true), f(false))

    val leadSampling: Optic[Dup[Int], Dup[Int], Int, Int, MultiFocus[Function1[Boolean, *]]] =
      new Optic[Dup[Int], Dup[Int], Int, Int, MultiFocus[Function1[Boolean, *]]]:
        type X = Unit
        def to(fa: Dup[Int]): MultiFocus[Function1[Boolean, *]][X, Int] =
          MultiFocus((), dupRepresentable.index(fa))
        def from(b: MultiFocus[Function1[Boolean, *]][X, Int]): Dup[Int] =
          val lead: Boolean = true // the "representative index"; sampled for the whole rebuild
          Dup(b.foci(lead), b.foci(lead))
    val lawful: Optic[Dup[Int], Dup[Int], Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[Dup, Int]

    // Named outside the law body so the instance's own `functor` member cannot take part in its
    // own initialisation.
    val boolFnFunctor: Functor[Function1[Boolean, *]] = summon[Functor[Function1[Boolean, *]]]
    def lawsFor(
        o: Optic[Dup[Int], Dup[Int], Int, Int, MultiFocus[Function1[Boolean, *]]]
    ) = new MultiFocusLaws[Dup[Int], Int, Function1[Boolean, *]]:
      given functor: Functor[Function1[Boolean, *]] = boolFnFunctor
      def multiFocus = o

    (lawsFor(leadSampling).modifyIdentity(Dup(1, 2)) must beFalse)
      .and(lawsFor(lawful).modifyIdentity(Dup(1, 2)) must beTrue)
      // The pin `MultiFocusFunction1Spec` holds the shipped factory to — that a write agrees
      // with the carrier instance's own `map` — discriminates the same shape.
      .and(
        (leadSampling.modify(_ * 10)(Dup(1, 2)) == dupFunctor.map(Dup(1, 2))(_ * 10)) must beFalse
      )
  }
