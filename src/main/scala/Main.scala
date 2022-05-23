package eo

import cats.instances.list._
import cats.syntax.either._

case class UP(a: Int, b: Boolean)

@main def main = {

  val aLens = Lens[UP, UP, Int, Int](_.a, (s, b) => s.copy(a = b))
  val bLens = Lens.second[Int, Boolean]

  val aPrism =
    Prism.optional[UP, Int, Int](u => Some(u.a).filter(_ % 2 == 0), UP(_, false))

  val mod = aLens.modifyF(i => if (i % 2 == 0) None else Some(i + 1))
  val bm = bLens.replace(false)
  val split = aPrism.modifyA(i => if (i % 4 == 0) None else Some(i + 3))

  val t = Traversal.each[List, Int]

  println(mod(UP(1, false)))
  println(split(UP(4, false)))
  println(bm((3, true)))

  val l = List(1, 2, 3)
  println(t.all(l))
  println(t.modify(_ + 1)(l))
  println(t.modifyA( a => (a % 2).asRight[String])(l))
}
