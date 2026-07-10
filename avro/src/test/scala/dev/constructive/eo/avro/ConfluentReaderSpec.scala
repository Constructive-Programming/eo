package dev.constructive.eo.avro

import cats.instances.either.given
import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** Versioned fixtures for the resolving reader (top-level so kindlings' `new T(...)` derivation
  * isn't tripped by a missing outer accessor):
  *   - `WriterEvent(id, legacy)` — the writer version,
  *   - `ReaderEvent(id)` — a reader that DROPS `legacy` (Avro resolution skips the extra writer
  *     field; no reader default needed → a reliable happy-path resolution),
  *   - `StrictEvent(id, mandatory)` — a reader that needs a field the writer lacks with no default
  *     (resolution must fail).
  */
final case class WriterEvent(id: String, legacy: Int)

object WriterEvent:
  given AvroEncoder[WriterEvent] = AvroEncoder.derived
  given AvroDecoder[WriterEvent] = AvroDecoder.derived
  given AvroSchemaFor[WriterEvent] = AvroSchemaFor.derived

final case class ReaderEvent(id: String)

object ReaderEvent:
  given AvroEncoder[ReaderEvent] = AvroEncoder.derived
  given AvroDecoder[ReaderEvent] = AvroDecoder.derived
  given AvroSchemaFor[ReaderEvent] = AvroSchemaFor.derived

final case class StrictEvent(id: String, mandatory: Int)

object StrictEvent:
  given AvroEncoder[StrictEvent] = AvroEncoder.derived
  given AvroDecoder[StrictEvent] = AvroDecoder.derived
  given AvroSchemaFor[StrictEvent] = AvroSchemaFor.derived

/** Field MOVED across versions: writer and reader declare the SAME field names in a DIFFERENT
  * order. Avro resolves record fields by name, so the reader must recover each value regardless of
  * position — the "a field moved" case.
  */
final case class ReorderWriter(alpha: String, beta: Int, gamma: Boolean)

object ReorderWriter:
  given AvroEncoder[ReorderWriter] = AvroEncoder.derived
  given AvroDecoder[ReorderWriter] = AvroDecoder.derived
  given AvroSchemaFor[ReorderWriter] = AvroSchemaFor.derived

final case class ReorderReader(gamma: Boolean, alpha: String, beta: Int)

object ReorderReader:
  given AvroEncoder[ReorderReader] = AvroEncoder.derived
  given AvroDecoder[ReorderReader] = AvroDecoder.derived
  given AvroSchemaFor[ReorderReader] = AvroSchemaFor.derived

/** Field TYPE CHANGED across versions: the writer stores `count` as an Avro `int`, the reader wants
  * a `long`. Avro's numeric-promotion resolution widens int → long — the "a field changed type"
  * case. `label` is carried unchanged alongside, to prove the promotion is per-field.
  */
final case class PromoteWriter(label: String, count: Int)

object PromoteWriter:
  given AvroEncoder[PromoteWriter] = AvroEncoder.derived
  given AvroDecoder[PromoteWriter] = AvroDecoder.derived
  given AvroSchemaFor[PromoteWriter] = AvroSchemaFor.derived

final case class PromoteReader(label: String, count: Long)

object PromoteReader:
  given AvroEncoder[PromoteReader] = AvroEncoder.derived
  given AvroDecoder[PromoteReader] = AvroDecoder.derived
  given AvroSchemaFor[PromoteReader] = AvroSchemaFor.derived

/** Behaviour spec for the no-hassle Confluent resolving reader — `ConfluentWire.reader` /
  * `recordReader` (effectful, raise-in-F) and the pure `resolving` Prism. Uses
  * `Either[Throwable, *]` as the test effect `F` (it has a `MonadThrow`), so no fs2 / cats-effect
  * is needed.
  */
class ConfluentReaderSpec extends Specification:

  private type Res[A] = Either[Throwable, A]

  private val writerSchema: Schema = summon[AvroCodec[WriterEvent]].schema
  private val readerSchema: Schema = summon[AvroCodec[ReaderEvent]].schema

  // Registry: id 1 → the WriterEvent schema; anything else → a lookup failure (raised in F).
  private val registry: Int => Res[Schema] =
    case 1  => Right(writerSchema)
    case id => Left(new NoSuchElementException(s"no schema for id $id"))

  private def framed(id: Int, e: WriterEvent): Array[Byte] =
    ConfluentWire.attach(
      id,
      AvroSpecFixtures.toBinaryValue(summon[AvroCodec[WriterEvent]].encode(e), writerSchema),
    )

  "reader: framed writer bytes resolve-decode to the reader type (drops the extra writer field)" >> {
    val read = ConfluentWire.reader[Res, ReaderEvent](registry)
    read(framed(1, WriterEvent("e-1", 99))) === Right(ReaderEvent("e-1"))
  }

  "recordReader: framed → IndexedRecord resolved under the caller-supplied reader schema" >> {
    val read = ConfluentWire.recordReader[Res](registry, readerSchema)
    read(framed(1, WriterEvent("e-2", 7))) match
      case Right(rec) => rec.get(readerSchema.getField("id").pos).toString === "e-2"
      case Left(t)    =>
        org.specs2.execute.Failure(s"expected Right(record), got $t"): org.specs2.execute.Result
  }

  // covers: strict frame contract — an unframed payload raises NotConfluentFramed rather than
  //   silently direct-decoding (which could accidentally succeed on corrupt bytes and yield
  //   garbage); the hook is never consulted. Mixed-topic callers opt into their own fallback by
  //   catching this and decoding directly (AvroCodec.decodeValue).
  "reader: unframed bytes raise NotConfluentFramed in F, hook never consulted" >> {
    val boom: Int => Res[Schema] = _ => Left(new AssertionError("hook must not be called"))
    val raw = AvroSpecFixtures.toBinaryValue(
      summon[AvroCodec[ReaderEvent]].encode(ReaderEvent("e-3")),
      readerSchema,
    )
    ConfluentWire.reader[Res, ReaderEvent](boom)(raw) match
      case Left(e: AvroFailureException) =>
        (e.failure match
          case AvroFailure.NotConfluentFramed(_) => true
          case _                                 => false
        ) === true
      case other =>
        org
          .specs2
          .execute
          .Failure(
            s"expected Left(AvroFailureException(NotConfluentFramed)), got $other"
          ): org.specs2.execute.Result
  }

  // covers: recordReader shares the strict frame contract
  "recordReader: unframed bytes raise NotConfluentFramed in F" >> {
    val boom: Int => Res[Schema] = _ => Left(new AssertionError("hook must not be called"))
    ConfluentWire.recordReader[Res](boom, readerSchema)(Array[Byte](1, 2, 3)) match
      case Left(e: AvroFailureException) =>
        (e.failure match
          case AvroFailure.NotConfluentFramed(_) => true
          case _                                 => false
        ) === true
      case other =>
        org
          .specs2
          .execute
          .Failure(
            s"expected Left(AvroFailureException(NotConfluentFramed)), got $other"
          ): org.specs2.execute.Result
  }

  "reader: incompatible reader (field absent from writer, no default) raises ResolveFailed in F" >> {
    val read = ConfluentWire.reader[Res, StrictEvent](registry)
    read(framed(1, WriterEvent("e-4", 1))) match
      case Left(e: AvroFailureException) =>
        (e.failure match
          case AvroFailure.ResolveFailed(_) => true
          case _                            => false
        ) === true
      case other =>
        org
          .specs2
          .execute
          .Failure(
            s"expected Left(AvroFailureException(ResolveFailed)), got $other"
          ): org.specs2.execute.Result
  }

  "reader: a schemaById lookup failure propagates as F's own error" >> {
    val read = ConfluentWire.reader[Res, ReaderEvent](registry)
    read(framed(2, WriterEvent("e-5", 0))) match // id 2 → registry Left
      case Left(_: NoSuchElementException) => true === true
      case other                           =>
        org
          .specs2
          .execute
          .Failure(s"expected Left(NoSuchElementException), got $other"): org.specs2.execute.Result
  }

  "resolving Prism: pure read (framed → A) + re-frame round-trip on a known writer schema" >> {
    val cf = ConfluentWire.resolving[ReaderEvent](writerSchema, frameId = 1)
    val frame = framed(1, WriterEvent("e-6", 5))
    val readOk = cf.getOption(frame) === Some(ReaderEvent("e-6"))
    // modify then the reader-schema re-frame decodes back as ReaderEvent
    val modified = cf.modify(r => ReaderEvent(r.id.toUpperCase))(frame)
    val writeOk = modified match
      case Right(bytes) =>
        ConfluentWire.reader[Res, ReaderEvent](_ => Right(readerSchema))(bytes) === Right(
          ReaderEvent("E-6")
        )
      case Left(f) =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($f)"): org.specs2.execute.Result
    readOk.and(writeOk)
  }

  "reader: fields MOVED (reordered writer→reader) resolve by name, not position" >> {
    val ws = summon[AvroCodec[ReorderWriter]].schema
    val reg: Int => Res[Schema] =
      case 7  => Right(ws)
      case id => Left(new NoSuchElementException(s"no schema for id $id"))
    val bytes = ConfluentWire.attach(
      7,
      AvroSpecFixtures.toBinaryValue(
        summon[AvroCodec[ReorderWriter]].encode(ReorderWriter("x", 42, true)),
        ws,
      ),
    )
    // gamma/alpha/beta land on the right reader fields despite the flipped declaration order.
    ConfluentWire.reader[Res, ReorderReader](reg)(bytes) === Right(ReorderReader(true, "x", 42))
  }

  "reader: a field whose TYPE CHANGED (int writer → long reader) is promoted, siblings intact" >> {
    val ws = summon[AvroCodec[PromoteWriter]].schema
    val reg: Int => Res[Schema] =
      case 7  => Right(ws)
      case id => Left(new NoSuchElementException(s"no schema for id $id"))
    val bytes = ConfluentWire.attach(
      7,
      AvroSpecFixtures.toBinaryValue(
        summon[AvroCodec[PromoteWriter]].encode(PromoteWriter("n", 42)),
        ws,
      ),
    )
    ConfluentWire.reader[Res, PromoteReader](reg)(bytes) === Right(PromoteReader("n", 42L))
  }

  "resolving Prism: read across a MOVED-field schema, modify, re-frame back to the reader shape" >> {
    val ws = summon[AvroCodec[ReorderWriter]].schema
    val rs = summon[AvroCodec[ReorderReader]].schema
    val cf = ConfluentWire.resolving[ReorderReader](ws, frameId = 7)
    val frame = ConfluentWire.attach(
      7,
      AvroSpecFixtures.toBinaryValue(
        summon[AvroCodec[ReorderWriter]].encode(ReorderWriter("x", 42, true)),
        ws,
      ),
    )
    val readOk = cf.getOption(frame) === Some(ReorderReader(true, "x", 42))
    val writeOk = cf.modify(r => ReorderReader(r.gamma, r.alpha.toUpperCase, r.beta))(frame) match
      case Right(bytes) =>
        ConfluentWire.reader[Res, ReorderReader](_ => Right(rs))(bytes) === Right(
          ReorderReader(true, "X", 42)
        )
      case Left(f) =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($f)"): org.specs2.execute.Result
    readOk.and(writeOk)
  }

  // ---- resolvingBytes (framed → reader-layout framed bytes, drift-translating) -----

  "resolvingBytes: a drifted-writer frame is TRANSLATED to reader-layout bytes, re-framed under frameId" >> {
    val ws = summon[AvroCodec[ReorderWriter]].schema
    val rs = summon[AvroCodec[ReorderReader]].schema
    val reg: ConfluentWire.SchemaById =
      case 7  => ws
      case id => throw new NoSuchElementException(s"no schema for id $id")
    val f = ConfluentWire.resolvingBytes(reg, rs, frameId = 1)
    val in = ConfluentWire.attach(
      7,
      AvroSpecFixtures.toBinaryValue(
        summon[AvroCodec[ReorderWriter]].encode(ReorderWriter("x", 42, true)),
        ws,
      ),
    )
    f(in) match
      case Right(out) =>
        val framedOk = ConfluentWire.strip(out) match
          case Right(ConfluentWire.Framed(1, _)) => true === true
          case other                             =>
            org
              .specs2
              .execute
              .Failure(s"expected reframe under id 1, got $other"): org.specs2.execute.Result
        // reader-layout bytes read back (under the reader schema) as the resolved reader value.
        val readBack = ConfluentWire.reader[Res, ReorderReader](_ => Right(rs))(out) === Right(
          ReorderReader(true, "x", 42)
        )
        framedOk.and(readBack)
      case Left(fail) =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(bytes), got Left($fail)"): org.specs2.execute.Result
  }

  "resolvingBytes: schemaById is consulted once per distinct writer id (bridge cached)" >> {
    val ws = summon[AvroCodec[ReorderWriter]].schema
    val rs = summon[AvroCodec[ReorderReader]].schema
    var calls = 0
    val counting: ConfluentWire.SchemaById = id =>
      calls += 1
      if id == 7 then ws else throw new NoSuchElementException(s"no schema for id $id")
    val f = ConfluentWire.resolvingBytes(counting, rs, frameId = 1)
    val in = ConfluentWire.attach(
      7,
      AvroSpecFixtures.toBinaryValue(
        summon[AvroCodec[ReorderWriter]].encode(ReorderWriter("x", 42, true)),
        ws,
      ),
    )
    val _ = f(in)
    val _ = f(in)
    val _ = f(in)
    calls === 1
  }

  "resolvingBytes: unframed / unresolvable / incompatible surface the specific AvroFailure" >> {
    val ws = summon[AvroCodec[WriterEvent]].schema
    val body =
      AvroSpecFixtures.toBinaryValue(summon[AvroCodec[WriterEvent]].encode(WriterEvent("e", 1)), ws)

    // Raw body, no header. A throwing hook proves the frame check precedes any resolution.
    val boom: ConfluentWire.SchemaById = _ => throw new AssertionError("hook must not be called")
    val unframed = ConfluentWire.resolvingBytes(boom, readerSchema, 1)(body) match
      case Left(AvroFailure.NotConfluentFramed(_)) => true === true
      case other                                   =>
        org
          .specs2
          .execute
          .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result

    // id 99 → the hook throws → SchemaResolutionFailed(99).
    val throwing: ConfluentWire.SchemaById =
      id => throw new NoSuchElementException(s"no schema for id $id")
    val unresolvable =
      ConfluentWire.resolvingBytes(throwing, readerSchema, 1)(ConfluentWire.attach(99, body)) match
        case Left(AvroFailure.SchemaResolutionFailed(99, _)) => true === true
        case other                                           =>
          org
            .specs2
            .execute
            .Failure(s"expected SchemaResolutionFailed(99), got $other"): org.specs2.execute.Result

    // Writer resolves, but the reader schema needs a field the writer lacks (no default) →
    // ResolveFailed, surfaced as a Left (the drift is genuinely unresolvable, not merely a gate).
    val strictSchema = summon[AvroCodec[StrictEvent]].schema
    val reg: ConfluentWire.SchemaById =
      case 1  => ws
      case id => throw new NoSuchElementException(s"no schema for id $id")
    val incompatible =
      ConfluentWire.resolvingBytes(reg, strictSchema, 1)(ConfluentWire.attach(1, body)) match
        case Left(AvroFailure.ResolveFailed(_)) => true === true
        case other                              =>
          org
            .specs2
            .execute
            .Failure(s"expected ResolveFailed, got $other"): org.specs2.execute.Result

    unframed.and(unresolvable).and(incompatible)
  }

end ConfluentReaderSpec
