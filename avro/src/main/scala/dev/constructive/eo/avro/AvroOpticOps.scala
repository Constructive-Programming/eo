package dev.constructive.eo.avro

import cats.data.{Chain, Ior}
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** Public surface forwarder for the Avro optic carriers (Unit 4: just [[AvroPrism]]; Units 6 / 7
  * will add `AvroTraversal` / `AvroFieldsTraversal`).
  *
  * '''Per OQ-avro-7 (plan):''' this is a deliberate copy-paste of
  * `dev.constructive.eo.circe.JsonOpticOps`. When a third cursor module appears (eo-protobuf,
  * eo-cbor, ...) the right thing is to generalise to `core.OpticOps[Carrier, Failure, A]`. Until
  * then the duplication is intentional — the surface is small enough that a clean abstraction is
  * cheaper to reach for once we have three concrete shapes than to design upfront.
  *
  * Concrete classes supply six per-record hooks (three Ior-bearing, three silent); the trait wires
  * up the public surface in terms of those hooks plus the common [[AvroFailure.parseInputIor]] /
  * [[AvroFailure.parseInputUnsafe]] dual-input shim.
  */
private[avro] trait AvroOpticOps[A]:

  // ---- Abstract members supplied by each carrier ---------------------

  /** The reader schema used to decode `Array[Byte]` inputs. The carrier's constructor cached this
    * off the user's `AvroCodec[Root]` (or accepted it as a `given Schema` per OQ-avro-5); exposing
    * it here lets [[AvroFailure.parseInputIor]] thread it without re-summoning.
    */
  protected def rootSchema: Schema

  protected def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord]

  protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord]

  protected def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord]

  protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord

  protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord

  protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord

  // ---- Default (Ior-bearing) surface --------------------------------

  /** Apply `f` to the focused value; returns the modified record. Failures (path miss, decode
    * failure, type mismatch) accumulate into `Chain[AvroFailure]`; partial success returns
    * `Ior.Both(chain, inputRecord)` (the modify-family preserves the input as the silent fallback).
    */
  def modify(f: A => A): (IndexedRecord | Array[Byte]) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => modifyIor(j, f))

  /** Apply `f` to the raw [[IndexedRecord]] at the focused position (no decode / encode through the
    * codec layer). Useful for carrier-aware rewrites that don't want to round-trip through the
    * focus type.
    *
    * Note: [[transformIor]]'s implementation in [[AvroPrism]] passes through the focused cursor's
    * *value* — for a leaf focus that may be a primitive (Long, Utf8, …), so the user's
    * `f: IndexedRecord => IndexedRecord` is a slight type misfit at non-record-typed leaves. Unit
    * 5+ may revisit the parametric shape.
    */
  def transform(
      f: IndexedRecord => IndexedRecord
  ): (IndexedRecord | Array[Byte]) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => transformIor(j, f))

  /** Replace the focused value with `a`. Same failure surface as [[modify]]. */
  def place(a: A): (IndexedRecord | Array[Byte]) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, a))

  /** Lift a `C => A` into a focus-replacer: `transfer(f)(record)(c)` decodes `c` to `A` via `f` and
    * writes it at the focused position. Same failure surface as [[modify]].
    */
  def transfer[C](
      f: C => A
  ): (IndexedRecord | Array[Byte]) => C => Ior[Chain[AvroFailure], IndexedRecord] =
    input => c => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, f(c)))

  // ---- *Unsafe (silent) escape hatches -------------------------------

  /** Silent counterpart to [[modify]] — input pass-through on any failure. */
  def modifyUnsafe(f: A => A): (IndexedRecord | Array[Byte]) => IndexedRecord =
    input => modifyImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[transform]]. */
  def transformUnsafe(
      f: IndexedRecord => IndexedRecord
  ): (IndexedRecord | Array[Byte]) => IndexedRecord =
    input => transformImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[place]]. */
  def placeUnsafe(a: A): (IndexedRecord | Array[Byte]) => IndexedRecord =
    input => placeImpl(AvroFailure.parseInputUnsafe(input, rootSchema), a)

  /** Silent counterpart to [[transfer]]. */
  def transferUnsafe[C](f: C => A): (IndexedRecord | Array[Byte]) => C => IndexedRecord =
    input => c => placeImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f(c))

end AvroOpticOps
