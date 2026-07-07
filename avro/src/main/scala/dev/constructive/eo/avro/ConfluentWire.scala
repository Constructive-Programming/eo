package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.MonadThrow
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{Optic, Optional, PickMendPrism, Prism}
import dev.constructive.eo.{widenLeft, widenRight}
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.{Schema, SchemaNormalization}

/** Confluent Schema Registry wire-format framing for binary Avro payloads: a 5-byte header — magic
  * byte `0x00` followed by the big-endian 4-byte schema id — then the Avro binary body.
  *
  * Registry-agnostic by design: no registry client, no new dependencies. [[strip]] / [[attach]]
  * only move the frame; resolving a schema id to a [[Schema]] is the caller's job, for which the
  * [[SchemaById]] alias is the hook (plug in a registry client, a static map, a cache — whatever
  * the deployment owns).
  *
  * Two layers sit on top of that framing, both decode-agnostic — they hand you the header-stripped,
  * writer-resolved, fingerprint-gated BODY BYTES and let you decode however you like (kindlings,
  * vulcan, a generic-record → JSON walk, …):
  *
  *   - [[resolve]] — the one-shot primitive: `Array[Byte] => Either[AvroFailure, Array[Byte]]`.
  *   - [[confluent]] — the same as a composable eo [[Prism]] `Array[Byte] (framed) ↔ Array[Byte]
  *     (body)`, so you drop it BEFORE any byte optic:
  *     `confluent(…).andThen(codecPrism[A].field(…))`.
  */
object ConfluentWire:

  /** Resolve a Confluent schema id to the writer [[Schema]] it names. Synchronous by design: the
    * caller owns the registry client, the cache, and any effect wrapping around it, and hands
    * [[resolve]] / [[confluent]] an already-resolved lookup. May throw (a registry miss, a network
    * error); [[resolve]] catches it into `AvroFailure.SchemaResolutionFailed`.
    */
  type SchemaById = Int => Schema

  /** The Confluent magic byte — always `0x00` in the current wire format. */
  val Magic: Byte = 0x00

  /** Header length: 1 magic byte + 4 big-endian schema-id bytes. */
  val HeaderLength: Int = 5

  /** A stripped Confluent frame: the schema id and the Avro binary body.
    *
    * `body` is a COPY of the payload bytes, not a zero-copy offset view: `Array[Byte]` cannot carry
    * an offset, and every downstream consumer in this module ([[AvroPrism]]'s dual-input surface,
    * `sliceBytes` / `graftBytes`) takes whole arrays. The copy is one `arraycopy` of `length - 5`
    * bytes — noise next to any decode that follows. A true offset view is a concern for the
    * deferred byte-native optic surface.
    */
  final case class Framed(schemaId: Int, body: Array[Byte])

  /** Validate and strip the 5-byte Confluent header. Fails structurally
    * ([[AvroFailure.NotConfluentFramed]]) on inputs shorter than the header or whose magic byte
    * isn't `0x00`.
    */
  def strip(bytes: Array[Byte]): Either[AvroFailure, Framed] =
    if bytes.length < HeaderLength then
      Left(
        AvroFailure.NotConfluentFramed(
          s"payload is ${bytes.length} bytes, shorter than the $HeaderLength-byte header"
        )
      )
    else if bytes(0) != Magic then
      Left(AvroFailure.NotConfluentFramed(s"magic byte is 0x${"%02x".format(bytes(0))}, not 0x00"))
    else
      val schemaId =
        ((bytes(1) & 0xff) << 24) |
          ((bytes(2) & 0xff) << 16) |
          ((bytes(3) & 0xff) << 8) |
          (bytes(4) & 0xff)
      val body = new Array[Byte](bytes.length - HeaderLength)
      System.arraycopy(bytes, HeaderLength, body, 0, body.length)
      Right(Framed(schemaId, body))

  /** Frame an Avro binary body under `schemaId` — the inverse of [[strip]]:
    * `strip(attach(id, body)) == Right(Framed(id, body))` for every id / body.
    */
  def attach(schemaId: Int, body: Array[Byte]): Array[Byte] =
    val out = new Array[Byte](HeaderLength + body.length)
    out(0) = Magic
    out(1) = ((schemaId >>> 24) & 0xff).toByte
    out(2) = ((schemaId >>> 16) & 0xff).toByte
    out(3) = ((schemaId >>> 8) & 0xff).toByte
    out(4) = (schemaId & 0xff).toByte
    System.arraycopy(body, 0, out, HeaderLength, body.length)
    out

  /** Strip + resolve + fingerprint-gate a Confluent-framed payload down to its BODY BYTES, without
    * decoding to any type — the seam issue #41 asked for. Steps:
    *
    *   1. [[strip]] the 5-byte header → `(schemaId, body)`;
    *   2. resolve the writer schema for `schemaId` via `schemaById`;
    *   3. gate by Avro parsing-canonical-form fingerprint
    *      ([[org.apache.avro.SchemaNormalization.parsingFingerprint64]]): when the writer
    *      fingerprint equals the reader's, the body is byte-identical under both schemas — return
    *      it as-is; else refuse with [[AvroFailure.SchemaMismatch]] rather than hand back bytes
    *      that would misdecode.
    *
    * The returned bytes are the caller's to decode with whatever they own — kindlings
    * (`codecPrism[A]`), vulcan `Codec.fromBinary`, a generic-record → JSON walk, anything.
    * Failures: [[AvroFailure.NotConfluentFramed]] (bad frame),
    * [[AvroFailure.SchemaResolutionFailed]] (the hook threw), [[AvroFailure.SchemaMismatch]]
    * (writer ≠ reader fingerprint). Exactly one failure point per payload, so `Either`, not the
    * `Ior[Chain, …]` the multi-failure walk surfaces.
    */
  def resolve(
      framed: Array[Byte],
      schemaById: SchemaById,
      readerSchema: Schema,
  ): Either[AvroFailure, Array[Byte]] =
    resolveWith(framed, schemaById, SchemaNormalization.parsingFingerprint64(readerSchema))

  /** [[resolve]] with a precomputed reader fingerprint — the hot-path form [[confluent]] closes
    * over so `parsingFingerprint64(readerSchema)` runs once at construction, not per payload.
    */
  private def resolveWith(
      framed: Array[Byte],
      schemaById: SchemaById,
      readerFingerprint: Long,
  ): Either[AvroFailure, Array[Byte]] =
    strip(framed) match
      case l @ Left(_)  => l.widenRight
      case Right(frame) =>
        val writer =
          try Right(schemaById(frame.schemaId))
          catch case NonFatal(t) => Left(AvroFailure.SchemaResolutionFailed(frame.schemaId, t))
        writer match
          case l @ Left(_) => l.widenRight
          case Right(w)    =>
            val writerFingerprint = SchemaNormalization.parsingFingerprint64(w)
            if writerFingerprint == readerFingerprint then Right(frame.body)
            else
              Left(AvroFailure.SchemaMismatch(frame.schemaId, writerFingerprint, readerFingerprint))

  /** The [[resolve]] strip + resolve + fingerprint-gate as a composable eo [[Prism]] over bytes:
    * `Array[Byte]` (a full Confluent frame) ↔ `Array[Byte]` (the byte-exact resolved body). Drop it
    * BEFORE any byte optic and let the normal walk run on the body:
    *
    * {{{
    *   val cf = ConfluentWire.confluent(schemaById, readerSchema, frameId)
    *   // typed (kindlings):
    *   cf.andThen(codecPrism[A].field(_.x)).getOption(framedBytes)                 // Option[X]
    *   // decode-agnostic: hand the resolved body to vulcan / a generic-record walk:
    *   cf.getOption(framedBytes)                                                   // Option[Array[Byte]]
    * }}}
    *
    *   - `getOption(framed)` runs strip + resolve + fingerprint-gate; a bad frame, an unresolvable
    *     id, or a fingerprint mismatch all yield `None`. Use [[resolve]] when you want the specific
    *     [[AvroFailure]] reason instead of `None`.
    *   - `reverseGet(body)` re-frames via [[attach]] under `frameId` — the schema id to publish
    *     under (typically the reader schema's registry id). Only exercised on write-back (`modify`
    *     / `replace`); a read-only pipeline never calls it. A monomorphic byte Prism can't thread
    *     the original per-message id from read to write, so re-framing uses this fixed id; if you
    *     must preserve the incoming id on write, use [[resolve]] + [[attach]] by hand.
    *
    * The reader fingerprint is computed once here, not per payload.
    */
  def confluent(
      schemaById: SchemaById,
      readerSchema: Schema,
      frameId: Int,
  ): PickMendPrism[Array[Byte], Array[Byte], Array[Byte]] =
    val readerFingerprint = SchemaNormalization.parsingFingerprint64(readerSchema)
    Prism.optional[Array[Byte], Array[Byte]](
      getOption = framed => resolveWith(framed, schemaById, readerFingerprint).toOption,
      reverseGet = body => attach(frameId, body),
    )

  // ---- Resolving surface (read→write translation, not a gate) --------
  //
  // Unlike `confluent` / `resolve` (which byte-exact GATE and refuse on drift), these actually
  // TRANSLATE the drift via `AvroCodec.decodeResolved*` (Avro's `ResolvingDecoder`). Named from the
  // optic's view: `readSchema` = the schema the bytes were read in (Avro's writer schema),
  // `writeSchema` = the shape they resolve into / are written under (Avro's reader schema).

  /** A pure, read+write Confluent optic for a KNOWN read schema. `to` strips the header and
    * resolve-decodes the body from `readSchema` into `A` (the write schema =
    * `AvroCodec[A].schema`); `from` re-encodes `A` and re-frames under `frameId`. `Affine`-carried
    * with the same `T = Either[AvroFailure, Array[Byte]]` fallible-build shape as [[AvroBridge]].
    * Per-read-schema — for a single-schema topic or a producer; for a mixed-schema stream use
    * [[reader]], which looks the read schema up per message.
    */
  def resolving[A](readSchema: Schema, frameId: Int)(using
      codec: AvroCodec[A]
  ): Optic[Array[Byte], AvroBridge.BridgedBytes, A, A, Affine] =
    new Optional[Array[Byte], AvroBridge.BridgedBytes, A, A](
      getOrModify = framed =>
        strip(framed).flatMap(f => AvroCodec.decodeResolvedValue[A](f.body, readSchema)) match
          case r @ Right(_) => r.widenLeft
          case Left(fail)   => Left(Left(fail)),
      reverseGet = (_, a) => AvroCodec.encodeValue[A](a).map(attach(frameId, _)),
    )

  /** Generic counterpart of [[resolving]] — resolves `readSchema` → the caller-supplied
    * `writeSchema` into an [[IndexedRecord]] (no reader codec / case class).
    */
  def resolvingRecord(
      readSchema: Schema,
      writeSchema: Schema,
      frameId: Int,
  ): Optic[Array[Byte], AvroBridge.BridgedBytes, IndexedRecord, IndexedRecord, Affine] =
    new Optional[Array[Byte], AvroBridge.BridgedBytes, IndexedRecord, IndexedRecord](
      getOrModify = framed =>
        strip(framed).flatMap(f =>
          AvroCodec.decodeResolvedRecord(f.body, readSchema, writeSchema)
        ) match
          case r @ Right(_) => r.widenLeft
          case Left(fail)   => Left(Left(fail)),
      reverseGet = (_, r) => AvroCodec.encodeRecord(r, writeSchema).map(attach(frameId, _)),
    )

  /** The no-hassle Confluent reader: `Array[Byte] => F[A]`, ready for an fs2 `Stream.evalMap`. Per
    * message it auto-detects the header — a framed payload has its read schema (the schema the
    * bytes were written under) looked up by id (`schemaById`, effectful) and is resolve-decoded
    * into `A` (the write schema = the codec's own schema); an unframed payload is decoded directly
    * under the codec's schema. Failures (`ResolveFailed` / `DecodeFailed` / `BinaryParseFailed`, or
    * a `schemaById` failure) are raised in `F` — the former via [[AvroFailureException]], the
    * latter as the effect's own error.
    */
  def reader[F[_], A](schemaById: Int => F[Schema])(using
      F: MonadThrow[F],
      codec: AvroCodec[A],
  ): Array[Byte] => F[A] =
    framed =>
      strip(framed) match
        case Right(frame) =>
          F.flatMap(schemaById(frame.schemaId)) { readSchema =>
            raiseFailure(AvroCodec.decodeResolvedValue[A](frame.body, readSchema))
          }
        case Left(_) => raiseFailure(AvroCodec.decodeValue[A](framed))

  /** Generic counterpart of [[reader]] — yields `Array[Byte] => F[IndexedRecord]` resolved into the
    * caller-supplied `writeSchema`, for when the reader's case class is unavailable.
    */
  def recordReader[F[_]](schemaById: Int => F[Schema], writeSchema: Schema)(using
      F: MonadThrow[F]
  ): Array[Byte] => F[IndexedRecord] =
    framed =>
      strip(framed) match
        case Right(frame) =>
          F.flatMap(schemaById(frame.schemaId)) { readSchema =>
            raiseFailure(AvroCodec.decodeResolvedRecord(frame.body, readSchema, writeSchema))
          }
        case Left(_) => raiseFailure(AvroCodec.decodeRecord(framed, writeSchema))

  /** Lift an `Either[AvroFailure, A]` into `F`, raising a `Left` as an [[AvroFailureException]]. */
  private def raiseFailure[F[_], A](e: Either[AvroFailure, A])(using F: MonadThrow[F]): F[A] =
    F.fromEither(e.left.map(AvroFailureException(_)))

end ConfluentWire
