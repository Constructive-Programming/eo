package dev.constructive.eo
package examples

import io.circe.syntax.*
import org.specs2.mutable.Specification

import CrudRoundtrip.*

/** Motivating example for cats-eo — the "why this library exists" showcase (happy path).
  *
  * Scenario: an HTTP endpoint receives a JSON user-update, runs effectful validation over specific
  * fields (including every `shippingAddress.zipCode` of every order the user owns), persists the
  * result through a toy database, and replies with the new id.
  *
  * The punchline is [[CrudRoundtrip.handleWithOptics]]: every validation step is one line,
  * `optic.modifyA[Result](validator)`. Compare with [[CrudRoundtrip.handleNaive]], whose order-list
  * traversal writes the `copy(copy(copy(...)))` chain by hand. The error path lives in
  * [[CrudRoundtripErrorPathSpec]]; the model + optics + handlers in [[CrudRoundtrip]].
  */
class CrudRoundtripHappyPathSpec extends Specification:

  "CRUD round-trip — happy path" should {

    val alice = User(
      id = 42L,
      name = "  Alice  ",
      address = Address("Main St", "Amsterdam", "01234"),
      orders = List(
        Order(1L, Address("Ship1", "Rotterdam", "56789"), List(Item("SKU-A", 2))),
        Order(2L, Address("Ship2", "Utrecht", "24680"), List(Item("SKU-B", 1))),
      ),
    )

    "naive and optic handlers persist the same user" >> {
      val dbNaive = InMemoryDb()
      val dbOptic = InMemoryDb()
      val payload = alice.asJson

      val idNaive = handleNaive(dbNaive, payload)
      val idOptic = handleWithOptics(dbOptic, payload)

      (idNaive.isRight && idOptic.isRight) must beTrue
      dbNaive.snapshot.values.toSet === dbOptic.snapshot.values.toSet
    }

    "optic handler normalises the user's name in place" >> {
      val db = InMemoryDb()
      val body = alice.asJson
      handleWithOptics(db, body)
        .map(id => db.snapshot(id).name) === Right("Alice")
    }
  }

end CrudRoundtripHappyPathSpec
