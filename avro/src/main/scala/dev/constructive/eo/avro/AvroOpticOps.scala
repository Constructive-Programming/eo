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

  /** The reader schema used to decode `Array[Byte]` and `String` (Avro JSON wire format) inputs.
    * The carrier's constructor cached this off the user's `AvroCodec[Root]` (or accepted it as a
    * `given Schema` per OQ-avro-5); exposing it here lets [[AvroFailure.parseInputIor]] thread it
    * without re-summoning.
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
  ): (IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => transformIor(j, f))

  /** Replace the focused value with `a`. Same failure surface as [[modify]]. */
  def place(
      a: A
  ): (IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord] =
    input => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, a))

  /** Lift a `C => A` into a focus-replacer: `transfer(f)(record)(c)` decodes `c` to `A` via `f` and
    * writes it at the focused position. Same failure surface as [[modify]].
    */
  def transfer[C](
      f: C => A
  ): (IndexedRecord | Array[Byte] | String) => C => Ior[Chain[AvroFailure], IndexedRecord] =
    input => c => AvroFailure.parseInputIor(input, rootSchema).flatMap(j => placeIor(j, f(c)))

  // ---- *Unsafe (silent) escape hatches -------------------------------

  /** Silent counterpart to [[modify]] — input pass-through on any failure. */
  def modifyUnsafe(f: A => A): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => modifyImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[transform]]. */
  def transformUnsafe(
      f: IndexedRecord => IndexedRecord
  ): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => transformImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f)

  /** Silent counterpart to [[place]]. */
  def placeUnsafe(a: A): (IndexedRecord | Array[Byte] | String) => IndexedRecord =
    input => placeImpl(AvroFailure.parseInputUnsafe(input, rootSchema), a)

  /** Silent counterpart to [[transfer]]. */
  def transferUnsafe[C](f: C => A): (IndexedRecord | Array[Byte] | String) => C => IndexedRecord =
    input => c => placeImpl(AvroFailure.parseInputUnsafe(input, rootSchema), f(c))

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
