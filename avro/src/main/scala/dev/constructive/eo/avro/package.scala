package dev.constructive.eo

import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import vulcan.Codec

/** Cross-representation optics bridging native Scala types and their Apache Avro on-the-wire form.
  *
  * The entry point is [[AvroPrism.codecPrism]] (read-aloud API parallel to
  * [[dev.constructive.eo.circe.codecPrism]]):
  *
  * {{{
  *   import vulcan.Codec
  *
  *   case class Person(name: String, age: Int)
  *   given Codec[Person] = ???   // user-supplied vulcan codec
  *
  *   val personPrism: AvroPrism[Person]   = codecPrism[Person]
  *   val namePrism:   AvroPrism[String]   = personPrism.field(_.name)
  *   namePrism.modify(_.toUpperCase)(record)
  *   // → the same IndexedRecord, with `name` upper-cased — no Person
  *   //   ever materialised at intermediate steps.
  * }}}
  *
  * Mirrors `dev.constructive.eo.circe`'s architecture decisions on `Json` for the Avro carrier:
  * `IndexedRecord` plays the role of `Json`, `vulcan.Codec[A]` plays the role of
  * `(io.circe.Encoder[A], io.circe.Decoder[A])`, and `AvroFailure` plays the role of `JsonFailure`.
  *
  * '''Gap-1 (per the eo-avro plan).''' [[PathStep]] is duplicated, not shared with eo-circe — the
  * `UnionBranch` case is Avro-only and forcing it into eo-circe would pollute that module. The
  * duplication is intentional; see [[PathStep]]'s class doc.
  */
package object avro:

  /** Type alias for the Avro carrier — the underlying `org.apache.avro.generic.IndexedRecord`
    * interface, exposing positional `get(i)` / `put(i, v)` accessors that map cleanly to a
    * path-step navigation. The optic surface threads `Avro` everywhere `JsonPrism` would have
    * threaded `Json`.
    */
  type Avro = IndexedRecord

  /** Root-level Prism from Avro to a native type `S`. Reads `S`'s schema off the in-scope
    * `Codec[S]` (per OQ-avro-5). Alias for [[AvroPrism.codecPrism]] that reads more naturally when
    * composed with `.field`.
    */
  def codecPrism[S](using codec: Codec[S]): AvroPrism[S] = AvroPrism.codecPrism[S]

  /** Root-level Prism from Avro to a native type `S`, with an explicit reader schema. Use when the
    * schema is loaded at runtime from an `.avsc` file or a Schema Registry rather than derived from
    * the codec.
    */
  def codecPrism[S](schema: Schema)(using codec: Codec[S]): AvroPrism[S] =
    AvroPrism.codecPrism[S](schema)

end avro
