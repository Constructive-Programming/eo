package dev.constructive.eo.chimney.samples

import io.scalaland.chimney.{partial, PartialTransformer, Transformer}

// Top-level fixtures: eo-generics' macro-built `new T(...)` calls carry no outer accessor, so ADTs
// nested in a spec class trip the back-end (see CLAUDE.md, "Auto-derivation").

// --- Transformer → Getter: a domain user read as its public view -------------------------------

final case class Address(street: String, city: String)
final case class User(name: String, age: Int, password: String, address: Address)

final case class AddressView(city: String, street: String)
final case class UserView(name: String, address: AddressView)

// --- chimney Iso → eo Iso: the same record, two field orders ----------------------------------

final case class Person(name: String, age: Int)
final case class PersonRow(age: Int, name: String)

// --- Codec → Prism: a validated domain value and its wire DTO ----------------------------------

/** Non-negative money: only `Cents.from` builds one, so decoding a DTO can fail. */
final case class Cents private (value: Long)

object Cents:
  def from(value: Long): Option[Cents] = Option.when(value >= 0)(new Cents(value))

  given Transformer[Cents, Long] = _.value

  given PartialTransformer[Long, Cents] =
    PartialTransformer(l => partial.Result.fromOptionOrString(from(l), s"negative amount: $l"))

final case class Account(owner: String, balance: Cents)
final case class AccountDto(owner: String, balance: Long)

final case class Ledger(name: String, account: Account)
final case class LedgerDto(name: String, account: AccountDto)

// --- Patcher: a sparse update merged into a nested record -------------------------------------

final case class Customer(name: String, email: String, phone: String)
final case class Order(id: Long, customer: Customer)

final case class ContactPatch(email: String, phone: String)

// --- transformFocus: retyping a polymorphic focus ---------------------------------------------

final case class RawPrice(cents: Long)
final case class Price(cents: Long)

object Price:

  given PartialTransformer[RawPrice, Price] =
    PartialTransformer(r =>
      if r.cents >= 0 then partial.Result.Value(Price(r.cents))
      else partial.Result.fromErrorString(s"negative price: ${r.cents}")
    )

final case class Line[P](sku: String, price: P)

// --- ResultFold ∘ prism: a sum type --------------------------------------------------------------

enum Shape:
  case Circle(radius: Double)
  case Square(side: Double)
