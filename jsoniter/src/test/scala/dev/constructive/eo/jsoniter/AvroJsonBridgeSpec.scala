package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import org.apache.avro.generic.{GenericDatumWriter, IndexedRecord}
import org.apache.avro.io.EncoderFactory
import org.specs2.mutable.Specification

/** Cross-format bridge: `Array[Byte]` (Avro binary) → `Array[Byte]` (JSON), combining
  * [[dev.constructive.eo.avro.AvroPrism]] (byte-carried read: locate the branch's span, decode
  * only that slice) with [[JsoniterPrism]] (byte-carried write: encode the branch, splice it into
  * a JSON template).
  *
  * The load-bearing claim: '''the full object is never constructed — only the focused branches
  * are.''' Proven two ways:
  *
  *   - '''Dynamically''': the root `AvroCodec[Envelope]` in scope is a counting wrapper — its
  *     decode side increments [[BridgeFixtures.rootDecodes]]. The bridge moves the `click` branch
  *     from Avro bytes into JSON bytes with the counter still at ZERO; the record face
  *     (`.record.get`) as a positive control drives it up, proving the counter is wired to the
  *     only path that could materialise an `Envelope`.
  *   - '''Statically''': no `JsonValueCodec` for the output document exists anywhere in this spec
  *     — only `JsonValueCodec[Click]`. A full JSON-side object CANNOT be constructed because its
  *     codec was never derived.
  */
class AvroJsonBridgeSpec extends Specification:

  import AvroJsonBridgeSpec.*
  import BridgeFixtures.given

  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  "Avro bytes → JSON bytes: branch moves across formats with zero root constructions" >> {
    val env = Envelope("env-7", Click("https://x.example/a?b=1", 42L), "keep-me")
    val avroBytes = BridgeFixtures.toAvroBinary(env)

    // The two byte optics, one per format. Drilled once, reusable across payloads.
    val clickAvro = codecPrism[Envelope].field(_.click)
    val clickJson = JsoniterPrism[Click]("$.click")

    // The click placeholder must be a VALID Click encoding — the Affine write decodes the
    // current focus before splicing (a Hit carries the span AND the decoded value).
    val template: Array[Byte] =
      """{"schema":"click-v1","click":{"url":"","ts":0},"note":"static"}""".getBytes("UTF-8")

    BridgeFixtures.rootDecodes.set(0)

    // ---- the bridge: Avro slice-decode → JSON splice-encode ----
    val out: Array[Byte] = clickAvro.getOption(avroBytes) match
      case Some(click) => clickJson.replace(click)(template)
      case None        => template

    val bridgedOk = str(out) ===
      """{"schema":"click-v1","click":{"url":"https://x.example/a?b=1","ts":42},"note":"static"}"""

    // The Envelope was never constructed: the Avro side decoded ONLY the click slice (via
    // AvroCodec[Click]), the JSON side encoded ONLY the click branch.
    val neverConstructedOk = BridgeFixtures.rootDecodes.get === 0

    // Positive control — the record face DOES materialise the root, so the counter is
    // demonstrably wired to the only Envelope-constructing path.
    val roundTrip = codecPrism[Envelope].record.get(avroBytes)
    val controlOk = (roundTrip.toOption === Some(env)).and(BridgeFixtures.rootDecodes.get === 1)

    bridgedOk.and(neverConstructedOk).and(controlOk)
  }

  "Avro bytes → JSON bytes: scalar branch via the same bridge, still zero root constructions" >> {
    val env = Envelope("env-9", Click("u", 1L), "note-9")
    val avroBytes = BridgeFixtures.toAvroBinary(env)

    val idAvro = codecPrism[Envelope].field(_.id)
    val idJson = JsoniterPrism[String]("$.envelopeId")
    val template: Array[Byte] = """{"envelopeId":"","v":1}""".getBytes("UTF-8")

    BridgeFixtures.rootDecodes.set(0)
    val out = idAvro.getOption(avroBytes) match
      case Some(id) => idJson.replace(id)(template)
      case None     => template

    (str(out) === """{"envelopeId":"env-9","v":1}""")
      .and(BridgeFixtures.rootDecodes.get === 0)
  }

end AvroJsonBridgeSpec

object AvroJsonBridgeSpec:

  /** The branch that crosses formats. */
  final case class Click(url: String, ts: Long)

  /** The full object — decodable in principle (the positive control does), but the bridge must
    * never construct one.
    */
  final case class Envelope(id: String, click: Click, note: String)

  object BridgeFixtures:

    given AvroEncoder[Click] = AvroEncoder.derived
    given AvroDecoder[Click] = AvroDecoder.derived
    given AvroSchemaFor[Click] = AvroSchemaFor.derived

    given AvroEncoder[Envelope] = AvroEncoder.derived
    given AvroDecoder[Envelope] = AvroDecoder.derived
    given AvroSchemaFor[Envelope] = AvroSchemaFor.derived

    /** Every decode that would materialise a full [[Envelope]] lands here. */
    val rootDecodes: AtomicInteger = new AtomicInteger(0)

    private val derivedRoot: AvroCodec[Envelope] = AvroCodec.derived

    /** Counting wrapper around the derived root codec — local givens outrank the companion-scope
      * [[AvroCodec.derived]], so every summon of `AvroCodec[Envelope]` in this spec goes through
      * the counter.
      */
    given AvroCodec[Envelope] = new AvroCodec[Envelope]:
      def schema = derivedRoot.schema
      def encode(a: Envelope): Any = derivedRoot.encode(a)
      def decodeEither(any: Any): Either[Throwable, Envelope] =
        rootDecodes.incrementAndGet()
        derivedRoot.decodeEither(any)

    /** JSON codecs for BRANCHES only (the record branch and a scalar) — deliberately no codec for
      * any full output document exists in this spec.
      */
    given JsonValueCodec[Click] = JsonCodecMaker.make
    given JsonValueCodec[String] = JsonCodecMaker.make

    /** Fixture setup: serialise an [[Envelope]] to Avro binary. Uses the codec's encode side
      * (setup necessarily holds a full object; the claim is about the bridge, which doesn't).
      */
    def toAvroBinary(env: Envelope): Array[Byte] =
      val record = summon[AvroCodec[Envelope]].encode(env).asInstanceOf[IndexedRecord]
      val out = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[IndexedRecord](derivedRoot.schema)
      val encoder = EncoderFactory.get().binaryEncoder(out, null)
      writer.write(record, encoder)
      encoder.flush()
      out.toByteArray

  end BridgeFixtures

end AvroJsonBridgeSpec
