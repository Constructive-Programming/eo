package dev.constructive.eo.avro

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.widenRight

/** A two-version Avro *migration bridge* as an `Affine`-carried optic. Named from the OPTIC's point
  * of view — it reads with the `readCodec` and writes with the `writeCodec`:
  *
  * {{{
  *   AvroBridge.between[A, B]:
  *     Optic[Array[Byte],   // bytes READ (encoded under readCodec's schema)
  *           BridgedBytes,  // = Either[AvroFailure, Array[Byte]] — bytes WRITTEN (under writeCodec's schema), or the failure
  *           A,             // readFocus:  the value read out of the source bytes
  *           B,             // writeFocus: the value written back
  *           Affine]
  * }}}
  *
  * `to` decodes the source bytes under the `readCodec`'s schema — a `Miss` (so `getOption` is
  * `None`) when they don't decode as an `A`. You supply the `A ⇒ B` migration as the function
  * passed to the carrier-generic `.modify` (`import dev.constructive.eo.optics.Optic.*`); `from`
  * re-encodes the resulting `B` under the `writeCodec`'s schema. That re-encode can itself fail,
  * and eo has no carrier whose `from` is fallible — so the outcome lives in the target type
  * `T = BridgedBytes = Either[AvroFailure, Array[Byte]]` (this is the "`BiAffine` behaviour without
  * a new carrier" we settled on; the generalized optic that would carry a fallible build natively
  * is deferred).
  *
  * '''Directed by design.''' The read side is `A` (the `readCodec`), the write side `B` (the
  * `writeCodec`). [[AvroBridge.reverse]] swaps the two codecs to give the
  * `Optic[Array[Byte], BridgedBytes, B, A, Affine]` going the other way. A fully symmetric encoding
  * would be a `BijectionIso` `Optic[BridgedBytes, BridgedBytes, A, B, Direct]`, but we deliberately
  * favour directedness over that Iso and don't implement it.
  *
  * Each side decodes / encodes under its OWN exact schema, so this is an explicit, user-driven
  * migration between two model versions — NOT Avro's automatic writer→reader compatibility
  * resolution (`ResolvingDecoder`; for that, see [[ConfluentWire.reader]]). The decode / encode go
  * through the shared [[AvroCodec.decodeValue]] / [[AvroCodec.encodeValue]] helpers — no
  * serialization path of its own.
  *
  * Being an `Optic[…, Affine]`, it picks up the capability-gated surface — `.getOption`, `.modify`,
  * `.replace`, `.andThen`, … — from `dev.constructive.eo.optics.Optic`.
  */
final class AvroBridge[A, B] private (
    readCodec: AvroCodec[A],
    writeCodec: AvroCodec[B],
) extends Optic[Array[Byte], AvroBridge.BridgedBytes, A, B, Affine]:

  import AvroBridge.BridgedBytes

  /** Structural leftover: `Fst[X]` is a [[AvroBridge.BridgedBytes]] (a Miss carries the failure as
    * the `T` value), `Snd[X]` the source bytes (unused by [[from]], which re-encodes the migrated
    * `B` from scratch). Same `X` shape as `Optional`.
    */
  type X = (BridgedBytes, Array[Byte])

  /** Decode the source bytes under the `readCodec`'s schema — a `Miss` when they don't decode as an
    * `A`.
    */
  def to(bytes: Array[Byte]): Affine[X, A] =
    AvroCodec.decodeValue(bytes)(using readCodec) match
      case Right(a)    => new Affine.Hit[X, A](bytes, a)
      case l @ Left(_) => new Affine.Miss[X](l.widenRight)

  /** Re-encode the migrated `B` under the `writeCodec`'s schema; a `Miss` passes its failure
    * through as the `Left` of [[AvroBridge.BridgedBytes]].
    */
  def from(aff: Affine[X, B]): BridgedBytes =
    aff match
      case h: Affine.Hit[X, B] => AvroCodec.encodeValue(h.b)(using writeCodec)
      case m: Affine.Miss[X]   => m.fst

  /** The reverse migration — reads `B` under the (old) write schema, migrates `B ⇒ A` via
    * `.modify`, and writes `A` under the (old) read schema. Just the two codecs swapped.
    */
  def reverse: AvroBridge[B, A] = new AvroBridge(writeCodec, readCodec)

end AvroBridge

/** Constructor for [[AvroBridge]]. */
object AvroBridge:

  /** The fallible written-bytes result of a migration: `Right(bytes)` encoded under the
    * `writeCodec` (version-`B`) schema, or a `Left` failure (source didn't decode as the read
    * version `A`, or the migrated `B` didn't encode). A single [[AvroFailure]] — the bridge has one
    * failure point per direction, so no `Chain` accumulation.
    */
  type BridgedBytes = Either[AvroFailure, Array[Byte]]

  /** Bridge that reads version `A` (via `readCodec`) and writes version `B` (via `writeCodec`),
    * under each codec's own schema. `.reverse` gives the `B ⇒ A` bridge.
    *
    * @group Constructors
    */
  def between[A, B](using
      readCodec: AvroCodec[A],
      writeCodec: AvroCodec[B],
  ): AvroBridge[A, B] =
    new AvroBridge(readCodec, writeCodec)

end AvroBridge
