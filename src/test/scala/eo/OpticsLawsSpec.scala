package eo

import optics.{Iso, Lens, Prism, Optional, Setter, Traversal, Optic, Fold}
import data.{Affine, Forgetful}
import laws.OpticLaws.*

import cats.instances.list.given
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

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

  // ----- Fold.select: focuses on values matching a predicate ------

  "Fold.select" should {
    val evenFold: Optic[Int, Unit, Int, Int, data.Forget[Option]] =
      Fold.select[Int](_ % 2 == 0)

    "expose the value via .to when the predicate holds, None otherwise" >> {
      forAll((n: Int) =>
        evenFold.to(n) == (if n % 2 == 0 then Some(n) else None)
      )
    }
  }
