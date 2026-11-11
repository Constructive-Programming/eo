// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo
package kyo

import _root_.kyo.*

import optics.Lens

final case class NDb(url: String, pool: Int)
final case class NMetrics(prefix: String)

/** Scala Native twin of `KyoOpticsSpec` (specs2 has no Native artifact). */
class NativeKyoSuite extends munit.FunSuite:

  private val db = NDb("jdbc:h2", 4)
  private val metrics = NMetrics("eo")
  private val tm = TypeMap(db, metrics)

  private val dbL = service[NDb & NMetrics, NDb]
  private val urlL = Lens[NDb, String](_.url, (d, u) => d.copy(url = u))

  given CanGet[NDb, String] = urlL
  given CanModify[NDb, String] = urlL
  given CanModify[NDb, Int] = Lens[NDb, Int](_.pool, (d, p) => d.copy(pool = p))

  test("service lens over TypeMap: get, put-get, get-put, put-put, siblings, andThen") {
    assertEquals(dbL.get(tm), db)
    assertEquals(dbL.get(dbL.replace(NDb("x", 1))(tm)), NDb("x", 1))
    val rt = dbL.replace(dbL.get(tm))(tm)
    assertEquals((rt.get[NDb], rt.get[NMetrics]), (db, metrics))
    assertEquals(dbL.replace(NDb("b", 2))(dbL.replace(NDb("a", 1))(tm)).get[NDb], NDb("b", 2))
    assertEquals(dbL.replace(NDb("x", 1))(tm).get[NMetrics], metrics)
    assertEquals(dbL.andThen(urlL).modify(_ + "/prod")(tm).get[NDb], NDb("jdbc:h2/prod", 4))
  }

  test("Env.focus, Var focus ops, Layer.focus") {
    assertEquals(Env.run(db)(Env.focus[NDb, String]).eval, "jdbc:h2")
    val prog =
      for
        _ <- Var.updateFocus[NDb, String](_.toUpperCase)
        _ <- Var.setFocus[NDb, Int](8)
        url <- Var.getFocus[NDb, String]
      yield url
    assertEquals(Var.runTuple(db)(prog).eval, (NDb("JDBC:H2", 8), "JDBC:H2"))
    val out = Env.run(db)(Memo.run(Layer.focus[NDb, String].run)).eval
    assertEquals(out.get[String], "jdbc:h2")
  }

  test("Maybe / Result prisms serve capability code, misses passing through") {
    def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)
    assertEquals(bump(Maybe(1)), Maybe(2))
    assertEquals(bump(Maybe.empty[Int]), Maybe.empty[Int])
    assertEquals(bump(Result.succeed(1): Result[String, Int]), Result.succeed(2))
    assertEquals(bump(Result.fail("e"): Result[String, Int]), Result.fail("e"))
  }

// CPD-ON
