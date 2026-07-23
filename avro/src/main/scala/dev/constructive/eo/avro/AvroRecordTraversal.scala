package dev.constructive.eo.avro

import scala.annotation.tailrec

import cats.data.{Chain, Ior}
import java.util.{ArrayList, List as JList}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, IndexedRecord}

/** The `org.apache.avro.generic.IndexedRecord`-carried face of an [[AvroTraversal]] — reachable
  * ONLY through [[AvroTraversal.record]], never constructed directly. Walks the record from the
  * root down to some array, then applies the focus update to every element of that array.
  *
  * Mirrors `dev.constructive.eo.circe.JsonTraversal` line-for-line: a traversal is "a prism applied
  * per-element after a prefix walk". Two call-surface tiers (via `AvroOpticOps`):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` / `getAll`
  *     accumulate per-element failures into the chain. Prefix-walk failures return `Ior.Left` —
  *     nothing to iterate.
  *   - '''`*Unsafe` (silent).''' Drops failures; preserves the silent forgiving semantics.
  *
  * Drilling (`.field` / `.at` / `.fields` / Dynamic selection) lives on [[AvroTraversal]] alone —
  * drill there, then flip with `.record` at the end. One drilling mechanism, two carriers.
  */
final class AvroRecordTraversal[A] private[avro] (
    private[avro] val prefix: Array[PathStep],
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends AvroOpticOps[A]:

  override protected def rootSchema: Schema = rootSchemaCached

  // ---- Read surface (multi-focus specific) --------------------------

  /** Decode the focus of every element of the focused array. Parse / prefix-walk failures return
    * `Ior.Left`; per-element failures accumulate into the chain while surviving elements land in
    * the Vector (`Ior.Both`).
    */
  def getAll(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], Vector[A]] =
    AvroCodec.parseInputIor(input, rootSchemaCached).flatMap(getAllIor)

  /** Silent counterpart to [[getAll]] — refusing elements are dropped; a whole-walk failure yields
    * `Vector.empty`.
    */
  inline def getAllUnsafe(input: IndexedRecord | Array[Byte] | String): Vector[A] =
    val record = AvroCodec.parseInputUnsafe(input, rootSchemaCached)
    walkPrefixOpt(record).fold(Vector.empty[A])(_.flatMap {
      case rec: IndexedRecord => focus.readImpl(rec)
      case _                  => None
    })

  // ---- Prefix walks -------------------------------------------------

  /** Walk the prefix and project the terminal as a `java.util.List` of element values. `None` on
    * any walk failure or on a non-array terminal — the silent surface uses this directly.
    */
  private def walkPrefixOpt(record: IndexedRecord): Option[Vector[Any]] =
    AvroWalk.walkPath(record, prefix).toOption.flatMap {
      case (cur, _) =>
        cur match
          case lst: JList[?] => Some(toVector(lst))
          case _             => None
    }

  /** Walk the prefix with structured failure reporting; success projects the terminal as a
    * `java.util.List`-as-Vector plus the parents collected during the walk.
    */
  private def walkPrefixIor(
      record: IndexedRecord
  ): Either[Ior.Left[Chain[AvroFailure]], (JList[?], Vector[Any], Vector[Any])] =
    AvroWalk
      .walkPath(record, prefix)
      .left
      .map(failure => Ior.Left(Chain.one(failure)))
      .flatMap {
        case (cur, parents) =>
          cur match
            case lst: JList[?] =>
              Right((lst, toVector(lst), parents))
            case _ =>
              Left(Ior.Left(Chain.one(AvroFailure.NotAnArray(AvroWalk.terminalOf(prefix)))))
      }

  private def toVector(lst: JList[?]): Vector[Any] =
    val b = Vector.newBuilder[Any]
    b.sizeHint(lst.size)
    @tailrec def loop(i: Int): Unit =
      if i < lst.size then
        b += lst.get(i)
        loop(i + 1)
    loop(0)
    b.result()

  // ---- *Unsafe surface — delegates to focus.modifyImpl etc. --------

  override protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
    mapAtPrefix(record)(onRec(_)(focus.modifyImpl(_, f)))

  override protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord =
    mapAtPrefix(record)(onRec(_)(focus.transformImpl(_, f)))

  override protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
    mapAtPrefix(record)(onRec(_)(focus.placeImpl(_, a)))

  /** Walk the prefix, replace the focused array by mapping every element through `elemUpdate`. On
    * prefix miss the input is returned unchanged. Element values that aren't [[IndexedRecord]] are
    * left unchanged (the focus's per-record hooks expect a record to walk into).
    */
  private def mapAtPrefix(record: IndexedRecord)(elemUpdate: Any => Any): IndexedRecord =
    AvroWalk.walkPath(record, prefix).toOption match
      case None                 => record
      case Some((cur, parents)) =>
        cur match
          case lst: JList[?] =>
            val newList = replaceArrayContents(lst, toVector(lst).map(elemUpdate))
            AvroWalk.rebuildRecord(parents, prefix, newList)
          case _ => record

  // The focus's per-record hooks expect an IndexedRecord. Per-element values inside an Avro array
  // are runtime `Any` — when the focus is a Leaf with a non-empty suffix or a Fields focus, the
  // element MUST be an IndexedRecord (the schema guarantees it for record-shaped element types).
  // For the rare leaf-shaped element types (e.g. an array<string>) the silent surface preserves
  // the element unchanged — the codec round-trip happens in the Ior path below.

  private def onRec(elem: Any)(f: IndexedRecord => Any): Any =
    elem match
      case rec: IndexedRecord => f(rec)
      case _                  => elem

  // ---- Default Ior surface — delegates to focus.modifyIor etc. -----

  override protected def modifyIor(
      record: IndexedRecord,
      f: A => A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(onRecIor(_)(focus.modifyIor(_, f)))

  override protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(onRecIor(_)(focus.transformIor(_, f)))

  override protected def placeIor(
      record: IndexedRecord,
      a: A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(onRecIor(_)(focus.placeIor(_, a)))

  /** Per-element Ior dispatch — coerces `elem` to an [[IndexedRecord]] before delegating to the
    * focus, surfacing `NotARecord` for non-record elements (a basket entry that's a primitive).
    */
  private def onRecIor(elem: Any)(
      f: IndexedRecord => Ior[Chain[AvroFailure], IndexedRecord]
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    elem match
      case rec: IndexedRecord => f(rec)
      case _                  =>
        Ior.Left(Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(prefix))))

  /** Shared backbone for the three Ior-bearing modify/transform/place surfaces: walk the prefix,
    * map each element through `elemUpdate`, lift the (chain, vector) pair to a single Ior, rebuild
    * the prefix.
    */
  private def iorMapElements(
      record: IndexedRecord
  )(
      elemUpdate: Any => Ior[Chain[AvroFailure], IndexedRecord]
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    walkPrefixIor(record) match
      case Left(l)                         => l
      case Right((origList, arr, parents)) =>
        val (newArr, chain) = AvroTraversalAccumulator.mapElementsIor(arr, elemUpdate)
        val newList = replaceArrayContents(origList, newArr)
        val newRecord =
          AvroWalk.rebuildRecord(parents, prefix, newList)
        if chain.isEmpty then Ior.Right(newRecord) else Ior.Both(chain, newRecord)

  private def getAllIor(record: IndexedRecord): Ior[Chain[AvroFailure], Vector[A]] =
    walkPrefixIor(record) match
      case Left(l)            => l
      case Right((_, arr, _)) =>
        AvroTraversalAccumulator.collectIor(
          arr,
          {
            case rec: IndexedRecord => focus.readIor(rec)
            case _                  =>
              Ior.Left(Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(prefix))))
          },
        )

  // ---- Array allocation ---------------------------------------------

  /** Allocate a fresh array container holding `newElems`, preserving the original list's runtime
    * shape: a [[GenericData.Array]] under the original schema if available, otherwise a plain
    * `ArrayList`. Counterpart to circe's `Json.fromValues(...)` — the Avro side has to choose
    * between the schema-aware container (which round-trips through apache-avro's writer) and the
    * plain `java.util.List` (which doesn't but can't be created from a schema we don't have).
    *
    * Kept private to the traversal — `AvroWalk.rebuildStep` already handles single-Index splicing
    * with the same allocation choice; the traversal replaces the WHOLE list at once so it does the
    * allocation directly here.
    */
  private def replaceArrayContents(
      orig: JList[?],
      newElems: Vector[Any],
  ): JList[Any] =
    val schema = orig match
      case ga: GenericData.Array[?] => ga.getSchema
      case _                        => null
    val fresh: JList[Any] =
      if schema != null then new GenericData.Array[Any](newElems.size, schema)
      else new ArrayList[Any](newElems.size)
    newElems.foreach(fresh.add(_))
    fresh

end AvroRecordTraversal
