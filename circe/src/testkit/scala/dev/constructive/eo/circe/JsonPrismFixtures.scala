package dev.constructive.eo.circe

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec
import org.scalacheck.{Arbitrary, Gen}

import JsonSpecFixtures.{Address, Person}

/** Generators and NamedTuple codecs shared by the [[JsonPrismSuite]] properties (framework-free, so
  * every platform's test sources can use them).
  */
object JsonPrismFixtures:

  // ---- Generators (light) -------------------------------------------

  given Arbitrary[Address] = Arbitrary(
    for
      s <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      z <- Gen.choose(0, 99999)
    yield Address(s, z)
  )

  given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
      addr <- Arbitrary.arbitrary[Address]
    yield Person(n, a, addr)
  )

  // Common ADTs (`Address`, `Person`, `Order`, `Basket`) live in
  // `JsonSpecFixtures` — this object holds only the spec-specific
  // NamedTuple aliases + their codec instances.

  // ---- NamedTuple codec givens for multi-field (.fields) specs ----

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derived

  type NameAgeAddress =
    NamedTuple.NamedTuple[("name", "age", "address"), (String, Int, Address)]

  given Codec.AsObject[NameAgeAddress] = KindlingsCodecAsObject.derived

  type AgeName = NamedTuple.NamedTuple[("age", "name"), (Int, String)]
  given Codec.AsObject[AgeName] = KindlingsCodecAsObject.derived
