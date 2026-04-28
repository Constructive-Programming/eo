package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.optics.Optic
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

/** Specialised Prism from [[io.circe.Json]] to native `A`.
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Either]
  *   type X = (DecodingFailure, HCursor)
  * }}}
  *
  * Two call-surface tiers:
  *   - Default Ior-bearing: `modify` / `get` etc. accumulate `Chain[JsonFailure]`; partial success
  *     surfaces as `Ior.Both(chain, inputJson)`.
  *   - `*Unsafe`: silent pass-through hot path.
  *
  * Storage decomposition: a `JsonPrism[A]` holds a [[JsonFocus]] (Leaf vs Fields). The
  * compatibility alias [[JsonFieldsPrism]] points back at this class so old code keeps compiling.
  */
final class JsonPrism[A] private[circe] (
    private[circe] val focus: JsonFocus[A]
) extends Optic[Json, Json, A, A, Either],
      JsonOpticOps[A],
      Dynamic:

  type X = (DecodingFailure, HCursor)

  /** The focus's storage path (`path` for a Leaf focus, `parentPath` for a Fields focus). */
  private[circe] def path: Array[PathStep] = focus match
    case l: JsonFocus.Leaf[A]   => l.path
    case f: JsonFocus.Fields[A] => f.parentPath

  /** Codec accessors — kept for the `widenPath*` paths and external readers. */
  private[circe] def encoder: Encoder[A] = focus.encoder
  private[circe] def decoder: Decoder[A] = focus.decoder

  // Selectable field sugar — `codecPrism[Person].name` lowers to
  // `codecPrism[Person].field(_.name)` via the macro.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  def to: Json => Either[(DecodingFailure, HCursor), A] = json =>
    val c = focus.navigateCursor(json)
    focus.decodeFromCursor(c) match
      case Right(a) => Right(a)
      case Left(df) => Left((df, json.hcursor))

  def from: Either[(DecodingFailure, HCursor), A] => Json = {
    case Left((_, hc)) => hc.top.getOrElse(hc.value)
    case Right(a)      => focus.encoder(a)
  }

  // ---- Read surface (single-focus specific) -------------------------

  def get(input: Json | String): Ior[Chain[JsonFailure], A] =
    JsonFailure.parseInputIor(input).flatMap(focus.readIor)

  inline def reverseGet(a: A): Json = focus.encoder(a)

  inline def getOptionUnsafe(input: Json | String): Option[A] =
    focus.readImpl(JsonFailure.parseInputUnsafe(input))

  // ---- JsonOpticOps wiring ------------------------------------------
  //
  // The JsonOpticOps mixin demands six per-Json hooks; this prism's
  // hooks delegate straight to the focus.

  override protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    focus.modifyIor(json, f)

  override protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json] =
    focus.transformIor(json, f)

  override protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    focus.placeIor(json, a)

  override protected def modifyImpl(json: Json, f: A => A): Json =
    focus.modifyImpl(json, f)

  override protected def transformImpl(json: Json, f: Json => Json): Json =
    focus.transformImpl(json, f)

  override protected def placeImpl(json: Json, a: A): Json =
    focus.placeImpl(json, a)

  // ---- Path widening (used by the macro extensions) ----------------

  /** Extend the Leaf path by a field step. Used by [[field]] / `selectDynamic`. */
  private[circe] def widenPath[B](
      step: String
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend by an array-index step. Used by [[at]]. */
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
    new JsonPrism[B](new JsonFocus.Leaf[B](newPath, encB, decB))

  /** Hand off as a `JsonTraversal[B]` over the iterated element type. Used by `.each`. */
  private[circe] def toTraversal[B](using
      encB: Encoder[B],
      decB: Decoder[B],
  ): JsonTraversal[B] =
    new JsonTraversal[B](path, new JsonFocus.Leaf[B](Array.empty[PathStep], encB, decB))

  /** Hand off as a `JsonPrism[B]` with a Fields focus over `fieldNames`. Used by `.fields`. */
  private[circe] def toFieldsPrism[B](
      fieldNames: Array[String]
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    new JsonPrism[B](new JsonFocus.Fields[B](path, fieldNames, encB, decB))

object JsonPrism:

  /** Construct a root-level `JsonPrism[S]` — the focus is a Leaf with empty path. */
  def apply[S](using enc: Encoder[S], dec: Decoder[S]): JsonPrism[S] =
    new JsonPrism[S](new JsonFocus.Leaf[S](Array.empty[PathStep], enc, dec))

  /** `.field(_.x)` — drill via selector lambda. */
  extension [A](o: JsonPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
      ${ JsonPrismMacro.fieldImpl[A, B]('o, 'selector, 'encB, 'decB) }

  /** `.at(i)` — drill into the i-th array element. */
  extension [A](o: JsonPrism[A])

    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atImpl[A]('o, 'i) }

  /** `.each` — split into a `JsonTraversal` over the iterated array. */
  extension [A](o: JsonPrism[A])

    transparent inline def each: Any =
      ${ JsonPrismMacro.eachImpl[A]('o) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple over selected fields. */
  extension [A](o: JsonPrism[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ JsonPrismMacro.fieldsImpl[A]('o, 'selectors) }
