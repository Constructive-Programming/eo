package eo

import optics.{AffineFold, Fold, Getter, Iso, Lens, Optic, Optional, Prism, Setter, Traversal}
import data.{Affine, Forget, Forgetful, Grate, Kaleidoscope, SetterF}
import data.Affine.given
import data.Forget.given
import data.Grate.given
import data.Kaleidoscope.given
import data.SetterF.given
import laws.{
  AffineFoldLaws,
  FoldLaws,
  GetterLaws,
  GrateLaws,
  IsoLaws,
  KaleidoscopeLaws,
  LensLaws,
  OptionalLaws,
  PrismLaws,
  SetterLaws,
  TraversalLaws,
}
import laws.discipline.{
  AffineFoldTests,
  FoldTests,
  GetterTests,
  GrateTests,
  IsoTests,
  KaleidoscopeTests,
  LensTests,
  OptionalTests,
  PrismTests,
  SetterTests,
  TraversalTests,
}
import laws.data.{AffineLaws, FixedTraversalLaws, PowerSeriesLaws, SetterFLaws}
import laws.data.discipline.{AffineTests, FixedTraversalTests, PowerSeriesTests, SetterFTests}
import laws.typeclass.{ForgetfulFunctorLaws, ForgetfulTraverseLaws}
import laws.typeclass.discipline.{ForgetfulFunctorTests, ForgetfulTraverseTests}

import cats.instances.list.given
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

// Arbitrary[Affine[(Int, String), Boolean]] — picks between the Fst-only
// left branch and the (Snd, A) right branch with equal weight.
private given arbAffineIntStringBool: Arbitrary[Affine[(Int, String), Boolean]] =
  Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[Int].map(Affine.ofLeft[(Int, String), Boolean]),
      for
        s <- Arbitrary.arbitrary[String]
        b <- Arbitrary.arbitrary[Boolean]
      yield Affine.ofRight[(Int, String), Boolean]((s, b)),
    )
  )

/** End-to-end check that EO's optics satisfy the Monocle-style discipline laws. Each block
  * constructs a concrete instance and feeds it to the matching `*Tests` runner in
  * `eo.laws.OpticLaws`.
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

  // ----- Traversal.forEach on List[Int] ---------------------------

  val listTraversal: Optic[List[Int], List[Int], Int, Int, data.Forget[List]] =
    Traversal.forEach[List, Int, Int]

  checkAll(
    "Traversal.forEach[List, Int]",
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
        val getter = firstGetter
        val reference = (p: (Int, String)) => p._1
    .getter,
  )

  val lengthGetter: Optic[String, Unit, Int, Int, Forgetful] =
    Getter[String, Int](_.length)

  checkAll(
    "Getter[String, Int] — string length",
    new GetterTests[String, Int]:
      val laws = new GetterLaws[String, Int]:
        val getter = lengthGetter
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
      forAll((n: Int) => evenSelectFold.to(n) == (if n % 2 == 0 then Some(n) else None))
    }

    "for the always-false predicate, foldMap always returns Monoid.empty" >> {
      val neverFold: Optic[Int, Unit, Int, Int, data.Forget[Option]] =
        Fold.select[Int](_ => false)
      forAll((n: Int) => neverFold.foldMap[Int](identity)(n) == 0)
    }
  }

  // ----- AffineFold: partial projection + filtering select -------

  val adultAgeAF: AffineFold[(Int, String), Int] =
    AffineFold { case (age, _) => Option.when(age >= 18)(age) }

  checkAll(
    "AffineFold[(Int,String), Int] — age ≥ 18",
    new AffineFoldTests[(Int, String), Int]:
      val laws = new AffineFoldLaws[(Int, String), Int]:
        val af = adultAgeAF
    .affineFold,
  )

  val evenSelectAF: AffineFold[Int, Int] = AffineFold.select[Int](_ % 2 == 0)

  checkAll(
    "AffineFold.select[Int](even)",
    new AffineFoldTests[Int, Int]:
      val laws = new AffineFoldLaws[Int, Int]:
        val af = evenSelectAF
    .affineFold,
  )

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

  // ----- PowerSeries carrier laws ---------------------------------

  import data.PowerSeries
  import data.PowerSeries.given
  import data.PSVec

  private given arbPowerSeries: Arbitrary[PowerSeries[(Int, Int), Int]] =
    Arbitrary(
      for
        x <- Arbitrary.arbitrary[Int]
        a0 <- Arbitrary.arbitrary[Int]
        a1 <- Arbitrary.arbitrary[Int]
        a2 <- Arbitrary.arbitrary[Int]
      yield
        val arr = new Array[AnyRef](3)
        arr(0) = a0.asInstanceOf[AnyRef]
        arr(1) = a1.asInstanceOf[AnyRef]
        arr(2) = a2.asInstanceOf[AnyRef]
        PowerSeries[(Int, Int), Int](x, PSVec.unsafeWrap[Int](arr))
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

  private given arbFixedTrav4: Arbitrary[FixedTraversal[4][Unit, Int]] =
    Arbitrary(
      for
        a0 <- Arbitrary.arbitrary[Int]
        a1 <- Arbitrary.arbitrary[Int]
        a2 <- Arbitrary.arbitrary[Int]
        a3 <- Arbitrary.arbitrary[Int]
      yield (a0, a1, a2, a3, ()).asInstanceOf[FixedTraversal[4][Unit, Int]]
    )

  checkAll(
    "FixedTraversal[4][Unit, Int]",
    new FixedTraversalTests[4, Unit, Int]:
      val laws = new FixedTraversalLaws[4, Unit, Int] {}
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

  // Forget[List] ForgetfulFunctor + ForgetfulTraverse — exercises
  // Forgetful.scala's traverse2 and bifunctor paths that pure Forgetful
  // fixtures don't land.
  checkAll(
    "ForgetfulFunctor[Forget[List]] on Forget[List][Unit, Int]",
    new ForgetfulFunctorTests[Forget[List], Unit, Int]:
      val laws = new ForgetfulFunctorLaws[Forget[List], Unit, Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[Forget[List], Applicative] on Forget[List][Unit, Int]",
    new ForgetfulTraverseTests[Forget[List], Unit, Int]:
      val laws = new ForgetfulTraverseLaws[Forget[List], Unit, Int] {}
    .forgetfulTraverse,
  )

  // Either ForgetfulTraverse[Applicative] — exercises
  // ForgetfulTraverse.eitherFTraverse.
  checkAll(
    "ForgetfulTraverse[Either, Applicative] on Either[Int, Int]",
    new ForgetfulTraverseTests[Either, Int, Int]:
      val laws = new ForgetfulTraverseLaws[Either, Int, Int] {}
    .forgetfulTraverse,
  )

  // Use a custom extensional equality check inside the law — structural
  // `==` on SetterF compares closures by identity which is too strict.
  // We sample the builder at a fixed Snd input to witness identity /
  // composition. The shared ForgetfulFunctorLaws uses `==`, so we wire
  // the SetterF-specific laws directly (from eo.laws.data) rather than
  // the carrier-generic ones.

  // FixedTraversal[2] ForgetfulFunctor at the typeclass level —
  // exercises the typeclass-level witness for the fixed-arity carrier.
  checkAll(
    "ForgetfulFunctor[FixedTraversal[2]] on (Int, Int, Unit)",
    new ForgetfulFunctorTests[FixedTraversal[2], Unit, Int]:
      val laws = new ForgetfulFunctorLaws[FixedTraversal[2], Unit, Int] {}
    .forgetfulFunctor,
  )

  // AlgLens[F] carrier-level laws for F in {List, Option, Vector, Chain} — pins down the
  // `ForgetfulFunctor` / `ForgetfulTraverse` laws across a representative cross-section of
  // classifier shapes. Custom Arbitraries below mix empty / singleton / multi-element
  // payloads so `ForgetfulTraverse`'s sequencing law sees non-trivial cardinalities (the
  // Scalacheck defaults skew to short lists and under-sample the empty edge).
  import data.AlgLens
  import data.AlgLens.given
  import cats.data.Chain

  // Weighted generator aimed at the cardinality edges most likely to break carrier-level laws:
  //   - ~16% empty (exercises `empty`/`Nil` paths),
  //   - ~16% singleton (tests the fast-path cardinality),
  //   - ~68% multi-element — split across fixed-size-5 and random-length-2-to-8 bands so
  //     sequencing laws see non-trivial lengths most of the time.
  //
  // Frequencies 1/1/2/2 = 6 total → 16.6 / 16.6 / 33.3 / 33.3 percent.
  private def weightedListOf[A](using Arbitrary[A]): Gen[List[A]] =
    Gen.frequency(
      1 -> Gen.const(List.empty[A]),
      1 -> Arbitrary.arbitrary[A].map(List(_)),
      2 -> Gen.listOfN(5, Arbitrary.arbitrary[A]),
      2 -> Gen.choose(2, 8).flatMap(n => Gen.listOfN(n, Arbitrary.arbitrary[A])),
    )

  private given arbAlgLensListIntInt: Arbitrary[AlgLens[List][Int, Int]] =
    Arbitrary(Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map((x, _))))

  private given arbAlgLensOptionIntInt: Arbitrary[AlgLens[Option][Int, Int]] =
    Arbitrary(
      for
        x <- Arbitrary.arbitrary[Int]
        o <- Gen.oneOf(
          Gen.const(Option.empty[Int]),
          Arbitrary.arbitrary[Int].map(Option(_)),
        )
      yield (x, o)
    )

  private given arbAlgLensVectorIntInt: Arbitrary[AlgLens[Vector][Int, Int]] =
    Arbitrary(
      Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map(xs => (x, xs.toVector)))
    )

  private given arbAlgLensChainIntInt: Arbitrary[AlgLens[Chain][Int, Int]] =
    Arbitrary(
      Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map(xs => (x, Chain.fromSeq(xs))))
    )

  checkAll(
    "ForgetfulFunctor[AlgLens[List]] on (Int, List[Int])",
    new ForgetfulFunctorTests[AlgLens[List], Int, Int]:
      val laws = new ForgetfulFunctorLaws[AlgLens[List], Int, Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[AlgLens[List], Applicative] on (Int, List[Int])",
    new ForgetfulTraverseTests[AlgLens[List], Int, Int]:
      val laws = new ForgetfulTraverseLaws[AlgLens[List], Int, Int] {}
    .forgetfulTraverse,
  )

  checkAll(
    "ForgetfulFunctor[AlgLens[Option]] on (Int, Option[Int])",
    new ForgetfulFunctorTests[AlgLens[Option], Int, Int]:
      val laws = new ForgetfulFunctorLaws[AlgLens[Option], Int, Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[AlgLens[Option], Applicative] on (Int, Option[Int])",
    new ForgetfulTraverseTests[AlgLens[Option], Int, Int]:
      val laws = new ForgetfulTraverseLaws[AlgLens[Option], Int, Int] {}
    .forgetfulTraverse,
  )

  checkAll(
    "ForgetfulFunctor[AlgLens[Vector]] on (Int, Vector[Int])",
    new ForgetfulFunctorTests[AlgLens[Vector], Int, Int]:
      val laws = new ForgetfulFunctorLaws[AlgLens[Vector], Int, Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[AlgLens[Vector], Applicative] on (Int, Vector[Int])",
    new ForgetfulTraverseTests[AlgLens[Vector], Int, Int]:
      val laws = new ForgetfulTraverseLaws[AlgLens[Vector], Int, Int] {}
    .forgetfulTraverse,
  )

  checkAll(
    "ForgetfulFunctor[AlgLens[Chain]] on (Int, Chain[Int])",
    new ForgetfulFunctorTests[AlgLens[Chain], Int, Int]:
      val laws = new ForgetfulFunctorLaws[AlgLens[Chain], Int, Int] {}
    .forgetfulFunctor,
  )

  checkAll(
    "ForgetfulTraverse[AlgLens[Chain], Applicative] on (Int, Chain[Int])",
    new ForgetfulTraverseTests[AlgLens[Chain], Int, Int]:
      val laws = new ForgetfulTraverseLaws[AlgLens[Chain], Int, Int] {}
    .forgetfulTraverse,
  )

  // ----- ForgetfulApplicative via Optic.put ---------------------
  //
  // Exercise Optic.put which requires ForgetfulApplicative[F] — this
  // lights up ForgetfulApplicative.scala (0% baseline).

  "ForgetfulApplicative" should {
    // Exercise Optic.put on a Forgetful-carrier Iso (covers
    // Forgetful.applicative in core/src/main/scala/eo/data/Forgetful.scala)
    // AND exercise the forgetFApplicative given on a Forget[List]-carrier
    // Fold (covers core/src/main/scala/eo/ForgetfulApplicative.scala).
    import Optic.*
    given ForgetfulApplicative[Forgetful] = Forgetful.applicative
    given data.ReverseAccessor[Forgetful] = Forgetful.reverseAccessor
    val intDouble: Optic[Int, Int, Int, Int, Forgetful] =
      optics.Iso[Int, Int, Int, Int](_ * 2, _ / 2)
    "lift Optic.put via pure on Forgetful" >> {
      forAll((a: Int, f: Int => Int) => intDouble.put(f)(a) == intDouble.reverseGet(f(a)))
    }

    "forgetFApplicative[List].pure wraps a single element" >> {
      val ap = summon[ForgetfulApplicative[data.Forget[List]]]
      forAll((n: Int) =>
        ap.pure[Unit, Int](n) == List(n) &&
          ap.map[Unit, Int, Int](List(n), _ + 1) == List(n + 1)
      )
    }
  }

  // ----- FoldMap homomorphism on Lens / Prism / Optional ---------
  //
  // The ForgetfulFold instances for Tuple2 / Either / Affine in
  // core/src/main/scala/eo/ForgetfulFold.scala are only reachable
  // through an optic-level fold. These checkAll blocks wire the
  // EO-specific FoldMapHomomorphismLaws (used earlier on
  // Traversal.forEach) against the tuple / prism / optional fixtures
  // already declared above.
  import laws.eo.FoldMapHomomorphismLaws
  import laws.eo.discipline.FoldMapHomomorphismTests

  checkAll(
    "Lens foldMap homomorphism (Tuple2 carrier)",
    new FoldMapHomomorphismTests[(Int, String), Int, Tuple2]:
      val laws =
        new FoldMapHomomorphismLaws[(Int, String), Int, Tuple2]:
          val optic = firstLens
    .foldMapHomomorphism,
  )

  checkAll(
    "Prism foldMap homomorphism (Either carrier)",
    new FoldMapHomomorphismTests[Int, Int, Either]:
      val laws = new FoldMapHomomorphismLaws[Int, Int, Either]:
        val optic = evenPrism
    .foldMapHomomorphism,
  )

  checkAll(
    "Optional foldMap homomorphism (Affine carrier)",
    new FoldMapHomomorphismTests[
      (Int, List[Int]),
      Int,
      Affine,
    ]:
      val laws = new FoldMapHomomorphismLaws[
        (Int, List[Int]),
        Int,
        Affine,
      ]:
        val optic = headOptional
    .foldMapHomomorphism,
  )

  // ----- Grate: tuple-indexed + Function1[Boolean, *] -------------
  //
  // Two fixtures per plan R7 — homogeneous tuples of arity 2 and 3,
  // both via `Grate.tuple[T <: Tuple, A]`. All three laws are checked
  // (G1 modify-identity, G2 compose-modify, G3 replace-idempotent).

  val tuple2Grate: Optic[(Int, Int), (Int, Int), Int, Int, Grate] =
    Grate.tuple[(Int, Int), Int]

  checkAll(
    "Grate[(Int, Int), Int] — tuple arity 2",
    new GrateTests[(Int, Int), Int]:
      val laws = new GrateLaws[(Int, Int), Int]:
        val grate = tuple2Grate
    .grate,
  )

  val tuple3Grate: Optic[(Int, Int, Int), (Int, Int, Int), Int, Int, Grate] =
    Grate.tuple[(Int, Int, Int), Int]

  checkAll(
    "Grate[(Int, Int, Int), Int] — tuple arity 3",
    new GrateTests[(Int, Int, Int), Int]:
      val laws = new GrateLaws[(Int, Int, Int), Int]:
        val grate = tuple3Grate
    .grate,
  )

  // ----- Kaleidoscope: List (cartesian) + ZipList (zipping) -------
  //
  // Two Applicative-distinct fixtures per plan R8 — the whole point of
  // Kaleidoscope is that the optic's behaviour tracks the supplied
  // Reflector's Applicative semantics. Both fixtures go through the
  // generic `Kaleidoscope.apply[F, A]` factory (`S = F[A]`, rebuild =
  // identity), which is the only constructor shipped in v1.
  //
  // Three laws each: K1 modify-identity, K2 compose-modify, K3
  // collect-via-reflect (the Kaleidoscope-specific universal).

  import cats.data.ZipList

  // Arbitrary[ZipList[Int]] — constructed from an Arbitrary[List[Int]]; the same fixture pattern
  // the R1 spikes in `ReflectorInstancesSpec` used but kept local so this file is the single
  // source of truth for law fixtures.
  private given arbZipListInt: Arbitrary[ZipList[Int]] =
    Arbitrary(Arbitrary.arbitrary[List[Int]].map(ZipList(_)))

  // Cogen[ZipList[Int]] — routes through the underlying List's Cogen so scalacheck can
  // synthesise `ZipList[Int] => Int` aggregator functions for K3.
  private given cogenZipListInt: Cogen[ZipList[Int]] =
    Cogen[List[Int]].contramap(_.value)

  val listKaleidoscope: Optic[List[Int], List[Int], Int, Int, Kaleidoscope] =
    Kaleidoscope.apply[List, Int]

  checkAll(
    "Kaleidoscope[List[Int], Int] — cartesian Reflector",
    new KaleidoscopeTests[List[Int], Int, List]:
      val laws = new KaleidoscopeLaws[List[Int], Int, List]:
        val kaleidoscope = listKaleidoscope
        val reflector = summon[Reflector[List]]
    .kaleidoscope,
  )

  val zipListKaleidoscope: Optic[ZipList[Int], ZipList[Int], Int, Int, Kaleidoscope] =
    Kaleidoscope.apply[ZipList, Int]

  checkAll(
    "Kaleidoscope[ZipList[Int], Int] — zipping Reflector",
    new KaleidoscopeTests[ZipList[Int], Int, ZipList]:
      val laws = new KaleidoscopeLaws[ZipList[Int], Int, ZipList]:
        val kaleidoscope = zipListKaleidoscope
        val reflector = summon[Reflector[ZipList]]
    .kaleidoscope,
  )

  // Const[Int, *] summation fixture — the third "aggregation shape" (summation). Lands as a plain
  // bonus fixture per plan R8's stretch goal. `Const[Int, Int]`'s `A` slot is phantom, so the K3
  // law reduces to a retag witness (the aggregator is applied once, but the monoid value doesn't
  // change). Scalacheck Arbitraries for Const are hand-rolled.
  import cats.data.Const

  private given arbConstIntInt: Arbitrary[Const[Int, Int]] =
    Arbitrary(Arbitrary.arbitrary[Int].map(Const(_)))

  private given cogenConstIntInt: Cogen[Const[Int, Int]] =
    Cogen[Int].contramap(_.getConst)

  val constKaleidoscope: Optic[Const[Int, Int], Const[Int, Int], Int, Int, Kaleidoscope] =
    Kaleidoscope.apply[Const[Int, *], Int]

  checkAll(
    "Kaleidoscope[Const[Int, Int], Int] — summation Reflector",
    new KaleidoscopeTests[Const[Int, Int], Int, Const[Int, *]]:
      val laws = new KaleidoscopeLaws[Const[Int, Int], Int, Const[Int, *]]:
        val kaleidoscope = constKaleidoscope
        val reflector = summon[Reflector[Const[Int, *]]]
    .kaleidoscope,
  )
