package dev.constructive.eo.avro

/** Structural Avro ↔ JSON-bytes bridge (`AvroJsoniter`): move between Avro generic runtime values /
  * payload bytes and UTF-8 JSON byte arrays with no typed case class and no JSON AST in the middle,
  * plus the drilled cursor faces — `.json` on `AvroPrism` / `AvroTraversal` and the reverse `.avro`
  * on `JsoniterPrism`. jsoniter-scala-core and `cats-eo-jsoniter` are '''Optional''' dependencies
  * of `cats-eo-avro` — add jsoniter-scala-core for the walks / parses, and `cats-eo-jsoniter` as
  * well for the `.avro` face.
  */
package object jsoniter:

  /** Avro '''binary payload''' bytes — canonical definition hoisted to the
    * [[dev.constructive.eo.avro]] package object (both bridges use it); re-aliased here so this
    * package's signatures and `import …avro.jsoniter.*` users resolve it unchanged.
    */
  type AvroBytes = dev.constructive.eo.avro.AvroBytes

  /** UTF-8 '''JSON document''' bytes as rendered by jsoniter-scala's `JsonWriter` — the bridge's
    * output side, playing the role `io.circe.Json` plays in `AvroJson`.
    */
  type JsoniterBytes = Array[Byte]
