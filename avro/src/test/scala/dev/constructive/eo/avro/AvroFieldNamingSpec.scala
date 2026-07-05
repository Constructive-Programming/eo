package dev.constructive.eo.avro

import scala.language.implicitConversions

import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.specs2.mutable.Specification

/** Regression spec for issue #35 — field navigation must honour the SCHEMA field name, not the raw
  * Scala field name. Every fixture here derives its codec under a snake_case name transform, so the
  * schema fields are `click_id` / `landing_page_id` / … while the case-class fields stay camelCase.
  * `.field(_.clickId)` must resolve `click_id` by declaration position, on both the byte and record
  * faces, for reads and writes, through nested records and multi-field foci.
  */
object AvroFieldNamingSpec:

  // snake_case schema — clickId -> click_id. Shadows AvroCodec.default (identity) lexically.
  given AvroConfig = AvroConfig().withSnakeCaseFieldNames

  case class Click(clickId: String, landingPageId: Int)

  object Click:
    given AvroEncoder[Click] = AvroEncoder.derived
    given AvroDecoder[Click] = AvroDecoder.derived
    given AvroSchemaFor[Click] = AvroSchemaFor.derived

  case class Meta(performanceSourceId: Int, tag: String)

  object Meta:
    given AvroEncoder[Meta] = AvroEncoder.derived
    given AvroDecoder[Meta] = AvroDecoder.derived
    given AvroSchemaFor[Meta] = AvroSchemaFor.derived

  case class Event(eventId: String, meta: Meta)

  object Event:
    given AvroEncoder[Event] = AvroEncoder.derived
    given AvroDecoder[Event] = AvroDecoder.derived
    given AvroSchemaFor[Event] = AvroSchemaFor.derived

  private def toBinary(record: GenericRecord, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[GenericRecord](schema).write(record, encoder)
    encoder.flush()
    out.toByteArray

end AvroFieldNamingSpec

class AvroFieldNamingSpec extends Specification:

  import AvroFieldNamingSpec.*

  private val clickCodec = summon[AvroCodec[Click]]
  private val click = Click("abc", 7)
  private val clickBytes = toBinary(clickCodec.encode(click).asInstanceOf[GenericRecord], clickCodec.schema)

  "the fixture really uses a divergent (snake_case) schema name" >> {
    // Guards the whole spec: if the codec stopped transforming, these fixtures would prove nothing.
    (clickCodec.schema.getField("clickId") must beNull) and
      (clickCodec.schema.getField("click_id") must not(beNull))
  }

  "byte face: .field(_.clickId).getOption resolves click_id (issue #35 repro)" >> {
    codecPrism[Click].field(_.clickId).getOption(clickBytes) must beSome("abc")
  }

  "byte face: .modify round-trips through the snake_case field" >> {
    val out = codecPrism[Click].field(_.clickId).modify(_.toUpperCase)(clickBytes)
    codecPrism[Click].getOption(out) must beSome(Click("ABC", 7))
  }

  "byte face: selectDynamic sugar resolves the schema name too" >> {
    codecPrism[Click].clickId.getOption(clickBytes) must beSome("abc")
  }

  "record face: read + modify resolve the schema name" >> {
    val rec = clickCodec.encode(click).asInstanceOf[GenericRecord]
    val readOk = codecPrism[Click].field(_.landingPageId).record.getOption(rec) must beSome(7)
    val modified = codecPrism[Click].field(_.landingPageId).record.modifyUnsafe(_ + 1)(rec)
    val modOk = clickCodec.decodeEither(modified) must beRight(Click("abc", 8))
    readOk and modOk
  }

  "nested record: .field(_.meta).field(_.performanceSourceId) descends under snake_case" >> {
    val ev = Event("e1", Meta(42, "hot"))
    val evCodec = summon[AvroCodec[Event]]
    val bytes = toBinary(evCodec.encode(ev).asInstanceOf[GenericRecord], evCodec.schema)
    val optic = codecPrism[Event].field(_.meta).field(_.performanceSourceId)
    val readOk = optic.getOption(bytes) must beSome(42)
    val writeOk = codecPrism[Event].getOption(optic.modify(_ + 1)(bytes)) must beSome(
      Event("e1", Meta(43, "hot"))
    )
    readOk and writeOk
  }

  ".fieldNamed escape hatch navigates by explicit schema name" >> {
    codecPrism[Click].fieldNamed[String]("click_id").getOption(clickBytes) must beSome("abc")
  }

  "a bad explicit .fieldNamed misses (None), it does not corrupt" >> {
    codecPrism[Click].fieldNamed[String]("no_such_field").getOption(clickBytes) must beNone
  }

end AvroFieldNamingSpec
