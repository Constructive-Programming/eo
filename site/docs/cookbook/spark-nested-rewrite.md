# Rewriting a deeply nested field in a Spark table

**The task:** a Parquet table of orders, where each order holds a customer, the customer holds an
optional billing address, and the address holds a country code. Normalize every country code to
upper case. Leave rows without a billing address untouched, and leave **every other column
exactly as it is**.

Plain Spark makes you rebuild each level by hand: a `withField` per struct, plus a
`when(isnull(...))` for the optional one. Get a level wrong and you silently drop a field.

With `cats-eo-spark` the path is one selector. The optic built from it compiles to the projection
you would have written by hand. Catalyst removes the row wrapper and folds the NULL guard into
`withField`'s own NULL check.

```scala mdoc:compile-only
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.upper
import dev.constructive.eo.*
import dev.constructive.eo.spark.*
import dev.constructive.eo.spark.DatasetOptics.*
import dev.constructive.eo.spark.SparkEncoders.given

case class Address(line: String, country: String)
case class Customer(id: String, billing: Option[Address])
case class Order(id: String, customer: Customer, total: BigDecimal)

given AgnosticEncoder[Address] = SparkEncoders.derived
given AgnosticEncoder[Customer] = SparkEncoders.derived
given AgnosticEncoder[Order] = SparkEncoders.derived

// `.get` marks the optional step: rows whose billing address is NULL pass through.
val country = ColumnOptics.path[Order](_.customer.billing.get.country)

val spark: SparkSession = ??? // a Spark Connect session — see the Spark integration page
val orders: Dataset[Order] = spark.read.parquet("s3://bucket/orders").as[Order]

orders
  .editRows(country.symbolic.modify(c => Col(upper(c.column))))
  .write
  .parquet("s3://bucket/orders-normalized")
```

**The same rule, unit-tested without Spark.** `country.eager` is the twin optic over `Order`
values, built from the same selector, so it addresses the same field by construction:

```scala mdoc:compile-only
import dev.constructive.eo.spark.ColumnOptics

case class Address(line: String, country: String)
case class Customer(id: String, billing: Option[Address])
case class Order(id: String, customer: Customer, total: BigDecimal)

val country = ColumnOptics.path[Order](_.customer.billing.get.country)

val sample = Order("o1", Customer("c1", Some(Address("1 Main St", "se"))), BigDecimal(10))
assert(country.eager.modify(_.toUpperCase)(sample).customer.billing.map(_.country) == Some("SE"))
assert(country.eager.getOption(sample.copy(customer = Customer("c2", None))) == None)
```

**When the edit isn't expressible as a column expression** (an arbitrary Scala function, say),
`orders.mapThrough(f)(using country.eager)` runs the eager optic inside a typed `Dataset.map`. That
leaves the optimizer and costs about 5× on the benchmark; see the
[Spark integration page](../integrations/spark.md#the-eager-bridge).
