package dev.constructive.eo.avro

import scala.language.implicitConversions

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[ConfluentWire]] — the registry-agnostic Confluent framing helper (magic
  * `0x00` + big-endian 4-byte schema id + Avro binary body).
  */
class ConfluentWireSpec extends Specification with ScalaCheck:

  private given Arbitrary[Array[Byte]] = Arbitrary(
    Gen.containerOf[Array, Byte](Arbitrary.arbitrary[Byte])
  )

  // ---- Round-trip ----------------------------------------------------

  // covers: strip∘attach == identity on (schemaId, body) across the full Int range (incl. 0,
  //   negatives — the id is carried verbatim, not validated) and arbitrary bodies (incl. empty);
  //   attach∘strip reconstructs the original framed payload byte-for-byte
  "strip ∘ attach == identity + attach ∘ strip reconstructs the framed payload" >> forAll {
    (schemaId: Int, body: Array[Byte]) =>
      val framed = ConfluentWire.attach(schemaId, body)
      val stripped = ConfluentWire.strip(framed)

      val roundTrip = stripped match
        case Right(f) => f.schemaId == schemaId && java.util.Arrays.equals(f.body, body)
        case Left(_)  => false
      val reconstruct = stripped match
        case Right(f) => java.util.Arrays.equals(ConfluentWire.attach(f.schemaId, f.body), framed)
        case Left(_)  => false
      val length = framed.length == ConfluentWire.HeaderLength + body.length
      val magic = framed(0) == ConfluentWire.Magic

      roundTrip && reconstruct && length && magic
  }

  // ---- Failure paths --------------------------------------------------

  // covers: input shorter than the 5-byte header (empty, 1-byte, 4-byte) → NotConfluentFramed
  //   naming the length check; wrong magic byte → NotConfluentFramed naming the magic check;
  //   a 5-byte header with an empty body is valid
  "strip: short input and bad magic surface NotConfluentFramed" >> {
    val emptyOk = ConfluentWire.strip(Array.emptyByteArray) match
      case Left(AvroFailure.NotConfluentFramed(reason)) => reason must contain("0 bytes")
      case other                                        =>
        org
          .specs2
          .execute
          .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result

    val shortOk = ConfluentWire.strip(Array[Byte](0, 0, 0, 1)) match
      case Left(AvroFailure.NotConfluentFramed(reason)) => reason must contain("4 bytes")
      case other                                        =>
        org
          .specs2
          .execute
          .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result

    val badMagic = ConfluentWire.attach(7, Array[Byte](1, 2, 3))
    badMagic(0) = 0x2a
    val magicOk = ConfluentWire.strip(badMagic) match
      case Left(AvroFailure.NotConfluentFramed(reason)) => reason must contain("0x2a")
      case other                                        =>
        org
          .specs2
          .execute
          .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result

    val headerOnlyOk = ConfluentWire.strip(ConfluentWire.attach(42, Array.emptyByteArray)) match
      case Right(f) => (f.schemaId === 42).and(f.body.length === 0)
      case other    =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    emptyOk.and(shortOk).and(magicOk).and(headerOnlyOk)
  }

end ConfluentWireSpec
