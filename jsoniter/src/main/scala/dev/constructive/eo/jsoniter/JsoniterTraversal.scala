package dev.constructive.eo.jsoniter

import scala.annotation.tailrec
import scala.language.dynamics

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromSubArray, writeToArray, JsonValueCodec}
import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.optics.Traversal

/** Read-WRITE Traversal over JSON byte buffers. Resolves a wildcard-bearing JSONPath against
  * `Array[Byte]`, decodes each matched span via `JsonValueCodec[A]` on read, and encodes-and-
  * splices every focus back in one pass on write. No runtime AST.
  *
  * Carrier: `MultiFocus[PSVec]` — the same carrier `Traversal.each` uses, peer to all the classical
  * Traversal idioms. The choice over `MultiFocus[List]` matters for downstream composition:
  * `JsoniterTraversal[A].andThen(Traversal.each[List, A])` and similar same-carrier chains work via
  * the existing `mfAssocPSVec` `AssociativeFunctor` instance, with zero-copy per-element
  * reassembly. A List-carrier traversal would force a Composer hop or a manual `.modify` rebuild.
  *
  *   - `type X = (Array[Byte], List[JsonPathScanner.Span])` — original bytes + each KEPT focus's
  *     span, 1:1 aligned with the foci (spans whose decode throws are dropped TOGETHER with their
  *     focus, so the write side can trust the pairing).
  *   - `to: Array[Byte] => (X, PSVec[A])` — runs [[JsonPathScanner.findAll]], decodes every span
  *     via the codec into a single `Array[Any]` allocation, wraps it as a PSVec. Spans whose decode
  *     throws are silently dropped (matches the read semantic "fold the focuses that exist") — and
  *     stay byte-untouched on write.
  *   - `from: ((X, PSVec[A])) => Array[Byte]` — encodes each focus via `writeToArray` and splices
  *     all spans back in a single pass (segments between spans are `arraycopy`d verbatim). A
  *     span↔foci length mismatch (an aggregate `from` after a shape-changing carrier op) passes the
  *     original bytes through unchanged, as does an empty focus set.
  *
  * Composability: standard cats-eo extensions on `Optic[..., MultiFocus[PSVec]]` light up
  * automatically via `mfFold[PSVec]`, `mfFunctor[PSVec]`, `mfAssocPSVec`, etc. — `.foldMap`,
  * `.modify`, `.headOption`, `.length`, `.exists`, `.modifyA`, same-carrier `.andThen` all work
  * without anything new shipping in this module. Mirrors `dev.constructive.eo.avro.AvroTraversal`'s
  * byte-carried write semantics. `JsoniterPrism`'s '''Laws & preconditions''' apply verbatim: laws
  * hold up to canonical re-encoding of the focused slices, and writes need decodable current
  * focuses.
  *
  * '''Write-side failure is per-element and silent''': a focus whose ENCODE throws keeps its
  * ORIGINAL bytes while its siblings are written, so a partially applied write is reported as
  * success (`from` is total by type). Pinned by `JsoniterTraversalWriteSpec`; the carrier-level fix
  * is `docs/research/2026-09-22-exception-audit.md` (class C), tracked as issue #117.
  *
  * A document inside a larger buffer is read in place through the slice face,
  * [[JsoniterTraversal.onSlice]]: the same traversal over a [[JsonSlice]], nothing copied. This
  * traversal's whole-array splice is the `(0, length)` case of the slice face's.
  *
  * Typed drilling continues past the wildcard, mirroring eo-circe's `JsonTraversal`:
  * `JsoniterPrism[Basket].field(_.items).each.field(_.price)` focuses `price` of every element.
  * Each step is compile-time checked against the case-class schema and appends to the path.
  *
  * @group Optics
  */
final class JsoniterTraversal[A] private[jsoniter] (
    private[jsoniter] val steps: List[PathStep]
)(using codec: JsonValueCodec[A])
    extends Traversal[Array[Byte], Array[Byte], A, A],
      Dynamic:

  type X = (Array[Byte], List[JsonPathScanner.Span])

  def to(bytes: Array[Byte]): MultiFocus[PSVec][X, A] =
    val spans = JsonPathScanner.findAll(bytes, steps)
    if spans.isEmpty then MultiFocus((bytes, spans), PSVec.empty[A])
    else
      val (keptSpans, psv) = JsoniterTraversal.decodeSpans[A](bytes, spans)
      MultiFocus((bytes, keptSpans), psv)

  def from(pair: MultiFocus[PSVec][X, A]): Array[Byte] =
    val (bytes, spans) = pair.context
    val foci = pair.foci
    if spans.isEmpty || spans.length != foci.length then bytes
    else
      // The whole array is the `(0, length)` case of the ranged splice the slice face uses.
      JsoniterTraversal.spliceAll(bytes, 0, bytes.length, JsoniterTraversal.encodeAll(spans, foci))

  /** Re-source this traversal on a [[JsonSlice]]: same path, same foci, but the JSON document is a
    * range of a larger byte array, read in place. Same write rule as [[JsoniterPrism.onSlice]]: a
    * write that splices returns a slice over a FRESH array holding only the slice's bytes (offset
    * 0); one that changes nothing (no foci) hands back the input slice itself. See
    * [[JsoniterSliceTraversal]].
    */
  def onSlice: JsoniterSliceTraversal[A] = new JsoniterSliceTraversal[A](steps)

  /** Dynamic field sugar — `itemsT.price` lowers to `itemsT.field(_.price)`. */
  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsoniterPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Path widening (used by the macro extensions) ----------------

  /** Extend the path by a field step. Used by [[JsoniterTraversal.field]] / `selectDynamic`. */
  private[jsoniter] def widenField[B](name: String)(using
      JsonValueCodec[B]
  ): JsoniterTraversal[B] =
    new JsoniterTraversal[B](steps :+ PathStep.Field(name))

  /** Extend by an array-index step. Used by [[JsoniterTraversal.at]]. */
  private[jsoniter] def widenIndex[B](i: Int)(using JsonValueCodec[B]): JsoniterTraversal[B] =
    new JsoniterTraversal[B](steps :+ PathStep.Index(i))

object JsoniterTraversal:

  /** Build a read-write Traversal over a JSON byte buffer at the given JSONPath — the ONLY
    * string-path constructor, and it keeps the failure in the type. The path MAY contain `[*]`
    * wildcard steps (without them, this collapses to a single-or-zero-focus Traversal that still
    * uses the MultiFocus[PSVec] carrier — fine, just narrower). An unparseable path comes back as
    * `Left(message)`: a path is DATA, so there is no throwing twin to reach for.
    *
    * @example
    *   {{{
    * import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    * given codec: JsonValueCodec[Long] = JsonCodecMaker.make
    *
    * val itemsT: Either[String, JsoniterTraversal[Long]] =
    *   JsoniterTraversal.fromPath[Long]("$.cart.items[*]")
    * val bytes: Array[Byte] = ...
    * itemsT.map(_.foldMap(identity[Long])(bytes))  // Either[String, Long], zero AST allocation
    * itemsT.map(_.modify(_ * 10)(bytes))           // Either[String, Array[Byte]], spliced in place
    *   }}}
    *
    * @group Constructors
    */
  def fromPath[A](path: String)(using
      codec: JsonValueCodec[A]
  ): Either[String, JsoniterTraversal[A]] =
    PathParser
      .parse(path)
      .map(steps => fromSteps[A](steps))
      .left
      .map(e => s"invalid JSONPath '$path': $e")

  /** Build a Traversal from an already-parsed step list.
    *
    * @group Optics
    */
  private[jsoniter] def fromSteps[A](steps: List[PathStep])(using
      codec: JsonValueCodec[A]
  ): JsoniterTraversal[A] =
    new JsoniterTraversal[A](steps)

  /** `.field(_.x)` — drill every focus into a field, compile-time checked against `A`. */
  extension [A](o: JsoniterTraversal[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: JsonValueCodec[B]): JsoniterTraversal[B] =
      ${ JsoniterPrismMacro.fieldTraversalImpl[A, B]('o, 'selector, 'codecB) }

  /** `.at(i)` — drill every focus into the i-th element of a nested array. */
  extension [A](o: JsoniterTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ JsoniterPrismMacro.atTraversalImpl[A]('o, 'i) }

  /** Encode each focus, paired with its span; an element whose encode throws is left out, so it
    * keeps its original bytes (see the class doc). `spans` and `foci` are 1:1.
    */
  private[jsoniter] def encodeAll[A](
      spans: List[JsonPathScanner.Span],
      foci: PSVec[A],
  )(using codec: JsonValueCodec[A]): List[(JsonPathScanner.Span, Array[Byte])] =
    val reps = List.newBuilder[(JsonPathScanner.Span, Array[Byte])]
    @tailrec def loop(rest: List[JsonPathScanner.Span], i: Int): Unit =
      rest match
        case span :: tail =>
          try reps += ((span, writeToArray(foci(i))(using codec)))
          catch case scala.util.control.NonFatal(_) => ()
          loop(tail, i + 1)
        case Nil => ()
    loop(spans, 0)
    reps.result()

  /** Decode every span via the codec into a single `Array[Any]`, tight-packing only on partial
    * decode failure. A span whose decode throws is dropped together with its slot so spans↔foci
    * stay 1:1 for the write side. Returns the kept spans paired with the packed foci. `spans` must
    * be non-empty.
    */
  private[jsoniter] def decodeSpans[A](
      bytes: Array[Byte],
      spans: List[JsonPathScanner.Span],
  )(using codec: JsonValueCodec[A]): (List[JsonPathScanner.Span], PSVec[A]) =
    val n = spans.length
    val arr = new Array[Any](n)
    val keptSpans = List.newBuilder[JsonPathScanner.Span]
    val it = spans.iterator
    @tailrec def loop(written: Int): Int =
      if it.hasNext then
        val span = it.next()
        val next =
          try
            arr(written) = readFromSubArray[A](bytes, span.start, span.end)
            keptSpans += span
            written + 1
          catch case scala.util.control.NonFatal(_) => written
        loop(next)
      else written
    val written = loop(0)
    val psv: PSVec[A] =
      if written == n then PSVec.unsafeWrap[A](arr)
      else if written == 0 then PSVec.empty[A]
      else
        val tight = new Array[Any](written)
        System.arraycopy(arr, 0, tight, 0, written)
        PSVec.unsafeWrap[A](tight)
    (keptSpans.result(), psv)

  /** One-pass multi-span splice of the document in `bytes[from, until)` into a fresh array:
    * `replacements` are (span, encodedValue) pairs sorted by span start (document order, guaranteed
    * by [[JsonPathScanner.findAll]]), non-overlapping, and inside the range.
    */
  private[jsoniter] def spliceAll(
      bytes: Array[Byte],
      from: Int,
      until: Int,
      replacements: List[(JsonPathScanner.Span, Array[Byte])],
  ): Array[Byte] =
    var total = until - from
    replacements.foreach { (span, enc) =>
      total += enc.length - (span.end - span.start)
    }
    val out = new Array[Byte](total)
    var src = from
    var dst = 0
    replacements.foreach { (span, enc) =>
      val pre = span.start - src
      System.arraycopy(bytes, src, out, dst, pre)
      dst += pre
      System.arraycopy(enc, 0, out, dst, enc.length)
      dst += enc.length
      src = span.end
    }
    System.arraycopy(bytes, src, out, dst, until - src)
    out

end JsoniterTraversal

/** A [[JsoniterTraversal]] whose JSON document is a [[JsonSlice]] — a range of a larger byte array,
  * read in place. Obtained from [[JsoniterTraversal.onSlice]] (drill first, then take the slice
  * face); same path, same codec, same laws and write-side rules as the traversal it came from, with
  * slices compared by content.
  *
  *   - `type X = (JsonSlice, List[JsonPathScanner.Span])` — the source slice + each kept focus's
  *     ABSOLUTE span in its array.
  *   - `to` scans `slice.array` from `slice.offset` to `slice.until` and decodes every span in
  *     place. Nothing outside the slice is read, and nothing is copied.
  *   - `from` splices every focus into a FRESH array holding only the slice's bytes, returned as a
  *     slice at offset 0; the source array is never written. No foci (or a span/foci mismatch)
  *     returns the input slice itself.
  *
  * @group Optics
  */
final class JsoniterSliceTraversal[A] private[jsoniter] (
    private[jsoniter] val steps: List[PathStep]
)(using codec: JsonValueCodec[A])
    extends Traversal[JsonSlice, JsonSlice, A, A]:

  type X = (JsonSlice, List[JsonPathScanner.Span])

  def to(slice: JsonSlice): MultiFocus[PSVec][X, A] =
    val spans = JsonPathScanner.findAll(slice.array, slice.offset, slice.until, steps)
    if spans.isEmpty then MultiFocus((slice, spans), PSVec.empty[A])
    else
      val (keptSpans, psv) = JsoniterTraversal.decodeSpans[A](slice.array, spans)
      MultiFocus((slice, keptSpans), psv)

  def from(pair: MultiFocus[PSVec][X, A]): JsonSlice =
    val (slice, spans) = pair.context
    val foci = pair.foci
    if spans.isEmpty || spans.length != foci.length then slice
    else
      val reps = JsoniterTraversal.encodeAll(spans, foci)
      JsonSlice(JsoniterTraversal.spliceAll(slice.array, slice.offset, slice.until, reps))
