package dev.constructive.eo.jsoniter

import scala.language.dynamics

import com.github.plokhotnyuk.jsoniter_scala.core.{
  readFromSubArray,
  writeToArray,
  JsonReader,
  JsonValueCodec,
  JsonWriter
}
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
  *     the bytes and read/write individual fields through [[JsoniterPrism.apply]] / `.field` with
  *     leaf codecs only. Consuming code then follows the standard doctrine — consume via
  *     capability, construct via optic — leaving the CARRIER generic: signatures demand
  *     `CanGetOption[T, WhatYouWant]` (or `CanModify` / `CanFold`), never naming the optic or the
  *     wire format,
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
  * '''In place.''' A document inside a larger buffer (one network read holding several messages,
  * say) is read where it lies by the in-place face: `prism.inPlace` — or
  * `JsoniterPrism.inPlace[A]`, to drill from there — is the same prism over a [[JsonSlice]]
  * (`JsonSlice(bytes, offset, length)`, a view, nothing copied), with the same laws. This prism's
  * whole-array scan and splice are the `(0, length)` case of the in-place face's. See
  * [[JsoniterInPlacePrism]] for what not copying asks of the caller, and what writes return.
  *
  * Each drilled step needs a `JsonValueCodec` for its focus type in scope (derive leaf codecs via
  * `JsonCodecMaker.make` from `jsoniter-scala-macros`, which callers add themselves). To skip
  * intermediate codecs entirely, use the string-path factory — `JsoniterPrism.fromPath[String]`
  * needs only the leaf codec, and returns the prism in an `Either` (a path is data).
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
  *   - '''A write that fails to ENCODE the new value is a silent pass-through''' (`from` is total
  *     by type — there is nowhere in `Optic.from` to put the failure). On a drilled prism the
  *     original slice survives and the call reports success. Pinned by `JsoniterPrismWriteSpec`;
  *     see `docs/research/2026-09-22-exception-audit.md` (class C) for the carrier-level fix,
  *     tracked as issue #117.
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
      JsoniterPrismPath[A, JsoniterPrism, JsoniterTraversal]:

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

  /** Re-focus this prism on the '''raw encoded slice''': same path, same span scan, but the focus
    * is the focused value's own JSON bytes — captured verbatim on read (`readRawValAsBytes`),
    * spliced verbatim on write (`writeRawVal`). No typed decode ever runs. The seam for
    * format-level consumers that transform the slice as a document of its own — e.g. the avro
    * bridge's structural `.avro` face (`dev.constructive.eo.avro.jsoniter`).
    *
    * The focus is a '''copy''' of the span, in an array of its own. The in-place face's `.raw`
    * ([[JsoniterInPlacePrism.raw]]) focuses a [[JsonSlice]] view of the span instead.
    */
  def raw: JsoniterPrism[Array[Byte]] =
    new JsoniterPrism[Array[Byte]](steps)(using JsoniterPrism.rawCodec)

  /** This prism, reading its document '''in place''': same path, same focus, but the source is a
    * [[JsonSlice]] — a range of a larger byte array — scanned and decoded where it lies.
    * `JsoniterPrism[Person].field(_.age).inPlace` is the same optic as
    * `JsoniterPrism.inPlace[Person].field(_.age)`: drill before or after.
    *
    * '''No copy is made''': the optic reads, and with `.raw` focuses, the caller's array where it
    * lies. So:
    *
    *   - '''Don't mutate the array''' while the slice, or anything read from it, is in use.
    *   - A slice, and any slice focus (`.raw`), keeps the '''whole''' underlying array reachable:
    *     call [[JsonSlice.toArray]] for anything kept long-term.
    *   - '''Writes never mutate the source.''' A write that splices returns a slice over a
    *     '''fresh''' array holding only the slice's bytes, the new focus spliced in (offset 0); one
    *     that changes nothing (a miss) hands back the input slice itself.
    *
    * See [[JsoniterInPlacePrism]].
    */
  def inPlace: JsoniterInPlacePrism[A] = new JsoniterInPlacePrism[A](steps, InPlaceFocus.typed)

  // The whole array is the `(0, length)` case of the ranged scan and splice the in-place face uses.
  private def scan(bytes: Array[Byte]): Affine[X, A] =
    JsoniterPrism.scanRange[A, Affine[X, A]](bytes, 0, bytes.length, steps)(
      (start, end) => readFromSubArray(bytes, start, end)(using codec),
      new Affine.Miss[X](bytes),
      (start, end, a) => new Affine.Hit[X, A]((bytes, start, end), a),
    )

  private def splice(aff: Affine[X, A]): Array[Byte] =
    aff match
      case h: Affine.Hit[X, A] =>
        // Phase-2 splice. h.snd carries (src, start, end). The user's `.modify` /
        // `.replace` arrives baked into h.b — encode it via the codec, then memcpy
        // src[0, start) ++ encoded ++ src[end, src.length) into a fresh buffer.
        val (src, start, end) = h.snd
        val encoded = writeToArray(h.b)(using codec)
        JsoniterPrism.spliceRange(src, 0, src.length, start, end, encoded, 0, encoded.length)
      case m: Affine.Miss[X] => m.fst

  // ---- Path building (for the JsoniterPath macros) ----------------

  private[jsoniter] def withSteps[B](steps: List[PathStep])(using
      JsonValueCodec[B]
  ): JsoniterPrism[B] =
    new JsoniterPrism[B](steps)

  private[jsoniter] def toTraversal[B](using JsonValueCodec[B]): JsoniterTraversal[B] =
    new JsoniterTraversal[B](steps :+ PathStep.Wildcard)

object JsoniterPrism:

  /** Scan the document in `bytes[from, until)` for `steps` and read the focused span with `decode`
    * (which may throw): `hit(start, end, value)` with the span's absolute positions, or `miss` when
    * the path doesn't resolve or the span doesn't decode. Inline, so the whole-array prism and its
    * in-place face share one body (and the one decode `catch`) without a call or a closure between
    * them.
    */
  private[jsoniter] inline def scanRange[A, R](
      bytes: Array[Byte],
      from: Int,
      until: Int,
      steps: List[PathStep],
  )(inline decode: (Int, Int) => A, inline miss: R, inline hit: (Int, Int, A) => R): R =
    val span = JsonPathScanner.find(bytes, from, until, steps)
    if !span.isHit then miss
    else
      try
        val a = decode(span.start, span.end)
        hit(span.start, span.end, a)
      catch case scala.util.control.NonFatal(_) => miss

  /** `src[from, start) ++ enc[encFrom, encFrom + encLength) ++ src[end, until)` in a fresh array:
    * the document in `src[from, until)` with its `[start, end)` span replaced by the encoded focus.
    * Three `arraycopy`s.
    */
  private[jsoniter] def spliceRange(
      src: Array[Byte],
      from: Int,
      until: Int,
      start: Int,
      end: Int,
      enc: Array[Byte],
      encFrom: Int,
      encLength: Int,
  ): Array[Byte] =
    val head = start - from
    val tail = until - end
    val out = new Array[Byte](head + encLength + tail)
    System.arraycopy(src, from, out, 0, head)
    System.arraycopy(enc, encFrom, out, head, encLength)
    System.arraycopy(src, end, out, head + encLength, tail)
    out

  /** Raw-slice codec behind [[JsoniterPrism.raw]]: decode captures the focused value's encoded
    * bytes verbatim, encode splices them back verbatim.
    */
  private[jsoniter] val rawCodec: JsonValueCodec[Array[Byte]] = new JsonValueCodec[Array[Byte]]:
    def decodeValue(in: JsonReader, default: Array[Byte]): Array[Byte] = in.readRawValAsBytes()
    def encodeValue(bs: Array[Byte], out: JsonWriter): Unit = out.writeRawVal(bs)
    def nullValue: Array[Byte] = null.asInstanceOf[Array[Byte]]

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

  /** The root prism, reading its document '''in place''' — `JsoniterPrism[A].inPlace`, a
    * `Prism[JsonSlice, A]` to drill from: `JsoniterPrism.inPlace[Issue].field(_.detail).raw`.
    *
    * '''No copy is made''': the optic reads, and with `.raw` focuses, the caller's array where it
    * lies. Don't mutate the array while the slice or anything read from it is in use; a slice, and
    * any slice focus, keeps the whole underlying array reachable (call [[JsonSlice.toArray]] for
    * anything kept long-term); writes never mutate the source, but return a slice over a fresh
    * array. See [[JsoniterInPlacePrism]].
    *
    * @group Optics
    */
  def inPlace[A](using JsonValueCodec[A]): JsoniterInPlacePrism[A] =
    fromSteps[A](Nil).inPlace

  /** Build a read-write Prism over a JSON byte buffer at the given JSONPath — the ONLY string-path
    * constructor, and it keeps the failure in the type. An unparseable path, or one carrying a
    * `[*]` wildcard (a [[JsoniterTraversal]] shape), comes back as `Left(message)`: a path is DATA
    * (a config value, a CLI argument, a registry lookup), so there is no throwing twin to reach
    * for. Path syntax: `$`, dotted field names (`$.foo.bar`), array indices (`$[0]`). See
    * [[PathParser]] for the full grammar; [[apply]] is the path-free root constructor.
    *
    * @example
    *   {{{
    * import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    * given codec: JsonValueCodec[Long] = JsonCodecMaker.make
    *
    * val idP: Either[String, JsoniterPrism[Long]] =
    *   JsoniterPrism.fromPath[Long]("$.payload.user.id")
    * val bytes: Array[Byte] = ...
    * idP.map(_.foldMap(identity)(bytes))   // Either[String, Long], no AST allocation
    *   }}}
    *
    * @group Constructors
    */
  def fromPath[A](path: String)(using
      codec: JsonValueCodec[A]
  ): Either[String, JsoniterPrism[A]] =
    PathParser.parse(path) match
      case Right(steps) =>
        if steps.contains(PathStep.Wildcard) then
          Left(
            s"JsoniterPrism path '$path' contains '[*]' — use JsoniterTraversal for wildcard paths"
          )
        else Right(fromSteps[A](steps))
      case Left(e) => Left(s"invalid JSONPath '$path': $e")

  /** Build a Prism from an already-parsed step list — useful for reusing a parsed path across
    * multiple optics, or when constructing programmatically.
    *
    * @group Optics
    */
  private[jsoniter] def fromSteps[A](steps: List[PathStep])(using
      codec: JsonValueCodec[A]
  ): JsoniterPrism[A] =
    new JsoniterPrism[A](steps)

/** A [[JsoniterPrism]] that reads its document '''in place''': the source is a [[JsonSlice]], a
  * range of a larger byte array, scanned and decoded where it lies. Made by `prism.inPlace`, or at
  * the root by [[JsoniterPrism.inPlace]] to drill from there with the same `.field` / `.at` /
  * `.each` / Dynamic surface ([[JsoniterPrismPath]]). Same path, same codec, and the same '''Laws &
  * preconditions''' as the whole-array prism, with slices compared by content.
  *
  * '''No copy is made.''' The optic reads the caller's array where it lies, and `.raw` focuses a
  * view of it. So:
  *
  *   - '''Don't mutate the array''' while the slice, or anything read from it (a `.raw` focus), is
  *     in use: the optic reads the bytes as they are when it runs.
  *   - A slice, and any slice focus, keeps the '''whole''' underlying array reachable, however few
  *     bytes it covers. Call [[JsonSlice.toArray]] for anything kept long-term.
  *   - '''Writes never mutate the source.''' A write that splices (a `Hit`) returns a slice over a
  *     '''fresh''' array holding only the slice's bytes — `array[offset, start) ++ encoded ++
  *     array[end, until)` — at offset 0; the bytes around the slice are neither copied nor touched.
  *     A write that changes nothing (a `Miss`) hands back the input slice itself.
  *
  * Carrier: [[dev.constructive.eo.data.Affine]]. Shape `Optic[JsonSlice, JsonSlice, A, A, Affine]`:
  *
  *   - `type X = (JsonSlice, (JsonSlice, Int, Int))` — Miss carries the source slice for
  *     pass-through; Hit carries it with the focused span's ABSOLUTE `[start, end)` in its array.
  *   - `to` scans `slice.array` from `slice.offset` to `slice.until` and decodes the span via
  *     `readFromSubArray`. Nothing outside the slice is read, and nothing is copied.
  *   - `from` on a Hit encodes the new focus and splices it in, as above.
  *
  * The whole-array [[JsoniterPrism]] runs the same scan and splice over `(0, length)`, so a slice
  * reads and writes exactly like the same bytes copied out.
  *
  * @group Optics
  */
final class JsoniterInPlacePrism[A] private[jsoniter] (
    private[jsoniter] val steps: List[PathStep],
    focus: InPlaceFocus[A],
) extends Optic[JsonSlice, JsonSlice, A, A, Affine],
      JsoniterPrismPath[A, JsoniterInPlacePrism, JsoniterInPlaceTraversal]:

  type X = (JsonSlice, (JsonSlice, Int, Int))

  // 1-call forwarders, for the same inlining reason as JsoniterPrism's.
  def to(slice: JsonSlice): Affine[X, A] = scan(slice)
  def from(aff: Affine[X, A]): JsonSlice = splice(aff)

  /** Encode `a` standalone, as a slice over an array of its own. Lawful only for the ROOT prism, as
    * [[JsoniterPrism.reverseGet]] is.
    */
  def reverseGet(a: A): JsonSlice = focus.standalone(a)

  /** Re-focus on the '''raw encoded span''', as a [[JsonSlice]]: same path, same scan, but the
    * focus is the focused value's own JSON bytes — a '''view''' of them where they lie in the
    * source array, not a copy (the whole-array [[JsoniterPrism.raw]] copies them out). No typed
    * decode runs. The span is checked to be one well-formed JSON value (jsoniter-scala's `skip`,
    * which allocates nothing), so a malformed one misses, as it does for `JsoniterPrism.raw`.
    *
    * The focus is a slice of the '''source''' array, under the rules above: don't mutate the array
    * while it's in use, and it keeps the whole array reachable (`.toArray` to keep it). It is the
    * source of further in-place optics, through ordinary `andThen`:
    * `JsoniterPrism.inPlace[Issue].field(_.detail).raw.andThen(severityP)`, with
    * `severityP = JsoniterPrism.inPlace[Detail].field(_.severity)`, is one
    * `Optic[JsonSlice, JsonSlice, Severity, Severity, Affine]`, nothing copied on the way down.
    *
    * '''Writing''' through it splices the new focus's bytes '''verbatim''' — no re-encoding and no
    * validation, as `JsoniterPrism.raw` writes — into a fresh array, by the write rule above:
    * neither the source array nor the new focus's own array is changed. A composed write (`(raw
    * andThen severityP).replace(s)`) re-encodes only the leaf: the inner prism splices it into a
    * fresh copy of the focused span, and this one splices that into a fresh copy of the document.
    */
  def raw: JsoniterInPlacePrism[JsonSlice] =
    new JsoniterInPlacePrism[JsonSlice](steps, InPlaceFocus.raw)

  private def scan(slice: JsonSlice): Affine[X, A] =
    val bytes = slice.array
    JsoniterPrism.scanRange[A, Affine[X, A]](bytes, slice.offset, slice.until, steps)(
      (start, end) => focus.read(bytes, start, end),
      new Affine.Miss[X](slice),
      (start, end, a) => new Affine.Hit[X, A]((slice, start, end), a),
    )

  private def splice(aff: Affine[X, A]): JsonSlice =
    aff match
      case h: Affine.Hit[X, A] =>
        val (src, start, end) = h.snd
        val enc = focus.encode(h.b)
        JsonSlice(
          JsoniterPrism.spliceRange(
            src.array,
            src.offset,
            src.until,
            start,
            end,
            enc.array,
            enc.offset,
            enc.length,
          )
        )
      case m: Affine.Miss[X] => m.fst

  // ---- Path building (for the JsoniterPath macros) ----------------

  private[jsoniter] def withSteps[B](steps: List[PathStep])(using
      JsonValueCodec[B]
  ): JsoniterInPlacePrism[B] =
    new JsoniterInPlacePrism[B](steps, InPlaceFocus.typed[B])

  private[jsoniter] def toTraversal[B](using JsonValueCodec[B]): JsoniterInPlaceTraversal[B] =
    new JsoniterInPlaceTraversal[B](steps :+ PathStep.Wildcard)

/** How a [[JsoniterInPlacePrism]] reads its focus out of a span and writes one back: decoded
  * through a codec, or (`.raw`) the span itself, as a view.
  */
sealed abstract private[jsoniter] class InPlaceFocus[A]:

  /** The focus in `bytes[start, end)`. Throws when the span doesn't decode: the scan's one `catch`
    * makes that a miss.
    */
  def read(bytes: Array[Byte], start: Int, end: Int): A

  /** `a`'s JSON, to splice in. May be a view of `a`'s own bytes: the splice copies them. */
  def encode(a: A): JsonSlice

  /** `a`'s JSON, in an array of its own. */
  def standalone(a: A): JsonSlice

private[jsoniter] object InPlaceFocus:

  /** Decode and encode through `codec`. */
  def typed[A](using codec: JsonValueCodec[A]): InPlaceFocus[A] = new InPlaceFocus[A]:
    def read(bytes: Array[Byte], start: Int, end: Int): A =
      readFromSubArray(bytes, start, end)(using codec)
    def encode(a: A): JsonSlice = JsonSlice(writeToArray(a)(using codec))
    def standalone(a: A): JsonSlice = encode(a)

  /** The span itself, once checked to be one well-formed JSON value; written back verbatim. */
  val raw: InPlaceFocus[JsonSlice] = new InPlaceFocus[JsonSlice]:
    def read(bytes: Array[Byte], start: Int, end: Int): JsonSlice =
      readFromSubArray(bytes, start, end)(using skipCodec)
      JsonSlice.unchecked(bytes, start, end - start)
    def encode(a: JsonSlice): JsonSlice = a
    def standalone(a: JsonSlice): JsonSlice = JsonSlice(a.toArray)

  /** Checks that a span holds one JSON value and nothing after it, allocating nothing: skips it. */
  private val skipCodec: JsonValueCodec[Unit] = new JsonValueCodec[Unit]:
    def decodeValue(in: JsonReader, default: Unit): Unit = in.skip()
    def encodeValue(x: Unit, out: JsonWriter): Unit = out.writeNull()
    def nullValue: Unit = ()
