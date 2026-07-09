package dev.constructive.eo
package bench

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.generics.{lens, prism}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** Runtime-parity check for `eo-generics` derivation (plan 009, Phase 2).
  *
  * The macros emit `new S(...)` setters (not `s.copy(...)`) so they work uniformly for case classes
  * and enum cases. This bench confirms the *derived* optic costs nothing at runtime relative to a
  * hand-written EO optic and to raw `copy` / pattern-match — i.e. the derivation tax is entirely
  * compile-time.
  *
  * Three rows per operation:
  *   - `gen*` — the macro-derived `lens` / `prism`.
  *   - `hand*` — the equivalent hand-written `EoLens` / `EoPrism`.
  *   - `raw*` — plain `copy` / pattern-match, no optic at all (the floor).
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class GenericsBench extends JmhDefaults:

  private val person: GPerson = GPerson("Alice", 30)
  private val circle: GShape = GShape.Circle(2.0)
  private val square: GShape = GShape.Square(3.0)

  // ---- Lens on GPerson.age ------------------------------------------

  private val genAge = lens[GPerson](_.age)
  private val handAge = EoLens[GPerson, Int](_.age, (p, a) => p.copy(age = a))

  @Benchmark def genLensModify: GPerson = genAge.modify(_ + 1)(person)
  @Benchmark def handLensModify: GPerson = handAge.modify(_ + 1)(person)
  @Benchmark def rawLensModify: GPerson = person.copy(age = person.age + 1)

  @Benchmark def genLensGet: Int = genAge.get(person)
  @Benchmark def handLensGet: Int = handAge.get(person)
  @Benchmark def rawLensGet: Int = person.age

  // ---- Prism on GShape.Circle ---------------------------------------

  private val genCircle = prism[GShape, GShape.Circle]

  private val handCircle =
    EoPrism[GShape, GShape.Circle](
      {
        case c: GShape.Circle => Right(c)
        case other            => Left(other)
      },
      identity,
    )

  // getOption on the Either-carrier derived prism — lit up by the parity extension added to
  // `Optic` (the concrete `Prism`'s own member still wins at its static type).
  @Benchmark def genPrismGetHit: Option[GShape.Circle] = genCircle.getOption(circle)
  @Benchmark def handPrismGetHit: Option[GShape.Circle] = handCircle.getOption(circle)

  @Benchmark def rawPrismGetHit: Option[GShape.Circle] = circle match
    case c: GShape.Circle => Some(c)
    case _                => None

  @Benchmark def genPrismGetMiss: Option[GShape.Circle] = genCircle.getOption(square)
  @Benchmark def handPrismGetMiss: Option[GShape.Circle] = handCircle.getOption(square)

  // `.modify`: hit (Circle) rebuilds the focus; miss (Square) is the pass-through branch.
  @Benchmark def genPrismModifyHit: GShape = genCircle.modify(c => GShape.Circle(c.r + 1))(circle)
  @Benchmark def handPrismModifyHit: GShape = handCircle.modify(c => GShape.Circle(c.r + 1))(circle)

  @Benchmark def rawPrismModifyHit: GShape = circle match
    case c: GShape.Circle => GShape.Circle(c.r + 1)
    case other            => other

  @Benchmark def genPrismModifyMiss: GShape = genCircle.modify(c => GShape.Circle(c.r + 1))(square)

  @Benchmark def handPrismModifyMiss: GShape =
    handCircle.modify(c => GShape.Circle(c.r + 1))(square)
