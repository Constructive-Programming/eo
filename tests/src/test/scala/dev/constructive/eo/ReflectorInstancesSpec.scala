package dev.constructive.eo

import cats.data.{Const, ZipList}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Reflector-law unit tests per the plan's D6 (Unit 1). These exist in place of a full discipline
  * `ReflectorTests` RuleSet: plan Open Question #6 defers promotion to discipline until the
  * Reflector instance surface grows beyond the v1 three shipped instances.
  *
  * Two laws per instance:
  *   - **R1 — map-compat**: `reflect(fa)(f).map(g) == reflect(fa)(fa_ => g(f(fa_)))`. Post-
  *     composition with a pure function distributes into the reflector's aggregator.
  *   - **R2 — const-collapse**: `reflect(fa)(_ => b).map(_ => ()) == fa.map(_ => ())` up to
  *     F-shape. Witnesses that the aggregator's `B` doesn't alter the output F's *shape* — only its
  *     element values.
  *
  * Plus an instance-specific witness that the `reflect` produces the documented shape: singleton
  * for `List`, length-broadcast for `ZipList`, phantom-retag for `Const[M, *]`.
  */
class ReflectorInstancesSpec extends Specification with ScalaCheck:

  // Cogen[ZipList[Int]] — routes `ZipList[Int] => Int` functions through the underlying List's
  // Cogen. Needed so scalacheck can synthesise Arbitrary function values keyed on ZipList inputs.
  // Used by the R1 map-compat test's `f: ZipList[Int] => Int` generator.
  private given cogenZipList: Cogen[ZipList[Int]] =
    Cogen[List[Int]].contramap(_.value)

  // Cogen[Const[Int, Int]] — by the underlying monoid `Int`. Used by the R1 map-compat test's
  // `f: Const[Int, Int] => Int` generator.
  private given cogenConstIntInt: Cogen[Const[Int, Int]] =
    Cogen[Int].contramap(_.getConst)

  "Reflector[List]" should {
    val R = summon[Reflector[List]]

    "produce a singleton list (the documented cartesian-shape)" >> {
      forAll { (xs: List[Int], b: Int) =>
        R.reflect(xs)(_ => b) == List(b)
      }
    }

    "R1 map-compat: reflect(fa)(f).map(g) == reflect(fa)(fa_ => g(f(fa_)))" >> {
      forAll { (xs: List[Int], f: List[Int] => Int, g: Int => String) =>
        val lhs = R.map(R.reflect(xs)(f))(g)
        val rhs = R.reflect(xs)(xs0 => g(f(xs0)))
        lhs == rhs
      }
    }

    "R2 const-collapse shape: reflect(fa)(_ => b).map(_ => ()) is singleton-unit" >> {
      // For List, `reflect` always produces a singleton, so the "shape preserves" form
      // specialises to "singleton-unit" regardless of xs' original size.
      forAll { (xs: List[Int], b: Int) =>
        R.map(R.reflect(xs)(_ => b))(_ => ()) == List(())
      }
    }
  }

  "Reflector[ZipList]" should {
    val R = summon[Reflector[ZipList]]

    "broadcast f(fa) across fa.value.size (the documented zipping-shape)" >> {
      forAll { (xs: List[Int], b: Int) =>
        val fa = ZipList(xs)
        R.reflect(fa)(_ => b).value == List.fill(xs.size)(b)
      }
    }

    "R1 map-compat: reflect(fa)(f).map(g) == reflect(fa)(fa_ => g(f(fa_)))" >> {
      forAll { (xs: List[Int], f: ZipList[Int] => Int, g: Int => String) =>
        val fa = ZipList(xs)
        val lhs = R.map(R.reflect(fa)(f))(g)
        val rhs = R.reflect(fa)(fa_ => g(f(fa_)))
        lhs.value == rhs.value
      }
    }

    "R2 const-collapse shape: reflect(fa)(_ => b).map(_ => ()) has the same size as fa" >> {
      forAll { (xs: List[Int], b: Int) =>
        val fa = ZipList(xs)
        val reflected = R.map(R.reflect(fa)(_ => b))(_ => ())
        reflected.value.size == fa.value.size
      }
    }
  }

  "Kaleidoscope.apply (Unit 3 smoke)" should {
    import data.Kaleidoscope
    import data.Kaleidoscope.given
    import optics.Optic.*

    "round-trip .modify(identity) on a List kaleidoscope" >> {
      val k = Kaleidoscope.apply[List, Int]
      forAll { (xs: List[Int]) =>
        k.modify(identity[Int])(xs) == xs
      }
    }

    "apply .modify(_ + 1) element-wise on a List kaleidoscope" >> {
      val k = Kaleidoscope.apply[List, Int]
      k.modify(_ + 1)(List(1, 2, 3)) == List(2, 3, 4)
    }

    "apply .modify on a ZipList kaleidoscope" >> {
      val k = Kaleidoscope.apply[ZipList, Int]
      k.modify(_ * 10)(ZipList(List(1, 2, 3))).value == List(10, 20, 30)
    }

    "collect[List, Int] produces the List-singleton reflector output" >> {
      val k = Kaleidoscope.apply[List, Int]
      // The generic factory returns the raw reflected F[B] — for List (singleton), the result
      // of `.collect(_.sum)` is `List(sum)`.
      k.collect[List, Int](_.sum)(List(1, 2, 3, 4)) == List(10)
    }

    "collect[ZipList, Int] broadcasts the aggregate across the ZipList length" >> {
      val k = Kaleidoscope.apply[ZipList, Int]
      // ZipList's reflector broadcasts the aggregate to the input's length.
      k.collect[ZipList, Int](_.value.sum)(ZipList(List(1, 2, 3))).value == List(6, 6, 6)
    }
  }

  "Composer[Forgetful, Kaleidoscope] (Unit 4)" should {
    import data.Kaleidoscope
    import data.Kaleidoscope.given
    import optics.{Iso, Optic}
    import optics.Optic.*

    // Iso: wrap/unwrap a single Int as List[Int] (singleton list). The Iso's carrier is Forgetful;
    // composition with a Kaleidoscope through `.andThen` should morph the Iso through the bridge.
    val singletonIso: Optic[Int, Int, List[Int], List[Int], data.Forgetful] =
      Iso[Int, Int, List[Int], List[Int]](i => List(i), _.head)

    "Iso .andThen Kaleidoscope composes cleanly via Composer[Forgetful, Kaleidoscope]" >> {
      val kOverList: Optic[List[Int], List[Int], Int, Int, Kaleidoscope] =
        Kaleidoscope.apply[List, Int]
      val composed: Optic[Int, Int, Int, Int, Kaleidoscope] =
        singletonIso.andThen(kOverList)
      // .modify(_ * 3) on the composed optic — runs through the Iso (int -> List(int)), through
      // the Kaleidoscope's modify (each element * 3), then back through the Iso's reverse
      // (List.head).
      composed.modify(_ * 3)(7) == 21
    }
  }

  "Reflector[Const[Int, *]]" should {
    val R = summon[Reflector[Const[Int, *]]]

    "retag the phantom B side (the documented summation-shape)" >> {
      forAll { (m: Int, b: String) =>
        // `Const(m)` as `Const[Int, Int]`; `reflect(_)(_ => b)` gives `Const[Int, String]`;
        // the underlying monoid value `m` should survive intact.
        val fa: Const[Int, Int] = Const(m)
        R.reflect(fa)(_ => b).getConst == m
      }
    }

    "R1 map-compat: reflect(fa)(f).getConst == reflect(fa)(fa_ => g(f(fa_))).getConst" >> {
      forAll { (m: Int, f: Const[Int, Int] => Int, g: Int => String) =>
        val fa: Const[Int, Int] = Const(m)
        val lhs = R.map(R.reflect(fa)(f))(g)
        val rhs = R.reflect(fa)(fa_ => g(f(fa_)))
        lhs.getConst == rhs.getConst
      }
    }

    "R2 const-collapse shape: reflect(fa)(_ => b).getConst == m (unchanged monoid side)" >> {
      forAll { (m: Int, b: String) =>
        val fa: Const[Int, Int] = Const(m)
        R.reflect(fa)(_ => b).getConst == m
      }
    }
  }
