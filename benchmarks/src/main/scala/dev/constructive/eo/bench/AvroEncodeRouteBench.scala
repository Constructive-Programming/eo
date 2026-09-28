package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

import avro.AvroCodec
import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.{BinaryEncoder, EncoderFactory}
import org.openjdk.jmh.annotations.*
import scala.jdk.CollectionConverters.*

/** Fixtures + routes for [[AvroEncodeRouteBench]]. Kept top-level: kindlings' derivation, like
  * hearth's constructor synthesis, must not see an outer accessor.
  */
object EncodeRouteImpls:

  given AvroConfig = AvroConfig()

  /** All-`Option` nested record — the issue's 55-field `metrics` shape, scaled to 6. */
  final case class Metrics(
      a1: Option[Double],
      a2: Option[Double],
      b1: Option[Long],
      s1: Option[String],
      i1: Option[Int],
      b2: Option[Boolean],
  )

  object Metrics:
    given AvroEncoder[Metrics] = AvroEncoder.derived
    given AvroDecoder[Metrics] = AvroDecoder.derived
    given AvroSchemaFor[Metrics] = AvroSchemaFor.derived

  /** Multi-branch union — the issue's 18-branch slot, scaled to 6. */
  enum Event:
    case Ev0(v: Long)
    case Ev1(v: Long)
    case Ev2(v: Long)
    case Ev3(v: Long)
    case Ev4(v: Long)
    case Ev5(v: Long)

  object Event:
    given AvroEncoder[Event] = AvroEncoder.derived
    given AvroDecoder[Event] = AvroDecoder.derived
    given AvroSchemaFor[Event] = AvroSchemaFor.derived

  /** 10-field top level: strings, numerics, a boolean, one optional nested record, one union. */
  final case class Payload(
      id: String,
      tenant: String,
      source: String,
      ts: Long,
      seq: Long,
      amount: Double,
      flag: Boolean,
      kind: Int,
      metrics: Option[Metrics],
      event: Event,
  )

  object Payload:
    given AvroEncoder[Payload] = AvroEncoder.derived
    given AvroDecoder[Payload] = AvroDecoder.derived
    given AvroSchemaFor[Payload] = AvroSchemaFor.derived

  val payload: Payload =
    Payload(
      "id-1234567890",
      "tenant-xyz",
      "src.system.a",
      1_700_000_000L,
      987_654L,
      123_456.789,
      flag = true,
      kind = 3,
      metrics = Some(Metrics(Some(1.5), Some(2.5), None, Some("alpha"), None, Some(true))),
      event = Event.Ev3(99L),
    )

  val codec: AvroCodec[Payload] = summon[AvroCodec[Payload]]
  val schema: Schema = codec.schema
  val metricsUnion: Schema = schema.getField("metrics").schema()

  /** Which arm of a `union<null, X>` is the null arm — read off the DERIVED schema once at object
    * init, so the hand-written route below never guesses the spelling and never re-derives it on
    * the hot path.
    */
  def nullArm(s: Schema): Int =
    s.getTypes.asScala.indexWhere(_.getType == Schema.Type.NULL)

  private def nonNullArm(s: Schema): Schema =
    s.getTypes.asScala.find(_.getType != Schema.Type.NULL).get

  val metricsSomeIdx: Int = 1 - nullArm(metricsUnion)
  private val metricsSchema: Schema = nonNullArm(metricsUnion)

  val metricsNullIdx: Array[Int] =
    List("a1", "a2", "b1", "s1", "i1", "b2")
      .map(n => nullArm(metricsSchema.getField(n).schema()))
      .toArray

/** Whole-record ENCODE attribution — the route decomposition issue #119 asked for.
  *
  * The report measured `AvroCodec.derived` encode at ~1.4–1.7× a hand-written
  * direct-`BinaryEncoder` writer and ~9–18% more allocated, and hypothesised the intermediate
  * `GenericData.Record` tree. Four routes split the cost so the remaining gap is attributable:
  *
  *   - `eo_encodeValue` — the full production route: kindlings A → Any, then the module's
  *     `writeDatum`. Post-#119 `writeDatum` writes through per-thread reused buffer/encoder/writer
  *     plumbing, so its allocation over `eo_encodeToAny` is (almost) exactly the returned `byte[]`.
  *   - `naive_freshPlumbing` — the pre-#119 shape reproduced here as a baseline: a FRESH
  *     `ByteArrayOutputStream` (32-byte start, doubling), `GenericDatumWriter` and
  *     `BufferedBinaryEncoder` per call over the SAME prebuilt tree. The delta vs `eo_encodeValue`
  *     is the per-call plumbing churn the fix removed — the dominant write-side allocator, not
  *     `GenericDatumWriter` dispatch.
  *   - `eo_encodeToAny` — the kindlings tree build alone (the reporter's hypothesis: what the
  *     generic-record materialisation costs, isolated).
  *   - `handwritten_stream` — the reporter's baseline: fields straight to a reused `BinaryEncoder`
  *     in schema order, no tree. The gap to `eo_encodeValue` is tree + `GenericDatumWriter`
  *     dispatch — the irreducible cost of going through avro's datum model, and what a true
  *     streaming `A => Encoder => Unit` derivation would eventually remove.
  *
  * Run with the GC profiler — B/op is the metric, ns/op advises:
  * {{{
  *   sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1 -prof gc .*AvroEncodeRouteBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroEncodeRouteBench extends JmhDefaults:

  import EncodeRouteImpls.*

  var tree: Any = uninitialized
  var out: ByteArrayOutputStream = uninitialized
  var encoder: BinaryEncoder = uninitialized

  @Setup(Level.Trial)
  def init(): Unit =
    tree = codec.encode(payload)
    out = new ByteArrayOutputStream(16384)
    encoder = EncoderFactory.get().binaryEncoder(out, null)
    // sanity: the hand-written route must land on the codec's bytes, or the attribution is fiction
    val eo = AvroCodec.encodeValue(payload)(using codec).getOrElse(null)
    val hand = handwrittenToByteArray()
    require(
      eo != null && java.util.Arrays.equals(eo, hand),
      "handwritten route diverged from eo encode"
    )

  @Benchmark def eo_encodeValue: Array[Byte] =
    AvroCodec.encodeValue(payload)(using codec).fold(_ => null, identity)

  @Benchmark def eo_encodeToAny: Any = codec.encode(payload)

  /** The pre-#119 write plumbing, reconstructed: fresh BAOS + writer + encoder per call. */
  @Benchmark def naive_freshPlumbing: Array[Byte] =
    val o = new ByteArrayOutputStream()
    val writer = new GenericDatumWriter[Any](schema)
    val enc = EncoderFactory.get().binaryEncoder(o, null)
    writer.write(tree, enc)
    enc.flush()
    o.toByteArray

  @Benchmark def handwritten_stream: Array[Byte] = handwrittenToByteArray()

  private def handwrittenToByteArray(): Array[Byte] =
    out.reset()
    encoder = EncoderFactory.get().binaryEncoder(out, encoder)
    val e = encoder
    e.writeString(payload.id)
    e.writeString(payload.tenant)
    e.writeString(payload.source)
    e.writeLong(payload.ts)
    e.writeLong(payload.seq)
    e.writeDouble(payload.amount)
    e.writeBoolean(payload.flag)
    e.writeInt(payload.kind)
    payload.metrics match
      case Some(m) =>
        e.writeIndex(metricsSomeIdx)
        m.a1 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(0)); e.writeDouble(v)
          case None    => e.writeIndex(metricsNullIdx(0))
        m.a2 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(1)); e.writeDouble(v)
          case None    => e.writeIndex(metricsNullIdx(1))
        m.b1 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(2)); e.writeLong(v)
          case None    => e.writeIndex(metricsNullIdx(2))
        m.s1 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(3)); e.writeString(v)
          case None    => e.writeIndex(metricsNullIdx(3))
        m.i1 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(4)); e.writeInt(v)
          case None    => e.writeIndex(metricsNullIdx(4))
        m.b2 match
          case Some(v) => e.writeIndex(1 - metricsNullIdx(5)); e.writeBoolean(v)
          case None    => e.writeIndex(metricsNullIdx(5))
      case None => e.writeIndex(nullArm(metricsUnion))
    payload.event match
      case Event.Ev0(v) => e.writeIndex(0); e.writeLong(v)
      case Event.Ev1(v) => e.writeIndex(1); e.writeLong(v)
      case Event.Ev2(v) => e.writeIndex(2); e.writeLong(v)
      case Event.Ev3(v) => e.writeIndex(3); e.writeLong(v)
      case Event.Ev4(v) => e.writeIndex(4); e.writeLong(v)
      case Event.Ev5(v) => e.writeIndex(5); e.writeLong(v)
    e.flush()
    out.toByteArray
  end handwrittenToByteArray

end AvroEncodeRouteBench
