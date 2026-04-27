package dev.constructive.eo.avro

import scala.language.dynamics

import cats.data.{Chain, Ior}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, IndexedRecord}

/** Multi-focus counterpart to [[AvroPrism]]: walks the record from the root down to some array,
  * then applies the focus update to every element of that array.
  *
  * Mirrors `dev.constructive.eo.circe.JsonTraversal` line-for-line: a traversal is "an AvroPrism
  * applied per-element after a prefix walk". The class holds two pieces:
  *
  *   - `prefix: Array[PathStep]` — the root-to-array path, walked once;
  *   - `focus: AvroFocus[A]` — the per-element focus, applied to every element of the array.
  *
  * The Fields-vs-Leaf split lives entirely inside `focus` (per [[AvroFocus]]), so the four legacy
  * carriers — `AvroTraversal` over a single field, `AvroFieldsTraversal` over a NamedTuple —
  * collapse to one class. The compatibility alias [[AvroFieldsTraversal]] points back here.
  *
  * Two call-surface tiers (parallel to JsonTraversal, supplied via [[AvroOpticOps]]):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` / `getAll`
  *     accumulate per-element failures into the chain. Prefix-walk failures return `Ior.Left` —
  *     nothing to iterate.
  *   - '''`*Unsafe` (silent).''' Drops failures; preserves the silent forgiving semantics.
  *
  * '''Schema threading.''' `rootSchemaCached` parallels [[AvroPrism]]'s field by the same name —
  * needed to decode `Array[Byte]` inputs at the dual-input boundary. The `.each` macro on
  * [[AvroPrism]] threads the prism's `rootSchemaCached` straight into the new traversal.
  */
final class AvroTraversal[A] private[avro] (
    private[avro] val prefix: Array[PathStep],
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends AvroOpticOps[A],
      Dynamic:

  override protected def rootSchema: Schema = rootSchemaCached

  /** Compatibility shim — historically the `suffix: Array[PathStep]` was a bare `val` on the
    * traversal. With the focus unification this shape is internal to a Leaf focus; the read-side
    * accessor is preserved for the macro extensions that route through `widenSuffix*`.
    */
  private[avro] def suffix: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  private[avro] def codec: AvroCodec[A] = focus.codec

  // ---- Dynamic field sugar -----------------------------------------

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Read surface (multi-focus specific) --------------------------

  def getAll(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], Vector[A]] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(getAllIor)

  inline def getAllUnsafe(input: IndexedRecord | Array[Byte] | String): Vector[A] =
    val record = AvroFailure.parseInputUnsafe(input, rootSchemaCached)
    walkPrefixOpt(record).fold(Vector.empty[A])(_.flatMap {
      case rec: IndexedRecord => focus.readImpl(rec)
      case _                  => None
    })

  // ---- Path extension (used by field / at / selectDynamic macros) --

  private[avro] def widenSuffix[B](
      step: String
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Field(step))

  private[avro] def widenSuffixIndex[B](
      i: Int
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Index(i))

  private def widenSuffixStep[B](
      step: PathStep
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    val newSuffix = new Array[PathStep](suffix.length + 1)
    System.arraycopy(suffix, 0, newSuffix, 0, suffix.length)
    newSuffix(suffix.length) = step
    new AvroTraversal[B](prefix, new AvroFocus.Leaf[B](newSuffix, codecB), rootSchemaCached)

  /** Hand off the current (prefix, suffix) as a multi-field [[AvroTraversal]] whose focus is an
    * [[AvroFocus.Fields]] enumerating `fieldNames`. Historically returned `AvroFieldsTraversal[B]`;
    * the alias is preserved.
    */
  private[avro] def toFieldsTraversal[B](
      fieldNames: Array[String]
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    new AvroTraversal[B](
      prefix,
      new AvroFocus.Fields[B](suffix, fieldNames, codecB),
      rootSchemaCached,
    )

  // ---- Prefix walks -------------------------------------------------

  /** Walk the prefix and project the terminal as a `java.util.List` of element values. `None` on
    * any walk failure or on a non-array terminal — the silent surface uses this directly.
    */
  private def walkPrefixOpt(record: IndexedRecord): Option[Vector[Any]] =
    AvroWalk.walkPath(record, prefix).toOption.flatMap {
      case (cur, _) =>
        cur match
          case lst: java.util.List[?] => Some(toVector(lst))
          case _                      => None
    }

  /** Walk the prefix with structured failure reporting; success projects the terminal as a
    * `java.util.List`-as-Vector plus the parents collected during the walk.
    */
  private def walkPrefixIor(
      record: IndexedRecord
  ): Either[Ior.Left[Chain[AvroFailure]], (java.util.List[?], Vector[Any], Vector[AnyRef])] =
    AvroWalk
      .walkPath(record, prefix)
      .left
      .map(failure => Ior.Left(Chain.one(failure)))
      .flatMap {
        case (cur, parents) =>
          cur match
            case lst: java.util.List[?] =>
              Right((lst, toVector(lst), parents))
            case _ =>
              Left(Ior.Left(Chain.one(AvroFailure.NotAnArray(AvroWalk.terminalOf(prefix)))))
      }

  private def toVector(lst: java.util.List[?]): Vector[Any] =
    val b = Vector.newBuilder[Any]
    b.sizeHint(lst.size)
    var i = 0
    while i < lst.size do
      b += lst.get(i)
      i += 1
    b.result()

  // ---- *Unsafe surface — delegates to focus.modifyImpl etc. --------

  override protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
    mapAtPrefix(record)(elem => focusModifyOnElem(elem, f))

  override protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord =
    mapAtPrefix(record)(elem => focusTransformOnElem(elem, f))

  override protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
    mapAtPrefix(record)(elem => focusPlaceOnElem(elem, a))

  /** Walk the prefix, replace the focused array by mapping every element through `elemUpdate`. On
    * prefix miss the input is returned unchanged. Element values that aren't [[IndexedRecord]] are
    * left unchanged (the focus's per-record hooks expect a record to walk into).
    */
  private def mapAtPrefix(record: IndexedRecord)(elemUpdate: Any => Any): IndexedRecord =
    AvroWalk.walkPath(record, prefix).toOption match
      case None                 => record
      case Some((cur, parents)) =>
        cur match
          case lst: java.util.List[?] =>
            val newList = replaceArrayContents(lst, toVector(lst).map(elemUpdate))
            AvroWalk.rebuildPath(parents, prefix, newList).asInstanceOf[IndexedRecord]
          case _ => record

  // The focus's per-record hooks expect an IndexedRecord. Per-element values inside an Avro array
  // are runtime `Any` — when the focus is a Leaf with a non-empty suffix or a Fields focus, the
  // element MUST be an IndexedRecord (the schema guarantees it for record-shaped element types).
  // For the rare leaf-shaped element types (e.g. an array<string>) the silent surface preserves
  // the element unchanged — the codec round-trip happens in the Ior path below.

  private def focusModifyOnElem(elem: Any, f: A => A): Any =
    elem match
      case rec: IndexedRecord => focus.modifyImpl(rec, f)
      case _                  => elem

  private def focusTransformOnElem(elem: Any, f: IndexedRecord => IndexedRecord): Any =
    elem match
      case rec: IndexedRecord => focus.transformImpl(rec, f)
      case _                  => elem

  private def focusPlaceOnElem(elem: Any, a: A): Any =
    elem match
      case rec: IndexedRecord => focus.placeImpl(rec, a)
      case _                  => elem

  // ---- Default Ior surface — delegates to focus.modifyIor etc. -----

  override protected def modifyIor(
      record: IndexedRecord,
      f: A => A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(elem => focusModifyIor(elem, f))

  override protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(elem => focusTransformIor(elem, f))

  override protected def placeIor(
      record: IndexedRecord,
      a: A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    iorMapElements(record)(elem => focusPlaceIor(elem, a))

  /** Per-element Ior dispatch — coerces `elem` to an [[IndexedRecord]] before delegating to the
    * focus, surfacing `NotARecord` for non-record elements (a basket entry that's a primitive).
    */
  private def focusModifyIor(elem: Any, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
    elem match
      case rec: IndexedRecord => focus.modifyIor(rec, f)
      case _                  =>
        Ior.Left(Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(prefix))))

  private def focusTransformIor(
      elem: Any,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    elem match
      case rec: IndexedRecord => focus.transformIor(rec, f)
      case _                  =>
        Ior.Left(Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(prefix))))

  private def focusPlaceIor(elem: Any, a: A): Ior[Chain[AvroFailure], IndexedRecord] =
    elem match
      case rec: IndexedRecord => focus.placeIor(rec, a)
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
          AvroWalk.rebuildPath(parents, prefix, newList).asInstanceOf[IndexedRecord]
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
      orig: java.util.List[?],
      newElems: Vector[Any],
  ): java.util.List[Any] =
    val schema = orig match
      case ga: GenericData.Array[?] => ga.getSchema
      case _                        => null
    val fresh: java.util.List[Any] =
      if schema != null then new GenericData.Array[Any](newElems.size, schema)
      else new java.util.ArrayList[Any](newElems.size)
    newElems.foreach(fresh.add(_))
    fresh

end AvroTraversal

object AvroTraversal:

  /** `.field(_.x)` sugar — extend the suffix by a named field. */
  extension [A](t: AvroTraversal[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: AvroCodec[B]): AvroTraversal[B] =
      ${ AvroPrismMacro.fieldTraversalImpl[A, B]('t, 'selector, 'codecB) }

  /** `.at(i)` sugar — extend the suffix by an array index. */
  extension [A](t: AvroTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ AvroPrismMacro.atTraversalImpl[A]('t, 'i) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple per element. */
  extension [A](t: AvroTraversal[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ AvroPrismMacro.fieldsTraversalImpl[A]('t, 'selectors) }

end AvroTraversal
