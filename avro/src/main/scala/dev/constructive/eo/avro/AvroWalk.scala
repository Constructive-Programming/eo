package dev.constructive.eo.avro

import scala.jdk.CollectionConverters.*

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

  /** One walk step. Index steps always fail strictly. Runtime dispatch: `IndexedRecord` → field,
    * `Map` → key (same `Field(name)` shape), `List` → index, `UnionBranch` → type-check passthrough
    * (apache-avro reader has already resolved the alternative).
    */
  def stepInto(
      step: PathStep,
      cur: Any,
      parents: Vector[AnyRef],
      policy: OnMissingField,
  ): Either[AvroFailure, State] =
    step match
      case PathStep.Field(name) =>
        cur match
          case rec: IndexedRecord =>
            val schema = rec.getSchema
            val field = schema.getField(name)
            if field == null then
              policy match
                case OnMissingField.Strict  => Left(AvroFailure.PathMissing(step))
                case OnMissingField.Lenient => Right((null, parents :+ rec))
            else Right((rec.get(field.pos), parents :+ rec))
          case map: java.util.Map[?, ?] =>
            val asMap = map.asInstanceOf[java.util.Map[Any, Any]]
            val direct = asMap.get(name)
            val viaUtf8 =
              if direct == null then asMap.get(new org.apache.avro.util.Utf8(name)) else direct
            viaUtf8 match
              case null =>
                policy match
                  case OnMissingField.Strict  => Left(AvroFailure.PathMissing(step))
                  case OnMissingField.Lenient => Right((null, parents :+ map.asInstanceOf[AnyRef]))
              case v => Right((v, parents :+ map.asInstanceOf[AnyRef]))
          case _ =>
            Left(AvroFailure.NotARecord(step))

      case PathStep.Index(idx) =>
        cur match
          case lst: java.util.List[?] =>
            val size = lst.size
            if idx < 0 || idx >= size then Left(AvroFailure.IndexOutOfRange(step, size))
            else Right((lst.get(idx), parents :+ lst.asInstanceOf[AnyRef]))
          case _ => Left(AvroFailure.NotAnArray(step))

      case PathStep.UnionBranch(branchName) =>
        cur match
          case null =>
            if branchName == "null" then Right((null, parents))
            else Left(AvroFailure.UnionResolutionFailed(List("null"), step))
          case other =>
            val actualName = unionBranchName(other)
            if actualName == branchName then Right((other, parents))
            else Left(AvroFailure.UnionResolutionFailed(List(actualName), step))

  /** Legacy entry — Vector-shaped parents. Preserved for tests; hot-path callers use
    * [[walkPathArr]].
    */
  def walkPath(
      record: IndexedRecord,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[AvroFailure, State] =
    walkPathArr(record, path, policy) match
      case Left(failure) => Left(failure)
      case Right(walked) =>
        val parentsVec =
          if walked.parentsLen == 0 then Vector.empty[AnyRef]
          else
            val b = Vector.newBuilder[AnyRef]
            b.sizeHint(walked.parentsLen)
            var i = 0
            while i < walked.parentsLen do
              b += walked.parents(i)
              i += 1
            b.result()
        Right((walked.cur, parentsVec))

  /** Hot-path walk — array-indexed `while` loop. Failure short-circuit uses an in-loop sentinel
    * (`failure: AvroFailure | Null`) rather than `return` (scalafix `DisableSyntax.return`).
    * UnionBranch failures resolve the full schema-declared alternative list inline.
    */
  def walkPathArr(
      record: IndexedRecord,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[AvroFailure, WalkRes] =
    val len = path.length
    if len == 0 then Right(new WalkRes(record, EmptyParents, 0))
    else
      val parents = new Array[AnyRef](len)
      var parentsLen = 0
      var cur: Any = record
      var failure: AvroFailure | Null = null
      var i = 0
      while i < len && failure == null do
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
                      failure = AvroFailure.PathMissing(step)
                    case OnMissingField.Lenient =>
                      parents(parentsLen) = rec
                      parentsLen += 1
                      cur = null
                else
                  parents(parentsLen) = rec
                  parentsLen += 1
                  cur = rec.get(field.pos)
              case map: java.util.Map[?, ?] =>
                val asMap = map.asInstanceOf[java.util.Map[Any, Any]]
                val direct = asMap.get(name)
                val viaUtf8 =
                  if direct == null then asMap.get(new org.apache.avro.util.Utf8(name)) else direct
                if viaUtf8 == null then
                  policy match
                    case OnMissingField.Strict =>
                      failure = AvroFailure.PathMissing(step)
                    case OnMissingField.Lenient =>
                      parents(parentsLen) = map.asInstanceOf[AnyRef]
                      parentsLen += 1
                      cur = null
                else
                  parents(parentsLen) = map.asInstanceOf[AnyRef]
                  parentsLen += 1
                  cur = viaUtf8
              case _ =>
                failure = AvroFailure.NotARecord(step)

          case PathStep.Index(idx) =>
            cur match
              case lst: java.util.List[?] =>
                val size = lst.size
                if idx < 0 || idx >= size then failure = AvroFailure.IndexOutOfRange(step, size)
                else
                  parents(parentsLen) = lst.asInstanceOf[AnyRef]
                  parentsLen += 1
                  cur = lst.get(idx)
              case _ =>
                failure = AvroFailure.NotAnArray(step)

          case PathStep.UnionBranch(branchName) =>
            cur match
              case null =>
                if branchName == "null" then ()
                else
                  failure = AvroFailure
                    .UnionResolutionFailed(unionBranchesAtArr(path, i, parents, parentsLen), step)
              case other =>
                val actualName = unionBranchName(other)
                if actualName == branchName then ()
                else
                  failure = AvroFailure
                    .UnionResolutionFailed(unionBranchesAtArr(path, i, parents, parentsLen), step)
        i += 1
      if failure == null then Right(new WalkRes(cur, parents, parentsLen))
      else Left(failure)

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
    var pIdx = parentsLen - 1
    var j = unionStepIdx - 1
    var result: List[String] | Null = null
    while j >= 0 && result == null do
      path(j) match
        case PathStep.Field(name) =>
          if pIdx >= 0 then
            parents(pIdx) match
              case rec: IndexedRecord =>
                val f = rec.getSchema.getField(name)
                if f != null && f.schema.getType == org.apache.avro.Schema.Type.UNION then
                  result = f.schema.getTypes.asScala.map(_.getFullName).toList
                else result = Nil
              case _ => result = Nil
          else result = Nil
        case PathStep.UnionBranch(_) =>
          // doesn't decrement pIdx (UnionBranch steps don't push parents)
          ()
        case _ =>
          pIdx -= 1
      j -= 1
    if result == null then Nil else result

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
    var i = parents.length - 1
    var child: Any = newLeaf
    while i >= 0 do
      child = rebuildStep(parents(i), path(i), child)
      i -= 1
    child

  /** Hot-path rebuild — folds parents backwards. UnionBranch steps that didn't push parents are
    * skipped (matches the legacy `parents.zip(path).foldRight` shape).
    */
  def rebuildPathArr(
      parents: Array[AnyRef],
      parentsLen: Int,
      path: Array[PathStep],
      newLeaf: Any,
  ): Any =
    var i = parentsLen - 1
    var child: Any = newLeaf
    while i >= 0 do
      child = rebuildStep(parents(i), path(i), child)
      i -= 1
    child

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
            val fields = schema.getFields
            val n = fields.size
            val targetPos = schema.getField(name).pos
            var i = 0
            while i < n do
              if i == targetPos then fresh.put(i, child.asInstanceOf[AnyRef])
              else fresh.put(i, rec.get(i))
              i += 1
            fresh
          case map: java.util.Map[?, ?] =>
            val asMap = map.asInstanceOf[java.util.Map[Any, Any]]
            val fresh = new java.util.LinkedHashMap[Any, Any](asMap)
            // Drop both string + Utf8 forms before re-inserting under the string key.
            fresh.remove(name)
            fresh.remove(new org.apache.avro.util.Utf8(name))
            fresh.put(name, child)
            fresh
          case other =>
            sys.error(s"AvroWalk.rebuildStep: cannot Field-splice into $other (step=$step)")
      case PathStep.Index(idx) =>
        parent match
          case lst: java.util.List[?] =>
            val asList = lst.asInstanceOf[java.util.List[Any]]
            val schema =
              lst match
                case ga: GenericData.Array[?] => ga.getSchema
                case _                        => null
            val fresh: java.util.List[Any] =
              if schema != null then new GenericData.Array[Any](asList.size, schema)
              else new java.util.ArrayList[Any](asList.size)
            var i = 0
            while i < asList.size do
              if i == idx then fresh.add(child)
              else fresh.add(asList.get(i))
              i += 1
            fresh
          case other =>
            sys.error(s"AvroWalk.rebuildStep: cannot Index-splice into $other (step=$step)")
      case PathStep.UnionBranch(_) =>
        // Union steps don't change the parent shape — the alternative IS the value directly.
        child

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
    var i = 0
    while i < n do
      val fname = fields.get(i).name
      updates.get(fname) match
        case Some(v) => fresh.put(i, v.asInstanceOf[AnyRef])
        case None    => fresh.put(i, parent.get(i))
      i += 1
    fresh

  /** Union-branch name for a runtime value. Schema-driven where possible; falls back to the raw
    * class name (the fallback is cosmetic — UnionBranch steps reach this only from a schemaful
    * parent).
    */
  private def unionBranchName(value: Any): String =
    value match
      case rec: IndexedRecord     => rec.getSchema.getFullName
      case _: CharSequence        => "string"
      case _: java.lang.Integer   => "int"
      case _: java.lang.Long      => "long"
      case _: java.lang.Float     => "float"
      case _: java.lang.Double    => "double"
      case _: java.lang.Boolean   => "boolean"
      case _: Array[Byte]         => "bytes"
      case _: java.util.List[?]   => "array"
      case _: java.util.Map[?, ?] => "map"
      case other                  => other.getClass.getName

end AvroWalk
