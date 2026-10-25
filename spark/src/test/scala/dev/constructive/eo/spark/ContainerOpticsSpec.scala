package dev.constructive.eo
package spark

import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{lit, sum, upper}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.specs2.mutable.Specification

import optics.{Getter, Lens, Modify, Traversal}

case class Item(sku: String, qty: Int, price: Double)
case class Order(id: String, items: List[Item], tags: Map[String, Int])

object ContainerFixtures:
  import SparkEncoders.given

  given AgnosticEncoder[Item] = SparkEncoders.derived
  given AgnosticEncoder[Order] = SparkEncoders.derived

  val orders: List[Order] =
    val item = for
      sku <- Gen.alphaStr.map(_.take(5))
      qty <- Gen.choose(0, 20)
      // dyadic prices keep the symbolic and eager double sums bit-identical
      price <- Gen.choose(0, 400).map(_ / 4.0)
    yield Item(sku, qty, price)
    val order = for
      id <- Gen.alphaStr.map(_.take(6))
      n <- Gen.choose(0, 4)
      items <- Gen.listOfN(n, item)
      m <- Gen.choose(0, 3)
      tags <- Gen.listOfN(m, Gen.zip(Gen.alphaStr.map(_.take(3)), Gen.choose(0, 9)))
    yield Order(id, items, tags.toMap)
    Gen.listOfN(150, order).apply(Gen.Parameters.default, Seed(42L)).getOrElse(Nil)

  // symbolic optics and their eager twins
  val itemsC = ColumnOptics.field[Order, List[Item]]("items")
  val tagsC = ColumnOptics.field[Order, Map[String, Int]]("tags")
  val idC = ColumnOptics.field[Order, String]("id")
  val qtyC = ColumnOptics.field[Item, Int]("qty")
  val priceC = ColumnOptics.field[Item, Double]("price")

  val itemsE = Lens[Order, List[Item]](_.items, (o, is) => o.copy(items = is))
  val tagsE = Lens[Order, Map[String, Int]](_.tags, (o, t) => o.copy(tags = t))
  val qtyE = Lens[Item, Int](_.qty, (i, q) => i.copy(qty = q))
  val valuesE = Modify[Map[String, Int], Map[String, Int], Int, Int](f => _.view.mapValues(f).toMap)

  def dataset(spark: SparkSession): Dataset[Order] = spark.createDataset(orders)

class ContainerOpticsSpec extends Specification:

  import ContainerFixtures.{*, given}
  import DatasetOptics.*
  import SparkEncoders.given

  private lazy val spark: SparkSession = Sessions.connect

  private def edited(edit: Col[Order] => Col[Order]): List[Order] =
    dataset(spark).editRows(edit).as[Order].collect().toList

  private def read[A](get: Col[Order] => Col[A])(using AgnosticEncoder[A]): List[A] =
    val ds = dataset(spark)
    ds.select(get(ds.rowCol).column).as[A].collect().toList

  private def optimizedPlan(edit: Col[Order] => Col[Order]): String =
    val out = new java.io.ByteArrayOutputStream
    Console.withOut(out)(dataset(spark).editRows(edit).explain(true))
    out
      .toString("UTF-8")
      .linesIterator
      .dropWhile(!_.startsWith("== Optimized Logical Plan =="))
      .drop(1)
      .takeWhile(!_.startsWith("=="))
      .mkString("\n")

  "container optics on Spark Connect" should {

    "round-trip List and Map fields through the derived encoders" >> {
      dataset(spark).collect().toList === orders
    }

    "modify every element's field through Lens ∘ each ∘ Lens" >> {
      val qtyAll = itemsC.andThen(ColumnOptics.each[List, Item]).andThen(qtyC)
      val qtyAllE = itemsE.andThen(Traversal.each[List, Item]).andThen(qtyE)
      edited(qtyAll.modify(q => Col(q.column * 3))) === orders.map(qtyAllE.modify(_ * 3))
    }

    "compile an element-wise edit to ONE projection with ONE transform" >> {
      val plan = optimizedPlan(
        itemsC.andThen(ColumnOptics.each[List, Item]).andThen(qtyC).modify(q => Col(q.column + 1))
      )
      (plan.linesIterator.toList must haveSize(2))
        .and(
          plan must beMatching(
            "(?s)Project \\[id#\\d+, .*transform\\(.* AS items#\\d+, tags#\\d+\\].*"
          )
        )
        .and("transform\\(".r.findAllIn(plan).size === 1)
    }

    "modify every map value, keeping keys" >> {
      val all = tagsC.andThen(ColumnOptics.values[String, Int])
      edited(all.modify(v => Col(v.column + 100))) ===
        orders.map(tagsE.andThen(valuesE).modify(_ + 100))
    }

    "fold an array per row with aggregate" >> {
      val total = itemsC.andThen(
        ColumnOptics.aggregate[List, Item, Double](Col(lit(0.0)))((acc, it) =>
          Col(acc.column + qtyC.get(it).column * priceC.get(it).column)
        )
      )
      read(total.get) === orders.map(_.items.map(i => i.qty * i.price).sum)
    }

    "read size / exists / forall per row" >> {
      val big = itemsC.andThen(ColumnOptics.exists[List, Item](i => Col(qtyC.get(i).column > 15)))
      val free =
        itemsC.andThen(ColumnOptics.forall[List, Item](i => Col(priceC.get(i).column === 0)))
      (read(itemsC.andThen(ColumnOptics.size[List[Item]]).get) === orders.map(_.items.size))
        .and(read(big.get) === orders.map(_.items.exists(_.qty > 15)))
        .and(read(free.get) === orders.map(_.items.forall(_.price == 0)))
    }

    "lift a given optic over the whole Dataset" >> {
      given CanModify[Col[Order], Col[String]] = idC
      given CanGet[Col[Order], Col[Int]] =
        Getter[Col[Order], Col[Int]](o => ColumnOptics.size[List[Item]].get(itemsC.get(o)))
      val ds = dataset(spark)
      (ds.modifyAll[String](s => Col(upper(s.column))).collect().toList ===
        orders.map(o => o.copy(id = o.id.toUpperCase)))
        .and(ds.getAll[Int].collect().toList === orders.map(_.items.size))
        .and(ds.foldAll[Int, Long](n => Col(sum(n.column))) === orders.map(_.items.size.toLong).sum)
    }
  }

case class Leaves(
    b: Byte,
    s: Short,
    f: Float,
    bin: List[Byte],
    big: BigInt,
    dec: BigDecimal,
    day: java.time.LocalDate,
    at: java.time.Instant,
)

class LeafEncodersSpec extends Specification:

  import SparkEncoders.given

  private given AgnosticEncoder[Leaves] = SparkEncoders.derived

  private lazy val spark: SparkSession = Sessions.connect

  "the leaf encoders" should {
    "round-trip every covered leaf type through Spark Connect" >> {
      val values = List(
        Leaves(
          1.toByte,
          2.toShort,
          1.5f,
          List[Byte](1, 2, 3),
          BigInt("123456789012345678901234567890"),
          BigDecimal("12345.678901234567890123"),
          java.time.LocalDate.of(2026, 10, 25),
          java.time.Instant.parse("2026-10-25T12:00:00.123456Z"),
        ),
        Leaves(
          (-1).toByte,
          0.toShort,
          -0.25f,
          Nil,
          BigInt(0),
          BigDecimal("0.5"),
          java.time.LocalDate.of(1970, 1, 1),
          java.time.Instant.EPOCH,
        ),
      )
      spark.createDataset(values).collect().toList === values
    }
  }
