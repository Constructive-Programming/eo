package dev.constructive.eo

import org.specs2.mutable.Specification

import data.{Affine, IntArrBuilder, MultiFocusFromList, MultiFocusK, ObjArrBuilder, PSVec}

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

  // covers: PSVec.Empty has length 0 + isEmpty + toAnyArray empty,
  //   PSVec.Empty apply throws IndexOutOfBoundsException,
  //   PSVec.Empty head throws NoSuchElementException,
  //   PSVec.Empty slice returns Empty,
  //   PSVec.empty[A] == PSVec.empty[B] across type args (Nothing-arg unification);
  //   PSVec.Single has length 1 + apply(0) + head + toAnyArray exposes element,
  //   PSVec.Single apply(i>0) throws IndexOutOfBoundsException,
  //   PSVec.Single slice(0,1) returns self, slice(0,0) / slice(1,1) return Empty,
  //   PSVec.Single slice(-5,5) clamps to self;
  //   PSVec.Slice apply / head / length / toAnyArray over the backing,
  //   PSVec.Slice slice produces smaller Slice / Single / Empty,
  //   unsafeShareableArray returns backing on full slice (identity),
  //   unsafeShareableArray copies on partial slice (no aliasing);
  //   PSVec.unsafeWrap returns Empty / Single / Slice based on size;
  //   PSVec equality + hashCode treat same-shape vectors as equal regardless of variant,
  //   PSVec.toString uses "PSVec(...)" format
  "PSVec Empty / Single / Slice / unsafeWrap / equality+hashCode+toString — full surface" >> {
    val empty: PSVec[Nothing] = PSVec.empty[Nothing]
    val emptyLen = (empty.length === 0)
      .and(empty.isEmpty === true)
      .and(empty.toAnyArray.length === 0)
    val emptyApplyThrows =
      try { PSVec.empty[Nothing].apply(0); false }
      catch case _: IndexOutOfBoundsException => true
    val emptyHeadThrows =
      try { PSVec.empty[Nothing].head; false }
      catch case _: NoSuchElementException => true
    val emptySlice = PSVec.empty[Int].slice(0, 5) === PSVec.Empty
    val emptyAcrossTypeArgs = (PSVec.empty[Int] == PSVec.empty[String]) === true

    val s: PSVec[Int] = PSVec.singleton(42)
    val singleBasics = (s.length === 1)
      .and(s.apply(0) === 42)
      .and(s.head === 42)
      .and(s.toAnyArray.toList === List(42))
    val singleOvershoot =
      try { PSVec.singleton(1).apply(1); false }
      catch case _: IndexOutOfBoundsException => true
    val s2 = PSVec.singleton(7)
    val singleSlice = (s2.slice(0, 1) === s2)
      .and(s2.slice(0, 0) === PSVec.Empty)
      .and(s2.slice(1, 1) === PSVec.Empty)
      .and(s2.slice(-5, 5) === s2)

    val arrS: Array[Any] = Array("a", "b", "c", "d")
    val v: PSVec[String] = PSVec.unsafeWrap[String](arrS)
    val sliceBasics = (v.length === 4)
      .and(v.apply(2) === "c")
      .and(v.head === "a")
      .and(v.toAnyArray.toList === List("a", "b", "c", "d"))
    val arrI: Array[Any] = Array(1, 2, 3, 4, 5)
    val w: PSVec[Int] = PSVec.unsafeWrap[Int](arrI)
    val sliceOk = (w.slice(1, 4).toAnyArray.toList === List(2, 3, 4))
      .and(w.slice(2, 3).toAnyArray.toList === List(3))
      .and(w.slice(3, 3) === PSVec.Empty)
    val arrFull: Array[Any] = Array(1, 2, 3)
    val full: PSVec[Int] = PSVec.unsafeWrap[Int](arrFull)
    val identityOk = (full.unsafeShareableArray eq arrFull) === true
    val arrP: Array[Any] = Array(1, 2, 3, 4)
    val partial: PSVec[Int] = PSVec.unsafeWrap[Int](arrP).slice(1, 3)
    val copyOk = ((partial.unsafeShareableArray eq arrP) === false)
      .and(partial.unsafeShareableArray.toList === List(2, 3))

    val unsafeWrapEmpty = PSVec.unsafeWrap(Array.empty[Any]) === PSVec.Empty
    val singleWrap: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1))
    val unsafeWrapSingle = (singleWrap.length === 1).and(singleWrap.head === 1)
    val multiWrap: PSVec[Int] = PSVec.unsafeWrap[Int](Array(1, 2))
    val unsafeWrapMulti = multiWrap.length === 2

    val eqA: PSVec[Int] = PSVec.singleton(7)
    val eqB: PSVec[Int] =
      PSVec.unsafeWrap[Int](Array(7, 8)).slice(0, 1)
    val eqOk = (eqA === eqB).and(eqA.hashCode === eqB.hashCode)
    val toStringOk =
      PSVec.unsafeWrap[Int](Array(1, 2)).toString === "PSVec(1, 2)"

    emptyLen
      .and(emptyApplyThrows === true)
      .and(emptyHeadThrows === true)
      .and(emptySlice)
      .and(emptyAcrossTypeArgs)
      .and(singleBasics)
      .and(singleOvershoot === true)
      .and(singleSlice)
      .and(sliceBasics)
      .and(sliceOk)
      .and(identityOk)
      .and(copyOk)
      .and(unsafeWrapEmpty)
      .and(unsafeWrapSingle)
      .and(unsafeWrapMulti)
      .and(eqOk)
      .and(toStringOk)
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
    exact.append("a"); exact.append("b")
    val exactOk = (exact.size === 2).and(exact.freezeArr.toList === List("a", "b"))

    val growing = new ObjArrBuilder(2)
    growing.append("a"); growing.append("b")
    growing.append("c")
    val growOk = (growing.size === 3).and(growing.freezeArr.toList === List("a", "b", "c"))

    val partial = new ObjArrBuilder(4)
    partial.append("x")
    val out = partial.freezeArr
    val partialOk = (out.length === 1).and(out(0) === "x")

    exactOk.and(growOk).and(partialOk)
  }

  // covers: Affine.Hit.equals conjunction (Affine.scala:106) — equal snd with different b
  // must discriminate; an `||`-weakened equals would accept it.
  "Affine.Hit equality discriminates on each component independently" >> {
    val base = new Affine.Hit[(Int, String), Int]("s", 1)
    val sameSndDiffB = new Affine.Hit[(Int, String), Int]("s", 2)
    val diffSndSameB = new Affine.Hit[(Int, String), Int]("t", 1)
    (base == sameSndDiffB) must beFalse
    (base == diffSndSameB) must beFalse
  }

  // covers: Affine.Miss.equals (Affine.scala:83) — `fst == other.fst` weakened to `!=`
  // would flip both arms; no existing test ever compares Miss to Miss with the same shape.
  "Affine.Miss equality discriminates same-shape equal vs differing fst" >> {
    val missX = new Affine.Miss[(Int, String)](1)
    val missXAgain = new Affine.Miss[(Int, String)](1)
    val missY = new Affine.Miss[(Int, String)](2)
    (missX == missXAgain) must beTrue
    (missX == missY) must beFalse
  }

  // covers: PSVec.Slice.slice specialization guards (PSVec.scala:220-221) — `n == 0` /
  // `n == 1` weakened to `false` would skip the Empty/Single specialization and fall
  // through to an allocated zero/one-length Slice; structural `===` can't tell the
  // difference (PSVec equality ignores variant), so this needs reference/type checks.
  "PSVec.Slice.slice specializes to Empty (identity) and Single (type) at the n==0/n==1 edges" >> {
    val arr: Array[Any] = Array(1, 2, 3, 4, 5)
    val w: PSVec[Int] = PSVec.unsafeWrap[Int](arr)
    (w.slice(2, 2) must beTheSameAs(PSVec.empty[Int]))
      .and(w.slice(2, 3) must beAnInstanceOf[PSVec.Single[?]])
  }

  // covers: PSVec.Slice.unsafeShareableArray aliasing guard (PSVec.scala:230) —
  // `offset == 0 && length == arr.length` weakened to `||` would alias the FULL backing
  // array whenever offset==0, even on a partial slice. The existing offset!=0 test can't
  // discriminate this (both operands false either way); this pins offset==0, length<arr.length.
  "PSVec.Slice.unsafeShareableArray does not alias the full backing array on a partial offset-0 slice" >> {
    val backing: Array[Any] = Array(1, 2, 3, 4, 5)
    val partial: PSVec[Int] = PSVec.unsafeWrap[Int](backing).slice(0, 3)
    partial.unsafeShareableArray.length must beEqualTo(3)
  }

  // covers: MultiFocusK.pickSingletonOrThrow cardinality guard (MultiFocus.scala:245) —
  // weakened to `true` it silently returns the LAST element of a multi-focus instead of
  // raising the Composer cardinality error.
  "pickSingletonOrThrow returns the singleton and throws on any other cardinality" >> {
    MultiFocusK.pickSingletonOrThrow[List, Int](List(7), "test") must beEqualTo(7)
    MultiFocusK.pickSingletonOrThrow[List, Int](List(1, 2), "test") must
      throwAn[IllegalStateException]
    MultiFocusK.pickSingletonOrThrow[List, Int](List.empty[Int], "test") must
      throwAn[IllegalStateException]
  }

  // covers: the dead contract corners mutation testing flagged NoCoverage —
  //   Affine.Miss/Hit equals false-arms (Affine.scala:92/:107): cross-variant comparison,
  //   Hit.hashCode null guard (:109): hashing a null focus must be total,
  //   PSVec equals false-arm (PSVec.scala:71): comparison against a non-PSVec,
  //   Foldable[PSVec].foldRight (:132): the Eval loop's boundary and accumulation.
  "dead contract corners: cross-variant equality, null hashing, foldRight" >> {
    val miss = new Affine.Miss[(Int, String)](1)
    val hit = new Affine.Hit[(Int, String), Int]("s", 1)
    val crossOk = !miss.equals(hit) && !hit.equals(miss) && !miss.equals("x")
    val nullHit = new Affine.Hit[(Int, String), String]("s", null)
    val nullHashOk = nullHit.hashCode == nullHit.hashCode
    val vec = PSVec.from(List(1, 2, 3))
    val notAVecOk = !vec.equals("not a vector")
    val folded = cats
      .Foldable[PSVec]
      .foldRight(vec, cats.Eval.now(List.empty[Int]))((a, eb) => eb.map(a :: _))
      .value
    (crossOk, nullHashOk, notAVecOk, folded) must beEqualTo((true, true, true, List(1, 2, 3)))
  }

  // covers: the remaining cold paths —
  //   PSVec base toAnyArray (PSVec.scala:49): only Empty reaches it (Single/Slice
  //   override); boundary mutants make the vacuous loop call the throwing Empty.apply(0);
  //   MultiFocusFromList.forPSVec.fromList loop (MultiFocus.scala:106): a skipped loop
  //   wraps a null-filled array;
  //   Affine.Hit.hashCode null-guard `true` (Affine.scala:109): collapses to a
  //   snd-only hash — pinned-witness canary, not a contract claim.
  "cold paths: Empty.toAnyArray, fromList loop, Hit hash discrimination" >> {
    val emptyArrOk = PSVec.empty[Int].toAnyArray.length == 0
    val viaFromList = MultiFocusFromList.forPSVec.fromList(List(1, 2, 3))
    val fromListOk = viaFromList == PSVec.from(List(1, 2, 3))
    val h1 = new Affine.Hit[(Int, String), Int]("s", 1).hashCode
    val h2 = new Affine.Hit[(Int, String), Int]("s", 2).hashCode
    (emptyArrOk, fromListOk, h1 == h2) must beEqualTo((true, true, false))
  }
