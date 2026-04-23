package eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}

import eo.optics.Optic

import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}

/** A specialised Prism from [[io.circe.Json]] to a native type `A`, backed by a circe `Encoder[A]`
  * / `Decoder[A]` pair and a flat `Array[PathStep]` path that tracks the cursor steps.
  *
  * Shape on the Optic type:
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Either]
  *   type X = (DecodingFailure, HCursor)
  * }}}
  *
  * Extends `Optic` directly, in the same vein as [[eo.optics.MendTearPrism]] — so it composes with
  * the rest of the cats-eo universe via stock `.andThen`, and its abstract `to` / `from` satisfy
  * the Either-carrier contract.
  *
  * Storage is a flat `Array[PathStep]` of field-name / array-index steps (the path from the root
  * Json to the focused value). Each `.field(_.name)` call copies the array and appends one step.
  * Walking the path at `modify` / `transform` time is an imperative loop that pulls JsonObjects
  * directly via `Json.asObject` / `JsonObject.apply` — skipping `HCursor` entirely — and rebuilds
  * by walking up through `JsonObject.add`, which keeps the underlying `LinkedHashMap`
  * representation and avoids the expensive `LinkedHashMap → MapAndVector` conversion that
  * `HCursor.set.top` triggers.
  *
  * Two call-surface tiers:
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` return
  *     `Json => Ior[Chain[JsonFailure], Json]`; `get` returns `Ior[Chain[JsonFailure], A]`.
  *     Failures observed during the walk (missing path steps, non-object / non-array parents,
  *     out-of-range indices, decode failures) are collected into a `Chain[JsonFailure]` and
  *     surfaced to the caller rather than silently swallowed. Partial success returns
  *     `Ior.Both(chain, inputJson)` — the Json is the pre-plan silent behaviour, the chain
  *     documents what went wrong.
  *   - '''`*Unsafe` (silent).''' `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` /
  *     `transferUnsafe` / `getOptionUnsafe` preserve the pre-v0.2 forgiving behaviour
  *     byte-for-byte: silent pass-through on the modify family, `Option[A]` on the read family.
  *     Escape hatch for callers who have measured and don't want the `Ior.Right(json)` allocation
  *     on the happy path.
  *
  * Failure diagnostics on the abstract `Optic` surface: the inherited `to` method still returns
  * `Left((DecodingFailure, HCursor))` so callers routing through the generic cats-eo extensions can
  * both diagnose (`DecodingFailure.history`) and recover raw Json (`HCursor.value` /
  * `HCursor.top`). Users reaching for the direct-call hot path should prefer the Ior-bearing
  * default surface, which carries richer per-step context.
  */
final class JsonPrism[A] private[circe] (
    private[circe] val path: Array[PathStep],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either],
      Dynamic:
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

  // ---- Default (Ior-bearing) surface --------------------------------
  //
  // Apply what's possible; surface every miss as a JsonFailure in the
  // Chain. For a single-field prism there are three shapes:
  //   - happy path               → Ior.Right(newJson)
  //   - path-level or decode miss → Ior.Both(chain-of-one, inputJson)
  //   - (Ior.Left does not arise on modify — the input Json is always
  //     a fallback carrier; `get` is the place where Ior.Left surfaces,
  //     because "nothing read" has no Json-shaped fallback.)

  def modify(f: A => A): Json => Ior[Chain[JsonFailure], Json] =
    json => modifyIor(json, f)

  def transform(f: Json => Json): Json => Ior[Chain[JsonFailure], Json] =
    json => transformIor(json, f)

  def place(a: A): Json => Ior[Chain[JsonFailure], Json] =
    json => placeIor(json, a)

  def transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json] =
    json => c => placeIor(json, f(c))

  def get(json: Json): Ior[Chain[JsonFailure], A] =
    getIor(json)

  // ---- *Unsafe (silent) escape hatches ------------------------------
  //
  // Byte-identical to the pre-v0.2 forgiving bodies. Callers who have
  // measured and know the Ior wrap isn't wanted reach for these.

  inline def modifyUnsafe(f: A => A): Json => Json =
    json => modifyImpl(json, f)

  inline def transformUnsafe(f: Json => Json): Json => Json =
    json => transformImpl(json, f)

  inline def placeUnsafe(a: A): Json => Json =
    json => placeImpl(json, a)

  inline def transferUnsafe[C](f: C => A): Json => C => Json =
    json => c => placeImpl(json, f(c))

  inline def reverseGet(a: A): Json = encoder(a)

  inline def getOptionUnsafe(json: Json): Option[A] =
    val c = navigateCursor(json)
    c.as[A](using decoder).toOption

  // ---- Non-inline *Unsafe helper bodies -----------------------------
  //
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
      var i = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None      => return json
              case Some(obj) =>
                parents(i) = obj
                obj(name) match
                  case None    => return json
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None      => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Left(_)  => json
        case Right(a) =>
          var newChild: Json = encoder(f(a))
          var j = n - 1
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
      var i = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None      => return json
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None      => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = f(cur)
      var j = n - 1
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
      var i = 0
      while i < n do
        path(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None      => return json
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None      => return json
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return json
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = encoder(a)
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), path(j), newChild)
        j -= 1
      newChild

  // ---- Ior-bearing helper bodies ------------------------------------
  //
  // Same walk shape as the *Unsafe bodies. On path miss we synthesise
  // a single-failure Chain and return Ior.Both(chain, inputJson);
  // the caller can choose to ignore the chain or surface it. No
  // Chain.Builder allocation on the happy path — we build a one-
  // element Chain only when a miss surfaces. Every success path
  // returns `Ior.Right(newJson)` with a single Chain.empty-free wrap.

  private def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    val n = path.length
    if n == 0 then
      decoder.decodeJson(json) match
        case Right(a) => Ior.Right(encoder(f(a)))
        case Left(df) => Ior.Both(Chain.one(JsonFailure.DecodeFailed(rootStep, df)), json)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i = 0
      while i < n do
        val step = path(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), json)
              case Some(obj) =>
                parents(i) = obj
                obj(name) match
                  case None =>
                    return Ior.Both(Chain.one(JsonFailure.PathMissing(step)), json)
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), json)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)), json)
                parents(i) = arr
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Left(df) =>
          Ior.Both(Chain.one(JsonFailure.DecodeFailed(path(n - 1), df)), json)
        case Right(a) =>
          var newChild: Json = encoder(f(a))
          var j = n - 1
          while j >= 0 do
            newChild = rebuildStep(parents(j), path(j), newChild)
            j -= 1
          Ior.Right(newChild)

  private def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    val n = path.length
    if n == 0 then Ior.Right(f(json))
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i = 0
      while i < n do
        val step = path(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), json)
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), json)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)), json)
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = f(cur)
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), path(j), newChild)
        j -= 1
      Ior.Right(newChild)

  private def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    val n = path.length
    if n == 0 then Ior.Right(encoder(a))
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = json
      var i = 0
      while i < n do
        val step = path(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), json)
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), json)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)), json)
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = encoder(a)
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), path(j), newChild)
        j -= 1
      Ior.Right(newChild)

  /** Ior-bearing read — walks `path`, returning `Ior.Left(chain-of-one)` on any miss. Success is
    * `Ior.Right(a)`; `Ior.Both` does not arise because a read either produces an `A` or it doesn't.
    */
  private def getIor(json: Json): Ior[Chain[JsonFailure], A] =
    val n = path.length
    if n == 0 then
      decoder.decodeJson(json) match
        case Right(a) => Ior.Right(a)
        case Left(df) => Ior.Left(Chain.one(JsonFailure.DecodeFailed(rootStep, df)))
    else
      var cur: Json = json
      var i = 0
      while i < n do
        val step = path(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Left(Chain.one(JsonFailure.NotAnObject(step)))
              case Some(obj) =>
                obj(name) match
                  case None =>
                    return Ior.Left(Chain.one(JsonFailure.PathMissing(step)))
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Left(Chain.one(JsonFailure.NotAnArray(step)))
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Left(Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)))
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Right(a) => Ior.Right(a)
        case Left(df) => Ior.Left(Chain.one(JsonFailure.DecodeFailed(path(n - 1), df)))

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

  // ---- Helpers ------------------------------------------------------

  /** Build an ACursor from the root by walking each path step via `downField` / `downN`. Only used
    * by the generic Optic `to` / `from`; the hot-path methods above go straight through
    * `JsonObject` / `Vector[Json]` without materialising a cursor.
    */
  private def navigateCursor(json: Json): ACursor =
    var c: ACursor = json.hcursor
    var i = 0
    while i < path.length do
      c = path(i) match
        case PathStep.Field(name) => c.downField(name)
        case PathStep.Index(idx)  => c.downN(idx)
      i += 1
    c

  /** Sentinel step for root-level decode failures on a path-empty prism. The decode-failed leaf has
    * no field-name or index to point at, so we use `PathStep.Field("")` as a marker.
    */
  private inline def rootStep: PathStep = PathStep.Field("")

  /** Extend the path by a field step and swap to a narrower Encoder/Decoder pair. Used by the
    * [[field]] / `selectDynamic` macros; not intended for direct call by end users.
    */
  private[circe] def widenPath[B](
      step: String
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend the path by an array-index step and swap to the element type's Encoder/Decoder pair.
    * Used by the [[at]] macro.
    */
  private[circe] def widenPathIndex[B](
      i: Int
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Index(i))

  private def widenPathStep[B](
      step: PathStep
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = step
    new JsonPrism[B](newPath, encB, decB)

  /** Hand off the current path as a [[JsonTraversal]] prefix with an empty suffix; the new focus
    * type `B` is the element type of the array at this position. Used by the [[each]] macro; not
    * meant for direct call by end users.
    */
  private[circe] def toTraversal[B](using
      encB: Encoder[B],
      decB: Decoder[B],
  ): JsonTraversal[B] =
    new JsonTraversal[B](path, Array.empty[PathStep], encB, decB)

object JsonPrism:

  /** Construct a root-level `JsonPrism[S]` — the path is empty, so `modify` applies the user
    * function to the whole Json after decoding, and `transform` applies to the raw Json at the
    * root.
    */
  def apply[S](using enc: Encoder[S], dec: Decoder[S]): JsonPrism[S] =
    new JsonPrism[S](Array.empty[PathStep], enc, dec)

  /** Drill into a named field via a selector lambda. The macro extracts the field name at compile
    * time; the resulting `JsonPrism[B]` extends the parent's path by one step and carries the inner
    * `Encoder[B]` / `Decoder[B]` for encoding and decoding at that position.
    *
    * Chain freely: `codecPrism[Person].field(_.address).field(_.street)`.
    */
  extension [A](o: JsonPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
      ${ JsonPrismMacro.fieldImpl[A, B]('o, 'selector, 'encB, 'decB) }

  /** Drill into the `i`-th element of a JSON array. The parent `JsonPrism[A]` must focus a Scala
    * collection (`Vector[B]`, `List[B]`, `Seq[B]`, etc.); the macro extracts the element type `B`,
    * summons `Encoder[B]` and `Decoder[B]` from the enclosing scope, and emits a child
    * `JsonPrism[B]`.
    *
    * `i` is free to be a runtime value — the macro does not constant-evaluate it.
    *
    * Out-of-range and missing-array cases follow the rest of `JsonPrism`'s forgiving semantics:
    * `modify` returns the input unchanged.
    */
  extension [A](o: JsonPrism[A])

    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atImpl[A]('o, 'i) }

  /** Split the path at the current array focus and return a [[JsonTraversal]] whose element type is
    * `A`'s first type argument (Vector/List/Seq/…). Subsequent `.field(_.x)` /
    * `.selectDynamic("x")` / `.at(i)` calls on the traversal extend its suffix, so one traversal
    * covers arbitrarily deep paths under the iterated array.
    *
    * Usage: `codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(json)` upper-cases
    * every `items[*].name` in one pass (or `.modify(...)` for the default Ior-bearing surface).
    */
  extension [A](o: JsonPrism[A])

    transparent inline def each: Any =
      ${ JsonPrismMacro.eachImpl[A]('o) }
