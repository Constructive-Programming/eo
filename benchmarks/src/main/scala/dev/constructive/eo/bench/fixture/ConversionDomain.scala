package dev.constructive.eo
package bench
package fixture

import dev.constructive.eo.avro.{AvroCodec, AvroPrism, codecPrism}

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord, IndexedRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

/** Conversion-pipeline-shaped fixtures for `AvroBytesBench` — deliberately NOT toy records.
  *
  * Two shapes, mirroring the Kafka payloads the bytes-out / slice / graft surface was built for:
  *
  *   - [[TrackEnvelope]] — a 7-field envelope whose 4th field `payload` is a MID-RECORD UNION of
  *     two alternative record types ([[ClickPayload]], 11 string/int fields, the realistic branch;
  *     [[ImpressionPayload]], the small sibling). Slicing / grafting the payload while the suffix
  *     fields (`partnerId`, `schemaVersion`, `region`) survive is exactly the passthrough emit
  *     path.
  *   - [[WideConversion]] — an 18-field record with three nested sub-records ([[Money]] ×2,
  *     [[AttributionWindow]]), so the full-codec-roundtrip baseline pays a real per-field cost.
  */
final case class ClickPayload(
    clickId: String,
    campaignId: String,
    adGroupId: String,
    creativeId: String,
    keyword: String,
    matchType: String,
    device: String,
    gclid: String,
    landingUrl: String,
    position: Int,
    costMicros: Long,
) extends TrackPayload

final case class ImpressionPayload(
    impressionId: String,
    slot: String,
    viewable: Boolean,
) extends TrackPayload

sealed trait TrackPayload

final case class TrackEnvelope(
    eventId: String,
    eventTime: Long,
    source: String,
    payload: TrackPayload,
    partnerId: String,
    schemaVersion: Int,
    region: String,
)

final case class Money(amountMicros: Long, currency: String)

final case class AttributionWindow(startEpochMs: Long, endEpochMs: Long)

final case class WideConversion(
    conversionId: String,
    clickId: String,
    visitorId: String,
    campaignId: String,
    adGroupId: String,
    creativeId: String,
    keyword: String,
    device: String,
    browser: String,
    os: String,
    country: String,
    dma: String,
    revenue: Money,
    payout: Money,
    attribution: AttributionWindow,
    conversionTimeMs: Long,
    isDuplicate: Boolean,
    note: String,
)

object ConversionDomain:

  given AvroEncoder[ClickPayload] = AvroEncoder.derived
  given AvroDecoder[ClickPayload] = AvroDecoder.derived
  given AvroSchemaFor[ClickPayload] = AvroSchemaFor.derived

  given AvroEncoder[ImpressionPayload] = AvroEncoder.derived
  given AvroDecoder[ImpressionPayload] = AvroDecoder.derived
  given AvroSchemaFor[ImpressionPayload] = AvroSchemaFor.derived

  given AvroEncoder[TrackPayload] = AvroEncoder.derived
  given AvroDecoder[TrackPayload] = AvroDecoder.derived
  given AvroSchemaFor[TrackPayload] = AvroSchemaFor.derived

  given AvroEncoder[TrackEnvelope] = AvroEncoder.derived
  given AvroDecoder[TrackEnvelope] = AvroDecoder.derived
  given AvroSchemaFor[TrackEnvelope] = AvroSchemaFor.derived

  given AvroEncoder[Money] = AvroEncoder.derived
  given AvroDecoder[Money] = AvroDecoder.derived
  given AvroSchemaFor[Money] = AvroSchemaFor.derived

  given AvroEncoder[AttributionWindow] = AvroEncoder.derived
  given AvroDecoder[AttributionWindow] = AvroDecoder.derived
  given AvroSchemaFor[AttributionWindow] = AvroSchemaFor.derived

  given AvroEncoder[WideConversion] = AvroEncoder.derived
  given AvroDecoder[WideConversion] = AvroDecoder.derived
  given AvroSchemaFor[WideConversion] = AvroSchemaFor.derived

  /** Whole-document codecs the `naive*` roundtrip baselines decode / encode through. */
  val envelopeCodec: AvroCodec[TrackEnvelope] = summon[AvroCodec[TrackEnvelope]]
  val wideCodec: AvroCodec[WideConversion] = summon[AvroCodec[WideConversion]]

  val envelopeSchema: Schema = envelopeCodec.schema
  val wideSchema: Schema = wideCodec.schema

  /** Suffix scalar AFTER the mid-record union — the read/modify focus on the envelope, so the
    * byte cursor has to skip the union payload to reach it.
    */
  val partnerPrism: AvroPrism[String] = codecPrism[TrackEnvelope].field(_.partnerId)

  /** The union-branch focus the graft benchmarks splice through. */
  val clickPrism: AvroPrism[ClickPayload] =
    codecPrism[TrackEnvelope].field(_.payload).union[ClickPayload]

  /** Mid-record scalar on the wide record (field 11 of 18, past no nested records — the nested
    * [[Money]] / [[AttributionWindow]] sub-records sit after it and ride the suffix).
    */
  val countryPrism: AvroPrism[String] = codecPrism[WideConversion].field(_.country)

  /** Deterministic fixtures — no randomness, every fork sees identical input. */
  val inputEnvelope: TrackEnvelope =
    TrackEnvelope(
      eventId = "evt-0001-4dc9-8a2b-4f1a9c33",
      eventTime = 1_751_600_000_000L,
      source = "google-ads",
      payload = ClickPayload(
        clickId = "gcl-8c33-41f2-9d0a-77aa1289",
        campaignId = "cmp-2200481",
        adGroupId = "adg-90114532",
        creativeId = "cre-66120987",
        keyword = "running shoes",
        matchType = "EXACT",
        device = "MOBILE",
        gclid = "EAIaIQobChMI4v3Wl9uC_QIVj4xoCR0",
        landingUrl = "https://shop.example.com/landing?utm_campaign=cmp-2200481",
        position = 2,
        costMicros = 1_340_000L,
      ),
      partnerId = "partner-778",
      schemaVersion = 3,
      region = "us-east-2",
    )

  /** The output envelope the graft benchmarks splice INTO — sits on the small union branch so a
    * graft has to switch branches (the realistic emit-path shape: a fresh output record whose
    * payload slot gets the input's payload).
    */
  val outputEnvelope: TrackEnvelope =
    inputEnvelope.copy(
      eventId = "evt-0002-out",
      payload = ImpressionPayload("imp-000000", "slot-a", viewable = true),
      partnerId = "partner-778-out",
    )

  val wideConversion: WideConversion =
    WideConversion(
      conversionId = "cnv-51aa-4711-b3c2-9d88f0e2",
      clickId = "gcl-8c33-41f2-9d0a-77aa1289",
      visitorId = "vis-30cf-49a1-b7d3-11209aa4",
      campaignId = "cmp-2200481",
      adGroupId = "adg-90114532",
      creativeId = "cre-66120987",
      keyword = "running shoes",
      device = "MOBILE",
      browser = "Chrome",
      os = "Android",
      country = "US",
      dma = "501",
      revenue = Money(49_990_000L, "USD"),
      payout = Money(4_990_000L, "USD"),
      attribution = AttributionWindow(1_751_500_000_000L, 1_751_600_000_000L),
      conversionTimeMs = 1_751_555_555_555L,
      isDuplicate = false,
      note = "post-click purchase, 30d window",
    )

  /** Stock-avro projection baseline: a PRUNED reader schema carrying only the focused field.
    * Schema resolution skips every writer field absent from the reader — the "no new library"
    * way to read one field without materialising the rest.
    */
  def prunedSchemaOf(full: Schema, fieldName: String): Schema =
    val source = full.getField(fieldName)
    val fields = new java.util.ArrayList[Schema.Field]()
    fields.add(new Schema.Field(fieldName, source.schema, null, null))
    Schema.createRecord(full.getName, null, full.getNamespace, false, fields)

  /** Encode a kindlings-encoded record to Avro binary. */
  def toBytes(record: IndexedRecord, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[IndexedRecord](schema).write(record, encoder)
    encoder.flush()
    out.toByteArray

  /** Parse Avro binary to a `GenericRecord` under (writer, reader) schemas. */
  def toRecord(bytes: Array[Byte], writer: Schema, reader: Schema): GenericRecord =
    new GenericDatumReader[GenericRecord](writer, reader)
      .read(null, DecoderFactory.get().binaryDecoder(bytes, null))

  /** Native-value round-trip entry for the `naive*` baselines: bytes → case class. */
  def decodeEnvelope(bytes: Array[Byte]): TrackEnvelope =
    envelopeCodec.decodeEither(toRecord(bytes, envelopeSchema, envelopeSchema)).toOption.get

  /** Native-value round-trip exit for the `naive*` baselines: case class → bytes. */
  def encodeEnvelope(e: TrackEnvelope): Array[Byte] =
    toBytes(envelopeCodec.encode(e).asInstanceOf[IndexedRecord], envelopeSchema)

  /** [[decodeEnvelope]]'s wide-record sibling. */
  def decodeWide(bytes: Array[Byte]): WideConversion =
    wideCodec.decodeEither(toRecord(bytes, wideSchema, wideSchema)).toOption.get

  /** [[encodeEnvelope]]'s wide-record sibling. */
  def encodeWide(w: WideConversion): Array[Byte] =
    toBytes(wideCodec.encode(w).asInstanceOf[IndexedRecord], wideSchema)

end ConversionDomain
