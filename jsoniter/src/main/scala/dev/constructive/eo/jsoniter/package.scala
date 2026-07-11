package dev.constructive.eo

/** Byte-level JSON optics over jsoniter-scala: `JsoniterPrism` and `JsoniterTraversal` focus a
  * JSONPath '''directly in the encoded byte buffer''' — scanning for the span, decoding only the
  * focused slice, and splicing writes back — so reads and writes never materialise the full
  * document AST.
  */
package object jsoniter
