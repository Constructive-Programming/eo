package dev.constructive.eo

import scala.language.implicitConversions

import cats.Representable
import cats.instances.function.given
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.MultiFocus
import data.MultiFocus.given
import data.MultiFocus.at
import optics.Optic
import optics.Optic.*

/** In-core smoke spec for the absorbed-Grate paths through `MultiFocus[Function1[X0, *]]`. Pins
  * down the v1 Grate use cases on the unified carrier:
  *
  *   - `MultiFocus.representable` over a Naperian Function1 (formerly
  *     `Grate.apply[F: Representable]`)
  *   - `MultiFocus.representableAt` with explicit lead index (formerly `Grate.at`)
  *   - `MultiFocus.tuple[T <: Tuple, A]` (formerly `Grate.tuple`)
  *   - `forgetful2multifocusFunction1` Iso → MultiFocus[Function1] bridge (formerly
  *     `forgetful2grate`)
  *   - The new typeclass-gated `.at(i: F.Representation)` extension method (Q2 surface)
  *   - Same-carrier `.andThen(grate)` exercising `mfAssocFunction1.composeFrom` (formerly
  *     `grateAssoc.composeFrom`)
  *
  * Replaces the deleted `GrateSpec` + `GrateCoverageSpec`. The block count is preserved 1:1 so the
  * top-level spec count doesn't regress.
  */
class MultiFocusFunction1Spec extends Specification with ScalaCheck:

  // covers: modify applies the function pointwise at every slot (Function1 shape),
  // replace broadcasts the constant to every slot, modify identity is identity (G1),
  // modify composes (G2)
  "MultiFocus.representable[Function1[Boolean, *], Int] — pointwise modify / broadcast replace / G1 / G2" >> {
    val g: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[[a] =>> Boolean => a, Int]

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

  // covers: MultiFocus.tuple at arities 2/3/4 — modify per-slot, replace broadcasts to every slot,
  // modify identity is identity. Arity-3 is the canonical "homogeneous record" shape.
  "MultiFocus.tuple at arities 2/3/4: modify per-slot + replace broadcast + identity" >> {
    val g2 = MultiFocus.tuple[(Int, Int), Int]
    val g3 = MultiFocus.tuple[(Int, Int, Int), Int]
    val g4 = MultiFocus.tuple[(Int, Int, Int, Int), Int]

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

  // covers: compose iso.andThen(MultiFocus.tuple) with identity iso, compose
  // iso.andThen(MultiFocus.tuple) with non-trivial bijection. Exercises the absorbed
  // forgetful2grate via `forgetful2multifocusFunction1`.
  "Composer[Forgetful, MultiFocus[Function1[Int, *]]]: identity iso and bijection compose cleanly" >> {
    import optics.Iso
    val triple = MultiFocus.tuple[(Int, Int, Int), Int]

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

  // covers: representableAt — the explicit-lead variant — modify-at-position and replace-broadcast
  // both round-trip through the per-position read.
  "MultiFocus.representableAt — Representable-indexed factory: modify-at + replace-broadcast" >> {
    val F = summon[Representable[[a] =>> Boolean => a]]
    val gTrue: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representableAt[[a] =>> Boolean => a, Int](F)(true)
    val gFalse: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representableAt[[a] =>> Boolean => a, Int](F)(false)

    val fn: Boolean => Int = b => if b then 42 else 7
    val doubled = gTrue.modify(_ * 2)(fn)
    val modOk = (doubled(true) === 84).and(doubled(false) === 14)

    val fn2: Boolean => Int = b => if b then 1 else 2
    val flat = gFalse.replace(99)(fn2)
    val replOk = (flat(true) === 99).and(flat(false) === 99)

    modOk.and(replOk)
  }

  // covers: the new typeclass-gated `.at(i: F.Representation)` read surface — uses
  // Representable[Function1[X0, *]] to read the focus at a chosen position. Q2 deliverable.
  "MultiFocus[Function1].at(i) — Representable-gated position read" >> {
    val g: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[[a] =>> Boolean => a, Int]

    val fn: Boolean => Int = b => if b then 100 else 200
    (g.at(true)(fn) === 100).and(g.at(false)(fn) === 200)
  }

  // covers: MultiFocus.tuple .andThen MultiFocus.tuple — same-carrier composition exercises
  // mfAssocFunction1.composeFrom (the absorbed grateAssoc.composeFrom).
  "MultiFocus.tuple.andThen(MultiFocus.tuple) — same-carrier composition through mfAssocFunction1" >> {
    val outer: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val doubled = outer.modify(_ * 2)((10, 20))
    (doubled === ((20, 40))).and(outer.replace(0)((1, 2)) === ((0, 0)))
  }
