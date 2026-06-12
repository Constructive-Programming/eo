package dev.constructive.eo

import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import optics.Traversal
import data.PSVec

/** Capacity-boundary and contract checks for the PSVec/builder machinery, driven through the public
  * composed-traversal surface.
  *
  * The grow-on-demand builders (`ObjArrBuilder`, `IntArrBuilder`) start at capacity 16 and the
  * default generators rarely cross that line through the slice-append path, so the grow-guard
  * conditionals survived mutation testing: a corrupted guard only crashes
  * (ArrayIndexOutOfBoundsException) once a single flatten step exceeds the remaining capacity. The
  * generators here straddle the boundary on purpose — inner lists up to 48 elements force
  * `appendAllFromPSVec`'s bulk-grow, outer lists up to 24 force the per-element counts builder past 16.
  */
class PSVecBoundarySpec extends Specification with ScalaCheck:

  private val outerTrav = Traversal.each[List, List[Int]]
  private val innerTrav = Traversal.each[List, Int]
  private val composed = outerTrav.andThen(innerTrav)

  // Sizes biased around the builders' initial capacity (16): empties, singletons, and
  // slices well past the growth boundary, in long outer lists.
  private val nested: Gen[List[List[Int]]] =
    val inner = Gen.oneOf(
      Gen.const(Nil),
      Gen.listOfN(1, Gen.choose(-10, 10)),
      Gen.choose(15, 48).flatMap(n => Gen.listOfN(n, Gen.choose(-10, 10))),
    )
    Gen.choose(0, 24).flatMap(n => Gen.listOfN(n, inner))

  "trav ∘ trav foldMap equals flatten-sum across the capacity boundary" >> {
    // covers: ObjArrBuilder.appendAllFromPSVec grow guard + grow loop (lines 32/38),
    //         IntArrBuilder append guard (line 15) — a corrupted guard crashes here.
    Prop.forAll(nested) { xs =>
      val got: Int = composed.foldMap(identity[Int])(xs)
      val want: Int = xs.map(_.sum).sum
      got == want
    }
  }

  "trav ∘ trav modify equals nested map across the capacity boundary" >> {
    // covers: the same builder paths on the write side, plus PSVec slice/wrap edges.
    Prop.forAll(nested) { xs =>
      val got: List[List[Int]] = composed.modify(_ + 1)(xs)
      val want: List[List[Int]] = xs.map(_.map(_ + 1))
      got == want
    }
  }

  "PSVec equality discriminates prefix-equal vectors of different lengths" >> {
    // covers: PSVec.equals length guard (line 67) — `!=` weakened to `>` or dropped
    // accepts a shorter prefix-equal vector.
    val shorter = PSVec.fromIterable(List(1, 2))
    val longer = PSVec.fromIterable(List(1, 2, 3))
    (shorter == longer) must beFalse
    (longer == shorter) must beFalse
  }

  "PSVec equality discriminates same-length different-content vectors" >> {
    // covers: PSVec.equals elementwise while-loop (line 67) — loop-skipping mutants
    // (condition → false / i > length) accept ANY same-length pair; the length guard
    // above never fires for these.
    val a = PSVec.fromIterable(List(1, 2, 3))
    val b = PSVec.fromIterable(List(1, 9, 3))
    (a == b) must beFalse
  }

  "unequal PSVecs of equal length hash differently (pinned witness)" >> {
    // covers: PSVec.hashCode while-loop (line 76) and null-guard `true` (line 78) —
    // loop-skipping mutants collapse every hash to 1, the always-null guard to a
    // length-only hash. The hashCode CONTRACT doesn't promise inequality; this
    // deterministic witness pair hashes apart under the real polynomial hash, so it is
    // a canary for hash-collapse, not a contract claim.
    val a = PSVec.fromIterable(List(1, 2))
    val b = PSVec.fromIterable(List(3, 4))
    (a.hashCode == b.hashCode) must beFalse
  }

  "hashing a null-bearing PSVec is total" >> {
    // covers: PSVec.hashCode null guard (line 78) — a flipped or dropped guard NPEs on
    // null elements.
    val withNull = PSVec.fromIterable(List("a", null, "b"))
    withNull.hashCode must beEqualTo(withNull.hashCode)
  }

  "structurally equal PSVecs hash equally (and hashing terminates)" >> {
    // covers: PSVec.hashCode loop condition (line 78) — the `true` mutant loops forever,
    // so merely hashing a non-empty vector detects it (timeout). Equal-hash asserted on
    // vectors built through different constructors so the contract is exercised, not an
    // implementation echo.
    val viaIterable = PSVec.fromIterable(List(1, 2, 3))
    val viaSlice = PSVec.fromIterable(List(0, 1, 2, 3, 4)).slice(1, 4)
    (viaIterable == viaSlice) must beTrue
    viaIterable.hashCode must beEqualTo(viaSlice.hashCode)
  }
