package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.circe.JsonFailure

import cats.data.{Chain, Ior}

import io.circe.Json
import io.circe.syntax.*

/** Canonical-schema circe bench (plan 009, Phase 1 proof-of-concept).
  *
  * One realistic [[Order]] document, two foci, four baselines side-by-side per metric so a single
  * JMH report row tells the whole story:
  *
  *   - `eo*` / `eoIor*` — the EO `JsonPrism` / `JsonTraversal` (silent `*Unsafe` hot path and the
  *     default `Ior`-bearing surface).
  *   - `naive*` — decode the whole `Order`, rebuild via `copy`, re-encode (what you'd write without
  *     optics).
  *   - `hcursor*` — circe's `HCursor` navigation. `.top` replays the zipper to rebuild the root, so
  *     this is often *slower* than a full decode — a cautionary datapoint, not an optimum.
  *   - `direct*` — direct `JsonObject` surgery (rebuild only the touched parents, no cursor). The
  *     hand-written shape of what `JsonPrism` does, so it lands near `eo*`.
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

  import DomainCirce.{namesTraversal, namesTraversalIor, streetPrism, given}

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

  // hcursor walk: `.top` replays the zipper from the focus back to the root
  // to rebuild — the surprising "slower than a full decode" cost.
  @Benchmark def hcursorStreet: Json =
    json
      .hcursor
      .downField("customer")
      .downField("address")
      .downField("street")
      .withFocus(_.mapString(_.toUpperCase))
      .top
      .get

  // Direct AST surgery: destructure the JsonObjects on the path and rebuild
  // only the touched parents — no cursor, no `.top` rewalk. The hand-written
  // shape of what `JsonPrism` does internally.
  @Benchmark def directStreet: Json =
    (for
      obj <- json.asObject
      cust <- obj("customer").flatMap(_.asObject)
      addr <- cust("address").flatMap(_.asObject)
      street <- addr("street").flatMap(_.asString)
    yield
      val addr2 = addr.add("street", Json.fromString(street.toUpperCase))
      val cust2 = cust.add("address", Json.fromJsonObject(addr2))
      Json.fromJsonObject(obj.add("customer", Json.fromJsonObject(cust2)))
    ).getOrElse(json)

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

  @Benchmark def hcursorNames: Json =
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

  // Direct AST surgery: rebuild the `lines` array in place, no cursor.
  @Benchmark def directNames: Json =
    (for
      obj <- json.asObject
      arr <- obj("lines").flatMap(_.asArray)
    yield
      val arr2 = arr.map(
        _.mapObject(o =>
          o("name").flatMap(_.asString).fold(o)(s => o.add("name", Json.fromString(s.toUpperCase)))
        )
      )
      Json.fromJsonObject(obj.add("lines", Json.fromValues(arr2)))
    ).getOrElse(json)

  @Benchmark def monocleNames: Json =
    val o = json.as[Order].toOption.get
    DomainMonocle.names.modify(_.toUpperCase)(o).asJson
