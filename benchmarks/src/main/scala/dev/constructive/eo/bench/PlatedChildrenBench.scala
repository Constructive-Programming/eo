// CPD-OFF — JMH harness header (imports / annotations / defaults) repeats across benchmarks.
package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.Plated as EoPlated
import org.openjdk.jmh.annotations.*

/** The `Plated.children` / `mapChildren` shapes from issues #169 / #172: a product whose only child
  * position is a `List[S]` field, and one `S` plus a `List[S]` field. `eo*` is the derived plate;
  * `hand*` is the code you'd write without optics.
  */
enum ChildrenExpr:
  case Leaf(n: Int)
  case Many(args: List[ChildrenExpr])
  case Pair(head: ChildrenExpr, tail: List[ChildrenExpr])

object ChildrenExpr:
  given EoPlated[ChildrenExpr] = plate[ChildrenExpr]

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PlatedChildrenBench extends JmhDefaults:
  // CPD-ON

  private val two = List[ChildrenExpr](ChildrenExpr.Leaf(1), ChildrenExpr.Leaf(2))
  private val many: ChildrenExpr = ChildrenExpr.Many(two)
  private val pair: ChildrenExpr = ChildrenExpr.Pair(ChildrenExpr.Leaf(0), two)
  private val id: ChildrenExpr => ChildrenExpr = identity
  private val plate = summon[EoPlated[ChildrenExpr]]

  @Benchmark def eoChildrenMany: List[ChildrenExpr] = EoPlated.children(many)

  @Benchmark def handChildrenMany: List[ChildrenExpr] =
    many match
      case ChildrenExpr.Many(args) => args
      case _                       => Nil

  @Benchmark def eoChildrenPair: List[ChildrenExpr] = EoPlated.children(pair)

  @Benchmark def handChildrenPair: List[ChildrenExpr] =
    pair match
      case ChildrenExpr.Pair(head, tail) => head :: tail
      case _                             => Nil

  @Benchmark def eoMapChildrenMany: ChildrenExpr = plate.mapChildren(many, id)

  @Benchmark def handMapChildrenMany: ChildrenExpr =
    many match
      case ChildrenExpr.Many(args) => ChildrenExpr.Many(args.map(id))
      case other                   => other
