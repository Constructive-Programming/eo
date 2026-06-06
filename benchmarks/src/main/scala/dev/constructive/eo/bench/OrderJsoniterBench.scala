package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromArray, writeToArray}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.Optic

/** Canonical-schema jsoniter bench (plan 009, Phase 1).
  *
  * eo-jsoniter walks the raw `Array[Byte]` directly (skip the AST entirely), so its baselines
  * differ from the circe bench: jsoniter has no cursor, so "native" and "naive" collapse into the
  * single full-codec round-trip (`readFromArray` → modify → `writeToArray`). Three baselines per
  * metric:
  *
  *   - `eo*` — eo-jsoniter `JsoniterPrism` / `JsoniterTraversal` over the bytes.
  *   - `native*` — jsoniter's own codec round-trip (decode the whole `Order`, modify, re-encode).
  *   - `monocle*` — same jsoniter codec round-trip, but the in-memory modify is a Monocle optic.
  *
  * Three foci:
  *   - **read** a depth-3 scalar `$.customer.address.street` (extract without materialising the
  *     doc).
  *   - **write** the same scalar (`.modify` re-splices only the focus span).
  *   - **fold** the `$.lines[*].price` array (read-only `[*]` traversal — jsoniter's phase-1.5
  *     array surface is fold-only; the write-traversal story lives in the circe / avro benches).
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

  import OrderJsoniterBench.given
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

  // ---- read scalar: $.customer.address.street -----------------------

  @Benchmark def eoReadStreet: String =
    eoStreetP.foldMap(identity[String])(bytes)

  @Benchmark def nativeReadStreet: String =
    readFromArray[Order](bytes).customer.address.street

  @Benchmark def monocleReadStreet: String =
    DomainMonocle.street.get(readFromArray[Order](bytes))

  // ---- write scalar: $.customer.address.street ----------------------

  @Benchmark def eoModifyStreet: Array[Byte] =
    eoStreetP.modify(_.toUpperCase)(bytes)

  @Benchmark def nativeModifyStreet: Array[Byte] =
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

  @Benchmark def nativeSumPrices: Double =
    readFromArray[Order](bytes).lines.map(_.price).sum

  @Benchmark def monocleSumPrices: Double =
    DomainMonocle.prices.getAll(readFromArray[Order](bytes)).sum

object OrderJsoniterBench:

  // Whole-document codec for the native / monocle round-trip baselines.
  given JsonValueCodec[Order] = JsonCodecMaker.make

  // Leaf codecs the JsoniterPrism / JsoniterTraversal foci decode through.
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[Double] = JsonCodecMaker.make
