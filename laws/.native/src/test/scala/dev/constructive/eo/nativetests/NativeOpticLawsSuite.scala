package dev.constructive.eo.nativetests

import cats.instances.list.given
import dev.constructive.eo.data.{Affine, Direct, ModifyF, MultiFocus, PSVec}
import dev.constructive.eo.laws.discipline.{
  GetterTests,
  IsoTests,
  LensTests,
  ModifyTests,
  OptionalTests,
  PrismTests,
  TraversalTests,
}
import dev.constructive.eo.laws.{
  GetterLaws,
  IsoLaws,
  LensLaws,
  ModifyLaws,
  OptionalLaws,
  PrismLaws,
  TraversalLaws,
}
import dev.constructive.eo.optics.{Getter, Iso, Lens, Modify, Optic, Optional, Prism, Traversal}
import org.scalacheck.{Arbitrary, Gen}

/** Scala Native twin of the core of `OpticsLawsSpec` (the `tests` module). specs2 publishes no
  * Scala Native artifact, so the specs2 suites cannot compile there; this munit suite re-runs the
  * discipline rule sets — which are framework-independent — on the Native build of core + laws.
  */
class NativeOpticLawsSuite extends munit.DisciplineSuite:

  private val swapIso: Optic[(Int, String), (Int, String), (String, Int), (String, Int), Direct] =
    Iso[(Int, String), (Int, String), (String, Int), (String, Int)](_.swap, _.swap)

  checkAll(
    "Iso[(Int,String), (String,Int)] — tuple swap",
    new IsoTests[(Int, String), (String, Int)]:
      val laws = new IsoLaws[(Int, String), (String, Int)]:
        val iso = swapIso
    .iso,
  )

  checkAll(
    "Lens[(Int,String), Int] — first projection",
    new LensTests[(Int, String), Int]:
      val laws = new LensLaws[(Int, String), Int]:
        val lens = Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    .lens,
  )

  private val evenPrism: Optic[Int, Int, Int, Int, Either] =
    Prism[Int, Int](n => if n % 2 == 0 then Right(n / 2) else Left(n), k => k * 2)

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

  private val headOptional: Optic[(Int, List[Int]), (Int, List[Int]), Int, Int, Affine] =
    Optional[(Int, List[Int]), (Int, List[Int]), Int, Int](
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

  private val pairModify: Optic[(Int, Int), (Int, Int), Int, Int, ModifyF] =
    Modify[(Int, Int), (Int, Int), Int, Int](f => { case (a, b) => (f(a), f(b)) })

  checkAll(
    "Modify[(Int,Int), Int] — both pair components",
    new ModifyTests[(Int, Int), Int]:
      val laws = new ModifyLaws[(Int, Int), Int]:
        val modify = pairModify
    .modify,
  )

  private val listTraversal: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
    Traversal.each[List, Int]

  checkAll(
    "Traversal.each[List, Int]",
    new TraversalTests[List, Int]:
      val laws = new TraversalLaws[List, Int]:
        val traversal = listTraversal
    .traversal,
  )

  checkAll(
    "Getter[(Int,String), Int] — first projection",
    new GetterTests[(Int, String), Int]:
      val laws = new GetterLaws[(Int, String), Int]:
        val getter = Getter[(Int, String), Int](_._1)
        val reference = (p: (Int, String)) => p._1
    .getter,
  )
