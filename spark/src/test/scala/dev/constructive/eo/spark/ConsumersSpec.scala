package dev.constructive.eo
package spark

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{count, sum}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.specs2.mutable.Specification

import optics.Getter

case class Score(name: String, points: Int)

/** Bridge functions live in a top-level object: a lambda written inside the spec would be compiled
  * as a method of the spec class, which the server would then have to load (specs2 and all).
  */
object BridgeFns:
  val reverse: String => String = _.reverse

/** The capability-taking consumers (filter, join, group, explode) and the eager bridge. */
class ConsumersSpec extends Specification:

  import DatasetOptics.*
  import Fixtures.{*, given}
  import SparkEncoders.given

  private given AgnosticEncoder[Score] = SparkEncoders.derived

  private lazy val spark: SparkSession = Sessions.connect

  private val scores: List[Score] =
    people.zipWithIndex.collect { case (p, i) if i % 3 != 0 => Score(p.name, i) } ++
      List(Score("nobody", -1))

  "consumers on Spark Connect" should {

    "keep the rows where a partial focus hits" >> {
      given CanGetOption[Col[Person], Col[Int]] = ColumnOptics.path[Person](_.work.get.zip).symbolic
      dataset(spark).filterHits[Int].collect().toList === people.filter(_.work.isDefined)
    }

    "join on keys read through capabilities" >> {
      given personName: CanGet[Col[Person], Col[String]] =
        ColumnOptics.path[Person](_.name).symbolic
      given scoreName: CanGet[Col[Score], Col[String]] = ColumnOptics.path[Score](_.name).symbolic
      val joined =
        dataset(spark).joinOn[Score, String](spark.createDataset(scores)).collect().toList
      val expected = for p <- people; s <- scores if s.name == p.name yield (p, s)
      joined.sortBy(_.toString) === expected.sortBy(_.toString)
    }

    "group by one focus and aggregate another" >> {
      given employed: CanGet[Col[Person], Col[Boolean]] =
        Getter[Col[Person], Col[Boolean]](p => Col(workC.getOption(p).get.column.isNotNull))
      given name: CanGet[Col[Person], Col[String]] = ColumnOptics.path[Person](_.name).symbolic
      val counted =
        dataset(spark).groupAggregate[Boolean, String, Long](n => Col(count(n.column))).collect()
      counted.toMap === people.groupBy(_.work.isDefined).view.mapValues(_.size.toLong).toMap
    }

    "explode an array focus into its own Dataset" >> {
      import ContainerFixtures.{orders, given}
      given CanGet[Col[Order], Col[List[Item]]] = ColumnOptics.path[Order](_.items).symbolic
      val ds: Dataset[Order] = spark.createDataset(orders)
      (ds.explodeAll[List, Item].collect().toList.sortBy(_.toString) ===
        orders.flatMap(_.items).sortBy(_.toString))
        .and(
          ds.explodeAll[List, Item].select(sum("qty")).head().getLong(0) === orders
            .flatMap(_.items)
            .map(_.qty.toLong)
            .sum
        )
    }
  }

  "the eager bridge" should {

    "serialize composed eager optics" >> {
      val optic = ColumnOptics.path[Person](_.previous.get.office.get.zip).eager
      val bytes = new ByteArrayOutputStream
      val out = new ObjectOutputStream(bytes)
      out.writeObject(optic)
      out.close()
      val back = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray))
        .readObject()
        .asInstanceOf[optics.Optional[Person, Person, Int, Int]]
      people.map(back.modify(_ + 1)) === people.map(optic.modify(_ + 1))
    }

    "run an eager optic inside a server-side Dataset.map" >> {
      given CanModify[Person, String] = ColumnOptics.path[Person](_.address.street).eager
      dataset(spark).mapThrough[String](BridgeFns.reverse).collect().toList ===
        people.map(p => p.copy(address = p.address.copy(street = p.address.street.reverse)))
    }
  }
