package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import io.circe.Json
import monocle.function.Plated as MPlated

import dev.constructive.eo.bench.fixture.{Bin, Expr, PlatedTrees}
import dev.constructive.eo.bench.fixture.PlatedTrees.given
import dev.constructive.eo.circe.platedJson // EO Plated[Json]
import dev.constructive.eo.optics.Plated as EoPlated

/** `Plated.universe` — enumerate every sub-term of a recursive structure — measured three ways
  * across three subjects:
  *
  *   - `eo*`      — cats-eo `Plated.universe` (stack-safe: `children` worklist over the optic).
  *   - `m*`       — Monocle `monocle.function.Plated.universe` over a hand-written `Plated` instance.
  *   - `visitor*` — the hand-rolled recursive collector you'd write without optics.
  *
  * Subjects (all sized by `n`, so a row reads across at equal node counts):
  *
  *   - `*Expr`  — a balanced `App` tree (normal depth ~log n) — the cookbook expression-tree shape.
  *   - `*Deep`  — a left-leaning `Bin` spine of depth `n` (the degenerate deep tree; this is where
  *                the trampolined `universe` earns its keep).
  *   - `*Json`  — a balanced `io.circe.Json` array tree (normal depth) via the universal
  *                `Plated[Json]`.
  *
  * Each method returns the sub-term count, forcing the full enumeration (Monocle's `universe` is a
  * lazy `LazyList`, so `.size` materialises it).
  *
  * '''No `mDeep`.''' Monocle's `universe` is not stack-safe: on the degenerate spine it
  * `StackOverflowError`s at depth ≳ 2048 (and, where it does survive — depth 1024 — runs ~700×
  * slower than EO, its lazy-`#:::` append going quadratic). So the deep subject compares EO against
  * the hand visitor only. EO's `Eval` trampoline clears the spine at every size here and at 100k in
  * the stack-safety test; the recursive visitor shares Monocle's call-stack ceiling and only
  * survives because `n` is bounded.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PlatedUniverseBench extends JmhDefaults:

  @Param(Array("64", "512", "4096"))
  var n: Int = uninitialized

  var expr: Expr = uninitialized
  var deep: Bin = uninitialized
  var json: Json = uninitialized

  @Setup(Level.Iteration)
  def init(): Unit =
    expr = PlatedTrees.balancedExpr(n)
    deep = PlatedTrees.deepBin(n)
    json = PlatedTrees.balancedJson(n)

  // ----- normal-depth Expr tree -----
  @Benchmark def eoExpr: Int = EoPlated.universe(expr).size
  @Benchmark def mExpr: Int = MPlated.universe(expr).size
  @Benchmark def visitorExpr: Int = PlatedTrees.visitExpr(expr).size

  // ----- degenerate deep Bin spine (Monocle omitted — it StackOverflows here) -----
  @Benchmark def eoDeep: Int = EoPlated.universe(deep).size
  @Benchmark def visitorDeep: Int = PlatedTrees.visitBin(deep).size

  // ----- normal-depth JSON tree -----
  @Benchmark def eoJson: Int = EoPlated.universe(json).size
  @Benchmark def mJson: Int = MPlated.universe(json).size
  @Benchmark def visitorJson: Int = PlatedTrees.visitJson(json).size
