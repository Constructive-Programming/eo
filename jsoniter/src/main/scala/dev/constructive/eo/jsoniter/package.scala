package dev.constructive.eo

/** Byte-level JSON optics over jsoniter-scala: `JsoniterPrism` and `JsoniterTraversal` focus a
  * JSONPath '''directly in the encoded byte buffer''' — scanning for the span, decoding only the
  * focused slice, and splicing writes back — so reads and writes never materialise the full
  * document AST.
  *
  * The typed entry point is `JsoniterPrism[A]` — the root `Prism[Array[Byte], A]`:
  *
  * {{{
  *   val streetP: JsoniterPrism[String] =
  *     JsoniterPrism[Person].field(_.address).field(_.street)
  *   streetP.modify(_.toUpperCase)(personBytes)
  *   // → Array[Byte] with .address.street upper-cased —
  *   //   no Person ever materialised, only the String leaf decoded.
  * }}}
  */
package object jsoniter
