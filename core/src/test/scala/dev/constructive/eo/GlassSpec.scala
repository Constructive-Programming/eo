package dev.constructive.eo

import scala.compiletime.testing.typeChecks

import cats.instances.function.given
import cats.{Functor, Representable}
import org.scalacheck.Cogen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.GlassF
import optics.{Indexed, Optic}

object GlassFixtures:
  type Pair[A] = (A, A)
  type Tagged[A] = (String, Pair[A])

  val pairRepresentation: Representable.Aux[Pair, Boolean] = new Representable[Pair]:
    type Representation = Boolean
    def F: Functor[Pair] = new Functor[Pair]:
      def map[A, B](fa: Pair[A])(f: A => B): Pair[B] = (f(fa._1), f(fa._2))
    def index[A](fa: Pair[A]): Boolean => A = i => if i then fa._2 else fa._1
    def tabulate[A](f: Boolean => A): Pair[A] = (f(false), f(true))

  def pair[A, B]: Indexed.Grate[Pair[A], Pair[B], A, B, Boolean] =
    Indexed.representable[Pair, A, B](pairRepresentation)

  def tagged[A, B]: Indexed.Aux[Tagged[A], Tagged[B], A, B, Boolean, String] =
    Indexed[Tagged[A], Tagged[B], A, B, Boolean, String](s =>
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

/** Executable laws for the prototype. Generated functions are user-written tabulations, not
  * functions extracted from `to`. Function/context equality is checked pointwise; the Boolean grids
  * and Slot domain are enumerated completely. Infinite/Double domains can only be probed finitely
  * here and remain an equational proof obligation.
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
      val (context, values) = g.to(source)
      val (newContext, readBack) = g.to(g.from((context = (), values = written)))
      val first: (Boolean, Int) => Int = (i, n) => if i then n + 3 else n - 1
      val second: (Boolean, Int) => Int = (i, n) => if i then n * 2 else -n
      g.from((context = context, values = values)) == source &&
      newContext == () && agrees(readBack, written, bits) &&
      g.modify((_, n) => n)(source) == source &&
      g.modify(second)(g.modify(first)(source)) ==
        g.modify((i, n) => second(i, first(i, n)))(source)
    }
  }

  "Nested pair composition reads and replaces the full grid, NOT the legacy diagonal" >> {
    val g = pair[Pair[Int], Pair[Int]].andThen(pair[Int, Int])
    val source = ((1, 2), (3, 4))
    val (context, values) = g.to(source)
    val expectedRead: ((Boolean, Boolean)) => Int = ij =>
      val row = if ij._1 then source._2 else source._1
      if ij._2 then row._2 else row._1
    agrees(values, expectedRead, grid) must beTrue
    g.from((context = context, values = values)) must beEqualTo(source)
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

  "Full-grid to after from accepts arbitrary writes rather than only to's own values" >> {
    val g = pair[Pair[Int], Pair[Int]].andThen(pair[Int, Int])
    forAll { (a: Int, b: Int, c: Int, d: Int, written: ((Boolean, Boolean)) => Int) =>
      val source = ((a, b), (c, d))
      val (context, values) = g.to(source)
      val (afterContext, afterValues) = g.to(g.from((context = context, values = written)))
      g.from((context = context, values = values)) == source &&
      afterContext._1 == context._1 && bits.forall(i => afterContext._2(i) == context._2(i)) &&
      agrees(afterValues, written, grid) &&
      g.from((context = context, values = written)) ==
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
        val context: g.X = (outerTag, innerTags)
        val expected: Tagged[Tagged[Int]] =
          (
            outerTag,
            (
              (innerTags(false), (written((false, false)), written((false, true)))),
              (innerTags(true), (written((true, false)), written((true, true))))
            )
          )
        val built = g.from((context = context, values = written))
        val (afterContext, afterValues) = g.to(built)
        val (sourceContext, sourceValues) = g.to(expected)
        built == expected && g.from((context = sourceContext, values = sourceValues)) == expected &&
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
        val leftContext: left.X = ((root, rows), cells)
        val rightContext: right.X = (root, i => (rows(i), j => cells((i, j))))
        val leftValues: (((Boolean, Boolean), Boolean)) => Int =
          ijk => written((ijk._1._1, ijk._1._2, ijk._2))
        val rightValues: ((Boolean, (Boolean, Boolean))) => Int =
          ijk => written((ijk._1, ijk._2._1, ijk._2._2))
        val builtLeft = left.from((context = leftContext, values = leftValues))
        val builtRight = right.from((context = rightContext, values = rightValues))
        val (lc, lv) = left.to(builtLeft)
        val (rc, rv) = right.to(builtRight)
        builtLeft == builtRight && left.from((context = lc, values = lv)) == builtLeft &&
        right.from((context = rc, values = rv)) == builtRight &&
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
    val toStringFocus = Indexed.iso[Int, String, Int, Long](identity)(_.toString)
    val left = outer.andThen(inner).andThen(toStringFocus)
    val right = outer.andThen(inner.andThen(toStringFocus))
    // A non-inline generic helper has no same-focus evidence, and needs none.
    def compose[S, T, A, B, C, D, I, J](
        o: Indexed[S, T, A, B, I],
        in: Indexed[A, B, C, D, J],
    ) = o.andThen(in)
    val generic = compose(outer, inner)
    val source = ((1, 2), (3, 4))
    val expected = (("11", "12"), ("13", "14"))
    left.modify((_, n) => n.toLong + 10)(source) must beEqualTo(expected)
    right.modify((_, n) => n.toLong + 10)(source) must beEqualTo(expected)
    generic.modify((_, n) => (n + 10).toString)(source) must beEqualTo(expected)
    // The polymorphic round trip is stated with the matching target-side `to`.
    val polymorphic = outer.andThen(inner)
    val (context, _) = polymorphic.to(source)
    val written: ((Boolean, Boolean)) => String = ij => s"${ij._1}/${ij._2}"
    val rebuilt = polymorphic.from((context = context, values = written))
    val target = pair[Pair[String], Pair[String]].andThen(pair[String, String])
    agrees(target.to(rebuilt).values, written, grid) must beTrue
  }

  "Representable keeps a permuted index order, including when nested" >> {
    val g = Indexed.representable[Tri, Int, Int](permutedRepresentation)
    val nested = Indexed
      .representable[Tri, Pair[Int], Pair[Int]](permutedRepresentation)
      .andThen(pair[Int, Int])
    forAll { (a: Int, b: Int, c: Int, written: Slot => Int) =>
      val source = Tri(a, b, c)
      val (context, values) = g.to(source)
      val rebuilt = g.from((context = (), values = written))
      g.from((context = context, values = values)) == source &&
      agrees(g.to(rebuilt).values, written, Slot.values.toList) &&
      rebuilt == Tri(written(Slot.Second), written(Slot.Third), written(Slot.First)) &&
      nested.at((Slot.First, false))(Tri((a, b), (b, c), (c, a))) == c
    }
  }

  "Iso and Unit specializations have one coordinate and compose in either direction" >> {
    val single = Indexed.unit[Int, Int]
    val iso =
      Indexed.iso[(String, Int), (String, Int), (Int, String), (Int, String)](s => (s._2, s._1))(
        a => (a._2, a._1)
      )
    val source = ("kept", 2)
    val (context, values) = iso.to(source)
    iso.from((context = context, values = values)) must beEqualTo(source)
    val userWrite: Unit => (Int, String) = _ => (9, "new")
    iso.to(iso.from((context = (), values = userWrite))).values(()) must beEqualTo(userWrite(()))
    single.at(())(3) must beEqualTo(3)
    single.from((context = (), values = (_: Unit) => 9)) must beEqualTo(9)
    pair[Int, Int].andThen(single).modify((_, n) => n + 1)((1, 2)) must beEqualTo((2, 3))
    Indexed
      .unit[Pair[Int], Pair[Int]]
      .andThen(pair[Int, Int])
      .modify((_, n) => n + 1)((1, 2)) must beEqualTo((2, 3))
    typeChecks("""
      import dev.constructive.eo.optics.Indexed
      val g: Indexed.Grate[Int, String, Int, String, Unit] = Indexed.unit[Int, String]
      val result: String = g.replace("ok")(1)
    """) must beTrue
  }

  "Empty and singleton index spaces require no inhabitant or representative witness" >> {
    type Empty[A] = Nothing => A
    type Single[A] = Unit => A
    val empty = Indexed.representable[Empty, Int, Int](summon[Representable[Empty]])
    val single = Indexed.representable[Single, Int, Int](summon[Representable[Single]])
    val source: Empty[Int] = identity[Nothing]
    val written: Empty[Int] = identity[Nothing]
    val (context, values) = empty.to(source)
    // All functions from Nothing are extensionally equal: there is no coordinate to probe.
    val recovered: Empty[Int] = empty.from((context = context, values = values))
    val rewritten: Empty[Int] = empty.replace(9)(recovered)
    val readAgain = empty.to(empty.from((context = (), values = written)))
    readAgain.context must beEqualTo(())
    val emptyInner = pair[Empty[Int], Empty[Int]].andThen(empty)
    val emptyOuter = Indexed
      .representable[Empty, Pair[Int], Pair[Int]](summon[Representable[Empty]])
      .andThen(pair[Int, Int])
    emptyInner.to(emptyInner.replace(9)((source, rewritten))).context._1 must beEqualTo(())
    emptyOuter.to(emptyOuter.modify((_, n) => n)(identity[Nothing])).context._1 must beEqualTo(())
    val fn: Unit => Int = _ => 42
    val (sc, sv) = single.to(fn)
    single.from((context = sc, values = sv))(()) must beEqualTo(42)
    single.to(single.from((context = (), values = (_: Unit) => 9))).values(()) must beEqualTo(9)
    single.andThen(Indexed.unit[Int, Int]).at(((), ()))(fn) must beEqualTo(42)
  }

  "Double indexes preserve NaN and signed-zero observations with no coordinate-equality machinery" >> {
    type Doubles[A] = Double => A
    val g = Indexed.representable[Doubles, Int, Int](summon[Representable[Doubles]])
    val nested = Indexed
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
    val (context, values) = g.to(source)
    agrees(g.from((context = context, values = values)), source, probes) must beTrue
    agrees(g.to(g.from((context = (), values = classify))).values, classify, probes) must beTrue
    val full: ((Double, Double)) => Int = ij => 100 * classify(ij._1) + classify(ij._2)
    val fn: Double => Double => Int = i => j => full((i, j))
    val (nc, nv) = nested.to(fn)
    val rewritten = nested.from((context = nc, values = full))
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

  "Indexed is a member of the Optic family — widens to the carrier, dispatches capabilities" >> {
    val concrete = pair[Int, Int]
    val widened: Optic[Pair[Int], Pair[Int], Int, Int, GlassF[Boolean]] = concrete
    // `ForgetfulFunctor`'s companion gives the shared `.modify` / `.replace`
    // extensions — a plain `A => B` rewrites every coordinate pointwise, context untouched.
    widened.modify(_ + 1)((1, 2)) must beEqualTo((2, 3))
    widened.replace(9)((1, 2)) must beEqualTo((9, 9))
    // The named-tuple bundle is available through the shared Optic interface.
    widened.to((1, 2)).context must beEqualTo(())
    agrees(widened.to((1, 2)).values, concrete.to((1, 2)).values, bits) must beTrue
    concrete.from(concrete.to((5, 6))) must beEqualTo((5, 6))
  }
