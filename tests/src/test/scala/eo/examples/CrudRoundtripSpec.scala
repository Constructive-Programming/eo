package eo
package examples

import optics.{Lens, Optic, Traversal}
import optics.Optic.*
import generics.lens
import data.PowerSeries
import data.PowerSeries.given

import cats.{Eq, Show}
import cats.instances.either.given
import cats.instances.list.given
import cats.syntax.either.*
import cats.syntax.traverse.*

import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.*
import io.circe.parser.decode as parseJson

// Kindlings enriches every cats / alleycats typeclass companion with
// a `.derived` extension method. The wildcard brings the extensions
// in — `.given` alone is not enough because `.derived` is an
// extension, not a given.
import hearth.kindlings.catsderivation.extensions.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Motivating example for cats-eo — the "why this library exists"
  * showcase.
  *
  * Scenario: an HTTP endpoint receives a JSON user-update, runs
  * effectful validation over specific fields (including every
  * `shippingAddress.zipCode` of every order the user owns), persists
  * the result through a toy database, and replies with the new id.
  *
  * Iteration 1 — single file, mock HTTP via a plain
  * `Json => Result[Long]` handler, `Result[A] = Either[Problem, A]`
  * as the applicative so the example stays focused on optics. Real
  * code would swap `Result` for `IO` or `EitherT[IO, Problem, *]`;
  * the optic composition is identical — only the `modify{F,A}[G]`
  * summoner changes.
  *
  * Domain typeclasses (`Codec`, `Show`, `Eq`) are derived by
  * Kubuszok's kindlings library so the boilerplate doesn't distract
  * from the optic story. Per-field lenses use eo-generics' `lens[S]`
  * macro where the case class has ≤ 2 fields; wider records fall
  * back to `Lens.apply` (the macro's 3+-field support is future
  * work).
  *
  * The punchline is in [[CrudRoundtripSpec.handleWithOptics]]: every
  * validation step is one line, `optic.modifyX[Result](validator)`.
  * Compare with [[CrudRoundtripSpec.handleNaive]], whose order-list
  * traversal has to write the `copy(copy(copy(...)))` chain by hand.
  */
class CrudRoundtripSpec extends Specification with ScalaCheck:

  import CrudRoundtripSpec.*

  // ---------------------------------------------------------------------
  // Happy path
  // ---------------------------------------------------------------------

  "CRUD round-trip — happy path" should {

    val alice = User(
      name = "  Alice  ",
      address = Address("Amsterdam", "01234"),
      orders = List(
        Order(Address("Rotterdam", "56789"), List(Item("SKU-A", 2))),
        Order(Address("Utrecht",   "24680"), List(Item("SKU-B", 1))),
      ),
    )

    "naive and optic handlers persist the same user" >> {
      val dbNaive  = InMemoryDb()
      val dbOptic  = InMemoryDb()
      val payload  = alice.asJson

      val idNaive  = handleNaive(dbNaive, payload)
      val idOptic  = handleWithOptics(dbOptic, payload)

      (idNaive.isRight && idOptic.isRight) must beTrue
      dbNaive.snapshot.values.toSet === dbOptic.snapshot.values.toSet
    }

    "optic handler normalises the user's name in place" >> {
      val db   = InMemoryDb()
      val body = alice.asJson
      handleWithOptics(db, body)
        .map(id => db.snapshot(id).name) === Right("Alice")
    }
  }

  // ---------------------------------------------------------------------
  // Error path: invalid zip in a nested order surfaces as a Problem
  // ---------------------------------------------------------------------

  "CRUD round-trip — error path" should {

    val bob = User(
      name = "Bob",
      address = Address("Amsterdam", "01234"),
      orders = List(
        Order(Address("Rotterdam", "not-a-zip"), Nil),
      ),
    )

    "both handlers return the same Problem on an invalid nested zip" >> {
      val db      = InMemoryDb()
      val payload = bob.asJson
      handleNaive(db, payload) === handleWithOptics(db, payload)
    }
  }

object CrudRoundtripSpec:

  // =====================================================================
  // Domain types
  // =====================================================================
  //
  // Kept intentionally narrow (≤ 2 fields per inner type) so the
  // eo-generics `lens[S](_.field)` macro derives the inner lenses
  // automatically. `User` has three fields and its lenses are
  // hand-written below — a feature of the example, not a bug: it
  // shows `Lens.apply` and the macro coexisting.
  //

  case class Item(sku: String, qty: Int)
  object Item:
    given Codec.AsObject[Item] = KindlingsCodecAsObject.derive
    given Eq[Item]             = Eq.derived
    given Show[Item]           = Show.derived

  case class Address(city: String, zipCode: String)
  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derive
    given Eq[Address]             = Eq.derived
    given Show[Address]           = Show.derived

  case class Order(shippingAddress: Address, items: List[Item])
  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derive
    given Eq[Order]             = Eq.derived
    given Show[Order]           = Show.derived

  case class User(name: String, address: Address, orders: List[Order])
  object User:
    given Codec.AsObject[User] = KindlingsCodecAsObject.derive
    given Eq[User]             = Eq.derived
    given Show[User]           = Show.derived

  // =====================================================================
  // Application errors + Result applicative
  // =====================================================================

  enum Problem:
    case InvalidZip(zip: String)
    case EmptyName
    case JsonDecodeError(msg: String)
  object Problem:
    given Eq[Problem]   = Eq.derived
    given Show[Problem] = Show.derived

  type Result[A] = Either[Problem, A]

  // =====================================================================
  // Mock services
  // =====================================================================

  /** Stand-in for a geolocation API. Accepts 5-digit zips only. */
  def validateZip(zip: String): Result[String] =
    if zip.matches("""\d{5}""") then Right(zip)
    else Left(Problem.InvalidZip(zip))

  /** Stand-in for a name normaliser. Trims whitespace; rejects empty. */
  def normalizeName(raw: String): Result[String] =
    val trimmed = raw.trim
    if trimmed.isEmpty then Left(Problem.EmptyName) else Right(trimmed)

  /** Stand-in for a write-through DB. In-memory `Map`; thread-unsafe. */
  final class InMemoryDb private (
      private var data: Map[Long, User],
      private var next: Long,
  ):
    def store(u: User): Result[Long] =
      val id = next
      next = next + 1L
      data = data.updated(id, u)
      Right(id)

    def snapshot: Map[Long, User] = data
  object InMemoryDb:
    def apply(): InMemoryDb = new InMemoryDb(Map.empty, 1L)

  // =====================================================================
  // Optic declarations — the heart of the example
  // =====================================================================
  //
  // Three optics, one per validation target:
  //
  //   * userName : Lens[User, String]              — the user's name
  //   * userZip  : Lens[User, String]              — address.zipCode
  //   * everyZip : Traversal[User, String]         — every order's
  //                                                   shippingAddress.zipCode
  //
  // `userName` and `userAddress` are hand-written because `User` has
  // three fields (the macro currently supports ≤ 2). Every other lens
  // is macro-derived; the composition is pure `andThen`.
  //

  val userName: Optic[User, User, String, String, Tuple2] =
    Lens[User, String](_.name, (u, n) => u.copy(name = n))

  val userAddress: Optic[User, User, Address, Address, Tuple2] =
    Lens[User, Address](_.address, (u, a) => u.copy(address = a))

  val userOrders: Optic[User, User, List[Order], List[Order], Tuple2] =
    Lens[User, List[Order]](_.orders, (u, os) => u.copy(orders = os))

  val userZip: Optic[User, User, String, String, Tuple2] =
    userAddress.andThen(lens[Address](_.zipCode))

  /** `Order → shippingAddress.zipCode` — used as the inner focus of
    * the PowerSeries traversal below. Composed first as a plain
    * Tuple2 lens so the subsequent `.morph[PowerSeries]` only lifts
    * once. */
  val orderShippingZip: Optic[Order, Order, String, String, Tuple2] =
    lens[Order](_.shippingAddress).andThen(lens[Address](_.zipCode))

  /** Every `orders → shippingAddress → zipCode` in one composed optic.
    *
    * This is the flagship cats-eo move: lift the outer Lens into the
    * `PowerSeries` carrier, compose with a `Traversal.powerEach` over
    * the list, then pre-compose the inner nested lens (also lifted)
    * in a single `andThen`. A single `modifyA[Result]` then threads
    * an `Either`-effectful validator through every leaf at once. */
  val everyZip: Optic[User, User, String, String, PowerSeries] =
    userOrders.morph[PowerSeries]
      .andThen[Order, Order](Traversal.powerEach[List, Order])
      .andThen(orderShippingZip.morph[PowerSeries])

  // =====================================================================
  // Handler #1 — naive, no optics
  // =====================================================================

  /** What you'd write today without cats-eo: a for-comprehension that
    * destructures every nested field by hand and rebuilds the copy
    * chain at each level. Every new validated field costs a
    * `<- validate` line AND a `.copy(...)` line. The order loop is
    * the worst offender — two nested copies just to update one
    * string. */
  def handleNaive(db: InMemoryDb, body: Json): Result[Long] =
    for
      user   <- parseUser(body)
      name   <- normalizeName(user.name)
      zip    <- validateZip(user.address.zipCode)
      orders <- user.orders.traverse { o =>
        validateZip(o.shippingAddress.zipCode).map { z =>
          o.copy(shippingAddress = o.shippingAddress.copy(zipCode = z))
        }
      }
      updated = user.copy(
        name    = name,
        address = user.address.copy(zipCode = zip),
        orders  = orders,
      )
      id     <- db.store(updated)
    yield id

  // =====================================================================
  // Handler #2 — cats-eo optics
  // =====================================================================

  /** Same behaviour as [[handleNaive]], written against the optic
    * declarations above. Every validation step is a single
    * `optic.modifyX[Result](f)` line — no `.copy(...)` shows up in
    * the handler, and adding a new validated field is a one-liner
    * (declare the lens, plug it in).
    *
    * Carrier-aware method choice: Tuple2-carrier lenses satisfy
    * `ForgetfulTraverse[Tuple2, Functor]` only, so they expose
    * `modifyF`; the PowerSeries-carrier traversal has
    * `ForgetfulTraverse[PowerSeries, Applicative]` and exposes
    * `modifyA`. `Either[Problem, *]` satisfies both constraints,
    * so the same validator function plugs into both. */
  def handleWithOptics(db: InMemoryDb, body: Json): Result[Long] =
    for
      user  <- parseUser(body)
      step1 <- userName.modifyF[Result](normalizeName)(user)
      step2 <- userZip.modifyF[Result](validateZip)(step1)
      step3 <- everyZip.modifyA[Result](validateZip)(step2)
      id    <- db.store(step3)
    yield id

  // =====================================================================
  // Plumbing helpers
  // =====================================================================

  private def parseUser(body: Json): Result[User] =
    parseJson[User](body.noSpaces).leftMap(e =>
      Problem.JsonDecodeError(e.getMessage)
    )
