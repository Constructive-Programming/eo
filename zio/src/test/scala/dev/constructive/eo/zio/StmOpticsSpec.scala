package dev.constructive.eo
package zio

import _root_.zio.stm.{STM, TMap, TRef}
import _root_.zio.{Runtime, Unsafe, ZIO}
import org.specs2.mutable.Specification

import optics.Lens

class StmOpticsSpec extends Specification:

  val urlL = Lens[Db, String](_.url, (d, u) => d.copy(url = u))
  val poolL = Lens[Db, Int](_.pool, (d, p) => d.copy(pool = p))

  given CanModify[Db, String] = urlL
  given CanGet[Db, String] = urlL
  given CanModify[Db, Int] = poolL

  private def run[A](io: ZIO[Any, Any, A]): A =
    Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(io).getOrThrowFiberFailure())

  "TRef focus ops" should {
    "read and rewrite foci within a transaction" >> {
      val program = for
        ref <- TRef.make(Db("jdbc:h2", 4)).commit
        // explicit `(using urlL)` pins that the Ref/TRef overload pair stays resolvable
        _ <- ref.updateFocus[String](_.toUpperCase)(using urlL).commit
        _ <- ref.setFocus(8).commit
        url <- ref.getFocus[String].commit
        d <- ref.get.commit
      yield (url, d)
      run(program) === (("JDBC:H2", Db("JDBC:H2", 8)))
    }
    "compose focused updates across TRefs into ONE atomic transaction" >> {
      val program = for
        primary <- TRef.make(Db("a", 1)).commit
        replica <- TRef.make(Db("b", 1)).commit
        _ <- STM
          .atomically(primary.updateFocus[Int](_ + 9) *> replica.updateFocus[Int](_ + 9))
        p <- primary.get.commit
        r <- replica.get.commit
      yield (p.pool, r.pool)
      run(program) === ((10, 10))
    }
  }

  "TMap focus ops" should {
    "rewrite the focus at a present key, absent keys passing through" >> {
      val program = for
        m <- TMap.make(("k", Db("jdbc:h2", 4))).commit
        _ <- m.updateFocusAt[Int]("k")(_ * 2).commit
        _ <- m.updateFocusAt[Int]("nope")(_ * 2).commit
        hit <- m.getFocusAt[String]("k").commit
        miss <- m.getFocusAt[String]("nope").commit
        size <- m.size.commit
      yield (hit, miss, size)
      run(program) === ((Some("jdbc:h2"), None, 1))
    }
  }
