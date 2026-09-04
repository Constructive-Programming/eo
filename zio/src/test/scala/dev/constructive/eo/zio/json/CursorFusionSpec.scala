package dev.constructive.eo
package zio
package json

import _root_.zio.json.ast.Json
import org.specs2.mutable.Specification

import optics.Traversal

/** The fused `Prism.andThen(Traversal)` member, exercised where its absence used to bite: this kit
  * hand-rolls `each` through `Traversal.selfChildren` with a `case other => other` rebuild arm that
  * re-encodes the prism's miss branch by hand, because `arr.andThen(Chunks.each)` produced an
  * anonymous `Optic` that could not be named as a `Traversal`.
  *
  * Now it can be — and this spec pins that the two spellings AGREE, which is what makes the
  * hand-rolled version a measured B/op choice (it skips the bridge's `Option[X]` wrapper) rather
  * than a forced workaround.
  */
class CursorFusionSpec extends Specification:

  // The composed spelling, now nameable at the concrete type.
  val composedEach: Traversal[Json, Json, Json, Json] =
    JsonValues.arr.andThen(Chunks.each[Json, Json])

  val arrDoc: Json = Json.Arr(Json.Str("a"), Json.Str("b"))
  val notArr: Json = Json.Obj("k" -> Json.Str("a"))

  "composed vs hand-rolled each" should {
    "agree on a hit — element-wise modify" >> {
      val f: Json => Json = j => JsonValues.str.modify(_.toUpperCase)(j)
      (composedEach.modify(f)(arrDoc) === JsonValues.each.modify(f)(arrDoc))
        .and(composedEach.modify(f)(arrDoc) === Json.Arr(Json.Str("A"), Json.Str("B")))
    }
    "agree on a miss — zero foci, write passes the source through" >> {
      (composedEach.modify(_ => Json.Null)(notArr) === notArr)
        .and(composedEach.foldMap(_ => 1)(notArr) === JsonValues.each.foldMap(_ => 1)(notArr))
    }
    "agree on folds" >> {
      (composedEach.foldMap(_ => 1)(arrDoc) === 2)
        .and(composedEach.foldMap(_ => 1)(arrDoc) === JsonValues.each.foldMap(_ => 1)(arrDoc))
    }
    "compose onward on the fused path (what the anonymous Optic could not do)" >> {
      // Traversal ∘ Traversal resolves to Traversal's own fused member. NB the mirror cell,
      // Traversal ∘ Prism, is still Morph-routed and unnameable — the next fused member to add.
      val nested: Json = Json.Arr(Json.Arr(Json.Str("a")), Json.Arr(Json.Str("b")))
      val inner: Traversal[Json, Json, Json, Json] = composedEach.andThen(JsonValues.each)
      inner.modify(j => JsonValues.str.modify(_.toUpperCase)(j))(nested) ===
        Json.Arr(Json.Arr(Json.Str("A")), Json.Arr(Json.Str("B")))
    }
  }
