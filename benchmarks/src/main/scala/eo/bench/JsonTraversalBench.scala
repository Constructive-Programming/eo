package eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import eo.circe.{JsonPrism, codecPrism}

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** JsonTraversal vs. naive decode-modify-encode across an array.
  *
  * The story: uppercasing every `name` across an N-element array
  * of records nested inside an outer object. The cursor-backed
  * traversal touches only:
  *   1. the outer object once (prefix walk),
  *   2. each array element's `name` field,
  *   3. a single array rebuild.
  *
  * The naive comparator decodes the entire `Basket` (array and
  * all), maps `copy(name = ...)` over the whole array, re-encodes.
  *
  * Three sizes — 8, 64, 512 elements — to show how the gap grows
  * with the element count.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class JsonTraversalBench:

  import JsonTraversalBench.*

  private val basket8:   Basket = mkBasket(8)
  private val basket64:  Basket = mkBasket(64)
  private val basket512: Basket = mkBasket(512)

  private val json8:   Json = basket8.asJson
  private val json64:  Json = basket64.asJson
  private val json512: Json = basket512.asJson

  // Construct the traversal once, outside the bench loop.
  // `Any`-typed because the transparent inline widens at the
  // definition site but `.modify` still resolves to the right
  // overload at the call site.
  private val nameEach8:   Json => Json =
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)
  private val nameEach64:  Json => Json =
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)
  private val nameEach512: Json => Json =
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)

  // ---- Size 8 -------------------------------------------------------

  @Benchmark def eoTraverse_8: Json =
    nameEach8(json8)

  @Benchmark def naiveTraverse_8: Json =
    json8.as[Basket].map { b =>
      b.copy(items = b.items.map(i => i.copy(name = i.name.toUpperCase)))
    }.toOption.get.asJson

  // ---- Size 64 ------------------------------------------------------

  @Benchmark def eoTraverse_64: Json =
    nameEach64(json64)

  @Benchmark def naiveTraverse_64: Json =
    json64.as[Basket].map { b =>
      b.copy(items = b.items.map(i => i.copy(name = i.name.toUpperCase)))
    }.toOption.get.asJson

  // ---- Size 512 -----------------------------------------------------

  @Benchmark def eoTraverse_512: Json =
    nameEach512(json512)

  @Benchmark def naiveTraverse_512: Json =
    json512.as[Basket].map { b =>
      b.copy(items = b.items.map(i => i.copy(name = i.name.toUpperCase)))
    }.toOption.get.asJson

object JsonTraversalBench:

  // Four-field item so the naive path pays full decode cost per
  // element, not just a minimal single-field hit.
  final case class Item(
      name: String,
      sku:  String,
      qty:  Int,
      tags: Vector[String],
  )
  object Item:
    given Codec.AsObject[Item] = KindlingsCodecAsObject.derive

  final case class Basket(
      owner:    String,
      currency: String,
      region:   String,
      items:    Vector[Item],
  )
  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive

  def mkBasket(n: Int): Basket =
    val items = Vector.tabulate(n) { i =>
      Item(
        name = s"item-$i",
        sku  = s"sku-$i",
        qty  = i,
        tags = Vector("a", "b", "c"),
      )
    }
    Basket("Alice", "USD", "us-east-1", items)
