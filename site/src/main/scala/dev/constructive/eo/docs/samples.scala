package dev.constructive.eo.docs

// Top-level ADT samples referenced by mdoc fences in site/docs/*.md.
//
// Macro-derived optics (`lens[…]` / `prism[…]`) need their target
// case classes / enum cases to live at a package-level location —
// mdoc wraps every fence in an anonymous object, and nested case
// classes trip the back-end on "missing outer accessor" when the
// macro tries to emit `new T(...)`. Hosting the samples here means
// site pages can freely show macro usage without hitting that.

final case class Zip(code: Int, extension: String)

final case class Address(street: String, zip: Zip)

final case class Person(name: String, address: Address)

final case class Config(name: String, timeout: Option[Int])

final case class App(config: Config)

final case class Phone(isMobile: Boolean, number: String)

final case class Owner(phones: List[Phone])

enum Event:
  case Click(x: Int, y: Int)
  case Scroll(delta: Int)

enum Shape:
  case Circle(r: Double)
  case Square(s: Double)
  case Triangle(b: Double, h: Double)

final case class Coords(x: Int, y: Int)

enum Shape2:
  case Circle(c: Coords, r: Double)
  case Square(c: Coords, s: Double)

final case class Customer(name: String, age: Int)

final case class NameAgePair(name: String, age: Int)

enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

enum Result:
  case Ok(value: Int)
  case Err(reason: String)

// ---- kindlings-derived codecs ---------------------------------------
//
// These givens expand kindlings' hearth-based derivation macros
// (budgeted by `<ns>Derivation.timeout` in `-Xmacro-settings`). They
// are hosted here rather than inside mdoc fences because mdoc's fence
// compiler never receives `-Xmacro-settings` (verified: a 1ms override
// doesn't reach the expansion), so fences always run on the kindlings
// 5s default — which a loaded machine intermittently trips. Compiled
// sources DO honour the flag, and zinc caches them, so docs builds
// stop re-expanding these derivations altogether.

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec

final case class UserAddress(street: String, zip: Int)
object UserAddress:
  given Codec.AsObject[UserAddress] = KindlingsCodecAsObject.derived

final case class SiteUser(name: String, address: UserAddress)
object SiteUser:
  given Codec.AsObject[SiteUser] = KindlingsCodecAsObject.derived

final case class Item(name: String, price: Double)
object Item:
  given Codec.AsObject[Item] = KindlingsCodecAsObject.derived

final case class Basket(owner: String, items: Vector[Item])
object Basket:
  given Codec.AsObject[Basket] = KindlingsCodecAsObject.derived

final case class OrderEvent(orderId: String, customer: String, total: Double)
object OrderEvent:
  given AvroEncoder[OrderEvent] = AvroEncoder.derived
  given AvroDecoder[OrderEvent] = AvroDecoder.derived
  given AvroSchemaFor[OrderEvent] = AvroSchemaFor.derived

final case class BalanceSheet(id: Long, owner: String, total: Double)
object BalanceSheet:
  given Codec.AsObject[BalanceSheet] = KindlingsCodecAsObject.derived

/** The `.fields(_.name, _.age)` focus type for circe.md, and its codec. Hosted here for the reason
  * above — as a fence it was the LAST kindlings derivation expanding on the 5s default, and it
  * intermittently tripped the pre-commit docs build on a cold JVM while passing on a warm one.
  * NamedTuple derivation is the slowest of these, so it is the one that had to move.
  */
type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

object NameAgeCodec:
  given Codec.AsObject[NameAge] = KindlingsCodecAsObject.derived

// ---- kyo Record / kyo-schema samples --------------------------------
//
// Same hosting rule, two new reasons: `Record.iso[T]` on a case class
// stages `new T(...)` (outer-accessor trap on fence-local classes), and
// kyo's `Schema.derived` wants package-level ADTs too.

import kyo.Schema

final case class KyoItem(name: String, price: Double) derives Schema

final case class KyoCart(id: String, items: Vector[KyoItem]) derives Schema

enum KyoShape derives Schema:
  case Circle(radius: Double)
  case Square(side: Double)
