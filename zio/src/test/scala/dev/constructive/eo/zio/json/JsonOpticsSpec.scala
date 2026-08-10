package dev.constructive.eo
package zio
package json

import _root_.zio.Chunk
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

  "duplicate object keys (Json.Obj is Chunk-backed, so they are representable)" should {
    val dup = Json.Obj("k" -> Json.Num(1), "k" -> Json.Num(2))

    // zio-json's own `Json.Obj.equals` maps the LEFT side before comparing every right entry, so a
    // duplicate-key object is not equal even to ITSELF (upstream reflexivity wart). Compare the
    // underlying `fields` chunk instead — which is also the stricter assertion.
    def fieldsOf(j: Json): Chunk[(String, Json)] = obj.getOption(j).getOrElse(Chunk.empty)

    "read the first match" >> (field("k").getOption(dup) === Some(Json.Num(1)))
    "write ONLY the first match — the sibling survives" >> {
      fieldsOf(field("k").replace(Json.Num(9))(dup)) ===
        Chunk("k" -> Json.Num(9), "k" -> Json.Num(2))
    }
    "satisfy write-back-what-you-read (the law the write-all version broke)" >> {
      (fieldsOf(field("k").replace(field("k").getOption(dup).get)(dup)) === fieldsOf(dup))
        .and(fieldsOf(field("k").modify(identity)(dup)) === fieldsOf(dup))
    }
    "not clobber a duplicate sibling through a nested cursor write" >> {
      val nested = Json.Obj("a" -> Json.Obj("x" -> Json.Num(1)), "a" -> Json.Str("s"))
      val cur = JsonCursor.field("a").isObject.field("x")
      fieldsOf(cur.optional.replace(Json.Num(2))(nested)) ===
        Chunk("a" -> Json.Obj("x" -> Json.Num(2)), "a" -> Json.Str("s"))
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
    "write with a type filter as the cursor TERMINAL" >> {
      val terminal = JsonCursor.field("tags").isArray
      terminal.optional.replace(Json.Arr(Json.Str("z")))(doc) === Json.Obj(
        "name" -> Json.Str("ada"),
        "tags" -> Json.Arr(Json.Str("z")),
        "age" -> Json.Num(41),
      )
    }
    "pass writes through when the TERMINAL filter fails" >> {
      JsonCursor.field("name").isArray.optional.replace(Json.Arr(Json.Str("z")))(doc) === doc
    }
    "read and write a multi-level object spine" >> {
      val nested =
        Json.Obj("a" -> Json.Obj("b" -> Json.Obj("c" -> Json.Num(1))), "keep" -> Json.Num(0))
      val deep = JsonCursor.field("a").isObject.field("b").isObject.field("c")
      (deep.optional.getOption(nested) === Some(Json.Num(1)))
        .and(
          deep.optional.replace(Json.Num(2))(nested) === Json.Obj(
            "a" -> Json.Obj("b" -> Json.Obj("c" -> Json.Num(2))),
            "keep" -> Json.Num(0),
          )
        )
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
      // Assert the whole decoded value, not a substring: a corrupted sku would pass `contains`.
      (p.getOption(p.modify(i => i.copy(price = i.price * 2))(s)) === Some(ItemJ("s-1", 19.0)))
        .and(p.getOption("not json") === None)
        .and(p.replace(ItemJ("x", 1.0))("not json") === "not json")
    }
  }
