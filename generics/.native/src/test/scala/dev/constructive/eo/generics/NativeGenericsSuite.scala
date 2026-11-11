// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo
package generics

import dev.constructive.eo.optics.Optic.*
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}

// Top-level fixtures (a macro's `new T(...)` loses its outer accessor on nested classes).
final case class NativePerson(name: String, age: Int)

enum NativeShape:
  case Circle(radius: Double)
  case Square(side: Double)

object NativeFixtures:

  given Arbitrary[NativePerson] =
    Arbitrary(for
      n <- Gen.alphaNumStr
      a <- Gen.choose(-1000, 1000)
    yield NativePerson(n, a))

  given Arbitrary[NativeShape] =
    Arbitrary(
      Gen.oneOf(
        Gen.posNum[Double].map(NativeShape.Circle(_)),
        Gen.posNum[Double].map(NativeShape.Square(_)),
      )
    )

/** Scala Native twin of the core of `GenericsSpec` (specs2 has no Native artifact): the Hearth
  * macros expand on the JVM compiler either way, so this checks the DERIVED optics behave on
  * Native.
  */
class NativeGenericsSuite extends ScalaCheckSuite:
  import NativeFixtures.given

  private val ageL = lens[NativePerson](_.age)
  private val nameL = lens[NativePerson](_.name)

  private val circleP: dev.constructive.eo.optics.Optic[
    NativeShape,
    NativeShape,
    NativeShape.Circle,
    NativeShape.Circle,
    Either,
  ] = prism[NativeShape, NativeShape.Circle]

  property("derived Lens: get / set-get / get-set / set-set / modify") {
    forAll { (p: NativePerson, a: Int, a1: Int, a2: Int, s: String) =>
      ageL.get(p) == p.age && nameL.get(p) == p.name &&
      ageL.get(ageL.replace(a)(p)) == a &&
      ageL.replace(ageL.get(p))(p) == p &&
      ageL.replace(a2)(ageL.replace(a1)(p)) == ageL.replace(a2)(p) &&
      ageL.modify(_ + 1)(p) == ageL.replace(ageL.get(p) + 1)(p) &&
      nameL.place(s)(p) == NativePerson(s, p.age)
    }
  }

  property("derived Prism: round-trip, miss, partial round-trip") {
    forAll { (r: Double, s: NativeShape) =>
      val c: NativeShape.Circle = NativeShape.Circle(r)
      val sq: NativeShape = NativeShape.Square(2.0)
      circleP.to(c) == Right(c) &&
      circleP.to(sq) == Left(sq) &&
      circleP.reverseGet(c) == (c: NativeShape) &&
      (circleP.to(s) match
        case Right(cc) => circleP.reverseGet(cc) == s
        case Left(s2)  => s2 == s)
    }
  }
// CPD-ON
