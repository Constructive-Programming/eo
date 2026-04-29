package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromSubArray, writeToArray}

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic

/** Read-write optic over a JSON byte buffer. Resolves a JSONPath subset against `Array[Byte]`,
  * decodes the focused slice via `JsonValueCodec[A]` on read, encodes-and-splices on write. No
  * runtime AST.
  *
  * Carrier: [[Affine]]. Shape `Optic[Array[Byte], Array[Byte], A, A, Affine]`:
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
  * @group Optics
  */
object JsoniterPrism:

  /** Build a read-only Prism over a JSON byte buffer at the given JSONPath.
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
    * val idP = JsoniterPrism[Long]("\$.payload.user.id")
    * val bytes: Array[Byte] = ...
    * idP.foldMap(identity)(bytes)  // Long, no AST allocation
    *   }}}
    *
    * @group Optics
    */
  def apply[A](path: String)(using
      codec: JsonValueCodec[A]
  ): Optic[Array[Byte], Array[Byte], A, A, Affine] =
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
  ): Optic[Array[Byte], Array[Byte], A, A, Affine] =
    new Optic[Array[Byte], Array[Byte], A, A, Affine]:
      type X = (Array[Byte], (Array[Byte], Int, Int))

      val to: Array[Byte] => Affine[X, A] = bytes =>
        val span = JsonPathScanner.find(bytes, steps)
        if !span.isHit then new Affine.Miss[X, A](bytes)
        else
          try
            val a = readFromSubArray(bytes, span.start, span.end)(using codec)
            new Affine.Hit[X, A]((bytes, span.start, span.end), a)
          catch case _: Throwable => new Affine.Miss[X, A](bytes)

      val from: Affine[X, A] => Array[Byte] = aff =>
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
