package dev.constructive.eo

import cats.instances.int.given
import cats.instances.list.given
import org.specs2.mutable.Specification

import optics.{At, Each, Index, Iso, Traversal}
import laws.{LensLaws, OptionalLaws}
import laws.discipline.{LensTests, OptionalTests}

/** Law + behaviour coverage for the Monocle-named container constructors — [[optics.Each]],
  * [[optics.Index]], [[optics.At]] — and the [[optics.Iso]] type alias. The objects are thin
  * constructors over already-law-tested classes (`Traversal.each`, the 4-param `Optional`,
  * `GetReplaceLens`), so the law suites here pin the INSTANCES (Index's no-insert write, At's
  * upsert/delete lens) rather than new machinery. Monocle-semantics deltas asserted explicitly:
  * `Index` never inserts; `At` replacing `None` deletes the key.
  */
class ContainerOpticsSpec extends Specification with CheckAllHelpers:

  // ----- laws -----------------------------------------------------

  checkAll(
    "Index[Vector, Int](1) — positional Optional",
    new OptionalTests[Vector[Int], Int]:
      val laws = new OptionalLaws[Vector[Int], Int]:
        val optional = Index[Vector, Int](1)
    .optional,
  )

  checkAll(
    "Index[String, Int](\"k\") — keyed Optional",
    new OptionalTests[Map[String, Int], Int]:
      val laws = new OptionalLaws[Map[String, Int], Int]:
        val optional = Index[String, Int]("k")
    .optional,
  )

  checkAll(
    "At[String, Int](\"k\") — Map lens to Option[V]",
    new LensTests[Map[String, Int], Option[Int]]:
      val laws = new LensLaws[Map[String, Int], Option[Int]]:
        val lens = At[String, Int]("k")
    .lens,
  )

  // ----- Monocle-semantics behaviour pins -------------------------

  "Index (seq): hit reads and writes in place; miss (OOB) never inserts" >> {
    val second = Index[Vector, Int](1)
    (second.getOption(Vector(1, 2, 3)) === Some(2))
      .and(second.replace(9)(Vector(1, 2, 3)) === Vector(1, 9, 3))
      .and(second.replace(9)(Vector(1)) === Vector(1))
      .and(second.getOption(Vector.empty[Int]) === None)
  }

  "Index (map): hit writes existing key; missing key passes through — no insert" >> {
    val port = Index[String, Int]("port")
    (port.replace(8080)(Map("port" -> 80)) === Map("port" -> 8080))
      .and(port.replace(8080)(Map("host" -> 1)) === Map("host" -> 1))
  }

  "At: Some upserts (insert AND update), None deletes; get is total" >> {
    val port = At[String, Int]("port")
    (port.get(Map.empty[String, Int]) === None)
      .and(port.replace(Some(80))(Map.empty[String, Int]) === Map("port" -> 80))
      .and(port.replace(Some(81))(Map("port" -> 80)) === Map("port" -> 81))
      .and(port.replace(None)(Map("port" -> 80, "host" -> 1)) === Map("host" -> 1))
  }

  "Each ≡ Traversal.each — same foldMap and modify behaviour" >> {

    // The concrete family class is ascribable — constructors return `Traversal`, not a bare Optic.
    val viaEach: Traversal[List[Int], List[Int], Int, Int] = Each[List, Int]
    val viaTraversal = Traversal.each[List, Int]
    (viaEach.foldMap(identity[Int])(List(1, 2, 3)) === 6)
      .and(viaEach.modify(_ + 1)(List(1, 2)) === viaTraversal.modify(_ + 1)(List(1, 2)))
  }

  "Iso[S, A] alias names the concrete BijectionIso — ascription keeps the fused surface" >> {
    val swap: Iso[(Int, String), (String, Int)] =
      Iso[(Int, String), (Int, String), (String, Int), (String, Int)](_.swap, _.swap)
    (swap.get((1, "a")) === (("a", 1)))
      .and(swap.reverseGet(("a", 1)) === ((1, "a")))
  }
