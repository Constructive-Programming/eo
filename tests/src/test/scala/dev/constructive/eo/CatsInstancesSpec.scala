package dev.constructive.eo

import scala.collection.immutable.SortedSet

import org.specs2.mutable.Specification

import instances.given

/** Behaviour pins for the cats-typeclass capability bridge (`eo.instances`): the strongest cats
  * class elects the single optic given per `(F[A], A)` pair, and generic capability-consuming code
  * works against plain cats containers with no hand-written optic.
  */
class CatsInstancesSpec extends Specification:

  private def bump[S](s: S)(using m: CanModify[S, Int]): S = m.modify(_ + 1)(s)
  private def total[S](s: S)(using f: CanFold[S, Int]): Int = f.foldMap(identity)(s)

  "Traverse-backed containers (Traversal.each)" should {
    "satisfy CanModify" >> (bump(List(1, 2, 3)) === List(2, 3, 4))
    "satisfy CanFold" >> (total(Vector(1, 2, 3)) === 6)
    "cover one-or-zero containers" >> {
      (bump(Option(41)) === Option(42))
        .and(total(Option.empty[Int]) === 0)
        .and(bump(Right(41): Either[String, Int]) === Right(42))
    }
    "expose the derived CanFold helpers" >> {
      val f = summon[CanFold[List[Int], Int]]
      (f.length(List(1, 2, 3)) === 3)
        .and(f.headOption(List(1, 2, 3)) === Some(1))
        .and(f.exists(_ > 2)(List(1, 2, 3)) === true)
    }
  }

  "Functor-only containers (write-only Modify)" should {
    "satisfy CanModify for Function1 results" >> {
      val g: String => Int = _.length
      bump(g)("four") === 5
    }
  }

  "Foldable-only containers (Fold)" should {
    "satisfy CanFold for SortedSet" >> (total(SortedSet(1, 2, 3)) === 6)
    "not silently pretend to CanModify" >> {
      // SortedSet has no Functor/Traverse in cats — the demand must not compile.
      scala.compiletime.testing.typeChecks("bump(SortedSet(1, 2, 3))") === false
    }
  }
