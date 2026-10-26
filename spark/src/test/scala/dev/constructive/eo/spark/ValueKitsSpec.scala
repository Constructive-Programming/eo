package dev.constructive.eo
package spark

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.*
import org.apache.spark.unsafe.types.VariantVal
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.specs2.mutable.Specification

import optics.Plated

/** Behaviour pins for [[Schemas]] / [[Rows]], plus a guard that the law generators in
  * [[ValueKitsFixtures]] really produce BOTH hits and misses — the navigation laws are vacuous on
  * one-sided samples.
  */
class ValueKitsSpec extends Specification:

  import ValueKitsFixtures.given
  import Schemas.platedDataType

  private val person = StructType(
    Array(
      StructField("name", StringType, nullable = false),
      StructField(
        "address",
        StructType(Array(StructField("zip", IntegerType), StructField("street", StringType))),
      ),
      StructField("scores", ArrayType(IntegerType, containsNull = false)),
      StructField("tags", MapType(StringType, IntegerType)),
    )
  )

  private def sample[A](gen: Gen[A]): List[A] =
    Gen.listOfN(500, gen).apply(Gen.Parameters.default.withSize(3), Seed(7L)).getOrElse(Nil)

  "Schemas" should {

    "drill through nested structs and keep siblings, names and flags" >> {
      val zip = Schemas.field("address").andThen(Schemas.field("zip"))
      val widened = zip.replace(LongType)(person)
      (zip.getOption(widened) === Some(LongType))
        .and(Schemas.nullable("name").getOption(widened) === Some(false))
        .and(widened.asInstanceOf[StructType].fieldNames.toList === person.fieldNames.toList)
    }

    "widen every IntegerType in the schema with one Plated.transform" >> {
      val widened = Plated.transform[DataType] {
        case IntegerType => LongType
        case other       => other
      }(person)
      (widened.json must not(contain("\"integer\""))).and(
        Schemas.field("scores").andThen(Schemas.element).getOption(widened) === Some(LongType)
      )
    }

    "miss on the wrong node kind and on an absent field, passing writes through" >> {
      (Schemas.element.getOption(person) must beNone)
        .and(Schemas.field("nope").replace(LongType)(person) === person)
        .and(Schemas.mapValue.getOption(IntegerType) must beNone)
    }

    "generate both hits and misses for the drilled law optics" >> {
      val types = sample(summon[org.scalacheck.Arbitrary[DataType]].arbitrary)
      val hits = types.count(Schemas.field("f").getOption(_).isDefined)
      (hits must be_>(25)).and(types.size - hits must be_>(25))
    }
  }

  "Rows" should {

    val schema = StructType(Array(StructField("f", IntegerType), StructField("s", StringType)))
    val row: Row = new GenericRowWithSchema(Array[Any](41, "x"), schema)

    "read and write a typed field by name, keeping the schema" >> {
      val f = Rows.field[Int]("f")
      val written = f.modify(_ + 1)(row)
      (f.getOption(row) === Some(41))
        .and(f.getOption(written) === Some(42))
        .and(written.schema === schema)
    }

    "treat a NULL, a wrong-typed, an absent field and a schema-less row as misses" >> {
      val nul: Row = new GenericRowWithSchema(Array[Any](null, "x"), schema)
      (Rows.field[Int]("f").getOption(nul) must beNone)
        .and(Rows.field[String]("f").getOption(row) must beNone)
        .and(Rows.field[Int]("g").getOption(row) must beNone)
        .and(Rows.field[Int]("f").getOption(Row(41, "x")) must beNone)
        .and(Rows.at[Int](0).getOption(Row(41, "x")) === Some(41))
    }

    "generate both hits and misses for the drilled law optics" >> {
      val rows = sample(summon[org.scalacheck.Arbitrary[Row]].arbitrary)
      val hits = rows.count(Rows.field[Int]("f").getOption(_).isDefined)
      (hits must be_>(50)).and(rows.size - hits must be_>(50))
    }
  }

  "Variants" should {

    val doc = Variants.obj(
      "name" -> Variants.string.reverseGet("eo"),
      "tags" -> Variants.arr(Variants.long.reverseGet(1L), Variants.long.reverseGet(2L)),
    )

    "encode one logical value one way, whatever the key order" >> {
      val a = Variants.obj("b" -> Variants.nullValue, "a" -> Variants.boolean.reverseGet(true))
      val b = Variants.obj("a" -> Variants.boolean.reverseGet(true), "b" -> Variants.nullValue)
      (a === b).and(Variants.canonical(a) === a)
    }

    "drill, rewrite every element, and keep siblings" >> {
      val doubled = Variants.field("tags").andThen(Variants.each).andThen(Variants.long)
      val out = doubled.modify(_ * 2)(doc)
      (Variants.field("tags").andThen(Variants.at(1)).andThen(Variants.long).getOption(out) ===
        Some(4L))
        .and(Variants.field("name").andThen(Variants.string).getOption(out) === Some("eo"))
    }

    "pass writes through misses without inventing nodes" >> {
      (Variants.field("nope").replace(Variants.nullValue)(doc) === doc)
        .and(Variants.at(5).replace(Variants.nullValue)(doc) === doc)
        .and(Variants.string.getOption(doc) must beNone)
    }

    "generate both hits and misses for the drilled law optics" >> {
      val vs = sample(summon[org.scalacheck.Arbitrary[VariantVal]].arbitrary)
      val hits = vs.count(Variants.field("f").getOption(_).isDefined)
      (hits must be_>(25)).and(vs.size - hits must be_>(25))
    }
  }
