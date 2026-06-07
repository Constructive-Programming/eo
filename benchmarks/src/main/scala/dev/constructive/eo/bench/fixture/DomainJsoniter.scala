package dev.constructive.eo
package bench
package fixture

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.Optic

/** jsoniter-scala codecs, EO jsoniter optics, and hand-rolled partial-scan codecs for the canonical
  * [[Order]] schema, shared by `OrderJsoniterBench` and the eo-jsoniter side of `JsoniterBench`.
  *
  * The hand-rolled scanners ([[streetScanCodec]] / [[pricesSumScanCodec]]) walk to a focus and
  * `skip()` every sibling — the optimum a jsoniter expert would write by hand, and exactly what
  * eo-jsoniter does generically. Their agreement with a full decode is asserted in each bench's
  * `@Setup` (JMH never checks return values, so that `require` is the only honesty guard).
  */
object DomainJsoniter:

  // ---- whole-document + leaf codecs ---------------------------------

  given JsonValueCodec[Order] = JsonCodecMaker.make
  given JsonValueCodec[Long] = JsonCodecMaker.make
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[Double] = JsonCodecMaker.make

  // ---- EO jsoniter optics (JSONPath) --------------------------------

  /** depth-1 scalar `$.id` (`Long`) — read / replace / modify vehicle. */
  val idPrism: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    JsoniterPrism[Long]("$.id")

  /** depth-3 scalar `$.customer.address.street` (`String`). */
  val streetPrism: Optic[Array[Byte], Array[Byte], String, String, Affine] =
    JsoniterPrism[String]("$.customer.address.street")

  /** a deliberately-absent path `$.customer.absent` — the honest "miss" stress test. */
  val absentPrism: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    JsoniterPrism[Long]("$.customer.absent")

  /** array fold `$.lines[*].price` (`Double`). */
  val pricesTraversal: Optic[Array[Byte], Array[Byte], Double, Double, MultiFocus[PSVec]] =
    JsoniterTraversal[Double]("$.lines[*].price")

  // ---- hand-rolled partial-scan codecs ------------------------------

  /** Read the value of `target` inside the current JSON object, skipping every other field, then
    * return `inner` applied to the reader positioned at that value. Returns `default` if the object
    * / field is absent.
    */
  private def field[A](in: JsonReader, target: String, default: A)(inner: JsonReader => A): A =
    if in.isNextToken('{') then
      var result = default
      if !in.isNextToken('}') then
        in.rollbackToken()
        while
          if in.readKeyAsString() == target then result = inner(in)
          else in.skip()
          in.isNextToken(',')
        do ()
      result
    else
      in.rollbackToken()
      in.skip()
      default

  /** Hand-rolled scanner for `$.customer.address.street` — walks only the path and `skip()`s
    * siblings, so the full `Order` is never materialised. A plain `val` (not `given`) so it doesn't
    * collide with the derived `JsonValueCodec[String]`; passed explicitly to `readFromArray`.
    */
  val streetScanCodec: JsonValueCodec[String] = new JsonValueCodec[String]:
    def nullValue: String = null
    def encodeValue(x: String, out: JsonWriter): Unit = out.writeVal(x)
    def decodeValue(in: JsonReader, default: String): String =
      field(in, "customer", default) { in =>
        field(in, "address", default) { in =>
          field(in, "street", default)(_.readString(null))
        }
      }

  /** Hand-rolled scanner for `$.lines[*].price` — walks to the `lines` array, sums each element's
    * `price`, and `skip()`s every sibling at both levels. The line objects are never fully
    * materialised.
    */
  val pricesSumScanCodec: JsonValueCodec[Double] = new JsonValueCodec[Double]:
    def nullValue: Double = 0.0
    def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)
    def decodeValue(in: JsonReader, default: Double): Double =
      field(in, "lines", default) { in =>
        var sum = 0.0
        if in.isNextToken('[') then
          if !in.isNextToken(']') then
            in.rollbackToken()
            while
              sum += field(in, "price", 0.0)(_.readDouble())
              in.isNextToken(',')
            do ()
        else in.rollbackToken()
        sum
      }
