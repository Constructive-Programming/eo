package dev.constructive.eo.avro

import cats.data.{Chain, Ior}
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** Public surface forwarder for the record-carried Avro optics [[AvroRecordPrism]] and
  * [[AvroRecordTraversal]] — the `.record` faces of [[AvroPrism]] / [[AvroTraversal]].
  *
  * A deliberate copy-paste of `dev.constructive.eo.circe.JsonOpticOps`. When a third cursor module
  * appears (eo-protobuf, eo-cbor, ...) the right thing is to generalise to
  * `core.OpticOps[Carrier, Failure, A]`. Until then the duplication is intentional — the surface is
  * small enough that a clean abstraction is cheaper to reach for once we have three concrete shapes
  * than to design upfront.
  *
  * Concrete classes supply six per-record hooks (three Ior-bearing, three silent); the trait wires
  * up the public surface in terms of those hooks plus the common [[AvroCodec.parseInputIor]] /
  * [[AvroCodec.parseInputUnsafe]] dual-input shim.
  */
private[avro] trait AvroOpticOps[A]:

  // ---- Abstract members supplied by each carrier ---------------------

  /** The reader schema used to decode `Array[Byte]` and `String` (Avro JSON wire format) inputs.
    * The carrier's constructor cached this off the user's `AvroCodec[Root]`; exposing it here lets
    * `AvroCodec.parseInputIor` thread it without re-summoning.
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
  def modify(
      f: A => A
  ): (IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroCodec.parseInputIor(input, rootSchema).flatMap(j => modifyIor(j, f))

  /** Apply `f` to the raw `IndexedRecord` at the focused position (no decode / encode through the
    * codec layer). Useful for carrier-aware rewrites that don't want to round-trip through the
    * focus type.
    *
    * Note: the focused cursor's *value* is what gets passed through — for a leaf focus that may be
    * a primitive (Long, Utf8, …), so the user's `f: IndexedRecord => IndexedRecord` is a slight
    * type misfit at non-record-typed leaves.
    */
  def transform(
      f: IndexedRecord => IndexedRecord
  ): (IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroCodec.parseInputIor(input, rootSchema).flatMap(j => transformIor(j, f))

  /** Replace the focused value with `a`. Same failure surface as [[modify]]. */
  def place(
      a: A
  ): (IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroCodec.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, a))

  /** Lift a `C => A` into a focus-replacer: `transfer(f)(record)(c)` decodes `c` to `A` via `f` and
    * writes it at the focused position. Same failure surface as [[modify]].
    */
  def transfer[C](
      f: C => A
  ): (IndexedRecord | Array[Byte] | String) => C => Ior[Chain[AvroFailure], IndexedRecord] =
    input => c => AvroCodec.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, f(c)))

  /** Replace the focused value with `a`, rebuilding the record around it — silent tier (pair with
    * [[place]] for Ior diagnostics).
    *
    * A trait MEMBER deliberately: without it, `.replace` on a record-carried optic resolves to the
    * generic Either-carrier extension, which routes through `from(Right(a)) = reverseGet(a)` and
    * reconstructs the focus STANDALONE — for any drilled optic that silently destroys every sibling
    * field. This member shadows the extension with the sibling-preserving placeImpl walk.
    */
  def replace(a: A): (IndexedRecord | Array[Byte] | String) => IndexedRecord = placeUnsafe(a)

  // NOTE: there is deliberately no bytes-in/bytes-out surface here. [[AvroPrism]] and
  // [[AvroTraversal]] ARE the byte-carried optics — `.modify` / `.replace` / `.getOption` /
  // `.foldMap` on them operate on `Array[Byte]` directly (locate + slice-decode + splice), so a
  // parse→record→re-encode tier would be a second way to do the same thing.

  // ---- *Unsafe (silent) escape hatches -------------------------------

  /** Silent counterpart to [[modify]] — input pass-through on any failure. */
  def modifyUnsafe(f: A => A): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => modifyImpl(AvroCodec.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[transform]]. */
  def transformUnsafe(
      f: IndexedRecord => IndexedRecord
  ): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => transformImpl(AvroCodec.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[place]]. */
  def placeUnsafe(a: A): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => placeImpl(AvroCodec.parseInputUnsafe(input, rootSchema), a)

  /** Silent counterpart to [[transfer]]. */
  def transferUnsafe[C](f: C => A): (IndexedRecord | Array[Byte] | String) => C => IndexedRecord =
    input => c => placeImpl(AvroCodec.parseInputUnsafe(input, rootSchema), f(c))

end AvroOpticOps

/** Per-element accumulator for [[AvroTraversal]] — folds an `Ior`-bearing element rewrite over a
  * `Vector[Any]` (the runtime element values of an Avro array) and lifts the (chain, vector) pair
  * to a single `Ior`. Mirrors `dev.constructive.eo.circe.JsonTraversalAccumulator` line-for-line.
  */
private[avro] object AvroTraversalAccumulator:

  /** Apply `readElement` to every element, accumulating the per-element `Ior`s into
    * `(Vector[A], Chain[AvroFailure])` and lifting at the end. `Ior.Right(a)` adds `a`,
    * `Ior.Both(c, a)` adds both, `Ior.Left(c)` drops the element and adds `c`.
    */
  def collectIor[A](
      arr: Vector[Any],
      readElement: Any => Ior[Chain[AvroFailure], A],
  ): Ior[Chain[AvroFailure], Vector[A]] =
    val (chain, result) =
      arr.foldLeft((Chain.empty[AvroFailure], Vector.empty[A])) {
        case ((chain, acc), elem) =>
          readElement(elem) match
            case Ior.Right(a)   => (chain, acc :+ a)
            case Ior.Left(c)    => (chain ++ c, acc)
            case Ior.Both(c, a) => (chain ++ c, acc :+ a)
      }
    if chain.isEmpty then Ior.Right(result) else Ior.Both(chain, result)

  /** Per-element rewrite with failure accumulation. Elements whose `elemUpdate` returns `Ior.Left`
    * are left unchanged in the output Vector; their failure(s) contribute to the accumulated chain.
    */
  def mapElementsIor(
      arr: Vector[Any],
      elemUpdate: Any => Ior[Chain[AvroFailure], IndexedRecord],
  ): (Vector[Any], Chain[AvroFailure]) =
    arr.foldLeft((Vector.empty[Any], Chain.empty[AvroFailure])) {
      case ((acc, chain), elem) =>
        elemUpdate(elem) match
          case Ior.Right(r)   => (acc :+ r, chain)
          case Ior.Both(c, r) => (acc :+ r, chain ++ c)
          case Ior.Left(c)    => (acc :+ elem, chain ++ c)
    }

end AvroTraversalAccumulator
