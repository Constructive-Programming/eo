package dev.constructive.eo
package schemes

import scala.annotation.tailrec

import org.specs2.mutable.Specification

import optics.Plated
import generics.plate
import schemes.samples.Expr
import SchemeFixtures.*

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

  /** Records the min / max JVM stack depth seen across calls of the functions it wraps. */
  final private class Probe:
    var maxD = 0
    var minD = Int.MaxValue
    def spread: Int = maxD - minD

    def wrap[A, B](f: A => B): A => B = a =>
      tick()
      f(a)

    def wrap2[A, B, C](f: (A, B) => C): (A, B) => C = (a, b) =>
      tick()
      f(a, b)

    private def tick(): Unit =
      val d = Thread.currentThread().getStackTrace.length
      if d > maxD then maxD = d
      if d < minD then minD = d

  "hylo's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:66 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    val probe = Probe()
    ((Schemes.hylo(probe.wrap(spineExpand), spineDepth).get(50) == 50) &&
      (probe.spread >= 30)) must beTrue
  }

  "ana's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:124 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    val probe = Probe()
    val built: Expr = Schemes.ana(probe.wrap(negCoalg)).reverseGet(50)
    ((built != null) && (probe.spread >= 30)) must beTrue
  }

  "cata's on-stack fast path recurses on the JVM call stack below OnStackLimit" >> {
    // covers: Schemes.scala:191 `depth >= OnStackLimit` -> `<` / ConditionalExpression -> `true`
    val probe = Probe()
    @tailrec def negSpine50(e: Expr, i: Int): Expr =
      if i < 50 then negSpine50(Expr.Neg(e), i + 1) else e
    val e = negSpine50(Expr.Lit(0.0), 0)
    ((Schemes.cata(probe.wrap2(negDepth)).get(e) == 50) && (probe.spread >= 30)) must beTrue
  }
