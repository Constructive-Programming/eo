package dev.constructive.eo.avro

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

import dev.constructive.eo.widenRight
import java.util.{ArrayList, LinkedHashMap, List as JList, Map as JMap}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, IndexedRecord}

/** Shared internal helpers for fold-based Avro walks used by [[AvroPrism]] and [[AvroTraversal]].
  * Mirrors `circe.JsonWalk` with parent-type dispatch generalised across `IndexedRecord` /
  * `java.util.List` / `java.util.Map` / union alternatives.
  *
  * Avro's runtime string type is `Utf8 <: CharSequence`; the walker uses `instanceof CharSequence`.
  *
  * Two entry points: [[walkPath]] — legacy `(Any, Vector[AnyRef])` shape; [[walkPathArr]] —
  * hot-path entry storing parents in a pre-allocated `Array[AnyRef]`. The latter eliminates
  * `Right(...)` boxing, `Vector.:+`, and `Vector` materialisation; [[rebuildPathArr]] folds it
  * backwards. The four `AvroFocus` hot paths (`modifyImpl` / `transformImpl` / `placeImpl` /
  * `readImpl`) plus their Ior siblings use `walkPathArr`.
  */
private[avro] object AvroWalk:

  /** Walked-cursor state: current focus (Avro runtime values span `IndexedRecord` / `List` / `Map`
    * / `Utf8` / primitives), paired with parents Vector. Root-to-leaf ordered.
    */
  type State = (Any, Vector[AnyRef])

  /** Hot-path walk result. `parents` is sized to `path.length`; `parentsLen` counts filled slots
    * (`UnionBranch` steps don't push parents). Held through `AvroFocus` hooks and fed straight into
    * [[rebuildPathArr]].
    */
  final class WalkRes(val cur: Any, val parents: Array[AnyRef], val parentsLen: Int)

  /** Missing-field policy. */
  enum OnMissingField:

    /** Missing → `Left(PathMissing(step))`. Default for read / decoded-modify. */
    case Strict

    /** Missing → `Right((null, parents :+ parent))`. Used by Traversal raw-transform / place where
      * missing elements are synthesised as `null` leaves.
      */
    case Lenient

  /** Legacy entry — Vector-shaped parents. Preserved for tests; hot-path callers use
    * [[walkPathArr]].
    */
  def walkPath(
      record: IndexedRecord,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[AvroFailure, State] =
    walkPathArr(record, path, policy) match
      case l @ Left(_)   => l.widenRight
      case Right(walked) =>
        val parentsVec =
          if walked.parentsLen == 0 then Vector.empty[AnyRef]
          else
            val b = Vector.newBuilder[AnyRef]
            b.sizeHint(walked.parentsLen)
            @tailrec def loop(i: Int): Unit =
              if i < walked.parentsLen then
                b += walked.parents(i)
                loop(i + 1)
            loop(0)
            b.result()
        Right((walked.cur, parentsVec))

  /** Hot-path walk — array-indexed `@tailrec` loop threading `(i, parentsLen, cur)`. Failure
    * short-circuits by returning `Left` in place (no sentinel, no `return`). UnionBranch failures
    * resolve the full schema-declared alternative list inline.
    */
  def walkPathArr(
      record: IndexedRecord,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[AvroFailure, WalkRes] =
    if path.length == 0 then Right(new WalkRes(record, EmptyParents, 0))
    else walkArrLoop(path, new Array[AnyRef](path.length), policy, 0, 0, record)

  /** Step machine for [[walkPathArr]] — advances `(i, parentsLen, cur)` through `path`, filling
    * `parents` in place. Failure short-circuits by returning `Left`; UnionBranch failures resolve
    * the schema-declared alternative list inline.
    */
  @tailrec private def walkArrLoop(
      path: Array[PathStep],
      parents: Array[AnyRef],
      policy: OnMissingField,
      i: Int,
      parentsLen: Int,
      cur: Any,
  ): Either[AvroFailure, WalkRes] =
    if i >= path.length then Right(new WalkRes(cur, parents, parentsLen))
    else
      val step = path(i)
      step match
        case PathStep.Field(name) =>
          cur match
            case rec: IndexedRecord =>
              val schema = rec.getSchema
              val field = schema.getField(name)
              if field == null then
                policy match
                  case OnMissingField.Strict =>
                    Left(AvroFailure.PathMissing(step))
                  case OnMissingField.Lenient =>
                    parents(parentsLen) = rec
                    walkArrLoop(path, parents, policy, i + 1, parentsLen + 1, null)
              else
                parents(parentsLen) = rec
                walkArrLoop(path, parents, policy, i + 1, parentsLen + 1, rec.get(field.pos))
            case map: JMap[?, ?] =>
              val asMap = map.asInstanceOf[JMap[Any, Any]]
              val direct = asMap.get(name)
              val viaUtf8 =
                if direct == null then asMap.get(new org.apache.avro.util.Utf8(name))
                else direct
              if viaUtf8 == null then
                policy match
                  case OnMissingField.Strict =>
                    Left(AvroFailure.PathMissing(step))
                  case OnMissingField.Lenient =>
                    parents(parentsLen) = map.asInstanceOf[AnyRef]
                    walkArrLoop(path, parents, policy, i + 1, parentsLen + 1, null)
              else
                parents(parentsLen) = map.asInstanceOf[AnyRef]
                walkArrLoop(path, parents, policy, i + 1, parentsLen + 1, viaUtf8)
            case _ =>
              Left(AvroFailure.NotARecord(step))

        case PathStep.Index(idx) =>
          cur match
            case lst: JList[?] =>
              val size = lst.size
              if idx < 0 || idx >= size then Left(AvroFailure.IndexOutOfRange(step, size))
              else
                parents(parentsLen) = lst.asInstanceOf[AnyRef]
                walkArrLoop(path, parents, policy, i + 1, parentsLen + 1, lst.get(idx))
            case _ =>
              Left(AvroFailure.NotAnArray(step))

        case PathStep.UnionBranch(branchName) =>
          cur match
            case null =>
              if branchName == "null" then
                walkArrLoop(path, parents, policy, i + 1, parentsLen, null)
              else
                Left(
                  AvroFailure
                    .UnionResolutionFailed(
                      unionBranchesAtArr(path, i, parents, parentsLen),
                      step,
                    )
                )
            case other =>
              val actualName = unionBranchName(other)
              if actualName == branchName then
                walkArrLoop(path, parents, policy, i + 1, parentsLen, cur)
              else
                Left(
                  AvroFailure
                    .UnionResolutionFailed(
                      unionBranchesAtArr(path, i, parents, parentsLen),
                      step,
                    )
                )

  private val EmptyParents: Array[AnyRef] = new Array[AnyRef](0)

  /** Recover the union schema's branch list. Walks backwards from `unionStepIdx - 1` to the most
    * recent `Field` step; the parent at that position carries the union field. `pIdx` cursor
    * decrements only on non-UnionBranch steps to stay aligned with parents-array layout. `Nil` if
    * no such step is found.
    */
  private def unionBranchesAtArr(
      path: Array[PathStep],
      unionStepIdx: Int,
      parents: Array[AnyRef],
      parentsLen: Int,
  ): List[String] =
    @tailrec def loop(j: Int, pIdx: Int): List[String] =
      if j < 0 then Nil
      else
        path(j) match
          case PathStep.Field(name) =>
            if pIdx >= 0 then
              parents(pIdx) match
                case rec: IndexedRecord =>
                  val f = rec.getSchema.getField(name)
                  if f != null && f.schema.getType == org.apache.avro.Schema.Type.UNION then
                    f.schema.getTypes.asScala.map(_.getFullName).toList
                  else Nil
                case _ => Nil
            else Nil
          case PathStep.UnionBranch(_) =>
            // doesn't decrement pIdx (UnionBranch steps don't push parents)
            loop(j - 1, pIdx)
          case _ =>
            loop(j - 1, pIdx - 1)
    loop(unionStepIdx - 1, parentsLen - 1)

  /** Terminal step of `path` (sentinel `PathStep.Field("")` when empty). Used by non-step-shaped
    * failures.
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)

  /** Rebuild from leaf to root using the parents collected during a walk. Always allocates fresh
    * records (avro's `put` mutates in place); cost is one allocation per parent step. Legacy
    * Vector-shaped entry; hot-path callers use [[rebuildPathArr]].
    */
  def rebuildPath(
      parents: Vector[AnyRef],
      path: Array[PathStep],
      newLeaf: Any,
  ): Any =
    @tailrec def loop(i: Int, child: Any): Any =
      if i < 0 then child
      else loop(i - 1, rebuildStep(parents(i), path(i), child))
    loop(parents.length - 1, newLeaf)

  /** Hot-path rebuild — folds parents backwards. UnionBranch steps that didn't push parents are
    * skipped (matches the legacy `parents.zip(path).foldRight` shape).
    */
  def rebuildPathArr(
      parents: Array[AnyRef],
      parentsLen: Int,
      path: Array[PathStep],
      newLeaf: Any,
  ): Any =
    @tailrec def loop(i: Int, child: Any): Any =
      if i < 0 then child
      else loop(i - 1, rebuildStep(parents(i), path(i), child))
    loop(parentsLen - 1, newLeaf)

  /** Splice `child` into `parent` at `step`. Dispatch:
    *   - `Field` on `IndexedRecord` — fresh record with `put` at the matching slot.
    *   - `Field` on `Map` — fresh `LinkedHashMap` with the entry replaced.
    *   - `Index` on `List` — fresh `GenericData.Array` with the entry replaced.
    *   - `UnionBranch` — passthrough (union value is the alternative directly).
    */
  def rebuildStep(
      parent: AnyRef,
      step: PathStep,
      child: Any,
  ): Any =
    step match
      case PathStep.Field(name) =>
        parent match
          case rec: IndexedRecord =>
            val schema = rec.getSchema
            val fresh = new GenericData.Record(schema)
            // Iterate by index — avoids the asScala iterator alloc.
            putRecordSlots(fresh, rec, child, schema.getField(name).pos, 0, schema.getFields.size)
            fresh
          case map: JMap[?, ?] =>
            val asMap = map.asInstanceOf[JMap[Any, Any]]
            val fresh = new LinkedHashMap[Any, Any](asMap)
            // Drop both string + Utf8 forms before re-inserting under the string key.
            fresh.remove(name)
            fresh.remove(new org.apache.avro.util.Utf8(name))
            fresh.put(name, child)
            fresh
          case other =>
            sys.error(s"AvroWalk.rebuildStep: cannot Field-splice into $other (step=$step)")
      case PathStep.Index(idx) =>
        parent match
          case lst: JList[?] =>
            val asList = lst.asInstanceOf[JList[Any]]
            val schema =
              lst match
                case ga: GenericData.Array[?] => ga.getSchema
                case _                        => null
            val fresh: JList[Any] =
              if schema != null then new GenericData.Array[Any](asList.size, schema)
              else new ArrayList[Any](asList.size)
            addListSlots(fresh, asList, child, idx, 0, asList.size)
            fresh
          case other =>
            sys.error(s"AvroWalk.rebuildStep: cannot Index-splice into $other (step=$step)")
      case PathStep.UnionBranch(_) =>
        // Union steps don't change the parent shape — the alternative IS the value directly.
        child

  /** Copy every slot of `rec` into `fresh` for indices `[i, n)`, substituting `child` at
    * `targetPos` — the record splice loop behind [[rebuildStep]].
    */
  @tailrec private def putRecordSlots(
      fresh: GenericData.Record,
      rec: IndexedRecord,
      child: Any,
      targetPos: Int,
      i: Int,
      n: Int,
  ): Unit =
    if i < n then
      if i == targetPos then fresh.put(i, child.asInstanceOf[AnyRef])
      else fresh.put(i, rec.get(i))
      putRecordSlots(fresh, rec, child, targetPos, i + 1, n)

  /** Copy every element of `src` into `fresh` for indices `[i, n)`, substituting `child` at `idx` —
    * the list splice loop behind [[rebuildStep]].
    */
  @tailrec private def addListSlots(
      fresh: JList[Any],
      src: JList[Any],
      child: Any,
      idx: Int,
      i: Int,
      n: Int,
  ): Unit =
    if i < n then
      if i == idx then fresh.add(child)
      else fresh.add(src.get(i))
      addListSlots(fresh, src, child, idx, i + 1, n)

  /** Allocate a fresh `IndexedRecord` under `parent.getSchema`, replacing slots whose name appears
    * in `updates`. Used by [[AvroFocus.Fields]]'s multi-field overlay paths (counterpart to
    * `JsonFieldsPrism`'s overlay helpers).
    */
  def replaceRecordFields(
      parent: IndexedRecord,
      updates: Map[String, Any],
  ): IndexedRecord =
    val schema = parent.getSchema
    val fresh = new GenericData.Record(schema)
    val fields = schema.getFields
    val n = fields.size
    @tailrec def loop(i: Int): Unit =
      if i < n then
        val fname = fields.get(i).name
        updates.get(fname) match
          case Some(v) => fresh.put(i, v.asInstanceOf[AnyRef])
          case None    => fresh.put(i, parent.get(i))
        loop(i + 1)
    loop(0)
    fresh

  // ---- Schema-name resolution (issue #35) ----------------------------
  //
  // Field navigation must honour the schema's field name, not the raw Scala field name: a codec
  // built with a name transform (kindlings snake/kebab/custom) or vulcan overrides emits schema
  // fields whose names differ from the case-class fields. The `.field(_.x)` macros know `x`'s
  // DECLARATION index; these helpers walk the cached schema at prism-construction time and read
  // back the actual schema field name at that position, so the stored PathStep.Field carries the
  // schema name and the (unchanged) runtime walkers hit it. Resolution is construction-time only —
  // zero per-operation cost.

  /** Walk `root` along `steps` and return the terminal schema, or a diagnostic. The Field steps
    * carry already-resolved schema names, so `getField` hits; UnionBranch unwraps to the branch,
    * Index descends into the element/value type. Mirrors the cursor's schema descent.
    */
  def schemaAt(root: Schema, steps: Array[PathStep]): Either[String, Schema] =
    @tailrec def loop(i: Int, schema: Schema): Either[String, Schema] =
      if i >= steps.length then Right(schema)
      else
        steps(i) match
          case PathStep.Field(name) =>
            if schema.getType != Schema.Type.RECORD then
              Left(s"step $i: expected a record but found ${schema.getType} (field '$name')")
            else
              val f = schema.getField(name)
              if f == null then Left(s"step $i: schema record has no field '$name'")
              else loop(i + 1, f.schema)
          case PathStep.UnionBranch(branchName) =>
            if schema.getType != Schema.Type.UNION then
              Left(s"step $i: expected a union but found ${schema.getType}")
            else
              val types = schema.getTypes
              @tailrec def find(j: Int): Schema | Null =
                if j >= types.size then null
                else if types.get(j).getFullName == branchName then types.get(j)
                else find(j + 1)
              find(0) match
                case null      => Left(s"step $i: union has no branch '$branchName'")
                case b: Schema => loop(i + 1, b)
          case PathStep.Index(_) =>
            schema.getType match
              case Schema.Type.ARRAY => loop(i + 1, schema.getElementType)
              case Schema.Type.MAP   => loop(i + 1, schema.getValueType)
              case other             => Left(s"step $i: expected an array/map but found $other")
    loop(0, root)

  /** The schema field name for the case-class field at declaration index `declIdx` inside the
    * record reached by [[schemaAt]]`(root, parentPath)`. Position resolution: the i-th case field
    * maps to the i-th schema field, honouring any codec name transform (identity / snake / kebab /
    * custom / vulcan overrides), because the name is read back OUT of the schema.
    *
    * `declIdx < 0` (index undeterminable — a non-case-class parent such as a NamedTuple) falls back
    * to the literal `scalaName`, preserving the pre-#35 behaviour for those parents. Every other
    * mismatch (path miss, non-record parent, index past the schema's field count) throws LOUDLY —
    * never a silent miss — with the candidate names and a pointer at the `.fieldNamed` escape
    * hatch.
    */
  def resolveFieldName(
      root: Schema,
      parentPath: Array[PathStep],
      scalaName: String,
      declIdx: Int,
      who: String,
  ): String =
    if declIdx < 0 then scalaName
    else
      schemaAt(root, parentPath) match
        case Left(err) =>
          throw new IllegalArgumentException(
            s"$who('$scalaName'): cannot resolve the schema field — $err."
              + " Navigate by explicit schema name with .fieldNamed(\"<name>\")."
          )
        case Right(parent) =>
          fieldNameAt(parent, scalaName, declIdx, who)

  /** Schema field name at declaration index `declIdx` of `record` (which must be a RECORD). Split
    * out so [[AvroTraversal]] can resolve against an element record it computed itself.
    */
  def fieldNameAt(record: Schema, scalaName: String, declIdx: Int, who: String): String =
    if record.getType != Schema.Type.RECORD then
      throw new IllegalArgumentException(
        s"$who('$scalaName'): parent focus is a ${record.getType}, not a record."
          + " Descend a nullable/union field with .union[Branch] first."
      )
    else
      val fields = record.getFields
      if declIdx >= fields.size then
        throw new IllegalArgumentException(
          s"$who('$scalaName'): case field #$declIdx has no matching schema field —"
            + s" record '${record.getFullName}' has ${fields.size} field(s): "
            + fields.asScala.map(_.name).mkString(", ")
            + ". For a reordered hand-written codec, navigate by explicit schema name with"
            + " .fieldNamed(\"<name>\")."
        )
      else fields.get(declIdx).name

  /** Union-branch name for a runtime value. Schema-driven where possible; falls back to the raw
    * class name (the fallback is cosmetic — UnionBranch steps reach this only from a schemaful
    * parent).
    */
  private def unionBranchName(value: Any): String =
    value match
      case rec: IndexedRecord   => rec.getSchema.getFullName
      case _: CharSequence      => "string"
      case _: java.lang.Integer => "int"
      case _: java.lang.Long    => "long"
      case _: java.lang.Float   => "float"
      case _: java.lang.Double  => "double"
      case _: java.lang.Boolean => "boolean"
      case _: Array[Byte]       => "bytes"
      case _: JList[?]          => "array"
      case _: JMap[?, ?]        => "map"
      case other                => other.getClass.getName

end AvroWalk
