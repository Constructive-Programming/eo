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

  "reader: unframed bytes decode under the codec's own schema, hook never consulted" >> {
    val boom: Int => Res[Schema] = _ => Left(new AssertionError("hook must not be called"))
    val raw = AvroSpecFixtures.toBinaryValue(
      summon[AvroCodec[ReaderEvent]].encode(ReaderEvent("e-3")),
      readerSchema,
    )
    ConfluentWire.reader[Res, ReaderEvent](boom)(raw) === Right(ReaderEvent("e-3"))
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

end ConfluentReaderSpec
