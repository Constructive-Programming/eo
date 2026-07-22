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

  /** One collected parent hop: the container walked through PLUS the position taken in it —
    * everything a rebuild needs, captured at walk time. Reifying the (container, position) pair
    * makes the old correlation invariant ("`parents(i)`'s runtime shape matches `path(i)`'s step
    * kind") structural: [[rebuildPath]] no longer takes the path at all, and the `AnyRef` casts the
    * correlation demanded are gone.
    */
  enum ParentStep:
    case InObject(obj: JsonObject, name: String)
    case InArray(arr: Vector[Json], idx: Int)

  /** A walked-cursor state: the current Json focus, paired with the Vector of parent hops collected
    * so far. Hops are ordered root-to-leaf — `parents(0)` is the root container, `parents(n-1)` is
    * the immediate parent of the terminal value.
    */
  type State = (Json, Vector[ParentStep])

  /** One step of a walk. A missing field, an out-of-range index, and a non-container parent are all
    * hard misses.
    */
  def stepInto(
      step: PathStep,
      cur: Json,
      parents: Vector[ParentStep],
  ): Either[JsonFailure, State] =
    step match
      case PathStep.Field(name) =>
        cur.asObject match
          case None      => Left(JsonFailure.NotAnObject(step))
          case Some(obj) =>
            obj(name) match
              case Some(c) => Right((c, parents :+ ParentStep.InObject(obj, name)))
              case None    => Left(JsonFailure.PathMissing(step))
      case PathStep.Index(idx) =>
        cur.asArray match
          case None      => Left(JsonFailure.NotAnArray(step))
          case Some(arr) =>
            if idx < 0 || idx >= arr.length then Left(JsonFailure.IndexOutOfRange(step, arr.length))
            else Right((arr(idx), parents :+ ParentStep.InArray(arr, idx)))

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
        parents: Vector[ParentStep],
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

  /** Rebuild a Json from the leaf back to the root, using the parent hops collected during a walk.
    * Each hop carries its own container and position, so no path is needed here.
    *
    * Manual reverse index loop rather than a `foldRight`: like [[walkPath]] this runs per array
    * element on a Traversal write, so the fold machinery was pure churn. Behaviour is identical.
    */
  def rebuildPath(
      parents: Vector[ParentStep],
      newLeaf: Json,
  ): Json =
    @tailrec def loop(child: Json, i: Int): Json =
      if i >= 0 then loop(rebuildStep(parents(i), child), i - 1)
      else child
    loop(newLeaf, parents.length - 1)

  /** Splice `child` into `parent` at the position the hop recorded. */
  inline def rebuildStep(parent: ParentStep, child: Json): Json =
    parent match
      case ParentStep.InObject(obj, name) => Json.fromJsonObject(obj.add(name, child))
      case ParentStep.InArray(arr, idx)   => Json.fromValues(arr.updated(idx, child))
