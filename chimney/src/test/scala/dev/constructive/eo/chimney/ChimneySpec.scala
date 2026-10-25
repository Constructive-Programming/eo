package dev.constructive.eo
package chimney

import io.scalaland.chimney.{partial, PartialTransformer, Transformer}
import org.specs2.mutable.Specification

import generics.{lens, prism}
import optics.{AffineFold, Getter, Lens, Traversal}
import samples.*

class ChimneySpec extends Specification:

  val user = User("Ada", 36, "hunter2", Address("1 Analytical St", "London"))
  val good = AccountDto("ada", 120L)
  val bad = AccountDto("bob", -5L)

  def paths[A](r: partial.Result[A]): List[(String, String)] =
    r.asErrorPathMessageStrings.toList

  "Chimney.getter (option 0)" should {
    "read a target field through eo-generics lenses" in {
      val city = Chimney
        .getter[User, UserView]
        .andThen(lens[UserView](_.address))
        .andThen(lens[AddressView](_.city))
      city.get(user) === "London"
    }

    "use a hand-configured Transformer via .toGetter" in {
      val shout: Transformer[User, UserView] =
        u => UserView(u.name.toUpperCase, AddressView(u.address.city, u.address.street))
      shout.toGetter.andThen(lens[UserView](_.name)).get(user) === "ADA"
    }
  }

  "Chimney.iso / Chimney.prism (option 1)" should {
    "write through an Iso into the other representation's fields" in {
      val age = Chimney.iso[Person, PersonRow].andThen(lens[PersonRow](_.age))
      age.modify(_ + 1)(Person("Ada", 36)) === Person("Ada", 37)
    }

    "edit a DTO through its decoded domain value" in {
      val owner = Chimney.prism[Account, AccountDto].andThen(lens[Account](_.owner))
      owner.modify(_.toUpperCase)(good) === AccountDto("ADA", 120L)
    }

    "pass a DTO that fails to decode through untouched" in {
      val owner = Chimney.prism[Account, AccountDto].andThen(lens[Account](_.owner))
      (owner.modify(_.toUpperCase)(bad) === bad).and(owner.getOption(bad) === None)
    }

    "encode with reverseGet" in {
      Chimney.prism[Account, AccountDto].reverseGet(Account("ada", Cents.from(7L).get)) ===
        AccountDto("ada", 7L)
    }
  }

  "Chimney.resultFold (option 2)" should {
    val decode = Chimney.resultFold[AccountDto, Account]

    "hit on a valid DTO" in {
      decode.getOption(good).map(_.balance.value) === Some(120L)
    }

    "say why a DTO missed, with chimney's field path" in {
      paths(decode.result(bad)) === List("balance" -> "negative amount: -5")
    }

    "keep the errors through a derived lens" in {
      val owner = decode.andThen(lens[Account](_.owner))
      (owner.either(good) === Right("ada"))
        .and(paths(owner.result(bad)) === List("balance" -> "negative amount: -5"))
    }

    "keep the errors through a Getter, a ResultFold and a prism" in {
      val cents = decode.andThen(Getter[Account, Long](_.balance.value))
      val positive = ResultFold[Long, Long](l =>
        if l > 0 then partial.Result.Value(l) else partial.Result.fromErrorString("zero")
      )
      val zero = AccountDto("z", 0L)
      (cents.andThen(positive).getOption(good) === Some(120L))
        .and(paths(cents.andThen(positive).result(zero)) === List("" -> "zero"))
        .and(
          paths(cents.andThen(positive).result(bad)) === List("balance" -> "negative amount: -5")
        )
    }

    "turn an inner prism / AffineFold miss into chimney's empty-value error" in {
      val square = prism[Shape, Shape.Square]
      val shapes = ResultFold[Int, Shape](n =>
        partial.Result.Value(if n > 0 then Shape.Square(n.toDouble) else Shape.Circle(1.0))
      )
      val big = AffineFold[Shape.Square, Double](sq => Option.when(sq.side > 10)(sq.side))
      (shapes.andThen(square).getOption(2) === Some(Shape.Square(2.0)))
        .and(shapes.andThen(square).result(-1) must beAnInstanceOf[partial.Result.Errors])
        .and(
          shapes.andThen(square).andThen(big).result(2) must beAnInstanceOf[partial.Result.Errors]
        )
        .and(shapes.andThen(square).andThen(big).getOption(20) === Some(20.0))
    }

    "pass failFast through to chimney" in {
      val probe: PartialTransformer[Int, Boolean] = (_, failFast) => partial.Result.Value(failFast)
      (probe.toResultFold().getOption(0) === Some(false))
        .and(probe.toResultFold(failFast = true).getOption(0) === Some(true))
    }
  }

  "patch / Chimney.patching (option 3)" should {
    val order = Order(1L, Customer("Ada", "ada@old", "000"))

    "merge a patch into the focus, leaving other fields alone" in {
      lens[Order](_.customer).patch(ContactPatch("ada@new", "111"))(order) ===
        Order(1L, Customer("Ada", "ada@new", "111"))
    }

    "patch every focus of a Traversal" in {
      Traversal
        .each[List, Customer]
        .patch(ContactPatch("x", "y"))(List(order.customer, order.customer)) ===
        List(Customer("Ada", "x", "y"), Customer("Ada", "x", "y"))
    }

    "compose as a stored write-only optic" in {
      val contact = lens[Order](_.customer).andThen(Chimney.patching[Customer, ContactPatch])
      contact.replace(ContactPatch("e", "p"))(order) === Order(1L, Customer("Ada", "e", "p"))
    }
  }

  "transformFocus / transformFocusPartial (option 4)" should {
    val raws = List(RawPrice(100), RawPrice(250))

    "write the focus as another type through a monomorphic lens" in {
      val address = lens[User](_.address).transformFocus[AddressView]
      (address.replace(AddressView("Paris", "2 Rue"))(user) ===
        user.copy(address = Address("2 Rue", "Paris"))).and(
        address.modify(a => AddressView(a.city.toUpperCase, a.street))(user).address.city ===
          "LONDON"
      )
    }

    "keep reads on the original optic" in {
      lens[User](_.address).transformFocus[AddressView].get(user) === user.address
    }

    "write every focus of a polymorphic Traversal as the type it reads" in {
      // Reads RawPrice, writes Price; transformFocus[RawPrice] lets a RawPrice edit be written.
      Traversal
        .pEach[List, RawPrice, Price]
        .transformFocus[RawPrice]
        .modify(r => RawPrice(r.cents * 2))(raws) === List(Price(200), Price(500))
    }

    "retype a field through a polymorphic lens" in {
      val price = Lens.pLens[Line[RawPrice], Line[Price], RawPrice, Price](
        _.price,
        (l, p) => Line(l.sku, p),
      )
      price
        .transformFocus[RawPrice]
        .modify(r => RawPrice(r.cents + 1))(
          Line("sku-1", RawPrice(9))
        ) === Line("sku-1", Price(10))
    }

    "compose with transformImage into a fully retyped write side" in {
      val viewAddress =
        lens[User](_.address).transformFocus[AddressView].transformImage[UserView]
      viewAddress.replace(AddressView("Paris", "2 Rue"))(user) ===
        UserView("Ada", AddressView("Paris", "2 Rue"))
    }

    "never hand a failing write to the optic's own write" in {
      // This write reads the B it is given: a null placeholder would throw here.
      val inspecting = Lens.pLens[Line[RawPrice], Line[Price], RawPrice, Price](
        _.price,
        (l, p) => Line(l.sku, Price(p.cents.abs)),
      )
      val price = inspecting.transformFocusPartial[RawPrice]
      val line = Line("sku", RawPrice(1))
      (price.replace(RawPrice(7))(line).asOption === Some(Line("sku", Price(7)))).and(
        paths(price.replace(RawPrice(-7))(line)) === List("" -> "negative price: -7")
      )
    }

    "collect every written focus's errors" in {
      val prices = Traversal.pEach[List, RawPrice, Price].transformFocusPartial[RawPrice]
      val discount = (r: RawPrice) => RawPrice(r.cents - 150)
      (prices.modify(r => RawPrice(r.cents * 2))(raws).asOption ===
        Some(List(Price(200), Price(500)))).and(
        paths(prices.modify(discount)(raws ++ raws)) ===
          List("" -> "negative price: -50", "" -> "negative price: -50")
      )
    }
  }

  "transformImage / transformImagePartial" should {
    "hand a write back as the target type" in {
      lens[User](_.name).transformImage[UserView].modify(_.toUpperCase)(user) ===
        UserView("ADA", AddressView("London", "1 Analytical St"))
    }

    "keep reads on the original optic" in {
      lens[User](_.name).transformImage[UserView].get(user) === "Ada"
    }

    "transform a prism's miss too" in {
      val owner = Chimney
        .prism[Account, AccountDto]
        .andThen(lens[Account](_.owner))
        .transformImage[AccountRecord]
      (owner.modify(_.toUpperCase)(good) === AccountRecord(120L, "ADA")).and(
        owner.modify(_.toUpperCase)(bad) === AccountRecord(-5L, "bob")
      )
    }

    "validate the written value with a PartialTransformer" in {
      val balance = lens[AccountDto](_.balance).transformImagePartial[Account]
      (balance.modify(_ + 1)(good).asOption.map(_.balance.value) === Some(121L)).and(
        paths(balance.modify(_ - 200)(good)) === List("balance" -> "negative amount: -80")
      )
    }
  }
