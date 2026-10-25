package dev.constructive.eo
package chimney

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import generics.lens
import laws.{GetterLaws, IsoLaws, OptionalLaws, PrismLaws}
import laws.discipline.{GetterTests, IsoTests, OptionalTests, PrismTests}
import samples.*

/** The bridges are only as lawful as the chimney values behind them — these pin the derived
  * fixtures' round trips, and the composed chains through eo-generics' lenses.
  */
class ChimneyLawsSpec extends Specification with Discipline:

  given Arbitrary[Address] = Arbitrary(Gen.resultOf(Address.apply))
  given Arbitrary[User] = Arbitrary(Gen.resultOf(User.apply))
  given Arbitrary[Person] = Arbitrary(Gen.resultOf(Person.apply))
  given Arbitrary[PersonRow] = Arbitrary(Gen.resultOf(PersonRow.apply))
  given Cogen[PersonRow] = Cogen[(Int, String)].contramap(r => (r.age, r.name))
  given Cogen[Int] = Cogen.cogenInt

  // Negative balances reach the prism too, so the miss side of every law is exercised.
  given Arbitrary[AccountDto] =
    Arbitrary(Gen.resultOf(AccountDto.apply)(using summon, Arbitrary(Gen.choose(-50L, 50L))))

  given Arbitrary[Account] = Arbitrary(
    for
      owner <- Arbitrary.arbitrary[String]
      cents <- Gen.choose(0L, 1000000L)
    yield Account(owner, Cents.from(cents).get)
  )

  given Cogen[Account] = Cogen[(String, Long)].contramap(a => (a.owner, a.balance.value))
  given Cogen[String] = Cogen.cogenString

  given Arbitrary[LedgerDto] = Arbitrary(Gen.resultOf(LedgerDto.apply))
  given Arbitrary[Ledger] = Arbitrary(Gen.resultOf(Ledger.apply))
  given Cogen[Ledger] = Cogen[(String, Account)].contramap(l => (l.name, l.account))

  given Arbitrary[Customer] = Arbitrary(Gen.resultOf(Customer.apply))
  given Arbitrary[Order] = Arbitrary(Gen.resultOf(Order.apply))
  given Arbitrary[ContactPatch] = Arbitrary(Gen.resultOf(ContactPatch.apply))

  checkAll(
    "Chimney.getter[User, UserView]",
    new GetterTests[User, UserView]:
      val laws = new GetterLaws[User, UserView]:
        val getter = Chimney.getter[User, UserView]
        val reference = (u: User) => UserView(u.name, AddressView(u.address.city, u.address.street))
    .getter,
  )

  checkAll(
    "Chimney.getter[User, UserView] ∘ lens(_.address) ∘ lens(_.city)",
    new GetterTests[User, String]:
      val laws = new GetterLaws[User, String]:
        val getter = Chimney
          .getter[User, UserView]
          .andThen(lens[UserView](_.address))
          .andThen(lens[AddressView](_.city))
        val reference = (u: User) => u.address.city
    .getter,
  )

  checkAll(
    "Chimney.iso[Person, PersonRow]",
    new IsoTests[Person, PersonRow]:
      val laws = new IsoLaws[Person, PersonRow]:
        val iso = Chimney.iso[Person, PersonRow]
    .iso,
  )

  checkAll(
    "Chimney.prism[Account, AccountDto]",
    new PrismTests[AccountDto, Account]:
      val laws = new PrismLaws[AccountDto, Account]:
        val prism = Chimney.prism[Account, AccountDto]
    .prism,
  )

  checkAll(
    "Chimney.prism[Ledger, LedgerDto] (nested validation)",
    new PrismTests[LedgerDto, Ledger]:
      val laws = new PrismLaws[LedgerDto, Ledger]:
        val prism = Chimney.prism[Ledger, LedgerDto]
    .prism,
  )

  checkAll(
    "Chimney.prism[Account, AccountDto] ∘ lens(_.owner)",
    new OptionalTests[AccountDto, String]:
      val laws = new OptionalLaws[AccountDto, String]:
        val optional = Chimney.prism[Account, AccountDto].andThen(lens[Account](_.owner))
    .optional,
  )

  "ResultFold" should {
    val decode = Chimney.resultFold[AccountDto, Account]

    "agree with its error-free view and its foldMap" in forAll { (dto: AccountDto) =>
      decode.getOption(dto) == decode.result(dto).asOption &&
      decode.affineFold.getOption(dto) == decode.getOption(dto) &&
      decode.foldMap(List(_))(dto) == decode.getOption(dto).toList
    }

    "keep the outer errors through a lens" in forAll { (dto: AccountDto) =>
      decode.andThen(lens[Account](_.owner)).either(dto).left.map(_.asErrorPathMessageStrings) ==
        decode.either(dto).map(_.owner).left.map(_.asErrorPathMessageStrings)
    }
  }

  // A patch is a write with no read, so the Modify laws (which need `A = B`) don't apply; these
  // are the write-only laws that do: replace is idempotent, and the stored optic agrees with the
  // one-off `.patch`.
  // transformImage only post-composes the write side, so on hits and misses alike it must equal
  // "write with the original optic, then transform".
  "transformImage" should {
    val owner = Chimney.prism[Account, AccountDto].andThen(lens[Account](_.owner))
    val toRecord = summon[io.scalaland.chimney.Transformer[AccountDto, AccountRecord]]

    "be the original write followed by the transformer" in forAll {
      (dto: AccountDto, f: String => String) =>
        owner.transformImage[AccountRecord].modify(f)(dto) ==
          toRecord.transform(owner.modify(f)(dto))
    }
  }

  // transformFocus only pre-composes the write side with the transformer, so writing a C must
  // equal converting it and writing the B with the original optic.
  "transformFocus" should {
    val address = lens[User](_.address)
    val fromView = summon[io.scalaland.chimney.Transformer[AddressView, Address]]

    "be the transformer followed by the original write" in forAll {
      (u: User, street: String, city: String) =>
        val v = AddressView(city, street)
        val viaFocus = address.transformFocus[AddressView].replace(v)(u)
        viaFocus == address.replace(fromView.transform(v))(u)
    }
  }

  "patch" should {
    val customer = lens[Order](_.customer)
    val contact = customer.andThen(Chimney.patching[Customer, ContactPatch])

    "be idempotent for a field-overwriting patch" in forAll { (o: Order, p: ContactPatch) =>
      val once = customer.patch(p)(o)
      customer.patch(p)(once) == once
    }

    "agree with the composed patching optic" in forAll { (o: Order, p: ContactPatch) =>
      contact.replace(p)(o) == customer.patch(p)(o)
    }
  }
