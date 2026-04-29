package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromSubArray}

import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.optics.Optic

/** Read-only Traversal over JSON byte buffers. Resolves a wildcard-bearing JSONPath against
  * `Array[Byte]`, decodes each matched span via `JsonValueCodec[A]` lazily during fold/traverse.
  *
  * Carrier: `MultiFocus[PSVec]` — the same carrier `Traversal.each` uses, peer to all the classical
  * Traversal idioms. The choice over `MultiFocus[List]` matters for downstream composition:
  * `JsoniterTraversal[A].andThen(Traversal.each[List, A])` and similar same-carrier chains work via
  * the existing `mfAssocPSVec` `AssociativeFunctor` instance, with zero-copy per-element
  * reassembly. A List-carrier traversal would force a Composer hop or a manual `.modify` rebuild.
  *
  *   - `type X = (Array[Byte], List[JsonPathScanner.Span])` — original bytes + each focus's span.
  *     The `List[Span]` is the future phase-2 write-back leftover; PSVec is the carrier for the
  *     focused values.
  *   - `to: Array[Byte] => (X, PSVec[A])` — runs [[JsonPathScanner.findAll]], decodes every span
  *     via the codec into a single `Array[AnyRef]` allocation, wraps it as a PSVec. Spans whose
  *     decode throws are silently dropped (matches the read semantic "fold the focuses that
  *     exist").
  *   - `from: ((X, PSVec[A])) => Array[Byte]` — phase-1.5 identity (returns the original bytes).
  *
  * Composability: standard cats-eo extensions on `Optic[..., MultiFocus[PSVec]]` light up
  * automatically via `mfFold[PSVec]`, `mfFunctor[PSVec]`, `mfAssocPSVec`, etc. — `.foldMap`,
  * `.headOption`, `.length`, `.exists`, `.modifyA`, same-carrier `.andThen` all work without
  * anything new shipping in this module.
  *
  * @group Optics
  */
object JsoniterTraversal:

  /** Build a read-only Traversal over a JSON byte buffer at the given JSONPath. The path MAY
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

      val to: Array[Byte] => (X, PSVec[A]) = bytes =>
        val spans = JsonPathScanner.findAll(bytes, steps)
        val n = spans.length
        if n == 0 then ((bytes, spans), PSVec.empty[A])
        else
          // Single Array[AnyRef] allocation; tight-pack only on partial decode failure.
          val arr = new Array[AnyRef](n)
          var written = 0
          val it = spans.iterator
          while it.hasNext do
            val span = it.next()
            try
              arr(written) = readFromSubArray[A](bytes, span.start, span.end).asInstanceOf[AnyRef]
              written += 1
            catch case _: Throwable => ()
          val psv: PSVec[A] =
            if written == n then PSVec.unsafeWrap[A](arr)
            else if written == 0 then PSVec.empty[A]
            else
              val tight = new Array[AnyRef](written)
              System.arraycopy(arr, 0, tight, 0, written)
              PSVec.unsafeWrap[A](tight)
          ((bytes, spans), psv)

      val from: ((X, PSVec[A])) => Array[Byte] = pair => pair._1._1
