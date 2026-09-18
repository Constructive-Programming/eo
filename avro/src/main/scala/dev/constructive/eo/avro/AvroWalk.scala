package dev.constructive.eo.avro

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

import dev.constructive.eo.widenRight
import java.util.{ArrayList, LinkedHashMap, List as JList, Map as JMap}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericEnumSymbol, GenericFixed, IndexedRecord}

/** Shared internal helpers for fold-based Avro walks used by [[AvroPrism]] and [[AvroTraversal]].
  * Mirrors `circe.JsonWalk` with parent-type dispatch generalised across `IndexedRecord` /
  * `java.util.List` / `java.util.Map` / union alternatives.
  *
  * Avro's runtime string type is `Utf8 <: CharSequence`; the walker uses `instanceof CharSequence`.
  *
  * Two entry points: [[walkPath]] — legacy `(Any, Vector[Any])` shape; [[walkPathArr]] — hot-path
  * entry storing parents in a pre-allocated `Array[Any]`. The latter eliminates `Right(...)`
  * boxing, `Vector.:+`, and `Vector` materialisation; [[rebuildPathArr]] folds it backwards. The
  * four `AvroFocus` hot paths (`modifyImpl` / `transformImpl` / `placeImpl` / `readImpl`) plus
  * their Ior siblings use `walkPathArr`.
  */
private[avro] object AvroWalk:

  /** Walked-cursor state: current focus (Avro runtime values span `IndexedRecord` / `List` / `Map`
    * / `Utf8` / primitives), paired with parents Vector. Root-to-leaf ordered.
    */
  type State = (Any, Vector[Any])

  /** Hot-path walk result. `parents` is sized to `path.length`; `parentsLen` counts filled slots
    * (`UnionBranch` steps don't push parents). Held through `AvroFocus` hooks and fed straight into
    * [[rebuildPathArr]].
    */
  final class WalkRes(val cur: Any, val parents: Array[Any], val parentsLen: Int)

  /** Legacy entry — Vector-shaped parents. Preserved for the non-hot callers; hot-path callers use
    * [[walkPathArr]].
    */
  def walkPath(
      record: IndexedRecord,
      path: Array[PathStep],
  ): Either[AvroFailure, State] =
    walkPathArr(record, path) match
      case l @ Left(_)   => l.widenRight
      case Right(walked) =>
        Right((walked.cur, Vector.from(walked.parents.iterator.take(walked.parentsLen))))

  /** Hot-path walk — array-indexed `@tailrec` loop threading `(i, parentsLen, cur)`. Failure
    * short-circuits by returning `Left` in place (no sentinel, no `return`). UnionBranch failures
    * resolve the full schema-declared alternative list inline.
    */
  def walkPathArr(
      record: IndexedRecord,
      path: Array[PathStep],
  ): Either[AvroFailure, WalkRes] =
    if path.length == 0 then Right(new WalkRes(record, EmptyParents, 0))
    else walkArrLoop(path, new Array[Any](path.length), 0, 0, record)

  /** Step machine for [[walkPathArr]] — advances `(i, parentsLen, cur)` through `path`, filling
    * `parents` in place. Failure short-circuits by returning `Left`; UnionBranch failures resolve
    * the schema-declared alternative list inline.
    */
  @tailrec private def walkArrLoop(
      path: Array[PathStep],
      parents: Array[Any],
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
              if field == null then Left(AvroFailure.PathMissing(step))
              else
                parents(parentsLen) = rec
                walkArrLoop(path, parents, i + 1, parentsLen + 1, rec.get(field.pos))
            case map: JMap[?, ?] =>
              val asMap = map.asInstanceOf[JMap[Any, Any]]
              val direct = asMap.get(name)
              val viaUtf8 =
                if direct == null then asMap.get(new org.apache.avro.util.Utf8(name))
                else direct
              if viaUtf8 == null then Left(AvroFailure.PathMissing(step))
              else
                parents(parentsLen) = map
                walkArrLoop(path, parents, i + 1, parentsLen + 1, viaUtf8)
            case _ =>
              Left(AvroFailure.NotARecord(step))

        case PathStep.Index(idx) =>
          cur match
            case lst: JList[?] =>
              val size = lst.size
              if idx < 0 || idx >= size then Left(AvroFailure.IndexOutOfRange(step, size))
              else
                parents(parentsLen) = lst
                walkArrLoop(path, parents, i + 1, parentsLen + 1, lst.get(idx))
            case _ =>
              Left(AvroFailure.NotAnArray(step))

        case PathStep.UnionBranch(branchName) =>
          cur match
            case null =>
              if branchName == "null" then walkArrLoop(path, parents, i + 1, parentsLen, null)
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
              if actualName != branchName then
                Left(
                  AvroFailure
                    .UnionResolutionFailed(
                      unionBranchesAtArr(path, i, parents, parentsLen),
                      step,
                    )
                )
              else
                other match
                  // EnumSymbol doesn't validate at construction, so a hand-built payload can
                  // carry a symbol the schema never declared — refuse it here rather than
                  // passing a corrupt leaf downstream.
                  case e: GenericEnumSymbol[?]
                      if !e.getSchema.getEnumSymbols.contains(e.toString) =>
                    Left(
                      AvroFailure.BadEnumSymbol(
                        e.toString,
                        e.getSchema.getEnumSymbols.asScala.toList,
                        step,
                      )
                    )
                  case _ =>
                    walkArrLoop(path, parents, i + 1, parentsLen, cur)

  private val EmptyParents: Array[Any] = new Array[Any](0)

  /** Recover the union schema's branch list. Walks backwards from `unionStepIdx - 1` to the most
    * recent `Field` step; the parent at that position carries the union field. `pIdx` cursor
    * decrements only on non-UnionBranch steps to stay aligned with parents-array layout. `Nil` if
    * no such step is found.
    */
  private def unionBranchesAtArr(
      path: Array[PathStep],
      unionStepIdx: Int,
      parents: Array[Any],
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
      parents: Vector[Any],
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
      parents: Array[Any],
      parentsLen: Int,
      path: Array[PathStep],
      newLeaf: Any,
  ): Any =
    @tailrec def loop(i: Int, child: Any): Any =
      if i < 0 then child
      else loop(i - 1, rebuildStep(parents(i), path(i), child))
    loop(parentsLen - 1, newLeaf)

  /** [[rebuildPath]] narrowed to the root record — every walk the callers rebuild through starts at
    * an `IndexedRecord`, so the final splice yields one. The narrowing cast lives here, ONCE,
    * instead of at every call site.
    */
  def rebuildRecord(
      parents: Vector[Any],
      path: Array[PathStep],
      newLeaf: Any,
  ): IndexedRecord =
    rebuildPath(parents, path, newLeaf).asInstanceOf[IndexedRecord]

  /** [[rebuildPathArr]] narrowed to the root record — see [[rebuildRecord]]. */
  def rebuildRecordArr(
      parents: Array[Any],
      parentsLen: Int,
      path: Array[PathStep],
      newLeaf: Any,
  ): IndexedRecord =
    rebuildPathArr(parents, parentsLen, path, newLeaf).asInstanceOf[IndexedRecord]

  /** Splice `child` into `parent` at `step`. Dispatch:
    *   - `Field` on `IndexedRecord` — fresh record with `put` at the matching slot.
    *   - `Field` on `Map` — fresh `LinkedHashMap` with the entry replaced.
    *   - `Index` on `List` — fresh `GenericData.Array` with the entry replaced.
    *   - `UnionBranch` — passthrough (union value is the alternative directly).
    */
  def rebuildStep(
      parent: Any,
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
    * `JsonFocus.Fields`'s overlay helpers).
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

  /** The schema field name for the case-class field `scalaName` — declaration index `declIdx` among
    * the case fields `caseNames` — inside the record reached by [[schemaAt]]`(root, parentPath)`.
    * See [[fieldNameAt]] for the resolution rule.
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
      caseNames: List[String],
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
          fieldNameAt(parent, scalaName, declIdx, caseNames, who)

  /** Schema field name for case field `scalaName` (declaration index `declIdx` among `caseNames`)
    * in `record`, which must be a RECORD. Split out so [[AvroTraversal]] can resolve against an
    * element record it computed itself.
    *
    * '''Two rungs, tried in order (issue #95).'''
    *
    *   1. NOMINAL, all-or-nothing. If EVERY case field in `caseNames` maps to a DISTINCT schema
    *      field — exactly, or uniquely up to `_`/`-`/`.` and case — then the codec has told us the
    *      whole correspondence and `declIdx`'s answer is read off that map. Partial or colliding
    *      coverage is no signal at all: the rung abstains for every field rather than trusting one
    *      lucky match (see [[totalNominalIndex]] for why the per-field form is unsound).
    *   1. POSITIONAL (issue #35): the i-th case field is the i-th schema field. This is the rung a
    *      name transform lands on — `withSnakeCaseFieldNames`, a custom `transformFieldNames`, a
    *      vulcan override map — because a transform REMOVES the literal Scala name by construction,
    *      so rung 1 cannot have fired. It is also the only rung that can be wrong, and it is right
    *      exactly when the codec's schema is positionally 1:1 with the case class.
    *
    * Nominal before positional is safe precisely because the two rungs see disjoint populations.
    * What nominal DOES see is the codec whose schema is not positionally 1:1 — a computed/derived
    * schema field, a dropped one, a reordered hand-written or `vulcan.Codec` field list — where
    * position silently targets the wrong slot and produces valid wire bytes with wrong content.
    *
    * '''Known residual''': a codec that both renames beyond recognition AND reorders (equal arity,
    * no name hit) still resolves by position, and is still wrong — nothing about the names or the
    * shape can see it. Use [[AvroPrism.fieldNamed]] there.
    *
    * '''Known behaviour change''': a codec that PERMUTES the Scala names (writes case field `a`
    * into a schema field literally named `b`, and vice versa) resolved correctly by position and
    * now resolves by name, i.e. wrongly. No name transform can produce that shape — a transform is
    * a function of the name alone — but a hand-written field list can.
    */
  def fieldNameAt(
      record: Schema,
      scalaName: String,
      declIdx: Int,
      caseNames: List[String],
      who: String,
  ): String =
    if record.getType != Schema.Type.RECORD then
      throw new IllegalArgumentException(
        s"$who('$scalaName'): parent focus is a ${record.getType}, not a record."
          + " Descend a nullable/union field with .union[Branch] first."
      )
    else
      val fields = record.getFields
      val nominal = totalNominalIndex(record, caseNames, declIdx)
      if nominal >= 0 then fields.get(nominal).name
      else if declIdx >= fields.size then
        throw new IllegalArgumentException(
          s"$who('$scalaName'): case field #$declIdx has no matching schema field —"
            + s" record '${record.getFullName}' has ${fields.size} field(s): "
            + fields.asScala.map(_.name).mkString(", ")
            + s", none named '$scalaName'"
            + ". For a reordered hand-written codec, navigate by explicit schema name with"
            + " .fieldNamed(\"<name>\")."
        )
      else fields.get(declIdx).name

  /** ALL-OR-NOTHING nominal resolution: the schema-field position for the case field at `declIdx`,
    * or `-1` to abstain and let position decide.
    *
    * The per-field form of this rung — "does THIS case field's name appear in the schema?" — is
    * unsound, and measurably so: a schema field can bear a name that resembles a DIFFERENT case
    * field. `FpVisit(userId, user)` against legacy columns `{uid, user_id}` writes `userId` into
    * `uid`, and a per-field rung matches `userId` against `user_id` and re-aims a currently-correct
    * call site at the wrong column. Requiring the WHOLE case-field list to map to DISTINCT schema
    * fields (total and injective) before trusting any single answer removes that class: `user`
    * matches nothing, the map is not total, the rung abstains, position stays right.
    *
    * An empty `caseNames` (a NamedTuple parent, which has no case fields) abstains too.
    */
  private def totalNominalIndex(record: Schema, caseNames: List[String], declIdx: Int): Int =
    if caseNames.isEmpty || declIdx < 0 || declIdx >= caseNames.size then -1
    else
      val out = new Array[Int](caseNames.size)
      @tailrec def loop(i: Int, rest: List[String], seen: Set[Int]): Boolean =
        rest match
          case Nil    => true
          case n :: t =>
            val idx = nominalIndex(record, n)
            if idx < 0 || seen.contains(idx) then false
            else
              out(i) = idx
              loop(i + 1, t, seen + idx)
      if loop(0, caseNames, Set.empty) then out(declIdx) else -1

  /** Position of the schema field naming `scalaName` — exactly, else uniquely up to separators and
    * case. `-1` when no field matches or when more than one does: an ambiguous signal is no signal.
    */
  private def nominalIndex(record: Schema, scalaName: String): Int =
    val exact = record.getField(scalaName)
    if exact != null then exact.pos
    else
      val fields = record.getFields
      @tailrec def loop(i: Int, found: Int): Int =
        if i >= fields.size then found
        else if sameFieldName(fields.get(i).name, scalaName) then
          if found >= 0 then -1 else loop(i + 1, i)
        else loop(i + 1, found)
      loop(0, -1)

  /** `a` and `b` name the same field up to `_` / `-` / `.` and case: `landingPageId`,
    * `landing_page_id`, `LANDING_PAGE_ID` and `landing-page-id` all match. Two cursors rather than
    * two normalised copies — this runs once per candidate schema field per drilled hop, at
    * construction time, and has no business allocating.
    *
    * NOT a transform inverter: it recognises only the letter-preserving transform family, which is
    * exactly why an unrecognised transform falls through to the positional rung (issue #35) instead
    * of being guessed at.
    */
  private def sameFieldName(a: String, b: String): Boolean =
    @tailrec def skip(s: String, i: Int): Int =
      if i >= s.length then i
      else
        val c = s.charAt(i)
        if c == '_' || c == '-' || c == '.' then skip(s, i + 1) else i
    @tailrec def loop(i: Int, j: Int): Boolean =
      val x = skip(a, i)
      val y = skip(b, j)
      if x >= a.length || y >= b.length then x >= a.length && y >= b.length
      else if Character.toLowerCase(a.charAt(x)) != Character.toLowerCase(b.charAt(y)) then false
      else loop(x + 1, y + 1)
    loop(0, 0)

  /** Union-branch name for a runtime value. Schema-driven where possible — the named leaves
    * (records, enums, fixed) answer with their schema's FULL name, matching how union branches are
    * declared; generic-decoded `bytes` arrive as `ByteBuffer`. Falls back to the raw class name
    * (the fallback is cosmetic — UnionBranch steps reach this only from a schemaful parent).
    */
  private def unionBranchName(value: Any): String =
    value match
      case rec: IndexedRecord      => rec.getSchema.getFullName
      case e: GenericEnumSymbol[?] => e.getSchema.getFullName
      case f: GenericFixed         => f.getSchema.getFullName
      case _: CharSequence         => "string"
      case _: java.lang.Integer    => "int"
      case _: java.lang.Long       => "long"
      case _: java.lang.Float      => "float"
      case _: java.lang.Double     => "double"
      case _: java.lang.Boolean    => "boolean"
      case _: java.nio.ByteBuffer  => "bytes"
      case _: Array[Byte]          => "bytes"
      case _: JList[?]             => "array"
      case _: JMap[?, ?]           => "map"
      case other                   => other.getClass.getName

end AvroWalk
