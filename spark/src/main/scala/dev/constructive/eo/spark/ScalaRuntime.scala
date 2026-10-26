package dev.constructive.eo
package spark

import java.nio.file.Paths
import org.apache.spark.sql.SparkSession

/** Shipping Scala 3 code to a Spark Connect server, for the eager bridge
  * ([[DatasetOptics.mapThrough]] or any typed `Dataset.map`).
  *
  * The server runs on Spark's own Scala 2.13 stdlib. A Scala 3 closure needs, in the session's
  * artifacts: the Scala 3 runtime (`scala3-library`, and the Scala-3-compiled `scala-library` it
  * links against), the libraries it calls (cats, cats-eo), and the application's own classes.
  * Verified against Spark 4.2: with those uploaded, typed `map`s over eo-encoded case classes run
  * server-side.
  */
object ScalaRuntime:

  /** The jar (or class directory) a class was loaded from, when the JVM reports one. */
  def locationOf(cls: Class[?]): Option[String] =
    Option(cls.getProtectionDomain)
      .flatMap(pd => Option(pd.getCodeSource))
      .flatMap(cs => Option(cs.getLocation))
      .map(url => Paths.get(url.toURI).toString)

  /** Where the Scala 3 runtime, cats (core + kernel), cats-eo and cats-eo-spark were loaded from.
    */
  def locations: List[String] =
    List(
      classOf[scala.runtime.LazyVals.type], // scala3-library
      classOf[scala.Option[?]], // scala-library (Scala-3-compiled since 3.8)
      classOf[cats.Functor[?]], // cats-core
      classOf[cats.kernel.Monoid[?]], // cats-kernel
      classOf[optics.Optic[?, ?, ?, ?, ?]], // cats-eo
      classOf[Col.type], // cats-eo-spark
    ).flatMap(locationOf).distinct

  /** Upload every JAR among [[locations]] (plus `extraJars`) with `addArtifact`, returning the
    * locations that are class DIRECTORIES — `addArtifact` takes files only, so package those as
    * jars first (in production they already are). The Connect client's `REPLClassDirMonitor` also
    * works for a directory, but it re-uploads every class before EACH query, and concurrent queries
    * on one session then collide on the server's artifact files.
    */
  def addTo(spark: SparkSession, extraJars: String*): List[String] =
    val (jars, dirs) = (locations ++ extraJars).distinct.partition(_.endsWith(".jar"))
    jars.foreach(spark.addArtifact)
    dirs
