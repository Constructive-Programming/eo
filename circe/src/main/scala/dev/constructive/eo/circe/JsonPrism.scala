package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.optics.Optic
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

/** A specialised Prism from [[io.circe.Json]] to a native type `A`.
  *
  * '''Design (2026-04-26 unification).''' Originally cats-eo shipped four sibling carriers —
  * `JsonPrism`, `JsonFieldsPrism`, `JsonTraversal`, `JsonFieldsTraversal` — each ~200 lines of
  * near-identical surface. They varied along two orthogonal axes:
  *
  *   1. ''single-field vs multi-field focus'' — [[JsonFocus.Leaf]] vs [[JsonFocus.Fields]];
  *   2. ''single-focus vs multi-focus'' — [[JsonPrism]] (apply at root) vs [[JsonTraversal]] (apply
  *      per-element of an array reached by a `prefix` walk).
  *
  * Two booleans, four classes — and the multi-field × single-focus class is just `JsonPrism` whose
  * focus is `JsonFocus.Fields(...)` instead of `JsonFocus.Leaf(...)`. That observation collapses
  * the four classes to two and the four "multi-field" duplicates (Fields-side reads / writes /
  * overlays) into one [[JsonFocus.Fields]] body.
  *
  * Shape on the Optic type (unchanged):
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Either]
  *   type X = (DecodingFailure, HCursor)
  * }}}
  *
  * Two call-surface tiers (unchanged):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` return
  *     `Json => Ior[Chain[JsonFailure], Json]`; `get` returns `Ior[Chain[JsonFailure], A]`.
  *     Failures (path miss, non-object / non-array parent, out-of-range index, decode failure)
  *     accumulate into `Chain[JsonFailure]`. Partial success returns `Ior.Both(chain, inputJson)`.
  *   - '''`*Unsafe` (silent).''' `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` /
  *     `transferUnsafe` / `getOptionUnsafe` preserve the pre-v0.2 forgiving behaviour
  *     byte-for-byte: silent pass-through on the modify family, `Option[A]` on the read family.
  *
  * Failure diagnostics on the abstract `Optic` surface: the inherited `to` returns
  * `Left((DecodingFailure, HCursor))` so callers routing through the generic cats-eo extensions can
  * both diagnose (`DecodingFailure.history`) and recover raw Json (`HCursor.value` /
  * `HCursor.top`).
  *
  * The compatibility alias [[JsonFieldsPrism]] points back at `JsonPrism[A]` — old code that
  * ascribed `: JsonFieldsPrism[NT]` keeps compiling.
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

  /** Codec accessors — kept for the `widenPath` / `widenPathIndex` paths and for backwards
    * compatibility with code that read these fields off the prism directly.
    */
  private[circe] def encoder: Encoder[A] = focus.encoder
  private[circe] def decoder: Decoder[A] = focus.decoder

  // ---- Selectable field sugar ---------------------------------------
  //
  // `codecPrism[Person].address.street` compiles to
  // `codecPrism[Person].field(_.address).field(_.street)` — no
  // explicit selector lambdas, no extension-method noise. The macro
  // looks the field up on `A`'s case-class schema at compile time,
  // summons its Encoder/Decoder, and delegates to `widenPath`.

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

  /** Extend the leaf focus's path by a field step and swap to a narrower codec pair. Only valid
    * when the current focus is a Leaf (the `field` macro never composes Fields focuses with further
    * `.field`). Used by [[field]] / `selectDynamic`.
    */
  private[circe] def widenPath[B](
      step: String
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend the leaf focus's path by an array-index step. Used by [[at]]. */
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

  /** Hand off the current path as a [[JsonTraversal]] prefix; the new focus is a Leaf focus over
    * the iterated element type `B`. Used by the `.each` macro.
    */
  private[circe] def toTraversal[B](using
      encB: Encoder[B],
      decB: Decoder[B],
  ): JsonTraversal[B] =
    new JsonTraversal[B](path, new JsonFocus.Leaf[B](Array.empty[PathStep], encB, decB))

  /** Hand off the current path as a [[JsonPrism]] whose focus is a Fields focus enumerating
    * `fieldNames` under that parent. Used by the `.fields` macro. The static return type is
    * `JsonPrism[B]` — historically named `JsonFieldsPrism[B]`, kept as an alias.
    */
  private[circe] def toFieldsPrism[B](
      fieldNames: Array[String]
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    new JsonPrism[B](new JsonFocus.Fields[B](path, fieldNames, encB, decB))

object JsonPrism:

  /** Construct a root-level `JsonPrism[S]` — the focus is a Leaf with empty path. */
  def apply[S](using enc: Encoder[S], dec: Decoder[S]): JsonPrism[S] =
    new JsonPrism[S](new JsonFocus.Leaf[S](Array.empty[PathStep], enc, dec))

  /** `.field(_.x)` — drill into a named field via a selector lambda. The macro extracts the field
    * name at compile time, summons the inner codec, and emits `widenPath`.
    */
  extension [A](o: JsonPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
      ${ JsonPrismMacro.fieldImpl[A, B]('o, 'selector, 'encB, 'decB) }

  /** `.at(i)` — drill into the i-th element of a JSON array. Requires the parent focus `A` to be a
    * Scala collection.
    */
  extension [A](o: JsonPrism[A])

    transparent inline def at(i: Int): Any =
      ${ JsonPrismMacro.atImpl[A]('o, 'i) }

  /** `.each` — split into a [[JsonTraversal]] over the iterated array's elements. */
  extension [A](o: JsonPrism[A])

    transparent inline def each: Any =
      ${ JsonPrismMacro.eachImpl[A]('o) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple over selected fields. Returns a `JsonPrism[NT]`
    * whose focus is a [[JsonFocus.Fields]] (formerly `JsonFieldsPrism[NT]`).
    */
  extension [A](o: JsonPrism[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ JsonPrismMacro.fieldsImpl[A]('o, 'selectors) }
