package eo

import cats.instances.function.given

import data.Grate
import data.Grate.given
import optics.Optic
import optics.Optic.*

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** In-core smoke spec for the Grate carrier. Pins down the generic `Grate.apply[F: Representable]`
  * factory on `Function1[Boolean, *]` — the Naperian "record-of-pairs" shape that every
  * distributive-optic tutorial reaches for first. The richer law suites live in `cats-eo-tests`.
  */
class GrateSpec extends Specification with ScalaCheck:

  "Grate.apply[Function1[Boolean, *], Int]" should {
    val g: Optic[Boolean => Int, Boolean => Int, Int, Int, Grate] =
      Grate[[a] =>> Boolean => a, Int]

    "modify applies the function pointwise at every slot (Function1 shape)" >> {
      forAll { (t: Int, f: Int) =>
        val fn: Boolean => Int = b => if b then t else f
        val doubled = g.modify(_ * 2)(fn)
        doubled(true) == t * 2 && doubled(false) == f * 2
      }
    }

    "replace broadcasts the constant to every slot" >> {
      forAll { (t: Int, f: Int, b: Int) =>
        val fn: Boolean => Int = bb => if bb then t else f
        val replaced = g.replace(b)(fn)
        replaced(true) == b && replaced(false) == b
      }
    }

    "modify identity is identity (G1, functor-identity)" >> {
      forAll { (t: Int, f: Int, bb: Boolean) =>
        val fn: Boolean => Int = b => if b then t else f
        g.modify(identity[Int])(fn)(bb) == fn(bb)
      }
    }

    "modify composes (G2, functor-composition)" >> {
      forAll { (t: Int, f: Int, bb: Boolean) =>
        val fn: Boolean => Int = b => if b then t else f
        val g1: Int => Int = _ + 1
        val g2: Int => Int = _ * 3
        g.modify(g2)(g.modify(g1)(fn))(bb) == g.modify(g1.andThen(g2))(fn)(bb)
      }
    }
  }
