package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromSubArray}

import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.optics.Optic

/** Read-only Traversal over JSON byte buffers. Resolves a wildcard-bearing JSONPath against
  * `Array[Byte]`, decodes each matched span via `JsonValueCodec[A]` lazily during fold/traverse.
  *
  * Carrier: `MultiFocus[List]`. The carrier shape `Optic[Array[Byte], Array[Byte], A, A,
  * MultiFocus[List]]` is read-write-shaped (`T = Array[Byte]` not `Unit`), but phase-1.5 ships
  * read-only — `from` is identity, returning the original bytes. Phase-2 splice writes drop in
  * by changing `from` to encode each new focus and re-emit the byte buffer with all spans
  * replaced; the existential is shaped to carry the spans for that purpose.
  *
  *   - `type X = (Array[Byte], List[JsonPathScanner.Span])` — original bytes + each focus's span.
  *   - `to: Array[Byte] => (X, List[A])` — runs [[JsonPathScanner.findAll]], decodes every span
  *     via the codec; spans whose decode throws are silently dropped (matches the read semantic
  *     "fold the focuses that exist").
  *   - `from: ((X, List[A])) => Array[Byte]` — phase-1.5 identity (returns the original bytes).
  *
  * Composability: standard cats-eo extensions on `Optic[..., MultiFocus[List]]` light up
  * automatically via `mfFold[List]`, `mfFunctor[List]`, etc. — `.foldMap`, `.headOption`,
  * `.length`, `.exists`, `.modifyA` all work without anything new shipping in this module.
  *
  * @group Optics
  */
object JsoniterTraversal:

  /** Build a read-only Traversal over a JSON byte buffer at the given JSONPath. The path MAY
    * contain `[*]` wildcard steps (without them, this collapses to a single-or-zero-focus
    * Traversal that still uses the MultiFocus[List] carrier — fine, just narrower).
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
  ): Optic[Array[Byte], Array[Byte], A, A, MultiFocus[List]] =
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
  ): Optic[Array[Byte], Array[Byte], A, A, MultiFocus[List]] =
    new Optic[Array[Byte], Array[Byte], A, A, MultiFocus[List]]:
      type X = (Array[Byte], List[JsonPathScanner.Span])

      val to: Array[Byte] => (X, List[A]) = bytes =>
        val spans = JsonPathScanner.findAll(bytes, steps)
        val decoded = spans.flatMap { span =>
          try Some(readFromSubArray[A](bytes, span.start, span.end))
          catch case _: Throwable => None
        }
        ((bytes, spans), decoded)

      val from: ((X, List[A])) => Array[Byte] = pair => pair._1._1
