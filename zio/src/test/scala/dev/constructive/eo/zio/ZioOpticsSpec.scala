package dev.constructive.eo
package zio

import _root_.zio.{Ref, Runtime, Unsafe, ZEnvironment, ZIO, ZLayer}
import org.specs2.mutable.Specification

import optics.Lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

class ZioOpticsSpec extends Specification:

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

  "service lens" should {
    "get the tagged slot" >> (dbL.get(env) === db)
    "put-get" >> (dbL.get(dbL.replace(Db("x", 1))(env)) === Db("x", 1))
    "get-put observes identically" >> {
      val rt = dbL.replace(dbL.get(env))(env)
      (rt.get[Db] === db).and(rt.get[Metrics] === metrics)
    }
    "put-put" >> {
      val twice = dbL.replace(Db("b", 2))(dbL.replace(Db("a", 1))(env))
      twice.get[Db] === Db("b", 2)
    }
    "leave sibling services untouched" >> {
      dbL.replace(Db("x", 1))(env).get[Metrics] === metrics
    }
    "drill into a service field via andThen" >> {
      val urlInEnv = dbL.andThen(urlL)
      val out = urlInEnv.modify(_ + "/prod")(env)
      (out.get[Db] === Db("jdbc:h2/prod", 4)).and(out.get[Metrics] === metrics)
    }
  }

  "Ref focus ops" should {
    "getFocus / updateFocus / setFocus through capabilities" >> {
      val program = for
        ref <- Ref.make(db)
        _ <- ref.updateFocus[String](_.toUpperCase)
        _ <- ref.setFocus(8)
        url <- ref.getFocus[String]
        d <- ref.get
      yield (url, d)
      run(program) === (("JDBC:H2", Db("JDBC:H2", 8)))
    }
  }

  "layer projection" should {
    "focusLayer derives the sub-service from the aggregate" >> {
      val out = ZIO.service[String].provideLayer(ZLayer.succeed(db) >>> focusLayer[Db, String])
      run(out) === "jdbc:h2"
    }
    "serviceFocus reads a focus of the S service" >> {
      run(serviceFocus[Db, String].provideLayer(ZLayer.succeed(db))) === "jdbc:h2"
    }
  }
