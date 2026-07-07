package dev.constructive.eo
package examples

import cats.instances.either.given
import cats.instances.list.given
import cats.syntax.either.*
import cats.syntax.traverse.*
import cats.{Eq, Show}
import hearth.kindlings.catsderivation.extensions.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.parser.decode as parseJson
import io.circe.{Codec, Json}

import optics.{Optic, Traversal}
import optics.Optic.*
import generics.lens
import data.{MultiFocus, PSVec}

/** Domain model, mock services, optic declarations, and both handlers for the CRUD round-trip
  * showcase — the compile-time-heavy half of the example.
  *
  * Split out of `CrudRoundtripSpec` (the behaviour assertions) so the kindlings `Codec` / `Eq` /
  * `Show` derivations (14 across `Item` / `Address` / `Order` / `User` / `Problem`) compile in
  * their OWN unit rather than interleaved with the specs2 + ScalaCheck spec body. Fewer live macro
  * expansions per compilation unit means lower transient heap, which keeps any single kindlings
  * derivation from tripping its 2 s wall-clock budget under a loaded CI runner (the
  * `KindlingsEncoder.deriveAsObject timed out after 2000ms` flake). Behaviour is unchanged; this is
  * a compile-pressure split, not a logic change.
  *
  * See [[CrudRoundtripHappyPathSpec]] / [[CrudRoundtripErrorPathSpec]] for the scenarios and the
  * `handleWithOptics` vs `handleNaive` punchline.
  */
object CrudRoundtrip:

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
    given Codec.AsObject[Item] = KindlingsCodecAsObject.derived
    given Eq[Item] = Eq.derived
    given Show[Item] = Show.derived

  case class Address(street: String, city: String, zipCode: String)

  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derived
    given Eq[Address] = Eq.derived
    given Show[Address] = Show.derived

  case class Order(id: Long, shippingAddress: Address, items: List[Item])

  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derived
    given Eq[Order] = Eq.derived
    given Show[Order] = Show.derived

  case class User(
      id: Long,
      name: String,
      address: Address,
      orders: List[Order],
  )

  object User:
    given Codec.AsObject[User] = KindlingsCodecAsObject.derived
    given Eq[User] = Eq.derived
    given Show[User] = Show.derived

  // =====================================================================
  // Application errors + Result applicative
  // =====================================================================

  enum Problem:
    case InvalidZip(zip: String)
    case EmptyName
    case JsonDecodeError(msg: String)

  object Problem:
    given Eq[Problem] = Eq.derived
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
  // Three optics, one per validation target. Every lens below is
  // macro-derived via `lens[S](_.field)` — including on the 4-field
  // `User`, which the eo-generics macro now supports by synthesising
  // the complement as a scala.Tuple of the non-focused field types.
  //
  //   * userName : Lens[User, String]       — the user's name
  //   * userZip  : Lens[User, String]       — address.zipCode
  //   * everyZip : Traversal[User, String]  — every order's
  //                                           shippingAddress.zipCode
  //

  val userName: Optic[User, User, String, String, Tuple2] =
    lens[User](_.name)

  val userZip: Optic[User, User, String, String, Tuple2] =
    lens[User](_.address).andThen(lens[Address](_.zipCode))

  /** `Order → shippingAddress.zipCode` — used as the inner focus of the PowerSeries traversal
    * below. Kept as a plain Tuple2 lens and let the cross-carrier `.andThen` auto-morph it into the
    * PowerSeries carrier when composed into the traversal chain.
    */
  val orderShippingZip: Optic[Order, Order, String, String, Tuple2] =
    lens[Order](_.shippingAddress).andThen(lens[Address](_.zipCode))

  /** Every `orders → shippingAddress → zipCode` in one composed optic.
    *
    * This is the flagship cats-eo move: compose the outer Lens with a `Traversal.each` and then
    * with the inner nested Lens — no explicit `.morph` anywhere. The cross-carrier `andThen`
    * extension picks the `Composer[Tuple2, PowerSeries]` bridge automatically on each hop. A single
    * `modifyA[Result]` then threads an `Either`-effectful validator through every leaf at once.
    */
  val everyZip: Optic[User, User, String, String, MultiFocus[PSVec]] =
    lens[User](_.orders)
      .andThen(Traversal.each[List, Order])
      .andThen(orderShippingZip)

  // =====================================================================
  // Handler #1 — naive, no optics
  // =====================================================================

  /** What you'd write today without cats-eo: a for-comprehension that destructures every nested
    * field by hand and rebuilds the copy chain at each level. Every new validated field costs a
    * `<- validate` line AND a `.copy(...)` line. The order loop is the worst offender — two nested
    * copies just to update one string.
    */
  def handleNaive(db: InMemoryDb, body: Json): Result[Long] =
    for
      user <- parseUser(body)
      name <- normalizeName(user.name)
      zip <- validateZip(user.address.zipCode)
      orders <- user.orders.traverse { o =>
        validateZip(o.shippingAddress.zipCode).map { z =>
          o.copy(shippingAddress = o.shippingAddress.copy(zipCode = z))
        }
      }
      updated = user.copy(
        name = name,
        address = user.address.copy(zipCode = zip),
        orders = orders,
      )
      id <- db.store(updated)
    yield id

  // =====================================================================
  // Handler #2 — cats-eo optics
  // =====================================================================

  /** Same behaviour as [[handleNaive]], written against the optic declarations above. Every
    * validation step is a single `optic.modifyA[Result](f)` line — no `.copy(...)` shows up in the
    * handler, and adding a new validated field is a one-liner (declare the lens, plug it in).
    *
    * `modifyA` applies uniformly across Tuple2 (Lens) and PowerSeries (Traversal) carriers because
    * both ship `ForgetfulTraverse[_, Applicative]` — and `Either[Problem, *]` is an `Applicative`.
    */
  def handleWithOptics(db: InMemoryDb, body: Json): Result[Long] =
    for
      user <- parseUser(body)
      step1 <- userName.modifyA[Result](normalizeName)(user)
      step2 <- userZip.modifyA[Result](validateZip)(step1)
      step3 <- everyZip.modifyA[Result](validateZip)(step2)
      id <- db.store(step3)
    yield id

  // =====================================================================
  // Plumbing helpers
  // =====================================================================

  private def parseUser(body: Json): Result[User] =
    parseJson[User](body.noSpaces).leftMap(e => Problem.JsonDecodeError(e.getMessage))

end CrudRoundtrip
