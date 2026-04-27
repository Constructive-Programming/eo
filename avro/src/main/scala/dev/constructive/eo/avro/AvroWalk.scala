package dev.constructive.eo.avro

import scala.jdk.CollectionConverters.*

import org.apache.avro.generic.{GenericData, IndexedRecord}

/** Shared internal helpers for the fold-based Avro walks used by [[AvroPrism]] and (in Unit 6+) its
  * traversal siblings.
  *
  * Mirrors `dev.constructive.eo.circe.JsonWalk` line-by-line, with parent-type dispatch generalised
  * from "JsonObject vs Vector\[Json\]" to "IndexedRecord vs java.util.List vs java.util.Map" so the
  * walker can pierce all four reachable Avro shapes (records, arrays, maps, union alternatives)
  * under one fold.
  *
  * '''Gap-6 (per the eo-avro plan).''' Avro's runtime string type is `org.apache.avro.util.Utf8`, a
  * subtype of `CharSequence`. The walker is lenient about string shapes — fields whose schema is
  * `STRING` come back as `Utf8` from the apache-avro reader and the codec layer converts to Scala
  * `String` at the leaf. The string check uses `instanceof CharSequence`, not `instanceof String`.
  *
  * The structural `Eq[IndexedRecord]` / `Eq[GenericRecord]` givens (per Gap-5 / OQ-avro-9) live in
  * the [[dev.constructive.eo.avro]] package object — that's the public-facing user surface;
  * `AvroWalk` itself is implementation detail.
  *
  * '''Unit 12 (perf).''' Two parallel entry points:
  *
  *   - [[walkPath]] — the legacy `(Any, Vector[AnyRef])`-shaped surface, kept for tests / direct
  *     introspection.
  *   - [[walkPathArr]] — the hot-path entry. Stores parents in a pre-allocated `Array[AnyRef]` of
  *     size `path.length` and runs as a tail-recursive while-loop. Per-step `Right(...)` boxing,
  *     `Vector.:+` constant factor (~5× `ArrayBuilder`), and `Vector` materialisation from the
  *     `Array` are all eliminated. The companion [[rebuildPathArr]] folds the array backwards via a
  *     `while` loop. Used by the four [[AvroFocus]] hot paths (`modifyImpl` / `transformImpl` /
  *     `placeImpl` / `readImpl`) and the corresponding Ior surfaces.
  */
private[avro] object AvroWalk:

  /** A walked-cursor state: the current Avro focus (typed as `Any` because Avro's runtime values
    * span [[IndexedRecord]] / [[java.util.List]] / [[java.util.Map]] / `Utf8` / unboxed
    * primitives), paired with the Vector of parents collected so far. Parents are ordered
    * root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is the immediate parent of
    * the terminal value.
    */
  type State = (Any, Vector[AnyRef])

  /** Hot-path walk result: the current Avro focus, the parents array (sized to `path.length` —
    * `path` is the input passed to [[walkPathArr]]), and the count of parent slots actually filled.
    * `UnionBranch` steps don't push parents, so `parentsLen <= path.length`.
    *
    * Allocated once per walk. Held by reference through the per-record hooks in [[AvroFocus]] and
    * fed straight into [[rebuildPathArr]] without intermediate copying.
    */
  final class WalkRes(val cur: Any, val parents: Array[AnyRef], val parentsLen: Int)

  /** Per-step policy controlling how missing field-steps behave. Mirrors
    * [[dev.constructive.eo.circe.JsonWalk.OnMissingField]].
    */
  enum OnMissingField:

    /** Missing field at a Field step → `Left(AvroFailure.PathMissing(step))`. The strict default,
      * used by every read / decoded-modify path.
      */
    case Strict

    /** Missing field at a Field step → `Right((null, parents :+ parent))`. Used by the per-element
      * raw-transform / place paths on a Traversal, where a missing-field element is treated as
      * "synthesise a null leaf and let the user-supplied f or value take over".
      */
    case Lenient

  /** One step of a walk under the given `OnMissingField` policy. Index steps always fail strictly
    * (out-of-range indices and non-array parents are uniform errors regardless of policy).
    *
    * The runtime dispatch is type-driven: `IndexedRecord` for record fields, `java.util.Map[_, _]`
    * for map keys (with the same `PathStep.Field(name)` shape), `java.util.List[_]` for array
    * indices, and pass-through for `UnionBranch` (the apache-avro reader has already resolved the
    * alternative; we just verify the runtime type aligns).
    *
    * Kept as a single-step, Vector-shaped entry point for direct tests; the hot path uses
    * [[walkPathArr]]'s inlined loop.
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

  /** Walk an entire `path` from `record`, accumulating parents in a `Vector[AnyRef]`. Strict by
    * default — pass `OnMissingField.Lenient` to tolerate missing field-steps as `null` leaves.
    *
    * Legacy entry point — preserved for tests and for the few callers that historically held
    * parents as a Vector. Hot-path callers should use [[walkPathArr]] instead, which avoids the
    * Vector materialisation.
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

  /** Hot-path walk — array-indexed `while` loop with a pre-allocated parents array sized to
    * `path.length`. Always returns the same [[WalkRes]] shape; UnionBranch failures are
    * post-processed in-line so [[AvroFailure.UnionResolutionFailed.branches]] always carries the
    * full schema-declared alternative list.
    *
    * Failure short-circuit uses an in-loop sentinel (`failure: AvroFailure | Null`) rather than
    * `return` so the scalafix `DisableSyntax.return` linter doesn't fire. The JIT inlines the
    * one-instruction `null` check on the loop guard.
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

  /** Recover the union-schema branch list from the path step that produced the current parent.
    *
    * Walks backwards from `unionStepIdx - 1` to find the most recent `Field` step on the path; the
    * parent record collected at that position carries the field whose schema is the union we're
    * trying to resolve. Operates directly on the partially-filled parents array — tracks a `pIdx`
    * cursor that decrements only on non-UnionBranch steps so it stays aligned with the walker's
    * parents-array layout. Returns `Nil` when no such step is found.
    */
  private def unionBranchesAtArr(
      path: Array[PathStep],
      unionStepIdx: Int,
      parents: Array[AnyRef],
      parentsLen: Int,
  ): List[String] =
    // The union step itself didn't push a parent, so the most recent parent index equals
    // `parentsLen - 1`. Decrement on each non-UnionBranch step as we walk backwards.
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

  /** The terminal step of `path`, or a sentinel `PathStep.Field("")` when `path` is empty. Used
    * when a non-step-shaped failure (decode failure, "terminal value isn't of the expected type")
    * needs to point at "the last step we tried to interpret".
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)

  /** Rebuild an Avro record from the leaf back to the root, using the parents collected during a
    * walk.
    *
    * '''D15 (per the eo-avro plan).''' Avro's [[IndexedRecord.put]] mutates in place; we choose the
    * immutable shape — `rebuildStep` allocates a fresh
    * [[org.apache.avro.generic.GenericData.Record]] (or [[GenericData.Array]] / `HashMap`) on each
    * splice so successive `.modify` calls don't corrupt the input record. Cost: one allocation per
    * parent step, per `modify`.
    *
    * Legacy Vector-shaped entry point — kept for callers that historically held parents as a Vector
    * (currently `AvroTraversal.iorMapElements` and `AvroFocus.Fields.rebuild`). Hot-path callers
    * should use [[rebuildPathArr]].
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

  /** Hot-path rebuild — folds the parents array backwards via a `while` loop. The pairing with
    * `path` mirrors the legacy `parents.zip(path).foldRight` shape: i-th parent pairs with i-th
    * path step, iterating `parentsLen` times. UnionBranch steps in the prefix that didn't push
    * parents simply aren't visited (matching the legacy `zip` semantics that drops dangling path
    * tail steps).
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

  /** Splice `child` into `parent` at `step`. The parent's runtime shape is determined by the step
    * kind plus the parent's runtime class:
    *   - `Field` step on [[IndexedRecord]]: clone via [[GenericData.deepCopy]] then `put`
    *     positionally (immutable boundary per D15).
    *   - `Field` step on [[java.util.Map]]: build a fresh `LinkedHashMap` with the entry replaced.
    *   - `Index` step on [[java.util.List]]: build a fresh [[GenericData.Array]] with the entry
    *     replaced.
    *   - `UnionBranch` step: passthrough — the union representation in apache-avro generic data is
    *     just the alternative value directly.
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
            // Iterate fields by index — avoids the asScala iterator allocation; the field-name
            // → position lookup runs once at construction (the matching slot only).
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
            // Replace by string key; if the parent stored a Utf8 key, drop both forms first
            // and re-insert.
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

  /** Allocate a fresh [[IndexedRecord]] by copying `parent`'s positional slots, replacing any slot
    * whose schema field name appears in `updates`. The fresh record is allocated under
    * `parent.getSchema`, so the result has the same schema identity as the input.
    *
    * Used by the multi-field overlay paths in [[AvroFocus.Fields]] (and, in Unit 6+, the matching
    * traversal Fields focus). Counterpart to circe's `JsonFieldsPrism.writeFields` /
    * `overlayFields` helpers; the underlying mechanic — "rewrite a subset of named slots in one
    * shot" — is uniform enough to live in [[AvroWalk]] alongside [[rebuildStep]].
    *
    * Per D15 (immutable boundary): always allocates a new record so the input is never mutated.
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

  /** Look up the union-branch name for a runtime value. Apache-avro's `GenericData.resolveUnion`
    * returns the index of the matching alternative; we map back to the schema's name list to
    * surface a stable, schema-driven identifier in [[AvroFailure.UnionResolutionFailed]].
    *
    * Defensive: returns the raw class name when no schema is in scope (the walker's `UnionBranch`
    * step is reachable only from a parent that has a schema, so this fallback is mostly cosmetic).
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
