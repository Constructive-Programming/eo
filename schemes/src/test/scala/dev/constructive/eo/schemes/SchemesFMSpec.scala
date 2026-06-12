package dev.constructive.eo
package schemes

import cats.data.State
import cats.{Eval, Id}
import org.specs2.mutable.Specification

import schemes.samples.{Bin, BinF}

/** The M-generic path (`cataFM` / `anaFM` / `hyloFM`, the tailRecM-lifted machine):
  *
  *   - Fast-path agreement laws — `M = Id` on the lifted machine == the pure citizens on the hybrid
  *     machine (a real cross-architecture pin: there is NO Id special-case).
  *   - The fused `AnaFM.andThen(CataFM)` == the run-then-run composition (extensional), resolved to
  *     the concrete member (ascription pin).
  *   - Stack-safety on `Eval` (200k spine; safety rides on M's `tailRecM` — tested, not asserted).
  *   - The linear-M boundary: `List` (a branching M) is documented UNSUPPORTED — the machine's
  *     mutable state is shared across branches; this test pins that the result is NOT the branching
  *     semantics a lawful reading would give, so a contract change cannot land silently.
  *   - The arbo-shaped acceptance example: children fetched effectfully (a counted `GetSellOptions`
  *     analogue in `State`), built and folded in ONE fused pass.
  */
class SchemesFMSpec extends Specification:

  // Deep (10^6 / 200k) examples: run one-at-a-time to bound peak heap (shared test JVM).
  sequential

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  private def expand(n: Int): BinF[Int] =
    if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  private val sumAlgSeed: (Int, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // ----- fast-path agreement (M = Id vs the pure hybrid machine) -------------

  "cataFM[Id].run == cataF.get (cross-architecture agreement)" >> {
    Schemes.cataFM[Id, BinF, Bin, Int]((s, fa) => sumAlg(s, fa)).run(tree) ===
      Schemes.cataF(sumAlg).get(tree)
  }

  "anaFM[Id].run == anaF.reverseGet" >> {
    Schemes.anaFM[Id, BinF, Int, Bin](n => expand(n)).run(6) ===
      Schemes.anaF[BinF, Int, Bin](expand).reverseGet(6)
  }

  "hyloFM[Id].run == hyloF.get" >> {
    Schemes.hyloFM[Id, BinF, Int, Int](n => expand(n), (s, fa) => sumAlgSeed(s, fa)).run(13) ===
      Schemes.hyloF[BinF, Int, Int](expand, sumAlgSeed).get(13)
  }

  // ----- fusion ---------------------------------------------------------------

  "AnaFM.andThen(CataFM) resolves to the fused member (ascription pin) and == run∘run" >> {
    val anaM = Schemes.anaFM[Id, BinF, Int, Bin](n => expand(n))
    val cataM = Schemes.cataFM[Id, BinF, Bin, Int]((s, fa) => sumAlg(s, fa))
    val fused: FoldFM[Id, Int, Int] = anaM.andThen(cataM) // generic andThen returns a bare Optic
    List(1, 2, 3, 5, 8, 13).map(fused.run) ===
      List(1, 2, 3, 5, 8, 13).map(seed => cataM.run(anaM.run(seed)))
  }

  // ----- stack-safety on Eval --------------------------------------------------

  // Depth bar: stack-safety needs >> the ~10k-frame JVM stack; 200k proves the
  // tailRecM-driven machine (the 10^6 SPACE bar lives with the pure-machine sweeps —
  // Eval's tailRecM adds per-event Either+Eval nodes, and the suites share one JVM).
  "hyloFM[Eval] is stack-safe on a 200k-deep spine (tailRecM-driven, no intermediate Bin)" >> {
    val Deep = 200_000
    def spine(n: Int): BinF[Int] =
      if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    def leafOrSpine(n: Int): BinF[Int] = if n < 0 then BinF.LeafF(0) else spine(n)
    val depthAlg: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    val run = Schemes
      .hyloFM[Eval, BinF, Int, Int](
        n => Eval.now(leafOrSpine(n)),
        (s, fa) => Eval.now(depthAlg(s, fa)),
      )
      .run(Deep)
    (run.value == Deep) must beTrue
  }

  // ----- the linear-M boundary --------------------------------------------------

  "List (a branching M) is UNSUPPORTED — the machine does not implement branching semantics" >> {
    // One fetch returns TWO alternative layers. A lawful branching interpretation
    // would yield two independent folds: List(1, 2). The machine's mutable state is
    // shared across List's branches, so it must NOT produce that — this pin fails
    // loudly if the contract ever changes (e.g. a persistent-state machine lands).
    def coalgM(n: Int): List[BinF[Int]] =
      if n == 9 then List(BinF.LeafF(1), BinF.LeafF(2)) else List(BinF.LeafF(n))
    val results = Schemes
      .hyloFM[List, BinF, Int, Int](coalgM, (_, fa) => List(sumAlgSeed(0, fa)))
      .run(9)
    (results != List(1, 2)) must beTrue
  }

  // ----- propagation of short-circuiting effects --------------------------------

  "cataFM[Option] propagates a mid-fold None" >> {
    // Algebra returns None for the inner branch (the Branch(Leaf(1), Leaf(2)) node) only.
    val algM: (Bin, BinF[Int]) => Option[Int] = (s, fa) =>
      s match
        case Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)) => None // force failure at this node
        case _                                    => Some(sumAlg(s, fa))
    Schemes.cataFM[Option, BinF, Bin, Int](algM).run(tree) === None
  }

  "anaFM[Option] propagates a mid-unfold None" >> {
    // Coalg returns None at seed 3 — forces failure mid-build.
    val coalgM: Int => Option[BinF[Int]] = n => if n == 3 then None else Some(expand(n))
    Schemes.anaFM[Option, BinF, Int, Bin](coalgM).run(6) === None
  }

  // ----- re-forcing the same Eval result is safe (Fix 1 regression guard) ------

  "re-forcing the same Eval result is safe (fresh state per force)" >> {
    val m = Schemes
      .hyloFM[Eval, BinF, Int, Int](
        n => Eval.now(expand(n)),
        (s, fa) => Eval.now(sumAlgSeed(s, fa)),
      )
      .run(6)
    val expected = Schemes.hyloF[BinF, Int, Int](expand, sumAlgSeed).get(6)
    (m.value === expected).and(m.value === expected)
  }

  "exception-interrupted force then re-force computes correctly (fresh state per force)" >> {
    // On the first invocation of the 3-node specific interior seed, throw via a one-shot flag.
    var thrown = false
    val algM: (Int, BinF[Int]) => Eval[Int] = (seed, fa) =>
      if seed == 3 && !thrown then
        thrown = true
        Eval.later(throw new RuntimeException("deliberate first-force failure"))
      else Eval.now(sumAlgSeed(seed, fa))
    val m = Schemes
      .hyloFM[Eval, BinF, Int, Int](n => Eval.now(expand(n)), algM)
      .run(6)
    // First force: throws
    val firstThrew =
      try { val _ = m.value; false }
      catch
        case _: RuntimeException => true
    // Second force: flag already set, should succeed with correct answer
    val expected = Schemes.hyloF[BinF, Int, Int](expand, sumAlgSeed).get(6)
    firstThrew.and(m.value === expected)
  }

  // ----- the arbo-shaped acceptance example -------------------------------------

  "arbo-shaped: effectful children (counted fetches), built and folded in one fused pass" >> {
    // GetSellOptions analogue: fetching a node's options is effectful — here State
    // counts the service calls, the way arbo's M wraps an options service.
    type Svc[T] = State[Int, T]
    def fetchOptions(n: Int): Svc[BinF[Int]] = State(calls => (calls + 1, expand(n)))

    val build = Schemes.anaFM[Svc, BinF, Int, Bin](fetchOptions)
    val best = Schemes.cataFM[Svc, BinF, Bin, Int]((s, fa) => State.pure(sumAlg(s, fa)))
    val selection: FoldFM[Svc, Int, Int] = build.andThen(best)

    val (calls, result) = selection.run(6).run(0).value
    // expand(6) yields 11 nodes (6 leaves of weight 1) — one fetch per node, one pass.
    (result === 6).and(calls === 11)
  }
