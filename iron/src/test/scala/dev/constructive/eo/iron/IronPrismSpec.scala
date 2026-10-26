package dev.constructive.eo
package iron

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import io.github.iltotore.iron.constraint.numeric.Positive
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import laws.discipline.{OptionalTests, PrismTests}
import laws.{OptionalLaws, PrismLaws}
import optics.{Lens, Traversal}

object IronPrismSpec:

  type Age = Age.T
  object Age extends RefinedType[Int, Positive]

  type Name = Name.T
  object Name extends RefinedSubtype[String, MinLength[1]]

  final case class Person(name: String, age: Int)

  val positive = IronPrism[Int, Positive]

  val ageL = Lens[Person, Int](_.age, (p, a) => p.copy(age = a))

  // Half the generated Ints miss, so every law sees both branches.
  given Arbitrary[Int :| Positive] = Arbitrary(Gen.posNum[Int].map(_.assume[Positive]))
  given Cogen[Int :| Positive] = Cogen[Int].contramap(identity)
  given Arbitrary[Age] = Arbitrary(Gen.posNum[Int].map(Age.assume))
  given Cogen[Age] = Cogen[Int].contramap(_.value)
  given Arbitrary[Name] = Arbitrary(Gen.alphaStr.suchThat(_.nonEmpty).map(Name.assume))
  given Cogen[Name] = Cogen[String].contramap(_.value)

  given positiveInts: Arbitrary[List[Int :| Positive]] =
    Arbitrary(Gen.listOf(Gen.posNum[Int]).map(_.assumeAll[Positive]))

  given Cogen[List[Int :| Positive]] = Cogen[List[Int]].contramap(identity)

  // Lists that are often all positive (so `all` hits) and often not.
  given mixedInts: Arbitrary[List[Int]] = Arbitrary(
    Gen.oneOf(Gen.listOf(Gen.posNum[Int]), Gen.listOf(Arbitrary.arbitrary[Int]))
  )

  given Arbitrary[Person] = Arbitrary(
    for
      n <- Arbitrary.arbitrary[String]
      a <- Arbitrary.arbitrary[Int]
    yield Person(n, a)
  )

class IronPrismSpec extends Specification with Discipline with ScalaCheck:

  import IronPrismSpec.{*, given}

  checkAll(
    "IronPrism[Int, Positive]",
    new PrismTests[Int, Int :| Positive]:
      val laws = new PrismLaws[Int, Int :| Positive]:
        val prism = positive
    .prism,
  )

  checkAll(
    "IronPrism.refined[Age] (RefinedType)",
    new PrismTests[Int, Age]:
      val laws = new PrismLaws[Int, Age]:
        val prism = IronPrism.refined[Age]
    .prism,
  )

  checkAll(
    "IronPrism.refined[Name] (RefinedSubtype)",
    new PrismTests[String, Name]:
      val laws = new PrismLaws[String, Name]:
        val prism = IronPrism.refined[Name]
    .prism,
  )

  checkAll(
    "IronPrism.all[List, Int, Positive]",
    new PrismTests[List[Int], List[Int :| Positive]]:
      val laws = new PrismLaws[List[Int], List[Int :| Positive]]:
        val prism = IronPrism.all[List, Int, Positive]
    .prism,
  )

  checkAll(
    "Lens[Person, Int] andThen IronPrism[Int, Positive]",
    new OptionalTests[Person, Int :| Positive]:
      val laws = new OptionalLaws[Person, Int :| Positive]:
        val optional = ageL.andThen(positive)
    .optional,
  )

  "IronPrism" should {
    "hit exactly when the constraint holds" >> forAll { (n: Int) =>
      positive.getOption(n).map(p => p: Int) === Option.when(n > 0)(n)
    }

    "agree with Iron's own refineOption" >> forAll { (n: Int) =>
      positive.getOption(n) === n.refineOption[Positive]
    }

    "pass a failing value through writes untouched" >> {
      positive.modify(_ => 7.assume[Positive])(-3) === -3
    }

    "write through a Lens only when the field is valid" >> {
      val ageO = ageL.andThen(positive)
      (ageO.modify(a => (a + 1).assume[Positive])(Person("ada", 36)) === Person("ada", 37))
        .and(ageO.modify(a => (a + 1).assume[Positive])(Person("bob", -1)) === Person("bob", -1))
    }

    "filter a traversal down to the valid elements" >> forAll { (xs: List[Int]) =>
      val valid = Traversal.each[List, Int].andThen(positive)
      (valid.foldMap(List(_))(xs) === xs.filter(_ > 0))
        .and(
          valid.modify(p => ((p: Int).min(Int.MaxValue - 1) + 1).assume[Positive])(xs) ===
            xs.map(n => if n > 0 then n.min(Int.MaxValue - 1) + 1 else n)
        )
    }
  }

  "IronPrism.refined" should {
    "read a valid base value as the newtype" >> {
      IronPrism.refined[Age].getOption(30) === Some(Age(30))
    }

    "miss on an invalid base value" >> {
      (IronPrism.refined[Age].getOption(0) === None)
        .and(IronPrism.refined[Name].getOption("") === None)
    }
  }

  "IronPrism.all" should {
    "hit only when every element satisfies the constraint" >> forAll { (xs: List[Int]) =>
      IronPrism.all[List, Int, Positive].getOption(xs).isDefined === xs.forall(_ > 0)
    }

    "refine the container in place, without copying it" >> {
      val xs = List(1, 2, 3)
      IronPrism.all[List, Int, Positive].getOption(xs).exists(_ eq xs) === true
    }
  }
