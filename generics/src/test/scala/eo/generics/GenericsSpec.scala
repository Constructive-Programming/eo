package eo
package generics

import eo.optics.Optic.*
import eo.data.Forgetful.given

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behavioural specs for macro-derived Lens and Prism. The derived
  * optics must be observationally identical to hand-written ones, so
  * we exercise the three Lens laws (get-set / set-get / set-set) and
  * the three Prism laws (partial-round-trip / round-trip /
  * reverseGet-then-getOption) directly rather than leaning on the
  * cross-module law harness. Keeping the checks in-module means
  * `eo-generics` stays independent of `cats-eo-laws` at runtime.
  */
class GenericsSpec extends Specification with ScalaCheck:

  // ---------- Product-type Lens derivation ----------

  final case class Person(name: String, age: Int)

  object Person:
    given Arbitrary[Person] =
      Arbitrary(for
        n <- Gen.alphaNumStr
        a <- Gen.choose(-1000, 1000)
      yield Person(n, a))

  val ageL  = lens[Person](_.age)
  val nameL = lens[Person](_.name)

  "derived Lens has the correct getter" >> forAll { (p: Person) =>
    ageL.get(p) == p.age && nameL.get(p) == p.name
  }

  "derived Lens obeys the set-get law" >> forAll { (p: Person, a: Int, s: String) =>
    ageL.get(ageL.replace(a)(p)) == a &&
      nameL.get(nameL.replace(s)(p)) == s
  }

  "derived Lens obeys the get-set law" >> forAll { (p: Person) =>
    ageL.replace(ageL.get(p))(p) == p &&
      nameL.replace(nameL.get(p))(p) == p
  }

  "derived Lens obeys the set-set law" >> forAll { (p: Person, a1: Int, a2: Int) =>
    ageL.replace(a2)(ageL.replace(a1)(p)) == ageL.replace(a2)(p)
  }

  "derived Lens.modify matches replace ∘ get" >> forAll { (p: Person) =>
    ageL.modify(_ + 1)(p) == ageL.replace(ageL.get(p) + 1)(p)
  }

  // ---------- Sum-type Prism derivation ----------

  enum Shape:
    case Circle(radius: Double)
    case Square(side: Double)
    case Triangle(base: Double, height: Double)

  object Shape:
    given Arbitrary[Shape] =
      Arbitrary(Gen.oneOf(
        Gen.posNum[Double].map(Circle(_)),
        Gen.posNum[Double].map(Square(_)),
        Gen.zip(Gen.posNum[Double], Gen.posNum[Double]).map((b, h) => Triangle(b, h))
      ))

  val circleP: eo.optics.Optic[Shape, Shape, Shape.Circle, Shape.Circle, Either] =
    prism[Shape, Shape.Circle]

  "derived Prism round-trips on matching variants" >> forAll { (r: Double) =>
    val c = Shape.Circle(r)
    circleP.to(c) == Right(c)
  }

  "derived Prism returns Left on non-matching variants" >> {
    val sq: Shape = Shape.Square(2.0)
    circleP.to(sq) == Left(sq)
    val tr: Shape = Shape.Triangle(1.0, 2.0)
    circleP.to(tr) == Left(tr)
  }

  "derived Prism.reverseGet widens variant to parent" >> forAll { (r: Double) =>
    val c: Shape.Circle = Shape.Circle(r)
    circleP.reverseGet(c) == (c: Shape)
  }

  "derived Prism obeys reverseGet-then-getOption == Some" >> forAll { (r: Double) =>
    val c: Shape.Circle = Shape.Circle(r)
    circleP.to(circleP.reverseGet(c)) == Right(c)
  }

  "derived Prism obeys partial-round-trip on any Shape" >> forAll { (s: Shape) =>
    circleP.to(s) match
      case Right(c) => circleP.reverseGet(c) == s
      case Left(s2) => s2 == s
  }
