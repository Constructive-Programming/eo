package dev.constructive.eo.circe

import scala.annotation.tailrec
import scala.util.control.ControlThrowable

import io.circe.Json

/** Shared internal helpers for the fold-based JSON walks used by [[JsonPrism]] / [[JsonTraversal]]
  * and the [[JsonFocus]] enum.
  *
  * '''2026-07-22 rethink — fused walk + rebuild.''' This file used to walk down collecting a
  * `parents` Vector (one node per hop, later one wrapper per hop) and fold it back leaf-to-root in
  * a separate rebuild pass. The parents existed ONLY to serve that rebuild, in reverse order —
  * which is exactly what a call stack provides for free. [[modifyPath]] now recurses down and
  * splices on the way back up: each frame's container is a typed local (no stored parent, no cast,
  * no walk/rebuild correlation invariant) and NOTHING per-hop reaches the heap — measured −72 B/op
  * on `OrderCirceBench.eoStreet` vs the wrapper encoding, below the pre-fusion baseline too, since
  * the `Vector.:+` nodes are gone as well. Reads take [[readPath]], which never touches rebuild
  * state at all. Recursion depth = `path.length` (small, user-authored); rebuild-on-return is
  * deliberately non-tail.
  *
  * Deferred writers (the `(a, writer)` algebraic-lens seam in `JsonFocus.navigateForWrite`) re-walk
  * on invocation instead of capturing parents — a second pointer-chasing descent per write, paying
  * (cheap, allocation-free) CPU instead of per-hop heap.
  */
private[circe] object JsonWalk:

  /** Cold-branch control signal for [[modifyPath]] — a `ControlThrowable` (no stack trace, not
    * caught by `NonFatal`), allocated only when the walk misses. Also throwable from the terminal
    * function via [[miss]] for shape checks ("terminal isn't an array").
    */
  final private class MissSignal(val failure: JsonFailure) extends ControlThrowable

  /** Abort the enclosing [[modifyPath]] with `failure` — the input Json is returned untouched by
    * the caller's `Left` branch. For terminal-shape checks inside the spliced function.
    */
  def miss(failure: JsonFailure): Nothing = throw new MissSignal(failure)

  /** Walk `path` and return the terminal value. Read-only: no rebuild state of any kind. */
  def readPath(json: Json, path: Array[PathStep]): Either[JsonFailure, Json] =
    @tailrec def loop(cur: Json, i: Int): Either[JsonFailure, Json] =
      if i >= path.length then Right(cur)
      else
        path(i) match
          case step @ PathStep.Field(name) =>
            cur.asObject match
              case None      => Left(JsonFailure.NotAnObject(step))
              case Some(obj) =>
                obj(name) match
                  case Some(c) => loop(c, i + 1)
                  case None    => Left(JsonFailure.PathMissing(step))
          case step @ PathStep.Index(idx) =>
            cur.asArray match
              case None      => Left(JsonFailure.NotAnArray(step))
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  Left(JsonFailure.IndexOutOfRange(step, arr.length))
                else loop(arr(idx), i + 1)
    loop(json, 0)

  /** Walk `path`, apply `f` to the terminal value, and splice the result back up to the root in the
    * same recursion — the containers live on the call stack, typed, per frame. `f` may call
    * [[miss]] to abort (decode failure, terminal-shape mismatch); the input is then untouched.
    */
  def modifyPath(json: Json, path: Array[PathStep])(f: Json => Json): Either[JsonFailure, Json] =
    def go(cur: Json, i: Int): Json =
      if i >= path.length then f(cur)
      else
        path(i) match
          case step @ PathStep.Field(name) =>
            cur.asObject match
              case None      => miss(JsonFailure.NotAnObject(step))
              case Some(obj) =>
                obj(name) match
                  case None    => miss(JsonFailure.PathMissing(step))
                  case Some(c) => Json.fromJsonObject(obj.add(name, go(c, i + 1)))
          case step @ PathStep.Index(idx) =>
            cur.asArray match
              case None      => miss(JsonFailure.NotAnArray(step))
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  miss(JsonFailure.IndexOutOfRange(step, arr.length))
                else Json.fromValues(arr.updated(idx, go(arr(idx), i + 1)))
    try Right(go(json, 0))
    catch case m: MissSignal => Left(m.failure)

  /** The terminal step of `path`, or a sentinel `PathStep.Field("")` when `path` is empty. Used
    * when a non-step-shaped failure (decode failure, "terminal value isn't an array") needs to
    * point at "the last step we tried to interpret".
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)
