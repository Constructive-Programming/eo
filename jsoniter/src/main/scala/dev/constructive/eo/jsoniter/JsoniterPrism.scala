package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromSubArray}

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic

/** Read-only optic over a JSON byte buffer. Resolves a JSONPath subset against `Array[Byte]`,
  * decodes the focused slice via `JsonValueCodec[A]` only when the user reads it. No runtime AST.
  *
  * Carrier: [[Affine]]. The shape `Optic[Array[Byte], Array[Byte], A, A, Affine]` is deliberately
  * read-write-shaped even though phase-1 only ships read-only operations:
  *
  *   - `type X = (Array[Byte], (Array[Byte], Int, Int))`
  *     - `Fst[X] = Array[Byte]` — original source bytes (Miss carries this for pass-through)
  *     - `Snd[X] = (Array[Byte], Int, Int)` — bytes + the focused span (Hit carries this; future
  *       phase-2 splice writes use the start/end to memcpy the new encoding back in)
  *   - `to: Array[Byte] => Affine[X, A]` runs the path scanner; on hit, decodes the slice via
  *     `readFromSubArray` and packs `Hit(snd = (bytes, start, end), b = decoded)`. On miss (path
  *     doesn't resolve, decode throws), packs `Miss(fst = bytes)` for pass-through.
  *   - `from: Affine[X, A] => Array[Byte]` is identity in phase 1 — read-only spike. Hit returns
  *     `h.snd._1` (the original bytes); Miss returns `m.fst`. Phase 2 will splice the encoded focus
  *     into the byte buffer at the recorded span.
  *
  * Composability: the standard cats-eo extension methods on `Optic[..., Affine]` light up
  * automatically — `.foldMap`, `.modify` (no-op for read-only since `from` is identity),
  * `.andThen`, etc. No new Composer / AssociativeFunctor needed; reuses the existing Affine
  * machinery.
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
          case h: Affine.Hit[X, A]  => h.snd._1
          case m: Affine.Miss[X, A] => m.fst
