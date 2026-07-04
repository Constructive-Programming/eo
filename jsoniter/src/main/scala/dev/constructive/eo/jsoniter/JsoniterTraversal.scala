package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromSubArray, writeToArray, JsonValueCodec}
import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.optics.Optic

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
  *     via the codec into a single `Array[AnyRef]` allocation, wraps it as a PSVec. Spans whose
  *     decode throws are silently dropped (matches the read semantic "fold the focuses that exist")
  *     — and stay byte-untouched on write.
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
  * @group Optics
  */
object JsoniterTraversal:

  /** Build a read-write Traversal over a JSON byte buffer at the given JSONPath. The path MAY
    * contain `[*]` wildcard steps (without them, this collapses to a single-or-zero-focus Traversal
    * that still uses the MultiFocus[PSVec] carrier — fine, just narrower).
    *
    * Throws `IllegalArgumentException` if the path string is not parseable.
    *
    * @example
    *   {{{
    * import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
    * given codec: JsonValueCodec[Long] = JsonCodecMaker.make
    *
    * val itemsT = JsoniterTraversal[Long]("\$.cart.items[*]")
    * val bytes: Array[Byte] = ...
    * itemsT.foldMap(identity[Long])(bytes)  // Long — sum of all items, zero AST allocation
    * itemsT.modify(_ * 10)(bytes)           // Array[Byte] — every item spliced in place
    *   }}}
    *
    * @group Optics
    */
  def apply[A](path: String)(using
      codec: JsonValueCodec[A]
  ): Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]] =
    val steps = PathParser.parse(path) match
      case Right(s) => s
      case Left(e)  => throw new IllegalArgumentException(s"invalid JSONPath '$path': $e")
    fromSteps(steps)

  /** Build a Traversal from an already-parsed step list.
    *
    * @group Optics
    */
  def fromSteps[A](steps: List[PathStep])(using
      codec: JsonValueCodec[A]
  ): Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]] =
    new Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]]:
      type X = (Array[Byte], List[JsonPathScanner.Span])

      def to(bytes: Array[Byte]): MultiFocus[PSVec][X, A] =
        val spans = JsonPathScanner.findAll(bytes, steps)
        if spans.isEmpty then MultiFocus((bytes, spans), PSVec.empty[A])
        else
          val (keptSpans, psv) = decodeSpans[A](bytes, spans)
          MultiFocus((bytes, keptSpans), psv)

      def from(pair: MultiFocus[PSVec][X, A]): Array[Byte] =
        val (bytes, spans) = pair.context
        val foci = pair.foci
        if spans.isEmpty || spans.length != foci.length then bytes
        else
          // Encode each focus; an element whose encode throws keeps its original bytes.
          val reps = List.newBuilder[(JsonPathScanner.Span, Array[Byte])]
          var i = 0
          spans.foreach { span =>
            try reps += ((span, writeToArray(foci(i))(using codec)))
            catch case scala.util.control.NonFatal(_) => ()
            i += 1
          }
          spliceAll(bytes, reps.result())

  /** Decode every span via the codec into a single `Array[AnyRef]`, tight-packing only on partial
    * decode failure. A span whose decode throws is dropped together with its slot so spans↔foci
    * stay 1:1 for the write side. Returns the kept spans paired with the packed foci. `spans` must
    * be non-empty.
    */
  private def decodeSpans[A](
      bytes: Array[Byte],
      spans: List[JsonPathScanner.Span],
  )(using codec: JsonValueCodec[A]): (List[JsonPathScanner.Span], PSVec[A]) =
    val n = spans.length
    val arr = new Array[AnyRef](n)
    val keptSpans = List.newBuilder[JsonPathScanner.Span]
    val it = spans.iterator
    @tailrec def loop(written: Int): Int =
      if it.hasNext then
        val span = it.next()
        val next =
          try
            arr(written) = readFromSubArray[A](bytes, span.start, span.end).asInstanceOf[AnyRef]
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
        val tight = new Array[AnyRef](written)
        System.arraycopy(arr, 0, tight, 0, written)
        PSVec.unsafeWrap[A](tight)
    (keptSpans.result(), psv)

  /** One-pass multi-span splice: `replacements` are (span, encodedValue) pairs sorted by span start
    * (document order, guaranteed by [[JsonPathScanner.findAll]]) and non-overlapping.
    */
  private def spliceAll(
      bytes: Array[Byte],
      replacements: List[(JsonPathScanner.Span, Array[Byte])],
  ): Array[Byte] =
    var total = bytes.length
    replacements.foreach { (span, enc) =>
      total += enc.length - (span.end - span.start)
    }
    val out = new Array[Byte](total)
    var src = 0
    var dst = 0
    replacements.foreach { (span, enc) =>
      val pre = span.start - src
      System.arraycopy(bytes, src, out, dst, pre)
      dst += pre
      System.arraycopy(enc, 0, out, dst, enc.length)
      dst += enc.length
      src = span.end
    }
    System.arraycopy(bytes, src, out, dst, bytes.length - src)
    out

end JsoniterTraversal
