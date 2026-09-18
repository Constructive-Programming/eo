package dev.constructive.eo.circe

import scala.language.implicitConversions

import io.circe.syntax.*
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck

/** Index-bounds discrimination for the array walk.
  *
  * `JsonWalk` carries TWO copies of `idx < 0 || idx >= arr.length` — one in `readPath`, one in
  * `modifyPath`. The previous version of this spec drove only `.modify(…)`, so every operator
  * mutant on the `readPath` copy survived. The property below drives the read, the Ior write and
  * the silent write from one index, against an oracle DERIVED from the backing `Vector` rather than
  * hard-coded, so a guard that mis-fires is caught whichever direction it mis-fires in.
  */
class JsonIndexBoundsSpec extends JsonSpecBase with ScalaCheck:

  import JsonSpecFixtures.*

  // covers: JsonWalk.scala:56 readPath Index bounds, every operator variant (:20 `→false`,
  //   :24 `<`→`<=`, :24 `<`→`==`, :28 `||`→`&&`, :35 `>=`→`>`, :35 `>=`→`==`) and the
  //   already-covered twin at JsonWalk.scala:81 (modifyPath).
  //   Range -2..5 against a FIXED size-3 array straddles all five discriminating classes:
  //   i<0, i=0 (low boundary), 0<i<3, i=3 (exactly length), i>3 (strictly past length).
  //   i=3 alone kills `>=`→`>`; i>=4 alone kills `>=`→`==`; i=0 alone kills both `<` variants.
  "read / Ior write / silent write agree with the Vector oracle at every index class" >> {
    val items = Vector(Order("A"), Order("B"), Order("C"))
    val basket: Json = Basket("Alice", items).asJson
    forAll(Gen.chooseNum(-2, 5)) { (i: Int) =>
      val p = codecPrism[Basket].items.at(i)
      val hit = i >= 0 && i < items.length
      val oor: JsonFailure = JsonFailure.IndexOutOfRange(PathStep.Index(i), items.length)

      val read: Ior[Chain[JsonFailure], Order] = p.get(basket)
      val expectedRead: Ior[Chain[JsonFailure], Order] =
        if hit then Ior.Right(items(i)) else Ior.Left(Chain.one(oor))

      val write: Ior[Chain[JsonFailure], Json] = p.modify(identity)(basket)
      val expectedWrite: Ior[Chain[JsonFailure], Json] =
        if hit then Ior.Right(basket) else Ior.Both(Chain.one(oor), basket)

      val silent: Json = p.modifyUnsafe(identity)(basket)

      (read == expectedRead) && (write == expectedWrite) && (silent == basket)
    }
  }
