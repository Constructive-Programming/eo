// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo
package zio

import _root_.zio.{Chunk, Exit, Ref, Runtime, Unsafe, ZEnvironment, ZIO, ZLayer}
import cats.laws.discipline.TraverseTests
import org.scalacheck.{Arbitrary, Cogen, Gen}

import laws.TraversalLaws
import laws.discipline.TraversalTests
import optics.Lens

final case class NDb(url: String, pool: Int)
final case class NMetrics(prefix: String)

/** Scala Native twin of `ZioOpticsSpec` + `ChunksLawsSpec` (specs2 has no Native artifact). */
class NativeZioSuite extends munit.DisciplineSuite:

  private val db = NDb("jdbc:h2", 4)
  private val metrics = NMetrics("eo")
  private val env = ZEnvironment(db).add(metrics)

  private val dbL = service[NDb & NMetrics, NDb]
  private val urlL = Lens[NDb, String](_.url, (d, u) => d.copy(url = u))

  given CanModify[NDb, Int] = Lens[NDb, Int](_.pool, (d, p) => d.copy(pool = p))
  given CanGet[NDb, String] = urlL
  given CanModify[NDb, String] = urlL

  private def run[A](io: ZIO[Any, Any, A]): A =
    Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(io).getOrThrowFiberFailure())

  test("service lens: get, put-get, get-put, put-put, siblings untouched, andThen") {
    assertEquals(dbL.get(env), db)
    assertEquals(dbL.get(dbL.replace(NDb("x", 1))(env)), NDb("x", 1))
    val rt = dbL.replace(dbL.get(env))(env)
    assertEquals((rt.get[NDb], rt.get[NMetrics]), (db, metrics))
    assertEquals(dbL.replace(NDb("b", 2))(dbL.replace(NDb("a", 1))(env)).get[NDb], NDb("b", 2))
    assertEquals(dbL.replace(NDb("x", 1))(env).get[NMetrics], metrics)
    assertEquals(dbL.andThen(urlL).modify(_ + "/prod")(env).get[NDb], NDb("jdbc:h2/prod", 4))
  }

  test("Ref focus ops, Exit prism givens, layer projection") {
    val program = for
      ref <- Ref.make(db)
      _ <- ref.updateFocus[String](_.toUpperCase)
      _ <- ref.setFocus(8)
      url <- ref.getFocus[String]
      d <- ref.get
    yield (url, d)
    assertEquals(run(program), ("JDBC:H2", NDb("JDBC:H2", 8)))
    def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)
    assertEquals(bump(Exit.succeed(1)), Exit.succeed(2))
    assertEquals(bump(Exit.fail("boom"): Exit[String, Int]), Exit.fail("boom"))
    assertEquals(
      run(ZIO.service[String].provideLayer(ZLayer.succeed(db) >>> focusLayer[NDb, String])),
      "jdbc:h2"
    )
  }

  given [A: Arbitrary]: Arbitrary[Chunk[A]] =
    Arbitrary(Gen.listOf(Arbitrary.arbitrary[A]).map(Chunk.fromIterable))

  given [A: Cogen]: Cogen[Chunk[A]] = Cogen[List[A]].contramap(_.toList)
  given [A]: cats.Eq[Chunk[A]] = cats.Eq.fromUniversalEquals
  given cats.Functor[Chunk] = Chunks.chunkTraverse

  checkAll(
    "Traverse[Chunk] (the adapter behind Chunks.each)",
    TraverseTests[Chunk](using Chunks.chunkTraverse).traverse[Int, Int, Int, Int, Option, Option],
  )

  checkAll(
    "Traversal[Chunk, Int] via Chunks.each",
    new TraversalTests[Chunk, Int]:
      val laws: TraversalLaws[Chunk, Int] = new TraversalLaws[Chunk, Int]:
        val traversal = Chunks.each[Int, Int]
    .traversal,
  )

// CPD-ON
