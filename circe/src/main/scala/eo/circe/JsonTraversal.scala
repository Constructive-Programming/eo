package eo.circe

import scala.language.dynamics

import io.circe.{Decoder, Encoder, Json, JsonObject}

/** Multi-focus counterpart to [[JsonPrism]]: a Traversal that walks the JSON from the root down to
  * some array, then applies the focus update to every element of that array.
  *
  * A traversal is the natural result of composing a prism with `.each` — the path splits at the
  * array into a `prefix` (root to the array) and a `suffix` (each element to the focused leaf).
  * Further `.field(_.x)` / `.selectDynamic("x")` / `.at(i)` composition extends the suffix, so one
  * `JsonTraversal[A]` can accumulate arbitrarily deep navigation under the iterated array.
  *
  * Operations on a traversal:
  *
  *   - [[modify]] — decode every element, apply `f`, re-encode, and rebuild the array and the
  *     prefix.
  *   - [[transform]] — apply `f: Json => Json` on the raw Json at every element (bypasses Codec).
  *   - [[getAll]] — collect every element that decodes successfully. Failed decodes are silently
  *     dropped.
  *
  * Failure semantics mirror [[JsonPrism]]: a missing or non-array prefix leaves the input
  * unchanged; per-element suffix walks that miss leave that element unchanged.
  */
final class JsonTraversal[A] private[circe] (
    private[circe] val prefix: Array[PathStep],
    private[circe] val suffix: Array[PathStep],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Dynamic:

  // ---- Dynamic field sugar -----------------------------------------

  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldTraversalImpl[A]('this, 'name) }

  // ---- Public API ---------------------------------------------------

  def modify(f: A => A): Json => Json =
    json => modifyImpl(json, f)

  def transform(f: Json => Json): Json => Json =
    json => transformImpl(json, f)

  def getAll(json: Json): Vector[A] =
    navigateToArray(json) match
      case None      => Vector.empty
      case Some(arr) =>
        val b = Vector.newBuilder[A]
        b.sizeHint(arr.length)
        var i = 0
        while i < arr.length do
          walkSuffix(arr(i)) match
            case None       => ()
            case Some(leaf) =>
              decoder.decodeJson(leaf) match
                case Right(a) => b += a
                case Left(_)  => ()
          i += 1
        b.result()

  // ---- Path extension (used by field / at / selectDynamic macros) --

  private[circe] def widenSuffix[B](
      step: String
  )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
    widenSuffixStep[B](PathStep.Field(step))

  private[circe] def widenSuffixIndex[B](
      i: Int
  )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
    widenSuffixStep[B](PathStep.Index(i))

  private def widenSuffixStep[B](
      step: PathStep
  )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
    val newSuffix = new Array[PathStep](suffix.length + 1)
    System.arraycopy(suffix, 0, newSuffix, 0, suffix.length)
    newSuffix(suffix.length) = step
    new JsonTraversal[B](prefix, newSuffix, encB, decB)

  // ---- Internals ---------------------------------------------------

  private def navigateToArray(json: Json): Option[Vector[Json]] =
    var cur: Json = json
    var i = 0
    while i < prefix.length do
      prefix(i) match
        case PathStep.Field(name) =>
          cur.asObject match
            case None      => return None
            case Some(obj) =>
              obj(name) match
                case None    => return None
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None      => return None
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then return None
              cur = arr(idx)
      i += 1
    cur.asArray

  private def walkSuffix(elemJson: Json): Option[Json] =
    var cur: Json = elemJson
    var i = 0
    while i < suffix.length do
      suffix(i) match
        case PathStep.Field(name) =>
          cur.asObject match
            case None      => return None
            case Some(obj) =>
              obj(name) match
                case None    => return None
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None      => return None
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then return None
              cur = arr(idx)
      i += 1
    Some(cur)

  private def modifyImpl(json: Json, f: A => A): Json =
    mapAtPrefix(json) { elem =>
      updateElementDecoded(elem, f)
    }

  private def transformImpl(json: Json, f: Json => Json): Json =
    mapAtPrefix(json) { elem =>
      updateElementRaw(elem, f)
    }

  /** Walk the prefix, collect parents, then replace the focused array by mapping every element
    * through `elemUpdate`. Unwinds the prefix with `rebuildStep`. Returns the input json unchanged
    * if the prefix walk fails.
    */
  private def mapAtPrefix(json: Json)(elemUpdate: Json => Json): Json =
    val n = prefix.length
    val parents = new Array[AnyRef](n)
    var cur: Json = json
    var i = 0
    while i < n do
      prefix(i) match
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

    val arr = cur.asArray match
      case Some(a) => a
      case None    => return json
    val newArr = arr.map(elemUpdate)

    var newChild: Json = Json.fromValues(newArr)
    var j = n - 1
    while j >= 0 do
      newChild = rebuildStep(parents(j), prefix(j), newChild)
      j -= 1
    newChild

  /** For one array element: walk the suffix collecting parents, decode the leaf, apply `f`,
    * re-encode, and rebuild the element. On any failure (suffix miss, decode failure) the original
    * element is returned unchanged.
    */
  private def updateElementDecoded(elemJson: Json, f: A => A): Json =
    val n = suffix.length
    if n == 0 then
      decoder.decodeJson(elemJson) match
        case Right(a) => encoder(f(a))
        case Left(_)  => elemJson
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = elemJson
      var i = 0
      while i < n do
        suffix(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None      => return elemJson
              case Some(obj) =>
                parents(i) = obj
                obj(name) match
                  case None    => return elemJson
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None      => return elemJson
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return elemJson
                parents(i) = arr
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Left(_)  => elemJson
        case Right(a) =>
          var newChild: Json = encoder(f(a))
          var j = n - 1
          while j >= 0 do
            newChild = rebuildStep(parents(j), suffix(j), newChild)
            j -= 1
          newChild

  /** Raw-Json element update: same walk as [[updateElementDecoded]] but applies `f` to the Json at
    * the suffix leaf. Missing leaves receive `Json.Null`.
    */
  private def updateElementRaw(elemJson: Json, f: Json => Json): Json =
    val n = suffix.length
    if n == 0 then f(elemJson)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = elemJson
      var i = 0
      while i < n do
        suffix(i) match
          case PathStep.Field(name) =>
            cur.asObject match
              case None      => return elemJson
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None      => return elemJson
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then return elemJson
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = f(cur)
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), suffix(j), newChild)
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

object JsonTraversal:

  /** `.field(_.x)` sugar — extend the suffix by a named field. Analogous to [[JsonPrism.field]].
    */
  extension [A](t: JsonTraversal[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
      ${ JsonPrismMacro.fieldTraversalImpl[A, B]('t, 'selector, 'encB, 'decB) }

  /** `.at(i)` sugar — extend the suffix by an array index, requiring the current focus to be a
    * collection. Analogous to [[JsonPrism.at]].
    */
  extension [A](t: JsonTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atTraversalImpl[A]('t, 'i) }
