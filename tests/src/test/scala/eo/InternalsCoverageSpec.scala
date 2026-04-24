package eo

import data.{IntArrBuilder, ObjArrBuilder, PSVec}

import cats.data.{Const, ZipList}
import cats.instances.list.given

import org.specs2.mutable.Specification

/** Direct coverage for internal machinery that shipped without user-facing specs.
  *
  * Focused on three areas flagged by the code-quality review + coverage report:
  *
  *   1. [[PSVec]] variants (`Empty`, `Single`, `Slice`) — the array-backed focus vector that
  *      `PowerSeries.assoc` relies on. Many edge cases (slice boundaries, overshoot, error throws)
  *      are exercised only in composition hot paths that coverage doesn't naturally reach. Direct
  *      tests here.
  *   2. [[IntArrBuilder]] / [[ObjArrBuilder]] grow-on-demand arrays — `PowerSeries.assoc` pre-sizes
  *      them but the dynamic-grow path never fires from optics tests. Exercise directly with
  *      capacities forced to grow.
  *   3. [[eo.Reflector]] instance surface — `ap`, `pure`, `map`, `product` delegation overrides for
  *      `List` / `ZipList` / `Const[M, *]`. Kaleidoscope specs exercise `reflect` but not the cats
  *      `Apply` overrides.
  */
class InternalsCoverageSpec extends Specification:

  // ---- PSVec variants --------------------------------------------------

  "PSVec.Empty" should {
    "have length 0 and toAnyRefArray empty" >> {
      val e: PSVec[Nothing] = PSVec.empty[Nothing]
      e.length === 0
      e.isEmpty === true
      e.toAnyRefArray.length === 0
    }

    "apply throws IndexOutOfBoundsException" >> {
      try {
        PSVec.empty[Nothing].apply(0)
        ko("expected IndexOutOfBoundsException")
      } catch {
        case _: IndexOutOfBoundsException => ok
      }
    }

    "head throws NoSuchElementException" >> {
      try {
        PSVec.empty[Nothing].head
        ko("expected NoSuchElementException")
      } catch {
        case _: NoSuchElementException => ok
      }
    }

    "slice returns Empty" >> {
      val e = PSVec.empty[Int]
      e.slice(0, 5) === PSVec.Empty
    }

    "equals itself and another empty" >> {
      (PSVec.empty[Int] == PSVec.empty[String]) === true
    }
  }

  "PSVec.Single" should {
    "have length 1, apply(0) returns the element, head returns the element" >> {
      val s: PSVec[Int] = PSVec.singleton(42)
      s.length === 1
      s.apply(0) === 42
      s.head === 42
      s.toAnyRefArray.toList === List(42)
    }

    "apply(i>0) throws IndexOutOfBoundsException" >> {
      try {
        PSVec.singleton(1).apply(1)
        ko("expected IndexOutOfBoundsException")
      } catch {
        case _: IndexOutOfBoundsException => ok
      }
    }

    "slice(0, 1) returns self; other slices return Empty" >> {
      val s = PSVec.singleton(7)
      s.slice(0, 1) === s
      s.slice(0, 0) === PSVec.Empty
      s.slice(1, 1) === PSVec.Empty
      s.slice(-5, 5) === s
    }
  }

  "PSVec.Slice (via unsafeWrap with length > 1)" should {
    "apply / head / length / toAnyRefArray work over the backing" >> {
      val arr: Array[AnyRef] = Array("a", "b", "c", "d").map(_.asInstanceOf[AnyRef])
      val v: PSVec[String] = PSVec.unsafeWrap[String](arr)
      v.length === 4
      v.apply(2) === "c"
      v.head === "a"
      v.toAnyRefArray.toList === List("a", "b", "c", "d")
    }

    "slice produces smaller Slice, Single, or Empty based on bounds" >> {
      val arr: Array[AnyRef] = Array(1, 2, 3, 4, 5).map(_.asInstanceOf[AnyRef])
      val v: PSVec[Int] = PSVec.unsafeWrap[Int](arr)
      v.slice(1, 4).toAnyRefArray.toList === List(2, 3, 4) // Slice
      v.slice(2, 3).toAnyRefArray.toList === List(3) // Single
      v.slice(3, 3) === PSVec.Empty // Empty
    }

    "unsafeShareableArray returns the backing when the slice covers it fully" >> {
      val arr: Array[AnyRef] = Array(1, 2, 3).map(_.asInstanceOf[AnyRef])
      val full: PSVec[Int] = PSVec.unsafeWrap[Int](arr)
      (full.unsafeShareableArray eq arr) === true // reference identity
    }

    "unsafeShareableArray copies when the slice is partial" >> {
      val arr: Array[AnyRef] = Array(1, 2, 3, 4).map(_.asInstanceOf[AnyRef])
      val partial: PSVec[Int] = PSVec.unsafeWrap[Int](arr).slice(1, 3)
      (partial.unsafeShareableArray eq arr) === false
      partial.unsafeShareableArray.toList === List(2, 3)
    }
  }

  "PSVec.unsafeWrap" should {
    "return Empty for empty arrays, Single for one-element arrays, Slice otherwise" >> {
      PSVec.unsafeWrap(Array.empty[AnyRef]) === PSVec.Empty
      val single: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1).map(_.asInstanceOf[AnyRef]))
      single.length === 1
      single.head === 1
      val multi: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1, 2).map(_.asInstanceOf[AnyRef]))
      multi.length === 2
    }
  }

  "PSVec equality and hashCode" should {
    "treat same-shape vectors as equal regardless of variant" >> {
      val s1: PSVec[Int] = PSVec.singleton(7)
      val s2: PSVec[Int] =
        PSVec.unsafeWrap[Int](Array(7, 8).map(_.asInstanceOf[AnyRef])).slice(0, 1)
      s1 === s2
      s1.hashCode === s2.hashCode
    }

    "toString uses PSVec(...) format" >> {
      val v: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1, 2).map(_.asInstanceOf[AnyRef]))
      v.toString === "PSVec(1, 2)"
    }
  }

  // ---- IntArrBuilder ---------------------------------------------------

  "IntArrBuilder" should {
    "append and freeze exactly-sized" >> {
      val b = new IntArrBuilder(initialCapacity = 2)
      b.append(1)
      b.append(2)
      b.size === 2
      b.freeze.toList === List(1, 2)
    }

    "grow on overflow when capacity exceeded" >> {
      val b = new IntArrBuilder(initialCapacity = 2)
      b.append(1)
      b.append(2)
      b.append(3) // triggers grow
      b.append(4)
      b.append(5) // triggers grow again
      b.size === 5
      b.freeze.toList === List(1, 2, 3, 4, 5)
    }

    "freeze copies when the internal array isn't exactly filled" >> {
      val b = new IntArrBuilder(initialCapacity = 8)
      b.append(10)
      b.append(20)
      val out = b.freeze
      out.length === 2
      out.toList === List(10, 20)
    }

    "initialCapacity lower bound treats 0 as 1" >> {
      val b = new IntArrBuilder(initialCapacity = 0)
      b.append(1)
      b.append(2)
      b.freeze.toList === List(1, 2)
    }
  }

  // ---- ObjArrBuilder ---------------------------------------------------

  "ObjArrBuilder" should {
    "append and freeze exactly-sized" >> {
      val b = new ObjArrBuilder(2)
      b.append("a".asInstanceOf[AnyRef])
      b.append("b".asInstanceOf[AnyRef])
      b.size === 2
      b.freezeArr.toList === List("a", "b")
    }

    "grow on overflow" >> {
      val b = new ObjArrBuilder(2)
      b.append("a".asInstanceOf[AnyRef])
      b.append("b".asInstanceOf[AnyRef])
      b.append("c".asInstanceOf[AnyRef]) // grow
      b.size === 3
      b.freezeArr.toList === List("a", "b", "c")
    }

    "freeze copies when internal array isn't exactly filled" >> {
      val b = new ObjArrBuilder(4)
      b.append("x".asInstanceOf[AnyRef])
      val out = b.freezeArr
      out.length === 1
      out(0) === "x"
    }
  }

  // ---- Reflector instance surface --------------------------------------

  "Reflector[List]" should {
    "pure wraps in a singleton, ap combines cartesian" >> {
      val R = Reflector.forList
      R.pure(1) === List(1)
      R.ap(List((_: Int) + 1, (_: Int) * 10))(List(1, 2)) === List(2, 3, 10, 20)
      R.map(List(1, 2, 3))(_ + 1) === List(2, 3, 4)
      R.product(List(1, 2), List("a", "b")) === List((1, "a"), (1, "b"), (2, "a"), (2, "b"))
      R.reflect(List(1, 2, 3))(_.sum) === List(6)
    }
  }

  "Reflector[ZipList]" should {
    "ap zips, map zips, reflect length-aware broadcasts" >> {
      val R = Reflector.forZipList
      R.ap(ZipList(List((_: Int) * 2, (_: Int) + 10)))(ZipList(List(3, 5))).value ===
        List(6, 15)
      R.map(ZipList(List(1, 2, 3)))(_ * 10).value === List(10, 20, 30)
      R.product(ZipList(List(1, 2)), ZipList(List("a", "b"))).value === List((1, "a"), (2, "b"))
      R.reflect(ZipList(List(1.0, 2.0, 3.0)))(zl => zl.value.sum / zl.value.size.toDouble)
        .value === List(2.0, 2.0, 2.0)
    }
  }

  "Reflector[Const[Int, *]]" should {
    "ap / map / product / reflect delegate through cats Const" >> {
      val R = Reflector.forConst[Int]
      R.pure(42).getConst === 0 // Monoid.empty on Int
      R.ap(Const[Int, Int => Int](5))(Const[Int, Int](3)).getConst === 8
      R.map(Const[Int, String](7))(_.length).getConst === 7
      R.product(Const[Int, String](2), Const[Int, Int](3)).getConst === 5
      // reflect is phantom — returns the Const cast to a fresh phantom focus type.
      R.reflect[String, Int](Const[Int, String](99))(_ => 0).getConst === 99
    }
  }
