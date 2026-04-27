package dev.constructive.eo

import cats.instances.list.given
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification

import optics.{AffineFold, Fold, Getter, Iso, Lens, Optic, Optional, Prism, Setter, Traversal}
import data.{Affine, Forget, Forgetful, MultiFocus, PSVec, SetterF}
import data.Affine.given
import data.Forget.given
import data.MultiFocus.given
import data.SetterF.given
import laws.{AffineFoldLaws, GetterLaws, IsoLaws, LensLaws, OptionalLaws, PrismLaws, SetterLaws}
import laws.discipline.{
  AffineFoldTests,
  GetterTests,
  IsoTests,
  LensTests,
  OptionalTests,
  PrismTests,
  SetterTests,
}
import laws.data.{AffineLaws, SetterFLaws}
import laws.data.discipline.{AffineTests, SetterFTests}
import laws.typeclass.AssociativeFunctorLaws
import laws.typeclass.discipline.AssociativeFunctorTests

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
  * `dev.constructive.eo.laws.OpticLaws`.
  */
class OpticsLawsSpec extends Specification with CheckAllHelpers:

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

  // covers: Traversal.forEach over List[Int]
  checkAllTraversalFor[List, Int]("Traversal.forEach[List, Int]", listTraversal)

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

  // covers: Fold over List[Int]
  checkAllFoldFor[List[Int], Int, data.Forget[List]]("Fold[List, Int]", listFold)

  val optionFold: Optic[Option[Int], Unit, Int, Int, data.Forget[Option]] =
    Fold[Option, Int]

  // covers: Fold over Option[Int]
  checkAllFoldFor[Option[Int], Int, data.Forget[Option]]("Fold[Option, Int]", optionFold)

  // ----- Fold.select: focuses on values matching a predicate ------

  val evenSelectFold: Optic[Int, Unit, Int, Int, data.Forget[Option]] =
    Fold.select[Int](_ % 2 == 0)

  // covers: Fold.select over Int (Forget[Option] carrier)
  checkAllFoldFor[Int, Int, data.Forget[Option]]("Fold.select[Int](even)", evenSelectFold)

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

  // ----- MultiFocus[PSVec] carrier carrier laws -------------------
  // Replaces the legacy PowerSeries-carrier law sweep — the optic shape is
  // identical (the X leftover and the PSVec[A] focus vector); only the
  // top-level carrier name changed.

  private given arbMultiFocusPSVec: Arbitrary[((Int, Int), PSVec[Int])] =
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
        ((x, x), PSVec.unsafeWrap[Int](arr))
    )

  // ----- MultiFocus[Function1[Int, *]] carrier laws ----------------
  // The legacy FixedTraversal[N] per-arity law sweep is now redundant: post-fold,
  // `Traversal.{two,three,four}` all share `MultiFocus[Function1[Int, *]]`, and
  // its `ForgetfulFunctor` instance is the carrier-wide `mfFunctor[F: Functor]`
  // already exercised by the `MultiFocus[List|Option|Vector|Chain]` blocks below
  // (same body, just a different `F`).
  //
  // No carrier-level discipline block is added for `MultiFocus[Function1[Int, *]]`
  // because structural `==` on functions is reference equality — exactly the
  // problem SetterF had. The functor laws are instead witnessed extensionally by
  // `MultiFocusFunction1Spec` (G1/G2 on `MultiFocus.tuple`, the same carrier) and
  // by `EoSpecificLawsSpec`'s "Traversal.two / three modifies …" forAll blocks.

  // ----- Carrier type-class laws via representative fixtures ------
  //
  // Uses the same Arbitrary givens as the carrier-specific law blocks
  // above. The full fixture matrix (Tuple2, Either, Forget[F], Forgetful)
  // is partially covered — sufficient to witness that each instance
  // satisfies the shared ForgetfulFunctor / ForgetfulTraverse laws.
  // Expanding the matrix is a follow-up (0.1.1).

  // covers: ForgetfulFunctor over Tuple2 (built-in Arbitrary[(Int, Int)])
  checkAllForgetfulFunctorFor[Tuple2, Int, Int]("ForgetfulFunctor[Tuple2] on (Int, Int)")

  // covers: ForgetfulFunctor over Either (built-in Arbitrary[Either[Int, Int]])
  checkAllForgetfulFunctorFor[Either, Int, Int]("ForgetfulFunctor[Either] on Either[Int, Int]")

  // AssociativeFunctor[Tuple2] — nested Lens composition.
  // Outer: ((Int, String), Boolean) → (Int, String); inner: (Int, String) → Int.
  // `composed` threads the outer `.andThen(inner)` through the stock
  // AssociativeFunctor[Tuple2] given at fixture-construction time so the
  // abstract-F trait body doesn't need to resolve it.
  val nestedTuple2OuterL: Optic[
    ((Int, String), Boolean),
    ((Int, String), Boolean),
    (Int, String),
    (Int, String),
    Tuple2,
  ] =
    Lens[((Int, String), Boolean), (Int, String)](_._1, (s, a) => (a, s._2))

  checkAll(
    "AssociativeFunctor[Tuple2] on nested Lens chain",
    new AssociativeFunctorTests[((Int, String), Boolean), (Int, String), Int, Tuple2]:
      val laws = new AssociativeFunctorLaws[((Int, String), Boolean), (Int, String), Int, Tuple2]:
        val outer = nestedTuple2OuterL
        val inner = firstLens
        val composed = nestedTuple2OuterL.andThen(firstLens)
        val functor = summon[ForgetfulFunctor[Tuple2]]
    .associativeFunctor,
  )

  // AssociativeFunctor[Either] — nested Prism composition.
  // `evenPrism.andThen(evenPrism)` focuses only n such that n is even AND
  // n/2 is also even (i.e. n mod 4 == 0). Domain-restricted [-10000, 10000]
  // for the same doubling-overflow reason as the outer evenPrism fixture.
  locally {
    given Arbitrary[Int] = Arbitrary(Gen.choose(-10000, 10000))
    checkAll(
      "AssociativeFunctor[Either] on nested Prism chain (even ∘ even)",
      new AssociativeFunctorTests[Int, Int, Int, Either]:
        val laws = new AssociativeFunctorLaws[Int, Int, Int, Either]:
          val outer = evenPrism
          val inner = evenPrism
          val composed = evenPrism.andThen(evenPrism)
          val functor = summon[ForgetfulFunctor[Either]]
      .associativeFunctor,
    )
  }

  // covers: ForgetfulFunctor + ForgetfulTraverse over Affine (uses arbAffineIntStringBool)
  checkAllForgetfulFunctorFor[Affine, (Int, String), Boolean](
    "ForgetfulFunctor[Affine] on Affine[(Int, String), Boolean]"
  )
  checkAllForgetfulTraverseFor[Affine, (Int, String), Boolean](
    "ForgetfulTraverse[Affine, Applicative] on Affine[(Int, String), Boolean]"
  )

  // covers: ForgetfulFunctor + ForgetfulTraverse over MultiFocus[PSVec] (uses arbMultiFocusPSVec)
  checkAllForgetfulFunctorFor[MultiFocus[PSVec], (Int, Int), Int](
    "ForgetfulFunctor[MultiFocus[PSVec]] on ((Int, Int), PSVec[Int])"
  )
  checkAllForgetfulTraverseFor[MultiFocus[PSVec], (Int, Int), Int](
    "ForgetfulTraverse[MultiFocus[PSVec], Applicative] on ((Int, Int), PSVec[Int])"
  )

  // covers: ForgetfulFunctor + ForgetfulTraverse over Forget[List] —
  // exercises Forgetful.scala's traverse2 and bifunctor paths that pure Forgetful
  // fixtures don't land.
  checkAllForgetfulFunctorFor[Forget[List], Unit, Int](
    "ForgetfulFunctor[Forget[List]] on Forget[List][Unit, Int]"
  )
  checkAllForgetfulTraverseFor[Forget[List], Unit, Int](
    "ForgetfulTraverse[Forget[List], Applicative] on Forget[List][Unit, Int]"
  )

  // covers: ForgetfulTraverse over Either — exercises ForgetfulTraverse.eitherFTraverse.
  checkAllForgetfulTraverseFor[Either, Int, Int](
    "ForgetfulTraverse[Either, Applicative] on Either[Int, Int]"
  )

  // Use a custom extensional equality check inside the law — structural
  // `==` on SetterF compares closures by identity which is too strict.
  // We sample the builder at a fixed Snd input to witness identity /
  // composition. The shared ForgetfulFunctorLaws uses `==`, so we wire
  // the SetterF-specific laws directly (from dev.constructive.eo.laws.data) rather than
  // the carrier-generic ones.

  // MultiFocus[F] carrier-level laws for F in {List, Option, Vector, Chain} — pins down the
  // `ForgetfulFunctor` / `ForgetfulTraverse` laws across a representative cross-section of
  // classifier shapes. Custom Arbitraries below mix empty / singleton / multi-element
  // payloads so `ForgetfulTraverse`'s sequencing law sees non-trivial cardinalities (the
  // Scalacheck defaults skew to short lists and under-sample the empty edge).
  import data.MultiFocus.given
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

  private given arbMultiFocusListIntInt: Arbitrary[MultiFocus[List][Int, Int]] =
    Arbitrary(Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map((x, _))))

  private given arbMultiFocusOptionIntInt: Arbitrary[MultiFocus[Option][Int, Int]] =
    Arbitrary(
      for
        x <- Arbitrary.arbitrary[Int]
        o <- Gen.oneOf(
          Gen.const(Option.empty[Int]),
          Arbitrary.arbitrary[Int].map(Option(_)),
        )
      yield (x, o)
    )

  private given arbMultiFocusVectorIntInt: Arbitrary[MultiFocus[Vector][Int, Int]] =
    Arbitrary(
      Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map(xs => (x, xs.toVector)))
    )

  private given arbMultiFocusChainIntInt: Arbitrary[MultiFocus[Chain][Int, Int]] =
    Arbitrary(
      Arbitrary.arbitrary[Int].flatMap(x => weightedListOf[Int].map(xs => (x, Chain.fromSeq(xs))))
    )

  // covers: ForgetfulFunctor + ForgetfulTraverse over MultiFocus[List]
  checkAllForgetfulFunctorFor[MultiFocus[List], Int, Int](
    "ForgetfulFunctor[MultiFocus[List]] on (Int, List[Int])"
  )
  checkAllForgetfulTraverseFor[MultiFocus[List], Int, Int](
    "ForgetfulTraverse[MultiFocus[List], Applicative] on (Int, List[Int])"
  )

  // covers: ForgetfulFunctor + ForgetfulTraverse over MultiFocus[Option]
  checkAllForgetfulFunctorFor[MultiFocus[Option], Int, Int](
    "ForgetfulFunctor[MultiFocus[Option]] on (Int, Option[Int])"
  )
  checkAllForgetfulTraverseFor[MultiFocus[Option], Int, Int](
    "ForgetfulTraverse[MultiFocus[Option], Applicative] on (Int, Option[Int])"
  )

  // covers: ForgetfulFunctor + ForgetfulTraverse over MultiFocus[Vector]
  checkAllForgetfulFunctorFor[MultiFocus[Vector], Int, Int](
    "ForgetfulFunctor[MultiFocus[Vector]] on (Int, Vector[Int])"
  )
  checkAllForgetfulTraverseFor[MultiFocus[Vector], Int, Int](
    "ForgetfulTraverse[MultiFocus[Vector], Applicative] on (Int, Vector[Int])"
  )

  // covers: ForgetfulFunctor + ForgetfulTraverse over MultiFocus[Chain]
  checkAllForgetfulFunctorFor[MultiFocus[Chain], Int, Int](
    "ForgetfulFunctor[MultiFocus[Chain]] on (Int, Chain[Int])"
  )
  checkAllForgetfulTraverseFor[MultiFocus[Chain], Int, Int](
    "ForgetfulTraverse[MultiFocus[Chain], Applicative] on (Int, Chain[Int])"
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
  // covers: foldMap homomorphism on Lens (Tuple2 carrier)
  checkAllFoldMapHomomorphismFor[(Int, String), Int, Tuple2](
    "Lens foldMap homomorphism (Tuple2 carrier)",
    firstLens,
  )

  // covers: foldMap homomorphism on Prism (Either carrier)
  checkAllFoldMapHomomorphismFor[Int, Int, Either](
    "Prism foldMap homomorphism (Either carrier)",
    evenPrism,
  )

  // covers: foldMap homomorphism on Optional (Affine carrier)
  checkAllFoldMapHomomorphismFor[(Int, List[Int]), Int, Affine](
    "Optional foldMap homomorphism (Affine carrier)",
    headOptional,
  )

  // ----- MultiFocus[Function1[Int, *]]: tuple-indexed (absorbed Grate) ----
  //
  // Two fixtures — homogeneous tuples of arity 2 and 3, both via
  // `MultiFocus.tuple[T <: Tuple, A]` (the absorbed Grate.tuple). The three
  // load-bearing laws (modify-identity, compose-modify, replace-idempotent
  // — formerly G1/G2/G3 from GrateLaws) are checked inline as `forAll`
  // props because `MultiFocusTests` requires `S =:= F[A]`, which doesn't
  // hold for the tuple-shaped factory (S = TupleN, F = Function1[Int, *],
  // so F[A] ≠ S). Same coverage as the deleted GrateTests.

  val tuple2MultiFocus: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
    MultiFocus.tuple[(Int, Int), Int]

  "MultiFocus.tuple[(Int, Int), Int] — modify-identity (G1)" >> forAll { (s: (Int, Int)) =>
    tuple2MultiFocus.modify(identity[Int])(s) == s
  }

  "MultiFocus.tuple[(Int, Int), Int] — compose-modify (G2)" >> forAll {
    (s: (Int, Int), f: Int => Int, g: Int => Int) =>
      tuple2MultiFocus.modify(g)(tuple2MultiFocus.modify(f)(s)) ==
        tuple2MultiFocus.modify(f.andThen(g))(s)
  }

  "MultiFocus.tuple[(Int, Int), Int] — replace-idempotent (G3)" >> forAll {
    (s: (Int, Int), a: Int) =>
      tuple2MultiFocus.replace(a)(tuple2MultiFocus.replace(a)(s)) ==
        tuple2MultiFocus.replace(a)(s)
  }

  val tuple3MultiFocus
      : Optic[(Int, Int, Int), (Int, Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
    MultiFocus.tuple[(Int, Int, Int), Int]

  "MultiFocus.tuple[(Int, Int, Int), Int] — modify-identity (G1)" >> forAll {
    (s: (Int, Int, Int)) => tuple3MultiFocus.modify(identity[Int])(s) == s
  }

  "MultiFocus.tuple[(Int, Int, Int), Int] — compose-modify (G2)" >> forAll {
    (s: (Int, Int, Int), f: Int => Int, g: Int => Int) =>
      tuple3MultiFocus.modify(g)(tuple3MultiFocus.modify(f)(s)) ==
        tuple3MultiFocus.modify(f.andThen(g))(s)
  }

  "MultiFocus.tuple[(Int, Int, Int), Int] — replace-idempotent (G3)" >> forAll {
    (s: (Int, Int, Int), a: Int) =>
      tuple3MultiFocus.replace(a)(tuple3MultiFocus.replace(a)(s)) ==
        tuple3MultiFocus.replace(a)(s)
  }

  // ----- MultiFocus: List + ZipList + Const fixtures --------------
  //
  // Three Functor-distinct fixtures — the whole point of `MultiFocus[F]` is that the optic's
  // behaviour tracks the supplied `Functor[F]`. All three fixtures route through the generic
  // `MultiFocus.apply[F, A]` factory (`X = F[A]`, rebuild = identity), the only constructor at this
  // shape.
  //
  // Three laws each: MF1 modify-identity, MF2 compose-modify, MF3 collect-via-map (Functor-
  // broadcast). The K3 cartesian-singleton statement does NOT survive as a discipline law — per
  // the user's option (a) we ship only `collectViaMap`. The List-singleton story stays at the call
  // site as the `collectList` extension.

  import cats.data.ZipList

  // Arbitrary[ZipList[Int]] — constructed from an Arbitrary[List[Int]].
  private given arbZipListInt: Arbitrary[ZipList[Int]] =
    Arbitrary(Arbitrary.arbitrary[List[Int]].map(ZipList(_)))

  // Cogen[ZipList[Int]] — routes through the underlying List's Cogen so scalacheck can synthesise
  // `ZipList[Int] => Int` aggregator functions for MF3.
  private given cogenZipListInt: Cogen[ZipList[Int]] =
    Cogen[List[Int]].contramap(_.value)

  val listMultiFocus: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
    MultiFocus.apply[List, Int]

  // covers: MultiFocus over List
  checkAllMultiFocusFor[List[Int], Int, List](
    "MultiFocus[List][Int] — List carrier",
    listMultiFocus,
  )

  val zipListMultiFocus: Optic[ZipList[Int], ZipList[Int], Int, Int, MultiFocus[ZipList]] =
    MultiFocus.apply[ZipList, Int]

  // covers: MultiFocus over ZipList (length-preserving Functor.map)
  checkAllMultiFocusFor[ZipList[Int], Int, ZipList](
    "MultiFocus[ZipList][Int] — ZipList carrier",
    zipListMultiFocus,
  )

  // Const[Int, *] summation fixture — the third "aggregation shape" (summation). Lands as a plain
  // bonus fixture. `Const[Int, Int]`'s `A` slot is phantom, so MF3 reduces to a retag witness.
  // Scalacheck Arbitraries for Const are hand-rolled.
  import cats.data.Const

  private given arbConstIntInt: Arbitrary[Const[Int, Int]] =
    Arbitrary(Arbitrary.arbitrary[Int].map(Const(_)))

  private given cogenConstIntInt: Cogen[Const[Int, Int]] =
    Cogen[Int].contramap(_.getConst)

  val constMultiFocus
      : Optic[Const[Int, Int], Const[Int, Int], Int, Int, MultiFocus[Const[Int, *]]] =
    MultiFocus.apply[Const[Int, *], Int]

  // covers: MultiFocus over Const (phantom retag)
  checkAllMultiFocusFor[Const[Int, Int], Int, Const[Int, *]](
    "MultiFocus[Const[Int, *]][Int] — Const carrier",
    constMultiFocus,
  )
