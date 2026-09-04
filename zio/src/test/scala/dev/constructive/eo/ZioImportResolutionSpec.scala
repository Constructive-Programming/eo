package dev.constructive.eo

import _root_.zio.stm.TRef
import _root_.zio.{Ref, Runtime, Unsafe, ZIO}
import dev.constructive.eo.zio.*
import org.specs2.mutable.Specification

import optics.Lens

/** Pins that the `Ref` and `TRef` focus-op extension groups stay jointly resolvable at a normal
  * IMPORT site (wildcard import from outside the package, explicit type args, explicit
  * `(using myLens)`) — the in-package specs cannot catch this class of breakage: same-name
  * extensions declared in different files don't form one overload group under an import, and the
  * failure only shows at consumer call sites (it broke the docs build first).
  */
class ZioImportResolutionSpec extends Specification:

  case class Cfg(url: String, pool: Int)
  val urlL = Lens[Cfg, String](_.url, (c, u) => c.copy(url = u))

  private def run[A](io: ZIO[Any, Any, A]): A =
    Unsafe.unsafe(implicit u => Runtime.default.unsafe.run(io).getOrThrowFiberFailure())

  "focus ops at an import site" should {
    "resolve the Ref overload with explicit type args and explicit using" >> {
      val program = for
        ref <- Ref.make(Cfg("jdbc:h2", 4))
        _ <- ref.updateFocus[String](_.toUpperCase)(using urlL)
        out <- ref.get
      yield out.url
      run(program) === "JDBC:H2"
    }
    "resolve the TRef overload the same way" >> {
      val program = for
        ref <- TRef.make(Cfg("jdbc:h2", 4)).commit
        _ <- ref.updateFocus[String](_.toUpperCase)(using urlL).commit
        out <- ref.get.commit
      yield out.url
      run(program) === "JDBC:H2"
    }
  }
