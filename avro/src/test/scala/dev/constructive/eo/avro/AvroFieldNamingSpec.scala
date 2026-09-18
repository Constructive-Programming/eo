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

/** NT codec for the grouped bytes-face read, derived OUTSIDE the snake_case scope so the DEFAULT
  * (identity) transform applies: the NT schema names (`landingPageId`, `clickId`) diverge from the
  * parent's snake_case names, and the selection is reordered relative to the parent layout. The
  * grouped read succeeds only if the bytes face projects the selected fields by resolved parent
  * name (the record face's `readFields` semantics) — never by handing the whole parent datum to the
  * NT codec.
  */
object AvroFieldNamingSpecNt:
  type LpAndClick = NamedTuple.NamedTuple[("landingPageId", "clickId"), (Int, String)]
  given AvroEncoder[LpAndClick] = AvroEncoder.derived
  given AvroDecoder[LpAndClick] = AvroDecoder.derived
  given AvroSchemaFor[LpAndClick] = AvroSchemaFor.derived
end AvroFieldNamingSpecNt

class AvroFieldNamingSpec extends Specification:

  import AvroFieldNamingSpec.*
  import AvroFieldNamingSpecNt.{*, given}

  private val clickCodec = summon[AvroCodec[Click]]
  private val click = Click("abc", 7)

  private val clickBytes =
    toBinary(clickCodec.encode(click).asInstanceOf[GenericRecord], clickCodec.schema)

  // The four byte-face examples (fixture guard, `.field` read, `.field` modify, `selectDynamic`)
  // were one code path with one fixture, so they are one example. The leading clause is the
  // tripwire: if the codec stopped transforming, nothing below would prove anything.
  "byte face: the derived schema really diverges, and .field / selectDynamic both resolve it" >> {
    val modified = codecPrism[Click].field(_.clickId).modify(_.toUpperCase)(clickBytes)
    (clickCodec.schema.getField("clickId") must beNull)
      .and(clickCodec.schema.getField("click_id") must not(beNull))
      .and(codecPrism[Click].field(_.clickId).getOption(clickBytes) must beSome("abc"))
      .and(codecPrism[Click].clickId.getOption(clickBytes) must beSome("abc"))
      .and(codecPrism[Click].getOption(modified) must beSome(Click("ABC", 7)))
  }

  "record face and a nested descent resolve the same snake_case names" >> {
    val rec = clickCodec.encode(click).asInstanceOf[GenericRecord]
    val bumped = codecPrism[Click].field(_.landingPageId).record.modifyUnsafe(_ + 1)(rec)
    val evCodec = summon[AvroCodec[Event]]
    val ev = Event("e1", Meta(42, "hot"))
    val bytes = toBinary(evCodec.encode(ev).asInstanceOf[GenericRecord], evCodec.schema)
    val nested = codecPrism[Event].field(_.meta).field(_.performanceSourceId)
    (codecPrism[Click].field(_.landingPageId).record.getOption(rec) must beSome(7))
      .and(clickCodec.decodeEither(bumped) must beRight(Click("abc", 8)))
      .and(nested.getOption(bytes) must beSome(42))
      .and(
        codecPrism[Event].getOption(nested.modify(_ + 1)(bytes)) must beSome(
          Event("e1", Meta(43, "hot"))
        )
      )
  }

  // Not folded into the block above: this is the ONE shape in the module where the focus codec's
  // own field names diverge from the parent's, so the grouped read is only correct if the bytes
  // face projects by RESOLVED PARENT name rather than handing the parent datum to the NT codec.
  "byte face: .fields(...) grouped read projects by schema name, not NT-codec name" >> {
    codecPrism[Click].fields(_.landingPageId, _.clickId).getOption(clickBytes) must beSome(
      (landingPageId = 7, clickId = "abc"): LpAndClick
    )
  }

  // `.fieldNamed`'s two verdicts — a name the schema HAS builds and reads, a name it LACKS is
  // refused at construction (issue #95, which used to be a silent runtime miss) — are pinned on a
  // divergent-name fixture by `AvroNominalResolutionSpec`, with strictly more of the refusal
  // message asserted than the duplicate that used to live here.

end AvroFieldNamingSpec
