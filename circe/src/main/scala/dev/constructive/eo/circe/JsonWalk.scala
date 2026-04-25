package dev.constructive.eo.circe

import cats.syntax.foldable.*
import io.circe.{Json, JsonObject}

/** Shared internal helpers for the fold-based JSON walks used by [[JsonPrism]],
  * [[JsonFieldsPrism]], [[JsonTraversal]], and [[JsonFieldsTraversal]].
  *
  * The walk shape is identical across all four: descend an `Array[PathStep]`, at each step either
  * enter a JsonObject by name or a JsonArray by index, accumulate the parent into a Vector for the
  * eventual rebuild phase, and short-circuit on the first failure.
  *
  * Encoding the loop as a `foldLeftM` over the path with `Either[JsonFailure, _]` as the target
  * monad keeps the walks pure (no `var`, no `while`, no early `return`, no mutation of the parents
  * buffer) while preserving the same fail-fast semantics. Callers that need a different result
  * monad (e.g. `Option`, `Ior`) walk in `Either` first and then transform the result.
  */
private[circe] object JsonWalk:

  /** A walked-cursor state: the current Json focus, paired with the Vector of parents collected so
    * far. Parents are ordered root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is
    * the immediate parent of the terminal value.
    */
  type State = (Json, Vector[AnyRef])

  /** One step of a strict walk. Either descend into the named field of `cur.asObject`, or into the
    * indexed element of `cur.asArray`; in either case, append the container to `parents`. Failure
    * cases surface as the matching `JsonFailure`.
    *
    * Used by the `modify` / `get` families on [[JsonPrism]] / [[JsonFieldsPrism]] /
    * [[JsonTraversal]] / [[JsonFieldsTraversal]], where a missing field at any step is a hard miss.
    */
  def stepInto(
      step: PathStep,
      cur: Json,
      parents: Vector[AnyRef],
  ): Either[JsonFailure, State] =
    step match
      case PathStep.Field(name) =>
        cur.asObject match
          case None      => Left(JsonFailure.NotAnObject(step))
          case Some(obj) =>
            obj(name) match
              case None    => Left(JsonFailure.PathMissing(step))
              case Some(c) => Right((c, parents :+ obj))
      case PathStep.Index(idx) =>
        cur.asArray match
          case None      => Left(JsonFailure.NotAnArray(step))
          case Some(arr) =>
            if idx < 0 || idx >= arr.length then Left(JsonFailure.IndexOutOfRange(step, arr.length))
            else Right((arr(idx), parents :+ arr))

  /** One step of a lenient walk — same as [[stepInto]] except missing fields are silently filled
    * with `Json.Null` rather than failing. Index steps still fail on non-array parents and on
    * out-of-range indices.
    *
    * Used by the `transform` / `place` families on [[JsonPrism]] / [[JsonTraversal]], which write a
    * value at the leaf regardless of whether the source path contained one — a missing field
    * effectively gets created on the way back up.
    */
  def stepIntoLenient(
      step: PathStep,
      cur: Json,
      parents: Vector[AnyRef],
  ): Either[JsonFailure, State] =
    step match
      case PathStep.Field(name) =>
        cur.asObject match
          case None      => Left(JsonFailure.NotAnObject(step))
          case Some(obj) =>
            Right((obj(name).getOrElse(Json.Null), parents :+ obj))
      case PathStep.Index(_) => stepInto(step, cur, parents)

  /** Walk an entire `path` from `json`, accumulating parents at each step. Returns
    * `Left(firstFailure)` on the first miss; on success returns the terminal value and the parent
    * Vector.
    */
  def walkPath(
      json: Json,
      path: Array[PathStep],
  ): Either[JsonFailure, State] =
    path.toVector.foldLeftM[[T] =>> Either[JsonFailure, T], State]((json, Vector.empty)) {
      case ((cur, parents), step) => stepInto(step, cur, parents)
    }

  /** Lenient counterpart to [[walkPath]]: tolerates missing fields by replacing the leaf with
    * `Json.Null`. Used by the `transform` / `place` paths on [[JsonPrism]] / [[JsonTraversal]].
    */
  def walkPathLenient(
      json: Json,
      path: Array[PathStep],
  ): Either[JsonFailure, State] =
    path.toVector.foldLeftM[[T] =>> Either[JsonFailure, T], State]((json, Vector.empty)) {
      case ((cur, parents), step) => stepIntoLenient(step, cur, parents)
    }

  /** The terminal step of `path`, or a sentinel `PathStep.Field("")` when `path` is empty. Used by
    * walks that need to point at "the last step we tried to interpret" when a non-step-shaped
    * failure arises (e.g. a decode failure on the assembled leaf, or a "terminal value isn't an
    * array" on a [[JsonTraversal]] prefix).
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)

  /** Rebuild a Json from the leaf back to the root, using the parents collected during a walk.
    * Pairs each `parents(j)` with `path(j)` and folds right-to-left, applying [[rebuildStep]] at
    * each level. The result is `parent₀(parent₁(... parentₙ(newLeaf) ...))` — the root JSON with
    * the new leaf spliced through every collected parent.
    */
  def rebuildPath(
      parents: Vector[AnyRef],
      path: Array[PathStep],
      newLeaf: Json,
  ): Json =
    parents.zip(path.toIndexedSeq).foldRight(newLeaf) {
      case ((parent, step), child) =>
        rebuildStep(parent, step, child)
    }

  /** Splice `child` into `parent` at `step`. The parent's runtime shape is determined by the step
    * kind: Field steps require a JsonObject; Index steps require a `Vector[Json]`.
    */
  inline def rebuildStep(
      parent: AnyRef,
      step: PathStep,
      child: Json,
  ): Json =
    step match
      case PathStep.Field(name) =>
        Json.fromJsonObject(parent.asInstanceOf[JsonObject].add(name, child))
      case PathStep.Index(idx) =>
        Json.fromValues(parent.asInstanceOf[Vector[Json]].updated(idx, child))
