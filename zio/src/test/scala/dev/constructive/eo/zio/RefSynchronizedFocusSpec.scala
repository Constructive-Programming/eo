package dev.constructive.eo
package zio

import _root_.zio.{Ref, Runtime, Unsafe, ZIO}
import cats.Applicative
import org.specs2.mutable.Specification

import optics.{Lens, Prism, Traversal}

/** `Ref.Synchronized.updateFocusZIO` — the effectful focus rewrite.
  *
  * The `Applicative[ZIO[R, E, *]]` normally comes from zio-interop-cats; this module declares none
  * (coherence: interop-cats owns that instance), so the spec hand-writes a local one rather than
  * pulling the dependency in just to test the plumbing.
  */
class RefSynchronizedFocusSpec extends Specification:

  private given zioApplicative[R, E]: Applicative[[x] =>> ZIO[R, E, x]] with
    def pure[A](a: A): ZIO[R, E, A] = ZIO.succeed(a)

    def ap[A, B](ff: ZIO[R, E, A => B])(fa: ZIO[R, E, A]): ZIO[R, E, B] =
      ff.flatMap(f => fa.map(f))

  private def run[A](io: ZIO[Any, Any, A]): A =
    Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(io).getOrThrowFiberFailure())

  val urlL = Lens[Db, String](_.url, (d, u) => d.copy(url = u))

  "updateFocusZIO" should {
    "run the effect at the focus and write the result back atomically" >> {
      given optics.GetReplaceLens[Db, Db, String, String] = urlL
      val program = for
        ref <- Ref.Synchronized.make(Db("jdbc:h2", 4))
        _ <- ref.updateFocusZIO[Any, Nothing, String](u => ZIO.succeed(u.toUpperCase))
        out <- ref.get
      yield out
      run(program) === Db("JDBC:H2", 4)
    }
    "propagate the effect's failure and leave the ref untouched" >> {
      given optics.GetReplaceLens[Db, Db, String, String] = urlL
      val program = for
        ref <- Ref.Synchronized.make(Db("jdbc:h2", 4))
        res <- ref.updateFocusZIO[Any, String, String](_ => ZIO.fail("boom")).either
        out <- ref.get
      yield (res, out)
      run(program) === ((Left("boom"), Db("jdbc:h2", 4)))
    }
    "never run the effect through a missed prism" >> {
      val hitP = Prism.optional[Either[String, Int], Int](_.toOption, Right(_))
      given optics.PickMendPrism[Either[String, Int], Int, Int] = hitP
      val program = for
        ref <- Ref.Synchronized.make(Left("no int"): Either[String, Int])
        _ <- ref.updateFocusZIO[Any, Nothing, Int](i => ZIO.succeed(i + 1))
        out <- ref.get
      yield out
      run(program) === Left("no int")
    }
    "sequence one effect per focus through a traversal" >> {
      given Traversal[List[Int], List[Int], Int, Int] = Traversal.each
      val program = for
        ref <- Ref.Synchronized.make(List(1, 2, 3))
        _ <- ref.updateFocusZIO[Any, Nothing, Int](i => ZIO.succeed(i * 10))
        out <- ref.get
      yield out
      run(program) === List(10, 20, 30)
    }
  }
