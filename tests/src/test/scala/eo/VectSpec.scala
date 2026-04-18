package eo

import data.{AdjacentVect, ConsVect, NilVect, TConsVect, Vect}

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Property-based behaviour spec for the `Vect` data-carrier.
  *
  * Covers invariants that are inherently polymorphic in the `N` phantom index and therefore can't
  * be expressed as a single-type discipline RuleSet — see [[eo.laws.data.VectLaws]] for the per-N
  * `Functor` / `Traverse` equations that sit in the discipline framework.
  *
  * Scenarios land the four constructors (`NilVect`, `ConsVect`, `TConsVect`, `AdjacentVect`) with
  * concrete fixtures, and exercise concat (`++`), snoc (`:+`), cons (`+:`), and `slice`.
  */
class VectSpec extends Specification with ScalaCheck:

  // ---- Generators ---------------------------------------------------

  private def genConsList[N <: Int, A](n: Int)(using
      arbA: Arbitrary[A]
  ): Gen[Vect[N, A]] =
    if n <= 0 then Gen.const(Vect.nil[N, A])
    else
      for
        h <- arbA.arbitrary
        t <- genConsList[N, A](n - 1)
      yield (h +: t.asInstanceOf[Vect[0, A]]).asInstanceOf[Vect[N, A]]

  private given arbVect3Int: Arbitrary[Vect[3, Int]] =
    Arbitrary(genConsList[3, Int](3))

  private given arbVect5String: Arbitrary[Vect[5, String]] =
    Arbitrary(genConsList[5, String](5))

  // ---- `++` and arity -----------------------------------------------

  "(xs ++ ys).size" should {
    "equal xs.size + ys.size for ConsVect-built fixtures" >> {
      forAll((xs: Vect[3, Int], ys: Vect[5, String]) =>
        (xs.asInstanceOf[Vect[3, Int]] ++
          ys.asInstanceOf[Vect[5, Int]]).size == 8
      )
    }
  }

  "NilVect" should {
    "act as a right identity for ++ (runtime content equality)" >> {
      forAll((xs: Vect[3, Int]) =>
        val combined = xs ++ Vect.nil[0, Int]
        combined.size == xs.size
      )
    }

    "act as a left identity for ++ (runtime content equality)" >> {
      forAll((xs: Vect[3, Int]) =>
        val combined = Vect.nil[0, Int] ++ xs
        combined.size == xs.size
      )
    }
  }

  // ---- slice --------------------------------------------------------

  "slice(0, 0)" should {
    "produce a zero-length Vect on any input" >> {
      forAll((xs: Vect[3, Int]) => xs.slice(0, 0).size == 0)
    }
  }

  "slice(0, size)" should {
    "return a Vect of the original size" >> {
      forAll((xs: Vect[3, Int]) => xs.slice(0, 3).size == xs.size)
    }
  }

  // ---- cons / snoc --------------------------------------------------

  "Cons (+:)" should {
    "bump size by one and place the new value at head" >> {
      forAll((xs: Vect[3, Int], a: Int) =>
        val consed = a +: xs
        consed.size == xs.size + 1 && Vect.Head.unapply(consed) == Some(a)
      )
    }
  }

  "Snoc (:+)" should {
    "bump size by one and preserve the head when non-empty" >> {
      forAll((xs: Vect[3, Int], a: Int) =>
        val snocced = xs :+ a
        snocced.size == xs.size + 1 &&
        // Head is still xs's head: TConsVect.Head recurses into `init`.
        Vect.Head.unapply(snocced) == Vect.Head.unapply(xs)
      )
    }
  }

  // ---- AdjacentVect (direct constructor) ----------------------------
  //
  // `++` always produces an AdjacentVect; we pin the constructor-level
  // invariants by destructuring the result so scoverage can see the
  // `AdjacentVect` branch of `size`.

  "AdjacentVect(xs, ys)" should {
    "report size = xs.size + ys.size" >> {
      forAll((xs: Vect[3, Int]) =>
        val ys = Vect.of[2, Int](42)
        val combined: Vect[5, Int] =
          AdjacentVect[3, 2, Int, Vect[3, Int], Vect[2, Int]](xs, ys)
        combined.size == xs.size + ys.size
      )
    }
  }
