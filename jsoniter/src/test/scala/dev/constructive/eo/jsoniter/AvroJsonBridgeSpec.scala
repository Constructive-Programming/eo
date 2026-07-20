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

/** Cross-format bridge, BOTH directions between `Array[Byte]` (Avro binary) and `Array[Byte]`
  * (JSON), combining [[dev.constructive.eo.avro.AvroPrism]] and [[JsoniterPrism]] — each side reads
  * by locating the branch's span and decoding only that slice, and writes by encoding the branch
  * and splicing it into an existing payload of its format.
  *
  * The load-bearing claim: '''the full object is never constructed — only the focused branches
  * are.''' Proven two ways:
  *
  *   - '''Dynamically''': the root `AvroCodec[Envelope]` in scope is a counting wrapper — its
  *     decode side increments [[BridgeFixtures.rootDecodes]], its encode side
  *     [[BridgeFixtures.rootEncodes]]. Both directions run with both counters at ZERO; the record
  *     face (`.record.get`) as a positive control drives the decode counter up, proving the
  *     counters are wired to the only paths that could materialise an `Envelope`.
  *   - '''Statically''': no `JsonValueCodec` for any full JSON document exists anywhere in this
  *     spec — only branch codecs. A full JSON-side object CANNOT be constructed because its codec
  *     was never derived.
  *
  * Precision note: "constructed" here means the TYPED root value (`Envelope`) and the whole-
  * document JSON object. A record-SHAPED branch (like `Click`) is materialised as that branch's own
  * generic record during its slice decode — the claim is that nothing root-sized ever is, which for
  * the `.field`-drilled paths exercised here also holds at the generic-record level (the byte walk
  * skips, it never parses the root).
  */
class AvroJsonBridgeSpec extends Specification:

  // The construction-counting fixtures are shared mutable state; examples must not interleave.
  sequential

  import AvroJsonBridgeSpec.*
  import BridgeFixtures.given

  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  "Avro bytes → JSON bytes: branch moves across formats with zero root constructions" >> {
    val env = Envelope("env-7", Click("https://x.example/a?b=1", 42L), "keep-me")
    val avroBytes = BridgeFixtures.toAvroBinary(env)

    // The two byte optics, one per format. Drilled once, reusable across payloads.
    val clickAvro = codecPrism[Envelope].field(_.click)
    val clickJson = JsoniterPrism.fromPath[Click]("$.click")

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
    val idJson = JsoniterPrism.fromPath[String]("$.envelopeId")
    val template: Array[Byte] = """{"envelopeId":"","v":1}""".getBytes("UTF-8")

    BridgeFixtures.rootDecodes.set(0)
    val out = idAvro.getOption(avroBytes) match
      case Some(id) => idJson.replace(id)(template)
      case None     => template

    (str(out) === """{"envelopeId":"env-9","v":1}""")
      .and(BridgeFixtures.rootDecodes.get === 0)
  }

  "JSON bytes → Avro bytes: reverse bridge, zero root decodes AND zero root encodes" >> {
    // The Avro payload the branch is spliced INTO — built at setup (the claim is about the
    // bridge, not the fixture), then both counters reset.
    val templateEnv = Envelope("env-tmpl", Click("placeholder", 0L), "static-note")
    val avroTemplate = BridgeFixtures.toAvroBinary(templateEnv)
    val jsonBytes: Array[Byte] =
      """{"click":{"url":"https://json.example/in","ts":77},"meta":"x"}""".getBytes("UTF-8")

    val clickJson = JsoniterPrism.fromPath[Click]("$.click")
    val clickAvro = codecPrism[Envelope].field(_.click)

    BridgeFixtures.rootDecodes.set(0)
    BridgeFixtures.rootEncodes.set(0)

    // ---- the reverse bridge: JSON slice-decode → Avro splice-encode ----
    val outAvro: Array[Byte] = clickJson.getOption(jsonBytes) match
      case Some(click) => clickAvro.replace(click)(avroTemplate)
      case None        => avroTemplate

    // Neither side materialised an Envelope: JSON decoded ONLY the click slice, the Avro write
    // encoded ONLY the click branch and spliced it into the wire bytes.
    val neverConstructedOk = (BridgeFixtures.rootDecodes.get === 0)
      .and(BridgeFixtures.rootEncodes.get === 0)

    // Branch-level read-back of the spliced payload — still no root construction.
    val branchOk = clickAvro.getOption(outAvro) === Some(Click("https://json.example/in", 77L))
    val stillZeroOk = BridgeFixtures.rootDecodes.get === 0

    // Positive control: the record face decodes the WHOLE envelope — siblings survived the
    // splice, and the decode counter finally moves.
    val roundTrip = codecPrism[Envelope].record.get(outAvro)
    val controlOk = (roundTrip.toOption ===
      Some(templateEnv.copy(click = Click("https://json.example/in", 77L))))
      .and(BridgeFixtures.rootDecodes.get === 1)

    neverConstructedOk.and(branchOk).and(stillZeroOk).and(controlOk)
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

    /** Every encode that would require a full [[Envelope]] value lands here. */
    val rootEncodes: AtomicInteger = new AtomicInteger(0)

    private val derivedRoot: AvroCodec[Envelope] = AvroCodec.derived

    /** Counting wrapper around the derived root codec — local givens outrank the companion-scope
      * [[AvroCodec.derived]], so every summon of `AvroCodec[Envelope]` in this spec goes through
      * the counters.
      */
    given AvroCodec[Envelope] = new AvroCodec[Envelope]:
      def schema = derivedRoot.schema
      def encode(a: Envelope): Any =
        rootEncodes.incrementAndGet()
        derivedRoot.encode(a)
      def decodeEither(any: Any): Either[Throwable, Envelope] =
        rootDecodes.incrementAndGet()
        derivedRoot.decodeEither(any)

    /** JSON codecs for BRANCHES only (the record branch and a scalar) — deliberately no codec for
      * any full output document exists in this spec.
      */
    given JsonValueCodec[Click] = JsonCodecMaker.make
    given JsonValueCodec[String] = JsonCodecMaker.make

    /** Fixture setup: serialise an [[Envelope]] to Avro binary. Uses the codec's encode side (setup
      * necessarily holds a full object; the claim is about the bridge, which doesn't).
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
