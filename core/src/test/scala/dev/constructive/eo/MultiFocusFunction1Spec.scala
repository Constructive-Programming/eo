package dev.constructive.eo

import scala.language.implicitConversions

import cats.instances.function.given
import cats.{Functor, Representable}
import dev.constructive.eo.compose.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.MultiFocus
import data.MultiFocus.at
import optics.Iso
import optics.Optic
import optics.Optic.*

/** In-core smoke spec for the absorbed-Grate paths through `MultiFocus[Function1[X0, *]]`. Pins
  * down the v1 Grate use cases on the unified carrier:
  *
  *   - `MultiFocus.representable` over a Naperian Function1 (formerly
  *     `Grate.apply[F: Representable]`)
  *   - the `.at(i)` read surface standing in for the retired `representableAt` / `Grate.at`
  *     construction-time index, pinned against `F.index` on a permuted `Representable`
  *   - `MultiFocus.tuple[T <: Tuple, A]` (formerly `Grate.tuple`)
  *   - `forgetful2multifocusFunction1` Iso → MultiFocus[Function1] bridge (formerly
  *     `forgetful2grate`)
  *   - The new typeclass-gated `.at(i: F.Representation)` extension method (Q2 surface)
  *   - The `mfAssocFunction1` composition rules: the broadcast branch (an Iso inner, `grate ∘ iso`)
  *     rewrites every position from its own focus, the bundle branch (`iso ∘ grate`) takes the
  *     whole rebuild. See the two composition blocks below.
  *
  * Replaces the deleted `GrateSpec` + `GrateCoverageSpec`. The block count is preserved 1:1 so the
  * top-level spec count doesn't regress.
  */
class MultiFocusFunction1Spec extends Specification with ScalaCheck:

  /** Fixture for the non-tuple Grate sources: a function of a boxed focus. */
  case class Box[A](a: A)

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
  "Composer[Direct, MultiFocus[Function1[Int, *]]]: identity iso and bijection compose cleanly" >> {
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

  // A second Grate `Representable`, over a container whose index ORDER is a permutation of its
  // field order — so "index 0" and "first field" are different questions and a privileged / lead
  // position has nowhere to hide. Lawful: `Representable` asks for `index` / `tabulate` to be
  // mutually inverse, never for the index order to follow the declaration order.
  enum Slot:
    case S0, S1, S2

  case class Tri[A](a: A, b: A, c: A)

  given triFunctor: Functor[Tri] with
    def map[A, B](fa: Tri[A])(f: A => B): Tri[B] = Tri(f(fa.a), f(fa.b), f(fa.c))

  given triRepresentable: Representable.Aux[Tri, Slot] = new Representable[Tri]:
    type Representation = Slot
    def F: Functor[Tri] = triFunctor

    def index[A](fa: Tri[A]): Slot => A =
      case Slot.S0 => fa.c
      case Slot.S1 => fa.a
      case Slot.S2 => fa.b

    def tabulate[A](f: Slot => A): Tri[A] = Tri(f(Slot.S1), f(Slot.S2), f(Slot.S0))

  // covers: MultiFocus.representable + .at(i) — position is a READ-time argument
  //   (`g.at(i)(fa) == F.index(fa)(i)`; the typeclass-gated read surface that replaced
  //   `representableAt`'s construction-time index), pinned against `F.index` / the instance's own
  //   `map` for two `Representable`s, the second permuted; modify / replace stay pointwise — the
  //   property a lead-sampling rebuild would break (witnessed negatively by UnlawfulFixturesSpec)
  "MultiFocus.representable + .at(i): position read is index-parametric, modify stays pointwise" >> {
    val F = summon[Representable[[a] =>> Boolean => a]]
    val g: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[[a] =>> Boolean => a, Int]

    val fn: Boolean => Int = b => if b then 42 else 7
    val doubled = g.modify(_ * 2)(fn)
    val modOk = (doubled(true) === 84).and(doubled(false) === 14)

    val fn2: Boolean => Int = b => if b then 1 else 2
    val flat = g.replace(99)(fn2)
    val replOk = (flat(true) === 99).and(flat(false) === 99)

    val readFn: Boolean => Int = b => if b then 100 else 200
    val readOk = (g.at(true)(readFn) === F.index(readFn)(true))
      .and(g.at(false)(readFn) === F.index(readFn)(false))

    // Permuted instance: the optic must follow the INSTANCE's index order (S0 is the THIRD field),
    // and its write must stay pointwise — a rebuild that sampled one index would make every Slot
    // equal, so `modify` would stop agreeing with the instance's own `map`.
    val T = summon[Representable[Tri]]
    val gt: Optic[Tri[Int], Tri[Int], Int, Int, MultiFocus[Function1[Slot, *]]] =
      MultiFocus.representable[Tri, Int](using T)
    val tri = Tri(1, 2, 3) // S0 -> 3, S1 -> 1, S2 -> 2
    val triRead = (gt.at(Slot.S0)(tri) === 3)
      .and(gt.at(Slot.S1)(tri) === 1)
      .and(gt.at(Slot.S2)(tri) === 2)
    val triWrite = (gt.modify(_ * 10)(tri) === triFunctor.map(tri)(_ * 10))
      .and(gt.replace(0)(tri) === T.tabulate(_ => 0))
    val triOk = triRead.and(triWrite)

    modOk.and(replOk).and(readOk).and(triOk)
  }

  // covers: the mfAssocFunction1 BROADCAST branch — `grate ∘ iso` must rewrite every position from
  // its own written focus. Regression for the pre-fix collapse, which sampled the outer's rebuild
  // once and broadcast the result: read of a doubled (10,20,30) gave 20,20,20 (not 20,40,60) and
  // `modify(_ + 1)` gave (11,11,11). `tuple` / `representable` / `apply` are the three shipped
  // tabulating Grate factories — all three are pinned here because all three were affected.
  "grate ∘ iso positions: tuple / representable / apply rebuild each slot from its own focus" >> {
    val shift = Iso[Int, Int, Int, Int](_ + 1000, _ - 1000)

    val tupleIso = MultiFocus.tuple[(Int, Int, Int), Int].andThen(shift)
    val tupleRead =
      (tupleIso.at(0)((10, 20, 30)) === 1010)
        .and(tupleIso.at(1)((10, 20, 30)) === 1020)
        .and(tupleIso.at(2)((10, 20, 30)) === 1030)
    val tupleModify = tupleIso.modify(_ + 1)((10, 20, 30)) === ((11, 21, 31))
    val tupleReplaced = tupleIso.replace(7)((10, 20, 30))
    val tupleReplace =
      (tupleIso.at(0)(tupleReplaced) === 7).and(tupleIso.at(2)(tupleReplaced) === 7)

    val repIso = MultiFocus.representable[Function1[Int, *], Int].andThen(shift)
    val repSource: Int => Int = i => i * 10
    val repRead = (repIso.at(0)(repSource) === 1000).and(repIso.at(2)(repSource) === 1020)
    val repWritten = repIso.modify(_ + 1)(repSource)
    val repWrite = (repWritten(0) === 1).and(repWritten(1) === 11).and(repWritten(2) === 21)

    val appliedIso = MultiFocus
      .apply[Function1[Int, *], Box[Int]]
      .andThen(Iso[Box[Int], Box[Int], Int, Int](_.a, Box(_)))
    val appliedSource: Int => Box[Int] = i => Box(i * 10)
    val appliedWritten = appliedIso.modify(_ + 1)(appliedSource)
    val appliedWrite = (appliedWritten(0) === Box(1)).and(appliedWritten(2) === Box(21))

    tupleRead.and(tupleModify).and(tupleReplace).and(repRead).and(repWrite).and(appliedWrite)
  }

  // covers: the mfAssocFunction1 BUNDLE branch read rule — the inner's bundle is read at the index
  // the outer's element came from (the diagonal), not re-read from index 0. The write stays
  // bundle-level for a bundle inner: its `from` consumes the whole rebuild once.
  "grate ∘ grate reads the diagonal: element i of the outer's bundle at index i" >> {
    val composed = MultiFocus
      .apply[Function1[Int, *], Int => Int]
      .andThen(MultiFocus.apply[Function1[Int, *], Int])
    val source: Int => (Int => Int) = i => j => i * 100 + j

    (composed.at(0)(source) === 0)
      .and(composed.at(1)(source) === 101)
      .and(composed.at(2)(source) === 202)
  }

  // covers: MultiFocus.tuple's own write surface (modify per slot, replace broadcast). NOT a
  // `.andThen` case: same-carrier F1 composition is pinned by the two blocks above.
  "MultiFocus.tuple: modify per slot / replace broadcast" >> {
    val outer: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val doubled = outer.modify(_ * 2)((10, 20))
    (doubled === ((20, 40))).and(outer.replace(0)((1, 2)) === ((0, 0)))
  }
