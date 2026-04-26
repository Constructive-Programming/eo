package dev.constructive.eo

import cats.instances.function.given
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.Grate
import data.Grate.given
import optics.Optic
import optics.Optic.*

import scala.language.implicitConversions

/** In-core smoke spec for the Grate carrier. Pins down the generic `Grate.apply[F: Representable]`
  * factory on `Function1[Boolean, *]` — the Naperian "record-of-pairs" shape that every
  * distributive-optic tutorial reaches for first.
  *
  * '''2026-04-25 consolidation.''' 11 → 5 named blocks. Pre-image had 4 separate Function1[Boolean,
  * Int] specs (modify pointwise, replace broadcast, identity-law, composition-law) collapsed into
  * one composite. Three different tuple arities (Tuple2/3/4) collapsed into one arity-parametric
  * test. Composer specs left as-is (one identity + one non-trivial bijection).
  */
class GrateSpec extends Specification with ScalaCheck:

  // covers: modify applies the function pointwise at every slot (Function1 shape),
  // replace broadcasts the constant to every slot, modify identity is identity (G1),
  // modify composes (G2)
  "Grate.apply[Function1[Boolean, *], Int] — modify pointwise / replace broadcast / G1 / G2" >> {
    val g: Optic[Boolean => Int, Boolean => Int, Int, Int, Grate] =
      Grate[[a] =>> Boolean => a, Int]

    val modPointwise = forAll { (t: Int, f: Int) =>
      val fn: Boolean => Int = b => if b then t else f
      val doubled = g.modify(_ * 2)(fn)
      doubled(true) == t * 2 && doubled(false) == f * 2
    }
    val replaceBroadcast = forAll { (t: Int, f: Int, b: Int) =>
      val fn: Boolean => Int = bb => if bb then t else f
      val replaced = g.replace(b)(fn)
      replaced(true) == b && replaced(false) == b
    }
    val g1 = forAll { (t: Int, f: Int, bb: Boolean) =>
      val fn: Boolean => Int = b => if b then t else f
      g.modify(identity[Int])(fn)(bb) == fn(bb)
    }
    val g2 = forAll { (t: Int, f: Int, bb: Boolean) =>
      val fn: Boolean => Int = b => if b then t else f
      val g1f: Int => Int = _ + 1
      val g2f: Int => Int = _ * 3
      g.modify(g2f)(g.modify(g1f)(fn))(bb) == g.modify(g1f.andThen(g2f))(fn)(bb)
    }
    modPointwise && replaceBroadcast && g1 && g2
  }

  // covers: Grate.tuple[Tuple2/3/4]: modify per-slot, replace broadcasts to every
  // slot, modify identity is identity. The arity-3 case is the canonical "record"
  // shape; arity-2 and arity-4 witness that the Tuple syntax derivation handles
  // varying widths uniformly.
  "Grate.tuple at arities 2/3/4: modify per-slot + replace broadcast + identity" >> {
    val g2 = Grate.tuple[(Int, Int), Int]
    val g3 = Grate.tuple[(Int, Int, Int), Int]
    val g4 = Grate.tuple[(Int, Int, Int, Int), Int]

    val a2 = forAll { (a: Int, b: Int) =>
      g2.modify((x: Int) => x * 2)((a, b)) == ((a * 2, b * 2))
    }
    val a3 = forAll { (a: Int, b: Int, c: Int) =>
      g3.modify((x: Int) => x + 1)((a, b, c)) == ((a + 1, b + 1, c + 1))
    }
    val a3replace = forAll { (a: Int, b: Int, c: Int, r: Int) =>
      g3.replace(r)((a, b, c)) == ((r, r, r))
    }
    val a3identity = forAll { (a: Int, b: Int, c: Int) =>
      g3.modify(identity[Int])((a, b, c)) == ((a, b, c))
    }
    val a4 = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g4.modify((x: Int) => -x)((a, b, c, d)) == ((-a, -b, -c, -d))
    }
    a2 && a3 && a3replace && a3identity && a4
  }

  // covers: compose iso.andThen(grate.tuple) with identity iso, compose
  // iso.andThen(grate.tuple) with non-trivial bijection
  "Composer[Forgetful, Grate]: identity iso and non-trivial bijection both compose cleanly" >> {
    import optics.Iso
    val triple = Grate.tuple[(Int, Int, Int), Int]

    val idIso = Iso[(Int, Int, Int), (Int, Int, Int), (Int, Int, Int), (Int, Int, Int)](
      identity,
      identity,
    )
    val composedId = idIso.andThen(triple)
    val idOk = forAll { (a: Int, b: Int, c: Int) =>
      composedId.modify((x: Int) => x + 1)((a, b, c)) == ((a + 1, b + 1, c + 1))
    }

    val rotate = Iso[(Int, Int, Int), (Int, Int, Int), (Int, Int, Int), (Int, Int, Int)](
      t => (t._2, t._3, t._1),
      t => (t._3, t._1, t._2),
    )
    val composedRot = rotate.andThen(triple)
    val rotOk = forAll { (a: Int, b: Int, c: Int) =>
      composedRot.modify((x: Int) => x + 1)((a, b, c)) == ((a + 1, b + 1, c + 1))
    }

    idOk && rotOk
  }
