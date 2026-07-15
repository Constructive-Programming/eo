package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.MonadThrow
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{Optic, Optional, PickMendPrism, Prism}
import dev.constructive.eo.{widenLeft, widenRight}
import java.util.concurrent.ConcurrentHashMap
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.{Schema, SchemaNormalization}

/** The Confluent Schema Registry wire format for binary Avro payloads — a 5-byte header (magic byte
  * `0x00`, then the writer schema's registry id as a big-endian 4-byte int) in front of the plain
  * Avro binary body — and the consumer surface built on it. This is the framing every
  * Confluent-serialized Kafka topic carries; anything reading such a topic outside Confluent's own
  * serializer stack needs exactly what lives here.
  *
  * '''Registry-agnostic by design.''' No registry client, no new dependency: every member that
  * needs a schema looked up takes the [[SchemaById]] hook (or its effectful `Int => F[Schema]`
  * sibling). Plug in a registry client, a static map, a cache — the deployment owns it. All
  * failures are values from the [[AvroFailure]] taxonomy (or an [[AvroFailureException]] raised in
  * the caller's `F`), never a bare NPE or an Avro internal exception.
  *
  * The surface splits along two axes.
  *
  * '''Axis 1 — drift policy.''' What happens when the writer schema named by the frame's id differs
  * from the schema the consumer reads with:
  *   - '''Gate''' ([[resolve]], [[confluent]]): compare parsing-canonical-form fingerprints and
  *     REFUSE any difference ([[AvroFailure.SchemaMismatch]]). The body bytes pass through
  *     untouched — byte-exact, zero re-encode. For payloads that must stay verbatim (hashing,
  *     signatures, archiving, pass-through forwarding), and for consumers where drift is a bug to
  *     surface, not a condition to absorb.
  *   - '''Translate''' ([[resolving]], [[resolvingBytes]], [[reader]], [[recordReader]]): run Avro
  *     schema resolution (`ResolvingDecoder`) writer → reader, so compatible evolution — added
  *     fields with defaults, reordered fields, promotions — is absorbed instead of refused.
  *
  * '''Axis 2 — output altitude.''' Each translating member hands back a different currency:
  *   - typed `A` (via [[AvroCodec]]): [[resolving]] for a KNOWN writer schema (pure optic),
  *     [[reader]] for a per-message lookup (effectful, `Stream.evalMap`-ready);
  *   - generic `IndexedRecord` (no case class needed): [[recordReader]] (per-message lookup; for a
  *     known writer schema, `AvroCodec.decodeResolvedRecord` after [[strip]] is the two-liner);
  *   - reader-layout framed '''bytes''' (no decode at all): [[resolvingBytes]] — per-message
  *     lookup, writer-id-cached, for consumers that hash / slice / forward rather than deserialize.
  *
  * '''Produce side.''' [[graftGated]] / [[graftResolving]] are the write twins of the two drift
  * policies: gate-or-translate a stored, framed fragment into the focus of an eo prism, so a
  * producer can splice a verbatim Confluent-framed sub-record into an outgoing encode without
  * decoding it — same fingerprint criterion, refused or absorbed.
  *
  * Underneath sit the pure frame primitives [[strip]] / [[attach]] (header handling only, no schema
  * involvement) — the building blocks for anything the members above don't cover.
  */
object ConfluentWire:

  // ---- Frame primitives (header only, no schema involvement) ---------

  /** Resolve a Confluent schema id to the writer `Schema` it names. Synchronous by design: the
    * caller owns the registry client, the cache, and any effect wrapping, and hands this surface an
    * already-synchronous lookup. May throw (a registry miss, a network error) — callers here catch
    * that into [[AvroFailure.SchemaResolutionFailed]].
    */
  type SchemaById = Int => Schema

  /** The Confluent magic byte — always `0x00` in the current wire format. */
  val Magic: Byte = 0x00

  /** Header length: 1 magic byte + 4 big-endian schema-id bytes. */
  val HeaderLength: Int = 5

  /** A stripped Confluent frame: the schema id and the Avro binary body.
    *
    * `body` is a COPY of the payload bytes, not a zero-copy offset view — `Array[Byte]` cannot
    * carry an offset, and every downstream consumer takes whole arrays. The copy is one `arraycopy`
    * of `length - 5` bytes: noise next to any decode that follows.
    */
  final case class Framed(schemaId: Int, body: Array[Byte])

  /** Validate and strip the 5-byte Confluent header. Fails structurally
    * ([[AvroFailure.NotConfluentFramed]]) on a `null` payload (a Kafka tombstone / mis-produced
    * record — a defined failure rather than an NPE), on inputs shorter than the header, or whose
    * magic byte isn't `0x00`.
    */
  def strip(bytes: Array[Byte] | Null): Either[AvroFailure, Framed] =
    if bytes == null then
      Left(AvroFailure.NotConfluentFramed("payload is null (e.g. a Kafka tombstone value)"))
    else if bytes.length < HeaderLength then
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

  // ---- Gate surface (byte-exact: refuse drift, never re-encode) ------

  /** Strip + resolve + fingerprint-gate a Confluent-framed payload down to its BODY BYTES, without
    * decoding. Per payload:
    *
    *   1. [[strip]] the 5-byte header → `(schemaId, body)`;
    *   2. resolve the writer schema for `schemaId` via `schemaById`;
    *   3. gate by parsing-canonical-form fingerprint
    *      (`org.apache.avro.SchemaNormalization.parsingFingerprint64`): when the writer fingerprint
    *      equals the reader's, the body is byte-identical under both schemas — return it as-is;
    *      else refuse with [[AvroFailure.SchemaMismatch]] rather than hand back bytes that would
    *      misdecode.
    *
    * The returned bytes are the caller's to decode (or hash, or forward) with whatever they own.
    * Failures: [[AvroFailure.NotConfluentFramed]] (bad frame),
    * [[AvroFailure.SchemaResolutionFailed]] (the hook threw), [[AvroFailure.SchemaMismatch]]
    * (writer ≠ reader fingerprint). To ABSORB compatible drift instead of refusing it, use the
    * translating surface below.
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

  /** [[resolve]] as a composable eo [[dev.constructive.eo.optics.Prism]] over bytes: `Array[Byte]`
    * (a full Confluent frame) ↔ `Array[Byte]` (the byte-exact gated body). Drop it BEFORE any byte
    * optic and compose:
    *
    * {{{
    *   val cf = ConfluentWire.confluent(schemaById, readerSchema, frameId)
    *   cf.andThen(codecPrism[A].field(_.x)).getOption(framedBytes)   // Option[X]
    *   cf.getOption(framedBytes)                                     // Option[Array[Byte]]
    * }}}
    *
    *   - `getOption(framed)` runs strip + resolve + fingerprint-gate; a bad frame, an unresolvable
    *     id, or a fingerprint mismatch all yield `None`. Use [[resolve]] when you need the specific
    *     [[AvroFailure]] instead of `None`.
    *   - `reverseGet(body)` re-frames via [[attach]] under `frameId` — the schema id to publish
    *     under (typically the reader schema's registry id). Only exercised on write-back (`modify`
    *     / `replace`). A monomorphic byte Prism can't thread the original per-message id from read
    *     to write, so re-framing uses this fixed id; to preserve the incoming id, use [[resolve]] +
    *     [[attach]] by hand.
    *
    * The reader fingerprint is computed once at construction, not per payload.
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

  // ---- Translating surface (absorb drift via Avro schema resolution) --
  //
  // Everything below runs `ResolvingDecoder` writer → reader (via `AvroCodec.decodeResolved*`)
  // instead of gating. Parameter naming on the pure optics follows the OPTIC's view, which is
  // the reverse of Avro's: `readSchema` = the schema the bytes were written under (Avro's
  // "writer"), `writeSchema` = the shape they resolve into and are re-encoded under (Avro's
  // "reader"). The per-message members use Avro's names (`readerSchema`) since no optic is
  // involved.

  /** The translating surface's decode — [[AvroBinaryCursor.readDatum]] writer → reader under the
    * constructor's captured `threadLocalStorage` field, failures as [[AvroFailure.ResolveFailed]].
    * (The always-cached public counterpart is `AvroCodec.decodeResolvedRecord`.)
    */
  private def resolveDecodeRecord(
      bytes: Array[Byte],
      readSchema: Schema,
      writeSchema: Schema,
      threadLocalStorage: Boolean,
  ): Either[AvroFailure, IndexedRecord] =
    try
      Right(
        AvroBinaryCursor
          .readDatum[IndexedRecord](
            bytes,
            0,
            bytes.length,
            readSchema,
            writeSchema,
            threadLocalStorage
          )
      )
    catch case NonFatal(t) => Left(AvroFailure.ResolveFailed(t))

  /** [[resolveDecodeRecord]] into `codec`'s schema, then the codec's `Any ⇒ A` side. */
  private def resolveDecodeValue[A](
      bytes: Array[Byte],
      readSchema: Schema,
      threadLocalStorage: Boolean,
  )(using codec: AvroCodec[A]): Either[AvroFailure, A] =
    resolveDecodeRecord(bytes, readSchema, codec.schema, threadLocalStorage).flatMap { record =>
      codec.decodeEither(record).left.map(t => AvroFailure.DecodeFailed(PathStep.Field(""), t))
    }

  /** Read+write Confluent optic for a KNOWN writer schema (single-schema topic, or a producer's own
    * output). `to` strips the header and resolve-decodes the body from `readSchema` into `A` (the
    * write schema = `AvroCodec[A].schema`); `from` re-encodes `A` and re-frames under `frameId`.
    * `Affine`-carried with the same `T = Either[AvroFailure, Array[Byte]]` fallible-build shape as
    * [[AvroBridge]]. For a mixed-schema stream use [[reader]] (typed) or [[resolvingBytes]]
    * (bytes), which look the writer schema up per message.
    */
  def resolving[A](readSchema: Schema, frameId: Int, threadLocalStorage: Boolean = true)(using
      codec: AvroCodec[A]
  ): Optic[Array[Byte], AvroBridge.BridgedBytes, A, A, Affine] =
    new Optional[Array[Byte], AvroBridge.BridgedBytes, A, A](
      getOrModify = framed =>
        strip(framed).flatMap(f =>
          resolveDecodeValue[A](f.body, readSchema, threadLocalStorage)
        ) match
          case r @ Right(_) => r.widenLeft
          case Left(fail)   => Left(Left(fail)),
      reverseGet = (_, a) => AvroCodec.encodeValue[A](a).map(attach(frameId, _)),
    )

  /** Framed bytes → reader-layout framed bytes for a MIXED-schema stream: per-message writer lookup
    * (like [[recordReader]]) fused with drift translation (Avro schema resolution), handing back
    * `Array[Byte]` rather than a typed `A` or an `F[A]`. Per payload: [[strip]], resolve-decode the
    * body writer → reader, re-encode under `readerSchema`, re-frame under `frameId`. Because the
    * output is reader-layout, it is stable across writer-schema evolution within a reader
    * generation — so digests, slices, and forwards computed downstream don't churn when producers
    * upgrade. That is the property the gate ([[resolve]]) cannot give.
    *
    * A factory, like [[confluent]]: the returned function closes over a per-writer-id cache
    * (`java.util.concurrent.ConcurrentHashMap`), so `schemaById` is consulted once per DISTINCT
    * writer id — one entry per schema version seen on the stream. Keep the returned function;
    * re-calling `resolvingBytes` per message discards the cache.
    *
    * Failures as `Left`: [[AvroFailure.NotConfluentFramed]] (bad or `null` frame, checked before
    * `schemaById`), [[AvroFailure.SchemaResolutionFailed]] (the hook threw),
    * [[AvroFailure.ResolveFailed]] / [[AvroFailure.EncodeFailed]] (resolve-decode / re-encode
    * failed).
    */
  def resolvingBytes(
      schemaById: SchemaById,
      readerSchema: Schema,
      frameId: Int,
      threadLocalStorage: Boolean = true,
  ): Array[Byte] => Either[AvroFailure, Array[Byte]] =
    val bridges =
      new ConcurrentHashMap[Int, Array[Byte] => Either[AvroFailure, Array[Byte]]]()

    def bridgeFor(
        writerId: Int
    ): Either[AvroFailure, Array[Byte] => Either[AvroFailure, Array[Byte]]] =
      try
        Right(
          bridges.computeIfAbsent(
            writerId,
            id =>
              val writerSchema = schemaById(id)
              body =>
                resolveDecodeRecord(body, writerSchema, readerSchema, threadLocalStorage)
                  .flatMap(AvroCodec.encodeRecord(_, readerSchema))
                  .map(attach(frameId, _)),
          )
        )
      catch case NonFatal(t) => Left(AvroFailure.SchemaResolutionFailed(writerId, t))

    framed => strip(framed).flatMap(f => bridgeFor(f.schemaId).flatMap(bridge => bridge(f.body)))

  // ---- Produce surface (graft a framed fragment into a parent encode) -
  //
  // The write-side twins of the gate ([[resolve]]) and translate ([[resolvingBytes]]) members
  // above, expressed against a target eo prism instead of a reader schema. The prism already
  // carries its focus schema (`into.codec.schema`) and codec, so the caller supplies only the
  // prism — composed from plain Scala types, e.g. `codecPrism[Conversion].field(_.clickInfo)
  // .union[ClickInfo]` — and the registry hook; no frame math, fingerprint compare, branch-index
  // synthesis, or [[AvroFailure]] juggling leaks into their code. Both close over a per-writer-id
  // cache keyed like [[resolvingBytes]], so `schemaById` is consulted once per DISTINCT writer id;
  // KEEP the returned function (re-calling per message discards the cache). Lookup FAILURES are
  // never cached — a throwing `schemaById` leaves no map entry — only the successful per-id verdict.

  /** Gate + graft a Confluent-framed fragment into `parent` at `into`'s focus, byte-exact: refuse
    * any writer/focus drift rather than splice bytes that would misdecode. Per call:
    *
    *   1. [[strip]] the fragment's 5-byte header → `(writerId, body)`;
    *   2. resolve the writer schema for `writerId` via `schemaById` (cached per id);
    *   3. gate by parsing-canonical-form fingerprint against `into`'s focus schema
    *      (`into.codec.schema`); on equality graft the bare body into `parent` via
    *      [[AvroPrism.graftBytes]] (branch index re-synthesised), else refuse with
    *      [[AvroFailure.SchemaMismatch]].
    *
    * Distinct `Left` causes for metering: [[AvroFailure.SchemaMismatch]] (permanent structural
    * drift) vs [[AvroFailure.SchemaResolutionFailed]] (transient — the hook threw) vs
    * [[AvroFailure.NotConfluentFramed]] (bad fragment frame) vs the graft's own locate failure. The
    * caller owns the fallback on a `Left` (decode-and-re-encode, alert, drop). To ABSORB compatible
    * drift automatically instead of refusing it, use [[graftResolving]].
    */
  def graftGated[B](
      into: AvroPrism[B],
      schemaById: SchemaById,
  ): (Array[Byte], Array[Byte]) => Either[AvroFailure, Array[Byte]] =
    val branchFingerprint = SchemaNormalization.parsingFingerprint64(into.codec.schema)
    val verdicts = new ConcurrentHashMap[Int, Either[AvroFailure, Unit]]()

    def verdictFor(writerId: Int): Either[AvroFailure, Unit] =
      try
        verdicts.computeIfAbsent(
          writerId,
          id =>
            val writerFingerprint = SchemaNormalization.parsingFingerprint64(schemaById(id))
            if writerFingerprint == branchFingerprint then Right(())
            else Left(AvroFailure.SchemaMismatch(id, writerFingerprint, branchFingerprint)),
        )
      catch case NonFatal(t) => Left(AvroFailure.SchemaResolutionFailed(writerId, t))

    (parent, framedFragment) =>
      strip(framedFragment).flatMap(f =>
        verdictFor(f.schemaId).flatMap(_ => into.graftBytes(parent, f.body))
      )

  /** Graft a Confluent-framed fragment into `parent` at `into`'s focus, ABSORBING writer/focus
    * drift instead of refusing it: on fingerprint match the body is spliced verbatim (zero
    * re-encode); on drift the body is resolve-decoded writer → `into`'s focus schema and re-encoded
    * into that shape, so a compatible-but-not-identical fragment grafts as correct bytes rather
    * than the silent garbage a raw byte splice would produce. Always yields valid `parent` bytes
    * for a resolvable, decodable fragment — the fingerprint gate never surfaces.
    *
    * The auto-translate twin of [[resolvingBytes]]. Per writer id the cache holds a bridge —
    * identity (verbatim) on match, resolve + re-encode on drift — built once. `Left` only on the
    * irreducibly-unrecoverable: [[AvroFailure.NotConfluentFramed]] (bad frame),
    * [[AvroFailure.SchemaResolutionFailed]] (the hook threw), [[AvroFailure.ResolveFailed]] /
    * [[AvroFailure.EncodeFailed]] (the translate itself failed), or the graft's own locate failure.
    * Use [[graftGated]] when drift is a bug to surface and meter, not a condition to absorb.
    */
  def graftResolving[B](
      into: AvroPrism[B],
      schemaById: SchemaById,
  ): (Array[Byte], Array[Byte]) => Either[AvroFailure, Array[Byte]] =
    given AvroCodec[B] = into.codec
    val branchFingerprint = SchemaNormalization.parsingFingerprint64(into.codec.schema)
    val bridges = new ConcurrentHashMap[Int, Array[Byte] => Either[AvroFailure, Array[Byte]]]()

    def bridgeFor(
        writerId: Int
    ): Either[AvroFailure, Array[Byte] => Either[AvroFailure, Array[Byte]]] =
      try
        Right(
          bridges.computeIfAbsent(
            writerId,
            id =>
              val writerSchema = schemaById(id)
              if SchemaNormalization.parsingFingerprint64(writerSchema) == branchFingerprint then
                body => Right(body)
              else
                body =>
                  AvroCodec
                    .decodeResolvedValue[B](body, writerSchema)
                    .flatMap(AvroCodec.encodeValue[B]),
          )
        )
      catch case NonFatal(t) => Left(AvroFailure.SchemaResolutionFailed(writerId, t))

    (parent, framedFragment) =>
      strip(framedFragment).flatMap(f =>
        bridgeFor(f.schemaId)
          .flatMap(bridge => bridge(f.body))
          .flatMap(body => into.graftBytes(parent, body))
      )

  /** Typed per-message Confluent reader: `Array[Byte] => F[A]`, ready for an fs2 `Stream.evalMap`.
    * Per payload: [[strip]] the header, look the writer schema up by id (`schemaById`, effectful),
    * resolve-decode into `A` (reader shape = the codec's own schema).
    *
    * Strict on the frame: a payload that does not parse as a Confluent frame raises
    * [[AvroFailure.NotConfluentFramed]] — no silent fallback to a direct decode, which could
    * accidentally succeed on corrupt bytes and yield garbage. A topic with mixed framed / unframed
    * producers opts into its own fallback by catching that failure and decoding directly
    * (`AvroCodec.decodeValue[A]`).
    *
    * All failures are raised in `F`: `NotConfluentFramed` / `ResolveFailed` / `DecodeFailed` /
    * `BinaryParseFailed` via [[AvroFailureException]]; a `schemaById` failure as the effect's own
    * error.
    */
  def reader[F[_], A](schemaById: Int => F[Schema], threadLocalStorage: Boolean = true)(using
      F: MonadThrow[F],
      codec: AvroCodec[A],
  ): Array[Byte] => F[A] =
    framed =>
      strip(framed) match
        case Right(frame) =>
          F.flatMap(schemaById(frame.schemaId)) { readSchema =>
            raiseFailure(resolveDecodeValue[A](frame.body, readSchema, threadLocalStorage))
          }
        case Left(fail) => raiseFailure(Left(fail))

  /** Generic counterpart of [[reader]] — yields `Array[Byte] => F[IndexedRecord]` resolved into the
    * caller-supplied `writeSchema`, for when no reader case class exists. Same strict frame
    * contract as [[reader]] (opt-in fallback decode: `AvroCodec.decodeRecord`).
    */
  def recordReader[F[_]](
      schemaById: Int => F[Schema],
      writeSchema: Schema,
      threadLocalStorage: Boolean = true,
  )(using
      F: MonadThrow[F]
  ): Array[Byte] => F[IndexedRecord] =
    framed =>
      strip(framed) match
        case Right(frame) =>
          F.flatMap(schemaById(frame.schemaId)) { readSchema =>
            raiseFailure(
              resolveDecodeRecord(frame.body, readSchema, writeSchema, threadLocalStorage)
            )
          }
        case Left(fail) => raiseFailure(Left(fail))

  /** Lift an `Either[AvroFailure, A]` into `F`, raising a `Left` as an [[AvroFailureException]]. */
  private def raiseFailure[F[_], A](e: Either[AvroFailure, A])(using F: MonadThrow[F]): F[A] =
    F.fromEither(e.left.map(AvroFailureException(_)))

end ConfluentWire
