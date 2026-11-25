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
  * per-element after a prefix walk". Exposes the Ior-bearing surface (via `AvroOpticOps`): `modify`
  * / `transform` / `place` / `transfer` / `getAll` accumulate per-element failures into the chain
  * over `IndexedRecord | Array[Byte] | String`; parse and prefix-walk failures return `Ior.Left` —
  * nothing to iterate.
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
