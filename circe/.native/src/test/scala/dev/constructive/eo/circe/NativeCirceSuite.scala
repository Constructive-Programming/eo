// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo.circe

import cats.data.Ior
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.parser.parse
import io.circe.syntax.*
import io.circe.{Codec, Json}
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}

final case class NativeAddress(street: String, zip: Int)

object NativeAddress:
  given Codec.AsObject[NativeAddress] = KindlingsCodecAsObject.derived

final case class NativePerson(name: String, age: Int, address: NativeAddress)

object NativePerson:
  given Codec.AsObject[NativePerson] = KindlingsCodecAsObject.derived

/** Scala Native twin of the core of `JsonPrismSpec` (specs2 has no Native artifact): the cursor
  * prism over circe's `Json` AST — modify, decode failure as `Ior.Both`, field drill-down, and the
  * String-input parse path.
  */
class NativeCirceSuite extends ScalaCheckSuite:

  private given Arbitrary[NativePerson] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
      s <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      z <- Gen.choose(0, 99999)
    yield NativePerson(n, a, NativeAddress(s, z))
  )

  property("codecPrism: modify round-trips (parity with Unsafe); a decode failure is Ior.Both") {
    forAll { (p: NativePerson) =>
      val json = p.asJson
      val f = (q: NativePerson) => q.copy(name = q.name.toUpperCase)
      val expected = f(p).asJson
      val happy = codecPrism[NativePerson].modifyUnsafe(f)(json) == expected &&
        codecPrism[NativePerson].modify(f)(json) == Ior.Right(expected)
      val notAPerson = Json.fromString("not a person")
      val bad = codecPrism[NativePerson].modify(identity[NativePerson])(notAPerson) match
        case Ior.Both(chain, j) =>
          j == notAPerson && chain.length == 1L &&
          chain.headOption.exists(_.isInstanceOf[JsonFailure.DecodeFailed])
        case _ => false
      happy && bad
    }
  }

  property("field drill-down: modify one nested field, leave the rest of the Json alone") {
    forAll { (p: NativePerson) =>
      val zipL = codecPrism[NativePerson].field(_.address).field(_.zip)
      zipL.modifyUnsafe(_ + 1)(p.asJson) ==
        p.copy(address = p.address.copy(zip = p.address.zip + 1)).asJson &&
        zipL.getOptionUnsafe(p.asJson) == Some(p.address.zip)
    }
  }

  test("String input is parsed; unparseable input is a ParseFailed, not an exception") {
    val ageL = codecPrism[NativePerson].field(_.age)
    val raw = NativePerson("a", 1, NativeAddress("s", 2)).asJson.noSpaces
    assertEquals(
      parse(ageL.modifyUnsafe(_ + 1)(raw).noSpaces).flatMap(_.as[NativePerson]).map(_.age),
      Right(2)
    )
    assertEquals(ageL.getOptionUnsafe("not json"), None)
  }

// CPD-ON
