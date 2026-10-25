package dev.constructive.eo.bench.fixture

import io.scalaland.chimney.{partial, PartialTransformer}

// Top-level ADTs for ChimneyBench: eo-generics' `lens` macro emits `new T(...)` with no outer
// accessor, so the case classes can't be nested in the bench class.

final case class ChAddress(street: String, city: String)
final case class ChUser(name: String, age: Int, address: ChAddress)

final case class ChAddressView(city: String, street: String)
final case class ChUserView(name: String, address: ChAddressView)

/** Non-negative money: only `ChCents.from` builds one, so decoding can fail. */
final case class ChCents private (value: Long)

object ChCents:
  def from(value: Long): Option[ChCents] = Option.when(value >= 0)(new ChCents(value))

  given io.scalaland.chimney.Transformer[ChCents, Long] = _.value

  given PartialTransformer[Long, ChCents] =
    PartialTransformer(l => partial.Result.fromOptionOrString(from(l), "negative amount"))

final case class ChAccount(owner: String, balance: ChCents)
final case class ChAccountDto(owner: String, balance: Long)

final case class ChCustomer(name: String, email: String, phone: String)
final case class ChOrder(id: Long, customer: ChCustomer)
final case class ChContactPatch(email: String, phone: String)

final case class ChRawPrice(cents: Long)
final case class ChPrice(cents: Long)

object ChPrice:
  given PartialTransformer[ChRawPrice, ChPrice] =
    PartialTransformer(r =>
      if r.cents >= 0 then partial.Result.Value(ChPrice(r.cents))
      else partial.Result.fromErrorString("negative price")
    )
