package eo
package bench

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import eo.optics.{Iso => EoIso, Lens => EoLens, Prism => EoPrism, Traversal => EoTraversal, Optic}
import eo.optics.Optic.*
import eo.data.{Forget, Forgetful}
import eo.data.Forgetful.given

import cats.instances.list.given

import monocle.{Iso => MIso, Lens => MLens, Prism => MPrism, Traversal => MTraversal}

/** Shared sample data used across benchmarks. */
final case class Person(age: Int, name: String)

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

  val person: Person = Person(30, "Alice")

  val eoAge =
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
  * Two use cases:
  *   - `optional`-style prism on `Option[Int]` (Some / None).
  *   - `Either[String, Int]` prism focusing the Right (Int) branch;
  *     this is a more natural Prism since the residual type differs
  *     from the focused type.
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

  val eoSome = EoPrism.optional[Option[Int], Int](identity, Some(_))

  val mSome: MPrism[Option[Int], Int] =
    MPrism[Option[Int], Int](identity)(Some(_))

  @Benchmark def eoGetOptionPresent: Option[Int] = eoSome.getOption(present)
  @Benchmark def mGetOptionPresent:  Option[Int] = mSome.getOption(present)

  @Benchmark def eoGetOptionAbsent: Option[Int] = eoSome.getOption(absent)
  @Benchmark def mGetOptionAbsent:  Option[Int] = mSome.getOption(absent)

  @Benchmark def eoReverseGet: Option[Int] = eoSome.reverseGet(raw)
  @Benchmark def mReverseGet:  Option[Int] = mSome.reverseGet(raw)

  // ---- Either[String, Int] prism on the Right (Int) branch ---------

  val rightPresent: Either[String, Int] = Right(42)
  val rightAbsent:  Either[String, Int] = Left("nope")

  // Right-prism on Either[String, Int]. Both sides use the `optional`
  // / `Option`-flavoured constructor so the shapes match across EO and
  // Monocle.
  val eoRight =
    EoPrism.optional[Either[String, Int], Int](_.toOption, Right(_))
  val mRight: MPrism[Either[String, Int], Int] =
    MPrism[Either[String, Int], Int](_.toOption)(Right(_))

  @Benchmark def eoGetRightPresent: Option[Int] = eoRight.getOption(rightPresent)
  @Benchmark def mGetRightPresent:  Option[Int] = mRight.getOption(rightPresent)

  @Benchmark def eoGetRightAbsent: Option[Int] = eoRight.getOption(rightAbsent)
  @Benchmark def mGetRightAbsent:  Option[Int] = mRight.getOption(rightAbsent)

  @Benchmark def eoRightReverseGet: Either[String, Int] = eoRight.reverseGet(raw)
  @Benchmark def mRightReverseGet:  Either[String, Int] = mRight.reverseGet(raw)

/** Iso.get / Iso.reverseGet on `(Int, String) <-> Person(age, name)`.
  *
  * A product-to-product isomorphism is a more honest benchmark than
  * a primitive conversion: every call allocates a new carrier on each
  * side and exercises the same boxing behaviour a real Iso would.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class IsoBench:

  val tuple:  (Int, String) = (30, "Alice")
  val person: Person        = Person(30, "Alice")

  // No explicit type annotation: let inference pick BijectionIso, the
  // concrete EO subclass that stores get / reverseGet directly and
  // skips the Accessor[Forgetful] / ReverseAccessor[Forgetful] dispatch.
  val eoIso =
    EoIso[(Int, String), (Int, String), Person, Person](
      t => Person(t._1, t._2),
      p => (p.age, p.name),
    )

  val mIso: MIso[(Int, String), Person] =
    MIso[(Int, String), Person](t => Person(t._1, t._2))(p => (p.age, p.name))

  @Benchmark def eoGet: Person        = eoIso.get(tuple)
  @Benchmark def mGet:  Person        = mIso.get(tuple)

  @Benchmark def eoReverseGet: (Int, String) = eoIso.reverseGet(person)
  @Benchmark def mReverseGet:  (Int, String) = mIso.reverseGet(person)

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
  var size: Int = uninitialized

  var xs: List[Int] = uninitialized

  val eoEach: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
    EoTraversal.forEach[List, Int, Int]

  val mEach: MTraversal[List[Int], Int] =
    MTraversal.fromTraverse[List, Int]

  @Setup(Level.Iteration)
  def init(): Unit =
    xs = List.tabulate(size)(identity)

  @Benchmark def eoModify: List[Int] = eoEach.modify(_ + 1)(xs)
  @Benchmark def mModify:  List[Int] = mEach.modify(_ + 1)(xs)
