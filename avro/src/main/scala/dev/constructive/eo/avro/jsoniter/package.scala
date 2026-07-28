package dev.constructive.eo.avro

/** Structural Avro ↔ JSON-bytes bridge (`AvroJsoniter`): move between Avro generic runtime values /
  * payload bytes and UTF-8 JSON byte arrays with no typed case class and no JSON AST in the middle,
  * plus the drilled cursor faces — `.json` on `AvroPrism` / `AvroTraversal` and the reverse `.avro`
  * on `JsoniterPrism`. jsoniter-scala-core and `cats-eo-jsoniter` are '''Optional''' dependencies
  * of `cats-eo-avro` — add jsoniter-scala-core for the walks / parses, and `cats-eo-jsoniter` as
  * well for the `.avro` face.
  */
package object jsoniter:

  /** Avro '''binary payload''' bytes — the wire encoding under a writer schema. Same runtime type
    * as [[JsoniterBytes]]; the aliases keep the two `Array[Byte]` roles apart in the bridge's
    * signatures (`bytesPrism[A]: MendTearPrism[AvroBytes, JsoniterBytes, A, A]` reads as Avro-in /
    * JSON-out, where four bare `Array[Byte]`s would not).
    */
  type AvroBytes = Array[Byte]

  /** UTF-8 '''JSON document''' bytes as rendered by jsoniter-scala's `JsonWriter` — the bridge's
    * output side, playing the role `io.circe.Json` plays in `AvroJson`.
    */
  type JsoniterBytes = Array[Byte]
