package dev.constructive.eo
package bench

import cats.{Applicative, Functor}
import cats.instances.list.given
import cats.instances.option.given
import cats.instances.vector.given
import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.{Forget, MultiFocus, PSVec}
import dev.constructive.eo.optics.{
  Fold as EoFold,
  ForgetFold,
  GetReplaceLens,
  Lens as EoLens,
  Optic,
  Traversal as EoTraversal,
}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** Capability-call vs direct-call on the canonical [[Order]] — the evidence behind the "consume via
  * capability" doctrine. Four variants per operation:
  *
  *   - `*Direct` — receiver statically typed as the concrete class; the fused member, today's
  *     baseline.
  *   - `*Cap` — receiver statically typed as the capability trait; interface dispatch into the SAME
  *     final method (the mixin path a consuming signature takes when handed a concrete optic).
  *   - `*DerivedHeld` — the capability derived ONCE from a generic `Optic[…, F]` given and held;
  *     the wrapper's cost per call, amortised summon.
  *   - `*DerivedPerCall` — the given re-summoned inside the measured call, a fresh SAM every
  *     invocation: what a generic method's use-site pays when its optic given is only known at the
  *     generic type. This is the variant that can falsify "the ergonomics are free" — the held
  *     variants structurally cannot.
  *
  * Numbers are read off the CI benchmarks workflow only (local boxes are too noisy for the ns
  * deltas at stake); `-prof gc` distinguishes wrapper allocation from EA-elided cost.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class CapsBench extends JmhDefaults:

  val order: Order = Domain.mkOrder(8)

  // ---- fixtures: shallow lens, depth-3 lens, list fold ----------------

  val eoId: GetReplaceLens[Order, Order, Long, Long] =
    EoLens[Order, Long](_.id, (o, v) => o.copy(id = v))

  val eoStreet: GetReplaceLens[Order, Order, String, String] =
    EoLens[Order, String](
      _.customer.address.street,
      (o, s) => o.copy(customer = o.customer.copy(address = o.customer.address.copy(street = s))),
    )

  val eoFold: ForgetFold[List[Int], List, Int] = EoFold[List, Int]
  val ints: List[Int] = List.range(0, 8)

  // capability-typed views of the SAME instances (mixin path)
  val idGetCap: CanGet[Order, Long] = eoId
  val idModifyCap: CanModify[Order, Long] = eoId
  val streetGetCap: CanGet[Order, String] = eoStreet
  val streetModifyCap: CanModify[Order, String] = eoStreet
  val foldCap: CanFold[List[Int], Int] = eoFold

  // generic-optic givens — the derivation route (wrapper, not mixin)
  given idGeneric: Optic[Order, Order, Long, Long, Tuple2] = eoId
  given foldGeneric: Optic[List[Int], Unit, Int, Unit, Forget[List]] = eoFold

  // derived once, held
  val idGetDerived: CanGet[Order, Long] = summon
  val idModifyDerived: CanModify[Order, Long] = summon
  val foldDerived: CanFold[List[Int], Int] = summon

  // effectful modify: CanModifyF's gate is inferred per carrier — Functor (lens), Applicative
  // (traversal); effect `Option`
  val vec: Vector[Int] = Vector.range(0, 8)

  val eoEach: Optic[Vector[Int], Vector[Int], Int, Int, MultiFocus[PSVec]] =
    EoTraversal.each[Vector, Int]

  given eachGeneric: Optic[Vector[Int], Vector[Int], Int, Int, MultiFocus[PSVec]] = eoEach
  val idModifyFDerived: CanModifyF[Order, Long, Functor] = summon
  val idModifyFCap: CanModifyF[Order, Long, Functor] = eoId
  val eachModifyFDerived: CanModifyF[Vector[Int], Int, Applicative] = summon
  val bumpL: Long => Option[Long] = n => Some(n + 1)
  val bumpI: Int => Option[Int] = n => Some(n + 1)

  // ---- get -----------------------------------------------------------

  @Benchmark def getDirect: Long = eoId.get(order)
  @Benchmark def getCap: Long = idGetCap.get(order)
  @Benchmark def getDerivedHeld: Long = idGetDerived.get(order)
  @Benchmark def getDerivedPerCall: Long = summon[CanGet[Order, Long]].get(order)

  @Benchmark def getDeepDirect: String = eoStreet.get(order)
  @Benchmark def getDeepCap: String = streetGetCap.get(order)

  // ---- modify --------------------------------------------------------

  @Benchmark def modifyDirect: Order = eoId.modify(_ + 1)(order)
  @Benchmark def modifyCap: Order = idModifyCap.modify(_ + 1)(order)
  @Benchmark def modifyDerivedHeld: Order = idModifyDerived.modify(_ + 1)(order)
  @Benchmark def modifyDerivedPerCall: Order = summon[CanModify[Order, Long]].modify(_ + 1)(order)

  @Benchmark def modifyDeepDirect: Order = eoStreet.modify(_.toUpperCase)(order)
  @Benchmark def modifyDeepCap: Order = streetModifyCap.modify(_.toUpperCase)(order)

  // ---- foldMap -------------------------------------------------------

  @Benchmark def foldMapDirect: Int = eoFold.foldMap(identity)(ints)
  @Benchmark def foldMapCap: Int = foldCap.foldMap(identity)(ints)
  @Benchmark def foldMapDerivedHeld: Int = foldDerived.foldMap(identity)(ints)

  @Benchmark def foldMapDerivedPerCall: Int =
    summon[CanFold[List[Int], Int]].foldMap(identity)(ints)

  // ---- modifyF (lens: Functor gate; traversal: Applicative gate) ------

  @Benchmark def modifyFDirect: Option[Order] = eoId.modifyF(bumpL)(order)
  @Benchmark def modifyFCap: Option[Order] = idModifyFCap.modifyF(bumpL)(order)
  @Benchmark def modifyFDerivedHeld: Option[Order] = idModifyFDerived.modifyF(bumpL)(order)

  @Benchmark def modifyFDerivedPerCall: Option[Order] =
    summon[CanModifyF[Order, Long, Functor]].modifyF(bumpL)(order)

  @Benchmark def modifyFExt: Option[Order] = order.modifyF[Long](bumpL)

  @Benchmark def modifyFEachDirect: Option[Vector[Int]] = eoEach.modifyF(bumpI)(vec)

  @Benchmark def modifyFEachDerivedHeld: Option[Vector[Int]] =
    eachModifyFDerived.modifyF(bumpI)(vec)

  @Benchmark def modifyFEachDerivedPerCall: Option[Vector[Int]] =
    summon[CanModifyF[Vector[Int], Int, Applicative]].modifyF(bumpI)(vec)

  @Benchmark def modifyFEachExt: Option[Vector[Int]] = vec.modifyF[Int](bumpI)

  // Modify under its Distributive gate, at the reader effect `Int => *`: the fused member against
  // the generic ModifyF carrier route (`ForgetfulTraverse[ModifyF, Distributive]`).
  private val eoIdModify = DomainOptics.eoSetId

  private val eoIdModifyGeneric: Optic[Order, Order, Long, Long, dev.constructive.eo.data.ModifyF] =
    eoIdModify

  private val shiftBy: Long => (Int => Long) = id => n => id + n
  @Benchmark def modifyFModifyDirect: Order = eoIdModify.modifyF(shiftBy)(order)(1)
  @Benchmark def modifyFModifyGeneric: Order = eoIdModifyGeneric.modifyF(shiftBy)(order)(1)
