package dev.constructive.eo.avro

import cats.data.{Chain, Ior}
import java.util.Arrays
import org.apache.avro.generic.IndexedRecord
import org.specs2.mutable.Specification

/** Regression pins for issue #101.
  *
  * `AvroCodec.parseInputUnsafe` used to turn an unparseable `Array[Byte]` / Avro-JSON `String` into
  * a fresh empty `GenericData.Record` built from the reader schema, so `*Unsafe` writes on the
  * record face returned a structurally valid empty record with every field erased. The entire
  * `*Unsafe` surface is gone; every record-face operation accepting `Array[Byte]` / `String` routes
  * through `AvroCodec.parseInputIor` and returns the structured parse failure as `Ior.Left`.
  */
class AvroParseFailureSpec extends Specification:

  import AvroSpecFixtures.*

  private type In = IndexedRecord | Array[Byte] | String

  private val namePrism: AvroRecordPrism[String] =
    codecPrism[Person].field(_.name).record

  private val namesTraversal: AvroRecordTraversal[String] =
    codecPrism[Basket].items.each.name.record

  /** Unparseable under the `Person` schema: `0x04` declares a two-byte `name` string and only one
    * byte follows, so both the root decode and the focused-span locate run past EOF.
    */
  private val junkBytes: Array[Byte] = Array[Byte](0x04, 0x41)

  private val junkJson = "not json at all"

  private def binaryParse(failure: AvroFailure): Boolean = failure match
    case _: AvroFailure.BinaryParseFailed => true
    case _                                => false

  private def jsonParse(failure: AvroFailure): Boolean = failure match
    case _: AvroFailure.JsonParseFailed => true
    case _                              => false

  private def reported(ior: Ior[Chain[AvroFailure], Any]): Option[AvroFailure] =
    ior match
      case Ior.Left(chain) => chain.headOption
      case _               => None

  private def recordWriteAndReadOps(
      prism: AvroRecordPrism[String],
      traversal: AvroRecordTraversal[String],
      in: In,
  ): List[(String, Option[AvroFailure])] =
    List(
      "prism.modify" -> reported(prism.modify(_.toUpperCase)(in)),
      "prism.transform" -> reported(prism.transform(identity[IndexedRecord])(in)),
      "prism.place" -> reported(prism.place("Zed")(in)),
      "prism.transfer" -> reported(prism.transfer((s: String) => s.toUpperCase)(in)("zed")),
      "prism.get" -> reported(prism.get(in)),
      "traversal.modify" -> reported(traversal.modify(_.toUpperCase)(in)),
      "traversal.transform" -> reported(traversal.transform(identity[IndexedRecord])(in)),
      "traversal.place" -> reported(traversal.place("Zed")(in)),
      "traversal.transfer" -> reported(traversal.transfer((s: String) => s.toUpperCase)(in)("zed")),
      "traversal.getAll" -> reported(traversal.getAll(in)),
    )

  // covers: every triple-input record-face op on both AvroRecordPrism and AvroRecordTraversal
  //   surfaces Ior.Left(BinaryParseFailed | JsonParseFailed) on junk input — never fabricates
  //   an empty GenericData.Record
  "record-face ops surface Ior.Left(BinaryParseFailed | JsonParseFailed) on junk input" >> {
    val unmatched = List(
      "bytes" -> recordWriteAndReadOps(namePrism, namesTraversal, junkBytes)
        .filterNot(_._2.exists(binaryParse))
        .map(_._1),
      "json" -> recordWriteAndReadOps(namePrism, namesTraversal, junkJson)
        .filterNot(_._2.exists(jsonParse))
        .map(_._1),
    )

    unmatched.filter(_._2.nonEmpty) === Nil
  }

  // covers: byte-face sliceBytes / graftBytes return Left(BinaryParseFailed) on junk bytes,
  //   and Optic.modify / getOption do not fabricate data
  "byte-face sliceBytes / graftBytes return Left(BinaryParseFailed) on junk bytes" >> {
    val nameP = codecPrism[Person].field(_.name)
    val namesT = codecPrism[Basket].items.each.name

    (nameP.sliceBytes(junkBytes).left.exists(binaryParse) === true)
      .and(nameP.graftBytes(junkBytes, Array[Byte](1, 2)).left.exists(binaryParse) === true)
      .and(nameP.getOption(junkBytes) === None)
      .and(Arrays.equals(nameP.modify(_.toUpperCase)(junkBytes), junkBytes) === true)
      .and(Arrays.equals(namesT.modify(_.toUpperCase)(junkBytes), junkBytes) === true)
  }

end AvroParseFailureSpec
