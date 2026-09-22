package dev.constructive.eo.circe

import scala.annotation.tailrec

import io.circe.Json

/** Shared internal helpers for the fold-based JSON walks used by [[JsonPrism]] / [[JsonTraversal]]
  * and the [[JsonFocus]] enum.
  *
  * '''2026-07-22 rethink — fused walk + rebuild.''' This file used to walk down collecting a
  * `parents` Vector (one node per hop, later one wrapper per hop) and fold it back leaf-to-root in
  * a separate rebuild pass. The parents existed ONLY to serve that rebuild, in reverse order —
  * which is exactly what a call stack provides for free. [[modifyPath]] now recurses down and
  * splices on the way back up: each frame's container is a typed local (no stored parent, no cast,
  * no walk/rebuild correlation invariant) and nothing per-hop reaches the heap — measured −72 B/op
  * on `OrderCirceBench.eoStreet` vs the wrapper encoding, below the pre-fusion baseline too, since
  * the `Vector.:+` nodes are gone as well. Reads take [[readPath]], which never touches rebuild
  * state at all. Recursion depth = `path.length` (small, user-authored); rebuild-on-return is
  * deliberately non-tail.
  *
  * '''2026-09-22 — no control-flow exceptions.''' [[modifyPath]] used to abort a miss by throwing a
  * private `ControlThrowable` caught in the same method, because `f: Json => Json` had no way to
  * say "stop here". `f` now RETURNS the failure channel as [[WalkResult]] — `JsonFailure | Json`, a
  * union rather than an `Either`, so no splice frame boxes anything — and this module carries no
  * throwable-based control flow. B/op on the `OrderCirceBench.eoStreet` shape: hit unchanged at
  * 1080, miss 80 → 40 (no Throwable to allocate, no `try`/`catch` frame). See
  * `docs/research/2026-09-22-exception-audit.md`.
  *
  * Deferred writers (the `(a, writer)` algebraic-lens seam in `JsonFocus.navigateForWrite`) re-walk
  * on invocation instead of capturing parents — a second pointer-chasing descent per write, paying
  * (cheap) CPU instead of per-hop heap.
  */
private[circe] object JsonWalk:

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

  /** Result of a write walk: the rebuilt document, or the failure that aborted it.
    *
    * A union rather than an `Either` on purpose. Both arms are already heap objects, so wrapping
    * them in `Right`/`Left` would add one box per splice frame for no extra information — and
    * [[modifyPath]]'s frames are the hot path (see the 2026-09-22 note above). [[readPath]] keeps
    * `Either`: it is tail-recursive, so it boxes once per CALL, not once per hop, and its callers
    * want the `Either` combinators.
    */
  type WalkResult = JsonFailure | Json

  extension (result: WalkResult)

    /** The rebuilt document, or `fallback` when the walk missed — the `Either.getOrElse` analogue
      * the silent `*Unsafe` surfaces use. The `*Ior` twins pattern-match the failure out instead,
      * so the miss is never merely dropped.
      */
    def getOrElse(fallback: Json): Json = result match
      case json: Json     => json
      case _: JsonFailure => fallback

  /** Walk `path`, apply `f` to the terminal value, and splice the result back up to the root in the
    * same recursion — the containers live on the call stack, typed, per frame. `f` RETURNS the
    * failure channel: a [[JsonFailure]] (decode failure, terminal-shape mismatch) aborts the walk
    * and the input Json comes back untouched through the caller's fallback branch, so a partial
    * rebuild is never observable. The walk itself never throws and never catches — a throwable
    * raised by `f` (a user `Encoder`/`Decoder` blowing up) propagates to the caller as-is, which is
    * the honest signal for "this is a bug, not a miss".
    */
  def modifyPath(
      json: Json,
      path: Array[PathStep],
  )(f: Json => WalkResult): WalkResult =
    def go(cur: Json, i: Int): WalkResult =
      if i >= path.length then f(cur)
      else
        path(i) match
          case step @ PathStep.Field(name) =>
            cur.asObject match
              case None      => JsonFailure.NotAnObject(step)
              case Some(obj) =>
                obj(name) match
                  case None    => JsonFailure.PathMissing(step)
                  case Some(c) =>
                    go(c, i + 1) match
                      case child: Json          => Json.fromJsonObject(obj.add(name, child))
                      case failure: JsonFailure => failure
          case step @ PathStep.Index(idx) =>
            cur.asArray match
              case None      => JsonFailure.NotAnArray(step)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then JsonFailure.IndexOutOfRange(step, arr.length)
                else
                  go(arr(idx), i + 1) match
                    case child: Json          => Json.fromValues(arr.updated(idx, child))
                    case failure: JsonFailure => failure
    go(json, 0)

  /** The terminal step of `path`, or a sentinel `PathStep.Field("")` when `path` is empty. Used
    * when a non-step-shaped failure (decode failure, "terminal value isn't an array") needs to
    * point at "the last step we tried to interpret".
    */
  inline def terminalOf(path: Array[PathStep]): PathStep =
    if path.length == 0 then PathStep.Field("") else path(path.length - 1)
