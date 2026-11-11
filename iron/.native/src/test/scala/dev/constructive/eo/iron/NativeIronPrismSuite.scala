package dev.constructive.eo
package iron

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive
import org.scalacheck.{Arbitrary, Cogen, Gen}

import laws.PrismLaws
import laws.discipline.PrismTests

/** Scala Native twin of the `IronPrismSpec` prism laws (specs2 has no Native artifact). */
class NativeIronPrismSuite extends munit.DisciplineSuite:

  given Arbitrary[Int :| Positive] = Arbitrary(Gen.posNum[Int].map(_.assume[Positive]))
  given Cogen[Int :| Positive] = Cogen[Int].contramap(identity)

  checkAll(
    "IronPrism[Int, Positive]",
    new PrismTests[Int, Int :| Positive]:
      val laws = new PrismLaws[Int, Int :| Positive]:
        val prism = IronPrism[Int, Positive]
    .prism,
  )
