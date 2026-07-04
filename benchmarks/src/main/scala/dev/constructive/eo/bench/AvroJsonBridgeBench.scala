package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.bench.fixture.ConversionDomain.*
import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.Optic.*

/** Cross-format bridge bench, BOTH directions between `Array[Byte]` (Avro binary) and `Array[Byte]`
  * (JSON) — the AvroPrism × JsoniterPrism pipeline the `AvroJsonBridgeSpec` proves correct. Each
  * strategy pair has byte-identical outputs (asserted once at setup):
  *
  *   - `naive*` — full materialisation: decode the WHOLE source object to case classes, build the
  *     output value, encode the WHOLE output with its codec. What a conventional consumer/producer
  *     bridge does.
  *   - `eo*` — branches only: the source-format prism slice-decodes each focused branch straight
  *     off the bytes, the target-format prism splices it into an existing payload (span scan/locate
  *     + encode + arraycopy splice). Neither the source object nor the output document is ever
  *     constructed — no root codec call on this path.
  *
  * Row naming: `{naive|eo}{Click|Wide}{ToJson|ToAvro}`. Two fixture shapes, deliberately at
  * opposite ends of the branch/object ratio:
  *
  *   - `*Click*` — [[TrackEnvelope]] ⇄ [[ClickMessage]]: the moved branch (11-field
  *     [[ClickPayload]]) IS most of the envelope, so the eo win is bounded — this is the honest
  *     worst case.
  *   - `*Wide*` — [[WideConversion]] (18 fields, 3 nested sub-records) ⇄ [[WideSummaryMessage]] (2
  *     scalar branches): allocation on the eo rows scales with the BRANCHES, on the naive rows with
  *     the OBJECT.
  *
  * Honest asymmetry: the reverse (`*ToAvro`) eo rows pay the Affine replace tax — each `.replace`
  * locates AND decodes the CURRENT focus before splicing, and allocates one output buffer per
  * replaced branch — while the reverse naive rows decode a tiny JSON document, so naive can win
  * B/op there even though it materialises the full Avro object on encode. The construction
  * guarantee (no root values, ever) holds regardless; fragment-shaped moves between two AVRO
  * payloads should use `sliceBytes` / `graftBytes` (see [[AvroBytesBench]]), which decode nothing.
  *
  * The construction claim is proven functionally in
  * `jsoniter/src/test/.../AvroJsonBridgeSpec.scala` (counting root codec); this bench puts a number
  * on it. Allocation pressure is the headline — run with the GC profiler:
  *
  * {{{
  *   sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 -prof gc .*AvroJsonBridgeBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroJsonBridgeBench extends JmhDefaults:

  import AvroJsonBridgeBench.given

  var envelopeBytes: Array[Byte] = uninitialized
  var wideBytes: Array[Byte] = uninitialized

  @Setup(Level.Trial)
  def init(): Unit =
    envelopeBytes = encodeEnvelope(inputEnvelope)
    wideBytes = encodeWide(wideConversion)
    // Each strategy pair must agree byte-for-byte, or the comparison is meaningless.
    assert(
      java.util.Arrays.equals(naiveClickToJson, eoClickToJson),
      "click→json outputs diverged",
    )
    assert(
      java.util.Arrays.equals(naiveWideToJson, eoWideToJson),
      "wide→json outputs diverged",
    )
    assert(
      java.util.Arrays.equals(naiveClickToAvro, eoClickToAvro),
      "click→avro outputs diverged",
    )
    assert(
      java.util.Arrays.equals(naiveWideToAvro, eoWideToAvro),
      "wide→avro outputs diverged",
    )

  // ---- envelope → click message (branch ≈ object: honest worst case) --

  @Benchmark def naiveClickToJson: Array[Byte] =
    val env = decodeEnvelope(envelopeBytes)
    writeToArray(
      ClickMessage(
        schema = "click-v1",
        click = env.payload.asInstanceOf[ClickPayload],
        partner = env.partnerId,
      )
    )

  @Benchmark def eoClickToJson: Array[Byte] =
    val withClick = clickPrism.getOption(envelopeBytes) match
      case Some(click) =>
        AvroJsonBridgeBench.clickJson.replace(click)(AvroJsonBridgeBench.clickTemplate)
      case None => AvroJsonBridgeBench.clickTemplate
    partnerPrism.getOption(envelopeBytes) match
      case Some(partner) => AvroJsonBridgeBench.partnerJson.replace(partner)(withClick)
      case None          => withClick

  // ---- wide record → 2-scalar summary (branch ≪ object) ---------------

  @Benchmark def naiveWideToJson: Array[Byte] =
    val w = decodeWide(wideBytes)
    writeToArray(
      WideSummaryMessage(
        schema = "conv-v1",
        conversionId = w.conversionId,
        country = w.country,
      )
    )

  @Benchmark def eoWideToJson: Array[Byte] =
    val withId = AvroJsonBridgeBench.conversionIdPrism.getOption(wideBytes) match
      case Some(id) =>
        AvroJsonBridgeBench.conversionIdJson.replace(id)(AvroJsonBridgeBench.wideTemplate)
      case None => AvroJsonBridgeBench.wideTemplate
    countryPrism.getOption(wideBytes) match
      case Some(country) => AvroJsonBridgeBench.countryJson.replace(country)(withId)
      case None          => withId

  // ---- reverse: JSON click message → envelope (branch ≈ object) -------

  @Benchmark def naiveClickToAvro: Array[Byte] =
    val msg = readFromArray[ClickMessage](AvroJsonBridgeBench.jsonClickBytes)
    encodeEnvelope(inputEnvelope.copy(payload = msg.click, partnerId = msg.partner))

  @Benchmark def eoClickToAvro: Array[Byte] =
    val withClick =
      AvroJsonBridgeBench.clickJson.getOption(AvroJsonBridgeBench.jsonClickBytes) match
        case Some(click) => clickPrism.replace(click)(envelopeBytes)
        case None        => envelopeBytes
    AvroJsonBridgeBench.partnerJson.getOption(AvroJsonBridgeBench.jsonClickBytes) match
      case Some(partner) => partnerPrism.replace(partner)(withClick)
      case None          => withClick

  // ---- reverse: JSON summary → wide record (branch ≪ object) ----------

  @Benchmark def naiveWideToAvro: Array[Byte] =
    val s = readFromArray[WideSummaryMessage](AvroJsonBridgeBench.jsonWideBytes)
    encodeWide(wideConversion.copy(conversionId = s.conversionId, country = s.country))

  @Benchmark def eoWideToAvro: Array[Byte] =
    val withId =
      AvroJsonBridgeBench.conversionIdJson.getOption(AvroJsonBridgeBench.jsonWideBytes) match
        case Some(id) => AvroJsonBridgeBench.conversionIdPrism.replace(id)(wideBytes)
        case None     => wideBytes
    AvroJsonBridgeBench.countryJson.getOption(AvroJsonBridgeBench.jsonWideBytes) match
      case Some(country) => countryPrism.replace(country)(withId)
      case None          => withId

end AvroJsonBridgeBench

/** The naive rows' output shapes — their `JsonValueCodec`s exist ONLY for those rows; the eo rows
  * splice into static templates and never construct one.
  */
final case class ClickMessage(schema: String, click: ClickPayload, partner: String)
final case class WideSummaryMessage(schema: String, conversionId: String, country: String)

object AvroJsonBridgeBench:

  import dev.constructive.eo.avro.codecPrism

  given JsonValueCodec[ClickMessage] = JsonCodecMaker.make
  given JsonValueCodec[WideSummaryMessage] = JsonCodecMaker.make
  given JsonValueCodec[ClickPayload] = JsonCodecMaker.make
  given JsonValueCodec[String] = JsonCodecMaker.make

  /** Avro-side scalar branch not already in [[ConversionDomain]]. */
  val conversionIdPrism = codecPrism[WideConversion].field(_.conversionId)

  /** JSON-side byte optics — branch codecs only. */
  val clickJson = JsoniterPrism[ClickPayload]("$.click")
  val partnerJson = JsoniterPrism[String]("$.partner")
  val conversionIdJson = JsoniterPrism[String]("$.conversionId")
  val countryJson = JsoniterPrism[String]("$.country")

  /** Static output templates. Placeholders are VALID encodings of their branch types (the Affine
    * write decodes the current focus before splicing), built once from the naive codecs so the
    * field order matches the `writeToArray` output byte-for-byte.
    */
  val clickTemplate: Array[Byte] =
    writeToArray(
      ClickMessage(
        schema = "click-v1",
        click = ClickPayload("", "", "", "", "", "", "", "", "", 0, 0L),
        partner = "",
      )
    )

  val wideTemplate: Array[Byte] =
    writeToArray(WideSummaryMessage(schema = "conv-v1", conversionId = "", country = ""))

  /** JSON inputs for the reverse direction — branch values deliberately DIFFERENT from the Avro
    * fixtures so every splice does real work.
    */
  val jsonClickBytes: Array[Byte] =
    writeToArray(
      ClickMessage(
        schema = "click-v1",
        click = inputEnvelope
          .payload
          .asInstanceOf[ClickPayload]
          .copy(clickId = "gcl-alt-0001", costMicros = 2_710_000L),
        partner = "partner-999",
      )
    )

  val jsonWideBytes: Array[Byte] =
    writeToArray(
      WideSummaryMessage(schema = "conv-v1", conversionId = "conv-alt-77", country = "DE")
    )

end AvroJsonBridgeBench
