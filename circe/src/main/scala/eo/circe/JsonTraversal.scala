package eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}

import io.circe.{Decoder, Encoder, Json, JsonObject}

/** Multi-focus counterpart to [[JsonPrism]]: a Traversal that walks the JSON from the root down to
  * some array, then applies the focus update to every element of that array.
  *
  * A traversal is the natural result of composing a prism with `.each` — the path splits at the
  * array into a `prefix` (root to the array) and a `suffix` (each element to the focused leaf).
  * Further `.field(_.x)` / `.selectDynamic("x")` / `.at(i)` composition extends the suffix, so one
  * `JsonTraversal[A]` can accumulate arbitrarily deep navigation under the iterated array.
  *
  * Two call-surface tiers (introduced in v0.2):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` / `getAll` return
  *     `Ior[Chain[JsonFailure], Json]` (or `…, Vector[A]]` for `getAll`). Per-element failures
  *     accumulate naturally into the `Chain` — one `JsonFailure` per skipped element, each pointing
  *     at the step that refused. Prefix-walk failures return `Ior.Left(chain)`: there's nothing to
  *     iterate, so the Json is intentionally not carried as a fallback.
  *   - '''`*Unsafe` (silent).''' `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` /
  *     `transferUnsafe` / `getAllUnsafe` preserve the pre-v0.2 forgiving behaviour byte-for-byte:
  *     silent pass-through / drop on any miss. Byte-identical to today's hot path.
  *
  * Note: [[place]] / [[transfer]] on a traversal overwrite every element with the same value (or
  * same function applied to `C`). Useful for broadcasting a constant or overlaying a common value
  * across an array.
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

  // ---- Default (Ior-bearing) surface --------------------------------

  def modify(f: A => A): Json => Ior[Chain[JsonFailure], Json] =
    json => modifyIor(json, f)

  def transform(f: Json => Json): Json => Ior[Chain[JsonFailure], Json] =
    json => transformIor(json, f)

  def place(a: A): Json => Ior[Chain[JsonFailure], Json] =
    json => placeIor(json, a)

  def transfer[C](f: C => A): Json => C => Ior[Chain[JsonFailure], Json] =
    json => c => placeIor(json, f(c))

  def getAll(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    getAllIor(json)

  // ---- *Unsafe (silent) escape hatches ------------------------------

  inline def modifyUnsafe(f: A => A): Json => Json =
    json => modifyImpl(json, f)

  inline def transformUnsafe(f: Json => Json): Json => Json =
    json => transformImpl(json, f)

  inline def placeUnsafe(a: A): Json => Json =
    json => placeImpl(json, a)

  inline def transferUnsafe[C](f: C => A): Json => C => Json =
    json => c => placeImpl(json, f(c))

  inline def getAllUnsafe(json: Json): Vector[A] =
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

  // ---- Internals: *Unsafe walks (byte-identical to pre-v0.2) -------

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

  private def placeImpl(json: Json, a: A): Json =
    val encoded = encoder(a)
    mapAtPrefix(json) { elem =>
      placeElement(elem, encoded)
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

  /** Like [[updateElementDecoded]] but the user's focus value is precomputed (a single encoded
    * Json). Used by [[placeImpl]] so the encoder runs once, not once per element.
    */
  private def placeElement(elemJson: Json, encoded: Json): Json =
    val n = suffix.length
    if n == 0 then encoded
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
      var newChild: Json = encoded
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), suffix(j), newChild)
        j -= 1
      newChild

  // ---- Internals: Ior-bearing walks --------------------------------

  /** Walk the prefix with failure-accumulation. Returns either an array (with a builder for
    * per-element accumulation) or an `Ior.Left(chain-of-one)` because there's nothing to iterate.
    * Shared by the default `modify` / `transform` / `place` / `transfer`.
    */
  private def navigateToArrayIor(
      json: Json
  ): Either[Ior.Left[Chain[JsonFailure]], (Vector[Json], Array[AnyRef])] =
    val n = prefix.length
    val parents = new Array[AnyRef](n)
    var cur: Json = json
    var i = 0
    while i < n do
      val step = prefix(i)
      step match
        case PathStep.Field(name) =>
          cur.asObject match
            case None =>
              return Left(Ior.Left(Chain.one(JsonFailure.NotAnObject(step))))
            case Some(obj) =>
              parents(i) = obj
              obj(name) match
                case None =>
                  return Left(Ior.Left(Chain.one(JsonFailure.PathMissing(step))))
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None =>
              return Left(Ior.Left(Chain.one(JsonFailure.NotAnArray(step))))
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then
                return Left(
                  Ior.Left(Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)))
                )
              parents(i) = arr
              cur = arr(idx)
      i += 1
    cur.asArray match
      case Some(arr) => Right((arr, parents))
      case None      =>
        // Prefix resolved but the terminal value isn't an array — synthesise
        // a NotAnArray at the last step. For a prefix-empty traversal this
        // is the root-step sentinel.
        val lastStep =
          if n == 0 then PathStep.Field("") else prefix(n - 1)
        Left(Ior.Left(Chain.one(JsonFailure.NotAnArray(lastStep))))

  private def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, updateElementDecodedIor(_, f))
        rebuildPrefix(newArrAndChain, parents)

  private def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, updateElementRawIor(_, f))
        rebuildPrefix(newArrAndChain, parents)

  private def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    val encoded = encoder(a)
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, placeElementIor(_, encoded))
        rebuildPrefix(newArrAndChain, parents)

  /** Per-element map that accumulates each per-element Ior.Both / Ior.Right into a shared
    * Chain.Builder. Ior.Left from an element is treated as "couldn't update this element" — the
    * element stays in the output unchanged, failure contributes to the chain.
    */
  private def mapElementsIor(
      arr: Vector[Json],
      elemUpdate: Json => Ior[Chain[JsonFailure], Json],
  ): (Vector[Json], Chain[JsonFailure]) =
    val b = Vector.newBuilder[Json]
    b.sizeHint(arr.length)
    var chain: Chain[JsonFailure] = Chain.empty
    var i = 0
    while i < arr.length do
      elemUpdate(arr(i)) match
        case Ior.Right(j)   => b += j
        case Ior.Both(c, j) =>
          chain = chain ++ c
          b += j
        case Ior.Left(c) =>
          chain = chain ++ c
          b += arr(i)
      i += 1
    (b.result(), chain)

  /** Rebuild the prefix path back up to the root and wrap the result in the appropriate Ior shape:
    * Ior.Right on an empty chain, Ior.Both otherwise.
    */
  private def rebuildPrefix(
      arrAndChain: (Vector[Json], Chain[JsonFailure]),
      parents: Array[AnyRef],
  ): Ior[Chain[JsonFailure], Json] =
    val (newArr, chain) = arrAndChain
    var newChild: Json = Json.fromValues(newArr)
    var j = prefix.length - 1
    while j >= 0 do
      newChild = rebuildStep(parents(j), prefix(j), newChild)
      j -= 1
    if chain.isEmpty then Ior.Right(newChild)
    else Ior.Both(chain, newChild)

  /** Per-element Ior-bearing decoded update. Walks the suffix, decodes the leaf, applies `f`,
    * re-encodes. Returns Ior.Left(chain-of-one) + inputElement via Ior.Both's contract if the
    * suffix or decode misses. This way the element is preserved in the output.
    */
  private def updateElementDecodedIor(
      elemJson: Json,
      f: A => A,
  ): Ior[Chain[JsonFailure], Json] =
    val n = suffix.length
    if n == 0 then
      decoder.decodeJson(elemJson) match
        case Right(a) => Ior.Right(encoder(f(a)))
        case Left(df) =>
          Ior.Both(Chain.one(JsonFailure.DecodeFailed(PathStep.Field(""), df)), elemJson)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = elemJson
      var i = 0
      while i < n do
        val step = suffix(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), elemJson)
              case Some(obj) =>
                parents(i) = obj
                obj(name) match
                  case None =>
                    return Ior.Both(Chain.one(JsonFailure.PathMissing(step)), elemJson)
                  case Some(c) => cur = c
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), elemJson)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(
                    Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)),
                    elemJson,
                  )
                parents(i) = arr
                cur = arr(idx)
        i += 1
      decoder.decodeJson(cur) match
        case Left(df) =>
          Ior.Both(Chain.one(JsonFailure.DecodeFailed(suffix(n - 1), df)), elemJson)
        case Right(a) =>
          var newChild: Json = encoder(f(a))
          var j = n - 1
          while j >= 0 do
            newChild = rebuildStep(parents(j), suffix(j), newChild)
            j -= 1
          Ior.Right(newChild)

  /** Per-element Ior-bearing raw-Json update. Like updateElementDecodedIor but skips the decode
    * step.
    */
  private def updateElementRawIor(
      elemJson: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    val n = suffix.length
    if n == 0 then Ior.Right(f(elemJson))
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = elemJson
      var i = 0
      while i < n do
        val step = suffix(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), elemJson)
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), elemJson)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(
                    Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)),
                    elemJson,
                  )
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = f(cur)
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), suffix(j), newChild)
        j -= 1
      Ior.Right(newChild)

  /** Per-element Ior-bearing place (constant Json). */
  private def placeElementIor(
      elemJson: Json,
      encoded: Json,
  ): Ior[Chain[JsonFailure], Json] =
    val n = suffix.length
    if n == 0 then Ior.Right(encoded)
    else
      val parents = new Array[AnyRef](n)
      var cur: Json = elemJson
      var i = 0
      while i < n do
        val step = suffix(i)
        step match
          case PathStep.Field(name) =>
            cur.asObject match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), elemJson)
              case Some(obj) =>
                parents(i) = obj
                cur = obj(name).getOrElse(Json.Null)
          case PathStep.Index(idx) =>
            cur.asArray match
              case None =>
                return Ior.Both(Chain.one(JsonFailure.NotAnArray(step)), elemJson)
              case Some(arr) =>
                if idx < 0 || idx >= arr.length then
                  return Ior.Both(
                    Chain.one(JsonFailure.IndexOutOfRange(step, arr.length)),
                    elemJson,
                  )
                parents(i) = arr
                cur = arr(idx)
        i += 1
      var newChild: Json = encoded
      var j = n - 1
      while j >= 0 do
        newChild = rebuildStep(parents(j), suffix(j), newChild)
        j -= 1
      Ior.Right(newChild)

  /** Per-element Ior-bearing read: walk the suffix, then decode the leaf. Success = Ior.Right(a),
    * any miss = Ior.Left(chain-of-one). Used by `getAll`.
    */
  private def readElementIor(elemJson: Json): Ior[Chain[JsonFailure], A] =
    val n = suffix.length
    var cur: Json = elemJson
    var i = 0
    while i < n do
      val step = suffix(i)
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
      case Left(df) =>
        val lastStep = if n == 0 then PathStep.Field("") else suffix(n - 1)
        Ior.Left(Chain.one(JsonFailure.DecodeFailed(lastStep, df)))

  private def getAllIor(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    navigateToArrayIor(json) match
      case Left(l)         => l
      case Right((arr, _)) =>
        val b = Vector.newBuilder[A]
        b.sizeHint(arr.length)
        var chain: Chain[JsonFailure] = Chain.empty
        var i = 0
        while i < arr.length do
          readElementIor(arr(i)) match
            case Ior.Right(a)   => b += a
            case Ior.Left(c)    => chain = chain ++ c
            case Ior.Both(c, a) =>
              chain = chain ++ c
              b += a
          i += 1
        val result = b.result()
        if chain.isEmpty then Ior.Right(result)
        else Ior.Both(chain, result)

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
