package dev.constructive.eo

import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.Plated
import io.circe.{Decoder, Encoder, Json, JsonObject}

/** Cross-representation optics bridging native Scala types and their circe-serialised form.
  *
  * The entry point is [[JsonPrism.apply]] (aliased as `codecPrism` for the read-aloud API):
  *
  * {{{
  *   val personPrism: JsonPrism[Person] = codecPrism[Person]
  *   val streetPrism: JsonPrism[String] =
  *     personPrism.field(_.address).field(_.street)
  *   streetPrism.modify(_.toUpperCase)(personJson)
  *   // → Ior.Right of the same Json, with .address.street upper-cased —
  *   //   no Person ever materialised.
  * }}}
  *
  * From a root prism the whole surface lights up: `.field` (and its Dynamic sugar) drills deeper,
  * `.at(i)` indexes into an array, `.each` widens to a [[JsonTraversal]] over every element,
  * `.fields(_.a, _.b)` focuses a bundle of case fields as a Scala 3 NamedTuple. Independent of the
  * path-walkers, [[platedJson]] makes `Json` a recursive self-traversal for whole-document
  * rewrites.
  *
  * Every operation comes in two tiers: the default methods return `Ior[Chain[JsonFailure], _]` —
  * `Ior.Both` is partial success, every skip documented in the chain — and the `*Unsafe` siblings
  * are the silent pass-through hot path.
  *
  * Coming from circe-optics `JsonPath` or raw cursors: this is the compile-time-checked equivalent
  * — `codecPrism[Person].address.street` ≈ `root.address.street.string` ≈ an `hcursor.downField`
  * chain, with typed per-step codecs instead of stringly paths.
  */
package object circe:

  /** Root-level Prism from Json to a native type `S`. Alias for [[JsonPrism.apply]] that reads more
    * naturally when composed with `.field`.
    */
  def codecPrism[S: Encoder: Decoder]: JsonPrism[S] = JsonPrism[S]

  /** A [[dev.constructive.eo.optics.Plated]] over the JSON tree itself — the immediate children of
    * a node are an array's elements or an object's field values (a primitive has none). With it,
    * `Plated.transform` / `rewrite` / `universe` walk a whole `Json` document recursively: redact
    * every field at any depth, rewrite every string, round every number, rename keys throughout.
    * Pure and total — rebuilding an array/object from new children needs no decode — and stack-safe
    * via the `cats.Eval` trampoline in the combinators.
    *
    * {{{
    *   import dev.constructive.eo.circe.given
    *   import dev.constructive.eo.optics.Plated
    *   // Uppercase every string anywhere in the document:
    *   Plated.transform[Json](j => j.asString.fold(j)(s => Json.fromString(s.toUpperCase)))(doc)
    * }}}
    */
  given platedJson: Plated[Json] =
    Plated.fromChildrenVec(immediateChildJson, rebuildJsonFromChildren)

  private val emptyJsonVec: PSVec[Json] = PSVec.empty[Json]

  private def immediateChildJson(json: Json): PSVec[Json] =
    json.fold(
      jsonNull = emptyJsonVec,
      jsonBoolean = _ => emptyJsonVec,
      jsonNumber = _ => emptyJsonVec,
      jsonString = _ => emptyJsonVec,
      jsonArray = PSVec.fromIterable,
      jsonObject = obj => PSVec.fromIterable(obj.values),
    )

  private def rebuildJsonFromChildren(json: Json, vec: PSVec[Json]): Json =
    json.fold(
      jsonNull = json,
      jsonBoolean = _ => json,
      jsonNumber = _ => json,
      jsonString = _ => json,
      jsonArray = _ => Json.fromValues(vec.toList),
      jsonObject =
        obj => Json.fromJsonObject(JsonObject.fromIterable(obj.keys.zip(vec.toList).toSeq)),
    )
