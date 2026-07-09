package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray}
import dev.constructive.eo.bench.fixture.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

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

  import DomainJsoniter.{pricesSumScanCodec, pricesTraversal, streetPrism, streetScanCodec, given}
  import cats.instances.double.given
  import cats.instances.string.given

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var bytes: Array[Byte] = uninitialized

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
    streetPrism.foldMap(identity[String])(bytes)

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
    streetPrism.modify(_.toUpperCase)(bytes)

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
    pricesTraversal.foldMap(identity[Double])(bytes)

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
