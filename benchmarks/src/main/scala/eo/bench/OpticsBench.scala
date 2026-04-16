package eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import eo.optics.{Iso => EoIso, Lens => EoLens, Prism => EoPrism, Traversal => EoTraversal, Optic}
import eo.optics.Optic.*
import eo.data.{Forget, Forgetful}
import eo.data.Forgetful.given

import cats.instances.list.given

import monocle.{Iso => MIso, Lens => MLens, Prism => MPrism, Traversal => MTraversal}

/** Shared sample data used across benchmarks. */
final case class Person(name: String, age: Int)

/** Lens.get / Lens.replace / Lens.modify
  *
  * Both Monocle's `Lens` and EO's `Optic[..., Tuple2]` implement the
  * lens algebra; this benchmark compares the per-call overhead of each
  * surface for the canonical `Person.age` case.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class LensBench:

  val person: Person = Person("Alice", 30)

  val eoAge: Optic[Person, Person, Int, Int, Tuple2] =
    EoLens[Person, Int](_.age, (p, a) => p.copy(age = a))

  val mAge: MLens[Person, Int] =
    MLens[Person, Int](_.age)(a => p => p.copy(age = a))

  @Benchmark def eoGet: Int     = eoAge.get(person)
  @Benchmark def mGet:  Int     = mAge.get(person)

  @Benchmark def eoReplace: Person = eoAge.replace(40)(person)
  @Benchmark def mReplace:  Person = mAge.replace(40)(person)

  @Benchmark def eoModify: Person = eoAge.modify(_ + 1)(person)
  @Benchmark def mModify:  Person = mAge.modify(_ + 1)(person)

/** Prism.getOption / Prism.reverseGet
  *
  * Some-constructor on `Option[Int]` for both libraries.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PrismBench:

  val present: Option[Int] = Some(7)
  val absent:  Option[Int] = None
  val raw:     Int         = 7

  val eoSome: Optic[Option[Int], Option[Int], Int, Int, Either] =
    EoPrism[Option[Int], Int](_.toRight(None), Some(_))

  val mSome: MPrism[Option[Int], Int] =
    MPrism[Option[Int], Int](identity)(Some(_))

  @Benchmark def eoGetOptionPresent: Option[Int] = eoSome.to(present).toOption
  @Benchmark def mGetOptionPresent:  Option[Int] = mSome.getOption(present)

  @Benchmark def eoGetOptionAbsent: Option[Int] = eoSome.to(absent).toOption
  @Benchmark def mGetOptionAbsent:  Option[Int] = mSome.getOption(absent)

  @Benchmark def eoReverseGet: Option[Int] = eoSome.reverseGet(raw)
  @Benchmark def mReverseGet:  Option[Int] = mSome.reverseGet(raw)

/** Iso.get / Iso.reverseGet on `Int <-> String`.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class IsoBench:

  val n:  Int    = 42
  val str: String = "42"

  val eoIso: Optic[Int, Int, String, String, Forgetful] =
    EoIso[Int, Int, String, String](_.toString, _.toInt)

  val mIso: MIso[Int, String] =
    MIso[Int, String](_.toString)(_.toInt)

  @Benchmark def eoGet: String = eoIso.get(n)
  @Benchmark def mGet:  String = mIso.get(n)

  @Benchmark def eoReverseGet: Int = eoIso.reverseGet(str)
  @Benchmark def mReverseGet:  Int = mIso.reverseGet(str)

/** Traversal.modify over a moderately sized List[Int].
  *
  * The Monocle traversal walks the list with a Traverse[List]; the EO
  * traversal walks via the Forget[List] carrier's bifunctor map. Same
  * shape, different machinery.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class TraversalBench:

  @Param(Array("8", "64", "512"))
  var size: Int = _

  var xs: List[Int] = _

  val eoEach: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
    EoTraversal.each[List, Int, Int]

  val mEach: MTraversal[List[Int], Int] =
    MTraversal.fromTraverse[List, Int]

  @Setup(Level.Iteration)
  def init(): Unit =
    xs = List.tabulate(size)(identity)

  @Benchmark def eoModify: List[Int] = eoEach.modify(_ + 1)(xs)
  @Benchmark def mModify:  List[Int] = mEach.modify(_ + 1)(xs)
