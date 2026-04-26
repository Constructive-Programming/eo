package dev.constructive.eo
package bench

import scala.collection.immutable.ArraySeq
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*


import cats.instances.arraySeq.given

/** Sparse-Prism PowerSeries bench — half-hit, half-miss per element through a Prism sitting after a
  * Traversal.
  *
  * Fixture: `ArraySeq[Result]` where `Result` is a sum type `Ok(Int) | Err(String)` with a 50/50
  * distribution. The optic chain traverses every element and focuses the `Ok.value` via a Prism.
  * Modify increments each `Ok.value` by one; the `Err` cases pass through untouched.
  *
  * {{{
  *   Traversal.each[ArraySeq, Result].andThen(Prism[Result, Int](...))
  * }}}
  *
  * The ArraySeq container isolates the optic cost from List's pointer-chasing overhead —
  * `Functor[ArraySeq].map` is a native array walk, so the numbers here reflect the PowerSeries /
  * Prism machinery rather than any container traversal artefact.
  */
class PowerSeriesPrismBench extends JmhDefaults:

  import PowerSeriesPrismBench.*

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var results: ArraySeq[Result] = uninitialized

  private val okValues =
    EoTraversal
      .each[ArraySeq, Result]
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
    results =
      ArraySeq.tabulate(size)(i => if i % 2 == 0 then Result.Ok(i) else Result.Err(s"err-$i"))

  @Benchmark def eoModify_sparse: ArraySeq[Result] =
    okValues.modify(_ + 1)(results)

  @Benchmark def naive_sparse: ArraySeq[Result] =
    results.map {
      case Result.Ok(v) => Result.Ok(v + 1)
      case other        => other
    }

object PowerSeriesPrismBench:

  enum Result:
    case Ok(value: Int)
    case Err(msg: String)
