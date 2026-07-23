package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import cats.instances.list.given
import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.optics.{
  Iso as EoIso,
  Lens as EoLens,
  Prism as EoPrism,
  Traversal as EoTraversal
}
import java.util.concurrent.TimeUnit
import monocle.{Iso as MIso, Lens as MLens, Prism as MPrism, Traversal as MTraversal}
import org.openjdk.jmh.annotations.*

/** Lens.get / Lens.replace / Lens.modify on the canonical [[Order]], paired EO vs Monocle.
  *
  * Two foci on the one fixture:
  *   - **shallow scalar** `order.id: Long` — the bare per-call lens-algebra overhead (the
  *     surrounding `lines` list is shared by reference, so `replace`/`modify` is a single-record
  *     copy).
  *   - **depth-3 path** `customer.address.street: String` — the nested-copy cost both libraries pay
  *     for a deep write (three records rebuilt, `lines` still shared).
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class LensBench extends JmhDefaults:

  val order: Order = Domain.mkOrder(8)

  // ---- shallow: order.id --------------------------------------------

  val eoId =
    EoLens[Order, Long](_.id, (o, v) => o.copy(id = v))

  val mId: MLens[Order, Long] =
    MLens[Order, Long](_.id)(v => o => o.copy(id = v))

  @Benchmark def eoGet: Long = eoId.get(order)
  @Benchmark def mGet: Long = mId.get(order)

  @Benchmark def eoReplace: Order = eoId.replace(99L)(order)
  @Benchmark def mReplace: Order = mId.replace(99L)(order)

  @Benchmark def eoModify: Order = eoId.modify(_ + 1)(order)
  @Benchmark def mModify: Order = mId.modify(_ + 1)(order)

  // ---- deep: customer.address.street (Monocle peer = DomainMonocle.street) ----

  val eoStreet =
    EoLens[Order, String](
      _.customer.address.street,
      (o, s) => o.copy(customer = o.customer.copy(address = o.customer.address.copy(street = s))),
    )

  @Benchmark def eoModifyDeep: Order = eoStreet.modify(_.toUpperCase)(order)
  @Benchmark def mModifyDeep: Order = DomainMonocle.street.modify(_.toUpperCase)(order)

/** Prism.getOption / Prism.reverseGet
  *
  * Two use cases:
  *   - `optional`-style prism on `Option[Int]` (Some / None).
  *   - `Either[String, Int]` prism focusing the Right (Int) branch; this is a more natural Prism
  *     since the residual type differs from the focused type.
  *
  * These exercise the Prism algebra over the stdlib `Option`/`Either` carriers directly, so there's
  * no domain fixture to share — the sum-type-over-a-domain-ADT story lives in
  * [[PowerSeriesPrismBench]] and [[GenericsBench]].
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PrismBench extends JmhDefaults:

  val present: Option[Int] = Some(7)
  val absent: Option[Int] = None
  val raw: Int = 7

  val eoSome = EoPrism.optional[Option[Int], Int](identity, Some(_))

  val mSome: MPrism[Option[Int], Int] =
    MPrism[Option[Int], Int](identity)(Some(_))

  @Benchmark def eoGetOptionPresent: Option[Int] = eoSome.getOption(present)
  @Benchmark def mGetOptionPresent: Option[Int] = mSome.getOption(present)

  @Benchmark def eoGetOptionAbsent: Option[Int] = eoSome.getOption(absent)
  @Benchmark def mGetOptionAbsent: Option[Int] = mSome.getOption(absent)

  @Benchmark def eoReverseGet: Option[Int] = eoSome.reverseGet(raw)
  @Benchmark def mReverseGet: Option[Int] = mSome.reverseGet(raw)

  // ---- Either[String, Int] prism on the Right (Int) branch ---------

  val rightPresent: Either[String, Int] = Right(42)
  val rightAbsent: Either[String, Int] = Left("nope")

  // Right-prism on Either[String, Int]. Both sides use the `optional`
  // / `Option`-flavoured constructor so the shapes match across EO and
  // Monocle.
  val eoRight =
    EoPrism.optional[Either[String, Int], Int](_.toOption, Right(_))

  val mRight: MPrism[Either[String, Int], Int] =
    MPrism[Either[String, Int], Int](_.toOption)(Right(_))

  @Benchmark def eoGetRightPresent: Option[Int] = eoRight.getOption(rightPresent)
  @Benchmark def mGetRightPresent: Option[Int] = mRight.getOption(rightPresent)

  @Benchmark def eoGetRightAbsent: Option[Int] = eoRight.getOption(rightAbsent)
  @Benchmark def mGetRightAbsent: Option[Int] = mRight.getOption(rightAbsent)

  @Benchmark def eoRightReverseGet: Either[String, Int] = eoRight.reverseGet(raw)
  @Benchmark def mRightReverseGet: Either[String, Int] = mRight.reverseGet(raw)

/** Iso.get / Iso.reverseGet on `Address <-> (String, String, String, String)`.
  *
  * A product-to-product isomorphism over the canonical [[Address]] is a more honest benchmark than
  * a primitive conversion: every call allocates a new carrier on each side and exercises the same
  * boxing behaviour a real Iso would.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class IsoBench extends JmhDefaults:

  val address: Address = Domain.DefaultCustomer.address

  val tuple: (String, String, String, String) =
    (address.street, address.city, address.zip, address.country)

  // No explicit type annotation: let inference pick BijectionIso, the
  // concrete EO subclass that stores get / reverseGet directly and
  // skips the Accessor[Direct] / ReverseAccessor[Direct] dispatch.
  val eoIso =
    EoIso[(String, String, String, String), (String, String, String, String), Address, Address](
      t => Address(t._1, t._2, t._3, t._4),
      a => (a.street, a.city, a.zip, a.country),
    )

  val mIso: MIso[(String, String, String, String), Address] =
    MIso[(String, String, String, String), Address](t => Address(t._1, t._2, t._3, t._4))(a =>
      (a.street, a.city, a.zip, a.country)
    )

  @Benchmark def eoGet: Address = eoIso.get(tuple)
  @Benchmark def mGet: Address = mIso.get(tuple)

  @Benchmark def eoReverseGet: (String, String, String, String) = eoIso.reverseGet(address)
  @Benchmark def mReverseGet: (String, String, String, String) = mIso.reverseGet(address)

/** Traversal.modify over the canonical `Order.lines` (a `List[LineItem]`).
  *
  * The Monocle traversal walks the list with a Traverse[List]; the EO traversal walks via the
  * MultiFocus[PSVec] carrier (Functor[PSVec].map after collecting into the focus vector). Modify
  * bumps every line's `qty` — a bare `each` over the canonical element type, so the row reads
  * apples-to-apples against the integration benches' `lines[*]` traversals.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class TraversalBench extends JmhDefaults:

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var lines: List[LineItem] = uninitialized

  // Un-ascribed: constructors return the concrete `Traversal` class since 0.12, so this is the
  // type a user's val holds by default — resolution lands on the fused inline `modify` member
  // (−40 B/op vs the generic extension; measured, see Traversal's scaladoc).
  val eoEach =
    EoTraversal.each[List, LineItem]

  val mEach: MTraversal[List[LineItem], LineItem] =
    MTraversal.fromTraverse[List, LineItem]

  @Setup(Level.Iteration)
  def init(): Unit =
    lines = Domain.mkOrder(size).lines
    nested = lines.grouped(8).toList

  @Benchmark def eoModify: List[LineItem] = eoEach.modify(li => li.copy(qty = li.qty + 1))(lines)
  @Benchmark def mModify: List[LineItem] = mEach.modify(li => li.copy(qty = li.qty + 1))(lines)

  // Traversal-as-fold: sum `price` through the SAME optic used for writes — the read-only
  // consumption path a user pays when holding one Traversal for both directions (the read-only
  // Fold-family peer of this row is FoldBench.eoFoldPrices).
  @Benchmark def eoFoldPrices: Double = eoEach.foldMap[Double](_.price)(lines)
  @Benchmark def mFoldPrices: Double = mEach.foldMap[Double](_.price)(lines)

  // Nested each∘each fold — tracks the composed-fold cost (the materialized `composeTo` walk;
  // streaming this was measured +37% B/op and rejected, see `Traversal.composed`).
  var nested: List[List[LineItem]] = uninitialized

  val eoNested: EoTraversal[List[List[LineItem]], List[List[LineItem]], LineItem, LineItem] =
    EoTraversal.each[List, List[LineItem]].andThen(eoEach)

  @Benchmark def eoFoldNested: Double =
    eoNested.foldMap[Double](_.price)(nested)
