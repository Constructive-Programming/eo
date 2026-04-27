package dev.constructive.eo.avro

import scala.util.control.NonFatal

import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema

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
  * threading on every call — kindlings' decoders throw on failure, which the prism layer wraps
  * into [[AvroFailure]]).
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

  /** Encode an `A` to an Avro-shaped runtime value (an [[org.apache.avro.generic.IndexedRecord]],
    * a primitive box, an [[org.apache.avro.util.Utf8]], or a [[org.apache.avro.generic.GenericArray]]
    * — varies by `A`). The kindlings encoder is a pure (no-`Either`) shape; the optic layer never
    * needs to recover here.
    */
  def encode(a: A): Any

  /** Decode an Avro-shaped runtime value to an `A`, or surface the kindlings decoder's exception
    * via [[Left]]. The decoder itself throws on bad input; this method wraps the throw to match
    * the project's structured-failure conventions.
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

end AvroCodec
