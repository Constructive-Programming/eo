package eo
package bench

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import eo.data.PowerSeries
import eo.optics.{Prism => EoPrism, Traversal => EoTraversal}
import eo.optics.Optic.*

import cats.instances.list.given

/** Sparse-Prism PowerSeries bench — half-hit, half-miss per element through a Prism sitting
  * after a Traversal.
  *
  * Fixture: `List[Result]` where `Result` is a sum type `Ok(Int) | Err(String)` with a 50/50
  * distribution. The optic chain traverses every element and focuses the `Ok.value` via a
  * Prism. Modify increments each `Ok.value` by one; the `Err` cases pass through untouched.
  *
  * {{{
  *   Traversal.each[List, Result].andThen(Prism[Result, Int](...))
  * }}}
  *
  * This is the shape where a trie carrier's `PSEmpty` node could short-circuit half the
  * per-element work: the current flat design still allocates per-element carrier structure for
  * misses and filters them at `from` time, whereas a trie could mark the miss once and skip
  * its subtree entirely.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PowerSeriesPrismBench:

  import PowerSeriesPrismBench.*

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var results: List[Result] = uninitialized

  private val okValues =
    EoTraversal
      .each[List, Result]
      .andThen(
        EoPrism[Result, Int](
          {
            case Result.Ok(v)  => Right(v)
            case e: Result.Err => Left(e)
          },
          Result.Ok(_),
        )
      )

  @Setup(Level.Iteration)
  def init(): Unit =
    results = List.tabulate(size)(i =>
      if i % 2 == 0 then Result.Ok(i) else Result.Err(s"err-$i")
    )

  @Benchmark def eoModify_sparse: List[Result] =
    okValues.modify(_ + 1)(results)

  @Benchmark def naive_sparse: List[Result] =
    results.map {
      case Result.Ok(v) => Result.Ok(v + 1)
      case other        => other
    }

object PowerSeriesPrismBench:

  enum Result:
    case Ok(value: Int)
    case Err(msg: String)
