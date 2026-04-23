package eo
package bench

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ArraySeq
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import eo.data.PowerSeries
import eo.optics.{Lens => EoLens, Traversal => EoTraversal}
import eo.optics.Optic.*

import cats.instances.arraySeq.*

/** PowerSeries-backed Traversal over a nested focus, paired against a hand-written iterative
  * baseline.
  *
  * `Traversal.each[ArraySeq, A]` (PowerSeries-backed) is EO-only — Monocle has no direct analog
  * because the PowerSeries carrier is the mechanism by which EO supports multi-focus traversals
  * with full `Composer` bridges into `Tuple2` (Lens), `Either` (Prism), and `Affine` (Optional).
  * This bench documents its runtime cost.
  *
  * Fixture: `Person(name, phones: ArraySeq[Phone])`. The optic chain drills
  * `Person → phones → each Phone → isMobile` (Boolean). Modify toggles every `isMobile` in-place.
  *
  * The `naive_*` comparator does the same work via `p.copy(phones = p.phones.map(ph =>
  * ph.copy(isMobile = !ph.isMobile)))` — this is essentially what the PowerSeries chain must
  * produce, so the gap shows the optic-machinery overhead for multi-focus write.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PowerSeriesBench:

  import PowerSeriesBench.*

  @Param(Array("4", "32", "256", "1024"))
  var size: Int = uninitialized

  var person: Person = uninitialized

  // Composed once at fixture setup — the bench measures modify only.
  // Cross-carrier `.andThen` auto-morphs Tuple2 ↔ PowerSeries; no
  // explicit `.morph[PowerSeries]` anywhere on the call site.
  private val personAllMobiles =
    EoLens[Person, ArraySeq[Phone]](_.phones, (s, b) => s.copy(phones = b))
      .andThen(EoTraversal.each[ArraySeq, Phone])
      .andThen(EoLens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b)))

  @Setup(Level.Iteration)
  def init(): Unit =
    person = Person(
      "Alice",
      ArraySeq.tabulate(size)(i => Phone(isMobile = i % 2 == 0, s"n-$i")),
    )

  @Benchmark def eoModify_powerEach: Person =
    personAllMobiles.modify(!_)(person)

  @Benchmark def naive_powerEach: Person =
    person.copy(
      phones = person.phones.map(ph => ph.copy(isMobile = !ph.isMobile))
    )

object PowerSeriesBench:

  case class Phone(isMobile: Boolean, number: String)

  case class Person(name: String, phones: ArraySeq[Phone])
