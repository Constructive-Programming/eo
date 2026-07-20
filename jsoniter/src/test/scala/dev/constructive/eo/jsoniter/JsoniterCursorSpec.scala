package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.optics.Optic.*
import org.specs2.mutable.Specification

// Fixture ADTs at top level (mirrors the eo-generics convention for macro-facing test types).
final case class CursorZip(code: Int, ext: String)
final case class CursorAddress(street: String, zip: CursorZip)
final case class CursorPerson(name: String, age: Int, address: CursorAddress)
final case class CursorItem(sku: String, price: Double)
final case class CursorBasket(items: List[CursorItem], total: Double)

/** Typed cursor surface (issue #79 / eo-circe parity): `JsoniterPrism[A]` root prism, `.field(_.x)`
  * / `.at(i)` / `.each` drilling, Dynamic field sugar, and the traversal continuations. Every typed
  * cursor is the same `JsoniterPrism` / `JsoniterTraversal` the string-path factories build, so the
  * drilled optics are asserted equivalent to their JSONPath twins. Also pins the
  * consume-via-capability seam: a `JsoniterPrism` given serves the derived `CanGetOption` /
  * `CanModify` evidence.
  */
class JsoniterCursorSpec extends Specification:

  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[Int] = JsonCodecMaker.make
  given JsonValueCodec[Double] = JsonCodecMaker.make
  given JsonValueCodec[CursorZip] = JsonCodecMaker.make
  given JsonValueCodec[CursorAddress] = JsonCodecMaker.make
  given JsonValueCodec[CursorPerson] = JsonCodecMaker.make
  given JsonValueCodec[CursorItem] = JsonCodecMaker.make
  given JsonValueCodec[List[CursorItem]] = JsonCodecMaker.make
  given JsonValueCodec[CursorBasket] = JsonCodecMaker.make

  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  private val person =
    CursorPerson("alice", 42, CursorAddress("main st", CursorZip(12345, "9")))

  private val personBytes: Array[Byte] = writeToArray(person)

  private val basket =
    CursorBasket(List(CursorItem("a", 1.5), CursorItem("b", 2.5), CursorItem("c", 3.0)), 7.0)

  private val basketBytes: Array[Byte] = writeToArray(basket)

  // ----- root: the Prism[Array[Byte], A] --------------------------------

  "codecPrism root: decodes the whole document, reverseGet encodes it" >> {
    val rootP = JsoniterPrism[CursorPerson]
    (rootP.headOption(personBytes) === Some(person))
      .and(rootP.reverseGet(person).toSeq === personBytes.toSeq)
  }

  "codecPrism root: replace swaps the whole document, undecodable input passes through" >> {
    val rootP = JsoniterPrism[CursorPerson]
    val other = person.copy(name = "bob")
    val replaced = rootP.replace(other)(personBytes)
    val garbage = """{"unrelated":true}""".getBytes("UTF-8")
    (rootP.headOption(replaced) === Some(other))
      .and(rootP.headOption(garbage) === None)
      .and(rootP.modify(_.copy(age = 1))(garbage) === garbage)
  }

  // ----- field drilling -------------------------------------------------

  "field drilling ≡ string-path prism, on read and write" >> {
    val typed = JsoniterPrism[CursorPerson].field(_.address).field(_.street)
    val pathed = JsoniterPrism.fromPath[String]("$.address.street")
    (typed.headOption(personBytes) === Some("main st"))
      .and(typed.headOption(personBytes) === pathed.headOption(personBytes))
      .and(str(typed.replace("elm st")(personBytes)) === str(pathed.replace("elm st")(personBytes)))
  }

  "field write splices in place: siblings preserved" >> {
    val ageP = JsoniterPrism[CursorPerson].field(_.age)
    val out = ageP.modify(_ + 1)(personBytes)
    JsoniterPrism[CursorPerson].headOption(out) === Some(person.copy(age = 43))
  }

  "Dynamic sugar: JsoniterPrism[Person].address.street ≡ .field(_.address).field(_.street)" >> {
    val sugared = JsoniterPrism[CursorPerson].address.street
    val explicit = JsoniterPrism[CursorPerson].field(_.address).field(_.street)
    (sugared.steps === explicit.steps)
      .and(sugared.headOption(personBytes) === Some("main st"))
  }

  "field miss: absent-in-bytes path is a pass-through" >> {
    val zipExtP = JsoniterPrism[CursorPerson].field(_.address).field(_.zip).field(_.ext)
    val noExt = """{"name":"x","age":1,"address":{"street":"s","zip":{"code":9}}}"""
      .getBytes("UTF-8")
    (zipExtP.headOption(noExt) === None)
      .and(zipExtP.replace("0")(noExt) === noExt)
  }

  // ----- at / each ------------------------------------------------------

  ".at(i) drills into the i-th element" >> {
    val secondP = JsoniterPrism[CursorBasket].field(_.items).at(1)
    (secondP.headOption(basketBytes) === Some(CursorItem("b", 2.5)))
      .and(
        secondP.headOption(basketBytes) ===
          JsoniterPrism.fromPath[CursorItem]("$.items[1]").headOption(basketBytes)
      )
  }

  ".each fans out as a JsoniterTraversal; .field continues past the wildcard" >> {
    import cats.instances.double.given
    import cats.instances.int.given

    val pricesT = JsoniterPrism[CursorBasket].field(_.items).each.field(_.price)
    (pricesT.foldMap(identity[Double])(basketBytes) === 7.0)
      .and(pricesT.foldMap(_ => 1)(basketBytes) === 3)
      .and(
        str(pricesT.modify(_ * 2)(basketBytes)) ===
          str(JsoniterTraversal[Double]("$.items[*].price").modify(_ * 2)(basketBytes))
      )
  }

  "traversal Dynamic sugar: .each.price ≡ .each.field(_.price)" >> {
    import cats.instances.double.given

    val sugared = JsoniterPrism[CursorBasket].field(_.items).each.price
    sugared.foldMap(identity[Double])(basketBytes) === 7.0
  }

  // ----- capabilities ---------------------------------------------------

  "capability consumption: a JsoniterPrism given serves CanGetOption / CanModify evidence" >> {
    import dev.constructive.eo.{CanGetOption, CanModify}

    // Consumer signatures know nothing about the wire format — only the evidence.
    def readName[T](t: T)(using cg: CanGetOption[T, String]): Option[String] = cg.getOption(t)
    def upcaseName[T](t: T)(using cm: CanModify[T, String]): T = cm.modify(_.toUpperCase)(t)

    given JsoniterPrism[String] = JsoniterPrism[CursorPerson].field(_.name)

    (readName(personBytes) === Some("alice"))
      .and(readName(upcaseName(personBytes)) === Some("ALICE"))
  }
