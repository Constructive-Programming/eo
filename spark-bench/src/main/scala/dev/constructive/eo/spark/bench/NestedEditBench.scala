package dev.constructive.eo.spark
package bench

import dev.constructive.eo.CanModify
import java.util.concurrent.TimeUnit
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{col, concat, hash, lit, struct, sum, upper}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.openjdk.jmh.annotations.*

case class BAddress(street: String, zip: Int)
case class BPerson(name: String, address: BAddress)

/** Bridge functions in a top-level object, so the server can load their implementing class. */
object BenchFns:
  val upperCase: String => String = _.toUpperCase

/** One nested edit — upper-case `address.street` — three ways, each forced by a checksum aggregate
  * over the edited rows, against a cached server-side Dataset (Spark Connect, local server JVM):
  *
  *   - `symbolic`: `ColumnOptics.path[BPerson](_.address.street)` lifted with `editRows`;
  *   - `handWritten`: the same projection spelled with `withField` directly — the baseline the
  *     optic should match;
  *   - `eagerMap`: `mapThrough` with the eager twin — decode, Scala closure, re-encode.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(1)
class NestedEditBench:

  import DatasetOptics.*
  import SparkEncoders.given

  @Param(Array("200000"))
  var rows: Int = 0

  private given AgnosticEncoder[BAddress] = SparkEncoders.derived
  private given AgnosticEncoder[BPerson] = SparkEncoders.derived

  private val street = ColumnOptics.path[BPerson](_.address.street)

  private var spark: SparkSession = null
  private var people: Dataset[BPerson] = null

  @Setup(Level.Trial)
  def setup(): Unit =
    spark = Sessions.connect
    Sessions.ship(spark, classOf[BPerson])
    people = spark
      .range(rows.toLong)
      .select(
        concat(lit("n"), col("id").cast("string")).as("name"),
        struct(
          concat(lit("street "), col("id").cast("string")).as("street"),
          (col("id") % 99999).cast("int").as("zip"),
        ).as("address"),
      )
      .as[BPerson]
      .cache()
    people.count(): Unit

  @TearDown(Level.Trial)
  def tearDown(): Unit = people.unpersist(): Unit

  private def checksum(ds: Dataset[?]): Long =
    ds.select(sum(hash(col("name"), col("address")))).head().getLong(0)

  @Benchmark
  def symbolic(): Long =
    checksum(people.editRows(street.symbolic.modify(s => Col(upper(s.column)))))

  @Benchmark
  def handWritten(): Long =
    checksum(
      people.select(
        col("name"),
        col("address").withField("street", upper(col("address.street"))).as("address")
      )
    )

  @Benchmark
  def eagerMap(): Long =
    given CanModify[BPerson, String] = street.eager
    checksum(people.mapThrough[String](BenchFns.upperCase))
