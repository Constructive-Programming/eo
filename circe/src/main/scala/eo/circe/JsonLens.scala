package eo.circe

import eo.optics.SplitCombineLens

import io.circe.{Encoder, Json, JsonObject}

/** A **cross-representation lens** that bridges a native Scala source
  * type `S` (a case class, typically) and its circe-serialised form
  * `JsonObject`. The native field of type `A` is read from `S` on the
  * `to` side; on the write side the lens takes a `Json` value and
  * re-attaches it to the `JsonObject` target at the same field name.
  *
  * The polymorphic shape `Optic[S, JsonObject, A, Json, Tuple2]` is
  * what unlocks the flagship operation:
  *
  * {{{
  *   // `transform` operates purely at the JsonObject level —
  *   // the S-side decoder never runs, and only the focused field
  *   // pays any per-call cost.
  *   val out: JsonObject = nameL.transform(_.mapString(_.toUpperCase))(json)
  * }}}
  *
  * Compare with the naïve round-trip (decode → copy → encode), which
  * spends O(all fields of S) reading and re-emitting the whole record
  * just to tweak one value. `JsonLens` collapses that to O(one field
  * access on a JsonObject) by exposing `transform` / `place` /
  * `transfer` directly on the class body, using the stored
  * `fieldName` to locate the focused entry.
  *
  * Forgiving missing-field semantics: if the target JsonObject does
  * not contain the focused field key (or its value at that key is
  * absent), the complement presents `Json.Null` as the current value
  * and the user's transformation runs on that. Writing back adds the
  * key if it was missing. A strict / affine variant (JsonAffine)
  * that surfaces missing fields as a failure is on the roadmap.
  */
final class JsonLens[S, A](
    val fieldName: String,
    _get:          S => A,
    enc:           Encoder.AsObject[S],
) extends SplitCombineLens[S, JsonObject, A, Json, JsonObject](
      _get,
      s =>
        val obj = enc.encodeObject(s)
        (obj.remove(fieldName), _get(s)),
      (complement, fieldJson) =>
        complement.add(fieldName, fieldJson),
    ):

  /** Modify the focused field directly on a JsonObject target. O(1)
    * — no S-side decode, no re-encoding of the other fields. */
  inline def transform(f: Json => Json): JsonObject => JsonObject =
    obj =>
      val current = obj(fieldName).getOrElse(Json.Null)
      obj.add(fieldName, f(current))

  /** Replace the focused field on a JsonObject target with a
    * caller-supplied Json value. Creates the key if it was absent. */
  inline def place(b: Json): JsonObject => JsonObject =
    _.add(fieldName, b)

  /** Curried placement: given a source `C` and a `C => Json` encoder,
    * produce a `JsonObject => JsonObject` that writes `f(c)` into the
    * focused field. Handy when the "new value" is computed from
    * something outside the JsonObject — a request body, a
    * calculation, etc. */
  inline def transfer[C](f: C => Json): JsonObject => C => JsonObject =
    obj => c => obj.add(fieldName, f(c))

object JsonLens:

  /** Construct a `JsonLens` from a JSON key and a native selector.
    *
    * @param fieldName the JSON key for `selector(s)` in the serialised
    *                  form of `S`. The macro (future work) will derive
    *                  this from the selector's AST and kindlings'
    *                  `Configuration`; for now the caller supplies it.
    * @param selector  the native accessor — `(_.name)` for a case
    *                  class field.
    */
  def apply[S, A](
      fieldName: String,
      selector:  S => A,
  )(using
      enc: Encoder.AsObject[S],
  ): JsonLens[S, A] =
    new JsonLens[S, A](fieldName, selector, enc)
