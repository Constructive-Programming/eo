package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given

import cats.instances.list.*

/** Side-by-side perf comparison: `AlgLens[List]` via `fromLensF` vs `PowerSeries` via
  * `Traversal.each[List, _]`, both routing through the same chain shape `Lens[Person, List[Phone]]
  * → inner → Lens[Phone, Boolean]`. This isolates the per-optic-machinery cost on the common "Lens
  * over a List field" traversal shape.
  *
  * `naive_*` does the same work via plain case-class copy + List.map, as the unconstrained
  * baseline.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AlgLensBench extends JmhDefaults:

  import AlgLensBench.*

  @Param(Array("4", "32", "256", "1024"))
  var size: Int = uninitialized

  var person: Person = uninitialized

  private val phonesLens =
    EoLens[Person, List[Phone]](_.phones, (s, b) => s.copy(phones = b))

  private val isMobileLens =
    EoLens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b))

  // Path A: AlgLens[List] via fromLensF — cardinality-preserving chunking.
  // `fromLensF(phonesLens)` already exposes the Phone focus at the
  // Person level, so the chain is just `(algLens).andThen(isMobileLens)`.
  private val algPath =
    MultiFocus.fromLensF(phonesLens).andThen(isMobileLens)

  // Path B: PowerSeries via Traversal.each[List, Phone] — specialized
  // PSVec-backed traversal carrier. The chain needs the outer lens
  // explicitly because Traversal.each[List, Phone] takes `List[Phone]`,
  // not `Person`.
  private val psPath =
    phonesLens
      .andThen(EoTraversal.each[List, Phone])
      .andThen(isMobileLens)

  @Setup(Level.Iteration)
  def init(): Unit =
    person = Person(
      "Alice",
      List.tabulate(size)(i => Phone(isMobile = i % 2 == 0, s"n-$i")),
    )

  @Benchmark def eoModify_algLens: Person =
    algPath.modify(!_)(person)

  @Benchmark def eoModify_powerEach: Person =
    psPath.modify(!_)(person)

  @Benchmark def naive_listMap: Person =
    person.copy(
      phones = person.phones.map(ph => ph.copy(isMobile = !ph.isMobile))
    )

object AlgLensBench:

  case class Phone(isMobile: Boolean, number: String)

  case class Person(name: String, phones: List[Phone])
