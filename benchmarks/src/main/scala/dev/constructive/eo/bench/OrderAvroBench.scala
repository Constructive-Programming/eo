package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*

import org.apache.avro.generic.IndexedRecord

/** Canonical-schema avro bench (plan 009, Phase 1).
  *
  * Unlike jsoniter, eo-avro's `[*]` traversal writes back, so avro carries the same two foci as the
  * circe bench — a depth-3 scalar and an array traversal. Three baselines per metric:
  *
  *   - `eo*` — eo-avro `AvroPrism` / `AvroTraversal` walking the `IndexedRecord` (`*Unsafe` hot
  *     path).
  *   - `naive*` — the kindlings codec round-trip (`decodeEither` → case-class `copy` → `encode`),
  *     which materialises the whole record. (No hand-optimized `native*` partial-walk is benched —
  *     that's exactly what eo-avro already is.)
  *   - `monocle*` — same codec round-trip, but the in-memory modify is a Monocle optic.
  *
  * Foci: `customer.address.street` (depth-3 scalar, modify + read) and `lines[*].name` (array write
  * traversal). `customer.loyaltyId` is *not* focused: kindlings encodes `Option` as an Avro union
  * navigated via `.union[Branch]`, not a transparent field, so it isn't a clean cross-backend peer
  * (plan 009, Avro `Option` caveat).
  *
  * `@Param size` grows the record so the path-walk's O(depth) cost shows against the codec
  * round-trip's O(all-fields) cost.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class OrderAvroBench extends JmhDefaults:

  import DomainAvro.{codec, namesTraversal, streetPrism}

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var record: IndexedRecord = uninitialized

  @Setup(Level.Iteration)
  def init(): Unit =
    record = codec.encode(Domain.mkOrder(size)).asInstanceOf[IndexedRecord]

  // ---- read scalar: customer.address.street -------------------------

  @Benchmark def eoReadStreet: Option[String] =
    streetPrism.getOptionUnsafe(record)

  @Benchmark def naiveReadStreet: String =
    codec.decodeEither(record).toOption.get.customer.address.street

  @Benchmark def monocleReadStreet: String =
    DomainMonocle.street.get(codec.decodeEither(record).toOption.get)

  // ---- write scalar: customer.address.street ------------------------

  @Benchmark def eoModifyStreet: IndexedRecord =
    streetPrism.modifyUnsafe(_.toUpperCase)(record)

  @Benchmark def naiveModifyStreet: IndexedRecord =
    val o = codec.decodeEither(record).toOption.get
    codec
      .encode(
        o.copy(customer =
          o.customer
            .copy(address = o.customer.address.copy(street = o.customer.address.street.toUpperCase))
        )
      )
      .asInstanceOf[IndexedRecord]

  @Benchmark def monocleModifyStreet: IndexedRecord =
    val o = codec.decodeEither(record).toOption.get
    codec.encode(DomainMonocle.street.modify(_.toUpperCase)(o)).asInstanceOf[IndexedRecord]

  // ---- write array: lines[*].name -----------------------------------

  @Benchmark def eoModifyNames: IndexedRecord =
    namesTraversal.modifyUnsafe(_.toUpperCase)(record)

  @Benchmark def naiveModifyNames: IndexedRecord =
    val o = codec.decodeEither(record).toOption.get
    codec
      .encode(o.copy(lines = o.lines.map(li => li.copy(name = li.name.toUpperCase))))
      .asInstanceOf[IndexedRecord]

  @Benchmark def monocleModifyNames: IndexedRecord =
    val o = codec.decodeEither(record).toOption.get
    codec.encode(DomainMonocle.names.modify(_.toUpperCase)(o)).asInstanceOf[IndexedRecord]
