package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import com.github.plokhotnyuk.jsoniter_scala.core.{
  JsonReader,
  JsonValueCodec,
  JsonWriter,
  readFromArray,
  writeToArray,
}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.Optic

/** Canonical-schema jsoniter bench (plan 009, Phase 1).
  *
  * eo-jsoniter walks the raw `Array[Byte]` directly (skip the AST entirely). Baselines:
  *
  *   - `eo*` — eo-jsoniter `JsoniterPrism` / `JsoniterTraversal` over the bytes.
  *   - `native*` (reads / fold) — a hand-rolled `JsonReader` codec that walks to the focus and
  *     `skip()`s every sibling, so the whole `Order` is never materialised. This is the optimum a
  *     jsoniter expert would write by hand — and exactly what eo-jsoniter does generically.
  *   - `naive*` — jsoniter's full-codec round-trip (`readFromArray[Order]` → modify →
  *     `writeToArray`), which materialises the entire object.
  *   - `monocle*` — same full round-trip, but the in-memory modify is a Monocle optic.
  *
  * Three foci:
  *   - **read** a depth-3 scalar `$.customer.address.street` (`eo` / `native` partial-scan /
  *     `naive` full-decode / `monocle`).
  *   - **write** the same scalar (`.modify` re-splices only the focus span; only `eo` avoids the
  *     full read here — a hand-rolled partial-splice writer is essentially eo-jsoniter itself).
  *   - **fold** the `$.lines[*].price` array (`eo` / `native` partial-scan sum / `naive`
  *     full-decode / `monocle`). eo's `[*]` is read-only in jsoniter phase-1.5, so this is a fold,
  *     not a write traversal — the write-traversal story lives in the circe / avro benches.
  *
  * `@Param size` grows the surrounding document so the byte-walk's depth-independent cost shows
  * against the codec round-trip's O(all-fields) cost.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class OrderJsoniterBench extends JmhDefaults:

  import OrderJsoniterBench.{pricesSumScanCodec, streetScanCodec, given}
  import cats.instances.double.given
  import cats.instances.string.given

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var bytes: Array[Byte] = uninitialized

  private val eoStreetP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
    JsoniterPrism[String]("$.customer.address.street")

  private val eoPricesT: Optic[Array[Byte], Array[Byte], Double, Double, MultiFocus[PSVec]] =
    JsoniterTraversal[Double]("$.lines[*].price")

  @Setup(Level.Iteration)
  def init(): Unit =
    bytes = writeToArray(Domain.mkOrder(size))
    // Correctness guard: the hand-rolled partial scanner must agree with the
    // full decode (street = "Main St" for the default customer). JMH never
    // checks return values, so this is the only thing keeping the `native*`
    // read honest.
    require(
      readFromArray(bytes)(using streetScanCodec) == readFromArray[Order](bytes)
        .customer
        .address
        .street,
      "streetScanCodec disagrees with the full decode",
    )
    require(
      math.abs(
        readFromArray(bytes)(using pricesSumScanCodec) - readFromArray[Order](bytes)
          .lines
          .map(_.price)
          .sum
      ) < 1e-9,
      "pricesSumScanCodec disagrees with the full decode",
    )

  // ---- read scalar: $.customer.address.street -----------------------

  @Benchmark def eoReadStreet: String =
    eoStreetP.foldMap(identity[String])(bytes)

  // The optimum native read: a hand-rolled JsonReader that walks to the focus
  // and `skip()`s every other field — the whole object is *not* materialised.
  // This is exactly what eo-jsoniter does generically; here it's spelled out
  // by hand for the one path.
  @Benchmark def nativeReadStreet: String =
    readFromArray(bytes)(using streetScanCodec)

  // The naive read: decode the entire `Order`, then project the field.
  @Benchmark def naiveReadStreet: String =
    readFromArray[Order](bytes).customer.address.street

  @Benchmark def monocleReadStreet: String =
    DomainMonocle.street.get(readFromArray[Order](bytes))

  // ---- write scalar: $.customer.address.street ----------------------

  @Benchmark def eoModifyStreet: Array[Byte] =
    eoStreetP.modify(_.toUpperCase)(bytes)

  // Naive write: full decode → copy → re-encode. (A native partial-splice
  // write is what eo-jsoniter's `.modify` already does — there's no simpler
  // hand-rolled JsonReader form for the write path, so it's not benched here.)
  @Benchmark def naiveModifyStreet: Array[Byte] =
    val o = readFromArray[Order](bytes)
    writeToArray(
      o.copy(customer =
        o.customer
          .copy(address = o.customer.address.copy(street = o.customer.address.street.toUpperCase))
      )
    )

  @Benchmark def monocleModifyStreet: Array[Byte] =
    writeToArray(DomainMonocle.street.modify(_.toUpperCase)(readFromArray[Order](bytes)))

  // ---- fold array: $.lines[*].price ---------------------------------

  @Benchmark def eoSumPrices: Double =
    eoPricesT.foldMap(identity[Double])(bytes)

  // The optimum native fold: walk to `lines`, sum each element's `price`,
  // skip()ing every sibling field at both levels — the line objects are never
  // fully materialised. (A fold must still visit every element, so this won't
  // beat the full decode by the margin the scalar read does.)
  @Benchmark def nativeSumPrices: Double =
    readFromArray(bytes)(using pricesSumScanCodec)

  @Benchmark def naiveSumPrices: Double =
    readFromArray[Order](bytes).lines.map(_.price).sum

  @Benchmark def monocleSumPrices: Double =
    DomainMonocle.prices.getAll(readFromArray[Order](bytes)).sum

object OrderJsoniterBench:

  // Whole-document codec for the naive / monocle round-trip baselines.
  given JsonValueCodec[Order] = JsonCodecMaker.make

  // Leaf codecs the JsoniterPrism / JsoniterTraversal foci decode through.
  given JsonValueCodec[String] = JsonCodecMaker.make

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

  given JsonValueCodec[Double] = JsonCodecMaker.make
