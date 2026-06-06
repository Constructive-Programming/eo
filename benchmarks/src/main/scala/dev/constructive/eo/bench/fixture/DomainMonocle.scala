package dev.constructive.eo
package bench
package fixture

import cats.instances.list.given

import monocle.{Lens => MLens, Traversal => MTraversal}

/** Monocle optics over the decoded canonical [[Order]], shared by every integration bench's
  * `monocle*` baseline (circe / jsoniter / avro).
  *
  * Monocle has no byte/AST carrier, so the `monocle*` rows always pay a full decode → modify →
  * encode round-trip; these optics do only the in-memory modify in the middle. Centralised here so
  * all three backends compare against the *same* Monocle definitions.
  */
object DomainMonocle:

  /** Depth-3 product path `customer.address.street`. */
  val street: MLens[Order, String] =
    MLens[Order, String](_.customer.address.street) { s => o =>
      o.copy(customer = o.customer.copy(address = o.customer.address.copy(street = s)))
    }

  /** Array traversal `lines[*].name`. */
  val names: MTraversal[Order, String] =
    MLens[Order, List[LineItem]](_.lines)(ls => o => o.copy(lines = ls))
      .andThen(MTraversal.fromTraverse[List, LineItem])
      .andThen(MLens[LineItem, String](_.name)(s => li => li.copy(name = s)))

  /** Array traversal `lines[*].price` — the Fold focus. */
  val prices: MTraversal[Order, Double] =
    MLens[Order, List[LineItem]](_.lines)(ls => o => o.copy(lines = ls))
      .andThen(MTraversal.fromTraverse[List, LineItem])
      .andThen(MLens[LineItem, Double](_.price)(p => li => li.copy(price = p)))
