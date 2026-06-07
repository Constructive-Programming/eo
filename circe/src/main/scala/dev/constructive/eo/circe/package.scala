package dev.constructive.eo

import io.circe.{Decoder, Encoder, Json, JsonObject}

import dev.constructive.eo.optics.Plated

/** Cross-representation optics bridging native Scala types and their circe-serialised form.
  *
  * The entry point is [[JsonPrism.apply]] (aliased as `codecPrism` for the read-aloud API):
  *
  * {{{
  *   val personPrism: JsonPrism[Person] = codecPrism[Person]
  *   val streetPrism: JsonPrism[String] =
  *     personPrism.field(_.address).field(_.street)
  *   streetPrism.modify(_.toUpperCase)(personJson)
  *   // → the same Json, with .address.street upper-cased — no Person
  *   //   ever materialised.
  * }}}
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
    Plated.fromChildren(immediateChildJson, rebuildJsonFromChildren)

  private def immediateChildJson(json: Json): List[Json] =
    json.fold(
      jsonNull = Nil,
      jsonBoolean = _ => Nil,
      jsonNumber = _ => Nil,
      jsonString = _ => Nil,
      jsonArray = _.toList,
      jsonObject = _.values.toList,
    )

  private def rebuildJsonFromChildren(json: Json, cs: List[Json]): Json =
    json.fold(
      jsonNull = json,
      jsonBoolean = _ => json,
      jsonNumber = _ => json,
      jsonString = _ => json,
      jsonArray = _ => Json.fromValues(cs),
      jsonObject = obj => Json.fromJsonObject(JsonObject.fromIterable(obj.keys.zip(cs).toSeq)),
    )
