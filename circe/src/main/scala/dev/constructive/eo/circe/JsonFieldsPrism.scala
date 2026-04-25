package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}

import dev.constructive.eo.optics.Optic

import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}

/** Multi-field Prism sibling of [[JsonPrism]] — focuses a Scala 3 NamedTuple assembled from N named
  * fields of a parent JsonObject. Introduced in v0.2 by the [[JsonPrism.fields]] macro.
  *
  * Storage:
  *
  * {{{
  *   parentPath: Array[PathStep]  // root → parent JsonObject
  *   fieldNames: Array[String]    // selected fields in SELECTOR order
  *   encoder  : Encoder[A]        // codec for the synthesised NamedTuple A
  *   decoder  : Decoder[A]
  * }}}
  *
  * where `A` is a Scala 3 NamedTuple (opaque alias over the values tuple) built at macro time from
  * the selector list. See D5 of the multi-field plan for the selector-vs-declaration-order rule.
  *
  * Read semantics (D4 — "NamedTuple cannot be partial"):
  *   - All selected fields read cleanly → the encoder-assembled sub-object decodes to NT →
  *     `Ior.Right(nt)`.
  *   - Some selected fields miss → `Ior.Both(chain, partial)` is NOT expressible (NT cannot
  *     represent a partial read), so we emit `Ior.Left(chain)` (get) or leave the JSON unchanged
  *     with `Ior.Both(chain, inputJson)` (modify / transform / place / transfer).
  *   - The *parent* path misses → `Ior.Left(chain-of-one)` on `get`; `Ior.Left` on modify too (the
  *     parent-object carrier is missing outright; there is no sensible "apply to partial
  *     sub-object" fallback, but we still return the input Json for `modify` as a forgiving
  *     fallback — see `*Unsafe` semantic preservation).
  *
  * Write semantics:
  *   - User yields a new `NT` → encoder produces a JsonObject → each selected field is overlaid
  *     onto the parent via successive `JsonObject.add` calls → the outer path is rebuilt →
  *     `Ior.Right(newJson)`.
  *   - The encoder can't fail post-construction, so write-phase failures only surface from the
  *     parent-walk.
  *
  * Sibling to [[JsonPrism]]: no inheritance, distinct hot loops (D6). Extends the same
  * `Optic[Json, Json, A, A, Either]` supertype so cross-carrier composition works uniformly.
  *
  * See also the plan's D2 / D4 / D6 decisions for the design rationale.
  */
final class JsonFieldsPrism[A] private[circe] (
    private[circe] val parentPath: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either],
      Dynamic:

  type X = (DecodingFailure, HCursor)

  // ---- Abstract Optic members --------------------------------------
  //
  // Same shape as JsonPrism: the generic Optic extensions route
  // through these, and callers wanting the (DecodingFailure, HCursor)
  // diagnostic need an HCursor-aware `to`.

  def to: Json => Either[(DecodingFailure, HCursor), A] = json =>
    val c = navigateCursor(json)
    // Build a sub-object cursor from the selected fields under the
    // parent. We walk parent → pick each field → assemble a JsonObject
    // → re-enter as a cursor for the decoder.
    c.focus match
      case None => Left((DecodingFailure("JsonFieldsPrism.to: no focus", Nil), json.hcursor))
      case Some(parentJson) =>
        parentJson.asObject match
          case None =>
            Left(
              (DecodingFailure("JsonFieldsPrism.to: parent is not a JsonObject", Nil), json.hcursor)
            )
          case Some(obj) =>
            val subObj = buildSubObject(obj)
            Json
              .fromJsonObject(subObj)
              .as[A](using decoder) match
              case Right(a) => Right(a)
              case Left(df) => Left((df, json.hcursor))

  def from: Either[(DecodingFailure, HCursor), A] => Json = {
    case Left((_, hc)) => hc.top.getOrElse(hc.value)
    case Right(a)      => encoder(a)
  }

  // ---- Default (Ior-bearing) surface --------------------------------

  def modify(f: A => A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => modifyIor(j, f))

  def transform(f: Json => Json): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => transformIor(j, f))

  def place(a: A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, a))

  def transfer[C](f: C => A): (Json | String) => C => Ior[Chain[JsonFailure], Json] =
    input => c => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, f(c)))

  def get(input: Json | String): Ior[Chain[JsonFailure], A] =
    JsonFailure.parseInputIor(input).flatMap(getIor)

  // ---- *Unsafe escape hatches ---------------------------------------

  inline def modifyUnsafe(f: A => A): (Json | String) => Json =
    input => modifyImpl(JsonFailure.parseInputUnsafe(input), f)

  inline def transformUnsafe(f: Json => Json): (Json | String) => Json =
    input => transformImpl(JsonFailure.parseInputUnsafe(input), f)

  inline def placeUnsafe(a: A): (Json | String) => Json =
    input => placeImpl(JsonFailure.parseInputUnsafe(input), a)

  inline def transferUnsafe[C](f: C => A): (Json | String) => C => Json =
    input => c => placeImpl(JsonFailure.parseInputUnsafe(input), f(c))

  inline def getOptionUnsafe(input: Json | String): Option[A] =
    getOptionUnsafeImpl(JsonFailure.parseInputUnsafe(input))

  // ---- Shared helpers ----------------------------------------------

  /** Build an ACursor pointing at the parent JsonObject (root + parentPath). */
  private def navigateCursor(json: Json): ACursor =
    var c: ACursor = json.hcursor
    var i = 0
    while i < parentPath.length do
      c = parentPath(i) match
        case PathStep.Field(name) => c.downField(name)
        case PathStep.Index(idx)  => c.downN(idx)
      i += 1
    c

  /** Pick the selected fields out of `obj` into a fresh JsonObject, preserving selector order.
    * Missing fields become `Json.Null` entries — the caller decides whether that's acceptable (the
    * decode-path checks for missing-ness explicitly before assembly).
    */
  private def buildSubObject(obj: JsonObject): JsonObject =
    var sub: JsonObject = JsonObject.empty
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      val v = obj(name).getOrElse(Json.Null)
      sub = sub.add(name, v)
      i += 1
    sub

  /** Walk the parent path *and* collect per-step parents into the supplied buffer. Returns the
    * parent JsonObject on success, or a single-step failure reason if the walk misses. The buffer
    * is populated only up to the step that succeeded — callers inspect `parents` only after a
    * successful return.
    */
  private def walkParent(
      json: Json,
      parents: Array[AnyRef],
  ): Either[JsonFailure, JsonObject] =
    var cur: Json = json
    var i = 0
    while i < parentPath.length do
      val step = parentPath(i)
      step match
        case PathStep.Field(name) =>
          cur.asObject match
            case None      => return Left(JsonFailure.NotAnObject(step))
            case Some(obj) =>
              parents(i) = obj
              obj(name) match
                case None    => return Left(JsonFailure.PathMissing(step))
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None      => return Left(JsonFailure.NotAnArray(step))
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then
                return Left(JsonFailure.IndexOutOfRange(step, arr.length))
              parents(i) = arr
              cur = arr(idx)
      i += 1
    // Terminal: cur must be a JsonObject to host the selected fields.
    cur.asObject match
      case None =>
        val terminalStep =
          if parentPath.length == 0 then PathStep.Field("") else parentPath(parentPath.length - 1)
        Left(JsonFailure.NotAnObject(terminalStep))
      case Some(obj) => Right(obj)

  /** Read each selected field from `obj`. Returns either the assembled sub-object (for decoder
    * feed) or a Chain of per-field failures. Missing fields accumulate as PathMissing.
    */
  private def readFields(
      obj: JsonObject
  ): Either[Chain[JsonFailure], JsonObject] =
    var chain: Chain[JsonFailure] = Chain.empty
    var sub: JsonObject = JsonObject.empty
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      obj(name) match
        case None =>
          chain = chain :+ JsonFailure.PathMissing(PathStep.Field(name))
        case Some(v) =>
          sub = sub.add(name, v)
      i += 1
    if chain.isEmpty then Right(sub) else Left(chain)

  /** Given a computed NamedTuple `a`, produce a new parent JsonObject with each selected field
    * overlaid. The encoder runs once; the resulting JsonObject is split back into individual (name,
    * Json) pairs that we overlay onto `parent` via successive `JsonObject.add` calls.
    */
  private def writeFields(parent: JsonObject, a: A): JsonObject =
    val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
    var out: JsonObject = parent
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      encoded(name) match
        case Some(v) => out = out.add(name, v)
        case None    =>
          // Encoder omitted a field; we don't overlay anything for it.
          // This is unusual — NamedTuple codecs should always write every
          // selected field — but we tolerate it silently for now.
          ()
      i += 1
    out

  /** Rebuild the root Json by unwinding the parent-path with a newly-synthesised JsonObject at the
    * terminal step.
    */
  private def rebuild(newParent: JsonObject, parents: Array[AnyRef]): Json =
    var newChild: Json = Json.fromJsonObject(newParent)
    var j = parentPath.length - 1
    while j >= 0 do
      newChild = rebuildStep(parents(j), parentPath(j), newChild)
      j -= 1
    newChild

  private inline def rebuildStep(
      parent: AnyRef,
      step: PathStep,
      child: Json,
  ): Json =
    step match
      case PathStep.Field(name) =>
        Json.fromJsonObject(parent.asInstanceOf[JsonObject].add(name, child))
      case PathStep.Index(idx) =>
        Json.fromValues(parent.asInstanceOf[Vector[Json]].updated(idx, child))

  // ---- *Unsafe impls (pre-v0.2-shape forgiving bodies) -------------

  private def modifyImpl(json: Json, f: A => A): Json =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(_)    => json
      case Right(obj) =>
        readFields(obj) match
          case Left(_)    => json
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Left(_)  => json
              case Right(a) =>
                val newA = f(a)
                val newParent = writeFields(obj, newA)
                rebuild(newParent, parents)

  private def transformImpl(json: Json, f: Json => Json): Json =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(_)    => json
      case Right(obj) =>
        // transform operates on the raw sub-object Json — assemble, apply f,
        // then overlay each key back. Missing fields become Json.Null in
        // the sub-object the user receives.
        val sub = buildSubObject(obj)
        val newSubJson = f(Json.fromJsonObject(sub))
        newSubJson.asObject match
          case None         => json // f produced a non-object; can't write back
          case Some(newSub) =>
            var newParent: JsonObject = obj
            var i = 0
            while i < fieldNames.length do
              val name = fieldNames(i)
              newSub(name) match
                case Some(v) => newParent = newParent.add(name, v)
                case None    => ()
              i += 1
            rebuild(newParent, parents)

  private def placeImpl(json: Json, a: A): Json =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(_)    => json
      case Right(obj) =>
        val newParent = writeFields(obj, a)
        rebuild(newParent, parents)

  private def getOptionUnsafeImpl(json: Json): Option[A] =
    val parents = new Array[AnyRef](parentPath.length)
    walkParent(json, parents) match
      case Left(_)    => None
      case Right(obj) =>
        readFields(obj) match
          case Left(_)    => None
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder).toOption

  // ---- Ior-bearing impls -------------------------------------------

  private def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(failure) => Ior.Both(Chain.one(failure), json)
      case Right(obj)    =>
        readFields(obj) match
          case Left(chain) =>
            // D4: atomicity — partial reads can't assemble NT.
            Ior.Both(chain, json)
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Left(df) =>
                // Terminal decode on the assembled NT failed; point at
                // the parent path (or root-step sentinel for a path-empty
                // prism).
                val step =
                  if n == 0 then PathStep.Field("") else parentPath(n - 1)
                Ior.Both(Chain.one(JsonFailure.DecodeFailed(step, df)), json)
              case Right(a) =>
                val newA = f(a)
                val newParent = writeFields(obj, newA)
                Ior.Right(rebuild(newParent, parents))

  private def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(failure) => Ior.Both(Chain.one(failure), json)
      case Right(obj)    =>
        val sub = buildSubObject(obj)
        val newSubJson = f(Json.fromJsonObject(sub))
        newSubJson.asObject match
          case None =>
            // transform produced a non-object; can't write back. Synthesise
            // a NotAnObject at the terminal step.
            val step =
              if n == 0 then PathStep.Field("") else parentPath(n - 1)
            Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), json)
          case Some(newSub) =>
            var newParent: JsonObject = obj
            var i = 0
            while i < fieldNames.length do
              val name = fieldNames(i)
              newSub(name) match
                case Some(v) => newParent = newParent.add(name, v)
                case None    => ()
              i += 1
            Ior.Right(rebuild(newParent, parents))

  private def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    val n = parentPath.length
    val parents = new Array[AnyRef](n)
    walkParent(json, parents) match
      case Left(failure) => Ior.Both(Chain.one(failure), json)
      case Right(obj)    =>
        val newParent = writeFields(obj, a)
        Ior.Right(rebuild(newParent, parents))

  private def getIor(json: Json): Ior[Chain[JsonFailure], A] =
    val parents = new Array[AnyRef](parentPath.length)
    walkParent(json, parents) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(obj)    =>
        readFields(obj) match
          case Left(chain) => Ior.Left(chain)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(a)
              case Left(df) =>
                val step =
                  if parentPath.length == 0 then PathStep.Field("")
                  else parentPath(parentPath.length - 1)
                Ior.Left(Chain.one(JsonFailure.DecodeFailed(step, df)))
