package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import cats.data.{Const, ZipList}
import cats.instances.list.*

import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap}

/** Side-by-side perf comparison: `MultiFocus[List]` via `fromLensF` vs `PowerSeries` via
  * `Traversal.each[List, _]`, both routing through the same chain shape `Lens[Person, List[Phone]]
  * → inner → Lens[Phone, Boolean]`. This isolates the per-optic-machinery cost on the common "Lens
  * over a List field" traversal shape.
  *
  * `naive_*` does the same work via plain case-class copy + List.map, as the unconstrained
  * baseline.
  *
  * Two additional fixtures cover the Kaleidoscope-side `.collect` universal (now absorbed into
  * MultiFocus[F]):
  *
  *   - **ZipList column-wise mean** via `collectMap` (Functor-broadcast). Reduces the entire
  *     ZipList focus to its mean and broadcasts it back across every position.
  *   - **Const[Int, *] summation** via `collectMap`. The phantom `A` slot makes `.collectMap`
  *     reduce to a retag.
  *   - **List cartesian-singleton** via `collectList`. The v1 `Reflector[List]` semantics —
  *     produces a singleton list regardless of input length.
  *
  * `naive_*` baselines exercise the hand-rolled equivalents — zero carrier allocation, zero
  * Composer dispatch.
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

  // ----- collectMap: ZipList column-wise mean (Functor-broadcast) -----
  // Fixture absorbed from the deleted KaleidoscopeBench. The aggregator folds the entire ZipList
  // into a single mean; collectMap broadcasts it back through Functor[ZipList] so every position
  // carries the same mean.
  private val zipListMF
      : dev.constructive.eo.optics.Optic[ZipList[Double], ZipList[Double], Double, Double, MultiFocus[
        ZipList
      ]] =
    MultiFocus.apply[ZipList, Double]

  private val zipData: ZipList[Double] =
    ZipList((1 to 16).toList.map(_.toDouble))

  private val meanAgg: ZipList[Double] => Double =
    zl => zl.value.sum / zl.value.size.toDouble

  @Benchmark def eoCollectMap_zipMean: ZipList[Double] =
    zipListMF.collectMap[Double](meanAgg)(zipData)

  @Benchmark def naive_zipMeanBroadcast: ZipList[Double] =
    val mean = zipData.value.sum / zipData.value.size.toDouble
    ZipList(List.fill(zipData.value.size)(mean))

  // ----- collectMap: Const[Int, *] summation -----
  // Const[Int, Int]'s A slot is phantom; collectMap calls the aggregator once and Functor[Const]
  // is identity on the M side — measures pure carrier + dispatch overhead.
  private val constMF
      : dev.constructive.eo.optics.Optic[Const[Int, Int], Const[Int, Int], Int, Int, MultiFocus[
        Const[Int, *]
      ]] =
    MultiFocus.apply[Const[Int, *], Int]

  private val constData: Const[Int, Int] = Const(42)

  private val identityAgg: Const[Int, Int] => Int =
    _.getConst

  @Benchmark def eoCollectMap_constSum: Const[Int, Int] =
    constMF.collectMap[Int](identityAgg)(constData)

  @Benchmark def naive_constSum: Const[Int, Int] =
    Const(constData.getConst)

  // ----- collectList: List cartesian-singleton -----
  // Reproduces the v1 Reflector[List] semantics: the result is List(agg(fa)) regardless of input
  // length. This is the carrier-level evidence for the "function, not a law" cartesian variant.
  private val listMF
      : dev.constructive.eo.optics.Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
    MultiFocus.apply[List, Int]

  private val listData: List[Int] = (1 to 16).toList

  private val sumAgg: List[Int] => Int = _.sum

  @Benchmark def eoCollectList_listSum: List[Int] =
    listMF.collectList(sumAgg)(listData)

  @Benchmark def naive_listSum: List[Int] =
    List(listData.sum)

  // ----- Absorbed Grate fixtures: tuple3 / tuple6 modify -----
  //
  // Side-by-side comparison: the v1 GrateBench's `Grate.tuple` perf is now exercised through
  // `MultiFocus.tuple` (the absorbed factory) which carries `MultiFocus[Function1[Int, *]]`. The
  // `mfFunctor` for that F = `Function1[Int, *]` reduces `.modify` to one `andThen` allocation
  // plus the per-slot tuple rebuild — semantically identical to the deleted Grate.tuple body.
  //
  // 3-deep is the v1 baseline (`(Double, Double, Double)`); 6-deep doubles the slot count to
  // surface the per-slot constant factor.

  private val tripleMF = MultiFocus.tuple[(Double, Double, Double), Double]

  private val sextupleMF =
    MultiFocus.tuple[(Double, Double, Double, Double, Double, Double), Double]

  var tripleData: (Double, Double, Double) = (1.0, 2.0, 3.0)

  var sextupleData: (Double, Double, Double, Double, Double, Double) =
    (1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

  @Benchmark def eoModify_multiFocusTuple3: (Double, Double, Double) =
    tripleMF.modify(_ * 2.0)(tripleData)

  @Benchmark def naive_tuple3Rewrite: (Double, Double, Double) =
    (tripleData._1 * 2.0, tripleData._2 * 2.0, tripleData._3 * 2.0)

  @Benchmark def eoModify_multiFocusTuple6
      : (Double, Double, Double, Double, Double, Double) =
    sextupleMF.modify(_ * 2.0)(sextupleData)

  @Benchmark def naive_tuple6Rewrite: (Double, Double, Double, Double, Double, Double) =
    (
      sextupleData._1 * 2.0,
      sextupleData._2 * 2.0,
      sextupleData._3 * 2.0,
      sextupleData._4 * 2.0,
      sextupleData._5 * 2.0,
      sextupleData._6 * 2.0,
    )

object MultiFocusBench:

  case class Phone(isMobile: Boolean, number: String)

  case class Person(name: String, phones: List[Phone])
