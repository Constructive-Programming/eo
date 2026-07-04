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

/** `Plated` recursion — the read side (`universe`, enumerate every sub-term) and the write side
  * (`transform`, bottom-up rebuild) — measured three ways:
  *
  *   - `eo*` — cats-eo `Plated.universe` / `Plated.transform` (PSVec-native carrier, stack-safe via
  *     an explicit post-order stack machine / worklist on the heap).
  *   - `m*` — Monocle `monocle.function.Plated.universe` / `.transform` over a hand-written
  *     `Plated` instance.
  *   - `visitor*` — the hand-rolled recursive collector / rebuild you'd write without optics.
  *
  * Subjects (all sized by `n`, so a row reads across at equal node counts):
  *
  *   - `*Expr` — a balanced `App` tree (normal depth ~log n) — the cookbook expression-tree shape.
  *   - `*Deep` — a left-leaning `Bin` spine of depth `n` (the degenerate deep tree; this is where
  *     the trampolined recursion earns its keep).
  *   - `*Json` — a balanced `io.circe.Json` array tree (normal depth) via the universal
  *     `Plated[Json]`.
  *
  * `universe*` methods return the sub-term count, forcing the full enumeration (Monocle's
  * `universe` is a lazy `LazyList`, so `.size` materialises it). `transform*` methods rebuild every
  * node.
  *
  * '''No `*Deep` for Monocle.''' Monocle's `universe` / `transform` are not stack-safe: on the
  * degenerate spine they `StackOverflowError` at depth ≳ 2048 (and, where `universe` does survive —
  * depth 1024 — it runs ~700× slower than EO, its lazy-`#:::` append going quadratic). So the deep
  * subject compares EO against the hand visitor only. EO's heap-stack machine clears the spine at
  * every size here and at 100k in the stack-safety test; the recursive visitor shares Monocle's
  * call-stack ceiling and only survives because `n` is bounded.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PlatedBench extends JmhDefaults:

  @Param(Array("64", "512", "4096"))
  var n: Int = uninitialized

  var expr: Expr = uninitialized
  var deep: Bin = uninitialized
  var json: Json = uninitialized

  // Per-node rewrites that force a rebuild (touch a leaf, keep the spine shape).
  val shoutExpr: Expr => Expr = {
    case Expr.Var(name) => Expr.Var(name.toUpperCase)
    case other          => other
  }

  val bumpLeaf: Bin => Bin = {
    case Bin.Leaf(v) => Bin.Leaf(v + 1)
    case node        => node
  }

  @Setup(Level.Iteration)
  def init(): Unit =
    expr = PlatedTrees.balancedExpr(n)
    deep = PlatedTrees.deepBin(n)
    json = PlatedTrees.balancedJson(n)

  // ===== universe (read) =====

  // ----- normal-depth Expr tree -----
  @Benchmark def eoUniverseExpr: Int = EoPlated.universe(expr).size
  @Benchmark def mUniverseExpr: Int = MPlated.universe(expr).size
  @Benchmark def visitorUniverseExpr: Int = PlatedTrees.visitExpr(expr).size

  // ----- degenerate deep Bin spine (Monocle omitted — it StackOverflows here) -----
  @Benchmark def eoUniverseDeep: Int = EoPlated.universe(deep).size
  @Benchmark def visitorUniverseDeep: Int = PlatedTrees.visitBin(deep).size

  // ----- normal-depth JSON tree -----
  @Benchmark def eoUniverseJson: Int = EoPlated.universe(json).size
  @Benchmark def mUniverseJson: Int = MPlated.universe(json).size
  @Benchmark def visitorUniverseJson: Int = PlatedTrees.visitJson(json).size

  // ===== transform (write) =====

  @Benchmark def eoTransformExpr: Expr = EoPlated.transform(shoutExpr)(expr)
  @Benchmark def mTransformExpr: Expr = MPlated.transform(shoutExpr)(expr)
  @Benchmark def visitorTransformExpr: Expr = PlatedTrees.visitTransformExpr(shoutExpr)(expr)

  @Benchmark def eoTransformDeep: Bin = EoPlated.transform(bumpLeaf)(deep)
  @Benchmark def visitorTransformDeep: Bin = PlatedTrees.visitTransformBin(bumpLeaf)(deep)
