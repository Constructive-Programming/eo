package dev.constructive.eo.avro

import scala.collection.immutable.ArraySeq
import scala.language.implicitConversions

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalTime}
import java.util.UUID
import org.apache.avro.Schema
import org.specs2.mutable.Specification

import ProbeToken.ProbeToken

/** An opaque alias — `AvroProbe[ProbeToken]` is unreachable from outside [[ProbeToken]], which is
  * exactly the "custom type" abstention the probe must degrade on.
  */
object ProbeToken:

  opaque type ProbeToken = String

  def apply(s: String): ProbeToken = s

end ProbeToken

// ---- fixtures ---------------------------------------------------------------------------------

final case class ProbeRow(id: String, count: Long, flag: Boolean)

object ProbeRow:

  given AvroEncoder[ProbeRow] = AvroEncoder.derived
  given AvroDecoder[ProbeRow] = AvroDecoder.derived
  given AvroSchemaFor[ProbeRow] = AvroSchemaFor.derived

final case class ProbeNested(row: ProbeRow, tag: String)

object ProbeNested:

  given AvroEncoder[ProbeNested] = AvroEncoder.derived
  given AvroDecoder[ProbeNested] = AvroDecoder.derived
  given AvroSchemaFor[ProbeNested] = AvroSchemaFor.derived

/** A sum with two alternatives — the probe uses one representative per branch. */
enum ProbeShape:
  case Dot(n: Int)
  case Dash(s: String)

/** A case class with a field whose type has no [[AvroProbe]] (an opaque type: the compiler sees
  * `ProbeToken` as abstract here, so neither a leaf instance nor a `Mirror` applies).
  */
final case class ProbeOpaque(id: String, token: ProbeToken)

/** A NON-record codec, to check that a parent which does not encode to a record abstains. */
final case class ProbeLeaf(n: Int)

object ProbeLeaf:

  given AvroEncoder[ProbeLeaf] = AvroEncoder.derived
  given AvroDecoder[ProbeLeaf] = AvroDecoder.derived
  given AvroSchemaFor[ProbeLeaf] = AvroSchemaFor.derived

/** Sentinel synthesis and the differential probe's driver (issue #100).
  *
  * The end-to-end contract — which codec shapes the probe closes and which it must leave alone — is
  * `vulcan.ResolutionProbeSpec`'s. This spec is the machinery underneath it: that every leaf and
  * container type the probe claims to cover really yields two DISTINCT values, that a product's
  * synthesiser lines up with its case fields in declaration order, and that every abstention path
  * (no synthesiser, no slot, an unreadable parent) answers `None` instead of throwing.
  */
class AvroProbeSpec extends Specification:

  private def distinct[A](pair: Option[(A, A)]): Boolean =
    pair.exists { (a, b) =>
      (a, b) match
        case (x: Array[Byte], y: Array[Byte]) => !java.util.Arrays.equals(x, y)
        case _                                => a != b
    }

  "every leaf sentinel pair is two DISTINCT values" >> {
    val cells = List(
      distinct(AvroProbe.pair[Boolean]),
      distinct(AvroProbe.pair[Byte]),
      distinct(AvroProbe.pair[Short]),
      distinct(AvroProbe.pair[Int]),
      distinct(AvroProbe.pair[Long]),
      distinct(AvroProbe.pair[Float]),
      distinct(AvroProbe.pair[Double]),
      distinct(AvroProbe.pair[Char]),
      distinct(AvroProbe.pair[String]),
      distinct(AvroProbe.pair[Array[Byte]]),
      distinct(AvroProbe.pair[ByteBuffer]),
      distinct(AvroProbe.pair[BigDecimal]),
      distinct(AvroProbe.pair[UUID]),
      distinct(AvroProbe.pair[Instant]),
      distinct(AvroProbe.pair[LocalDate]),
      distinct(AvroProbe.pair[LocalTime]),
    )
    cells.filterNot(identity) === Nil
  }

  "container sentinels are distinct, and carry the leaf pair inside" >> {
    val cells = List(
      distinct(AvroProbe.pair[Option[String]]),
      distinct(AvroProbe.pair[List[Int]]),
      distinct(AvroProbe.pair[Vector[Long]]),
      distinct(AvroProbe.pair[Seq[String]]),
      distinct(AvroProbe.pair[Set[Int]]),
      distinct(AvroProbe.pair[ArraySeq[Int]]),
      distinct(AvroProbe.pair[Map[String, Long]]),
      distinct(AvroProbe.pair[Either[String, Long]]),
    )
    (cells.filterNot(identity) === Nil)
      .and(AvroProbe.pair[Option[Int]] === Some((Some(1001), Some(1002))))
      .and(
        AvroProbe.pair[Map[String, Long]] === Some(
          (Map("eo-probe-0" -> 1000000001L), Map("eo-probe-1" -> 1000000002L))
        )
      )
  }

  "a product's sentinels differ in EVERY field; a sum's in their branch" >> {
    val row = AvroProbe.pair[ProbeRow]
    val nested = AvroProbe.pair[ProbeNested]
    val shape = AvroProbe.pair[ProbeShape]
    (row === Some(
      (ProbeRow("eo-probe-0", 1000000001L, true), ProbeRow("eo-probe-1", 1000000002L, false))
    ))
      .and(nested.isDefined)
      .and(shape.map((a, _) => a) === Some(ProbeShape.Dot(1001)))
      .and(shape.map((_, b) => b) === Some(ProbeShape.Dash("eo-probe-0")))
  }

  "`Slots` reports one pair per case field, in DECLARATION order, and builds an A back" >> {
    val slots = summon[AvroProbe.Slots[ProbeRow]]
    val pairs = slots.pairs
    (pairs.length === 3)
      .and(pairs(0) === (("eo-probe-0": Any, "eo-probe-1": Any)))
      .and(pairs(1) === ((1000000001L: Any, 1000000002L: Any)))
      .and(pairs(2) === ((true: Any, false: Any)))
      .and(slots.build(Array[Any]("x", 5L, false)) === ProbeRow("x", 5, false))
  }

  "a field type with no sentinel drops the WHOLE synthesiser — the abstention is compile-time" >> {
    (probeAvailable[ProbeRow] must beTrue)
      .and(probeAvailable[ProbeNested] must beTrue)
      .and(probeAvailable[ProbeOpaque] must beFalse)
      .and(probeAvailable[ProbeToken] must beFalse)
  }

  // ---- the driver's abstention paths ---------------------------------------------------------

  "the driver answers `None` for every unreadable parent, never a wrong name" >> {
    val codec = summon[AvroCodec[ProbeRow]]
    val slots = summon[AvroProbe.Slots[ProbeRow]]
    val fieldCodec = summon[AvroCodec[String]]
    val record = codec.schema
    val foreign = summon[AvroCodec[ProbeNested]].schema
    // A codec that has never been probed, so its abstention is not answered from the cache.
    val unprobed = summon[AvroCodec[ProbeLeaf]]
    (AvroProbe.fieldName(codec, fieldCodec, Some(slots), 0, record) must beSome("id"))
      // declaration index outside the case class
      .and(AvroProbe.fieldName(codec, fieldCodec, Some(slots), 99, record) must beNone)
      .and(AvroProbe.fieldName(codec, fieldCodec, Some(slots), -1, record) must beNone)
      // the record the walk will use does not carry the verdict
      .and(AvroProbe.fieldName(codec, fieldCodec, Some(slots), 0, foreign) must beNone)
      // a non-record parent has no slots to diff at all
      .and(
        AvroProbe
          .fieldName(codec, fieldCodec, Some(slots), 0, Schema.create(Schema.Type.STRING)) must
          beNone
      )
      // no synthesiser at all
      .and(
        AvroProbe.fieldName(unprobed, summon[AvroCodec[Int]], None, 0, unprobed.schema) must beNone
      )
  }

  "a parent whose encode is not a record has no slots to diff" >> {
    val slots = summon[AvroProbe.Slots[ProbeLeaf]]
    val scalar = new ScalarCodec(summon[AvroCodec[ProbeLeaf]])
    AvroProbe.fieldName(scalar, summon[AvroCodec[Int]], Some(slots), 0, scalar.schema) must beNone
  }

  /** A codec that encodes to a bare box — the driver must refuse to diff it rather than index a
    * non-record.
    */
  final private class ScalarCodec[A](inner: AvroCodec[A]) extends AvroCodec[A]:

    val schema: Schema = Schema.create(Schema.Type.STRING)

    def encode(a: A): Any = "not-a-record"

    def decodeEither(any: Any): Either[Throwable, A] = inner.decodeEither(any)

  private inline def probeAvailable[A]: Boolean =
    scala.compiletime.summonFrom {
      case _: AvroProbe.Slots[A] => true
      case _                     => false
    }

end AvroProbeSpec
