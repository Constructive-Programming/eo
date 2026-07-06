package dev.constructive.eo.avro

import scala.util.control.NonFatal

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.EncoderFactory

/** A two-version Avro *migration bridge* as an `Affine`-carried optic:
  *
  * {{{
  *   AvroBridge.between[A, B]:
  *     Optic[Array[Byte],   // writer bytes (encoded under A's schema)
  *           BridgedBytes,  // = Either[AvroFailure, Array[Byte]] — reader bytes (under B's schema), or the failure
  *           A,             // writerFocus: the version-A value read out of the writer bytes
  *           B,             // readerFocus: the version-B value written back as reader bytes
  *           Affine]
  * }}}
  *
  * `to` decodes the writer bytes under `A`'s (the writer codec's) schema — a `Miss` (so `getOption`
  * is `None`) when they don't decode as an `A`. You supply the `A ⇒ B` migration as the function
  * passed to the carrier-generic `.modify` (`import dev.constructive.eo.optics.Optic.*`); `from`
  * re-encodes the resulting `B` under `B`'s (the reader codec's) schema. That re-encode can itself
  * fail, and eo has no carrier whose `from` is fallible — so the outcome lives in the target type
  * `T = BridgedBytes = Either[AvroFailure, Array[Byte]]` (this is the "`BiAffine` behaviour without
  * a new carrier" we settled on; the generalized optic that would carry a fallible build natively
  * is deferred).
  *
  * '''Directed by design.''' The read side is always the WRITER version (`A`), the write side the
  * READER version (`B`). [[AvroBridge.reverse]] swaps the two codecs to give the
  * `Optic[Array[Byte], BridgedBytes, B, A, Affine]` going the other way. A fully symmetric encoding
  * would be a `BijectionIso` `Optic[BridgedBytes, BridgedBytes, A, B, Direct]`, but we deliberately
  * favour directedness over that Iso and don't implement it.
  *
  * Each side decodes / encodes under its OWN exact schema, so this is an explicit, user-driven
  * migration between two model versions — NOT Avro's automatic writer→reader compatibility
  * resolution (`ResolvingDecoder`). Reuses the module's binary decode
  * ([[AvroFailure.decodeBinary]]); the binary encode is local (there's no other module caller for
  * it).
  *
  * Being an `Optic[…, Affine]`, it picks up the capability-gated surface — `.getOption`, `.modify`,
  * `.replace`, `.andThen`, … — from `dev.constructive.eo.optics.Optic`.
  */
final class AvroBridge[A, B] private (
    writerCodec: AvroCodec[A],
    readerCodec: AvroCodec[B],
) extends Optic[Array[Byte], AvroBridge.BridgedBytes, A, B, Affine]:

  import AvroBridge.BridgedBytes

  // Fst[X] = BridgedBytes (a Miss carries the failure as the T value); Snd[X] = the source bytes
  // (unused by `from`, which re-encodes the migrated B from scratch). Same X shape as `Optional`.
  type X = (BridgedBytes, Array[Byte])

  private val writerSchema: Schema = writerCodec.schema
  private val readerSchema: Schema = readerCodec.schema

  def to(bytes: Array[Byte]): Affine[X, A] =
    decode(bytes) match
      case Right(a) => new Affine.Hit[X, A](bytes, a)
      case Left(f)  => new Affine.Miss[X, A](Left(f))

  def from(aff: Affine[X, B]): BridgedBytes =
    aff match
      case h: Affine.Hit[X, B]  => encode(h.b)
      case m: Affine.Miss[X, B] => m.fst

  /** The reverse migration — reads `B` under the reader schema, migrates `B ⇒ A` via `.modify`, and
    * writes `A` under the writer schema. Just the two codecs swapped.
    */
  def reverse: AvroBridge[B, A] = new AvroBridge(readerCodec, writerCodec)

  /** Binary-decode under the writer schema, then run the codec's `Any ⇒ A` side — collapsed to a
    * single [[AvroFailure]] (the source has exactly one failure point).
    */
  private def decode(bytes: Array[Byte]): Either[AvroFailure, A] =
    AvroFailure
      .decodeBinary(bytes, writerSchema)
      .left
      .map(AvroFailure.BinaryParseFailed(_))
      .flatMap { record =>
        writerCodec
          .decodeEither(record)
          .left
          .map(t => AvroFailure.DecodeFailed(PathStep.Field(""), t))
      }

  /** Run the codec's `B ⇒ Any` side, then binary-encode under the reader schema via apache-avro's
    * `GenericDatumWriter` — the write-side counterpart of [[AvroFailure.decodeBinary]], local
    * because [[AvroBridge]] is its only caller. `GenericDatumWriter` throws `AvroTypeException` /
    * `NullPointerException` / `ClassCastException` when the datum doesn't line up with the schema.
    */
  private def encode(b: B): BridgedBytes =
    try
      val out = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[Any](readerSchema)
      val encoder = EncoderFactory.get().binaryEncoder(out, null)
      writer.write(readerCodec.encode(b), encoder)
      encoder.flush()
      Right(out.toByteArray)
    catch case NonFatal(t) => Left(AvroFailure.EncodeFailed(t))

end AvroBridge

/** Constructor for [[AvroBridge]]. */
object AvroBridge:

  /** The fallible reader-bytes result of a migration: `Right(bytes)` encoded under the reader
    * (version-`B`) schema, or a `Left` failure (source didn't decode as the writer version `A`, or
    * the migrated `B` didn't encode). A single [[AvroFailure]] — the bridge has one failure point
    * per direction, so no `Chain` accumulation.
    */
  type BridgedBytes = Either[AvroFailure, Array[Byte]]

  /** Bridge from writer version `A` to reader version `B`, reading / writing under each codec's own
    * schema. `.reverse` gives the `B ⇒ A` bridge.
    *
    * @group Constructors
    */
  def between[A, B](using
      writerCodec: AvroCodec[A],
      readerCodec: AvroCodec[B],
  ): AvroBridge[A, B] =
    new AvroBridge(writerCodec, readerCodec)

end AvroBridge
