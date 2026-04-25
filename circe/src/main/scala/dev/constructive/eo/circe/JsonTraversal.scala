package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import io.circe.{Decoder, Encoder, Json}

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
) extends JsonOpticOps[A],
      Dynamic:

  // ---- Dynamic field sugar -----------------------------------------

  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Read surface (multi-focus specific) --------------------------

  def getAll(input: Json | String): Ior[Chain[JsonFailure], Vector[A]] =
    JsonFailure.parseInputIor(input).flatMap(getAllIor)

  inline def getAllUnsafe(input: Json | String): Vector[A] =
    val json = JsonFailure.parseInputUnsafe(input)
    navigateToArray(json).fold(Vector.empty[A]) { arr =>
      arr.flatMap(elem => walkSuffix(elem).flatMap(leaf => decoder.decodeJson(leaf).toOption))
    }

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

  /** Hand off the current (prefix, suffix) as a [[JsonFieldsTraversal]] parent descriptor, with the
    * caller-supplied `fieldNames` enumerating the selected fields under the per-element parent.
    * Used by the [[JsonTraversal.fields]] macro; not meant for direct call by end users.
    *
    * The existing suffix becomes the new `elemParent` — the sub-path walked under each array
    * element to reach the JsonObject that carries the selected fields. In the common case
    * (`.each.fields` with no further drill), that suffix is empty and the elements themselves carry
    * the fields.
    */
  private[circe] def toFieldsTraversal[B](
      fieldNames: Array[String]
  )(using encB: Encoder[B], decB: Decoder[B]): JsonFieldsTraversal[B] =
    new JsonFieldsTraversal[B](prefix, suffix, fieldNames, encB, decB)

  // ---- Internals: *Unsafe walks (byte-identical to pre-v0.2) -------

  /** Read-only walk over the prefix that discards parents (we only need the terminal). Returns
    * `None` on any miss; `Some(arr)` if the terminal is a JsonArray.
    */
  private def navigateToArray(json: Json): Option[Vector[Json]] =
    JsonWalk.walkPath(json, prefix).toOption.flatMap(_._1.asArray)

  /** Read-only walk over the suffix that discards parents — used by the `getAll` family which only
    * needs the leaf to decode.
    */
  private def walkSuffix(elemJson: Json): Option[Json] =
    JsonWalk.walkPath(elemJson, suffix).toOption.map(_._1)

  override protected def modifyImpl(json: Json, f: A => A): Json =
    mapAtPrefix(json) { elem =>
      updateElementDecoded(elem, f)
    }

  override protected def transformImpl(json: Json, f: Json => Json): Json =
    mapAtPrefix(json) { elem =>
      updateElementRaw(elem, f)
    }

  override protected def placeImpl(json: Json, a: A): Json =
    val encoded = encoder(a)
    mapAtPrefix(json) { elem =>
      placeElement(elem, encoded)
    }

  /** Walk the prefix, collect parents, then replace the focused array by mapping every element
    * through `elemUpdate`. Unwinds the prefix with `rebuildStep`. Returns the input json unchanged
    * if the prefix walk fails.
    */
  private def mapAtPrefix(json: Json)(elemUpdate: Json => Json): Json =
    JsonWalk
      .walkPath(json, prefix)
      .toOption
      .flatMap { case (cur, parents) => cur.asArray.map(arr => (arr, parents)) }
      .map {
        case (arr, parents) =>
          JsonWalk.rebuildPath(parents, prefix, Json.fromValues(arr.map(elemUpdate)))
      }
      .getOrElse(json)

  /** For one array element: walk the suffix collecting parents, decode the leaf, apply `f`,
    * re-encode, and rebuild the element. On any failure (suffix miss, decode failure) the original
    * element is returned unchanged.
    */
  private def updateElementDecoded(elemJson: Json, f: A => A): Json =
    JsonWalk.walkPath(elemJson, suffix) match
      case Left(_)               => elemJson
      case Right((cur, parents)) =>
        decoder
          .decodeJson(cur)
          .map(a => JsonWalk.rebuildPath(parents, suffix, encoder(f(a))))
          .getOrElse(elemJson)

  /** Raw-Json element update: lenient walk so missing leaves become Json.Null at the leaf. */
  private def updateElementRaw(elemJson: Json, f: Json => Json): Json =
    JsonWalk.walkPathLenient(elemJson, suffix) match
      case Left(_)               => elemJson
      case Right((cur, parents)) => JsonWalk.rebuildPath(parents, suffix, f(cur))

  /** Like [[updateElementDecoded]] but the user's focus value is precomputed (a single encoded
    * Json). Used by [[placeImpl]] so the encoder runs once, not once per element.
    */
  private def placeElement(elemJson: Json, encoded: Json): Json =
    if suffix.length == 0 then encoded
    else
      JsonWalk.walkPathLenient(elemJson, suffix) match
        case Left(_)             => elemJson
        case Right((_, parents)) => JsonWalk.rebuildPath(parents, suffix, encoded)

  // ---- Internals: Ior-bearing walks --------------------------------

  /** Walk the prefix with failure-accumulation. Returns either an array (with a builder for
    * per-element accumulation) or an `Ior.Left(chain-of-one)` because there's nothing to iterate.
    * Shared by the default `modify` / `transform` / `place` / `transfer`.
    */
  private def navigateToArrayIor(
      json: Json
  ): Either[Ior.Left[Chain[JsonFailure]], (Vector[Json], Vector[AnyRef])] =
    JsonWalk
      .walkPath(json, prefix)
      .left
      .map(failure => Ior.Left(Chain.one(failure)))
      .flatMap {
        case (cur, parents) =>
          cur
            .asArray
            .map(arr => (arr, parents))
            .toRight(Ior.Left(Chain.one(JsonFailure.NotAnArray(JsonWalk.terminalOf(prefix)))))
      }

  override protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, updateElementDecodedIor(_, f))
        rebuildPrefix(newArrAndChain, parents)

  override protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, updateElementRawIor(_, f))
        rebuildPrefix(newArrAndChain, parents)

  override protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    val encoded = encoder(a)
    navigateToArrayIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val newArrAndChain: (Vector[Json], Chain[JsonFailure]) =
          mapElementsIor(arr, placeElementIor(_, encoded))
        rebuildPrefix(newArrAndChain, parents)

  /** Per-element map that accumulates each per-element Ior into a shared (Vector, Chain) pair.
    * Ior.Left from an element is treated as "couldn't update this element" — the element stays in
    * the output unchanged, failure contributes to the chain.
    */
  private def mapElementsIor(
      arr: Vector[Json],
      elemUpdate: Json => Ior[Chain[JsonFailure], Json],
  ): (Vector[Json], Chain[JsonFailure]) =
    arr.foldLeft((Vector.empty[Json], Chain.empty[JsonFailure])) {
      case ((acc, chain), elem) =>
        elemUpdate(elem) match
          case Ior.Right(j)   => (acc :+ j, chain)
          case Ior.Both(c, j) => (acc :+ j, chain ++ c)
          case Ior.Left(c)    => (acc :+ elem, chain ++ c)
    }

  /** Rebuild the prefix path back up to the root and wrap the result in the appropriate Ior shape:
    * Ior.Right on an empty chain, Ior.Both otherwise.
    */
  private def rebuildPrefix(
      arrAndChain: (Vector[Json], Chain[JsonFailure]),
      parents: Vector[AnyRef],
  ): Ior[Chain[JsonFailure], Json] =
    val (newArr, chain) = arrAndChain
    val newChild = JsonWalk.rebuildPath(parents, prefix, Json.fromValues(newArr))
    if chain.isEmpty then Ior.Right(newChild) else Ior.Both(chain, newChild)

  /** Per-element Ior-bearing decoded update. Walks the suffix, decodes the leaf, applies `f`,
    * re-encodes. Returns Ior.Both(chain-of-one, originalElement) if the suffix or decode misses
    * (the element is preserved in the output).
    */
  private def updateElementDecodedIor(
      elemJson: Json,
      f: A => A,
  ): Ior[Chain[JsonFailure], Json] =
    JsonWalk.walkPath(elemJson, suffix) match
      case Left(failure)         => Ior.Both(Chain.one(failure), elemJson)
      case Right((cur, parents)) =>
        decoder.decodeJson(cur) match
          case Right(a) => Ior.Right(JsonWalk.rebuildPath(parents, suffix, encoder(f(a))))
          case Left(df) =>
            Ior.Both(
              Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(suffix), df)),
              elemJson,
            )

  /** Per-element Ior-bearing raw-Json update — lenient walk so missing leaves become Json.Null. */
  private def updateElementRawIor(
      elemJson: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    JsonWalk.walkPathLenient(elemJson, suffix) match
      case Left(failure)         => Ior.Both(Chain.one(failure), elemJson)
      case Right((cur, parents)) => Ior.Right(JsonWalk.rebuildPath(parents, suffix, f(cur)))

  /** Per-element Ior-bearing place (constant Json). */
  private def placeElementIor(
      elemJson: Json,
      encoded: Json,
  ): Ior[Chain[JsonFailure], Json] =
    if suffix.length == 0 then Ior.Right(encoded)
    else
      JsonWalk.walkPathLenient(elemJson, suffix) match
        case Left(failure)       => Ior.Both(Chain.one(failure), elemJson)
        case Right((_, parents)) => Ior.Right(JsonWalk.rebuildPath(parents, suffix, encoded))

  /** Per-element Ior-bearing read: walk the suffix, then decode the leaf. Success = Ior.Right(a),
    * any miss = Ior.Left(chain-of-one). Used by `getAll`.
    */
  private def readElementIor(elemJson: Json): Ior[Chain[JsonFailure], A] =
    JsonWalk.walkPath(elemJson, suffix) match
      case Left(failure)   => Ior.Left(Chain.one(failure))
      case Right((cur, _)) =>
        decoder.decodeJson(cur) match
          case Right(a) => Ior.Right(a)
          case Left(df) =>
            Ior.Left(Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(suffix), df)))

  private def getAllIor(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    navigateToArrayIor(json) match
      case Left(l)         => l
      case Right((arr, _)) =>
        val (chain, result) =
          arr.foldLeft((Chain.empty[JsonFailure], Vector.empty[A])) {
            case ((chain, acc), elem) =>
              readElementIor(elem) match
                case Ior.Right(a)   => (chain, acc :+ a)
                case Ior.Left(c)    => (chain ++ c, acc)
                case Ior.Both(c, a) => (chain ++ c, acc :+ a)
          }
        if chain.isEmpty then Ior.Right(result) else Ior.Both(chain, result)

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

  /** Focus a bundle of named fields of a case class `A` as a Scala 3 NamedTuple, per element of the
    * iterated array. Multi-field sibling of [[field]] — arity ≥ 2 required (single- selector calls
    * should use `.field(_.x)`).
    *
    * {{{
    *   codecPrism[Basket].items.each.fields(_.name, _.price)
    *     // JsonFieldsTraversal[NamedTuple[("name","price"), (String, Double)]]
    * }}}
    *
    * Per-element atomicity (D4 / Unit 3 carry-over): if any selected field of an element misses,
    * that element is left unchanged in the output and one JsonFailure per missing field contributes
    * to the accumulated chain.
    */
  extension [A](t: JsonTraversal[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ JsonPrismMacro.fieldsTraversalImpl[A]('t, 'selectors) }
