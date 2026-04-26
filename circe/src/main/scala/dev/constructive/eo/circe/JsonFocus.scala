package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, Json, JsonObject}

/** What it means to focus on a value of type `A` somewhere inside a `Json`.
  *
  * The four legacy carrier classes — `JsonPrism`, `JsonFieldsPrism`, `JsonTraversal`,
  * `JsonFieldsTraversal` — used to be four classes, but they only varied along two orthogonal axes:
  *
  *   1. ''Where does the focus live?'' — at a leaf reached by a path (Leaf), or as a NamedTuple
  *      assembled from selected fields under a parent (Fields).
  *   2. ''Single-focus or multi-focus?'' — applied once at the root (Prism), or applied per-element
  *      after walking a `prefix` to an array (Traversal).
  *
  * Axis 1 lives here: a `JsonFocus[A]` factors out everything that depends on the storage shape of
  * the focus. Axis 2 lives in [[JsonPrism]] / [[JsonTraversal]], each of which holds a
  * `JsonFocus[A]` and delegates the per-element focus operations to it. The four-class hierarchy
  * collapses to two, with the storage variation captured here as a sealed enum.
  *
  * Each focus exposes the same six per-Json operations: three Ior-bearing (decoded modify, raw
  * transform, decoded place) for the default surface, and three silent (input-on-failure) for the
  * `*Unsafe` escape hatches. Plus two reads (decoded get, decoded silent get) and the abstract
  * Optic-trait diagnostics (`navigateCursor` returning an `ACursor` for the (DecodingFailure,
  * HCursor) shape).
  */
private[circe] sealed abstract class JsonFocus[A]:

  /** Codec for the focused value `A`. For Leaf focuses this is the user's `Encoder[A]` /
    * `Decoder[A]`; for Fields focuses this is the codec for the synthesised NamedTuple type.
    */
  private[circe] def encoder: Encoder[A]

  /** See [[encoder]]. */
  private[circe] def decoder: Decoder[A]

  /** The terminal step of this focus's path — used by the failure surfaces to point at the last
    * cursor position the walk attempted. `PathStep.Field("")` for root-level focuses with empty
    * paths.
    */
  protected def terminalStep: PathStep

  // ---- Default Ior-bearing ops ------------------------------------

  /** Walk to the focus, decode, apply `f`, encode, rebuild root. Failures (path miss, decode
    * failure) surface as `Ior.Both(chain, inputJson)` — the input is preserved as the silent
    * fallback while the diagnostic chain documents what went wrong.
    */
  def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json]

  /** Walk to the focus, apply `f` to the raw Json there, rebuild root. Same failure surface as
    * [[modifyIor]].
    */
  def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json]

  /** Walk to the focus, write the encoded `a`, rebuild root. Same failure surface. */
  def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json]

  /** Walk to the focus, decode. Success = `Ior.Right(a)`; failure = `Ior.Left(chain)` — a read
    * either produces an `A` or it doesn't, so the Json is intentionally not carried as a fallback.
    */
  def readIor(json: Json): Ior[Chain[JsonFailure], A]

  // ---- *Unsafe (silent) ops ---------------------------------------

  /** Silent counterpart to [[modifyIor]] — input pass-through on any failure. */
  def modifyImpl(json: Json, f: A => A): Json

  /** Silent counterpart to [[transformIor]]. */
  def transformImpl(json: Json, f: Json => Json): Json

  /** Silent counterpart to [[placeIor]]. */
  def placeImpl(json: Json, a: A): Json

  /** Silent read — `None` on any failure. */
  def readImpl(json: Json): Option[A]

  // ---- Optic-trait diagnostics (HCursor-based) --------------------

  /** Build the ACursor for the abstract `to` method. The HCursor route is used by the generic
    * `Optic` extensions; the hot-path Ior / Unsafe surfaces above bypass it.
    */
  def navigateCursor(json: Json): ACursor

  /** Decode an `A` from the navigated `ACursor` — used by the abstract `to` method. */
  def decodeFromCursor(c: ACursor): Either[DecodingFailure, A] = c.as[A](using decoder)

private[circe] object JsonFocus:

  /** Single-leaf focus — reaches an `A` via the `path` walk and reads / writes it through the
    * user-supplied codec. Powers `JsonPrism[A]` and the per-element step of `JsonTraversal[A]`
    * when the user did `.each.<field>` style chains.
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
          decoder.decodeJson(cur) match
            case Right(a) => Ior.Right(JsonWalk.rebuildPath(parents, path, encoder(f(a))))
            case Left(df) => Ior.Both(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)), json)

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
        case Right((cur, _)) =>
          decoder.decodeJson(cur) match
            case Right(a) => Ior.Right(a)
            case Left(df) => Ior.Left(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)))

  /** Multi-field focus — reaches a parent JsonObject via `parentPath`, then assembles a NamedTuple
    * `A` by reading the selected `fieldNames` from it. Powers the multi-field variants (formerly
    * `JsonFieldsPrism` / `JsonFieldsTraversal`).
    *
    * Per-element atomicity (D4 of the multi-field plan): the NamedTuple is all-or-nothing, so
    * partial reads (some selected fields missing) cannot synthesise an `A` — the chain accumulates
    * one `JsonFailure.PathMissing` per missing field, and the modify family preserves the input
    * unchanged at that location.
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

    /** Project the selected fields out of `obj`, missing fields default to `Json.Null`. Used
      * whenever transform-shape ops need a synthesis of "the focus subobject" without insisting on
      * atomicity.
      */
    private def buildSubObject(obj: JsonObject): JsonObject =
      fieldNames.foldLeft(JsonObject.empty)((sub, name) =>
        sub.add(name, obj(name).getOrElse(Json.Null))
      )

    /** Atomic read — succeeds with the assembled sub-object only when ALL selected fields are
      * present. Missing fields accumulate as `PathMissing` failures.
      */
    private def readFields(obj: JsonObject): Either[Chain[JsonFailure], JsonObject] =
      val (chain, sub) = fieldNames.toVector.foldLeft((Chain.empty[JsonFailure], JsonObject.empty)) {
        case ((chain, sub), name) =>
          obj(name) match
            case None    => (chain :+ JsonFailure.PathMissing(PathStep.Field(name)), sub)
            case Some(v) => (chain, sub.add(name, v))
      }
      Either.cond(chain.isEmpty, sub, chain)

    /** Overlay the encoder's output for a focus value `a` onto `parent`. Encoders that omit a
      * selected field leave the parent's existing entry untouched.
      */
    private def writeFields(parent: JsonObject, a: A): JsonObject =
      val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
      fieldNames.foldLeft(parent)((out, name) => encoded(name).fold(out)(v => out.add(name, v)))

    /** Overlay a foreign sub-object (the result of a `transform`'s user function) onto `parent`. */
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
              Json.fromJsonObject(sub).as[A](using decoder) match
                case Right(a) => Ior.Right(rebuild(writeFields(obj, f(a)), parents))
                case Left(df) =>
                  Ior.Both(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)), json)

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
            case Right(sub)  =>
              Json.fromJsonObject(sub).as[A](using decoder) match
                case Right(a) => Ior.Right(a)
                case Left(df) =>
                  Ior.Left(Chain.one(JsonFailure.DecodeFailed(terminalStep, df)))
