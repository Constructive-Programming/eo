package dev.constructive.eo

import cats.instances.list.given
import org.scalacheck.{Arbitrary, Gen}

import laws.{GetterLaws, IsoLaws, LensLaws, ModifyLaws, OptionalLaws, PrismLaws, TraversalLaws}
import laws.discipline.{
  GetterTests,
  IsoTests,
  LensTests,
  ModifyTests,
  OptionalTests,
  PrismTests,
  TraversalTests,
}
import OpticFixtures.*

/** The base optic families against the discipline laws, on the hand-built optics of
  * [[OpticFixtures]]. munit, so it runs on every platform (the rest of the law matrix — carriers,
  * composition, fused paths — is specs2 and runs on JVM and Scala.js).
  */
class OpticLawsSuite extends munit.DisciplineSuite:

  // ----- Iso: tuple swap (Int, String) <-> (String, Int) ----------

  checkAll(
    "Iso[(Int,String), (String,Int)] — tuple swap",
    new IsoTests[(Int, String), (String, Int)]:
      val laws = new IsoLaws[(Int, String), (String, Int)]:
        val iso = swapIso
    .iso,
  )

  // ----- Lens: first / second on a pair --------------------------

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

  // ----- Prism: even Int, isomorphic to its half (domain restricted to ±10000) ----

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

  checkAll(
    "Optional[(Int,List[Int]), Int] — head of non-empty pair",
    new OptionalTests[(Int, List[Int]), Int]:
      val laws = new OptionalLaws[(Int, List[Int]), Int]:
        val optional = headOptional
    .optional,
  )

  // ----- Modify: maps `f` over both sides of a pair --------------

  checkAll(
    "Modify[(Int,Int), Int] — both pair components",
    new ModifyTests[(Int, Int), Int]:
      val laws = new ModifyLaws[(Int, Int), Int]:
        val modify = pairModify
    .modify,
  )

  // ----- Traversal.each on List[Int] ------------------------------

  checkAll(
    "Traversal.each[List, Int]",
    new TraversalTests[List, Int]:
      val laws = new TraversalLaws[List, Int]:
        val traversal = listTraversal
    .traversal,
  )

  // ----- Getter: first projection ---------------------------------

  checkAll(
    "Getter[(Int,String), Int] — first projection",
    new GetterTests[(Int, String), Int]:
      val laws = new GetterLaws[(Int, String), Int]:
        val getter = firstGetter
        val reference = (p: (Int, String)) => p._1
    .getter,
  )
