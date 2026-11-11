package dev.constructive.eo
package kyo

import _root_.kyo.*

import optics.Lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

class KyoOpticsSuite extends munit.FunSuite:

  val db = Db("jdbc:h2", 4)
  val metrics = Metrics("eo")
  val tm = TypeMap(db, metrics)

  val dbL = service[Db & Metrics, Db]
  val urlL = Lens[Db, String](_.url, (d, u) => d.copy(url = u))
  val poolL = Lens[Db, Int](_.pool, (d, p) => d.copy(pool = p))

  given CanGet[Db, String] = urlL
  given CanModify[Db, String] = urlL
  given CanModify[Db, Int] = poolL

  test("service lens over TypeMap: get the tagged slot") {
    assertEquals(dbL.get(tm), db)
  }
  test("service lens over TypeMap: put-get") {
    assertEquals(dbL.get(dbL.replace(Db("x", 1))(tm)), Db("x", 1))
  }
  test("service lens over TypeMap: get-put observes identically") {
    val rt = dbL.replace(dbL.get(tm))(tm)
    assertEquals(rt.get[Db], db)
    assertEquals(rt.get[Metrics], metrics)
  }
  test("service lens over TypeMap: put-put") {
    assertEquals(dbL.replace(Db("b", 2))(dbL.replace(Db("a", 1))(tm)).get[Db], Db("b", 2))
  }
  test("service lens over TypeMap: leave sibling slots untouched") {
    assertEquals(dbL.replace(Db("x", 1))(tm).get[Metrics], metrics)
  }
  test("service lens over TypeMap: drill into a service field via andThen") {
    val out = dbL.andThen(urlL).modify(_ + "/prod")(tm)
    assertEquals(out.get[Db], Db("jdbc:h2/prod", 4))
    assertEquals(out.get[Metrics], metrics)
  }

  test("Env.focus reads a focus of the environment service") {
    assertEquals(Env.run(db)(Env.focus[Db, String]).eval, "jdbc:h2")
  }

  test("Var focus ops: getFocus / updateFocus / setFocus through capabilities") {
    val prog =
      for
        _ <- Var.updateFocus[Db, String](_.toUpperCase)
        _ <- Var.setFocus[Db, Int](8)
        url <- Var.getFocus[Db, String]
      yield url
    assertEquals(Var.runTuple(db)(prog).eval, (Db("JDBC:H2", 8), "JDBC:H2"))
  }

  test("automatic capability givens serve generic code on TypeMap") {
    def prefix[T](t: T)(using g: CanGet[T, Metrics]): String = g.get(t).prefix
    def resize[T](t: T)(using m: CanModify[T, Db]): T = m.modify(_.copy(pool = 9))(t)
    assertEquals(prefix(tm), "eo")
    assertEquals(resize(tm).get[Db].pool, 9)
  }
  test("automatic capability givens serve Maybe and Result prisms, misses passing through") {
    def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)
    assertEquals(bump(Maybe(1)), Maybe(2))
    assertEquals(bump(Maybe.empty[Int]), Maybe.empty[Int])
    assertEquals(bump(Result.succeed(1): Result[String, Int]), Result.succeed(2))
    assertEquals(bump(Result.fail("e"): Result[String, Int]), Result.fail("e"))
    assertEquals(summon[CanGetOption[Maybe[Int], Int]].getOption(Maybe(5)), Some(5))
  }

  test("Layer.focus derives the sub-service from the aggregate") {
    val out = Env.run(db)(Memo.run(Layer.focus[Db, String].run)).eval
    assertEquals(out.get[String], "jdbc:h2")
  }
  test("Layer.focus wires through Env.runLayer") {
    val prog = Env.runLayer(Layer(db), Layer.focus[Db, String])(Env.get[String])
    assertEquals(Memo.run(prog).eval, "jdbc:h2")
  }
