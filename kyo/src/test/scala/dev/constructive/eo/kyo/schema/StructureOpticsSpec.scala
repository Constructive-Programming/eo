package dev.constructive.eo
package kyo
package schema

import _root_.kyo.*
import _root_.kyo.Structure.Value
import org.specs2.mutable.Specification

import optics.Plated
import StructureValues.{*, given}

// Fixtures PersonS / ShapeS / ItemS / OrderS are shared with SchemaOpticsSpec
// (top-level package-private in this package).

class StructureOpticsSpec extends Specification:

  val person = PersonS("Alice", 30)
  val personV = Structure.encode(person)
  val order = OrderS("ord-1", Vector(ItemS("apple", 1.0), ItemS("pear", 2.0)))
  val orderV = Structure.encode(order)

  "constructor prisms" should {
    "str round-trips and misses" >> {
      (str.getOption(Value.Str("hi")) === Some("hi"))
        .and(str.reverseGet("hi") === Value.Str("hi"))
        .and(str.getOption(Value.Bool(true)) === None)
    }
    "integer does not coerce decimal" >> {
      (integer.getOption(Value.Integer(1L)) === Some(1L))
        .and(integer.getOption(Value.Decimal(1.0)) === None)
    }
    "nul focuses Unit" >> {
      (nul.getOption(Value.Null) === Some(()))
        .and(nul.reverseGet(()) === Value.Null)
        .and(nul.getOption(Value.Str("")) === None)
    }
    "sequence exposes the element chunk" >> {
      val v = Value.Sequence(Chunk(Value.Integer(1L), Value.Integer(2L)))
      (sequence.getOption(v) === Some(Chunk(Value.Integer(1L), Value.Integer(2L))))
        .and(sequence.getOption(Value.Null) === None)
    }
    "record exposes the ordered fields" >> {
      record.getOption(personV).map(_.map(_._1)) === Some(Chunk("name", "age"))
    }
  }

  "field" should {
    "get a field of an encoded case class" >> (field("name").getOption(personV) === Some(
      Value.Str("Alice")
    ))
    "modify through a drilled prism, preserving siblings and order" >> {
      val nameS = field("name").andThen(str)
      val out = nameS.modify(_.toUpperCase)(personV)
      (nameS.getOption(out) === Some("ALICE"))
        .and(field("age").getOption(out) === Some(Value.Integer(30L)))
        .and(record.getOption(out).map(_.map(_._1)) === Some(Chunk("name", "age")))
    }
    "miss on an absent field passes writes through" >> (field("nope")
      .replace(Value.Null)(personV) === personV)
    "miss on a non-record passes writes through" >> (field("name")
      .replace(Value.Null)(Value.Str("x")) === Value.Str("x"))
  }

  "field / key on duplicate names (Record and MapEntries are Chunk-backed)" should {
    val dupRec = Value.Record(Chunk("k" -> Value.Integer(1L), "k" -> Value.Integer(2L)))
    val dupMap = Value.MapEntries(
      Chunk(Value.Str("k") -> Value.Integer(1L), Value.Str("k") -> Value.Integer(2L))
    )

    "read the first match and write ONLY it" >> {
      (field("k").getOption(dupRec) === Some(Value.Integer(1L)))
        .and(
          field("k").replace(Value.Integer(9L))(dupRec) === Value.Record(
            Chunk("k" -> Value.Integer(9L), "k" -> Value.Integer(2L))
          )
        )
    }
    "satisfy write-back-what-you-read (the law write-all broke)" >> {
      (field("k").modify(identity)(dupRec) === dupRec)
        .and(key(Value.Str("k")).modify(identity)(dupMap) === dupMap)
    }
    "key: first match only, sibling entry surviving" >> {
      key(Value.Str("k")).replace(Value.Integer(9L))(dupMap) === Value.MapEntries(
        Chunk(Value.Str("k") -> Value.Integer(9L), Value.Str("k") -> Value.Integer(2L))
      )
    }
  }

  "atField (create / update / delete)" should {
    "insert an absent field, where `field` writes are a no-op" >> {
      val out = atField("extra").replace(Some(Value.Integer(9L)))(personV)
      (field("extra").getOption(out) === Some(Value.Integer(9L)))
        .and(record.getOption(out).map(_.map(_._1)) === Some(Chunk("name", "age", "extra")))
        .and(field("extra").replace(Value.Integer(9L))(personV) === personV)
    }
    "update in place, keeping field order" >> {
      val out = atField("name").replace(Some(Value.Str("Bob")))(personV)
      (field("name").getOption(out) === Some(Value.Str("Bob")))
        .and(record.getOption(out).map(_.map(_._1)) === Some(Chunk("name", "age")))
    }
    "delete with None, removing every duplicate" >> {
      val dup = Value.Record(Chunk("k" -> Value.Integer(1L), "k" -> Value.Integer(2L)))
      (record.getOption(atField("name").replace(None)(personV)).map(_.map(_._1)) ===
        Some(Chunk("age")))
        .and(atField("k").replace(None)(dup) === Value.Record(Chunk.empty))
    }
    "read Some(None) for a record lacking the field, miss on a non-record" >> {
      (atField("nope").getOption(personV) === Some(None))
        .and(atField("x").getOption(Value.Str("s")) === None)
        .and(atField("x").replace(Some(Value.Null))(Value.Str("s")) === Value.Str("s"))
    }
    // The documented limit: put-put holds only up to field ORDER, because a delete destroys the
    // position a later insert would have to restore. Pinned so the semantics are a decision.
    "delete-then-insert APPENDS (put-put holds only up to order)" >> {
      val deleted = atField("name").replace(None)(personV)
      val reinserted = atField("name").replace(Some(Value.Str("Alice")))(deleted)
      val direct = atField("name").replace(Some(Value.Str("Alice")))(personV)
      (record.getOption(reinserted).map(_.map(_._1)) === Some(Chunk("age", "name")))
        .and(record.getOption(direct).map(_.map(_._1)) === Some(Chunk("name", "age")))
        .and(reinserted !== direct)
    }
  }

  "at" should {
    val seq = Value.Sequence(Chunk(Value.Str("a"), Value.Str("b")))
    "get by index" >> (at(1).getOption(seq) === Some(Value.Str("b")))
    "replace by index, preserving siblings" >> (at(1).replace(Value.Str("B"))(seq) ===
      Value.Sequence(Chunk(Value.Str("a"), Value.Str("B"))))
    "miss out of range passes writes through" >> (at(5).replace(Value.Null)(seq) === seq)
    "miss on a non-sequence passes writes through" >> (at(0).replace(Value.Null)(personV) ===
      personV)
  }

  "key" should {
    val m = Value.MapEntries(
      Chunk(Value.Str("a") -> Value.Integer(1L), Value.Str("b") -> Value.Integer(2L))
    )
    "get by structural key" >> (key(Value.Str("b")).getOption(m) === Some(Value.Integer(2L)))
    "replace the matching entry's value only" >> {
      key(Value.Str("a")).replace(Value.Integer(9L))(m) === Value.MapEntries(
        Chunk(Value.Str("a") -> Value.Integer(9L), Value.Str("b") -> Value.Integer(2L))
      )
    }
    "miss on an absent key passes writes through" >> (key(Value.Str("z"))
      .replace(Value.Null)(m) === m)
  }

  "each" should {
    val seq = Value.Sequence(Chunk(Value.Integer(1L), Value.Integer(2L), Value.Integer(3L)))
    "modify every element" >> {
      each.andThen(integer).modify(_ * 10)(seq) ===
        Value.Sequence(Chunk(Value.Integer(10L), Value.Integer(20L), Value.Integer(30L)))
    }
    "fold across elements" >> (each.andThen(integer).foldMap(identity)(seq) === 6L)
    "zero foci on a non-sequence: writes pass through" >> (each
      .replace(Value.Null)(personV) === personV)
  }

  "variant" should {
    val circleV = Structure.encode[ShapeS](ShapeS.Circle(2.5))
    val squareV = Structure.encode[ShapeS](ShapeS.Square(4.0))
    "navigate into the active variant's payload" >> {
      val radius = variant("Circle").andThen(field("radius")).andThen(decimal)
      radius.getOption(circleV) === Some(2.5)
    }
    "modify through the variant and decode back" >> {
      val radius = variant("Circle").andThen(field("radius")).andThen(decimal)
      Structure.decode[ShapeS](radius.modify(_ * 2)(circleV)).foldError(Some(_), _ => None) ===
        Some(ShapeS.Circle(5.0))
    }
    "miss on the other variant passes writes through" >> (variant("Circle")
      .replace(Value.Null)(squareV) === squareV)
    "also match the declared VariantCase spelling" >> {
      val vc = Value.VariantCase("Circle", Value.Decimal(2.5))
      (variant("Circle").getOption(vc) === Some(Value.Decimal(2.5)))
        .and(
          variant("Circle").replace(Value.Decimal(5.0))(vc) ===
            Value.VariantCase("Circle", Value.Decimal(5.0))
        )
        .and(variant("Square").replace(Value.Null)(vc) === vc)
    }
  }

  "platedValue" should {
    "transform rewrites every string at any depth" >> {
      val out = Plated.transform[Value] {
        case Value.Str(s) => Value.Str(s.toUpperCase)
        case other        => other
      }(orderV)
      Structure.decode[OrderS](out).foldError(Some(_), _ => None) ===
        Some(OrderS("ORD-1", Vector(ItemS("APPLE", 1.0), ItemS("PEAR", 2.0))))
    }
    "universe enumerates every sub-value" >> {
      Plated.universe[Value](personV).count {
        case Value.Str(_) => true
        case _            => false
      } === 1
    }
  }

  "Schema.valuePrism (typed ↔ untyped)" should {
    val personP = Schema[PersonS].valuePrism
    "reverseGet then get is identity" >> (personP.getOption(personP.reverseGet(person)) === Some(
      person
    ))
    "reverseGet agrees with Structure.encode" >> (personP.reverseGet(person) === personV)
    "miss on a shape-mismatched value passes writes through" >> {
      val notAPerson = Value.Str("nope")
      (personP.getOption(notAPerson) === None)
        .and(personP.modify(identity)(notAPerson) === notAPerson)
    }
    "decode only the leaf you touch: untyped spine, typed leaves" >> {
      val itemPrices = field("items").andThen(each).andThen(Schema[ItemS].valuePrism)
      val out = itemPrices.modify(i => i.copy(price = i.price + 0.5))(orderV)
      Structure.decode[OrderS](out).foldError(Some(_), _ => None) ===
        Some(OrderS("ord-1", Vector(ItemS("apple", 1.5), ItemS("pear", 2.5))))
    }
  }

  "wire-face composition (Schema[Value] is public in RC6)" should {
    "edit one field inside encoded bytes with no typed value materialised" >> {
      val personP = Schema[PersonS].stringPrism[Json]
      val ageInJson =
        summon[Schema[Value]].stringPrism[Json].andThen(field("age")).andThen(integer)
      val bumped = ageInJson.modify(_ + 12)(personP.reverseGet(person))
      personP.getOption(bumped) === Some(PersonS("Alice", 42))
    }
  }
