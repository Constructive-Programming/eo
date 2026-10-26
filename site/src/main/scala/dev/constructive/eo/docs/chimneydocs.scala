package dev.constructive.eo.docs.chimney

import io.scalaland.chimney.{partial, PartialTransformer, Transformer}

// Top-level ADTs for site/docs/integrations/chimney.md — see samples.scala for why macro-derived
// lenses need them here rather than in the fences.

final case class Address(street: String, city: String)
final case class User(name: String, password: String, address: Address)

final case class AddressView(city: String, street: String)
final case class UserView(name: String, address: AddressView)

final case class Person(name: String, age: Int)
final case class PersonRow(age: Int, name: String)

/** Non-negative money: only `Cents.from` builds one, so decoding a DTO can fail. */
final case class Cents private (value: Long)

object Cents:
  def from(value: Long): Option[Cents] = Option.when(value >= 0)(new Cents(value))

  given Transformer[Cents, Long] = _.value

  given PartialTransformer[Long, Cents] =
    PartialTransformer(l => partial.Result.fromOptionOrString(from(l), s"negative amount: $l"))

final case class Account(owner: String, balance: Cents)
final case class AccountDto(owner: String, balance: Long)

final case class Customer(name: String, email: String, phone: String)
final case class Order(id: Long, customer: Customer)
final case class ContactPatch(email: String, phone: String)

final case class RawPrice(cents: Long)
final case class Price(cents: Long)

object Price:

  given PartialTransformer[RawPrice, Price] =
    PartialTransformer(r =>
      if r.cents >= 0 then partial.Result.Value(Price(r.cents))
      else partial.Result.fromErrorString(s"negative price: ${r.cents}")
    )
