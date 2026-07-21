package dev.constructive.eo.circe

import scala.annotation.tailrec

import dev.constructive.eo.widenRight
import io.circe.{Json, JsonObject}

/** Shared internal helpers for the fold-based JSON walks used by [[JsonPrism]] / [[JsonTraversal]]
  * and the [[JsonFocus]] enum.
  *
  * '''2026-04-26 rethink.''' This file used to host two walks (strict / lenient) and two
  * single-step helpers with the same fold shape. They were first collapsed behind an
  * `OnMissingField` policy; the lenient arm then lost its last caller (the raw-transform / place
  * paths now route through [[JsonFocus]]) and was deleted — `walkPath` and `stepInto` are the only
  * entry points, and a missing field is always a hard miss.
  */
private[circe] object JsonWalk:

  /** A walked-cursor state: the current Json focus, paired with the Vector of parents collected so
    * far. Parents are ordered root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is
    * the immediate parent of the terminal value.
    */
  type State = (Json, Vector[AnyRef])

  /** One step of a walk. A missing field, an out-of-range index, and a non-container parent are all
    * hard misses.
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
              case Some(c) => Right((c, parents :+ obj))
              case None    => Left(JsonFailure.PathMissing(step))
      case PathStep.Index(idx) =>
        cur.asArray match
          case None      => Left(JsonFailure.NotAnArray(step))
          case Some(arr) =>
            if idx < 0 || idx >= arr.length then Left(JsonFailure.IndexOutOfRange(step, arr.length))
            else Right((arr(idx), parents :+ arr))

  /** Walk an entire `path` from `json`, accumulating parents at each step.
    *
    * Manual index loop rather than a `foldLeftM` over `path.toVector`: this runs once per array
    * element on a Traversal write, so the per-call `Array → Vector` copy and the `Either`-monad
    * fold machinery were pure churn. Behaviour is identical — first `Left` short-circuits (the
    * `failure` flag ends the loop; no `return`, per the `DisableSyntax` scalafix rule). The happy
    * path allocates nothing extra; `Some(f)` is built only on the cold miss branch.
    */
  def walkPath(
      json: Json,
      path: Array[PathStep],
  ): Either[JsonFailure, State] =
    @tailrec def loop(
        cur: Json,
        parents: Vector[AnyRef],
        i: Int,
    ): Either[JsonFailure, State] =
      if i < path.length then
        stepInto(path(i), cur, parents) match
          case l @ Left(_)          => l.widenRight
          case Right((c, parents2)) => loop(c, parents2, i + 1)
      else Right((cur, parents))
    loop(json, Vector.empty, 0)

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
    @tailrec def loop(child: Json, i: Int): Json =
      if i >= 0 then loop(rebuildStep(parents(i), path(i), child), i - 1)
      else child
    loop(newLeaf, parents.length - 1)

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
