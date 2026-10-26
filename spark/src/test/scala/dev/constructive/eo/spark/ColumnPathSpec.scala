package dev.constructive.eo
package spark

import scala.compiletime.testing.typeCheckErrors

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.upper
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import laws.discipline.{LensTests, OptionalTests}
import laws.{LensLaws, OptionalLaws}
import Fixtures.{*, given}

/** `ColumnOptics.path`: the macro-built symbolic optic must behave like the hand-composed one on
  * Spark, its eager twin like the hand-composed one in Scala, and the twins must be lawful.
  */
class ColumnPathSpec extends Specification with Discipline:

  import SparkEncoders.given

  private lazy val spark: SparkSession = Sessions.connect

  private val street = ColumnOptics.path[Person](_.address.street)
  private val zip = ColumnOptics.path[Person](_.work.get.zip)
  private val officeZip = ColumnOptics.path[Person](_.previous.get.office.get.zip)
  private val work = ColumnOptics.path[Person](_.work)

  private def normalized(plan: String): String = plan.replaceAll("#\\d+", "#")

  "ColumnOptics.path on Spark Connect" should {

    "derive a Total path (Lens + Lens) and match the hand-composed optics" >> {
      (editRows(spark)(street.symbolic.modify(s => Col(upper(s.column)))) ===
        people.map(addressE.andThen(streetE).modify(_.toUpperCase)))
        .and(people.map(street.eager.get) === people.map(_.address.street))
        .and(street.names === List("address", "street"))
    }

    "derive a Partial path (Optional + Optional) through one .get" >> {
      (editRows(spark)(zip.symbolic.modify(z => Col(z.column + 1))) ===
        people.map(workE.andThen(zipE).modify(_ + 1)))
        .and(
          readRows(spark)(r => zip.symbolic.getOption(r).get.asOption) === people.map(
            zip.eager.getOption
          )
        )
        .and(people.map(zip.eager.getOption) === people.map(workE.andThen(zipE).getOption))
    }

    "guard every .get on a deeper path" >> {
      val handE = previousE.andThen(officeE).andThen(zipE)
      (editRows(spark)(officeZip.symbolic.modify(z => Col(z.column * 2))) === people.map(
        handE.modify(_ * 2)
      ))
        .and(people.map(officeZip.eager.modify(_ * 2)) === people.map(handE.modify(_ * 2)))
    }

    "treat a trailing Option field without .get as a total, nullable focus" >> {
      readRows(spark)(work.symbolic.get) === people.map(_.work)
    }

    "compile to the same plan as the hand-composed chain" >> {
      normalized(optimizedPlan(spark)(street.symbolic.modify(s => Col(upper(s.column))))) ===
        normalized(
          optimizedPlan(spark)(addressC.andThen(streetC).modify(s => Col(upper(s.column))))
        )
    }

    "reject selectors that are not field paths at compile time" >> {
      (typeCheckErrors("ColumnOptics.path[Person](_.address.hashCode)") must not(beEmpty))
        .and(typeCheckErrors("ColumnOptics.path[Person](_.work.map(_.zip))") must not(beEmpty))
        .and(typeCheckErrors("ColumnOptics.path[Person](p => p.name.length)") must not(beEmpty))
    }
  }

  private given Arbitrary[Person] = Arbitrary(Gen.oneOf(people))
  private given Cogen[Int] = Cogen.cogenInt

  checkAll(
    "eager twin of a Total path",
    new LensTests[Person, String]:
      val laws = new LensLaws[Person, String]:
        val lens = street.eager
    .lens,
  )

  checkAll(
    "eager twin of a Partial path",
    new OptionalTests[Person, Int]:
      val laws = new OptionalLaws[Person, Int]:
        val optional = officeZip.eager
    .optional,
  )
