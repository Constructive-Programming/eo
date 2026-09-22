package dev.constructive.eo.circe

import scala.language.implicitConversions

import io.circe.syntax.*

/** Contract pins for [[JsonWalk.modifyPath]] — the fused write walk.
  *
  * `modifyPath` used to abort a miss by throwing a private `ControlThrowable` caught in the same
  * method (`miss`), because `f: Json => Json` had no way to say "stop". It now returns
  * [[JsonWalk.WalkResult]] — `JsonFailure | Json`, a union rather than an `Either` so the splice
  * frames box nothing — and the walk neither throws nor catches. These examples pin what that bought
  * and what it must not lose: the failure travels as a VALUE (no partial rebuild is observable), `f`
  * runs only on a successful walk, and a throwable raised by user code propagates instead of being
  * mistaken for a miss.
  */
class JsonWalkSpec extends JsonSpecBase:

  import JsonSpecFixtures.*

  private val person: Json = Person("Alice", 30, Address("main st", 1234)).asJson

  private val streetPath: Array[PathStep] =
    Array(PathStep.Field("address"), PathStep.Field("street"))

  /** The walk's failure arm, or `None` when it returned a rebuilt document. */
  private def asFailure(result: JsonWalk.WalkResult): Option[JsonFailure] = result match
    case f: JsonFailure => Some(f)
    case _: Json        => None

  // covers: JsonWalk.scala modifyPath — the terminal frame returns `f`'s value verbatim, so a
  //   failure must reach the caller as that exact object (a rewritten failure, or a rebuild that
  //   returned the document instead, both fail here).
  "modifyPath: a terminal failure is returned as-is, never as a rebuilt document" >> {
    val failure = JsonFailure.PathMissing(PathStep.Field("street"))
    val out = JsonWalk.modifyPath(person, streetPath)(_ => failure)

    ((out eq failure) must beTrue).and(asFailure(out) must beSome(failure))
  }

  // covers: JsonWalk.scala modifyPath go — the splice frames short-circuit on the failure arm
  //   (`case failure: JsonFailure => failure`). A frame that rebuilt anyway would return a `Json`
  //   here, so this is the "no partial rebuild" pin for the DEEP failure case.
  "modifyPath: a miss below the root short-circuits every splice frame" >> {
    val deepMiss = Array(PathStep.Field("address"), PathStep.Field("nope"))
    asFailure(JsonWalk.modifyPath(person, deepMiss)(_ => Json.Null)) must
      beSome(JsonFailure.PathMissing(PathStep.Field("nope")))

    val notAnObject = Array(PathStep.Field("name"), PathStep.Field("nope"))
    asFailure(JsonWalk.modifyPath(person, notAnObject)(_ => Json.Null)) must
      beSome(JsonFailure.NotAnObject(PathStep.Field("nope")))
  }

  // covers: JsonWalk.scala modifyPath the `if i >= path.length then f(cur)` guard — `f` must NOT run
  //   when the walk missed (a decode/encode that ran anyway would show up as a side effect here),
  //   which is what keeps a missing focus free on the silent surface.
  "modifyPath: f is not invoked when the walk misses" >> {
    var calls = 0
    val out = JsonWalk.modifyPath(person, Array(PathStep.Field("nope"))) { _ =>
      calls += 1
      Json.Null
    }

    (asFailure(out) must beSome(JsonFailure.PathMissing(PathStep.Field("nope"))))
      .and(calls must beEqualTo(0))
  }

  // covers: the 2026-09-22 removal of the MissSignal ControlThrowable — the walk body has no
  //   `try`/`catch` left, so a throwable from user code (a circe Encoder/Decoder blowing up) is a
  //   bug that surfaces loudly rather than a miss that silently returns the input unchanged. The
  //   old design's guarantee was weaker: it relied on `NonFatal` not catching `ControlThrowable`.
  "modifyPath: a throwable from f propagates instead of being swallowed as a miss" >> {
    JsonWalk.modifyPath(person, streetPath)(_ => throw new IllegalStateException("user bug")) must
      throwAn[IllegalStateException]
  }
