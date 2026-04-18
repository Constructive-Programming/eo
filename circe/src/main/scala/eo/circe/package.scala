package eo

import io.circe.{Decoder, Encoder}

/** Cross-representation optics bridging native Scala types and their circe-serialised form.
  *
  * The entry point is [[JsonPrism.apply]] (aliased as `codecPrism` for the read-aloud API):
  *
  * {{{
  *   val personPrism: JsonPrism[Person] = codecPrism[Person]
  *   val streetPrism: JsonPrism[String] =
  *     personPrism.field(_.address).field(_.street)
  *   streetPrism.modify(_.toUpperCase)(personJson)
  *   // → the same Json, with .address.street upper-cased — no Person
  *   //   ever materialised.
  * }}}
  */
package object circe:

  /** Root-level Prism from Json to a native type `S`. Alias for [[JsonPrism.apply]] that reads more
    * naturally when composed with `.field`.
    */
  def codecPrism[S: Encoder: Decoder]: JsonPrism[S] = JsonPrism[S]
