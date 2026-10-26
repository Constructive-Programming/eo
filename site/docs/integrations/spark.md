# Spark integration

The `cats-eo-spark` module turns the Spark SQL operations that **are** optics into eo optic
constructions, so they compose with `andThen` and run through the usual `.get` / `.modify` /
`.replace` / `.getOption` surface. The operations that are not optics (joins, grouping,
`explode`) become **consumers** that read their keys through a capability.

The central idea: **a Spark `Column` is a plain Scala value, an unevaluated expression.** A lens
whose source and focus are typed columns is an ordinary eo `Lens`. Its `get` builds a
`getField`, its `replace` builds a `withField`, and Spark's optimizer compiles the composed
expression. No row is ever decoded into a JVM object.

```scala
libraryDependencies ++= Seq(
  "dev.constructive" %% "cats-eo-spark" % "@VERSION@",
  // Spark publishes Scala 2.13 artifacts only; Scala 3 consumes them like this:
  ("org.apache.spark" %% "spark-connect-client-jvm" % "4.2.0").cross(CrossVersion.for3Use2_13),
)
```

The module compiles against `spark-sql-api` alone (`Provided`). That is the API Spark Classic and
Spark Connect share.

## Runtime: Spark Connect

**Spark Classic cannot run in a JVM on Scala 3.8+.** Since 3.8 the Scala standard library is
compiled by Scala 3, and Spark's Scala 2 runtime reflection (`ScalaReflection`, the reflective
session lookup in `SparkSession.builder()`) can no longer start on it: `class Array does not have a
member apply`. Use **Spark Connect** instead. The client runs in your Scala 3 JVM, and the engine
runs in a Connect server, on Spark's own Scala 2.13 stdlib.

```scala
import org.apache.spark.sql.SparkSession

// NOT org.apache.spark.sql.SparkSession.builder(): its implementation lookup is reflective.
val spark: SparkSession =
  org.apache.spark.sql.connect.SparkSession.builder().remote("sc://localhost:15002").getOrCreate()
```

Two consequences:
- **Encoders come from eo.** `SparkEncoders.derived` derives case-class encoders at compile
  time (a [Hearth](https://scala-hearth.readthedocs.io/en/stable/) macro, like eo-generics').
  Spark's `Encoders.product` needs a Scala 2 `TypeTag`, and its `ProductEncoder` decode path
  needs `ScalaReflection`.
- **Collections and tuples are covered too**, through paths Spark decodes without reflection.

```scala mdoc:silent
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{col, lit, struct, upper}
import dev.constructive.eo.*
import dev.constructive.eo.spark.*
import dev.constructive.eo.spark.SparkEncoders.given

case class Address(street: String, zip: Int)
case class Person(name: String, address: Address, work: Option[Address])

given AgnosticEncoder[Address] = SparkEncoders.derived
given AgnosticEncoder[Person] = SparkEncoders.derived
```

## Typed columns and struct lenses

`Col[A]` is an opaque `Column` tagged with the Scala shape of its values. A struct field is a
core `Lens` over `Col`s. A row is a struct column, so its edits are column expressions too:

```scala mdoc
val row: Col[Person] = Col(struct(col("name"), col("address"), col("work")))

val street = ColumnOptics.field[Person, Address]("address")
  .andThen(ColumnOptics.field[Address, String]("street"))

street.get(row).column
street.modify(s => Col(upper(s.column)))(row).column
```

The selector macro builds the same optic from a path, and also builds its **eager twin**, the
ordinary optic over `Person` values. Spark runs the first and Scala runs the second. Because both
come from one selector, they address the same field by construction:

```scala mdoc
val streetPath = ColumnOptics.path[Person](_.address.street)

streetPath.symbolic.modify(s => Col(upper(s.column)))(row).column
streetPath.eager.modify(_.toUpperCase)(Person("eo", Address("main st", 1), None))
```

## Partial foci: partiality lives in the complement

A symbolic optic runs when the query is **built**, before any row exists. A partial focus
therefore has two kinds of miss, and core `Optional` holds both. **There is no new carrier.**

- **A plan-time miss** (the schema lacks the field) is `Left`: `getOption` is `None` and `modify`
  hands the column back unchanged.
- **A per-row miss** is a plan-time **hit** whose focus is NULL on the missing rows. The rebuild
  is guarded by `when(present, rebuild).otherwise(source)`, with the source taken from the
  complement, so missing rows come out exactly as they went in.

`.get` on an `Option` field marks a nullable step:

```scala mdoc
val workZip = ColumnOptics.path[Person](_.work.get.zip)

workZip.symbolic.modify(z => Col(z.column + 1))(row).column
```

Catalyst folds that guard into the NULL check `withField` emits anyway. The optimized plan of a
whole-Dataset edit is the projection you would write by hand:

```
Project [name#16, address#17, if (isnull(work#18)) null else named_struct(street, work#18.street, zip, (work#18.zip + 1)) AS work#31]
+- LocalRelation [name#16, address#17, work#18]
```

The other partial foci follow the same pattern:

| Constructor | Focus | Writes | Lawful |
|---|---|---|---|
| `key(k)` | `try_element_at` | `transform_values` on that entry | yes |
| `atKey(k)` | `Col[Option[V]]` | create / update / delete | for maps without NULL values |
| `index(i)` | `get` | positional `transform` | yes |
| `cast(to, back)` | `try_cast` | `cast` back | on canonical inputs (`"007"` writes back as `"7"`) |
| `json(schema)` | `from_json` + corrupt-record column | `to_json` | on canonical JSON |
| `parseJson` | `try_parse_json` | `to_json` | on canonical JSON |
| `variantGet(path, type)` | `try_variant_get` | read-only (`Getter`) | — |

`json` carries a reserved corrupt-record column because plain PERMISSIVE `from_json` decodes
malformed or non-object input to a struct of NULL fields, **not** to NULL. A naive presence test
would then rewrite `"not json"` as `{}`.

## Containers: writes are `Modify`, reads are aggregates

A symbolic array has no elements to hand out while the query is being built, so element-wise
writes are a core `Modify` (`transform`, `transform_values`) and element-wise reads are `Getter`s
into one aggregate. An effectful `modifyA` cannot run inside a Spark expression at all.

```scala mdoc
case class Item(sku: String, qty: Int, price: Double)
case class Order(id: String, items: List[Item])

val orderRow: Col[Order] = Col(struct(col("id"), col("items")))
val qty = ColumnOptics.field[Order, List[Item]]("items")
  .andThen(ColumnOptics.each[List, Item])
  .andThen(ColumnOptics.field[Item, Int]("qty"))

qty.modify(q => Col(q.column * 2))(orderRow).column
```

## Datasets: one projection, consumed through capabilities

`DatasetOptics` lifts any `Col[S]` optic onto a `Dataset[S]` as **one** projection, whose wrapping
struct Catalyst removes. The non-optic operations take the optic as a capability, per eo's
consume-via-capability rule:

```scala mdoc:compile-only
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions.count
import dev.constructive.eo.spark.DatasetOptics.*

val spark: SparkSession = ???
val people: Dataset[Person] = spark.createDataset(Seq.empty[Person])

people.editRows(streetPath.symbolic.modify(s => Col(upper(s.column))))   // DataFrame

given CanModify[Col[Person], Col[String]] = streetPath.symbolic
people.modifyAll[String](s => Col(upper(s.column)))                      // Dataset[Person]

given CanGetOption[Col[Person], Col[Int]] = workZip.symbolic
people.filterHits[Int]                                                   // rows with a work zip
```

`joinOn`, `groupAggregate` and `explodeAll` follow the same pattern. Each one reads its keys and
values through `CanGet`.

## The eager bridge

`mapThrough` runs an **eager** optic inside a typed `Dataset.map`. It is the escape hatch for
edits a column expression cannot say. It leaves Catalyst, and the closure runs **on the server**,
so the session needs:
- the Scala 3 runtime, cats and cats-eo (`ScalaRuntime.addTo(spark)` uploads their jars);
- your application's classes.

Optics are `Serializable`. Define bridge functions where the server can load their class, e.g. in
a top-level object.

```scala mdoc:compile-only
import org.apache.spark.sql.{Dataset, SparkSession}
import dev.constructive.eo.spark.DatasetOptics.*

object Fns:
  val upperCase: String => String = _.toUpperCase

val spark: SparkSession = ???
ScalaRuntime.addTo(spark) // plus spark.addArtifact("my-app.jar")

given CanModify[Person, String] = streetPath.eager
spark.createDataset(Seq.empty[Person]).mapThrough[String](Fns.upperCase)
```

The cost, for one nested edit over a cached 200 000-row Dataset (local Connect server, JDK 25,
`sparkBench`):

| | ms/op |
|---|---:|
| hand-written `withField` | 107.0 ± 5.4 |
| `ColumnOptics.path` (symbolic) | 106.4 ± 12.2 |
| `mapThrough` (eager bridge) | 187.6 ± 10.9 |
| `Dataset.map` with a hand-written `copy` | 188.7 ± 11.3 |
| `Dataset.map(identity)` | 174.2 ± 11.7 |

The bridge costs what any typed `Dataset.map` costs: every row is decoded to JVM objects and
re-encoded. The optic itself adds nothing measurable.

## Spark's own values: schemas, rows and variants

These kits need no session. `Schemas` covers the `DataType` tree. It is not named `DataTypes`,
because Spark has a Java factory of that name in `org.apache.spark.sql.types`. `Plated` gives
whole-schema rewrites:

```scala mdoc:silent
import org.apache.spark.sql.types.*
import dev.constructive.eo.optics.Plated
import Schemas.platedDataType

val schema: DataType = StructType.fromDDL("id INT, address STRUCT<zip: INT, street: STRING>")
```

```scala mdoc
Schemas.field("address").andThen(Schemas.field("zip")).getOption(schema)
Plated.transform[DataType] { case IntegerType => LongType; case t => t }(schema).simpleString
```

`Rows` gives typed `field[A]` / `at[A]` access into untyped `Row`s, keeping the schema on writes.
`Variants` edits Spark 4 `VARIANT` values. The binary format has no in-place update, so writes
rebuild the value, always in one canonical encoding. That makes `VariantVal`'s byte equality
structural:

```scala mdoc
val doc = Variants.obj(
  "name" -> Variants.string.reverseGet("eo"),
  "tags" -> Variants.arr(Variants.long.reverseGet(1L), Variants.long.reverseGet(2L)),
)

val tags = Variants.field("tags").andThen(Variants.each).andThen(Variants.long)
tags.modify(_ * 10)(doc).toJson(java.time.ZoneOffset.UTC)
```

## Which Spark operations are optics?

| Spark operation | eo construction | Lawful? |
|---|---|---|
| `getField` / `withField` | `Lens` (`field`, `path`) | on non-NULL structs |
| nullable struct / `Option` field | guarded `Optional` (`nullableField`, `path(_.x.get…)`) | per row |
| map key, array index | guarded `Optional` (`key`, `atKey`, `index`) | yes (`atKey`: no NULL values) |
| `try_cast`, `from_json`, `try_parse_json` | guarded `Optional` | on canonical inputs |
| `transform`, `transform_values` | `Modify` (`each`, `values`) | yes |
| `aggregate`, `size`, `exists`, `forall` | `Getter` | Getter laws |
| `try_variant_get` | `Getter` (read-only) | — |
| `select` / `withColumn` over the whole row | `editRows` / `modifyAll` | as the optic |
| `filter` on a focus, `join`, `groupBy().agg`, `explode` | capability consumers | not optics |
| `Dataset.map` | `mapThrough` (eager bridge) | as the eager optic |
| `DataType`, `Row`, `VariantVal` values | `Schemas`, `Rows`, `Variants` kits | yes (law-checked) |
