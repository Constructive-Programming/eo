package dev.constructive.eo
package zio
package schema

import _root_.zio.schema.codec.JsonCodec
import _root_.zio.schema.{DeriveSchema, DynamicValue, Schema}
import org.specs2.mutable.Specification

import optics.Plated

case class PersonZ(name: String, age: Int)

object PersonZ:
  given schema: Schema[PersonZ] = DeriveSchema.gen[PersonZ]

sealed trait ShapeZ
case class CircleZ(r: Double) extends ShapeZ
case class SquareZ(s: Double) extends ShapeZ

object ShapeZ:
  given schema: Schema[ShapeZ] = DeriveSchema.gen[ShapeZ]

class SchemaOpticsSpec extends Specification:

  import DynamicValues.{*, given}

  val ada = PersonZ("ada", 41)
  val adaDvOuter: DynamicValue = PersonZ.schema.toDynamic(ada)

  "EoAccessorBuilder" should {
    val cc = PersonZ.schema.asInstanceOf[Schema.CaseClass2[String, Int, PersonZ]]
    val (nameL, ageL) = cc.makeAccessors(EoAccessorBuilder)

    "turn record fields into fused lenses" >> {
      (nameL.get(ada) === "ada")
        .and(ageL.modify(_ + 1)(ada) === PersonZ("ada", 42))
        .and(nameL.replace("gr")(ada).name === "gr")
    }
    "turn enum cases into prisms" >> {
      val e2 = ShapeZ.schema.asInstanceOf[Schema.Enum2[CircleZ, SquareZ, ShapeZ]]
      val (circleP, squareP) = e2.makeAccessors(EoAccessorBuilder)
      (circleP.getOption(CircleZ(2.5)) === Some(CircleZ(2.5)))
        .and(circleP.getOption(SquareZ(1.0)) === None)
        .and(squareP.modify(s => SquareZ(s.s * 2))(SquareZ(3.0): ShapeZ) === SquareZ(6.0))
        .and(circleP.reverseGet(CircleZ(2.0)) === (CircleZ(2.0): ShapeZ))
        // write on a MISS passes the whole sum through untouched (the prism's mend half)
        .and(squareP.modify(s => SquareZ(s.s * 2))(CircleZ(1.0): ShapeZ) === CircleZ(1.0))
    }
    "turn collections into traversals" >> {
      val seqSchema = Schema.list[Int].asInstanceOf[Schema.Sequence[List[Int], Int, ?]]
      val eachT = seqSchema.makeAccessors(EoAccessorBuilder)
      (eachT.modify(_ + 1)(List(1, 2, 3)) === List(2, 3, 4))
        .and(eachT.foldMap(identity)(List(1, 2, 3)) === 6)
    }
    "Set collections: lawful under an injective update, collapsing otherwise (documented caveat)" >> {
      val setSchema = Schema.set[Int].asInstanceOf[Schema.Set[Int]]
      val setT = setSchema.makeAccessors(EoAccessorBuilder)
      (setT.modify(_ + 1)(Set(1, 2, 3)) === Set(2, 3, 4))
        // `fromChunk` is `toSet`, so a colliding update drops entries — pinned, not fixed
        .and(setT.modify(_ / 2)(Set(2, 3)) === Set(1))
    }
    "Map collections: entries survive a key-preserving update" >> {
      val mapSchema = Schema.map[String, Int].asInstanceOf[Schema.Map[String, Int]]
      val mapT = mapSchema.makeAccessors(EoAccessorBuilder)
      mapT.modify((k, v) => (k, v * 2))(Map("a" -> 1, "b" -> 2)) === Map("a" -> 2, "b" -> 4)
    }
  }

  "DynamicValues navigation" should {
    val adaDv = PersonZ.schema.toDynamic(ada)

    "read and rewrite a record field through the primitive prism" >> {
      val ageO = field("age").andThen(int)
      (ageO.getOption(adaDv) === Some(41))
        .and(
          ageO.modify(_ + 1)(adaDv).toTypedValue(using PersonZ.schema) === Right(
            PersonZ("ada", 42)
          )
        )
    }
    "pass writes through on a missing field" >> {
      field("nope").replace(str.reverseGet("x"))(adaDv) === adaDv
    }
    "navigate a sum through variant" >> {
      val dv = ShapeZ.schema.toDynamic(CircleZ(2.5))
      val rO = variant("CircleZ").andThen(field("r")).andThen(double)
      (rO.getOption(dv) === Some(2.5))
        .and(rO.modify(_ * 2)(dv).toTypedValue(using ShapeZ.schema) === Right(CircleZ(5.0)))
        .and(variant("SquareZ").getOption(dv) === None)
    }
    "traverse sequence elements with each" >> {
      val dv = Schema.list[Int].toDynamic(List(1, 2, 3))
      each.andThen(int).modify(_ + 1)(dv).toTypedValue(using Schema.list[Int]) ===
        Right(List(2, 3, 4))
    }
    "rewrite every string at any depth with Plated" >> {
      val dv = ShapeZ.schema.toDynamic(CircleZ(2.5))
      val out = Plated.transform[DynamicValue](v => str.modify(_.toUpperCase)(v))(
        PersonZ.schema.toDynamic(ada)
      )
      (out.toTypedValue(using PersonZ.schema) === Right(PersonZ("ADA", 41)))
        .and(Plated.universe[DynamicValue](dv).exists(double.getOption(_) == Some(2.5)) === true)
    }
  }

  "DynamicValues: dictionaries, sets, indices" should {
    val dict = Schema.map[String, Int].toDynamic(Map("a" -> 1, "b" -> 2))
    val keyA = str.reverseGet("a")

    "key: read, sibling-preserving write, miss pass-through" >> {
      (key(keyA).andThen(int).getOption(dict) === Some(1))
        .and(
          key(keyA)
            .andThen(int)
            .modify(_ + 10)(dict)
            .toTypedValue(using
              Schema.map[String, Int]
            ) === Right(Map("a" -> 11, "b" -> 2))
        )
        .and(key(str.reverseGet("zz")).replace(keyA)(dict) === dict)
        .and(key(keyA).getOption(adaDvOuter) === None)
    }
    "key on a duplicate-key dictionary: first match only, write-back is identity" >> {
      val dup = DynamicValues
        .dictionary
        .reverseGet(_root_.zio.Chunk(keyA -> int.reverseGet(1), keyA -> int.reverseGet(2)))
      (key(keyA).getOption(dup) === Some(int.reverseGet(1)))
        .and(
          key(keyA).replace(int.reverseGet(9))(dup) === DynamicValues
            .dictionary
            .reverseGet(
              _root_.zio.Chunk(keyA -> int.reverseGet(9), keyA -> int.reverseGet(2))
            )
        )
        .and(key(keyA).modify(identity)(dup) === dup)
    }
    "at: in-range read/write, out-of-range pass-through" >> {
      val seq = Schema.list[Int].toDynamic(List(1, 2, 3))
      (at(1).andThen(int).getOption(seq) === Some(2))
        .and(
          at(1).andThen(int).modify(_ * 10)(seq).toTypedValue(using Schema.list[Int]) === Right(
            List(1, 20, 3)
          )
        )
        .and(at(9).replace(int.reverseGet(0))(seq) === seq)
    }
    "Plated reaches set elements and dictionary keys AND values" >> {
      val sets = Schema.set[String].toDynamic(Set("a", "b"))
      val upper = Plated.transform[DynamicValue](str.modify(_.toUpperCase))
      (upper(sets).toTypedValue(using Schema.set[String]) === Right(Set("A", "B")))
        .and(
          upper(Schema.map[String, String].toDynamic(Map("k" -> "v")))
            .toTypedValue(using Schema.map[String, String]) === Right(Map("K" -> "V"))
        )
    }
    "record / sequence / long / bool prisms hit their case" >> {
      (record.getOption(adaDvOuter).map(_.keys.toList) === Some(List("name", "age")))
        .and(sequence.getOption(Schema.list[Int].toDynamic(List(1))).map(_.size) === Some(1))
        .and(long.getOption(long.reverseGet(7L)) === Some(7L))
        .and(bool.getOption(bool.reverseGet(true)) === Some(true))
    }
  }

  "DynamicValues.atField (create / update / delete)" should {
    val nine = int.reverseGet(9)

    "insert a field that was absent — what `field` cannot do" >> {
      val out = atField("extra").replace(Some(nine))(adaDvOuter)
      (field("extra").andThen(int).getOption(out) === Some(9))
        // the original fields survive, in order
        .and(record.getOption(out).map(_.keys.toList) === Some(List("name", "age", "extra")))
        .and(field("extra").replace(nine)(adaDvOuter) === adaDvOuter) // `field` write is a no-op
    }
    "update a present field" >> {
      atField("age").replace(Some(nine))(adaDvOuter).toTypedValue(using PersonZ.schema) ===
        Right(PersonZ("ada", 9))
    }
    "delete a field with None" >> {
      val out = atField("age").replace(None)(adaDvOuter)
      (record.getOption(out).map(_.keys.toList) === Some(List("name")))
        .and(atField("age").getOption(out) === Some(None))
    }
    "read Some(None) for a record lacking the field, and miss on a non-record" >> {
      (atField("nope").getOption(adaDvOuter) === Some(None))
        .and(atField("x").getOption(nine) === None)
        .and(atField("x").replace(Some(nine))(nine) === nine)
    }
  }

  "typed ↔ untyped and byte faces" should {
    "dynamicPrism roundtrips and composes inward" >> {
      val p = PersonZ.schema.dynamicPrism
      (p.getOption(PersonZ.schema.toDynamic(ada)) === Some(ada))
        .and(
          p.getOption(DynamicValue.Primitive(1, _root_.zio.schema.StandardType.IntType)) ===
            None
        )
    }
    "BinaryCodec.prism edits a typed value inside encoded bytes" >> {
      val codec = JsonCodec.schemaBasedBinaryCodec[PersonZ](using PersonZ.schema)
      val bytes = codec.encode(ada)
      val bumped = codec.prism.modify(p => p.copy(age = p.age + 1))(bytes)
      (codec.decode(bumped) === Right(PersonZ("ada", 42)))
        .and(codec.prism.getOption(_root_.zio.Chunk.fromArray("nope".getBytes)) === None)
    }
  }
