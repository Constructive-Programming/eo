package dev.constructive.eo
package bench
package fixture

import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}

/** Avro `Encoder`/`Decoder`/`SchemaFor` instances and EO avro optics for the canonical [[Order]]
  * schema, shared by `OrderAvroBench`.
  *
  * `customer.loyaltyId` is intentionally not focused: kindlings encodes `Option` as an Avro union
  * navigated via `.union[Branch]`, not a transparent field, so it isn't a clean cross-backend peer
  * (plan 009, Avro `Option` caveat).
  */
object DomainAvro:

  given AvroEncoder[Address] = AvroEncoder.derived
  given AvroDecoder[Address] = AvroDecoder.derived
  given AvroSchemaFor[Address] = AvroSchemaFor.derived

  given AvroEncoder[Customer] = AvroEncoder.derived
  given AvroDecoder[Customer] = AvroDecoder.derived
  given AvroSchemaFor[Customer] = AvroSchemaFor.derived

  given AvroEncoder[LineItem] = AvroEncoder.derived
  given AvroDecoder[LineItem] = AvroDecoder.derived
  given AvroSchemaFor[LineItem] = AvroSchemaFor.derived

  given AvroEncoder[Order] = AvroEncoder.derived
  given AvroDecoder[Order] = AvroDecoder.derived
  given AvroSchemaFor[Order] = AvroSchemaFor.derived

  /** The whole-document codec the `naive*` / `monocle*` round-trip baselines decode/encode through.
    */
  val codec: AvroCodec[Order] = summon[AvroCodec[Order]]

  /** depth-3 scalar `customer.address.street`. */
  val streetPrism = codecPrism[Order].field(_.customer).field(_.address).field(_.street).record

  /** array write traversal `lines[*].name`. */
  val namesTraversal = codecPrism[Order].lines.each.name.record
