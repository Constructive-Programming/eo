package dev.constructive.eo.circe

import io.circe.{Json, JsonObject}

/** Shared internal helpers for the fold-based JSON walks used by [[JsonPrism]] / [[JsonTraversal]]
  * and the [[JsonFocus]] enum.
  *
  * '''2026-04-26 rethink.''' This file used to host two walks (`walkPath` strict /
  * `walkPathLenient` lenient) and two single-step helpers (`stepInto` / `stepIntoLenient`). The
  * four routines had the same fold shape and only diverged on a single decision: whether a missing
  * field at a Field step is a hard miss or a `Json.Null` fallback. The duplicates have been
  * collapsed by factoring the per-step decision out as an `OnMissingField` policy — `walkPath` and
  * `stepInto` are the only entry points; passing `OnMissingField.Lenient` recovers the lenient
  * semantics.
  */
private[circe] object JsonWalk:

  /** A walked-cursor state: the current Json focus, paired with the Vector of parents collected so
    * far. Parents are ordered root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is
    * the immediate parent of the terminal value.
    */
  type State = (Json, Vector[AnyRef])

  /** Per-step policy controlling how missing field-steps behave. */
  enum OnMissingField:

    /** Missing field at a Field step → `Left(JsonFailure.PathMissing(step))`. The strict default,
      * used by every read / decoded-modify path.
      */
    case Strict

    /** Missing field at a Field step → `Right((Json.Null, parents :+ obj))`. Used by the per-
      * element raw-transform / place paths on a Traversal, where a missing-field element is treated
      * as "synthesise a Null leaf and let the user-supplied f or value take over".
      */
    case Lenient

  /** One step of a walk under the given `OnMissingField` policy. Index steps always fail strictly
    * (out-of-range indices and non-array parents are uniform errors regardless of policy).
    */
  def stepInto(
      step: PathStep,
      cur: Json,
      parents: Vector[AnyRef],
      policy: OnMissingField,
  ): Either[JsonFailure, State] =
    step match
      case PathStep.Field(name) =>
        cur.asObject match
          case None      => Left(JsonFailure.NotAnObject(step))
          case Some(obj) =>
            obj(name) match
              case Some(c) => Right((c, parents :+ obj))
              case None    =>
                policy match
                  case OnMissingField.Strict  => Left(JsonFailure.PathMissing(step))
                  case OnMissingField.Lenient => Right((Json.Null, parents :+ obj))
      case PathStep.Index(idx) =>
        cur.asArray match
          case None      => Left(JsonFailure.NotAnArray(step))
          case Some(arr) =>
            if idx < 0 || idx >= arr.length then Left(JsonFailure.IndexOutOfRange(step, arr.length))
            else Right((arr(idx), parents :+ arr))

  /** Walk an entire `path` from `json`, accumulating parents at each step. Strict by default — pass
    * `OnMissingField.Lenient` to tolerate missing field-steps as `Json.Null` leaves.
    *
    * Manual index loop rather than a `foldLeftM` over `path.toVector`: this runs once per array
    * element on a Traversal write, so the per-call `Array → Vector` copy and the `Either`-monad
    * fold machinery were pure churn. Behaviour is identical — first `Left` short-circuits.
    */
  def walkPath(
      json: Json,
      path: Array[PathStep],
      policy: OnMissingField = OnMissingField.Strict,
  ): Either[JsonFailure, State] =
    var cur: Json = json
    var parents: Vector[AnyRef] = Vector.empty
    var i = 0
    while i < path.length do
      stepInto(path(i), cur, parents, policy) match
        case Left(failure)        => return Left(failure)
        case Right((c, parents2)) =>
          cur = c
          parents = parents2
      i += 1
    Right((cur, parents))

  /** The terminal step of `path`, or a sentinel `PathStep.Field("")` when `path` is empty. Used
    * when a non-step-shaped failure (decode failure, "terminal value isn't an array") needs to
    * point at "the last step we tried to interpret".
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)

  /** Rebuild a Json from the leaf back to the root, using the parents collected during a walk.
    *
    * Manual reverse index loop rather than `parents.zip(path.toIndexedSeq).foldRight(...)`: like
    * [[walkPath]] this runs per array element on a Traversal write, so the per-call `Array →
    * IndexedSeq` copy, the `zip` pair allocation, and the `foldRight` machinery were pure churn.
    * `parents` is the prefix of `path` collected during the walk (`parents.length <= path.length`),
    * so index `i` lines up. Behaviour is identical.
    */
  def rebuildPath(
      parents: Vector[AnyRef],
      path: Array[PathStep],
      newLeaf: Json,
  ): Json =
    var child = newLeaf
    var i = parents.length - 1
    while i >= 0 do
      child = rebuildStep(parents(i), path(i), child)
      i -= 1
    child

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
