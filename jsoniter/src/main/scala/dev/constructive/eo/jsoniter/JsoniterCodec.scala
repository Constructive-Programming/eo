package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

/** jsoniter-scala's codec, under eo-jsoniter's name: declaring the codecs a [[JsoniterPrism]] reads
  * through takes one import, next to the prism's own, instead of one from each of jsoniter-scala's
  * `core` and `macros` packages.
  *
  * {{{
  *   import dev.constructive.eo.jsoniter.{JsoniterCodec, JsoniterPrism}
  *
  *   given JsoniterCodec[Person] = JsoniterCodec.derived
  * }}}
  *
  * The same type as `JsonValueCodec`, so codecs declared either way serve each other.
  */
type JsoniterCodec[A] = JsonValueCodec[A]

object JsoniterCodec:

  /** Derive a codec with jsoniter-scala's `JsonCodecMaker.make` and its default configuration.
    * `inline`, so the derivation runs at the call site, which therefore needs jsoniter-scala-macros
    * on its compile classpath (as any jsoniter-scala derivation does). For a non-default
    * `CodecMakerConfig`, call `JsonCodecMaker.make(config)` directly.
    */
  inline def derived[A]: JsoniterCodec[A] = JsonCodecMaker.make[A]
