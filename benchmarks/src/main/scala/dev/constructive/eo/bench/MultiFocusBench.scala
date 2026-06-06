package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import cats.instances.list.*

import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given

/** `MultiFocus[List]` (via `fromLensF`) vs `MultiFocus[PSVec]` (via `Traversal.each[List, _]`) on
  * the same `Lens[Person, List[Phone]] → inner → Lens[Phone, Boolean]` chain. This isolates the
  * per-optic-machinery cost of the two carriers on the common "Lens over a List field" traversal
  * shape; `naive_*` is the hand-rolled `copy` + `List.map` baseline.
  *
  * The aggregator (`collect*`) and `MultiFocus.tuple` benches that used to share this file now live
  * in [[MultiFocusCollectBench]]; the duplicate ArraySeq PSVec path (identical to
  * [[PowerSeriesBench]]) was dropped — see plan 009, Phase 3.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class MultiFocusBench extends JmhDefaults:

  import MultiFocusBench.*

  @Param(Array("4", "32", "256", "1024"))
  var size: Int = uninitialized

  var person: Person = uninitialized

  private val phonesLens =
    EoLens[Person, List[Phone]](_.phones, (s, b) => s.copy(phones = b))

  private val isMobileLens =
    EoLens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b))

  // Path A: MultiFocus[List] via fromLensF — cardinality-preserving chunking; the
  // tuple2multifocus singleton fast-path fires on the inner Lens.
  private val mfPath =
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

  @Benchmark def eoModify_multiFocus: Person =
    mfPath.modify(!_)(person)

  @Benchmark def eoModify_powerEach: Person =
    psPath.modify(!_)(person)

  @Benchmark def naive_listMap: Person =
    person.copy(
      phones = person.phones.map(ph => ph.copy(isMobile = !ph.isMobile))
    )

object MultiFocusBench:

  case class Phone(isMobile: Boolean, number: String)

  case class Person(name: String, phones: List[Phone])
