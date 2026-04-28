package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import io.circe.{Decoder, Encoder, Json}

/** Multi-focus counterpart to [[JsonPrism]]: walks the JSON to some array, then applies the focus
  * update to every element. Two pieces: `prefix: Array[PathStep]` (root-to-array, walked once) and
  * `focus: JsonFocus[A]` (per-element). The Leaf-vs-Fields split lives in `focus`. Compat alias
  * [[JsonFieldsTraversal]] points back here.
  *
  * Two tiers (Ior-bearing default + `*Unsafe`), same shape as [[JsonPrism]].
  */
final class JsonTraversal[A] private[circe] (
    private[circe] val prefix: Array[PathStep],
    private[circe] val focus: JsonFocus[A],
) extends JsonOpticOps[A],
      Dynamic:

  /** Compat shim — historically the suffix was a bare `val`; the read-side accessor is preserved
    * for `widenSuffix*` macro extensions.
    */
  private[circe] def suffix: Array[PathStep] = focus match
    case l: JsonFocus.Leaf[A]   => l.path
    case f: JsonFocus.Fields[A] => f.parentPath

  private[circe] def encoder: Encoder[A] = focus.encoder
  private[circe] def decoder: Decoder[A] = focus.decoder

  // ---- Dynamic field sugar -----------------------------------------

  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Read surface (multi-focus specific) --------------------------

  def getAll(input: Json | String): Ior[Chain[JsonFailure], Vector[A]] =
    JsonFailure.parseInputIor(input).flatMap(getAllIor)

  inline def getAllUnsafe(input: Json | String): Vector[A] =
    val json = JsonFailure.parseInputUnsafe(input)
    walkPrefixOpt(json).fold(Vector.empty[A])(arr => arr.flatMap(focus.readImpl))

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
    new JsonTraversal[B](prefix, new JsonFocus.Leaf[B](newSuffix, encB, decB))

  /** Hand off as a multi-field traversal whose focus is `JsonFocus.Fields` over `fieldNames`. */
  private[circe] def toFieldsTraversal[B](
      fieldNames: Array[String]
  )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
    new JsonTraversal[B](prefix, new JsonFocus.Fields[B](suffix, fieldNames, encB, decB))

  // ---- Prefix walks -------------------------------------------------

  private def walkPrefixOpt(json: Json): Option[Vector[Json]] =
    JsonWalk.walkPath(json, prefix).toOption.flatMap(_._1.asArray)

  private def walkPrefixIor(
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

  // ---- *Unsafe surface — delegates to focus.modifyImpl etc. --------

  override protected def modifyImpl(json: Json, f: A => A): Json =
    mapAtPrefix(json)(elem => focus.modifyImpl(elem, f))

  override protected def transformImpl(json: Json, f: Json => Json): Json =
    mapAtPrefix(json)(elem => focus.transformImpl(elem, f))

  override protected def placeImpl(json: Json, a: A): Json =
    mapAtPrefix(json)(elem => focus.placeImpl(elem, a))

  /** Walk prefix, replace the focused array by mapping each element. Prefix miss → input. */
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

  // ---- Default Ior surface — delegates to focus.modifyIor etc. -----

  override protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    iorMapElements(json)(elem => focus.modifyIor(elem, f))

  override protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    iorMapElements(json)(elem => focus.transformIor(elem, f))

  override protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    iorMapElements(json)(elem => focus.placeIor(elem, a))

  /** Shared backbone for the three Ior-bearing modify/transform/place surfaces. */
  private def iorMapElements(
      json: Json
  )(elemUpdate: Json => Ior[Chain[JsonFailure], Json]): Ior[Chain[JsonFailure], Json] =
    walkPrefixIor(json) match
      case Left(l)               => l
      case Right((arr, parents)) =>
        val (newArr, chain) = JsonTraversalAccumulator.mapElementsIor(arr, elemUpdate)
        val newChild = JsonWalk.rebuildPath(parents, prefix, Json.fromValues(newArr))
        if chain.isEmpty then Ior.Right(newChild) else Ior.Both(chain, newChild)

  private def getAllIor(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    walkPrefixIor(json) match
      case Left(l)         => l
      case Right((arr, _)) => JsonTraversalAccumulator.collectIor(arr, focus.readIor)

object JsonTraversal:

  /** `.field(_.x)` sugar — extend the suffix by a named field. */
  extension [A](t: JsonTraversal[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using encB: Encoder[B], decB: Decoder[B]): JsonTraversal[B] =
      ${ JsonPrismMacro.fieldTraversalImpl[A, B]('t, 'selector, 'encB, 'decB) }

  /** `.at(i)` sugar — extend the suffix by an array index. */
  extension [A](t: JsonTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atTraversalImpl[A]('t, 'i) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple per element. */
  extension [A](t: JsonTraversal[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ JsonPrismMacro.fieldsTraversalImpl[A]('t, 'selectors) }
