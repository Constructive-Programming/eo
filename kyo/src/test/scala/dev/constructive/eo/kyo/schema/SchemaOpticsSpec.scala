package dev.constructive.eo
package kyo
package schema

import _root_.kyo.*
import org.specs2.mutable.Specification

// Fixtures at top level (same rule as RecordOpticsSpec: kyo/specs2 DSL collisions,
// and schema derivation prefers stable top-level ADTs).
private case class PersonS(name: String, age: Int) derives Schema

private enum ShapeS derives Schema:
  case Circle(radius: Double)
  case Square(side: Double)

private case class ItemS(name: String, price: Double) derives Schema
private case class OrderS(id: String, items: Vector[ItemS]) derives Schema

class SchemaOpticsSpec extends Specification:

  val person = PersonS("Alice", 30)
  val schema = Schema[PersonS]

  val nameL = schema.focus(_.name).lens
  val ageL = schema.focus(_.age).lens

  "Focus.lens bridge (Id mode)" should {
    "get the field" >> (nameL.get(person) === "Alice")
    "put-get" >> (nameL.get(nameL.replace("Bob")(person)) === "Bob")
    "get-put observes identically" >> (nameL.replace(nameL.get(person))(person) === person)
    "put-put" >> (nameL.get(nameL.replace("B")(nameL.replace("A")(person))) === "B")
    "leave sibling fields untouched" >> (nameL.replace("Bob")(person).age === 30)
    "serve capability-consuming code" >> {
      def bump[S](s: S)(using m: CanModify[S, Int]): S = m.modify(_ + 12)(s)
      bump(person)(using ageL).age === 42
    }
  }

  "Focus.optional bridge (Maybe mode)" should {
    val radiusO = Schema[ShapeS].focus(_.Circle.radius).toOptional
    val circle: ShapeS = ShapeS.Circle(2.5)
    val square: ShapeS = ShapeS.Square(4.0)
    "get on the matching variant" >> (radiusO.getOption(circle) === Some(2.5))
    "miss on the other variant" >> (radiusO.getOption(square) === None)
    "modify the matching variant" >> (radiusO.modify(_ * 2)(circle) === ShapeS.Circle(5.0))
    "pass writes through untouched on a miss" >> (radiusO.replace(9.9)(square) === square)
  }

  "Focus.traversal bridge (Chunk mode)" should {
    val itemsT = Schema[OrderS].foreach(_.items).traversal
    val order = OrderS("ord-1", Vector(ItemS("apple", 1.0), ItemS("pear", 2.0)))
    "modify every element" >> {
      itemsT.modify(i => i.copy(price = i.price * 2))(order) ===
        OrderS("ord-1", Vector(ItemS("apple", 2.0), ItemS("pear", 4.0)))
    }
    "leave sibling fields untouched" >> (itemsT.replace(ItemS("x", 0.0))(order).id === "ord-1")
    "no-op on an empty collection" >> {
      val empty = OrderS("ord-2", Vector.empty)
      itemsT.modify(i => i.copy(price = 9.9))(empty) === empty
    }
    "fold across the elements" >> (itemsT.foldMap(_.price)(order) === 3.0)
    "drill into each element's field through andThen" >> {
      val priceInOrder = itemsT.andThen(Schema[ItemS].focus(_.price).lens)
      (priceInOrder.foldMap(identity)(order) === 3.0)
        .and(priceInOrder.modify(_ + 0.5)(order).items.map(_.price) === Vector(1.5, 2.5))
    }
  }

  "Schema.stringPrism (json face)" should {
    val personP = schema.stringPrism[Json]
    "reverseGet then get is identity" >> (personP.getOption(personP.reverseGet(person)) === Some(
      person
    ))
    "get a valid payload" >> (personP.getOption("""{"name":"Alice","age":30}""") === Some(person))
    "miss on malformed input" >> (personP.getOption("not json") === None)
    "miss on schema-mismatched input" >> (personP.getOption("""{"radius":2.5}""") === None)
    "prefix-decode: trailing garbage is ignored, and rewrites drop it" >> {
      val in = """{"name":"Alice","age":30} extra"""
      (personP.getOption(in) === Some(person))
        .and(personP.modify(identity)(in) === personP.reverseGet(person))
    }
    "pass writes through untouched on a miss" >> (personP.modify(identity)(
      "not json"
    ) === "not json")
    "modify a field inside the encoded payload" >> {
      val ageInJson = personP.andThen(ageL)
      ageInJson.modify(_ + 1)("""{"name":"Alice","age":30}""") === personP.reverseGet(
        PersonS("Alice", 31)
      )
    }
  }

  "Schema.prism (byte face)" should {
    val personBP = schema.prism[Json]
    val garbage = Span.fromUnsafe("not json".getBytes(java.nio.charset.StandardCharsets.UTF_8))
    "reverseGet then get is identity" >> (personBP.getOption(personBP.reverseGet(person)) === Some(
      person
    ))
    "miss on malformed bytes" >> (personBP.getOption(garbage) === None)
    "pass writes through untouched on a miss" >> (personBP.modify(identity)(garbage) === garbage)
    "miss on bytes of a different schema" >> {
      val shapeBytes = Schema[ShapeS].prism[Json].reverseGet(ShapeS.Circle(2.5))
      personBP.getOption(shapeBytes) === None
    }
    "modify a field inside the encoded bytes" >> {
      val bumped = personBP.andThen(ageL).modify(_ + 1)(personBP.reverseGet(person))
      personBP.getOption(bumped) === Some(PersonS("Alice", 31))
    }
  }
