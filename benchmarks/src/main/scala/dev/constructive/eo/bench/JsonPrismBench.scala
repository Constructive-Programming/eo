package dev.constructive.eo
package bench


import org.openjdk.jmh.annotations.*

import dev.constructive.eo.circe.{JsonFailure, JsonPrism, codecPrism}

import io.circe.{Codec, Json}
import io.circe.syntax.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** JsonPrism vs. naive decode-modify-encode round-trip.
  *
  * The motivating story for eo-circe: modifying a single field of a JSON payload shouldn't pay to
  * decode / re-emit the whole record. `JsonPrism[A].modify` navigates via `ACursor`, writes at the
  * focus with `set`, and reassembles the root via `.top` — O(path depth) rather than O(all fields).
  *
  * Three depths benched here — depth 1, 2, 3 — with matching fixtures. Each benchmark toggles the
  * focused string to upper-case and back. Bench classes ship paired `eo*` / `naive*` methods so JMH
  * reports them side-by-side.
  */

class JsonPrismBench extends JmhDefaults:

  import JsonPrismBench.*

  // Pre-computed inputs — the benchmark measures the rewrite only,
  // not the initial encoding / Codec lookup.
  private val aliceJson: Json = Alice.asJson

  private val deep3Json: Json = Deep3(
    "root",
    Deep2("middle", Deep1("leaf", Atom("Alice"))),
  ).asJson

  // The prisms — constructed once, outside the bench loop.
  private val nameD1: JsonPrism[String] =
    codecPrism[Person].field(_.name)

  private val streetD2: JsonPrism[String] =
    codecPrism[Person].field(_.address).field(_.street)

  private val leafD3: JsonPrism[String] =
    codecPrism[Deep3].field(_.d2).field(_.d1).field(_.atom).field(_.value)

  // ---- Depth 1: Person → name ---------------------------------------
  //
  // Pre-v0.2 reference baseline: `modifyUnsafe` is byte-identical to
  // the pre-rename silent `modify`. The default Ior-bearing `modify`
  // benches are reported alongside as a new datapoint per OQ6.

  @Benchmark def eoModify_d1: Json =
    nameD1.modifyUnsafe(_.toUpperCase)(aliceJson)

  @Benchmark def eoModifyIor_d1: cats.data.Ior[cats.data.Chain[JsonFailure], Json] =
    nameD1.modify(_.toUpperCase)(aliceJson)

  @Benchmark def naiveModify_d1: Json =
    aliceJson.as[Person].map(p => p.copy(name = p.name.toUpperCase)).toOption.get.asJson

  // ---- Depth 2: Person → address → street ---------------------------

  @Benchmark def eoModify_d2: Json =
    streetD2.modifyUnsafe(_.toUpperCase)(aliceJson)

  @Benchmark def eoModifyIor_d2: cats.data.Ior[cats.data.Chain[JsonFailure], Json] =
    streetD2.modify(_.toUpperCase)(aliceJson)

  @Benchmark def naiveModify_d2: Json =
    aliceJson
      .as[Person]
      .map(p => p.copy(address = p.address.copy(street = p.address.street.toUpperCase)))
      .toOption
      .get
      .asJson

  // ---- Depth 3: Deep3 → d2 → d1 → atom → value ----------------------

  @Benchmark def eoModify_d3: Json =
    leafD3.modifyUnsafe(_.toUpperCase)(deep3Json)

  @Benchmark def eoModifyIor_d3: cats.data.Ior[cats.data.Chain[JsonFailure], Json] =
    leafD3.modify(_.toUpperCase)(deep3Json)

  @Benchmark def naiveModify_d3: Json =
    deep3Json
      .as[Deep3]
      .map(d =>
        d.copy(d2 =
          d.d2
            .copy(d1 =
              d.d2.d1.copy(atom = d.d2.d1.atom.copy(value = d.d2.d1.atom.value.toUpperCase))
            )
        )
      )
      .toOption
      .get
      .asJson

object JsonPrismBench:

  // ---- Depth-1 / depth-2 fixture (Person with nested Address) -------

  final case class Address(street: String, zip: Int)

  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derive

  final case class Person(name: String, age: Int, address: Address)

  object Person:
    given Codec.AsObject[Person] = KindlingsCodecAsObject.derive

  val Alice: Person = Person("Alice", 30, Address("Main St", 12345))

  // ---- Depth-3 fixture ----------------------------------------------

  final case class Atom(value: String)

  object Atom:
    given Codec.AsObject[Atom] = KindlingsCodecAsObject.derive

  final case class Deep1(label: String, atom: Atom)

  object Deep1:
    given Codec.AsObject[Deep1] = KindlingsCodecAsObject.derive

  final case class Deep2(label: String, d1: Deep1)

  object Deep2:
    given Codec.AsObject[Deep2] = KindlingsCodecAsObject.derive

  final case class Deep3(label: String, d2: Deep2)

  object Deep3:
    given Codec.AsObject[Deep3] = KindlingsCodecAsObject.derive
