package dev.constructive.eo

import scala.collection.immutable.SortedSet

import cats.data.Ior
import org.specs2.mutable.Specification

import optics.{Fold, Lens, Modify, Traversal}

/** Behaviour pins for the cats-typeclass optic constructors on the optic companions: construct via
  * optic, consume via capability — the given declarations belong to the client (bind a constructor
  * result as an optic given, or write a direct capability instance).
  */
class CatsConstructorsSpec extends Specification:

  private def bump[S](s: S)(using m: CanModify[S, Int]): S = m.modify(_ + 1)(s)
  private def total[S](s: S)(using f: CanFold[S, Int]): Int = f.foldMap(identity)(s)

  "client-declared givens (the documented pattern)" should {
    "bind a constructor result as the pair's optic given" >> {
      given Traversal[List[Int], List[Int], Int, Int] = Traversal.each
      (bump(List(1, 2, 3)) === List(2, 3, 4)).and(total(List(1, 2, 3)) === 6)
    }
    "declare a direct capability instance with no optic at all" >> {
      given CanModify[Vector[Int], Int] = f => _.map(f)
      bump(Vector(1, 2)) === Vector(2, 3)
    }
  }

  "Traversal.each (Traverse)" should {
    "serve CanModify and CanFold through the derivation given" >> {
      given Traversal[Option[Int], Option[Int], Int, Int] = Traversal.each
      (bump(Option(41)) === Option(42)).and(total(Option.empty[Int]) === 0)
    }
    "expose the CanFold helpers" >> {
      given Traversal[List[Int], List[Int], Int, Int] = Traversal.each
      val f = summon[CanFold[List[Int], Int]]
      (f.length(List(1, 2, 3)) === 3).and(f.headOption(List(1, 2, 3)) === Some(1))
    }
  }

  "Modify.forFunctor" should {
    "modify Function1 results — Modify IS CanModify evidence" >> {
      val g: String => Int = _.length
      bump(g)(using Modify.forFunctor[[x] =>> String => x, Int])("four") === 5
    }
    "change the focus type via pForFunctor" >> {
      Modify
        .pForFunctor[[x] =>> String => x, Int, String]
        .modify(_.toString)((_: String).length)(
          "ab"
        ) === "2"
    }
  }

  "Fold.apply (Foldable)" should {
    "fold a SortedSet — Fold IS CanFold evidence" >> {
      total(SortedSet(1, 2, 3))(using Fold[SortedSet, Int]) === 6
    }
  }

  "Traversal.first / second / both (Bitraverse)" should {
    "first: Either's left, Right rides along" >> {
      val leftT = Traversal.first[Either, Int, Int, String]
      (leftT.modify(_ + 1)(Left(41): Either[Int, String]) === Left(42))
        .and(leftT.modify(_ + 1)(Right("ok"): Either[Int, String]) === Right("ok"))
    }
    "second: Tuple2._2, _1 rides along" >> {
      Traversal.second[Tuple2, Int, Int, String].modify(_ * 2)(("k", 21)) === ("k", 42)
    }
    "both: every A position of an F[A, A]" >> {
      val bothT = Traversal.both[Ior, Int, Int]
      (bothT.modify(_ + 1)(Ior.both(1, 2)) === Ior.both(2, 3))
        .and(bothT.foldMap(identity)(Ior.both(1, 2)) === 3)
        .and(Traversal.both[Tuple2, Int, Int].modify(_ + 1)((1, 2)) === ((2, 3)))
    }
  }

  "Lens.representable (positional Lens)" should {
    val atK = Lens.representable[[x] =>> String => x, Int](using summon)("k")
    val f: String => Int = _.length
    "get the value at the point" >> (atK.get(f) === 1)
    "replace only that point — siblings survive" >> {
      val g = atK.replace(99)(f)
      (g("k") === 99).and(g("other") === 5)
    }
    "get-put: replacing with the read is identity at every point" >> {
      val g = atK.replace(atK.get(f))(f)
      (g("k") === f("k")).and(g("xyz") === f("xyz"))
    }
  }
