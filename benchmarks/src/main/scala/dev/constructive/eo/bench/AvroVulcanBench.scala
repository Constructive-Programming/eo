package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import cats.syntax.all.*
import org.openjdk.jmh.annotations.*

import _root_.vulcan.Codec as VCodec

import avro.vulcan.AvroVulcan
import avro.{codecPrism, AvroCodec, AvroPrism}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema

/** vulcan → eo bridge (`AvroVulcan.codec`, issue #73) vs the kindlings-native path.
  *
  * Three planes, each with a `native` (kindlings-derived `AvroCodec`) / `bridged`
  * (`AvroVulcan.codec` over a `vulcan.Codec`) pair so the bridge's own overhead is the only
  * variable, plus a raw-vulcan reference where the operation exists without eo:
  *
  *   - '''codec plane''' — `encode` / `decodeEither` on the generic-value form (no bytes, no
  *     optics): the adapter itself.
  *   - '''bytes plane''' — root `codecPrism.getOption`: the typed entry point downstream actually
  *     calls (issue #73's motivating site). Bytes-out needs no benchmark of its own: the serialize
  *     step after `encode` is codec-independent, so the codec plane already isolates its delta.
  *   - '''field plane''' — a drilled `codecPrism[Hit].field(_.count).getOption`: the
  *     navigation-only pattern that used to need throw-stub codecs.
  *
  * B/op (`-prof gc`) is the decision metric; ns/op is directional (local boxes are noisy).
  *
  * First local `-prof gc` verdict (2026-07-13): the bridge is allocation-free — `encode_bridged`
  * and `decode_bridged` match `*_vulcanRaw` byte-for-byte (1208 / 880 B/op), so every byte over
  * `*_native` (56 / 48 B/op) is vulcan's own Either-based machinery, unreachable behind its public
  * API; and `fieldGet_bridged` == `fieldGet_native` (696 B/op) — navigation-only prisms (the
  * ex-throw-stub sites) pay ZERO for bridged evidence, the byte walk only reads the schema.
  */
object AvroVulcanImpls:

  case class Hit(name: String, count: Long, active: Boolean)

  object Hit:
    given AvroEncoder[Hit] = AvroEncoder.derived
    given AvroDecoder[Hit] = AvroDecoder.derived
    given AvroSchemaFor[Hit] = AvroSchemaFor.derived

  val vulcanCodec: VCodec[Hit] =
    VCodec.record(name = "Hit", namespace = "dev.constructive.eo.bench") { fb =>
      (fb("name", _.name), fb("count", _.count), fb("active", _.active)).mapN(Hit.apply)
    }

  val nativeCodec: AvroCodec[Hit] = summon[AvroCodec[Hit]]
  val bridgedCodec: AvroCodec[Hit] = AvroVulcan.codec[Hit](using vulcanCodec)
  val vulcanSchema: Schema = bridgedCodec.schema

  val hit: Hit = Hit("ada", 42L, active = true)

  // Generic-value forms, one per codec (schemas differ only in record name).
  val nativeValue: Any = nativeCodec.encode(hit)
  val bridgedValue: Any = bridgedCodec.encode(hit)

  // Wire forms, written by each side's own codec.
  val nativeBytes: Array[Byte] =
    AvroCodec.encodeValue(hit)(using nativeCodec).fold(f => sys.error(f.toString), identity)

  val bridgedBytes: Array[Byte] =
    AvroCodec.encodeValue(hit)(using bridgedCodec).fold(f => sys.error(f.toString), identity)

  // Optics built once — construction is not the hot path.
  val nativeRoot: AvroPrism[Hit] = codecPrism[Hit](using nativeCodec)
  val bridgedRoot: AvroPrism[Hit] = codecPrism[Hit](using bridgedCodec)
  val nativeCount: AvroPrism[Long] = nativeRoot.field(_.count)
  val bridgedCount: AvroPrism[Long] = bridgedRoot.field(_.count)

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroVulcanBench extends JmhDefaults:

  import AvroVulcanImpls.*

  // ---- codec plane: the adapter itself --------------------------------------------------------
  @Benchmark def encode_native: Any = nativeCodec.encode(hit)
  @Benchmark def encode_bridged: Any = bridgedCodec.encode(hit)
  @Benchmark def encode_vulcanRaw: Any = vulcanCodec.encode(hit)

  @Benchmark def decode_native: Either[Throwable, Hit] = nativeCodec.decodeEither(nativeValue)
  @Benchmark def decode_bridged: Either[Throwable, Hit] = bridgedCodec.decodeEither(bridgedValue)
  @Benchmark def decode_vulcanRaw: Any = vulcanCodec.decode(bridgedValue, vulcanSchema)

  // ---- bytes plane: the typed entry points ----------------------------------------------------
  @Benchmark def rootGet_native: Option[Hit] = nativeRoot.getOption(nativeBytes)
  @Benchmark def rootGet_bridged: Option[Hit] = bridgedRoot.getOption(bridgedBytes)

  // ---- field plane: navigation-only (the ex-throw-stub pattern) -------------------------------
  @Benchmark def fieldGet_native: Option[Long] = nativeCount.getOption(nativeBytes)
  @Benchmark def fieldGet_bridged: Option[Long] = bridgedCount.getOption(bridgedBytes)
