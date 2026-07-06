package dev.constructive.eo.avro

import cats.data.Chain
import dev.constructive.eo.optics.{Optic, Optional}
import org.apache.avro.Schema

/** A two-version Avro *migration bridge* as an `Affine`-carried optic:
  *
  * {{{
  *   AvroBridge.between[A, B]:
  *     Optic[Array[Byte],                     // S: writer bytes, encoded under A's schema
  *           Either[Chain[AvroFailure], Array[Byte]], // T: reader bytes (under B's schema) or failure
  *           A,                               // read focus: the version-A value
  *           B,                               // write focus: the version-B value
  *           Affine]
  * }}}
  *
  * `to` decodes the writer bytes under `A`'s schema (a `Miss` if they don't decode — bad bytes, or
  * a payload that isn't an `A` at all); you supply the `A ⇒ B` migration through `.modify`; `from`
  * re-encodes the `B` under `B`'s schema, yielding `Either[Chain[AvroFailure], Array[Byte]]`. The
  * failure on the write side is the point of the shape: eo has no carrier whose `from` can itself
  * fail, so we simulate that "fallible build" by putting the failure in `T = Either[E, T1]` (the
  * `BiAffine`-without-a-new-carrier trick) — the generalized optic that would carry it natively is
  * deferred.
  *
  * Reversing the migration is just the type-swapped bridge — `AvroBridge.between[B, A]` gives the
  * `B`-bytes ⇒ `A`-bytes direction. Each side decodes / encodes under its OWN exact schema, so no
  * writer↔reader schema-resolution (`ResolvingDecoder`) is involved: this is an explicit,
  * user-driven migration between two model versions, not Avro's automatic compatibility resolution.
  *
  * Composes like any `Affine` optic: `AvroBridge.between[A, B].andThen(...)`, `.getOption`,
  * `.modify`, `.replace`. Reuses the module's existing byte-boundary helpers
  * ([[AvroFailure.parseInputIor]] for decode, [[AvroFailure.encodeBinary]] for encode) — no new
  * serialization path.
  */
object AvroBridge:

  /** The fallible reader-bytes result of a migration: `Right(bytes)` under `B`'s schema, or a
    * `Left` failure chain (source didn't decode as `A`, or the `B` didn't encode).
    */
  type Migrated = Either[Chain[AvroFailure], Array[Byte]]

  /** Bridge between the schemas the in-scope codecs carry — `A` read under `AvroCodec[A].schema`,
    * `B` written under `AvroCodec[B].schema`.
    *
    * @group Constructors
    */
  def between[A, B](using
      codecA: AvroCodec[A],
      codecB: AvroCodec[B],
  ): Optic[Array[Byte], Migrated, A, B, dev.constructive.eo.data.Affine] =
    between[A, B](codecA.schema, codecB.schema)

  /** Bridge with explicit reader/writer schemas — the streaming-pipeline form where the schema for
    * a version arrives at runtime rather than off the derived codec.
    *
    * @group Constructors
    */
  def between[A, B](schemaA: Schema, schemaB: Schema)(using
      codecA: AvroCodec[A],
      codecB: AvroCodec[B],
  ): Optic[Array[Byte], Migrated, A, B, dev.constructive.eo.data.Affine] =
    new Optional[Array[Byte], Migrated, A, B](
      getOrModify = bytesA =>
        decodeUnder(bytesA, schemaA, codecA) match
          case Right(a)    => Right(a)
          case Left(chain) => Left(Left(chain)), // Optional-miss; the T value carries the failure
      reverseGet = (_, b) => encodeUnder(b, schemaB, codecB),
    )

  /** Binary-decode `bytes` under `schema`, then run the codec's `Any ⇒ A` side. */
  private def decodeUnder[A](
      bytes: Array[Byte],
      schema: Schema,
      codec: AvroCodec[A],
  ): Either[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(bytes, schema).toEither.flatMap { record =>
      codec
        .decodeEither(record)
        .left
        .map(t => Chain.one(AvroFailure.DecodeFailed(PathStep.Field(""), t)))
    }

  /** Run the codec's `B ⇒ Any` side, then binary-encode under `schema`. */
  private def encodeUnder[B](
      b: B,
      schema: Schema,
      codec: AvroCodec[B],
  ): Migrated =
    AvroFailure
      .encodeBinary(codec.encode(b), schema)
      .left
      .map(t => Chain.one(AvroFailure.EncodeFailed(t)))

end AvroBridge
