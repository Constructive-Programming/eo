package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import dev.constructive.eo.widenRight
import io.circe.{Decoder, Encoder, Json, JsonObject}

/** Focus on a value of type `A` somewhere inside a `Json`. Storage decomposition along two axes:
  *
  *   1. Where the focus lives — at a leaf reached by a path (Leaf), or as a NamedTuple assembled
  *      from selected fields under a parent record (Fields).
  *   2. Single- vs multi-focus — applied once at the root ([[JsonPrism]]) or per-element after a
  *      prefix walk ([[JsonTraversal]]).
  *
  * Each focus exposes six per-Json ops (three Ior-bearing default + three silent) plus two reads.
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

  /** Navigate + decode the focus in ONE walk, and capture a writer that places a new focus back
    * into the SOURCE json (preserving siblings) — backs [[JsonPrism]]'s `Affine` `to`/`from` seam
    * so a generic/composed `.modify` rebuilds around the original document rather than
    * reconstructing the focus standalone. `Left` on navigate/decode failure. Mirrors
    * `dev.constructive.eo.avro.AvroFocus.navigateForWrite`.
    */
  def navigateForWrite(json: Json): Either[JsonFailure, (A, A => Json)]

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

    def navigateForWrite(json: Json): Either[JsonFailure, (A, A => Json)] =
      if path.length == 0 then
        decoder.decodeJson(json) match
          case Left(df) => Left(JsonFailure.DecodeFailed(terminalStep, df))
          case Right(a) =>
            // Root full-cover: the writer IS the old reverseGet (encode standalone).
            Right((a, (b: A) => encoder(b)))
      else
        JsonWalk.readPath(json, path) match
          case l @ Left(_) => l.widenRight
          case Right(cur)  =>
            decoder.decodeJson(cur) match
              case Left(df) => Left(JsonFailure.DecodeFailed(terminalStep, df))
              case Right(a) =>
                // Deferred writer: re-walk on invocation (fused, allocation-free per hop)
                // instead of capturing parents. The walk above succeeded on this same
                // immutable json + path, so the re-walk cannot miss; the fallback is dead.
                Right(
                  (a, (b: A) => JsonWalk.modifyPath(json, path)(_ => encoder(b)).getOrElse(json))
                )

    def modifyImpl(json: Json, f: A => A): Json =
      // ONE fused walk: decode + re-encode happen at the terminal frame; a decode failure
      // aborts via miss so the input passes through untouched (no partial rebuild).
      if path.length == 0 then decoder.decodeJson(json).map(a => encoder(f(a))).getOrElse(json)
      else
        JsonWalk
          .modifyPath(json, path) { cur =>
            decoder.decodeJson(cur) match
              case Right(a) => encoder(f(a))
              case Left(df) => JsonWalk.miss(JsonFailure.DecodeFailed(terminalStep, df))
          }
          .getOrElse(json)

    def transformImpl(json: Json, f: Json => Json): Json =
      JsonWalk.modifyPath(json, path)(f).getOrElse(json)

    def placeImpl(json: Json, a: A): Json =
      if path.length == 0 then encoder(a)
      else JsonWalk.modifyPath(json, path)(_ => encoder(a)).getOrElse(json)

    def readImpl(json: Json): Option[A] =
      JsonWalk.readPath(json, path).toOption.flatMap(cur => decoder.decodeJson(cur).toOption)

    def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
      JsonWalk.modifyPath(json, path) { cur =>
        decoder.decodeJson(cur) match
          case Right(a) => encoder(f(a))
          case Left(df) => JsonWalk.miss(JsonFailure.DecodeFailed(terminalStep, df))
      } match
        case Right(out)    => Ior.Right(out)
        case Left(failure) => Ior.Both(Chain.one(failure), json)

    def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
      JsonWalk.modifyPath(json, path)(f) match
        case Right(out)    => Ior.Right(out)
        case Left(failure) => Ior.Both(Chain.one(failure), json)

    def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
      if path.length == 0 then Ior.Right(encoder(a))
      else
        JsonWalk.modifyPath(json, path)(_ => encoder(a)) match
          case Right(out)    => Ior.Right(out)
          case Left(failure) => Ior.Both(Chain.one(failure), json)

    def readIor(json: Json): Ior[Chain[JsonFailure], A] =
      JsonWalk.readPath(json, path) match
        case Left(failure) => Ior.Left(Chain.one(failure))
        case Right(cur)    => decodeOrLeft(cur)

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
      overlayFields(parent, encoder(a).asObject.getOrElse(JsonObject.empty))

    /** Overlay a foreign sub-object (transform's result) onto `parent`. */
    private def overlayFields(parent: JsonObject, newSub: JsonObject): JsonObject =
      fieldNames.foldLeft(parent)((out, name) => newSub(name).fold(out)(v => out.add(name, v)))

    /** Walk the parent path (read-only) and project the terminal as a JsonObject. */
    private def readParent(json: Json): Either[JsonFailure, JsonObject] =
      JsonWalk.readPath(json, parentPath).flatMap { cur =>
        cur.asObject.toRight(JsonFailure.NotAnObject(terminalStep))
      }

    /** Fused walk + parent-object splice: `compute` sees the walked parent and returns its
      * replacement; a non-object terminal aborts via miss.
      */
    private def spliceParent(
        json: Json
    )(compute: JsonObject => JsonObject): Either[JsonFailure, Json] =
      JsonWalk.modifyPath(json, parentPath) { cur =>
        cur.asObject match
          case Some(obj) => Json.fromJsonObject(compute(obj))
          case None      => JsonWalk.miss(JsonFailure.NotAnObject(terminalStep))
      }

    def navigateForWrite(json: Json): Either[JsonFailure, (A, A => Json)] =
      readParent(json) match
        case l @ Left(_) => l.widenRight
        case Right(obj)  =>
          readFields(obj) match
            case Left(chain) =>
              Left(chain.headOption.getOrElse(JsonFailure.PathMissing(terminalStep)))
            case Right(sub) =>
              Json.fromJsonObject(sub).as[A](using decoder) match
                case Left(df) => Left(JsonFailure.DecodeFailed(terminalStep, df))
                case Right(a) =>
                  // Writer overlays the NT fields BY NAME onto the resolved parent object,
                  // re-walking (fused, per-hop-allocation-free) on invocation.
                  Right((a, (b: A) => spliceParent(json)(_ => writeFields(obj, b)).getOrElse(json)))

    def modifyImpl(json: Json, f: A => A): Json =
      // ONE fused walk: fields read + decode + overlay happen at the parent frame; any
      // failure aborts via miss and the input passes through. NB the *Ior* modify stays
      // separate — it must surface readFields' accumulated PathMissing Chain, which this
      // silent surface collapses to pass-through.
      spliceParent(json) { obj =>
        readFields(obj) match
          case Left(chain) =>
            JsonWalk.miss(chain.headOption.getOrElse(JsonFailure.PathMissing(terminalStep)))
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Left(df) => JsonWalk.miss(JsonFailure.DecodeFailed(terminalStep, df))
              case Right(a) => writeFields(obj, f(a))
      }.getOrElse(json)

    def transformImpl(json: Json, f: Json => Json): Json =
      spliceParent(json) { obj =>
        f(Json.fromJsonObject(buildSubObject(obj))).asObject match
          case Some(newSub) => overlayFields(obj, newSub)
          case None         => JsonWalk.miss(JsonFailure.NotAnObject(terminalStep))
      }.getOrElse(json)

    def placeImpl(json: Json, a: A): Json =
      spliceParent(json)(obj => writeFields(obj, a)).getOrElse(json)

    def readImpl(json: Json): Option[A] =
      readParent(json).toOption.flatMap { obj =>
        readFields(obj)
          .toOption
          .flatMap(sub => Json.fromJsonObject(sub).as[A](using decoder).toOption)
      }

    def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
      readParent(json) match
        case Left(failure) => Ior.Both(Chain.one(failure), json)
        case Right(obj)    =>
          readFields(obj) match
            case Left(chain) => Ior.Both(chain, json)
            case Right(sub)  =>
              decodeOrFail(Json.fromJsonObject(sub), json)(a =>
                spliceParent(json)(_ => writeFields(obj, f(a))).getOrElse(json)
              )

    def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
      spliceParent(json) { obj =>
        f(Json.fromJsonObject(buildSubObject(obj))).asObject match
          case Some(newSub) => overlayFields(obj, newSub)
          case None         => JsonWalk.miss(JsonFailure.NotAnObject(terminalStep))
      } match
        case Right(out)    => Ior.Right(out)
        case Left(failure) => Ior.Both(Chain.one(failure), json)

    def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
      spliceParent(json)(obj => writeFields(obj, a)) match
        case Right(out)    => Ior.Right(out)
        case Left(failure) => Ior.Both(Chain.one(failure), json)

    def readIor(json: Json): Ior[Chain[JsonFailure], A] =
      readParent(json) match
        case Left(failure) => Ior.Left(Chain.one(failure))
        case Right(obj)    =>
          readFields(obj) match
            case Left(chain) => Ior.Left(chain)
            case Right(sub)  => decodeOrLeft(Json.fromJsonObject(sub))
