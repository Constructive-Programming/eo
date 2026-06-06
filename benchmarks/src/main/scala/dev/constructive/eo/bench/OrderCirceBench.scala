package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.circe.{JsonFailure, JsonPrism, codecPrism}

import cats.data.{Chain, Ior}

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** Canonical-schema circe bench (plan 009, Phase 1 proof-of-concept).
  *
  * One realistic [[Order]] document, two foci, four baselines side-by-side per metric so a single
  * JMH report row tells the whole story:
  *
  *   - `eo*` / `eoIor*` — the EO `JsonPrism` / `JsonTraversal` (silent `*Unsafe` hot path and the
  *     default `Ior`-bearing surface).
  *   - `naive*` — decode the whole `Order`, rebuild via `copy`, re-encode (what you'd write without
  *     optics).
  *   - `native*` — circe's own `ACursor` navigation, no EO. The honest "use the library directly"
  *     comparator.
  *   - `monocle*` — decode to the case class, run a Monocle optic, re-encode. A general optics
  *     library that has *no* AST carrier, so it must pay the full decode/encode round-trip — this
  *     is where EO's structural-edit advantage shows.
  *
  * Two foci:
  *   - **depth-3 scalar** `customer.address.street` — touched once. The `@Param size` makes the
  *     surrounding document grow, so the naive / monocle decode cost climbs while the cursor walk
  *     stays flat.
  *   - **array traversal** `lines[*].name` — uppercases every line name across an N-element array.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class OrderCirceBench extends JmhDefaults:

  import OrderCirceBench.{*, given}

  @Param(Array("8", "64", "512"))
  var size: Int = uninitialized

  var order: Order = uninitialized
  var json: Json = uninitialized

  @Setup(Level.Iteration)
  def init(): Unit =
    order = Domain.mkOrder(size)
    json = order.asJson

  // ---- depth-3 scalar: customer.address.street ----------------------

  @Benchmark def eoStreet: Json =
    streetPrism.modifyUnsafe(_.toUpperCase)(json)

  @Benchmark def eoStreetIor: Ior[Chain[JsonFailure], Json] =
    streetPrism.modify(_.toUpperCase)(json)

  @Benchmark def naiveStreet: Json =
    json
      .as[Order]
      .map(o =>
        o.copy(customer =
          o.customer
            .copy(address = o.customer.address.copy(street = o.customer.address.street.toUpperCase))
        )
      )
      .toOption
      .get
      .asJson

  @Benchmark def nativeStreet: Json =
    json
      .hcursor
      .downField("customer")
      .downField("address")
      .downField("street")
      .withFocus(_.mapString(_.toUpperCase))
      .top
      .get

  @Benchmark def monocleStreet: Json =
    val o = json.as[Order].toOption.get
    DomainMonocle.street.modify(_.toUpperCase)(o).asJson

  // ---- array traversal: lines[*].name -------------------------------

  @Benchmark def eoNames: Json =
    namesTraversal(json)

  @Benchmark def eoNamesIor: Ior[Chain[JsonFailure], Json] =
    namesTraversalIor(json)

  @Benchmark def naiveNames: Json =
    json
      .as[Order]
      .map(o => o.copy(lines = o.lines.map(li => li.copy(name = li.name.toUpperCase))))
      .toOption
      .get
      .asJson

  @Benchmark def nativeNames: Json =
    json
      .hcursor
      .downField("lines")
      .withFocus(
        _.mapArray(
          _.map(
            _.mapObject(obj =>
              obj("name").flatMap(_.asString).fold(obj) { s =>
                obj.add("name", Json.fromString(s.toUpperCase))
              }
            )
          )
        )
      )
      .top
      .get

  @Benchmark def monocleNames: Json =
    val o = json.as[Order].toOption.get
    DomainMonocle.names.modify(_.toUpperCase)(o).asJson

object OrderCirceBench:

  // ---- circe codecs for the canonical schema ------------------------

  given Codec.AsObject[Address] = KindlingsCodecAsObject.derive
  given Codec.AsObject[Customer] = KindlingsCodecAsObject.derive
  given Codec.AsObject[LineItem] = KindlingsCodecAsObject.derive
  given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

  // ---- EO circe optics ----------------------------------------------

  val streetPrism: JsonPrism[String] =
    codecPrism[Order].field(_.customer).field(_.address).field(_.street)

  // `lines[*].name` traversal — same proven selectDynamic chain as the
  // legacy Basket bench. Pre-built into the `Json => …` function so the
  // bench loop measures only the rewrite.
  val namesTraversal: Json => Json =
    codecPrism[Order].lines.each.name.modifyUnsafe(_.toUpperCase)

  val namesTraversalIor: Json => Ior[Chain[JsonFailure], Json] =
    codecPrism[Order].lines.each.name.modify(_.toUpperCase)

  // Monocle peers live on the shared `DomainMonocle` fixture.
