package dev.constructive.eo
package bench
package fixture

/** Canonical realistic domain model shared across the integration benches.
  *
  * One `Order` event, deliberately deep *and* wide *and* arrayed so a single fixture exercises
  * every optic family and every byte/AST backend (circe / jsoniter / avro) on the *same* logical
  * schema — the precondition for apples-to-apples cross-backend numbers.
  *
  * Foci the schema unlocks:
  *   - `order.id: Long` — depth-1 scalar (Lens / Getter / Modify; jsoniter `$.id`).
  *   - `customer.address.street: String` — depth-3 product path (deep Lens / JsonPrism).
  *   - `customer.loyaltyId: Option[String]` — Optional / AffineFold focus.
  *   - `lines[*].name: String` — array Traversal (`$.lines[*].name`).
  *   - `lines[*].price: Double` — Fold focus (`$.lines[*].price`).
  *
  * Wide on purpose: every level carries ≥5 fields, so the naive decode-modify-encode baseline pays
  * the full per-field codec cost — the non-degenerate case the old `JsonPrismWideBench` was built
  * to surface, now folded into the canonical fixture.
  *
  * Codec / optic instances (circe `Codec.AsObject`, jsoniter `JsonValueCodec`, avro
  * `AvroEncoder`/`Decoder`/`SchemaFor`, plus the EO and Monocle optics) live in per-backend fixture
  * objects so a backend that fails to derive never blocks the others.
  */
final case class Address(
    street: String,
    city: String,
    zip: String,
    country: String,
)

final case class Customer(
    name: String,
    email: String,
    tier: String,
    loyaltyId: Option[String],
    address: Address,
)

final case class LineItem(
    sku: String,
    name: String,
    qty: Int,
    price: Double,
    tags: List[String],
)

final case class Order(
    id: Long,
    currency: String,
    region: String,
    customer: Customer,
    // `List` (not `Vector`) so kindlings-avro derives a `Schema.Type.ARRAY`
    // uniformly with circe / jsoniter — keeps the schema cross-backend.
    lines: List[LineItem],
)

object Domain:

  /** A fully-populated customer with a non-empty `loyaltyId` so the Optional / AffineFold
    * Some-branch does real work.
    */
  val DefaultCustomer: Customer =
    Customer(
      name = "Alice",
      email = "alice@example.com",
      tier = "gold",
      loyaltyId = Some("LOYAL-001"),
      address = Address("Main St", "Springfield", "12345", "US"),
    )

  /** Deterministic `Order` with `n` line items — no `Math.random`, so every fork sees identical
    * input and JMH's steady-state numbers are comparable across runs.
    */
  def mkOrder(n: Int): Order =
    val lines = List.tabulate(n) { i =>
      LineItem(
        sku = s"sku-$i",
        name = s"item-$i",
        qty = i,
        price = i.toDouble + 0.99,
        tags = List("a", "b", "c"),
      )
    }
    Order(
      id = 42L,
      currency = "USD",
      region = "us-east-1",
      customer = DefaultCustomer,
      lines = lines,
    )
