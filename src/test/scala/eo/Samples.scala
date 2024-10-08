package eo

import data._
import optics._

import cats.instances.list._
import cats.syntax.either._
import eo.data

case class UP(a: Int, b: Boolean)

@main def samples =

  import Lens.given
  import Optic.*
  import Forgetful.given

  val aLens = Lens[UP, Int](_.a, (s, b) => s.copy(a = b))
  val bLens = Lens.second[Int, Boolean]
  val cLens = Lens.first[Int, Boolean]

  given upCompositionEvidence: (UP => (aLens.X, Int)) with
      def apply(up: UP): (aLens.X, Int) = (up, up.a)

  val aPrism =
    Prism.optional[UP, Int](u => Some(u.a).filter(_ % 2 == 0), UP(_, false))

  val mod = aLens.modifyF(i => if (i % 2 == 0) None else Some(i + 1))
  val bm = bLens.replace(false)
  val split = aPrism.modifyA(i => if (i % 4 == 0) None else Some(i + 3))
  val atransfer = aLens.transfer((d: Double) => d.toInt)
  val bplace = bLens.place(true)
  val cplace  = cLens.place(56)

  val t = Traversal.each[List, Int, Int]

  println(mod(UP(1, false)))
  println(split(UP(4, false)))
  println(bm((3, true)))
  println(atransfer(UP(1, false))(6.0))
  println(bplace((1, false)))
  println(cplace((1, false)))

  val fd: (Int => Int) => Double => Double =
    f => d => f(d.toInt).toDouble

  val l = List(1, 2, 3)
  println(t.modify(_ + 1)(l))
  println(t.modifyA(a => (a % 2).asRight[String])(l))
  println(Fold.select[Int](_ % 2 == 0).to(4))
  println(Setter[Double, Double, Int, Int](fd).modify(_ + 1)(1.23))
