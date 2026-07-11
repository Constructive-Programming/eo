package dev.constructive.eo.avro

/** Structural Avro ↔ circe bridge (`AvroJson`): move between Avro generic runtime values, payload
  * bytes and `io.circe.Json` with no typed case class in the middle. circe is an '''Optional'''
  * dependency of `cats-eo-avro` — add `circe-core` to use this package.
  */
package object circe
