package dev.constructive.eo.circe

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec

/** Shared ADT fixtures + Circe codecs for the circe-test specs.
  *
  * '''2026-04-26 dedup.''' Six spec files were each carrying identical companion-object copies of
  * `Address` / `Person` / `Order` / `Basket` (and the `KindlingsCodecAsObject.derive` `given` for
  * each). They now `import JsonSpecFixtures.*` for a single source of truth.
  *
  * The macro-summon path used by [[JsonPrism.field]] / [[JsonPrism.fields]] resolves `Encoder` /
  * `Decoder` from the enclosing scope at the call site, so a wildcard import of this object brings
  * the codecs into scope without further ceremony.
  *
  * See [[JsonSpecBase]] for the per-spec import-deduplication trait that re-exports the rest of
  * the boilerplate (`cats.data.{Chain, Ior}`, `io.circe.{Codec, Json}` etc.) at member level.
  */
object JsonSpecFixtures:

  case class Address(street: String, zip: Int)

  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derive

  case class Person(name: String, age: Int, address: Address)

  object Person:
    given Codec.AsObject[Person] = KindlingsCodecAsObject.derive

  case class Order(name: String)

  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

  case class Basket(owner: String, items: Vector[Order])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive
