package dev.constructive.eo

import scala.compiletime.testing.typeChecks

import cats.instances.function.given
import cats.{Functor, Representable}
import org.scalacheck.Cogen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.IndexedGlass

object GlassFixtures:
  type Pair[A] = (A, A)
  type Tagged[A] = (String, Pair[A])

  val pairRepresentation: Representable.Aux[Pair, Boolean] = new Representable[Pair]:
    type Representation = Boolean
    def F: Functor[Pair] = new Functor[Pair]:
      def map[A, B](fa: Pair[A])(f: A => B): Pair[B] = (f(fa._1), f(fa._2))
    def index[A](fa: Pair[A]): Boolean => A = i => if i then fa._2 else fa._1
    def tabulate[A](f: Boolean => A): Pair[A] = (f(false), f(true))

  def pair[A, B]: IndexedGlass.Grate[Pair[A], Pair[B], A, B, Boolean] =
    IndexedGlass.representable[Pair, A, B](pairRepresentation)

  def tagged[A, B]: IndexedGlass.Aux[Tagged[A], Tagged[B], A, B, Boolean, String] =
    IndexedGlass[Tagged[A], Tagged[B], A, B, Boolean, String](s =>
      (s._1, pairRepresentation.index(s._2)),
    )((context, values) => (context, pairRepresentation.tabulate(values)))

  enum Slot:
    case First, Second, Third

  given Cogen[Slot] = Cogen[Int].contramap(_.ordinal)

  case class Tri[A](a: A, b: A, c: A)

  val permutedRepresentation: Representable.Aux[Tri, Slot] = new Representable[Tri]:
    type Representation = Slot
    def F: Functor[Tri] = new Functor[Tri]:
      def map[A, B](fa: Tri[A])(f: A => B): Tri[B] = Tri(f(fa.a), f(fa.b), f(fa.c))
    def index[A](fa: Tri[A]): Slot => A =
      case Slot.First  => fa.c
      case Slot.Second => fa.a
      case Slot.Third  => fa.b
    def tabulate[A](f: Slot => A): Tri[A] = Tri(f(Slot.Second), f(Slot.Third), f(Slot.First))

/** Executable laws for the standalone prototype. Generated functions are user-written tabulations,
  * not functions extracted from a split. Function/context equality is checked pointwise; the
  * Boolean grids and Slot domain are enumerated completely. Infinite/Double domains can only be
  * probed finitely here and remain an equational proof obligation.
  */
class GlassSpec extends Specification with ScalaCheck:
  import GlassFixtures.*

  private val bits = List(false, true)
  private val grid = for i <- bits; j <- bits yield (i, j)
  private val cube = for i <- bits; j <- bits; k <- bits yield (i, j, k)

  private def agrees[I, A](actual: I => A, expected: I => A, indexes: List[I]): Boolean =
    indexes.forall(i => actual(i) == expected(i))

  "Representable: both round trips on arbitrary tabulations, identity and indexed modification composition" >> {
    val g = pair[Int, Int]
    forAll { (a: Int, b: Int, written: Boolean => Int) =>
      val source = (a, b)
      val (context, values) = g.split(source)
      val (newContext, readBack) = g.split(g.rebuild((), written))
      val first: (Boolean, Int) => Int = (i, n) => if i then n + 3 else n - 1
      val second: (Boolean, Int) => Int = (i, n) => if i then n * 2 else -n
      g.rebuild(context, values) == source &&
      newContext == () && agrees(readBack, written, bits) &&
      g.modify((_, n) => n)(source) == source &&
      g.modify(second)(g.modify(first)(source)) ==
        g.modify((i, n) => second(i, first(i, n)))(source)
    }
  }

  "Nested pair composition reads and replaces the full grid, NOT the legacy diagonal" >> {
    val g = pair[Pair[Int], Pair[Int]].andThen(pair[Int, Int])
    val source = ((1, 2), (3, 4))
    val (context, values) = g.split(source)
    val expectedRead: ((Boolean, Boolean)) => Int = ij =>
      val row = if ij._1 then source._2 else source._1
      if ij._2 then row._2 else row._1
    agrees(values, expectedRead, grid) must beTrue
    g.rebuild(context, values) must beEqualTo(source)
    g.modify((_, n) => n)(source) must beEqualTo(source)
    g.replace(9)(source) must beEqualTo(((9, 9), (9, 9)))
    g.replace(9)(source) must not(beEqualTo(((9, 2), (3, 9))))
    g.at((false, true))(source) must beEqualTo(2)
    g.at((true, false))(source) must beEqualTo(3)
    g.modify((ij, n) => n + (if ij._1 then 100 else 0) + (if ij._2 then 10 else 0))(
      source
    ) must beEqualTo(
      ((1, 12), (103, 114)),
    )
  }

  "Full-grid split after rebuild accepts arbitrary writes rather than only a split's own values" >> {
    val g = pair[Pair[Int], Pair[Int]].andThen(pair[Int, Int])
    forAll { (a: Int, b: Int, c: Int, d: Int, written: ((Boolean, Boolean)) => Int) =>
      val source = ((a, b), (c, d))
      val (context, values) = g.split(source)
      val (afterContext, afterValues) = g.split(g.rebuild(context, written))
      g.rebuild(context, values) == source &&
      afterContext._1 == context._1 && bits.forall(i => afterContext._2(i) == context._2(i)) &&
      agrees(afterValues, written, grid) &&
      g.rebuild(context, written) ==
        (
          (written((false, false)), written((false, true))),
          (written((true, false)), written((true, true)))
        )
    }
  }

  "User-defined contexts survive both round trips, including arbitrary contexts and grids" >> {
    val g = tagged[Tagged[Int], Tagged[Int]].andThen(tagged[Int, Int])
    forAll {
      (outerTag: String, innerTags: Boolean => String, written: ((Boolean, Boolean)) => Int) =>
        val context: g.Context = (outerTag, innerTags)
        val expected: Tagged[Tagged[Int]] =
          (
            outerTag,
            (
              (innerTags(false), (written((false, false)), written((false, true)))),
              (innerTags(true), (written((true, false)), written((true, true))))
            )
          )
        val built = g.rebuild(context, written)
        val (afterContext, afterValues) = g.split(built)
        val (sourceContext, sourceValues) = g.split(expected)
        built == expected && g.rebuild(sourceContext, sourceValues) == expected &&
        afterContext._1 == outerTag && agrees(afterContext._2, innerTags, bits) &&
        agrees(afterValues, written, grid)
    }
  }

  "Three-level associativity re-associates indexes AND non-Unit contexts on arbitrary writes" >> {
    val outer = tagged[Tagged[Tagged[Int]], Tagged[Tagged[Int]]]
    val middle = tagged[Tagged[Int], Tagged[Int]]
    val inner = tagged[Int, Int]
    val left = outer.andThen(middle).andThen(inner)
    val right = outer.andThen(middle.andThen(inner))
    forAll {
      (
          root: String,
          rows: Boolean => String,
          cells: ((Boolean, Boolean)) => String,
          written: ((Boolean, Boolean, Boolean)) => Int
      ) =>
        val leftContext: left.Context = ((root, rows), cells)
        val rightContext: right.Context = (root, i => (rows(i), j => cells((i, j))))
        val leftValues: (((Boolean, Boolean), Boolean)) => Int =
          ijk => written((ijk._1._1, ijk._1._2, ijk._2))
        val rightValues: ((Boolean, (Boolean, Boolean))) => Int =
          ijk => written((ijk._1, ijk._2._1, ijk._2._2))
        val builtLeft = left.rebuild(leftContext, leftValues)
        val builtRight = right.rebuild(rightContext, rightValues)
        val (lc, lv) = left.split(builtLeft)
        val (rc, rv) = right.split(builtRight)
        builtLeft == builtRight && left.rebuild(lc, lv) == builtLeft &&
        right.rebuild(rc, rv) == builtRight &&
        lc._1._1 == root && rc._1 == root &&
        bits.forall(i => lc._1._2(i) == rows(i) && rc._2(i)._1 == rows(i)) &&
        grid.forall(ij => lc._2(ij) == cells(ij) && rc._2(ij._1)._2(ij._2) == cells(ij)) &&
        cube.forall {
          case (i, j, k) =>
            lv(((i, j), k)) == written((i, j, k)) && rv((i, (j, k))) == written((i, j, k))
        } &&
        left.modify((_, n) => n + 1)(builtLeft) == right.modify((_, n) => n + 1)(builtRight) &&
        left.replace(9)(builtLeft) == right.replace(9)(builtRight)
    }
  }

  "Type-changing intermediate and final writes are full-grid and route/grouping independent" >> {
    val outer = pair[Pair[Int], Pair[String]]
    val inner = pair[Int, String]
    val toStringFocus = IndexedGlass.iso[Int, String, Int, Long](identity)(_.toString)
    val left = outer.andThen(inner).andThen(toStringFocus)
    val right = outer.andThen(inner.andThen(toStringFocus))
    // A non-inline generic helper has no same-focus evidence, and needs none.
    def compose[S, T, A, B, C, D, I, J](
        o: IndexedGlass[S, T, A, B, I],
        in: IndexedGlass[A, B, C, D, J],
    ) = o.andThen(in)
    val generic = compose(outer, inner)
    val source = ((1, 2), (3, 4))
    val expected = (("11", "12"), ("13", "14"))
    left.modify((_, n) => n.toLong + 10)(source) must beEqualTo(expected)
    right.modify((_, n) => n.toLong + 10)(source) must beEqualTo(expected)
    generic.modify((_, n) => (n + 10).toString)(source) must beEqualTo(expected)
    // The polymorphic round trip is stated with the matching target-side split.
    val polymorphic = outer.andThen(inner)
    val (context, _) = polymorphic.split(source)
    val written: ((Boolean, Boolean)) => String = ij => s"${ij._1}/${ij._2}"
    val rebuilt = polymorphic.rebuild(context, written)
    val target = pair[Pair[String], Pair[String]].andThen(pair[String, String])
    agrees(target.split(rebuilt)._2, written, grid) must beTrue
  }

  "Representable keeps a permuted index order, including when nested" >> {
    val g = IndexedGlass.representable[Tri, Int, Int](permutedRepresentation)
    val nested = IndexedGlass
      .representable[Tri, Pair[Int], Pair[Int]](permutedRepresentation)
      .andThen(pair[Int, Int])
    forAll { (a: Int, b: Int, c: Int, written: Slot => Int) =>
      val source = Tri(a, b, c)
      val (context, values) = g.split(source)
      val rebuilt = g.rebuild((), written)
      g.rebuild(context, values) == source &&
      agrees(g.split(rebuilt)._2, written, Slot.values.toList) &&
      rebuilt == Tri(written(Slot.Second), written(Slot.Third), written(Slot.First)) &&
      nested.at((Slot.First, false))(Tri((a, b), (b, c), (c, a))) == c
    }
  }

  "Iso and Unit specializations have one coordinate and compose in either direction" >> {
    val single = IndexedGlass.unit[Int, Int]
    val iso = IndexedGlass.iso[(String, Int), (String, Int), (Int, String), (Int, String)](s =>
      (s._2, s._1),
    )(a => (a._2, a._1))
    val source = ("kept", 2)
    val (context, values) = iso.split(source)
    iso.rebuild(context, values) must beEqualTo(source)
    val userWrite: Unit => (Int, String) = _ => (9, "new")
    iso.split(iso.rebuild((), userWrite))._2(()) must beEqualTo(userWrite(()))
    single.at(())(3) must beEqualTo(3)
    single.rebuild((), _ => 9) must beEqualTo(9)
    pair[Int, Int].andThen(single).modify((_, n) => n + 1)((1, 2)) must beEqualTo((2, 3))
    IndexedGlass
      .unit[Pair[Int], Pair[Int]]
      .andThen(pair[Int, Int])
      .modify((_, n) => n + 1)((1, 2)) must beEqualTo((2, 3))
    typeChecks("""
      import dev.constructive.eo.data.IndexedGlass
      val g: IndexedGlass.Grate[Int, String, Int, String, Unit] = IndexedGlass.unit[Int, String]
      val result: String = g.replace("ok")(1)
    """) must beTrue
  }

  "Empty and singleton index spaces require no inhabitant or representative witness" >> {
    type Empty[A] = Nothing => A
    type Single[A] = Unit => A
    val empty = IndexedGlass.representable[Empty, Int, Int](summon[Representable[Empty]])
    val single = IndexedGlass.representable[Single, Int, Int](summon[Representable[Single]])
    val source: Empty[Int] = identity[Nothing]
    val written: Empty[Int] = identity[Nothing]
    val (context, values) = empty.split(source)
    // All functions from Nothing are extensionally equal: there is no coordinate to probe.
    val recovered: Empty[Int] = empty.rebuild(context, values)
    val rewritten: Empty[Int] = empty.replace(9)(recovered)
    val splitAgain = empty.split(empty.rebuild((), written))
    splitAgain._1 must beEqualTo(())
    val emptyInner = pair[Empty[Int], Empty[Int]].andThen(empty)
    val emptyOuter = IndexedGlass
      .representable[Empty, Pair[Int], Pair[Int]](summon[Representable[Empty]])
      .andThen(pair[Int, Int])
    emptyInner.split(emptyInner.replace(9)((source, rewritten)))._1._1 must beEqualTo(())
    emptyOuter.split(emptyOuter.modify((_, n) => n)(identity[Nothing]))._1._1 must beEqualTo(())
    val fn: Unit => Int = _ => 42
    val (sc, sv) = single.split(fn)
    single.rebuild(sc, sv)(()) must beEqualTo(42)
    single.split(single.rebuild((), _ => 9))._2(()) must beEqualTo(9)
    single.andThen(IndexedGlass.unit[Int, Int]).at(((), ()))(fn) must beEqualTo(42)
  }

  "Double indexes preserve NaN and signed-zero observations with no coordinate-equality machinery" >> {
    type Doubles[A] = Double => A
    val g = IndexedGlass.representable[Doubles, Int, Int](summon[Representable[Doubles]])
    val nested = IndexedGlass
      .representable[Doubles, Doubles[Int], Doubles[Int]](
        summon[Representable[Doubles]],
      )
      .andThen(g)
    val probes = List(Double.NaN, -0.0, 0.0, 1.0, Double.PositiveInfinity)
    // Bit observations belong to this USER function, not the optic or its addressing protocol.
    val classify: Double => Int = d =>
      if d.isNaN then 7
      else if java.lang.Double.doubleToRawLongBits(d) == Long.MinValue then 11
      else if d == 0.0 then 13
      else 17
    val source: Double => Int = classify
    val (context, values) = g.split(source)
    agrees(g.rebuild(context, values), source, probes) must beTrue
    agrees(g.split(g.rebuild((), classify))._2, classify, probes) must beTrue
    val full: ((Double, Double)) => Int = ij => 100 * classify(ij._1) + classify(ij._2)
    val fn: Double => Double => Int = i => j => full((i, j))
    val (nc, nv) = nested.split(fn)
    val rewritten = nested.rebuild(nc, full)
    probes.forall(i =>
      probes.forall(j =>
        nv((i, j)) == full((i, j)) && rewritten(i)(j) == full((i, j)) &&
          nested.modify((_, n) => n)(fn)(i)(j) == fn(i)(j) &&
          nested.replace(9)(fn)(i)(j) == 9,
      )
    ) must beTrue
    nested.at((-0.0, 0.0))(fn) must beEqualTo(1113)
    nested.at((0.0, -0.0))(fn) must beEqualTo(1311)
    nested.at((Double.NaN, Double.NaN))(fn) must beEqualTo(707)
  }
