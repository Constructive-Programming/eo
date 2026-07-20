package dev.constructive.eo.jsoniter

import scala.language.dynamics

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromSubArray, writeToArray, JsonValueCodec}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic

/** Read-write optic over a JSON byte buffer. Resolves a JSONPath subset against `Array[Byte]`,
  * decodes the focused slice via `JsonValueCodec[A]` on read, encodes-and-splices on write. No
  * runtime AST.
  *
  * '''Two usage modes''' — pick deliberately:
  *
  *   - '''Layer on an existing codec''' (hot-path reads): you keep materialising your case class
  *     elsewhere and use a prism for the one or two fields on the hot path.
  *   - '''Replace the materialised model''' (optics-as-evidence): the wire `Array[Byte]` IS the
  *     data structure. Instead of `JsonCodecMaker.make[Whole]` + decoding the whole document, hold
  *     the bytes and read/write individual fields through [[JsoniterPrism.apply]] /
  *     [[JsoniterPrism.field]] with leaf codecs only. Consuming code then follows the standard
  *     doctrine — consume via capability, construct via optic — leaving the CARRIER generic:
  *     signatures demand `CanGetOption[T, WhatYouWant]` (or `CanModify` / `CanFold`), never naming
  *     the optic or the wire format,
  *     {{{
  *     def validateId[T](idCarrier: T)(using CanGetOption[T, Id]): Boolean
  *     }}}
  *     and a prism given at the use site is the evidence that instantiates `T = Array[Byte]`. No
  *     `JsonValueCodec[Whole]` is ever derived — do NOT go looking for a codec-derivation API in
  *     this module; not needing one is the point. See the migration recipe in the jsoniter
  *     integration docs.
  *
  * Carrier: [[dev.constructive.eo.data.Affine]]. Shape
  * `Optic[Array[Byte], Array[Byte], A, A, Affine]`:
  *
  *   - `type X = (Array[Byte], (Array[Byte], Int, Int))`
  *     - `Fst[X] = Array[Byte]` — original source bytes (Miss carries this for pass-through)
  *     - `Snd[X] = (Array[Byte], Int, Int)` — bytes + the focused span (Hit carries this; phase-2
  *       splice writes use the span to memcpy the new encoding back in)
  *   - `to: Array[Byte] => Affine[X, A]` runs the path scanner; on hit, decodes the slice via
  *     `readFromSubArray` and packs `Hit(snd = (bytes, start, end), b = decoded)`. On miss (path
  *     doesn't resolve, decode throws), packs `Miss(fst = bytes)` for pass-through.
  *   - `from: Affine[X, A] => Array[Byte]` (phase 2) — Hit encodes `h.b` via `writeToArray` and
  *     splices into the source bytes at the recorded `[start, end)` span. Three `arraycopy`s into a
  *     fresh buffer; cost is O(src.length). Miss returns the original bytes unchanged.
  *
  * Composability: the standard cats-eo extension methods on `Optic[..., Affine]` light up
  * automatically — `.foldMap`, `.modify`, `.replace`, `.andThen`, etc. No new Composer /
  * AssociativeFunctor needed; reuses the existing Affine machinery.
  *
  * Typed drilling (compile-time checked against the case-class schema, mirroring eo-circe's
  * `JsonPrism` surface):
  *
  * {{{
  * val streetP = JsoniterPrism[Person].field(_.address).field(_.street)
  * val street2 = JsoniterPrism[Person].address.street        // Dynamic sugar, same optic
  * val firstT  = JsoniterPrism[Basket].field(_.items).at(0)  // array index
  * val allT    = JsoniterPrism[Basket].field(_.items).each   // JsoniterTraversal
  * }}}
  *
  * Each drilled step needs a `JsonValueCodec` for its focus type in scope (derive leaf codecs via
  * `JsonCodecMaker.make` from `jsoniter-scala-macros`, which callers add themselves). To skip
  * intermediate codecs entirely, use the string-path factory:
  * `JsoniterPrism.fromPath[String]("\$.a.b")` needs only the leaf codec.
  *
  * '''Laws & preconditions''' (normative):
  *
  *   - The Optional laws hold '''up to canonical re-encoding of the focused slice''':
  *     `modify(identity)` re-encodes the focus through the codec, normalising number forms (`1e0` →
  *     `1.0`), escapes, and key order INSIDE the span. Bytes outside the span (whitespace, sibling
  *     formatting) are never touched. Byte-for-byte identity holds only for focus slices already in
  *     the codec's canonical form.
  *   - '''Writes require a decodable current focus''': the Affine `to` decodes eagerly, so
  *     `.replace` onto a span whose current value doesn't decode as `A` is a Miss pass-through —
  *     template placeholders must be VALID encodings of the focus type.
  *   - Only the ROOT prism ([[JsoniterPrism.apply]], empty path) is additionally a lawful
  *     full-cover Prism: there [[reverseGet]] is a genuine build and `to` misses only on
  *     undecodable input.
  *
  * @group Optics
  */
final class JsoniterPrism[A] private[jsoniter] (
    private[jsoniter] val steps: List[PathStep]
)(using codec: JsonValueCodec[A])
    extends Optic[Array[Byte], Array[Byte], A, A, Affine],
      Dynamic:

  type X = (Array[Byte], (Array[Byte], Int, Int))

  // `to` / `from` are 1-call forwarders; the large bodies live in private
  // methods so the abstract-`def` entry stays a few bytes and inlines into
  // every caller without blowing its C2 inlining budget — PrintInlining
  // showed the direct 103/175-byte bodies failing with "callee is too
  // large" at hot use sites, where the old `val to` encoding's 12-byte
  // Function1 bridges inlined everywhere (the big body compiled
  // separately). The forwarders recreate that shape under the `def`
  // encoding: `to (6 bytes) inline` at every site.
  def to(bytes: Array[Byte]): Affine[X, A] = scan(bytes)
  def from(aff: Affine[X, A]): Array[Byte] = splice(aff)

  /** Encode `a` standalone. Lawful only for the ROOT prism (a real `Prism.reverseGet` —
    * `Array[Byte] <-> A` whole-document); on a drilled prism it cannot restore siblings, which is
    * why the Optic seam carries the source bytes instead of calling this.
    */
  def reverseGet(a: A): Array[Byte] = writeToArray(a)(using codec)

  /** Dynamic field sugar — `JsoniterPrism[Person].name` lowers to
    * `JsoniterPrism[Person].field(_.name)`. Compile-time checked against `A`'s case fields; the
    * field's `JsonValueCodec` is summoned at the call site.
    */
  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsoniterPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  private def scan(bytes: Array[Byte]): Affine[X, A] =
    val span = JsonPathScanner.find(bytes, steps)
    if !span.isHit then new Affine.Miss[X, A](bytes)
    else
      try
        val a = readFromSubArray(bytes, span.start, span.end)(using codec)
        new Affine.Hit[X, A]((bytes, span.start, span.end), a)
      catch case scala.util.control.NonFatal(_) => new Affine.Miss[X, A](bytes)

  private def splice(aff: Affine[X, A]): Array[Byte] =
    aff match
      case h: Affine.Hit[X, A] =>
        // Phase-2 splice. h.snd carries (src, start, end). The user's `.modify` /
        // `.replace` arrives baked into h.b — encode it via the codec, then memcpy
        // src[0, start) ++ encoded ++ src[end, src.length) into a fresh buffer.
        val (src, start, end) = h.snd
        val encoded = writeToArray(h.b)(using codec)
        val tail = src.length - end
        val out = new Array[Byte](start + encoded.length + tail)
        System.arraycopy(src, 0, out, 0, start)
        System.arraycopy(encoded, 0, out, start, encoded.length)
        System.arraycopy(src, end, out, start + encoded.length, tail)
        out
      case m: Affine.Miss[X, A] => m.fst

  // ---- Path widening (used by the macro extensions) ----------------

  /** Extend the path by a field step. Used by [[JsoniterPrism.field]] / `selectDynamic`. */
  private[jsoniter] def widenField[B](name: String)(using JsonValueCodec[B]): JsoniterPrism[B] =
    new JsoniterPrism[B](steps :+ PathStep.Field(name))

  /** Extend by an array-index step. Used by [[JsoniterPrism.at]]. */
  private[jsoniter] def widenIndex[B](i: Int)(using JsonValueCodec[B]): JsoniterPrism[B] =
    new JsoniterPrism[B](steps :+ PathStep.Index(i))

  /** Hand off as a [[JsoniterTraversal]] over the iterated element type. Used by
    * [[JsoniterPrism.each]].
    */
  private[jsoniter] def toTraversal[B](using JsonValueCodec[B]): JsoniterTraversal[B] =
    new JsoniterTraversal[B](steps :+ PathStep.Wildcard)

object JsoniterPrism:

  /** Root-level Prism from a JSON byte buffer to a native type `A` — a `Prism[Array[Byte], A]`:
    * `to` decodes the WHOLE document via the codec (Miss when it doesn't decode as `A`), `from` /
    * [[JsoniterPrism.reverseGet]] encode via `writeToArray`. At the root the full-cover Prism laws
    * hold (up to canonical re-encoding). Drill from here with `.field`:
    * `JsoniterPrism[Person].field(_.age)`.
    *
    * @group Optics
    */
  def apply[A](using JsonValueCodec[A]): JsoniterPrism[A] =
    fromSteps(Nil)

  /** Build a read-write Prism over a JSON byte buffer at the given JSONPath.
    *
    * Throws `IllegalArgumentException` if the path string is not parseable. Path syntax: `$`,
    * dotted field names (`$.foo.bar`), array indices (`$[0]`). See [[PathParser]] for the full
    * grammar.
    *
    * @example
    *   {{{
    * import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    * given codec: JsonValueCodec[Long] = JsonCodecMaker.make
    *
    * val idP = JsoniterPrism.fromPath[Long]("\$.payload.user.id")
    * val bytes: Array[Byte] = ...
    * idP.foldMap(identity)(bytes)  // Long, no AST allocation
    *   }}}
    *
    * @group Optics
    */
  def fromPath[A](path: String)(using
      codec: JsonValueCodec[A]
  ): JsoniterPrism[A] =
    val steps = PathParser.parse(path) match
      case Right(s) => s
      case Left(e)  => throw new IllegalArgumentException(s"invalid JSONPath '$path': $e")
    if steps.contains(PathStep.Wildcard) then
      throw new IllegalArgumentException(
        s"JsoniterPrism path '$path' contains '[*]' — use JsoniterTraversal for wildcard paths"
      )
    fromSteps(steps)

  /** Build a Prism from an already-parsed step list — useful for reusing a parsed path across
    * multiple optics, or when constructing programmatically.
    *
    * @group Optics
    */
  def fromSteps[A](steps: List[PathStep])(using
      codec: JsonValueCodec[A]
  ): JsoniterPrism[A] =
    new JsoniterPrism[A](steps)

  /** `.field(_.x)` — drill via selector lambda, compile-time checked against `A`'s case fields. */
  extension [A](o: JsoniterPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: JsonValueCodec[B]): JsoniterPrism[B] =
      ${ JsoniterPrismMacro.fieldImpl[A, B]('o, 'selector, 'codecB) }

  /** `.at(i)` — drill into the i-th array element. */
  extension [A](o: JsoniterPrism[A])

    transparent inline def at(i: Int): Any =
      ${ JsoniterPrismMacro.atImpl[A]('o, 'i) }

  /** `.each` — split into a [[JsoniterTraversal]] over the iterated array. */
  extension [A](o: JsoniterPrism[A])

    transparent inline def each: Any =
      ${ JsoniterPrismMacro.eachImpl[A]('o) }
