package dev.constructive.eo.avro

import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.specs2.mutable.Specification

/** Two top-level model versions for the [[AvroBridge]] spec. Top-level (not nested in the spec
  * class) so kindlings' `new T(...)` derivation isn't tripped by a missing outer accessor.
  * `PersonV2` adds an `age` field over `PersonV1` — the canonical "schema evolved" shape.
  */
final case class PersonV1(name: String)

object PersonV1:
  given AvroEncoder[PersonV1] = AvroEncoder.derived
  given AvroDecoder[PersonV1] = AvroDecoder.derived
  given AvroSchemaFor[PersonV1] = AvroSchemaFor.derived

final case class PersonV2(name: String, age: Int)

object PersonV2:
  given AvroEncoder[PersonV2] = AvroEncoder.derived
  given AvroDecoder[PersonV2] = AvroDecoder.derived
  given AvroSchemaFor[PersonV2] = AvroSchemaFor.derived

/** Behaviour spec for [[AvroBridge]] — the `Affine`-carried, two-version migration bridge whose
  * fallible build is simulated by `T = BridgedBytes = Either[AvroFailure, Array[Byte]]`.
  */
class AvroBridgeSpec extends Specification:

  // Encode a value to its Avro binary body under its own codec's schema.
  private def bytesOf[T](t: T)(using c: AvroCodec[T]): Array[Byte] =
    AvroSpecFixtures.toBinaryValue(c.encode(t), c.schema)

  // Decode Avro binary back to a value under its own codec's schema.
  private def valueOf[T](bs: Array[Byte])(using c: AvroCodec[T]): T =
    AvroCodec.parseInputIor(bs, c.schema).toOption.flatMap(c.decodeEither(_).toOption).get

  "forward bridge V1 -> V2: read V1, migrate via modify, get V2 bytes" >> {
    val bridge = AvroBridge.between[PersonV1, PersonV2]
    val v1bytes = bytesOf(PersonV1("Ada"))

    // read side: the writer value decodes as V1
    val readOk = bridge.getOption(v1bytes) === Some(PersonV1("Ada"))

    // migration: A => B supplied through modify; T is BridgedBytes = Either[AvroFailure, bytes]
    val migrated: AvroBridge.BridgedBytes = bridge.modify(v1 => PersonV2(v1.name, 0))(v1bytes)
    val writeOk = migrated match
      case Right(v2bytes) => valueOf[PersonV2](v2bytes) === PersonV2("Ada", 0)
      case Left(f)        =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($f)"): org.specs2.execute.Result

    readOk.and(writeOk)
  }

  "reverse: bridge[V1, V2].reverse is the V2 -> V1 bridge (codecs swapped)" >> {
    val back: AvroBridge[PersonV2, PersonV1] = AvroBridge.between[PersonV1, PersonV2].reverse
    val v2bytes = bytesOf(PersonV2("Bo", 41))

    back.modify(v2 => PersonV1(v2.name))(v2bytes) match
      case Right(v1bytes) => valueOf[PersonV1](v1bytes) === PersonV1("Bo")
      case Left(f)        =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($f)"): org.specs2.execute.Result
  }

  "replace overwrites the write focus without reading it (on a decodable source)" >> {
    val bridge = AvroBridge.between[PersonV1, PersonV2]
    val v1bytes = bytesOf(PersonV1("ignored"))
    bridge.replace(PersonV2("Cy", 7))(v1bytes) match
      case Right(v2bytes) => valueOf[PersonV2](v2bytes) === PersonV2("Cy", 7)
      case Left(f)        =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($f)"): org.specs2.execute.Result
  }

  "undecodable source: getOption None + modify surfaces the single AvroFailure in T's Left" >> {
    val bridge = AvroBridge.between[PersonV1, PersonV2]
    // Claims a 4-char string (zigzag length 4) but supplies no bytes → EOF on decode.
    val garbage = Array[Byte](0x08)

    val noneOk = bridge.getOption(garbage) === None
    val leftOk = bridge.modify(v1 => PersonV2(v1.name, 0))(garbage) match
      case Left(AvroFailure.BinaryParseFailed(_) | AvroFailure.DecodeFailed(_, _)) =>
        true === true: org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected Left(decode failure), got $other"): org.specs2.execute.Result

    noneOk.and(leftOk)
  }

end AvroBridgeSpec
