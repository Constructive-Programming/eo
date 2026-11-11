package dev.constructive.eo
package zio

import _root_.zio.{Exit, Ref, Runtime, Unsafe, ZEnvironment, ZIO, ZLayer}

import optics.Lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

class ZioOpticsSuite extends munit.FunSuite:

  val db = Db("jdbc:h2", 4)
  val metrics = Metrics("eo")
  val env = ZEnvironment(db).add(metrics)

  val dbL = service[Db & Metrics, Db]
  val urlL = Lens[Db, String](_.url, (d, u) => d.copy(url = u))
  val poolL = Lens[Db, Int](_.pool, (d, p) => d.copy(pool = p))

  given CanGet[Db, String] = urlL
  given CanModify[Db, String] = urlL
  given CanModify[Db, Int] = poolL

  private def run[A](io: ZIO[Any, Any, A]): A =
    Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(io).getOrThrowFiberFailure())

  test("service lens: get the tagged slot") {
    assertEquals(dbL.get(env), db)
  }
  test("service lens: put-get") {
    assertEquals(dbL.get(dbL.replace(Db("x", 1))(env)), Db("x", 1))
  }
  test("service lens: get-put observes identically") {
    val rt = dbL.replace(dbL.get(env))(env)
    assertEquals(rt.get[Db], db)
    assertEquals(rt.get[Metrics], metrics)
  }
  test("service lens: put-put") {
    val twice = dbL.replace(Db("b", 2))(dbL.replace(Db("a", 1))(env))
    assertEquals(twice.get[Db], Db("b", 2))
  }
  test("service lens: leave sibling services untouched") {
    assertEquals(dbL.replace(Db("x", 1))(env).get[Metrics], metrics)
  }
  test("service lens: drill into a service field via andThen") {
    val out = dbL.andThen(urlL).modify(_ + "/prod")(env)
    assertEquals(out.get[Db], Db("jdbc:h2/prod", 4))
    assertEquals(out.get[Metrics], metrics)
  }

  test("Ref focus ops: getFocus / updateFocus / setFocus through capabilities") {
    val program = for
      ref <- Ref.make(db)
      _ <- ref.updateFocus[String](_.toUpperCase)
      _ <- ref.setFocus(8)
      url <- ref.getFocus[String]
      d <- ref.get
    yield (url, d)
    assertEquals(run(program), ("JDBC:H2", Db("JDBC:H2", 8)))
  }

  test("automatic capability givens serve generic code on ZEnvironment") {
    def prefix[T](t: T)(using g: CanGet[T, Metrics]): String = g.get(t).prefix
    def resize[T](t: T)(using m: CanModify[T, Db]): T = m.modify(_.copy(pool = 9))(t)
    assertEquals(prefix(env), "eo")
    assertEquals(resize(env).get[Db].pool, 9)
  }
  test("automatic capability givens serve the Exit success prism, failures passing through") {
    def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)
    assertEquals(bump(Exit.succeed(1)), Exit.succeed(2))
    assertEquals(bump(Exit.fail("boom"): Exit[String, Int]), Exit.fail("boom"))
    assertEquals(summon[CanGetOption[Exit[String, Int], Int]].getOption(Exit.succeed(3)), Some(3))
  }

  test("layer projection: focusLayer derives the sub-service from the aggregate") {
    val out = ZIO.service[String].provideLayer(ZLayer.succeed(db) >>> focusLayer[Db, String])
    assertEquals(run(out), "jdbc:h2")
  }
  test("layer projection: serviceFocus reads a focus of the S service") {
    assertEquals(run(serviceFocus[Db, String].provideLayer(ZLayer.succeed(db))), "jdbc:h2")
  }
