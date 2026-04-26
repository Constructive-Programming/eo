package dev.constructive.eo

import cats.data.{Const, ZipList}
import org.scalacheck.Prop.forAll
import org.scalacheck.Cogen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.language.implicitConversions

/** Reflector-law unit tests per the plan's D6 (Unit 1). These exist in place of a full discipline
  * `ReflectorTests` RuleSet: plan Open Question #6 defers promotion to discipline until the
  * Reflector instance surface grows beyond the v1 three shipped instances.
  *
  * '''2026-04-25 consolidation.''' 15 → 5 named blocks. Each Reflector instance previously had
  * three separate specs (shape-witness + R1 map-compat + R2 const-collapse). Collapsed each into
  * one composite test that asserts all three invariants. The Kaleidoscope (Unit 3) and Composer
  * (Unit 4) blocks merged similarly.
  */
class ReflectorInstancesSpec extends Specification with ScalaCheck:

  // Cogen[ZipList[Int]] — routes `ZipList[Int] => Int` functions through the underlying List's
  // Cogen. Used by R1 map-compat tests.
  private given cogenZipList: Cogen[ZipList[Int]] =
    Cogen[List[Int]].contramap(_.value)

  // Cogen[Const[Int, Int]] — by the underlying monoid `Int`. Used by R1 map-compat tests.
  private given cogenConstIntInt: Cogen[Const[Int, Int]] =
    Cogen[Int].contramap(_.getConst)

  // covers: produce a singleton list (the documented cartesian-shape), R1 map-compat,
  // R2 const-collapse shape: reflect(fa)(_ => b).map(_ => ()) is singleton-unit
  "Reflector[List]: shape (singleton) + R1 map-compat + R2 const-collapse" >> {
    val R = summon[Reflector[List]]

    val shape = forAll { (xs: List[Int], b: Int) => R.reflect(xs)(_ => b) == List(b) }
    val r1 = forAll { (xs: List[Int], f: List[Int] => Int, g: Int => String) =>
      R.map(R.reflect(xs)(f))(g) == R.reflect(xs)(xs0 => g(f(xs0)))
    }
    val r2 = forAll { (xs: List[Int], b: Int) =>
      R.map(R.reflect(xs)(_ => b))(_ => ()) == List(())
    }
    shape && r1 && r2
  }

  // covers: broadcast f(fa) across fa.value.size (zipping-shape), R1 map-compat,
  // R2 const-collapse shape: reflect(fa)(_ => b).map(_ => ()) has same size as fa
  "Reflector[ZipList]: shape (zipping) + R1 map-compat + R2 const-collapse" >> {
    val R = summon[Reflector[ZipList]]

    val shape = forAll { (xs: List[Int], b: Int) =>
      val fa = ZipList(xs)
      R.reflect(fa)(_ => b).value == List.fill(xs.size)(b)
    }
    val r1 = forAll { (xs: List[Int], f: ZipList[Int] => Int, g: Int => String) =>
      val fa = ZipList(xs)
      R.map(R.reflect(fa)(f))(g).value == R.reflect(fa)(fa_ => g(f(fa_))).value
    }
    val r2 = forAll { (xs: List[Int], b: Int) =>
      val fa = ZipList(xs)
      R.map(R.reflect(fa)(_ => b))(_ => ()).value.size == fa.value.size
    }
    shape && r1 && r2
  }

  // covers: retag the phantom B side (summation-shape), R1 map-compat,
  // R2 const-collapse shape: monoid side unchanged
  "Reflector[Const[Int, *]]: shape (phantom-retag) + R1 map-compat + R2 const-collapse" >> {
    val R = summon[Reflector[Const[Int, *]]]

    val shape = forAll { (m: Int, b: String) =>
      val fa: Const[Int, Int] = Const(m)
      R.reflect(fa)(_ => b).getConst == m
    }
    val r1 = forAll { (m: Int, f: Const[Int, Int] => Int, g: Int => String) =>
      val fa: Const[Int, Int] = Const(m)
      R.map(R.reflect(fa)(f))(g).getConst == R.reflect(fa)(fa_ => g(f(fa_))).getConst
    }
    val r2 = forAll { (m: Int, b: String) =>
      val fa: Const[Int, Int] = Const(m)
      R.reflect(fa)(_ => b).getConst == m
    }
    shape && r1 && r2
  }

  // covers: round-trip .modify(identity) on a List kaleidoscope, .modify(_ + 1)
  // element-wise on List, .modify on a ZipList kaleidoscope, collect[List, Int]
  // produces List-singleton output, collect[ZipList, Int] broadcasts across length
  "Kaleidoscope.apply: modify on List+ZipList, collect on List-singleton+ZipList-broadcast" >> {
    import data.Kaleidoscope
    import data.Kaleidoscope.given
    import optics.Optic.*

    val kList = Kaleidoscope.apply[List, Int]
    val identityOk = forAll { (xs: List[Int]) => kList.modify(identity[Int])(xs) == xs }
    val incOk = kList.modify(_ + 1)(List(1, 2, 3)) == List(2, 3, 4)

    val kZip = Kaleidoscope.apply[ZipList, Int]
    val zipOk = kZip.modify(_ * 10)(ZipList(List(1, 2, 3))).value == List(10, 20, 30)

    val collectListOk = kList.collect[List, Int](_.sum)(List(1, 2, 3, 4)) == List(10)
    val collectZipOk =
      kZip.collect[ZipList, Int](_.value.sum)(ZipList(List(1, 2, 3))).value == List(6, 6, 6)

    identityOk && incOk && zipOk && collectListOk && collectZipOk
  }

  // covers: Iso .andThen Kaleidoscope composes cleanly via Composer[Forgetful, Kaleidoscope]
  "Composer[Forgetful, Kaleidoscope] (Unit 4): Iso .andThen Kaleidoscope composes cleanly" >> {
    import data.Kaleidoscope
    import data.Kaleidoscope.given
    import optics.{Iso, Optic}
    import optics.Optic.*

    val singletonIso: Optic[Int, Int, List[Int], List[Int], data.Forgetful] =
      Iso[Int, Int, List[Int], List[Int]](i => List(i), _.head)
    val kOverList: Optic[List[Int], List[Int], Int, Int, Kaleidoscope] =
      Kaleidoscope.apply[List, Int]
    val composed: Optic[Int, Int, Int, Int, Kaleidoscope] =
      singletonIso.andThen(kOverList)
    composed.modify(_ * 3)(7) == 21
  }
