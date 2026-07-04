package dev.constructive.eo.avro

import cats.data.{Chain, Ior}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** The [[org.apache.avro.generic.IndexedRecord]]-carried face of an [[AvroPrism]] — reachable ONLY
  * through [[AvroPrism.record]], never constructed directly:
  *
  * {{{
  *   AvroRecordPrism[A] <: Optic[IndexedRecord, IndexedRecord, A, A, Affine]
  *   type X = (IndexedRecord, A => IndexedRecord)  // Fst = source (miss); Snd = single-walk writer
  * }}}
  *
  * '''An Optional, not a Prism.''' A drilled record focus lives INSIDE a record, so rebuilding the
  * record needs the siblings — which a `Prism`'s `from(reverseGet)` cannot see (it gets the focus
  * alone). Carrying the source on the `Affine` seam fixes that at the carrier level: `to` captures
  * a writer over the walk it already did, and `from(Hit)` applies it — so `.modify` / `.replace`
  * preserve siblings whether called directly, upcast to `Optic[…, Affine]`, or composed via
  * `andThen` (the composite threads the writer end-to-end). Same carrier and law story as the byte
  * face; only the root full-cover prism is also a lawful Prism, and [[reverseGet]] stays for it.
  *
  * Two call-surface tiers (via [[AvroOpticOps]]):
  *
  *   - Default Ior-bearing: `modify` / `get` etc. accumulate `Chain[AvroFailure]` on failure;
  *     partial success surfaces as `Ior.Both(chain, inputRecord)`.
  *   - `*Unsafe`: silent pass-through hot path.
  *
  * Triple-input shape `IndexedRecord | Array[Byte] | String` (where `String` is the Avro JSON wire
  * format), decoding byte / JSON input under the cached root schema.
  *
  * Drilling (`.field` / `.fields` / `.union` / `.at` / `.each` / Dynamic selection) lives on
  * [[AvroPrism]] alone — drill there, then flip with `.record` at the end. One drilling mechanism,
  * two carriers.
  */
final class AvroRecordPrism[A] private[avro] (
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends Optic[IndexedRecord, IndexedRecord, A, A, Affine],
      AvroOpticOps[A]:

  // Fst[X] = source record (Miss pass-through); Snd[X] = the single-walk writer captured by `to`.
  type X = (IndexedRecord, A => IndexedRecord)

  override protected def rootSchema: Schema = rootSchemaCached

  // ---- Abstract Optic members ---------------------------------------

  def to(record: IndexedRecord): Affine[X, A] =
    focus.navigateForWrite(record) match
      case Left(_)            => new Affine.Miss[X, A](record)
      case Right((a, writer)) => new Affine.Hit[X, A](writer, a)

  def from(aff: Affine[X, A]): IndexedRecord =
    aff match
      case m: Affine.Miss[X, A] => m.fst
      case h: Affine.Hit[X, A]  => h.snd(h.b)

  // ---- Read surface --------------------------------------------------

  /** Decode the focused value, threading parse failures (for `Array[Byte]` input) and walk failures
    * through the Ior channel.
    */
  def get(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(focus.readIor)

  /** Encode `a` standalone, returning the codec's [[IndexedRecord]] payload (or a synthesised empty
    * record when the encoded value isn't record-shaped). Lawful only for the ROOT full-cover prism
    * (a real `Prism.reverseGet`); on a drilled prism it cannot restore siblings, which is exactly
    * why the Optic seam carries the source instead of calling this. Kept for root full-cover users
    * and the reverseGet round-trip law checks.
    */
  def reverseGet(a: A): IndexedRecord =
    focus.codec.encode(a) match
      case any: IndexedRecord => any
      case _                  =>
        new org.apache.avro.generic.GenericData.Record(rootSchemaCached)

  /** Silent counterpart to [[get]] — `None` on any failure. */
  inline def getOptionUnsafe(input: IndexedRecord | Array[Byte] | String): Option[A] =
    focus.readImpl(AvroFailure.parseInputUnsafe(input, rootSchemaCached))

  // ---- Per-record Ior-bearing hooks (delegate to focus) -------------

  override protected def modifyIor(
      record: IndexedRecord,
      f: A => A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.modifyIor(record, f)

  override protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.transformIor(record, f)

  override protected def placeIor(
      record: IndexedRecord,
      a: A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.placeIor(record, a)

  // ---- Per-record silent (*Unsafe) hooks (delegate to focus) --------

  override protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
    focus.modifyImpl(record, f)

  override protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord =
    focus.transformImpl(record, f)

  override protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
    focus.placeImpl(record, a)

end AvroRecordPrism
