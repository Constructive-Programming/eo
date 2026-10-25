// Deliberately OUTSIDE `dev.constructive.eo.iron`: the spec compiles only if the `.prism`
// extensions reach callers through the documented import.
package dev.constructive.eo.irontest

import dev.constructive.eo.iron.{prism, IronPrism}
import dev.constructive.eo.laws.PrismLaws
import dev.constructive.eo.laws.discipline.PrismTests
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive
import io.github.iltotore.iron.constraint.string.Alphanumeric
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

class ConstraintSyntaxSpec extends Specification with Discipline with ScalaCheck:

  given arbPositive: Arbitrary[Int :| Positive] = Arbitrary(Gen.posNum[Int].map(_.assume[Positive]))
  given cogenPositive: Cogen[Int :| Positive] = Cogen[Int].contramap(identity)

  given arbAlphanumeric: Arbitrary[String :| Alphanumeric] =
    Arbitrary(Gen.alphaNumStr.map(_.assume[Alphanumeric]))

  given cogenAlphanumeric: Cogen[String :| Alphanumeric] = Cogen[String].contramap(identity)

  val positiveInt: RuntimeConstraint[Int, Positive] = summon

  checkAll(
    "RuntimeConstraint[Int, Positive].prism",
    new PrismTests[Int, Int :| Positive]:
      val laws = new PrismLaws[Int, Int :| Positive]:
        val prism = positiveInt.prism
    .prism,
  )

  checkAll(
    "Constraint[String, Alphanumeric].prism",
    new PrismTests[String, String :| Alphanumeric]:
      val laws = new PrismLaws[String, String :| Alphanumeric]:
        val prism = summon[Constraint[String, Alphanumeric]].prism
    .prism,
  )

  "constraint .prism" should {
    "agree with IronPrism, from a RuntimeConstraint" >> forAll { (n: Int) =>
      positiveInt.prism.getOption(n) === IronPrism[Int, Positive].getOption(n)
    }

    "agree with IronPrism, from a Constraint" >> forAll { (n: Int) =>
      summon[Constraint[Int, Positive]].prism.getOption(n) ===
        IronPrism[Int, Positive].getOption(n)
    }
  }
