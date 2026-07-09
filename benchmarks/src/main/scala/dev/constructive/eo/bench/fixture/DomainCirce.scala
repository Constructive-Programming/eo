package dev.constructive.eo
package bench
package fixture

import cats.data.{Chain, Ior}
import cats.instances.list.given
import dev.constructive.eo.circe.{codecPrism, JsonFailure, JsonPrism}
import dev.constructive.eo.optics.{Lens as EoLens, Traversal as EoTraversal}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.{Codec, Json}

/** circe `Codec.AsObject` instances and EO circe optics for the canonical [[Order]] schema, shared
  * by every circe-flavoured bench (`OrderCirceBench`, `OpticBuildBench`, the eo-circe side of
  * `JsoniterBench`).
  *
  * Centralised here — rather than re-derived in each bench companion — so the codecs and the foci
  * are defined exactly once and every bench measures the *same* optic against the *same* document.
  */
object DomainCirce:

  // ---- codecs (every type on every focus path) ----------------------

  given Codec.AsObject[Address] = KindlingsCodecAsObject.derived
  given Codec.AsObject[Customer] = KindlingsCodecAsObject.derived
  given Codec.AsObject[LineItem] = KindlingsCodecAsObject.derived
  given Codec.AsObject[Order] = KindlingsCodecAsObject.derived

  // ---- EO circe optics ----------------------------------------------

  /** depth-1 scalar `$.id` (`Long`). */
  val idPrism: JsonPrism[Long] = codecPrism[Order].field(_.id)

  /** depth-3 scalar `$.customer.address.street` (`String`). */
  val streetPrism: JsonPrism[String] =
    codecPrism[Order].field(_.customer).field(_.address).field(_.street)

  /** array fold focus `$.lines[*].price` (`Double`) — read side for the cross-EO fold comparison.
    * Built through the core `Traversal.each` (not circe's `Dynamic` `.each`) so it carries a real
    * `foldMap`.
    */
  val pricesFold =
    codecPrism[Order]
      .field(_.lines)
      .andThen(EoTraversal.each[List, LineItem])
      .andThen(EoLens[LineItem, Double](_.price, (li, p) => li.copy(price = p)))

  /** `lines[*].name` write traversal, pre-built into the `Json => …` rewrite so the bench loop
    * measures only the modify.
    */
  val namesTraversal: Json => Json =
    codecPrism[Order].lines.each.name.modifyUnsafe(_.toUpperCase)

  val namesTraversalIor: Json => Ior[Chain[JsonFailure], Json] =
    codecPrism[Order].lines.each.name.modify(_.toUpperCase)
