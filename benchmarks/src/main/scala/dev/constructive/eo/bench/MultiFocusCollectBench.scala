package dev.constructive.eo
package bench

import cats.data.{Const, ZipList}
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap}
import dev.constructive.eo.optics.Optic
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** `MultiFocus` aggregator (`collect*`) and `tuple` benches — split out of the former
  * `MultiFocusBench` junk drawer (plan 009, Phase 3).
  *
  * These exercise the carrier-level reduction/broadcast machinery that absorbed the v1 Kaleidoscope
  * (`collectMap` / `collectList`) and Grate (`MultiFocus.tuple`) families, against hand-rolled
  * baselines with zero carrier allocation and zero Composer dispatch:
  *
  *   - **ZipList column-wise mean** via `collectMap` (Functor-broadcast): folds the whole ZipList
  *     to its mean and broadcasts it back across every position.
  *   - **`Const[Int, *]` summation** via `collectMap`: the phantom `A` slot makes `collectMap`
  *     reduce to a retag — pure carrier + dispatch overhead.
  *   - **List cartesian-singleton** via `collectList`: v1 `Reflector[List]` semantics —
  *     `List(agg(fa))` regardless of input length.
  *   - **`MultiFocus.tuple` 3-/6-slot modify** (absorbed Grate): the `MultiFocus[Function1[X, *]]`
  *     carrier reduces `.modify` to one `andThen` plus the per-slot tuple rebuild.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class MultiFocusCollectBench extends JmhDefaults:

  // ----- collectMap: ZipList column-wise mean (Functor-broadcast) -----
  private val zipListMF
      : Optic[ZipList[Double], ZipList[Double], Double, Double, MultiFocus[ZipList]] =
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
  private val constMF
      : Optic[Const[Int, Int], Const[Int, Int], Int, Int, MultiFocus[Const[Int, *]]] =
    MultiFocus.apply[Const[Int, *], Int]

  private val constData: Const[Int, Int] = Const(42)

  private val identityAgg: Const[Int, Int] => Int = _.getConst

  @Benchmark def eoCollectMap_constSum: Const[Int, Int] =
    constMF.collectMap[Int](identityAgg)(constData)

  @Benchmark def naive_constSum: Const[Int, Int] =
    Const(constData.getConst)

  // ----- collectList: List cartesian-singleton -----
  private val listMF: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
    MultiFocus.apply[List, Int]

  private val listData: List[Int] = (1 to 16).toList

  private val sumAgg: List[Int] => Int = _.sum

  @Benchmark def eoCollectList_listSum: List[Int] =
    listMF.collectList(sumAgg)(listData)

  @Benchmark def naive_listSum: List[Int] =
    List(listData.sum)

  // ----- MultiFocus.tuple: tuple3 / tuple6 modify (absorbed Grate) -----
  private val tripleMF = MultiFocus.tuple[(Double, Double, Double), Double]

  private val sextupleMF =
    MultiFocus.tuple[(Double, Double, Double, Double, Double, Double), Double]

  private val tripleData: (Double, Double, Double) = (1.0, 2.0, 3.0)

  private val sextupleData: (Double, Double, Double, Double, Double, Double) =
    (1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

  @Benchmark def eoModify_multiFocusTuple3: (Double, Double, Double) =
    tripleMF.modify(_ * 2.0)(tripleData)

  @Benchmark def naive_tuple3Rewrite: (Double, Double, Double) =
    (tripleData._1 * 2.0, tripleData._2 * 2.0, tripleData._3 * 2.0)

  @Benchmark def eoModify_multiFocusTuple6: (Double, Double, Double, Double, Double, Double) =
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
