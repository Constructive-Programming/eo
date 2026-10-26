package dev.constructive.eo
package spark

import org.apache.spark.sql.functions.{col, lit, parse_json, upper, variant_get}
import org.apache.spark.sql.types.{StructField, StructType, VariantType}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.unsafe.types.VariantVal
import org.specs2.mutable.Specification

import Fixtures.*
import SparkEncoders.given

/** Differential checks: every symbolic optic, evaluated by Spark over the whole fixture as ONE
  * projection, must agree with its eager twin evaluated in Scala.
  */
class ColumnOpticsSpec extends Specification:

  private lazy val spark: SparkSession = Sessions.connect

  "column optics on Spark Connect" should {

    "read a nested field through a composed lens" >> {
      readRows(spark)(addressC.andThen(streetC).get) ===
        people.map(addressE.andThen(streetE).get)
    }

    "modify a nested field through a composed lens" >> {
      editRows(spark)(addressC.andThen(streetC).modify(s => Col(upper(s.column)))) ===
        people.map(addressE.andThen(streetE).modify(_.toUpperCase))
    }

    "replace a nested field" >> {
      editRows(spark)(addressC.andThen(zipC).replace(Col(lit(7)))) ===
        people.map(addressE.andThen(zipE).replace(7))
    }

    "read a field under a NULLable struct as a per-row miss" >> {
      readRows(spark)(r => workC.andThen(zipC).getOption(r).get.asOption) ===
        people.map(workE.andThen(zipE).getOption)
    }

    "modify under a NULLable struct, passing NULL rows through" >> {
      editRows(spark)(workC.andThen(zipC).modify(z => Col(z.column + 1))) ===
        people.map(workE.andThen(zipE).modify(_ + 1))
    }

    "compose two per-row guards (NULLable inside NULLable)" >> {
      val zipUnder = previousC.andThen(officeC).andThen(zipC)
      val zipUnderE = previousE.andThen(officeE).andThen(zipE)
      (editRows(spark)(zipUnder.modify(z => Col(z.column * 2))) ===
        people.map(zipUnderE.modify(_ * 2)))
        .and(
          readRows(spark)(r => zipUnder.getOption(r).get.asOption) ===
            people.map(zipUnderE.getOption)
        )
    }

    "treat an absent schema field as a plan-time miss" >> {
      val ds = dataset(spark)
      val nope = ColumnOptics.fieldOf[Person, String](ds.schema, "nope")
      val name = ColumnOptics.fieldOf[Person, String](ds.schema, "name")
      (nope.getOption(row(ds)) must beNone)
        .and(nope.modify(s => Col(upper(s.column)))(row(ds)).column must beEqualTo(row(ds).column))
        .and(name.getOption(row(ds)) must beSome)
    }

    "compile a nested edit to ONE projection that rebuilds only the edited struct" >> {
      val plan = optimizedPlan(spark)(addressC.andThen(streetC).modify(s => Col(upper(s.column))))
      // the row-struct wrapper is gone: untouched columns stay bare attributes over the relation
      (plan.linesIterator.toList must haveSize(2))
        .and(
          plan must beMatching(
            "(?s)Project \\[name#\\d+, .* AS address#\\d+, work#\\d+, previous#\\d+\\].*"
          )
        )
        .and(plan must not(contain("named_struct(name")))
        .and(plan must contain("LocalRelation"))
    }

    "edit a VARIANT collected from Spark and hand Spark back valid bytes" >> {
      val collected = spark
        .range(1)
        .select(parse_json(lit("""{"a":{"b":5,"z":"keep"},"c":[1,2]}""")).as("v"))
        .collect()
        .head
        .getAs[VariantVal](0)
      val b = Variants.field("a").andThen(Variants.field("b")).andThen(Variants.long)
      val edited = b.modify(_ + 1)(collected)
      val back = spark.createDataFrame(
        java.util.List.of(Row(edited)),
        StructType(Array(StructField("v", VariantType))),
      )
      val read = back
        .select(
          variant_get(col("v"), "$.a.b", "int"),
          variant_get(col("v"), "$.a.z", "string"),
          variant_get(col("v"), "$.c[1]", "int"),
        )
        .collect()
        .head
      (b.getOption(collected) === Some(5L))
        .and((read.getInt(0), read.getString(1), read.getInt(2)) === ((6, "keep", 2)))
    }

    "fold the per-row guard into the struct rebuild's own NULL check" >> {
      val plan = optimizedPlan(spark)(workC.andThen(zipC).modify(z => Col(z.column + 1)))
      (plan must not(contain("CASE WHEN"))).and(plan must contain("if (isnull(work#"))
    }
  }
