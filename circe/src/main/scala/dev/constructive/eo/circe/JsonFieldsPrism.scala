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
      JsonOpticOps[A],
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

  // ---- Read surface (single-focus specific) -------------------------

  def get(input: Json | String): Ior[Chain[JsonFailure], A] =
    JsonFailure.parseInputIor(input).flatMap(getIor)

  inline def getOptionUnsafe(input: Json | String): Option[A] =
    getOptionUnsafeImpl(JsonFailure.parseInputUnsafe(input))

  // ---- Shared helpers ----------------------------------------------

  /** Build an ACursor pointing at the parent JsonObject (root + parentPath). */
  private def navigateCursor(json: Json): ACursor =
    parentPath.foldLeft[ACursor](json.hcursor) { (c, step) =>
      step match
        case PathStep.Field(name) => c.downField(name)
        case PathStep.Index(idx)  => c.downN(idx)
    }

  /** Pick the selected fields out of `obj` into a fresh JsonObject, preserving selector order.
    * Missing fields become `Json.Null` entries — the caller decides whether that's acceptable (the
    * decode-path checks for missing-ness explicitly before assembly).
    */
  private def buildSubObject(obj: JsonObject): JsonObject =
    fieldNames.foldLeft(JsonObject.empty) { (sub, name) =>
      sub.add(name, obj(name).getOrElse(Json.Null))
    }

  /** Walk the parent path *and* collect per-step parents into the supplied buffer. Returns the
    * parent JsonObject on success, or a single-step failure reason if the walk misses. The buffer
    * is populated only up to the step that succeeded — callers inspect `parents` only after a
    * successful return.
    */
  /** Walk parentPath via [[JsonWalk.walkPath]] and project the terminal value as a JsonObject so
    * the selected-field reads / overlays can proceed. Returns the parent JsonObject paired with the
    * Vector of parents collected along the path — the rebuild phase needs both.
    */
  private def walkParent(json: Json): Either[JsonFailure, (JsonObject, Vector[AnyRef])] =
    JsonWalk.walkPath(json, parentPath).flatMap {
      case (cur, parents) =>
        cur
          .asObject
          .toRight(JsonFailure.NotAnObject(JsonWalk.terminalOf(parentPath)))
          .map(obj => (obj, parents))
    }

  /** Read each selected field from `obj`. Returns either the assembled sub-object (for decoder
    * feed) or a Chain of per-field failures. Missing fields accumulate as PathMissing.
    */
  private def readFields(
      obj: JsonObject
  ): Either[Chain[JsonFailure], JsonObject] =
    val (chain, sub) = fieldNames.toVector.foldLeft((Chain.empty[JsonFailure], JsonObject.empty)) {
      case ((chain, sub), name) =>
        obj(name) match
          case None    => (chain :+ JsonFailure.PathMissing(PathStep.Field(name)), sub)
          case Some(v) => (chain, sub.add(name, v))
    }
    Either.cond(chain.isEmpty, sub, chain)

  /** Given a computed NamedTuple `a`, produce a new parent JsonObject with each selected field
    * overlaid. The encoder runs once; the resulting JsonObject is split back into individual (name,
    * Json) pairs that we overlay onto `parent` via successive `JsonObject.add` calls.
    *
    * The encoder is expected to produce every selected field; encoders that silently omit a field
    * leave the corresponding entry on `parent` untouched (this mirrors the pre-FP body's silent
    * tolerance).
    */
  private def writeFields(parent: JsonObject, a: A): JsonObject =
    val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
    fieldNames.toVector.foldLeft(parent) { (out, name) =>
      encoded(name).fold(out)(v => out.add(name, v))
    }

  /** Overlay the fields of `newSub` onto `parent` via successive `JsonObject.add` — same shape as
    * [[writeFields]] but starting from a `JsonObject` produced by `f` in `transform`.
    */
  private def overlayFields(parent: JsonObject, newSub: JsonObject): JsonObject =
    fieldNames.toVector.foldLeft(parent) { (out, name) =>
      newSub(name).fold(out)(v => out.add(name, v))
    }

  /** Rebuild the root Json by splicing `newParent` through the collected parents. */
  private def rebuild(newParent: JsonObject, parents: Vector[AnyRef]): Json =
    JsonWalk.rebuildPath(parents, parentPath, Json.fromJsonObject(newParent))

  // ---- *Unsafe impls (pre-v0.2-shape forgiving bodies) -------------

  override protected def modifyImpl(json: Json, f: A => A): Json =
    walkParent(json) match
      case Left(_)               => json
      case Right((obj, parents)) =>
        readFields(obj)
          .flatMap(sub => Json.fromJsonObject(sub).as[A](using decoder).left.map(_ => Chain.empty))
          .map(a => rebuild(writeFields(obj, f(a)), parents))
          .getOrElse(json)

  override protected def transformImpl(json: Json, f: Json => Json): Json =
    walkParent(json) match
      case Left(_)               => json
      case Right((obj, parents)) =>
        // Assemble a sub-object, apply f, overlay back. Non-object f-output → input unchanged.
        f(Json.fromJsonObject(buildSubObject(obj)))
          .asObject
          .map(newSub => rebuild(overlayFields(obj, newSub), parents))
          .getOrElse(json)

  override protected def placeImpl(json: Json, a: A): Json =
    walkParent(json) match
      case Left(_)               => json
      case Right((obj, parents)) => rebuild(writeFields(obj, a), parents)

  private def getOptionUnsafeImpl(json: Json): Option[A] =
    walkParent(json).toOption.flatMap {
      case (obj, _) =>
        readFields(obj)
          .toOption
          .flatMap(sub => Json.fromJsonObject(sub).as[A](using decoder).toOption)
    }

  // ---- Ior-bearing impls -------------------------------------------

  override protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    walkParent(json) match
      case Left(failure)         => Ior.Both(Chain.one(failure), json)
      case Right((obj, parents)) =>
        readFields(obj) match
          // D4: atomicity — partial reads can't assemble NT.
          case Left(chain) => Ior.Both(chain, json)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(rebuild(writeFields(obj, f(a)), parents))
              case Left(df) =>
                Ior.Both(
                  Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(parentPath), df)),
                  json,
                )

  override protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    walkParent(json) match
      case Left(failure)         => Ior.Both(Chain.one(failure), json)
      case Right((obj, parents)) =>
        f(Json.fromJsonObject(buildSubObject(obj))).asObject match
          // f produced a non-object; synthesise a NotAnObject at the terminal step.
          case None =>
            Ior.Both(Chain.one(JsonFailure.NotAnObject(JsonWalk.terminalOf(parentPath))), json)
          case Some(newSub) => Ior.Right(rebuild(overlayFields(obj, newSub), parents))

  override protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    walkParent(json) match
      case Left(failure)         => Ior.Both(Chain.one(failure), json)
      case Right((obj, parents)) => Ior.Right(rebuild(writeFields(obj, a), parents))

  private def getIor(json: Json): Ior[Chain[JsonFailure], A] =
    walkParent(json) match
      case Left(failure)   => Ior.Left(Chain.one(failure))
      case Right((obj, _)) =>
        readFields(obj) match
          case Left(chain) => Ior.Left(chain)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(a)
              case Left(df) =>
                Ior.Left(
                  Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(parentPath), df))
                )
