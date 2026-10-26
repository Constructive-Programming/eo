package dev.constructive.eo
package spark

import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{col, struct}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

import optics.{Lens, Optional}

// Top level: Spark's product deserializer calls the constructor, and a nested class would need an
// outer pointer.
case class Address(street: String, zip: Int)
case class Job(title: String, office: Option[Address])
case class Person(name: String, address: Address, work: Option[Address], previous: Option[Job])

object Fixtures:
  import SparkEncoders.given

  given AgnosticEncoder[Address] = SparkEncoders.derived
  given AgnosticEncoder[Job] = SparkEncoders.derived
  given AgnosticEncoder[Person] = SparkEncoders.derived

  val people: List[Person] =
    val address = for
      street <- Gen.alphaStr.map(_.take(8))
      zip <- Gen.choose(0, 99999)
    yield Address(street, zip)
    val job = for
      title <- Gen.alphaStr.map(_.take(8))
      office <- Gen.option(address)
    yield Job(title, office)
    val person = for
      name <- Gen.alphaStr.map(_.take(8))
      home <- address
      work <- Gen.option(address)
      previous <- Gen.option(job)
    yield Person(name, home, work, previous)
    Gen.listOfN(200, person).apply(Gen.Parameters.default, Seed(2026L)).getOrElse(Nil)

  // ---- The symbolic optics under test, and their eager twins ----------

  val addressC = ColumnOptics.field[Person, Address]("address")
  val streetC = ColumnOptics.field[Address, String]("street")
  val zipC = ColumnOptics.field[Address, Int]("zip")
  val workC = ColumnOptics.nullableField[Person, Address]("work")
  val previousC = ColumnOptics.nullableField[Person, Job]("previous")
  val officeC = ColumnOptics.nullableField[Job, Address]("office")

  val addressE = Lens[Person, Address](_.address, (p, a) => p.copy(address = a))
  val streetE = Lens[Address, String](_.street, (a, s) => a.copy(street = s))
  val zipE = Lens[Address, Int](_.zip, (a, z) => a.copy(zip = z))

  val workE = Optional[Person, Person, Address, Address](
    p => p.work.toRight(p),
    (p, a) => p.copy(work = Some(a)),
  )

  val previousE = Optional[Person, Person, Job, Job](
    p => p.previous.toRight(p),
    (p, j) => p.copy(previous = Some(j)),
  )

  val officeE = Optional[Job, Job, Address, Address](
    j => j.office.toRight(j),
    (j, a) => j.copy(office = Some(a)),
  )

  // ---- Dataset lifting: a row IS a struct column ----------------------

  def dataset(spark: SparkSession): Dataset[Person] = spark.createDataset(people)

  def row(ds: Dataset[Person]): Col[Person] = Col(struct(ds.columns.toSeq.map(col)*))

  /** Run `edit` on every row as ONE projection and decode the result. */
  def editRows(spark: SparkSession)(edit: Col[Person] => Col[Person]): List[Person] =
    val ds = dataset(spark)
    ds.select(edit(row(ds)).column.as("r")).select("r.*").as[Person].collect().toList

  /** The server's optimized logical plan for `edit` run on every row. */
  def optimizedPlan(spark: SparkSession)(edit: Col[Person] => Col[Person]): String =
    val ds = dataset(spark)
    val out = new java.io.ByteArrayOutputStream
    Console.withOut(out)(ds.select(edit(row(ds)).column.as("r")).select("r.*").explain(true))
    out
      .toString("UTF-8")
      .linesIterator
      .dropWhile(!_.startsWith("== Optimized Logical Plan =="))
      .drop(1)
      .takeWhile(!_.startsWith("=="))
      .mkString("\n")

  def readRows[A](spark: SparkSession)(read: Col[Person] => Col[A])(using
      AgnosticEncoder[A]
  ): List[A] =
    val ds = dataset(spark)
    ds.select(read(row(ds)).column).as[A].collect().toList

/** The Spark the specs run on: a Spark Connect CLIENT in this JVM, talking to a Connect server
  * (Classic engine, local master) in a child JVM on the genuine Scala 2.13 stdlib — Classic itself
  * cannot run next to Scala 3.8+'s `scala-library` (see build.sbt `sparkIntegration`). The build
  * passes the server classpath and JVM flags as system properties.
  */
object Sessions:

  lazy val connect: SparkSession =
    val port = freePort()
    val server = startServer(port)
    sys.addShutdownHook(server.destroy())
    awaitPort(port, server, deadline = System.nanoTime() + 180L * 1000_000_000L)
    val session = org
      .apache
      .spark
      .sql
      .connect
      .SparkSession
      .builder()
      .remote(s"sc://localhost:$port")
      .getOrCreate()
    // The eager bridge ships Scala 3 closures: upload the runtime jars, and the compiled class
    // directories (eo's and the specs') packed into jars ONCE. (`REPLClassDirMonitor` would re-upload
    // every class before each query, and concurrent examples then collide on the server.)
    val dirs = ScalaRuntime.addTo(session, classpathJars.filter(_.contains("scala3-library"))*)
    (dirs ++ classpathDirs).distinct.foreach(d => session.addArtifact(jarOf(d)))
    session

  /** Pack a class directory into a temporary jar, for `addArtifact`. */
  private def jarOf(dir: String): String =
    val root = java.nio.file.Paths.get(dir)
    val jar = java.nio.file.Files.createTempFile("eo-spark-classes-", ".jar")
    scala
      .util
      .Using
      .resource(new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {
        out =>
          scala.util.Using.resource(java.nio.file.Files.walk(root)) { paths =>
            paths.filter(java.nio.file.Files.isRegularFile(_)).forEach { f =>
              out.putNextEntry(
                new java.util.jar.JarEntry(root.relativize(f).toString.replace('\\', '/'))
              )
              java.nio.file.Files.copy(f, out)
              out.closeEntry()
            }
          }
      }
    jar.toFile.deleteOnExit()
    jar.toString

  private def classpath: List[String] =
    prop("java.class.path").split(java.io.File.pathSeparator).nn.toList.map(_.nn)

  private def classpathJars: List[String] = classpath.filter(_.endsWith(".jar"))

  private def classpathDirs: List[String] = classpath.filter(p => new java.io.File(p).isDirectory)

  private def prop(key: String): String =
    sys.props.getOrElse(key, sys.error(s"$key not set — run the spark specs through sbt")).nn

  private def freePort(): Int =
    val socket = new java.net.ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

  private def startServer(port: Int): Process =
    val javaBin = java.nio.file.Paths.get(prop("java.home"), "bin", "java").toString
    val command =
      (javaBin :: prop("eo.spark.connect.jvmOptions").split(' ').toList.filter(_.nonEmpty)) ++ List(
        "-cp",
        prop("eo.spark.connect.classpath"),
        "-Dspark.master=local[1]",
        "-Dspark.ui.enabled=false",
        "-Dspark.sql.shuffle.partitions=1",
        s"-Dspark.connect.grpc.binding.port=$port",
        "org.apache.spark.sql.connect.service.SparkConnectServer",
      )
    new ProcessBuilder(command*)
      .redirectErrorStream(true)
      .redirectOutput(new java.io.File(prop("eo.spark.connect.log")))
      .start()

  @scala.annotation.tailrec
  private def awaitPort(port: Int, server: Process, deadline: Long): Unit =
    val up = scala.util.Using(new java.net.Socket("localhost", port))(_ => ()).isSuccess
    if !up then
      if !server.isAlive then
        sys.error(s"Connect server exited; see ${prop("eo.spark.connect.log")}")
      if System.nanoTime() > deadline then sys.error("Connect server did not start in time")
      Thread.sleep(250)
      awaitPort(port, server, deadline)
