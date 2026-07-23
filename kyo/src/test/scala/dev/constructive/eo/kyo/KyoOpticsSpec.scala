package dev.constructive.eo
package kyo

import _root_.kyo.*
import org.specs2.mutable.Specification

import optics.Lens

case class Db(url: String, pool: Int)
case class Metrics(prefix: String)

class KyoOpticsSpec extends Specification:

  val db = Db("jdbc:h2", 4)
  val metrics = Metrics("eo")
  val tm = TypeMap(db, metrics)

  val dbL = service[Db & Metrics, Db]
  val urlL = Lens[Db, String](_.url, (d, u) => d.copy(url = u))
  val poolL = Lens[Db, Int](_.pool, (d, p) => d.copy(pool = p))

  given CanGet[Db, String] = urlL
  given CanModify[Db, String] = urlL
  given CanModify[Db, Int] = poolL

  "service lens over TypeMap" should {
    "get the tagged slot" >> (dbL.get(tm) === db)
    "put-get" >> (dbL.get(dbL.replace(Db("x", 1))(tm)) === Db("x", 1))
    "get-put observes identically" >> {
      val rt = dbL.replace(dbL.get(tm))(tm)
      (rt.get[Db] === db).and(rt.get[Metrics] === metrics)
    }
    "put-put" >> {
      dbL.replace(Db("b", 2))(dbL.replace(Db("a", 1))(tm)).get[Db] === Db("b", 2)
    }
    "leave sibling slots untouched" >> {
      dbL.replace(Db("x", 1))(tm).get[Metrics] === metrics
    }
    "drill into a service field via andThen" >> {
      val urlInEnv = dbL.andThen(urlL)
      val out = urlInEnv.modify(_ + "/prod")(tm)
      (out.get[Db] === Db("jdbc:h2/prod", 4)).and(out.get[Metrics] === metrics)
    }
  }

  "Env.focus" should {
    "read a focus of the environment service" >> {
      Env.run(db)(Env.focus[Db, String]).eval === "jdbc:h2"
    }
  }

  "Var focus ops" should {
    "getFocus / updateFocus / setFocus through capabilities" >> {
      val prog =
        for
          _ <- Var.updateFocus[Db, String](_.toUpperCase)
          _ <- Var.setFocus[Db, Int](8)
          url <- Var.getFocus[Db, String]
        yield url
      Var.runTuple(db)(prog).eval === ((Db("JDBC:H2", 8), "JDBC:H2"))
    }
  }

  "automatic capability givens" should {
    "serve generic capability-consuming code on TypeMap" >> {
      def prefix[T](t: T)(using g: CanGet[T, Metrics]): String = g.get(t).prefix
      def resize[T](t: T)(using m: CanModify[T, Db]): T = m.modify(_.copy(pool = 9))(t)
      (prefix(tm) === "eo").and(resize(tm).get[Db].pool === 9)
    }
    "serve Maybe and Result prisms, misses passing through" >> {
      def bump[T](t: T)(using m: CanModify[T, Int]): T = m.modify(_ + 1)(t)
      (bump(Maybe(1)) === Maybe(2))
        .and(bump(Maybe.empty[Int]) === Maybe.empty[Int])
        .and(bump(Result.succeed(1): Result[String, Int]) === Result.succeed(2))
        .and(bump(Result.fail("e"): Result[String, Int]) === Result.fail("e"))
        .and(summon[CanGetOption[Maybe[Int], Int]].getOption(Maybe(5)) === Some(5))
    }
  }

  "Layer.focus" should {
    "derive the sub-service from the aggregate" >> {
      val out = Env.run(db)(Memo.run(Layer.focus[Db, String].run)).eval
      out.get[String] === "jdbc:h2"
    }
    "wire through Env.runLayer" >> {
      val prog = Env.runLayer(Layer(db), Layer.focus[Db, String])(Env.get[String])
      Memo.run(prog).eval === "jdbc:h2"
    }
  }
