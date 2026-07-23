package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import io.circe.{Decoder, Encoder, Json}

/** Specialised optic from [[io.circe.Json]] to native `A`.
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Affine]
  *   type X = (Json, A => Json)  // Fst = source (miss); Snd = single-walk writer
  * }}}
  *
  * '''Two usage modes''' — pick deliberately:
  *
  *   - '''Layer on an existing model''' (hot-path edits): you keep decoding your case class
  *     elsewhere and use a prism for the one or two fields on the hot path.
  *   - '''Replace the materialised model''' (optics-as-evidence): the `Json` — or the wire
  *     `String`, parsed on the fly — IS the data structure. `json.as[Whole]` never runs — do NOT go
  *     looking for a whole-document decode step; not needing one is the point. Hold the `Json` and
  *     read/write individual fields through `codecPrism` with leaf codecs only. Consuming code then
  *     follows the standard doctrine — consume via capability, construct via optic — leaving the
  *     CARRIER generic:
  *     {{{
  *     def widenStreet[T](t: T)(using cm: CanModify[T, String]): T = cm.replace("Broadway")(t)
  *     }}}
  *     and a `JsonPrism` given at the use site is the evidence that instantiates `T = Json`. See
  *     the migration recipe in the circe integration docs.
  *
  * '''An Optional, not a Prism.''' A drilled focus lives INSIDE a document, so rebuilding needs the
  * siblings — which a `Prism`'s `from(reverseGet)` cannot see (it gets the focus alone). Carrying
  * the source on the `Affine` seam fixes that at the carrier level: `to` captures a writer over the
  * walk it already did, and `from(Hit)` applies it — so `.modify` / `.replace` preserve siblings
  * whether called directly, upcast to `Optic[…, Affine]`, or composed via `andThen`. Only the root
  * full-cover prism is also a lawful Prism, and [[reverseGet]] stays for it. Mirrors
  * `dev.constructive.eo.avro.AvroRecordPrism`.
  *
  * Two call-surface tiers:
  *   - Default Ior-bearing: `modify` / `get` etc. accumulate `Chain[JsonFailure]`; partial success
  *     surfaces as `Ior.Both(chain, inputJson)`.
  *   - `*Unsafe`: silent pass-through hot path.
  *
  * Storage decomposition: a `JsonPrism[A]` holds a `JsonFocus` (Leaf vs Fields).
  */
final class JsonPrism[A] private[circe] (
    private[circe] val focus: JsonFocus[A]
) extends Optic[Json, Json, A, A, Affine],
      JsonOpticOps[A],
      Dynamic:

  /** Existential seam: `Fst[X]` = source json (Miss pass-through); `Snd[X]` = the single-walk
    * sibling-preserving writer captured by [[to]].
    */
  type X = (Json, A => Json)

  /** The focus's storage path (`path` for a Leaf focus, `parentPath` for a Fields focus). */
  private[circe] def path: Array[PathStep] = focus match
    case l: JsonFocus.Leaf[A]   => l.path
    case f: JsonFocus.Fields[A] => f.parentPath

  /** Codec accessors — kept for the `widenPath*` paths and external readers. */
  private[circe] def encoder: Encoder[A] = focus.encoder
  private[circe] def decoder: Decoder[A] = focus.decoder

  /** Dynamic field sugar — `codecPrism[Person].name` lowers to `codecPrism[Person].field(_.name)`.
    * Compile-time checked against `A`'s case fields; the field's codecs are summoned at the call
    * site, so the result is a precisely-typed child `JsonPrism`.
    */
  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsonPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  /** Navigate + decode in ONE walk. Hit carries the decoded focus and a writer that splices a new
    * focus back into the SOURCE json (siblings preserved); Miss carries the source for
    * pass-through.
    */
  def to(json: Json): Affine[X, A] =
    focus.navigateForWrite(json) match
      case Left(_)            => new Affine.Miss[X](json)
      case Right((a, writer)) => new Affine.Hit[X, A](writer, a)

  /** Rebuild: Hit applies the captured writer to the (possibly modified) focus; Miss returns the
    * source unchanged.
    */
  def from(aff: Affine[X, A]): Json =
    aff match
      case m: Affine.Miss[X]   => m.fst
      case h: Affine.Hit[X, A] => h.snd(h.b)

  // ---- Read surface (single-focus specific) -------------------------

  /** Read the focus. `String` input is parsed first (parse failures arrive as
    * `JsonFailure.ParseFailed`); navigation / decode failures surface as `Ior.Left(chain)`.
    */
  def get(input: Json | String): Ior[Chain[JsonFailure], A] =
    JsonFailure.parseInputIor(input).flatMap(focus.readIor)

  /** Encode `a` standalone. Lawful only for the ROOT full-cover prism (a real `Prism.reverseGet`);
    * on a drilled prism it cannot restore siblings, which is why the Optic seam carries the source
    * instead of calling this. Kept for root full-cover users and the reverseGet round-trip laws.
    */
  inline def reverseGet(a: A): Json = focus.encoder(a)

  /** Silent read — `None` on any parse / navigation / decode failure. */
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
