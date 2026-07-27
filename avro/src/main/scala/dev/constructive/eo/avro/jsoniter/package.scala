package dev.constructive.eo.avro

/** Structural Avro → JSON-bytes bridge (`AvroJsoniter`): render Avro generic runtime values and
  * payload bytes straight to UTF-8 JSON byte arrays with no typed case class and no JSON AST in the
  * middle. jsoniter-scala-core is an '''Optional''' dependency of `cats-eo-avro` — add
  * `jsoniter-scala-core` to use this package.
  */
package object jsoniter
