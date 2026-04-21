# Cookbook

Runnable patterns for the questions that come up most often.
Every fence is compiled by mdoc against the current library
version.

```scala mdoc:silent
import eo.optics.{Iso, Lens, Optic, Optional, Prism, Traversal}
import eo.optics.Optic.*
import eo.data.Forgetful.given    // Accessor[Forgetful] — powers .get on Iso / Getter
```

## Edit a deeply-nested field

Using hand-written Lenses (the
[generics module](generics.md) provides the `lens[S](_.field)`
macro that generates this boilerplate):

```scala mdoc:silent
case class Zip(code: Int, extension: String)
case class Address(street: String, zip: Zip)
case class Company(name: String, address: Address)

val extL =
  Lens[Company, Address](_.address, (c, a) => c.copy(address = a))
    .andThen(Lens[Address, Zip](_.zip, (a, z) => a.copy(zip = z)))
    .andThen(Lens[Zip, String](_.extension, (z, e) => z.copy(extension = e)))
```

```scala mdoc
val initech = Company("Initech", Address("Main St", Zip(12345, "6789")))
extL.get(initech)
extL.replace("0000")(initech)
```

## Focus an `Option` field (Optional-shaped)

```scala mdoc:silent
import eo.data.Affine

case class Setting(name: String, timeout: Option[Int])

val timeoutL = Optional[Setting, Setting, Int, Int, Affine](
  getOrModify = s => s.timeout.toRight(s),
  reverseGet  = { case (s, t) => s.copy(timeout = Some(t)) },
)
```

```scala mdoc
timeoutL.modify(_ * 2)(Setting("a", Some(10)))
timeoutL.modify(_ * 2)(Setting("a", None))
```

## Compose a Lens with an Optional

Cross-carrier `.andThen` summons `Morph[Tuple2, Affine]` for you
— no manual carrier lifting:

```scala mdoc:silent
case class AppRoot(setting: Setting)

val appTimeoutL =
  Lens[AppRoot, Setting](_.setting, (a, s) => a.copy(setting = s))
    .andThen(timeoutL)
```

```scala mdoc
appTimeoutL.modify(_ * 2)(AppRoot(Setting("a", Some(5))))
appTimeoutL.modify(_ * 2)(AppRoot(Setting("a", None)))
```

## Modify every element of a list

`Traversal.forEach` is the map-only fast path — use it when the
chain terminates at the traversal:

```scala mdoc:silent
import cats.instances.list.given

val forEachInt = Traversal.forEach[List, Int, Int]
```

```scala mdoc
forEachInt.modify(_ + 1)(List(1, 2, 3))
forEachInt.foldMap(identity[Int])(List(1, 2, 3))     // sum
forEachInt.foldMap((_: Int) => 1)(List(1, 2, 3))     // count
```

## Modify every element *and* continue through a field

Use `Traversal.each` — the composable default — when the chain
continues past the traversal (see
[Optics → Traversal](optics.md#traversal) for the cost
tradeoff):

```scala mdoc:silent
import eo.data.PowerSeries

case class Dial(isMobile: Boolean, number: String)
case class Subscriber(phones: List[Dial])

val everyMobile =
  Lens[Subscriber, List[Dial]](_.phones, (o, ps) => o.copy(phones = ps))
    .andThen(Traversal.each[List, Dial])
    .andThen(Lens[Dial, Boolean](_.isMobile, (p, m) => p.copy(isMobile = m)))
```

```scala mdoc
everyMobile.modify(!_)(Subscriber(List(
  Dial(isMobile = false, "555-0001"),
  Dial(isMobile = true,  "555-0002"),
)))
```

## Branch into a sum type

```scala mdoc:silent
enum Input:
  case Click(x: Int, y: Int)
  case Scroll(delta: Int)

val clickP = Prism[Input, Input.Click](
  {
    case c: Input.Click => Right(c)
    case other          => Left(other)
  },
  identity,
)
```

```scala mdoc
clickP.modify(c => Input.Click(c.x + 1, c.y))(Input.Click(10, 20))
clickP.modify(c => Input.Click(c.x + 1, c.y))(Input.Scroll(5))
```

## Edit JSON without decoding

```scala mdoc:silent
import eo.circe.codecPrism
import io.circe.Codec
import io.circe.syntax.*
import hearth.kindlings.circederivation.KindlingsCodecAsObject

case class UserAddress(street: String, zip: Int)
object UserAddress:
  given Codec.AsObject[UserAddress] = KindlingsCodecAsObject.derive

case class SiteUser(name: String, address: UserAddress)
object SiteUser:
  given Codec.AsObject[SiteUser] = KindlingsCodecAsObject.derive

val userStreet = codecPrism[SiteUser].address.street
```

```scala mdoc
val userJson = SiteUser("Alice", UserAddress("Main St", 12345)).asJson
userStreet.modify(_.toUpperCase)(userJson).noSpacesSortKeys
```

## Apply a function that needs an effect

Both `.modifyF` and `.modifyA` lift an `A => G[B]` through an
optic. Use `.modifyF` when the carrier admits just `Functor[G]`,
`.modifyA` when you need the full `Applicative[G]`:

```scala mdoc:silent
case class Shopper(name: String, age: Int)
val shopperAgeL =
  Lens[Shopper, Int](_.age, (s, a) => s.copy(age = a))
```

```scala mdoc
import cats.syntax.functor.*
import cats.instances.option.*

shopperAgeL.modifyF[Option](age => if age >= 0 then Some(age + 1) else None)(
  Shopper("Alice", 30)
)

shopperAgeL.modifyF[Option](age => if age >= 0 then Some(age + 1) else None)(
  Shopper("Alice", -1)
)
```

## Iso between equivalent shapes

```scala mdoc:silent
case class UserTuple(name: String, age: Int)
case class UserRecord(name: String, age: Int)

val userIso =
  Iso[UserTuple, UserTuple, UserRecord, UserRecord](
    ut => UserRecord(ut.name, ut.age),
    ur => UserTuple(ur.name, ur.age),
  )
```

```scala mdoc
userIso.get(UserTuple("Alice", 30))
userIso.reverseGet(UserRecord("Bob", 25))
```

## Derive a read-only view

```scala mdoc:silent
import eo.optics.Getter

val nameInitial = Getter[Shopper, Char](_.name.head)
```

```scala mdoc
nameInitial.get(Shopper("Alice", 30))
```

## Further reading

- [Concepts](concepts.md) — the theory behind the unified Optic
  trait and carriers.
- [Optics reference](optics.md) — the full per-family tour.
- [Generics](generics.md) — macro-derived Lens / Prism.
- [Circe integration](circe.md) — cursor-backed JSON optics.
- [Migrating from Monocle](migration-from-monocle.md) — a side-
  by-side translation guide.
