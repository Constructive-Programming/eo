package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.optics.Optic.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** Bytes-in/bytes-out avro bench (zero-copy plan, Phase 1) over CONVERSION-SHAPED fixtures — a
  * 7-field envelope with a mid-record union payload and an 18-field record with nested sub-records
  * (see [[ConversionDomain]]).
  *
  * Three strategies per single-field READ, two per single-field MODIFY:
  *
  *   - `naive*` — full codec round-trip: bytes → `GenericRecord` → kindlings case class, access /
  *     `copy`, re-encode. What a conventional consumer/producer pair does today.
  *   - `eo*` — the eo-avro byte-carried optic (the AvroPrism default): `.getOption(bytes)` for
  *     reads (offset walk + slice decode, no record materialised), `.modify` for writes (offset
  *     walk + re-encode focus + 3-arraycopy splice).
  *   - `pruned*` (read only) — stock apache-avro projection: `GenericDatumReader` with a PRUNED
  *     reader schema carrying only the focused field, so schema resolution skips the rest. The
  *     no-new-library baseline.
  *
  * Plus the GRAFT trio: splicing a pre-sliced payload fragment into the output envelope's bytes
  * ([[dev.constructive.eo.avro.AvroPrism.graftBytes]] — no decode at all) vs the decode + re-encode
  * passthrough, with the slice+graft pipeline in between.
  *
  * Allocation pressure is half the story for the graft path — run with the GC profiler to see B/op
  * alongside ns/op:
  *
  * {{{
  *   sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 -prof gc .*AvroBytesBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroBytesBench extends JmhDefaults:

  import ConversionDomain.*

  var envelopeBytes: Array[Byte] = uninitialized
  var outputBytes: Array[Byte] = uninitialized
  var wideBytes: Array[Byte] = uninitialized
  var clickFragment: Array[Byte] = uninitialized

  private val prunedPartnerSchema = prunedSchemaOf(envelopeSchema, "partnerId")
  private val prunedCountrySchema = prunedSchemaOf(wideSchema, "country")

  @Setup(Level.Iteration)
  def init(): Unit =
    envelopeBytes = encodeEnvelope(inputEnvelope)
    outputBytes = encodeEnvelope(outputEnvelope)
    wideBytes = encodeWide(wideConversion)
    clickFragment = clickPrism.sliceBytesUnsafe(envelopeBytes).get.bytes

  // ---- envelope: read partnerId (scalar AFTER the mid-record union) --

  @Benchmark def naiveReadPartner: String =
    decodeEnvelope(envelopeBytes).partnerId

  @Benchmark def eoReadPartner: Option[String] =
    partnerPrism.getOption(envelopeBytes)

  @Benchmark def prunedReadPartner: AnyRef =
    toRecord(envelopeBytes, envelopeSchema, prunedPartnerSchema).get("partnerId")

  // ---- envelope: modify partnerId ------------------------------------

  @Benchmark def naiveModifyPartner: Array[Byte] =
    val e = decodeEnvelope(envelopeBytes)
    encodeEnvelope(e.copy(partnerId = e.partnerId.toUpperCase))

  @Benchmark def eoModifyPartner: Array[Byte] =
    partnerPrism.modify(_.toUpperCase)(envelopeBytes)

  // ---- wide record: read country (field 11 of 18) --------------------

  @Benchmark def naiveReadCountry: String =
    decodeWide(wideBytes).country

  @Benchmark def eoReadCountry: Option[String] =
    countryPrism.getOption(wideBytes)

  @Benchmark def prunedReadCountry: AnyRef =
    toRecord(wideBytes, wideSchema, prunedCountrySchema).get("country")

  // ---- wide record: modify country ------------------------------------

  @Benchmark def naiveModifyCountry: Array[Byte] =
    val w = decodeWide(wideBytes)
    encodeWide(w.copy(country = w.country.toUpperCase))

  @Benchmark def eoModifyCountry: Array[Byte] =
    countryPrism.modify(_.toUpperCase)(wideBytes)

  // ---- graft: splice the payload fragment into the output envelope ---

  /** The passthrough baseline: decode BOTH envelopes to native values, copy the payload across,
    * re-encode. This is today's emit path for a payload-preserving transform.
    */
  @Benchmark def naivePassthroughPayload: Array[Byte] =
    val in = decodeEnvelope(envelopeBytes)
    val out = decodeEnvelope(outputBytes)
    encodeEnvelope(out.copy(payload = in.payload))

  /** Pre-sliced fragment, graft only — the steady-state hot path when the fragment is sliced once
    * and grafted into many outputs (or carried between services).
    */
  @Benchmark def eoGraftPayload: Array[Byte] =
    clickPrism.graftBytesUnsafe(outputBytes, clickFragment)

  /** Full slice + graft pipeline — both offset walks, still no decode. */
  @Benchmark def eoSliceGraftPayload: Array[Byte] =
    clickPrism.graftBytesUnsafe(outputBytes, clickPrism.sliceBytesUnsafe(envelopeBytes).get.bytes)
