package dev.constructive.eo
package zio
package json

import _root_.zio.json.ast.{Json, JsonCursor}
import _root_.zio.json.{DeriveJsonCodec, JsonCodec}
import org.specs2.mutable.Specification

import optics.Plated

case class ItemJ(sku: String, price: Double)

object ItemJ:
  given codec: JsonCodec[ItemJ] = DeriveJsonCodec.gen[ItemJ]

class JsonOpticsSpec extends Specification:

  import JsonValues.{*, given}

  val doc: Json = Json.Obj(
    "name" -> Json.Str("ada"),
    "tags" -> Json.Arr(Json.Str("a"), Json.Str("b")),
    "age" -> Json.Num(41),
  )

  "constructor prisms" should {
    "hit their case and miss the others" >> {
      (str.getOption(Json.Str("x")) === Some("x"))
        .and(str.getOption(Json.Num(1)) === None)
        .and(bool.reverseGet(true) === Json.Bool(true))
        .and(nul.getOption(Json.Null) === Some(()))
    }
  }

  "field / at / each" should {
    "read and rewrite a field, siblings surviving" >> {
      val nameO = field("name").andThen(str)
      (nameO.getOption(doc) === Some("ada"))
        .and(
          field("name").replace(Json.Str("gr"))(doc) === Json.Obj(
            "name" -> Json.Str("gr"),
            "tags" -> Json.Arr(Json.Str("a"), Json.Str("b")),
            "age" -> Json.Num(41),
          )
        )
        .and(field("nope").replace(Json.Null)(doc) === doc)
    }
    "index into arrays and traverse them" >> {
      val tag1 = field("tags").andThen(at(1)).andThen(str)
      (tag1.getOption(doc) === Some("b"))
        .and(
          field("tags").andThen(each).andThen(str).modify(_.toUpperCase)(doc) === Json.Obj(
            "name" -> Json.Str("ada"),
            "tags" -> Json.Arr(Json.Str("A"), Json.Str("B")),
            "age" -> Json.Num(41),
          )
        )
    }
  }

  "Plated" should {
    "rewrite every string at any depth" >> {
      val out = Plated.transform[Json](j => str.modify(_.toUpperCase)(j))(doc)
      field("name").andThen(str).getOption(out) === Some("ADA")
    }
  }

  "JsonCursor bridge" should {
    val cursor = JsonCursor.field("tags").isArray.element(1)

    "read through the cursor" >> {
      cursor.optional.getOption(doc) === Some(Json.Str("b"))
    }
    "rewrite through the cursor, siblings surviving" >> {
      cursor.optional.replace(Json.Str("B"))(doc) === Json.Obj(
        "name" -> Json.Str("ada"),
        "tags" -> Json.Arr(Json.Str("a"), Json.Str("B")),
        "age" -> Json.Num(41),
      )
    }
    "pass writes through on a cursor miss" >> {
      val miss = JsonCursor.field("tags").isArray.element(9)
      (miss.optional.replace(Json.Null)(doc) === doc)
        .and(JsonCursor.field("name").isArray.optional.getOption(doc) === None)
    }
  }

  "wire faces" should {
    "text: String ↔ Json on-ramp into the kit" >> {
      val raw = """{"name":"ada","tags":["a","b"],"age":41}"""
      val bumped = text.andThen(field("name")).andThen(str).modify(_.toUpperCase)(raw)
      text.andThen(field("name")).andThen(str).getOption(bumped) === Some("ADA")
    }
    "stringPrism: typed codec face, misses passing through" >> {
      val p = ItemJ.codec.stringPrism
      val s = """{"sku":"s-1","price":9.5}"""
      (p.modify(i => i.copy(price = i.price * 2)).apply(s).contains("19.0") === true)
        .and(p.getOption("not json") === None)
        .and(p.replace(ItemJ("x", 1.0))("not json") === "not json")
    }
  }
