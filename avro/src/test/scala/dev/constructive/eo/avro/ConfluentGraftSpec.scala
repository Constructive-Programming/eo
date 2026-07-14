package dev.constructive.eo.avro

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.util.concurrent.atomic.AtomicInteger
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** Produce-side graft fixtures (top-level so kindlings' `new T(...)` derivation isn't tripped by a
  * missing outer accessor):
  *   - `Click(id, ts)` — the branch the prism focuses (reader shape);
  *   - `ClickWide(id, ts, extra)` — a COMPATIBLE-but-NOT-identical writer: adds an optional field,
  *     so its parsing-canonical-form fingerprint differs (gate must refuse it) yet it resolves
  *     writer → `Click` by dropping the extra field (translate must absorb it);
  *   - `Conversion(cid, click)` — the parent carrying `click` as `union<null, Click>`.
  */
final case class Click(id: String, ts: Long)

object Click:
  given AvroEncoder[Click] = AvroEncoder.derived
  given AvroDecoder[Click] = AvroDecoder.derived
  given AvroSchemaFor[Click] = AvroSchemaFor.derived

final case class ClickWide(id: String, ts: Long, extra: Option[String])

object ClickWide:
  given AvroEncoder[ClickWide] = AvroEncoder.derived
  given AvroDecoder[ClickWide] = AvroDecoder.derived
  given AvroSchemaFor[ClickWide] = AvroSchemaFor.derived

final case class Conversion(cid: String, click: Option[Click])

object Conversion:
  given AvroEncoder[Conversion] = AvroEncoder.derived
  given AvroDecoder[Conversion] = AvroDecoder.derived
  given AvroSchemaFor[Conversion] = AvroSchemaFor.derived

/** Behaviour spec for the produce-side gated / resolving graft — `ConfluentWire.graftGated` /
  * `graftResolving`. Pins the load-bearing safety property: a compatible-but-NOT-identical fragment
  * writer schema is refused into the fallback by the gate and translated (never byte-grafted) by
  * the resolving form; a byte-verbatim graft of that same drifted fragment would produce different
  * (silently wrong) bytes.
  */
class ConfluentGraftSpec extends Specification:

  private val clickSchema: Schema = summon[AvroCodec[Click]].schema
  private val clickWideSchema: Schema = summon[AvroCodec[ClickWide]].schema

  // Registry: id 1 → the identical Click writer schema; id 2 → the compatible-but-wider writer;
  // anything else → a lookup failure (the hook throws).
  private val registry: ConfluentWire.SchemaById =
    case 1  => clickSchema
    case 2  => clickWideSchema
    case id => throw new NoSuchElementException(s"no schema for id $id")

  // Focus the Click branch of the optional field — composed from plain Scala types, no byte math.
  private val branch: AvroPrism[Click] =
    codecPrism[Conversion].field(_.click).union[Click]

  private def enc[A](a: A)(using AvroCodec[A]): Array[Byte] =
    AvroCodec.encodeValue(a).fold(f => sys.error(f.message), identity)

  private def frame(id: Int, body: Array[Byte]): Array[Byte] = ConfluentWire.attach(id, body)

  private def unexpected(x: Any): org.specs2.execute.Result =
    org.specs2.execute.Failure(s"unexpected outcome: $x")

  private val parent: Array[Byte] = enc(Conversion("c", Some(Click("x", 1L))))

  "graftGated: an identical-schema fragment grafts verbatim, byte-equal to a direct encode" >> {
    val fragment = frame(1, enc(Click("y", 2L)))
    val expected = enc(Conversion("c", Some(Click("y", 2L))))
    ConfluentWire.graftGated(branch, registry)(parent, fragment).map(_.toList) ===
      Right(expected.toList)
  }

  "graftResolving: an identical-schema fragment also grafts verbatim (no re-encode path)" >> {
    val fragment = frame(1, enc(Click("y", 2L)))
    val expected = enc(Conversion("c", Some(Click("y", 2L))))
    ConfluentWire.graftResolving(branch, registry)(parent, fragment).map(_.toList) ===
      Right(expected.toList)
  }

  "graftGated: a compatible-but-NOT-identical fragment is REFUSED with SchemaMismatch, never grafted" >> {
    val fragment = frame(2, enc(ClickWide("y", 2L, Some("k"))))
    ConfluentWire.graftGated(branch, registry)(parent, fragment) match
      case Left(m: AvroFailure.SchemaMismatch) => m.schemaId === 2
      case other                               => unexpected(other)
  }

  // THE load-bearing case: the drifted fragment resolves writer → Click (dropping `extra`) and
  // re-encodes, so graftResolving yields the SAME bytes as directly encoding the resolved value —
  // whereas a raw byte-verbatim graft of the same fragment produces DIFFERENT (garbage) bytes.
  "graftResolving: a compatible-but-NOT-identical fragment is translated to correct bytes, not byte-grafted" >> {
    val wideBody = enc(ClickWide("y", 2L, Some("k")))
    val fragment = frame(2, wideBody)
    val expected = enc(Conversion("c", Some(Click("y", 2L))))
    val resolved = ConfluentWire.graftResolving(branch, registry)(parent, fragment).map(_.toList)
    val rawVerbatim = branch.graftBytes(parent, wideBody).map(_.toList)
    (resolved === Right(expected.toList)).and(rawVerbatim !== Right(expected.toList))
  }

  "graftGated: an unresolvable frame id fails with SchemaResolutionFailed and is NOT cached (hook re-consulted)" >> {
    val lookups = new AtomicInteger(0)
    val reg: ConfluentWire.SchemaById =
      case 1  => clickSchema
      case id =>
        lookups.incrementAndGet()
        throw new NoSuchElementException(s"no schema for id $id")
    val graft = ConfluentWire.graftGated(branch, reg)
    val fragment = frame(99, enc(Click("y", 2L)))
    (graft(parent, fragment), graft(parent, fragment), lookups.get) match
      case (
            Left(_: AvroFailure.SchemaResolutionFailed),
            Left(_: AvroFailure.SchemaResolutionFailed),
            2,
          ) =>
        success
      case other => unexpected(other)
  }

  "graftGated / graftResolving: a non-Confluent-framed fragment fails with NotConfluentFramed" >> {
    val garbage = Array[Byte](1, 2, 3)
    (
      ConfluentWire.graftGated(branch, registry)(parent, garbage),
      ConfluentWire.graftResolving(branch, registry)(parent, garbage),
    ) match
      case (Left(_: AvroFailure.NotConfluentFramed), Left(_: AvroFailure.NotConfluentFramed)) =>
        success
      case other => unexpected(other)
  }

  "graftResolving: grafting onto a null branch SWITCHES the union to Click" >> {
    val parentNone = enc(Conversion("c", None))
    val fragment = frame(1, enc(Click("y", 2L)))
    val expected = enc(Conversion("c", Some(Click("y", 2L))))
    ConfluentWire.graftResolving(branch, registry)(parentNone, fragment).map(_.toList) ===
      Right(expected.toList)
  }

end ConfluentGraftSpec
