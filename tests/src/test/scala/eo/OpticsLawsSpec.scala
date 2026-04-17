package eo

import optics.{Iso, Lens, Prism, Optional, Setter, Traversal, Optic, Fold, Getter}
import data.{Affine, Forgetful, SetterF}
import data.Affine.given
import data.SetterF.given
import laws.{
  IsoLaws, LensLaws, PrismLaws, OptionalLaws, SetterLaws, TraversalLaws,
  GetterLaws, FoldLaws,
}
import laws.discipline.{
  IsoTests, LensTests, PrismTests, OptionalTests, SetterTests, TraversalTests,
  GetterTests, FoldTests,
}
import laws.data.{
  AffineLaws, SetterFLaws, VectLaws, PowerSeriesLaws, FixedTraversalLaws,
}
import laws.data.discipline.{
  AffineTests, SetterFTests, VectTests, PowerSeriesTests, FixedTraversalTests,
}
import laws.typeclass.{ForgetfulFunctorLaws, ForgetfulTraverseLaws}
import laws.typeclass.discipline.{
  ForgetfulFunctorTests, ForgetfulTraverseTests,
}

import cats.instances.list.given
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

// Arbitrary[Affine[(Int, String), Boolean]] — picks between the Fst-only
// left branch and the (Snd, A) right branch with equal weight.
private given arbAffineIntStringBool
    : Arbitrary[Affine[(Int, String), Boolean]] =
  Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[Int].map(Affine.ofLeft[(Int, String), Boolean]),
      for
        s <- Arbitrary.arbitrary[String]
        b <- Arbitrary.arbitrary[Boolean]
      yield Affine.ofRight[(Int, String), Boolean]((s, b)),
    )
  )

/** End-to-end check that EO's optics satisfy the Monocle-style discipline
  * laws. Each block constructs a concrete instance and feeds it to the
  * matching `*Tests` runner in `eo.laws.OpticLaws`.
  */
class OpticsLawsSpec extends Specification with Discipline:

  // ----- Iso: tuple swap (Int, String) <-> (String, Int) ----------

  val swapIso: Optic[(Int, String), (Int, String), (String, Int), (String, Int), Forgetful] =
    Iso[(Int, String), (Int, String), (String, Int), (String, Int)](
      _.swap,
      _.swap,
    )

  checkAll(
    "Iso[(Int,String), (String,Int)] — tuple swap",
    new IsoTests[(Int, String), (String, Int)]:
      val laws = new IsoLaws[(Int, String), (String, Int)]:
        val iso = swapIso
    .iso,
  )

  // ----- Lens: first / second on a pair --------------------------

  val firstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
    Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

  val secondLens: Optic[(Int, String), (Int, String), String, String, Tuple2] =
    Lens[(Int, String), String](_._2, (s, a) => (s._1, a))

  checkAll(
    "Lens[(Int,String), Int] — first projection",
    new LensTests[(Int, String), Int]:
      val laws = new LensLaws[(Int, String), Int]:
        val lens = firstLens
    .lens,
  )

  checkAll(
    "Lens[(Int,String), String] — second projection",
    new LensTests[(Int, String), String]:
      val laws = new LensLaws[(Int, String), String]:
        val lens = secondLens
    .lens,
  )

  // ----- Prism: even Int, isomorphic to its half ----------------
  //
  // getOrModify(n) = Right(n / 2) when n is even, else Left(n).
  // reverseGet(k)  = k * 2.
  //
  // Domain is restricted to [-10000, 10000] so the doubling never
  // overflows Int — the laws hold on the whole 32-bit range only if
  // you switch to a wider carrier, which is more about Int's algebra
  // than about the prism itself.

  val evenPrism: Optic[Int, Int, Int, Int, Either] =
    Prism[Int, Int](
      n => if n % 2 == 0 then Right(n / 2) else Left(n),
      k => k * 2,
    )

  locally {
    given Arbitrary[Int] = Arbitrary(Gen.choose(-10000, 10000))
    checkAll(
      "Prism[Int, Int] — even values (Int domain restricted to ±10000)",
      new PrismTests[Int, Int]:
        val laws = new PrismLaws[Int, Int]:
          val prism = evenPrism
      .prism,
    )
  }

  // ----- Optional: head of a non-empty pair (Int, List[Int]) -----
  //
  // getOrModify takes (h, hs) to Right(h); reverseGet(s, h') = (h', hs).

  val headOptional: Optic[(Int, List[Int]), (Int, List[Int]), Int, Int, Affine] =
    Optional[(Int, List[Int]), (Int, List[Int]), Int, Int, Affine](
      { case (h, _) => Right(h) },
      { case ((_, hs), h2) => (h2, hs) },
    )

  checkAll(
    "Optional[(Int,List[Int]), Int] — head of non-empty pair",
    new OptionalTests[(Int, List[Int]), Int]:
      val laws = new OptionalLaws[(Int, List[Int]), Int]:
        val optional = headOptional
    .optional,
  )

  // ----- Setter: maps `f` over both sides of a pair --------------

  val pairSetter: Optic[(Int, Int), (Int, Int), Int, Int, data.SetterF] =
    Setter[(Int, Int), (Int, Int), Int, Int](f => { case (a, b) => (f(a), f(b)) })

  checkAll(
    "Setter[(Int,Int), Int] — both pair components",
    new SetterTests[(Int, Int), Int]:
      val laws = new SetterLaws[(Int, Int), Int]:
        val setter = pairSetter
    .setter,
  )

  // ----- Traversal.each on List[Int] ------------------------------

  val listTraversal: Optic[List[Int], List[Int], Int, Int, data.Forget[List]] =
    Traversal.each[List, Int, Int]

  checkAll(
    "Traversal.each[List, Int]",
    new TraversalTests[List, Int]:
      val laws = new TraversalLaws[List, Int]:
        val traversal = listTraversal
    .traversal,
  )

  // ----- Getter: first projection and a synthetic one ------------

  val firstGetter: Optic[(Int, String), Unit, Int, Int, Forgetful] =
    Getter[(Int, String), Int](_._1)

  checkAll(
    "Getter[(Int,String), Int] — first projection",
    new GetterTests[(Int, String), Int]:
      val laws = new GetterLaws[(Int, String), Int]:
        val getter    = firstGetter
        val reference = (p: (Int, String)) => p._1
    .getter,
  )

  val lengthGetter: Optic[String, Unit, Int, Int, Forgetful] =
    Getter[String, Int](_.length)

  checkAll(
    "Getter[String, Int] — string length",
    new GetterTests[String, Int]:
      val laws = new GetterLaws[String, Int]:
        val getter    = lengthGetter
        val reference = (s: String) => s.length
    .getter,
  )

  // ----- Fold: Fold.apply over a Foldable + Fold.select ----------

  val listFold: Optic[List[Int], Unit, Int, Int, data.Forget[List]] =
    Fold[List, Int]

  checkAll(
    "Fold[List, Int]",
    new FoldTests[List[Int], Int, data.Forget[List]]:
      val laws = new FoldLaws[List[Int], Int, data.Forget[List]]:
        val fold = listFold
    .fold,
  )

  val optionFold: Optic[Option[Int], Unit, Int, Int, data.Forget[Option]] =
    Fold[Option, Int]

  checkAll(
    "Fold[Option, Int]",
    new FoldTests[Option[Int], Int, data.Forget[Option]]:
      val laws = new FoldLaws[Option[Int], Int, data.Forget[Option]]:
        val fold = optionFold
    .fold,
  )

  // ----- Fold.select: focuses on values matching a predicate ------

  val evenSelectFold: Optic[Int, Unit, Int, Int, data.Forget[Option]] =
    Fold.select[Int](_ % 2 == 0)

  checkAll(
    "Fold.select[Int](even)",
    new FoldTests[Int, Int, data.Forget[Option]]:
      val laws = new FoldLaws[Int, Int, data.Forget[Option]]:
        val fold = evenSelectFold
    .fold,
  )

  "Fold.select" should {
    "expose the value via .to when the predicate holds, None otherwise" >> {
      forAll((n: Int) =>
        evenSelectFold.to(n) == (if n % 2 == 0 then Some(n) else None)
      )
    }

    "for the always-false predicate, foldMap always returns Monoid.empty" >> {
      val neverFold: Optic[Int, Unit, Int, Int, data.Forget[Option]] =
        Fold.select[Int](_ => false)
      forAll((n: Int) => neverFold.foldMap[Int](identity)(n) == 0)
    }
  }

  // ----- Affine carrier laws --------------------------------------

  checkAll(
    "Affine[(Int, String), Boolean]",
    new AffineTests[(Int, String), Boolean]:
      val laws = new AffineLaws[(Int, String), Boolean] {}
    .affine,
  )

  // ----- SetterF carrier laws -------------------------------------

  checkAll(
    "SetterF[(Int, String), Boolean]",
    new SetterFTests[(Int, String), Boolean]:
      val laws = new SetterFLaws[(Int, String), Boolean] {}
    .setterF,
  )

  // ----- Vect carrier laws at N=3, A=Int --------------------------
  //
  // Arbitrary[Vect[3, Int]] is constructed by cons-ing three ints onto
  // a NilVect base; all four constructors (NilVect, ConsVect,
  // TConsVect, AdjacentVect) are exercised through the behaviour spec
  // in VectSpec.scala.

  private given arbVect3Int: Arbitrary[data.Vect[3, Int]] =
    Arbitrary(
      for
        a <- Arbitrary.arbitrary[Int]
        b <- Arbitrary.arbitrary[Int]
        c <- Arbitrary.arbitrary[Int]
      yield (a +: b +: c +: data.Vect.nil[0, Int]).asInstanceOf[data.Vect[3, Int]]
    )

  checkAll(
    "Vect[3, Int]",
    new VectTests[3, Int]:
      val laws = new VectLaws[3, Int]:
        val F = data.Vect.functor[3]
        val T = data.Vect.trav[3]
    .vect,
  )

  // ----- PowerSeries carrier laws ---------------------------------

  import data.PowerSeries
  import data.PowerSeries.given

  private given arbPowerSeries: Arbitrary[PowerSeries[(Int, Int), Int]] =
    Arbitrary(
      for
        x  <- Arbitrary.arbitrary[Int]
        a0 <- Arbitrary.arbitrary[Int]
        a1 <- Arbitrary.arbitrary[Int]
        a2 <- Arbitrary.arbitrary[Int]
      yield PowerSeries[(Int, Int), Int]((
        x,
        (a0 +: a1 +: a2 +: data.Vect.nil[0, Int])
          .asInstanceOf[data.Vect[Int, Int]],
      ))
    )

  checkAll(
    "PowerSeries[(Int, Int), Int]",
    new PowerSeriesTests[(Int, Int), Int]:
      val laws = new PowerSeriesLaws[(Int, Int), Int] {}
    .powerSeries,
  )

  // ----- FixedTraversal carrier laws ------------------------------

  import data.FixedTraversal
  import data.FixedTraversal.given

  private given arbFixedTrav2: Arbitrary[FixedTraversal[2][Unit, Int]] =
    Arbitrary(
      for
        a0 <- Arbitrary.arbitrary[Int]
        a1 <- Arbitrary.arbitrary[Int]
      yield (a0, a1, ()).asInstanceOf[FixedTraversal[2][Unit, Int]]
    )

  checkAll(
    "FixedTraversal[2][Unit, Int]",
    new FixedTraversalTests[2, Unit, Int]:
      val laws = new FixedTraversalLaws[2, Unit, Int] {}
    .fixedTraversal,
  )

  private given arbFixedTrav3: Arbitrary[FixedTraversal[3][Unit, Int]] =
    Arbitrary(
      for
        a0 <- Arbitrary.arbitrary[Int]
        a1 <- Arbitrary.arbitrary[Int]
        a2 <- Arbitrary.arbitrary[Int]
      yield (a0, a1, a2, ()).asInstanceOf[FixedTraversal[3][Unit, Int]]
    )

  checkAll(
    "FixedTraversal[3][Unit, Int]",
    new FixedTraversalTests[3, Unit, Int]:
      val laws = new FixedTraversalLaws[3, Unit, Int] {}
    .fixedTraversal,
  )

  // ----- Carrier type-class laws via representative fixtures ------
  //
  // Uses the same Arbitrary givens as the carrier-specific law blocks
  // above. The full fixture matrix (Tuple2, Either, Forget[F], Forgetful)
  // is partially covered — sufficient to witness that each instance
  // satisfies the shared ForgetfulFunctor / ForgetfulTraverse laws.
  // Expanding the matrix is a follow-up (0.1.1).

  // Tuple2 ForgetfulFunctor: reuses scalacheck's built-in Arbitrary[(Int, Int)].
  checkAll(
    "ForgetfulFunctor[Tuple2] on (Int, Int)",
    new ForgetfulFunctorTests[Tuple2, Int, Int]:
      val laws = new ForgetfulFunctorLaws[Tuple2, Int, Int] {}
    .forgetfulFunctor,
  )

  // Either ForgetfulFunctor: reuses built-in Arbitrary[Either[Int, Int]].
  checkAll(
    "ForgetfulFunctor[Either] on Either[Int, Int]",
    new ForgetfulFunctorTests[Either, Int, Int]:
      val laws = new ForgetfulFunctorLaws[Either, Int, Int] {}
    .forgetfulFunctor,
  )

  // Affine ForgetfulFunctor + ForgetfulTraverse: reuses arbAffineIntStringBool.
  checkAll(
    "ForgetfulFunctor[Affine] on Affine[(Int, String), Boolean]",
    new ForgetfulFunctorTests[Affine, (Int, String), Boolean]:
      val laws = new ForgetfulFunctorLaws[Affine, (Int, String), Boolean] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[Affine, Applicative] on Affine[(Int, String), Boolean]",
    new ForgetfulTraverseTests[Affine, (Int, String), Boolean]:
      val laws = new ForgetfulTraverseLaws[Affine, (Int, String), Boolean] {}
    .forgetfulTraverse,
  )

  // PowerSeries ForgetfulFunctor + ForgetfulTraverse: reuses arbPowerSeries.
  checkAll(
    "ForgetfulFunctor[PowerSeries] on PowerSeries[(Int, Int), Int]",
    new ForgetfulFunctorTests[PowerSeries, (Int, Int), Int]:
      val laws = new ForgetfulFunctorLaws[PowerSeries, (Int, Int), Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[PowerSeries, Applicative] on PowerSeries[(Int, Int), Int]",
    new ForgetfulTraverseTests[PowerSeries, (Int, Int), Int]:
      val laws = new ForgetfulTraverseLaws[PowerSeries, (Int, Int), Int] {}
    .forgetfulTraverse,
  )
