package dev.constructive.eo

import cats.instances.list.given
import org.scalacheck.{Arbitrary, Gen}

import data.{Affine, Direct}
import laws.{
  GetterLaws,
  IsoLaws,
  LensLaws,
  ModifyLaws,
  OptionalLaws,
  PrismLaws,
  ReadTargetingLaws,
  TargetingLaws,
  TraversalLaws,
}
import laws.discipline.{
  GetterTests,
  IsoTests,
  LensTests,
  ModifyTests,
  OptionalTests,
  PrismTests,
  ReadTargetingTests,
  TargetingTests,
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

  // ----- Targeting: agreement with the carrier's own accessor --------
  //
  // Every suite above is INTRINSIC: it relates the optic to itself, so a resolution mechanism
  // aimed at the wrong slot passes it (a mis-targeted optic is a perfectly lawful optic onto the
  // slot it did hit). These register the EXTRINSIC reference — the carrier's own projection and
  // pair rebuild, written without the optic — so a disagreement about WHICH slot the focus is
  // fails loudly.

  checkAll(
    "Targeting — Lens[(Int,String), Int] first projection",
    new TargetingTests[(Int, String), Int, Tuple2]:
      val laws = new TargetingLaws[(Int, String), Int, Tuple2]:
        val optic = firstLens
        def read(s: (Int, String)) = Some(optic.get(s))
        val refGet = (s: (Int, String)) => Some(s._1)
        val refReplace = (s: (Int, String), a: Int) => (a, s._2)
        val eqv = (a: (Int, String), b: (Int, String)) => a == b
    .targeting,
  )

  checkAll(
    "Targeting — Optional[(Int,List[Int]), Int] head of non-empty pair",
    new TargetingTests[(Int, List[Int]), Int, Affine]:
      val laws = new TargetingLaws[(Int, List[Int]), Int, Affine]:
        val optic = headOptional
        def read(s: (Int, List[Int])) = optic.getOption(s)
        val refGet = (s: (Int, List[Int])) => Some(s._1)
        val refReplace = (s: (Int, List[Int]), a: Int) => (a, s._2)
        val eqv = (a: (Int, List[Int]), b: (Int, List[Int])) => a == b
    .targeting,
  )

  checkAll(
    "ReadTargeting — Getter[(Int,String), Int] first projection",
    new ReadTargetingTests[(Int, String), Int]:
      val laws = new ReadTargetingLaws[(Int, String), Int, Direct]:
        val optic = firstGetter
        def read(s: (Int, String)) = Some(optic.get(s))
        val refGet = (s: (Int, String)) => Some(s._1)
    .readTargeting,
  )
