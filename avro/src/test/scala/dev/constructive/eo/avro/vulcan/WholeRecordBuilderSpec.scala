package dev.constructive.eo.avro.vulcan

import java.time.Instant

import scala.language.implicitConversions

import _root_.vulcan.Codec as VCodec
import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification

/** The derived whole-record builder's contract (issue #95): byte-for-byte fidelity with the full
  * vulcan codec on the fields the case class holds, round-trips through the codec's decode, the
  * name-resolution doctrine on the build side, the computed-column difference, the recursive-case
  * knot, and every construction-time refusal — surfaced as the union's Exception half, never
  * thrown.
  */
class WholeRecordBuilderSpec extends Specification:

  /** Unwrap a construction that the suite asserts SUCCEEDS (the refusal cases assert the other half
    * below).
    */
  private def built[A](r: Exception | WholeRecordBuilder[A]): WholeRecordBuilder[A] = r match
    case b: WholeRecordBuilder[A] => b
    case e: Exception             => sys.error(e.getMessage)

  private val vraw = summon[VCodec[ClickInfo]]
  private val builder = built(AvroVulcan.recordBuilder[ClickInfo])

  private val rich = ClickInfo(
    clickId = "ck_123",
    servedAt = Instant.ofEpochMilli(1726000000000L),
    sessionId = "s_abc",
    geo = Geo("US", "NY", "New York", 40.71, -74.01),
    userAgent = UserAgentInfo(
      "Firefox",
      "119.0",
      "Linux",
      "desktop",
      "en-US",
      doNotTrack = true,
      robot = false,
    ),
    postClick = Some(
      PostClick(
        1726000001000L,
        Some("ord_9"),
        129.99,
        "USD",
        3,
        completed = true,
        Some(7L),
        "/lp",
        "google",
      )
    ),
    ivt = Ivt(
      true,
      false,
      false,
      true,
      false,
      87,
      1,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      42L,
      "v2",
      "acme",
      "clean",
    ),
    mavenEntities = MavenEntities(
      Array[Byte](1, 2, 3),
      12,
      "se1,se2",
      "seg_a",
      "tax7",
      0.87,
      0.5f,
      true,
      TrafficClass.Paid,
    ),
    valid = true,
    clickTimestamp = Some(1726000000500L),
  )

  private val bare = rich.copy(postClick = None, clickTimestamp = None)

  "recordBuilder[ClickInfo]" should {

    "produce the codec's own record, field for field — deep equality incl. every nested record" in {
      builder.toRecord(rich) must beEqualTo(vraw.encode(rich).toOption.get)
    }

    "serialise to byte-identical wire bytes under the codec's schema" in {
      val derived: Array[Byte] =
        AvroCodec.encodeRecord(builder.toRecord(rich), builder.schema).toOption.get
      val coded: Array[Byte] =
        AvroCodec
          .encodeRecord(vraw.encode(rich).toOption.get, vraw.schema.toOption.get)
          .toOption
          .get
      derived.toSeq must beEqualTo(coded.toSeq)
    }

    "round-trip through the codec's decode, Some and None variants" in {
      vraw.decode(builder.toRecord(rich), builder.schema).toOption.get must beEqualTo(rich)
      vraw.decode(builder.toRecord(bare), builder.schema).toOption.get must beEqualTo(bare)
    }

    "leave the schema-only computed column at its default while the codec fills it — and still round-trip" in {
      val b = built(AvroVulcan.recordBuilder[WithComputed])
      val c = summon[VCodec[WithComputed]]
      val rec = b.toRecord(WithComputed(21))
      rec.get("derived") must beNull
      c.encode(WithComputed(21))
        .toOption
        .get
        .asInstanceOf[GenericRecord]
        .get("derived") must beEqualTo(
        Int.box(42)
      )
      c.decode(rec, c.schema.toOption.get).toOption.get must beEqualTo(WithComputed(21))
    }

    "resolve by NAME, never by declaration index: a reordered codec field list" in {
      val b = built(AvroVulcan.recordBuilder[Reordered])
      val c = summon[VCodec[Reordered]]
      val v = Reordered(7, "bee")
      b.toRecord(v) must beEqualTo(c.encode(v).toOption.get)
    }

    "resolve through the normalised-name rung: snake_case columns" in {
      val b = built(AvroVulcan.recordBuilder[Snakey])
      val c = summon[VCodec[Snakey]]
      val v = Snakey(9, "ada")
      b.toRecord(v) must beEqualTo(c.encode(v).toOption.get)
    }

    "install as the AvroCodec encode and drive codecPrism reads and writes" in {
      given AvroCodec[ClickInfo] = builder.asAvroCodec
      codecPrism[ClickInfo].record.reverseGet(rich) must beEqualTo(builder.toRecord(rich))
      val bytes = AvroCodec.encodeValue(rich).toOption.get
      codecPrism[ClickInfo].getOption(bytes) must beSome(rich)
      codecPrism[ClickInfo].field(_.sessionId).getOption(bytes) must beSome("s_abc")
    }
  }

  "a self-recursive shape" should {

    "terminate construction and build the chain through the level knot" in {
      // create-then-setFields: the recursive schema's own object fills its `next` union —
      // no self-referencing initialiser.
      val nodeSchema: Schema =
        val rec = Schema.createRecord(
          "Node",
          "recursive node fixture",
          "dev.constructive.eo.avro.vulcan",
          false,
        )
        val next =
          Schema.createUnion(Schema.create(Schema.Type.NULL), rec)
        rec.setFields(
          java
            .util
            .List
            .of(
              new Schema.Field("value", Schema.create(Schema.Type.INT)),
              new Schema.Field("next", next),
            )
        )
        rec
      val shape = WholeRecordBuilder.RecordShape(
        "Node",
        List(
          WholeRecordBuilder.FieldShape(
            "value",
            WholeRecordBuilder.DirectKind(Schema.Type.INT),
          ),
          WholeRecordBuilder.FieldShape(
            "next",
            WholeRecordBuilder.OptionKind(WholeRecordBuilder.SelfKind(0)),
          ),
        ),
      )
      WholeRecordBuilder.derive[Node](nodeSchema, shape, "spec[Node]") match
        case e: Exception                => sys.error(e.getMessage)
        case b: WholeRecordBuilder[Node] =>
          val rec = b.toRecord(Node(1, Some(Node(2, Some(Node(3, None))))))
          rec.get("value") must beEqualTo(Int.box(1))
          val n2 = rec.get("next").asInstanceOf[GenericRecord]
          n2.get("value") must beEqualTo(Int.box(2))
          val n3 = n2.get("next").asInstanceOf[GenericRecord]
          n3.get("value") must beEqualTo(Int.box(3))
          n3.get("next") must beNull
    }
  }

  "construction-time refusal" should {

    "refuse a case field renamed beyond normalisation, naming field and record" in {
      AvroVulcan.recordBuilder[Renamed] match
        case e: IllegalArgumentException =>
          e.getMessage must (contain("'beta'").and(contain("does not name a schema field")))
        case e: Exception                   => ko(e.getMessage)
        case _: WholeRecordBuilder[Renamed] => ko("expected a construction refusal")
    }

    "refuse an Option case field over a non-nullable column" in {
      AvroVulcan.recordBuilder[OptMismatch] match
        case e: IllegalArgumentException        => e.getMessage must contain("not a null-union")
        case e: Exception                       => ko(e.getMessage)
        case _: WholeRecordBuilder[OptMismatch] => ko("expected a construction refusal")
    }

    "refuse a case-class field whose column is not a record" in {
      AvroVulcan.recordBuilder[RecMis] match
        case e: IllegalArgumentException =>
          e.getMessage must (contain("needs a RECORD schema field").and(contain("is a STRING")))
        case e: Exception                  => ko(e.getMessage)
        case _: WholeRecordBuilder[RecMis] => ko("expected a construction refusal")
    }

    "refuse a LONG case field over an INT column (never widens silently)" in {
      AvroVulcan.recordBuilder[LongField] match
        case e: IllegalArgumentException =>
          e.getMessage must (contain("needs a LONG schema field").and(contain("is a INT")))
        case e: Exception                     => ko(e.getMessage)
        case _: WholeRecordBuilder[LongField] => ko("expected a construction refusal")
    }

    "refuse a column ambiguous under normalisation (the ambiguity sentinel)" in {
      AvroVulcan.recordBuilder[Ambig] match
        case e: IllegalArgumentException =>
          e.getMessage must contain("matches more than one schema field")
        case e: Exception                 => ko(e.getMessage)
        case _: WholeRecordBuilder[Ambig] => ko("expected a construction refusal")
    }

    "refuse two case fields claiming one column (the injectivity half)" in {
      AvroVulcan.recordBuilder[Collide] match
        case e: IllegalArgumentException =>
          e.getMessage must contain("collides with case field 'aCol'")
        case e: Exception                   => ko(e.getMessage)
        case _: WholeRecordBuilder[Collide] => ko("expected a construction refusal")
    }
  }
