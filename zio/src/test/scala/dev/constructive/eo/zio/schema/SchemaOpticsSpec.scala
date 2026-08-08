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
    }
    "turn collections into traversals" >> {
      val seqSchema = Schema.list[Int].asInstanceOf[Schema.Sequence[List[Int], Int, ?]]
      val eachT = seqSchema.makeAccessors(EoAccessorBuilder)
      (eachT.modify(_ + 1)(List(1, 2, 3)) === List(2, 3, 4))
        .and(eachT.foldMap(identity)(List(1, 2, 3)) === 6)
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
