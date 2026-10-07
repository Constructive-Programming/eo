package dev.constructive.eo
package bench

import cats.data.{Const, ZipList}
import cats.{Functor, Representable}
import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.{collectList, collectMap}
import dev.constructive.eo.optics.{Indexed, Optic}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** `MultiFocus` aggregator (`collect*`) and indexed tuple benches — split out of the former
  * `MultiFocusBench` junk drawer (plan 009, Phase 3).
  *
  * These exercise container reduction/broadcast (`collectMap` / `collectList`) and `Indexed`
  * fixed-index rebuilding, against hand-rolled baselines with zero carrier allocation and zero
  * Composer dispatch:
  *
  *   - **ZipList column-wise mean** via `collectMap` (Functor-broadcast): folds the whole ZipList
  *     to its mean and broadcasts it back across every position.
  *   - **`Const[Int, *]` summation** via `collectMap`: the phantom `A` slot makes `collectMap`
  *     reduce to a retag — pure carrier + dispatch overhead.
  *   - **List cartesian-singleton** via `collectList`: v1 `Reflector[List]` semantics —
  *     `List(agg(fa))` regardless of input length.
  *   - **`Indexed` 3-/6-slot modify**: lawful finite-index tuple tabulation and rebuild.
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

  // ----- Indexed: tuple3 / tuple6 modify -----
  private val tripleMF =
    Indexed.representable[TupleRepresentables.Triple, Double](
      TupleRepresentables.triple
    )

  private val sextupleMF =
    Indexed.representable[TupleRepresentables.Sextuple, Double](
      TupleRepresentables.sextuple
    )

  private val tripleData: (Double, Double, Double) = (1.0, 2.0, 3.0)

  private val sextupleData: (Double, Double, Double, Double, Double, Double) =
    (1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

  @Benchmark def eoModify_multiFocusTuple3: (Double, Double, Double) =
    tripleMF.modify((_, a) => a * 2.0)(tripleData)

  @Benchmark def naive_tuple3Rewrite: (Double, Double, Double) =
    (tripleData._1 * 2.0, tripleData._2 * 2.0, tripleData._3 * 2.0)

  @Benchmark def eoModify_multiFocusTuple6: (Double, Double, Double, Double, Double, Double) =
    sextupleMF.modify((_, a) => a * 2.0)(sextupleData)

  @Benchmark def naive_tuple6Rewrite: (Double, Double, Double, Double, Double, Double) =
    (
      sextupleData._1 * 2.0,
      sextupleData._2 * 2.0,
      sextupleData._3 * 2.0,
      sextupleData._4 * 2.0,
      sextupleData._5 * 2.0,
      sextupleData._6 * 2.0,
    )

/** Benchmark-local homogeneous tuples: every index names a real slot. Unlike Int indexing, these
  * finite index types make both Representable round trips total.
  */
private object TupleRepresentables:
  type Triple[A] = (A, A, A)
  type Sextuple[A] = (A, A, A, A, A, A)

  enum Three:
    case First, Second, Third

  enum Six:
    case First, Second, Third, Fourth, Fifth, Sixth

  val triple: Representable.Aux[Triple, Three] = new Representable[Triple]:
    type Representation = Three
    val F: Functor[Triple] = new Functor[Triple]:
      def map[A, B](fa: Triple[A])(f: A => B): Triple[B] = (f(fa._1), f(fa._2), f(fa._3))
    def index[A](fa: Triple[A]): Three => A =
      case Three.First  => fa._1
      case Three.Second => fa._2
      case Three.Third  => fa._3
    def tabulate[A](f: Three => A): Triple[A] =
      (f(Three.First), f(Three.Second), f(Three.Third))

  val sextuple: Representable.Aux[Sextuple, Six] = new Representable[Sextuple]:
    type Representation = Six
    val F: Functor[Sextuple] = new Functor[Sextuple]:
      def map[A, B](fa: Sextuple[A])(f: A => B): Sextuple[B] =
        (f(fa._1), f(fa._2), f(fa._3), f(fa._4), f(fa._5), f(fa._6))
    def index[A](fa: Sextuple[A]): Six => A =
      case Six.First  => fa._1
      case Six.Second => fa._2
      case Six.Third  => fa._3
      case Six.Fourth => fa._4
      case Six.Fifth  => fa._5
      case Six.Sixth  => fa._6
    def tabulate[A](f: Six => A): Sextuple[A] =
      (f(Six.First), f(Six.Second), f(Six.Third), f(Six.Fourth), f(Six.Fifth), f(Six.Sixth))
