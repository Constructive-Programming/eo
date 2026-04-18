package eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import eo.bench.fixture.*
import eo.data.Forgetful.given
import eo.optics.{Getter => EoGetter}
import eo.optics.Optic.*

import monocle.{Getter => MGetter}

/** `Getter.get` at the leaf plus deep manual composition, paired
  * EO vs Monocle.
  *
  * **Scope note.** EO `Getter`s are `Optic[S, Unit, A, A, Forgetful]`;
  * their T slot is fixed to `Unit`, which means two Getters cannot
  * be composed via `Optic.andThen` (the outer B / inner T slots
  * don't align). For depth-3 / depth-6 reads the bench chains
  * `get` calls manually — `g4.get(g3.get(g2.get(g1.get(s))))` —
  * matching what a user would write. Monocle's `Getter.andThen`
  * (first-class on the trait) produces the equivalent composed
  * reader on its side.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class GetterBench:

  private val eoValue = EoGetter[Nested0, Int](_.value)
  private val mValue  = MGetter[Nested0, Int](_.value)

  private val eoN1 = EoGetter[Nested1, Nested0](_.n)
  private val eoN2 = EoGetter[Nested2, Nested1](_.n)
  private val eoN3 = EoGetter[Nested3, Nested2](_.n)
  private val eoN4 = EoGetter[Nested4, Nested3](_.n)
  private val eoN5 = EoGetter[Nested5, Nested4](_.n)
  private val eoN6 = EoGetter[Nested6, Nested5](_.n)

  private val mN1  = MGetter[Nested1, Nested0](_.n)
  private val mN2  = MGetter[Nested2, Nested1](_.n)
  private val mN3  = MGetter[Nested3, Nested2](_.n)
  private val mN4  = MGetter[Nested4, Nested3](_.n)
  private val mN5  = MGetter[Nested5, Nested4](_.n)
  private val mN6  = MGetter[Nested6, Nested5](_.n)

  private val mGet3 = mN3.andThen(mN2).andThen(mN1).andThen(mValue)
  private val mGet6 =
    mN6.andThen(mN5).andThen(mN4).andThen(mN3).andThen(mN2).andThen(mN1).andThen(mValue)

  private val leaf: Nested0 = Nested.DefaultLeaf
  private val d3:   Nested3 = Nested.Default3
  private val d6:   Nested6 = Nested.Default6

  @Benchmark def eoGet_0: Int = eoValue.get(leaf)
  @Benchmark def mGet_0:  Int = mValue.get(leaf)

  @Benchmark def eoGet_3: Int =
    eoValue.get(eoN1.get(eoN2.get(eoN3.get(d3))))
  @Benchmark def mGet_3:  Int =
    mGet3.get(d3)

  @Benchmark def eoGet_6: Int =
    eoValue.get(eoN1.get(eoN2.get(eoN3.get(eoN4.get(eoN5.get(eoN6.get(d6)))))))
  @Benchmark def mGet_6:  Int =
    mGet6.get(d6)
