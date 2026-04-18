package eo.circe

import scala.language.dynamics

import eo.optics.Optic

import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}

/** A specialised Prism from [[io.circe.Json]] to a native type `A`,
  * backed by a circe `Encoder[A]` / `Decoder[A]` pair and a flat
  * `Array[String]` path that tracks the cursor steps.
  *
  * Shape on the Optic type:
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Either]
  *   type X = (DecodingFailure, HCursor)
  * }}}
  *
  * Extends `Optic` directly, in the same vein as
  * [[eo.optics.MendTearPrism]] — so it composes with the rest of the
  * cats-eo universe via stock `.andThen`, and its abstract `to` /
  * `from` satisfy the Either-carrier contract.
  *
  * Storage is a flat `Array[String]` of field names (the path from
  * the root Json to the focused value). Each `.field(_.name)` call
  * copies the array and appends one step. Walking the path at
  * `modify` / `transform` time is an imperative loop that pulls
  * JsonObjects directly via `Json.asObject` / `JsonObject.apply` —
  * skipping `HCursor` entirely — and rebuilds by walking up through
  * `JsonObject.add`, which keeps the underlying `LinkedHashMap`
  * representation and avoids the expensive
  * `LinkedHashMap → MapAndVector` conversion that
  * `HCursor.set.top` triggers (≈27 % of runtime on the narrow
  * d2 bench before this optimisation).
  *
  * Forgiving semantics: `modify` returns the input Json unchanged
  * if any step on the path is missing, if the Json at that step
  * isn't a JsonObject, or if the focused value doesn't decode.
  * `transform` applies the user's function to `Json.Null` when
  * the leaf is absent and inserts the result at the missing field.
  *
  * Failure diagnostics: the abstract `to` method (used through the
  * generic Optic extensions) returns `Left((DecodingFailure, HCursor))`
  * so callers can both diagnose (`DecodingFailure.history`) and
  * recover raw Json (`HCursor.value` / `HCursor.top`).
  */
final class JsonPrism[A] private[circe] (
    private[circe] val path:    Array[PathStep],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either], Dynamic:
  type X = (DecodingFailure, HCursor)

  // ---- Selectable field sugar ---------------------------------------
  //
  // `codecPrism[Person].address.street` compiles to
  // `codecPrism[Person].field(_.address).field(_.street)` — no
  // explicit selector lambdas, no extension-method noise. The macro
  // looks the field up on `A`'s case-class schema at compile time,
  // summons its Encoder/Decoder, and delegates to `widenPath`.
  //
  // Only case-class case fields are reachable this way. Methods
  // already defined on the class (`modify`, `transform`, `to`, …)
  // bind first via normal member resolution, so the Dynamic path
  // never shadows them.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldImpl[A]('this, 'name) }

  // ---- Abstract Optic members (for generic Optic contract) ----------
  //
  // These still use `HCursor`-based navigation: the generic `Optic`
  // extensions route through them, and callers that care about the
  // (DecodingFailure, HCursor) diagnostic need an HCursor-aware `to`.
  // The hot-path class methods below bypass them.

  def to: Json => Either[(DecodingFailure, HCursor), A] = json =>
    val c = navigateCursor(json)
    c.as[A](using decoder) match
      case Right(a) => Right(a)
      case Left(df) => Left((df, json.hcursor))

  def from: Either[(DecodingFailure, HCursor), A] => Json = {
    case Left((_, hc)) => hc.top.getOrElse(hc.value)
    case Right(a)      => encoder(a)
  }

  // ---- Cursor-aware overrides for the hot path ----------------------
  //
  // These shadow the generic `Optic` extension methods. They walk
  // the `path` array directly, collecting parent JsonObjects on the
  // way down and rebuilding them on the way up via
  // `JsonObject.add` — which for an existing key copies the
  // underlying `LinkedHashMap` once and mutates in place. One
  // `JsonObject.add` per path step; no `HCursor`, no internal
  // representation switch in circe.

  inline def modify[X](f: A => A): Json => Json =
    json => modifyImpl(json, f)

  inline def transform[X](f: Json => Json): Json => Json =
    json => transformImpl(json, f)

  inline def place[X](a: A): Json => Json =
    json => placeImpl(json, a)

  inline def transfer[C, X](f: C => A): Json => C => Json =
    json => c => placeImpl(json, f(c))

  inline def reverseGet(a: A): Json = encoder(a)

  inline def getOption(json: Json): Option[A] =
    val c = navigateCursor(json)
    c.as[A](using decoder).toOption

  // ---- Non-inline helper bodies (the inline wrappers above just
  //      dispatch, so they can be used on a base-typed Optic without
  //      exploding their body at every call site).

  // Walk loops below share one `Array[AnyRef]` to remember each
  // step's parent container — `JsonObject` for `Field` steps,
  // `Vector[Json]` for `Index` steps. We cast on the way back up;
  // the PathStep at position `j` tells us which cast is safe.

  private def modifyImpl(json: Json, f: A => A): Json =
    val n = path.length
    if n == 0 then
      decoder.decodeJson(json) match
        case Right(a) => encoder(f(a))
        case Left(_)  => json
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i         = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None => return json
              case Some(obj) =>
                parents(i) = obj
                obj(name) match
                  case None    => return json
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Left(_) => json
        case Right(a) =>
          var newChild: Json = encoder(f(a))
          var j              = n - 1
          while j >= 0 do
            newChild = rebuildStep(parents(j), path(j), newChild)
            j -= 1
          newChild

  private def transformImpl(json: Json, f: Json => Json): Json =
    val n = path.length
    if n == 0 then f(json)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i         = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None => return json
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = f(cur)
      var j              = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), path(j), newChild)
        j -= 1
      newChild

  private def placeImpl(json: Json, a: A): Json =
    val n = path.length
    if n == 0 then encoder(a)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i         = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None => return json
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = encoder(a)
      var j              = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), path(j), newChild)
        j -= 1
      newChild

  private inline def rebuildStep(
      parent: AnyRef,
      step:   PathStep,
      child:  Json,
  ): Json =
    step match
      case PathStep.Field(name) =>
        Json.fromJsonObject(parent.asInstanceOf[JsonObject].add(name, child))
      case PathStep.Index(idx)  =>
        Json.fromValues(parent.asInstanceOf[Vector[Json]].updated(idx, child))

  // ---- Helpers ------------------------------------------------------

  /** Build an ACursor from the root by walking each path step via
    * `downField` / `downN`. Only used by the generic Optic `to` /
    * `from`; the hot-path methods above go straight through
    * `JsonObject` / `Vector[Json]` without materialising a cursor. */
  private def navigateCursor(json: Json): ACursor =
    var c: ACursor = json.hcursor
    var i          = 0
    while i < path.length do
      c = path(i) match
        case PathStep.Field(name) => c.downField(name)
        case PathStep.Index(idx)  => c.downN(idx)
      i += 1
    c

  /** Extend the path by a field step and swap to a narrower
    * Encoder/Decoder pair. Used by the [[field]] / `selectDynamic`
    * macros; not intended for direct call by end users. */
  private[circe] def widenPath[B](
      step: String,
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend the path by an array-index step and swap to the
    * element type's Encoder/Decoder pair. Used by the [[at]]
    * macro. */
  private[circe] def widenPathIndex[B](
      i: Int,
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Index(i))

  private def widenPathStep[B](
      step: PathStep,
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = step
    new JsonPrism[B](newPath, encB, decB)

  /** Hand off the current path as a [[JsonTraversal]] prefix with
    * an empty suffix; the new focus type `B` is the element type
    * of the array at this position. Used by the [[each]] macro;
    * not meant for direct call by end users. */
  private[circe] def toTraversal[B](using
      encB: Encoder[B],
      decB: Decoder[B],
  ): JsonTraversal[B] =
    new JsonTraversal[B](path, Array.empty[PathStep], encB, decB)

object JsonPrism:

  /** Construct a root-level `JsonPrism[S]` — the path is empty, so
    * `modify` applies the user function to the whole Json after
    * decoding, and `transform` applies to the raw Json at the
    * root. */
  def apply[S](using enc: Encoder[S], dec: Decoder[S]): JsonPrism[S] =
    new JsonPrism[S](Array.empty[PathStep], enc, dec)

  /** Drill into a named field via a selector lambda. The macro
    * extracts the field name at compile time; the resulting
    * `JsonPrism[B]` extends the parent's path by one step and
    * carries the inner `Encoder[B]` / `Decoder[B]` for encoding
    * and decoding at that position.
    *
    * Chain freely: `codecPrism[Person].field(_.address).field(_.street)`. */
  extension [A](o: JsonPrism[A])
    transparent inline def field[B](
        inline selector: A => B,
    )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
      ${ JsonPrismMacro.fieldImpl[A, B]('o, 'selector, 'encB, 'decB) }

  /** Drill into the `i`-th element of a JSON array. The parent
    * `JsonPrism[A]` must focus a Scala collection (`Vector[B]`,
    * `List[B]`, `Seq[B]`, etc.); the macro extracts the element
    * type `B`, summons `Encoder[B]` and `Decoder[B]` from the
    * enclosing scope, and emits a child `JsonPrism[B]`.
    *
    * `i` is free to be a runtime value — the macro does not
    * constant-evaluate it.
    *
    * Out-of-range and missing-array cases follow the rest of
    * `JsonPrism`'s forgiving semantics: `modify` returns the
    * input unchanged. */
  extension [A](o: JsonPrism[A])
    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atImpl[A]('o, 'i) }

  /** Split the path at the current array focus and return a
    * [[JsonTraversal]] whose element type is `A`'s first type
    * argument (Vector/List/Seq/…). Subsequent `.field(_.x)` /
    * `.selectDynamic("x")` / `.at(i)` calls on the traversal
    * extend its suffix, so one traversal covers arbitrarily deep
    * paths under the iterated array.
    *
    * Usage: `codecPrism[Basket].items.each.name.modify(_.toUpperCase)(json)`
    * upper-cases every `items[*].name` in one pass. */
  extension [A](o: JsonPrism[A])
    transparent inline def each: Any =
      ${ JsonPrismMacro.eachImpl[A]('o) }
