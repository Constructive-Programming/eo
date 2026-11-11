package dev.constructive.eo
package schemes

import scala.annotation.tailrec

import org.specs2.mutable.Specification

import data.PSVec
import optics.Plated
import generics.plate
import schemes.samples.Expr

/** JVM-only: the on-stack mechanism probes read `Thread.getStackTrace.length`, which Scala.js and
  * Scala Native stub out (an empty trace), so the examples cannot observe frame nesting there.
  */
class SchemesStackSpec extends Specification:

  private given Plated[Expr] = plate[Expr]

  // ----- on-stack MECHANISM below OnStackLimit (not just below-limit correctness) -----
  //
  // The tests above pin VALUES at depths beyond OnStackLimit (they'd pass even if the
  // engine always ran on the heap machine). These pin the MECHANISM claimed at
  // Schemes.scala:26-28 ("a `< 512`-deep on-stack fast path ... Shallow trees pay no frame
  // allocation"): well below the 512 limit, the engine must actually recurse on the JVM
  // call stack, not immediately hand every node to the heap `ArrayDeque` trampoline. Each
  // engine's `expand`/`coalg`/`alg` callback records `Thread.currentThread().getStackTrace
  // .length` on every invocation over a depth-50 linear chain; on-stack recursion nests one
  // JVM frame per tree level, so max-observed - min-observed grows large (empirically ~100+
  // for depth 50, two frames/level: the outer rec call + its inner `loop`). The
  // `depth >= OnStackLimit` -> `<` and `ConditionalExpression` -> `true` mutants make the
  // very first call take the heap branch, so every node is visited from inside the heap
  // loop's flat, roughly-constant-depth call chain instead — the delta collapses to a
  // handful of frames. Relative (delta) assertion only: an absolute frame count is JVM/JIT
  // fragile.

  private def stackDepth(): Int = Thread.currentThread().getStackTrace.length

  "hylo's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:66 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    var maxD = 0
    var minD = Int.MaxValue
    val expandSpine: Int => PSVec[Int] = n =>
      val d = stackDepth()
      if d > maxD then maxD = d
      if d < minD then minD = d
      if n <= 0 then PSVec.empty[Int] else PSVec.singleton(n - 1)
    val depthAlg: (Int, PSVec[Int]) => Int = (n, rs) => if n <= 0 then 0 else rs(0) + 1
    ((Schemes.hylo(expandSpine, depthAlg).get(50) == 50) && (maxD - minD >= 30)) must beTrue
  }

  "ana's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:124 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    var maxD = 0
    var minD = Int.MaxValue
    val buildNegCoalgProbed: Schemes.Coalg[Int, Expr] = n =>
      val d = stackDepth()
      if d > maxD then maxD = d
      if d < minD then minD = d
      if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(0.0))
      else (PSVec.singleton(n - 1), (ks: PSVec[Expr]) => Expr.Neg(ks(0)))
    val built: Expr = Schemes.ana(buildNegCoalgProbed).reverseGet(50)
    ((built != null) && (maxD - minD >= 30)) must beTrue
  }

  "cata's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:191 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    var maxD = 0
    var minD = Int.MaxValue
    @tailrec def negSpine50(e: Expr, i: Int): Expr =
      if i < 50 then negSpine50(Expr.Neg(e), i + 1) else e
    val e = negSpine50(Expr.Lit(0.0), 0)
    val depthAlgProbed: (Expr, PSVec[Int]) => Int = (node, kids) =>
      val d = stackDepth()
      if d > maxD then maxD = d
      if d < minD then minD = d
      node match
        case Expr.Lit(_) => 0
        case _           => kids(0) + 1
    ((Schemes.cata(depthAlgProbed).get(e) == 50) && (maxD - minD >= 30)) must beTrue
  }
