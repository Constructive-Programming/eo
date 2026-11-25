package dev.constructive.eo

import cats.{Functor, Representable}
import org.specs2.mutable.Specification

import optics.{AffineFold, Lens, Optic, Optional, PickFold}
import data.{Affine, ModifyF}
import forgetful.ForgetfulFunctor
import laws.{AffineFoldLaws, LensLaws, OptionalLaws, TargetingLaws}
import laws.data.ModifyFLaws

/** Negative fixtures: deliberately UNLAWFUL instances asserted to FAIL specific laws.
  *
  * Discipline registrations can only witness that lawful instances pass; they can never witness
  * that a law *discriminates*. A law-weakening mutation (a guard short-circuited to `false`, an
  * `&&` flipped to `||`) leaves every lawful instance passing, so the whole borrowed suite is blind
  * to it — mutation testing surfaced exactly three such survivors in `eo-laws`. Each fixture below
  * is minimally unlawful: it violates precisely the clause the mutant weakens, so the law method
  * must return `false` here, and any weakening of that clause flips the result to `true` and fails
  * this spec.
  *
  * See site/docs/quality-assurance.md ("Mutation testing" caveats) for the full story.
  *
  * The last fixture is a *semantic* one rather than a mutation-killing one: it pins the rebuild
  * shape of a broken Indexed algebra (sample ONE written focus at a lead index, rebuild every
  * position from it), so the pointwise-tabulation property is known to discriminate that shape
  * rather than merely to pass.
  *
  * The final block is the #99 control, and it inverts the pattern: the fixtures there are LAWFUL
  * and asserted to PASS their intrinsic law family, because the defect they carry — a resolution
  * mechanism aimed at the wrong slot — is invisible to every intrinsic law. `TargetingLaws`, which
  * compares the optic against an extrinsic reference accessor, must reject them.
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

  "Indexed round-trip probe rejects a lead-sampling rebuild" >> {
    // A deliberately broken algebra collapses both positions to its lead.
    // This negative control ensures the full-grid identity probe detects that collapse.
    case class Dup[A](a: A, b: A)
    given dupFunctor: Functor[Dup] with
      def map[A, B](fa: Dup[A])(f: A => B): Dup[B] = Dup(f(fa.a), f(fa.b))
    given dupRepresentable: Representable.Aux[Dup, Boolean] = new Representable[Dup]:
      type Representation = Boolean
      def F: Functor[Dup] = dupFunctor
      def index[A](fa: Dup[A]): Boolean => A = if _ then fa.a else fa.b
      def tabulate[A](f: Boolean => A): Dup[A] = Dup(f(true), f(false))

    val leadSampling = optics
      .Indexed
      .apply[Dup[Int], Dup[Int], Int, Int, Boolean, Unit](fa => ((), dupRepresentable.index(fa)))(
        (_, values) => Dup(values(true), values(true))
      )
    val lawful = optics.Indexed.representable[Dup, Int](dupRepresentable)
    val source = Dup(1, 2)
    (leadSampling.from(leadSampling.to(source)) == source) must beFalse
    (lawful.from(lawful.to(source)) == source) must beTrue
    (leadSampling.modify((_, n) => n * 10)(source) == dupFunctor.map(source)(_ * 10)) must beFalse
    (lawful.modify((_, n) => n * 10)(source) == dupFunctor.map(source)(_ * 10)) must beTrue
  }

  // ---- #99: LAWFUL but MIS-TARGETED optics --------------------------------
  //
  // The discriminator for TargetingLaws. An optic whose resolution landed on the wrong slot is a
  // perfect optic ONTO THAT SLOT: every intrinsic law family (get-put, put-get, put-put, modify
  // identity / fusion, the seam round-trips) holds, so `LensTests` / `OptionalTests` cannot see
  // the defect. Only the comparison against an extrinsic reference — what the carrier's own
  // accessor says the focus is — can. Both fixtures below must keep the "lawful" half TRUE: if an
  // intrinsic law ever starts rejecting them the control has stopped proving the gap.

  "TargetingLaws rejects a lawful mis-targeted Lens that LensLaws accepts" >> {
    // Reads and writes `y`; the carrier's own accessor says the focus is `x`. Lawful as a lens
    // onto `y`, wrong as a lens onto `x`.
    final case class Pt(x: Int, y: Int)
    val misAimed: Optic[Pt, Pt, Int, Int, Tuple2] = Lens[Pt, Int](_.y, (p, a) => p.copy(y = a))
    val intrinsic = new LensLaws[Pt, Int]:
      val lens = misAimed
    val targeting = new TargetingLaws[Pt, Int, Tuple2]:
      val optic = misAimed
      def read(s: Pt) = Some(optic.get(s))
      val refGet = (s: Pt) => Some(s.x)
      val refReplace = (s: Pt, a: Int) => s.copy(x = a)
      val eqv = (a: Pt, b: Pt) => a == b

    val sample = Pt(1, 2)
    val lawful =
      intrinsic.getReplace(sample) &&
        intrinsic.replaceGet(sample, 9) &&
        intrinsic.replaceIdempotent(sample, 9) &&
        intrinsic.modifyIdentity(sample) &&
        intrinsic.composeModify(sample, _ + 1, _ * 2) &&
        intrinsic.consistentReplaceModify(sample, 9)

    lawful must beTrue
    targeting.readTargets(sample) must beFalse
    targeting.writeTargets(sample, 9) must beFalse
    targeting.modifyTargets(sample, _ + 1) must beFalse
  }

  "TargetingLaws rejects a lawful mis-targeted Optional that OptionalLaws accepts" >> {
    // The partial-focus shape of the same defect: hits on `b`, the reference says `a`.
    final case class Box(a: Int, b: Int)
    val misAimed: Optic[Box, Box, Int, Int, Affine] =
      Optional[Box, Box, Int, Int](
        getOrModify = s => Right(s.b),
        reverseGet = { case (s, v) => s.copy(b = v) },
      )
    val intrinsic = new OptionalLaws[Box, Int]:
      val optional = misAimed
    val targeting = new TargetingLaws[Box, Int, Affine]:
      val optic = misAimed
      def read(s: Box) = optic.getOption(s)
      val refGet = (s: Box) => Some(s.a)
      val refReplace = (s: Box, a: Int) => s.copy(a = a)
      val eqv = (a: Box, b: Box) => a == b

    val sample = Box(1, 2)
    val lawful =
      intrinsic.modifyIdentity(sample) &&
        intrinsic.composeModify(sample, _ + 1, _ * 2) &&
        intrinsic.consistentGetOptionModifyId(sample) &&
        intrinsic.getOption(sample) == Some(2)

    lawful must beTrue
    (targeting.readTargets(sample) must beFalse)
      .and(targeting.writeTargets(sample, 7) must beFalse)
      .and(targeting.modifyTargets(sample, _ * 2) must beFalse)
  }
