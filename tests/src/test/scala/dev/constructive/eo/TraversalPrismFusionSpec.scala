package dev.constructive.eo

import org.specs2.mutable.Specification

import optics.{Prism, Traversal}

enum Shp:
  case Circle(r: Double)
  case Square(s: Double)

/** `Traversal.andThen(Prism)` — the FILTERING composition, and the mirror of
  * `Prism.andThen(Traversal)`. Walking a heterogeneous collection and touching only the elements
  * that match is the everyday use, and the law that makes it safe is that a missed element is
  * rebuilt untouched rather than dropped.
  */
class TraversalPrismFusionSpec extends Specification:

  val circleP = Prism.optional[Shp, Double](
    { case Shp.Circle(r) => Some(r); case _ => None },
    Shp.Circle(_),
  )

  val mendTearCircleP = Prism[Shp, Double](
    { case Shp.Circle(r) => Right(r); case other => Left(other) },
    Shp.Circle(_),
  )

  // Concrete `Traversal` — the point of the fused member.
  val circleRadii: Traversal[List[Shp], List[Shp], Double, Double] =
    Traversal.each[List, Shp].andThen(circleP)

  val mixed = List(Shp.Circle(1.0), Shp.Square(2.0), Shp.Circle(3.0))

  "the filtering traversal" should {
    "fold only the matching elements" >> {
      (circleRadii.foldMap(identity)(mixed) === 4.0)
        .and(circleRadii.foldMap(_ => 1)(mixed) === 2)
    }
    "modify matching elements and rebuild misses UNTOUCHED (not dropped)" >> {
      circleRadii.modify(_ * 10)(mixed) ===
        List(Shp.Circle(10.0), Shp.Square(2.0), Shp.Circle(30.0))
    }
    "preserve length and order — a miss is not a deletion" >> {
      val out = circleRadii.modify(_ + 1)(mixed)
      (out.length === 3).and(out(1) === Shp.Square(2.0))
    }
    "have zero foci when nothing matches, passing writes through" >> {
      val squares = List(Shp.Square(1.0), Shp.Square(2.0))
      (circleRadii.foldMap(_ => 1)(squares) === 0)
        .and(circleRadii.modify(_ * 10)(squares) === squares)
    }
    "modify(identity) is the identity (the seam law, on a filtering chain)" >> {
      circleRadii.modify(identity)(mixed) === mixed
    }
    "replace broadcasts to matching elements only" >> {
      circleRadii.replace(9.0)(mixed) ===
        List(Shp.Circle(9.0), Shp.Square(2.0), Shp.Circle(9.0))
    }
    "work for the MendTearPrism shape too" >> {
      val viaMendTear: Traversal[List[Shp], List[Shp], Double, Double] =
        Traversal.each[List, Shp].andThen(mendTearCircleP)
      (viaMendTear.foldMap(identity)(mixed) === 4.0)
        .and(viaMendTear.modify(_ * 10)(mixed) === circleRadii.modify(_ * 10)(mixed))
    }
    "compose onward on the fused path" >> {
      val nested: Traversal[List[List[Shp]], List[List[Shp]], Double, Double] =
        Traversal.each[List, List[Shp]].andThen(circleRadii)
      nested.modify(_ * 2)(List(mixed, List(Shp.Circle(5.0)))) ===
        List(List(Shp.Circle(2.0), Shp.Square(2.0), Shp.Circle(6.0)), List(Shp.Circle(10.0)))
    }
  }
