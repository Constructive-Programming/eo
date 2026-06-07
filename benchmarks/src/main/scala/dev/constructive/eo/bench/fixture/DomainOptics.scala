package dev.constructive.eo
package bench
package fixture

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.{
  AffineFold,
  Getter => EoGetter,
  Optional => EoOptional,
  Setter => EoSetter,
}

import monocle.{Getter => MGetter, Optional => MOptional, Setter => MSetter}

/** Canonical in-memory EO + Monocle optics over the decoded [[Order]], for the per-family
  * micro-benches that don't go through a byte/AST backend (`GetterBench`, `SetterBench`,
  * `OptionalBench`, `AffineFoldBench`).
  *
  * These cover the two canonical foci the integration benches couldn't reach in their own carrier:
  *   - `order.id: Long` — the advertised Getter / Setter scalar (`$.id`).
  *   - `customer.loyaltyId: Option[String]` — the advertised Optional / AffineFold focus. Avro
  *     omits it (kindlings encodes `Option` as a union, not a transparent field — plan 009 caveat),
  *     but in memory it is exactly an `Optional`/`AffineFold`, so it lives here, not on a synthetic
  *     record.
  *
  * The depth-0/3/6 *composition* sweep stays on `Nested` (see [[NestedOptics]]) — `Order`'s fixed
  * depth-3 shape can't express it.
  */
object DomainOptics:

  /** Populated order (`loyaltyId = Some(...)`) — the Optional / AffineFold Some-branch does work.
    */
  val order: Order = Domain.mkOrder(8)

  /** Same order with `loyaltyId = None` — the miss branch. */
  val orderNoLoyalty: Order =
    order.copy(customer = order.customer.copy(loyaltyId = None))

  // ---- Getter on order.id -------------------------------------------

  val eoGetId = EoGetter[Order, Long](_.id)
  val mGetId: MGetter[Order, Long] = MGetter[Order, Long](_.id)

  // ---- Setter on order.id -------------------------------------------

  val eoSetId = EoSetter[Order, Order, Long, Long](f => o => o.copy(id = f(o.id)))
  val mSetId: MSetter[Order, Long] = MSetter[Order, Long](f => o => o.copy(id = f(o.id)))

  // ---- Optional / AffineFold on customer.loyaltyId ------------------

  val eoLoyalty =
    EoOptional[Order, Order, String, String, Affine](
      getOrModify = o => o.customer.loyaltyId.toRight(o),
      reverseGet = (pair: (Order, String)) =>
        pair._1.copy(customer = pair._1.customer.copy(loyaltyId = Some(pair._2))),
    )

  val mLoyalty: MOptional[Order, String] =
    MOptional[Order, String](_.customer.loyaltyId)(s =>
      o => o.copy(customer = o.customer.copy(loyaltyId = Some(s)))
    )

  val eoLoyaltyAF: AffineFold[Order, String] =
    AffineFold[Order, String](_.customer.loyaltyId)
