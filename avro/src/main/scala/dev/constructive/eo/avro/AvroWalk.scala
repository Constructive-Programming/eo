package dev.constructive.eo.avro

import scala.jdk.CollectionConverters.*

import cats.Eq
import cats.syntax.foldable.*
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}

/** Shared internal helpers for the fold-based Avro walks used by [[AvroPrism]] and (in Unit 6+) its
  * traversal siblings.
  *
  * Mirrors `dev.constructive.eo.circe.JsonWalk` line-by-line, with parent-type dispatch generalised
  * from "JsonObject vs Vector\[Json\]" to "IndexedRecord vs java.util.List vs java.util.Map" so the
  * walker can pierce all four reachable Avro shapes (records, arrays, maps, union alternatives)
  * under one fold.
  *
  * '''Gap-5 (per the eo-avro plan).''' Avro's [[IndexedRecord]] is a Java interface with
  * reference-based equality. Property-test comparisons (`record1 === record2`) need a structural
  * `Eq[IndexedRecord]` instance — supplied below as a public `given` so downstream property tests
  * pick it up via `import dev.constructive.eo.avro.AvroWalk.given` (per OQ-avro-9). The
  * implementation compares schemas + positional values recursively.
  *
  * '''Gap-6 (per the eo-avro plan).''' Avro's runtime string type is `org.apache.avro.util.Utf8`, a
  * subtype of `CharSequence`. The walker is lenient about string shapes — fields whose schema is
  * `STRING` come back as `Utf8` from the apache-avro reader and the codec layer converts to Scala
  * `String` at the leaf. The string check uses `instanceof CharSequence`, not `instanceof String`.
  */
object AvroWalk:

  /** A walked-cursor state: the current Avro focus (typed as `Any` because Avro's runtime values
    * span [[IndexedRecord]] / [[java.util.List]] / [[java.util.Map]] / `Utf8` / unboxed
    * primitives), paired with the Vector of parents collected so far. Parents are ordered
    * root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is the immediate parent of
    * the terminal value.
    */
  type State = (Any, Vector[AnyRef])

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
            // Map keys are stored as CharSequence (Utf8) in apache-avro's
            // generic data; check both `String` and CharSequence forms.
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
        // For union steps we don't actually walk into a child container; the
        // "value-at-this-position" already IS the resolved alternative as far
        // as apache-avro's generic data model is concerned. We only need to
        // verify the runtime type lines up with the requested branch name; if
        // it doesn't, surface UnionResolutionFailed pointing at this step.
        cur match
          case null =>
            if branchName == "null" then Right((null, parents))
            else Left(AvroFailure.UnionResolutionFailed(List("null"), step))
          case other =>
            val actualName = unionBranchName(other)
            if actualName == branchName then Right((other, parents))
            else Left(AvroFailure.UnionResolutionFailed(List(actualName), step))

  /** Walk an entire `path` from `record`, accumulating parents at each step. Strict by default —
    * pass `OnMissingField.Lenient` to tolerate missing field-steps as `null` leaves.
    */
  def walkPath(
      record: IndexedRecord,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[AvroFailure, State] =
    path
      .toVector
      .foldLeftM[[T] =>> Either[AvroFailure, T], State]((record: Any, Vector.empty)) {
        case ((cur, parents), step) => stepInto(step, cur, parents, policy)
      }

  /** Lenient variant of [[walkPath]] — missing field-steps at any depth become `null` leaves. */
  def walkPathLenient(
      record: IndexedRecord,
      path: Array[PathStep],
  ): Either[AvroFailure, State] =
    walkPath(record, path, OnMissingField.Lenient)

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
    */
  def rebuildPath(
      parents: Vector[AnyRef],
      path: Array[PathStep],
      newLeaf: Any,
  ): Any =
    parents.zip(path.toIndexedSeq).foldRight(newLeaf) {
      case ((parent, step), child) =>
        rebuildStep(parent, step, child)
    }

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
            schema
              .getFields
              .asScala
              .foreach { f =>
                if f.name == name then fresh.put(f.pos, child.asInstanceOf[AnyRef])
                else fresh.put(f.pos, rec.get(f.pos))
              }
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
    schema
      .getFields
      .asScala
      .foreach { f =>
        updates.get(f.name) match
          case Some(v) => fresh.put(f.pos, v.asInstanceOf[AnyRef])
          case None    => fresh.put(f.pos, parent.get(f.pos))
      }
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

  /** Structural equality for [[IndexedRecord]] — schema + positional field values, recursing
    * through nested records / arrays / maps.
    *
    * Per Gap-5 / OQ-avro-9 this is a public `given`. Downstream property tests and round-trip specs
    * that compare records by value (rather than reference) pick this up via
    * `import dev.constructive.eo.avro.AvroWalk.given` or `import dev.constructive.eo.avro.given`.
    *
    * Implementation note: defers to `org.apache.avro.generic.GenericData.compare` which already
    * walks the schema-driven runtime shape recursively. Equal iff `compare == 0`.
    */
  given Eq[IndexedRecord] with

    def eqv(x: IndexedRecord, y: IndexedRecord): Boolean =
      x.getSchema == y.getSchema &&
        GenericData.get().compare(x, y, x.getSchema) == 0

  /** Public re-export of [[Eq[IndexedRecord]]] specialised to [[GenericRecord]] — the more common
    * runtime type. Reuses the same `compare`-based implementation.
    */
  given Eq[GenericRecord] = Eq.by(identity[IndexedRecord])

end AvroWalk
