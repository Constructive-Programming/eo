package dev.constructive.eo.avro

import cats.data.{Chain, Ior}
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** The [[org.apache.avro.generic.IndexedRecord]]-carried face of an [[AvroPrism]] — reachable ONLY
  * through [[AvroPrism.record]], never constructed directly:
  *
  * {{{
  *   AvroRecordPrism[A] <: Optic[IndexedRecord, IndexedRecord, A, A, Either]
  *   type X = (AvroFailure, IndexedRecord)
  * }}}
  *
  * Mirrors `dev.constructive.eo.circe.JsonPrism` for the parsed-record carrier. Two call-surface
  * tiers (via [[AvroOpticOps]]):
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
) extends Optic[IndexedRecord, IndexedRecord, A, A, Either],
      AvroOpticOps[A]:

  type X = (AvroFailure, IndexedRecord)

  override protected def rootSchema: Schema = rootSchemaCached

  private def path: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  // ---- Abstract Optic members ---------------------------------------

  def to(record: IndexedRecord): Either[(AvroFailure, IndexedRecord), A] =
    focus.navigateRaw(record) match
      case Left(failure) => Left((failure, record))
      case Right(any)    =>
        focus.decodeFrom(any) match
          case Right(a) => Right(a)
          case Left(t)  =>
            Left((AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), t), record))

  def from(e: Either[(AvroFailure, IndexedRecord), A]): IndexedRecord =
    e match
      case Left((_, record)) => record
      case Right(a)          => reverseGet(a)

  // ---- Read surface --------------------------------------------------

  /** Decode the focused value, threading parse failures (for `Array[Byte]` input) and walk failures
    * through the Ior channel.
    */
  def get(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(focus.readIor)

  /** Encode `a` standalone, returning the codec's [[IndexedRecord]] payload (or a synthesised empty
    * record when the encoded value isn't record-shaped). Counterpart to `JsonPrism.reverseGet`.
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
