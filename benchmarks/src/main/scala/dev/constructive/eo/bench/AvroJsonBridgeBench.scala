package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.bench.fixture.ConversionDomain.*
import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.Optic.*

/** Cross-format bridge bench: `Array[Byte]` (Avro binary) → `Array[Byte]` (JSON), the
  * AvroPrism × JsoniterPrism pipeline the `AvroJsonBridgeSpec` proves correct. Each pair has
  * byte-identical outputs (asserted once at setup):
  *
  *   - `naive*` — full materialisation: decode the WHOLE Avro object to case classes, build an
  *     output message, encode the WHOLE message with a jsoniter codec. What a conventional
  *     consumer/producer bridge does.
  *   - `eo*` — branches only: AvroPrism slice-decodes each focused branch straight off the Avro
  *     bytes (offset walk, no IndexedRecord), JsoniterPrism splices it into a static JSON
  *     template (span scan + encode + arraycopy splice). Neither the source object nor the
  *     output document is ever constructed — no root `AvroCodec` decode, no output-document
  *     `JsonValueCodec` on this path.
  *
  * Two fixture shapes, deliberately at opposite ends of the branch/object ratio:
  *
  *   - `*ClickBridge` — [[TrackEnvelope]] → [[ClickMessage]]: the moved branch (11-field
  *     [[ClickPayload]]) IS most of the envelope, so the eo win is bounded — this is the honest
  *     worst case.
  *   - `*WideBridge` — [[WideConversion]] (18 fields, 3 nested sub-records) →
  *     [[WideSummaryMessage]] (2 scalar branches): allocation on the eo row scales with the
  *     BRANCHES, on the naive row with the OBJECT.
  *
  * The construction claim is proven functionally in
  * `jsoniter/src/test/.../AvroJsonBridgeSpec.scala` (counting root codec); this bench puts a
  * number on it. Allocation pressure is the headline — run with the GC profiler:
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

  @Setup(Level.Iteration)
  def init(): Unit =
    envelopeBytes = encodeEnvelope(inputEnvelope)
    wideBytes = encodeWide(wideConversion)
    // Each strategy pair must agree byte-for-byte, or the comparison is meaningless.
    assert(
      java.util.Arrays.equals(naiveClickBridge, eoClickBridge),
      "click bridge outputs diverged",
    )
    assert(
      java.util.Arrays.equals(naiveWideBridge, eoWideBridge),
      "wide bridge outputs diverged",
    )

  // ---- envelope → click message (branch ≈ object: honest worst case) --

  @Benchmark def naiveClickBridge: Array[Byte] =
    val env = decodeEnvelope(envelopeBytes)
    writeToArray(
      ClickMessage(
        schema = "click-v1",
        click = env.payload.asInstanceOf[ClickPayload],
        partner = env.partnerId,
      )
    )

  @Benchmark def eoClickBridge: Array[Byte] =
    val withClick = clickPrism.getOption(envelopeBytes) match
      case Some(click) =>
        AvroJsonBridgeBench.clickJson.replace(click)(AvroJsonBridgeBench.clickTemplate)
      case None => AvroJsonBridgeBench.clickTemplate
    partnerPrism.getOption(envelopeBytes) match
      case Some(partner) => AvroJsonBridgeBench.partnerJson.replace(partner)(withClick)
      case None          => withClick

  // ---- wide record → 2-scalar summary (branch ≪ object) ---------------

  @Benchmark def naiveWideBridge: Array[Byte] =
    val w = decodeWide(wideBytes)
    writeToArray(
      WideSummaryMessage(
        schema = "conv-v1",
        conversionId = w.conversionId,
        country = w.country,
      )
    )

  @Benchmark def eoWideBridge: Array[Byte] =
    val withId = AvroJsonBridgeBench.conversionIdPrism.getOption(wideBytes) match
      case Some(id) =>
        AvroJsonBridgeBench.conversionIdJson.replace(id)(AvroJsonBridgeBench.wideTemplate)
      case None => AvroJsonBridgeBench.wideTemplate
    countryPrism.getOption(wideBytes) match
      case Some(country) => AvroJsonBridgeBench.countryJson.replace(country)(withId)
      case None          => withId

end AvroJsonBridgeBench

/** The naive rows' output shapes — their `JsonValueCodec`s exist ONLY for those rows; the eo
  * rows splice into static templates and never construct one.
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

end AvroJsonBridgeBench
