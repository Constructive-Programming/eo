package dev.constructive.eo

import scala.compiletime.testing.typeChecks
import scala.language.implicitConversions

import cats.instances.function.given
import cats.{Functor, Representable}
import dev.constructive.eo.compose.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.{Direct, Function1BroadcastOptic, MultiFocus, RepresentativeIndex}
import data.MultiFocus.at
import optics.{Iso, Optic}
import optics.Optic.*

/** Fixtures for the [[RepresentativeIndex]] blocks — the two index types the shipped instances do
  * not cover, one witnessed and one not (`Side`/`Phantom`; the spec's own `Slot`, which #123 made
  * the read-time index of `Tri`, is witnessed on the class instead). Same shape as
  * `GrateShapeSpec`'s fixture object: the `typeChecks` cells need the names in the *typechecking*
  * scope, not a nested block's.
  */
object WitnessFixtures:

  /** An algebraic index type — no canonical inhabitant, so the caller has to witness it. */
  sealed trait Side
  case object LeftSide extends Side
  case object RightSide extends Side

  given RepresentativeIndex[Side] = RepresentativeIndex.at(RightSide)

  /** Same carrier shape, same factories, **no** witness — the bridge must not resolve. */
  sealed trait Phantom

  type SideFn = [a] =>> Side => a
  type PhantomFn = [a] =>> Phantom => a
  type BoolFn = [a] =>> Boolean => a

  val grateSide: Optic[Side => Int, Side => Int, Int, Int, MultiFocus[Function1[Side, *]]] =
    MultiFocus.representable[SideFn, Int]

  val grateBool
      : Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
    MultiFocus.representable[BoolFn, Int]

  val gratePhantom
      : Optic[Phantom => Int, Phantom => Int, Int, Int, MultiFocus[Function1[Phantom, *]]] =
    MultiFocus.representable[PhantomFn, Int]

  val isoInt: Iso[Int, Int] = Iso[Int, Int, Int, Int](_ + 1000, _ - 1000)

  val isoBoolFocus: Iso[Boolean => Int, Boolean => Int] =
    Iso[Boolean => Int, Boolean => Int, Boolean => Int, Boolean => Int](identity, identity)

  val isoSideFocus: Iso[Side => Int, Side => Int] =
    Iso[Side => Int, Side => Int, Side => Int, Side => Int](identity, identity)

  val isoPhantomFocus: Iso[Phantom => Int, Phantom => Int] =
    Iso[Phantom => Int, Phantom => Int, Phantom => Int, Phantom => Int](identity, identity)

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
  *   - The `RepresentativeIndex` witness: the bridged `from` reads at a supplied index, the
  *     kernel's per-index write ignores it, and an index type with no instance does not bridge at
  *     all.
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

  // The witness the inbound Iso bridge needs for that same `Slot`-indexed carrier — the index #123
  // made a read-time argument is the one the BRIDGE cannot take per call (the kernel hands it the
  // whole bundle), so here it is a construction-time value instead: S1, the SECOND index, not the
  // first field. Additive: nothing in this spec built a `Slot`-indexed bridge before it.
  given slotWitness: RepresentativeIndex[Slot] = RepresentativeIndex.at(Slot.S1)

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

  // covers: the RepresentativeIndex witness — `Function1BroadcastOptic.from` reads a bundle at the
  // index it was built with, which is the one place a Grate read needs a value it cannot compute.
  // Shipped wrappers (`modify` / `collect*` / the kernel) hand it a CONSTANT bundle, so the witness
  // is invisible through them; a varying bundle is read at the witness — a defined answer where the
  // sentinel had a forged one.
  "RepresentativeIndex: the bridged `from` reads at the supplied index; constant bundles are witness-invariant" >> {
    // The varying-bundle read is white-box on purpose: outside this package the carrier's leftover
    // is abstract, so no caller can even NAME a bundle whose value depends on the index (the only
    // in-scope source of such a value is the optic's own `to`, which is constant). Inside, the
    // product class is nameable and its `type X = Unit` comes with it.
    val varying: MultiFocus[Function1[Int, *]][Unit, Int] =
      MultiFocus[[a] =>> Int => a, Unit, Int]((), (i: Int) => i * 10)
    val readAtZero = new Function1BroadcastOptic[Int, Int, Int, Int, Int](WitnessFixtures.isoInt, 0)
    val readAtOne = new Function1BroadcastOptic[Int, Int, Int, Int, Int](WitnessFixtures.isoInt, 1)
    val varies = (readAtZero.from(varying) === -1000).and(readAtOne.from(varying) === -990)

    // The public construction path: a local witness + the ordinary Composer.
    def bridgeAt(i: Int): Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      given RepresentativeIndex[Int] = RepresentativeIndex.at(i)
      summon[Composer[Direct, MultiFocus[Function1[Int, *]]]].to(WitnessFixtures.isoInt)

    val atZero = bridgeAt(0)
    val atOne = bridgeAt(1)

    // The bundle here is the optic's OWN (`to` ignores the index, it broadcasts the Iso's focus), so
    // both witnesses agree — this is the shipped read path: `modify` / `replace` / `collect*` and the
    // kernel's fallback. The Iso's read/write cancel, so the round trip is the identity either way.
    val invariant =
      (atZero.from(atZero.to(3)) === 3).and(atOne.from(atOne.to(3)) === 3)
    val modifyInvariant = (atZero.modify(_ + 1)(3) === 4).and(atOne.modify(_ + 1)(3) === 4)

    varies.and(invariant).and(modifyInvariant)
  }

  // covers: the witness does NOT become the write index — `grate ∘ iso` still rebuilds every
  // position from its own focus, even when the witness in scope is a non-canonical one. This is the
  // guarantee that separates the witness (a read-side convenience) from `broadcastFrom` (the
  // kernel's per-index write).
  "grate ∘ iso stays positional under a non-canonical witness" >> {
    given RepresentativeIndex[Int] = RepresentativeIndex.at(2)

    val composed = MultiFocus.tuple[(Int, Int, Int), Int].andThen(WitnessFixtures.isoInt)
    val modified = composed.modify(_ + 1)((10, 20, 30)) === ((11, 21, 31))
    val read0 = composed.at(0)((10, 20, 30)) === 1010
    val read2 = composed.at(2)((10, 20, 30)) === 1030

    modified.and(read0).and(read2)
  }

  // covers: the explicit construction path on an index type with no canonical inhabitant — the
  // caller's witness (WitnessFixtures' `given RepresentativeIndex[Side]`) is what makes the inbound
  // Iso bridge resolve, and the composite is positional over that algebraic index space.
  "explicit witness on an algebraic index: bridge resolves and composes positionally" >> {
    import WitnessFixtures.*

    val composed: Optic[Side => Int, Side => Int, Int, Int, MultiFocus[Function1[Side, *]]] =
      grateSide.andThen(isoInt)

    val sides: Side => Int = s => if s == LeftSide then 1 else 2
    val modified = composed.modify(_ + 1)(sides)
    // The Iso's read (`+1000`) and its write (`-1000`) cancel position by position — each side is
    // rebuilt from its own focus, not from a single sampled one.
    val positional = (modified(LeftSide) === 2).and(modified(RightSide) === 3)
    val reads = (composed.at(LeftSide)(sides) === 1001).and(composed.at(RightSide)(sides) === 1002)

    positional.and(reads)
  }

  // covers: the witness on a `Representable`-indexed grate whose index ORDER is a permutation of its
  // field order (`Tri` / `Slot` above — the fixture #123 introduced for "position is a read-time
  // argument"). `representable` pins the carrier's index to `F.Representation`, which has no
  // canonical inhabitant, so the bridge takes the caller's witness — here `Slot.S1`, the SECOND
  // index, not the first field — and the write still follows the instance's own `map`.
  "witnessed permuted Representable: bridge takes the caller's index, write stays pointwise" >> {
    val composed =
      MultiFocus.representable[Tri, Int](using triRepresentable).andThen(WitnessFixtures.isoInt)
    val tri = Tri(1, 2, 3)

    val points = composed.modify(_ + 1)(tri) === triFunctor.map(tri)(_ + 1)
    // `replace` writes the composite's focus through the inner Iso's own `-1000`, pointwise — so the
    // bare grate's `tabulate(_ => 0)` is this test's `tabulate(_ => -1000)`.
    val replaces = composed.replace(0)(tri) === triRepresentable.tabulate(_ => -1000)
    // The composite's `.at` reads the inner Iso's `+1000` on top of the instance's index order:
    // S0 is the THIRD field (3) and S2 the second (2).
    val reads = (composed.at(Slot.S0)(tri) === 1003).and(composed.at(Slot.S2)(tri) === 1002)

    points.and(replaces).and(reads)
  }

  // covers: the cost side of the witness — an index type with no instance does not bridge at all,
  // while the same shape with a witness does. Compile-pinned, no expected-type ascription. This is
  // the trade the sentinel used to paper over: a read that has no index is refused rather than
  // read at a value that cannot exist.
  "RepresentativeIndex: witnessed index bridges, unwitnessed index does not resolve" >> {
    import WitnessFixtures.*

    val witnessed = typeChecks("grateSide.andThen(isoInt)")
    val unwitnessed = typeChecks("gratePhantom.andThen(isoPhantomFocus)")

    witnessed must beTrue
    unwitnessed must beFalse
  }

  // covers: the shipped instances themselves — every index type the factories fix with a canonical
  // value has one, and it is a real value (`0` / `false` / `()` / the singleton), not a sentinel.
  "RepresentativeIndex: the shipped instances name a real index" >> {
    val indices = (
      summon[RepresentativeIndex[Int]].index,
      summon[RepresentativeIndex[Boolean]].index,
      summon[RepresentativeIndex[Unit]].index,
      summon[RepresentativeIndex[true]].index,
    )

    indices === ((0, false, (), true))
  }

  // covers: the second shipped index type reaching the bridge with no local `given` — the companion
  // instance is what resolves it, so a Boolean-indexed grate composes import-free like `tuple`.
  "RepresentativeIndex: a Boolean-indexed grate bridges off the companion instance" >> {
    import WitnessFixtures.*

    typeChecks("isoBoolFocus.andThen(grateBool)") must beTrue
  }
