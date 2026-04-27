package dev.constructive.eo

import org.specs2.mutable.Specification

import data.{IntArrBuilder, ObjArrBuilder, PSVec}

/** Direct coverage for internal machinery that shipped without user-facing specs.
  *
  * '''2026-04-25 consolidation.''' 25 → 9 named blocks. The pre-image had:
  *
  *   - PSVec.Empty: 5 separate single-property specs collapsed into one. Each one tested a tiny
  *     invariant (length-0, apply-throws, head-throws, slice-empty, equals).
  *   - PSVec.Single: 3 separate specs collapsed into one (length, slice, apply-throws).
  *   - PSVec.Slice: 4 separate specs collapsed into one (apply/head/length, slice variants,
  *     unsafeShareableArray identity, copy-on-partial).
  *   - IntArrBuilder: 4 specs collapsed into one (append+freeze, grow, freeze-copies,
  *     initialCapacity=0).
  *   - ObjArrBuilder: 3 specs collapsed into one.
  *
  * '''2026-04-28 MultiFocus migration.''' The three Reflector-instance blocks (List / ZipList /
  * Const) were dropped — `Reflector` is deleted and `MultiFocus[F]` derives its `.collect` variants
  * directly from `Functor[F]` (broadcast) or per-instance `collectList` (cartesian singleton on
  * List). The carrier-level surface is exercised by the `MultiFocusSpec` PoC and the
  * discipline-laws blocks in `OpticsLawsSpec`.
  */
class InternalsCoverageSpec extends Specification:

  // ---- PSVec variants --------------------------------------------------

  // covers: have length 0 and toAnyRefArray empty, apply throws
  // IndexOutOfBoundsException, head throws NoSuchElementException, slice returns
  // Empty, equals itself and another empty
  "PSVec.Empty: length-0 / apply-throws / head-throws / slice / equals" >> {
    val e: PSVec[Nothing] = PSVec.empty[Nothing]
    val lengthOk = (e.length === 0)
      .and(e.isEmpty === true)
      .and(e.toAnyRefArray.length === 0)

    val applyThrows =
      try { PSVec.empty[Nothing].apply(0); false }
      catch case _: IndexOutOfBoundsException => true

    val headThrows =
      try { PSVec.empty[Nothing].head; false }
      catch case _: NoSuchElementException => true

    val sliceEmpty = PSVec.empty[Int].slice(0, 5) === PSVec.Empty
    val equalsAcrossTypeArgs = (PSVec.empty[Int] == PSVec.empty[String]) === true

    lengthOk
      .and(applyThrows === true)
      .and(headThrows === true)
      .and(sliceEmpty)
      .and(equalsAcrossTypeArgs)
  }

  // covers: have length 1, apply(0) returns the element, head returns the element,
  // toAnyRefArray; apply(i>0) throws IndexOutOfBoundsException; slice(0,1) returns
  // self, others return Empty
  "PSVec.Single: length-1 / apply / head / slice variants / overshoot-throws" >> {
    val s: PSVec[Int] = PSVec.singleton(42)
    val basics = (s.length === 1)
      .and(s.apply(0) === 42)
      .and(s.head === 42)
      .and(s.toAnyRefArray.toList === List(42))

    val applyOvershootThrows =
      try { PSVec.singleton(1).apply(1); false }
      catch case _: IndexOutOfBoundsException => true

    val s2 = PSVec.singleton(7)
    val sliceOk =
      (s2.slice(0, 1) === s2)
        .and(s2.slice(0, 0) === PSVec.Empty)
        .and(s2.slice(1, 1) === PSVec.Empty)
        .and(s2.slice(-5, 5) === s2)

    basics.and(applyOvershootThrows === true).and(sliceOk)
  }

  // covers: apply/head/length/toAnyRefArray work over the backing, slice produces
  // smaller Slice/Single/Empty, unsafeShareableArray returns backing on full slice,
  // unsafeShareableArray copies on partial slice
  "PSVec.Slice: apply/head/length, slice variants, unsafeShareableArray identity vs copy" >> {
    val arrS: Array[AnyRef] = Array("a", "b", "c", "d").map(_.asInstanceOf[AnyRef])
    val v: PSVec[String] = PSVec.unsafeWrap[String](arrS)
    val basics = (v.length === 4)
      .and(v.apply(2) === "c")
      .and(v.head === "a")
      .and(v.toAnyRefArray.toList === List("a", "b", "c", "d"))

    val arrI: Array[AnyRef] = Array(1, 2, 3, 4, 5).map(_.asInstanceOf[AnyRef])
    val w: PSVec[Int] = PSVec.unsafeWrap[Int](arrI)
    val sliceOk = (w.slice(1, 4).toAnyRefArray.toList === List(2, 3, 4))
      .and(w.slice(2, 3).toAnyRefArray.toList === List(3))
      .and(w.slice(3, 3) === PSVec.Empty)

    val arrFull: Array[AnyRef] = Array(1, 2, 3).map(_.asInstanceOf[AnyRef])
    val full: PSVec[Int] = PSVec.unsafeWrap[Int](arrFull)
    val identityOk = (full.unsafeShareableArray eq arrFull) === true

    val arrP: Array[AnyRef] = Array(1, 2, 3, 4).map(_.asInstanceOf[AnyRef])
    val partial: PSVec[Int] = PSVec.unsafeWrap[Int](arrP).slice(1, 3)
    val copyOk = ((partial.unsafeShareableArray eq arrP) === false)
      .and(partial.unsafeShareableArray.toList === List(2, 3))

    basics.and(sliceOk).and(identityOk).and(copyOk)
  }

  // covers: return Empty for empty arrays, Single for one-element arrays, Slice
  // otherwise
  "PSVec.unsafeWrap: returns Empty / Single / Slice based on size" >> {
    val empty = PSVec.unsafeWrap(Array.empty[AnyRef]) === PSVec.Empty
    val single: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1).map(_.asInstanceOf[AnyRef]))
    val singleOk = (single.length === 1).and(single.head === 1)
    val multi: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1, 2).map(_.asInstanceOf[AnyRef]))
    val multiOk = multi.length === 2
    empty.and(singleOk).and(multiOk)
  }

  // covers: treat same-shape vectors as equal regardless of variant; toString
  // uses PSVec(...) format
  "PSVec equality+hashCode across variants and toString format" >> {
    val s1: PSVec[Int] = PSVec.singleton(7)
    val s2: PSVec[Int] =
      PSVec.unsafeWrap[Int](Array(7, 8).map(_.asInstanceOf[AnyRef])).slice(0, 1)
    val eqOk = (s1 === s2).and(s1.hashCode === s2.hashCode)

    val v: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1, 2).map(_.asInstanceOf[AnyRef]))
    val toStringOk = v.toString === "PSVec(1, 2)"

    eqOk.and(toStringOk)
  }

  // ---- IntArrBuilder ---------------------------------------------------

  // covers: append and freeze exactly-sized, grow on overflow when capacity
  // exceeded, freeze copies when the internal array isn't exactly filled,
  // initialCapacity lower bound treats 0 as 1
  "IntArrBuilder: append+freeze, grow path, partial-freeze copy, capacity=0 lower bound" >> {
    val exact = new IntArrBuilder(initialCapacity = 2)
    exact.append(1); exact.append(2)
    val exactOk = (exact.size === 2).and(exact.freeze.toList === List(1, 2))

    val growing = new IntArrBuilder(initialCapacity = 2)
    growing.append(1); growing.append(2); growing.append(3)
    growing.append(4); growing.append(5)
    val growOk = (growing.size === 5).and(growing.freeze.toList === List(1, 2, 3, 4, 5))

    val partial = new IntArrBuilder(initialCapacity = 8)
    partial.append(10); partial.append(20)
    val out = partial.freeze
    val partialOk = (out.length === 2).and(out.toList === List(10, 20))

    val zeroCap = new IntArrBuilder(initialCapacity = 0)
    zeroCap.append(1); zeroCap.append(2)
    val zeroCapOk = zeroCap.freeze.toList === List(1, 2)

    exactOk.and(growOk).and(partialOk).and(zeroCapOk)
  }

  // ---- ObjArrBuilder ---------------------------------------------------

  // covers: append and freeze exactly-sized, grow on overflow, freeze copies
  // when internal array isn't exactly filled
  "ObjArrBuilder: append+freeze, grow path, partial-freeze copy" >> {
    val exact = new ObjArrBuilder(2)
    exact.append("a".asInstanceOf[AnyRef]); exact.append("b".asInstanceOf[AnyRef])
    val exactOk = (exact.size === 2).and(exact.freezeArr.toList === List("a", "b"))

    val growing = new ObjArrBuilder(2)
    growing.append("a".asInstanceOf[AnyRef]); growing.append("b".asInstanceOf[AnyRef])
    growing.append("c".asInstanceOf[AnyRef])
    val growOk = (growing.size === 3).and(growing.freezeArr.toList === List("a", "b", "c"))

    val partial = new ObjArrBuilder(4)
    partial.append("x".asInstanceOf[AnyRef])
    val out = partial.freezeArr
    val partialOk = (out.length === 1).and(out(0) === "x")

    exactOk.and(growOk).and(partialOk)
  }
