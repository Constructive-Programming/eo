package dev.constructive.eo.docs.avrodocs

// ADTs for site/docs/integrations/avro.md, with their kindlings-derived
// codec triplets. Hosted in compiled sources (own sub-package so the
// names can stay exactly as the page shows them without clashing with
// `docs.*` samples) because mdoc's fence compiler never receives
// `-Xmacro-settings` — fences always run kindlings' 5s default budget,
// which a loaded machine intermittently trips. Compiled sources honour
// the 30s budget and zinc caches them.

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}

final case class Address(street: String, zip: Int)
object Address:
  given AvroEncoder[Address] = AvroEncoder.derived
  given AvroDecoder[Address] = AvroDecoder.derived
  given AvroSchemaFor[Address] = AvroSchemaFor.derived

final case class Person(name: String, age: Int, address: Address)
object Person:
  given AvroEncoder[Person] = AvroEncoder.derived
  given AvroDecoder[Person] = AvroDecoder.derived
  given AvroSchemaFor[Person] = AvroSchemaFor.derived

final case class Order(name: String)
object Order:
  given AvroEncoder[Order] = AvroEncoder.derived
  given AvroDecoder[Order] = AvroDecoder.derived
  given AvroSchemaFor[Order] = AvroSchemaFor.derived

final case class Basket(owner: String, items: List[Order])
object Basket:
  given AvroEncoder[Basket] = AvroEncoder.derived
  given AvroDecoder[Basket] = AvroDecoder.derived
  given AvroSchemaFor[Basket] = AvroSchemaFor.derived

final case class Transaction(id: String, amount: Option[Long])
object Transaction:
  given AvroEncoder[Transaction] = AvroEncoder.derived
  given AvroDecoder[Transaction] = AvroDecoder.derived
  given AvroSchemaFor[Transaction] = AvroSchemaFor.derived

type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
given AvroEncoder[NameAge] = AvroEncoder.derived
given AvroDecoder[NameAge] = AvroDecoder.derived
given AvroSchemaFor[NameAge] = AvroSchemaFor.derived
