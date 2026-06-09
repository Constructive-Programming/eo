package dev.constructive.eo.spike

import cats.Functor
import dev.constructive.eo.data.PSVec
import higherkindness.droste.{scheme, Algebra, Coalgebra, Embed}
import io.circe.Json

/** Stage-0 gate (U0b): the SAME domain generation — unfold a numeric range
  * `(lo, hi)` into a balanced binary tree rendered as circe `Json` (int leaves,
  * 2-element arrays for branches) — written THREE ways, so the gate can compare
  * call-site ceremony honestly.
  *
  *   1. [[hand]]   — plain recursion ("what a user writes today").
  *   2. [[droste]] — droste `scheme.ana` over a hand-defined pattern functor.
  *   3. [[eo]]     — eo encoding-A: closure-carrying `ana`, full per-node closure
  *                   spelled out at the call site (the sketch the gate measures;
  *                   the real stack-safe engine is U1b).
  *
  * This is a generation example: there is no pre-existing `Json` to walk — the tree
  * is built from a flat seed. See the plan's U0b / falsifiable-gate criterion.
  */
object DomainUnfold:

  // ---- 1. Hand-written recursion (the baseline that is hard to beat on LOC) ----

  def hand(lo: Int, hi: Int): Json =
    if lo >= hi then Json.fromInt(lo)
    else
      val mid = lo + (hi - lo) / 2
      Json.arr(hand(lo, mid), hand(mid + 1, hi))

  // ---- 2. droste-interop: pattern functor + Functor + Embed + Coalgebra + ana ----
  //
  // The "honest cost" the gate measures: a user must reify the recursion point as a
  // pattern functor `RangeF[A]`, prove `Functor[RangeF]`, and supply `Embed[RangeF, Json]`
  // (how one layer becomes Json) plus the `Coalgebra` (how one seed expands one layer).

  enum RangeF[+A]:
    case Leaf(value: Int)
    case Branch(left: A, right: A)

  given Functor[RangeF] with
    def map[A, B](fa: RangeF[A])(f: A => B): RangeF[B] = fa match
      case RangeF.Leaf(v)      => RangeF.Leaf(v)
      case RangeF.Branch(l, r) => RangeF.Branch(f(l), f(r))

  given Embed[RangeF, Json] with
    def algebra: Algebra[RangeF, Json] = Algebra {
      case RangeF.Leaf(v)      => Json.fromInt(v)
      case RangeF.Branch(l, r) => Json.arr(l, r)
    }

  private val rangeCoalgebra: Coalgebra[RangeF, (Int, Int)] = Coalgebra {
    case (lo, hi) =>
      if lo >= hi then RangeF.Leaf(lo)
      else
        val mid = lo + (hi - lo) / 2
        RangeF.Branch((lo, mid), (mid + 1, hi))
  }

  val droste: ((Int, Int)) => Json = scheme.ana(rangeCoalgebra)

  // ---- 3. eo encoding-A: closure-carrying ana, full per-node closure at call site ----
  //
  // Minimal naive-recursive `ana` sketch (the real stack-safe ArrayDeque engine is U1b).
  // The point of the gate is the *call-site* ceremony, which is the closure below.

  private def anaA[Seed, S](coalg: Seed => (PSVec[Seed], PSVec[S] => S)): Seed => S =
    def go(seed: Seed): S =
      val (childSeeds, build) = coalg(seed)
      val n = childSeeds.length
      if n == 0 then build(PSVec.empty[S])
      else
        val out = new Array[AnyRef](n)
        var i = 0
        while i < n do
          out(i) = go(childSeeds(i)).asInstanceOf[AnyRef]
          i += 1
        build(PSVec.unsafeWrap[S](out))
    go

  val eo: ((Int, Int)) => Json =
    anaA[(Int, Int), Json] {
      case (lo, hi) =>
        if lo >= hi then (PSVec.empty[(Int, Int)], (_: PSVec[Json]) => Json.fromInt(lo))
        else
          val mid = lo + (hi - lo) / 2
          (
            PSVec.fromIterable(List((lo, mid), (mid + 1, hi))),
            (kids: PSVec[Json]) => Json.arr(kids(0), kids(1)),
          )
    }
