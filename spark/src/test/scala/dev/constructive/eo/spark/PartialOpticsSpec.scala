package dev.constructive.eo
package spark

import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.specs2.mutable.Specification

import optics.{Lens, Optional}

case class Raw(n: String, js: String, tags: Map[String, Int], nums: List[Int])

object PartialFixtures:
  import SparkEncoders.given

  given AgnosticEncoder[Raw] = SparkEncoders.derived

  /** `to_json`'s own spelling of an `Address` — the canonical form the JSON optics round-trip. */
  def addressJson(a: Address): String = s"""{"street":"${a.street}","zip":${a.zip}}"""

  private val AddressJson = """\{"street":"([A-Za-z]*)","zip":(-?\d+)\}""".r

  def parseAddress(s: String): Option[Address] = s match
    case AddressJson(street, zip) => zip.nn.toIntOption.map(Address(street.nn, _))
    case _                        => None

  val raws: List[Raw] =
    val n = Gen.oneOf(
      Gen.choose(-500, 500).map(_.toString),
      Gen.alphaStr.map(_.take(3)),
      Gen.const("007"),
      Gen.const("+5"),
      Gen.const(""),
      Gen.const("99999999999"), // overflows Int: a miss for both twins
    )
    val js = Gen.oneOf(
      Gen
        .zip(Gen.alphaStr.map(_.take(5)), Gen.choose(0, 99999))
        .map((s, z) => addressJson(Address(s, z))),
      Gen.const("not json"),
      Gen.const(""),
      Gen.const("5"), // valid JSON, not an object
      Gen.const("""{"street":"x","zip":"bad"}"""), // an object, wrong field type
    )
    val raw = for
      n <- n
      js <- js
      k <- Gen.choose(0, 3)
      tags <- Gen.listOfN(k, Gen.zip(Gen.oneOf("a", "b", "c"), Gen.choose(0, 9)))
      m <- Gen.choose(0, 3)
      nums <- Gen.listOfN(m, Gen.choose(-9, 9))
    yield Raw(n, js, tags.toMap, nums)
    Gen.listOfN(200, raw).apply(Gen.Parameters.default, Seed(3L)).getOrElse(Nil)

  // symbolic optics
  val nC = ColumnOptics.field[Raw, String]("n")
  val jsC = ColumnOptics.field[Raw, String]("js")
  val tagsC = ColumnOptics.field[Raw, Map[String, Int]]("tags")
  val numsC = ColumnOptics.field[Raw, List[Int]]("nums")

  // eager twins
  val nE = Lens[Raw, String](_.n, (r, v) => r.copy(n = v))
  val jsE = Lens[Raw, String](_.js, (r, v) => r.copy(js = v))
  val tagsE = Lens[Raw, Map[String, Int]](_.tags, (r, v) => r.copy(tags = v))
  val numsE = Lens[Raw, List[Int]](_.nums, (r, v) => r.copy(nums = v))

  def keyE(k: String) = Optional[Map[String, Int], Map[String, Int], Int, Int](
    m => m.get(k).toRight(m),
    (m, v) => if m.contains(k) then m.updated(k, v) else m,
  )

  def atKeyE(k: String) = Optional[Map[String, Int], Map[String, Int], Option[Int], Option[Int]](
    m => Right(m.get(k)),
    (m, ov) => ov.fold(m - k)(m.updated(k, _)),
  )

  def indexE(i: Int) = Optional[List[Int], List[Int], Int, Int](
    l => l.lift(i).toRight(l),
    (l, x) => if l.isDefinedAt(i) then l.updated(i, x) else l,
  )

  /** Spark's `try_cast(string AS int)` on the generated spellings: optional sign, leading zeros. */
  val intTextE =
    Optional[String, String, Int, Int](s => s.toIntOption.toRight(s), (_, i) => i.toString)

  val addressTextE = Optional[String, String, Address, Address](
    s => parseAddress(s).toRight(s),
    (_, a) => addressJson(a),
  )

  def dataset(spark: SparkSession): Dataset[Raw] = spark.createDataset(raws)

class PartialOpticsSpec extends Specification:

  import PartialFixtures.{*, given}
  import DatasetOptics.*
  import SparkEncoders.given

  private lazy val spark: SparkSession = Sessions.connect

  private def edited(edit: Col[Raw] => Col[Raw]): List[Raw] =
    dataset(spark).editRows(edit).as[Raw].collect().toList

  private def read[A](get: Col[Raw] => Col[A])(using AgnosticEncoder[A]): List[A] =
    val ds = dataset(spark)
    ds.select(get(ds.rowCol).column).as[A].collect().toList

  private val addressSchema: StructType =
    StructType.fromDDL("street STRING, zip INT")

  "partial column optics on Spark Connect" should {

    "read and modify a map entry by key, passing rows without the key through" >> {
      val a = tagsC.andThen(ColumnOptics.key[String, Int](Col(lit("a"))))
      val aE = tagsE.andThen(keyE("a"))
      (read(r => a.getOption(r).get.asOption) === raws.map(aE.getOption))
        .and(edited(a.modify(v => Col(v.column * 10))) === raws.map(aE.modify(_ * 10)))
    }

    "create, update and delete a map entry with atKey" >> {
      val at = tagsC.andThen(ColumnOptics.atKey[String, Int](Col(lit("b"))))
      val atE = tagsE.andThen(atKeyE("b"))
      (edited(at.replace(Col(lit(7)))) === raws.map(atE.replace(Some(7))))
        .and(edited(at.replace(Col(lit(null).cast(IntegerType)))) === raws.map(atE.replace(None)))
        .and(read(r => at.getOption(r).get) === raws.map(r => atE.getOption(r).flatten))
    }

    "read and modify an array element by index, passing short arrays through" >> {
      val second = numsC.andThen(ColumnOptics.index[List, Int](1))
      val secondE = numsE.andThen(indexE(1))
      (read(r => second.getOption(r).get.asOption) === raws.map(secondE.getOption))
        .and(edited(second.modify(x => Col(x.column + 100))) === raws.map(secondE.modify(_ + 100)))
    }

    "read a string column as Int with cast, passing unconvertible rows through" >> {
      val asInt = nC.andThen(ColumnOptics.cast[String, Int](IntegerType, StringType))
      val asIntE = nE.andThen(intTextE)
      (read(r => asInt.getOption(r).get.asOption) === raws.map(asIntE.getOption))
        .and(edited(asInt.modify(i => Col(i.column + 1))) === raws.map(asIntE.modify(_ + 1)))
    }

    "edit a field inside JSON text, passing unparseable rows through" >> {
      val zip = jsC
        .andThen(ColumnOptics.json[Address](addressSchema))
        .andThen(ColumnOptics.field[Address, Int]("zip"))
      val zipE =
        jsE.andThen(addressTextE).andThen(Lens[Address, Int](_.zip, (a, z) => a.copy(zip = z)))
      (read(r => zip.getOption(r).get.asOption) === raws.map(zipE.getOption))
        .and(edited(zip.modify(z => Col(z.column + 1))) === raws.map(zipE.modify(_ + 1)))
    }

    "read a VARIANT path out of JSON text" >> {
      val zip = jsC.andThen(ColumnOptics.parseJson)
      val zipRead = (r: Col[Raw]) =>
        ColumnOptics.variantGet[Option[Int]]("$.zip", "int").get(zip.getOption(r).get)
      read(zipRead) === raws.map(r => parseAddress(r.js).map(_.zip))
    }
  }
