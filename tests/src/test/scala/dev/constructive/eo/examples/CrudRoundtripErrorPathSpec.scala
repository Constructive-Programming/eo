package dev.constructive.eo
package examples

import io.circe.syntax.*
import org.specs2.mutable.Specification

import CrudRoundtrip.*

/** Error-path half of the CRUD round-trip showcase (happy path in [[CrudRoundtripHappyPathSpec]],
  * model + optics + handlers in [[CrudRoundtrip]]): an invalid zip in a nested order must surface
  * as the same `Problem` from both the naive and the optic handler.
  */
class CrudRoundtripErrorPathSpec extends Specification:

  "CRUD round-trip — error path" should {

    val bob = User(
      id = 7L,
      name = "Bob",
      address = Address("Main St", "Amsterdam", "01234"),
      orders = List(
        Order(1L, Address("Ship1", "Rotterdam", "not-a-zip"), Nil)
      ),
    )

    "both handlers return the same Problem on an invalid nested zip" >> {
      val db = InMemoryDb()
      val payload = bob.asJson
      handleNaive(db, payload) === handleWithOptics(db, payload)
    }
  }

end CrudRoundtripErrorPathSpec
