package dev.constructive.eo
package bench

import scala.collection.immutable.ArraySeq
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import cats.instances.arraySeq.*

import dev.constructive.eo.bench.fixture.{Person, Phone}
import dev.constructive.eo.data.MultiFocus.given

import monocle.{Lens => MLens, Traversal => MTraversal}

/** PowerSeries-backed Traversal over a nested focus, paired against a hand-written iterative
  * baseline and the equivalent Monocle composition.
  *
  * EO's PowerSeries carrier (`MultiFocus[PSVec]`) is an internal mechanism, but the *operation*
  * this bench measures — a `Lens → Traversal → Lens` chain — is equally expressible in Monocle
  * (`Lens.andThen(Traversal.fromTraverse).andThen(Lens)`), so the `monocle_*` row is a fair peer
  * alongside the hand-written `naive_*` baseline.
  *
  * Fixture: `Person(name, phones: ArraySeq[Phone])`. The optic chain drills
  * `Person → phones → each Phone → isMobile` (Boolean). Modify toggles every `isMobile` in-place.
  *
  * The `naive_*` comparator does the same work via `p.copy(phones = p.phones.map(ph =>
  * ph.copy(isMobile = !ph.isMobile)))` — this is essentially what the chain must produce, so the
  * gap shows each library's optic-machinery overhead for multi-focus write.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PowerSeriesBench extends JmhDefaults:

  @Param(Array("4", "16", "64", "256", "1024", "4096"))
  var size: Int = uninitialized

  var person: Person = uninitialized

  // Composed once at fixture setup — the bench measures modify only.
  // Cross-carrier `.andThen` auto-morphs Tuple2 ↔ PowerSeries; no
  // explicit `.morph[PowerSeries]` anywhere on the call site.
  private val personAllMobiles =
    EoLens[Person, ArraySeq[Phone]](_.phones, (s, b) => s.copy(phones = b))
      .andThen(EoTraversal.each[ArraySeq, Phone])
      .andThen(EoLens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b)))

  // Same chain in Monocle: Lens → Traversal.fromTraverse → Lens, composing to a Traversal.
  private val mPersonAllMobiles: MTraversal[Person, Boolean] =
    MLens[Person, ArraySeq[Phone]](_.phones)(b => s => s.copy(phones = b))
      .andThen(MTraversal.fromTraverse[ArraySeq, Phone])
      .andThen(MLens[Phone, Boolean](_.isMobile)(b => s => s.copy(isMobile = b)))

  @Setup(Level.Iteration)
  def init(): Unit =
    person = Person(
      "Alice",
      ArraySeq.tabulate(size)(i => Phone(isMobile = i % 2 == 0, s"n-$i")),
    )
    // Correctness guard: all three paths produce the same modified record.
    val eoR = personAllMobiles.modify(!_)(person)
    require(
      mPersonAllMobiles.modify(!_)(person) == eoR && naive_powerEach == eoR,
      "PowerSeriesBench: monocle / naive disagree with eo",
    )

  @Benchmark def eoModify_powerEach: Person =
    personAllMobiles.modify(!_)(person)

  @Benchmark def naive_powerEach: Person =
    person.copy(
      phones = person.phones.map(ph => ph.copy(isMobile = !ph.isMobile))
    )

  @Benchmark def monocle_powerEach: Person =
    mPersonAllMobiles.modify(!_)(person)
