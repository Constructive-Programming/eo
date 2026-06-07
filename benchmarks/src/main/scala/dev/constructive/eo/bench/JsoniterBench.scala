package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.MultiFocus.given

import io.circe.Json
import io.circe.parser.parse as circeParse

/** `eo-jsoniter` vs `eo-circe` on the canonical [[Order]] — the cross-EO comparison the
  * `Order*Bench` baselines don't cover (those pit one backend against native/naive/monocle).
  *
  * eo-jsoniter walks the raw `Array[Byte]`; eo-circe parses the same bytes to an AST and drills it.
  * Parsing is part of eo-circe's realistic workflow, so the `c*` rows always `circeParse(bytes)`
  * first — the comparison is "byte-walk" vs "parse + AST-walk" on identical input.
  *
  * Foci, all on the shared schema + shared codecs ([[fixture.DomainJsoniter]] /
  * [[fixture.DomainCirce]]):
  *   - **read `$.id`** (depth-1 `Long`) and **read `$.customer.address.street`** (depth-3 `String`)
  *     — two depths confirm the byte-walk's advantage doesn't degrade as the focus sinks.
  *   - **fold `$.lines[*].price`** — array aggregation; a fold must visit every element either way,
  *     so this is the case where the gap is narrowest.
  *   - **miss `$.customer.absent`** — eo-jsoniter only. A typed circe codec can't drill a
  *     non-existent field, so there's no honest circe peer; this row is the byte-walker's own
  *     hit-vs-miss cost, not a cross-library comparison.
  *   - **`.replace` / `.modify` `$.id`** — jsoniter splices the byte span; eo-circe modifies the
  *     AST and re-emits via `noSpaces`.
  *
  * `@Param size` grows the surrounding document so the parse cost (circe) climbs while the
  * byte-walk (jsoniter) stays focus-local.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class JsoniterBench extends JmhDefaults:

  import DomainJsoniter.given
  import cats.instances.double.given
  import cats.instances.long.given
  import cats.instances.string.given

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var bytes: Array[Byte] = uninitialized

  @Setup(Level.Iteration)
  def init(): Unit =
    bytes = writeToArray(Domain.mkOrder(size))

  private def json: Json = circeParse(new String(bytes, "UTF-8")).getOrElse(Json.Null)

  // ---- read $.id (depth-1, Long) ------------------------------------

  @Benchmark def jReadId: Long = DomainJsoniter.idPrism.foldMap(identity[Long])(bytes)
  @Benchmark def cReadId: Long = DomainCirce.idPrism.foldMap(identity[Long])(json)

  // ---- read $.customer.address.street (depth-3, String) -------------

  @Benchmark def jReadStreet: String = DomainJsoniter.streetPrism.foldMap(identity[String])(bytes)
  @Benchmark def cReadStreet: String = DomainCirce.streetPrism.foldMap(identity[String])(json)

  // ---- fold $.lines[*].price ----------------------------------------

  @Benchmark def jSumPrices: Double =
    DomainJsoniter.pricesTraversal.foldMap(identity[Double])(bytes)

  @Benchmark def cSumPrices: Double = DomainCirce.pricesFold.foldMap(identity[Double])(json)

  // ---- miss $.customer.absent (eo-jsoniter only — see scaladoc) -----

  @Benchmark def jMiss: Long = DomainJsoniter.absentPrism.foldMap(identity[Long])(bytes)

  // ---- .replace / .modify $.id --------------------------------------

  @Benchmark def jReplaceId: Array[Byte] = DomainJsoniter.idPrism.replace(99L)(bytes)
  @Benchmark def cReplaceId: String = DomainCirce.idPrism.placeUnsafe(99L)(json).noSpaces

  @Benchmark def jModifyId: Array[Byte] = DomainJsoniter.idPrism.modify(_ * 10)(bytes)
  @Benchmark def cModifyId: String = DomainCirce.idPrism.modifyUnsafe(_ * 10)(json).noSpaces
