package dev.constructive.eo.docs.circedocs

// ADTs for site/docs/integrations/circe.md, with their kindlings-derived
// codecs — same hosting rationale as `avrodocs`: mdoc fences never
// receive `-Xmacro-settings`, so heavy derivation macros run here where
// the raised budget applies and zinc caches the result.

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec

final case class Address(street: String, zip: Int)
object Address:
  given Codec.AsObject[Address] = KindlingsCodecAsObject.derived

final case class Person(name: String, age: Int, address: Address)
object Person:
  given Codec.AsObject[Person] = KindlingsCodecAsObject.derived

final case class Order(name: String)
object Order:
  given Codec.AsObject[Order] = KindlingsCodecAsObject.derived

final case class Basket(owner: String, items: Vector[Order])
object Basket:
  given Codec.AsObject[Basket] = KindlingsCodecAsObject.derived
