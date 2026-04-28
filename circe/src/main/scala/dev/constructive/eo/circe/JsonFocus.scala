package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, Json, JsonObject}

/** Focus on a value of type `A` somewhere inside a `Json`. Storage decomposition along two axes:
  *
  *   1. Where the focus lives — at a leaf reached by a path (Leaf), or as a NamedTuple assembled
  *      from selected fields under a parent record (Fields).
  *   2. Single- vs multi-focus — applied once at the root ([[JsonPrism]]) or per-element after a
  *      prefix walk ([[JsonTraversal]]).
  *
  * Each focus exposes six per-Json ops (three Ior-bearing default + three silent) plus two reads
  * and Optic-trait `to`-side hooks (`navigateCursor` / `decodeFromCursor`).
  */
sealed abstract private[circe] class JsonFocus[A]:

  /** Codec for `A`. Leaf: user's `Encoder[A]` / `Decoder[A]`. Fields: synthesised NT codec. */
  private[circe] def encoder: Encoder[A]
  private[circe] def decoder: Decoder[A]

  /** Terminal path step — used by failure surfaces. `PathStep.Field("")` for root-level focuses. */
  protected def terminalStep: PathStep

  /** Decode `cur` and fold success into `onHit`, or surface a `DecodeFailed`. Shared between Leaf
    * and Fields.
    */
  final protected def decodeOrFail(
      cur: Json,
      json: Json,
  )(onHit: A => Json): Ior[Chain[JsonFailure], Json] =
    decoder.decodeJson(cur) match
      case Right(a) => Ior.Right(onHit(a))
      case Left(df) => Ior.Both(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)), json)

  /** Read counterpart to [[decodeOrFail]] — decode → `Ior.Right(a)` or `Ior.Left(chain)`. */
  final protected def decodeOrLeft(cur: Json): Ior[Chain[JsonFailure], A] =
    decoder.decodeJson(cur) match
      case Right(a) => Ior.Right(a)
      case Left(df) => Ior.Left(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)))

  // Default Ior-bearing ops — failures surface as `Ior.Both(chain, inputJson)` (input preserved as
  // silent fallback). `readIor` returns `Ior.Left(chain)` on failure.
  def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json]
  def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json]
  def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json]
  def readIor(json: Json): Ior[Chain[JsonFailure], A]

  // *Unsafe (silent) ops — input pass-through on any failure (read returns None).
  def modifyImpl(json: Json, f: A => A): Json
  def transformImpl(json: Json, f: Json => Json): Json
  def placeImpl(json: Json, a: A): Json
  def readImpl(json: Json): Option[A]

  /** Build the ACursor for the abstract `to` method. */
  def navigateCursor(json: Json): ACursor

  /** Decode an `A` from the navigated `ACursor`. */
  def decodeFromCursor(c: ACursor): Either[DecodingFailure, A] = c.as[A](using decoder)

private[circe] object JsonFocus:

  /** Single-leaf focus — reaches `A` via `path` and reads / writes through the user-supplied codec.
    * Powers root-level `JsonPrism[A]` and per-element steps of `JsonTraversal[A]`.
    */
  final class Leaf[A] private[circe] (
      private[circe] val path: Array[PathStep],
      private[circe] val encoder: Encoder[A],
      private[circe] val decoder: Decoder[A],
  ) extends JsonFocus[A]:

    protected def terminalStep: PathStep = JsonWalk.terminalOf(path)

    def navigateCursor(json: Json): ACursor =
      var c: ACursor = json.hcursor
      var i = 0
      while i < path.length do
        c = path(i) match
          case PathStep.Field(name) => c.downField(name)
          case PathStep.Index(idx)  => c.downN(idx)
        i += 1
      c

    def modifyImpl(json: Json, f: A => A): Json =
      JsonWalk.walkPath(json, path) match
        case Left(_)               => json
        case Right((cur, parents)) =>
          decoder.decodeJson(cur) match
            case Left(_)  => json
            case Right(a) => JsonWalk.rebuildPath(parents, path, encoder(f(a)))

    def transformImpl(json: Json, f: Json => Json): Json =
      JsonWalk.walkPath(json, path) match
        case Left(_)               => json
        case Right((cur, parents)) => JsonWalk.rebuildPath(parents, path, f(cur))

    def placeImpl(json: Json, a: A): Json =
      if path.length == 0 then encoder(a)
      else
        JsonWalk.walkPath(json, path) match
          case Left(_)             => json
          case Right((_, parents)) => JsonWalk.rebuildPath(parents, path, encoder(a))

    def readImpl(json: Json): Option[A] =
      JsonWalk.walkPath(json, path).toOption.flatMap(s => decoder.decodeJson(s._1).toOption)

    def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
      JsonWalk.walkPath(json, path) match
        case Left(failure)         => Ior.Both(Chain.one(failure), json)
        case Right((cur, parents)) =>
          decodeOrFail(cur, json)(a => JsonWalk.rebuildPath(parents, path, encoder(f(a))))

    def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
      JsonWalk.walkPath(json, path) match
        case Left(failure)         => Ior.Both(Chain.one(failure), json)
        case Right((cur, parents)) =>
          Ior.Right(JsonWalk.rebuildPath(parents, path, f(cur)))

    def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
      if path.length == 0 then Ior.Right(encoder(a))
      else
        JsonWalk.walkPath(json, path) match
          case Left(failure)       => Ior.Both(Chain.one(failure), json)
          case Right((_, parents)) =>
            Ior.Right(JsonWalk.rebuildPath(parents, path, encoder(a)))

    def readIor(json: Json): Ior[Chain[JsonFailure], A] =
      JsonWalk.walkPath(json, path) match
        case Left(failure)   => Ior.Left(Chain.one(failure))
        case Right((cur, _)) => decodeOrLeft(cur)

  /** Multi-field focus — reaches a parent `JsonObject` via `parentPath`, then assembles a
    * NamedTuple `A` from `fieldNames`. All-or-nothing: a partial read accumulates one `PathMissing`
    * per missing field and the modify family leaves the input unchanged.
    */
  final class Fields[A] private[circe] (
      private[circe] val parentPath: Array[PathStep],
      private[circe] val fieldNames: Array[String],
      private[circe] val encoder: Encoder[A],
      private[circe] val decoder: Decoder[A],
  ) extends JsonFocus[A]:

    protected def terminalStep: PathStep = JsonWalk.terminalOf(parentPath)

    def navigateCursor(json: Json): ACursor =
      parentPath.foldLeft[ACursor](json.hcursor) { (c, step) =>
        step match
          case PathStep.Field(name) => c.downField(name)
          case PathStep.Index(idx)  => c.downN(idx)
      }

    /** Non-atomic projection — missing → `Json.Null`. Used by transform-shape ops. */
    private def buildSubObject(obj: JsonObject): JsonObject =
      fieldNames.foldLeft(JsonObject.empty)((sub, name) =>
        sub.add(name, obj(name).getOrElse(Json.Null))
      )

    /** Atomic read — succeeds only when ALL fields are present; missing → `PathMissing`. */
    private def readFields(obj: JsonObject): Either[Chain[JsonFailure], JsonObject] =
      val (chain, sub) =
        fieldNames.toVector.foldLeft((Chain.empty[JsonFailure], JsonObject.empty)) {
          case ((chain, sub), name) =>
            obj(name) match
              case None    => (chain :+ JsonFailure.PathMissing(PathStep.Field(name)), sub)
              case Some(v) => (chain, sub.add(name, v))
        }
      Either.cond(chain.isEmpty, sub, chain)

    /** Overlay the encoder's output for `a` onto `parent`; encoders that omit a selected field
      * leave the parent's entry untouched.
      */
    private def writeFields(parent: JsonObject, a: A): JsonObject =
      val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
      fieldNames.foldLeft(parent)((out, name) => encoded(name).fold(out)(v => out.add(name, v)))

    /** Overlay a foreign sub-object (transform's result) onto `parent`. */
    private def overlayFields(parent: JsonObject, newSub: JsonObject): JsonObject =
      fieldNames.foldLeft(parent)((out, name) => newSub(name).fold(out)(v => out.add(name, v)))

    /** Walk the parent path and project the terminal as a JsonObject. */
    private def walkParent(json: Json): Either[JsonFailure, (JsonObject, Vector[AnyRef])] =
      JsonWalk.walkPath(json, parentPath).flatMap {
        case (cur, parents) =>
          cur
            .asObject
            .toRight(JsonFailure.NotAnObject(terminalStep))
            .map(obj => (obj, parents))
      }

    private def rebuild(newParent: JsonObject, parents: Vector[AnyRef]): Json =
      JsonWalk.rebuildPath(parents, parentPath, Json.fromJsonObject(newParent))

    def modifyImpl(json: Json, f: A => A): Json =
      walkParent(json) match
        case Left(_)               => json
        case Right((obj, parents)) =>
          readFields(obj)
            .flatMap(sub =>
              Json.fromJsonObject(sub).as[A](using decoder).left.map(_ => Chain.empty)
            )
            .map(a => rebuild(writeFields(obj, f(a)), parents))
            .getOrElse(json)

    def transformImpl(json: Json, f: Json => Json): Json =
      walkParent(json) match
        case Left(_)               => json
        case Right((obj, parents)) =>
          f(Json.fromJsonObject(buildSubObject(obj)))
            .asObject
            .map(newSub => rebuild(overlayFields(obj, newSub), parents))
            .getOrElse(json)

    def placeImpl(json: Json, a: A): Json =
      walkParent(json) match
        case Left(_)               => json
        case Right((obj, parents)) => rebuild(writeFields(obj, a), parents)

    def readImpl(json: Json): Option[A] =
      walkParent(json).toOption.flatMap {
        case (obj, _) =>
          readFields(obj)
            .toOption
            .flatMap(sub => Json.fromJsonObject(sub).as[A](using decoder).toOption)
      }

    def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
      walkParent(json) match
        case Left(failure)         => Ior.Both(Chain.one(failure), json)
        case Right((obj, parents)) =>
          readFields(obj) match
            case Left(chain) => Ior.Both(chain, json)
            case Right(sub)  =>
              decodeOrFail(Json.fromJsonObject(sub), json)(a =>
                rebuild(writeFields(obj, f(a)), parents)
              )

    def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
      walkParent(json) match
        case Left(failure)         => Ior.Both(Chain.one(failure), json)
        case Right((obj, parents)) =>
          f(Json.fromJsonObject(buildSubObject(obj))).asObject match
            case None         => Ior.Both(Chain.one(JsonFailure.NotAnObject(terminalStep)), json)
            case Some(newSub) => Ior.Right(rebuild(overlayFields(obj, newSub), parents))

    def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
      walkParent(json) match
        case Left(failure)         => Ior.Both(Chain.one(failure), json)
        case Right((obj, parents)) => Ior.Right(rebuild(writeFields(obj, a), parents))

    def readIor(json: Json): Ior[Chain[JsonFailure], A] =
      walkParent(json) match
        case Left(failure)   => Ior.Left(Chain.one(failure))
        case Right((obj, _)) =>
          readFields(obj) match
            case Left(chain) => Ior.Left(chain)
            case Right(sub)  => decodeOrLeft(Json.fromJsonObject(sub))
