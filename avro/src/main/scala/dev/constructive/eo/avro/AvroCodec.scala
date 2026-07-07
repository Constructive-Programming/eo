package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.apache.avro.Schema
import org.apache.avro.generic.{
  GenericData,
  GenericDatumReader,
  GenericDatumWriter,
  GenericRecord,
  IndexedRecord,
}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

/** A unified read/write/schema codec for Avro values, combining the kindlings-avro-derivation
  * triplet `(AvroEncoder[A], AvroDecoder[A], AvroSchemaFor[A])` into a single typeclass that user
  * code summons once per type.
  *
  * Why an extra wrapper at all? kindlings ships three independent typeclasses so users can encode-
  * only or decode-only — but cats-eo-avro's optics always need both sides to be honest (a Prism's
  * `get` decodes, its `reverseGet` / `place` encodes). Forcing every call site to thread two
  * `using` parameters is noisy. `AvroCodec[A]` is the project-internal shorthand.
  *
  * Why not vulcan? Vulcan 1.13.x pins apache-avro 1.11.5; kindlings-avro-derivation 0.1.2 pins
  * 1.12.1. cats-eo-avro chose kindlings + avro 1.12 because it lines up with the rest of the
  * ecosystem moving forward, and the typeclass surface is simpler (no `Either[AvroError, A]`
  * threading on every call — kindlings' decoders throw on failure, which the prism layer wraps into
  * [[AvroFailure]]).
  *
  * '''Per the eo-avro plan (OQ-avro-1):''' the codec library at v0.1.0 is
  * `com.kubuszok:kindlings-avro-derivation`. Vulcan and raw apache-avro stay viable as future
  * alternatives if user demand surfaces, behind a copy-paste of this file.
  */
trait AvroCodec[A]:

  /** The Avro schema describing this codec's wire shape. Pulled off the encoder (or decoder — they
    * agree by construction) so callers don't have to summon `AvroSchemaFor[A]` separately.
    */
  def schema: Schema

  /** Encode an `A` to an Avro-shaped runtime value (an [[org.apache.avro.generic.IndexedRecord]], a
    * primitive box, an [[org.apache.avro.util.Utf8]], or a [[org.apache.avro.generic.GenericArray]]
    * — varies by `A`). The kindlings encoder is a pure (no-`Either`) shape; the optic layer never
    * needs to recover here.
    */
  def encode(a: A): Any

  /** Decode an Avro-shaped runtime value to an `A`, or surface the kindlings decoder's exception
    * via [[Left]]. The decoder itself throws on bad input; this method wraps the throw to match the
    * project's structured-failure conventions.
    */
  def decodeEither(any: Any): Either[Throwable, A]

  /** Convenience: throw on failure. Used internally by the macro layer when a structured failure
    * isn't available; production call sites should prefer [[decodeEither]].
    */
  def decodeUnsafe(any: Any): A = decodeEither(any) match
    case Right(a) => a
    case Left(t)  => throw t

end AvroCodec

object AvroCodec:

  /** Default project-wide kindlings config: identity field-name transform, no namespace override,
    * no decimal config. Users can shadow this with their own `given AvroConfig` if they need a
    * snake-cased schema or a forced namespace.
    */
  given default: AvroConfig = AvroConfig()

  /** Combine kindlings' derived `AvroEncoder[A]` / `AvroDecoder[A]` / `AvroSchemaFor[A]` into a
    * single [[AvroCodec]]. Each kindlings typeclass is summoned independently, so this works
    * whether the user has hand-written one of the sides or used kindlings' macro derivation for
    * both.
    */
  given derived[A](using
      enc: AvroEncoder[A],
      dec: AvroDecoder[A],
      sf: AvroSchemaFor[A],
  ): AvroCodec[A] =
    new AvroCodec[A]:
      val schema: Schema = sf.schema
      def encode(a: A): Any = enc.encode(a)
      def decodeEither(any: Any): Either[Throwable, A] =
        try Right(dec.decode(any))
        catch case NonFatal(t) => Left(t)

  // ---- Serialization boundary (apache-avro binary / JSON) ------------
  //
  // The `Any` ↔ bytes/JSON wire boundary. Kindlings owns the native↔`Any` side (`encode` /
  // `decodeEither` above); apache-avro owns wire-format serialization, and these helpers are the
  // eo-avro counterpart to it. Record-level (`*Record`) works under an explicit schema; value-level
  // (`*Value`) threads a codec's own schema plus its `Any` conversion.

  /** Binary-decode `bytes` under `schema` to a generic record — no typed codec involved. Catches
    * apache-avro's binary-decoder throws (`IOException` / `EOFException` / `AvroRuntimeException`)
    * into [[AvroFailure.BinaryParseFailed]].
    */
  def decodeRecord(bytes: Array[Byte], schema: Schema): Either[AvroFailure, IndexedRecord] =
    try
      val reader = new GenericDatumReader[GenericRecord](schema)
      val decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null)
      Right(reader.read(null, decoder))
    catch case NonFatal(t) => Left(AvroFailure.BinaryParseFailed(t))

  /** [[decodeRecord]] under `codec`'s schema, then the codec's `Any ⇒ A` side —
    * [[AvroFailure.DecodeFailed]] when the parsed record doesn't line up with `A`.
    */
  def decodeValue[A](bytes: Array[Byte])(using codec: AvroCodec[A]): Either[AvroFailure, A] =
    decodeRecord(bytes, codec.schema).flatMap { record =>
      codec.decodeEither(record).left.map(t => AvroFailure.DecodeFailed(PathStep.Field(""), t))
    }

  /** Binary-decode `bytes` (encoded under `readSchema`) and RESOLVE them into `writeSchema` via
    * apache-avro's schema resolution (reorder / default / promotion / aliases) — the drift
    * counterpart of [[decodeRecord]] (which assumes one schema). Named from the reader's point of
    * view: `readSchema` is the schema the bytes are read in, `writeSchema` the shape they resolve
    * into (what you'd write). In apache-avro's own vocabulary these are the *writer* schema and the
    * *reader* schema respectively — hence `GenericDatumReader(readSchema, writeSchema)` below.
    * Incompatible schemas → [[AvroFailure.ResolveFailed]].
    */
  def decodeResolvedRecord(
      bytes: Array[Byte],
      readSchema: Schema,
      writeSchema: Schema,
  ): Either[AvroFailure, IndexedRecord] =
    try
      // apache-avro arg order is (writerSchema, readerSchema) = (readSchema, writeSchema) here.
      val reader = new GenericDatumReader[GenericRecord](readSchema, writeSchema)
      val decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null)
      Right(reader.read(null, decoder))
    catch case NonFatal(t) => Left(AvroFailure.ResolveFailed(t))

  /** [[decodeResolvedRecord]] resolving `readSchema` → `codec`'s schema (the write schema), then
    * the codec's `Any ⇒ A` side — the drift counterpart of [[decodeValue]].
    */
  def decodeResolvedValue[A](bytes: Array[Byte], readSchema: Schema)(using
      codec: AvroCodec[A]
  ): Either[AvroFailure, A] =
    decodeResolvedRecord(bytes, readSchema, codec.schema).flatMap { record =>
      codec.decodeEither(record).left.map(t => AvroFailure.DecodeFailed(PathStep.Field(""), t))
    }

  /** Binary-encode an already-`Any`-shaped `datum` under `schema` — the write-side counterpart of
    * [[decodeRecord]]. `GenericDatumWriter` throws `AvroTypeException` / `NullPointerException` /
    * `ClassCastException` when the datum doesn't line up with the schema; caught into
    * [[AvroFailure.EncodeFailed]].
    */
  def encodeRecord(datum: Any, schema: Schema): Either[AvroFailure, Array[Byte]] =
    try
      val out = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[Any](schema)
      val encoder = EncoderFactory.get().binaryEncoder(out, null)
      writer.write(datum, encoder)
      encoder.flush()
      Right(out.toByteArray)
    catch case NonFatal(t) => Left(AvroFailure.EncodeFailed(t))

  /** The codec's `A ⇒ Any` side, then [[encodeRecord]] under `codec`'s schema. */
  def encodeValue[A](value: A)(using codec: AvroCodec[A]): Either[AvroFailure, Array[Byte]] =
    encodeRecord(codec.encode(value), codec.schema)

  /** Resolve an `IndexedRecord | Array[Byte] | String` input to a parsed `IndexedRecord`, threading
    * parse failures through the Ior channel. Used by every dual-/triple-input-accepting overload on
    * [[AvroPrism]] / [[AvroTraversal]] so the parse step is uniform.
    *
    * Arms: [[IndexedRecord]] passes through (`Ior.Right`, no parse); `Array[Byte]` runs
    * [[decodeRecord]] (→ `BinaryParseFailed`); `String` runs the Avro-JSON wire format decoder (→
    * `JsonParseFailed`). The `String` arm matches the exact runtime type —
    * `org.apache.avro.util.Utf8` also implements `CharSequence` and would otherwise be miscaptured
    * as JSON.
    *
    * @param schema
    *   the reader schema used to decode binary / JSON input. Ignored for record input.
    */
  private[avro] def parseInputIor(
      input: IndexedRecord | Array[Byte] | String,
      schema: Schema,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    input match
      case r: IndexedRecord => Ior.Right(r)
      case bs: Array[Byte]  =>
        decodeRecord(bs, schema).fold(f => Ior.Left(Chain.one(f)), Ior.Right(_))
      case s: String =>
        decodeJsonString(s, schema) match
          case Right(r) => Ior.Right(r)
          case Left(t)  => Ior.Left(Chain.one(AvroFailure.JsonParseFailed(t)))

  /** Resolve an `IndexedRecord | Array[Byte] | String` input, dropping failures — for the `*Unsafe`
    * escape hatches. Bad bytes / bad JSON produce a synthetic empty record built from `schema`
    * (positional slots zero-initialised).
    */
  private[avro] def parseInputUnsafe(
      input: IndexedRecord | Array[Byte] | String,
      schema: Schema,
  ): IndexedRecord =
    input match
      case r: IndexedRecord => r
      case bs: Array[Byte]  => decodeRecord(bs, schema).getOrElse(new GenericData.Record(schema))
      case s: String        =>
        decodeJsonString(s, schema) match
          case Right(r) => r
          case Left(_)  => new GenericData.Record(schema)

  /** Avro-JSON wire-format decode (Jackson-backed under the hood). Kept private — the public parse
    * surface is [[parseInputIor]] / [[parseInputUnsafe]]; there's no bare JSON-record caller yet.
    */
  private def decodeJsonString(s: String, schema: Schema): Either[Throwable, IndexedRecord] =
    try
      val reader = new GenericDatumReader[GenericRecord](schema)
      val decoder = DecoderFactory.get().jsonDecoder(schema, s)
      Right(reader.read(null, decoder))
    catch case NonFatal(t) => Left(t)

end AvroCodec
